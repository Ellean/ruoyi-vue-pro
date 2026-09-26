package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingShopDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqListingShopMapper extends BaseMapperX<XqListingShopDO> {

    default List<XqListingShopDO> selectByPlatformId(String platformId) {
        return selectList(new LambdaQueryWrapperX<XqListingShopDO>()
                .eqIfPresent(XqListingShopDO::getPlatformId, platformId)
                .eq(XqListingShopDO::getEnabled, true)
                .orderByAsc(XqListingShopDO::getSortOrder)
                .orderByAsc(XqListingShopDO::getName));
    }

}
