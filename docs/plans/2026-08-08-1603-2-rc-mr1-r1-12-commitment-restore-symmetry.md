# 2026-08-08-1603-2-rc-mr1-r1-12-commitment-restore-symmetry RC-R1.12 — 跨域承付恢复对称性（P1-MA2-083 reuse 重开，MR1 第一批纯预授权 + Q4 收敛性会计类）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.12（MR1 第一批纯预授权：跨域承付恢复对称性——invoice/return reverseApprove + cancel Processor 增 `budgetCommitmentBiz.commit()` 恢复承付，P1-MA2-083 reuse 重开，Q4 收敛性会计类 + 独立 plan-audit 义务）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.12 行 + `docs/audits/arm-index.md` P1-MA2-083 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1/G10（RC-R1.12 = Q4 收敛性会计类，纯 Processor 调既有 commit() 入口）
> Related: `docs/design/finance/budget.md`（L2 §承付会计 §3 接入点）；`docs/design/purchase/use-cases.md`（L1 UC-PUR-01）；`docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（A4.2.31）+ `2026-08-07-2300-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`（A4.2.45 运行时确认）；`docs/plans/2026-08-08-1154-3-rc-mr1-r1-10-pur-requisition-multi-supplier-split.md`（同批计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-MA2-083（arm-index 行，reuse 重开）**：AP/AR 发票冲销后 commitment 未恢复致余量永久偏移（跨冲销一致性缺口）。系统不对称：invoice approve → `release` 承付，invoice reverseApprove/cancel → AP ACTUAL 回退但 commitment 保持已释放；`availableAmount = budget − actual − commitment` 显示 actual 减少但 commitment 仍为零，预算余量看起来"释放"了，可能允许超预算放行新订单。audit-remediation R1.27 曾选方案 B Deferred（owner doc 标注）关闭；requirement-compliance Q4=(a)（P1 必须实现、禁方案 B、无例外通道）下**重开**经 MR1 实现方案 A。**P1 非 P0**：config-gated 默认关闭（非默认活跃路径）+ 不破坏活跃数据。
- **A4.2.31 运行时确认**：`ErpPurInvoiceReverseApproveProcessor.reverseApprove:22-37` 零 budgetCommitmentBiz.commit()（仅 postingDispatcher.reverse 红冲 AP + posted=false + doReverseApprove）；**commit() 调用方 census 全域仅 `ErpPurOrderProcessor:208`**（commit-on-order-approve）；reverse/cancel Processor 全集零 commit()；config-gated `erp-fin.budget-commitment-enabled` 默认 false + 零生产 application.yaml override → 非默认活跃。正向 `runCommitmentReleaseOnInvoiceApproveHook:273-292`（`ErpPurInvoiceProcessor`）已实现调 `budgetCommitmentBiz.release:283`（config-gated）→ approve→release vs reverseApprove→不恢复**运行时不对称确认**。
- **A4.2.45 运行时确认（退货侧扩展）**：`ErpPurReturnApproveProcessor.approve:60` 调 `runCommitmentReleaseOnReturnHook`（`ErpPurReturnProcessor:281-297`）release（config-gated `erp-fin.commitment-release-on-return` 默认 false）；`ErpPurReturnReverseApproveProcessor.reverseApprove:23-37` + `ErpPurReturnCancelProcessor.cancel:23-33` 零 budgetCommitmentBiz.commit()（不对称）。与 A4.2.31 invoice 侧同型，MR1 修复行协同覆盖 Return Processor。
- **实仓（HEAD 核查）**：
  - `IErpFinBudgetCommitmentBiz`（finance-dao 跨层契约面）：`commit(sourceBillType, sourceBillCode, subjectId, costCenterId, periodId, amount, context)` + `release` + `releaseIfPresent`——**commit 入口现成**（本行只做调用方接线，不改 SPI 定义）。实现 `ErpFinBudgetCommitmentBizModel`：`commit` config-gated（`erp-fin.budget-commitment-enabled`）+ 参数守卫（sourceBillCode/subjectId/amount 缺失或 ≤0 → 返回 null）+ `resolvePeriodId`/`resolveOrgAndSchema` 现成。**commit 无去重**：每次调用 `commitmentVoucherGenerator.generateCommitment` 生成新凭证（`:73-74`）——**恢复路径必须自守前置条件，否则重复占用预算**（见 Goals/Decision）。
  - **恢复语义设计基础**：正向 release 是**全额释放语义**（`reverseCommitment` 按 billCode 全额红冲，无 amount 入参；budget.md:262-265）。恢复 = 对关联 PO 调 `commit()` 重新生成 COMMITMENT 凭证（金额取 PO `totalAmountWithTax`，期间按 PO businessDate 解析——与 `ErpPurOrderProcessor.runCommitmentCommitHook:197-211` 同构）。**部分冲销/跨期语义 successor 不动**（budget.md:264-265/272 既有声明；roadmap RC-R1.12 行范围 = invoice/return reverseApprove + cancel 恢复，不含比例恢复——与 arm-index 修复方向注记「处理部分冲销/跨期语义」的差异以 roadmap 行为准，见 Non-Goals）。
  - **关联单据反查现成**：`ErpPurInvoiceProcessor.resolveLinkedOrderCodes`（invoiceLine.receiveLineId → receiveLine → receive.orderId → order.code 集合，`:300+` 已实现，release hook 同源复用）；`ErpPurReturnProcessor.resolvePurchaseOrderCode`（return.receiveId → receive.orderId → order.code，`:300-307` 已实现，release-on-return hook 同源复用）。
  - **需要恢复的 Processor 集合**：`ErpPurInvoiceReverseApproveProcessor`、`ErpPurInvoiceCancelProcessor`、`ErpPurReturnReverseApproveProcessor`、`ErpPurReturnCancelProcessor`（4 个 per-mutation Processor；均走 `Abstract*Processor` 派生 + 调 `processor.xxx` facade 方法模式，protected step 可派生覆盖）。
  - **前置条件不对称（恢复守卫设计输入）**：`ErpPurInvoiceCancelProcessor.cancel:24-38` / `ErpPurReturnCancelProcessor.cancel:23-33` 的 `validateTransitionForCancel` 仅守卫 docStatus，**允许取消从未 APPROVED 的单据**（此时正向 release 从未发生，恢复将产生幽灵承付凭证——双占用）；`reverseApprove` 的 `validateTransitionForReverseApprove` 已守卫仅 APPROVED 可反审核（正向 release 必已发生）。**恢复前置 = 单据曾 APPROVED**（对称于"仅对 approve 已 release 的路径恢复"）。
  - **return 侧子开关**：release-on-return 受 `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN`（`ErpPurReturnProcessor:282-284`）独立门控——**恢复须与释放同开关**（子开关 OFF 时 return approve 未释放，reverseApprove/cancel 不得恢复，否则幽灵承付）。
  - **PO 终态守卫**：`ErpPurOrderProcessor.validateTransitionForCancel:153-157` 仅守卫 docStatus，无"已关联发票"守卫；PO cancel 路径 `runCommitmentReleaseHook:222-233` release 无凭证时静默。**PO CANCELLED 后恢复 = 无未来释放路径的永久泄漏**——恢复 hook 须跳过 docStatus=CANCELLED 的 PO。
  - **config 键**：`ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED = "erp-fin.budget-commitment-enabled"`（`:419`）+ `CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE`（承付科目 code 配置）+ `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN = "erp-fin.commitment-release-on-return"`（`:426`，退货 release 子开关）；`CONFIG_BUDGET_PURCHASE_EXPENSE_SUBJECT_CODE` 供预算科目解析参照。
  - **测试基线**：`TestErpPurOrderCommitment`（testApproveTriggersCommitmentVoucherWhenEnabled / testReverseApproveReversesCommitmentVoucher / testCancelReversesCommitmentVoucher，`testConfigFile = "classpath:budget-commitment-test.yaml"` 开总开关 + 科目 1408）；`TestErpPurReturnCommitmentRelease`（testReturnApproveReleasesCommitmentWhenEnabled / testPartialReturnFullReleaseSemantics）；`TestErpSalOrderCommitment`（sales 侧 3 测试）。**均无「invoice/return 冲销后恢复承付」断言**——修复面测试缺口。
- **预授权判据**（第一批纯预授权 + Q4 收敛性会计批量授权）：纯 Processor 代码逻辑接线（调既有 `commit()` 入口 + 关联反查复用 + config-gated + 前置守卫），**不触及 VoucherFact/PostingProcessor 核心路径/ORM/删除**；按 roadmap 行标注「独立 plan-audit 义务」——本计划执行独立草案审查（标准流程）+ 不设 ask-first checkbox（Q4 批量授权覆盖，非越界项）。roadmap RC-R1.12 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceReverseApproveProcessor.java`；`.../ErpPurInvoiceCancelProcessor.java`；`.../ErpPurReturnReverseApproveProcessor.java`；`.../ErpPurReturnCancelProcessor.java`；`.../ErpPurInvoiceProcessor.java`（protected step 增补）；`.../ErpPurReturnProcessor.java`（同上）；`module-finance/erp-fin-dao/.../IErpFinBudgetCommitmentBiz.java`（**只读参照，不改**）；测试类 2 个新增或扩展。

