# MA4 finance 代码质量审计 — 预算/AR-AP/成本/期间链路（A4.1b — 代码实现质量）

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> 域/功能模块：finance / 预算与承付 + AR-AP 核销 + 坏账与汇兑 + 成本（finance 侧 GL 映射）+ 期间与结账 + GL 映射与科目（A4.1b S 级拆分 2/2）
> 审计 plan：`docs/plans/2026-07-28-2130-3-audit-remediation-ma4-finance-budget-arap-cost-period-code-quality.md`
> 来源 finding（运行时复核）：P1-MA1-016 / P1-MA2-017 / P1-MA2-018 / P1-MA2-019 / P1-MA2-020 / P1-MA2-021 / P1-MA2-022 / P1-MA2-033 / P1-MA2-034 / P1-MA2-081 / P1-MA2-082 / P1-MA2-083 / P1-MA2-084 / P1-MA2-087 / P1-MA2-096 / P1-MA2-098 / P1-MA3-024~029 / P1-MA3-032~038 / P0-MA2-016（fixed 复核）/ P0-MA2-018（deferred 复核）
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + 严重性指南 P0-P3）
> 审计日期：2026-07-29
> 审计者：主代理（独立子代理已完成草案审查 + 结束审计，见 plan §Draft Review Record / §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏路径） | **0** | 无即时通道修复（TOCTOU 已登记 P0-MA2-018 deferred、CloseVoucherWriter 幂等缺口已登记 P1-MA2-087，本审计不重复） |
| **P1**（新登记） | **3** | P1-MA4-004（期间结账编排跨域 command 异常吞咽致业财悬挂）/ P1-MA4-005（测试有效性不足）/ P1-MA4-006（预算/AR-AP/成本/期间链路跨域 daoFor 绕 I\*Biz） |
| **P2**（watch-only） | **1** | P2-MA4-003（可维护性/性能热点合并：FX 全表扫描 / 预算与坏账余额全局 voucherId 内存载入 / closePeriod no-op 状态 / precheck load-then-filter / 重复 subject 解析模式） |
| MA1/MA2/MA3 finding 运行时复核 | 20 项 | 全部「如登记」无升级；P0-MA2-016（FX 损益结转）复核**确认已修复**；P1-MA2-022 复核时**发现相邻代码路径新缺陷**（银行存款重估全表扫描→P2-MA4-003(a)） |

**整体裁决**：**FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；TOCTOU 类已登记 deferred / CloseVoucherWriter 经期间 version guard bounded）。预算/AR-AP/成本/期间链路在**算术正确性 / 状态机对称性 / 事务边界 / 红冲闭环强一致**四面扎实（核销三策略 FIFO/BY_AMOUNT/BY_RATIO 实现正确 + ReconciliationSettler 双向对称 + 坏账 writeOff/recovery/reverseApprove 三向对称 + 年度结转/损益结转/汇兑重估聚合公式正确 + @BizMutation 事务回滚保证辅助账与凭证一致性），但**失败恢复闭环 / 错误传播**存在 1 项 P1 代码缺陷（期间结账编排 catch(Exception)→LOG.warn 吞咽折旧/存货成本重算失败致期间带病关闭）、**测试有效性**存在 1 项 P1 缺陷（期间结账 E2E 仅 1 测试 + FX 仅 2 测试 + 多期/异常路径覆盖薄）、**架构边界**存在 1 项 P1 缺陷（DualSideConsistencyChecker 等多站点跨域 daoFor 绕 I\*Biz，同 P1-MA1-022/P1-MA4-003 根因在 A4.1b 范围投影）。MA1/MA2/MA3 已知 finding 运行时复核 20 项全部「如登记」无升级，其中 P0-MA2-016 复核确认修复落地（ProfitLossClosingService 仅排除 PERIOD_CLOSE、EXCHANGE_GAIN_LOSS 正常结转）、P1-MA2-022 复核发现相邻银行存款重估路径有性能缺陷 P2-MA4-003(a)。

**裁决分布**：P1-MA4-004 → **MR1**（业财悬挂闭环，与 P1-MA2-017/032/048/060 同型根因；R4.1 可裁决）/ P1-MA4-005 → **MR2**（测试质量，MA4 维度；与 A5.1 覆盖深度互补）/ P1-MA4-006 → **MR1**（同 P1-MA1-022/P1-MA4-003 一并裁决，**不重复计入 MR2**）。

**与 A4.1a 合并后 finance 代码质量全片终态**：A4.1a（过账/凭证）3 P1 + 2 P2 + A4.1b（预算/AR-AP/成本/期间）3 P1 + 1 P2 = **finance 代码质量全片 6 P1 + 3 P2**，零 P0。finance 代码质量维度在此收口。

---

## 1. 审计范围与方法覆盖矩阵

### 1.1 审计对象（实仓逐项核实，60 源文件中核心组件抽样）

