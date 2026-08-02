# RC MA1 A1.3 — finance-F3 AR/AP 核销与坏账 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.3（MA1 需求追踪矩阵审计 — finance-F3 AR/AP 核销与坏账）
> 审计 plan：`docs/plans/2026-08-02-1600-3-rc-ma1-a1-3-finance-f3-arap-reconciliation.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-08，1 UC）+ 坏账 WRITTEN_OFF 状态轴衍生命题（status 轴 `use-cases.md:11` 含 WRITTEN_OFF）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.3
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **0** | 无 |
| **P2**（新登记） | **0** | 无 |
| **接受**（符合需求契约） | **1 UC + 坏账状态轴 4 命题** | UC-FIN-08 三条验收标准全接受 + 坏账核销/收回/反审核/准备 4 衍生命题全接受 |
| MA2 既有行为证据复用 | A2.5c 6 项 P2 watch-only | 无升级（详见 §4 / §9） |

**整体裁决**：A1.3 切片 UC-FIN-08 五级追踪矩阵填齐，三条逐字验收标准（核销明细生成 / 累计核销→部分|已核销状态派生 / 应收余额恒等式 Σ发票 − Σ核销 − Σ红字）经 L3-L5 四级证据确认**全部符合**。坏账 WRITTEN_OFF 状态轴 4 项衍生命题（核销/收回/反审核红冲闭环/准备计提 + 一致排除）一并核查符合。**零 P0 / 零 P1 / 零新 P2**——三项已知注意点（①核销不自身产 GL 凭证 ②往来余额为 ErpMdPartner 缓存字段 ③WRITTEN_OFF 一致排除）按 §2 判据**全部裁定"接受"**（设计一致，契约未要求核销产 GL 凭证；余额恒等式经 refresh 重算成立；WRITTEN_OFF 在期末门禁/账龄/坏账准备/开放项查询四处一致排除）。**1 项静态存疑点**（注意点②的边界：`PartnerBalanceUpdater.sumOpen` 仅显式排除 SETTLED/CANCELLED，WRITTEN_OFF 隐式排除依赖 `executeWriteOff` 置零 openAmount 的不变量）交 MA4 A4.1 运行时展开。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-08 收款核销发票（`use-cases.md:147`）

**场景**：收款单核销一张或多张应收发票（部分/全额）。

```
可验证断言（见 ar-ap-reconciliation.md §核销流程/§状态）：
收款单.核销(发票1, 发票2, ...) →
  生成核销明细(每条: 收款单行 ↔ 发票行, 金额)
发票.核销状态: 按累计核销金额计算
  累计核销 < 发票金额 → 部分
  累计核销 == 发票金额 → 已核销
往来单位.应收余额 = Σ发票 - Σ核销 - Σ红字
```

**涉及机制**：`ar-ap-reconciliation.md §核销流程/§状态/§余额计算`

### 坏账 WRITTEN_OFF 状态轴衍生命题（状态轴 `use-cases.md:11`）

> roadmap A1.3 标题为"AR/AP 核销与坏账"。UC-FIN-08 状态轴（`use-cases.md:11`）逐字声明 `核销状态(erp-fin/ar-ap-status): OPEN(未核销) / PARTIAL(部分) / SETTLED(已核销) / CANCELLED(已作废) / WRITTEN_OFF(已坏账核销)`。坏账无独立 UC 编号，但状态轴含 WRITTEN_OFF 且 `ar-ap-reconciliation.md §核销状态/§状态扩展` 衍生覆盖坏账，故作为状态轴衍生命题一并核查（§3 不抽样：凡 L1 契约触及的控制点逐条核）。命题清单：

- **命题 W1（坏账核销）**：经 `ErpFinBadDebtBizModel.writeOff` → ArApItem `status=WRITTEN_OFF` + `openAmount=0` + 坏账核销凭证（`bad-debt.md §步骤3`）。
- **命题 W2（坏账收回）**：经 `recover` → ArApItem `WRITTEN_OFF → OPEN` + 恢复 openAmount + 收回凭证（`bad-debt.md §步骤4a`）。
- **命题 W3（坏账反审核红冲闭环）**：`reverseApprove` 红冲凭证 → ArApItem 对称回退 → APPROVED→REJECTED 强一致（`bad-debt.md §步骤4b`）。
- **命题 W4（坏账准备 + WRITTEN_OFF 一致排除）**：`BadDebtProvisionCalculator` 账龄分桶法计提必需准备，排除 SETTLED/WRITTEN_OFF/CANCELLED；期末结账前置门禁、账龄、开放项查询一致排除 WRITTEN_OFF（`ar-ap-reconciliation.md §状态扩展` + `§账龄分析`）。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-finance/erp-fin-service/.../`）。L3 引用格式遵循 §1 L3 规范（含行号）。行号经本次审计实测复核。

