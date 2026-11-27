# DBA 接手清单（08 组 · 物资管理系统）

## 一、拿到后按顺序做

```bash
# 1) 解压
tar -xzf 物资管理-DBA交付-最终.tar.gz
cd 物资管理-DBA交付-最终

# 2) 建库（顺序不能变；第 3 步是完整性约束，不能省）
mysql -h<host> -u<user> -p material_mgmt < docs/sql/full-schema.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/init-data.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/incremental/20261126-02-constraints.sql
mysql -h<host> -u<user> -p material_mgmt < docs/sql/demo-data.sql     # 演示数据可选
mysql -h<host> -u<user> -p material_mgmt < docs/sql/verify.sql

# 3) 配置（DB_URL 含 & 必须用单引号）
cp .env.example .env
vim .env      # DB_URL / DB_USER / DB_PASSWORD；联调统一登录时 AUTH_ENFORCE=true

# 4) 启动
bash scripts/start-all.sh

# 5) 验证
curl -H "X-DBA-USER-ID: T0001" -H "X-DBA-ROLE: ADMIN" http://127.0.0.1:8080/api/v1/admin/overview
```

## 二、必须知道的三件事

| 项 | 说明 |
| --- | --- |
| 服务数 | **6 个**：`m08-00`～`m08-05-backend.jar`，端口 `8080`~`8085`（全部读 `PORT_xx` 环境变量） |
| 身份约定 | 只接收 `X-DBA-USER-ID`（工号）+ `X-DBA-ROLE`（角色）。**部门由本组按工号查 `mat_sys_user.org_id`**，不需要 `X-DBA-BUSINESS-ID`（仅过渡期回退） |
| 认证开关 | `AUTH_ENFORCE=false` 用本组登录页（演示）；`AUTH_ENFORCE=true` 只认统一登录，未登录 401、越权 403 |

## 三、本组已就绪

- 6 个后端 jar（Java 17，配置全走环境变量，无明文口令）
- 5 个前端 `dist`（`dist/frontend*`）
- 数据库脚本：42 张表 + 8 张跨组镜像表 + 35 条外键 + 2 个触发器
- 文档：交付说明 / 认证接入 / 身份权限矩阵 / 部署与启动
- 云库实测：6 个服务全部 `Started`，登录、总览、采购、基础数据、领用接口均返回真实数据

## 四、需要 DBA 反馈

1. 最终库名、账号密码；
2. 统一入口地址与 Cookie 名称（改 `auth.identity-*` 即可）；
3. 角色编码映射是否与《身份权限矩阵》一致。
