package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 品库-产品 DO
 */
@TableName("xq_product")
@KeySequence("xq_product_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqProductDO extends BaseDO {

    @TableId
    private Long id;
    private String sku;
    private String itemCode;
    private String name;
    private String categoryName;
    /** Giga 类目 giga_id（叶子或挂载节点） */
    private Long gigaCategoryId;
    private String imageUrl;
    private Integer qtyAvailable;
    private String supplierCode;
    private String supplierName;
    private String listedTag;
    private Integer status;
    private String remark;

}
