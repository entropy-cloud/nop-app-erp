# 设计文档索引

## 目的

`docs/design/` 用于存放稳定的应用层 owner docs。

本目录适合承载：

- 产品功能基线
- 页面与流程行为
- 角色与权限
- 应用层壳和交互行为
- 各业务域的业务语义、状态机、跨域协作规则

跨功能的技术结构请写入 `docs/architecture/`。

## 范围边界

- `docs/requirements/` 负责某个切片或产品基线"应该做什么"
- `docs/design/` 负责该切片落定后的稳定应用层基线
- `docs/architecture/` 负责技术设计与跨功能结构
- `model/app-erp-<domain>.orm.xml` 负责持久化实体结构、字段集、字典和 ORM 真相

当某个功能同时依赖业务设计与技术设计时，应将两类内容分别写在对应 owner doc 中，并通过引用建立关联。

设计文档可以保留面向业务的实体名称、状态含义和状态迁移规则。

设计文档不应重复表目录、逐字段 schema 定义、字典清单，或本应属于 `model/*.orm.xml`、`docs/architecture/` 的平台实现章节。

## 全局设计文档

| 文档 | 职责 |
|-----|------|
| `app-overview.md` | 应用界面范围、角色、核心业务流程总览、关键领域区域 |
| `domain-design-guidelines.md` | 10 个业务域的设计原则、归属映射、跨域协作规则与数据一致性策略 |
| `domain-glossary.md` | 跨域核心术语统一词汇表 |
| `flow-overview.md` | 全局业务流程编排（L1 宏观流程、L2 状态机映射、L3 跨域规则） |
| `roles-and-permissions.md` | ERP 角色与权限模型 |
| `feature-inventory.md` | 已支持功能清单 |

## 业务域设计文档（每域一个目录，结构因域而异）

ERP 业务按 10 个独立领域工程组织（见 `docs/architecture/domain-module-split-analysis.md`），每个域对应 `docs/design/<domain>/` 目录。**不同域的文档结构不同**——按该域的业务复杂度与状态机重量决定包含哪些详细文档：

### 核心业务域（进销存+财务）

| 域目录 | 工程 | 权威模型 | 文档结构（因域而异） |
|--------|------|----------|----------------------|
| `master-data/` | `app-erp-master-data` | `module-master-data/model/app-erp-master-data.orm.xml` | 仅 README（启停二态非状态机，内嵌一节） |
| `inventory/` | `app-erp-inventory` | `module-inventory/model/app-erp-inventory.orm.xml` | README + state-machine + cross-domain |
| `purchase/` | `app-erp-purchase` | `module-purchase/model/app-erp-purchase.orm.xml` | README + state-machine + three-way-match + returns |
| `sales/` | `app-erp-sales` | `module-sales/model/app-erp-sales.orm.xml` | README + state-machine + returns |
| `finance/` | `app-erp-finance` | `module-finance/model/app-erp-finance.orm.xml` | README + state-machine + posting + period-close + ar-ap-reconciliation + multiple-accounting-schemas |

### 扩展业务域（资产/项目/制造/质量/维护）

| 域目录 | 工程 | 权威模型 | 文档结构 |
|--------|------|----------|----------|
| `assets/` | `app-erp-assets` | `module-assets/model/app-erp-assets.orm.xml` | README + state-machine（资产生命周期+折旧） |
| `projects/` | `app-erp-projects` | `module-projects/model/app-erp-projects.orm.xml` | README + state-machine（项目+任务） |
| `manufacturing/` | `app-erp-manufacturing` | `module-manufacturing/model/app-erp-manufacturing.orm.xml` | README + state-machine + bom-and-routing |
| `quality/` | `app-erp-quality` | `module-quality/model/app-erp-quality.orm.xml` | README + state-machine（质检+NCR） |
| `maintenance/` | `app-erp-maintenance` | `module-maintenance/model/app-erp-maintenance.orm.xml` | README + state-machine（维护访问+请求） |

**结构选择原则**：
- 状态机重的域（inventory/purchase/sales/finance）才有独立 `state-machine.md`。
- 主数据是启停二态（非工作流状态机），不单独建状态机文档，规则内嵌 README。
- 跨域协作复杂的域才有独立 `cross-domain.md`（如 inventory）。
- 特定业务规则独有的域才有专属文档（如 purchase 的 `three-way-match.md`、finance 的 `posting.md`）。
- 后续按需扩展（如 finance 可能增加 `period-close.md`、`costing.md`；purchase 可能增加 `returns.md`）。

## 编写规则

- 保持 `docs/design/` 聚焦于业务语义、角色、流程和支持行为。
- 通用 Nop 应用 owner-doc 与领域设计规则以上游 `../nop-entropy-wt/nop-entropy-master/docs-for-ai/02-core-guides/application-project-docs-and-domain-design.md` 为准（若已正式定位 `nop-entropy` 兄弟目录，则替换为该路径）。
- `domain-design-guidelines.md` 只负责 ERP 项目自己的领域归属映射和本地解释。
- 术语冲突或中英文对应不一致时，以 `domain-glossary.md` 为准。
- 当文档需要持久化模型细节时，应引用 `model/app-erp-<domain>.orm.xml`，不要重复抄写 schema。
- 当文档需要实现策略时，应引用 `docs/architecture/`，不要在这里混入平台实现细节。
- 不要把设计文档当作 roadmap 或实施状态跟踪器；实施顺序应写入 `docs/backlog/` 或计划文件。
- 跨域流程的主 owner 是 `flow-overview.md` 与触发域目录；相邻域只引用或摘要，不重复维护完整规则。

## 目录内文档组织约定

每个域目录至少包含 `README.md`（域概览）。是否包含其他详细文档**因域而异**，不强制统一结构：

- `state-machine.md`（状态机详解）：仅状态机重的域才有。状态机设计必须按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织，并在文档头部引用该提示词。
- `cross-domain.md`（跨域协作细则）：仅跨域协作复杂的域才有。
- `ui-patterns.md`（页面设计要点）：每个域一份，定义该域关键业务页面的结构布局、交互模式与导航流程。聚焦页面结构骨架和交互行为，不重复字段定义（归 `*.orm.xml`）和业务语义（归 `state-machine.md`、`use-cases.md`）。调研引用格式 `[源项目#要点]` 关联 `docs/analysis/erp-survey/`。
- 其他专属文档（如 `three-way-match.md`、`posting.md`）：按该域独有业务规则按需建立。

详细文档命名使用稳定名（非日期），因为它们是稳定 owner doc 而非时间敏感记录。

## 审查方法引用

设计文档审查与补全使用 `docs/skills/` 下的提示词作为方法依据：

| 审查场景 | 使用提示词 |
|----------|------------|
| 主动扫描设计缺口、驱动文档增量补全 | `docs/skills/design-completeness-scan-prompt.md` |
| 整体设计文档基线审查（已有文档的正确性） | `docs/skills/design-doc-audit-prompt.md` |
| 状态机正确性审查 | `docs/skills/state-machine-business-review-prompt.md` |
| 文档完整性/一致性审查 | `docs/skills/document-audit-prompt.md` |

状态机设计文档在头部引用对应的审查提示词，作为设计要点的依据。新增域或文档时，先用 `design-completeness-scan-prompt.md` 扫描确认缺口与优先级，再按域复杂度选择文档结构。
