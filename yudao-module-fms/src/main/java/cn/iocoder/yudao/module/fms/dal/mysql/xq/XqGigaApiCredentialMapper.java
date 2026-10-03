package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.hutool.core.util.StrUtil;
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
                .orderByAsc(XqGigaApiCredentialDO::getVendorCode)
                .orderByAsc(XqGigaApiCredentialDO::getPriceRole)
                .orderByDesc(XqGigaApiCredentialDO::getIsDefault)
                .orderByDesc(XqGigaApiCredentialDO::getId));
    }

    /** 清除同一商家 + 价格角色下的默认标记 */
    default void clearDefault(String vendorCode, String priceRole) {
        LambdaUpdateWrapper<XqGigaApiCredentialDO> uw = new LambdaUpdateWrapper<XqGigaApiCredentialDO>()
                .set(XqGigaApiCredentialDO::getIsDefault, false)
                .eq(XqGigaApiCredentialDO::getIsDefault, true)
                .eq(XqGigaApiCredentialDO::getPriceRole, priceRole);
        if (StrUtil.isBlank(vendorCode)) {
            uw.and(w -> w.isNull(XqGigaApiCredentialDO::getVendorCode)
                    .or().eq(XqGigaApiCredentialDO::getVendorCode, ""));
        } else {
            uw.eq(XqGigaApiCredentialDO::getVendorCode, vendorCode);
        }
        update(null, uw);
    }

    default List<XqGigaApiCredentialDO> selectScheduledSyncEnabled() {
        return selectList(new LambdaQueryWrapperX<XqGigaApiCredentialDO>()
                .eq(XqGigaApiCredentialDO::getEnabled, true)
                .eq(XqGigaApiCredentialDO::getEnableScheduledSync, true)
                .orderByAsc(XqGigaApiCredentialDO::getVendorCode)
                .orderByAsc(XqGigaApiCredentialDO::getPriceRole));
    }

}