### 2.1 核销主链（UC-FIN-08 验收标准①②③共用）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 核销聚合根 Facade | `module-finance/erp-fin-service/.../service/entity/ErpFinReconciliationBizModel.java` create:68-75 / post:78-81 / reverse:84-87 / previewReverse:94-120 / runAutoReconciliation:125-131 / checkDualSideConsistency:134-140 | ✅ |
| 核销过账编排 | `module-finance/erp-fin-service/.../service/processor/ErpFinReconciliationPostProcessor.java` post:21-50（requireHead :22 → docStatus==DRAFT 守卫 :23-25 → loadLines :26 → validateLine 循环 :33-35 → **FX 门控** :37-42 [isReconFxGainLossEnabled ? settleWithFx+generateReconFxVoucher : settle] → POSTED :43 → postedAt/By :44-45 → flushBeforeBalance :47 → partnerBalanceUpdater.refresh :48） | ✅ |
| 核销红冲编排 | `ErpFinReconciliationReverseProcessor.java`（reverse：reverseSettle + REVERSED + refresh） | ✅ |
| 自动核销编排 | `ErpFinReconciliationRunAutoReconciliationProcessor.java:27`（class decl，extends AbstractErpFinReconciliationProcessor）→ `AutoReconciliationEngine.java:40`（class）/ matchFifo:98 / matchByAmount:142 / matchByRatio:169（三策略） | ✅ |
| 结算状态机 | `module-finance/erp-fin-service/.../service/reconciliation/ReconciliationSettler.java` settle:32-47（applySettlement 双方辅助账回写 + 头合计）/ settleWithFx:58-80（per-item functional 折算 + fxGainLoss）/ reverseSettle:97-105（相反数恢复）/ applySettlement:107-126（settled/open 回写 + resolveStatus）/ **resolveStatus:128-138**（settled≤0→OPEN / settled≥total→SETTLED / 否则 PARTIAL） | ✅ |
| 双边一致性 | `DualSideConsistencyChecker.java` check:52-102（finSettled[辅助账 settledAmountFunctional] vs domSettled[ErpSalInvoice.receivedAmount / ErpPurInvoice.paidAmount] 差异报告，只读 @BizQuery） | ✅ |
| 往来余额刷新 | `module-finance/erp-fin-service/.../service/reconciliation/PartnerBalanceUpdater.java` refresh:33-44（partner.receivableBalance = sumOpen(RECEIVABLE) / payableBalance = sumOpen(PAYABLE)）/ sumOpen:46-62（**notIn(status, [SETTLED, CANCELLED])** :51-52，Σ openAmountFunctional） | ✅（机制 B，javadoc:22-23 自述） |
| 核销行实体（验收①承载） | `ErpFinReconciliationLine`（ORM）：`paymentItemId`↔`invoiceItemId`（均指向 `ErpFinArApItem`）+ `settledAmountSource/Functional` | ✅ |
| 核销本身不写 GL 凭证 | `ErpFinReconciliationBizModel.java` javadoc:46（"核销不直接生成 GL 凭证（凭证由收付款审核时生成）"）+ `ErpFinReconciliationPostProcessor.java:37-42`（唯一 GL 影响 = 可选汇兑损益凭证，gated by `isReconFxGainLossEnabled`，默认 false） | ✅（设计一致，见 §5 注意点①） |

### 2.2 坏账链（命题 W1-W4）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 坏账 Facade | `module-finance/erp-fin-service/.../service/entity/ErpFinBadDebtBizModel.java` writeOff:55 / reverseApprove:83（→ reverseApproveProcessor）/ reverseBadDebtProvision:101（→ badDebtProvisionService） | ✅ |
| 坏账核销生效（W1） | `module-finance/erp-fin-service/.../service/processor/ErpFinBadDebtProcessor.java` executeWriteOff:163-182（validateAmount :165 → settled+=amount :166-167 → **open-=amount :168-169** → **status=WRITTEN_OFF :170** → BAD_DEBT_WRITE_OFF 凭证 借Allowance/贷AR :173-181） | ✅ |
| 坏账收回生效（W2） | `ErpFinBadDebtProcessor.java` executeRecovery:188-206（settled-=amount :190-191 → open+=amount :192-193 → **status=OPEN :194** → BAD_DEBT_RECOVERY 凭证 借AR/贷Allowance :197-205） | ✅ |
| 坏账反审核红冲闭环（W3） | `ErpFinBadDebtProcessor.java` executeReverseApprove:111-144（step1 红冲凭证 finPostingExecutor.reverse :116 → step2 ArApItem 对称回退 :118-138 [writeOff 反向: WRITTEN_OFF→OPEN / recovery 反向: OPEN→WRITTEN_OFF] → step3 APPROVED→REJECTED :141） | ✅ |
| 坏账准备计提（W4） | `module-finance/erp-fin-service/.../service/baddebt/BadDebtProvisionCalculator.java` calculate:37-81（账龄 5 桶 × lossRate → requiredProvision；**排除 SETTLED/CANCELLED/WRITTEN_OFF :55-58** + 排除 open≤0 :51-53）+ `BadDebtProvisionService.java` findReceivableOpenItems:288-297（**notIn(status, [SETTLED, CANCELLED, WRITTEN_OFF])** :292-295） | ✅ |

### 2.3 WRITTEN_OFF 一致排除（命题 W4 + 注意点③）

