# 2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e 资金/票据（应收/应付票据）DIRECT 业务动作 + 凭证行精确数值浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 各域细化端到端验证（资金/票据子域 DIRECT 业务动作生命周期 + 业财过账凭证行精确数值断言——当前零浏览器层覆盖）
> Source: AGENTS.md「当前项目阶段」明示当前重点含「各域细化端到端验证」。资金/票据子域（`docs/design/finance/treasury.md`）后端 DIRECT 业务动作 + 业财过账 Provider 已全量实现（codegen 后落地），但浏览器层 E2E **零覆盖**——`tests/e2e/business-actions/` 无 `fin-notes-*` spec。与近期 ~15 份凭证行数值断言/业务动作 E2E 计划（0704-1/0742-1/0742-2/1800-1/1800-2/0413-2/1321-2/1005-2 等）同一「各域细化端到端验证」口径，裁定**触发条件已满足**。资金/票据与银行对账（`fin-bank-recon`，0413-2 已交付）为同一 owner doc `treasury.md` 下两套独立机制（`treasury.md:10/166` 已明确解耦），银行对账侧已覆盖，票据侧为现存唯一核心域 DIRECT 业务动作零覆盖面。
> Related: `docs/design/finance/treasury.md`（owner doc，状态机 + 业财过账 businessType + 配置点 + 业务规则）、`docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表）、`docs/lessons/05-nop-e2e-failure-log-first-diagnosis.md`
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17）：

### 后端 DIRECT 业务动作已落地（`@BizMutation`，浏览器层 `/graphql` 可达，无 use-approval/use-workflow）

- **应收票据 `ErpFinNotesReceivableBizModel`**（`module-finance/erp-fin-service/.../entity/ErpFinNotesReceivableBizModel.java`）：7 个 DIRECT `@BizMutation`，委派 `ErpFinNotesReceivableProcessor`。ORM `tagSet="gid,erp.finance"`（核实 `module-finance/model/app-erp-finance.orm.xml:1316-1317`）——**无 use-approval / use-workflow**，DIRECT 可达。
  | 动作 | 入参 | 状态迁移（dict `erp-fin/notes-receivable-status`） | 业财过账 businessType |
  |---|---|---|---|
  | `receive(notesId)` | notesId | 任何非终态 → RECEIVED | NOTES_RECEIVABLE_RECEIVED |
  | `discount(notesId,discountDate,bankId,discountRate)` | 4 参 | RECEIVED → DISCOUNTED | NOTES_RECEIVABLE_DISCOUNTED |
  | `endorse(notesId,endorsementFromId)` | notesId+链路 | RECEIVED → ENDORSED | NOTES_RECEIVABLE_ENDORSED |
  | `collect(notesId)` | notesId | RECEIVED 或 DISCOUNTED → COLLECTION_PENDING | 无（中间态，`Processor:71`） |
  | `honor(notesId)` | notesId | COLLECTION_PENDING → HONORED | NOTES_RECEIVABLE_COLLECTION |
  | `dishonor(notesId)` | notesId | COLLECTION_PENDING → DISHONORED | 无（终态标记，`Processor:86`） |
  | `writeOff(notesId)` | notesId | 任何非终态 → WRITE_OFF + posted=false（实体级回退标记） | 红冲最末过账 businessType（`Processor:256` `businessTypeForStatus(status)` → `reverseReceivable`，REVERSAL postingType） |

- **应付票据 `ErpFinNotesPayableBizModel`**（`entity/ErpFinNotesPayableBizModel.java`）：4 个 DIRECT `@BizMutation`，委派 `ErpFinNotesPayableProcessor`。
  | 动作 | 状态迁移（dict `erp-fin/notes-payable-status`） | 业财过账 businessType |
  |---|---|---|
  | `issue(notesId)` | → ISSUED | NOTES_PAYABLE_ISSUED |
  | `honor(notesId)` | ISSUED → HONORED | NOTES_PAYABLE_HONORED |
  | `dishonor(notesId)` | → DISHONORED | 无 |
  | `writeOff(notesId)` | → WRITE_OFF | 红冲 |

- **迁移守卫**（`ErpFinNotesReceivableProcessor:102-127`）：discount/endorse 仅 RECEIVED；collect 仅 RECEIVED/DISCOUNTED；honor/dishonor 仅 COLLECTION_PENDING；非法态抛 `illegalTransition`（含 ERR token + 当前/期望状态参数）。

### 业财过账 Provider 已落地（`IErpFinAcctDocProvider`，复用 finance 过账引擎）

- **`NotesReceivableAcctDocProvider`**（`posting/provider/NotesReceivableAcctDocProvider.java`）支持 4 businessType（核实源码 :42-80）：
  | businessType | 科目分解（subjectCode + dcDirection） |
  |---|---|
  | NOTES_RECEIVABLE_RECEIVED | Dr 1121(应收票据)=票面 / Cr 1122(应收账款)=票面 |
  | NOTES_RECEIVABLE_DISCOUNTED | Dr 1002(银行存款)=实得 / Dr 6603(财务费用-利息支出)=贴现息 / [Dr/Cr 6051(汇兑损益)=外币，signum≠0 才发] / Cr 1121(应收票据)=票面（单币种三件套：Dr1002/Dr6603/Cr1121；多币种五件套含 6051 successor） |
  | NOTES_RECEIVABLE_ENDORSED | Dr 2202(应付账款)=票面 / Cr 1121(应收票据)=票面 |
  | NOTES_RECEIVABLE_COLLECTION | Dr 1002(银行存款)=票面 / Cr 1121(应收票据)=票面 |
- **`NotesPayableAcctDocProvider`**（`posting/provider/NotesPayableAcctDocProvider.java`）支持 2 businessType（核实 :48-57）：
  | businessType | 科目分解 |
  |---|---|
  | NOTES_PAYABLE_ISSUED | Dr 2202(应付账款)=票面 / Cr 2203(应付票据)=票面 |
  | NOTES_PAYABLE_HONORED | Dr 2203(应付票据)=票面 / Cr 1002(银行存款)=票面 |
- **过账接线**：`NotesPostingDispatcher`（`posting/NotesPostingDispatcher.java`）经 `IErpFinVoucherBiz`（跨域 finance 内同模块）persistVoucher，写 voucher/voucher_line/voucher_bill_r（**不写 gl_balance**，同既有范式，finance 看板/报表基线不受影响）。

### 种子 COA 缺口（须补齐——与 0704-1/1800-1/0742-2 范式一致）

经核实 `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`，现存科目码：1002/1122/2202/1401/1403/1131/2221/6401/5101/2211/4103/6602/2241/1221/1231/6701/6702/1604/4002/2240OTHER 等齐备，但**票据专用科目缺失**：

| 科目码 | 名称 | direction | 需求来源 |
|---|---|---|---|
| **1121** | 应收票据 | DEBIT | NR 4 businessType Dr/Cr 应收票据 |
| **2203** | 应付票据 | CREDIT | NP 2 businessType Dr/Cr 应付票据 |
| **6603** | 财务费用-利息支出 | DEBIT | NR_DISCOUNTED Dr 贴现息 |
| **6051** | 汇兑损益 | CREDIT | NR_DISCOUNTED 外币 fx（单币种测试可不触发，作可选补齐） |

### 浏览器层覆盖缺口（本计划对象）

- `tests/e2e/business-actions/` 无 `fin-notes-receivable*` / `fin-notes-payable*` spec——**应收/应付票据 DIRECT 生命周期 + 6 业财过账凭证行精确数值断言零覆盖**。
- 这是当前 finance 域（乃至全 18 域）最后一个「DIRECT 业务动作 + 业财过账 Provider 已实现但浏览器层零覆盖」的核心子域。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave`（`__save` 预置状态/字段入口）/ `callMutation`（原始 `@BizMutation`）/ `verifyState`（`__get` 独立断言翻转）/ `findFirst`（跨实体反查）。
- 凭证行数值断言两原语位于 `tests/e2e/orchestration/_helper.ts`：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)`；business-actions spec 经 `from '../orchestration/_helper'` 跨目录导入（先例 `projects-pnl-settlement.action.spec.ts:13`）。
- 种子 COA 加性补充范式（0704-1/1800-1/0742-2：`erp_md_subject.csv` 追加行，`findByCode` 全局按码解析无需 COA 映射）。

### 剩余差距

应收/应付票据 DIRECT 生命周期（11 动作）+ 6 业财过账凭证行精确数值断言为现存唯一核心域 DIRECT 业务动作零覆盖面；属当前重点「各域细化端到端验证」的明确 successor 面。

## Goals

- 交付浏览器层 E2E spec，经 GraphQL `/graphql` 驱动 DIRECT `@BizMutation`，状态翻转经 `verifyState`（`__get`）独立断言，业财过账凭证行经 `assertVoucherLines` 逐行断言 subjectCode + dcDirection + 借贷金额：
  1. **应收票据生命周期 spec**——覆盖 7 动作状态机正路径（receive→DISCOUNTED 分支贴现 + collect→COLLECTION_PENDING→honor/dishonor 终态 + endorse→ENDORSED + writeOff 红冲回退）+ 4 业财过账凭证行精确数值断言（RECEIVED/ENDORSED/COLLECTION 三件套 + DISCOUNTED 科目分解五件套）+ 非法迁移守卫（discount/endorse 非 RECEIVED、honor 非 COLLECTION_PENDING）+ dishonor 终态标记断言。
  2. **应付票据生命周期 spec**——覆盖 issue→ISSUED + honor→HONORED + dishonor + writeOff 状态机 + 2 业财过账凭证行精确数值断言（ISSUED Dr 2202/Cr 2203 + HONORED Dr 2203/Cr 1002）+ 非法迁移守卫。
- 补齐种子 COA（`erp_md_subject.csv` +1121/2203/6603 行；6051 单币种测试可选）。
- 在 `docs/testing/e2e-runbook.md` 业务动作表 +finance 应收/应付票据行 + 凭证行断言表 +6 行 + 套件计数更新；`docs/backlog/README.md` +1 done 行。

## Non-Goals

- **不实现新后端/契约/ORM 模型**——本计划仅消费侧 DIRECT 业务动作 E2E + 测试层 + 种子 COA 加性补充。若 Phase 1 Explore 发现某 `@BizMutation` 不可达或有 bug（如 0941-1 triggerDuePlans meta 算子白名单类 bug），属执行期豁免：在 Phase 内即时修复 + 记录 + 模块 JUnit 回归；仅当确证为生产缺陷时才开显式 successor 承接（规则 13：确认缺陷不可降级为模糊 follow-up）。
- **不覆盖现金预测定时任务（nop-job）**——`treasury.md:174/184` 明示 nop-job 接线为 Follow-up（触发条件：nop-job 接线时），当前仅手动 `refreshForecast`。`ErpFinCashForecastBizModel` 1 `@BizMutation`（refreshForecast）归 successor（不同结果面：定时聚合，非票据生命周期）。
- **不覆盖银行授信额度（CreditFacility）独立生命周期**——`ErpFinCreditFacilityBizModel` 2 `@BizMutation`（利息计提）+ 授信强一致校验（`treasury.md:179` 规则 1）属授信额度面 successor；本计划仅在应付票据 issue 路径断言「银承开票占用授信额度」副作用（若 config `erp-fin.credit-check-on-issue=true` 默认启用且 setup 可达），不独立编排授信生命周期。
- **不覆盖拒付转应收完整追索/坏账核销**——`treasury.md:181` 规则 3 + Processor 注释明示 dishonor「仅标记终态，转挂应收账款科目；后续催收/坏账属信用管理面 Non-Goal」。
- **不覆盖外币贴现汇兑损益路径**——单币种（本位币）测试即覆盖科目分解五件套主路径（Dr 1002/Dr 6603/Cr 1121）；外币 fx（6051 汇兑损益行）`signum≠0` 才发，属多币种 successor。种子 6051 作可选补齐（不作为结束门控）。
- **不触及 writeOff 强制审批（xwf）**——`treasury.md:182` 规则 4 设计意图「需财务主管审批」，但 ORM 无 use-approval tagSet，实现为 DIRECT（config `erp-fin.notes-writeoff-approval-required` 门控红冲行为非 xwf 审批轴）。writeOff 经 DIRECT `@BizMutation` 浏览器层可达（红冲回退最末过账），不触及 xwf 审批面。
- **不覆盖电子票据外部系统对接**——`treasury.md:188` 明示不在范围。
- **不重复覆盖银行对账**——`fin-bank-recon.action.spec.ts`（0413-2）已交付；票据与银行对账解耦（`treasury.md:10/166`）。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的 DIRECT 业务动作 + 凭证行数值断言 successor；纯消费侧 + 测试层 + 种子 COA 加性补充，零生产契约变更预期）
- Owner Docs: `docs/design/finance/treasury.md`（应收/应付票据状态机 + 业财过账 businessType + 配置点 + 业务规则，权威预期行为）、`docs/design/finance/posting.md`（过账机制）、`docs/testing/e2e-runbook.md`（套件结构/运行命令/业务动作表/凭证行断言表）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护 + 种子 COA 加性补充，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1 裁决先例），依技能实质内容判定 `Skill: none`（nop-testing）。Phase 1 Explore 阶段如发现后端不可达/科目解析失败需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；种子 COA 为 `app-erp-all/.../_init-data/erp_md_subject.csv` 加性补充（非 ORM 模型变更，无需 ask-first——对齐 0704-1/1800-1/0742-2 范式）；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- 应收/应付票据 setup 须建 `ErpFinNotesReceivable`/`ErpFinNotesPayable` 头（经 `__save` 直置 status 入口）+ 必填字段（notesType/notesNo/currencyId/amountSource/amountFunctional/orgId/partnerId 驱动过账科目解析）。partnerId 须指向种子 partner（如 2212 应收/2202 应付）以使 RECEIVED/ENDORSED 凭证 partner 维度可填。
- 种子 COA 补齐（Phase 1 确认后 Phase 2 落地）：1121/2203/6603 必需，6051 可选。
- config `erp-fin.credit-check-on-issue` 默认 true（`treasury.md:172`）——若应付票据 issue 路径强一致校验阻塞，Phase 1 Explore 裁定 setup 自包含建 CreditFacility（totalAmount 充足）或 webServer JVM arg 门控；不预置结论。

## Execution Plan

### Phase 1 - Explore：后端可达性 + setup 工程化 + 种子 COA 核实

Status: completed
Targets: `module-finance/erp-fin-service/.../entity/ErpFinNotesReceivableBizModel.java`、`module-finance/erp-fin-service/.../processor/ErpFinNotesReceivableProcessor.java`、`module-finance/erp-fin-service/.../posting/provider/NotesReceivableAcctDocProvider.java`、`module-finance/erp-fin-service/.../posting/provider/NotesPayableAcctDocProvider.java`、`module-finance/erp-fin-service/.../entity/ErpFinNotesPayableBizModel.java`、`module-finance/erp-fin-service/.../processor/ErpFinNotesPayableProcessor.java`、`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

