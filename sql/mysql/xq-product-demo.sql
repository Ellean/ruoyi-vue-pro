-- xq product demo: library + workspace
-- db: ruoyi-vue-pro

CREATE TABLE IF NOT EXISTS `xq_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku` varchar(64) NOT NULL,
  `name` varchar(256) NOT NULL,
  `category_name` varchar(128) DEFAULT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xq_source_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_sku` varchar(64) NOT NULL,
  `title` varchar(512) NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  `stock` int DEFAULT NULL,
  `source_name` varchar(64) DEFAULT 'DEMO',
  `image_url` varchar(512) DEFAULT NULL,
  `claimed` bit(1) NOT NULL DEFAULT b'0',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_external_sku` (`external_sku`),
  KEY `idx_claimed` (`claimed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xq_work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(32) NOT NULL,
  `source_id` bigint DEFAULT NULL COMMENT '货源ID，选品库下发可为空',
  `external_sku` varchar(64) NOT NULL,
  `title` varchar(512) NOT NULL,
  `cover_url` varchar(512) DEFAULT NULL COMMENT '选品封面',
  `category_name` varchar(128) DEFAULT NULL COMMENT '分类名',
  `giga_category_id` bigint DEFAULT NULL COMMENT 'Giga 类目 id',
  `status` tinyint NOT NULL DEFAULT 10,
  `content_title` varchar(512) DEFAULT NULL,
  `content_selling_points` varchar(1024) DEFAULT NULL,
  `generated_image_url` varchar(512) DEFAULT NULL COMMENT '生成图URL',
  `product_id` bigint DEFAULT NULL,
  `product_sku` varchar(64) DEFAULT NULL,
  `assignee_user_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_status` (`status`),
  KEY `idx_source` (`source_id`),
  KEY `idx_external_sku` (`external_sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `xq_product` (`sku`,`name`,`category_name`,`status`,`remark`) VALUES
('SKU-DEMO-001','Demo Mug','Home',0,'demo'),
('SKU-DEMO-002','Demo Earphone','Digital',0,'demo')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

INSERT INTO `xq_source_item` (`external_sku`,`title`,`price`,`stock`,`source_name`,`claimed`) VALUES
('GIGA-1001','Outdoor Folding Chair',89.90,120,'GigaDemo',b'0'),
('GIGA-1002','Steel Thermos 500ml',45.00,300,'GigaDemo',b'0'),
('GIGA-1003','Desk Organizer Set',29.90,80,'GigaDemo',b'0'),
('GIGA-1004','Silent Wireless Mouse',59.00,200,'GigaDemo',b'0')
ON DUPLICATE KEY UPDATE `title`=VALUES(`title`);

DELETE FROM `system_role_menu` WHERE `menu_id` BETWEEN 12800 AND 12820;
DELETE FROM `system_menu` WHERE `id` BETWEEN 12800 AND 12820;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12800,'产品','',1,35,0,'/xq-product','lucide:package',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12801,'选品库','',1,1,12800,'library','lucide:warehouse',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12802,'选品列表','xq:product:query',2,1,12801,'product','lucide:boxes','xq/product/library/index','XqProductLibrary',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12803,'产品创建','xq:product:create',3,1,12802,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12804,'产品更新','xq:product:update',3,2,12802,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12805,'产品删除','xq:product:delete',3,3,12802,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12806,'下发工作台','xq:product:dispatch',3,4,12802,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12810,'工作台','',1,2,12800,'workspace','lucide:layout-dashboard',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12811,'货源池','xq:source:query',2,1,12810,'source','lucide:inbox','xq/product/source/index','XqSourcePool',
 0,b'0',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12812,'认领货源','xq:source:claim',3,1,12811,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12813,'任务列表','xq:work-order:query',2,2,12810,'work-order','lucide:clipboard-list','xq/product/work-order/index','XqWorkbench',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12814,'编辑文案','xq:work-order:update',3,1,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12815,'上架完成','xq:work-order:complete',3,2,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12816,'生成文案','xq:work-order:gen-copy',3,3,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12817,'生成图片','xq:work-order:gen-image',3,4,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0');

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m WHERE m.id BETWEEN 12800 AND 12817;
