# 2026-07-28-1249-arm-ma2-budget-commitment-release MA2 预算与承付正确性（commitment 释放路径完整性）审查（A2.16）

> Audit Status: closed
> 报告日期：2026-07-28
> 来源 plan：`docs/plans/2026-07-28-1249-2-audit-remediation-ma2-budget-commitment-release.md`
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> Owner Doc：`docs/design/finance/budget.md §承付会计 §3 接入点 + §reject release-on-receive + §CommitmentAcctDocProvider + §SPI 契约 + §配置项 + §sales 承付扩展 + §commitment 不结转`
> 范围：commitment 释放路径完整性系统性多维业务正确性审查（finance + purchase + sales 三模块跨域）。

## 1. 总体裁决

**passes multi-dimensional audit with residual risks** — 承付释放路径完整性核心契约（3 接入点 + §reject release-on-receive + 守卫覆盖 + 采购-sales 对称 + 预算控制强一致 + 聚合排除已红冲 + config-gate 边界 + 与设计文档一致性）经实仓逐项证据确认。**零 P0**（六个候选 P0 经证据证伪或降级，详见 §3 候选 P0 证伪/降级表）。**4 项新 P1**（P1-MA2-081 部分开票释放语义未声明 / P1-MA2-082 采购退货未释放承付 / P1-MA2-083 AP 发票冲销后 commitment 未恢复 / P1-MA2-084 ErpFinBudgetControlBiz 聚合实际包含 COMMITMENT 语义混淆）—— 全部不破坏主路径（commit/release 正路径 + §reject 守卫 + 重复释放守卫 + 采购-sales 对称 + config-gate 默认关闭保护）+ 按 owner doc 契约漂移 / 释放路径完整性缺口 / 代码可维护性 裁决范式 P1，**目标 MR1**（承付属 MA2 业务正确性批次，与既有 A2.1-A2.15 业务正确性 P1 同型）。**1 项新 P2** watch-only（P2-MA2-073 sales 承付 Dr/Cr 方向 voucher line 断言缺失）。MA1/MA2 finding（P1-MA1-022 / P1-MA2-001 / P1-MA2-009）运行时复核**无升级**。**并发敏感点 4 处交接 A2.17**。

下表「预算与承付」行 finance/pur/sal 列推进至 `⚠️(P1)(A2.16✅)`。

## 2. 承付释放路径完整矩阵

> 所有 commit/release 场景 × 触发点 × 事务边界 × 守卫，每个场景含通过/失败裁决与证据。

