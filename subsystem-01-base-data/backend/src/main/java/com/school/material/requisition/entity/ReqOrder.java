package com.school.material.requisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领用/出入库申请单（团队 req_order 表）
 * 扩展列：applicant_user_no、warehouse_id、batch_no、direction、audit_remark
 */
@Data
@TableName("mat_req_order")
public class ReqOrder {

    @TableId(value = "req_id", type = IdType.AUTO)
    private Long reqId;

    /** 申请单号 LY+yyyyMMdd+四位流水 */
    private String reqNo;

    /** 申请人 user_id（查不到填 0） */
    private Long applicantId;

    /** 子系统①扩展：申请人工号，仅展示用 */
    @TableField("applicant_user_no")
    private String applicantUserNo;

    @TableField("org_id")
    private String deptId;

    private Long materialId;

    /** 申请数量 */
    private BigDecimal qtyApply;

    /** 核准数量 */
    private BigDecimal qtyApprove;

    private BigDecimal amount;

    /** 1待审核 2已通过 3已驳回 */
    private Integer reqStatus;

    /** 子系统①扩展：申请仓库 */
    private Long warehouseId;

    /** 子系统①扩展：库存批次 */
    private String batchNo;

    /** 子系统①扩展：IN入库 / OUT出库 */
    private String direction;

    /** 子系统①扩展：审核意见 */
    private String auditRemark;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
