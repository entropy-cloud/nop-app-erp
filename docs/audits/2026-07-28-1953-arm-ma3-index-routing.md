# MA3 索引路由有效性审计报告（A3.7）

> 报告时间：2026-07-28
> 里程碑/工作项：MA3 / A3.7（索引路由有效性）
> Skill：`docs/skills/index-routing-audit-prompt.md`（4 步：覆盖表 / 基于角色路由测试 / 结构质量检查 / 发现返回）
> 审查目标：`docs/index.md`（顶级文档路由器，143 行）+ `AGENTS.md` §快速路由 + §文档所有权 + 各 `docs/*/README.md` 与子索引 + `docs/` 全目录树 + `docs/archive/`
> Verdict：**FAIL（有路由缺口）** —— 零 P0（索引审查为文档层，原则上不产生 P0）；6 项 P1（BLOCKER/MAJOR 类，目标 MR2）+ 4 项 P2 watch-only

## 0. 审查范围与方法

按 `index-routing-audit-prompt.md` 4 步法对 `docs/` 索引结构做系统性路由有效性审查。**项目定制化**（按 skill §定制说明）：角色需求替换为 nop-app-erp 真实任务场景；结构规则采用项目实际目录树（`docs/` 26 个子目录 + `module-<domain>/` 21 个）；跳数阈值 >3 判深度不平衡；>10 文件目录无 README 判缺中间索引；分层索引模式（顶层 `docs/index.md` → 各目录 README/子索引 → 单文档）逐层核查。

**覆盖面**：
- 顶层路由器 `docs/index.md`（首先阅读表 ~40 行 + 技能路由表 4 行 + 域快速参考表 [占位] + 目录角色 ~22 项）
- `AGENTS.md` §快速路由（8 行任务路由）+ §文档所有权 + §首先阅读
- 各目录子索引：`context/README.md`、`backlog/README.md`、`design/README.md`、`architecture/README.md`、`skills/README.md`、`audits/00-audit-execution-guide.md` + `arm-index.md`、`bugs/00-bug-fix-note-writing-guide.md`、`requirements/README.md`、`references/`、`articles/`（**无 README**）、`examples/README.md`、`analysis/README.md`、`lessons/README.md`、`retrospectives/README.md`、`input/README.md`、`discussions/README.md`、`logs/index.md`、`testing/index.md`、`errors/README.md`、`archive/README.md`
- `docs/` 全目录树（孤立文件 / 缺中间索引检测）
- `docs/archive/`（引用断链检测）

## 1. 步骤 1 — 覆盖表

> 抽样策略：`docs/index.md`「首先阅读」表 + 「技能路由」表 + 「目录角色」逐条核查 exists / matches purpose；子索引抽样到 README 首屏。**仅记录需标注条目**（exists=Y 且 matches=Y 的常规条目省略，共 ~45 条全部 exists=Y/matches=Y）。

### 1.1 顶层 `docs/index.md` 路由条目

| entry | stated purpose | target path | exists | matches purpose | notes |
|-------|---------------|-------------|--------|-----------------|-------|
| 阅读解释性方法论文章 | 路由到方法论文章 | `docs/articles/README.md` | **N** | N/A | **目标文件不存在**（`docs/articles/` 有 2 篇文章但无 README）。顶层路由器指向不存在的子索引 → BLOCKER 类 |
| 从 ORM 模型生成多模块项目 | 路由到代码生成 runbook | `module-<domain>/model/app-erp-<domain>.orm.xml` | Y（19 域实测） | Y | 占位 `<domain>` 合理（多域项目模板惯例） |
| 决定更改应使用模型、Delta、钩子还是 Java | 路由到平台决策表 | `../nop-entropy/docs-for-ai/INDEX.md` | Y | Y | 跨仓引用可达 |
| 了解 Nop 实现决策顺序 | 路由到 ai-defaults | `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md` | Y | Y | 跨仓引用可达 |
| 查看最近的实现历史 | 路由到日志索引 | `docs/logs/index.md` | Y | **部分** | exists 但「Current」段过时（见 §3 P1-MA3-054） |
| 复制现成的日期文档骨架 | 路由到 examples | `docs/examples/README.md` | Y | Y | — |
| （域快速参考表） | 多域快速路由 | `<area>` / `<path>` 占位 | N/A | N/A | **占位未填充**（18 域全空，见 §3 P1-MA3-056） |
| 目录角色 `docs/errors/` | — | （未声明） | Y（目录存在+README） | N/A | **errors/ 未纳入目录角色表**（见 §3 P1-MA3-055） |
| 目录角色 `docs/ppts/` | — | （未声明） | Y（目录存在） | N/A | **ppts/ 未纳入目录角色表且无 README**（见 §3 P1-MA3-055） |

