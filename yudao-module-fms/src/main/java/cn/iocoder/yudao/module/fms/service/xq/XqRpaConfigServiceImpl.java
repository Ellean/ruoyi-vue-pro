package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaUserConfigMapper;
import cn.iocoder.yudao.module.fms.service.xq.rpa.CommanderRpaJobLauncher;
import cn.iocoder.yudao.module.fms.service.xq.rpa.XqRpaJobLauncher;
import cn.iocoder.yudao.module.fms.service.xq.rpa.XqRpaJobLauncherRegistry;
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
    private XqRpaJobLauncherRegistry jobLauncherRegistry;

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
            vo.setRunnerMode(CommanderRpaJobLauncher.MODE);
            return vo;
        }
        vo.setImageJobUuid(row.getImageJobUuid());
        vo.setCopyJobUuid(row.getCopyJobUuid());
        vo.setErpSiteUrl(row.getErpSiteUrl());
        vo.setAccount(row.getAccount());
        vo.setHasPassword(StrUtil.isNotBlank(row.getPassword()));
        vo.setRunnerMode(XqRpaJobLauncherRegistry.normalizeMode(row.getRunnerMode()));
        vo.setRunnerPath(row.getRunnerPath());
        vo.setRunnerArgs(row.getRunnerArgs());
        vo.setRunnerWorkdir(row.getRunnerWorkdir());
        return vo;
    }

    @Override
    public void saveConfig(Long userId, XqRpaConfigSaveReqVO reqVO) {
        String runnerMode = XqRpaJobLauncherRegistry.normalizeMode(
                reqVO == null ? null : reqVO.getRunnerMode());
        XqRpaUserConfigDO existing = rpaUserConfigMapper.selectByUserId(userId);
        if (existing == null) {
            XqRpaUserConfigDO insert = XqRpaUserConfigDO.builder()
                    .userId(userId)
                    .imageJobUuid(trimOrNull(reqVO.getImageJobUuid()))
                    .copyJobUuid(trimOrNull(reqVO.getCopyJobUuid()))
                    .erpSiteUrl(trimOrNull(reqVO.getErpSiteUrl()))
                    .account(trimOrNull(reqVO.getAccount()))
                    .password(trimOrNull(reqVO.getPassword()))
                    .runnerMode(runnerMode)
                    .runnerPath(trimOrNull(reqVO.getRunnerPath()))
                    .runnerArgs(trimOrNull(reqVO.getRunnerArgs()))
                    .runnerWorkdir(trimOrNull(reqVO.getRunnerWorkdir()))
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
        update.setRunnerMode(runnerMode);
        update.setRunnerPath(trimOrNull(reqVO.getRunnerPath()));
        update.setRunnerArgs(trimOrNull(reqVO.getRunnerArgs()));
        update.setRunnerWorkdir(trimOrNull(reqVO.getRunnerWorkdir()));
        rpaUserConfigMapper.updateById(update);
    }

    @Override
    public XqRpaTriggerRespVO trigger(Long userId, XqRpaTriggerReqVO reqVO) {
        XqRpaUserConfigDO row = rpaUserConfigMapper.selectByUserId(userId);
        if (row == null) {
            throw exception(XQ_RPA_CONFIG_NOT_EXISTS);
        }
        String kind = StrUtil.blankToDefault(reqVO.getJobKind(), "image").trim().toLowerCase();
        String jobUuid = "copy".equals(kind) ? row.getCopyJobUuid() : row.getImageJobUuid();
        String mode = XqRpaJobLauncherRegistry.normalizeMode(row.getRunnerMode());
        // 控制中枢仍要求 UUID；本地脚本/exe 可用路径代替
        if (CommanderRpaJobLauncher.MODE.equals(mode) && StrUtil.isBlank(jobUuid)) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        if (!CommanderRpaJobLauncher.MODE.equals(mode) && StrUtil.isBlank(row.getRunnerPath())) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "请先填写本地脚本/exe 路径");
        }
        return launch(userId, kind, StrUtil.blankToDefault(jobUuid, row.getRunnerPath()), buildInputParam(row));
    }

    /**
     * 统一触发入口（文案流水线等可复用）：按个人 runnerMode 分发。
     */
    @Override
    public XqRpaTriggerRespVO launch(Long userId, String jobKind, String jobUuid, Map<String, Object> input) {
        XqRpaUserConfigDO row = rpaUserConfigMapper.selectByUserId(userId);
        if (row == null) {
            throw exception(XQ_RPA_CONFIG_NOT_EXISTS);
        }
        String mode = XqRpaJobLauncherRegistry.normalizeMode(row.getRunnerMode());
        XqRpaJobLauncher launcher = jobLauncherRegistry.require(mode);
        Map<String, Object> payload = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
        // 便于本机 Agent 识别通道
        payload.putIfAbsent("runnerMode", mode);
        if (StrUtil.isNotBlank(row.getRunnerPath())) {
            payload.putIfAbsent("runnerPath", row.getRunnerPath().trim());
        }
        return launcher.launch(row, jobKind, jobUuid, payload);
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
        // 与工作台同租户登录；RPA 流程勿再写死 tenant-id:162
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            String tid = String.valueOf(tenantId);
            param.put("租户", tid);
            param.put("tenantId", tid);
        }
        return param;
    }

    private static String trimOrNull(String value) {
        String v = StrUtil.trim(value);
        return StrUtil.isBlank(v) ? null : v;
    }

}
