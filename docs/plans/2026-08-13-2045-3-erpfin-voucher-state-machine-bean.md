# 2026-08-13-2045-3-erpfin-voucher-state-machine-bean 会计凭证 ErpFinVoucher.docStatus 实体级状态机 Bean（M4.1）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护会计过账核心行为（凭证 DRAFT→POSTED 过账 + reverseVoucher 红冲置 isReversed=true，全域经 `IErpFinAcctDocProvider` 聚合 + `ErpFinPostingProcessor` 引擎过账，已由起草者经 live code 实证：`ErpFinVoucherBizModel:88-103/105-116` + `ErpFinPostingProcessor:808,812`）。M4 plan-first 门控成立且为 M4 最核心保护项；该人工裁定非起草者可自主解除（project-context.md 会计保护域硬停止）。计划格式/完备性/范围/结束证据就绪 + 人工门控确认后，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.1（ErpFinVoucher.docStatus，plan-first，过账核心）；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.1` + §5.1 死状态（ErpFinVoucher CANCELLED）
> Related: M4 plan-first 先例 `2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（§11.2 M4 硬约束 (i)–(v) + 人工门控 honest framing）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；姊妹 M4 计划 `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`（M4.2，凭证-期间耦合耦合方）
> Mission: entity-state-machine
> Work Item: M4.1
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。凭证 docStatus `DRAFT→POSTED`（过账核心）+ reverseVoucher（POSTED 上置 `isReversed=true` 红冲）均属受保护会计过账/红冲行为，且 `IErpFinAcctDocProvider` 为全域业务单据聚合入口。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退/红冲闭环不改，继续由 `ErpFinPostingProcessor` 引擎 + `isReversed`/docStatus=POSTED 契约管理；(iii) `posted` 不入轴（§3；注：`ErpFinVoucher` 无独立 `posted` boolean 字段，过账状态即 `docStatus=POSTED`，此约束对本实体概念上成立）；(iv) 跨域副作用（`IErpFinAcctDocProvider` 聚合、业财回链 `ErpFinVoucherBillR`、BudgetVoucherGenerator/CloseVoucherWriter/IntercompanyVoucherGenerator/Consolidation/CommitmentVoucherGenerator 等生成路径）保留原 Processor/生成器路径；(v) 既有红冲闭环（isReversed 单边标记 + postingType=REVERSAL）以 isReversed 为契约不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施。
>
> **规则 14 拆分声明（与 M4.2 AccountingPeriod）**：M4.1（Voucher）与 M4.2（AccountingPeriod）虽同属 finance owner doc，但具**实质性不同的机器形状、结束标准与验证路径**（Voucher = 单边 Bean DRAFT→POSTED + isReversed 标志边界 + 7 生成路径 writer 分类 + CANCELLED 死状态；Period = 5 态窗口机 + 反结账 kill-switch + 事务瞬态），且为不同路线图工作项，按指南规则 14 例外拆为独立计划。两者经「凭证-期间耦合约束」（postVoucher/reverseVoucher 的 assertPeriodNotLocked 动态守卫）相互引用，但各自独立迁移、独立结束。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.1` + §5.1 死状态 + 实仓核实。

