package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqListingCategoryMapper extends BaseMapperX<XqListingCategoryDO> {

    default List<XqListingCategoryDO> selectEnabledByPlatformId(String platformId) {
        return selectList(new LambdaQueryWrapperX<XqListingCategoryDO>()
                .eq(XqListingCategoryDO::getPlatformId, platformId)
                .eq(XqListingCategoryDO::getEnabled, true)
                .orderByAsc(XqListingCategoryDO::getSortOrder)
                .orderByAsc(XqListingCategoryDO::getName));
    }

}
