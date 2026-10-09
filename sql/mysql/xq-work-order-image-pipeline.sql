-- 工作台：生图 RPA 流水线字段
-- 依赖：xq_work_order 已存在
-- 在 ruoyi-vue-pro 主库执行

ALTER TABLE `xq_work_order`
  ADD COLUMN `rpa_image_work_uuid` varchar(64) DEFAULT NULL COMMENT '生图 RPA workUuid' AFTER `rpa_copy_error`,
  ADD COLUMN `rpa_image_status` varchar(16) DEFAULT NULL COMMENT 'idle/queued/running/success/fail' AFTER `rpa_image_work_uuid`,
  ADD COLUMN `rpa_image_error` varchar(512) DEFAULT NULL COMMENT '生图 RPA 失败原因' AFTER `rpa_image_status`;

CREATE INDEX `idx_xq_wo_rpa_image` ON `xq_work_order` (`rpa_image_status`, `image_user_id`);
