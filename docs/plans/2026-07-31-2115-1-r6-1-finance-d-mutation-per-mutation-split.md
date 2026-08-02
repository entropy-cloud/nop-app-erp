# 2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split finance 域 D-mutation + 内联多步 mutation per-mutation 拆分

> **草案审查修正（iteration 1）**：R6.0 triage 将 BadDebt `submit` 误分类为 D-mutation——实测 `ErpFinBadDebtProcessor.submit:98-100` 已是 MR5 单行委托（`submitForApprovalProcessor.submitForApproval`），属 S-mutation。本 plan 修正 catA 20→**19**，total 41→**40**，并标记 R6.0 triage 计数错误待回填。

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.1
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-30-1433-3-mr5-r5-3-finance-s-mutation.md`（R5.3 finance S-mutation 先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.1
> Audit: required

## Current Baseline

- **MR5 finance 域 S-mutation 已完成**：20 个 S-mutation per-mutation Processor 自包含，4 个含 S-mutation 的 facade（EmployeeAdvance/ExpenseClaim/BadDebt/BudgetScenario）公共 S-mutation 方法已精简为单行委托。MR6 **不重开 MR5**。
- **类别 A 违规 facade（4 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + 公共 D-mutation 入口**：
  - `ErpFinAccountingPeriodProcessor`（916 行）— D-mutation 入口 6：`preCheck`、`closePeriod`、`finalizePeriod`、`generateNextYearPeriods`、`reverseClose`、`openPeriod`（实测 `grep public` 确认）。处置：**delete-after-extract**（无 S-mutation 委托残留——AccountingPeriod 无标准审批六动作）。
  - `ErpFinBadDebtProcessor`（427 行）— D-mutation 入口 **2**：`writeOff`（:73）、`recover`（:85）。`submit`（:98）**非 D-mutation**——实测已是 MR5 S-mutation 单行委托（`return submitForApprovalProcessor.submitForApproval(...)`），与 `approve`/`reject`/`reverseApprove` 同属已拆分的标准审批六动作。处置：**slim-to-S-delegation-facade**（facade 仅保留 S-mutation 单行委托 + delete writeOff/recover D-mutation）。
  - `ErpFinNotesPayableProcessor`（236 行）— D-mutation 入口 4：`issue`、`honor`、`dishonor`、`writeOff`。处置：**delete-after-extract**（纯 D-mutation facade）。
  - `ErpFinNotesReceivableProcessor`（406 行）— D-mutation 入口 7：`receive`、`discount`、`endorse`、`collect`、`honor`、`dishonor`、`writeOff`。处置：**delete-after-extract**（纯 D-mutation facade）。
  - **类别 A 须拆合计：19 D-mutation → 19 个新 `<Entity><Method>Processor`**（AccountingPeriod 6 + BadDebt 2 + NotesPayable 4 + NotesReceivable 7）。D-mutation per-mutation 文件**尚不存在**（实测 `ls` finance processor 目录无 ClosePeriod/WriteOff/Issue/Honor 等文件），本 plan 须**新建**。
- **类别 A BizModel 配线现状**（实测）：`ErpFinAccountingPeriodBizModel`/`ErpFinNotesPayableBizModel`/`ErpFinNotesReceivableBizModel` 各 `@Inject` 对应 facade 并委托 D-mutation（`return facade.method(...)`）；`ErpFinBadDebtBizModel` 对 S-mutation 已 `@Inject` per-mutation Processor（MR5 成果），对 D-mutation（writeOff/recover）仍委托 `badDebtProcessor`。facade 删除/瘦身 D-mutation 后，BizModel 须**重配线**为 `@Inject` 对应 per-mutation Processor + 单行委托（BadDebtBizModel 仅需加 writeOff/recover Processor 注入；其余 3 BizModel 全部 mutation 重配线）。
- **类别 B 违规 BizModel（9 个 BizModel，21 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）——R6.0 triage 须拆清单**：
  - `ErpFinBankReconciliationBizModel`（3：generate/post/reverse）
  - `ErpFinBankStatementBizModel`（1：importStatement）
  - `ErpFinBankStatementLineBizModel`（2：autoMatch/manualMatch）
  - `ErpFinCashForecastBizModel`（1：refreshForecast）
  - `ErpFinConsolidationEliminationBizModel`（2：generateEliminationCandidates/postElimination）
  - `ErpFinCreditFacilityBizModel`（3：accrueInterest/releaseCredit/reserveCredit）
  - `ErpFinGlMappingRuleBizModel`（1：refreshCache）
  - `ErpFinIntercompanyMatchBizModel`（1：runMatching）
  - `ErpFinPostingExceptionBizModel`（2：ignore/retry）
  - `ErpFinReconciliationBizModel`（4：create/post/reverse/runAutoReconciliation）
  - `ErpFinVoucherTemplateBizModel`（1：renderTemplate）
  - **类别 B 须拆合计：21 个新 `<Entity><Method>Processor`**（target 命名见 roadmap §R6.0 triage 展开 §R6.1）。
  - 合法豁免 1（`ErpFinPostingException.manualEntry`，`:46` 单步状态翻转），保留 BizModel 不动。
- **[会计保护区域]** AccountingPeriod close/finalize/reverseClose/openPeriod 涉及期间状态机 + GL 余额；BadDebt writeOff/recover 涉及凭证 + ArApItem；NotesPayable issue/honor + NotesReceivable receive/discount/collect 涉及票据凭证；Reconciliation post/reverse 涉及核销凭证。owner doc `docs/design/finance/`（period-close / posting / ar-ap-reconciliation / state-machine）已固化语义；R1.8-R1.11/R1.16 已修复相关缺陷。本 plan 仅做**编排位置迁移**（facade/BizModel → per-mutation Processor），不改业务语义。
- **既有测试基线**：finance 域 `mvn test` ~306 测试 0 failures（R5.3 实测 303 + R3.6 后增量）。
- **与 R5.3 的差异**：R5.3 per-mutation 文件已存在（hollow），本 plan 须**新建文件**；R5.3 有抽象基类 hook（AbstractApproveProcessor 等），D-mutation 无审批 hook → 直接写 `process()` 主流程 + protected step（对齐 `:80-97` 范式，roadmap line 295）。

## Goals

- finance 域 40 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 19 + 类别 B 21），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 类别 A 4 facade 按 triage 处置（3 delete-after-extract [AccountingPeriod/NotesPayable/NotesReceivable 无 S-mutation] + 1 slim-to-S-delegation-facade [BadDebt]）；类别 A BizModel 重配线为 `@Inject` per-mutation Processor + 单行委托。facade 被多 mutation 共享的辅助方法保留 facade protected helper（单一真相源）。
- 类别 B 11 个 BizModel 的 21 个内联 `@BizMutation` 改为 `@Inject <Entity><Method>Processor` + 单行委托。
- beans.xml 注册全部新 Processor bean；xbiz 无 inline-script 残留（类别 B 是 Java→Processor，非脚本转换）。
- finance 域 `mvn test` 全绿（0 failures），会计保护区域语义不变经既有测试验证。
- arm-index P1-MA3-062 finance 域须拆项标记 done。

## Non-Goals

- R6.2-R6.8（其他域 + 全量验证）——属后续 plan。
- BadDebt S-mutation 重构（MR5 已完成，状态保持 done）。
- 新增业务测试——测试覆盖深挖属 MR2/MR3（已完成）；本 plan 仅验证既有测试行为等价。
- 业务语义变更、状态机迁移、错误码语义调整——仅编排位置迁移。
- 合法豁免 `ErpFinPostingException.manualEntry`（保留 BizModel）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/finance/`（state-machine / period-close / posting / ar-ap-reconciliation）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。涉及会计保护区域（期间/坏账/票据/核销），须对照 R1.x owner doc 静态校验语义不变。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（4 facade → 19 per-mutation Processor）+ BizModel 重配线

