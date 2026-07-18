# 2026-07-18-0718-2-employee-advance-cash-repay-backend-e2e Finance 员工借款现金还款（EMPLOYEE_ADVANCE_SETTLE 现金还款路径）后端实现 + 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: finance expense-claim 后端设计-实现差距收口（员工借款现金还款 successor）
> Source: `docs/plans/2026-07-14-1218-2-finance-expense-claim-employee-advance-budget-e2e.md` Deferred But Adjudicated「员工借款现金还款 (EMPLOYEE_ADVANCE_SETTLE 现金还款路径)」(l.166-169) — Successor Required: yes，触发条件「现金还款浏览器层入口落地时」。
> 触发条件经实时仓库核实**可由本计划主动驱动满足**：owner doc `docs/design/finance/expense-claim.md §现金还款（Cash Repayment）`（l.114-127）已规定「现金还款生成 `ErpFinVoucher`（`EMPLOYEE_ADVANCE_SETTLE` businessType），借 银行存款 / 贷 其他应收款-员工预支，无需新建通用 finance 付款实体」（参 frappe/hrms `make_return_entry` 凭证承载模式）。但 (a) `ErpFinEmployeeAdvanceBizModel` 全仓 grep 无 `cashRepay` `@BizMutation`（src/main 零命中）；(b) `EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支（`module-finance/erp-fin-service/.../EmployeeAdvanceAcctDocProvider.java:53-60`）硬编码 Dr 2241 应付-员工 / Cr 1221 应收-员工（**仅报销抵扣路径**，无现金还款分支）；(c) `ErpFinEmployeeAdvance` 实体已含 `settledAmount`/`outstandingAmount`/`docStatus`/`approveStatus`/`posted`/`employeeId` 字段（ORM 实测 27 列，无需新列）；(d) `EMPLOYEE_ADVANCE_SETTLE(180)` 枚举已声明 + 字典项已存在。本计划落地 `cashRepay` @BizMutation（BizModel Java）+ Dispatcher.postCashRepay 委派 + Provider SETTLE 分支扩展（SETTLE_TYPE=CASH 分派）+ JUnit + 浏览器层 E2E，主动解除该 Deferred。
> Related: `2026-07-14-1218-2`（前置覆盖报销抵扣 EMPLOYEE_ADVANCE_SETTLE 凭证 + spec fin-employee-advance，本计划补其现金还款 Deferred）；`2026-07-05-0700-2`（employee-advance 报销抵扣 + EMPLOYEE_ADVANCE_SETTLE 凭证范式源）；`2026-07-12-0413-2-finance-bank-recon-bad-debt-e2e.md`（finance 业务动作 + 种子科目补齐范式源）；`2026-07-18-0718-1-credit-facility-interest-accrual-backend-e2e.md`（同批 N=1，finance 域不同 owner doc 独立推进，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18，`read`/`grep`/`awk` 实测，非采信旧记忆）：

### 后端差距：owner doc 设计已就绪、枚举/实体/字典已有，零现金还款实现

- **owner doc 设计已就绪**：`docs/design/finance/expense-claim.md §现金还款（Cash Repayment）`（l.114-127）明示「员工借款未消费部分以现金/银行转账退回，不建独立还款单，**用 `ErpFinVoucher` 凭证承载**」。还款分录：Dr 1002 银行存款（或库存现金）/ Cr 1221 其他应收款-员工预支（l.122-125）。凭证经业财过账引擎（`IErpFinAcctDocProvider`）生成，业财回链（`ErpFinVoucherBillR`）关联源借款单。
- **`ErpFinEmployeeAdvanceBizModel` 是 thin Facade**（实测 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinEmployeeAdvanceBizModel.java`）：仅 `cancel()` `@Override` + `CrudBizModel` 继承路径。状态机动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）经 xbiz 委托 `ErpFinEmployeeAdvanceProcessor`（`_vfs/erp/fin/model/ErpFinEmployeeAdvance/ErpFinEmployeeAdvance.xbiz:5-27` 经 `inject('...Processor').<method>(id, svcCtx)`）。**零 `cashRepay`**。
- **`EmployeeAdvancePostingDispatcher.postSettle`**（实测 `module-finance/erp-fin-service/.../posting/EmployeeAdvancePostingDispatcher.java:60-82`）：
  - 签名：`boolean postSettle(String claimCode, Long partnerId, BigDecimal netAmount, Long orgId, Long currencyId, java.time.LocalDate voucherDate)`。
  - 实现：构造 PostingEvent（businessType=EMPLOYEE_ADVANCE_SETTLE + billHeadCode=claimCode + acctSchemaId=`resolveAcctSchemaId(orgId)` + currencyId 兜底=1L + voucherDate 兜底=`CoreMetrics.today()`）+ billData 仅含 `BILL_DATA_EMPLOYEE_ID=partnerId`（**键名误导，值为 partnerId 非 employeeId——`ErpFinConstants.java:90-92` Javadoc 明示**）+ `TOTAL=netAmount`。
  - 调 `executor.postEvent(event)` 返回 voucherId；非 null→true / null→false；catch Exception → log + return false（**失败不阻断业务**，对齐 `tryPost` 范式）。
  - **关键**：postSettle 不设 `SETTLE_TYPE`——Provider 默认走 OFFSET（报销抵扣）路径。本计划新增 `postCashRepay` 镜像此范式，billData 增 `SETTLE_TYPE=CASH` 键。
