-- 工作台：将「我的图片」更名为「编辑图片」（美工看已分配待执行/已出图）
-- 前端也会动态注入同名菜单；执行本脚本可与库内菜单对齐
SET NAMES utf8mb4;

UPDATE `system_menu`
SET `name` = '编辑图片',
    `path` = 'edit-image',
    `icon` = 'lucide:images',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 12851;

-- 图片角色保留菜单权限
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 12802, 12851, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `system_role` r WHERE r.id = 12802 AND r.deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 12802 AND rm.menu_id = 12851 AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