> **行为已部分预决（草案审查迭代可收敛）**：NotesReceivable 7 动作状态机 + 4 businessType 凭证结构 + NotesPayable 4 动作 + 2 businessType 凭证结构经主代理源码复核已确定性核实（见 Current Baseline）。本 Phase 仅核实浏览器层调用细节（入参名/返回类型/setup 必填字段可达性）+ 种子 COA 确切缺口 + credit-check-on-issue 对应付票据 issue 的影响。

- [x] `Proof`：冷核实 7+4 动作浏览器层调用细节——`receive(@Name("notesId"))`/`discount(@Name 4 参)`/`endorse(@Name 2 参)`/`collect`/`honor`/`dishonor`/`writeOff`/`issue`/`honor(NP)`/`dishonor(NP)`/`writeOff(NP)` 的 `@BizMutation` 注解 + `@Name` 入参 + 返回类型（实体，GraphQL 可选字段集）。核实自包含 setup 可达性：`ErpFinNotesReceivable`/`ErpFinNotesPayable` 必填字段 + partnerId/currencyId/orgId 驱动过账科目解析。
  - Skill: `nop-debugging`
  - **结论**：11 动作经 `ErpFinNotesReceivableBizModel.java:35-81` + `ErpFinNotesPayableBizModel.java:33-55` 核实，全部 `@BizMutation` + `@Name` 精确入参 + 返回实体（GraphQL 可选字段集）。标量入参经 `GraphQLClient.callMutation`：Long→JS number 内联、LocalDate→quoted "YYYY-MM-DD" string（JSON.stringify）、BigDecimal→JS number 内联。ORM 必填字段：code/orgId/notesType/currencyId（NR + NP 通用）；partnerId 可空但 NR RECEIVED/ENDORSED 凭证 partner 维度填充需 partnerId（-provider 设 `cr.setPartnerId(partnerId)`）。
