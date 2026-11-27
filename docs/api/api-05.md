# 子系统⑤ 接口文档 —— 盘点预警与补货计划

> 负责人：金日勋 · 端口 8085 · 前缀 `/api/v1/stocktake`
> 统一响应：`{ "code": 0, "msg": "success", "data": ... }`，错误码区间 **5000–5999**
> 预警只提示不改库存；库存写入口唯一在子系统② 的 `InventoryService`。

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-18 | 金日勋 | 初始版：盘点任务、三档预警、补货建议、采购计划 |

## 接口列表

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/orders` | 盘点单列表 |
| 2 | GET | `/orders/{id}` | 盘点单详情（含明细） |
| 3 | POST | `/orders` | 新建盘点单（账面快照） |
| 4 | POST | `/orders/{id}/start` | 开始盘点 |
| 5 | POST | `/orders/{id}/items` | 录入实盘数量 |
| 6 | POST | `/orders/{id}/finish` | 完成盘点并调账 |
| 7 | GET | `/warnings` | 预警清单 |
| 8 | GET | `/warnings/summary` | 预警统计 |
| 9 | POST | `/warnings/scan` | 手动扫描 |
| 10 | GET / POST / DELETE | `/rules` | 预警规则 |
| 11 | GET | `/replenish/suggestions` | 补货建议 |
| 12 | POST | `/replenish/plans` | 生成采购计划 |
| 13 | GET | `/plans` | 采购计划列表 |
| 14 | POST | `/plans/submit` | 提交审批 |
| 15 | POST | `/plans/approve` | 审批 |

---

## 1. 新建盘点单

- **路径**：`POST /api/v1/stocktake/orders`
- **触发规则**：仓库不存在 5002；该仓库没有在库批次 5003；未选仓库 5001
- **说明**：明细账面数量取建单时刻 `stock.qty_on_hand` 快照，之后出库不影响本次差异

### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| warehouseId | Long | 是 | 盘点仓库（实体仓） |
| stocktakeDate | Date | 否 | 盘点日期，默认当天 |
| auditorId | Long | 否 | 盘点负责人 `sys_user.user_id` |
| materialIds | Long[] | 否 | 为空表示盘点该仓库全部在库物资 |
| remark | String | 否 | 备注 |
| operatorId | Long | 否 | 操作人 |

```json
{ "warehouseId": 1, "stocktakeDate": "2026-11-18", "auditorId": 1, "materialIds": [1, 2], "remark": "11月月末盘点" }
```

### 响应示例

```json
{ "code": 0, "msg": "success", "data": { "stocktakeId": 1, "stocktakeNo": "PD202611180001", "status": 0 } }
```

---

## 2. 录入实盘数量

- **路径**：`POST /api/v1/stocktake/orders/{id}/items`
- **触发规则**：明细不存在 5021；单据不在“进行中” 5022；实盘为负 5023；
  **\|差异\| > 账面×10% 且未确认返回 5024**（前端二次确认后带 `confirmOver10=true` 重提）
- **说明**：只记录差异，不写库存流水

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| itemId | Long | 是 | 盘点明细主键 |
| actualQuantity | BigDecimal | 是 | 实盘数量，≥0 |
| diffReason | String | 否 | 差异原因 |
| confirmOver10 | Boolean | 否 | 差异超 10% 时的二次录入确认 |

```json
{ "itemId": 3, "actualQuantity": 42, "diffReason": "领用未及时出库", "confirmOver10": true }
```

```json
{ "code": 0, "msg": "success",
  "data": { "itemId": 3, "bookQuantity": 45, "actualQuantity": 42, "diffQuantity": -3, "diffType": "盘亏" } }
