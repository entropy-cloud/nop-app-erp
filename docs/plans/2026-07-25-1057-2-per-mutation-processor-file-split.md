# 2026-07-25-1057-2-per-mutation-processor-file-split per-mutation Processor 文件拆分

> Plan Status: active
> Last Reviewed: 2026-07-25
> Source: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` §Deferred But Adjudicated「Phase 2 per-mutation Processor 文件拆分（42 → ~250 文件）」（Successor Required: yes；`docs/analysis/per-mutation-processor-split-plan.md` 已记录拆分方向）
> Related: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md`（Phase 1 创建 7 抽象基类 + ORM use-approval 移除 + xbiz 桥接）、`docs/analysis/per-mutation-processor-split-plan.md`（拆分方向分析）、`docs/audits/compliance-baseline.md`（R8=42 基线，per-mutation 拆分将改变 R8 形态）
> Audit: required

## Current Baseline

`2026-07-24-2200-1` Phase 1 创建了 7 个抽象基类（`AbstractProcessor<T>` 根 + `AbstractApproveProcessor<T>` / `AbstractRejectProcessor<T>` / `AbstractSubmitForApprovalProcessor<T>` / `AbstractReverseApproveProcessor<T>` / `AbstractWithdrawApprovalProcessor<T>` / `AbstractCancelProcessor<T>`），位于 `module-common-service/`。

**关键差距：0 个领域 Processor 当前继承这些抽象基类。** 抽象基类经 `rg -l 'extends Abstract.*Processor' module-*/erp-*-service/src/main/java` 实测命中 0 文件——基类已创建但无消费者，处于 dead code 状态。本计划是这些基类的首次实际接入与验证。

**42 个领域 Processor（monolithic 文件）** 经 `rg -l 'class Erp.*Processor' module-*/erp-*-service/src/main/java`（排除 Abstract）实测分布于 10 域：

| 域 | Processor 数 | Processor 清单 |
|----|-------------|---------------|
| purchase | 6 | Order / Receive / Return / Invoice / Requisition / Payment |
| sales | 6 | Order / Quotation / Delivery / Receipt / Return / Invoice |
| finance | 8 | EmployeeAdvance / BadDebt / ExpenseClaim / NotesPayable / NotesReceivable / AccountingPeriod / Posting / BudgetScenario |
| assets | 9 | Cip / Split / AssetCapitalization / Inventory / Maintenance / Disposal / DepreciationSchedule / ValueAdjustment / Merge |
| manufacturing | 4 | WorkOrder / ScheduleToJobCard / JobCard / SubcontractOrder |
| inventory | 4 | CostAdjust / LandedCost / OwnershipTransfer / StockMove |
| crm | 2 | Conversion / Lead |
| quality | 1 | Recall |
| aps | 1 | Scheduling |
| projects | 1 | ProjectSettlement |

**每个 Processor 典型 mutation 矩阵**（以 `ErpPurOrderProcessor` 为例，实测 6 个 public 方法）：`submitForApproval` / `withdrawApproval` / `approve` / `reject` / `reverseApprove` / `cancel`。部分 Processor 有额外域特定 mutation（如 `ErpPurReceiveProcessor.confirm` / `ErpMfgWorkOrderProcessor.checkAvailability+start+reportCompletion` / `ErpInvLandedCostProcessor.allocate+approve` / `ErpFinAccountingPeriodProcessor.closePeriod+openPeriod`）。

**当前委托链**（2200-1 Phase 1 后）：BizModel `@Inject ErpXxxProcessor` → Processor.approve(...) 单体方法 → xbiz `<source>` 块内联 approval-support.xbiz 行为（Phase 1 桥接，行为等价但 delegation 仍在 xbiz 层非 BizModel `@BizMutation`）。

**拆分目标**（2200-1 §Deferred + analysis doc）：每个 Processor 拆为 N 个 per-mutation 文件，每个继承对应抽象基类，仅 override 该 mutation 的 template hooks（validate / doXxx / afterStateChange）。估算 42 Processor × ~6 mutation = **~250 个 per-mutation 文件**。拆分后委托链变为：BizModel `@BizMutation approve(...)` → `@Inject ErpXxxApproveProcessor` → `approveProcessor.approve(id, context)`（一行委托）。

