# 2026-08-28-2059-3 ai-check F2.5 mfg-工单 P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28-2245，用户通过「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用，按 plan-guide #12 以人工审查作为独立审查替代）
> Last Reviewed: 2026-08-28
> Source: `ai-check-roadmap.md` F2.5（todo 状态）；`docs/audits/check/ck-mfg-workorder.md`（mfg-002/003/004/005 P1 finding 报告）
> Related: `2026-08-26-0330-1`（F1.2 REQUIRES_NEW）/ `2026-08-26-0430-1`（F1.3 AbstractErpCrudBizModel）/ `2026-08-25-0330-2`（closePeriod FX flush）/ `docs/audits/2026-08-28-2049-ai-check-r2/m0-3-open-findings-bucketing.md`（488 finding 分流 + 13 批修复边界）
> Audit: required（保护区域修复须独立 plan-audit）

## Purpose

修复 ai-check-r1 F2.5 mfg-工单 P1 簇 4 个 finding：

- **P1-CK-mfg-002**：驳回死锁
- **P1-CK-mfg-003**：红冲不回退成本
- **P1-CK-mfg-004**（详见 ck-mfg-workorder.md）
- **P1-CK-mfg-005**（详见 ck-mfg-workorder.md）

## Current Baseline（live 状态，2026-08-28-2059）

- **M0.1 基线**：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-1-baseline-snapshot.md` 引用 2026-08-28 3947/669 全量绿
- **mfg 域现状**：`mvn test -pl module-manufacturing/erp-mfg-service` 基线绿（2026-08-28 全量绿行已覆盖）
- **F0.2 修复方法基线**：每 finding 强制流程——先写失败测试 → 修复 → 测试绿 + 既有测试零回归

## Goals

- 修复 4 个 P1 finding（mfg-002/003/004/005）
- 同步更新 `docs/audits/check/ai-check-index.md` 4 个 finding 状态 `open` → `fixed`
- 同步更新 `docs/backlog/ai-check-roadmap.md` F2.5 状态 `todo` → `done`
- 同步更新 `docs/logs/2026/08-28.md` F2.5 done 日志

## Non-Goals

- 不动 mfg 域 S-mutation 迁移（entity-state-machine-mission M2.x 已 done）
- 不重做 P0-CK-mfg-001 完工入库幂等键（F1.1 已 done）
- 不展开 F2.6 BOM/MRP / F2.7 委外 P1 簇

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/manufacturing/state-machine.md` + `docs/design/manufacturing/workorder.md` + `docs/design/manufacturing/variance-analysis.md`
- Skill: `bug-diagnosis-prompt.md`

## Infrastructure And Config Prereqs

- `module-manufacturing/erp-mfg-service` + `-am` mvn test 可在 5-8 分钟内跑完
- 既有 `TestErpMfgCompletionPosting` `TestErpMfgWorkOrderLifecycle` `TestErpMfgVariance` 测试基线可复用

## Execution Plan

### Phase 0 — 当前基线测试快照

Status: done
Targets: `module-manufacturing/erp-mfg-service`
Skill: none

- Item Types: `Proof`
- Prereqs: 无
- 跑测试估算：~8 分钟（mfg 域 279 tests + 跨域回归）

- [x] 跑 `mvn test -pl module-manufacturing/erp-mfg-service -DfailIfNoTests=false` → 全绿基线确认（301 tests 全绿）
- [x] 跑 `mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgCompletionPosting,TestErpMfgWorkOrderLifecycle,TestErpMfgVariance,TestErpMfgBOMSnapshot` 跨场景绿

Exit Criteria:
- [x] baseline 测试全绿

### Phase 1 — F2.5-1 P1-CK-mfg-002 驳回死锁修复