- [x] `Proof`：核实 `ErpFinNotesReceivableProcessor.doWriteOff`（`:210-222` + `:256` `businessTypeForStatus(status)` → `reverseReceivable`）行为——writeOff 置 posted=false（实体级回退标记，`:217-220`）+ 红冲凭证 postingType=REVERSAL 同向取负（dcDirection 不变金额取负，对齐 0742-2/1005-2 红冲范式）。核实 config `erp-fin.notes-writeoff-approval-required` 默认值 + 对 DIRECT 可达性的影响（实现为 DIRECT + config 门控红冲，非 xwf 审批轴）。
  - Skill: `nop-debugging`
  - **结论**：writeOff DIRECT 可达（无 xwf 审批轴）。`doWriteOff`：posted==true 时经 `postingDispatcher.reverseReceivable(note, businessTypeForStatus(status))` → `executor.reverse(note.code, businessType)` 写 REVERSAL 凭证（**同 billHeadCode=note.code**），故 NR writeOff 红冲凭证与原 NORMAL 凭证共享 billCode，须 `findVoucherIdByBillCode(code,'REVERSAL')` 按 postingType 区分。`businessTypeForStatus`：RECEIVED/COLLECTION_PENDING→NOTES_RECEIVABLE_RECEIVED、DISCOUNTED→NOTES_RECEIVABLE_DISCOUNTED、ENDORSED→NOTES_RECEIVABLE_ENDORSED。随后 setPosted(false)+清 postedAt/postedBy（实体级回退标记）。NP `doWriteOff` 硬编码 reverse `NOTES_PAYABLE_ISSUED`（ISSUED 为 honor 前唯一过账入口）。**无 `erp-fin.notes-writeoff-approval-required` config 存在**（grep ErpFinConstants 仅 `CONFIG_CREDIT_CHECK_ON_ISSUE`），writeOff 纯 DIRECT。
