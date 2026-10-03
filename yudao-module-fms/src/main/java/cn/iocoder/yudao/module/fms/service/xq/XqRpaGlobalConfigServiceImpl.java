package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaGlobalConfigDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaGlobalConfigMapper;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_CONFIG_INVALID;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_GLOBAL_NOT_CONFIGURED;

@Service
@Validated
public class XqRpaGlobalConfigServiceImpl implements XqRpaGlobalConfigService {

    private static final String DEFAULT_BASE = "https://z-commander-api.ai-indeed.com";

    @Resource
    private XqRpaGlobalConfigMapper globalConfigMapper;

    @Override
    public XqRpaGlobalConfigRespVO get() {
        XqRpaGlobalConfigDO row = getOrInit();
        XqRpaGlobalConfigRespVO vo = new XqRpaGlobalConfigRespVO();
        boolean ok = StrUtil.isAllNotBlank(row.getAppKey(), row.getAppSecret(), row.getBaseUrl());
        vo.setConfigured(ok);
        vo.setBaseUrl(StrUtil.blankToDefault(row.getBaseUrl(), DEFAULT_BASE));
        vo.setAppKeyMasked(XqSecretCrypto.mask(row.getAppKey()));
        vo.setHasAppSecret(StrUtil.isNotBlank(row.getAppSecret()));
        vo.setRemark(row.getRemark());
        return vo;
    }

    @Override
    public void save(XqRpaGlobalConfigSaveReqVO reqVO) {
        XqRpaGlobalConfigDO row = getOrInit();
        String appKey = StrUtil.trim(reqVO.getAppKey());
        String appSecret = StrUtil.trim(reqVO.getAppSecret());
        if (StrUtil.isBlank(row.getAppKey()) && StrUtil.isBlank(appKey)) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        if (StrUtil.isBlank(row.getAppSecret()) && StrUtil.isBlank(appSecret)) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        XqRpaGlobalConfigDO update = new XqRpaGlobalConfigDO();
        update.setId(row.getId());
        update.setBaseUrl(StrUtil.trim(reqVO.getBaseUrl()));
        if (StrUtil.isNotBlank(appKey)) {
            update.setAppKey(appKey);
        }
        if (StrUtil.isNotBlank(appSecret)) {
            update.setAppSecret(appSecret);
        }
        update.setRemark(StrUtil.trim(reqVO.getRemark()));
        globalConfigMapper.updateById(update);
    }

    @Override
    public XqRpaGlobalConfigDO requireConfigured() {
        XqRpaGlobalConfigDO row = getOrInit();
        if (StrUtil.hasBlank(row.getAppKey(), row.getAppSecret(), row.getBaseUrl())) {
            throw exception(XQ_RPA_GLOBAL_NOT_CONFIGURED);
        }
        return row;
    }

    private XqRpaGlobalConfigDO getOrInit() {
        XqRpaGlobalConfigDO row = globalConfigMapper.selectById(XqRpaGlobalConfigDO.SINGLETON_ID);
        if (row != null) {
            return row;
        }
        XqRpaGlobalConfigDO init = XqRpaGlobalConfigDO.builder()
                .id(XqRpaGlobalConfigDO.SINGLETON_ID)
                .baseUrl(DEFAULT_BASE)
                .build();
        globalConfigMapper.insert(init);
        return init;
    }

}
