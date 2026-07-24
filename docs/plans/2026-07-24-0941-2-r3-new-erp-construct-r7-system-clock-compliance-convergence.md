# 2026-07-24-0941-2-r3-new-erp-construct-r7-system-clock-compliance-convergence R3 `new Erp*()` 构造 + R7 `System.currentTimeMillis()` 合规收敛

> Plan Status: active
> Mission: erp
> Work Item: compliance checker R3（`new Erp*()` 构造实体，基线 19）+ R7（`System.currentTimeMillis()`，基线 2）命中项收敛——基线门控上线后的既定 successor
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md` §Deferred「R8（42）/ R10（51）/ R1d（23）/ R3（19）命中项修复归后续专项计划（**基线门控上线后触发**）」——**触发条件已满足**：0930-1 已将 checker 接入 CI（`.github/workflows/compliance.yml`）+ 落盘基线（`docs/audits/compliance-baseline.md`），dead armor → live guard 转换完成（2026-07-24）。本计划承接 R3 + R7 两项：R3 来自前述 §Deferred；**R7（基线 2）虽不在 0930-1 §Deferred 显式列表，但经 `AGENTS.md:186` 平台 helper 强制规则（`CoreMetrics.currentTimeMillis()` 而非 `System.currentTimeMillis()`）确认为已落实的实时缺陷（R13 不可降级项），同属「基线门控上线后可收敛」范围，一并纳入本计划**。R8 经 `2026-07-20-2200-1` §M-5 审计已证全合法基线、R1d 经 `2026-07-24-2000-1` 评估为 watch-only residual，均不在本计划范围。
> Related: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §闭包项 #4（F8 checker CI 集成）、`docs/audits/compliance-baseline.md`（R3=19/R7=2 基线 + 调高基线唯一途径规则）、`docs/lessons/06-codegen-product-edit-overwrite.md`（代码生成产物编辑必被覆盖——`_` 前缀保护区域，与 R3 瞬态聚合「勿用 domain entity 作内存容器」不同主题，仅作平台 helper 强制规则参照系）、`docs/audits/nop-compliance-checker.sh`（R3/R7 规则定义）、`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`CoreMetrics` 平台 helper 权威来源，支撑 R7）
> Audit: required

## Current Baseline

compliance checker CI 门控已上线（0930-1，2026-07-24）：`.github/workflows/compliance.yml` 每次运行 checker 后将汇总表与本基线比对，**任何规则命中数超过基线即 CI 失败**（单向收紧）。R3/R7 命中项修复的触发条件「基线门控上线后」**已满足**。

**R7（基线 2，确定性违规）**——AGENTS.md + lesson 06 明示「`CoreMetrics.currentTimeMillis()` 而非 `System.currentTimeMillis()`」（平台 helper 强制规则）。两处均为生产代码直调：

| # | file:line | 上下文 | 替换为 |
|---|-----------|--------|--------|
| 1 | `ErpFinGlMappingResolver.java:259` | `lastLoadTimeMillis = System.currentTimeMillis();`（GL Mapping 缓存 TTL 降级时间戳，A1 落地代码） | `CoreMetrics.currentTimeMillis()` |
| 2 | `ErpMdExchangeRateApiClientFactory.java:69` | `long now = System.currentTimeMillis();`（汇率 API 客户端 TTL 缓存，D1 落地代码） | `CoreMetrics.currentTimeMillis()` |

**R3（基线 19，混合：含 false positive + 瞬态聚合；经 iter-1 独立审查实测修订）**——checker 规则 `rgrep_prodjava 'new Erp[A-Z]'`（`nop-compliance-checker.sh:163`）匹配任何 `new Erp*(`。iter-1 独立审查逐处读码核实，**19 处中绝大多数为非 ORM 实体类**（引擎/service 类、support/value 类、内部投影类），规则显著过匹配。修订后三态分布（Phase 1 须全量复核定终值）：

