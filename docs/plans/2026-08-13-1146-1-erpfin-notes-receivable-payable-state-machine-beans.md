# 2026-08-13-1146-1-erpfin-notes-receivable-payable-state-machine-beans 应收/应付票据 ErpFinNotesReceivable/ErpFinNotesPayable.status 实体级状态机 Bean（M4.8 + M4.9）

> Plan Status: draft
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控待确认——两轴票据迁移触发票据过账（receive/discount/endorse/honor/issue 经 `NotesPostingDispatcher`→`FinPostingExecutor` 生成凭证 + `ErpFinArApItem` 应收/应付辅助账，已由起草者经 live code 实证：`ErpFinNotesReceivableProcessor:112,128,143,154` + `ErpFinNotesPayableProcessor:88,99` tryPost + `ErpFinArApItemGenerator:174-179`）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 财务保护域硬停止）。计划格式/完备性/范围/结束证据就绪后，保持 `draft` 直至门控确认。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.8（ErpFinNotesReceivable.status）+ M4.9（ErpFinNotesPayable.status），plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.8/M4.9`（FIN-16/FIN-18 纳入，既有测试 TestErpFinNotesReceivableStateMachine/TestErpFinNotesPayableStateMachine）
> Related: M4 plan-first 先例 `2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4.1）+ `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`（M4.2）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；单域多实体 M4 先例 `2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（M4.29+M4.30）
> Mission: entity-state-machine
> Work Item: M4.8 + M4.9
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。应收/应付票据的过账触发动作（receive/discount/endorse/honor/issue）生成业财凭证 + ArApItem 辅助账，应付票据 issue/honor 还占用/释放授信额度（`IErpFinCreditFacilityBiz`）。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账/授信时序/编排/失败兜底不改，继续由 `NotesPostingDispatcher`→`FinPostingExecutor` 引擎 + `posted`/`postedBy`/`postedAt` 标志契约管理；(iii) `posted` 不入轴（§3；两实体均有 `posted` boolean，排除-posted）；(iv) 跨域副作用（ArApItem 生成、授信额度占用/释放、dishonor→应收票据冲销联动）保留原 Processor 路径；(v) 既有过账/授信/冲销闭环不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控未满足前保持 `draft`。
>
> **规则 14 合并声明（M4.8 + M4.9 同计划）**：应收票据（7 态）与应付票据（4 态）虽为不同实体，但同属 finance treasury 票据子系统、同一 owner-doc 义务（finance/treasury.md + state-machine.md §适用对象）、同一结果表面（票据 status 单轴状态机 Bean）、同一验证路径（既有 *StateMachine 测试 + 过账测试）。按规则 14 合并为单计划两个 Phase 组（Phase 1-3 Receivable，Phase 4 Payable，共享 Phase 5 四方对照 + owner doc），不拆为两计划。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.8/M4.9` + 实仓核实。

### ErpFinNotesReceivable（应收票据，7 态）

- **实体**：`ErpFinNotesReceivable`（`module-finance/model/app-erp-finance.orm.xml:1440`），单轴 `status` propId=20 `:1462` `ext:dict="erp-fin/notes-receivable-status"`。dict 7 值（orm.xml:284-292）：`RECEIVED/DISCOUNTED/ENDORSED/COLLECTION_PENDING/HONORED/DISHONORED/WRITE_OFF`。常量 `ErpFinConstants.NOTES_RECV_*`（`ErpFinConstants.java:256-262`）。
- **`posted` boolean**（propId=21 `:1463`，排除-posted 不入轴）+ `postedBy`(`:464`)/`postedAt`(`:465`)。无 `approveStatus` 字段——单轴实体。
- **status 现状 writer（7 命名动作，全部 `@BizMutation`，实仓核实）**：
  - `receive`→RECEIVED：facade `doReceive`（`ErpFinNotesReceivableProcessor:109`），经 `ErpFinNotesReceivableReceiveProcessor`。**initial 态写入**（票据收到即 RECEIVED，无 DRAFT 前态）。
  - `discount`→DISCOUNTED：facade `doDiscount`（`:124`），守卫 `validateTransitionForDiscount`（`:49` 要求 RECEIVED）。
  - `endorse`→ENDORSED：facade `doEndorse`（`:140`），守卫 `validateTransitionForEndorse`（`:56` 要求 RECEIVED）。
  - `collect`→COLLECTION_PENDING：**per-mutation Processor 直接写** `ErpFinNotesReceivableCollectProcessor:21`，守卫 `validateTransitionForCollect`（`:63` 要求 RECEIVED 或 DISCOUNTED）。
  - `honor`→HONORED：facade `doHonor`（`:151`），守卫 `validateTransitionForHonorOrDishonor`（`:73` 要求 COLLECTION_PENDING）。
  - `dishonor`→DISHONORED：**per-mutation Processor 直接写** `ErpFinNotesReceivableDishonorProcessor:22`，守卫同 `validateTransitionForHonorOrDishonor`（要求 COLLECTION_PENDING）。
  - `writeOff`→WRITE_OFF：facade `doWriteOff`（`:168`），守卫 `validateNotTerminal`（`:80`，任何非终态可注销）。
  - **writer 放置不对称**：collect/dishonor 在 per-mutation Processor 直接 setStatus，其余 5 个在 facade `do*` 写。迁移须两处均委托 Bean。
