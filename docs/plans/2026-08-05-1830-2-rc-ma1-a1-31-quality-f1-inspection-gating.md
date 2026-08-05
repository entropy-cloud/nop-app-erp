# 2026-08-05-1830-2 rc-ma1-a1-31-quality-f1-inspection-gating 质量域 quality-F1 检验门控需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.31（MA1 需求追踪矩阵审计 — quality-F1 检验门控：强制质检阻塞 / 不合格退货 / 让步 / 完工返工 / 关键项否决 / 模板优先级 / 作废联动）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.31
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.31 的 0.2 依赖）、`2026-08-05-1830-1-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（同批次 N=1，先行无依赖关系）、`2026-08-05-1830-3-rc-ma1-a1-32-quality-f2-ncr-capa-closure.md`（同批次 N=3，NCR-CAPA 闭环为本切片不合格处置的后置环节，F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.31 给出 UC 清单 = `UC-QA-01/02/03/04/06/07/08`（7 UC），含 `use-cases.md:15/:33/:50/:67/:101/:116/:133` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 quality 域首个 RC 切片（quality 域共 3 切片 A1.31/A1.32/A1.33）。

- **L1 需求契约（权威真相源）**：`docs/design/quality/use-cases.md`（机制见 `quality/inspection-integration.md §一/§二/§三/§四/§五`、`quality/state-machine.md`）：
  - UC-QA-01 来料强制质检阻塞流转（`:15`）：`物料.inspection_required==强制` → 采购入库审核 → 发布事件(INCOMING) → 创建质检单(PENDING)，入库流程阻塞；PENDING 期间入库单不可继续后续（过账/可用）；ACCEPTED→入库继续；REJECTED→触发退货。
  - UC-QA-02 质检不合格触发退货（`:33`）：质检单 REJECTED（关键项不合格）→ 创建 NCR(OPEN) + 触发退货（关联入库单生成退货单）；NCR 记录不合格详情；退货走 `../purchase/returns.md`。
  - UC-QA-03 让步接收（`:50`）：非关键项不合格 → 让步流程（建议→审批→CONDITIONAL，记录让步原因/审批人）；CONDITIONAL 业务单据继续流转但标记让步；关键项不合格→不可让步（直接 REJECTED）。
  - UC-QA-04 完工检验不合格→返工（`:67`）：完工质检(FINAL) REJECTED → 不合格处理路径：返工 → 触发 `../manufacturing` 新建返工工单（关联原工单）。
  - UC-QA-06 关键项否决（`:101`）：质检模板行.是否关键项==true 且该行不合格 → 整体质检单=REJECTED（关键项否决，无论其他项）；非关键项不合格→可让步(CONDITIONAL)。
  - UC-QA-07 质检模板优先级解析（`:116`）：优先级 物料级模板 > 物料类别级 > 全局默认；物料有专属→用专属；否则查类别→用类别；否则用全局默认；模板缺失且强制质检→报错或用最小默认。
  - UC-QA-08 业务单据作废联动取消质检（`:133`）：入库单.作废 → 关联质检单（若 PENDING）→取消(CANCELLED)；不影响已 ACCEPTED/REJECTED 的质检单（历史完整）。

- **L3 代码实现现状（实测）**——7 UC 中 4 UC（QA-01/02/03 + 模板/门控基础设施）主路径已实现且测试强；**3 UC（QA-04/06/07/08）存在实质缺口待核，其中 UC-QA-06 关键项否决与 UC-QA-08 作废联动是高风险核查点**：
  - **UC-QA-01 来料强制质检阻塞**（✅ 已实现 & 强测）：`InspectionTrigger.enforceGate`（config-gated 同步 I*Biz 写触发，A2.12 已证实）+ `processor/ErpQaInspectionCreateForBusinessBillProcessor.java:20-67`（创建质检单 `result=PENDING :38` + copyTemplateLinesToInspection `:56` + 行 PENDING `:67`）+ `ErpQaInspectionBizModel.java:88-95 isInspectionCleared`（业务域 confirm/DONE 前校验，ACCEPTED/CONDITIONAL 放行，REJECTED 阻塞 `:89`）——**与 L1 一致**。
  - **UC-QA-02 质检不合格触发退货**（✅ 已实现 & 强测）：`processor/ErpQaInspectionFailInspectionProcessor.java`（PENDING→REJECTED 终态翻转 + posted 簿记 + 自动生成 NCR）+ NCR RETURN 处置经 `NcrReturnOrchestrator`→`IErpPurReturnBiz` Facade（A2.12 已证实，`testReturnDispositionOrchestratesPurchaseReturn:111`）——**与 L1 一致**（REJECTED→NCR+退货走 purchase/returns）。
  - **UC-QA-03 让步接收**（✅ 已实现 & 强测）：`entity/InspectionResultEvaluator.java:69-92`（汇总：全行 ACCEPTED→ACCEPTED；含 REJECTED 且 allowConcession→CONDITIONAL `:91`；含 REJECTED 且未让步→REJECTED `:92`）+ 关键项直接 REJECTED（`:43-49` 单行评测，关键项不合格→REJECTED 不可让步）——**与 L1 一致**。
  - **UC-QA-04 完工检验不合格→返工**（⚠️ 重点核查——跨域返工工单触发链待确认）：FINAL 质检触发 `ErpQaInspectionCreateForBusinessBillProcessor`（triggerType=FINAL）+ REJECTED→自动生成 NCR（`NcrLifecycleService`）；**但 L1 `:75` 要求「触发 ../manufacturing 新建返工工单（关联原工单）」**——grep `module-quality/erp-qa-service/src/main/` `rework|ReworkOrder|createRework|IErpMfg.*Order|返工工单` **零业务命中**（仅 `ErpQaInspectionBizModel.java:88` 注释「业务域应触发退货/返工/NCR 处置」声明由业务域处理）。**候选缺口**：quality 侧仅生成 NCR，**未直接跨域建返工工单**——owner doc `inspection-integration.md §2.3 路径表:90` 显式列「完工检验不合格 | 返工/报废 | 返工工单/报废单」+ §7.2:265 记录跨域 REJECTED→返工协议（事件解耦，quality DAG 无环不反向依赖 business）；须核 manufacturing 侧（UC-MFG-09，属 A1.9 done）是否监听 NCR/FINAL-REJECTED 主动建返工工单闭环。**倾向 P2（事件解耦 successor）/ 接受（若 mfg 侧已闭环）**——须 §4 三判据复核 inspection-integration.md §二/§7.2。
  - **UC-QA-06 关键项否决**（❌ 倾向 **P1**——核心验收标准完全缺失）：L1 `:107-109` 要求「质检模板行.是否关键项==true 且该行不合格 → 整体质检单=REJECTED（关键项否决，无论其他项）；关键项不合格→不可让步（直接 REJECTED）」。**实测 `ErpQaInspectionLine`/`ErpQaInspectionTemplateLine` ORM 无 `isCritical`/`关键项`/`criticalItem` 列**（`ErpQaInspectionTemplateLine` 仅 `isRequired`=是否必检，语义不同于关键项；grep `isCritical|criticalItem|关键项` 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + orm.xml 零业务命中，仅 SPC SEVERITY_CRITICAL 无关）。**`InspectionResultEvaluator.aggregate:73-92` 无关键项否决逻辑**——对所有 REJECTED 行统一处理：`anyRejected && allowConcession → CONDITIONAL (:90-91)`，**关键项不合格 + allowConcession=true 错误产出 CONDITIONAL，直接违反 L1「关键项不合格→不可让步→直接 REJECTED」**。所引 `:43-49` 是通用 out-of-spec/null-measured REJECTED 探测，非关键项逻辑。**测试 `testRejectedCriticalGoesRejected:84` 命名误导**——实为 `allowConcession=false` 的通用拒绝断言（无关键项标记可设），未覆盖否决覆盖让步的路径。**此为 Q1 根因（代理转述已向实现妥协）的典型**：测试名误导致基线误判。属需求契约核心验收标准完全缺失（数据模型 + 评估器逻辑双缺），按 §2 P1①（功能完全缺失）+ P1②（异常路径未实现）。须人工确认 product-scope 是否要求关键项否决（若 L1 明确要求则 P1 强制实现，修复 = ORM 加 isCritical 列[ask-first] + aggregate 增关键项否决分支）。
  - **UC-QA-07 质检模板优先级解析**（⚠️ 倾向 **P2**——类别级解析缺失）：`entity/InspectionTemplateMatcher.java:19-35 match`（Javadoc `:19-20` 显式描述仅**两级**：(1) `materialId × inspectionType` 匹配 active 模板 `:31` → (2) 回落全局默认 `erp-qua.default-inspection-template` `:33-35` → (3) 仍无返回 null 人工补录）。**L1 `:122` 要求三级（物料级 > 物料类别级 > 全局默认），实测无类别级（物料类别级）查询**（`findActiveByMaterialAndType:50-54` 仅按 materialId 过滤）。主路径（专属模板/全局默认）OK，类别级回退缺失（次要验收标准未完全满足）。`testNoTemplateFallsToGlobalDefault:107` + `testNoTemplateNoLinesManualEntry:123` 仅覆盖两级回退。按 §2 P2①。
  - **UC-QA-08 业务单据作废联动取消质检**（⚠️ 高优先核查——R1.20 **显式 Deferred** 该 Facade，L1 验收标准经 deferral 未满足）：L1 `:138-141` 要求「入库单.作废 → 关联质检单（若 PENDING）→取消(CANCELLED)；不影响已 ACCEPTED/REJECTED」。**实测 `IErpQaInspectionBiz`（85 行）无 `cancelForBusinessBill`**；grep 跨 module-quality/purchase/sales/mfg = 零命中。**关键事实（纠正「未合入/被回退」臆测）**：R1.20 plan `docs/plans/2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md:37,64` **显式裁决 P1-MA2-064 为 Deferred**（owner-doc 正式化，非 arm-index 推荐的方案 A 实现）——理由：方案 A 属跨域 wiring（purchase/sales/mfg cancel Processor）跨表面实现，与危害（TODO 噪音，不破坏主路径 + 残留经 useLogicalDelete 手工清理）不成比例；successor 触发条件「业务作废自动取消质检需求时」；`docs/design/quality/state-machine.md:190` 正式标注「业务单据作废联动取消（Deferred）...本期不落地」。**故 Facade 缺失是 deliberate deferral，非丢失合入/回退**。`docs/audits/arm-index.md:354` 标 P1-MA2-064「✅ resolved (R1.20 done)」未披露该 resolution 是 *deferral* 而非 *implementation*——属 **arm-index 状态澄清事项**（误导性「done」标签致基线初判误为已实现）。**本切片处置（与 §去重协议一致）**：不「重开」P1-MA2-064（那是 audit-remediation mission 的 finding，本审计 MA1 不重审 MA2 行为），而是从 L1 视角登记 **新 `P1-RC-xxx`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类：判据 (i) R1.20 plan 含独立 plan-audit 通过记录（ses_0502be6efffeIzndtBDicVNwJR iteration 2 accept）→ 「经审计裁决的简化」成立 → 倾向 **P2**（documented simplification with approval，非静默降级）；但同时声明 Q4=(a) 张力——若判定 L1 UC-QA-08 为硬性 P1 要求则须实现（须人工确认 product-scope）+ arm-index 状态澄清（resolved-via-deferral 而非 resolved-via-implementation）。

- **L4 测试证据现状**（`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/`）：
  - `TestErpQaInspectionStateMachine.java`（13 @Test：testAllAcceptedGoesAccepted:63/testPartialRejectedWithConcessionGoesConditional:73/testRejectedCriticalGoesRejected:84/testLineSpecEvaluationParseFailureTreatedAsRejected:94/testTerminalResultCannotReRecord:115/testPassInspectionRejectsTerminalState:127/testFailInspectionRejectsTerminalState:140/testPassInspectionFromPendingSetsPosted:153/testFailInspectionFromPendingSetsPostedAndTriggersNcr:166/testReinspectionViaNewIndependentInspection:195/testIsInspectionClearedFalseWhenPending:232——**强**，覆盖状态机迁移 + 关键项否决 + 让步 + 终态守卫 + P0-MA2-017 修复后状态守卫）。
  - `TestErpQaInspectionTrigger.java`（6 @Test：testPurchaseReceiptTriggerGeneratesIncomingWithTemplateLines:68/testSalesOutgoingTrigger:84/testWorkOrderFinalTrigger:96/testNoTemplateFallsToGlobalDefault:107/testNoTemplateNoLinesManualEntry:123/testMandatoryInspectionBlockedWhenPendingClearedWhenAccepted:131——**强**，覆盖 UC-QA-01/07 触发 + 模板优先级 + 强制门控）。
  - `TestErpQaNcrPosting.java`（7 @Test：testScrapAutoPostedOnResolve:86/testReturnDispositionOrchestratesPurchaseReturn:111/testConcessionNotPostable:125/testManualPostingModeDefersUntilPostNcr:136/testDuplicatePostingRejected:152/testReverseNcrClearsPostedAndRedOffset:165/testPostingBeforeResolvedRejected:179——**强**，覆盖 UC-QA-02 退货处置 + 过账）。
  - **⚠️ 潜在测试缺口**：UC-QA-04 完工返工跨域触发链（FINAL-REJECTED→返工工单）零 dedicated 测试（testWorkOrderFinalTrigger:96 仅核触发创建质检单，未核 REJECTED→返工工单）；UC-QA-06 关键项否决零覆盖（无 isCritical 字段可 seed，testRejectedCriticalGoesRejected:84 实为 allowConcession=false 通用拒绝）；UC-QA-08 cancelForBusinessBill（R1.20 显式 Deferred 的 Facade，实测缺失）故无 dedicated 测试。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12）：quality 状态机核心契约经实仓逐项证据确认——**强制质检门控**（InspectionTrigger.enforceGate config-gated + isInspectionCleared 业务域 confirm/DONE 前校验，DAG 无环）、**NCR 过账引擎齐全**（SCRAP/RETURN/CONCESSION/DOWNGRADE）、**reverseNcr 红冲闭环对称**。
  - A2.12 关键 finding（直接相关本切片）：`P0-MA2-017`（ErpQaInspectionBizModel passInspection/failInspection/reInspect 三方法完全缺状态守卫——**resolved**，已注入异步 fix plan，本切片复核状态守卫现状）、`P1-MA2-064`（业务单据作废联动取消质检单 **R1.20 显式 Deferred（owner-doc 正式化，非方案 A 实现），UC-QA-08 直接相关——本审计不重开 MA2 finding，从 L1 视角登记新 P1-RC + §4 三判据分类**）、`P1-MA2-065`（dict 死状态 + CRUD 桩 **resolved R1.20**）、`P2-MA2-063`（state-machine.md 缺独立章节 watch-only）、`P2-MA2-064`（「事件驱动」vs「业务域查 quality 结果」文档未同步 watch-only）。
  - `docs/audits/2026-07-2*-arm-ma4-*`（A4.5）：P1-MA1-012（qa businessDate propId 缺失状态机角度无升级）+ P1-MA1-022（跨域只读 daoFor ErpInvStockBalance 异常路径经 @BizMutation 事务回滚覆盖）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用 P0-MA2-017（inspection 状态守卫 resolved）+ A2.12 状态机/门控/过账已证实行为（P1-MA2-064 作废联动行为本身经 R1.20 显式 Deferred，本审计不重审该行为，只补 L1 视角差异），只补需求视角差异（UC-QA-04 完工返工跨域触发链 / UC-QA-06 关键项否决缺失 / UC-QA-07 类别级模板解析缺失 / UC-QA-08 L1 验收标准经 deferral 未满足→新 P1-RC + arm-index 状态澄清）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P0-MA2-017`（inspection 状态守卫 **resolved**）、`P1-MA2-064`（作废联动 **R1.20 显式 Deferred（非方案 A 实现）**，UC-QA-08——本审计不重开，从 L1 视角登记新 P1-RC + arm-index 状态澄清）、`P1-MA2-065`（dict 死状态 **resolved R1.20**）、`P1-MA1-012`（businessDate propId 非状态机）、`P1-MA1-022`（跨域只读 daoFor 事务回滚覆盖）、`P2-MA2-063/064`（文档 watch-only）。**RC 系列对 quality 为零**（本切片为 quality 域首批 RC 切片）。本切片须 grep arm-index qa inspection/NCR/concession/critical/template/void 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。**UC-QA-06 关键项否决修复触及 ORM 结构变更**（ErpQaInspectionTemplateLine/ErpQaInspectionLine 加 isCritical 列）→ **须 ask-first + 独立 plan-audit**；UC-QA-04 返工跨域链 / UC-QA-07 类别级模板属**代码逻辑**类（预授权）；UC-QA-08 若确认 Facade 缺失则修复属**代码逻辑**类（预授权——新增 Facade + 业务域 cancel 回调）。