### 1.2 `AGENTS.md` §快速路由（与 `docs/index.md` 重叠核查）

| AGENTS.md 条目 | docs/index.md 对应条目 | 重叠裁决 |
|----------------|----------------------|---------|
| 了解产品基线 → product-scope/app-overview | 了解项目目标和产品形态 / 当前应用层基线 | **重叠**（同事实两处维护） |
| 选择下一个工作项 → backlog/README | 选择下一个 AI 就绪工作项 | **重叠** |
| 实现一个功能 → design/architecture/orm.xml | （多条目覆盖） | **重叠** |
| 更改持久化模型或 API 契约 → orm.xml/api.xml | 了解持久化模型或字典真相 / 从 ORM 生成 | **重叠** |
| 更改页面或视图 → AMIS .view.xml | （index.md 无对应行） | 部分独有 |
| 审查已计划或已完成的切片 → plans/ | 开始或审查非平凡实现 | **重叠** |
| 运行或验证项目 → project-context/codebase-map | （index.md 无对应行） | 部分独有 |
| 起草/执行/审计 plans → plan-authoring/log-guide | （index.md 无对应行） | 部分独有 |

→ 8 行中 5 行与 `docs/index.md`「首先阅读」表语义重叠，**无交叉引用**声明单一真相源（见 §3 P1-MA3-052）。

### 1.3 子索引内容匹配抽样

| 子索引 | exists | matches stated purpose | notes |
|--------|--------|----------------------|-------|
| `context/README.md` | Y | Y | 5 文件清单 + 归属规则清晰 |
| `backlog/README.md` | Y | Y | 工作项表（67+ 行）+ roadmap 引用 |
| `design/README.md` | Y | Y | 18 域 + portal + 全局文档表；**§编写规则:99 引用 `../nop-entropy-wt/nop-entropy-master/...` 路径**（该路径实测存在，但与全仓其他引用 `../nop-entropy/docs-for-ai/` 不一致——路径约定一致性问题，见 §4 P2-MA3-042） |
| `architecture/README.md` | Y | Y | 建议阅读顺序 9 项 + owner-doc 规则 |
| `skills/README.md` | Y | Y | 21 技能注册表 + 项目定制化层（保护区域/验证/命名/失败模式）|
| `audits/00-audit-execution-guide.md` | Y | Y | 审计工作流指导 |
| `audits/arm-index.md` | Y | Y | 报告清单 + P0/P1 追踪（活跃维护） |
| `bugs/00-bug-fix-note-writing-guide.md` | Y | 部分替代 | 是写作指南非目录索引；目录无 README（见 §3 P1-MA3-051） |
| `logs/index.md` | Y | 部分匹配 | 「Current」段过时（见 §3 P1-MA3-054） |
| `testing/index.md` | Y | Y | — |
| `errors/README.md` | Y | Y | 错误码集中索引（但目录未纳入顶层目录角色，见 P1-MA3-055） |
| `articles/`（无 README） | **N** | N/A | 见 §3 P1-MA3-050 |
| 其余 README（requirements/references/examples/analysis/lessons/retrospectives/input/discussions/process/archive） | Y | Y | 内容匹配声明目的 |

### 1.4 跨仓引用可达性（`../nop-entropy/docs-for-ai/`）

| 引用 | exists | notes |
|------|--------|-------|
| `INDEX.md` | Y | 路由表可达 |
| `00-start-here/ai-defaults.md` | Y | — |
| `02-core-guides/model-first-development.md` | Y | — |
| `02-core-guides/service-layer.md` | Y | — |
| `03-runbooks/` | Y | — |

