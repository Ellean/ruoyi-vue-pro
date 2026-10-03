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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_CREDENTIAL_NOT_EXISTS;
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
        if (Boolean.TRUE.equals(reqVO.getIsDefault())) {
            credentialMapper.clearDefault();
        }
        if (create) {
            XqGigaApiCredentialDO row = XqGigaApiCredentialDO.builder()
                    .name(reqVO.getName().trim())
                    .clientId(reqVO.getClientId().trim())
                    .clientSecretEnc(secretCrypto.encrypt(secret))
                    .clientSecretMask(XqSecretCrypto.mask(secret))
                    .sandbox(Boolean.TRUE.equals(reqVO.getSandbox()))
                    .baseUrl(StrUtil.trim(reqVO.getBaseUrl()))
                    .isDefault(Boolean.TRUE.equals(reqVO.getIsDefault()))
                    .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                    .remark(StrUtil.trim(reqVO.getRemark()))
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
        update.setClientId(reqVO.getClientId().trim());
        if (StrUtil.isNotBlank(secret)) {
            update.setClientSecretEnc(secretCrypto.encrypt(secret));
            update.setClientSecretMask(XqSecretCrypto.mask(secret));
        }
        update.setSandbox(Boolean.TRUE.equals(reqVO.getSandbox()));
        update.setBaseUrl(StrUtil.trim(reqVO.getBaseUrl()));
        update.setIsDefault(Boolean.TRUE.equals(reqVO.getIsDefault()));
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(StrUtil.trim(reqVO.getRemark()));
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
        credentialMapper.clearDefault();
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
        // 解密结果暂存到 remark 不安全；调用方应使用 decrypt API
        // 这里返回 DO，额外把明文放在 clientSecretMask 临时覆盖不合适
        // 提供 getPlainSecret 方法更好 — 见下方通过重新设 enc 旁路
        String plain = secretCrypto.decrypt(row.getClientSecretEnc());
        row.setClientSecretEnc(plain); // 调用约定：返回时 enc 字段为明文，仅内部使用
        return row;
    }

}
