-- SKU / 价格增幅规则：支持「设为默认」（原库 xq 数据源）
-- 执行前确认连到 t_giga_platform_* 所在库

ALTER TABLE `t_giga_platform_sku_rule`
  ADD COLUMN `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '同店铺+运营下默认规则' AFTER `skip_sku`;

ALTER TABLE `t_giga_platform_price_markup_rule`
  ADD COLUMN `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '同店铺+运营下默认规则' AFTER `enabled`;
