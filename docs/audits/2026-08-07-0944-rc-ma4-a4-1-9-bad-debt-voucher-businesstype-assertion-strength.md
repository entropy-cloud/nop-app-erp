# RC MA4 A4.1.9 — TestErpFinBadDebt 凭证 businessType 枚举断言强度评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.9（MA4 运行时行为验证 — A1.3 §7-2：`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` 凭证 businessType 枚举断言强度评估，未断言 `BAD_DEBT_WRITE_OFF`）
> 输入：`docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 2 + §3 测试证据汇总 + §5.1 命题 W1 接受
> 验证 plan：`docs/plans/2026-08-07-0944-3-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §去重协议 + §9 冻结）
> 审计性质：**只读断言强度评估**（grep 测试断言 + 读凭证 businessType 写入点 + 复用 MA2/A1.3；不改代码/ORM/api.xml/真相源）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.3 §7-2：`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` 凭证断言集未断言 `businessType==BAD_DEBT_WRITE_OFF` 枚举值 |
| 凭证 businessType 写入点（写时实测） | `ErpFinBadDebtProcessor.executeWriteOff:163-182` 显式传 `ErpFinBusinessType.BAD_DEBT_WRITE_OFF`（:180）→ `writeBadDebtVoucher:208-214` → `CloseVoucherWriter.writeVoucher` 持久化 `businessType.name()` |
| 测试断言集全集（写时实测） | approvalStatus / voucherId / status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证行 借Allowance(1231,DEBIT)/贷AR(1122,CREDIT) / 无 6701 费用科目；**businessType 枚举零断言**（grep `businessType|getBusinessType|BAD_DEBT_WRITE_OFF` 于 TestErpFinBadDebt.java 仅 javadoc:48 描述性引用 + import:5，无 assert） |
| businessType 角色 | 过账引擎**路由维度**（`ErpFinPostingProcessor.findBillLinks:908-912` 按 `(billCode, businessType)` 查 `ErpFinVoucherBillR`；reverse 路由依赖此匹配），非 L1 验收标准 |
| 间接回归保护（关键发现） | `TestErpFinBadDebtReversal#testWriteOffReverseApproveRedReversesVoucherAndArApItem:75` 反审核成功（断言原凭证 isReversed==true + 红字凭证存在 + ArApItem 对称回退）**仅当 writeOff 时 businessType 被正确持久化为 BAD_DEBT_WRITE_OFF**——reverse 经 `finPostingExecutor.reverse(code, BAD_DEBT_WRITE_OFF)` → `findBillLinks` 按 `(billCode, businessType)` 查找，匹配失败则红冲抛 `ERR_..._NO_POSTED_VOUCHER_TO_REVERSE` → 测试 fail。故 businessType 正确性经 reverse 测试**间接回归保护** |
| 符合性结论（§2 判据） | **接受（断言强度足够）**——凭证科目/方向/金额断言已实质覆盖核销语义；businessType 为路由维度非 L1 验收标准；枚举正确性经 reverse 测试间接保护 |
| 新 finding | **0**（接受，无新 finding，无 successor） |
| P0 即时通道 | 不触发（未出 P0/P1/P2） |

**核心裁决**：存疑点 A1.3 §7-2 的断言强度评估结论 = **接受（断言强度足够，A1.3 §5.1 命题 W1 接受维持）**。判据三层：(1) **businessType 是过账引擎路由维度，非 L1 验收标准**——L1（`use-cases.md` UC-FIN-08 三条验收标准 + 状态轴 WRITTEN_OFF）未要求凭证 businessType 枚举断言；L2（`bad-debt.md §步骤3 + §businessType 映射`）描述凭证科目结构 + businessType 映射表，未规定枚举断言契约。(2) **凭证科目/方向/金额断言已实质覆盖核销语义**——`testWriteOffSetsStatusAndVoucherNoPL` 断言 借Allowance(1231)/贷AR(1122)/金额500/无6701费用科目（不进P&L）+ status==WRITTEN_OFF + openAmount==0，核销语义六维覆盖齐备（A1.3 §3 已评级「强」）。(3) **businessType 枚举正确性经 reverse 测试间接回归保护**——`TestErpFinBadDebtReversal` 反审核成功本身即证明 writeOff 时 businessType 被正确持久化（reverse 路由依赖 `(billCode, businessType)` 匹配）。A1.3 §7 存疑点 2 经本评估**正向消解为接受**，无遗留运行时存疑点，无 successor。**不实施修复**（§5 保护区域 + plan Non-Goals；若未来要求显式 businessType 断言，属 A5.6 测试质量维度，经纯测试代码 MR1 预授权类目，非本 MA4 范围）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

