# -*- coding: utf-8 -*-
import pymysql

o = pymysql.connect(
    host="182.92.3.138",
    user="root",
    password="qwer123456..",
    database="xq_finance_test",
    charset="utf8mb4",
)
c = o.cursor()
c.execute("SHOW TABLES")
tables = [r[0] for r in c.fetchall()]
# look for bind/map/config containing store+ware
cands = []
for t in tables:
    low = t.lower()
    if any(
        x in low
        for x in [
            "bind",
            "map",
            "config",
            "relation",
            "ship_ware",
            "store_ware",
            "shop_ware",
            "ware_store",
            "ware_shop",
            "gc_",
            "goodcang",
            "physical",
        ]
    ):
        cands.append(t)
print("cands", cands)

# dump warehouse names utf8
c.execute(
    "SELECT id, warehouse_code, warehouse_name, country_code, status FROM t_erp_warehouse"
)
import json

rows = c.fetchall()
with open(r"D:\ruoyi-vue-pro\tmp_wh.json", "w", encoding="utf-8") as f:
    json.dump(rows, f, ensure_ascii=False, indent=2)

c.execute(
    "SELECT id, wp_code, wp_name, warehouse_id, country_code, status FROM t_erp_warehouse_address"
)
with open(r"D:\ruoyi-vue-pro\tmp_wh_addr.json", "w", encoding="utf-8") as f:
    json.dump(c.fetchall(), f, ensure_ascii=False, indent=2)

# search columns mentioning store/shop + warehouse
for t in tables:
    try:
        c.execute(f"SHOW COLUMNS FROM `{t}`")
        cols = [r[0].lower() for r in c.fetchall()]
        has_store = any(x in cols for x in ["store_id", "shop_id", "shopid", "storeid"])
        has_ware = any("ware" in x for x in cols)
        if has_store and has_ware:
            print("JOIN TABLE", t, cols)
    except Exception:
        pass
o.close()
print("done")
