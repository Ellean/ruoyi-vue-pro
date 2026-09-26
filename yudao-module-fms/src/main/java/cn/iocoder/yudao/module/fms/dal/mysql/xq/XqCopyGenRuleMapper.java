package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqCopyGenRuleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqCopyGenRuleMapper extends BaseMapperX<XqCopyGenRuleDO> {

    default List<XqCopyGenRuleDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<XqCopyGenRuleDO>()
                .orderByAsc(XqCopyGenRuleDO::getPlatformId));
    }

    default XqCopyGenRuleDO selectByPlatformId(String platformId) {
        String pid = platformId == null ? "" : platformId;
        return selectOne(new LambdaQueryWrapperX<XqCopyGenRuleDO>()
                .eq(XqCopyGenRuleDO::getPlatformId, pid)
                .last("LIMIT 1"));
    }

}
