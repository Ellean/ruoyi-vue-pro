-- 工作台：认领 / 文案 / 美工分配 + 平台店铺 + 菜单
-- 依赖：xq-workbench-flow.sql、xq-workbench-roles.sql
-- 平台店铺/文案规则数据在 xq_finance_test（原库），此处只扩展 ruoyi 作业单与菜单

ALTER TABLE `xq_work_order`
  ADD COLUMN `copy_user_id` bigint DEFAULT NULL COMMENT '文案领取人' AFTER `assignee_user_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `image_user_id` bigint DEFAULT NULL COMMENT '美工人员' AFTER `copy_user_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_platform_id` varchar(64) DEFAULT NULL COMMENT '上架平台ID(原库)' AFTER `image_user_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `listing_shop_id` varchar(64) DEFAULT NULL COMMENT '上架店铺ID(原库)' AFTER `listing_platform_id`;

ALTER TABLE `xq_work_order`
  ADD COLUMN `workflow_phase` varchar(16) NOT NULL DEFAULT 'copy' COMMENT 'copy/image/list/done' AFTER `listing_shop_id`;

-- 配置目录（文案/图片/RPA 管理页挂这里；完整补丁见 xq-config-menu-group.sql）
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12828,'配置','',1,4,12810,'config','lucide:settings',NULL,NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`);

-- 文案子页 + 文案规则管理
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12818,'我的文案','xq:work-order:my-copy',2,3,12810,'copy-pool','lucide:file-text','xq/product/copy-pool/index','XqCopyPool',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12819,'分配美工','xq:work-order:assign-image',3,1,12818,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12820,'批量生成文案','xq:work-order:batch-copy',3,5,12813,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12821,'文案管理','xq:copy-rule:query',2,1,12828,'copy-rule','lucide:settings-2','xq/product/copy-rule/index','XqCopyRule',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12822,'保存文案规则','xq:copy-rule:update',3,1,12821,'','','',NULL,
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `component`=VALUES(`component`),
  `parent_id`=VALUES(`parent_id`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id BETWEEN 12818 AND 12822
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

-- 文案人员：我的文案 + 分配美工 + 批量生成 + 文案管理查看
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12801, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12818, 12819, 12820, 12821)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12801 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );

-- 图片人员：可进工作台看被分配任务（任务列表已有）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12802, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12813, 12817)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12802 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