| 排除点 | 文件:行 | 排除方式 | 审计状态 |
|---|---|---|---|
| WRITTEN_OFF 状态定义 | `ErpFinConstants.java:370`（`AR_AP_STATUS_WRITTEN_OFF`）+ dict `erp-fin/ar-ap-status.dict.yaml:25`（`value: WRITTEN_OFF`，label 已坏账核销 :24） | 真实状态值 | ✅ |
| 开放项查询 | `ErpFinArApItemBizModel.java` findOpenItemsByPartner:38（**in(status, [OPEN, PARTIAL])** :44-45）/ aging:64（in(status, [OPEN, PARTIAL]) :72-73） | 正向包含 → 隐式排除 WRITTEN_OFF/SETTLED/CANCELLED | ✅ |
| 期末结账前置门禁 | `ErpFinAccountingPeriodProcessor.java` findUnsettledArApCodes:442（post-query stream filter 排除 SETTLED:457 / CANCELLED:458 / **WRITTEN_OFF:459**） | 显式排除 | ✅ |
| 坏账准备基线 | `BadDebtProvisionService.java:292-295` + `BadDebtProvisionCalculator.java:55-58` | 显式排除 | ✅ |
| 往来余额刷新 | `PartnerBalanceUpdater.java:51-52`（**notIn(status, [SETTLED, CANCELLED])**，未显式排除 WRITTEN_OFF） | WRITTEN_OFF 项进入查询，但 openAmount==0（executeWriteOff:168 置零）→ 贡献 0；**隐式排除依赖 openAmount=0 不变量** | ⚠️ 静态存疑点（见 §7，机制正确） |

---

## 3. 测试证据（L4，注明断言强度）

> 断言强度分档引用 MA5（`docs/audits/2026-07-29-1430-arm-ma5-finance-test-coverage.md`）评级口径：强断言 = 断言验收标准数值/状态；弱断言 = 仅断言不抛异常或仅冒烟。行号经本次审计实测复核。

| UC / 命题 | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| UC-FIN-08 ①② 核销明细 + 状态派生（部分） | `reconciliation/TestErpFinReconciliation.java#testPartialSettlement:49` | **强** | ✅ 断言 invoice settledAmount=300 / openAmount=700 / **status=PARTIAL** (:62) |
| UC-FIN-08 ①② 核销明细 + 状态派生（全额） | `TestErpFinReconciliation.java#testFullSettlement:74` | **强** | ✅ 断言 **status=SETTLED** (:84-85) + 核销行 created |
| UC-FIN-08 ①② 自动核销（FIFO） | `reconciliation/TestErpFinAutoReconciliation.java#testFifoMultipleInvoicesPaidBySingleReceipt:48` | **强** | ✅ FIFO per-item settled/open + SETTLED/PARTIAL (:63-67) |
| UC-FIN-08 ①② 自动核销（BY_AMOUNT/BY_RATIO） | `TestErpFinAutoReconciliation.java#testByAmountExactMatch:71` / `#testByRatioProportionalAllocation:100` | **强** | ✅ 精确匹配 / 比例分摊 |
| UC-FIN-08 ③ 应收余额恒等式 | `reconciliation/TestErpFinPartnerBalance.java#testReceivableBalanceViaReconciliation:70` / `#testPayableBalanceDrivenByOpenAmount:46` | **强** | ✅ 断言 partner.receivableBalance==0 (全核销 :79) / payableBalance==Σ openAmount (0→800→1200 经 post/reverse :50/:60/:65)——**数学等价于 Σ发票 − Σ核销 − Σ红字**（openAmount = amount − settled；红字发票 amount 为负） |
| UC-FIN-08 负向守卫（验收②边界） | `TestErpFinReconciliation.java#testCrossPartnerRejected:89` / `#testOverAmountRejected:104` / `#testDateBeforeInvoiceRejected:118` / `#testPostPostedAgainRejected:155` | **强** | ✅ 跨伙伴 / 超额 / 日期序 / 重复过账 全 `assertThrows` + errorCode |
| UC-FIN-08 红冲（验收②回退） | `TestErpFinReconciliation.java#testReverseRestoresItems:131` + `reconciliation/TestErpFinReconciliationReversePreview.java` | **强** | ✅ reverse→reverseSettle→OPEN (:147) |
| UC-FIN-08 双边一致性 | `reconciliation/TestErpFinDualSideConsistency.java#testInconsistentWhenFinanceSettledMore:64` / `#testInconsistentWhenDomainSettledMore:82` / `#testPartnerLevelReportCorrect:97` | **强** | ✅ finSettled vs domSettled 差异检测（diff=40 :78） |
| 命题 W1 坏账核销 | `entity/TestErpFinBadDebt.java#testWriteOffSetsStatusAndVoucherNoPL:140` | **强** | ✅ 断言 **status==WRITTEN_OFF** (:162) / **openAmount==0** (:163) / settledAmount==500 (:164) / 凭证 借Allowance/贷AR + **无 6701 费用科目** (:174) |
| 命题 W2 坏账收回 | `TestErpFinBadDebt.java#testRecoveryRestoresArApItem:179` | **强** | ✅ WRITTEN_OFF→OPEN + openAmount 恢复 + RECOVERY 凭证 |
| 命题 W3 坏账反审核 | `entity/TestErpFinBadDebtReversal.java`（per MA2 §2.3 场景 f）+ `entity/TestErpFinBadDebtProvisionReversal.java#testGuardNoProvisionVoucherRejects:178` / `#testGuardPeriodFinalClosedRejects:197` | **强** | ✅ writeOff/recovery 双向红冲闭环 + 守卫（ERR_BAD_DEBT_PROVISION_NOT_FOUND / ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED） |
| 命题 W4 坏账准备 + 一致排除 | `TestErpFinBadDebt.java#testAgingBucketProvisionAndReserve:76` / `#testProvisionExcludesNegativeAndWrittenOff:114` / `#testPeriodCloseAllowanceGateBlocksWhenShortfall:266` | **强** | ✅ 账龄 5 桶准备 + 排除负余额/WRITTEN_OFF + 期末门禁 shortfall |

