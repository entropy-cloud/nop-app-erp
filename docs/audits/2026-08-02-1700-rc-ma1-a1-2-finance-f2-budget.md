# RC MA1 A1.2 — finance-F2 预算与承付 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.2（MA1 需求追踪矩阵审计 — finance-F2 预算与承付）
> 审计 plan：`docs/plans/2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-11/13，2 UC）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.2
> Related（非 Deps）：`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1 同批先行——过账引擎是 BUDGET/COMMITMENT 凭证生成的基础；其结论影响本切片对"凭证是否生成"的判读）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **1** | P1-RC-003（UC-FIN-13 断言④ 三列对比报表未满足——`getBudgetVsActual` 仅出 Budget/Actual 两列 + 把 COMMITMENT 计入 actual；`BudgetVsActualRow` DTO 无 `commitmentAmount` 字段）→ 待 MR1（R1.0 展开为 RC-R1.n） |
| **接受**（符合需求契约） | **1 UC + 1 UC 的 3/4 断言** | UC-FIN-11 全验收标准接受；UC-FIN-13 断言①②③接受（BUDGET 影子凭证 / 三通道控制 / 承付 commit+release），断言④ P1 |
| MA2 既有行为证据复用 | 5 项 finding（P1-MA2-081/082/083/084 + P2-MA2-073）+ A2.5b/A2.16 行为背书 | 无升级（详见 §4 / §9） |

**整体裁决**：A1.2 切片 2 UC 五级追踪矩阵填齐。预算控制主体（UC-FIN-11 三通道余量公式 BUDGET−COMMITMENT−NORMAL + HARD 拦截/WARN 放行+日志/NONE 放行）经 L3-L5 四级证据确认符合验收标准（控制引擎的三通道分离经 `testAvailableDeductsCommitmentSeparately` 强断言 available=1000−300−200=500 证实）。UC-FIN-13 的预算编制（BUDGET 影子凭证）/ 预算控制（复用 UC-FIN-11）/ 承付款（commit + release-on-cancel + release-on-invoice-approve + release-on-return，采购-销售对称 + 跨域钩子齐全）三项验收标准均接受。**一项 P1 需求分歧**：UC-FIN-13 断言④「预算对比报表按 postingType 分组得 Budget/Commitment/Actual **三列**」**未满足**——实现 `ErpFinBudgetLineBizModel.getBudgetVsActual:48-108` 仅产出 budget/actual/available **两列**（DTO `BudgetVsActualRow` 无 `commitmentAmount` 字段），且 voucher 过滤为 `postingType=BUDGET OR NOT BUDGET`（`:64-65`）把 COMMITMENT 凭证**计入 actual**——与控制引擎 `ErpFinBudgetControlBiz` 已修正的三通道分离（P1-MA2-084 已 fix）**口径不一致**。按 §2 判据定为 P1（验收标准④未满足 + 功能实质偏离），按 §10 经 MR1 批量修复通道修复；**无 P0**——报表口径错误属报表正确性分歧（非活跃数据破坏，且控制引擎的余量计算已正确三通道分离，预算硬拦截本身不受影响）。**plan baseline 三个 caveat 判定**：①config 默认关闭 = 接受（控制机制完整正确，L1 描述控制语义未强制默认开启；记入 §7 静态存疑点供 MA4 确认部署契约）；②actual 口径不一致 + 两列 = **P1-RC-003**；③承付单行凭证结构 = 接受（L1 未规定 Dr/Cr 结构；L2 drift `budget.md:255` 两行 vs 实现单行属 MA3 doc↔code 维度非 RC；凭证头借贷不平记入 §7 静态存疑点供 MA4）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐 UC 逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-11 预算硬拦截（`use-cases.md:204`）

**场景**:采购订单审核时超预算,被硬拦截。

**可验证断言**(见 budget.md §业务规则):
```
采购订单.审核 →
  调用 IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = 预算(BUDGET凭证) - 承付(COMMITMENT凭证) - 实际(NORMAL凭证)
  若 余量 < 0 且 控制级别 == HARD:
    返回 BLOCKED → 审核抛异常, 订单保持 SUBMITTED
  若 == WARN: 写日志放行
  若 == NONE: 放行
```

**涉及机制**:budget.md §业务规则/§PostingType

### UC-FIN-13 预算管理(编制/控制/对比)（`use-cases.md:238`）

**场景**:编制年度预算,采购/付款时按预算控制,期末对比预算 vs 实际。见 budget.md。

**可验证断言**:
```
// 预算方案审批即过账(BUDGET 影子凭证)
预算方案.审核通过 →
  生成凭证(postingType=BUDGET, businessType=BUDGET_SCENARIO)
  借贷按 subject.direction 自动取(资产/费用借,负债/收入贷)

// 预算控制(采购订单审核时,强一致校验)
采购订单.审核 →
  IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = BUDGET凭证 - COMMITMENT凭证 - NORMAL凭证(同维度)
  若 余量 < 0 且 控制级别==HARD: 返回 BLOCKED → 审核抛异常
  若 == WARN: 写 BudgetControlLog 放行

// 承付款
采购订单.APPROVED → 生成 COMMITMENT 凭证
订单 CANCELLED 或发票接收 → 红冲 COMMITMENT

// 预算对比(报表)
按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine
得到 Budget/Commitment/Actual 三列, 无需独立预算余额表
```

**涉及机制**:budget.md §PostingType/§业务规则、cost-center.md