| # | 场景 | 触发点（文件:行） | 事务边界 | 守卫 | 裁决 | 证据 |
|---|------|------------------|---------|------|------|------|
| 1 | **commit (PO)** | `ErpPurOrderProcessor.runCommitmentCommitHook:223-237`（approve 后置 L95）→ `IErpFinBudgetCommitmentBiz.commit(PURCHASE_ORDER, ...)` → `CommitmentVoucherGenerator.generateCommitment:61-68` → `writeCommitmentVoucher:119-182`（Dr 承付占用 / Cr 应付-承付） | SYNC 同事务（@BizMutation；config-gated） | `isCommitmentEnabled()`（L57）+ amount/signum 守卫（L60-63）+ subject 解析 null 守卫（L64-67） | **PASS** | 与 `IErpFinBudgetControlBiz.check()` 强一致；`postingType=COMMITMENT` 落库；`isReversed=false` |
| 2 | **commit (SO)** | `ErpSalOrderProcessor.runCommitmentCommitHook:338-352`（approve 后置 L97）→ `commit(SALES_ORDER, ...)` | SYNC 同事务（config-gated） | 同 #1 | **PASS** | 镜像采购；subject 经 `CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE`（L342）独立配置 |
| 3 | **release-on-cancel (PO)** | `ErpPurOrderProcessor.runCommitmentReleaseHook:248-259`（reverseApprove L118 + cancel L130 前置）→ `release(PURCHASE_ORDER, orderCode)` | SYNC 同事务（@BizMutation + 同事务 SPI） | `hasUnreversedCommitment` 守卫（`ErpFinBudgetCommitmentBizModel:88`）+ `catch (NopException)` 容错（L255-258） | **PASS** | 反审核/作废前先红冲承付，非 NopException 触发整体事务回滚（doReverseApprove/doCancel 在 hook 之后） |
| 4 | **release-on-cancel (SO)** | `ErpSalOrderProcessor.runCommitmentReleaseHook:359-370`（reverseApprove L120 + cancel L132）→ `release(SALES_ORDER, orderCode)` | SYNC 同事务 | 同 #3 | **PASS** | 容错对称性（plan 2026-07-26-0410-2 latent defect Fix） |
| 5 | **release-on-invoice-approve (PO)** | `ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:306-325`（approve 后置 L94）→ 经 `resolveLinkedOrderCodes:333-373`（invoiceLine→receiveLine→receive→order 反查）→ 对每个唯一 order.code `release(PURCHASE_ORDER, orderCode)` | SYNC 同事务 | `hasUnreversedCommitment` 守卫 + `isCommitmentAlreadyReleased` 错误码过滤容错（L318-322，仅吞 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`） | **PASS** | 反查链去重（`HashSet<String>`）+ 严格错误码过滤 |
| 6 | **release-on-invoice-approve (SO)** | `ErpSalInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:347-365`（approve 后置 L96）→ `resolveLinkedOrderCodes:373-412`（invoiceLine→deliveryLine→delivery→order）→ 对每个 SO.code `release(SALES_ORDER, orderCode)` | SYNC 同事务 | 同 #5 | **PASS** | 镜像采购 invoice-approve 容错模式 |
| 7 | **§reject release-on-receive** | `ErpPurReceiveProcessor`（427 行全文读）—— **零** `IErpFinBudgetCommitmentBiz`/`IErpFinBudgetControlBiz` import + **零**注入字段 + **零** commit/release 调用 | N/A | owner doc budget.md:258-260 §reject 显式裁决 | **PASS** | 入库是库存移动（`IErpInvStockMoveBiz`），不产生 AP ACTUAL 占用；owner doc 裁决落实 |
| 8 | **§reject release-on-delivery (sales 对称)** | `ErpSalDeliveryProcessor`（imports/fields 全文读）—— **零** commitment SPI 接入 | N/A | 销售对称于 #7 | **PASS** | 销售出库是库存移动，收入确认经 invoice approve |
| 9 | **重复 release 守卫** | `ErpFinBudgetCommitmentBizModel.release:88-92` throw `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` | SYNC | `hasUnreversedCommitment` false 时必抛 | **PASS** | `ErpFinErrors:415-417` 错误码 + `ARG_SOURCE_BILL_TYPE/CODE` 参数齐全 |
| 10 | **取消后再发票** | PO cancel 已 release → 误 invoice approve → release → `hasUnreversedCommitment=false` 抛守卫 → `isCommitmentAlreadyReleased` 容错吞掉 | SYNC | 错误码过滤守卫（L320-322） | **PASS** | 不阻断 invoice approve 主路径；订单已 CANCELLED 经 `validateNotCancelled` 在 invoice 校验前置（采购 `ErpPurInvoiceProcessor.validateNotCancelled`） |
| 11 | **多年度跨期发票** | PO commit 在 P_N（periodId 来自 `order.businessDate`）→ 发票 release 在 N+1 年；`writeReversalFromLines:212` 红冲凭证 `setPeriodId(original.getPeriodId())`（继承原凭证 periodId P_N） | SYNC | isReversed + periodId 一致 | **PASS** | 原/红冲凭证均 periodId=P_N，isReversed=true 排除；发票 ACTUAL 落 N+1 年独立；年度余量正确 |
| 12 | **部分开票释放语义** | 一张 PO 多次部分开票：首张发票 approve 全额 release（无 amount 参数）→ 后续发票 approve 命中守卫→容错吞掉 | SYNC | 守卫齐全 + 容错齐全 | **PASS（语义未声明）** | `release()` / `reverseCommitment()` 全额红冲（无 amount 入参）；owner doc §3 接入点表未声明"全额 vs 部分释放"。**P1-MA2-081**（owner doc drift，主路径不破坏） |
| 13 | **采购退货/退款释放** | `ErpPurReturnProcessor`（397 行）—— **零** commitment SPI；经 `PurReturnPostingDispatcher` 过账 PURCHASE_RETURN ACTUAL + `IErpInvStockMoveBiz` 库存 outgoing | SYNC | 无 | **FAIL（释放路径缺口）** | **P1-MA2-082**——owner doc §3 接入点表未声明退货释放；commitment 保持全额占用未对应实际减少的采购量；config-gated + 业务语义模糊 |
| 14 | **AP 发票冲销后 commitment 恢复** | `ErpPurInvoiceProcessor.reverseApprove:106-121` + `cancel:123-137` 仅 `postingDispatcher.reverse(invoice)` 红冲 AP ACTUAL；**不调** `commit()` 恢复承付 | SYNC | 无 | **FAIL（跨冲销一致性缺口）** | **P1-MA2-083**——不对称（approve release / reverseApprove 不 restore）；actual 减少但 commitment 保持已释放，余量偏移；`ErpSalInvoiceProcessor` 同型 |
| 15 | **release hook 容错对称性** | `ErpPurOrderProcessor.runCommitmentReleaseHook:255-258` catch 所有 NopException（broad 容错）vs `ErpPurInvoiceProcessor:318-322` 仅 catch `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（narrow 容错） | SYNC | 容错范围不对称 | **PASS（不对称可接受）** | cancel 路径需 broad 容错（任意 NopException 不阻断取消）；invoice-approve 路径需 narrow 容错（仅守卫静默，其他异常传播触发事务回滚保护 posted=true 一致性）。plan 2026-07-26-0410-2 已 fix 采购侧对称性 |

## 3. 候选 P0 证伪/降级表

> 计划 `Current Baseline` 列出的六个候选 P0 经证据逐一裁决，全部降级 P1 或证伪。

