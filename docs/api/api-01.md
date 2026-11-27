# 子系统① 接口文档 —— 物资管理一级数据

> 负责人：韦森宁 · 端口 8081 · 接口前缀 `/api/v1/basedata`
> 统一响应：`{ "code": 0, "msg": "success", "data": ... }`，成功 `code=0`，失败 `code≠0`，错误码区间 **1000–1999**

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-17 | 韦森宁 | 初始版：物资分类、物资档案、仓库、库存查询与库存变更接口 |

## 0. 通用约定

- **全局前缀**：`/api/v1/basedata` 由 `server.servlet.context-path` 统一配置，本文档"路径"均已省略该前缀。
  实际访问地址 = 前缀 + 路径，例：`GET /api/v1/basedata/materials`。
- **统一响应体**：所有接口（含成功、失败、异常）一律返回 `{ "code": ..., "msg": "...", "data": ... }`。
- **分页**：请求参数 `pageNum`（默认 1）、`pageSize`（默认 10，最大 100），响应 `data` 为 `{ "total": 100, "list": [] }`。
- **时间格式**：`LocalDateTime` 序列化为 ISO-8601，如 `2026-11-17T10:00:00`。
- **金额/数量**：`BigDecimal`，金额两位小数、数量三位小数。
- **逻辑删除**：所有业务表 `deleted` 字段（0 正常 / 1 已删除），接口不会返回已删除数据，`deleted` 字段不出现在响应中。

## 1. 错误码

| code | 说明 |
|---|---|
| 0 | 成功 |
| 500 | 系统内部错误 |
| 1001 | 请求参数校验失败（含类型错误、JSON 格式错误、缺少参数） |
| 1002 | 分类不存在 |
| 1003 | 分类编号已存在 |
| 1004 | 该分类下存在子分类，不能删除 |
| 1005 | 该分类下存在物资，不能删除 |
| 1006 | 数据重复（唯一约束冲突） |
| 1010 | 物资不存在 |
| 1011 | 物资编码已存在 |
| 1012 | 物资所属分类不存在 |
| 1020 | 仓库不存在 |
| 1021 | 仓库编号已存在 |
| 1030 | 库存记录不存在 |
| 1031 | 库存不足，不能出库 |
| 1032 | 库存变更方向非法（只允许 IN / OUT） |
| 1033 | 变更数量必须大于 0 |

## 2. 接口列表

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | /categories/tree | 分类树查询 |
| 2 | GET | /categories/{id} | 分类详情 |
| 3 | POST | /categories | 新增分类 |
| 4 | PUT | /categories/{id} | 修改分类 |
| 5 | DELETE | /categories/{id} | 删除分类（逻辑删除） |
| 6 | GET | /materials | 物资分页查询（②③④⑤ 共用） |
| 7 | GET | /materials/{id} | 物资详情（②③④⑤ 共用） |
| 8 | POST | /materials | 新增物资档案 |
| 9 | PUT | /materials/{id} | 修改物资档案 |
| 10 | DELETE | /materials/{id} | 删除物资档案（逻辑删除） |
| 11 | GET | /warehouses | 仓库分页查询 |
| 12 | GET | /warehouses/{id} | 仓库详情 |
| 13 | POST | /warehouses | 新增仓库 |
| 14 | PUT | /warehouses/{id} | 修改仓库 |
| 15 | DELETE | /warehouses/{id} | 删除仓库（逻辑删除） |
| 16 | GET | /stocks | 库存查询（②③④⑤ 共用） |
| 17 | POST | /stocks/change | 库存变更（②③④⑤ 共用，幂等） |

## 3. 通用对象结构

### 3.1 分类对象（MaterialCategory）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| categoryNo | String | 分类编号（唯一） |
| categoryName | String | 分类名称 |
| parentId | Long | 上级分类 id，0 为根节点 |
| sort | Integer | 排序号，升序 |
| status | Integer | 1 启用 / 0 停用 |
| createdBy | String | 创建人 |
| createdAt | LocalDateTime | 创建时间 |
| updatedBy | String | 更新人 |
| updatedAt | LocalDateTime | 更新时间 |

