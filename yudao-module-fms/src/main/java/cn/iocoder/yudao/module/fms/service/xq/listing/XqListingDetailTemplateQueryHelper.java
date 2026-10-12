package cn.iocoder.yudao.module.fms.service.xq.listing;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingDetailTemplateDO;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主库 detail_template 查询（t_giga_listing_detail_template 已迁入 ruoyi-vue-pro）。
 */
@Slf4j
@Service
public class XqListingDetailTemplateQueryHelper {

    private static final String DS_MASTER = "master";

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
        JdbcTemplate jdbc = new JdbcTemplate(requireMasterDataSource());
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

    private DataSource requireMasterDataSource() {
        Map<String, DataSource> map = dynamicRoutingDataSource.getDataSources();
        DataSource ds = map == null ? null : map.get(DS_MASTER);
        if (ds == null) {
            try {
                ds = dynamicRoutingDataSource.getDataSource(DS_MASTER);
            } catch (Exception ignored) {
                ds = null;
            }
        }
        if (ds == null) {
            // primary 即 master
            return dynamicRoutingDataSource;
        }
        return ds;
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
