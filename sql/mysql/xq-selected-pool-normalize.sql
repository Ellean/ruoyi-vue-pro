-- 已选产品池：仅「无平台 + 已领取 + 已写图片提示词」
-- 不做全量自动分平台；运营在「已选产品池」手工补绑平台/分类
-- 灌数脚本：xq-erp/backend/scripts/dispatch-claimed-prompts-to-selected-pool.js
SET NAMES utf8mb4;

-- ========== 1) 诊断：符合已选产品池条件（收紧后）==========
SELECT COUNT(*) AS selected_pool_no_platform_prompts
FROM xq_work_order
WHERE deleted = 0
  AND status = 10
  AND parent_work_order_id IS NULL
  AND (assignee_user_id IS NOT NULL OR copy_user_id IS NOT NULL OR image_user_id IS NOT NULL)
  AND image_prompt_json IS NOT NULL
  AND image_prompt_json <> ''
  AND image_prompt_json <> '[]'
  AND (listing_platform_id IS NULL OR listing_platform_id = '');

-- 旧脏数据：有平台但无分类（错误分发，不应进已选池）
SELECT COUNT(*) AS has_platform_no_category
FROM xq_work_order
WHERE deleted = 0
  AND status = 10
  AND parent_work_order_id IS NULL
  AND listing_platform_id IS NOT NULL AND listing_platform_id <> ''
  AND (listing_category_id IS NULL OR listing_category_id = '');

-- 同 SKU 重复（无平台进行中）
SELECT external_sku, COUNT(*) n
FROM xq_work_order
WHERE deleted = 0 AND status = 10
  AND (listing_platform_id IS NULL OR listing_platform_id = '')
GROUP BY external_sku
HAVING n > 1
ORDER BY n DESC
LIMIT 30;

-- ========== 2) 规范化：空串平台/店铺/分类 → NULL ==========
UPDATE xq_work_order
SET listing_platform_id = NULL,
    listing_platform_name = NULL,
    updater = 'selected-pool-normalize',
    update_time = NOW()
WHERE deleted = 0
  AND status = 10
  AND (listing_platform_id IS NOT NULL AND TRIM(listing_platform_id) = '');

UPDATE xq_work_order
SET listing_shop_id = NULL,
    listing_shop_name = NULL,
    updater = 'selected-pool-normalize',
    update_time = NOW()
WHERE deleted = 0
  AND status = 10
  AND (listing_shop_id IS NOT NULL AND TRIM(listing_shop_id) = '');

UPDATE xq_work_order
SET listing_category_id = NULL,
    listing_category_name = NULL,
    updater = 'selected-pool-normalize',
    update_time = NOW()
WHERE deleted = 0
  AND status = 10
  AND (listing_category_id IS NOT NULL AND TRIM(listing_category_id) = '');

-- ========== 3) 无平台池内同 SKU 去重：保留最小 id，其余 soft-delete ==========
UPDATE xq_work_order w
INNER JOIN (
  SELECT external_sku, MIN(id) AS keep_id
  FROM xq_work_order
  WHERE deleted = 0
    AND status = 10
    AND parent_work_order_id IS NULL
    AND (listing_platform_id IS NULL OR listing_platform_id = '')
    AND external_sku IS NOT NULL AND external_sku <> ''
  GROUP BY external_sku
  HAVING COUNT(*) > 1
) d ON d.external_sku = w.external_sku
SET w.deleted = b'1',
    w.updater = 'selected-pool-dedupe',
    w.update_time = NOW()
WHERE w.deleted = 0
  AND w.status = 10
  AND (w.listing_platform_id IS NULL OR w.listing_platform_id = '')
  AND w.id <> d.keep_id;