### 3.2 物资对象（MaterialInfo）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| materialNo | String | 物资编码（唯一） |
| materialName | String | 物资名称 |
| spec | String | 规格型号 |
| unit | String | 计量单位 |
| categoryId | Long | 分类 id |
| brand | String | 品牌 |
| referencePrice | BigDecimal | 参考单价 |
| safeStockLower | BigDecimal | 安全库存下限（预警用） |
| safeStockUpper | BigDecimal | 安全库存上限 |
| status | Integer | 1 在用 / 0 停用 |
| remark | String | 备注 |
| createdBy | String | 创建人 |
| createdAt | LocalDateTime | 创建时间 |
| updatedBy | String | 更新人 |
| updatedAt | LocalDateTime | 更新时间 |

### 3.3 仓库对象（Warehouse）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| warehouseNo | String | 仓库编号（唯一） |
| warehouseName | String | 仓库名称 |
| location | String | 位置 |
| keeperUserId | String | 保管人（→师资系统教师工号） |
| status | Integer | 1 启用 / 0 停用 |
| createdBy | String | 创建人 |
| createdAt | LocalDateTime | 创建时间 |
| updatedBy | String | 更新人 |
| updatedAt | LocalDateTime | 更新时间 |

### 3.4 库存对象（Stock）

| 字段 | 类型 | 说明 |
|---|---|---|
| materialId | Long | 物资 id |
| warehouseId | Long | 仓库 id |
| batchNo | String | 批次号，联合主键之一；发票登记入库固定 `DEFAULT` |
| qtyOnHand | BigDecimal | 在库总量 |
| qtyAvailable | BigDecimal | 可领量 |
| qtyLocked | BigDecimal | 预占量 |
| inboundDate | LocalDate | 入库日期 |
| materialCode | String | 物资编号（关联 material） |
| materialName | String | 物资名称（关联 material） |
| warehouseName | String | 仓库名称（关联 warehouse） |
| safeStock | BigDecimal | 安全库存阈值（关联 material） |

### 3.5 分页对象（PageResult）

```json
{ "total": 100, "list": [] }
```

---

## 4. 物资分类接口

### 4.1 分类树查询

- **路径**：`GET /categories/tree`
- **调用方**：本系统前端
- **触发规则**：无。`status` 为空时返回全部状态的分类

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | Integer | 否 | 1 启用 / 0 停用，不传返回全部 |

#### 请求示例

```
GET /api/v1/basedata/categories/tree?status=1
```

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "categoryNo": "C01",
      "categoryName": "办公耗材",
      "parentId": 0,
      "sort": 0,
      "status": 1,
      "children": [
        {
          "id": 3,
          "categoryNo": "C0101",
          "categoryName": "纸张/墨盒",
          "parentId": 1,
          "sort": 0,
          "status": 1,
          "children": []
        }
      ]
    }
  ]
}
```

> 树节点在分类对象基础上增加 `children`（数组），其余字段同 §3.1。

---

### 4.2 分类详情

- **路径**：`GET /categories/{id}`
- **调用方**：本系统前端
- **触发规则**：分类不存在返回 1002

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 分类 id |

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 3,
    "categoryNo": "C0101",
    "categoryName": "纸张/墨盒",
    "parentId": 1,
    "sort": 0,
    "status": 1,
    "createdBy": "T0001",
    "createdAt": "2026-11-17T10:00:00",
    "updatedBy": "T0001",
    "updatedAt": "2026-11-17T10:00:00"
  }
}
```

#### 响应示例（失败：分类不存在）

```json
{ "code": 1002, "msg": "分类不存在", "data": null }
```

---

### 4.3 新增分类

- **路径**：`POST /categories`
- **调用方**：本系统前端
- **触发规则**：`categoryNo` 重复返回 1003；`parentId` 指向的分类不存在返回 1002

#### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| categoryNo | String | 是 | 分类编号，最长 32 |
| categoryName | String | 是 | 分类名称，最长 64 |
| parentId | Long | 否 | 上级分类 id，默认 0（根节点） |
| sort | Integer | 否 | 排序号，默认 0 |
| status | Integer | 否 | 1 启用 / 0 停用，默认 1 |

#### 请求示例

