package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 根据原库 catalog_source 解析导表 / API 上架模式。
 */
@Component
public class XqListingModeHelper {

    public static final String MODE_EXPORT = "export";
    public static final String MODE_API = "api";

    @Resource
    private XqPlatformMapper platformMapper;
    @Resource
    private XqListingPlatformMapper listingPlatformMapper;

    public record PlatformListingMeta(
            Long platformId,
            String sourcePlatformId,
            String code,
            String name,
            String catalogSource,
            String listingMode,
            String apiProvider
    ) {}

    public PlatformListingMeta resolveByBusinessPlatformId(String platformIdRaw) {
        Long platformId = parseLong(platformIdRaw);
        if (platformId == null) {
            return defaultMeta(null, null, null, null);
        }
        XqPlatformDO biz = platformMapper.selectById(platformId);
        if (biz == null) {
            return defaultMeta(platformId, null, null, null);
        }
        XqListingPlatformDO legacy = loadLegacy(biz.getSourcePlatformId(), biz.getCode());
        return toMeta(biz.getId(), biz.getSourcePlatformId(), biz.getCode(), biz.getName(), legacy);
    }

    public PlatformListingMeta resolveBySourceOrBusinessId(String listingPlatformId) {
        if (StrUtil.isBlank(listingPlatformId)) {
            return defaultMeta(null, null, null, null);
        }
        String raw = listingPlatformId.trim();
        Long asLong = parseLong(raw);
        if (asLong != null) {
            XqPlatformDO biz = platformMapper.selectById(asLong);
            if (biz != null) {
                XqListingPlatformDO legacy = loadLegacy(biz.getSourcePlatformId(), biz.getCode());
                return toMeta(biz.getId(), biz.getSourcePlatformId(), biz.getCode(), biz.getName(), legacy);
            }
        }
        XqListingPlatformDO legacy = loadLegacy(raw, null);
        if (legacy != null) {
            XqPlatformDO biz = null;
            for (XqPlatformDO p : platformMapper.selectListAll()) {
                if (raw.equals(StrUtil.trim(p.getSourcePlatformId()))) {
                    biz = p;
                    break;
                }
            }
            return toMeta(biz == null ? null : biz.getId(),
                    legacy.getId(), legacy.getCode(), legacy.getName(), legacy);
        }
        return defaultMeta(asLong, raw, null, null);
    }

    public static String listingModeOf(String catalogSource) {
        String s = StrUtil.blankToDefault(catalogSource, "excel_template").trim().toLowerCase(Locale.ROOT);
        if (s.contains("mirakl") || s.contains("wayfair") || "api".equals(s)) {
            return MODE_API;
        }
        return MODE_EXPORT;
    }

    public static String apiProviderOf(String catalogSource, String platformCode) {
        String s = StrUtil.blankToDefault(catalogSource, "").toLowerCase(Locale.ROOT);
        String code = StrUtil.blankToDefault(platformCode, "").toLowerCase(Locale.ROOT);
        if (s.contains("wayfair") || code.contains("wayfair") || code.contains("沃宜坊")) {
            return "wayfair";
        }
        if (s.contains("mirakl") || code.contains("kohls") || code.contains("bestbuy")
                || code.contains("lowes") || code.contains("homedepot")) {
            return "mirakl";
        }
        if (MODE_API.equals(listingModeOf(catalogSource))) {
            return s.contains("wayfair") ? "wayfair" : "mirakl";
        }
        return null;
    }

    private XqListingPlatformDO loadLegacy(String sourcePlatformId, String code) {
        // t_giga_listing_platform 已在 master
        try {
            if (StrUtil.isNotBlank(sourcePlatformId)) {
                XqListingPlatformDO byId = listingPlatformMapper.selectById(sourcePlatformId.trim());
                if (byId != null) {
                    return byId;
                }
            }
            if (StrUtil.isNotBlank(code)) {
                return listingPlatformMapper.selectByCode(code.trim());
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PlatformListingMeta toMeta(
            Long platformId, String sourcePlatformId, String code, String name, XqListingPlatformDO legacy) {
        String catalog = legacy == null ? "excel_template"
                : StrUtil.blankToDefault(legacy.getCatalogSource(), "excel_template");
        String mode = listingModeOf(catalog);
        String provider = MODE_API.equals(mode) ? apiProviderOf(catalog, code) : null;
        String srcId = StrUtil.blankToDefault(sourcePlatformId,
                legacy == null ? null : legacy.getId());
        return new PlatformListingMeta(platformId, srcId, code, name, catalog, mode, provider);
    }

    private static PlatformListingMeta defaultMeta(
            Long platformId, String sourcePlatformId, String code, String name) {
        return new PlatformListingMeta(platformId, sourcePlatformId, code, name,
                "excel_template", MODE_EXPORT, null);
    }

    private static Long parseLong(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

}
