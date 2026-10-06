package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqPriceMarkupRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqPriceMarkupRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqSkuRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqSkuRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolImportReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolImportRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolItemRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolStatsRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformPriceMarkupRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformSkuRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformUpcRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqUpcPoolDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformPriceMarkupRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformSkuRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformUpcRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqStoreMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqUpcPoolMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_MARKUP_RULE_DUPLICATE;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_MARKUP_RULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PLATFORM_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_SKU_RULE_DUPLICATE;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_SKU_RULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_SPECIAL_CONFIG_INVALID;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_STORE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_STORE_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_UPC_POOL_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_UPC_RULE_DUPLICATE;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_UPC_RULE_NOT_EXISTS;

/**
 * 特殊配置：SKU / 价格增幅 / UPC 池。写回原库 t_giga_*，平台/店铺用业务 ID，落库用 source_*。
 * 不要加事务，否则 @DS("xq") 会被主库 SqlSession 钉死。
 */
@Service
@Validated
public class XqSpecialConfigService {

    private static final String SHARED_USER = "shared";
    private static final Set<String> SKU_TYPES = Set.of(
            "text", "shopCode", "ownerInitials", "date", "seq", "randomDigits");
    private static final Pattern UPC_SPLIT = Pattern.compile("[\\s,;，；\\n\\r\\t]+");

    @Resource
    private XqStoreScopeService storeScopeService;
    @Resource
    private XqPlatformMapper xqPlatformMapper;
    @Resource
    private XqStoreMapper xqStoreMapper;
    @Resource
    private XqPlatformSkuRuleMapper skuRuleMapper;
    @Resource
    private XqPlatformPriceMarkupRuleMapper markupRuleMapper;
    @Resource
    private XqPlatformUpcRuleMapper upcRuleMapper;
    @Resource
    private XqUpcPoolMapper upcPoolMapper;
    @Resource
    private AdminUserApi adminUserApi;

    public List<XqSkuRuleRespVO> listSkuRules(String platformId, String shopId) {
        String sourcePlatformId = requireSourcePlatform(platformId);
        List<String> shopIds = shopIdCandidates(shopId);
        List<XqPlatformSkuRuleDO> rows = onXqDs(() -> skuRuleMapper.selectListByPlatform(
                sourcePlatformId, shopIds.isEmpty() ? null : shopIds));
        Map<String, StoreView> shops = shopViewMap();
        Map<Long, String> owners = ownerNames(rows.stream().map(XqPlatformSkuRuleDO::getOwnerUserId).toList());
        XqPlatformDO platform = storeScopeService.getPlatform(XqStoreScopeService.parseLong(platformId));
        List<XqSkuRuleRespVO> out = new ArrayList<>();
        for (XqPlatformSkuRuleDO row : rows) {
            out.add(toSkuVo(row, platform, shops, owners));
        }
        return out;
    }