跨仓引用全部可达（非本仓所有权，仅核验可达性）。

## 2. 步骤 2 — 基于角色路由测试

> 4 角色真实信息需求定制化追踪。hops = 从 `docs/index.md` 起点到可操作答案的跳数。

| persona | need | starting point | hops | found | path taken | problem |
|---------|------|---------------|------|-------|-----------|---------|
| A 新开发人员 | 如何设置开发环境运行项目 | `docs/index.md` | 2 | ✅ | index.md「了解强制 AI 上下文」→ `context/README.md` → `project-context.md`（含验证命令）| 无 |
| A 新开发人员 | finance 过账引擎代码/owner doc 在哪 | `docs/index.md` | 3 | ✅ | index.md「了解当前应用层基线」→ `design/README.md` → finance 行 → `design/finance/posting.md` | 2-3 跳可接受；无直达 finance/posting.md 的顶层条目（轻微） |
| B AI 代理 | 编码前必须遵循哪些当前规则 | `AGENTS.md` | 1 | ✅ | AGENTS.md §操作规则 / §Nop Platform 特定规则 | AGENTS.md 是工作流权威，直达 |
| B AI 代理 | 某域 orm.xml owner doc 在哪 | `docs/index.md` | 1 | ✅ | index.md「从 ORM 模型生成」/「了解持久化模型真相」→ `module-<domain>/model/*.orm.xml` | 占位 `<domain>` 合理 |
| B AI 代理 | Nop Delta 定制 runbook 在哪 | `docs/index.md` | 2 | ✅ | index.md「决定更改应使用模型/Delta/钩子/Java」→ `../nop-entropy/docs-for-ai/INDEX.md` → `03-runbooks/` | 跨仓可达 |
| C 审查者 | 当前审计-修复 roadmap todo 在哪 | `docs/index.md` | 3 | ⚠️ | index.md「为复杂项目规划…roadmap」→ 指向 **authoring skill**（非 roadmap 文件）→ 需推断 → `backlog/audit-remediation-roadmap.md` | **roadmap 文件本身未直接索引**（只索引了产出它的 skill）；多 1 跳推断（见 §3 P2-MA3-040） |
| C 审查者 | 近期实现日志在哪 | `docs/index.md` | 2 | ⚠️ | index.md「查看最近的实现历史」→ `logs/index.md` | **`logs/index.md`「Current」段停在 06-25，实际日志至 07-28**——索引指向过时「当前」（见 §3 P1-MA3-054） |
| C 审查者 | 某 plan 的关闭门控 | `docs/index.md` | 2 | ✅ | index.md「开始或审查非平凡实现」→ `plans/00-plan-authoring-and-execution-guide.md` | — |
| D 维护者 | 何时更新索引 vs 新建文档 | `docs/index.md` | 1 | ✅ | index.md「核心原则」+ `references/document-naming-and-timeliness.md` | — |
| D 维护者 | 哪些文档已知过时 | `docs/index.md` | 2 | ⚠️ | 无专门「已知过时」登记；需读 `logs/` + `bugs/` + `retrospectives/` 推断 | 无集中「已知过时文档」索引（轻微，设计上分散） |
| D 维护者 | 归档规则 | `AGENTS.md` | 1 | ✅ | AGENTS.md §操作规则 14（archive 检查）+ `archive/README.md` | — |

**路由测试结论**：4 角色主路径全部可在 ≤3 跳到达答案（无角色完全无法到达关键答案的 BLOCKER）。2 处 ⚠️：logs/index.md 过时（P1-MA3-054）+ audit-remediation-roadmap 间接可达（P2-MA3-040）。

## 3. 步骤 3 — 结构质量检查

