-- 店铺平台可维护：平台 / 店铺账号 / 别名映射 / 用户绑店
-- 在 ruoyi-vue-pro 主库执行（数据从原库 sys_store + t_giga_listing_platform 同步）
-- 依赖：集成密钥目录 12830（xq-integration-secrets.sql）

-- 1) 上架平台（可编辑）
CREATE TABLE IF NOT EXISTS `xq_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(64) NOT NULL COMMENT '平台编码 amz/wayfair…',
  `name` varchar(128) NOT NULL COMMENT '平台名称',
  `source_platform_id` varchar(64) DEFAULT NULL COMMENT '原库 t_giga_listing_platform.id',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_platform_code` (`code`, `deleted`),
  KEY `idx_xq_platform_source` (`source_platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 上架平台（可维护）';

-- 2) 店铺（含账号密码，属于平台）
CREATE TABLE IF NOT EXISTS `xq_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NOT NULL COMMENT '店铺名称',
  `platform_id` bigint NOT NULL COMMENT '所属平台 xq_platform.id',
  `source_store_id` int DEFAULT NULL COMMENT '原库 sys_store.id',
  `account` varchar(128) DEFAULT NULL COMMENT '店铺登录账号',
  `password_enc` varchar(1024) DEFAULT NULL COMMENT '店铺密码（加密）',
  `password_mask` varchar(64) DEFAULT NULL COMMENT '密码掩码',
  `image_url` varchar(512) DEFAULT NULL COMMENT '店铺图片',
  `status` int NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `dept_id` varchar(64) DEFAULT NULL COMMENT '原部门/公司',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_store_source` (`source_store_id`, `deleted`),
  KEY `idx_xq_store_platform` (`platform_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 店铺（可维护，含账号）';

-- 3) 平台别名映射（旧 sys_store.platform 文本 → 平台）
CREATE TABLE IF NOT EXISTS `xq_platform_alias` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `platform_id` bigint NOT NULL COMMENT 'xq_platform.id',
  `alias` varchar(64) NOT NULL COMMENT '别名 amazon/亚马逊…',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_platform_alias` (`alias`, `deleted`),
  KEY `idx_xq_platform_alias_pid` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 平台别名映射';

-- 4) 用户绑定店铺（空=不限店；有记录=白名单）
CREATE TABLE IF NOT EXISTS `xq_user_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '芋道用户 ID',
  `store_id` bigint NOT NULL COMMENT 'xq_store.id',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_user_store` (`user_id`, `store_id`, `deleted`),
  KEY `idx_xq_user_store_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 用户-店铺绑定';

-- 5) 预置常见别名（同步时按 code 挂到平台；平台尚未导入时先插入占位可再改）
INSERT INTO `xq_platform` (`code`,`name`,`sort`,`enabled`,`creator`,`updater`)
VALUES
('amz','Amazon',10,b'1','1','1'),
('wayfair','Wayfair',20,b'1','1','1'),
('walmart','Walmart',30,b'1','1','1'),
('home_depot','Home Depot',40,b'1','1','1'),
('best_buy','Best Buy',50,b'1','1','1'),
('kohl_s','Kohls',60,b'1','1','1'),
('lowes','Lowes',70,b'1','1','1'),
('overstock','Overstock',80,b'1','1','1')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `sort`=VALUES(`sort`);

INSERT INTO `xq_platform_alias` (`platform_id`,`alias`,`creator`,`updater`)
SELECT p.id, a.alias, '1', '1'
FROM (
  SELECT 'amz' c, 'amazon' alias UNION ALL SELECT 'amz','amz' UNION ALL SELECT 'amz','亚马逊'
  UNION ALL SELECT 'wayfair','wayfair' UNION ALL SELECT 'wayfair','沃宜坊'
  UNION ALL SELECT 'walmart','walmart' UNION ALL SELECT 'walmart','wm' UNION ALL SELECT 'walmart','沃尔玛'
  UNION ALL SELECT 'home_depot','homedepot' UNION ALL SELECT 'home_depot','home_depot' UNION ALL SELECT 'home_depot','hd' UNION ALL SELECT 'home_depot','家得宝'
  UNION ALL SELECT 'best_buy','bestbuy' UNION ALL SELECT 'best_buy','best_buy' UNION ALL SELECT 'best_buy','bb' UNION ALL SELECT 'best_buy','百思买'
  UNION ALL SELECT 'kohl_s','kohls' UNION ALL SELECT 'kohl_s','kohl_s' UNION ALL SELECT 'kohl_s','kohl''s'
  UNION ALL SELECT 'lowes','lowes' UNION ALL SELECT 'lowes','lowe''s' UNION ALL SELECT 'lowes','劳氏' UNION ALL SELECT 'lowes','劳斯'
  UNION ALL SELECT 'overstock','overstock' UNION ALL SELECT 'overstock','ost'
) a
JOIN `xq_platform` p ON p.code = a.c AND p.deleted = b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `xq_platform_alias` x WHERE x.alias = a.alias AND x.deleted = b'0'
);

-- 6) 菜单：店铺 → 店铺配置 → 店铺平台
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12848,'店铺','',1,36,0,'/xq-store','lucide:store',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12849,'店铺配置','',1,1,12848,'config','lucide:settings',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12840,'店铺平台','xq:store-platform:query',2,1,12849,'store-platform','lucide:store','xq/system/store-platform/index','XqStorePlatform',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12841,'创建平台/店铺','xq:store-platform:create',3,1,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12842,'更新平台/店铺','xq:store-platform:update',3,2,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12843,'删除平台/店铺','xq:store-platform:delete',3,3,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12844,'同步旧店铺','xq:store-platform:sync',3,4,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12845,'用户绑店','xq:store-platform:bind',3,5,12840,'','','',NULL,
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

-- 7) 角色：店铺管理员（维护平台/店铺/映射/绑店）
INSERT INTO `system_role`
(`id`,`name`,`code`,`sort`,`data_scope`,`data_scope_dept_ids`,`status`,`type`,`remark`,
 `creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
(12803, CONVERT(UNHEX('E5BA97E993BEE7AEA1E79086E59198') USING utf8mb4), 'xq_store_admin', 22, 1, '', 0, 2,
 CONVERT(UNHEX('E7BBB4E68AA4E4B88AE69EB6E5B9B3E58FB0E38081E5BA97E993BEE8B4A6E58FB7E38081E698A0E5B084E4B88EE794A8E688B7E7BB91E5BA97') USING utf8mb4),
 '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `code`=VALUES(`code`),
  `remark`=VALUES(`remark`),
  `status`=VALUES(`status`);

-- 超管 + 店铺管理员
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12840, 12841, 12842, 12843, 12844, 12845, 12848, 12849)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12803, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12840, 12841, 12842, 12843, 12844, 12845, 12848, 12849)
  AND EXISTS (SELECT 1 FROM `system_role` r WHERE r.id = 12803 AND r.deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12803 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