> **L1 锚点核对**：A1.2 切片 UC 清单 = UC-FIN-11/13（`rc-requirement-baseline-inventory.md §切片索引 A1.2`），2 UC 逐 UC 一矩阵行（§3 完整枚举纪律，不合并）。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-finance/erp-fin-service/.../` + `module-purchase/erp-pur-service/.../`）。L3 引用格式遵循 §1 L3 规范（含行号）。

### 2.1 预算控制引擎（UC-FIN-11 / UC-FIN-13 断言②共用）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 控制入口 `IErpFinBudgetControlBiz.check` | `module-finance/erp-fin-service/.../service/budget/ErpFinBudgetControlBiz.java` check:62-104 | ✅ |
| 三通道余量公式 `available = budgetBalance − actualBalance − commitmentBalance` | `ErpFinBudgetControlBiz.java` :78-81（budgetBalance=BUDGET 通道 :78 / actualBalance=非 BUDGET 非 COMMITMENT 通道 :79 / commitmentBalance=COMMITMENT 通道 :80 / available 三项式 :81） | ✅ 显式三通道分离（P1-MA2-084 已 fix） |
| 通道过滤 `applyPostingTypeFilter` | `ErpFinBudgetControlBiz.java` :151-167（BUDGET `eq` :154 / COMMITMENT `eq` :157 / ACTUAL=`isNull OR notIn(BUDGET,COMMITMENT)` :162-164） | ✅ actual 通道排除 COMMITMENT |
| HARD 拦截（抛异常） | `ErpFinBudgetControlBiz.java` :88-95（`ERR_BUDGET_EXCEEDED` NopException + writeControlLog BLOCKED） | ✅ |
| WARN 放行 + 日志 | `ErpFinBudgetControlBiz.java` :96-102（writeControlLog WARNED + LOG.warn + return WARNED） | ✅ |
| NONE 放行 | `ErpFinBudgetControlBiz.java` :103（return PASS） | ✅ |
| 控制日志实体写入 | `ErpFinBudgetControlBiz.java` writeControlLog:199-223（ErpFinBudgetControlLog：scenarioId/budgetLineId/sourceBill/subject/costCenter/period/requested/committed/available/actionResult/operator） | ✅ |
| 命中预算行查找 | `ErpFinBudgetControlBiz.java` findMatchingBudgetLine:180-197（subjectId+periodId+costCenterId + scenario.docStatus=APPROVED） | ✅ |
| 控制总开关（config-gated） | `ErpFinBudgetControlBiz.java` isBudgetCheckEnabled:225-228（`CONFIG_BUDGET_CHECK_ENABLED` 默认 `Boolean.FALSE`） | ⚠️ 默认关闭（见 §5 caveat ① + §7） |

### 2.2 BUDGET 影子凭证生成/红冲（UC-FIN-13 断言①）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 审批→生成 BUDGET 凭证 | `module-finance/erp-fin-service/.../service/budget/BudgetVoucherGenerator.java` generate:53-73（按 periodId 分组每组一凭证 :58-64） | ✅ |
| 凭证写库（postingType=BUDGET） | `BudgetVoucherGenerator.java` writeBudgetVoucher:97-180（`setPostingType(POSTING_TYPE_BUDGET)` :126 / 借贷按 subject.direction resolveDcDirection :207-210 / DrCr 行 :151-153 / 业财回链 billType=BUDGET_VOUCHER_BILL_TYPE billCode=scenario.code :172-177） | ✅ |
| 作废→红冲 BUDGET 凭证 | `BudgetVoucherGenerator.java` reverse:79-95（按 billCode=scenario.code 反查 :80 / 逐张红字冲销 :82-93 / 原凭证 isReversed=true :89） | ✅ |
| 红字行金额取负 | `BudgetVoucherGenerator.java` toFact（VoucherLine 分支）:193-202（`amount.negate()` :200） | ✅ |
| Processor 接线（审批/作废 mutation） | `ErpFinBudgetScenarioProcessor` + 各 mutation Processor（Approve/Cancel/CarryForward/RollForward/SubmitForApproval/Reject，per plan baseline） | ✅ |

### 2.3 承付款 commit/release（UC-FIN-13 断言③，finance 侧）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 承付门面 SPI `IErpFinBudgetCommitmentBiz` | `module-finance/erp-fin-service/.../service/budget/ErpFinBudgetCommitmentBizModel.java` commit:55-78 / release:81-99 / releaseIfPresent:104-115 / isCommitmentEnabled:117-119 | ✅ |
| commit→生成 COMMITMENT 凭证 | `module-finance/erp-fin-service/.../service/budget/CommitmentVoucherGenerator.java` generateCommitment:61-68 | ✅ |
| release→红冲 COMMITMENT 凭证 | `CommitmentVoucherGenerator.java` reverseCommitment:77-94（按 billType+billCode 反查 :79 / 逐张红冲 :81-93 / 原凭证 isReversed=true :88） | ✅ |
| 重复 release 守卫 | `ErpFinBudgetCommitmentBizModel.java` release:88-92（`hasUnreversedCommitment=false` → `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`）+ `CommitmentVoucherGenerator.hasUnreversedCommitment`:97-105 | ✅ |
| billType 派发（PO/SO 隔离） | `CommitmentVoucherGenerator.java` resolveCommitmentBillType:112-117（PURCHASE_ORDER→PURCHASE_ORDER_COMMITMENT / SALES_ORDER→SALES_ORDER_COMMITMENT / 未知回退采购） | ✅ |
| 承付凭证写库（postingType=COMMITMENT） | `CommitmentVoucherGenerator.java` writeCommitmentVoucher:119-182（`setPostingType(POSTING_TYPE_COMMITMENT)` :135 / 业财回链 billType+billCode=订单 code :174-179） | ⚠️ **单行单边凭证**（见 §5 caveat ③ + §7） |
| 承付总开关（config-gated） | `ErpFinBudgetCommitmentBizModel.java` isCommitmentEnabled:117-119（`CONFIG_BUDGET_COMMITMENT_ENABLED` 默认 `Boolean.FALSE`） | ⚠️ 默认关闭（见 §5 caveat ① + §7） |

### 2.4 跨域承付钩子（UC-FIN-13 断言③，purchase 侧——本切片 finance 契约视角引用，逐行核验）

| 钩子 | 文件:行 | 审计状态 |
|---|---|---|
| #1 commit（PO approve 后置） | `module-purchase/erp-pur-service/.../service/processor/ErpPurOrderProcessor.java` runCommitmentCommitHook:197-211（config-gate :198 / `budgetCommitmentBiz.commit(PURCHASE_ORDER, code, …)` :208-210） | ✅ |
| #2 release-on-cancel（PO reverseApprove/cancel） | `ErpPurOrderProcessor.java` runCommitmentReleaseHook:222-233（try-catch `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 容错 :226-232）+ `ErpPurOrderReverseApproveProcessor.java` beforeStateChange:42（`processor.runCommitmentReleaseHook`） | ✅ |
| #3 release-on-invoice-approve（AP 发票过账释放） | `module-purchase/erp-pur-service/.../service/processor/ErpPurInvoiceProcessor.java` runCommitmentReleaseOnInvoiceApproveHook:273-292（invoiceLine→receiveLine→receive→order.code 反查 :301 / 对每个 order.code release + 容错 :281-291） | ✅ |
| #4 release-on-return（config-gated） | `module-purchase/erp-pur-service/.../service/processor/ErpPurReturnProcessor.java` runCommitmentReleaseOnReturnHook:281-297（`CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN` 默认 false :282-284 / `releaseIfPresent` 容错 :290-296） | ✅ |
| 预算 check 钩子（PO approve） | `ErpPurOrderProcessor.java` runBudgetCheckHook:177-189（config-gate :178 / `budgetControlBiz.check(PURCHASE_ORDER, code, amount)` :188） | ✅ |
| **reject release-on-receive**（入库路径） | `ErpPurReceiveProcessor` 427 行零 commitment SPI 接入（per MA2 A2.16 §证实） | ✅ 入库不释放（正确） |

