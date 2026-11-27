package com.school.material.stocktake;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 子系统⑤ 盘点预警与补货计划流程 启动类
 * 端口 8085，接口前缀 /api/v1/stocktake
 * 定时任务：每日预警扫描（合并期由 material-boot 统一调度）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.school.material.stocktake.mapper")
public class StocktakeApplication {
    public static void main(String[] args) {
        SpringApplication.run(StocktakeApplication.class, args);
    }
}
