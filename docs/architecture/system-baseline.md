# 系统基线

## 目的

记录 `nop-app-erp` 当前支持的实现基线。

## 当前基线（post-codegen BizModel 深化阶段）

- 运行时形态：Quarkus 应用（单 uber-jar），JVM 模式
- 前端栈：百度 AMIS — `.view.xml` 文件中的 JSON 驱动页面（已生成可用）
- 后端栈：Java 17+、Quarkus、Nop Platform（nop-entropy 2.0.0-SNAPSHOT）
- 状态管理方式：服务端驱动的 AMIS 页面；Nop ORM 会话/事务范围
- 数据访问方式：Nop ORM（基于实体，生成的 DAO）；XML 模型作为真相源（每业务域一份 `<domain>/model/app-erp-<domain>.orm.xml`）
- 测试栈：JUnit 5 + nop-autotest（已就绪）
- 构建和打包工具：Maven 多模块，父项目 `io.github.entropy-cloud:nop-entropy`
- 部署形态：JVM uber-jar；开发环境使用 H2，生产环境使用 MySQL/Oracle/PostgreSQL（可通过 `application.yaml` 配置）
- 此应用必须集成的外部平台或企业系统：`<待在选择业务域时定义>`

## 模块结构（codegen 已完成）

nop-app-erp 采用**每业务域独立 Maven 工程**结构（18 域），由 `app-erp-all` 聚合启动。详细决策、命名规则与完整工程命名映射见 `domain-module-split-analysis.md`（§2.0 映射表）。

领域工程分两层：核心业务域（5 个，进销存+财务）与扩展业务域（13 个，资产/项目/制造/质量/维护 + CRM/CS/HR/APS/合同/DRP/物流/B2B）。依赖方向（DAG）：

```
master-data ← inventory ← purchase/sales ← finance
                                       ↑
assets/projects/manufacturing/quality/maintenance（扩展域，各自依赖核心域）
```

### 核心业务域

| 领域工程 | appName | 表前缀 | 类名前缀 | 权威模型 |
|----------|---------|--------|----------|----------|
| `app-erp-master-data` | `erp-md` | `erp_md_` | `ErpMd*` | `module-master-data/model/app-erp-master-data.orm.xml` |
| `app-erp-inventory` | `erp-inv` | `erp_inv_` | `ErpInv*` | `module-inventory/model/app-erp-inventory.orm.xml` |
| `app-erp-purchase` | `erp-pur` | `erp_pur_` | `ErpPur*` | `module-purchase/model/app-erp-purchase.orm.xml` |
| `app-erp-sales` | `erp-sal` | `erp_sal_` | `ErpSal*` | `module-sales/model/app-erp-sales.orm.xml` |
| `app-erp-finance` | `erp-fin` | `erp_fin_` | `ErpFin*` | `module-finance/model/app-erp-finance.orm.xml` |

### 扩展业务域（13 个）

> 第一批 5 个（assets/projects/manufacturing/quality/maintenance）+ 第二批 8 个（crm/cs/hr/aps/contract/drp/logistics/b2b）。完整命名维度（含 moduleId、VFS 路径）见 `domain-module-split-analysis.md §2.0`。

| 领域工程 | appName | 表前缀 | 类名前缀 | 权威模型 |
|----------|---------|--------|----------|----------|
| `app-erp-assets` | `erp-ast` | `erp_ast_` | `ErpAst*` | `module-assets/model/app-erp-assets.orm.xml` |
| `app-erp-projects` | `erp-prj` | `erp_prj_` | `ErpPrj*` | `module-projects/model/app-erp-projects.orm.xml` |
| `app-erp-manufacturing` | `erp-mfg` | `erp_mfg_` | `ErpMfg*` | `module-manufacturing/model/app-erp-manufacturing.orm.xml` |
| `app-erp-quality` | `erp-qa` | `erp_qa_` | `ErpQa*` | `module-quality/model/app-erp-quality.orm.xml` |
| `app-erp-maintenance` | `erp-mnt` | `erp_mnt_` | `ErpMnt*` | `module-maintenance/model/app-erp-maintenance.orm.xml` |
| `app-erp-crm` | `erp-crm` | `erp_crm_` | `ErpCrm*` | `module-crm/model/app-erp-crm.orm.xml` |
| `app-erp-cs` | `erp-cs` | `erp_cs_` | `ErpCs*` | `module-cs/model/app-erp-cs.orm.xml` |
| `app-erp-hr` | `erp-hr` | `erp_hr_` | `ErpHr*` | `module-hr/model/app-erp-hr.orm.xml` |
| `app-erp-aps` | `erp-aps` | `erp_aps_` | `ErpAps*` | `module-aps/model/app-erp-aps.orm.xml` |
| `app-erp-contract` | `erp-ct` | `erp_ct_` | `ErpCt*` | `module-contract/model/app-erp-contract.orm.xml` |
| `app-erp-drp` | `erp-drp` | `erp_drp_` | `ErpDrp*` | `module-drp/model/app-erp-drp.orm.xml` |
| `app-erp-logistics` | `erp-log` | `erp_log_` | `ErpLog*` | `module-logistics/model/app-erp-logistics.orm.xml` |
| `app-erp-b2b` | `erp-b2b` | `erp_b2b_` | `ErpB2b*` | `module-b2b/model/app-erp-b2b.orm.xml` |

