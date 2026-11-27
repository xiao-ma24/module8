package com.school.material.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.sys.entity.Dept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 物理删除：部门编号为唯一键，逻辑删除会导致同一编号无法重新建档。
     */
    @Delete("DELETE FROM mat_dept WHERE org_id = #{id}")
    int deletePhysically(@Param("id") String id);
}