| 检查项 | 结论 |
|--------|------|
| **孤立文件**（无法从任何索引访问） | 抽样 `docs/` 全目录树：未发现完全孤立的核心文档（所有 README/子索引经 `docs/index.md` 目录角色或首先阅读表可达）。`docs/ppts/` 整目录未纳入目录角色（见 P1-MA3-055），属「孤立类别」非孤立文件。 |
| **过时引用**（指向已移动/重命名/删除） | **1 处确认**：`docs/index.md:28` → `docs/articles/README.md`（不存在）。`docs/design/README.md:99` 引用 `../nop-entropy-wt/nop-entropy-master/...` 经独立 closure audit 实测**该路径存在**（与 `../nop-entropy/docs-for-ai/` 内容相同的两个兄弟目录），非过时引用，降级为路径约定一致性 P2（见 §4 P2-MA3-042）。`docs/archive/` 引用断链：未发现（archive/ 仅含 architecture/ + README）。 |
| **深度不平衡**（>3 跳） | 抽样最远路径 3 跳（index.md → design/README → 域 README → 域 state-machine.md）。无 >3 跳到可操作内容。**PASS**。 |
| **重复**（同一规则多索引无交叉引用） | **1 处确认**：`AGENTS.md` §快速路由（8 行）vs `docs/index.md`「首先阅读」表（~40 行）—— 5 行语义重叠，无「本表与 index.md 互为补充/单一真相源」交叉引用声明（见 P1-MA3-052）。 |
| **类别混淆** | **3 处**：`docs/errors/`（错误码索引，有 README）+ `docs/ppts/`（演示材料，无 README）均未纳入 `docs/index.md` §目录角色表（见 P1-MA3-055）；`docs/design/l10n/`（本地化设计，含 `cn-golden-tax.md`）未纳入 `docs/design/README.md` 业务域文档表 + 未在 index.md §目录角色（见 P2-MA3-043）。 |
| **缺少中间索引**（>10 文件目录无 README） | **1 处确认**：`docs/bugs/` 含 13 个 .md 文件（>10 阈值）+ 仅 `00-bug-fix-note-writing-guide.md`（写作指南，非目录索引），**无 README.md**（见 P1-MA3-051）。其余 >10 文件目录（analysis 65 / audits 53 / plans 379 / architecture 33 / design 24 / skills 22）均有 README 或子索引。 |

## 4. 步骤 4 — 发现返回（按严重性排序）

> 本审计为文档层，**原则上无 P0**（plan Non-Goals + Goals 明示）。BLOCKER/MAJOR 类发现登记为 P1（目标 MR2，文档类目标）。MINOR 登记 P2 watch-only。Finding ID 接续 A3.6（止于 P1-MA3-049），本批自 **P1-MA3-050** 起。

### BLOCKER 类（登记为 P1，目标 MR2）

#### P1-MA3-050 — `docs/index.md` 顶层路由器引用不存在的 `docs/articles/README.md`

- **受影响路径**：`docs/index.md:28`（「阅读解释性方法论文章」→ `docs/articles/README.md`）
- **当前差距**：`docs/articles/` 目录存在（含 2 篇文章 `loop-engineering-x-attractor.md` + `mission-driver--loop-engineering.md`）但**无 README.md**。顶层路由器的「首先阅读」表条目指向不存在的子索引。
- **对路由有效性的影响**：代理/人工按顶层路由器追「方法论文章」时到达 404；可能转而忽略 articles 目录或重复创建。BLOCKER 类（plan 定义的 blocker 信号之一：「索引条目指向不存在文件」）。
- **建议**：MR2 补建 `docs/articles/README.md`（列出 2 篇文章 + 目的 + 与 `docs/skills/` 边界），或移除 index.md 该行 / 改指具体文章文件。

#### P1-MA3-051 — `docs/bugs/` 13 文件无 README 中间索引（>10 阈值）

- **受影响路径**：`docs/bugs/`（13 个 .md 文件）+ `docs/index.md:55,118`（路由到 `00-bug-fix-note-writing-guide.md`）
- **当前差距**：`docs/bugs/` 含 13 个回归笔记（>10 文件阈值）但**无 README.md**；仅 `00-bug-fix-note-writing-guide.md`（写作指南，非目录索引/清单）。`docs/index.md` 将「查找过去的微妙回归」路由到该 00- 指南而非目录索引。
- **对路由有效性的影响**：维护者/审查者无法经单一索引浏览全部回归历史（须 `ls` 目录）；新回归笔记的组织规则仅靠写作指南隐含。BLOCKER 类（plan 定义的 blocker 信号之一：「>10 文件目录无 README」）。
- **建议**：MR2 补建 `docs/bugs/README.md`（回归笔记清单表 + 分类 + 与 00- 指南交叉引用），或在 00- 指南顶部增「目录索引」节。

