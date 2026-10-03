package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;

public interface XqRpaConfigService {

    XqRpaConfigRespVO getConfig(Long userId);

    void saveConfig(Long userId, XqRpaConfigSaveReqVO reqVO);

    XqRpaTriggerRespVO trigger(Long userId, XqRpaTriggerReqVO reqVO);

}