| 功能模块 | 组件 | 文件 | 关键行 | 审计状态 |
|---|---|---|---|---|
| 预算控制 | 预算控制 Biz | `budget/ErpFinBudgetControlBiz.java` | check:57-99 / aggregateAmount:102-137 / findMatchingBudgetLine:140-157 / writeControlLog:159-183 | ✅ |
| 预算与承付 | 预算方案 Processor | `processor/ErpFinBudgetScenarioProcessor.java` | resolveUserId:551-559(catch ignored) / generateBudgetVoucher:562-569 / validateCarryForwardPreconditions | ✅ |
| 预算与承付 | 承付 Biz | `budget/ErpFinBudgetCommitmentBizModel.java` | loadSubject:120,129（daoFor ErpMdSubject） | ✅ |
| 预算凭证 | 预算凭证生成器 | `budget/BudgetVoucherGenerator.java` | loadSubject:220（daoFor ErpMdSubject） | ✅ |
| AR-AP 核销 | 核销编排 Biz | `entity/ErpFinReconciliationBizModel.java` | create:68-112 / post:114-140 / reverse:142-157 / runAutoReconciliation:216-247（flushSession:242）/ validateLine:278-306 | ✅ |
| AR-AP 核销 | 自动核销引擎 | `reconciliation/AutoReconciliationEngine.java` | matchFifo:98-138 / matchByAmount:142-165 / matchByRatio:169-221 / line:289-298 | ✅ |
| AR-AP 核销 | 结算器 | `reconciliation/ReconciliationSettler.java` | settle:32-47 / reverseSettle:52-60 / applySettlement:62-81 / resolveStatus:83-93 | ✅ |
| AR-AP 核销 | 双方一致性检查 | `reconciliation/DualSideConsistencyChecker.java` | daoFor(ErpPurInvoice):133 / daoFor(ErpSalInvoice):141 | ✅ |
| AR-AP 核销 | 辅助账生成器 | `posting/ErpFinArApItemGenerator.java` | generate:65-119 / cancelOnReverse:125-136 / resolveProfile:140-183 / buildCode:216-230 | ✅ |
| 坏账核销/收回 | 坏账 Processor | `processor/ErpFinBadDebtProcessor.java` | writeOff:61-71 / recover:73-82 / approve:93-100 / reverseApprove:124-164 / executeWriteOff:183-202 / executeRecovery:208-226 | ✅ |
| 坏账计提/释放 | 坏账准备服务 | `baddebt/BadDebtProvisionService.java` | runBadDebtProvision:85-94 / reverseBadDebtProvision:116-155 / getAllowanceBalance:251-270（findPostedVoucherIds 无 period 过滤）/ findReceivableOpenItems:288-297 | ✅ |
| 汇兑重估 | FX 服务 | `fx/ExchangeRevaluationService.java` | revalueArAp:103-155（无 period 过滤）/ revalueBankDeposits:162-216 / aggregateBankSubjectBookFunctional:219-251（findAllByQuery(new QueryBean()) 全表扫描） | ✅ |
| 期间结账编排 | 期间 Processor | `processor/ErpFinAccountingPeriodProcessor.java` | closePeriod:130-163（setStatus no-op:157-158）/ preCheck:94-102 / closeInvModule:314-318 / closeAssetModule:321-325 / closeGlModule:333-341 / runDepreciation:344-357（catch Exception→LOG.warn）/ recloseInvCosts:364-378（catch Exception→LOG.warn）/ reverseDepreciation:381-396（catch Exception→LOG.warn + daoFor ErpAstDepreciationSchedule:385）/ populateTrialBalanceForAllSchemas:464-516 / findUnpostedVoucherCodes:565-573 / findUnsettledArApCodes:575-586 | ✅ |
| 损益结转 | PNL 服务 | `profitloss/ProfitLossClosingService.java` | closeForSchema:69-156（排除 PERIOD_CLOSE:91，含 EXCHANGE_GAIN_LOSS ✓） | ✅ |
| 年度结转 | 年结服务 | `annualclose/AnnualCloseService.java` | transferProfitToRetainedEarnings:92-128 / populateNextYearOpening:135-182（aggregateYearSubjectActivity 仅本年:283）/ assertAuxiliaryReconciles:191-230（sumArApOpenFunctional 无年度过滤:232-247） | ✅ |
| 测试套件 | 期间结账 E2E | `test/.../TestErpFinPeriodCloseEndToEnd.java` | 1 @Test（单黄金路径） | ✅ |
| 测试套件 | 汇兑重估 | `test/.../TestErpFinExchangeRevaluation.java` | 2 @Test | ✅ |
| 测试套件 | 核销/坏账/年结/预算 | `TestErpFinReconciliation/BadDebt/AnnualClose/BudgetEndToEnd` 等 | 7/7/6/6 @Test | ✅ |

### 1.2 Skill 维度覆盖（`code-quality-audit-prompt.md` 7 重点领域）

| # | 维度 | 裁决 | 发现 |
|---|------|------|------|
| 1 | 架构和边界完整性 | ⚠️(P1) | A4.1b 范围多站点跨域 `daoFor(ErpMd*)` 只读 + `DualSideConsistencyChecker:133,141` 跨域 `daoFor(ErpPurInvoice/ErpSalInvoice)` + `ErpFinAccountingPeriodProcessor:385` `daoFor(ErpAstDepreciationSchedule)`（=P1-MA1-016）；生成物零手编 ✅；跨域 command（折旧/存货成本）经 I\*Biz Facade ✅ |
| 2 | 核心实现正确性 | ⚠️(P1) | 期间结账编排 catch(Exception)→LOG.warn 吞咽折旧/存货成本重算失败（P1-MA4-004）；核销 flush 时机正确（runAutoReconciliation:242 flushSession 已修复 ✓）/ 核销 runMatching 非幂等如 P1-MA2-098 登记 / 年初余额非累计如 P1-MA2-018 登记 / 辅助账对账作用域如 P1-MA2-019 登记 / FX 无前期 reversal + 无 period 过滤如 P1-MA2-022 登记 + 相邻银行存款重估全表扫描 P2-MA4-003(a) |
| 3 | 类型和契约质量 | ✅(P2) | 核销三策略 FIFO/BY_AMOUNT/BY_RATIO 类型安全 ✅；多账套 cache key 如 P1-MA2-099 登记（不在本批文件直接投影）；billData 字段键名回退（SUPPLIER_ID/CUSTOMER_ID/EMPLOYEE_ID）文档化为 0300-1/0300-2 兼容，P2 可维护性 |
| 4 | 错误处理和操作安全 | ⚠️(P1) | 全链路 NopException + ErrorCode（erp.err.fin.*）✅；但期间结账跨域 command 失败仅 LOG.warn 不进异常工作台不阻断（P1-MA4-004，复核 P1-MA2-017 阻断分级）/ 反结账 kill-switch 如 P1-MA2-020 登记 |
| 5 | 测试有效性 | ⚠️(P1) | 期间结账 E2E 仅 1 测试 + FX 仅 2 测试 + 无多期 FX reversal（P1-MA2-022）/ 无期间非法迁移 E2E / 坏账核销非 OPEN 异常路径覆盖薄 + 断言强度弱（P1-MA4-005） |
| 6 | 可维护性和未来变更风险 | ⚠️(P2) | FX 银行存款重估全表扫描 + 预算/坏账余额全局 voucherId 内存载入 + closePeriod setStatus(CLOSING) 紧接 setStatus(CLOSED) no-op + precheck load-then-filter-in-Java + 重复 subject 解析模式散布（P2-MA4-003） |
| 7 | 自动化和防护覆盖 | ⚠️(P2) | compliance checker R2d 未覆盖 `*Service`/`*Generator`/`*Checker`/`*Settler`/`*Engine` 文件名（DualSideConsistencyChecker/ErpFinArApItemGenerator 的跨域 daoFor 无静态守卫）；期间结账/年结/核销无回归测试门控（P1-MA4-005） |

---

## 2. 重点领域逐项审查结果

### 2.1 领域「架构和边界完整性」— ⚠️(P1)

**核查项**：期间结账编排跨域 command 是否经 I\*Biz / 核销辅助账生成是否经 Facade / 承付释放接入点是否合规 / 生成物是否手编 / P1-MA1-016 运行时状态。

