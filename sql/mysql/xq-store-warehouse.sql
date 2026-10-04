-- 店铺发货仓：仓库主数据 + 店铺→发货仓绑定（独立菜单页，手工维护）

CREATE TABLE IF NOT EXISTS `xq_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(64) NOT NULL COMMENT '仓库/区域编码 USWE、USWE-8',
  `name` varchar(128) NOT NULL COMMENT '仓库名称',
  `country_code` varchar(16) DEFAULT NULL COMMENT '国家代码 US',
  `country_name` varchar(64) DEFAULT NULL COMMENT '国家/地区',
  `source_warehouse_id` int DEFAULT NULL COMMENT '原库 t_erp_warehouse.id（可选追溯）',
  `source_address_id` int DEFAULT NULL COMMENT '原库 t_erp_warehouse_address.id（可选追溯）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xq_warehouse_code` (`code`, `deleted`),
  KEY `idx_xq_warehouse_source` (`source_warehouse_id`),
  KEY `idx_xq_warehouse_addr` (`source_address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 发货仓库';

CREATE TABLE IF NOT EXISTS `xq_store_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `store_id` bigint NOT NULL COMMENT 'xq_store.id',
  `warehouse_id` bigint DEFAULT NULL COMMENT 'xq_warehouse.id（可选）',
  `physical_code` varchar(64) DEFAULT NULL COMMENT '物理仓码/店铺仓码',
  `physical_name` varchar(128) DEFAULT NULL COMMENT '物理仓名称',
  `country_code` varchar(16) DEFAULT 'US' COMMENT '国家/地区',
  `priority` int NOT NULL DEFAULT 1 COMMENT '优先级 1级最高',
  `shipping_method` varchar(255) DEFAULT NULL COMMENT '默认运输方式（多条用换行分隔）',
  `courier_account` varchar(128) DEFAULT NULL COMMENT '快递账号',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_xq_store_wh_store` (`store_id`, `enabled`),
  KEY `idx_xq_store_wh_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='XQ 店铺发货仓绑定';

-- 菜单：店铺 → 店铺配置 → 店铺发货仓（需先有 12848/12849，见 xq-store-platform.sql 或 xq-store-menu.sql）
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12846,'店铺发货仓','xq:store-warehouse:query',2,2,12849,'store-warehouse','lucide:warehouse','xq/system/store-warehouse/index','XqStoreWarehouse',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12847,'店铺发货仓/配置','xq:store-warehouse:update',3,1,12846,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `type`=VALUES(`type`),
  `sort`=VALUES(`sort`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `icon`=VALUES(`icon`),
  `component`=VALUES(`component`),
  `component_name`=VALUES(`component_name`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', r.tenant_id
FROM `system_role` r
CROSS JOIN (SELECT 12846 AS id UNION ALL SELECT 12847 UNION ALL SELECT 12848 UNION ALL SELECT 12849) m
WHERE r.code IN ('super_admin', 'xq_store_admin') AND r.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = r.tenant_id
  );
