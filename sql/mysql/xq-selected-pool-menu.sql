-- 工作台：已选产品池（与「编辑图片」同级）
-- 旧数据：已领取、有文案、已跑图，但未选平台/分类 → 运营补绑
-- 权限：xq:work-order:selected-pool
SET NAMES utf8mb4;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12856,'已选产品池','xq:work-order:selected-pool',2,4,12810,'selected-pool','lucide:package-open',
 'xq/product/selected-pool/index','XqSelectedPool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12857,'补绑平台分类','xq:work-order:selected-pool',3,1,12856,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `type`=VALUES(`type`),
  `sort`=VALUES(`sort`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `icon`=VALUES(`icon`),
  `component`=VALUES(`component`),
  `component_name`=VALUES(`component_name`),
  `status`=VALUES(`status`),
  `deleted`=b'0',
  `updater`='1',
  `update_time`=NOW();

-- 超管（租户 1）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12856, 12857)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

-- 星企科技：管理员 / 租户管理员 / 运营（文案岗也可看池子补绑）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', 162
FROM `system_role` r
JOIN `system_menu` m ON m.id IN (12856, 12857)
WHERE r.tenant_id = 162 AND r.deleted = b'0'
  AND r.code IN ('xq_GLY', 'tenant_admin', 'xq_copy')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 162
  );
