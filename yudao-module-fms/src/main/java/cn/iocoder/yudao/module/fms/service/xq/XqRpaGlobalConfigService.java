package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaGlobalConfigDO;

public interface XqRpaGlobalConfigService {

    XqRpaGlobalConfigRespVO get();

    void save(XqRpaGlobalConfigSaveReqVO reqVO);

    /** 供触发 RPA 使用；未配置抛业务异常 */
    XqRpaGlobalConfigDO requireConfigured();

}
