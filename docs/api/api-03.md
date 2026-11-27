# 子系统③ 接口文档 —— 领用申请审批出库

> 负责人：刘志豪 · 端口 **8083** · 前缀 `/api/v1/requisition` · 错误码区间 **3000–3999**
> 统一响应：`{ "code": 0, "msg": "success", "data": ... }`；`code=0` 成功，非 0 失败
> 数据表：`req_order`、`out_order`、`approve_record`（biz_type=1）；库存写入调用子系统②

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-16 | 刘志豪 | 初始版：按 11.16 报告数据字典（`req_order` / `out_order` / `approve_record`）实现领用全流程 |

## 0. 角色与数据范围（权限口径）

| 角色 | 可做什么 | 数据范围 |
|---|---|---|
| `APPLICANT` 教职工/申请人 | 提交领用申请、撤回、签收 | 仅本人单据 |
| `STUDENT` 学生 | 同 APPLICANT | 仅本人单据 |
| `DEPT_HEAD` 部门负责人 | 提交申请 + 审批本部门申请 | 本部门单据 |
| `STORE_KEEPER` 仓库管理员 | 查看全部、拣货出库 | 全部单据 |
| `ADMIN` 系统管理员 | **只读**查看全部（不参与业务动作） | 全部单据 |
| `PURCHASER` / `CHECKER` | 与领用业务无关，接口直接拒绝（403） | — |

实现方式：登录后后端签发 `X-Token`，`AuthInterceptor` 校验令牌并用 `@RequireRoles` 做方法级角色控制；
数据范围在 Service 层按角色拼 SQL 条件，越权访问单条单据返回 3014。

## 1. 状态机（req_status）

| 值 | 含义 | 允许的下一动作 |
|---|---|---|
| 0 | 草稿 | 提交（→1） |
| 1 | 待审批 | 审批通过（→3）、驳回（→4）、撤回（→7） |
| 2 | 审批中 | 审批（多级审批预留） |
| 3 | 已批准 | **出库（→5）**，仅库管员 |
| 4 | 已驳回 | 终态 |
| 5 | 已出库 | 申请人签收（→6） |
| 6 | 已完成 | 终态 |
| 7 | 已关闭 | 终态 |

> 规则：**仅 3 及以后状态允许出库**；每次状态变更同时记录操作人与时间。

## 2. 接口列表

| # | 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|---|
| 1 | POST | `/auth/login` | 登录，签发令牌 | 公开 |
| 2 | POST | `/auth/logout` | 退出 | 登录即可 |
| 3 | GET | `/auth/me` | 当前登录用户 | 登录即可 |
| 4 | GET | `/dashboard` | 工作台统计 | 登录即可 |
| 5 | GET | `/stats` | 部门/物资领用统计 | DEPT_HEAD、STORE_KEEPER |
| 6 | GET | `/meta/materials` | 可选物资（仅低值易耗品） | 登录即可 |
| 7 | GET | `/meta/warehouses` | 实体仓库列表 | 登录即可 |
| 8 | GET | `/meta/stocks` | 库存总览（按批次） | 登录即可 |
| 9 | GET | `/meta/batches` | 某物资可选批次（先到期先出）+ 可领量 | 登录即可 |
| 10 | GET | `/orders` | 领用单列表 | APPLICANT/STUDENT/DEPT_HEAD/STORE_KEEPER |
| 11 | GET | `/orders/pending-approve` | 待我审批 | DEPT_HEAD |
| 12 | GET | `/orders/{id}` | 领用单详情 | 同上（受数据范围约束） |
| 13 | GET | `/orders/{id}/logs` | 审批留痕 | 同上 |
| 14 | POST | `/orders` | 提交领用申请 | APPLICANT/STUDENT/DEPT_HEAD |
| 15 | POST | `/orders/{id}/submit` | 草稿提交 | 申请人本人 |
| 16 | POST | `/orders/{id}/approve` | 审批（可部分批准） | DEPT_HEAD |
| 17 | POST | `/orders/{id}/withdraw` | 撤回 | 申请人本人 |
| 18 | POST | `/orders/{id}/receive` | 签收 | 申请人本人 |
| 19 | GET | `/outbounds` | 出库单列表 | 登录即可（受数据范围） |
| 20 | GET | `/outbounds/pending` | 待出库单据 | STORE_KEEPER |
| 21 | POST | `/outbounds/confirm` | 出库确认 | STORE_KEEPER |

---

## 3. 登录

- **路径**：`POST /api/v1/requisition/auth/login`
- **规则**：账号必须在 `sys_user` 中存在且 `status=1`；口令与平台管理端同一加盐 SHA-256 口径（初始 `123456`）。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userNo | String | 是 | 工号/学号 |
| password | String | 是 | 口令 |

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "token": "tk_9f1c...",
    "user": {
      "userId": 6, "userNo": "T0006", "userName": "张老师",
      "roleCode": "APPLICANT", "roleName": "教职工/申请人",
      "deptId": 2, "deptName": "实验室管理处"
    }
  }
}
```

### 响应示例（失败）

```json
{ "code": 3006, "msg": "工号或密码错误", "data": null }
```

---

## 4. 提交领用申请

- **路径**：`POST /api/v1/requisition/orders`
- **调用方**：本系统前端
- **提交时一次性校验**（不满足直接退回，不进入审批）：
  1. 物资存在且为**低值易耗品**（`material_type=1`），否则 3053；
  2. 可用库存 ≥ 申请数量（实体仓、未过期批次汇总），否则 **3001**；
  3. 部门当月额度（material_dept_quota）剩余 ≥ 预估金额（申请数量 × 参考单价），否则 **3002**。

### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| materialId | Long | 是 | 物资主键（一单一物资） |
| qtyApply | BigDecimal | 是 | 申请数量，大于 0 |
| submit | Boolean | 否 | 是否直接提交审批，默认 `true`；`false` 存为草稿 |

### 请求示例

```json
{ "materialId": 1, "qtyApply": 10, "submit": true }
```

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reqId": 1, "reqNo": "LY202611160001", "applicantId": 6, "deptId": 2,
    "materialId": 1, "qtyApply": 10.000, "qtyApprove": null,
    "amount": 250.00, "reqStatus": 1, "availableQty": 300.000
  }
}
```

