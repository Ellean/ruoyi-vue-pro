package cn.iocoder.yudao.module.fms.service.xq.rpa;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaGlobalConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.service.xq.XqCommanderClient;
import cn.iocoder.yudao.module.fms.service.xq.XqRpaGlobalConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_TRIGGER_FAIL;

/** 影刀控制中枢（默认）。 */
@Component
public class CommanderRpaJobLauncher implements XqRpaJobLauncher {

    public static final String MODE = "commander";

    @Resource
    private XqRpaGlobalConfigService rpaGlobalConfigService;
    @Resource
    private XqCommanderClient commanderClient;

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public XqRpaTriggerRespVO launch(XqRpaUserConfigDO userCfg, String jobKind, String jobUuid,
                                     Map<String, Object> input) {
        XqRpaGlobalConfigDO global = rpaGlobalConfigService.requireConfigured();
        try {
            Map<String, Object> result = commanderClient.triggerJob(
                    global.getBaseUrl(), global.getAppKey(), global.getAppSecret(),
                    jobUuid.trim(), input);
            XqRpaTriggerRespVO resp = new XqRpaTriggerRespVO();
            resp.setJobKind(jobKind);
            resp.setJobUuid(jobUuid.trim());
            resp.setWorkUuid((String) result.get("workUuid"));
            resp.setInputParamSent(Boolean.TRUE.equals(result.get("inputParamSent")));
            resp.setRunnerMode(MODE);
            return resp;
        } catch (Exception ex) {
            throw exception(XQ_RPA_TRIGGER_FAIL, StrUtil.blankToDefault(ex.getMessage(), "未知错误"));
        }
    }

}
