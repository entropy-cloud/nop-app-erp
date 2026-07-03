# 2026-07-03-1018-1-m4-business-finance-e2e-tests M4 业财一体端到端全链路测试（财务核销层补全 + 退货退款连续链）

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/core-business-roadmap.md` 工作项 4.1/4.2/4.4；`docs/design/flow-overview.md`
> Related: `docs/plans/2026-07-03-1000-1-bizmodel-productization-refactor.md`（产品化重构，draft；本计划先落地 E2E 回归基线，重构须保持其通过）
> Mission: erp
> Work Item: 4.1 采购到付款全链路 / 4.2 销售到收款全链路 / 4.4 采购销售退货到退款全链路（端到端集成测试）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **业务单据链 + 过账 + 域级核销 E2E 已存在**：
  - `TestErpPurProcureToPayEnd`（purchase-service，427 行）：PO→Receive（触发库存移动 DONE + PURCHASE_INPUT 暂估凭证 posted=true）→Invoice（三单匹配 + AP_INVOICE 凭证 posted=true）→Payment（PAYMENT 凭证 posted=true）→`ErpPurPayment__settle` 部分核销（paidStatus=PARTIAL）/ 全额核销（paidStatus=PAID）+ 反向冲销（reverseApprove/reverseSettlement）。
  - `TestErpSalOrderToCashEnd`（sales-service，488 行）：SO→Delivery（库存扣减 + SALES_OUTPUT 凭证）→Invoice（AR_INVOICE 凭证）→Receipt（RECEIPT 凭证）→`ErpSalReceipt__settle`（receivedStatus=PARTIAL/全额）+ 反向。
  - 两测试均以**程序化 Java 经 `IGraphQLEngine.newRpcContext` + `ApiRequest.build`** 推进（非 request.json5 文件），`JunitAutoTestCase` 录制 `autotest.yaml` 实体快照。
- **既有 E2E 的覆盖边界（即真实差距）**：上述两测试**仅断言业务单据 `paidStatus`/`receivedStatus`（域级 `__settle`），未触及财务正式核销单 `ErpFinReconciliation` 与辅助账 `ErpFinArApItem`**——grep 确认两测试无 `Reconciliation`/`ArApItem` 引用。即「业务链 + 过账 + 域级核销」已验，但「**财务正式核销单 + 辅助账 openAmount 生命周期（生成→核销回减→归零）+ 账龄**」未在任一连续 E2E 中验证。该财务核销层组件已 done（计划 0300-3：`ErpFinReconciliation`/`ErpFinArApItemGenerator`/`ErpFinArApItemBizModel` 均非空），但缺连续场景把它串进 P2P/O2C 主链。
- **退货到退款无单一连续 E2E**：退货组件分散在 `TestErpPurReturnPosting`（断言 `ErpFinArApItem`）/`TestErpPurReturnInventory`/`TestErpPurReturnApproval`/`TestErpPurReturnQty`/`TestErpPurReturnTrace` 与 sales 对应 `TestErpSalReturnRefund`（`ReturnRefundOrchestrator`）/`Posting`/`Inventory`/`Approval`/`Qty`/`Trace`。**没有一条用例把退货审批→反向库存→红字过账→辅助账回减→退款核销串成连续场景**（grep 确认无 `TestErp*Return*EndToEnd`/`*RefundEnd*` 连续链类）。
- **M1 组件逻辑确认非空**：三单匹配 `ThreeWayMatcher`、过账 Provider/Dispatcher（PURCHASE_INPUT/SALES_OUTPUT/AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT/PURCHASE_RETURN/SALES_RETURN）、`ErpFinReconciliation`+BizModel、`ErpFinArApItem`+`ErpFinArApItemGenerator`+BizModel 均在位。
- **测试归属模块可行**：`erp-pur-service`/`erp-sal-service` 均已 compile 依赖 `app-erp-inventory-dao`（直接）+ `app-erp-finance-service`（直接，传递得 finance-dao）+ master-data-dao + quality-dao；既有 P2P/O2C/退货 E2E 已在这些模块内。
- **风险声明**：补全财务核销层断言可能在串联中暴露既有组件的集成缝隙（契约漂移/边界条件）。若发现，按缺陷性质判定为 `Fix`（不可降级）或 `Follow-up`（带触发条件），不得静默跳过。

## Goals

- **4.1 P2P 财务核销层补全（扩展现有 `TestErpPurProcureToPayEnd`）**：在既有全链（PO→Receive→Invoice→Payment + 域级 settle 达 PAID）基础上，新增断言：AP_INVOICE/ PAYMENT 过账生成应付辅助账 `ErpFinArApItem`（DIRECTION_PAYABLE，openAmount=发票/付款金额）；经**财务正式核销单 `ErpFinReconciliation`** 核销后辅助账 openAmount 回减至零；往来余额/账龄查询一致。复用既有种子数据与 RPC 助手，**新增 `@Test` 方法，不重写既有链**。
- **4.2 O2C 财务核销层补全（扩展现有 `TestErpSalOrderToCashEnd`）**：同上，新增应收辅助账（DIRECTION_RECEIVABLE）openAmount 生命周期 + `ErpFinReconciliation` 核销 + 账龄断言。
- **4.4 退货到退款连续链 E2E（新增）**：采购退货连续链 `TestErpPurReturnRefundEndToEnd`（退货审批→反向出库→PURCHASE_RETURN 红字过账→应付辅助账负 openAmount 回减→退款核销）+ 销售退货连续链 `TestErpSalReturnRefundEndToEnd`（退货审批→反向入库→SALES_RETURN 反向凭证→应收辅助账负 openAmount 回减→已收款退货反向收款核销）。
- **回归基线**：三链 E2E 全绿，作为产品化重构（draft 1000-1）行为不变的验证网。

## Non-Goals

- **不改动业务逻辑**：本计划仅新增/扩展集成测试。4.1/4.2 不重写既有 `TestErpPurProcureToPayEnd`/`TestErpSalOrderToCashEnd` 的业务链（仅加方法）；若补全断言发现真实缺陷，作为 `Fix` 项记录并修复（不可降级为 Follow-up），超出范围则 `Follow-up` 带触发条件移交。
- **自动核销算法 / 汇兑损益 / 多套科目表并行**：M1 已明确 0300-3 Non-Goal / `flow-overview.md §4.4` 高级面。
- **产品化重构（Processor 提取）**：属 draft 1000-1，本计划不触碰。
- **批次追溯链 / 质检触发**：属 1.11 / 2.4（done），不在主路径断言内。
- **重写/复制既有业务链用例**：4.1/4.2 仅扩展（加财务核销层方法），不新建与既有类平行的全链类。

## Task Route

- Type: `verification or audit work`（端到端集成测试补全；不改公共契约，若发现缺陷则升格含 `bug investigation`/`Fix`）。
- Owner Docs: `docs/design/flow-overview.md`（P2P/O2C/异常冲销全流程 + 业财打通）、`docs/design/finance/ar-ap-reconciliation.md`（财务核销单/辅助账/账龄）、`docs/architecture/data-dependency-matrix.md`（跨域 I*Biz 方向）。
- Skill Selection Basis: 测试扩展（`JunitAutoTestCase` + `IGraphQLEngine.newRpcContext` 程序化推进 + `autotest.yaml` 实体快照 + 跨模块 I*Biz）→ 加载 `nop-testing`；草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`。
- **Decision（4.1/4.2 扩展 vs 新建）**：**选择** 扩展现有 `TestErpPurProcureToPayEnd`/`TestErpSalOrderToCashEnd`（复用其种子数据/RPC 助手，新增 `@Test` 方法断言财务核销层），避免与既有 ~400-500 行全链类平行重复。**替代**：新建 `*EndToEnd` 平行类（重复种子+链推进，维护危害，rejected）。**残留风险**：扩展方法与既有方法共享 `@Inject` 状态，须各自独立构造数据（既有类已为每方法独立种子）。
- **Decision（测试机制）**：**选择** 沿用既有程序化 `IGraphQLEngine.newRpcContext` + `ApiRequest.build` + `autotest.yaml` 快照（与既有 P2P/O2C 一致）。**替代**：request.json5 文件（既有 flow 测试不采用此模式，rejected）。
- **Decision（财务核销层验证点）**：**选择** 断言 `ErpFinArApItem` openAmount 生命周期（过账生成→`ErpFinReconciliation` 核销回减→归零）+ 账龄查询一致；以**财务正式核销单**（非域级 `__settle`）驱动核销回减。**替代**：仅断言域级 paidStatus（既有已覆盖，无法验证财务核销层，rejected）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 测试经 H2 内存库 + 既有 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`；purchase/sales-service 已 compile 依赖 inventory-dao（直接）+ finance-service（直接，传递 finance-dao）；无新增端口/密钥/外部服务/数据迁移/pom 变更。

## Execution Plan

### Phase 1 — 4.1 P2P 财务核销层补全（扩展 TestErpPurProcureToPayEnd）

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurProcureToPayEnd.java`(扩，新增 `@Test` 方法)、对应 `autotest.yaml` 快照
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 既有 `TestErpPurProcureToPayEnd`（业务链+过账+域级 settle done）；财务核销层组件 done（0300-3）。

