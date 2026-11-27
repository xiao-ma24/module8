package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;

/** 仓库只读 Mapper（子系统①维护） */
@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
}
