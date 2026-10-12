-- AI 图结果测试页（只读展示本地已同步 agent_results_json.images）
-- 在 ruoyi-vue-pro 主库执行

INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,
 `status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(12855,'AI图测试','xq:agent-ai-test:query',2,7,12810,'agent-ai-test','lucide:image',
 'xq/product/agent-ai-test/index','XqAgentAiTest',
 0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `permission`=VALUES(`permission`),
  `parent_id`=VALUES(`parent_id`),
  `path`=VALUES(`path`),
  `component`=VALUES(`component`),
  `component_name`=VALUES(`component_name`),
  `icon`=VALUES(`icon`),
  `sort`=VALUES(`sort`);

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.id = 12855
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0' AND rm.tenant_id = 1
  );
