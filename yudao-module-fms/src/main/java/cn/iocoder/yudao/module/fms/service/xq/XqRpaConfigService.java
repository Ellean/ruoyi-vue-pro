package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;

import java.util.Map;

public interface XqRpaConfigService {

    XqRpaConfigRespVO getConfig(Long userId);

    void saveConfig(Long userId, XqRpaConfigSaveReqVO reqVO);

    XqRpaTriggerRespVO trigger(Long userId, XqRpaTriggerReqVO reqVO);

    /**
     * 按个人 runnerMode 分发（commander / script / exe），流水线复用此入口以便随时切换。
     */
    XqRpaTriggerRespVO launch(Long userId, String jobKind, String jobUuid, Map<String, Object> input);

}
