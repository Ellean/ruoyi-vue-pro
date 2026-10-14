-- 工单维度 AI 消耗流水（文案 / 生图 / 改图）
-- 在 ruoyi-vue-pro 主库执行

CREATE TABLE IF NOT EXISTS `xq_ai_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `work_order_id` bigint NOT NULL COMMENT '作业单 ID',
  `category` varchar(32) NOT NULL COMMENT 'copy / image_gen / image_revise',
  `model` varchar(128) DEFAULT NULL COMMENT '模型名',
  `prompt_tokens` int NOT NULL DEFAULT 0,
  `completion_tokens` int NOT NULL DEFAULT 0,
  `total_tokens` int NOT NULL DEFAULT 0,
  `call_count` int NOT NULL DEFAULT 1 COMMENT '调用次数（无 token 时按次计）',
  `cost_usd` decimal(16, 8) DEFAULT NULL COMMENT '费用 USD（有则填）',
  `remark` varchar(255) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_xq_ai_usage_wo` (`work_order_id`, `category`),
  KEY `idx_xq_ai_usage_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 工单 AI 消耗流水';
