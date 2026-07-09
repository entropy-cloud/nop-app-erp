# 代码库映射

## 目的

本文件为 AI 代理提供实时仓库的紧凑映射，使其不必通过反复搜索导入和目录来重新发现结构。

保持足够新以路由常见工作。不要将其变成完整的架构文档。

## 当前结构

根 pom.xml 列出 20 个 reactor 模块：18 个 `module-<domain>` + 1 个 `module-notify`（跨域 sys 通知派发子系统）+ 1 个 `app-erp-all`（子模块链合计 154 个 reactor 模块）。

> **物理目录 ↔ 逻辑工程名 ↔ appName ↔ moduleId 映射**：完整 19 行映射表见 `docs/architecture/domain-module-split-analysis.md §2.0`（唯一规范）。物理目录 `module-<domain>/` 是 bootstrap 期别名，逻辑工程名为 `app-erp-<domain>`，聚合启动工程逻辑名 = 物理名 = `app-erp-all`。`module-notify`（逻辑工程名 `app-erp-notify`，appName `erp-notify`，moduleId `erp/notify`）为跨域 sys 子系统，与 18 业务域并列。

所有域 codegen 骨架已生成，含实体类、DAO、I*Biz 接口、BizModel、XMeta、view.xml 骨架。后续模型变更用 `mvn clean install` 增量重新生成，**不要**重跑 `nop-cli gen`。

## 入口点

| 区域 | 路径 | 说明 | 置信度 |
|------|------|------|--------|
| ORM 模型（真相） | `module-<domain>/model/app-erp-<domain>.orm.xml` | 各域权威模型（18 业务域 + notify 通知派发子系统） | high |
| 聚合启动工程 | `app-erp-all/` | Quarkus main + 聚合所有域依赖（含 notify）+ `nop-integration-api`（邮件/短信 SPI）；`app.action-auth.xml` 合并各域 + 系统管理菜单 | high |
| 根 POM | `pom.xml` | 聚合 20 个模块 | high |
| 设计文档（全局） | `docs/design/*.md` | 多份全局 owner doc（app-overview/flow-overview/domain-design-guidelines 等） | high |
| 设计文档（域） | `docs/design/<domain>/` | 各域目录，含 README + state-machine + use-cases + ui-patterns 等 | high |
| 架构文档 | `docs/architecture/*.md` | 多份技术基线文档 | high |
| 文档路由器 | `docs/index.md` | 顶层导航 | high |
| 待办事项 | `docs/backlog/README.md` | 工作项选择 | high |

## ORM 模型清单

| 域 | 路径 | 字典命名空间 |
|----|------|-------------|
| master-data | `module-master-data/model/app-erp-master-data.orm.xml` | `erp-md/*` |
| inventory | `module-inventory/model/app-erp-inventory.orm.xml` | `erp-inv/*` |
| purchase | `module-purchase/model/app-erp-purchase.orm.xml` | `erp-pur/*` |
| sales | `module-sales/model/app-erp-sales.orm.xml` | `erp-sal/*` |
| finance | `module-finance/model/app-erp-finance.orm.xml` | `erp-fin/*` |
| assets | `module-assets/model/app-erp-assets.orm.xml` | `erp-ast/*` |
| projects | `module-projects/model/app-erp-projects.orm.xml` | `erp-prj/*` |
| manufacturing | `module-manufacturing/model/app-erp-manufacturing.orm.xml` | `erp-mfg/*` |
| quality | `module-quality/model/app-erp-quality.orm.xml` | `erp-qa/*` |
| maintenance | `module-maintenance/model/app-erp-maintenance.orm.xml` | `erp-mnt/*` |
| crm | `module-crm/model/app-erp-crm.orm.xml` | `erp-crm/*` |
| customer-service | `module-cs/model/app-erp-cs.orm.xml` | `erp-cs/*` |
| human-resource | `module-hr/model/app-erp-hr.orm.xml` | `erp-hr/*` |
| aps | `module-aps/model/app-erp-aps.orm.xml` | `erp-aps/*` |
| contract | `module-contract/model/app-erp-contract.orm.xml` | `erp-ct/*` |
| drp | `module-drp/model/app-erp-drp.orm.xml` | `erp-drp/*` |
| logistics | `module-logistics/model/app-erp-logistics.orm.xml` | `erp-log/*` |
| b2b | `module-b2b/model/app-erp-b2b.orm.xml` | `erp-b2b/*` |
| notify | `module-notify/model/app-erp-notify.orm.xml` | `erp-notify/*` |

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
