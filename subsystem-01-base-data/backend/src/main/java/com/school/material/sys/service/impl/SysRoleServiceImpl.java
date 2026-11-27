package com.school.material.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.sys.entity.SysRole;
import com.school.material.sys.mapper.SysRoleMapper;
import com.school.material.sys.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<SysRole> list(Integer status) {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSortNo)
                .orderByAsc(SysRole::getId));
    }
}