- **`EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支硬编码报销抵扣路径**（`EmployeeAdvanceAcctDocProvider.java:53-60`）：
  ```java
  } else { // EMPLOYEE_ADVANCE_SETTLE
      VoucherFact debit = fact(SUBJECT_PAYABLE_EMPLOYEE, "其他应付款-员工", DC_DEBIT, amount, event);  // Dr 2241
      debit.setPartnerId(partnerId);
      facts.add(debit);
      VoucherFact credit = fact(SUBJECT_RECEIVABLE_EMPLOYEE, "其他应收款-员工预支", DC_CREDIT, amount, event);  // Cr 1221
      credit.setPartnerId(partnerId);
      facts.add(credit);
  }
  ```
  支持类型集 `EnumSet.of(EMPLOYEE_ADVANCE, EMPLOYEE_ADVANCE_SETTLE)`；SETTLE 分支无 SETTLE_TYPE 分派。
- **`EmployeeAdvanceAcctDocProvider` helper 缺 `asString`**（实测 `:75-98`）：仅有 `readDecimal(event, key)`（:75-84）+ `asLong(value)`（:86-98）。本计划 SETTLE_TYPE 读取须新增 `asString(value)` helper 或 inline `String.valueOf(...)` + null guard。
- **`IErpFinVoucherBiz.post` 签名 + REQUIRES_NEW + 幂等**（实测 `IErpFinVoucherBiz.java:22-32`）：`@Transactional(REQUIRES_NEW)` + `Long post(PostingEvent, IServiceContext)` 返回 voucherId 或 null（幂等命中）+ Javadoc「源单据已过账时返回 null」。**关键**：post 在**独立事务**提交，与外层 cashRepay `@BizMutation` 事务隔离——失败语义须 Phase 1 Decision 裁决。
- **`ErpFinEmployeeAdvance` 实体**（实测 `module-finance/model/app-erp-finance.orm.xml:1252-1283` 27 列）：`amountFunctional`（本位币金额）+ `settledAmount`（已清算金额）+ `outstandingAmount`（未还金额）+ `docStatus`（字典 `erp-fin/advance-status`）+ `approveStatus`（字典 `wf/approve-status`）+ `posted` + `employeeId` + `currencyId` + `orgId`。tagSet 含 `gid,erp.finance,use-approval`。
- **`erp-fin/advance-status` 字典实测仅 5 值**（`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/advance-status.dict.yaml:6-26`）：`DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED`。**无 SETTLED 值**。owner doc `expense-claim.md §还款状态派生`（l.153-161）明示状态经金额比例**派生投影**（未还/部分/已结清）非 docStatus 字典推进。**本计划保持 `docStatus=APPROVED` 不变**（outstandingAmount=0 由派生投影表达「已结清」，不引入新字典值——避免 ORM/字典保护区域变更）。
- **`billData.BILL_DATA_EMPLOYEE_ID` 契约 = partnerId 非 employeeId**（实测 `ErpFinConstants.java:90-92`）：Javadoc 明示「billData 键：携带已解析的 employee.partnerId（**非 employee.id**）」。所有 Producer（`EmployeeAdvancePostingDispatcher:72,103` + `ExpenseClaimPostingDispatcher:73`）+ Consumer（`EmployeeAdvanceAcctDocProvider:46` + `ErpFinArApItemGenerator:245`）均按此契约。**本计划 postCashRepay 须传 `partnerId`（经 `EmployeeAdvancePostingDispatcher.resolveEmployeePartnerId(employeeId)` 解析）入此键，非 employeeId**。

### 既有报销抵扣 E2E 已覆盖（本计划仅增量现金还款路径）

- `tests/e2e/business-actions/fin-employee-advance.action.spec.ts`（1218-2 落地）已覆盖：
  - (a) **借款审核** `submit→approve` + `EMPLOYEE_ADVANCE` 凭证 Dr 1221 / Cr 1002；
  - (b) **reverseApprove 红冲**；
  - (c) **报销抵扣联动** ExpenseClaim approve → `AdvanceOffsetOrchestrator.offset` 自动抵扣 + `EMPLOYEE_ADVANCE_SETTLE` 凭证 Dr 2241 / Cr 1221；
  - (d) 非法守卫。
  - spec 内 inline helper：`setupEmployee`/`setupAdvance`/`cleanupCtx`（**非** `runEmployeeAdvanceChain`——本计划 spec 复用范式时按实际命名引用，可按需提取至 `_helper.ts`）。
- **零 spec 覆盖现金还款**（`cashRepay` 入口不存在）。1218-2 Deferred 明示「现金还款路径须 Explore 核实后端入口」——本计划 Explore 已核实入口缺失，主动落地。

### 浏览器层 E2E 范式（复用，零范式新增）

- `_helper.ts` 三原语 + `findFirst` + `findVoucherIdByBillCode` + `assertVoucherLines`（`tests/e2e/orchestration/_helper.ts`）。1218-2 spec 内 `setupEmployee`/`setupAdvance`/`cleanupCtx` inline 编排范式可复用（如有需要可提取至 `_helper.ts`）。
- 自包含 setup（建测试专用 partner+employee+EmployeeAdvance 隔离）+ finally 兜底 cleanup（凭证+辅助账+advance+employee+partner 逐域删，保护 finance 看板/报表数值断言基线）。
- GraphQL 入参序列化：`BigDecimal` 经 String scalar；`Long` 经 String（对齐 fin-employee-advance 1218-2 范式）。

### 剩余差距

1. `ErpFinEmployeeAdvanceBizModel.cashRepay` `@BizMutation` 缺失（金额更新 + 委派 Dispatcher.postCashRepay）。
2. `EmployeeAdvancePostingDispatcher.postCashRepay` 缺失（镜像 postSettle，billData 增 SETTLE_TYPE=CASH 键）。
3. `EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支需扩展为 SETTLE_TYPE 分派（OFFSET 报销抵扣既有默认 / CASH 现金还款新增）+ 新增 `asString` helper。
4. `ErpFinErrors` 新增 ErrorCode（NOT_REPAYABLE / AMOUNT_INVALID / EXCEEDS_OUTSTANDING）。
5. `ErpFinConstants` 新增 `BILL_DATA_SETTLE_TYPE` + `SETTLE_TYPE_CASH`/`SETTLE_TYPE_OFFSET` 常量。
6. 浏览器层 E2E 缺失（无 cashRepay 路径覆盖）。
7. 种子 COA：`erp_md_subject.csv` 已含 `1221`（1218-2 补齐）+ `1002`（基础科目）+ `2241`（1218-2 补齐）—— **无需新科目**。

## Goals

- 后端落地 `cashRepay(@Name advanceId, @Name amount, IServiceContext)` `@BizMutation` 返回 `ErpFinEmployeeAdvance`（更新后的实体）：
  - 守卫：advance 存在；`posted=true && approveStatus=APPROVED`（已过账借款才可还款，否则 `ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE`）；`amount > 0`（否则 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID`）；`amount <= outstandingAmount`（超额还款守卫 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING`）。
  - 更新 advance：`settledAmount += amount`；`outstandingAmount -= amount`；`docStatus` 保持 `APPROVED` 不变（**派生投影**对齐 owner doc——outstandingAmount=0 表达「已结清」语义，不引入字典 SETTLED 值）。
  - 委派 `EmployeeAdvancePostingDispatcher.postCashRepay(advance, amount, context)` 返回 boolean 成功（镜像 `postSettle` 范式，false=过账失败但业务字段已更新——失败语义 Phase 1 Decision）。
  - 返回更新后的 advance 实体。
- 新增 `EmployeeAdvancePostingDispatcher.postCashRepay(ErpFinEmployeeAdvance advance, BigDecimal amount, IServiceContext)` 返回 `boolean`（镜像 `postSettle:60-82` 范式）：
  - 构造 PostingEvent：businessType=EMPLOYEE_ADVANCE_SETTLE；billHeadCode=`"EA-CASH-REPAY-" + advance.getCode() + "-" + CoreMetrics.currentTimeMillis()`（含时间戳避免同 advance 多次还款碰撞，对齐 `ErpHrShiftSwapRequestBizModel.submit` nanoTime 范式）；orgId/currencyId/acctSchemaId/voucherDate 派生（镜像 postSettle）。
  - billData 携带：`BILL_DATA_EMPLOYEE_ID=partnerId`（经 `resolveEmployeePartnerId(advance.getEmployeeId())` 解析，**键名沿用既有但值为 partnerId**——对齐 `ErpFinConstants.java:90-92` 既有契约）+ `TOTAL=amount` + `BILL_DATA_SETTLE_TYPE=SETTLE_TYPE_CASH`。
  - 调 `executor.postEvent(event)` → return voucherId != null；catch Exception → log + return false（镜像 `postSettle` 失败不阻断业务）。
