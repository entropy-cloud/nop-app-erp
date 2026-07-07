# AGENTS.md

## 项目意图

`nop-app-erp` 是基于 Nop Platform 构建的企业资源规划（ERP）参考应用，采用 Attractor-Guided Engineering（AGE）工作流进行 AI 辅助应用开发。

本仓库是构建在 Nop 低代码平台（nop-entropy）之上的应用层产品，而非框架核心项目。

仓库是唯一真相源，聊天记录仅作为临时工作表面。

在编写非平凡代码之前，代理必须首先理解：

- `docs/context/project-context.md`
- `docs/context/ai-autonomy-policy.md`
- `docs/context/codebase-map.md`
- 定义当前切片的需求、待办事项（backlog）、路线图、计划或用户请求
- 从 `docs/index.md` 路由的相关 owner docs
- 当需求含义依赖于源材料时，`docs/input/` 下的相关原始输入

当事实冲突或不确定哪个工件拥有答案时，请阅读 `docs/context/source-of-truth-and-precedence.md`。
当规划或工作流决策是任务的一部分时，请阅读 `docs/process/application-development-workflow.md`。

## 快速路由

| 如果你需要... | 从这里开始 | 然后检查 |
| --- | --- | --- |
| 了解产品基线 | `docs/requirements/product-scope.md` | `docs/design/app-overview.md` |
| 选择下一个工作项 | `docs/backlog/README.md` | 相关需求和 owner docs |
| 实现一个功能 | 相关需求或待办项 | `docs/design/`、`docs/architecture/`、`<domain>/model/*.orm.xml`，如需触发则参考计划指南 |
| 更改持久化模型或 API 契约 | `<domain>/model/*.orm.xml`、`<domain>/model/*.api.xml` | `../nop-entropy/docs-for-ai/` 下的 Nop 文档 |
| 更改页面或视图 | 相关设计 owner doc | `app-erp-web` 下的 AMIS `.view.xml` 文件（代码生成设置后） |
| 审查已计划或已完成的切片 | `docs/plans/` 下的相关计划 | 计划/结束审计提示；普通审计证据保留在计划中 |
| 运行或验证项目 | `docs/context/project-context.md` | `docs/context/codebase-map.md` |
| 起草、执行或审计 `docs/plans/` 下的计划 | `docs/plans/00-plan-authoring-and-execution-guide.md` | `docs/logs/00-log-writing-guide.md` |

### 计划起草与执行

创建、修订、执行或审计 `docs/plans/` 下的文件时，必须首先阅读 `docs/plans/00-plan-authoring-and-execution-guide.md`。计划是具有明确状态、范围、退出标准和验证检查清单的执行文档。完成时勾选检查清单项。在声称完成之前，重新审计实时仓库。

## 任务路由

编写代码前，代理必须首先对任务进行分类：

1. 确定任务类型：
   - 需求澄清
   - 应用层设计变更
   - 架构变更
   - 仅实现变更
   - Bug 调查
   - 验证或审计工作
2. 在行动前，使用 `docs/index.md` 阅读该任务类型的 owner docs。
3. 在起草或修订计划前，检查 `docs/skills/README.md` 中的候选可复用技能。
4. 对于非平凡工作，在实现前记录所选路线和计划的技能使用情况。

除非从相关需求和 owner docs 中路线已经很明显，否则不要从功能请求直接跳转到代码。

## 操作规则

