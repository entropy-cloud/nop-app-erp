# 需求-实现符合性审计的范围与方法讨论

> 日期：2026-08-02
> 状态：开放（多轮澄清中）
> 起因：audit-remediation（MA1-MA7 + MR1-MR6 + MQ）全部闭包后，人工观察仍发现"明显很多地方与架构设计和最佳实践不一致，逻辑实现也不符合需求"。需要判断：是否新建"逐模块、逐功能点"的需求-实现符合性审计 mission + roadmap，以及审计什么。

## 源文件

- `docs/backlog/audit-remediation-roadmap.md`（v28，MR1-MR6 CLOSED + MQ Q0-Q7 done）
- `docs/audits/arm-index.md`（292 条 P0/P1/P2 finding）
- `docs/audits/audit-remediation-scope-and-dimension-matrix.md`（维度矩阵终态大量 `⚠️(P1)`/`⚠️(residual)`/deferred）
- `docs/requirements/product-scope.md`（产品需求基线，曾被标记陈旧 P1-MA3-023）
- 各域 `docs/design/<domain>/use-cases.md`（18 域共 192 条 UC）
- `docs/context/project-context.md §已知失败模式`（compliance 基线漂移复发）

## 为什么审计了很多轮仍然不一致（根因分析，基于仓库证据）

1. **审计收口方式系统性允许"文档对齐代替需求实现"**：arm-index 大量 finding 以方案 B（documented simplification / owner doc Deferred 标注）关闭而非方案 A（真正实现）。例：P1-MA2-001 GRNI 冲回、P1-MA2-018 年初余额、P1-MA2-022 FX reversal、R2.6 material-reservation 整子系统 Deferred 正式化。审计闭环后 doc↔code 自洽，但**需求层（product-scope/use-cases）与实现的分歧被写进文档合法化**。
2. **真相源在审计过程中被自身修订**：P1-MA3-062 显示 MR5 曾用 owner doc 不承认的"S/D 之分"伪概念豁免 D-mutation，违反 AGENTS.md §真相源优先级，被迫新建 MR6 纠正。架构纪律本身被 roadmap 覆盖过。
3. **守卫机制反复失守**：compliance 基线漂移复发（R6.9 closure 误把 checker 纯 reporter 退出码当门控，`project-context.md §已知失败模式` 登记的复发模式）；MV 回填 102 条陈旧 todo 标签；R3.5 Round 3 补审计 14 份 closure-pending 计划。
4. **审计粒度是"域×维度 + 抽样"而非"功能点全量"**：A1.13 为 12 域合并一个工作项、A2.14 为 5 域合并、A4.5/A5.6/A7.3 均抽样。维度矩阵终态大量停在 `⚠️(P1)`/`⚠️(residual)`/deferred——"audit done"=已登记分类，不等于干净。
5. **41 个 arm-index 内嵌 successor 声明未系统映射到 `docs/backlog/README.md` 既有追踪行（81 行含 successor/Deferred）**：触发条件满足性依赖人工/代理"经实时仓库核实"，无自动回队机制；部分声明已 done 但计数口径含叙述性提及（`rg -c successor`=41 含非注册项）。**真实差距 = 追踪完整性 + 触发条件满足性 + 回队正确性**，而非"无集中追踪"。

**结论**：不是"没审计"，而是审计的**维度定位**（doc↔code 一致性）与**收口偏好**（方案 B 优先）共同导致需求-实现分歧被系统性地"文档化"而非"实现化"。

## 新审计的维度定位（已确认方向）

**不做**：复跑 MA1-MA7（架构漂移类，已充分覆盖，且与既有 finding 去重成本高）。

**要做**：**需求→实现符合性审计**（requirement traceability / functional compliance）——从需求真相源（product-scope + use-cases + owner doc 需求契约）出发，逐模块逐功能点核对**运行时行为**是否符合需求，而非 doc↔code 文本一致性。