- **剩余差距**：A1.31 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-QA-04 完工返工跨域触发链是潜在需求符合性风险（不合格完工品返工闭环是否完整）。本计划产出 A1.31 报告并登记 finding，解除 quality 域首批 RC 切片证据缺口。

## Goals

- 产出 A1.31 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-31-quality-f1-inspection-gating.md`，含方法论 §6 **9 段全部内容**。
- 对 7 UC（UC-QA-01/02/03/04/06/07/08）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并、禁止跳号。
- 对候选缺口给出分级结论：①**UC-QA-06 关键项否决完全缺失**（无 isCritical 字段 + aggregate 无否决逻辑，倾向 **P1**，须人工确认 product-scope 是否要求关键项否决，修复触及 ORM ask-first）；②**UC-QA-08 作废联动 R1.20 显式 Deferred**（IErpQaInspectionBiz 无 cancelForBusinessBill + grep 零命中，**非丢失合入——R1.20 plan:37/64 + state-machine.md:190 显式裁决 Deferred**）——从 L1 视角登记 **新 `P1-RC-xxx`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类（判据 (i) R1.20 plan-audit 通过记录成立 → 倾向 **P2** documented simplification with approval；声明 Q4=(a) 张力，若 L1 为硬性 P1 要求则须人工确认 product-scope）+ arm-index 状态澄清（:354「resolved R1.20 done」须披露为 resolved-via-deferral 而非 resolved-via-implementation）；**不「重开」P1-MA2-064**（§去重协议：不重审 audit-remediation MA2 行为）；③UC-QA-04 完工返工跨域触发链（倾向**P2 successor** 或**接受**，须 §4 三判据复核 inspection-integration.md §二/§7.2 + 交叉引用 A1.9 UC-MFG-09 结论）；④UC-QA-07 类别级模板解析缺失（倾向**P2**，主路径 OK）；⑤UC-QA-01/02/03 主路径已实现复核接受——按 §2 判据定级，P0/P1 新建 `P1-RC-xxx`（自 P1-RC-040+ 起，与 A1.30/A1.32 协调序号）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/inspection-integration.md/state-machine.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.32 NCR-CAPA 闭环独立 plan；A1.33 SPC 与看板独立；A1.31 只覆盖 UC-QA-01/02/03/04/06/07/08）。
- **不重审 NCR-CAPA 闭环主路径**（UC-QA-05 属 A1.32，本切片仅核 REJECTED→NCR 创建 + RETURN 处置，不重审 CAPA 闭环）。
- **不复审 UC-QA-09/10/11/12**（SPC + 看板属 A1.33）。
- **不重跑 P0-MA2-017 / P1-MA2-064/065 的 MA2 行为审计**（§去重协议：不重审 audit-remediation MA2 状态机行为；P1-MA2-064 经 R1.20 显式 Deferred 属该 mission 裁决，本审计不重开该 finding，只从 L1 视角登记新 P1-RC + arm-index 状态澄清 + §4 三判据分类）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.31 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.31 UC 锚点）+ `docs/design/quality/use-cases.md`（L1 真相源）+ `docs/design/quality/inspection-integration.md`+`state-machine.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.12/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-quality/erp-qa-service -Dtest=TestErpQaInspectionStateMachine,TestErpQaInspectionTrigger,TestErpQaNcrPosting`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-31-quality-f1-inspection-gating.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-QA-01/02/03/04/06/07/08 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/:33/:50/:67/:101/:116/:133` 验收标准原文；L2 引用 `inspection-integration.md §一/§二/§三/§四/§五`+`state-machine.md`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpQaInspectionBizModel.java`/`ErpQaInspectionCreateForBusinessBillProcessor.java`/`ErpQaInspectionFailInspectionProcessor.java`/`ErpQaInspectionPassInspectionProcessor.java`/`InspectionResultEvaluator.java`/`InspectionTemplateMatcher.java`/`NcrLifecycleService.java`（含行号）；L4 引用对应 `Test*.java#method`（注明断言强度）；L5 复用 A2.12（P0-MA2-017/P1-MA2-064 resolved）+ A4。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-QA-01 强制质检阻塞（enforceGate + isInspectionCleared:88-95，PENDING 阻塞/ACCEPTED 放行/REJECTED 阻塞）；②UC-QA-02 REJECTED→NCR+退货（FailInspectionProcessor + NcrReturnOrchestrator→IErpPurReturnBiz，testReturnDispositionOrchestratesPurchaseReturn:111）；③UC-QA-03 让步 CONDITIONAL（InspectionResultEvaluator:73-92 allowConcession→CONDITIONAL）；④**UC-QA-04 完工返工跨域触发链**（grep rework/ReworkOrder/createRework/IErpMfg 零命中复核 + 交叉引用 A1.9 UC-MFG-09 结论 + inspection-integration.md §2.3/§7.2 §4 三判据）；⑤**UC-QA-06 关键项否决缺失**（ORM 无 isCritical 字段复核 + InspectionResultEvaluator.aggregate:73-92 无否决逻辑[关键项不合格+allowConcession→错误 CONDITIONAL]复核 + testRejectedCriticalGoesRejected:84 实为 allowConcession=false 通用拒绝复核 + §2 P1①/② 定级 + product-scope 人工确认）；⑥UC-QA-07 模板优先级（InspectionTemplateMatcher:19-35 仅两级 materialId→global，类别级缺失复核 + §2 P2① 定级）；⑦**UC-QA-08 作废联动 R1.20 显式 Deferred**（IErpQaInspectionBiz 无 cancelForBusinessBill + grep 跨 module-quality/purchase/sales/mfg 零命中 + **R1.20 plan:37/64 + state-machine.md:190 显式裁决 Deferred 非「未合入/回退」**复核 + arm-index:354「resolved R1.20 done」未披露为 deferral 致状态澄清事项 + 从 L1 视角登记新 P1-RC + §4 三判据[判据(i) R1.20 plan-audit 通过成立→倾向 P2 documented simplification；声明 Q4=(a) 张力]+ 不重开 P1-MA2-064[§去重协议]）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：UC-QA-01/02/03 主路径已实现 → 倾向**接受**；**UC-QA-06 关键项否决 → 倾向 P1**（核心验收标准完全缺失，须 product-scope 人工确认）；**UC-QA-08 作废联动 → R1.20 显式 Deferred，从 L1 视角登记新 P1-RC + §4 三判据（判据(i) 成立→倾向 P2 documented simplification；声明 Q4=(a) 张力）+ arm-index 状态澄清，不重开 P1-MA2-064**；UC-QA-04 返工跨域链 → 倾向**P2 successor**/接受（若 mfg 侧闭环）；UC-QA-07 类别级 → 倾向**P2**（主路径 OK）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-QA-01/02/03/04/06/07/08 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.12/A4 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；UC-QA-06 关键项否决有明确 P1 倾向 + product-scope 人工确认范围；UC-QA-08 作废联动有明确「R1.20 显式 Deferred→L1 未满足」裁决 + 新 P1-RC 登记 + §4 三判据分类 + arm-index 状态澄清路径（**不重开 P1-MA2-064**）；UC-QA-04 返工跨域链有明确分级 + A1.9 交叉引用；UC-QA-07 类别级有 P2 倾向

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-31-quality-f1-inspection-gating.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` qa inspection/NCR/concession/critical/template/void/rework 同域同控制点后裁决——**UC-QA-08**：P1-MA2-064 在 audit-remediation mission 经 R1.20 显式 Deferred（非本审计重审对象，§去重协议不重开 MA2 行为）；本审计从 **L1 视角**登记**新 `P1-RC-xxx`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类（判据(i) R1.20 plan-audit 通过成立→倾向 P2）+ arm-index 状态澄清（:354 须披露 resolved-via-deferral）；UC-QA-06 关键项否决 + UC-QA-04 返工跨域链 + UC-QA-07 类别级为**新发现**（既有 arm-index 无 RC finding 涉及 qa 关键项否决/返工跨域触发/类别级模板）→ 确认为分歧则新建 `P*-RC-xxx`（与 A1.30/A1.32 协调序号）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 FINAL-REJECTED 后 manufacturing 侧是否实际建返工工单的运行时行为、cancelForBusinessBill 在业务域 cancel 路径的实际触发、类别级模板查询实际命中、强制质检门控 config-gated 默认值实际生效等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12 P0-MA2-017 inspection 状态守卫 resolved + 门控/过账已证实行为；P1-MA2-064 作废联动行为经 R1.20 显式 Deferred，本审计不重审该行为），列明只补的需求视角差异（UC-QA-04 返工跨域链 / UC-QA-06 关键项否决缺失 / UC-QA-07 类别级模板缺失 / UC-QA-08 L1 验收标准经 deferral 未满足→新 P1-RC + arm-index 状态澄清）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.31 行。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding（若有）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_030db68e3ffeOx7bi0CEF7hHQc，fresh session，未起草本计划）。Blocking issues 经主代理实仓复核全部 CONFIRMED：①**UC-QA-06 关键项否决完全缺失**——ORM `ErpQaInspectionLine`/`ErpQaInspectionTemplateLine` 无 `isCritical`/`关键项` 列（仅 `isRequired`=是否必检，语义不同），`InspectionResultEvaluator.aggregate:73-92` 无否决逻辑（关键项不合格+allowConcession=true 错误产出 CONDITIONAL，违反 L1 `:109`），`testRejectedCriticalGoesRejected:84` 实为 allowConcession=false 通用拒绝（无关键项标记可 seed），原基线「✅ 已实现」误判（Q1 根因：测试名误导）→ 已纠正为候选 **P1**；②**UC-QA-08 cancelForBusinessBill Facade 实测缺失**——`IErpQaInspectionBiz`（85 行）无该方法 + grep 跨 module-quality/purchase/sales/mfg 零命中，但 arm-index:354 标 P1-MA2-064「resolved R1.20 done」属 **arm-index 漂移** → 已纠正为高优先核查（确认缺失则 P1-MA2-064 重开/新 P1-RC）；③**UC-QA-07 类别级 lookup 缺失**——`InspectionTemplateMatcher:19-35` 仅两级 materialId→global，L1 要求三级 → 已纠正为候选 **P2**。另修正 TestErpQaNcrPosting 测试数 8→7。已据上述修订 Current Baseline / Goals / Phase 1 / Exit Criteria / 保护区域段。
- Independent draft review iteration 2: `needs revision`（独立子代理 ses_030d364d5ffe0j4YigQdlwyzuy，fresh session）。iteration-1 blocking issues UC-QA-06（候选 P1）/UC-QA-07（候选 P2）resolved 并 live-verified；UC-QA-08 表面框架（「✅ 已实现」→初步缺失）fixed。**但发现 2 项 NEW blocking**：①**内部矛盾**——baseline/Goals/Phase1（L33/57/94）说「确认缺失则 P1-MA2-064 重新打开或新建 P1-RC」而 Phase2/L5/Non-Goals（L113/43/45/68）说「P1-MA2-064 resolved→复用注记（不重开）」；②**事实错误**——L33 臆测「R1.20 Facade 未合入/被回退」被实仓证伪：R1.20 plan `2026-07-30-0512-3:37,64` + `state-machine.md:190` 显式裁决 P1-MA2-064 **Deferred**（owner-doc 正式化，非方案 A 实现，successor 命名触发条件），Facade 缺失是 deliberate deferral 非 lost merge；drift 在 arm-index:354「resolved (R1.20 done)」未披露为 deferral。已据以修订：删除「未合入/回退」臆测；统一全段（baseline/Goals/Phase1/Phase2/L5/§9/Non-Goals/Exit Criteria）为单一一致立场——**不重开 P1-MA2-064（§去重协议：不重审 audit-remediation MA2 行为），从 L1 视角登记新 P1-RC + §4 三判据分类（判据(i) R1.20 plan-audit 通过成立→倾向 P2 documented simplification；声明 Q4=(a) 张力）+ arm-index 状态澄清（resolved-via-deferral 而非 resolved-via-implementation）**。
- Independent draft review iteration 3: `accept`（独立子代理 ses_030cec6eeffe7DLoN9sBsXqceI，fresh session，未起草本计划）。iteration-2 两项 blocking 全部 resolved 并独立 live-verified：(B1) UC-QA-08/P1-MA2-064 全部 11+ 触点（Current Baseline L33 / L5 L43,45 / arm-index-linkage L47 / Goals L57 / Non-Goals L68 / Phase1 L94,96 / Exit Criteria L102 / Phase2 L113,121）现持单一一致立场「不重开 P1-MA2-064（§去重）+ 从 L1 视角登记新 P1-RC + §4 三判据（判据(i) R1.20 plan-audit 通过→倾向 P2 documented simplification；声明 Q4=(a) 张力）+ arm-index 状态澄清（resolved-via-deferral）」；(B2) deferral 事实独立复核于 `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md:37,64` + `state-machine.md:190`（均显式标 P1-MA2-064 Deferred 非实现），plan 现正确表述缺失为 deliberate deferral，「未合入/被回退」仅存于显式否定语境。iteration-1 修订（UC-QA-06 候选 P1、UC-QA-07 候选 P2）保持。无新 blocking；已据 1 项非阻塞措辞观察收紧 L39（「R1.20 声称新增 Facade」→「R1.20 显式 Deferred 的 Facade」）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.31 报告 9 段齐全 + 7 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.31 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选缺口（完工返工跨域触发链 / 类别级模板）均属**代码逻辑**类（预授权——跨域 Facade/事件接线/查询补全，不涉及 ORM 结构变更）；若涉及 NCR 过账逻辑则须 ask-first。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <待完成时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
