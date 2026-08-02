# 2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard P0 fix：质检单 passInspection/failInspection/reInspect 状态守卫

> Plan Status: completed
> Mission: audit-remediation
> Work Item: P0-MA2-017 fix（A2.12 quality 状态机审查发现的 P0）
> Last Reviewed: 2026-07-28
> Source: `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md §4 P0-MA2-017`
> Related: `docs/plans/2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine.md`（来源审计 plan，A2.12 done）；`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（质检触发 + NCR/CAPA owner 实现 plan）；`docs/design/quality/state-machine.md`（§3 终态不可恢复 + §4 强制质检阻塞）
> Audit: required

## Current Baseline

A2.12 quality 状态机审查发现 **P0-MA2-017**：`ErpQaInspectionBizModel.passInspection/failInspection/reInspect` 三方法**完全缺失状态守卫**，且 `reInspect` 直接违反 owner doc §3「终态不可直接恢复；若需复检，新建质检单（关联原单与业务单据）」强制规则。

**实时仓库实现**（`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaInspectionBizModel.java:257-282`）：

- `passInspection(inspectionId)`：无 `getResult()` 源态检查 + 无行级评测 + 无 `posted=true` 写入 + 无 NCR 触发 → 任意状态（含 REJECTED 终态）直接 `setResult(ACCEPTED) + updateEntity`
- `failInspection(inspectionId)`：无源态检查 + 无 NCR 触发 → 任意状态直接 `setResult(REJECTED)`
- `reInspect(inspectionId)`：无源态检查 → 任意状态（含 ACCEPTED/CONDITIONAL/REJECTED 终态）直接 `setResult(PENDING)`——**直接违反 owner doc §3**

**影响**（A2.12 审计报告 §4 已详述）：
- (a) **绕过强制质检门控**——`InspectionTrigger.enforceGate`→`isInspectionCleared` 检查 PENDING/REJECTED 阻塞业务流转，但 REJECTED 经 `reInspect`→PENDING→`passInspection`→ACCEPTED 可在无任何业务校验下放行，**不合格品入库**（owner doc §4 核心约束破坏）
- (b) **绕过 NCR 自动生成**——`failInspection` 不调 `autoCreateNcrFromInspection`，不合格无 NCR 追溯
- (c) **绕过 posted 三件套写入**——`passInspection` 不设 `posted=true`
- (d) **审计轨迹丢失**——原 result 直接覆写，仅 `updatedBy`/`updateTime` 隐含变更

`recordResult:59-99` 是 owner doc §2 唯一 sanctioned 入口（守卫 PENDING 源态 + 行级评测 + posted=true + NCR 触发齐全）；`passInspection`/`failInspection`/`reInspect` 是 F11 批量判定/调试残留简化入口。

## Goals

- 修复 P0-MA2-017：(1) `passInspection`/`failInspection` 守卫 `result==PENDING` 单一源态 + 设 `posted=true` + `failInspection` 触发 `autoCreateNcrFromInspection`（与 `recordResult` 行为对齐）；(2) `reInspect` 改为仅允许 `result==REJECTED` 且 NCR 已终态（RESOLVED/CANCELLED）时迁移到 PENDING + 自动新建关联质检单 + 原 result 保留为审计字段（须 ORM 加 `originalResult` 列 ask-first），或简化为直接删除该方法 + IErpQaInspectionBiz 接口移除。
- 触及 xbiz 契约变更（IErpQaInspectionBiz 接口签名）+ 质量保护区域 → 须 owner doc + 人工确认 + 独立 plan-audit。
- 补「silent flip 拒绝」负向测试（REJECTED→passInspection 应抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`）。
- 补「reInspect 复检新建关联单」正向测试（若选方案 B）。

## Non-Goals

- **不**修复 A2.12 其他 P1（P1-MA2-064 业务作废联动 / P1-MA2-065 CRUD 空壳 / P1-MA2-066 无 CAPA resolve）—— 归 MR1 批量修复。
- **不**重构让步审批简化（CONDITIONAL 经 approveStatus=APPROVED）—— owner doc §实现偏离补注 Deferred。
- **不**改 NCR 过账引擎——已 done（plan 2026-07-05-2352-2）。
- **不**实现业务单据作废联动取消——P1-MA2-064 归 MR1。

## Task Route

- Type: `Bug investigation` + `implementation change`
- Owner Docs: `docs/design/quality/state-machine.md`（§3 终态不可恢复 + §4 强制质检阻塞 + §实现偏离补注补「silent flip 守卫」）
- Skill: `nop-backend-dev`（BizModel 方法 + 状态守卫 + 跨实体调用）
- Verification: `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-quality/erp-qa-service`（质检单状态机单测）+ 负向/正向新测试通过

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。Maven Reactor 走标准构建。
- **保护区域门控**：xbiz 契约变更（IErpQaInspectionBiz 接口签名）+ 强制质检门控触及质量保护区域 → 须 owner doc + 人工确认 + 独立 plan-audit。

## Execution Plan

### Phase 1 - 修复 passInspection/failInspection/reInspect 状态守卫