**UC-FIN-08 收款核销发票**（`docs/design/finance/use-cases.md:147`）三条验收标准（本验证核验对象的 L1 锚点，逐字引用 A1.3 §1）：

```
收款单.核销(发票1, 发票2, ...) →
  生成核销明细(每条: 收款单行 ↔ 发票行, 金额)
发票.核销状态: 按累计核销金额计算
  累计核销 < 发票金额 → 部分
  累计核销 == 发票金额 → 已核销
往来单位.应收余额 = Σ发票 - Σ核销 - Σ红字
```

状态轴（`use-cases.md:11`）逐字声明：

```
核销状态(erp-fin/ar-ap-status): OPEN(未核销) / PARTIAL(部分) / SETTLED(已核销) / CANCELLED(已作废) / WRITTEN_OFF(已坏账核销)
```

**L1 未显式要求凭证 businessType 枚举断言**：UC-FIN-08 三条验收标准（核销明细 / 状态派生 / 余额恒等式）+ 状态轴 WRITTEN_OFF 均聚焦于核销语义（辅助账状态 + 往来余额），**businessType 是过账引擎内部路由维度，不出现在任何 L1 验收标准原文中**。坏账核销凭证的 L1 锚点 = `bad-debt.md §步骤3` 描述的科目结构（借Allowance/贷AR，不进 P&L），L1/L2 均未规定凭证须携带特定 businessType 枚举值作为验收条件。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测）

### 2.1 凭证 businessType 写入点核验（Phase 1 item 1）

> 核验目标：证实生产代码正确写入 `BAD_DEBT_WRITE_OFF`（行为正确），评估测试是否须镜像断言。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| executeWriteOff 入口 | `module-finance/erp-fin-service/.../service/processor/ErpFinBadDebtProcessor.java#executeWriteOff:163-182` | `validateAmount(amount, item):165` → `settled+=amount:166-167` → `open-=amount:168-169` → `status=WRITTEN_OFF:170` → 组装凭证行（借Allowance/贷AR）:173-179 | ✅（状态 + 金额变异） |
| **businessType 显式传入（关键）** | `ErpFinBadDebtProcessor.java#executeWriteOff:180` | `writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_WRITE_OFF, "坏账核销", lines)`——**硬编码传 `ErpFinBusinessType.BAD_DEBT_WRITE_OFF` 枚举常量**，非动态派发 | ✅（生产代码正确写入 BAD_DEBT_WRITE_OFF） |
| 凭证写入持久化 | `ErpFinBadDebtProcessor.java#writeBadDebtVoucher:208-214` → `CloseVoucherWriter.writeVoucher` | `businessType.name()`（:211）即 `"BAD_DEBT_WRITE_OFF"` 持久化至 voucher + `ErpFinVoucherBillR.businessType` 列 | ✅（businessType 经 enum.name() 持久化） |
| businessType 枚举定义 | `module-finance/erp-fin-dao/.../ErpFinBusinessType.java:48` | `BAD_DEBT_WRITE_OFF(350)`（坏账核销专用，枚举 name 与 dict `erp-fin/business-type` value 逐一一致，per 枚举 javadoc:3-12） | ✅ |
| reverse 路由消费（间接保护链） | `ErpFinPostingProcessor.java#findBillLinks:908-912` | `q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())))`——reverse 经 `(billCode, businessType)` 双键查 `ErpFinVoucherBillR` | ✅（reverse 路由依赖 businessType 匹配） |

