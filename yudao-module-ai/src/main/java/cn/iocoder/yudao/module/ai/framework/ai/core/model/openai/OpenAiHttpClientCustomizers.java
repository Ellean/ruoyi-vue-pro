package cn.iocoder.yudao.module.ai.framework.ai.core.model.openai;

import com.openai.client.OpenAIClient;
import com.openai.core.ClientOptions;
import com.openai.core.http.HttpClient;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Spring AI OpenAI OkHttp 客户端定制。
 * <p>
 * 部分中转站对 HTTP/2 不兼容，会出现 {@code stream was reset: PROTOCOL_ERROR}。
 * {@code SpringAiOpenAiHttpClient.Builder} 不暴露 {@code protocols()}，且
 * {@code okBuilder} 只是 {@code build()} 里的局部变量，因此在客户端建好后
 * 把内部 {@link OkHttpClient} 换成仅 HTTP/1.1。
 * <p>
 * ClientOptions 同时持有 {@code httpClient}/{@code originalHttpClient}，且前者常被
 * Retry 包装，因此除直接替换外，还会在对象图中递归查找并改写 OkHttpClient。
 */
public final class OpenAiHttpClientCustomizers {

    private static final Logger log = LoggerFactory.getLogger(OpenAiHttpClientCustomizers.class);

    private OpenAiHttpClientCustomizers() {
    }

    /** 保留兼容：不再改 Builder（改了也无效），真正生效的是 {@link #forceHttp11(OpenAIClient)} */
    public static final OpenAiHttpClientBuilderCustomizer FORCE_HTTP_1_1 = builder -> {
        // Spring AI 2.0：okBuilder 非字段，此处不能改协议
    };

    public static final List<OpenAiHttpClientBuilderCustomizer> FORCE_HTTP_1_1_LIST =
            Collections.singletonList(FORCE_HTTP_1_1);

    public static OpenAIClient forceHttp11(OpenAIClient client) {
        if (client == null) {
            return null;
        }
        int patched = 0;
        Object options = readField(client, ClientOptions.class);
        if (options != null) {
            List<HttpClient> httpClients = readFields(options, HttpClient.class);
            for (HttpClient httpClient : httpClients) {
                if (httpClient instanceof SpringAiOpenAiHttpClient springHttp) {
                    if (replaceOkHttp(springHttp)) {
                        patched++;
                    }
                }
            }
            // 包装层 / 其它实现：递归抠 OkHttpClient
            patched += forceOkHttp11Deep(options, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        // 再扫一层 client 本体（部分版本 ClientOptions 不在同层）
        patched += forceOkHttp11Deep(client, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (patched <= 0) {
            throw new IllegalStateException(
                    "无法强制 HTTP/1.1：未找到可替换的 OkHttpClient（中转站易 PROTOCOL_ERROR）");
        }
        log.debug("[OpenAI] forced HTTP/1.1 on {} OkHttpClient(s)", patched);
        return client;
    }

    private static boolean replaceOkHttp(SpringAiOpenAiHttpClient springHttp) {
        OkHttpClient current = springHttp.getOkHttpClient();
        if (current == null) {
            return false;
        }
        if (isHttp11Only(current)) {
            return true;
        }
        OkHttpClient http11 = current.newBuilder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        Field field = findField(springHttp.getClass(), OkHttpClient.class);
        if (field == null) {
            throw new IllegalStateException("SpringAiOpenAiHttpClient 无 OkHttpClient 字段");
        }
        field.setAccessible(true);
        try {
            field.set(springHttp, http11);
            return true;
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("无法写入 OkHttpClient 以强制 HTTP/1.1", ex);
        }
    }

    /**
     * 在对象图中查找 OkHttpClient 字段并改为仅 HTTP/1.1。
     * 返回成功改写（或已是 HTTP/1.1）的数量。
     */
    private static int forceOkHttp11Deep(Object root, int depth, Set<Object> seen) {
        if (root == null || depth > 6 || seen.contains(root)) {
            return 0;
        }
        Class<?> type = root.getClass();
        if (type.isPrimitive() || type.isEnum() || type.isArray()
                || root instanceof String || root instanceof Number
                || root instanceof Boolean || root instanceof Class) {
            return 0;
        }
        // 跳过 JDK / 第三方大对象
        String name = type.getName();
        if (name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("kotlin.") || name.startsWith("com.fasterxml.")) {
            return 0;
        }
        seen.add(root);
        int patched = 0;
        if (root instanceof OkHttpClient ok) {
            // 调用方应以字段替换为准；这里仅统计
            return isHttp11Only(ok) ? 1 : 0;
        }
        if (root instanceof SpringAiOpenAiHttpClient springHttp) {
            return replaceOkHttp(springHttp) ? 1 : 0;
        }
        for (Class<?> cur = type; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            for (Field field : cur.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(root);
                } catch (IllegalAccessException ex) {
                    continue;
                }
                if (value instanceof OkHttpClient ok) {
                    if (isHttp11Only(ok)) {
                        patched++;
                        continue;
                    }
                    OkHttpClient http11 = ok.newBuilder()
                            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                            .build();
                    try {
                        field.set(root, http11);
                        patched++;
                    } catch (IllegalAccessException ignored) {
                        // ignore
                    }
                    continue;
                }
                if (value instanceof SpringAiOpenAiHttpClient springHttp) {
                    if (replaceOkHttp(springHttp)) {
                        patched++;
                    }
                    continue;
                }
                if (value != null && !value.getClass().isPrimitive()) {
                    String vn = value.getClass().getName();
                    if (vn.startsWith("com.openai.")
                            || vn.startsWith("org.springframework.ai.openai.")
                            || vn.contains("HttpClient")
                            || vn.contains("OkHttp")) {
                        patched += forceOkHttp11Deep(value, depth + 1, seen);
                    }
                }
            }
        }
        return patched;
    }

    private static boolean isHttp11Only(OkHttpClient client) {
        if (client == null || client.protocols() == null || client.protocols().isEmpty()) {
            return false;
        }
        for (Protocol p : client.protocols()) {
            if (p != Protocol.HTTP_1_1) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, Class<T> type) {
        List<T> values = readFields(target, type);
        return values.isEmpty() ? null : values.get(0);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> readFields(Object target, Class<T> type) {
        List<T> values = new ArrayList<>();
        for (Class<?> cur = target.getClass(); cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            for (Field field : cur.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !type.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(target);
                    if (type.isInstance(value)) {
                        values.add((T) value);
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("无法读取字段 " + field.getName(), ex);
                }
            }
        }
        return values;
    }

    private static Field findField(Class<?> type, Class<?> fieldType) {
        for (Class<?> cur = type; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            for (Field field : cur.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (fieldType.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
        }
        return null;
    }
}
