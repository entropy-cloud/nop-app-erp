# MA2 质量状态机审查（A2.12）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：quality / 质检单 + NCR + CAPA + 召回 + SPC + 抽样 + 校准 + 风险 + 目标 + 评审（16 状态字段）
> 审计 plan：`docs/plans/2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine.md`
> 行为基线：`docs/design/quality/{state-machine,recall,spc,inspection-integration}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 范围：16 状态字段（plan baseline），实际审查经逐文件全文阅读 + grep 验证
> 审计执行：2026-07-28
> 上游基线：MA1 done（P1-MA1-012 propId + P1-MA1-022 跨域只读 9 域合并含 qa 已登记，本审计复核状态机角度）；A2.1 P2P done（强制质检阻塞来料检验 + NCR 退货编排）；A2.2 O2C done（强制质检阻塞销售出库）；A2.6a done（完工检验返工反馈制造域）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞异常悬挂同型范式）；A2.11 done（inventory NCR 过账 + posted 三件套同型范式）

## 1. 审查范围与状态字段清单

| 实体 / 组件 | 状态轴（dict） | 实现文件 | 审查方式 |
|------------|---------------|----------|---------|
| **ErpQaInspection**（质检单 4 态） | `result`(erp-qa/inspection-result) + `inspectionType`(erp-qa/inspection-type) + `posted`/`postedAt`/`postedBy` 三件套 + `approveStatus`(wf/approve-status) | `ErpQaInspectionBizModel.java`（307 行）+ `IErpQaInspectionBiz` + `InspectionResultEvaluator.java` + `InspectionTrigger.java`（强制质检门控） | 全文逐行 |
| **ErpQaInspectionLine**（质检单行） | `result`(erp-qa/inspection-result 复用) | `ErpQaInspectionLineBizModel.java`（18 行 CRUD 桩）+ 经 `ErpQaInspectionBizModel.recordResult` 行级评测 writer | 全文逐行 |
| **ErpQaNonConformance**（NCR 5 态） | `status`(erp-qa/ncr-status) + `disposition`(erp-qa/disposition-type) + `approveStatus`(wf/approve-status) + `posted`/`postedAt`/`postedBy` 三件套 + `returnCode` | `ErpQaNonConformanceBizModel.java`（242 行）+ `NcrLifecycleService.java`（151 行 resolve 门控 + 自动 NCR） | 全文逐行 |
| **ErpQaAction**（CAPA 4 态） | `status`(erp-qa/action-status) + `actionType`(erp-qa/action-type) | `ErpQaActionBizModel.java`（101 行 PENDING→IN_PROGRESS→COMPLETED + verifyAction） | 全文逐行 |
| **ErpQaRecall**（召回 5 态） | `status`(erp-qa/recall-status) + `approveStatus`(wf/approve-status) + `triggerType` + `severityLevel` | `ErpQaRecallBizModel.java`（351 行）+ `ErpQaRecallProcessor.java`（195 行标准审批）+ 5 个 per-mutation Processor（薄包装）+ `RecallTargetLocator.java` | 全文逐行 |
| **ErpQaRecallTarget** | `returnStatus`(erp-qa/recall-target-return-status) | `ErpQaRecallTargetBizModel.java`（22 行 CRUD 桩）+ 经 `ErpQaRecallBizModel.notifyCustomers/generateReturns` writer | 全文逐行 |
| **ErpQaSpcChart**（SPC 控制图） | `calcStatus`(erp-qa/spc-calc-status) + `isActive` + `chartType` + `clCenterType` + `docStatus` + `approveStatus` | `ErpQaSpcChartBizModel.java`（87 行 collect/recalc/evaluateRules 委托 spc/* 服务）+ `SpcControlLimitCalculator.java`（writer） | 全文逐行 |
| **ErpQaCalibration**（量具校准） | `result`(erp-qa/inspection-result 复用) + `docStatus` + `approveStatus` | `ErpQaCalibrationBizModel.java`（18 行 CRUD 桩，无任何 setStatus/result writer） | 全文逐行 |
| **ErpQaRiskRegister**（风险登记） | `status`(erp-qa/risk-status OPEN/MITIGATED/CLOSED) | `ErpQaRiskRegisterBizModel.java`（15 行 CRUD 桩）+ `SpcCapabilityCalculator:308` 仅写 OPEN | 全文逐行 |
| **ErpQaQualityGoal**（质量目标） | `status`(erp-qa/goal-status ACTIVE/ACHIEVED/FAILED/CANCELLED) | `ErpQaQualityGoalBizModel.java`（18 行 CRUD 桩，零 setStatus writer） | 全文逐行 |
| **ErpQaReview**（质量评审） | `docStatus`(erp/doc-status) + `approveStatus` + `reviewType` | `ErpQaReviewBizModel.java`（18 行 CRUD 桩，零 setStatus writer） | 全文逐行 |
| **ErpQaSamplingPlan**（抽样方案） | 无 status 列（仅 stdStatus 字段） | `ErpQaSamplingPlanBizModel.java`（15 行 CRUD 桩） | 全文逐行 |
| **NCR 过账副作用** | — | `NcrPostingDispatcher.java` + `NcrPostingExecutor.java`（IErpFinVoucherBiz Facade）+ `NcrScrapAcctDocProvider.java` + `NcrReturnOrchestrator.java`（IErpPurReturnBiz/IErpSalReturnBiz Facade） | 全文逐行 |
| **强制质检门控** | — | `InspectionTrigger.java`（enforceGate 同步 I*Biz 写触发） | 全文逐行 |

16 状态字段分布在 11 个状态承载实体（含复用字典）+ NCR 过账/触发助手（与 plan baseline 一致 ✓）。

## 2. 10 维度审查

### 2.1 维度「状态定义」

**裁决：PASS（含 Deferred CRUD 空壳清晰性注记）」

#### 质检单 result（erp-qa/inspection-result PENDING/ACCEPTED/CONDITIONAL/REJECTED 4 态）

✅ **每个状态表达「等待什么」**（owner doc `state-machine.md §1` 表）：PENDING=等待检验；ACCEPTED/CONDITIONAL/REJECTED 是终态（业务单据影响：阻塞/继续/退货返工）。dict option 与 `ErpQaConstants.INSPECTION_RESULT_*` 常量 1:1 对齐。

✅ **终态语义清晰**：三个终态均无出边（owner doc §3 强制；recordResult:65-67 守卫 PENDING 单一入口）。

#### NCR status（erp-qa/ncr-status OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED 5 态）

✅ **每个状态表达业务等待点**（owner doc §适用对象二表）：OPEN=等待评审；IN_REVIEW=正在分析+制定 CAPA；RESOLVED/ESCALATED_TO_RECALL/CANCELLED 是终态。`ESCALATED_TO_RECALL` 终态指向 `recall.md`（owner doc 显式声明「不走 RESOLVED 路线」）。

✅ **`ESCALATED_TO_RECALL` 终态清晰**：`ErpQaNonConformanceBizModel.escalateToRecall:140-147`（仅状态迁移占位）+ `upgradeToRecall:149-172`（状态迁移 + 建召回实体经 `recallBiz.register` Facade）两入口，前者用于状态标记，后者用于实际升级。两入口均守卫 `IN_REVIEW` 单一源态。

#### 召回 status（erp-qa/recall-status OPEN/APPROVED/IN_PROGRESS/CLOSED/CANCELLED 5 态）

✅ **每个状态表达业务等待点**（`recall.md §状态机`）：OPEN=等待审批；APPROVED=已审批等待执行；IN_PROGRESS=执行中（定位/通知/退货编排）；CLOSED/CANCELLED 终态。

⚠️ **APPROVED 态语义复合**：APPROVED 既表达「审批通过」也作为「等待执行 locateTargets」的中间态。`ErpQaRecallBizModel.locateTargets:119-128` 守卫 APPROVED 单一源态，APPROVED→IN_PROGRESS 迁移落实。owner doc `recall.md` ASCII 图明示此设计（APPROVED 后才执行目标定位与通知）✓。

#### 让步审批简化（CONDITIONAL）

✅ **owner doc §实现偏离补注 让步审批简化**：设计 §2 让步 CONDITIONAL 需「让步审批」；实现以 `approveStatus=APPROVED` 简化。`ErpQaInspectionBizModel.recordResult:87-91` 当 `allowConcession=true` 且汇总为 CONDITIONAL 时设 `approveStatus=APPROVED + approvedBy/At`。完整多级让步审批工作流 Non-Goal（owner doc 已登记 Deferred 触发条件）。

⚠️ **让步审批简化残留风险**：审批人通过 `allowConcession` 参数表达，调用方可在 `recordResult` 同一事务中既「录入结果」又「审批让步」——非真正的「质检员录入 + 主管审批」两角色分离。但 owner doc 已显式裁定为「简化 Non-Goal」，本审计不登记为新 finding（按 owner doc 边界裁定）。

#### SPC/抽样/校准/风险/目标/评审状态轴（Deferred CRUD 空壳）

⚠️ **6 个 Deferred CRUD 空壳状态轴清晰性受限**——Calibration/Risk/Goal/Review/SamplingPlan/SPC-Sample/Capability 的 BizModel 均 15-22 行 CRUD 桩（仅 `setEntityName` 构造器）。状态值由 codegen 默认值/外部触发（如 `SpcCapabilityCalculator:308 risk.setStatus("OPEN")` 自动建风险登记）承载。owner doc `spc.md` 描述 SPC 控制图生命周期但 state-machine.md 无独立章节（详 §2.10 + P2-MA2-063）。

按 mfg A2.6b P1-MA2-036 + hr A2.7a P1-MA2-041 + inv A2.11 P1-MA2-063 同型裁决，本审计对其中**有 owner doc 显式生命周期声明 + 实现为 CRUD 桩 + dict 死状态**的组合登记新 P1（详 §4 P1-MA2-065）。

---

### 2.2 维度「转换完整性」

**裁决：FAIL（1 项 P0 + 1 项新 P1 + 1 项 owner doc 已登记 Deferred）**

#### 质检单生命周期迁移矩阵（核心）

| From → To | 触发 | 前置 | 结果 | 代码位置 | 裁决 |
|-----------|------|------|------|----------|------|
| PENDING → ACCEPTED | `recordResult`（行级评测汇总） | status==PENDING + 行结果无 REJECTED + posted=true | setResult(ACCEPTED) + posted=true | `ErpQaInspectionBizModel.recordResult:59-99` | ✅ |
| PENDING → CONDITIONAL | `recordResult` + allowConcession | status==PENDING + 含 REJECTED 行 + allowConcession=true + 审批 | setResult(CONDITIONAL) + posted=true + approveStatus=APPROVED + approvedBy/At | `recordResult:83-92` | ✅ |
| PENDING → REJECTED | `recordResult` | status==PENDING + 含 REJECTED 行 + allowConcession=false | setResult(REJECTED) + posted=true + 触发 NCR 自动生成 | `recordResult:83-99 + ncrLifecycleService.autoCreateNcrFromInspection` | ✅ |
| **任意 → ACCEPTED**（绕过门控） | `passInspection` | **无前置**（无 status 检查 + 无行级评测 + 无 posted=true 写入 + 无 NCR 触发） | setResult(ACCEPTED) | `ErpQaInspectionBizModel.passInspection:257-264` | ❌ **P0-MA2-017** |
| **任意 → REJECTED**（绕过门控） | `failInspection` | **无前置**（无 status 检查 + 无 NCR 触发） | setResult(REJECTED) | `ErpQaInspectionBizModel.failInspection:266-273` | ❌ **P0-MA2-017** |
| **任意 → PENDING**（违反终态不可恢复） | `reInspect` | **无前置**（无 status 检查） | setResult(PENDING) | `ErpQaInspectionBizModel.reInspect:275-282` | ❌ **P0-MA2-017** |

❌ **P0-MA2-017**（详 §4）：`passInspection`/`failInspection`/`reInspect` 三方法**完全缺失状态守卫**，且 `reInspect` 直接违反 owner doc §3「终态不可直接恢复；若需复检，新建质检单（关联原单与业务单据）」强制规则。三个方法允许从任意状态（含 REJECTED 终态）直接翻到 ACCEPTED，**绕过强制质检门控**（`InspectionTrigger.enforceGate`→`isInspectionCleared` 检查 PENDING/REJECTED 阻塞业务流转；但 REJECTED 经 `reInspect`→PENDING→`passInspection`→ACCEPTED 可在无任何业务校验下放行）+ 绕过 NCR 自动生成（`failInspection` 不调 `autoCreateNcrFromInspection`）+ 绕过行级评测（`passInspection`/`failInspection` 不调 `InspectionResultEvaluator.aggregate`）+ 绕过 posted 三件套写入（`passInspection` 不设 `posted=true`）。**破坏质量门控核心约束**（owner doc §4 + §审查提示「强制质检的业务单据在质检完成前是否阻塞流转」）。

#### NCR 生命周期迁移矩阵

| From → To | 触发 | 前置 | 结果 | 代码位置 | 裁决 |
|-----------|------|------|------|----------|------|
| OPEN → IN_REVIEW | `submitReview` | status==OPEN | setStatus(IN_REVIEW) | `ErpQaNonConformanceBizModel.submitReview:73-81` | ✅ |
| IN_REVIEW → RESOLVED | `resolve` | status==IN_REVIEW + CAPA 全 COMPLETED + 验证人/日期 + posted=true（若 SCRAP + AUTO_POST）+ returnCode（若 RETURN） | setStatus(RESOLVED) + resolvedAt + dispatchFinancialImpact | `resolve:83-102` + `NcrLifecycleService.requireResolveGate:131-136` | ⚠️ **P1-MA2-066**（无 CAPA 措施时允许 resolve） |
| IN_REVIEW → ESCALATED_TO_RECALL | `escalateToRecall`/`upgradeToRecall` | status==IN_REVIEW | setStatus(ESCALATED_TO_RECALL) + upgradeToRecall 建 Recall 实体 | `escalateToRecall:138-147 + upgradeToRecall:149-172` | ✅ |
| OPEN/IN_REVIEW → CANCELLED | `cancel` | status==OPEN 或 IN_REVIEW | setStatus(CANCELLED) | `cancel:174-186` | ✅ |
| RESOLVED → posted=true | `postNcr`（人工入口，AUTO_POST 模式不需要） | status==RESOLVED + posted != true + disposition==SCRAP | dispatchScrap + posted=true | `postNcr:104-121` | ✅ |
| posted=true → posted=false | `reverseNcr`（红冲） | posted==true + disposition==SCRAP | reverseScrap + posted=false + 清 postedAt/By | `reverseNcr:123-136` | ✅ |

⚠️ **P1-MA2-066**（详 §4）：`NcrLifecycleService.allActionsCompletedAndVerified:95-102` 当 NCR 无任何 CAPA 措施时直接返回 true 允许 resolve——`requireResolveGate` 通过。owner doc `state-machine.md §NCR 与 CAPA 的关系`「CAPA 需效果验证才能关闭 NCR（闭环）」+ inspection-integration.md §4.3「效果验证通过 → NCR 状态转为 RESOLVED」暗示 CAPA 是 RESOLVED 的前置。代码允许「无 CAPA 直接 resolve」用于误开 NCR 场景（注释 :98-101 显式声明），但**未配置「无 CAPA」的合法场景门控**（如必须 dispositionType=CONCESSION 或评审标注「无措施」）——评审人可绕过 CAPA 闭环直接 resolve 真实不合格 NCR。属设计漏洞非数据破坏（仍需人工 resolve 动作 + owner doc 边界已部分声明）。

#### SCRAP/RETURN/CONCESSION 财务分派（owner doc §NCR 财务影响规则 ✅ 已落地）

✅ **SCRAP 处置**（`dispatchFinancialImpact:201-205`）：`NcrPostingDispatcher.dispatchScrap:63-83` 构造 `PostingEvent(businessType=NCR_SCRAP, billHeadCode=NCR.code, scrapAmount=quantity×avgCost)` → `NcrPostingExecutor.postEvent` → `IErpFinVoucherBiz.post` Facade（跨域写经 Facade 合规）→ 成功置 posted 三件套。**跨域写经 I\*Biz Facade 合规**（production 代码无 `daoFor(ErpFin*)` 直写）。

✅ **RETURN 处置**（`dispatchFinancialImpact:206-209`）：`NcrReturnOrchestrator.orchestrateReturn:72-82` 按 NCR.supplierId 分派：supplierId 非空 → `IErpPurReturnBiz.save` Facade（采购退货）；为空 → `IErpSalReturnBiz.save` Facade（销售退货）。NCR 侧仅登记 `returnCode`（单一过账来源原则——退货域独占凭证，NCR 不重复过账）。**跨域写经 I\*Biz Facade 合规**。

✅ **CONCESSION/DOWNGRADE 处置**：`postNcr:113-117` 守卫 `!isScrap(disposition)` 抛 `ERR_NCR_DISPOSITION_NOT_POSTABLE`——让步/降级无凭证（owner doc §NCR 财务影响规则「让步接收：无额外凭证」+ §实现偏离补注「DOWNGRADE 无凭证」一致）。

#### CAPA 生命周期（4 态 + 验证）

| From → To | 触发 | 前置 | 代码位置 | 裁决 |
|-----------|------|------|----------|------|
| PENDING → IN_PROGRESS | `startAction` | status==PENDING | `ErpQaActionBizModel.startAction:34-42` | ✅ |
| IN_PROGRESS → COMPLETED | `completeAction` | status==IN_PROGRESS | `completeAction:44-54` | ✅ |
| COMPLETED + verification | `verifyAction` | status==COMPLETED + verificationPerson/Date 非空 | `verifyAction:56-77` | ✅ |
| **OVERDUE** | 无 setStatus writer | — | grep 全 src/main `ACTION_STATUS_OVERDUE` 零业务使用 | ⚠️ dict 死状态（归 P1-MA2-065 同型） |

#### 召回生命周期（5 态全可达 + 标准审批 + 通知门控）

| From → To | 触发 | 前置 | 代码位置 | 裁决 |
|-----------|------|------|----------|------|
| → OPEN | `register` | — | `ErpQaRecallBizModel.register:87-102` | ✅ |
| OPEN → SUBMITTED→APPROVED | 标准审批（`ErpQaRecallProcessor.submitForApproval/approve`） | approveStatus UNSUBMITTED→SUBMITTED→APPROVED + status==OPEN | `ErpQaRecallProcessor:31-66 + 121-137` | ✅ |
| SUBMITTED → REJECTED + status=CANCELLED | `reject`（标准审批） | approveStatus==SUBMITTED | `ErpQaRecallProcessor.doReject:139-145` | ✅ |
| APPROVED → IN_PROGRESS | `locateTargets` | status==APPROVED | `ErpQaRecallBizModel.locateTargets:119-128` | ✅ |
| IN_PROGRESS + notify + generateReturns | `notifyCustomers`/`generateReturns` | status==IN_PROGRESS | `:130-162` | ✅ |
| IN_PROGRESS → CLOSED | `close` | status==IN_PROGRESS + notifyCustomer=true + 全 target returnStatus≠PENDING（config-gated） | `close:164-187` | ✅ |
| OPEN/APPROVED/IN_PROGRESS → CANCELLED | `cancel` | status 非 CLOSED/CANCELLED | `cancel:104-117` | ✅ |
| APPROVED → REJECTED（反审） | `reverseApprove`（标准审批） | approveStatus==APPROVED | `ErpQaRecallProcessor.doReverseApprove:147-152` | ✅ |

✅ **召回状态机完整覆盖 5 态全迁移 + 标准审批 + 通知门控**。owner doc `recall.md §状态机` 与代码 1:1 对应。

#### 业务单据作废联动取消（owner doc §4 声明未落地 — P1-MA2-064）

❌ **owner doc `state-machine.md §4 异常路径` + §审查提示「业务单据作废时关联质检单的联动取消」** + `§实现偏离补注 业务单据作废联动取消（未落地）`：「设计 §4「业务单据作废时关联质检单自动取消」本期未接线（业务域 cancel 未回调 quality 取消质检单）」。

**实际代码复核**：grep 全 `module-quality/erp-qa-service/src/main/` `cancelForBusinessBill\|cancelInspection\|cancelOnBillVoid\|onBusinessBillCancelled` **零匹配**。业务域（purchase/sales/mfg）cancel 路径无 `IErpQaInspectionBiz.cancel*` 调用。质检单悬挂风险：业务单据 CANCELLED 后 PENDING 质检单残留（强制质检 config 开启时 `isInspectionCleared` 仍查到 PENDING → 返回 false → 业务单据若重新流转仍被阻塞，但已 CANCELLED 业务单据不再流转故无运行时悬挂）。登记 **P1-MA2-064**（详 §4，owner doc 已登记 Deferred + 本审计交叉登记 + 升级评估裁决维持 P1 非 P0）。

---

### 2.3 维度「终端状态和恢复」

**裁决：FAIL（P0-MA2-017 直接违反）**

✅ **NCR RESOLVED/ESCALATED_TO_RECALL/CANCELLED 三终态无出边**——`submitReview/resolve/escalateToRecall/cancel` 全部守卫源态非终态（OPEN/IN_REVIEW），终态 setStatus 无 writer。

✅ **召回 CLOSED/CANCELLED 终态**——CLOSED 经 `close:164-187` 单一入口守卫 IN_REVIEW；CANCELLED 经 `cancel` + `reject` 多入口。两终态无出边。

❌ **质检单 ACCEPTED/CONDITIONAL/REJECTED 终态被 `reInspect` 绕过**——`reInspect:275-282` 无源态守卫，可直接将 ACCEPTED/CONDITIONAL/REJECTED 翻回 PENDING。owner doc §3「终态不可直接恢复；若需复检，新建质检单（关联原单与业务单据）」**直接违反**。

✅ **NCR 纠错路径**：`reverseNcr:123-136` 仅清除 posted 三件套（红冲凭证），NCR.status 保持 RESOLVED（不可恢复）。再次过账经 `postNcr:104-121` 守卫 status==RESOLVED + posted != true，无终态违反。

✅ **归档与活跃区分**：质检单/NCR/召回均 `useLogicalDelete=true`（delVersion 字段承载逻辑删除），终态归档不参与活跃 TODO。

---

### 2.4 维度「异常路径」

**裁决：FAIL（1 项 P0 + 1 项 P1 + Deferred 同型评估）」

| 异常场景 | 处理 | 代码位置 | 裁决 |
|----------|------|----------|------|
| 强制质检业务单据未经质检就流转 | `InspectionTrigger.enforceGate` 首次生成 PENDING 质检单 + 阻塞（BLOCKED）；二次流转查 `isInspectionCleared` PENDING/REJECTED 阻塞 / ACCEPTED-CONDITIONAL 放行 | `InspectionTrigger.enforceGate:35-50` | ✅（但被 P0-MA2-017 绕过） |
| **质检员 silent flip REJECTED→ACCEPTED**（绕过门控） | `passInspection` 无任何前置校验，REJECTED 直接翻 ACCEPTED | `passInspection:257-264` | ❌ **P0-MA2-017** |
| 质检模板缺失（物料未配置检验标准） | `InspectionTemplateMatcher.match` 返回 null → 全局默认模板；仍无则无行（人工补录） | `ErpQaInspectionBizModel.doCreateForBusinessBill:150-177` | ✅ |
| 复检结果与原检冲突 | owner doc §4 「以复检结果为准，原检记录保留」**实现缺失**——`reInspect` 直接覆写 result 不保留原值（无审计字段记录原 result） | `reInspect:275-282` | ❌ 归 **P0-MA2-017**（无原值保留 + 无新建关联单） |
| 让步接收未经审批 | `recordResult` 守卫 allowConcession=true 才设 CONDITIONAL；allowConcession=false 含 REJECTED 行 → 汇总 REJECTED | `recordResult:83-92` + `InspectionResultEvaluator.aggregate:73-93` | ✅ |
| 并发录入同一质检单 | 依赖 ORM `versionProp="version"` 透明乐观锁（`ErpQaInspection` ORM 声明 versionProp） | ORM 行 169 | ⚠️ 系统性并发归 **A2.17** |
| **业务单据作废联动取消质检单** | 未落地（业务域 cancel 未回调 quality） | grep 零匹配 | ❌ **P1-MA2-064** |
| **NCR 过账失败悬挂**（posted=false 窗口期） | `dispatchScrap` 失败抛 NopException → @BizMutation 事务回滚 → NCR.status 不进 RESOLVED（resolve:84-101 同事务）；MANUAL_POST 模式人工 `postNcr` 失败保持 posted=false | `resolve + dispatchFinancialImpact + postNcr` | ✅ + ⚠️ 同型悬挂交接（详下） |
| reverseNcr 红冲闭环对称性 | `reverseNcr` 守卫 posted==true → `reverseScrap`（凭证红冲） + 清 posted 三件套；对称 | `reverseNcr:123-136` | ✅ |

#### NCR 过账失败悬挂（owner doc §审查提示 + finance P1-MA2-032 + hr P1-MA2-048 同型评估）

**状态机角度复核**：

- **AUTO_POST 模式**（默认）：`resolve:84-101` 在同一 @BizMutation 事务内调 `dispatchFinancialImpact:196-210` → `dispatchScrap`。若 `dispatchScrap` 抛 NopException（如 `ERR_NCR_NO_QUANTITY` 或 `IErpFinVoucherBiz.post` 失败），整个事务回滚 → NCR.status 不进 RESOLVED 保持 IN_REVIEW。**无悬挂**——失败回滚到 IN_REVIEW，评审人可重试。
- **MANUAL_POST 模式**：resolve 跳过 dispatchScrap，NCR 进 RESOLVED + posted=false。后续人工 `postNcr` 失败（如 finance 引擎故障）抛 NopException → @BizMutation 事务回滚 → posted 保持 false + status 保持 RESOLVED（resolve 已提交，postNcr 独立事务）。**悬挂窗口期**：NCR RESOLVED + posted=false，需人工重试 postNcr 或 reverseNcr 回退。
- **RETURN 处置**：`orchestrateReturn` 失败抛 NopException（如 `IErpPurReturnBiz`/`IErpSalReturnBiz` Bean 未注入抛 `ERR_NCR_DISPOSITION_NOT_POSTABLE`）→ resolve 事务回滚 → NCR 不进 RESOLVED。**无悬挂**。

**裁决**：MANUAL_POST 模式 posted=false 窗口期同 finance P1-MA2-032 IGNORED 悬挂 + hr P1-MA2-048 salary 过账悬挂 + assets P1-MA2-060 同型根因（posting dispatcher 容错设计 + DeferredPostingSweepJob 兜底归 finance 域通用机制）。**不升 P0**：(1) AUTO_POST 默认路径经事务回滚覆盖；(2) MANUAL_POST 是显式人工入口（评审人主动选择）；(3) LOG.warn/error 提供运维可见性；(4) `reverseNcr` 提供回退路径；(5) 业财不一致可经期末试算平衡人工发现。**维持同型 P1 治理缺陷**（不重复登记根因，仅状态机角度复核）。

#### reverseNcr 红冲闭环对称性

✅ **对称**——`reverseNcr:123-136` 守卫 posted==true → `reverseScrap`（凭证红冲经 IErpFinVoucherBiz Facade）+ 清 posted 三件套（posted=false + postedAt=null + postedBy=null）。与 `dispatchScrap` 设 posted 三件套严格对称。NCR.status 保持 RESOLVED（无回退，owner doc §3 终态不可恢复合规）。再次过账经 `postNcr:104-121` 守卫 status==RESOLVED + posted != true（reverseNcr 后满足）→ 可重新 dispatchScrap。**对称性合规**。

---

### 2.5 维度「可达性」

**裁决：FAIL（P0-MA2-017 + Deferred CRUD 空壳死状态 P1-MA2-065）」

#### 质检单可达性

✅ 从 PENDING 可达三终态（ACCEPTED/CONDITIONAL/REJECTED）经 `recordResult` 单一入口。

❌ **终态被 `reInspect` 绕过可达 PENDING**（P0-MA2-017）——破坏「终态无出边」核心不变量。

#### NCR 可达性

✅ 从 OPEN 可达全 5 态（OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL + OPEN/IN_REVIEW→CANCELLED）。无不可达状态，无死锁（RESOLVED/ESCALATED_TO_RECALL/CANCELLED 三终态无出边）。

#### 召回可达性

✅ 从 OPEN 可达全 5 态（OPEN→SUBMITTED→APPROVED[审批]→IN_PROGRESS→CLOSED + OPEN→CANCELLED + SUBMITTED→REJECTED+CANCELLED + APPROVED→CANCELLED）。`register:87-102` 唯一入口。

#### CAPA 可达性

⚠️ dict `erp-qa/action-status` 4 态（PENDING/IN_PROGRESS/COMPLETED/OVERDUE）：PENDING→IN_PROGRESS→COMPLETED 经 `startAction/completeAction` 全可达；**OVERDUE 无 setStatus writer**——dict 死状态（与 inv P1-MA2-063 PickingOrder PICKING/PICKED + mfg P1-MA2-035 作业卡 TRANSFERRED + hr P1-MA2-042 发展计划项 OVERDUE 同型）。归 **P1-MA2-065** 合并裁决。

#### SPC/校准/风险/目标/评审可达性（**新 P1-MA2-065**）

❌ **dict 死状态 + CRUD 桩**：

| 实体 | dict 死状态 | BizModel 行数 | setStatus writer |
|------|-------------|---------------|------------------|
| ErpQaRiskRegister（risk-status 3 态） | **MITIGATED/CLOSED** | 15 行 CRUD 桩 | 仅 `SpcCapabilityCalculator:308 risk.setStatus("OPEN")`（自动建风险） |
| ErpQaQualityGoal（goal-status 4 态） | **ACTIVE/ACHIEVED/FAILED/CANCELLED 全 4 态** | 18 行 CRUD 桩 | **零 writer** |
| ErpQaCalibration（result + docStatus + approveStatus） | docStatus/APPROVE 全死状态 | 18 行 CRUD 桩 | 零 setStatus writer |
| ErpQaReview（docStatus + approveStatus） | 全死状态 | 18 行 CRUD 桩 | 零 setStatus writer |
| ErpQaSpcChart（calcStatus 3 态） | **STALE** | 87 行（collect/recalc/evaluateRules） | 仅 `SpcControlLimitCalculator:162 setCalcStatus(CALCULATED)` + codegen 默认 PENDING；**STALE 零 writer** |

按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）+ mfg A2.6a P1-MA2-035（作业卡 TRANSFERRED 死状态）+ mfg A2.6b P1-MA2-036（MRP CANCELLED + 预测 CONSUMED 死状态）+ hr A2.7a P1-MA2-040/041/042 + hr A2.7b P1-MA2-043/045 + inv A2.11 P1-MA2-063（PickingOrder PICKING/PICKED 死状态）同型裁决，合并登记 **P1-MA2-065**（详 §4）。**不破坏主路径**——质检单/NCR/召回三大主状态机完整覆盖生命周期；CRUD 空壳实体的状态字段由 codegen 默认值承载，不产生悬挂数据。

---

### 2.6 维度「角色和权限」

**裁决：PASS（owner doc 角色绑定齐全，运行时经 @BizMutation 入口权限）」

✅ **每个迁移绑定执行角色**（owner doc `state-machine.md §6` + §NCR 与 CAPA + recall.md §状态机）：
- 质检员：录入结果（PENDING→ACCEPTED/REJECTED）
- 质检员 + 质量主管审批：让步接收（PENDING→CONDITIONAL）
- 质量主管：NCR 评审/RESOLVED + CAPA 验证
- 质量主管/管理层：召回升级（危险操作）+ 召回审批

✅ **危险操作控制**：
- **召回升级**（`escalateToRecall`/`upgradeToRecall` IN_REVIEW→ESCALATED_TO_RECALL 终态）：owner doc `recall.md §状态机`「召回**强制审批**（高风险，severityLevel=CRITICAL 需高层审批）」+ `ErpQaRecallProcessor.validateBusinessRulesForApprove:115-117` 守卫 status==OPEN。
- **召回关闭**（`close` IN_PROGRESS→CLOSED）：通知门控 `erp-qua.recall-notify-required-to-close=true`（默认）+ 全 target returnStatus≠PENDING。

⚠️ **NCR RESOLVED 无显式角色校验**——`resolve:83-102` 不校验调用者角色，依赖 @BizMutation 入口权限。owner doc §NCR 与 CAPA 暗示「质量主管」角色。与 finance/mfg/pur/sal 同型（业务规则文档化 + 权限由平台层统一）。**不登记为新 finding**。

⚠️ **P0-MA2-017 加剧角色漂移风险**——`passInspection`/`failInspection`/`reInspect` 无角色+无状态守卫，任何能调 @BizMutation 的角色均可 silent flip 质检结果。owner doc §6「录入结果（质检员）」角色绑定被绕过。

✅ **角色名与状态名同源业务词汇**（质检员/质量主管/管理层，见 `roles-and-permissions.md`）。

---

### 2.7 维度「外部依赖」

**裁决：PASS（业务域查 quality 结果 + NCR 过账 + 退货编排经 I\*Biz Facade 全合规）」

✅ **业务单据触发质检**（owner doc §实现偏离补注「业务域查 quality 结果」替代设计 §7 事件驱动）：业务域 Processor/BizModel 在 confirm/approve/reportCompletion 等流转节点调 `InspectionTrigger.enforceGate`（business→quality 同步 I\*Biz 写触发）：
- billType 不在 `erp-qua.mandatory-inspection-bill-types`（默认空=不强制）→ 直接 CLEARED
- 属强制类型且无关联质检单 → 经 `IErpQaInspectionBiz.createForBusinessBill` 生成 PENDING 质检单 + BLOCKED
- 关联质检单未决（PENDING/REJECTED）→ BLOCKED
- 关联质检单 ACCEPTED/CONDITIONAL → CLEARED

**DAG 无环**——quality 不反向依赖 business（owner doc §实现偏离补注「业务域查 quality 结果」+ `IErpQaInspectionBiz.findByRelatedBill`/`isInspectionCleared` 只读 Facade）。✅

✅ **质检结果反馈业务域**：业务域 config-gated 查询 `isInspectionCleared` 在 confirm/DONE 前校验。残留风险：业务域须主动查（owner doc §实现偏离补注已声明，事件驱动留后继）。

✅ **NCR 过账事件发布给财务域**：`NcrPostingDispatcher.dispatchScrap` → `NcrPostingExecutor.postEvent` → `IErpFinVoucherBiz.post` Facade（@Transactional(REQUIRES_NEW) 跨域失败隔离）。**跨域写经 I\*Biz Facade 合规**（production 代码无 `daoFor(ErpFin*)` 直写——grep `module-quality/erp-qa-service/src/main` `daoFor(ErpFin` 零匹配）。

✅ **NCR 退货编排**：`NcrReturnOrchestrator.orchestrateReturn` 经 `IErpPurReturnBiz`/`IErpSalReturnBiz` Facade（@Nullable Inject + tryGetBeanByType 延迟查找避免 standalone 测试失败）。**跨域写经 I\*Biz Facade 合规**。

⚠️ **跨域只读 daoFor**（P1-MA1-022 持续）——`NcrPostingDispatcher.resolveStockBalance:117-131 daoFor(ErpInvStockBalance)` + `NcrReturnOrchestrator.findStockBalance:131-141 daoFor(ErpInvStockBalance)` + `ErpQaReportBizModel daoFor(ErpMdMaterial)` facade read-only（plan baseline 已声明）。**状态机角度复核无升级**：跨域只读是 avgCost 解析/warehouseId/currencyId 解析的副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；resolve/dispatchFinancialImpact 跨域读失败抛异常回滚 RESOLVED 不发生）。维持 P1-MA1-022 todo MR1。

---

### 2.8 维度「TODO / 任务策略」

**裁决：PASS（owner doc §8 避免沉没设计已落实 + PENDING 阻塞业务流转双重保护）」

✅ **PENDING 产生 assigned TODO**（质检员待检任务）——owner doc §8 表 + `createForBusinessBill:147-179` 生成 PENDING 质检单（强制质检 config-gated 时经 `InspectionTrigger.enforceGate` 自动生成）。

✅ **REJECTED 产生 assigned TODO**（质量主管不合格处理决策——退货/返工/报废）——NCR 自动生成（`autoCreateNcrFromInspection`）+ NCR OPEN 产生评审 TODO。

✅ **ACCEPTED/CONDITIONAL 不产生 TODO**（终态）。

✅ **避免 PENDING 长期滞留**（owner doc §8）——强制质检的业务单据在质检完成前阻塞流转（`isInspectionCleared` PENDING 阻塞 → 业务单据无法继续流转 → 业务方有动力推进质检）。

⚠️ **NCR IN_REVIEW 产生 TODO**（评审/CAPA 制定）——owner doc §NCR 与 CAPA + inspection-integration.md §4.2。

⚠️ **召回 OPEN 产生 TODO**（管理层审批——owner doc `recall.md §状态机` 强制审批）。

⚠️ **RiskRegister/QualityGoal/Calibration/Review 等 CRUD 空壳 TODO 策略未定义**——owner doc 无独立章节。CRUD 桩经 CrudBizModel 默认机制承载，不产生沉没（与 inv 拣货单同型 Deferred）。

---

### 2.9 维度「场景演练」（最重要）

**裁决：FAIL（场景 G 业务作废联动 + 场景 H NCR 无 CAPA resolve + 场景 K silent flip 三场景暴露 finding）」

#### 场景 A：来料检验合格 happy path

1. `ErpPurReceiveProcessor.approve` config-gated 调 `InspectionTrigger.enforceGate(billType="PUR_RECEIPT", ...)`（强制质检 config 开启时）
2. 首次流转：`enforceGate:42-47` 经 `IErpQaInspectionBiz.createForBusinessBill` 生成 PENDING 质检单 + 返回 BLOCKED → 业务单据 confirm 阻塞
3. 质检员录入行级 measuredValue + 调 `recordResult(allowConcession=false)`：行级评测全合格 → aggregate=ACCEPTED + posted=true
4. 二次流转：`enforceGate:49 isInspectionCleared` 返回 true → CLEARED → 业务单据继续流转

证据：`recordResult:59-99` + `InspectionTrigger.enforceGate:35-50` + `InspectionResultEvaluator.aggregate:73-93`。状态机：PENDING→ACCEPTED 全迁移有代码路径 ✓。

#### 场景 B：来料检验不合格退货（REJECTED→NCR→RETURN 编排退货域）

1. 质检员录入不合格行 + 调 `recordResult(allowConcession=false)`：含 REJECTED 行 → aggregate=REJECTED + posted=true
2. `recordResult:95-97` 触发 `ncrLifecycleService.autoCreateNcrFromInspection` → 自动建 NCR（status=OPEN, sourceType=INSPECTION, severity=NORMAL 默认）
3. 质量主管评审 NCR：`submitReview`（OPEN→IN_REVIEW）→ 设 `dispositionType=RETURN` + supplierId → `resolve`（IN_REVIEW→RESOLVED）
4. `resolve:99-101 dispatchFinancialImpact` → `dispatchFinancialImpact:206-209 isReturn` → `NcrReturnOrchestrator.orchestrateReturn` → supplierId 非空 → `IErpPurReturnBiz.save` 创建采购退货单 + NCR.returnCode 登记

证据：`autoCreateNcrFromInspection:46-65` + `resolve:83-102` + `NcrReturnOrchestrator.orchestrateReturn:72-101`。✅

#### 场景 C：让步接收（CONDITIONAL + 质量主管审批简化）

1. 质检员录入部分不合格行（非关键项）+ 调 `recordResult(allowConcession=true)`：含 REJECTED 行 + allowConcession=true → aggregate=CONDITIONAL
2. `recordResult:87-91`：CONDITIONAL + concession=true → 设 `approveStatus=APPROVED + approvedBy/At`（简化审批——审批经调用方 allowConcession 参数表达，非真正两角色分离）
3. posted=true + result=CONDITIONAL → `isInspectionCleared` 返回 true → 业务单据继续流转

证据：`recordResult:83-92` + `isInspectionCleared:113-130`（CONDITIONAL 不阻塞）。✅（owner doc §实现偏离补注让步审批简化已登记 Deferred）

#### 场景 D：完工检验返工（REJECTED→反馈制造域）

1. 工单完工 config-gated `InspectionTrigger.enforceGate(billType="MFG_WORK_ORDER_COMPLETION", ...)` 生成 PENDING 质检单
2. 不合格 → `recordResult` REJECTED + 自动 NCR
3. 制造域（A2.6a done）查询 `isInspectionCleared` 返回 false → 工单不进 DONE → 制造域新建返工工单（关联原工单）

证据：跨域交互经 config-gated 查询 Facade（详 A2.6a mfg 审计报告 §反馈机制）。✅

#### 场景 E：强制质检阻塞业务流转（enforceGate config-gated）

1. config `erp-qua.mandatory-inspection-bill-types=PUR_RECEIPT,SAL_DELIVERY,MFG_WORK_ORDER_COMPLETION`
2. 业务单据流转节点调 `enforceGate` → 首次生成 PENDING + BLOCKED → 业务单据 confirm 阻塞
3. 质检完成后再流转 → `isInspectionCleared` CLEARED → 放行

证据：`InspectionTrigger.enforceGate:35-50` + `isMandatoryBillType:53-62`（config 逗号分隔）。✅

#### 场景 F：NCR 报废过账（SCRAP→报废损失凭证）

1. NCR 评审 `dispositionType=SCRAP` + resolve → `dispatchFinancialImpact:201-205 isScrap` + AUTO_POST 模式 → `dispatchScrap`
2. `NcrPostingDispatcher.dispatchScrap:63-83`：quantity × avgCost（daoFor ErpInvStockBalance 只读解析）→ PostingEvent(NCR_SCRAP) → `NcrPostingExecutor.postEvent` → `IErpFinVoucherBiz.post`
3. 成功 → NCR.posted=true + postedAt/By 三件套

证据：`dispatchScrap:63-83` + `buildScrapEvent:99-115`。✅ 跨域写经 Facade 合规。

#### 场景 G：NCR 召回升级（ESCALATED_TO_RECALL→recall.md 终态）

1. NCR IN_REVIEW → 调 `upgradeToRecall`（建召回实体）
2. `upgradeToRecall:149-172`：守卫 IN_REVIEW → setStatus(ESCALATED_TO_RECALL) → 经 `recallBiz.register` Facade 建召回事件（triggerType=BATCH_NCR_UPGRADE, sourceNcrId, materialId, severityLevel 业务规则映射）
3. NCR 进 ESCALATED_TO_RECALL 终态（无出边）+ 召回事件独立状态机（OPEN→...→CLOSED/CANCELLED）

证据：`upgradeToRecall:149-172` + `ErpQaRecallBizModel.register:87-102`。✅ owner doc `recall.md §裁决 D2` 落实。

#### 场景 H：NCR 无 CAPA 直接 resolve（**新 P1-MA2-066**）

1. 质检 REJECTED → 自动建 NCR（OPEN）
2. 评审 `submitReview`（OPEN→IN_REVIEW）
3. **不创建任何 CAPA 措施**（ErpQaAction 留空）
4. 调 `resolve` → `NcrLifecycleService.requireResolveGate:131-136` → `allActionsCompletedAndVerified:95-102` 检查 actions.isEmpty() → **返回 true（允许 resolve）**
5. NCR.status=RESOLVED + resolvedAt + dispatchFinancialImpact（按 dispositionType 分派过账）

❌ **owner doc `state-machine.md §NCR 与 CAPA 的关系`「CAPA 需效果验证才能关闭 NCR（闭环）」+ inspection-integration.md §4.3「效果验证通过 → NCR 状态转为 RESOLVED」**——代码允许「无 CAPA 直接 resolve」（无措施时 requireResolveGate 通过）。代码注释（`NcrLifecycleService:97-101`）声明「无 CAPA 措施：允许 resolve（NCR 可无措施直接关闭，如误开评审后作废场景由 cancel 走）」——但**未配置合法场景门控**，评审人可绕过 CAPA 闭环直接 resolve 真实不合格 NCR。登记 **P1-MA2-066**（详 §4）。

#### 场景 I：reverseNcr 红冲闭环

1. NCR SCRAP + posted=true（AUTO_POST 自动或 MANUAL_POST 人工 postNcr 后）
2. 调 `reverseNcr` → 守卫 posted==true → `reverseScrap`（IErpFinVoucherBiz.reverse Facade 凭证红冲）+ 清 posted 三件套
3. NCR.status 保持 RESOLVED（终态不可恢复）+ posted=false → 可重新 `postNcr`

证据：`reverseNcr:123-136` + `NcrPostingDispatcher.reverseScrap:92-97`。✅ 对称性合规。

#### 场景 J：业务单据作废联动取消未落地（**P1-MA2-064**）

1. 业务单据（如 PUR_RECEIPT）已审核 → 强制质检生成 PENDING 质检单
2. 业务单据后续作废（CANCELLED）
3. **预期**（owner doc §4「业务单据作废时关联质检单自动取消」）：PENDING 质检单自动取消（status=CANCELLED 或逻辑删除）
4. **实际**：业务域 cancel 路径无 `IErpQaInspectionBiz.cancel*` 调用 → 质检单保持 PENDING 悬挂

❌ owner doc §实现偏离补注「业务单据作废联动取消（未落地）：设计 §4「业务单据作废时关联质检单自动取消」本期未接线（业务域 cancel 未回调 quality 取消质检单）」+ §审查提示「业务单据作废时关联质检单的联动取消」**未实现**。登记 **P1-MA2-064**（详 §4）。

#### 场景 K：质检员 silent flip REJECTED→ACCEPTED（**P0-MA2-017**）

1. 强制质检业务单据流转 → enforceGate 生成 PENDING 质检单 → 质检员录入不合格 → `recordResult` REJECTED + 自动 NCR + posted=true
2. 业务单据流转阻塞（`isInspectionCleared` REJECTED 返回 false）
3. **质检员调 `passInspection(inspectionId)`**（无任何状态守卫 + 无行级评测 + 无 NCR 校验）
4. `passInspection:257-264` `inspection.setResult(ACCEPTED) + updateEntity` —— **质检单直接 REJECTED→ACCEPTED**
5. 业务单据再次流转 → `isInspectionCleared` ACCEPTED 返回 true → **业务单据放行**
6. **不合格品入库**（质检员可能误操作 / 滥用 / 调试代码残留）

❌ **P0-MA2-017**（详 §4）。破坏 owner doc §3「终态不可直接恢复」+ §4「强制质检的业务单据在质检完成前阻塞流转」+ §审查提示「强制质检的业务单据在质检完成前是否阻塞流转」核心不变量。

#### 场景 L：NCR 过账失败悬挂（AUTO_POST 事务回滚 + MANUAL_POST 窗口期）

1. **AUTO_POST 模式**（默认）：resolve 同事务调 dispatchScrap → `IErpFinVoucherBiz.post` 失败抛 NopException → 整个 @BizMutation 事务回滚 → NCR.status 不进 RESOLVED（保持 IN_REVIEW） → 无悬挂 ✓
2. **MANUAL_POST 模式**：resolve 跳过 dispatch（NCR RESOLVED + posted=false）→ 人工 `postNcr` 失败 → @BizMutation 事务回滚 → posted 保持 false + status 保持 RESOLVED → **悬挂窗口期**（需 reverseNcr 回退或重试 postNcr）

⚠️ MANUAL_POST 窗口期同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 同型裁决 P1（不重复登记根因，状态机角度无升级——AUTO_POST 默认经事务回滚覆盖）。

---

### 2.10 维度「与设计文档一致性」

**裁决：FAIL（P0-MA2-017 owner doc §3+§4 违反 + 1 项新 P2 owner doc 章节缺失）」

| owner doc 章节 | 代码位置 | 一致性 | 裁决 |
|---------------|----------|--------|------|
| `state-machine.md §1 状态定义`（质检单 4 态） | `ErpQaConstants.INSPECTION_RESULT_*` + `erp-qa/inspection-result` dict | ✅ | ✓ |
| `state-machine.md §2 迁移完整性` | `recordResult` PENDING→三终态 | ✅ | ✓（但 `passInspection`/`failInspection`/`reInspect` 三方法未声明） |
| `state-machine.md §3 终态与恢复`「终态不可直接恢复；若需复检，新建质检单」 | `reInspect` 直接 setResult(PENDING) 无源态守卫 + 无新建关联单 | ❌ 代码违反 owner doc | **P0-MA2-017** |
| `state-machine.md §4 异常路径`「业务单据作废联动 → 关联质检单自动取消」 | 业务域 cancel 无回调 quality | ❌ 未落地 | **P1-MA2-064**（owner doc §实现偏离补注已声明 Deferred） |
| `state-machine.md §4 异常路径`「强制质检未经质检就流转 → 系统拦截」 | `InspectionTrigger.enforceGate` + `isInspectionCleared` | ✅ | ✓（但被 P0-MA2-017 绕过） |
| `state-machine.md §6 角色与权限` | @BizMutation 入口权限 | ✅ | ✓ |
| `state-machine.md §7 外部依赖`「事件驱动」 | 实际改为业务域查 quality 结果（owner doc §实现偏离补注已声明） | ⚠️ 偏离已登记 | 已闭环 |
| `state-machine.md §8 TODO 任务策略` | 强制质检阻塞业务流转避免沉没 | ✅ | ✓ |
| `state-machine.md §NCR 与 CAPA`「CAPA 需效果验证才能关闭」 | `allActionsCompletedAndVerified:95-102` 无 CAPA 时允许 resolve | ❌ 漏洞 | **P1-MA2-066** |
| `state-machine.md §NCR 财务影响规则` + §实现偏离补注 NCR 过账 | `NcrPostingDispatcher` SCRAP/RETURN/CONCESSION 分派 + posted 三件套 | ✅ | ✓（plan 2026-07-05-2352-2 done） |
| `state-machine.md §审查提示`「事件驱动」 | 实际业务域查 quality（owner doc §实现偏离补注已声明） | ⚠️ §审查提示文字未同步偏离补注 | **P2-MA2-064** |
| `recall.md §状态机`（召回 5 态） | `ErpQaRecallBizModel` + `ErpQaRecallProcessor` 全迁移 | ✅ | ✓ |
| `recall.md §裁决 D2`（NCR ESCALATED_TO_RECALL + 独立 Recall 实体） | `upgradeToRecall:149-172` + `ErpQaRecallBizModel.register` | ✅ | ✓ |
| `spc.md §关键流程`（采样/计算/失控预警/能力分析） | `SpcSamplingService`/`SpcControlLimitCalculator`/`SpcOutOfControlHandler`/`SpcCapabilityCalculator` | ✅ | ✓（plan 2026-07-07-0305-2 + 2026-07-19-0120-2 done） |
| `inspection-integration.md §4 NCR 与 CAPA 闭环` | `NcrLifecycleService` + `ErpQaActionBizModel` | ⚠️ 无 CAPA 允许 resolve | 归 **P1-MA2-066** |
| `inspection-integration.md §九 抽样检验规则`（AQL/转移规则） | owner doc 标注 Deferred（`ErpQaSamplingPlanBizModel` 15 行 CRUD 桩） | ⚠️ Deferred 已声明 | 不登记 |
| `processor-extension-pattern.md` Facade+Processor 两层 | `ErpQaRecallProcessor` protected step 方法 + 5 per-mutation Processor 薄包装 | ✅ | ✓ |
| `posting-exemptions.md` 跨域写豁免登记 | quality 无新豁免（NcrPostingDispatcher 经 IErpFinVoucherBiz Facade + NcrReturnOrchestrator 经 IErpPurReturnBiz/IErpSalReturnBiz Facade） | ✅ | ✓ |

#### owner doc 章节缺失（**新 P2-MA2-063**）

⚠️ `state-machine.md` 仅含「适用对象一：质检单（Inspection）」+「适用对象二：不符合项报告（NonConformance）」两章节。**8 个其他状态承载实体（CAPA/召回/召回目标/SPC 控制图/校准/风险登记/质量目标/质量评审）无独立章节**——散落在 `recall.md`、`spc.md`、各 plan 文件中。与 purchase P2-MA2-053 + sales P2-MA2-056 + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 + inv P2-MA2-062 同型（owner doc 缺独立章节）。登记 **P2-MA2-063**（详 §4）。

---

## 3. MA1 finding 运行时影响复核（quality 状态机角度）

| Finding ID | 原登记 | 本审计复核（状态机角度） | 终态 |
|-----------|--------|------------------------|------|
| **P1-MA1-012** | todo MR1（quality `ErpQaInspection.businessDate` propId 缺失） | **状态机角度无升级**——`businessDate` 是 ORM 规范层 propId 缺失（`app-erp-quality.orm.xml:183` 确认无 propId，对比同实体其他列 propId 齐全 + Calibration/Recall businessDate 有 propId）。`businessDate` 不参与状态机迁移判定（`recordResult`/`reInspect`/`passInspection`/`failInspection` 均不读 businessDate 决定 result 状态）；NCR 过账 `voucherDate` 优先 `ncrDate` fallback `CoreMetrics.today()`（`NcrPostingDispatcher.buildScrapEvent:107`）。**仅规范层缺陷** | **不升级**（维持 todo MR1） |
| **P1-MA1-022** | todo MR1（9 域合并含 qa `NcrPostingDispatcher`/`NcrReturnOrchestrator` ErpInvStockBalance + `ErpQaReportBizModel` ErpMdMaterial） | **状态机角度无升级**——跨域只读是 avgCost/warehouseId/currencyId 解析副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；resolve→dispatchFinancialImpact→resolveStockBalance 跨域读失败抛异常回滚 RESOLVED 不发生）。维持 P1-MA1-022 todo MR1 | **不升级**（维持 todo MR1） |

## 4. 新登记 finding

### P0（1 项，注入异步 fix plan）

#### P0-MA2-017 ErpQaInspectionBizModel passInspection/failInspection/reInspect 三方法缺状态守卫 + reInspect 违反 owner doc §3 终态不可恢复

- **位置**：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaInspectionBizModel.java:257-282`（passInspection:257-264 + failInspection:266-273 + reInspect:275-282）
- **现象**：三个 `@BizMutation` 方法**完全缺失状态守卫**，且 `reInspect` 直接违反 owner doc §3「终态不可直接恢复；若需复检，新建质检单（关联原单与业务单据）」强制规则：
  - `passInspection(inspectionId)`：无 `getResult()` 源态检查 + 无行级评测 + 无 `posted=true` 写入 + 无 NCR 触发 → 任意状态（含 REJECTED 终态）直接 `setResult(ACCEPTED) + updateEntity`
  - `failInspection(inspectionId)`：无源态检查 + 无 NCR 触发 → 任意状态直接 `setResult(REJECTED)`
  - `reInspect(inspectionId)`：无源态检查 → 任意状态（含 ACCEPTED/CONDITIONAL/REJECTED 终态）直接 `setResult(PENDING)`——**直接违反 owner doc §3 终态不可恢复 + 无新建关联单 + 无原值保留**
- **影响**：(a) **绕过强制质检门控**——`InspectionTrigger.enforceGate`→`isInspectionCleared` 检查 PENDING/REJECTED 阻塞业务流转，但 REJECTED 经 `reInspect`→PENDING→`passInspection`→ACCEPTED 可在无任何业务校验下放行，**不合格品入库**（owner doc §4「强制质检的业务单据在质检完成前阻塞流转」核心约束破坏）；(b) **绕过 NCR 自动生成**——`failInspection` 不调 `autoCreateNcrFromInspection`，不合格无 NCR 追溯；(c) **绕过 posted 三件套写入**——`passInspection` 不设 `posted=true`，业务单据反馈机制失效；(d) **审计轨迹丢失**——原 result 直接覆写，仅 `updatedBy`/`updateTime` 隐含变更。
- **裁决**：**P0 blocker**——破坏业务路径（强制质检门控可绕过）+ 导致数据错误（不合格品入库 + 无 NCR 追溯 + 审计轨迹丢失）。owner doc §3+§4+§审查提示核心不变量违反。**比 inv P1-MA2-062 StockTake 未自动生成移动单更严重**——后者是「应做未做」（破坏追溯链完整性），P0-MA2-017 是「不应做能做」（破坏终态不可恢复 + 强制门控）。
- **修复方式**：注入异步 fix plan（`docs/plans/2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard.md`）。触及质量保护区域 + xbiz 契约（IErpQaInspectionBiz 接口签名变更须 owner doc + 人工确认）。MR1 裁决——方案 A（推荐）：
  - `passInspection`/`failInspection`：守卫 `result==PENDING` 单一源态 + 设 `posted=true` + `failInspection` 触发 `autoCreateNcrFromInspection`（与 `recordResult` 行为对齐）；
  - `reInspect`：删除该方法 + IErpQaInspectionBiz 接口移除 + owner doc §3 强化注记「复检经新建质检单 `createForBusinessBill(relatedOriginalId)` 关联原单」（owner doc 补「原 result 保留为审计字段」语义）；
  - 方案 B（保留 reInspect 但加守卫）：reInspect 仅允许 `result==REJECTED` 且 NCR.status ∈ {RESOLVED/CANCELLED} 时迁移到 PENDING + 自动新建关联质检单 + 原 result 转 `originalResult` 审计字段（须 ORM 加列 ask-first）。
- **状态**：注入异步 fix plan（本审计不改代码 per plan Non-Goal）；状态 `pending fix plan`。

### P1（3 项，目标 MR1）

#### P1-MA2-064 业务单据作废联动取消质检单未落地（owner doc §4 声明 Deferred + 本审计交叉登记 + 升级评估维持 P1）

- **位置**：跨域（purchase/sales/mfg 业务域 cancel 路径 + quality `IErpQaInspectionBiz` 无 cancel 入口）
- **现象**：owner doc `state-machine.md §4 异常路径` + §审查提示「业务单据作废时关联质检单的联动取消」+ §实现偏离补注「业务单据作废联动取消（未落地）：设计 §4「业务单据作废时关联质检单自动取消」本期未接线（业务域 cancel 未回调 quality 取消质检单）」。grep 全 `module-quality/erp-qa-service/src/main/` `cancelForBusinessBill\|cancelInspection\|cancelOnBillVoid` **零匹配**。业务域 cancel 路径无 `IErpQaInspectionBiz.cancel*` 调用。
- **影响**：业务单据 CANCELLED 后 PENDING 质检单残留——强制质检 config 开启时 `isInspectionCleared` 仍查到 PENDING（但已 CANCELLED 业务单据不再流转故无运行时悬挂）。残留质检单产生 TODO 噪音（质检员看到已作废业务单据的待检任务）。
- **裁决**：**升级评估维持 P1 非 P0**——(1) 已 CANCELLED 业务单据不再流转，质检单悬挂不破坏主路径；(2) owner doc §实现偏离补注已显式声明 Deferred（非静默降级）；(3) 残留质检单经 useLogicalDelete 手工清理。**不破坏强制质检门控**（门控经 `isInspectionCleared` 检查，CANCELLED 业务单据不触发 enforceGate 二次流转）。
- **修复方式**：MR1 裁决——方案 A（推荐）`IErpQaInspectionBiz` 增 `cancelForBusinessBill(billType, billCode)` Facade（PENDING→cancelled via useLogicalDelete）+ 业务域 cancel Processor config-gated 调用；方案 B owner doc §4 标注「业务作废联动取消 Deferred——残留质检单经 useLogicalDelete 手工清理」+ 删除 owner doc §4「自动取消」语义。

#### P1-MA2-065 QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态 + CRUD 桩（合并裁决）

- **位置**：`ErpQaQualityGoalBizModel.java`（18 行）/ `ErpQaRiskRegisterBizModel.java`（15 行）/ `ErpQaCalibrationBizModel.java`（18 行）/ `ErpQaReviewBizModel.java`（18 行）/ `ErpQaActionBizModel.java`（OVERDUE 死状态）/ `SpcControlLimitCalculator.java:162`
- **现象**：6 处 dict 死状态合并登记：
  - `erp-qa/goal-status` 4 态（ACTIVE/ACHIEVED/FAILED/CANCELLED）——**全 4 态零 setStatus writer**（QualityGoalBizModel 18 行 CRUD 桩）；
  - `erp-qa/risk-status` 3 态（OPEN/MITIGATED/CLOSED）——仅 `SpcCapabilityCalculator:308 risk.setStatus("OPEN")` 自动建，MITIGATED/CLOSED 死状态；
  - `erp-qa/action-status` OVERDUE——PENDING/IN_PROGRESS/COMPLETED 经 `ErpQaActionBizModel` 全可达，OVERDUE 零 writer（dict 死状态）；
  - `erp-qa/spc-calc-status` STALE——PENDING（codegen 默认）/CALCULATED（`SpcControlLimitCalculator:162`）可达，STALE 零 writer；
  - ErpQaCalibration docStatus/approveStatus/result——BizModel 18 行 CRUD 桩零 writer；
  - ErpQaReview docStatus/approveStatus——BizModel 18 行 CRUD 桩零 writer。
- **影响**：状态字段 dict-bound 但无 setStatus 业务路径，状态值由 codegen 默认值/外部触发承载。UI/查询层期望按状态筛选时会失效。
- **裁决**：按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED）+ mfg A2.6a P1-MA2-035（作业卡 TRANSFERRED）+ mfg A2.6b P1-MA2-036（MRP CANCELLED + 预测 CONSUMED）+ hr A2.7a P1-MA2-040/041/042 + hr A2.7b P1-MA2-043/045 + inv A2.11 P1-MA2-063（PickingOrder PICKING/PICKED）同型裁决（dict 死状态 + BizModel CRUD 桩）。**不破坏主路径**——质检单/NCR/召回三大主状态机完整覆盖；CRUD 空壳实体状态字段不参与主路径迁移判定。
- **修复方式**：MR1 裁决——方案 A（推荐）各实体 owner doc + state-machine.md 标注「Deferred」+ 删除 dict 死状态项 + 删除常量定义；方案 B 实现 BizModel 状态机迁移（如 QualityGoal ACTIVE→ACHIEVED/FAILED/CANCELLED + RiskRegister OPEN→MITIGATED→CLOSED + SPC chart 自动 STALE 标记 + CAPA OVERDUE 定时 job）。

#### P1-MA2-066 NCR resolve 允许无 CAPA 直接关闭（闭环不变量缺口）

- **位置**：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/NcrLifecycleService.java:95-102`
- **现象**：`allActionsCompletedAndVerified` 当 NCR 无任何 CAPA 措施时直接返回 true 允许 resolve：`if (actions.isEmpty()) { ... return true; }`。`requireResolveGate:131-136` 通过 → resolve 不抛异常。owner doc `state-machine.md §NCR 与 CAPA 的关系`「CAPA 需效果验证才能关闭 NCR（闭环）」+ inspection-integration.md §4.3「效果验证通过 → NCR 状态转为 RESOLVED」暗示 CAPA 是 RESOLVED 前置。
- **影响**：评审人可绕过 CAPA 闭环直接 resolve 真实不合格 NCR（如 REJECTED→自动 NCR→submitReview→不建 CAPA→resolve）——破坏 CAPA 闭环不变量。代码注释（:97-101）声明「无 CAPA 措施：允许 resolve（NCR 可无措施直接关闭，如误开评审后作废场景由 cancel 走）」用于误开 NCR 场景，但**未配置合法场景门控**（如必须 dispositionType=CONCESSION 或评审标注「无措施」+ 理由）。
- **裁决**：**P1 非 P0**——(1) 仍需人工 resolve 动作（非自动路径）；(2) owner doc §NCR 与 CAPA 未显式声明「必有 CAPA」+ inspection-integration.md §4.1「质检不合格→自动创建 NCR」未声明 NCR 必有 CAPA；(3) 误开 NCR 场景合法（cancel 走，但误开识别前可能先进 IN_REVIEW）。属闭环不变量治理缺口非数据破坏。
- **修复方式**：MR1 裁决——方案 A（推荐）`allActionsCompletedAndVerified` 改为「actions.isEmpty() 时要求 NCR 显式标注 `noCapaReason`（须 ORM 加列 ask-first）+ 否则抛 `ERR_NCR_RESOLVE_NO_CAPA`」；方案 B owner doc §NCR 与 CAPA 标注「无 CAPA 允许 resolve 用于误开/降级 NCR 场景，由评审人保证」+ `allActionsCompletedAndVerified` 保留 isEmpty→true 行为。

### P2 watch-only（2 项）

#### P2-MA2-063 state-machine.md 缺 8 状态承载实体独立章节

- **位置**：`docs/design/quality/state-machine.md`
- **现象**：state-machine.md 仅含「适用对象一：质检单」+「适用对象二：不符合项报告」两章节。**8 个其他状态承载实体（CAPA/召回/召回目标/SPC 控制图/校准/风险登记/质量目标/质量评审）无独立章节**——散落在 `recall.md`、`spc.md`、各 plan 文件中。
- **裁决**：与 purchase P2-MA2-053 + sales P2-MA2-056 + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 + inv P2-MA2-062 同型（owner doc 缺独立章节）。无运行时影响（每实体状态机经代码 + plan 文件证据可追溯），仅 owner doc 可读性缺陷。
- **修复方式**：watch-only，MR1 顺手——方案 A（推荐）state-machine.md 新增「对象三：CAPA 状态机」+「对象四：召回事件状态机」+「对象五：SPC 控制图状态机」+「对象六：量具校准状态机」+「对象七：风险登记状态机」+「对象八：质量目标状态机」+「对象九：质量评审状态机」（本审计 §2.2-2.5 状态图可直接采用）；方案 B 交叉链接到各 owner doc。

#### P2-MA2-064 state-machine.md §审查提示「事件驱动」vs §实现偏离补注「业务域查 quality 结果」未同步

- **位置**：`docs/design/quality/state-machine.md §审查提示`
- **现象**：§审查提示「质检结果反馈业务域的机制（事件驱动）」+「业务单据（采购入库/销售出库/工单）触发质检：业务域发布事件，本域订阅生成质检单」文字与 §实现偏离补注「质检结果反馈业务域：设计 §7「事件驱动」本期改为**业务域查 quality 结果**（`IErpQaInspectionBiz.findByRelatedBill`/`isInspectionCleared`），quality 不反向依赖 business」+「强制质检阻塞机制：经 `erp-qua.mandatory-inspection-bill-types` config-gated + `InspectionTrigger.enforceGate`（business→quality 同步 I\*Biz 写触发）」偏离补注不一致。§审查提示文字未同步偏离补注。
- **裁决**：owner doc 内部不一致（§审查提示 vs §实现偏离补注）。审查者按 §审查提示期望事件驱动，实际是业务域查 quality（偏离已登记但 §审查提示未同步）。无运行时影响。
- **修复方式**：watch-only，MR1 顺手——`state-machine.md §审查提示`更新为「质检结果反馈业务域的机制：业务域查 quality 结果（`isInspectionCleared`），事件驱动 Deferred」+「业务单据触发质检：业务域 config-gated 调 `InspectionTrigger.enforceGate`（同步 I\*Biz 写触发）」。

## 5. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 风险 | 交接状态 |
|--------|------|------|----------|
| ErpQaInspection 并发录入同一质检单 | `ErpQaInspectionBizModel.recordResult/passInspection/failInspection/reInspect` 读-改-写 result 无显式锁 | 并发 recordResult + passInspection 可能 silent lost-update（前者 PENDING→REJECTED + NCR，后者 REJECTED→ACCEPTED） | 交接 A2.17（依赖 ErpQaInspection ORM `versionProp="version"` 透明乐观锁降级为 detectable conflict；P0-MA2-017 修复后状态守卫进一步限制并发窗口） |
| NCR 并发 resolve + reverseNcr | `ErpQaNonConformanceBizModel.resolve/reverseNcr` 读-改-写 status + posted 无显式锁 | 并发 resolve（IN_REVIEW→RESOLVED + posted=true）+ reverseNcr（posted=true→false）可能状态漂移 | 交接 A2.17（依赖 ErpQaNonConformance ORM versionProp 透明乐观锁） |
| NcrPostingExecutor.postEvent 并发同 NCR.code | `NcrPostingDispatcher.dispatchScrap` + `postNcr` 无 NCR.code 互斥 | 并发 postNcr 同 NCR 可能重复过账（幂等键 `(billHeadCode=NCR.code, businessType=NCR_SCRAP)` 经 `IErpFinVoucherBiz` 引擎反查兜底——详 A2.5a finance 凭证状态机审计） | 交接 A2.17（凭证引擎幂等键兜底 + NCR.posted=true 守卫二次 postNcr 抛 ERR_NCR_ALREADY_POSTED） |
| ErpQaRecall 并发 locateTargets/notifyCustomers/generateReturns | `ErpQaRecallBizModel` 多 mutation 读-改-写 status 无显式锁 | 并发触发可能状态漂移（如 locateTargets + cancel 同时） | 交接 A2.17（依赖 ErpQaRecall ORM versionProp 透明乐观锁 + requireRecallStatus 守卫单点迁移） |

## 6. 残留风险

1. **P0-MA2-017 强制质检门控可绕过**：`passInspection`/`failInspection`/`reInspect` 三方法缺状态守卫，可 silent flip 质检结果绕过强制质检门控。**注入异步 fix plan** 待 R1.0 展开机制处理。
2. **P1-MA2-064 业务单据作废联动取消未落地**：owner doc §4 声明 Deferred。残留质检单经 useLogicalDelete 手工清理。
3. **P1-MA2-065 QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态**：CRUD 空壳实体状态字段不参与主路径。归 MR1 裁决。
4. **P1-MA2-066 NCR resolve 无 CAPA 闭环缺口**：评审人可绕过 CAPA 闭环。归 MR1 裁决。
5. **NCR 过账失败悬挂（MANUAL_POST 窗口期）**：同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 同型，AUTO_POST 默认经事务回滚覆盖。归 MR1 整体裁决。
6. **并发敏感点 4 处交接 A2.17**：本审计不做系统性并发正确性裁决。
7. **让步审批简化**（CONDITIONAL 经 approveStatus=APPROVED 单点审批 + 调用方 allowConcession 参数表达）：owner doc §实现偏离补注已登记 Deferred，多级审批 successor。

## 7. 裁决

### 7.1 10 维度裁决汇总

| 维度 | 裁决 | 关键证据 |
|------|------|----------|
| 1. 状态定义 | ✅ PASS（含 Deferred CRUD 空壳清晰性注记） | 质检单 4 态 + NCR 5 态 + 召回 5 态清晰；让步审批简化 owner doc Deferred；CRUD 空壳清晰性受限（归 P1-MA2-065） |
| 2. 转换完整性 | ❌ FAIL（1 项 P0 + 1 项新 P1 + 1 项 owner doc Deferred） | recordResult PENDING→三终态 + NCR 5 态 + 召回 5 态齐全；**P0-MA2-017** passInspection/failInspection/reInspect 缺守卫；**P1-MA2-064** 业务作废联动未落地；**P1-MA2-066** 无 CAPA resolve 漏洞 |
| 3. 终端与恢复 | ❌ FAIL（P0-MA2-017 直接违反） | NCR/召回终态无出边 ✓；**质检单 ACCEPTED/CONDITIONAL/REJECTED 被 reInspect 绕过** |
| 4. 异常路径 | ❌ FAIL（1 项 P0 + 1 项 P1 + 同型悬挂交接） | 强制质检门控 + 模板缺失兜底 + 让步审批前置齐全；**P0-MA2-017 silent flip** + **P1-MA2-064 作废联动**；NCR 过账悬挂 MANUAL_POST 窗口期同型交接 |
| 5. 可达性 | ❌ FAIL（P0-MA2-017 + P1-MA2-065） | 质检单 PENDING→三终态 + NCR/召回全态可达 ✓；**质检单终态被 reInspect 绕过** + CRUD 空壳 dict 死状态 |
| 6. 角色与权限 | ✅ PASS（P0-MA2-017 加剧角色漂移） | owner doc §6 角色绑定齐全 + 召回强制审批 + 通知门控；@BizMutation 入口权限；P0-MA2-017 加剧 silent flip 角色漂移 |
| 7. 外部依赖 | ✅ PASS | InspectionTrigger.enforceGate 同步 I*Biz 写 + NCR 过账经 IErpFinVoucherBiz Facade + 退货编排经 IErpPurReturnBiz/IErpSalReturnBiz Facade；P1-MA1-022 跨域只读维持 |
| 8. TODO 任务策略 | ✅ PASS | PENDING assigned TODO + REJECTED NCR TODO + 强制质检阻塞避免沉没；CRUD 空壳 TODO 经默认机制承载 |
| 9. 场景演练 | ❌ FAIL（12 场景覆盖；场景 J/K/H 暴露 finding） | 场景 A-G + I-L 覆盖 ✓；**场景 J 作废联动缺失**（P1-MA2-064）+ **场景 K silent flip**（P0-MA2-017）+ **场景 H 无 CAPA resolve**（P1-MA2-066） |
| 10. 与设计文档一致性 | ❌ FAIL（P0-MA2-017 owner doc §3+§4 违反 + 1 项新 P2） | owner doc 12 章节中 §3/§4 被 P0 违反；§NCR 与 CAPA 被 P1-MA2-066 漏洞；8 实体无独立章节（P2-MA2-063）+ §审查提示文字未同步（P2-MA2-064） |

### 7.2 状态机正确性维度 qa 列推进

| 维度（前） | qa 列（前） | qa 列（后） | 推进依据 |
|------------|-------------|-------------|----------|
| 状态机正确性 | ❓ | **⚠️P0→fix-plan + P1(A2.12✅)** | 质量状态机核心契约（质检单 4 态 + NCR 5 态 + 召回 5 态 + CAPA 闭环 + SPC 失控预警 + 强制质检门控 + NCR 过账 SCRAP/RETURN/CONCESSION 分派 + posted 三件套 + reverseNcr 红冲闭环 + 跨域 Facade）经证据逐项确认；**1 项 P0** P0-MA2-017 passInspection/failInspection/reInspect 缺状态守卫致强制质检门控可绕过 [已注入异步 fix plan]；**3 项新 P1**（P1-MA2-064 业务作废联动未落地 / P1-MA2-065 CRUD 空壳 dict 死状态合并 / P1-MA2-066 NCR resolve 无 CAPA 漏洞）；**2 项新 P2** watch-only（P2-MA2-063 owner doc 缺独立章节 / P2-MA2-064 §审查提示文字未同步）；2 项已登记 MA1 finding（P1-MA1-012 propId / P1-MA1-022 跨域只读）运行时复核无升级；4 处并发敏感点交接 A2.17 |

### 7.3 Verdict

**Verdict: fail（条件性 → pass after P0 fix）**——质量状态机核心契约（NCR 5 态 + 召回 5 态 + CAPA 闭环 + SPC + 强制质检门控 + NCR 过账 + 跨域 Facade）经证据逐项确认，但**质检单状态机存在 P0**——`passInspection`/`failInspection`/`reInspect` 三方法缺状态守卫致强制质检门控可绕过（不合格品可 silent 入库）+ 违反 owner doc §3 终态不可恢复。**P0 已注入异步 fix plan**（`docs/plans/2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard.md`）；3 项新 P1 + 2 项新 P2 已登记待 MR1；2 项已登记 MA1 finding 运行时复核无升级；4 处并发敏感点交接 A2.17。**P0 修复完成 + 独立审计后 Verdict 转 pass**。

**审查范围**：module-quality 11 个状态承载实体 + NCR 过账/退货编排/强制质检门控助手 + 4 个 owner doc + 2 个 architecture doc。

**可达性摘要**：质检单 PENDING→三终态可达但**终态被 reInspect 绕过**（P0-MA2-017）；NCR 从 OPEN 可达全 5 态；召回从 OPEN 可达全 5 态；CRUD 空壳 dict 死状态（P1-MA2-065）。

**角色/权限摘要**：每个迁移绑定执行角色（owner doc §6 + NCR/CAPA/召回）；召回强制审批 + 通知门控经 config-gated 双层保护；P0-MA2-017 加剧 silent flip 角色漂移风险。

**外部依赖摘要**：跨域写经 I*Biz Facade（NcrPostingExecutor→IErpFinVoucherBiz + NcrReturnOrchestrator→IErpPurReturnBiz/IErpSalReturnBiz + InspectionTrigger→IErpQaInspectionBiz）；跨域只读维持 P1-MA1-022 todo MR1。

**剩余风险**：详 §6（7 项，均归 P0 fix plan / MR1 / Deferred successor / A2.17）。

## 8. 引用

- 审计 plan：`docs/plans/2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine.md`
- 范本（inventory A2.11）：`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`
- 范本（assets A2.10）：`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`
- 上游 A2.1 P2P：`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`
- 上游 A2.5a finance 凭证状态机（reverseNcr 红冲同型范式 + tryPost 吞异常悬挂同型）：`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`
- NCR 过账引擎 plan（done）：`docs/plans/2026-07-05-2352-2-ncr-financial-posting.md`
- 质检触发 + NCR/CAPA plan（done）：`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`
- P0-MA2-017 fix plan：`docs/plans/2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard.md`
- owner docs：`docs/design/quality/{state-machine,recall,spc,inspection-integration}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
- skill：`docs/skills/state-machine-business-review-prompt.md`
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（状态机正确性 + qa 列推进至 ⚠️P0→fix-plan + P1(A2.12✅)）