### MAJOR 类（登记为 P1，目标 MR2）

#### P1-MA3-052 — `AGENTS.md` §快速路由 vs `docs/index.md` 路由表重叠无交叉引用

- **受影响路径**：`AGENTS.md` §快速路由（8 行）+ `docs/index.md` §首先阅读（~40 行）+ §路由权威
- **当前差距**：两表 5 行语义重叠（产品基线/工作项选择/功能实现/模型契约/计划审查），**无交叉引用**声明哪个是单一真相源。`docs/index.md` §路由权威声明「本文件是顶级文档路由器 / AGENTS.md 拥有代理工作流规则」，但未明确两表的重叠裁决（应以哪个为准 / 互为补充）。
- **对路由有效性的影响**：同一事实两个维护点 → 漂移风险（一处更新另一处遗忘）。已知风险信号（plan Current Baseline 风险信号 #2）经核实为真。
- **建议**：MR2 裁决——方案 A（推荐）AGENTS.md §快速路由顶部加交叉引用「完整路由见 docs/index.md，本表仅列代理高频任务」+ 删除完全重叠行；方案 B index.md §首先阅读加注「AGENTS.md §快速路由为代理精简子集」。

#### P1-MA3-054 — `docs/logs/index.md`「Current」段过时（停在 06-25，实际至 07-28）

- **受影响路径**：`docs/logs/index.md`（「Current:」段）+ `docs/index.md:54`（路由到 logs/index.md）
- **当前差距**：`logs/index.md`「Current:」段最新条目为 `2026/06-25.md`，但 `docs/logs/2026/` 实际日志文件至 `07-28.md`（缺 06-26 ~ 07-28 共 ~30 天日志登记）。
- **对路由有效性的影响**：审查者/维护者按「Current」追「最近实现历史」被指向 1+ 月前日志；与 index.md「查看最近的实现历史」声明目的不符。
- **建议**：MR2 更新「Current:」段至最新（07-28）+ 评估是否改为「按年/月自动列表」机制避免再次过时。

#### P1-MA3-055 — `docs/errors/` + `docs/ppts/` 未纳入 `docs/index.md` §目录角色（orphan 类别）

- **受影响路径**：`docs/index.md` §目录角色（~22 项）+ `docs/errors/`（有 README）+ `docs/ppts/`（无 README）
- **当前差距**：`docs/` 下 26 个子目录中，`errors/`（错误码集中索引，有 README，由 plan 2026-07-20-2200-1 落地）与 `ppts/`（演示材料，无 README）**均未纳入** `docs/index.md` §目录角色表。`errors/` 经 audits/00-audit-execution-guide.md 等间接可达，`ppts/` 完全无顶层路由。
- **对路由有效性的影响**：类别混淆——存在的目录无顶层角色声明；代理可能重复创建错误码索引或忽略 ppts 资产。
- **建议**：MR2 在 index.md §目录角色补 `docs/errors/`（错误码集中索引）+ `docs/ppts/`（演示材料，可选）两行。

#### P1-MA3-056 — `docs/index.md` §域快速参考表占位未填充（18 域全空）

- **受影响路径**：`docs/index.md:91-97`（§域快速参考[可选]）
- **当前差距**：表为模板占位（`<area>` / `<path>` / `<skill-name|none>`），18 业务域 + notify 全部未填充。表头自述「这是可选的；小型项目可以跳过」，但本项目为 19 域多模块项目（AGENTS.md / design/README.md 均以多域组织）。
- **对路由有效性的影响**：多域项目本应经此表一次查找路由到正确 owner doc + 技能；占位迫使代理经 design/README.md 多跳。已知风险信号（plan Current Baseline 风险信号 #1）经核实为真。
- **建议**：MR2 填充 18+1 域快速参考表（域 → design/<domain>/ owner doc → 推荐 skill），或将该节改为「见 design/README.md 业务域设计文档表」交叉引用并删除占位。