**测试证据汇总**：UC-FIN-08 三条验收标准 + 坏账 4 命题均有**强断言**覆盖。唯一已知弱覆盖点 = `TestErpFinAutoReconciliation#testConfigGatedDisabled:120`（javadoc:121-123 自述因类级 `@NopTestConfig` 无法测禁用路径，覆盖缺口已 in-code 声明）。坏账核销凭证测试断言科目/方向/金额/无费用科目，**未断言 `businessType==BAD_DEBT_WRITE_OFF` 枚举值**（次要，凭证内容已实质覆盖）。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，**不重新核实行为本身**；本切片只补"需求契约↔实际行为"差异。

### 4.1 复用 MA2 已证实行为（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`，A2.5c）

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| 辅助账项 5 态全部可达（OPEN/PARTIAL/SETTLED/WRITTEN_OFF/CANCELLED），无死状态 | MA2 §1.2 + §2.1 矩阵 | ✅ 复用（UC-FIN-08 验收②状态派生 OPEN→PARTIAL→SETTLED 证实；WRITTEN_OFF 经 executeWriteOff 可达） |
| CANCELLED 经 `ErpFinArApItemGenerator.cancelOnReverse` 可达（非死状态，证伪候选 P0） | MA2 §1（裁决表）+ §2.1 | ✅ 复用（状态轴 5 态完整性证实） |
| 核销单 3 态 DRAFT→POSTED→REVERSED 守卫齐全（docStatus==DRAFT / POSTED 守卫） | MA2 §2.2 | ✅ 复用（UC-FIN-08 create→post→reverse 链路证实） |
| 坏账 reverseApprove 红冲闭环对称回退强一致（红冲凭证失败→tx 回滚→无半状态） | MA2 §1（裁决表）+ §2.3 场景 f | ✅ 复用（命题 W3 证实） |
| 域侧 ReceiptSettler/PaymentSettler 与 finance ErpFinReconciliation **双路径并行**（by design，非分歧） | MA2 §1（裁决表）+ 控制点 7 | ✅ 复用（UC-FIN-08 作用于 finance 辅助账，独立于域侧运营核销） |
| ErpFinArApItem `versionProp="version"` 乐观锁 → 并发核销 detectable conflict（无 silent lost-update） | MA2 维度 12 + §6 | ✅ 复用（UC-FIN-08 并发核销安全性证实） |
| 期末结账 preCheck `findUnsettledArApCodes` 排除 SETTLED/CANCELLED/WRITTEN_OFF | MA2 §1.4 + 控制点 7 | ✅ 复用（命题 W4 WRITTEN_OFF 一致排除证实） |
| 坏账 REJECTED 后重试经新建记录（非重提） | MA2 §2.3 + P2-MA2-040 | ✅ 复用（坏账状态机完整性证实） |

### 4.2 本切片需求视角差异增量（MA2 未覆盖）

| 差异点 | MA2 视角（状态机/链路行为） | RC 视角（需求契约逐字对照） | 本切片裁决 |
|---|---|---|---|
| 核销 GL 凭证生成 | MA2 未审视（核销不动 GL） | UC-FIN-08 验收标准**未要求**核销产 GL 凭证（仅要求核销明细 + 状态派生 + 余额更新）；L2 `ar-ap-reconciliation.md §核销流程 步骤5` 明示"核销不直接生成凭证" | **接受**（注意点①，设计一致） |
| 应收余额恒等式（Σ发票 − Σ核销 − Σ红字） | MA2 未审视（恒等式属需求契约） | UC-FIN-08 验收③逐字要求恒等式；实现经 PartnerBalanceUpdater.refresh 重算 partner 缓存 = Σ openAmountFunctional（数学等价） | **接受**（注意点②，恒等式成立） |
| WRITTEN_OFF 一致排除（4 处） | MA2 仅证实期末门禁排除（控制点 7） | RC 逐处核实：开放项查询（正向包含 OPEN/PARTIAL）/ 期末门禁 / 坏账准备基线 / 往来余额刷新 4 处一致排除 | **接受**（注意点③，一致排除） |

### 4.3 E2E 行为证据（复用）

- `tests/e2e/business-actions/fin-reconciliation.action.spec.ts`（核销 create/post/reverse + 双边查询 + 5 负向守卫，per plan baseline，断言强度引用 A5.6 评级）。
- `tests/e2e/business-actions/fin-auto-recon.action.spec.ts` / `fin-bad-debt.action.spec.ts` / `fin-bad-debt-reverse-approve.action.spec.ts` / `fin-bad-debt-provision-reverse.action.spec.ts`（自动核销三策略 + 坏账核销/收回/反审核/准备反转 E2E）。
- `tests/e2e/reports/fin-ar-ap-aging.{smoke,value}.spec.ts`（账龄报表）。
- 本切片无新 E2E 探针需求（存疑点见 §7）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵（UC-FIN-08 一行 + 坏账状态轴衍生命题核查，不合并）