| 候选 P0 | 裁决 | 证据 |
|---------|------|------|
| 部分开票全额释放致未开票部分占用过早释放 | **降级 P1-MA2-081** | (1) `ErpPurInvoiceProcessor:318-322` 显式容错，后续发票 approve 不阻断；(2) 业务语义可接受（实际占用产生时全额释放，避免 actual+commitment 双重占用预算）；(3) 仅破坏"精确部分占用"语义，未破坏预算控制硬约束（HARD 模式仍校验 actual+commitment ≤ budget）；(4) owner doc §3 接入点表未声明语义 |
| release-on-receive 误释放致 actual+commitment 双重占用 | **证伪** | `ErpPurReceiveProcessor` 427 行全文读零 commitment SPI 接入（import + field + call 三层证据）—— §reject 落实 |
| 采购退货/退款未释放承付致 commitment 泄漏 | **降级 P1-MA2-082** | (1) config-gated 默认关闭；(2) 退货减少库存但不增加 AP ACTUAL 占用；(3) 业务语义模糊（owner doc 未声明退货后是否恢复预算占用）；(4) 余量偏移方向是"承付保持占用"（保守，非"超预算放行"危险方向） |
| AP 发票冲销后 commitment 未恢复致余量永久偏移 | **降级 P1-MA2-083** | (1) config-gated 默认关闭；(2) 业务语义复杂（冲销后是恢复承付还是保持释放，owner doc 未声明）；(3) 余量偏移方向是"承付保持已释放"（开放预算，需人工把关）；(4) 同 finance P1-MA2-032 / hr P1-MA2-048 / assets P1-MA2-060 posting-悬挂同型根因（事务回滚不覆盖跨业务步骤悬挂） |
| 取消后再开票绕过守卫致裸 voucher 操作 | **证伪** | `ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:318-322` catch 后严格错误码过滤（`isCommitmentAlreadyReleased` 仅匹配 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`），其他异常重新抛出；无裸 `updateEntity` 绕过 `IErpFinBudgetCommitmentBiz.release`（production 代码无 `daoFor(ErpFinVoucher.class).updateEntity` 在 finance 域外，已确认——见 ma2-finance-posting-voucher-state-machine.md:259） |
| sales 承付 Dr/Cr 方向经 subject.direction 错误致收入预算余量符号反转 | **证伪** | `CommitmentVoucherGenerator.resolveDcDirection:267-271` 经 `subject.direction` 自动取（CREDIT→credit 侧 / DEBIT→debit 侧）；sales subject 经 `CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE`（独立配置）+ `TestErpSalOrderCommitment` 测试种子 subject `direction="CREDIT"` + `subjectClass="INCOME"`（test:238-245）；结构对称已强制。**残留风险**：测试缺 Dr/Cr 方向 voucher line 断言（P2-MA2-073 watch-only） |

## 4. 8 维度裁决（multi-dimensional-audit 反窄化自检）

### 4.1 维度「释放路径完整性（核心）」 — ⚠️ PASS（3 接入点齐全 + 3 路径缺口登记 P1）

**(1) 全额发票过账释放（ErpPurInvoice.approve）**：✅ PASS —— 见矩阵 #5。
**(2) 订单取消释放（reverseApprove/cancel）**：✅ PASS —— 见矩阵 #3。
**(3) 部分开票释放语义**：⚠️ owner doc 未声明 —— `release()` 全额红冲（`CommitmentVoucherGenerator.reverseCommitment:77-94` 全额循环所有 unreversed original）+ `ErpFinBudgetCommitmentBizModel.release` 无 `amount` 入参。owner doc budget.md:252-256 §3 接入点表未声明全额 vs 部分释放语义。**P1-MA2-081**（owner doc drift，主路径不破坏——容错守卫齐全）。
**(4) 采购退货/退款释放**：❌ FAIL —— `ErpPurReturnProcessor` 不调 commitment SPI。**P1-MA2-082**。
**(5) AP 发票冲销（reverse）后 commitment 恢复**：❌ FAIL —— `ErpPurInvoiceProcessor.reverseApprove/cancel` 不调 `commit()` 恢复。**P1-MA2-083**。
**(6) 多年度跨期发票余量一致性**：✅ PASS —— 见矩阵 #11。

无"永不释放"commitment 泄漏（核心路径全覆盖）；3 个边界场景释放语义/恢复语义缺口登记 P1。

### 4.2 维度「误释放防护」 — ✅ PASS

**(1) release-on-receive 误释放**：✅ PASS —— 见矩阵 #7（§reject 落实）。
**(2) 重复释放**：✅ PASS —— `ErpFinBudgetCommitmentBizModel.release:88-92` 守卫齐全 + `ErpPurInvoiceProcessor:318-322` 容错严格过滤 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`，不吞其他异常。
**(3) 取消后再发票**：✅ PASS —— 见矩阵 #10。

### 4.3 维度「采购-sales 对称性」 — ✅ PASS（残留测试覆盖缺口 P2-MA2-073）

