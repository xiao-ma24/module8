-- ============================================================
-- ②b 外部模拟表（可选，幂等）
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 来源：Gitee main 分支 docs/sql/（提交 b9fabd1 及后续修复）
-- 口径：系统数据字典统一版 v1.4 / G08
-- 说明：01 已包含这三张表的建表语句，本步仅用于单独补建/核对
-- 若 01 已执行，本步重复执行无副作用（CREATE IF NOT EXISTS + INSERT IGNORE）
-- ============================================================

-- 【安全护栏】强制切到本组库，防止在 mysql / information_schema 下误执行
USE `material_mgmt`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_teacher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_no` varchar(32) NOT NULL COMMENT '教师工号（外部主键）',
  `teacher_name` varchar(64) NOT NULL COMMENT '姓名',
  `dept_code` varchar(32) DEFAULT NULL COMMENT '部门编码（外部），需映射到 mat_dept.dept_code',
  `dept_name` varchar(128) DEFAULT NULL COMMENT '部门名称（冗余展示）',
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-师资系统模拟表';

CREATE TABLE IF NOT EXISTS `sys_student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(32) NOT NULL COMMENT '学号（外部主键）',
  `student_name` varchar(64) NOT NULL,
  `class_name` varchar(64) DEFAULT NULL COMMENT '班级（冗余展示）',
  `major` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-学生系统模拟表';

CREATE TABLE IF NOT EXISTS `sys_budget_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_no` varchar(32) NOT NULL COMMENT '预算科目号（外部主键）',
  `subject_name` varchar(128) NOT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '预算余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_no` (`subject_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部-财务系统模拟表';

-- 种子与 02 保持一致，重复执行不会覆盖已有工号
INSERT IGNORE INTO `sys_teacher` (`teacher_no`, `teacher_name`, `dept_code`, `dept_name`, `phone`) VALUES
  ('T0001', '王主任', 'JWC', '教务处', '13800002001'),
  ('T0002', '李思滢', 'SYS', '实验室管理处', '13800002002'),
  ('T0003', '刘志豪', 'SYS', '实验室管理处', '13800002003'),
  ('T0004', '金日勋', 'HQ', '后勤保障处', '13800002004'),
  ('T0005', '韦森宁', 'JWC', '教务处', '13800002005'),
  ('T0006', '张老师', 'SYS', '实验室管理处', '13800002006'),
  ('T0007', '韦森聍', 'SYS', '实验室管理处', '13800000007'),
  ('T0008', '陈晨', 'TSG', '图书馆', '13800000008'),
  ('T0010', '周航', 'HQ', '后勤保障处', '13800000010');

INSERT IGNORE INTO `sys_student` (`student_no`, `student_name`, `class_name`, `major`) VALUES
  ('S2024001', '张三', '计科2401', '计算机科学与技术'),
  ('S2024002', '李四', '软工2402', '软件工程');

INSERT IGNORE INTO `sys_budget_subject` (`subject_no`, `subject_name`, `balance`) VALUES
  ('B01', '教学耗材采购预算', 50000.00),
  ('B02', '实验室设备采购预算', 120000.00),
  ('B03', '后勤维修物料预算', 28000.00);
