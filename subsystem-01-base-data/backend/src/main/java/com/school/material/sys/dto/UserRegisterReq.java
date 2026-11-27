package com.school.material.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新教师建档请求：系统生成默认密码，状态置为待启用
 */
@Data
public class UserRegisterReq {

    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String userName;

    @NotNull(message = "部门不能为空")
    private String deptId;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    /** 用户类型：1 教师 / 2 学生 / 3 管理员，默认 1 */
    private Integer userType;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
