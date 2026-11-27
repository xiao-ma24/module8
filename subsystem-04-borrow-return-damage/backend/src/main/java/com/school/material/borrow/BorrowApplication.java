package com.school.material.borrow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 子系统④ 借用归还报损流程（刘志豪）
 * 端口 8084 · 接口前缀 /api/v1/borrow · 错误码区间 4000-4999
 */
@EnableScheduling
@SpringBootApplication
public class BorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BorrowApplication.class, args);
    }
}