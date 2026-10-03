package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaApiCredentialDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaApiCredentialMapper;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_CREDENTIAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_CREDENTIAL_PRICE_ROLE_INVALID;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_CREDENTIAL_SECRET_REQUIRED;

@Service
@Validated
public class XqGigaCredentialServiceImpl implements XqGigaCredentialService {

    @Resource
    private XqGigaApiCredentialMapper credentialMapper;
    @Resource
    private XqSecretCrypto secretCrypto;

    @Override
    public List<XqGigaCredentialRespVO> list() {
        return BeanUtils.toBean(credentialMapper.selectListAll(), XqGigaCredentialRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(XqGigaCredentialSaveReqVO reqVO) {
        boolean create = reqVO.getId() == null;
        String secret = StrUtil.trim(reqVO.getClientSecret());
        if (create && StrUtil.isBlank(secret)) {
            throw exception(XQ_GIGA_CREDENTIAL_SECRET_REQUIRED);
        }
        String priceRole = normalizePriceRole(reqVO.getPriceRole());
        String vendorCode = StrUtil.trim(reqVO.getVendorCode());
        String dedupe = normalizeDedupe(reqVO.getSyncDedupeMode());
        if (Boolean.TRUE.equals(reqVO.getIsDefault())) {
            credentialMapper.clearDefault(vendorCode, priceRole);
        }
        if (create) {
            XqGigaApiCredentialDO row = XqGigaApiCredentialDO.builder()
                    .name(reqVO.getName().trim())
                    .vendorCode(StrUtil.blankToDefault(vendorCode, null))
                    .vendorName(trimOrNull(reqVO.getVendorName()))
                    .clientId(reqVO.getClientId().trim())
                    .clientSecretEnc(secretCrypto.encrypt(secret))
                    .clientSecretMask(XqSecretCrypto.mask(secret))
                    .sandbox(Boolean.TRUE.equals(reqVO.getSandbox()))
                    .baseUrl(trimOrNull(reqVO.getBaseUrl()))
                    .priceRole(priceRole)
                    .enableScheduledSync(Boolean.TRUE.equals(reqVO.getEnableScheduledSync()))
                    .syncDedupeMode(dedupe)
                    .isDefault(Boolean.TRUE.equals(reqVO.getIsDefault()))
                    .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                    .remark(trimOrNull(reqVO.getRemark()))
                    .build();
            credentialMapper.insert(row);
            return row.getId();
        }
        XqGigaApiCredentialDO existing = credentialMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(XQ_GIGA_CREDENTIAL_NOT_EXISTS);
        }
        XqGigaApiCredentialDO update = new XqGigaApiCredentialDO();
        update.setId(existing.getId());
        update.setName(reqVO.getName().trim());
        update.setVendorCode(StrUtil.blankToDefault(vendorCode, null));
        update.setVendorName(trimOrNull(reqVO.getVendorName()));
        update.setClientId(reqVO.getClientId().trim());
        if (StrUtil.isNotBlank(secret)) {
            update.setClientSecretEnc(secretCrypto.encrypt(secret));
            update.setClientSecretMask(XqSecretCrypto.mask(secret));
        }
        update.setSandbox(Boolean.TRUE.equals(reqVO.getSandbox()));
        update.setBaseUrl(trimOrNull(reqVO.getBaseUrl()));
        update.setPriceRole(priceRole);
        update.setEnableScheduledSync(Boolean.TRUE.equals(reqVO.getEnableScheduledSync()));
        update.setSyncDedupeMode(dedupe);
        update.setIsDefault(Boolean.TRUE.equals(reqVO.getIsDefault()));
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(trimOrNull(reqVO.getRemark()));
        credentialMapper.updateById(update);
        return existing.getId();
    }

    @Override
    public void delete(Long id) {
        if (credentialMapper.selectById(id) == null) {
            throw exception(XQ_GIGA_CREDENTIAL_NOT_EXISTS);
        }
        credentialMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        XqGigaApiCredentialDO existing = credentialMapper.selectById(id);
        if (existing == null) {
            throw exception(XQ_GIGA_CREDENTIAL_NOT_EXISTS);
        }
        credentialMapper.clearDefault(existing.getVendorCode(), existing.getPriceRole());
        XqGigaApiCredentialDO update = new XqGigaApiCredentialDO();
        update.setId(id);
        update.setIsDefault(true);
        update.setEnabled(true);
        credentialMapper.updateById(update);
    }

    @Override
    public XqGigaApiCredentialDO getDefaultDecrypted() {
        List<XqGigaApiCredentialDO> list = credentialMapper.selectListAll();
        XqGigaApiCredentialDO row = list.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()) && Boolean.TRUE.equals(r.getIsDefault()))
                .findFirst()
                .orElse(list.stream().filter(r -> Boolean.TRUE.equals(r.getEnabled())).findFirst().orElse(null));
        if (row == null) {
            return null;
        }
        String plain = secretCrypto.decrypt(row.getClientSecretEnc());
        row.setClientSecretEnc(plain);
        return row;
    }

    private static String normalizePriceRole(String raw) {
        String role = StrUtil.blankToDefault(raw, "").trim().toLowerCase(Locale.ROOT);
        if (XqGigaApiCredentialDO.PRICE_ROLE_PICKUP.equals(role)
                || XqGigaApiCredentialDO.PRICE_ROLE_DROPSHIP.equals(role)) {
            return role;
        }
        throw exception(XQ_GIGA_CREDENTIAL_PRICE_ROLE_INVALID);
    }

    private static String normalizeDedupe(String raw) {
        String mode = StrUtil.blankToDefault(raw, XqGigaApiCredentialDO.SYNC_DEDUPE_SKIP)
                .trim().toLowerCase(Locale.ROOT);
        if (XqGigaApiCredentialDO.SYNC_DEDUPE_REFRESH.equals(mode)) {
            return mode;
        }
        return XqGigaApiCredentialDO.SYNC_DEDUPE_SKIP;
    }

    private static String trimOrNull(String value) {
        String v = StrUtil.trim(value);
        return StrUtil.isBlank(v) ? null : v;
    }

}
