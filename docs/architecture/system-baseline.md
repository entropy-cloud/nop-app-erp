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

领域工程分两层：核心业务域（5 个，进销存+财务）与扩展业务域（5 个，资产/项目/制造/质量/维护）。依赖方向（DAG）：

```
master-data ← inventory ← purchase/sales ← finance
                                       ↑
assets/projects/manufacturing/quality/maintenance（扩展域，各自依赖核心域）
```

### 核心业务域

| 领域工程 | appName | 表前缀 | 类名前缀 | 权威模型 |
|----------|---------|--------|----------|----------|
| `app-erp-master-data` | `app-erp-md` | `erp_md_` | `ErpMd*` | `master-data/model/app-erp-master-data.orm.xml` |
| `app-erp-inventory` | `app-erp-inv` | `erp_inv_` | `ErpInv*` | `inventory/model/app-erp-inventory.orm.xml` |
| `app-erp-purchase` | `app-erp-pur` | `erp_pur_` | `ErpPur*` | `purchase/model/app-erp-purchase.orm.xml` |
| `app-erp-sales` | `app-erp-sal` | `erp_sal_` | `ErpSal*` | `sales/model/app-erp-sales.orm.xml` |
| `app-erp-finance` | `app-erp-fin` | `erp_fin_` | `ErpFin*` | `finance/model/app-erp-finance.orm.xml` |

### 扩展业务域

| 领域工程 | appName | 表前缀 | 类名前缀 | 权威模型 |
|----------|---------|--------|----------|----------|
| `app-erp-assets` | `app-erp-ast` | `erp_ast_` | `ErpAst*` | `assets/model/app-erp-assets.orm.xml` |
| `app-erp-projects` | `app-erp-prj` | `erp_prj_` | `ErpPrj*` | `projects/model/app-erp-projects.orm.xml` |
| `app-erp-manufacturing` | `app-erp-mfg` | `erp_mfg_` | `ErpMfg*` | `manufacturing/model/app-erp-manufacturing.orm.xml` |
| `app-erp-quality` | `app-erp-qa` | `erp_qa_` | `ErpQa*` | `quality/model/app-erp-quality.orm.xml` |
| `app-erp-maintenance` | `app-erp-mnt` | `erp_mnt_` | `ErpMnt*` | `maintenance/model/app-erp-maintenance.orm.xml` |

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

## 多租户策略（tenant model）

Nop 平台的多租户支持是**框架内置的薄层**，不是业务功能。平台机制见 `../nop-entropy/docs-for-ai/02-core-guides/tenant-model.md`。本节明确 nop-app-erp 的租户策略。

### 默认不启用租户

nop-app-erp **默认不启用多租户**（`useTenant` 默认 `false`）。开发阶段与单体部署场景下，所有实体不区分租户，框架完全忽略租户机制。

### 启用方式：按实体声明 useTenant

需要租户隔离时，在 `<entity>` 上声明 `useTenant="true"`，框架自动完成：
- **自动创建租户列**：`OrmEntityModelInitializer` 自动创建隐式列 `nopTenantId`（VARCHAR(32)，默认值 `"0"`，非空）。**orm.xml 不预置 `tenantId` 字段**。
- **自动填充**：save/insert、load、session 缓存、集合加载等所有入口自动填充 `nopTenantId`（从 `ContextProvider.currentTenantId()` 获取）。
- **自动过滤**：EQL 编译期与 SQL 生成自动追加 `nopTenantId = ?` 到 WHERE 子句。
- **缓存隔离**：Session 缓存与全局缓存按租户分区。

开发者**不需要**在业务代码中调用 `entity.setNopTenantId(...)`，也**不应该**在 orm.xml 中显式声明 `tenantId` 列。

### 混合使用

部分实体可启用租户、部分不启用，各自在 orm.xml 独立声明 `useTenant`，框架按实体模型分别处理。适合 ERP 场景：业务单据（采购/销售/库存）可启用租户隔离，全局主数据（币种/汇率/计量单位）可不启用（多租户共享）。

### 选型决策（待业务确认）

下表是初步建议，最终哪些实体启用租户取决于产品定位（单租户部署 vs SaaS 多租户），待业务确认后定稿：

| 实体类别 | 建议 useTenant | 理由 |
|----------|----------------|------|
| 业务单据（采购/销售/库存/凭证/工单等） | true（启用 SaaS 时） | 不同租户业务数据隔离 |
| 业务主数据（物料/SKU/往来单位/仓库/科目） | true（启用 SaaS 时） | 不同租户主数据隔离 |
| 全局主数据（币种/汇率/计量单位/国家/语言） | false | 多租户共享，避免重复维护 |
| 平台数据（用户/角色/权限/字典） | 复用 nop-auth 机制 | nop-auth 已内置租户隔离 |

### 硬规则

- **禁止在 orm.xml 预置 `tenantId` 字段**。租户列由框架在 `useTenant="true"` 时自动创建为 `nopTenantId`。
- **禁止在业务代码手动设置租户 ID**。框架自动填充，手动设置且不匹配上下文会抛跨租户异常。
- **跨租户操作**使用 `ContextProvider.runWithTenant()` / `runWithoutTenantId()`，不要直接修改实体租户字段。
- **原生 SQL 不自动过滤**：`dao().executeNativeSql(...)` 不经过 EQL 编译，需手动包含租户列。数据初始化 SQL 也必须手动包含。

## Stable Rules

- Model → Delta → Java decision order (see `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`)
- 每业务域一份 `model/app-erp-<domain>.orm.xml` 是该域持久化实体的唯一真相来源；生成文件（`_` 前缀、`_gen/`）不可手编辑
- Module dependency direction: web → service → dao → api; app aggregates web + service + delta
- Entity class names use the `Erp<Domain>` prefix (e.g., `ErpMdMaterial`, `ErpInvStockMove`) to avoid collision with nop built-in entities
- Dict namespace is `erp-<domain-short>/<dict-name>` (e.g., `erp-md/material-type`, `erp-fin/voucher-type`)
- **禁止在 orm.xml 预置 `tenantId` 字段**；租户隔离通过 `useTenant="true"` 声明，框架自动管理 `nopTenantId`（见上文"多租户策略"）
- Forbidden shortcuts: hand-editing generated code, bypassing `I*Biz` for cross-entity access without a documented reason, `extends RuntimeException` for business exceptions

## Update Rule

When the supported baseline changes, update this file in the same change.