| `app-erp-all`（聚合） | — | — | — | — |

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

**项目状态**：当前阶段未启用租户（`useTenant` 默认 `false`，未配置全局开关）。是否启用 SaaS 多租户待业务确认；启用时在 `app-erp-all/application.yaml` 配置 `nop.orm.enable-tenant-by-default: true` + `auto-add-tenant-col: true`。

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

### 服务层编写规范

服务方法实现遵循 **双轨编排 + I*Biz 逻辑**的分工原则。详细约定、步骤实现方式选择、映射规则、Delta 定制见 `service-layer-orchestration.md`；拓扑稳定流程的 Java Processor 实现与配置余地见 `processor-extension-pattern.md`，此处仅列核心规则：

- **CRUD 方法**使用 CrudBizModel 默认实现，不写 task.xml / Java / xbiz
- **多步骤编排方法**（≥2 步编排逻辑）按拓扑稳定性二选一：拓扑可变用 `task.xml` + xbiz 绑定（首选，支持 Delta 覆盖单步）；拓扑稳定但单步实现需按客户/行业覆盖用 Java Processor + 派生 bean 覆盖（见 `processor-extension-pattern.md`）。简单单步逻辑直接 Java 或 xbiz
- **映射约定**：xbiz `task:name="{BizObj}/{method}"` → `service/_task/{BizObj}/{method}.task.xml`
- **不推荐通过 `ITaskStep` 封装业务动作**，直接 `<invoke bean="erpXxxBiz">`

### 字段与类型约定（审计 D1/D3/D4 裁决落地）

以下约定源自 `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md` 整改计划 `docs/plans/2026-07-02-0900-1-audit-remediation.md`：

- **业务动作责任字段（D3）**：`approvedBy`/`postedBy`/`closedBy` 是治理动作责任字段（执行审核/过账/结账的**系统用户**），统一 `stdDomain="userId"` + `VARCHAR(36)`；**不**建到员工实体的 `<to-one>`（员工引用只留给分派字段如 `requesterId`/`assignedEmployeeId`）。与被动审计字段（`createdBy`/`updatedBy`，`domain="createdBy"` 登录名）语义区分。命名保留 `xxxBy`（churn 权衡；命名瑕疵登记为残留）。
- **金额与布尔类型（D4）**：`amount` domain = `precision=18 scale=2`（金额标准 2 位小数）；`quantity`/`unitPrice`/`taxAmount` 保持各自精度。`boolFlag` domain = `stdSqlType="BOOLEAN" stdDataType="boolean"`。注：部分列显式 override `amount` 精度为 `20,4`（历史显式覆盖，重生成保留，为 18,2 的精度超集，无精度损失——见计划残留登记）。
- **字典 valueType（D1，已落地）**：全域字典统一 `valueType="string"`（语义编码如 `"APPROVED"`、`"INCOMING"`），option `value` 与 `code` 合一；`ext:dict` 字段列类型 `VARCHAR(20|30)`、`stdDataType="string"`。Java 常量文件 `Erp*Constants` 持有 `String` 常量与字典 code 一一对应；比较一律 `Objects.equals()` / `.equals()`，禁止 `==`/`!=`（规避装箱历史习惯导致的 String 引用比较恒不等 bug）。落地计划：`docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md`。

## 数据依赖

跨模块数据依赖分三类（R 只读引用 / S 同步修改 / P 弱指针反查），完整矩阵见 `data-dependency-matrix.md`。核心约束：

- 所有业务域对 `master-data` 表族通过 `notGenCode="true"` 外部实体引用建立 ORM `<to-one>`（机制 B），支持 EQL 点导航与 GraphQL 展开
- **同步修改（S）** 仅限业财一体闭环：业务单据过账同事务写 inventory + finance
- 跨业务域引用（finance → projects/assets 单向合法；purchase/sales → projects 项目采购/销售单按 `projectId` 建 to-one 只读归集成本；hr → projects 员工项目分配/工时归集）单向合法；其他业务域之间走**弱指针（P）**：`relatedBillType`/`sourceBillType` + `billCode` 字符串三元组
- 单向 DAG，禁止循环依赖（已验证：17 业务域所有引用边单向合法，零循环）

## 更新规则

当支持的基线更改时，在同一更改中更新此文件。