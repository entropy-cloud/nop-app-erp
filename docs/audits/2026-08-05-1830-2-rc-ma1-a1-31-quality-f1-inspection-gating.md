# A1.31 quality-F1 检验门控 需求-实现符合性五级追踪审计报告

> 报告类型：MA1（RC）需求-实现符合性五级追踪审计
> 切片：A1.31 quality-F1 检验门控（强制质检阻塞 / 不合格退货 / 让步 / 完工返工 / 关键项否决 / 模板优先级 / 作废联动）
> 审计范围：UC-QA-01 / UC-QA-02 / UC-QA-03 / UC-QA-04 / UC-QA-06 / UC-QA-07 / UC-QA-08（7 UC，逐 UC 一矩阵行，禁止合并/跳号）
> 真相源层级（§4 Q1）：L1 = `docs/design/quality/use-cases.md`（UC-QA-01 `:15` / 02 `:33` / 03 `:50` / 04 `:67` / 06 `:101` / 07 `:116` / 08 `:133`）；L2 = `inspection-integration.md` + `state-machine.md`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.12 + A4.5 + 本切片差异。
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 结论速览：⚠️(P1) — 7 UC 中 UC-QA-01/02/03 **接受**（主路径已实现 & 强测）、UC-QA-04 **倾向接受 / P2 successor**（mfg 侧闭环经 A1.9 证实）、UC-QA-06 **P1**（关键项否决核心验收标准完全缺失）、UC-QA-07 **P2**（类别级模板缺失）、UC-QA-08 **P2（documented simplification with approval）+ 新 P1-RC 登记**（L1 验收标准经 R1.20 deferral 未满足）。**零 P0**。**新登记 4 项 finding**：`P1-RC-040`（UC-QA-06 关键项否决缺失，须人工确认 product-scope，修复触及 ORM ask-first）/ `P1-RC-041`（UC-QA-08 L1 验收标准经 deferral 未满足→§4 三判据[i] 成立倾向 P2 documented simplification；声明 Q4=(a) 张力；arm-index:361 状态澄清）/ `P2-RC-040`（UC-QA-07 类别级模板解析缺失）/ `P2-RC-041`（UC-QA-04 完工返工跨域触发链 successor）。

---

## 9. 与既有 MA2 报告差异增量声明（§6 段落 9，置顶便于去重）

本切片为 quality 域**首批 RC 切片**（A1.31/A1.32/A1.33 三切片，本切片覆盖 UC-QA-01/02/03/04/06/07/08）。按 §去重协议，本报告**不复跑** MA2 状态机行为审计，直接复用 `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12）已证实行为作为 L5 既有证据输入，只补"需求契约↔实际行为"差异：

- **复用 A2.12 已证实行为**：强制质检门控（`InspectionTrigger.enforceGate` config-gated 同步 I\*Biz 写触发 + `isInspectionCleared` 业务域 confirm/DONE 前校验，DAG 无环）、NCR 过账引擎齐全（SCRAP/RETURN/CONCESSION/DOWNGRADE）、`reverseNcr` 红冲闭环对称、**P0-MA2-017 inspection 状态守卫 resolved**（passInspection/failInspection/reInspect 三方法已注入 `result==PENDING` 守卫 + 设 posted=true + failInspection 触发 autoCreateNcrFromInspection + reInspect 删除，复检走 createForBusinessBill 新建关联单）。
- **本切片不重开 MA2 finding**（§去重协议）：
  - **P1-MA2-064**（业务单据作废联动取消）经 **R1.20 显式裁决 Deferred**（owner doc 正式化，非方案 A 实现，详见 `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md:37,64` + `state-machine.md:190`）。本审计**不重开 P1-MA2-064**（不重审 audit-remediation MA2 行为裁决），而是从 **L1 视角**登记**新 `P1-RC-041`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类（判据 (i) R1.20 plan 含独立 plan-audit 通过记录成立 → 倾向 P2 documented simplification with approval；声明 Q4=(a) 张力）+ **arm-index 状态澄清**（:361「✅ resolved (R1.20 done)」未披露该 resolution 是 *deferral* 而非 *implementation*，属误导性「done」标签）。
  - **P1-MA2-065**（dict 死状态 + CRUD 桩）经 R1.20 Deferred，属 MA2 行为裁决，本审计不重审（与 UC-QA-01/02/03/04/06/07/08 验收标准无直接投影）。
- **本切片只补的需求视角差异**：① UC-QA-04 完工返工跨域触发链（quality 侧仅生成 NCR，未直接跨域建返工工单；交叉引用 A1.9 UC-MFG-09 闭环结论）；② UC-QA-06 关键项否决**完全缺失**（ORM 无 isCritical 字段 + aggregate 无否决逻辑，根因 = 代理转述已向实现妥协的 Q1 典型）；③ UC-QA-07 类别级模板解析缺失（InspectionTemplateMatcher 仅两级）；④ UC-QA-08 L1 验收标准经 deferral 未满足→新 P1-RC-041 + arm-index 状态澄清。

---

## 1. 需求契约原文（逐字引用，§1 L1 格式）

> 来源 `docs/design/quality/use-cases.md`，逐字引用验收标准（禁止转述）。

### UC-QA-01 来料强制质检阻塞流转（`:15`）
```
物料.inspection_required == 强制 →
  采购入库审核 → 发布事件(INCOMING) → 创建质检单(PENDING)
  入库流程阻塞(等待质检结果)
质检单 PENDING 期间: 入库单不可继续后续(过账/可用)
质检单 ACCEPTED → 入库继续流转
质检单 REJECTED → 触发退货(见 inspection §二)
```

### UC-QA-02 质检不合格触发退货（`:33`）
```
质检单 REJECTED(关键项不合格) →
  创建 NCR(OPEN)
  触发退货: 关联入库单生成退货单
NCR 记录不合格详情
退货走 ../purchase/returns.md 流程
```

### UC-QA-03 让步接收（`:50`）
```
质检单(非关键项不合格) →
  让步流程: 建议 → 审批 → CONDITIONAL
  记录让步原因/审批人
CONDITIONAL: 业务单据继续流转, 但标记让步
关键项不合格 → 不可让步(直接 REJECTED)
```

### UC-QA-04 完工检验不合格→返工（`:67`）
```
完工质检(FINAL) REJECTED →
  不合格处理路径: 返工(见 §二 路径表)
  触发 ../manufacturing 新建返工工单(关联原工单)
```

### UC-QA-06 关键项否决（`:101`）
```
质检模板行.是否关键项 == true 且 该行不合格 →
  整体质检单 = REJECTED(关键项否决,无论其他项)
非关键项不合格 → 可让步(CONDITIONAL)
```

### UC-QA-07 质检模板优先级解析（`:116`）
```
优先级: 物料级模板 > 物料类别级 > 全局默认
物料有专属模板 → 用专属
否则 查类别模板 → 用类别
否则 用全局默认
模板缺失且强制质检 → 报错或用最小默认
```

### UC-QA-08 业务单据作废联动取消质检（`:133`）
```
入库单.作废 →
  关联质检单(若 PENDING) → 取消(CANCELLED)
  不影响已 ACCEPTED/REJECTED 的质检单(历史完整)
