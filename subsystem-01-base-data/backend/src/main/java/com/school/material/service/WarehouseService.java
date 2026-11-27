package com.school.material.service;

import com.school.material.common.PageResult;
import com.school.material.dto.WarehouseReq;
import com.school.material.entity.Warehouse;

public interface WarehouseService {

    /** 仓库分页查询 */
    PageResult<Warehouse> page(Integer pageNum, Integer pageSize, String warehouseName, Integer status);

    /** 仓库详情 */
    Warehouse getById(Long id);

    /** 新增仓库，返回主键 */
    Long create(WarehouseReq req);

    /** 修改仓库 */
    void update(Long id, WarehouseReq req);

    /** 删除仓库（逻辑删除） */
    void delete(Long id);
}