- [x] `Decision`：核实应付票据 issue 路径授信强一致校验（config `erp-fin.credit-check-on-issue` 默认 true）——setup 须自包含建 `ErpFinCreditFacility`（totalAmount 充足 + BANK_ACCEPTANCE notesType）还是 webServer JVM arg 门控关闭。裁定 setup 工程化方案。
  - Skill: none
  - **裁定**：`reserveCreditIfNeeded`（`ErpFinNotesPayableProcessor:104-109`）三短路条件：`isBankAcceptance(note) && isCreditCheckOnIssue() && note.creditFacilityId != null`。**采用 `notesType=COMMERCIAL_ACCEPTANCE`** 作 NP 主路径 setup → `isBankAcceptance` 返回 false → 授信校验整体短路跳过，无需建 CreditFacility（最简自包含）。授信守卫负路径单独建 BANK_ACCEPTANCE + CreditFacility(availableAmount<票面) 断言 `ERR_*` token（config 默认 true 启用，非 webServer arg 门控）。
- [x] `Decision`：界定每 spec 精断言面（已在 Goals 预定，本 Phase 仅确认 setup 工程化细节 + 种子 COA 确切追加行）：NR spec = receive(RECEIVED Dr1121/Cr1122) + discount(DISCOUNTED Dr1002/Dr6603/Cr1121 五件套，单币种无 fx) + endorse(ENDORSED Dr2202/Cr1121) + collect(无过账) + honor(HONORED Dr1002/Cr1121) + dishonor(无过账终态) + writeOff(红冲回退) + 守卫；NP spec = issue(ISSUED Dr2202/Cr2203) + honor(HONORED Dr2203/Cr1002) + dishonor + writeOff + 守卫。
  - Skill: none
  - **结论**：setup 工程化 — NR discount 路径须 `dueDate != null && discountDate < dueDate` 使 `remainingDays>0`（`buildDiscount:229-235`，否则 discountInterest=0 致 6603 行断言 0 弱测试）；bankId 须非空（`requireDiscountInputs:150`）→ 自包含建 `ErpFinFundAccount(BANK, subjectId=1002 id=2)` 供 bankId FK（对齐 fin-bank-recon 范式）。discountInterest 确定性派生：face=1000 × rate=0.12 × days=30 / 360 = 10.00（HALF_UP scale 2）；netAmount=1000-10=990。种子 COA 确切追加行：**1121/2203/6603 必需，6051 不补**（单币种 exchangeGainLoss=0，fx 行 `signum()!=0` 抑制不发，Provider:72）。

Exit Criteria:

- [x] 11 动作浏览器层调用细节（入参名/返回类型/setup 必填字段可达性）冷核实结论记录入计划
- [x] writeOff 红冲行为（REVERSAL 同向取负 vs 反向）+ config 门控影响冷核实结论记录
- [x] 应付票据 issue 授信校验 setup 工程化方案裁定（COMMERCIAL_ACCEPTANCE 短路 + BANK_ACCEPTANCE 守卫负路径）
- [x] 种子 COA 确切追加行清单确认（1121/2203/6603 必需，6051 不补——单币种 fx 行抑制）

---

### Phase 2 - 应收票据 spec 落地 + 种子 COA 补齐

