# 家记项目 - 开发环境快速启动指南

## 📋 前置要求

- **Java 8** (已安装：`java version "1.8.0_281"`)
- **Maven 3.6+** (已安装)
- **MySQL 8.0** (已运行，已执行 `V1__init.sql`)
- **Node.js 18+** (需安装)
- **npm/pnpm** (随 Node.js 安装)

---

## 🚀 后端启动

### 1. 进入后端目录
```bash
cd d:/CodexWork/HomeLog/jiaji-backend
```

### 2. 修改配置 (如需)
编辑 `src/main/resources/application-dev.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jiaji?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: root
    password: ling3.41592654

jiaji:
  jwt:
    secret: jiaji-dev-secret-key-change-in-production
  mail:
    username: your-qq@qq.com
    password: your-16-char-auth-code
  frontend:
    base-url: http://localhost:5173
```

### 3. 编译并运行
```bash
# 编译
mvn clean compile -DskipTests

# 运行 (开发模式)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端将在 `http://localhost:8080/api` 启动
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- 健康检查: `http://localhost:8080/api/actuator/health`

---

## 🌐 前端启动

### 1. 安装 Node.js 18+
- 下载: https://nodejs.org/ (推荐 LTS 版本)
- 验证: `node --version` 应显示 v18+

### 2. 进入前端目录
```bash
cd d:/CodexWork/HomeLog/jiaji-frontend
```

### 3. 安装依赖
```bash
npm install
# 或使用 pnpm (更快)
pnpm install
```

### 4. 启动开发服务器
```bash
npm run dev
# 或
pnpm dev
```

前端将在 `http://localhost:5173` 启动
- 自动代理 `/api` 请求到后端 `http://localhost:8080`
- PWA 功能在开发环境可用

---

## 🔧 常用命令

### 后端
```bash
# 运行测试
mvn test

# 打包 JAR
mvn package -DskipTests

# Flyway 迁移
mvn flyway:migrate -Dflyway.url=jdbc:mysql://localhost:3306/jiaji -Dflyway.user=root -Dflyway.password=ling3.41592654

# 代码格式化
mvn spotless:apply
```

### 前端
```bash
# 类型检查
npm run type-check  # 或 npx vue-tsc --noEmit

# 代码检查
npm run lint

# 代码格式化
npm run format

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

---

## 🐳 Docker 部署 (可选)

### 构建镜像
```bash
# 后端
cd jiaji-backend
docker build -t jiaji-backend .

# 前端
cd jiaji-frontend
docker build -t jiaji-frontend .
```

### 使用 Docker Compose (需创建 docker-compose.yml)
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ling3.41592654
      MYSQL_DATABASE: jiaji
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build: ./jiaji-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/jiaji
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ling3.41592654
    depends_on:
      - mysql

  frontend:
    build: ./jiaji-frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

```bash
docker-compose up -d
```

---

## ✅ 验证清单

启动后验证以下功能：

- [ ] 访问 `http://localhost:5173` 显示登录页
- [ ] 注册新用户 (自动创建默认空间)
- [ ] 登录后进入首页仪表盘
- [ ] 侧边菜单显示 4 个模块：药品档案、家庭待办、生活记事相册、证件档案
- [ ] 点击任意模块进入列表页
- [ ] 新增记录、编辑、删除功能正常
- [ ] 详情页显示字段、图片、标签
- [ ] 设置中心：成员管理、模块管理、导出导入、适老化设置
- [ ] 长辈模式切换生效 (字号变大、按钮变大)
- [ ] PWA: 移动端浏览器可"添加到主屏幕"

---

## 🐛 常见问题

### MySQL 连接失败
- 检查 MySQL 服务是否运行: `net start mysql80`
- 检查端口 3306 是否被占用
- 确认密码 `ling3.41592654` 正确

### Maven 编译慢
- 使用阿里云镜像源 (在 `~/.m2/settings.xml` 配置)
- 或设置环境变量: `MAVEN_OPTS="-Xmx1024m"`

### 前端依赖安装失败
- 使用淘宝镜像: `npm config set registry https://registry.npmmirror.com`
- 或使用 pnpm: `npm install -g pnpm && pnpm install`

### 端口冲突
- 后端默认 8080，前端默认 5173
- 修改 `application-dev.yml` `server.port` 或 `vite.config.ts` `server.port`

---

## 📁 目录结构概览

```
HomeLog/
├── db/migration/V1__init.sql          # 数据库初始化脚本
├── jiaji-backend/                     # Spring Boot 2.7 后端
│   ├── src/main/java/com/jiaji/       # Java 源码
│   ├── src/main/resources/            # 配置文件
│   ├── pom.xml                        # Maven 配置
│   └── Dockerfile                     # 后端镜像
├── jiaji-frontend/                    # Vue 3 前端
│   ├── src/                           # 源码
│   ├── public/                        # 静态资源
│   ├── package.json                   # 依赖配置
│   ├── vite.config.ts                 # Vite 配置
│   ├── Dockerfile                     # 前端镜像
│   └── nginx.conf                     # Nginx 配置
├── docs/ux/                           # UX 设计文档
├── .github/workflows/ci-cd.yml        # CI/CD 流水线
└── CLAUDE.md                          # 项目记忆
```

---

## 🔗 关键接口地址

| 功能 | 接口 | 方法 |
|------|------|------|
| 登录 | `/api/auth/login` | POST |
| 注册 | `/api/auth/register` | POST |
| 刷新 Token | `/api/auth/refresh` | POST |
| 获取用户信息 | `/api/auth/me` | GET |
| 获取模块列表 | `/api/meta/modules` | GET |
| 获取所有模块 | `/api/meta/modules/all` | GET |
| 记录列表 | `/api/records` | GET |
| 记录详情 | `/api/records/{id}` | GET |
| 创建记录 | `/api/records` | POST |
| 更新记录 | `/api/records/{id}` | PUT |
| 删除记录 | `/api/records/{id}` | DELETE |
| 导出数据 | `/api/export/all` | GET |
| 药品导入 | `/api/import/drug/csv` | POST |

---

*文档版本：v1.0 | 更新日期：2026-11-02*