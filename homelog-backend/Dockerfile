# ============================================================================
# 后端 Dockerfile - Spring Boot 2.7 + Java 8 【适配Render Free】
# ============================================================================
# 构建阶段
FROM maven:3.8-openjdk-8-slim AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:8-jre

WORKDIR /app

# 安装必要工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户
RUN groupadd -r homelog && useradd -r -g homelog homelog

# 复制构建产物（artifactId = homelog-backend）
COPY --from=builder /app/target/homelog-backend-*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads && chown -R homelog:homelog /app

# 切换用户
USER homelog

# ========== 修改1：暴露端口改为10000（Render要求） ==========
EXPOSE 10000

# ========== 修改2：健康检查端口改成10000 ==========
# 如果你不用actuator，就改成你自己的 /common/health
# 方案A（沿用你actuator）：
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:10000/actuator/health || exit 1
# 方案B（用你自己写的健康接口，推荐，和reggie统一）
# HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
#    CMD curl -f http://localhost:10000/common/health || exit 1

# ========== 修改3：内存下调，Render免费实例上限512MB，防止OOM ==========
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod

# ========== 修改4：application.yml 里 server.port: 10000 要配套！ ==========
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
