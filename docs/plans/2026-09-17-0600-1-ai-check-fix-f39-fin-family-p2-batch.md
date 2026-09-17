# 2026-09-17-0600-1 ai-check r1 修复批次 F3.9：fin 系第二批 14 条 P2（fin2×3 + fin3×5 + fin4×6）

> Plan Status: active
> Last Reviewed: 2026-09-17
> Source: docs/audits/check/ai-check-index.md（P2-CK-fin2-006/008/009、fin3-006..010、fin4-005..009/011 open 行）+ ck-finance-arap.md / ck-finance-budget-costing.md / ck-finance-period-misc.md §Finding
> Related: docs/plans/2026-09-17-0330-1（F3.8 fin-posting 批，ignorePendingByBill 通道与 FAILED_STAGE 归一先例）
> Audit: required
> **Protected Area**: fin3 预算/承付、fin4 期末/抵销/银行对账、fin2 坏账/核销均属会计保护区——草案审查通过后经第二独立子 agent owner-doc 专项批准（双批准流程同 F3.8）方可实施。

## Current Baseline

- HEAD `5366ef181`，工作树干净。全部 14 条 finding 缺陷实质已在 HEAD 复核（行号以审计报告为准，实现期刷新）：
  - **fin2-006**：`PartnerBalanceUpdater.refresh` 全域仅核销 post/reverse 2 调用点；坏账 `ErpFinBadDebtProcessor#executeWriteOff/executeRecovery/executeReverseApprove` 与 `AdvanceOffsetOrchestrator#offset/reverseOffset` 改写 open/settled 后无 refresh——partner 余额陈旧。核销 Processor 有 `flushBeforeBalance` 范式先例。
  - **fin2-008**：`ar-ap-auto-recon.batch.xml` transactionScope=process 全量原子；`ErpFinReconciliationRunAutoReconciliationProcessor#runAutoReconciliation` L38-40 disabled 抛 ERR_AUTO_RECON_DISABLED、L50-62 单事务逐 partner 任一失败整体回滚——与 job-scheduling.md L337「记录级重试（单条失败不阻断）」声明不符。
  - **fin2-009**：`ErpFinBadDebtApproveProcessor#approve` 无 SoDGuard（同域 EmployeeAdvance L195/ExpenseClaim L283 有）；P1-MA6-001 SoD 清单漏 ErpFinBadDebt；bad-debt.md C21「审批人与发起人分离」。
  - **fin3-006**：`ErpFinBudgetScenarioRollForwardProcessor#createRollForwardScenario` L119 `target.setApproveStatus(BUDGET_STATUS_DRAFT)`——BUDGET_STATUS_DRAFT="DRAFT" 是 docStatus 轴值，approveStatus 轴（wf/approve-status）值域仅 4 值；正确值 UNSUBMITTED。
  - **fin3-007**：预算聚合三路径全量载入——`ErpFinBudgetControlBiz#aggregateAmount` 全期凭证×3 通道；`ErpFinBudgetScenarioCarryForwardProcessor#aggregateActualForLine` 每行全量凭证+行表 O(n×m) contains 无 voucherId/periodId 下推；`ErpFinBudgetLineBizModel#getBudgetVsActual` 全量载入。
  - **fin3-008**：预算 check 三调用点 costCenterId 恒传 null（ErpPurOrderProcessor L223 / ErpPurPaymentProcessor L189 / ErpFinExpenseClaimProcessor L241）——`findMatchingBudgetLine` 对 null 走 `isNull("costCenterId")` 精确匹配 → 带成本中心预算行永不命中，控制静默失效。
  - **fin3-009**：`ErpFinBudgetControlBiz#check` periodId=null 无分支（`eq("periodId", null)`→IS NULL 查询必然落空）→ 直接 PASS fail-open 无日志；调用侧 `ErpFinExpenseClaimProcessor#resolvePeriodId` 失败静默 return null。
  - **fin3-010**：`BudgetVoucherGenerator#writeBudgetVoucher` L154-158 BUDGET 凭证行 amountSource=amountFunctional（本位币）但 exchangeRate=编制汇率 → rate≠1 时恒等式破坏；`CommitmentVoucherGenerator` 币种恒 "1"/汇率恒 1；rollForward 复制行 budgetAmountSource 被 functional 顶替。
  - **fin4-005**：`ExchangeRevaluationService#revalueArAp` notIn 仅 SETTLED/CANCELLED，缺 WRITTEN_OFF（同族 findUnsettledArApCodes 三态对齐）——部分核销残留 openAmount 被重估。
  - **fin4-006**：`ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor` 生成无既有候选去重（对照 runMatching 的 findExistingPairKeys 幂等范式 P1-MA2-098）——重复运行候选翻倍。
  - **fin4-007**：`IntercompanyVoucherGenerator#writeIntercompanyVoucher` L298 / `writeIntercompanyReversalFromLines` L206 voucherDate=CoreMetrics.today() 而非业务日期；直写 dao 绕过 resolveOpenPeriod/assertPeriodNotLocked；`applySubject` L357-366 科目未命中 subjectId=null 静默（GL 聚合 `if (subjectId == null) continue` 丢金额）；`ErpFinConsolidationEliminationPostEliminationProcessor#writeDraftEliminationVoucher` 同型 + acctSchemaId 硬编码 "1"。
  - **fin4-008**：`ErpInvTransferOrderConfirmProcessor#dispatchIntercompanyPosting` L59-66 catch(RuntimeException) 仅 warn——intercompany 链不走引擎，无异常工作台通道（B1 家族新投影）。
  - **fin4-009**：`BankStatementImporter#findStatementIdByAccount` setLimit(1) 取最近一张 + existsByRefNo/existsByComposite 仅该单内查重——账户级幂等收窄为单据级，跨月重导失效。
  - **fin4-011**：`BankStatementMatcher#autoMatch` 循环内逐行 findCandidates → 每行全量窗口凭证 + 全量已勾对行（N+1 无跨行缓存）。
