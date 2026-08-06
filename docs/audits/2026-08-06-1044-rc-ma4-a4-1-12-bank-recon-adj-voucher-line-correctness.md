# RC MA4 A4.1.12 — UC-FIN-09/14 调整凭证行级 Dr/Cr/科目/金额正确性评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.12（MA4 运行时行为验证 — A1.4 §7-2：UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性，`BankReconAdjAcctDocProvider.createFacts` 产出的 2-4 条 VoucherFact 行级无测试断言）
> 输入：`docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 2 + §3 测试证据 + §5 断言④ 接受
> 验证 plan：`docs/plans/2026-08-06-1044-3-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）
> 审计性质：**只读行级正确性评估**（读 `createFacts` 行级生成逻辑 + grep 测试断言全集 + 复用 MA2/A1.4；不改代码/ORM/api.xml/真相源）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.4 §7-2：UC-FIN-09/14 断言④ 调整凭证行级（Dr/Cr/科目/金额）正确性——L4 仅断言凭证存在性 + billR 计数，`BankReconAdjAcctDocProvider.createFacts:51-70` 产出的 2-4 条 VoucherFact 行级正确性无测试断言 |
| 行级生成逻辑核验（写时实测） | `BankReconAdjAcctDocProvider#createFacts:51-70`——bankCredit>0（银行已收）→ `fact(bankSubject,DEBIT,bankCredit,"BANK_RECV"):61` + `fact(adjSubject,CREDIT,bankCredit,"ADJ_BANK_RECV"):62`（借银行存款/贷调整科目）；bankDebit>0（银行已付）→ `fact(adjSubject,DEBIT,bankDebit,"ADJ_BANK_PAID"):66` + `fact(bankSubject,CREDIT,bankDebit,"BANK_PAID"):67`（借调整科目/贷银行存款）；`fact:72-82` 设 subjectCode/dcDirection/amount/accountKey/businessType=BANK_RECON_ADJ。**每对借贷同金额（bankCredit 或 bankDebit）→ 借贷平衡；bankCredit 对 + bankDebit 对互不干扰 → 整体平衡；科目方向符合 L2 `bank-reconciliation.md §业务规则 3/4`** |
| 测试断言集全集（写时实测） | **JUnit**（`TestErpFinBankReconciliation#testPostGeneratesAdjustmentVoucherAndReverse:136-171`）：仅断言凭证**存在性**（`assertNotNull(adjVoucherId)`:164）+ billR 计数（`countBillLinks>=1`:161）+ docStatus（POSTED:166 / CANCELLED:169）+ reversal 计数（`countReversalVouchers>=1`:170）——**未断言行级 subjectCode/dcDirection/debitAmount/creditAmount，亦未断言借贷合计**。**E2E**（`tests/e2e/business-actions/fin-bank-recon.action.spec.ts:182-219`）：`assertVoucherLines` **强断言行级**——正向凭证 `{1002 DEBIT 100 / 2240OTHER CREDIT 100}`（:185-188）+ 红冲凭证同向取负 `{1002 DEBIT -100 / 2240OTHER CREDIT -100}`（:216-219） |
| 存疑点前提校正（关键） | A1.4 §7-2 / §3 称「行级零断言」**仅对 JUnit 成立**；E2E `fin-bank-recon.action.spec.ts:185-219` **实际已强断言行级 subjectCode/dcDirection/debitAmount/creditAmount**（A1.4 §3 评级「中」未计入此 E2E 行级补充）——行级正确性经 E2E 层**回归保护** |
| 借贷平衡间接保护 | 过账引擎 `ErpFinPostingProcessor.assertBalanced:736-742` 强制 `totalDebit==totalCredit` 否则抛 `ERR_UNBALANCED`（:738）；JUnit post 成功（POSTED）即间接证明 facts 借贷平衡 |
| 符合性结论（§2 判据） | **P2-RC-083（测试覆盖补强 successor，watch-only，非行为缺陷）**——createFacts 行级生成逻辑对称正确（无行为缺陷）+ E2E 行级强补充（行级语义已回归保护）；但 JUnit 单测行级断言完全缺失（仅存在性/计数/状态，弱于 P2-RC-017/029 的「合计+计数」）→ 与 P2-RC-017（sales AR）/ P2-RC-029（inventory 估值）**同型不同域/UC** → §2 P2① |
| 新 finding | **1**（P2-RC-083，watch-only successor，纯测试代码补强） |
| P0 即时通道 | 不触发（未出 P0/P1） |

