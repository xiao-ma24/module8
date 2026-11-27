package com.school.material.service;

import com.school.material.common.PageResult;
import com.school.material.dto.MaterialReq;
import com.school.material.dto.MaterialVO;

public interface MaterialService {

    /** 物资分页查询（status 仅为兼容旧前端保留，不参与查询） */
    PageResult<MaterialVO> page(Integer pageNum, Integer pageSize, String materialName,
                                String categoryCode, Integer status);

    /** 物资详情 */
    MaterialVO getById(Long id);

    /** 新增物资，返回主键 */
    Long create(MaterialReq req);

    /** 修改物资 */
    void update(Long id, MaterialReq req);

    /** 删除物资（逻辑删除） */
    void delete(Long id);
}
