# -*- coding: utf-8 -*-
import json
from datetime import datetime

import pymysql

AI_FILTER = """
  agent_results_json IS NOT NULL AND agent_results_json <> ''
  AND agent_results_json <> 'null'
  AND JSON_EXTRACT(agent_results_json, '$.images') IS NOT NULL
  AND JSON_LENGTH(JSON_EXTRACT(agent_results_json, '$.images')) > 0
"""


def main():
    ry = pymysql.connect(
        host="127.0.0.1", user="root", password="root", database="ruoyi-vue-pro", charset="utf8mb4"
    )
    xq = pymysql.connect(
        host="127.0.0.1", user="root", password="root", database="xq_finance_test", charset="utf8mb4"
    )
    rc = ry.cursor(pymysql.cursors.DictCursor)
    xc = xq.cursor(pymysql.cursors.DictCursor)
    out = {"at": datetime.now().isoformat()}

    ry_tables = [
        "xq_work_order",
        "xq_product",
        "xq_source_item",
        "xq_platform",
        "xq_platform_alias",
        "xq_store",
        "xq_user_store",
        "xq_warehouse",
        "xq_giga_site_category",
        "xq_giga_api_credential",
        "xq_listing_platform_credential",
        "xq_rpa_global_config",
        "xq_rpa_user_config",
        "t_giga_listing_category_field_config",
        "t_giga_listing_field_pool",
        "t_gigab2b_product_family_index",
        "t_gigab2b_associate_gap",
    ]
    out["ruoyi_counts"] = {}
    for t in ry_tables:
        rc.execute(f"SELECT COUNT(*) AS c FROM `{t}`")
        out["ruoyi_counts"][t] = rc.fetchone()["c"]

    rc.execute("SELECT COUNT(*) AS c FROM xq_work_order WHERE deleted = 0")
    out["wo_active"] = rc.fetchone()["c"]
    rc.execute(
        "SELECT COUNT(*) AS c FROM xq_work_order WHERE deleted = 0 AND DATE(create_time) = CURDATE()"
    )
    out["wo_created_today"] = rc.fetchone()["c"]
    rc.execute(
        "SELECT COUNT(*) AS c FROM xq_work_order WHERE deleted = 0 AND DATE(update_time) = CURDATE()"
    )
    out["wo_updated_today"] = rc.fetchone()["c"]
    rc.execute(
        """
        SELECT MIN(create_time) AS min_create, MAX(create_time) AS max_create,
               MIN(update_time) AS min_update, MAX(update_time) AS max_update
        FROM xq_work_order WHERE deleted = 0
        """
    )
    out["wo_time_range"] = rc.fetchone()

    rc.execute(
        """
        SELECT status, workflow_phase, listing_status,
               rpa_copy_status, rpa_image_status, image_status, COUNT(*) AS c
        FROM xq_work_order WHERE deleted = 0
        GROUP BY status, workflow_phase, listing_status, rpa_copy_status, rpa_image_status, image_status
        ORDER BY c DESC LIMIT 30
        """
    )
    out["wo_status_matrix"] = rc.fetchall()

    rc.execute(
        """
        SELECT creator, COUNT(*) AS c, MIN(create_time) AS first_at, MAX(create_time) AS last_at
        FROM xq_work_order WHERE deleted = 0
        GROUP BY creator ORDER BY c DESC LIMIT 12
        """
    )
    out["wo_by_creator"] = rc.fetchall()

    rc.execute(
        """
        SELECT DATE(create_time) AS d, COUNT(*) AS c
        FROM xq_work_order
        WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
        GROUP BY DATE(create_time) ORDER BY d
        """
    )
    out["wo_created_14d"] = [{**r, "d": str(r["d"])} for r in rc.fetchall()]

    rc.execute(
        """
        SELECT product_sku, COUNT(*) AS c, GROUP_CONCAT(id ORDER BY id) AS ids
        FROM xq_work_order
        WHERE deleted = 0 AND product_sku IS NOT NULL AND product_sku <> ''
        GROUP BY product_sku HAVING c > 1
        ORDER BY c DESC LIMIT 15
        """
    )
    out["duplicate_product_sku"] = rc.fetchall()
    rc.execute(
        """
        SELECT COUNT(*) AS dup_groups, COALESCE(SUM(c - 1), 0) AS extra_rows
        FROM (
          SELECT product_sku, COUNT(*) AS c FROM xq_work_order
          WHERE deleted = 0 AND product_sku <> ''
          GROUP BY product_sku HAVING COUNT(*) > 1
        ) t
        """
    )
    out["duplicate_product_sku_summary"] = rc.fetchone()

    rc.execute(
        """
        SELECT COUNT(*) AS c FROM xq_work_order
        WHERE deleted = 0
          AND (image_prompt_json IS NULL OR TRIM(image_prompt_json) IN ('', '{}', '[]'))
        """
    )
    out["wo_image_prompt_empty"] = rc.fetchone()["c"]
    rc.execute(
        """
        SELECT COUNT(*) AS c FROM xq_work_order
        WHERE deleted = 0
          AND copy_result_json IS NOT NULL
          AND TRIM(copy_result_json) NOT IN ('', '{}', '[]')
        """
    )
    out["wo_has_copy_result"] = rc.fetchone()["c"]
    rc.execute(
        """
        SELECT COUNT(*) AS c FROM xq_work_order
        WHERE deleted = 0 AND (
          rpa_copy_status IN ('running', 'RUNNING', 'pending', 'PENDING')
          OR rpa_image_status IN ('running', 'RUNNING', 'pending', 'PENDING')
        )
        """
    )
    out["wo_rpa_pending_running"] = rc.fetchone()["c"]
    rc.execute(
        "SELECT COUNT(*) AS c FROM xq_work_order WHERE deleted = 0 AND parent_work_order_id IS NOT NULL"
    )
    out["wo_variant_children"] = rc.fetchone()["c"]

    xq_tables = [
        "t_gigab2b_product_list",
        "t_gigab2b_product_snapshot",
        "t_gigab2b_agent_result_public",
        "t_gigab2b_sync_combo",
        "t_gigab2b_user_config",
        "t_giga_listing_platform",
        "t_giga_listing_shop",
        "t_giga_listing_category",
        "t_giga_listing_category_shop",
        "t_giga_listing_field_pool",
        "t_giga_listing_shop_field_config",
        "t_giga_site_category",
        "t_giga_copy_gen_rule",
        "t_giga_image_gen_rule",
        "t_gigab2b_product_family_index",
        "t_gigab2b_associate_gap",
        "xq_listing_platform_credential",
    ]
    out["finance_counts"] = {}
    for t in xq_tables:
        xc.execute(f"SELECT COUNT(*) AS c FROM `{t}`")
        out["finance_counts"][t] = xc.fetchone()["c"]

    xc.execute(f"SELECT COUNT(*) AS c FROM t_gigab2b_product_list WHERE {AI_FILTER}")
    out["list_ai_with_images"] = xc.fetchone()["c"]
    xc.execute(
        f"""
        SELECT COUNT(*) AS c FROM t_gigab2b_product_list
        WHERE {AI_FILTER}
          AND listing_platform_id IS NOT NULL AND TRIM(listing_platform_id) <> ''
          AND assign_owner_user_id IS NOT NULL AND TRIM(assign_owner_user_id) <> ''
          AND assign_image_user_id IS NOT NULL AND TRIM(assign_image_user_id) <> ''
        """
    )
    out["list_ai_platform_assigned"] = xc.fetchone()["c"]
    xc.execute(
        f"""
        SELECT COUNT(*) AS c FROM t_gigab2b_product_list
        WHERE {AI_FILTER}
          AND (listing_platform_id IS NULL OR TRIM(listing_platform_id) = '')
          AND assign_owner_user_id IS NOT NULL AND TRIM(assign_owner_user_id) <> ''
          AND assign_image_user_id IS NOT NULL AND TRIM(assign_image_user_id) <> ''
        """
    )
    out["list_ai_no_platform_assigned"] = xc.fetchone()["c"]

    rc.execute(
        """
        SELECT external_sku, listing_platform_id
        FROM xq_work_order
        WHERE deleted = 0 AND status IN (10, 20)
          AND external_sku IS NOT NULL AND external_sku <> ''
        """
    )
    wo_keys = {(r["external_sku"], str(r["listing_platform_id"] or "")) for r in rc.fetchall()}
    xc.execute(
        f"""
        SELECT sku, listing_platform_id FROM t_gigab2b_product_list
        WHERE {AI_FILTER}
          AND listing_platform_id IS NOT NULL AND TRIM(listing_platform_id) <> ''
          AND assign_owner_user_id IS NOT NULL AND TRIM(assign_owner_user_id) <> ''
          AND assign_image_user_id IS NOT NULL AND TRIM(assign_image_user_id) <> ''
        """
    )
    pending = 0
    for r in xc.fetchall():
        if (r["sku"], str(r["listing_platform_id"] or "")) not in wo_keys:
            pending += 1
    out["pending_dispatch_ai_platform"] = pending
    out["wo_dedupe_keys_active"] = len(wo_keys)

    ry.close()
    xq.close()
    print(json.dumps(out, ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()