**R8 关联**：R8 checker 当前实测 49（42 领域 Processor + 7 抽象基类 false positive，由 plan N=1 `2026-07-25-1057-1` 校准排除后回落至 42）。per-mutation 拆分后，42 个 monolithic Processor 拆为 ~250 个 per-mutation 文件（每个仍以 `*Processor.java` 命名），R8 形态将发生结构性变化——须在拆分后（Phase 4）一并裁决 R8 新基线 + checker 口径（per-mutation 文件按 xbiz 路由计入与否）。

剩余差距：抽象基类零消费者（dead code）；42 monolithic Processor 未拆分；BizModel @BizMutation 接管未落地（xbiz `<source>` 桥接仍承担 delegation）。

## Goals

1. **激活抽象基类**：将 42 个 monolithic Processor 拆分为 ~250 个 per-mutation 文件，每个继承对应的 `Abstract*Processor<T>`，使 2200-1 Phase 1 的 7 个抽象基类从 dead code 转为生产消费。**域不被阻塞的前提下完成全域 42 Processor**；若某域在执行期发现阻塞（如抽象基类 hook 与域特定 side-effect 模式根本不兼容），该域按 `Deferred But Adjudicated` 处理并记录触发条件，不影响其他域收口。
2. **BizModel @BizMutation 接管**：每个有自定义 Processor 的实体，BizModel 新增 `@BizMutation` 方法一行委托到 per-mutation Processor，移除 xbiz `<source>` 委托块（保留内联默认 `<source>` 用于无 Processor 的实体）。
3. **行为不变**：拆分前后审批/状态机/过账/副作用行为完全一致（经既有单测 + xbiz 快照匹配验证）。
4. **R8 基线结构性裁决**：拆分后 R8 形态变化（42 monolithic → ~250 per-mutation），在 `compliance-baseline.md` 记录新 R8 基线 + 裁决注记（per-mutation 文件按 xbiz 路由还是 @Inject 路由）。

## Non-Goals

- **不改 Processor 业务逻辑**——拆分仅改变代码组织（monolithic → per-mutation），不改变 validate/doXxx/afterStateChange 的行为语义。若拆分中发现行为 bug，记入 bug 文档归 successor，不在本计划修复。
- **不改 ORM / 契约 / 字典 / view.xml / page.yaml**——纯 service 层 Java + xbiz 文件重组。
- **不拆分非审批类 Processor 方法**——域特定 mutation（如 `confirm` / `allocate` / `checkAvailability` / `reportCompletion` / `closePeriod`）保留在域特定 Processor 中不强制 per-mutation 拆分（它们无对应抽象基类）。仅 6 个标准审批 mutation（submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel）强制 per-mutation 拆分。
- **不重构 daoFor / 不降 R2c**——daoFor 收敛是独立工作流（governed-path + Type 1 重构），本计划不触及 daoFor 站点。
- **不改测试断言**——既有单测 + xbiz 快照是行为等价性的证明，不修改断言值；仅当快照因文件路径变化失配时重录。

## Task Route

- Type: `architecture change`（service 层代码组织重构，结果面 = per-mutation Processor 文件架构 + 抽象基类激活）
- Owner Docs: `docs/analysis/per-mutation-processor-split-plan.md`（拆分方向分析，将 EXPAND 为完整清单）、`docs/architecture/service-layer-orchestration.md`（Processor 接线约定）、`docs/audits/compliance-baseline.md`（R8 基线结构性裁决）
- Skill Selection Basis: `nop-backend-dev`（匹配「BizModel / IBiz / xbiz action / Processor / protected step 方法 / 跨实体调用 / 产品化可定制性自检」工作方法；2200-1 同型 Processor 架构工作经该技能路由）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 service 层 Java 重构 + xbiz 文件重组，无端口/密钥/外部服务/数据迁移）。
- **软依赖**：Plan N=1（`2026-07-25-1057-1-compliance-baseline-drift-adjudication`）校准 R8 checker 排除 `module-common-service` 抽象基类。若 N=1 未先落地，本计划 Phase 4 R8 基线裁决会在 checker 仍含 7 处 false positive 的基础上进行——非阻塞（per-mutation 拆分改变 R8 形态不受 false positive 影响），但建议 N=1 先落地以获得干净 R8 基线。