### 2.5 预算对比报表（UC-FIN-13 断言④）— **三列要求未满足**

| 期望组件 | 实仓核实结果 | 证据 |
|---|---|---|
| 报表查询 `getBudgetVsActual` | **存在但仅两列** | `module-finance/erp-fin-service/.../service/entity/ErpFinBudgetLineBizModel.java` getBudgetVsActual:48-108 |
| voucher 过滤 | **`BUDGET OR NOT BUDGET`**（COMMITMENT 计入 actual） | `ErpFinBudgetLineBizModel.java` :64-65（`or(eq("postingType",BUDGET), or(isNull, ne(BUDGET)))`） |
| 行聚合分通道 | **仅 budget/actual 两通道**（`isBudget` flag :72-73 / budgetAmount 累加 :98-99 / else actualAmount 累加 :100-102） | `ErpFinBudgetLineBizModel.java` :70-102 |
| Commitment 列 | **不存在** | DTO `BudgetVsActualRow` 仅有 `budgetAmount`/`actualAmount`/`availableAmount` 三字段（`module-finance/erp-fin-dao/.../dto/BudgetVsActualRow.java:24-26`），**无 `commitmentAmount` 字段** |
| available 计算 | `budgetAmount − actualAmount`（**未减 commitment**） | `ErpFinBudgetLineBizModel.java` :104-106 |

**结论**：UC-FIN-13 断言④「按 postingType 分组得 Budget/Commitment/Actual **三列**」在 L3 **未满足**——实现仅出 Budget/Actual 两列，且 actual 口径把 COMMITMENT 计入（与控制引擎 `ErpFinBudgetControlBiz` 已修正的三通道分离口径不一致）。报表数据源本身正确（从 VoucherLine 聚合，无独立预算余额表，符合"无需独立预算余额表"），但**三列拆分缺失**。

---

## 3. 测试证据（L4，注明断言强度）

> 断言强度分档：强断言 = 断言验收标准数值/状态/凭证字段；弱断言 = 仅断言不抛异常或仅冒烟。

| UC | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| UC-FIN-11（HARD 拦截） | `TestErpFinBudgetEndToEnd.java#testHardControlBlocked:95-118` | **强** | ✅ 余量=1000−800=200<300 → `assertThrows(NopException)` + ERR_BUDGET_EXCEEDED |
| UC-FIN-11（WARN 放行+日志） | `TestErpFinBudgetEndToEnd.java#testWarnControlLogsAndPasses:121-144` | **强** | ✅ result=WARNED + available=200 + `countControlLogs>=1` |
| UC-FIN-11（NONE 放行） | `TestErpFinBudgetEndToEnd.java#testNoneControlPasses:147-166` | **强** | ✅ result=PASS（amount=9999 远超预算仍放行） |
| UC-FIN-11（三通道余量公式） | `TestErpFinBudgetEndToEnd.java#testAvailableDeductsCommitmentSeparately:222-249` | **强** | ✅ BUDGET(1000)+ACTUAL(300)+COMMITMENT(200) → available=**500**（=1000−300−200，三通道分离强断言） |
| UC-FIN-11（三通道回归：commitment=0 + RESERVATION 计入 actual） | `TestErpFinBudgetEndToEnd.java#testCommitmentZeroEquivalentAndReservationCountsAsActual:252-280` | **强** | ✅ commitment=0 时与旧公式等价 + RESERVATION 计入 actual（available=1000−(300+100)−0=600） |
| UC-FIN-13 断言①（审批→BUDGET 凭证） | `TestErpFinBudgetEndToEnd.java#testApproveGeneratesBudgetVoucher:71-92` | **强** | ✅ docStatus=APPROVED + voucherId 回写 + 预算余额从 VoucherLine 聚合=1000 |
| UC-FIN-13 断言①（作废→红冲 BUDGET） | `TestErpFinBudgetEndToEnd.java#testCancelReversesBudgetVoucher:169-192` | **强** | ✅ 红冲后净预算余额归零（原+红 isReversed=true 均不计入） |
| UC-FIN-13 断言③（commit→COMMITMENT 凭证） | `TestErpFinBudgetCommitment.java#testCommitGeneratesCommitmentVoucher:61-88` | **强** | ✅ postingType=COMMITMENT + POSTED + 业财回链 billType=PURCHASE_ORDER_COMMITMENT/billCode |
| UC-FIN-13 断言③（release-on-cancel 红冲） | `TestErpFinBudgetCommitment.java#testReleaseOnCancelReversesCommitment:91-119` | **强** | ✅ 原凭证 isReversed=true + 红冲凭证 isReversed=true + reversalOfVoucherId 指向原凭证 |
| UC-FIN-13 断言③（release-on-invoice-approve） | `TestErpFinBudgetCommitment.java#testReleaseOnInvoiceApproveReversesCommitment:122-146` | **强** | ✅ 与 cancel 共用 SPI.release，行为对称 |
| UC-FIN-13 断言③（重复 release 守卫） | `TestErpFinBudgetCommitment.java#testDoubleReleaseThrowsGuard:149-172` | **强** | ✅ 第二次 release 抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED |
| UC-FIN-13 断言③（SO billType 隔离） | `TestErpFinBudgetCommitment.java#testSalesCommitmentDispatchesSalesBillType:175-216` | **强** | ✅ SALES_ORDER_COMMITMENT 与 PURCHASE_ORDER_COMMITMENT 同 billCode 隔离 |
| UC-FIN-13 断言③（多发票全额释放容错） | `TestErpFinBudgetCommitment.java#testMultiInvoiceFullReleaseProducesSingleReversal:219-257` | **强** | ✅ 仅一张红冲 + 第二张发票 release 抛守卫 |
| UC-FIN-13 断言③（跨域 PO commit） | `TestErpPurOrderCommitment.java`（per baseline） | **强** | ✅ 采购订单 approve 触发 commit 凭证 |
| UC-FIN-13 断言③（跨域 release-on-return） | `TestErpPurReturnCommitmentRelease.java`（per baseline） | **强** | ✅ 退货释放承付 |
| UC-FIN-13 断言③（跨域预算控制集成） | `TestErpPurBudgetControlIntegration.java`（per baseline） | **强** | ✅ PO 审核预算 check 集成 |
| UC-FIN-13 断言④（三列对比报表） | `TestErpFinBudgetEndToEnd.java#testGetBudgetVsActual:195-219` | **强（断言两列行为，非需求三列行为）** | ❌ 仅断言 budgetAmount=1000/actualAmount=400/availableAmount=600（**两列**）；**未 seed COMMITMENT 凭证** → 既未验证 commitment 独立列，也未验证 actual 不含 commitment。L1 要求的"三列"验收标准**零覆盖** |