```

**断言计数**：UC-QA-01 ×4（强制质检触发 / PENDING 阻塞 / ACCEPTED 放行 / REJECTED 触发退货）+ UC-QA-02 ×4（REJECTED→NCR / 关联入库单生成退货单 / NCR 记录详情 / 走 purchase returns）+ UC-QA-03 ×4（让步流程→CONDITIONAL / 记录原因审批人 / CONDITIONAL 继续流转 / 关键项不可让步）+ UC-QA-04 ×2（FINAL REJECTED→返工 / 触发 manufacturing 新建返工工单关联原工单）+ UC-QA-06 ×2（关键项不合格→整体 REJECTED / 非关键项→可让步）+ UC-QA-07 ×4（物料级>类别级>全局 / 专属优先 / 否则类别 / 否则全局）+ UC-QA-08 ×2（作废→PENDING 质检单 CANCELLED / 不影响 ACCEPTED/REJECTED）= **22 条验收标准**（逐 UC 完整枚举，无合并无跳号）。

---

## 2. 实现证据（代码路径，§1 L3 格式，含跨域调用链）

> 全部 `module-quality/erp-qa-service/src/main/...` 或 `module-quality/erp-qa-dao/src/main/...`，含行号。

| 控制点 | 代码路径（file:line） | 备注 |
|--------|----------------------|------|
| 强制质检门控触发 | `InspectionTrigger.java:35-50`（`enforceGate`：mandatory bill type 且无关联质检单 → `IErpQaInspectionBiz.createForBusinessBill` 生成 PENDING 质检单 + 返回 BLOCKED；config-gated `erp-qua.mandatory-inspection-bill-types`） | config-gated，默认空=不强制；`isMandatoryBillType:53` |
| 业务域 confirm/DONE 前校验 | `ErpQaInspectionBizModel.java:79-94`（`isInspectionCleared`：PENDING→false / ACCEPTED+CONDITIONAL→true / REJECTED→false） | 业务域 Processor 在 confirm/DONE 前查（business→quality 只读，DAG 无环） |
| 业务触发生成质检单 | `ErpQaInspectionCreateForBusinessBillProcessor.java:22-54`（建 ErpQaInspection `result=PENDING :38` + copyTemplateLinesToInspection 行 PENDING `:67`） | INCOMING/IN_PROCESS/FINAL/OUTGOING 通用 |
| 模板匹配 | `InspectionTemplateMatcher.java:30-47`（match：materialId×inspectionType→active 模板 `:31` → 回落 `erp-qua.default-inspection-template` 全局默认 `:33-35` → null） | **仅两级**（物料级→全局），**无类别级** |
| 行级评测 + 结果汇总 | `InspectionResultEvaluator.java:28-52`（evaluateLine：specMin/specMax vs measuredValue 数值比较，越界→REJECTED；无数值规格→实测非空即 ACCEPTED）+ `aggregate:73-92`（全行 ACCEPTED→ACCEPTED `:87-88`；含 REJECTED 且 allowConcession→CONDITIONAL `:90-91`；含 REJECTED 且未让步→REJECTED `:92`） | **无关键项否决逻辑**——对所有 REJECTED 行统一处理，关键项不合格+allowConcession=true → 错误产出 CONDITIONAL |
| recordResult 编排 | `ErpQaInspectionRecordResultProcessor.java:55-67`（concession=allowConcession `:55` → aggregate `:56` → setResult `:57` → CONDITIONAL 记录让步 `:59` → REJECTED 自动建 NCR `:67`） | UC-QA-03 让步 CONDITIONAL 入口 |
| REJECTED→NCR | `ErpQaInspectionFailInspectionProcessor.java:19-27`（requireInspectionPending 守卫 `:21` → setResult(REJECTED) `:22` → markPosted `:23` → `ncrLifecycleService.autoCreateNcrFromInspection:25`）+ `NcrLifecycleService.autoCreateNcrFromInspection:49` | P0-MA2-017 修复后状态守卫 + posted 三件套 + 自动建 NCR |
| NCR RETURN→采购退货 | `NcrReturnOrchestrator.orchestrateReturn:72`（supplierId 非空→`IErpPurReturnBiz.save` Facade，退货单自带红字过账 PURCHASE_RETURN + 负辅助账；`NcrReturnOrchestrator.java:48,57` 跨域 Bean 延迟查找） | UC-QA-02 退货处置 |
| passInspection 状态守卫 | `ErpQaInspectionPassInspectionProcessor.java:16-18`（requireInspectionPending → setResult(ACCEPTED) → markPosted） | P0-MA2-017 resolved |
| **关键项否决字段** | **缺失**——ORM `app-erp-quality.orm.xml:326` `ErpQaInspectionTemplateLine.isRequired`="是否必检"（propId 8），**无 `isCritical`/`criticalItem`/`关键项` 列**（grep `isCritical\|criticalItem\|关键项` 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + orm.xml **零业务命中**，仅 SPC SEVERITY_CRITICAL 无关） | **UC-QA-06 数据模型缺失**；`isRequired` 语义≠`isCritical`（必检≠关键项） |
| **业务作废联动取消 Facade** | **缺失**——`IErpQaInspectionBiz.java`（85 行，方法 recordResult/findByRelatedBill/createForBusinessBill/isInspectionCleared/passInspection/failInspection/batchPassInspection）**无 `cancelForBusinessBill`**；grep 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + `module-purchase` + `module-sales` + `module-manufacturing` `cancelForBusinessBill` **零命中** | **UC-QA-08 Facade 缺失——R1.20 显式裁决 Deferred（非丢失合入/回退），详见 §6 + arm-index 状态澄清** |
| **完工返工跨域触发** | quality 侧：FINAL REJECTED → `NcrLifecycleService.autoCreateNcrFromInspection`（仅生成 NCR，`NcrLifecycleService.java:49`）；grep `rework\|ReworkOrder\|createRework\|返工工单` 跨 `module-quality/erp-qa-service/src/main` **零业务命中**（仅 `ErpQaInspectionBizModel.java:88` 注释「业务域应触发退货/返工/NCR 处置」声明由业务域处理）。mfg 侧（A1.9 复用）：`ErpMfgWorkOrderReportCompletionProcessor:46-58` `InspectionTrigger.enforceGate` BLOCKED→工单保持 IN_PROCESS；`ErpMfgWorkOrderProcessor:69` 注入 `IErpQaInspectionBiz`（**仅门控查询 isInspectionCleared，不监听 REJECTED 建返工工单**）；**无 originalWorkOrderId 字段、无自动建返工工单代码** | **UC-QA-04 跨域返工触发链——mfg 侧闭环经 A1.9 证实（门控+操作员驱动 successor），quality 侧仅生成 NCR** |

---

## 3. 测试证据（§1 L4 格式，注明断言强度）

> `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/`。

| 测试方法 | 覆盖 UC/断言 | 强度 |
|---------|-------------|------|
| `TestErpQaInspectionStateMachine#testAllAcceptedGoesAccepted:63` | UC-QA-03 全行 ACCEPTED→ACCEPTED | 强 |
| `TestErpQaInspectionStateMachine#testPartialRejectedWithConcessionGoesConditional:73` | UC-QA-03 含 REJECTED+allowConcession→CONDITIONAL | 强 |
| `TestErpQaInspectionStateMachine#testRejectedCriticalGoesRejected:84` | **UC-QA-06 名义覆盖，实为 allowConcession=false 通用拒绝**（无 isCritical 字段可 seed，命名误导） | **弱/误导**（Q1 根因：测试名误导致基线误判） |
| `TestErpQaInspectionStateMachine#testLineSpecEvaluationParseFailureTreatedAsRejected:94` | 行级评测解析失败→REJECTED | 强 |
| `TestErpQaInspectionStateMachine#testTerminalResultCannotReRecord:115` | 终态守卫（P0-MA2-017 修复后） | 强 |
| `TestErpQaInspectionStateMachine#testPassInspectionRejectsTerminalState:127` / `#testFailInspectionRejectsTerminalState:140` | passInspection/failInspection 终态守卫 | 强 |
| `TestErpQaInspectionStateMachine#testPassInspectionFromPendingSetsPosted:153` / `#testFailInspectionFromPendingSetsPostedAndTriggersNcr:166` | posted 三件套 + failInspection 触发 NCR | 强 |
| `TestErpQaInspectionStateMachine#testReinspectionViaNewIndependentInspection:195` | 复检走 createForBusinessBill 新建（reInspect 已删除） | 强 |
| `TestErpQaInspectionStateMachine#testIsInspectionClearedFalseWhenPending:232` | UC-QA-01 门控 isInspectionCleared PENDING→false | 强 |
| `TestErpQaInspectionTrigger#testPurchaseReceiptTriggerGeneratesIncomingWithTemplateLines:68` | UC-QA-01 来料触发 INCOMING + 模板行复制 | 强 |
| `TestErpQaInspectionTrigger#testSalesOutgoingTrigger:84` / `#testWorkOrderFinalTrigger:96` | OUTGOING/FINAL 触发（**testWorkOrderFinalTrigger 仅核触发创建质检单，未核 REJECTED→返工工单**） | 强（触发）/ 缺（返工） |
| `TestErpQaInspectionTrigger#testNoTemplateFallsToGlobalDefault:107` / `#testNoTemplateNoLinesManualEntry:123` | UC-QA-07 两级回退（物料级→全局默认） | 强（两级）/ 缺（类别级） |
| `TestErpQaInspectionTrigger#testMandatoryInspectionBlockedWhenPendingClearedWhenAccepted:131` | UC-QA-01 强制门控 PENDING 阻塞 / ACCEPTED 放行 | 强 |
| `TestErpQaNcrPosting#testReturnDispositionOrchestratesPurchaseReturn:111` | UC-QA-02 RETURN→`IErpPurReturnBiz` 编排退货 | 强 |
| `TestErpQaNcrPosting#testScrapAutoPostedOnResolve:86` / `#testReverseNcrClearsPostedAndRedOffset:165` | NCR 过账 + 红冲闭环 | 强 |
| `TestErpQaNcrPosting#testConcessionNotPostable:125` / `#testPostingBeforeResolvedRejected:179` | CONCESSION 不可过账 + 未 resolve 拒过账 | 强 |

