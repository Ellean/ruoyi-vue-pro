package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformCredentialMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_CREDENTIAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_CREDENTIAL_REQUIRED;

@Slf4j
@Service
@Validated
public class XqListingCredentialServiceImpl implements XqListingCredentialService {

    @Resource
    private XqListingPlatformCredentialMapper credentialMapper;
    @Resource
    private XqListingPlatformMapper listingPlatformMapper;
    @Resource
    private XqPlatformMapper platformMapper;
    @Resource
    private XqSecretCrypto secretCrypto;

    @Override
    public List<XqListingCredentialRespVO> list() {
        return BeanUtils.toBean(credentialMapper.selectListAll(), XqListingCredentialRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(XqListingCredentialSaveReqVO reqVO) {
        String provider = normalizeProvider(reqVO.getProvider());
        boolean create = reqVO.getId() == null;
        if (create) {
            if (XqListingPlatformCredentialDO.PROVIDER_MIRAKL.equals(provider)
                    && StrUtil.isBlank(reqVO.getApiKey())) {
                throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
            }
            if (XqListingPlatformCredentialDO.PROVIDER_WAYFAIR.equals(provider)
                    && (StrUtil.isBlank(reqVO.getClientId()) || StrUtil.isBlank(reqVO.getClientSecret()))) {
                throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
            }
            XqListingPlatformCredentialDO row = buildNew(reqVO, provider);
            credentialMapper.insert(row);
            return row.getId();
        }
        XqListingPlatformCredentialDO existing = credentialMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(XQ_LISTING_CREDENTIAL_NOT_EXISTS);
        }
        XqListingPlatformCredentialDO update = new XqListingPlatformCredentialDO();
        update.setId(existing.getId());
        applyCommon(update, reqVO, provider);
        if (StrUtil.isNotBlank(reqVO.getApiKey())) {
            update.setApiKeyEnc(secretCrypto.encrypt(reqVO.getApiKey().trim()));
            update.setApiKeyMask(XqSecretCrypto.mask(reqVO.getApiKey().trim()));
        }
        if (StrUtil.isNotBlank(reqVO.getClientSecret())) {
            update.setClientSecretEnc(secretCrypto.encrypt(reqVO.getClientSecret().trim()));
            update.setClientSecretMask(XqSecretCrypto.mask(reqVO.getClientSecret().trim()));
        }
        credentialMapper.updateById(update);
        return existing.getId();
    }

    @Override
    public void delete(Long id) {
        if (credentialMapper.selectById(id) == null) {
            throw exception(XQ_LISTING_CREDENTIAL_NOT_EXISTS);
        }
        credentialMapper.deleteById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> syncFromXq() {
        // 原库平台表已迁入 master
        List<XqListingPlatformDO> legacy = listingPlatformMapper.selectAllList();
        Map<String, XqPlatformDO> bySource = new HashMap<>();
        Map<String, XqPlatformDO> byCode = new HashMap<>();
        for (XqPlatformDO p : platformMapper.selectListAll()) {
            if (StrUtil.isNotBlank(p.getSourcePlatformId())) {
                bySource.put(p.getSourcePlatformId().trim(), p);
            }
            if (StrUtil.isNotBlank(p.getCode())) {
                byCode.put(p.getCode().trim().toLowerCase(Locale.ROOT), p);
            }
        }
        int mirakl = 0;
        int wayfair = 0;
        int skipped = 0;
        for (XqListingPlatformDO lp : legacy) {
            if (lp == null || StrUtil.isBlank(lp.getId())) {
                continue;
            }
            XqPlatformDO biz = bySource.get(lp.getId());
            if (biz == null && StrUtil.isNotBlank(lp.getCode())) {
                biz = byCode.get(lp.getCode().trim().toLowerCase(Locale.ROOT));
            }
            boolean any = false;
            if (upsertMirakl(lp, biz)) {
                mirakl++;
                any = true;
            }
            if (upsertWayfair(lp, biz)) {
                wayfair++;
                any = true;
            }
            if (!any) {
                skipped++;
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("mirakl", mirakl);
        out.put("wayfair", wayfair);
        out.put("skipped", skipped);
        out.put("totalLegacy", legacy.size());
        return out;
    }

    @Override
    public XqListingPlatformCredentialDO getDecryptedForPlatform(Long platformId, String provider) {
        XqListingPlatformCredentialDO row =
                credentialMapper.selectEnabledByPlatformAndProvider(platformId, normalizeProvider(provider));
        if (row == null) {
            return null;
        }
        return decryptCopy(row);
    }

    @Override
    public XqListingPlatformCredentialDO getDecryptedBySource(String sourcePlatformId, String provider) {
        XqListingPlatformCredentialDO row =
                credentialMapper.selectByProviderAndSource(normalizeProvider(provider), sourcePlatformId);
        if (row == null || Boolean.FALSE.equals(row.getEnabled())) {
            return null;
        }
        return decryptCopy(row);
    }

    private boolean upsertMirakl(XqListingPlatformDO lp, XqPlatformDO biz) {
        JSONObject cfg = parseJson(lp.getMiraklConfigJson());
        String apiKey = StrUtil.trim(cfg.getStr("apiKey", cfg.getStr("api_key")));
        String baseUrl = StrUtil.trim(cfg.getStr("baseUrl", cfg.getStr("base_url")));
        if (StrUtil.isBlank(apiKey) && StrUtil.isBlank(baseUrl)) {
            return false;
        }
        String shopId = StrUtil.trim(cfg.getStr("shopId", cfg.getStr("shop_id")));
        XqListingPlatformCredentialDO existing = credentialMapper.selectByProviderAndSource(
                XqListingPlatformCredentialDO.PROVIDER_MIRAKL, lp.getId());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            XqListingPlatformCredentialDO row = XqListingPlatformCredentialDO.builder()
                    .platformId(biz == null ? null : biz.getId())
                    .sourcePlatformId(lp.getId())
                    .platformCode(StrUtil.blankToDefault(lp.getCode(), "unknown"))
                    .platformName(StrUtil.blankToDefault(lp.getName(), lp.getCode()))
                    .provider(XqListingPlatformCredentialDO.PROVIDER_MIRAKL)
                    .baseUrl(StrUtil.blankToDefault(baseUrl, null))
                    .shopId(StrUtil.blankToDefault(shopId, null))
                    .apiKeyEnc(StrUtil.isBlank(apiKey) ? null : secretCrypto.encrypt(apiKey))
                    .apiKeyMask(StrUtil.isBlank(apiKey) ? null : XqSecretCrypto.mask(apiKey))
                    .enabled(true)
                    .syncedFromXqAt(now)
                    .remark("sync from xq mirakl_config_json")
                    .build();
            credentialMapper.insert(row);
        } else {
            XqListingPlatformCredentialDO update = new XqListingPlatformCredentialDO();
            update.setId(existing.getId());
            update.setPlatformId(biz == null ? existing.getPlatformId() : biz.getId());
            update.setPlatformCode(StrUtil.blankToDefault(lp.getCode(), existing.getPlatformCode()));
            update.setPlatformName(StrUtil.blankToDefault(lp.getName(), existing.getPlatformName()));
            update.setBaseUrl(StrUtil.blankToDefault(baseUrl, existing.getBaseUrl()));
            update.setShopId(StrUtil.blankToDefault(shopId, existing.getShopId()));
            if (StrUtil.isNotBlank(apiKey)) {
                update.setApiKeyEnc(secretCrypto.encrypt(apiKey));
                update.setApiKeyMask(XqSecretCrypto.mask(apiKey));
            }
            update.setEnabled(true);
            update.setSyncedFromXqAt(now);
            credentialMapper.updateById(update);
        }
        return true;
    }

    private boolean upsertWayfair(XqListingPlatformDO lp, XqPlatformDO biz) {
        JSONObject cfg = parseJson(lp.getWayfairConfigJson());
        String clientId = StrUtil.trim(cfg.getStr("clientId", cfg.getStr("client_id")));
        String clientSecret = StrUtil.trim(cfg.getStr("clientSecret", cfg.getStr("client_secret")));
        if (StrUtil.isBlank(clientId) && StrUtil.isBlank(clientSecret)) {
            return false;
        }
        String supplierId = StrUtil.trim(cfg.getStr("supplierId", cfg.getStr("supplier_id")));
        String env = StrUtil.trim(cfg.getStr("env", "prod"));
        XqListingPlatformCredentialDO existing = credentialMapper.selectByProviderAndSource(
                XqListingPlatformCredentialDO.PROVIDER_WAYFAIR, lp.getId());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            XqListingPlatformCredentialDO row = XqListingPlatformCredentialDO.builder()
                    .platformId(biz == null ? null : biz.getId())
                    .sourcePlatformId(lp.getId())
                    .platformCode(StrUtil.blankToDefault(lp.getCode(), "unknown"))
                    .platformName(StrUtil.blankToDefault(lp.getName(), lp.getCode()))
                    .provider(XqListingPlatformCredentialDO.PROVIDER_WAYFAIR)
                    .clientId(StrUtil.blankToDefault(clientId, null))
                    .clientSecretEnc(StrUtil.isBlank(clientSecret) ? null : secretCrypto.encrypt(clientSecret))
                    .clientSecretMask(StrUtil.isBlank(clientSecret) ? null : XqSecretCrypto.mask(clientSecret))
                    .supplierId(StrUtil.blankToDefault(supplierId, null))
                    .env(StrUtil.blankToDefault(env, "prod"))
                    .enabled(true)
                    .syncedFromXqAt(now)
                    .remark("sync from xq wayfair_config_json")
                    .build();
            credentialMapper.insert(row);
        } else {
            XqListingPlatformCredentialDO update = new XqListingPlatformCredentialDO();
            update.setId(existing.getId());
            update.setPlatformId(biz == null ? existing.getPlatformId() : biz.getId());
            update.setPlatformCode(StrUtil.blankToDefault(lp.getCode(), existing.getPlatformCode()));
            update.setPlatformName(StrUtil.blankToDefault(lp.getName(), existing.getPlatformName()));
            if (StrUtil.isNotBlank(clientId)) {
                update.setClientId(clientId);
            }
            if (StrUtil.isNotBlank(clientSecret)) {
                update.setClientSecretEnc(secretCrypto.encrypt(clientSecret));
                update.setClientSecretMask(XqSecretCrypto.mask(clientSecret));
            }
            update.setSupplierId(StrUtil.blankToDefault(supplierId, existing.getSupplierId()));
            update.setEnv(StrUtil.blankToDefault(env, existing.getEnv()));
            update.setEnabled(true);
            update.setSyncedFromXqAt(now);
            credentialMapper.updateById(update);
        }
        return true;
    }

    private XqListingPlatformCredentialDO buildNew(XqListingCredentialSaveReqVO reqVO, String provider) {
        XqListingPlatformCredentialDO row = XqListingPlatformCredentialDO.builder()
                .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                .build();
        applyCommon(row, reqVO, provider);
        if (StrUtil.isNotBlank(reqVO.getApiKey())) {
            row.setApiKeyEnc(secretCrypto.encrypt(reqVO.getApiKey().trim()));
            row.setApiKeyMask(XqSecretCrypto.mask(reqVO.getApiKey().trim()));
        }
        if (StrUtil.isNotBlank(reqVO.getClientSecret())) {
            row.setClientSecretEnc(secretCrypto.encrypt(reqVO.getClientSecret().trim()));
            row.setClientSecretMask(XqSecretCrypto.mask(reqVO.getClientSecret().trim()));
        }
        return row;
    }

    private void applyCommon(XqListingPlatformCredentialDO row, XqListingCredentialSaveReqVO reqVO, String provider) {
        row.setPlatformId(reqVO.getPlatformId());
        row.setSourcePlatformId(trimOrNull(reqVO.getSourcePlatformId()));
        row.setPlatformCode(reqVO.getPlatformCode().trim());
        row.setPlatformName(trimOrNull(reqVO.getPlatformName()));
        row.setProvider(provider);
        row.setBaseUrl(trimOrNull(reqVO.getBaseUrl()));
        row.setShopId(trimOrNull(reqVO.getShopId()));
        row.setClientId(trimOrNull(reqVO.getClientId()));
        row.setSupplierId(trimOrNull(reqVO.getSupplierId()));
        row.setEnv(trimOrNull(reqVO.getEnv()));
        row.setExtraJson(trimOrNull(reqVO.getExtraJson()));
        row.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        row.setRemark(trimOrNull(reqVO.getRemark()));
    }

    private XqListingPlatformCredentialDO decryptCopy(XqListingPlatformCredentialDO row) {
        XqListingPlatformCredentialDO copy = BeanUtils.toBean(row, XqListingPlatformCredentialDO.class);
        if (StrUtil.isNotBlank(row.getApiKeyEnc())) {
            copy.setApiKeyEnc(secretCrypto.decrypt(row.getApiKeyEnc()));
        }
        if (StrUtil.isNotBlank(row.getClientSecretEnc())) {
            copy.setClientSecretEnc(secretCrypto.decrypt(row.getClientSecretEnc()));
        }
        return copy;
    }

    private static String normalizeProvider(String raw) {
        String p = StrUtil.blankToDefault(raw, "").trim().toLowerCase(Locale.ROOT);
        if (XqListingPlatformCredentialDO.PROVIDER_MIRAKL.equals(p)
                || XqListingPlatformCredentialDO.PROVIDER_WAYFAIR.equals(p)) {
            return p;
        }
        throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
    }

    private static JSONObject parseJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(raw);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private static String trimOrNull(String s) {
        String t = StrUtil.trim(s);
        return StrUtil.isBlank(t) ? null : t;
    }

}
