package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingShopFieldConfigDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("xq")
public interface XqListingShopFieldConfigMapper extends BaseMapperX<XqListingShopFieldConfigDO> {

    default XqListingShopFieldConfigDO selectByScope(String platformId, String shopId, String country) {
        return selectOne(new LambdaQueryWrapperX<XqListingShopFieldConfigDO>()
                .eq(XqListingShopFieldConfigDO::getPlatformId, platformId == null ? "" : platformId)
                .eq(XqListingShopFieldConfigDO::getShopId, shopId == null ? "" : shopId)
                .eq(XqListingShopFieldConfigDO::getCountry, country == null ? "" : country)
                .last("LIMIT 1"));
    }

}