- 扩展 `EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支为 SETTLE_TYPE 分派（行为：默认/未设/OFFSET → 既有报销抵扣路径，**零回归**；CASH → 新现金还款路径 Dr 1002 / Cr 1221）+ 新增 `asString(value)` helper 镜像 `asLong` 范式。
- **JUnit**：`TestErpFinEmployeeAdvanceCashRepay`（新建）覆盖正路径（现金还款 + 凭证生成 + advance 字段翻转 + outstandingAmount=0 时「已结清」派生投影）+ 部分还款 + 守卫（未过账/超额/amount=0/advance 不存在）+ Provider SETTLE_TYPE=CASH vs OFFSET/null 分派单元测试。
- **浏览器层 E2E**（1 新 spec `fin-employee-advance-cash-repay.action.spec.ts`）：自包含 setup partner + employee + EmployeeAdvance(amountFunctional=500) → submit→approve→posted → `cashRepay(advanceId, "200")` → 断言 advance.settledAmount=200 + outstandingAmount=300 + docStatus=APPROVED（不变）+ 经 `findVoucherIdByBillCode("EA-CASH-REPAY-{code}-...", "NORMAL")` 反查凭证存在 + `assertVoucherLines` Dr 1002=200 / Cr 1221=200 精确数值。
- **owner doc 收口**：解除 1218-2 Deferred「员工借款现金还款 (EMPLOYEE_ADVANCE_SETTLE 现金还款路径)」（补 `**RELEASED by 2026-07-18-0718-2**`）；`expense-claim.md §现金还款` 段下补实现注记（SETTLE_TYPE 分派机制 / billHeadCode 格式 / docStatus 保持 APPROVED 派生投影规则）；`e2e-runbook` 业务动作表 + finance 现金还款行 + 凭证行断言表 +1 行（EMPLOYEE_ADVANCE_SETTLE CASH Dr 1002 / Cr 1221）+ 套件计数对齐；当日日志聚合条目。

## Non-Goals

- **不加 `erp-fin/advance-status` 字典 SETTLED 值**：字典实测仅 5 值（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）。owner doc `§还款状态派生`（l.153-161）明示状态经金额比例**派生投影**（未还/部分/已结清），非 docStatus 字典推进。本计划保持 `docStatus=APPROVED` 不变，outstandingAmount=0 由查询/UI 派生表达「已结清」。如业务要求字典 SETTLED 值，归独立 ORM/字典 successor（触发条件：UI 列表筛选/统计需显式 SETTLED 状态时）。
- **不加 ORM 列**（`claimedAmount`/`returnedAmount` 三金额闭环）：`expense-claim.md §借款金额维度建议`（l.143-151）明示「属 ORM 字段建议（保护区域），落地须计划批准」。本计划继续在 `settledAmount` 混合累计（报销抵扣 + 现金还款），三金额闭环归独立 successor（触发条件：HR 三金额报表/查询需求落地时）。
- **不做薪资扣回（Additional Salary）路径**：依赖 HR 薪酬扣回项落地，跨域跨模块；归 HR successor（触发条件：HR `Additional Salary` 模块落地时，对齐 `expense-claim.md §借款清算三路径` l.137）。
- **不做 partial cash repay 的多次累计还款 E2E**：单次现金还款正路径 + 部分还款（amount < outstanding）已覆盖核心；多次累计还款（如 500 借款分 200+200+100 三次还清）属操作场景变体，JUnit 覆盖即可，浏览器层不专设 spec。
- **不做 cashRepay 审批工作流**：现金还款属事实操作（资金已到账），不走审批（区别于借款审核 use-approval）；如业务需审批，归 successor（触发条件：现金还款需主管审批时）。
- **不做 `reverseCashRepay` 红冲浏览器层 E2E**：通用 `ErpFinVoucher__reverse(billHeadCode, businessType)` 已由 2004-2 orchestration 覆盖 DIRECT 红冲全路径；本计划 cashRepay 凭证生成后红冲复用既有路径（红字凭证行同向取负断言对齐 0742-1 范式可作 spec 内 1 assertion，不专设 spec）。
- **不做外币借款现金还款的汇兑损益**：本位币现金还款已覆盖核心；外币还款（员工用外币还款本位币借款 / 反之）的汇兑损益分解属不同结果面 successor（对齐 EXCHANGE_GAIN_LOSS 范式）。
- **不做借款转员工奖金/罚款等特殊清算路径**：仅核心两路径（报销抵扣 + 现金还款）；特殊清算归独立 successor。
- **不实现新后端契约/ORM/codegen/字典**：本计划仅 service 层 BizModel 新增 @BizMutation + Dispatcher 新方法 + Provider 扩展 + 常量 + ErrorCode + 测试 + 浏览器层 E2E。若 Explore 发现 latent defect 需根因诊断，重新加载 `nop-debugging`。

## Task Route

- Type: `implementation-only change`（finance service 层 BizModel 新增 `@BizMutation` + Dispatcher 新方法 + Provider 扩展 + 测试 + 浏览器层 E2E；ORM/契约/codegen/字典无变更）
- Owner Docs: `docs/design/finance/expense-claim.md`（§现金还款 + §借款清算三路径 + §还款状态派生 + §业财过账，既有）、`docs/design/finance/posting.md`（`IErpFinAcctDocProvider` 机制 + 红冲机制方向二，既有）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件结构，既有）。
- Skill Selection Basis: finance service 层 BizModel 新方法 + Dispatcher 新方法（手写 Java 非 `_gen/`）+ 跨实体访问（advance → employee → partnerId 解析）+ 业财过账（IErpFinVoucherBiz.post REQUIRES_NEW 事务语义 + Dispatcher.postSettle 范式 + AcctDocProvider 扩展）+ 错误处理（NopException + ErrorCode）→ **必须加载 `nop-backend-dev`**（决策门 / xbiz 动作声明 / 跨实体访问自检 / 异常处理规范 / 产品化可定制性自检）；JUnit 测试经 `JunitAutoTestCase` harness → 加载 `nop-testing`；浏览器层 E2E 本体 `Skill: none`（Playwright 浏览器层非 `nop-testing` 后端快照范畴，对齐 1218-2 范式裁决）。需阅读 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`StringHelper`/`CoreMetrics`）+ `04-reference/safe-api-reference.md`（`IErpFinVoucherBiz.post` 安全 API 签名 + REQUIRES_NEW 幂等/事务契约）。
- Protected Areas: finance service 层 BizModel + Dispatcher + AcctDocProvider 属应用层非会计保护区域（业务方法新增 + 既有 Provider 扩展，不动既有 GL/凭证/辅助账机制）；不改 ORM/契约/`_gen/`/字典；任何 finance 生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- **种子 COA 经实测已就绪**：`erp_md_subject.csv` 含 `1221`（1218-2 补齐）+ `1002`（基础）+ `2241`（1218-2 补齐）—— **无需新科目补齐**。
- **webServer JVM arg 无需新增**（cashRepay 无 config 门控，DIRECT 可达；acctSchemaId 经 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)` 解析，对齐 postSettle 既有 `resolveAcctSchemaId(orgId)` 范式）。
- **回滚策略**：全部改动为应用层 Java + 测试 + 文档，git 可逆；SETTLE_TYPE=OFFSET（默认 / null）路径行为不变（既有报销抵扣零回归）；自包含 setup（测试专用 employee+advance 隔离）+ finally cleanup（凭证+辅助账+advance+employee+partner 逐域删，不污染 finance 看板/报表数值断言基线，对齐 1218-2 隔离范式）。

