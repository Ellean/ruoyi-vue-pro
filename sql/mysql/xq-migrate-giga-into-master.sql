-- Auto-generated: Giga/listing tables into ruoyi-vue-pro master
-- Source: 127.0.0.1/xq_finance_test
-- Generated: 2026-10-12T09:35:29.125Z
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `t_gigab2b_product_list`;
CREATE TABLE `t_gigab2b_product_list` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL COMMENT '同步操作人/绑定账号用户ID',
  `giga_config_id` varchar(64) DEFAULT NULL COMMENT 'Giga 凭证 ID',
  `source_platform` varchar(32) DEFAULT 'giga' COMMENT '货源: giga/alllumex',
  `external_product_id` varchar(128) DEFAULT NULL COMMENT '外部产品ID',
  `external_variant_id` varchar(128) DEFAULT NULL COMMENT '外部变体ID',
  `sku` varchar(128) NOT NULL,
  `product_name` varchar(500) DEFAULT NULL,
  `update_time` varchar(32) DEFAULT NULL COMMENT 'API 最后更新时间',
  `first_arrival_date` varchar(20) DEFAULT NULL,
  `added_time` varchar(32) DEFAULT NULL COMMENT '收藏时间',
  `sync_batch_no` varchar(64) DEFAULT NULL,
  `sync_time` datetime DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `agent_selected` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Agent勾选',
  `agent_selected_at` datetime DEFAULT NULL COMMENT 'Agent勾选时间',
  `agent_daily_selected` tinyint(1) NOT NULL DEFAULT '0' COMMENT '定时日清单：未跑完RPA',
  `agent_daily_selected_at` datetime DEFAULT NULL COMMENT '日清单写入时间',
  `image_prompts_json` longtext COMMENT 'Agent图片提示词JSON数组',
  `image_prompt_bindings_json` longtext COMMENT '图片提示词绑定原图 JSON',
  `copy_prompts_json` longtext COMMENT 'Agent文案提示词JSON',
  `agent_results_json` longtext COMMENT 'RPA Agent 生成结果 JSON（图片 OSS + 文案/关键词）',
  `workflow_pins_json` longtext COMMENT '工作流置顶(按用户隔离)',
  `erp_sku` varchar(64) DEFAULT NULL COMMENT '上架 SKU：姓名首字母+录入日期+序号',
  `listing_platform_id` varchar(64) DEFAULT NULL COMMENT '上架平台',
  `listing_category_id` varchar(64) DEFAULT NULL COMMENT '上架分类',
  `listing_detail_values_json` longtext COMMENT '上架细节字段值 JSON',
  `listing_bindings_json` longtext COMMENT '上架多平台绑定 JSON 数组',
  `shop_id` varchar(64) DEFAULT NULL COMMENT '所属店铺',
  `patent_risk_status` varchar(16) DEFAULT 'none' COMMENT 'none/clean/below/risk',
  `patent_hit_count` int(11) DEFAULT '0' COMMENT '最近一次命中数',
  `patent_max_score` decimal(8,2) DEFAULT NULL COMMENT '最近一次最高相似度百分制',
  `patent_threshold` decimal(8,2) DEFAULT NULL COMMENT '最近一次阈值',
  `patent_searched_at` datetime DEFAULT NULL COMMENT '最近查询时间',
  `patent_searched_by` varchar(64) DEFAULT NULL COMMENT '最近查询人',
  `patent_searched_by_name` varchar(128) DEFAULT NULL COMMENT '最近查询人姓名',
  `patent_last_search_id` varchar(64) DEFAULT NULL COMMENT '最近一次查询记录ID',
  `assign_owner_user_id` varchar(64) DEFAULT NULL COMMENT '初始运营负责人',
  `assign_current_user_id` varchar(64) DEFAULT NULL COMMENT '当前处理人',
  `assign_phase` varchar(16) DEFAULT NULL COMMENT '分配阶段: ops/copy/image',
  `assign_copy_user_id` varchar(64) DEFAULT NULL COMMENT '文案处理人',
  `assign_image_user_id` varchar(64) DEFAULT NULL COMMENT '图片处理人',
  `assign_prev_user_id` varchar(64) DEFAULT NULL COMMENT '上一环节处理人',
  `assign_updated_at` datetime DEFAULT NULL COMMENT '分配更新时间',
  `data_marks_json` longtext,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_list_sku` (`sku`),
  UNIQUE KEY `uk_giga_product_list_user_config_sku` (`user_id`,`giga_config_id`,`sku`),
  KEY `idx_giga_product_list_sku` (`sku`),
  KEY `idx_giga_product_list_sync_time` (`sync_time`),
  KEY `idx_giga_product_list_agent_selected` (`user_id`,`agent_selected`),
  KEY `idx_giga_product_list_config` (`user_id`,`giga_config_id`),
  KEY `idx_giga_product_assign_current` (`assign_current_user_id`),
  KEY `idx_giga_list_erp_sku` (`erp_sku`),
  KEY `idx_giga_product_list_agent_daily` (`agent_daily_selected`,`agent_daily_selected_at`),
  KEY `idx_giga_list_shop` (`shop_id`),
  KEY `idx_giga_list_patent_risk` (`patent_risk_status`,`patent_searched_at`),
  KEY `idx_giga_list_listing_platform` (`listing_platform_id`),
  KEY `idx_giga_list_listing_category` (`listing_category_id`),
  KEY `idx_giga_list_listing_platform_category` (`listing_platform_id`,`listing_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GigaB2B 产品列表本地同步';

