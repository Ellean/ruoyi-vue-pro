package cn.iocoder.yudao.module.fms.service.xq.rpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XqRpaLauncherConfiguration {

    @Bean
    public XqRpaJobLauncher scriptRpaJobLauncher() {
        return new LocalProcessRpaJobLauncher(LocalProcessRpaJobLauncher.MODE_SCRIPT);
    }

    @Bean
    public XqRpaJobLauncher exeRpaJobLauncher() {
        return new LocalProcessRpaJobLauncher(LocalProcessRpaJobLauncher.MODE_EXE);
    }

}
