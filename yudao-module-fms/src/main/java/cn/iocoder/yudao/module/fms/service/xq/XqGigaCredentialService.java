package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaApiCredentialDO;

import java.util.List;

public interface XqGigaCredentialService {

    List<XqGigaCredentialRespVO> list();

    Long save(XqGigaCredentialSaveReqVO reqVO);

    void delete(Long id);

    void setDefault(Long id);

    /** 供后续同步选品库调用；返回解密后的 DO（含 clientSecretEnc 已解密到临时字段需调用方注意） */
    XqGigaApiCredentialDO getDefaultDecrypted();

}
