package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "可分配美工用户")
@Data
public class XqAssignableImageUserRespVO {

    private Long id;
    private String nickname;
    private String username;

}