DROP TABLE IF EXISTS `t_gigab2b_product_snapshot`;
CREATE TABLE `t_gigab2b_product_snapshot` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `giga_config_id` varchar(64) DEFAULT NULL COMMENT 'Giga 凭证 ID',
  `source_platform` varchar(32) DEFAULT 'giga' COMMENT '货源: giga/alllumex',
  `external_product_id` varchar(128) DEFAULT NULL COMMENT '外部产品ID',
  `external_variant_id` varchar(128) DEFAULT NULL COMMENT '外部变体ID',
  `sku` varchar(128) NOT NULL,
  `product_name` varchar(500) DEFAULT NULL,
  `sku_available` tinyint(1) DEFAULT NULL,
  `currency` varchar(16) DEFAULT NULL,
  `price` decimal(14,4) DEFAULT NULL,
  `exclusive_price` decimal(14,4) DEFAULT NULL,
  `discounted_price` decimal(14,4) DEFAULT NULL,
  `shipping_fee` decimal(14,4) DEFAULT NULL,
  `seller_available_inventory` int(11) DEFAULT NULL,
  `buyer_available_inventory` int(11) DEFAULT NULL,
  `detail_json` longtext COMMENT '详情 API 原始 JSON',
  `price_json` longtext COMMENT '价格 API 原始 JSON',
  `inventory_json` longtext COMMENT '库存 API 原始 JSON',
  `sync_batch_no` varchar(64) DEFAULT NULL,
  `sync_time` datetime DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_snapshot_sku` (`sku`),
  UNIQUE KEY `uk_giga_product_snapshot_user_config_sku` (`user_id`,`giga_config_id`,`sku`),
  KEY `idx_giga_product_snapshot_sku` (`sku`),
  KEY `idx_giga_product_snapshot_sync_time` (`sync_time`),
  KEY `idx_giga_product_snapshot_config` (`user_id`,`giga_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GigaB2B 产品详情/价格/库存合并快照';

