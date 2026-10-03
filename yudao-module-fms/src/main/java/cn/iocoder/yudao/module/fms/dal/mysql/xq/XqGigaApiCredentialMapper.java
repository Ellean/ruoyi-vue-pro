package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaApiCredentialDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqGigaApiCredentialMapper extends BaseMapperX<XqGigaApiCredentialDO> {

    default List<XqGigaApiCredentialDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqGigaApiCredentialDO>()
                .orderByDesc(XqGigaApiCredentialDO::getIsDefault)
                .orderByDesc(XqGigaApiCredentialDO::getId));
    }

    default void clearDefault() {
        update(null, new LambdaUpdateWrapper<XqGigaApiCredentialDO>()
                .set(XqGigaApiCredentialDO::getIsDefault, false)
                .eq(XqGigaApiCredentialDO::getIsDefault, true));
    }

}
