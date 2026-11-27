# 子系统⑤ 盘点预警与补货计划流程 — 运行说明（金日勋）

> 后端 Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8 · 前端 Vue3 + Element Plus + Vite
> 对应课程报告 2.5 节「库存管理模型」、3.3.5 / 3.6.4 节「盘点预警与补货模块」、数据字典 §3.5。
> 包结构 `com.school.material.stocktake.{controller,service,mapper,entity,dto,config,common}`，端口 8085，前缀 `/api/v1/stocktake`。

## 1. 后端

```bash
# 前提：JDK 17、Maven 3.8+、MySQL 8（与子系统② 使用同一个库 material_mgmt）
# ① 建表：本分支自带 DDL，建 stocktake_order / stocktake_item / warning_rule
mysql -u root -p material_mgmt < ../../docs/sql/schema-05-stocktake.sql
#    建库顺序（与团队手册一致）：schema-02 → schema-00-admin → schema-05-stocktake → incremental/*（按文件名）
# ② 启动
cd backend
mvn spring-boot:run
# 服务地址 http://localhost:8085 ，接口文档 http://localhost:8085/swagger-ui.html
```

数据库连接按团队约定走环境变量（不在仓库写地址与密码）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/material_mgmt?...
    username: ${DB_USER:root}
    password: ${DB_PASS:}
```

本地库若不在 3306，启动前设置环境变量即可，例如本机联调：
`set DB_PORT=3307` 后再 `mvn spring-boot:run`。

`application.yml` 中跨子系统地址（**禁止在代码里硬编码**）：

```yaml
material:
  base-service-url: http://localhost:8081     # ① 基础数据与审批
  purchase-service-url: http://localhost:8082 # ② 库存变更唯一写入口
```

补货模型参数（报告 2.5 节）默认值在 `stocktake:` 下，可按物资在平台管理端 `sys_config` 覆盖：

| 键 | 含义 | 默认 |
|---|---|---|
| `stocktake.cover-days` / `stocktake.cover_days.<materialId>` | 覆盖天数 Dc | 30 |
| `stocktake.lead-time-days` / `stocktake.lead_time.<materialId>` | 采购提前期 L | 7（有 ≥2 次入库流水时按入库间隔均值） |
| `stocktake.pack-size` / `stocktake.pack_size.<materialId>` | 包装规格 P | 1（不取整） |
| `stocktake.scan-cron` | 预警扫描定时 | 每日 02:00 |

## 2. 模块功能与处理逻辑（报告表3-44）

| 模块 | 主要输入 | 核心处理 | 主要输出 |
|---|---|---|---|
| 盘点任务 | 仓库、物资集合、实盘数量 | 建单时以 `stock.qty_on_hand` 写账面快照；实盘 − 账面得差异；\|差异\| > 账面×10% 强制二次录入确认 | `stocktake_order`、`stocktake_item` |
| 差异调账 | 盘点明细差异、审批意见 | 完成盘点时逐条调用② 库存服务写盘盈（biz_type=5）/ 盘亏（biz_type=6），由② 在同一事务内更新 `stock` 与 `stock_flow` | 库存余额、库存流水 |
| 库存预警 | `warning_rule`、批次到期日、入账日期 + 限用年限、可用量 | 三档规则扫描：低于补货点 / 临期 / 已过期与到期报废；预警**不落库、不改库存**，结果缓存供查询 | 预警清单、预警统计 |
| 补货建议 | 日均消耗 d、提前期 L、安全库存 S、可用量 qa、在途量 qt | `QS = (Dc+L)×d + S − qa − qt`，按包装规格向上取整 `QO = ⌈QS÷P⌉×P` | 补货建议清单 |
| 采购计划 | 建议明细、申请部门、期望到货 | 一计划一物资写入共享表 `purchase_plan`（`plan_type=1`），草稿 → 待审批 → 已批准/已关闭 | `purchase_plan` |

关键控制：

- **预警只提示不改库存**；差异只有审批通过（完成盘点）后才写流水，驳回则库存不变。
- **库存写入口在②**：本模块只读 `stock` / `stock_flow`，盘盈盘亏核销一律调用 `InventoryClient` → `POST /api/v1/purchase/inventory/{profit|loss}`。
- 跨服务调用**幂等**：`(biz_type, stocktake_id, item_id)` 唯一，重复提交被② 的 `stock_flow` 唯一键拦下，不重复记账；中途失败可修正后重试。
- 盘亏差异按所属状态传入 `scope`：可用 `AVAILABLE` → 预占 `LOCKED` → 在借 `BORROWED`，由② 分别扣减。

## 3. 编码规则（报告表3-32）

| 单据 | 规则 | 示例 |
|---|---|---|
| 盘点单 `stocktake_no` | PD + yyyyMMdd + 4 位流水 | PD202611180001 |
| 采购计划 `plan_no` | CG + yyyyMMdd + 4 位流水 | CG202611180001 |
| 预警编号（展示用） | YJ + yyyyMMdd + 5 位流水 | YJ2026111800001 |

## 4. 对外接口

完整契约见 [`docs/api/api-05.md`](../../docs/api/api-05.md)，摘要：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/stocktake/orders` | 盘点单列表（状态/关键字） |
| GET | `/api/v1/stocktake/orders/{id}` | 盘点单详情（含明细与差异合计） |
| POST | `/api/v1/stocktake/orders` | 新建盘点单（账面快照） |
| POST | `/api/v1/stocktake/orders/{id}/start` | 开始盘点 |
| POST | `/api/v1/stocktake/orders/{id}/items` | 录入实盘数量（超 10% 需二次确认） |
| POST | `/api/v1/stocktake/orders/{id}/finish` | 完成盘点并调账 |
| GET | `/api/v1/stocktake/warnings` | 预警清单（按类型过滤） |
| GET | `/api/v1/stocktake/warnings/summary` | 预警统计 |
| POST | `/api/v1/stocktake/warnings/scan` | 手动触发扫描 |
| GET/POST/DELETE | `/api/v1/stocktake/rules` | 预警规则维护 |
| GET | `/api/v1/stocktake/replenish/suggestions` | 补货建议（含 d / ROP / QS / QO） |
| POST | `/api/v1/stocktake/replenish/plans` | 由建议生成采购计划 |
| GET/POST | `/api/v1/stocktake/plans`、`/plans/submit`、`/plans/approve` | 计划列表、提交、审批 |

