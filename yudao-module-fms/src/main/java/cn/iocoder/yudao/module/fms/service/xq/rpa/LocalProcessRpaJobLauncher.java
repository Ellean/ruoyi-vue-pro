package cn.iocoder.yudao.module.fms.service.xq.rpa;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_TRIGGER_FAIL;

/**
 * 自写脚本 / exe 扩展点（由 {@link XqRpaLauncherConfiguration} 注册 script、exe 两个 Bean）。
 * <p>
 * 当前不在 API 服务器上 ProcessBuilder 拉起进程，而是签发 local workUuid，
 * 把启动意图写入日志与响应，供本机 Agent 认领。接入守护进程时只改本类即可。
 */
@Slf4j
public class LocalProcessRpaJobLauncher implements XqRpaJobLauncher {

    public static final String MODE_SCRIPT = "script";
    public static final String MODE_EXE = "exe";

    private final String boundMode;

    public LocalProcessRpaJobLauncher(String boundMode) {
        this.boundMode = StrUtil.blankToDefault(boundMode, MODE_SCRIPT);
    }

    @Override
    public String mode() {
        return boundMode;
    }

    @Override
    public XqRpaTriggerRespVO launch(XqRpaUserConfigDO userCfg, String jobKind, String jobUuid,
                                     Map<String, Object> input) {
        String path = StrUtil.trim(userCfg == null ? null : userCfg.getRunnerPath());
        if (StrUtil.isBlank(path)) {
            throw exception(XQ_RPA_TRIGGER_FAIL,
                    "本地执行未配置路径：请在 RPA 配置填写脚本/exe 路径（runnerPath）");
        }
        String workUuid = "local-" + IdUtil.fastSimpleUUID();
        String args = expandArgs(StrUtil.blankToDefault(userCfg.getRunnerArgs(), ""),
                jobKind, workUuid, jobUuid);
        String workdir = StrUtil.trim(userCfg.getRunnerWorkdir());

        // 扩展点：日后改为 ProcessBuilder / Redis 队列 / 本机 Agent HTTP
        log.info("[rpa-local-launcher] queued mode={} path={} args={} workdir={} jobKind={} workUuid={} inputKeys={}",
                boundMode, path, args, workdir, jobKind, workUuid,
                input == null ? 0 : input.keySet());

        XqRpaTriggerRespVO resp = new XqRpaTriggerRespVO();
        resp.setJobKind(jobKind);
        resp.setJobUuid(StrUtil.blankToDefault(jobUuid, path));
        resp.setWorkUuid(workUuid);
        resp.setInputParamSent(true);
        resp.setRunnerMode(boundMode);
        resp.setRunnerPath(path);
        resp.setRunnerCommandPreview(StrUtil.isBlank(args) ? path : path + " " + args);
        resp.setMessage("已登记本地执行任务，等待本机 Agent 拉取（扩展点已预留，切换 runnerMode 即可）");
        return resp;
    }

    private static String expandArgs(String template, String jobKind, String workUuid, String jobUuid) {
        return template
                .replace("{jobKind}", StrUtil.blankToDefault(jobKind, ""))
                .replace("{workUuid}", StrUtil.blankToDefault(workUuid, ""))
                .replace("{jobUuid}", StrUtil.blankToDefault(jobUuid, ""));
    }

}