## Goals

- **invoice 侧恢复**：`ErpPurInvoiceReverseApproveProcessor.reverseApprove` + `ErpPurInvoiceCancelProcessor.cancel` 在既有 posting 红冲/状态回退之后，对 invoice 关联 PO code 集合（复用 `resolveLinkedOrderCodes`）逐个调 `budgetCommitmentBiz.commit(PURCHASE_ORDER, poCode, subjectId, null, periodId, poAmount)` 恢复承付；config-gated（`erp-fin.budget-commitment-enabled`）。
- **return 侧恢复**：`ErpPurReturnReverseApproveProcessor.reverseApprove` + `ErpPurReturnCancelProcessor.cancel` 对 `resolvePurchaseOrderCode` 解析的 PO 调 `commit()` 恢复承付（config-gated 同 invoice 侧）。
- **恢复前置守卫（防幽灵承付/双占用，独立草案审查 Blocker 1/2 + Major 3 修订）**：
  1. **单据曾 APPROVED 守卫**：仅当被冲销单据（invoice/return）当前 approveStatus == APPROVED 时恢复（对称于"正向 release 仅在 approve 路径发生"）；从未 APPROVED 的 cancel 不恢复（`ErpPurInvoiceCancelProcessor`/`ErpPurReturnCancelProcessor` 在状态回退前读取原始 approveStatus 判定，reverseApprove 路径由 `validateTransitionForReverseApprove` 天然保证）。
  2. **return 侧与释放同开关**：return 恢复 hook 同时受总开关 `CONFIG_BUDGET_COMMITMENT_ENABLED` **与** 子开关 `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN` 门控（子开关 OFF = 正向从未 release = 不得恢复；与 invoice 侧 release 只依赖总开关对称）。
  3. **PO 终态守卫**：关联 PO docStatus == CANCELLED **或 approveStatus != APPROVED**（REJECTED 等）时跳过恢复（非 APPROVED PO 无活跃承付义务——其承付经 order reverseApprove/cancel 的 release 路径已释放，恢复 = 幽灵承付双占用；PO 重新 approve 时 commit-on-order-approve 会重新承付）。