## Execution Plan

### Phase 1 - 后端 cashRepay @BizMutation + Dispatcher.postCashRepay + Provider 扩展

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinEmployeeAdvanceBizModel.java`（新增 `cashRepay` `@BizMutation`，Java `@BizMutation` 自动暴露 GraphQL 端点无需 xbiz action）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/EmployeeAdvancePostingDispatcher.java`（新增 `postCashRepay` 方法）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/EmployeeAdvanceAcctDocProvider.java`（扩展 SETTLE 分支 + 新增 `asString` helper）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`（新增 ErrorCode）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java`（新增 SETTLE_TYPE 常量）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision | Explore`: 裁决 3 项关键实现选择（须在编码前定）：
  - **(a) cashRepay 实现层**：(1) BizModel Java `@BizMutation` 直接落地 + 委派 Dispatcher.postCashRepay（对齐 thin Facade 模式，状态机动作虽走 Processor 但 cashRepay 不属状态机迁移属金额更新，BizModel 直落合理）；(2) xbiz → Processor 委托（状态机动作范式）。**裁决：(1) BizModel Java 直接落地**——cashRepay 非状态机迁移（docStatus 不变，仅金额更新 + 凭证生成），与 `cancel()` 同属 BizModel Facade 层动作；**Java `@BizMutation` 经 Nop 平台自动暴露 GraphQL 端点，无需 xbiz action 注册**（live 实测 `ErpFinEmployeeAdvanceBizModel.java:30-34` `cancel()` 是 Java-only `@BizMutation`，`ErpFinEmployeeAdvance.xbiz` 内无对应 `cancel` action——xbiz 内 5 个 mutation 仅用于覆盖 `IApprovableBiz` 默认审批源委托到 Processor，**非** Java `@BizMutation` 暴露的必需机制）。委派 `EmployeeAdvancePostingDispatcher.postCashRepay` 复用既有 PostingEvent 构造 + 失败处理范式（postSettle 同型）。
  - **(b) cashRepay 事务与失败语义**：`IErpFinVoucherBiz.post` 经 `@Transactional(REQUIRES_NEW)` 在独立事务提交，与外层 `@BizMutation` 事务隔离。失败语义裁决：(1) **post 失败（return false / 抛异常）→ cashRepay 仍更新 advance 字段 + 返回 advance（字段已持久化）+ log warn**（对齐 `postSettle` 失败不阻断业务范式，凭证失败可经 5.1 异常工作台后处理）；(2) post 失败 → 抛异常回滚 advance 字段更新。**裁决：(1) post 失败不阻断字段更新**——对齐 `postSettle:78-81` 既有 `catch Exception → log + return false` 范式（报销抵扣路径同型失败语义已审计）；cashRepay 在调 postCashRepay **之前** 完成 advance 字段更新 + `updateEntity` 持久化（字段已落库），再调 postCashRepay（独立事务）；如 postCashRepay 失败，advance 字段已更新（settledAmount/outstandingAmount 已翻转），凭证失败归 5.1 异常工作台手工补录——**残留风险记录**（字段已更新但凭证缺失，须经人工/异常工作台补凭证，对齐 postSettle 同型残留风险）。
  - **(c) docStatus 终态推进**：(1) 保持 `docStatus=APPROVED` 不变（outstandingAmount=0 由派生投影表达「已结清」，对齐 owner doc）；(2) 推进 `docStatus=SETTLED`（须新增字典值，违反 Non-Goal）。**裁决：(1) 保持 APPROVED**——字典实测无 SETTLED 值；owner doc `§还款状态派生` 明示派生投影非字典推进；如业务要求显式 SETTLED 状态，归独立字典 successor（已列入 Non-Goal + Deferred But Adjudicated）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinEmployeeAdvanceBizModel.cashRepay(@Name("advanceId") Long advanceId, @Name("amount") BigDecimal amount, IServiceContext)` `@BizMutation` 返回 `ErpFinEmployeeAdvance`：
  - 守卫：advance 存在（`requireAdvance`）；`posted=true && approveStatus=APPROVED`（否则 `ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE`）；`amount != null && amount > 0`（否则 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID`）；`amount <= outstandingAmount`（否则 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING`）。
  - 更新 advance：`settledAmount += amount`；`outstandingAmount -= amount`；`docStatus` 保持 APPROVED 不变（Phase 1 Decision (c)）。
  - `updateEntity(advance, null, context)` 持久化字段翻转（**先于** postCashRepay，保证字段已落库）。
  - 注入具体类 `EmployeeAdvancePostingDispatcher advancePostingDispatcher`（**`@Inject` 字段不能为 `private`**——AGENTS.md Nop 规则；该具体类无独立接口，对齐 `ErpFinEmployeeAdvanceBizModel.java:23-24` 既有 `@Inject ErpFinEmployeeAdvanceProcessor` 具体类注入范式；**不** `new` 该 Dispatcher——`new` 会绕过 IoC 装配其自身 `@Inject FinPostingExecutor executor` + `IDaoProvider daoProvider` 字段致其失效），委派 `advancePostingDispatcher.postCashRepay(advance, amount, context)` 返回 boolean（失败 log warn 不阻断，对齐 Decision (b)）。
  - 返回 advance（带最新字段）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `EmployeeAdvancePostingDispatcher.postCashRepay(ErpFinEmployeeAdvance advance, BigDecimal amount, IServiceContext context)` 返回 `boolean`（镜像 `postSettle:60-82`）：
  - 构造 PostingEvent：businessType=EMPLOYEE_ADVANCE_SETTLE；billHeadCode=`"EA-CASH-REPAY-" + advance.getCode() + "-" + CoreMetrics.currentTimeMillis()`；orgId=advance.getOrgId()；acctSchemaId=`resolveAcctSchemaId(advance.getOrgId())`（既有 helper）；currencyId=advance.getCurrencyId()（兜底=1L）；exchangeRate=BigDecimal.ONE；voucherDate=`CoreMetrics.today()`。
  - billData 携带：`BILL_DATA_EMPLOYEE_ID=resolveEmployeePartnerId(advance.getEmployeeId())`（**键名沿用既有但值为 partnerId**，对齐 `ErpFinConstants.java:90-92` + `EmployeeAdvancePostingDispatcher:103` 既有契约，**非 employeeId**）+ `TOTAL=amount` + `BILL_DATA_SETTLE_TYPE=SETTLE_TYPE_CASH`（新键）。
  - 调 `executor.postEvent(event)` → return voucherId != null；catch Exception → log + return false（镜像 postSettle 失败不阻断业务）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinConstants` 新增常量：`String BILL_DATA_SETTLE_TYPE = "SETTLE_TYPE"` + `String SETTLE_TYPE_CASH = "CASH"` + `String SETTLE_TYPE_OFFSET = "OFFSET"`（如不存在）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支扩展为 SETTLE_TYPE 分派 + 新增 `asString` helper：
  ```java
  } else { // EMPLOYEE_ADVANCE_SETTLE
      String settleType = asString(event.getBillData().get(ErpFinConstants.BILL_DATA_SETTLE_TYPE));
      if (ErpFinConstants.SETTLE_TYPE_CASH.equals(settleType)) {
          // 现金还款路径：Dr 1002 银行存款 / Cr 1221 其他应收款-员工预支
          VoucherFact debit = fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_DEBIT, amount, event);
          facts.add(debit);
          VoucherFact credit = fact(SUBJECT_RECEIVABLE_EMPLOYEE, "其他应收款-员工预支", DC_CREDIT, amount, event);
          credit.setPartnerId(partnerId);
          facts.add(credit);
      } else {
          // 报销抵扣路径（默认 / OFFSET / null）：Dr 2241 应付-员工 / Cr 1221 应收-员工（既有行为不变）
          VoucherFact debit = fact(SUBJECT_PAYABLE_EMPLOYEE, "其他应付款-员工", DC_DEBIT, amount, event);
          debit.setPartnerId(partnerId);
          facts.add(debit);
          VoucherFact credit = fact(SUBJECT_RECEIVABLE_EMPLOYEE, "其他应收款-员工预支", DC_CREDIT, amount, event);
          credit.setPartnerId(partnerId);
          facts.add(credit);
      }
  }
  ```
  - 新增 `private String asString(Object value)` helper（镜像 `asLong:86-98` 范式：null→null / String→trim / 其他→String.valueOf）。
  - **零回归保证**：`postSettle` 不设 SETTLE_TYPE → `asString(null)` 返回 null → 不 equals `SETTLE_TYPE_CASH` → 走 else 分支（既有报销抵扣路径行为不变）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinErrors` 新增 ErrorCode：`ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE`（advance 未过账/审核）+ `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID`（amount ≤ 0）+ `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING`（超额）。描述用中文（i18n 处理翻译），对齐 `ERR_EMPLOYEE_ADVANCE_NOT_FOUND` 既有范式（l.181-203）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `cashRepay` 方法签名稳定（Long advanceId, BigDecimal amount → ErpFinEmployeeAdvance）；Java `@BizMutation` 经 Nop 平台自动暴露 GraphQL 端点（无需 xbiz action 注册）；本地化 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2/3 测试与 E2E 阻塞）。
- [x] `EmployeeAdvanceAcctDocProvider` SETTLE 分支扩展不破坏既有报销抵扣路径（SETTLE_TYPE 缺省/null → OFFSET 既有行为；通过单元测试或既有 `TestErpFinEmployeeAdvancePosting` 回归验证）。

---

### Phase 2 - JUnit 后端单元/集成测试

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinEmployeeAdvanceCashRepay.java`（新建，包路径对齐 Dispatcher）
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Decision`: 测试 harness 选择——`JunitAutoTestCase` + `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`（端到端经真实 ORM/DB + `IErpFinVoucherBiz.post` 链路，对齐 `TestErpFinEmployeeAdvancePosting` 范式）。**新建文件**（非扩展既有——既有仅 `TestErpFinEmployeeAdvancePosting` + `TestErpFinEmployeeAdvanceApproval`，无 `CashRepay` 测试）。
  - Skill: `nop-testing`
- [x] `Add`: `TestErpFinEmployeeAdvanceCashRepay` 覆盖：
  - **测试 1（正路径全额还款 + 字段翻转 + 「已结清」派生投影 + 凭证行精确数值）**：setup advance(posted=true, approveStatus=APPROVED, amountFunctional=500, outstandingAmount=500, settledAmount=0, docStatus=APPROVED) → `cashRepay(advanceId, 500)` → 断言 settledAmount=500 + outstandingAmount=0 + docStatus=APPROVED（不变，对齐 Decision (c) 派生投影）+ 经 `IErpFinVoucherBiz` 反查凭证 businessType=EMPLOYEE_ADVANCE_SETTLE + voucherType=NORMAL + billHeadCode 格式 + 凭证行 Dr 1002=500 / Cr 1221=500。
  - **测试 2（部分还款 + 字段部分翻转 + docStatus 不变）**：setup advance(amountFunctional=500, outstandingAmount=500) → cashRepay(200) → 断言 settledAmount=200 + outstandingAmount=300 + docStatus=APPROVED（不变）+ 凭证行 Dr 1002=200 / Cr 1221=200。
  - **测试 3（未过账守卫）**：setup advance(posted=false, approveStatus=UNSUBMITTED) → cashRepay(200) → 抛 `ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE` + 无凭证生成 + advance 字段不变。
  - **测试 4（超额守卫）**：setup advance(outstandingAmount=300) → cashRepay(500) → 抛 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING` + 无凭证生成 + advance 字段不变。
  - **测试 5（amount=0/null 守卫）**：cashRepay(0) / cashRepay(null) → 抛 `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID`。
  - **测试 6（Provider SETTLE_TYPE 分派单元测试）**：直接 `new EmployeeAdvanceAcctDocProvider().createFacts(event, ctx)` 单测：(a) SETTLE_TYPE=CASH → Dr 1002 / Cr 1221；(b) SETTLE_TYPE=OFFSET → Dr 2241 / Cr 1221（既有报销抵扣）；(c) SETTLE_TYPE=null（未设）→ Dr 2241 / Cr 1221（默认 OFFSET 路径，零回归验证）；(d) businessType 不在 supportedTypes 时返回空列表。
  - Skill: `nop-testing`
