package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Giga 官网类目（扒取入库）
 */
@TableName("xq_giga_site_category")
@KeySequence("xq_giga_site_category_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqGigaSiteCategoryDO {

    @TableId
    private Long id;
    private Long gigaId;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private String imagePath;
    private String pathIds;
    private String pathNames;
    private String href;
    private String source;
    private LocalDateTime fetchedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
