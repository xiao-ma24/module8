# 学校物资管理大系统（School Material Management System）

> **本仓库为 DBA 集成交付版**（由演示仓库完整迁移后改造，演示仓库未做任何修改）。
> **DBA 请先读 [`docs/delivery/DBA交付说明.md`](docs/delivery/DBA交付说明.md)**（建库 → 配置 → 启动 → 部署 → 接统一登录 → 验收）；
> 其余细节见 [`docs/delivery/`](docs/delivery/)。
>
> - 数据库脚本以 `docs/sql/full-schema.sql` 为准，代码表名统一为 `mat_*`（README 下文中的旧表名仅作历史说明）
> - 子系统① 接口前缀已改为 `/api/v1/basedata`，其余模块前缀不变
> - 交付 **6 份前端产物**（`dist/frontend` + `dist/frontend-m00/m01/m03/m04/m05`），DBA 统一前端负责登录与模块门户跳转

> Java + MySQL · 平台管理端 + 统一业务工作台 + 五项业务模块 · 4 人协作 · 最终合并为一个可运行的大系统
>
> 本说明与《08 学校物资管理系统 开发文档（11.14）》的系统规划与分析章节保持一致。

## 一、项目简介与系统边界

面向学校**低值易耗品**和**低值耐用品**的管理平台，围绕五项核心业务：采购验收与入库（含科研经费自购登记入库）、领用申请与出库、借用归还与报损、盘点预警与补货，把需求提出、审批、验收、入库、出库、盘点处置纳入同一套线上规则，使**业务单据、库存余额、库存流水三者一致**。

| 范围 | 本系统处理内容 | 边界说明 |
|---|---|---|
| 范围内 | 采购验收、入库、领用、借用归还、报损、盘点、预警补货 | 所有库存变化必须具有业务来源和操作记录 |
| 固定资产管理 | 不建资产卡、不贴标签、不计提折旧 | 达到固定资产起点的设备移交固定资产管理 |
| 财务管理 | 不付款、不记会计账 | 科研经费自购验收合格后生成**报销凭证号**，仅作报销依据 |
| 课程演示环境 | 模拟/脱敏数据 | 演示期使用临时云服务器，不保存真实敏感数据 |

## 二、系统分层与团队分工

系统采用**「平台管理端 — 统一业务工作台 — 五项业务模块」**三层结构：平台管理端解决「谁能进入、能看什么、谁做过什么」；统一业务工作台按当前用户角色显示菜单与待办；五个业务模块只处理各自的业务单据和库存动作，共享物资目录、仓库、库存、库存流水与审批记录。

| 层次 | 功能 | 目录 | 负责人 | 端口 |
|---|---|---|---|---|
| 平台管理端 | 用户、部门、角色权限、数据字典、系统参数、审计日志 | `subsystem-00-admin` | 李思滢 | 8080 / 5180 |
| 统一业务工作台 | 统一登录、角色分流、待办与模块导航、自助注册 | `portal` | 李思滢 | 5179 |
| 基础数据与审批 | 物资目录、供应商、仓库、统一审批处理 | `subsystem-01-base-data` | 韦森聍 | 8081 |
| 采购验收入库 | 到货登记、验收、异常处理、入库确认；**科研经费自购登记入库** | `subsystem-02-purchase-instock` | 李思滢 | 8082 / 5182 |
| 领用申请审批出库 | 申请、库存与额度校验、部门审批、拣货出库、签收 | `subsystem-03-requisition-outstock` | 刘志豪 | 8083 |
| 借用归还报损 | 借用审批、借出登记、归还验收、损坏缺失报损核销 | `subsystem-04-borrow-return-damage` | 刘志豪 | 8084 |
| 盘点预警与补货 | 盘点差异、安全库存/临期/超限预警、补货建议与采购计划 | `subsystem-05-stocktake-replenish` | 金日勋 | 8085 |

> 报告中「基础数据与审批」对应 ①；「采购验收入库」中的科研经费自购场景归入 ②；「领用与借用」在仓库中拆为 ③④ 两个目录，便于并行开发，合并后仍是同一业务层。

## 三、技术栈

- **后端**：Java 17 + Spring Boot 3.2.x + Maven + MyBatis-Plus
- **数据库**：MySQL 8.0，统一库名 `material_mgmt`（utf8mb4）
- **前端**：Vue 3 + Vite + Element Plus + ECharts
- **接口**：RESTful JSON，统一响应格式见 `docs/接口规范与合并指南.md`
- **AI 助手**：OpenAI 兼容接口（默认 DeepSeek），未配置 Key 时自动降级为规则引擎演示模式