```

失败示例：

```json
{ "code": 5024, "msg": "差异 -15 超过账面数量 10%，请二次录入确认后再提交", "data": null }
```

---

## 3. 完成盘点并调账

- **路径**：`POST /api/v1/stocktake/orders/{id}/finish`
- **触发规则**：非进行中 5031；无明细 5032；存在未录入实盘 5033；库存服务调用失败 5034 / 5011
- **说明**：逐条调用子系统② `POST /api/v1/purchase/inventory/profit|loss`，
  幂等键 `(biz_type, stocktake_id, item_id)`；中途失败可修正后重试，不会重复记账

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| operatorId | Long | 是 | 操作人（写入流水 operator_id） |
| opinion | String | 否 | 审批意见，追加到盘点单备注 |

```json
{ "stocktakeNo": "PD202611180001", "adjusted": 3, "profit": 0, "loss": 3, "same": 12 }
```

---

## 4. 预警清单

- **路径**：`GET /api/v1/stocktake/warnings?warningType=&keyword=`
- **说明**：预警由 `warning_rule` + 实时库存计算，不落库；`warningType`：1 低于补货点 / 2 临期 / 3 已过期 / 4 到期报废

```json
{ "code": 0, "msg": "success",
  "data": [ { "warningType": 1, "warningLabel": "低于补货点", "level": "高",
              "materialId": 1, "materialCode": "01-001", "materialName": "A4 复印纸", "unit": "箱",
              "batchNo": null, "currentQty": 45, "threshold": 100,
              "message": "可用量 45 已低于补货点 100（日均 15，提前期 4 天）" } ] }
```

统计接口 `GET /warnings/summary`：

```json
{ "total": 12, "low": 7, "nearExpire": 3, "expired": 1, "lifeDue": 1, "scanTime": "2026-11-18T10:00:00" }
```

---

## 5. 补货建议

- **路径**：`GET /api/v1/stocktake/replenish/suggestions?keyword=`
- **说明**：只返回“可用量 ≤ 补货点且建议量 > 0”的物资

| 字段 | 含义 | 公式 |
|---|---|---|
| daily | 日均消耗 d | Σ\|领用出库\| / T，T 默认 90 天 |
| leadTimeDays | 采购提前期 L | 最近入库间隔均值，不足两次取默认值 |
| rop | 补货点 ROP | d × L + S |
| qa | 当前可用量 | 未过期批次 `qty_available` 之和 |
| inTransit | 在途量 qt | 未完成采购订单已订购未到货 |
| suggestQty | 建议补货量 QS | (Dc + L) × d + S − qa − qt |
| packSize / orderQty | 包装规格 P / 下单量 QO | ⌈QS ÷ P⌉ × P |

```json
{ "materialId": 1, "materialName": "A4 复印纸", "unit": "箱", "daily": 15, "leadTimeDays": 4,
  "coverDays": 30, "safeStock": 40, "rop": 100, "qa": 45, "inTransit": 0,
  "suggestQty": 430, "packSize": 5, "orderQty": 430, "refPrice": 120, "amount": 51600 }
```

---

## 6. 生成采购计划

- **路径**：`POST /api/v1/stocktake/replenish/plans`
- **触发规则**：未选择建议 5051；未选部门 5052；计划数量 ≤ 0 5054
- **说明**：写入共享表 `purchase_plan`（`plan_type=1`），单号 CG+yyyyMMdd+4 位，状态 0 草稿

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deptId | Long | 是 | 申请部门 |
| operatorId | Long | 否 | 操作人 |
| lines[].materialId | Long | 是 | 物资 |
| lines[].planQty | BigDecimal | 是 | 计划数量（已取整） |
| lines[].stockQty | BigDecimal | 否 | 生成时可用量快照 |
| lines[].supplierId | Long | 否 | 建议供应商 |
| lines[].expectDate | Date | 否 | 期望到货 |

```json
{ "created": 3, "planNos": ["CG202611180001", "CG202611180002", "CG202611180003"] }
```

---

## 7. 采购计划审批

- `POST /api/v1/stocktake/plans/submit`：`{ "planIds": [1,2], "approverId": 1 }` —— 草稿(0) → 待审批(1)
- `POST /api/v1/stocktake/plans/approve`：`{ "planIds": [1,2], "approverId": 4, "pass": true, "opinion": "同意补货" }`
  —— 待审批(1) → 已批准(2) / 已关闭(4)；条件更新，重复审批返回 5064

---

## 错误码

| 码 | 含义 |
|---|---|
| 5001 | 未选择盘点仓库 |
| 5002 | 仓库不存在 |
| 5003 | 该仓库没有可盘点的在库批次 |
| 5004 | 盘点单不存在 |
| 5011 | 子系统② 库存服务调用失败 |
| 5012 | 单据状态不允许开始盘点 |
| 5021–5024 | 明细相关（不存在 / 状态 / 数量非法 / 需二次确认） |
| 5031–5034 | 完成盘点相关（状态 / 明细 / 未录入 / 调账失败） |
| 5041–5043 | 预警规则相关 |
| 5051–5054 | 采购计划生成相关 |
| 5061–5064 | 采购计划提交/审批相关 |