**证据**：
- **跨域 command 经 I\*Biz Facade**：期间结账的折旧（`runDepreciation:349-351` 经 `bizObjectManager.getBizObject(...).asProxy()` → `IErpAstDepreciationScheduleBiz.executeBatchDepreciation`）+ 存货成本重算（`recloseInvCosts:369-371` 经 `IErpInvCostingBiz.reclosePeriodCosts`）均经 I\*Biz Facade，符合 data-dependency-matrix.md §3.2/§4.4「finance 对业务域纯读不回写；期间结账 command 编排在 I\*Biz 层合法」✅。
- **【P1-MA1-016 运行时状态 + 本审计补充】reverseDepreciation 跨域 DAO**：`ErpFinAccountingPeriodProcessor.reverseDepreciation:385` 使用 `daoProvider.daoFor(ErpAstDepreciationSchedule.class)` 直接跨域查询 assets 实体（仅按 period+posted 过滤），如 **P1-MA1-016 登记**。本审计补充：该处仅只读查询 schedules 用于循环调 `depreciationBiz.reverseDepreciation`（写仍经 I\*Biz），无活跃数据破坏，维持治理层 P1。
- **【新缺陷 P1-MA4-006】A4.1b 范围跨域 daoFor 绕 I\*Biz（多站点）**：grep `daoFor(Erp` 于 budget/reconciliation/fx/baddebt/annualclose/profitloss/processor/entity 命中多站点 finance→master-data 只读 + finance→purchase/sales 只读：
  - `ErpFinBudgetControlBiz:191` / `BudgetVoucherGenerator:220` / `ErpFinBudgetCommitmentBizModel:120,129` / `ErpFinBudgetLineBizModel:121` → `daoFor(ErpMdSubject.class)` 只读（科目解析）
  - `ProfitLossClosingService:173,182,217` / `ExchangeRevaluationService:259,268,281` / `AnnualCloseService:360,389` / `BadDebtProvisionService:328,365` / `ErpFinBadDebtProcessor:367` → `daoFor(ErpMdSubject/Currency.class)` 只读
  - **`DualSideConsistencyChecker:133,141` → `daoFor(ErpPurInvoice.class)` / `daoFor(ErpSalInvoice.class)` 只读**（finance→purchase/sales 跨域，P1-MA1-022 原枚举 9 域未显式枚举此 finance 核销 checker 投影）
  - 与 P1-MA1-022 / P1-MA4-003（过账链路投影）同根因，本批是其在 budget/arap/baddebt/close/fx/pnl/annual helpers 的投影。
- **核销辅助账生成经内部 DAO**：`ErpFinArApItemGenerator` / `ReconciliationSettler` 经 `daoFor(ErpFinArApItem.class)` 操作本域辅助账实体（finance 域内部实体，非跨域）✅。`ErpFinReconciliationBizModel.loadItem:351` 注释「D2 边界场景：跨实体只读加载辅助账项（核销内部实体，无独立 IBiz）」合规 ✅。
- **生成物零手编**：budget/ / reconciliation/ / fx/ / baddebt/ / annualclose/ / profitloss/ / processor/ 下全部为手写非 `_gen` 文件；未发现 `_` 前缀文件手编 ✅。

**裁决**：⚠️(P1) — P1-MA4-006（A4.1b 范围跨域 daoFor 投影）登记交叉引用 P1-MA1-022 / P1-MA4-003，MR1 一并裁决（不重复计入 MR2）。reverseDepreciation 跨域 DAO 维持 P1-MA1-016。

### 2.2 领域「核心实现正确性」— ⚠️(P1)

**核查项**：预算控制事务一致性 / 核销 runAutoReconciliation flush 时机 / 坏账 Processor 异常路径 / 汇兑重估期间过滤 / 年度结转累计余额 / 辅助账对账作用域 / runMatching 幂等。

**证据**：
- **【新缺陷 P1-MA4-004】期间结账编排 catch(Exception)→LOG.warn 吞咽跨域 command 失败致业财悬挂**：`ErpFinAccountingPeriodProcessor` 三处跨域集成均 `catch (Exception e) { LOG.warn(...) }` 后**不阻断、不进异常工作台、无告警**：
  - `runDepreciation:353-356`（折旧计提失败）
  - `recloseInvCosts:375-377`（存货成本兜底重算失败）
  - `reverseDepreciation:393-395`（反结账折旧冲销失败）
  后果：若 assets 折旧引擎 / inventory 成本重算因**配置错误或真实故障**（非仅"impl 未就绪"）抛 NopException（如 `ERR_DEPRECIATION_*` / `ERR_COSTING_*`），期间仍正常推进至 CLOSED（`closePeriod:157-159` 紧接 setStatus），GL 缺折旧/成本调整凭证但期间已锁定。与 P1-MA2-017（auto-post-on-close 阻断分级）+ P1-MA2-032（IGNORED 悬挂）+ P1-MA2-048（salary 吞异常）+ P1-MA2-060（assets tryPost 吞异常）是**同型根因**（业财不一致悬挂），但本缺陷审的是**期间结账编排层的跨域 command 错误传播**——MA2 未覆盖此层（MA2 审的是各 dispatcher 的 tryPost 吞咽，非期间 Processor 的集成编排吞咽）。期末结账前置检查 `findUnresolvedPostingExceptionKeys:589-602` 仅扫 `status=PENDING/RETRYING` 的 `ErpFinPostingException`，**不覆盖** assets/inventory 域内部失败（这些失败不进 finance 异常工作台），间接兜底失效。
- **核销 flush 时机正确（已修复复核）**：`runAutoReconciliation:242` `orm().flushSession()` 在 `post(head.getId(), ctx)` 前，确保 `create` 写入的核销单+行落库使 `post`→`settler.settle`→`partnerBalanceUpdater.refresh` 聚合查询读到最新数据。`post:137` / `reverse:154` 的 `flushBeforeBalance()`（`orm().flushSession()`）在 `partnerBalanceUpdater.refresh` 前，确保 settler 对辅助账的脏改动落库。flush 缺失 bug 已修复 ✅。
- **核销三策略算术正确**：`matchFifo` / `matchByAmount` / `matchByRatio` 实现正确——FIFO 按到期日升序逐笔核销 + 尾差归末行（BY_RATIO:205-213 `tail = pmtOpen - allocated`）保证 Σallocated==pmtOpen；`ReconciliationSettler.applySettlement` 双向对称（reverse sign=-1）；`resolveStatus` 三态翻转正确（settled≤0→OPEN / settled≥total→SETTLED / else PARTIAL）✅。
- **坏账 Processor 异常路径对称**：`executeWriteOff:183-202` / `executeRecovery:208-226` / `reverseApprove:124-164` 三向对称（writeOff 反向 = reverseApprove 的 writeOff 分支；recovery 反向 = reverseApprove 的 recovery 分支）。`reverseApprove` 先红冲凭证（`finPostingExecutor.reverse:136` 抛 NopException 触发事务回滚）再回退辅助账状态，强一致 ✅。`writeOff:64-69` 无审批路径先 executeWriteOff 再 setApproved，全在 @BizMutation 事务内，原子 ✅。
- **runMatching 非幂等如 P1-MA2-098 登记**：`runAutoReconciliation` 每次 create+post 新核销单（code=REC-UUID），无 `(pairKey, periodId)` UK 去重，重复调用产生重复核销单。运行时确认如登记，无新代码层缺陷。
- **年初余额非累计如 P1-MA2-018 登记**：`AnnualCloseService.populateNextYearOpening:148` 经 `aggregateYearSubjectActivity(year)` 仅聚合本年度分录净额（`findYearPostedVoucherIds:309-335` 按 year 过滤期间），缺上年结转额。运行时确认如登记。
- **辅助账对账作用域如 P1-MA2-019 登记**：`assertAuxiliaryReconciles:199-200` 的 `sumArApOpenFunctional:232-247` 汇总全历史开放项（无年度过滤），GL 侧 `subjectNetForYear:266-281` 仅本年，作用域不一致。运行时确认如登记。
- **FX 无前期 reversal + 无期间过滤如 P1-MA2-022 登记**：`revalueArAp:106-112` 查询所有未核销外币项不按期间过滤，重估后不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。运行时确认如登记。**相邻代码路径新缺陷**：`aggregateBankSubjectBookFunctional:236` `lineDao.findAllByQuery(new QueryBean())` 全表载入后内存过滤 voucherIds（P2-MA4-003(a)）。
- **预算控制事务一致性**：`ErpFinBudgetControlBiz.check` 在采购/付款/报销审核事务内同步校验，HARD 阻断抛异常触发外层 @BizMutation 回滚 ✅。`aggregateAmount` COMMITMENT 语义如 P1-MA2-084 登记（actual 通道含 COMMITMENT，等价正确但语义混淆）。
- **CloseVoucherWriter 幂等缺口如 P1-MA2-087 登记**：FX/PNL/年结/坏账凭证经 `CloseVoucherWriter.writeVoucher` → `IErpFinVoucherBiz.post`，依赖 alreadyPosted TOCTOU + 无 (billHeadCode,businessType) UK。bounded by 期间 version guard。与 P0-MA2-018 同根因，P0-MA2-018 修复后自动闭包。运行时确认如登记。