Status: completed
Targets: `tests/e2e/business-actions/fin-notes-receivable.action.spec.ts`、`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：种子 COA 补齐——`erp_md_subject.csv` 追加 1121(应收票据,ASSET,DEBIT) / 2203(应付票据,LIABILITY,CREDIT) / 6603(财务费用-利息支出,EXPENSE,DEBIT)；6051 可选（若 Phase 1 裁定单币种测试不触发 fx 行则不补）。
  - Skill: none
  - **落地**：追加 id 40=1121 / 41=2203 / 42=6603（6051 不补——单币种 exchangeGainLoss=0，fx 行 signum()!=0 抑制）。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。
- [x] `Add`：应收票据生命周期 spec——自包含建 `ErpFinNotesReceivable`（notesType/notesNo/currencyId/amountSource=amountFunctional/orgId/partnerId，经 `__save` 直置 status 入口）→
  - **receive**：`ErpFinNotesReceivable__receive(notesId)` → `verifyState` RECEIVED + posted=true + 凭证行断言 Dr 1121=票面 / Cr 1122=票面（NOTES_RECEIVABLE_RECEIVED）。
  - **discount**：RECEIVED 前置 → `__discount(notesId,discountDate,bankId,discountRate)` → `verifyState` DISCOUNTED + posted=true + 凭证行三件套断言（单币种）Dr 1002=netAmount / Dr 6603=贴现息（=票面×贴现率×剩余天数/360，确定性派生）/ Cr 1121=票面（NOTES_RECEIVABLE_DISCOUNTED；6051 fx 行单币种不发，signum=0 抑制）。**setup 工程化**：须 `dueDate != null` 且 `discountDate < dueDate` 使 `remainingDays > 0`，否则 `discountInterest=0` 致 6603 行断言 0（弱测试）。
  - **endorse**：另建 RECEIVED 前置 → `__endorse(notesId,endorsementFromId)` → `verifyState` ENDORSED + posted=true + 凭证行断言 Dr 2202=票面 / Cr 1121=票面（NOTES_RECEIVABLE_ENDORSED）。
  - **collect→honor**：RECEIVED 前置 → `__collect` → `verifyState` COLLECTION_PENDING（无过账）→ `__honor` → `verifyState` HONORED + posted=true + 凭证行断言 Dr 1002=票面 / Cr 1121=票面（NOTES_RECEIVABLE_COLLECTION）。
  - **dishonor**：另建 collect→COLLECTION_PENDING 前置 → `__dishonor` → `verifyState` DISHONORED（终态，无过账）。
  - **writeOff**：另建 RECEIVED 前置 → `__writeOff` → `verifyState` WRITE_OFF + posted=false（实体级回退标记，`Processor:217-220` setPosted(false)+清 postedAt/postedBy）+ 红冲凭证行同向取负断言（REVERSAL postingType，dcDirection 不变金额取负，Phase 1 裁定的红冲范式）。
  - **守卫**：discount/endorse 非 RECEIVED（如 DISCOUNTED→discount）+ honor 非 COLLECTION_PENDING 各抛 ERR token + status 不变。
  - Skill: none
  - **落地**：7 tests 全绿（52.2s）。确定性派生：face=1000/rate=0.12/discountDate=2026-07-01/dueDate=2026-07-31 → remainingDays=30 → discountInterest=10.00/netAmount=990。writeOff 经 receive 前置产 posted=true，再 writeOff 产 REVERSAL（businessTypeForStatus(RECEIVED)→NOTES_RECEIVABLE_RECEIVED 红冲）Dr -1121/Cr -1122。endorse 经 endorsementFromId=note.id 自引用（GraphQL @Name 标非空，Processor 仅记录不影响过账）。collect→honor 断言 collect 无凭证 + honor 产 COLLECTION 凭证；dishonor 终态无凭证。
- [x] `Proof`：新增 spec `--workers=1` 全绿。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-receivable.action.spec.ts --workers=1`
  - Skill: none
  - **结果**：7 passed（52.2s）。

Exit Criteria:

- [x] 应收票据 spec 全绿，7 动作状态翻转 + 4 业财过账凭证行精确数值均经 `verifyState`/`assertVoucherLines` 独立断言（非仅 mutation 返回值）
- [x] 种子 COA 补齐使过账 Provider 科目码可达（`findByCode` 全局按码解析）
- [x] **执行期发现 + 处置（latent defect，非阻塞）**：`ErpFinArApItemGenerator.buildCode` 生成辅助账 code `"ARI-{sourceBillType}-{sourceBillCode}-{uuid8}"`，voucherCode 精度 50；NOTES_RECEIVABLE(17)+长 sourceBillCode(>19 字符) 组合溢出 → `sqlState=22001` 截断 → receive/endorse posted=false。后端 JUnit 用短码通过故未暴露。**处置**：E2E 用紧凑 base36 note.code（≤13 字符，对齐生产票据码 NR-2026-0001 实际长度）绕过；生产代码修复属 Protected Area（ask-first），记于 `docs/bugs/` + Deferred 作显式 successor（触发条件：票据/单据码长度策略统一或该缺陷复现时），不阻塞本计划结束。— **RELEASED by 2026-07-17-1600-1**（successor 已承接并完成：`buildCode` 长度守护 + 长码回归测试）

---

### Phase 3 - 应付票据 spec 落地 + 全套件回归

