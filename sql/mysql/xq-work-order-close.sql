-- 工作台任务：关闭/撤回权限（拉错可去掉）
-- 在 ruoyi-vue-pro 主库执行

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12837,'关闭任务','xq:work-order:close',3,6,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `parent_id`=VALUES(`parent_id`),
  `sort`=VALUES(`sort`);

-- 文案人员也可关闭拉错任务
INSERT INTO `system_role_menu`
(`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12801, 12837, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu`
  WHERE `role_id` = 12801 AND `menu_id` = 12837 AND `tenant_id` = 1 AND `deleted` = b'0'
);

-- 超管角色（通常 id=1）补权限
INSERT INTO `system_role_menu`
(`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, 12837, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu`
  WHERE `role_id` = 1 AND `menu_id` = 12837 AND `tenant_id` = 1 AND `deleted` = b'0'
);
