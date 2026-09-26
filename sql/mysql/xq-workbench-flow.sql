-- 选品库 → 工作台（文案 / 图片 / 上架）流程补丁
-- 在已执行 xq-product-demo.sql 的库上再跑本文件
-- 若列已存在会报 Duplicate column，可忽略后继续执行后续语句

ALTER TABLE `xq_work_order`
  MODIFY COLUMN `source_id` bigint NULL COMMENT '货源ID，选品库下发可为空';

ALTER TABLE `xq_work_order`
  ADD COLUMN `cover_url` varchar(512) DEFAULT NULL COMMENT '选品封面' AFTER `title`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `generated_image_url` varchar(512) DEFAULT NULL COMMENT '生成图URL' AFTER `content_selling_points`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `category_name` varchar(128) DEFAULT NULL COMMENT '分类名' AFTER `cover_url`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `giga_category_id` bigint DEFAULT NULL COMMENT 'Giga 类目 id' AFTER `category_name`;

UPDATE `system_menu` SET `name` = '选品库' WHERE `id` = 12801;
UPDATE `system_menu` SET `name` = '选品列表' WHERE `id` = 12802;
UPDATE `system_menu` SET `name` = '工作台', `icon` = 'lucide:layout-dashboard' WHERE `id` = 12810;
UPDATE `system_menu` SET `name` = '任务列表', `component_name` = 'XqWorkbench' WHERE `id` = 12813;
UPDATE `system_menu` SET `name` = '编辑文案' WHERE `id` = 12814;
UPDATE `system_menu` SET `name` = '上架完成' WHERE `id` = 12815;
UPDATE `system_menu` SET `visible` = b'0' WHERE `id` = 12811;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12806,'下发工作台','xq:product:dispatch',3,4,12802,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12816,'生成文案','xq:work-order:gen-copy',3,3,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12817,'生成图片','xq:work-order:gen-image',3,4,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `parent_id`=VALUES(`parent_id`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12806, 12816, 12817)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