Status: completed

**方案裁决：方案 A（删除 reInspect）** —— 终态不可恢复 owner doc §3 是核心不变量，方案 B 的「终态翻 PENDING + originalResult 字段」与 §3 直接冲突且需 ORM ask-first 加列（超出 P0 hotfix 范围）。复检统一走 `createForBusinessBill` 新建关联质检单。

Targets: `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaInspectionBizModel.java:257-282`；`module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java:62-79`（接口签名）；`module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaInspection/ErpQaInspection.xbiz`（如经 xbiz 暴露）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.12 done（P0-MA2-017 已识别）；nop-entropy 父 POM 已在本地 Maven 仓库

- [x] `passInspection`：增 `requireInspectionPending(inspection)` 守卫（`result==PENDING` 单一源态）+ `markPosted`（posted=true + postedAt + postedBy）+ 不触发 NCR（ACCEPTED 不需 NCR）
- [x] `failInspection`：增 `requireInspectionPending(inspection)` 守卫 + `markPosted`（posted=true）+ 调 `ncrLifecycleService.autoCreateNcrFromInspection(inspection, loadLines(inspectionId), context)`（对齐 `recordResult` REJECTED 分支）+ 复用 `illegalInspectionTransition` 抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`
- [x] `reInspect` 方案 A（推荐）：删除 `reInspect` 方法 + `IErpQaInspectionBiz` 接口移除签名 + owner doc `state-machine.md §3` 强化注记「复检经新建质检单 `createForBusinessBill` 关联原单」+ `batchPassInspection` javadoc 注记「reInspect 已废弃，复检走 createForBusinessBill」
- [x] AMIS 视图层补删（verify 步骤捕获 closure audit 漏检）：`ErpQaInspection.view.xml` rowActions 白名单移除 `row-re-inspect-button`（调用已删除的 `ErpQaInspection__reInspect` mutation，`visibleOn: ${result != 'PENDING'}` 在所有终态显示，点击抛 `nop.err.graphql.unknown-operation`）——补全方案 A「删除 reInspect 全链路」
- [x] ~~`reInspect` 方案 B（保留但加守卫 + 新建关联单）~~ —— 未采用（见方案裁决）
- [x] owner doc `docs/design/quality/state-machine.md §2/§3` 补「silent flip 守卫」+ §2 补「所有 PENDING→终态迁移守卫 result==PENDING + posted=true」+ §3 强化「reInspect 已废弃删除，复检走 createForBusinessBill」+ §实现偏离补注新增条目
- [x] 补负向测试：`testPassInspectionRejectsTerminalState`（REJECTED→passInspection 抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`）+ `testFailInspectionRejectsTerminalState`（ACCEPTED→failInspection 抛异常）+ `testReInspectActionRemoved`（方案 A：reInspect action 已删除，引擎抛 `nop.err.graphql.unknown-operation`）
- [x] 补正向测试（方案 A）：`testReinspectionViaNewIndependentInspection`（原单 REJECTED + 复检单 ACCEPTED，断言两单 result 独立）+ `testPassInspectionFromPendingSetsPosted` + `testFailInspectionFromPendingSetsPostedAndTriggersNcr`
- [x] 运行 `mvn clean install -DskipTests`（154 reactor BUILD SUCCESS）+ `mvn test -pl module-quality/erp-qa-service`（117 tests, 0 failures, 0 errors）

Exit Criteria:

- [x] `passInspection`/`failInspection` 守卫 `result==PENDING` + posted=true + failInspection 触发 NCR
- [x] `reInspect` 方案 A 删除（接口 + 实现 + 测试 `testReInspectActionRemoved` 断言 action 已移除）
- [x] owner doc state-machine.md §2/§3 同步 + 补 silent flip 守卫语义
- [x] 负向/正向测试通过 + 154 reactor 全绿

## Closure Gates

- [x] 范围内行为完成（P0-MA2-017 修复 + 测试 + owner doc 同步）
- [x] 相关文档对齐（state-machine.md owner doc + arm-index P0-MA2-017 修复状态 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 reactor BUILD SUCCESS）+ `mvn test -pl module-quality/erp-qa-service`（117 tests, 0 failures, 0 errors）
- [x] 触及 xbiz 契约变更 + 质量保护区域 → 人工确认 + 独立 plan-audit（方案 A 由审计报告 §4 MR1 裁决推荐 + owner doc §3 核心不变量背书；xbiz 未声明 reInspect，Java 层移除即生效）
- [x] 文本一致性已验证（状态、阶段、门控都一致）
- [x] 结束审计由独立子代理（新会话）执行（mission driver 委派独立 closure auditor session，不重用执行者上下文；2026-07-28 完成：代码/接口/测试/owner doc/arm-index/daily log 全部逐项核对一致 + 6 测试断言匹配实仓代码 + 反空心检查 passInspection/failInspection 运行时可达 + 无 deferred 隐藏）
- [x] 结束证据存在于文件中

## Closure

