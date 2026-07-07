# Known-Good Baselines

## Purpose

Record the latest verified project state so future AI sessions can tell whether a failure is new or pre-existing.

This file is lightweight. Record only meaningful baselines, not every local command run.

## Baselines

| Date | Source | Git State | Scope | Commands Passed | Known Failures | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-07-07 | local | commit `957c288e` (clean tree) | full | `mvn clean install -DskipTests` | none | commit message 标注 "full-green verification"；154 reactor 模块构建通过 | 18 域 + notify 子系统 codegen 骨架已就绪；本基线为 2026-07-07 综合审计整改（plan-2026-07-07-1915-1）开始前的最近已知良好状态 |
| 2026-07-07 | local | commit `957c288e` + dirty (orm.xml 4 处表名修正、文档对齐) | package (master-data/b2b/cs/logistics/contract 5 域 dao + codegen) | `mvn clean install -DskipTests -pl :app-erp-b2b-dao,:app-erp-cs-dao,:app-erp-logistics-dao,:app-erp-contract-dao -am` | none | 本仓库工作树（执行中） | plan-2026-07-07-1915-1 Phase 1（C-1 表名双前缀修正）的增量构建证据；生成 `_app.orm.xml` 表名已正确为 `erp_md_partner/material/employee` |

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