### MINOR 类（P2 watch-only）

#### P2-MA3-040 — `audit-remediation-roadmap.md` / `arm-index.md` 未直接索引（审查者多 1 跳）

- **受影响路径**：`docs/index.md:47`（仅索引 authoring skill）+ `docs/backlog/audit-remediation-roadmap.md` + `docs/audits/arm-index.md`
- **当前差距**：index.md「为复杂项目规划…roadmap」条目指向 authoring **skill**（`audit-remediation-roadmap-authoring-prompt.md`）而非产出的 roadmap 文件本身。审查者追「当前审计-修复 todo」需经 skill 推断到 roadmap 文件（角色 C 测试 ⚠️）。
- **影响**：轻微——roadmap 文件经 backlog/README + audits/ 间接可达，非完全孤立。
- **建议**：MR2 在 index.md 增「查看审计-修复 roadmap 当前 todo」→ `backlog/audit-remediation-roadmap.md` + `audits/arm-index.md` 行；或在 backlog/README 增该 roadmap 引用。

#### P2-MA3-041 — `docs/articles/` 目录无 README（与 P1-MA3-050 同根因）

- **受影响路径**：`docs/articles/`（2 篇文章，无 README）
- **当前差距**：与 P1-MA3-050 同根因（顶层路由引用不存在的 README）。本条登记目录级缺口 watch-only；修复随 P1-MA3-050 一并（补建 README）。
- **建议**：随 P1-MA3-050 一并修复。

#### P2-MA3-042 — `docs/design/README.md:99` 平台文档路径约定不一致（降级，原误判 P1）

- **受影响路径**：`docs/design/README.md:99`（§编写规则）+ 全仓平台文档引用约定
- **当前差距**：design/README.md 引用 `../nop-entropy-wt/nop-entropy-master/docs-for-ai/02-core-guides/application-project-docs-and-domain-design.md`，而 `AGENTS.md` + `docs/index.md` 全部一致使用 `../nop-entropy/docs-for-ai/`。**经独立 closure audit 实测两条路径均存在且内容相同**（nop-entropy-wt/nop-entropy-master 与 nop-entropy 是两个含相同 docs-for-ai 内容的兄弟目录）。初版报告误判为「过时不存在路径」(P1)，closure audit 证伪后降级为 P2。
- **影响**：轻微——两条路径都可达目标内容；仅是「哪个兄弟目录是正式定位」的约定不一致，可能困惑维护者。README 有「若已正式定位 nop-entropy 兄弟目录，则替换为该路径」对冲注记。
- **建议**：MR2 统一全仓平台文档引用为 `../nop-entropy/docs-for-ai/`（AGENTS.md/index.md 主流）并移除 design/README 对冲注记；或 owner doc 显式声明两兄弟目录等价。

#### P2-MA3-043 — `docs/design/l10n/` 未纳入 `docs/design/README.md` 业务域文档表（orphan 子目录）

- **受影响路径**：`docs/design/l10n/`（含 `cn-golden-tax.md`）+ `docs/design/README.md` 业务域设计文档表
- **当前差距**：`docs/design/l10n/` 目录存在（本地化设计，含 cn-golden-tax.md）但**未纳入** `docs/design/README.md` 业务域设计文档表（该表列 18 域 + portal，无 l10n 行），亦未在 `docs/index.md` §目录角色。仅经浏览 `docs/design/` 可达。由独立 closure audit spot-check 发现（执行者初版遗漏）。
- **影响**：轻微——单文件子目录（<10 阈值），非 BLOCKER；与 P1-MA3-055（orphan 类别）同精神但规模更小。
- **建议**：MR2 在 design/README.md 业务域表增 l10n 行（或在 design/README 说明 l10n/portal 为非基线扩展目录的归类规则）。

## 5. 裁决