    public XqSkuRuleRespVO saveSkuRule(XqSkuRuleSaveReqVO reqVO) {
        String sourcePlatformId = requireSourcePlatform(reqVO.getPlatformId());
        XqStoreDO store = requireStore(reqVO.getShopId());
        String persistShopId = persistShopId(store);
        String ownerUserId = StrUtil.blankToDefault(reqVO.getOwnerUserId(), "");
        boolean skipSku = Boolean.TRUE.equals(reqVO.getSkipSku());
        List<Map<String, Object>> segments = normalizeSkuSegments(reqVO.getSegments());
        if (!skipSku && segments.isEmpty()) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "请至少配置一个 SKU 规则段");
        }
        final String excludeId = StrUtil.blankToDefault(reqVO.getId(), "");
        XqPlatformSkuRuleDO dup = onXqDs(() -> skuRuleMapper.selectDuplicate(
                sourcePlatformId, persistShopId, ownerUserId, excludeId));
        if (dup != null) {
            throw exception(XQ_SKU_RULE_DUPLICATE);
        }
        String name = StrUtil.blankToDefault(reqVO.getName(), defaultSkuName(reqVO.getPlatformId(), store, ownerUserId));
        LocalDateTime now = LocalDateTime.now();
        final String savedId;
        if (StrUtil.isBlank(excludeId)) {
            XqPlatformSkuRuleDO insert = new XqPlatformSkuRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setUserId(SHARED_USER);
            insert.setPlatformId(sourcePlatformId);
            insert.setShopId(persistShopId);
            insert.setOwnerUserId(ownerUserId);
            insert.setName(name);
            insert.setSegmentsJson(JSONUtil.toJsonStr(segments));
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setSkipSku(skipSku);
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            onXqDs(() -> {
                skuRuleMapper.insert(insert);
                return true;
            });
            savedId = insert.getId();
        } else {
            XqPlatformSkuRuleDO exists = onXqDs(() -> skuRuleMapper.selectById(excludeId));
            if (exists == null) {
                throw exception(XQ_SKU_RULE_NOT_EXISTS);
            }
            exists.setPlatformId(sourcePlatformId);
            exists.setShopId(persistShopId);
            exists.setOwnerUserId(ownerUserId);
            exists.setName(name);
            exists.setSegmentsJson(JSONUtil.toJsonStr(segments));
            exists.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            exists.setSkipSku(skipSku);
            exists.setUpdateDate(now);
            onXqDs(() -> {
                skuRuleMapper.updateById(exists);
                return true;
            });
            savedId = excludeId;
        }
        XqPlatformSkuRuleDO saved = onXqDs(() -> skuRuleMapper.selectById(savedId));
        return toSkuVo(saved, storeScopeService.getPlatform(store.getPlatformId()), shopViewMap(),
                ownerNames(List.of(ownerUserId)));
    }

    public void deleteSkuRule(String id) {
        XqPlatformSkuRuleDO row = onXqDs(() -> skuRuleMapper.selectById(id));
        if (row == null) {
            throw exception(XQ_SKU_RULE_NOT_EXISTS);
        }
        onXqDs(() -> {
            skuRuleMapper.deleteById(id);
            return true;
        });
    }

    public List<XqPriceMarkupRuleRespVO> listMarkupRules(String platformId, String shopId) {
        String sourcePlatformId = requireSourcePlatform(platformId);
        List<String> shopIds = shopIdCandidates(shopId);
        List<XqPlatformPriceMarkupRuleDO> rows = onXqDs(() -> markupRuleMapper.selectListByPlatform(
                sourcePlatformId, shopIds.isEmpty() ? null : shopIds));
        Map<String, StoreView> shops = shopViewMap();
        Map<Long, String> owners = ownerNames(rows.stream().map(XqPlatformPriceMarkupRuleDO::getOwnerUserId).toList());
        XqPlatformDO platform = storeScopeService.getPlatform(XqStoreScopeService.parseLong(platformId));
        List<XqPriceMarkupRuleRespVO> out = new ArrayList<>();
        for (XqPlatformPriceMarkupRuleDO row : rows) {
            out.add(toMarkupVo(row, platform, shops, owners));
        }
        return out;
    }

    public XqPriceMarkupRuleRespVO saveMarkupRule(XqPriceMarkupRuleSaveReqVO reqVO) {
        String sourcePlatformId = requireSourcePlatform(reqVO.getPlatformId());
        XqStoreDO store = requireStore(reqVO.getShopId());
        String persistShopId = persistShopId(store);
        String ownerUserId = StrUtil.blankToDefault(reqVO.getOwnerUserId(), "");
        String mode = normalizeMode(reqVO.getMode());
        BigDecimal value = reqVO.getValue();
        if (value == null) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "请填写增幅数值");
        }
        if (("percent".equals(mode) || "ratio".equals(mode)) && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "百分比 / 比例增幅须大于 0");
        }
        BigDecimal freight = reqVO.getFreightRate() == null || reqVO.getFreightRate().compareTo(BigDecimal.ZERO) <= 0
                ? new BigDecimal("0.85") : reqVO.getFreightRate();
        String applyTo = normalizeApplyTo(reqVO.getApplyTo());
        final String excludeId = StrUtil.blankToDefault(reqVO.getId(), "");
        XqPlatformPriceMarkupRuleDO dup = onXqDs(() -> markupRuleMapper.selectDuplicate(
                sourcePlatformId, persistShopId, ownerUserId, excludeId));
        if (dup != null) {
            throw exception(XQ_MARKUP_RULE_DUPLICATE);
        }
        String name = StrUtil.blankToDefault(reqVO.getName(), defaultMarkupName(reqVO.getPlatformId(), store, ownerUserId));
        LocalDateTime now = LocalDateTime.now();
        final String savedId;
        if (StrUtil.isBlank(excludeId)) {
            XqPlatformPriceMarkupRuleDO insert = new XqPlatformPriceMarkupRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setUserId(SHARED_USER);
            insert.setPlatformId(sourcePlatformId);
            insert.setShopId(persistShopId);
            insert.setOwnerUserId(ownerUserId);
            insert.setName(name);
            insert.setMode(mode);
            insert.setValue(value);
            insert.setFreightRate(freight);
            insert.setApplyTo(applyTo);
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            onXqDs(() -> {
                markupRuleMapper.insert(insert);
                return true;
            });
            savedId = insert.getId();
        } else {
            XqPlatformPriceMarkupRuleDO exists = onXqDs(() -> markupRuleMapper.selectById(excludeId));
            if (exists == null) {
                throw exception(XQ_MARKUP_RULE_NOT_EXISTS);
            }
            exists.setPlatformId(sourcePlatformId);
            exists.setShopId(persistShopId);
            exists.setOwnerUserId(ownerUserId);
            exists.setName(name);
            exists.setMode(mode);
            exists.setValue(value);
            exists.setFreightRate(freight);
            exists.setApplyTo(applyTo);
            exists.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            exists.setUpdateDate(now);
            onXqDs(() -> {
                markupRuleMapper.updateById(exists);
                return true;
            });
            savedId = excludeId;
        }
        XqPlatformPriceMarkupRuleDO saved = onXqDs(() -> markupRuleMapper.selectById(savedId));
        return toMarkupVo(saved, storeScopeService.getPlatform(store.getPlatformId()), shopViewMap(),
                ownerNames(List.of(ownerUserId)));
    }

    public void deleteMarkupRule(String id) {
        XqPlatformPriceMarkupRuleDO row = onXqDs(() -> markupRuleMapper.selectById(id));
        if (row == null) {
            throw exception(XQ_MARKUP_RULE_NOT_EXISTS);
        }
        onXqDs(() -> {
            markupRuleMapper.deleteById(id);
            return true;
        });
    }

    public List<XqUpcRuleRespVO> listUpcRules(String platformId) {
        String sourcePlatformId = requireSourcePlatform(platformId);
        List<XqPlatformUpcRuleDO> rows = onXqDs(() -> upcRuleMapper.selectListByPlatform(sourcePlatformId));
        XqUpcPoolStatsRespVO stats = getUpcPoolStats();
        XqPlatformDO platform = storeScopeService.getPlatform(XqStoreScopeService.parseLong(platformId));
        List<XqUpcRuleRespVO> out = new ArrayList<>();
        for (XqPlatformUpcRuleDO row : rows) {
            out.add(toUpcRuleVo(row, platform, stats));
        }
        return out;
    }

    public XqUpcRuleRespVO saveUpcRule(XqUpcRuleSaveReqVO reqVO) {
        String sourcePlatformId = requireSourcePlatform(reqVO.getPlatformId());
        String mode = normalizeUpcMode(reqVO.getMode());
        if ("auto".equals(mode) && StrUtil.isBlank(digitsOnly(reqVO.getManufacturerCode()))) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "自生成模式请填写厂商代码");
        }
        final String excludeId = StrUtil.blankToDefault(reqVO.getId(), "");
        XqPlatformUpcRuleDO dup = onXqDs(() -> upcRuleMapper.selectPlatformWide(sourcePlatformId, excludeId));
        if (dup != null && StrUtil.isBlank(reqVO.getShopId())) {
            throw exception(XQ_UPC_RULE_DUPLICATE);
        }
        XqPlatformDO platform = storeScopeService.getPlatform(XqStoreScopeService.parseLong(reqVO.getPlatformId()));
        String name = StrUtil.blankToDefault(reqVO.getName(),
                (platform != null ? platform.getName() : "") + " UPC 通用规则");
        LocalDateTime now = LocalDateTime.now();
        final String savedId;
        if (StrUtil.isBlank(excludeId)) {
            XqPlatformUpcRuleDO insert = new XqPlatformUpcRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setUserId(SHARED_USER);
            insert.setPlatformId(sourcePlatformId);
            insert.setShopId(StrUtil.blankToDefault(reqVO.getShopId(), ""));
            insert.setName(name);
            insert.setMode(mode);
            insert.setFlagDigit(StrUtil.blankToDefault(digitsOnly(reqVO.getFlagDigit()), "0").substring(0, 1));
            insert.setManufacturerCode(digitsOnly(reqVO.getManufacturerCode()));
            insert.setProductWidth(clamp(reqVO.getProductWidth(), 1, 6, 5));
            insert.setProductStart(Math.max(0, reqVO.getProductStart() == null ? 1 : reqVO.getProductStart()));
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            onXqDs(() -> {
                upcRuleMapper.insert(insert);
                return true;
            });
            savedId = insert.getId();
        } else {
            XqPlatformUpcRuleDO exists = onXqDs(() -> upcRuleMapper.selectById(excludeId));
            if (exists == null) {
                throw exception(XQ_UPC_RULE_NOT_EXISTS);
            }
            exists.setPlatformId(sourcePlatformId);
            exists.setShopId(StrUtil.blankToDefault(reqVO.getShopId(), ""));
            exists.setName(name);
            exists.setMode(mode);
            exists.setFlagDigit(StrUtil.blankToDefault(digitsOnly(reqVO.getFlagDigit()), "0").substring(0, 1));
            exists.setManufacturerCode(digitsOnly(reqVO.getManufacturerCode()));
            exists.setProductWidth(clamp(reqVO.getProductWidth(), 1, 6, 5));
            exists.setProductStart(Math.max(0, reqVO.getProductStart() == null ? 1 : reqVO.getProductStart()));
            exists.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            exists.setUpdateDate(now);
            onXqDs(() -> {
                upcRuleMapper.updateById(exists);
                return true;
            });
            savedId = excludeId;
        }
        XqPlatformUpcRuleDO saved = onXqDs(() -> upcRuleMapper.selectById(savedId));
        return toUpcRuleVo(saved, platform, getUpcPoolStats());
    }

    public void deleteUpcRule(String id) {
        XqPlatformUpcRuleDO row = onXqDs(() -> upcRuleMapper.selectById(id));
        if (row == null) {
            throw exception(XQ_UPC_RULE_NOT_EXISTS);
        }
        onXqDs(() -> {
            upcRuleMapper.deleteById(id);
            return true;
        });
    }

    public XqUpcPoolStatsRespVO getUpcPoolStats() {
        List<Map<String, Object>> rows = onXqDs(() -> upcPoolMapper.selectGlobalStats());
        XqUpcPoolStatsRespVO vo = new XqUpcPoolStatsRespVO();
        vo.setFreeAvailable(0L);
        vo.setPaidAvailable(0L);
        vo.setUsed(0L);
        vo.setVoidCount(0L);
        if (rows == null) {
            return vo;
        }
        for (Map<String, Object> row : rows) {
            String type = String.valueOf(row.getOrDefault("poolType", row.get("pool_type")));
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.getOrDefault("cnt", 0)).longValue();
            if ("available".equals(status) && "free".equals(type)) {
                vo.setFreeAvailable(cnt);
            } else if ("available".equals(status) && "paid".equals(type)) {
                vo.setPaidAvailable(cnt);
            } else if ("used".equals(status)) {
                vo.setUsed(vo.getUsed() + cnt);
            } else if ("void".equals(status)) {
                vo.setVoidCount(vo.getVoidCount() + cnt);
            }
        }
        return vo;
    }

    public PageResult<XqUpcPoolItemRespVO> pageUpcPool(String poolType, String status, Integer pageNo, Integer pageSize) {
        int pn = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int ps = pageSize == null || pageSize < 1 ? 50 : Math.min(pageSize, 200);
        IPage<XqUpcPoolDO> page = onXqDs(() -> upcPoolMapper.selectPage(
                new Page<>(pn, ps), StrUtil.blankToDefault(poolType, null), StrUtil.blankToDefault(status, null)));
        List<XqUpcPoolItemRespVO> list = new ArrayList<>();
        for (XqUpcPoolDO row : page.getRecords()) {
            XqUpcPoolItemRespVO item = new XqUpcPoolItemRespVO();
            item.setId(row.getId());
            item.setUpc(row.getUpc());
            item.setPoolType(row.getPoolType());
            item.setStatus(row.getStatus());
            item.setClaimedByName(row.getClaimedByName());
            item.setClaimedAt(row.getClaimedAt());
            item.setCreateDate(row.getCreateDate());
            list.add(item);
        }
        return new PageResult<>(list, page.getTotal());
    }

    public XqUpcPoolImportRespVO importUpcPool(XqUpcPoolImportReqVO reqVO) {
        String poolType = "free".equalsIgnoreCase(StrUtil.blankToDefault(reqVO.getPoolType(), "paid")) ? "free" : "paid";
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (reqVO.getCodes() != null) {
            for (String c : reqVO.getCodes()) {
                String d = digitsOnly(c);
                if (StrUtil.isNotBlank(d)) {
                    candidates.add(d);
                }
            }
        }
        if (StrUtil.isNotBlank(reqVO.getText())) {
            for (String part : UPC_SPLIT.split(reqVO.getText())) {
                String d = digitsOnly(part);
                if (StrUtil.isNotBlank(d)) {
                    candidates.add(d);
                }
            }
        }
        if (candidates.isEmpty()) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "请粘贴或导入至少一个 UPC");
        }
        List<String> invalid = new ArrayList<>();
        List<String> valid = new ArrayList<>();
        for (String code : candidates) {
            if (isValidUpcA(code)) {
                valid.add(code);
            } else {
                invalid.add(code);
            }
        }
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < valid.size(); i += 800) {
            List<String> chunk = valid.subList(i, Math.min(i + 800, valid.size()));
            List<XqUpcPoolDO> found = onXqDs(() -> upcPoolMapper.selectExisting(chunk));
            for (XqUpcPoolDO row : found) {
                existing.add(row.getUpc());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        int imported = 0;
        int skipped = 0;
        for (String code : valid) {
            if (existing.contains(code)) {
                skipped++;
                continue;
            }
            existing.add(code);
            XqUpcPoolDO row = new XqUpcPoolDO();
            row.setId(IdUtil.fastUUID());
            row.setUserId(SHARED_USER);
            row.setPlatformId(XqUpcPoolMapper.GLOBAL_PLATFORM);
            row.setShopId("");
            row.setUpc(code);
            row.setPoolType(poolType);
            row.setStatus("available");
            row.setSource("import");
            row.setCreateDate(now);
            row.setUpdateDate(now);
            onXqDs(() -> {
                upcPoolMapper.insert(row);
                return true;
            });
            imported++;
        }
        XqUpcPoolImportRespVO vo = new XqUpcPoolImportRespVO();
        vo.setImported(imported);
        vo.setSkipped(skipped);
        vo.setInvalid(invalid);
        vo.setPoolType(poolType);
        return vo;
    }

    public void voidUpcPoolItem(String id) {
        XqUpcPoolDO row = onXqDs(() -> upcPoolMapper.selectById(id));
        if (row == null) {
            throw exception(XQ_UPC_POOL_NOT_EXISTS);
        }
        if ("used".equals(row.getStatus())) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "已占用的 UPC 请先释放再作废");
        }
        row.setStatus("void");
        row.setUpdateDate(LocalDateTime.now());
        onXqDs(() -> {
            upcPoolMapper.updateById(row);
            return true;
        });
    }

    public void releaseUpcPoolItem(String id) {
        XqUpcPoolDO row = onXqDs(() -> upcPoolMapper.selectById(id));
        if (row == null) {
            throw exception(XQ_UPC_POOL_NOT_EXISTS);
        }
        row.setStatus("available");
        row.setProductListId(null);
        row.setClaimedBy(null);
        row.setClaimedByName(null);
        row.setClaimedAt(null);
        row.setUpdateDate(LocalDateTime.now());
        onXqDs(() -> {
            upcPoolMapper.updateById(row);
            return true;
        });
    }

    private String requireSourcePlatform(String platformId) {
        String source = storeScopeService.resolveSourcePlatformId(platformId);
        if (StrUtil.isBlank(source)) {
            throw exception(XQ_PLATFORM_NOT_EXISTS);
        }
        return source;
    }

    private XqStoreDO requireStore(String shopId) {
        Long id = XqStoreScopeService.parseLong(shopId);
        if (id == null) {
            throw exception(XQ_STORE_NOT_EXISTS);
        }
        if (!storeScopeService.isStoreAllowed(getLoginUserId(), id)) {
            throw exception(XQ_STORE_ACCESS_DENIED);
        }
        XqStoreDO store = xqStoreMapper.selectById(id);
        if (store == null) {
            throw exception(XQ_STORE_NOT_EXISTS);
        }
        return store;
    }

    private List<String> shopIdCandidates(String shopId) {
        if (StrUtil.isBlank(shopId)) {
            return List.of();
        }
        Long xqId = XqStoreScopeService.parseLong(shopId);
        if (xqId == null) {
            return List.of(shopId.trim());
        }
        List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(xqId));
        XqStoreDO store = xqStoreMapper.selectById(xqId);
        if (store != null && store.getSourceStoreId() != null) {
            ids.add(String.valueOf(store.getSourceStoreId()));
        }
        return ids;
    }

    private String persistShopId(XqStoreDO store) {
        if (store.getSourceStoreId() != null) {
            return String.valueOf(store.getSourceStoreId());
        }
        return String.valueOf(store.getId());
    }

    private Map<String, StoreView> shopViewMap() {
        Map<String, StoreView> map = new HashMap<>();
        for (XqStoreDO store : xqStoreMapper.selectListAll()) {
            StoreView view = new StoreView(String.valueOf(store.getId()), store.getName());
            map.put(String.valueOf(store.getId()), view);
            if (store.getSourceStoreId() != null) {
                map.put(String.valueOf(store.getSourceStoreId()), view);
            }
        }
        return map;
    }

    private Map<Long, String> ownerNames(List<String> ownerIds) {
        List<Long> ids = ownerIds.stream()
                .map(XqStoreScopeService::parseLong)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(ids);
        Map<Long, String> names = new HashMap<>();
        users.forEach((id, u) -> names.put(id, StrUtil.blankToDefault(u.getNickname(), String.valueOf(id))));
        return names;
    }

    private XqSkuRuleRespVO toSkuVo(XqPlatformSkuRuleDO row, XqPlatformDO platform,
                                    Map<String, StoreView> shops, Map<Long, String> owners) {
        XqSkuRuleRespVO vo = new XqSkuRuleRespVO();
        vo.setId(row.getId());
        vo.setPlatformId(platform != null ? String.valueOf(platform.getId()) : "");
        vo.setPlatformName(platform != null ? platform.getName() : "");
        StoreView shop = shops.get(StrUtil.blankToDefault(row.getShopId(), ""));
        vo.setShopId(shop != null ? shop.id : row.getShopId());
        vo.setShopName(shop != null ? shop.name : "");
        vo.setOwnerUserId(StrUtil.blankToDefault(row.getOwnerUserId(), ""));
        Long oid = XqStoreScopeService.parseLong(row.getOwnerUserId());
        vo.setOwnerUserName(oid != null ? owners.getOrDefault(oid, row.getOwnerUserId()) : "");
        vo.setName(row.getName());
        vo.setEnabled(row.getEnabled());
        vo.setSkipSku(Boolean.TRUE.equals(row.getSkipSku()));
        vo.setSegments(parseSegments(row.getSegmentsJson()));
        return vo;
    }

    private XqPriceMarkupRuleRespVO toMarkupVo(XqPlatformPriceMarkupRuleDO row, XqPlatformDO platform,
                                               Map<String, StoreView> shops, Map<Long, String> owners) {
        XqPriceMarkupRuleRespVO vo = new XqPriceMarkupRuleRespVO();
        vo.setId(row.getId());
        vo.setPlatformId(platform != null ? String.valueOf(platform.getId()) : "");
        vo.setPlatformName(platform != null ? platform.getName() : "");
        StoreView shop = shops.get(StrUtil.blankToDefault(row.getShopId(), ""));
        vo.setShopId(shop != null ? shop.id : row.getShopId());
        vo.setShopName(shop != null ? shop.name : "");
        vo.setOwnerUserId(StrUtil.blankToDefault(row.getOwnerUserId(), ""));
        Long oid = XqStoreScopeService.parseLong(row.getOwnerUserId());
        vo.setOwnerUserName(oid != null ? owners.getOrDefault(oid, row.getOwnerUserId()) : "");
        vo.setName(row.getName());
        vo.setMode(row.getMode());
        vo.setValue(row.getValue());
        vo.setFreightRate(row.getFreightRate());
        vo.setApplyTo(row.getApplyTo());
        vo.setEnabled(row.getEnabled());
        vo.setSummary(markupSummary(row.getMode(), row.getValue(), row.getFreightRate()));
        vo.setSamplePrice(calcMarkup(new BigDecimal("100"), row.getMode(), row.getValue(), row.getFreightRate()));
        return vo;
    }

    private XqUpcRuleRespVO toUpcRuleVo(XqPlatformUpcRuleDO row, XqPlatformDO platform, XqUpcPoolStatsRespVO stats) {
        XqUpcRuleRespVO vo = new XqUpcRuleRespVO();
        vo.setId(row.getId());
        vo.setPlatformId(platform != null ? String.valueOf(platform.getId()) : "");
        vo.setPlatformName(platform != null ? platform.getName() : "");
        vo.setShopId(StrUtil.blankToDefault(row.getShopId(), ""));
        vo.setName(row.getName());
        vo.setMode(row.getMode());
        vo.setFlagDigit(row.getFlagDigit());
        vo.setManufacturerCode(row.getManufacturerCode());
        vo.setProductWidth(row.getProductWidth());
        vo.setProductStart(row.getProductStart());
        vo.setEnabled(row.getEnabled());
        if (stats != null) {
            vo.setPoolFreeAvailable(stats.getFreeAvailable());
            vo.setPoolPaidAvailable(stats.getPaidAvailable());
            vo.setPoolUsed(stats.getUsed());
        }
        return vo;
    }

    private List<Map<String, Object>> normalizeSkuSegments(List<Map<String, Object>> raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (Map<String, Object> seg : raw) {
            if (seg == null) {
                continue;
            }
            String type = String.valueOf(seg.getOrDefault("type", "")).trim();
            if ("originalSku".equals(type)) {
                throw exception(XQ_SPECIAL_CONFIG_INVALID, "已禁止使用「原SKU」规则段");
            }
            if (!SKU_TYPES.contains(type)) {
                continue;
            }
            Map<String, Object> next = new HashMap<>();
            next.put("type", type);
            next.put("value", seg.get("value") == null ? "" : String.valueOf(seg.get("value")));
            next.put("format", seg.get("format") == null ? "" : String.valueOf(seg.get("format")).trim());
            int widthFallback = "randomDigits".equals(type) ? 5 : 3;
            int width = toInt(seg.get("width"), widthFallback);
            if ("randomDigits".equals(type)) {
                width = Math.max(4, Math.min(8, width));
            } else {
                width = Math.max(1, Math.min(12, width));
            }
            next.put("width", width);
            next.put("start", Math.max(0, toInt(seg.get("start"), 1)));
            out.add(next);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSegments(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object o : arr) {
                if (o instanceof JSONObject obj) {
                    list.add(obj);
                } else if (o instanceof Map<?, ?> map) {
                    list.add((Map<String, Object>) map);
                }
            }
            return list;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String defaultSkuName(String platformId, XqStoreDO store, String ownerUserId) {
        XqPlatformDO p = storeScopeService.getPlatform(XqStoreScopeService.parseLong(platformId));
        String owner = "";
        Long oid = XqStoreScopeService.parseLong(ownerUserId);
        if (oid != null) {
            owner = " / " + ownerNames(List.of(ownerUserId)).getOrDefault(oid, ownerUserId);
        }
        return (p != null ? p.getName() : "") + " / " + store.getName() + owner + " SKU 规则";
    }

    private String defaultMarkupName(String platformId, XqStoreDO store, String ownerUserId) {
        return defaultSkuName(platformId, store, ownerUserId).replace(" SKU 规则", " 价格增幅");
    }

    private static String normalizeMode(String mode) {
        String m = StrUtil.blankToDefault(mode, "percent").trim().toLowerCase();
        if (!Set.of("fixed", "percent", "ratio").contains(m)) {
            throw exception(XQ_SPECIAL_CONFIG_INVALID, "增幅类型须为固定 / 百分比 / 比例");
        }
        return m;
    }

    private static String normalizeApplyTo(String applyTo) {
        String m = StrUtil.blankToDefault(applyTo, "both").trim().toLowerCase();
        return Set.of("dropship", "pickup", "both").contains(m) ? m : "both";
    }

    private static String normalizeUpcMode(String mode) {
        String m = StrUtil.blankToDefault(mode, "none").trim().toLowerCase();
        return Set.of("none", "auto", "pool").contains(m) ? m : "none";
    }

    private static String markupSummary(String mode, BigDecimal value, BigDecimal freight) {
        BigDecimal rate = freight == null || freight.compareTo(BigDecimal.ZERO) <= 0
                ? new BigDecimal("0.85") : freight;
        if (value == null) {
            return "—";
        }
        if ("fixed".equals(mode)) {
            return "(货价 + " + value.stripTrailingZeros().toPlainString() + ") ÷ " + rate.stripTrailingZeros().toPlainString();
        }
        if ("percent".equals(mode)) {
            return "(货价 × " + value.stripTrailingZeros().toPlainString() + "%) ÷ " + rate.stripTrailingZeros().toPlainString();
        }
        if ("ratio".equals(mode)) {
            return "(货价 × " + value.stripTrailingZeros().toPlainString() + ") ÷ " + rate.stripTrailingZeros().toPlainString();
        }
        return "—";
    }

    private static BigDecimal calcMarkup(BigDecimal base, String mode, BigDecimal value, BigDecimal freight) {
        if (base == null || value == null) {
            return null;
        }
        BigDecimal rate = freight == null || freight.compareTo(BigDecimal.ZERO) <= 0
                ? new BigDecimal("0.85") : freight;
        BigDecimal marked = base;
        if ("fixed".equals(mode)) {
            marked = base.add(value);
        } else if ("percent".equals(mode)) {
            marked = base.multiply(value).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        } else if ("ratio".equals(mode)) {
            marked = base.multiply(value);
        } else {
            return null;
        }
        return marked.divide(rate, 2, RoundingMode.HALF_UP);
    }

    private static String digitsOnly(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\D", "");
    }

    private static boolean isValidUpcA(String digits) {
        if (digits == null || digits.length() != 12) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int d = digits.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            sum += (i % 2 == 0) ? d * 3 : d;
        }
        int check = (10 - (sum % 10)) % 10;
        return check == (digits.charAt(11) - '0');
    }

    private static int clamp(Integer value, int min, int max, int fallback) {
        int v = value == null ? fallback : value;
        return Math.max(min, Math.min(max, v));
    }

    private static int toInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static <T> T onXqDs(java.util.function.Supplier<T> action) {
        DynamicDataSourceContextHolder.push("xq");
        try {
            return action.get();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private record StoreView(String id, String name) {
    }

}
