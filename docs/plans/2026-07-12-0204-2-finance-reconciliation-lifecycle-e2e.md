# 2026-07-12-0204-2-finance-reconciliation-lifecycle-e2e Finance AR/AP 核销生命周期业务动作浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: Finance AR/AP 核销单生命周期（create→post→reverse + 双面对账查询）浏览器层端到端验证
> Last Reviewed: 2026-07-12
> Source: Deferred 项承接 `docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md` Deferred「Payment/Receipt xwf 浏览器层 E2E + 域级 settle 核销」（Successor Required: yes）。**Payment/Receipt xwf 段经 `2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md` 权威裁决浏览器层不可行**（`WorkflowEngineImpl.newSteps` fallback sysUser(0)，nop 用户无法物化），本计划**仅承接「域级 settle 核销」段**——即 finance 域正式核销单 `ErpFinReconciliation`（独立作用于辅助账 `ErpFinArApItem`，不经 xwf，DIRECT `@BizMutation` 可达）。触发条件已满足：(1) 核销单 BizModel + AutoReconciliationEngine + DualSideConsistencyChecker + ReconciliationSettler 已落地（plan `2026-07-02-0300-3` + `2026-07-04-0115-1`，纯 DIRECT @BizMutation 无 xwf 依赖）；(2) 项目当前重点「各域细化端到端验证」（AGENTS.md），finance 作为核心域其核销生命周期是 business-actions E2E 覆盖的最大单一缺口（11 域 covered，finance 核销未 covered）。
> Related: `2026-07-09-1249-1`（Deferred source，本计划承接 settle 段）、`2026-07-02-0300-3`（核销单 + 辅助账 + 往来余额落地源）、`2026-07-04-0115-1`（自动核销引擎 + 双面对账兜底落地源）、`2026-07-09-0814-2`（business-actions 三原语 helper 范式源）、`docs/design/finance/ar-ap-reconciliation.md`（权威设计）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **核销单 BizModel 已完整落地**（`module-finance/erp-fin-service/.../entity/ErpFinReconciliationBizModel.java`，extends `CrudBizModel<ErpFinReconciliation>`，`@BizModel("ErpFinReconciliation")`）：
  - `create(direction, partnerId, businessDate, lines:List<ReconciliationLineInput>, ctx)` `@BizMutation @SingleSession` :71——构造草稿头（code=REC-UUID，docStatus=DRAFT）+ 行（paymentItemId/invoiceItemId/settledAmount），头/行经 daoProvider 独立 dao 落库；前置校验 direction/partnerId/businessDate/lines 非空。
  - `post(reconciliationId, ctx)` :118 `@BizMutation @SingleSession`——DRAFT 态守卫 + 行加载 + `validateLine`（direction/partner 一致 + 双方 OPEN + 不超额 + 业务日期不早于发票日）→ `settler.settle(head, lines)` → docStatus=POSTED + postedAt/postedBy → `flushBeforeBalance` + `partnerBalanceUpdater.refresh(partnerId)`。
  - `reverse(reconciliationId, ctx)` :147 `@BizMutation @SingleSession`——POSTED 态守卫 → `settler.reverseSettle(lines)` → docStatus=REVERSED → flush + partnerBalanceUpdater.refresh。
  - `runAutoReconciliation(direction, partnerId, strategy, ctx)` :166 `@BizMutation`——**config 门控** `erp-fin.auto-reconcile`（默认 false，:208 `isAutoReconcileEnabled`），关闭时抛 `ERR_AUTO_RECON_DISABLED`；启用时按 strategy（FIFO/BY_AMOUNT/BY_RATIO，默认 FIFO :212 `resolveStrategy`）逐 partner 匹配并 create+post。
  - `checkDualSideConsistency(direction, partnerId, ctx)` :198 `@BizQuery`——`DualSideConsistencyChecker.check` 返回 `DualSideDiffReport`（双面对账兜底）。
