# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

家记 (HomeLog / "Jiaji") is a private, modular family-record web app — families collaboratively manage medications, todos, notes, and ID documents. Slogan: "家里的事，都记在这里". Backend is Spring Boot 2.7 + Java 8; frontend is Vue 3 + Vite + TypeScript.

This is a two-part monorepo (no root package manager or workspace tooling):

- `homelog-backend/` — Spring Boot 2.7.18, Java 8, Maven, MyBatis-Plus, MySQL 8, JWT auth, SpringDoc OpenAPI
- `homelog-frontend/` — Vue 3 (Composition API), Element Plus, Pinia, Vue Router, Axios

Reference docs (authoritative for setup and API surface): `README.md`, `DEVELOPMENT_GUIDE.md`, `docs/`.

Local development is on **Windows 11** (Git Bash / PowerShell). Commands below are shell-agnostic; the local CI script is PowerShell (`./scripts/ci.ps1`).

## Common commands

### Backend (`homelog-backend/`, run from that dir)

```bash
mvn spring-boot:run                                  # run (active profile = "local" by default)
mvn spring-boot:run -Dspring-boot.run.profiles=prod  # run another profile
mvn clean package -DskipTests                        # build executable jar
mvn test                                             # run tests (currently none — CI uses -DskipTests)
mvn test -Dtest=ClassName#methodName                 # run a single test
```

### Frontend (`homelog-frontend/`, run from that dir)

**Use pnpm only** — `pnpm-lock.yaml` is the single lockfile (`packageManager` is pinned in `package.json`, CI and Docker both use pnpm). Do not run `npm install` here; `package-lock.json` is gitignored, and `pnpm install --frozen-lockfile` is what CI verifies against.

```bash
pnpm install                  # or pnpm install --frozen-lockfile (CI-equivalent)
pnpm dev            # dev server at :5173, proxies /api → :8080
pnpm build          # production build (vite build only — does NOT type-check)
pnpm type-check     # vue-tsc --noEmit
pnpm lint           # eslint + style guard (node scripts/lint-styles.mjs)
pnpm lint:fix       # eslint --fix only
pnpm format         # prettier on src/**
pnpm test           # vitest run
npx vitest run src/path/to/file.spec.ts   # single test file (or: pnpm vitest run ...)
pnpm test:watch     # vitest watch
```

### Local CI self-check (PowerShell, repo root)

```bash
./scripts/ci.ps1                        # full check (backend package + frontend lint + build)
./scripts/ci.ps1 -Target frontend       # frontend only
./scripts/ci.ps1 -StrictLint -RunTests  # strict lint + backend tests
```

## Architecture

### Core data model: schema-driven records (EAV + JSON)

The central design is a generic, metadata-driven record store. Do not add per-module tables — a new "module" is rows in `jj_module`/`jj_field`, not a new schema.

- `jj_module` — a module (a "form type"). Four built-in preset modules are seeded with fixed `module_key`s: `drug` (药品档案), `todo` (家庭待办), `note` (生活记事相册), `idoc` (证件档案); `module_type=2` is a user-created custom form.
- `jj_field` — field definitions for a module (`field_key`, `field_label`, `field_type` ∈ text/textarea/number/date/image/member/select/multi_select/tag/boolean, `is_required`, `is_private`, `options_json`).
- `jj_record` — every record is a row here; actual field values live in `data_json` (`{fieldKey: value}`) and images in `images_json` (relative `/uploads/...` paths). `title`, `tags`, `remind_date` are promoted columns for list/search/reminders.

The frontend renders forms and detail views dynamically from field metadata — see `homelog-frontend/src/components/DynamicFieldRenderer.vue` and `src/stores/module.ts`.

### Multi-tenancy: the "Space" model (single-space)

All data is scoped by `space_id`. `jj_space` (family space) + `jj_space_member` (role: `1=admin`, `2=edit`, `3=readonly`). The app currently collapses this to a single space — `CurrentUserService.DEFAULT_SPACE_ID = 1` (`homelog-backend/src/main/java/com/homelog/security/CurrentUserService.java`), the frontend hides the space switcher, and `spaceId` defaults to `1`. When adding code, route current-user/space/role resolution through `CurrentUserService` rather than hardcoding `userId=1`/`spaceId=1`.

