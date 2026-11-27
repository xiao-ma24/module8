# 子系统⓪ 平台管理端（负责人：李思滢）

学校物资管理大系统的**平台级管理端**，横跨全部 5 个子系统，负责"人、组织、权限、字典、参数、审计"六件事。

## 1. 为什么需要它

| 问题 | 没有平台管理端 | 有平台管理端 |
|---|---|---|
| 用户从哪来 | 每个子系统各建一套账号，同一人多个密码 | `user` 一张主档，全平台一套账号 |
| 权限怎么控 | 代码里写死角色判断 | 角色 → 菜单 → 按钮三级授权，改配置即生效 |
| 下拉框枚举 | 各子系统各写一份状态值 | 统一数据字典，`sys_dict_item` 一处维护 |
| 业务开关 | 改代码重新发版 | `sys_config` 参数热改（初始密码、免审批额度、AI 开关） |
| 出问题怎么查 | 翻日志文件 | 操作日志 + 登录日志，按人/模块/IP 检索并导出 |

## 2. 目录结构

```
subsystem-00-admin/
├── backend/                     Spring Boot 3.2.5 · 端口 8080
│   └── src/main/java/com/school/material/admin/
│       ├── auth/                登录、Token 存储、拦截器、当前用户上下文
│       ├── common/              统一返回 Result、异常、SHA-256
│       ├── config/              MyBatis-Plus 分页、CORS、拦截器注册
│       ├── controller/          Auth / User / Role / Dept / Dict / Config / Log / Overview / Profile
│       ├── entity/ mapper/      9 张表对应实体与 Mapper
│       └── service/             业务服务（含审计写入、个人中心聚合）
└── frontend/                    Vue 3 + Vite 5 · 端口 5180
    └── src/
        ├── api/                  axios 封装，自动注入 X-Token
        ├── layout/              侧栏菜单（来自后端返回的权限菜单）+ 顶栏用户下拉
        ├── views/               登录 / 总览 / 用户 / 角色 / 部门 / 字典 / 参数 / 操作日志 / 登录日志 / 个人中心
        ├── composables/         usePager 前端分页
        └── utils/               导出 Excel、打印 PDF、读取 Excel
```

## 3. 启动

前置：MySQL 已在跑、已执行建库脚本。

```bash
# 1. 建表 + 种子数据（幂等，可重复执行）
mysql -h 127.0.0.1 -P 3306 -u root -p < docs/sql/schema-00-admin.sql

# 2. 后端
cd subsystem-00-admin/backend
mvn -DskipTests package
java -jar target/admin-platform-1.0.0.jar

# 3. 前端
cd subsystem-00-admin/frontend
npm install
npm run dev            # http://localhost:5180
```

演示账号（初始密码均为 `123456`）：

| 工号 | 姓名 | 角色 | 说明 |
|---|---|---|---|
| T0005 | 李思滢2 | ADMIN 系统管理员 | 平台管理端唯一演示账号 |

> 建议从统一登录入口 **https://localhost:5179** 进入：只输入账号密码，系统按角色自动跳转，无需选择身份。采购验收与入库系统的模拟账号为「李思滢1（T0002 · 采购员）」，两个子系统共用统一身份认证。

## 4. 功能清单

| 模块 | 能力 |
|---|---|
| 数据总览 | 账号/组织/字典/参数统计；物资、低库存、待审批、在途、待入库跨子系统指标；近 7 天登录趋势、角色分布、最近操作、平台健康检查 |
| 用户管理 | 账号全生命周期：提交身份信息 → 工号/手机号/邮箱查重 → 建立账号（待启用、默认无业务权限）→ 分配部门与多角色 → 启用并发送首次登录提示 → 变更 / 停用并撤权；支持 Excel 导入、导出 Excel/PDF、身份源同步（模拟师资/学籍） |
| 角色权限 | 角色卡片、数据范围、在岗人数、菜单权限树勾选（含半选）、增删改（ADMIN 不可删、有用户不可删） |
| 部门管理 | 部门编码、负责人、成员数、月度预算与执行率、增删改（有成员不可删） |
| 数据字典 | 左侧类型 + 右侧字典项双栏；标签颜色预览；类型与字典项增删改 |
| 系统参数 | 按分组动态 Tab、内置参数保护、按类型校验改值（字符串/数字/布尔） |
| 操作日志 | 统计卡 + 成功/失败 Tab + 模块筛选 + 导出 Excel/PDF |
| 登录日志 | 登录总数/成功/失败/涉及账号统计 + 设备筛选 + 导出 |
| 个人中心 | 账号名片、操作指标、我的待办（未分配角色/缺手机号/登录异常/停用账号）、操作时间线、账号与安全 |