**裁决**：⚠️(P1) — P1-MA4-004（期间结账编排异常吞咽）是新发现的核心实现正确性 + 错误处理闭环缺陷；其余（flush 修复确认 / 三策略算术 / 坏账对称 / 年初余额 / 辅助账作用域 / FX reversal / runMatching 幂等 / CloseVoucherWriter 幂等）均如 MA2 登记，运行时确认无升级。

### 2.3 领域「类型和契约质量」— ✅(P2)

**核查项**：预算场景 Processor 参数返回契约一致性 / 核销三策略类型安全 / 多账套 cache key 类型。

**证据**：
- **核销三策略类型安全**：`AutoReconciliationEngine.matchFifo/matchByAmount/matchByRatio` 全程 BigDecimal（`openFunctional` / `precision` / `norm`），无浮点损失；`line:289-298` 构造 `ReconciliationLineInput` 字段类型一致 ✅。
- **ErpFinArApItemGenerator billData 字段键名回退**：`resolvePartnerId:240-254` / `resolveAmountFunctional:268-306` 支持 `partnerId` / `SUPPLIER_ID` / `CUSTOMER_ID` / `EMPLOYEE_ID` / `TOTAL_AMOUNT_WITH_TAX` / `TOTAL` / `TOTAL_AMOUNT` / `AMOUNT` / `FACE_AMOUNT` / `TOTAL_COST` 多键回退——文档化为 0300-1/0300-2 派发器兼容（finance 为 DAG 顶，生成器只读 billData）。键名散布是可维护性缺陷（P2），但文档化充分，非契约违规。
- **`buildCode:216-230` 哈希摘要兜底**：sourceBillCode 超长时截断 + MD5 前 4 hex 摘要 + uuid8 兜底，消除 voucherCode precision 50 字符串右截断 latent defect，类型/长度契约正确 ✅。
- **多账套 cache key**：如 P1-MA2-099 登记（GL 映射 cache 默认 org-dimension-enabled=false 时塌缩 "_" bucket），不在本批文件直接投影，无新代码层缺陷。

**裁决**：✅(P2) — 三策略类型安全 + buildCode 长度契约正确；billData 键名回退为文档化兼容（P2 可维护性，归 P2-MA4-003）；无新类型/契约缺陷。

### 2.4 领域「错误处理和操作安全」— ⚠️(P1)

**核查项**：预算/AR-AP/成本/期间链路异常是否全扩展 NopException + ErrorCode / 期间结账前置检查错误传播 / 反结账 kill-switch。

**证据**：
- **全链路 NopException + ErrorCode**：grep `extends RuntimeException` 于 budget/reconciliation/fx/baddebt/annualclose/profitloss/processor = **0 命中**（R4 合规）。所有 throw 点使用 NopException + `ErpFinErrors.ERR_*` + `.param()` 携带上下文 ✅。`ErpFinReconciliationBizModel.statusError:378-382` / `ErpFinBadDebtProcessor.illegalTransition:389-394` 等异常工厂携带状态上下文 ✅。
- **【P1-MA4-004 同根】期间结账跨域 command 错误传播不足**：`runDepreciation:353` / `recloseInvCosts:375` / `reverseDepreciation:393` `catch (Exception e)` 过宽（含 NopException 配置错误），仅 `LOG.warn("...跳过（{}）", e.getMessage())`，**不进 ErpFinPostingException 异常工作台、不派发 IErpSysNotificationBiz 告警、不阻断结账**。配置错误（如 `ERR_DEPRECIATION_RATE_MISSING`）与"impl 未就绪"被同等吞咽。复核 P1-MA2-017（auto-post-on-close 阻断分级）：auto-post-on-close=false 时 `preCheck.hasIssues()` 阻断未核销 AR-AP/未过账凭证，但**不覆盖** assets/inventory 集成失败（这些在 closeInvModule/closeAssetModule 内吞咽，不在 preCheck 范围）。
- **反结账 kill-switch 如 P1-MA2-020 登记**：`reverseClose:278-281` 默认 config `reverse-close-approval-required=true` 时直接 throw，无审批流。运行时确认如登记，无新代码层缺陷。
- **allowance 门控失败容错**：`populateAllowanceCheck:113-127` catch NopException（Allowance/Expense 科目未配置）→ LOG.warn 跳过门控（告警不阻断，避免阻塞未启用坏账模块的账套）——文档化容错设计，合理 ✅。

**裁决**：⚠️(P1) — 全链路异常规范化扎实（NopException+ErrorCode+上下文），但期间结账编排跨域 command 失败吞咽过宽（P1-MA4-004），需 MR1 收窄（区分"impl 未就绪"与"配置错误/真实故障" + 后者阻断或进异常工作台 + 告警）。

### 2.5 领域「测试有效性」— ⚠️(P1)

**核查项**：异常路径覆盖（核销负路径 / 期间非法迁移 / 坏账核销非 OPEN / 汇兑重估多期）+ 断言强度（凭证行数值 / 辅助账状态翻转 / 年初余额数值）。