- **实体**：`ErpFinVoucher`（`module-finance/model/app-erp-finance.orm.xml:411`），`docStatus` `ext:dict="erp-fin/voucher-status"`（`:428`）。dict 3 值（`app-erp-finance.orm.xml:51`）：`DRAFT/POSTED/CANCELLED`。常量 `ErpFinConstants.VOUCHER_STATUS_*`。
- **关联契约字段（不迁移，但行为边界）**：`isReversed`（boolean `:426`，红冲单边标记）；`postedBy/postedAt`（审计 `:429-430`）；`reversalOfVoucher`（`:447` to-one，红字凭证回链）；`billLinks`（`:443` 业财回链 `ErpFinVoucherBillR`）。**注**：`ErpFinVoucher` 无独立 `posted` boolean 字段——过账状态即 `docStatus=POSTED` 本身（区别于 `ErpFinExpenseClaim`/`EmployeeAdvance` 等源单据有独立 `posted`）；§11.2 M4 (iii)「`posted` 不入轴」对本实体概念上成立（无 posted 轴可排除），本计划迁移的是 `docStatus` 业务轴。
- **docStatus 现状 writer — 命名业务动作（2 个，Bean 委托对象，实仓核实）**：
  - `postVoucher`（`ErpFinVoucherBizModel:88-103` `@BizMutation`）：`assertPeriodNotLocked`（:90，凭证-期间耦合动态守卫，CLOSED/CLOSED_FINAL 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`）+ 守卫 `docStatus==DRAFT`（:92 否则 `ERR_FIN_VOUCHER_ILLEGAL_TRANSITION` + `ARG_VOUCHER_ID` + `ARG_CURRENT_STATUS`）→ `setDocStatus(VOUCHER_STATUS_POSTED)`（:96）+ postedBy/postedAt（:97-98）。**唯一 docStatus 迁移边 DRAFT→POSTED**。
  - `reverseVoucher`（`:105-116` `@BizMutation`）：`assertPeriodNotLocked`（:107）+ 守卫 `docStatus==POSTED`（:109 否则 `ERR_FIN_VOUCHER_ILLEGAL_TRANSITION`）→ `setIsReversed(true)`（:113）。**不写 docStatus**（POSTED 保留）——是 `isReversed` 标志操作，**非 docStatus 迁移边**；其 docStatus==POSTED 守卫是 isReversed 操作的前置分类（Bean 提供 `isPosted` 分类 helper，不发明 POSTED→? 边）。
- **docStatus 现状 writer — 程序化生成路径（7，§9.2 选项 c 初始态/生成写入，不调 assertCan*，layer-2 须全集枚举分类）**：
  - `ErpFinPostingProcessor:808,812`（引擎 `persistVoucher`：setIsReversed + setDocStatus(POSTED)）——业财自动过账生成，凭证生成即 POSTED。
  - `CloseVoucherWriter:101-102`（期末结转：setIsReversed(false) + POSTED）。
  - `BudgetVoucherGenerator:133,137`（预算凭证：isReversed + POSTED）。
  - `ErpFinConsolidationEliminationPostEliminationProcessor:88-89`（合并抵销：setIsReversed(false) + DRAFT）。
  - `IntercompanyVoucherGenerator:212-214,304-305`（内部交易：POSTED）。
  - `ErpFinBudgetScenarioCarryForwardProcessor:315`（预算结转：POSTED）。
  - `CommitmentVoucherGenerator:149,217`（承付占用凭证生成 + 释放红冲：POSTED）。
  - （layer-2 全集审计须确认无遗漏；上述 7 生成路径经起草者 grep 核实。**layer-2 须重新独立 grep 全集**——起草者首轮漏列 `CommitmentVoucherGenerator`，已由独立草案审查捕获并补入，故 Phase 3 layer-2 不得再声明「起草者已知全集」，须以审查者零信任 grep 为准。`BankStatementImporter:72`/`BankReconciliationBuilder:96,129,140` 使用 `VOUCHER_STATUS_*` 常量但作用于 `ErpFinBankStatement`/`ErpFinBankReconciliation` head 实体，**非 ErpFinVoucher writer**，layer-2 排除。）
- **CANCELLED 死状态（§5.1 已登记）**：dict 有 CANCELLED，但**零生产 writer** 写入 ErpFinVoucher.docStatus=CANCELLED；草稿凭证废弃经 `useLogicalDelete`（逻辑删除）承载，不经 DRAFT→CANCELLED 迁移（owner doc §1 + §3 + §5 明确）。Bean 不纳入 initial/terminal/transitions，登记为 `intentional reserved` 死状态。
- **isReversed 红冲闭环（owner doc 已知简化）**：reverseVoucher 在原凭证置 `isReversed=true` 单边标记（保留 POSTED），**不建立 `reversedVoucherId` 双向回链**；红字凭证经 `postingType=REVERSAL` 关联。红冲闭环功能完整。双向回链 = successor（报表需求驱动）。本计划保持既有红冲语义（§11.2 M4 (v)）。
- **凭证-期间耦合（跨计划）**：postVoucher/reverseVoucher 的 `assertPeriodNotLocked` 守卫凭证所属期间非 CLOSED/CLOSED_FINAL（`ErpFinVoucherBizModel:191` 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`）。期间轴归 M4.2（姊妹计划）；本计划保留此动态守卫原位，不破坏耦合。
- **错误码现状**：`ERR_FIN_VOUCHER_ILLEGAL_TRANSITION`（`ErpFinErrors:436`，参数 voucherId/currentStatus）；`ERR_FIN_VOUCHER_PERIOD_LOCKED`（:443）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（Bean 抛 common 码，BizModel 映射领域码，common 作 cause）。
- **生产 Bean 注册范式**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml` 已显式注册 Processor/服务 Bean（FQN id）。StateMachine Bean 追加于文件末尾。
- **既有层 3 回归基线（非 greenfield）**：`TestErpFinVoucherPeriodLock`（期间锁定守卫）、`TestErpFinVoucherReversePreview`（红冲预览/反向）、`TestErpFinPostingExceptionWorkbench`（过账异常工作台）、`TestErpFinPostingMetrics`、`TestErpFinPeriodCloseEndToEnd`/`TestErpFinAnnualClose`（期末/年结凭证生成路径）、合并/内部交易/预算凭证相关测试。**注意**：M0.2 §3.5 finance M4.1「测试：无」——层 1 矩阵测试为 greenfield（新增），层 3 既有回归为上述具名测试。Phase 3 登记 M0.2 测试名漂移。
- **合规基线**：`@Inject private` 须保持 R5=0（fin-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md §对象一：会计凭证状态机` 完整覆盖 DRAFT/POSTED/CANCELLED + 迁移矩阵（DRAFT→POSTED + 红冲 isReversed + CANCELLED 预留）+ 红冲已知简化 + 草稿废弃经 logical delete。**无 owner-doc 缺口**。

