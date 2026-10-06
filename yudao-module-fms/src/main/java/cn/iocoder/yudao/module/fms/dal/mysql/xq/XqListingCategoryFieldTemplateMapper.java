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

    default java.util.List<XqListingCategoryFieldTemplateDO> selectByPlatformId(String platformId) {
        String pid = platformId == null ? "" : platformId;
        if (pid.isBlank()) {
            return java.util.List.of();
        }
        return selectByPlatformIds(java.util.List.of(pid));
    }

    default java.util.List<XqListingCategoryFieldTemplateDO> selectByPlatformIds(
            java.util.Collection<String> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> ids = platformIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<XqListingCategoryFieldTemplateDO> meta = selectList(
                new LambdaQueryWrapperX<XqListingCategoryFieldTemplateDO>()
                        .in(XqListingCategoryFieldTemplateDO::getPlatformId, ids)
                        .select(XqListingCategoryFieldTemplateDO::getId,
                                XqListingCategoryFieldTemplateDO::getPlatformId,
                                XqListingCategoryFieldTemplateDO::getCategoryId,
                                XqListingCategoryFieldTemplateDO::getHierarchyCode));
        java.util.List<XqListingCategoryFieldTemplateDO> out = new java.util.ArrayList<>();
        for (XqListingCategoryFieldTemplateDO row : meta) {
            if (row == null || row.getId() == null) {
                continue;
            }
            XqListingCategoryFieldTemplateDO full = selectById(row.getId());
            if (full != null) {
                out.add(full);
            }
        }
        return out;
    }

}
