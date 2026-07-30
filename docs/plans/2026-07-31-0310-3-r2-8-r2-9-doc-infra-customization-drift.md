# 2026-07-31-0310-3-r2-8-r2-9-doc-infra-customization-drift

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.8（P1-MA3-050~052, 054~056）+ R2.9（P1-MA3-057~061）
> Related: `docs/audits/2026-07-28-1953-arm-ma3-index-routing.md`（A3.7 审计报告）；`docs/audits/2026-07-28-2130-arm-ma3-customization-verification.md`（A3.8 审计报告）；`docs/audits/arm-index.md §P1-MA3-050~061`；`docs/architecture/customization-capabilities.md`（R2.9 单 owner doc）；plan `2026-07-31-0010-1-r2-1-design-doc-execution-status-leakage-cleanup.md`（R2.1 文档治理先例）
> Audit: required

## Current Baseline

审计来源：A3.7 索引路由审计登记 6 项 P1（P1-MA3-050/051/052/054/055/056）+ A3.8 可定制性验证登记 5 项 P1（P1-MA3-057~061）。R2.0 展开为 R2.8 + R2.9 两行（status `todo`）。**两工作项均为纯文档**（Skill: none，零代码变更，drift 方向 = doc→实际仓库状态），共享同一验证特征（无 build；交叉引用 + grep 一致性复核）。按 plan 指南反碎片化原则（规则 14）合并为本计划两阶段——避免为两批小型 doc-only 修复各开一计划。

> **编号说明**：A3.7 索引路由登记的 P1 为 P1-MA3-050/051/052/054/055/056——`P1-MA3-053` 是不存在的 ID（roadmap 与 arm-index 均无此项，编号区间有意跳过），故 R2.8 覆盖 6 项而非 7 项，非遗漏。

逐项实时基线（grep / file:line 可复现）：

**R2.8 — 索引路由有效性（6 项）**：

- **P1-MA3-050 [BLOCKER 类，指向不存在文件]**：`docs/index.md:28` 顶层路由器「首先阅读」表「阅读解释性方法论文章」→ `docs/articles/README.md`，但 `docs/articles/` 目录存在（含 `loop-engineering-x-attractor.md` + `mission-driver--loop-engineering.md` 2 篇）**无 README.md**。代理/人工追"方法论文章"到达 404。
- **P1-MA3-051 [BLOCKER 类，>10 文件目录无 README]**：`docs/bugs/` 含 13 个回归笔记（实测 13 文件）**无 README.md**；仅 `00-bug-fix-note-writing-guide.md`（写作指南，非目录索引）。维护者无法经单一索引浏览全部回归历史。
- **P1-MA3-052 [MAJOR，重复无交叉引用]**：`AGENTS.md §快速路由` vs `docs/index.md` 路由表 5 行语义重叠（产品基线/工作项选择/功能实现/模型契约/计划审查），**无交叉引用**声明单一真相源 → 双维护点漂移风险。
- **P1-MA3-054 [MAJOR，过时指针]**：`docs/logs/index.md`「Current:」段最新条目停在 `2026/06-25.md`，但 `docs/logs/2026/` 实际日志文件远超（截至本计划起草日 07-31，含多日日志未登记）。审查者追"最近实现历史"被指向 1+ 月前。
- **P1-MA3-055 [MAJOR，orphan 类别]**：`docs/errors/`（错误码集中索引，有 README）+ `docs/ppts/`（演示材料，无 README）**均未纳入** `docs/index.md §目录角色` 表。`ppts/` 完全无顶层路由。
- **P1-MA3-056 [MAJOR，核心导航占位]**：`docs/index.md:91-97` §域快速参考[可选]表为模板占位（`<area>`/`<path>`/`<skill-name|none>`），18 业务域 + notify 全部未填。多域项目本应经此表一次查找路由到 owner doc + 技能。

**R2.9 — 可定制性 owner doc 实证状态（5 项，均 `docs/architecture/customization-capabilities.md`）**：

