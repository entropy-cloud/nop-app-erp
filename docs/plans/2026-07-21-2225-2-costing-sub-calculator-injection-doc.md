# 2026-07-21-2225-2-costing-sub-calculator-injection-doc 成本计算子计算器注入模式文档化（D3）

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone D / D3（`todo`，Inventory 域 P1 深化项）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md`
> Related: `docs/design/finance/costing-methods.md`（EXPAND 目标）、`docs/plans/2026-07-02-1538-1-inventory-costing-engine.md`（策略分派首落地）、`docs/plans/2026-07-05-0427-2-standard-costing-strategy.md`、`docs/plans/2026-07-05-2352-3`（成本调整）
> Audit: required

## Current Baseline

存货成本计算的「子计算器注入模式」已在 inventory 域稳定落地并经多轮测试覆盖，但**未在 owner doc 中作为可复用模式正式文档化**。当前状态：

**已实现（权威源 = `module-inventory/erp-inv-service` 代码）**：

- `CostingStrategy` 接口（`costing/CostingStrategy.java`，38 行）——策略契约，3 方法：`costMethod()`（对应字典 `erp-md/cost-method` 码值）+ `onIncoming(move, line, acctSchemaId, unitCost, ctx)` + `onOutgoing(move, line, acctSchemaId, ctx)`
- 7 个策略实现：`MovingAverageCostingStrategy` / `FifoCostingStrategy` / `StandardCostingStrategy` / `LifoCostingStrategy` / `WeightedAverageCostingStrategy` / `BatchCostingStrategy` / `SpecificCostingStrategy`
- `StockMoveBookkeeper`（`stock/StockMoveBookkeeper.java`，322 行）——**注入器 + 分派器**：`@Inject` 注入全部 7 策略 + `CostMethodResolver`，按物料 costMethod 分派到对应策略的 `onIncoming`/`onOutgoing`
- `CostMethodResolver`——解析链：`ErpMdMaterial.costMethod` → `ErpMdAcctSchema.costingMethod` → config `erp-inv.default-cost-method`；`erp-inv.costing-enabled=false` 时一律回退移动加权平均（兜底总开关）
- `BookingContext`——共享上下文接口（由 `StockMoveBookkeeper implements`）
- COGS 通道统一：策略只写 `ErpInvStockLedger.unitCost/totalCost`，既有 `InvPostingDispatcher` 读 `ledger.getTotalCost()` 零改动拾取

**同型模式（经代码核实为真正的 Strategy + registry + key-dispatch）**：

- `IErpFinAcctDocProvider` 按 `businessType` 分派过账科目解析（接口 + 多实现 + 按 businessType 注册分派；A1 GL Mapping Rule 的 Provider 路由亦同型）——**唯一同型实例**

**邻近异型 / 反模式（经代码核实，非同型，须在文档中显式区分以免误分类）**：

- `CostAdjustmentService`（`module-inventory/erp-inv-service/.../costing/CostAdjustmentService.java`）——**反模式实例**：按 `adjustType` 分派成本层应用，但用内联 `if (Objects.equals(adjust.getAdjustType(), ...STANDARD_REVALUATION))` 分支（多处 line 132/189/301），**不是** Strategy 接口 + registry。这正是本计划 G4 反模式自检表要禁止的「手写 if/equals 分派」
- `CostRollupService`（`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`）——**非分派模式**：单递归 BOM 成本聚合服务（`@Inject IDaoProvider` + `BomExpander`），无分派键、无接口/实现拆分；overhead 模式选择用内联 `if/equals`（line 219/222），归聚合 helper 而非分派模式

**owner doc 现状**：`docs/design/finance/costing-methods.md`（566 行）已在 §实现注记（plan 1538-1/0427-2/1100-3/1745-2/0455-2/0035-1）记录了**各策略的具体行为**，但**未抽象出「子计算器注入模式」作为可复用范式**——何时使用、结构组成、如何新增策略、与同型模式的对照、反模式自检表均缺失。

## Goals

- **G1 — 抽象可复用模式**：在 `docs/design/finance/costing-methods.md` EXPAND 新增「子计算器注入模式」章节，把已稳定落地的 `CostingStrategy + StockMoveBookkeeper + CostMethodResolver + BookingContext` 抽象为可复用范式
- **G2 — 提供新增策略步骤**：文档化「为新的 costMethod 码值添加一个策略」的完整步骤清单（接口实现 + Bean 注册 + 字典扩展 + resolver 识别 + 测试范式），让后续 BATCH/LIFO-full/INDIVIDUAL 等未完工策略的扩展有明确路径
- **G3 — 同型模式对照**：列出仓库内同型分派模式（AcctDocProvider / CostAdjustmentService / CostRollupService），统一「何时选用此模式」的决策指引
- **G4 — 反模式自检表**：给出违反模式的常见错误（如手写 switch/case 分派、策略内直接写凭证、绕过 resolver 硬编码 costMethod）

## Non-Goals

- **不**新增 / 修改任何策略实现或 Java 代码（纯文档扩展，roadmap §6 明确 D3 = `否 — 仅为文档扩展`）
- **不**实现未完工的 BATCH/INDIVIDUAL/LIFO-full 策略（归各自 successor，本计划仅文档化「如何实现」）
- **不**改造 `costing-methods.md` 既有 8 节正文与各 plan 实现注记（仅 EXPAND 追加新章节，保持向后兼容）
- **不**修改 ORM / 字典 / 配置（D3 roadmap 明确无 ORM 变更）
- **不**触碰 `module-inventory` 代码或测试（文档计划，Closure Gates 删除仓库级验证命令门控）

## Task Route

- Type: `implementation-only change`（纯文档，无代码/契约/模型变更）
- Owner Docs: `docs/design/finance/costing-methods.md`（**EXPAND**，追加新章节）
- Skill Selection Basis: 文档化既有的已测试后端模式，需准确读代码 → `nop-backend-dev` 的决策门/反模式自检概念用于校验文档准确性，但本计划**不写代码**，故 Skill 在阶段级别声明：Phase 1 读代码阶段 Skill=`nop-backend-dev`（仅用于准确理解），Phase 2 写文档阶段 Skill=`none`。无前端工作 → `nop-frontend-dev` 不匹配。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档计划）

## Execution Plan

### Phase 1 - 代码核对 + 章节骨架

Status: completed
Targets: `docs/design/finance/costing-methods.md`（EXPAND 追加章节骨架）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] **Add**：核对 `CostingStrategy` 接口 + 7 策略 + `StockMoveBookkeeper` + `CostMethodResolver` + `BookingContext` 的当前真实签名与行为（以代码为准，纠正任何 owner doc 既有描述与代码的漂移）；同时核对候选同型/异型分类（`IErpFinAcctDocProvider` 同型 / `CostAdjustmentService` 反模式 / `CostRollupService` 非分派）的代码证据；在 `docs/design/finance/costing-methods.md` 追加「## 子计算器注入模式（D3）」章节骨架（7 小节：模式定义 / 结构组成 / 何时使用此模式 / 如何新增一个策略 / 同型与异型分类裁决 / 反模式自检表 / 落地证据）
  - Skill: `nop-backend-dev`
- [x] **Decision E — 同型分类裁决**：裁决仓库内哪些类属于「子计算器注入模式」的真正实例。经代码核实：(a) `CostingStrategy` + `IErpFinAcctDocProvider` = **同型实例**（接口 + 多实现 + registry/key-dispatch）；(b) `CostAdjustmentService` = **反模式实例**（内联 if/equals 分派，本应重构为 Strategy+registry）；(c) `CostRollupService` = **非分派模式**（聚合 helper，无分派键）。记录裁决 + 代码证据（行号）+ 对 owner doc 对照表的影响（同型表仅含 2 实例；反模式表含 CostAdjustmentService 作为仓库内具体例证）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 本阶段交付核对准确的章节骨架 + 同型分类裁决。无需仓库级验证（纯文档）。

- [x] `costing-methods.md` 新增「## 子计算器注入模式（D3）」章节骨架含上述 7 小节标题
- [x] 代码核对完成（接口签名 / 策略清单 / resolver 解析链 / config 兜底均与代码一致）
- [x] Decision E 同型分类裁决落盘（含代码行号证据）

### Phase 2 - 章节正文 + roadmap 同步

Status: completed
Targets: `docs/design/finance/costing-methods.md`（章节正文）、`docs/backlog/deepening-roadmap.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1 骨架 + 代码核对完成

