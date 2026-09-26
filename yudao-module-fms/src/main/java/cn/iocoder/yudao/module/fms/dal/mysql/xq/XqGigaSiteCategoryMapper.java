package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaSiteCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqGigaSiteCategoryMapper extends BaseMapperX<XqGigaSiteCategoryDO> {

    default List<XqGigaSiteCategoryDO> selectAllOrdered() {
        return selectList(new LambdaQueryWrapperX<XqGigaSiteCategoryDO>()
                .orderByAsc(XqGigaSiteCategoryDO::getLevel)
                .orderByAsc(XqGigaSiteCategoryDO::getParentId)
                .orderByAsc(XqGigaSiteCategoryDO::getSortOrder)
                .orderByAsc(XqGigaSiteCategoryDO::getGigaId));
    }

    default List<XqGigaSiteCategoryDO> selectByLevel(Integer level) {
        return selectList(new LambdaQueryWrapperX<XqGigaSiteCategoryDO>()
                .eq(XqGigaSiteCategoryDO::getLevel, level)
                .orderByAsc(XqGigaSiteCategoryDO::getSortOrder)
                .orderByAsc(XqGigaSiteCategoryDO::getGigaId));
    }

}