- [x] `Proof`: `mvn test -pl module-finance/erp-fin-service -am` 全绿（既有 finance-service tests + 6 新增 0 failures/0 errors）+ 既有 `TestErpFinEmployeeAdvancePosting`（报销抵扣路径 SETTLE_TYPE=null 默认行为）零回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 6 测试用例全绿（正路径全额 + 部分还款 + 3 守卫 + provider 单元 SETTLE_TYPE 三态）；凭证行精确数值断言对齐 Phase 1 Decision（Dr 1002 / Cr 1221）。
- [x] 既有 `fin-employee-advance.action.spec.ts`（1218-2 落地 EXPENSE_CLAIM offset 路径）+ finance 抽样回归零回归（SETTLE_TYPE=null 默认路径行为不变）。

---

### Phase 3 - 浏览器层 E2E spec + 文档对齐 + 日志

Status: completed
Targets: `tests/e2e/business-actions/fin-employee-advance-cash-repay.action.spec.ts`（新建）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件计数）、`docs/plans/2026-07-14-1218-2-finance-expense-claim-employee-advance-budget-e2e.md`（Deferred RELEASED）、`docs/design/finance/expense-claim.md`（§现金还款 实现注记）、`docs/logs/2026/07-18.md`、`docs/backlog/README.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`: `fin-employee-advance-cash-repay.action.spec.ts` —— 自包含 setup（finally 兜底 cleanup），inline 复用 1218-2 spec 内 `setupEmployee`/`setupAdvance`/`cleanupCtx` 范式（如需要可提取至 `_helper.ts`，否则 inline 重用范式）：
  - **测试 1（正路径全额还款 + 「已结清」派生投影 + 凭证行精确数值）**：经 `__save` 建 ErpMdPartner(EMPLOYEE 类型) + ErpMdEmployee(partnerId=上述) + ErpFinEmployeeAdvance(amountFunctional=500, settledAmount=0, outstandingAmount=500, posted=true, approveStatus=APPROVED, employeeId, docStatus=APPROVED) → `cashRepay(advanceId, "200")` → GraphQL 返回 ErpFinEmployeeAdvance 实体（settledAmount=500, outstandingAmount=0, docStatus=APPROVED 不变）→ verifyState 经 `__get` 独立断言字段翻转（非仅 mutation 返回值）→ 经 `findVoucherIdByBillCode("EA-CASH-REPAY-{advance.code}-...", "NORMAL")` 反查凭证 + `assertVoucherLines` Dr 1002=500 / Cr 1221=500。
  - **测试 2（部分还款 + docStatus 不变）**：同 setup → cashRepay("200") → 断言 settledAmount=200 + outstandingAmount=300 + docStatus=APPROVED（不变）+ 凭证行 Dr 1002=200 / Cr 1221=200。
  - **测试 3（超额守卫）**：setup advance(outstandingAmount=300) → cashRepay("500") → GraphQL errors 含中文 message token「超出」/「未还余额」（`ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING` 对应 i18n 描述）+ advance 字段不变（verifyState 独立断言回退）。
  - **清理**：finally 删 voucher（经 cleanupVoucherByBillCode 删 `EA-CASH-REPAY-{code}-*` 凭证）+ advance + employee + partner。
  - Skill: none
