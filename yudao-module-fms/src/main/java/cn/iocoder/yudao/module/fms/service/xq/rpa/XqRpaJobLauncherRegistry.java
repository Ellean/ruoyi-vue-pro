package cn.iocoder.yudao.module.fms.service.xq.rpa;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 按 runnerMode 选择启动器。新增通道：实现 {@link XqRpaJobLauncher} 并注册为 Spring Bean。
 */
@Component
public class XqRpaJobLauncherRegistry {

    @Resource
    private List<XqRpaJobLauncher> launchers;

    private final Map<String, XqRpaJobLauncher> byMode = new HashMap<>();

    @PostConstruct
    public void init() {
        for (XqRpaJobLauncher launcher : launchers) {
            if (launcher == null || StrUtil.isBlank(launcher.mode())) {
                continue;
            }
            byMode.put(launcher.mode().trim().toLowerCase(Locale.ROOT), launcher);
        }
    }

    public XqRpaJobLauncher require(String mode) {
        String key = StrUtil.blankToDefault(mode, CommanderRpaJobLauncher.MODE)
                .trim().toLowerCase(Locale.ROOT);
        XqRpaJobLauncher launcher = byMode.get(key);
        if (launcher == null) {
            launcher = byMode.get(CommanderRpaJobLauncher.MODE);
        }
        if (launcher == null) {
            throw new IllegalStateException("未注册任何 RPA 启动器");
        }
        return launcher;
    }

    public static String normalizeMode(String mode) {
        String key = StrUtil.blankToDefault(mode, CommanderRpaJobLauncher.MODE)
                .trim().toLowerCase(Locale.ROOT);
        if (LocalProcessRpaJobLauncher.MODE_SCRIPT.equals(key)
                || LocalProcessRpaJobLauncher.MODE_EXE.equals(key)
                || CommanderRpaJobLauncher.MODE.equals(key)) {
            return key;
        }
        return CommanderRpaJobLauncher.MODE;
    }

}
