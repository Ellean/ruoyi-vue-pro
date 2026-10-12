package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingValuesListDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface XqListingValuesListMapper extends BaseMapperX<XqListingValuesListDO> {

    default List<XqListingValuesListDO> selectByPlatformIdsAndCodes(
            Collection<String> platformIds, Collection<String> listCodes) {
        if (platformIds == null || platformIds.isEmpty()
                || listCodes == null || listCodes.isEmpty()) {
            return List.of();
        }
        List<String> pids = platformIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        List<String> codes = listCodes.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        if (pids.isEmpty() || codes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqListingValuesListDO>()
                .in(XqListingValuesListDO::getPlatformId, pids)
                .in(XqListingValuesListDO::getListCode, codes));
    }

}