**测试证据汇总**：UC-FIN-11 三通道+三级控制强断言全覆盖；UC-FIN-13 断言①②③强断言覆盖；UC-FIN-13 断言④"三列对比报表"——测试本身只断言两列行为且不 seed commitment，**三列需求零覆盖**（测试与实现同步偏离 L1）。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，**不重新核实行为本身**；本切片只补"需求契约↔实际行为"差异。

### 4.1 复用 MA2 已证实行为

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| 预算方案状态机（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED）+ 承付 commit/release 独立凭证状态机 + 期间状态机守卫 | `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（A2.5b） | ✅ 复用（UC-FIN-13 断言① BUDGET 凭证生成/红冲 + 断言③ 承付 commit/release 状态机证实） |
| 3 接入点齐全（commit / release-on-cancel / release-on-invoice-approve，采购-销售对称）+ §reject release-on-receive 落实 + 重复释放守卫 + 取消后再发票容错 + 多年度跨期余量一致 + config-gate 默认 false 保护 | `2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16） | ✅ 复用（UC-FIN-13 断言③ 承付释放路径完整性证实） |
| P1-MA2-081 部分开票全额释放语义 | A2.16 | ✅ 复用（owner doc 契约漂移，已登记 P1，非本切片新发现） |
| P1-MA2-082 采购退货未释放（release-on-return） | A2.16 | ✅ 复用（保守方向偏移 + config-gated；本切片核验 release-on-return 钩子 `ErpPurReturnProcessor:281-297` 已落地，config 默认 off） |
| P1-MA2-083 AP/AR 发票冲销后 commitment 未恢复 | A2.16 | ✅ 复用（跨冲销一致性缺口，已登记 P1） |
| **P1-MA2-084 `ErpFinBudgetControlBiz.aggregateAmount` 实际聚合含 COMMITMENT（等价正确但脆弱）** | A2.16 | ✅ **已 fix**（控制引擎现三通道分离，`testAvailableDeductsCommitmentSeparately` 强断言 available=500 证实）。**本切片新发现 = 其姊妹站点 REPORT（`ErpFinBudgetLineBizModel.getBudgetVsActual`）未同步修正**——见 §5 P1-RC-003 |

### 4.2 本切片需求视角差异增量（MA2 未覆盖）

| 差异点 | MA2 视角 | RC 视角（需求契约） | 本切片裁决 |
|---|---|---|---|
| 三列对比报表（Budget/Commitment/Actual） | MA2 A2.16 审查承付释放路径完整性 + 控制引擎三通道，**未审查报表 `getBudgetVsActual` 的列数/口径** | UC-FIN-13 断言④ 明确要求"得 Budget/Commitment/Actual **三列**"，实现仅两列 + COMMITMENT 计入 actual | **P1-RC-003**（§5 详述） |
| 报表 actual 口径 vs 控制引擎 actual 口径 | MA2 A2.16 P1-MA2-084 fix 了**控制引擎** actual 通道含 COMMITMENT；未触及**报表** actual 口径 | RC 视角：同一"actual"语义在控制引擎（已三通道分离）与报表（仍 BUDGET OR NOT BUDGET）间**口径不一致** | 归 **P1-RC-003**（同一 finding 的两面：列缺失 + 口径不一致） |

### 4.3 E2E 行为证据（复用）