```json
{ "categoryNo": "C0102", "categoryName": "办公文具", "parentId": 1, "sort": 1, "status": 1 }
```

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": 5 }
```

> `data` 为新增分类的主键 id。

#### 响应示例（失败：编号重复）

```json
{ "code": 1003, "msg": "分类编号已存在", "data": null }
```

---

### 4.4 修改分类

- **路径**：`PUT /categories/{id}`
- **调用方**：本系统前端
- **触发规则**：分类不存在 1002；`categoryNo` 与其他分类重复 1003；`parentId` 不存在 1002；`parentId` 指向自身或自身子分类 1001

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 分类 id |

#### 请求参数（Body JSON）

字段同 §4.3，`categoryNo`、`categoryName` 必填。

#### 请求示例

```json
{ "categoryNo": "C0102", "categoryName": "办公文具耗材", "parentId": 1, "sort": 2, "status": 1 }
```

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

#### 响应示例（失败：上级分类非法）

```json
{ "code": 1001, "msg": "上级分类不能是自身的子分类", "data": null }
```

---

### 4.5 删除分类

- **路径**：`DELETE /categories/{id}`
- **调用方**：本系统前端
- **触发规则**：分类不存在 1002；存在子分类 1004；分类下存在物资 1005。删除为逻辑删除

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 分类 id |

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

#### 响应示例（失败：存在子分类）

```json
{ "code": 1004, "msg": "该分类下存在子分类，不能删除", "data": null }
```

---

## 5. 物资档案接口

### 5.1 物资分页查询

- **路径**：`GET /materials`
- **调用方**：本系统前端；子系统②（采购选料）、③（领用选料）、④（借用选料）、⑤（盘点/预警）
- **触发规则**：无

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10，最大 100 |
| materialName | String | 否 | 物资名称，模糊匹配 |
| categoryId | Long | 否 | 分类 id，精确匹配 |
| status | Integer | 否 | 1 在用 / 0 停用 |

#### 请求示例

```
GET /api/v1/basedata/materials?pageNum=1&pageSize=10&materialName=A4&categoryId=3
```

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 1,
    "list": [
      {
        "id": 1,
        "materialNo": "M0001",
        "materialName": "A4复印纸",
        "spec": "70g/500张",
        "unit": "包",
        "categoryId": 3,
        "brand": "得力",
        "referencePrice": 25.00,
        "safeStockLower": 100.000,
        "safeStockUpper": 1000.000,
        "status": 1,
        "remark": null,
        "createdBy": "T0001",
        "createdAt": "2026-11-17T10:00:00",
        "updatedBy": "T0001",
        "updatedAt": "2026-11-17T10:00:00"
      }
    ]
  }
}
```

---

### 5.2 物资详情

- **路径**：`GET /materials/{id}`
- **调用方**：本系统前端；②③④⑤（跨子系统公共契约）
- **触发规则**：物资不存在返回 1010

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 物资 id |

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "materialNo": "M0001",
    "materialName": "A4复印纸",
    "spec": "70g/500张",
    "unit": "包",
    "categoryId": 3,
    "brand": "得力",
    "referencePrice": 25.00,
    "safeStockLower": 100.000,
    "safeStockUpper": 1000.000,
    "status": 1,
    "remark": null,
    "createdBy": "T0001",
    "createdAt": "2026-11-17T10:00:00",
    "updatedBy": "T0001",
    "updatedAt": "2026-11-17T10:00:00"
  }
}
```

#### 响应示例（失败：物资不存在）

```json
{ "code": 1010, "msg": "物资不存在", "data": null }
```

---

### 5.3 新增物资档案

- **路径**：`POST /materials`
- **调用方**：本系统前端
- **触发规则**：`materialNo` 重复返回 1011；`categoryId` 不存在返回 1012

#### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| materialNo | String | 是 | 物资编码，最长 32，唯一 |
| materialName | String | 是 | 物资名称，最长 128 |
| spec | String | 否 | 规格型号，最长 128 |
| unit | String | 是 | 计量单位，最长 16 |
| categoryId | Long | 是 | 分类 id |
| brand | String | 否 | 品牌，最长 64 |
| referencePrice | BigDecimal | 否 | 参考单价，≥ 0 |
| safeStockLower | BigDecimal | 否 | 安全库存下限，≥ 0 |
| safeStockUpper | BigDecimal | 否 | 安全库存上限，≥ 0 |
| status | Integer | 否 | 1 在用 / 0 停用，默认 1 |
| remark | String | 否 | 备注，最长 255 |

#### 请求示例

```json
{
  "materialNo": "M0003",
  "materialName": "订书机",
  "spec": "12号",
  "unit": "个",
  "categoryId": 3,
  "brand": "得力",
  "referencePrice": 18.50,
  "safeStockLower": 20,
  "safeStockUpper": 200,
  "status": 1,
  "remark": "办公文具"
}
```

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": 3 }
```

