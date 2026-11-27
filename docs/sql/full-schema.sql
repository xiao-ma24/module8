-- ============================================================
-- 学校物资管理大系统 · 全量数据库结构脚本（DBA 交付版）
-- ------------------------------------------------------------
-- 目标库：material_mgmt（MySQL 8.0+，utf8mb4）
-- 来源：基于联调环境数据库导出结构，并按 2026-11-24 数据字典统一提交对齐：
--       表名 mat_*；列名 person_no / org_id / user_status / dept_status /
--       role_status / approved_at；补齐 mat_material.spec 等列
-- 内容：建库 + 39 张业务表 + 3 张课程模拟外部表（不含业务数据）
-- 执行顺序：
--   1) full-schema.sql   建库 + 建表
--   2) init-data.sql     部门/角色/用户/菜单/字典/参数/仓库/供应商/分类
--   3) verify.sql        导入自检
-- 注意：
--   * 脚本含 DROP TABLE IF EXISTS，重复执行会清空同名表，禁止在生产库直接执行
--   * 全库统一 utf8mb4_general_ci，禁止混用 utf8mb4_0900_ai_ci，否则跨表 join 会报
--     Illegal mix of collations
--   * 旧版本遗留表（asset_inbound / dept / stock ...）会在开头自动清理：其 CHECK
--     约束名（如 ck_asset_amount）与新表同名，不清理会报 ERROR 3822
--   * sys_teacher / sys_student / sys_budget_subject 为课程模拟外部系统表，
--     新增用户、自助注册、预算科目下拉依赖它们，缺失会 500
--   * 师资/学生/预算等外部系统数据正式环境由 DBA 通过接口集成
-- ============================================================

CREATE DATABASE IF NOT EXISTS `material_mgmt`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `material_mgmt`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

-- MySQL dump 10.13  Distrib 9.6.0, for macos15 (arm64)
-- Host: 127.0.0.1    Database: material_mgmt
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- ------------------------------------------------------------
-- 旧版本遗留对象清理（v1.4 之前的旧表名/兼容视图）
-- 说明：旧表 asset_inbound 等携带的 CHECK 约束名（ck_asset_amount 等）与新表
--       同名，CHECK 约束名在 schema 内唯一，不先清理会在建新表时报
--       ERROR 3822 Duplicate check constraint name
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `tmp_drop_legacy_tables`;
DELIMITER $$
CREATE PROCEDURE `tmp_drop_legacy_tables`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_name VARCHAR(64);
  DECLARE v_type VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT table_name, table_type FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN (
         'asset_inbound','sys_operation_log','dept','material','stock','stock_flow',
         'req_order','out_order','loss_order','borrow_return','approve_record',
         'supplier','warehouse','sys_user','sys_role','sys_user_role','sys_role_menu',
         'purchase_plan','purchase_request','purchase_request_item',
         'purchase_order','purchase_order_item','inbound_order','inbound_item',
         'material_inbound_interface');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_name, v_type;
    IF done = 1 THEN LEAVE read_loop; END IF;
    SET @ddl = IF(v_type = 'VIEW',
                  CONCAT('DROP VIEW `', v_name, '`'),
                  CONCAT('DROP TABLE `', v_name, '`'));
    PREPARE st FROM @ddl;
    EXECUTE st;
    DEALLOCATE PREPARE st;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;
CALL `tmp_drop_legacy_tables`();
DROP PROCEDURE `tmp_drop_legacy_tables`;