Privacy is field/record-level `is_private` flags (admin-only visibility), not encryption.

### Backend layering (`com.homelog`)

`controller` → `service` → `mapper` (MyBatis-Plus `BaseMapper`, no XML mappers). `entity`/`dto`/`vo` are separated; responses wrap in `PageResult`/`*VO`. Cross-cutting pieces:

- `security/` — JWT: `JwtAuthenticationFilter` → `JwtTokenProvider`, `CurrentUserService`, `UserDetailsServiceImpl`. Roles map to `ROLE_ADMIN`/`ROLE_EDIT` via `getCurrentRole()`.
- `config/` — `SecurityConfig` (permit-all paths, CORS), `GlobalExceptionHandler`, `OpenApiConfig`.
- `task/` — scheduled jobs (`RemindTask` email reminders, `DBTask`).
- `service/` — `RecordService`, `ImportService`/`ExportService` (CSV/Excel), `FileStorageService`, `MailService`, `MessageService`, `SearchService`.

### Frontend layering (`homelog-frontend/src/`)

- `api/index.ts` — single Axios instance; request interceptor injects `Authorization: Bearer`, response interceptor auto-refreshes the access token on 401 and normalizes error toasts. All API groups are exported here (`authApi`, `recordApi`, `metaApi`, `spaceApi`, `messageApi`, etc.).
- `stores/` — Pinia. `auth.ts` holds JWT + user + role (tokens in `localStorage`); `module.ts` caches module/field metadata.
- `router/index.ts` — route guard restores auth on refresh (`initAuth`) and enforces `requiresAuth`/`requiresAdmin`.
- `views/` — pages (`Dashboard`, `ModuleList`, `RecordDetail`, `RecordEdit`, `Settings`, `MessageCenter`, `GlobalSearch`).
- `layouts/` — `DefaultLayout` (authenticated shell) vs `AuthLayout` (login/register).
- Components auto-register via `unplugin-vue-components` + Element Plus resolver (`vite.config.ts`); `@` aliases to `src`.

## Configuration & environment

Spring profile files live in `homelog-backend/src/main/resources/`:

- `application.yml` — only sets `spring.profiles.active: local`.
- `application-local.yml` — full local config (committed). **Note:** it currently contains a hardcoded MySQL password and points at DB `homelog`, while the Flyway migration `db/migration/V1__init.sql` creates DB `jiaji` — the DB name is inconsistent across the repo (README says `homelog`, migration script says `jiaji`).
- `application-prod.yml` — full prod config, driven by env vars (`DB_URL`/`DB_USER`/`DB_PASS`, `JWT_SECRET`, `MAIL_USERNAME`/`MAIL_PASSWORD`, `OSS_*`).

`.gitignore` lists `application-local.yml`/`application-dev.yml`/`application-prod.yml` as ignored, but `application-local.yml` and `application-prod.yml` are already tracked in git (committed before the ignore rule). There is no committed `application-dev.yml`; the README's `--spring.profiles.active=dev` reference has no backing file — use `local` instead.

Dev-server endpoints: backend `:8080` (API context `/api`, actuator `/actuator` without the `/api` prefix, Swagger UI `/swagger-ui.html`); frontend `:5173` proxying `/api` → `:8080`.

## Tests & CI

- Frontend unit tests use Vitest + `@vue/test-utils` + jsdom, colocated in `src/**/__tests__/*.spec.ts` (`vite.config.ts`/`vitest.config.ts`).
- Backend has `spring-boot-starter-test` wired but no tests yet.
- GitHub Actions (`.github/workflows/`): `ci.yml` builds backend jar + frontend dist (with `-DskipTests`), `frontend-quality.yml` runs `type-check` + `vitest` + `lint`. Active branches: `main`, `develop`, `codex/**`.

## Team agent workflow

`.claude/agents/` defines 13 role subagents (PM, BA, UX, architect, DDD, API designer, DBA, backend/frontend/fullstack dev, reviewer, test, docs) with a documented collaboration flow (see `.claude/agents/README.md`). Deliverables land under `docs/{prd,ba,ux,arch,ddd,api,dba,review,test}`.