- **结算器产物可断言**（`ReconciliationSettler.java`）：`settle` 对双方 `ErpFinArApItem` 回写 `setOpenAmountFunctional/Source`（settled 累计减 open）+ `setStatus(resolveStatus(settledF, amountF))`（settled==amount→`SETTLED`，部分→`PARTIAL`，:80/:90）；`reverseSettle` 反向恢复。**故 post 后可断言双方辅助账 openAmount→0 + status=SETTLED；reverse 后恢复原 openAmount + status=OPEN**。
- **辅助账实体可经标准 __save 自包含建**（`ErpFinArApItemBizModel extends CrudBizModel<ErpFinArApItem>` :31）——business-actions helper `createViaSave` 范式可直接用。关键字段（`module-finance/model/app-erp-finance.orm.xml` + seed CSV `erp_fin_ar_ap_item.csv` 核实）：direction/orgId/acctSchemaId/partnerId/sourceBillType/sourceBillCode/businessDate/currencyId/exchangeRate/amountSource/amountFunctional/settledAmountSource(=0)/settledAmountFunctional(=0)/openAmountSource/openAmountFunctional/status(=OPEN)/periodId。
- **种子库无可直接核销的 OPEN 对**（关键约束）：`erp_fin_ar_ap_item.csv` 6 行——AP/AR 段 id 1-4（AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT）**全部 SETTLED + openAmount=0**（已结算，不可再核销）；仅 id 5/6（EMPLOYEE_ADVANCE/EXPENSE_CLAIM，partner 5，非发票-收付款对）OPEN。**故核销 E2E 须自包含建 OPEN 发票项+收付款项对**（同 partner+direction+金额），不能复用种子。
- **business-actions helper 范式已验证**（`tests/e2e/business-actions/_helper.ts`）：`createViaSave`（`__save` 建前置，:40）/ `callMutation`/`callMutationOk`（`@BizMutation` 经 GraphQL，标量内联/复杂入参经 `input(type,value)` 走 variable，:65）/ `verifyState`（`__get` 独立断言，:112）/ `findFirst`/`findPageTotal`/`deleteByFilter`/`deleteById`（Nop FieldTreeBean filter 格式，:245；**注**：`findItems` 原语在 `tests/e2e/orchestration/_helper.ts` 而非 business-actions helper——本计划辅助账行断言优先用 `verifyState('ErpFinArApItem', itemId, 'openAmountFunctional status')` 逐项 `__get`，或经 `page.request.post` findPage；若需批量取行，Phase 1 可从 orchestration helper 移植 `findItems`）。复杂入参 `List<ReconciliationLineInput>` 经 `input('<待 Explore 确认精确 input 类型名>', {...})`——GraphQL 对 DAO DTO 入参的映射范式经既有先例确认：`StockMoveRequest`（plain DTO，package `app.erp.inv.biz`）映射为 GraphQL input `i_app_erp_inv_biz_StockMoveRequest`（`inventory-stock-move.action.spec.ts:18` 实证）；按同一命名规则 `ReconciliationLineInput`（package `app.erp.fin.dao.dto`）应映射为 `i_app_erp_fin_dao_dto_ReconciliationLineInput`，list 形式 `[i_app_erp_fin_dao_dto_ReconciliationLineInput]`——但 `List<DTO>` 经 `input()` helper 经无既有测试先例，精确 list 类型可调用性需 Phase 1 Explore 确认。
- **finance 域 business-actions E2E 现状**：仅 `finance-voucher-post.action.spec.ts`（LANDED_COST 手工 post，2329-2）覆盖 finance；**核销/银行对账/坏账三大财务运营流程零 business-actions 覆盖**——是核心域最大 E2E 缺口。
- **config 门控约束**：`runAutoReconciliation` 需 `erp-fin.auto-reconcile=true`，但 webServer JVM 启动参数（e2e-runbook.md 方式 A）当前不含此键；运行中无法翻转（JVM 属性启动期固定）。**故 runAutoReconciliation 浏览器层 E2E 归 Non-Goal**（除非 Phase 1 Explore 裁定可在 webServer 命令追加该 JVM 属性且不影响种子基线——但 auto-recon 默认对全 SETTLED 种子无可匹配 OPEN 对，仅对自包含新建对生效，需谨慎）。

