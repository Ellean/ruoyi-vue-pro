package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库按分类图片提示词 t_giga_image_gen_rule（数据源 xq）
 */
@TableName("t_giga_image_gen_rule")
@Data
@TenantIgnore
public class XqImageGenRuleDO {

    @TableId(type = IdType.INPUT)
    private String id;
    /** 上架平台 ID，空=通用 */
    private String platformId;
    /** 上架分类 ID，空=该平台默认 */
    private String categoryId;
    private String categoryName;
    private String name;
    private String promptText;
    private String negativePrompt;
    private String configJson;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