- **P1-MA3-057**：§能力一 Delta 声明为"核心手段" + 列 4 业务场景，但业务级 Delta = 0（仅 2 平台层 nop-auth view delta）。修复 = 方案 A（§能力一 4 场景后追加"实证状态注记"）。
- **P1-MA3-058**：§能力二 EAV 声明 3 业务场景，全域 19 ORM 零 `extField`/`NopSysExtField`。客户化字段经 codegen ORM 物理加列承载。修复 = 方案 A（§能力二 追加实证注记）。
- **P1-MA3-059**：§定制能力总览表 + §能力三 + §能力六 声明 nop-dyn / task.xml / @BizLoader 三项，实际全部零业务使用（采用 codegen ORM + Processor 模式）。修复 = 方案 A（§定制能力总览 增"实际启用"列 + 章节末实证注记）。
- **P1-MA3-060**：§能力六 BizLoader 3 业务示例全部以其他机制实现（未交量→SQL/Processor / 当前库存→StockBalance 实体 / 借贷平衡→balanceStatus 字段 + view.xml gen-control 内联脚本）。§决策提示"普通扩展字段优先用 @BizLoader"误导。修复 = 方案 A（§能力六 追加实证注记 + 修订决策提示）。
- **P1-MA3-061**：§升级路径保护 5 项机制仅 (4) 模块化组装 + (5) 保留层不冲突 经项目实证，(1) Delta 自动合并 / (2) EAV 独立存储 / (3) nop-dyn 运行时配置 平台机制可用但项目零实证。doc 未标注每项实证状态。同步 §产品定位"不改基线源码"边界模糊。修复 = 方案 A（5 项各加"实证状态" + §产品定位 边界澄清）。

## Goals

- **G1（R2.8）**：消除 docs 索引路由的 404 / 过时指针 / 重复无交叉引用 / orphan 类别 / 核心导航占位——补建缺失 README、加交叉引用、更新 Current 段、补 §目录角色、填充/交叉引用域快速参考表。
- **G2（R2.9）**：为 customization-capabilities.md 各能力声明追加"实证状态注记"——诚实反映"平台能力可用 / 项目零实证 / successor 触发条件"，消除"声明核心手段但业务级零落地"的声明-实证 gap。

## Non-Goals

- **任何应用代码 / ORM / 配置变更**——纯文档。
- 删除 customization-capabilities.md 任一能力章节（方案 B）——保留章节 + 追加实证注记（方案 A），保留设计意图。
- 为 articles/ppts/bugs 补充内容性文章——仅补**索引 README / 路由声明**。
- R2.1 设计文档执行状态 scrub——R2.1 已 done；本计划聚焦索引路由 + 定制实证状态，不同维度。

## Task Route

- Type: `app-layer design change`（文档路由与实证状态对齐，无代码变更）
- Owner Docs: `docs/index.md`、`AGENTS.md`（§快速路由）、`docs/articles/`、`docs/bugs/`、`docs/logs/index.md`、`docs/architecture/customization-capabilities.md`
- Skill Selection Basis: 无匹配技能（可用技能集均针对代码/前端/测试/调试/Git，不覆盖纯文档编辑）。本计划为文档对齐实际仓库状态，**Skill: none**。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - R2.8 索引路由有效性（P1-MA3-050~052, 054~056）

Status: completed
Targets: `docs/articles/README.md`（新建）、`docs/bugs/README.md`（新建）、`AGENTS.md`（§快速路由）、`docs/index.md`（§目录角色 + §域快速参考）、`docs/logs/index.md`（Current 段）
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: none

- [x] `Fix`（050）：新建 `docs/articles/README.md`——列出 2 篇文章 + 目的 + 与 `docs/skills/` 边界（articles = 解释性方法论长文；skills = 可复用审查/审计方法）。
- [x] `Fix`（051）：新建 `docs/bugs/README.md`——回归笔记清单表（13 文件分类）+ 与 `00-bug-fix-note-writing-guide.md` 交叉引用。
- [x] `Fix`（052）：`AGENTS.md §快速路由` 顶部加交叉引用——"完整路由见 `docs/index.md`，本表仅列代理高频任务"+ 删除与 index.md 完全重叠的行（声明 index.md 为顶级路由器单一真相源）。
  - `Decision`（052）：经核实 §快速路由 8 行均为"语义重叠"非"完全重叠"——每行"然后检查"列携带 index.md 未覆盖的 Nop 平台/代理导向指引（如 codegen 增量规则、`nop-entropy/docs-for-ai/` 路由），删除会损失代理高频便利路由。裁决：保留行 + 顶部加「路由权威」声明（index.md=顶级路由器单一真相源，本表=代理高频精简子集，漂移时以 index.md 修正）——此声明本身消除"无交叉引用→双维护漂移"根因，达成 finding 修复目标（方案 A 变体，理由已记录）。
- [x] `Fix`（054）：`docs/logs/index.md`「Current:」段更新至最新日志日期（执行时确认 `docs/logs/2026/` 最新文件）+ 评估改为"按年/月自动列表"或"见 design/README.md 业务域表"机制避免再次过时（若改机制，记录 Decision）。
  - `Decision`（054）：机制 = 目录指针（`docs/logs/2026/` 为真相源，`ls` 按日期自然排序）+ 最近若干条导航锚点。放弃逐条手工全量登记（曾致 06-25→07-31 漂移）。自动化逐日刷新生成脚本 = successor（超出 doc-only 范围）。理由已写入 logs/index.md「机制说明」注记。