剩余差距：(1) 核销单 create→post→reverse 生命周期零浏览器层覆盖；(2) post/reverse 对辅助账 openAmount/status 回写零断言；(3) 双面对账查询 `checkDualSideConsistency` 零覆盖。

## Goals

- 落地 finance 域正式核销单 `ErpFinReconciliation` 生命周期的浏览器层端到端验证：自包含建 OPEN 发票项+收付款项对（同 partner+direction+金额，经 `ErpFinArApItem__save`）→ `create(direction, partnerId, businessDate, lines)` → `post(reconciliationId)` → 断言核销单 docStatus=POSTED + 双方辅助账 openAmount→0 + status=SETTLED + 往来余额刷新 → `reverse(reconciliationId)` → 断言 docStatus=REVERSED + 双方辅助账 openAmount 恢复 + status=OPEN。
- 落地双面对账查询 `checkDualSideConsistency(direction, partnerId)` `@BizQuery` 浏览器层可达性（返回 DualSideDiffReport 结构非空可观测）。
- 验证 `validateLine` 守卫的负路径：direction 不一致（`ERR_RECONCILIATION_DIRECTION_MISMATCH`）/ 双方非同 partner（`ERR_RECONCILIATION_PARTNER_MISMATCH`）/ 已 SETTLED 项再核销（`ERR_RECONCILIATION_ITEM_NOT_OPEN`）/ 超额核销（`ERR_RECONCILIATION_OVER_AMOUNT`）/ 业务日期早于发票日（`ERR_RECONCILIATION_DATE_BEFORE_INVOICE`），经 `callMutation`（不断言成功）+ errors 含对应 ErrorCode message token。

## Non-Goals

- **不**覆盖 `runAutoReconciliation`——config 门控 `erp-fin.auto-reconcile=true` 需 webServer JVM 启动参数，运行中不可翻转；且自动核销引擎单元/集成测试（0115-1）已覆盖三策略。归后继（触发条件：当 webServer 启动参数可稳定追加该 JVM 属性且对种子基线无副作用时）。
- **不**覆盖银行对账 `ErpFinBankReconciliation`（generate/post/reverse）——不同结果面（需 ErpFinBankStatement + 流水 setup），不同 owner doc（`bank-reconciliation.md`）。归独立 successor。
- **不**覆盖坏账 `ErpFinBadDebt`（writeOff/recover/submit/approve/reject/runBadDebtProvision）——不同结果面（计提/核销/收回 + ALLOWANCE 门控），不同 owner doc（`bad-debt.md`）。归独立 successor。
- **不**覆盖 Payment/Receipt xwf 浏览器层——2330-1 权威裁决不可行（需 nop-entropy 平台变更）。
- **不**断言核销单产生的 GL 凭证——**核销不直接生成 GL 凭证**（`ar-ap-reconciliation.md §核销流程` 步骤5：凭证由收付款审核时生成，核销仅作用于辅助账 openAmount/status）。本计划断言辅助账层产物。
- **不**新增/修改 finance 生产代码——核销单 BizModel + Settler + 引擎均已落地；本计划为纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。

## Task Route