**测试缺口**：① UC-QA-04 完工返工跨域触发链零 dedicated 测试（testWorkOrderFinalTrigger:96 仅核触发）；② UC-QA-06 关键项否决零覆盖（无 isCritical 字段可 seed，testRejectedCriticalGoesRejected:84 名义覆盖实为 allowConcession=false 通用拒绝，未覆盖「关键项否决覆盖让步」路径）；③ UC-QA-08 cancelForBusinessBill（R1.20 显式 Deferred 的 Facade）故无 dedicated 测试。

---

## 4. 运行时行为证据（§1 L5 格式）

> 既有 MA2 报告已证实行为直接引用（§去重协议）。

| 控制点 | L5 行为证据来源 | 状态 |
|--------|----------------|------|
| 强制质检门控（enforceGate + isInspectionCleared） | **行为已证实（引用 A2.12）**：InspectionTrigger.enforceGate config-gated 同步 I\*Biz 写触发 + isInspectionCleared 业务域 confirm/DONE 前校验，DAG 无环 | 复用 |
| inspection 状态守卫（PENDING 单一源态 + posted 三件套 + failInspection→NCR） | **行为已证实（引用 A2.12 P0-MA2-017 resolved）**：passInspection/failInspection/reInspect 三方法已注入 result==PENDING 守卫 + posted=true + failInspection 触发 autoCreateNcrFromInspection + reInspect 删除 | 复用 |
| NCR 过账引擎（SCRAP/RETURN/CONCESSION/DOWNGRADE）+ reverseNcr 红冲对称 | **行为已证实（引用 A2.12）** | 复用 |
| NCR RETURN→采购退货编排 | **行为已证实（引用 A2.12 + A4.5）**：NcrReturnOrchestrator→IErpPurReturnBiz Facade，testReturnDispositionOrchestratesPurchaseReturn:111 强断言 | 复用 |
| UC-QA-04 完工返工跨域链 | **行为已证实（引用 A1.9 UC-MFG-09 倾向接受）**：mfg 侧 `ErpMfgWorkOrderReportCompletionProcessor:46-58` 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建返工工单（无 originalWorkOrderId 自动关联，owner doc §3 successor） | 复用（A1.9 SP-2 存疑点） |
| UC-QA-06 关键项否决 | **行为缺失（本切片新发现）**：aggregate:73-92 无关键项否决分支，关键项不合格+allowConcession=true → 错误 CONDITIONAL | **新发现**（交 SP 清单 MA4 复核运行时触发面） |
| UC-QA-07 类别级模板 | **行为缺失（本切片新发现）**：InspectionTemplateMatcher:30-47 仅两级，类别级回退缺失 | **新发现** |
| UC-QA-08 业务作废联动取消 | **行为缺失（R1.20 显式 Deferred，非丢失合入）**：IErpQaInspectionBiz 无 cancelForBusinessBill + grep 跨 quality/purchase/sales/mfg 零命中；残留质检单经 useLogicalDelete 手工清理 | **新发现（deferral 非实现）** |

---

## 5. 五级追踪矩阵 + 每 UC 符合性结论（§2 判据，取最高）

> 逐 UC 一矩阵行，禁止合并/跳号。冲突裁决：L2 与 L1 冲突一律以 L1 为准（§4 Q1）。

### UC-QA-01 来料强制质检阻塞流转