- `tests/e2e/business-actions/fin-budget-control.action.spec.ts`：UC-FIN-11 三级控制 E2E。
- `tests/e2e/business-actions/fin-budget-scenario.action.spec.ts`：UC-FIN-13 断言① BUDGET 凭证 E2E。
- `tests/e2e/business-actions/fin-commitment-accounting.action.spec.ts`：UC-FIN-13 断言③ 承付 commit/release E2E。
- `tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts`：UC-FIN-13 断言④ 报表 E2E（断言强度引用 A5.6 评级；本切片 §5 指出该 E2E 同样未验证 commitment 独立列——交 MA4 A4.1 按需展开运行时确认）。
- `tests/e2e/business-actions/fin-budget-rollforward-carryforward.action.spec.ts`：rollForward/carryForward E2E（UC-FIN-13 断言① 关联，A2 范围）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵（2 UC，每 UC 一行，不合并）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-FIN-11** 预算硬拦截 | `use-cases.md:204` ①PO.审核→check(科目,成本中心,期间,金额,来源单) ②余量=BUDGET−COMMITMENT−NORMAL ③余量<0 且 HARD→BLOCKED 抛异常订单保持 SUBMITTED ④==WARN 写日志放行 ⑤==NONE 放行 | `budget.md §业务规则2/4/8` + `§设计范式 余量公式实现注记`（设计参考，与 L1 一致；P1-MA2-084 已将控制引擎三通道分离对齐 L1） | `ErpFinBudgetControlBiz.check:62-104`（三通道 :78-81 / HARD 抛异常 :88-95 / WARN+日志 :96-102 / NONE :103）+ `applyPostingTypeFilter:151-167`（actual 排除 COMMITMENT）+ `ErpPurOrderProcessor.runBudgetCheckHook:177-189`（跨域钩子 :188） | `TestErpFinBudgetEndToEnd#testHardControlBlocked`（强）/`#testWarnControlLogsAndPasses`（强）/`#testNoneControlPasses`（强）/`#testAvailableDeductsCommitmentSeparately`（强：available=500 三通道）/`#testCommitmentZeroEquivalentAndReservationCountsAsActual`（强） | MA2 A2.5b 证实（预算方案状态机 + 控制钩子强一致）+ A2.16 P1-MA2-084 fix 证实（控制引擎三通道分离） | **接受**（5 验收标准 L3-L5 全证据一致；config 默认关闭见 §5.2 caveat ① + §7） |
| **UC-FIN-13** 预算管理(编制/控制/对比) | `use-cases.md:238` ①审批→BUDGET 凭证(postingType=BUDGET,businessType=BUDGET_SCENARIO,借贷按 subject.direction) ②PO.审核→check 三通道余量 HARD 抛异常/WARN 写日志 ③PO.APPROVED→COMMITMENT 凭证；CANCELLED/发票接收→红冲 ④按(acctSchema,subject,period,costCenter,project,postingType)分组 VoucherLine 得 Budget/Commitment/Actual **三列**，无需独立预算余额表 | `budget.md §业务规则1/3/5` + `§承付会计` + `cost-center.md`（设计参考；承付凭证 L2 描述两行 Dr-准备/Cr-承付 `budget.md:255` vs 实现单行——L2 drift 属 MA3 doc↔code 维度，以 L1 为准，L1 未规定 Dr/Cr 结构） | 断言①：`BudgetVoucherGenerator.generate:53-73`/`reverse:79-95`/`writeBudgetVoucher:97-180`；断言②：复用 `ErpFinBudgetControlBiz.check`；断言③：`CommitmentVoucherGenerator.generateCommitment:61-68`/`reverseCommitment:77-94` + `ErpFinBudgetCommitmentBizModel.commit:55/release:81/releaseIfPresent:104` + 跨域钩子 `ErpPurOrderProcessor:197/222`+`ErpPurOrderReverseApproveProcessor:42`+`ErpPurInvoiceProcessor:273`+`ErpPurReturnProcessor:281`；**断言④：`ErpFinBudgetLineBizModel.getBudgetVsActual:48-108` 仅两列 + COMMITMENT 计入 actual（:64-65,70-102），`BudgetVsActualRow` 无 commitmentAmount 字段** | 断言①：`#testApproveGeneratesBudgetVoucher`（强）+`#testCancelReversesBudgetVoucher`（强）；断言②：同 UC-FIN-11；断言③：`TestErpFinBudgetCommitment` 6 用例（强）+跨域 `TestErpPurOrderCommitment`/`TestErpPurReturnCommitmentRelease`/`TestErpPurBudgetControlIntegration`（强）；**断言④：`#testGetBudgetVsActual:195-219` 仅断言两列 + 未 seed commitment（三列需求零覆盖）** | 断言①③：MA2 A2.5b/A2.16 证实；**断言④：L3-L4 均仅两列，三列需求未实现** | **P1**（断言①②③接受；**断言④ 三列对比报表未满足 → §2 P1① 功能实质偏离验收标准 → P1-RC-003**。取最高=P1） |

### 5.2 plan baseline 三个 caveat 分级（§2 判据，逐项明确分级非悬空）

#### caveat ① config 默认关闭（`isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false）

- **三源对照**：
  - L1（`use-cases.md:204,238`）：描述控制**语义**（HARD/WARN/NONE 三通道 + 余量公式 + 承付 commit/release 触发条件），**未声明"必须默认开启"**。UC 文本以"采购订单.审核 → 调用 check"描述控制机制，控制级别由命中的 BudgetScenario.controlLevel 决定。
  - L2（`budget.md §配置项`）：`erp-fin.budget-commitment-enabled` 默认 false（"保护既有 113 purchase 测试不触发承付凭证"）；控制开关 javadoc 自述"默认 false，向后兼容"。
  - L3（`ErpFinBudgetControlBiz.isBudgetCheckEnabled:225-228` / `ErpFinBudgetCommitmentBizModel.isCommitmentEnabled:117-119`）：config-gate 默认 false。
- **分级裁决**：**接受**（非 P1）。理由：(1) 控制机制（HARD 拦截异常路径 + WARN 日志 + NONE 放行 + 三通道余量公式 + 承付 commit/release）**完整且正确实现**，L1 要求的异常路径（HARD 抛异常）**已实现非缺失**（§2 P1②「异常路径未实现」不成立——异常路径存在，仅由 config-gate 条件启用）；(2) L1 未强制"默认开启"，config-gate 是 ERP 通用启用范式（iDempiere 亦 gate 预算控制，未启用预算数据时硬开将阻断全部采购审核）；(3) §2 P1①「功能完全缺失」不成立——功能存在，启用后行为与 L1 完全一致。
- **残留观察**：config 默认关闭意味着"开箱默认不启用预算控制/承付"——若产品存在"开箱即用预算硬拦截"的隐含部署契约，则属默认行为分歧。此点 L1 未显式裁决，记入 §7 静态存疑点供 MA4 A4.1 运行时确认（无 P1 上证据，不自行升 P1）。

