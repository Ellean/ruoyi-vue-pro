-- AI API 密钥：多中转 / 官方 API 差异化配置
-- 一条密钥 = 一个接入源（官方或某一中转）；字段按 gateway_type 选用，不必全填

-- 若已执行过旧版脚本，下面 ADD 已存在列会报错，可忽略对应行后继续

ALTER TABLE `ai_api_key`
  ADD COLUMN `gateway_type` varchar(32) DEFAULT 'openai_compatible' COMMENT '接入类型：openai_official/aixoras/hao/toapis/cun/openai_compatible/custom' AFTER `url`,
  ADD COLUMN `capabilities` varchar(128) DEFAULT 'chat,vision' COMMENT '能力：chat,vision,image_gen,image_edit 逗号分隔' AFTER `gateway_type`,
  ADD COLUMN `chat_model` varchar(128) DEFAULT NULL COMMENT '对话模型（可选）' AFTER `capabilities`,
  ADD COLUMN `vision_model` varchar(128) DEFAULT NULL COMMENT '识图模型（可选）' AFTER `chat_model`,
  ADD COLUMN `image_model` varchar(128) DEFAULT NULL COMMENT '生图模型（可选）' AFTER `vision_model`,
  ADD COLUMN `image_edit_model` varchar(128) DEFAULT NULL COMMENT '改图模型（可选）' AFTER `image_model`,
  ADD COLUMN `image_body_style` varchar(32) DEFAULT NULL COMMENT '生图请求风格（中转用）' AFTER `image_edit_model`,
  ADD COLUMN `supports_async` tinyint DEFAULT NULL COMMENT '异步生图：1是0否；官方一般空' AFTER `image_body_style`,
  ADD COLUMN `prefer_responses_api` tinyint DEFAULT NULL COMMENT '优先 Responses：1是0否；官方/中转按需' AFTER `supports_async`,
  ADD COLUMN `vision_image_detail` varchar(16) DEFAULT NULL COMMENT '识图 detail；空则调用方默认' AFTER `prefer_responses_api`,
  ADD COLUMN `extra_config` text DEFAULT NULL COMMENT '扩展 JSON：各中转私有参数' AFTER `vision_image_detail`,
  ADD COLUMN `remark` varchar(512) DEFAULT NULL COMMENT '备注' AFTER `extra_config`;
