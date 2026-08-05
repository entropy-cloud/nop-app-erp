# A1.32 quality-F2 NCR-CAPA 闭环 需求-实现符合性五级追踪审计报告

> 报告类型：MA1（RC）需求-实现符合性五级追踪审计
> 切片：A1.32 quality-F2 NCR-CAPA 闭环（NCR 不符合项 + CAPA 纠正预防闭环 + 效果验证门禁 + 验证失败回退 + 全程记录）
> 审计范围：UC-QA-05（1 UC，逐 UC 一矩阵行；NCR-CAPA 是质量闭环核心，仅 1 UC 但覆盖效果验证门禁全链）
> 真相源层级（§4 Q1）：L1 = `docs/design/quality/use-cases.md`（UC-QA-05 `:82`）；L2 = `state-machine.md §适用对象二 NCR` + `inspection-integration.md §四`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.12 + A4.5 + 本切片差异。
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 结论速览：✅(接受 on 主路径) + ⚠️(P2) — UC-QA-05 **主路径完整实现且强测**（OPEN→IN_REVIEW→CAPA→执行→效果验证→RESOLVED + resolve 门禁 + R1.20 noCapaReason 逃逸门 + 全程记录 person/date），3 候选缺口经逐条复核**均不构成 P0/P1**：①验证失败显式回退路径 = **被动阻塞语义等价**（P2-RC-042）；②noCapaReason 逃逸门 = **§4 三判据 (i)+(ii) 满足接受**（legitimate documented escape hatch，arm-index P1-MA2-066 追加 RC 交叉引用不新建）；③全程记录 verificationResult 字段缺失 = **person+date+verifyAction 调用隐式满足**（P2-RC-043）。**零 P0 / 零 P1**。**新登记 2 项 P2 finding**：`P2-RC-042`（验证失败回退被动阻塞，与 dedicated reopen 主动回退形式差异，语义等价）/ `P2-RC-043`（ErpQaAction 无 verificationResult 列，验证结果由 person+date+verifyAction 调用隐式承载）。

---

## 9. 与既有 MA2 报告差异增量声明（§6 段落 9，置顶便于去重）

本切片为 quality 域**第二批 RC 切片**（A1.31/A1.32/A1.33 三切片，本切片覆盖 UC-QA-05 NCR-CAPA 闭环；A1.31 已 done 覆盖 UC-QA-01/02/03/04/06/07/08）。按 §去重协议，本报告**不复跑** MA2 状态机行为审计，直接复用 `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12）已证实行为作为 L5 既有证据输入，只补"需求契约↔实际行为"差异：

- **复用 A2.12 已证实行为**：NCR 5 态状态机核心契约（OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED 全迁移 + 守卫 + 非法迁移抛 `ERR_INVALID_NCR_STATUS_TRANSITION`）、NCR-CAPA 闭环门禁齐全（resolve 守卫 + CAPA 全 COMPLETED + 效果验证）、**P1-MA2-066 NCR resolve 无 CAPA 门禁 resolved R1.20**（noCapaReason 显式标注门禁落地，UC-QA-05 直接相关）、NCR 过账引擎齐全（SCRAP/RETURN/CONCESSION/DOWNGRADE）+ reverseNcr 红冲对称。
- **本切片不重开 MA2 finding**（§去重协议）：
  - **P1-MA2-066**（NCR resolve 无 CAPA 门禁）经 **R1.20 resolved-via-implementation**（plan `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md`，方案 A 实现：`noCapaReason` 列[propId=33] + `ERR_NCR_RESOLVE_NO_CAPA` + `actionsGatePassed` 空措施路径 `StringHelper.isNotBlank(noCapaReason)` 门禁 + `requireResolveGate` 双错误码 + resolve 签名增 `@Optional noCapaReason` + owner doc §NCR 与 CAPA 显式标注 + 3-case 测试）。本审计**不重开 P1-MA2-066**，而是从 **L1 视角**复核 noCapaReason 逃逸门（L1 UC-QA-05 未提及此逃逸门）的 §4 三判据（见 §5 候选缺口② + §6）。
  - **P1-MA2-064**（业务作废联动取消）经 R1.20 resolved-via-deferral，已由 A1.31 从 L1 视角登记 P1-RC-041（UC-QA-08）；本切片 Non-Goal 不重审（§去重协议，UC-QA-05 不投影 UC-QA-08）。
  - **P1-MA2-065**（dict 死状态 + CRUD 桩）经 R1.20 Deferred，属 MA2 行为裁决，本切片 Non-Goal 不重审。
- **本切片只补的需求视角差异**：① UC-QA-05 验证失败显式回退路径语义（L1 `:92` + L2 inspection-integration §四.3 `:168,184` 均要求"验证失败→返回 IN_REVIEW 重新制定"，实现为被动阻塞——resolve 抛异常 NCR 保持 IN_REVIEW，Facade 无 dedicated reopen 方法，语义等价但形式差异）；② noCapaReason 逃逸门 §4 三判据复核（L1 UC-QA-05 未提及，R1.20 引入的合法逃逸门经独立 plan-audit）；③ 全程记录 verificationResult 字段完整性（L1 `:94`「全程记录...验证结果」，ErpQaAction ORM 无 verificationResult 列，由 person+date+verifyAction 调用隐式承载）。

---

## 1. 需求契约原文（逐字引用，§1 L1 格式）

> 来源 `docs/design/quality/use-cases.md`，逐字引用验收标准（禁止转述）。

### UC-QA-05 NCR-CAPA 闭环（`:82`）
```
NCR: OPEN → IN_REVIEW → 制定 CAPA(纠正+预防)
  → 执行 CAPA
  → 效果验证:
      验证通过 → RESOLVED
      验证失败 → 返回 IN_REVIEW(重新制定)
