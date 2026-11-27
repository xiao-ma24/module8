# module8 · 物资管理系统统一登录集成

08 组《学校物资管理系统》的 DBA 集成交付版：在组内最终交付的基础上完成统一登录接入，并修复了阻断集成的缺陷。

- 来源：`物资管理-DBA交付-最终`（2026-11-27）
- 状态：**已通过七种身份全流程验收**（接口矩阵、界面菜单、真实写入、数据一致性）
- 认证方式：统一登录 Cookie → 网关核验 → 注入工号/角色 → 部门由各模块按工号查自己的库

## 目录

| 路径 | 内容 |
| --- | --- |
| `subsystem-00-admin` ~ `subsystem-05-stocktake-replenish` | 6 个后端 + 6 个前端源码 |
| `portal/` | 组内开发期门户（可选） |
| `docs/` | 交付文档 + 本次集成说明、验收报告、补丁清单 |
| `docs/sql/` | 建库、初始化、演示数据、增量与自检脚本 |
| `deploy/` | Nginx 配置、本地网关、环境变量模板 |
| `patches/` | 本次集成所做的源码补丁（可重放） |
| `tools/` | 验收脚本（接口矩阵、浏览器逐身份走查） |
| `scripts/` | 组内启停脚本 |

## 快速开始

```bash
# 1. 建库（按顺序）
mysql -h<host> -u<user> -p material_mgmt < docs/sql/full-schema.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/init-data.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/incremental/20261126-02-constraints.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/demo-data.sql     # 可选
mysql -h<host> -u<user> -p material_mgmt < docs/sql/verify.sql

# 2. 配置
cp .env.example .env && vi .env

# 3. 构建
for m in subsystem-0*; do (cd "$m/backend" && mvn -DskipTests package); done
cd subsystem-02-purchase-instock/frontend && npm ci && npm run build

# 4. 启动（六个后端）
bash scripts/start-all.sh

# 5. 网关（本地验证用；生产用 deploy/nginx.conf）
node deploy/gateway.mjs
```

统一认证服务（DBA 提供）默认在 `http://127.0.0.1:18080`，需与本系统同时运行。

## 测试账号

密码均为 `Campus@123`。

| 账号 | 姓名 | 角色 |
| --- | --- | --- |
| `T0005` | 韦森宁 | ADMIN 系统管理员 |
| `T0002` | 李思滢 | PURCHASER 采购员 |
| `T0007` | 韦森聍 | CHECKER 验收员 |
| `T0003` | 刘志豪 | STORE_KEEPER 库管员 |
| `T0004` | 金日勋 | STOCKTAKER 盘点员 |
| `T0006` | 张老师 | APPLICANT 申请人 |
| `T0001` | 王主任 | DEPT_HEAD 部门负责人 |

## 统一登录契约

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| `X-DBA-USER-ID` | 网关注入 | 工号，等于 `mat_sys_user.person_no` |
| `X-DBA-ROLE` | 网关注入 | 角色编码，取值见《身份权限矩阵》 |
| 部门 / 仓库 | **各模块自查** | 按工号查 `mat_sys_user.org_id`，不再从请求头取 |

未登录 401，角色不符 403；`AUTH_ENFORCE=true` 时生效。

## 本次修复的缺陷

| # | 问题 | 影响 |
| --- | --- | --- |
| 1 | 13 处 `@RequireRole({"A","D","M","I","N"})` | 00/01 所有受保护接口对任何角色 403 |
| 2 | 子系统① `context-path` 与拦截器 `/api/**` 不匹配 | 01 鉴权完全失效 |
| 3 | 02 `/profile` 把工号当数字解析 | 登录 A 显示 B |
| 4 | 03/04 未把工号换成数字 `user_id` | 领用/借用建单时 `applicant_id` 为空，写入失败 |
| 5 | 01 发票登记信任请求体里的登记人 | 可伪造他人身份 |
| 6 | 菜单与后端角色不一致 | 采购员点验收、部门负责人点采购申请等 403 |
| 7 | 前端路由依赖 `localStorage.mockUserId` | 统一登录进不去 |

详见 [docs/缺陷与补丁清单.md](docs/缺陷与补丁清单.md)。

## 验收

见 [docs/验收报告.md](docs/验收报告.md)：7 种身份 × 34 个接口 + 9 个越权用例 0 失败；65 个菜单页面 0 个 4xx；领用/借用真实建单身份落库正确。

## 已知限制

1. 6 份前端使用绝对路径 `/assets/`，**只能部署在站点根路径**，不能挂 `/modules/m08/` 子路径；
2. 角色编码按模块定义，同一人在不同模块角色不同时需开多个账号；
3. 组内演示登录（`X-Token` / `handoff`）仍保留，`AUTH_ENFORCE=true` 时不生效；正式上线可删除。

## 文档

- [集成说明](docs/集成说明.md)
- [验收报告](docs/验收报告.md)
- [缺陷与补丁清单](docs/缺陷与补丁清单.md)
- [原交付说明](docs/交付说明.md) / [认证接入说明](docs/delivery/认证接入说明.md) / [身份权限矩阵](docs/delivery/身份权限矩阵.md) / [DBA接手清单](docs/delivery/DBA接手清单.md)