## Goals

- 落地无状态 `ErpFinVoucherDocumentStateMachine`（docStatus 轴，`Document` 后缀避免与 future approveStatus 混淆，§3 单轴分离）：**唯一迁移边** `postVoucher`：`{DRAFT}→POSTED`（`assertCanPost(DRAFT)` + `postVoucherTargetStatus()=POSTED`）；分类 helper `isPosted(POSTED)`（供 reverseVoucher 前置分类，非迁移边）；`isReversed` 标志操作（reverseVoucher）**不建模为 docStatus 边**（POSTED 保留）。分类 initial=`{DRAFT}`，terminal=`{POSTED}`。CANCELLED = intentional reserved 死状态（不纳入任一集合）。可经 Delta 同名覆盖。
- 将 `ErpFinVoucherBizModel.postVoucher` 的固定来源态守卫（docStatus==DRAFT）改调 Bean `assertCanPost(from)` + 目标态回写；reverseVoucher 的 docStatus==POSTED 守卫改委托 Bean `isPosted` 分类 helper。**动态业务守卫与副作用保留原位**：`assertPeriodNotLocked`（凭证-期间耦合）、postedBy/postedAt 写入、isReversed 红冲单边标记、乐观锁、业财回链。
- 保持全部既有外部行为不变（错误码 + 参数、DRAFT→POSTED 边、reverseVoucher isReversed 语义、期间锁定守卫、7 生成路径 POSTED 写入、CANCELLED 逻辑删除路径、红冲闭环）。
- 新增层 1 矩阵完备性表驱动测试（DRAFT→POSTED 唯一边 + 非法来源态 + CANCELLED 死状态排除 + isPosted 分类 + isReversed 非 docStatus 边）；层 3 既有集成测试回归全绿。
- 层 2 四方对照：7 生成路径 writer 全集分类（命名动作 vs §9.2 生成写入 vs 跨域副作用）+ CANCELLED 死状态裁定 + isReversed 红冲边界 + 期间耦合边界。

## Non-Goals

