package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldTemplateRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqFieldPoolRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqShopFieldConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqShopFieldConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCategoryRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingPlatformRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingShopRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqWorkbenchScopeRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqCopyGenRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqImageGenRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryFieldConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryFieldTemplateDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldCommonDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldPoolDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldSpecialDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingShopFieldConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqCopyGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqImageGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryFieldConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryFieldTemplateMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingFieldCommonMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingFieldPoolMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingFieldSpecialMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingShopFieldConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqStoreMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 上架目录：平台/店铺业务只认主库 xq_platform / xq_store / xq_user_store。
 * 用户身份与菜单权限走芋道 system_*；xq_user_store 仅补充「用户可操作哪些店铺」。
 * 原库分类/文案规则仍按 sourcePlatformId 对接，不套用原库店铺权限业务。
 */
@Service
@Validated
public class XqListingCatalogService {

    @Resource
    private XqListingPlatformMapper platformMapper;
    @Resource
    private XqPlatformMapper xqPlatformMapper;
    @Resource
    private XqStoreMapper xqStoreMapper;
    @Resource
    private XqListingCategoryMapper categoryMapper;
    @Resource
    private XqListingCategoryFieldTemplateMapper categoryFieldTemplateMapper;
    @Resource
    private XqListingCategoryFieldConfigMapper categoryFieldConfigMapper;
    @Resource
    private XqListingShopFieldConfigMapper shopFieldConfigMapper;
    @Resource
    private XqListingFieldPoolMapper listingFieldPoolMapper;
    @Resource
    private XqListingFieldCommonMapper listingFieldCommonMapper;
    @Resource
    private XqListingFieldSpecialMapper listingFieldSpecialMapper;
    @Resource
    private XqCopyGenRuleMapper copyGenRuleMapper;
    @Resource
    private XqImageGenRuleMapper imageGenRuleMapper;
    @Resource
    private XqStoreScopeService storeScopeService;

    /** 业务平台：xq_platform.id；有绑店白名单时只返回可见店铺所属平台 */
    public List<XqListingPlatformRespVO> listPlatforms() {
        Set<Long> allowedStoreIds = storeScopeService.getAllowedStoreIds(getLoginUserId());
        Set<Long> allowedPlatformIds = null;
        if (allowedStoreIds != null) {
            allowedPlatformIds = new HashSet<>();
            if (!allowedStoreIds.isEmpty()) {
                for (XqStoreDO store : xqStoreMapper.selectEnabledByIds(allowedStoreIds)) {
                    if (store.getPlatformId() != null) {
                        allowedPlatformIds.add(store.getPlatformId());
                    }
                }
            }
        }
        List<XqListingPlatformRespVO> out = new ArrayList<>();
        for (XqPlatformDO p : xqPlatformMapper.selectEnabledList()) {
            if (allowedPlatformIds != null && !allowedPlatformIds.contains(p.getId())) {
                continue;
            }
            XqListingPlatformRespVO vo = new XqListingPlatformRespVO();
            vo.setId(String.valueOf(p.getId()));
            vo.setCode(p.getCode());
            vo.setName(p.getName());
            vo.setSortOrder(p.getSort());
            vo.setEnabled(p.getEnabled());
            out.add(vo);
        }
        return out;
    }

