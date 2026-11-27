package com.school.material.borrow.common;

/**
 * 角色编码（与 sys_role.role_code 一致）。
 * 本子系统（③领用）的权限口径：
 * <pre>
 *   APPLICANT / STUDENT / DEPT_HEAD  提交领用申请，仅能看本人单据
 *   DEPT_HEAD                        审批本部门领用申请
 *   STORE_KEEPER                     拣货出库，可看全部单据与库存
 *   ADMIN                            只读查看全部，不参与业务动作
 *   PURCHASER / CHECKER              与领用业务无关，仅可查看影响面（默认无菜单）
 * </pre>
 */
public final class Role {

    public static final String ADMIN = "ADMIN";
    public static final String DEPT_HEAD = "DEPT_HEAD";
    public static final String PURCHASER = "PURCHASER";
    public static final String STORE_KEEPER = "STORE_KEEPER";
    public static final String APPLICANT = "APPLICANT";
    public static final String STUDENT = "STUDENT";
    public static final String CHECKER = "CHECKER";

    private Role() {
    }

    /** 可发起申请的角色 */
    public static final String[] CAN_APPLY = {APPLICANT, STUDENT, DEPT_HEAD};

    /** 只读全量数据的角色（不做业务动作） */
    public static boolean readOnly(String roleCode) {
        return ADMIN.equals(roleCode);
    }
}
