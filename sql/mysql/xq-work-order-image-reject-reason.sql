-- 图片驳回原因（运营/文案侧与美工侧驳回均需填写）
-- 在主库执行

ALTER TABLE `xq_work_order`
  ADD COLUMN `image_reject_reason` varchar(512) DEFAULT NULL
    COMMENT '图片驳回原因' AFTER `image_status`;
