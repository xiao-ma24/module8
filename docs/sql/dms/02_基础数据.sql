-- ============================================================
-- ② 基础数据：部门/角色/用户/菜单/字典/参数/仓库/供应商/分类/预算
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 来源：Gitee main 分支 docs/sql/（提交 b9fabd1 及后续修复）
-- 口径：系统数据字典统一版 v1.4 / G08
-- 含：师资/学生/预算科目种子数据（模拟外部系统，供新增用户与自助注册使用）
-- ============================================================

-- 【安全护栏】强制切到本组库，防止在 mysql / information_schema 下误执行
USE `material_mgmt`;
SET NAMES utf8mb4;

-- mat_dept
INSERT IGNORE INTO `mat_dept` (`org_id`,`dept_code`,`dept_name`,`parent_id`,`leader_id`,`annual_quota`,`used_amount`,`sort_no`,`dept_status`,`remark`,`created_by`,`created_at`,`updated_by`,`updated_at`,`del_flag`) VALUES
  ('000001','JWC','教务处','000000',5,20000.00,3500.00,1,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),
  ('000002','SYS','实验室管理处','000000',1,80000.00,22000.00,2,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),
  ('000003','HQ','后勤保障处','000000',4,30000.00,8000.00,3,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),
  ('000004','TSG','图书馆','000000',6,15000.00,2000.00,4,1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0);

-- mat_sys_role
INSERT IGNORE INTO `mat_sys_role` VALUES (1,'ADMIN','系统管理员','全校',1,1,'平台全部权限，含用户、角色、参数、审计','2026-11-13 22:30:41',NULL),(2,'DEPT_HEAD','部门负责人','部门',1,2,'本部门数据，采购申请审批','2026-11-13 22:30:41',NULL),(3,'PURCHASER','采购员','全校',1,3,'采购全流程单据','2026-11-13 22:30:41',NULL),(4,'STORE_KEEPER','仓库管理员','全校',1,4,'验收、入库、出库、盘点','2026-11-13 22:30:41',NULL),(5,'APPLICANT','教职工/申请人','本人',1,5,'本人申请、领用、借用','2026-11-13 22:30:41',NULL),(6,'STUDENT','学生','本人',1,6,'本人领用与借用申请','2026-11-13 22:30:41',NULL),(7,'CHECKER','验收人','全校',1,7,'到货验收、质量数量核验（与库管员岗位分离）','2026-11-13 22:30:41',NULL),(9,'STOCKTAKER','盘点员','全校',1,8,'盘点任务、实盘录入、库存预警核实、补货建议、盘点差异登记（不含审批与直接调账）','2026-11-23 15:50:56',NULL);

