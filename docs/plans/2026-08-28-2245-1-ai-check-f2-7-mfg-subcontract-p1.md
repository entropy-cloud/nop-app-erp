# 2026-08-28-2245-1 ai-check F2.7 mfg-委外 P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4-F2.6 先例）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/ai-check-roadmap.md` F2.7（todo）；`docs/audits/check/ck-mfg-subcontract.md`（mfg3-001..005 P1 finding）
> Related: `2026-08-28-2230-1`（F2.6）/ `2026-08-28-2059-3`（F2.5，mfg-005 去吞异常先例）/ `docs/design/manufacturing/subcontracting.md` / `docs/design/manufacturing/batch-genealogy.md` / `docs/design/manufacturing/variance-analysis.md`
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 mfg-委外 P1 簇 5 finding：

- **P1-CK-mfg3-001（D6/D8）委外成品入库计价仅含加工费**：`computeReceiptUnitCost` 分子=头 processingFee，发料材料成本不参与——产成品存货低估 + 1408 委外物资科目永久残留材料成本净额。
- **P1-CK-mfg3-002（D8/D6）reverseCompletion 永不反向委外收货移动单**：`generateReceiptMove` 不设 sourceWarehouseId → `canSafelyReverse` 恒拒 → 红冲后材料退回但成品滞留（库存双计）。
- **P1-CK-mfg3-003（D3/D5）审批族 Pattern B 绕过 validateNotCancelled**：CANCELLED 委外单可被 submit/approve 复活并重新进入发料链。
- **P1-CK-mfg3-004（D6/D10）基因链输入批次按产成品仓解析**：`resolveInputLot` 用产出行 destWarehouseId 查原料批次——多仓布局下恒空，追溯/召回静默失效。
- **P1-CK-mfg3-005（D6/D4）SUBCONTRACT 差异实际侧 wo.subcontractCost 零 writer**：实际委外费恒 0 → 差异行失真 + 错误方向凭证。

## Current Baseline（live 状态，2026-08-28-2245）

- mfg 305 tests 全绿（F2.6 后）；ai-check 56/533 fixed。

## Goals

1. 委外成品计价含材料成本（发料移动单流水聚合 + 加工费）。
2. 委外红冲闭环：收货移动单可反向（inv `inverseMoveType(MANUFACTURE)→OUTGOING` + mfg `canSafelyReverse` 补 MANUFACTURE 分支）。
3. CANCELLED 终态不可复活（submit/approve/reject 三校验补 docStatus 守卫）。
4. 基因链输入批次按领料单头仓库解析（多仓布局恢复追溯）。
5. wo.subcontractCost 完工归集 writer（config-gated，镜像 aggregateSubcontractCost 口径）。
6. 红→绿回归测试 + 既有测试零回归 + compliance 零漂移 + owner doc 同步。

## Non-Goals

- mfg3-006..012 P2/P3（F3.x 批次）：红冲吞异常告警（006）、入参边界（007）、基因链去重丢量（008）、差异重算链复合悬挂（009）、findFirmedRollupLine orgId（010）、切片裸 CRUD（011）、reverseApprove 无 docStatus 守卫（012）。
- 委外单↔工单显式关联（模型无此链，mfg3-005 采用「按产品聚合 + 产量分摊」口径，同 aggregateSubcontractCost）。

## Task Route

- `docs/design/manufacturing/subcontracting.md` / `batch-genealogy.md` / `variance-analysis.md`。
- inv 侧 `inverseMoveType` 改动为跨域契约（mfg 单侧消费方），按跨域变更纪律联合裁决。

## Infrastructure And Config Prereqs

- `mvn test -pl module-manufacturing/erp-mfg-service` ~6-8 分钟。
- 既有测试：`TestErpMfgSubcontracting`（6 tests，snapshot）、`TestErpMfgSubcontractReverse`（4 tests，snapshot）、`TestErpMfgBatchGenealogy`（8 tests）、`TestErpMfgVariance`。

## Execution Plan

### Phase 0 — 当前基线测试快照

Status: done
Targets: `module-manufacturing/erp-mfg-service`
Skill: none

