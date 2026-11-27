# 子系统④ 接口文档 —— 借用归还报损

> 负责人：刘志豪 · 端口 **8084** · 前缀 `/api/v1/borrow` · 错误码区间 **4000–4999**
> 统一响应：`{ "code": 0, "msg": "success", "data": ... }`
> 数据表：`borrow_return`、`borrow_return_item`、`loss_order`、`out_order`（source_type=2）、`approve_record`（biz_type=2/3）

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-16 | 刘志豪 | 初始版：按 11.16 报告数据字典（`borrow_return` / `loss_order`）实现借用归还报损全流程 |

## 0. 角色与数据范围

| 角色 | 可做什么 | 数据范围 |
|---|---|---|
| `APPLICANT` / `STUDENT` | 提交借用申请、查看本人在借与归还、查看本人报损 | 本人单据 |
| `DEPT_HEAD` | 借用审批 + 报损审批 | 本部门单据 |
| `STORE_KEEPER` | 借出登记、归还验收、报损核销执行 | 全部单据 |
| `ADMIN` | **只读**查看全部 | 全部单据 |
| `PURCHASER` / `CHECKER` | 与借用业务无关，接口直接拒绝（403） | — |

## 1. 状态机（borrow_status）

| 值 | 含义 | 允许的下一动作 |
|---|---|---|
| 0 | 待审批 | 审批通过（→1）、驳回（→5 关闭） |
| 1 | 待借出 | 借出登记（→2） |
| 2 | 在借 | 归还验收：全部完好（→3）、损坏/缺失转报损（→4） |
| 3 | 已归还 | 终态 |
| 4 | 已报损 | 报损审批并核销完成后（→5） |
| 5 | 已核销 | 终态 |

补充口径（报告 §2.2 注）：
- `condition_code`：1 完好、2 轻微损坏、3 严重损坏、4 缺失；
- 归还验收为 1 的恢复可用量，为 2～4 的转报损登记；
- **逾期不落库**，由 `due_date < 今天` 且 `borrow_status=2` 实时计算（`overdue` / `overdueDays` 字段）；
- 报告 `borrow_status` 取值集合无「已驳回」，驳回即关闭单据并置 5 已核销，驳回结论由
  `approve_record`（`biz_type=2`、`approve_result=0`）留痕。

## 2. 库存口径（关键）

| 动作 | qty_on_hand | qty_available | qty_locked | 流水 |
|---|---|---|---|---|
| 借出 | 不变 | −q | 不变 | `stock_flow.biz_type=3` 借用 |
| 完好归还 | 不变 | +q | 不变 | `biz_type=4` 归还 |
| 借用报损核销 | −q | 不变 | 不变 | `biz_type=7` 报废核销 |
| 库内报废核销（盘亏/超限） | −q | −q | 不变 | `biz_type=7` |

> 即「借出只减可用、不减在库」，这正是借用与领用的本质区别；库存在同一事务内由子系统② 统一写入。

## 3. 接口列表

| # | 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|---|
| 1 | POST | `/auth/login` | 登录 | 公开 |
| 2 | POST | `/auth/logout` | 退出 | 登录即可 |
| 3 | GET | `/auth/me` | 当前用户 | 登录即可 |
| 4 | GET | `/dashboard` | 工作台 | 登录即可 |
| 5 | GET | `/meta/materials` | 可借物资（仅低值耐用品） | 登录即可 |
| 6 | GET | `/meta/warehouses` | 实体仓库 | 登录即可 |
| 7 | GET | `/meta/stocks` | 库存总览 | 登录即可 |
| 8 | GET | `/meta/batches` | 可选批次 + 可借量 | 登录即可 |
| 9 | GET | `/orders` | 借用单列表 | APPLICANT/STUDENT/DEPT_HEAD/STORE_KEEPER |
| 10 | GET | `/orders/pending-approve` | 待我审批（借用） | DEPT_HEAD |
| 11 | GET | `/orders/{id}` | 借用单详情 | 同上（受数据范围） |
| 12 | GET | `/orders/{id}/items` | 分次归还明细 | 同上 |
| 13 | GET | `/orders/{id}/logs` | 审批留痕 | 同上 |
| 14 | POST | `/orders` | 提交借用申请 | APPLICANT/STUDENT/DEPT_HEAD |
| 15 | POST | `/orders/{id}/approve` | 借用审批 | DEPT_HEAD |
| 16 | GET | `/lends/pending` | 待借出清单 | STORE_KEEPER |
| 17 | POST | `/lends` | 借出登记 | STORE_KEEPER |
| 18 | GET | `/outbounds` | 借出出库单列表 | 登录即可（受数据范围） |
| 19 | GET | `/returns/borrowed` | 在借清单 | 登录即可（受数据范围） |
| 20 | GET | `/returns/overdue` | 逾期清单（按逾期天数倒序） | 登录即可（受数据范围） |
| 21 | POST | `/returns` | 归还验收（可分次） | STORE_KEEPER |
| 22 | GET | `/losses` | 报损单列表 | 登录即可（受数据范围） |
| 23 | GET | `/losses/{id}/logs` | 报损审批留痕 | 登录即可（受数据范围） |
| 24 | POST | `/losses/{id}/approve` | 报损审批（含处置结论） | DEPT_HEAD |
| 25 | POST | `/losses/{id}/execute` | 报损核销执行 | STORE_KEEPER |