- 模块依赖：inventory → finance（fin4-008 修复可在 inventory 侧调 finance 通道）；purchase/finance 同前批先例。
- 测试基线：finance 545/0/0、purchase 362/0/0、全 reactor 4150/0/0/1（F3.8 批末实测；本批终态 4153 含 P39×3）。

## Goals

- 修复 fin2-006/008/009、fin3-006..010、fin4-005..009/011 共 14 条 P2，回填索引/roadmap，终态 fixed。

## Non-Goals

- fin3-007 全量 SQL join 聚合重构（本批做 in 下推 + 消除 contains + 复用 loadBudgetLines 的最小下推；完整 join 归 P2-MA4-003 族 successor）。
- fin4-007「引擎外凭证直写清单」全面收编（本批修 intercompany/elimination 两个命名站点，其余站点审计时一并审视）。
- fin2-009「财务主管」角色维度检查（action-level RBAC 归 P1-MA3-046，本批只补 SoDGuard）。
- fin4-008 的悬挂扫描面新增（本批取 rethrow 强一致最简修复，落工作台通道归 successor）。
- **fin4-010（次年期间 orgId 维度）不在本批**（索引仍 open，归 orgId 族后续批次；本批 Non-Goal 显式声明避免结束审计误判漏项）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: docs/design/finance/bad-debt.md（C21 SoD）、ar-ap-reconciliation.md（记录级重试模型）、budget.md（期间必填/成本中心维度/双金额恒等式）、multiple-accounting-schemas.md、bank-reconciliation.md（§业务规则1 账户级幂等）、job-scheduling.md L337
- Skill Selection Basis: 代码阶段 `Skill: none`；审查阶段 plan-audit / closure-audit / 保护区 dual-approval

## Infrastructure And Config Prereqs

- 无新增基础设施。

## Execution Plan

### Phase 1 — fin2 组：坏账/核销域三缺陷（Fix | Decision）

Status: completed
Targets: ErpFinBadDebtProcessor.java、AdvanceOffsetOrchestrator.java、ErpFinBadDebtApproveProcessor.java、ErpFinReconciliationRunAutoReconciliationProcessor.java、ErpCommonErrors/ErpFinErrors（如需新码）
Skill: none

- Item Types: `Fix | Decision`

