-- 2026-11-25 部门额度授权改为引用财务系统预算表
-- 背景：平台管理端「部门月度额度」不再手工核定，额度统一取财务预算表中按部门核定的年度额度 ÷ 12。
-- 执行一次即可；已有库升级使用，全新库直接用 full-schema.sql + demo-data.sql。

ALTER TABLE `budget`
  ADD COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID：财务按部门核定的预算额度' AFTER `budget_no`,
  ADD KEY `idx_budget_dept` (`dept_id`);

-- 财务系统按部门核定的年度预算（示例数据，可按实际财务数据调整）
INSERT IGNORE INTO `budget`
  (`budget_no`, `dept_id`, `fund_card_no`, `subject_name`, `balance`, `frozen`, `del_flag`)
VALUES
  ('D01', 1, 'F2026-D01', '教务处部门年度预算', 20000.00, 0.00, 0),
  ('D02', 2, 'F2026-D02', '化学实验室部门年度预算', 80000.00, 0.00, 0),
  ('D03', 3, 'F2026-D03', '后勤处部门年度预算', 30000.00, 0.00, 0),
  ('D04', 4, 'F2026-D04', '物理实验室部门年度预算', 15000.00, 0.00, 0);

-- 按财务预算刷新月度额度（年度 ÷ 12），已用额度不动
INSERT INTO `material_dept_quota` (`dept_id`, `quota_period`, `month_quota`, `used_amount`, `update_time`)
SELECT b.`dept_id`, DATE_FORMAT(CURDATE(), '%Y%m'), ROUND(b.`balance` / 12, 2), 0, NOW()
FROM `budget` b
JOIN `mat_dept` d ON d.`org_id` = b.`dept_id` AND d.`del_flag` = 0
WHERE b.`dept_id` IS NOT NULL AND b.`del_flag` = 0
ON DUPLICATE KEY UPDATE `month_quota` = VALUES(`month_quota`), `update_time` = NOW();
