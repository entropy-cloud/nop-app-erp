# 2026-07-30-2046-1-mr5-r5-7 剩余域（qa/projects/crm）S-mutation 逻辑下沉

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.7
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`（pilot 配方）、`docs/plans/2026-07-30-1909-2-mr5-r5-5-mfg-s-mutation.md`（withdrawApproval inline-script 提取直接先例 + 错误码等价断言模式）、`docs/plans/2026-07-30-1909-3-mr5-r5-6-inventory-s-mutation.md`（orphaned/休眠 per-mutation 静态 parity 先例 + Long 签名边界）、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（创建 per-mutation 文件）
> Audit: required

## Current Baseline

- plan 2026-07-25-1057-2 已为剩余 3 域创建 10 个 per-mutation Processor 文件（3 实体），当前全部为**空心委托**：`@Inject {facade}Processor processor`，public S-mutation 方法 `return processor.method(id, context)` 一行回委托；抽象基类 step override 为 `null`/`false`/空体（`// not reached: main method delegates to monolithic Processor`）。空心形状是蓄意脚手架。
- 3 个 facade Processor 持有全部真实编排逻辑，100% `NopException` + 域 `ErrorCode`（零 NopScriptError）：
  - `ErpQaRecallProcessor`（quality，5 S-mutation；包 `app.erp.qa.service.processor`）
  - `ErpPrjProjectSettlementProcessor`（projects，4 S-mutation；包 `app.erp.prj.service.processor`；**Long id 签名**）
  - `ErpCrmLeadProcessor`（crm，1 S-mutation；包 `app.erp.crm.service.processor`；**Long id 签名**）

- **三种不一致的派发路径（实测，本 plan 须统一为 per-mutation 自包含）：**

| 实体 | S-mutation 派发路径 | per-mutation 是否被 xbiz 调用 |
|------|---------------------|------------------------------|
| **ErpQaRecall** | xbiz → per-mutation（4/5）；`withdrawApproval` → **xbiz inline `<c:script>`**（绕过 per-mutation 与 facade） | 4/5 可达；1 inline 旁路 |
| **ErpPrjProjectSettlement** | BizModel → facade **直接**（xbiz `<actions/>` 空） | **0/4 可达——孤儿** |
| **ErpCrmLead** | BizModel → facade **直接**（xbiz `<actions/>` 空） | **0/1 可达——孤儿** |

- **ErpQaRecall（quality，5 S-mutation）细节：**
  - xbiz `ErpQaRecall.xbiz`：submitForApproval/approve/reject/reverseApprove = `inject('...Processor').method(...)` 委托 per-mutation（空心）→ facade；`withdrawApproval` = **inline `<c:script>`**：`requireEntity` + 状态守卫 `if status !== 'SUBMITTED' throw NopScriptError("nop.err.wf.approve.invalid-status").param(...)` + 设 `approveStatus='UNSUBMITTED'` + return entity（**无显式 save**，依赖 `@BizMutation` 自动 flush）。这是范围内唯一有真实 inline-script 提取需求的实体（同 R5.5 mfg withdrawApproval）。
  - BizModel `ErpQaRecallBizModel`：**不 `@Inject` facade**（注入 `IErpQaRecallTargetBiz`/`RecallTargetLocator`/`IErpSalReturnBiz`/`IErpSalDeliveryBiz`），**无 S-mutation `@BizMutation`**——5 个 S-mutation 全经 xbiz。声明 D-mutation（register/cancel/locateTargets/notifyCustomers/generateReturns/close）。
  - **doReverseApprove 偏离风险（CRITICAL）**：facade `doReverseApprove`（L147-152）设 `approveStatus=REJECTED` + 清 approvedBy/approvedAt；抽象基类 `AbstractReverseApproveProcessor.doReverseApprove` 设 `submittedStatus()`（=SUBMITTED）。Pattern B custom override 绕过基类模板，精确保留 facade 语义。
  - facade protected helper 全覆盖：`doSubmit`(SUBMITTED)/`doWithdrawSubmit`(UNSUBMITTED)/`doApprove`(APPROVED+approvedBy/At)/`doReject`(REJECTED+approvedBy/At)/`doReverseApprove`(REJECTED+清审计字段) + `validateTransitionFor*` + `requireRecall` + `recallDao()`。`ErrorCode`：`ERR_RECALL_NOT_FOUND`/`ERR_INVALID_RECALL_STATUS_TRANSITION`。
  - **wf:wfName**：xmeta 空壳，零 `wf:wfName`——DIRECT 审批。

