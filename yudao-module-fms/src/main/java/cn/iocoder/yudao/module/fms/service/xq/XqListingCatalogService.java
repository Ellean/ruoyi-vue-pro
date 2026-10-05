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
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqCopyGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqImageGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryFieldConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryFieldTemplateMapper;
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
            if (StrUtil.isBlank(def) && StrUtil.isBlank(src)) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.set("code", item.getCode().trim());
            obj.set("defaultValue", def);
            obj.set("valueSource", src);
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

    private void applyFieldConfig(XqCategoryFieldTemplateRespVO vo, String categoryId) {
        if (vo == null || vo.getFields() == null || vo.getFields().isEmpty()
                || StrUtil.isBlank(categoryId)) {
            return;
        }
        XqListingCategoryFieldConfigDO row = categoryFieldConfigMapper.selectByCategoryId(categoryId);
        Map<String, XqCategoryFieldConfigRespVO.Item> map = parseFieldConfigItems(row);
        if (map.isEmpty()) {
            return;
        }
        for (XqCategoryFieldTemplateRespVO.Field field : vo.getFields()) {
            XqCategoryFieldConfigRespVO.Item item = map.get(StrUtil.blankToDefault(field.getCode(), ""));
            if (item == null) {
                continue;
            }
            field.setDefaultValue(item.getDefaultValue());
            field.setValueSource(item.getValueSource());
        }
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
