package com.school.material.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.common.PageResult;
import com.school.material.sys.dto.AssignRoleReq;
import com.school.material.sys.dto.UserRegisterReq;
import com.school.material.sys.dto.UserReq;
import com.school.material.sys.dto.UserVO;
import com.school.material.sys.entity.Dept;
import com.school.material.sys.entity.SysRole;
import com.school.material.sys.entity.SysUser;
import com.school.material.sys.entity.SysUserRole;
import com.school.material.sys.mapper.DeptMapper;
import com.school.material.sys.mapper.SysRoleMapper;
import com.school.material.sys.mapper.SysUserMapper;
import com.school.material.sys.mapper.SysUserRoleMapper;
import com.school.material.sys.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 建档默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";
    /** 待启用 */
    private static final int STATUS_PENDING = 0;
    /** 启用 */
    private static final int STATUS_ENABLED = 1;
    /** 默认用户类型：教师 */
    private static final int DEFAULT_USER_TYPE = 1;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final DeptMapper deptMapper;

    @Override
    public PageResult<UserVO> page(Integer pageNum, Integer pageSize, String keyword, String deptId, Integer status) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(num, size), Wrappers.<SysUser>lambdaQuery()
                .and(kw != null, w -> w.like(SysUser::getUserNo, kw).or().like(SysUser::getUserName, kw))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId));

        return PageResult.of(page.getTotal(), toVOList(page.getRecords()));
    }

    @Override
    public UserVO getById(Long id) {
        return toVOList(List.of(getEntity(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterReq req) {
        checkUserNoUnique(req.getUserNo(), null);
        if (req.getDeptId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门不能为空");
        }
        checkDeptExists(req.getDeptId());

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUserNo(req.getUserNo().trim());
        user.setUserName(req.getUserName().trim());
        user.setPassword(encodePassword(DEFAULT_PASSWORD));
        user.setUserType(req.getUserType() == null ? DEFAULT_USER_TYPE : req.getUserType());
        user.setDeptId(req.getDeptId());
        user.setPhone(req.getPhone());
        user.setStatus(STATUS_PENDING);
        user.setRemark(req.getRemark());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);

        return toVOList(List.of(user)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserReq req) {
        checkUserNoUnique(req.getUserNo(), null);
        if (req.getDeptId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门不能为空");
        }
        checkDeptExists(req.getDeptId());

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        fill(user, req);
        user.setPassword(encodePassword(DEFAULT_PASSWORD));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserReq req) {
        SysUser user = getEntity(id);
        checkUserNoUnique(req.getUserNo(), id);
        if (req.getDeptId() != null) {
            checkDeptExists(req.getDeptId());
        }

        fill(user, req);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getEntity(id);
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        sysUserMapper.deletePhysically(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO assignRoles(AssignRoleReq req) {
        SysUser user = getEntity(req.getUserId());

        List<Long> roleIds = req.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色 id 列表不能为空");
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "存在无效的角色 id");
        }

        // 覆盖式分配：先清空该用户已有角色，再写入新角色
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getId()));
        LocalDateTime now = LocalDateTime.now();
        for (Long roleId : roleIds) {
            SysUserRole link = new SysUserRole();
            link.setUserId(user.getId());
            link.setRoleId(roleId);
            link.setGrantedBy(0L);
            link.setGrantedAt(now);
            sysUserRoleMapper.insert(link);
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setStatus(STATUS_ENABLED);
        update.setRoleCode(roles.get(0).getRoleNo());
        update.setUpdatedAt(now);
        sysUserMapper.updateById(update);

        user.setStatus(STATUS_ENABLED);
        user.setRoleCode(update.getRoleCode());
        return toVOList(List.of(user)).get(0);
    }

    private SysUser getEntity(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void fill(SysUser user, UserReq req) {
        user.setUserNo(req.getUserNo().trim());
        user.setUserName(req.getUserName().trim());
        user.setDeptId(req.getDeptId());
        user.setPhone(req.getPhone());
        user.setUserType(req.getUserType() == null ? DEFAULT_USER_TYPE : req.getUserType());
        user.setStatus(req.getStatus() == null ? STATUS_PENDING : req.getStatus());
        user.setRemark(req.getRemark());
    }

    private void checkUserNoUnique(String userNo, Long selfId) {
        Long count = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUserNo, userNo.trim())
                .ne(selfId != null, SysUser::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USER_NO_DUPLICATE, "工号已存在");
        }
    }

    private void checkDeptExists(String deptId) {
        if (deptId != null && deptMapper.selectById(deptId) == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }
    }

    private String encodePassword(String rawPassword) {
        return DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
    }

    private List<UserVO> toVOList(List<SysUser> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, Dept::getDeptName));

        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        List<SysUserRole> links = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));
        Set<Long> roleIds = links.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        Map<Long, List<SysUserRole>> linksByUser = links.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId));

        List<UserVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUserNo(user.getUserNo());
            vo.setUserName(user.getUserName());
            vo.setUserType(user.getUserType());
            vo.setDeptId(user.getDeptId());
            vo.setDeptName(user.getDeptId() == null ? null : deptNameMap.get(user.getDeptId()));
            vo.setPhone(user.getPhone());
            vo.setStatus(user.getStatus());
            vo.setStatusText(statusText(user.getStatus()));
            vo.setRemark(user.getRemark());
            vo.setCreatedAt(user.getCreatedAt());

            List<SysRole> userRoles = linksByUser.getOrDefault(user.getId(), Collections.emptyList()).stream()
                    .map(link -> roleMap.get(link.getRoleId()))
                    .filter(Objects::nonNull)
                    .toList();
            vo.setRoleIds(userRoles.stream().map(SysRole::getId).toList());
            vo.setRoleNames(userRoles.stream().map(SysRole::getRoleName).collect(Collectors.joining("、")));
            result.add(vo);
        }
        return result;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return status == STATUS_ENABLED ? "启用" : "待启用";
    }
}