    /** 业务店铺：xq_store.id；按 xq_user_store 过滤（空绑定=不限店） */
    public List<XqListingShopRespVO> listShops(String platformId) {
        Long filterPlatformId = XqStoreScopeService.parseLong(platformId);
        Set<Long> allowedStoreIds = storeScopeService.getAllowedStoreIds(getLoginUserId());
        Map<Long, XqPlatformDO> platforms = xqPlatformMapper.selectEnabledList().stream()
                .collect(Collectors.toMap(XqPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqListingShopRespVO> result = new ArrayList<>();
        for (XqStoreDO store : xqStoreMapper.selectListAll()) {
            if (store.getStatus() != null && store.getStatus() != 1) {
                continue;
            }
            if (allowedStoreIds != null && !allowedStoreIds.contains(store.getId())) {
                continue;
            }
            if (filterPlatformId != null && !filterPlatformId.equals(store.getPlatformId())) {
                continue;
            }
            XqPlatformDO platform = platforms.get(store.getPlatformId());
            XqListingShopRespVO vo = new XqListingShopRespVO();
            vo.setId(String.valueOf(store.getId()));
            vo.setPlatformId(store.getPlatformId() == null ? "" : String.valueOf(store.getPlatformId()));
            vo.setCode(StrUtil.blankToDefault(store.getName(), String.valueOf(store.getId())));
            vo.setName(store.getName());
            vo.setEnabled(true);
            vo.setStorePlatform(platform != null ? platform.getCode() : "");
            result.add(vo);
        }
        return result;
    }

    /** 当前登录人可见平台 + 绑定店铺，供侧边栏平台工作台 */
    public XqWorkbenchScopeRespVO getWorkbenchScope() {
        XqWorkbenchScopeRespVO vo = new XqWorkbenchScopeRespVO();
        for (XqListingPlatformRespVO p : listPlatforms()) {
            XqWorkbenchScopeRespVO.Platform row = new XqWorkbenchScopeRespVO.Platform();
            row.setId(p.getId());
            row.setCode(p.getCode());
            row.setName(p.getName());
            row.setSortOrder(p.getSortOrder());
            row.setShops(listShops(p.getId()));
            if (row.getShops() == null || row.getShops().isEmpty()) {
                continue;
            }
            vo.getPlatforms().add(row);
        }
        return vo;
    }

    /** 分类树：入参业务平台 id，内部用 sourcePlatformId 查原库类目 */
    public List<XqListingCategoryRespVO> listCategoryTree(String platformId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        if (StrUtil.isBlank(sourcePlatformId)) {
            return List.of();
        }
        List<XqListingCategoryDO> rows = categoryMapper.selectEnabledByPlatformId(sourcePlatformId);
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

    /**
     * 源库类目字段模板（t_giga_listing_category_field_template），不是平台文案规则。
     * 当前类目没有则沿父级往上找。不要加事务，以免绑死主库 SqlSession。
     */
    public XqCategoryFieldTemplateRespVO getCategoryFieldTemplate(String categoryId) {
        if (StrUtil.isBlank(categoryId)) {
            return emptyCategoryTemplate(categoryId);
        }
        XqListingCategoryFieldTemplateDO row = null;
        String cid = categoryId.trim();
        String hitCategoryName = "";
        for (int i = 0; i < 16 && StrUtil.isNotBlank(cid); i++) {
            row = categoryFieldTemplateMapper.selectByCategoryId(cid);
            if (row != null && StrUtil.isNotBlank(row.getTemplateJson())) {
                XqListingCategoryDO cat = categoryMapper.selectById(cid);
                hitCategoryName = cat != null ? StrUtil.blankToDefault(cat.getName(), "") : "";
                break;
            }
            row = null;
            XqListingCategoryDO cat = categoryMapper.selectById(cid);
            if (cat == null) {
                break;
            }
            if (i == 0) {
                hitCategoryName = StrUtil.blankToDefault(cat.getName(), "");
            }
            String parentId = StrUtil.blankToDefault(cat.getParentId(), "");
            if (StrUtil.isBlank(parentId) || "0".equals(parentId) || parentId.equals(cid)) {
                break;
            }
            cid = parentId;
        }
        XqCategoryFieldTemplateRespVO vo;
        if (row == null) {
            vo = emptyCategoryTemplate(categoryId);
            vo.setName(hitCategoryName);
        } else {
            vo = toCategoryFieldTemplateVo(row, hitCategoryName);
            vo.setCategoryId(categoryId);
        }
        applyFieldConfig(vo, categoryId);
        return vo;
    }

    private static XqCategoryFieldTemplateRespVO emptyCategoryTemplate(String categoryId) {
        XqCategoryFieldTemplateRespVO vo = new XqCategoryFieldTemplateRespVO();
        vo.setCategoryId(StrUtil.blankToDefault(categoryId, ""));
        vo.setTotal(0);
        vo.setRequiredCount(0);
        vo.setRecommendedCount(0);
        vo.setOptionalCount(0);
        return vo;
    }

    private static XqCategoryFieldTemplateRespVO toCategoryFieldTemplateVo(
            XqListingCategoryFieldTemplateDO row, String categoryName) {
        XqCategoryFieldTemplateRespVO vo = new XqCategoryFieldTemplateRespVO();
        vo.setId(row.getId());
        vo.setCategoryId(row.getCategoryId());
        vo.setPlatformId(row.getPlatformId());
        vo.setHierarchyCode(row.getHierarchyCode());
        vo.setName(StrUtil.blankToDefault(categoryName, row.getHierarchyCode()) + " · 字段模板");
        JSONArray fieldsArr = null;
        try {
            JSONObject payload = JSONUtil.parseObj(row.getTemplateJson());
            fieldsArr = payload.getJSONArray("fields");
            if (StrUtil.isBlank(vo.getHierarchyCode())) {
                vo.setHierarchyCode(payload.getStr("hierarchyCode", ""));
            }
        } catch (Exception ignored) {
            fieldsArr = null;
        }
        int required = 0;
        int recommended = 0;
        int optional = 0;
        if (fieldsArr != null) {
            for (Object item : fieldsArr) {
                JSONObject field;
                try {
                    field = JSONUtil.parseObj(item);
                } catch (Exception ignored) {
                    continue;
                }
                String level = StrUtil.blankToDefault(field.getStr("requirementLevel"), "").toUpperCase();
                if ("DISABLED".equals(level)) {
                    continue;
                }
                String code = StrUtil.blankToDefault(field.getStr("code"), "").trim();
                if (StrUtil.isBlank(code)) {
                    continue;
                }
                boolean req = Boolean.TRUE.equals(field.getBool("required"))
                        || "REQUIRED".equals(level)
                        || "MANDATORY".equals(level);
                String group = req ? "必填" : ("RECOMMENDED".equals(level) ? "推荐" : "选填");
                if (req) {
                    required++;
                } else if ("RECOMMENDED".equals(level)) {
                    recommended++;
                } else {
                    optional++;
                }
                String source = StrUtil.blankToDefault(field.getStr("source"), "");
                XqCategoryFieldTemplateRespVO.Field f = new XqCategoryFieldTemplateRespVO.Field();
                f.setCode(code);
                f.setLabel(StrUtil.blankToDefault(field.getStr("label"), code));
                f.setType(StrUtil.blankToDefault(field.getStr("type"), ""));
                f.setRequirementLevel(level);
                f.setRequired(req);
                f.setSource(source);
                f.setGroupLabel(group);
                f.setBandLabel("special".equalsIgnoreCase(source) ? "特殊" : "固定");
                f.setValuesList(StrUtil.blankToDefault(field.getStr("valuesList"), ""));
                vo.getFields().add(f);
            }
        }
        vo.setTotal(vo.getFields().size());
        vo.setRequiredCount(required);
        vo.setRecommendedCount(recommended);
        vo.setOptionalCount(optional);
        return vo;
    }

    public List<XqCopyGenRuleRespVO> listCopyRules() {
        // 规则配置页：平台展示以 xq_platform 为准；规则行仍按 sourcePlatformId 存原库
        List<XqPlatformDO> xqPlatforms = xqPlatformMapper.selectEnabledList();
        Map<String, XqPlatformDO> bySource = xqPlatforms.stream()
                .filter(p -> StrUtil.isNotBlank(p.getSourcePlatformId()))
                .collect(Collectors.toMap(XqPlatformDO::getSourcePlatformId, p -> p, (a, b) -> a));
        List<XqCopyGenRuleDO> rules = copyGenRuleMapper.selectAll();
        List<XqCopyGenRuleRespVO> result = new ArrayList<>();
        Set<String> coveredSources = new HashSet<>();
        for (XqCopyGenRuleDO rule : rules) {
            XqCopyGenRuleRespVO vo = BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
            XqPlatformDO p = bySource.get(StrUtil.blankToDefault(rule.getPlatformId(), ""));
            if (p != null) {
                vo.setPlatformId(String.valueOf(p.getId()));
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
                coveredSources.add(p.getSourcePlatformId());
            } else if (StrUtil.isBlank(rule.getPlatformId())) {
                vo.setPlatformCode("global");
                vo.setPlatformName("通用规则");
            } else {
                // 尚未挂到 xq_platform 的历史规则：补名称
                XqListingPlatformDO legacy = platformMapper.selectById(rule.getPlatformId());
                if (legacy != null) {
                    vo.setPlatformCode(legacy.getCode());
                    vo.setPlatformName(legacy.getName());
                }
            }
            result.add(vo);
        }
        for (XqPlatformDO p : xqPlatforms) {
            if (StrUtil.isBlank(p.getSourcePlatformId()) || coveredSources.contains(p.getSourcePlatformId())) {
                continue;
            }
            XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
            vo.setPlatformId(String.valueOf(p.getId()));
            vo.setPlatformCode(p.getCode());
            vo.setPlatformName(p.getName());
            vo.setCode("deep");
            vo.setName(p.getName() + "文案规则");
            vo.setEnabled(true);
            vo.setConfigJson(defaultConfigJson());
            result.add(vo);
        }
        return result;
    }

    /**
     * 切勿在本方法上加 @Transactional：NOT_SUPPORTED 仍会打开事务同步，
     * 前面 resolveSourcePlatformId 打主库后 SqlSession 绑死 master，
     * Mapper 上的 @DS("xq") 无法切到源库 t_giga_copy_gen_rule。
     */
    public XqCopyGenRuleRespVO getCopyRule(String platformId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        XqCopyGenRuleDO rule = onXqDs(() -> copyGenRuleMapper.selectByPlatformId(sourcePlatformId));
        if (rule == null) {
            XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
            vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
            vo.setCode("deep");
            vo.setName("文案生成规则");
            vo.setEnabled(true);
            vo.setConfigJson(defaultConfigJson());
            return vo;
        }
        XqCopyGenRuleRespVO vo = BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
        vo.setPlatformId(StrUtil.blankToDefault(platformId, sourcePlatformId));
        return vo;
    }

    public void saveCopyRule(XqCopyGenRuleSaveReqVO reqVO) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(reqVO.getPlatformId());
        if (StrUtil.isBlank(sourcePlatformId)) {
            sourcePlatformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        }
        XqCopyGenRuleDO exists = copyGenRuleMapper.selectByPlatformId(sourcePlatformId);
        LocalDateTime now = LocalDateTime.now();
        if (exists == null) {
            XqCopyGenRuleDO insert = new XqCopyGenRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(sourcePlatformId);
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
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        List<XqImageGenRuleDO> rows = imageGenRuleMapper.selectByPlatformId(sourcePlatformId);
        Map<String, XqPlatformDO> bySource = xqPlatformMapper.selectEnabledList().stream()
                .filter(p -> StrUtil.isNotBlank(p.getSourcePlatformId()))
                .collect(Collectors.toMap(XqPlatformDO::getSourcePlatformId, p -> p, (a, b) -> a));
        List<XqImageGenRuleRespVO> result = new ArrayList<>();
        for (XqImageGenRuleDO row : rows) {
            XqImageGenRuleRespVO vo = BeanUtils.toBean(row, XqImageGenRuleRespVO.class);
            XqPlatformDO p = bySource.get(StrUtil.blankToDefault(row.getPlatformId(), ""));
            if (p != null) {
                vo.setPlatformId(String.valueOf(p.getId()));
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
            }
            result.add(vo);
        }
        return result;
    }

    /** 同 getCopyRule：不要加事务，否则图片规则也会打到主库 */
    public XqImageGenRuleRespVO getImageRule(String platformId, String categoryId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        String cid = StrUtil.blankToDefault(categoryId, "");
        XqImageGenRuleDO rule = onXqDs(() -> imageGenRuleMapper.selectByPlatformAndCategory(sourcePlatformId, cid));
        if (rule == null) {
            XqImageGenRuleRespVO vo = new XqImageGenRuleRespVO();
            vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
            vo.setCategoryId(cid);
            vo.setName(StrUtil.isBlank(cid) ? "平台默认图片提示词" : "分类图片提示词");
            vo.setPromptText("");
            vo.setNegativePrompt("");
            vo.setEnabled(true);
            vo.setConfigJson("{}");
            return vo;
        }
        XqImageGenRuleRespVO vo = BeanUtils.toBean(rule, XqImageGenRuleRespVO.class);
        vo.setPlatformId(StrUtil.blankToDefault(platformId, sourcePlatformId));
        return vo;
    }

    /** 不要加事务，否则 @DS("xq") 会打到主库 */
    public List<XqCategoryFieldConfigRespVO> listCategoryFieldConfigs(String platformId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        List<XqListingCategoryFieldConfigDO> rows =
                categoryFieldConfigMapper.selectByPlatformId(sourcePlatformId);
        List<XqCategoryFieldConfigRespVO> result = new ArrayList<>();
        for (XqListingCategoryFieldConfigDO row : rows) {
            result.add(toFieldConfigVo(row, platformId));
        }
        return result;
    }

    public XqCategoryFieldConfigRespVO getCategoryFieldConfig(String platformId, String categoryId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        String cid = StrUtil.blankToDefault(categoryId, "");
        XqListingCategoryFieldConfigDO row =
                categoryFieldConfigMapper.selectByPlatformAndCategory(sourcePlatformId, cid);
        if (row == null) {
            XqCategoryFieldConfigRespVO vo = new XqCategoryFieldConfigRespVO();
            vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
            vo.setCategoryId(cid);
            return vo;
        }
        return toFieldConfigVo(row, platformId);
    }

    public void saveCategoryFieldConfig(XqCategoryFieldConfigSaveReqVO reqVO) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(reqVO.getPlatformId());
        if (StrUtil.isBlank(sourcePlatformId)) {
            sourcePlatformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        }
        String categoryId = StrUtil.blankToDefault(reqVO.getCategoryId(), "");
        List<XqCategoryFieldConfigSaveReqVO.Item> items =
                reqVO.getFields() == null ? List.of() : reqVO.getFields();
        JSONArray arr = new JSONArray();
        for (XqCategoryFieldConfigSaveReqVO.Item item : items) {
            if (item == null || StrUtil.isBlank(item.getCode())) {
                continue;
            }
            String def = StrUtil.blankToDefault(item.getDefaultValue(), "");
            String src = StrUtil.blankToDefault(item.getValueSource(), "");
            String zone = normalizeFieldZone(item.getZone());
            if (StrUtil.isBlank(def) && StrUtil.isBlank(src) && StrUtil.isBlank(zone)) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.set("code", item.getCode().trim());
            obj.set("defaultValue", def);
            obj.set("valueSource", src);
            obj.set("zone", zone);
            arr.add(obj);
        }
        LocalDateTime now = LocalDateTime.now();
        XqListingCategoryFieldConfigDO exists =
                categoryFieldConfigMapper.selectByPlatformAndCategory(sourcePlatformId, categoryId);
        if (exists == null) {
            XqListingCategoryFieldConfigDO insert = new XqListingCategoryFieldConfigDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(sourcePlatformId);
            insert.setCategoryId(categoryId);
            insert.setFieldsJson(arr.toString());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            categoryFieldConfigMapper.insert(insert);
            return;
        }
        XqListingCategoryFieldConfigDO update = new XqListingCategoryFieldConfigDO();
        update.setId(exists.getId());
        update.setFieldsJson(arr.toString());
        update.setUpdateDate(now);
        categoryFieldConfigMapper.updateById(update);
    }

    public XqFieldPoolRespVO getFieldPool(String platformId, String shopId, String country) {
        XqFieldPoolRespVO out = new XqFieldPoolRespVO();
        out.setPlatformId(StrUtil.blankToDefault(platformId, ""));
        Set<String> templatePlatformIds = resolveTemplatePlatformIds(platformId);
        Map<String, XqCategoryFieldTemplateRespVO.Field> byCode = new java.util.LinkedHashMap<>();
        Set<String> categoryIds = new HashSet<>();
        String loadedFrom = "";
        if (!templatePlatformIds.isEmpty()) {
            List<XqListingFieldPoolDO> poolRows =
                    onXqDs(() -> listingFieldPoolMapper.selectByPlatformIds(templatePlatformIds));
            for (XqListingFieldPoolDO row : poolRows) {
                putPoolField(byCode, toFieldFromPool(row));
            }
            if (!byCode.isEmpty()) {
                loadedFrom = "pool";
            }
        }
        if (byCode.isEmpty() && !templatePlatformIds.isEmpty()) {
            List<XqListingFieldCommonDO> commons =
                    onXqDs(() -> listingFieldCommonMapper.selectByPlatformIds(templatePlatformIds));
            for (XqListingFieldCommonDO row : commons) {
                putPoolField(byCode, toFieldFromSplit(
                        row.getFieldCode(), row.getLabel(), row.getFieldType(),
                        row.getRequired(), row.getRequirementLevel(),
                        row.getValuesListCode(), row.getFieldJson(), "common"));
            }
            List<XqListingFieldSpecialDO> specials =
                    onXqDs(() -> listingFieldSpecialMapper.selectByPlatformIds(templatePlatformIds));
            for (XqListingFieldSpecialDO row : specials) {
                putPoolField(byCode, toFieldFromSplit(
                        row.getFieldCode(), row.getLabel(), row.getFieldType(),
                        row.getRequired(), row.getRequirementLevel(),
                        row.getValuesListCode(), row.getFieldJson(), "special"));
            }
            if (!byCode.isEmpty()) {
                loadedFrom = "split";
            }
        }
        List<XqListingCategoryFieldTemplateDO> templates = List.of();
        if (byCode.isEmpty() && !templatePlatformIds.isEmpty()) {
            templates = onXqDs(() -> categoryFieldTemplateMapper.selectByPlatformIds(templatePlatformIds));
        }
        for (XqListingCategoryFieldTemplateDO row : templates) {
            if (row == null || StrUtil.isBlank(row.getTemplateJson())) {
                continue;
            }
            categoryIds.add(StrUtil.blankToDefault(row.getCategoryId(), ""));
            XqCategoryFieldTemplateRespVO parsed = toCategoryFieldTemplateVo(row, "");
            row.setTemplateJson(null);
            for (XqCategoryFieldTemplateRespVO.Field field : parsed.getFields()) {
                putPoolField(byCode, field);
            }
        }
        List<XqCategoryFieldTemplateRespVO.Field> fields;
        if (byCode.isEmpty()) {
            if (isAmazonLike(platformId)) {
                fields = amazonDefaultFields();
                out.setSourceHint("amazon-default");
            } else {
                fields = new ArrayList<>();
                out.setSourceHint("catalog-empty");
            }
            out.setCategoryCount(0);
        } else {
            fields = new ArrayList<>(byCode.values());
            out.setSourceHint(StrUtil.blankToDefault(loadedFrom, "catalog"));
            out.setCategoryCount(categoryIds.size());
        }
        int required = 0;
        for (XqCategoryFieldTemplateRespVO.Field field : fields) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                required++;
            }
        }
        out.setRequiredCount(required);
        out.setTotal(fields.size());
        XqShopFieldConfigRespVO cfg = getShopFieldConfig(platformId, shopId, country);
        Map<String, XqShopFieldConfigRespVO.Item> overlay = new HashMap<>();
        if (cfg.getFields() != null) {
            for (XqShopFieldConfigRespVO.Item item : cfg.getFields()) {
                if (item == null || StrUtil.isBlank(item.getCode())) {
                    continue;
                }
                String key = normalizePoolFieldCode(item.getCode());
                // 多实例字段配置合并到基础编码；后写覆盖前写
                overlay.put(key, item);
            }
        }
        boolean anySavedZone = overlay.values().stream().anyMatch(i -> StrUtil.isNotBlank(i.getZone()));
        for (XqCategoryFieldTemplateRespVO.Field field : fields) {
            XqShopFieldConfigRespVO.Item item = overlay.get(normalizePoolFieldCode(field.getCode()));
            if (item != null) {
                field.setDefaultValue(item.getDefaultValue());
                field.setValueSource(item.getValueSource());
                field.setZone(normalizeFieldZone(item.getZone()));
                if (item.getRequired() != null) {
                    field.setRequired(item.getRequired());
                    field.setGroupLabel(Boolean.TRUE.equals(item.getRequired()) ? "必填" : "选填");
                    field.setRequirementLevel(
                            Boolean.TRUE.equals(item.getRequired()) ? "REQUIRED" : "OPTIONAL");
                }
            }
            if (StrUtil.isBlank(field.getZone()) && !anySavedZone) {
                field.setZone(guessFieldZone(field));
            }
        }
        out.setFields(fields);
        out.setPartitions(ensureSystemPartitions(cfg.getPartitions()));
        return out;
    }

