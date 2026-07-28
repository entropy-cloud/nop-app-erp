# 2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard P0 fix：质检单 passInspection/failInspection/reInspect 状态守卫

> Plan Status: active
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

Status: planned

Targets: `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaInspectionBizModel.java:257-282`；`module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java:62-79`（接口签名）；`module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaInspection/ErpQaInspection.xbiz`（如经 xbiz 暴露）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.12 done（P0-MA2-017 已识别）；nop-entropy 父 POM 已在本地 Maven 仓库

- [ ] `passInspection`：增 `requireInspectionStatus(inspection, PENDING, "PENDING")` 守卫 + 设 `inspection.setPosted(Boolean.TRUE)` + `postedAt/postedBy`（对齐 `recordResult` 行为）+ 不触发 NCR（ACCEPTED 不需 NCR）
- [ ] `failInspection`：增 `requireInspectionStatus(inspection, PENDING, "PENDING")` 守卫 + 设 `inspection.setPosted(Boolean.TRUE)` + 调 `ncrLifecycleService.autoCreateNcrFromInspection(inspection, loadLines(inspectionId), context)`（对齐 `recordResult` REJECTED 分支）+ 复用 `illegalInspectionTransition` 抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`
- [ ] `reInspect` 方案 A（推荐）：删除 `reInspect` 方法 + `IErpQaInspectionBiz` 接口移除签名 + owner doc `state-machine.md §3` 强化注记「复检经新建质检单 `createForBusinessBill` 关联原单」+ F11 `batchPassInspection` javadoc 注记「reInspect 已废弃，复检走 createForBusinessBill」
- [ ] `reInspect` 方案 B（保留但加守卫 + 新建关联单）：仅允许 `result==REJECTED` 且关联 NCR.status ∈ {RESOLVED, CANCELLED, ESCALATED_TO_RECALL}（即 NCR 已闭环）时迁移 → `setResult(PENDING)` + 自动 `createForBusinessBill(relatedBillType="RE_INSPECTION", relatedBillCode=原 inspection.code, ...)` + ORM ask-first 加 `originalResult` 列保留原值
- [ ] owner doc `docs/design/quality/state-machine.md §2/§3` 补「silent flip 守卫」+ 删除/更新 §2 中 passInspection/failInspection/reInspect 简化入口描述
- [ ] 补负向测试：`TestErpQaInspectionBizModel.testPassInspectionRejectsTerminalState`（REJECTED→passInspection 抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`）+ `testFailInspectionRejectsTerminalState`（ACCEPTED→failInspection 抛异常）+ `testReInspectRejectsTerminalState`（若方案 B：ACCEPTED→reInspect 抛异常；若方案 A：方法已删除，测试接口变更）
- [ ] 补正向测试（若方案 A）：`testReInspectViaCreateForBusinessBill`（PENDING→REJECTED→新建关联质检单）+ 断言两单 result 独立
- [ ] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-quality/erp-qa-service`（质检单状态机单测通过）

Exit Criteria:

- [ ] `passInspection`/`failInspection` 守卫 `result==PENDING` + posted=true + failInspection 触发 NCR
- [ ] `reInspect` 方案 A 删除（接口 + 实现 + 测试）或方案 B 加守卫 + 新建关联单 + originalResult 字段
- [ ] owner doc state-machine.md §2/§3 同步 + 补 silent flip 守卫语义
- [ ] 负向/正向测试通过 + 154 reactor 全绿

## Closure Gates

- [ ] 范围内行为完成（P0-MA2-017 修复 + 测试 + owner doc 同步）
- [ ] 相关文档对齐（state-machine.md owner doc + arm-index P0-MA2-017 修复状态 done）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-quality/erp-qa-service`
- [ ] 触及 xbiz 契约变更 + 质量保护区域 → 人工确认 + 独立 plan-audit
- [ ] 文本一致性已验证（状态、阶段、门控都一致）
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Closure

Status Note: _（待执行 + 独立 closure audit）_

Closure Audit Evidence:

- _（待执行后填充）_

Follow-up:

- _（待执行后填充非阻塞跟进项）_