**证据**（`TestErpFinPeriodCloseEndToEnd` 1 @Test / `TestErpFinExchangeRevaluation` 2 @Test / `TestErpFinReconciliation` 7 / `TestErpFinBadDebt` 7 / `TestErpFinAnnualClose` 6 / `TestErpFinBudgetEndToEnd` 6 + `TestErpFinAutoReconciliation` / `TestErpFinReconciliationReversePreview` / `TestErpFinBadDebtReversal` / `TestErpFinBadDebtProvisionReversal` / `TestErpFinPeriodStateMachine` / `TestErpFinPeriodPreCheck` / `TestErpFinReverseClose` / `TestErpFinModuleCloseOrder` / `TestErpFinAuxiliaryReconGate` 抽样）：
- **【新缺陷 P1-MA4-005】测试有效性系统性不足**：
  - (a) **期间结账 E2E 仅 1 测试**（`TestErpFinPeriodCloseEndToEnd`）：单黄金路径，无"折旧失败吞咽"（P1-MA4-004）/ 无"auto-post-on-close 阻断分级"（P1-MA2-017）/ 无"模块关账顺序违反"（advanceModule 前置校验）E2E 覆盖。
  - (b) **FX 重估仅 2 测试**：无多期 reversal 场景（P1-MA2-022 累计漂移）/ 无银行存款重估（revalueBankDeposits 零直接测试，全表扫描缺陷 P2-MA4-003(a) 不可见）/ 无外币+本位币混合断言。
  - (c) **核销负路径覆盖薄**：`TestErpFinReconciliation` 7 测试覆盖黄金路径 + reverse，但 `runAutoReconciliation` 三策略（FIFO/BY_AMOUNT/BY_RATIO）的边界（尾差/超额/无候选）主要由 `TestErpFinAutoReconciliation` 单测覆盖，**非幂等场景**（重复 runAutoReconciliation 产生重复核销单，P1-MA2-098）零测试。
  - (d) **坏账核销非 OPEN 异常路径**：`TestErpFinBadDebt` 7 测试覆盖 writeOff/recovery/reverse，但 `requireOpenArApItem` / `requireWrittenOffArApItem` 守卫的负向（对 SETTLED/CANCELLED 项 writeOff 抛错）覆盖薄。
  - (e) **断言强度弱**：年结/损益结转测试多断言凭证存在性 + 合计金额，**未校验行级** `ErpFinVoucherLine.amountSource/amountFunctional/debitAmount/creditAmount` —— 多币种场景（amountSource=amountFunctional，P1-MA3-039）对测试不可见（与 A4.1a P1-MA4-002 同型）。年初余额数值（P1-MA2-018 非累计）无多年结转测试。
- **辅助账状态翻转断言**：`TestErpFinReconciliation` 覆核 OPEN→PARTIAL→SETTLED 翻转 + reverse 回退 ✅（局部断言强度合格）。
- **P1-MA2-022 测试覆盖复核**：如 MA2 登记（FX 无前期 reversal，无多期测试），本审计补充银行存款重估路径同样零测试。

**裁决**：⚠️(P1) — P1-MA4-005（测试有效性系统性不足：E2E 单测 / FX 多期缺 / 非幂等缺 / 行级断言弱）。测试存在性合格但**有效性不足**——核销/坏账单元测试较好，但期间结账/FX/年结 E2E 与异常路径覆盖存在系统性空洞。

### 2.6 领域「可维护性和未来变更风险」— ⚠️(P2)

**核查项**：期间结账编排复杂度 / 坏账 Processor 重复模式 / 预算场景 Processor 对称性 / 性能热点。

**证据**：
- **【P2-MA4-003】可维护性/性能热点合并**：
  - (a) **FX 银行存款重估全表扫描**：`ExchangeRevaluationService.aggregateBankSubjectBookFunctional:236` `lineDao.findAllByQuery(new QueryBean())` 加载**整个 erp_fin_voucher_line 表**后内存过滤 `voucherIds.contains(l.getVoucherId())`——严重性能缺陷（每次 FX 重估全表扫描 voucher line，随历史增长线性恶化）。应改 `q.addFilter(in("voucherId", voucherIds))` 在 DB 过滤。
  - (b) **预算控制余额内存载入**：`ErpFinBudgetControlBiz.aggregateAmount:115` `voucherDao.findAllByQuery(vq)` 载入全部匹配 voucher 实体后 `.map(getId)` 再 `in("voucherId", voucherIds)` 查行——应投影字段或 subQuery，避免载入完整 voucher 实体。
  - (c) **坏账准备 Allowance 余额全局 voucherId 内存载入**：`BadDebtProvisionService.getAllowanceBalance:256` `findPostedVoucherIds()` **无 period 过滤**（全系统所有 posted 非红冲凭证 ID）→ `in("voucherId", voucherIds)` 可能巨大。cumulative 余额需要全历史，但应按 subjectId 直接聚合（SQL sum）而非先载入所有 voucherId。
  - (d) **closePeriod no-op 状态**：`ErpFinAccountingPeriodProcessor.closePeriod:157-158` `period.setStatus(PERIOD_STATUS_CLOSING); period.setStatus(PERIOD_STATUS_CLOSED);`——CLOSING 紧接被 CLOSED 覆盖，CLOSING 无任何可观察效果（无 flush 间隙、无并发可见性），dead code / no-op（与 A4.1a P2-MA4-001(a) no-op catch 块同型）。
  - (e) **precheck load-then-filter-in-Java**：`findUnpostedVoucherCodes:569-572` 载入期间全部 voucher 后 Java stream 过滤 `!POSTED`（应在 query 用 `ne`）；`findUnsettledArApCodes:579-585` 载入日期范围全部 item 后 Java 过滤 status（应在 query 用 `notIn`）。
  - (f) **重复 subject/currency 解析模式散布**：`requireSubject` / `findSubjectByCode` / `resolveFunctionalCurrencyId` / `resolveAcctSchemaId` 在 ProfitLossClosingService / ExchangeRevaluationService / AnnualCloseService / BadDebtProvisionService / ErpFinBadDebtProcessor / ErpFinAccountingPeriodProcessor **6+ 处重复实现**（各 service 自带一份），应提取共享 helper。
- **期间结账编排复杂度**：`ErpFinAccountingPeriodProcessor` 774 行，但已步骤化为 protected 单一职责方法（closeInvModule/closeAssetModule/closeGlModule/closeAnnual/advanceModule/populateTrialBalanceForAllSchemas）——派生覆盖友好，可维护性可接受 ✅。
- **坏账 Processor 系列**：6 个 Processor（SubmitForApproval/Approve/Reject/ReverseApprove/Cancel + 主 ErpFinBadDebtProcessor）状态机迁移校验模式对称（validateTransitionForXxx + doXxx），重复但模式一致 ✅。预算场景 4 Processor 同型对称 ✅。

**裁决**：⚠️(P2) — P2-MA4-003（6 项可维护性/性能热点合并）watch-only，MR2 顺手收敛（FX 全表扫描优先——性能影响最大）。

### 2.7 领域「自动化和防护覆盖」— ⚠️(P2)

