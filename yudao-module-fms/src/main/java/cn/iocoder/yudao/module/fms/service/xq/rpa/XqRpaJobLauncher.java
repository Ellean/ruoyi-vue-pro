package cn.iocoder.yudao.module.fms.service.xq.rpa;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;

import java.util.Map;

/**
 * RPA 任务启动扩展点。
 * <p>
 * 实现类按 {@link #mode()} 注册；切换个人配置 runnerMode 即可换通道，业务入参不变。
 * <ul>
 *   <li>commander — 影刀 / 控制中枢 API（当前默认）</li>
 *   <li>script — 自写脚本（由本机 Agent 拉起，或后续接进程启动）</li>
 *   <li>exe — 本地 exe</li>
 * </ul>
 */
public interface XqRpaJobLauncher {

    /** 与 xq_rpa_user_config.runner_mode 一致 */
    String mode();

    /**
     * @param userCfg 个人配置（含 runnerPath / args）
     * @param jobKind copy | image
     * @param jobUuid 控制中枢任务 UUID；本地模式可作逻辑标识
     * @param input   下发给机器人的入参
     */
    XqRpaTriggerRespVO launch(XqRpaUserConfigDO userCfg, String jobKind, String jobUuid,
                              Map<String, Object> input);

}
