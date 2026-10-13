package cn.iocoder.yudao.module.fms.job.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.fms.service.xq.XqGigaProductListSyncService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 大健选品列表定时同步：每天一次，近 N 天（默认 3），只增不覆盖，同 SKU 不录入；
 * 三套（及所有开启定时的）凭证分开串行跑。
 *
 * <p>handler 参数可选 JSON：{"days":3}
 */
@Component
public class XqGigaProductListSyncJob implements JobHandler {

    @Resource
    private XqGigaProductListSyncService listSyncService;

    @Override
    public String execute(String param) {
        int days = 3;
        if (StrUtil.isNotBlank(param)) {
            try {
                JSONObject json = JSONUtil.parseObj(param.trim());
                Integer d = json.getInt("days");
                if (d != null && d > 0) {
                    days = d;
                }
            } catch (Exception ignored) {
                // 也支持纯数字 "3"
                if (StrUtil.isNumeric(param.trim())) {
                    days = Integer.parseInt(param.trim());
                }
            }
        }
        return listSyncService.syncScheduledCredentials(days);
    }

}