| L1（逐字） | L2（设计参考） | L3（代码） | L4（测试，强度） | L5（行为） | 冲突裁决 |
|-----------|---------------|-----------|-----------------|-----------|---------|
| `:15` 强制质检触发 / PENDING 阻塞 / ACCEPTED 放行 / REJECTED 触发退货 | `inspection-integration.md §一/§二`+`state-machine.md §1-§2`（强制质检阻塞 + PENDING 暂停流转）一致 | `InspectionTrigger.enforceGate:35-50`（mandatory + 无关联质检单→createForBusinessBill PENDING + BLOCKED）+ `ErpQaInspectionBizModel.isInspectionCleared:79-94`（PENDING/REJECTED→false 阻塞 / ACCEPTED+CONDITIONAL→true 放行） | `TestErpQaInspectionTrigger#testMandatoryInspectionBlockedWhenPendingClearedWhenAccepted:131`（**强**）+ `#testPurchaseReceiptTriggerGeneratesIncomingWithTemplateLines:68`（**强**）+ `TestErpQaInspectionStateMachine#testIsInspectionClearedFalseWhenPending:232`（**强**） | 行为已证实（引用 A2.12 强制质检门控 + isInspectionCleared） | 无冲突（L1↔L2↔L3 一致） |

**结论：接受**（§2 接受——4 条验收标准在 L3-L5 各级均有证据且一致）。config-gated 默认空=不强制属设计实现注记（`state-machine.md §实现约定`），主路径（config 启用后）行为正确。

### UC-QA-02 质检不合格触发退货

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:33` REJECTED→NCR(OPEN) / 关联入库单生成退货单 / NCR 记录详情 / 走 purchase returns | `inspection-integration.md §二/§四`+`../purchase/returns.md` 一致 | `ErpQaInspectionFailInspectionProcessor:19-27`（REJECTED→`autoCreateNcrFromInspection:25`）+ `NcrReturnOrchestrator.orchestrateReturn:72`（supplierId 非空→`IErpPurReturnBiz.save` Facade） | `TestErpQaNcrPosting#testReturnDispositionOrchestratesPurchaseReturn:111`（**强**：编排退货单 + 红字过账 PURCHASE_RETURN + 负辅助账）+ `TestErpQaInspectionStateMachine#testFailInspectionFromPendingSetsPostedAndTriggersNcr:166`（**强**） | 行为已证实（引用 A2.12 NCR 过账引擎齐全 + A4.5 NcrReturnOrchestrator） | 无冲突 |

**结论：接受**（§2 接受——4 条验收标准全证据一致）。

### UC-QA-03 让步接收

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:50` 让步流程→CONDITIONAL / 记录原因审批人 / CONDITIONAL 继续流转 / **关键项不合格→不可让步直接 REJECTED** | `inspection-integration.md §三`+`state-machine.md §2` 一致（非关键项不合格 + 审批→CONDITIONAL） | `InspectionResultEvaluator.aggregate:73-92`（含 REJECTED + allowConcession→CONDITIONAL `:90-91`）+ `ErpQaInspectionRecordResultProcessor:55-67`（CONDITIONAL 记录让步 `:59`） | `TestErpQaInspectionStateMachine#testPartialRejectedWithConcessionGoesConditional:73`（**强**）+ `#testAllAcceptedGoesAccepted:63`（**强**） | 行为已证实（引用 A2.12 让步 CONDITIONAL 主路径） | **L1「关键项不合格→不可让步」断言在本 UC 范围内只声明，实际否决逻辑归属 UC-QA-06（见下，P1 缺失）。让步 CONDITIONAL 主路径（非关键项场景）与 L1 一致** |

**结论：接受 on ①②③（让步 CONDITIONAL 主路径已实现 & 强测）**。**关键项否决维度（L1 `:60`「关键项不合格→不可让步」）的裁决归 UC-QA-06**（同一数据模型 + 同一 evaluator 控制点，UC-QA-03 与 UC-QA-06 共享 `aggregate:73-92`，按 §7 复用不新建——UC-QA-06 的 P1-RC-040 同时覆盖 UC-QA-03 的关键项否决断言）。

### UC-QA-04 完工检验不合格→返工

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:67` FINAL REJECTED→返工 / **触发 ../manufacturing 新建返工工单（关联原工单）** | `inspection-integration.md §二/§2.3 路径表:90`「完工检验不合格 \| 返工/报废 \| 返工工单/报废单」+ `§7.2:265`「REJECTED \| 触发返工/报废」（事件解耦，quality DAG 无环不反向依赖 business）+ `state-machine.md §场景D`（不合格→反馈制造域→新建返工工单关联原工单）一致 | quality 侧：`ErpQaInspectionFailInspectionProcessor:19-27`（FINAL REJECTED→NCR via `autoCreateNcrFromInspection:25`，**不直接跨域建返工工单**）；mfg 侧（A1.9 复用）：`ErpMfgWorkOrderReportCompletionProcessor:46-58`（enforceGate BLOCKED→工单保持 IN_PROCESS）+ `ErpMfgWorkOrderProcessor:69`（注入 IErpQaInspectionBiz **仅门控查询**，不监听 REJECTED 建返工工单）+ **无 originalWorkOrderId 字段、无自动建返工工单代码** | `TestErpQaInspectionTrigger#testWorkOrderFinalTrigger:96`（**强**：FINAL 触发创建质检单）— **零 dedicated 测试核 REJECTED→返工工单**；mfg 侧 `TestErpMfgWorkOrderEndToEnd#testInspectionGateBlocksCompletionWhenEnabled:144-185`（**强**：门控 + 工单保持 IN_PROCESS） | 行为已证实（引用 A1.9 UC-MFG-09 倾向接受）：mfg 门控闭环（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建返工工单（owner doc §3 successor）；quality 侧仅生成 NCR | L1↔L2 一致（§7.2 事件解耦）；**L1 `:75`「触发 manufacturing 新建返工工单（关联原工单）」与实现（操作员驱动简化范式，无自动建单+无 originalWorkOrderId）存在张力——A1.9 已从 mfg 视角裁决倾向接受（owner doc §3 + §场景D 与 L1 一致声明该路径，设计裁定 successor 触发条件）** |

**结论：倾向接受 / P2 successor**（§2 P2③——mfg 侧闭环经 A1.9（done）证实：门控（工单保持 IN_PROCESS）+ 操作员手动新建返工工单承载「返工」语义；quality 侧生成 NCR + mfg 门控构成跨域闭环（事件解耦 per §7.2，quality 不反向依赖 business）；「自动建返工工单 + originalWorkOrderId 关联」属 owner doc §3 已裁定的 successor 触发条件，非阻断主路径）。**§4 三判据复核**：(i) A1.9 plan 含独立 plan-audit 通过记录（A1.9 done，Draft Review Record 存在）→ documented simplification 成立 → 倾向 P2。**登记 `P2-RC-041`**（quality 侧投影，交叉引用 A1.9 UC-MFG-09 + A1.9 SP-2 存疑点；不重开 A1.9 结论，按 §去重协议 MA1 同 mission 兄弟切片互补不重复）。