- **终态判定**：`isTerminal(status)`（`:280-285`）= `{HONORED, DISHONORED, WRITE_OFF}`。ENDORSED **非终态**（仅可 writeOff 出边，不可 collect）。initial=`{RECEIVED}`，terminal=`{HONORED, DISHONORED, WRITE_OFF}`。
- **过账副作用**：receive/discount/endorse/honor 经 `tryPostReceivable`（facade `:112,128,143,154`）→`NotesPostingDispatcher`→`FinPostingExecutor.postEvent` 生成凭证；COLLECTION_PENDING 与 DISHONORED **不过账**（collect 为在途态，dishonor 为终态重分类）。writeOff 若已过账则 `reverseReceivable`（`:165`）红冲。ArApItem：`ErpFinArApItemGenerator:174-179`（RECEIVED→RECEIVABLE 抵销客户 AR；ENDORSED→PAYABLE 抵销供应商 AP）。

### ErpFinNotesPayable（应付票据，4 态）

- **实体**：`ErpFinNotesPayable`（`module-finance/model/app-erp-finance.orm.xml:1482`），单轴 `status` propId=18 `:1502` `ext:dict="erp-fin/notes-payable-status"`。dict 4 值（orm.xml:293-298）：`ISSUED/HONORED/DISHONORED/WRITE_OFF`。常量 `ErpFinConstants.NOTES_PAY_*`（`ErpFinConstants.java:265-268`）。
- **`posted` boolean**（propId=19 `:1503`，排除-posted）+ `postedBy`(`:1504`)/`postedAt`(`:1505`)。无 `approveStatus` 字段——单轴实体。
- **status 现状 writer（4 命名动作，全部 facade `do*` 写，实仓核实）**：
  - `issue`→ISSUED：facade `doIssue`（`ErpFinNotesPayableProcessor:85`）。**initial 态写入**（票据开出即 ISSUED，无 DRAFT 前态）。
  - `honor`→HONORED：facade `doHonor`（`:96`），守卫 `validateTransitionForHonor`（`:45` 要求 ISSUED）。
  - `dishonor`→DISHONORED：facade `doDishonor`（`:107`）。
  - `writeOff`→WRITE_OFF：facade `doWriteOff`（`:115`），守卫 `validateNotTerminal`（`:52`）。
  - **writer 全部在 facade `do*`**（与 Receivable 不同——Payable dishonor 经 facade，Receivable dishonor 经 per-mutation）。
- **终态判定**：`isTerminal(status)`（`:161-166`）= `{HONORED, DISHONORED, WRITE_OFF}`。initial=`{ISSUED}`，terminal=`{HONORED, DISHONORED, WRITE_OFF}`。
- **过账 + 授信副作用**：issue/honor 经 `tryPostPayable`（`:88,99`）→`NotesPostingDispatcher` 生成凭证；writeOff 若已过账则 `reversePayable`（`:113`）红冲。**授信额度**：`IErpFinCreditFacilityBiz` 注入（`:38`），issue 时 `reserveCreditIfNeeded`（`:69`，银行承兑汇票占用授信），honor/dishonor/writeOff 时 `releaseOccupiedCredit`（`:76`）；config-gated `erp-fin.credit-check-on-issue`（`:156-158`）。ArApItem：Payable ISSUED/HONORED 未在 `ErpFinArApItemGenerator` profile（return null，不生成 AR/AP open item）。

### 共同基线