DROP TABLE IF EXISTS `t_gigab2b_product_family_index`;
CREATE TABLE `t_gigab2b_product_family_index` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `family_sku` varchar(128) NOT NULL COMMENT '家族稳定键=已收藏成员中字典序最小 SKU',
  `sku` varchar(128) NOT NULL COMMENT '本行 SKU',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1=列表代表（一族只出一张卡片）',
  `collected` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1=本地有列表+快照',
  `sort_no` int(11) NOT NULL DEFAULT '0',
  `member_count` int(11) NOT NULL DEFAULT '1' COMMENT '本族已收藏成员数（写在每行，便于列表）',
  `list_id` varchar(64) DEFAULT NULL COMMENT 't_gigab2b_product_list.id',
  `item_code` varchar(128) DEFAULT NULL,
  `product_name` varchar(500) DEFAULT NULL,
  `variant_label` varchar(500) DEFAULT NULL COMMENT 'associateProductInfo.name / mainColor',
  `image_url` varchar(1000) DEFAULT NULL,
  `image_count` int(11) DEFAULT '0',
  `main_color` varchar(128) DEFAULT NULL,
  `qty_available` int(11) DEFAULT NULL,
  `price` decimal(14,4) DEFAULT NULL,
  `exclusive_price` decimal(14,4) DEFAULT NULL,
  `discounted_price` decimal(14,4) DEFAULT NULL,
  `currency` varchar(16) DEFAULT NULL,
  `sku_available` tinyint(1) DEFAULT NULL,
  `category_name` varchar(255) DEFAULT NULL,
  `giga_category_id` bigint(20) DEFAULT NULL,
  `supplier_code` varchar(64) DEFAULT NULL,
  `supplier_name` varchar(255) DEFAULT NULL,
  `listed_tag` varchar(64) DEFAULT NULL,
  `source_platform` varchar(32) DEFAULT NULL,
  `sync_time` datetime DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_family_idx_sku` (`sku`),
  KEY `idx_giga_family_idx_primary` (`is_primary`,`collected`,`sync_time`),
  KEY `idx_giga_family_idx_family` (`family_sku`,`collected`,`sort_no`),
  KEY `idx_giga_family_idx_cat` (`giga_category_id`),
  KEY `idx_giga_family_idx_list` (`list_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 主变体家族索引（列表入口）';

DROP TABLE IF EXISTS `t_giga_listing_platform`;
CREATE TABLE `t_giga_listing_platform` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL COMMENT '所属用户',
  `code` varchar(32) NOT NULL COMMENT '平台编码，如 amazon',
  `name` varchar(100) NOT NULL COMMENT '平台名称',
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `split_platform_key` varchar(32) DEFAULT NULL COMMENT '上架分表平台键：bestbuy/walmart/laosi/custom',
  `catalog_source` varchar(32) NOT NULL DEFAULT 'excel_template' COMMENT '目录来源：excel_template | mirakl_api',
  `mirakl_config_json` longtext COMMENT 'Mirakl 接口配置 JSON（baseUrl/apiKey/shopId）',
  `mirakl_last_sync_at` datetime DEFAULT NULL COMMENT 'Mirakl 最近同步时间',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `wayfair_config_json` longtext COMMENT 'Wayfair API 配置 JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_listing_platform_user_code` (`user_id`,`code`),
  KEY `idx_giga_listing_platform_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 上架平台';

DROP TABLE IF EXISTS `t_giga_listing_shop`;
CREATE TABLE `t_giga_listing_shop` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL DEFAULT 'shared',
  `platform_id` varchar(64) NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(200) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL COMMENT '店铺Logo图片URL',
  `category_id` varchar(64) DEFAULT NULL,
  `detail_values_json` longtext,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_shop_platform_code` (`platform_id`,`code`),
  KEY `idx_giga_shop_user` (`user_id`),
  KEY `idx_giga_shop_platform` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 上架店铺（一平台多店铺）';