## Execution Plan

### Phase 1 — 全量 mutation 矩阵枚举 + purchase 域试点（pattern 验证）

Status: planned
Targets: `docs/analysis/per-mutation-processor-split-plan.md`（EXPAND 为完整 mutation 矩阵）、`module-purchase/erp-pur-service/.../processor/`（6 Processor → ~36 per-mutation 文件）、`module-purchase/erp-pur-service/.../entity/*BizModel.java`（6 BizModel @BizMutation 接管）、`module-purchase/erp-pur-meta/.../xbiz/*.xbiz`（xbiz `<source>` 清理）
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add | Proof`
- Prereqs: 无（抽象基类已就位）

- [ ] `Explore`：枚举全 42 Processor 的 mutation 矩阵——逐文件 grep public 方法签名，产出完整清单表（Processor × mutation × 方法签名 × 是否标准审批 mutation × 是否域特定 mutation）。EXPAND `docs/analysis/per-mutation-processor-split-plan.md` 为权威拆分清单（替换当前 40 行骨架）。估算输出 ~250 行 per-mutation 拆分目标。
  - Skill: `nop-backend-dev`
- [ ] `Explore`：**`<source>` 块 delegation vs inline-script 分类**——逐 xbiz 文件审查全部 `<source>` 块，按二态分类：(a) **delegation**（`<source>` 仅调 `inject(xxxProcessor).method(...)` 一行委托）→ 拆分时移除 `<source>` 块即可；(b) **inline-script**（`<source>` 内联 XLang 脚本含 `throw new NopScriptError(...)` / 状态迁移逻辑 / ErrorCode 参数）→ 拆分时须将内联脚本语义提取为新 per-mutation Processor 的 hook 实现（非机械移除）。审查 6 个 purchase xbiz 实测：每实体 5 个 `<source>` 块中仅 2-4 个为 delegation，余 1-3 个为 inline-script。产出分类清单表（xbiz × mutation × delegation/inline × 关键 ErrorCode/参数）供 Phase 2-4 引用。
  - Skill: `nop-backend-dev`
- [ ] `Explore`：**抽象基类 hook 兼容性 + side-effect 重定位语义审计**——逐 Processor 审查 doApprove/doReject 等方法体，三态分类 side-effect：(a) **before-save**（save 前执行的校验/计算）→ 映射到 `validateBusinessRules` / `validateCanCancel` 等 pre-hook；(b) **after-save-inline**（当前在 doXxx 方法体内 `dao().updateEntity()` 后立即执行的副作用，如 commitment hook / intercompany hook / budget hook）→ 映射到 `afterStateChange` post-hook；(c) **idempotent**（幂等副作用，重定位无语义影响）。关键审计点：`AbstractApproveProcessor.approve()` 在末尾统一调 `dao().updateEntity(entity)` 一次，但既有 Processor 在 doApprove 方法体内多次/内联调 `updateEntity`——须确认 side-effect 重定位后「updateEntity 调用时序相对 side-effect」的可观察等价性（如 commitment voucher 创建在 order save 之后是否改变最终一致性语义）。产出 hook 映射表 + 每族 Processor 的等价性论证。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：per-mutation 文件命名 + 包结构裁决——选项：(A) `module-<domain>/erp-<short>-service/.../processor/<Entity><Method>Processor.java`（如 `ErpPurOrderApproveProcessor`，与既有 monolithic 同包）；(B) `.../processor/<entity>/<Method>Processor.java`（按实体分子包）。记录选择 + 理由。推荐 (A)：与既有命名链一致 + checker R8 regex 兼容 + IDE 检索友好。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：BizModel @BizMutation 委托模式裁决——选项：(A) BizModel `@Inject` N 个 per-mutation Processor（每个 mutation 一个 @Inject 字段）；(B) BizModel `@Inject` 一个 `ProcessorFactory` 或保留原 monolithic Processor 作 facade（内部委派 per-mutation）。记录选择 + 理由 + Nop IoC 注入约束验证。推荐 (A)：Nop @Inject 非 private 字段已支持多注入 + 与抽象基类设计意图一致 + 避免 facade 层冗余。**@BizMutation 重载消歧说明**：BizModel 持有唯一 `@BizMutation` 方法名（`approve`/`reject`/etc.），per-mutation Processor 同名 public 方法经 `@Inject` 字段名消歧（`approveProcessor.approve(...)` vs `rejectProcessor.reject(...)`），Nop GraphQL resolver 按 BizModel 方法名绑定不经 Processor 类名，无 GraphQL 重载冲突。
  - Skill: `nop-backend-dev`
- [ ] `Add`：purchase 域 6 Processor 试点拆分——按 Phase 1 Explore 产出的 `<source>` 分类表 + hook 映射表，逐 Processor 逐 mutation 创建 per-mutation 文件：delegation 类直接移除 `<source>` + BizModel `@BizMutation` 一行委托；inline-script 类将内联脚本语义提取为 per-mutation Processor 的 hook 实现（保留 ErrorCode/参数/状态迁移逻辑逐字等价）。purchase 域选作试点因 mutation 矩阵最全（6 标准审批 + Requisition `convertToOrder` + Receive `confirm` 域特定）**且 inline-script 形态在此域最显著**（`ErpPurOrder.xbiz` withdrawApproval 含 `NopScriptError` 内联），试点同步验证 delegation-swap 与 inline-extract 两条路径。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：purchase 域 `mvn test -pl module-purchase/erp-pur-service` 全绿（既有测试行为不变）+ xbiz 快照匹配（`<source>` 清理/提取后行为等价，inline-script 提取的 per-mutation hook 经既有快照覆盖 ErrorCode/参数/状态迁移断言）。若抽象基类 hook 签名与既有 Processor side-effect 模式不兼容（如 `updateEntity` 时序差异致 side-effect 可观察变化），在此 Phase 暴露并修正——Phase 1 Explore 产出的 hook 映射表 + 等价性论证是此 Proof 的前置裁决依据。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 全 42 Processor mutation 矩阵清单落盘（`per-mutation-processor-split-plan.md` EXPAND）
- [ ] `<source>` delegation vs inline-script 分类表 + 抽象基类 hook 兼容性 + side-effect 重定位映射表落盘（含每族 Processor 等价性论证）
- [ ] per-mutation 文件命名 + BizModel 委托模式（含 @BizMutation 重载消歧说明）裁决记录
- [ ] purchase 域 6 Processor 试点拆分完成（delegation-swap + inline-extract 两路径均验证）+ `mvn test -pl module-purchase/erp-pur-service` 全绿（pattern 验证通过）

### Phase 2 — sales + finance 域批量拆分（14 Processor）

Status: planned
Targets: `module-sales/erp-sal-service/.../processor/`（6 Processor）、`module-finance/erp-fin-service/.../processor/` + `.../budget/` + `.../posting/`（8 Processor）、对应 BizModel + xbiz
Skill: `nop-backend-dev`

- Item Types: `Add`
- Item Types Note: Phase 2 is Add-heavy（per-mutation 文件创建 + BizModel 接管）
- Prereqs: Phase 1 pattern 验证通过（purchase 试点全绿）

- [ ] `Add`：sales 域 6 Processor 按试点 pattern 拆分（Order / Quotation / Delivery / Receipt / Return / Invoice）——delegation 类 `<source>` 移除 + BizModel `@BizMutation` 接管，inline-script 类 `<source>` 按分类表提取为 per-mutation hook。参照 Phase 1 产出的 hook 映射表处理 side-effect 重定位。
  - Skill: `nop-backend-dev`
- [ ] `Add`：finance 域 8 Processor 按试点 pattern 拆分（EmployeeAdvance / BadDebt / ExpenseClaim / NotesPayable / NotesReceivable / AccountingPeriod / Posting / BudgetScenario）——delegation + inline-script 双路径 + hook 映射。注意 `ErpFinPostingProcessor` + `ErpFinBudgetScenarioProcessor` 位于非标准包（`.../posting/` + `.../budget/`），保持原包路径。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`mvn test -pl module-sales/erp-sal-service,module-finance/erp-fin-service` 全绿（行为不变）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] sales + finance 14 Processor 拆分完成 + 对应域 `mvn test` 全绿

### Phase 3 — assets + manufacturing + inventory 域批量拆分（17 Processor）

Status: planned
Targets: `module-assets/erp-ast-service/.../processor/`（9 Processor）、`module-manufacturing/erp-mfg-service/.../processor/`（4 Processor）、`module-inventory/erp-inv-service/.../processor/`（4 Processor）、对应 BizModel + xbiz
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [ ] `Add`：assets 域 9 Processor 拆分 + BizModel `@BizMutation` 接管（delegation + inline-script 双路径 + hook 映射）。assets 域 Processor 数最多（9），含域特定 mutation（Disposal / Split / Merge / Inventory count / ValueAdjustment / Capitalization / Depreciation / Maintenance / Cip）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：manufacturing 域 4 Processor 拆分（WorkOrder / ScheduleToJobCard / JobCard / SubcontractOrder）+ 域特定 mutation（checkAvailability / start / reportCompletion / recordWork）保留在域特定 Processor。
  - Skill: `nop-backend-dev`
- [ ] `Add`：inventory 域 4 Processor 拆分（CostAdjust / LandedCost / OwnershipTransfer / StockMove）+ 域特定 mutation（allocate / confirm / reverseConfirm）保留。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`mvn test -pl module-assets/erp-ast-service,module-manufacturing/erp-mfg-service,module-inventory/erp-inv-service` 全绿。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] assets + manufacturing + inventory 17 Processor 拆分完成 + 对应域 `mvn test` 全绿

### Phase 4 — 剩余域批量拆分（crm + quality + aps + projects，5 Processor）+ 全域验证

Status: planned
Targets: `module-crm/erp-crm-service/.../processor/`（2）、`module-quality/erp-qa-service/.../processor/`（1）、`module-aps/erp-aps-service/.../processor/`（1）、`module-projects/erp-prj-service/.../processor/`（1）、对应 BizModel + xbiz、`docs/audits/compliance-baseline.md`（R8 新基线裁决）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 3 完成

- [ ] `Add`：crm（Conversion / Lead）+ quality（Recall）+ aps（Scheduling）+ projects（ProjectSettlement）共 5 Processor 拆分 + BizModel `@BizMutation` 接管（delegation + inline-script 双路径 + hook 映射）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test`（0 Failure / 0 Error，行为全量回归）+ xbiz 快照全域匹配。
  - Skill: none
