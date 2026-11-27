-- ============================================================
-- ③ 演示数据：物资/库存/采购/领用/借用/盘点（部门编码 6 位字符串）
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 来源：Gitee main 分支 docs/sql/demo-data.sql（提交 fd5e619 及后续修复）
-- 口径：系统数据字典统一版 v1.4 / G08
-- 含：脚本末尾的 6 位编码兜底修正与自检（ORG编码非6位行数应为 0）
-- ============================================================

-- 【安全护栏】强制切到本组库，防止在 mysql / information_schema 下误执行
USE `material_mgmt`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- material_category: 4 行
INSERT IGNORE INTO `material_category` (`id`, `category_no`, `category_name`, `parent_id`, `sort_no`, `status`, `remark`, `created_at`, `updated_at`, `del_flag`) VALUES
(1, '01', '办公用品', 0, 1, 1, NULL, '2026-11-23T18:41:47', '2026-11-23T18:41:47', 0),
(2, '02', '实验耗材', 0, 2, 1, NULL, '2026-11-23T18:41:47', '2026-11-23T18:41:47', 0),
(3, '03', '后勤物资', 0, 3, 1, NULL, '2026-11-23T18:41:47', '2026-11-23T18:41:47', 0),
(4, '04', '通用物资', 0, 4, 1, NULL, '2026-11-23T18:41:47', '2026-11-23T18:41:47', 0);

