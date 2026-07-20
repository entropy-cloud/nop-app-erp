# 技能索引

使用此目录存储可复用提示和工作流剧本。

这些不是一次性聊天消息。它们是可复用的仓库记忆。

技能应主要捕获可复用工作方法、审查方法或审计方法。不要将技能用作需求真相、设计真相或架构真相的替代品。

技能库不是吸引子。如果不通过 `AGENTS.md`、`docs/index.md`、活动需求和 owner docs 进行路由，大型技能库通常会退化为结构化氛围编码。

这些提示是复制项目的通用默认值。复制模板后，**必须**根据项目的真实 owner docs、保护区域、验证堆栈、命名约定、已知失败模式和误报容忍度进行定制。

## 技能路由规则

选择技能前：

1. 首先阅读相关需求和 owner docs。
2. 使用 `AGENTS.md` 对任务类型进行分类。
3. 通过匹配工作方法而非仅业务标签来选择技能。
4. 如果多个技能都可能适合，请在实施前请独立子代理或审查者选择。
5. 如果没有现有技能明显适合，记录 `Skill: none` 并继续正常的文档驱动工作流。
6. 对于非平凡计划，在计划中记录技能选择依据和审查结果。

不要添加广泛的业务场景技能来替代项目特定的 owner docs。如果场景经常重复，请首先检查是否缺少路由、owner docs 或计划指南。仅当可复用工作方法稳定时才提升技能。

## 技能注册表

| 技能 | 使用场景 | 不使用场景 | 必需输入 | 预期输出 |
| ----------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------- |
| `age-practice-gap-audit-prompt.md` | 仓库需要比较实时实践与预期 AGE 工作流 | 任务是本地功能实现 | AGE 基线文档、当前仓库结构、活动文档、采样实时证据 | `docs/analysis/` 下的分析笔记，含优先级差距 |
| `document-audit-prompt.md` | 需求、设计或架构文档可能不完整或不一致 | 任务琐碎且本地 | 目标文档路径、相关输入或 owner docs | 审计发现和修订目标 |
| `design-doc-audit-prompt.md` | `docs/design/` 需要重新验证为应用层行为基线 | 需要单一更窄的审计（状态机、计划） | 所有 `docs/design/` 文件、相关需求、存在时的 `domain-design-guidelines.md`、**必须含 `erp-survey/` 覆盖矩阵作为功能对标基准** | 按严重性排序的发现和处理结果 |
| `design-completeness-scan-prompt.md` | 主动扫描 `docs/design/` 以查找目标范围内缺失的域/文档/功能 | 验证现有文档（改用 `design-doc-audit-prompt.md`） | `docs/design/` 树、`product-scope.md`、路线图、`flow-overview.md`、**必须含 `erp-survey/` 全部报告（否则会遗漏未设计的功能）** | 优先级差距列表，驱动下一轮文档添加 |
| `state-machine-business-review-prompt.md` | 工作流状态机（订单/审批/争议/生命周期）需要正确性审查 | 更改琐碎或与转换无关 | 定义状态机的 owner doc、相关需求 | P0–P3 发现、裁决、可达性/角色/外部摘要 |
| `plan-audit-prompt.md` | 非平凡计划在实施前准备好接受挑战 | 尚无计划 | 计划文件、相关需求和 owner docs | 通过/失败审计，含具体问题 |
| `closure-audit-prompt.md` | 实施声称完成并需要独立结束审查 | 工作仍在进行中 | 计划、验证证据、相关更改文档 | 结束裁决和剩余差距 |
| `requirement-gap-retrospective-prompt.md` | 落地工作仍未达到预期，需求管道需要诊断 | 需求仍在起草中 | 原始输入、需求/讨论文档、交付结果 | 回顾发现和流程修正 |
| `multi-dimensional-audit-prompt.md` | 高风险工作需要同时跨多个维度挑战 | 单一对象审计已足够 | 相关需求/owner docs、计划或更改区域、验证证据 | 按维度分组的发现 |
| `open-ended-audit-prompt.md` | 正常检查清单之外可能存在隐藏问题 | 工作仅需要狭窄的结构化审计 | 相关需求/owner docs、计划（如有）、日志、实时更改代码 | 对抗性发现和未知风险笔记 |
| `index-routing-audit-prompt.md` | 文档索引或目录结构需要路由有效性审查 | 索引没有路由角色或琐碎 | 顶层索引、子索引、目标文件 | 覆盖表、角色测试结果、结构发现 |
| `bug-diagnosis-prompt.md` | Bug 真实存在但根本原因尚未证明 | 缺陷已经明显且本地 | Bug 报告、owner docs、复现路径、验证命令 | 确认原因和证明路径 |
| `code-quality-audit-prompt.md` | 审查代码的行为风险和实现质量 | 仅需要格式或琐碎细节 | 更改文件、owner docs、测试或验证证据 | 按严重性排序的发现 |
| `code-refactor-discovery-prompt.md` | 结构清理候选需要在重构前发现 | 结构目标已经达成一致 | 目标区域、owner docs、当前代码 | 排名重构候选 |
| `code-refactor-prompt.md` | 行为保留结构重构工作是任务 | 任务更改支持的行为 | 目标区域、不变量、验证命令 | 安全重构执行和证明 |
| `orm-model-audit-prompt.md` | `<domain>/model/*.orm.xml` 需要规范与完整性审计（类型/长度/字典/标准字段/业务字段/关系） | 单模块内部审计、需求综合 | `domain-design-guidelines.md` §10/§11、平台 `orm-model-design.md`、所有 orm.xml | 按维度的问题清单 + 裁决 + 字段补齐统计 |
| `cross-module-dependency-audit-prompt.md` | 多模块跨工程数据依赖合理性、DAG 合规性、外部实体引用一致性审计 | 单模块审计、需求综合 | `module-boundaries.md`、`data-dependency-matrix.md`、`cross-module-entity-reference.md`、所有 orm.xml | DAG 验证结果 + 外部实体声明完整性矩阵 + 裁决 |
| `nop-platform-conformance-audit-prompt.md` | 项目设计与实现对 Nop Platform 最佳实践的遵循度审计 | 业务设计审计（用 design-doc-audit）、ORM 字段审计（用 orm-model-audit） | `../nop-entropy/docs-for-ai/` 全部、项目 architecture 文档 | 12 维度合规率 + 反模式清单 + 裁决 |
| `development-wisdom-gate-prompt.md` | AI 开发过程中自检：假设面出、深度充电、跨层一致性、意图忠实度、生态约束、第一性原理验证，使用通用开发通识在声称完成前系统性挑战产出 | 审计特定对象需要针对性工具（plan-audit、multi-dimensional-audit 等） | 当前产出（设计/计划/代码）、项目 owner docs | 6 维度裁决 + 综合通过/不通过 |