1. 优先采用文件输入/输出协作方式。
2. 不要将聊天摘要视为持久项目记忆。
3. 当范围仍不明确时，不要从原始 PM 文本或原型截图直接跳到代码。
4. 如果输入模糊，首先在 `docs/discussions/` 或 `docs/requirements/` 中创建或更新文件。
5. 当以下规划触发条件适用时，在实现前创建或更新计划。
6. 保持 `docs/design/` 和 `docs/architecture/` 专注于稳定支持的产品基线，而非迁移历史、路线图顺序或执行状态。
7. 保持 `docs/design/` 专注于业务语义、工作流和状态含义；将持久化实体、字段集和数据字典保存在 `<domain>/model/*.orm.xml` 中作为权威来源，而非在散文中重复。
8. 保持日志简短、带日期且仅追加。完成任何重大代码更改后，必须更新每日开发日志 `docs/logs/{year}/{month}-{day}.md`（按时间倒序，格式见 `docs/logs/00-log-writing-guide.md`）。对 `nop-entropy` 的更改必须记录在 `nop-entropy/ai-dev/logs/` 中，而非本项目的 `docs/logs/`。
9. 在 `docs/bugs/` 中记录非显而易见的回归问题。
10. 如果原型和实现存在实质性差异，在 `docs/retrospectives/` 中记录原因，而非默默继续。
11. 只有当模式重复出现足以证明复用合理时，才将重复的过程经验提升到 `docs/skills/` 或 `docs/audits/`。
12. 对于高风险或高度模糊的需求、设计或计划草案，请求独立子代理或审查者通过并修订，直到解决主要异议。每个创建的计划必须在实施开始前通过独立计划审计，并在标记完成前通过独立结束审计。
13. 保持代码注释最少。优先使用自解释代码；仅当本地约束容易被误读且代码本身不足以表达时，才添加罕见注释。
14. 当引用的文件未在预期路径找到时，在断定不存在之前检查 `docs/archive/`。归档文件在 `docs/archive/` 下保留其原始相对名称。未经人工批准，不要将文件移动到 `docs/archive/`。
15. 将可复用技能视为方法选择器，而非需求、设计或架构文档的替代品。业务知识首先属于 owner docs。

## 当前项目阶段

`nop-app-erp` 处于 **codegen 已完成、待 BizModel 业务逻辑深化阶段**。18 业务域 + 1 跨域通知派发子系统（共 19 个 `module-*/`）的 ORM 模型已设计完成（279 实体 + 3 通知实体），Maven 多模块结构已由 `nop-cli gen` 从 `<domain>/model/*.orm.xml` 生成（1730+ Java 文件）。`app-erp-all` 聚合 app 构建通过（154 reactor 模块）。当前重点：按 roadmap 依次深化 BizModel → ErrorCode → 页面定制 → 端到端验证。

完整 18+1 域列表（物理目录 ↔ 逻辑工程名映射详见 `docs/architecture/domain-module-split-analysis.md §2.0`）：

- **核心域（11）**：master-data, inventory, purchase, sales, finance, assets, projects, manufacturing, quality, maintenance, notify（跨域通知派发子系统，3 实体）
- **第一批扩展域（5）**：crm, cs, hr, aps, logistics
- **第二批扩展域（4）**：b2b, contract, drp, contract（含外部实体 notGenCode 引用 master-data）

### 多域目录结构

遵循 nop-entropy 模式（见 `docs-for-ai/01-repo-map/domain-module-pattern.md`），每个子域是一个**顶层目录**，包含自己的 `model/` 子目录：

```text
nop-app-erp/
├── module-master-data/
│   └── model/
│       └── app-erp-master-data.orm.xml    # 源模型（唯一真相）
├── module-purchase/
│   └── model/
│       └── app-erp-purchase.orm.xml
├── module-sales/
│   └── model/
│       └── app-erp-sales.orm.xml
├── module-inventory/
│   └── model/
│       └── app-erp-inventory.orm.xml
├── module-finance/
│   └── model/
│       └── app-erp-finance.orm.xml
├── module-assets/
│   └── model/
│       └── app-erp-assets.orm.xml
├── module-manufacturing/
│   └── model/
│       └── app-erp-manufacturing.orm.xml
├── module-projects/
│   └── model/
│       └── app-erp-projects.orm.xml
├── module-maintenance/
│   └── model/
│       └── app-erp-maintenance.orm.xml
├── module-quality/
│   └── model/
│       └── app-erp-quality.orm.xml
├── module-notify/                          # 跨域通知派发子系统
├── module-crm/
├── module-cs/
├── module-hr/
├── module-aps/
├── module-logistics/
├── module-b2b/
├── module-contract/
├── module-drp/
└── docs/...
```