- 不迁移 `posted`（boolean，§3 不入轴）、不迁移 `isReversed` 为状态轴（它是红冲单边标记 boolean，非状态机轴；reverseVoucher 的 docStatus==POSTED 守卫用 Bean 分类 helper，不发明 POSTED→? 边）。
- 不改变 `ErpFinPostingProcessor` 引擎过账编排（persistVoucher 时序）、不改变 7 生成路径（BudgetVoucherGenerator/CloseVoucherWriter/IntercompanyVoucherGenerator/Consolidation/CarryForward/CommitmentVoucherGenerator）的 POSTED/DRAFT 写入语义（§9.2 生成路径，不调 assertCan*）、不改变红冲闭环（isReversed 单边标记 + postingType=REVERSAL + 业财回链）、不改变凭证-期间耦合守卫 `assertPeriodNotLocked`（属期间侧 M4.2，保留原位）。
- 不实现 `reversedVoucherId` 双向回链（owner doc 已知简化 successor）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（CANCELLED 死状态保留 dict 项，不删；草稿废弃继续经 useLogicalDelete）。
- 不迁移 `ErpFinAccountingPeriod.status`（M4.2 姊妹计划）、`ErpFinVoucherLine/Template/BillR`（非状态轴）或 finance 其余轴。
- 不引入通用 CRUD 对 docStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地单边 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——凭证过账核心 + 红冲，全域 `IErpFinAcctDocProvider` 聚合入口）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 死状态/生成路径 + §9.2 初始态写入选项 c）、`docs/design/finance/state-machine.md`（§对象一 会计凭证 + §两类状态机的耦合约束 + §已知限制 xwf）、`docs/design/finance/posting.md`（业财打通）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 finance M4.1 + §5.1 CANCELLED 死状态）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.1 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel 接线、单边矩阵、生成路径识别（§9.2）、isReversed 红冲边界、过账引擎边界保持、错误码映射、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「矩阵表驱动测试（含死状态排除 + isReversed 非 docStatus 边）+ 既有集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`（重点：7 生成路径 writer 全集分类）。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护会计过账核心行为（凭证 DRAFT→POSTED + 红冲），且为全域 `IErpFinAcctDocProvider` 聚合入口。在人工/owner-doc 确认「以行为保持的单边矩阵集中化方式迁移此轴、过账引擎/7 生成路径/红冲闭环完整保留、isReversed 非 docStatus 轴」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record（对齐 M3.10/M4 plan-first 先例）。M4.1 为 M4 最核心保护项，门控审查应最严格。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpFinVoucherDocumentStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinVoucherDocumentStateMachine.java`（新建）、`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinVoucherDocumentStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [x] 新建无状态 `ErpFinVoucherDocumentStateMachine`（§2 无状态约束）：**唯一迁移边** `assertCanPost(DRAFT)`→`postVoucherTargetStatus()=POSTED`；分类 helper `isPosted(status)`（POSTED=true）+ `isTerminal(POSTED)=true`；`initialStatuses()={DRAFT}`、`terminalStatuses()={POSTED}`、`transitions()`=1 边（postVoucher）。**CANCELLED 不纳入任一集合**（intentional reserved 死状态，javadoc 标注 + §5.1 引用）。**isReversed 不建模为 docStatus 边**（javadoc 标注：reverseVoucher 在 POSTED 上置 isReversed=true，POSTED 保留，非 docStatus 迁移；isPosted helper 供其前置分类）。非法来源态（POSTED/CANCELLED post）抛 common 码携带 action/fromStatus。grep 证实不 import DAO/IBiz/IServiceContext/事务。
  - Skill: `nop-backend-dev`
- [x] Decision（前置）：记录三项分类供 Phase 3 引用：(a) CANCELLED = intentional reserved 死状态（零 writer，草稿废弃经 useLogicalDelete，§5.1 已登记）；(b) isReversed = boolean 红冲标记（非 docStatus 轴，reverseVoucher 不写 docStatus）；(c) 7 生成路径 = §9.2 选项 c 初始态/生成写入（不调 assertCan*）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] 在非生成 `app-service.beans.xml` 以 FQN id 注册 Bean（§11.1 步骤 2）。
  - Skill: `nop-backend-dev`
- [x] Proof（层 1 矩阵完备性，表驱动，§11.1 步骤 4）：`TestErpFinVoucherDocumentStateMachineMatrix` 覆盖 post（DRAFT 合法、POSTED/CANCELLED 非法）+ 终态 POSTED 无出边 + transitions(1) + initial/terminal + **断言 CANCELLED 不在 initial/terminal/transitions 任一集合**（死状态）+ isPosted(POSTED=true, DRAFT/CANCELLED=false)。**不经 BizModel 入口**（层 1 只测 Bean）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] Bean 无状态、单边矩阵完整；CANCELLED 死状态排除；isReversed 非 docStatus 边；三项 Decision 记录在案
- [x] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [x] 层 1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - BizModel 接线（行为保持，过账引擎/生成路径/红冲/期间耦合保留）+ 层 3 回归

Status: completed
Targets: `ErpFinVoucherBizModel`（postVoucher/reverseVoucher 委托）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [x] `ErpFinVoucherBizModel` 注入 `ErpFinVoucherDocumentStateMachine`（`@Inject` 非 private）：postVoucher 将 `:91-92` 内联 `docStatus==DRAFT` 守卫替换为 `stateMachine.assertCanPost(from)` + 目标态写回 `postVoucherTargetStatus()`（`:96`）；reverseVoucher 将 `:108-109` `docStatus==POSTED` 守卫改委托 `stateMachine.isPosted(status)` 分类 helper；`previewReverseVoucher`（`:126`，`@BizQuery` 只读预览）的 `docStatus==POSTED` 守卫同样委托 `isPosted`（只读预览守卫，一致性，非迁移边）。common→`ERR_FIN_VOUCHER_ILLEGAL_TRANSITION` 映射（common 作 cause）+ `ARG_VOUCHER_ID`/`ARG_CURRENT_STATUS` 对外不变。**完整保留**：postVoucher 的 `assertPeriodNotLocked`（:90，凭证-期间耦合动态守卫）+ postedBy/postedAt（:97-98）；reverseVoucher 的 `assertPeriodNotLocked`（:107）+ `setIsReversed(true)`（:113）红冲单边标记 + 业财回链；乐观锁。**7 生成路径（PostingProcessor 引擎 / BudgetVoucherGenerator / CloseVoucherWriter / IntercompanyVoucherGenerator / Consolidation / CarryForward / CommitmentVoucherGenerator）不接线 Bean**（§9.2 生成写入，保留原位）。
  - Skill: `nop-backend-dev`
- [x] Proof（层 3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinVoucherPeriodLock`（期间锁定守卫 `ERR_FIN_VOUCHER_PERIOD_LOCKED` 不变）、`TestErpFinVoucherReversePreview`（红冲预览/反向 isReversed 语义不变）、`TestErpFinPostingExceptionWorkbench`（过账异常路径）、`TestErpFinPeriodCloseEndToEnd`/`TestErpFinAnnualClose`（期末/年结凭证生成路径 POSTED 写入不变）。证明 DRAFT→POSTED 边、reverseVoucher isReversed 语义、期间锁定、生成路径、红冲闭环均不变。
  - Skill: `nop-testing`