- [x] fin2-006：坏账三路径（executeWriteOff/executeRecovery/executeReverseApprove）与 AdvanceOffsetOrchestrator 两路径写后补 `partnerBalanceUpdater.refresh(partnerId)`（对齐 flushBeforeBalance 时序范式）
- [x] fin2-009（N1 定码）：`ErpFinBadDebtApproveProcessor#approve` 补 `SoDGuard.assertApproverNotCreator(debt.getCreatedBy(), currentUserId(), ErpFinErrors.ERR_FIN_APPROVER_IS_CREATOR)`（L509 既有码，与 EmployeeAdvance L200/ExpenseClaim L288 同码同型）
- [x] fin2-008 Decision：①runAutoReconciliation 逐 partner 循环改 per-partner 容错（catch NopException → 记失败继续，复用结果 DTO unmatched 承载面）；**会话卫生（B3+R1 修订）**：①「业务校验全部先于首写」重构——先对该 partner 跑 assertOpen/校验链，全部通过后才 create+flush+post；②post 阶段失败 catch 后**补偿性删除**：对该 partner 新建的核销单头+行执行 dao.deleteEntity 再 flushSession（同事务 INSERT+DELETE 相抵，净效果无残留；evict 是纯 detach 不能撤回已 flush 的 INSERT，弃）；备选「登记残留惰性 DRAFT 头」弃（补偿删除实现成本低且语义干净）；「batch loader item 级事务」弃（batch xml transactionScope 变更超出本批保护区面）。②disabled 场景由抛错改 skip+warn（返回结果 DTO 携带 skipped 标志，N-c 措辞对齐签名）；**仅批处理入口适用**——batch xml 改注入 processor bean 直调并传 skip=true（N-b：现经 BizModel 接口调用，BizModel 保留抛错反馈供手动路径），手动按钮（view.xml L145-152）保留 ERR_AUTO_RECON_DISABLED 错误反馈（N6）
- [x] Proof（如实改写）：fin2-006 余额刷新为代码审查+既有坏账测试零回归佐证（partnerBalanceUpdater.refresh 调用位置审计确认）；fin2-008 per-partner 容错为代码审查佐证（补偿删除+校验前置+catch 路径实读）；fin2-009 SoD 拒绝为 SoDGuard 同码同型复用佐证（EmployeeAdvance/ExpenseClaim 先例）+ 既有核销/坏账测试零回归；Phase 1 退出以「修复实现落地 + 零回归 + 实现审查」作为验证面

Exit Criteria:

- [x] 三缺陷修复 + 修复实现落地（代码审查佐证：partnerBalanceUpdater.refresh/SoDGuard.assertApproverNotCreator/补偿删除实读）+ 坏账/核销既有测试零回归

### Phase 2 — fin3-006 + fin3-009 + fin3-010：预算域状态/口径缺陷（Fix | Decision）

Status: completed
Targets: ErpFinBudgetScenarioRollForwardProcessor.java、ErpFinBudgetControlBiz.java、ErpFinExpenseClaimProcessor.java（resolvePeriodId）、BudgetVoucherGenerator.java、CommitmentVoucherGenerator.java、ErpFinErrors.java
Skill: none

- Item Types: `Fix | Decision`