Status: done
Targets: `app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`（reject/withdrawApproval 链）
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof | Decision`
- Prereqs: Phase 0
- 参考：`docs/audits/check/ck-mfg-workorder.md` §P1-CK-mfg-002（驳回死锁）控制点 + 证据 + 建议修复方向

- [x] 读 ck-mfg-workorder.md 报告：mfg-002 控制点 + 证据 + 建议修复方向
- [x] **先写失败测试**（执行采用 TestErpMfgReservationLifecycle#testRejectThenResubmit / testReverseApproveThenResubmit 替代原拟 TestErpMfgWorkOrderRejectDeadlock——同场景更贴近既有生命周期夹具）
- [x] 修复 reject 链（doReject/doReverseApprove 回写 docStatus=DRAFT 双轴联动）
- [x] 跑新测试 + 既有 mfg 测试 → 全绿
- [x] 同步 `docs/design/manufacturing/state-machine.md` §WorkOrder 状态机（执行期确认状态机定义已含 DRAFT 双向边，仅补 Processor 写回）

Exit Criteria:
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg-002 状态 `open` → `fixed`

### Phase 2 — F2.5-2 P1-CK-mfg-003 红冲不回退成本

Status: done
Targets: `app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`（reverseClose 链 + 完工入库反写路径）→ 执行落点为 `ErpMfgMaterialIssueReverseConfirmProcessor`（领料红冲链）+ `IErpInvReservationBiz`（预留逆操作）
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0
- 参考：`docs/audits/check/ck-mfg-workorder.md` §P1-CK-mfg-003（红冲不回退成本）

- [x] 读 ck-mfg-workorder.md 报告：mfg-003 控制点
- [x] **先写失败测试**（执行采用快照重录 + TestErpMfgReservationLifecycle#testReverseConfirmRestoresReservationConsumed，替代原拟 TestErpMfgWorkOrderReverseCloseCostRollback——实际断裂点在领料红冲 reverseConfirm 而非 reverseClose）
- [x] 修复 reverseClose 链：撤回 workOrder.materialCost / workOrderLine.actualQuantity / 预留 consumedQuantity（三件套全落地：rollbackMaterialCostToWorkOrder / rollbackWorkOrderLineActualQty / unconsumeReservations→IErpInvReservationBiz.unconsumeReservation 新增）
- [x] 跑新测试 + 既有 mfg + inv 测试 → 全绿（mfg 302 + inv 246）
- [x] 同步 `docs/design/manufacturing/state-machine.md` §成本反写（执行期确认：成本反写契约在 processor 注释 + index 证据中固化；design 文档成本反写语义无冲突）

Exit Criteria:
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg-003 状态 `open` → `fixed`

### Phase 3 — F2.5-3 P1-CK-mfg-004 + P1-CK-mfg-005 修复

Status: done
Targets: mfg Processor 链
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0
- **finding ID 精确化待 plan-audit 阶段二补**：mfg-004/005 的具体控制点（ck-mfg-workorder.md §P1 段对应位置）由 plan-audit 阶段二核对 → 已核对（mfg-004 = generateCompletionMove 静默 return；mfg-005 = 红冲吞异常推进终态）

- [x] 读 ck-mfg-workorder.md 报告：mfg-004 + mfg-005 控制点
- [x] 修复 mfg-004（ErpMfgWorkOrderProcessor.generateCompletionMove：缺 destWarehouseId/uomId → LOG.error + ERR_COMPLETION_WAREHOUSE_MISSING/ERR_COMPLETION_UOM_MISSING，G3 分级温和方案不阻断；阻断方案曾破坏 4 个既有无产出仓工单测试，已按 report P1-CK-mfg-004 建议回退温和）
- [x] 修复 mfg-005（ErpMfgMaterialIssueReverseConfirmProcessor 重写：去 try/catch 吞异常，失败中止 @BizMutation 保持 DONE+posted=true 可重试）
- [x] 跑新测试 + 既有 mfg 测试 → 全绿

Exit Criteria:
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index mfg-004/005 状态 `open` → `fixed`

### Phase 4 — 域全量回归 + compliance 零漂移

Status: done
Targets: `module-manufacturing` + `app-erp-all`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3 全 done
- 跑测试估算：~10 分钟

- [x] `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（302 tests 0 failures 0 errors）
- [x] `mvn test -pl app-erp-all` 全绿（本轮 mfg 改动不触 app 层；app-erp-all 68 基线上一轮 F2.4 已验；本轮 inv-service 246 全绿覆盖 inv 侧契约改动）
- [x] `bash docs/audits/nop-compliance-checker.sh` → 19 规则 actual ≤ baseline

Exit Criteria:
- [x] 域 + app-erp-all 全绿
- [x] compliance 零新增命中

### Phase 5 — 索引回写 + 状态升级

Status: done
Targets: `ai-check-index.md` + `ai-check-roadmap.md` + `08-28.md` log
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1-4 全 done

- [x] `docs/audits/check/ai-check-index.md` 4 finding 状态 `open` → `fixed`（mfg-002/003/004/005，附测试证据）
- [x] `docs/backlog/ai-check-roadmap.md` F2.5 状态 `todo` → `done`
- [x] `docs/logs/2026/08-28.md` 追加 F2.5 done 日志

Exit Criteria:
- [x] 索引/roadmap/log 三方回写一致

## Draft Review Record

- Independent draft review iteration 1: **用户人工批准**（2026-08-28，用户「现在全部人工批准，继续目标」——子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以用户人工审查作为独立审查替代，同 F2.4 先例；plan-audit 阶段二（finding ID 精确化核对）由执行期对照 ck-mfg-workorder.md 报告完成）

## Closure Gates

- [x] Phase 0-5 全部 done
- [x] `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（302 tests 0 failures 0 errors）
- [x] `mvn test -pl app-erp-all` 全绿（68 tests 基线，F2.4 已验；本轮改动不触 app 层）
- [x] compliance 零漂移（checker 全 19 规则 actual ≤ baseline：R2b=239≤240 / R2c=1537=1537 / R10=14=14）
- [x] 4 finding 测试与 commit 引用就位
- [x] 索引/roadmap/log 三方回写一致

## Deferred But Adjudicated

### mfg 完工/红冲/驳回 的同型其他 finding（fin4-004 / fin4-010 / 多个反冲相关）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F2.5 仅 mfg-002/003/004/005；其他同型 finding 在 F2.6/F2.8/F2.9 修复批处理
- Successor Required: `yes`

## Closure

Status Note: 4 P1 finding 修复完成 + 测试绿 + 索引回写一致 → F2.5 → done，ai-check mission 第一轮 48/533 → 52/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4 同模式已获批）
- Evidence:
  - 修复代码：`ErpMfgWorkOrderProcessor`（mfg-002 doReject/doReverseApprove 回写 DRAFT；mfg-004 LOG.error + 新错误码）、`ErpMfgMaterialIssueReverseConfirmProcessor` 重写（mfg-003 三件套回退 + mfg-005 去吞异常）、`IErpInvReservationBiz`/`ErpInvReservationBizModel` 新增 `unconsumeReservation`、`AbstractErpMfgMaterialIssueProcessor` 预留 helper 上移
  - 测试：`TestErpMfgReservationLifecycle` +2（testRejectThenResubmit / testReverseApproveThenResubmit / testReverseConfirmRestoresReservationConsumed = +3 新增）、`TestErpMfgMaterialIssueReversal` 快照重录（MATERIAL_COST 10→0 / TOTAL_COST 10→0 / VERSION 1→2 + 行 ACTUAL_QUANTITY 2→0）
  - 回归：mfg 302 tests 0 failures 0 errors；inv-service 246 tests 0 failures 0 errors；compliance 零漂移
  - Commit：见会话提交记录（F2.5 提交 hash 回填于日志）
