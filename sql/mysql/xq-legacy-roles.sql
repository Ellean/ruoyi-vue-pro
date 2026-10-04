-- 补全原库岗位角色：财务 / 采购（文案、图片、店铺管理员已有）
-- 租户 1 + 星企科技 162

INSERT INTO `system_role`
(`id`,`name`,`code`,`sort`,`data_scope`,`data_scope_dept_ids`,`status`,`type`,`remark`,
 `creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
(12807, '财务', 'xq_finance', 23, 1, '', 0, 2, '原库财务岗：FMS 财务管理',
 '1', NOW(), '1', NOW(), b'0', 1),
(12808, '采购', 'xq_purchase', 24, 1, '', 0, 2, '原库采购岗',
 '1', NOW(), '1', NOW(), b'0', 1),
(12809, '财务', 'xq_finance', 23, 1, '', 0, 2, '原库财务岗：FMS 财务管理',
 '1', NOW(), '1', NOW(), b'0', 162),
(12810, '采购', 'xq_purchase', 24, 1, '', 0, 2, '原库采购岗',
 '1', NOW(), '1', NOW(), b'0', 162)
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `code`=VALUES(`code`),
  `remark`=VALUES(`remark`),
  `status`=VALUES(`status`),
  `tenant_id`=VALUES(`tenant_id`);

-- 财务：挂 FMS 财务管理整棵菜单（1894 及子孙）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', r.tenant_id
FROM `system_role` r
JOIN `system_menu` m ON m.deleted = b'0'
WHERE r.code = 'xq_finance' AND r.deleted = b'0'
  AND (
    m.id = 1894
    OR m.parent_id = 1894
    OR m.parent_id IN (SELECT id FROM `system_menu` WHERE parent_id = 1894 AND deleted = b'0')
    OR m.parent_id IN (
      SELECT id FROM `system_menu` WHERE parent_id IN (
        SELECT id FROM `system_menu` WHERE parent_id = 1894 AND deleted = b'0'
      ) AND deleted = b'0'
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = r.tenant_id
  );

-- 采购：先给产品工作台只读入口（与运营重叠少，可再手工加菜单）
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', r.tenant_id
FROM `system_role` r
JOIN `system_menu` m ON m.id IN (12800, 12810, 12811) AND m.deleted = b'0'
WHERE r.code = 'xq_purchase' AND r.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = r.tenant_id
  );
