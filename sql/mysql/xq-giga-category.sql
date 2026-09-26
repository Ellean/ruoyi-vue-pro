-- Giga site category (synced from xq-erp scrape)
CREATE TABLE IF NOT EXISTS `xq_giga_site_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `giga_id` bigint unsigned NOT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `level` tinyint unsigned NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 0,
  `image_path` varchar(1000) DEFAULT NULL,
  `path_ids` varchar(255) DEFAULT NULL,
  `path_names` varchar(1000) DEFAULT NULL,
  `href` varchar(500) DEFAULT NULL,
  `source` varchar(100) DEFAULT 'gigab2b_header',
  `fetched_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_giga_id` (`giga_id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product: link to giga category
ALTER TABLE `xq_product`
  ADD COLUMN IF NOT EXISTS `giga_category_id` bigint DEFAULT NULL COMMENT 'Giga category giga_id' AFTER `category_name`,
  ADD COLUMN IF NOT EXISTS `item_code` varchar(64) DEFAULT NULL COMMENT 'Item Code' AFTER `sku`,
  ADD COLUMN IF NOT EXISTS `qty_available` int DEFAULT NULL AFTER `image_url`,
  ADD COLUMN IF NOT EXISTS `supplier_code` varchar(64) DEFAULT NULL AFTER `qty_available`,
  ADD COLUMN IF NOT EXISTS `supplier_name` varchar(128) DEFAULT NULL AFTER `supplier_code`,
  ADD COLUMN IF NOT EXISTS `listed_tag` varchar(64) DEFAULT NULL AFTER `supplier_name`;