-- mat_sys_user
INSERT IGNORE INTO `mat_sys_user` (`user_id`,`person_no`,`user_name`,`user_type`,`org_id`,`role_code`,`title`,`phone`,`email`,`hire_date`,`password`,`pwd_need_change`,`pwd_updated_at`,`source_type`,`source_sync_at`,`last_login_at`,`user_status`,`remark`,`created_by`,`created_at`,`updated_by`,`updated_at`,`del_flag`) VALUES
  (1,'T0001','王主任',1,'000002','DEPT_HEAD','教授','13800002001','t0001@school.edu.cn',NULL,'ccc40871f61b5672$xyMD2RE5obfm2ip4ViNmCdLeZ7KG9v1zzVYXl0baI3I=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-24 11:45:20',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 11:45:20',0),
  (2,'T0002','李思滢',1,'000002','PURCHASER','实验师','13800002002','t0002@school.edu.cn',NULL,'932eb26fe8757c59$Go/6UyetMk+iL2mEHExFnlFdw2f32j9ZXk3gtwuuXzQ=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-24 11:45:12',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 11:45:11',0),
  (3,'T0003','刘志豪',1,'000002','STORE_KEEPER','库管员','13800002003','t0003@school.edu.cn',NULL,'36dc7d8f343bfbf2$eO91rXAz4d9pyEvvngmJ6SAjc2ldLueti5xPmX0cpCg=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-24 10:27:25',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 10:27:24',0),
  (4,'T0004','金日勋',1,'000003','STOCKTAKER','教师','13800002004','t0004@school.edu.cn',NULL,'ecb749cb22c186c3$DDnCqQG/BcJgXOXhWbbg8yykq1Oa05fhlhTmJ6iRFrY=',0,NULL,'TEACHER','2026-11-23 11:23:47','2026-11-24 10:27:31',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 10:27:31',0),
  (5,'T0005','韦森宁',1,'000001','ADMIN','信息中心管理员','13800002005','t0005@school.edu.cn',NULL,'2c7c086333ed0a7d$Od0qayUvtygElfLiiYz3nSlNwVCF58oCjJFofP8L97w=',0,NULL,'LOCAL',NULL,'2026-11-24 11:07:45',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 11:07:44',0),
  (6,'T0006','张老师',1,'000002','APPLICANT','教师','13800002006','t0006@school.edu.cn',NULL,'aa3c8a68a7d372fe$uk3FVjm3wpyma8B5FRgYCGMThx5NcQB5Ao6JYi2WF90=',0,NULL,'LOCAL',NULL,'2026-11-24 10:33:24',1,NULL,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 10:33:24',0),
  (7,'T0007','韦森聍',1,'000002','CHECKER','验收工程师','13800000007','weisenning@school.edu.cn',NULL,'776b4c363f99b5e5$blSm7N6ILH8t27LI4Fyc8CPfzP7WIuHPNtT9sZuWYEw=',0,NULL,'LOCAL',NULL,'2026-11-24 10:12:50',1,'到货验收与异常登记演示账号',NULL,'2026-11-13 22:30:50',NULL,'2026-11-24 10:12:49',0);

-- mat_sys_user_role
INSERT IGNORE INTO `mat_sys_user_role` VALUES (1,2,1,'2026-11-13 22:30:41'),(2,3,2,'2026-11-13 22:30:41'),(3,4,3,'2026-11-13 22:30:41'),(4,5,4,'2026-11-13 22:30:41'),(4,9,5,'2026-11-23 15:50:57'),(5,1,5,'2026-11-13 22:30:41'),(6,5,6,'2026-11-13 22:30:41'),(7,7,5,'2026-11-13 22:30:50');

-- mat_warehouse
INSERT IGNORE INTO `mat_warehouse` VALUES (1,'耗材库',1,'行政楼B1',3,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),(2,'实验器材库',1,'实验楼101',2,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),(3,'后勤物料库',1,'后勤楼1层',4,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0);

-- mat_supplier
INSERT IGNORE INTO `mat_supplier` VALUES (1,'得力办公用品有限公司',1,'陈经理','13800001001','XX市科技园区得力大厦','91330100MA27XXXX1A',5,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),(2,'蜀牛玻璃仪器厂',1,'刘厂长','13800001002','XX市蜀牛工业区','91330100MA27XXXX2B',4,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),(3,'晨光文具股份有限公司',1,'周敏','13800001003','XX市晨光产业园','91330100MA27XXXX3C',5,NULL,'2026-11-13 22:30:40',NULL,'2026-11-23 19:39:24',0),(4,'华骏实验设备有限公司',1,'孙浩','13800001004','XX市华骏科技城','91330100MA27XXXX4D',4,NULL,'2026-11-13 22:30:40',NULL,'2026-11-23 20:25:05',0),(5,'五交化建材经营部',1,'赵老板','13800001005','XX市建材市场12号','91330100MA27XXXX5E',3,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),(6,'宏图数码科技',0,'钱经理','13800001006','XX市数码广场','91330100MA27XXXX6F',2,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0);

-- material_category
INSERT IGNORE INTO `material_category` VALUES (1,'01','办公用品',0,1,1,NULL,'2026-11-23 18:41:47','2026-11-23 18:41:47',0),(2,'02','实验耗材',0,2,1,NULL,'2026-11-23 18:41:47','2026-11-23 18:41:47',0),(3,'03','后勤物资',0,3,1,NULL,'2026-11-23 18:41:47','2026-11-23 18:41:47',0),(4,'04','通用物资',0,4,1,NULL,'2026-11-23 18:41:47','2026-11-23 18:41:47',0);

-- material_dept_quota
INSERT IGNORE INTO `material_dept_quota` (`dept_id`,`quota_period`,`month_quota`,`used_amount`,`approve_by`,`update_time`) VALUES
  ('000001','202611',1666.67,0.00,5,'2026-11-24 09:28:07'),
  ('000002','202611',6666.67,260.00,NULL,'2026-11-23 20:26:07'),
  ('000003','202611',2500.00,0.00,NULL,'2026-11-18 10:50:40'),
  ('000004','202611',1250.00,0.00,NULL,'2026-11-18 10:50:40');

-- budget
INSERT IGNORE INTO `budget` (`id`,`budget_no`,`dept_id`,`fund_card_no`,`subject_name`,`balance`,`frozen`,`created_by`,`created_at`,`updated_by`,`updated_at`,`del_flag`) VALUES
  (1,'B01',NULL,'F2024-01','教学耗材采购预算',47515.00,24554.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-24 09:26:39',0),
  (2,'B02',NULL,'F2024-02','实验器材采购预算',116000.00,0.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0),
  (3,'B03',NULL,'F2024-03','后勤维修物料预算',28000.00,400.00,NULL,'2026-11-13 22:30:40',NULL,'2026-11-13 22:30:40',0);

-- sys_menu
INSERT IGNORE INTO `sys_menu` VALUES (100,0,'数据总览','/overview','DataBoard','admin:overview',2,1,1),(200,0,'用户与组织','','OfficeBuilding','',1,2,1),(210,200,'用户管理','/users','User','admin:user:list',2,1,1),(220,200,'角色权限','/roles','Key','admin:role:list',2,2,1),(230,200,'部门管理','/depts','Grid','admin:dept:list',2,3,1),(300,0,'平台配置','','Setting','',1,3,1),(310,300,'数据字典','/dicts','Collection','admin:dict:list',2,1,1),(320,300,'系统参数','/configs','Tools','admin:config:list',2,2,1),(400,0,'安全审计','','Lock','',1,4,1),(410,400,'操作日志','/logs','Document','admin:log:list',2,1,1),(420,400,'登录日志','/login-logs','Monitor','admin:loginlog:list',2,2,1),(421,200,'部门月度额度','/dept-quotas','Money','admin:deptquota:list',2,4,1);

-- sys_role_menu
INSERT IGNORE INTO `sys_role_menu` VALUES (1,100),(1,200),(1,210),(1,220),(1,230),(1,300),(1,310),(1,320),(1,400),(1,410),(1,420),(1,421),(2,100),(2,230),(2,421),(3,100),(4,100),(5,100),(6,100),(7,100),(9,100);

-- sys_dict_type
INSERT IGNORE INTO `sys_dict_type` VALUES ('doc_status','单据状态','统一单据状态字典',1,'2026-11-13 22:30:41'),('material_category','物资分类','与 01 子系统 category_code 对应',1,'2026-11-13 22:30:41'),('pay_type','付款方式','采购订单付款方式',1,'2026-11-13 22:30:41'),('priority','紧急程度','申请优先级',1,'2026-11-13 22:30:41'),('source_type','采购需求来源','采购申请来源渠道',1,'2026-11-13 22:30:41'),('user_type','人员类型','教职工/学生/外部',1,'2026-11-13 22:30:41');

-- sys_dict_item
INSERT IGNORE INTO `sys_dict_item` VALUES (1,'material_category','01','办公耗材','primary',1,1,NULL),(2,'material_category','02','实验器材','warning',2,1,NULL),(3,'material_category','03','后勤物料','success',3,1,NULL),(4,'material_category','04','通用物资','info',4,1,NULL),(5,'material_category','CTRL','管控物资','danger',5,1,NULL),(6,'source_type','1','部门申购','primary',1,1,NULL),(7,'source_type','2','领用转申购','warning',2,1,NULL),(8,'source_type','3','安全库存预警','danger',3,1,NULL),(9,'source_type','4','实验室自备','info',4,1,NULL),(10,'pay_type','1','货到付款','primary',1,1,NULL),(11,'pay_type','2','预付','warning',2,1,NULL),(12,'pay_type','3','月度结算','success',3,1,NULL),(13,'doc_status','0','草稿/待处理','info',1,1,NULL),(14,'doc_status','1','处理中','warning',2,1,NULL),(15,'doc_status','2','已完成','success',3,1,NULL),(16,'doc_status','3','已驳回/异常','danger',4,1,NULL),(17,'user_type','1','教职工','primary',1,1,NULL),(18,'user_type','2','学生','success',2,1,NULL),(19,'user_type','3','外部人员','info',3,1,NULL),(20,'priority','1','普通','info',1,1,NULL),(21,'priority','2','加急','danger',2,1,NULL);

-- sys_config
INSERT IGNORE INTO `sys_config` VALUES (1,'sys.school.name','示范职业技术学院','学校名称','基础','string',1,'显示在登录页与顶栏','2026-11-13 22:30:41'),(2,'sys.system.name','学校物资管理大系统','系统名称','基础','string',1,'','2026-11-13 22:30:41'),(3,'sys.system.version','1.0.0','系统版本','基础','string',1,'','2026-11-13 22:30:41'),(4,'sys.user.initPassword','123456','新用户初始密码','安全','string',1,'新增用户默认密码','2026-11-13 22:30:41'),(5,'sys.login.maxRetry','5','登录失败锁定次数','安全','number',1,'连续失败达次数后锁定 10 分钟','2026-11-13 22:30:41'),(6,'sys.login.lockMinutes','10','锁定时长(分钟)','安全','number',1,'','2026-11-13 22:30:41'),(7,'sys.pwd.minLength','6','密码最小长度','安全','number',1,'修改密码时校验','2026-11-13 22:30:41'),(8,'sys.pwd.forceFirstChange','true','首次登录强制改密','安全','boolean',1,'新账号首次登录需修改初始密码','2026-11-13 22:30:41'),(9,'purchase.autoApproveAmount','500','小额自动审批上限(元)','业务','number',1,'低于该金额可免审批','2026-11-13 22:30:41'),(10,'stock.lowWarningRate','1.0','低库存预警系数','业务','number',1,'可用量低于安全库存×系数时预警','2026-11-13 22:30:41'),(11,'ai.enabled','true','AI 助手开关','智能','boolean',1,'关闭后隐藏各端 AI 入口','2026-11-13 22:30:41'),(12,'ai.model','deepseek-chat','AI 模型','智能','string',1,'OpenAI 兼容接口模型名','2026-11-13 22:30:41'),(13,'audit.retainDays','180','审计日志保留天数','审计','number',1,'超期归档','2026-11-13 22:30:41');

-- ============ 课程模拟外部系统基线数据（正式环境改走接口，不建这些表） ============
-- 师资系统：新增用户/自助注册的工号候选来自本表
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

-- 学生系统：学生自助注册
INSERT IGNORE INTO `sys_student` (`student_no`, `student_name`, `class_name`, `major`) VALUES
  ('S2024001', '张三', '计科2401', '计算机科学与技术'),
  ('S2024002', '李四', '软工2402', '软件工程');

-- 财务系统：预算科目资料
INSERT IGNORE INTO `sys_budget_subject` (`subject_no`, `subject_name`, `balance`) VALUES
  ('B01', '教学耗材采购预算', 50000.00),
  ('B02', '实验室设备采购预算', 120000.00),
  ('B03', '后勤维修物料预算', 28000.00);
