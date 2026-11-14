# 家记 (HomeLog)

[English](#english) | [中文](#中文)

---

## 中文

### 项目简介

家记是一款面向家庭的数字化生活记录与管理平台，帮助家庭成员共同管理药品、待办事项、生活记事、证件信息等重要数据。

### 技术栈

#### 后端
- **框架**: Spring Boot 2.7.x
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis-Plus
- **认证**: JWT (JSON Web Token)
- **文档**: SpringDoc OpenAPI (Swagger)

#### 前端
- **框架**: Vue 3 + Composition API
- **UI库**: Element Plus
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **构建工具**: Vite
- **HTTP客户端**: Axios

### 核心功能

| 模块 | 说明 |
|------|------|
| 药品管理 | 家庭药品记录、过期提醒 |
| 待办事项 | 任务分配、提醒通知 |
| 生活记事 | 日记、相册记录 |
| 证件管理 | 身份证、医保卡等重要证件 |
| 自定义表单 | 灵活的数据录入结构 |
| 全局搜索 | 跨模块快速检索 |
| 数据导入导出 | CSV/Excel 数据迁移 |

### 系统特性

- **多成员协作**: 支持家庭成员邀请、权限管理
- **适老化模式**: 针对老年用户的放大字体、简化界面
- **数据安全**: JWT认证、私密字段保护
- **响应式设计**: 支持桌面端和移动端
- **PWA支持**: 可安装为桌面/移动应用
- **消息通知**: 内置消息中心，支持邮件提醒

### 项目结构

```
home-log/
├── homelog-backend/          # Spring Boot 后端
│   └── src/main/java/com/homelog/
│       ├── config/           # 配置类
│       ├── controller/       # 控制器
│       ├── dto/              # 数据传输对象
│       ├── entity/           # 实体类
│       ├── mapper/           # MyBatis Mapper
│       ├── security/         # 安全认证
│       ├── service/          # 业务逻辑
│       ├── task/             # 定时任务
│       └── vo/               # 视图对象
│
├── homelog-frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── views/            # 页面组件
│   │   ├── components/       # 通用组件
│   │   ├── composables/      # 组合式函数
│   │   ├── stores/           # Pinia 状态
│   │   └── api/              # API 接口
│   └── dist/                 # 构建产物
│
├── docs/                     # 项目文档
│   └── ux/                   # UX 设计规范
│
└── db/migration/             # Flyway 数据库迁移
```

### 快速开始

#### 前置要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+

#### 后端启动

```bash
cd homelog-backend

# 配置数据库连接信息

# 编译并运行
mvn clean package -DskipTests
java -jar target/homelog-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

#### 前端启动

```bash
cd homelog-frontend

# 安装依赖
npm install
# 或使用 pnpm
pnpm install

# 启动开发服务器
npm run dev
```

访问 `http://localhost:5173` 即可看到前端页面。

### API 文档

启动后端服务后，访问 Swagger 文档:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 环境变量

#### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/homelog
    username: root
    password: your_password
  
homelog:
  jwt:
    secret: your-jwt-secret-key
    access-token-expire: 86400000
    refresh-token-expire: 604800000
  storage:
    local:
      base-path: ./uploads
  remind:
    from: noreply@homelog.com
    lead-days: 7,1,0
  frontend:
    base-url: http://localhost:5173
```

### 开发指南

详见 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)

### 许可证

本项目仅供个人学习交流使用。

---

## English

### Project Overview

HomeLog is a digital household management platform designed for families to collaboratively manage medications, todo items, life notes, ID documents, and other important data.

### Tech Stack

#### Backend
- **Framework**: Spring Boot 2.7.x
- **Database**: MySQL 8.0+
- **ORM**: MyBatis-Plus
- **Authentication**: JWT (JSON Web Token)
- **API Docs**: SpringDoc OpenAPI (Swagger)

#### Frontend
- **Framework**: Vue 3 + Composition API
- **UI Library**: Element Plus
- **Routing**: Vue Router 4
- **State Management**: Pinia
- **Build Tool**: Vite
- **HTTP Client**: Axios

### Core Features

| Module | Description |
|--------|-------------|
| Drug Management | Family medication records, expiration reminders |
| Todo Items | Task assignment, notifications |
| Life Notes | Diary, photo albums |
| ID Documents | ID cards, insurance cards, etc. |
| Custom Forms | Flexible data entry structures |
| Global Search | Cross-module quick search |
| Import/Export | CSV/Excel data migration |

### System Features

- **Multi-member Collaboration**: Family member invitation, role management
- **Elderly Mode**: Enlarged fonts, simplified interface for seniors
- **Data Security**: JWT authentication, private field protection
- **Responsive Design**: Desktop and mobile support
- **PWA Support**: Installable as desktop/mobile app
- **Notifications**: Built-in message center, email reminders

### Project Structure

```
home-log/
├── homelog-backend/          # Spring Boot backend
│   └── src/main/java/com/homelog/
│       ├── config/           # Configuration
│       ├── controller/       # REST Controllers
│       ├── dto/              # Data Transfer Objects
│       ├── entity/           # Entity classes
│       ├── mapper/           # MyBatis Mappers
│       ├── security/         # Security & Auth
│       ├── service/          # Business logic
│       ├── task/             # Scheduled tasks
│       └── vo/               # View Objects
│
├── homelog-frontend/         # Vue 3 frontend
│   ├── src/
│   │   ├── views/            # Page components
│   │   ├── components/       # Shared components
│   │   ├── composables/      # Composables
│   │   ├── stores/           # Pinia stores
│   │   └── api/              # API definitions
│   └── dist/                 # Build output
│
├── docs/                     # Documentation
│   └── ux/                   # UX design specs
│
└── db/migration/             # Flyway migrations
```

### Quick Start

#### Prerequisites

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+

#### Backend Setup

```bash
cd homelog-backend


# Build and run
mvn clean package -DskipTests
java -jar target/homelog-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Frontend Setup

```bash
cd homelog-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Visit `http://localhost:5173` to access the application.

### API Documentation

After starting the backend:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### License

For personal learning and communication purposes only.