### 标准模块链（每域）

每个域遵循 nop-entropy 标准链（见 `docs-for-ai/01-repo-map/domain-module-pattern.md`）：

```text
model → codegen → dao → meta → service → web → app → api
```

- `model/` — ORM 源模型（唯一真相）
- `{domain}-codegen/` — 代码生成入口
- `{domain}-dao/` — ORM、Entity、DAO、I*Biz 接口
- `{domain}-meta/` — XMeta 与 i18n
- `{domain}-service/` — BizModel、xbiz
- `{domain}-web/` — 页面与视图
- `{domain}-app/` — 应用打包与启动
- `{domain}-api/` — 外部 RPC 接口契约

codegen 后阶段规则：

- Java 模块路径、包名、视图路径均已存在（`module-<domain>/erp-<short>-{dao,meta,service,web,app,api}/`）
- ORM 模型变更后用 `mvn clean install -DskipTests` 触发增量重新生成（不要重跑 `nop-cli gen`）
- 设计与讨论工作仍应集中在 `<domain>/model/*.orm.xml`（权威源）以及 `docs/design/`、`docs/architecture/`、`docs/requirements/`
- `docs/context/project-context.md` 中的验证命令已可执行（154 reactor 模块全绿基线见 `docs/testing/known-good-baselines.md`）

## Nop Platform 特定规则

- 代码生成由 XML 模型驱动（`<domain>/model/*.orm.xml`、`<domain>/model/*.api.xml`）。XLSX 文件可通过 `nop-cli convert` 从 XML 生成，但不是真相源。不要手动编辑生成的代码；应从模型重新生成。
- ORM XML 文件（`*.orm.xml`）定义实体映射。权威模型源是 `<domain>/model/app-erp-<domain>.orm.xml`；其他一切都是生成的。
- 业务逻辑位于 `*.xbiz.xml` 和 `{domain}-service` 中的 BizModel Java 类（代码生成后）。
- SQL 库定义在 `*.sql-lib.xml` 文件中。
- 构建需要先构建 `nop-entropy` 父项目。
- 项目使用 Maven 多模块结构。每个域生成：codegen → dao → service → web → app，delta/meta 作为附加模块。`api` 模块最后生成为外部 RPC 接口契约。

### Nop Platform 文档（`nop-entropy/docs-for-ai/`）

Nop Platform 的权威开发文档位于 `../nop-entropy/docs-for-ai/`（兄弟目录）。这是所有 Nop 平台约定、API 和开发模式的主要参考。

**何时阅读：** 在实现任何涉及 Nop 平台 API、代码生成、BizModel 模式、页面/视图定制、delta 定制、测试或任何非平凡平台交互的功能之前。

**如何使用：**

1. 从 `docs-for-ai/INDEX.md` 开始 — 包含路由表，映射约 40 个常见任务到正确文档。
2. 推荐查找顺序：INDEX → `00-start-here/` → `03-runbooks/` → `02-core-guides/` → `01-repo-map/` → `04-reference/`
3. `00-start-here/ai-defaults.md` — 核心决策框架：Model → Delta → Java，反模式表，自检清单。
4. `02-core-guides/` — 规范模式文档，涵盖模型优先开发、服务层（CrudBizModel）、页面定制、delta 机制、认证、测试等。
5. `03-runbooks/` — 面向任务的分步指南，用于常见操作（创建实体、编写 BizModel 方法、构建页面等）。
6. `04-reference/common-java-helpers.md` 和 `04-reference/safe-api-reference.md` — 平台辅助工具和 CrudBizModel 安全 API 的快速参考。

**平台文档中适用于本项目的关键规则：**

