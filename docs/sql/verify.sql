-- ============================================================
-- 导入后自检脚本（可选）
-- 用法：mysql -u<user> -p material_mgmt < verify.sql
-- 说明：expect 列为期望值，actual 为实际值；两者一致即通过
-- ============================================================

USE `material_mgmt`;

SELECT '表数量（不含视图）' AS item, COUNT(*) AS actual, '>= 30' AS expect
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT '视图数量' AS item, COUNT(*) AS actual, '0（本交付未包含视图）' AS expect
FROM information_schema.views WHERE table_schema = DATABASE();

SELECT '旧表名残留' AS item, COUNT(*) AS actual, '0' AS expect
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('material','sys_user','sys_role','sys_user_role','dept','warehouse',
                     'supplier','stock','stock_flow','req_order','out_order','purchase_plan',
                     'approve_record','borrow_return','loss_order');

SELECT '核心表缺失' AS item, COUNT(*) AS actual, '0' AS expect
FROM (
  SELECT 'mat_material' AS t UNION ALL SELECT 'mat_stock' UNION ALL
  SELECT 'mat_sys_user' UNION ALL SELECT 'mat_sys_role' UNION ALL
  SELECT 'purchase_request' UNION ALL SELECT 'purchase_order' UNION ALL
  SELECT 'inbound_order' UNION ALL SELECT 'acceptance_order' UNION ALL
  SELECT 'stocktake_order' UNION ALL SELECT 'sys_dict_item'
) x
WHERE NOT EXISTS (
  SELECT 1 FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = x.t
);

SELECT '角色数量' AS item, COUNT(*) AS actual, '8' AS expect FROM `mat_sys_role`;
SELECT '用户数量' AS item, COUNT(*) AS actual, '>= 7' AS expect FROM `mat_sys_user`;
SELECT '菜单数量' AS item, COUNT(*) AS actual, '>= 11' AS expect FROM `sys_menu`;
SELECT '字典项数量' AS item, COUNT(*) AS actual, '>= 10' AS expect FROM `sys_dict_item`;
SELECT '物资数量' AS item, COUNT(*) AS actual, '0（仅基础数据）或 12（含 demo-data）' AS expect FROM `mat_material`;
SELECT '库存记录' AS item, COUNT(*) AS actual, '0（仅基础数据）或 23（含 demo-data）' AS expect FROM `mat_stock`;
SELECT '业务单据-采购申请' AS item, COUNT(*) AS actual, '0（仅基础数据）或 27（含 demo-data）' AS expect FROM `purchase_request`;
