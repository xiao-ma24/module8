package com.school.material.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增/修改请求
 */
@Data
public class DeptReq {

    @NotBlank(message = "部门编号不能为空")
    @Size(max = 32, message = "部门编号长度不能超过 32")
    private String deptNo;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 128, message = "部门名称长度不能超过 128")
    private String deptName;

    /** 上级部门 id，默认 0（顶级） */
    private String parentId;

    private Integer sort;

    /** 1 启用 / 0 停用，默认 1 */
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
