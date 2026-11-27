# 接口文档模板

> 复制本模板到 `docs/api/api-0X.md`（如 api-02.md），按子系统维护。
> 文档是接口的唯一契约：先写文档、群确认、再编码；实现与文档不一致以文档为准并整改实现。

---

# 子系统② 接口文档 —— 采购验收与入库

> 负责人：李思滢 · 端口 8082 · 前缀 `/api/v1/purchase`
> 统一响应：`{ "code": 0, "msg": "success", "data": ... }`，错误码区间 2000–2999

## 变更记录

| 日期 | 修改人 | 变更内容 |
|---|---|---|
| 2026-11-09 | 李思滢 | 初始版 |

## 接口列表

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | POST | /api/v1/purchase/requests | 新建采购申请（⑤补货确认后调用） |
| 2 | GET | /api/v1/purchase/requests/{id} | 查询采购申请详情 |

---

## 1. 新建采购申请

- **路径**：`POST /api/v1/purchase/requests`
- **调用方**：本系统前端；子系统⑤（补货计划确认）
- **触发规则**：预算余额不足返回 code=2001；物料不存在返回 code=2002

### 请求参数（Body JSON）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 申请标题 |
| applicantUserId | String | 是 | 申请人（教师工号，→师资系统） |
| budgetSubjectNo | String | 是 | 预算科目号（→财务系统） |
| items | Array | 是 | 明细列表 |
| items[].materialNo | String | 是 | 物资编码 |
| items[].quantity | BigDecimal | 是 | 数量 |
| items[].price | BigDecimal | 是 | 单价 |

### 请求示例

```json
{
  "title": "11月实验耗材采购",
  "applicantUserId": "T0002",
  "budgetSubjectNo": "B01",
  "items": [
    { "materialNo": "M0001", "quantity": 100, "price": 25.00 }
  ]
}
```

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": { "requestNo": "CG202611090001", "status": 0 }
}
```

### 响应示例（失败：预算不足）

```json
{ "code": 2001, "msg": "预算余额不足，当前余额 500.00", "data": null }
```

---

## 2. 查询采购申请详情

- **路径**：`GET /api/v1/purchase/requests/{id}`
- ...（按同样格式补充）
