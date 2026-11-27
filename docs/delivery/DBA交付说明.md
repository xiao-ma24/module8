# 学校物资管理系统 · DBA 交付说明

> 本文是交付给 DBA 的**唯一入口文档**，按顺序执行即可完成部署。
> 细节可查阅同目录：`部署与启动说明.md`（部署细节）、`认证接入说明.md`（统一登录）、`身份权限矩阵.md`（角色权限）。

---

## 0. 一页速览

### 交付物

| 类别 | 位置 | 说明 |
| --- | --- | --- |
| 源码 | Gitee `school-material-dba` | 6 个子系统 + 6 份前端源码 |
| 后端 jar | `dist/jars/m00-backend.jar` ~ `m05-backend.jar` | 6 个可执行 jar，JDK 17 |
| 前端产物 | `dist/frontend`、`dist/frontend-m00/m01/m03/m04/m05` | 6 份已构建静态文件 |
| 建库脚本 | `docs/sql/full-schema.sql`、`init-data.sql`、`demo-data.sql`、`verify.sql` | 39 张表 + 基线数据 + 演示数据 |
| 启停脚本 | `scripts/build-all.sh`、`start-all.sh`、`stop-all.sh` | 打包与启停 |
| 配置模板 | `.env.example` | 全部环境变量说明，无真实口令 |

### 端口与接口前缀

| 服务 | 后端端口 | 前端端口 | 接口前缀 |
| --- | --- | --- | --- |
| 00 管理端 | 8080 | 5180 | `/api/v1/auth`、`/api/v1/admin/**` |
| 01 基础数据 | 8081 | 5173 | `/api/v1/basedata/**` |
| 02 采购入库 | 8082 | 5182 | `/api/v1/purchase/**`、`/api/v1/base/**`、`/api/v1/profile` |
| 03 领用出库 | 8083 | 5183 | `/api/v1/requisition/**` |
| 04 借用报损 | 8084 | 5184 | `/api/v1/borrow/**` |
| 05 盘点补货 | 8085 | 5185 | `/api/v1/stocktake/**` |

### 演示账号（密码均为 `123456`）

| 账号 | 姓名 | 角色 |
| --- | --- | --- |
| T0005 | 韦森宁 | ADMIN 系统管理员 |
| T0002 | 李思滢 | PURCHASER 采购员 |
| T0001 | 王主任 | DEPT_HEAD 部门负责人 |
| T0003 | 刘志豪 | STORE_KEEPER 库管员 |
| T0004 | 金日勋 | STOCKTAKER 盘点员 |
| T0006 | 张老师 | APPLICANT 申请人 |
| T0007 | 韦森聍 | CHECKER 验收人（到货验收，与库管员岗位分离） |

---

## 1. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **17**（必须） | Spring Boot 3.2.5，JDK 11/8 无法运行 |
| MySQL | 8.0+ | utf8mb4；5.7 需把脚本中 `utf8mb4_0900_ai_ci` 换成 `utf8mb4_general_ci` |
| Nginx | 任意稳定版 | 托管 6 份前端静态文件 |
| 操作系统 | Linux / macOS | jar 已内置 Tomcat，无需额外容器 |

---

## 2. 第一步：建库

脚本**已去除库名硬编码**，用哪个库名由命令行决定。

```bash
# 1) 建库 + 39 张表（库名 material_mgmt 可在脚本内替换）
mysql -u root -p < docs/sql/full-schema.sql

# 2) 基线数据：部门/角色/用户/菜单/字典/参数/仓库/供应商/分类
mysql -u root -p material_mgmt < docs/sql/init-data.sql

# 3) 演示业务数据（可选；仅联调与演示环境，生产禁止导入）
mysql -u root -p material_mgmt < docs/sql/demo-data.sql

# 4) 导入自检
mysql -u root -p material_mgmt < docs/sql/verify.sql
```

预期结果：

| 项 | 预期 |
| --- | --- |
| 表数量 | 39 |
| 用户 | 8 |
| 物资 / 库存 | 12 / 20（未导入 demo-data 时为 0） |
| 采购申请 / 订单 / 入库单 | 27 / 13 / 12 |
| 领用单 / 借用单 / 盘点单 | 5 / 2 / 6 |
| 旧表名残留（`sys_user`、`material` 等） | 0 |
| 核心表缺失 | 0 |

> **注意**：`full-schema.sql` 含 `DROP TABLE IF EXISTS`，重复执行会清空同名表，生产库请先备份。
> `init-data.sql`、`demo-data.sql` 全部 `INSERT IGNORE`，可安全重复执行。
> `verify.sql` 中「演示数据行数」一项，未导入 demo-data 时显示 0 属正常。

---

## 3. 第二步：配置

```bash
cp .env.example .env
vi .env
```

必填项：

| 变量 | 说明 | 示例 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | 数据库地址与端口 | `127.0.0.1` / `3306` |
| `DB_NAME` | 库名 | `material_mgmt` |
| `DB_USER` / `DB_PASS` | 数据库账号 | `root` / 由 DBA 提供 |
| `AUTH_ENFORCE` | 是否强制统一登录鉴权 | 联调初期 `false`，接入后 `true` |

其余可选项（端口、AI 地址、跨域来源）见 `.env.example` 注释。`.env` 已在 `.gitignore`，不会被提交。

---

## 4. 第三步：启动后端

```bash
bash scripts/start-all.sh      # 依次启动 8080~8085
bash scripts/stop-all.sh       # 停止
```

启动成功标志（每个服务约 9 秒）：

```bash
for p in 8080 8081 8082 8083 8084 8085; do
  curl -s -o /dev/null -w "$p:%{http_code}\n" http://127.0.0.1:$p/
done
```

冒烟验证：