Exit Criteria:

- [x] postVoucher/reverseVoucher 内联固定守卫改调/委托 Bean，grep 证实两方法体内不再有内联固定 docStatus 矩阵判断（动态守卫 assertPeriodNotLocked + postedBy/postedAt + isReversed 写入除外；7 生成路径不调 assertCan*）
- [x] 领域错误码 + 参数对外不变（层 3 断言证实）；DRAFT→POSTED 边 + reverseVoucher isReversed + 期间锁定 + 生成路径 + 红冲闭环行为不变
- [x] 层 3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 3 - 层 2 四方对照（7 生成路径 writer 全集分类）+ 漂移 Decision + owner doc 补注

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（§对象一 生成路径/isReversed 边界补注）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度，重点 writer 全集分类）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（层 2 四方对照，§11.1 步骤 5，10 维度）：dict（voucher-status 3 值）↔ owner doc（§对象一）↔ Bean ↔ writer。**重点 = writer 全集分类**（独立 grep 重核，结论 = 恰好 7 生成路径 + 1 命名动作，无第 8 writer）：(a) 命名动作 postVoucher（DRAFT→POSTED，Bean assertCanPost + postVoucherTargetStatus 写回）；(b) reverseVoucher + previewReverseVoucher（isReversed 标志操作/只读预览，docStatus==POSTED 守卫用 Bean isPosted，非迁移边）；(c) 7 生成路径（PostingProcessor:812 / CloseVoucherWriter:102 / BudgetVoucherGenerator:137 / ConsolidationElimination:89 / IntercompanyVoucherGenerator:214,305 / BudgetScenarioCarryForward:315 / CommitmentVoucherGenerator:149,217 = §9.2 初始态/生成写入，不调 assertCan*）；(d) BankStatementImporter:72/BankReconciliationBuilder:96,129,140 使用 VOUCHER_STATUS 常量但作用于 ErpFinBankStatement/ErpFinBankReconciliation head（非 ErpFinVoucher writer，排除）；(e) CANCELLED 死状态（零 writer，草稿废弃经 useLogicalDelete）；(f) isReversed 红冲边界（单边标记，无双向回链 successor）；(g) 期间耦合边界（assertPeriodNotLocked 属期间侧 M4.2）。writer 盘点含命名动作 + 生成路径 + 框架入口 + 测试 fixture。详细四方对照记录见 §Closure。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Add owner doc：在 `docs/design/finance/state-machine.md §对象一` 补生成路径实现注记（§7.1：业财自动过账/期末结转/预算/合并抵销/内部交易/预算结转/承付占用 7 生成路径直接写 POSTED/DRAFT，不经 DRAFT→POSTED 命名动作，Bean 不覆盖此 §9.2 路径）+ isReversed 非 docStatus 轴边界声明（reverseVoucher 在 POSTED 上置 isReversed=true，Bean isPosted 分类 helper；CANCELLED 死状态排除）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Decision（漂移裁定，路线图规则 5）：(a) CANCELLED = intentional reserved 死状态（§5.1 已登记，Bean 不纳入任一集合，dict 值保留不删）；(b) isReversed = boolean 红冲标记（非状态轴）；(c) 7 生成路径 = §9.2 生成写入（如实反映，非 implementation drift）；(d) `reversedVoucherId` 双向回链 = owner doc 已知简化 successor（报表需求驱动）；(e) M0.2 §3.5 finance M4.1「测试：无」与实仓具名层 3 测试存在漂移——登记建议 reconcile。裁定明细见 §Closure。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 四方对照无未裁决漂移（7 生成路径分类 + CANCELLED 死状态 + isReversed 边界 + 期间耦合 + 双向回链 successor 均裁定并落入 owner doc/计划）
- [x] owner doc §对象一 生成路径/isReversed 补注与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00718f89affemc064PrFPB62Or`，新会话零信任实仓复核 + 穷尽 writer 扫描) — 1 BLOCKER + 1 MAJOR + 2 MINOR：BLOCKER = writer 全集不完整——漏列 `CommitmentVoucherGenerator:149,217`（POSTED on ErpFinVoucher），而计划多次声明「全集」（layer-2 四方对照核心交付会不完整）；MAJOR = 计划误称 `ErpFinVoucher` 有独立 `posted` boolean 字段（实仓该实体无 posted 列，过账状态即 docStatus=POSTED）；MINOR = guard 行号 off-by-one（`:92`/`:109` 为 throw 行）+ `previewReverseVoucher:126` 同 docStatus==POSTED 守卫未处理。v2 已：补入 CommitmentVoucherGenerator 全位置（基线/Phase2/Phase3/Non-Goals/Goals/Deferred/owner-doc）+ 全量「6+」→「7」+ 撤回「全集」声明（Phase 3 须独立 re-grep）、删除 posted 字段误称并概念化 §11.2 M4 (iii)、previewReverseVoucher 委托 isPosted。
- Independent draft review iteration 2: `acceptable as-is`（draft pending M4 gate）（`ses_00710428dffeQzBPq4QgLdafXY`，新会话零信任复核 + 跨模块穷尽 writer 扫描）— writer 全集审计结论 = **COMPLETE（恰好 7，无第 8 writer）**；全部 iter1 BLOCKER/MAJOR/MINOR CONFIRMED-FIXED（CommitmentVoucherGenerator 全位置落地 + 计数一致「7」+ 全集声明撤回 + posted 字段修正 + previewReverseVoucher 处理）；无新增 blocker/major；§11.2 M4 (i)–(v)、rule 14 拆分（vs M4.2）有效、CANCELLED 死状态裁定、isReversed 非 docStatus 轴设计、anti-slack 全 PASS。草案审查收敛。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。本计划触及受保护会计过账核心行为（凭证 DRAFT→POSTED + 红冲，全域 `IErpFinAcctDocProvider` 聚合入口），为 M4 最核心保护项。草案审查已收敛（acceptable as draft）。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的单边矩阵集中化方式迁移此轴、过账引擎/7 生成路径/红冲闭环完整保留、isReversed 非 docStatus 轴」可接受。门控解除，`Plan Status: draft → active`。
- Mission-driver 四维审查（iter 3，`2026-08-13`）：`approved（held as draft）` — 格式/完备性/范围/结束证据四维 checklist 全 PASS：必需 section + 字段名 + Phase 结构 + Item Types 均合法；退出标准清晰可测且覆盖全部 checklist 项；范围边界清晰（M4.1 vs M4.2 rule 14 拆分声明 + 详尽 Non-Goals，无 scope creep）；Closure Gates 含验证命令 + Closure 段 + Deferred 分类均完备。**无 Blocker/Major**。唯一 hold = §11.2 M4 (i) 人工/owner-doc 门控（上游人工决策，review 时不可自主解除，project-context.md 会计保护域硬停止）——escape-hatch 保持 `Plan Status: draft`，Review Hold 已在位。
- Mission-driver 四维审查（iter 4，`2026-08-13`，mission `2026-08-13-193118-mission-driver`）：`approved（held as draft）` — 独立复核确认 iter 3 结论。四维 checklist 复 PASS：格式（全部模板 section + 字段名 + Phase 结构 + 合法 Item Types）、完备性（Exit Criteria 可测、Execution Plan 覆盖全部 checklist 项含 CANCELLED 死状态/isReversed 边界/7 生成路径分类/期间耦合）、范围（单轴 docStatus + 详尽 Non-Goals + rule 14 拆分声明，无 scope creep）、结束证据（Closure Gates 含验证命令 + Deferred 分类 + 证据占位）。**无 Blocker/Major**。唯一 hold 仍为 §11.2 M4 (i) 人工/owner-doc 门控（上游人工决策，不可自主解除）——保持 `Plan Status: draft`，Review Hold 在位（holding provision 适用，已发 `approved` marker 表示 review 已运行）。

## Closure Gates

> 本计划含生产代码变更（1 Bean + VoucherBizModel 接线 + 测试 + owner doc 补注），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（3 值保留 + CANCELLED 死状态不删绑），Compliance 基线预期无漂移（R5=0/R11=0）。

- [x] 范围内行为完成（Bean + VoucherBizModel 接线 + 三层证据；过账引擎/7 生成路径/红冲闭环/期间耦合完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [x] 相关文档对齐（owner doc §对象一 §7.1 生成路径/isReversed 补注 + 漂移 Decision 登记；路线图 M4.1 done）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）——M4.1 为最核心保护项，门控最严格
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### reversedVoucherId 双向回链

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §3 + §对象一 已知简化——reverseVoucher 在原凭证置 isReversed=true 单边标记（保留 POSTED），不建立 reversedVoucherId 双向回链；红冲闭环功能完整（postingType=REVERSAL + 业财回链）。本计划保持既有红冲语义。
- Successor Required: yes（触发条件 = 报表需求驱动双向回链时）

### DRAFT→CANCELLED 显式作废动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: CANCELLED = intentional reserved 死状态，草稿废弃经 useLogicalDelete 承载。dict 值保留为未来「保留审计轨迹的显式作废动作」语义入口（owner doc §1 + §3）。
- Successor Required: yes（触发条件 = PM 要求草稿凭证显式作废工作流时）

### 7 生成路径统一经 Bean assertCanPost

- Classification: `watch-only residual`
- Why Not Blocking Closure: 7 生成路径（业财自动过账引擎 / 期末 / 预算 / 合并 / 内交易 / 结转 / 承付占用）按 §9.2 选项 c 为生成写入（凭证生成即 POSTED/DRAFT），非用户命名动作迁移；统一经 Bean 属更强写锁范畴 = M0.1 successor（CRUD/生成路径写入边界）。
- Successor Required: no（归 M0.1 全局 CRUD 写锁裁定）

### 凭证-期间耦合迁移（assertPeriodNotLocked）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `assertPeriodNotLocked`/`ERR_FIN_VOUCHER_PERIOD_LOCKED` 是凭证侧动态业务守卫，期间轴归 M4.2（姊妹计划）。本计划保留此守卫原位，不破坏耦合。
- Successor Required: yes（触发条件 = M4.2 期间 Bean 落地后，核对耦合不破坏）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD/生成路径写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: 已执行 Phase 1-3 全部交付。Bean `ErpFinVoucherDocumentStateMachine` 落地（1 迁移边 DRAFT→POSTED + isPosted/isTerminal 分类 + transitions/initial/terminal 元数据，CANCELLED 死状态排除）并注册于 `app-service.beans.xml`（FQN id）；`ErpFinVoucherBizModel` postVoucher/reverseVoucher/previewReverseVoucher 三方法接线（内联固定 docStatus 守卫 → Bean assertCanPost/isPosted 委托，common→领域码映射保持 `ERR_FIN_VOUCHER_ILLEGAL_TRANSITION` + 参数对外不变，动态守卫 assertPeriodNotLocked + postedBy/postedAt + isReversed 标记 + 业财回链 + 乐观锁原位保留）；层 1 矩阵测试 8 项 green；层 3 fin-service 全套 359 项 green（含 TestErpFinVoucherPeriodLock/ReversePreview/PostingExceptionWorkbench/PeriodCloseEndToEnd/AnnualClose 等关键回归）；owner doc §对象一 §7.1 补生成路径实现注记 + isReversed 边界声明。过账引擎/7 生成路径/红冲闭环/期间耦合完整保留（§11.2 M4 (ii)/(iv)/(v)），无 ORM/API/字典变更。

### 层 2 四方对照记录（§11.1 步骤 5，10 维度 + §11.4 警示）

独立 grep 命令：`rg -n "setDocStatus\(.*VOUCHER_STATUS|setDocStatus\(ErpFinConstants\.VOUCHER_STATUS" module-finance/ --type java -g '!*/test/*'`。结论：**ErpFinVoucher.docStatus 生产 writer = 1 命名动作 + 7 生成路径，无第 8 writer**。

| dict（erp-fin/voucher-status） | owner doc §对象一 | Bean 元数据 | 生产 writer（独立 grep 重核） |
|---|---|---|---|
| DRAFT | 初始态，等待过账 | `initialStatuses()={DRAFT}` | 命名动作 postVoucher 的来源态；生成路径 ConsolidationElimination:89 写 DRAFT（§9.2 生成写入） |
| POSTED | 终态（参与总账，红冲置 isReversed） | `terminalStatuses()={POSTED}`、`isTerminal(POSTED)=true`、`isPosted(POSTED)=true`、唯一迁移边 `postVoucher DRAFT→POSTED` | 命名动作 postVoucher:102 写回（经 Bean target）；7 生成路径直接写 POSTED（PostingProcessor:812/CloseVoucherWriter:102/BudgetVoucherGenerator:137/CommitmentVoucherGenerator:149,217/IntercompanyVoucherGenerator:214,305/BudgetScenarioCarryForward:315）§9.2 |
| CANCELLED | 预留 dict 项（未启用迁移，草稿废弃经 logical delete） | **不纳入** initial/terminal/transitions 任一集合（死状态） | **零生产 writer**（死状态）；草稿废弃经 useLogicalDelete 承载 |

writer 全集分类（10 维度核查）：

- (a) **命名动作迁移边**：`postVoucher`（DRAFT→POSTED）— 唯一经 Bean `assertCanPost` + `postVoucherTargetStatus()` 的迁移边。
- (b) **isReversed 标志操作（非 docStatus 边）**：`reverseVoucher`（置 isReversed=true，POSTED 保留）+ `previewReverseVoucher`（只读预览）— docStatus==POSTED 前置守卫委托 Bean `isPosted` 分类 helper，不产生 docStatus 迁移边。
- (c) **7 生成路径（§9.2 选项 c，不调 assertCan*）**：PostingProcessor:812（业财自动过账引擎）/ CloseVoucherWriter:102（期末结转）/ BudgetVoucherGenerator:137（预算影子凭证）/ CommitmentVoucherGenerator:149,217（承付占用/释放）/ IntercompanyVoucherGenerator:214,305（内部交易）/ BudgetScenarioCarryForward:315（预算结转）/ ConsolidationElimination:89（合并抵销候选）。
- (d) **排除（非 ErpFinVoucher writer）**：BankStatementImporter:72 / BankReconciliationBuilder:96,129,140 使用 VOUCHER_STATUS 常量但作用于 ErpFinBankStatement/ErpFinBankReconciliation head 实体，非 ErpFinVoucher writer。
- (e) **CANCELLED 死状态**：零 writer，草稿废弃经 useLogicalDelete，intentional reserved（§5.1 已登记）。
- (f) **isReversed 红冲边界**：单边标记（保留 POSTED），无 reversedVoucherId 双向回链（owner doc 已知简化 successor）。
- (g) **期间耦合边界**：assertPeriodNotLocked 是凭证侧动态业务守卫，期间轴归 M4.2 姊妹计划，本计划保留原位。
- (h) **框架入口（CRUD）**：契约 §9.2 选项 c 显式排除——通用 CRUD 可写 docStatus（无全局写锁），但本计划是「命名动作迁移矩阵唯一权威」声明（successor=M0.1 全局 CRUD 写锁）。
- (i) **测试 fixture**：多个测试种子直接 setDocStatus 构造初始/任意态（TestErpFinVoucherPeriodLock 等），非生产 writer。
- (j) **可达性**：DRAFT→POSTED 单边有向无环；CANCELLED 不可达（死状态，无入边）；POSTED 终态无出边。无死锁、无循环。

### 漂移裁定（Decision，路线图规则 5）

- **(a) CANCELLED = intentional reserved 死状态**：dict 有 CANCELLED 但零生产 writer；草稿废弃经 useLogicalDelete。Bean 不纳入 initial/terminal/transitions 任一集合；dict 值保留不删（未来显式作废工作流语义入口）。裁定 = 如实反映，非 implementation drift（§5.1 已登记 + owner doc §1/§3 明确）。Successor = PM 要求显式作废工作流时。
- **(b) isReversed = boolean 红冲标记（非状态轴）**：reverseVoucher 在 POSTED 上置 isReversed=true，不写 docStatus，非迁移边。Bean 提供 isPosted 分类 helper 供其前置守卫。裁定 = 设计裁定（契约 §3 + owner doc §对象一 §2/§3）。
- **(c) 7 生成路径 = §9.2 生成写入**：凭证生成即落目标态（POSTED/DRAFT），非用户命名动作迁移，不经 Bean assertCan*。裁定 = 如实反映，非 implementation drift（契约 §9.2 选项 c）。统一经 Bean = M0.1 successor。
- **(d) reversedVoucherId 双向回链 = owner doc 已知简化 successor**：reverseVoucher 单边标记 isReversed，不建双向回链；红冲闭环功能完整（postingType=REVERSAL + 业财回链）。Successor = 报表需求驱动双向回链时。
- **(e) M0.2 §3.5 finance M4.1「测试：无」与实仓漂移**：M0.2 清单标注 finance M4.1「测试：无」，但实仓存在具名层 3 集成测试（TestErpFinVoucherPeriodLock/ReversePreview/PostingExceptionWorkbench/PeriodCloseEndToEnd/AnnualClose）。登记 = 建议 M0.2 reconcile（非阻塞，属清单维护）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_003ea9048ffeRZobUN2TTZFNXN`，零信任 read-only，2026-08-14）
- Verdict: **PASS**（无 P0/P1/P2 发现）。8 维度（A Bean 无状态/单边矩阵/CANCELLED 死状态/isReversed 非边 + B Bean 注册 FQN id + C BizModel 接线无内联矩阵/动态守卫保留/错误码映射 + D 7 生成路径未接线 + E writer全集=1 命名动作+7 生成路径无第8 writer/CANCELLED 零 writer + F 层 1 矩阵不经 BizModel + G owner doc §7.1 与代码一致 + H plan/roadmap 一致性）全部经 live grep/read 实证确认。
- Execution Evidence: Bean `module-finance/erp-fin-service/.../statemachine/ErpFinVoucherDocumentStateMachine.java`（新建）+ `app-service.beans.xml`（注册）+ `ErpFinVoucherBizModel`（postVoucher/reverseVoucher/previewReverseVoucher 接线）+ `TestErpFinVoucherDocumentStateMachineMatrix`（层 1，8 项 green）+ owner doc `docs/design/finance/state-machine.md §7.1`（补注）。验证：`mvn test -pl module-finance/erp-fin-service -o`（层 3 全套 359 项 0 failures/errors BUILD SUCCESS）+ Closure `mvn clean install -DskipTests`（全 reactor BUILD SUCCESS）+ `bash docs/audits/nop-compliance-checker.sh`（R5=0/R11=0 基线维持）。

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
