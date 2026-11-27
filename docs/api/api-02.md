# 个人科研经费发票登记 · 接口文档（api-02）

> 归属：个人散单登记 · 数据库：`material_mgmt` · 全局前缀：`/api/v1/base`
> 统一响应体：`{ "code": 0, "msg": "success", "data": ... }`
> 说明：本接口只做**个人科研经费发票的实物入库登记**，不涉及任何财务经费表，
> 也**不是**子系统②的集体采购验收单（`inbound_order` / `inbound_item`），两者互不替代。

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-18 | — | 新增个人发票登记接口 `POST /purchase/register`；明确与集体采购验收单的边界 |
| 2026-11-18 | — | 登记改为"待验收"不直接入库；新增 `GET /purchase/records` 查询、`POST /purchase/audit` 审核接口 |
| 2026-11-18 | — | 新增 `audit_remark` 审核意见字段与 `GET /purchase/pending` 待审核列表；审核请求参数改为 `auditRemark` |

---

## 1. 个人发票登记

**路径**：`POST /purchase/register`
**调用方**：个人科研经费采购登记页
**功能**：保存个人发票登记记录，审核状态置为 `0-待验收`，**不变更库存**；
管理员审核通过后（见 §3）才在同一事务内执行入库。

### 请求参数（JSON Body）

| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| invoiceNo | String(32) | 是 | 发票号码，审核通过时作为库存变更的幂等来源单号 |
| materialId | Long | 是 | 物资 id |
| warehouseId | Long | 是 | 入库仓库 id |
| qtyApply | BigDecimal | 是 | 入库数量，必须 > 0 |
| supplierName | String(128) | 否 | 供应商名称 |
| registrarUserId | String(32) | 是 | 登记人 id（→ 师资系统） |
| remark | String(255) | 否 | 备注 |

### 请求示例

```json
{
  "invoiceNo": "FP20261118001",
  "materialId": 1,
  "warehouseId": 1,
  "qtyApply": 10,
  "supplierName": "晨光文具",
  "registrarUserId": "T0001",
  "remark": "初次登记"
}
```

### 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 4,
    "invoiceNo": "FP20261118001",
    "materialId": 1,
    "warehouseId": 1,
    "qtyApply": 10.000,
    "supplierName": "晨光文具",
    "registrarUserId": "T0001",
    "registerTime": "2026-11-18T16:20:11",
    "qtyOnHand": 315.000,
    "auditStatus": 0,
    "duplicated": false
  }
}
```

> `qtyOnHand` 为登记时刻该物资在该仓库的在库总量（跨批次合计），**本次登记不会改变它**。

### 幂等响应（同一发票 + 同一物资 + 同一仓库重复提交）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 4,
    "invoiceNo": "FP20261118001",
    "materialId": 1,
    "warehouseId": 1,
    "qtyApply": 10.000,
    "supplierName": "晨光文具",
    "registrarUserId": "T0001",
    "registerTime": "2026-11-18T16:20:11",
    "qtyOnHand": 315.000,
    "auditStatus": 0,
    "duplicated": true
  }
}
```

> `duplicated=true` 表示该发票的该物资行已登记过，本次**未重复创建记录**，返回已有记录及其当前审核状态。

### 失败响应

```json
{ "code": 1001, "msg": "发票号码不能为空", "data": null }
```

```json
{ "code": 1010, "msg": "物资不存在", "data": null }
```

```json
{ "code": 1020, "msg": "仓库不存在", "data": null }
```

### 业务规则

1. **登记不入库**：只写 `purchase_record`，`audit_status=0`（待验收）。
2. **登记即校验**：物资（`1010`）与仓库（`1020`）不存在时直接拒绝，避免产生无法入库的无效记录。
3. **幂等**：唯一键 `uk_invoice_material(invoice_no, material_id, warehouse_id)`，重复提交返回 `duplicated=true`，不重复创建记录。
4. **参数校验失败**统一返回 `1001`。

---

## 2. 查询登记记录

**路径**：`GET /purchase/records`
**调用方**：个人登记记录列表 / 管理员待验收列表
**功能**：按登记人、审核状态查询发票登记记录，按登记时间倒序。

### 请求参数（Query）

| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| registrarUserId | String(32) | 否 | 登记人工号；不传则查询全部（鉴权模块接入后由当前登录人自动填充） |
| auditStatus | Integer | 否 | 0-待验收，1-已入库，2-已驳回；不传则查询全部 |

### 请求示例