DROP TABLE IF EXISTS `t_giga_listing_category`;
CREATE TABLE `t_giga_listing_category` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `name` varchar(200) NOT NULL COMMENT '分类名称',
  `parent_id` varchar(64) DEFAULT NULL COMMENT '父分类',
  `platform_id` varchar(64) DEFAULT NULL COMMENT '默认平台',
  `template_id` varchar(64) DEFAULT NULL COMMENT '绑定的细节模板',
  `split_category_name` varchar(200) DEFAULT NULL COMMENT '上架分表分类前缀，如 Chandeliers',
  `walmart_spec_type` varchar(200) DEFAULT NULL COMMENT '沃尔玛 spec_product_type',
  `external_hierarchy_code` varchar(120) DEFAULT NULL COMMENT 'Mirakl 分类编码(H11)',
  `parent_external_code` varchar(120) DEFAULT NULL COMMENT 'Mirakl 父分类编码',
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_giga_listing_category_user` (`user_id`),
  KEY `idx_giga_listing_category_parent` (`parent_id`),
  KEY `idx_giga_listing_category_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 上架分类';

DROP TABLE IF EXISTS `t_giga_listing_category_field_template`;
CREATE TABLE `t_giga_listing_category_field_template` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL,
  `category_id` varchar(64) NOT NULL COMMENT '关联 t_giga_listing_category',
  `hierarchy_code` varchar(120) NOT NULL COMMENT 'Mirakl 分类编码',
  `template_json` longtext NOT NULL COMMENT '合并字段方案 JSON',
  `sync_version` varchar(64) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_listing_category_field_template` (`category_id`),
  KEY `idx_giga_listing_category_field_template_platform` (`platform_id`),
  KEY `idx_giga_listing_category_field_template_hierarchy` (`hierarchy_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架分类字段模板 JSON（固定+特殊合并）';