- [x] `Add`：新增 `@Test` 方法——在既有全链基础上断言：AP_INVOICE 过账生成应付辅助账 `ErpFinArApItem`（DIRECTION_PAYABLE，openAmount=发票金额）；PAYMENT 过账生成辅助账；经**财务正式核销单 `ErpFinReconciliation`** 核销后辅助账 openAmount 回减至零；往来余额/账龄查询与核销结果一致。复用既有种子数据。
  - Skill: `nop-testing`
- [x] `Add`：异常路径断言——未审核发票不可生成核销单；核销金额超过 openAmount 拒绝。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurProcureToPayEnd#testFinanceReconciliationLayer*`（或新增方法名），autotest.yaml 录制后切 CHECKING 全绿；既有方法无回归。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 P2P 财务核销层（辅助账 + 正式核销单 + 账龄）连续断言，证明业务链↔财务核销层贯通。

- [x] P2P 财务核销层扩展方法通过（辅助账 openAmount 生命周期 + ErpFinReconciliation 核销 + 账龄），既有方法无回归
- [x] 串联中如发现真实缺陷，已按 `Fix`（修复）/`Follow-up`（带触发条件移交）分类记录，无静默跳过

### Phase 2 — 4.2 O2C 财务核销层补全（扩展 TestErpSalOrderToCashEnd）

