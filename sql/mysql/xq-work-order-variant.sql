ALTER TABLE xq_work_order
    ADD COLUMN parent_work_order_id BIGINT NULL COMMENT '主体任务ID，变体指向主体' AFTER giga_product_id,
    ADD COLUMN parent_sku VARCHAR(64) NULL COMMENT '主体 SKU' AFTER parent_work_order_id,
    ADD COLUMN variant_label VARCHAR(128) NULL COMMENT '变体名称/颜色' AFTER parent_sku;

ALTER TABLE xq_work_order
    ADD INDEX idx_xq_wo_parent (parent_work_order_id);
