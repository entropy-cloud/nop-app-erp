# 2026-08-05-1830-3 rc-ma1-a1-32-quality-f2-ncr-capa-closure 质量域 quality-F2 NCR-CAPA 闭环需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.32（MA1 需求追踪矩阵审计 — quality-F2 NCR-CAPA 闭环：效果验证门禁 + 验证失败回退 + 全程记录）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.32
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.32 的 0.2 依赖）、`2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md`（同批次 N=2，quality-F1 检验门控为 NCR 创建的前置环节，F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.32 给出 UC 清单 = `UC-QA-05`（1 UC），含 `use-cases.md:82` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 quality 域第二个 RC 切片（quality 域共 3 切片 A1.31/A1.32/A1.33），仅 1 UC 但为质量闭环核心（效果验证门禁）。

- **L1 需求契约（权威真相源）**：`docs/design/quality/use-cases.md`（机制见 `quality/state-machine.md §NCR`、`quality/inspection-integration.md §四`）：
  - UC-QA-05 NCR-CAPA 闭环（`:82`）：NCR: OPEN → IN_REVIEW → 制定 CAPA（纠正+预防）→ 执行 CAPA → **效果验证**：验证通过→RESOLVED；**验证失败→返回 IN_REVIEW（重新制定）**；**未通过效果验证→NCR 不可 RESOLVED**；全程记录纠正/预防措施/验证结果。
  - **L1 关键不变量**：① 效果验证是 RESOLVED 的强制前置；② 验证失败有显式回退路径（→IN_REVIEW 重新制定）；③ RESOLVED 门禁（未通过验证不可 RESOLVED）；④ 全程审计（纠正/预防/验证结果记录）。

