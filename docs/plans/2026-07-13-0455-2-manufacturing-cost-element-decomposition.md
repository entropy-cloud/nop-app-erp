# 2026-07-13-0455-2-manufacturing-cost-element-decomposition 制造成本要素拆分（overhead 分配率 + subcontract 委外费归集）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md` 裁决 M-2（成本要素拆分 successor，与 M-1 合并为一个 backlog successor）；`docs/backlog/README.md` P8 行。**计划拆分协调**：1504-1 裁决将 M-1+M-2 作为一个 successor（一个 P8 行）跟踪并否决"独立 overhead successor"；本计划将其拆为两个**顺序计划**（N=1 委外引擎 / N=2 成本要素）的依据与残留风险见 Phase 1 Decision「计划拆分 vs 1504-1 合并裁决协调」。
> Related: `2026-07-13-0455-1-manufacturing-subcontracting-engine.md`（N=1，提供 subcontract 费归集源）；`2026-07-12-1504-1-competitive-comparison-correction.md`（裁决源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD=6d34e665 范围）：

- **CostRollupService 仅算材料+人工两要素**：`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`（271 行）——`overhead` 在购买件（:148）与制造件（:163）两处均硬编码 `BigDecimal.ZERO`，根因为工作中心仅有单一 `hourlyRate`（无人工/制造费率分列，工序工时成本统一计入 laborCost，见类 javadoc Decision :49-50「待工作中心费率拆分后细化」）；`subcontractCost` 在 `writeLines()`（:116）对每条 rollup 行无条件 `setSubcontractCost(BigDecimal.ZERO)`。
- **`CostBreakdown` 内部结构无 subcontract 字段**：`CostRollupService.java:264-270` 仅 `material/labor/overhead/unit`——无 subcontract 字段承载委外费，须补。
- **`ErpMfgCostRollupLine` schema 已完整**：`app-erp-manufacturing.orm.xml:1249-1289` 四要素列 materialCost(6)/laborCost(7)/**overheadCost(8)**/subcontractCost(9)/totalCost(10)/unitCost(11) 均 DECIMAL(20,4) 已存在——schema 零缺口，M-2 为纯 service 层变更。
- **overhead 根因 = 工作中心无费率拆分**：`state-machine.md:174` 明示 overhead=0；`ErpMfgWorkcenter` 仅有单一 `hourlyRate` 列（无 laborRate/overheadRate 分列）。
- **subcontract 缺归集源**：委外费当前无来源（N=1 计划落地后，已过账委外订单的 `processingFee`/`amount` 为 subcontract 要素归集源）。
- **既有常量/字典已就位**：`ErpMfgConstants.COST_ELEMENT_SUBCONTRACT="SUBCONTRACT"`（:185）；ORM 字典 `erp-mfg/cost-element`（:147-152）四值 MATERIAL/LABOR/OVERHEAD/SUBCONTRACT 已含 SUBCONTRACT。
- **标准成本传播链**：STANDARD 成本法经 `StandardCostResolver` 读最近 FIRMED `ErpMfgCostRollupLine.unitCost`（costing-methods.md:56）——overhead/subcontract 恒 0 会**传播进存货标准成本**，使 M-2 修复有跨模块价值。
- **ProductionVarianceCalculator 显式跳过 SUBCONTRACT**：其 javadoc（:63-64）「SUBCONTRACT 要素本期不算差异（5 类差异类型未含 SUBCONTRACT，委外差异需求落地时新增类型码）」——subcontract 差异归本计划 Non-Goal。

剩余差距：overheadCost/subcontractCost 两列恒 0；须补 overhead 分配率应用 + subcontract 费归集。

## Goals

- `CostRollupService` 真实计算并填充 `overheadCost`（制造费用分配率应用）与 `subcontractCost`（从已过账委外订单归集加工费）两列，使成本卷算四要素完整。
- `CostBreakdown` 结构补 subcontract 字段，totalCost = material+labor+overhead+subcontract 四要素之和。
- overhead 分配率经 config-gated 配置（默认关=向后兼容，关时 behavior 不变=0）应用，避免本期触碰工作中心 ORM schema（ask-first 保护区域）。
- subcontract 归集源来自 N=1（`2026-07-13-0455-1`）已过账委外订单，闭合"委外单→委外费→成本滚算 subcontract 列"链路。
- 修复向 STANDARD 标准成本的传播（FIRMED rollup 行 unitCost 含四要素）。

## Non-Goals

