package com.school.material.dto;

import com.school.material.entity.SysDictItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点（数据来源：平台字典 sys_dict_item）
 */
@Data
public class CategoryTreeResp {

    private Long id;
    private String categoryNo;
    private String categoryName;
    private Long parentId;
    private Integer sort;
    private Integer status;
    private List<CategoryTreeResp> children = new ArrayList<>();

    public static CategoryTreeResp from(SysDictItem item) {
        CategoryTreeResp node = new CategoryTreeResp();
        node.setId(item.getItemId());
        node.setCategoryNo(item.getItemValue());
        node.setCategoryName(item.getItemLabel());
        node.setParentId(0L);
        node.setSort(item.getSortNo());
        node.setStatus(item.getItemStatus());
        return node;
    }
}