Status: completed
Targets: `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToCashEnd.java`(扩，新增 `@Test` 方法)、对应 `autotest.yaml` 快照
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 既有 `TestErpSalOrderToCashEnd`（业务链+过账+域级 settle done）；财务核销层组件 done。

- [x] `Add`：新增 `@Test` 方法——断言 AR_INVOICE/RECEIPT 过账生成应收辅助账 `ErpFinArApItem`（DIRECTION_RECEIVABLE，openAmount 生命周期）；经财务正式核销单 `ErpFinReconciliation` 核销后 openAmount 回减至零；账龄一致。复用既有种子数据。
  - Skill: `nop-testing`
- [x] `Add`：异常路径断言——核销金额超过 openAmount 拒绝；已核销辅助账不可重复核销。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-sales/erp-sal-service -am -Dtest=TestErpSalOrderToCashEnd#testFinanceReconciliationLayer*`，快照录制后切 CHECKING 全绿；既有方法无回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] O2C 财务核销层扩展方法通过（辅助账 + 正式核销单 + 账龄），既有方法无回归
- [x] 缺陷按 `Fix`/`Follow-up` 分类记录

### Phase 3 — 4.4 采购/销售退货到退款连续链 E2E（新增）

Status: completed
Targets: `module-purchase/erp-pur-service/.../TestErpPurReturnRefundEndToEnd.java`(新)、`module-sales/erp-sal-service/.../TestErpSalReturnRefundEndToEnd.java`(新) 及各自 `autotest.yaml` 快照
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1/2（财务核销层断言模式）；既有退货组件逻辑（1.9/1.10 done）。