| UC / 命题 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-FIN-08** 收款核销发票 | `use-cases.md:147` ①生成核销明细(收款单行↔发票行,金额) ②累计核销<发票→部分 / ==→已核销 ③应收余额=Σ发票−Σ核销−Σ红字 | `ar-ap-reconciliation.md §核销流程/§状态/§余额计算`（设计参考，与 L1 一致；§核销流程 步骤5 明示核销不产 GL 凭证） | ①`ErpFinReconciliationBizModel.create:68-75`→`ErpFinReconciliationLine`(paymentItemId↔invoiceItemId+settledAmount)；②`ReconciliationSettler.settle:32`+`applySettlement:107`+`resolveStatus:128-138`(OPEN/PARTIAL/SETTLED)；③`PartnerBalanceUpdater.refresh:33-44`+`sumOpen:46-62`(重算 partner.receivableBalance=Σ openAmountFunctional)；过账编排 `ErpFinReconciliationPostProcessor.post:21-50` | `TestErpFinReconciliation#testPartialSettlement:49`(强:PARTIAL) / `#testFullSettlement:74`(强:SETTLED) / `TestErpFinAutoReconciliation#testFifoMultipleInvoicesPaidBySingleReceipt:48`(强) / `TestErpFinPartnerBalance#testReceivableBalanceViaReconciliation:70`(强:余额恒等) / `#testCrossPartnerRejected:89`+`#testOverAmountRejected:104`+`#testDateBeforeInvoiceRejected:118`(强:负向守卫) | MA2 §1.2/§2.1/§2.2 证实（5 态可达 + 核销单 3 态守卫 + 双路径并行 + versionProp 乐观锁） | **接受**（3 验收标准 L3-L5 全证据一致；恒等式经 Σ openAmount 数学等价；核销不产 GL 凭证契约未要求） |
| **命题 W1** 坏账核销 | 状态轴 `use-cases.md:11` WRITTEN_OFF + `ar-ap-reconciliation.md §状态扩展`（坏账核销经 ErpFinBadDebtBizModel.writeOff 置 status=WRITTEN_OFF + openAmount=0） | `bad-debt.md §步骤3`（设计参考，与 L1 一致） | `ErpFinBadDebtBizModel.writeOff:55`→`ErpFinBadDebtProcessor.executeWriteOff:163-182`(status=WRITTEN_OFF:170 / open-=amount:168-169 / BAD_DEBT_WRITE_OFF 凭证 借Allowance/贷AR:173-181) | `TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140`(强:status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证 借Allowance/贷AR 无6701) | MA2 §2.1（OPEN→WRITTEN_OFF 迁移 PASS）+ §2.3 场景 d | **接受** |
| **命题 W2** 坏账收回 | 状态轴 WRITTEN_OFF↔OPEN + `ar-ap-reconciliation.md §状态扩展`（recover 恢复） | `bad-debt.md §步骤4a`（设计参考，与 L1 一致） | `ErpFinBadDebtProcessor.executeRecovery:188-206`(WRITTEN_OFF→OPEN:194 / open+=amount:192-193 / BAD_DEBT_RECOVERY 凭证 借AR/贷Allowance:197-205) | `TestErpFinBadDebt#testRecoveryRestoresArApItem:179`(强) | MA2 §2.1（WRITTEN_OFF→OPEN 经 executeRecovery/reverseApprove PASS）+ §2.3 场景 e | **接受** |
| **命题 W3** 坏账反审核红冲闭环 | `bad-debt.md §步骤4b`（reverseApprove 红冲凭证 + ArApItem 对称回退 + APPROVED→REJECTED） | `bad-debt.md §步骤4b`（设计参考，与 L1 一致） | `ErpFinBadDebtProcessor.executeReverseApprove:111-144`(step1 红冲凭证:116 → step2 ArApItem 对称回退:118-138 → step3 APPROVED→REJECTED:141) | `TestErpFinBadDebtReversal`(强:writeOff/recovery 双向红冲闭环) + `TestErpFinBadDebtProvisionReversal#testGuardNoProvisionVoucherRejects:178`+`#testGuardPeriodFinalClosedRejects:197`(强:守卫) | MA2 §2.3（reverseApprove 强一致顺序 PASS）+ 场景 f | **接受** |
| **命题 W4** 坏账准备 + WRITTEN_OFF 一致排除 | `ar-ap-reconciliation.md §账龄分析`（账龄分桶为基础）+ `§状态扩展`（期末门禁/账龄排除 WRITTEN_OFF） | `bad-debt.md §坏账准备计提方法`（设计参考，与 L1 一致） | `BadDebtProvisionCalculator.calculate:37-81`(5 桶×lossRate + 排除 SETTLED/CANCELLED/WRITTEN_OFF:55-58) + `BadDebtProvisionService.findReceivableOpenItems:288-297`(notIn:292-295) + `ErpFinArApItemBizModel.findOpenItemsByPartner:38`+`aging:64`(in[OPEN,PARTIAL]:44-45/72-73) + `ErpFinAccountingPeriodProcessor.findUnsettledArApCodes:442`(排除 WRITTEN_OFF:459) | `TestErpFinBadDebt#testAgingBucketProvisionAndReserve:76`+`#testProvisionExcludesNegativeAndWrittenOff:114`+`#testPeriodCloseAllowanceGateBlocksWhenShortfall:266`(强) | MA2 §1.4 + 控制点 7（期末门禁排除 WRITTEN_OFF 证实） | **接受**（4 处一致排除，含正向包含与显式排除两种正确实现） |

