package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldPoolDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqListingFieldPoolMapper extends BaseMapperX<XqListingFieldPoolDO> {

    default List<XqListingFieldPoolDO> selectByPlatformIds(Collection<String> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return List.of();
        }
        List<String> ids = platformIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqListingFieldPoolDO>()
                .in(XqListingFieldPoolDO::getPlatformId, ids)
                .orderByDesc(XqListingFieldPoolDO::getRequired)
                .orderByAsc(XqListingFieldPoolDO::getLabel));
    }

}
