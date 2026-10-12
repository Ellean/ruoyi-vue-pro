package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("xq_listing_platform_credential")
@KeySequence("xq_listing_platform_credential_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqListingPlatformCredentialDO extends BaseDO {

    public static final String PROVIDER_MIRAKL = "mirakl";
    public static final String PROVIDER_WAYFAIR = "wayfair";

    @TableId
    private Long id;
    private Long platformId;
    private String sourcePlatformId;
    private String platformCode;
    private String platformName;
    private String provider;
    private String baseUrl;
    private String shopId;
    private String apiKeyEnc;
    private String apiKeyMask;
    private String clientId;
    private String clientSecretEnc;
    private String clientSecretMask;
    private String supplierId;
    private String env;
    private String extraJson;
    private Boolean enabled;
    private LocalDateTime syncedFromXqAt;
    private String remark;

}