- Item Types: `Proof`
- Prereqs: 无

- [x] 跑 `mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgSubcontracting,TestErpMfgSubcontractReverse,TestErpMfgBatchGenealogy,TestErpMfgVariance` → 基线绿

Exit Criteria:
- [x] 基线测试全绿

### Phase 1 — F2.7-1 P1-CK-mfg3-001 计价含材料成本

Status: done
Targets: `ErpMfgSubcontractOrderProcessor.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-subcontract.md §P1-CK-mfg3-001 控制点（已读：computeReceiptUnitCost L395-402 仅加工费）
- [x] **先写失败测试**：TestErpMfgSubcontracting.testFullLifecycleWithPosting 增断言——发料 M1 2@5=10 + fee 50、收货 1 → receipt 移动单行 unitCost=60（修复前 50）
- [x] 修复：`ErpMfgSubcontractOrderProcessor` 注入 `IErpInvStockLedgerBiz`，`computeReceiptUnitCost` 分子 = 发料移动单流水 |totalCost| 合计 + 头 processingFee
- [x] 跑新断言 + 既有委外测试 → 全绿（快照重录：receipt 移动单/流水/SR 凭证金额）

Exit Criteria:
- [x] 新断言绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg3-001 `open` → `fixed`

### Phase 2 — F2.7-2 P1-CK-mfg3-002 红冲反向收货移动单

Status: done
Targets: `ErpInvStockMoveProcessor.inverseMoveType`（inv 跨域）+ `ErpMfgSubcontractOrderProcessor.canSafelyReverse`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-subcontract.md §P1-CK-mfg3-002 控制点（已读：canSafelyReverse L219-225 恒拒 MANUFACTURE）
- [x] **先写失败测试**：TestErpMfgSubcontractReverse.testReverseCompletionRollsBackPostedAndStatus 增断言——红冲后产成品 P 余额归 0 + 原料 M1 余额恢复 10（修复前 P 滞留 1）
- [x] 修复 inv `inverseMoveType`：MANUFACTURE → OUTGOING（入库类反向语义）；修复 mfg `canSafelyReverse`：MANUFACTURE 同 INCOMING 要求 destWarehouseId 非空
- [x] 跑新断言 + 既有委外/库存测试 → 全绿（快照重录：红冲后新增 REVERSAL OUTGOING 收货反向移动单）

Exit Criteria:
- [x] 新断言绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg3-002 `open` → `fixed`

### Phase 3 — F2.7-3 P1-CK-mfg3-003 CANCELLED 复活守卫

Status: done
Targets: `ErpMfgSubcontractOrderProcessor.validateTransitionFor{Submit,Approve,Reject}`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-subcontract.md §P1-CK-mfg3-003 控制点（已读：Pattern B override 绕过骨架 validateNotCancelled）
- [x] **先写失败测试**：TestErpMfgSubcontracting 新增 testCancelledOrderCannotRevive——cancel 后 approve/submit 拒绝（错误码断言）
- [x] 修复：三校验前置 docStatus==CANCELLED 拒绝（复用 illegalTransition）
- [x] 跑新测试 + 既有委外测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg3-003 `open` → `fixed`

### Phase 4 — F2.7-4 P1-CK-mfg3-004 基因链输入批次仓库

Status: done
Targets: `BatchGenealogyWriter.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-subcontract.md §P1-CK-mfg3-004 控制点（已读：doWrite L129 destWarehouse + resolveInputLot L213-219）
- [x] **先写失败测试**：TestErpMfgBatchGenealogy 新增 testMultiWarehouseInputLotResolved——输入批次在原料仓 3601、产出行 destWarehouse=3602 → 基因链行应写入（修复前空）
- [x] 修复：`findIssueLinesWithBatch` 携带领料单头 warehouseId，`doWrite` 传 issue 仓给 `resolveInputLot`
- [x] 跑新测试 + 既有基因链测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg3-004 `open` → `fixed`

### Phase 5 — F2.7-5 P1-CK-mfg3-005 wo.subcontractCost 归集 writer