- [x] `Proof`: spec 独立运行全绿（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-employee-advance-cash-repay.action.spec.ts --workers=1` 3 passed）+ finance 既有 spec 抽样回归（fin-employee-advance + fin-credit-facility + fin-notes-* + fin-reconciliation 抽样）0 新增失败。
  - Skill: none
- [x] `Add`: 文档对齐——`e2e-runbook` 业务动作表 +1 finance 现金还款行 + 凭证行断言表 +1 行（EMPLOYEE_ADVANCE_SETTLE CASH Dr 1002 / Cr 1221）+ 套件计数对齐（实测后增）；`2026-07-14-1218-2` Deferred 段补 `**RELEASED by 2026-07-18-0718-2**` 行 + 实施摘要；`expense-claim.md §现金还款` 段下补实现注记（SETTLE_TYPE 分派机制 / billHeadCode 格式 / docStatus 保持 APPROVED 派生投影规则 / 字段先于凭证持久化的失败残留风险 / 三金额闭环仍 Deferred）；`docs/logs/2026/07-18.md` 增聚合条目（背景 / Phase 1-3 / 验证状态 / 范围纪律）；`docs/backlog/README.md` +1 done 行。
  - Skill: none

Exit Criteria:

- [x] 1 spec 全绿（3 用例：正路径全额 + 派生投影 / 部分还款 + docStatus 不变 / 超额守卫）；字段翻转 + 凭证行数值均经 `verifyState` `__get` 与 `findVoucherIdByBillCode` + `assertVoucherLines` 独立反查（非仅 mutation 返回值）。
- [x] finally cleanup 保护共享 DB（finance 看板/报表数值断言基线无漂移，对齐 1218-2 自包含 employee+advance 隔离范式）。
- [x] e2e-runbook 业务动作表 + 凭证行断言表 + 套件计数对齐；1218-2 Deferred RELEASED 登记落地；expense-claim.md 实现注记就位；日志 + backlog 条目在位。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_08d9c19b9ffeJEOB4N62wX7UYK`，general agent 新会话冷审计) — 2 BLOCKERS / 4 MAJORS / 4 MINORS。13+ load-bearing 事实主张经实时仓库核实，发现 2 处事实伪 + 4 处设计 Decision 缺失：B1 `erp-fin/advance-status` 字典实测仅 5 值（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）无 SETTLED，plan 误称推进 docStatus=SETTLED 对齐字典设计；B2 `BILL_DATA_EMPLOYEE_ID` 键契约实测为 partnerId（经 `employee.partnerId` 解析）非 employeeId（`ErpFinConstants.java:90-92` Javadoc 明示），plan 误称传 employeeId 入此键 + 加冗余 PARTNER_ID 键；M1 cashRepay 实现层（BizModel Java 直落 vs xbiz→Processor 委托）未 Decision；M2 PostingEvent 构造应经 `EmployeeAdvancePostingDispatcher.postCashRepay` 镜像 postSettle，plan 误称 BizModel 直调 voucherBiz.post；M3 cashRepay 事务与失败语义未 Decision（IErpFinVoucherBiz.post REQUIRES_NEW + 幂等 + 字段更新顺序未定）；M4 `asString` helper 在 `EmployeeAdvanceAcctDocProvider` 实测不存在须新增说明。legitimately warranted PASS（1218-2 Deferred 真实存在）。
- 修订落地（iteration 1 → 2）：(1) B1 改 docStatus 保持 APPROVED 不变（对齐 owner doc §还款状态派生 派生投影语义）+ Decision (c) 重写 + JUnit/spec 断言更新；(2) B2 改 billData 携带 `BILL_DATA_EMPLOYEE_ID=partnerId`（经 `resolveEmployeePartnerId` 解析，沿用既有键契约）+ 删除冗余 PARTNER_ID 键；(3) M1 新增 Decision (a) 实现层（BizModel Java 直落 + xbiz action 暴露）；(4) M2 新增 `EmployeeAdvancePostingDispatcher.postCashRepay` 方法镜像 postSettle 范式 + BizModel 委派；(5) M3 新增 Decision (b) 失败语义（字段先于凭证持久化 + post 失败不阻断字段更新 + 残留风险记录，对齐 postSettle 范式）；(6) M4 在 Provider 扩展 item 明示新增 `asString` helper；(7) m1 改 helper 命名引用为实际 `setupEmployee`/`setupAdvance`/`cleanupCtx`；(8) m2 改测试文件为「新建」（既有无 CashRepay 测试）；(9) m3 改 BizModel 描述为「thin Facade」+ 状态机动作经 xbiz→Processor；(10) m4 改 acctSchema 解析为既有 `resolveAcctSchemaId(orgId)` helper（postSettle 范式）。
- Independent draft review iteration 2: needs-revision (`ses_08d9379caffezr3fiFjKX2f00l`，general agent 新会话冷审计) — 0 BLOCKERS / 1 NEW-MAJOR / 1 NEW-MINOR。iter-1 全部 2 BLOCKERS + 4 MAJORS + 4 MINORS 经实时仓库核实 FIXED：B1（字典实测仅 5 值 + docStatus 保持 APPROVED ✓ + owner doc 派生投影对齐 ✓）/ B2（`BILL_DATA_EMPLOYEE_ID=partnerId` 经 `resolveEmployeePartnerId` 解析 + 删除冗余 PARTNER_ID ✓ + `ErpFinConstants.java:90-92` 既有契约核实 ✓）/ M1-M4（Decision (a)(b)(c) + Dispatcher.postCashRepay 镜像 postSettle:60-82 + asString helper ✓ live 逐项核实）/ m1-m4（helper 命名 + 测试文件 + BizModel 描述 + acctSchema helper 全部订正 ✓）。但 iter-1 修订引入 1 NEW-MAJOR：NEW-MAJOR-1 xbiz action 暴露 item 不必要 + XML 语法错 + 引用不存在的 `cancel` action 范式（live 实测 `ErpFinEmployeeAdvance.xbiz` 仅 5 mutation 委托 Processor，**无 `cancel` action**——Java `@BizMutation` 经 Nop 平台自动暴露 GraphQL 端点，对齐 `ErpFinEmployeeAdvanceBizModel.java:30-34` `cancel()` 仅有 Java `@BizMutation` 无 xbiz entry 范式）+ 1 NEW-MINOR：`IErpFinEmployeeAdvancePostingDispatcher` interface 不存在（dispatcher 为具体类，对齐 `ErpFinEmployeeAdvanceProcessor` 具体类注入范式，不应 `new` 否则绕过 IoC 装配）。
- 修订落地（iteration 2 → 3）：(1) NEW-MAJOR-1 删除「xbiz action 暴露」Phase 1 Add item + 改 Decision (a) 明示「Java `@BizMutation` 经 Nop 平台自动暴露 GraphQL 端点无需 xbiz action 注册」+ 改 Phase 1 Targets 删除 xbiz 文件 + 改 Phase 1 Exit Criteria 删除「xbiz action 就位」+ 改 Closure Gate 范围列表删除「xbiz action」；(2) NEW-MINOR-1 改 BizModel 注入为具体类 `EmployeeAdvancePostingDispatcher advancePostingDispatcher`（对齐 `ErpFinEmployeeAdvanceBizModel.java:23-24` 既有 `@Inject ErpFinEmployeeAdvanceProcessor` 具体类范式）+ 删除「如不可达则...直接 new」fallback（`new` 绕过 IoC 装配致 dispatcher 自身 `@Inject` 字段失效）+ 显式标注「`@Inject` 字段不能为 `private`」对齐 AGENTS.md Nop 规则。
- Independent draft review iteration 3: needs-revision (trivial) (`ses_08d8eb2e7ffe6eWI9DQ78xafFO`，general agent 新会话冷审计) — 0 BLOCKERS / 0 MAJORS / 1 residual MINOR。iter-2 NEW-MAJOR-1 功能性修订全部落地（Phase 1 Targets / Decision (a) / Phase 1 Exit Criteria / Phase 1 无 xbiz Add item 均已订正 ✓）+ iter-2 NEW-MINOR-1 修订全部落地（注入具体类 `EmployeeAdvancePostingDispatcher advancePostingDispatcher` + `@Inject` 字段非 private + 删除 new Dispatcher fallback ✓）。iter-1 全部 10 项 B/M/m 修订稳定在位（spot check 全部 ✓）。R1-R10 + R12-R14 + anti-slack + scope + Non-Goals honesty 全 PASS。唯一残留：R11 文本一致性——Closure Gates intro 段（l.242）描述性 scope summary 仍含 stale "+ xbiz action"，与实际 gate item（l.244）+ Decision (a)（l.124）矛盾。
- 修订落地（iteration 3 → 4）：删除 Closure Gates intro 段 l.242 "+ xbiz action" stale 描述，与实际 gate item + Decision (a) 一致。
- Independent draft review iteration 4: **accept**（共识裁决——iter-3 仅 1 处文本不一致 MINOR（R11 文本一致性），功能性 + 设计层面 0 BLOCKER / 0 MAJOR；修订为 1-行删除无新风险引入；R1-R14 + anti-slack + scope + Non-Goals honesty 全 PASS；legitimately warranted 维持）。共识达成 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划触及 finance service 层生产代码（1 BizModel 新方法 + 1 Dispatcher 新方法 + 1 Provider 扩展 + 新增 helper + 3 ErrorCode + 常量）+ 测试 + 文档；Closure Gates 须含完整 `mvn` 验证 + Playwright 全套件回归。

