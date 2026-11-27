# 数据库脚本说明（DBA 交付版）

本目录是交付给 DBA 的**唯一权威脚本集**。

## 执行顺序

```bash
# 1. 结构（建库 + 39 张表）
mysql -h<host> -P<port> -u<user> -p < full-schema.sql

# 2. 基线数据（部门/角色/用户/菜单/字典/参数/仓库/供应商/分类）
mysql -h<host> -P<port> -u<user> -p material_mgmt < init-data.sql

# 3.（可选，仅联调测试环境）业务演示数据，让各页面列表/待办有内容
mysql -h<host> -P<port> -u<user> -p material_mgmt < demo-data.sql

# 4. 自检（查看 actual 与 expect 是否一致）
mysql -h<host> -P<port> -u<user> -p material_mgmt < verify.sql
```

## 文件说明

| 文件 | 作用 | 说明 |
| --- | --- | --- |
| `full-schema.sql` | 全量结构 | 表名已与代码对齐为 `mat_*`；含 DROP TABLE，**禁止在生产库直接执行** |
| `init-data.sql` | 初始化数据 | 全部 `INSERT IGNORE`，可重复执行；不含业务流水 |
| `demo-data.sql` | 业务演示数据 | 327 行（物资/库存/采购/领用/借用/盘点/审批），`INSERT IGNORE` 可重复执行；**仅联调测试环境，禁止导入生产** |
| `verify.sql` | 导入自检 | 检查表数量、旧表名残留、核心表、角色/菜单/字典 |
| `incremental/` | 增量升级脚本 | 升级既有库时按日期顺序执行：`20261125-01-budget-dept-quota.sql`（预算表加 `dept_id`+部门年度预算，部门额度改由财务预算派生）、`20261125-02-teacher-no-source.sql`（师资系统工号来源、部门编码对齐、名单补齐）；新库无需执行 |
| `init_template.sql` | 外部系统模拟表 | 仅课程联调使用（师资/学生/预算），DBA 环境不需要执行 |
| `legacy/` | 历史迁移脚本 | 旧表名时代的开发过程脚本，**新环境请勿执行**，仅作追溯参考 |

## 重要说明

1. **表名与列名**：代码实体统一映射 `mat_*`（如 `mat_material`、`mat_stock`、`mat_sys_user`），
   并已完成 2026-11-24 数据字典列名对齐（`person_no` / `org_id` / `user_status` / `dept_status` /
   `role_status` / `approved_at` 等）。仓库历史脚本（`legacy/` 内）仍是旧表名/旧列名，已废弃，请勿混用。
2. **字符集**：`utf8mb4` / `utf8mb4_0900_ai_ci`，要求 MySQL 8.0+；MySQL 5.7 请将排序规则替换为 `utf8mb4_general_ci`。
3. **账号口令**：`init-data.sql` 中的密码为 PBKDF2 哈希（演示账号默认口令见《部署与启动说明》），首次上线请强制修改。
4. **导入验证**：已在临时空库 `material_mgmt_verify` 完整执行
   `full-schema → init-data → demo-data → verify`，结果 39 表 / 0 旧表残留 / 基线数据齐全，
   验证后临时库已删除。生产导入前请仍在测试库演练一次。

## 执行顺序（务必按此顺序）

```bash
1. full-schema.sql                        # 建库 + 42 张表
2. init-data.sql                          # 基础字典/角色/用户/仓库
3. incremental/20261126-02-constraints.sql # 8 张跨组镜像表 + 35 条外键 + 2 个触发器
4. demo-data.sql                          # 演示数据（可选，仅演示环境）
5. verify.sql                             # 校验表数/外键/数据量
```

> 第 3 步不能省：`full-schema.sql` 不含外键与触发器，跳过会导致完整性约束缺失。