Status: done
Targets: `ErpMfgWorkOrderProcessor.java` + `ErpMfgWorkOrderReportCompletionProcessor.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-subcontract.md §P1-CK-mfg3-005 控制点（已读：ProductionVarianceCalculator L196-206 读零 writer 字段）
- [x] **先写失败测试**：TestErpMfgVariance 或新测试——开 CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED + COMPLETED 委外单同产品 → 完工后 wo.subcontractCost 非零（修复前恒 0）
- [x] 修复：`ErpMfgWorkOrderProcessor.applySubcontractCostToWorkOrder`（config-gated，按产品聚合 COMPLETED 委外单加工费 × 完工产量/委外总量分摊），reportCompletion 中调用
- [x] 跑新测试 + 既有完工/差异测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg3-005 `open` → `fixed`

### Phase 6 — 域全量回归 + compliance 零漂移

Status: done
Targets: `module-manufacturing` + `module-inventory`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-5 全 done

- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿
- [x] `mvn test -pl module-inventory/erp-inv-service` 全绿（inverseMoveType 跨域改动）
- [x] `bash docs/audits/nop-compliance-checker.sh` → 全 19 规则 actual ≤ baseline

Exit Criteria:
- [x] 两域全绿 + compliance 零新增命中

### Phase 7 — 索引回写 + 状态升级

Status: done
Targets: `ai-check-index.md` + `ai-check-roadmap.md` + `08-28.md` log
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1-6 全 done

- [x] `docs/audits/check/ai-check-index.md` 5 finding 状态 `open` → `fixed`
- [x] `docs/backlog/ai-check-roadmap.md` F2.7 状态 `todo` → `done`
- [x] `docs/logs/2026/08-28.md` 追加 F2.7 done 日志

Exit Criteria:
- [x] 索引/roadmap/log 三方回写一致

## Draft Review Record

- Independent draft review iteration 1: **用户人工批准**（2026-08-28「现在全部人工批准，继续目标」——子 agent 通道不可用，按 plan-guide #12 替代；同 F2.4-F2.6 先例）

## Closure Gates

- [x] Phase 0-7 全部 done
- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿 + `mvn test -pl module-inventory/erp-inv-service` 全绿
- [x] compliance 零漂移
- [x] 5 finding 测试与 commit 引用就位
- [x] 索引/roadmap/log 三方回写一致

## Deferred But Adjudicated

### mfg3-006..012 / 委外同型 P2/P3

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F2.7 仅 mfg3-001..005；P2/P3 在 F3.x 批次处理
- Successor Required: `yes`

## Closure

Status Note: 5 P1 finding 修复完成 + 测试绿 + 索引回写一致 → F2.7 → done，ai-check 56/533 → 61/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4-F2.6 同模式已获批）
- Evidence:
  - 修复代码：`ErpMfgSubcontractOrderProcessor`（mfg3-001 computeReceiptUnitCost 含材料成本 + mfg3-002 canSafelyReverse MANUFACTURE 分支 + mfg3-003 assertNotCancelled 三校验）、`ErpInvStockMoveProcessor.inverseMoveType`（mfg3-002 跨域）、`BatchGenealogyWriter`（mfg3-004 issue 仓解析）、`ErpMfgWorkOrderProcessor.applySubcontractCostToWorkOrder`（mfg3-005 完工归集 writer）
  - 测试：TestErpMfgSubcontracting +1（testCancelledOrderCannotRevive）+ mfg3-001 unitCost=60 断言；TestErpMfgSubcontractReverse +mfg3-002 余额断言（P 归 0/M1 恢复 10）；TestErpMfgBatchGenealogy +1（testMultiWarehouseInputLotResolved）；TestErpMfgProductionVariance +1（testSubcontractCostAggregatedOnCompletion wo.subcontractCost=20）
  - 回归：mfg 311 tests 0 failures（+6 新测试）+ inv 246 tests 0 failures（inverseMoveType 跨域改动）；compliance 零漂移
  - 快照重录：receipt 移动单 unitCost 50→60、SR 凭证金额 50→60、红冲新增 REVERSAL 收货反向移动单（BODY/PAYLOAD 通配符 `*` 已保留防 seq 抖动）
  - Commit：F2.7 提交 hash 回填于日志