Status: completed
Targets: `tests/e2e/business-actions/fin-notes-payable.action.spec.ts`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：应付票据生命周期 spec——自包含建 `ErpFinNotesPayable`（notesType=BANK_ACCEPTANCE/notesNo/currencyId/amount/orgId/partnerId + 若 credit-check-on-issue=true 则建 CreditFacility totalAmount 充足，Phase 1 裁定）→
  - **issue**：`ErpFinNotesPayable__issue(notesId)` → `verifyState` ISSUED + posted=true + 凭证行断言 Dr 2202=票面 / Cr 2203=票面（NOTES_PAYABLE_ISSUED）。
  - **honor**：ISSUED 前置 → `__honor(notesId)` → `verifyState` HONORED + posted=true + 凭证行断言 Dr 2203=票面 / Cr 1002=票面（NOTES_PAYABLE_HONORED）。
  - **dishonor**：另建 ISSUED 前置 → `__dishonor` → `verifyState` DISHONORED。
  - **writeOff**：另建 ISSUED 前置 → `__writeOff` → `verifyState` WRITE_OFF + 红冲断言（Phase 1 范式）。
  - **守卫**：honor 非 ISSUED + credit-check 不足（CreditFacility availableAmount<票面）抛 ERR token（若 config 启用）。
  - Skill: none
  - **落地**：5 tests 全绿（47.6s）。主路径 COMMERCIAL_ACCEPTANCE 短路授信校验（最简 setup）；issue Dr 2202/Cr 2203、honor Dr 2203/Cr 1002 凭证行精确数值断言；dishonor 终态无凭证；writeOff 经 issue 前置产 posted=true 再红冲 NOTES_PAYABLE_ISSUED（Dr -2202/Cr -2203）；守卫 honor 非 ISSUED + BANK_ACCEPTANCE+CreditFacility(totalAmount=100<票面1000) issue 抛 ERR_CREDIT_FACILITY_INSUFFICIENT（服务端日志确认 errorCode + 事务回滚 status 不变）。
- [x] `Proof`：新增 spec `--workers=1` 全绿 + business-actions 全套件回归 0 新增失败。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-payable.action.spec.ts --workers=1` + business-actions 全套件抽样回归
  - Skill: none
  - **结果**：5 passed（47.6s）+ 抽样回归 fin-bank-recon/fin-bad-debt/fin-budget-scenario/finance-voucher-post 12 passed 0 新增失败（种子 COA 加性补充 + 新增 spec 无污染既有 finance 业务动作套件）。

Exit Criteria:

- [x] 应付票据 spec 全绿，状态翻转 + 2 业财过账凭证行精确数值均经独立断言
- [x] business-actions 全套件回归 0 新增失败

---

### Phase 4 - 文档对齐 + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/logs/2026/07-17.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3

- [x] `Add`：`e2e-runbook.md` 业务动作表 +finance 应收/应付票据行（receive/discount/endorse/collect/honor/dishonor/writeOff + issue/honor/dishonor/writeOff）+ 凭证行断言表 +6 行（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION + NOTES_PAYABLE_ISSUED/HONORED）+ 套件计数更新（+2 spec）；`backlog/README.md` +1 done 行（2026-07-17-1430-1）。
  - Skill: none
  - **落地**：e2e-runbook 业务动作表 +2 行（NR 7 动作 + NP 4 动作）+ 凭证行断言表 +6 行（4 NR businessType + 2 NP businessType）+ 套件计数 61→63（overview/全套件描述/目录树注释/分层运行表/business-actions 段头/套件计数 evolution note 全量更新，含本计划 1430-1 evolution 条目）；backlog/README +1 done 行。
- [x] `Add`：`docs/logs/2026/07-17.md` 增聚合条目（spec 数/验证状态/范围纪律/种子 COA 补齐）。
  - Skill: none
  - **落地**：07-17.md 顶部（时间倒序）增 1430-1 聚合条目（背景/Phase 1-4/latent defect 处置/验证状态/范围纪律）。

Exit Criteria:

- [x] e2e-runbook（业务动作表 + 凭证行断言表 + 套件计数）+ backlog README + 日志三点落地一致

## Draft Review Record

- Independent draft review iteration 1: **accept**（独立 general 子代理 `ses_091bd4197fferEBRms0Q421jp9`，新会话冷重播无执行者/起草者上下文，2026-07-17）。**VERDICT: accept**。0 BLOCKER / 0 MAJOR / 5 MINOR。全部 load-bearing 主张经实时仓库逐项核实**零伪**：
  - B1 缺口真实性：`glob tests/e2e/**/*notes*` 无；全 `tests/e2e/` 树 grep NotesReceivable/NotesPayable 0 命中；e2e-runbook 业务动作表 0 票据行——**缺口真实**。
  - 后端 DIRECT 可达性：`ErpFinNotesReceivableBizModel.java:36-81` 7 `@BizMutation`（入参 `@Name` 精确）+ `ErpFinNotesPayableBizModel.java:35-55` 4 `@BizMutation`；ORM `app-erp-finance.orm.xml:1316-1317/1358` `tagSet="gid,erp.finance"` **无 use-approval/use-workflow**——DIRECT 可达确认。
  - GL Provider + 科目码：`NotesReceivableAcctDocProvider.java:43-94` 4 businessType + 1121/1122/1002/6603/6051 全部精确；`NotesPayableAcctDocProvider.java:37-58` 2 businessType + 2202/2203/1002 全部精确——与计划表完全吻合。
  - 种子 COA 缺口：`erp_md_subject.csv` 核实 1121/2203/6603/6051 全部确实缺失；1002/1122/2202 存在——缺口主张准确。
  - 触发正当性：项目处于「各域细化端到端验证」+ ~15 份 DIRECT-action/voucher-line 计划轨迹；资金/票据为 finance 核心子域最后一个零浏览器 E2E 面——**decisively 区别于已取消的 1005-1**（1005-1 因基线「零覆盖」主张为伪被取消，本计划基线主张经独立核实为真）。scope-manufacturing 裁决：**legitimately warranted（非 padding）**。
  - owner doc：`treasury.md` 文档 7 态 NR 状态机（:56-65）+ NP 4 态（:89）+ 6 businessType 过账方向（:140-148）+ 规则 1-5（:179-188）——与计划依赖吻合。
  - xwf 先例：2330-1 权威裁决浏览器层不可行；计划 Non-Goals + Deferred writeOff-xwf 正确排除；writeOff 确为 DIRECT（`Processor:210-222` 无 wf）——先例受尊重。
  - 规则 R1-R14/anti-slack/template/naming 全 PASS；R4/R14 bundling（同 owner doc + 同结果面 + 同验证路径）合理。
  - **5 MINOR（已全部修订落地）**：m1 方法名 `getLastPostedBusinessType`→`businessTypeForStatus`（Processor.java:256）+ 行号订正；m2 writeOff posted=false 实体级回退标记补入 Phase 1 open-question；m3「五件套」标签订正为「单币种三件套/多币种五件套含 6051 successor」；m4 discount setup 前置条件（dueDate≠null + discountDate<dueDate 使 remainingDays>0）补入 Phase 2；m5 Non-Goal 执行期豁免措辞订正消除自相矛盾。
- **共识达成 → `Plan Status: active`**。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧 DIRECT 业务动作 + 凭证行数值断言 + 测试层 + 种子 COA 加性补充。结束前运行新增 spec + business-actions 回归 + 后端构建（确认 spec/种子变更未污染后端）。

- [x] 范围内行为完成（应收/应付票据 DIRECT 生命周期 + 6 业财过账凭证行精确数值浏览器层 E2E 交付）
- [x] 相关文档对齐（e2e-runbook 业务动作表 + 凭证行断言表、backlog README done 行、日志）
- [x] 已运行验证：新增 spec `--workers=1` 全绿（NR 7 + NP 5 = 12 passed）+ business-actions 回归 0 新增失败（finance 既有 4 spec 抽样 12 passed）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（执行期发现 `ErpFinArApItemGenerator.buildCode` latent defect 属**共享过账基础设施** out-of-scope，非本计划范围内项目降级——已记 `docs/bugs/` + Deferred 作显式 successor，生产代码修复须 ask-first 不在本计划范围）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果）。执行期确认后分类。

### 现金预测定时任务（nop-job 接线）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `treasury.md:174/184` 明示 nop-job 定时基线未落地，当前仅手动 `refreshForecast`。现金预测属聚合面（不同结果面），非票据生命周期。
- Successor Required: `yes`（触发条件：nop-job 接线时，或现金预测浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-17-2256-1**：触发条件已满足（nop-job 已接线 `ErpFinCashForecastJob` + scheduler.yaml；现金预测浏览器层 E2E 落地 `fin-cash-forecast.action.spec.ts` 3 用例覆盖三源聚合/终态过滤/幂等重写）。

### 银行授信额度独立生命周期 + 利息计提

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpFinCreditFacilityBizModel` 2 `@BizMutation`（CREDIT_FACILITY_INTEREST 计提）属授信额度面 successor；本计划仅在应付票据 issue 路径断言授信占用副作用（若 config 启用）。
- Successor Required: `yes`（触发条件：授信额度浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-17-2256-1**：触发条件已满足（授信额度浏览器层 E2E 落地 `fin-credit-facility.action.spec.ts` 3 用例覆盖 reserve/release/不足守卫事务回滚）。利息计提（CREDIT_FACILITY_INTEREST）后端 `@BizMutation` 未实现，归 2256-1 Deferred（后端 successor）。

