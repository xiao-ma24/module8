package com.school.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/修改请求
 */
@Data
public class MaterialCategoryReq {

    @NotBlank(message = "分类编号不能为空")
    @Size(max = 32, message = "分类编号长度不能超过 32")
    private String categoryNo;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过 64")
    private String categoryName;

    /** 上级分类 id，默认 0（根节点） */
    private Long parentId;

    private Integer sort;

    /** 1 启用 / 0 停用，默认 1 */
    private Integer status;
}
