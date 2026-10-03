-- 工作台：文案/图片提示词流水线字段
-- 依赖：xq_work_order 已存在
-- 在 ruoyi-vue-pro 主库执行

ALTER TABLE `xq_work_order`
  ADD COLUMN `source_description` mediumtext COMMENT 'Giga 原文案/描述' AFTER `cover_url`,
  ADD COLUMN `source_image_urls` mediumtext COMMENT '原图 URL JSON 数组' AFTER `source_description`,
  ADD COLUMN `giga_product_id` varchar(64) DEFAULT NULL COMMENT 'Giga list id' AFTER `source_id`,
  ADD COLUMN `copy_result_json` mediumtext COMMENT '生成文案结构 JSON' AFTER `content_selling_points`,
  ADD COLUMN `image_prompt_json` mediumtext COMMENT '图片提示词条目 JSON' AFTER `copy_result_json`,
  ADD COLUMN `rpa_copy_work_uuid` varchar(64) DEFAULT NULL COMMENT '文案 RPA workUuid' AFTER `workflow_phase`,
  ADD COLUMN `rpa_copy_status` varchar(16) DEFAULT NULL COMMENT 'idle/queued/running/success/fail' AFTER `rpa_copy_work_uuid`,
  ADD COLUMN `rpa_copy_error` varchar(512) DEFAULT NULL COMMENT '文案 RPA 失败原因' AFTER `rpa_copy_status`;

CREATE INDEX `idx_xq_wo_rpa_copy` ON `xq_work_order` (`rpa_copy_status`, `rpa_copy_work_uuid`);
