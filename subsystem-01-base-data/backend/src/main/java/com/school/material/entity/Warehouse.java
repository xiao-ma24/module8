package com.school.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库 / 库房（团队数据字典 warehouse 表）
 */
@Data
@TableName("mat_warehouse")
public class Warehouse {

    @TableId(value = "warehouse_id", type = IdType.AUTO)
    private Long id;

    /** 仓库名称（唯一） */
    private String warehouseName;

    /** 1中心库 2科室库 */
    private Integer warehouseType;

    /** 位置 */
    private String location;

    /** 保管人（sys_user.user_id） */
    private Long keeperId;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