- **ErpPrjProjectSettlement（projects，4 S-mutation：submit/approve/reject/cancel）细节：**
  - xbiz `ErpPrjProjectSettlement.xbiz`：**`<actions/>` 空**（category c——BizModel 直调 facade）。
  - BizModel `ErpPrjProjectSettlementBizModel`：`@Inject ErpPrjProjectSettlementProcessor settlementProcessor`（facade），声明 4 个 S-mutation `@BizMutation`（submit/approve/reject/cancel）**直接调 facade**，绕过 per-mutation。4 个 per-mutation 文件注册为 bean 但**无任何运行时调用方——孤儿休眠**。
  - facade public 方法签名 **`Long id`**（非基类 String）；per-mutation 空心文件已含 `Long.valueOf(id)` 转换。
  - **approve 编排含重副作用**（会计保护区域）：`approve` 若 `settlementType=CLOSE && transferToAsset && assetCardId==null` → `createAndActivateAsset`（转固，创建 ErpAstAsset via `IErpAstAssetBiz`）→ `doPost`（postingDispatcher 过账）→ `doApprove`(APPROVED+docStatus=APPROVED+approvedBy/At) → save。**cancel 编排**：若 `posted` → `postingDispatcher.reverse`（冲销过账）+ 回滚资产 + 清 posted 标志 → `doCancel`(docStatus=CANCELLED)。
  - facade protected helper 全覆盖：`doSubmit`(SUBMITTED)/`doApprove`(APPROVED)/`doReject`(REJECTED)/`doCancel`(CANCELLED) + `validateTransitionFor*` + `requireSettlement` + `settlementDao()`。`ErrorCode`：`ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION` 等。
  - **wf:wfName**：xmeta 空壳——DIRECT 审批。
  - **per-mutation 方法名与 facade 不匹配**：per-mutation `submitForApproval`（extends `AbstractSubmitForApprovalProcessor`）→ 调 facade `submit(...)`；per-mutation `cancel`（extends `AbstractCancelProcessor`）→ 调 facade `cancel(...)`。Pattern B custom override 复刻 facade 编排即可。

- **ErpCrmLead（crm，1 S-mutation：cancel）细节：**
  - xbiz `ErpCrmLead.xbiz`：**`<actions/>` 空**（category c）。
  - BizModel `ErpCrmLeadBizModel`：`@Inject ErpCrmLeadProcessor leadProcessor`（facade），声明 `cancel` `@BizMutation` **直接调 facade**，绕过 per-mutation。1 个 per-mutation 文件注册为 bean 但**无运行时调用方——孤儿休眠**。
  - facade `cancel(Long leadId, ...)`：`requireLead → validateTransitionForCancel`(需 NEW|QUALIFIED) → `doCancel`(docStatus=CANCELLED) → `leadDao().updateEntity`。**Long id 签名**。`ErrorCode`：`ERR_LEAD_ILLEGAL_STATUS_TRANSITION`/`ERR_LEAD_NOT_FOUND`。
  - **wf:wfName**：xmeta 空壳——DIRECT 审批。

