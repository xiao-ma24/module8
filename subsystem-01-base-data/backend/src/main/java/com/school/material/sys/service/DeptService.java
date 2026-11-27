package com.school.material.sys.service;

import com.school.material.sys.dto.DeptReq;
import com.school.material.sys.entity.Dept;

import java.util.List;

public interface DeptService {

    /**
     * 查询部门列表，可按上级部门与状态筛选。
     */
    List<Dept> list(String parentId, Integer status);

    Dept getById(String id);

    String create(DeptReq req);

    void update(String id, DeptReq req);

    void delete(String id);
}
