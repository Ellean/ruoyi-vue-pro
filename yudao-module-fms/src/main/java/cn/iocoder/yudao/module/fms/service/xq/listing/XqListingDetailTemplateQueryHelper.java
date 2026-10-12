package cn.iocoder.yudao.module.fms.service.xq.listing;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingDetailTemplateDO;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_TEMPLATE_MISSING;

/**
 * 原库 detail_template 查询。
 * <p>
 * 不用 Mapper/@DS：主库 MyBatis 会话在同线程绑死后，@DS("xq") 仍可能拿到 master 连接。
 * 这里从 DynamicRoutingDataSource 显式取名为 xq 的 DataSource 再查。
 */
@Slf4j
@Service
public class XqListingDetailTemplateQueryHelper {

    private static final String DS_XQ = "xq";

    private final DynamicRoutingDataSource dynamicRoutingDataSource;

    public XqListingDetailTemplateQueryHelper(DataSource dataSource) {
        if (!(dataSource instanceof DynamicRoutingDataSource routing)) {
            throw new IllegalStateException("期望 DynamicRoutingDataSource，实际: "
                    + (dataSource == null ? "null" : dataSource.getClass().getName()));
        }
        this.dynamicRoutingDataSource = routing;
    }

    public XqListingDetailTemplateDO selectFirstWithSourceByPlatform(String platformId) {
        if (StrUtil.isBlank(platformId)) {
            return null;
        }
        DataSource xqDs = requireXqDataSource();
        assertNotMasterCatalog(xqDs);
        JdbcTemplate jdbc = new JdbcTemplate(xqDs);
        String sql = """
                SELECT id, user_id, platform_id, name, source_file_name, source_file_path,
                       header_row, sheet_name, key_row, data_start_row, columns_json,
                       workbook_snapshot_json, tables_json, create_date, update_date
                FROM t_giga_listing_detail_template
                WHERE platform_id = ? AND source_file_path IS NOT NULL AND source_file_path <> ''
                ORDER BY update_date DESC
                LIMIT 1
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, platformId.trim());
        if (rows.isEmpty()) {
            return null;
        }
        return mapRow(rows.get(0));
    }

    private DataSource requireXqDataSource() {
        Map<String, DataSource> map = dynamicRoutingDataSource.getDataSources();
        DataSource ds = map == null ? null : map.get(DS_XQ);
        if (ds == null) {
            // lazy 数据源有时不在 map 里，再走一次按名获取
            try {
                ds = dynamicRoutingDataSource.getDataSource(DS_XQ);
            } catch (Exception ignored) {
                ds = null;
            }
        }
        if (ds == null) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING,
                    "未配置数据源 xq（当前已加载: "
                            + (map == null || map.isEmpty() ? "无" : String.join(",", map.keySet()))
                            + "）。请在 application-local.yaml 配置 "
                            + "spring.datasource.dynamic.datasource.xq.url 指向 xq_finance_test");
        }
        return ds;
    }

    /** 启动后若 xq 误指主库，尽早给出可读错误 */
    private void assertNotMasterCatalog(DataSource xqDs) {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(xqDs);
            String catalog = StrUtil.blankToDefault(conn.getCatalog(), "");
            String url = "";
            try {
                url = StrUtil.blankToDefault(conn.getMetaData().getURL(), "");
            } catch (Exception ignored) {
                // ignore
            }
            if (StrUtil.containsIgnoreCase(catalog, "ruoyi-vue-pro")
                    || StrUtil.containsIgnoreCase(url, "ruoyi-vue-pro")) {
                log.error("[listing-template] xq 数据源指到了主库 catalog={} url={}", catalog, url);
                throw exception(XQ_LISTING_TEMPLATE_MISSING,
                        "数据源 xq 当前连到 " + catalog + "，应指向 xq_finance_test。"
                                + "请检查 application-local.yaml 中 spring.datasource.dynamic.datasource.xq.url");
            }
            log.debug("[listing-template] xq datasource ok catalog={} url={}", catalog, url);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING, "无法连接 xq 数据源: " + ex.getMessage());
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, xqDs);
            }
        }
    }

    private static XqListingDetailTemplateDO mapRow(Map<String, Object> row) {
        XqListingDetailTemplateDO t = new XqListingDetailTemplateDO();
        t.setId(str(row.get("id")));
        t.setUserId(str(row.get("user_id")));
        t.setPlatformId(str(row.get("platform_id")));
        t.setName(str(row.get("name")));
        t.setSourceFileName(str(row.get("source_file_name")));
        t.setSourceFilePath(str(row.get("source_file_path")));
        t.setHeaderRow(toInt(row.get("header_row")));
        t.setSheetName(str(row.get("sheet_name")));
        t.setKeyRow(toInt(row.get("key_row")));
        t.setDataStartRow(toInt(row.get("data_start_row")));
        t.setColumnsJson(str(row.get("columns_json")));
        t.setWorkbookSnapshotJson(str(row.get("workbook_snapshot_json")));
        t.setTablesJson(str(row.get("tables_json")));
        t.setCreateDate(toLdt(row.get("create_date")));
        t.setUpdateDate(toLdt(row.get("update_date")));
        return t;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Integer.parseInt(s);
    }

    private static LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (v instanceof java.util.Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime();
        }
        return null;
    }

}