### 5.2 三项已知注意点分级裁决（§2 判据）

#### 注意点① — 核销本身不写 GL 凭证（`ErpFinReconciliationBizModel.java` javadoc:46 + `ErpFinReconciliationPostProcessor.java:37-42`）

- **裁定**：**接受**（§2 "接受"判据：需求契约的全部验收标准在 L3-L5 各级均有证据且一致）。
- **判据**：UC-FIN-08 三条验收标准（核销明细 / 状态派生 / 余额恒等式）**均未要求核销产 GL 凭证**。L2 owner doc `ar-ap-reconciliation.md §核销流程 步骤5` 逐字明示"核销不直接生成凭证，凭证由收款单/付款单审核时生成"——L1 与 L2 一致。实现：`ErpFinReconciliationPostProcessor.post:37-42` 唯一 GL 影响是**可选**汇兑损益凭证（gated by `isReconFxGainLossEnabled`，默认 false），单币种路径零 GL 影响。核销的会计效果经辅助账（ErpFinArApItem settled/open）+ 往来余额缓存表达，业财回链经收付款过账时的 AR/AP 科目凭证实现（A2.5a 已确认）。
- **三源对照**：L1（未要求）= L2（明示不产）= L3（gated 可选 FX 凭证，默认不产）——一致。

#### 注意点② — 往来余额是 ErpMdPartner 缓存字段（`PartnerBalanceUpdater.java` javadoc:22-23 + refresh:33-44 + sumOpen:46-62）

- **裁定**：**接受**（§2 "接受"判据）+ **1 项静态存疑点**（见 §7）。
- **判据**：UC-FIN-08 验收③"应收余额 = Σ发票 − Σ核销 − Σ红字"经 `PartnerBalanceUpdater.refresh:33-44` 重算 `ErpMdPartner.receivableBalance/payableBalance` 实现。机制：`sumOpen:46-62` Σ 对应方向辅助账 `openAmountFunctional`。**数学等价证明**：`openAmountFunctional = amountFunctional − settledAmountFunctional`（`ReconciliationSettler.applySettlement:118-119`），红字发票 `amountFunctional` 为负（过账时金额取负），故 `Σ openAmount = Σ(amount − settled) = Σ发票 − Σ核销 − Σ红字`（红字 amount 为负，等价 "+Σ红字绝对值"）。`TestErpFinPartnerBalance#testReceivableBalanceViaReconciliation:70` 强断言全核销后 receivableBalance==0 + 经 post/reverse 余额变化，证实恒等式。
- **三源对照**：L1（恒等式）= L2（`§余额计算` Σ openAmount）= L3（refresh 重算）——一致。
- **静态存疑点**（交 MA4 A4.1）：`sumOpen:51-52` 仅显式排除 SETTLED/CANCELLED，**未显式排除 WRITTEN_OFF**。WRITTEN_OFF 项进入查询后贡献 0（因 `executeWriteOff:168-169` 置 openAmount=0），故**当前余额正确**，但隐式排除依赖"executeWriteOff 永远将 openAmount 置零"不变量。边界场景"部分核销后坏账"（PARTIAL→WRITTEN_OFF，此时 openAmount 已非 0，executeWriteOff 以 debt.amount ≤ openAmount 置零）经静态推理成立（`validateAmount:285-294` 守卫 amount ≤ openAmount，executeWriteOff:168 open-=amount 使其归零），但运行时未单独覆盖该边界——交 MA4 A4.1 按需追加 A4.1.n 实体行验证。

#### 注意点③ — WRITTEN_OFF 一致排除（4 处）

- **裁定**：**接受**（§2 "接受"判据）。
- **判据**：WRITTEN_OFF 在以下 4 处一致排除（§2.3 实测）：
  1. 开放项查询 `findOpenItemsByPartner:38`/`aging:64` — 正向包含 `in(status, [OPEN, PARTIAL])`（:44-45/:72-73）→ 隐式排除 WRITTEN_OFF/SETTLED/CANCELLED。
  2. 期末结账门禁 `findUnsettledArApCodes:442` — 显式排除 WRITTEN_OFF（:459）。
  3. 坏账准备基线 `BadDebtProvisionService.findReceivableOpenItems:288-297` — 显式 notIn（:292-295）+ `BadDebtProvisionCalculator.calculate:55-58` 显式排除。
  4. 往来余额刷新 `PartnerBalanceUpdater.sumOpen:51-52` — 隐式排除（openAmount=0，见注意点②静态存疑点）。
- **三源对照**：L1（状态轴含 WRITTEN_OFF，已核销项不应再参与核销/账龄/准备）= L2（`§状态扩展` 期末门禁排除）= L3（4 处一致）——一致。
- **裁决**：4 处排除机制虽分两种（正向包含 vs 显式 notIn），但**语义一致**——WRITTEN_OFF 作为"已核销"等价态在所有"未核销/开放"聚合中均不参与。无分歧。

### 5.3 P0/P1/P2 新发现裁决