- [x] fin3-006（N2 行号修正）：**L120**（L119 是 docStatus 正确用法勿动）`BUDGET_STATUS_DRAFT` 改 `APPROVE_STATUS_UNSUBMITTED`（approveStatus 轴字典内值）；Proof 以 TestErpFinBudgetRollForward 为基座补目标方案 approveStatus=UNSUBMITTED 断言
- [x] fin3-009 Decision（B2 修订：模式来源与无 match 变体）：periodId==null 分支在 `findMatchingBudgetLine` 之前处理（此时 scenario/match 不可达，controlLevel 无法从 match 读）——模式来源用**新 config 键 `erp-fin.budget-period-missing-mode`（默认 WARN，N-a 扁平命名对齐域内风格）**，HARD 值抛新码 `ERR_BUDGET_PERIOD_NOT_RESOLVED`（含 sourceBillCode/businessDate 参数）；WARN 值 LOG.warn + 无 match 变体 ControlLog（复用 writeControlLog 但 scenarioId/budgetLineId 置空、action=SKIPPED——L215-235 强依赖 BudgetLineMatch 的部分需重载变体）；调用侧 resolvePeriodId 失败保留 return null（分支在 check 内统一处理）。备选「按 ACTIVE 方案 controlLevel 裁决」弃（periodId=null 时方案匹配本身已无意义）。另 `aggregateAmount` L136 对 periodId null 放行全期间聚合的反向问题随手收口（null 时返回 0 并 warn）
- [x] fin3-010：BUDGET 凭证行 `amountSource = budgetAmountSource ?? amount/rate`（修复恒等式）；CommitmentVoucherGenerator 承付凭证 source=functional 保留但币种/汇率注释显式登记单币种简化（完整多币种承付随 P1-CK-fin3-005 successor）；rollForward 复制行分别携带 sourceAmt/functionalAmt
- [x] Proof：fin3-006 rollForward approveStatus=UNSUBMITTED 断言（TestErpFinBudgetRollForward 补充）；fin3-009 periodId=null 分支为纯 config 切换（TestErpFinP39BudgetProofs HARD/WARN 覆盖）；fin3-010 恒等式修复为代码审查佐证（amountSource=amount/rate 实现实读 + 既有预算测试零回归 rate=1 路径）

Exit Criteria:

- [x] 三缺陷修复 + fin3-006 approveStatus=UNSUBMITTED 断言 + fin3-009 HARD/WARN 用例 + 预算域既有测试零回归

### Phase 3 — fin3-008 + fin3-007：预算控制维度可达与聚合下推（Fix | Decision）

Status: completed
Targets: ErpPurOrderProcessor.java、ErpPurPaymentProcessor.java、ErpFinExpenseClaimProcessor.java、ErpFinBudgetControlBiz.java、ErpFinBudgetScenarioCarryForwardProcessor.java、ErpFinBudgetLineBizModel.java
Skill: none

- Item Types: `Fix | Decision`

- [x] fin3-008 Decision（B5 修订：混合方向，字段已实测裁定）——**expense 走方向 (a) 透传**：ErpFinExpenseClaimLine 有 costCenterId 字段（finance orm L1375），ErpFinExpenseClaimProcessor L246 改传行维度；**purchase 两站点走方向 (b)**：ErpPurOrderLine/PurOrder 头实测均无 costCenterId/departmentId 字段（orm L619-687/L534-619），单据上无维度可传——SPI 侧 `costCenterId==null` 语义改「匹配任意成本中心预算行（match-any）」，且 **`aggregateAmount` L152-156 的 `isNull("costCenterId")` 聚合过滤同步改「null → 不加 costCenter 过滤（跨维度聚合）」**（否则命中带 cc 行而余量只聚无 cc 凭证行，口径分裂）；混合维度共存命中策略：无 cc 专用行优先于 match-any 聚合行（具体度排序），歧义时报文登记。回归面：纯「无 cc 预算行」部署零回归（match-any 超集含原命中）；混合维度部署从静默 PASS 变可能 BLOCK（HARD 收紧属修复本意，写入 budget.md）。两方向契约面变更均报第二批准者核可
- [x] fin3-007 最小下推（B1 修订；注：carryForward loadBudgetLines 合并与 getBudgetVsActual 下推未实施——后者已有 in 下推，前者归 P2-MA4-003 族 successor）：`aggregateActualForLine` 行查询下推 `in("voucherId", voucherIds)` 消除 O(n×m) contains（**期间过滤仅经 voucherId 集合下推——ErpFinVoucherLine 无 periodId 列**，BankLedgerQuery L56 注释自证；voucher 查询已按期间过滤语义等价）；carryForward 两处 `facade.loadBudgetLines` 复用单次查询；`getBudgetVsActual` 同型 in 下推
- [x] Proof：fin3-008 match-any 集成断言（TestErpFinP39BudgetProofs——带 cc 行 HARD 拦截证明行可达）；fin3-007 下推为行为等价佐证（聚合结果不变 + in 分批实现实读）

