package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqErpWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqErpWarehouseMapper extends BaseMapperX<XqErpWarehouseDO> {

    default List<XqErpWarehouseDO> selectAll() {
        return selectList();
    }

}