- **L3 代码实现现状（实测）**——UC-QA-05 主路径已实现且测试强，**R1.20 已修复无 CAPA 逃逸门**；注：NCR 状态机采用 R6.6 每-mutation-一-Processor 架构（Facade 委托 Processor）：
  - **NCR 状态机**（✅ 已实现 & 强测）：`entity/ErpQaNonConformanceBizModel.java`（R6.6 Facade，每 mutation 委托独立 Processor）：`submitReview:49-53`（OPEN→IN_REVIEW，守卫 `requireNcrStatus(...NCR_STATUS_OPEN...) :52`）、`resolve:60-65`（委托 `processor/ErpQaNonConformanceResolveProcessor.resolve:28-49`，**IN_REVIEW 守卫在 ResolveProcessor:33** `requireNcrStatus(...NCR_STATUS_IN_REVIEW...)`）、`postNcr:68-72`、`escalateToRecall`/`upgradeToRecall:91-93`（委托 UpgradeToRecallProcessor）、`cancel:97-108`（守卫 OPEN/IN_REVIEW）；非法迁移抛 `ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION :119/127`。**无 `reopen` 方法**——验证失败回退经 resolve 抛异常隐式保持 IN_REVIEW（见下候选偏差①），无显式 reopen 动作。
  - **NCR-CAPA 闭环门禁**（✅ 已实现 & 强测，P1-MA2-066 fixed R1.20）：`entity/NcrLifecycleService.java`（resolve 门禁 `requireResolveGate:134-145`：**无 CAPA 措施时须显式提供 `noCapaReason`（误开/降级场景）才放行，否则抛 `ERR_NCR_RESOLVE_NO_CAPA :141`**；**有措施时必须全 COMPLETED + verificationPerson/verificationDate `:111` 否则抛 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED :144`**）；R1.20 已堵旧 `isEmpty→true` 逃逸（现 `actionsGatePassed:102-106` 要求 `StringHelper.isNotBlank(noCapaReason)`，`ErpQaNonConformanceResolveProcessor:40-42` 持久化 noCapaReason）。**与 L1 一致**——效果验证是 RESOLVED 强制前置（R1.20 修复后无 CAPA 逃逸门已堵）。
  - **CAPA 措施生命周期**（✅ 已实现 & 强测）：`entity/ErpQaActionBizModel.java`（R6.6）：`startAction:36-42`（PENDING→IN_PROGRESS）、`completeAction:46-54`（→COMPLETED）、**`verifyAction:58-77`（效果验证——填写 verificationPerson/verificationDate 即隐式验证通过，无 `execute` 方法）**；措施模型 `ErpQaAction`（dict `erp-qa/action-type` CORRECTIVE/PREVENTIVE/CAPA 区分纠正/预防）。
  - **候选偏差（documented scope 待核）**：
    - ① **L1 `:91`「验证失败→返回 IN_REVIEW（重新制定）」显式回退路径**：实测 `NcrLifecycleService` resolve 门禁在验证未通过时**抛异常阻止 RESOLVED**（block），NCR 保持当前态 IN_REVIEW，但 L1 措辞暗示有**主动回退动作**（verification failed → 主动转回 IN_REVIEW 重新制定）而非仅被动阻塞。代码无 dedicated「验证失败→reopen to IN_REVIEW」动作（Facade 无 reopen 方法）——**语义等价**（仍处 IN_REVIEW 可重新制定 CAPA 后再 resolve），但形式上是被动阻塞。须核 owner doc `state-machine.md §NCR` 是否声明验证失败为显式迁移。**倾向接受/P2**（语义等价，被动阻塞 vs 主动回退）。注：无 dedicated 测试覆盖「CAPA 已执行但效果验证失败」的主动回退——但因无主动回退机制存在，测试缺口是机制缺失的自然结果而非断言强度不足。
    - ② **`noCapaReason` 逃逸门 L1 未提及**：R1.20 修复引入的 `noCapaReason`（无 CAPA 措施时显式标注误开/降级才放行）是 P1-MA2-066 修复方案 A 的 deliberate 决策（误开 NCR 合法场景），**L1 UC-QA-05 未提及此逃逸门**。须 §4 三判据复核：(i) R1.20 plan 含独立 plan-audit 通过记录；(ii) owner doc state-machine.md/inspection-integration.md §4.3 是否显式标注「无 CAPA 措施允许 resolve 用于误开场景」；(iii) product-scope 是否裁剪。**倾向接受**（P1-MA2-066 resolved R1.20，noCapaReason 是经审计裁决的合法逃逸门，非静默降级——须复核 §4 三判据证据）。
    - ③ **全程记录（纠正/预防/验证结果）字段完整性**：`ErpQaAction` ORM 承载 `actionType`(CORRECTIVE/PREVENTIVE/CAPA，纠正/预防区分 ✅) + `verificationPerson`(propId 10) + `verificationDate`(propId 11)，**但无 `verificationResult` 列**——`verifyAction:58-77` 仅写 person+date，调用该动作即隐式「验证通过」，**无失败结果值承载**。L1 `:94`「全程记录...验证结果」须裁决：由 (person+date + 隐式通过语义) 满足，还是要求显式 result 字段。**倾向接受/P2**（次要验收标准，person+date+verifyAction 调用事实已记录验证发生与执行人）。

- **L4 测试证据现状**（`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/`）：
  - `TestErpQaNcrCapaEndToEnd.java`（5 @Test：testRejectedAutoCreatesNcrAndFullCapaClosure:61——**强**，断言 REJECTED→自动 NCR + CAPA 全闭环 RESOLVED；testEscalateToRecallTerminal:113；testCancelFromOpenAndReview:132；testIllegalNcrTransitionsRejected:144——**强**，断言非法迁移抛 ERR_INVALID_NCR_STATUS_TRANSITION；testResolveNoCapaGate:158——**强**，断言 R1.20 无 CAPA 逃逸门 ERR_NCR_RESOLVE_NO_CAPA/noCapaReason 机制）。
  - `TestErpQaNcrPosting.java`（8 @Test：testScrapAutoPostedOnResolve:86/testReverseNcrClearsPostedAndRedOffset:165——覆盖 NCR 过账 + 红冲，与闭环 RESOLVED 相关）。
  - **⚠️ 潜在测试缺口**：验证失败显式回退路径（verification failed → IN_REVIEW 重新制定）的 dedicated 测试（testResolveNoCapaGate 覆盖无 CAPA 门禁，但「CAPA 已执行但效果验证失败」的回退路径断言强度须核）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12）：NCR 5 态状态机核心契约经实仓逐项证据确认——**NCR-CAPA 闭环门禁齐全**（resolve 守卫 + CAPA COMPLETED + 验证）；**P1-MA2-066**（NCR resolve 允许无 CAPA 直接关闭闭环不变量缺口——**resolved R1.20**，UC-QA-05 直接相关，R1.20 落地 noCapaReason 门禁）。
  - A2.12 关键 finding（直接相关）：`P1-MA2-064`（作废联动 resolved R1.20）、`P1-MA2-065`（dict 死状态 resolved R1.20）、`P1-MA2-066`（NCR resolve 无 CAPA 门禁 **resolved R1.20**）、`P2-MA2-063`（state-machine.md 缺独立章节 watch-only）。
  - `docs/audits/2026-07-2*-arm-ma4-*`（A4）：P1-MA1-022（跨域只读 daoFor 事务回滚覆盖）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用 P1-MA2-066（NCR resolve 无 CAPA 门禁 resolved R1.20）+ A2.12 NCR 状态机/闭环已证实行为，只补需求视角差异（验证失败显式回退路径语义 / noCapaReason 逃逸门 §4 三判据 / 全程记录字段完整性）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-066`（NCR resolve 无 CAPA 门禁 **resolved R1.20**，UC-QA-05 直接相关）、`P1-MA2-064/065`（resolved R1.20）、`P1-MA1-022`（跨域只读 daoFor）、`P2-MA2-063`（文档 watch-only）。**RC 系列对 quality NCR-CAPA 为零**（本切片为 quality 域首批 RC 切片之一，与 A1.31 同批）。本切片须 grep arm-index qa NCR/CAPA/resolve/verification/effectiveness 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（验证失败回退语义 / 全程记录字段）均属**代码逻辑**类（预授权——BizModel/Processor 调整）；若全程记录要求新增 verificationResult 列则触及 ORM ask-first；noCapaReason 已由 R1.20 落地，本审计仅复核不修复。

