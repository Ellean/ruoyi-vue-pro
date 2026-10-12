package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformSkuRuleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
@DS("xq")
public interface XqPlatformSkuRuleMapper extends BaseMapperX<XqPlatformSkuRuleDO> {

    default List<XqPlatformSkuRuleDO> selectListByPlatform(String platformId, Collection<String> shopIds) {
        return selectList(new LambdaQueryWrapperX<XqPlatformSkuRuleDO>()
                .eqIfPresent(XqPlatformSkuRuleDO::getPlatformId, platformId)
                .inIfPresent(XqPlatformSkuRuleDO::getShopId, shopIds)
                .orderByDesc(XqPlatformSkuRuleDO::getUpdateDate)
                .orderByAsc(XqPlatformSkuRuleDO::getId));
    }

    default XqPlatformSkuRuleDO selectDuplicate(String platformId, String shopId, String ownerUserId, String excludeId) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformSkuRuleDO>()
                .eq(XqPlatformSkuRuleDO::getPlatformId, platformId)
                .eq(XqPlatformSkuRuleDO::getShopId, shopId)
                .eq(XqPlatformSkuRuleDO::getOwnerUserId, ownerUserId == null ? "" : ownerUserId)
                .ne(excludeId != null && !excludeId.isEmpty(), XqPlatformSkuRuleDO::getId, excludeId)
                .last("LIMIT 1"));
    }

    /** 同店铺取消其它默认（保留 excludeId） */
    default void clearDefaultByShop(String platformId, String shopId, String excludeId) {
        List<XqPlatformSkuRuleDO> rows = selectList(new LambdaQueryWrapperX<XqPlatformSkuRuleDO>()
                .eq(XqPlatformSkuRuleDO::getPlatformId, platformId)
                .eq(XqPlatformSkuRuleDO::getShopId, shopId)
                .eq(XqPlatformSkuRuleDO::getIsDefault, true)
                .ne(excludeId != null && !excludeId.isEmpty(), XqPlatformSkuRuleDO::getId, excludeId));
        for (XqPlatformSkuRuleDO row : rows) {
            row.setIsDefault(false);
            updateById(row);
        }
    }

}