## 入门技能

- `age-practice-gap-audit-prompt.md`
- `document-audit-prompt.md`
- `design-doc-audit-prompt.md`
- `design-completeness-scan-prompt.md`
- `state-machine-business-review-prompt.md`
- `plan-audit-prompt.md`
- `closure-audit-prompt.md`
- `requirement-gap-retrospective-prompt.md`
- `multi-dimensional-audit-prompt.md`
- `open-ended-audit-prompt.md`
- `index-routing-audit-prompt.md`
- `bug-diagnosis-prompt.md`
- `code-quality-audit-prompt.md`
- `code-refactor-discovery-prompt.md`
- `orm-model-audit-prompt.md`
- `cross-module-dependency-audit-prompt.md`
- `nop-platform-conformance-audit-prompt.md`
- `code-refactor-prompt.md`
- `development-wisdom-gate-prompt.md`

## 与工具原生技能的关系

`docs/skills/` 包含存储在仓库内并跨工具和编辑器保持可移植的方法和审计类型技能。一些 AI 工具也支持自己的原生技能加载（例如工具自动加载的项目本地技能目录）。两者互补而非竞争：

- 将可复用审查/审计方法和提示放在 `docs/skills/` 中，以便它们与文档一起版本化，并可供任何代理或人工读取
- 如果使用工具，请将工具加载的操作技能（特定于框架的操作指南、代码生成配方、调试配方）放在工具自己的约定中
- 无论技能物理位置如何，都通过 `AGENTS.md`、`docs/index.md` 和 owner docs 进行路由；技能选择工作方法，不替代 owner-doc 路由

模板本身保持工具中立，不假设任何特定的 AI 工具。

## 项目定制化层（nop-app-erp）

> 此节是本仓库对各技能模板的项目特定覆盖。任何技能在被使用前，必须将下面这些项目事实注入到提示上下文中——技能模板的通用默认值在本仓库不充分。

