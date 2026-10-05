package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingCategoryFieldTemplateDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("xq")
public interface XqListingCategoryFieldTemplateMapper
        extends BaseMapperX<XqListingCategoryFieldTemplateDO> {

    default XqListingCategoryFieldTemplateDO selectByCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingCategoryFieldTemplateDO>()
                .eq(XqListingCategoryFieldTemplateDO::getCategoryId, categoryId)
                .last("LIMIT 1"));
    }

}
