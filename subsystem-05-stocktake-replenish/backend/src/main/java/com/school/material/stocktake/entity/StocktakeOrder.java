package com.school.material.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 盘点单（stocktake_order，数据字典 §3.5）— 按仓库制定，账面数量在明细中做快照 */
@Data
@TableName("stocktake_order")
public class StocktakeOrder {

    @TableId(type = IdType.AUTO)
    private Long stocktakeId;

    /** 盘点单号：PD + yyyyMMdd + 4 位流水 */
    private String stocktakeNo;

    private Long warehouseId;

    private LocalDate stocktakeDate;

    /** 盘点负责人，关联 sys_user.user_id */
    private Long auditorId;

    /** 0 待盘点 1 进行中 2 已完成（差异已调账） */
    private Integer status;

    private String remark;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer delFlag;
}
