package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSysStoreDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("xq")
public interface XqSysStoreMapper extends BaseMapperX<XqSysStoreDO> {

    default List<XqSysStoreDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<XqSysStoreDO>()
                .eq(XqSysStoreDO::getDelFlag, "0")
                .and(w -> w.eq(XqSysStoreDO::getStatus, 1).or().isNull(XqSysStoreDO::getStatus))
                .orderByAsc(XqSysStoreDO::getName)
                .orderByAsc(XqSysStoreDO::getId));
    }

}