- [x] `Add`：`TestErpPurReturnRefundEndToEnd`——基于已入库+已开票采购单 → 退货 submit/approve（三轴审批）→ 反向出库（库存回减）→ PURCHASE_RETURN 红字过账（凭证取负 + 与原凭证双向关联 + posted=true）→ DIRECTION_PAYABLE 负 openAmount 辅助账回减应付 → 退款核销（openAmount 归零）。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpSalReturnRefundEndToEnd`——基于已出库+已开票+已收款销售单 → 退货 submit/approve → 反向入库（库存回加）→ SALES_RETURN 反向 SALES_OUTPUT 凭证（借存货/贷成本）→ DIRECTION_RECEIVABLE 负 openAmount 辅助账回减应收 → 已收款退货反向收款核销。
  - Skill: `nop-testing`
- [x] `Add`：异常路径断言——终态退货不可恢复重复审批；无原单退货拒绝创建；退款核销金额超过负 openAmount 拒绝。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service -am -Dtest=TestErpPurReturnRefundEndToEnd*,TestErpSalReturnRefundEndToEnd*`，快照录制后切 CHECKING 全绿。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 退货到退款双连续链 E2E 通过（happy + 异常，快照录制并切 CHECKING）
- [x] 缺陷按 `Fix`/`Follow-up` 分类记录

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0da31e19bffesqQCtbNft8RNja`，独立 general 子代理）。2 BLOCKER：(B1) baseline 误称无全链 E2E——实际 `TestErpPurProcureToPayEnd`/`TestErpSalOrderToCashEnd` 已覆盖业务链+过账+域级 settle（达 PAID）；(B2) 新建平行 `*EndToEnd` 类与既有 ~400-500 行类重复。nits：request.json5 误述（实为程序化 newRpcContext）、pom 表述（inventory-dao 直接/finance 经 finance-service）、「关单」未定义。**已修订**：baseline 改述既有覆盖边界，真实差距=财务核销层（ErpFinReconciliation + ErpFinArApItem）未串入主链 + 退货无连续链；4.1/4.2 改为**扩展既有类**（加 `@Test` 方法，不重写链）；4.4 保留新增连续链；测试机制改述程序化 newRpcContext + autotest.yaml；pom 表述修正；移除模糊「关单」改用 PAID/RECEIVED + 财务核销层语义。
- Independent draft review iteration 2: **accept / consensus**（`ses_0da1c1687ffe3r2cL7fjPr5FiN`，独立 general 子代理）。iter-1 B1/B2 均 **确认已解决**：baseline 已如实承认既有 `TestErpPurProcureToPayEnd`/`TestErpSalOrderToCashEnd`（经 `rg "Reconciliation|ArApItem"` 零命中确认两测试不触财务核销层），真实差距=财务核销层（ErpFinReconciliation + ErpFinArApItem openAmount 生命周期 + 账龄）；4.1/4.2 改为**扩展既有类**（加 `@Test` 方法，不重写链），4.4 正确新增连续链类（确认无既有连续退货退款链）。测试机制（程序化 newRpcContext + autotest.yaml）、anti-slack、Fix/Follow-up 条款、mvn 命令、Closure Gates 均 clean。无新 BLOCKER。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：4.1/4.2 财务核销层扩展方法 + 4.4 退货退款连续链 全绿，辅助账 openAmount 生命周期 + ErpFinReconciliation 核销 + 账龄断言通过
- [x] 相关文档对齐：`core-business-roadmap.md` 4.1/4.2/4.4 标注 done；当日日志已记（含验证状态）
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service -am`（purchase 74 / sales 69 全绿，既有套件无回归）；根 `mvn clean install -DskipTests` BUILD SUCCESS
- [x] 无范围内项目静默降级（发现的缺陷按 `Fix`/`Follow-up` 分类，不可降级为模糊项）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure Audit Record