- [x] 范围内行为完成（cashRepay @BizMutation（Java 自动暴露 GraphQL）+ Dispatcher.postCashRepay + Provider SETTLE 分支扩展 SETTLE_TYPE=CASH + asString helper + 3 ErrorCode + 常量 + JUnit 6 用例 + 1 spec 3 用例）
- [x] 相关文档对齐（e2e-runbook + 1218-2 Deferred RELEASED + expense-claim.md 实现注记 + 当日日志 + backlog）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿（含新增 6 tests）+ 新增 spec 独立运行全绿 + finance 既有 spec 抽样回归 0 新增失败（特别 fin-employee-advance + TestErpFinEmployeeAdvancePosting SETTLE_TYPE=null 默认路径零回归）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（advance-status 字典 SETTLED 值 / 三金额闭环 ORM 列 / 薪资扣回 / cashRepay 审批 / 多次累计还款 spec / 外币还款汇兑 / 特殊清算均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `erp-fin/advance-status` 字典 SETTLED 值

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 字典实测仅 5 值（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）。owner doc `§还款状态派生`（l.153-161）明示状态经金额比例**派生投影**（未还/部分/已结清）非 docStatus 字典推进。本计划保持 `docStatus=APPROVED` 不变，outstandingAmount=0 由查询/UI 派生表达「已结清」。如 UI 列表筛选/统计需显式 SETTLED 状态，归字典 successor。
- Successor Required: `yes`（触发条件：UI 列表筛选/统计需显式 SETTLED 状态时）

### 三金额闭环 ORM 字段拆分（claimedAmount/returnedAmount/settledAmount）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `expense-claim.md §借款金额维度建议`（l.143-151）明示「属 ORM 字段建议（保护区域），落地须计划批准」。本计划继续在 `settledAmount` 混合累计；三金额闭环可支持 HR 三金额报表（paid/claimed/return）但属 ORM 保护区域 + 跨模块查询面。
- Successor Required: `yes`（触发条件：HR 三金额报表/查询需求落地时）

### 薪资扣回（Additional Salary）路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `expense-claim.md §借款清算三路径` 明示「依赖 HR 薪酬扣回项」。本计划仅落地两路径（报销抵扣 + 现金还款）；薪资扣回需 HR `Additional Salary` 模块 + 跨域集成。
- Successor Required: `yes`（触发条件：HR `Additional Salary` 模块落地时）

### cashRepay 审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 现金还款属事实操作（资金已到账）不走审批；如业务需主管审批（如金额阈值审批），属不同结果面。
- Successor Required: `yes`（触发条件：现金还款需主管审批业务需求落地时）

### 外币借款现金还款汇兑损益

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本位币现金还款已覆盖核心；外币还款（员工用外币还本位币借款 / 反之）的汇兑损益分解属不同结果面 successor（对齐 EXCHANGE_GAIN_LOSS 范式）。
- Successor Required: `yes`（触发条件：外币借款业务落地时）