- [ ] `Decision`：R8 基线结构性裁决——per-mutation 拆分后 R8 形态变化（42 monolithic → ~250 per-mutation）。裁决 per-mutation 文件是否计入 R8（按 xbiz 路由 = 不计入，因 per-mutation 文件经 BizModel @BizMutation 路由非 Processor xbiz 接线）。更新 `compliance-baseline.md` R8 基线 + 结构性变化注记。
  - Skill: none

Exit Criteria:

- [ ] 剩余 5 Processor 拆分完成（全域 42 Processor 全部拆分为 per-mutation）
- [ ] `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 0 Failure（行为全量回归）
- [ ] R8 基线结构性裁决记录（per-mutation 形态 + 新基线 + checker 校准方向）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_068c7a2b5ffedNl2jk9fszVrRr`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-25) — 0 Blocker / 4 Major / 5 Minor。全部 load-bearing 事实主张经实时仓库逐项核实（42 Processor ✓ + 0 抽象基类消费者 ✓ + ~250 目标估算合理 + 6 purchase xbiz 每实体 5 `<source>` 块仅 2-4 delegation 余 inline-script ✓ + `AbstractApproveProcessor.approve()` 末尾统一 `updateEntity` vs 既有 Processor 内联 `updateEntity` + side-effect 时序差异 ✓）。4 Major 已修订：(M1) Phase 1 新增 `<source>` delegation vs inline-script 二态分类 Explore + Phase 2-4 Add 项区分 delegation-swap 与 inline-extract 双路径；(M2) Closure Gates 矛盾消解——Goal #1 参数化「域不被阻塞的前提下完成全域 42」+ gate 措辞对齐（域级阻塞 = 显式范围变更非静默降级）；(M3) Phase 1 新增抽象基类 hook 兼容性 + side-effect 重定位语义审计 Explore（before-save/after-save-inline/idempotent 三态 + updateEntity 时序等价性论证）；(M4) Decision #3 补 @BizMutation 重载消歧说明（BizModel 持有唯一方法名，per-mutation Processor 同名方法经 @Inject 字段名消歧，无 GraphQL 重载冲突）。5 Minor 已修订：(m1) R8=49 叙述精确化 + Plan N=1 依赖说明；(m2) xbir→xbiz 术语统一；(m3) N=1 软依赖 Infrastructure prereqs 段补齐；(m4) Phase 4 R8 Decision Skill 待执行期复核；(m5) 试点选择理由补 inline-script 验证路径。R1-R14 全 PASS（修订后）。