- [x] `Fix`（055）：`docs/index.md §目录角色` 补 `docs/errors/`（错误码集中索引）+ `docs/ppts/`（演示材料，可选）两行。
- [x] `Decision`（056）：§域快速参考表裁决 = **交叉引用 `docs/design/README.md` 业务域设计文档表**（推荐，避免重复真相源漂移）或填充 18+1 域表。记录选择 + 理由（design/README.md 已维护域表，重复填写会双维护点漂移）。
- [x] `Fix`（056）：按 Decision 执行——改 §域快速参考为"见 `docs/design/README.md` 业务域设计文档表"交叉引用并删占位，或填充 18+1 域（域 → design/<domain>/ owner doc → 推荐 skill）。

Exit Criteria:

> 阶段交付：索引路由无 404 / 无过时指针 / 重复表有交叉引用 / orphan 类别纳入 / 域导航可用。无代码变更。

- [x] `docs/articles/README.md` 存在 + 列 2 篇文章（index.md:28 指向不再 404）
- [x] `docs/bugs/README.md` 存在 + 含 13 文件清单
- [x] AGENTS.md §快速路由含 index.md 交叉引用 + 重叠行处理（路由权威声明，行保留理由见 Decision）
- [x] logs/index.md Current 段指向最新日志（07-31）+ 目录指针机制
- [x] index.md §目录角色含 errors/ + ppts/
- [x] index.md §域快速参考占位已处理（交叉引用 design/README.md）

### Phase 2 - R2.9 customization 实证状态注记（P1-MA3-057~061）

