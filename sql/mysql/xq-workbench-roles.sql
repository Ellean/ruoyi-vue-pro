-- 工作台角色：文案人员 / 图片人员（先文案，后图片）
-- 在 xq-workbench-flow.sql 之后执行
-- 请用 mysql --default-character-set=utf8mb4 执行，避免中文乱码

INSERT INTO `system_role`
(`id`,`name`,`code`,`sort`,`data_scope`,`data_scope_dept_ids`,`status`,`type`,`remark`,
 `creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
(12801, CONVERT(UNHEX('E69687E6A188E4BABAE59198') USING utf8mb4), 'xq_copy', 20, 1, '', 0, 2,
 CONVERT(UNHEX('E5B7A5E4BD9CE58FB0E69687E6A188EFBC9AE7949FE68890E7BC96E8BE91E69687E6A188') USING utf8mb4),
 '1', NOW(), '1', NOW(), b'0', 1),
(12802, CONVERT(UNHEX('E59BBEE78987E4BABAE59198') USING utf8mb4), 'xq_image', 21, 1, '', 0, 2,
 CONVERT(UNHEX('E5B7A5E4BD9CE58FB0E59BBEE78987EFBC9AE69687E6A188E5AE8CE68890E5908EE7949FE68890E59BBEE78987') USING utf8mb4),
 '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `code`=VALUES(`code`),
  `remark`=VALUES(`remark`),
  `status`=VALUES(`status`);

DELETE FROM `system_role_menu` WHERE `role_id` IN (12801, 12802) AND `tenant_id` = 1;

-- 文案人员：工作台 + 编辑/生成文案
INSERT INTO `system_role_menu`
(`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12801, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12800, 12810, 12813, 12814, 12816);

-- 图片人员：工作台 + 生成图片（后端校验须先文案）
INSERT INTO `system_role_menu`
(`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12802, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12800, 12810, 12813, 12817);