### UC-QA-06 关键项否决

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:101` 质检模板行.是否关键项==true 且该行不合格→整体 REJECTED（关键项否决，无论其他项）/ 非关键项不合格→可让步 CONDITIONAL | `inspection-integration.md §五/§5.3 检验读数判定:219-222`「关键项不合格→整体不合格 / 非关键项不合格→可让步接收」+ `§十 关键业务规则总结:333`「关键项否决：关键检验项不合格则整体判定为不合格」一致 | **数据模型缺失**：ORM `ErpQaInspectionTemplateLine`（`app-erp-quality.orm.xml:326`）仅 `isRequired`="是否必检"（propId 8），**无 `isCritical`/`关键项` 列**（grep `isCritical\|criticalItem\|关键项` 跨 quality dao/service + orm.xml **零业务命中**；`isRequired` 语义≠关键项——必检≠关键项）。**评估器逻辑缺失**：`InspectionResultEvaluator.aggregate:73-92` **无关键项否决分支**——对所有 REJECTED 行统一处理 `anyRejected && allowConcession → CONDITIONAL (:90-91)`，**关键项不合格 + allowConcession=true 错误产出 CONDITIONAL，直接违反 L1 `:108-109`「关键项不合格→不可让步→直接 REJECTED」**。所引 `evaluateLine:43-49` 是通用 out-of-spec REJECTED 探测，非关键项逻辑 | `TestErpQaInspectionStateMachine#testRejectedCriticalGoesRejected:84`（**命名误导/弱**——实为 `allowConcession=false` 通用拒绝断言，无关键项标记可 seed，未覆盖「关键项否决覆盖让步」路径；Q1 根因：测试名误导致基线误判） | **行为缺失**（本切片新发现）：关键项不合格 + allowConcession=true 运行时产出 CONDITIONAL，违反 L1 | L1↔L2 一致（均要求关键项否决）；**L3 完全缺失（数据模型 + 评估器逻辑双缺）→ 推定实现未落地核心验收标准** |

**结论：P1**（§2 P1①「功能完全缺失」——核心验收标准完全缺失：数据模型无 isCritical 字段 + 评估器 aggregate 无关键项否决分支；§2 P1⑤「验收标准零断言」——testRejectedCriticalGoesRejected:84 命名误导实为通用拒绝）。**须人工确认 product-scope 是否要求关键项否决**（`docs/requirements/product-scope.md` quality 域 + `inspection-integration.md §十:333`「关键项否决」+ §5.3:219-222 均显式声明，L1 明确要求 → 倾向 P1 强制实现 Q4 无例外）。**登记 `P1-RC-040`**（修复触及 ORM 结构变更[ErpQaInspectionTemplateLine + ErpQaInspectionLine 加 isCritical 列] + aggregate 增关键项否决分支 → **须 ask-first + 独立 plan-audit §5 ORM 结构变更类**）。

### UC-QA-07 质检模板优先级解析

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:116` 优先级 物料级 > **物料类别级** > 全局默认 / 物料有专属→用专属 / 否则查类别→用类别 / 否则用全局默认 / 模板缺失且强制质检→报错或用最小默认 | `inspection-integration.md §5.2:199-205` 一致（物料级别→物料类别级别→全局默认） | `InspectionTemplateMatcher.match:30-47`（**仅两级**：materialId×inspectionType→active 模板 `:31` → 回落全局默认 `erp-qua.default-inspection-template` `:33-35` → null 人工补录）；`findActiveByMaterialAndType:50-64` 仅按 materialId 过滤（**无物料类别级查询**） | `TestErpQaInspectionTrigger#testNoTemplateFallsToGlobalDefault:107`（**强**：两级回退）+ `#testNoTemplateNoLinesManualEntry:123`（**强**：无模板人工补录）— **类别级零覆盖** | 主路径（专属模板/全局默认）已证实（引用 A2.12 + 本切片）；**类别级回退行为缺失** | L1↔L2 一致（均要求三级）；**L3 仅两级（物料类别级缺失）→ 次要验收标准未完全满足，主路径 OK 边界弱** |

**结论：P2**（§2 P2①「次要验收标准未完全满足，主路径 OK，边界场景弱」——主路径[专属模板/全局默认]已实现 & 强测，类别级[物料类别级]查询缺失）。**登记 `P2-RC-040`**（修复 = InspectionTemplateMatcher 增类别级回退查询[按 material.categoryId 查 active 模板]，纯代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first）。

### UC-QA-08 业务单据作废联动取消质检

| L1（逐字） | L2 | L3 | L4（强度） | L5 | 冲突裁决 |
|-----------|----|----|-----------|----|---------|
| `:133` 入库单.作废→关联质检单（若 PENDING）→取消 CANCELLED / 不影响已 ACCEPTED/REJECTED | `state-machine.md §4 异常路径:50`「业务单据作废联动：关联业务单据作废时，未完成的质检单自动取消（**Deferred**——见 §实现约定 + §CRUD 桩实体状态机，残留经 useLogicalDelete 手工清理，successor：业务作废自动取消质检需求时）」+ `§实现约定:190`「业务单据作废联动取消（Deferred）……本期不落地」 | **Facade 缺失**：`IErpQaInspectionBiz.java`（85 行）无 `cancelForBusinessBill`；grep 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + `module-purchase` + `module-sales` + `module-manufacturing` `cancelForBusinessBill` **零命中**。**关键事实**：R1.20 plan `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md:37,64` **显式裁决 P1-MA2-064 为 Deferred**（owner doc 正式化，非方案 A 实现）——理由：方案 A 属跨域 wiring（purchase/sales/mfg cancel Processor config-gated 调用）跨表面实现，与危害（TODO 噪音，不破坏主路径 + 残留经 useLogicalDelete 手工清理）不成比例；successor：业务作废自动取消质检需求时 | 无 dedicated 测试（Facade 经 R1.20 显式 Deferred 缺失） | **行为缺失（R1.20 显式 Deferred，非丢失合入/回退）**：业务单据作废后 PENDING 质检单残留（CANCELLED 业务单据不再流转，故不破坏主路径；残留质检单经 useLogicalDelete 手工清理） | L1↔L2 张力：L1 `:138-141` 要求作废联动取消；L2 `state-machine.md §4:50 + §实现约定:190` 显式标 Deferred（owner doc 向实现妥协的 documented simplification）。**按 §4 Q1，L1↔L2 冲突以 L1 为准——但 §4 三判据复核 R1.20 deferral：(i) R1.20 plan 含独立 plan-audit 通过记录（iteration 2 accept, ses_0502be6efffeIzndtBDicVNwJR，`2026-07-30-0512-3:119`）成立 → documented simplification with approval → 倾向 P2；声明 Q4=(a) 张力：若判定 L1 UC-QA-08 为硬性 P1 要求则须实现（须人工确认 product-scope）** |

