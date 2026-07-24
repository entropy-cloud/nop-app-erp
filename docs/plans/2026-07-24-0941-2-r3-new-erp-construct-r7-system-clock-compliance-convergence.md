# 2026-07-24-0941-2-r3-new-erp-construct-r7-system-clock-compliance-convergence R3 `new Erp*()` 构造 + R7 `System.currentTimeMillis()` 合规收敛

> Plan Status: completed
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

Status: completed
Targets: 全域 `src/main/java`（排除 `_gen`/`target`/test）、`docs/audits/nop-compliance-checker.sh:156-167`（R3 规则）、`docs/audits/compliance-baseline.md`
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Proof`
- Prereqs: 无（CI 门控已上线）

- [x] `Proof`：产出 R3 权威候选清单——多行 grep（`rg -n 'new Erp[A-Z]' module-*/erp-*-service/src/main/java module-*/erp-*-dao/src/main/java`，排除 `_gen`/`/test/`）枚举全 19 处，逐处附类全限定名。与 checker 实测计数（R3=19）对账。
  - Skill: `nop-backend-dev`
- [x] `Decision`：逐处三态分类——(A) **false positive**：确认非 ORM 实体（grep `@Entity`/查 `<domain>/model/*.orm.xml` 无对应 entity 声明；如 engine/service/DTO Impl 类）；(B) **合法持久化创建**：确认走 `dao().saveEntity()` 或等价持久化（grep 同方法块内 saveEntity/`daoFor(...).saveEntity`）；(C) **瞬态聚合/虚拟实体**：确认作内存计算容器不持久化（无 saveEntity 调用，仅 setter 聚合后读取字段）。每条记录 file:line + 类全限定名 + 类性质证据 + 判定 + 理由。
  - Skill: `nop-backend-dev`
- [x] `Decision`（R3 测量口径校准裁决）：裁决如何收紧 `nop-compliance-checker.sh` R3 规则以排除 (A) false positive。**裁决=选 (c) 交叉引用 `*.orm.xml` 实体声明**（精确校准，0 false positive / 0 false negative；未来新增实体自动纳入白名单，因 checker 运行时从 orm.xml 动态提取）。option (a) 后缀排除法否决：iter-1 审查 Major-2 实证仅能排除 ~4/14，余 ~10 处非实体类（`ErpCrmTerritoryPipeline`/`ErpCrmPipelineAccumulator`/`ErpFinPostingMetricsSnapshot` 及内部类）无匹配后缀仍被计数。option (b) 不降基线不满足「让规则测量其意图」目标。
  - Skill: `nop-debugging`
- [x] `Proof`（R7 确定性核实）：核实 2 处 R7 站点（`ErpFinGlMappingResolver:259` / `ErpMdExchangeRateApiClientFactory:69`）确为 `System.currentTimeMillis()` 直调，确认替换为 `CoreMetrics.currentTimeMillis()` 语义等价（同 epoch millis，平台 helper 委托 `System.nanoTime` 对齐的 monotonic clock，epoch millis 语义一致）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] R3 候选清单 19 处全量枚举 + checker 计数对账
- [x] 逐处三态分类完成（A false positive / B 合法持久化 / C 瞬态聚合），每条带类性质证据 + 判定
- [x] R3 regex 校准裁决记录（选 c orm.xml 白名单 + 理由 + 残留风险）
- [x] R7 两处确定性违规核实完成

#### Phase 1 Evidence：R3 权威候选清单（19 处）+ 三态分类

checker 实测 R3=19 与多行 grep 全量枚举 19 处对账一致。逐处三态分类（A=14 / B=0 / C=5）：

**A. false positive（Erp* 前缀非 ORM 实体）= 14 处**

| # | file:line | 类全限定名 | 类性质证据 | 判定 |
|---|-----------|-----------|-----------|------|
| A1 | `ErpApsAtpCtpServiceImpl.java:164` | `app.erp.aps.service.scheduling.ErpApsSchedulingEngine` | `public class ErpApsSchedulingEngine`（调度引擎 service 类，NOT in orm.xml） | A false positive |
| A2 | `ErpApsAtpCtpServiceImpl.java:81` | 同上 | 同上 | A false positive |
| A3 | `ErpApsSchedulingProcessor.java:235` | 同上 | 同上 | A false positive |
| A4 | `ErpCrmQuotaBizModel.java:90` | `app.erp.crm.dao.entity.ErpCrmTerritoryPipeline` | `@DataBean`（DTO/value 类，虽在 dao/entity 包但非 ORM 实体；NOT in orm.xml） | A false positive |
| A5 | `ErpCrmQuotaBizModel.java:94` | `ErpCrmTerritoryPipeline.QuotaSummary` | 内部 `@DataBean` | A false positive |
| A6 | `ErpCrmQuotaBizModel.java:100` | `ErpCrmTerritoryPipeline.ForecastSummary` | 内部 `@DataBean` | A false positive |
| A7 | `ErpCrmQuotaBizModel.java:108` | `ErpCrmTerritoryPipeline.ActualSummary` | 内部 `@DataBean` | A false positive |
| A8 | `QuotaRollupCalculator.java:164` | `app.erp.crm.service.support.ErpCrmPipelineAccumulator` | mutable 累加器 support 类，NOT in orm.xml | A false positive |
| A9 | `ErpFinPostingExceptionBizModel.java:173` | `app.erp.fin.dao.dto.ErpFinPostingMetricsSnapshot` | finance-dao 跨层契约 DTO（代码注释 `:172` 明示「非 ORM 实体」），NOT in orm.xml | A false positive |
| A10 | `ErpFinPostingExceptionBizModel.java:183` | `ErpFinPostingMetricsSnapshot.MetricValue` | 内部 value 类 | A false positive |
| A11 | `ErpFinPostingExceptionBizModel.java:190` | 同上 | 同上 | A false positive |
| A12 | `ErpFinPostingExceptionBizModel.java:197` | 同上 | 同上 | A false positive |
| A13 | `ErpFinPostingExceptionBizModel.java:203` | 同上 | 同上 | A false positive |
| A14 | `NcrLifecycleService.java:122` | `app.erp.qa.service.entity.NcrLifecycleService$ErpQaActionImpl` | `private static final class` 私有内部投影类（代码注释 `:121` 明示「非 ORM 实体，不适用 newEntity()」）；实体是 `ErpQaAction` 非 `ErpQaActionImpl`，NOT in orm.xml | A false positive |

**B. 合法持久化创建（`new Erp*()` + saveEntity）= 0 处**

iter-1 审查逐处核实：原草案列为 B 的代表站点实为 C 或 A。Phase 1 全量复核确认 B=0。Non-Goal「不重构合法持久化创建」自动满足（无对象）。

**C. 瞬态聚合 / 虚拟实体（ORM 实体作内存计算容器）= 5 处**

| # | file:line | 类全限定名 | 类性质证据 + 用途 | 判定 |
|---|-----------|-----------|------------------|------|
| C1 | `FunnelAggregationEngine.java:272` | `app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics` | ORM 实体（orm.xml:1483）；纯函数引擎快照（代码注释 `:269-271` 明示「无 daoProvider 注入，测试直接 new；产出快照由调用方 saveEntity 落库」）→ nop-backend-dev skill「ORM 实体构造反转模式」适用，**保留** | C 瞬态（保留） |
| C2 | `QuotaRollupCalculator.java:105` | `app.erp.crm.dao.entity.ErpCrmQuota` | ORM 实体（orm.xml:1046）；虚拟聚合行只读返回（代码注释 `:101` 明示「构造虚拟聚合行（不持久化）」），`rollup()` 返回类型=实体 → DTO 化改公共签名（Non-Goal），**保留** | C 瞬态（保留） |
| C3 | `GapAnalysisCalculator.java:90` | `app.erp.hr.dao.entity.ErpHrGapAnalysis` | ORM 实体（orm.xml:1692）；`daoProvider==null` fallback（`:86-88` 优先 `dao.newEntity()`），standalone/测试用途防御性 fallback → 已最优，**保留** | C 瞬态（保留） |
| C4 | `SimulationMrpEngine.java:273` | `app.erp.mfg.dao.entity.ErpMfgMrpDemand` | ORM 实体（orm.xml:856）；仿真内存构造（代码注释 `:272` 明示「内存构造 demand（不持久化）」），仿真结果返回 → DTO 化跨多调用方高风险，**保留** | C 瞬态（保留） |
| C5 | `ErpSalPricingRuleEngine.java:206` | `app.erp.sal.dao.entity.ErpSalOrderLine` | ORM 实体（orm.xml:387）；赠品行评估快照（代码注释 `:204-205` 明示「纯函数式引擎，调用方负责持久化，与 FunnelAggregationEngine 同模式」）→ skill 反转模式适用，**保留** | C 瞬态（保留） |

**Phase 2 收敛裁决**：C 子集 5 处全部**保留 + 登记理由**（无一改 DTO）。理由：(1) C1/C5 符合 nop-backend-dev skill「ORM 实体构造反转模式」例外——纯函数引擎由调用方 new 实例化、测试直接 new，改 newEntity() 需引入 IoC 注入破坏无状态纯净性与测试可构造性；(2) C2/C4 DTO 化改变公共方法返回类型（Non-Goal「不改 biz 方法签名」）；(3) C3 已优先 `newEntity()`，`new` 仅 daoProvider==null fallback，已最优。5 处代码注释均已文档化瞬态用途。降级 Deferred 无对象（均非高风险跨调用方公共契约破坏）。

**预期 R3 新基线**：19 → **5**（A=14 false positive 经 option (c) orm.xml 白名单校准排除；B=0；C=5 真实瞬态实体构造，健康合法基线 > 0，符合 Non-Goal「不强求 R3→0」）。

### Phase 2 — R7 修复 + R3 regex 校准 + 瞬态聚合子集收敛

Status: completed
Targets: Phase 1 R7 两站点、`docs/audits/nop-compliance-checker.sh`（R3 regex，选 c）、Phase 1 判定「应改 DTO」的瞬态聚合站点（C 子集，全部保留登记）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Item Types Note: Phase 2 is Fix-heavy (R7 clock helper + C-class retain rationale) with Add (checker regex 校准 option c)
- Prereqs: Phase 1 完成（三态分类 + regex 裁决已落）

- [x] `Fix`：R7 两处 `System.currentTimeMillis()` → `CoreMetrics.currentTimeMillis()`（`ErpFinGlMappingResolver:259` + `ErpMdExchangeRateApiClientFactory:69`），补 `import io.nop.api.core.time.CoreMetrics`。受影响域 `mvn test` 验证通过（fin 264 / md 109 全绿，含 `TestErpMdExchangeRateApiClient` 5）。
  - Skill: `nop-backend-dev`
- [x] `Add`（选 c）：校准 `nop-compliance-checker.sh` R3 规则——checker 脚本加 `*.orm.xml` `<entity className>` 提取 + 已注册实体名白名单比对（R3 仅对白名单内实体类计数）。复跑 checker 确认 (A) false positive 14 处子集不再计入 R3（19→5），且 (C) 真实瞬态实体构造 5 处仍被计数（规则未误伤）。checker 汇总 R3=5 / R7=0。
  - Skill: `nop-debugging`
- [x] `Fix`（瞬态聚合 C 子集 5 处，逐处）：Phase 1 判定全部保留实体形状 + 登记瞬态用途理由（无一改 DTO）。C1 `FunnelAggregationEngine:269-271` / C2 `QuotaRollupCalculator:101` / C4 `SimulationMrpEngine:272` / C5 `ErpSalPricingRuleEngine:204-205` 代码注释均已文档化瞬态用途；C3 `GapAnalysisCalculator:86-90` 已优先 `newEntity()`，`new` 仅 daoProvider==null fallback（自解释，已最优）。保留依据见 Phase 1 收敛裁决（skill「ORM 实体构造反转模式」例外 + Non-Goal「不改 biz 方法签名」）。无高风险站点降级 Deferred（均非跨调用方公共契约破坏）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：抽样验证 R7 + 瞬态保留后行为不变——clock helper `CoreMetrics.currentTimeMillis()` 返回同 epoch millis（fin/md 既有测试全绿覆盖）；瞬态实体形状未改（C 子集 5 处零代码改动，仅保留登记）。全仓 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] R7 两处修复，受影响域 `mvn test` 全绿（fin 264 / md 109，R7 → 0）
- [x] R3 测量口径校准落地（选 c orm.xml 白名单），checker 复跑确认 false positive 14 排除（19→5）+ 真实瞬态实体 5 处仍计数（非误伤）
- [x] 瞬态聚合 C 子集逐处收敛（5 处全部保留 + 登记理由；无高风险降级 Deferred）

### Phase 3 — 基线更新 + 反模式自检 + 文档对齐

Status: completed
Targets: `docs/audits/compliance-baseline.md`（R3 新基线 + R7=0 + regex 校准注记）、`docs/audits/nop-compliance-checker.sh`（regex 已改）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 复跑 `bash docs/audits/nop-compliance-checker.sh` 记录 R3 新基线=5（真实实体构造计数）+ R7=0；更新 `docs/audits/compliance-baseline.md` 基线表（R3 19→5 / R7 2→0）+ machine-readable 块 + R3/R7 收敛注记（含三态分类摘要 A=14/B=0/C=5 + option (c) regex 校准理由）。
  - Skill: none
- [x] `Add`：瞬态聚合重构未触及业务语义（C 子集 5 处零代码改动，全部保留登记）→ 无 owner doc 反模式自检需补；`compliance-baseline.md` 已记录合法持久化 B 类 baseline rationale（B=0，标准模式非违规，未来出现时独立计划登记）。
  - Skill: none

Exit Criteria:

- [x] 全仓 BUILD SUCCESS + checker R3（真实计数=5）/R7=0 基线记录（authoritative full-repo gate 见 Closure Gates）
- [x] R3 三态分类摘要 + regex 校准理由记录；合法基线 rationale 记录

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06e34306cffewBD6Sk5GYWl1O5`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker / 2 Major / 3 Minor。R7=2/R3=19 基线 + checker 规则 + CI 门控 + 触发条件满足均经实时仓库逐项核实。**Major-1（R1）**：Current Baseline 三态预分类 materially 误判——`ErpMfgMrpDemand`（代码注释明示「内存构造不持久化」→ C 非 B）、`ErpSalOrderLine`（评估快照 → C）、`ErpCrmTerritoryPipeline`（不在 orm.xml → A 非 B）、`ErpHrGapAnalysis`（daoProvider==null fallback → C）；iter-1 实测真实分布 A≈14 / B≈0 / C≈5，已据实修订 Current Baseline 表（含代码注释/orm.xml 缺失证据）。**Major-2（R9）**：草案 option (a) 后缀排除法仅能排除 ~4/14 false positive（余 ~10 无匹配后缀），已新增 option (c) 交叉引用 `*.orm.xml` 实体声明精确校准。**Minor-1（R9/一致性）**：Goal #3 原预设「收紧」与 Phase 1 开放 Decision 矛盾，已改为「Phase 1 裁决校准方案」并列三候选。**Minor-2（R1/sourcing）**：R7 不在 0930-1 §Deferred 显式列表（经核实该列表为 R8/R10/R1d/R3），已补 Source 注记 R7 经 AGENTS.md:186 平台 helper 规则 + lesson 06 确认为 R13 不可降级实时缺陷纳入。**Minor-3（R7 item types）**：Phase 1 探索型混合（Proof/Decision），非阻塞。successor 触发条件经审查确认**合法满足**；regex 校准经专项审查确认**诚实测量校准**（19 处中 ~14 为非实体类，规则显著过匹配）非 gaming；不强求 R3→0 推理 sound。R1-R14 + anti-slack 修订后 PASS。
- Independent draft review iteration 2: `acceptable as-is` (`ses_06e27fd7cffeFubV0qi3mTM5lA`，独立 general 子代理，新会话冷重播，2026-07-24) — 0 Blocker / 0 Major / 2 Minor。iter-1 两项 Major 均确认 **genuine 解决**：Major-1 三态预分类经独立逐处读码复核 materially 准确（A=14/B=0/C=5，含 `ErpMfgMrpDemand` 代码注释「不持久化」/`ErpCrmTerritoryPipeline` 不在 orm.xml/`ErpQaActionImpl` 私有内部投影类/`ErpFinPostingMetricsSnapshot` support 类 + .MetricValue 内部类 全部实仓核实）；Major-2 option (c) orm.xml 交叉引用已加入 Goal #3 + Phase 1 Decision，option (a) 残留 ~10 false positive 风险注记数学准确。「诚实测量校准」+「不强求 R3→0」+ R7 R13 不可降级 + R4/R14 单结果面 bundling + anti-slack 全 sound。**Minor-1（R1/sourcing）**：lesson 06 实为代码生成产物覆盖主题（0 处提及 CoreMetrics），CoreMetrics 规则权威来源是 AGENTS.md:186——已删除 Source 中「+ lesson 06 确认」+ Related 中 lesson 06 CoreMetrics 误归因，改引 `common-java-helpers.md` 作 CoreMetrics 权威来源。**Minor-2（R7）**：Phase 2 option-(c) 执行步骤缺枚举（原仅「若选 a」）——已泛化 Phase 2 `Add` 项 + Exit Criteria 覆盖选 a 后缀排除 / 选 c orm.xml 白名单两路径。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及服务层 Java（R7 clock helper + 瞬态 DTO 重构）+ checker shell 脚本（R3 regex 校准），无 ORM/ext:dict/biz 公共契约/页面变更（瞬态 DTO 重构仅限低风险不破坏公共契约的站点）。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test` + checker 复跑（R3/R7 基线记录）。

- [x] 范围内行为完成（R7 两处修复 + R3 三态分类 + regex 校准 + 瞬态聚合 C 子集收敛=全部保留登记）
- [x] 相关文档对齐（compliance-baseline 基线表 + machine-readable 块 + R3/R7 收敛注记 + checker R3 regex 校准；无 owner doc 反模式自检需补——C 子集零代码改动不触及业务语义）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 受影响域 `mvn test`（fin 264 / md 109 全绿）+ checker 复跑（R3 真实计数=5 / R7=0，非回归）
- [x] 无范围内项目降级为 deferred/follow-up（C 子集 5 处全部保留登记 + 代码注释理由，无降级；合法持久化 B 类=0 是空集登记非范围缩减；R8/R10/R1d 为范围外 watch-only residual 非 in-scope 降级）
- [x] 独立草案审查已完成并记录（iter-1 needs revision → iter-2 acceptable as-is，Draft Review Record 已填）
- [x] 文本一致性已验证：Plan Status / 三 Phase Status / Gates / 日志均一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中（见 Closure 执行证据；独立审计 verdict 待 auditor 填）

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

- Classification: `optimization candidate`（Phase 2 评估后无实际降级对象——C 子集 5 处全部保留登记，无一达「高风险」门槛）
- Why Not Blocking Closure: Phase 2 逐处评估后，C 子集 5 处（`ErpCrmFunnelStageMetrics`/`ErpCrmQuota`/`ErpHrGapAnalysis`/`ErpMfgMrpDemand`/`ErpSalOrderLine`）均符合 skill「ORM 实体构造反转模式」例外（纯函数引擎由调用方 new 实例化），改 DTO 破坏公共返回类型触 Non-Goal，全部保留登记。无站点达「跨多调用方 + 破坏公共契约」高风险门槛，故本 Deferred 无具体降级对象。
- Successor Required: `yes`（触发条件：某瞬态站点所属业务模块重大重构时顺带 DTO 化，或 DTO 化需求明确且不破坏公共契约）

## Closure

Status Note: 3 Phase 全部执行并验证通过（2026-07-24，EXECUTE 模式）。**R7（2→0）**：两处 `System.currentTimeMillis()` → `CoreMetrics.currentTimeMillis()`（fin `ErpFinGlMappingResolver:259` + md `ErpMdExchangeRateApiClientFactory:69` + import）。**R3（19→5）**：三态分类 A=14/B=0/C=5 定终值；regex 校准=option (c) orm.xml 实体白名单（checker 运行时动态提取，0 FP/0 FN）；C 子集 5 处全部保留登记（skill 反转模式例外 + Non-Goal 签名不变），无一改 DTO、无降级 Deferred。验证：全 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS + 受影响域 `mvn test` 全绿（fin 264 / md 109 含 `TestErpMdExchangeRateApiClient` 5）+ checker 复跑 R3=5 / R7=0（基线表 + machine-readable 块已更新）。`compliance-baseline.md` R3/R7 收敛注记 + 三态摘要 + B 类 baseline rationale 已记录。`backlog/README.md` +1 done 行；0930-1 §Follow-up R3+R7 RELEASED。R8/R10/R1d 维持范围外 watch-only residual。独立结束审计为剩余步骤（见 Closure Gates gate 7）。

Closure Audit Evidence:

- Executor / Agent: opencode（EXECUTE 模式，2026-07-24）
- 执行证据：(1) R7 两处修复——`ErpFinGlMappingResolver.java:7` import + `:259` CoreMetrics.currentTimeMillis()；`ErpMdExchangeRateApiClientFactory.java:8` import + `:69` CoreMetrics.currentTimeMillis()。(2) R3 regex 校准——`docs/audits/nop-compliance-checker.sh` R3 段加 ENTITY_WHITELIST 提取 + `grep -qxF` 白名单比对 + 尾随换行 strip。(3) checker 复跑 R3=5（C 子集 5 处真实瞬态实体）/ R7=0。(4) `mvn clean install -DskipTests` BUILD SUCCESS（154 模块，01:38 min）；`mvn test -pl module-finance/erp-fin-service` Tests run: 264, Failures: 0, Errors: 0；`mvn test -pl module-master-data/erp-md-service` Tests run: 109, Failures: 0, Errors: 0。(5) `docs/audits/compliance-baseline.md` 基线表（R3 19→5 / R7 2→0）+ machine-readable 块 + R3/R7 收敛注记。(6) `docs/backlog/README.md` +2026-07-24-0941-2 done 行；`0930-1` §Follow-up R3+R7 RELEASED。
- Auditor / Agent: 独立 closure auditor（opencode 新会话，非 EXECUTE 执行者，2026-07-24）— verdict: **approved**。逐项核实：(1) R7 两处修复实仓落地——`ErpFinGlMappingResolver.java:9` import + `:260` `CoreMetrics.currentTimeMillis()`、`ErpMdExchangeRateApiClientFactory.java:8` import + `:70` `CoreMetrics.currentTimeMillis()`，无残留 `System.currentTimeMillis()`；(2) R3 regex 校准实仓落地——`nop-compliance-checker.sh:169-180` `ENTITY_WHITELIST` 从源 `model/*.orm.xml` 动态提取 + `grep -qxF` 白名单比对；(3) checker 复跑 R3=5（精确命中 C 子集 5 处：`GapAnalysisCalculator:90`/`SimulationMrpEngine:273`/`FunnelAggregationEngine:272`/`QuotaRollupCalculator:105`/`ErpSalPricingRuleEngine:206`，A 14 处 false positive 已排除非误伤）/ R7=0；(4) 基线实仓 `compliance-baseline.md` R3=5/R7=0 + machine-readable 块 + R3/R7 收敛注记（三态摘要 + option (c) 校准理由）已记录；(5) 五点一致性 PASS（Plan Status completed / 3 Phase 全 completed / 全 Exit Criteria `[x]` / 全 Closure Gates `[x]` / Closure 证据实仓在）；(6) Anti-hollow PASS（CoreMetrics 调用 + checker 白名单逻辑均为运行时生效代码）；(7) Deferred honesty PASS（R8/R10/R1d 三项 watch-only residual 均带 successor 触发条件，C 子集 5 处为显式保留登记非降级 deferred，B 类=0 是空集登记非范围缩减，无 in-scope 缺陷隐藏）；(8) Docs sync PASS（`docs/logs/2026/07-24.md` 完整条目 + `docs/backlog/README.md:118` done 行 + `0930-1` §Follow-up R3+R7 RELEASED）。无 Blocker，可关闭。

Follow-up:

- R10（触发条件见上）
- 高风险瞬态聚合重构站点（Phase 2 评估后无实际降级对象——C 子集 5 处全部保留登记，触发条件同上）
- R1d（触发条件见上）