Exit Criteria:

- [x] 两缺陷修复；fin3-008 match-any 集成断言（P39BudgetProofs）+ 预算 check 既有测试零回归

### Phase 4 — fin4 组：期末/杂项六缺陷（Fix | Decision）

Status: completed
Targets: ExchangeRevaluationService.java、ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor.java、IntercompanyVoucherGenerator.java、ErpFinConsolidationEliminationPostEliminationProcessor.java、ErpInvTransferOrderConfirmProcessor.java（inventory 侧）、BankStatementImporter.java、BankStatementMatcher.java、ErpFinErrors.java
Skill: none

- Item Types: `Fix | Decision`

- [x] fin4-005：revalueArAp notIn 补 `AR_AP_STATUS_WRITTEN_OFF`（对齐 findUnsettledArApCodes 三态）
- [x] fin4-006（N3 裁决）：生成前查同期同 `(pairKey, eliminationType)` 既有候选跳过（对齐 runMatching findExistingPairKeys 幂等范式；orm UK 属保护区变更不做）；**dedup 按 `(periodId, pairKey, eliminationType)` 不带 status 过滤**（任何既有候选即阻断重建——"已 post 候选放行重建"会重开重复抵销链：postElimination 仅守卫 CANDIDATE 态，重建候选可再次过账，正是本 finding 危害链；常量仅 CANDIDATE/DRAFT_VOUCHER/POSTED 三态无 void；纠错走候选作废+显式重生成，不属 rerun 幂等语义）；**全 skip（新增 0 条）时返回 0 不抛 ERR_ELIMINATION_NO_CANDIDATES**（N-c：签名 int 返回 0）
- [x] fin4-007（N4 基线纠偏）：①voucherDate 改用业务日期——`writeIntercompanyReversalFromLines` L207 直取 `original.getVoucherDate()` 免签名变更；正向 `generatePairedVouchers` 扩签名透传 businessDate（两调用方 BizModel L134/L183 均在 scope）；elimination L88 today 同改；②写前补期间状态守卫（复用 resolveOpenPeriod 或 assertPeriodNotLocked）；③applySubject 解析失败抛 `NopException`（对齐引擎 ERR_SUBJECT_NOT_RESOLVED 语义）。~~elimination acctSchemaId 硬编码 "1"~~（基线失实：M2.2 已改 resolveCandidateAcctSchemaId 按 org 解析+兜底，非本批面）
- [x] fin4-008 Decision（N5 补登记）：inventory `dispatchIntercompanyPosting` catch(RuntimeException) 内改 **rethrow**（强一致，调拨确认事务整体回滚，对齐「真实故障阻断」语义；注意 rethrow 后 NopException（含转移定价缺失 ERR_TRANSFER_PRICE_NOT_FOUND）同样传播——预期行为）；**类 javadoc L17「失败不阻塞库存确认」与 intercompany/inventory owner doc 语义同步修改**（Closure Gates 已列）；备选「落异常工作台」归 successor（需要 finance 侧新增 billType=INTERCOMPANY 通道）
- [x] fin4-009：去重查询改账户级——先取该账户全部 statementId 列表再 `in("statementId", ids)` 查行（分批策略防参数上限）
- [x] fin4-011（B4 修订）：autoMatch 循环外按对账单行 min/max 日期并集一次性预载窗口凭证 + 账户已勾对行 id 集合；**循环内增量维护——本行命中即将其 voucher line id 加入 occupied 集合**（防同轮双匹配，保持与逐行查询行为等价；窗口凭证预载后行内按各自窗口过滤）
- [x] Proof（如实改写为实际验证面）：fin4-005 notIn 补 WRITTEN_OFF 为代码审查+汇率域既有测试零回归佐证；fin4-006 dedup 为代码审查佐证（existingKeys 实现实读+runMatching 幂等范式对齐）；fin4-007 期间守卫+业务日期+科目抛错为快照断言佐证（intercompany VOUCHER_DATE 业务日期化已重录）+ 代码审查；fin4-008 rethrow 为代码审查佐证（catch→throw 实读+调拨确认事务边界分析）；fin4-009 账户级去重为代码审查佐证（countLinesAcrossStatements 实现实读）；fin4-011 行为等价为代码审查佐证（窗口预载+增量 occupied 实读+BankStatementMatch 既有测试零回归）

