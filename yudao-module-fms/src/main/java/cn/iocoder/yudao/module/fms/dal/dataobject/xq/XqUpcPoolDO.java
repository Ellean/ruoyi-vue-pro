package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 原库全局 UPC 池 t_giga_upc_pool（数据源 xq） */
@TableName("t_giga_upc_pool")
@Data
@TenantIgnore
public class XqUpcPoolDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String shopId;
    private String upc;
    /** free / paid */
    private String poolType;
    /** available / used / void */
    private String status;
    private String productListId;
    private String claimedBy;
    private String claimedByName;
    private LocalDateTime claimedAt;
    private String source;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
