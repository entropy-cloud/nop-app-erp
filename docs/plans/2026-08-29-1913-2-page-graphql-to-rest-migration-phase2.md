# 2026-08-29-1913-2-page-graphql-to-rest-migration-phase2 剩余 58 个页面 /graphql 迁移

> Plan Status: active
> Last Reviewed: 2026-08-29
> Source: docs/plans/2026-08-29-1913-1-page-graphql-to-rest-migration.md (Deferred But Adjudicated)
> Related: 2026-08-29-1913-1-page-graphql-to-rest-migration
> Audit: required

## Current Baseline

- **已完成**：Phase 1 迁移 16 文件（inventory, sales, purchase, master-data, notify, logistics, drp, contract, aps 9 模块）✅
- **本计划范围**：剩余 58 文件（10 模块）
- **模块分布**：
  - finance: 14 文件
  - crm: 7 文件
  - quality: 6 文件
  - projects: 6 文件
  - hr: 5 文件
  - assets: 5 文件
  - manufacturing: 5 文件
  - maintenance: 4 文件
  - cs: 4 文件
  - b2b: 2 文件

## Goals

- 将剩余 58 个 `.page.yaml` 文件中的 `/graphql` 调用迁移到 `@query:`/`@mutation:` 前缀
- 确保全部 74 个页面符合 flux-only 约定

## Non-Goals

- 不修改 `view.xml` 文件
- 不修改测试文件中的 `/graphql`
- 不修改报表下载按钮（已修复为 `/p/` 路径）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/view-and-page-strategy.md`
- Skill Selection Basis: `nop-frontend-dev`

## Execution Plan

### Phase 1 - 按模块批量迁移

Status: in_progress
Targets: `module-{finance,crm,quality,projects,hr,assets,manufacturing,maintenance,cs,b2b}/erp-*-web/src/main/resources/_vfs/erp/*/pages/**/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 (1913-1) 完成

- [ ] **Finance 模块**（14 文件）
- [ ] **CRM 模块**（7 文件）
- [ ] **Quality 模块**（6 文件）
- [ ] **Projects 模块**（6 文件）
- [ ] **HR 模块**（5 文件）
- [ ] **Assets 模块**（5 文件）
- [ ] **Manufacturing 模块**（5 文件）
- [ ] **Maintenance 模块**（4 文件）
- [ ] **CS 模块**（4 文件）
- [ ] **B2B 模块**（2 文件）

Exit Criteria:

- [ ] 所有 58 个文件的 `/graphql` 调用已迁移到 `@query:`/`@mutation:`
- [ ] 全仓库 `grep "url: /graphql"` 返回 0

### Phase 2 - 验证

Status: planned
Targets: 全仓库
Skill: none

- Item Types: `Proof`

- [ ] `grep -r "url: /graphql" module-*/src/main/resources --include="*.page.yaml"` 返回 0
- [ ] 抽样验证 3-5 个代表性页面渲染正常

## Closure Gates

- [ ] 范围内行为完成
- [ ] 已运行验证（grep 检查）
- [ ] 独立结束审计完成