- **错误码**：`ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION`（`ErpFinErrors:236-238`，参数 `ARG_NOTES_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS`，消息含 expectedStatus 如「RECEIVED 或 DISCOUNTED」）；`ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION`（`:243-245`，同 3 参数）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（Bean 抛 common 码，Processor 映射领域码，common 作 cause）。
- **`illegalTransition` helper**：Receivable/Payable Processor 内各有 `illegalTransition(note, currentStatus, expectedStatus)` 工厂构造领域 `NopException`（携带 notesCode/currentStatus/expectedStatus）。
- **生产 Bean 注册**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（374 行）已显式注册 Processor（FQN id）：facade helper（`:151-154`）+ 11 per-mutation Processor（`:308-329`，注释 `<!-- R6.1 ... NotesPayable 4 + NotesReceivable 7 -->`）+ `NotesPostingDispatcher`（`:133-134`）。StateMachine Bean 追加于文件末尾。**finance 当前无 `statemachine/` 包**——本计划创建 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/`（M4.1/M4.2 计划已规划但尚未落地，本计划与之并行，互不依赖）。
- **既有测试（layer-3 回归基线，非 greenfield）**：
  - `TestErpFinNotesReceivableStateMachine`（`module-finance/erp-fin-service/src/test/.../entity/`，339 行，12 `@Test`）：receive/discount（含 FX 贴现 + interest/netAmount 数学）/endorse/collect(from RECEIVED & DISCOUNTED)/honor/dishonor/writeOff/非法 honor-from-RECEIVED/非法 writeOff-from-HONORED。
  - `TestErpFinNotesPayableStateMachine`（同目录，219 行，6 `@Test`）：issue(商业承兑/银行承兑占授信/授信不足拒绝)/honor 释放授信/writeOff 释放授信/非法 honor-from-WRITE_OFF。
  - 过账集成：`TestErpFinNotesReceivablePosting`（6 `@Test`：receive/discount/endorse 过账 + writeOff 红冲 + ArInvoice 核销）、`TestErpFinNotesPayablePosting`（2 `@Test`：issue 过账占授信 + honor 过账释放授信）。
  - **layer-1 矩阵测试为 greenfield**（新增）。
- **合规基线**：`@Inject private` 须保持 R5=0（fin-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md` §适用对象（`:7-10`）**仅列 Voucher + AccountingPeriod**，无 Notes Receivable/Payable 章节。票据状态机当前在 `docs/design/finance/treasury.md`（Receivable 7 态 `:56-75`，Payable `:77-93`，关键业务规则 `:197-202`）。**owner-doc 缺口**：需在 state-machine.md 补 §对象三（Notes Receivable）/§对象四（Notes Payable），交叉引用 treasury.md 业务规则。

## Goals

- 落地两个无状态 Bean（§2 无状态约束，单轴 `status`）：
  - `ErpFinNotesReceivableStateMachine`：矩阵 `receive`→RECEIVED（initial）；`discount` {RECEIVED}→DISCOUNTED；`endorse` {RECEIVED}→ENDORSED；`collect` {RECEIVED,DISCOUNTED}→COLLECTION_PENDING；`honor` {COLLECTION_PENDING}→HONORED；`dishonor` {COLLECTION_PENDING}→DISHONORED；`writeOff` {非终态}→WRITE_OFF。initial=`{RECEIVED}`，terminal=`{HONORED,DISHONORED,WRITE_OFF}`。
  - `ErpFinNotesPayableStateMachine`：矩阵 `issue`→ISSUED（initial）；`honor` {ISSUED}→HONORED；`dishonor` {ISSUED}→DISHONORED；`writeOff` {非终态}→WRITE_OFF。initial=`{ISSUED}`，terminal=`{HONORED,DISHONORED,WRITE_OFF}`。
  - 两 Bean 均可经 Delta 同名覆盖。
- 将 11 个命名动作的固定 `validateTransitionFor*`/`validateNotTerminal` 内联守卫改调 Bean `assertCan<Action>(from)` + 目标态回写（`<action>TargetStatus()`）。**动态业务守卫与副作用保留原位**：过账（`NotesPostingDispatcher.tryPost`/`reverse`）、授信占用/释放（`IErpFinCreditFacilityBiz`）、FX 贴现派生、posted 标志写入、乐观锁。
- 保持全部既有外部行为不变（错误码 + 参数、7+4 矩阵边、COLLECTION_PENDING/DISHONORED 不过账、ENDORSED 非终态仅 writeOff 出边、授信占用/释放时序、writeOff 红冲闭环）。
- 新增 layer-1 矩阵完备性表驱动测试（两 Bean 各覆盖全部合法/非法边 + initial/terminal + ENDORSED 非终态 + COLLECTION_PENDING/DISHONORED 不过账边界不属 Bean 范畴）；layer-3 既有集成测试回归全绿。
- layer-2 四方对照：确认 7+4 矩阵全可达 + ENDORSED 边界 + collect/dishonor writer 放置不对称 + M0.2 测试名（既有正确）。

