-- ============================================================
-- 2026-11-26 师资名单种子对齐（已有库升级用）
-- ------------------------------------------------------------
-- 背景：
--   * 旧版种子把 T0001 记为「韦森宁」，实际用户表 T0001 是「王主任」
--   * 旧版末位教师为 T0009 周航，最新种子改为 T0010 周航
--   * 管理端「新增用户 → 师资工号候选」直接读 sys_teacher，名单不一致会被追问
-- 说明：可重复执行；T0010 已存在时跳过改名，避免唯一键冲突
-- ============================================================

USE `material_mgmt`;
SET NAMES utf8mb4;

UPDATE `sys_teacher`
   SET `teacher_name` = '王主任',
       `phone`        = '13800002001'
 WHERE `teacher_no` = 'T0001';

SET @has_t0010 = (SELECT COUNT(*) FROM `sys_teacher` WHERE `teacher_no` = 'T0010');
UPDATE `sys_teacher`
   SET `teacher_no` = 'T0010',
       `phone`      = '13800000010'
 WHERE `teacher_no` = 'T0009' AND @has_t0010 = 0;

SELECT u.`person_no` AS 工号, u.`user_name` AS 用户表姓名, t.`teacher_name` AS 师资表姓名,
       IF(t.`teacher_no` IS NULL, 'MISSING', IF(u.`user_name` = t.`teacher_name`, 'PASS', 'DIFF')) AS 结论
  FROM `mat_sys_user` u
  LEFT JOIN `sys_teacher` t ON t.`teacher_no` = u.`person_no`
 WHERE u.`source_type` IN ('TEACHER','LOCAL')
 ORDER BY u.`person_no`;
