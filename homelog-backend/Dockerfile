# ============================================================================
# 后端 Dockerfile - Spring Boot 2.7 + Java 8
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
FROM openjdk:8-jre-slim

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

# 暴露端口
EXPOSE 8080

# 健康检查（注意：actuator 不带 /api 前缀，Controller 才带）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]