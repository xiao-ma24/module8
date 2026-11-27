package com.school.material.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 部门（团队 sys 字典 dept 表）
 */
@Data
@TableName("mat_dept")
public class Dept {

    @TableId(value = "org_id", type = IdType.INPUT)
    private String id;

    /** 部门编码（唯一） */
    @TableField("dept_code")
    private String deptNo;

    /** 部门名称 */
    private String deptName;

    /** 上级部门 id，0 表示顶级 */
    private String parentId;

    /** 排序号 */
    @TableField("sort_no")
    private Integer sort;

    /** 1 启用 / 0 停用 */
    @TableField("dept_status")
    private Integer status;

    private String remark;

    @JsonIgnore
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
