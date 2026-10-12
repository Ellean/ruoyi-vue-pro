-- 文案卖点：varchar(1024) 装不下 5～8 条英文卖点，RPA 回调整单回滚，界面一直「生成中」
-- 在 ruoyi-vue-pro 主库执行

ALTER TABLE `xq_work_order`
  MODIFY COLUMN `content_selling_points` mediumtext COMMENT '卖点（多行文本）',
  MODIFY COLUMN `content_title` varchar(1024) DEFAULT NULL COMMENT '文案标题';
