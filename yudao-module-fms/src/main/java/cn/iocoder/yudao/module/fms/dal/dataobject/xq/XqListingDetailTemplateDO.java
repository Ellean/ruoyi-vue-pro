package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库上架分表模板 t_giga_listing_detail_template（数据源 xq）
 */
@TableName("t_giga_listing_detail_template")
@Data
@TenantIgnore
public class XqListingDetailTemplateDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String name;
    private String sourceFileName;
    private String sourceFilePath;
    private Integer headerRow;
    private String sheetName;
    private Integer keyRow;
    private Integer dataStartRow;
    private String columnsJson;
    private String workbookSnapshotJson;
    private String tablesJson;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