### 字段更新已落库但凭证缺失的残留风险

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 Decision (b) 裁决字段先于凭证持久化 + post 失败不阻断（对齐 `postSettle` 范式）。残留风险：advance 字段已翻转（settledAmount/outstandingAmount 已更新）但 `EA-CASH-REPAY-` 凭证缺失，须经 5.1 异常工作台手工补录或 cashRepay 调用方重试。同 `postSettle` 报销抵扣路径既有残留风险（已审计）。
- Successor Required: `yes`（触发条件：生产环境观察到字段已更新但凭证缺失的事故频次足以证明 postSettle 同型范式须统一改造时）

### 多次累计还款浏览器层 E2E

- Classification: `watch-only residual`
- Why Not Blocking Closure: 单次全额 + 部分还款已覆盖核心路径；多次累计还款（如 500 借款分 200+200+100 三次还清）属操作场景变体，JUnit 覆盖即可（可作测试 2 扩展）。
- Successor Required: `no`（浏览器层不必专设 spec；JUnit 累计还款测试就位即解除）

## Closure

Status Note: 全部 3 Phase 执行完毕，所有范围内 item 与 exit criteria 均 `[x]`。生产代码 + 测试 + 文档全部对齐。Closure Gates 8/8 `[x]`，独立子代理结束审计已由新会话执行并通过（APPROVED）。

Closure Audit Evidence:

- Phase 1（后端）：`mvn compile -pl module-finance/erp-fin-service -am` BUILD SUCCESS（含 erp-fin-dao install 传递新接口方法）+ 既有 `TestErpFinEmployeeAdvancePosting`/`TestErpFinEmployeeAdvanceApproval`/`TestErpFinExpenseOffsetAdvance` 11 passed 零回归（SETTLE_TYPE=null 默认路径行为不变）。
- Phase 2（JUnit）：`mvn test -pl module-finance/erp-fin-service -am` BUILD SUCCESS——206 tests, 0 failures, 0 errors（含新增 `TestErpFinEmployeeAdvanceCashRepay` 6 用例）。
- Phase 3（E2E + 文档）：`fin-employee-advance-cash-repay.action.spec.ts` 3 passed（48.3s）+ finance 抽样回归（fin-employee-advance 4 + fin-expense-claim 4 + fin-credit-facility-interest 3 = 11 passed）0 新增失败。
- Closure Gate：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:45 min）。
- 文档对齐：e2e-runbook 业务动作表 +1 行 + 凭证行断言表 +1 行 + 套件计数 74→75；1218-2 Deferred 段补 RELEASED；expense-claim.md §现金还款 补实现注记；docs/logs/2026/07-18.md 增聚合条目；docs/backlog/README.md +1 done 行。
- Auditor / Agent: independent closure auditor（新会话，未重用执行者上下文，task=gLM closure-audit 冷审计 2026-07-18）
- 语义核实（grep/read live 仓库，非采信 [x]）：(1) `ErpFinEmployeeAdvanceBizModel.cashRepay` `@BizMutation` 实体在位（`erp-fin-service/.../entity/ErpFinEmployeeAdvanceBizModel.java:52`）—— 守卫 posted+APPROVED / amount>0 / amount≤outstanding 三守卫齐全 + 字段翻转（settledAmount+=amount / outstandingAmount-=amount / docStatus 不变）+ `updateEntity` 先持久化 + 委派 `advancePostingDispatcher.postCashRepay` 返回 boolean（失败 log warn 不阻断）—— 非 hollow（实参带 NopException+ErrorCode+param，无空体/return null 占位）。(2) `EmployeeAdvancePostingDispatcher.postCashRepay`（`posting/EmployeeAdvancePostingDispatcher.java:95`）—— PostingEvent 实参（businessType=EMPLOYEE_ADVANCE_SETTLE + billHeadCode=`EA-CASH-REPAY-{code}-{millis}` + orgId/acctSchemaId/currencyId/exchangeRate/voucherDate 派生）+ billData 实参（BILL_DATA_EMPLOYEE_ID=partnerId 经 `resolveEmployeePartnerId` 解析 + TOTAL + BILL_DATA_SETTLE_TYPE=SETTLE_TYPE_CASH）+ `executor.postEvent` + catch Exception → log + return false —— 非 hollow。(3) `EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支扩展（`posting/provider/EmployeeAdvanceAcctDocProvider.java:61-79`）—— `asString` 读取 SETTLE_TYPE，CASH → Dr 1002 / Cr 1221（新路径）；else → Dr 2241 / Cr 1221（既有报销抵扣零回归）；新增 `asString(value)` helper（:119-127 镜像 asLong 范式）—— 非 hollow。(4) 3 ErrorCode 实测在位（`ErpFinErrors.java:207/211/215` NOT_REPAYABLE / AMOUNT_INVALID / EXCEEDS_OUTSTANDING，中文描述对齐 i18n 范式）。(5) 2 常量实测在位（`ErpFinConstants.java:106/108` BILL_DATA_SETTLE_TYPE / SETTLE_TYPE_CASH）。(6) JUnit `TestErpFinEmployeeAdvanceCashRepay.java` 实测在位。(7) E2E spec `fin-employee-advance-cash-repay.action.spec.ts` 实测在位（3 用例）。(8) 文档对齐实测：`docs/testing/e2e-runbook.md:262`（业务动作表 +1 finance 现金还款行）+ `:392`（凭证行断言表 +1 行 EMPLOYEE_ADVANCE_SETTLE CASH Dr 1002 / Cr 1221）+ `docs/design/finance/expense-claim.md:135`（§现金还款 SETTLE_TYPE 分派机制注记）+ `docs/plans/2026-07-14-1218-2-...md:172`（Deferred RELEASED by 2026-07-18-0718-2）+ `docs/logs/2026/07-18.md`（聚合日志条目）+ `docs/backlog/README.md:90`（done 行）。
- Anti-hollow 检查：cashRepay 方法体完整非空（守卫→字段翻转→updateEntity→委派→return）；postCashRepay 方法体完整非空（PostingEvent 构造→billData 装配→postEvent→try/catch→return boolean）；Provider SETTLE 分支两路均落地（CASH/else）；无 swallowed exception（cashRepay 守卫全抛 NopException+ErrorCode，postCashRepay catch 仅用于失败不阻断语义对齐 postSettle 范式）。
- 五点一致性：Plan Status=completed / Phase 1-3 Status=completed / 3 Exit Criteria 全 [x] / Closure Gates 8/8 [x] / Closure evidence 实测在位 + 本审计补全 Auditor 行 —— 全一致。
- Deferred honesty：6 Deferred But Adjudicated 项（字典 SETTLED 值 / 三金额 ORM 列 / 薪资扣回 / cashRepay 审批 / 外币还款汇兑 / 字段已更新凭证缺失残留风险 / 多次累计还款 spec）均附触发条件，无已确认缺陷藏匿。
- Docs sync：当日日志 + owner doc + 1218-2 RELEASED + e2e-runbook + backlog 全部实测落地，对齐 AGENTS.md。
- 审计结论：APPROVED — 计划可关闭。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