## 5. 前端

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5185 ，已配置 /api 代理到 8085
```

页面：工作台（KPI + 快捷入口）、盘点任务（建单/录入/调账/详情）、库存预警（三档 tab + 规则维护）、补货计划（建议 + 采购计划）。

## 6. 演示主线（答辩/联调）

1. 工作台看右上角预警统计 → 「立即扫描预警」
2. 盘点任务：新建盘点单（选仓库，留空物资 = 全仓）→ 开始盘点 → 录入实盘（故意填差 15 件触发 10% 二次确认）→ 完成调账 → 到② 的库存流水核对盘盈/盘亏记录
3. 库存预警：临期与到期报废批次 → 核实后转报损（报损单在④）
4. 补货计划：勾选建议 → 生成采购计划（CG 单号）→ 提交审批 → 管理者审批为已批准 → 采购模块转采购申请

## 7. 依赖与合并

- 依赖子系统② 的 `POST /api/v1/purchase/inventory/profit|loss`（刘志豪 `feature/lzh` 分支已提供 `InventoryController`，合入 main 后即可联调）。
- 依赖子系统① 的物资目录 / 仓库查询；开发期由本模块 `MetaController` 只读兜底，合并后前端改调 `/api/v1/basedata/**` 并删除该控制器。
- 定时任务已抽到 `config/StocktakeScanJob`，由 `stocktake.scan-enabled` 开关控制（缺省 true）。
  合并到 material-boot 单进程后建议设为 `false`，由统一调度器触发，避免与其他子系统任务抢执行；
  不开启定时扫描时仍可手动 `POST /api/v1/stocktake/warnings/scan`。
- 跨子系统地址只从配置读取（`material.purchase-service-url` / `material.base-service-url`），
  代码里不再保留 `localhost` 默认值，配置缺失直接启动失败，避免静默连错环境。
- **建表脚本**：`docs/sql/schema-05-stocktake.sql` 随本分支提交（与基线 `feature/lzh` 中的同名文件内容一致，合并无冲突），
  不再依赖他人脚本。
- **合并基线**：团队评审（2026-11-23）确定以 `feature/lzh` 为基线，本分支合并顺序为
  `merge feature/lzh → 解决冲突 → PR main`；已核对基线最新的
  `20261117-01-inbound-cost-interface.sql`、`20261118-01-dept-quota-po-align.sql`
  只影响 `purchase_order(_item)` 与新增两张共享表，本模块代码无需改动。
- 本分支从 `main` 拉出；PR 前请先 `git pull --rebase origin main`。