### 保护区域（ask-first，未经人工批准不得修改）

- **ORM 模型**：`module-<domain>/model/app-erp-<domain>.orm.xml` 是代码生成的真相源，**禁止在无人批准下修改**。修改后必须用 `mvn clean install -DskipTests` 触发增量重新生成（不要重跑 `nop-cli gen`）。
- **API 契约**：`module-<domain>/model/app-erp-<domain>.api.xml` 同上。
- **会计/财务/数据删除**：财务凭证、过账、期末结账、坏账、成本核算等代码区域是 ERP 保护区域，无 owner doc 不实现。
- **生成产物**：`_gen/` 目录、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml` 永不手写。
- 完整规则见 `docs/context/ai-autonomy-policy.md` 与 `docs/context/project-context.md §AI 阻塞条件`。

### 验证命令（按场景）

| 场景 | 命令 |
|------|------|
| 全项目构建（绿色基线） | `mvn clean install -DskipTests` |
| 类型/编译检查（快速反馈） | `mvn compile -DskipTests` |
| 单元测试 | `mvn test` |
| XML well-formed | `xmllint --noout module-<domain>/model/app-erp-<domain>.orm.xml` |
| 合规性检查 | `bash docs/audits/nop-compliance-checker.sh` |
| 本地运行 | `java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar` |

### 命名约定（与本仓库已有产物对齐）

- 实体类前缀：`Erp<Domain>`（如 `ErpMdMaterial`、`ErpInvStockMove`、`ErpSysNotification`）
- 表前缀：`erp_<short>_*`（如 `erp_md_`、`erp_inv_`、`erp_sys_`）
- 字典命名空间：`erp-<short>/<dict-name>`（如 `erp-md/material-type`）
- BizModel 包：`io.github.nop.app.erp.<domain>.service`（如 `io.github.nop.app.erp.purchase.service`）
- ErrorCode 命名空间前缀：`erp.err.<short>`（如 `erp.err.pur`、`erp.err.sal`、`erp.err.mfg`；详见 `docs/design/domain-design-guidelines.md §7.1`）
- 字典 `valueType` 统一为 `string`（语义编码），option `value` 与 `code` 合一（详见 `system-baseline.md §字段与类型约定` D1）
- 服务层模型：双轨编排（task.xml 用于拓扑可变流程；Processor + 派生 bean 用于拓扑稳定但单步可覆盖）见 `service-layer-orchestration.md`、`processor-extension-pattern.md`

### 已知失败模式（必须在审计/审查中针对性检查）

1. **跨模块外部实体引用表前缀双重拼接**：`notGenCode="true"` 外部实体引用必须验证表名前缀不被双重拼接（参见 `docs/lessons/01-orm-cross-module-table-prefix-validation.md`）。
2. **章节重编号后的残留引用**：章节重编号后必须全文搜索所有域文档的残留引用（参见 `docs/lessons/02-cross-ref-renumber-scan.md`）。
3. **`dao().updateEntity()` 越权访问**：跨实体访问应通过 `I*Biz` 接口；直接 `IDaoProvider`/`IOrmTemplate` 调用须在注释中说明原因。
4. **`System.currentTimeMillis()` / `LocalDateTime.now()` 在生产代码**：必须替换为 `CoreMetrics.currentTimeMillis()` 以支持时间可控测试。
5. **业务异常未扩展 `NopException`**：禁止 `extends RuntimeException`；公共错误必须用 `ErrorCode` + 中文描述。
6. **`@Inject private`**：Nop IoC 中 `@Inject` 字段不能是 `private`。
7. **字符串比较用 `==`/`!=`**：字典 String 比较一律 `Objects.equals()` / `.equals()`。
8. **propId 编号断续**：orm.xml 列定义 `propId` 必须连续（gap 会触发平台校验失败）。

---

## 技能组合使用方式：用技能系统代替人工审查

### 设计意图

这个技能库被设计为一个**协同工作的 AI 审查系统**。不是让一个 AI 运行一个技能就完成所有检查——而是让**不同的 AI 子代理在不同时机使用不同技能**，交替覆盖，使技能库整体覆盖人工在项目中提供的通用开发通识。

### 核心原则

1. **技能分立，组合出效果**。没有单个技能能覆盖所有人工审查——需按阶段交替使用多个技能。
2. **时机决定技能选择**。同一项工作在不同阶段需不同技能（如：实施前用 `plan-audit`，实施中用 `development-wisdom-gate`，实施后用 `closure-audit`）。
3. **每项产出至少经过两个技能门控**。任何非平凡的产出在声称完成前至少经过一次 `development-wisdom-gate` 自检 + 一次面向对象审计（`closure-audit`/`plan-audit`/`multi-dimensional-audit` 等）。
4. **独立子代理做审计，实施代理做自检**。`development-wisdom-gate` 由实施 AI 自己运行（自检门控），对象级审计由独立子代理运行（外部审查）。

### 开发全生命周期中的技能分配

```
阶段               实施AI自检门控             独立子代理审查
────────────────────────────────────────────────────────────
需求综合          development-wisdom-gate    document-audit
                 (假设面出 + 意图忠实)       (需求文档完整性)

