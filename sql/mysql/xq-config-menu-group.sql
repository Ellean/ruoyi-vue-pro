-- 工作台：将「文案管理 / 图片管理 / RPA配置」收入「配置」目录
-- 在 ruoyi-vue-pro 主库执行

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12828,'配置','',1,4,12810,'config','lucide:settings',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `type`=VALUES(`type`),
  `sort`=VALUES(`sort`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `icon`=VALUES(`icon`),
  `always_show`=VALUES(`always_show`);

-- 三个管理页挂到「配置」下
UPDATE `system_menu`
SET `parent_id` = 12828, `sort` = 1, `updater` = '1', `update_time` = NOW()
WHERE `id` = 12821;

UPDATE `system_menu`
SET `parent_id` = 12828, `sort` = 2, `updater` = '1', `update_time` = NOW()
WHERE `id` = 12823;

UPDATE `system_menu`
SET `parent_id` = 12828, `sort` = 3, `updater` = '1', `update_time` = NOW()
WHERE `id` = 12825;

-- 超管可见目录
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, 12828, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu` rm
  WHERE rm.role_id = 1 AND rm.menu_id = 12828 AND rm.deleted = b'0' AND rm.tenant_id = 1
);

-- 文案角色可进配置目录（看文案管理）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12801, 12828, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `system_role` r WHERE r.id = 12801 AND r.deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12801 AND rm.menu_id = 12828 AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

-- 图片角色可进配置目录（看图片管理）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12802, 12828, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `system_role` r WHERE r.id = 12802 AND r.deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12802 AND rm.menu_id = 12828 AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