- **Verdict：FAIL（有路由缺口）** —— 顶层路由器存在 1 处指向不存在文件的 BLOCKER 类缺口 + 1 处 >10 文件目录无 README 的 BLOCKER 类缺口 + 4 处 MAJOR（路由表重叠无交叉引用 / 过时子索引 / orphan 类别 / 占位未填充）。
- **零 P0**：索引审查为文档层（plan Goals/Non-Goals 明示原则上无 P0）；未发现索引断链致代理实际走入错误实现路径的活跃证据（角色路由测试 4 角色主路径全部 ≤3 跳可达）。
- **域快速参考表占位裁决**：占位不合理（19 域多模块项目应填充）→ P1-MA3-056。
- **AGENTS.md-vs-index.md 重复裁决**：重复成立（5 行重叠无交叉引用）→ P1-MA3-052；裁决为「应以 docs/index.md 为单一导航真相源，AGENTS.md §快速路由为代理精简子集并加交叉引用」（MR2 落地）。
- **路径约定一致性裁决**：design/README.md:99 引用的 `../nop-entropy-wt/nop-entropy-master/...` 路径经 closure audit 实测**存在**（与 `../nop-entropy/` 内容相同的两兄弟目录），初版误判「过时不存在」经证伪降级 P2-MA3-042（约定一致性，非断链）。
- **MA3 累计**：本审计 6 P1（P1-MA3-050/051/052/054/055/056）+ 4 P2（P2-MA3-040/041/042/043）。MA3 累计 P1=47（A3.1 13 + A3.2 2 + A3.3-A3.5 22 + A3.6 4 + 本审计 6），P2=30（A3.1 8 + A3.2 1 + A3.3-A3.5 13 + A3.6 4 + 本审计 4）。

## 6. 与 A3.1-A3.6 已登记 P1 交叉去重

本审计 6 项 P1 全部为**索引路由结构**维度（条目存在性/匹配/可达/结构质量），与 A3.1-A3.6 已登记 P1-MA3-001~049（设计文档内容质量 / 完整性扫描 / owner-doc drift / API 契约）**维度不同，无重复登记**。仅以下弱关联注明：
- P2-MA3-042（design/README 路径约定一致性）与 A3.3-A3.5 owner-doc drift 同属「文档内部引用」类，但本条是**索引子文件内的路径约定一致性**（两路径均存在，非断链），A3.3-A3.5 是**业务语义 drift**，不重叠。
- P1-MA3-056（域快速参考占位）与 A3.1 P1-MA3-009（8 扩展域从导航遗漏）/ A3.2 P1-MA3-022（flow-overview 缺扩展域引用）同根因方向（扩展域全局视图缺位），但本条是**顶层 index.md 快速参考表**，A3.1/A3.2 是 design/ 内部导航，MR2 可协同但不重复。

## 7. 剩余风险

- **索引时效性**：文档树持续增长（plans/ 379 文件、audits/ 持续增长、design/ 21 子目录），无自动化索引刷新机制——本审计发现 logs/index.md 过时即为实例。MR2 应评估「子索引自动生成/校验脚本」。
- **archive 引用断链**：AGENTS.md §14 要求引用未在预期路径找到时检查 archive/。本审计未发现活跃断链，但随着归档增长风险上升。
- **跨仓索引**：`../nop-entropy/docs-for-ai/` 非本仓所有权，仅核验可达性；其内部索引质量归 nop-entropy 仓。

## 8. 范围内 Non-Goals 确认（未越界）

- ✅ 未做设计文档内容质量审查（归 A3.1 done）
- ✅ 未做前瞻性文档缺失扫描（归 A3.2 done）
- ✅ 未做 owner doc vs 代码 drift（归 A3.3-A3.5 done）
- ✅ 未做 API 契约一致性（归 A3.6 done）
- ✅ 未做可定制性验证（归 A3.8 todo）
- ✅ 未批量修复索引（finding 经 R2.0 进入 MR2）
- ✅ 未审计 nop-entropy/docs-for-ai/ 兄弟目录索引（仅核验跨目录引用可达性）
- ✅ 未手改生成物或 ORM