#### caveat ② actual 口径不一致 + 报表两列（`getBudgetVsActual` vs `aggregateAmount`）

- **三源对照**：
  - L1（`use-cases.md:261-262`）：逐字「按 ... postingType 分组 VoucherLine 得到 Budget/Commitment/Actual **三列**」。
  - L2（`budget.md §业务规则5`）：「按 (acctSchemaId, subjectId, periodId, costCenterId, projectId, postingType) 分组 ErpFinVoucherLine,得到 Budget/Commitment/Actual 三列」——L2 与 L1 一致，均要求三列。
  - L3（`ErpFinBudgetLineBizModel.getBudgetVsActual:48-108`）：voucher 过滤 `BUDGET OR NOT BUDGET`（:64-65，COMMITMENT 计入 actual）+ 仅 budget/actual 两通道聚合（:70-102）+ `BudgetVsActualRow` 无 commitmentAmount 字段（DTO :24-26 仅 budgetAmount/actualAmount/availableAmount）+ available=budget−actual（:104-106，未减 commitment）。**与控制引擎 `ErpFinBudgetControlBiz` 已修正的三通道分离（actual 排除 COMMITMENT）口径不一致**。
- **分级裁决**：**P1**（§2 P1①「功能实质偏离验收标准」——L1 明确要求三列，实现仅两列且口径错误；同 hitting §2 P1④「跨域/跨组件契约行为不一致」——同一"actual"语义在控制引擎与报表间不一致）。→ **P1-RC-003**（§5.3 详述）。
- **P0 升级评估**：经评估**维持 P1 不升 P0**。理由：(1) 此为**报表正确性**分歧，非活跃数据破坏——预算硬拦截本身（UC-FIN-11）经控制引擎三通道分离已正确，预算控制决策不受报表口径错误影响；(2) 触发面为"用户查看预算对比报表时看到两列/actual 含 commitment"，不破坏 GL 余额、不导致错误过账、不重复扣减预算（控制路径独立正确）；(3) 与 MA2 A2.16 对承付族 P1 分级一致（报表口径属报表正确性，非数据破坏）。

#### caveat ③ 承付单行凭证结构（`writeCommitmentVoucher:154-172`）

- **plan baseline 描述校正**：plan baseline 称"写单一资产负债式对称行（同科目 Dr/Cr 对称）"。**实仓核实校正**：`writeCommitmentVoucher:119-182` 实际写**单行单边凭证**——DEBIT 科目：debit=absAmount/credit=0（:141-142），凭证头 totalDebit=absAmount/totalCredit=0（:143-144），**仅一行** debitAmount=absAmount/creditAmount=0（:161-162）。即 header 借贷**不平**（Dr without Cr）。**非**"同科目 Dr/Cr 对称"。本审计以实仓代码为准。
- **三源对照**：
  - L1（`use-cases.md:257-258`）：仅要求「采购订单.APPROVED → 生成 COMMITMENT 凭证」「订单 CANCELLED 或发票接收 → 红冲 COMMITMENT」——**未规定 Dr/Cr 结构或凭证平衡**。
  - L2（`budget.md:255`）：描述「Dr 预算占用科目 / Cr 应付-承付科目」（**两行**平衡结构）——L2 与实现冲突，但承付凭证结构属 owner doc 机制描述（L2 非真相源）。
  - L3（`CommitmentVoucherGenerator.writeCommitmentVoucher:119-182`）：单行单边，header 借贷不平。
- **分级裁决**：**接受**（非 P1，L1 层面）。理由：(1) L1 未规定 Dr/Cr 结构，仅要求"生成 COMMITMENT 凭证 + 红冲"——L3 满足（凭证生成 + 红冲均实现且强测试覆盖）；(2) L2（`budget.md:255` 两行）vs L3（单行）的 drift 属 **owner-doc vs code 文本一致性**维度，归 audit-remediation MA3（本 RC mission 不重做文本一致性，§去重协议），非 RC 需求契约分歧；(3) 单行凭证的余额聚合正确性已由控制引擎三通道分离 + `testAvailableDeductsCommitmentSeparately` 证实（commitment 通道净额=200 正确计入余量减项）。
- **残留观察（记入 §7）**：承付凭证 header 借贷不平（totalDebit≠totalCredit）——若任何通用试算平衡报表/校验对所有 postingType 求和（不过滤 COMMITMENT），会破坏平衡恒等式。COMMITMENT 凭证经专用 Generator 直接写入（不经 ErpFinAcctDocRegistry 路由 / 不经 assertBalanced，per `budget.md:282` + CommitmentVoucherGenerator javadoc:25-26），故过账引擎的 balance 校验不触及——但通用报表层是否暴露此不平衡属运行时确认，交 MA4 A4.1。

### 5.3 P1 分级判据命中明细（§2）

#### P1-RC-003 — UC-FIN-13 断言④ 三列对比报表未满足（仅两列 + COMMITMENT 计入 actual）

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」+ §2 **P1④**「需求契约要求的跨域/跨组件契约行为不一致」（取最高=P1）
- **三源对照**：
  - L1（`use-cases.md:261-262`）：逐字「按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine 得到 Budget/Commitment/Actual **三列**, 无需独立预算余额表」。
  - L2（`budget.md §业务规则5`）：「按 ... postingType 分组 ... 得到 Budget/Commitment/Actual 三列」——L2 与 L1 一致，均要求三列。
  - L3（`ErpFinBudgetLineBizModel.getBudgetVsActual:48-108`）：(a) voucher 过滤 `BUDGET OR NOT BUDGET`（:64-65）把 COMMITMENT 计入 actual；(b) 仅 budget/actual 两通道聚合（:70-102）；(c) DTO `BudgetVsActualRow`（`module-finance/erp-fin-dao/.../dto/BudgetVsActualRow.java:24-26`）无 `commitmentAmount` 字段；(d) available=budget−actual 未减 commitment（:104-106）。**与控制引擎 `ErpFinBudgetControlBiz`（actual 通道排除 COMMITMENT，P1-MA2-084 已 fix）口径不一致**。
