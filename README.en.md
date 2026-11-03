# HomeLog

---

## Project Overview

HomeLog is a digital household management platform designed for families to collaboratively manage medications, todo items, life notes, ID documents, and other important data.

## Tech Stack

### Backend
- **Framework**: Spring Boot 2.7.x
- **Database**: MySQL 8.0+
- **ORM**: MyBatis-Plus
- **Authentication**: JWT (JSON Web Token)
- **API Docs**: SpringDoc OpenAPI (Swagger)

### Frontend
- **Framework**: Vue 3 + Composition API
- **UI Library**: Element Plus
- **Routing**: Vue Router 4
- **State Management**: Pinia
- **Build Tool**: Vite
- **HTTP Client**: Axios

## Core Features

| Module | Description |
|--------|-------------|
| Drug Management | Family medication records, expiration reminders |
| Todo Items | Task assignment, notifications |
| Life Notes | Diary, photo albums |
| ID Documents | ID cards, insurance cards, etc. |
| Custom Forms | Flexible data entry structures |
| Global Search | Cross-module quick search |
| Import/Export | CSV/Excel data migration |

## System Features

- **Multi-member Collaboration**: Family member invitation, role management
- **Elderly Mode**: Enlarged fonts, simplified interface for seniors
- **Data Security**: JWT authentication, private field protection
- **Responsive Design**: Desktop and mobile support
- **PWA Support**: Installable as desktop/mobile app
- **Notifications**: Built-in message center, email reminders

## Project Structure

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

## Quick Start

### Prerequisites

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+

### Backend Setup

```bash
cd homelog-backend

# Configure database (application-dev.yml)

# Build and run
mvn clean package -DskipTests
java -jar target/homelog-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Frontend Setup

```bash
cd homelog-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Visit `http://localhost:5173` to access the application.

## API Documentation

After starting the backend:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Environment Variables

### Backend Configuration (application.yml)

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

## Development Guide

See [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) for details.

## License

For personal learning and communication purposes only.