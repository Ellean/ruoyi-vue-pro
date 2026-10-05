package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Giga detail_json.associateProductList / associateProductInfo → 同一品的变体 SKU。
 */
public final class XqGigaAssociateSupport {

    private static final Pattern COLOR_SUFFIX = Pattern.compile("^(.*\\d)[A-Za-z]$");
    private static final int MAX_FAMILY = 12;

    private XqGigaAssociateSupport() {
    }

    public static List<String> parseSkuList(String json) {
        List<String> out = new ArrayList<>();
        if (StrUtil.isBlank(json) || "null".equalsIgnoreCase(json.trim())) {
            return out;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object o : arr) {
                String sku = StrUtil.trim(String.valueOf(o));
                if (StrUtil.isNotBlank(sku) && !"null".equalsIgnoreCase(sku)) {
                    out.add(sku);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return out;
    }

    public static Map<String, String> parseInfoNames(String infoJson) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StrUtil.isBlank(infoJson) || "null".equalsIgnoreCase(infoJson.trim())) {
            return map;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(infoJson);
            for (Object o : arr) {
                if (!(o instanceof JSONObject jo)) {
                    continue;
                }
                String sku = StrUtil.blankToDefault(jo.getStr("itemCode"), jo.getStr("sku"));
                String name = StrUtil.blankToDefault(jo.getStr("name"), jo.getStr("mainColor"));
                if (StrUtil.isNotBlank(sku)) {
                    map.put(sku.trim(), StrUtil.blankToDefault(name, sku.trim()));
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return map;
    }

    public static String stem(String sku) {
        String s = StrUtil.trim(sku);
        if (StrUtil.isBlank(s)) {
            return "";
        }
        Matcher m = COLOR_SUFFIX.matcher(s);
        return m.matches() ? m.group(1) : s;
    }

    /** 当前 SKU + 同品变体（list + info 的 itemCode） */
    public static List<String> familySkus(String sku, String associateListJson) {
        return familySkus(sku, associateListJson, null);
    }

    public static List<String> familySkus(String sku, String associateListJson, String infoJson) {
        Set<String> set = new LinkedHashSet<>();
        String self = StrUtil.trim(sku);
        if (StrUtil.isNotBlank(self)) {
            set.add(self);
        }
        String prefix = stem(self);
        boolean stemFilter = StrUtil.isNotBlank(prefix) && !prefix.equals(self);
        List<String> codes = new ArrayList<>(parseSkuList(associateListJson));
        codes.addAll(parseInfoNames(infoJson).keySet());
        for (String s : codes) {
            if (stemFilter && !s.startsWith(prefix) && !s.contains(prefix)) {
                continue;
            }
            set.add(s);
            if (set.size() >= MAX_FAMILY) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    public static String variantLabel(String sku, String mainColor, String infoJson) {
        Map<String, String> names = parseInfoNames(infoJson);
        String hit = names.get(StrUtil.trim(sku));
        if (StrUtil.isNotBlank(hit)) {
            return hit;
        }
        if (StrUtil.isNotBlank(mainColor)) {
            return mainColor.trim();
        }
        return StrUtil.blankToDefault(sku, "");
    }

}
