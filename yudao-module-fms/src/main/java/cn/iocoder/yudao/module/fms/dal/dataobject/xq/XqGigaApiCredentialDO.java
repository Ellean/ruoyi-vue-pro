package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("xq_giga_api_credential")
@KeySequence("xq_giga_api_credential_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqGigaApiCredentialDO extends BaseDO {

    /** 自提价（不含运费） */
    public static final String PRICE_ROLE_PICKUP = "pickup";
    /** 一键代发价（含运费场景） */
    public static final String PRICE_ROLE_DROPSHIP = "dropship";
    /** 库中已有 SKU 则不扩 */
    public static final String SYNC_DEDUPE_SKIP = "skip_if_exists";
    /** 始终刷新 */
    public static final String SYNC_DEDUPE_REFRESH = "always_refresh";

    @TableId
    private Long id;
    private String name;
    /** 商家/账号编码，多家分组 */
    private String vendorCode;
    private String vendorName;
    private String clientId;
    private String clientSecretEnc;
    private String clientSecretMask;
    private Boolean sandbox;
    private String baseUrl;
    /** pickup / dropship */
    private String priceRole;
    /** 是否参与定时拉取选品库 */
    private Boolean enableScheduledSync;
    /** skip_if_exists / always_refresh */
    private String syncDedupeMode;
    /** 同商家+同价格角色下的默认凭证 */
    private Boolean isDefault;
    private Boolean enabled;
    private String remark;

}
