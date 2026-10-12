-- 线下工作台数据清理：删掉已下发/已跑文案的任务，从选品库重新下发再走一遍。
-- 在 ruoyi 主库执行（xq_work_order 所在库）。先跑预览，确认后再解开 UPDATE。
-- 同 SKU+同平台有进行中/已上架任务时无法再次下发，所以必须清掉这些行。

-- ========== 1) 预览 ==========
SELECT status, IFNULL(rpa_copy_status, '(null)') AS rpa_copy_status, COUNT(*) AS cnt
FROM xq_work_order
WHERE deleted = 0
GROUP BY status, rpa_copy_status
ORDER BY status, rpa_copy_status;

SELECT COUNT(*) AS doing_or_listed
FROM xq_work_order
WHERE deleted = 0
  AND status IN (10, 20);

-- ========== 2) 软删全部工作台任务（推荐） ==========
-- 执行后：工作台为空；选品库可重新下发；认领人按新逻辑写成当前用户。
UPDATE xq_work_order
SET deleted = 1,
    updater = 'reset-local',
    update_time = NOW()
WHERE deleted = 0;

-- ========== 3) 核对 ==========
SELECT COUNT(*) AS remain_alive
FROM xq_work_order
WHERE deleted = 0;

-- ========== 4) 选品库「已选取」角标（可选，在 Giga 快照库执行） ==========
-- 下发会在 t_gigab2b_product_snapshot.detail_json.xqPickedPlatforms 打标。
-- 若不清，列表仍可能显示已选，但不挡下发（挡下发的是上面的工作台进行中任务）。
--
-- UPDATE t_gigab2b_product_snapshot
-- SET detail_json = JSON_SET(IFNULL(detail_json, JSON_OBJECT()), '$.xqPickedPlatforms', JSON_OBJECT())
-- WHERE JSON_EXTRACT(detail_json, '$.xqPickedPlatforms') IS NOT NULL;