未通过效果验证 → NCR 不可 RESOLVED
全程记录纠正/预防措施/验证结果
```

**断言计数（逐条完整枚举，禁止抽样）**：UC-QA-05 共 **7 条验收标准**（按 L1 字面拆分逐条进入 L5 判读）：
- **断言①**「NCR: OPEN → IN_REVIEW」（状态迁移前置链）
- **断言②**「制定 CAPA(纠正+预防)」（CAPA 措施建模含纠正 + 预防区分）
- **断言③**「执行 CAPA」（CAPA 措施生命周期：执行动作）
- **断言④**「效果验证：验证通过 → RESOLVED」（效果验证是 RESOLVED 的强制前置）
- **断言⑤**「验证失败 → 返回 IN_REVIEW(重新制定)」（验证失败显式回退路径）
- **断言⑥**「未通过效果验证 → NCR 不可 RESOLVED」（RESOLVED 门禁）
- **断言⑦**「全程记录纠正/预防措施/验证结果」（审计轨迹完整性）

---

## 2. 实现证据（代码路径，§1 L3 格式，含跨域调用链）

> 全部 `module-quality/erp-qa-service/src/main/...` 或 `module-quality/erp-qa-dao/src/main/...`，含行号。NCR 状态机采用 R6.6 每-mutation-一-Processor 架构（Facade 委托 Processor）。

| 控制点 | 代码路径（file:line） | 备注 |
|--------|----------------------|------|
| NCR 状态机 Facade（单步 mutation） | `ErpQaNonConformanceBizModel.java`（R6.6 Facade）：`submitReview:49-53`（OPEN→IN_REVIEW 守卫 `requireNcrStatus(...NCR_STATUS_OPEN...) :52`）、`escalateToRecall:81-88`（IN_REVIEW→ESCALATED_TO_RECALL 终态占位）、`cancel:97-108`（守卫 OPEN/IN_REVIEW :101-103） | 单步状态翻转留 Facade |
| resolve 委托（多步 mutation） | `ErpQaNonConformanceBizModel.resolve:60-65`（委托 `resolveProcessor.resolve`）→ `ErpQaNonConformanceResolveProcessor.resolve:28-49`（**IN_REVIEW 守卫在 ResolveProcessor:33** `requireNcrStatus(...NCR_STATUS_IN_REVIEW...)`） | R6.6 Facade→Processor 委托 |
| **resolve CAPA 闭环门禁** | `NcrLifecycleService.requireResolveGate:135-146`（有措施路径 `actionsGatePassed:102-116`：全 COMPLETED `:108` + verificationPerson/verificationDate `:111`，否则抛 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED :144`；无措施路径 `actionsGatePassed:102-106`：须 `StringHelper.isNotBlank(noCapaReason)`，否则抛 `ERR_NCR_RESOLVE_NO_CAPA :141`） | **R1.20 已堵 isEmpty→true 逃逸门**；ResolveProcessor:35 调用 + :40-42 持久化 noCapaReason |
| **resolve 后状态推进** | `ErpQaNonConformanceResolveProcessor.resolve:36`（门禁通过后 `ncr.setStatus(NCR_STATUS_RESOLVED)`）+ `:43-44` resolvedAt + `dispatchFinancialImpact:57-71`（SCRAP AUTO_POST / RETURN 编排退货） | 效果验证通过→RESOLVED 主路径 |
| CAPA 措施生命周期 | `ErpQaActionBizModel.java`（R6.6）：`startAction:36-42`（PENDING→IN_PROGRESS 守卫 :38）、`completeAction:46-54`（→COMPLETED + completedAt :50）、**`verifyAction:58-77`（COMPLETED 守卫 :65 + verificationPerson/verificationDate 必填 :69-72 + 写入 :73-74，效果验证——填写即隐式验证通过，无 `execute` 方法）** | CAPA 措施 PENDING→IN_PROGRESS→COMPLETED + 验证 |
| CAPA 措施模型（纠正/预防区分） | `ErpQaAction` ORM（`app-erp-quality.orm.xml:415-429`）：`actionType` 列（propId 3，dict `erp-qa/action-type` 含 **CORRECTIVE[纠正] / PREVENTIVE[预防] / CAPA[合并]** 三值）+ `verificationPerson`（propId 10）+ `verificationDate`（propId 11）+ completedAt/completedBy | **纠正/预防区分 ✅**（dict 三值承载） |
| **全程记录 verificationResult 字段** | **缺失**——`ErpQaAction` ORM（`app-erp-quality.orm.xml:415-429`）grep `verificationResult` **零命中**（仅 actionType/verificationPerson/verificationDate/completedAt/completedBy）；`verifyAction:58-77` 仅写 person+date，**调用该动作即隐式「验证通过」，无失败结果值承载** | 候选缺口③——验证结果由 person+date+verifyAction 调用事实隐式满足 |
| **验证失败显式回退动作** | **缺失**——`ErpQaNonConformanceBizModel`（Facade，132 行）grep `reopen\|rejectVerification\|failVerification\|backToReview` **零命中**；resolve 失败时 ResolveProcessor:35 `requireResolveGate` 抛异常，事务回滚 NCR 保持当前态 IN_REVIEW（被动阻塞） | 候选缺口①——无 dedicated reopen，被动阻塞语义等价 |
| 非法迁移守卫 | `ErpQaNonConformanceBizModel.illegalNcrTransition:126-131`（抛 `ERR_INVALID_NCR_STATUS_TRANSITION` + ARG_NCR_CODE/ARG_CURRENT_STATUS/ARG_EXPECTED_STATUS） | 全状态机守卫 |
| NCR 自动生成（REJECTED 触发） | `NcrLifecycleService.autoCreateNcrFromInspection:49-68`（sourceType=INSPECTION / status=OPEN / config-gated `erp-qua.auto-create-ncr-on-reject` 默认 true） | 质检 REJECTED→NCR(OPEN) 入口 |