设计定稿          development-wisdom-gate    design-doc-audit
                 (跨层一致性 + 生态约束)      + orm-model-audit（如涉及ORM）
                                           + state-machine-review（如涉及状态机）

计划起草          自检-延后（计划本身        plan-audit
                 不触发自检门控）              (拦截P0缺陷的最佳时机)
                 plan draft 本身不触发自检    (拦截P0缺陷的最佳时机)

实施中进行        development-wisdom-gate    code-quality-audit（复杂实现）
                 (深度充电 + 第一性原理)      + nop-platform-conformance（平台合规）

实施完毕准备      development-wisdom-gate    closure-audit（必须独立子代理）
声称完成          (全6维度最终裁决)            + multi-dimensional-audit（高风险）
                                           + nop-platform-conformance（如涉及平台变更）

修复 Bug          bug-diagnosis（根因定位）   bug-diagnosis（由独立子代理复核根因）
                 + development-wisdom-gate   + 确认修复后 closure-audit
                 (检查是否修了症状而非根因)
```

### 技能触发规则

在 AGENTS.md 的"任务路由"阶段（实施前），按以下规则选择技能：

1. **扫描技能库**：读取本 README 的「技能注册表」+ 本「使用方式」节。
2. **按阶段匹配**：当前属于上表中哪个阶段？该阶段需要的自检门控和外部审查各是什么？
3. **加载匹配技能**：用 `skill` 工具加载**所有匹配的技能**（不要只加载一个）。
4. **按顺序执行**：先自检（`development-wisdom-gate`），后外部审查。自检未通过时先修改，再进入外部审查。
5. **记录技能使用**：在计划或日志中记录 `Skill: <name>` 供后续审计追踪。

### 反模式

| 反模式 | 后果 | 正确做法 |
|--------|------|---------|
| 只有一个 skills 加载运行全部检查 | 思维定势，漏检 | 按阶段交替使用不同 skills |
| 实施 AI 自己审计自己的产出 | 盲区保留 | closure-audit / plan-audit 必须由独立子代理运行 |
| 声称完成后才运行 development-wisdom-gate | 失去"开发过程中的自检"意义 | 在声称完成前作为门控运行 |
| 认为某个 skill 可以覆盖所有检查 | 该 skill 膨胀，其他 skill 闲置 | 每个 skill 专注一个视角，组合使用 |
| 只跑 scoped 验证就声明完成 | 假阳性 | closure-audit 强制 full reactor 验证检查 |
| 跳过自检直接进入外部审查 | 低质量问题浪费审查者时间 | 每份产出先自检通过，再提交外部审查 |
| 一个阶段未使用任何技能 | 该阶段无人检查，盲区保留 | 每个阶段至少使用一个技能（自检或审查） |

### 已知覆盖盲区（设计上不试图通过技能本身解决）

- **平台源码/文档变更**：当 nop-entropy 平台本身更改了行为，所有技能需要同步更新——这不在技能设计范围内。
- **新领域知识**：技能不替代 owner docs。技能捕获"如何检查"的方法，不捕获"需要检查什么"的业务知识。
- **非常规架构决策**：当项目做出架构级选择（如换数据库、换部署模式），现有技能不够用时，人工干预是预期路径——技能无法替代人的判断。

---

### 与平台文档的关系

技能不替代平台文档阅读。涉及 Nop 平台 API/代码生成/BizModel 模式/页面定制/delta/测试时，先读 `../nop-entropy/docs-for-ai/INDEX.md` 路由表，再回到本目录选用合适的审查/审计方法。