**核心裁决**：存疑点 A1.4 §7-2 的行级正确性评估结论 = **P2 测试覆盖补强 successor（非行为缺陷）**。判据三层：(1) **createFacts 行级生成逻辑对称正确**——bankCredit 对（借银行存款/贷调整科目）+ bankDebit 对（借调整科目/贷银行存款），每对借贷同金额 → 平衡 + 整体平衡 + 科目方向符合 L2 `bank-reconciliation.md §业务规则 3（方向语义对齐）/4（未达账项调整）/6（posted 联动）`；无行为缺陷（故非 P1/P0，A1.4 §5 断言④ 接受维持）。(2) **存疑点前提校正**——A1.4 §7-2 / §3 称「行级零断言」**仅对 JUnit 成立**；E2E `fin-bank-recon.action.spec.ts:185-219` 已强断言行级（subjectCode/dcDirection/debitAmount/creditAmount，正向 + 红冲凭证双向），行级正确性经 E2E 层回归保护。(3) **JUnit 行级断言完全缺失**——`testPostGeneratesAdjustmentVoucherAndReverse` 仅断言存在性/计数/状态（弱于 P2-RC-017/029 的「合计+计数」），与 P2-RC-017/P2-RC-029 **同型不同域/UC**（finance 银行对账调整凭证 BankReconAdjAcctDocProvider vs sales AR / inventory 估值），按 §7 新建 P2-RC-083 watch-only successor。修复 = 补强 `TestErpFinBankReconciliation` 行级断言（断言每行 subjectCode[1002/2240OTHER] + dcDirection[DEBIT/CREDIT] + debitAmount/creditAmount 精确值）——**纯测试代码，按 roadmap 预授权类目（测试补充）经 MR1 自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账核心路径）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

**UC-FIN-09 银行对账与未达账项**（`docs/design/finance/use-cases.md:165-177`）断言④ 逐字：

```
未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ), 下月红冲
```

**UC-FIN-14 银行对账与未达账项**（`use-cases.md:269-288`）断言④ 逐字：

```
RECONCILED 时若存在未达 → 生成调整凭证(businessType=BANK_RECON_ADJ)
```

**L1 行级要求判定**：UC-FIN-09/14 断言④ L1 字面仅要求「生成调整凭证(BANK_RECON_ADJ)」——**未显式规定凭证行级 Dr/Cr/科目/金额字段断言契约**（L1 聚焦凭证生成存在性 + businessType）。行级科目结构属 L2 设计参考层（§4 真相源层级：L1 权威 > L2 设计参考）。

**L2 行级科目结构**（`docs/design/finance/bank-reconciliation.md §业务规则`，设计参考层）：
- 规则 3（:100）「方向语义对齐：银行"借"= 企业账面"贷"(资金流出)，反之亦然。勾对时必须方向相反且金额相等」
- 规则 4（:102-104）「未达账项：银行有、账面无 → amtBankNotInBooks...月末生成暂估调整凭证(businessType=BANK_RECON_ADJ)」
- 规则 6（:108）「posted 联动：调节表 RECONCILED 时若存在未达账项,生成调整凭证(isReversed=false),下月初自动红冲(跨期还原)」

> **L1/L2 行级正确性要求**：L1 要求凭证生成（存在性）；L2 描述行级科目结构（银行已收→借银行存款/贷调整科目；银行已付→借调整科目/贷银行存款）+ 每对借贷平衡 + 整体平衡。两者均**未显式要求测试须断言每行字段**——行级断言属测试覆盖补强项（断言强度维度），非 L1/L2 验收标准。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测）

### 2.1 行级生成逻辑核验（Phase 1 item 1）

> 核验目标：证实 `BankReconAdjAcctDocProvider#createFacts` 行级生成逻辑对称正确（每对借贷平衡 + 整体平衡 + 科目方向符合 L2）。

