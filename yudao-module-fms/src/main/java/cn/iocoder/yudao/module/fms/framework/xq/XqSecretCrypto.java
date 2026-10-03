package cn.iocoder.yudao.module.fms.framework.xq;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 产品库 Client Secret 等敏感字段加密（AES）。
 * 配置项：yudao.xq.secret-aes-key（任意字符串，内部取 SHA-256 前 16 字节）
 */
@Component
public class XqSecretCrypto {

    private final AES aes;

    public XqSecretCrypto(
            @Value("${yudao.xq.secret-aes-key:xq-giga-dev-secret-change-me}") String rawKey) {
        byte[] digest = SecureUtil.sha256().digest(StrUtil.blankToDefault(rawKey, "xq-giga-dev-secret-change-me"));
        byte[] key = Arrays.copyOf(digest, 16);
        this.aes = SecureUtil.aes(key);
    }

    public String encrypt(String plain) {
        if (StrUtil.isBlank(plain)) {
            return null;
        }
        return HexUtil.encodeHexStr(aes.encrypt(plain.getBytes(StandardCharsets.UTF_8)));
    }

    public String decrypt(String enc) {
        if (StrUtil.isBlank(enc)) {
            return null;
        }
        return new String(aes.decrypt(HexUtil.decodeHex(enc.trim())), StandardCharsets.UTF_8);
    }

    public static String mask(String plain) {
        if (StrUtil.isBlank(plain)) {
            return "";
        }
        String v = plain.trim();
        if (v.length() <= 8) {
            return "****";
        }
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

}