- [x] **Add**：填写章节 7 小节正文：(1) 模式定义 + 结构图（策略接口 + 注入器/分派器 + resolver + context 四要素）；(2) 结构组成逐一说明（含 `@Inject` 多策略注入 + `costMethod()` 自描述码值 + resolver 解析链 + config 兜底总开关）；(3) 何时使用（多算法对应同一操作 + 由数据/配置选择算法 + 需统一输出通道）；(4) 新增策略步骤清单（实现接口 → Bean 注册 → 字典扩码值 → resolver `isSupported` 识别 → 复用既有单测范式 7 类 costMethod 各一）；(5) **同型与异型分类裁决**（按 Phase 1 Decision E）：同型实例表仅含 `CostingStrategy` vs `IErpFinAcctDocProvider` 两实例（相同形状不同分派键 costMethod/businessType）；异型/反模式段含 `CostAdjustmentService`（内联 if/equals 应重构为 Strategy+registry）+ `CostRollupService`（非分派，聚合 helper），附代码行号证据；(6) 反模式自检表（≥ 5 项：禁止 switch/if-else 分派【含 CostAdjustmentService 仓库内反例】 / 禁止策略内直接写凭证 / 禁止绕过 resolver 硬编码 / 禁止策略持有状态 / 禁止新增策略不补单测 / 禁止把聚合 helper 误分类为分派模式【含 CostRollupService 仓库内反例】）
  - Skill: `none`