DROP TABLE IF EXISTS `t_giga_listing_category_field_config`;
CREATE TABLE `t_giga_listing_category_field_config` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `platform_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '原库平台ID',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '上架分类ID',
  `fields_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '[{code,defaultValue,valueSource}]',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_cat_field_cfg` (`platform_id`,`category_id`),
  KEY `idx_giga_cat_field_cfg_cat` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上架类目字段默认值与AI映射';

DROP TABLE IF EXISTS `t_giga_listing_shop_field_config`;
CREATE TABLE `t_giga_listing_shop_field_config` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL DEFAULT '' COMMENT '原库平台 ID',
  `shop_id` varchar(64) NOT NULL DEFAULT '' COMMENT '业务店铺 xq_store.id',
  `country` varchar(16) NOT NULL DEFAULT '' COMMENT '国家码 US/CA/UK...',
  `fields_json` longtext COMMENT '[{code,defaultValue,valueSource,zone}]',
  `partitions_json` longtext COMMENT '[{id,name,keywords,system,color}]',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_shop_field_scope` (`platform_id`,`shop_id`,`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架模板：店铺+国家字段池与分区';

DROP TABLE IF EXISTS `t_giga_listing_detail_template`;
CREATE TABLE `t_giga_listing_detail_template` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `source_file_name` varchar(255) DEFAULT NULL COMMENT '导入来源文件名',
  `source_file_path` varchar(500) DEFAULT NULL COMMENT '原始模板文件相对路径',
  `header_row` int(11) NOT NULL DEFAULT '1' COMMENT '表头行号(1-based)',
  `sheet_name` varchar(120) DEFAULT NULL COMMENT '工作表名',
  `key_row` int(11) DEFAULT NULL COMMENT '字段键行号(1-based)',
  `data_start_row` int(11) DEFAULT NULL COMMENT '数据起始行号(1-based)',
  `columns_json` longtext NOT NULL COMMENT '列定义 JSON',
  `workbook_snapshot_json` longtext COMMENT 'Excel 工作簿快照 JSON',
  `tables_json` longtext COMMENT '模板多表格配置 JSON',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_giga_listing_template_user` (`user_id`),
  KEY `idx_giga_listing_template_platform` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 上架细节模板（由平台表格表头导入）';

DROP TABLE IF EXISTS `t_giga_listing_field_pool`;
CREATE TABLE `t_giga_listing_field_pool` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL DEFAULT '',
  `field_code` varchar(120) NOT NULL DEFAULT '',
  `label` varchar(500) DEFAULT NULL,
  `field_type` varchar(64) DEFAULT NULL,
  `required` tinyint(1) NOT NULL DEFAULT '0',
  `requirement_level` varchar(32) DEFAULT NULL,
  `values_list_code` varchar(120) DEFAULT NULL,
  `source` varchar(32) DEFAULT NULL COMMENT 'common/special/template',
  `source_count` int(11) NOT NULL DEFAULT '1',
  `field_json` longtext,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_field_pool` (`platform_id`,`field_code`),
  KEY `idx_giga_field_pool_plat` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架模板：平台字段池（拆分后）';

DROP TABLE IF EXISTS `t_giga_listing_field_common`;
CREATE TABLE `t_giga_listing_field_common` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `field_code` varchar(120) NOT NULL COMMENT 'Mirakl 属性 code',
  `field_json` longtext NOT NULL COMMENT '归一化后的属性 JSON',
  `required` tinyint(1) NOT NULL DEFAULT '0',
  `requirement_level` varchar(32) DEFAULT NULL COMMENT 'REQUIRED/OPTIONAL/...',
  `field_type` varchar(64) DEFAULT NULL COMMENT 'TEXT/LIST/...',
  `label` varchar(500) DEFAULT NULL,
  `values_list_code` varchar(120) DEFAULT NULL COMMENT '枚举列表 code',
  `sync_version` varchar(64) DEFAULT NULL COMMENT '同步批次',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_listing_field_common` (`platform_id`,`field_code`),
  KEY `idx_giga_listing_field_common_platform` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架固定字段（Mirakl 全分类通用）';

DROP TABLE IF EXISTS `t_giga_listing_field_special`;
CREATE TABLE `t_giga_listing_field_special` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `field_code` varchar(120) NOT NULL COMMENT 'Mirakl 属性 code',
  `hierarchy_codes_json` longtext NOT NULL COMMENT '适用分类编码 JSON 数组',
  `field_json` longtext NOT NULL COMMENT '归一化后的属性 JSON',
  `required` tinyint(1) NOT NULL DEFAULT '0',
  `requirement_level` varchar(32) DEFAULT NULL,
  `field_type` varchar(64) DEFAULT NULL,
  `label` varchar(500) DEFAULT NULL,
  `values_list_code` varchar(120) DEFAULT NULL,
  `sync_version` varchar(64) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_listing_field_special` (`platform_id`,`field_code`),
  KEY `idx_giga_listing_field_special_platform` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架特殊字段（Mirakl 分类专属）';

DROP TABLE IF EXISTS `t_giga_listing_values_list`;
CREATE TABLE `t_giga_listing_values_list` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `list_code` varchar(255) NOT NULL COMMENT '枚举列表 code（VL11 / type_parameters）',
  `label` varchar(500) DEFAULT NULL COMMENT '列表名称',
  `values_json` longtext NOT NULL COMMENT '枚举项 JSON [{code,label}]',
  `sync_version` varchar(64) DEFAULT NULL COMMENT '同步批次',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_listing_values_list` (`platform_id`,`list_code`),
  KEY `idx_giga_listing_values_list_platform` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上架枚举下拉本地缓存（Mirakl VL11）';

DROP TABLE IF EXISTS `t_giga_copy_gen_rule`;
CREATE TABLE `t_giga_copy_gen_rule` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL DEFAULT '' COMMENT '上架平台ID，空=通用规则',
  `code` varchar(64) NOT NULL DEFAULT 'deep' COMMENT '规则编码，deep=外部Deep文案',
  `name` varchar(200) NOT NULL DEFAULT 'Deep 文案生成规则',
  `config_json` longtext NOT NULL COMMENT '规则 JSON：卖点数/字数限制/标题内容分割',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_copy_gen_rule_platform` (`platform_id`),
  KEY `idx_giga_copy_gen_rule_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga Deep AI 文案生成规则';

DROP TABLE IF EXISTS `t_giga_image_gen_rule`;
CREATE TABLE `t_giga_image_gen_rule` (
  `id` varchar(64) NOT NULL,
  `platform_id` varchar(64) NOT NULL DEFAULT '' COMMENT '上架平台ID，空=通用',
  `category_id` varchar(64) NOT NULL DEFAULT '' COMMENT '上架分类ID，空=平台默认',
  `category_name` varchar(200) DEFAULT NULL COMMENT '分类名冗余',
  `name` varchar(200) NOT NULL DEFAULT '图片提示词规则',
  `prompt_text` longtext NOT NULL COMMENT '主提示词',
  `negative_prompt` text COMMENT '反向提示词',
  `config_json` longtext COMMENT '扩展 JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_image_gen_rule_pc` (`platform_id`,`category_id`),
  KEY `idx_giga_image_gen_rule_platform` (`platform_id`),
  KEY `idx_giga_image_gen_rule_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 按分类图片提示词规则';

DROP TABLE IF EXISTS `t_giga_platform_price_markup_rule`;
CREATE TABLE `t_giga_platform_price_markup_rule` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL DEFAULT 'shared' COMMENT '所属用户（共享目录）',
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `shop_id` varchar(64) NOT NULL COMMENT '店铺ID（sys_store.id）',
  `owner_user_id` varchar(64) NOT NULL DEFAULT '' COMMENT '运营负责人用户ID（空=店铺通用）',
  `name` varchar(200) NOT NULL COMMENT '规则名称',
  `mode` varchar(32) NOT NULL DEFAULT 'percent' COMMENT 'fixed固定/percent百分比/ratio比例增幅',
  `value` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '增幅值：固定金额 / 百分比数值 / 比例倍数',
  `apply_to` varchar(32) NOT NULL DEFAULT 'both' COMMENT 'dropship/pickup/both',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '同店铺+运营下默认规则',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `freight_rate` decimal(18,6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_price_markup_platform_shop_owner` (`platform_id`,`shop_id`,`owner_user_id`),
  KEY `idx_giga_price_markup_user` (`user_id`),
  KEY `idx_giga_price_markup_platform` (`platform_id`),
  KEY `idx_giga_price_markup_shop` (`shop_id`),
  KEY `idx_giga_price_markup_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 平台价格增幅规则（平台+店铺+运营）';

DROP TABLE IF EXISTS `t_giga_platform_sku_rule`;
CREATE TABLE `t_giga_platform_sku_rule` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL DEFAULT 'shared' COMMENT '所属用户（共享目录）',
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `shop_id` varchar(64) DEFAULT NULL COMMENT '店铺ID（sys_store.id）',
  `owner_user_id` varchar(64) NOT NULL DEFAULT '' COMMENT '运营负责人用户ID（空=店铺通用）',
  `name` varchar(200) NOT NULL COMMENT '规则名称',
  `segments_json` longtext NOT NULL COMMENT 'SKU 多段规则 JSON: [{type,value,format,width,start}]',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `skip_sku` tinyint(1) NOT NULL DEFAULT '0' COMMENT '该平台不生成上架SKU，导出也不写入SKU',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '同店铺+运营下默认规则',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_sku_rule_platform_shop_owner` (`platform_id`,`shop_id`,`owner_user_id`),
  KEY `idx_giga_sku_rule_user` (`user_id`),
  KEY `idx_giga_sku_rule_platform` (`platform_id`),
  KEY `idx_giga_sku_rule_shop` (`shop_id`),
  KEY `idx_giga_sku_rule_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 平台 SKU 生成规则（每平台一条）';

DROP TABLE IF EXISTS `t_giga_platform_upc_rule`;
CREATE TABLE `t_giga_platform_upc_rule` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL DEFAULT 'shared' COMMENT '所属用户（共享目录）',
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `shop_id` varchar(64) DEFAULT NULL COMMENT '店铺ID（sys_store.id）',
  `name` varchar(200) NOT NULL COMMENT '规则名称',
  `mode` varchar(32) NOT NULL DEFAULT 'none' COMMENT 'none/auto/pool',
  `flag_digit` char(1) DEFAULT NULL COMMENT '旗码 0-9（auto）',
  `manufacturer_code` varchar(16) DEFAULT NULL COMMENT '厂商代码（auto）',
  `product_width` int(11) NOT NULL DEFAULT '5' COMMENT '商品码位宽（auto）',
  `product_start` int(11) NOT NULL DEFAULT '1' COMMENT '商品码起始（auto）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_upc_rule_platform_shop` (`platform_id`,`shop_id`),
  KEY `idx_giga_upc_rule_user` (`user_id`),
  KEY `idx_giga_upc_rule_platform` (`platform_id`),
  KEY `idx_giga_upc_rule_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 平台 UPC 生成规则（每平台一条）';

DROP TABLE IF EXISTS `t_giga_upc_pool`;
CREATE TABLE `t_giga_upc_pool` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL DEFAULT 'shared' COMMENT '所属用户（共享目录）',
  `platform_id` varchar(64) NOT NULL COMMENT '所属平台',
  `shop_id` varchar(64) DEFAULT NULL COMMENT '店铺ID（sys_store.id）',
  `upc` varchar(16) NOT NULL COMMENT '12位 UPC-A',
  `pool_type` varchar(16) NOT NULL DEFAULT 'paid' COMMENT 'free=免费池 / paid=付费池',
  `status` varchar(16) NOT NULL DEFAULT 'available' COMMENT 'available/used/void',
  `product_list_id` varchar(64) DEFAULT NULL COMMENT '占用产品 t_gigab2b_product_list.id',
  `claimed_by` varchar(64) DEFAULT NULL,
  `claimed_by_name` varchar(128) DEFAULT NULL,
  `claimed_at` datetime DEFAULT NULL,
  `source` varchar(32) NOT NULL DEFAULT 'import',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giga_upc_pool_platform_shop_upc` (`user_id`,`platform_id`,`shop_id`,`upc`),
  KEY `idx_giga_upc_pool_platform_status` (`platform_id`,`status`),
  KEY `idx_giga_upc_pool_product` (`product_list_id`),
  KEY `idx_giga_upc_pool_shop_status` (`platform_id`,`shop_id`,`status`),
  KEY `idx_giga_upc_pool_type_status` (`platform_id`,`pool_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giga 付费 UPC 库存池';

DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint(20) unsigned NOT NULL COMMENT '整型自增主键',
  `account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `password` varchar(60) COLLATE utf8mb4_bin DEFAULT NULL,
  `salt` char(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `leader_id` bigint(20) unsigned DEFAULT NULL COMMENT '上层',
  `createDate` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `losingEffect` date DEFAULT NULL COMMENT '失效时间',
  `logicDelete` bit(1) DEFAULT b'0' COMMENT '逻辑删除',
  `disable` bit(1) DEFAULT b'0' COMMENT '停用',
  `isActive` bit(1) DEFAULT b'1' COMMENT '账户是否激活',
  `hasEmail` bit(1) DEFAULT b'0' COMMENT '邮箱是否激活',
  `member` int(11) DEFAULT '5' COMMENT '拥有下属数量上限',
  `passwordkey` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `googleAuthenticatorSecret` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lastlogintime` datetime DEFAULT NULL,
  `lastloginip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lastloginsession` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `ftype` char(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `oldid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `deptid` bigint(20) unsigned DEFAULT NULL,
  `active_tenant_id` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '当前激活的租户ID',
  `opttime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `account` (`account`) USING BTREE,
  KEY `leader_id` (`leader_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='用户表';

DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `id` bigint(20) unsigned NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '角色名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '角色标识：admin/finance/purchase/leader/operator等',
  `issystem` bit(1) NOT NULL DEFAULT b'0',
  `shopid` bigint(20) unsigned DEFAULT NULL,
  `dept_id` bigint(20) unsigned DEFAULT NULL COMMENT '部门ID，NULL=系统角色',
  `oldid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `Index 2` (`shopid`,`name`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='角色';

DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role` (
  `id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned DEFAULT NULL COMMENT '用户ID',
  `role_id` bigint(20) unsigned DEFAULT NULL COMMENT '角色ID',
  `dept_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `Index 4` (`user_id`,`role_id`) USING BTREE,
  UNIQUE KEY `uk_user_role_dept` (`user_id`,`role_id`,`dept_id`) USING BTREE,
  KEY `FK_t_user_role_t_user` (`user_id`) USING BTREE,
  KEY `FK_t_user_role_t_role` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='用户角色分配表';

DROP TABLE IF EXISTS `sys_store`;
CREATE TABLE `sys_store` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `platform` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `image_url` varchar(500) DEFAULT NULL COMMENT '店铺Logo图片URL',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` int(11) DEFAULT '1',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `create_date` datetime DEFAULT NULL,
  `del_flag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0',
  `dept_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

DROP TABLE IF EXISTS `t_sys_user_group`;
CREATE TABLE `t_sys_user_group` (
  `userid` bigint(20) unsigned DEFAULT NULL COMMENT '用户ID',
  `groupid` bigint(20) unsigned DEFAULT NULL COMMENT '店铺ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='用户客户对店铺的权限绑定';

DROP TABLE IF EXISTS `t_erp_warehouse`;
CREATE TABLE `t_erp_warehouse` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '仓库 ID',
  `warehouse_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '仓库编码（如 CA, USEA）',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '仓库名称（如 多伦多区）',
  `warehouse_name_text` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库完整名称（如 CA[多伦多区]）',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家代码（如 CA, US）',
  `country_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家名称（中文）',
  `country_name_en` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家名称（英文）',
  `currency_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货币代码（如 CAD, USD）',
  `value_added_tax` decimal(10,2) DEFAULT '0.00' COMMENT '增值税率',
  `is_used` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0 正常 1 停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `dept_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门/公司ID',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`) USING BTREE,
  UNIQUE KEY `t_erp_warehouse_warehouse_code` (`warehouse_code`) USING BTREE,
  KEY `idx_country_code` (`country_code`) USING BTREE,
  KEY `t_erp_warehouse_country_code` (`country_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='仓库区域表';

DROP TABLE IF EXISTS `t_erp_warehouse_address`;
CREATE TABLE `t_erp_warehouse_address` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '仓库地址 ID',
  `dept_id` bigint(20) unsigned DEFAULT NULL COMMENT '所属公司ID（数据权限隔离）',
  `wp_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物理仓编码（如 CA-1, USEA-11）',
  `wp_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物理仓名称（如 多伦多1号仓）',
  `warehouse_id` bigint(20) unsigned NOT NULL COMMENT '所属仓库区域 ID',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家代码',
  `country_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家名称',
  `wp_province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份/州',
  `wp_city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
  `wp_postcode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮编',
  `wp_company` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公司名称',
  `wp_contacter` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `wp_phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `wp_street_address1` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址行1',
  `wp_street_address2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址行2',
  `wp_street_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门牌号',
  `is_main_wp` tinyint(1) DEFAULT '0' COMMENT '是否主仓库（1=是，0=否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0 正常 1 停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_wp_code` (`wp_code`) USING BTREE,
  UNIQUE KEY `t_erp_warehouse_address_wp_code` (`wp_code`) USING BTREE,
  KEY `idx_warehouse_id` (`warehouse_id`) USING BTREE,
  KEY `idx_is_main_wp` (`is_main_wp`) USING BTREE,
  KEY `t_erp_warehouse_address_warehouse_id` (`warehouse_id`) USING BTREE,
  KEY `t_erp_warehouse_address_is_main_wp` (`is_main_wp`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  CONSTRAINT `fk_warehouse_address_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `t_erp_warehouse` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='仓库地址表';