- 决策顺序：Model → Delta → Java。始终优先使用模型/Delta/定制而非编写新 Java 代码。
- 切勿手动编辑生成的文件（`_gen/` 下的文件、带 `_` 前缀的文件，或 `_app.orm.xml`/`_service.beans.xml`）。
- 标准实体服务使用 `CrudBizModel<T>`；使用 `@BizQuery`/`@BizMutation` 注解。
- 使用平台辅助工具：`CoreMetrics.currentTimeMillis()` 而非 `System.currentTimeMillis()`，`JsonTool` 而非第三方 JSON 库，`StringHelper` 而非 Apache Commons。
- 在 Nop 的 IoC 容器中，`@Inject` 字段不能是 `private`。
- `@BizMutation` 自动包装事务；除非需要显式传播控制，否则不要添加 `@Transactional`。
- **跨实体访问**：在 BizModel 中，始终为其他实体注入 `I*Biz` 接口。仅当 `I*Biz` 无法满足需求时才使用 `IDaoProvider` / `IOrmTemplate` / `@SqlLibMapper`，并在代码注释中记录原因。
- **异常处理**：所有业务异常必须扩展 `NopException`。公共/GraphQL 面向错误使用 `ErrorCode` + `NopException`（描述为中文，i18n 处理翻译）。

## 强制技能加载

在进行**任何**实现工作之前，代理**必须**扫描可用技能列表并加载每个描述与当前任务匹配的技能。此规则适用于**所有**代理 — 主代理、子代理和审计/审查代理 — 无一例外。

**匹配规则：** 当技能的 `description` 或触发词涵盖即将进行的工作时（例如设计 ORM 模型、编写 BizModel 方法、创建 view.xml 页面、编写测试类），技能匹配。多个技能匹配时，全部加载。

**执行规范：**

1. **在编写代码或设计模型之前**，扫描可用技能并加载所有匹配的技能。技能是单一入口点 — 包含必需阅读的路由表、代码模式和反模式自检清单。
2. 加载技能后，阅读它路由到的文档。技能不替代阅读平台文档 — 它选择当前任务应阅读的文档。
3. 添加或修改代码或模型后，使用技能提供的自检机制验证无反模式。
4. 如果任务跨越多个阶段，进入每个新阶段时重新扫描并加载匹配的技能。
5. 如果没有技能匹配当前任务，继续执行但不要跳过扫描步骤本身。

## 首先阅读

- `docs/context/project-context.md`
- `docs/context/ai-autonomy-policy.md`
- `docs/context/codebase-map.md`
- 定义当前切片的需求、待办事项、路线图、计划或用户请求
- 从 `docs/index.md` 路由的相关 owner docs

根据需要额外阅读：

- `docs/context/source-of-truth-and-precedence.md` — 所有权或冲突问题
- `docs/context/conventions.md` — 项目范围约定
- `docs/process/application-development-workflow.md` — 工作流问题
- `docs/index.md` — 当需要超出活动文件的路由时

## 文档所有权

- `docs/context/` — 强制 AI 上下文、真相源优先级和项目范围约定
- `docs/backlog/` — 路线图、实现顺序和候选工作选择
- `docs/input/` — 原始外部输入，如 PM 笔记、卡片文档、文章摘录、原型参考和复制的源材料
- `docs/discussions/` — 需求澄清对话和未解决问题记录
- `docs/requirements/` — 实现就绪需求综合
- `docs/design/` — 稳定的应用层业务和功能设计
- `docs/architecture/` — 跨领域技术和模块边界真相
- `<domain>/model/*.orm.xml` 和 `<domain>/model/*.api.xml` — 持久化模型结构、数据字典和生成的契约真相
- `docs/lessons/` — 从 Bug、审计和回顾中提取的持久可复用经验
- `docs/plans/` — 非平凡工作的执行和结束标准
- `docs/audits/` — 审计方法论和专业审计记录
- `docs/skills/` — 可复用提示、审查剧本和审计提示模板
- `docs/logs/` — 带日期的实现记忆
- `docs/testing/` — 手动和探索性测试记录
- `docs/bugs/` — 非显而易见的 Bug 历史和回归笔记
- `docs/analysis/` — 研究、权衡分析和被拒绝的方向
- `docs/retrospectives/` — 实现后差距分析和流程改进