## 四、目录结构

```
school-material-management/
├── README.md                        # 本文件
├── .gitignore
├── docs/
│   ├── 队友上手指南.md              # ★ 队友必读：加入仓库、环境、日常工作流
│   ├── 开发约束与规范.md             # ★ 强制规范：分支/PR、包结构、错误码、接口文档契约
│   ├── 数据字典.md                  # 全部业务表 + 外部系统引用（师资/学生/财务）
│   ├── 接口规范与合并指南.md         # 端口分配、接口约定、5→1 合并方案
│   ├── 合并运行手册.md              # 合并前检查清单、多模块改造步骤、端到端冒烟测试
│   ├── api/
│   │   ├── api-template.md          # 接口文档模板（复制为 api-0X.md 使用）
│   │   └── api-0X.md                # 各子系统接口契约文档（各自维护）
│   └── sql/
│       ├── full-schema.sql          # ★ 全量结构（DBA 执行，39 张表，表名 mat_*）
│       ├── init-data.sql            # ★ 初始化基线数据（可重复执行）
│       ├── demo-data.sql            # 业务演示数据（仅联调测试环境，禁止入生产）
│       ├── verify.sql               # ★ 导入后自检
│       ├── README.md                # 脚本执行顺序与说明
│       ├── init_template.sql        # 课程联调用外部系统模拟表（DBA 不需要）
│       └── legacy/                  # 旧表名时代的历史脚本（禁止用于新环境）
├── portal/                          # 统一业务工作台（登录/注册/角色分流/待办，5179）
├── subsystem-00-admin/              # 平台管理端 — 李思滢（后端 8080 / 前端 5180）
├── subsystem-01-base-data/          # 基础数据与审批 — 韦森聍（8081）
├── subsystem-02-purchase-instock/   # 采购验收入库 — 李思滢（后端 8082 / 前端 5182）
├── subsystem-03-requisition-outstock/  # 领用申请审批出库 — 刘志豪（8083）
├── subsystem-04-borrow-return-damage/  # 借用归还报损 — 刘志豪（8084）
└── subsystem-05-stocktake-replenish/   # 盘点预警与补货 — 金日勋（8085）
```

## 五、核心业务主流程

业务主线：**需求或预警触发 → 审批或验收控制 → 库存或个人保管登记 → 流水留痕 → 报表、预警或处置反馈**。

| 流程 | 关键控制 | 数据落点 |
|---|---|---|
| 基础与权限管理 | 工号/部门/岗位校验后授权，信息不全退回 | `sys_user`、`sys_user_role`、`approve_record`、`sys_operation_log` |
| 科研经费自购登记入库 | 先登记申请与发票，部门负责人核验，验收合格才允许入库 | `purchase_plan`（`plan_type=2`）、`stock`（个人保管仓）、`stock_flow`、报销凭证号 |
| 采购验收与入库 | 先验收后入公共库存；不合格登记异常，不增加库存 | `acceptance_*`、`inbound_*`、`stock`、`stock_flow` |
| 领用申请审批出库 | 提交时校验可用量与部门额度；出库即扣库 | `req_order`、`approve_record`、`out_order`、`stock_flow`（负数） |
| 借用归还报损 | 借出减可用量不减在库量；归还验收，损坏缺失转报损 | `borrow_return(_item)`、`loss_order`、`stock_flow` |
| 盘点预警与补货 | 预警不直接改数量；差异与报废须审批后走盘盈盘亏/报废核销流水 | `stocktake_*`、`warning_rule`、`purchase_plan`、`stock_flow` |

## 六、库存模型与补货算法

库存采用**「余额表记录现在有多少，流水表记录为什么变化」**双表口径：`stock` 保存批次级余额，`stock_flow` 保存每一次变动；两者必须在同一事务中更新，禁止直接改余额。

- 三量分离：`qty_on_hand`（在库，含在借与预占）、`qty_available`（公共可领）、`qty_locked`（预占），约束 `qty_available + qty_locked <= qty_on_hand`
- 公共仓与**虚拟个人保管仓**（科研经费自购）分开：个人仓 `qty_available` 恒为 0，不参与公共领用
- `stock_flow.biz_type`：1 入库、2 领用、3 借用、4 归还、5 盘盈、6 盘亏、7 报废核销
- 幂等：`stock_flow` 以 `(biz_type, biz_id, biz_item_id)` 唯一，防止重复入账