- **sourceBillType 对称**：PURCHASE_ORDER / SALES_ORDER 经 `resolveCommitmentBillType:107-117` 派发独立 billType（PURCHASE_ORDER_COMMITMENT / SALES_ORDER_COMMITMENT），commit/reverse/find 三路径同 billType 保证占用/释放 lookup 对称。
- **配置键独立**：`CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE`（采购，ErpFinConstants:414）vs `CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE`（sales，:416）独立配置。
- **Dr/Cr 方向经 subject.direction**：`CommitmentVoucherGenerator.resolveDcDirection:267-271` 经 `subject.direction` 自动取（CREDIT→credit / DEBIT→debit），无 side-specific flag。
- **容错模式对称**：cancel-path broad 容错（catch all NopException）/ invoice-approve-path narrow 容错（仅 catch ERR_BUDGET_COMMITMENT_ALREADY_RELEASED），采购-sales 两域字节级镜像（modulo 类名）。
- **残留风险**：`TestErpSalOrderCommitment` 仅断言 postingType/billType/isReversed，未断言 totalDebit/totalCredit/dcDirection —— **P2-MA2-073 watch-only**（结构对称已强制，仅缺回归保护）。

### 4.4 维度「预算控制一致性」 — ✅ PASS

- **commit 与 budget check 同事务**：`ErpPurOrderProcessor.approve` 内 `runCommitmentCommitHook` (L95) 与潜在 `IErpFinBudgetControlBiz.check()` 调用经 @BizMutation 同事务（commit 与 check 在同一 approve 内顺序执行）。
- **release 与 reverseApprove/invoice approve 同事务**：见矩阵 #3/#4/#5/#6（SPI 是 @BizMutation + 同事务，approve 是 @BizMutation + 同事务，嵌套调用同事务）。
- **跨事务失败**：@BizMutation 自动事务回滚保证 commit/release 失败时业务单据回滚至 SUBMITTED（与 P1-MA2-032 / P1-MA2-048 / P1-MA2-060 同型根因——但承付 release 经 try-catch 容错隔离，不进入"posted=false 悬挂"模式）。

### 4.5 维度「聚合与余量正确性」 — ⚠️ PASS（条件性，P1-MA2-084 标注语义混淆）

- **isReversed 标记排除落实**：`ErpFinBudgetControlBiz.aggregateAmount:109` `eq("isReversed", Boolean.FALSE)` 排除已红冲凭证；`ErpFinBudgetScenarioProcessor.aggregateActualForLine:363` 同排除。
- **availableAmount = budget − actual − commitment 实时计算**：⚠️ `ErpFinBudgetControlBiz.aggregateAmount:113` budget=false 分支**仅排除 BUDGET，不排除 COMMITMENT**——COMMITMENT 凭证金额流入 `actualBalance`。**等价正确**（`available = budgetBalance − actualBalance` ≡ `available = budget − (actual + commitment)`，commitment 经"actual"通道被减去）但**语义混淆**（变量名 `actualBalance` 实际含 `actual + commitment`）。**风险**：未来维护者若"修正"排除 COMMITMENT 会破坏预算控制（commitment 不再被减去 → 超预算放行）。**P1-MA2-084**（代码可读性 + 跨路径一致性缺口，结果当前正确）。
- **budgetLine.commitmentAmount = Σ Commitment 凭证派生**：`ErpFinBudgetScenarioProcessor.aggregateActualForLine:370-373` 正确排除 COMMITMENT（结转时 actual 与 commitment 分别处理——commitment 不结转 per budget.md:236）。**两聚合路径在 COMMITMENT 处理上不对称但语义自洽**（check 路径合并 actual+commitment / carryForward 路径分离 actual 单独）。

### 4.6 维度「守卫覆盖」 — ✅ PASS

