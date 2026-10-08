-- 工单上架子状态：export_pending_confirm=导表成功待用户确认
SET NAMES utf8mb4;

-- 若列已存在会报错，可忽略后继续
ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_status` varchar(32) NULL
    COMMENT '上架子状态：export_pending_confirm / listed' AFTER `listing_result_json`;
