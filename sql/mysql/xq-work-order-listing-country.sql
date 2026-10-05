-- 作业单上架国家。若列已存在会报 Duplicate column，可忽略。

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_country_code` varchar(16) DEFAULT NULL COMMENT '上架国家代码' AFTER `listing_category_name`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_country_name` varchar(64) DEFAULT NULL COMMENT '上架国家名' AFTER `listing_country_code`;
