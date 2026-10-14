package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqAiUsageLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface XqAiUsageLogMapper extends BaseMapperX<XqAiUsageLogDO> {

    default List<XqAiUsageLogDO> selectByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<XqAiUsageLogDO>()
                .in(XqAiUsageLogDO::getWorkOrderId, workOrderIds)
                .orderByDesc(XqAiUsageLogDO::getId));
    }
}