通用交互与子系统②保持一致：主内容板等高、工具栏搜索、前端分页、Excel/PDF 导出、右下角 AI 运维助手、顶栏仅"首页 + 用户头像下拉"。

## 5. 接口一览

统一前缀 `/api/v1`，除登录外均需请求头 `X-Token`；返回 `{ code, msg, data }`，`code=0` 成功、`401` 登录失效、`1000-1999` 平台端业务错误。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 登录，返回 token / 用户 / 菜单树 / 权限码 |
| GET | `/auth/me` | 当前登录人信息与菜单 |
| POST | `/auth/logout` | 退出并销毁 token |
| GET | `/admin/overview` | 总览数据 |
| GET/POST/PUT/DELETE | `/admin/users` | 用户列表 / 新增 / 修改 / 删除（删除会被拒绝，引导使用停用撤权） |
| POST | `/admin/users/{id}/status` | 兼容接口：status=1 启用，0 停用并撤权（带 reason） |
| POST | `/admin/users/{id}/activate` | 启用（校验部门/角色/手机号，写审计） |
| POST | `/admin/users/{id}/disable` | 停用并撤销全部角色（不删账号，带 reason） |
| GET | `/admin/users/check` | 工号/手机号/邮箱查重，返回冲突说明 |
| POST | `/admin/users/sync` | 模拟从师资/学籍系统同步身份状态 |
| POST | `/auth/change-pwd` | 修改本人密码（首次登录强制改密） |
| POST | `/admin/users/{id}/reset-pwd` | 重置密码为 123456 |
| POST | `/admin/users/import` | 批量导入 |
| GET/POST/PUT/DELETE | `/admin/roles` | 角色维护 |
| GET | `/admin/roles/menus` | 菜单树 |
| POST | `/admin/roles/{id}/menus` | 保存角色菜单权限 |
| GET/POST/PUT/DELETE | `/admin/depts` | 部门维护 |
| GET/POST/DELETE | `/admin/dicts/types` | 字典类型 |
| GET/POST/DELETE | `/admin/dicts/items` | 字典项 |
| GET | `/admin/dicts/all` | 全量字典（供各子系统缓存） |
| GET/POST/PUT/DELETE | `/admin/configs` | 系统参数 |
| PUT | `/admin/configs/{id}/value` | 仅改参数值 |
| GET | `/admin/logs` | 操作日志 |
| GET | `/admin/login-logs` | 登录日志 |
| GET | `/admin/log-stats` | 日志统计 |
| GET | `/admin/profile` | 个人中心聚合数据 |

## 6. 与其他子系统的边界（重要）

- `user`、`dept` 两张表**只有本子系统可写**，其他子系统只读
- 其他子系统需要用户列表时，调用 `GET /api/v1/admin/users?keyword=`，不要自己建账号表
- 字典通过 `GET /api/v1/admin/dicts/all` 一次性拉取并本地缓存，避免每次下拉都请求
- 业务表的 `created_by` / `applicant_user_id` 等存 `user_id`，展示时按需联查 `user_name`，不做冗余写入以外的双向同步

## 7. 安全说明

- 密码使用 SHA-256 存储，不保存明文；初始密码由参数 `sys.user.initPassword` 决定
- 连续 5 次密码错误锁定账号（参数 `sys.login.maxRetry`）
- Token 保存在服务端内存，默认 480 分钟过期（`auth.token-expire-minutes`），重启服务后需重新登录
- 所有增删改均写 `sys_audit_log`，包含操作人、模块、动作、对象、IP、结果