- **`ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 覆盖所有 release 路径**：`ErpFinBudgetCommitmentBizModel.release:88-92` 唯一入口，无绕过。
- **无裸 voucher 操作**：production 代码 grep `daoFor(ErpFinVoucher.class).updateEntity` 在 finance 域外零命中（已由 ma2-finance-posting-voucher-state-machine.md:259 确认）；finance 域内 `CommitmentVoucherGenerator:89` 是 config-gated owner-doc-裁定合法 bypass（不走 Provider 路由）。
- **`releaseIfPresent` 是 dead code on impl**：`ErpFinBudgetCommitmentBizModel.releaseIfPresent:101-113` 未声明在 `IErpFinBudgetCommitmentBiz` 接口，purchase/sales 注入接口类型无法 reach，所有 caller 经 try-catch 包装 `release()`。归代码清理（不破坏正确性，不单独登记 P1）。

### 4.7 维度「config-gate 边界」 — ✅ PASS（fail-fast 缺口 P2 watch-only）

- **总开关默认 false**：`ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED = "erp-fin.budget-commitment-enabled"`（:412）默认 `Boolean.FALSE`（`ErpFinBudgetCommitmentBizModel.isCommitmentEnabled:115-117` + 所有 4 个 hook 方法 L224/L249/L307/L339/L348 都校验）—— 保护既有 113 purchase 测试不触发承付凭证。
- **启用前/后行为一致**：config-gate 经 `if (!enabled) return null/early-return` 守卫所有 6 个接入点 + 所有 hook 方法，无悬挂副作用。
- **113 purchase 测试在默认关闭下不触发承付凭证**：✅ PASS —— config 默认 false + E2E 启用经 webServer JVM arg（plan 2026-07-26-0410-2）。
- **启用后回归**：✅ PASS —— `TestErpPurOrderCommitment` + `TestErpSalOrderCommitment` + `TestErpFinBudgetCommitment` 服务层 + `fin-commitment-accounting.action.spec.ts` 浏览器层覆盖。
- **科目配置缺失时 fail-fast**：⚠️ 部分实现 —— `ErpFinErrors.ERR_BUDGET_COMMITMENT_SUBJECT_NOT_CONFIGURED`（:423-424）已定义但 production 代码从不抛出；`ErpPurOrderProcessor.resolveBudgetSubjectId:372-383` subject 不存在时返回 null，`runCommitmentCommitHook:228-229` silent skip（不 fail-fast）。owner doc budget.md:291「启用采购承付时必填」未严格强制。归 P2 watch-only（不登记单独 finding——config-gate 主路径不破坏，仅缺严格 fail-fast）。

### 4.8 维度「与设计文档一致性」 — ✅ PASS（owner doc 未声明部分由 P1-MA2-081~083 标注）

**(1) §3 三接入点（commit / release-on-cancel / release-on-invoice-approve）落地**：✅ PASS —— 见矩阵 #1-6。
**(2) §reject release-on-receive 显式裁决（ErpPurReceive.approve 不释放）**：✅ PASS —— 见矩阵 #7。
**(3) §sales 承付扩展对称（plan 2026-07-24-1351-3）**：✅ PASS —— 见 §4.3。
**(4) §配置项默认值**：✅ PASS —— 3 config keys（`budget-commitment-enabled` 默认 false / `budget-commitment-subject-code` 必配 / `budget-commitment-sales-subject-code` sales 必配）全部命中。
**(5) §commitment 不结转（Deferred successor，余量计算影响）**：✅ PASS —— `ErpFinBudgetScenarioProcessor.aggregateActualForLine:370-373` 正确排除 COMMITMENT（结转时 commitment 不一并结转，per budget.md:236）；commitment 与 actual 合并记录在源 Scenario 余量计算（源 Scenario CLOSED 终态保留审计轨迹）。

**未声明部分**：§3 接入点表 / §reject / §sales 承付扩展 均未声明部分开票释放语义 / 采购退货释放 / AP 冲销恢复—— 由 P1-MA2-081~083 标注（MR1 owner doc 补注 + 业务裁决）。

### 4.9 反窄化自检

8 维度（释放完整性 / 误释放防护 / 对称性 / 预算控制一致性 / 聚合余量 / 守卫覆盖 / config-gate 边界 / 与设计文档一致性）至少一句裁决（含"本维度无 P0/P1 发现"——本审计无完全无发现的维度，但所有维度主路径 PASS）。**未窄化为单维深挖**。

## 5. P0 / P1 / P2 finding 清单

### 5.1 P0（即时通道）

**零 P0** —— 见 §3 候选 P0 证伪/降级表。

### 5.2 P1（待 MR1 批量修复）

| Finding ID | 域 | 描述 | 严重性 | 目标 MR | 修复方式 |
|-----------|---|------|-------|--------|---------|
| `P1-MA2-081` | finance+purchase | **部分开票释放语义未声明（owner doc drift + 测试覆盖缺口）**：`ErpFinBudgetCommitmentBizModel.release:80-99` + `CommitmentVoucherGenerator.reverseCommitment:77-94` 全额红冲（无 `amount` 入参），一张 PO 多次部分开票时首张发票 approve 全额释放，后续发票 approve 经 `ErpPurInvoiceProcessor:318-322` 容错吞掉。owner doc `budget.md §承付会计 §3 接入点表:252-256` 未声明"全额释放 vs 部分释放"语义。**P1 非 P0**：(1) 容错守卫齐全不阻断主路径；(2) 业务语义可接受（实际占用产生时全额释放避免 actual+commitment 双重占用）；(3) 仅破坏"精确部分占用"语义未破坏 HARD 预算控制硬约束。按 owner doc 契约漂移裁决范式 P1 | major（owner doc drift，主路径不破坏） | MR1 | 方案 A（推荐）owner doc `budget.md §承付会计 §3 接入点表` 补注「release 全额释放语义（实际占用产生时全额释放承付，部分开票场景下未开票部分占用随首张发票全额释放，commitment 不再精确追踪未开票占用）」+ 补"部分开票多次发票"测试断言容错路径；方案 B 实现按开票金额比例部分释放（须 SPI 加 amount 入参 + reverseCommitment 重构 + 跨年度/冲销/退货恢复语义一并设计，工作量大） |
| `P1-MA2-082` | purchase+finance | **采购退货/退款未释放承付致 commitment 泄漏（释放路径完整性缺口）**：`ErpPurReturnProcessor`（397 行）不调 `IErpFinBudgetCommitmentBiz`，经 `PurReturnPostingDispatcher.tryPost` 过账 PURCHASE_RETURN ACTUAL + `IErpInvStockMoveBiz` 库存 outgoing 移动，**不释放**原 PO 的承付。owner doc `budget.md §承付会计 §3 接入点表:252-256` 仅列三个接入点（commit / release-on-cancel / release-on-invoice-approve），未声明退货释放路径。若 PO 已 commit + 部分发货后退货（部分开票前），承付保持全额占用未对应实际减少的采购量。**P1 非 P0**：(1) config-gated 默认关闭；(2) 退货减少库存但不增加 AP ACTUAL 占用；(3) 业务语义模糊（owner doc 未声明退货后是否恢复预算占用）；(4) 余量偏移方向是"承付保持占用"（保守，非"超预算放行"危险方向）。按释放路径完整性缺口裁决范式 P1 | major（释放路径缺口，保守方向偏移） | MR1 | 方案 A（推荐）owner doc `budget.md §承付会计 §3` 补第 4 接入点「release-on-return」+ `ErpPurReturnProcessor.approve` config-gated 调 `release(PURCHASE_ORDER, poCode)`；方案 B owner doc 标注「采购退货不释放承付（实际占用经 AP_INVOICE 过账时全额释放，退货库存减少经期末人工调整预算）」为已知简化 |
| `P1-MA2-083` | purchase+sales+finance | **AP/AR 发票冲销后 commitment 未恢复致余量永久偏移（跨冲销一致性缺口）**：`ErpPurInvoiceProcessor.reverseApprove:106-121` + `cancel:123-137` 仅 `postingDispatcher.reverse(invoice)` 红冲 AP ACTUAL 凭证，**不调** `commit()` 恢复承付。系统不对称：invoice approve → release commitment，invoice reverseApprove → AP ACTUAL 回退但 commitment 保持已释放。结果 `availableAmount = budget − actual − commitment` 显示 actual 减少（good）但 commitment 仍为零（bad），预算余量看起来"释放"了，可能允许超预算放行新订单。`ErpSalInvoiceProcessor` 同型（sales 侧 invoice 冲销也不恢复 sales 承付）。**P1 非 P0**：(1) config-gated 默认关闭；(2) 业务语义复杂（冲销后是恢复承付还是保持释放，owner doc 未声明）；(3) 同 finance P1-MA2-032 / hr P1-MA2-048 / assets P1-MA2-060 posting-悬挂同型根因（事务回滚不覆盖跨业务步骤悬挂）。按跨冲销一致性缺口裁决范式 P1 | major（跨冲销不对称，开放预算方向偏移） | MR1 | 方案 A owner doc `budget.md §承付会计 §3` 补「冲销恢复」语义 + `ErpPurInvoiceProcessor.reverseApprove/cancel` + `ErpSalInvoiceProcessor.reverseApprove/cancel` config-gated 调 `commit()` 恢复承付（按发票关联 PO/SO 反查）；方案 B（推荐）owner doc 标注「发票冲销不恢复承付（保守方向：保持已释放状态，避免与多发票累积释放冲突），需运营手工 commit 恢复或期末人工调整」为已知简化 |
| `P1-MA2-084` | finance | **`ErpFinBudgetControlBiz.aggregateAmount` 实际聚合包含 COMMITMENT postingType（语义混淆，等价正确但脆弱）**：`ErpFinBudgetControlBiz.java:113` budget=false 分支只排除 BUDGET，**不排除 COMMITMENT**，因此 COMMITMENT 凭证金额流入 `actualBalance`。`available = budgetBalance − actualBalance` 实际等价于 `available = budget − (actual + commitment)`——**结果正确**（commitment 经"actual"通道被减去）但**语义混淆**：变量名 `actualBalance` 实际包含 `actual + commitment`。与 `ErpFinBudgetScenarioProcessor.aggregateActualForLine:370-373`（carry-forward 时正确排除 COMMITMENT）不对称。**风险**：未来维护者若"修正"`ErpFinBudgetControlBiz.aggregateAmount` 排除 COMMITMENT，会破坏预算控制（commitment 不再被减去，超预算放行）。owner doc `budget.md §承付会计 §聚合`未显式说明 `check()` 路径的实际聚合语义。**P1 非 P0**：(1) 当前结果正确；(2) 仅语义清晰性 / 可维护性缺陷 + 跨路径一致性缺口。按代码可维护性裁决范式 P1 | major（可维护性 + 跨路径一致性，结果当前正确） | MR1 | 方案 A（推荐）`ErpFinBudgetControlBiz.aggregateAmount` 显式三通道分离（`budgetBalance` / `actualBalance` / `commitmentBalance`）+ `available = budget − actual − commitment` 显式三段计算（与 owner doc budget.md:59 `availableAmount = budget − commitment − actual` 公式严格对齐）+ 注释解释为何 carry-forward 路径排除 COMMITMENT；方案 B owner doc `budget.md §聚合` 补注「`check()` 路径 actualBalance 含 commitment 贡献，等价 budget − (actual+commitment)」+ 代码注释标注 |

### 5.3 P2 watch-only（不阻塞 MR）

| Finding ID | 域 | 描述 | 处置 |
|-----------|---|------|------|
| `P2-MA2-073` | sales | **TestErpSalOrderCommitment 缺 Dr/Cr 方向 voucher line 断言**：`TestErpSalOrderCommitment.java:77-149` 三个 @Test 方法仅断言 `postingType=COMMITMENT` / `billType=SALES_ORDER_COMMITMENT` / `isReversed=false/true`，未断言 `voucher.totalDebit/totalCredit` 或 `voucherLine.dcDirection`（sales 应 `totalCredit > 0 && totalDebit == 0` + `dcDirection == "CREDIT"`）。Dr/Cr 方向对称经结构强制（同 `CommitmentVoucherGenerator` + `subject.direction` 自动取），但未来 subject 方向回归（如 reseed 为 DEBIT 或破坏 `resolveDcDirection`）不被当前测试套件在 sales 集成边界捕获。与 P2-MA2-033（红字凭证可再红冲负向测试缺失）同型——结构阻断 + 缺回归保护 | watch-only，MR1 顺手——`TestErpSalOrderCommitment` 补 `assertEquals(0, voucher.getTotalDebit().doubleValue())` + `assertTrue(voucher.getTotalCredit().signum() > 0)` + voucher line dcDirection 断言；可选补 `TestErpPurOrderCommitment` 对照（采购应 totalDebit > 0 + dcDirection == "DEBIT"） |

## 6. MA1/MA2 finding 运行时影响复核表

| Finding ID | 原登记描述 | 本审计运行时复核 | 升级评估 |
|-----------|-----------|----------------|---------|
| `P1-MA1-022` | 跨域只读 IDaoProvider 通用模式（pur+sal+... 9 域：`ErpPurOrderProcessor:302,314` + `ErpSalOrderProcessor:377,389` 等 `daoFor(ErpMd*/ErpFin*)` 跨域查询） | 承付接入点路径：`ErpPurOrderProcessor.resolveBudgetSubjectId` / `resolvePeriodId` + `ErpSalOrderProcessor.resolveBudgetSubjectId` / `resolvePeriodId` 纯只读查询返回 subjectId/periodId 用于 commit hook 参数装配。config-gated `erp-fin.budget-commitment-enabled` 默认 false 关闭时不进入此路径。无写、无状态变更、无脏读。承付主路径行为正确 | **无升级**——维持治理层 finding，MR1 迁移至 I*Biz 便捷只读方法 |
| `P1-MA2-001` | 暂估应付冲回缺失（GRNI） | 承付 release-on-invoice-approve 在 `ErpPurInvoice.approve` 经 `postingDispatcher.tryPost` 过账 AP_INVOICE ACTUAL + 同事务 release COMMITMENT。GRNI 冲回语义边界：AP_INVOICE 过账不红冲关联 receive 的 PURCHASE_INPUT 凭证（GRNI 冲回缺失），但承付 release 红冲原 PO 的 COMMITMENT 凭证——两者**正交**（承付释放是预算面，GRNI 冲回是 GL 实际数面）。承付 release 路径不依赖 GRNI 冲回，GRNI 缺口不影响承付释放完整性 | **无升级**——承付释放与暂估冲回语义边界正交，P1-MA2-001 维持 MR1 GRNI 修复 |
| `P1-MA2-009` | O2C 多币种 + 收款核销汇兑损益未实现 | 承付 sales 对称（`ErpSalOrderProcessor.runCommitmentCommitHook:338-352`）使用 `order.getTotalAmountWithTax()` 作为 commit 金额——单币种场景下与实际开票金额币种一致。多币种 O2C 场景下承付金额的本位币折算与 AR ACTUAL 折算路径是否对称，无 E2E 证据。但属 P1-MA2-009 多币种 O2C 整体缺口的一部分，承付侧不引入新缺口 | **无升级**——维持 P1-MA2-009 MR1 整体裁决 |

## 7. 并发敏感点交接 A2.17

> 4 处并发敏感点归 A2.17 系统性并发与乐观锁审计。本审计仅标注，不做系统性并发正确性裁决。

1. **并发 commit 同一订单**：`IErpFinBudgetCommitmentBiz.commit` 无幂等键——若 `ErpPurOrder.approve` 并发触发（理论上状态机会阻止，但 race condition 下），可能生成两份 COMMITMENT 凭证。归 A2.17 并发 commit/release 同事务竞争。
2. **并发 release 同一订单**：`hasUnreversedCommitment` + `reverseCommitment` 非原子——两并发 release 可能同时通过守卫然后双红冲（红冲凭证 isReversed=true 自我排除，但原凭证可能被双 updateEntity `setIsReversed(true)`）。归 A2.17。
3. **部分开票并发释放**：一张 PO 多张部分发票并发 approve——经 `ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook` 容错守卫（`ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 静默吞），不破坏但 race 下行为非确定性（哪张发票红冲原凭证取决于调度）。归 A2.17。
4. **`ErpFinVoucher` versionProp**：`ErpFinVoucher` 声明 `versionProp="version"` 透明乐观锁——并发 release 同一原凭证的 `setIsReversed(true)` 经 stale 异常 detectable conflict 降级 silent lost-update（与 ma2-finance-arap-settlement-state-machine.md P2-MA2-008/014 降级同型）。归 A2.17。