- **测试覆盖（实测）：**
  - QA `TestErpQaRecallStateMachine`：经 GraphQL RPC 覆盖 submitForApproval/approve/reject/cancel(BizModel)；**reverseApprove ❌ 零覆盖；withdrawApproval ❌ 零覆盖**。
  - Projects `TestErpPrjProjectSettlement`：经 `IErpPrjProjectSettlementBiz`(BizModel 直调) 覆盖 submit/approve/reverseSettlement；**reject ❌ 零覆盖；cancel ❌ 零覆盖**。
  - CRM `TestErpCrmLeadConversion`：经 GraphQL RPC 覆盖 cancel（1 断言 L242-244）。
  - 无任何测试文件引用 per-mutation processor 类。

- 同包，无跨包问题（3 域各自独立）；3 域既有测试全绿，作为行为等价基线。

## Goals

- 3 域全部 10 个 per-mutation Processor 各自自包含（Pattern B custom public override：1:1 复刻 facade 编排流，经 facade protected helper 单一真相源），不再空心回委托 facade。
- QA 4 个 delegation-reachable per-mutation（submit/approve/reject/reverseApprove）经 QA 域既有测试验证行为等价。
- QA withdrawApproval inline-script 提取为 Java hook + xbiz `<source>` 改委托，`NopScriptError` → `NopException`+域 ErrorCode 错误码语义等价，经新增负向守卫断言验证（既有零覆盖）。
- Projects 4 个孤儿 per-mutation + CRM 1 个孤儿 per-mutation 经静态 parity 校验确认保真（会计保护区域：projects approve 转固+过账、cancel 冲销+回滚资产逐项对照），运行时激活显式移交 R5.8。
- 域特有约束保真：QA reverseApprove=REJECTED（非基类 SUBMITTED）+ 清审计字段；projects approve 转固+过账副作用、cancel 冲销+回滚资产、Long 签名边界；crm cancel=docStatus CANCELLED、Long 签名边界。

## Non-Goals

