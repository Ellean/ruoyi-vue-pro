package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldCommonDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqListingFieldCommonMapper extends BaseMapperX<XqListingFieldCommonDO> {

    default List<XqListingFieldCommonDO> selectByPlatformIds(Collection<String> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqListingFieldCommonDO>()
                .in(XqListingFieldCommonDO::getPlatformId, platformIds));
    }

}
