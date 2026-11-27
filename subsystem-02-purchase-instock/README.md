# 子系统② 采购验收与入库流程 — 运行说明（李思滢）

> 后端 Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8 · 前端 Vue3 + Element Plus + ECharts
> 对应课程报告 3.7 节「采购验收与入库模块」、数据字典表2-9/表2-11/表2-15；字段口径见 `docs/数据字典.md` 3.2 节。

## 1. 后端

```bash
# 前提：JDK 17（必须，Spring Boot 3 不支持 8/11）、Maven 3.8+、MySQL 8
# ① 初始化数据库（先 schema-02，再 schema-00-admin、schema-05-stocktake、incremental/*）
mysql -u root -p < ../../docs/sql/schema-02.sql
mysql -u root -p material_mgmt < ../../docs/sql/schema-00-admin.sql
mysql -u root -p material_mgmt < ../../docs/sql/schema-05-stocktake.sql
mysql -u root -p material_mgmt < ../../docs/sql/incremental/20261117-01-inbound-cost-interface.sql
# ② 本地私有配置：数据源密码 + DeepSeek Key 写入
#    backend/src/main/resources/application-local.yml（已 gitignore，不入库）
# ③ 启动（必须带 local profile，否则读不到私有配置）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
# 服务地址 http://localhost:8082 ，接口文档 http://localhost:8082/swagger-ui.html
```

## 2. 模块功能与处理逻辑（报告表3-7、表3-28）

| 模块 | 主要输入 | 核心处理 | 主要输出 |
|---|---|---|---|
| 采购申请与预算校验 | 物资、数量、用途、预算科目 | 计算参考金额；校验预算并冻结；提交审批 | `purchase_request(_item)`、审批记录 |
| 采购订单与供应商选择 | 已批准申请、供应商、下单数量与价格 | 只允许合作中供应商；历史均价对比与价格异常提醒；跟踪到货 | `purchase_order(_item)` |
| 到货验收与异常处理 | 送货实物、送货单、订单明细 | 逐项登记验收/合格/不合格数量；不合格强制选退货/换货 | `acceptance_order/_item`、验收报告 |
| 入库确认与批次登记 | 合格验收明细、仓库、批次、生产/有效期 | 仅 `handle_type=入库` 且 `pass_qty>0` 生成入库单；补充批次号与质保期 | `inbound_order/_item`、二维码标签 |
| 库存同步与凭证输出 | 已确认入库单及明细 | 事务写 `stock`、`stock_flow`；输出实际入库成本；回填凭证号 | 库存余额、流水、`material_inbound_interface`、`voucher_no`/`reimburse_voucher_no` |

关键控制（报告表3-28）：

- 确认入库使用事务与行锁（`in_status=0` 条件更新），`stock` 与 `stock_flow` 必须同时成功，重复提交整体回滚。
- `purchase_mode=2` 科研经费自购只增加 `qty_on_hand`，不增加公共 `qty_available`，且必须登记实际保管教师与虚拟个人保管仓。
- 停止合作的供应商不得确认入库；低值耐用品必须维护限用年限，有保质期的物资必须落到批次到期日。
- 验收人与库管员原则上分离，同一笔业务不得为同一人。

## 3. 编码规则（报告表3-22）

| 单据 | 规则 | 示例 |
|---|---|---|
| 采购订单 `po_no` | PO + yyyyMMdd + 4位流水 | PO202611170001 |
| 验收单 `acpt_no` | YS + yyyyMMdd + 4位流水 | YS202611170001 |
| 入库单 `in_no` | RK + yyyyMMdd + 4位流水 | RK202611170001 |
| 报销凭证 `voucher_no` | BX + yyyyMMdd + 4位流水 | BX202611170001 |
| 库存批次 `batch_no` | 优先供应商批号，无批号则入库单号派生 | RK202611170001-1 |

## 4. 跨模块成本接口（报告表2-11）

- 表 `material_inbound_interface`，联合主键 `(in_no, material_id)`，字段：`in_price`、`in_qty`、`in_amount`、`in_date`、`warehouse_id`。
- 确认入库时与 `stock`、`stock_flow` **同一事务**写入；`in_price` 取采购订单实际下单价，`in_amount = in_price × in_qty`，四舍五入两位。
- 第5组财务按本表做成本结转，只读引用；库存数量仍以 `stock` / `stock_flow` 为准。
- 财务核对接口：`GET /api/v1/purchase/finance/recon?supplierId=&materialId=&startDate=&endDate=`，直接读取本表输出入库日期、数量、单价、金额与经办人。

## 5. 前端

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5182
```

## 6. AI 大模型（可选）

`application.yml` 中：

```yaml
ai:
  base-url: https://api.deepseek.com/v1   # 任意 OpenAI 兼容接口
  api-key: "sk-xxx"                        # 留空 = 规则引擎演示模式
  model: deepseek-chat
```

不配置 Key 时 AI 助手同样可用（规则引擎查真实数据库），配置后自动升级为大模型对话。

## 7. 演示主线（答辩/联调用）

1. 工作台看板：四张统计卡 + 趋势/类别/供应商图表 + 待办
2. 采购申请：新建申请 → 提交（预算校验+冻结）→ 审批（同意/驳回）→ 转订单（含历史均价比价、价格异常提醒）
3. 采购订单：确认 → 到货验收：逐项送检/合格数量，不合格强制选退货/换货 → 完成验收（评分计入供应商星级）
4. 入库管理：登记批次号/质保期 → 确认入库（库存+流水+成本接口+订单进度+预算扣减联动）→ 打印二维码标签 → 获取报销凭证号
5. **科研经费自购登记入库**（报告表2-9、表2-18）：自购登记（`purchase_mode=2`、`holder_id`）→ 部门负责人核验审批 → 到货验收 → 入库到**虚拟个人保管仓**（`qty_available` 恒为 0，不参与公共领用）→ 生成报销凭证号
6. 财务核对：按供应商/物资/日期查 `material_inbound_interface`，展示实际入库单价、金额，导出 Excel
7. AI 助手：查库存 / 补货建议 / 订单情况 / 预算余额；验收报告 AI 润色

## 8. 与大系统合并

- 包结构 `com.school.material.purchase.{controller,service,mapper,entity,config,common}`，按合并手册整包搬移
- **库存写入口径在②**：`InventoryService` 是 `stock` / `stock_flow` 的唯一写入口，③④⑤ 通过接口调用，合并后仍由本模块提供，不替换给⑤
- 预算/凭证写入隔离在 `BudgetMapper`，合并时替换为第 5 组财务系统接口
- 审批写入统一审批表 `approve_record`（`biz_type=4` 采购），与①共用同一张表
- 成本接口表 `material_inbound_interface` 由②写入、第5组只读消费；合并后保持「②写、财务读」的单向依赖，不反向修改入库数据
