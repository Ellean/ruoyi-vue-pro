-- 套餐勾选全量菜单时 JSON 会超过 varchar(4096)，保存失败或截断成空，
-- 业务租户角色菜单树就会一直「暂无数据」。
ALTER TABLE `system_tenant_package`
    MODIFY COLUMN `menu_ids` LONGTEXT NOT NULL COMMENT '关联的菜单编号';
