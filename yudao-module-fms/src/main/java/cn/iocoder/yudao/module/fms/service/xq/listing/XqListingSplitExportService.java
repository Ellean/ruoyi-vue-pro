package cn.iocoder.yudao.module.fms.service.xq.listing;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingDetailTemplateDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.service.xq.XqListingModeHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_EXPORT_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_TEMPLATE_MISSING;

/**
 * 上架分表导出（ruoyi 内）：读原库 detail_template + 本地模板根目录源 xlsx，填一行工单数据。
 */
@Slf4j
@Service
public class XqListingSplitExportService {

    @Value("${xq.listing-template-root:${yudao.xq.listing-template-root:}}")
    private String listingTemplateRoot;

    @Resource
    private XqListingDetailTemplateQueryHelper detailTemplateQueryHelper;
    @Resource
    private XqListingModeHelper listingModeHelper;

    /**
     * 必须挂起调用方主库事务，否则 @DS("xq") / ContextHolder 切不到原库，
     * 会打到 ruoyi-vue-pro 并报 t_giga_listing_detail_template 不存在。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public XqListingExportResult exportWorkOrder(XqWorkOrderDO order, Map<String, String> listingValues) {
        XqListingModeHelper.PlatformListingMeta meta =
                listingModeHelper.resolveBySourceOrBusinessId(order.getListingPlatformId());
        String sourcePlatformId = meta.sourcePlatformId();
        if (StrUtil.isBlank(sourcePlatformId)) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING, "平台未关联原库上架平台");
        }
        XqListingDetailTemplateDO template = loadTemplate(sourcePlatformId);
        if (template == null || StrUtil.isBlank(template.getSourceFilePath())) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING, "请先在分类维护导入带原始 Excel 的模板");
        }
        Path file = resolveTemplateFile(template.getSourceFilePath());
        if (!Files.isRegularFile(file)) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING,
                    "模板文件不存在：" + file + "（检查 xq.listing-template-root）");
        }
        Map<String, String> values = buildValueMap(order, listingValues);
        try (InputStream in = Files.newInputStream(file);
             Workbook workbook = WorkbookFactory.create(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fillWorkbook(workbook, template, values);
            workbook.write(out);
            String base = StrUtil.blankToDefault(template.getSourceFileName(), "listing");
            base = base.replaceAll("\\.(xlsx|xls|xlsm)$", "");
            String sku = StrUtil.blankToDefault(order.getExternalSku(), "WO" + order.getId());
            String filename = sanitize(base) + "_" + sanitize(sku) + ".xlsx";
            return new XqListingExportResult(
                    out.toByteArray(),
                    filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    1);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[listing-export] fail orderId={} err={}", order.getId(), ex.getMessage());
            throw exception(XQ_LISTING_EXPORT_FAIL, ex.getMessage());
        }
    }

    private XqListingDetailTemplateDO loadTemplate(String sourcePlatformId) {
        try {
            return detailTemplateQueryHelper.selectFirstWithSourceByPlatform(sourcePlatformId);
        } catch (Exception ex) {
            String msg = StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
            if (StrUtil.containsIgnoreCase(msg, "ruoyi-vue-pro")
                    || StrUtil.containsIgnoreCase(msg, "doesn't exist")) {
                throw exception(XQ_LISTING_TEMPLATE_MISSING,
                        "读取原库模板失败（请确认 xq 数据源指向 xq_finance_test 且已有 t_giga_listing_detail_template）："
                                + msg);
            }
            throw ex instanceof RuntimeException re ? re : exception(XQ_LISTING_EXPORT_FAIL, msg);
        }
    }

    private Path resolveTemplateFile(String relativePath) {
        String rel = StrUtil.blankToDefault(relativePath, "").replace('\\', '/').replaceAll("^/+", "");
        String root = StrUtil.blankToDefault(listingTemplateRoot, "").trim();
        if (StrUtil.isBlank(root)) {
            throw exception(XQ_LISTING_TEMPLATE_MISSING,
                    "未配置 xq.listing-template-root（应对齐旧 ERP uploads/gigab2b/listing-templates）");
        }
        return Path.of(root).resolve(rel).normalize();
    }

    private void fillWorkbook(Workbook workbook, XqListingDetailTemplateDO template, Map<String, String> values) {
        JSONArray columns = parseColumns(template.getColumnsJson());
        String sheetName = StrUtil.trim(template.getSheetName());
        Sheet sheet = StrUtil.isNotBlank(sheetName) ? workbook.getSheet(sheetName) : null;
        if (sheet == null) {
            sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet("Listing");
        }
        int headerRowIdx = Math.max(1, template.getHeaderRow() == null ? 1 : template.getHeaderRow()) - 1;
        Integer keyRow = template.getKeyRow();
        int dataStart = template.getDataStartRow() == null
                ? (keyRow != null && keyRow > 0 ? keyRow + 1 : headerRowIdx + 2)
                : template.getDataStartRow();
        int dataRowIdx = Math.max(headerRowIdx + 1, dataStart - 1);
        if (keyRow != null && keyRow > 0 && dataRowIdx < keyRow) {
            dataRowIdx = keyRow;
        }
        Row dataRow = sheet.getRow(dataRowIdx);
        if (dataRow == null) {
            dataRow = sheet.createRow(dataRowIdx);
        }
        if (columns != null && !columns.isEmpty()) {
            for (int i = 0; i < columns.size(); i++) {
                JSONObject col = columns.getJSONObject(i);
                if (col == null) {
                    continue;
                }
                int colIndex = col.getInt("colIndex", col.getInt("index", i));
                String value = resolveColumnValue(col, values);
                if (value == null) {
                    continue;
                }
                Cell cell = dataRow.getCell(colIndex);
                if (cell == null) {
                    cell = dataRow.createCell(colIndex);
                }
                cell.setCellValue(value);
            }
            return;
        }
        // 无 columnsJson：按表头名匹配
        Row header = sheet.getRow(headerRowIdx);
        if (header == null) {
            throw exception(XQ_LISTING_EXPORT_FAIL, "模板缺少表头行");
        }
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell hc = header.getCell(c);
            if (hc == null) {
                continue;
            }
            String headerText = hc.getStringCellValue();
            if (StrUtil.isBlank(headerText)) {
                continue;
            }
            String value = matchByHeader(headerText, values);
            if (value == null) {
                continue;
            }
            Cell cell = dataRow.getCell(c);
            if (cell == null) {
                cell = dataRow.createCell(c);
            }
            cell.setCellValue(value);
        }
    }

    private static String resolveColumnValue(JSONObject col, Map<String, String> values) {
        String code = StrUtil.trim(col.getStr("code", col.getStr("fieldCode")));
        if (StrUtil.isNotBlank(code) && values.containsKey(code)) {
            return values.get(code);
        }
        String valueSource = StrUtil.trim(col.getStr("valueSource")).toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(valueSource) && values.containsKey(valueSource)) {
            return values.get(valueSource);
        }
        String header = StrUtil.trim(col.getStr("header", col.getStr("label", col.getStr("name"))));
        if (StrUtil.isNotBlank(header)) {
            String byHeader = matchByHeader(header, values);
            if (byHeader != null) {
                return byHeader;
            }
        }
        return null;
    }

    private static String matchByHeader(String header, Map<String, String> values) {
        String h = header.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (e.getKey() != null && h.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                return e.getValue();
            }
        }
        if (h.contains("sku") || h.contains("item") && h.contains("code")) {
            return values.get("erp_sku");
        }
        if (h.contains("title") || h.contains("product name") || h.contains("标题")) {
            return values.get("title");
        }
        if (h.contains("description") || h.contains("描述") || h.contains("bullet")) {
            return values.get("description");
        }
        if (h.contains("main image") || h.contains("image 1") || h.contains("主图")) {
            return values.get("image_1");
        }
        return values.getOrDefault(header, null);
    }

    private Map<String, String> buildValueMap(XqWorkOrderDO order, Map<String, String> listingValues) {
        Map<String, String> values = new LinkedHashMap<>();
        if (listingValues != null) {
            listingValues.forEach((k, v) -> {
                if (StrUtil.isNotBlank(k) && v != null) {
                    values.put(k, v);
                }
            });
        }
        String sku = StrUtil.blankToDefault(order.getExternalSku(), "SKU" + order.getId());
        String title = StrUtil.blankToDefault(order.getContentTitle(), order.getTitle());
        values.putIfAbsent("erp_sku", sku);
        values.putIfAbsent("sku", sku);
        values.putIfAbsent("title", StrUtil.blankToDefault(title, ""));
        values.putIfAbsent("product_name", StrUtil.blankToDefault(title, ""));
        values.putIfAbsent("description", StrUtil.blankToDefault(order.getContentSellingPoints(), ""));
        values.putIfAbsent("category", StrUtil.blankToDefault(order.getListingCategoryName(), ""));
        List<String> images = extractImages(order);
        for (int i = 0; i < images.size(); i++) {
            values.putIfAbsent("image_" + (i + 1), images.get(i));
            values.putIfAbsent("main_image_" + (i + 1), images.get(i));
        }
        if (!images.isEmpty()) {
            values.putIfAbsent("main_image", images.get(0));
            values.putIfAbsent("image_url", images.get(0));
        }
        return values;
    }

    private static List<String> extractImages(XqWorkOrderDO order) {
        List<String> urls = new ArrayList<>();
        if (StrUtil.isNotBlank(order.getGeneratedImageUrl())) {
            urls.add(order.getGeneratedImageUrl().trim());
        }
        try {
            JSONArray arr = JSONUtil.parseArray(StrUtil.blankToDefault(order.getImagePromptJson(), "[]"));
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String url = StrUtil.blankToDefault(item.getStr("resultUrl"), item.getStr("generatedImageUrl"));
                if (StrUtil.isNotBlank(url) && !urls.contains(url.trim())) {
                    urls.add(url.trim());
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return urls;
    }

    private static JSONArray parseColumns(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new JSONArray();
        }
        try {
            Object parsed = JSONUtil.parse(raw);
            if (parsed instanceof JSONArray arr) {
                return arr;
            }
            if (parsed instanceof JSONObject obj) {
                JSONArray cols = obj.getJSONArray("columns");
                return cols == null ? new JSONArray() : cols;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return new JSONArray();
    }

    private static String sanitize(String raw) {
        return StrUtil.blankToDefault(raw, "file").replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
    }

}
