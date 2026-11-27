package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.SysMenu;
import com.school.material.admin.entity.SysRole;
import com.school.material.admin.mapper.SysMenuMapper;
import com.school.material.admin.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 角色与菜单权限 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public List<SysRole> list(String keyword, Integer status) {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(status != null, SysRole::getRoleStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleCode, keyword))
                .orderByAsc(SysRole::getSortNo));
        for (SysRole r : roles) {
            Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM mat_sys_user u JOIN mat_sys_user_role ur ON ur.user_id = u.user_id WHERE ur.role_id = ? AND u.del_flag = 0", Integer.class, r.getRoleId());
            r.setUserCount(c == null ? 0 : c);
            r.setMenuIds(menuMapper.selectMenuIdsByRole(r.getRoleId()));
        }
        return roles;
    }

    public SysRole save(SysRole in) {
        if (in.getRoleCode() == null || in.getRoleCode().isBlank()) throw new BizException("角色编码不能为空");
        Long dup = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, in.getRoleCode().trim())
                .ne(in.getRoleId() != null, SysRole::getRoleId, in.getRoleId()));
        if (dup != null && dup > 0) throw new BizException("角色编码已存在：" + in.getRoleCode());
        in.setRoleCode(in.getRoleCode().trim().toUpperCase());
        if (in.getRoleId() == null) {
            in.setCreatedAt(LocalDateTime.now());
            in.setRoleStatus(in.getRoleStatus() == null ? 1 : in.getRoleStatus());
            in.setDataScope(in.getDataScope() == null ? "全校" : in.getDataScope());
            roleMapper.insert(in);
            audit.write("角色权限", "新增角色", in.getRoleCode(), in.getRoleName());
        } else {
            SysRole old = roleMapper.selectById(in.getRoleId());
            in.setUpdatedAt(LocalDateTime.now());
            roleMapper.updateById(in);
            audit.write("角色权限", "修改角色", in.getRoleCode(), roleDiff(old, in),
                    old == null ? null : "名称=" + old.getRoleName() + "，数据范围=" + old.getDataScope()
                            + "，状态=" + old.getRoleStatus(),
                    "名称=" + in.getRoleName() + "，数据范围=" + in.getDataScope() + "，状态=" + in.getRoleStatus());
        }
        return in;
    }

    @Transactional
    public void saveMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BizException("角色不存在");
        List<Long> beforeIds = menuMapper.selectMenuIdsByRole(roleId);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        if (menuIds != null) {
            for (Long mid : menuIds) {
                jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (?, ?)", roleId, mid);
            }
        }
        audit.write("角色权限", "分配菜单", role.getRoleCode(), "菜单数 " + (menuIds == null ? 0 : menuIds.size()),
                "菜单ID=" + beforeIds, "菜单ID=" + (menuIds == null ? List.of() : menuIds));
    }

    public void remove(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BizException("角色不存在");
        if ("ADMIN".equals(role.getRoleCode())) throw new BizException("内置管理员角色不可删除");
        Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM mat_sys_user_role WHERE role_id = ?", Integer.class, roleId);
        if (used != null && used > 0) throw new BizException("该角色下还有 " + used + " 个用户，请先调整用户角色");
        roleMapper.deleteById(roleId);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        audit.write("角色权限", "删除角色", role.getRoleCode(), role.getRoleName());
    }

    /** 全部菜单（树），供权限分配树使用 */
    public List<SysMenu> menuTree() {
        List<SysMenu> all = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSortNo));
        Map<Long, SysMenu> map = all.stream().collect(Collectors.toMap(SysMenu::getMenuId, m -> m, (a, b) -> a, LinkedHashMap::new));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : all) {
            SysMenu p = m.getParentId() == null ? null : map.get(m.getParentId());
            if (p == null) roots.add(m);
            else p.getChildren().add(m);
        }
        return roots;
    }

    private String roleDiff(SysRole old, SysRole now) {
        List<String> changes = new ArrayList<>();
        if (old == null) return "更新角色信息";
        if (!Objects.equals(old.getRoleName(), now.getRoleName()))
            changes.add("名称 " + old.getRoleName() + " → " + now.getRoleName());
        if (!Objects.equals(old.getDataScope(), now.getDataScope()))
            changes.add("数据范围 " + old.getDataScope() + " → " + now.getDataScope());
        if (!Objects.equals(old.getRoleStatus(), now.getRoleStatus()))
            changes.add("状态 " + old.getRoleStatus() + " → " + now.getRoleStatus());
        if (!Objects.equals(old.getRemark(), now.getRemark()))
            changes.add("备注已更新");
        return changes.isEmpty() ? "更新角色信息" : String.join("；", changes);
    }
}
