package cn.iocoder.yudao.module.fms.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Star 产品工作台（/xq/**）独立 OpenAPI 分组。
 * Knife4j「API 接口」页下拉选择 {@code xq}，与 all / fms / product 分开。
 */
@Configuration(proxyBeanMethods = false)
public class XqWebConfiguration {

    @Bean
    public GroupedOpenApi xqGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("xq");
    }

}
