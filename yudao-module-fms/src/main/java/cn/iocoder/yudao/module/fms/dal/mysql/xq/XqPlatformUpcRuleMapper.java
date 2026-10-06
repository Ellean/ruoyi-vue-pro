package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformUpcRuleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqPlatformUpcRuleMapper extends BaseMapperX<XqPlatformUpcRuleDO> {

    default List<XqPlatformUpcRuleDO> selectListByPlatform(String platformId) {
        return selectList(new LambdaQueryWrapperX<XqPlatformUpcRuleDO>()
                .eqIfPresent(XqPlatformUpcRuleDO::getPlatformId, platformId)
                .orderByDesc(XqPlatformUpcRuleDO::getUpdateDate)
                .orderByAsc(XqPlatformUpcRuleDO::getId));
    }

    default XqPlatformUpcRuleDO selectPlatformWide(String platformId, String excludeId) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformUpcRuleDO>()
                .eq(XqPlatformUpcRuleDO::getPlatformId, platformId)
                .and(w -> w.isNull(XqPlatformUpcRuleDO::getShopId).or().eq(XqPlatformUpcRuleDO::getShopId, ""))
                .ne(excludeId != null && !excludeId.isEmpty(), XqPlatformUpcRuleDO::getId, excludeId)
                .last("LIMIT 1"));
    }

}
