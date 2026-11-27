package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物资目录（material，只读，子系统①维护）。
 * material_type：1 低值易耗品（走领用）、2 低值耐用品（走借用）。
 * is_controlled：1 管控物资，需加签。
 */
@Data
@TableName("mat_material")
public class Material {

    @TableId(value = "material_id", type = IdType.AUTO)
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String categoryCode;
    private Integer materialType;
    private String unit;
    private BigDecimal refPrice;
    private Integer isControlled;
    private Integer serviceLifeYears;
    private Integer shelfLifeDays;
    private BigDecimal safeStock;
    @TableLogic
    private Integer delFlag;
}
