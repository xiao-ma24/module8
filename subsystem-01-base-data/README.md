# 子系统① 基础数据与审批

> 负责人：**韦森聍** · 后端 `backend/` · 前端 `frontend/` · 端口 8081 · 接口前缀 `/api/v1/basedatadata`

## 1. 业务流程

1. 维护**物资目录**：编码、名称、分类（字典 `material_category`）、单位、参考价、物资类型（1 低值易耗品 / 2 低值耐用品）、限用年限、保质期天数、安全库存
2. 维护**供应商**档案与合作状态
3. 维护**仓库**：实体仓库与虚拟个人保管仓（科研经费自购）、默认库管员
4. 维护**统一审批记录** `approve_record`，供领用/借用/报损/采购/报废各模块共用
5. 提供物资、供应商、仓库、库存的**查询**接口给②③④⑤

## 2. 数据表

见 `docs/数据字典.md` §3.1：`material`、`warehouse`、`stock`，以及 §3.2 的 `supplier`、`approve_record`

> 物资分类不单独建表，统一由平台管理端 `sys_dict_type=material_category` 下的 `sys_dict_item` 维护，`material.category_code` 只存字典编码。

## 3. 库存写入口径（重要）

`stock` / `stock_flow` 的写入统一走子系统② 的 `InventoryService`（合格入库、预占、释放、领用出库、借出、归还、报损核销、盘盈、盘亏），**①不得自行更新余额**，只负责目录、仓库与查询接口；非②模块确实需要变更库存时，调用②的库存变更接口。

## 4. 对外接口（被②③④⑤调用）

- `GET /api/v1/basedata/materials` · `GET /api/v1/basedata/materials/{id}` 物资目录查询
- `GET /api/v1/basedata/suppliers` 供应商查询
- `GET /api/v1/basedata/warehouses` 仓库查询
- `GET /api/v1/basedata/stocks` 库存查询（按物资/仓库/批次）

## 5. 依赖与外部系统

- 被依赖：②③④⑤全部子系统
- 外部系统：师资系统（仓库保管人姓名，模拟表 `sys_teacher`）；部门与人员主数据只读 `sys_user` / `dept`

## 6. 开发要求

- Spring Boot 3.x + MySQL 8.0，库名 `material_mgmt`，JDK 17
- 表结构变更先更新 `docs/数据字典.md` 与 `docs/sql/full-schema.sql`（历史增量已归档 `docs/sql/legacy/`）
- `application.yml` 提交通用配置，本地差异放 `application-local.yml`（不入库）
