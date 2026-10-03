package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaGlobalConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaUserConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

@Service
@Validated
public class XqRpaConfigServiceImpl implements XqRpaConfigService {

    @Resource
    private XqRpaUserConfigMapper rpaUserConfigMapper;
    @Resource
    private XqRpaGlobalConfigService rpaGlobalConfigService;
    @Resource
    private XqCommanderClient commanderClient;

    @Override
    public XqRpaConfigRespVO getConfig(Long userId) {
        var global = rpaGlobalConfigService.get();
        XqRpaUserConfigDO row = rpaUserConfigMapper.selectByUserId(userId);
        XqRpaConfigRespVO vo = new XqRpaConfigRespVO();
        vo.setGlobalConfigured(Boolean.TRUE.equals(global.getConfigured()));
        vo.setGlobalBaseUrl(global.getBaseUrl());
        vo.setGlobalAppKeyMasked(global.getAppKeyMasked());
        if (row == null) {
            vo.setHasPassword(false);
            return vo;
        }
        vo.setImageJobUuid(row.getImageJobUuid());
        vo.setCopyJobUuid(row.getCopyJobUuid());
        vo.setErpSiteUrl(row.getErpSiteUrl());
        vo.setAccount(row.getAccount());
        vo.setHasPassword(StrUtil.isNotBlank(row.getPassword()));
        return vo;
    }

    @Override
    public void saveConfig(Long userId, XqRpaConfigSaveReqVO reqVO) {
        XqRpaUserConfigDO existing = rpaUserConfigMapper.selectByUserId(userId);
        if (existing == null) {
            XqRpaUserConfigDO insert = XqRpaUserConfigDO.builder()
                    .userId(userId)
                    .imageJobUuid(trimOrNull(reqVO.getImageJobUuid()))
                    .copyJobUuid(trimOrNull(reqVO.getCopyJobUuid()))
                    .erpSiteUrl(trimOrNull(reqVO.getErpSiteUrl()))
                    .account(trimOrNull(reqVO.getAccount()))
                    .password(trimOrNull(reqVO.getPassword()))
                    .build();
            rpaUserConfigMapper.insert(insert);
            return;
        }
        XqRpaUserConfigDO update = new XqRpaUserConfigDO();
        update.setId(existing.getId());
        update.setImageJobUuid(trimOrNull(reqVO.getImageJobUuid()));
        update.setCopyJobUuid(trimOrNull(reqVO.getCopyJobUuid()));
        update.setErpSiteUrl(trimOrNull(reqVO.getErpSiteUrl()));
        update.setAccount(trimOrNull(reqVO.getAccount()));
        if (StrUtil.isNotBlank(reqVO.getPassword())) {
            update.setPassword(reqVO.getPassword().trim());
        }
        rpaUserConfigMapper.updateById(update);
    }

    @Override
    public XqRpaTriggerRespVO trigger(Long userId, XqRpaTriggerReqVO reqVO) {
        XqRpaGlobalConfigDO global = rpaGlobalConfigService.requireConfigured();
        XqRpaUserConfigDO row = rpaUserConfigMapper.selectByUserId(userId);
        if (row == null) {
            throw exception(XQ_RPA_CONFIG_NOT_EXISTS);
        }
        String kind = StrUtil.blankToDefault(reqVO.getJobKind(), "image").trim().toLowerCase();
        String jobUuid = "copy".equals(kind) ? row.getCopyJobUuid() : row.getImageJobUuid();
        if (StrUtil.isBlank(jobUuid)) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        Map<String, Object> input = buildInputParam(row);
        try {
            Map<String, Object> result = commanderClient.triggerJob(
                    global.getBaseUrl(), global.getAppKey(), global.getAppSecret(),
                    jobUuid.trim(), input);
            XqRpaTriggerRespVO resp = new XqRpaTriggerRespVO();
            resp.setJobKind(kind);
            resp.setJobUuid(jobUuid.trim());
            resp.setWorkUuid((String) result.get("workUuid"));
            resp.setInputParamSent(Boolean.TRUE.equals(result.get("inputParamSent")));
            return resp;
        } catch (Exception ex) {
            throw exception(XQ_RPA_TRIGGER_FAIL, StrUtil.blankToDefault(ex.getMessage(), "未知错误"));
        }
    }

    private static Map<String, Object> buildInputParam(XqRpaUserConfigDO row) {
        Map<String, Object> param = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(row.getErpSiteUrl())) {
            param.put("http", row.getErpSiteUrl().trim());
        }
        if (StrUtil.isNotBlank(row.getAccount())) {
            param.put("账户", row.getAccount().trim());
        }
        if (StrUtil.isNotBlank(row.getPassword())) {
            param.put("密码", row.getPassword().trim());
        }
        return param;
    }

    private static String trimOrNull(String value) {
        String v = StrUtil.trim(value);
        return StrUtil.isBlank(v) ? null : v;
    }

}