---

## 4. 提交借用申请

- **路径**：`POST /api/v1/borrow/orders`
- **触发规则**：
  1. 物资必须为**低值耐用品**（`material_type=2`），否则 4054（易耗品请走领用）；
  2. 可借数量 ≥ 借用数量，否则 **4001**；
  3. 预计归还日期不得早于今天，否则 4052。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| materialId | Long | 是 | 物资主键（一单一物资） |
| borrowQty | BigDecimal | 是 | 借用数量 > 0 |
| dueDate | Date | 是 | 预计归还日期（yyyy-MM-dd） |

### 响应示例（成功）

```json
{
  "code": 0, "msg": "success",
  "data": {
    "borrowId": 2, "borrowNo": "JY202611160001", "borrowerId": 6,
    "materialId": 5, "borrowQty": 2.000, "returnedQty": 0, "writtenOffQty": 0,
    "dueDate": "2026-11-23", "borrowStatus": 0, "availableQty": 172.000, "overdue": false
  }
}
```

---

## 5. 借用审批

- **路径**：`POST /api/v1/borrow/orders/{id}/approve`
- **规则**：仅本部门 `DEPT_HEAD`（否则 4016）；通过前再次校验可借数量；结论写 `approve_record(biz_type=2)`。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| result | Integer | 是 | 1 通过、0 驳回 |
| opinion | String | 否 | 驳回必填 |

---

## 6. 借出登记

- **路径**：`POST /api/v1/borrow/lends`
- **规则**：
  - 仅 `STORE_KEEPER`；
  - 单据必须为 `borrow_status=1 待借出`，否则 4012；一单只能借出一次（4013）；
  - 生成 `out_order`（`source_type=2`、`source_id=borrow_id`、记录仓库与批次）；
  - 调用子系统② `POST /api/v1/purchase/inventory/borrow-out?reserved=false`：**在库不变、可用 −q**，
    写 `stock_flow(biz_type=3)`；
  - 借用单记录 `warehouse_id` / `batch_no`，状态置 2 在借。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| borrowId | Long | 是 | 借用单主键 |
| warehouseId | Long | 是 | 借出仓库（实体仓） |
| batchNo | String | 是 | 借出批次（先到期先出） |

### 响应示例（成功）

```json
{
  "code": 0, "msg": "success",
  "data": { "outId": 2, "outNo": "CK202611160002", "sourceType": 2, "sourceId": 2,
            "warehouseId": 2, "batchNo": "INIT-02-001", "outQty": 2.000 }
}
```

---

## 7. 归还验收（可分次归还）

- **路径**：`POST /api/v1/borrow/returns`
- **规则**：
  - 仅 `STORE_KEEPER`；单据必须为 `2 在借` 或 `4 已报损`，否则 4014；
  - 归还数量 ≤ 未归还数量（`borrow_qty − returned_qty − written_off_qty`），否则 4059；
  - 每次归还写一条 `borrow_return_item`（`item_no = GH+日期+4位流水`，记录验收结果与经办人）；
  - `acceptResult=1 完好入库`：调用② `return-good`（可用量 +q），累计 `returned_qty`；
  - `acceptResult=2/3 损坏/缺失`：生成 `loss_order`（`source_type=1`、`approve_status=0`），
    **此时不动库存**，待审批并核销后才扣减在库量；
  - 归还完成后按累计数量重算状态：全部完好 → 3 已归还；含核销 → 5 已核销；有未处理报损 → 4 已报损。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| borrowId | Long | 是 | 借用单主键 |