## 候选审计内容（待逐项确认）

| # | 审计内容 | 判据/方法 | 状态 |
|---|---------|----------|------|
| 1 | 需求追踪矩阵 | 每个 use-case → owner doc 契约 → 代码路径 → 测试断言 → **运行时行为**，五级逐点核验 | 方向确认，粒度已定（见 Q2） |
| 2 | 已裁决简化复查 | arm-index 全部 documented simplification / Deferred 逐项复核，区分"有意设计"vs"静默降级"，与 product-scope 对照 | 方向确认 |
| 3 | successor 触发条件复查 | 41 个 successor 是否有触发条件、是否已满足、是否该回队 | 方向确认 |
| 4 | 运行时行为验证 | 关键功能点用现有 E2E/单测 + 临时探针验证实际行为（不只静态读代码） | 方法待定 |
| 5 | 跨域契约行为一致性 | 状态机 dict × code × owner doc 的**行为**一致（MA3 只做了文本一致） | 方向确认 |
| 6 | Nop 最佳实践回归复核 | I\*Biz Facade / Processor 纪律 / 事务边界只查"新引入回归"，不重审存量 | 范围待定 |
| 7 | 保护区域行为复核 | 会计过账 / 数据删除 / ORM 的实际行为守卫 | 方向确认 |
| 8 | 审计过程自身纪律 | checker 门控 vs 退出码、closure-audit 独立性 | 方向确认 |

## 未解决问题（需人工/后续讨论裁决）

### Q1 — 需求真相源权威性（2026-08-02 已裁决：c）
> **裁决**：采用 **(c) 两者逐项对照，分歧处逐项裁决**。需求真相源 = `product-scope.md`（顶层范围）+ 各域 `use-cases.md`（192 UC 功能契约，权威）；owner doc 为**设计参考**（机制说明、状态机定义、跨域协作契约）。**三者逐项对照核验**，分歧处逐项裁决；当 owner doc 与 product-scope/use-cases 冲突时，**一律以需求真相源为准**，推定 owner doc 已向实现妥协（除非有显式人工批准登记证明是有意设计——证据标准见 roadmap MA2 详情"显式人工批准记录"三判据）。notify 无 use-cases → **0.2 必须补写完整 use-cases**（不标注 N/A——notify 是已实现的跨域子系统，必须有需求契约；补写后纳入 MA1 审计范围，可能新增切片行）。

### Q2 — 审计粒度与成本（方向已定：功能切片全量，禁止抽样）
> **2026-08-02 已裁决方向**：采用"**功能切片 × 显式 UC 清单**"粒度全量枚举——18 域 192 UC 拆为 51 个功能切片（见 `requirement-compliance-roadmap.md` MA1 表），每个切片 = 一份 plan，全表即完整工作量。**禁止合并切片抽样**——正是"A1.13 十二域合并一个工作项"导致执行退化为抽样的教训。notify 无 use-cases 文件，待 0.2 裁决补切片。此项由规划会议定案，不再列入待裁决。
候选（历史参考）：
- (a) S/A 级域全量（finance/mfg/hr/assets/pur/sal/inv），B/C 级抽样——参照 arm-scope 复杂度分级（**已被否决**：抽样即漏检）
- (b) 全 19 域全量（成本最高，但一次收敛）（**采纳**，但按功能切片拆行，非整域一行）
- (c) 人工点名的功能点优先（用户最痛处先查）（作为 MA1 的启动优先级参考，不替代全量）

### Q3 — 已裁决简化（方案 B）复查范围（2026-08-02 已裁决：a）
> **裁决**：采用 **(a) 全部复查**。arm-index 中所有方案 B（documented simplification / Deferred 标注）关闭项与 product-scope 逐项对照后重新分级（P0/P1/P2/接受）。无抽样、无"仅会计保护区域"缩窄、无"仅人工点名"缩窄——与 Q2 完整枚举纪律一致。复查判据与"显式人工批准记录"证据标准见 roadmap MA2 节。

