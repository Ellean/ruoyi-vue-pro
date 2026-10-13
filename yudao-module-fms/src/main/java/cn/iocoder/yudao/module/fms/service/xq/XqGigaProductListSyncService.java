package cn.iocoder.yudao.module.fms.service.xq;

/**
 * 产品库凭证定时拉取选品列表：近 N 天、只增不覆盖、同 SKU 跳过；各凭证分开跑。
 */
public interface XqGigaProductListSyncService {

    /**
     * 拉取全部「启用 + 开启定时」的凭证，逐套分开执行。
     *
     * @param recentDays 近几天（收藏/加入时间窗口），默认 3
     * @return 摘要文案
     */
    String syncScheduledCredentials(int recentDays);

}