- **运行时影响**：预算对比报表显示两列（Budget/Actual）而非三列（Budget/Commitment/Actual）；且当存在承付凭证时，actual 列虚高（含 commitment 金额），available 偏低（未单独减 commitment）。**不影响预算硬拦截决策**（控制引擎独立三通道正确），仅影响报表展示正确性。
- **严重性**：major（验收标准明确要求三列未满足 + 跨组件口径不一致，但非活跃数据破坏）
- **P0 升级评估**：维持 P1 不升 P0（见 §5.2 caveat ②）。
- **修复义务**：§5 Q4=(a) 强制实现，禁止方案 B。经 MR1（R1.0 展开为 RC-R1.n）。修复 = (a) DTO `BudgetVsActualRow` 增 `commitmentAmount` 字段；(b) `getBudgetVsActual` voucher 过滤改三通道（BUDGET / COMMITMENT / ACTUAL 排除 BUDGET+COMMITMENT）+ 三通道分别累加；(c) available=budget−actual−commitment；(d) 报表前端/XPT 同步增 Commitment 列。**纯 BizModel/DTO/XPT 代码逻辑修复**（非 ORM 结构 / 非会计过账核心路径 VoucherFact/PostingProcessor）→ 按 roadmap 预授权类目（代码逻辑修复）可自动执行，**不触发 §5 ask-first**（未触及 ORM/过账核心/数据删除三类保护区域）。
- **与既有 finding 关系**：grep arm-index finance 预算/承付同域。**P1-MA2-084**（A2.16）= 控制引擎 `ErpFinBudgetControlBiz.aggregateAmount` actual 含 COMMITMENT（**已 fix**）。本 finding = **报表** `ErpFinBudgetLineBizModel.getBudgetVsActual`（**不同方法 / 不同类 / 不同控制点**：控制决策 vs 报表展示）+ **额外**的 commitment 列缺失问题——**新根因新控制点**，§7 裁决=新建 P1-RC-003 + 交叉引用 P1-MA2-084（同 pattern 根因的姊妹站点，P1-MA2-084 fix 时未同步修报表）。

### 5.4 接受类结论汇总

| UC / 断言 | 接受依据 |
|---|---|
| UC-FIN-11（全 5 验收标准） | 三通道余量公式（:78-81）+ HARD 抛异常（:88-95）+ WARN+日志（:96-102）+ NONE（:103）L3 实现 + `testHardControlBlocked`/`testWarnControlLogsAndPasses`/`testNoneControlPasses`/`testAvailableDeductsCommitmentSeparately`/`testCommitmentZeroEquivalentAndReservationCountsAsActual` 强断言 + MA2 A2.5b/A2.16 行为背书全一致 |
| UC-FIN-13 断言①（BUDGET 影子凭证） | `BudgetVoucherGenerator.generate:53`/`reverse:79` + postingType=BUDGET + 借贷按 subject.direction + `testApproveGeneratesBudgetVoucher`/`testCancelReversesBudgetVoucher` 强断言 |
| UC-FIN-13 断言②（预算控制） | 复用 UC-FIN-11 控制引擎（同 check SPI），接受依据同 UC-FIN-11 |
| UC-FIN-13 断言③（承付 commit/release） | `CommitmentVoucherGenerator` + `ErpFinBudgetCommitmentBizModel` + 跨域 4 钩子（commit/release-on-cancel/release-on-invoice-approve/release-on-return）+ reject release-on-receive + 重复 release 守卫 + `TestErpFinBudgetCommitment` 6 用例 + 跨域 3 测试强断言 + MA2 A2.16 释放路径完整性背书 |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` finance 预算/承付同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本切片 finding 关系 | 裁决 |
|---|---|---|---|
| **P1-MA2-084** `ErpFinBudgetControlBiz.aggregateAmount` 实际聚合含 COMMITMENT | **控制引擎** actual 通道（已 fix 为三通道分离） | **姊妹站点**：本切片 finding 是**报表** `getBudgetVsActual`（不同方法/不同类）+ 额外 commitment 列缺失。P1-MA2-084 fix 时未同步修报表 | **新建 P1-RC-003** + 交叉引用 P1-MA2-084（同 pattern 根因，不同控制点不可合并） |
| P1-MA2-081 部分开票全额释放语义 | 承付释放语义（owner doc drift） | 不同控制点（释放语义 vs 报表列数） | 不相关（本切片复用其行为证据，§4.1） |
| P1-MA2-082 采购退货未释放 | release-on-return（保守方向偏移） | 不同控制点；本切片核验 release-on-return 钩子已落地（config 默认 off） | 不相关（本切片复用，§4.1） |
| P1-MA2-083 AP/AR 冲销后 commitment 未恢复 | 冲销恢复语义 | 不同控制点 | 不相关（本切片复用，§4.1） |
| P2-MA2-073 TestErpSalOrderCommitment 缺 Dr/Cr 断言 | 测试断言强度（sales） | 不同维度（测试质量 vs 需求契约报表列） | 不相关 |
| P1-MA3-025 / P1-MA3-026 / P1-MA3-031 postingType 字典/Provider doc drift | doc↔code 文本一致性 | 不同维度（MA3 文本一致性 vs RC 需求契约） | 不相关（§去重协议 MA3 边界） |

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| **P1-RC-003** | UC-FIN-13 断言④ | 三列对比报表未满足（`getBudgetVsActual` 仅两列 + COMMITMENT 计入 actual + DTO 无 commitmentAmount 字段） | P1-MA2-084 = **控制引擎** actual 通道（已 fix）；本 finding = **报表** getBudgetVsActual（不同方法/不同类/不同控制点：控制决策 vs 报表展示）+ 额外 commitment 列缺失——**新根因新控制点**，不可合并 | **新建**（交叉引用 P1-MA2-084） |

### 6.3 双向可追溯

- **新 finding → arm-index**：P1-RC-003 将写入 `arm-index.md` MA1 RC finding 分区（§7 归档纪律）。
- **finding → 修复**：待 MR1 R1.0 展开为 RC-R1.n 修复行（本审计不实施修复）。
- **既有 finding 交叉引用**：P1-RC-003 交叉引用 P1-MA2-084（姊妹站点，同 pattern 根因），MR1 修复时协同（报表三通道化对齐控制引擎已 fix 的三通道分离）。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

1. **caveat ① config 默认关闭是否与"开箱即用预算硬拦截"部署契约冲突**：L3 静态确认 `isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false（控制机制完整正确，仅 config-gate 条件启用）。L1 未显式声明"默认开启"。若产品存在"开箱即用预算控制"隐含契约，则属默认行为分歧——交 MA4 A4.1 运行时确认（核对 product-scope / 部署文档是否声明默认预算控制启用；无则维持接受）。当前无 P1 上证据。
2. **caveat ③ 承付凭证 header 借贷不平（totalDebit≠totalCredit）是否被通用试算平衡报表暴露**：L3 静态确认 COMMITMENT 凭证经专用 Generator 直接写入（不经 ErpFinAcctDocRegistry 路由 / 不经 assertBalanced），过账引擎 balance 校验不触及。但通用试算平衡 / 财务三大报表（UC-FIN-16，A1.7 切片）若对所有 postingType 求和不过滤 COMMITMENT，会破坏平衡恒等式——交 MA4 A4.1 运行时确认（核对试算平衡报表是否过滤 BUDGET/COMMITMENT 影子凭证；A1.7 切片审计 UC-FIN-16 时复核）。当前归 MA3 doc↔code 维度（L2 `budget.md:255` 两行 vs 实现单行）+ MA4 运行时确认，非 RC P1（L1 未规定凭证平衡）。
3. **UC-FIN-13 断言④ E2E `fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列**：L4 静态确认单测 `testGetBudgetVsActual` 仅断言两列且不 seed commitment。E2E spec 的断言强度属运行时确认（A5.6 评级视角）——交 MA4 A4.1 按需展开（与 P1-RC-003 修复协同：修复后 E2E 须同步补 commitment 列断言）。
4. **承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态**：L3 确认钩子已落地（`ErpPurReturnProcessor:281-297`）但 config 默认 false（`commitment-release-on-return` + 依赖 `budget-commitment-enabled`）。MA2 A2.16 P1-MA2-082 已登记（保守方向偏移）。本切片不重复登记，交 MA4 按需确认部署是否启用——属既有 finding 行为证据，非新发现。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（P1-RC-003 为 P1），按 §10 **不触发 MR0**。该 finding 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-02 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)` 权威块）。

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

