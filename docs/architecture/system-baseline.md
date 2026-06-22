# 系统基线

## 目的

记录 `nop-app-erp` 当前支持的实现基线。

## 当前基线（bootstrap 阶段）

- 运行时形态：Quarkus 应用（单 uber-jar），JVM 模式
- 前端栈：百度 AMIS — `.view.xml` 文件中的 JSON 驱动页面（代码生成后可用）
- 后端栈：Java 17+、Quarkus、Nop Platform（nop-entropy 2.0.0-SNAPSHOT）
- 状态管理方式：服务端驱动的 AMIS 页面；Nop ORM 会话/事务范围
- 数据访问方式：Nop ORM（基于实体，生成的 DAO）；XML 模型作为真相源（每业务域一份 `<domain>/model/app-erp-<domain>.orm.xml`）
- 测试栈：JUnit 5 + nop-autotest（代码生成后）
- 构建和打包工具：Maven 多模块，父项目 `io.github.entropy-cloud:nop-entropy`
- 部署形态：JVM uber-jar；开发环境使用 H2，生产环境使用 MySQL/Oracle/PostgreSQL（可通过 `application.yaml` 配置）
- 此应用必须集成的外部平台或企业系统：`<待在选择业务域时定义>`

## 模块结构（代码生成后）

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

**按平台标准执行**：Nop 多租户是框架内置薄层，orm.xml **不预置 `tenantId` 字段**，租户列（`nopTenantId`）由框架自动创建/填充/过滤。完整机制（按实体声明 `useTenant`、全局开关 `enable-tenant-by-default`、自动添加列 `auto-add-tenant-col`、自动过滤、跨租户保护、临时切换租户、混合使用、原生 SQL 不自动过滤等）见 `../nop-entropy/docs-for-ai/02-core-guides/tenant-model.md`，此处不重复。

**项目状态**：bootstrap 阶段未启用租户（`useTenant` 默认 `false`，未配置全局开关）。是否启用 SaaS 多租户待业务确认；启用时在 `app-erp-app/application.yaml` 配置 `nop.orm.enable-tenant-by-default: true` + `auto-add-tenant-col: true`。

**项目硬规则**：
- **禁止在 orm.xml 预置 `tenantId` 字段**——租户列由框架自动管理。
- **原生 SQL 与数据初始化 SQL** 必须手动包含 `nopTenantId` 列（框架不自动过滤）。
- **跨租户操作**使用 `ContextProvider.runWithTenant()` / `runWithoutTenantId()`，不绕过框架。

## 稳定规则

- Model → Delta → Java 决策顺序（见 `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`）
- 每业务域一份 `<domain>/model/app-erp-<domain>.orm.xml` 是该域持久化实体的唯一真相来源；生成文件（`_` 前缀、`_gen/`）不可手编辑
- 模块依赖方向：web → service → dao → api；app 聚合 web + service + delta
- 实体类名使用 `Erp<Domain>` 前缀（例如 `ErpMdMaterial`、`ErpInvStockMove`）以避免与 nop 内置实体冲突
- 字典命名空间为 `erp-<domain-short>/<dict-name>`（例如 `erp-md/material-type`、`erp-fin/voucher-type`）
- **禁止在 orm.xml 预置 `tenantId` 字段**；租户隔离机制（按实体 `useTenant` 或全局开关）见 `../nop-entropy/docs-for-ai/02-core-guides/tenant-model.md`，本项目按平台标准执行
- 禁止的捷径：手动编辑生成的代码、无文档原因绕过 `I*Biz` 进行跨实体访问、业务异常使用 `extends RuntimeException`

## 更新规则

当支持的基线更改时，在同一更改中更新此文件。