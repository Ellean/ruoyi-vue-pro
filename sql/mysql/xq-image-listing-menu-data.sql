-- 工作台：我的图片、上架（12851+）。配置目录顺延为 sort 6
SET NAMES utf8mb4;

UPDATE `system_menu`
SET `sort` = 6, `updater` = '1', `update_time` = NOW()
WHERE `id` = 12828;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12851,'我的图片','xq:work-order:my-image',2,4,12810,'image-pool','lucide:images','xq/product/image-pool/index','XqImagePool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12853,'驳回/完成图片','xq:work-order:update',3,1,12851,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12852,'上架','xq:work-order:list',2,5,12810,'listing-pool','lucide:upload','xq/product/listing-pool/index','XqListingPool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12854,'提交上架','xq:work-order:complete',3,1,12852,'','','',NULL,
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
  `component_name`=VALUES(`component_name`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12851, 12852, 12853, 12854)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12802, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12851, 12853)
  AND EXISTS (SELECT 1 FROM `system_role` r WHERE r.id = 12802 AND r.deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12802 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
