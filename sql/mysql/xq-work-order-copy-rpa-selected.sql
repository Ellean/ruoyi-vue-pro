-- 文案 RPA 勾选：选中的变体独立跑 RPA；未选中沿用主体文案。若列已存在会报 Duplicate column，可忽略。

ALTER TABLE `xq_work_order`
  ADD COLUMN `copy_rpa_selected` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否勾选独立跑文案RPA：1是 0否（未选沿用主体）' AFTER `rpa_copy_error`;
