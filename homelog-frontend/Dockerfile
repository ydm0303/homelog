# ============================================================================
# 前端 Dockerfile - Vue 3 + Vite + Nginx
# ============================================================================

# 构建阶段
FROM node:20-alpine AS builder

# 通过 corepack 启用 pnpm，版本取 package.json 的 packageManager 字段
RUN corepack enable && corepack prepare pnpm@10.32.1 --activate

WORKDIR /app

# 复制 lockfile 并安装依赖（--frozen-lockfile 保证与 pnpm-lock.yaml 一致）
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# 复制源码并构建
COPY . .
RUN pnpm run build

# 运行阶段 - Nginx
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]