# 家记项目 Agent 角色清单

创建时间：2026-11-01
项目：家记 - 私有模块化家庭记录 PWA 平台
目录：`.claude/agents/`

---

## 角色总览（共 13 个）

| # | Agent ID | 角色名称 | 核心职责 | 主要交付物目录 |
|---|----------|----------|----------|----------------|
| 1 | `product-manager` | 产品需求产品经理 | PRD、用户故事、功能规划、优先级、验收标准 | `docs/prd/` |
| 2 | `business-analyst` | 业务分析师 (BA) | 业务流程、规则提取、数据字典、AC 细化、影响分析 | `docs/ba/` |
| 3 | `ux-designer` | UX 设计师 | 交互原型、线框图、设计系统、适老化、交互规范 | `docs/ux/` |
| 4 | `architect` | 架构师 | 总体架构、技术选型、ADR、非功能需求、技术评审 | `docs/arch/` |
| 5 | `ddd-modeler` | DDD 领域建模专家 | 限界上下文、聚合根、领域事件、仓储契约 | `docs/ddd/` |
| 6 | `api-designer` | API 设计师 | REST 设计、OpenAPI 3.0、错误码、安全约定 | `docs/api/` |
| 7 | `dba-designer` | DBA 数据库设计师 | 物理表设计、ER 图、索引优化、迁移脚本、备份恢复 | `docs/dba/` |
| 8 | `backend-developer` | 后端开发工程师 | Spring Boot 实现、领域落地、API 实现、单测、可观测 | `src/main/java/com/jiaji/` |
| 9 | `frontend-developer` | 前端开发工程师 | Vue3 PWA、动态表单、组件库、响应式、构建部署 | `client/` |
| 10 | `fullstack-developer` | 全栈开发工程师 | 端到端交付、共享类型、联调、Docker、CI/CD | 完整功能分支 |
| 11 | `code-reviewer` | 代码评审与重构工程师 | PR 审查、重构方案、技术债、质量门禁、最佳实践 | `docs/review/` |
| 12 | `test-engineer` | 测试工程师 | 测试策略、用例设计、自动化、专项测试、缺陷管理 | `docs/test/` |
| 13 | `documentation-engineer` | 文档工程师 | 文档体系、规范模板、文档站点、治理流程、知识沉淀 | `docs/`, `.vitepress/` |

---

## 协作流向

```
PM (需求) 
  → BA (业务规则/AC) 
    → Architect + DDD Modeler (架构/领域模型) 
      → API Designer + DBA (接口契约/物理模型) 
        → Backend + Frontend + Fullstack (并行开发) 
          → Code Reviewer (质量把关) 
            → Test Engineer (验收测试) 
              → Documentation Engineer (文档沉淀)
                ↓
              UX Designer (全程参与：原型→设计系统→验收)
```

---

## 文档目录结构

```
docs/
├── prd/              # 产品需求文档
├── ba/               # 业务分析文档
├── ux/               # UX 设计文档
├── arch/             # 架构设计文档
├── ddd/              # DDD 领域建模文档
├── api/              # API 设计文档
├── dba/              # 数据库设计文档
├── review/           # 代码评审与重构文档
├── test/             # 测试文档
└── (由 documentation-engineer 维护索引与站点)
```

---

## 使用方式

在 Claude Code 中调用特定 Agent：

```bash
# 例如：让产品经理编写新模块的 PRD
/agent product-manager "为「家庭台账」模块编写详细 PRD，包含用户故事和验收标准"

# 例如：让架构师评审技术方案
/agent architect "评审自定义表单引擎的技术方案，输出 ADR"

# 例如：让 DBA 设计新表索引
/agent dba-designer "为 jj_record 表设计复合索引，优化按 space_id+module_id+remind_date 的查询"
```

---

## 项目上下文锚点

所有 Agent 共享的核心参考文档：
- `家记-产品文档(PRD).md` - 产品需求基线
- `家记-技术实现文档.md` - 技术实现基线

核心术语统一：
- 产品名：家记 / Slogan：家里的事，都记在这里
- 核心模型：家庭空间、三级权限、模块化插拔、自定义表单、EAV+JSON
- 技术栈：Vue3 + Spring Boot 3 + MySQL 8.0 + PWA + JWT + QQ SMTP
- 部署：Render/Railway + Aiven MySQL + HTTPS + Docker