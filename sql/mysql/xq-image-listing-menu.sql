-- 工作台任务：图片四态 + 上架字段快照
ALTER TABLE xq_work_order
    ADD COLUMN image_status VARCHAR(16) NULL COMMENT 'todo未完成/rejected已驳回/revised已修改/done已完成' AFTER image_user_id,
    ADD COLUMN listing_values_json LONGTEXT NULL COMMENT '上架模板字段值 JSON' AFTER listing_category_name,
    ADD COLUMN listing_result_json VARCHAR(2000) NULL COMMENT '上架调用结果' AFTER listing_values_json;

ALTER TABLE xq_work_order
    ADD INDEX idx_xq_wo_image_user (image_user_id),
    ADD INDEX idx_xq_wo_image_status (image_status);
