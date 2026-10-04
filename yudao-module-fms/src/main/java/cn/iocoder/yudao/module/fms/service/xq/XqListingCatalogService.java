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
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqCopyGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqImageGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingCategoryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqStoreMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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

    /** 挂起外层主库事务，否则 @DS("xq") 仍会打到 ruoyi-vue-pro */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public XqCopyGenRuleRespVO getCopyRule(String platformId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        XqCopyGenRuleDO rule = copyGenRuleMapper.selectByPlatformId(sourcePlatformId);
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public XqImageGenRuleRespVO getImageRule(String platformId, String categoryId) {
        String sourcePlatformId = storeScopeService.resolveSourcePlatformId(platformId);
        String cid = StrUtil.blankToDefault(categoryId, "");
        XqImageGenRuleDO rule = imageGenRuleMapper.selectByPlatformAndCategory(sourcePlatformId, cid);
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

}
