package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 仓库表（warehouse）— 本人负责 */
@Data
@TableName("mat_warehouse")
public class Warehouse {
    @TableId(type = IdType.AUTO)
    private Long warehouseId;
    private String warehouseName;
    private Integer warehouseType; // 1实体仓库 2虚拟个人保管仓
    private String location;
    private Long keeperId;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String keeperName;
    @TableField(exist = false)
    private Long stockCount;
}
