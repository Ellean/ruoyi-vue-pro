-- 大健选品列表定时同步：每天 08:35，近 3 天，只增不覆盖，同 SKU 不录入；凭证分开跑
-- 在 ruoyi-vue-pro 主库执行

-- 1) 三套（及所有启用）凭证：打开定时 + 只增不覆盖
UPDATE `xq_giga_api_credential`
SET `enable_scheduled_sync` = b'1',
    `sync_dedupe_mode` = 'skip_if_exists',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `enabled` = b'1'
  AND (
    `name` LIKE '%自提%'
    OR `name` LIKE '%一件代发%'
    OR `name` LIKE '%代发%'
    OR `name` LIKE '%高少山大健%'
    OR `name` LIKE '%高少山%'
    OR `client_id` LIKE '67490205%'
    OR `client_id` LIKE '46296010%'
    OR `client_id` LIKE '8e34cbbb%'
  );

-- 兜底：凡已启用且名称/角色齐全的，也可手动打开；上面已覆盖三套

-- 2) 注册 Quartz Job（状态 1=开启；可在「基础设施 → 定时任务」再调）
INSERT INTO `infra_job`
(`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
 `retry_count`, `retry_interval`, `monitor_timeout`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 13010, '大健选品列表同步', 1, 'xqGigaProductListSyncJob', '{"days":3}', '0 35 8 * * ?',
       1, 60000, 0,
       '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'xqGigaProductListSyncJob' AND `deleted` = b'0'
);

-- 若已存在则纠正参数与 cron
UPDATE `infra_job`
SET `name` = '大健选品列表同步',
    `status` = 1,
    `handler_param` = '{"days":3}',
    `cron_expression` = '0 35 8 * * ?',
    `updater` = '1',
    `update_time` = NOW()
WHERE `handler_name` = 'xqGigaProductListSyncJob'
  AND `deleted` = b'0';