**结论：P2（documented simplification with approval）+ 新 P1-RC 登记**。本审计**不重开 P1-MA2-064**（§去重协议：不重审 audit-remediation MA2 行为裁决），而是从 **L1 视角登记新 `P1-RC-041`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类（判据 (i) R1.20 plan-audit 通过成立 → 倾向 P2 documented simplification with approval；声明 Q4=(a) 张力，若 L1 为硬性 P1 要求则须人工确认 product-scope）+ **arm-index 状态澄清**（:361「✅ resolved (R1.20 done)」未披露该 resolution 是 *deferral* 而非 *implementation*——属误导性「done」标签致基线初判误为已实现，须澄清为 resolved-via-deferral）。

### 矩阵汇总（7 UC 符合性结论）

| UC | L1 锚点 | 结论 | 命中判据 | finding |
|----|---------|------|---------|---------|
| UC-QA-01 来料强制质检阻塞 | `:15` | **接受** | §2 接受（主路径已实现 & 强测） | 无 |
| UC-QA-02 质检不合格触发退货 | `:33` | **接受** | §2 接受（NCR+退货编排已实现 & 强测） | 无 |
| UC-QA-03 让步接收 | `:50` | **接受 on ①②③**（关键项否决维度归 UC-QA-06） | §2 接受（让步 CONDITIONAL 主路径） | 无（关键项否决 → 复用 UC-QA-06 P1-RC-040） |
| UC-QA-04 完工检验不合格→返工 | `:67` | **倾向接受 / P2 successor** | §2 P2③（documented simplification，mfg 侧闭环经 A1.9 证实） | **P2-RC-041**（新） |
| UC-QA-06 关键项否决 | `:101` | **P1** | §2 P1①（功能完全缺失——数据模型+评估器双缺）+ §2 P1⑤（断言误导） | **P1-RC-040**（新，须人工确认 product-scope，ORM ask-first） |
| UC-QA-07 质检模板优先级解析 | `:116` | **P2** | §2 P2①（次要验收标准未满足，主路径 OK） | **P2-RC-040**（新） |
| UC-QA-08 业务单据作废联动取消质检 | `:133` | **P2（documented simplification with approval）+ 新 P1-RC** | §4 三判据[i] 成立→倾向 P2；声明 Q4=(a) 张力；**不重开 P1-MA2-064** | **P1-RC-041**（新，L1 视角 + arm-index 状态澄清） |

**整体 Verdict**：⚠️(P1) — 7 UC 中 3 UC（QA-01/02/03）接受、1 UC（QA-04）倾向接受/P2 successor、1 UC（QA-06）P1、1 UC（QA-07）P2、1 UC（QA-08）P2 documented simplification + 新 P1-RC，**零 P0**。新登记 4 项 finding（P1-RC-040/041 + P2-RC-040/041）。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 每条 finding 产出前已 grep `arm-index.md` qa inspection/NCR/concession/critical/template/void/rework 同域同控制点后裁决。**RC 系列对 quality 为首批**（本切片为 quality 域首批 RC 切片，既有 arm-index 无 RC finding 涉及 qa 检验门控/关键项否决/类别级模板/作废联动/完工返工）。

