-- 集成密钥拆分：控制中枢全局密钥 + 产品库凭证池（管理员配置）
-- 个人 RPA 页只保留任务 UUID / 入参
-- 在 ruoyi-vue-pro 主库执行

-- 1) 控制中枢：全局共用密钥（单行）
CREATE TABLE IF NOT EXISTS `xq_rpa_global_config` (
  `id` bigint NOT NULL COMMENT '固定 1',
  `base_url` varchar(255) DEFAULT NULL COMMENT '控制中枢 API 根地址',
  `app_key` varchar(512) DEFAULT NULL COMMENT '全局 appKey',
  `app_secret` varchar(512) DEFAULT NULL COMMENT '全局 appSecret',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='控制中枢全局密钥（管理员）';

INSERT INTO `xq_rpa_global_config` (`id`, `base_url`, `creator`, `updater`)
VALUES (1, 'https://z-commander-api.ai-indeed.com', '1', '1')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 2) 产品库凭证池（特殊：多凭证、加密 secret、默认/沙箱）
CREATE TABLE IF NOT EXISTS `xq_giga_api_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NOT NULL COMMENT '凭证名称',
  `client_id` varchar(128) NOT NULL COMMENT 'Giga Client ID',
  `client_secret_enc` varchar(1024) NOT NULL COMMENT 'Giga Client Secret（加密）',
  `client_secret_mask` varchar(64) DEFAULT NULL COMMENT 'Secret 掩码展示',
  `sandbox` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否沙箱',
  `base_url` varchar(255) DEFAULT NULL COMMENT '自定义 API，空用官方默认',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认凭证',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_xq_giga_cred_default` (`is_default`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品库 Giga OpenAPI 凭证池（管理员）';

-- 3) 系统管理 → 集成密钥
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12830,'集成密钥','',1,99,1,'xq-integration','lucide:key-round',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12831,'控制中枢密钥','xq:rpa-global:query',2,1,12830,'rpa-global','lucide:bot','xq/system/rpa-global/index','XqRpaGlobal',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12832,'保存控制中枢密钥','xq:rpa-global:update',3,1,12831,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12833,'产品库凭证','xq:giga-credential:query',2,2,12830,'giga-credential','lucide:package','xq/system/giga-credential/index','XqGigaCredential',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12834,'创建产品库凭证','xq:giga-credential:create',3,1,12833,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12835,'更新产品库凭证','xq:giga-credential:update',3,2,12833,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12836,'删除产品库凭证','xq:giga-credential:delete',3,3,12833,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `component`=VALUES(`component`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id BETWEEN 12830 AND 12836
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
