package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 供应商表（supplier）— 本人负责，在班级 3 字段基础上扩充联系/税号/评级 */
@Data
@TableName("mat_supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long supplierId;
    private String supplierName;
    private Integer supplierStatus;  // 1合作中 0停止合作
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String taxNo;
    private Integer starLevel;       // 1-5星
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;
}
