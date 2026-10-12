package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface XqListingCredentialService {

    List<XqListingCredentialRespVO> list();

    Long save(@Valid XqListingCredentialSaveReqVO reqVO);

    void delete(Long id);

    /** 从原库 t_giga_listing_platform 同步 Mirakl/Wayfair 密钥到主库 */
    Map<String, Object> syncFromXq();

    XqListingPlatformCredentialDO getDecryptedForPlatform(Long platformId, String provider);

    XqListingPlatformCredentialDO getDecryptedBySource(String sourcePlatformId, String provider);
}