### Q4 — 修复义务边界（2026-08-02 已裁决：a，无例外通道）
> **裁决**：采用 **(a) P0/P1 必须实现，禁止再用方案 B 关闭**；P2 登记不强制。**无"技术不可行"例外通道**——技术不可行项（如 P0-MA2-018 字面 UK 三重契约冲突）须找到技术可行路径（如重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK），而非退缩到方案 B 降级。**唯一出口**：审计发现**需求本身不合理**（非实现不达标）时，经人工批准修改 product-scope（这是需求变更，非方案 B 降级；须登记变更理由 + 影响面 + 批准人）。会计/数据安全类强制实现（对齐 `ai-autonomy-policy.md` 保护区域，无任何例外）。此裁决正式化为 MR1 判据，roadmap 推荐口径（Q4 选项 (a)+(c) 缝合）**升级为纯 (a)**——取消"混合"后门。

### Q5 — 与既有审计的去重边界（已定方向：行为符合性 vs 断言强度）
MA3 已做 doc↔code drift、A4.6-A4.8 已做 view.xml drift。新审计**不重复**：不重做文本一致性。**已定**：运行时行为验证（MA4）审"行为是否符合需求"，与 A5.6 审"E2E 断言强度"判据不同——按此边界执行，Q5 非阻塞。

### Q6 — mission 与 roadmap 形态（已确认）
新 mission（`requirement-compliance`）+ 新 roadmap（`docs/backlog/requirement-compliance-roadmap.md`），沿用 MQ 文档先行工作流。**需要独立审计方法论文档**（落盘 `docs/audits/requirement-compliance-methodology.md`）作为 Phase 1 契约（仿 MQ Q0-Q7 范式）——M0.1 工作项承载。

### Q7 — 审计与修复的关系（已确认：同一闭环自动衔接）
roadmap **同时包含审计与后续修复**，且由 mission driver 自动衔接：
- 审计发现 P0 → **MR0 即时止血通道**（发现即独立 fix plan，不等里程碑完成）
- 审计发现 P0/P1 → **R1.0 展开器自动汇总为 RC-R1.n 修复行**（前缀 RC 避免与 audit-remediation R1.x 混淆） → mission driver 逐项自动执行（DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → done），无需人工逐个介入
- 修复义务：P0/P1 强制实现（禁止方案 B 关闭，Q4 裁决后生效）

## 已确认决策（截至本版）

1. 需要新建 mission + roadmap（人工已确认方向）。
2. 审计维度定位 = 需求→实现符合性，非 doc↔code 文本一致性。
3. 沿用 MQ 文档先行工作流：方法论文档 → 独立子代理审查 → 逐域执行 → 独立 closure audit。
4. 不复跑 MA1-MA7 架构漂移类审计。

## 阻止实施的未解决项

- ~~Q1（需求真相源）未裁决~~ → **2026-08-02 已裁决：(c) 逐项对照，需求真相源优先**。
- ~~Q4（修复义务边界）未裁决~~ → **2026-08-02 已裁决：(a) P0/P1 必须实现，禁止方案 B，无例外通道**。

## 后续行动

1. ~~本讨论文档经独立子代理审查~~ → 已完成（三轮独立审查，CONDITIONAL PASS → PASS）。
2. ~~裁决 Q1-Q4~~ → **已完成**：Q1=（c）、Q2=（b 功能切片）、Q3=（a 全部复查）、Q4=（a 无例外）、Q5/Q6/Q7 已定方向。
3. ~~起草 roadmap 骨架 + mission json~~ → 已完成（v1.2，经三轮独立审查修订）。
4. **roadmap 转 ready**（本次）：Q1/Q4 裁决后解除阻塞，M0 工作项 Deps 满足 → 转 ready，可启动 mission driver 执行。