## Non-Goals

- 不迁移 `posted`（boolean，§3 不入轴）、不迁移 `postedBy`/`postedAt`/授信占用为状态轴。
- 不改变 `NotesPostingDispatcher`→`FinPostingExecutor` 过账编排（时序、COLLECTION_PENDING/DISHONORED 不过账语义）、不改变授信占用/释放时序、不改变 writeOff 红冲闭环、不改变 FX 贴现派生。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（7+4 值保留）。
- 不统一 collect/dishonor 的 writer 放置位置（Receivable 当前在 per-mutation Processor，迁移只改其守卫委托 Bean，不改 writer 物理位置——避免行为变化）。
- 不迁移 `ErpFinVoucher.docStatus`（M4.1）、`ErpFinAccountingPeriod.status`（M4.2）、`ErpFinReconciliation`（M4.3）或 finance 其余轴。
- 不引入通用 CRUD 对 status 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地两单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——票据过账 + 授信，finance 保护域）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 死状态/initial 态写入 + §9.2 初始态写入选项 c）、`docs/design/finance/state-machine.md`（**§适用对象 缺口需补 §对象三/四**）、`docs/design/finance/treasury.md`（票据状态机 + 关键业务规则 §源）、`docs/design/finance/posting.md`（业财打通）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 finance M4.8/M4.9）、`docs/plans/2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（单域多实体 M4 先例）、`docs/plans/2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.8/M4.9 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade/per-mutation Processor 接线、单轴矩阵、过账/授信副作用保留、writer 放置不对称处理、错误码映射、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 *StateMachine/Posting 集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`（重点：11 writer 全集 + ENDORSED 边界）。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护票据过账 + 授信行为。在人工/owner-doc 确认「以行为保持的单轴矩阵集中化方式迁移两轴、过账/授信/红冲闭环完整保留、posted 不入轴、writer 放置不对称仅改守卫委托不改物理位置」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record（对齐 M4.1/M4.2 plan-first 先例）。
- 无新增端口/环境变量/CORS/密钥/.env/外部服务依赖（既有 `erp-fin.credit-check-on-issue` 保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - ErpFinNotesReceivableStateMachine Bean + 注册 + layer-1 矩阵测试

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinNotesReceivableStateMachine.java`（新建）、`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinNotesReceivableStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（layer-1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建无状态 `ErpFinNotesReceivableStateMachine`（§2 无状态约束）：矩阵 `assertCanReceive(initial/null)`→`receiveTargetStatus()=RECEIVED`（initial 态写入，§9.2 选项 c 接受 null→RECEIVED，但 Bean 仍校验非法来源态：非 null 且非 initial 非法）；`assertCanDiscount(RECEIVED)`→DISCOUNTED；`assertCanEndorse(RECEIVED)`→ENDORSED；`assertCanCollect({RECEIVED,DISCOUNTED})`→COLLECTION_PENDING；`assertCanHonor(COLLECTION_PENDING)`→HONORED；`assertCanDishonor(COLLECTION_PENDING)`→DISHONORED；`assertCanWriteOff(非终态)`→WRITE_OFF（`assertCanWriteOff` 校验 `!isTerminal(from)`）。分类 `initialStatuses()={RECEIVED}`、`terminalStatuses()={HONORED,DISHONORED,WRITE_OFF}`、`isTerminal` 同。`transitions()` 编码 7 命名边。**ENDORSED 标注为非终态中间态**（javadoc：endorse 后仅 writeOff 出边，不可 collect/discount）。非法来源态抛 common 码携带 `action`/`fromStatus`。grep 证实不 import DAO/IBiz/IServiceContext/事务。
  - Skill: `nop-backend-dev`
- [ ] Decision（前置）：记录 collect/dishonor writer 放置不对称分类——Receivable collect(`ErpFinNotesReceivableCollectProcessor:21`)、dishonor(`ErpFinNotesReceivableDishonorProcessor:22`) 在 per-mutation Processor 直接 setStatus，其余 5 个在 facade `do*`；迁移**两处均委托 Bean 守卫**，但**不改 writer 物理位置**（避免行为变化）。供 Phase 2/3 引用。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] 在非生成 `app-service.beans.xml` 以 FQN id 注册 Bean。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-1 矩阵完备性，表驱动，§11.1 步骤 4）：覆盖 receive（initial 合法、非 initial 非法）/discount（RECEIVED 合法、其余非法）/endorse（RECEIVED 合法）/collect（RECEIVED & DISCOUNTED 合法、ENDORSED/COLLECTION_PENDING/终态非法）/honor & dishonor（COLLECTION_PENDING 合法、其余非法）/writeOff（4 非终态合法、3 终态非法）+ 终态无出边 + transitions(7) + initial/terminal。**断言 ENDORSED 非终态但仅 writeOff 出边**（collect/discount/endorse 从 ENDORSED 非法）。**不经 Processor 入口**（layer-1 只测 Bean）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] Bean 无状态、7 边矩阵完整；ENDORSED 非终态边界正确；collect/dishonor 不对称 Decision 记录在案
- [ ] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [ ] layer-1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - NotesReceivable Processor 接线（行为保持，过账/红冲/FX 副作用保留）+ layer-3 回归

Status: planned
Targets: `ErpFinNotesReceivableProcessor`（facade do* 守卫委托）+ `ErpFinNotesReceivableCollectProcessor`、`ErpFinNotesReceivableDishonorProcessor`（per-mutation 守卫委托）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

  - [ ] `ErpFinNotesReceivableProcessor` 注入 `ErpFinNotesReceivableStateMachine`（`@Inject` 非 private），将 5 个 facade 守卫（`validateTransitionForDiscount:49`/`validateTransitionForEndorse:56`/`validateTransitionForCollect:63`/`validateTransitionForHonorOrDishonor:73`/`validateNotTerminal:80`）的内联固定 `Objects.equals` 判断替换为 Bean `assertCan<Action>(from)` + 目标态写回（`<action>TargetStatus()`）。**receive 路径**：既有 `ErpFinNotesReceivableReceiveProcessor` 经 `validateNotTerminal`（loose：任何非终态）+ `isAlreadyReceived` 幂等短路；迁移后 receive 委托 Bean `assertCanReceive(null/initial)`——此为**有意收窄**（receive 是 §9.2 选项 c 初始态写入，票据先于 receive 不存在有效非终态；实仓零生产路径/测试从非 initial 态 receive，回归无风险），writeOff 另委托 `assertCanWriteOff(非终态)` 保留 loose 语义。**collect/dishonor 的 per-mutation Processor**（`ErpFinNotesReceivableCollectProcessor:21`/`ErpFinNotesReceivableDishonorProcessor:22`）同样注入 Bean 并改守卫委托（writer 物理位置不动）。common→`ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION` 映射（common 作 cause）+ `ARG_NOTES_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` 对外不变（expectedStatus 文案如「RECEIVED 或 DISCOUNTED」保留）。**完整保留**：`tryPostReceivable`（`:112,128,143,154`）过账、`reverseReceivable`（`:165`）writeOff 红冲、FX 贴现派生、`markPosted`（`:265`）posted 标志、授信（Receivable 无授信）、乐观锁。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinNotesReceivableStateMachine`（11 @Test 全部：receive/discount Fx+interest/endorse/collect 双源/honor/dishonor/writeOff/非法 honor-from-RECEIVED/非法 writeOff-from-HONORED）、`TestErpFinNotesReceivablePosting`（receive/discount/endorse 过账 + writeOff 红冲 + ArInvoice 核销不变）。证明 7 矩阵边 + 过账/红冲 + FX 派生行为不变。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 5 facade 守卫 + 2 per-mutation 守卫改调 Bean 委托，grep 证实相关方法体内不再有内联固定 status 矩阵判断（动态副作用 tryPost/reverse/FX/markPosted 除外）