```
GET /api/v1/base/purchase/records?registrarUserId=T0001&auditStatus=0
```

### 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 4,
      "invoiceNo": "FP20261118001",
      "materialId": 1,
      "warehouseId": 1,
      "qtyApply": 10.000,
      "supplierName": "晨光文具",
      "registrarUserId": "T0001",
      "registerTime": "2026-11-18T16:20:11",
      "remark": "初次登记",
      "auditStatus": 0,
      "auditStatusText": "待验收",
      "auditRemark": null
    }
  ]
}
```

### 失败响应

```json
{ "code": 500, "msg": "系统异常", "data": null }
```

---

## 3. 审核登记记录

**路径**：`POST /purchase/audit`
**调用方**：管理员验收入库页
**功能**：审核待验收的登记记录。通过（`1`）时在**同一事务**内调用库存变更
（`direction=IN`、`qtyApply=登记数量`、`source_no=发票号码`）增加库存；驳回（`2`）只更新状态与审核意见，不碰库存。

### 请求参数（JSON Body）

| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 登记记录 id |
| auditStatus | Integer | 是 | 1-通过（入库），2-驳回 |
| auditorUserId | String(32) | 否 | 审核人工号（→ 师资系统） |
| auditRemark | String(255) | 否 | 审核意见，写入 `audit_remark`，不影响登记备注 `remark` |

### 请求示例

```json
{
  "id": 4,
  "auditStatus": 1,
  "auditorUserId": "T0002",
  "auditRemark": "实物已验收"
}
```

### 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 4,
    "invoiceNo": "FP20261118001",
    "materialId": 1,
    "warehouseId": 1,
    "qtyApply": 10.000,
    "supplierName": "晨光文具",
    "registrarUserId": "T0001",
    "registerTime": "2026-11-18T16:20:11",
    "remark": "初次登记",
    "auditStatus": 1,
    "auditStatusText": "已入库",
    "auditRemark": "实物已验收"
  }
}
```

### 失败响应

```json
{ "code": 1040, "msg": "登记记录不存在", "data": null }
```

```json
{ "code": 1041, "msg": "该记录已处理，不能重复审核", "data": null }
```

```json
{ "code": 1042, "msg": "审核状态只能为 1（通过入库）或 2（驳回）", "data": null }
```

```json
{ "code": 1010, "msg": "物资不存在", "data": null }
```

### 业务规则

1. **事务一致性**：审核通过时状态更新与库存变更在同一事务内；库存变更失败（物资/仓库不存在等）时审核状态一并回滚，记录仍为待验收。
2. **只能审核待验收记录**：`audit_status != 0` 时返回 `1041`。
3. **库存只经子系统①变更**：内部调用库存变更逻辑，不直接改 `stock` 表。
4. **库存幂等**：审核通过以 `source_no=发票号码` 走子系统①的幂等表，重复调用不会重复增加库存。
5. **审核意见独立保存**：请求中的 `auditRemark` 只写入 `audit_remark`，不会覆盖登记时填写的 `remark`。

---

## 4. 待审核列表

**路径**：`GET /purchase/pending`
**调用方**：管理员验收入库页（待办队列）
**功能**：查询所有 `audit_status=0`（待验收）的发票登记记录，按登记时间正序返回。

### 请求参数

无。

### 请求示例

```
GET /api/v1/base/purchase/pending
```

### 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 6,
      "invoiceNo": "FP20261118012",
      "materialId": 1,
      "warehouseId": 1,
      "qtyApply": 8.000,
      "supplierName": "晨光文具",
      "registrarUserId": "T0001",
      "registerTime": "2026-11-18T16:43:35",
      "remark": "实物待验收",
      "auditStatus": 0,
      "auditStatusText": "待验收",
      "auditRemark": null
    }
  ]
}
```

### 失败响应

```json
{ "code": 500, "msg": "系统异常", "data": null }
```

### 业务规则

1. 只返回状态为 `0-待验收` 的记录，已入库/已驳回的记录不出现在队列中。
2. 按 `register_time` 正序，先进先审。
3. 审核完成后再次调用该接口，该记录会自动消失。

## 错误码

| 错误码 | 说明 |
|---|---|
| 1001 | 参数校验失败 |
| 1010 | 物资不存在 |
| 1020 | 仓库不存在 |
| 1031 | 库存不足（审核入库不涉及） |
| 1040 | 登记记录不存在 |
| 1041 | 登记记录已处理，不能重复审核 |
| 1042 | 审核状态非法 |
