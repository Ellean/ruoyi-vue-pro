-- 产品库凭证：商家 / 价格角色 / 定时拉取用途 / 去重策略
-- 在 ruoyi-vue-pro 主库执行

ALTER TABLE `xq_giga_api_credential`
  ADD COLUMN `vendor_code` varchar(64) DEFAULT NULL COMMENT '商家/账号编码（多家分组）' AFTER `name`,
  ADD COLUMN `vendor_name` varchar(128) DEFAULT NULL COMMENT '商家展示名' AFTER `vendor_code`,
  ADD COLUMN `price_role` varchar(16) NOT NULL DEFAULT 'dropship' COMMENT '价格角色: pickup自提 / dropship一键代发' AFTER `sandbox`,
  ADD COLUMN `enable_scheduled_sync` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否用于定时拉取选品库' AFTER `price_role`,
  ADD COLUMN `sync_dedupe_mode` varchar(32) NOT NULL DEFAULT 'skip_if_exists' COMMENT '拉取去重: skip_if_exists存在则不扩 / always_refresh始终刷新' AFTER `enable_scheduled_sync`;

CREATE INDEX `idx_xq_giga_cred_vendor_role`
  ON `xq_giga_api_credential` (`vendor_code`, `price_role`, `enabled`);

CREATE INDEX `idx_xq_giga_cred_sync`
  ON `xq_giga_api_credential` (`enable_scheduled_sync`, `enabled`);
