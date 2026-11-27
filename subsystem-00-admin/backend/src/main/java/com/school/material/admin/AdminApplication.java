package com.school.material.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 子系统⓪ 平台管理端（管理员端）启动类
 * 端口 8080，接口前缀 /api/v1/auth 与 /api/v1/admin
 *
 * 职责：全平台共用主数据（用户、部门、角色、菜单权限、数据字典、系统参数、审计日志）
 * 其他子系统对 user / dept 只读，写操作统一由本端提供。
 */
@SpringBootApplication
@MapperScan("com.school.material.admin.mapper")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