- Type: `verification or audit work`（finance 核销单生命周期浏览器层端到端验证，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`（权威设计：核销流程/状态机/双面关系）、`docs/testing/e2e-runbook.md`（business-actions 套件运行手册 + helper 范式）、`docs/design/finance/posting.md`（核销与 GL 凭证关系边界）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E、business-actions 三原语 helper、GraphQL `@BizMutation`/`@BizQuery` 调用、复杂入参 input() 包装、ErrorCode 负路径断言）；无后端开发技能匹配（零生产代码）。既有 business-actions 范式已由 0814-2/0335-1/0335-2/1800-1 验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 无 JVM 属性新增（runAutoReconciliation 段归 Non-Goal 正是因 config 门控运行中不可翻转）
- 依赖 `app-erp-all/target/quarkus-app/quarkus-run.jar` 预构建（既有前置）
- 无 ORM 变更，故无 ask-first 保护区域门控；纯测试层改动

## Execution Plan

### Phase 1 - 核销 E2E setup 路径裁定 + 辅助账自包含建对验证

Status: completed
Targets: `tests/e2e/business-actions/_helper.ts`（如需新增 setup 原语）
Skill: `nop-testing`

- Item Types: `Explore | Decision | Add`
- Prereqs: 无

- [x] `Explore` | `Decision`：核销 create 入参的 GraphQL 类型映射裁定。**Explore 结果（经 Phase 2 spec 实测全绿确认）**：`create(direction, partnerId, businessDate, lines:List<ReconciliationLineInput>)` 的 `lines` 顶参经 `input('[i_app_erp_fin_dao_dto_ReconciliationLineInput]', [...])` 走 typed variable——helper `callMutation` 产出 `mutation($lines:[i_app_erp_fin_dao_dto_ReconciliationLineInput]){ ErpFinReconciliation__create(direction:"PAYABLE",partnerId:5,businessDate:"2026-07-10",lines:$lines){ id docStatus } }` + `variables:{lines:[...]}`，引擎接受。**Decision**：input 类型名 `i_app_erp_fin_dao_dto_ReconciliationLineInput` 由包名 `app.erp.fin.dao.dto` + 类名 `ReconciliationLineInput` 派生，对齐 0814-2 `StockMoveRequest`(package `app.erp.inv.biz`)→`i_app_erp_inv_biz_StockMoveRequest` 命名先例；List 形式经 GraphQL `[Type]` 列表语法包装为单 variable。字段集：paymentItemId/invoiceItemId/settledAmountSource/settledAmountFunctional。标量顶参 direction/partnerId/businessDate 内联（LocalDate 经字符串字面量 `"2026-07-10"` 由 Nop GraphQL coercion）。残留风险：无实质风险（命名约定确立 + list-of-input via variable 是标准 GraphQL，7/7 用例实测通过）。
      - Skill: `nop-testing`
- [x] `Explore` | `Decision`：辅助账自包含 OPEN 对建对方案 + 往来余额污染防护裁定。**Explore 结果（经 Phase 2 spec 实测 + finance 看板/ar-ap-aging 基线回归确认）**：经 `ErpFinArApItem__save` 建 2 行 OPEN 项成功，mandatory 字段集 code/orgId/acctSchemaId/direction/partnerId/sourceBillType/sourceBillCode/businessDate/currencyId/amountSource/amountFunctional/openAmountSource/openAmountFunctional/status 全提供（settledAmount* defaultValue=0、exchangeRate defaultValue=1、dueDate/periodId 非 mandatory 但 periodId=1 一并提供）。**Decision（往来余额防护）**：`PartnerBalanceUpdater.refresh` 直接回写 `ErpMdPartner.receivableBalance/payableBalance` 字段（非独立余额表），核销 post/reverse 会改写 partner 余额字段。**故 setup 用自包含新建 partner**（`E2E-RECON-PN-` 前缀，partnerType=SUPPLIER/status=ACTIVE，避开种子 partner 1/3/5）+ cleanup 一并删除该 partner（partner 删除使余额字段随之消失）。cleanup 顺序（依赖反向）：核销单行（filter reconciliationId）→ 核销单头 → 2 辅助账行 → partner。实测：finance 看板 KPI（revenue/netProfit=1130/1130 读 GL gl_balance）+ ar-ap-aging 报表（按 sourceBillCode 前缀隔离）+ ErpFinVoucher 列表基线在 spec 运行后全绿，无漂移。
      - Skill: `nop-testing`
- [x] `Add`（触发条件：Phase 1 Decision 2 确认既有 `createViaSave`/`verifyState` 不足以覆盖「建 OPEN 对 + 批量读辅助账行」时）：**触发条件未满足——既有 `createViaSave`（建 partner + ArApItem）/ `verifyState`（逐项 `__get` 断言 openAmountFunctional/status）/ `callMutation`+`callMutationOk`（驱动 create/post/reverse）已充分**。辅助账行断言用 `verifyState('ErpFinArApItem', itemId, 'openAmountFunctional status')` 逐项 `__get`（business-actions 约定，无需从 orchestration helper 移植 `findItems`）。setup 原语以**本地函数**形式写入 spec（`createPartner`/`createItem`/`createRecon`/`cleanupRecon`/`buildPair`，镜像 `quality-ncr-resolve-capa-gate.action.spec.ts` 的本地 `seedNcr` 范式），**不改 `_helper.ts`**。`checkDualSideConsistency` `@BizQuery` 返回复杂对象 `DualSideDiffReport` 需 selection set（非 `callQuery` 原语可表达），spec 内直接构造带 selection 的原始 query。

Exit Criteria:

- [x] Phase 1 两 Decision 落地：lines GraphQL input 传递方式（`[i_app_erp_fin_dao_dto_ReconciliationLineInput]` typed variable）+ 辅助账自包含建对字段集（mandatory 全提供 + 自包含 partner + cleanup 删除）均经实测确认，记录理由与残留风险。
- [x] setup 原语对 1 代表对（PAYABLE，AP_INVOICE + PAYMENT，amount=100）经 `ErpFinArApItem__save` 建成 2 行 OPEN（status=OPEN + openAmount=100），解除 Phase 2 阻塞。（happy-path 用例实测：post 后双方 openAmount→0/status=SETTLED + reverse 后恢复 openAmount=100/status=OPEN 全断言通过。）

### Phase 2 - 核销单生命周期 + 双面对账查询 E2E 落地

Status: completed
Targets: `tests/e2e/business-actions/fin-reconciliation.action.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（setup 路径 + 原语）