| 类别 | iter-1 实测计数 | 代表站点（已核实） | 处置 |
|------|----------------|--------------------|------|
| **A. false positive（Erp* 前缀非实体类）** | **~14/19** | `new ErpApsSchedulingEngine(...)` ×3（aps，调度引擎 service 类）、`new ErpQaActionImpl(...)`（qa，**代码注释明示「私有内部投影类（非 ORM 实体），不适用 newEntity()」** `NcrLifecycleService:121`）、`new ErpCrmTerritoryPipeline()`（crm，**不在 orm.xml**）、`new ErpCrmPipelineAccumulator()`（crm，不在 orm.xml）、`new ErpFinPostingMetricsSnapshot()` + 其 `.MetricValue` 内部类构造 ×4（fin，support/value 类） | 收紧 checker R3 测量口径排除非实体类，诚实降基线（见 Phase 1 regex 裁决 option c） |
| **B. 合法持久化创建**（`new Erp*()` + 同方法块 `saveEntity`） | **~0** | iter-1 审查逐处核实：原草案列为 B 的代表站点实为 C 或 A——`new ErpSalOrderLine()`（sales，**评估快照返回，调用方负责持久化**，`ErpSalPricingRuleEngine:204`）、`new ErpHrGapAnalysis()`（hr，`daoProvider==null` 时 fallback 路径，`GapAnalysisCalculator:86`）均非直接 saveEntity | B 类可能为空；Phase 1 全量复核后若确认 B=0，Non-Goal「不重构合法持久化创建」自动满足（无对象） |
| **C. 瞬态聚合/虚拟实体**（`new Erp*()` 作内存计算容器，不持久化） | **~5** | `new ErpMfgMrpDemand()`（mfg，**代码注释明示「内存构造 demand（不持久化）」** `SimulationMrpEngine:272`）、`new ErpCrmQuota()`（crm virtual）、`new ErpCrmFunnelStageMetrics()`、`new ErpSalOrderLine()`（snapshot）、`new ErpHrGapAnalysis()`（fallback） | 能力评估：若 DTO 更合适且低风险则重构；否则登记瞬态用途理由保留 |

**关键裁决点（Phase 1，iter-1 审查 Major-2 强化）**：R3 checker regex 当前过宽——19 处中 ~14 处为非 ORM 实体类（引擎 / support/value 类 / 内部投影类 / DTO）。收紧测量口径是**诚实测量校准**（让规则测量其意图：domain entity 构造），非「调尺子规避」。**iter-1 审查指出草案原列 option (a) 后缀排除法（Engine/Impl/Dto 等）不足**——仅能排除 ~4 处（`ErpApsSchedulingEngine`/`ErpQaActionImpl`），余 ~10 处非实体类（`ErpCrmTerritoryPipeline`/`ErpCrmPipelineAccumulator`/`ErpFinPostingMetricsSnapshot` 及内部类）无匹配后缀仍被计数。Phase 1 须评估更稳健的 option (c)：交叉引用 `*.orm.xml` 实体声明，仅对已注册实体类名计 R3（精确校准，0 false positive / 0 false negative）。

**checker 基线**（当前）：R3=19 / R7=2。本计划预期：R7 → 0（确定性修复）；R3 按三态处置 + 测量口径校准后下降至真实 domain entity 瞬态构造计数（~5 或更低，视 C 子集 DTO 化程度）。

剩余差距：R3/R7 命中项未收敛；R3 测量口径过宽（含 ~14 false positive）未校准。

## Goals