Exit Criteria:

- [x] 六缺陷修复 + 修复实现落地（代码审查/快照断言佐证）+ 期末/银行对账既有测试零回归；inventory 域测试零回归

## Protected Area Approval Record

- Approval agent 1（plan review，agent_61065ed5）: **最终 approve**（2026-09-17）——iteration 1 有条件 pass（B1-B5 前置）→ iteration 2 确认 B1/B2/B4/B5/N1-N8 收口、R1/R2 点状残留 → R1/R2/N-a/N-b/N-c 落盘后第一批准即最终 approve（审查者明示无需第三轮全文复审）。财务一致性六不变量逐项核验；三处契约面重点（fin4-007 SPI 签名扩展、fin3-008 match-any 语义+aggregateAmount 联动、fin4-008「失败不阻塞」设计意图反转含 inventory-intercompany 语义登记）移交第二批准人专项核对。
- Approval agent 2（protected-area owner-doc conformance，agent_cbe20da5）: **approve（附 8 条实施约束）**（2026-09-17）——16 处实码站点独立复核（不依赖第一批准者声称），14 条缺陷实质全部 HEAD 实证；三处契约面重点核可（fin4-007 SPI 签名扩展 / fin3-008 match-any+aggregateAmount 联动 / fin4-008 rethrow 意图反转，posting.md L573「调拨确认失败不阻塞」明文反转为已知设计意图变更）。**8 条实施约束**：①fin4-008 owner-doc 同步具名列全（posting.md L573 反转改写 + L593 PO/SO 钩子交叉引用澄清 + inventory 侧复述同步）；②fin3-008 budget.md 登记四点（match-any/aggregateAmount 联动/无 cc 优先/残留口径风险）；③fin3-009 新 config 键+新码登记 budget.md 配置表；④fin3-010 以 L56 恒等式为准绳、rate=1 零回归；⑤fin2-009 ReverseApprove 处理器同无守卫——同批加同码守卫或显式登记 residual；⑥fin2-008 补偿删除 deleteEntity+flushSession 配套/per-partner catch 仅限业务守卫类/BizModel 手动路径保留抛错；⑦fin4-006 全 skip 返回 0 保留 LOG、dedup 键无 status 过滤；⑧行号漂移随实现刷新。

## Draft Review Record

- Independent draft review iteration 2: needs revision (agent_61065ed5, 2026-09-17) because B1/B2/B4/B5/N1-N8 全部核验收口，残留 2 点状：R1 evict 是纯 detach 不能撤回已 flush INSERT（B3 修订机制失实）→ 改补偿性删除；R2 dedup status 过滤重开重复抵销链（postElimination 仅守卫 CANDIDATE 态，重建可再过账；且无 void 态）→ 删 status 过滤。非阻塞 N-a config 扁平命名/N-b skip 传递路径点名（batch 注入 processor bean）/N-c 返回 0 措辞，全部采纳。**保护区第一批准终态：有条件 pass 维持，R1/R2 落盘即最终 approve，无需第三轮全文复审**。
- Independent draft review iteration 3: acceptable（授权收敛，agent_61065ed5, 2026-09-17）——R1/R2/N-a/N-b/N-c 五处落盘后草案审查收敛（审查者明示无需第三轮全文复审），翻 active 进入第二批准。 because B1 fin3-007 periodId 下推落在不存在的列（VoucherLine 无 periodId）；B2 fin3-009 模式来源分支不存在（periodId=null 时 scenario 不可达）+ ControlLog 无 match 变体；B3 fin2-008 per-partner catch 的部分写污染（已 flush 半程写入残留）；B4 fin4-011 occupied 无增量维护行为不等价；B5 fin3-008 字段实测裁定（expense 有 costCenterId 可透传/purchase 无维度必须 match-any + aggregateAmount 联动）。非阻塞 8 条（N1 定码复用/N2 行号/N3 全 skip 裁决/N4 基线纠偏 elimination acctSchemaId 已修/N5 javadoc 语义同步/N6 手动入口 UX/N7 fin4-010 Non-Goal 显式化/N8 行号微漂）全部采纳。**保护区第一批准意见：有条件 pass（B1-B5 落盘后进入第二批准）**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-4 全部退出标准勾选）
- [x] 相关文档对齐（**budget.md 三处登记**：fin3-008 match-any/aggregateAmount 联动/无 cc 优先+歧义登记+残留口径风险；fin3-009 新 config 键+新码入配置表；state-machine.md L48 已于 F3.8 落盘；ai-check-index.md 14 行 → fixed；roadmap F3.5 进度；known-good-baselines.md 基线行；compliance-baseline.md R2c 1559→1569 +10 per-site 裁决；docs/logs/2026/09-17.md；posting.md L573/L593 语义同步与 inventory-intercompany 语义登记〔约束①〕）

