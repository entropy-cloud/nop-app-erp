# 2026-08-28-2230-1 ai-check F2.6 mfg-BOM/MRP P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4/F2.5 先例）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/ai-check-roadmap.md` F2.6（todo）；`docs/audits/check/ck-mfg-bom-mrp.md`（mfg2-001/002/003）+ `docs/audits/check/ck-drp.md`（P2-CK-drp-012 同型）
> Related: `2026-08-28-2059-3`（F2.5 mfg-工单，同域前批）/ `docs/design/manufacturing/mrp.md`（低层码声明 L92）/ `docs/design/manufacturing/simulation-engine.md`
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 mfg-BOM/MRP P1 簇 3 finding + drp 同型 1 finding：

- **P1-CK-mfg2-001（D6）SAFETY_STOCK 需求可用量双扣**：`DemandAggregator.collectSafetyStockDemands` 产出 net 短缺口（safety − available）作为需求行 quantity，`MrpEngine.processMaterial` 再扣一次 available——安全库存补货系统性低估（available ≥ safety/2 时净需求恒 0 永不补货）。fork `SimulationMrpEngine.applySafetyStockOverride` 同缺陷。
- **P1-CK-mfg2-002（D6）MRP 无低阶码净额归集**：共享子件 / 独立需求+子件同物料多次出现时每次独立扣减全部可用量，净需求系统性低估。owner doc `mrp.md` L92「低层码经 BomExpander DFS 层级标记实现」声明与实现不符（`BomExplosionNode.level` 零消费）。
- **P1-CK-mfg2-003（D6）SimulationMrpEngine.nextVersionNo ASC 取最小**：同场景第 3 次仿真 versionNo 与既有 v2 撞 UK（`UK_MFG_MRP_SCENARIO_VERSION_SCN_VER`），「粗调/细调/最终」多版本迭代第 3 次起必炸。
- **P2-CK-drp-012（D6，同型）**：`SimulationDrpEngine.nextVersionNo` 同代码模式 + 同 UK 形态（`UK_DRP_SCENARIO_VERSION_SCN_VER`），修复同批。

## Current Baseline（live 状态，2026-08-28-2230）

- mfg 域 302 tests 全绿（F2.5 后）；drp 域测试基线待确认（F2.6 Phase 0 快照）。
- ai-check 终态：52/533 fixed（F2.5 后）。

## Goals

1. 两个 MRP 引擎（`MrpEngine` + fork `SimulationMrpEngine`）净需求口径修正：SAFETY_STOCK net 需求不再二次扣 available；共享子件可用量每次 run 仅扣一次。
2. `nextVersionNo` 两个引擎（MRP + DRP）改 DESC 取 max，第 3+ 版本运行不再撞 UK。
3. 红→绿回归测试 + 既有测试零回归 + compliance 零漂移。
4. owner doc 同步：`mrp.md` L92 低层码声明修正为真实机制。

## Non-Goals

- mfg2-004 起 P2/P3 finding（F3.x 批次）。
- 物化 lowLevelCode 列 / 真正的低阶码拓扑排序（选项 a 全树收集再逐物料一次净额）——采用 finding 建议方向 (b)「run 内 per-material 已消耗可用量累计」，保留 pegging 行结构与父行引用。
- CRP / AUTO_SCHEDULED / 需求时界。

## Task Route

- `docs/design/manufacturing/mrp.md`（低层码/可用量口径声明）
- `docs/design/manufacturing/simulation-engine.md`（仿真参数变体）
- owner docs 更新走 `docs/process/application-development-workflow.md`。

## Infrastructure And Config Prereqs

- `mvn test -pl module-manufacturing/erp-mfg-service` ~6-8 分钟；`mvn test -pl module-drp/erp-drp-service` ~3-5 分钟。
- 既有测试：`TestErpMfgMrpEngine`（10 tests）、`TestErpMfgMrpSimulation`（9 tests）、`TestErpDrpSimulation`（5 tests）。

## Execution Plan

### Phase 0 — 当前基线测试快照

Status: done
Targets: `module-manufacturing/erp-mfg-service` + `module-drp/erp-drp-service`
Skill: none

- Item Types: `Proof`
- Prereqs: 无

- [x] 跑 `mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgMrpEngine,TestErpMfgMrpSimulation` → 基线绿
- [x] 跑 `mvn test -pl module-drp/erp-drp-service` → 基线绿

Exit Criteria:
- [x] 基线测试全绿

### Phase 1 — F2.6-1 P1-CK-mfg2-001 安全库存双扣

Status: done
Targets: `MrpEngine.java` + `SimulationMrpEngine.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-bom-mrp.md §P1-CK-mfg2-001 控制点（已读：DemandAggregator L132-143 net 短缺口 + MrpEngine L116-121 二次扣）
- [x] **先写失败测试**：`TestErpMfgMrpEngine#testSafetyStockNetNotDoubleDeducted`——safety=100/avail=99 → net 应为 1（修复前 0）；safety=100/avail=10 → net 应为 90（修复前 80）
- [x] 修复 `MrpEngine`：top 需求拆分 SAFETY_STOCK（net、skipAvailable）与非 SAFETY_STOCK（gross、正常扣减）；`processMaterial` 增 `skipAvailable` 参数
- [x] 修复 `SimulationMrpEngine`：同拆分（runSimulation top 循环 + `applySafetyStockOverride` 产出的 net 行）
- [x] 跑新测试 + 既有 mfg 测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg2-001 `open` → `fixed`