Status: completed
Targets: `module-finance/erp-fin-service/.../processor/ErpFin{AccountingPeriod,BadDebt,NotesPayable,NotesReceivable}*Processor.java`（新建 19 文件）；4 facade 瘦身/删除；4 BizModel 重配线
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——facade 被多 D-mutation 共享的 protected helper（如 `ErpFinAccountingPeriodProcessor.requirePeriod`/`validateCanClose`/GL 余额维护方法、`ErpFinBadDebtProcessor.executeWriteOff`/`executeRecovery`、NotesPayable/Receivable 凭证生成 helper）的归属：方案 A 保留 facade protected helper + per-mutation 经 `@Inject` facade 调用（最小变更，单一真相源，对齐 R5.3 Pattern B）；方案 B 上提到域专属基类 `AbstractErpFinXxxProcessor`。裁决依据：若 per-mutation 跨包（facade 在 budget 包），helper 提 public；同包则保留 protected。在首个 facade 拆分时定方案并记录替代分析。
  - Skill: `nop-backend-dev`
  - **裁决记录**：采用方案 A（保留 facade protected helper + per-mutation `@Inject` facade，同包 protected 可达）。4 facade 类保留为共享 helper 持有者（非物理删除），仅删除 D-mutation public 入口方法。AccountingPeriod `closeAnnual` 因调用 `generateNextYearPeriods`（已迁出 facade），为避免 facade↔GenerateNextYearPeriodsProcessor 循环依赖，将 `closeAnnual` 一并迁入 `ClosePeriodProcessor`（@Inject facade + PreCheckProcessor + GenerateNextYearPeriodsProcessor + AnnualCloseService）。`advanceModule`/`Module` 枚举保留 facade public（TestErpFinModuleCloseOrder 直引）。
