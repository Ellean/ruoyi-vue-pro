package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqStoreWarehouseMapper extends BaseMapperX<XqStoreWarehouseDO> {

    default List<XqStoreWarehouseDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqStoreWarehouseDO>()
                .orderByAsc(XqStoreWarehouseDO::getStoreId)
                .orderByAsc(XqStoreWarehouseDO::getPriority)
                .orderByAsc(XqStoreWarehouseDO::getId));
    }

    default List<XqStoreWarehouseDO> selectByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<XqStoreWarehouseDO>()
                .eq(XqStoreWarehouseDO::getStoreId, storeId)
                .orderByAsc(XqStoreWarehouseDO::getPriority)
                .orderByAsc(XqStoreWarehouseDO::getId));
    }

}
