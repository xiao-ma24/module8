package com.school.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.dto.MaterialVO;
import com.school.material.entity.Material;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 物资分页查询：关联平台字典项 sys_dict_item 返回分类名称，字典类型由入参指定
     */
    @Select("""
            <script>
            SELECT m.material_id AS id,
                   m.material_code AS material_no,
                   m.material_name AS material_name,
                   m.category_code AS category_code,
                   d.item_label AS category_name,
                   m.material_type AS material_type,
                   m.unit AS unit,
                   m.ref_price AS reference_price,
                   m.is_controlled AS is_controlled,
                   m.service_life_years AS service_life_years,
                   m.shelf_life_days AS shelf_life_days,
                   m.safe_stock AS safe_stock,
                   m.created_at AS created_at
            FROM mat_material m
            LEFT JOIN sys_dict_item d
                   ON d.item_value = m.category_code AND d.dict_type = #{categoryDictType}
            WHERE m.del_flag = 0
            <if test="materialName != null and materialName != ''">
                AND m.material_name LIKE CONCAT('%', #{materialName}, '%')
            </if>
            <if test="categoryCode != null and categoryCode != ''">
                AND m.category_code = #{categoryCode}
            </if>
            ORDER BY m.material_id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<MaterialVO> selectPageList(@Param("materialName") String materialName,
                                    @Param("categoryCode") String categoryCode,
                                    @Param("categoryDictType") String categoryDictType,
                                    @Param("offset") long offset,
                                    @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM mat_material m
            WHERE m.del_flag = 0
            <if test="materialName != null and materialName != ''">
                AND m.material_name LIKE CONCAT('%', #{materialName}, '%')
            </if>
            <if test="categoryCode != null and categoryCode != ''">
                AND m.category_code = #{categoryCode}
            </if>
            </script>
            """)
    long countPage(@Param("materialName") String materialName,
                   @Param("categoryCode") String categoryCode);

    @Select("""
            SELECT item_label
            FROM sys_dict_item
            WHERE dict_type = #{dictType} AND item_value = #{categoryCode} AND item_status = 1
            LIMIT 1
            """)
    String selectCategoryName(@Param("dictType") String dictType,
                              @Param("categoryCode") String categoryCode);
}