**核查项**：预算/AR-AP/成本/期间链路是否有 compliance checker 规则守护 / 是否有测试门控防止回归。

**证据**：
- **compliance checker R2d 覆盖缺口**：`nop-compliance-checker.sh` R2d 扫描 `*Processor.java`/`*Dispatcher.java`/`*Engine.java`——**未覆盖 `*Service`/`*Generator`/`*Checker`/`*Settler`/`*Biz`**。故：
  - `DualSideConsistencyChecker:133,141`（daoFor ErpPurInvoice/ErpSalInvoice）
  - `ErpFinArApItemGenerator`（本域实体，非跨域，不适用）
  - `BadDebtProvisionService` / `ProfitLossClosingService` / `ExchangeRevaluationService` / `AnnualCloseService`（daoFor ErpMdSubject/Currency）
  - `ErpFinBudgetControlBiz` / `ErpFinBudgetCommitmentBizModel`（daoFor ErpMdSubject）
  的 finance→master-data 跨域 daoFor **无静态守卫**（R2d 命中的 *Processor 有覆盖，如 ErpFinBadDebtProcessor:367）。
- **测试门控缺口**：期间结账/FX 多期/年结多年/核销非幂等无回归测试门控（P1-MA4-005）；期间结账编排异常吞咽（P1-MA4-004）无测试门控。
- **状态机/算术模式无静态规则**：核销三策略尾差/对称、坏账三向对称、年结累计余额是运行时行为模式，静态 checker 难以捕获——属测试门控职责。

