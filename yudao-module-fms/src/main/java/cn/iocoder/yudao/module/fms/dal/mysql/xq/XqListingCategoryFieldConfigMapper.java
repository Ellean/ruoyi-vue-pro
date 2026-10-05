package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryFieldConfigDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqListingCategoryFieldConfigMapper
        extends BaseMapperX<XqListingCategoryFieldConfigDO> {

    default XqListingCategoryFieldConfigDO selectByPlatformAndCategory(
            String platformId, String categoryId) {
        String pid = platformId == null ? "" : platformId;
        String cid = categoryId == null ? "" : categoryId;
        return selectOne(new LambdaQueryWrapperX<XqListingCategoryFieldConfigDO>()
                .eq(XqListingCategoryFieldConfigDO::getPlatformId, pid)
                .eq(XqListingCategoryFieldConfigDO::getCategoryId, cid)
                .last("LIMIT 1"));
    }

    default XqListingCategoryFieldConfigDO selectByCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingCategoryFieldConfigDO>()
                .eq(XqListingCategoryFieldConfigDO::getCategoryId, categoryId)
                .last("LIMIT 1"));
    }

    default List<XqListingCategoryFieldConfigDO> selectByPlatformId(String platformId) {
        String pid = platformId == null ? "" : platformId;
        return selectList(new LambdaQueryWrapperX<XqListingCategoryFieldConfigDO>()
                .eq(XqListingCategoryFieldConfigDO::getPlatformId, pid));
    }

}