**businessType 在过账引擎的角色**：businessType 是过账引擎的**路由维度**，用于区分核销（BAD_DEBT_WRITE_OFF）/收回（BAD_DEBT_RECOVERY）/准备（BAD_DEBT_RESERVE）/释放（BAD_DEBT_RELEASE）/反审核红冲（REVERSAL）等凭证类型。reverse 路径 `finPostingExecutor.reverse(billHeadCode, businessType)` → `ErpFinPostingProcessor.reverseProcess:218` → `findAllPostedVouchers:890` → `findBillLinks:908` 按 `(billHeadCode, businessType)` 双键查 `ErpFinVoucherBillR`，**若 writeOff 时写入的 businessType 与 reverse 时传入的 businessType 不一致，则查不到原凭证 → 抛 `ERR_..._NO_POSTED_VOUCHER_TO_REVERSE`（ErpFinPostingErrors:48）**。此路由依赖构成本评估的**间接回归保护**核心论据（见 §4.2）。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 测试断言集全集核验（Phase 1 item 2）

> grep `TestErpFinBadDebt.java` + `TestErpFinBadDebtReversal.java` 全部凭证相关断言（科目/方向/金额/status/openAmount/settledAmount/isReversed），产出断言集清单 + 标注 businessType 枚举断言缺失。引用 A1.3 §3 已有「强」评级依据。

#### `TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140-176`（存疑点对象）

| 断言 | 行（写时实测） | 断言内容 | 强度 |
|---|---|---|---|
| approvalStatus | :157 | `assertEquals(APPROVE_STATUS_APPROVED, debt.getApprovalStatus())`（自动审批通过） | 强 |
| voucherId | :158 | `assertNotNull(debt.getVoucherId())`（生成核销凭证） | 强 |
| status | :162 | `assertEquals(AR_AP_STATUS_WRITTEN_OFF, after.getStatus())`（status→WRITTEN_OFF） | **强** |
| openAmount | :163 | `assertEquals(0, after.getOpenAmountFunctional().compareTo(ZERO))`（openAmount→0） | **强** |
| settledAmount | :164 | `assertEquals(0, after.getSettledAmountFunctional().compareTo(500))`（settledAmount+=500） | **强** |
| 凭证行 借Allowance | :169-170 | `lineOfSubject(lines,"1231").getDcDirection()==DC_DEBIT`（核销借 Allowance） | **强** |
| 凭证行 贷AR | :171-172 | `lineOfSubject(lines,"1122").getDcDirection()==DC_CREDIT`（核销贷 AR） | **强** |
| 不进 P&L（无 6701） | :174 | `assertTrue(lines.stream().noneMatch(l -> "6701".equals(l.getSubjectCode())))`（核销不进 P&L） | **强** |
| **businessType 枚举** | — | **未断言**（grep `businessType\|getBusinessType\|BAD_DEBT_WRITE_OFF\|ErpFinBusinessType` 于本方法 = 零断言命中；全文件仅 `import app.erp.fin.dao.ErpFinBusinessType:5` + javadoc:48 描述性引用「BAD_DEBT_WRITE_OFF 凭证」） | 缺失（次要） |

**断言集判定**：`testWriteOffSetsStatusAndVoucherNoPL` 对坏账核销语义的覆盖 = **强**（A1.3 §3 已评级）：status/openAmount/settledAmount 三辅助账断言 + 凭证科目（Allowance/AR）+ 方向（DEBIT/CREDIT）+ 金额（经 lineOfSubject 定位行）+ 不进 P&L（无 6701 费用科目）共 8 项断言，核销语义六维（状态/金额/科目/方向/P&L 隔离）全覆盖。**唯一缺口 = businessType 枚举未断言**（次要，凭证内容已实质覆盖核销语义）。

#### `TestErpFinBadDebtReversal`（反审核测试，businessType 间接保护证据）