| 环节 | 文件:行（写时实测） | 关键行为 | 核验状态 |
|---|---|---|---|
| billData 解析 | `BankReconAdjAcctDocProvider.java#createFacts:52-56` | `bankSubject=stringValue(BILL_DATA_BANK_SUBJECT_CODE):53` / `adjSubject=stringValue("ADJ_SUBJECT_CODE"):54` / `bankCredit=decimalValue("TOTAL_BANK_CREDIT"):55` / `bankDebit=decimalValue("TOTAL_BANK_DEBIT"):56` | ✅ |
| **bankCredit 对（银行已收）** | `createFacts:59-63` | `if(bankCredit.signum()>0)` → `fact(bankSubject,DC_DEBIT,bankCredit,"BANK_RECV"):61` + `fact(adjSubject,DC_CREDIT,bankCredit,"ADJ_BANK_RECV"):62`——**借银行存款 / 贷调整科目，同金额 bankCredit** | ✅ 借贷平衡 + 方向符合 L2 规则 3/4 |
| **bankDebit 对（银行已付）** | `createFacts:64-68` | `if(bankDebit.signum()>0)` → `fact(adjSubject,DC_DEBIT,bankDebit,"ADJ_BANK_PAID"):66` + `fact(bankSubject,DC_CREDIT,bankDebit,"BANK_PAID"):67`——**借调整科目 / 贷银行存款，同金额 bankDebit** | ✅ 借贷平衡 + 方向符合 L2 规则 3/4 |
| 行级字段设置 | `BankReconAdjAcctDocProvider.java#fact:72-82` | `setSubjectCode:74` / `setDcDirection:75` / `setAmount:76` / `setAccountKey:77` / `setAmountKey(null):78` / `setMemo("银行对账未达账项调整"):79` / `setBusinessType(BANK_RECON_ADJ.name()):80` | ✅ |
| bankSubject 解析（上游） | `BankReconAdjustmentVoucherBuilder.java#resolveBankSubjectCode:118-130` | = `FundAccount.getSubject().getCode()`（资金账户对应银行存款科目编码，如 `1002`） | ✅ |
| adjSubject 解析（上游） | `BankReconAdjustmentVoucherBuilder.java#resolveAdjSubjectCode:132-139` | = `AppConfig.var("erp-fin.bank-recon-adj-subject-code","2240OTHER")`（未达账项对方科目，默认 `2240OTHER`） | ✅ |
| bankCredit/bankDebit 聚合（上游） | `BankReconAdjustmentVoucherBuilder.java#post:63-71` | `totalBankCredit`=Σ unmatched 行 `dc=CREDIT` 金额（:66-67，银行已收）；`totalBankDebit`=Σ unmatched 行 `dc=DEBIT` 金额（:68-69，银行已付） | ✅ 方向语义对齐 L2 规则 3 |

**行级正确性静态推理（每对借贷平衡 + 整体平衡）**：

- **bankCredit 对**：`fact(bankSubject,DEBIT,bankCredit)` + `fact(adjSubject,CREDIT,bankCredit)`——借/贷同金额 `bankCredit` → 该对借贷平衡；方向 = 银行已收（银行流水贷方）→ 企业借银行存款（资产增）/ 贷调整科目，符合 L2 规则 3（方向相反）+ 规则 4（未达调整）。
- **bankDebit 对**：`fact(adjSubject,DEBIT,bankDebit)` + `fact(bankSubject,CREDIT,bankDebit)`——借/贷同金额 `bankDebit` → 该对借贷平衡；方向 = 银行已付（银行流水借方）→ 企业借调整科目 / 贷银行存款（资产减），符合 L2 规则 3。
- **整体平衡**：bankCredit 对（Dr=Cr=bankCredit）+ bankDebit 对（Dr=Cr=bankDebit）互不干扰 → ΣDr = bankCredit+bankDebit = ΣCr → 整体平衡。产出 2 条（仅 bankCredit>0 或仅 bankDebit>0）或 4 条（两者均>0）VoucherFact。

**结论**：`createFacts:51-70` 行级生成逻辑**对称正确**——每对借贷平衡 + 整体平衡 + 科目方向符合 L2 `bank-reconciliation.md §业务规则 3/4/6`。无行为缺陷。

### 2.2 借贷平衡间接保护（关键，写时实测）

> 过账引擎在 Provider 产出 facts 后、写库前强制借贷平衡校验——JUnit post 成功即间接证明 facts 借贷平衡。

| 环节 | 文件:行（写时实测） | 关键行为 | 核验状态 |
|---|---|---|---|
| 平衡合计计算 | `ErpFinPostingProcessor.java#balanceTotals:722-734` | 遍历 facts：`DC_CREDIT`→`totalCredit+=amt`（:727-728）/ else→`totalDebit+=amt`（:729-730） | ✅ |
| **平衡校验守卫** | `ErpFinPostingProcessor.java#assertBalanced:736-742` | `if(totalDebit.compareTo(totalCredit)!=0) throw NopException(ERR_UNBALANCED)`（:737-740）——**借贷不平衡则过账失败** | ✅ 借贷平衡间接保护 |
| 错误码 | `ErpFinPostingErrors.java:44-45` | `ERR_UNBALANCED`「借贷不平衡：借方合计={totalDebit}，贷方合计={totalCredit}」 | ✅ |

**间接保护链**：`createFacts` 产出 facts → `balanceTotals:722` 计算 ΣDr/ΣCr → `assertBalanced:736` 强制相等 → 不平衡抛 `ERR_UNBALANCED` 阻止过账。JUnit `testPostGeneratesAdjustmentVoucherAndReverse` post 成功（断言 POSTED:166）→ **间接证明 createFacts 产出 facts 借贷平衡**。即若未来 createFacts 重构破坏借贷配对（如单边 Dr 无 Cr / 金额错配），post 抛 `ERR_UNBALANCED` → JUnit fail。

