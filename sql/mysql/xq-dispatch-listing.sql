-- 下发工作台：强制选择平台 / 店铺 / 上架分类，便于工作台筛选
-- 若列已存在会报 Duplicate column，可忽略

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_category_id` varchar(64) DEFAULT NULL COMMENT '上架分类ID(原库)' AFTER `listing_shop_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_platform_name` varchar(100) DEFAULT NULL COMMENT '上架平台名' AFTER `listing_category_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_shop_name` varchar(200) DEFAULT NULL COMMENT '上架店铺名' AFTER `listing_platform_name`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_category_name` varchar(200) DEFAULT NULL COMMENT '上架分类名' AFTER `listing_shop_name`;
