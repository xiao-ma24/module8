# 子系统④ 借用归还报损流程

> 负责人：**刘志豪** · 后端 `backend/`（Spring Boot，端口 **8084**）· 前端 `frontend/`（Vue3 + Element Plus，端口 **5184**）
> 接口前缀 `/api/v1/borrow` · 错误码区间 **4000–4999** · 接口契约见 [`docs/api/api-04.md`](../../docs/api/api-04.md)

## 1. 业务流程（按 11.16 报告 §2.3「借用归还报损」）

```
借用人提交 ──► 部门负责人审批 ──► 库管员借出登记（在库不变、可用 −q）
                                        │
                                        ▼
                                   借用单：在借
                                        │
              到期未还 ──► 逾期清单（催还）  │
                                        ▼
                              库管员归还验收（可分次）
                          ┌─────────────┴─────────────┐
                    完好入库                     损坏 / 缺失
                 可用量 +q                   生成报损单（待审批）
                 累计 returned_qty                   │
                                          部门负责人审批（赔偿/免赔/报废）
                                                      │
                                          库管员核销执行：在库 −q、写7号流水
                                                      ▼
                                              借用单：已核销（闭环）
```

状态机：`0 待审批 → 1 待借出 → 2 在借 → 3 已归还 / 4 已报损 → 5 已核销`。
**逾期不落库**，按 `due_date < 今天 && 状态=在借` 实时计算。

## 2. 数据表（报告表2-16、表2-17）

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `borrow_return` | 借用归还（一单一物资，借用+归还同表） | `borrow_no`(JY+日期+4位流水)、`borrower_id`、`material_id`、`warehouse_id`、`batch_no`、`borrow_qty`、`returned_qty`、`written_off_qty`、`due_date`、`condition_code`、`borrow_status` |
| `borrow_return_item` | 分次归还明细 | `item_no`(GH+日期+4位流水)、`return_qty`、`accept_result`(1完好入库/2损坏核销/3缺失核销)、`keeper_id` |
| `loss_order` | 报损单 | `loss_no`(BS+日期+4位流水)、`source_type`(1借用报损/2盘亏/3到期报废)、`loss_qty`、`reason`、`approve_status`、`dispose_result`、`executed` |
| `out_order` | 借出出库单（`source_type=2`） | `source_id→borrow_return`、`batch_no`、`out_qty` |
| `approve_record` | 审批记录（`biz_type=2` 借用、`3` 报损） | `approve_result`(0驳回/1通过) |

约束：`returned_qty + written_off_qty <= borrow_qty`（库表 CHECK 约束保证）。

## 3. 库存口径（借用与领用的本质区别）

| 动作 | 在库总量 | 可用量 | 库存流水 |
|---|---|---|---|
| 借出 | **不变** | −q | `biz_type=3` 借用（`change_qty=0`） |
| 完好归还 | 不变 | +q | `biz_type=4` 归还（`change_qty=0`） |
| 借用报损核销 | −q | 不变 | `biz_type=7` 报废核销 |
| 库内报废核销 | −q | −q | `biz_type=7` |

> 「账面有量但实际不可用」的问题正是通过「借出只减可用」消除的；库存写入统一由子系统② 完成。

## 4. 对外接口

见 `docs/api/api-04.md`，核心 6 个：

- `POST /api/v1/borrow/orders` 提交借用申请（仅低值耐用品）
- `POST /api/v1/borrow/orders/{id}/approve` 借用审批
- `POST /api/v1/borrow/lends` 借出登记（选仓库+批次）
- `POST /api/v1/borrow/returns` 归还验收（可分次；损坏/缺失自动转报损）
- `POST /api/v1/borrow/losses/{id}/approve` 报损审批（含处置结论）
- `POST /api/v1/borrow/losses/{id}/execute` 报损核销（幂等，重复提交不重复扣减）

## 5. 角色权限（后端 `@RequireRoles` 强制 + 前端菜单渲染）

| 角色 | 申请 | 借用审批 | 借出 | 归还验收 | 报损审批 | 核销执行 | 数据范围 |
|---|---|---|---|---|---|---|---|
| `APPLICANT` / `STUDENT` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | 本人 |
| `DEPT_HEAD` | ✅ | ✅（本部门） | ❌ | ❌ | ✅（本部门） | ❌ | 本部门 |
| `STORE_KEEPER` | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | 全部 |
| `ADMIN` | ❌（只读） | ❌ | ❌ | ❌ | ❌ | ❌ | 全部 |
| `PURCHASER` / `CHECKER` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 无（接口 403） |

## 6. 依赖

- **调用**：子系统② 库存服务（`borrow-out` / `return-good` / `write-off` / `scrap`）；只读 ① 的 `material`/`warehouse`/`stock`
- **预留**：赔偿金额（`dispose_result=1` 时按「报损数量 × 参考单价」计算并返回）推送财务系统，课程环境不接真实接口
- 跨子系统地址：`application.yml` 的 `material.purchase-service-url`

## 7. 本地运行

```bash
# 建库与启动方式同子系统③（同一套 docs/sql/ 脚本，同一数据库 material_mgmt）
mvn spring-boot:run -Dspring-boot.run.profiles=local     # 端口 8084
cd frontend && npm install && npm run dev                 # 端口 5184
# 依赖：子系统②（8082）需先启动，借出/归还/核销才可变更库存
```

演示账号（初始密码 `123456`）：`T0006` 借用人、`T0001` 部门负责人、`T0003` 库管员、`T0005` 管理员。
