package com.school.material.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增/修改请求（管理员维护基础信息）
 */
@Data
public class UserReq {

    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String userName;

    private String deptId;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    /** 用户类型：1 教师 / 2 学生 / 3 管理员，默认 1 */
    private Integer userType;

    /** 状态：0 待启用 / 1 启用，默认 0 */
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
