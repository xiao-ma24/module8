-- ============================================================
-- 2026-11-25 对齐《系统数据字典统一版 v1.4》G08 物资管理字典
-- ------------------------------------------------------------
-- 依据：系统数据字典统一版v1.4.xlsx / G08_物资管理_数据库字典（39 张业务表）
-- 范围：
--   1) 表改名：asset_inbound → mat_asset_inbound；sys_operation_log → mat_operation_log
--   2) mat_asset_inbound：emp_id/appr_id → CHAR(10)，asset_id → CHAR(20)
--   3) mat_sys_user：新增 person_id CHAR(20)；org_id → CHAR(6)
--   4) mat_dept：org_id / parent_id → CHAR(6)（org_id 去自增，6 位组织编码）
--   5) material_dept_quota：dept_id → CHAR(6)；新增 6 个财务联动字段
--   6) mat_purchase_plan.org_id / mat_req_order.org_id → CHAR(6)
--   7) budget.dept_id → CHAR(6)（字典外保留字段，随部门编码对齐）
--   8) mat_material：spec TEXT → VARCHAR(128)；material_status CHAR(1) → TINYINT
-- 说明：
--   * 组织编码转换规则：LPAD(原值, 6, '0')，parent_id 的 0 转为 '000000'
--   * 脚本可重复执行前请先备份；已有库升级使用
-- ============================================================

USE `material_mgmt`;
SET NAMES utf8mb4;

-- 1) 表改名（幂等：旧表存在且新表不存在时才改名；两者并存时补数据后删旧表）
--    注意 CHECK 约束名在 schema 内唯一，若旧表 asset_inbound 与 mat_asset_inbound 并存，
--    不能直接 RENAME（会报 ERROR 3822），必须先迁移数据再删旧表
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'asset_inbound') = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'mat_asset_inbound') = 0,
  'RENAME TABLE `asset_inbound` TO `mat_asset_inbound`',
  'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log') = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'mat_operation_log') = 0,
  'RENAME TABLE `sys_operation_log` TO `mat_operation_log`',
  'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) mat_asset_inbound 工号/资产编号对齐统一版长度
ALTER TABLE `mat_asset_inbound`
  MODIFY COLUMN `emp_id` CHAR(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请人工号，与人事主数据一致（第4组师资/第1组学籍）',
  MODIFY COLUMN `appr_id` CHAR(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批人（部门负责人）工号',
  MODIFY COLUMN `asset_id` CHAR(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '资产编号，与资产台账编码规则一致';

-- 3) mat_sys_user：新增 person_id，org_id 改 6 位字符串
ALTER TABLE `mat_sys_user`
  ADD COLUMN `person_id` CHAR(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联ID（新增）' AFTER `user_id`,
  MODIFY COLUMN `org_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属部门，关联 dept';
UPDATE `mat_sys_user` SET `org_id` = LPAD(`org_id`, 6, '0');

-- 4) mat_dept：org_id 去自增并改 6 位组织编码，parent_id 同步
ALTER TABLE `mat_dept` MODIFY COLUMN `org_id` BIGINT NOT NULL COMMENT '部门主键';
ALTER TABLE `mat_dept`
  MODIFY COLUMN `org_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门主键',
  MODIFY COLUMN `parent_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '上级部门，0为顶级';
UPDATE `mat_dept`
   SET `parent_id` = LPAD(`parent_id`, 6, '0'),
       `org_id`    = LPAD(`org_id`, 6, '0');

-- 5) material_dept_quota：dept_id 改 6 位字符串 + 财务联动字段
ALTER TABLE `material_dept_quota`
  MODIFY COLUMN `dept_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门，关联 dept.dept_id',
  ADD COLUMN `fin_quota_id` VARCHAR(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务额度编号',
  ADD COLUMN `fin_apply_no` VARCHAR(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务申报单号',
  ADD COLUMN `submit_status` CHAR(2) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '部门额度状态',
  ADD COLUMN `submit_at` DATETIME DEFAULT NULL COMMENT '提交财务组的时间',
  ADD COLUMN `fin_confirm_at` DATETIME DEFAULT NULL COMMENT '财务确认回写时间',
  ADD COLUMN `fin_opinion` VARCHAR(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务确认意见或退回原因';
UPDATE `material_dept_quota` SET `dept_id` = LPAD(`dept_id`, 6, '0');

-- 6) 采购计划 / 领用单 org_id 改 6 位字符串
ALTER TABLE `mat_purchase_plan` MODIFY COLUMN `org_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请部门，关联 dept';
UPDATE `mat_purchase_plan` SET `org_id` = LPAD(`org_id`, 6, '0');
ALTER TABLE `mat_req_order` MODIFY COLUMN `org_id` CHAR(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请部门，用于额度校验';
UPDATE `mat_req_order` SET `org_id` = LPAD(`org_id`, 6, '0');

-- 7) budget.dept_id（字典外保留字段）：随部门编码改 6 位字符串
ALTER TABLE `budget` MODIFY COLUMN `dept_id` CHAR(6) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门ID：财务按部门核定的预算额度';
UPDATE `budget` SET `dept_id` = LPAD(`dept_id`, 6, '0') WHERE `dept_id` IS NOT NULL;

-- 8) mat_material：规格型号/物资状态按统一版口径
ALTER TABLE `mat_material`
  MODIFY COLUMN `spec` VARCHAR(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  MODIFY COLUMN `material_status` TINYINT NOT NULL DEFAULT 1 COMMENT '物资状态：1启用 0停用';
