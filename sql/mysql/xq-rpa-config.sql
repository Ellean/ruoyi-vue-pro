-- 工作台 RPA 触发配置（迁移优化版，不照搬旧 t_commander_user_config）
-- 在 ruoyi-vue-pro 主库执行
-- 依赖：配置目录 12828（xq-config-menu-group.sql / xq-copy-workflow.sql）

CREATE TABLE IF NOT EXISTS `xq_rpa_user_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '芋道用户 ID',
  `base_url` varchar(255) DEFAULT NULL COMMENT '控制中枢 API 根地址',
  `app_key` varchar(512) DEFAULT NULL COMMENT '控制中枢 appKey',
  `app_secret` varchar(512) DEFAULT NULL COMMENT '控制中枢 appSecret',
  `image_job_uuid` varchar(128) DEFAULT NULL COMMENT '生图/拉取类任务 UUID（对应旧个人任务）',
  `copy_job_uuid` varchar(128) DEFAULT NULL COMMENT '文案图文生任务 UUID（新）',
  `erp_site_url` varchar(255) DEFAULT NULL COMMENT '写入 RPA 入参 http',
  `account` varchar(128) DEFAULT NULL COMMENT '写入 RPA 入参「账户」',
  `password` varchar(256) DEFAULT NULL COMMENT '写入 RPA 入参「密码」',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_rpa_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作台 RPA 个人配置';

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12825,'RPA配置','xq:rpa-config:query',2,3,12828,'rpa-config','lucide:bot','xq/product/rpa-config/index','XqRpaConfig',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12826,'保存RPA配置','xq:rpa-config:update',3,1,12825,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12827,'触发RPA','xq:rpa-config:trigger',3,2,12825,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `component`=VALUES(`component`),
  `parent_id`=VALUES(`parent_id`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12825, 12826, 12827)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
