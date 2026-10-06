package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqUpcPoolDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqUpcPoolMapper extends BaseMapperX<XqUpcPoolDO> {

    String GLOBAL_PLATFORM = "__global__";

    default List<XqUpcPoolDO> selectExisting(Collection<String> upcs) {
        if (upcs == null || upcs.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqUpcPoolDO>()
                .in(XqUpcPoolDO::getUpc, upcs)
                .select(XqUpcPoolDO::getUpc));
    }

    default IPage<XqUpcPoolDO> selectPage(IPage<XqUpcPoolDO> page, String poolType, String status) {
        return selectPage(page, new LambdaQueryWrapperX<XqUpcPoolDO>()
                .eq(XqUpcPoolDO::getPlatformId, GLOBAL_PLATFORM)
                .eqIfPresent(XqUpcPoolDO::getPoolType, poolType)
                .eqIfPresent(XqUpcPoolDO::getStatus, status)
                .orderByDesc(XqUpcPoolDO::getCreateDate)
                .orderByAsc(XqUpcPoolDO::getId));
    }

    @Select("SELECT pool_type AS poolType, status, COUNT(*) AS cnt FROM t_giga_upc_pool "
            + "WHERE platform_id = '__global__' GROUP BY pool_type, status")
    List<java.util.Map<String, Object>> selectGlobalStats();

}