- [x] Add: `ErpFinAccountingPeriodProcessor` 6 D-mutation 拆分 → `ErpFinAccountingPeriodPreCheckProcessor` / `...ClosePeriodProcessor` / `...FinalizePeriodProcessor` / `...GenerateNextYearPeriodsProcessor` / `...ReverseCloseProcessor` / `...OpenPeriodProcessor`。每个含 `process()` 主流程 + protected step（requirePeriod → validateTransition → execute → doStateChange/GL维护）。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpFinBadDebtProcessor` 2 D-mutation 拆分 → `ErpFinBadDebtWriteOffProcessor` / `...RecoverProcessor`。facade 保留 S-mutation 单行委托（slim-to-S-delegation-facade），delete writeOff/recover D-mutation。`submit` 非 D-mutation 不拆（实测已委托 `submitForApprovalProcessor`，MR5 成果）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpFinNotesPayableProcessor` 4 D-mutation 拆分 → `ErpFinNotesPayableIssueProcessor` / `...HonorProcessor` / `...DishonorProcessor` / `...WriteOffProcessor`。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpFinNotesReceivableProcessor` 7 D-mutation 拆分 → `ErpFinNotesReceivableReceiveProcessor` / `...DiscountProcessor` / `...EndorseProcessor` / `...CollectProcessor` / `...HonorProcessor` / `...DishonorProcessor` / `...WriteOffProcessor`。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 19 新 Processor bean（bean id = 首字母小写类名）。
  - Skill: `nop-backend-dev`
  - 实测 bean id 采用全限定类名（对齐既有 per-mutation bean 注册范式）；`@Inject` 按类型解析，id 不影响装配。
- [x] Add: 类别 A BizModel 重配线——4 BizModel（AccountingPeriod/NotesPayable/NotesReceivable/BadDebt）的 D-mutation `@BizMutation` 方法从 `@Inject facade` 改为 `@Inject` 对应 per-mutation Processor + 单行委托。delete-after-extract facade（AccountingPeriod/NotesPayable/NotesReceivable）删除后 BizModel 必须重配线才能编译；BadDebtBizModel 仅 writeOff/recover 重配线（S-mutation 已配线）。
  - Skill: `nop-backend-dev`
- [x] Proof: finance service 本地编译通过（`mvn compile -pl module-finance/erp-fin-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 19 per-mutation 自包含 + 4 facade 瘦身/删除 + 4 BizModel 重配线 + 编译通过。

