package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库后台店铺管理 sys_store（数据源 xq）
 * <p>
 * 旧 ERP 上架店铺列表实际从此表读取，再映射到 t_giga_listing_platform。
 */
@TableName("sys_store")
@Data
@TenantIgnore
public class XqSysStoreDO {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    /** 后台填写的平台名，如 amazon / wayfair / kohls */
    private String platform;
    private String account;
    /** 店铺卖家密码（原库明文或既有存储） */
    private String password;
    private String imageUrl;
    private Integer status;
    private String remark;
    private String delFlag;
    private String deptId;
    private LocalDateTime createDate;

}
