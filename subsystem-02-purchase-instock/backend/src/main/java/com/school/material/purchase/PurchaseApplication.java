package com.school.material.purchase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 子系统② 采购验收与入库流程 启动类
 * 端口 8082，接口前缀 /api/v1/purchase 与 /api/v1/base
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.school.material.purchase.mapper")
public class PurchaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(PurchaseApplication.class, args);
    }
}
