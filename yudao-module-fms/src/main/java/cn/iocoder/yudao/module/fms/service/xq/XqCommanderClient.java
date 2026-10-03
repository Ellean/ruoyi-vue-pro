package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 控制中枢 OpenAPI 精简客户端（参考旧 commanderApi.triggerRpa，不照搬全量能力）
 */
@Component
public class XqCommanderClient {

    public Map<String, Object> triggerJob(String baseUrl, String appKey, String appSecret,
                                          String jobUuid, Map<String, Object> inputParam) {
        String url = StrUtil.removeSuffix(baseUrl.trim(), "/") + "/openAPI/v2/job/operation";
        JSONObject body = new JSONObject();
        body.set("jobUuid", jobUuid);
        body.set("operation", 1);
        if (inputParam != null && !inputParam.isEmpty()) {
            body.set("inputParam", inputParam);
            body.set("needToUpdateCurrentJobProcessInputParam", false);
        }
        try (HttpResponse response = HttpRequest.post(url)
                .header("appKey", appKey)
                .header("appSecret", appSecret)
                .body(body.toString())
                .timeout(120_000)
                .execute()) {
            String raw = response.body();
            if (!response.isOk()) {
                throw new IllegalStateException("HTTP " + response.getStatus() + ": " + StrUtil.maxLength(raw, 200));
            }
            JSONObject payload = JSONUtil.parseObj(raw);
            Object code = payload.get("code");
            if (code != null && !"0".equals(String.valueOf(code)) && !Integer.valueOf(0).equals(code)) {
                throw new IllegalStateException(StrUtil.blankToDefault(
                        payload.getStr("msg"), payload.getStr("message", "控制中枢返回错误")));
            }
            Object data = payload.get("data");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobUuid", jobUuid);
            result.put("workUuid", extractWorkUuid(data));
            result.put("inputParamSent", inputParam != null && !inputParam.isEmpty());
            result.put("data", data);
            return result;
        }
    }

    private static String extractWorkUuid(Object data) {
        if (data == null) {
            return null;
        }
        JSONObject obj = data instanceof JSONObject
                ? (JSONObject) data
                : JSONUtil.parseObj(JSONUtil.toJsonStr(data));
        for (String key : new String[]{"workUuid", "workUUID", "workExecuteUuid", "workExecuteUUID", "uuid", "id"}) {
            String val = obj.getStr(key);
            if (StrUtil.isNotBlank(val)) {
                return val.trim();
            }
        }
        return null;
    }

}