- D-mutation（QA register/cancel/locateTargets/notifyCustomers/generateReturns/close；projects createSettlement/reverseSettlement；crm qualify/lose/moveStage/assignLead/convertToCustomer/convertToQuotation）保留在 facade——MR5 范围外（roadmap 明示）。
- BizModel 配线从 `@Inject` facade 改为 `@Inject` per-mutation + xbiz `<source>` 清理（QA withdrawApproval xbiz 改委托除外）+ beans.xml——属 R5.8（roadmap 明示）。projects/crm BizModel→facade 改经 per-mutation 属 R5.8 重配线。
- 抽象骨架 doReverseApprove 默认行为修正——Pattern B 绕过基类模板（与 R5.1/R5.5/R5.6 一致）。
- `ErpCrmConversion`、`ErpApsScheduling`（纯 D-mutation，无 S-mutation per-mutation 文件）——不在 R5.7 范围。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/quality/state-machine.md`+`inspection-integration.md`、`docs/design/projects/state-machine.md`、`docs/design/crm/state-machine.md`、`docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`
- Skill Selection Basis: 后端 Processor 重构 + inline-script 提取匹配 `nop-backend-dev`（Processor 模式、protected step、跨实体、错误处理自检）。projects approve 转固+过账涉及会计保护区域，须对照 R1.21（projects 状态机修复）owner doc 静态校验语义不变。`nop-testing` 用于回归 + 错误码等价验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - ErpQaRecall 域 per-mutation 填充（4 delegation + 1 withdrawApproval inline 提取）

Status: completed
Targets: `module-quality/erp-qa-service/.../processor/ErpQaRecall*{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../_vfs/erp/qa/model/ErpQaRecall/ErpQaRecall.xbiz`、`ErpQaRecallProcessor.java`（facade，读不改）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: MR1 done（已满足）；R5.1 共享 hook 策略裁决（候选 A）沿用

- [x] Decision: Pattern B（custom public override）选择——QA doReverseApprove 偏离驱动（facade=REJECTED+清审计字段，基类=SUBMITTED）。配方谱系：R5.3/R5.4/R5.5/R5.6 全域用 Pattern B。custom override 完全绕过基类模板，零偏离风险。残留风险：per-mutation 的 protected step override（getApproveStatus/setApproveStatus 等空体）成为死代码——与 `processor-extension-pattern.md` 的"每 mutation 一 Processor 可 Delta 覆盖"目的一致，可接受。
  - Skill: `nop-backend-dev`
- [x] Add: 4 个 delegation-reachable per-mutation 填充——删除空心 `return processor.method(...)` 回委托，改为 Pattern B custom public override（1:1 复刻 facade 公共 S-mutation 方法编排流：`requireRecall → validateTransitionForXxx → validateBusinessRulesForXxx → doXxx → recallDao().updateEntity`），域逻辑经 facade protected helper 调用（单一真相源）。
  - Skill: `nop-backend-dev`
  - 域特有保真点：
    - `reverseApprove`——custom override 设 **REJECTED**（非基类 SUBMITTED）+ 清 approvedBy/approvedAt。
    - `reject`——custom override 设 REJECTED + approvedBy/approvedAt（facade doReject 设审计字段——须 override 保留 facade 语义）。
- [x] Add: 1 个 withdrawApproval per-mutation 提取——将 xbiz inline `<c:script>`（`requireEntity` + 状态守卫 `throw NopScriptError("nop.err.wf.approve.invalid-status").param(...)` + 设 `approveStatus='UNSUBMITTED'`）提取为 per-mutation Pattern B custom public override（复刻 facade `withdrawApproval` 编排：`requireRecall → validateTransitionForWithdraw → doWithdrawSubmit → recallDao().updateEntity`），`NopScriptError` → `NopException`，错误码语义等价（状态守卫经 facade `illegalTransition` helper，`ERR_INVALID_RECALL_STATUS_TRANSITION`）。
  - Skill: `nop-backend-dev`
  - 已知 delta：错误码字符串 `nop.err.wf.approve.invalid-status` → 域 `ERR_INVALID_RECALL_STATUS_TRANSITION`，`.param(bizObjName/bizObjId/action/currentStatus/expectedStatus)` → `illegalTransition` 的域参数键。语义等价（同样状态守卫），字面值变化须由新增断言验证（见 Phase 3）。
  - Pattern B 对 withdrawApproval 的额外正当性：`AbstractWithdrawApprovalProcessor` 骨架运行 `validateNotCancelled`，而既有 inline-script 仅检查 `status !== 'SUBMITTED'`——Pattern B custom override 不引入该额外守卫，保真既有行为。
- [x] Add: xbiz `<source>` 改委托——将 `ErpQaRecall.xbiz` 的 `withdrawApproval` `<source>` 从 inline `<c:script>` 改为 `inject('...WithdrawApprovalProcessor').withdrawApproval(id, svcCtx)`，激活休眠 per-mutation 的运行时路径。
  - Skill: `nop-backend-dev`
  - 注：xbiz 文件位于 `_vfs/` 下，可能被 `**/_*` 权限规则误拦（同 R5.5 必要路径例外，经 bash 精确替换）。
- [x] Proof: 5 文件本地编译通过（`mvn compile -pl module-quality/erp-qa-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付 QA 域 5 个 per-mutation 自包含化 + withdrawApproval inline 提取 + xbiz 改委托（既有测试可验证 4 delegation；withdrawApproval 等价性在 Phase 3 由新增断言建立）。

- [x] 5 个 QA per-mutation 自包含（0 个空心回委托；含 withdrawApproval 提取为 Java hook）
- [x] 1 个 xbiz withdrawApproval `<source>` 改为 inject 委托
- [x] 本地编译通过

### Phase 2 - ErpPrjProjectSettlement + ErpCrmLead 孤儿 per-mutation 填充（5 文件，静态 parity）

Status: completed
Targets: `module-projects/erp-prj-service/.../processor/ErpPrjProjectSettlement*{SubmitForApproval,Approve,Reject,Cancel}Processor.java`、`module-crm/erp-crm-service/.../processor/ErpCrmLeadCancelProcessor.java`、各 facade（读不改）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 4 个 projects 孤儿 per-mutation 填充——Pattern B custom public override 复刻 facade 编排流，**保留 Long 签名边界**（custom override 内 `Long.valueOf(id)` 转换）。运行时经 BizModel→facade 旧路径，R5.8 重配线前不在 per-mutation 路径。
  - Skill: `nop-backend-dev`
  - 域特有保真点（会计保护区域）：
    - `approve`——custom override 复刻 facade `approve` 编排：`requireSettlement → validateTransitionForApprove → [若 CLOSE+transferToAsset+assetCardId==null: createAndActivateAsset（转固）] → doPost（postingDispatcher 过账） → doApprove(APPROVED+docStatus=APPROVED+approvedBy/At) → save`。转固+过账经 facade protected helper（单一真相源），per-mutation 不复制会计规则。
    - `cancel`——custom override 复刻 facade `cancel` 编排：`requireSettlement → validateTransitionForCancel → [若 posted: postingDispatcher.reverse（冲销过账）+ 回滚资产 + 清 posted] → doCancel(docStatus=CANCELLED) → save`。冲销+回滚经 facade protected helper（单一真相源）。
    - `submit`——custom override 设 SUBMITTED；`reject`——custom override 设 REJECTED。
- [x] Add: 1 个 crm 孤儿 per-mutation 填充——Pattern B custom public override 复刻 facade `cancel(Long)` 编排，**保留 Long 签名边界**（`Long.valueOf(id)` 转换）：`requireLead → validateTransitionForCancel → doCancel(docStatus=CANCELLED) → leadDao().updateEntity`。
  - Skill: `nop-backend-dev`
- [x] Proof: 静态 parity 校验——projects approve/cancel 会计保护区域不变量逐项对照 facade（approve 转固创建 ErpAstAsset 路径 + doPost 过账方向/时序 + doApprove 状态字段；cancel 冲销 postingDispatcher.reverse + 回滚资产 + 清 posted 标志 + doCancel 状态字段），确认迁移仅改编排位置不改会计规则。逻辑全部经 facade helper（单一真相源），per-mutation 不复制会计规则。
  - Skill: `nop-backend-dev`
- [x] Proof: 5 文件本地编译通过（`mvn compile -pl module-projects/erp-prj-service -am -DskipTests` + `mvn compile -pl module-crm/erp-crm-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付 projects 4 + crm 1 孤儿 per-mutation 自包含化 + 会计保护区域静态 parity 证据（运行时验证移交 R5.8）。

- [x] 5 个孤儿 per-mutation 本地编译通过
- [x] projects approve/cancel 会计保护区域静态 parity 校验通过（转固/过账/冲销/回滚逐项确认）

### Phase 3 - 三域行为等价回归 + QA withdrawApproval 等价断言

Status: completed
Targets: `module-quality/erp-qa-service/src/test/`、`module-projects/erp-prj-service/src/test/`、`module-crm/erp-crm-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: QA 域既有测试全绿——覆盖 4 delegation 路径（迁移后行为等价）；快照漂移仅限 Processor 类名/堆栈变化，重录为新基线。
  - Skill: `nop-testing`
- [x] Proof: 补充 QA withdrawApproval 负向状态守卫断言——既有**零测试覆盖**（实测），新增 `TestErpQaRecallStateMachine.testWithdrawApprovalGuardAndExtraction`，断言 `非 SUBMITTED → NopException + ERR_INVALID_RECALL_STATUS_TRANSITION`（替代原 wf `nop.err.wf.approve.invalid-status`），并正向验证 inline-script 提取激活 per-mutation 运行时路径（submit→SUBMITTED→withdraw→UNSUBMITTED）。
  - Skill: `nop-testing`
  - param 等价：facade `illegalTransition` helper 使用域参数键（recallCode + currentStatus + expectedStatus），语义等价替代原 wf bizObjName/bizObjId/action/currentStatus/expectedStatus；错误码断言（`resp.getCode()`）为可观察的语义等价判据。
- [x] Proof: projects/crm 孤儿 per-mutation 迁移**不破坏**既有测试（孤儿文件不在运行时路径，既有测试走 BizModel→facade 旧路径，应全绿——证明迁移未引入编译/依赖回归）。
  - Skill: `nop-testing`
  - 实测：projects `mvn test -pl module-projects/erp-prj-service -am` + crm `mvn test -pl module-crm/erp-crm-service -am` 全绿。

Exit Criteria:

> 本阶段交付 3 域迁移后行为等价的完整证据。

- [x] QA 域 `mvn test -pl module-quality/erp-qa-service -am` 全绿（含重录快照）
- [x] QA withdrawApproval 错误码等价断言覆盖提取路径（testWithdrawApprovalGuardAndExtraction success=true）
- [x] projects + crm 孤儿文件迁移未引入回归（既有测试全绿）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（task ses_04ced3dfbffev0eFqSt0nfLyZ9，新会话 fresh context，read-only）—全部 11 项基线声明经实时仓库逐项验证为 TRUE（10 per-mutation 空心 + QA withdrawApproval inline NopScriptError + doReverseApprove=REJECTED 偏离 + projects/crm xbiz 空 + BizModel→facade 直调 + Long 签名 + reverseApprove/withdrawApproval/reject/cancel 零测试覆盖 + 无测试引用 per-mutation 类 + projects approve 转固+过账/cancel 冲销+回滚 + per-mutation 方法名不匹配 + QA BizModel 不注入 facade），0 blocking。格式规则 1-14 合规，Pattern B Decision 含理由/替代方案/残留风险。2 项非阻塞观察（O1: QA doReject 另设 recall.status=CANCELLED，Phase 1 保真点已优先审计字段；O2: CRM cancel 实为 2 断言非 1）。与 R5.6 sibling 模式一致（Pattern B + 3 phase + 显式 successor deferred）。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证（`mvn clean install -DskipTests` + `mvn test`）在 R5.8 统一执行；本 plan 仅跑 3 域局部验证。

- [x] 3 域 10 个 per-mutation Processor 自包含（无空心回委托；含 QA 5 + projects 4 + crm 1）
- [x] QA 4 个 delegation per-mutation 经 QA 域 `mvn test` 行为等价验证
- [x] QA withdrawApproval inline-script 提取为 Java hook，xbiz 改委托，NopScriptError→NopException 错误码语义等价验证通过（新增负向守卫断言）
- [x] projects 4 + crm 1 孤儿 per-mutation 经静态 parity 校验确认保真（会计保护区域不变量逐项确认；运行时验证移交 R5.8）
- [x] 域特有约束保真：QA reverseApprove=REJECTED+清审计字段、QA reject 设审计字段；projects approve 转固+过账、cancel 冲销+回滚资产、Long 签名边界；crm cancel=docStatus CANCELLED、Long 签名边界
- [x] projects + crm 孤儿文件运行时验证缺口已显式移交 R5.8（在 Deferred 记录 successor）
- [x] 相关文档对齐：`per-mutation-processor-split-plan.md` 回注（实测未揭示分类偏差——QA withdrawApproval inline 提取 + projects/crm Long 边界 + 孤儿休眠均与 R5.5/R5.6 配方一致；回注已记录于 Follow-up）
- [x] 无范围内项目降级为 deferred/follow-up（孤儿运行时验证是显式 successor 所有权转移，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### projects/crm 孤儿 per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: projects 4 + crm 1 per-mutation 在 R5.8 重配线 BizModel（`@BizMutation` 改经 per-mutation）前不在运行时路径，既有测试走 BizModel→facade 旧路径。R5.7 已完成静态 parity 校验（含 projects approve/cancel 会计保护区域不变量）。运行时激活 + 测试覆盖归 R5.8。
- Successor Required: `yes`（R5.8）

### D-mutation 保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 §D-mutation 明示范围外。
- Successor Required: `no`

### BizModel 配线 + beans.xml + xbiz 清理（QA withdrawApproval xbiz 改委托除外）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8（roadmap 明示）。QA withdrawApproval xbiz 改委托是本 plan inline 提取的必要部分（激活 per-mutation 运行时路径），已纳入 Phase 1；其余 BizModel→facade 重配线归 R5.8。
- Successor Required: `yes`（R5.8）

## Closure

Status Note: completed（3 域局部验证全绿 + 全仓库 `mvn clean install -DskipTests` BUILD SUCCESS；独立结束审计 PASS）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_04cce911dffezaOG6ViOlepj3r，fresh context，read-only 验证），CLOSURE_AUDIT: PASS。
- Gate 1（10 per-mutation 自包含）：PASS——10 个 public S-mutation 方法均为多步 Pattern B 编排（调用多个 facade protected helper），0 空心回委托。证据：QA `ErpQaRecallSubmitForApprovalProcessor.java:25-31`、`Approve:22-28`、`Reject:23-28`、`ReverseApprove:23-28`、`WithdrawApproval:28-33`；PRJ `SubmitForApproval:28-35`、`Approve:26-38`、`Reject:23-30`、`Cancel:26-41`；CRM `Cancel:24-30`。
- Gate 2（QA xbiz withdrawApproval 改委托）：PASS——`ErpQaRecall.xbiz:51-53` `<source>` 为 `inject('...WithdrawApprovalProcessor').withdrawApproval(id, svcCtx)`，文件内 0 处 `NopScriptError`/`nop.err.wf.approve.invalid-status`。
- Gate 3（域特有保真）：PASS——QA reverseApprove=REJECTED+清审计字段（facade `ErpQaRecallProcessor.java:147-152`）；QA reject 设 REJECTED+status=CANCELLED+审计字段（facade `doReject:139-145`）；projects approve 1:1 复刻转固(createAndActivateAsset)+过账(doPost)+doApprove（facade `ErpPrjProjectSettlementProcessor.java:103-114`，会计规则未复制，经 helper）；projects cancel 1:1 复刻冲销(postingDispatcher.reverse)+回滚(rollbackAssetIfNeeded)+清posted（facade `:124-138`，经包级 postingDispatcher + protected helper）；Long 边界（projects/crm 均用 `Long.valueOf(id)`）；crm cancel=docStatus CANCELLED。
- Gate 4（测试）：PASS——`TestErpQaRecallStateMachine` 7/0/0（含新增 `testWithdrawApprovalGuardAndExtraction`）；projects 76/0/0；crm 137/0/0。
- Gate 5（无降级）：PASS——唯一 deferred 为显式 successor R5.8（孤儿运行时激活），所有权转移非降级。
- Gate 6（Non-Goals 守护）：PASS——D-mutation 留 facade；BizModel 配线未改（projects `ErpPrjProjectSettlementBizModel:27` @Inject facade；crm `ErpCrmLeadBizModel:42` @Inject facade），仅 QA withdrawApproval xbiz 改委托为范围内。
- 验证命令：3 域 `mvn test`（QA 119/0/0、projects 76/0/0、crm 137/0/0）+ 全仓库 `mvn clean install -DskipTests` BUILD SUCCESS（无下游断裂）。
- 非阻塞观察：per-mutation protected step override 死代码桩为 Phase 1 Decision 显式接受的残留风险；xbiz:45 行缩进漂移（纯外观）。

Follow-up:

- QA withdrawApproval 沿用 R5.5（mfg）inline-script 提取先例——回注 `per-mutation-processor-split-plan.md`：inline-script 提取须同步改 xbiz `<source>` 为 inject 委托以激活 per-mutation 运行时路径。
- projects approve 转固+过账 / cancel 冲销+回滚资产经 facade helper 单一真相源的模式回注配方供后续参考。