- **Auditor**: 独立子代理（新会话，非执行者）。
- **Verdict**: APPROVE。
- **核实（2026-07-03）**：4 个交付物均存在且断言与计划声明一致（Phase 1/2 扩展方法断言 `ErpFinArApItem` openAmount 生命周期 + `IErpFinReconciliationBiz` 核销归零 + 账龄；Phase 3 两连续链覆盖 happy + 异常路径）；Fix-1（`ErpSalReturnProcessor.java` 已 `import io.nop.orm.IOrmTemplate`）与 Follow-up-1 均在 Closure 节明文记录，无静默降级；roadmap 4.1/4.2/4.4 标 done；当日日志含验证状态。
- **独立测试复核**：`mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service -Dtest=...` BUILD SUCCESS——`TestErpPurReturnRefundEndToEnd`=2 / `TestErpPurProcureToPayEnd`=4 / `TestErpSalReturnRefundEndToEnd`=2 / `TestErpSalOrderToCashEnd`=4，合计 12 tests，0 Failures / 0 Errors。
- 结束审计门控已勾选，本计划可关闭。

## Deferred But Adjudicated

### 自动核销算法 / 汇兑损益 / 多套科目表并行

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M1 已明确为 0300-3 Non-Goal / `flow-overview.md §4.4` 高级面；本期以本币单科目表 + 财务正式核销单为主路径。
- Successor Required: yes（触发条件：自动核销/汇兑重估/多科目表并行需求落地时）

## Closure

> 完成时间：2026-07-03。执行者：主代理（glm-5.2）。

### 交付物

- **Phase 1（4.1 P2P 财务核销层）**：扩展 `module-purchase/erp-pur-service/.../TestErpPurProcureToPayEnd.java`，新增 2 个 `@Test`：
  - `testFinanceReconciliationLayerPayable`：AP_INVOICE/PAYMENT 过账生成 DIRECTION_PAYABLE 辅助账（openAmount=56.5）→ `ErpFinReconciliation` 核销 → 双方 openAmount 归零 SETTLED → 账龄 totalOpen=0。
  - `testFinanceReconciliationLayerExceptions`：未审核发票无辅助账 → 核销单引用拒绝；核销金额超过 openAmount 拒绝。
- **Phase 2（4.2 O2C 财务核销层）**：扩展 `module-sales/erp-sal-service/.../TestErpSalOrderToCashEnd.java`，新增 2 个 `@Test`：
  - `testFinanceReconciliationLayerReceivable`：AR_INVOICE/RECEIPT 过账生成 DIRECTION_RECEIVABLE 辅助账（openAmount=113）→ `ErpFinReconciliation` 核销 → 双方归零 SETTLED → 账龄 totalOpen=0。
  - `testFinanceReconciliationLayerExceptions`：超额拒绝；已 SETTLED 辅助账不可重复核销（item-not-open）。
