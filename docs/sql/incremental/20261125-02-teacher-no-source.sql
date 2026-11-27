-- ============================================================
-- 2026-11-25 工号来源切换为师资系统（sys_teacher）
-- 说明：
--   * 用户工号（mat_sys_user.person_no）统一取自师资系统模拟表 sys_teacher
--   * sys_teacher.dept_code 需与 mat_dept.dept_code 对齐，便于映射到部门 org_id
--   * 补齐师资系统在册名单（T0005~T0009），供新增账号/自助注册选择
-- 执行：mysql -u<user> -p material_mgmt < 20261125-02-teacher-no-source.sql
-- ============================================================

USE `material_mgmt`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 师资系统部门编码与主系统 mat_dept.dept_code 对齐（按部门名称匹配）
UPDATE `sys_teacher` t
  JOIN `mat_dept` d ON d.`dept_name` = t.`dept_name` AND d.`del_flag` = 0
   SET t.`dept_code` = d.`dept_code`
 WHERE t.`dept_code` IS NULL OR t.`dept_code` <> d.`dept_code`;

-- 2) 补齐师资系统在册名单（可重复执行）
INSERT IGNORE INTO `sys_teacher` (`teacher_no`, `teacher_name`, `dept_code`, `dept_name`, `phone`) VALUES
  ('T0001', '韦森宁', 'JWC', '教务处', '13800000001'),
  ('T0002', '李思滢', 'SYS', '实验室管理处', '13800000002'),
  ('T0003', '刘志豪', 'SYS', '实验室管理处', '13800000003'),
  ('T0004', '金日勋', 'HQ', '后勤保障处', '13800000004'),
  ('T0005', '韦森宁', 'JWC', '教务处', '13800000005'),
  ('T0006', '张老师', 'SYS', '实验室管理处', '13800000006'),
  ('T0007', '韦森聍', 'SYS', '实验室管理处', '13800000007'),
  ('T0008', '陈晨', 'TSG', '图书馆', '13800000008'),
  ('T0009', '周航', 'HQ', '后勤保障处', '13800000009');

SET FOREIGN_KEY_CHECKS = 1;