> **平衡已间接保护；科目方向语义（哪个科目 Dr/Cr）+ 金额值正确性（amount==bankCredit/bankDebit）不经平衡校验保护**——此两项经 E2E 行级断言回归保护（见 §3.2 + §4.2）。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 JUnit 测试断言集核验（Phase 1 item 2）

> 核验目标：grep `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/` 银行对账调整凭证相关 JUnit 全部断言，产出断言集清单 + 标注行级断言缺失。

#### `TestErpFinBankReconciliation#testPostGeneratesAdjustmentVoucherAndReverse:136-171`（存疑点对象）

| 断言 | 行（写时实测） | 断言内容 | 强度 |
|---|---|---|---|
| billR 计数 | :161 | `assertTrue(countBillLinks(recon.getCode()) >= 1)`（存在未达项时生成 BANK_RECON_ADJ 调整凭证） | 中（计数） |
| 凭证存在性 | :164 | `assertNotNull(adjVoucherId)`（反查到调整凭证 ID） | 中（存在性） |
| 凭证 docStatus | :166 | `assertEquals(VOUCHER_STATUS_POSTED, adj.getDocStatus())`（调整凭证已过账） | 中（状态） |
| reverse docStatus | :169 | `assertEquals(VOUCHER_STATUS_CANCELLED, reloadRecon(...).getDocStatus())` | 中（状态） |
| reversal 计数 | :170 | `assertTrue(countReversalVouchers(adjVoucherId) >= 1)`（生成红字调整凭证） | 中（计数） |
| **凭证行级 subjectCode** | — | **未断言**（grep `getSubjectCode\|lineOfSubject\|subjectCode` 于本方法 = 零断言命中；helper `unused():299-302` 返回 null 占位） | **缺失** |
| **凭证行级 dcDirection** | — | **未断言** | **缺失** |
| **凭证行级 debitAmount/creditAmount** | — | **未断言** | **缺失** |
| **凭证借贷合计** | — | **未断言**（不断言 totalDebit/totalCredit） | **缺失** |

grep `ErpFinVoucherLine\|subjectCode\|debitAmount\|creditAmount\|dcDirection\|lineOfSubject\|totalDebit\|totalCredit` 于 `TestErpFinBankReconciliation.java`：仅 `import ErpFinVoucherLine:14` + `unused():299-302` 返回 null 占位——**零行级断言命中**，亦**零借贷合计断言**。

#### 其余 bank-recon JUnit（断言强度引用 A1.4 §3）

