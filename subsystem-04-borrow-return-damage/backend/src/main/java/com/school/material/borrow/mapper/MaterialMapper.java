package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 物资目录只读 Mapper（子系统①维护）。领用只允许选择低值易耗品（material_type=1）。 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    @Select("""
        <script>
        SELECT material_id, material_code, material_name, category_code, material_type, unit,
               ref_price, is_controlled, service_life_years, shelf_life_days, safe_stock
        FROM mat_material
        WHERE del_flag = 0
        <if test="materialType != null"> AND material_type = #{materialType}</if>
        <if test="keyword != null and keyword != ''">
            AND (material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR material_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY material_code
        </script>""")
    List<Material> selectMaterials(@Param("keyword") String keyword,
                                   @Param("materialType") Integer materialType);
}