- [x] `Add`：`fin-reconciliation.action.spec.ts` 新建——`test.describe('Finance ErpFinReconciliation lifecycle browser-layer E2E', ...)`：
  - 正路径 test：setup OPEN 对 → `create(PAYABLE, partnerId, businessDate, lines=[{paymentItemId, invoiceItemId, settledAmountFunctional=amount}])` → `verifyState('ErpFinReconciliation', recId, 'docStatus')` 断言 DRAFT → `post(recId)` → 断言 docStatus=POSTED → `findItems('ErpFinArApItem', eqFilter id 两项)` 断言双方 openAmountFunctional=0 + status=SETTLED → `reverse(recId)` → 断言 docStatus=REVERSED + 双方 openAmountFunctional 恢复=原 amount + status=OPEN。
  - 双面对账 test：`callMutation`/`page.request.post` 调 `checkDualSideConsistency(PAYABLE, partnerId)` `@BizQuery` 返回 DualSideDiffReport 结构非空（字段存在可观测）。**注**：自包含 setup 仅建 finance 侧 OPEN 发票项（settled=0）、无对应域侧（`ErpPurInvoice.paidAmount`/`ErpSalInvoice.receivedAmount`）结算记录，故 financeSettled=domainSettled=0 → diff=0 ≤ precision → `consistent=true`（经 `DualSideConsistencyChecker.check` 实测确认）。本用例**仅断言查询可达 + 报告结构非空**（direction/partnerId/consistent/rows 字段可观测），不断言 consistency 取值——consistency=false（diff 非零）路径经 0115-1 双面对账兜底单元测试覆盖。
  - 负路径 test（经 `callMutation` 不断言成功 + errors 断言 ErrorCode message token）：direction 不一致 / partner 不一致 / 已 SETTLED 项再核销 / 超额核销 / 业务日期早于发票日，5 守卫各 1 用例。
  - cleanup：`finally` 调 setup 原语对应 cleanup（核销单+行+辅助账对），保护共享 DB 基线。
      - Skill: `nop-testing`