```bash
# 物资列表（应返回 12 条）
curl "http://127.0.0.1:8081/api/v1/basedata/materials?page=1&size=2"

# 登录（应返回 token 与用户信息）
curl -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userNo":"T0002","password":"123456"}'
```

> 日志在 `run/logs/`；忘记 `src` 参数时可直接 `java -jar dist/jars/m02-backend.jar --server.port=8082` 手工启动单个服务。

---

## 5. 第四步：部署前端

6 份产物互相独立，推荐**同域名不同端口**：

| 产物 | 监听端口 |
| --- | --- |
| `dist/frontend` | 5182（统一大系统，主入口） |
| `dist/frontend-m00` | 5180 |
| `dist/frontend-m01` | 5173 |
| `dist/frontend-m03` | 5183 |
| `dist/frontend-m04` | 5184 |
| `dist/frontend-m05` | 5185 |

每个站点一个 `server` 块，`root` 指向对应目录，并 `include` 同一份 `/api/v1/**` 反代配置（完整示例见 `部署与启动说明.md` §4.5）。

关键点：

- 前端用 SPA history 路由，必须 `try_files $uri $uri/ /index.html;`
- 产物使用绝对路径 `/assets/`，**必须部署在站点根路径**，不能挂到 `/m03/` 子路径（如必须子路径，需重新构建，见 §4.5 方案 B）
- Cookie 在同一 host 的不同端口间共享，DBA 登录一次即可全站识别

---

## 6. 第五步：接入统一登录

系统已预留标准接口，DBA 只需替换实现类，业务代码零改动。

| 项 | 位置 / 默认值 |
| --- | --- |
| 当前用户入口 | 各模块 `security/CurrentUserService`（接口）与 `CurrentUserServiceImpl`（实现） |
| 当前用户模型 | `CurrentUser(String userId, String role, String businessId)` |
| 身份 Cookie 名 | `auth.identity-cookie-name`，默认 `DBA_TOKEN` |
| 身份头（备用） | `X-DBA-USER-ID`（工号）、`X-DBA-ROLE`（角色）；部门由后端查 `mat_sys_user.org_id` |
| 强制鉴权开关 | `AUTH_ENFORCE=false`（默认，兼容演示登录）/ `true`（生产） |

接入步骤：

1. 在 `CurrentUserServiceImpl` 中解析 DBA 下发的 Cookie 或令牌，返回 `CurrentUser`
2. 角色映射到 8 个编码之一：`ADMIN`、`PURCHASER`、`CHECKER`、`STORE_KEEPER`、`DEPT_HEAD`、`APPLICANT`、`STUDENT`、`STOCKTAKER`
3. 设置 `AUTH_ENFORCE=true` 重启
4. 未登录访问任意业务接口应返回 **401**，角色不符返回 **403**

> 详细字段约定、各模块登录文件位置、401/403 行为见 `认证接入说明.md`。

---

## 7. 验收清单

- [ ] `full-schema.sql` + `init-data.sql` 在空库执行成功，`verify.sql` 各项命中预期
- [ ] 6 个 jar 全部启动，8080~8085 均可访问
- [ ] 登录接口返回 token 与用户信息
- [ ] 物资、库存、采购申请等接口返回数据（导入 demo-data 后）
- [ ] 6 份前端首页可打开，静态资源无 404
- [ ] 前端 `/api` 请求能经 Nginx 到达后端
- [ ] 接入统一登录后，未登录 401、越权 403（`AUTH_ENFORCE=true`）

---

## 8. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 启动报 `Table 'xxx.mat_sys_user' doesn't exist` | 用了旧版建库脚本 | 用 `full-schema.sql` 重新建库 |
| 所有业务接口 401 | `AUTH_ENFORCE=true` 但未接通统一登录 | 先设 `false`，或完成 §6 |
| 所有接口 500 且日志报 `Unknown column` | 库结构与代码版本不一致 | 重建库并核对 `verify.sql` |
| 前端白屏、`/assets/*.js` 404 | 部署在子路径 | 改为根路径部署（§5） |
| 前端接口 502 | Nginx 未代理或后端未启动 | 检查反代配置与 8080~8085 |
| 5173 / 5180 等站点打开是 404 | `start-all.sh` 只启动后端，前端站点要么用 Nginx（方案 A），要么用 `node scripts/serve-frontends.mjs`（方案 C）；用 Nginx 时检查 `root` 与 `try_files ... /index.html` | 见《部署与启动说明》§4.5 |
| 登录报 `Invalid CORS request`（403），Safari 显示 `The string did not match the expected pattern.` | 浏览器地址栏端口不在 `CORS_ALLOWED_ORIGINS` 内。POST 会带 `Origin` 头，即使走同源反代后端仍会校验 | 把**浏览器实际访问的来源（含端口）**写进 `.env` 的 `CORS_ALLOWED_ORIGINS`，重启后端；本机可先用 `http://localhost:*,http://127.0.0.1:*` |
| 端口被占用 | 已有实例在跑 | `bash scripts/stop-all.sh` 或改 `PORT_0X` |
| 中文乱码 | 库/连接非 utf8mb4 | 建库用脚本默认字符集，连接串保持 `characterEncoding=UTF-8` |

---

## 9. 相关文档

| 文档 | 用途 |
| --- | --- |
| `docs/delivery/部署与启动说明.md` | Nginx 完整配置、部署拓扑、端口约定 |
| `docs/delivery/认证接入说明.md` | 统一登录契约、Cookie、401/403、演示期兼容项 |
| `docs/delivery/身份权限矩阵.md` | 8 个角色 × 各模块接口权限 |
| `docs/sql/README.md` | SQL 脚本执行顺序与字段说明 |
| `docs/接口规范与合并指南.md` | 接口前缀、响应体、跨服务调用约定 |
