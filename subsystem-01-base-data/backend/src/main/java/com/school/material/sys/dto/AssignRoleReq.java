package com.school.material.sys.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配角色请求：分配成功后用户状态置为启用
 */
@Data
public class AssignRoleReq {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;

    @NotEmpty(message = "角色 id 列表不能为空")
    private List<Long> roleIds;
}
