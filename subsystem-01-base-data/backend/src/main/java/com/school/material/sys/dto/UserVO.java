package com.school.material.sys.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表/详情响应
 */
@Data
public class UserVO {

    private Long id;

    private String userNo;

    private String userName;

    /** 1 教师 / 2 学生 / 3 管理员 */
    private Integer userType;

    private String deptId;

    private String deptName;

    private String phone;

    /** 0 待启用 / 1 启用 */
    private Integer status;

    private String statusText;

    private List<Long> roleIds;

    private String roleNames;

    private String remark;

    private LocalDateTime createdAt;
}