| Finding ID | 报告 | 域 | UC | 描述（精简，详见 §5 矩阵行 + arm-index 行） | 分级判据 | 目标 MR | 复用/新增裁决 |
|-----------|------|---|----|------|---------|--------|------|---------|
| `P1-RC-040` | rc-ma1-a1-31-quality-f1-inspection-gating | quality | UC-QA-06（+ UC-QA-03 关键项否决断言复用） | **UC-QA-06 关键项否决核心验收标准完全缺失（数据模型 + 评估器逻辑双缺）**：L1（`use-cases.md:107-109`）逐字「质检模板行.是否关键项==true 且该行不合格 → 整体质检单=REJECTED（关键项否决，无论其他项）；关键项不合格→不可让步（直接 REJECTED）」。L3 实仓：ORM `ErpQaInspectionTemplateLine`（`app-erp-quality.orm.xml:326`）仅 `isRequired`="是否必检"（propId 8，语义=必检≠关键项），**无 `isCritical`/`criticalItem`/`关键项` 列**（grep `isCritical\|criticalItem\|关键项` 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + orm.xml **零业务命中**，仅 SPC SEVERITY_CRITICAL 无关）。`InspectionResultEvaluator.aggregate:73-92` **无关键项否决分支**——`anyRejected && allowConcession → CONDITIONAL (:90-91)`，**关键项不合格 + allowConcession=true 错误产出 CONDITIONAL，直接违反 L1 `:108-109`**。`evaluateLine:43-49` 是通用 out-of-spec REJECTED 探测非关键项逻辑。L4 `testRejectedCriticalGoesRejected:84` **命名误导**——实为 `allowConcession=false` 通用拒绝断言（无关键项标记可 seed），未覆盖「关键项否决覆盖让步」路径（Q1 根因：测试名误导致基线误判）。**非 P0**：不破坏活跃数据（关键项不合格 + 让步 → 错误 CONDITIONAL 放行而非破坏 GL/库存；CONDITIONAL 仍允许业务流转，会计过账不破坏；主路径[非关键项让步]正确）+ 非会计过账破坏 + 非核心循环断裂；属功能完全缺失类（§2 P1①）。**须人工确认 product-scope 是否要求关键项否决**（`inspection-integration.md §十:333` + §5.3:219-222 + L1 `:107-109` 均显式声明，倾向 P1 强制实现 Q4 无例外）。 | §2 P1①（功能完全缺失——数据模型 + 评估器逻辑双缺）+ §2 P1⑤（断言误导/零关键项覆盖） | MR1（R1.0 展开为 RC-R1.n）/ §4(iii) product-scope 修订（若人工确认裁剪） | **新增**——grep arm-index「关键项」「criticalItem」「isCritical」「关键项否决」零命中；与 P0-MA2-017（inspection 状态守卫，resolved）不同控制点（状态守卫 vs 关键项否决逻辑）；与 P1-MA2-065（dict 死状态）不同控制点。**修复触及 ORM 结构变更[ErpQaInspectionTemplateLine + ErpQaInspectionLine 加 isCritical 列] + aggregate 增关键项否决分支[关键项 REJECTED 行 → 强制整体 REJECTED，跳过 allowConcession] → 须 ask-first + 独立 plan-audit §5 ORM 结构变更类**。 |
| `P1-RC-041` | rc-ma1-a1-31-quality-f1-inspection-gating | quality | UC-QA-08 | **UC-QA-08 业务单据作废联动取消 L1 验收标准经 R1.20 deferral 未满足（从 L1 视角登记新 P1-RC；§4 三判据[i] 成立倾向 P2 documented simplification with approval；声明 Q4=(a) 张力；arm-index 状态澄清）**：L1（`use-cases.md:138-141`）逐字「入库单.作废 → 关联质检单（若 PENDING）→取消(CANCELLED)；不影响已 ACCEPTED/REJECTED」。L3 实仓：`IErpQaInspectionBiz`（85 行）**无 `cancelForBusinessBill`** + grep 跨 `module-quality/purchase/sales/mfg` **零命中**。**关键事实**：R1.20 plan `2026-07-30-0512-3:37,64` **显式裁决 P1-MA2-064 Deferred**（owner doc 正式化，非方案 A 实现）+ `state-machine.md §实现约定:190`「业务单据作废联动取消（Deferred）……本期不落地」——Facade 缺失是 **deliberate deferral，非丢失合入/回退**。**本切片处置（与 §去重协议一致）**：**不重开 P1-MA2-064**（audit-remediation mission 的 finding，本审计 MA1 不重审 MA2 行为裁决），从 **L1 视角**登记**新 `P1-RC-041`**（UC-QA-08 验收标准经 deferral 未满足）+ §4 三判据分类：判据 (i) R1.20 plan 含独立 plan-audit 通过记录成立（`2026-07-30-0512-3:119` iteration 2 accept, ses_0502be6efffeIzndtBDicVNwJR）→ **倾向 P2 documented simplification with approval**（非静默降级）；**声明 Q4=(a) 张力**——若判定 L1 UC-QA-08 为硬性 P1 要求则须实现（须人工确认 product-scope）。**arm-index 状态澄清**：arm-index:361 标 P1-MA2-064「✅ resolved (R1.20 done, roadmap 2026-07-31 确认 done)」未披露该 resolution 是 *deferral* 而非 *implementation*——属**误导性「done」标签**致基线初判误为已实现，须澄清为 **resolved-via-deferral**（非 resolved-via-implementation）。**非 P0**：CANCELLED 业务单据不再流转，质检单悬挂不破坏主路径（强制质检门控经 isInspectionCleared 检查，CANCELLED 业务单据不触发 enforceGate 二次流转）+ 残留质检单经 useLogicalDelete 手工清理。 | §4 三判据[i] 成立 → 倾向 P2（documented simplification with approval）；声明 Q4=(a) 张力（若 L1 硬性 P1 则须实现） | MR1（R1.0 展开为 RC-R1.n，若人工确认 L1 硬性要求）/ §4(iii) product-scope 修订（若人工确认裁剪） | **新增（L1 视角）+ 不重开 P1-MA2-064**——grep arm-index「cancelForBusinessBill」「作废联动」「UC-QA-08」RC 系列零命中；P1-MA2-064（audit-remediation MA2，resolved-via-deferral）与本 finding（requirement-compliance MA1 L1 视角）互补不重复（不同 mission 不同视角）；arm-index 状态澄清为 resolved-via-deferral 非新 finding。**修复属代码逻辑类预授权**（若人工确认 L1 硬性要求：新增 `IErpQaInspectionBiz.cancelForBusinessBill(billType, billCode)` Facade[PENDING→cancelled via useLogicalDelete] + purchase/sales/mfg cancel Processor config-gated wiring；纯 Facade + Processor 接线，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first[不触及 ORM/会计过账核心路径]）。 |
| `P2-RC-040` | rc-ma1-a1-31-quality-f1-inspection-gating | quality | UC-QA-07 | **UC-QA-07 质检模板优先级「物料类别级」查询缺失（次要验收标准未完全满足，主路径 OK）**：L1（`use-cases.md:122`）逐字「优先级: 物料级模板 > 物料类别级 > 全局默认 / 物料有专属→用专属 / 否则 查类别模板→用类别 / 否则 用全局默认」。L2（`inspection-integration.md §5.2:199-205`）一致要求三级。L3 实仓：`InspectionTemplateMatcher.match:30-47` **仅两级**——`findActiveByMaterialAndType:50-64` 按 `materialId × inspectionType × isActive` 过滤（`:53-58`），无匹配回落 `erp-qua.default-inspection-template` 全局默认（`:33-35`），仍无返回 null 人工补录；**无物料类别级（material.categoryId）查询**。L4 `testNoTemplateFallsToGlobalDefault:107` + `testNoTemplateNoLinesManualEntry:123` 仅覆盖两级回退，类别级零覆盖。主路径（专属模板/全局默认）OK，边界（物料无专属但有类别级模板）弱。 | §2 P2①（次要验收标准未完全满足——主路径[专属/全局默认]OK，边界[类别级回退]缺失） | successor watch-only（P2 登记不强制） | **新增**——grep arm-index「模板优先级」「类别级」「InspectionTemplateMatcher」「category-level template」零命中。**修复 = `InspectionTemplateMatcher.match` 增类别级回退查询[专属模板 null → 按 material.categoryId 查 active 模板 → 仍无则全局默认]，纯代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。 |
| `P2-RC-041` | rc-ma1-a1-31-quality-f1-inspection-gating | quality | UC-QA-04 | **UC-QA-04 完工返工跨域触发链 successor（mfg 侧闭环经 A1.9 证实，quality 侧仅生成 NCR，自动建返工工单属 owner doc §3 successor）**：L1（`use-cases.md:74-75`）逐字「完工质检(FINAL) REJECTED → 不合格处理路径: 返工 / 触发 ../manufacturing 新建返工工单(关联原工单)」。L2（`inspection-integration.md §2.3:90` + §7.2:265 + `state-machine.md §场景D`）一致（事件解耦，quality DAG 无环不反向依赖 business）。L3 实仓：quality 侧 FINAL REJECTED → `NcrLifecycleService.autoCreateNcrFromInspection:49`（仅生成 NCR，**不直接跨域建返工工单**）；grep `rework\|ReworkOrder\|createRework\|返工工单` 跨 `module-quality/erp-qa-service/src/main` **零业务命中**。mfg 侧（A1.9 复用）：`ErpMfgWorkOrderReportCompletionProcessor:46-58` 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建返工工单（**无 originalWorkOrderId 字段、无自动建返工工单代码**）。L4 零 dedicated 测试核 REJECTED→返工工单（testWorkOrderFinalTrigger:96 仅核触发创建质检单）。**§4 三判据复核**：(i) A1.9 plan 含独立 plan-audit 通过记录（A1.9 done）→ documented simplification 成立 → 倾向 P2 successor。**非 P0/P1**：mfg 门控闭环（工单保持 IN_PROCESS）+ quality 生成 NCR 构成跨域闭环（事件解耦）；自动建返工工单 + originalWorkOrderId 关联属 owner doc §3 已裁定的 successor 触发条件（A1.9 倾向接受 + SP-2 存疑点交 MA4），非阻断主路径。 | §2 P2③（owner doc 显式 documented simplification 标注，A1.9 plan-audit 通过）+ §2 P2②（可用性/可观测性 successor） | successor watch-only（P2 登记不强制） | **新增（quality 侧投影）+ 交叉引用 A1.9 UC-MFG-09**——grep arm-index「返工工单」「rework order」「originalWorkOrderId」RC 系列零命中；A1.9 UC-MFG-09（mfg 视角，倾向接受 + SP-2）与本 finding（quality 视角）互补不重复（不同切片不同域投影，同一跨域控制点）；按 §去重协议 MA1 同 mission 兄弟切片，不重开 A1.9 结论。**修复属代码逻辑类预授权**（若实现：quality 侧 FINAL REJECTED 后经 config-gated 事件/查询触发 mfg 建返工工单 + ErpMfgWorkOrder 加 originalWorkOrderId 关联字段[**触及 ORM ask-first**]，与 A1.9 SP-2 协同；纯 Facade + Processor + 可能 ORM 字段，按 roadmap 预授权类目[代码逻辑]可自动执行[非 ORM 部分]，ORM 部分须 ask-first）。 |