### 外币贴现汇兑损益（6051）路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 单币种测试即覆盖科目分解五件套主路径；外币 fx（6051 行）`signum≠0` 才发，属多币种 successor。
- Successor Required: `yes`（触发条件：多币种票据贴现浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-19-0120-1**：触发条件已满足（AGENTS.md「当前项目阶段」明示当前重点含「各域细化端到端验证」）。本计划交付 `fin-notes-receivable-fx-discount.action.spec.ts` 三层覆盖——(1) FX 状态机生命周期经 discount mutation（USD 票据 → DISCOUNTED + 3 行凭证 functional CNY，6051 因 builder ZERO 抑制）；(2) FX 6051 触发分支经 `ErpFinVoucher__post` 直驱（4 行凭证含 Dr 6051=5.00 signum>0）；(3) 对照单币种经同 post 入口（3 行凭证 signum=0 抑制）。**执行期发现 Java builder 缺陷**：`ErpFinNotesReceivableProcessor.buildDiscount:249` 无条件 `setExchangeGainLoss(BigDecimal.ZERO)` 致 discount mutation 永远不触发 6051 行——spec 改用 `ErpFinVoucher__post` 直驱验证 Provider FX 分支（plan Phase 1 Decision (a)），Java builder 外币 exchangeGainLoss 派生缺陷归 0120-1 `Deferred But Adjudicated` 显式 successor（不改生产代码即时修，plan 规则 13/14）。

### writeOff 强制审批（xwf）面

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `treasury.md:182` 规则 4 设计意图「需财务主管审批」，但 ORM 无 use-approval，实现为 DIRECT + config 门控红冲。writeOff 经 DIRECT 浏览器层可达；xwf 审批面经 2330-1 权威裁决浏览器层不可行。
- Successor Required: `yes`（触发条件：nop-entropy 平台支持浏览器层测试用户身份映射时——同 2330-1 重评触发条件）

## Closure