- [x] 19 个新 `<Entity><Method>Processor` 文件存在且自包含（`process()` + protected step，非 `return facade.method()` 回委托）
- [x] 4 facade 按处置执行（3 delete-after-extract [AccountingPeriod/NotesPayable/NotesReceivable] + 1 slim-to-S-delegation [BadDebt]）+ 4 BizModel D-mutation 重配线 + beans.xml 更新
- [x] finance service 本地编译通过

### Phase 2 - 类别 B BizModel 内联 mutation 拆分（11 BizModel → 21 per-mutation Processor）

Status: completed
Targets: `module-finance/erp-fin-service/.../processor/ErpFin*Processor.java`（新建 21 文件）；11 BizModel `@BizMutation` 改单行委托
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 21 个类别 B mutation 拆分——逐 BizModel 内联 `@BizMutation` 提取到 `<Entity><Method>Processor`（process + protected step），BizModel 改 `@Inject` Processor + `return processor.method(id, ctx)` 单行委托。完整清单（Entity.method → target Processor）：BankReconciliation.generate/post/reverse、BankStatement.importStatement、BankStatementLine.autoMatch/manualMatch、CashForecast.refreshForecast、ConsolidationElimination.generateEliminationCandidates/postElimination、CreditFacility.accrueInterest/releaseCredit/reserveCredit、GlMappingRule.refreshCache、IntercompanyMatch.runMatching、PostingException.ignore/retry、Reconciliation.create/post/reverse/runAutoReconciliation、VoucherTemplate.renderTemplate。
  - Skill: `nop-backend-dev`
  - **设计记录**：类别 B per-mutation Processor 自包含（`@Inject IDaoProvider` + 服务，对齐类别 A `dao.getEntityById/updateEntity` 范式）。Reconciliation 4 Processor 共享 helper 抽到 `AbstractErpFinReconciliationProcessor` 基类（FX 凭证 + 校验 + settler/partnerBalanceUpdater/voucherBiz）；runAutoReconciliation Processor 组合 CreateProcessor + PostProcessor。VoucherTemplate 的 `TemplateExprEvaluator` 随 renderTemplate 迁入 Processor，新增 public `evalTemplateExpr` 入口供 `TestErpFinVoucherTemplateExpr` 跨包访问（已更新测试引用）。
- [x] Add: beans.xml 注册全部 21 新 Processor bean（类别 B）。
  - Skill: `nop-backend-dev`
- [x] Proof: finance service 本地编译通过（`mvn compile -pl module-finance/erp-fin-service -am -DskipTests`）+ grep 确认 11 BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 B 21 per-mutation 自包含 + 11 BizModel 改 `@Inject` Processor 单行委托 + 编译通过。

- [x] 21 个新 Processor 文件存在且自包含
- [x] 11 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认无残留编排体）
- [x] beans.xml 更新 + finance service 本地编译通过

### Phase 3 - finance 域运行时行为等价回归

Status: completed
Targets: `module-finance/erp-fin-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: finance 域 `mvn test -pl module-finance/erp-fin-service -am` 全绿（~306+ 测试，0 failures）。类别 A + B mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线。
  - Skill: `nop-testing`
  - **实测**：`Tests run: 306, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。无快照漂移（GraphQL 经 BizModel 契约面不变，Processor 为内部编排重构）。

Exit Criteria:

> 本阶段交付 finance 域行为等价证据。