- [x] `Proof`：指定验证命令 `npx playwright test tests/e2e/business-actions/fin-reconciliation.action.spec.ts --workers=1` 全绿；正路径 post 后双方辅助账 status 翻转 SETTLED + reverse 后恢复 OPEN 为核心可观测断言。**实测结果**：7/7 用例全绿（happy path + 双面对账 + 5 负路径），用时 1.1m。business-actions 全套件 48 用例中 47 绿（唯一失败 `cs-canned-response` 为**预先存在的基线问题**——`callQuery` 对复杂返回类型缺 selection set，与本计划无关，本计划为零生产代码/共享 helper 变更的纯新增）。finance 看板 KPI（revenue/netProfit=1130/1130）+ ar-ap-aging 报表 + ErpFinVoucher 列表基线 spec 后全绿，确认 cleanup 无 DB 污染。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 正路径用例：核销单 DRAFT→POSTED→REVERSED 三态 + post 后双方辅助账 openAmount→0/status=SETTLED + reverse 后 openAmount 恢复/status=OPEN 全断言通过。
- [x] 双面对账用例：`checkDualSideConsistency` 返回 DualSideDiffReport 结构非空可观测（direction/partnerId/consistent/rows 字段）。
- [x] 5 负路径守卫用例：各抛对应 ErrorCode（errors 含 message token——方向不一致/往来单位不一致/已结清/超过未核销余额/早于发票业务日期），核销单保持 DRAFT 态不变。
- [x] cleanup 完整：用例后自包含 partner + 其辅助账对 + 核销单/行全部删除（partner 删除使 `PartnerBalanceUpdater.refresh` 写入的 receivableBalance/payableBalance 字段随之消失），finance 看板 KPI（revenue/netProfit）+ ar-ap-aging 报表数值断言基线无漂移（共享 DB 隔离，实测全绿）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0ada1b5b4ffeGijcRPJu4VH9Za) — 全部 Current Baseline 主张经实时仓库核实为真（ErpFinReconciliationBizModel 签名/注解/行号/@SingleSession、ReconciliationSettler status 翻转逻辑、seed CSV 全 SETTLED 1-4 / OPEN 5-6、config 门控 `erp-fin.auto-reconcile` 默认 false、deferred 源 1249-1 + xwf 2330-1 不可行裁决）。两处最险可行性经评估有可行解：(a) `List<ReconciliationLineInput>` GraphQL input——经 `StockMoveRequest`→`i_app_erp_inv_biz_StockMoveRequest` 命名先例确认 DTO→input 映射规则，list 形式无既有测试先例但 Phase 1 Explore 正确处理（含降级）；(b) `ErpFinArApItem__save` 自包含建对——`dueDate`/`periodId` 经 ORM 核实非 mandatory，UK code+orgId 经 `E2E-` 前缀隔离，可行。触发条件裁定 legitimate（xwf 段经权威裁决不可行，本计划仅承接可行 DIRECT settle 段，非 forcing）。**无 BLOCKER**。采纳 4 non-blocking 已修：①`findItems` 幻影引用更正（实为 orchestration helper，本计划改用 `verifyState`/`__get` 逐项或 Phase 1 移植）；②往来余额污染防护强化（`PartnerBalanceUpdater` 写 `ErpMdPartner` 字段非独立表 → Phase 1 Decision 裁定自包含新建 partner + cleanup 删除，Exit Criteria 反映）；③`checkDualSideConsistency` 自包含 setup 预期 `consistent=false`（仅断言可达性非 consistency）；④Phase 1 item 3「如需」改写为显式触发条件。无新增 BLOCKER，草案审查已收敛 → `Plan Status: active`。

## Closure Gates

