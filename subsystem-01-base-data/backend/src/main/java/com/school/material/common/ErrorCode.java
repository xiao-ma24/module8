package com.school.material.common;

/**
 * 子系统① 错误码（区间 1000–1999）
 */
public final class ErrorCode {

    /** 成功 */
    public static final int SUCCESS = 0;
    /** 系统内部错误 */
    public static final int SYSTEM_ERROR = 500;
    /** 接口不存在 */
    public static final int NOT_FOUND = 404;
    /** 请求方法不支持 */
    public static final int METHOD_NOT_ALLOWED = 405;

    /** 请求参数校验失败 */
    public static final int PARAM_ERROR = 1001;
    /** 分类不存在 */
    public static final int CATEGORY_NOT_FOUND = 1002;
    /** 分类编号已存在 */
    public static final int CATEGORY_NO_DUPLICATE = 1003;
    /** 该分类下存在子分类，不能删除 */
    public static final int CATEGORY_HAS_CHILDREN = 1004;
    /** 该分类下存在物资，不能删除 */
    public static final int CATEGORY_HAS_MATERIAL = 1005;
    /** 数据重复（唯一约束冲突） */
    public static final int DATA_DUPLICATE = 1006;

    /** 物资不存在 */
    public static final int MATERIAL_NOT_FOUND = 1010;
    /** 物资编码已存在 */
    public static final int MATERIAL_NO_DUPLICATE = 1011;
    /** 物资所属分类不存在 */
    public static final int MATERIAL_CATEGORY_NOT_FOUND = 1012;

    /** 仓库不存在 */
    public static final int WAREHOUSE_NOT_FOUND = 1020;
    /** 仓库编号已存在 */
    public static final int WAREHOUSE_NO_DUPLICATE = 1021;

    /** 库存记录不存在 */
    public static final int STOCK_NOT_FOUND = 1030;
    /** 库存不足，不能出库 */
    public static final int STOCK_NOT_ENOUGH = 1031;
    /** 库存变更方向非法 */
    public static final int STOCK_DIRECTION_INVALID = 1032;
    /** 变更数量必须大于 0 */
    public static final int STOCK_QUANTITY_INVALID = 1033;

    /** 发票登记记录不存在 */
    public static final int PURCHASE_RECORD_NOT_FOUND = 1040;
    /** 发票登记记录已处理，不能重复审核 */
    public static final int PURCHASE_RECORD_HANDLED = 1041;
    /** 审核状态非法 */
    public static final int PURCHASE_AUDIT_STATUS_INVALID = 1042;

    /** 库存变动申请单不存在 */
    public static final int STOCK_APPLY_NOT_FOUND = 1050;
    /** 库存变动申请单已处理，不能重复审核 */
    public static final int STOCK_APPLY_HANDLED = 1051;
    /** 库存变动申请审核状态非法 */
    public static final int STOCK_APPLY_AUDIT_STATUS_INVALID = 1052;

    /** 用户不存在 */
    public static final int USER_NOT_FOUND = 1060;
    /** 工号已存在 */
    public static final int USER_NO_DUPLICATE = 1061;
    /** 部门不存在 */
    public static final int DEPT_NOT_FOUND = 1062;
    /** 角色不存在 */
    public static final int ROLE_NOT_FOUND = 1063;
    /** 部门编号已存在 */
    public static final int DEPT_NO_DUPLICATE = 1064;
    /** 该部门下存在子部门，不能删除 */
    public static final int DEPT_HAS_CHILDREN = 1065;
    /** 该部门下存在用户，不能删除 */
    public static final int DEPT_HAS_USER = 1066;

    private ErrorCode() {
    }
}