- Independent draft review iteration 2: `accept` (`ses_068c253e6ffeolXifSyrIP7uqC`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-25) — M1/M2/M3/M4 全部修订经实时仓库逐项核实**genuine 落地**（M1 Phase 1 `<source>` 二态分类 Explore + Phase 2-4 双路径引用 ✓ / M2 Goal #1 参数化 + Closure Gates 矛盾消解 ✓ / M3 hook 兼容性 Explore 含 updateEntity 时序等价性论证 ✓ / M4 Decision #3 @BizMutation 消歧说明 ✓）。5 Minor 全部修订。0 new Blocker / 0 new Major。核心事实复核：42 Processor + 0 抽象基类消费者 + anti-slack clean。R1-R14 全 PASS。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及 service 层 Java（~250 新 per-mutation 文件 + 42 BizModel 接管 + xbiz 清理）。无 ORM/契约/字典/view 变更。完整仓库验证：`mvn clean install -DskipTests` + `mvn test`（行为全量回归）+ checker 复跑（R8 结构性变化记录）。

- [ ] 范围内行为完成（42 Processor 全部拆分为 per-mutation + 抽象基类激活 + BizModel @BizMutation 接管；若某域执行期发现阻塞，该域按 Deferred But Adjudicated 移出范围并记录触发条件——此为域级范围变更，须记录理由，非静默降级）
- [ ] 相关文档对齐（per-mutation-processor-split-plan.md 完整清单 + compliance-baseline.md R8 裁决）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（0 Failure / 0 Error）+ checker 复跑
- [ ] 无范围内项目静默降级为 deferred/follow-up（域级阻塞移出范围须显式记录理由 + 触发条件，符合规则 10 检查清单完整性）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 域特定 mutation per-mutation 拆分（confirm / allocate / checkAvailability / reportCompletion 等）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 域特定 mutation 无对应抽象基类（`Abstract*Processor` 仅覆盖 6 标准审批 mutation）；强制拆分需新建域特定抽象基类，scope 膨胀。本计划仅拆分标准审批 mutation，域特定 mutation 保留在 monolithic 残留 Processor 或 BizModel 内联。
- Successor Required: `no`（触发条件：域特定 mutation 数 > 5 且有重复模式时，开域特定抽象基类 successor）

### `ErpFinPostingProcessor` / `ErpFinBudgetScenarioProcessor` 非标准包路径

- Classification: `watch-only residual`
- Why Not Blocking Closure: 两 Processor 位于 `.../posting/` + `.../budget/` 而非 `.../processor/`，包路径不统一。本计划保持原包路径不迁移（包迁移是纯审美重构，高风险低收益）。
- Successor Required: `no`（触发条件：finance 域 Processor 包结构重大重组时）

## Closure

Status Note: <待独立草案审查 + 结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立审计>

Follow-up:

- 域特定 mutation 抽象基类（触发条件见上）
