-- ============================================================
-- ⑥ 师资名单对齐（sys_teacher 与 mat_sys_user 姓名/工号一致）
-- 目标库：material_mgmt（阿里云 RDS，DMS SQLConsole 执行）
-- 背景：
--   * 旧版种子把 T0001 记为「韦森宁」，实际用户表 T0001 是「王主任」
--   * 旧版最后一名教师用工号 T0009，与最新种子（T0010 周航）不一致
--   * 管理端「新增用户 → 师资工号候选」直接读 sys_teacher，名单不一致演示时会被问住
-- 说明：可重复执行；T0010 已存在时不会重复改名
-- ============================================================

-- 【安全护栏】强制切到本组库
USE `material_mgmt`;

SET NAMES utf8mb4;

-- 6.1 T0001 姓名/电话与用户表对齐
UPDATE `sys_teacher`
   SET `teacher_name` = '王主任',
       `phone`        = '13800002001'
 WHERE `teacher_no` = 'T0001';

-- 6.2 末位教师工号 T0009 → T0010（T0010 已存在则跳过，避免唯一键冲突）
SET @has_t0010 = (SELECT COUNT(*) FROM `sys_teacher` WHERE `teacher_no` = 'T0010');
UPDATE `sys_teacher`
   SET `teacher_no` = 'T0010',
       `phone`      = '13800000010'
 WHERE `teacher_no` = 'T0009' AND @has_t0010 = 0;

-- 6.3 对齐结果自检：名单应为 9 人，T0001=王主任，T0010=周航，T0009 不存在
SELECT '师资名单人数（应为9）' AS 项目, COUNT(*) AS 实际, IF(COUNT(*) = 9, 'PASS', 'CHECK') AS 结论
  FROM `sys_teacher`
UNION ALL
SELECT 'T0001 姓名（应为王主任）', IFNULL(MAX(`teacher_name`), '缺失'), IF(MAX(`teacher_name`) = '王主任', 'PASS', 'FAIL')
  FROM `sys_teacher` WHERE `teacher_no` = 'T0001'
UNION ALL
SELECT 'T0010 姓名（应为周航）', IFNULL(MAX(`teacher_name`), '缺失'), IF(MAX(`teacher_name`) = '周航', 'PASS', 'FAIL')
  FROM `sys_teacher` WHERE `teacher_no` = 'T0010'
UNION ALL
SELECT 'T0009 残留（应为0）', COUNT(*), IF(COUNT(*) = 0, 'PASS', 'FAIL')
  FROM `sys_teacher` WHERE `teacher_no` = 'T0009';

-- 6.4 与用户表交叉核对：用户工号必须都能在师资名单中找到且姓名一致
SELECT u.`person_no` AS 工号, u.`user_name` AS 用户表姓名, t.`teacher_name` AS 师资表姓名,
       IF(t.`teacher_no` IS NULL, 'MISSING', IF(u.`user_name` = t.`teacher_name`, 'PASS', 'DIFF')) AS 结论
  FROM `mat_sys_user` u
  LEFT JOIN `sys_teacher` t ON t.`teacher_no` = u.`person_no`
 WHERE u.`source_type` IN ('TEACHER','LOCAL')
 ORDER BY u.`person_no`;
