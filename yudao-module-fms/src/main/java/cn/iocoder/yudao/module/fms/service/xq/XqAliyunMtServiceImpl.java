package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextRespVO;
import cn.iocoder.yudao.module.fms.framework.xq.XqAliyunMtProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TreeMap;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_ALIYUN_MT_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_ALIYUN_MT_NOT_CONFIGURED;

@Service
@Slf4j
public class XqAliyunMtServiceImpl implements XqAliyunMtService {

    private static final String API_VERSION = "2018-10-12";

    @Resource
    private XqAliyunMtProperties mtProperties;

    @Override
    public boolean isConfigured() {
        return mtProperties.isEnabled()
                && StrUtil.isNotBlank(mtProperties.getAccessKeyId())
                && StrUtil.isNotBlank(mtProperties.getAccessKeySecret());
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", mtProperties.isEnabled());
        m.put("configured", isConfigured());
        m.put("endpoint", mtProperties.getEndpoint());
        m.put("defaultSourceLanguage", mtProperties.getDefaultSourceLanguage());
        m.put("defaultTargetLanguage", mtProperties.getDefaultTargetLanguage());
        m.put("hint", isConfigured()
                ? "已配置，可用于文案中文对照"
                : "请在配置中填写 xq.aliyun.mt.access-key-id / access-key-secret，并开通机器翻译专业版");
        return m;
    }

    @Override
    public XqTranslateTextRespVO translateText(XqTranslateTextReqVO reqVO) {
        ensureConfigured();
        String text = StrUtil.trim(reqVO.getText());
        if (StrUtil.isBlank(text)) {
            XqTranslateTextRespVO empty = new XqTranslateTextRespVO();
            empty.setTranslated("");
            empty.setScene(normalizeScene(reqVO.getScene()));
            return empty;
        }
        return doTranslate(
                text,
                StrUtil.blankToDefault(reqVO.getSourceLanguage(), mtProperties.getDefaultSourceLanguage()),
                StrUtil.blankToDefault(reqVO.getTargetLanguage(), mtProperties.getDefaultTargetLanguage()),
                normalizeScene(reqVO.getScene()));
    }

