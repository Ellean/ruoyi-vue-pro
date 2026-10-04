package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqListingPlatformMapper extends BaseMapperX<XqListingPlatformDO> {

    default List<XqListingPlatformDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<XqListingPlatformDO>()
                .eq(XqListingPlatformDO::getEnabled, true)
                .orderByAsc(XqListingPlatformDO::getSortOrder)
                .orderByAsc(XqListingPlatformDO::getName));
    }

    default List<XqListingPlatformDO> selectAllList() {
        return selectList(new LambdaQueryWrapperX<XqListingPlatformDO>()
                .orderByAsc(XqListingPlatformDO::getSortOrder)
                .orderByAsc(XqListingPlatformDO::getName));
    }

}