- **P0**：无。UC-FIN-08 三验收标准 + 坏账 4 命题全 L3-L5 一致，无活跃数据破坏 / 会计过账正确性破坏路径。
- **P1**：无。无功能缺失 / 异常路径未实现 / 状态迁移不可达 / 测试断言完全缺失。
- **P2**：无。无次要验收标准未满足 / 文档化简化无显式批准记录（本切片无 documented simplification 标注）。
- **三项注意点**：全裁定"接受"（设计一致），非 finding。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` finance AR-AP 核销/坏账同域同控制点。本切片**产出 0 项新 finding**（UC-FIN-08 + 坏账命题全接受）。裁决遵循 §7 规则。

### 6.1 grep 比对结果（MA2 A2.5c 既有 P2 finding 复核）

| 既有 finding | 控制点 | 与本切片关系 | 裁决 |
|---|---|---|---|
| P2-MA2-036 `ar-ap-reconciliation.md §核销状态` 命名漂移（UNRECONCILLED/.../OVER vs dict 5 态） | owner doc 命名治理 | 不同维度（owner-doc drift vs 需求契约）。本切片 L1 用 dict 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF），实现一致；owner doc 命名漂移不影响 L1 合规 | **不重开**（audit-remediation MR1 处置，本 RC 仅引用） |
| P2-MA2-037 `state-machine.md` 缺 AR/AP 独立章节 | owner doc 章节治理 | 不同维度（文档结构 vs 需求契约） | **不重开** |
| P2-MA2-038 域侧/finance 双路径无对账守卫 | 双路径余额一致性 | **观察**：本切片实测 `DualSideConsistencyChecker.check:52-102` 已存在（finSettled vs domSettled 差异报告，@BizQuery 只读 reporter）。MA2 登记时（2026-07-27）该 checker 是否已存在属 audit-remediation 范畴；本 RC 不重开 P2-MA2-038，仅在 §9 记录"checker 已存在为只读 reporter（非 auto-repair guard）"的差异增量 | **不重开**（边界属 audit-remediation；本 RC UC-FIN-08 余额恒等式经 finance 辅助账驱动成立，不依赖双路径守卫） |
| P2-MA2-039 `assertOpen` 不拒绝 WRITTEN_OFF | 坏账状态机隔离 config-gated | 不同维度（状态机隔离 vs 需求契约）。本切片命题 W1-W4 经 MA2 §2.1 证实 WRITTEN_OFF 经 executeWriteOff 可达 + 一致排除；P2-MA2-039 的 config-gated 隔离缺口（allow-over-reconcile=true 时可覆写）属状态机治理 | **不重开** |
| P2-MA2-040 坏账 REJECTED 无 resubmit | 坏账审批状态机重试路径 | 不同维度（状态机重试 vs 需求契约） | **不重开** |
| P2-MA2-041 核销 post/reverse 无期间 CLOSED_FINAL 守卫 | 期间锁定扩展 | 不同维度（期间锁定 vs 需求契约）。UC-FIN-08 验收标准未要求期间守卫；核销不动 GL | **不重开** |
| P1-MA2-009 多币种核销 FX plug 未实现 | 多币种辅助账本位币回写 | MA2 §维度 11 维持 P1。本切片 UC-FIN-08 验收标准为单币种语义（未要求多币种 FX plug）；多币种核销汇兑损益经 `ReconciliationSettler.settleWithFx:58-80` + `ErpFinReconciliationPostProcessor:37-39`（gated 可选）实现，单币种路径（tested baseline）正确 | **不重开**（不同控制点：需求契约单币种 vs 多币种 FX plug） |

### 6.2 新建 finding 裁决

- **无新 finding**。UC-FIN-08 + 坏账 4 命题全接受，三项注意点全裁定"接受"。本切片不向 arm-index 新增 `P*-RC-xxx` 行。

### 6.3 双向可追溯

- **新 finding → arm-index**：N/A（无新 finding）。
- **既有 finding 复用注记**：本切片 MA2 A2.5c 6 项 P2 watch-only（P2-MA2-036/037/038/039/040/041）+ P1-MA2-009 经运行时复核**无升级**（UC-FIN-08 需求契约视角确认实现符合，状态机/owner-doc/drift 维度维持原级）。
- **静态存疑点 → MA4**：注意点②边界（WRITTEN_OFF 隐式排除依赖 openAmount=0）登记入 §7，交 MA4 A4.1（运行时展开器，Deps=MA1 done）按需追加 A4.1.n 实体行。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明。

1. **注意点②边界：`PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 的隐式排除**（`PartnerBalanceUpdater.java:51-62` + `ErpFinBadDebtProcessor.executeWriteOff:163-182`）：sumOpen 仅显式排除 SETTLED/CANCELLED，WRITTEN_OFF 项进入查询后贡献 0（依赖 executeWriteOff:168-169 置 openAmount=0）。静态推理："部分核销后坏账"（PARTIAL→WRITTEN_OFF）经 `validateAmount:285-294`（amount ≤ openAmount）+ executeWriteOff:168（open-=amount）保证归零，成立；但该边界（非全额核销后坏账）运行时未单独覆盖。交 MA4 A4.1 按需追加 A4.1.n 实体行验证"PARTIAL 辅助账项执行 writeOff 后 partner.receivableBalance 是否正确反映剩余 openAmount=0"。