| 测试方法 | 行 | businessType 相关断言 | 间接保护机制 |
|---|---|---|---|
| `testWriteOffReverseApproveRedReversesVoucherAndArApItem:75` | :91 调 `badDebtBiz.writeOff`（前置）→ :104 调 `badDebtBiz.reverseApprove` | 断言原凭证 `isReversed==true`（:112）+ 红字凭证存在（:115-117）+ 行同向取负（:120-125）+ ArApItem WRITTEN_OFF→OPEN（:131） | **reverseApprove 经 `finPostingExecutor.reverse(debt.getCode(), BAD_DEBT_WRITE_OFF)`（executeReverseApprove:113-116）→ findBillLinks 按 `(billCode, BAD_DEBT_WRITE_OFF)` 查原凭证；若 writeOff 时写入的 businessType ≠ BAD_DEBT_WRITE_OFF，则查不到 → reverse 抛 ERR → 本测试 fail**。测试 pass 即证明 writeOff 时 businessType 正确持久化。 |
| `testRecoveryReverseApproveRedReversesVoucherAndArApItem:140` | :156 recover 前置 → :167 reverseApprove | 断言 ArApItem OPEN→WRITTEN_OFF 回退对称（:177-180） | 同型（recovery 路径 businessType=BAD_DEBT_RECOVERY 的间接保护） |
| `testGuardNotPostedRejects:185` | :219 assertThrows | 断言未过账坏账单 reverseApprove 抛 `ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED`（:222） | 守卫测试（非 businessType 直接断言，但证实 reverse 路径对凭证缺失的敏感性） |

grep `businessType|getBusinessType|BAD_DEBT_WRITE_OFF|BAD_DEBT_RECOVERY|ErpFinBusinessType` 于 `TestErpFinBadDebtReversal.java`：仅 `import:4` + javadoc:45/47 描述性引用，**零断言命中**。但 reverseApprove 测试的成功执行本身构成 businessType 正确性的**间接断言**（reverse 路由依赖 businessType 匹配，见 §4.2）。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2 复用（§去重协议）

| MA2 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| 坏账核销/收回/反审核红冲闭环强一致（凭证 + ArApItem 对称） | MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` §2.3 场景 d/e/f | ✅ 复用（凭证生成正确结论直接引用；本验证只补「businessType 枚举断言强度」差异） |
| reverseApprove 强一致顺序（红冲凭证失败→tx 回滚→无半状态） | MA2 §2.3 场景 f + 裁决表 | ✅ 复用（reverse 路径的健壮性已证实，本验证引用其「reverse 成功即证明原凭证可路由」结论） |

**声明**：本验证只补「businessType 枚举断言强度」差异（MA2/A1.3 证实凭证生成正确但未评级 businessType 枚举断言的必要性），不重新核实凭证生成行为本身。

### 4.2 businessType 枚举正确性的间接回归保护（关键差异增量）

> 本验证比 A1.3 起草时认知更强的关键发现：businessType 枚举正确性并非「无任何保护」，而是经 `TestErpFinBadDebtReversal` 的 reverse 路径**间接回归保护**。

**间接保护链**（写时实测）：

1. `executeWriteOff:180` 写凭证时 `businessType.name()="BAD_DEBT_WRITE_OFF"` 持久化至 `ErpFinVoucherBillR.businessType`（经 `CloseVoucherWriter`）。
2. `TestErpFinBadDebtReversal#testWriteOffReverseApproveRedReversesVoucherAndArApItem:91` 前置调 `writeOff`（生成原凭证）。
3. `:104` 调 `badDebtBiz.reverseApprove(debt.getId())` → `ErpFinBadDebtProcessor.executeReverseApprove:113-116` 判定 docType==WRITE_OFF → `businessType = BAD_DEBT_WRITE_OFF` → `finPostingExecutor.reverse(debt.getCode(), BAD_DEBT_WRITE_OFF):116`。
4. `FinPostingExecutor.reverse:31` → `voucherBiz.reverse(billHeadCode, businessType)` → `ErpFinPostingProcessor.reverseProcess:218` → `findAllPostedVouchers:890` → **`findBillLinks:908-912` 按 `(billCode=billHeadCode, businessType="BAD_DEBT_WRITE_OFF")` 查 `ErpFinVoucherBillR`**。
5. **若 step 1 写入的 businessType ≠ "BAD_DEBT_WRITE_OFF"（如误写为 BAD_DEBT_RECOVERY），则 findBillLinks 返回空 → reverseProcess 抛 `ERR_..._NO_POSTED_VOUCHER_TO_REVERSE`（ErpFinPostingErrors:48）→ 测试 fail**。
6. 测试实际 pass（断言 isReversed==true + 红字凭证存在 + ArApItem 回退）→ **证明 step 1 写入的 businessType == "BAD_DEBT_WRITE_OFF"**。

