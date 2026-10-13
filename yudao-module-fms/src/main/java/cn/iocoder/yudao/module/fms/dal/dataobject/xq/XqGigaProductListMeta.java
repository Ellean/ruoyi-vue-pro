package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import lombok.Data;

/**
 * 列表行元数据（不依赖 snapshot），供按需拉详情锚定 user/config。
 */
@Data
public class XqGigaProductListMeta {

    private String id;
    private String sku;
    private String userId;
    private String gigaConfigId;
    private String productName;

}