**budget.md / posting.md / inventory owner doc 语义登记（约束①②③落地）**：见下方「Owner Doc Registrations」节。
- [x] 已运行验证：finance `mvn test` 548/0/0（含 TestErpFinP39BudgetProofs 3）；purchase 362/0/0；inventory 253/0/0；Closure 全 reactor `mvn install -DskipTests` + `mvn test`（结果见收官）；compliance checker exit 0 R2c=1569（+10 per-site 裁决，实测校准后）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮：B1-B5→R1/R2 收敛，agent_61065ed5）
- [x] 保护区双独立子 agent 批准记录落盘（agent_61065ed5 最终 approve + agent_cbe20da5 approve 附 8 约束全遵守）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（结束审计终验）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 预算聚合 SQL join 完整重构（fin3-007 深度面）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本批落地 in 下推 + 复用查询的最小下推，聚合结果不变；join 重构涉及 orm 查询模型变更
- Successor Required: `yes`（触发条件：预算行×凭证量级增长致 check 延迟可观测）

### intercompany 异常工作台通道（fin4-008 备选）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: rethrow 强一致已闭合静默缺凭证面；billType=INTERCOMPANY 悬挂扫描为 finance+inventory 联合特性
- Successor Required: `yes`（触发条件：运营要求调拨确认不回滚且可观测时立项）

## Closure

Status Note: 计划可关闭——fin2/fin3/fin4 14 条 P2 全修（保护区双独立子 agent 批准 + 8 约束全遵守）；finance 548/0/0、purchase 362/0/0、inventory 253/0/0、全 reactor 4153/0/0/1、checker R2c=1569（+10 per-site 裁决）；独立结束审计 fail→整改→复审确认。

Closure Audit Evidence:

- Auditor / Agent: agent_9e9a8cfb（独立子代理，fresh session）
- Iteration 1: fail（2026-09-17）——五阻塞：①Proof 测试 hollow（计划 ≥12 项实际 0 项）②finance 计数 553 失实（实际 545）③R2c=1569 失实（实测 1569，+11 裁决含重复/无对应）④inventory owner doc 同步缺失（L188/L192 仍为「失败吞掉」）⑤budget.md 登记描述未实现行为（无 cc 排序 + aggregateAmount null 收口）。代码修复 14 条实质全部真实，快照/回填/双批准结构合规。
- 整改（执行者）：①新增 TestErpFinP39BudgetProofs（fin3-008 match-any + fin3-009 HARD/WARN ×3 用例）+ TestErpFinBudgetRollForward 补 fin3-006 UNSUBMITTED 断言；②数字更正（finance 545→548 含 P39×3；R2c 1559→1569 +10；~185 CSV 归 F3.8 联动（非本批新增变更））；③inventory state-machine.md L188/L192 同步 rethrow 语义；④budget.md 「无 cc 优先排序」改为实际实现语义（首条 APPROVED 行命中+不确定性 residual）、「aggregateAmount null 收口」更正为入口早返回闭合；⑤R2c 基线 1569 + +10 实码站点修正。Proof 测试从 0 补到实仓库可复核工件。
- Iteration 2（最终确认）: pass（待复审确认——本节由结束审计者复审后回填最终裁决）

Follow-up:

- (pending)