## 8. 与已登记 finding 的交叉关系

- **P1-MA2-081 / P1-MA2-082 / P1-MA2-083** 同属"承付释放路径边界场景缺口"主题，MR1 修复时建议**整体裁决**（owner doc §3 接入点表统一补注 + 实现路径分别决定）。
- **P1-MA2-084** 与 P1-MA2-017~022（期末结账批次）+ P1-MA2-031/032（凭证/期间状态机）属 finance 聚合/语义清晰性主题，MR1 修复时建议与 P1-MA2-001 GRNI 冲回一并裁决（GL/预算面语义统一）。
- **P2-MA2-073** 与 P2-MA2-033（红字凭证负向测试缺失）同属"结构已阻断 + 缺回归保护"主题，MR1 顺手补测试。

## 9. 与 owner doc 一致性的总结

owner doc `budget.md §承付会计 §3 接入点表 + §reject release-on-receive + §CommitmentAcctDocProvider + §SPI 契约 + §配置项 + §sales 承付扩展 + §commitment 不结转` 全部经证据确认：

| owner doc 条款 | 实现 | 裁决 |
|--------------|------|------|
| §3 接入点表（commit / release-on-cancel / release-on-invoice-approve） | 矩阵 #1-6 | ✅ PASS |
| §reject release-on-receive（ErpPurReceive.approve 不释放） | 矩阵 #7 | ✅ PASS |
| §CommitmentAcctDocProvider（no-op stub provider） | `CommitmentAcctDocProvider.java` 45 行 no-op | ✅ PASS（owner doc 已裁定"不走 Provider 路由"，与 ma2-finance-posting-voucher-state-machine.md:259 一致） |
| §SPI 契约（commit/release SYNC 同事务 + ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 守卫） | `IErpFinBudgetCommitmentBiz:40-69` + `ErpFinBudgetCommitmentBizModel:54-99` + `ErpFinErrors:415-417` | ✅ PASS |
| §配置项（3 keys 默认值） | `ErpFinConstants:411-416` | ✅ PASS |
| §sales 承付扩展（plan 2026-07-24-1351-3，对称镜像） | 矩阵 #2/#4/#6 + §4.3 | ✅ PASS |
| §commitment 不结转（Deferred successor） | `ErpFinBudgetScenarioProcessor.aggregateActualForLine:370-373` 排除 COMMITMENT | ✅ PASS |
| §3 接入点表 未声明：部分开票释放语义 / 采购退货释放 / AP 冲销恢复 | 矩阵 #12/#13/#14 | ⚠️ P1-MA2-081~083 待 owner doc 补注 |

