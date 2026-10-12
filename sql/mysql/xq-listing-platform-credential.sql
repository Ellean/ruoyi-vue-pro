-- 上架平台 API 密钥（主库）+ 菜单；从原库 t_giga_listing_platform 同步 Mirakl/Wayfair 配置
-- 在 ruoyi-vue-pro 主库执行

CREATE TABLE IF NOT EXISTS `xq_listing_platform_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `platform_id` bigint DEFAULT NULL COMMENT '业务平台 xq_platform.id',
  `source_platform_id` varchar(64) DEFAULT NULL COMMENT '原库 t_giga_listing_platform.id',
  `platform_code` varchar(64) NOT NULL COMMENT '平台编码',
  `platform_name` varchar(128) DEFAULT NULL COMMENT '平台名称',
  `provider` varchar(32) NOT NULL COMMENT 'mirakl / wayfair',
  `base_url` varchar(255) DEFAULT NULL COMMENT 'API Base URL（Mirakl）',
  `shop_id` varchar(128) DEFAULT NULL COMMENT 'Mirakl shop_id',
  `api_key_enc` varchar(2048) DEFAULT NULL COMMENT 'Mirakl API Key（加密）',
  `api_key_mask` varchar(64) DEFAULT NULL COMMENT 'API Key 掩码',
  `client_id` varchar(128) DEFAULT NULL COMMENT 'Wayfair Client ID',
  `client_secret_enc` varchar(2048) DEFAULT NULL COMMENT 'Wayfair Client Secret（加密）',
  `client_secret_mask` varchar(64) DEFAULT NULL COMMENT 'Client Secret 掩码',
  `supplier_id` varchar(128) DEFAULT NULL COMMENT 'Wayfair supplierId',
  `env` varchar(32) DEFAULT NULL COMMENT 'Wayfair env prod/sandbox',
  `extra_json` text COMMENT '其它非敏感配置 JSON',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `synced_from_xq_at` datetime DEFAULT NULL COMMENT '最近从原库同步时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_listing_cred_provider_src` (`provider`, `source_platform_id`, `deleted`),
  KEY `idx_xq_listing_cred_platform` (`platform_id`, `provider`, `enabled`),
  KEY `idx_xq_listing_cred_code` (`platform_code`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架平台 API 密钥（Mirakl/Wayfair）';

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12840,'上架平台密钥','xq:listing-credential:query',2,3,12830,'listing-credential','lucide:store',
 'xq/system/listing-credential/index','XqListingCredential',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12841,'创建上架密钥','xq:listing-credential:create',3,1,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12842,'更新上架密钥','xq:listing-credential:update',3,2,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12843,'删除上架密钥','xq:listing-credential:delete',3,3,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12844,'同步原库上架密钥','xq:listing-credential:sync',3,4,12840,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `component`=VALUES(`component`),
  `component_name`=VALUES(`component_name`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id BETWEEN 12840 AND 12844
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