- [x] 范围内行为完成（正路径 + 双面对账 + 5 负路径守卫用例全绿）
- [x] 相关文档对齐（e2e-runbook.md「业务动作套件」表 + 全套件计数 + business-actions 覆盖域段落更新：finance 核销纳入 covered）
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/fin-reconciliation.action.spec.ts --workers=1` 全绿（7/7）+ business-actions 全套件 48 用例无新增回归（唯一失败 `cs-canned-response` 为预存基线问题，与本纯新增计划无关）+ finance 看板 KPI/ar-ap-aging/ErpFinVoucher 列表基线无漂移（无生产代码变更，仅测试新增）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### runAutoReconciliation 浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config 门控 `erp-fin.auto-reconcile=true` 需 webServer JVM 启动参数，运行中不可翻转。自动核销引擎三策略（FIFO/BY_AMOUNT/BY_RATIO）经 0115-1 单元/集成测试覆盖。本计划覆盖 DIRECT create→post→reverse 手工核销全路径（自动核销内部复用 create+post，路径已间接验证）。
- Successor Required: `yes`（触发条件：webServer 启动参数可稳定追加 `-Derp-fin.auto-reconcile=true` 且对种子基线（全 SETTLED）无副作用时）

### 银行对账 ErpFinBankReconciliation 浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同结果面（generate 需 ErpFinBankStatement + 流水 setup），不同 owner doc（`bank-reconciliation.md`）。generate/post/reverse 三态与核销单生命周期同范式可复用本计划 helper。
- Successor Required: `yes`（触发条件：当推进 finance 域银行对账业务动作 E2E 时）

### 坏账 ErpFinBadDebt 浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同结果面（writeOff/recover/runBadDebtProvision + ALLOWANCE 充足性门控），不同 owner doc（`bad-debt.md`）。
- Successor Required: `yes`（触发条件：当推进 finance 域坏账业务动作 E2E 时）

## Closure

Status Note: 计划完成。Finance AR/AP 核销单 `ErpFinReconciliation` 生命周期（create→post→reverse + 双面对账查询）浏览器层端到端验证落地（7 用例全绿），承接 1249-1 Deferred「域级 settle 核销」段（xwf 段经 2330-1 裁决不可行）。零生产代码/契约/ORM 模型变更（纯 TypeScript 测试新增 + 文档对齐）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session `ses_0ad419effffeYECjs3VWjEK8T6`，read-only 审计，未执行变更）
- Evidence: 独立 closure audit VERDICT=PASS（无 BLOCKING 项）。逐项核实：(1) 新 spec `tests/e2e/business-actions/fin-reconciliation.action.spec.ts` 含正路径（post 后双方 openAmount→0/status=SETTLED + reverse 后恢复）+ 双面对照（DualSideDiffReport 结构非空）+ 5 负路径守卫（各 ErrorCode message token）+ 自包含 setup/cleanup；(2) `git status --short` 证实仅 1 新测试文件 + 4 文档变更，零 Java/ORM/生产代码；(3) 后端主张逐行核实——BizModel `@BizMutation`/`@BizQuery` 注解 + ReconciliationSettler 翻转逻辑 + ReconciliationLineInput 包名/字段（验证 GraphQL input 类型名）+ 5 ErrorCode message token 全匹配；(4) 计划内部一致性 OK（Phase 1/2 Status completed + 全 [x] + Plan Status completed）；(5) 文档对齐 OK（e2e-runbook 业务动作表+计数 234→241 + backlog/README +日志）。`cs-canned-response` 失败经核实为预存基线问题（该文件未 modified、根因为 `callQuery` 缺 selection set 的 helper 限制、与本纯新增计划无关），不构成回归。NON-BLOCKING：计划双面对账 prose 的 `consistent` 预期值已修正（自包含 setup diff=0 → consistent=true，实测确认；spec 本就仅断言 typeof===boolean 不受影响）。

Follow-up:

- runAutoReconciliation / 银行对账 / 坏账浏览器层 E2E（见 Deferred，各自触发条件）
