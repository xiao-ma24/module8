-- ============================================================
-- ④ 采购额度表外键（部门编码 CHAR(6) 口径）
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 生成自：Gitee main 分支 docs/sql/full-schema.sql（提交 b06a0d1，2026-11-26）
-- 口径：系统数据字典统一版 v1.4 / G08（39 张业务表）
-- ============================================================

-- 【安全护栏】强制切到本组库，防止在 mysql / information_schema 下误执行
USE `material_mgmt`;

-- ============================================================
-- ④ 外键（v1.4 口径：dept_id / org_id 均为 CHAR(6)，与 mat_dept.org_id 对齐）
--    策略：删除 RESTRICT / 更新 CASCADE（教程 p12 推荐）
--    说明：先清孤儿数据再建约束，可重复执行（已存在会报 1826，可忽略）
-- ============================================================

-- 4.1 清理孤儿额度行（部门已不存在的额度）
DELETE q FROM `material_dept_quota` q
  LEFT JOIN `mat_dept` d ON d.org_id = q.dept_id
 WHERE d.org_id IS NULL;

-- 4.2 核定人不存在时置空
UPDATE `material_dept_quota` q
  LEFT JOIN `mat_sys_user` u ON u.user_id = q.approve_by
   SET q.approve_by = NULL
 WHERE q.approve_by IS NOT NULL AND u.user_id IS NULL;

-- 4.3 部门额度表外键
ALTER TABLE `material_dept_quota`
  ADD CONSTRAINT `fk_mdq_dept`
      FOREIGN KEY (`dept_id`) REFERENCES `mat_dept` (`org_id`)
      ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_mdq_approve_by`
      FOREIGN KEY (`approve_by`) REFERENCES `mat_sys_user` (`user_id`)
      ON DELETE SET NULL ON UPDATE CASCADE;

-- 4.4 单据 → 物资 / 仓库 / 用户（可选，按需逐条放开）
-- ALTER TABLE `mat_stock`         ADD CONSTRAINT `fk_stock_material`  FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_stock_flow`    ADD CONSTRAINT `fk_flow_material`   FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_req_order`     ADD CONSTRAINT `fk_req_material`    FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_out_order`     ADD CONSTRAINT `fk_out_material`    FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_borrow_return` ADD CONSTRAINT `fk_borrow_material` FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_loss_order`    ADD CONSTRAINT `fk_loss_material`   FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_purchase_plan` ADD CONSTRAINT `fk_plan_material`   FOREIGN KEY (`material_id`) REFERENCES `mat_material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `mat_stock`         ADD CONSTRAINT `fk_stock_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `mat_warehouse` (`warehouse_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 4.5 校验
SELECT '部门额度行数' AS 项目, COUNT(*) AS 值 FROM `material_dept_quota`
UNION ALL SELECT '孤儿额度行（应为0）', COUNT(*) FROM `material_dept_quota` q LEFT JOIN `mat_dept` d ON d.org_id = q.dept_id WHERE d.org_id IS NULL;

SELECT q.quota_period AS 年月, d.org_id AS 部门编码, d.dept_name AS 部门, q.month_quota AS 月度额度,
       q.used_amount AS 已用, (q.month_quota - q.used_amount) AS 剩余, q.submit_status AS 申报状态
  FROM `material_dept_quota` q LEFT JOIN `mat_dept` d ON d.org_id = q.dept_id
 ORDER BY q.quota_period DESC, d.org_id;
