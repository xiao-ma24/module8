package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 入库明细表（inbound_item）— 含批次号/质保期/二维码标签 */
@Data
@TableName("inbound_item")
public class InboundItem {
    @TableId(type = IdType.AUTO)
    private Long iiId;
    private Long inId;
    private Long materialId;
    private String batchNo;
    private LocalDate produceDate;
    private LocalDate expireDate;
    private BigDecimal inQty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String qrCode;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private Integer isControlled;
}
