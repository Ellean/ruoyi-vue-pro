package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqImageGenRuleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqImageGenRuleMapper extends BaseMapperX<XqImageGenRuleDO> {

    default List<XqImageGenRuleDO> selectByPlatformId(String platformId) {
        String pid = platformId == null ? "" : platformId;
        return selectList(new LambdaQueryWrapperX<XqImageGenRuleDO>()
                .eq(XqImageGenRuleDO::getPlatformId, pid)
                .orderByAsc(XqImageGenRuleDO::getCategoryName)
                .orderByAsc(XqImageGenRuleDO::getCategoryId));
    }

    default XqImageGenRuleDO selectByPlatformAndCategory(String platformId, String categoryId) {
        String pid = platformId == null ? "" : platformId;
        String cid = categoryId == null ? "" : categoryId;
        return selectOne(new LambdaQueryWrapperX<XqImageGenRuleDO>()
                .eq(XqImageGenRuleDO::getPlatformId, pid)
                .eq(XqImageGenRuleDO::getCategoryId, cid)
                .last("LIMIT 1"));
    }

}