- **不拆 `ErpMfgWorkcenter` laborRate/overheadRate 列**（ORM ask-first 保护区域）——本期 overhead 用 config-gated 分配率（按工时或人工成本比例），工作中心 schema 拆分归 successor。
- 不做 subcontract 差异（ProductionVarianceCalculator 新增 SUBCONTRACT 差异类型）——successor。
- 不做委外引擎本身（归 N=1 计划）；本计划仅消费其加工费归集源。
- 不做成本滚算的其他增强（成本调整单已由 2352-3 完成；landed cost 由 1100-3 完成）。

## Task Route

- Type: `implementation-only change`（承接已审计 successor；纯 service 层 + config，无 ORM schema 变更）
- Owner Docs: `docs/design/finance/costing-methods.md`、`docs/design/manufacturing/state-machine.md`（:174 overhead successor 注）、`docs/design/manufacturing/bom-and-routing.md`（成本卷算）
- Skill Selection Basis: CostRollupService 引擎计算 + 配置门控 + 单测 → 匹配 `nop-backend-dev`（BizModel/service 方法、错误码、config）；测试 → `nop-testing`。
- Protected Areas: `accounting/finance postings`（成本滚算结果经 STANDARD 传播进存货成本）= **plan-first**。无 ORM ask-first 变更（overhead 用 config，subcontract 列已存在）。实施须本计划经独立草案审查后方可开始。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。
- 新增配置键（config-gated，默认关向后兼容）：`erp-mfg.overhead-allocation-enabled`（默认 false）+ `erp-mfg.overhead-allocation-mode`（MACHINE_HOUR/LABOR_RATIO）+ `erp-mfg.overhead-allocation-rate`（默认 0）；subcontract 归集 `erp-mfg.subcontract-cost-aggregation-enabled`（默认 false，依赖 N=1 完成方有意义）。
- 依赖：N=1 计划 `2026-07-13-0455-1` completed（提供已过账委外订单加工费归集源）。

## Execution Plan

### Phase 1 — overhead 制造费用分配率应用

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`；`ErpMfgConstants`/`ErpMfgConfigs` 配置键
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（overhead 独立于 N=1）

- [x] **Decision: 计划拆分 vs 1504-1「合并 successor」裁决的协调**——1504-1 将 M-1+M-2 作为**一个 successor**（一个 P8 backlog 行）跟踪，并**否决"独立 overhead successor"**（理由：割裂成本滚算完整性 + subcontract 仍需等 M-1）。本计划将合并 successor 拆为两个**顺序计划**（N=1/N=2），依据规则 14 拆分例外：(1) 实质不同的结果面与结束标准——N=1=委外生命周期+GL 过账+MRP 释放（manufacturing 业务流程面）；N=2=成本滚算要素+STANDARD 传播（costing 引擎面）；(2) 不同 owner docs——`subcontracting.md` vs `costing-methods.md`。**关键：本拆分未违反 1504-1 否决的"独立 overhead successor"**——N=2 保留 overhead+subcontract 两要素一体（不割裂成本滚算完整性），且 N=2 Phase 2 显式依赖 N=1（subcontract 归集源），满足"subcontract 仍需等 M-1"；两计划合起来仍是一个 successor 的完整交付（backlog P8 行不变）。替代方案（已否决）：(a) 合并回 `0455-1` 单计划——否决，因 N=2 Phase 2 阻塞至 N=1 完成，合并会使单计划前半与后半验证路径解耦度差、独立审计粒度变粗；(b) 仅拆 overhead 留 subcontract 于 N-1——即 1504-1 已否决的"独立 overhead successor"，割裂成本滚算完整性。残留风险：N=2 Phase 2 阻塞至 N=1 completed（Phase 1 overhead 独立可先行）；若审查偏好单计划贯通可合并回 0455-1。
  - Skill: `none`（计划结构裁决，非代码方法）
- [x] **Decision: overhead 分配率口径**——config-gated 全局/期间分配率（按机器工时或人工成本比例）vs 工作中心 schema 拆分（ask-first successor）。裁决：本期 config-gated 分配率（不改 ORM），记录选择/替代/残留风险（粗放 vs 精确）+ successor 触发条件（产品要求工作中心级精确费率时拆 schema）。
  - Skill: `none`（设计裁决）
- [x] **Add: overheadCost 计算分支**——替换 `CostRollupService.java:148/163` 两处硬编码 ZERO：config 关时 behavior 不变（=0，向后兼容）；config 开时按 allocation-mode（MACHINE_HOUR=工序工时×rate / LABOR_RATIO=laborCost×rate）计算 overheadCost 并填入 `CostBreakdown.overhead` + rollup 行。
  - Skill: `nop-backend-dev`
- [x] **Add: 配置键 + reader**——`ErpMfgConstants`/`ErpMfgConfigs` 增 overhead-allocation-enabled/mode/rate 三键（镜像既有 `erp-mfg.crp-overload-threshold` 范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] config 关时既有成本卷算测试零回归（overheadCost 仍 0）；config 开时 overheadCost 按选定 mode 非零且可单测断言（解除后续阶段依赖）。

### Phase 2 — subcontract 委外费归集

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`（`CostBreakdown` 结构 + `writeLines()` :116）；归集查询
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + **N=1 计划 completed**（已过账委外订单加工费为归集源）

