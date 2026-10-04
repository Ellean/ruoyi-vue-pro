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
keys = [
    "ware",
    "仓",
    "store",
    "shop",
    "ship",
    "logistic",
    "gucang",
    "gc_",
    "combine",
    "merge",
]
hit = []
for t in tables:
    low = t.lower()
    if any(k in low for k in keys):
        hit.append(t)
print("hit tables", hit)

for t in hit:
    try:
        c.execute(f"SELECT COUNT(*) FROM `{t}`")
        cnt = c.fetchone()[0]
        c.execute(f"SHOW COLUMNS FROM `{t}`")
        cols = [r[0] for r in c.fetchall()]
        print(f"\n=== {t} ({cnt}) cols={cols[:25]}")
        if cnt and cnt < 5000:
            c.execute(f"SELECT * FROM `{t}` LIMIT 2")
            print("sample", c.fetchall())
    except Exception as e:
        print(t, e)
o.close()