1. **收敛 R7**：2 处 `System.currentTimeMillis()` → `CoreMetrics.currentTimeMillis()`，R7 基线 → 0。
2. **权威分类 R3**：对 19 处 `new Erp*()` 逐处三态分类——(A) false positive（Erp* 前缀非实体类）/ (B) 合法持久化创建 / (C) 瞬态聚合虚拟实体。每条记录 file:line + 类性质证据（ORM 实体？持久化路径？瞬态用途？）+ 判定。
3. **校准 R3 checker 测量口径**：在 Phase 1 裁决 R3 规则校准方案——候选 (a) 后缀排除法（仅排除 Engine/Impl 等已知后缀，残留 ~10 false positive）、(b) 保留宽 regex + baseline 注记 false positive 子集、**(c) 交叉引用 `*.orm.xml` 实体声明仅对已注册实体类计 R3（精确校准，iter-1 审查推荐）**——使基线反映真实 domain entity 构造计数。
4. **收敛 R3 瞬态聚合子集**：对判定「应改 DTO」的低风险瞬态站点重构为 DTO（若 Phase 1 评估认定 DTO 更合适且不破坏业务契约）；保留的瞬态/合法站点登记理由。
5. **更新基线 + 反模式自检**：`compliance-baseline.md` 记录 R3 新基线（真实计数）+ R7=0 + 测量口径校准注记；owner doc（若涉及 DTO 重构）补反模式自检。

## Non-Goals

- **不重构合法持久化创建（B 类）**——`new Erp*()` + `saveEntity()` 是 Nop 标准模式，非违规。仅登记为合法基线。
- **不改 ORM 模型 / 不改 biz 方法签名 / 不改 API 契约**（除非某瞬态聚合重构为 DTO 改变内部返回类型——仅限低风险 + 不破坏公共契约的站点）。
- **不处理 R8（42 Processor 无 xbiz）**——`2026-07-20-2200-1` §M-5 已审计全合法（xbiz 按实体命名 + Java @Inject），为既定合法基线。
- **不处理 R1d（23 findAllByQuery）**——`2026-07-24-2000-1` 已评估 watch-only residual（可机械替换候选 <10）。
- **不处理 R10（51 REQUIRES_NEW）**——多为 `ErpFinPostingExceptionRecorder` 审计日志合法用途，独立 successor。
- **不在功能 PR 中直接调高基线**——基线变更须经本独立计划裁决（`compliance-baseline.md` 既定规则）。
- **不强求 R3 → 0**——合法持久化创建是标准模式，R3 真实基线 > 0 是预期健康态；目标是诚实测量 + 收敛真实违规，非清零。

## Task Route

- Type: `architecture change`（compliance 合规结构改进，结果面 = R3/R7 命中项收敛 + R3 regex 校准）
- Owner Docs: `docs/audits/compliance-baseline.md`（基线 + 调高/校准规则）、`docs/audits/nop-compliance-checker.sh`（R3/R7 规则定义）、`docs/lessons/06-*.md`（平台 helper 强制规则）、`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`CoreMetrics` 平台 helper）
- Skill Selection Basis: `nop-backend-dev`（匹配「平台 helper / 产品化可定制性自检 / 实体创建范式」工作方法，判定 `new Erp*()` 是否应改 DTO/`newEntity`）；`nop-debugging`（R3 regex 收紧的 false-positive 根因分析）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java + checker shell 脚本收敛，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 — R3 全量枚举 + 逐处三态分类 + R7 确定性核实 + R3 regex 校准裁决

Status: planned
Targets: 全域 `src/main/java`（排除 `_gen`/`target`/test）、`docs/audits/nop-compliance-checker.sh:156-167`（R3 规则）、`docs/audits/compliance-baseline.md`
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Proof`
- Prereqs: 无（CI 门控已上线）

- [ ] `Proof`：产出 R3 权威候选清单——多行 grep（`rg -n 'new Erp[A-Z]' module-*/erp-*-service/src/main/java module-*/erp-*-dao/src/main/java`，排除 `_gen`/`/test/`）枚举全 19 处，逐处附类全限定名。与 checker 实测计数（R3=19）对账。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：逐处三态分类——(A) **false positive**：确认非 ORM 实体（grep `@Entity`/查 `<domain>/model/*.orm.xml` 无对应 entity 声明；如 engine/service/DTO Impl 类）；(B) **合法持久化创建**：确认走 `dao().saveEntity()` 或等价持久化（grep 同方法块内 saveEntity/`daoFor(...).saveEntity`）；(C) **瞬态聚合/虚拟实体**：确认作内存计算容器不持久化（无 saveEntity 调用，仅 setter 聚合后读取字段）。每条记录 file:line + 类全限定名 + 类性质证据 + 判定 + 理由。
  - Skill: `nop-backend-dev`
