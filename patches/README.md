# 补丁说明

本目录保存 DBA 集成时对交付源码所做的修改脚本，便于在原始交付包上重放，或在下一版交付上重新应用。

| 脚本 | 作用 |
| --- | --- |
| `patch_final_backend.py` | 修正 13 处角色注解；01 拦截器路径；02 个人中心工号转换；01 登记人取凭证身份；01 读接口开放；菜单与角色对齐（02/03/04/05）；AI 状态开放 |
| `patch_final_front.py` | 02 个人中心工号转换；前端路由守卫改为向 `/api/v1/profile` 确认身份；盘点员不再调用库管接口 |
| `patch_interceptors.py` | 00/03/04 `AuthInterceptor` 按工号补齐数字 `user_id`、姓名、部门 |

使用方式：修改脚本顶部的 `ROOT` 常量指向解压后的交付目录，然后执行：

```bash
python patches/patch_final_backend.py
python patches/patch_final_front.py
python patches/patch_interceptors.py
```

注意：

1. 脚本只做文本替换，执行前建议先备份或使用 Git 管理；
2. 执行后需重新构建 6 个后端与 02 前端；
3. 详细缺陷说明见 [../docs/缺陷与补丁清单.md](../docs/缺陷与补丁清单.md)。