### 响应示例（失败：库存不足）

```json
{ "code": 3001, "msg": "可用库存不足：当前可领 5 包，申请 10", "data": null }
```

### 响应示例（失败：额度不足）

```json
{ "code": 3002, "msg": "部门月度额度不足：剩余 120.00 元，本单需要 250.00 元", "data": null }
```

---

## 5. 审批领用申请

- **路径**：`POST /api/v1/requisition/orders/{id}/approve`
- **调用方**：本系统前端（部门负责人）
- **触发规则**：
  - 仅 `DEPT_HEAD` 且与申请人**同部门**可审批，否则 3003 / 3015；
  - 只能审批 `req_status ∈ {1,2}` 的单据，否则 3010；
  - 驳回必须填 `opinion`，否则 3056；
  - 核准数量 ≤ 申请数量，且再次校验可用库存与部门额度；
  - **管控物资**（`material.is_controlled=1`）通过时必须填写审批意见（加签说明），否则 3058；
  - 审批结论同时写入 `approve_record`（`biz_type=1`，`approve_result` 1 通过 / 0 驳回）。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| result | Integer | 是 | 1 通过、0 驳回 |
| qtyApprove | BigDecimal | 否 | 核准数量，缺省等于申请数量（部分批准时填写） |
| opinion | String | 否 | 审批意见；驳回时必填 |

### 请求示例

```json
{ "result": 1, "qtyApprove": 8, "opinion": "按实际用量核准 8 包" }
```

### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

### 响应示例（失败：跨部门）

```json
{ "code": 3015, "msg": "只能审批本部门的领用申请", "data": null }
```

---

## 6. 出库确认

- **路径**：`POST /api/v1/requisition/outbounds/confirm`
- **调用方**：本系统前端（库管员）
- **触发规则**：
  - 仅 `STORE_KEEPER`，否则 3003；
  - 领用单必须为 `req_status=3 已批准`，否则 3012；
  - 同一领用单只能出库一次（`out_order` 唯一），否则 3013；
  - `outQty` ≤ 核准数量（部分出库允许），否则 3060；
  - 生成 `out_order`（`source_type=1`、`out_no=CK+日期+4位流水`、记录 `batch_no`）；
  - **调用子系统② `POST /api/v1/purchase/inventory/issue-direct`** 扣减库存：
    在库总量 −q、可用量 −q，并写 `stock_flow`（`biz_type=2` 领用，`change_qty` 为负数）；
  - 累加 `dept.used_amount`（超出月度额度则整单回滚 3002）；
  - 领用单置 `req_status=5 已出库`。

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reqId | Long | 是 | 领用单主键 |
| warehouseId | Long | 是 | 出库仓库（仅实体仓 `warehouse_type=1`） |
| batchNo | String | 是 | 出库批次（按先到期先出选择） |
| outQty | BigDecimal | 否 | 实发数量，缺省等于核准数量，可小于核准数量 |

### 请求示例

```json
{ "reqId": 1, "warehouseId": 1, "batchNo": "INIT-01-001-A", "outQty": 8 }
```

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "outId": 1, "outNo": "CK202611160001", "sourceType": 1, "sourceId": 1,
    "warehouseId": 1, "keeperId": 3, "materialId": 1, "batchNo": "INIT-01-001-A",
    "outQty": 8.000, "outTime": "2026-11-16T15:49:06"
  }
}
```

### 响应示例（失败：库存服务不可用）

```json
{ "code": 3011, "msg": "库存服务不可用，请确认子系统② 已在 8082 启动", "data": null }
```

---

## 7. 错误码

| 码 | 含义 |
|---|---|
| 3001 | 可用库存不足 |
| 3002 | 部门月度额度不足 |
| 3003 | 当前角色无权执行该操作 |
| 3004 | 领用单不存在 |
| 3005 | 未输入工号或密码 |
| 3006 | 工号或密码错误 |
| 3007 | 账号已停用 |
| 3008 | 只能操作本人单据 |
| 3009 | 当前状态不允许该操作 |
| 3010 | 当前状态不允许审批 |
| 3011 | 库存服务调用失败 |
| 3012 | 非「已批准」状态不可出库 |
| 3013 | 重复出库 |
| 3014 | 无权查看该领用单 |
| 3015 | 只能审批本部门单据 |
| 3050–3060 | 参数校验类（未选物资、数量非法、未选仓库/批次、管控物资缺加签意见等） |

## 8. 跨子系统依赖

| 方向 | 接口 | 用途 |
|---|---|---|
| ③ → ② | `POST /api/v1/purchase/inventory/issue-direct` | 领用出库扣减库存（唯一写入口，幂等键 = `bizId=outId`） |
| ③ ← ① | 只读 `material` / `warehouse` / `stock` 表 | 物资、仓库、库存查询 |
| ① | `dept.annual_quota` / `used_amount` | 部门额度校验与出库累加 |

> 库存规则说明：`req_order` 无仓库/批次字段，审批阶段无法定位具体批次，因此**领用不预先预占**；
> 出库时由库管员按先到期先出选定批次，一次性扣减「在库总量 + 可用量」，语义等价于
> 「审批通过并预占」+「领用出库消耗预占」两步，且不会出现预占泄漏。