2. **`TestErpFinBadDebt.testWriteOffSetsStatusAndVoucherNoPL:140` 凭证 businessType 断言强度**（`TestErpFinBadDebt.java:140-174`）：测试断言凭证科目/方向/金额/无 6701 费用科目，**未断言 `businessType==BAD_DEBT_WRITE_OFF` 枚举值**。属次要断言强度缺口（凭证内容已实质覆盖核销语义），非合规缺陷。交 MA4 A4.1 按需评估是否补强 businessType 枚举断言。

3. **`TestErpFinAutoReconciliation.testConfigGatedDisabled:120` 禁用路径覆盖缺口**（`TestErpFinAutoReconciliation.java:120-123`）：javadoc 自述因类级 `@NopTestConfig` 无法测试 auto-reconcile 禁用路径。属已知覆盖缺口（in-code 声明），非合规缺陷。交 MA4 A4.1 按需评估。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0/P1/P2**（全接受），按 §10 **不触发 MR0/MR1**。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-02 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)`）。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2 已证实行为**（不重新核实）：
  - `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（A2.5c）：AR/AP 核销三组件状态机（辅助账项 5 态 + 核销单 3 态 + 坏账审批状态机）+ 9 控制点 + 6 项 P2 watch-only + 5 并发敏感点。本切片 UC-FIN-08 + 坏账 W1-W4 命题的 L5 行为证据直接引用该报告 §1.2（5 态可达）/ §2.1（辅助账迁移矩阵）/ §2.2（核销单 3 态）/ §2.3（坏账审批 + reverseApprove 强一致）/ 控制点 7（双路径并行 + 期末门禁排除）/ 维度 12（versionProp 乐观锁）。
  - `2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b）：核销/坏账/汇兑/成本/期间代码质量，本切片未重复审查代码质量维度。
  - E2E specs（`fin-reconciliation`/`fin-auto-recon`/`fin-bad-debt`/`fin-bad-debt-reverse-approve`/`fin-bad-debt-provision-reverse`/`fin-ar-ap-aging`）：跨域核销/坏账链 E2E 行为，断言强度引用 A5.6 评级。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-FIN-08 三条验收标准逐字对照**（MA2 状态机视角未逐条对照需求契约）：本切片首次从 L1 逐字核验①核销明细生成 ②累计核销→状态派生 ③应收余额恒等式，三条全 L3-L5 一致 → 接受。
  2. **核销 GL 凭证生成判定**（注意点①）：MA2 未审视（核销不动 GL）；本切片从 UC-FIN-08 契约判定"契约未要求核销产 GL 凭证"，L1/L2/L3 一致 → 接受。
  3. **应收余额恒等式数学等价证明**（注意点②）：MA2 未审视（恒等式属需求契约）；本切片证明 `Σ openAmount ≡ Σ发票 − Σ核销 − Σ红字`（红字 amount 为负）→ 接受 + 1 静态存疑点（WRITTEN_OFF 隐式排除依赖 openAmount=0 不变量）。
  4. **WRITTEN_OFF 4 处一致排除核实**（注意点③）：MA2 仅证实期末门禁排除；本切片逐处核实开放项查询/期末门禁/坏账准备/往来余额刷新 4 处一致排除（正向包含 + 显式 notIn 两种正确实现）→ 接受。
  5. **DualSideConsistencyChecker 已存在观察**：P2-MA2-038 登记时（2026-07-27）述"双路径无对账守卫"；本切片实测 `DualSideConsistencyChecker.check:52-102` 已存在（finSettled vs domSettled 差异报告，@BizQuery 只读 reporter）。该 checker 为**只读 reporter 非 auto-repair guard**，是否完整关闭 P2-MA2-038 属 audit-remediation 范畴，本 RC 不重开。
- **MA2 finding 复核无升级**：本切片复核 MA2 A2.5c 6 项 P2 watch-only（P2-MA2-036/037/038/039/040/041）+ P1-MA2-009（多币种 FX plug），运行时行为与 MA2 登记一致，**无升级 P0/P1**（对齐 MA2 §9 结论）。P1-MA2-009 维持 P1（单币种路径正确，多币种 FX plug 为未实现功能缺口，非本切片 UC-FIN-08 单币种契约分歧）。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（UC-FIN-08 接受 + 坏账状态轴 4 命题接受，零 P0/P1/P2 新 finding）

**审查范围**：UC-FIN-08 共 1 UC 五级追踪矩阵（L1-L5）+ 坏账 WRITTEN_OFF 状态轴 4 项衍生命题（W1 核销 / W2 收回 / W3 反审核红冲闭环 / W4 准备 + 一致排除）+ 每 UC/命题符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-FIN-08 三验收标准（核销明细 / 状态派生 / 余额恒等式）全 L3-L5 一致；坏账 W1/W2/W3/W4 全接受；三项注意点（核销不产 GL 凭证 / 余额缓存字段 / WRITTEN_OFF 一致排除）全裁定"接受"。

**P0/P1/P2**：无。不触发 MR0/MR1。

**剩余风险**：见 §7 静态存疑点清单（3 项交 MA4 A4.1 运行时展开，其中 1 项为注意点②边界 + 2 项为测试断言强度次要缺口，均非合规缺陷）。