**裁决**：⚠️(P2) — checker R2d 未覆盖 *Service/*Generator/*Checker/*Settler/*Biz 文件名 + 测试门控缺口，watch-only，MR2 顺手扩展 R2d 文件名模式或补测试门控（与 A4.1a P2-MA4-002 合并裁决）。

---

## 3. P1 finding 清单（按严重性 + 目标 MR 排序）

### P1-MA4-004 期间结账编排跨域 command 异常吞咽致业财悬挂闭环缺失

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——业财不一致悬挂，需跨域集成失败前置 + 非正常路径） |
| 目标 MR | **MR1**（业务正确性：业财悬挂，与 P1-MA2-017/032/048/060 同型根因；R4.1 可裁决） |
| 文件 / 行 | `module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodProcessor.java:344-357`（runDepreciation catch Exception→LOG.warn）+ `:364-378`（recloseInvCosts catch Exception→LOG.warn）+ `:381-396`（reverseDepreciation catch Exception→LOG.warn） |
| 缺陷描述 | 期间结账编排的三处跨域集成（折旧计提 / 存货成本兜底重算 / 反结账折旧冲销）均 `catch (Exception e) { LOG.warn("...跳过（{}）", e.getMessage()); }`——过宽捕获（含 NopException 配置错误如 `ERR_DEPRECIATION_RATE_MISSING` / `ERR_COSTING_*`），仅日志不阻断、不进 `ErpFinPostingException` 异常工作台、不派发 `IErpSysNotificationBiz` 告警。失败后 `closePeriod:157-159` 紧接 setStatus(CLOSED)，期间正常关闭但 GL 缺折旧/成本调整凭证。期末前置检查 `findUnresolvedPostingExceptionKeys:589-602` 仅扫 finance 异常工作台 PENDING/RETRYING，**不覆盖** assets/inventory 域内部失败，间接兜底失效。 |
| 影响 | assets 折旧引擎或 inventory 成本重算因配置错误/基础设施故障失败时，期间带病关闭（GL 缺折旧 → 累计折旧/费用低估；缺成本调整 → COGS/库存漂移），CLOSED_FINAL 后不可补救（需反结账）。比 P1-MA2-032（IGNORED 悬挂）/ P1-MA2-048（salary 吞咽）/ P1-MA2-060（assets tryPost 吞咽）影响面更广——本缺陷在期间结账编排层，一次失败影响整个期间 GL 完整性。非 P0：(1) 需集成失败前置（非正常路径）；(2) LOG.warn 提供运维可见性；(3) 期末试算平衡人工可发现。 |
| 修复方向 | MR1 裁决——方案 A（推荐）：区分"impl 未就绪"（bizObjectManager 解析失败 → 容错跳过）与"配置错误/真实故障"（NopException ErrorCode → 阻断结账或进 ErpFinPostingException 异常工作台 + 派发告警）；catch 收窄为具体异常类型而非 `Exception`；owner doc `period-close.md §步骤2/3` 标注错误传播分级。方案 B：三处集成失败统一进异常工作台（status=PENDING）由期末前置检查兜底阻断。触及会计保护区域，修复须独立 plan-audit + 人工确认。 |

### P1-MA4-005 预算/AR-AP/成本/期间链路测试有效性系统性不足

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——测试空洞致既有 bug 不可见 + 无回归防护） |
| 目标 MR | **MR2**（测试质量，MA4「测试有效性」维度；与 A4.1a P1-MA4-002 + A5.1 测试覆盖深度互补不重叠——本项审异常路径+E2E+断言强度，A5.1 审覆盖深度数值） |
| 文件 / 行 | `test/.../TestErpFinPeriodCloseEndToEnd.java`（1 @Test 单黄金路径）+ `test/.../TestErpFinExchangeRevaluation.java`（2 @Test，无多期/银行存款）+ 全链路（缺异常/重试/非幂等/多年结转测试） |
| 缺陷描述 | (a) 期间结账 E2E 仅 1 测试：无"折旧/成本重算失败吞咽"（P1-MA4-004）/ 无"auto-post-on-close 阻断分级"（P1-MA2-017）/ 无"模块关账顺序违反"E2E 覆盖。(b) FX 重估仅 2 测试：无多期 reversal（P1-MA2-022 累计漂移）/ 无银行存款重估（revalueBankDeposits + 全表扫描 P2-MA4-003(a) 零测试）。(c) 核销非幂等场景（重复 runAutoReconciliation，P1-MA2-098）零测试；三策略边界（尾差/超额/无候选）单元覆盖但 E2E 缺。(d) 坏账核销非 OPEN 负向（对 SETTLED/CANCELLED 项 writeOff）覆盖薄。(e) 断言强度弱：年结/损益结转未校验行级 amountSource/amountFunctional/debitAmount/creditAmount，多币种 bug（P1-MA3-039）对测试不可见；年初余额（P1-MA2-018 非累计）无多年结转测试。 |
| 影响 | P1-MA4-004（异常吞咽）+ P1-MA2-017/018/019/022/098 + P1-MA3-039（多币种折算）均无测试门控；未来结构性变更（如 CloseVoucherWriter 增双金额字段、aggregateBankSubjectBookFunctional 改 DB 过滤）无回归保护。 |
| 修复方向 | MR2——补：(1) 期间结账 E2E 异常路径（折旧失败→阻断/工作台/告警，依赖 P1-MA4-004 修复 + auto-post-on-close 阻断分级 + 模块顺序违反 assertThrows ERR_MODULE_OUT_OF_ORDER）；(2) FX 多期 reversal（P1-MA2-022）+ 银行存款重估（含 amountFunctional 断言）；(3) 核销非幂等（重复 runAutoReconciliation 断言无重复核销单，闭合 P1-MA2-098）；(4) 多年结转年初余额断言（闭合 P1-MA2-018）；(5) 行级金额断言（闭合 P1-MA3-039 测试可见性，与 A4.1a P1-MA4-002 一并）。 |

### P1-MA4-006 预算/AR-AP/成本/期间链路跨域 daoFor 绕 I*Biz（同 P1-MA1-022/P1-MA4-003 根因在 A4.1b 范围投影）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——架构边界违规，read-only 跨域直访绕 I\*Biz 管道） |
| 目标 MR | **MR1**（同 P1-MA1-022 / P1-MA4-003 一并裁决，**不重复计入 MR2**——同根因 master-data/purchase/sales I\*Biz 补便捷只读方法后迁移） |
| 文件 / 行 | `budget/ErpFinBudgetControlBiz.java:191` / `budget/BudgetVoucherGenerator.java:220` / `budget/ErpFinBudgetCommitmentBizModel.java:120,129` / `entity/ErpFinBudgetLineBizModel.java:121` / `profitloss/ProfitLossClosingService.java:173,182,217` / `fx/ExchangeRevaluationService.java:259,268,281` / `annualclose/AnnualCloseService.java:360,389` / `baddebt/BadDebtProvisionService.java:328,365` / `processor/ErpFinBadDebtProcessor.java:367`（finance→master-data 只读 ErpMdSubject/Currency）+ **`reconciliation/DualSideConsistencyChecker.java:133,141`（finance→purchase/sales 只读 ErpPurInvoice/ErpSalInvoice，P1-MA1-022 原枚举未显式覆盖此投影）** + `processor/ErpFinAccountingPeriodProcessor.java:385`（finance→assets 只读 ErpAstDepreciationSchedule = P1-MA1-016） |
| 缺陷描述 | A4.1b 范围多站点跨域 `daoFor(ErpMdSubject/Currency/ErpPurInvoice/ErpSalInvoice/ErpAstDepreciationSchedule)` 只读直访，违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域同型）+ P1-MA4-003（过账链路投影）同根因，本批是其在 budget/arap/baddebt/close/fx/pnl/annual helpers 的投影。**DualSideConsistencyChecker finance→purchase/sales 是 P1-MA1-022 原枚举未显式覆盖的新投影方向**。 |
| 影响 | 架构边界侵蚀（read-only，无活跃数据破坏）；master-data/purchase/sales 实体变更时 finance 预算/核销/结账/坏账直访点不受 I\*Biz 契约保护。 |
| 修复方向 | MR1——同 P1-MA1-022 / P1-MA4-003 方案 A（master-data/purchase/sales I\*Biz 补便捷只读方法后迁移多站点）或方案 B（永久接受为 Helper 合法模式，登记 posting-exemptions.md）。DualSideConsistencyChecker 改用 `IErpPurInvoiceBiz` / `IErpSalInvoiceBiz` 只读 Facade。 |

---

## 4. P2 finding 清单（watch-only）

| Finding ID | 描述 | 处置 |
|---|---|---|
| `P2-MA4-003` | 可维护性/性能热点合并（6 项）：(a) `ExchangeRevaluationService.aggregateBankSubjectBookFunctional:236` `findAllByQuery(new QueryBean())` 全表扫描 erp_fin_voucher_line 后内存过滤 voucherIds（应 `in("voucherId", voucherIds)` DB 过滤——**性能影响最大**）；(b) `ErpFinBudgetControlBiz.aggregateAmount:115` 载入完整 voucher 实体后取 ID（应投影字段或 subQuery）；(c) `BadDebtProvisionService.getAllowanceBalance:256` `findPostedVoucherIds()` 无 period 过滤全局载入 voucherId（应按 subjectId SQL sum 聚合）；(d) `ErpFinAccountingPeriodProcessor.closePeriod:157-158` `setStatus(CLOSING)` 紧接 `setStatus(CLOSED)` no-op（CLOSING 无可观察效果，dead code）；(e) `findUnpostedVoucherCodes:569-572` / `findUnsettledArApCodes:579-585` load-then-filter-in-Java（应在 query 用 ne/notIn）；(f) `requireSubject`/`findSubjectByCode`/`resolveFunctionalCurrencyId`/`resolveAcctSchemaId` 在 ProfitLossClosingService/ExchangeRevaluationService/AnnualCloseService/BadDebtProvisionService/ErpFinBadDebtProcessor/ErpFinAccountingPeriodProcessor 6+ 处重复实现（应提取共享 helper）。 | watch-only，MR2 顺手收敛（(a) FX 全表扫描优先——性能影响最大；与 A4.1a P2-MA4-001/002 合并裁决） |

---

## 5. 与既有 P1 交叉去重

| 本审计 Finding | 既有 Finding | 关系 | 去重裁决 |
|---|---|---|---|
| P1-MA4-004 | P1-MA2-017（auto-post-on-close 阻断分级）/ P1-MA2-032（IGNORED 悬挂）/ P1-MA2-048（salary 吞咽）/ P1-MA2-060（assets tryPost 吞咽） | 同型根因（业财悬挂 + 异常吞咽），但 MA2 审各 dispatcher 的 tryPost 吞咽，本审**期间结账编排层**的跨域 command 错误传播——**不重叠**（不同代码层） | 独立登记 P1-MA4-004（MR1，与 P1-MA2-017/032/048/060 协同修复） |
| P1-MA4-005 | A4.1a P1-MA4-002（过账链路测试有效性）+ A5.1（todo，测试覆盖深度） | 互补不重叠——A4.1a 审过账断言，本审预算/AR-AP/成本/期间 E2E+异常路径，A5.1 审覆盖深度数值 | 独立登记 P1-MA4-005（MR2） |
| P1-MA4-006 | P1-MA1-022（9 域跨域 daoFor）+ P1-MA4-003（过账链路投影） | **同根因在 A4.1b 范围投影**（含 DualSideConsistencyChecker finance→purchase/sales 新方向） | 独立登记但**不重复计入 MR2**（MR1 同 P1-MA1-022/P1-MA4-003 一并裁决） |
| P2-MA4-003(d) | A4.1a P2-MA4-001(a)（no-op catch 块） | 同型（no-op / dead code），不同文件 | 独立 P2 watch-only |
| P2-MA4-003 checker 覆盖 | A4.1a P2-MA4-002（R2d 未覆盖 Resolver/Propagator/Helper） | 同型（checker R2d 文件名覆盖缺口），扩展至 *Service/*Generator/*Checker/*Settler/*Biz | P2 watch-only，MR2 与 P2-MA4-002 合并扩展 |

---

## 6. MA1/MA2/MA3 已知 finding 运行时复核（20 项）

| Finding ID | 运行时状态 | 裁决 |
|---|---|---|
| `P0-MA2-016` | **确认已修复**：`ProfitLossClosingService:88-93` 仅排除 `PERIOD_CLOSE`（:91），`EXCHANGE_GAIN_LOSS` 正常结转至本年利润（注释 :88-89 明示「汇兑重估凭证的汇兑损益须正常结转」） | **修复落地，无新代码层缺陷** |
| `P0-MA2-018` | 如 MA2 登记（alreadyPosted TOCTOU + billR 无 UK；deferred plan 方向 A/B/C/D 维持）；CloseVoucherWriter 经期间 version guard bounded（P1-MA2-087） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA1-016` | 如 MA1 登记（`ErpFinAccountingPeriodProcessor.reverseDepreciation:385` daoFor ErpAstDepreciationSchedule 跨域 DAO finance→assets 只读）；本审计登记 P1-MA4-006 含此站点 | **如 owner doc 声明，无新代码层缺陷**（归 P1-MA4-006 投影） |
| `P1-MA2-017` | 如 MA2 登记（`ErpFinAccountingPeriodProcessor:681-684` auto-post-on-close 默认 false；`PeriodPreCheckReport.hasIssues` 未核销 AR-AP 计入阻断）；**本审计补充发现相邻路径**：跨域集成失败（折旧/成本）在 closeInvModule/closeAssetModule 内吞咽不在 preCheck 范围（P1-MA4-004） | **如 owner doc 声明 + 发现相邻路径新缺陷**（P1-MA4-004） |
| `P1-MA2-018` | 如 MA2 登记（`AnnualCloseService.populateNextYearOpening:148` aggregateYearSubjectActivity 仅本年 :283-307） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-019` | 如 MA2 登记（`assertAuxiliaryReconciles:199-200` sumArApOpenFunctional 无年度过滤 :232-247 vs subjectNetForYear :266-281） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-020` | 如 MA2 登记（`reverseClose:278-281` config kill-switch，无审批流） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-021` | 如 MA2 登记（CLOSED_FINAL 凭证锁定未实现，post/reverse 不校验期间状态） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-022` | 如 MA2 登记（`revalueArAp:106-112` 无期间过滤 + 无前期 reversal）+ **本审计补充发现相邻路径**：`aggregateBankSubjectBookFunctional:236` 全表扫描（P2-MA4-003(a)） | **如 owner doc 声明 + 发现相邻路径性能缺陷**（P2-MA4-003(a)） |
| `P1-MA2-033` | 如 MA2 登记（`generateNextYearPeriods:260-261` 2-12 月 NEVER_OPENED，无 openPeriod action） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-034` | 如 MA2 登记（carryForward 不校验源年度全 CLOSED 前置） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-081` | 如 MA2 登记（部分开票释放语义未声明，release 全额红冲） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-082` | 如 MA2 登记（采购退货/退款未释放承付） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-083` | 如 MA2 登记（AP/AR 发票冲销后 commitment 未恢复） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-084` | 如 MA2 登记（`ErpFinBudgetControlBiz.aggregateAmount:113` actual 通道含 COMMITMENT，等价正确但语义混淆） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-087` | 如 MA2 登记（CloseVoucherWriter 无幂等 pre-check，与 P0-MA2-018 同根因；bounded by period version guard）；FX/PNL/年结/坏账凭证经此路径 | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-096` | 如 MA2 登记（ErpFinGlBalance 无 DB 强制自然键，GL 由过账引擎单线程维护并发风险低） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-098` | 如 MA2 登记（`runAutoReconciliation` 非幂等，无 (pairKey, periodId) UK，重复调用产生重复核销单）；运行时确认 create+post 无去重 | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-024~029/032~038` | 如 MA3 登记（owner doc vs 代码 drift：期间 CLOSED 语义三源冲突 / postingType 三源 / ar-ap-status 命名 / 合并抵消实体命名 / CommitmentAcctDocProvider 矛盾 / reverse REQUIRES_NEW / 多账套 4 键 / 合并抵消 4 键 / reverse-close 审批 / 报销借款 / AR-AP 核销 4 键 / 承付 release-on-receive guard）——配置门控在代码层经各 `isXxxEnabled()` 方法落地，drift 在文档侧 | **如 owner doc 声明，无新代码层缺陷**（drift 归 MR2 文档类） |

---

## 7. 剩余风险与交接

- **P1-MA4-004 修复前**：assets 折旧或 inventory 成本重算失败时期间带病关闭（GL 缺凭证），需运营手工扫日志 + 试算平衡人工发现。MR1 修复后闭环。
- **P1-MA2-022 修复前**：多期 FX 重估累计漂移 + 银行存款重估全表扫描（P2-MA4-003(a)）性能恶化。MR1（FX reversal）+ MR2（全表扫描）修复。
- **交接 A4.1a**：A4.1a 已 done（过账/凭证代码质量），本审计（A4.1b）覆盖 finance 其余功能模块。**finance 代码质量全片（A4.1a + A4.1b）终态在此收口**：6 P1 + 3 P2，零 P0。
- **交接 A5.1**：P1-MA4-005 的测试覆盖深度数值统计（finance 64 测试 / 137 mutation 比 0.47）归 A5.1 系统化；本审计仅审异常路径+E2E+断言强度。
- **交接 A4.6**：finance view.xml vs 后端契约 drift 归 A4.6；本审计不审前端消费。
- **交接 MR1**：P1-MA4-004（业财悬挂，与 P1-MA2-017/032/048/060 协同）+ P1-MA4-006（架构边界，与 P1-MA1-022/P1-MA4-003 协同）。
- **交接 MR2**：P1-MA4-005（测试质量）+ P2-MA4-003（可维护性/性能，与 P2-MA4-001/002 协同）。

## 8. 裁决

**Verdict: FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；TOCTOU 类已登记 deferred / CloseVoucherWriter 经期间 version guard bounded）。预算/AR-AP/成本/期间链路在算术正确性 / 状态机对称性 / 事务边界 / 红冲闭环强一致四面扎实（核销三策略 + ReconciliationSettler 双向对称 + 坏账三向对称 + 聚合公式正确 + @BizMutation 事务回滚），但失败恢复闭环（P1-MA4-004 期间编排异常吞咽）、测试有效性（P1-MA4-005）、架构边界（P1-MA4-006 跨域 daoFor 投影）三项 P1 缺陷需 MR1/MR2 修复。MA1/MA2/MA3 已知 finding 运行时复核 20 项全部「如登记」无升级，其中 P0-MA2-016 复核确认修复落地、P1-MA2-017 复核发现相邻路径新缺陷 P1-MA4-004、P1-MA2-022 复核发现相邻性能缺陷 P2-MA4-003(a)。**与 A4.1a 合并后 finance 代码质量全片终态收口：6 P1 + 3 P2，零 P0**。roadmap A4.1b 推进至 done（待独立 closure audit）。