**结论**：businessType 枚举正确性经 reverse 测试间接回归保护。若未来代码变更使 `executeWriteOff` 误写其他 businessType，`TestErpFinBadDebtReversal` 会立即 fail（reverse 找不到原凭证）。此间接保护显著强于 A1.3 起草时「测试未引用 businessType」的字面认知——businessType 正确性并非裸露无保护，而是经 reverse 路由依赖被结构性捕获。

### 4.3 MA4↔A5.6 边界声明（Phase 1 item 3）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（需求契约视角，断言强度是否足以覆盖核销语义）；A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「`testWriteOffSetsStatusAndVoucherNoPL` 的断言强度是否足以覆盖坏账核销语义（UC-FIN-08 + 状态轴 WRITTEN_OFF）」——**需求契约视角**。裁决依据 = §2 判据（L1/L2 是否要求 businessType 枚举断言 + 凭证内容断言是否实质覆盖核销语义）。
- 本验证**不重做 A5.6 E2E 断言强度全量评级**（A5.6 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md` 已对 258 spec 做断言强度分类矩阵）。本验证只评单元测试 businessType 枚举断言这一具体控制点。
- 若裁决为「断言强度足够」（接受）→ 无 successor；若裁决为「businessType 枚举断言缺失削弱语义覆盖且属可回归保护点」→ P2 测试覆盖补强 successor（纯测试代码 MR1 预授权类目），**非 A5.6 范围**（A5.6 是跨切测试质量审计，本验证是单控制点需求符合性裁决）。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 断言强度裁决（Phase 1 item 4，方法论 §2 判据 + 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 4） | 本验证结果 | 命中 |
|---|---|---|---|
| **① 接受（断言强度足够）** | 凭证科目/方向/金额断言已实质覆盖核销语义（businessType 为路由维度非 L1 验收标准，L1/L2 未要求枚举断言） | (a) 凭证内容断言（借Allowance/贷AR/金额500/无6701 + status/openAmount/settledAmount）实质覆盖核销语义 ✅；(b) businessType 是过账引擎路由维度，L1 UC-FIN-08 三验收标准 + 状态轴 WRITTEN_OFF 均未要求枚举断言 ✅；(c) L2 `bad-debt.md §步骤3 + §businessType 映射` 描述科目结构 + 映射表，未规定枚举断言契约 ✅；(d) businessType 正确性经 reverse 测试间接回归保护 ✅ | **命中** |
| ② P2（测试覆盖补强 successor） | businessType 枚举断言缺失削弱语义覆盖且属可回归保护点（如 future 凭证类型混入致误判） | businessType 正确性**已有间接回归保护**（reverse 测试，§4.2），非裸露无保护；凭证内容断言已实质覆盖核销语义；枚举断言缺失不削弱语义覆盖（凭证科目/方向/金额已独占表达核销语义） | 否 |

**裁决 = ① 接受（断言强度足够）**。

### 5.2 三源对照（L1/L2/L3）

- **L1**（`use-cases.md:147` UC-FIN-08 三验收标准 + `:11` 状态轴 WRITTEN_OFF）：验收标准聚焦核销明细/状态派生/余额恒等式 + 状态轴 WRITTEN_OFF；**businessType 不出现在任何 L1 验收标准原文**。
- **L2**（`bad-debt.md §步骤3` 核销分录 + `§businessType 映射` 表）：`§步骤3` 描述凭证科目结构（借Allowance/贷AR，不进 P&L）；`§businessType 映射` 是 businessType → 步骤/借贷方向/触发动作的映射表（设计参考，说明 businessType 是过账路由维度），**未规定测试须断言枚举值**。
- **L3**（`ErpFinBadDebtProcessor.executeWriteOff:180` 显式传 `BAD_DEBT_WRITE_OFF` + `findBillLinks:908-912` reverse 路由依赖）：生产代码正确写入 + reverse 路由依赖该值。

三源一致 → **断言强度 = 接受**（凭证内容断言实质覆盖核销语义 + businessType 非验收标准 + 间接回归保护）。

### 5.3 与 A1.3 §5.1 命题 W1 接受结论分层一致性

- A1.3 §5.1 命题 W1（坏账核销）= **接受**（5 验收标准 L3-L5 全证据一致：status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证 借Allowance/贷AR / 无 6701）——本验证**确认**其断言强度评级（A1.3 §3 已标「强」），businessType 枚举断言缺失不推翻接受结论。
- A1.3 §7 存疑点 2 标注为「次要断言强度缺口（凭证内容已实质覆盖核销语义），非合规缺陷」——本验证**正向消解**该存疑点为「接受（断言强度足够）」，并补充关键证据：businessType 正确性经 reverse 测试间接回归保护（A1.3 起草时未识别此间接保护链）。
- 本验证**不升级** A1.3 接受结论（无 P0/P1/P2）；不推翻命题 W1 接受。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance 坏账/凭证 businessType/测试断言强度同域同控制点。本验证裁决 = 接受，**产出 0 项新 finding**。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| P2-RC-082（A4.1.8）PARTIAL→WRITTEN_OFF→partner.receivableBalance 边界测试覆盖缺口 | partner balance sumOpen 隐式排除边界 | **不同控制点**（partner 余额边界测试 vs 凭证 businessType 枚举断言强度）；P2-RC-082 是行为边界测试缺口（行为正确但无边界测试），本验证是断言强度评估（断言强度足够，无缺口） | 不重开 / 不复用（不同控制点） |
| P1-MA1-018 ErpFinBusinessType enum↔dict value 漂移 4 项 | enum name 与 dict value 命名一致性 | **不同控制点**（enum-dict 命名 drift[已 resolved R1.x] vs 测试断言强度）；P1-MA1-018 是持久化值与 UI dict 不符致筛选漏命中，本验证是测试是否断言枚举值 | 不重开（不同维度） |
| P0-MA2-018 / P1-MA2-087 voucher billR `(billCode, businessType)` UK / CloseVoucherWriter 幂等 | 并发幂等（UK 缺失致重复凭证） | **不同控制点**（并发幂等 vs 测试断言强度）；P0-MA2-018 是并发重复过账，本验证是单线程断言强度 | 不重开（不同维度） |
| A1.3 §5.1 命题 W1 接受（凭证科目/方向/金额/无费用科目断言齐备） | 坏账核销符合性 | 本验证是其**断言强度差异**（businessType 枚举断言缺失），确认接受结论维持 | 复用（分层一致，不推翻） |

grep `arm-index.md` 「bad.?debt.*businessType|businessType.*断言|BAD_DEBT_WRITE_OFF.*assert|断言强度.*坏账」RC 系列 = **零命中**（无既有 finding 覆盖「TestErpFinBadDebt businessType 枚举断言强度」控制点）。

### 6.2 新建 finding 裁决

**无新 finding**。本验证裁决 = 接受（断言强度足够），UC-FIN-08 + 命题 W1 接受维持，businessType 枚举断言缺失不构成合规缺陷（凭证内容断言实质覆盖核销语义 + businessType 非验收标准 + 间接回归保护）。本验证**不向 arm-index 新增 `P*-RC-xxx` 行**，**不登记 successor**。

### 6.3 双向可追溯

- **新 finding → arm-index**：N/A（无新 finding）。
- **静态存疑点闭合**：A1.3 §7 存疑点 2 经本评估**正向消解为接受**（断言强度足够，无 successor），闭合。
- **与 A4.1.8（P2-RC-082）边界声明**：本验证（A4.1.9）与 A4.1.8（P2-RC-082）同属 A1.3 §7 存疑点族（§7-1/§7-2），但**不同控制点**——A4.1.8 是 partner 余额边界测试缺口（行为正确无边界测试 → P2），本验证是 businessType 枚举断言强度（断言强度足够 → 接受）。两者结论差异源于控制点性质不同（边界测试缺失 vs 断言强度充分），无矛盾。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.3 §7-2 经断言强度评估 + 间接回归保护链核验**正向消解为接受**（断言强度足够，businessType 正确性经 reverse 测试间接保护），无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1/P2**（接受），按 §10 **不触发 MR0/MR1**。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本验证实测 EXIT=0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读评估：grep 测试断言 + 读凭证写入点 + 引用 MA2/A1.3），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块） | Actual（本验证 HEAD 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3/R4/R5/R6/R7/R8/R10/R11/R12a/R12b/R12c | 5/0/0/2/0/0/6/0/69/66/40 | 5/0/0/2/0/0/6/0/69/66/40 | ✅ |

  > 全 19 规则 actual == baseline，**0 漂移**。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块（R2b=229 / R2c=1382 / R2d=34 等，line 296-316）为准；上方 `## 基线表` 行（R2b=240 等）已被多轮同步注记裁决性更新，以 machine-readable 块为权威。本验证零生产代码变更（docs-only），checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.3 §5.1 命题 W1 接受（分层一致，确认维持）+ 与 MA2 A2.5c 坏账强测试（复用凭证生成正确结论）+ 与 A4.1.8 P2-RC-082（不同控制点：partner 余额边界测试 vs businessType 枚举断言强度）+ MA4↔A5.6 边界（需求契约视角断言强度 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。只读评估（grep 测试断言 + 读凭证写入点 + 引用 MA2/A1.3），未修改代码/ORM/api.xml/view.xml/真相源。