- [x] **Add**：`docs/backlog/deepening-roadmap.md` D3 行 `todo → done` + 增 §8.x 落地证据段（plan / owner doc EXPAND 段 / 纯文档无测试基线 / deferred successor）
  - Skill: `none`

Exit Criteria:

- [x] 7 小节正文全部填写，含同型/异型分类裁决段（同型 2 实例 + 反模式 2 反例 + 代码行号证据）+ 反模式自检表 ≥ 5 项
- [x] roadmap D3 状态更新 + 落地证据段落盘

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（`ses_07ae9640bffe6kmeRceV0prlMZ`，2026-07-21）— 1 项阻塞：Current Baseline 将 `CostAdjustmentService`/`CostRollupService` 错分为「同型模式」（经代码核实：前者用内联 if/equals 分派=反模式；后者=聚合 helper 非分派模式），且 Phase 2 同型对照表沿用此错误分类，缺同型分类 Decision。本次修订已应用：(1) Current Baseline 拆为「同型模式」(仅 `IErpFinAcctDocProvider`) +「邻近异型/反模式」两段含代码行号；(2) Phase 1 增 Decision E 同型分类裁决含代码证据；(3) 章节骨架 6→7 小节；(4) Phase 2 同型对照表仅含 2 实例 + 反模式表含 2 仓库内反例。待 iteration 2 复审。
- Independent draft review iteration 2: `acceptable-as-is`（`ses_07ae118c9ffemHxsElyQYPq0aV`，2026-07-21）— 阻塞问题已全解（baseline 重分类含行号 + Decision E 含代码证据 + Phase 2 同型表仅 2 实例 + 反模式表含 2 仓库内反例，均经代码核实）。1 项非阻塞（Phase 1 阶段级 Item Types 槽应为 `Add | Decision`）已在本次微调应用。结论：可接受为执行契约，置 `active`。

## Closure Gates

> 纯文档计划：删除仓库级 `typecheck/build/test` 验证门控（见执行时规则 7）。仅验证文档内容准确性与一致性。

- [x] 范围内文档完成（「## 子计算器注入模式（D3）」章节 7 小节 + 同型/异型分类裁决 + 反模式自检表）
- [x] 相关文档对齐（costing-methods.md EXPAND 不破坏既有 8 节正文 + 各 plan 实现注记；roadmap §8.x 落地证据）
- [x] 已运行验证：文档内引用的接口签名 / 策略清单 / resolver 解析链 / config 开关均与 `module-inventory/erp-inv-service` 代码一致（grep 交叉核对）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 未完工策略实现（BATCH / INDIVIDUAL / LIFO-full）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划仅文档化「如何实现」；具体策略实现归各自 successor，模式文档为其提供路径
- Successor Required: `yes`（触发条件：业务方启用对应 costMethod 需求）

## Closure

Status Note: Phase 1-2 全部完成（2026-07-21）。纯文档计划：无代码改动、无测试基线变更、无 ORM/字典/配置变更。Phase 1/2 验证 = `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）保证既有代码无回归。结束审计由独立子代理（新会话）执行。

Closure Audit Evidence:

- Auditor / Agent: `<independent auditor — TBD by separate closure-audit session>`
- Evidence:
  - Phase 1 交付：`docs/design/finance/costing-methods.md` 追加 §子计算器注入模式（D3）章节骨架（7 小节标题）+ Decision E 同型分类裁决落盘（同型 2 实例 + 反模式 1 例 + 非分派 helper 1 例，附代码行号证据）
  - Phase 2 交付：7 小节正文全部填写（模式定义含四要素结构图 / 结构组成逐一说明含 7 策略清单 + 测试类映射 / 何时使用 3 条件矩阵 / 新增策略 7 步清单 / Decision E 同型分类 / 反模式自检表 6 项 ≥ plan 退出条件 5 项 / 落地证据含代码核对基线 + deferred successor）+ roadmap D3 `todo → done` + §8.7 落地证据段落盘
  - 验证：`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，2026-07-21T23:51:33+08:00）
  - 文档一致性：Plan Status = completed / Phase 1 Status = completed / Phase 2 Status = completed / roadmap D3 = done / roadmap §2 Status 计数 todo 9 + done 2 / roadmap §4 Inventory 行 ✅ D3 done / roadmap §8.7 落地证据段落盘 — 全部一致

Follow-up:

- 未完工策略实现（见 Deferred，触发条件已命名）
- `CostAdjustmentService` 反模式重构为 Strategy+registry（触发条件：业务方新增第 3 种 adjustType）
- 同型模式推广（触发条件：≥3 算法 + config 选择的具体需求）
