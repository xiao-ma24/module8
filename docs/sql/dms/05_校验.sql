-- ============================================================
-- ⑤ 导入自检：表数/行数/外键/关键字段类型
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 来源：Gitee main 分支 docs/sql/（提交 b9fabd1 及后续修复）
-- 口径：系统数据字典统一版 v1.4 / G08
-- ============================================================

-- 【安全护栏】强制切到本组库，防止在 mysql / information_schema 下误执行
USE `material_mgmt`;
SET NAMES utf8mb4;

SELECT '表数量（应为42 = 39业务 + 3模拟表）' AS 项目, COUNT(*) AS 实际,
       IF(COUNT(*) = 42, 'PASS', 'CHECK') AS 结论
FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
UNION ALL
SELECT '旧表名残留（应为0）', COUNT(*), IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM information_schema.tables WHERE table_schema = DATABASE()
  AND table_name IN ('asset_inbound','sys_operation_log')
UNION ALL
SELECT '非 utf8mb4_general_ci 表（应为0）', COUNT(*), IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM information_schema.tables WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE' AND table_collation <> 'utf8mb4_general_ci'
UNION ALL
SELECT '部门额度外键（应为2）', COUNT(*), IF(COUNT(*) = 2, 'PASS', 'CHECK')
FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND constraint_type = 'FOREIGN KEY'
UNION ALL
SELECT '角色数量（应为8）', COUNT(*), IF(COUNT(*) = 8, 'PASS', 'CHECK') FROM `mat_sys_role`
UNION ALL
SELECT '用户数量（应≥7）', COUNT(*), IF(COUNT(*) >= 7, 'PASS', 'CHECK') FROM `mat_sys_user`
UNION ALL
SELECT '部门数量（应为4）', COUNT(*), IF(COUNT(*) = 4, 'PASS', 'CHECK') FROM `mat_dept`
UNION ALL
SELECT '模拟表数量（应为3）', COUNT(*), IF(COUNT(*) = 3, 'PASS', 'FAIL')
FROM information_schema.tables WHERE table_schema = DATABASE()
  AND table_name IN ('sys_teacher','sys_student','sys_budget_subject')
UNION ALL
SELECT '物资数量（应≥12）', COUNT(*), IF(COUNT(*) >= 12, 'PASS', 'CHECK') FROM `mat_material`
UNION ALL
SELECT '库存记录（应≥20）', COUNT(*), IF(COUNT(*) >= 20, 'PASS', 'CHECK') FROM `mat_stock`
UNION ALL
SELECT '领用单部门编码异常（应为0）', COUNT(*), IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM `mat_req_order` r LEFT JOIN `mat_dept` d ON d.org_id = r.org_id WHERE d.org_id IS NULL
UNION ALL
SELECT '领用单部门可见（应>0）', COUNT(*), IF(COUNT(*) > 0, 'PASS', 'CHECK')
FROM `mat_req_order` r JOIN `mat_dept` d ON d.org_id = r.org_id;

-- 关键字段类型抽查
SELECT table_name AS 表, column_name AS 列, column_type AS 类型, IF(column_type = 'char(6)', 'PASS', 'CHECK') AS 结论
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND ((table_name = 'mat_dept' AND column_name IN ('org_id','parent_id'))
     OR (table_name = 'mat_sys_user' AND column_name = 'org_id')
     OR (table_name = 'material_dept_quota' AND column_name = 'dept_id')
     OR (table_name = 'mat_purchase_plan' AND column_name = 'org_id')
     OR (table_name = 'mat_req_order' AND column_name = 'org_id')
     OR (table_name = 'budget' AND column_name = 'dept_id'))
 ORDER BY table_name, column_name;
