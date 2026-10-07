-- 在已执行 ai-api-key-relay-config.sql 的基础上补「接入类型 / 能力 / 扩展 JSON」
-- 若列已存在会报错，跳过即可

ALTER TABLE `ai_api_key`
  ADD COLUMN `gateway_type` varchar(32) DEFAULT 'openai_compatible' COMMENT '接入类型' AFTER `url`,
  ADD COLUMN `capabilities` varchar(128) DEFAULT 'chat,vision' COMMENT '能力列表' AFTER `gateway_type`,
  ADD COLUMN `extra_config` text DEFAULT NULL COMMENT '扩展 JSON' AFTER `vision_image_detail`;
