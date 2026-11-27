package com.school.material.requisition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 子系统③ 领用申请审批出库流程（刘志豪）
 * 端口 8083 · 接口前缀 /api/v1/requisition · 错误码区间 3000-3999
 */
@EnableScheduling
@SpringBootApplication
public class RequisitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequisitionApplication.class, args);
    }
}
