# 子系统③ 领用申请审批出库流程

> 负责人：**刘志豪** · 后端 `backend/`（Spring Boot，端口 **8083**）· 前端 `frontend/`（Vue3 + Element Plus，端口 **5183**）
> 接口前缀 `/api/v1/requisition` · 错误码区间 **3000–3999** · 接口契约见 [`docs/api/api-03.md`](../../docs/api/api-03.md)

## 1. 业务流程（按 11.16 报告 §2.3「领用申请审批出库」）

```
申请人提交 ──► 系统校验（可用库存 + 部门月度额度） ──► 部门负责人审批（可部分批准）
      │                                                      │
      └── 不满足直接退回                                      ▼
                                                     库管员拣货出库（先到期先出选批）
                                                             │
                                            扣库存 + 写负数流水 + 累加部门已用额度
                                                             ▼
                                                    申请人签收 ──► 单据完成
```

状态机：`0 草稿 → 1 待审批 → 3 已批准 → 5 已出库 → 6 已完成`；`4 已驳回`、`7 已关闭` 为终态。
**仅 3 及以后状态允许出库。**

## 2. 数据表（报告表2-14、表2-15、表2-13）

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `req_order` | 领用单（一单一物资） | `req_no`(LY+日期+4位流水)、`applicant_id`、`dept_id`、`material_id`、`qty_apply`、`qty_approve`、`amount`、`req_status` |
| `out_order` | 出库单（`source_type=1` 领用） | `out_no`(CK+日期+4位流水)、`source_id→req_order`、`warehouse_id`、`keeper_id`、`material_id`、`batch_no`、`out_qty`、`out_time` |
| `approve_record` | 审批记录（`biz_type=1`） | `biz_id`、`approver_id`、`approve_result`(0驳回/1通过)、`approve_time`、`opinion` |
| `stock` / `stock_flow` | 只读 / 由②写入 | 出库写 `biz_type=2` 领用流水，`change_qty` 为负 |

> 建表脚本由仓库统一维护（`docs/sql/`），本子系统**不新建平行表**，也不直连修改 `stock`。

## 3. 对外接口

见 `docs/api/api-03.md`，核心 5 个：

- `POST /api/v1/requisition/orders` 提交领用申请（校验库存与额度）
- `POST /api/v1/requisition/orders/{id}/approve` 审批（可部分批准、驳回须意见）
- `GET  /api/v1/requisition/outbounds/pending` 待出库清单
- `POST /api/v1/requisition/outbounds/confirm` 出库确认（选仓库+批次+实发数量）
- `POST /api/v1/requisition/orders/{id}/receive` 申请人签收

## 4. 角色权限（后端 `@RequireRoles` 强制 + 前端菜单渲染）

| 角色 | 提交申请 | 审批 | 出库 | 签收 | 数据范围 |
|---|---|---|---|---|---|
| `APPLICANT` / `STUDENT` | ✅ | ❌ | ❌ | ✅ | 本人 |
| `DEPT_HEAD` | ✅ | ✅（本部门） | ❌ | ✅ | 本部门 |
| `STORE_KEEPER` | ❌ | ❌ | ✅ | ❌ | 全部 |
| `ADMIN` | ❌（只读） | ❌ | ❌ | ❌ | 全部 |
| `PURCHASER` / `CHECKER` | ❌ | ❌ | ❌ | ❌ | 无（接口 403） |

## 5. 依赖

- **调用**：子系统② 库存服务（`/api/v1/purchase/inventory/issue-direct`）扣减库存；只读 ① 的 `material`/`warehouse`/`stock`
- **被调用**：无
- 跨子系统地址在 `application.yml` 的 `material.purchase-service-url` 配置，**禁止硬编码**

## 6. 本地运行

```bash
# 1) 建库（仓库根目录，DBA 交付版脚本）
mysql -u root -p < docs/sql/full-schema.sql
mysql -u root -p material_mgmt < docs/sql/init-data.sql
# 2) 配置环境变量：cp .env.example .env 并填写 DB_*
# 3) 启动后端（端口 8083）
mvn spring-boot:run
# 4) 启动前端
cd frontend && npm install && npm run dev                 # 端口 5183
# 5) 依赖：子系统②（8082）需先启动，出库才能扣减库存
```

演示账号（初始密码 `123456`）：`T0006` 申请人、`T0001` 部门负责人、`T0003` 库管员、`T0005` 管理员。