**双向可追溯**：4 项新 finding 已写入本报告 §5 矩阵 + 本 §6 + arm-index RC 发现追踪分区（修复行预留 MR1 R1.0 → RC-R1.n）。

---

## 7. 静态存疑点清单（供 MA4 运行时展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。

| SP# | 存疑点 | 触发条件 | 交 MA4 |
|-----|--------|---------|--------|
| SP-1 | **UC-QA-06 关键项否决缺失的运行时触发面**：关键项不合格 + allowConception=true 运行时是否实际产出 CONDITIONAL（理论已证实 aggregate:90-91 无否决分支，但需运行时确认业务域实际是否传 allowConcession=true 场景 + 关键项数据是否实际可录入[模板行无 isCritical 字段，是否经 parameterId 或外部约定隐式标记关键项]） | FINAL/INCOMING 质检 + 部分项不合格 + allowConcession=true | A4.2/A4.5 运行时探针 |
| SP-2 | **UC-QA-04 完工返工跨域触发链运行时操作流程**（与 A1.9 SP-2 同一存疑点，quality 侧投影）：quality FINAL REJECTED + NCR 生成后，mfg 侧工单保持 IN_PROCESS，操作员面对 REJECTED 工单时实际工作流（手动关闭原工单→新建返工工单 / 重置质检状态重新报工 / 经 useLogicalDelete）+ 「关联原工单」可追溯性是否经工单备注/手工关联实际可达 | 完工质检 REJECTED + 工单保持 IN_PROCESS | A4.2 运行时探针（与 A1.9 SP-2 合并） |
| SP-3 | **UC-QA-07 类别级模板缺失的运行时影响面**：物料无专属模板但有类别级模板的实际数据分布（若绝大多数物料有专属模板或直接回落全局默认，类别级缺失影响面小；若存在大量「无专属+有类别级」物料则影响面大） | 物料无专属模板 + 该类别有类别级模板 | A4 运行时探针 |
| SP-4 | **UC-QA-08 业务作废联动 deferral 的残留质检单运行时累积**：业务单据作废后 PENDING 质检单残留经 useLogicalDelete 手工清理——运行时残留质检单的实际累积量 + 是否影响质检员待办列表（TODO 噪音）+ 是否经运营定期清理 | 业务单据作废 + 关联 PENDING 质检单未手工清理 | A4 运行时探针 |
| SP-5 | **UC-QA-01 强制质检门控 config-gated 默认值运行时生效**：`erp-qua.mandatory-inspection-bill-types` config 默认空=不强制——运行时各环境实际配置值 + 是否经运维显式启用 purchase/sales/mfg bill types | config 启用后业务单据流转 | A4 运行时探针 |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（4 项新 finding 均 P1/P2，候选 P0 经证据证伪或降级——UC-QA-06 关键项否决缺失不破坏活跃数据[CONDITIONAL 仍允许流转，非数据破坏]+非会计过账破坏；UC-QA-08 deferral 不破坏主路径[CANCELLED 业务单据不再流转]）。故**不触发 MR0 即时通道**。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 实测如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)）。**本报告不以 checker 脚本退出码作为门控通过依据**。**本审计为只读审计，无生产代码变更，checker 无回归风险**（实测计数反映审计前的既有仓库状态，非本审计引入）。

  | 规则 | 描述 | actual（实测） | baseline（`compliance-baseline.md`） | 状态 |
  |------|------|---------------|-------------------------------------|------|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | = baseline |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | = baseline |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | = baseline |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | = baseline |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | = baseline |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 240 | ≤ baseline ✓ |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1380 | = baseline（+2，本审计无生产代码变更，反映既有仓库状态，非本审计引入） |
  | R2d | Processor daoFor(ErpMd*) | 34 | 32 | = baseline（本审计无生产代码变更） |

  > **注**：R2c actual(1382) vs baseline(1380) 的 +2 delta 反映本审计之前既有仓库的合规状态（baseline 表 + 多个下降注记后的累计），**本审计为只读审计无生产代码变更，不引入任何 daoFor 站点**，故 checker 计数与本审计行为无关，无回归风险归因。R2b/R2d 同理（actual ≤ 或 ≈ baseline，无新增归因本审计）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（4 项均**新增**——RC 系列对 quality 域为首批，既有 arm-index 无 RC finding 涉及 qa 检验门控/关键项否决/类别级模板/作废联动/完工返工；P1-RC-041 与 P1-MA2-064 互补不重复[不同 mission 不同视角]，不重开 P1-MA2-064 per §去重协议），无未经比对直接新建的 finding。

---

## 9. 与 MA2 报告差异增量声明（详见报告开头 §9 置顶段）

> 完整声明见报告开头 §9。摘要：复用 A2.12（P0-MA2-017 inspection 状态守卫 resolved + 强制质检门控 + NCR 过账引擎齐全 + reverseNcr 红冲对称）+ A4.5（NcrReturnOrchestrator→IErpPurReturnBiz Facade）+ A1.9（UC-MFG-09 完工返工跨域链 mfg 侧闭环倾向接受）已证实行为，**只补需求视角差异**（UC-QA-04 返工跨域链 quality 侧投影 / UC-QA-06 关键项否决缺失 / UC-QA-07 类别级模板缺失 / UC-QA-08 L1 验收标准经 deferral 未满足→新 P1-RC-041 + arm-index 状态澄清）。**不重审 MA2 行为**（P1-MA2-064 经 R1.20 显式 Deferred 属 audit-remediation mission 裁决，本审计不重开；P1-MA2-065 dict 死状态同）。

---

## 报告完整性自检（§6 段落完整性，落盘前强制）

- [x] §1 需求契约原文（逐字引用，7 UC × 验收标准完整枚举）
- [x] §2 实现证据（代码路径含行号 + 跨域调用链）
- [x] §3 测试证据（注明断言强度 + 缺口标注）
- [x] §4 运行时行为证据（复用 A2.12/A4.5/A1.9 + 本切片新发现标注）
- [x] §5 五级追踪矩阵 + 每 UC 符合性结论（7 UC 逐矩阵行 + 汇总表 + 命中判据编号）
- [x] §6 与 arm-index 衔接（4 项新 finding 复用/新增裁决 + 双向可追溯）
- [x] §7 静态存疑点清单（5 存疑点交 MA4 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明（置顶 + 摘要）

**9 段齐全。未修改真相源**（§9 冻结条款——分歧记入报告 §5/§6，未直改 use-cases.md/inspection-integration.md/state-machine.md/product-scope.md）。**未修改代码/ORM/api.xml**（只读审计）。