### Phase 2 — F2.6-2 P1-CK-mfg2-002 低阶码净额归集

Status: done
Targets: `MrpEngine.java` + `SimulationMrpEngine.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-bom-mrp.md §P1-CK-mfg2-002 控制点（已读：L116 每次递归全扣 + L149-159 跨分支无累计 + topDemandsByMaterial 仅顶层合并）
- [x] **先写失败测试**：`TestErpMfgMrpEngine#testSharedComponentAvailableConsumedOnce`——A(100)+B(100) 共享 C（1:1）、available(C)=50 → ΣC planned = 150（修复前 100）
- [x] 修复 `MrpEngine`：`processMaterial` 线程化 run 内 `Map<materialId, 已消耗可用量>`——remaining = max(0, total − consumed)、net = gross − remaining、consumed += min(gross, remaining)；SAFETY_STOCK 顶层访问不消耗
- [x] 修复 `SimulationMrpEngine`：同 fork
- [x] 同步 `docs/design/manufacturing/mrp.md` L92 低层码声明（修正为 run 内 consumed-available 累计机制）
- [x] 跑新测试 + 既有 mfg 测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg2-002 `open` → `fixed`

### Phase 3 — F2.6-3 P1-CK-mfg2-003 + P2-CK-drp-012 versionNo

Status: done
Targets: `SimulationMrpEngine.java` + `SimulationDrpEngine.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0

- [x] 读 ck-mfg-bom-mrp.md §P1-CK-mfg2-003 + ck-drp.md §P2-CK-drp-012（已读：`addOrderField("versionNo", false)`=ASC 取 min）
- [x] **先写失败测试**：`TestErpMfgMrpSimulation#testThirdVersionRunSucceeds`（v1→v2→v3 连续三次 runSimulation，第 3 次 versionNo=3 不撞 UK）；`TestErpDrpSimulation` 同型（drp 第 3 次）
- [x] 修复 `SimulationMrpEngine.nextVersionNo`：`addOrderField("versionNo", true)`=DESC 取 max
- [x] 修复 `SimulationDrpEngine.nextVersionNo`：同
- [x] 跑新测试 + 既有 mfg/drp 测试 → 全绿

Exit Criteria:
- [x] 新测试红→绿 + 既有测试零回归
- [x] ai-check-index P1-CK-mfg2-003 + P2-CK-drp-012 `open` → `fixed`

### Phase 4 — 域全量回归 + compliance 零漂移

Status: done
Targets: `module-manufacturing` + `module-drp`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3 全 done

- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿
- [x] `mvn test -pl module-drp/erp-drp-service` 全绿
- [x] `bash docs/audits/nop-compliance-checker.sh` → 全 19 规则 actual ≤ baseline

Exit Criteria:
- [x] 两域全绿 + compliance 零新增命中

### Phase 5 — 索引回写 + 状态升级

Status: done
Targets: `ai-check-index.md` + `ai-check-roadmap.md` + `08-28.md` log
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1-4 全 done

- [x] `docs/audits/check/ai-check-index.md` 3+1 finding 状态 `open` → `fixed`
- [x] `docs/backlog/ai-check-roadmap.md` F2.6 状态 `todo` → `done`
- [x] `docs/logs/2026/08-28.md` 追加 F2.6 done 日志

Exit Criteria:
- [x] 索引/roadmap/log 三方回写一致

## Draft Review Record

- Independent draft review iteration 1: **用户人工批准**（2026-08-28「现在全部人工批准，继续目标」——子 agent 通道不可用，按 plan-guide #12 替代；同 F2.4/F2.5 先例）

## Closure Gates

- [x] Phase 0-5 全部 done
- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿 + `mvn test -pl module-drp/erp-drp-service` 全绿
- [x] compliance 零漂移
- [x] 4 finding 测试与 commit 引用就位
- [x] 索引/roadmap/log 三方回写一致

## Deferred But Adjudicated

### mfg2-004..010 / drp 其他同型

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F2.6 仅 mfg2-001/002/003 + drp-012；其余 P2 在 F3.x 批次处理
- Successor Required: `yes`

## Closure

Status Note: 3+1 finding 修复完成 + 测试绿 + 索引回写一致 → F2.6 → done，ai-check 52/533 → 56/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4/F2.5 同模式已获批）
- Evidence:
  - 修复代码：`MrpEngine`/`SimulationMrpEngine`（mfg2-001 SAFETY_STOCK 拆分 skipAvailable + mfg2-002 run 内 availableConsumed 累计 + mfg2-003 nextVersionNo DESC）、`SimulationDrpEngine`（drp-012 同型 DESC）
  - 测试：`TestErpMfgMrpEngine` +2（testSafetyStockNetNotDoubleDeducted / testSharedComponentAvailableConsumedOnce）、`TestErpMfgMrpSimulation` +1（testThirdVersionRunSucceeds）、`TestErpDrpSimulation` +1（testThirdVersionRunSucceeds）；快照重录 erp_mfg_mrp_plan_line.csv（M2 net/planned 2→5）
  - 回归：mfg 305 tests 0 failures 0 errors（+3）；drp 98 tests 0 failures 0 errors（+1）；compliance 零漂移（R2b=239≤240 / R2c=1537 / R10=14）
  - owner doc：`docs/design/manufacturing/mrp.md` L92 低层码声明修正（run 内 consumed-available 累计机制）
  - Commit：F2.6 提交 hash 回填于日志