- [x] **Add: `CostBreakdown` 补 subcontract 字段**（:264-270），totalCost = material+labor+overhead+subcontract 四要素和。
  - Skill: `nop-backend-dev`
- [x] **Add: subcontractCost 归集**——替换 `writeLines():116` 无条件 ZERO：config 关时=0（向后兼容）；config 开时按物料聚合已过账（N=1 产物）委外订单加工费（`processingFee`/`amount`）→ 按产量分摊填入 subcontractCost + `CostBreakdown.subcontract`。config-gated `erp-mfg.subcontract-cost-aggregation-enabled`。
  - Skill: `nop-backend-dev`
- [x] **Proof: STANDARD 传播验证**——确认 FIRMED rollup 行 unitCost 含四要素后经 `StandardCostResolver` 传播进存货 STANDARD 成本法（costing-methods.md:56 链路），无回归（inv-service STANDARD 成本法测试通过）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] config 关时既有成本卷算测试零回归；config 开 + N=1 已过账委外订单存在时 subcontractCost 非零且四要素 totalCost 正确；STANDARD 传播链无回归。

### Phase 3 — 测试 + owner-doc 对齐

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/...`；`docs/design/finance/costing-methods.md`；`docs/design/manufacturing/state-machine.md`（:174 successor 注解除标记）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2

- [x] **Add: 单元测试**——overhead 两 mode（关/开）+ subcontract（关/开 + 有/无已过账委外订单）+ totalCost 四要素和断言 + STANDARD 传播断言。
  - Skill: `nop-testing`
- [x] **Proof: mfg-service 全量测试无回归**——`mvn test -pl module-manufacturing/erp-mfg-service -am` 0 failures/0 errors。
  - Skill: `nop-testing`
- [x] **Add: owner-doc 对齐**——`costing-methods.md` 补 overhead/subcontract 要素计算说明 + config；`state-machine.md:174` overhead successor 注解除（标记已落地）。
  - Skill: `none`

Exit Criteria:

- [x] 成本要素测试全绿；owner-doc 实现偏离与 successor 注记录到位（含工作中心 schema 拆分 successor 触发条件）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a7dfb2caffelaYvXr58amr4F5) — 全部 load-bearing 事实主张经实时仓库核实为真（CostRollupService overhead ZERO :148/:163、subcontractCost ZERO :116、CostBreakdown 无 subcontract 字段 :264-270、ErpMfgCostRollupLine schema 已完整 :1260-1261、"无 ORM ask-first 变更"主张准确、N=1 依赖正确建模、Deferred 分类与触发条件合规）。**1 BLOCKER (B1)**：计划拆分与 1504-1「合并 M-1+M-2 successor」裁决未协调（Source 自引"与 M-1 合并"却独立成文，规则 9 内部不一致）。**已修订**：Source 行增拆分协调注 + Phase 1 增 `Decision: 计划拆分 vs 1504-1 合并裁决协调`（引规则 14 拆分例外 + 未违反否决的"独立 overhead successor"+ 替代方案/残留风险）；另修 Mj1（Phase 2「STANDARD 传播验证」`Add`→`Proof` + Item Types 补 Proof）。2 非阻塞 Minor（Mn1 Phase 3 Proof 与 Closure Gates 验证重叠可接受 / Mn2 配置键声明文件未点名）保留。修订后草案审查收敛 → 待 iteration 2 复核 B1。
- Independent draft review iteration 2: `accept` (ses_0a7da052bffeEKLQ82rcVqLkqz) — B1 全部四个子点满足（承认 1504-1 合并裁决+否决的"独立 overhead successor"、引规则 14 拆分例外含实质不同的结果面/owner docs/验证、论证本拆分未违反否决替代方案因 overhead+subcontract 仍一体且 N=2 依赖 N=1、记录两否决替代+残留风险）、Source 行内部一致、Mj1 已修（`Proof` 标签 + Item Types 补 Proof）、无新 Blocker/反 Slack 违规、残留风险三处诚实记录。计划 execution-ready。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（overheadCost + subcontractCost 两列真实计算 + CostBreakdown 四要素 + config 门控）
- [x] 相关文档对齐（`costing-methods.md`/`state-machine.md` 要素说明 + successor 注）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn test -pl module-inventory/erp-inv-service -am`（STANDARD 传播回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 工作中心 laborRate/overheadRate schema 拆分（精确工作中心级费率）

