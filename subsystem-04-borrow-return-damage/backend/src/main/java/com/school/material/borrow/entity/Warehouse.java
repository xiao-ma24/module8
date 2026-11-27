package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 仓库（warehouse，只读）。warehouse_type：1 实体仓库、2 虚拟个人保管仓（不参与公共领用）。 */
@Data
@TableName("mat_warehouse")
public class Warehouse {

    @TableId(value = "warehouse_id", type = IdType.AUTO)
    private Long warehouseId;
    private String warehouseName;
    private Integer warehouseType;
    private String location;
    private Long keeperId;
    @TableLogic
    private Integer delFlag;
}
