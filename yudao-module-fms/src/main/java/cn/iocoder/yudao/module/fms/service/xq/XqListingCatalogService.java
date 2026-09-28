package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCategoryRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingPlatformRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingShopRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqCopyGenRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqImageGenRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSysStoreDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqCopyGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqImageGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqSysStoreMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class XqListingCatalogService {

    @Resource
    private XqListingPlatformMapper platformMapper;
    @Resource
    private XqSysStoreMapper sysStoreMapper;
    @Resource
    private XqListingCategoryMapper categoryMapper;
    @Resource
    private XqCopyGenRuleMapper copyGenRuleMapper;
    @Resource
    private XqImageGenRuleMapper imageGenRuleMapper;

    /**
     * 后台 sys_store.platform → gigab2b 平台 code（与旧 ERP 一致）
     */
    private static final Map<String, String> SYS_STORE_PLATFORM_TO_GIGA_CODE = buildStorePlatformMap();

    private static Map<String, String> buildStorePlatformMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("amazon", "amz");
        map.put("amz", "amz");
        map.put("amazon_us", "amz");
        map.put("amazon_com", "amz");
        map.put("amazoncom", "amz");
        map.put("亚马逊", "amz");
        map.put("overstock", "overstock");
        map.put("ost", "overstock");
        map.put("walmart", "walmart");
        map.put("wm", "walmart");
        map.put("沃尔玛", "walmart");
        map.put("wayfair", "wayfair");
        map.put("沃宜坊", "wayfair");
        map.put("homedepot", "home_depot");
        map.put("home_depot", "home_depot");
        map.put("hd", "home_depot");
        map.put("家得宝", "home_depot");
        map.put("bestbuy", "best_buy");
        map.put("best_buy", "best_buy");
        map.put("bonanza", "best_buy");
        map.put("bb", "best_buy");
        map.put("百思买", "best_buy");
        map.put("kohls", "kohl_s");
        map.put("kohl_s", "kohl_s");
        map.put("kohl's", "kohl_s");
        map.put("lowes", "lowes");
        map.put("lowe's", "lowes");
        map.put("劳氏", "lowes");
        map.put("劳斯", "lowes");
        return map;
    }

    public List<XqListingPlatformRespVO> listPlatforms() {
        return BeanUtils.toBean(platformMapper.selectEnabledList(), XqListingPlatformRespVO.class);
    }

    /**
     * 店铺列表：与旧 ERP 一致，读 sys_store，再映射到 t_giga_listing_platform。
     * （t_giga_listing_shop 几乎为空，不是业务主数据源）
     */
    public List<XqListingShopRespVO> listShops(String platformId) {
        List<XqListingPlatformDO> platforms = platformMapper.selectEnabledList();
        Map<String, XqListingPlatformDO> byCode = platforms.stream()
                .filter(p -> StrUtil.isNotBlank(p.getCode()))
                .collect(Collectors.toMap(
                        p -> p.getCode().toLowerCase(Locale.ROOT),
                        p -> p,
                        (a, b) -> a));
        List<XqSysStoreDO> stores = sysStoreMapper.selectEnabledList();
        List<XqListingShopRespVO> result = new ArrayList<>();
        for (XqSysStoreDO store : stores) {
            XqListingPlatformDO platform = resolveGigaPlatform(store.getPlatform(), byCode, platforms);
            if (StrUtil.isNotBlank(platformId)) {
                if (platform == null || !platformId.equals(platform.getId())) {
                    continue;
                }
            }
            XqListingShopRespVO vo = new XqListingShopRespVO();
            vo.setId(String.valueOf(store.getId()));
            vo.setPlatformId(platform != null ? platform.getId() : "");
            vo.setCode(StrUtil.blankToDefault(store.getName(), String.valueOf(store.getId())));
            vo.setName(store.getName());
            vo.setEnabled(store.getStatus() == null || store.getStatus() == 1);
            vo.setStorePlatform(store.getPlatform());
            result.add(vo);
        }
        return result;
    }

    private static String normalizeStorePlatform(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s'\"_-]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static XqListingPlatformDO resolveGigaPlatform(
            String storePlatform,
            Map<String, XqListingPlatformDO> byCode,
            List<XqListingPlatformDO> platforms) {
        String norm = normalizeStorePlatform(storePlatform);
        if (StrUtil.isBlank(norm)) {
            return null;
        }
        String code = SYS_STORE_PLATFORM_TO_GIGA_CODE.getOrDefault(norm, norm);
        XqListingPlatformDO matched = byCode.get(code.toLowerCase(Locale.ROOT));
        if (matched != null) {
            return matched;
        }
        for (XqListingPlatformDO p : platforms) {
            String pCode = normalizeStorePlatform(p.getCode());
            if (pCode.equals(norm) || pCode.equals(normalizeStorePlatform(code))) {
                return p;
            }
        }
        for (XqListingPlatformDO p : platforms) {
            String pName = normalizeStorePlatform(p.getName());
            if (StrUtil.isNotBlank(pName)
                    && (pName.equals(norm) || pName.contains(norm) || norm.contains(pName))) {
                return p;
            }
        }
        return null;
    }

    /**
     * 按平台返回上架分类树（原库 t_giga_listing_category）
     */
    public List<XqListingCategoryRespVO> listCategoryTree(String platformId) {
        if (StrUtil.isBlank(platformId)) {
            return List.of();
        }
        List<XqListingCategoryDO> rows = categoryMapper.selectEnabledByPlatformId(platformId);
        Map<String, XqListingCategoryRespVO> nodeMap = new HashMap<>();
        for (XqListingCategoryDO row : rows) {
            XqListingCategoryRespVO node = BeanUtils.toBean(row, XqListingCategoryRespVO.class);
            node.setChildren(new ArrayList<>());
            nodeMap.put(row.getId(), node);
        }
        List<XqListingCategoryRespVO> roots = new ArrayList<>();
        for (XqListingCategoryDO row : rows) {
            XqListingCategoryRespVO node = nodeMap.get(row.getId());
            String parentId = StrUtil.blankToDefault(row.getParentId(), "");
            if (StrUtil.isBlank(parentId) || "0".equals(parentId) || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }

    public List<XqCopyGenRuleRespVO> listCopyRules() {
        List<XqListingPlatformDO> platforms = platformMapper.selectEnabledList();
        Map<String, XqListingPlatformDO> platformMap = platforms.stream()
                .collect(Collectors.toMap(XqListingPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqCopyGenRuleDO> rules = copyGenRuleMapper.selectAll();
        List<XqCopyGenRuleRespVO> result = new ArrayList<>();
        for (XqCopyGenRuleDO rule : rules) {
            XqCopyGenRuleRespVO vo = BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
            XqListingPlatformDO p = platformMap.get(StrUtil.blankToDefault(rule.getPlatformId(), ""));
            if (p != null) {
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
            } else if (StrUtil.isBlank(rule.getPlatformId())) {
                vo.setPlatformCode("global");
                vo.setPlatformName("通用规则");
            }
            result.add(vo);
        }
        // 补齐尚无规则的平台，方便前端配置
        for (XqListingPlatformDO p : platforms) {
            boolean exists = rules.stream().anyMatch(r -> p.getId().equals(r.getPlatformId()));
            if (!exists) {
                XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
                vo.setPlatformId(p.getId());
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
                vo.setCode("deep");
                vo.setName(p.getName() + "文案规则");
                vo.setEnabled(true);
                vo.setConfigJson(defaultConfigJson());
                result.add(vo);
            }
        }
        return result;
    }

    public XqCopyGenRuleRespVO getCopyRule(String platformId) {
        XqCopyGenRuleDO rule = copyGenRuleMapper.selectByPlatformId(platformId);
        if (rule == null) {
            XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
            vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
            vo.setCode("deep");
            vo.setName("文案生成规则");
            vo.setEnabled(true);
            vo.setConfigJson(defaultConfigJson());
            return vo;
        }
        return BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
    }

    public void saveCopyRule(XqCopyGenRuleSaveReqVO reqVO) {
        String platformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        XqCopyGenRuleDO exists = copyGenRuleMapper.selectByPlatformId(platformId);
        LocalDateTime now = LocalDateTime.now();
        if (exists == null) {
            XqCopyGenRuleDO insert = new XqCopyGenRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(platformId);
            insert.setCode("deep");
            insert.setName(reqVO.getName());
            insert.setConfigJson(reqVO.getConfigJson());
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setRemark(reqVO.getRemark());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            copyGenRuleMapper.insert(insert);
            return;
        }
        XqCopyGenRuleDO update = new XqCopyGenRuleDO();
        update.setId(exists.getId());
        update.setName(reqVO.getName());
        update.setConfigJson(reqVO.getConfigJson());
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(reqVO.getRemark());
        update.setUpdateDate(now);
        copyGenRuleMapper.updateById(update);
    }

    public List<XqImageGenRuleRespVO> listImageRules(String platformId) {
        String pid = StrUtil.blankToDefault(platformId, "");
        List<XqImageGenRuleDO> rows = imageGenRuleMapper.selectByPlatformId(pid);
        Map<String, XqListingPlatformDO> platformMap = platformMapper.selectEnabledList().stream()
                .collect(Collectors.toMap(XqListingPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqImageGenRuleRespVO> result = new ArrayList<>();
        for (XqImageGenRuleDO row : rows) {
            XqImageGenRuleRespVO vo = BeanUtils.toBean(row, XqImageGenRuleRespVO.class);
            XqListingPlatformDO p = platformMap.get(StrUtil.blankToDefault(row.getPlatformId(), ""));
            if (p != null) {
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
            }
            result.add(vo);
        }
        return result;
    }

    public XqImageGenRuleRespVO getImageRule(String platformId, String categoryId) {
        String pid = StrUtil.blankToDefault(platformId, "");
        String cid = StrUtil.blankToDefault(categoryId, "");
        XqImageGenRuleDO rule = imageGenRuleMapper.selectByPlatformAndCategory(pid, cid);
        if (rule == null) {
            XqImageGenRuleRespVO vo = new XqImageGenRuleRespVO();
            vo.setPlatformId(pid);
            vo.setCategoryId(cid);
            vo.setName(StrUtil.isBlank(cid) ? "平台默认图片提示词" : "分类图片提示词");
            vo.setPromptText("");
            vo.setNegativePrompt("");
            vo.setEnabled(true);
            vo.setConfigJson("{}");
            return vo;
        }
        return BeanUtils.toBean(rule, XqImageGenRuleRespVO.class);
    }

    public void saveImageRule(XqImageGenRuleSaveReqVO reqVO) {
        String platformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        String categoryId = StrUtil.blankToDefault(reqVO.getCategoryId(), "");
        XqImageGenRuleDO exists = imageGenRuleMapper.selectByPlatformAndCategory(platformId, categoryId);
        LocalDateTime now = LocalDateTime.now();
        if (exists == null) {
            XqImageGenRuleDO insert = new XqImageGenRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(platformId);
            insert.setCategoryId(categoryId);
            insert.setCategoryName(reqVO.getCategoryName());
            insert.setName(reqVO.getName());
            insert.setPromptText(reqVO.getPromptText());
            insert.setNegativePrompt(StrUtil.blankToDefault(reqVO.getNegativePrompt(), ""));
            insert.setConfigJson(StrUtil.blankToDefault(reqVO.getConfigJson(), "{}"));
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setRemark(reqVO.getRemark());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            imageGenRuleMapper.insert(insert);
            return;
        }
        XqImageGenRuleDO update = new XqImageGenRuleDO();
        update.setId(exists.getId());
        update.setCategoryName(reqVO.getCategoryName());
        update.setName(reqVO.getName());
        update.setPromptText(reqVO.getPromptText());
        update.setNegativePrompt(StrUtil.blankToDefault(reqVO.getNegativePrompt(), ""));
        update.setConfigJson(StrUtil.blankToDefault(reqVO.getConfigJson(), "{}"));
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(reqVO.getRemark());
        update.setUpdateDate(now);
        imageGenRuleMapper.updateById(update);
    }

    private static String defaultConfigJson() {
        return "{\"defaultFeatureCount\":5,\"allowedFeatureCounts\":[5,8],"
                + "\"generateTitle\":true,"
                + "\"limits\":{\"titleMaxLen\":200,\"descriptionMaxLen\":2000,\"featureMaxLen\":200}}";
    }

}
