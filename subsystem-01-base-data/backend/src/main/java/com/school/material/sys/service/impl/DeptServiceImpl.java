package com.school.material.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.sys.dto.DeptReq;
import com.school.material.sys.entity.Dept;
import com.school.material.sys.mapper.DeptMapper;
import com.school.material.sys.mapper.SysUserMapper;
import com.school.material.sys.entity.SysUser;
import com.school.material.sys.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<Dept> list(String parentId, Integer status) {
        return deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .eq(parentId != null && !parentId.isBlank(), Dept::getParentId, parentId)
                .eq(status != null, Dept::getStatus, status)
                .orderByAsc(Dept::getSort)
                .orderByAsc(Dept::getId));
    }

    @Override
    public Dept getById(String id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DeptReq req) {
        checkDeptNoUnique(req.getDeptNo(), null);
        checkParent(req.getParentId(), null);

        Dept dept = new Dept();
        dept.setId(nextOrgId());
        fill(dept, req);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, DeptReq req) {
        Dept dept = getById(id);
        checkDeptNoUnique(req.getDeptNo(), id);
        checkParent(req.getParentId(), id);

        fill(dept, req);
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        getById(id);
        Long children = deptMapper.selectCount(Wrappers.<Dept>lambdaQuery().eq(Dept::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN, "该部门下存在子部门，不能删除");
        }
        Long users = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id));
        if (users != null && users > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USER, "该部门下存在用户，不能删除");
        }
        deptMapper.deletePhysically(id);
    }

    /** 生成 6 位组织编码（与第4组师资系统 org_organization.org_id 对齐） */
    private String nextOrgId() {
        List<Dept> all = deptMapper.selectList(Wrappers.<Dept>lambdaQuery().select(Dept::getId));
        int max = 0;
        for (Dept d : all) {
            try {
                max = Math.max(max, Integer.parseInt(d.getId()));
            } catch (Exception ignore) {
            }
        }
        return String.format("%06d", max + 1);
    }

    private void fill(Dept dept, DeptReq req) {
        dept.setDeptNo(req.getDeptNo().trim());
        dept.setDeptName(req.getDeptName().trim());
        dept.setParentId(req.getParentId() == null || req.getParentId().isBlank() ? "000000" : req.getParentId());
        dept.setSort(req.getSort() == null ? 0 : req.getSort());
        dept.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        dept.setRemark(req.getRemark());
    }

    private void checkDeptNoUnique(String deptNo, String selfId) {
        Long count = deptMapper.selectCount(Wrappers.<Dept>lambdaQuery()
                .eq(Dept::getDeptNo, deptNo.trim())
                .ne(selfId != null, Dept::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DEPT_NO_DUPLICATE, "部门编号已存在");
        }
    }

    private void checkParent(String parentId, String selfId) {
        if (parentId == null || parentId.isBlank() || "0".equals(parentId) || "000000".equals(parentId)) {
            return;
        }
        if (selfId != null && selfId.equals(parentId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上级部门不能是自己");
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "上级部门不存在");
        }
    }
}