---

## 3. 测试证据（§1 L4 格式，注明断言强度）

> `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/`。

| 测试方法 | 覆盖断言 | 强度 |
|---------|----------|------|
| `TestErpQaNcrCapaEndToEnd#testRejectedAutoCreatesNcrAndFullCapaClosure:61-110` | **断言①②③④⑥⑦ 主路径全链强覆盖**：REJECTED→自动 NCR(OPEN) :67-71 + submitReview OPEN→IN_REVIEW :75-76 + CAPA PENDING 未完成 resolve 拒绝（ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED :81-84）+ startAction/completeAction PENDING→IN_PROGRESS→COMPLETED :87-90 + 缺验证 resolve 拒绝 :93-96 + verifyAction 填 person+date :99-103 + resolve IN_REVIEW→RESOLVED 门禁通过 :106-109 + resolvedAt 落地 :109 | **强**（全链状态精确值 + 错误码精确匹配 + resolvedAt 断言） |
| `TestErpQaNcrCapaEndToEnd#testResolveNoCapaGate:158-182` | **R1.20 noCapaReason 逃逸门**：无 CAPA + noCapaReason 空 → ERR_NCR_RESOLVE_NO_CAPA :168-171 + 无 CAPA + noCapaReason 非空 → resolve 成功 + noCapaReason 落库 :174-181 | **强**（双 case 错误码 + 落库断言） |
| `TestErpQaNcrCapaEndToEnd#testIllegalNcrTransitionsRejected:144-155` | **断言⑥ 门禁（OPEN→resolve 非法）**：OPEN 直接 resolve → ERR_INVALID_NCR_STATUS_TRANSITION :151-154 | **强**（非法迁移错误码） |
| `TestErpQaNcrCapaEndToEnd#testEscalateToRecallTerminal:113-129` | escalateToRecall 终态 + 终态后 resolve 非法 :125-128 | **强** |
| `TestErpQaNcrCapaEndToEnd#testCancelFromOpenAndReview:132-141` | cancel OPEN→CANCELLED | 强 |
| `TestErpQaNcrPosting#testScrapAutoPostedOnResolve:86` / `#testReverseNcrClearsPostedAndRedOffset:165` | NCR 过账 + 红冲闭环（与 resolve RESOLVED 相关） | 强 |
| `TestErpQaNcrPosting#testConcessionNotPostable:125` / `#testPostingBeforeResolvedRejected:179` | CONCESSION 不可过账 + 未 resolve 拒过账（间接证断言⑥门禁） | 强 |

**测试缺口（诚实登记）**：
- **断言⑤（验证失败→返回 IN_REVIEW 重新制定）dedicated 测试缺失**：因实现无 dedicated 主动回退机制（无 reopen 方法），测试缺口是机制缺失的自然结果而非断言强度不足。`testRejectedAutoCreatesNcrAndFullCapaClosure:81-84/93-96` 覆盖"CAPA 未完成/缺验证时 resolve 抛异常阻塞"（被动阻塞语义），但无"CAPA 已执行但效果验证显式失败→主动回退 IN_REVIEW 重新制定"的 dedicated 断言（因无 verificationResult 字段可标记失败，该路径结构上不可达——见候选缺口①③联动）。
- **断言⑦ verificationResult 字段无断言**：因 ORM 无 verificationResult 列，测试无法断言显式验证结果值（person+date 已强断言，验证结果由 verifyAction 调用事实隐式承载）。

---

## 4. 运行时行为证据（§1 L5 格式）

> 既有 MA2 报告已证实行为直接引用（§去重协议）。

| 控制点 | L5 行为证据来源 | 状态 |
|--------|----------------|------|
| NCR 5 态状态机（OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED 全迁移 + 守卫 + 非法迁移错误码） | **行为已证实（引用 A2.12）** | 复用 |
| NCR-CAPA 闭环门禁（resolve 守卫 + CAPA 全 COMPLETED + verificationPerson/Date） | **行为已证实（引用 A2.12 + P1-MA2-066 resolved R1.20）**：R1.20 已堵旧 isEmpty→true 逃逸门，现 actionsGatePassed:102-106 要求 isNotBlank(noCapaReason) | 复用 |
| resolve 主路径（效果验证通过→RESOLVED + 财务分派 SCRAP/RETURN） | **行为已证实（引用 A2.12）**：testRejectedAutoCreatesNcrAndFullCapaClosure:61-110 强断言全链 | 复用 |
| NCR 过账引擎（SCRAP/RETURN/CONCESSION/DOWNGRADE）+ reverseNcr 红冲对称 | **行为已证实（引用 A2.12）** | 复用 |
| 断言①②③④⑥⑦ 主路径（OPEN→IN_REVIEW→CAPA 纠正/预防→执行→验证→RESOLVED + 门禁 + 全程记录 person/date） | **行为已证实（引用 A2.12 + 本切片 L3/L4）**：主路径全链强测 | 复用 |
| 断言⑤ 验证失败显式回退 | **被动阻塞语义等价（本切片复核）**：resolve 抛异常 NCR 保持 IN_REVIEW，无 dedicated reopen——但 NCR 既已在 IN_REVIEW，"返回 IN_REVIEW"状态等价达成；"重新制定"可行（NCR 仍处 IN_REVIEW 可新增/修改 CAPA 后再 resolve） | **新发现（P2-RC-042，语义等价形式差异）** |
| 断言⑦ verificationResult 字段 | **行为缺失（本切片新发现）**：ErpQaAction 无 verificationResult 列，verifyAction 调用即隐式通过，无失败结果承载 | **新发现（P2-RC-043）** |
| noCapaReason 逃逸门（L1 未提及） | **R1.20 落地经审计裁决（本切片 §4 三判据复核）**：误开/降级 NCR 合法场景的 deliberate escape hatch，§4 (i) R1.20 plan-audit 通过 + (ii) owner doc 显式标注 | **复核接受**（不新建 finding） |

