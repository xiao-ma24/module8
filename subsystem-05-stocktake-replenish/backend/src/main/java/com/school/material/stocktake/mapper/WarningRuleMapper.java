package com.school.material.stocktake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.stocktake.entity.WarningRule;

/**
 * 预警规则：三档规则（安全库存 / 临期 / 到期报废）的增删改查由 MyBatis-Plus 提供；
 * 唯一键 (material_id, warehouse_id, warning_type) 在库层保证同一物资同一仓库同一类型只有一条规则。
 */
public interface WarningRuleMapper extends BaseMapper<WarningRule> {
}