> 注：checker 脚本打印头「基线 69/66/38」为脚本内 stale 硬编码字符串；权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 为准（R12c=40，经 V.2 plan `2026-08-02-0651-1` 裁决性上调）。actual R12c=40 ≤ baseline 40，0 漂移。

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2 已证实行为**（不重新核实）：
  - `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（A2.5b）：预算方案状态机（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED）+ 承付 commit/release 独立凭证状态机 + 期间状态机守卫。本切片 UC-FIN-13 断言①③的 L5 行为证据直接引用。
  - `2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16）：承付释放路径完整性（3+1 接入点齐全 + reject release-on-receive + 重复释放守卫 + 取消后再发票容错 + 多年度跨期余量一致 + config-gate 默认 false）。本切片 UC-FIN-13 断言③的 L5 行为证据直接引用。
  - `2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（MA4）：预算/AR-AP/成本/期间代码质量，本切片未重复审查代码质量维度。
  - E2E specs：`fin-budget-scenario`/`fin-budget-control`/`fin-commitment-accounting`/`fin-budget-vs-actual`/`fin-budget-rollforward-carryforward`，本切片 L5 复用（断言强度引用 A5.6 评级）。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-FIN-13 断言④ 三列对比报表未满足**（P1-RC-003）：MA2 A2.16 审查承付释放路径完整性 + 控制引擎三通道（P1-MA2-084 fix），**未审查报表 `getBudgetVsActual` 的列数/口径**。本切片从 UC-FIN-13 断言④ 逐字对照，定级报表仅两列 + actual 含 commitment 为独立 P1（P1-MA2-084 的姊妹站点，fix 时未同步修报表）。
  2. **plan baseline caveat ①③ 分级**：config 默认关闭（caveat ①）+ 承付单行凭证结构（caveat ③）——MA2 未从需求契约视角分级，本切片按 §2 判据定级（①接受/③接受+L2 drift 归 MA3），并校正 plan baseline caveat ③ 的描述（实仓为单行单边，非"Dr/Cr 对称"）。
- **MA2 finding 复核无升级**：本切片复核 MA2 已登记的 5 项预算/承付 finding（P1-MA2-081/082/083/084 + P2-MA2-073），运行时行为与 MA2 登记一致，**无升级 P0**。P1-MA2-084（控制引擎）已 fix 证实；其姊妹站点报表（P1-RC-003）为本切片新发现。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（带 1 项 P1 残留 + UC-FIN-11 全接受 + UC-FIN-13 断言①②③接受）

**审查范围**：UC-FIN-11/13 共 2 UC 五级追踪矩阵（L1-L5）+ 每 UC 符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-FIN-11 全 5 验收标准 L3-L5 一致；UC-FIN-13 断言①②③接受。

**P1 残留**：P1-RC-003（UC-FIN-13 断言④ 三列对比报表未满足——`getBudgetVsActual` 仅两列 + COMMITMENT 计入 actual + DTO 无 commitmentAmount）→ MR1（R1.0 展开为 RC-R1.n）。纯 BizModel/DTO/XPT 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first。

**P0**：无。不触发 MR0。

**剩余风险**：见 §7 静态存疑点清单（4 项交 MA4 A4.1 运行时展开：config 默认关闭部署契约 / 承付凭证借贷不平时报暴露 / E2E commitment 列断言 / release-on-return 启用状态）。
