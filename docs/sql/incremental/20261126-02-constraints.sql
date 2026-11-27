-- ============================================================
-- 2026-11-26 补齐 G08 完整性约束（引用镜像表 + 外键 + 触发器）
-- 依据《系统数据字典统一版 v1.4》03_主外键关系矩阵（G08 共 35 条）
-- 执行顺序：full-schema.sql → init-data.sql → 本脚本 → demo-data.sql
-- 可重复执行：CREATE ... IF NOT EXISTS / INSERT IGNORE 幂等；外键重复会报 Duplicate，跳过即可
-- ============================================================

USE `material_mgmt`;
SET NAMES utf8mb4;

-- ---------- A. 跨组引用镜像表（本组只读副本，供外键引用） ----------
CREATE TABLE IF NOT EXISTS org_organization (
  org_id char(6) NOT NULL COMMENT '组织编码',
  org_name varchar(60) DEFAULT NULL COMMENT '组织名称',
  org_type char(2) DEFAULT NULL COMMENT '组织类型',
  PRIMARY KEY (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一组织机构（G16 镜像）';

CREATE TABLE IF NOT EXISTS sys_account (
  user_id bigint NOT NULL COMMENT '统一账号ID',
  person_no varchar(20) DEFAULT NULL COMMENT '工号/学号',
  user_name varchar(50) DEFAULT NULL COMMENT '姓名',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一账号（G16 镜像）';

CREATE TABLE IF NOT EXISTS sys_person (
  person_no varchar(20) NOT NULL COMMENT '工号/学号',
  person_name varchar(50) DEFAULT NULL COMMENT '姓名',
  PRIMARY KEY (person_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自然人主索引（G16 镜像）';

CREATE TABLE IF NOT EXISTS sys_role (
  role_id bigint NOT NULL COMMENT '统一角色ID',
  role_code varchar(40) DEFAULT NULL COMMENT '角色编码',
  role_name varchar(50) DEFAULT NULL COMMENT '角色名称',
  PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一角色（G16 镜像）';

CREATE TABLE IF NOT EXISTS hr_employee (
  emp_id char(10) NOT NULL COMMENT '工号',
  emp_name varchar(30) DEFAULT NULL COMMENT '姓名',
  PRIMARY KEY (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教职工主数据（G04 镜像）';

CREATE TABLE IF NOT EXISTS ast_asset (
  asset_id char(20) NOT NULL COMMENT '资产编号',
  asset_name varchar(60) DEFAULT NULL COMMENT '资产名称',
  PRIMARY KEY (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='固定资产台账（G07 镜像）';

CREATE TABLE IF NOT EXISTS fin_quota (
  quota_id varchar(40) NOT NULL COMMENT '财务额度编号',
  dept_id char(6) DEFAULT NULL COMMENT '部门',
  PRIMARY KEY (quota_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='财务额度（G05 镜像）';

CREATE TABLE IF NOT EXISTS fin_budget_apply (
  apply_no varchar(50) NOT NULL COMMENT '财务申报单号',
  dept_id char(6) DEFAULT NULL COMMENT '部门',
  PRIMARY KEY (apply_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='财务预算申报（G05 镜像）';

-- ---------- B. 镜像表初始化数据 ----------
INSERT IGNORE INTO mat_dept (org_id, dept_code, dept_name, parent_id, dept_status, sort_no)
  VALUES ('000000','ROOT','学校（根组织）','000000',1,0);
INSERT IGNORE INTO org_organization (org_id,org_name) SELECT org_id,dept_name FROM mat_dept;
INSERT IGNORE INTO sys_account (user_id,person_no,user_name) SELECT user_id,person_no,user_name FROM mat_sys_user;
INSERT IGNORE INTO sys_person (person_no,person_name) SELECT person_no,user_name FROM mat_sys_user WHERE person_no IS NOT NULL;
INSERT IGNORE INTO sys_role (role_id,role_code,role_name) SELECT role_id,role_code,role_name FROM mat_sys_role;
INSERT IGNORE INTO hr_employee (emp_id,emp_name) SELECT teacher_no,teacher_name FROM sys_teacher;
UPDATE mat_dept d JOIN org_organization o ON o.org_id=d.org_id SET d.dept_name=d.dept_name;

-- ---------- C. 外键（35 条，按字典 03 矩阵） ----------
ALTER TABLE `mat_approve_record` ADD CONSTRAINT fk_mat_approve_record_approver_id FOREIGN KEY (`approver_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_asset_inbound` ADD CONSTRAINT fk_mat_asset_inbound_appr_id FOREIGN KEY (`appr_id`) REFERENCES `hr_employee` (`emp_id`);
ALTER TABLE `mat_asset_inbound` ADD CONSTRAINT fk_mat_asset_inbound_asset_id FOREIGN KEY (`asset_id`) REFERENCES `ast_asset` (`asset_id`);
ALTER TABLE `mat_asset_inbound` ADD CONSTRAINT fk_mat_asset_inbound_emp_id FOREIGN KEY (`emp_id`) REFERENCES `hr_employee` (`emp_id`);
ALTER TABLE `mat_borrow_return` ADD CONSTRAINT fk_mat_borrow_return_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_borrow_return` ADD CONSTRAINT fk_mat_borrow_return_borrower_id FOREIGN KEY (`borrower_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_borrow_return` ADD CONSTRAINT fk_mat_borrow_return_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `mat_warehouse` (`warehouse_id`);
ALTER TABLE `mat_dept` ADD CONSTRAINT fk_mat_dept_org_id FOREIGN KEY (`org_id`) REFERENCES `org_organization` (`org_id`);
ALTER TABLE `mat_dept` ADD CONSTRAINT fk_mat_dept_parent_id FOREIGN KEY (`parent_id`) REFERENCES `mat_dept` (`org_id`);
ALTER TABLE `mat_dept` ADD CONSTRAINT fk_mat_dept_leader_id FOREIGN KEY (`leader_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_loss_order` ADD CONSTRAINT fk_mat_loss_order_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_loss_order` ADD CONSTRAINT fk_mat_loss_order_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `mat_warehouse` (`warehouse_id`);
ALTER TABLE `mat_out_order` ADD CONSTRAINT fk_mat_out_order_keeper_id FOREIGN KEY (`keeper_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_out_order` ADD CONSTRAINT fk_mat_out_order_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_out_order` ADD CONSTRAINT fk_mat_out_order_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `mat_warehouse` (`warehouse_id`);
ALTER TABLE `mat_purchase_plan` ADD CONSTRAINT fk_mat_purchase_plan_supplier_id FOREIGN KEY (`supplier_id`) REFERENCES `mat_supplier` (`supplier_id`);
ALTER TABLE `mat_purchase_plan` ADD CONSTRAINT fk_mat_purchase_plan_org_id FOREIGN KEY (`org_id`) REFERENCES `org_organization` (`org_id`);
ALTER TABLE `mat_purchase_plan` ADD CONSTRAINT fk_mat_purchase_plan_holder_id FOREIGN KEY (`holder_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_purchase_plan` ADD CONSTRAINT fk_mat_purchase_plan_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_req_order` ADD CONSTRAINT fk_mat_req_order_applicant_id FOREIGN KEY (`applicant_id`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `mat_req_order` ADD CONSTRAINT fk_mat_req_order_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_req_order` ADD CONSTRAINT fk_mat_req_order_org_id FOREIGN KEY (`org_id`) REFERENCES `org_organization` (`org_id`);
ALTER TABLE `mat_stock` ADD CONSTRAINT fk_mat_stock_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_stock` ADD CONSTRAINT fk_mat_stock_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `mat_warehouse` (`warehouse_id`);
ALTER TABLE `mat_stock_flow` ADD CONSTRAINT fk_mat_stock_flow_material_id FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`);
ALTER TABLE `mat_sys_role` ADD CONSTRAINT fk_mat_sys_role_role_id FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`);
ALTER TABLE `mat_sys_user` ADD CONSTRAINT fk_mat_sys_user_org_id FOREIGN KEY (`org_id`) REFERENCES `org_organization` (`org_id`);
ALTER TABLE `mat_sys_user` ADD CONSTRAINT fk_mat_sys_user_person_no FOREIGN KEY (`person_no`) REFERENCES `sys_person` (`person_no`);
ALTER TABLE `mat_sys_user` ADD CONSTRAINT fk_mat_sys_user_user_id FOREIGN KEY (`user_id`) REFERENCES `sys_account` (`user_id`);
ALTER TABLE `mat_sys_user_role` ADD CONSTRAINT fk_mat_sys_user_role_role_id FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`);
ALTER TABLE `mat_sys_user_role` ADD CONSTRAINT fk_mat_sys_user_role_user_id FOREIGN KEY (`user_id`) REFERENCES `sys_account` (`user_id`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_approve_by FOREIGN KEY (`approve_by`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_dept_id FOREIGN KEY (`dept_id`) REFERENCES `mat_dept` (`org_id`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_approve_by FOREIGN KEY (`approve_by`) REFERENCES `mat_sys_user` (`user_id`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_dept_id FOREIGN KEY (`dept_id`) REFERENCES `mat_dept` (`org_id`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_fin_apply_no FOREIGN KEY (`fin_apply_no`) REFERENCES `fin_budget_apply` (`apply_no`);
ALTER TABLE `material_dept_quota` ADD CONSTRAINT fk_material_dept_quota_fin_quota_id FOREIGN KEY (`fin_quota_id`) REFERENCES `fin_quota` (`quota_id`);

-- ---------- D. 触发器：工号/学号必须存在于师资/学籍系统 ----------
DROP TRIGGER IF EXISTS trg_user_no_check_ins;
DROP TRIGGER IF EXISTS trg_user_no_check_upd;
CREATE TRIGGER trg_user_no_check_ins BEFORE INSERT ON mat_sys_user FOR EACH ROW
SET NEW.person_no = IF(NEW.source_type='TEACHER' AND NOT EXISTS(SELECT 1 FROM sys_teacher t WHERE t.teacher_no=NEW.person_no),
  CAST('工号不存在于师资系统 sys_teacher.teacher_no' AS UNSIGNED),
  IF(NEW.source_type='STUDENT' AND NOT EXISTS(SELECT 1 FROM sys_student s WHERE s.student_no=NEW.person_no),
     CAST('学号不存在于学籍系统 sys_student.student_no' AS UNSIGNED), NEW.person_no));
CREATE TRIGGER trg_user_no_check_upd BEFORE UPDATE ON mat_sys_user FOR EACH ROW
SET NEW.person_no = IF(NEW.source_type='TEACHER' AND NOT EXISTS(SELECT 1 FROM sys_teacher t WHERE t.teacher_no=NEW.person_no),
  CAST('工号不存在于师资系统 sys_teacher.teacher_no' AS UNSIGNED),
  IF(NEW.source_type='STUDENT' AND NOT EXISTS(SELECT 1 FROM sys_student s WHERE s.student_no=NEW.person_no),
     CAST('学号不存在于学籍系统 sys_student.student_no' AS UNSIGNED), NEW.person_no));