- [ ] 领域错误码 + 参数对外不变（layer-3 断言证实）；7 矩阵边 + 过账/红冲 + ENDORSED 边界行为不变
- [ ] layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 3 - ErpFinNotesPayableStateMachine Bean + 接线 + layer-1/3

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinNotesPayableStateMachine.java`（新建）、`app-service.beans.xml`（注册）、`TestErpFinNotesPayableStateMachineMatrix.java`（新建）、`ErpFinNotesPayableProcessor`（facade do* 守卫委托）
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2 完成（Payable 独立于 Receivable，但同计划顺序执行）

- [ ] 新建无状态 `ErpFinNotesPayableStateMachine`：矩阵 `assertCanIssue(initial/null)`→ISSUED；`assertCanHonor(ISSUED)`→HONORED；`assertCanDishonor(ISSUED)`→DISHONORED；`assertCanWriteOff(非终态)`→WRITE_OFF。initial=`{ISSUED}`、terminal=`{HONORED,DISHONORED,WRITE_OFF}`。`transitions()` 编码 4 命名边。非法来源态抛 common 码。grep 证实无 DAO/IBiz/事务 import。
  - Skill: `nop-backend-dev`
- [ ] 在 `app-service.beans.xml` 以 FQN id 注册 Bean。
  - Skill: `nop-backend-dev`
- [ ] `ErpFinNotesPayableProcessor` 注入 Bean，将 4 个 facade 守卫（`validateTransitionForHonor:45`/`validateNotTerminal:52` + doIssue/doDishonor 的隐式守卫）替换为 Bean `assertCan<Action>` + 目标态写回。**Payable writer 全部在 facade `do*`**（无 per-mutation 不对称）。common→`ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION` 映射 + 3 参数不变。**完整保留**：`tryPostPayable`（`:88,99`）过账、`reversePayable`（`:113`）writeOff 红冲、`reserveCreditIfNeeded`（`:69`）/`releaseOccupiedCredit`（`:76`）授信、`markPosted`（`:137`）、乐观锁。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-1 矩阵 + layer-3 回归）：layer-1 覆盖 issue/honor/dishonor/writeOff 合法+非法边 + 终态无出边 + transitions(4) + initial/terminal。layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinNotesPayableStateMachine`（6 @Test：issue 商业/银行承兑占授信/授信不足拒绝/honor 释放授信/writeOff 释放授信/非法 honor-from-WRITE_OFF）、`TestErpFinNotesPayablePosting`（issue 过账占授信 + honor 过账释放授信不变）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] Payable Bean 无状态、4 边矩阵完整；4 facade 守卫改调 Bean 委托，grep 证实无内联固定矩阵判断（动态副作用 tryPost/reverse/credit/markPosted 除外）
- [ ] 领域错误码 + 参数对外不变；4 矩阵边 + 过账/红冲 + 授信占用/释放行为不变
- [ ] layer-1 矩阵测试通过 + layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 4 - layer-2 四方对照（Receivable + Payable）+ owner doc 补章节 + 漂移 Decision

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（新增 §对象三 Notes Receivable + §对象四 Notes Payable）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度，重点 writer 全集 + ENDORSED 边界 + 不对称 writer）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 + Phase 3 完成

