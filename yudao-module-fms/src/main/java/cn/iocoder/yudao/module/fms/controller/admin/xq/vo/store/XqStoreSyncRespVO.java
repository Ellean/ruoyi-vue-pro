package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "从旧库同步结果")
@Data
public class XqStoreSyncRespVO {

    private int platformUpserted;
    private int storeUpserted;
    private int aliasBound;
    /** 新建的系统用户数 */
    private int userCreated;
    /** 已存在并复用的系统用户数 */
    private int userMatched;
    /** 写入的角色分配次数（用户×角色） */
    private int roleAssigned;
    /** 已写入 xq_user_store 的绑定条数 */
    private int userStoreBound;
    /** 创建失败或跳过的账号 */
    private List<String> unmatchedAccounts = new ArrayList<>();

}