---

## 10. 与 MA2/A1.3 报告差异增量声明（§去重协议）

本验证复用 MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` §2.3 场景 d/e/f（坏账核销/收回/反审核红冲闭环强一致）+ A1.3 §3 测试证据汇总（命题 W1 强评级）+ §5.1 命题 W1 接受，**不重新核实凭证生成行为本身**。只补 MA2/A1.3 未覆盖的「businessType 枚举断言强度」差异：

1. **businessType 写入点核验**（A1.3 未深入）：`executeWriteOff:180` 显式传 `BAD_DEBT_WRITE_OFF` + `findBillLinks` reverse 路由依赖——证实生产代码正确写入 + businessType 是路由维度非验收标准。
2. **断言强度评级**（A1.3 §7-2 标注为「次要缺口未定级」）：本验证定级 = **接受（断言强度足够）**，凭证内容断言实质覆盖核销语义 + businessType 非验收标准。
3. **间接回归保护链识别**（A1.3/A2 起草时未识别）：businessType 正确性经 `TestErpFinBadDebtReversal` reverse 路径间接回归保护（reverse 路由依赖 `(billCode, businessType)` 匹配，reverse 测试 pass 即证明 writeOff 时 businessType 正确持久化）。

差异增量与本验证范围一致，无与 MA2/A1.3 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance assertion-strength evaluation**（断言强度接受，零 P0/P1/P2 新 finding，零 successor）

**审查范围**：A1.3 §7-2 存疑点（`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` 凭证 businessType 枚举断言强度）评估——凭证 businessType 写入点核验（`executeWriteOff:180`）+ 测试断言集全集核验（8 项强断言 + businessType 缺失）+ businessType 角色（过账引擎路由维度）+ 间接回归保护链（reverse 测试）+ MA4↔A5.6 边界声明 + 三源对照 + §2 判据裁决 + 与 arm-index 衔接（零新 finding）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**接受类**：断言强度足够——凭证科目/方向/金额断言（借Allowance/贷AR/金额500/无6701）+ 辅助账断言（status==WRITTEN_OFF/openAmount==0/settledAmount==500）实质覆盖坏账核销语义；businessType 是过账引擎路由维度非 L1 验收标准（L1/L2 未要求枚举断言）；businessType 正确性经 `TestErpFinBadDebtReversal` reverse 路径间接回归保护。

**P0/P1/P2**：无。不触发 MR0/MR1。A1.3 §5.1 命题 W1 接受维持。

**剩余风险**：无遗留运行时存疑点。若未来要求显式 businessType 枚举断言（作为测试质量增强，非合规要求），属 A5.6 测试质量维度，经纯测试代码 MR1 预授权类目，非本 MA4 范围。
