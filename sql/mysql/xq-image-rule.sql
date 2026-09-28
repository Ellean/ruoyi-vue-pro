-- 图片管理：按平台分类配置生图提示词
-- 原库 xq_finance_test：t_giga_image_gen_rule
-- 芋道菜单：工作台 > 图片管理

CREATE TABLE IF NOT EXISTS `t_giga_image_gen_rule` (
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
