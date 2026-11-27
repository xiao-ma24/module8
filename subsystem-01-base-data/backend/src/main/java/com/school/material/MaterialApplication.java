package com.school.material;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.school.material.mapper", "com.school.material.purchase.mapper",
        "com.school.material.requisition.mapper", "com.school.material.sys.mapper"})
public class MaterialApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialApplication.class, args);
    }
}