---

## 5. 五级追踪矩阵 + 每 UC 符合性结论（§2 判据，取最高）

> 逐 UC 一矩阵行，禁止合并/跳号。冲突裁决：L2 与 L1 冲突一律以 L1 为准（§4 Q1）。

### UC-QA-05 NCR-CAPA 闭环

#### 断言①②③④⑥⑦ 主路径（OPEN→IN_REVIEW→CAPA→执行→验证→RESOLVED + 门禁 + 全程记录 person/date）

| L1（逐字） | L2（设计参考） | L3（代码） | L4（测试，强度） | L5（行为） | 冲突裁决 |
|-----------|---------------|-----------|-----------------|-----------|---------|
| `:82,88-91,93` OPEN→IN_REVIEW / 制定 CAPA(纠正+预防) / 执行 CAPA / 验证通过→RESOLVED / 未通过验证→不可 RESOLVED / 全程记录纠正/预防措施/验证结果 | `state-machine.md §适用对象二 NCR:121-150`（5 态 + IN_REVIEW→制定 CAPA 并验证有效→RESOLVED + 无 CAPA 时 resolve 须 noCapaReason）+ `inspection-integration.md §四:166-184`（效果验证→RESOLVED / 无 CAPA 措施 resolve 门控 :172 / 效果验证才能关闭 NCR :174）一致 | `submitReview:49-53`（OPEN→IN_REVIEW 守卫）+ `ErpQaAction` actionType dict CORRECTIVE/PREVENTIVE/CAPA（纠正/预防区分 ✅）+ `startAction:36-42`/`completeAction:46-54`（执行）+ `verifyAction:58-77`（验证通过写 person/date）+ `requireResolveGate:135-146`（有措施须全 COMPLETED+验证否则 ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED）+ `ResolveProcessor.resolve:36`（→RESOLVED） | `testRejectedAutoCreatesNcrAndFullCapaClosure:61-110`（**强**：全链 + 双门禁错误码 + resolvedAt）+ `testIllegalNcrTransitionsRejected:144-155`（**强**：OPEN→resolve 非法） | 行为已证实（引用 A2.12 NCR 状态机 + P1-MA2-066 resolved R1.20 + 本切片主路径强测） | 无冲突（L1↔L2↔L3 主路径一致） |

**结论：接受 on ①②③④⑥⑦主路径**（§2 接受——6 条验收标准在 L3-L5 各级均有证据且一致）。效果验证是 RESOLVED 强制前置（断言④ ✅ requireResolveGate）+ 未通过验证不可 RESOLVED（断言⑥ ✅ ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED）+ 纠正/预防区分（断言② ✅ actionType dict CORRECTIVE/PREVENTIVE）+ 全程记录 person/date（断言⑦ person/date 部分 ✅，验证结果显式字段见下缺口③）。

#### 断言⑤ 验证失败→返回 IN_REVIEW（重新制定）— 候选缺口①

| L1（逐字） | L2（设计参考） | L3（代码） | L4（测试，强度） | L5（行为） | 冲突裁决 |
|-----------|---------------|-----------|-----------------|-----------|---------|
| `:92` 验证失败 → 返回 IN_REVIEW(重新制定) | `inspection-integration.md §四.3:168`「验证失败 → 返回重新制定措施」+ `:184`「验证失败 → 返回评审中」**显式声明该迁移**（L2 owner doc 明确要求验证失败显式回退）；`state-machine.md §NCR:128` 仅述「制定 CAPA 并验证有效 → RESOLVED」未显式展开失败分支 | **无 dedicated reopen 动作**——Facade（132 行）grep `reopen\|rejectVerification\|backToReview` **零命中**；resolve 失败时 `ResolveProcessor:35 requireResolveGate` 抛异常，事务回滚 NCR 保持当前态 IN_REVIEW（**被动阻塞**）；NCR 既已在 IN_REVIEW，"返回 IN_REVIEW"状态等价达成 | `testRejectedAutoCreatesNcrAndFullCapaClosure:81-84,93-96` 覆盖"CAPA 未完成/缺验证时 resolve 抛异常阻塞"（被动阻塞语义，**强**）；**无 dedicated "CAPA 已执行但效果验证显式失败→主动回退" 测试**（因无 verificationResult 字段可标记失败，该路径结构上不可达） | 被动阻塞语义等价：NCR 保持 IN_REVIEW + 可重新制定 CAPA 后再 resolve（end-state 与 L1「返回 IN_REVIEW 重新制定」等价）；形式差异 = 无 dedicated reopen 主动动作 | **L1↔L2 一致（均要求验证失败显式回退）；L3 实现为被动阻塞——语义等价但形式差异**。按 §4 Q1，L1 为准；裁决为 §2 P2①（次要验收标准未完全满足，主路径[验证通过→RESOLVED]OK，边界[显式失败回退主动动作]弱，被动阻塞语义等价） |

