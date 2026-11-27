package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 菜单权限表 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long menuId;
    private Long parentId;
    private String menuName;
    private String path;
    private String icon;
    private String permCode;
    private Integer menuType;   // 1目录 2菜单 3按钮
    private Integer sortNo;
    private Integer menuStatus;

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
