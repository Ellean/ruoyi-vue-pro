package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "XQ 店铺保存")
@Data
public class XqStoreSaveReqVO {
    private Long id;
    @NotBlank
    private String name;
    @NotNull
    private Long platformId;
    private Integer sourceStoreId;
    private String account;
    /** 留空表示不改密码 */
    private String password;
    private String imageUrl;
    private Integer status;
    private String deptId;
    private String remark;
}
