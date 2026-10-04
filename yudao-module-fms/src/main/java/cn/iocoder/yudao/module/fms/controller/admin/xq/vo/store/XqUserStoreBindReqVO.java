package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "XQ 用户绑店（空 storeIds=不限店）")
@Data
public class XqUserStoreBindReqVO {
    @NotNull
    private Long userId;
    private List<Long> storeIds;
}
