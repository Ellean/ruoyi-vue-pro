######################################################################
# 构建层
#
# CI 内完成 Maven 打包，不再依赖「先在别处构建好 jar 再 COPY 进来」。
# 基础镜像锁 JDK 25：根 pom 是 <java.version>25</java.version> +
# Spring Boot 4.1.0，用 21 会直接编译失败。
######################################################################
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
ENV TZ=Asia/Shanghai

# 依赖层：只拷 reactor 内全部 23 个 pom（保持目录结构），pom 不变时该层
# 跨 run / 跨分支命中，配合 registry buildcache 复用。
#
# 不要用 RUN --mount=type=cache：cache-mount 的内容不随 CACHED 层回放，
# 换 builder 后该层虽显示 CACHED 但 .m2 为空，package 阶段会全量重下。
COPY pom.xml ./
COPY yudao-dependencies/pom.xml                                   yudao-dependencies/
COPY yudao-framework/pom.xml                                      yudao-framework/
COPY yudao-framework/yudao-common/pom.xml                         yudao-framework/yudao-common/
COPY yudao-framework/yudao-spring-boot-starter-mybatis/pom.xml    yudao-framework/yudao-spring-boot-starter-mybatis/
COPY yudao-framework/yudao-spring-boot-starter-redis/pom.xml      yudao-framework/yudao-spring-boot-starter-redis/
COPY yudao-framework/yudao-spring-boot-starter-web/pom.xml        yudao-framework/yudao-spring-boot-starter-web/
COPY yudao-framework/yudao-spring-boot-starter-security/pom.xml   yudao-framework/yudao-spring-boot-starter-security/
COPY yudao-framework/yudao-spring-boot-starter-websocket/pom.xml  yudao-framework/yudao-spring-boot-starter-websocket/
COPY yudao-framework/yudao-spring-boot-starter-monitor/pom.xml    yudao-framework/yudao-spring-boot-starter-monitor/
COPY yudao-framework/yudao-spring-boot-starter-protection/pom.xml yudao-framework/yudao-spring-boot-starter-protection/
COPY yudao-framework/yudao-spring-boot-starter-job/pom.xml        yudao-framework/yudao-spring-boot-starter-job/
COPY yudao-framework/yudao-spring-boot-starter-mq/pom.xml         yudao-framework/yudao-spring-boot-starter-mq/
COPY yudao-framework/yudao-spring-boot-starter-excel/pom.xml      yudao-framework/yudao-spring-boot-starter-excel/
COPY yudao-framework/yudao-spring-boot-starter-test/pom.xml       yudao-framework/yudao-spring-boot-starter-test/
COPY yudao-framework/yudao-spring-boot-starter-biz-tenant/pom.xml          yudao-framework/yudao-spring-boot-starter-biz-tenant/
COPY yudao-framework/yudao-spring-boot-starter-biz-data-permission/pom.xml yudao-framework/yudao-spring-boot-starter-biz-data-permission/
COPY yudao-framework/yudao-spring-boot-starter-biz-ip/pom.xml              yudao-framework/yudao-spring-boot-starter-biz-ip/
COPY yudao-server/pom.xml                                         yudao-server/
COPY yudao-module-system/pom.xml                                  yudao-module-system/
COPY yudao-module-infra/pom.xml                                   yudao-module-infra/
COPY yudao-module-fms/pom.xml                                     yudao-module-fms/
COPY yudao-module-ai/pom.xml                                      yudao-module-ai/

# excludeGroupIds 排除本 reactor 自己的 groupId：go-offline 无法解析尚未构建
# 出来的模块间依赖（yudao-server → yudao-module-system 等），不排除会直接失败。
# go-offline 也不保证拉全，缺口由下面的 package 阶段自动补下。
RUN mvn -B dependency:go-offline -DexcludeGroupIds=cn.iocoder.boot

# 单测由 CI 的 Test 腿承担，此处跳过
COPY . .
RUN mvn -B package -DskipTests

######################################################################
# 运行层
######################################################################
FROM eclipse-temurin:25-jre AS production
WORKDIR /app
ENV TZ=Asia/Shanghai

# HEALTHCHECK 要有 HTTP 客户端，temurin 的 Ubuntu 基础镜像不保证自带 curl
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# 不以 root 运行：这是会被前端反代到公网入口的服务
RUN groupadd --system app \
 && useradd --system --gid app --home-dir /app --no-create-home app \
 && mkdir -p /data/logs /data/xq-erp/uploads \
 && chown -R app:app /app /data

# finalName 固定为 artifactId，产物路径确定
COPY --from=builder --chown=app:app /app/yudao-server/target/yudao-server.jar app.jar

# JVM 参数与全部 Spring 配置经容器环境变量注入（compose 的 environment），
# 换配置不需要重建镜像
ENV JAVA_OPTS=""

# server.port 在 application-prod.yaml 里默认 48080，不是通用模板的 8080
EXPOSE 48080

USER app

# start-period 给足：模块多 + quartz 集群 + AI 依赖，冷启动明显慢于普通 Spring Boot 应用
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD curl -fsS http://127.0.0.1:48080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