不要将强制规则隐藏在 `docs/references/` 中；如果 AI 必须默认应用它，将其放在 `docs/context/` 或 `AGENTS.md` 中。

## 默认工作流

1. 在 `docs/input/` 中收集原材料。
2. 如有需要，在 `docs/discussions/` 中澄清歧义。
3. 在 `docs/requirements/` 中综合实现就绪需求。
4. 将稳定设计输出拆分为 `docs/design/` 下的应用层设计和 `docs/architecture/` 下的技术设计，必要时相互引用。
5. 路由任务并选择候选可复用技能。
6. 当规划触发条件适用时，编写或更新计划，并在相关时按阶段或项目记录技能使用情况。
7. 实施前审计计划。
8. 实现最小完整切片。
9. 运行验证。
10. 对创建的计划进行结束审计。
11. 记录日志和任何需要的 Bug 笔记。

## 可选工作流层

当任务复杂性需要时使用这些。创建的计划必须进行计划和结束审计。

- `docs/audits/` — 用于专业、复杂、有争议、可复用或未来可重放的审计证据；普通计划/结束审计证据默认保留在计划中
- `docs/testing/` — 用于手动或探索性证明
- `docs/retrospectives/` — 用于实质性需求/原型差距
- `docs/skills/` — 用于重复失败后的可复用提示
- `docs/lessons/` — 用于重复失败后的持久工程经验

当工作必须同时跨多个维度进行挑战时，使用 `multi-dimensional-audit-prompt.md`。当标准检查清单可能遗漏隐藏风险时，使用 `open-ended-audit-prompt.md`。这些提示是通用默认值，复制后**必须**定制以匹配项目的真实 owner docs、保护区域、验证模型和重复失败模式。

## 规划规则

当任务具有以下任何特征时创建计划：

- 更改 API、数据库/模型、认证、集成、部署或公共契约行为
- 跨多个功能表面更改用户可见行为
- 涉及多个模块并更改共享行为
- 预计需要多个 AI 会话
- 修改超过 5 个文件或可能超过大约 200 行更改
- 需要分阶段执行或显式结束门控
- 存在未解决的产品或技术风险，不能隐藏在聊天中

仅对于本地低风险编辑（如文案更改、小型样式修复、仅测试清理以及具有明确现有测试的单文件行为修复），可以跳过正式计划。

所有创建的计划必须在实施开始前通过独立子代理或审查者审计，并在标记完成前再次通过审计。保护区域、未解决的产品风险或真相源冲突需要人工/子代理审查或保持阻塞。

## 技能使用规则

使用可复用技能前，确认以下所有条件：

- 任务类型和路由已从需求和 owner docs 中明确
- 技能匹配工作方法，而非仅相似的业务标签
- `docs/skills/README.md` 中列出的必需输入可用
- 预期输出已知且可存储在正确的文档位置

对于非平凡计划，每个依赖可复用技能的阶段或项目应记录 `Skill: <name>` 或 `Skill: none`。

## 文档维护

完成任何重大代码更改后，必须：

1. **更新每日开发日志** — `docs/logs/{year}/{month}-{day}.md`（按时间倒序，格式见 `docs/logs/00-log-writing-guide.md`）。
2. **更新相关 owner docs** — 当更改影响应用层行为或技术结构时，更新 `docs/design/` 或 `docs/architecture/` 中的相关文档。

当验证完全通过（全绿）时，在日志条目中记录验证状态并将其包含在 git 提交消息中。这为未来调试提供可靠的已知良好基线。

## 验证基线

不要假设此模板的示例命令对复制的项目有效。

使用 `docs/context/project-context.md` 中列出的真实命令。

如果验证命令为空或仍然是占位符，在报告验证成功前停止并填写它们。