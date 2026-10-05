-- 主库 ruoyi-vue-pro：配置 > 模板配置
-- 注意：12830 已占用为「集成密钥」，本页用 12829 / 12850

SET NAMES utf8mb4;

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12829,'模板配置','xq:template-config:query',2,4,12828,'template-config','lucide:layout-template','xq/product/template-config/index','XqTemplateConfig',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
(12850,'保存模板配置','xq:template-config:update',3,1,12829,'','','',NULL,
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

-- 若曾误把 12830 改成模板配置按钮，恢复为系统管理下的集成密钥目录
UPDATE `system_menu`
SET `name`='集成密钥',
    `permission`='',
    `type`=1,
    `sort`=99,
    `parent_id`=1,
    `path`='xq-integration',
    `icon`='lucide:key-round',
    `component`=NULL,
    `component_name`=NULL,
    `updater`='1',
    `update_time`=NOW()
WHERE `id`=12830;

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id IN (12829, 12850)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