补货模型（详见 `subsystem-05-stocktake-replenish/README.md`）：

```
日均消耗量   d = Σ|change_qty| / N            （N 默认 90 天，不足按有效天数）
补货点       ROP = d × L + S                  （L 采购提前期，S 安全库存 material.safe_stock）
建议补货量   Q = max(0, (Dc + L) × d + S − qa − qt)   （Dc 覆盖天数默认 30，qa 可用量，qt 在途量）
包装取整     Qorder = ceil(Q / Pkg) × Pkg
```

## 七、协作规范（详见 `docs/开发约束与规范.md`，强制遵守）

- **分支**：`main` 为保护分支禁止直推；个人分支 `feature/<子系统编号>-<姓名拼音>`，通过 PR 合入
- **接口先行**：编码前先写 `docs/api/api-0X.md`，群确认后再实现
- **字典先行**：表结构变更先更新 `docs/数据字典.md`，增量 SQL 放 `docs/sql/incremental/`
- **提交信息**：`<类型>: <子系统编号>-描述`，如 `feat: 02-新增验收入库接口`

## 八、子系统合并策略（5 → 1 大系统）

> 详细操作步骤见 `docs/合并运行手册.md`（含合并前检查清单与端到端冒烟测试）

1. **开发期（现在）**：每个子系统独立 Spring Boot 应用独立运行，端口 8081–8085，通过 HTTP 接口互调
2. **合并期**：改造为 Maven 多模块工程 `material-parent`：
   - `material-common`：统一响应、公共实体、外部系统 Client（师资/学生/财务）、工具类
   - 五个子系统模块（base-data / purchase / requisition / borrow / stocktake）
   - `material-boot`：整合启动模块，扫描全部模块，统一端口 8080
3. **前端合并**：五个前端合并为一个 Vue 工程，按子系统划分路由/模块
4. **外部系统**：师资/学生/财务系统通过 `ExternalSystemClient` 统一对接，详见 `docs/数据字典.md` 第 4 节

## 九、本地运行说明

### 9.1 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | **17**（必须） | Spring Boot 3.x 不支持 JDK 8/11；JDK 21+ 需确认 Lombok 版本兼容 |
| Maven | 3.8+ | 或使用 IDEA 内置 Maven |
| Node.js | 18+ | 前端 Vue3 + Vite |
| MySQL | 8.0 | 库名 `material_mgmt`，字符集 utf8mb4 |

### 9.2 初始化数据库（只需一次）

