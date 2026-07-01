# 代码库映射

## 目的

本文件为 AI 代理提供实时仓库的紧凑映射，使其不必通过反复搜索导入和目录来重新发现结构。

保持足够新以路由常见工作。不要将其变成完整的架构文档。

## 当前结构

根 pom.xml 列出 19 个 reactor 模块：18 个 `module-<domain>` + 1 个 `app-erp-all`（子模块链合计 146 个 reactor 模块）。

> **物理目录 ↔ 逻辑工程名 ↔ appName ↔ moduleId 映射**：完整 19 行映射表见 `docs/architecture/domain-module-split-analysis.md §2.0`（唯一规范）。物理目录 `module-<domain>/` 是 bootstrap 期别名，逻辑工程名为 `app-erp-<domain>`，聚合启动工程逻辑名 = 物理名 = `app-erp-all`。

所有域 codegen 骨架已生成（1721 个 Java 文件），含实体类、DAO、I*Biz 接口、BizModel 空壳、XMeta、view.xml 骨架。后续模型变更用 `mvn clean install` 增量重新生成，**不要**重跑 `nop-cli gen`。

## 入口点

| 区域 | 路径 | 说明 | 最后验证 | 置信度 |
|------|------|------|----------|--------|
| ORM 模型（真相）×18 | `module-<domain>/model/app-erp-<domain>.orm.xml` | 18 域权威模型，共 279 实体 | 2026-07-01 | high |
| 聚合启动工程 | `app-erp-all/` | Quarkus main + 聚合所有域依赖；`app.action-auth.xml` 合并 18 域 + 系统管理菜单 | 2026-07-01 | high |
| 根 POM | `pom.xml` | 聚合 19 个模块（18 module-* + app-erp-all） | 2026-07-01 | high |
| 设计文档（全局） | `docs/design/*.md` | 7 份全局 owner doc（app-overview/flow-overview/domain-design-guidelines 等） | 2026-06-23 | high |
| 设计文档（域） | `docs/design/<domain>/` | 18 域目录，含 README + state-machine + use-cases + ui-patterns 等 | 2026-07-01 | high |
| 架构文档 | `docs/architecture/*.md` | 9 份技术基线文档 | 2026-06-23 | high |
| 文档路由器 | `docs/index.md` | 顶层导航 | 2026-06-25 | high |
| 待办事项 | `docs/backlog/README.md` | 工作项选择 | 2026-06-25 | high |

## ORM 模型清单（18 域 × 279 实体）

| 域 | 路径 | 实体数 | 字典命名空间 | 最后验证 |
|----|------|--------|-------------|----------|
| master-data | `module-master-data/model/app-erp-master-data.orm.xml` | 22 | `erp-md/*` | 2026-07-01 |
| inventory | `module-inventory/model/app-erp-inventory.orm.xml` | 15 | `erp-inv/*` | 2026-07-01 |
| purchase | `module-purchase/model/app-erp-purchase.orm.xml` | 17 | `erp-pur/*` | 2026-07-01 |
| sales | `module-sales/model/app-erp-sales.orm.xml` | 13 | `erp-sal/*` | 2026-07-01 |
| finance | `module-finance/model/app-erp-finance.orm.xml` | 17 | `erp-fin/*` | 2026-07-01 |
| assets | `module-assets/model/app-erp-assets.orm.xml` | 10 | `erp-ast/*` | 2026-07-01 |
| projects | `module-projects/model/app-erp-projects.orm.xml` | 13 | `erp-prj/*` | 2026-07-01 |
| manufacturing | `module-manufacturing/model/app-erp-manufacturing.orm.xml` | 23 | `erp-mfg/*` | 2026-07-01 |
| quality | `module-quality/model/app-erp-quality.orm.xml` | 11 | `erp-qa/*` | 2026-07-01 |
| maintenance | `module-maintenance/model/app-erp-maintenance.orm.xml` | 12 | `erp-mnt/*` | 2026-07-01 |
| crm | `module-crm/model/app-erp-crm.orm.xml` | 34 | `erp-crm/*` | 2026-07-01 |
| customer-service | `module-cs/model/app-erp-cs.orm.xml` | 16 | `erp-cs/*` | 2026-07-01 |
| human-resource | `module-hr/model/app-erp-hr.orm.xml` | 28 | `erp-hr/*` | 2026-07-01 |
| aps | `module-aps/model/app-erp-aps.orm.xml` | 6 | `erp-aps/*` | 2026-07-01 |
| contract | `module-contract/model/app-erp-contract.orm.xml` | 15 | `erp-ct/*` | 2026-07-01 |
| drp | `module-drp/model/app-erp-drp.orm.xml` | 7 | `erp-drp/*` | 2026-07-01 |
| logistics | `module-logistics/model/app-erp-logistics.orm.xml` | 7 | `erp-log/*` | 2026-07-01 |
| b2b | `module-b2b/model/app-erp-b2b.orm.xml` | 13 | `erp-b2b/*` | 2026-07-01 |

## 常见变更路由

| 任务类型 | 从这里开始 | 然后检查 | 验证 |
|----------|-----------|----------|------|
| 设计实体/字典 | `module-<domain>/model/app-erp-<domain>.orm.xml` | `docs/design/<domain>/`、`../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` | XML well-formed |
| 更改模型/模式 | `module-<domain>/model/app-erp-<domain>.orm.xml` | owner doc + plan（保护区域：`ask first`） | regenerate + `mvn clean install -DskipTests` |
| 生成单域模块（仅首次） | `module-<domain>/model/app-erp-<domain>.orm.xml` | `../nop-entropy/docs-for-ai/03-runbooks/`（codegen runbooks） | `mvn clean install -DskipTests` |
| 增量重新生成（模型变更后） | `module-<domain>/model/app-erp-<domain>.orm.xml` | `mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链） | 编译通过 |
| 编写 BizModel | `docs/design/<domain>/state-machine.md` + `README.md` | `../nop-entropy/docs-for-ai/03-runbooks/write-bizmodel-method.md` | `mvn test` |
| 添加页面 | `docs/design/<domain>/ui-patterns.md` | AMIS `.view.xml` 规范 | 启动应用验证 |
| 更改权限 | `app-erp-all/_vfs/app/erp/auth/` | `docs/design/roles-and-permissions.md` | 启动应用验证 |

## 大型或脆弱文件

| 路径 | 风险 | 首选方法 |
|------|------|----------|
| `module-<domain>/model/app-erp-<domain>.orm.xml` | 核心文件；驱动各域代码生成；保护区域 | 直接编辑；模式更改需要计划/设计文档 |
| `app-erp-all/_vfs/_delta/` | Delta 覆盖 nop core | 通过 delta 机制编辑，切勿修补 nop core |
| `app-erp-all/_vfs/app/erp/auth/app.action-auth.xml` | 应用聚合菜单入口 | 通过 x:extends 继承各域手写层，不直接修改 |

## 项目特定搜索提示

- 使用文件模式：`module-*/model/*.orm.xml`、`docs/design/**/*.md`、`app-erp-all/src/**/*.java`
- 使用内容锚点：实体类名使用 `Erp` 前缀（例如 `ErpMdMaterial`），字典命名空间 `erp-<domain-short>/<dict>`
- 避免编辑生成文件：任何 `_gen/` 目录、任何带 `_` 前缀的文件、`_app.orm.xml`、`_service.beans.xml`

## 更新规则

当更改创建新的主要入口点、移动公共代码或反复导致代理重新发现相同路径时，更新此文件。