- Classification: `successor`
- Why Not Blocking Closure: 本期 config-gated 全局/期间分配率交付 overhead 要素（粗放但可用），工作中心 schema 拆分为 ORM ask-first 保护区域，精确费率为增强面。
- Successor Required: `yes`（触发条件：产品要求工作中心级精确人工/制造费率分列时——须 ask-first ORM 批准）

### subcontract 委外差异（ProductionVarianceCalculator SUBCONTRACT 差异类型）✅ 已收口

- Classification: `successor`
- Why Not Blocking Closure: ProductionVarianceCalculator 当前 5 类差异未含 SUBCONTRACT（javadoc :63-64 明示），属差异分析增强面。
- Successor Required: `yes`（触发条件：委外差异分析业务需求落地时——新增 erp-mfg/variance-type 字典码）
- **已交付**：plan `2026-07-14-0035-1-manufacturing-subcontract-variance-type.md` 落地第 6 类差异 SUBCONTRACT（标准 = rollupLine.subcontractCost × 完工量；实际 = wo.subcontractCost）+ 字典码 + 常量 + Dispatcher 第 4 要素桶 + 1416/1417 科目对 + 测试。

## Closure

Status Note: completed — 全 3 Phase 执行完毕并经独立结束审计（新会话、不重用执行者上下文）复核通过。范围行为、owner-doc 对齐、config 门控、测试覆盖、Deferred 诚实度均已对实时仓库核实。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文）
- 审计范围与方法：完整重读计划全文 + 对照实时仓库逐项核验（grep/glob/read + 真实验证命令执行）
- 实现落地核验：`CostRollupService.java:205-226` `computeOverhead`（MACHINE_HOUR/LABOR_RATIO 两模式 + config 关时恒 0 向后兼容）真实存在且被 `computeUnit` 调用；`:257-280` `aggregateSubcontractCost`（COMPLETED 委外单按产量分摊）真实存在且被采购件/制造件分支调用；`:365-372` `CostBreakdown` 含 subcontract 字段；`writeLines():122` + `toResult():342` 双向写入 subcontractCost；`unit = material+labor+overhead+subcontract` 四要素和（`:156/:173`）。非 hollow：方法体非空、被运行时调用链触达。
- 配置键核验：`ErpMfgConstants:187-208` 增 4 键（`CONFIG_OVERHEAD_ALLOCATION_ENABLED/MODE/RATE` + `OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR/LABOR_RATIO` + `DEFAULT_OVERHEAD_ALLOCATION_RATE` + `CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED`），均经 `AppConfig.var` 默认关向后兼容。
- 测试核验：`TestErpMfgCostRollup.java:144-292` 增 5 测试（overhead MACHINE_HOUR / overhead LABOR_RATIO / subcontract 开 / subcontract 关向后兼容 / 四要素端到端集成），含 `assertFourElementSum` 四要素和不变式断言。
- 真实验证命令结果：`mvn test -pl module-manufacturing/erp-mfg-service -am` → Tests run: 111, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS；`mvn test -pl module-inventory/erp-inv-service -am`（STANDARD 传播回归）→ Tests run: 97, Failures: 0, Errors: 0, BUILD SUCCESS。
- owner-doc 对齐核验：`docs/design/finance/costing-methods.md:73-75` 补 overhead/subcontract 要素计算说明 + config；`docs/design/manufacturing/state-machine.md:174` overhead successor 注已更新为「已落地 + successor 触发条件（工作中心级精确费率 ask-first）」；`bom-and-routing.md:142/146` 一致。
- 日志核验：`docs/logs/2026/07-13.md` 已记录本计划全 3 Phase 执行（按 AGENTS.md 规则 8）。
- 五点一致性：Plan Status=completed / 3 Phase Status 均 completed / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / Closure evidence 实体化 — 全部一致。
- Deferred 诚实度：工作中心 schema 拆分 + subcontract 差异两类 successor 分类为 `successor`（非 `watch-only`/`Follow-up`），均命名触发条件，无范围内缺陷降级。
- 非阻塞 Follow-up：工作中心 schema 拆分（精确费率 successor，ask-first）/ subcontract 差异类型（见 Deferred But Adjudicated 触发条件）— 均非范围内 live defect。

Follow-up:

- 工作中心 schema 拆分（精确费率 successor，ask-first）/ subcontract 差异类型（见 Deferred But Adjudicated 触发条件）。
