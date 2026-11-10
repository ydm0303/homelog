# 家记项目 - 开发环境快速启动指南

## 📋 前置要求

- **Java 8** (已安装：`java version "1.8.0_281"`)
- **Maven 3.6+** (已安装)
- **MySQL 8.0** (已运行，已执行 `db/migration/V1__init.sql`)
- **Node.js 18+** (需安装)
- **npm/pnpm** (随 Node.js 安装)

---

## 🚀 后端启动

### 1. 进入后端目录
```bash
cd d:/CodexWork/HomeLog/homelog-backend
```

### 2. 配置数据库口令（不要写进 application.yml）

数据库连接全部走环境变量 / 本地私有配置，**仓库里不含任何明文口令**。

**方式一（推荐）：`application-local.yml`**

在 `homelog-backend/src/main/resources/` 下新建 `application-local.yml`（已被 `.gitignore` 忽略，不会入库）：

```yaml
spring:
  datasource:
    username: root
    password: 你的数据库口令
```

`application.yml` 已通过 `spring.config.import` 自动加载它，优先级最高，换机器只改这个文件。

**方式二：环境变量**

```bash
# Windows PowerShell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库口令"
# 可选：$env:DB_HOST / $env:DB_PORT / $env:DB_NAME
```

其余可配置项（`application.yml`）：
- `homelog.jwt.secret` ← `JWT_SECRET`
- `spring.mail.username` / `password` ← `MAIL_USERNAME` / `MAIL_PASSWORD`
- `homelog.remind.from` ← `MAIL_USERNAME`
- `homelog.storage.local.base-path` ← 默认 `./uploads`

### 3. 编译并运行
```bash
# 编译
mvn clean compile -DskipTests

# 运行 (开发模式)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端将在 `http://localhost:8080/api` 启动
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 健康检查: `http://localhost:8080/actuator/health`（actuator 不带 `/api` 前缀）

---

## 🌐 前端启动

### 1. 安装 Node.js 18+
- 下载: https://nodejs.org/ (推荐 LTS 版本)
- 验证: `node --version` 应显示 v18+

### 2. 进入前端目录
```bash
cd d:/CodexWork/HomeLog/homelog-frontend
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

# Flyway 迁移（口令用环境变量传入，不要写在命令行历史里）
mvn flyway:migrate -Dflyway.url=jdbc:mysql://localhost:3306/homelog -Dflyway.user=root -Dflyway.password=$env:DB_PASSWORD
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
cd homelog-backend
docker build -t homelog-backend .

# 前端
cd homelog-frontend
docker build -t homelog-frontend .
```

### 使用 Docker Compose (需创建 docker-compose.yml)
> 口令从同目录 `.env` 文件读取（`.env` 已在 `.gitignore` 中），不要写死在 compose 里。

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:?请在 .env 中设置 DB_PASSWORD}
      MYSQL_DATABASE: homelog
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build: ./homelog-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/homelog
      SPRING_DATASOURCE_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD:?请在 .env 中设置 DB_PASSWORD}
    depends_on:
      - mysql

  frontend:
    build: ./homelog-frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

`.env`（不放仓库）：
```bash
DB_PASSWORD=你的数据库口令
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
- [ ] **消息中心**：顶部铃铛图标显示未读数，点击进入列表页可标记已读、跳转详情
- [ ] **全局搜索**：顶部搜索框/移动端抽屉，跨模块搜索、筛选、高亮、分页
- [ ] 设置中心：成员管理、模块管理、**导出导入**、适老化设置
- [ ] **药品 CSV 导入**：预检模式返回错误详情、正式导入入库、错误 CSV 下载
- [ ] **数据导出**：单模块 Excel、全空间 ZIP (Excel+图片)、流式下载
- [ ] 长辈模式切换生效 (字号变大、按钮变大)
- [ ] PWA: 移动端浏览器可"添加到主屏幕"

---

## 🐛 常见问题

### MySQL 连接失败
- 检查 MySQL 服务是否运行: `net start mysql80`
- 检查端口 3306 是否被占用
- 确认 `DB_PASSWORD` 环境变量（或 `application-local.yml` 中的口令）正确

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
├── homelog-backend/                   # Spring Boot 2.7.18 后端
│   ├── src/main/java/com/homelog/     # Java 源码
│   ├── src/main/resources/            # 配置文件
│   ├── pom.xml                        # Maven 配置
│   └── Dockerfile                     # 后端镜像
├── homelog-frontend/                  # Vue 3 前端
│   ├── src/                           # 源码
│   ├── public/                        # 静态资源
│   ├── package.json                   # 依赖配置
│   ├── vite.config.ts                 # Vite 配置
│   ├── Dockerfile                     # 前端镜像
│   └── nginx.conf                     # Nginx 配置
├── docs/                              # 文档
├── .github/workflows/                 # CI/CD 流水线
└── CLAUDE.md                          # 项目记忆
```

---

## 🔗 关键接口地址

| 功能 | 接口 | 方法 |
|------|------|------|
| 登录 | `/api/auth/login` | POST |
| 注册 | `/api/auth/register` | POST |
| 刷新 Token | `/api/auth/refresh` | POST |
| 获取用户信息 | `/api/auth/detail` | GET |
| 修改密码 | `/api/auth/password/update` | POST |
| 获取启用模块 | `/api/meta/modules/list` | GET |
| 获取所有模块 | `/api/meta/modules/all` | GET |
| 更新模块 | `/api/meta/modules/update` | POST |
| 创建模块 | `/api/meta/modules/create` | POST |
| 删除模块 | `/api/meta/modules/delete` | POST |
| 记录列表 | `/api/records/list` | GET |
| 记录详情 | `/api/records/detail` | GET |
| 创建记录 | `/api/records/create` | POST |
| 更新记录 | `/api/records/update` | POST |
| 删除记录 | `/api/records/delete` | POST |
| **消息列表** | `/api/messages` | GET |
| **未读数** | `/api/messages/unread-count` | GET |
| **标记已读** | `/api/messages/{id}/read` | PUT |
| **全部已读** | `/api/messages/read-all` | PUT |
| **全局搜索** | `/api/search` | GET |
| **文件上传** | `/api/files/upload` | POST |
| **导出数据** | `/api/export` | GET |
| **下载导出** | `/api/export/download/{filename}` | GET |
| **药品预检** | `/api/import/drug/csv/preview` | POST |
| **药品导入** | `/api/import/drug/csv` | POST |
| 成员列表 | `/api/space/members/list` | GET |
| 添加成员 | `/api/space/members/create` | POST |
| 更新成员 | `/api/space/members/update` | POST |
| 删除成员 | `/api/space/members/delete` | POST |

---

*文档版本：v1.0 | 更新日期：2026-11-03*