| returnQty | BigDecimal | 是 | 本次归还数量 > 0 |
| acceptResult | Integer | 是 | 1 完好入库、2 损坏核销、3 缺失核销 |
| remark | String | 条件必填 | 非「完好」时必填，作为报损原因 |

### 响应示例（损坏转报损）

```json
{
  "code": 0, "msg": "success",
  "data": {
    "lossNo": "BS202611160001",
    "result": "已转报损登记，待审批核销",
    "borrow": { "borrowId": 2, "borrowStatus": 4, "returnedQty": 1.000, "conditionCode": 3 }
  }
}
```

---

## 8. 报损审批

- **路径**：`POST /api/v1/borrow/losses/{id}/approve`
- **规则**：仅 `DEPT_HEAD`（本部门）或管理员兜底；仅 `approve_status=0` 可审批（4020）；
  通过必须给处置结论（4062）；结论写 `approve_record(biz_type=3)`。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| result | Integer | 是 | 1 通过、2 驳回 |
| disposeResult | Integer | 通过时必填 | 1 赔偿、2 免赔、3 报废核销 |
| opinion | String | 驳回时必填 | 审批意见 |

---

## 9. 报损核销执行

- **路径**：`POST /api/v1/borrow/losses/{id}/execute`
- **规则**：
  - 仅 `STORE_KEEPER`；必须 `approve_status=1` 且 `executed=0`，否则 4022 / 4023；
  - 以 `executed=0` 为条件更新（`markExecuted`），重复提交不会重复扣减；
  - `source_type=1` 借用报损：调用② `write-off`（在库 −q，流水 `biz_type=7`），
    并累计 `borrow_return.written_off_qty`、重算借用单状态；
  - `source_type=2/3` 盘亏或到期报废：调用② `scrap`（在库 −q、可用 −q）；
  - 处置结论为「赔偿」时返回 `compensateAmount = 报损数量 × 参考单价`，作为推送财务系统的依据
    （课程环境不接真实财务接口）。

### 响应示例

```json
{
  "code": 0, "msg": "success",
  "data": {
    "lossNo": "BS202611160001",
    "disposeResult": 1,
    "compensateAmount": 8.50,
    "loss": { "lossId": 1, "approveStatus": 1, "executed": 1, "execOperatorId": 3 }
  }
}
```

---

## 10. 错误码

| 码 | 含义 |
|---|---|
| 4001 | 可借数量不足 |
| 4003 | 当前角色无权执行该操作 |
| 4004 | 借用单不存在 |
| 4005 | 报损单不存在 |
| 4010 | 当前状态不允许审批 |
| 4011 | 库存服务调用失败 |
| 4012 | 非「待借出」状态不可借出 |
| 4013 | 重复借出 |
| 4014 | 当前状态不可归还 |
| 4015 | 无权查看该借用单 |
| 4016 | 只能审批本部门借用申请 |
| 4020–4023 | 报损已处理 / 非本部门 / 未审批通过 / 重复核销 |
| 4050–4064 | 参数校验类（未选物资、数量非法、缺归还日期、损坏缺说明、缺仓库批次等） |

## 11. 跨子系统依赖

| 方向 | 接口 | 用途 |
|---|---|---|
| ④ → ② | `POST /api/v1/purchase/inventory/borrow-out?reserved=false` | 借出：可用 −q、在库不变 |
| ④ → ② | `POST /api/v1/purchase/inventory/return-good` | 完好归还：可用 +q |
| ④ → ② | `POST /api/v1/purchase/inventory/write-off` | 借用报损核销：在库 −q |
| ④ → ② | `POST /api/v1/purchase/inventory/scrap` | 库内报废核销：在库 −q、可用 −q |
| ④ → 财务（预留） | 赔偿信息推送 | 返回 `compensateAmount`，课程环境不接真实接口 |