Status: completed
Targets: `docs/architecture/customization-capabilities.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: none（与 Phase 1 不同文件，可并行，但同计划内顺序执行）

- [x] `Decision`: 修复方式 = **方案 A（追加实证状态注记，保留章节）**，不采纳方案 B（删除章节）。理由：保留设计意图 + 诚实标注实证状态优于删除；客户首项客户化场景落地时按 successor 验证。（已写入 customization-capabilities.md 文首「声明-实证对齐原则」注记）
- [x] `Fix`（057）：§能力一 Delta 4 业务场景表后追加"实证状态注记"——业务级 Delta = 0（保留层 + 模块化组装已覆盖产品基线定制需求）；合并机制经平台层 nop-auth view delta 实证可用；业务级实证为首项客户化场景落地时验证（successor）。
- [x] `Fix`（058）：§能力二 EAV 3 业务场景后追加实证注记——客户化字段经 codegen ORM 物理加列承载（如多币种四件套），EAV 路径未启用；机制经平台 nop-sys 可用；首个客户启用 EAV 时验证（successor）。
- [x] `Fix`（059）：§定制能力总览表增"实际启用"列（nop-dyn/task.xml/@BizLoader 标"⚠️ 平台能力，本项目未启用（codegen ORM/Processor 优先）"）+ §能力三 / §能力六 章节末实证注记。
- [x] `Fix`（060）：§能力六 §适用场景 3 业务示例追加实证注记（实际实现路径：未交量→SQL/Processor / 当前库存→StockBalance 实体 / 借贷平衡→balanceStatus + view.xml gen-control）+ §决策提示 修订为"普通扩展字段优先用 @BizLoader 或 view.xml gen-control，本项目当前路径选择 gen-control"。
- [x] `Fix`（061）：§升级路径保护 5 项机制各加"实证状态"——(1)(2)(3)"平台机制可用 / 项目零实证（successor）"+ (4)(5)"✅ 经项目实证落地"；§产品定位"不改基线源码"边界澄清（生成物 `_gen/`/`_*.xml` ≠ 基线源码；保留层手写是扩展 codegen 产物合法，改 `_gen/` 才违反硬规则）。

Exit Criteria:

- [x] customization-capabilities.md §能力一/二/三/六 + §定制能力总览 + §升级路径保护 + §产品定位 各含实证状态注记；grep "实证状态" 覆盖 5 项 finding 对应章节（实测：`实证状态` 字面 10 处 / `实证状态|实证注记` 合计 13 处，覆盖 057×1 / 058×1 / 059×2 / 060×1 / 061×2 + 总览综合说明 + 升级路径综合 + 文首原则 + §决策提示修订）

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is** (ses_04b8cb1a5ffenV9gWHvNZQ7ot) — 基线对实时仓库核实准确（articles/README + bugs/README 缺失 / index.md:28/55/91-97/118 一致 / §目录角色缺 errors+ppts / customization 零"实证状态"）。11 findings（R2.8×6 + R2.9×5）1:1 覆盖；P1-MA3-053 确认为不存在 ID（roadmap 有意跳过，非覆盖缺口）——已在 Current Baseline 补编号说明。R2.8+R2.9 合bundling 经裁定可接受：虽为不同结果表面（与规则 4 字面略有张力），但均纯文档、Skill: none、共享同一验证路径 + 规则 14 反碎片化目的 + 项目 plan 文件数逼近 300 阈值，倾向单计划双阶段。采纳一条非阻塞建议：补 P1-MA3-053 不存在说明。无 blocker，可进入实施。

## Closure Gates

> 纯文档计划：无代码变更，删除完整仓库 `build`/`test` 门控。保留交叉引用 + grep 一致性 + compliance checker（确认零新增命中）+ 独立审计门控。

- [x] 范围内行为/文档完成（R2.8 6 项 + R2.9 5 项 doc 对齐）
- [x] 相关文档对齐（index.md / AGENTS.md / articles/README / bugs/README / logs/index / customization-capabilities 内部一致 + 与实际仓库状态一致）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（预期零新增命中——本计划不改代码）；doc 一致性 grep 复核（articles/README 存在 / bugs/README 存在 / AGENTS↔index 交叉引用 / logs Current 最新 / index §目录角色含 errors+ppts / 域快速参考占位处理 / customization 实证注记覆盖 5 项）
- [x] 无范围内项目降级为 deferred/follow-up（11 项 finding 均为范围内 doc 对齐存活项）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### customization 业务级实证（Delta/EAV/nop-dyn 等）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台机制经平台文档可用，本项目未启用是合理设计选择（codegen ORM/Processor 优先）；doc 已诚实标注实证状态 + successor；无运行时风险（未启用即不影响）。
- Successor Required: `yes`（触发条件 = 首项客户化场景落地时验证对应能力集成）

## Closure

Status Note: 纯文档两阶段计划（R2.8 索引路由 6 项 + R2.9 可定制性实证 5 项）全部交付并经独立结束审计 PASS。零代码变更（git 仅 .md），compliance checker 零新增命中，grep 一致性复核全 pass。customization 业务级实证为显式裁决的 watch-only residual + successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，fresh context，ses_04b3d8137ffe31P0AsBlpuyC9s）
- Evidence: **VERDICT: PASS**。独立审计员在无先前执行上下文的全新会话中，对 plan `2026-07-31-0310-3` 的 11 项 finding（R2.8×6 + R2.9×5）逐项对照实时仓库验证：
  - **Phase 1（R2.8 索引路由）**：`docs/articles/README.md` 已建（2 篇文章 + 目的 + skills 边界，index.md:28 不再 404）/ `docs/bugs/README.md` 已建（13 文件分类清单 + 写作指南交叉引用）/ `AGENTS.md:25` 路由权威声明 index.md=单一真相源（行保留 Decision 理由充分——「然后检查」列携带 index.md 未覆盖的 Nop 指引）/ `docs/logs/index.md:15` Current 指 07-31.md（实测最新）+ 目录指针机制 Decision / `docs/index.md:113-114` §目录角色含 errors/+ppts/ / §域快速参考占位移除交叉引用 design/README.md（grep `<area>`/`<path>` 零命中）。
  - **Phase 2（R2.9 可定制性实证，方案 A 保留章节+注记）**：§能力一 Delta(L65 业务级=0+平台层实证+successor) / §能力二 EAV(L86 未启用+codegen ORM 加列+successor) / §定制能力总览(L24-33 实际启用列 ⚠️×3) + §能力三 nop-dyn(L112 实证注记) / §能力六 BizLoader(L192-197 三示例实际路径 + L200 决策提示修订 gen-control) / §升级路径保护(L226-230 五项 2✅+3△) + §产品定位(L18 生成物≠基线源码边界澄清)。
  - **验证门控**：`nop-compliance-checker.sh` EXIT=0（基线 Java 模式，零新增命中）；`git status` 仅 7 改 + 2 新建全为 `.md`（零 `.java/.xml/.orm.xml/.api.xml`）。roadmap R2.8/R2.9=done；日志 `07-31.md` 条目存在；计划两 Phase Status=completed + 全 `[x]` + exit criteria `[x]`。P1-MA3-053=不存在的有意跳过 ID（已说明）；customization 业务级实证=显式裁决的 watch-only residual + successor（合理，非范围内缺陷隐瞒）。1 项非阻塞观察（exit criteria 中 grep 计数估算已修正为实测值）。
