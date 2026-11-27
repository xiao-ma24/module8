package com.school.material.service;

import com.school.material.dto.CategoryTreeResp;
import com.school.material.dto.MaterialCategoryReq;

import java.util.List;

public interface MaterialCategoryService {

    /** 分类树查询 */
    List<CategoryTreeResp> tree(Integer status);

    /** 分类详情 */
    CategoryTreeResp getById(Long id);

    /** 新增分类，返回主键 */
    Long create(MaterialCategoryReq req);

    /** 修改分类 */
    void update(Long id, MaterialCategoryReq req);

    /** 删除分类 */
    void delete(Long id);
}