- [ ] `Decision`（R3 测量口径校准裁决）：裁决如何收紧 `nop-compliance-checker.sh` R3 规则以排除 (A) false positive。候选：(a) **后缀排除法**——加否定断言排除已知非实体后缀（`Engine`/`Impl`/`*Dto`/`*Request`/`*Result`/`*Input`/`*Response`）；**残留风险（iter-1 审查 Major-2 实证）**：仅能排除 ~4 处（`ErpApsSchedulingEngine`/`ErpQaActionImpl`），余 ~10 处非实体类（`ErpCrmTerritoryPipeline`/`ErpCrmPipelineAccumulator`/`ErpFinPostingMetricsSnapshot` 及内部类）无匹配后缀仍被计数，校准后基线仍虚高 ~15。(b) **保留宽 regex + baseline 注记** false positive 子集（不降基线，仅文档化）。**(c) 交叉引用 `*.orm.xml` 实体声明**——从全域 `<entity className="...">` 提取已注册实体类名白名单，R3 仅对 `new <RegisteredEntity>()` 计数（精确校准，0 false positive / 0 false negative；实现成本=checker 脚本加一段 orm.xml 实体名提取）。记录选择 + 残留风险（option a 残留 ~10 false positive 是否可接受 / option c 实现复杂度 / 未来新增实体自动纳入白名单）。
  - Skill: `nop-debugging`
- [ ] `Proof`（R7 确定性核实）：核实 2 处 R7 站点（`ErpFinGlMappingResolver:259` / `ErpMdExchangeRateApiClientFactory:69`）确为 `System.currentTimeMillis()` 直调，确认替换为 `CoreMetrics.currentTimeMillis()` 语义等价（同 epoch millis）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] R3 候选清单 19 处全量枚举 + checker 计数对账
- [ ] 逐处三态分类完成（A false positive / B 合法持久化 / C 瞬态聚合），每条带类性质证据 + 判定
- [ ] R3 regex 校准裁决记录（选 a 或 b + 理由 + 残留风险）
- [ ] R7 两处确定性违规核实完成

### Phase 2 — R7 修复 + R3 regex 校准 + 瞬态聚合子集收敛

Status: planned
Targets: Phase 1 R7 两站点、`docs/audits/nop-compliance-checker.sh`（R3 regex，若选 a）、Phase 1 判定「应改 DTO」的瞬态聚合站点（C 子集，低风险）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Item Types Note: Phase 2 is Fix-heavy (R7 clock helper + R3 transient-agg DTO refactor) with Add (checker regex)
- Prereqs: Phase 1 完成（三态分类 + regex 裁决已落）

- [ ] `Fix`：R7 两处 `System.currentTimeMillis()` → `CoreMetrics.currentTimeMillis()`（`ErpFinGlMappingResolver:259` + `ErpMdExchangeRateApiClientFactory:69`），补 import。每处所属域 `mvn test -pl <module>/<service> -am` 验证（语义等价，既有测试覆盖）。
  - Skill: `nop-backend-dev`
- [ ] `Add`（若 Phase 1 选 a 或 c）：校准 `nop-compliance-checker.sh` R3 规则——(选 a) 加否定断言排除已知非实体后缀（Engine/Impl/Dto/Request/Result/Input/Response）；(选 c) checker 脚本加 `*.orm.xml` `<entity className>` 提取 + 已注册实体名白名单比对（R3 仅对白名单内实体类计数）。复跑 checker 确认 (A) false positive 子集不再计入 R3，且 (C) 真实瞬态实体构造仍被计数（规则未误伤）。
  - Skill: `nop-debugging`