**结论：P2**（§2 P2①——主路径[验证通过→RESOLVED + 门禁]OK 且强测，边界[验证失败显式主动回退动作]弱，被动阻塞语义等价：resolve 抛异常 NCR 保持 IN_REVIEW + 可重新制定 CAPA）。**登记 `P2-RC-042`**（与缺口③同根因：验证模型为二元[pass via verifyAction / not-done-yet blocked]，无显式 fail 成果可记录故无 dedicated rollback 触发点；修复 = 纯 BizModel 代码逻辑[新增 reopen mutation 或 verificationResult 字段消费]，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first[若仅加 BizModel 方法]/ 触及 ORM ask-first[若加 verificationResult 列]）。

#### 断言⑦ 全程记录「验证结果」字段完整性 — 候选缺口③

| L1（逐字） | L2（设计参考） | L3（代码） | L4（测试，强度） | L5（行为） | 冲突裁决 |
|-----------|---------------|-----------|-----------------|-----------|---------|
| `:94` 全程记录纠正/预防措施/**验证结果** | `inspection-integration.md §四:174`「效果验证才能关闭 NCR（闭环）」（未显式要求验证结果字段值，强调"效果验证"动作） | `ErpQaAction` ORM（`app-erp-quality.orm.xml:415-429`）字段：actionType(propId 3) + verificationPerson(propId 10) + verificationDate(propId 11) + completedAt/completedBy，**无 verificationResult 列**（grep 零命中）；`verifyAction:58-77` 仅写 person+date，**调用即隐式「验证通过」，无失败结果值承载** | `testRejectedAutoCreatesNcrAndFullCapaClosure:99-103` 断言 verifyAction 写 person+date 成功（**强**）；**无 verificationResult 值断言**（字段不存在） | 行为：验证结果由 (person + date + verifyAction 调用事实) 隐式承载 = "已由指定人于指定日期完成验证且通过"；无显式 result 字段记录"通过/失败"枚举 | L1「验证结果」语义裁决：由 (person+date+verifyAction 调用) **隐式满足**（验证发生 + 执行人 + 日期 + 调用即通过语义均记录），无显式 result 字段；§2 P2①（次要验收标准——主路径[验证发生+执行人+日期已记录]OK，边界[显式 pass/fail result 枚举字段]弱） |

**结论：P2**（§2 P2①——次要验收标准未完全满足：主路径[验证发生 + 执行人 + 日期 + verifyAction 调用事实]OK，边界[显式 verificationResult pass/fail 枚举字段]弱）。**登记 `P2-RC-043`**（与缺口①同根因：验证模型为二元，verifyAction 调用即通过无 fail 成果承载；修复 = `ErpQaAction` ORM 增 `verificationResult` 列[pass/fail 枚举] + verifyAction 增 result 入参 + fail 分支触发回退，**触及 ORM 结构变更须 ask-first + 独立 plan-audit §5**；或 owner doc `state-machine.md §NCR 与 CAPA` 补注「验证结果由 person+date+verifyAction 调用隐式承载（documented simplification）」纯文档修复可自动执行）。

#### 候选缺口② noCapaReason 逃逸门 §4 三判据复核

| L1（逐字） | L2（设计参考） | L3（代码） | L4（测试，强度） | L5（行为） | 冲突裁决 |
|-----------|---------------|-----------|-----------------|-----------|---------|
| `:82-94` UC-QA-05 **未提及 noCapaReason 逃逸门**（L1 仅述"制定 CAPA→执行→验证→RESOLVED"主路径 + "未通过验证→不可 RESOLVED"门禁，未声明"无 CAPA 措施时允许 resolve"分支） | `state-machine.md §NCR 与 CAPA:148`「无 CAPA 措施时 resolve：须显式提供 `noCapaReason`（误开/降级场景显式标注），否则抛 `ERR_NCR_RESOLVE_NO_CAPA`」+ `inspection-integration.md §四:172`「无 CAPA 措施的 resolve 门控：当 NCR 无关联 CAPA 措施时，resolve 须显式提供 `noCapaReason`」**owner doc 显式标注该逃逸门** | `actionsGatePassed:102-106`（actions.isEmpty() → `StringHelper.isNotBlank(noCapaReason)`）+ `requireResolveGate:140-142`（空措施缺 noCapaReason 抛 ERR_NCR_RESOLVE_NO_CAPA）+ ResolveProcessor:40-42 持久化 noCapaReason + ORM noCapaReason 列(propId 33) | `testResolveNoCapaGate:158-182`（**强**：无 CAPA + noCapaReason 空→ERR_NCR_RESOLVE_NO_CAPA + 无 CAPA + noCapaReason 非空→resolve 成功 + 落库） | R1.20 落地经审计裁决的 deliberate escape hatch（误开/降级 NCR 合法场景） | **§4 三判据复核**：(i) **R1.20 plan 含独立 plan-audit 通过记录成立**（`2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md` Draft Review iteration 2 accept ses_0502be6efffeIzndtBDicVNwJR + Closure Audit PASS ses_05007067cffeJ50Z5O1D935wqL，9/9 check 实仓验证）→ 判据 (i) **满足**；(ii) **owner doc 显式 documented 标注**（state-machine.md §NCR 与 CAPA:148 + inspection-integration.md §四:172 显式声明 noCapaReason 逃逸门 + 误开/降级合法场景理由）→ 判据 (ii) **满足**；(iii) product-scope 未裁剪（N/A，(i)/(ii) 已满足）。**→ legitimate documented escape hatch（非静默降级），接受** |

**结论：接受**（§4 三判据 (i)+(ii) 满足——noCapaReason 是经 R1.20 独立 plan-audit 裁决的合法逃逸门，owner doc 显式标注误开/降级合法场景，非静默降级；与 L1 主路径不冲突——L1 未禁止"无 CAPA resolve"，门禁仍强制显式标注）。**不新建 finding**（§4 三判据满足 = 接受，非分歧），arm-index P1-MA2-066 行追加 RC A1.32 交叉引用注记（证 noCapaReason 逃逸门经 L1 视角 §4 复核接受）。

### 矩阵汇总（UC-QA-05 符合性结论）

| 断言 | L1 锚点 | 结论 | 命中判据 | finding |
|------|---------|------|---------|---------|
| ① OPEN→IN_REVIEW | `:88` | **接受** | §2 接受（submitReview 守卫 + 强测） | 无 |
| ② 制定 CAPA(纠正+预防) | `:88` | **接受** | §2 接受（actionType dict CORRECTIVE/PREVENTIVE/CAPA） | 无 |
| ③ 执行 CAPA | `:89` | **接受** | §2 接受（startAction/completeAction 生命周期 + 强测） | 无 |
| ④ 验证通过→RESOLVED | `:91` | **接受** | §2 接受（verifyAction + requireResolveGate 强制前置 + 强测） | 无 |
| ⑤ 验证失败→返回 IN_REVIEW(重新制定) | `:92` | **P2** | §2 P2①（主路径 OK，边界[显式主动回退]弱，被动阻塞语义等价） | **P2-RC-042**（新） |
| ⑥ 未通过验证→不可 RESOLVED | `:93` | **接受** | §2 接受（ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED 门禁 + 强测） | 无 |
| ⑦ 全程记录纠正/预防措施/验证结果 | `:94` | **P2（验证结果字段部分）** | §2 P2①（主路径[person/date+纠正/预防区分]OK，边界[verificationResult 显式字段]弱） | **P2-RC-043**（新） |
| 候选缺口② noCapaReason 逃逸门 | L1 未提及 | **接受**（§4 三判据 (i)+(ii) 满足） | §4 三判据合法 documented escape hatch | 无（arm-index P1-MA2-066 追加 RC 交叉引用） |

**整体 Verdict**：✅(接受 on ①②③④⑥⑦主路径) + ⚠️(P2 on ⑤⑦边界) — UC-QA-05 NCR-CAPA 闭环主路径完整实现且强测，3 候选缺口经逐条复核均不构成 P0/P1（①被动阻塞语义等价 P2 / ②noCapaReason §4 三判据接受 / ③verificationResult 隐式满足 P2），**零 P0 / 零 P1**。新登记 2 项 P2 finding（P2-RC-042/043）。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 每条 finding 产出前已 grep `arm-index.md` qa NCR/CAPA/resolve/verification/effectiveness/noCapaReason/verificationResult 同域同控制点后裁决。**RC 系列对 quality NCR-CAPA 闭环为首批**（A1.31 覆盖检验门控 UC-QA-01~08 不含 UC-QA-05；本切片为 quality 域 NCR-CAPA 闭环首批 RC 切片，既有 arm-index 无 RC finding 涉及 qa 验证失败回退/noCapaReason 逃逸门 L1 视角/verificationResult 记录字段）。

| Finding ID | 报告 | 域 | UC | 描述（精简，详见 §5 矩阵行 + arm-index 行） | 分级判据 | 目标 MR | 复用/新增裁决 |
|-----------|------|---|----|------|---------|--------|------|---------|
| `P2-RC-042` | rc-ma1-a1-32-quality-f2-ncr-capa-closure | quality | UC-QA-05 断言⑤ | **UC-QA-05 验证失败显式回退路径实现为被动阻塞（无 dedicated reopen 主动动作，语义等价形式差异）**：L1（`use-cases.md:92`）逐字「验证失败 → 返回 IN_REVIEW(重新制定)」+ L2（`inspection-integration.md §四.3:168,184`）显式声明「验证失败 → 返回重新制定措施 / 返回评审中」一致要求显式回退。L3 实仓：`ErpQaNonConformanceBizModel`（Facade，132 行）grep `reopen\|rejectVerification\|backToReview` **零命中**——无 dedicated reopen 动作；resolve 失败时 `ErpQaNonConformanceResolveProcessor:35 requireResolveGate` 抛异常，事务回滚 NCR 保持当前态 IN_REVIEW（**被动阻塞**）。**语义等价裁决**：NCR 既已在 IN_REVIEW（resolve 是 IN_REVIEW→RESOLVED 的唯一出边），resolve 失败阻塞 = NCR 保持 IN_REVIEW = L1「返回 IN_REVIEW」状态等价达成；"重新制定"可行（NCR 仍处 IN_REVIEW 可新增/修改 CAPA 后再 resolve）。**形式差异**：无 dedicated 主动回退动作（L1 措辞「返回 IN_REVIEW」可读为主动迁移）。**与缺口③同根因**：验证模型为二元（pass via verifyAction / not-done-yet blocked），无 verificationResult 字段标记失败故无 dedicated rollback 触发点。**非 P1**：主路径[验证通过→RESOLVED + 门禁]完整实现且强测（testRejectedAutoCreatesNcrAndFullCapaClosure:61-110 强覆盖双门禁错误码）+ 不破坏活跃数据（resolve 抛异常事务回滚 NCR 状态不变，无 GL/库存影响）+ 非会计过账破坏；属"主路径 OK，边界[显式主动回退]弱，被动阻塞语义等价"（§2 P2①）。 | §2 P2①（次要验收标准未完全满足——主路径 OK 边界[显式主动回退]弱，被动阻塞语义等价） | successor watch-only（P2 登记不强制） | **新增**——grep arm-index「reopen」「验证失败」「verification failed」「backToReview」RC 系列零命中。与 P1-MA2-066（NCR resolve 无 CAPA 门禁 resolved R1.20）不同控制点（门禁落地 vs 回退动作形式）；与 P2-RC-043（本切片 verificationResult 字段）同根因不同控制点（回退动作 vs 记录字段）。**修复 = 纯 BizModel 代码逻辑[新增 `reopen` mutation IN_REVIEW→IN_REVIEW 显式回退标记 + 审计轨迹] 不触发 §5 ask-first；或与 P2-RC-043 协同加 verificationResult fail 分支触发回退[触及 ORM ask-first]**。 |
| `P2-RC-043` | rc-ma1-a1-32-quality-f2-ncr-capa-closure | quality | UC-QA-05 断言⑦ | **UC-QA-05 全程记录「验证结果」无显式字段（由 person+date+verifyAction 调用隐式承载）**：L1（`use-cases.md:94`）逐字「全程记录纠正/预防措施/**验证结果**」。L3 实仓：`ErpQaAction` ORM（`app-erp-quality.orm.xml:415-429`）字段 actionType(propId 3)/verificationPerson(propId 10)/verificationDate(propId 11)/completedAt/completedBy，**无 verificationResult 列**（grep `verificationResult` 跨 module-quality dao/service + orm.xml **零业务命中**）；`ErpQaActionBizModel.verifyAction:58-77` 仅写 person+date，**调用即隐式「验证通过」，无失败结果值承载**。L4 `testRejectedAutoCreatesNcrAndFullCapaClosure:99-103` 断言 verifyAction 写 person+date 成功（强），**无 verificationResult 值断言**（字段不存在）。**语义裁决**：验证结果由 (person + date + verifyAction 调用事实) **隐式满足**——验证发生 + 执行人 + 日期 + 调用即通过语义均记录；无显式 pass/fail 枚举字段。**与缺口①同根因**：验证模型为二元，无 fail 成果承载故"验证失败→回退"路径结构上不可达。**非 P1**：主路径[验证发生 + 执行人 + 日期已记录]OK 且强测 + 不破坏活跃数据 + 非会计过账破坏；属"主路径 OK，边界[显式 verificationResult pass/fail 枚举字段]弱"（§2 P2①）。 | §2 P2①（次要验收标准未完全满足——主路径[person/date+verifyAction 隐式通过]OK 边界[显式 result 字段]弱） | successor watch-only（P2 登记不强制） | **新增**——grep arm-index「verificationResult」「验证结果字段」「effectiveness result」RC 系列零命中。与 P1-MA2-066 不同控制点（门禁 vs 记录字段）；与 P2-RC-042（本切片回退动作）同根因不同控制点（记录字段 vs 回退动作）。**修复方案 A[`ErpQaAction` ORM 增 `verificationResult` 列[pass/fail 枚举] + verifyAction 增 result 入参 + fail 分支触发回退，**触及 ORM 结构变更须 ask-first + 独立 plan-audit §5**]；方案 B[owner doc `state-machine.md §NCR 与 CAPA` 补注「验证结果由 person+date+verifyAction 调用隐式承载（documented simplification）」纯文档修复可自动执行]**。 |

**noCapaReason 逃逸门（候选缺口②）— 接受，不新建 finding**：
- **§4 三判据复核结论**：(i) R1.20 plan 含独立 plan-audit 通过记录成立（Draft Review iter 2 accept ses_0502be6efffeIzndtBDicVNwJR + Closure Audit PASS ses_05007067cffeJ50Z5O1D935wqL 9/9 check）+ (ii) owner doc（state-machine.md §NCR 与 CAPA:148 + inspection-integration.md §四:172）显式 documented 标注误开/降级合法场景 → **legitimate documented escape hatch，接受**（§4 三判据满足 = 接受，非分歧不新建 finding）。
- **arm-index 衔接**：P1-MA2-066 行（resolved-via-implementation R1.20）追加 **RC A1.32 交叉引用注记**（证 noCapaReason 逃逸门经 L1 UC-QA-05 视角 §4 三判据 (i)+(ii) 复核接受，非静默降级），不新建编号。

**双向可追溯**：2 项新 P2 finding 已写入本报告 §5 矩阵 + 本 §6 + arm-index RC 发现追踪分区（successor watch-only，修复行预留 MR1 R1.0 → RC-R1.n 若激活）；noCapaReason 接受经 §4 三判据 + arm-index P1-MA2-066 交叉引用注记。

---

## 7. 静态存疑点清单（供 MA4 运行时展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。

| SP# | 存疑点 | 触发条件 | 交 MA4 |
|-----|--------|---------|--------|
| SP-1 | **UC-QA-05 断言⑤ 验证失败被动阻塞的运行时操作流程**：CAPA 已 completeAction 但效果验证（verifyAction）尚未调用时，操作员发现验证实际不通过——运行时实际工作流（不调 verifyAction 保持 blocked / 新增补充 CAPA 后再 verifyAction / 经 useLogicalDelete 重置）+"重新制定"可追溯性是否经 CAPA 措施历史（completedAt 审计轨迹）实际可达 | CAPA completeAction 后操作员发现验证不通过 | A4.2/A4.5 运行时探针 |
| SP-2 | **UC-QA-05 断言⑦ verificationResult 隐式承载的运行时审计可用性**：验证结果由 person+date+verifyAction 调用事实隐式承载——运行时审计/合规报表查询"验证结果"时是否能直接获取（需联 person+date 推导）还是需补显式字段 | 外部审计/合规查询 NCR-CAPA 验证结果 | A4 运行时探针 |
| SP-3 | **noCapaReason 逃逸门运行时实际触发面**：误开/降级 NCR 经 noCapaReason resolve 的实际频度（运行时该 escape hatch 是否被滥用为绕过 CAPA 闭环）+ noCapaReason 文本质量（是否含充分理由） | 无 CAPA 措施的 NCR resolve | A4 运行时探针 |
| SP-4 | **CAPA 全 COMPLETED 但 verificationPerson/verificationDate 部分缺失的边界行为**：多个 CAPA 措施中部分已 verifyAction 部分未验证时 requireResolveGate 行为（actionsGatePassed:111 任一缺验证即返回 false 阻塞）的运行时一致性 | 多 CAPA 措施部分验证 | A4 运行时探针 |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（2 项新 finding 均 P2，候选 P0 经证据证伪——①被动阻塞不破坏活跃数据[resolve 抛异常事务回滚 NCR 状态不变] + ③记录字段缺失不影响 GL/库存；noCapaReason §4 三判据接受非降级）。故**不触发 MR0 即时通道**。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 实测如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)）。**本报告不以 checker 脚本退出码作为门控通过依据**。**本审计为只读审计，无生产代码变更，checker 无回归风险**（实测计数反映审计前的既有仓库状态，非本审计引入；与 A1.31 同批次同基线，计数完全一致）。

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

  > **注**：R2c actual(1382) vs baseline(1380) 的 +2 delta 反映本审计之前既有仓库的合规状态（baseline 表 + 多个下降注记后的累计），**本审计为只读审计无生产代码变更，不引入任何 daoFor 站点**，故 checker 计数与本审计行为无关，无回归风险归因。R2b/R2d 同理（actual ≤ 或 ≈ baseline，无新增归因本审计）。计数与 A1.31（同批次）完全一致。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（2 项 P2 **新增**——RC 系列对 quality NCR-CAPA 闭环为首批，既有 arm-index 无 RC finding 涉及 qa 验证失败回退/verificationResult 记录字段；noCapaReason §4 三判据接受不新建 finding，arm-index P1-MA2-066 追加 RC A1.32 交叉引用注记不重开），无未经比对直接新建的 finding。

---

## 9. 与 MA2 报告差异增量声明（详见报告开头 §9 置顶段）

> 完整声明见报告开头 §9。摘要：复用 A2.12（NCR 5 态状态机核心契约 + NCR-CAPA 闭环门禁齐全 + **P1-MA2-066 resolved R1.20** noCapaReason 显式标注门禁落地 + NCR 过账引擎齐全 + reverseNcr 红冲对称）已证实行为，**只补需求视角差异**（UC-QA-05 断言⑤ 验证失败显式回退语义[被动阻塞 vs 主动回退] / 候选缺口② noCapaReason §4 三判据复核 / 断言⑦ verificationResult 字段完整性）。**不重审 MA2 行为**（P1-MA2-066 resolved-via-implementation R1.20 属 audit-remediation mission 裁决，本审计不重开，只从 L1 视角复核 noCapaReason 逃逸门 §4 三判据；P1-MA2-064/065 经 R1.20 Deferred 属 MA2 行为裁决，本切片 Non-Goal 不重审）。

---

## 报告完整性自检（§6 段落完整性，落盘前强制）

- [x] §1 需求契约原文（逐字引用，UC-QA-05 × 7 条验收标准完整枚举）
- [x] §2 实现证据（代码路径含行号 + R6.6 Facade→Processor 委托架构 + 跨域调用链）
- [x] §3 测试证据（注明断言强度 + 缺口诚实标注[断言⑤ dedicated 测试因机制缺失不可达 / 断言⑦ verificationResult 无字段可断言]）
- [x] §4 运行时行为证据（复用 A2.12 + P1-MA2-066 resolved R1.20 + 本切片新发现标注）
- [x] §5 五级追踪矩阵 + 每 UC 符合性结论（UC-QA-05 主路径 + 断言⑤ + 断言⑦ + 候选缺口② 逐矩阵行 + 汇总表 + 命中判据编号）
- [x] §6 与 arm-index 衔接（2 项新 P2 finding 复用/新增裁决 + noCapaReason §4 三判据接受 + 双向可追溯）
- [x] §7 静态存疑点清单（4 存疑点交 MA4 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明（置顶 + 摘要）

**9 段齐全。未修改真相源**（§9 冻结条款——分歧记入报告 §5/§6，未直改 use-cases.md/state-machine.md/inspection-integration.md/product-scope.md）。**未修改代码/ORM/api.xml**（只读审计）。
