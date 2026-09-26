/**
 * Import Giga site categories JSON into ruoyi-vue-pro.xq_giga_site_category
 * and seed demo products for card UI.
 */
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const JSON_PATH =
  process.argv[2] ||
  path.resolve(
    'D:/xq-xm/xq-erp/tools/gigab2b-category-scraper/data/gigab2b-categories-tree-2026-09-24-00-50-56.json',
  );

const SITE_ORIGIN = 'https://www.gigab2b.com';

function toHref(gigaId) {
  return `${SITE_ORIGIN}/index.php?route=product/category&product_category_id=${gigaId}`;
}

function walk(nodes, parentId, level, pathIds, pathNames, fetchedAt, rows) {
  (Array.isArray(nodes) ? nodes : []).forEach((node, index) => {
    const gigaId = Number(node?.id);
    if (!Number.isFinite(gigaId) || gigaId <= 0) return;
    const name = String(node?.name || '').trim();
    if (!name) return;
    const nextPathIds = pathIds.concat(String(gigaId));
    const nextPathNames = pathNames.concat(name);
    rows.push({
      giga_id: gigaId,
      parent_id: parentId,
      name,
      level,
      sort_order: index,
      image_path: node?.image_path ? String(node.image_path) : null,
      path_ids: nextPathIds.join('/'),
      path_names: nextPathNames.join(' > '),
      href: toHref(gigaId),
      source: 'gigab2b_header',
      fetched_at: fetchedAt,
    });
    if (Array.isArray(node.children) && node.children.length) {
      walk(node.children, gigaId, level + 1, nextPathIds, nextPathNames, fetchedAt, rows);
    }
  });
  return rows;
}

async function ensureSchema(conn) {
  await conn.query(`
CREATE TABLE IF NOT EXISTS xq_giga_site_category (
  id bigint unsigned NOT NULL AUTO_INCREMENT,
  giga_id bigint unsigned NOT NULL,
  parent_id bigint unsigned DEFAULT NULL,
  name varchar(255) NOT NULL,
  level tinyint unsigned NOT NULL DEFAULT 1,
  sort_order int NOT NULL DEFAULT 0,
  image_path varchar(1000) DEFAULT NULL,
  path_ids varchar(255) DEFAULT NULL,
  path_names varchar(1000) DEFAULT NULL,
  href varchar(500) DEFAULT NULL,
  source varchar(100) DEFAULT 'gigab2b_header',
  fetched_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_giga_id (giga_id),
  KEY idx_parent (parent_id),
  KEY idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`);

  const [cols] = await conn.query(`SHOW COLUMNS FROM xq_product`);
  const names = new Set(cols.map((c) => c.Field));
  const adds = [];
  if (!names.has('giga_category_id')) adds.push('ADD COLUMN giga_category_id bigint DEFAULT NULL AFTER category_name');
  if (!names.has('item_code')) adds.push('ADD COLUMN item_code varchar(64) DEFAULT NULL AFTER sku');
  if (!names.has('qty_available')) adds.push('ADD COLUMN qty_available int DEFAULT NULL AFTER image_url');
  if (!names.has('supplier_code')) adds.push('ADD COLUMN supplier_code varchar(64) DEFAULT NULL AFTER qty_available');
  if (!names.has('supplier_name')) adds.push('ADD COLUMN supplier_name varchar(128) DEFAULT NULL AFTER supplier_code');
  if (!names.has('listed_tag')) adds.push('ADD COLUMN listed_tag varchar(64) DEFAULT NULL AFTER supplier_name');
  if (adds.length) {
    await conn.query(`ALTER TABLE xq_product ${adds.join(', ')}`);
  }
}

async function main() {
  const payload = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
  const fetchedAt = payload.fetchedAt ? new Date(payload.fetchedAt) : new Date();
  const rows = walk(payload.tree || [], null, 1, [], [], fetchedAt, []);
  console.log(`rows=${rows.length}`);

  const conn = await mysql.createConnection({
    host: '127.0.0.1',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'ruoyi-vue-pro',
    charset: 'utf8mb4',
  });

  try {
    await ensureSchema(conn);
    await conn.beginTransaction();
    await conn.query('DELETE FROM xq_giga_site_category');
    const chunk = 100;
    for (let i = 0; i < rows.length; i += chunk) {
      const part = rows.slice(i, i + chunk);
      const values = part.map((r) => [
        r.giga_id,
        r.parent_id,
        r.name,
        r.level,
        r.sort_order,
        r.image_path,
        r.path_ids,
        r.path_names,
        r.href,
        r.source,
        r.fetched_at,
      ]);
      await conn.query(
        `INSERT INTO xq_giga_site_category
        (giga_id,parent_id,name,level,sort_order,image_path,path_ids,path_names,href,source,fetched_at)
        VALUES ?`,
        [values],
      );
    }

    // seed: 2~3 products per L1 so category chips all have samples
    const l1 = rows.filter((r) => r.level === 1);
    const products = [];
    let idx = 0;
    for (const root of l1) {
      const leaves = rows
        .filter(
          (r) =>
            r.level === 3 &&
            r.image_path &&
            (r.path_ids === String(root.giga_id) ||
              r.path_ids.startsWith(`${root.giga_id}/`)),
        )
        .slice(0, 3);
      for (const leaf of leaves) {
        idx += 1;
        const sku = `SKU-GIGA-${String(idx).padStart(3, '0')}`;
        const itemCode = `GC${100000 + idx}`;
        products.push([
          sku,
          itemCode,
          `${leaf.name} Demo ${idx}`,
          root.name,
          leaf.giga_id,
          leaf.image_path,
          0,
          50 + (idx % 20) * 7,
          `W${1500 + (idx % 40)}`,
          ['Bonwell', 'DOUBLEH STORE', 'HIFINE', 'Demo Supplier'][idx % 4],
          idx % 3 === 0 ? 'Listed Today' : idx % 3 === 1 ? 'Listed in the last 7 days' : null,
          'seed from giga category',
        ]);
      }
    }
    await conn.query(`DELETE FROM xq_product WHERE sku LIKE 'SKU-GIGA-%' OR sku LIKE 'SKU-DEMO-%'`);
    if (products.length) {
      await conn.query(
        `INSERT INTO xq_product
        (sku,item_code,name,category_name,giga_category_id,image_url,status,qty_available,supplier_code,supplier_name,listed_tag,remark)
        VALUES ?`,
        [products],
      );
    }

    await conn.commit();
    console.log(`imported categories=${rows.length}, products=${products.length}`);
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    await conn.end();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
