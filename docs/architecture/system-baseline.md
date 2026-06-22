# System Baseline

## Purpose

Record the current supported implementation baseline for `nop-app-erp`.

## Current Baseline (bootstrap stage)

- Runtime shape: Quarkus application (single uber-jar), JVM mode
- Frontend stack: Baidu AMIS — JSON-driven pages in `.view.xml` files (available after codegen)
- Backend stack: Java 17+, Quarkus, Nop Platform (nop-entropy 2.0.0-SNAPSHOT)
- State management approach: server-driven AMIS pages; Nop ORM session/transaction scope
- Data access approach: Nop ORM (entity-based, generated DAO); XML models as source of truth（每业务域一份 `model/app-erp-<domain>.orm.xml`）
- Testing stack: JUnit 5 + nop-autotest (after codegen)
- Build and package tools: Maven multi-module, parent `io.github.entropy-cloud:nop-entropy`
- Deployment shape: JVM uber-jar; H2 for dev, MySQL/Oracle/PostgreSQL for prod (configurable via `application.yaml`)
- External platforms or enterprise systems this app must integrate with: `<to be defined when business domains are chosen>`

## Module Structure (after codegen)

nop-app-erp 采用**每业务域独立 Maven 工程**结构，由 `app-erp-app` 聚合启动。详细决策与命名规则见 `domain-module-split-analysis.md`。

领域工程依赖方向（DAG）：master-data ← inventory ← purchase/sales ← finance。

| 领域工程 | appName | 表前缀 | 类名前缀 | 权威模型 |
|----------|---------|--------|----------|----------|
| `app-erp-master-data` | `app-erp-md` | `erp_md_` | `ErpMd*` | `model/app-erp-master-data.orm.xml` |
| `app-erp-inventory` | `app-erp-inv` | `erp_inv_` | `ErpInv*` | `model/app-erp-inventory.orm.xml` |
| `app-erp-purchase` | `app-erp-pur` | `erp_pur_` | `ErpPur*` | `model/app-erp-purchase.orm.xml` |
| `app-erp-sales` | `app-erp-sal` | `erp_sal_` | `ErpSal*` | `model/app-erp-sales.orm.xml` |
| `app-erp-finance` | `app-erp-fin` | `erp_fin_` | `ErpFin*` | `model/app-erp-finance.orm.xml` |
| `app-erp-app`（聚合） | — | — | — | — |

每个领域工程内部由 `nop-cli gen` 生成标准 8 层骨架（遵循 `../nop-entropy/docs-for-ai/01-repo-map/domain-module-pattern.md`）：

| 子模块 | 职责 |
|--------|------|
| `app-erp-<domain>-codegen` | 代码生成入口（`postcompile/gen-orm.xgen`） |
| `app-erp-<domain>-api` | 对外 RPC 接口契约 |
| `app-erp-<domain>-dao` | 实体、DAO、`I*Biz` 接口 |
| `app-erp-<domain>-meta` | XMeta 与 i18n |
| `app-erp-<domain>-service` | BizModel 实现（`*.xbiz` + Java） |
| `app-erp-<domain>-web` | AMIS 页面（`*.view.xml`） |
| `app-erp-delta`（可选） | 对 nop-auth/nop-sys 的 Delta 扩展 |

## Stable Rules

- Model → Delta → Java decision order (see `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`)
- `model/app-erp.orm.xml` is the single source of truth for persisted entities; generated files (`_`-prefixed, `_gen/`) are never hand-edited
- Module dependency direction: web → service → dao → api; app aggregates web + service + delta
- Entity class names use the `Erp` prefix to avoid collision with nop built-in entities
- Dict namespace is `erp/<dict-name>`
- Forbidden shortcuts: hand-editing generated code, bypassing `I*Biz` for cross-entity access without a documented reason, `extends RuntimeException` for business exceptions

## Update Rule

When the supported baseline changes, update this file in the same change.
