package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 平台用户只读 Mapper（子系统⓪维护） */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