仓库约定端口为 `3307`；若本机 MySQL 实际端口不同（例如官方安装包默认 `3306`），把下面的 `-P` 改掉即可，并在 9.3 同步修改数据源 URL。

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p < docs/sql/init_template.sql
mysql -h 127.0.0.1 -P 3307 -u root -p < docs/sql/schema-02.sql          # 内含 CREATE DATABASE
mysql -h 127.0.0.1 -P 3307 -u root -p material_mgmt < docs/sql/schema-00-admin.sql
mysql -h 127.0.0.1 -P 3307 -u root -p material_mgmt < docs/sql/schema-05-stocktake.sql
for f in docs/sql/incremental/*.sql; do mysql -h 127.0.0.1 -P 3307 -u root -p material_mgmt < "$f"; done
```

### 9.3 本地私有配置（不入库）

`application.yml` 只放通用占位配置；数据库密码、AI Key 一律写 `application-local.yml`（已被 `.gitignore` 忽略），启动时加 `--spring.profiles.active=local`：

```yaml
# subsystem-0X/backend/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/material_mgmt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 你的数据库密码

ai:                                  # 仅 subsystem-02 使用
  base-url: https://api.deepseek.com/v1
  api-key: sk-你的DeepSeekKey
  model: deepseek-chat
```

> AI 助手：未配置 `ai.api-key` 时自动降级为规则引擎演示模式，功能可点通但回答不是大模型生成；配置后 `/api/v1/purchase/ai/status` 返回 `llmEnabled=true`。Key 在 https://platform.deepseek.com 创建，形如 `sk-` 开头。

### 9.4 启动顺序

```bash
# ① 平台管理端（后端 8080 / 前端 5180）—— 统一登录与权限入口，必须最先启动
cd subsystem-00-admin/backend && mvn -q package -DskipTests && \
  java -jar target/admin-platform-1.0.0.jar --spring.profiles.active=local
cd subsystem-00-admin/frontend && npm install && npm run dev

# ② 采购验收与入库（后端 8082）
cd subsystem-02-purchase-instock/backend && mvn -q package -DskipTests && \
  java -jar target/purchase-instock-1.0.0.jar --spring.profiles.active=local

# ③ 领用出库（后端 8083）
cd subsystem-03-requisition-outstock/backend && mvn -q package -DskipTests && \
  java -jar target/requisition-outstock-1.0.0.jar --spring.profiles.active=local

# ④ 借用归还报损（后端 8084）
cd subsystem-04-borrow-return-damage/backend && mvn -q package -DskipTests && \
  java -jar target/borrow-return-damage-1.0.0.jar --spring.profiles.active=local

# ① 基础数据与审批（后端 8081 / 前端 5173）—— 数据库账号走环境变量，默认 root 空密码
cd subsystem-01-base-data/backend && DB_USER=root DB_PASS=你的密码 mvn -q package -DskipTests && \
  DB_USER=root DB_PASS=你的密码 java -jar target/material-base-data-1.0.0.jar
cd subsystem-01-base-data/frontend && npm install && npm run dev

# ⑤ 盘点预警与补货（后端 8085 / 前端 5185）—— 数据库账号同样走环境变量
cd subsystem-05-stocktake-replenish/backend && DB_USER=root DB_PASS=你的密码 mvn -q package -DskipTests && \
  DB_USER=root DB_PASS=你的密码 java -jar target/stocktake-replenish-1.0.0.jar
cd subsystem-05-stocktake-replenish/frontend && npm install && npm run dev

# 统一业务大系统前端（5182）：采购 / 领用借用 / 盘点预警 / 基础数据与审批 页面与菜单已合并，一次登录全部可用
# /api/v1/requisition → 8083，/api/v1/borrow → 8084，/api/v1/stocktake → 8085，/api/v1/basedata → 8081，其余 /api → 8082
cd subsystem-02-purchase-instock/frontend && npm install && npm run dev

# ⑨ 门户登录入口（5179，登录后统一进入 5182）
node portal/serve.mjs
```

### 9.5 访问地址与演示账号

| 入口 | 地址 |
|---|---|
| 统一业务大系统（采购·领用·借用，推荐） | http://localhost:5182 |
| 门户登录入口 | http://localhost:5179 |
| 平台管理端 | http://localhost:5180 |
| ① 基础数据与审批 | http://localhost:5173 |
| ⑤ 盘点预警与补货 | http://localhost:5185 |

密码统一 `123456`。非管理员账号登录后统一进入 5182，左侧菜单按角色展示「采购管理 / 领用出库 / 借用归还」三组，无需再切换系统：

| 姓名 | 账号 | 角色 | 可用模块 |
|---|---|---|---|
| 李思滢 | T0002 | 采购员 | 采购全流程 + 领用/借用查询 |
| 刘志豪 | T0003 | 仓库管理员 | 入库/库存 + 领用出库 + 借出/归还/报损 |
| 王主任 | T0001 | 部门负责人 | 采购申请 + 领用/借用审批 |
| 金日勋 | T0004 | 盘点员 | 盘点核实 + 领用/借用库存查询（⑤盘点页面尚未并入 5182） |
| 韦森宁 | T0005 | 系统管理员 | 平台管理端 |
| 张老师 | T0006 | 申请人 | 采购/领用/借用申请 + 个人中心 |
| 韦森聍 | T0007 | 验收人 | 到货验收、数量质量核验（02 验收页，与库管员岗位分离） |

> 5183 / 5184 独立前端已并入 5182，不再需要单独启动。

### 9.6 常见问题

| 现象 | 原因与处理 |
|---|---|
| 编译报「找不到符号 getXxx()」 | JDK 版本过高导致 Lombok 注解处理失效，改用 JDK 17 |
| 启动后所有接口 500 | 数据源端口/库名/密码错误，检查 `application-local.yml`；`characterEncoding` 必须写 `UTF-8` |
| 登录提示账号待启用 | 自助注册账号需管理员在「用户管理 → 待启用」中点启用 |
| AI 回答像模板拼接 | `ai.api-key` 未配置或无效，查看 9.3 |