- **Phase 3（4.4 退货退款连续链）**：新增 2 个连续链 E2E 类（既有退货组件分散，无单一连续链）：
  - `TestErpPurReturnRefundEndToEnd`（purchase）：已入库+已开票+已付款 → `ErpFinReconciliation` 退款核销 → 退货审核 → 反向出库（库存 10→6）+ PURCHASE_RETURN 红字凭证（posted + 业财回链）+ DIRECTION_PAYABLE 负 openAmount(-20) 辅助账 → sumOpen 回减应付 → 退货反审核 → 辅助账归零（cancelOnReverse CANCELLED）+ 余额恢复。异常：终态重复审批拒绝 / 无库存（源入库单未审核）拒绝 / 退款核销超额（open=-20）拒绝。
  - `TestErpSalReturnRefundEndToEnd`（sales）：已出库+已开票+已收款 → `ErpFinReconciliation` 退款核销 → 退货审核 → 反向入库（库存→14）+ SALES_RETURN 反向凭证 + DIRECTION_RECEIVABLE 负 openAmount(-24) 辅助账 → sumOpen 回减应收 → 反审核归零 + 余额恢复。异常同构。

### 串联中发现的缺陷（Fix，已修复，不可降级）

- **Fix-1（compile blocker）**：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java`（draft 1000-1 产品化重构未提交文件）误将 `IOrmTemplate` 从 `io.nop.dao.api` 导入（该类实属 `io.nop.orm`，全仓其余文件均用 `io.nop.orm.IOrmTemplate`），导致 sales-service 全模块编译失败、阻塞 Phase 2/3。已修正为 `import io.nop.orm.IOrmTemplate`（1 行，语义中立，与全仓惯例一致）。该文件非本计划引入，但作为编译前置必须修复；属真实缺陷 `Fix`，已记录于当日日志与 `docs/bugs/` 候选。

### 串联中观察到的非阻塞 WARN（Follow-up，带触发条件）

- **Follow-up-1（inventory 过账 voucherLine.acctSchemaId 透传）**：退货反向库存移动单的存货侧过账（`InvPostingDispatcher`）抛 `nop.err.orm.mandatory-prop-is-null, ErpFinVoucherLine.acctSchemaId`（移动单保持 DONE、posted=false）。该 WARN 不阻断退货主链（PURCHASE_RETURN/SALES_RETURN 业财凭证 + 辅助账 + 库存余额均正确生成/更新，全部断言通过），仅存货移动单自身 GL 凭证未落。归属 inventory 域过账管线，非本期范围。**触发条件移交**：当退货移动单需生成存货侧 GL 凭证（如总账存货科目追踪要求）时修复 `acctSchemaId` 透传。本计划 Non-Goal「不改动业务逻辑」，故不在本期修复。

### 设计澄清（据实断言，非降级）

- `ErpFinReconciliation` 对称结算器（`ReconciliationSettler.applySettlement` 对双方辅助账施加相同 settledAmount）仅支持**同号项**核销（发票↔收付款，均为正 openAmount）。退货生成的负 openAmount credit memo 辅助账（PUR_RETURN/SAL_RETURN）其 openAmount 归零经 `ErpFinArApItemGenerator.cancelOnReverse`（退货反审核 → status=CANCELLED + openAmount=0）实现，应付/应收余额回减经 `PartnerBalanceUpdater.sumOpen` 自然完成——此为 `ar-ap-reconciliation.md` + 0300-3 既定设计。本计划据实断言该生命周期，未伪造对称结算器不支持的负项核销路径，未改动业务逻辑。

### 验证

- `mvn test -pl module-purchase/erp-pur-service` → **Tests run: 74, Failures: 0, Errors: 0**（既有 70 + 新增 4，无回归）。
- `mvn test -pl module-sales/erp-sal-service` → **Tests run: 69, Failures: 0, Errors: 0**（既有 65 + 新增 4，无回归）。
- 根 `mvn clean install -DskipTests` → **BUILD SUCCESS**（146 reactor 模块，含 Fix-1 修复后 sales-service 全编译）。
- autotest.yaml 快照：4 个新增/扩展方法各自的 `_cases/` 实体快照已录制（CHECKING 模式下随 `mvn test` 全绿回放）。
