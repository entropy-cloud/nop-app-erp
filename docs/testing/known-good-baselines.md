# Known-Good Baselines

## Purpose

Record the latest verified project state so future AI sessions can tell whether a failure is new or pre-existing.

This file is lightweight. Record only meaningful baselines, not every local command run.

## Baselines

| Date | Source | Git State | Scope | Commands Passed | Known Failures | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-07-07 | local | commit `957c288e` (clean tree) | full | `mvn clean install -DskipTests` | none | commit message 标注 "full-green verification"；154 reactor 模块构建通过 | 18 域 + notify 子系统 codegen 骨架已就绪；本基线为 2026-07-07 综合审计整改（plan-2026-07-07-1915-1）开始前的最近已知良好状态 |
| 2026-07-07 | local | commit `957c288e` + dirty (orm.xml 4 处表名修正、文档对齐) | package (master-data/b2b/cs/logistics/contract 5 域 dao + codegen) | `mvn clean install -DskipTests -pl :app-erp-b2b-dao,:app-erp-cs-dao,:app-erp-logistics-dao,:app-erp-contract-dao -am` | none | 本仓库工作树（执行中） | plan-2026-07-07-1915-1 Phase 1（C-1 表名双前缀修正）的增量构建证据；生成 `_app.orm.xml` 表名已正确为 `erp_md_partner/material/employee` |
| 2026-07-08 | local | commit `aaf42335` + dirty (34 page.yaml 修复 + Playwright E2E 基础设施新增) | full + E2E | `mvn clean install -DskipTests`（154 模块，1:54）；`npx playwright test --workers=1`（34 spec，5.4m） | none | 本仓库工作树（plan-2026-07-08-0637-1 执行中） | E2E 冒烟套件全绿：10 看板 + 24 报表；page.yaml 修复 `/api/GenericApi`→`/graphql`（115 处）+ Map 字段选择移除（91 处）；运行时 JVM 参数 `service-public`/`allow-create-default-user`/`validate-page-model=false` |
| 2026-07-08 | local | dirty (ErpCsTicket.view.xml + ErpHrEmployee.view.xml 修复 + playwright config/e2e-runbook 移除 validate-page-model 覆盖 + 新增 KB suggestion spec) | full + E2E | `mvn clean install -DskipTests`（154 模块）；默认配置启动（`validate-page-model=true`，无 JVM 覆盖，11.3s 启动成功）；`npx playwright test --workers=1`（35 spec，5.6m） | none | 本仓库工作树（plan-2026-07-08-1107-1 执行中） | 页面模型校验安全网已恢复：修复 ErpCsTicket layout `=` 前缀 + 4 处 `/api/GenericApi`→`/graphql` + Map 字段选择移除；修复 ErpHrEmployee cell `visible` + `<option>` 结构；移除 `-Dnop.web.validate-page-model=false` 全局绕过；新增 KB suggestion 冒烟 spec（`suggestForTicket` GraphQL 200） |
| 2026-07-08 | local | dirty (21 张主数据 seed CSV + playwright.config.ts webServer 加 seed flag/fresh-DB reset + nop-entropy DataInitInitializer sessionFactory patch) | full + E2E (seeded DB) | `mvn clean install -DskipTests`（154 模块）；fresh-DB 启动 `-Dnop.orm.init-database-data=true`（21 表 0 冲突，11.4s）；`SKIP_WEBSERVER=1 npx playwright test --workers=1`（35 spec，5.7m）；平台单测 `TestDataInitInitializer`（4 spec） | none | 本仓库工作树（plan-2026-07-08-1234-1 执行中） | 部署期种子数据启用：21 张主数据 CSV（`_vfs/_init-data/`）经 `DataInitInitializer` 加载；修复平台 bug（`ormTemplate.sessionFactory` 在 Quarkus `@PostConstruct` 因 IoC 循环依赖为 null → NPE）；seed 经 GraphQL 抽样证实非空（material=4/partner=4/subject=8...，`createdBy='sys'` 自动填充）；E2E 在种子库上 0 回归 |

## When To Update

Update this file when:

- full typecheck/build/lint/test verification passes after a meaningful change
- a previously failing command becomes green and should be remembered
- a team intentionally accepts a known failing command and records it as a known failure, not as a passed command

## Rule

Do not mark a command as passed unless it actually ran in the current repository state.

`Commands Passed` must contain only passing commands. Put accepted failures in `Known Failures` with the reason and evidence.

A dirty working-tree baseline must name the changed files in `Notes` or link to a dated log/testing note that does.

`full` means all real verification commands configured in `docs/context/project-context.md`. Commands explicitly marked `none` are excluded and should be noted.
