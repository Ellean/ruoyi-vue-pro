-- 补齐：我的文案 / 分配美工 / 批量生成文案（线上常缺 12818、12819）
-- 权限：xq:work-order:my-copy / xq:work-order:assign-image / xq:work-order:batch-copy
-- 我的图片 xq:work-order:my-image 已由 xq-image-listing-menu-data.sql 提供（12851）
SET NAMES utf8mb4;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12818,'我的文案','xq:work-order:my-copy',2,3,12810,'copy-pool','lucide:file-text','xq/product/copy-pool/index','XqCopyPool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12819,'分配美工','xq:work-order:assign-image',3,1,12818,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12820,'批量生成文案','xq:work-order:batch-copy',3,5,12813,'','','',NULL,
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

-- 确保「我的图片」存在且权限标识正确（可重复执行）
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12851,'我的图片','xq:work-order:my-image',2,4,12810,'image-pool','lucide:images','xq/product/image-pool/index','XqImagePool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `type`=VALUES(`type`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `component`=VALUES(`component`),
  `component_name`=VALUES(`component_name`),
  `status`=VALUES(`status`),
  `deleted`=b'0',
  `updater`='1',
  `update_time`=NOW();

-- 超管角色可见（租户 1）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12818, 12819, 12820, 12851)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

-- 星企科技：管理员 / 租户管理员 默认勾上「分配美工」；图片人员勾上「我的图片」
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', 162
FROM `system_role` r
JOIN `system_menu` m ON m.id IN (12818, 12819)
WHERE r.tenant_id = 162 AND r.deleted = b'0'
  AND r.code IN ('xq_GLY', 'tenant_admin', 'xq_copy')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 162
  );

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, 12851, '1', NOW(), '1', NOW(), b'0', 162
FROM `system_role` r
WHERE r.tenant_id = 162 AND r.deleted = b'0'
  AND r.code IN ('xq_image', 'xq_GLY', 'tenant_admin')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = 12851 AND rm.deleted = b'0' AND rm.tenant_id = 162
  );