`TestErpFinBankStatementImport`（6，强：refNo/composite/strict/cross-account/happy）+ `TestErpFinBankStatementMatch`（7，强：三态/方向/manual/occupied）+ `TestErpFinBankReconciliation`（5：平衡/不平衡/CLOSED 拒绝/无未达不产凭证/**post+reverse 仅存在性+计数**）+ `TestErpFinBankReconciliationEndToEnd`（1，全链冒烟，行级同上）——调整凭证测试**仅断言存在性 + billR 计数 + docStatus + reversal 计数**，**未断言行级 Dr/Cr/科目/金额**（与 A1.4 §3 评级「中」一致）。

**JUnit 断言集判定**：`testPostGeneratesAdjustmentVoucherAndReverse` 对调整凭证行级正确性的覆盖 = **零行级断言**（仅存在性/计数/状态），弱于 P2-RC-017（sales AR `countLines==3 + totalDebit==113`）/ P2-RC-029（inventory 估值 `totalDebit/totalCredit==50 + countLines==2`）的「合计 + 计数」——本控制点 JUnit 断言强度**最弱**（连借贷合计都未断言）。

### 3.2 E2E 测试断言集核验（存疑点前提校正，关键）

> 核验目标：grep `tests/e2e/business-actions/fin-bank-recon.action.spec.ts` 银行对账调整凭证 E2E 全部断言，校正 A1.4 §7-2「行级零断言」前提。

#### `fin-bank-recon.action.spec.ts`（282 行，3 case，E2E 行级强补充）

| 断言 | 行（写时实测） | 断言内容 | 强度 |
|---|---|---|---|
| 正向凭证行 借银行存款 | :185-186 | `assertVoucherLines(page, normalVoucherId, [{ subjectCode:'1002', dcDirection:'DEBIT', debitAmount:UNRECONCILED_AMT, creditAmount:0 }, ...])` | **强（行级）** |
| 正向凭证行 贷调整科目 | :187 | `{ subjectCode:'2240OTHER', dcDirection:'CREDIT', debitAmount:0, creditAmount:UNRECONCILED_AMT }` | **强（行级）** |
| 红冲凭证行 借银行存款（同向取负） | :216-217 | `{ subjectCode:'1002', dcDirection:'DEBIT', debitAmount:-UNRECONCILED_AMT, creditAmount:0 }` | **强（行级）** |
| 红冲凭证行 贷调整科目（同向取负） | :218 | `{ subjectCode:'2240OTHER', dcDirection:'CREDIT', debitAmount:0, creditAmount:-UNRECONCILED_AMT }` | **强（行级）** |
| helper 原语 | :15 | `import { assertVoucherLines } from '../orchestration/_helper'`（行级断言原语） | ✅ |

**E2E 断言集判定**：`fin-bank-recon.action.spec.ts:182-219` **强断言行级 subjectCode（1002/2240OTHER）+ dcDirection（DEBIT/CREDIT）+ debitAmount/creditAmount（精确值 UNRECONCILED_AMT）**，正向凭证（Dr 1002 / Cr 2240OTHER）+ 红冲凭证（同向取负）**双向行级断言**。E2E 行级断言精确匹配 `createFacts:61-62`（bankCredit>0 → Dr bankSubject[1002] / Cr adjSubject[2240OTHER]）。

**存疑点前提校正（关键差异增量）**：A1.4 §7-2 / §3 称「`BankReconAdjAcctDocProvider.createFacts` 产出的 2-4 条 VoucherFact 的行级...正确性无测试断言」**仅对 JUnit 成立**；E2E `fin-bank-recon.action.spec.ts:185-219` **实际已强断言行级**（A1.4 §3 评级「中」+ §4.4 称「E2E setup 含 1 条 UNMATCHED CREDIT 行触发调整凭证（Dr 1002 / Cr 2240OTHER）」未明确计入此行级断言强度）。**行级正确性经 E2E 层回归保护**——若 createFacts 重构破坏科目方向/金额值（如 Dr/Cr 互换 / 金额错配但保持借贷平衡），E2E `assertVoucherLines` 会 fail。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2/A1.4 复用（§去重协议）

| 既有已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| 银行对账独立子系统 + 调整凭证生成行为已证实 | MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365` + A1.4 §2.4/§4 | ✅ 复用（凭证生成正确结论直接引用；本验证只补「行级正确性 + 行级断言强度」差异） |
| UC-FIN-09/14 断言④ 调整凭证生成 = 接受 | A1.4 §5（`2026-08-02-1815-...-a1-4-bank-recon.md:281`） | ✅ 复用（本验证不推翻接受，只评行级断言强度差异） |

**声明**：本验证只补「行级正确性 + 行级断言强度」差异（MA2/A1.4 证实凭证生成正确但未评级行级断言强度），不重新核实凭证生成行为本身。

### 4.2 行级正确性的回归保护层次（关键差异增量）

> 本验证比 A1.4 §7-2 起草时认知更完整：行级正确性并非「无任何保护」，而是经**平衡间接保护 + E2E 行级强断言**双层回归保护。

**回归保护层次**（写时实测）：

1. **借贷平衡（每对 Dr==Cr + 整体平衡）**：经 `ErpFinPostingProcessor.assertBalanced:736-742` 间接保护（§2.2）——JUnit post 成功即证明 facts 借贷平衡。
2. **科目存在性/解析**：经过账引擎 `resolveSubjects`（subjectCode→subjectId）间接保护——createFacts 用 null/不存在 subjectCode → 过账失败。JUnit seed `1002`（:141）+ `2240OTHER`（:142）两端均解析成功。
3. **科目方向语义（bankSubject=DEBIT for 银行已收 / adjSubject=CREDIT）+ 金额值正确性（amount==bankCredit/bankDebit）**：经 E2E `assertVoucherLines:185-219` 行级强断言回归保护（§3.2）——若 createFacts 重构互换 Dr/Cr 或错配金额值（保持借贷平衡），E2E fail。

**结论**：行级正确性（Dr/Cr/科目/金额）经 E2E 层回归保护。JUnit 层行级断言缺失属**测试覆盖补强项**（fast 回归层缺失），非行为缺陷（createFacts 静态正确 + E2E 行级强保护 + 平衡间接保护）。此显著强于 A1.4 §7-2 起草时「行级零断言」的字面认知。

### 4.3 MA4↔A5.6 边界声明（Phase 1 item 3）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（需求契约视角，断言强度是否足以覆盖调整凭证语义）；A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「`createFacts` 行级生成逻辑是否正确 + `testPostGeneratesAdjustmentVoucherAndReverse` 的 JUnit 断言强度是否足以覆盖调整凭证行级语义（UC-FIN-09/14 断言④ + L2 §业务规则 3/4）」——**需求契约视角**。裁决依据 = §2 判据（L1/L2 是否要求行级断言 + createFacts 行级生成逻辑是否正确 + 行级断言强度是否削弱语义覆盖）。
- 本验证**不重做 A5.6 E2E 断言强度全量评级**（A5.6 已对 spec 做断言强度分类矩阵）。本验证只评单元测试行级断言这一具体控制点 + createFacts 行级生成逻辑正确性。
- 裁决为「P2 测试覆盖补强 successor」（JUnit 行级断言缺失削弱 fast 回归层语义覆盖且属可回归保护点）→ successor（纯测试代码 MR1 预授权类目），**非 A5.6 范围**（A5.6 是跨切测试质量审计，本验证是单控制点需求符合性裁决）。E2E 行级强补充已登记为本验证裁决的关键减档证据（§3.2），但 JUnit fast 回归层缺失仍独立成立 P2（与 P2-RC-017/029 一致）。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 行级正确性裁决（Phase 1 item 4，方法论 §2 判据 + plan 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 4） | 本验证结果 | 命中 |
|---|---|---|---|
| ① 接受（行级正确性充分） | createFacts 行级生成逻辑对称正确（每对借贷平衡 + 整体平衡 + 科目方向符合 L2）**且**行级断言缺失属测试覆盖补强项非合规缺陷 | (a) createFacts 行级生成逻辑对称正确 ✅（§2.1）；但 (b) JUnit 行级断言**完全缺失**（仅存在性/计数/状态，弱于 P2-RC-017/029 的「合计+计数」）→ 行级断言缺失**削弱 fast 回归层语义覆盖且属可回归保护点**（补 4 行行级断言即可保护）→ 不满足「非合规缺陷」的宽免条件 | 否 |
| **② P2（测试覆盖补强 successor）** | createFacts 行级生成逻辑有偏差 **或** 断言缺失削弱语义覆盖且属可回归保护点 | (a) createFacts 行级生成逻辑**对称正确**（无偏差，§2.1）→ 非行为缺陷（故非 P1/P0）；但 (b) JUnit 行级断言缺失**削弱 fast 回归层语义覆盖**（科目方向 + 金额值仅经 E2E 保护，fast 回归层裸露）**且属可回归保护点**（补 `testPostGeneratesAdjustmentVoucherAndReverse` 行级断言即可保护）✅ | **命中** |

**裁决 = ② P2（测试覆盖补强 successor，watch-only，非行为缺陷）**。

**§2 判据编号**：§2 **P2①**「需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）」——L2（设计参考层，次要验收标准）行级科目结构在 createFacts 行为正确（主路径 OK）+ E2E 行级强补充，但 JUnit fast 回归层行级断言缺失（边界[fast 回归层覆盖]弱）。**非 §2 P1⑤**「测试断言完全缺失或仅冒烟（验收标准无断言）」——UC-FIN-09/14 断言④（凭证生成）**有断言**（JUnit 存在性/计数/状态 + E2E 行级强），非「验收标准无断言」。

### 5.2 三源对照（L1/L2/L3）

- **L1**（`use-cases.md:176,288` UC-FIN-09/14 断言④）：逐字「未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ)」——L1 要求凭证生成（存在性），**未显式规定行级字段断言契约**。L1 验收标准（凭证生成）= 接受（A1.4 §5 维持）。
- **L2**（`bank-reconciliation.md §业务规则 3/4/6`：100-108）：描述行级科目结构（银行已收→借银行存款/贷调整科目；银行已付→借调整科目/贷银行存款）+ 每对借贷平衡 + 整体平衡——**设计参考层**，未规定测试须断言每行字段。
- **L3**（`BankReconAdjAcctDocProvider.createFacts:51-70` + `fact:72-82`）：行级生成逻辑**对称正确**（每对借贷平衡 + 整体平衡 + 科目方向符合 L2），无行为缺陷。

三源一致 → **行级正确性 = createFacts 行为正确（接受）+ 行级断言强度 = P2 测试覆盖补强 successor**（JUnit fast 回归层行级断言缺失削弱语义覆盖）。

### 5.3 与 A1.4 §5 断言④ 接受结论分层一致性

- A1.4 §5 断言④（调整凭证生成）= **接受**（BANK_RECON_ADJ 凭证生成路径 L3-L5 一致）——本验证**确认**其接受结论（createFacts 行级生成逻辑对称正确 + 凭证生成行为经 MA2/A1.4 证实），**不推翻**接受。
- A1.4 §7-2 标注「行级断言缺失未定级」——本验证**定级为 P2 测试覆盖补强 successor（非行为缺陷）**，并校正前提：行级断言**经 E2E 强补充**（非「零断言」），JUnit fast 回归层缺失独立成立 P2。
- 本验证**不升级** A1.4 接受结论为 P1/P0（createFacts 无行为缺陷 + E2E 行级强保护）；仅就 JUnit 行级断言强度补强登记 watch-only successor。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前已 grep `arm-index.md` finance 银行对账/调整凭证/行级断言同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| **P2-RC-017**（A1.18 sales）UC-SAL-01 JUnit AR 凭证仅合计+计数（E2E 行级补充） | sales AR_INVOICE 凭证 JUnit 行级断言强度 | **同型不同域/UC**（sales AR 凭证 SalAcctDocProvider vs finance 银行对账调整凭证 BankReconAdjAcctDocProvider；JUnit 行级断言缺失 + E2E 行级强补充同型） | **交叉引用**（同型范式，本控制点 JUnit 更弱[无合计]，新建不合并） |
| **P2-RC-029**（A1.27 inventory）UC-INV-10 JUnit 存货估值凭证仅合计+计数（无 inventory E2E 行级补充） | inventory 存货估值凭证 JUnit 行级断言强度 | **同型不同域/UC**（inventory 估值 InvAcctDocProvider vs finance 银行对账调整凭证） | **交叉引用**（同型范式，本控制点有 E2E 补充而 P2-RC-029 无，仍同型） |
| P1-RC-030（A1.23 assets）UC-AST-04 处置凭证科目腿（1606 缺失） | assets 处置凭证**科目结构行为偏离** L1 | **不同控制点**（行为缺陷[凭证科目腿缺失]vs 测试断言强度[行为正确断言缺失]） | 不相关 |
| P1-RC-004/005 / P2-RC-001/002/003（A1.4 bank-recon） | 银行对账对方账号/自动红冲/dedup/valueDate/多币种 | **不同控制点**（匹配/红冲/导入/日期/FX vs 调整凭证行级断言强度） | 不相关 |
| A1.4 §5 断言④ 接受（调整凭证生成） | 调整凭证生成符合性 | 本验证是其**行级断言强度差异**（createFacts 行级正确 + JUnit 行级断言缺失），确认接受结论维持 | 复用（分层一致，不推翻） |

grep `arm-index.md` 「调整凭证.*行级\|行级.*断言\|createFacts\|bank-recon.*adj.*line\|BankReconAdjAcctDocProvider\|断言强度.*银行」= **零同域同控制点命中**（无既有 finding 覆盖「TestErpFinBankReconciliation 调整凭证行级断言强度」控制点）；P2-RC-017/029 为**同型不同域/UC**（交叉引用不合并）。

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| **P2-RC-083** | UC-FIN-09/14 断言④ | `TestErpFinBankReconciliation` 调整凭证 JUnit 行级断言完全缺失（仅存在性/计数/状态，弱于 P2-RC-017/029 的「合计+计数」），createFacts 行级生成逻辑对称正确 + E2E 行级强补充 | arm-index 无 finance 银行对账调整凭证行级断言强度 finding；P2-RC-017（sales）/ P2-RC-029（inventory）为同型不同域/UC（不同 Provider/不同测试文件） | **新建**（交叉引用 P2-RC-017/029） |

### 6.3 双向可追溯

- **新 finding → arm-index**：**P2-RC-083** 将写入 `arm-index.md` RC 发现追踪分区（§7 归档纪律）。
- **finding → 修复**：P2-RC-083 为 successor watch-only（本审计不实施修复）；修复（补强 `TestErpFinBankReconciliation` 行级断言）经 MR1（R1.0→RC-R1.n，纯测试代码预授权类目）。
- **既有 finding 复用注记**：UC-FIN-09/14 断言④（调整凭证生成）引用 A1.4 §5 接受（不新建编号，分层一致）；P2-RC-083 交叉引用 P2-RC-017（sales AR）/ P2-RC-029（inventory 估值）同型范式。
- **存疑点闭合**：A1.4 §7-2 经本评估**定级为 P2 测试覆盖补强 successor**（非行为缺陷）+ **前提校正**（行级经 E2E 强补充非零断言），闭合。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.4 §7-2 经行级生成逻辑核验（createFacts 对称正确）+ 测试断言集全集核验（JUnit 行级缺失 + E2E 行级强补充）+ 平衡间接保护链核验**定级为 P2 测试覆盖补强 successor**（createFacts 无行为缺陷，行级经 E2E 回归保护，JUnit fast 回归层缺失独立成立 P2），无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1**（P2 watch-only successor），按 §10 **不触发 MR0/MR1 即时通道**；successor 经 MR1（R1.0→RC-R1.n）批量修复通道。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)）。本报告**不**以 checker 脚本退出码作为门控通过依据。**本验证无生产代码变更**（只读评估：读 createFacts 逻辑 + grep 测试断言 + 引用 MA2/A1.4），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块 :296-316） | Actual（本验证 HEAD 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3/R4/R5/R6/R7/R8/R10/R11/R12a/R12b/R12c | 5/0/0/2/0/0/6/0/69/66/40 | 5/0/0/2/0/0/6/0/69/66/40 | ✅ |

  > 全 19 规则 actual == baseline，**0 漂移**。R1/R2（含 R2c 生产代码总计 1382）经 checker 实测逐项命中基线；R3-R12 因本验证**零生产代码变更**（docs-only）按不变量 actual==baseline（无生产代码可漂移）。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块为准。本验证零生产代码变更，checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（1 项新 P2-RC-083）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.4 §5 断言④ 接受（分层一致，确认维持）+ 与 MA2 A2.5c 银行对账（复用凭证生成正确结论）+ 与 P2-RC-017（sales AR）/ P2-RC-029（inventory 估值）同型不同域/UC（交叉引用）+ MA4↔A5.6 边界（需求契约视角断言强度 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。只读评估（读 createFacts 逻辑 + grep 测试断言 + 引用 MA2/A1.4），未修改代码/ORM/api.xml/view.xml/真相源。

---

## 10. 与 MA2/A1.4 报告差异增量声明（§去重协议）

本验证复用 MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`（银行对账独立子系统 + 调整凭证生成行为）+ A1.4 §3 测试证据汇总 + §5 断言④ 接受，**不重新核实凭证生成行为本身**。只补 MA2/A1.4 未覆盖的「行级正确性 + 行级断言强度」差异：

1. **行级生成逻辑核验**（A1.4 §2.4 未深入行级）：`createFacts:51-70` + `fact:72-82` 行级生成逻辑对称正确（bankCredit 对 借银行存款/贷调整科目 + bankDebit 对 借调整科目/贷银行存款）——证实 createFacts 无行为缺陷。
2. **行级断言强度定级**（A1.4 §7-2 标注为「行级断言缺失未定级」）：本验证定级 = **P2 测试覆盖补强 successor（非行为缺陷）**，与 P2-RC-017/029 同型。
3. **存疑点前提校正**（A1.4 起草时未识别）：A1.4 §7-2 / §3 称「行级零断言」**仅对 JUnit 成立**；E2E `fin-bank-recon.action.spec.ts:185-219` **实际已强断言行级**（正向 + 红冲凭证双向 subjectCode/dcDirection/debitAmount/creditAmount），行级正确性经 E2E 层回归保护。
4. **平衡间接保护链识别**（A1.4 未识别）：过账引擎 `assertBalanced:736-742` 强制借贷平衡，JUnit post 成功即间接证明 facts 借贷平衡。

差异增量与本验证范围一致，无与 MA2/A1.4 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance line-level correctness evaluation**（createFacts 行级正确，1 项 P2 测试覆盖补强 successor，零 P0/P1）

**审查范围**：A1.4 §7-2 存疑点（UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性）评估——行级生成逻辑核验（`createFacts:51-70` + `fact:72-82`）+ 测试断言集全集核验（JUnit 行级缺失 + E2E 行级强补充）+ 平衡间接保护链（`assertBalanced:736-742`）+ MA4↔A5.6 边界声明 + 三源对照 + §2 判据裁决 + 与 arm-index 衔接（1 项新 P2-RC-083）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**接受类**：createFacts 行级生成逻辑对称正确（每对借贷平衡 + 整体平衡 + 科目方向符合 L2 §业务规则 3/4/6）——无行为缺陷；A1.4 §5 断言④（调整凭证生成）接受维持。

**P2 新登记**：P2-RC-083（`TestErpFinBankReconciliation` 调整凭证 JUnit 行级断言完全缺失，createFacts 行级正确 + E2E 行级强补充，JUnit fast 回归层缺失削弱语义覆盖）→ successor watch-only；交叉引用 P2-RC-017（sales AR）/ P2-RC-029（inventory 估值）同型范式。修复 = 补强 JUnit 行级断言，纯测试代码 MR1 预授权类目，不触发 §5 ask-first。

**P0/P1**：无。不触发 MR0/MR1 即时通道。A1.4 §5 断言④ 接受维持（createFacts 无行为缺陷）。

**剩余风险**：无遗留运行时存疑点。P2-RC-083 successor（JUnit 行级断言补强）经 MR1 落地后闭合；行级正确性在此之前经 E2E 行级强断言 + 平衡间接保护双层回归保护。