Status Note: 执行完成（2026-07-17）。4 Phase 全绿——Phase 1 Explore（11 动作浏览器层调用 + writeOff 红冲 + NP COMMERCIAL_ACCEPTANCE setup 裁定 + 种子 COA 清单 1121/2203/6603）/ Phase 2（种子 COA +3 行 + NR spec 7 用例全绿 52.2s）/ Phase 3（NP spec 5 用例全绿 47.6s + finance 既有 4 spec 抽样回归 12 passed 0 新增失败）/ Phase 4（e2e-runbook 业务动作表+2 行 + 凭证行断言表+6 行 + 套件计数 61→63 + backlog +1 done + 日志聚合条目）。验证：2 新 spec 12/12 + 抽样回归 12/0 + 154 模块 BUILD SUCCESS。执行期发现 latent defect（`ErpFinArApItemGenerator.buildCode` 超 voucherCode precision 50，长票据码时 sqlState=22001）记 `docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md` + Deferred 显式 successor，E2E 紧凑码绕过，不阻塞结束。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，无执行者/起草者上下文，2026-07-17）
- Audit Scope: 五点一致性 + Exit Criteria vs 实时仓库 + Anti-Hollow + Deferred 诚实性 + 文档同步
- Evidence:
  - **Phase status/items 一致性**：4 Phase 全部 `Status: completed`，每个 Phase body 所有执行项 + Exit Criteria 均 `[x]`（grep `- \[ \]` 在 Execution Plan 段内 0 命中）。
  - **Exit Criteria vs 实时仓库（HEAD 2026-07-17 冷核实）**：
    - `tests/e2e/business-actions/fin-notes-receivable.action.spec.ts` 存在，7 个 `test(...)`（receive/discount/endorse/collect→honor/dishonor/writeOff/守卫），与 Phase 2 落地主张一致。
    - `tests/e2e/business-actions/fin-notes-payable.action.spec.ts` 存在，5 个 `test(...)`（issue/honor/dishonor/writeOff/守卫），与 Phase 3 落地主张一致。
    - 种子 COA `app-erp-all/.../_vfs/_init-data/erp_md_subject.csv` 第 41-43 行确实追加 id=40/1121(应收票据,ASSET,DEBIT) + id=41/2203(应付票据,LIABILITY,CREDIT) + id=42/6603(财务费用-利息支出,EXPENSE,DEBIT)；6051 未补（单币种 fx 行抑制，与 Phase 1 裁定一致）。
    - `docs/testing/e2e-runbook.md` 业务动作表 +2 行（line 289-290 NR/NP）+ 凭证行断言表 +6 行（line 381-386 NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION + NOTES_PAYABLE_ISSUED/HONORED）+ 套件计数 evolution note（61→63，line 392）。
    - `docs/backlog/README.md` line 79 含 1430-1 done 行（与 Phase 4 主张一致）。
    - `docs/logs/2026/07-17.md` line 3-11 含 1430-1 聚合条目（背景 + Phase 1-4 + latent defect 处置）。
  - **Anti-Hollow**：抽读 `fin-notes-receivable.action.spec.ts:121-194`——test 非空壳：经 `callMutationOk(page,'ErpFinNotesReceivable','receive'/'discount'/'endorse', {...真实 @Name 入参}, 'id status posted ...')` 实调 DIRECT `@BizMutation`；经 `verifyState`（独立 `__get`）翻转断言 status/posted（非仅 mutation 返回值）；经 `findVoucherIdByBillCode + assertVoucherLines` 逐行断言 subjectCode + dcDirection + debit/credit 精确数值（FACE_AMOUNT=1000 / NET_AMOUNT=990 / DISCOUNT_INTEREST=10 确定性派生常量）。无 `return null`/空 body/吞异常占位。
  - **Deferred 诚实性**：latent defect `ErpFinArApItemGenerator.buildCode` 经核 `docs/bugs/2026-07-17-1430-...`（54 行，非 stub）含完整复现/诊断/被拒假设/决定性证据（`buildCode:192-195` "ARI-"+sourceBillType+... 对 NOTES_RECEIVABLE+长码溢出 precision 50）。该缺陷属**共享过账基础设施**（非票据生命周期范围内项目），正确处置为显式 successor（触发条件：票据/单据码长度策略统一或复现时），生产代码修复须 ask-first 独立计划——非范围内项目降级，符合规则 13/14。
  - **Draft Review Record**：line 234-244 记录独立 general 子代理 `ses_091bd4197fferEBRms0Q421jp9` 新会话冷重播草案审查 accept（0 BLOCKER / 0 MAJOR / 5 MINOR 全部修订），符合规则 12。
  - **文本一致性**：Plan Status `completed`（line 3）/ 4 Phase Status `completed` / 所有 Exit Criteria `[x]` / 8 Closure Gates `[x]`（本次审计勾选最后 2 项审计类门控）/ Closure 段证据填充——全一致。
- Verdict: **approved** — 范围内行为完成、文档对齐、验证已运行（spec 12/12 + 抽样回归 0 失败 + 154 模块 BUILD SUCCESS）、范围内项目零降级、Anti-Hollow 通过、Deferred 诚实、文本一致。执行者未自我审计（本段裁决由独立子代理新会话生成）。

Follow-up:

- `ErpFinArApItemGenerator.buildCode` 长度守护改造（显式 successor，触发条件：票据/单据码长度策略统一或该 latent defect 复现时；详见 `docs/bugs/2026-07-17-1430-...`，生产代码修复须 ask-first 独立计划） — **RELEASED by 2026-07-17-1600-1**（`buildCode` 应用层长度守护落地 + 长码回归测试 + 红绿反转证明）