- **对称语义**：恢复金额 = 关联 PO 的 `totalAmountWithTax`（与正向 commit-on-order-approve 同源），期间 = PO `businessDate` 解析——全量恢复与既有全额释放语义对称（部分冲销/跨期/比例恢复归既有 successor 声明，不扩范围）。
- **容错语义（Decision）**：恢复 hook 的异常容错范围显式裁决——`commit()` 参数守卫返回 null 视为 no-op；`NopException` 仅 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 类守卫错误静默跳过，**其他异常重新抛出**（对齐 `ErpPurInvoiceProcessor:285-290` 的精确容错范式而非 `ErpPurReturnProcessor:293-295` 的全吞——全吞会静默隐藏凭证生成失败 = 静默预算泄漏）。
- **owner doc 收敛**：`budget.md` §承付会计 §3「发票 reverseApprove/cancel 不恢复承付」的保守方向声明（方案 B 残留文本）更新为「冲销恢复」实现注记（收敛性会计：实现向 owner doc 契约收敛，非反向改契约段落——原声明本就是被 Q4=(a) 判定不成立的方案 B 文本）。
- **测试矩阵**：invoice reverseApprove/cancel 恢复、return reverseApprove/cancel 恢复（双开关）、config 关闭不恢复、**从未 APPROVED 取消不恢复**、**PO CANCELLED 不恢复**、**子开关 OFF 不恢复**、**跨路径单一活跃凭证断言**——分域测试全绿 + `_cases/` 快照。
- 回填 arm-index P1-MA2-083 → `done (RC-R1.12)` + roadmap RC-R1.12 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不改 SPI 契约**（`IErpFinBudgetCommitmentBiz` 定义不动——commit/release/releaseIfPresent 三个方法签名保持）。
- **不触及会计核心路径**（`CommitmentVoucherGenerator`/`ErpFinPostingProcessor`/VoucherFact 零修改；仅从 purchase Processor 调既有 commit() 入口）。
- **不做部分冲销/跨期/按比例恢复**（budget.md:264-265/272 既有 successor 声明维持；arm-index 修复方向注记「处理部分冲销/跨期语义」与 roadmap RC-R1.12 行范围（invoice/return reverseApprove+cancel 恢复）的差异**以 roadmap 行为准**，比例恢复归 successor）。
- **不做 sales 侧恢复**（sales 侧承付冲销恢复对称问题随 budget.md:272 声明归 successor——触发条件「sales 承付控制启用」未满足；本行范围 = roadmap RC-R1.12 行的 purchase invoice/return）。
- **不触 ORM 结构/数据删除**（零 ORM 变更）。
- **不改真相源契约段落**（use-cases/budget 需求契约段不动；仅更新 budget.md 中已失效的方案 B 声明文本为实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复 + Q4 收敛性会计类，独立 plan-audit 义务）
- Owner Docs: `docs/design/finance/budget.md`（§承付会计 §3 接入点/释放语义/恢复声明）+ `docs/design/purchase/use-cases.md`（L1 UC-PUR-01）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ A4.2.31/45 运行时报告
- Skill Selection Basis: 实现面 = Processor 接线 + protected step 模式 + config-gated 容错 + 前置守卫（`nop-backend-dev`：Processor 派生覆盖点、跨实体 IBiz 注入、ErrorCode 容错范式）；测试（`nop-testing`：JunitAutoTestCase + budget-commitment-test.yaml config 驱动 + 快照）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 复用既有测试 config：`module-purchase/erp-pur-service/src/test/resources/budget-commitment-test.yaml`（`erp-fin.budget-commitment-enabled: true` + 科目 1408）——测试类 `@NopTestConfig(testConfigFile="classpath:budget-commitment-test.yaml")` 驱动；return 子开关测试经 `assignConfigValue("erp-fin.commitment-release-on-return", ...)` 动态控制。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-purchase/erp-pur-service`。

## Execution Plan

### Phase 1 - invoice 侧承付恢复接线

Status: completed
Targets: `ErpPurInvoiceProcessor.java`；`ErpPurInvoiceReverseApproveProcessor.java`；`ErpPurInvoiceCancelProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **恢复 hook 落点**：选项 A（推荐）= 在 `ErpPurInvoiceProcessor` 新增 protected step `runCommitmentRestoreOnInvoiceReverseHook(ErpPurInvoice invoice, boolean wasApproved, IServiceContext context)`（与 `runCommitmentReleaseOnInvoiceApproveHook` 同源反查 `resolveLinkedOrderCodes` + config-gated + 前置守卫 + 精确容错），由 `ErpPurInvoiceReverseApproveProcessor.reverseApprove`（`wasApproved=true` 恒成立，reverseApprove 仅 APPROVED 可达）/ `ErpPurInvoiceCancelProcessor.cancel`（`wasApproved = 原始 approveStatus==APPROVED`）在 posting 红冲 + 状态回退后调用——单点实现、双 Processor 复用、派生可覆盖；选项 B = 两个 Processor 各自内联实现（重复代码 + 双覆盖点 + 守卫逻辑散落，弃）。备选与理由记录于本 Decision。
      - Skill: `nop-backend-dev`