#### 响应示例（失败：物资编码重复）

```json
{ "code": 1011, "msg": "物资编码已存在", "data": null }
```

---

### 5.4 修改物资档案

- **路径**：`PUT /materials/{id}`
- **调用方**：本系统前端
- **触发规则**：物资不存在 1010；`materialNo` 与其他物资重复 1011；`categoryId` 不存在 1012

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 物资 id |

#### 请求参数（Body JSON）

字段同 §5.3，`materialNo`、`materialName`、`unit`、`categoryId` 必填。

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

#### 响应示例（失败：分类不存在）

```json
{ "code": 1012, "msg": "物资所属分类不存在", "data": null }
```

---

### 5.5 删除物资档案

- **路径**：`DELETE /materials/{id}`
- **调用方**：本系统前端
- **触发规则**：物资不存在 1010。删除为逻辑删除

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 物资 id |

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

#### 响应示例（失败：物资不存在）

```json
{ "code": 1010, "msg": "物资不存在", "data": null }
```

---

## 6. 仓库接口

### 6.1 仓库分页查询

- **路径**：`GET /warehouses`
- **调用方**：本系统前端；②（入库选仓库）、③（出库选仓库）、⑤（盘点选仓库）
- **触发规则**：无

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10，最大 100 |
| warehouseName | String | 否 | 仓库名称，模糊匹配 |
| status | Integer | 否 | 1 启用 / 0 停用 |

#### 请求示例

```
GET /api/v1/basedata/warehouses?pageNum=1&pageSize=10&warehouseName=耗材库
```

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 1,
    "list": [
      {
        "id": 1,
        "warehouseNo": "W01",
        "warehouseName": "耗材库",
        "location": "行政楼B1",
        "keeperUserId": "T0001",
        "status": 1,
        "createdBy": "T0001",
        "createdAt": "2026-11-17T10:00:00",
        "updatedBy": "T0001",
        "updatedAt": "2026-11-17T10:00:00"
      }
    ]
  }
}
```

---

### 6.2 仓库详情

- **路径**：`GET /warehouses/{id}`
- **调用方**：本系统前端
- **触发规则**：仓库不存在返回 1020

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 仓库 id |

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "warehouseNo": "W01",
    "warehouseName": "耗材库",
    "location": "行政楼B1",
    "keeperUserId": "T0001",
    "status": 1,
    "createdBy": "T0001",
    "createdAt": "2026-11-17T10:00:00",
    "updatedBy": "T0001",
    "updatedAt": "2026-11-17T10:00:00"
  }
}
```

#### 响应示例（失败：仓库不存在）

```json
{ "code": 1020, "msg": "仓库不存在", "data": null }
```

---

### 6.3 新增仓库

- **路径**：`POST /warehouses`
- **调用方**：本系统前端
- **触发规则**：`warehouseNo` 重复返回 1021

#### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| warehouseNo | String | 是 | 仓库编号，最长 32，唯一 |
| warehouseName | String | 是 | 仓库名称，最长 64 |
| location | String | 否 | 位置，最长 255 |
| keeperUserId | String | 否 | 保管人教师工号（→师资系统） |
| status | Integer | 否 | 1 启用 / 0 停用，默认 1 |

#### 请求示例

