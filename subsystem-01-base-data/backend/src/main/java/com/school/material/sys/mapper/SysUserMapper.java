package com.school.material.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.sys.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 物理删除：工号为唯一键，逻辑删除会导致同一工号无法重新建档。
     */
    @Delete("DELETE FROM mat_sys_user WHERE user_id = #{id}")
    int deletePhysically(@Param("id") Long id);
}