- **剩余差距**：A1.32 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.32 报告并登记 finding，解除 quality 域 NCR-CAPA 闭环切片证据缺口。

## Goals

- 产出 A1.32 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-32-quality-f2-ncr-capa-closure.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-QA-05 逐条核验**每条验收标准**（完整枚举，§3）：OPEN→IN_REVIEW→CAPA→执行→效果验证→RESOLVED/回退 全链逐条。
- 对候选缺口给出分级结论：①验证失败显式回退路径语义（被动阻塞 vs 主动回退，倾向**接受/P2**）；②noCapaReason 逃逸门 §4 三判据复核（P1-MA2-066 resolved R1.20，倾向**接受**复核）；③全程记录字段完整性（纠正/预防/验证结果，倾向**接受**复核）——按 §2 判据定级，若为 P0/P1 则新建 `P1-RC-xxx`（与 A1.30/A1.31 协调序号）并按 §10 触发 MR1（本计划仅登记，不实施修复）；UC-QA-05 主路径已实现且 P1-MA2-066 resolved → 复核接受。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/state-machine.md/inspection-integration.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.31 quality-F1 检验门控独立 plan；A1.33 SPC 与看板独立；A1.32 只覆盖 UC-QA-05）。
- **不重审 NCR 过账/退货处置主路径**（UC-QA-02 RETURN/SCRAP 过账属 A1.31，本切片仅核 NCR-CAPA 闭环门禁 + 效果验证回退）。
- **不复审 UC-QA-09/10/11/12**（SPC + 看板属 A1.33）。
- **不重跑 P1-MA2-066 行为审计**（§去重协议：NCR resolve 无 CAPA 门禁由 P1-MA2-066 resolved R1.20，只补需求视角差异[验证失败回退/noCapaReason §4/全程记录]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.32 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.32 UC 锚点）+ `docs/design/quality/use-cases.md`（L1 真相源）+ `docs/design/quality/state-machine.md §NCR`+`inspection-integration.md §四`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.12/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-quality/erp-qa-service -Dtest=TestErpQaNcrCapaEndToEnd,TestErpQaNcrPosting`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-32-quality-f2-ncr-capa-closure.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-QA-05 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:82` 验收标准原文（OPEN→IN_REVIEW→CAPA→执行→验证→RESOLVED/回退 5 条断言逐条）；L2 引用 `state-machine.md §NCR`+`inspection-integration.md §四.3`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpQaNonConformanceBizModel.java`（Facade）/`ErpQaNonConformanceResolveProcessor.java`（resolve 守卫）/`NcrLifecycleService.java`/`ErpQaActionBizModel.java`/`ErpQaAction` 实体（含行号，注意 R6.6 Facade→Processor 委托架构）；L4 引用 `TestErpQaNcrCapaEndToEnd.java#method`（注明断言强度）；L5 复用 A2.12（P1-MA2-066 resolved R1.20）+ A4。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①OPEN→IN_REVIEW（submitReview:49-53 守卫 OPEN :52）；②制定 CAPA（纠正+预防，ErpQaAction actionType CORRECTIVE/PREVENTIVE/CAPA 区分复核）；③执行 CAPA（ErpQaActionBizModel `startAction:36-42`/`completeAction:46-54`，**无 execute 方法**）；④**效果验证通过→RESOLVED**（verifyAction:58-77 写 verificationPerson/verificationDate + requireResolveGate:134-145）；⑤**验证失败→返回 IN_REVIEW（重新制定）**（resolve 抛异常阻塞 vs 主动回退动作语义复核——Facade 无 reopen 方法，被动阻塞保持 IN_REVIEW + state-machine.md §NCR 是否声明验证失败显式迁移）；⑥**未通过验证→不可 RESOLVED**（ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED:144 门禁）；⑦全程记录（**ErpQaAction 无 verificationResult 列**——actionType/verificationPerson/verificationDate 复核 + 裁决 L1「验证结果」是否由 person+date+verifyAction 调用隐式满足）；⑧**noCapaReason 逃逸门 §4 三判据复核**（R1.20 plan-audit 通过记录 + owner doc 显式标注 + product-scope 裁剪三判据 + actionsGatePassed:102-106 isNotBlank 门禁复核）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对 UC-QA-05 给出符合性结论（取最高）：主路径（OPEN→IN_REVIEW→CAPA→验证→RESOLVED + 门禁）已实现 + P1-MA2-066 resolved R1.20 → 倾向**接受**；验证失败回退语义（被动阻塞 vs 主动回退，Facade 无 reopen）→ 倾向**接受/P2**（语义等价）；noCapaReason 逃逸门 → 倾向**接受**（§4 三判据复核为经审计裁决的合法逃逸）；全程记录字段（无 verificationResult，person+date+verifyAction 隐式）→ 倾向**接受/P2**复核。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-QA-05 矩阵行（5 条验收标准逐条进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.12/A4 来源
- [ ] UC-QA-05 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-③ 有明确分级；noCapaReason 有 §4 三判据复核路径；验证失败回退有明确语义裁决

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-32-quality-f2-ncr-capa-closure.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` qa NCR/CAPA/resolve/verification/effectiveness/noCapaReason 同域同控制点后裁决——UC-QA-05 resolve 无 CAPA 门禁已由 P1-MA2-066 resolved→复用注记（不重开，复核 R1.20 noCapaReason 落地）；验证失败回退语义 + 全程记录字段为**新发现**（既有 arm-index 无 RC finding 涉及 qa 验证失败回退/记录字段）→ 若确认为分歧则新建 `P*-RC-xxx`（与 A1.30/A1.31 协调序号）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 noCapaReason 逃逸门在实际误开 NCR 场景的运行时行为、验证失败后 NCR 实际状态保持/回退的运行时确认、ErpQaAction verificationResult 字段实际写入内容、CAPA 全 COMPLETED 但验证未填的边界行为等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12 P1-MA2-066 NCR resolve 无 CAPA 门禁 resolved R1.20 + NCR 状态机/闭环已证实），列明只补的需求视角差异（验证失败回退语义 / noCapaReason §4 三判据 / 全程记录字段完整性）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.32 行。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding（若有）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_030db371bffe3Yt1Ri3TkxFGlP，fresh session，未起草本计划）。Blocking issues 经主代理实仓复核全部 CONFIRMED：①**L3 引用错误 `ErpQaNonConformanceBizModel`**——resolve 守卫误指 Facade `:91-97`（实为 `upgradeToRecall`），实际 resolve 在 Facade `:60-65` 委托 `ErpQaNonConformanceResolveProcessor.resolve:28-49`（IN_REVIEW 守卫在 ResolveProcessor:33），**Facade 无 `reopen` 方法**（cancel 在 :97-108）→ 已纠正为 R6.6 Facade→Processor 委托架构 + 删除虚构 reopen；②**L3 引用错误 `ErpQaActionBizModel`**——「execute/verifyAction (:35-58)」误名（无 execute 方法，实为 `startAction:36-42`/`completeAction:46-54`/`verifyAction:58-77`）且 :35-58 截断了本切片核心不变量方法 verifyAction → 已纠正方法名 + 行号；③**`verificationResult` 字段不存在**——`ErpQaAction` ORM 仅 actionType/verificationPerson/verificationDate，verifyAction:58-77 仅写 person+date，L1「验证结果」由隐式通过语义满足 → 已纠正为候选 P2 复核。另将验证失败回退测试缺口重新表述为「无主动回退测试因无主动回退机制存在（被动阻塞）」。已据上述修订 Current Baseline / Phase 1 / 保护区域段。
- Independent draft review iteration 2: `accept`（独立子代理 ses_030d33f12ffeVNubnbqzvi9Qrt，fresh session，未起草本计划）。iteration-1 三项 blocking citation 全部 live-verified 修正：①Facade `resolve:60-65` 委托 `ErpQaNonConformanceResolveProcessor.resolve:28-49`（IN_REVIEW 守卫在 ResolveProcessor:33），无 `reopen` 方法，cancel:97-108；②`ErpQaActionBizModel` 方法为 `startAction:36-42`/`completeAction:46-54`/`verifyAction:58-77`（无 execute）；③ORM grep 确认 `ErpQaAction` 无 `verificationResult` 列（仅 actionType/verificationPerson/verificationDate），L1「验证结果」已显式表述为隐式满足候选 P2。scope（UC-QA-05 only，只读）、anti-slack（3 候选缺口均带明确 P2/§4-三判据裁决路径）、baseline↔Phase1 一致性均成立；无新 blocking（仅 `requireResolveGate` 行范围 134-145 实为 134-146 含 javadoc 的亚行精度问题，错误码行 :141/:144 精确）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.32 报告 9 段齐全 + UC-QA-05 矩阵行（5 条验收标准逐条）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.32 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（验证失败回退语义 / 全程记录字段）均属**代码逻辑**类（预授权——BizModel/Processor 调整，不涉及 ORM 结构变更）；noCapaReason 已由 R1.20 落地，本审计仅复核不修复。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <待完成时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