    private Set<String> resolveTemplatePlatformIds(String platformId) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        String source = storeScopeService.resolveSourcePlatformId(platformId);
        if (StrUtil.isNotBlank(source)) {
            ids.add(source);
        }
        Long xqId = XqStoreScopeService.parseLong(platformId);
        XqPlatformDO biz = xqId == null ? null : xqPlatformMapper.selectById(xqId);
        if (biz != null) {
            if (StrUtil.isNotBlank(biz.getSourcePlatformId())) {
                ids.add(biz.getSourcePlatformId());
            }
            String code = StrUtil.blankToDefault(biz.getCode(), "").trim();
            if (StrUtil.isNotBlank(code)) {
                XqListingPlatformDO listing =
                        onXqDs(() -> platformMapper.selectByCode(code));
                if (listing != null && StrUtil.isNotBlank(listing.getId())) {
                    ids.add(listing.getId());
                }
            }
        }
        return ids;
    }

    /**
     * 亚马逊多实例字段（bullet_point_2、material_5）合并为基础编码，避免同名重复出现。
     */
    private static String normalizePoolFieldCode(String raw) {
        String code = StrUtil.blankToDefault(raw, "").trim();
        if (code.isEmpty()) {
            return "";
        }
        return code.replaceAll("_(?:[1-9]\\d*)$", "");
    }

    private static void putPoolField(
            Map<String, XqCategoryFieldTemplateRespVO.Field> byCode,
            XqCategoryFieldTemplateRespVO.Field field) {
        if (field == null || StrUtil.isBlank(field.getCode())) {
            return;
        }
        String code = normalizePoolFieldCode(field.getCode());
        if (StrUtil.isBlank(code)) {
            return;
        }
        field.setCode(code);
        XqCategoryFieldTemplateRespVO.Field exist = byCode.get(code);
        if (exist == null) {
            if (field.getSourceCount() == null) {
                field.setSourceCount(1);
            }
            byCode.put(code, field);
            return;
        }
        int add = field.getSourceCount() == null ? 1 : field.getSourceCount();
        exist.setSourceCount((exist.getSourceCount() == null ? 1 : exist.getSourceCount()) + add);
        if (Boolean.TRUE.equals(field.getRequired()) && !Boolean.TRUE.equals(exist.getRequired())) {
            exist.setRequired(true);
            exist.setGroupLabel("必填");
            exist.setRequirementLevel(field.getRequirementLevel());
        }
        if (StrUtil.isBlank(exist.getLabel()) && StrUtil.isNotBlank(field.getLabel())) {
            exist.setLabel(field.getLabel());
        }
    }

    private static XqCategoryFieldTemplateRespVO.Field toFieldFromPool(XqListingFieldPoolDO row) {
        return toFieldFromSplit(
                row.getFieldCode(), row.getLabel(), row.getFieldType(),
                row.getRequired(), row.getRequirementLevel(),
                row.getValuesListCode(), row.getFieldJson(), row.getSource());
    }

    private static XqCategoryFieldTemplateRespVO.Field toFieldFromSplit(
            String code, String label, String type, Boolean required, String requirementLevel,
            String valuesList, String fieldJson, String source) {
        XqCategoryFieldTemplateRespVO.Field f = new XqCategoryFieldTemplateRespVO.Field();
        JSONObject json = null;
        if (StrUtil.isNotBlank(fieldJson)) {
            try {
                json = JSONUtil.parseObj(fieldJson);
            } catch (Exception ignored) {
                json = null;
            }
        }
        f.setCode(StrUtil.blankToDefault(code, json == null ? "" : json.getStr("code")));
        f.setLabel(StrUtil.blankToDefault(label, json == null ? f.getCode() : json.getStr("label", f.getCode())));
        f.setType(StrUtil.blankToDefault(type, json == null ? "" : json.getStr("type")));
        boolean req = Boolean.TRUE.equals(required)
                || (json != null && Boolean.TRUE.equals(json.getBool("required")));
        String level = StrUtil.blankToDefault(requirementLevel,
                json == null ? "" : json.getStr("requirementLevel"));
        f.setRequired(req);
        f.setRequirementLevel(level);
        f.setGroupLabel(req ? "必填" : ("RECOMMENDED".equalsIgnoreCase(level) ? "推荐" : "选填"));
        f.setValuesList(StrUtil.blankToDefault(valuesList,
                json == null ? "" : json.getStr("valuesList")));
        f.setSource(StrUtil.blankToDefault(source, json == null ? "" : json.getStr("source")));
        int sourceCount = 1;
        if (json != null && json.getJSONArray("categoryIds") != null) {
            sourceCount = Math.max(1, json.getJSONArray("categoryIds").size());
        }
        f.setSourceCount(sourceCount);
        return f;
    }

    private boolean isAmazonLike(String platformId) {
        Long xqId = XqStoreScopeService.parseLong(platformId);
        XqPlatformDO biz = xqId == null ? null : xqPlatformMapper.selectById(xqId);
        String code = biz == null ? "" : StrUtil.blankToDefault(biz.getCode(), "");
        String name = biz == null ? "" : StrUtil.blankToDefault(biz.getName(), "");
        return "amz".equalsIgnoreCase(code)
                || "amazon".equalsIgnoreCase(code)
                || name.toLowerCase().contains("amz")
                || name.toLowerCase().contains("amazon");
    }

    private static List<XqCategoryFieldTemplateRespVO.Field> amazonDefaultFields() {
        List<XqCategoryFieldTemplateRespVO.Field> list = new ArrayList<>();
        addPoolField(list, "item_name", "标题", "STRING", true);
        addPoolField(list, "brand", "品牌", "STRING", true);
        addPoolField(list, "product_type", "产品类型", "STRING", true);
        addPoolField(list, "product_description", "商品描述", "LONG_TEXT", true);
        addPoolField(list, "bullet_point", "卖点", "STRING", true);
        addPoolField(list, "generic_keyword", "搜索关键词", "STRING", false);
        addPoolField(list, "manufacturer", "制造商", "STRING", false);
        addPoolField(list, "model_number", "型号", "STRING", false);
        addPoolField(list, "part_number", "零件号", "STRING", false);
        addPoolField(list, "external_product_id", "UPC/EAN", "STRING", true);
        addPoolField(list, "standard_price", "售价", "DECIMAL", true);
        addPoolField(list, "list_price", "划线价", "DECIMAL", false);
        addPoolField(list, "quantity", "库存", "INTEGER", true);
        addPoolField(list, "condition_type", "成色", "STRING", true);
        addPoolField(list, "merchant_suggested_asin", "建议 ASIN", "STRING", false);
        addPoolField(list, "parentage_level", "父子关系", "STRING", false);
        addPoolField(list, "parent_sku", "父 SKU", "STRING", false);
        addPoolField(list, "variation_theme", "变体主题", "STRING", false);
        addPoolField(list, "color", "颜色", "STRING", false);
        addPoolField(list, "size", "尺寸", "STRING", false);
        addPoolField(list, "style", "款式", "STRING", false);
        addPoolField(list, "material", "材质", "STRING", false);
        addPoolField(list, "country_of_origin", "原产国", "STRING", false);
        addPoolField(list, "item_length", "物品长", "DECIMAL", false);
        addPoolField(list, "item_width", "物品宽", "DECIMAL", false);
        addPoolField(list, "item_height", "物品高", "DECIMAL", false);
        addPoolField(list, "item_weight", "物品重量", "DECIMAL", false);
        addPoolField(list, "package_length", "包装长", "DECIMAL", false);
        addPoolField(list, "package_width", "包装宽", "DECIMAL", false);
        addPoolField(list, "package_height", "包装高", "DECIMAL", false);
        addPoolField(list, "package_weight", "包装重量", "DECIMAL", false);
        addPoolField(list, "main_image_url", "主图", "MEDIA", true);
        addPoolField(list, "other_image_url", "附图", "MEDIA", false);
        addPoolField(list, "swatch_image_url", "色卡图", "MEDIA", false);
        return list;
    }

    private static void addPoolField(
            List<XqCategoryFieldTemplateRespVO.Field> list,
            String code, String label, String type, boolean required) {
        XqCategoryFieldTemplateRespVO.Field f = new XqCategoryFieldTemplateRespVO.Field();
        f.setCode(code);
        f.setLabel(label);
        f.setType(type);
        f.setRequired(required);
        f.setRequirementLevel(required ? "REQUIRED" : "OPTIONAL");
        f.setGroupLabel(required ? "必填" : "选填");
        f.setSourceCount(1);
        list.add(f);
    }

    public XqShopFieldConfigRespVO getShopFieldConfig(String platformId, String shopId, String country) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        String sid = StrUtil.blankToDefault(shopId, "");
        String cc = StrUtil.blankToDefault(country, "").toUpperCase();
        XqShopFieldConfigRespVO vo = new XqShopFieldConfigRespVO();
        vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
        vo.setShopId(sid);
        vo.setCountry(cc);
        vo.setPartitions(ensureSystemPartitions(null));
        if (StrUtil.isBlank(sourcePlatformId) || StrUtil.isBlank(sid) || StrUtil.isBlank(cc)) {
            return vo;
        }
        XqListingShopFieldConfigDO row =
                onXqDs(() -> shopFieldConfigMapper.selectByScope(sourcePlatformId, sid, cc));
        if (row == null) {
            return vo;
        }
        vo.setId(row.getId());
        vo.setFields(new ArrayList<>(parseShopFieldItems(row.getFieldsJson()).values()));
        vo.setPartitions(ensureSystemPartitions(parsePartitions(row.getPartitionsJson())));
        return vo;
    }

    public void saveShopFieldConfig(XqShopFieldConfigSaveReqVO reqVO) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(reqVO.getPlatformId());
        if (StrUtil.isBlank(sourcePlatformId)) {
            sourcePlatformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        }
        String shopId = StrUtil.blankToDefault(reqVO.getShopId(), "");
        String country = StrUtil.blankToDefault(reqVO.getCountry(), "").toUpperCase();
        JSONArray fieldArr = new JSONArray();
        List<XqShopFieldConfigSaveReqVO.Item> items =
                reqVO.getFields() == null ? List.of() : reqVO.getFields();
        for (XqShopFieldConfigSaveReqVO.Item item : items) {
            if (item == null || StrUtil.isBlank(item.getCode())) {
                continue;
            }
            String def = StrUtil.blankToDefault(item.getDefaultValue(), "");
            String src = StrUtil.blankToDefault(item.getValueSource(), "");
            String zone = normalizeFieldZone(item.getZone());
            Boolean required = item.getRequired();
            if (StrUtil.isBlank(def) && StrUtil.isBlank(src) && StrUtil.isBlank(zone) && required == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.set("code", item.getCode().trim());
            obj.set("defaultValue", def);
            obj.set("valueSource", src);
            obj.set("zone", zone);
            if (required != null) {
                obj.set("required", required);
            }
            fieldArr.add(obj);
        }
        JSONArray partArr = new JSONArray();
        for (XqShopFieldConfigRespVO.Partition p : ensureSystemPartitions(reqVO.getPartitions())) {
            JSONObject obj = new JSONObject();
            obj.set("id", p.getId());
            obj.set("name", p.getName());
            obj.set("keywords", StrUtil.blankToDefault(p.getKeywords(), ""));
            obj.set("system", Boolean.TRUE.equals(p.getSystem()));
            obj.set("color", StrUtil.blankToDefault(p.getColor(), ""));
            partArr.add(obj);
        }
        LocalDateTime now = LocalDateTime.now();
        String srcPid = sourcePlatformId;
        XqListingShopFieldConfigDO exists =
                onXqDs(() -> shopFieldConfigMapper.selectByScope(srcPid, shopId, country));
        if (exists == null) {
            XqListingShopFieldConfigDO insert = new XqListingShopFieldConfigDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(srcPid);
            insert.setShopId(shopId);
            insert.setCountry(country);
            insert.setFieldsJson(fieldArr.toString());
            insert.setPartitionsJson(partArr.toString());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            onXqDs(() -> {
                shopFieldConfigMapper.insert(insert);
                return true;
            });
            return;
        }
        XqListingShopFieldConfigDO update = new XqListingShopFieldConfigDO();
        update.setId(exists.getId());
        update.setFieldsJson(fieldArr.toString());
        update.setPartitionsJson(partArr.toString());
        update.setUpdateDate(now);
        onXqDs(() -> {
            shopFieldConfigMapper.updateById(update);
            return true;
        });
    }

    private static List<XqShopFieldConfigRespVO.Partition> ensureSystemPartitions(
            List<XqShopFieldConfigRespVO.Partition> raw) {
        Map<String, XqShopFieldConfigRespVO.Partition> map = new java.util.LinkedHashMap<>();
        map.put("copy", systemPartition("copy", "描述", "title,description,bullet,卖点,长描述", "#2563eb"));
        map.put("product", systemPartition("product", "产品", "sku,upc,gtin,inventory,库存,color", "#059669"));
        map.put("attr", systemPartition("attr", "属性", "length,width,height,weight,brand,material,尺寸", "#d97706"));
        if (raw != null) {
            for (XqShopFieldConfigRespVO.Partition p : raw) {
                if (p == null || StrUtil.isBlank(p.getId())) {
                    continue;
                }
                String id = normalizeFieldZone(p.getId());
                if (StrUtil.isBlank(id)) {
                    continue;
                }
                if (map.containsKey(id) && Boolean.TRUE.equals(map.get(id).getSystem())) {
                    XqShopFieldConfigRespVO.Partition sys = map.get(id);
                    if (StrUtil.isNotBlank(p.getKeywords())) {
                        sys.setKeywords(p.getKeywords());
                    }
                    continue;
                }
                if (Boolean.TRUE.equals(p.getSystem())) {
                    continue;
                }
                p.setId(id);
                p.setSystem(false);
                map.put(id, p);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static XqShopFieldConfigRespVO.Partition systemPartition(
            String id, String name, String keywords, String color) {
        XqShopFieldConfigRespVO.Partition p = new XqShopFieldConfigRespVO.Partition();
        p.setId(id);
        p.setName(name);
        p.setKeywords(keywords);
        p.setSystem(true);
        p.setColor(color);
        return p;
    }

    private static Map<String, XqShopFieldConfigRespVO.Item> parseShopFieldItems(String json) {
        Map<String, XqShopFieldConfigRespVO.Item> map = new HashMap<>();
        if (StrUtil.isBlank(json)) {
            return map;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object raw : arr) {
                JSONObject obj = JSONUtil.parseObj(raw);
                String code = StrUtil.blankToDefault(obj.getStr("code"), "").trim();
                if (StrUtil.isBlank(code)) {
                    continue;
                }
                XqShopFieldConfigRespVO.Item item = new XqShopFieldConfigRespVO.Item();
                item.setCode(code);
                item.setDefaultValue(obj.getStr("defaultValue"));
                item.setValueSource(obj.getStr("valueSource"));
                item.setZone(normalizeFieldZone(obj.getStr("zone")));
                if (obj.containsKey("required")) {
                    item.setRequired(obj.getBool("required"));
                }
                map.put(code, item);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return map;
    }

    private static List<XqShopFieldConfigRespVO.Partition> parsePartitions(String json) {
        List<XqShopFieldConfigRespVO.Partition> out = new ArrayList<>();
        if (StrUtil.isBlank(json)) {
            return out;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object raw : arr) {
                JSONObject obj = JSONUtil.parseObj(raw);
                XqShopFieldConfigRespVO.Partition p = new XqShopFieldConfigRespVO.Partition();
                p.setId(obj.getStr("id"));
                p.setName(obj.getStr("name"));
                p.setKeywords(obj.getStr("keywords"));
                p.setSystem(obj.getBool("system"));
                p.setColor(obj.getStr("color"));
                out.add(p);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return out;
    }

    private void applyFieldConfig(XqCategoryFieldTemplateRespVO vo, String categoryId) {
        if (vo == null || vo.getFields() == null || vo.getFields().isEmpty()
                || StrUtil.isBlank(categoryId)) {
            return;
        }
        XqListingCategoryFieldConfigDO row = categoryFieldConfigMapper.selectByCategoryId(categoryId);
        Map<String, XqCategoryFieldConfigRespVO.Item> map = parseFieldConfigItems(row);
        for (XqCategoryFieldTemplateRespVO.Field field : vo.getFields()) {
            XqCategoryFieldConfigRespVO.Item item = map.get(StrUtil.blankToDefault(field.getCode(), ""));
            if (item != null) {
                field.setDefaultValue(item.getDefaultValue());
                field.setValueSource(item.getValueSource());
                field.setZone(normalizeFieldZone(item.getZone()));
            }
        }
        boolean anyZone = vo.getFields().stream().anyMatch(f -> StrUtil.isNotBlank(f.getZone()));
        if (!anyZone) {
            for (XqCategoryFieldTemplateRespVO.Field field : vo.getFields()) {
                field.setZone(guessFieldZone(field));
            }
        }
    }

    private static String normalizeFieldZone(String raw) {
        String zone = StrUtil.blankToDefault(raw, "").trim();
        if (StrUtil.isBlank(zone)) {
            return "";
        }
        String lower = zone.toLowerCase();
        if ("copy".equals(lower) || "product".equals(lower) || "attr".equals(lower)) {
            return lower;
        }
        if (lower.startsWith("u_") && lower.matches("u_[a-z0-9_]{1,40}")) {
            return lower;
        }
        return "";
    }

    private static String guessFieldZone(XqCategoryFieldTemplateRespVO.Field field) {
        String hay = (StrUtil.blankToDefault(field.getLabel(), "") + " "
                + StrUtil.blankToDefault(field.getCode(), "") + " "
                + StrUtil.blankToDefault(field.getType(), ""))
                .toLowerCase()
                .replace('_', ' ')
                .replace('-', ' ');
        String type = StrUtil.blankToDefault(field.getType(), "").toUpperCase();
        if (type.contains("MEDIA") || type.contains("IMAGE")
                || hay.matches(".*\\b(image|photo|img|主图|附图|zoom|swatch)\\b.*")) {
            return "";
        }
        if (hay.matches(".*(product title|\\btitle\\b|product_title|long description|product description|\\bdescription\\b|商品描述|长描述|style description|selling point|highlight|bullet|卖点|product feature|feature\\s*\\d).*")) {
            return "copy";
        }
        if (hay.matches(".*(seller sku|shop sku|vendor style|item code|platform sku|\\bsku\\b|\\bupc\\b|\\bgtin\\b|variation group|\\bgroup\\b|parent sku|qty|inventory|stock|库存|display color|color family|\\bcolor\\b).*")) {
            return "product";
        }
        if (hay.matches(".*(pack(age|aging)?\\s*(length|width|height|weight)|item\\s*(length|width|height|weight)|overall\\s*(length|width|height)|dimension|包装|长宽高|\\blength\\b|\\bwidth\\b|\\bheight\\b|\\bweight\\b|\\bbrand\\b|品牌|material|材质).*")
                && !hay.matches(".*(font size|file size|battery|screen).*")) {
            return "attr";
        }
        return "";
    }

    private static XqCategoryFieldConfigRespVO toFieldConfigVo(
            XqListingCategoryFieldConfigDO row, String bizPlatformId) {
        XqCategoryFieldConfigRespVO vo = new XqCategoryFieldConfigRespVO();
        vo.setId(row.getId());
        vo.setPlatformId(StrUtil.blankToDefault(bizPlatformId, row.getPlatformId()));
        vo.setCategoryId(row.getCategoryId());
        vo.setFields(new ArrayList<>(parseFieldConfigItems(row).values()));
        return vo;
    }

    private static Map<String, XqCategoryFieldConfigRespVO.Item> parseFieldConfigItems(
            XqListingCategoryFieldConfigDO row) {
        Map<String, XqCategoryFieldConfigRespVO.Item> map = new HashMap<>();
        if (row == null || StrUtil.isBlank(row.getFieldsJson())) {
            return map;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(row.getFieldsJson());
            for (Object raw : arr) {
                JSONObject obj = JSONUtil.parseObj(raw);
                String code = StrUtil.blankToDefault(obj.getStr("code"), "").trim();
                if (StrUtil.isBlank(code)) {
                    continue;
                }
                XqCategoryFieldConfigRespVO.Item item = new XqCategoryFieldConfigRespVO.Item();
                item.setCode(code);
                item.setDefaultValue(StrUtil.blankToDefault(obj.getStr("defaultValue"), ""));
                item.setValueSource(StrUtil.blankToDefault(obj.getStr("valueSource"), ""));
                item.setZone(normalizeFieldZone(obj.getStr("zone")));
                map.put(code, item);
            }
        } catch (Exception ignored) {
            // 脏 JSON 当未配置
        }
        return map;
    }

    public void saveImageRule(XqImageGenRuleSaveReqVO reqVO) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(reqVO.getPlatformId());
        if (StrUtil.isBlank(sourcePlatformId)) {
            sourcePlatformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        }
        String categoryId = StrUtil.blankToDefault(reqVO.getCategoryId(), "");
        XqImageGenRuleDO exists = imageGenRuleMapper.selectByPlatformAndCategory(sourcePlatformId, categoryId);
        LocalDateTime now = LocalDateTime.now();
        if (exists == null) {
            XqImageGenRuleDO insert = new XqImageGenRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(sourcePlatformId);
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
                + "\"limits\":{\"titleMaxLen\":200,\"descriptionMaxLen\":2000,\"featureMaxLen\":400}}";
    }

    /** 主库事务会把 SqlSession 钉在 master，@DS("xq") 失效；强制切到原库再查规则表。 */
    private static <T> T onXqDs(java.util.function.Supplier<T> action) {
        DynamicDataSourceContextHolder.push("xq");
        try {
            return action.get();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

}