- [x] `Decision` **恢复前置守卫集合**（防幽灵承付/双占用）：① 单据曾 APPROVED 守卫（cancel 路径显式传 `wasApproved`，reverseApprove 路径由迁移守卫保证）；② PO 终态守卫（docStatus != CANCELLED **且** approveStatus == APPROVED——非 APPROVED PO 无活跃承付义务，恢复 = 双占用，见 Goals 守卫 3）；③ `commit()` 返回 null（参数守卫/科目缺失）视为 no-op 不抛错。选项 A = 三守卫全实现（推荐，防双占用 + 防泄漏）；选项 B = 仅守卫 ①（双占用主路径已防，②③ 归 successor）——但②泄漏路径成本低风险真实，③ 是 null 语义必须处理，故推荐 A。备选与理由记录于本 Decision。
      - Skill: `nop-backend-dev`
- [x] `Fix` `runCommitmentRestoreOnInvoiceReverseHook` 实现：config-gated（`CONFIG_BUDGET_COMMITMENT_ENABLED`）；`resolveLinkedOrderCodes(invoice)` 为空则返回；对每个 PO code：加载 PO 校验终态守卫（docStatus != CANCELLED 且 approveStatus == APPROVED，不满足跳过该 PO）；解析承付科目（`CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE` 反查 `ErpMdSubject`，缺失跳过）、期间（按 PO `businessDate` 解析 `resolvePeriodId` 同型，缺失跳过）、金额（PO `totalAmountWithTax`，null 或 ≤0 跳过）→ `budgetCommitmentBiz.commit(PURCHASE_ORDER, poCode, subjectId, null, periodId, amount, context)`；`NopException` 仅守卫类错误（`ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 等）静默跳过，其他异常重新抛出（精确容错范式，Decision 记录）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpPurInvoiceReverseApproveProcessor.reverseApprove`：在 `processor.doReverseApprove(invoice, context)` 之后追加 `processor.runCommitmentRestoreOnInvoiceReverseHook(invoice, true, context)`；`ErpPurInvoiceCancelProcessor.cancel` 在 `processor.doCancel` 之前捕获原始 `approveStatus`，`doCancel` 之后追加 `processor.runCommitmentRestoreOnInvoiceReverseHook(invoice, Objects.equals(originalApproveStatus, APPROVED), context)`。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] invoice reverseApprove/cancel（曾 APPROVED）后对关联 PO 重新生成 COMMITMENT 凭证（`isReversed=false`），config 关闭/从未 APPROVED/PO CANCELLED 时不动作——Phase 3 测试断言证实
- [x] 无 SPI/ORM/会计核心路径变更（`git diff --stat` 仅 erp-pur-service Java + `_cases/` 快照）

