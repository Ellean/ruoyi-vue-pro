package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqWarehouseMapper extends BaseMapperX<XqWarehouseDO> {

    default List<XqWarehouseDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqWarehouseDO>()
                .orderByAsc(XqWarehouseDO::getSort)
                .orderByAsc(XqWarehouseDO::getId));
    }

    default XqWarehouseDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<XqWarehouseDO>()
                .eq(XqWarehouseDO::getCode, code)
                .last("LIMIT 1"));
    }

    default XqWarehouseDO selectBySourceAddressId(Integer sourceAddressId) {
        return selectOne(new LambdaQueryWrapperX<XqWarehouseDO>()
                .eq(XqWarehouseDO::getSourceAddressId, sourceAddressId)
                .last("LIMIT 1"));
    }

}