    @Override
    public XqTranslateCopyPreviewRespVO translateCopyPreview(XqTranslateCopyPreviewReqVO reqVO) {
        ensureConfigured();
        String source = StrUtil.blankToDefault(
                reqVO.getSourceLanguage(), mtProperties.getDefaultSourceLanguage());
        String target = StrUtil.blankToDefault(
                reqVO.getTargetLanguage(), mtProperties.getDefaultTargetLanguage());

        XqTranslateCopyPreviewRespVO resp = new XqTranslateCopyPreviewRespVO();
        resp.setConfigured(true);

        String title = StrUtil.trim(reqVO.getTitle());
        if (StrUtil.isNotBlank(title)) {
            resp.setTitleZh(doTranslate(title, source, target, "title").getTranslated());
        } else {
            resp.setTitleZh("");
        }

        List<String> pointsZh = new ArrayList<>();
        List<String> points = reqVO.getSellingPoints() == null ? List.of() : reqVO.getSellingPoints();
        for (String p : points) {
            String t = StrUtil.trim(p);
            if (StrUtil.isBlank(t)) {
                pointsZh.add("");
            } else {
                pointsZh.add(doTranslate(t, source, target, "description").getTranslated());
            }
        }
        resp.setSellingPointsZh(pointsZh);

        String desc = StrUtil.trim(reqVO.getDescription());
        if (StrUtil.isNotBlank(desc)) {
            resp.setDescriptionZh(doTranslate(desc, source, target, "description").getTranslated());
        } else {
            resp.setDescriptionZh("");
        }
        return resp;
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw exception(XQ_ALIYUN_MT_NOT_CONFIGURED);
        }
    }

    private static String normalizeScene(String scene) {
        String s = StrUtil.blankToDefault(scene, "description").trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "title", "description", "communication", "general" -> s;
            default -> "description";
        };
    }

    private XqTranslateTextRespVO doTranslate(
            String text, String sourceLanguage, String targetLanguage, String scene) {
        // 单次上限 5000 字符，超长截断并提示（对照场景足够）
        String sourceText = text;
        if (sourceText.length() > 4800) {
            sourceText = sourceText.substring(0, 4800);
        }
        boolean general = "general".equals(scene);
        String action = general ? "TranslateGeneral" : "Translate";

        Map<String, String> params = new TreeMap<>();
        params.put("Action", action);
        params.put("Version", API_VERSION);
        params.put("Format", "JSON");
        params.put("AccessKeyId", mtProperties.getAccessKeyId().trim());
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureVersion", "1.0");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("Timestamp", utcTimestamp());
        params.put("FormatType", "text");
        params.put("SourceLanguage", StrUtil.blankToDefault(sourceLanguage, "auto"));
        params.put("TargetLanguage", StrUtil.blankToDefault(targetLanguage, "zh"));
        params.put("SourceText", sourceText);
        params.put("Scene", general ? "general" : scene);

        try {
            String signature = sign("POST", params, mtProperties.getAccessKeySecret().trim());
            params.put("Signature", signature);

            String url = "https://" + mtProperties.getEndpoint().trim() + "/";
            int timeout = Math.max(mtProperties.getConnectTimeoutMs(), mtProperties.getReadTimeoutMs());
            try (HttpResponse response = HttpRequest.post(url)
                    // hutool 的 form 只收 Map<String, Object>，泛型不协变，需按该类型复制一份
                    .form(new TreeMap<String, Object>(params))
                    .timeout(timeout)
                    .execute()) {
                String body = response.body();
                if (!response.isOk()) {
                    log.warn("[aliyun-mt] http={} body={}", response.getStatus(), body);
                    throw exception(XQ_ALIYUN_MT_FAIL, "HTTP " + response.getStatus());
                }
                JSONObject json = JSONUtil.parseObj(body);
                String codeStr = StrUtil.blankToDefault(json.getStr("Code"), "");
                if (StrUtil.isNotBlank(codeStr) && !"200".equals(codeStr)) {
                    String msg = StrUtil.blankToDefault(json.getStr("Message"), body);
                    log.warn("[aliyun-mt] code={} msg={}", codeStr, msg);
                    throw exception(XQ_ALIYUN_MT_FAIL, msg);
                }
                JSONObject data = json.getJSONObject("Data");
                if (data == null) {
                    throw exception(XQ_ALIYUN_MT_FAIL, "返回无 Data");
                }
                XqTranslateTextRespVO vo = new XqTranslateTextRespVO();
                vo.setTranslated(StrUtil.blankToDefault(data.getStr("Translated"), ""));
                vo.setDetectedLanguage(data.getStr("DetectedLanguage"));
                vo.setWordCount(data.getStr("WordCount"));
                vo.setScene(scene);
                return vo;
            }
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[aliyun-mt] translate fail", ex);
            throw exception(XQ_ALIYUN_MT_FAIL, StrUtil.blankToDefault(ex.getMessage(), "调用异常"));
        }
    }

    private static String utcTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(new Date());
    }

    /** 阿里云 POP RPC 签名（HMAC-SHA1） */
    private static String sign(String method, Map<String, String> params, String accessKeySecret)
            throws Exception {
        StringBuilder canonicalized = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if ("Signature".equals(e.getKey())) {
                continue;
            }
            if (canonicalized.length() > 0) {
                canonicalized.append('&');
            }
            canonicalized.append(percentEncode(e.getKey()))
                    .append('=')
                    .append(percentEncode(e.getValue()));
        }
        String stringToSign = method
                + "&"
                + percentEncode("/")
                + "&"
                + percentEncode(canonicalized.toString());
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private static String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
