-- 店铺大类 → 店铺配置（店铺平台 / 店铺发货仓 从「集成密钥」挪出）

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12848,'店铺','',1,36,0,'/xq-store','lucide:store',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12849,'店铺配置','',1,1,12848,'config','lucide:settings',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `type`=VALUES(`type`),
  `sort`=VALUES(`sort`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `icon`=VALUES(`icon`);

UPDATE `system_menu`
SET `parent_id` = 12849,
    `sort` = 1,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 12840 AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 12849,
    `sort` = 2,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 12846 AND `deleted` = b'0';

-- 超管 / 店铺管理员挂上新目录
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', r.tenant_id
FROM `system_role` r
CROSS JOIN (SELECT 12848 AS id UNION ALL SELECT 12849) m
WHERE r.code IN ('super_admin', 'xq_store_admin') AND r.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = r.tenant_id
  );