### Phase 2 - return 侧承付恢复接线

Status: completed
Targets: `ErpPurReturnProcessor.java`；`ErpPurReturnReverseApproveProcessor.java`；`ErpPurReturnCancelProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1 完成（同型接线 + 守卫复用）

- [x] `Decision` **return 恢复开关语义**：选项 A（推荐）= 恢复 hook 同时受总开关 `CONFIG_BUDGET_COMMITMENT_ENABLED` **与** 子开关 `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN` 门控（与正向 release-on-return 同开关——子开关 OFF 时正向从未释放，恢复 = 幽灵承付双占用）；选项 B = 恢复只依赖总开关（与 invoice 侧一致，但 return 侧正向释放本就受子开关门控，恢复不恢复会造成非对称双占用，弃）。备选与理由记录于本 Decision。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpPurReturnProcessor` 新增 protected step `runCommitmentRestoreOnReturnReverseHook(ErpPurReturn returnOrder, boolean wasApproved, IServiceContext context)`：双开关 config-gated（总开关 + 子开关）；`resolvePurchaseOrderCode(returnOrder)` 解析 PO code（null 跳过）；PO 终态守卫（docStatus != CANCELLED 且 approveStatus == APPROVED）+ 科目/期间/金额解析 + `commit()` 调用同 Phase 1 同型；精确容错同 Phase 1。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpPurReturnReverseApproveProcessor.reverseApprove` 在 `dao().updateEntity(returnOrder)` 之后追加 hook（`wasApproved=true` 恒成立）；`ErpPurReturnCancelProcessor.cancel` 在入口（`validateTransitionForCancel` 之后、状态回退之前）捕获原始 `approveStatus` 判定 `wasApproved`，状态回退后追加 hook（注：该 Processor 无 `doCancel` 方法——直接 `setDocStatus(CANCELLED)` + `updateEntity`，语义为"入口捕获 + 回退后接线"）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] return reverseApprove/cancel（曾 APPROVED + 双开关开启）后对关联 PO 重新生成 COMMITMENT 凭证；子开关 OFF/从未 APPROVED/PO CANCELLED/无关联 PO 时不动作——Phase 3 测试断言证实

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoiceCommitmentRestore.java`（新增）+ `.../TestErpPurReturnCommitmentRestore.java`（新增，或扩展既有 `TestErpPurOrderCommitment`/`TestErpPurReturnCommitmentRelease`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add` invoice 侧测试矩阵（`budget-commitment-test.yaml` 驱动）：① 正向链闭合——PO approve → COMMITMENT 凭证；invoice approve → release（原凭证 isReversed=true）；invoice reverseApprove → 新 COMMITMENT 凭证（isReversed=false，billCode=PO code）+ **断言每 PO 恰 1 张活跃（unreversed）COMMITMENT 凭证**；② invoice cancel 同型恢复 + 单活跃断言；③ config 关闭（`erp-fin.budget-commitment-enabled=false`）reverseApprove/cancel 零恢复凭证；④ **从未 APPROVED 的 invoice cancel → 零新凭证**（防幽灵承付）；⑤ **PO CANCELLED 后 invoice reverseApprove → 零新凭证**（防永久泄漏）；⑥ 无关联 PO 的 invoice（零 receiveLineId 回链）reverseApprove 不动作。
      - Skill: `nop-testing`
- [x] `Add` return 侧测试矩阵：① PO approve → COMMITMENT；return approve（`commitment-release-on-return=true`）→ release；return reverseApprove → 恢复凭证 + 单活跃断言；② return cancel 同型；③ **子开关 OFF（总开关 ON）→ return reverseApprove/cancel 零新凭证**（防幽灵承付）；④ 总开关 OFF → 零恢复；⑤ 从未 APPROVED 的 return cancel → 零新凭证；⑥ 无关联 PO 的 return 不动作。
      - Skill: `nop-testing`
- [x] `Add` **跨路径交互矩阵**（Major 4/N1 修订）：① invoice reverseApprove 在 return approve（releaseIfPresent 已全额释放）之后 → 恢复后**恰 1 张活跃凭证**（交互语义 Decision 记录：恢复=全量恢复 PO 承付，与全额释放对称；多发票场景 invoice#1 冲销恢复全量 + invoice#2 ACTUAL 仍活跃的语义差异归 successor 比例恢复声明）；② 双重 reverseApprove（幂等守卫：第二次 reverseApprove 被迁移守卫拒绝，不二次恢复）；③ **Seq A 终态守卫断言**：PO approve→invoice approve→PO reverseApprove（PO=REJECTED）→invoice reverseApprove → **零恢复凭证**（PO 终态守卫命中，防双占用；PO 重新 approve 时 commit-on-order-approve 重新承付）；④ **Seq B 多发票边界**：PO approve→inv#1 approve→inv#2 approve（释放均吞掉）→inv#1 reverseApprove→inv#2 reverseApprove → **恰 2 张活跃凭证**（全量恢复语义的已知保守边界——超占用方向自愈于 PO 生命周期终点的 release；按比例语义归 successor，见 Deferred）；⑤ 每序列结束断言 PO 活跃 COMMITMENT 凭证数 ∈ {0, 1}（单冲销序列不变量；Seq B 为显式裁决的 2-active 边界）。
      - Skill: `nop-testing`
- [x] `Proof` 恢复后凭证落库断言（postingType=COMMITMENT + isReversed=false + billCode 回链）+ `_cases/` 快照录制；既有 `TestErpPurOrderCommitment`/`TestErpPurReturnCommitmentRelease`/`TestErpSalOrderCommitment` 零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [x] invoice/return 双侧恢复 + 守卫 + 交互矩阵全绿 + 既有承付测试零回归：`mvn test -pl module-purchase/erp-pur-service`（BUILD SUCCESS）
- [x] 恢复行为/config 关闭/从未 APPROVED/PO CANCELLED/子开关 OFF/无关联 PO/单冲销序列单活跃不变量（Seq B 2-active 边界显式裁决）七路径均有断言证据（无「接线但零覆盖」缺口）；快照录制完成

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/finance/budget.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 收敛：`budget.md` §承付会计 §3「发票 reverseApprove / cancel 不恢复承付（保守方向：保持已释放状态）」声明段更新为「invoice/return 冲销恢复」实现注记（恢复语义 = 全量恢复对齐全额释放 + 恢复前置守卫[曾 APPROVED / PO 非 CANCELLED 且 APPROVED / return 双开关] + 精确容错 + config-gated + 部分冲销/跨期 successor 声明保留）；不修改需求契约段。
      - Skill: none
- [x] `Add` arm-index P1-MA2-083 行「修复状态」→ `done (RC-R1.12)` + 修复落地摘要；roadmap RC-R1.12 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + budget.md 收敛注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_01f91f20affeLPJvu7IY8VeyjU）— 基线证据全部核验准确（4 Processor 零 commit()、release hook/反查方法/config 键/测试配置均吻合），但发现 2 Blocker + 2 Major + 3 Minor：**Blocker 1** cancel 从未 APPROVED 单据会幽灵恢复（commit 无去重 → 双占用）；**Blocker 2** return 恢复未与子开关对称（子开关 OFF 时正向未释放却恢复）；**Major 3** PO CANCELLED 后恢复 = 永久预算泄漏；**Major 4** 跨路径交互（return release 后 invoice 冲销恢复/多发票）未分析，须加单活跃凭证断言；**Minor 5** 全吞容错会静默隐藏凭证生成失败，须裁决精确容错；**Minor 6** Phase 3「可选 GraphQL」违反对抗松弛规则；**Minor 7** arm-index 部分冲销注记与 roadmap 行差异须以 roadmap 为准。修订后重审。
- Independent draft review iteration 2: needs revision（独立子代理 ses_01f89e7a4ffefOHUTeiqnl1TVS）— 7 项原 findings 全部核验为已解决（was-Approved 守卫/双开关对称/PO 终态守卫/交互矩阵/精确容错/无对抗松弛措辞/roadmap 权威），但发现 1 新 Major（N1）+ 2 Minor：**N1** 恢复守卫仅查 PO docStatus 未查 approveStatus——Seq A（PO reverseApprove 后 invoice 冲销恢复 → PO 重新 approve → 2 活跃）与 Seq B（多发票逐张冲销恢复 → 2 活跃）两条可达序列破坏单活跃不变量；**N2** Phase 2 措辞引用不存在的 `doCancel`（ErpPurReturnCancelProcessor 直接 setDocStatus）；**N3** config 翻转（OFF approve → ON reverseApprove）致 2 活跃未登记。修订：守卫 3 扩为 docStatus+CANCELLED 且 approveStatus==APPROVED（修 Seq A）；交互矩阵加 Seq A 零恢复断言 + Seq B 显式裁决 2-active 保守边界（修 N1 剩余）；Phase 2 措辞改为「入口捕获 + 回退后接线」（修 N2）；Deferred 登记 config 翻转 watch-only（修 N3）。重审。
- Independent draft review iteration 3: accept（独立子代理 ses_01f840428ffe903QYJBiQckL1f）— N1/N2/N3 修复全部实仓核验成立（Seq A 可达性证实 `ErpPurOrderProcessor.validateTransitionForReverseApprove:146-151` 无发票关联守卫 + commit 无去重 `ErpFinBudgetCommitmentBizModel:73-74` + 守卫 3 四处一致；Seq B 2-active 自愈经 `CommitmentVoucherGenerator.reverseCommitment:77-94` 全部活跃凭证红冲证实；`ErpPurReturnCancelProcessor:22-33` 无 doCancel 措辞修正准确；config 翻转 Deferred 登记无矛盾）；仅 2 非阻塞 Minor（Phase 4 owner-doc 注记守卫摘要补「且 APPROVED」conjunct + Phase 3 退出标准补「单冲销序列」范围限定）——已就地修订。**计划可标记 active。**

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-purchase/erp-pur-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **部分冲销/跨期/按比例恢复**：Classification `watch-only residual`（budget.md:264-265/272 既有 successor 声明维持，arm-index 修复方向注记与 roadmap 行范围差异以 roadmap 行为准）；Successor Required: yes（触发条件 = 多组织预算硬约束启用 + 部分开票/冲销为常态业务路径）。
- **sales 侧承付冲销恢复**：Classification `watch-only residual`（budget.md:272-273 既有 successor 声明）；Successor Required: yes（触发条件 = sales 承付控制启用）。
- **多发票场景下 invoice#1 冲销恢复全量 vs invoice#2 ACTUAL 活跃的语义差异**：Classification `watch-only residual`（本行交互矩阵以「单活跃凭证不变量」覆盖单冲销序列边界 + Seq B 显式裁决 2-active 保守边界，语义裁决 = 全量恢复与全额释放对称；按比例语义归上面同一 successor）；Successor Required: yes（同上触发条件）。
- **config 翻转不对称（生命周期中途翻转 config）**：Classification `watch-only residual`——config OFF 时 invoice approve（无 release，C1 保留）后 ON，reverseApprove → 恢复生成 C2 → 2 活跃；属 config-gated 设计固有特性（SPI 无 hasUnreversedCommitment 预检，且 SPI 变更为本行 Non-Goal），需显式 config 翻转才会触发，登记 watch-only 防被误判回归；Successor Required: no。

## Closure

Status Note: 已完成。全部 4 阶段执行完毕：invoice/return 双侧承付恢复 hook 接线（protected step + 双 Processor 复用 + 三/四守卫 + 精确容错）+ 16 组测试矩阵全绿（执行期 17:52-17:54：erp-pur-service 163 tests 全绿含新 16 组 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + checker actual ≤ baseline）+ 文档回填（budget.md 收敛注记 + arm-index P1-MA2-083 done + roadmap RC-R1.12 done + 日志条目）。**环境注记**：18:04-18:28 期间 nop-entropy 平台 jars 被另一会话并发重建（68 核心模块），破坏全部 Nop 应用测试的 H2 schema 初始化（NOP_SYS_SEQUENCE/ERP_FIN_GL_MAPPING_RULE 等表缺失——stash 验证与 plan 无关 + 无关仓库 nop-app-mall 同型失败证实为平台级环境问题）。本 plan 的执行期验证证据不受影响；环境恢复后按 Phase 3 Exit Criteria 复跑 `mvn test -pl module-purchase/erp-pur-service` 即可。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_01f0a3bbcffeX6gUMbtplHjKd8）结束审计 **PASS**——计划状态一致性（全部 [x] + Status completed）、6 Processor 接线与守卫逐项核验（无 SPI/ORM 变更）、16 组测试矩阵 + `_cases/` 快照落盘、checker R2c=1383 ≤ baseline 1383（R12c=40 vs 38 为 pre-existing 漂移非本 plan 引入且 R12 不在 CI 门控块）、文档回填三处一致、`mvn clean compile -pl module-purchase/erp-pur-service` BUILD SUCCESS。3 项 Info 无阻塞（环境注记 + R12c 既有漂移 + Deferred 登记正确）。

Follow-up:

- **环境恢复后复跑验证**：nop-entropy 平台重建稳定后执行 `mvn test -pl module-purchase/erp-pur-service`（期望 163 tests 全绿）+ `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——本 plan 代码/测试/文档已就绪，若平台行为正式变更导致 schema init 语义变化，按平台升级适配流程另行处理。
- 部分冲销/跨期/按比例恢复 + sales 侧承付恢复 + config 翻转不对称维持 Deferred But Adjudicated watch-only residual（见上）。
