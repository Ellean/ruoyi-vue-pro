package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqListingPlatformCredentialMapper extends BaseMapperX<XqListingPlatformCredentialDO> {

    default List<XqListingPlatformCredentialDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqListingPlatformCredentialDO>()
                .orderByAsc(XqListingPlatformCredentialDO::getProvider)
                .orderByAsc(XqListingPlatformCredentialDO::getPlatformCode)
                .orderByDesc(XqListingPlatformCredentialDO::getId));
    }

    default XqListingPlatformCredentialDO selectByProviderAndSource(String provider, String sourcePlatformId) {
        if (provider == null || sourcePlatformId == null || sourcePlatformId.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingPlatformCredentialDO>()
                .eq(XqListingPlatformCredentialDO::getProvider, provider)
                .eq(XqListingPlatformCredentialDO::getSourcePlatformId, sourcePlatformId.trim())
                .last("LIMIT 1"));
    }

    default XqListingPlatformCredentialDO selectEnabledByPlatformAndProvider(Long platformId, String provider) {
        if (platformId == null || provider == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingPlatformCredentialDO>()
                .eq(XqListingPlatformCredentialDO::getPlatformId, platformId)
                .eq(XqListingPlatformCredentialDO::getProvider, provider)
                .eq(XqListingPlatformCredentialDO::getEnabled, true)
                .last("LIMIT 1"));
    }

    default XqListingPlatformCredentialDO selectEnabledByCodeAndProvider(String platformCode, String provider) {
        if (platformCode == null || platformCode.isBlank() || provider == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingPlatformCredentialDO>()
                .eq(XqListingPlatformCredentialDO::getPlatformCode, platformCode.trim())
                .eq(XqListingPlatformCredentialDO::getProvider, provider)
                .eq(XqListingPlatformCredentialDO::getEnabled, true)
                .last("LIMIT 1"));
    }

}