- [ ] Proof（layer-2 四方对照，§11.1 步骤 5，10 维度，两实体各一份）：dict（receivable 7 值 / payable 4 值）↔ owner doc（treasury.md + 新增 state-machine.md §对象三/四）↔ Bean 元数据 ↔ writer。Receivable 重点：(a) 7 命名动作 writer 全集（5 facade do* + collect/dishonor per-mutation，须独立 grep 重核）；(b) ENDORSED 非终态仅 writeOff 出边边界；(c) COLLECTION_PENDING/DISHONORED 不过账（非 Bean 范畴，过账侧不变）；(d) collect 双源（RECEIVED 或 DISCOUNTED）。Payable 重点：(a) 4 命名动作 writer 全集（全 facade do*）；(b) 授信占用/释放时序保留。writer 盘点含命名动作 + 过账路径（NotesPostingDispatcher 非状态 writer，排除）+ 框架入口 + 测试 fixture。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：在 `docs/design/finance/state-machine.md` §适用对象增列票据两项，新增 **§对象三：应收票据状态机**（7 态 + 矩阵：receive→RECEIVED/discount/endorse/collect 双源/honor/dishonor/writeOff + ENDORSED 非终态仅 writeOff + COLLECTION_PENDING/DISHONORED 不过账 + initial/terminal）与 **§对象四：应付票据状态机**（4 态 + 矩阵：issue→ISSUED/honor/dishonor/writeOff + 授信占用/释放 + initial/terminal），交叉引用 `treasury.md` 关键业务规则（`:197-202`）与 `posting.md` 票据业务类型。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision（漂移裁定，路线图规则 5）：(a) collect/dishonor writer 放置不对称 = `intentional legacy layout`（迁移仅改守卫委托，不改物理位置，非 implementation drift 需修正）；(b) ENDORSED 非终态仅 writeOff = `intentional business behavior`（背书后票据所有权转移，仅可注销）；(c) COLLECTION_PENDING/DISHONORED 不过账 = 过账侧语义，非状态机轴范畴；(d) M0.2 §3.5 finance M4.8/M4.9 标「既有测试：TestErpFinNotes*StateMachine」**与实仓一致**（无测试名漂移，区别于 M4.1/M4.4/M4.6）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移（11 writer 全集分类 + ENDORSED 边界 + 不对称 writer + 不过账语义均裁定并落入 owner doc/计划）
- [ ] owner doc §对象三/四 矩阵与 dict/Bean/代码一致；交叉引用 treasury.md 正确

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（draft pending M4 gate）（`ses_006be9154ffeFvxQq1d4j0QBGv`，新会话零信任实仓复核 + 穷尽 writer 扫描）— 零 BLOCKER / 零 MAJOR / 3 MINOR。全部 load-bearing 基线 CLAIM CONFIRMED：实体/dict/字段/writer 行号（Receivable 7 + Payable 4 全部精确）、writer 放置不对称（collect/dishonor per-mutation，其余 facade do*）、isTerminal 两实体均 = {HONORED,DISHONORED,WRITE_OFF}、ENDORSED 非终态仅 writeOff 出边（穷尽守卫核验）、错误码 + 参数、11 per-mutation Processor 注册、owner doc 缺口、COLLECTION_PENDING/DISHONORED 不过账、授信 Payable-only、ArApItem Receivable-only。3 MINOR：(M-1) Receivable 测试 @Test 计数 11→12（已修正）；(M-2) ErpFinConstants 行段 :255-262/:264-268 off-by-one（已修正为 :256-262/:265-268）；(M-3) receive 守卫有意收窄（validateNotTerminal loose→assertCanReceive null/initial）未显式记录（已在 Phase 2 补注，零回归风险——实仓零路径从非 initial 态 receive）。anti-slack 全 PASS（无禁用词，完整仓库验证在 Closure Gates 非阶段退出）。草案审查收敛。
- **M4 plan-first 人工/owner-doc 门控状态：pending**（§11.2 M4 (i)）。本计划触及受保护票据过账 + 授信行为。草案审查虽已收敛（acceptable as draft），但在人工/owner-doc 确认「以行为保持的单轴矩阵集中化方式迁移两轴、过账/授信/红冲闭环完整保留、posted 不入轴、writer 放置不对称仅改守卫委托不改物理位置、receive 守卫有意收窄零回归」前保持 `Plan Status: draft`（对齐 M4.1/M4.2 plan-first 先例）。确认后在此追加记录（日期 + 批准范围），方可转 `active`。
- Independent draft review iteration 2: `hold as draft`（mission-driver 草案审查）。格式/完备性/范围/结束证据全 PASS（必需节齐备、字段名正确、Phase 结构合法；Exit Criteria 可测；M4.8+M4.9 规则 14 合并理由充分无 scope creep；Closure Gates 证据完备）。零 BLOCKER / 零 MAJOR（格式与内容），唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控——属财务保护域硬停止（`project-context.md §AI 阻塞条件`），非审查时可自主解除的上游裁决（与 M4.1/M4.2/M4.29+M4.30 三姊妹计划保持 `draft` 一致）。按 mission-driver fix-forward 逃生舱保持 `Plan Status: draft` + Review Hold；approved marker 仅报告「审查已运行」。
- Independent draft review iteration 3: `hold as draft`（mission-driver 草案审查，复核）。逐项复核四清单：(1) 格式合规——必需节齐备（Title/Plan Status/Last Reviewed/Source/Related/Audit/Current Baseline/Goals/Non-Goals/Task Route 三字段/Infrastructure And Config Prereqs/Execution Plan 4 Phase 各含 Status/Targets/Skill/Item Types/Prereqs/items+Skill/Exit Criteria/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构合法；(2) 完备性——Exit Criteria 可测（grep 证据、测试命令、矩阵覆盖、错误码不变），阶段退出用 `-pl` 本地化验证、全仓验证正确归 Closure Gates（规则 7）；(3) 范围——M4.8+M4.9 规则 14 合并理由充分（同 owner doc/同结果表面/同验证路径），Non-Goals 显式边界，receive 守卫有意收窄已在迭代 1 (M-3) 记录为零回归经验性变更；(4) 结束证据——Closure Gates 定义具体证据（行为完成/文档对齐/指定验证命令/M4 门控记录/独立结束审计/文本一致性）。零 BLOCKER / 零 MAJOR（格式与内容），唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控（财务保护域硬停止，上游人工裁决，审查时不可自主解除）。按 mission-driver fix-forward 逃生舱保持 `Plan Status: draft` + Review Hold；approved marker 仅报告「审查已运行」（非「每计划 active」）。与 M4.1/M4.2/M4.29+M4.30 三姊妹计划状态一致。
- Independent draft review iteration 4: `hold as draft`（mission `2026-08-13-193118-mission-driver` 草案审查）。四维复核全 PASS，零 BLOCKER / 零 MAJOR（格式与内容）：(1) 格式——全部模板节齐备、字段名正确、Phase 结构合法、Item Types 合法；(2) 完备性——Exit Criteria 可测（grep 证据/测试命令/矩阵覆盖/错误码不变），Execution Plan 覆盖全部 checklist 项（2 Bean + 11 守卫接线 + 三层证据 + owner doc §对象三/四），阶段退出用 `-pl` 本地化验证、全仓验证正确归 Closure Gates（规则 7）；(3) 范围——M4.8+M4.9 规则 14 合并理由充分（同 finance treasury 子系统/同 owner doc/同结果表面/同验证路径），Non-Goals 显式排除 posted/过账编排/orm·dict·API/writer 物理位置统一/其余轴/CRUD 禁止/Delta 证明，无 scope creep；(4) 结束证据——Closure Gates 含具体验证命令（`mvn test -pl module-finance/erp-fin-service -am` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）+ M4 门控记录 + 独立结束审计 + 证据在文件。零信任实仓抽查 baseline CONFIRMED：`IErpFinCreditFacilityBiz` 注入于 `ErpFinNotesPayableProcessor:38`、`reserveCreditIfNeeded:69`/`releaseOccupiedCredit:76` + `ErpFinNotesPayableIssueProcessor:24`/`HonorProcessor:20`/`DishonorProcessor:20`/`WriteOffProcessor:20` 调用点（**授信占用/释放 = Payable 独有受保护维度**）；`statemachine/` 包确不存在（基线 line 53 属实）。**门控独立性裁定**：三姊妹（M4.1 Voucher/M4.2 Period/M4.29+M4.30 StockMove）的人工确认（2026-08-13）各以其本计划行为保持为批准范围，不含票据授信维度——本计划触及受保护票据过账（`NotesPostingDispatcher`→`FinPostingExecutor`）**且**独有授信占用/释放（`IErpFinCreditFacilityBiz`）受保护维度，门控与姊妹非同一裁定，不可由姊妹已解除推断本计划门控已解除。解除此 roadmap/mission 级财务保护域门控属「放宽保护区域」（`ai-autonomy-policy.md` + `project-context.md §AI 阻塞条件`），非审查子代理可自主解除。按 mission-driver fix-forward 逃生舱保持 `Plan Status: draft` + Review Hold（已在 front matter），不晋升 active；approved marker 仅报告「审查已运行」。

## Closure Gates

> 本计划含生产代码变更（2 Bean + Receivable 7 守卫 + Payable 4 守卫接线 + 测试 + owner doc 补章节），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（7+4 值保留），Compliance 基线预期无漂移（R5=0/R11=0）。

- [ ] 范围内行为完成（2 Bean + 11 守卫接线 + 三层证据；过账/红冲/授信/FX 完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [ ] 相关文档对齐（owner doc §对象三/四 新增 + 漂移 Decision 登记；路线图 M4.8/M4.9 done）
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 统一 collect/dishonor writer 物理位置至 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Receivable collect/dishonor 当前在 per-mutation Processor 直接 setStatus，其余在 facade do*。迁移仅改守卫委托 Bean，不改 writer 物理位置（避免行为变化）。统一放置属代码风格重构。
- Successor Required: no（归代码风格，非行为）

### COLLECTION_PENDING/DISHONORED 不过账语义纳入 Bean

- Classification: `watch-only residual`
- Why Not Blocking Closure: COLLECTION_PENDING（在途）/DISHONORED（终态重分类）不过账是过账侧（`NotesPostingDispatcher`）语义，非状态机轴范畴。Bean 只管 status 迁移矩阵。
- Successor Required: no（归过账侧）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD/生成路径写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
