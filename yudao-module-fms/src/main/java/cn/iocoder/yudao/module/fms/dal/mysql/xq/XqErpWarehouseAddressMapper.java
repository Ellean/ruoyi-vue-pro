package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqErpWarehouseAddressDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqErpWarehouseAddressMapper extends BaseMapperX<XqErpWarehouseAddressDO> {

    default List<XqErpWarehouseAddressDO> selectAll() {
        return selectList();
    }

}
