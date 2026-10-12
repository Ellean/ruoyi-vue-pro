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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

@Service
@Validated
public class XqRpaConfigServiceImpl implements XqRpaConfigService {

    @Value("${xq.rpa.callback-base-url:http://127.0.0.1:48080/admin-api}")
    private String callbackBaseUrl;

    @Resource
    private XqRpaUserConfigMapper rpaUserConfigMapper;
    @Resource
    private XqRpaGlobalConfigService rpaGlobalConfigService;
    @Resource
    private XqRpaJobLauncherRegistry jobLauncherRegistry;
    @Resource
    private AdminUserApi adminUserApi;

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
        // 账号/站点不再人工配置：账号走登录用户，站点走回调地址
        vo.setErpSiteUrl(null);
        vo.setAccount(null);
        vo.setHasPassword(StrUtil.isNotBlank(row.getPassword()));
        vo.setRunnerMode(XqRpaJobLauncherRegistry.normalizeMode(row.getRunnerMode()));
        vo.setRunnerPath(row.getRunnerPath());
        vo.setRunnerArgs(row.getRunnerArgs());
        vo.setRunnerWorkdir(row.getRunnerWorkdir());
        return vo;
    }

    @Override
    public void saveConfig(Long userId, XqRpaConfigSaveReqVO reqVO) {
        // 个人配置只需：文案/生图流程 UUID + 密码（账号由接口注入）
        String runnerMode = XqRpaJobLauncherRegistry.normalizeMode(
                reqVO == null ? null : reqVO.getRunnerMode());
        XqRpaUserConfigDO existing = rpaUserConfigMapper.selectByUserId(userId);
        if (existing == null) {
            XqRpaUserConfigDO insert = XqRpaUserConfigDO.builder()
                    .userId(userId)
                    .imageJobUuid(trimOrNull(reqVO.getImageJobUuid()))
                    .copyJobUuid(trimOrNull(reqVO.getCopyJobUuid()))
                    .erpSiteUrl(null)
                    .account(null)
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
        update.setErpSiteUrl(null);
        update.setAccount(null);
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
        return launch(userId, kind, StrUtil.blankToDefault(jobUuid, row.getRunnerPath()),
                buildInputParam(userId, row));
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
        // 补齐账号/密码/站点（调用方未显式传入时）
        Map<String, Object> defaults = buildInputParam(userId, row);
        defaults.forEach(payload::putIfAbsent);
        return launcher.launch(row, jobKind, jobUuid, payload);
    }

    private Map<String, Object> buildInputParam(Long userId, XqRpaUserConfigDO row) {
        Map<String, Object> param = new LinkedHashMap<>();
        String base = StrUtil.removeSuffix(StrUtil.blankToDefault(callbackBaseUrl, ""), "/");
        if (StrUtil.isNotBlank(base)) {
            param.put("http", base);
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null && StrUtil.isNotBlank(user.getUsername())) {
                param.put("账户", user.getUsername().trim());
            }
        } catch (Exception ignored) {
            // ignore
        }
        if (row != null && StrUtil.isNotBlank(row.getPassword())) {
            param.put("密码", row.getPassword().trim());
        }
        // 与工作台同租户登录；RPA 流程勿再写死 tenant-id
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