```json
{ "warehouseNo": "W03", "warehouseName": "体育器材库", "location": "体育馆1层", "keeperUserId": "T0004", "status": 1 }
```

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": 3 }
```

#### 响应示例（失败：编号重复）

```json
{ "code": 1021, "msg": "仓库编号已存在", "data": null }
```

---

### 6.4 修改仓库

- **路径**：`PUT /warehouses/{id}`
- **调用方**：本系统前端
- **触发规则**：仓库不存在 1020；`warehouseNo` 与其他仓库重复 1021

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 仓库 id |

#### 请求参数（Body JSON）

字段同 §6.3，`warehouseNo`、`warehouseName` 必填。

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

#### 响应示例（失败：仓库不存在）

```json
{ "code": 1020, "msg": "仓库不存在", "data": null }
```

---

### 6.5 删除仓库

- **路径**：`DELETE /warehouses/{id}`
- **调用方**：本系统前端
- **触发规则**：仓库不存在 1020。删除为逻辑删除

#### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 仓库 id |

#### 响应示例（成功）

```json
{ "code": 0, "msg": "success", "data": null }
```

---

## 7. 库存接口（共享核心表，仅本子系统写）

### 7.1 库存查询

- **路径**：`GET /stocks`
- **调用方**：本系统前端；③（申请时校验库存余量）、⑤（预警查询库存）
- **触发规则**：无。`materialId`、`warehouseId` 均不传时返回全部库存

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| materialId | Long | 否 | 物资 id |
| warehouseId | Long | 否 | 仓库 id |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

#### 请求示例

```
GET /api/v1/basedata/stocks?materialId=1&warehouseId=1
```

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 1,
    "list": [
      {
        "materialId": 1,
        "warehouseId": 1,
        "batchNo": "INIT-01-001-A",
        "qtyOnHand": 120.000,
        "qtyAvailable": 120.000,
        "qtyLocked": 0.000,
        "inboundDate": "2026-11-17",
        "materialCode": "M0001",
        "materialName": "A4复印纸",
        "warehouseName": "耗材库",
        "safeStock": 50.000
      }
    ]
  }
}
```

---

### 7.2 库存变更（幂等）

- **路径**：`POST /stocks/change`
- **调用方**：②（验收入库 IN）、③（领用出库 OUT）、④（借出/归还/报损）、⑤（盘点差异调整）
- **触发规则**：
  - `direction` 非 IN/OUT 返回 1032；`num` ≤ 0 返回 1033
  - 物资不存在 1010、仓库不存在 1020（首次建库存时校验）
  - **OUT 时库存不足返回 1031，库存数量不允许为负**（扣减在同一事务内完成）
  - **幂等**：以 `biz_type + biz_id + biz_item_id` 为幂等键，落在 `stock_flow`（唯一索引 `uk_flow_biz_op`）。
    同一来源单据的同一物资行重复请求不重复执行，返回当前库存并置 `duplicated=true`（仍返回 code=0）；
    同一单据包含多条物资明细时，每条明细各算一次，互不影响。
  - **入库批次**：`stock` 主键为 `(material_id, warehouse_id, batch_no)`，前端不暴露批次，入库缺省自动写 `DEFAULT`。

#### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| materialId | Long | 是 | 物资 id |
| warehouseId | Long | 是 | 仓库 id |
| direction | String | 是 | `IN` 入库 / `OUT` 出库（不区分大小写） |
| num | BigDecimal | 是 | 变更数量，必须 > 0；入库同时增加 `qty_on_hand`/`qty_available`，出库同时扣减 |
| sourceNo | String | 是 | 来源单据号，最长 32，用于计算幂等键 `biz_id`（如 RK202611170001） |

#### 请求示例

```json
{
  "materialId": 1,
  "warehouseId": 1,
  "direction": "OUT",
  "num": 10,
  "sourceNo": "LY202611170001"
}
```

#### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "materialId": 1,
    "warehouseId": 1,
    "direction": "OUT",
    "changeQty": 10,
    "qtyOnHand": 110.000,
    "qtyAvailable": 110.000,
    "sourceNo": "LY202611170001",
    "duplicated": false
  }
}
```

#### 响应示例（失败：库存不足）

```json
{ "code": 1031, "msg": "库存不足，当前库存 5.000", "data": null }
```

#### 响应示例（失败：方向非法）

```json
{ "code": 1032, "msg": "库存变更方向只能是 IN 或 OUT", "data": null }
```