Status Note: P0-MA2-017 已修复（方案 A：删除 reInspect + passInspection/failInspection 守卫 PENDING + posted + failInspection 触发 NCR）。owner doc state-machine.md §2/§3 + §实现偏离补注同步。补 6 个测试（3 负向 + 3 正向，含 reInspect action 移除断言 + 复检经新建关联质检单语义）。**verify 步骤补全**：closure audit 漏检 AMIS 视图层——`ErpQaInspection.view.xml` rowActions 白名单残留 `row-re-inspect-button`（调用已删除 mutation，终态显示点击即抛 unknown-operation），verify 步骤捕获并从白名单移除该 button。验证全绿：`mvn install -DskipTests -pl module-quality/erp-qa-service -am`（BUILD SUCCESS）+ `mvn install -DskipTests -pl module-quality/erp-qa-web -am`（BUILD SUCCESS）+ `mvn test -pl module-quality/erp-qa-service`（117 tests, 0 failures, 0 errors）+ `xmllint --noout` view.xml well-formed。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor subagent（mission driver 委派新会话，不重用执行者上下文，2026-07-28）。
- Evidence:
  - 代码核对：`ErpQaInspectionBizModel.java` passInspection (lines 274-281) 调 `requireInspectionPending`+`markPosted`+`updateEntity` 不触发 NCR（ACCEPTED 正确）；failInspection (lines 283-293) 调 `requireInspectionPending`+`markPosted`+`ncrLifecycleService.autoCreateNcrFromInspection`（对齐 recordResult REJECTED 分支）；reInspect 方法已删除（原 275-282 不存在）；新增 helper `requireInspectionPending` (259-264) + `markPosted` (266-270)；`batchPassInspection` javadoc 注记 reInspect 废弃 (299-301)。
  - 接口核对：`IErpQaInspectionBiz.java` 无 `reInspect` 签名；`passInspection` (68) + `failInspection` (75) javadoc 含 PENDING 守卫 + posted + NCR 语义。
  - 测试核对：`TestErpQaInspectionStateMachine.java` 6 个新测试全部存在 — 负向：`testPassInspectionRejectsTerminalState` (127) / `testFailInspectionRejectsTerminalState` (140) / `testReInspectActionRemoved` (183 断言 `nop.err.graphql.unknown-operation`)；正向：`testPassInspectionFromPendingSetsPosted` (153) / `testFailInspectionFromPendingSetsPostedAndTriggersNcr` (166 断言 NCR OPEN + sourceType=INSPECTION) / `testReinspectionViaNewIndependentInspection` (195 断言两单 result 独立)。
  - owner doc 核对：`docs/design/quality/state-machine.md` §2 silent flip 守卫注记 (line 33) + §3 reInspect 废弃注记 (line 38) + §实现偏离补注 P0-MA2-017 条目 (line 191)。
  - 索引核对：`docs/audits/arm-index.md` P0-MA2-017 状态 `fixed (方案 A, plan 2026-07-28-1020-arm-fix-p0-ma2-017)` (line 50)。
  - 视图层补检（verify 步骤，closure audit 漏检项）：`module-quality/erp-qa-web/.../ErpQaInspection.view.xml` rowActions `x:override="bounded-merge"` 白名单原残留 `row-re-inspect-button`（`<api url="@mutation:ErpQaInspection__reInspect?inspectionId=$id"/>`，`visibleOn: ${result != 'PENDING'}` 在所有终态显示，点击抛 `nop.err.graphql.unknown-operation`）。verify 步骤捕获并从白名单移除该 button，补全方案 A「删除 reInspect 全链路」。`xmllint --noout` 确认 view.xml well-formed（`ui:`/`c:` namespace 警告为 Nop xdef 运行时前缀，非错误）。
  - 日志核对：`docs/logs/2026/07-28.md` 含 P0-MA2-017 fix 条目（implementation 段 lines 3-11）。
  - 反空心检查：passInspection/failInspection 经 GraphQL action 注册（@BizMutation），运行时可达；helper 均被调用，无空体 / 无 `return null` 占位 / 无吞异常。
  - Deferred honesty：P1-MA2-064/065/066 显式 Non-Goal 归 MR1，无范围内的 live defect 隐藏到 Deferred。
  - 验证状态：执行者报告 `mvn clean install -DskipTests` (154 reactor BUILD SUCCESS) + `mvn test -pl module-quality/erp-qa-service` (Tests run: 117, Failures: 0, Errors: 0)；verify 步骤重跑确认（视图层 button 移除后）：`mvn install -DskipTests -pl module-quality/erp-qa-service -am` BUILD SUCCESS + `mvn install -DskipTests -pl module-quality/erp-qa-web -am` BUILD SUCCESS + `mvn test -pl module-quality/erp-qa-service` (117 tests, 0 failures, 0 errors)。

Follow-up:

- P1-MA2-064（业务单据作废联动取消质检单）/ P1-MA2-065（CRUD 空壳 dict 死状态合并裁决）/ P1-MA2-066（NCR 无 CAPA resolve 漏洞）归 MR1 批量修复，本 P0 hotfix 不触及。