-- GTID state at the beginning of the backup 
--
-- Table structure for table `acceptance_item`
DROP TABLE IF EXISTS `acceptance_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acceptance_item` (
  `ai_id` bigint NOT NULL AUTO_INCREMENT,
  `acpt_id` bigint NOT NULL,
  `poi_id` bigint NOT NULL,
  `material_id` bigint NOT NULL,
  `check_qty` decimal(12,3) NOT NULL,
  `pass_qty` decimal(12,3) DEFAULT '0.000',
  `fail_qty` decimal(12,3) DEFAULT '0.000',
  `fail_reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `handle_type` tinyint DEFAULT '1' COMMENT '1入库 2退货 3换货',
  `handle_status` tinyint DEFAULT '0' COMMENT '退换货处理状态 0待处理 1已完成',
  `handled_at` datetime DEFAULT NULL COMMENT '处理完成时间',
  `handled_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理人',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`ai_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `acceptance_order`
DROP TABLE IF EXISTS `acceptance_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acceptance_order` (
  `acpt_id` bigint NOT NULL AUTO_INCREMENT,
  `acpt_no` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'YS+年月日+4位流水',
  `po_id` bigint NOT NULL,
  `acpt_date` date DEFAULT NULL,
  `inspector_id` bigint DEFAULT NULL,
  `deliverer` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `photo_urls` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `acpt_score` tinyint DEFAULT NULL COMMENT '验收评分1-5',
  `acpt_status` tinyint DEFAULT '1' COMMENT '0待验收 1验收中 2已完成 3质量异议',
  `quality_report` text COLLATE utf8mb4_general_ci,
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`acpt_id`),
  UNIQUE KEY `acpt_no` (`acpt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_approve_record`
DROP TABLE IF EXISTS `mat_approve_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_approve_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批记录主键',
  `biz_type` tinyint NOT NULL COMMENT '1领用 2借用 3报损 4采购 5报废',
  `biz_id` bigint NOT NULL COMMENT '业务单据 ID',
  `approver_id` bigint NOT NULL COMMENT '审批人，关联 sys_user.user_id',
  `approve_result` tinyint NOT NULL COMMENT '审批结果：0驳回 1通过',
  `approved_at` datetime NOT NULL COMMENT '审批时间',
  `opinion` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`record_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_asset_inbound`
DROP TABLE IF EXISTS `mat_asset_inbound`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_asset_inbound` (
  `rk_no` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请单号：RK+年月日+4位流水，系统自动生成',
  `in_no` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '入库单号：In+年月日+4位流水，登记入库时生成',
  `appr_no` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批单号：审批动作产生，与申请单号一一对应',
  `flow_no` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库存流水号：同一资产多次变动不重复',
  `emp_id` char(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请人工号，与人事主数据一致（第4组师资/第1组学籍）',
  `appr_id` char(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批人（部门负责人）工号',
  `store_id` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库管员工号',
  `asset_id` char(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '资产编号，与资产台账编码规则一致',
  `asset_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '资产名称，如笔记本电脑、办公桌',
  `model` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号：品牌与型号，无型号时填规格尺寸',
  `qty` int NOT NULL COMMENT '数量',
  `price` decimal(12,2) NOT NULL COMMENT '单价（元）',
  `amount` decimal(14,2) NOT NULL COMMENT '金额（元）=单价×数量',
  `invoice_no` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发票号码：防止同一张发票重复报销或重复入库',
  `approve_result` tinyint NOT NULL COMMENT '审批结果：1通过 0驳回',
  `in_qty` int NOT NULL COMMENT '入库数量：不得大于审批通过数量',
  `stock_qty` int NOT NULL COMMENT '结存数量：由入库与出库累计得出',
  `change_type` tinyint NOT NULL COMMENT '变动类型：1入库 2出库',
  `change_qty` int NOT NULL COMMENT '变动数量：入库为正，出库为负',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`rk_no`),
  UNIQUE KEY `uk_asset_in_no` (`in_no`),
  UNIQUE KEY `uk_asset_appr_no` (`appr_no`),
  UNIQUE KEY `uk_asset_flow_no` (`flow_no`),
  UNIQUE KEY `uk_asset_id` (`asset_id`),
  UNIQUE KEY `uk_asset_invoice_no` (`invoice_no`),
  KEY `idx_asset_emp` (`emp_id`),
  CONSTRAINT `ck_asset_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `ck_asset_approve_result` CHECK ((`approve_result` in (0,1))),
  CONSTRAINT `ck_asset_change_sign` CHECK ((((`change_type` = 1) and (`change_qty` > 0)) or ((`change_type` = 2) and (`change_qty` < 0)))),
  CONSTRAINT `ck_asset_change_type` CHECK ((`change_type` in (1,2))),
  CONSTRAINT `ck_asset_in_qty` CHECK (((`in_qty` > 0) and (`in_qty` <= `qty`))),
  CONSTRAINT `ck_asset_price` CHECK ((`price` >= 0)),
  CONSTRAINT `ck_asset_qty` CHECK ((`qty` > 0)),
  CONSTRAINT `ck_asset_stock_qty` CHECK ((`stock_qty` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产入库登记表（第8组）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_borrow_return`
DROP TABLE IF EXISTS `mat_borrow_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_borrow_return` (
  `borrow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '借用单主键',
  `borrow_no` varchar(24) NOT NULL COMMENT '借用单号 JY+年月日+四位流水',
  `borrower_id` bigint NOT NULL COMMENT '借用人，关联 sys_user',
  `material_id` bigint NOT NULL COMMENT '借用物资，一单一物资',
  `warehouse_id` bigint DEFAULT NULL COMMENT '借出仓库，关联 warehouse；公共库存必填',
  `batch_no` varchar(32) DEFAULT NULL COMMENT '借出批次号：与物资、仓库共同定位实际出库批次',
  `borrow_qty` decimal(12,3) NOT NULL COMMENT '借用数量，大于 0',
  `returned_qty` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '累计完好归还数量，不得超过 borrow_qty',
  `written_off_qty` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '累计报损核销数量，与完好归还之和不得超过 borrow_qty',
  `due_date` date NOT NULL COMMENT '预计归还日期，用于逾期提醒',
  `borrow_time` datetime DEFAULT NULL COMMENT '借出时间',
  `return_time` datetime DEFAULT NULL COMMENT '实际归还时间',
  `condition_code` tinyint DEFAULT NULL COMMENT '归还状态：1完好至4缺失',
  `borrow_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审批至5已核销',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`borrow_id`),
  UNIQUE KEY `uk_borrow_no` (`borrow_no`),
  KEY `idx_borrower` (`borrower_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_status` (`borrow_status`),
  KEY `idx_borrow_batch` (`material_id`,`warehouse_id`,`batch_no`),
  CONSTRAINT `ck_borrow_return_qty` CHECK (((`returned_qty` >= 0) and (`written_off_qty` >= 0) and ((`returned_qty` + `written_off_qty`) <= `borrow_qty`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借用归还表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `borrow_return_item`
DROP TABLE IF EXISTS `borrow_return_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_return_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '归还明细主键',
  `borrow_id` bigint NOT NULL COMMENT '借用单主键，关联 borrow_return',
  `item_no` varchar(24) NOT NULL COMMENT '归还流水号 GH+年月日+四位流水，分次归还各不相同',
  `return_qty` decimal(12,3) NOT NULL COMMENT '本次归还数量，大于 0',
  `accept_result` tinyint NOT NULL COMMENT '验收结果：1完好入库 2损坏核销 3缺失核销',
  `warehouse_id` bigint NOT NULL COMMENT '归还入账仓库，关联 warehouse',
  `batch_no` varchar(32) NOT NULL COMMENT '归还入账批次号',
  `keeper_id` bigint NOT NULL COMMENT '经办库管员，关联 sys_user',
  `return_time` datetime NOT NULL COMMENT '本次归还时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_return_item_no` (`item_no`),
  KEY `idx_return_borrow` (`borrow_id`),
  KEY `idx_return_time` (`return_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借用归还明细表（分次归还）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `budget`
DROP TABLE IF EXISTS `budget`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `budget_no` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '预算科目号（引用第5组 budget.budget_no）',
  `dept_id` char(6) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门ID：财务按部门核定的预算额度（部门额度来源）',
  `fund_card_no` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经费卡号',
  `subject_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `frozen` decimal(12,2) DEFAULT '0.00' COMMENT '已冻结额度',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `budget_no` (`budget_no`),
  KEY `idx_budget_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预算科目（财务系统模拟）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_dept`
DROP TABLE IF EXISTS `mat_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_dept` (
  `org_id` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门主键（第4组师资系统 6 位组织编码）',
  `dept_code` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门编码',
  `dept_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `parent_id` char(6) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '上级部门，0为顶级',
  `leader_id` bigint DEFAULT NULL COMMENT '部门负责人，关联 sys_user.user_id',
  `annual_quota` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '部门年度领用额度（元）',
  `used_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '本年度已发生领用金额（元），出库时累加',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序',
  `dept_status` tinyint NOT NULL DEFAULT '1' COMMENT '部门状态：0停用 1启用',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`org_id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表（引用第4组师资系统 organization）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inbound_item`
DROP TABLE IF EXISTS `inbound_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbound_item` (
  `ii_id` bigint NOT NULL AUTO_INCREMENT,
  `in_id` bigint NOT NULL,
  `material_id` bigint NOT NULL,
  `batch_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `produce_date` date DEFAULT NULL,
  `expire_date` date DEFAULT NULL COMMENT '质保到期（临期预警）',
  `in_qty` decimal(12,3) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `qr_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资二维码标签内容',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`ii_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inbound_order`
DROP TABLE IF EXISTS `inbound_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbound_order` (
  `in_id` bigint NOT NULL AUTO_INCREMENT,
  `in_no` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'RK+年月日+4位流水',
  `acpt_id` bigint NOT NULL,
  `po_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `keeper_id` bigint DEFAULT NULL,
  `purchase_mode` tinyint NOT NULL DEFAULT '1' COMMENT '采购方式：1集中采购 2科研经费自购',
  `holder_id` bigint DEFAULT NULL COMMENT '实际保管人：科研经费自购时关联教师sys_user',
  `in_date` datetime DEFAULT NULL,
  `total_amount` decimal(12,2) DEFAULT NULL,
  `voucher_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报销/应付凭证号（第5组财务系统回填）',
  `in_status` tinyint DEFAULT '0' COMMENT '0待入库 1已入库',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`in_id`),
  UNIQUE KEY `in_no` (`in_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_loss_order`
DROP TABLE IF EXISTS `mat_loss_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_loss_order` (
  `loss_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报损单主键',
  `loss_no` varchar(24) NOT NULL COMMENT '报损单号 BS+年月日+四位流水',
  `source_type` tinyint NOT NULL COMMENT '来源类型：1借用报损 2盘亏报损 3到期或超限报废',
  `source_id` bigint DEFAULT NULL COMMENT '来源单据 ID：借用单或盘点记录',
  `material_id` bigint NOT NULL COMMENT '报损物资',
  `batch_no` varchar(32) DEFAULT NULL COMMENT '报损批次号：过期或超限报废必填',
  `warehouse_id` bigint DEFAULT NULL COMMENT '来源仓库：核销必须定位到具体仓库',
  `holder_id` bigint DEFAULT NULL COMMENT '持有人：个人保管仓报损时必填，关联 sys_user',
  `loss_qty` decimal(12,3) NOT NULL COMMENT '报损数量，大于 0',
  `reason` varchar(200) NOT NULL COMMENT '报损原因：损坏、遗失、过期',
  `approve_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审批 1通过 2驳回',
  `dispose_result` tinyint DEFAULT NULL COMMENT '处置结论：1赔偿 2免赔 3报废核销',
  `executed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已执行核销：0未执行 1已执行',
  `exec_time` datetime DEFAULT NULL COMMENT '核销执行时间',
  `exec_operator_id` bigint DEFAULT NULL COMMENT '核销经办人，关联 sys_user',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`loss_id`),
  UNIQUE KEY `uk_loss_no` (`loss_no`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_status` (`approve_status`),
  KEY `idx_loss_exec` (`executed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报损单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_material`
DROP TABLE IF EXISTS `mat_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_material` (
  `material_id` bigint NOT NULL AUTO_INCREMENT,
  `material_code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码',
  `material_name` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  `category_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '01办公 02实验 03后勤 04通用',
  `material_type` tinyint NOT NULL DEFAULT '1' COMMENT '物资类型：1低值易耗品 2低值耐用品',
  `unit` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `spec` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `material_status` tinyint NOT NULL DEFAULT '1' COMMENT '物资状态：1启用 0停用',
  `unit_code` varchar(20) DEFAULT NULL COMMENT '计量单位编码',
  `ref_price` decimal(12,2) NOT NULL COMMENT '参考单价',
  `is_controlled` tinyint NOT NULL DEFAULT '0' COMMENT '1管控物资（需实验室负责人加签）',
  `service_life_years` smallint DEFAULT NULL COMMENT '限用年限（年）：低值耐用品必填，到期进入报废评估',
  `shelf_life_days` smallint DEFAULT NULL COMMENT '保质期天数：需保质期管理的易耗品填写，实际到期日按批次记录',
  `safe_stock` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '安全库存下限（预警补货依据）',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`material_id`),
  UNIQUE KEY `material_code` (`material_code`),
  UNIQUE KEY `uk_material_code` (`material_code`),
  CONSTRAINT `ck_material_service_life` CHECK (((`material_type` <> 2) or ((`service_life_years` is not null) and (`service_life_years` >= 1)))),
  CONSTRAINT `ck_material_shelf_life` CHECK (((`material_type` <> 1) or (`shelf_life_days` is not null)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物资目录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material_category`
DROP TABLE IF EXISTS `material_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类主键',
  `category_no` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类编码：与 sys_dict_item(material_category) 的 item_value 对应',
  `category_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级分类，0 为顶级',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_no` (`category_no`),
  KEY `idx_category_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物资分类表（子系统①维护）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material_dept_quota`
DROP TABLE IF EXISTS `material_dept_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_dept_quota` (
  `dept_id` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门，关联 dept.dept_id',
  `quota_period` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '额度所属年月，如 202611，按月核定额度',
  `month_quota` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '月度领用额度（元），由后勤处年初核定',
  `used_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '本月已用额度（元），出库时累加',
  `approve_by` bigint DEFAULT NULL COMMENT '额度核定人，关联 sys_user.user_id',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `fin_quota_id` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务额度编号',
  `fin_apply_no` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务申报单号',
  `submit_status` char(2) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '部门额度状态',
  `submit_at` datetime DEFAULT NULL COMMENT '提交财务组的时间',
  `fin_confirm_at` datetime DEFAULT NULL COMMENT '财务确认回写时间',
  `fin_opinion` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务确认意见或退回原因',
  PRIMARY KEY (`dept_id`,`quota_period`),
  KEY `idx_mdq_period` (`quota_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门月度领用额度表（报告表2-4）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material_inbound_interface`
DROP TABLE IF EXISTS `material_inbound_interface`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_inbound_interface` (
  `in_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '入库单号，关联 inbound_order.in_no',
  `material_id` bigint NOT NULL COMMENT '物资，关联 material.material_id',
  `in_price` decimal(10,2) NOT NULL COMMENT '入库单价（元），供第5组成本结转',
  `in_qty` decimal(12,3) NOT NULL COMMENT '本次入库数量',
  `in_amount` decimal(12,2) NOT NULL COMMENT '入库金额（元）=入库单价×入库数量',
  `in_date` date NOT NULL COMMENT '验收合格并登记入库的日期',
  `warehouse_id` bigint DEFAULT NULL COMMENT '入库仓库，关联 warehouse.warehouse_id',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`in_no`,`material_id`),
  KEY `idx_mii_material` (`material_id`),
  KEY `idx_mii_date` (`in_date`),
  KEY `idx_mii_warehouse` (`warehouse_id`),
  CONSTRAINT `ck_mii_in_amount` CHECK ((`in_amount` >= 0)),
  CONSTRAINT `ck_mii_in_price` CHECK ((`in_price` >= 0)),
  CONSTRAINT `ck_mii_in_qty` CHECK ((`in_qty` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物资入库成本接口表（第5组成本结转只读依据）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_out_order`
DROP TABLE IF EXISTS `mat_out_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_out_order` (
  `out_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单主键',
  `out_no` varchar(24) NOT NULL COMMENT '出库单号 CK+年月日+四位流水',
  `source_type` tinyint NOT NULL COMMENT '来源类型：1领用 2借用 3调拨',
  `source_id` bigint NOT NULL COMMENT '来源单据 ID',
  `warehouse_id` bigint NOT NULL COMMENT '出库仓库，关联 warehouse',
  `keeper_id` bigint NOT NULL COMMENT '经办库管员，关联 sys_user',
  `holder_id` bigint DEFAULT NULL COMMENT '持有人：个人保管仓出库时必填；公共出库为空',
  `material_id` bigint NOT NULL COMMENT '出库物资',
  `batch_no` varchar(32) NOT NULL DEFAULT 'LEGACY' COMMENT '出库批次号：按先到期先出选择',
  `out_qty` decimal(12,3) NOT NULL COMMENT '实发数量',
  `out_time` datetime NOT NULL COMMENT '出库时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`out_id`),
  UNIQUE KEY `uk_out_no` (`out_no`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_out_batch` (`material_id`,`warehouse_id`,`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order`
DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `po_id` bigint NOT NULL AUTO_INCREMENT,
  `po_no` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CD+年月日+4位流水',
  `plan_id` bigint DEFAULT NULL COMMENT '采购计划，关联 purchase_plan.plan_id',
  `pr_id` bigint DEFAULT NULL,
  `supplier_id` bigint NOT NULL,
  `material_id` bigint DEFAULT NULL COMMENT '采购物资，关联 material；多明细订单取首行物资',
  `po_qty` decimal(12,3) DEFAULT NULL COMMENT '下单数量合计，大于 0',
  `order_date` date DEFAULT NULL,
  `expect_date` date DEFAULT NULL,
  `contact_person` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `pay_type` tinyint DEFAULT '1',
  `invoice_type` tinyint DEFAULT '1',
  `po_status` tinyint DEFAULT '0' COMMENT '0待确认 1已确认 2部分到货 3已完成 4已取消',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '下单单价（元）；多明细订单取加权均价',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '订单金额（元）=下单数量×下单单价',
  `po_date` date DEFAULT NULL COMMENT '下单日期，不早于采购计划批准日期',
  `buyer_id` bigint DEFAULT NULL COMMENT '采购员，关联 sys_user.user_id',
  `accept_qty` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '累计验收数量，由到货验收回写',
  PRIMARY KEY (`po_id`),
  UNIQUE KEY `po_no` (`po_no`),
  KEY `idx_po_plan` (`plan_id`),
  KEY `idx_po_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order_item`
DROP TABLE IF EXISTS `purchase_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_item` (
  `poi_id` bigint NOT NULL AUTO_INCREMENT,
  `po_id` bigint NOT NULL,
  `material_id` bigint NOT NULL,
  `order_qty` decimal(12,3) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `arrived_qty` decimal(12,3) DEFAULT '0.000',
  `inbound_qty` decimal(12,3) DEFAULT '0.000',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  `plan_id` bigint DEFAULT NULL COMMENT '采购计划，关联 purchase_plan.plan_id',
  PRIMARY KEY (`poi_id`),
  KEY `idx_poi_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_purchase_plan`
DROP TABLE IF EXISTS `mat_purchase_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_purchase_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购计划主键',
  `plan_no` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT '计划单号 CG+年月日+四位流水',
  `plan_type` tinyint NOT NULL COMMENT '计划类型：1常规补货 2临时采购',
  `purchase_mode` tinyint NOT NULL DEFAULT '1' COMMENT '采购方式：1集中采购入公共库存 2科研经费自购登记入个人保管库存',
  `org_id` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请部门，关联 dept',
  `holder_id` bigint DEFAULT NULL COMMENT '实际保管人：科研经费自购时关联教师sys_user',
  `reimburse_voucher_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '科研经费自购验收合格登记后生成的报销凭证号，仅作财务报销依据',
  `material_id` bigint NOT NULL COMMENT '采购物资，一计划一物资',
  `plan_qty` decimal(12,3) NOT NULL COMMENT '计划数量，大于 0',
  `stock_qty` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '生成计划时的可用量',
  `supplier_id` bigint DEFAULT NULL COMMENT '建议供应商，可空表示待定',
  `expect_date` date DEFAULT NULL COMMENT '期望到货日期',
  `plan_status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1待审批 2已批准 3已下单 4已关闭',
  `approve_id` bigint DEFAULT NULL COMMENT '审批记录，关联 approve_record.record_id',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  UNIQUE KEY `uk_plan_reimburse_voucher` (`reimburse_voucher_no`),
  KEY `idx_material` (`material_id`),
  KEY `idx_dept` (`org_id`),
  KEY `idx_status` (`plan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_request`
DROP TABLE IF EXISTS `purchase_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_request` (
  `pr_id` bigint NOT NULL AUTO_INCREMENT,
  `pr_no` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CG+年月日+4位流水',
  `source_type` tinyint DEFAULT '1' COMMENT '1部门申购 2领用转申购 3安全库存预警 4实验室自备',
  `purchase_mode` tinyint NOT NULL DEFAULT '1' COMMENT '采购方式：1集中采购入公共库存 2科研经费自购登记入个人保管库存',
  `source_id` bigint DEFAULT NULL COMMENT '来源单据ID（领用单/补货计划）',
  `applicant_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  `holder_id` bigint DEFAULT NULL COMMENT '实际保管人：科研经费自购时关联教师sys_user，集中采购为空',
  `reimburse_voucher_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '科研经费自购验收合格登记后生成的报销凭证号，仅作财务报销依据',
  `warehouse_id` bigint DEFAULT NULL COMMENT '入库仓库 id（个人发票登记使用）',
  `supplier_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称（个人发票登记使用）',
  `registrar_user_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登记人工号（个人发票登记使用）',
  `usage_purpose` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用途说明',
  `budget_no` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '预算科目号（第5组）',
  `expect_date` date DEFAULT NULL,
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `priority` tinyint DEFAULT '1' COMMENT '1普通 2加急',
  `pr_status` tinyint DEFAULT '0' COMMENT '0草稿 1待审批 2已批准 3已驳回 4已转订单',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `audit_remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`pr_id`),
  UNIQUE KEY `pr_no` (`pr_no`),
  UNIQUE KEY `reimburse_voucher_no` (`reimburse_voucher_no`),
  UNIQUE KEY `uk_pr_reimburse_voucher` (`reimburse_voucher_no`),
  KEY `idx_pr_registrar` (`registrar_user_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_request_item`
DROP TABLE IF EXISTS `purchase_request_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_request_item` (
  `pri_id` bigint NOT NULL AUTO_INCREMENT,
  `pr_id` bigint NOT NULL,
  `material_id` bigint NOT NULL,
  `qty_apply` decimal(12,3) NOT NULL,
  `ref_price` decimal(12,2) NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `qty_confirmed` decimal(12,3) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`pri_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购申请明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_req_order`
DROP TABLE IF EXISTS `mat_req_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_req_order` (
  `req_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领用单主键',
  `req_no` varchar(24) NOT NULL COMMENT '领用单号 LY+年月日+四位流水',
  `applicant_id` bigint NOT NULL COMMENT '申请人，关联 sys_user',
  `org_id` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请部门，用于额度校验',
  `material_id` bigint NOT NULL COMMENT '领用物资，一单一物资',
  `qty_apply` decimal(12,3) NOT NULL COMMENT '申请数量，大于 0',
  `qty_approve` decimal(12,3) DEFAULT NULL COMMENT '核准数量，可部分批准',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '核准数量×参考单价',
  `req_status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿至7已关闭',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  `warehouse_id` bigint DEFAULT NULL COMMENT '子系统①扩展：申请仓库',
  `batch_no` varchar(32) DEFAULT NULL COMMENT '子系统①扩展：库存批次',
  `direction` varchar(8) DEFAULT NULL COMMENT '子系统①扩展：IN入库 / OUT出库',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '子系统①扩展：审核意见',
  `applicant_user_no` varchar(20) DEFAULT NULL COMMENT '子系统①扩展：申请人工号',
  PRIMARY KEY (`req_id`),
  UNIQUE KEY `uk_req_no` (`req_no`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_status` (`req_status`),
  KEY `idx_req_warehouse` (`warehouse_id`),
  KEY `idx_req_direction` (`direction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='领用单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_stock`
DROP TABLE IF EXISTS `mat_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_stock` (
  `material_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `holder_id` bigint DEFAULT NULL COMMENT '实际保管人：个人保管库存必填，公共库存为空',
  `batch_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库存批次号：无供应商批号时由入库单号生成',
  `expire_date` date DEFAULT NULL COMMENT '到期日期：需保质期管理的易耗品必填',
  `inbound_date` date NOT NULL COMMENT '验收登记日期：低值耐用品限用年限预警起算依据',
  `qty_on_hand` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '在库总量（含在借与预占）',
  `qty_available` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '公共库存可领量=在库-预占；个人保管库存固定为0',
  `qty_locked` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '预占量',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`material_id`,`warehouse_id`,`batch_no`),
  KEY `idx_stock_holder` (`holder_id`),
  KEY `idx_stock_material` (`material_id`),
  KEY `idx_stock_expire` (`expire_date`),
  CONSTRAINT `ck_stock_qty_balance` CHECK (((`qty_available` + `qty_locked`) <= `qty_on_hand`)),
  CONSTRAINT `ck_stock_qty_nonneg` CHECK (((`qty_on_hand` >= 0) and (`qty_available` >= 0) and (`qty_locked` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存表（物资+仓库+批次；借出物资计入在库但不计入可用/预占）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_stock_flow`
DROP TABLE IF EXISTS `mat_stock_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_stock_flow` (
  `flow_id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `batch_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库存批次号：与物资、仓库共同定位库存批次',
  `biz_type` tinyint NOT NULL COMMENT '1入库 2领用 3借用 4归还 5盘盈 6盘亏 7报废核销（借用/归还流水不计入在库总量增减）',
  `biz_id` bigint DEFAULT NULL COMMENT '业务单据ID',
  `biz_item_id` bigint NOT NULL DEFAULT '0' COMMENT '来源单据明细ID：整单多明细/分批出库/分次归还的幂等标识',
  `change_qty` decimal(12,3) NOT NULL COMMENT '正入负出',
  `operator_id` bigint NOT NULL COMMENT '操作人，关联 sys_user.user_id；0 表示系统迁移数据',
  `flow_time` datetime NOT NULL COMMENT '流水时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`flow_id`),
  UNIQUE KEY `uk_flow_biz_op` (`biz_type`,`biz_id`,`biz_item_id`),
  KEY `idx_material_flow` (`material_id`,`flow_time`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_material` (`material_id`,`flow_time`),
  KEY `idx_stock_flow_batch` (`material_id`,`warehouse_id`,`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stocktake_item`
DROP TABLE IF EXISTS `stocktake_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stocktake_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点明细主键',
  `stocktake_id` bigint NOT NULL COMMENT '盘点单，关联 stocktake_order',
  `material_id` bigint NOT NULL COMMENT '物资，关联 material',
  `batch_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '库存批次号，与物资、仓库共同定位批次',
  `book_quantity` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '账面数量',
  `actual_quantity` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '实盘数量',
  `diff_quantity` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '差异数量=实盘-账面，正盘盈负盘亏',
  `diff_reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '差异原因',
  `counted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已录入实盘：0未录入 1已录入',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_stocktake_batch` (`stocktake_id`,`material_id`,`batch_no`),
  KEY `idx_stocktake_item` (`stocktake_id`),
  KEY `idx_stocktake_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stocktake_order`
DROP TABLE IF EXISTS `stocktake_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stocktake_order` (
  `stocktake_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点单主键',
  `stocktake_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '盘点单号：PD+yyyyMMdd+4位流水',
  `warehouse_id` bigint NOT NULL COMMENT '盘点仓库，关联 warehouse',
  `stocktake_date` date NOT NULL COMMENT '盘点日期',
  `auditor_id` bigint DEFAULT NULL COMMENT '盘点负责人，关联 sys_user.user_id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '盘点状态：0待盘点 1进行中 2已完成',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`stocktake_id`),
  UNIQUE KEY `uk_stocktake_no` (`stocktake_no`),
  KEY `idx_stocktake_wh` (`warehouse_id`,`stocktake_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_supplier`
DROP TABLE IF EXISTS `mat_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_supplier` (
  `supplier_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `supplier_status` tinyint NOT NULL DEFAULT '1' COMMENT '合作状态：1合作中 0停止合作',
  `contact_person` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `address` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tax_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `star_level` tinyint DEFAULT '3' COMMENT '评级1-5星',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `supplier_name` (`supplier_name`),
  UNIQUE KEY `uk_supplier_name` (`supplier_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--

-- Table structure for table `sys_config`
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `config_value` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_group` varchar(40) COLLATE utf8mb4_general_ci DEFAULT '基础',
  `value_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'string' COMMENT 'string/number/boolean',
  `builtin` tinyint DEFAULT '0' COMMENT '1内置不可删除',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_item`
DROP TABLE IF EXISTS `sys_dict_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `item_value` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `item_label` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `item_color` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签颜色 primary/success/warning/danger/info',
  `sort_no` int DEFAULT '0',
  `item_status` tinyint DEFAULT '1',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典编码，如 material_category',
  `dict_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dict_status` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_login_log`
DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_no` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `user_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ip` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `device` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` tinyint DEFAULT '1' COMMENT '1成功 0失败',
  `message` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT '0',
  `menu_name` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `path` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端路由',
  `icon` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Element Plus 图标名',
  `perm_code` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识，如 admin:user:add',
  `menu_type` tinyint DEFAULT '2' COMMENT '1目录 2菜单 3按钮',
  `sort_no` int DEFAULT '0',
  `menu_status` tinyint DEFAULT '1',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_operation_log`
DROP TABLE IF EXISTS `mat_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NOT NULL COMMENT '操作人，关联 sys_user.user_id；0 表示系统任务',
  `user_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `module` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块：用户管理/角色权限/仓储作业等',
  `action` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '动作：新增/修改/删除/停用/重置密码等',
  `target` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作对象',
  `detail` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_value` text COLLATE utf8mb4_general_ci COMMENT '关键变更前快照',
  `after_value` text COLLATE utf8mb4_general_ci COMMENT '关键变更后快照',
  `ip` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` tinyint DEFAULT '1' COMMENT '1成功 0失败',
  `cost_ms` int DEFAULT NULL,
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_sys_role`
DROP TABLE IF EXISTS `mat_sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '与 sys_user.role_code 对应',
  `role_name` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `data_scope` varchar(30) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '全校' COMMENT '数据范围：全校/部门/本人',
  `role_status` tinyint NOT NULL DEFAULT '1' COMMENT '角色状态：0停用 1启用',
  `sort_no` int DEFAULT '0',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL COMMENT '最近一次修改时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-菜单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--

--

--
-- Table structure for table `mat_sys_user`
DROP TABLE IF EXISTS `mat_sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `person_no` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '工号/学号，全平台唯一，登录账号',
  `person_id` char(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联ID（新增）',
  `user_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '真实姓名',
  `user_type` tinyint NOT NULL DEFAULT '1' COMMENT '1教职工 2学生 3外部人员',
  `org_id` char(6) COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属部门，关联 dept',
  `role_code` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主角色编码：ADMIN/DEPT_HEAD/PURCHASER/STORE_KEEPER/APPLICANT/STUDENT',
  `title` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职称/身份：教授/讲师/实验员/学生',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `hire_date` date DEFAULT NULL COMMENT '入职/入学日期',
  `password` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '登录口令：16位盐$SHA-256加盐摘要(Base64)；兼容历史无盐 SHA-256 摘要',
  `pwd_need_change` tinyint DEFAULT '0' COMMENT '是否需要首次登录改密：1是 0否',
  `pwd_updated_at` datetime DEFAULT NULL COMMENT '最近一次修改密码时间',
  `source_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'LOCAL' COMMENT '身份来源：LOCAL补录 TEACHER师资 STUDENT学籍',
  `source_sync_at` datetime DEFAULT NULL COMMENT '最近一次从身份源同步的时间',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `user_status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0停用 1启用',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_no` (`person_no`),
  KEY `idx_dept` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户主档表（全平台共用）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mat_sys_user_role`
DROP TABLE IF EXISTS `mat_sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `granted_by` bigint NOT NULL COMMENT '授权人，关联 sys_user.user_id；自助注册记本人',
  `granted_at` datetime NOT NULL COMMENT '授权时间',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-角色（一人可多角色）';
/*!40101 SET character_set_client = @saved_cs_client */;

--

-- Table structure for table `mat_warehouse`
DROP TABLE IF EXISTS `mat_warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mat_warehouse` (
  `warehouse_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `warehouse_type` tinyint NOT NULL DEFAULT '1' COMMENT '仓库类型：1实体仓库 2虚拟个人保管仓',
  `location` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `keeper_id` bigint DEFAULT NULL COMMENT '默认库管员',
  `created_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `warehouse_name` (`warehouse_name`),
  UNIQUE KEY `uk_warehouse_name` (`warehouse_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warning_rule`
DROP TABLE IF EXISTS `warning_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warning_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警规则主键',
  `material_id` bigint NOT NULL COMMENT '物资，关联 material',
  `warehouse_id` bigint NOT NULL DEFAULT '0' COMMENT '仓库，0 表示适用于全部仓库',
  `warning_type` tinyint NOT NULL DEFAULT '1' COMMENT '预警类型：1安全库存 2临期 3到期报废',
  `min_quantity` decimal(12,3) DEFAULT NULL COMMENT '安全库存下限',
  `max_quantity` decimal(12,3) DEFAULT NULL COMMENT '库存上限',
  `expire_days` int DEFAULT NULL COMMENT '临期提前天数（warning_type=2）',
  `service_life_years` int DEFAULT NULL COMMENT '到期报废年限（warning_type=3，为空时取 material.service_life_years）',
  `enable` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0停用 1启用',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_warning_rule` (`material_id`,`warehouse_id`,`warning_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预警规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_teacher`（课程模拟：师资系统，正式环境由接口替代）
--
DROP TABLE IF EXISTS `sys_teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_teacher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师工号（外部主键）',
  `teacher_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `dept_code` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门编码（外部），需映射到 mat_dept.dept_code',
  `dept_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门名称（冗余展示）',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-师资系统模拟表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_student`（课程模拟：学生系统）
--
DROP TABLE IF EXISTS `sys_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号（外部主键）',
  `student_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `class_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '班级（冗余展示）',
  `major` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-学生系统模拟表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_budget_subject`（课程模拟：财务预算科目）
--
DROP TABLE IF EXISTS `sys_budget_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_budget_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '预算科目号（外部主键）',
  `subject_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '预算余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_no` (`subject_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-财务系统模拟表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = 1;
