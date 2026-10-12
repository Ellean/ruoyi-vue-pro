package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingDetailTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqListingDetailTemplateMapper extends BaseMapperX<XqListingDetailTemplateDO> {

    default List<XqListingDetailTemplateDO> selectByPlatformId(String platformId) {
        if (platformId == null || platformId.isBlank()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqListingDetailTemplateDO>()
                .eq(XqListingDetailTemplateDO::getPlatformId, platformId.trim())
                .orderByDesc(XqListingDetailTemplateDO::getUpdateDate)
                .orderByDesc(XqListingDetailTemplateDO::getCreateDate));
    }

    default XqListingDetailTemplateDO selectFirstWithSourceByPlatform(String platformId) {
        if (platformId == null || platformId.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<XqListingDetailTemplateDO>()
                .eq(XqListingDetailTemplateDO::getPlatformId, platformId.trim())
                .isNotNull(XqListingDetailTemplateDO::getSourceFilePath)
                .ne(XqListingDetailTemplateDO::getSourceFilePath, "")
                .orderByDesc(XqListingDetailTemplateDO::getUpdateDate)
                .last("LIMIT 1"));
    }

}
