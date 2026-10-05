-- 原库 xq（数据源 xq）执行：类目字段默认值 + AI 映射
CREATE TABLE IF NOT EXISTS t_giga_listing_category_field_config (
  id            VARCHAR(64)  NOT NULL PRIMARY KEY,
  platform_id   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '原库平台ID',
  category_id   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '上架分类ID',
  fields_json   LONGTEXT     NOT NULL COMMENT '[{code,defaultValue,valueSource}]',
  create_date   DATETIME     NULL,
  update_date   DATETIME     NULL,
  UNIQUE KEY uk_giga_cat_field_cfg (platform_id, category_id),
  KEY idx_giga_cat_field_cfg_cat (category_id)
) COMMENT='上架类目字段默认值与AI映射';