-- mat_warehouse: 3 行
INSERT IGNORE INTO `mat_warehouse` (`warehouse_id`, `warehouse_name`, `warehouse_type`, `location`, `keeper_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, '耗材库', 1, '行政楼B1', 3, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, '实验器材库', 1, '实验楼101', 2, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(3, '后勤物料库', 1, '后勤楼1层', 4, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0);

-- mat_supplier: 6 行
INSERT IGNORE INTO `mat_supplier` (`supplier_id`, `supplier_name`, `supplier_status`, `contact_person`, `contact_phone`, `address`, `tax_no`, `star_level`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, '得力办公用品有限公司', 1, '陈经理', '13800001001', 'XX市科技园区得力大厦', '91330100MA27XXXX1A', 5, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, '蜀牛玻璃仪器厂', 1, '刘厂长', '13800001002', 'XX市蜀牛工业区', '91330100MA27XXXX2B', 5, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T13:02:13', 0),
(3, '晨光文具股份有限公司', 1, '周敏', '13800001003', 'XX市晨光产业园', '91330100MA27XXXX3C', 5, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T19:39:24', 0),
(4, '华骏实验设备有限公司', 1, '孙浩', '13800001004', 'XX市华骏科技城', '91330100MA27XXXX4D', 4, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T20:25:05', 0),
(5, '五交化建材经营部', 1, '赵老板', '13800001005', 'XX市建材市场12号', '91330100MA27XXXX5E', 3, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(6, '宏图数码科技', 0, '钱经理', '13800001006', 'XX市数码广场', '91330100MA27XXXX6F', 2, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0);

-- mat_dept: 4 行
INSERT IGNORE INTO `mat_dept` (`org_id`, `dept_code`, `dept_name`, `parent_id`, `leader_id`, `annual_quota`, `used_amount`, `sort_no`, `dept_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES ('000001','JWC','教务处','000000',5,20000.00,3500.00,1,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_dept` (`org_id`, `dept_code`, `dept_name`, `parent_id`, `leader_id`, `annual_quota`, `used_amount`, `sort_no`, `dept_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES ('000002','SYS','实验室管理处','000000',1,80000.00,22000.00,2,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_dept` (`org_id`, `dept_code`, `dept_name`, `parent_id`, `leader_id`, `annual_quota`, `used_amount`, `sort_no`, `dept_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES ('000003','HQ','后勤保障处','000000',4,30000.00,8000.00,3,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_dept` (`org_id`, `dept_code`, `dept_name`, `parent_id`, `leader_id`, `annual_quota`, `used_amount`, `sort_no`, `dept_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES ('000004','TSG','图书馆','000000',6,15000.00,2000.00,4,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);

-- mat_sys_role: 8 行
INSERT IGNORE INTO `mat_sys_role` (`role_id`, `role_code`, `role_name`, `data_scope`, `role_status`, `sort_no`, `remark`, `created_at`, `updated_at`) VALUES
(1, 'ADMIN', '系统管理员', '全校', 1, 1, '平台全部权限，含用户、角色、参数、审计', '2026-11-13T22:30:41', NULL),
(2, 'DEPT_HEAD', '部门负责人', '部门', 1, 2, '本部门数据，采购申请审批', '2026-11-13T22:30:41', NULL),
(3, 'PURCHASER', '采购员', '全校', 1, 3, '采购全流程单据', '2026-11-13T22:30:41', NULL),
(4, 'STORE_KEEPER', '仓库管理员', '全校', 1, 4, '验收、入库、出库、盘点', '2026-11-13T22:30:41', NULL),
(5, 'APPLICANT', '教职工/申请人', '本人', 1, 5, '本人申请、领用、借用', '2026-11-13T22:30:41', NULL),
(6, 'STUDENT', '学生', '本人', 1, 6, '本人领用与借用申请', '2026-11-13T22:30:41', NULL),
(7, 'CHECKER', '验收人', '全校', 1, 7, '到货验收、质量数量核验（与库管员岗位分离）', '2026-11-13T22:30:41', NULL),
(9, 'STOCKTAKER', '盘点员', '全校', 1, 8, '盘点任务、实盘录入、库存预警核实、补货建议、盘点差异登记（不含审批与直接调账）', '2026-11-23T15:50:56', NULL);

-- mat_sys_user: 8 行
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (1,NULL,'T0001','王主任',1,'000002','DEPT_HEAD','教授','13800002001','t0001@school.edu.cn',NULL,'ccc40871f61b5672$xyMD2RE5obfm2ip4ViNmCdLeZ7KG9v1zzVYXl0baI3I=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-25 21:13:18',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (2,NULL,'T0002','李思滢',1,'000002','PURCHASER','实验师','13800002002','t0002@school.edu.cn',NULL,'932eb26fe8757c59$Go/6UyetMk+iL2mEHExFnlFdw2f32j9ZXk3gtwuuXzQ=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-25 20:35:25',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (3,NULL,'T0003','刘志豪',1,'000002','STORE_KEEPER','库管员','13800002003','t0003@school.edu.cn',NULL,'36dc7d8f343bfbf2$eO91rXAz4d9pyEvvngmJ6SAjc2ldLueti5xPmX0cpCg=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-25 18:18:42',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (4,NULL,'T0004','金日勋',1,'000003','STOCKTAKER','教师','13800002004','t0004@school.edu.cn',NULL,'ecb749cb22c186c3$DDnCqQG/BcJgXOXhWbbg8yykq1Oa05fhlhTmJ6iRFrY=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-25 18:21:18',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (5,NULL,'T0005','韦森宁',1,'000001','ADMIN','信息中心管理员','13800002005','t0005@school.edu.cn',NULL,'2c7c086333ed0a7d$Od0qayUvtygElfLiiYz3nSlNwVCF58oCjJFofP8L97w=',0,NULL,'LOCAL',NULL,'2026-11-25 20:09:49',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (6,NULL,'T0006','张老师',1,'000002','APPLICANT','教师','13800002006','t0006@school.edu.cn',NULL,'aa3c8a68a7d372fe$uk3FVjm3wpyma8B5FRgYCGMThx5NcQB5Ao6JYi2WF90=',0,NULL,'LOCAL',NULL,'2026-11-25 18:17:52',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (7,NULL,'T0007','韦森聍',1,'000002','CHECKER','验收工程师','13800000007','weisenning@school.edu.cn',NULL,'776b4c363f99b5e5$blSm7N6ILH8t27LI4Fyc8CPfzP7WIuHPNtT9sZuWYEw=',0,NULL,'LOCAL',NULL,'2026-11-25 18:14:48',1,'到货验收与异常登记演示账号',NULL,'2026-11-13 22:30:50',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (9,NULL,'2024444','abc',1,'000001','PURCHASER',NULL,'11111111111',NULL,'2026-11-24','e11052023b419545$Iz0LBeAFiIQfuaKR1heaG1BJMW7ml9ZUKGS9pe3YqLQ=',0,NULL,'SELF',NULL,'2026-11-24 11:08:05',1,'自助注册申请，申请岗位=采购员','abc','2026-11-24 11:07:19',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_sys_user` (`user_id`, `person_id`, `person_no`, `user_name`, `user_type`, `org_id`, `role_code`, `title`, `phone`, `email`, `hire_date`, `password`, `pwd_need_change`, `pwd_updated_at`, `source_type`, `source_sync_at`, `last_login_at`, `user_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (10,NULL,'AT1111','123',1,'000003','APPLICANT',NULL,'12345678901',NULL,'2026-11-24','b19fb764a73763d3$KnljYQGbmd1aC8WdKskpfCuHuRZfV/p4VLGnNcDTOZg=',0,NULL,'SELF',NULL,NULL,1,'自助注册申请，申请岗位=教职工/申请人','123','2026-11-24 15:44:20',NULL,'2026-11-25 22:53:57',0);

-- mat_sys_user_role: 9 行
INSERT IGNORE INTO `mat_sys_user_role` (`user_id`, `role_id`, `granted_by`, `granted_at`) VALUES
(1, 2, 1, '2026-11-13T22:30:41'),
(2, 3, 2, '2026-11-13T22:30:41'),
(3, 4, 3, '2026-11-13T22:30:41'),
(4, 5, 4, '2026-11-13T22:30:41'),
(4, 9, 5, '2026-11-23T15:50:57'),
(5, 1, 5, '2026-11-13T22:30:41'),
(6, 5, 6, '2026-11-13T22:30:41'),
(7, 7, 5, '2026-11-13T22:30:50'),
(9, 3, 9, '2026-11-24T11:07:19');

-- material_dept_quota: 4 行
INSERT IGNORE INTO `material_dept_quota` (`dept_id`, `quota_period`, `month_quota`, `used_amount`, `approve_by`, `update_time`, `fin_quota_id`, `fin_apply_no`, `submit_status`, `submit_at`, `fin_confirm_at`, `fin_opinion`) VALUES ('000001','202611',1666.67,0.00,5,'2026-11-25 22:53:57',NULL,NULL,'0',NULL,NULL,NULL);
INSERT IGNORE INTO `material_dept_quota` (`dept_id`, `quota_period`, `month_quota`, `used_amount`, `approve_by`, `update_time`, `fin_quota_id`, `fin_apply_no`, `submit_status`, `submit_at`, `fin_confirm_at`, `fin_opinion`) VALUES ('000002','202611',6666.67,521.00,NULL,'2026-11-25 22:53:57',NULL,NULL,'0',NULL,NULL,NULL);
INSERT IGNORE INTO `material_dept_quota` (`dept_id`, `quota_period`, `month_quota`, `used_amount`, `approve_by`, `update_time`, `fin_quota_id`, `fin_apply_no`, `submit_status`, `submit_at`, `fin_confirm_at`, `fin_opinion`) VALUES ('000003','202611',2500.00,0.00,NULL,'2026-11-25 22:53:57',NULL,NULL,'0',NULL,NULL,NULL);
INSERT IGNORE INTO `material_dept_quota` (`dept_id`, `quota_period`, `month_quota`, `used_amount`, `approve_by`, `update_time`, `fin_quota_id`, `fin_apply_no`, `submit_status`, `submit_at`, `fin_confirm_at`, `fin_opinion`) VALUES ('000004','202611',1250.00,0.00,NULL,'2026-11-25 22:53:57',NULL,NULL,'0',NULL,NULL,NULL);

-- budget: 7 行（B01-B03 为采购科目预算；D01-D04 为财务按部门核定的年度额度，部门额度授权页引用）
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (1,'B01',NULL,'F2024-01','教学耗材采购预算',44323.00,28059.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 18:22:24',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (2,'B02',NULL,'F2024-02','实验器材采购预算',115890.00,95085.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 15:27:47',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (3,'B03',NULL,'F2024-03','后勤维修物料预算',28000.00,400.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (4,'D01','000001','F2026-D01','教务处部门年度预算',20000.00,0.00,NULL,'2026-11-25 19:43:28',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (5,'D02','000002','F2026-D02','化学实验室部门年度预算',80000.00,0.00,NULL,'2026-11-25 19:43:28',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (6,'D03','000003','F2026-D03','后勤处部门年度预算',30000.00,0.00,NULL,'2026-11-25 19:43:28',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `budget` (`id`, `budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (7,'D04','000004','F2026-D04','物理实验室部门年度预算',15000.00,0.00,NULL,'2026-11-25 19:43:28',NULL,'2026-11-25 22:53:57',0);

-- mat_material: 12 行
INSERT IGNORE INTO `mat_material` (`material_id`, `material_code`, `material_name`, `category_code`, `material_type`, `unit`, `ref_price`, `is_controlled`, `service_life_years`, `shelf_life_days`, `safe_stock`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`, `spec`, `material_status`, `unit_code`) VALUES
(1, '01-001', 'A4复印纸', '01', 1, '包', 25.00, 0, NULL, 1095, 100.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '包'),
(2, '01-002', '激光打印机墨盒', '01', 1, '个', 180.00, 1, NULL, 730, 20.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '个'),
(3, '01-003', '中性笔（盒装）', '01', 1, '盒', 15.00, 0, NULL, 1095, 50.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '盒'),
(4, '01-004', '订书机', '01', 2, '个', 12.00, 0, 3, NULL, 30.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '个'),
(5, '02-001', '锥形瓶250ml', '02', 2, '个', 8.50, 0, 5, NULL, 50.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '个'),
(6, '02-002', '容量瓶100ml', '02', 2, '个', 12.00, 0, 5, NULL, 40.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '个'),
(7, '02-003', '电子天平（0.01g）', '02', 2, '台', 1200.00, 1, 5, NULL, 2.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '台'),
(8, '02-004', '分析纯盐酸500ml', '02', 1, '瓶', 28.00, 1, NULL, 365, 10.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '瓶'),
(9, '02-005', '一次性乳胶手套', '02', 1, '盒', 22.00, 0, NULL, 1095, 60.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '盒'),
(10, '03-001', 'LED灯管', '03', 2, '支', 8.00, 0, 2, NULL, 80.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '支'),
(11, '03-002', '水龙头阀芯', '03', 2, '个', 5.00, 0, 2, NULL, 100.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '个'),
(12, '04-001', '拖把', '04', 2, '把', 15.00, 0, 2, NULL, 20.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T11:50:36', 0, NULL, '1', '把');

-- mat_stock: 23 行（含 3 条已过期批次，用于演示临期/过期预警）
INSERT IGNORE INTO `mat_stock` (`material_id`, `warehouse_id`, `holder_id`, `batch_no`, `expire_date`, `inbound_date`, `qty_on_hand`, `qty_available`, `qty_locked`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, NULL, 'INIT-01-001-A', NULL, '2026-08-15', 258.000, 258.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T15:30:38', 0),
(1, 2, NULL, 'INIT-01-001-B', NULL, '2026-09-14', 39.000, 39.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-24T13:05:37', 0),
(2, 1, NULL, 'INIT-01-002', NULL, '2026-08-15', 0.000, 0.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T19:43:23', 0),
(3, 1, NULL, 'INIT-01-003', NULL, '2026-08-15', 0.000, 0.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T20:30:44', 0),
(4, 1, NULL, 'RK202611230005-1', '2037-11-05', '2026-11-23', 10.000, 10.000, 0.000, NULL, '2026-11-23T19:39:57', NULL, '2026-11-23T19:39:57', 0),
(5, 1, NULL, 'RK202611230001-1', NULL, '2026-11-23', 10.000, 10.000, 0.000, NULL, '2026-11-23T19:21', NULL, '2026-11-23T19:21', 0),
(5, 2, NULL, 'INIT-02-001', NULL, '2026-05-17', 180.000, 172.000, 8.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(6, 1, NULL, 'RK202611240002-1', '2030-11-06', '2026-11-24', 8.000, 8.000, 0.000, NULL, '2026-11-24T12:34:49', NULL, '2026-11-24T12:34:49', 0),
(6, 2, NULL, 'INIT-02-002', NULL, '2026-05-17', 66.000, 66.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(7, 2, NULL, 'INIT-02-003', NULL, '2025-11-13', 3.000, 3.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T20:27:13', 0),
(8, 1, NULL, 'RK202611230006-1', '2027-11-11', '2026-11-23', 10.000, 10.000, 0.000, NULL, '2026-11-23T20:25:46', NULL, '2026-11-23T20:25:46', 0),
(8, 1, NULL, 'RK202611240005-1', '2027-01-26', '2026-11-24', 100.000, 100.000, 0.000, NULL, '2026-11-24T13:02:44', NULL, '2026-11-24T13:02:44', 0),
(8, 2, NULL, 'INIT-02-004', '2027-05-12', '2026-05-12', 8.000, 8.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(9, 2, NULL, 'INIT-02-005', '2028-07-05', '2025-07-06', 110.000, 110.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(10, 1, NULL, 'RK202611240003-1', '2032-11-10', '2026-11-24', 1.000, 1.000, 0.000, NULL, '2026-11-24T12:42:25', NULL, '2026-11-24T12:42:25', 0),
(10, 1, NULL, 'RK202611240004-1', '2031-11-06', '2026-11-24', 1.000, 1.000, 0.000, NULL, '2026-11-24T12:46:39', NULL, '2026-11-24T12:46:39', 0),
(10, 3, NULL, 'INIT-03-001', NULL, '2026-08-15', 150.000, 150.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(11, 3, NULL, 'INIT-03-002', NULL, '2026-08-15', 90.000, 90.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(12, 3, NULL, 'INIT-04-001', NULL, '2026-08-15', 25.000, 25.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(12, 3, 6, 'PER-6', NULL, '2026-11-23', 4.000, 0.000, 0.000, NULL, '2026-11-23T18:50:18', NULL, '2026-11-24T10:21:30', 0),
(8, 1, NULL, 'OLD-HCL-20250930', '2026-09-30', '2025-09-30', 6.000, 6.000, 0.000, NULL, '2026-11-25T09:10:00', NULL, '2026-11-25T09:10:00', 0),
(9, 2, NULL, 'OLD-GLV-20230620', '2026-06-19', '2023-06-20', 12.000, 12.000, 0.000, NULL, '2026-11-25T09:10:00', NULL, '2026-11-25T09:10:00', 0),
(2, 1, NULL, 'OLD-INK-20241020', '2026-10-20', '2024-10-20', 3.000, 3.000, 0.000, NULL, '2026-11-25T09:10:00', NULL, '2026-11-25T09:10:00', 0);

-- mat_stock_flow: 26 行
INSERT IGNORE INTO `mat_stock_flow` (`flow_id`, `material_id`, `warehouse_id`, `batch_no`, `biz_type`, `biz_id`, `biz_item_id`, `change_qty`, `operator_id`, `flow_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, 1, 'INIT-01-001-A', 2, 101, 1, -60.000, 3, '2026-10-19T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(2, 1, 2, 'INIT-01-001-B', 2, 102, 2, -20.000, 2, '2026-11-03T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(3, 2, 1, 'INIT-01-002', 2, 103, 3, -5.000, 3, '2026-10-26T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(4, 8, 2, 'INIT-02-004', 2, 104, 4, -6.000, 2, '2026-11-05T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(5, 9, 2, 'INIT-02-005', 2, 105, 5, -30.000, 2, '2026-10-29T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(6, 5, 2, 'INIT-02-001', 2, 106, 6, -40.000, 2, '2026-11-08T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(7, 5, 2, 'INIT-02-001', 1, 1, 7, 40.000, 3, '2026-10-04T22:30:40', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(8, 1, 1, 'INIT-01-001-A', 2, 2, 0, -2.000, 3, '2026-11-23T15:30:38', NULL, '2026-11-23T15:30:38', NULL, '2026-11-23T15:30:38', 0),
(10, 12, 3, 'DEFAULT', 1, 6, 9, 1.000, 0, '2026-11-23T18:50:18', NULL, '2026-11-23T18:50:18', NULL, '2026-11-23T18:50:18', 0),
(11, 5, 1, 'RK202611230001-1', 1, 3, 3, 10.000, 3, '2026-11-23T19:21:01', NULL, '2026-11-23T19:21', NULL, '2026-11-23T19:21', 0),
(12, 12, 3, 'DEFAULT', 1, 11, 14, 1.000, 0, '2026-11-23T19:24:38', NULL, '2026-11-23T19:24:38', NULL, '2026-11-23T19:24:38', 0),
(13, 4, 1, 'RK202611230005-1', 1, 7, 7, 10.000, 3, '2026-11-23T19:39:57', NULL, '2026-11-23T19:39:57', NULL, '2026-11-23T19:39:57', 0),
(14, 2, 1, 'INIT-01-002', 2, 3, 0, -1.000, 3, '2026-11-23T19:42:17', NULL, '2026-11-23T19:42:17', NULL, '2026-11-23T19:42:17', 0),
(15, 2, 1, 'INIT-01-002', 6, 4, 11, -14.000, 4, '2026-11-23T19:43:23', NULL, '2026-11-23T19:43:23', NULL, '2026-11-23T19:43:23', 0),
(16, 8, 1, 'RK202611230006-1', 1, 8, 8, 10.000, 3, '2026-11-23T20:25:46', NULL, '2026-11-23T20:25:46', NULL, '2026-11-23T20:25:46', 0),
(17, 3, 1, 'INIT-01-003', 2, 4, 0, -2.000, 3, '2026-11-23T20:26:07', NULL, '2026-11-23T20:26:07', NULL, '2026-11-23T20:26:07', 0),
(18, 7, 2, 'INIT-02-003', 3, 5, 0, 0.000, 3, '2026-11-23T20:26:21', NULL, '2026-11-23T20:26:21', NULL, '2026-11-23T20:26:21', 0),
(19, 7, 2, 'INIT-02-003', 4, 1, 0, 0.000, 3, '2026-11-23T20:27:14', NULL, '2026-11-23T20:27:13', NULL, '2026-11-23T20:27:13', 0),
(40, 3, 1, 'INIT-01-003', 6, 5, 12, -40.000, 4, '2026-11-23T20:30:44', NULL, '2026-11-23T20:30:44', NULL, '2026-11-23T20:30:44', 0),
(41, 12, 3, 'DEFAULT', 1, 18, 21, 1.000, 0, '2026-11-23T20:38:42', NULL, '2026-11-23T20:38:41', NULL, '2026-11-23T20:38:41', 0),
(42, 12, 3, 'PER-6', 1, 23, 26, 1.000, 0, '2026-11-24T10:21:31', NULL, '2026-11-24T10:21:30', NULL, '2026-11-24T10:21:30', 0),
(43, 6, 1, 'RK202611240002-1', 1, 10, 10, 8.000, 3, '2026-11-24T12:34:50', NULL, '2026-11-24T12:34:49', NULL, '2026-11-24T12:34:49', 0),
(44, 10, 1, 'RK202611240003-1', 1, 11, 11, 1.000, 3, '2026-11-24T12:42:25', NULL, '2026-11-24T12:42:25', NULL, '2026-11-24T12:42:25', 0),
(45, 10, 1, 'RK202611240004-1', 1, 12, 12, 1.000, 3, '2026-11-24T12:46:39', NULL, '2026-11-24T12:46:39', NULL, '2026-11-24T12:46:39', 0),
(46, 8, 1, 'RK202611240005-1', 1, 13, 13, 100.000, 3, '2026-11-24T13:02:45', NULL, '2026-11-24T13:02:44', NULL, '2026-11-24T13:02:44', 0),
(47, 1, 2, 'INIT-01-001-B', 6, 6, 13, -1.000, 4, '2026-11-24T13:05:38', NULL, '2026-11-24T13:05:37', NULL, '2026-11-24T13:05:37', 0);

-- mat_purchase_plan: 28 行
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (1,'CG202610300001',1,1,'000001',NULL,NULL,10,30.000,0.000,5,'2026-11-10',3,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (2,'CG202610250001',1,1,'000001',NULL,NULL,5,80.000,0.000,2,'2026-11-05',3,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (3,'CG202609200001',1,1,'000001',NULL,NULL,1,50.000,0.000,1,'2026-10-04',3,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (4,'CG202611230001',1,1,'000001',NULL,NULL,2,7.072,15.000,1,'2026-11-30',1,NULL,1,'2026-11-23 18:34:26',1,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (5,'CG202611230003',1,1,'000002',NULL,NULL,5,10.000,0.000,1,'2026-11-28',3,NULL,1,'2026-11-23 18:57:53',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (13,'CG202611230005',1,1,'000002',NULL,NULL,9,10.000,0.000,1,'2026-11-28',3,NULL,1,'2026-11-23 19:04:07',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (14,'CG202611230007',1,1,'000002',NULL,NULL,4,10.000,0.000,3,'2026-11-28',3,NULL,1,'2026-11-23 19:36:13',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (22,'CG202611230008',1,1,'000002',NULL,NULL,5,23.000,0.000,NULL,'2026-11-30',0,NULL,1,'2026-11-23 19:53:00',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (23,'CG202611230009',1,1,'000002',NULL,NULL,5,23.000,0.000,NULL,'2026-11-30',2,2,1,'2026-11-23 19:54:43',2,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (24,'CG202611230010',1,1,'000002',NULL,NULL,5,10.000,0.000,NULL,'2026-11-30',1,NULL,1,'2026-11-23 19:54:48',1,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (25,'CG202611230011',1,1,'000001',NULL,NULL,2,23.000,0.000,1,'2026-11-30',2,2,4,'2026-11-23 19:58:33',2,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (26,'CG202611230012',1,1,'000001',NULL,NULL,2,23.000,0.000,NULL,'2026-11-30',2,2,4,'2026-11-23 20:08:42',2,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (27,'CG202611230015',1,1,'000001',NULL,NULL,2,23.000,0.000,3,'2026-11-30',3,NULL,4,'2026-11-23 20:16:54',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (28,'CG202611230016',1,1,'000001',NULL,NULL,2,23.000,0.000,3,'2026-11-30',3,NULL,4,'2026-11-23 20:17:07',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (29,'CG202611230018',1,1,'000002',NULL,NULL,8,10.000,0.000,4,'2026-11-28',3,NULL,1,'2026-11-23 20:22:09',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (30,'CG202611230019',1,1,'000001',NULL,NULL,8,214.000,18.000,NULL,'2026-11-30',2,2,4,'2026-11-23 20:31:55',2,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (31,'CG202611230021',1,1,'000001',NULL,NULL,3,5.000,0.000,NULL,NULL,2,2,4,'2026-11-23 20:37:19',2,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (32,'CG202611230023',1,1,'000001',NULL,NULL,3,5.000,0.000,3,NULL,3,NULL,4,'2026-11-23 20:37:45',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (33,'CG202611230024',1,1,'000001',NULL,NULL,3,51.000,0.000,NULL,'2026-11-30',2,NULL,4,'2026-11-23 20:44:22',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (34,'CG202611230025',1,1,'000001',NULL,NULL,8,214.000,18.000,NULL,'2026-11-30',2,NULL,4,'2026-11-23 20:46:44',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (35,'CG202611230026',1,1,'000001',NULL,NULL,4,20.000,10.000,NULL,'2026-11-30',2,NULL,4,'2026-11-23 20:50:59',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (36,'CG202611240001',1,1,'000001',NULL,NULL,4,20.000,10.000,NULL,'2026-12-01',2,NULL,4,'2026-11-24 09:26:34',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (37,'CG202611240005',1,1,'000002',NULL,NULL,6,10.000,0.000,1,'2026-11-25',3,NULL,6,'2026-11-24 12:33:54',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (38,'CG202611240007',1,1,'000002',NULL,NULL,10,2.000,0.000,1,'2026-11-25',3,NULL,1,'2026-11-24 12:40:53',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (39,'CG202611240010',1,1,'000002',NULL,NULL,8,100.000,0.000,2,'2026-11-28',3,NULL,6,'2026-11-24 13:01:30',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (40,'CG202611240011',1,1,'000001',NULL,NULL,3,51.000,0.000,NULL,'2026-12-01',2,NULL,4,'2026-11-24 13:05:53',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (41,'CG202611240012',1,1,'000001',NULL,NULL,4,20.000,10.000,NULL,'2026-12-01',2,NULL,4,'2026-11-24 13:05:53',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (42,'CG202611240013',1,1,'000001',NULL,NULL,11,10.000,90.000,NULL,'2026-12-01',2,NULL,4,'2026-11-24 13:05:53',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (43,'CG202611240018',1,1,'000002',NULL,NULL,9,5.000,0.000,1,'2026-11-28',3,NULL,6,'2026-11-24 15:34:40',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (44,'CG202611240019',1,1,'000001',NULL,NULL,9,1055.000,115.000,NULL,'2026-12-01',2,NULL,4,'2026-11-24 15:39:45',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (45,'CG202611250001',1,1,'000001',NULL,NULL,9,1.000,115.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 08:45:23',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (46,'CG202611250005',1,1,'000002',NULL,NULL,1,1.000,0.000,1,NULL,3,NULL,6,'2026-11-25 08:46:00',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (47,'CG202611250007',1,1,'000002',NULL,NULL,9,1.000,0.000,4,'2026-11-26',3,NULL,1,'2026-11-25 10:42:49',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (48,'CG202611250008',1,1,'000001',NULL,NULL,1,2875.000,295.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 15:27:32',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (49,'CG202611250011',1,1,'000002',NULL,NULL,9,10.000,0.000,1,'2026-11-28',3,NULL,6,'2026-11-25 18:14:20',NULL,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (50,'CG202611250012',1,1,'000001',NULL,NULL,7,1.000,1.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 18:22:13',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (51,'CG202611250013',1,1,'000001',NULL,NULL,3,46.000,4.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 18:22:13',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (52,'CG202611250014',1,1,'000001',NULL,NULL,4,21.000,9.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 18:22:13',4,'2026-11-25 22:53:57',0);
INSERT IGNORE INTO `mat_purchase_plan` (`plan_id`, `plan_no`, `plan_type`, `purchase_mode`, `org_id`, `holder_id`, `reimburse_voucher_no`, `material_id`, `plan_qty`, `stock_qty`, `supplier_id`, `expect_date`, `plan_status`, `approve_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES (53,'CG202611250015',1,1,'000001',NULL,NULL,11,10.000,90.000,NULL,'2026-12-02',2,NULL,4,'2026-11-25 18:22:13',4,'2026-11-25 22:53:57',0);

-- purchase_request: 27 行
INSERT IGNORE INTO `purchase_request` (`pr_id`, `pr_no`, `source_type`, `purchase_mode`, `source_id`, `applicant_id`, `dept_id`, `holder_id`, `reimburse_voucher_no`, `warehouse_id`, `supplier_name`, `registrar_user_no`, `usage_purpose`, `budget_no`, `expect_date`, `total_amount`, `priority`, `pr_status`, `remark`, `audit_remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 'CG202611070001', 1, 1, NULL, 4, 3, NULL, NULL, NULL, NULL, NULL, '教学楼灯管更换', 'B03', '2026-11-20', 400.00, 1, 2, NULL, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T18:54:39', 0),
(2, 'CG202611080001', 1, 1, NULL, 6, 2, NULL, NULL, NULL, NULL, NULL, '化学实验室常规耗材补充', 'B02', '2026-11-18', 3560.00, 2, 2, NULL, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(3, 'CG202611090001', 1, 1, NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, '12月行政办公耗材', 'B01', '2026-12-13', 2500.00, 1, 2, NULL, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T18:54:34', 0),
(6, 'CG202611230001', 4, 2, NULL, 6, 2, 6, '121212121', 3, NULL, 'T0006', '实验室自备（个人发票登记，个人保管）', 'N/A', NULL, 15.00, 1, 2, NULL, NULL, 'T0006', '2026-11-23T18:50:04', 'system', '2026-11-24T12:18:40', 0),
(7, 'CG202611230002', 1, 1, NULL, 1, 2, NULL, NULL, NULL, NULL, NULL, '11月实验耗材补充', 'B01', '2026-11-28', 85.00, 2, 4, NULL, NULL, NULL, '2026-11-23T18:53:14', NULL, '2026-11-23T18:57:53', 0),
(9, 'CG202611230003', 1, 1, NULL, 1, 2, NULL, NULL, NULL, NULL, NULL, '办公用品补充（自审校验演示）', 'B01', '2026-12-05', 250.00, 2, 1, NULL, NULL, NULL, '2026-11-23T18:59:50', NULL, '2026-11-24T12:57:50', 0),
(10, 'CG202611230004', 1, 1, NULL, 1, 2, NULL, NULL, NULL, NULL, NULL, '11月实验耗材补充', 'B01', '2026-11-28', 220.00, 2, 4, NULL, NULL, NULL, '2026-11-23T19:03:05', NULL, '2026-11-23T19:04:07', 0),
(11, 'CG202611230005', 4, 2, NULL, 6, 2, 6, '121212', 3, NULL, 'T0006', '实验室自备（个人发票登记，个人保管）', 'N/A', NULL, 15.00, 1, 2, NULL, NULL, 'T0006', '2026-11-23T19:23:38', 'system', '2026-11-24T12:18:40', 0),
(12, 'CG202611230006', 1, 1, NULL, 1, 2, NULL, NULL, NULL, NULL, NULL, '补充', 'B01', '2026-11-28', 120.00, 1, 4, NULL, NULL, NULL, '2026-11-23T19:35:31', NULL, '2026-11-23T19:36:13', 0),
(13, 'CG202611230013', 3, 1, 26, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230012）', 'B01', '2026-11-30', 4140.00, 1, 4, '由补货计划 CG202611230012 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:15:23', NULL, '2026-11-23T20:16:55', 0),
(14, 'CG202611230014', 3, 1, 25, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230011）', 'B01', '2026-11-30', 4140.00, 1, 4, '由补货计划 CG202611230011 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:16:13', NULL, '2026-11-23T20:17:07', 0),
(15, 'CG202611230017', 1, 1, NULL, 1, 2, NULL, NULL, NULL, NULL, NULL, '11月实验耗材', 'B01', '2026-11-28', 280.00, 1, 4, NULL, NULL, NULL, '2026-11-23T20:21:02', NULL, '2026-11-23T20:22:09', 0),
(16, 'CG202611230020', 3, 1, 30, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230019）', 'B01', '2026-11-30', 5992.00, 1, 1, '由补货计划 CG202611230019 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:36:26', NULL, '2026-11-23T20:36:26', 0),
(17, 'CG202611230022', 3, 1, 31, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230021）', 'B01', NULL, 75.00, 1, 4, '由补货计划 CG202611230021 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:37:19', NULL, '2026-11-23T20:37:45', 0),
(18, 'CG202611230023', 4, 2, NULL, 6, 2, 6, '11111111', 3, NULL, 'T0006', '实验室自备（个人发票登记，个人保管）', 'N/A', NULL, 15.00, 1, 2, NULL, NULL, 'T0006', '2026-11-23T20:38:29', 'system', '2026-11-24T12:18:40', 0),
(19, 'CG202611230026', 3, 1, 33, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230024）', 'B01', '2026-11-30', 765.00, 1, 1, '由补货计划 CG202611230024 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:48:49', NULL, '2026-11-23T20:48:49', 0),
(20, 'CG202611230027', 3, 1, 34, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230025）', 'B01', '2026-11-30', 5992.00, 1, 1, '由补货计划 CG202611230025 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:48:49', NULL, '2026-11-23T20:48:49', 0),
(21, 'CG202611230028', 3, 1, 35, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611230026）', 'B01', '2026-11-30', 240.00, 1, 1, '由补货计划 CG202611230026 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-23T20:51:03', NULL, '2026-11-23T20:51:03', 0),
(22, 'CG202611240002', 3, 1, 36, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611240001）', 'B01', '2026-12-01', 240.00, 1, 1, '由补货计划 CG202611240001 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-24T09:26:39', NULL, '2026-11-24T09:26:39', 0),
(23, 'CG202611240003', 4, 2, NULL, 6, 2, 6, '111111111111', 3, NULL, 'T0006', '实验室自备（个人发票登记，个人保管）', 'N/A', NULL, 15.00, 1, 2, NULL, NULL, 'T0006', '2026-11-24T09:30:45', 'system', '2026-11-24T12:18:40', 0),
(24, 'CG202611240004', 1, 1, NULL, 6, 2, NULL, NULL, NULL, NULL, NULL, '物资急缺', 'B01', '2026-11-25', 120.00, 2, 4, NULL, NULL, NULL, '2026-11-24T12:32:39', NULL, '2026-11-24T12:33:54', 0),
(25, 'CG202611240006', 1, 1, NULL, 6, 2, NULL, NULL, NULL, NULL, NULL, '物资紧缺', 'B01', '2026-11-25', 16.00, 2, 4, NULL, NULL, NULL, '2026-11-24T12:40:32', NULL, '2026-11-24T12:49:11', 0),
(26, 'CG202611240008', 1, 1, NULL, 6, 2, NULL, NULL, NULL, NULL, NULL, '耗材补充', 'B01', '2026-11-28', 140.00, 1, 2, NULL, NULL, NULL, '2026-11-24T12:51:37', NULL, '2026-11-24T12:52:16', 0),
(29, 'CG202611240009', 1, 1, NULL, 6, 2, NULL, NULL, NULL, NULL, NULL, '12月耗材补充', 'B01', '2026-11-28', 2800.00, 1, 4, NULL, NULL, NULL, '2026-11-24T13:00:41', NULL, '2026-11-24T13:01:30', 0),
(30, 'CG202611240014', 3, 1, 42, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611240013）', 'B01', '2026-12-01', 50.00, 1, 1, '由补货计划 CG202611240013 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0),
(31, 'CG202611240015', 3, 1, 41, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611240012）', 'B01', '2026-12-01', 240.00, 1, 1, '由补货计划 CG202611240012 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0),
(32, 'CG202611240016', 3, 1, 40, 4, 1, NULL, NULL, NULL, NULL, NULL, '库存预警补货（计划 CG202611240011）', 'B01', '2026-12-01', 765.00, 1, 1, '由补货计划 CG202611240011 自动转入，请采购员审批后生成采购订单', NULL, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0);

-- purchase_request_item: 30 行
INSERT IGNORE INTO `purchase_request_item` (`pri_id`, `pr_id`, `material_id`, `qty_apply`, `ref_price`, `amount`, `qty_confirmed`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, 10, 50.000, 8.00, 400.00, 50.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, 2, 5, 120.000, 8.50, 1020.00, 120.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(3, 2, 6, 80.000, 12.00, 960.00, 80.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(4, 2, 8, 20.000, 28.00, 560.00, 20.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(5, 2, 9, 50.000, 22.00, 1100.00, 50.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(6, 3, 1, 100.000, 25.00, 2500.00, 100.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(9, 6, 12, 1.000, 15.00, 15.00, NULL, 6, '2026-11-23T18:50:04', 6, '2026-11-23T18:50:04', 0),
(10, 7, 5, 10.000, 8.50, 85.00, 10.000, NULL, '2026-11-23T18:53:14', NULL, '2026-11-23T18:53:14', 0),
(12, 9, 1, 10.000, 25.00, 250.00, 10.000, NULL, '2026-11-23T18:59:50', NULL, '2026-11-23T18:59:50', 0),
(13, 10, 9, 10.000, 22.00, 220.00, 10.000, NULL, '2026-11-23T19:03:05', NULL, '2026-11-23T19:03:05', 0),
(14, 11, 12, 1.000, 15.00, 15.00, NULL, 6, '2026-11-23T19:23:38', 6, '2026-11-23T19:23:38', 0),
(15, 12, 4, 10.000, 12.00, 120.00, 10.000, NULL, '2026-11-23T19:35:31', NULL, '2026-11-23T19:35:31', 0),
(16, 13, 2, 23.000, 180.00, 4140.00, 23.000, NULL, '2026-11-23T20:15:23', NULL, '2026-11-23T20:15:23', 0),
(17, 14, 2, 23.000, 180.00, 4140.00, 23.000, NULL, '2026-11-23T20:16:13', NULL, '2026-11-23T20:16:13', 0),
(18, 15, 8, 10.000, 28.00, 280.00, 10.000, NULL, '2026-11-23T20:21:02', NULL, '2026-11-23T20:21:02', 0),
(19, 16, 8, 214.000, 28.00, 5992.00, 214.000, NULL, '2026-11-23T20:36:26', NULL, '2026-11-23T20:36:26', 0),
(20, 17, 3, 5.000, 15.00, 75.00, 5.000, NULL, '2026-11-23T20:37:19', NULL, '2026-11-23T20:37:19', 0),
(21, 18, 12, 1.000, 15.00, 15.00, NULL, 6, '2026-11-23T20:38:29', 6, '2026-11-23T20:38:29', 0),
(22, 19, 3, 51.000, 15.00, 765.00, 51.000, NULL, '2026-11-23T20:48:49', NULL, '2026-11-23T20:48:49', 0),
(23, 20, 8, 214.000, 28.00, 5992.00, 214.000, NULL, '2026-11-23T20:48:49', NULL, '2026-11-23T20:48:49', 0),
(24, 21, 4, 20.000, 12.00, 240.00, 20.000, NULL, '2026-11-23T20:51:03', NULL, '2026-11-23T20:51:03', 0),
(25, 22, 4, 20.000, 12.00, 240.00, 20.000, NULL, '2026-11-24T09:26:39', NULL, '2026-11-24T09:26:39', 0),
(26, 23, 12, 1.000, 15.00, 15.00, NULL, 6, '2026-11-24T09:30:45', 6, '2026-11-24T09:30:45', 0),
(27, 24, 6, 10.000, 12.00, 120.00, 10.000, NULL, '2026-11-24T12:32:39', NULL, '2026-11-24T12:32:39', 0),
(28, 25, 10, 2.000, 8.00, 16.00, 2.000, NULL, '2026-11-24T12:40:32', NULL, '2026-11-24T12:40:32', 0),
(29, 26, 8, 5.000, 28.00, 140.00, 5.000, NULL, '2026-11-24T12:51:37', NULL, '2026-11-24T12:51:37', 0),
(30, 29, 8, 100.000, 28.00, 2800.00, 100.000, NULL, '2026-11-24T13:00:41', NULL, '2026-11-24T13:00:41', 0),
(31, 30, 11, 10.000, 5.00, 50.00, 10.000, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0),
(32, 31, 4, 20.000, 12.00, 240.00, 20.000, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0),
(33, 32, 3, 51.000, 15.00, 765.00, 51.000, NULL, '2026-11-24T13:05:59', NULL, '2026-11-24T13:05:59', 0);

-- purchase_order: 13 行
INSERT IGNORE INTO `purchase_order` (`po_id`, `po_no`, `plan_id`, `pr_id`, `supplier_id`, `material_id`, `po_qty`, `order_date`, `expect_date`, `contact_person`, `contact_phone`, `total_amount`, `pay_type`, `invoice_type`, `po_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`, `unit_price`, `amount`, `po_date`, `buyer_id`, `accept_qty`) VALUES
(1, 'CD202610300001', 1, NULL, 5, 10, 30.000, '2026-11-03', '2026-11-10', '赵老板', '13800001005', 240.00, 1, 2, 1, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 8.00, 240.00, '2026-11-03', NULL, 0.000),
(2, 'CD202610250001', 2, NULL, 2, 5, 80.000, '2026-10-29', '2026-11-05', '刘厂长', '13800001002', 680.00, 3, 1, 2, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 8.50, 680.00, '2026-10-29', NULL, 80.000),
(3, 'CD202609200001', 3, NULL, 1, 1, 50.000, '2026-09-29', '2026-10-04', '陈经理', '13800001001', 1250.00, 1, 1, 3, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 25.00, 1250.00, '2026-09-29', NULL, 50.000),
(4, 'PO202611230001', 5, 7, 1, 5, 10.000, '2026-11-23', '2026-11-28', '陈经理', '13800001001', 85.00, 1, 1, 3, NULL, NULL, '2026-11-23T18:57:53', NULL, '2026-11-23T19:23:58', 0, 8.50, 85.00, '2026-11-23', 2, 20.000),
(5, 'PO202611230002', 13, 10, 1, 9, 10.000, '2026-11-23', '2026-11-28', '陈经理', '13800001001', 220.00, 1, 1, 2, NULL, NULL, '2026-11-23T19:04:07', NULL, '2026-11-23T19:37:15', 0, 22.00, 220.00, '2026-11-23', 2, 10.000),
(6, 'PO202611230003', 14, 12, 3, 4, 10.000, '2026-11-23', '2026-11-28', '周敏', '13800001003', 120.00, 1, 1, 3, NULL, NULL, '2026-11-23T19:36:13', NULL, '2026-11-24T12:22:46', 0, 12.00, 120.00, '2026-11-23', 2, 10.000),
(7, 'PO202611230004', 27, 13, 3, 2, 23.000, '2026-11-23', '2026-11-30', '周敏', '13800001003', 4140.00, 1, 1, 1, NULL, NULL, '2026-11-23T20:16:54', NULL, '2026-11-23T20:17:20', 0, 180.00, 4140.00, '2026-11-23', 2, 0.000),
(8, 'PO202611230005', 28, 14, 3, 2, 23.000, '2026-11-23', '2026-11-30', '周敏', '13800001003', 4140.00, 1, 1, 1, NULL, NULL, '2026-11-23T20:17:07', NULL, '2026-11-23T20:17:22', 0, 180.00, 4140.00, '2026-11-23', 2, 0.000),
(9, 'PO202611230006', 29, 15, 4, 8, 10.000, '2026-11-23', '2026-11-28', '孙浩', '13800001004', 280.00, 1, 1, 3, NULL, NULL, '2026-11-23T20:22:09', NULL, '2026-11-23T20:25:46', 0, 28.00, 280.00, '2026-11-23', 2, 10.000),
(10, 'PO202611230007', 32, 17, 3, 3, 5.000, '2026-11-23', NULL, '周敏', '13800001003', 75.00, 1, 1, 2, NULL, NULL, '2026-11-23T20:37:45', NULL, '2026-11-24T12:26:34', 0, 15.00, 75.00, '2026-11-23', 2, 5.000),
(11, 'PO202611240001', 37, 24, 1, 6, 10.000, '2026-11-24', '2026-11-25', '陈经理', '13800001001', 120.00, 1, 1, 2, NULL, NULL, '2026-11-24T12:33:54', NULL, '2026-11-24T12:34:29', 0, 12.00, 120.00, '2026-11-24', 2, 10.000),
(12, 'PO202611240002', 38, 25, 1, 10, 2.000, '2026-11-24', '2026-11-25', '陈经理', '13800001001', 16.00, 1, 1, 3, NULL, NULL, '2026-11-24T12:40:53', NULL, '2026-11-24T12:46:39', 0, 8.00, 16.00, '2026-11-24', 2, 2.000),
(13, 'PO202611240003', 39, 29, 2, 8, 100.000, '2026-11-24', '2026-11-28', '刘厂长', '13800001002', 2800.00, 1, 1, 3, NULL, NULL, '2026-11-24T13:01:30', NULL, '2026-11-24T13:02:44', 0, 28.00, 2800.00, '2026-11-24', 2, 100.000);

-- purchase_order_item: 13 行
INSERT IGNORE INTO `purchase_order_item` (`poi_id`, `po_id`, `material_id`, `order_qty`, `unit_price`, `amount`, `arrived_qty`, `inbound_qty`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`, `plan_id`) VALUES
(1, 1, 10, 30.000, 8.00, 240.00, 0.000, 0.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 1),
(2, 2, 5, 80.000, 8.50, 680.00, 80.000, 40.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 2),
(3, 3, 1, 50.000, 25.00, 1250.00, 50.000, 50.000, NULL, '2026-11-13T22:30:40', NULL, '2026-11-18T10:50:40', 0, 3),
(4, 4, 5, 10.000, 8.50, 85.00, 20.000, 10.000, NULL, '2026-11-23T18:57:53', NULL, '2026-11-23T19:23:58', 0, 5),
(5, 5, 9, 10.000, 22.00, 220.00, 10.000, 0.000, NULL, '2026-11-23T19:04:07', NULL, '2026-11-23T19:37:15', 0, 13),
(6, 6, 4, 10.000, 12.00, 120.00, 10.000, 10.000, NULL, '2026-11-23T19:36:13', NULL, '2026-11-24T12:22:46', 0, 14),
(7, 7, 2, 23.000, 180.00, 4140.00, 0.000, 0.000, NULL, '2026-11-23T20:16:54', NULL, '2026-11-23T20:16:54', 0, 27),
(8, 8, 2, 23.000, 180.00, 4140.00, 0.000, 0.000, NULL, '2026-11-23T20:17:07', NULL, '2026-11-23T20:17:07', 0, 28),
(9, 9, 8, 10.000, 28.00, 280.00, 10.000, 10.000, NULL, '2026-11-23T20:22:09', NULL, '2026-11-23T20:25:46', 0, 29),
(10, 10, 3, 5.000, 15.00, 75.00, 5.000, 0.000, NULL, '2026-11-23T20:37:45', NULL, '2026-11-24T12:26:34', 0, 32),
(11, 11, 6, 10.000, 12.00, 120.00, 10.000, 8.000, NULL, '2026-11-24T12:33:54', NULL, '2026-11-24T12:38:42', 0, 37),
(12, 12, 10, 2.000, 8.00, 16.00, 2.000, 2.000, NULL, '2026-11-24T12:40:53', NULL, '2026-11-24T12:46:39', 0, 38),
(13, 13, 8, 100.000, 28.00, 2800.00, 100.000, 100.000, NULL, '2026-11-24T13:01:30', NULL, '2026-11-24T13:02:44', 0, 39);

-- acceptance_order: 13 行
INSERT IGNORE INTO `acceptance_order` (`acpt_id`, `acpt_no`, `po_id`, `acpt_date`, `inspector_id`, `deliverer`, `photo_urls`, `acpt_score`, `acpt_status`, `quality_report`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 'YS202610280001', 2, '2026-11-05', 3, '王师傅', NULL, 4, 2, '**验收报告（编号：YS202610280001）**\n\n1. **物料验收**：锥形瓶，数量80个，验收结果全部合格。\n2. **包装情况**：供应商包装完好，无异常。\n3. **供应商评分**：4分。', NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-23T19:29:55', 0),
(2, 'YS202611230001', 4, '2026-11-23', 3, '送货师傅', NULL, NULL, 1, NULL, NULL, NULL, '2026-11-23T18:58:10', NULL, '2026-11-23T18:58:10', 0),
(3, 'YS202611230002', 4, '2026-11-23', 3, '送货师傅', NULL, NULL, 1, NULL, NULL, NULL, '2026-11-23T19:04:28', NULL, '2026-11-24T12:31:44', 0),
(4, 'YS202611230003', 4, '2026-11-23', 3, '送货师傅', NULL, 5, 2, '1. 验收结论：合格。  \n2. 本报告验收结果判定为“合格”，相关数字事实均按原报告保留。', NULL, NULL, '2026-11-23T19:04:31', NULL, '2026-11-23T19:30:31', 0),
(5, 'YS202611230004', 4, '2026-11-23', 3, '送货师傅', NULL, 5, 2, '验收报告（YS202611230004）\n\n一、基本信息\n- 关联订单：PO202611230001\n- 验收人：刘志豪\n- 验收日期：2026-11-23\n\n二、验收结论\n- 验收评分：5 分\n- 评分已按 6:4 权重计入供应商评级\n\n三、送检与合格情况\n- 合计送检数量：10\n- 合格数量：10\n- 合格率：100.0%', NULL, NULL, '2026-11-23T19:04:32', NULL, '2026-11-23T19:33:18', 0),
(6, 'YS202611230005', 5, '2026-11-23', 7, '送货师傅', NULL, 5, 2, '验收报告（YS202611230005）\n\n一、基本信息\n- 关联订单：PO202611230002\n- 验收人：韦森聍\n- 验收日期：2026-11-23\n- 验收评分：待评\n\n二、验收结果\n- 合计送检：10\n- 合格数量：10\n- 合格率：100.0%\n\n三、验收结论\n本次送检物料共10件，合格10件，合格率100.0%，验收结果合格。验收评分待评。', NULL, NULL, '2026-11-23T19:36:49', NULL, '2026-11-23T19:37:13', 0),
(10, 'YS202611230009', 6, '2026-11-23', 7, '送货师傅', NULL, 5, 2, '验收报告（编号：YS202611230009）\n\n一、基本信息\n- 关联订单：PO202611230003\n- 验收人：韦森聍\n- 验收日期：2026-11-23\n- 验收评分：待评\n\n二、验收结果\n- 合计送检数量：10\n- 合格数量：10\n- 合格率：100.0%', NULL, NULL, '2026-11-23T19:39:06', NULL, '2026-11-23T19:39:28', 0),
(12, 'YS202611230011', 9, '2026-11-23', 7, '送货师傅', NULL, 5, 2, '【系统生成】验收报告（YS202611230011）\n关联订单：PO202611230006　验收人：韦森聍　验收日期：2026-11-23\n验收评分：待评\n合计送检 10，合格 10，合格率 100.0%\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）', NULL, NULL, '2026-11-23T20:24:28', NULL, '2026-11-23T20:25:04', 0),
(13, 'YS202611240001', 10, '2026-11-24', 7, '送货师傅', NULL, 5, 3, '存在破损退回一件', NULL, NULL, '2026-11-24T12:25:05', NULL, '2026-11-24T12:25:51', 0),
(14, 'YS202611240002', 11, '2026-11-24', 7, '送货师傅', NULL, 5, 3, '【系统生成】验收报告（YS202611240002）\n关联订单：PO202611240001　验收人：韦森聍　验收日期：2026-11-24\n验收评分：5 分\n异常项：容量瓶100ml，不合格 2，原因：破损，处置：换货\n合计送检 10，合格 8，合格率 80.0%\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）', NULL, NULL, '2026-11-24T12:34:03', NULL, '2026-11-24T12:34:26', 0),
(16, 'YS202611240003', 12, '2026-11-24', NULL, '送货师傅', NULL, 5, 3, '【系统生成】验收报告（YS202611240003）\n关联订单：PO202611240002　验收人：null　验收日期：2026-11-24\n验收评分：5 分\n异常项：LED灯管，不合格 1，原因：，处置：换货\n合计送检 2，合格 1，合格率 50.0%\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）', NULL, NULL, '2026-11-24T12:40:59', NULL, '2026-11-24T12:41:17', 0),
(17, 'YS202611240004', 12, '2026-11-24', NULL, '送货师傅', NULL, 5, 2, '【系统生成】验收报告（YS202611240004）\n关联订单：PO202611240002　验收人：null　验收日期：2026-11-24\n验收评分：5 分\n合计送检 1，合格 1，合格率 100.0%\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）', NULL, NULL, '2026-11-24T12:46:01', NULL, '2026-11-24T12:46:10', 0),
(18, 'YS202611240005', 13, '2026-11-24', 7, '送货师傅', NULL, 5, 2, '【系统生成】验收报告（YS202611240005）\n关联订单：PO202611240003　验收人：韦森聍　验收日期：2026-11-24\n验收评分：5 分\n合计送检 100，合格 100，合格率 100.0%\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）', NULL, NULL, '2026-11-24T13:01:51', NULL, '2026-11-24T13:02:11', 0);

-- acceptance_item: 13 行
INSERT IGNORE INTO `acceptance_item` (`ai_id`, `acpt_id`, `poi_id`, `material_id`, `check_qty`, `pass_qty`, `fail_qty`, `fail_reason`, `handle_type`, `handle_status`, `handled_at`, `handled_by`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, 2, 5, 80.000, 80.000, 0.000, NULL, 1, 1, NULL, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:50', 0),
(2, 2, 4, 5, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T18:58:10', NULL, '2026-11-23T18:58:10', 0),
(3, 3, 4, 5, 10.000, 9.000, 1.000, '质量问题', 2, 0, NULL, NULL, NULL, '2026-11-23T19:04:28', NULL, '2026-11-23T19:04:28', 0),
(4, 4, 4, 5, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T19:04:31', NULL, '2026-11-23T19:04:31', 0),
(5, 5, 4, 5, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T19:04:32', NULL, '2026-11-23T19:04:32', 0),
(6, 6, 5, 9, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T19:36:49', NULL, '2026-11-23T19:36:49', 0),
(10, 10, 6, 4, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T19:39:06', NULL, '2026-11-23T19:39:06', 0),
(12, 12, 9, 8, 10.000, 10.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-23T20:24:28', NULL, '2026-11-23T20:24:28', 0),
(13, 13, 10, 3, 5.000, 4.000, 1.000, '破损', 2, 0, NULL, NULL, NULL, '2026-11-24T12:25:06', NULL, '2026-11-24T12:26:34', 0),
(14, 14, 11, 6, 10.000, 8.000, 2.000, '破损', 3, 0, NULL, NULL, NULL, '2026-11-24T12:34:03', NULL, '2026-11-24T12:38:42', 0),
(16, 16, 12, 10, 2.000, 1.000, 1.000, 'led灯不亮', 3, 1, '2026-11-24T12:45:55', '库管员', NULL, '2026-11-24T12:40:59', NULL, '2026-11-24T12:45:55', 0),
(17, 17, 12, 10, 1.000, 1.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-24T12:46:01', NULL, '2026-11-24T12:46:01', 0),
(18, 18, 13, 8, 100.000, 100.000, 0.000, '', 1, 1, NULL, NULL, NULL, '2026-11-24T13:01:51', NULL, '2026-11-24T13:01:51', 0);

-- inbound_order: 12 行
INSERT IGNORE INTO `inbound_order` (`in_id`, `in_no`, `acpt_id`, `po_id`, `warehouse_id`, `keeper_id`, `purchase_mode`, `holder_id`, `in_date`, `total_amount`, `voucher_no`, `in_status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 'RK202609250001', 1, 2, 2, 3, 1, NULL, '2026-10-04T22:30:40', 680.00, 'FP20260928001', 1, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, 'RK202610280001', 1, 2, 2, 3, 1, NULL, NULL, 340.00, NULL, 0, NULL, NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(3, 'RK202611230001', 5, 4, 1, 3, 1, NULL, '2026-11-23T19:21:01', 85.00, NULL, 1, NULL, NULL, '2026-11-23T19:08:19', NULL, '2026-11-23T19:21', 0),
(4, 'RK202611230002', 4, 4, 1, 3, 1, NULL, NULL, 85.00, NULL, 0, NULL, NULL, '2026-11-23T19:23:58', NULL, '2026-11-23T19:23:58', 0),
(5, 'RK202611230003', 6, 5, 1, 3, 1, NULL, NULL, 220.00, NULL, 0, NULL, NULL, '2026-11-23T19:37:15', NULL, '2026-11-23T19:37:15', 0),
(7, 'RK202611230005', 10, 6, 1, 3, 1, NULL, '2026-11-23T19:39:57', 120.00, NULL, 1, NULL, NULL, '2026-11-23T19:39:29', NULL, '2026-11-23T19:39:57', 0),
(8, 'RK202611230006', 12, 9, 1, 3, 1, NULL, '2026-11-23T20:25:46', 280.00, 'FP20261123245', 1, NULL, NULL, '2026-11-23T20:25:05', NULL, '2026-11-23T20:25:48', 0),
(9, 'RK202611240001', 13, 10, 1, 3, 1, NULL, NULL, 60.00, NULL, 0, NULL, NULL, '2026-11-24T12:26:34', NULL, '2026-11-24T12:29:43', 0),
(10, 'RK202611240002', 14, 11, 1, 3, 1, NULL, '2026-11-24T12:34:50', 96.00, NULL, 1, NULL, NULL, '2026-11-24T12:34:29', NULL, '2026-11-24T12:34:49', 0),
(11, 'RK202611240003', 16, 12, 1, 3, 1, NULL, '2026-11-24T12:42:25', 8.00, NULL, 1, NULL, NULL, '2026-11-24T12:41:33', NULL, '2026-11-24T12:42:25', 0),
(12, 'RK202611240004', 17, 12, 1, 3, 1, NULL, '2026-11-24T12:46:39', 8.00, NULL, 1, NULL, NULL, '2026-11-24T12:46:12', NULL, '2026-11-24T12:46:39', 0),
(13, 'RK202611240005', 18, 13, 1, 3, 1, NULL, '2026-11-24T13:02:45', 2800.00, NULL, 1, NULL, NULL, '2026-11-24T13:02:13', NULL, '2026-11-24T13:02:44', 0);

-- inbound_item: 12 行
INSERT IGNORE INTO `inbound_item` (`ii_id`, `in_id`, `material_id`, `batch_no`, `produce_date`, `expire_date`, `in_qty`, `unit_price`, `amount`, `qr_code`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, 5, 'RK202609250001-1', '2026-09-14', '2027-09-09', 40.000, 8.50, 340.00, 'CD202609200001-5-RK2026092500011', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, 2, 5, 'RK202610280001-1', '2026-10-14', '2027-10-09', 40.000, 8.50, 340.00, 'CD202609200001-5-RK2026102800011', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(3, 3, 5, 'RK202611230001-1', NULL, NULL, 10.000, 8.50, 85.00, 'PO202611230001-5-RK2026112300011', NULL, '2026-11-23T19:08:19', NULL, '2026-11-23T19:08:19', 0),
(4, 4, 5, 'RK202611230002-1', NULL, NULL, 10.000, 8.50, 85.00, 'PO202611230001-5-RK2026112300021', NULL, '2026-11-23T19:23:58', NULL, '2026-11-23T19:23:58', 0),
(5, 5, 9, 'RK202611230003-1', NULL, NULL, 10.000, 22.00, 220.00, 'PO202611230002-9-RK2026112300031', NULL, '2026-11-23T19:37:15', NULL, '2026-11-23T19:37:15', 0),
(7, 7, 4, 'RK202611230005-1', '2026-11-06', '2037-11-05', 10.000, 12.00, 120.00, 'PO202611230003-4-RK2026112300051', NULL, '2026-11-23T19:39:29', NULL, '2026-11-23T19:39:53', 0),
(8, 8, 8, 'RK202611230006-1', '2026-11-06', '2027-11-11', 10.000, 28.00, 280.00, 'PO202611230006-8-RK2026112300061', NULL, '2026-11-23T20:25:05', NULL, '2026-11-23T20:25:43', 0),
(9, 9, 3, 'RK202611240001-1', NULL, NULL, 4.000, 15.00, 60.00, 'PO202611230007-3-RK2026112400011', NULL, '2026-11-24T12:29:43', NULL, '2026-11-24T12:29:43', 0),
(10, 10, 6, 'RK202611240002-1', '2026-11-04', '2030-11-06', 8.000, 12.00, 96.00, 'PO202611240001-6-RK2026112400021', NULL, '2026-11-24T12:34:29', NULL, '2026-11-24T12:34:47', 0),
(11, 11, 10, 'RK202611240003-1', '2026-11-04', '2032-11-10', 1.000, 8.00, 8.00, 'PO202611240002-10-RK2026112400031', NULL, '2026-11-24T12:41:33', NULL, '2026-11-24T12:42:22', 0),
(12, 12, 10, 'RK202611240004-1', '2026-11-06', '2031-11-06', 1.000, 8.00, 8.00, 'PO202611240002-10-RK2026112400041', NULL, '2026-11-24T12:46:12', NULL, '2026-11-24T12:46:37', 0),
(13, 13, 8, 'RK202611240005-1', '2026-11-17', '2027-01-26', 100.000, 28.00, 2800.00, 'PO202611240003-8-RK2026112400051', NULL, '2026-11-24T13:02:13', NULL, '2026-11-24T13:02:38', 0);

-- material_inbound_interface: 8 行
INSERT IGNORE INTO `material_inbound_interface` (`in_no`, `material_id`, `in_price`, `in_qty`, `in_amount`, `in_date`, `warehouse_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
('RK202609250001', 5, 8.50, 40.000, 340.00, '2026-10-04', 2, NULL, '2026-11-17T20:17:28', NULL, '2026-11-17T20:21:15', 0),
('RK202611230001', 5, 8.50, 10.000, 85.00, '2026-11-23', 1, NULL, '2026-11-23T19:21', NULL, '2026-11-23T19:21', 0),
('RK202611230005', 4, 12.00, 10.000, 120.00, '2026-11-23', 1, NULL, '2026-11-23T19:39:57', NULL, '2026-11-23T19:39:57', 0),
('RK202611230006', 8, 28.00, 10.000, 280.00, '2026-11-23', 1, NULL, '2026-11-23T20:25:46', NULL, '2026-11-23T20:25:46', 0),
('RK202611240002', 6, 12.00, 8.000, 96.00, '2026-11-24', 1, NULL, '2026-11-24T12:34:49', NULL, '2026-11-24T12:34:49', 0),
('RK202611240003', 10, 8.00, 1.000, 8.00, '2026-11-24', 1, NULL, '2026-11-24T12:42:25', NULL, '2026-11-24T12:42:25', 0),
('RK202611240004', 10, 8.00, 1.000, 8.00, '2026-11-24', 1, NULL, '2026-11-24T12:46:39', NULL, '2026-11-24T12:46:39', 0),
('RK202611240005', 8, 28.00, 100.000, 2800.00, '2026-11-24', 1, NULL, '2026-11-24T13:02:44', NULL, '2026-11-24T13:02:44', 0);

-- mat_req_order: 5 行
INSERT IGNORE INTO `mat_req_order` (`req_id`, `req_no`, `applicant_id`, `org_id`, `material_id`, `qty_apply`, `qty_approve`, `amount`, `req_status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`, `warehouse_id`, `batch_no`, `direction`, `audit_remark`, `applicant_user_no`) VALUES
(1, 'LY202611230001', 6, '000002', 1, 2.000, 2.000, 50.00, 5, 6, '2026-11-23T15:29:57', 3, '2026-11-23T15:30:38', 0, NULL, NULL, NULL, NULL, NULL),
(2, 'LY202611230002', 1, '000002', 2, 1.000, 1.000, 180.00, 5, 1, '2026-11-23T19:18:49', 3, '2026-11-23T19:42:17', 0, NULL, NULL, NULL, NULL, NULL),
(3, 'LY202611230003', 6, '000002', 1, 1.000, 1.000, 25.00, 3, 6, '2026-11-23T19:34:32', 3, '2026-11-23T19:34:33', 0, NULL, NULL, NULL, NULL, NULL),
(4, 'LY202611230004', 6, '000002', 1, 2.000, NULL, 50.00, 1, 6, '2026-11-23T19:46:39', 6, '2026-11-23T19:46:39', 0, NULL, NULL, NULL, NULL, NULL),
(5, 'LY202611230005', 1, '000002', 3, 2.000, 2.000, 30.00, 6, 1, '2026-11-23T20:21:20', 1, '2026-11-24T10:26:15', 0, NULL, NULL, NULL, NULL, NULL);

-- mat_out_order: 4 行
INSERT IGNORE INTO `mat_out_order` (`out_id`, `out_no`, `source_type`, `source_id`, `warehouse_id`, `keeper_id`, `holder_id`, `material_id`, `batch_no`, `out_qty`, `out_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(2, 'CK202611230001', 1, 1, 1, 3, NULL, 1, 'INIT-01-001-A', 2.000, '2026-11-23T15:30:38', 3, '2026-11-23T15:30:38', 3, '2026-11-23T15:30:38', 0),
(3, 'CK202611230002', 1, 2, 1, 3, NULL, 2, 'INIT-01-002', 1.000, '2026-11-23T19:42:17', 3, '2026-11-23T19:42:17', 3, '2026-11-23T19:42:17', 0),
(4, 'CK202611230003', 1, 5, 1, 3, NULL, 3, 'INIT-01-003', 2.000, '2026-11-23T20:26:07', 3, '2026-11-23T20:26:07', 3, '2026-11-23T20:26:07', 0),
(5, 'CK202611230004', 2, 2, 2, 3, NULL, 7, 'INIT-02-003', 1.000, '2026-11-23T20:26:21', 3, '2026-11-23T20:26:21', 3, '2026-11-23T20:26:21', 0);

-- mat_borrow_return: 2 行
INSERT IGNORE INTO `mat_borrow_return` (`borrow_id`, `borrow_no`, `borrower_id`, `material_id`, `warehouse_id`, `batch_no`, `borrow_qty`, `borrow_time`, `returned_qty`, `written_off_qty`, `due_date`, `return_time`, `condition_code`, `borrow_status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 'JY202611230001', 6, 5, NULL, NULL, 2.000, '2026-11-23T19:34:06', 0.000, 0.000, '2026-12-10', NULL, NULL, 1, 6, '2026-11-23T19:34:06', 3, '2026-11-24T11:50:36', 0),
(2, 'JY202611230002', 1, 7, 2, 'INIT-02-003', 1.000, '2026-11-23T20:21:30', 1.000, 0.000, '2026-11-30', '2026-11-23T20:27:14', 1, 3, 1, '2026-11-23T20:21:30', 3, '2026-11-24T11:50:36', 0);

-- borrow_return_item: 1 行
INSERT IGNORE INTO `borrow_return_item` (`item_id`, `borrow_id`, `item_no`, `return_qty`, `accept_result`, `warehouse_id`, `batch_no`, `keeper_id`, `return_time`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 2, 'GH202611230001', 1.000, 1, 2, 'INIT-02-003', 3, '2026-11-23T20:27:14', '完好', 3, '2026-11-23T20:27:13', 3, '2026-11-23T20:27:13', 0);

-- stocktake_order: 6 行
INSERT IGNORE INTO `stocktake_order` (`stocktake_id`, `stocktake_no`, `warehouse_id`, `stocktake_date`, `auditor_id`, `status`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 'PD202611230001', 1, '2026-11-23', 1, 2, '｜审批意见：差异原因属实，同意调账', 1, '2026-11-23T19:00:37', 1, '2026-11-23T19:00:37', 0),
(2, 'PD202611230002', 2, '2026-11-23', 3, 2, '｜审批意见：差异原因属实，同意调账', 1, '2026-11-23T19:01:29', 1, '2026-11-23T19:01:29', 0),
(3, 'PD202611230003', 1, '2026-11-23', 4, 1, '权限校验演示', 3, '2026-11-23T19:09:53', 4, '2026-11-23T19:09:53', 0),
(4, 'PD202611230004', 1, '2026-11-23', 4, 2, '｜审批意见：差异原因属实，同意调账', 3, '2026-11-23T19:42:37', 4, '2026-11-23T19:42:37', 0),
(5, 'PD202611230005', 1, '2026-11-23', 4, 2, '｜审批意见：差异原因属实，同意调账', 3, '2026-11-23T20:27:51', 4, '2026-11-23T20:27:51', 0),
(6, 'PD202611240001', 2, '2026-11-24', 4, 2, '｜审批意见：差异原因属实，同意调账', 3, '2026-11-24T13:04:49', 4, '2026-11-24T13:04:49', 0);

-- stocktake_item: 13 行
INSERT IGNORE INTO `stocktake_item` (`item_id`, `stocktake_id`, `material_id`, `batch_no`, `book_quantity`, `actual_quantity`, `diff_quantity`, `diff_reason`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 1, 1, 'INIT-01-001-A', 258.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:00:37', NULL, '2026-11-23T19:00:37', 0),
(2, 2, 1, 'INIT-01-001-B', 40.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(3, 2, 5, 'INIT-02-001', 180.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(4, 2, 6, 'INIT-02-002', 66.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(5, 2, 7, 'INIT-02-003', 3.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(6, 2, 8, 'INIT-02-004', 8.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(7, 2, 9, 'INIT-02-005', 110.000, 0.000, 0.000, NULL, 1, '2026-11-23T19:01:29', NULL, '2026-11-23T19:01:29', 0),
(8, 3, 1, 'INIT-01-001-A', 258.000, 0.000, 0.000, NULL, 3, '2026-11-23T19:09:53', NULL, '2026-11-23T19:09:53', 0),
(9, 3, 2, 'INIT-01-002', 15.000, 0.000, 0.000, NULL, 3, '2026-11-23T19:09:53', NULL, '2026-11-23T19:09:53', 0),
(10, 3, 3, 'INIT-01-003', 42.000, 0.000, 0.000, NULL, 3, '2026-11-23T19:09:53', NULL, '2026-11-23T19:09:53', 0),
(11, 4, 2, 'INIT-01-002', 14.000, 0.000, -14.000, '领用未及时出库', 3, '2026-11-23T19:42:37', 4, '2026-11-23T19:42:37', 0),
(12, 5, 3, 'INIT-01-003', 40.000, 0.000, -40.000, '领用未出库', 3, '2026-11-23T20:27:51', 4, '2026-11-23T20:27:51', 0),
(13, 6, 1, 'INIT-01-001-B', 40.000, 39.000, -1.000, '领用时未出库', 3, '2026-11-24T13:04:49', 4, '2026-11-24T13:04:49', 0);

-- mat_approve_record: 20 行
INSERT IGNORE INTO `mat_approve_record` (`record_id`, `biz_type`, `biz_id`, `approver_id`, `approve_result`, `approved_at`, `opinion`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`) VALUES
(1, 4, 2, 1, 1, '2026-11-12T22:30:40', '同意，实验优先', NULL, '2026-11-13T22:30:40', NULL, '2026-11-13T22:30:40', 0),
(2, 1, 1, 1, 1, '2026-11-23T15:29:58', '同意', 1, '2026-11-23T15:29:57', 1, '2026-11-23T15:29:57', 0),
(3, 4, 7, 1, 1, '2026-11-23T18:54:08', '', NULL, '2026-11-23T18:54:07', NULL, '2026-11-23T18:54:07', 0),
(4, 4, 3, 1, 1, '2026-11-23T18:54:35', '', NULL, '2026-11-23T18:54:34', NULL, '2026-11-23T18:54:34', 0),
(5, 4, 1, 1, 1, '2026-11-23T18:54:39', '', NULL, '2026-11-23T18:54:39', NULL, '2026-11-23T18:54:39', 0),
(6, 4, 10, 2, 1, '2026-11-23T19:03:55', '', NULL, '2026-11-23T19:03:54', NULL, '2026-11-23T19:03:54', 0),
(7, 2, 1, 3, 1, '2026-11-23T19:34:11', '库存充足，同意借用', 3, '2026-11-23T19:34:11', 3, '2026-11-23T19:34:11', 0),
(8, 1, 3, 3, 1, '2026-11-23T19:34:33', '同意领用', 3, '2026-11-23T19:34:33', 3, '2026-11-23T19:34:33', 0),
(9, 4, 12, 2, 1, '2026-11-23T19:35:52', '', NULL, '2026-11-23T19:35:52', NULL, '2026-11-23T19:35:52', 0),
(10, 1, 2, 3, 1, '2026-11-23T19:42:12', '同意', 3, '2026-11-23T19:42:12', 3, '2026-11-23T19:42:12', 0),
(11, 4, 13, 2, 1, '2026-11-23T20:15:44', 'ok', NULL, '2026-11-23T20:15:43', NULL, '2026-11-23T20:15:43', 0),
(12, 4, 14, 2, 1, '2026-11-23T20:16:47', '', NULL, '2026-11-23T20:16:47', NULL, '2026-11-23T20:16:47', 0),
(13, 4, 15, 2, 1, '2026-11-23T20:21:56', '同意', NULL, '2026-11-23T20:21:55', NULL, '2026-11-23T20:21:55', 0),
(14, 1, 5, 3, 1, '2026-11-23T20:26:03', '同意', 3, '2026-11-23T20:26:03', 3, '2026-11-23T20:26:03', 0),
(15, 2, 2, 3, 1, '2026-11-23T20:26:17', '同意', 3, '2026-11-23T20:26:17', 3, '2026-11-23T20:26:17', 0),
(16, 4, 17, 2, 1, '2026-11-23T20:37:39', '', NULL, '2026-11-23T20:37:39', NULL, '2026-11-23T20:37:39', 0),
(17, 4, 24, 2, 1, '2026-11-24T12:33:42', '', NULL, '2026-11-24T12:33:42', NULL, '2026-11-24T12:33:42', 0),
(18, 4, 25, 2, 1, '2026-11-24T12:40:46', '', NULL, '2026-11-24T12:40:46', NULL, '2026-11-24T12:40:46', 0),
(19, 4, 26, 1, 1, '2026-11-24T12:52:17', '', NULL, '2026-11-24T12:52:16', NULL, '2026-11-24T12:52:16', 0),
(20, 4, 29, 2, 1, '2026-11-24T13:01:14', '同意', NULL, '2026-11-24T13:01:14', NULL, '2026-11-24T13:01:14', 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 部门编码兜底修正（幂等）
-- 说明：v1.4 起 org_id / dept_id 统一为 CHAR(6)。若源数据或旧脚本写入了数字
--       形态（如 2），这里统一补零到 6 位，保证能关联 mat_dept.org_id
-- ============================================================
UPDATE `mat_dept`            SET `org_id`   = LPAD(`org_id`, 6, '0')  WHERE CHAR_LENGTH(`org_id`) <> 6;
UPDATE `mat_dept`            SET `parent_id`= LPAD(`parent_id`, 6, '0') WHERE CHAR_LENGTH(`parent_id`) <> 6;
UPDATE `mat_sys_user`        SET `org_id`   = LPAD(`org_id`, 6, '0')  WHERE CHAR_LENGTH(`org_id`) <> 6;
UPDATE `mat_purchase_plan`   SET `org_id`   = LPAD(`org_id`, 6, '0')  WHERE CHAR_LENGTH(`org_id`) <> 6;
UPDATE `mat_req_order`       SET `org_id`   = LPAD(`org_id`, 6, '0')  WHERE CHAR_LENGTH(`org_id`) <> 6;
UPDATE `material_dept_quota` SET `dept_id`  = LPAD(`dept_id`, 6, '0') WHERE CHAR_LENGTH(`dept_id`) <> 6;
UPDATE `budget`              SET `dept_id`  = LPAD(`dept_id`, 6, '0') WHERE `dept_id` IS NOT NULL AND CHAR_LENGTH(`dept_id`) <> 6;

-- 结果自检：以下两项都应为 0
SELECT 'ORG编码非6位行数' AS 项目,
       (SELECT COUNT(*) FROM `mat_dept` WHERE CHAR_LENGTH(`org_id`) <> 6)
     + (SELECT COUNT(*) FROM `mat_sys_user` WHERE CHAR_LENGTH(`org_id`) <> 6)
     + (SELECT COUNT(*) FROM `mat_purchase_plan` WHERE CHAR_LENGTH(`org_id`) <> 6)
     + (SELECT COUNT(*) FROM `mat_req_order` WHERE CHAR_LENGTH(`org_id`) <> 6)
     + (SELECT COUNT(*) FROM `material_dept_quota` WHERE CHAR_LENGTH(`dept_id`) <> 6) AS 值
UNION ALL
SELECT '领用单关联不到部门的行数', COUNT(*)
  FROM `mat_req_order` r LEFT JOIN `mat_dept` d ON d.org_id = r.org_id
 WHERE d.org_id IS NULL;