- [ ] `Fix`（瞬态聚合 C 子集，逐处）：对 Phase 1 判定「应改 DTO 且低风险」的瞬态站点，将 `new Erp*()` 改为对应 DTO/support 类（若已存在）或保留实体形状并登记瞬态用途理由。**每处须确认不破坏公共契约**（返回类型 / 方法签名）；若某站点重构风险高（跨多调用方），降级为登记保留 + 移入 Deferred，不在本计划强改。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：抽样验证 R7 + 瞬态重构后行为不变——clock helper 返回同 epoch millis；瞬态 DTO 字段集与原实体形状等价。经所属域既有测试覆盖。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] R7 两处修复，受影响域 `mvn test` 全绿（R7 → 0）
- [ ] R3 测量口径校准落地（按 Phase 1 裁决选 a 后缀排除或选 c orm.xml 白名单），checker 复跑确认 false positive 排除 + 真实瞬态实体仍计数（非误伤）
- [ ] 瞬态聚合 C 子集逐处收敛（重构或登记保留理由）；高风险站点降级 Deferred（附触发条件）

### Phase 3 — 基线更新 + 反模式自检 + 文档对齐

Status: planned
Targets: `docs/audits/compliance-baseline.md`（R3 新基线 + R7=0 + regex 校准注记）、`docs/audits/nop-compliance-checker.sh`（若 regex 已改）、相关 owner doc（若瞬态重构触及业务语义）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [ ] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 复跑 `bash docs/audits/nop-compliance-checker.sh` 记录 R3 新基线（真实实体构造计数）+ R7=0；更新 `docs/audits/compliance-baseline.md` 基线表 + machine-readable 块 + R3/R7 收敛注记（含三态分类摘要 + regex 校准理由）。
  - Skill: none
- [ ] `Add`：若瞬态聚合重构触及业务语义，在相关 owner doc 补反模式自检（「勿用 domain entity 作内存计算容器，用 DTO」）；`compliance-baseline.md` 记录合法持久化 B 类基线 rationale（标准模式，非违规）。
  - Skill: none

Exit Criteria:

- [ ] 全仓 BUILD SUCCESS + checker R3（真实计数）/R7=0 基线记录（authoritative full-repo gate 见 Closure Gates）
- [ ] R3 三态分类摘要 + regex 校准理由记录；合法基线 rationale 记录

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06e34306cffewBD6Sk5GYWl1O5`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker / 2 Major / 3 Minor。R7=2/R3=19 基线 + checker 规则 + CI 门控 + 触发条件满足均经实时仓库逐项核实。**Major-1（R1）**：Current Baseline 三态预分类 materially 误判——`ErpMfgMrpDemand`（代码注释明示「内存构造不持久化」→ C 非 B）、`ErpSalOrderLine`（评估快照 → C）、`ErpCrmTerritoryPipeline`（不在 orm.xml → A 非 B）、`ErpHrGapAnalysis`（daoProvider==null fallback → C）；iter-1 实测真实分布 A≈14 / B≈0 / C≈5，已据实修订 Current Baseline 表（含代码注释/orm.xml 缺失证据）。**Major-2（R9）**：草案 option (a) 后缀排除法仅能排除 ~4/14 false positive（余 ~10 无匹配后缀），已新增 option (c) 交叉引用 `*.orm.xml` 实体声明精确校准。**Minor-1（R9/一致性）**：Goal #3 原预设「收紧」与 Phase 1 开放 Decision 矛盾，已改为「Phase 1 裁决校准方案」并列三候选。**Minor-2（R1/sourcing）**：R7 不在 0930-1 §Deferred 显式列表（经核实该列表为 R8/R10/R1d/R3），已补 Source 注记 R7 经 AGENTS.md:186 平台 helper 规则 + lesson 06 确认为 R13 不可降级实时缺陷纳入。**Minor-3（R7 item types）**：Phase 1 探索型混合（Proof/Decision），非阻塞。successor 触发条件经审查确认**合法满足**；regex 校准经专项审查确认**诚实测量校准**（19 处中 ~14 为非实体类，规则显著过匹配）非 gaming；不强求 R3→0 推理 sound。R1-R14 + anti-slack 修订后 PASS。
- Independent draft review iteration 2: `acceptable as-is` (`ses_06e27fd7cffeFubV0qi3mTM5lA`，独立 general 子代理，新会话冷重播，2026-07-24) — 0 Blocker / 0 Major / 2 Minor。iter-1 两项 Major 均确认 **genuine 解决**：Major-1 三态预分类经独立逐处读码复核 materially 准确（A=14/B=0/C=5，含 `ErpMfgMrpDemand` 代码注释「不持久化」/`ErpCrmTerritoryPipeline` 不在 orm.xml/`ErpQaActionImpl` 私有内部投影类/`ErpFinPostingMetricsSnapshot` support 类 + .MetricValue 内部类 全部实仓核实）；Major-2 option (c) orm.xml 交叉引用已加入 Goal #3 + Phase 1 Decision，option (a) 残留 ~10 false positive 风险注记数学准确。「诚实测量校准」+「不强求 R3→0」+ R7 R13 不可降级 + R4/R14 单结果面 bundling + anti-slack 全 sound。**Minor-1（R1/sourcing）**：lesson 06 实为代码生成产物覆盖主题（0 处提及 CoreMetrics），CoreMetrics 规则权威来源是 AGENTS.md:186——已删除 Source 中「+ lesson 06 确认」+ Related 中 lesson 06 CoreMetrics 误归因，改引 `common-java-helpers.md` 作 CoreMetrics 权威来源。**Minor-2（R7）**：Phase 2 option-(c) 执行步骤缺枚举（原仅「若选 a」）——已泛化 Phase 2 `Add` 项 + Exit Criteria 覆盖选 a 后缀排除 / 选 c orm.xml 白名单两路径。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及服务层 Java（R7 clock helper + 瞬态 DTO 重构）+ checker shell 脚本（R3 regex 校准），无 ORM/ext:dict/biz 公共契约/页面变更（瞬态 DTO 重构仅限低风险不破坏公共契约的站点）。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test` + checker 复跑（R3/R7 基线记录）。

- [ ] 范围内行为完成（R7 两处修复 + R3 三态分类 + regex 校准 + 瞬态聚合 C 子集收敛）
- [ ] 相关文档对齐（compliance-baseline + checker + 涉及 owner doc 反模式自检）
- [ ] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test` + checker 复跑（R3 真实计数 / R7=0，非回归）
- [ ] 无范围内项目降级为 deferred/follow-up（高风险瞬态重构降级须附明确触发条件；合法持久化 B 类是登记基线非范围缩减）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### R8（42 Processor 无 xbiz）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `2026-07-20-2200-1` §M-5 已审计全合法（xbiz 按实体命名 + Java @Inject），为既定合法基线。
- Successor Required: `no`（除非平台 xbiz 装配机制变更）

### R1d（23 findAllByQuery）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `2026-07-24-2000-1` 已评估 watch-only residual（可机械替换候选 <10）。
- Successor Required: `no`（触发条件：≥10 处可机械替换候选）

### R10（51 REQUIRES_NEW）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 多为 `ErpFinPostingExceptionRecorder` 审计日志合法用途；独立 successor。
- Successor Required: `yes`（触发条件：REQUIRES_NEW 审计发现真实事务隔离缺陷时）

### 高风险瞬态聚合重构站点（Phase 2 降级）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 跨多调用方 / 改变公共返回类型，本计划范围低风险收敛不覆盖。
- Successor Required: `yes`（触发条件：该站点所属业务模块重大重构时顺带，或 DTO 化需求明确）

## Closure

Status Note: <待执行 + 结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填充>

Follow-up:

- R10（触发条件见上）
- 高风险瞬态聚合重构站点（触发条件见上）
- R1d（触发条件见上）