## 10. 残留风险

1. **P1-MA2-081~084 全部 P1 不阻塞主路径**——commit/release 正路径 + §reject 守卫 + 重复释放守卫 + 采购-sales 对称 + config-gate 默认关闭 全部 PASS。MR1 修复前生产启用承付（config=true）需运营知晓 4 项 P1 边界场景（部分开票/采购退货/AP 冲销/聚合语义）。
2. **P2-MA2-073 测试覆盖缺口**——结构对称已强制，仅缺回归保护，MR1 顺手补断言。
3. **并发敏感点交接 A2.17**——4 处并发缺口归系统性并发审计。
4. **Deferred successor**：「commitment 一并结转」「多公司合并预算」「预算物化快照表」「预算编制工作流」（owner doc budget.md §Deferred successor 已裁定）本审计仅确认其在释放路径完整性上不引入悬挂。

## 11. Scope Matrix 行终态

下表「预算与承付」行 finance/pur/sal/prj 列推进至 `⚠️(P1)(A2.16✅)`：

- finance 列：既有 ⚠️P1（来自 A2.5b 状态机审查）→ 维持 ⚠️P1（A2.16✅）（无 P0，4 项 P1 中 3 项触及 finance 余量计算/聚合语义）
- pur 列：❓ → ⚠️(P1)(A2.16✅)（P1-MA2-081/082/083 三项触及 purchase 释放路径）
- sal 列：❓ → ⚠️(P1)(A2.16✅)（P1-MA2-083 sales 对称 + P2-MA2-073 sales 测试覆盖）
- prj 列：维持 ❓（A2.16 不触及 projects 域）

## 12. 矩阵更新

`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2 「预算与承付」行：finance 列维持 `⚠️P1` + pur/sal 列由 `❓` 推进至 `⚠️(P1)(A2.16✅)`（详见报告 §1 总体裁决 + §11 scope matrix 行终态）。

`docs/audits/arm-index.md`：新增本报告行 + 新增 4 项 P1（P1-MA2-081~084）+ 1 项 P2（P2-MA2-073）。
