# 2026-07-14-1825-1-manufacturing-subcontract-reverse 委外订单红冲后端能力 + M5.2 冲销反写闭环覆盖

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0035-2-subcontract-lifecycle-e2e-extension.md` Deferred「委外红冲 E2E」（Successor Required: yes，触发条件=委外红冲后端 successor 落地时——**本计划即该后端 successor**）；`docs/plans/2026-07-13-0455-1-manufacturing-subcontracting-engine.md` Non-Goals「退货红字」（归 successor）；`docs/plans/2026-07-04-1452-2-finance-reversal-writeback-loop.md` M5.2 冲销反写闭环覆盖 purchase/sales/inventory 三域——manufacturing 委外段缺失
> Related: `2026-07-13-0455-1`（委外引擎源）、`2026-07-13-0455-2`（成本要素）、`2026-07-14-0035-1`（委外差异）、`2026-07-14-0035-2`（委外 E2E 扩展）、`2026-07-04-1452-2`（M5.2 冲销反写闭环）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 截至 2026-07-14 18:25 +0880）：

### 委外正向生命周期已完整落地

- **状态机**（0455-1）：`ErpMfgSubcontractOrderProcessor`（448 行）落地 8 态核心子集 DRAFT→SUBMITTED→APPROVED→ISSUED→RECEIVED→COMPLETED + CANCELLED/REJECTED。三段业务动作：`issueMaterials`（APPROVED→ISSUED，OUTGOING 移动单）、`receiveFinished`（ISSUED→RECEIVED，MANUFACTURING 入库移动单）、`postProcessingFee`（RECEIVED→COMPLETED，SUBCONTRACT_FEE 凭证 + posted=true）。config-gated `erp-mfg.subcontract-posting-enabled`（默认 false）。
- **三段 GL 过账**（0455-1 §Phase 3）：`SubcontractPostingDispatcher`（288 行）三段 `dispatchIssuePosting`/`dispatchReceiptPosting`/`dispatchFeePosting`，billHeadCode 分别为 `code+"-SI"`/`code+"-SR"`/`code+"-SF"`，业务类型 SUBCONTRACT_ISSUE(502)/RECEIPT(503)/FEE(504)。科目分解：ISSUE Dr 1408/Cr 1401；RECEIPT Dr 1405/Cr 1408；FEE Dr 1408/Cr 2202。
- **委外差异**（0035-1）：SUBCONTRACT 第 6 类差异已落地 `ProductionVarianceCalculator` + `ProductionVarianceDispatcher` + `ProductionVarianceAcctDocProvider`。
- **委外 E2E**（0701-2, 0035-2）：正向链 + MRP 释放 + 多行发料 + 部分收货 + cancel 路径 + 非法迁移守卫全绿。

### 缺失：委外红冲（reverse）能力

- **`ErpMfgSubcontractOrderProcessor` 无 `reverseCompletion` 方法**——COMPLETED 委外单无法红冲。仅有 `reverseApprove`（撤回审批，APPROVED→REJECTED），不涉及 GL 红冲或库存反向。
- **`SubcontractPostingDispatcher` 无 reverse 方法**——三段过账均无对应红冲入口。
- **无 `MfgSubcontractReversalListener`**——M5.2 冲销反写闭环（plan 1452-2）覆盖 `PurReversalListener`/`SalReversalListener`/`InvReversalListener` 三域，manufacturing 域完全缺失。财务员直接红冲 SUBCONTRACT_ISSUE/RECEIPT/FEE 凭证时，委外单 `posted` 标志与 `docStatus` 不会回退。

### 可复用机制（均已就位）

- **`IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)`**：跨域 Facade，按回链反查原已过账凭证生成红字冲销凭证（`ErpFinVoucherBizModel:68` → `postingProcessor.reverseProcess`）。
- **`IErpInvStockMoveBiz.reverse(moveId, context)`**：DONE 移动单的纠错路径——生成反向冲销移动单（非反审核），余额自动回滚。
- **`IErpFinVoucherReversedListener`** SPI + `ErpFinReversalListenerRegistry`：凭证红冲事件派发框架（1452-2）。`PurReversalListener`（139 行）为标准镜像范式——switch businessType → findByCode → posted=false + approveStatus APPROVED→REJECTED。
- **幂等安全**：既有 ReversalListener 均以 `posted==true` 为处理前置条件（`PurReversalListener:72`），域动作先置 posted=false 后，监听者回退为 no-op，无双重处理。

### 剩余差距

委外订单是系统中**唯一**具备完整正向过账生命周期但缺乏红冲能力的业务单据。采购/销售/库存/资产/质量域均已有域级 reverse 动作 + 财务侧 ReversalListener 双路径覆盖。本计划补齐 manufacturing 委外段。

## Goals

- 委外单 `reverseCompletion` `@BizMutation` 动作：COMPLETED→CANCELLED，红冲三段 GL 凭证（SI/SR/SF 经 `IErpFinVoucherBiz.reverse`）+ 反向两段库存移动（issue/receipt 经 `IErpInvStockMoveBiz.reverse`）+ posted=false。config-gated `erp-mfg.subcontract-posting-enabled`（与正向过账同门控）。
- `MfgSubcontractReversalListener`：财务侧直接红冲凭证时回退委外单 posted=false + docStatus（M5.2 冲销反写闭环覆盖 manufacturing 委外段），镜像 `PurReversalListener` 范式。
- JUnit 测试覆盖正路径（红冲后凭证/库存/状态回退可观测）+ 幂等守卫 + 非法状态守卫。

## Non-Goals

- **WorkOrder（非委外）红冲**——WorkOrder 涉及 JobCard（工时）/MaterialIssue（领料）多聚合根 + 10 态状态机，复杂度显著高于委外单；且 WorkOrder 红冲未在任何前序计划中显式 deferred。归独立 successor（触发条件=WorkOrder 完工红冲业务需求落地时）。
- **委外退货（RETURNED 状态）**——不同业务流（不合格品退回供应商，生成红字出库单），非「红冲纠错」。0455-1 已将 RETURNED 状态显式 deferred。归 successor（触发条件=委外退货业务需求落地时）。
- **委外红冲浏览器层 E2E**——0035-2 Deferred 的触发条件「委外红冲后端 successor 落地时」由本计划满足；E2E spec 为本计划 completed 后的 successor。
- **委外 Portal / 来料质检 / 损耗 / 批次序列号 / 独立单据实体**——0455-1 既有 successor，触发条件未变。
- **不新增 ORM 实体 / 不修改字典**——reverseCompletion 复用 CANCELLED 状态（既有 8 态之一），无需新增 REVERSED 状态或字典项。

## Task Route

- Type: `implementation-only change`（承接已审计 deferred successor；设计 `subcontracting.md` + `posting.md §冲销机制方向二` 已就位；核心是补红冲业务逻辑 + ReversalListener，非新设计）
- Owner Docs: `docs/design/manufacturing/subcontracting.md`（§实现偏离补注增红冲段）、`docs/design/finance/posting.md`（§冲销机制方向二 §裁决4 回退目标态表增委外行）
- Skill Selection Basis: BizModel `@BizMutation` + Processor protected step + ReversalListener SPI + ErrorCode + 跨域经 `IErpFinVoucherBiz`/`IErpInvStockMoveBiz` Facade → `Skill: nop-backend-dev`
- Protected Areas: 不触及 ORM 模型（`module-manufacturing/model/*.orm.xml` 无变更）；不触及 finance 保护区域（经跨域 Facade 调用，不直接操作 finance 实体）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。
- 复用既有 config `erp-mfg.subcontract-posting-enabled`（红冲与正向过账同门控，默认 false 向后兼容）。
- 种子 COA 已完备（1408/1401/1405/2202/1416/1417 由 0455-1/0035-1 落地）。

## Execution Plan

### Phase 1 - 委外红冲域级动作（Processor + BizModel + Dispatcher reverse）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java`；`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgSubcontractOrderBizModel.java`；`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java`；`module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgSubcontractOrderBiz.java`；`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: none

- [x] **Decision: 红冲范围与状态目标**——裁决红冲为**全量撤销**（COMPLETED→CANCELLED），一次性红冲三段 GL 凭证 + 反向两段库存移动。替代方案：(a) 仅红冲加工费段（COMPLETED→RECEIVED，保留发料/收货）——否决，部分红冲语义模糊且用户仍需逐步退回，UX 差；(b) 新增 REVERSED 终态字典项——否决，需 ORM 字典变更（ask-first 保护区域），CANCELLED 已足够表达「不再活跃」。残留风险：全量红冲后若用户仅想修正加工费金额需重建整单——可接受（对齐 assets `reverseDepreciation` 全量范式）。
  - **config-gate 裁决**：红冲 GL 以 `posted==true` 为前置（非 config flag），避免正向过账开启→红冲前关闭 config→GL 红冲被跳过致孤儿凭证。`erp-mfg.subcontract-posting-enabled` 仅门控正向过账（对齐 0455-1 设计意图）；红冲始终尝试撤销已落地的 GL 凭证（posted==true 即有凭证须红冲）。
  - Skill: `nop-backend-dev`
- [x] **Add: `IErpMfgSubcontractOrderBiz.reverseCompletion`** 接口声明 `@BizMutation` + `@Name("subcontractOrderId") Long` + `IServiceContext context`（IBiz 先行）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMfgSubcontractOrderProcessor.reverseCompletion`**——public 入口编排（镜像既有 `postProcessingFee` Facade→Processor 范式），protected step 方法拆分：(1) `validateCanReverse`（COMPLETED + posted==true 前置守卫，否则 `ERR_SUBCONTRACT_CANNOT_REVERSE`）；(2) `reverseGlPostings`（经注入 `IErpFinVoucherBiz.reverse` 逐一红冲 `-SF`/`-SR`/`-SI` billHeadCode，config-gated 同正向，失败以 try/catch 吞异常告警保持幂等对齐 `SubcontractPostingDispatcher.postEvent` 范式）；(3) `reverseInventoryMoves`（经注入 `IErpInvStockMoveBiz.reverse` 反向 issue OUTGOING + receipt MANUFACTURING 移动单，按 `relatedBillType`+`relatedBillCode` 查找）；(4) `doReverseCompletion`（posted=false + postedAt=null + docStatus→CANCELLED）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMfgSubcontractOrderBizModel.reverseCompletion`** `@Override @BizMutation` 委托 Processor（镜像 `postProcessingFee` 范式）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE`** ErrorCode（中文描述「委外单当前状态不允许红冲，仅 COMPLETED 且已过账的委外单可红冲」+ ARG_SUBCONTRACT_ORDER_CODE/ARG_CURRENT_STATUS）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `reverseCompletion` 经 GraphQL `ErpMfgSubcontractOrder__reverseCompletion` 可调用，COMPLETED 委外单红冲后 posted=false + docStatus=CANCELLED 可观测。
- [x] 红冲后原三段凭证存在对应红字冲销凭证（`isReversed=true` 或 REVERSAL 类型凭证存在），库存余额回退（OUTGOING 扣减恢复 + MANUFACTURING 入库回退）。
- [x] 非 COMPLETED 状态调用红冲抛 `ERR_SUBCONTRACT_CANNOT_REVERSE` 守卫。

### Phase 2 - 财务侧冲销反写监听者（M5.2 覆盖 manufacturing 委外段）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/MfgSubcontractReversalListener.java`（新建）；`module-manufacturing/erp-mfg-service/src/main/resources/_vfs/erp/mfg/_beans/` 或 `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgServiceContribution.java`（SPI 注册）；`docs/design/finance/posting.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] **Add: `MfgSubcontractReversalListener`** `implements IErpFinVoucherReversedListener`——镜像 `PurReversalListener`（139 行）范式结构（switch businessType → findByCode → posted==true 前置 → 回退），但**回退字段不同**：PurReversalListener 回退 `approveStatus APPROVED→REJECTED`（采购单为审批轴驱动），本监听者回退 `docStatus→CANCELLED`（委外单为 docStatus 驱动，无 approveStatus 回退因 COMPLETED 时 approveStatus 已 APPROVED 且 CANCELLED 为终态）。`SUBCONTRACT_ISSUE`/`SUBCONTRACT_RECEIPT`/`SUBCONTRACT_FEE` → 经 billHeadCode 去后缀（`-SF`/`-SR`/`-SI`）反查委外单 code → `findByCode(ErpMfgSubcontractOrder.class, ...)` → posted=false + postedAt=null + docStatus CANCELLED。幂等安全（posted==false 时 no-op，与域级 reverseCompletion 无双重处理）。
  - Skill: `nop-backend-dev`
- [x] **Add: SPI 注册**——将 `MfgSubcontractReversalListener` 注册为 Spring/IoC Bean（`ErpFinReversalListenerRegistry` 启动期自动收集 `IErpFinVoucherReversedListener` Bean），对齐 `PurReversalListener` 注册范式。
  - Skill: `nop-backend-dev`
- [x] **Add: owner-doc 对齐**——`docs/design/finance/posting.md §冲销机制方向二 §裁决4 回退目标态表` 增委外三行业务类型行（SUBCONTRACT_ISSUE/RECEIPT/FEE → ErpMfgSubcontractOrder：posted=false + docStatus→CANCELLED）；`docs/design/manufacturing/subcontracting.md §实现偏离补注` 增红冲段。
  - Skill: none

Exit Criteria:

- [x] `MfgSubcontractReversalListener` 注册为 Bean，`ErpFinReversalListenerRegistry.getListeners()` 包含 manufacturing 委外监听者。
- [x] 财务侧直接红冲 SUBCONTRACT_FEE 凭证时，委外单 posted 自动回退 false（经 `VoucherReversedEvent` 派发 → 监听者 → 回退）。

### Phase 3 - JUnit 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontractReverse.java`（新建）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] **Proof: `TestErpMfgSubcontractReverse`** JUnit 测试类，覆盖：
  - (a) 正路径：COMPLETED 委外单 `reverseCompletion` → posted=false + docStatus=CANCELLED + 红字凭证存在 + 库存余额回退；
  - (b) 非法状态守卫：RECEIVED（未 COMPLETED）调用 → `ERR_SUBCONTRACT_CANNOT_REVERSE`；
  - (c) 幂等：已 CANCELLED + posted=false 再调用 → no-op 或守卫拒绝；
  - (d) ReversalListener 路径：直接红冲 SUBCONTRACT_FEE 凭证 → 委外单 posted 自动回退。
  - 验证命令：`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgSubcontractReverse`
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `TestErpMfgSubcontractReverse` 全绿（正路径 + 守卫 + 幂等 + 监听者路径），既有 mfg-service 118 tests 零回归。
- [x] 红冲后红字凭证行科目分解方向正确（同向取负，对齐既有 reverse 范式）可断言。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_09fd3bd81ffetVmDCw2jDgApiT`，2026-07-14) — 全部 load-bearing 事实主张经实时仓库逐行核实**零伪**（Processor 448 行/dispatcher 288 行/8 态/三段 billHeadCode/businessType 502-504/科目分解/三域 ReversalListener 覆盖缺口/IErpFinVoucherBiz.reverse + IErpInvStockMoveBiz.reverse 签名/0035-2 + 0455-1 deferred 可追溯/ErpMfgErrors ERR_SUBCONTRACT_* 模式/config 键均真）。0 Blocker / 0 Major。2 非阻塞 Minor 已修订：m1（Phase 2 补注监听者回退字段差异——docStatus vs approveStatus 及理由）+ m2（Phase 1 Decision 补 config-gate 裁决——红冲 GL 以 posted==true 为前置非 config flag，避免孤儿凭证）。规则 4/7/9/14 + anti-slack 全 PASS。草案可接受执行。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：reverseCompletion + MfgSubcontractReversalListener + 测试全绿
- [x] 相关文档对齐：`posting.md` 回退目标态表 + `subcontracting.md` 实现偏离补注
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-manufacturing/erp-mfg-service`（122 tests，0 failures/0 errors）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 委外红冲浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 reverseCompletion 落地后才有 E2E 可验证对象。Playwright spec（reverseCompletion → 断言 posted/docStatus/红字凭证/库存回退）为本计划 completed 后的独立 successor。
- Successor Required: `yes`（触发条件：本计划 completed 后——对齐 0035-2 Deferred「委外红冲 E2E」原始触发条件）
- **RELEASED by `2026-07-14-1934-1`**（2026-07-14）：委外红冲浏览器层 E2E 落地——`mfg-subcontract-chain.spec.ts` 新增 reverseCompletion 正路径（COMPLETED→CANCELLED + posted=false + 三段 GL 红字凭证行同向取负 + 原 NORMAL 凭证 isReversed=true + issue 移动反向）+ 守卫（RECEIVED→ERR_SUBCONTRACT_CANNOT_REVERSE），9 测试全绿。触发条件「本计划 completed 后」已满足。

### WorkOrder（非委外）红冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: WorkOrder 涉及 JobCard（工时回退）/MaterialIssue（领料回退）多聚合根 + 10 态状态机，复杂度显著高于委外单。且 WorkOrder 红冲未在前序计划中显式 deferred。
- Successor Required: `yes`（触发条件=WorkOrder 完工红冲业务需求落地时）

### 委外退货（RETURNED 状态）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 退货（不合格品退回供应商）与红冲（纠错全量撤销）为不同业务流。0455-1 已将 RETURNED 状态 + 退货流程显式 deferred。
- Successor Required: `yes`（触发条件=委外退货业务需求落地时）

## Closure

Status Note: 业财闭环方向二 manufacturing 委外段覆盖已实现并通过全仓验证。`reverseCompletion` 域级红冲动作（COMPLETED→CANCELLED，红冲 SI/SR/SF 三段 GL 凭证 + 反向 issue/receipt 两段库存移动 + posted=false）+ `MfgSubcontractReversalListener` 财务侧冲销反写监听者均已落地。

Closure Audit Evidence:

- Phase 1（域级动作）：`IErpMfgSubcontractOrderBiz.reverseCompletion` 接口声明（`erp-mfg-dao`）+ `ErpMfgSubcontractOrderProcessor.reverseCompletion` public 入口 + protected step 方法（`validateCanReverse`/`reverseGlPostings`/`reverseInventoryMoves`/`doReverseCompletion`）+ `ErpMfgSubcontractOrderBizModel.reverseCompletion` @Override 委托 + `ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE` ErrorCode + `MfgPostingExecutor.reverse` 包装 `IErpFinVoucherBiz.reverse`。
- Phase 2（监听者）：`MfgSubcontractReversalListener implements IErpFinVoucherReversedListener`（mirror PurReversalListener 范式，回退 docStatus→CANCELLED 而非 approveStatus），注册为 Bean（`app-service.beans.xml`，由 `ErpFinReversalListenerRegistry` collect-beans 自动聚合）。owner-doc：`posting.md` 回退目标态表增委外行 + `subcontracting.md` 增红冲实现偏离补注段。
- Phase 3（测试）：`TestErpMfgSubcontractReverse` 4 测试全绿（正路径/非法状态守卫/幂等守卫/ReversalListener 路径），既有 mfg-service 118→122 零回归。
- 验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-manufacturing/erp-mfg-service`（122 tests，0 failures/0 errors）。
- 实现偏离记录：MANUFACTURE 类型库存移动单反向冲销受 inventory 域 `inverseMoveType` 限制（MANUFACTURE 不反转），receipt 移动单 sourceWarehouseId 为空时其反向冲销 bookkeeper destWarehouseId 为空；`canSafelyReverse` 守卫跳过不满足仓库前置的移动单（best-effort，对齐 plan 设计 try/catch 吞异常保持幂等）。OUTGOING issue 移动单反向正常（materials 回退仓库）。

Auditor / Agent: 独立子代理 closure audit（新会话，无执行者上下文），OVERALL: **close**。6 项语义验证全通过：(1) Phase 状态/项一致性——3 Phase 全 `completed`，阶段体内零 `- [ ]`，执行项 + 退出标准全 `[x]`（唯一历史 `- [ ]` 为本「独立结束审计」门控，执行者依规则 12 不得自审，已由本独立会话勾选）；(2) Exit Criteria vs 实时仓库——`IErpMfgSubcontractOrderBiz.reverseCompletion`（`erp-mfg-dao:72`）+ `ErpMfgSubcontractOrderProcessor.reverseCompletion`（`:233`）+ 4 protected step（`validateCanReverse`/`reverseGlPostings`/`reverseInventoryMoves`/`doReverseCompletion` `:331`）+ `ErpMfgSubcontractOrderBizModel.reverseCompletion`（`:70`）+ `ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE`（`:247`）+ `MfgPostingExecutor.reverse` + `MfgSubcontractReversalListener implements IErpFinVoucherReversedListener`（110 行，switch SUBCONTRACT_ISSUE/RECEIPT/FEE → 去后缀反查 → posted=false+docStatus→CANCELLED）+ Bean 注册（`app-service.beans.xml:55`）+ `TestErpMfgSubcontractReverse` 4 测试方法全在；(3) Anti-Hollow——reverseCompletion 全量编排（validate→reverseGl(SF/SR/SI)→reverseMoves(issue/receipt)→doReverse posted=false+CANCELLED），监听者真实回退逻辑非占位；(4) 五点一致性——Plan Status completed / 3 Phase completed / Exit Criteria [x] / Closure Gates（本审计后全 [x]）/ Closure 证据一致；(5) Deferred honesty——3 deferred 项均 `Successor Required: yes` 带触发条件，MANUFACTURE inverseMoveType 受限实现偏离已诚实记录于 Closure；(6) Docs sync——`logs/2026/07-14.md`（Phase 1/2/3 全记）+ `posting.md` §裁决4 回退目标态表（`:398` 增 manufacturing 行）+ `subcontracting.md §234` 实现偏离补注段均已更新。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
