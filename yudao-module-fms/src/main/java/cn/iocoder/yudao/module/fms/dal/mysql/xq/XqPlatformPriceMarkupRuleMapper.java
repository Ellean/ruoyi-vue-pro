package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformPriceMarkupRuleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqPlatformPriceMarkupRuleMapper extends BaseMapperX<XqPlatformPriceMarkupRuleDO> {

    default List<XqPlatformPriceMarkupRuleDO> selectListByPlatform(String platformId, Collection<String> shopIds) {
        return selectList(new LambdaQueryWrapperX<XqPlatformPriceMarkupRuleDO>()
                .eqIfPresent(XqPlatformPriceMarkupRuleDO::getPlatformId, platformId)
                .inIfPresent(XqPlatformPriceMarkupRuleDO::getShopId, shopIds)
                .orderByDesc(XqPlatformPriceMarkupRuleDO::getUpdateDate)
                .orderByAsc(XqPlatformPriceMarkupRuleDO::getId));
    }

    default XqPlatformPriceMarkupRuleDO selectDuplicate(String platformId, String shopId, String ownerUserId, String excludeId) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformPriceMarkupRuleDO>()
                .eq(XqPlatformPriceMarkupRuleDO::getPlatformId, platformId)
                .eq(XqPlatformPriceMarkupRuleDO::getShopId, shopId)
                .eq(XqPlatformPriceMarkupRuleDO::getOwnerUserId, ownerUserId == null ? "" : ownerUserId)
                .ne(excludeId != null && !excludeId.isEmpty(), XqPlatformPriceMarkupRuleDO::getId, excludeId)
                .last("LIMIT 1"));
    }

}