- [x] finance 域 `mvn test` 全绿（0 failures）
- [x] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0474ab096ffeCZd6LWCMH58Gum`）—B1：BadDebt `submit` 误分类为 D-mutation（实测 `ErpFinBadDebtProcessor.submit:98-100` 已是 MR5 单行委托 `submitForApprovalProcessor.submitForApproval`），应移除；catA 20→19，total 41→40。类别 B 枚举 + 豁免 + 会计保护区域 + 反 S/D 豁免均正确（非阻塞）。另发现类别 A BizModel 重配线缺口（facade 删除/瘦身后 BizModel 委托断链）。已修正：移除 BadDebt submit + 重计 40 + 新增 BizModel 重配线 item。
- Independent draft review iteration 2: needs revision（task `ses_04742fccdffeANGhiaITRnIAd8`）—NEW B1：facade 处置汇总计数自相矛盾——逐 facade 枚举（3 delete [AccountingPeriod/NotesPayable/NotesReceivable 无 S-mutation 实测确认] + 1 slim [BadDebt]）vs 汇总写"2 delete + 2 slim"（Goals/Exit/Closure Gates 3 处）。已修正为 3 delete + 1 slim。catA 19/total 40/BizModel 重配线均正确（非阻塞）。
- Independent draft review iteration 3: accept（task `ses_04740f4a4ffeiXjSy2htoib3OY`）—"3 delete + 1 slim" 处置汇总一致性已确认（Goals/Exit/Closure Gates 3 处对齐，唯一"2 delete + 2 slim"残留位于 iteration-2 审查记录属历史引用非活跃声明）。可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 finance 域 + compliance + 全量编译。

- [x] finance 域 40 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 19 + 类别 B 21）
- [x] 4 类别 A facade 按处置执行（3 delete-after-extract [AccountingPeriod/NotesPayable/NotesReceivable] + 1 slim-to-S-delegation [BadDebt]）
- [x] 4 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [x] 11 类别 B BizModel 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托
- [x] beans.xml 注册一致性（40 新 bean id 与 @Inject 匹配）
- [x] 合法豁免 `ErpFinPostingException.manualEntry` 保留 BizModel 未动
- [x] 会计保护区域语义不变（既有测试行为等价）
- [x] `mvn compile` 全域通过 + `mvn test -pl module-finance/erp-fin-service -am` 全绿
- [x] compliance checker 基线不高于当前基线
- [x] arm-index P1-MA3-062 finance 域须拆项标记 done
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: 全部三个 Phase 执行完毕。finance 域 40 个须拆 mutation（类别 A 19 + 类别 B 21）全部拆为独立 `<Entity><Method>Processor`，4 facade 按处置瘦身（3 delete-after-extract + 1 slim-to-S-delegation），4 类别 A + 11 类别 B BizModel 全部改为 `@Inject` per-mutation Processor 单行委托，beans.xml 注册 40 新 bean。合法豁免 `ErpFinPostingException.manualEntry` 保留 BizModel 未动。验证：finance `mvn test` 306 全绿（0 failures/0 errors）+ 全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + compliance checker exit 0。R6.0 triage 计数错误（BadDebt submit 误分类，catA 20→19/total 41→40）已回填 roadmap §MR6 R6.1 行 + §R6.1 triage 展开节 + arm-index P1-MA3-062。会计保护区域语义不变经既有测试验证（BizModel GraphQL 契约面不变，Processor 为内部编排重构）。唯一未勾门控=独立结束审计（执行者不可自我审计，留待独立子代理 CLOSURE_VERIFY）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY，新会话，不重用执行者上下文）于 2026-07-31 执行
- Evidence:
  - 新建 40 个 per-mutation Processor 文件（类别 A 19 + 类别 B 21）+ Reconciliation 共享基类 `AbstractErpFinReconciliationProcessor`（无 bean），均位于 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/`。
  - 4 facade 瘦身：`ErpFinAccountingPeriodProcessor`（删 6 D-mutation 入口 + closeAnnual 迁入 ClosePeriodProcessor，保留 shared helper/advanceModule/Module）/ `ErpFinNotesPayableProcessor`（删 4）/ `ErpFinNotesReceivableProcessor`（删 7）/ `ErpFinBadDebtProcessor`（删 writeOff/recover，保留 S-mutation 单行委托 + shared helper）。
  - 15 BizModel 重配线（类别 A 4 + 类别 B 11），所有 `@BizMutation` 方法体改为 `return processor.method(...)` 单行委托。
  - beans.xml 注册 40 新 bean（全限定类名 id，对齐既有范式）。
  - 验证命令与结果：
    - `mvn test -pl module-finance/erp-fin-service -am` → `Tests run: 306, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS
    - `mvn clean install -DskipTests`（全量）→ BUILD SUCCESS（156 reactor 模块）
    - `bash docs/audits/nop-compliance-checker.sh` → exit 0（actual ≤ baseline）
  - 测试引用修复：`TestErpFinVoucherTemplateExpr` 改引 `ErpFinVoucherTemplateRenderTemplateProcessor.evalTemplateExpr`（`TemplateExprEvaluator` 随 renderTemplate 迁入 Processor，新增 public 入口供跨包访问）。
  - 路线图回填：`docs/backlog/audit-remediation-roadmap.md` R6.1 行 todo→done + 计数 41/catA20→40/catA19；`docs/audits/arm-index.md` P1-MA3-062 标记 R6.1 finance done。
- Independent Closure Audit Walkthrough（独立子代理，新会话，2026-07-31）：
  - 实仓点数：`module-finance/erp-fin-service/.../processor/` 实测新建 40 个 per-mutation Processor（catA 19 = AccountingPeriod 6 + BadDebt 2 + NotesPayable 4 + NotesReceivable 7；catB 21 = BankReconciliation 3 / BankStatement 1 / BankStatementLine 2 / CashForecast 1 / ConsolidationElimination 2 / CreditFacility 3 / GlMappingRule 1 / IntercompanyMatch 1 / PostingException 2 / Reconciliation 4 / VoucherTemplate 1）+ 共享基类 `AbstractErpFinReconciliationProcessor`（无 bean），与 plan 计数一致。
  - beans 装配：`app-service.beans.xml` 实测 40 新 Processor 类名全部命中（逐类 `rg -q` 0 missing）。
  - Anti-hollow 抽查：`NotesPayableIssueProcessor`（requireNote→isAlreadyIssued→validateNotTerminal→requireAmountPositive→reserveCreditIfNeeded→`facade.doIssue`，`doIssue` 实测为 facade `protected` helper 非 public D-mutation 回委托，符合 Decision 方案 A）/ `BadDebtWriteOffProcessor`（requireOpenArApItem→newBadDebt→条件 executeWriteOff→saveEntity，真实编排体）/ `VoucherTemplateRenderTemplateProcessor`（320 行含 `evalTemplateExpr` public 入口）。无 `return null`/空体/`return facade.<public-mutation>` 反模式。
  - BizModel 单行委托：AccountingPeriod(closePeriod/finalizePeriod/reverseClose/openPeriod/generateNextYearPeriods)、BadDebt(writeOff/recover)、Reconciliation(create/post/reverse/runAutoReconciliation)、BankReconciliation(generate/post/reverse)、VoucherTemplate(renderTemplate)、PostingException(retry/ignore) 均为 `return <processor>.<method>(...)` 单行委托，零内联编排残留。
  - Facade 瘦身：4 facade `@BizMutation`/公共 D-mutation 入口已移除（grep 命中均为 javadoc 事务边界注释，非活跃方法）；BadDebt facade 保留 S-mutation 单行委托（slim-to-S-delegation）。
  - 合法豁免：`ErpFinPostingException.manualEntry` 实测仍位于 `ErpFinPostingExceptionBizModel:116`，无对应 ManualEntry Processor（豁免生效）。
  - 编译验证：`mvn compile -pl module-finance/erp-fin-service -am -DskipTests` → BUILD SUCCESS（独立复跑，非仅信任执行者声明）。
  - 文档同步：roadmap §MR6 R6.1 行 done + 计数 40/catA19 回填、arm-index P1-MA3-062 finance done、`docs/logs/2026/07-31.md` 存在并引用 R6.1，与 plan Closure 声明一致。
  - 裁决：Plan Status / 三 Phase Status / Exit Criteria（全 `[x]`）/ Closure Gates（全 `[x]`）/ Closure 证据五点一致，无 hollow 代码、无范围内项目降级、Deferred 区为空。审计通过。

Follow-up:

- 会计保护区域 helper 归属策略（facade protected vs 域专属基类）的裁决结果，回注供 R6.2-R6.7 参考。
- R6.0 triage 计数错误（BadDebt submit 误分类 D-mutation，catA 20→19 / total 41→40）须回填 roadmap §MR6 R6.1 行 + arm-index P1-MA3-062。
