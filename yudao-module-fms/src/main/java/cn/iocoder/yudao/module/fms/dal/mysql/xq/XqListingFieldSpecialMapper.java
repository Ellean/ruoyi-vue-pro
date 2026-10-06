package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingFieldSpecialDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqListingFieldSpecialMapper extends BaseMapperX<XqListingFieldSpecialDO> {

    default List<XqListingFieldSpecialDO> selectByPlatformIds(Collection<String> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqListingFieldSpecialDO>()
                .in(XqListingFieldSpecialDO::getPlatformId, platformIds));
    }

}
