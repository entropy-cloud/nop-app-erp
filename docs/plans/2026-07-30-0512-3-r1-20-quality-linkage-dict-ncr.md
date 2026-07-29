# 2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr quality 业务作废联动 + dict 死状态 Deferred + NCR 无 CAPA 闭环门控实现

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.20（P1-MA2-064 + P1-MA2-065 + P1-MA2-066，源自 A2.12 quality 状态机审查）
> Related: `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-064/065/066`、`docs/design/quality/inspection-integration.md`；plan `2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift.md`、plan `2026-07-30-0341-1-r1-15-hr-state-machine-dict-dead-state.md`（同型裁决先例——**注意 R1.15 是选择性裁决：死状态 Deferred + 便宜真实缺陷实现**：039/040/041 Deferred 而 044/046 实现）
> Audit: required

## Current Baseline

三项 finding 经实仓逐项确认：均为「owner doc 声明联动/迁移但代码未实现 + dict 死状态 + CRUD 桩 + 闭环不变量缺口」类型，**不破坏已实现主路径**（质检单 PENDING→ACCEPTED/CONDITIONAL/REJECTED + NCR 5 态 + 召回 + CAPA + 强制质检门控 `isInspectionCleared` 完整覆盖主生命周期）。

**P1-MA2-064（业务单据作废联动取消质检单未落地）— 确认：**
- 全 `module-quality/erp-qa-service/src/main` grep `cancelForBusinessBill` / `cancelInspection` / `onBusinessBillCancelled` = **零匹配**；业务域（purchase/sales/mfg）cancel 路径无 `IErpQaInspectionBiz.cancel*` 调用。
- owner doc `state-machine.md §4 异常路径` 声明「业务单据作废时关联质检单自动取消」+ `§实现偏离补注` 已显式声明「业务单据作废联动取消（未落地）」。
- 残留质检单产生 TODO 噪音；不破坏主路径（CANCELLED 业务单据不再流转 + 强制质检门控不触发二次流转 + 残留经 useLogicalDelete 手工清理）。

**P1-MA2-065（QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态 + CRUD 桩）— 确认：**
- 6 处 dict 死状态合并：`ErpQaQualityGoalBizModel`（18 行）/`ErpQaReviewBizModel`（18 行）/`ErpQaCalibrationBizModel`（18 行）= CRUD 桩零 setStatus writer；`erp-qa/risk-status` MITIGATED/CLOSED 死（仅 `SpcCapabilityCalculator:308` 写 OPEN）；`erp-qa/action-status` OVERDUE 零 writer；`erp-qa/spc-calc-status` STALE 零 writer。
- 不破坏主路径（质检单/NCR/召回三大主状态机完整；CRUD 空壳实体状态字段不参与主路径迁移判定）。

**P1-MA2-066（NCR resolve 允许无 CAPA 直接关闭——闭环不变量缺口）— 确认：**
- `NcrLifecycleService.allActionsCompletedAndVerified:95-102`：`actions.isEmpty()` 时直接 `return true`（代码注释声明「无 CAPA 措施：允许 resolve，由评审人保证」），`requireResolveGate:131-136` 因此放行。
- owner doc `state-machine.md §NCR 与 CAPA 的关系`「CAPA 需效果验证才能关闭 NCR（闭环）」暗示 CAPA 是 RESOLVED 前置；inspection-integration.md §4.3「效果验证通过 → NCR 状态转为 RESOLVED」。
- 未配置合法场景门控——评审人可绕过 CAPA 闭环直接 resolve 真实不合格 NCR。仍需人工 resolve 动作 + owner doc 未显式声明「必有 CAPA」+ 误开 NCR 场景合法 → 维持 P1 非 P0。
- `resolve` mutation 位于 `ErpQaNonConformanceBizModel:85-102`（签名 `resolve(ncrId, resolution)`，调用 `requireResolveGate(ncrId, ncr.getCode())`）；ORM `ErpQaNonConformance` 现有 propId 至 32（returnCode），下一 propId=33。

**保护区域：** 不触及会计/数据删除保护区域。P1-MA2-066 方案A 涉及 ORM ask-first 加列（roadmap `§ORM 变更已授权`）+ 单方法门控 + ErrorCode，按 roadmap 规则 8 走标准 plan-audit + closure-audit（与 R1.15 P1-MA2-046 ORM ask-first 同型授权）。

## Goals

- 消除 quality 域 owner doc 与代码间三项悬空：(1) 业务作废联动取消语义对齐；(2) 6 处 dict 死状态 + CRUD 桩对齐；(3) **实现** NCR 无 CAPA resolve 闭环门控（noCapaReason 显式标注 + 否则抛 `ERR_NCR_RESOLVE_NO_CAPA`）。
- owner doc 与代码零 writer / 实际行为一致；NCR 闭环不变量经显式门控落地（非仅靠评审人保证）。

## Non-Goals

- 不实现业务作废联动取消 Facade（P1-MA2-064）——裁决 Deferred（跨域 wiring purchase/sales/mfg cancel Processor 属跨表面），successor 命名触发条件。
- 不实现 QualityGoal/RiskRegister/Calibration/Review/SPC/CAPA 状态机 BizMutation（P1-MA2-065）——裁决 Deferred（QMS 全面需求 successor），dict 死状态保留为预留。
- 不从 ORM 删除 dict 死状态值（采纳「保留为预留 + 文档 Deferred」对齐 R1.13/R1.14/R1.15 既有先例）。
- 不实现 CAPA-always 强制（即禁止任何无 CAPA resolve）——本期保留「无 CAPA + 显式 noCapaReason 允许 resolve」合法场景；successor 命名严格强制门控。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（NCR 闭环门控 + ORM ask-first 加列）
- Owner Docs: `docs/design/quality/state-machine.md`、`docs/design/quality/inspection-integration.md`
- Skill Selection Basis: P1-MA2-066 涉及 ORM 加列 + BizModel/Service 门控逻辑 + ErrorCode → `Skill: nop-backend-dev`（xbiz 动作签名 + 跨实体门控 + ORM ext/列 + ErrorCode + 产品化可定制性自检）；owner doc Deferred 标注（064/065）为纯文档 → 该部分 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 三项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：三项 finding 处置方案逐项裁决（**选择性裁决**——对齐 R1.15 先例：死状态/跨表面 Deferred + 便宜真实缺陷实现）。
      - P1-MA2-064 业务作废联动取消：**Deferred**（owner doc 正式化）。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-064 推荐「实现 cancelForBusinessBill Facade + 业务域 wiring」；本计划裁决 Deferred，理由：方案A 属跨域 wiring（purchase/sales/mfg cancel Processor config-gated 调用）跨表面实现，与危害（TODO 噪音，不破坏主路径 + owner doc §实现偏离补注已声明 Deferred + 残留经 useLogicalDelete 手工清理）不成比例。successor：业务作废自动取消质检需求时。
      - P1-MA2-065 6 处 dict 死状态 + CRUD 桩：**Deferred + dict 保留为预留**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-065 方案A 推荐「删除 dict 死状态项 + 删除常量」；本计划裁决保留 dict（不删除），理由：与 R1.13/R1.14/R1.15「保留 dict 死状态为预留语义入口」先例一致（保留优于删除——避免数据迁移 + 保留 successor 语义入口）；CRUD 空壳实体状态字段不参与主路径迁移判定。successor：计量管理/QMS 全面需求时。
      - P1-MA2-066 NCR resolve 无 CAPA 闭环门控：**实现（arm-index 推荐方向）**。理由（选择性裁决，对齐 R1.15 实现便宜真实缺陷 044/046 的范式）：(1) arm-index §P1-MA2-066 推荐实现（`noCapaReason` 列 + 空措施时抛 `ERR_NCR_RESOLVE_NO_CAPA`）；(2) 这是本批次最 containment 的实现（单方法门控 + 1 列 + 1 ErrorCode），且针对**闭环不变量**「CAPA 需效果验证才能关闭 NCR」而非死状态/TODO 噪音；(3) roadmap `§ORM 变更已授权` + R1.15 P1-MA2-046 已证 ORM ask-first 是可接受范围内修复；(4) roadmap 行 R1.20 本身标注 `[P1-MA2-066 方案A ORM ask-first]` 预期实现。残留风险：误开 NCR 无 CAPA resolve 现需显式填 noCapaReason（合法场景保留）→ 不禁止无 CAPA resolve，仅强制显式标注 + 测试迁移。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 与 arm-index 推荐偏差声明 + successor 触发条件；064/065 进 Phase 2（Deferred 标注），066 进 Phase 3（实现）。

### Phase 2 - quality owner doc Deferred 标注（P1-MA2-064 + P1-MA2-065）

Status: completed
Targets: `docs/design/quality/state-machine.md`、`docs/design/quality/inspection-integration.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md §4 异常路径 + §实现偏离补注：业务作废联动取消由「本期未接线」正式化为「**Deferred**——残留质检单经 useLogicalDelete 手工清理；`IErpQaInspectionBiz.cancelForBusinessBill` Facade + 业务域 cancel Processor wiring 留 successor」；命名 successor 触发条件。
- [x] state-machine.md 新增「CRUD 桩实体状态机（Deferred）」补注段：合并标注 QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus/CAPA-OVERDUE 各 dict 死状态为预留值（零 writer），CRUD 桩为主路径可用，完整状态机属 QMS 全面需求 successor；dict 值保留不删除；命名 successor 触发条件。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md + inspection-integration.md 明确 064/065 Deferred，owner doc 与代码零 writer 一致；successor 触发事件已命名。

### Phase 3 - NCR 无 CAPA resolve 闭环门控实现（P1-MA2-066）

Status: completed
Targets: `module-quality/model/app-erp-quality.orm.xml`、`module-quality/erp-qa-service/.../ErpQaErrors.java`、`module-quality/erp-qa-service/.../entity/NcrLifecycleService.java`、`module-quality/erp-qa-service/.../entity/ErpQaNonConformanceBizModel.java`、`IErpQaNonConformanceBiz` 接口、`docs/design/quality/state-machine.md`（§NCR 与 CAPA）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（ORM ask-first）**：`app-erp-quality.orm.xml` ErpQaNonConformance 增 `noCapaReason` 列（propId=33，`stdSqlType="VARCHAR" precision="500" stdDataType="string"`，可空，displayName「无 CAPA 原因（误开/降级 NCR 显式标注）」）；`mvn clean install -DskipTests` 增量 regen。
      - Skill: `nop-backend-dev`
- [x] **Add（ErrorCode）**：`ErpQaErrors.java` 增 `ERR_NCR_RESOLVE_NO_CAPA`（描述「NCR 无 CAPA 措施时 resolve 须提供 noCapaReason（误开/降级场景显式标注）」，i18n 中文描述 + ARG_NCR_CODE）。
      - Skill: `nop-backend-dev`
- [x] **Fix（闭环门控）**：`NcrLifecycleService.allActionsCompletedAndVerified` 改签名带 `noCapaReason`：`actions.isEmpty()` 时返回 `StringHelper.isNotBlank(noCapaReason)`（非空才放行）；`requireResolveGate(ncrId, ncrCode, noCapaReason)` 失败抛 `ERR_NCR_RESOLVE_NO_CAPA`（有措施路径行为不变：全 COMPLETED + 验证人/验证日期）。
      - Skill: `nop-backend-dev`
- [x] **Fix（resolve 动作签名）**：`ErpQaNonConformanceBizModel.resolve` + `IErpQaNonConformanceBiz.resolve` 增可选参数 `@Optional @Name("noCapaReason") String noCapaReason`；调用 `requireResolveGate(ncrId, ncr.getCode(), noCapaReason)`；放行后 `ncr.setNoCapaReason(noCapaReason)` 落库（仅当非空）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) NCR 有 CAPA 全 COMPLETED+验证 → resolve 成功（noCapaReason 可空）；(2) NCR 无 CAPA + noCapaReason 空 → resolve assertThrows `ERR_NCR_RESOLVE_NO_CAPA`；(3) NCR 无 CAPA + noCapaReason 非空 → resolve 成功 + noCapaReason 落库；迁移现有依赖「无 CAPA 直接 resolve」的测试（补 noCapaReason 入参）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §NCR 与 CAPA 的关系 更新为「无 CAPA 措施时 resolve 须显式提供 noCapaReason（误开/降级场景），否则抛 `ERR_NCR_RESOLVE_NO_CAPA`；有 CAPA 措施时必须全完成+效果验证」；inspection-integration.md §4.3 核对一致。
      - Skill: `none`

Exit Criteria:

- [x] `allActionsCompletedAndVerified` 空措施路径经 noCapaReason 门控（grep 确认不再无条件 return true）；resolve 签名带 noCapaReason；ORM 列 propId=33 落地 regen；新增/迁移测试全绿（Closure Gates 跑全量 mvn）；owner doc §NCR 与 CAPA 与代码一致。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_05030be58ffeFgem8okz3j5sGt) because P1-MA2-066 裁决为 Deferred 是过度降级——arm-index §P1-MA2-066 推荐实现（方案A noCapaReason + ERR throw），该修复是本批次最 containment（单方法+1 列+1 ErrorCode）且针对闭环不变量非死状态/TODO 噪音；roadmap `§ORM 变更已授权` + R1.15 P1-MA2-064 已证 ORM ask-first 是可接受范围内修复；R1.15 先例是选择性裁决（039/040/041 Deferred 而 044/046 实现），非全 Deferred。另三项计划统一将 defer 重标为「方案B」与 arm-index 不一致标签掩盖了 062/063/064/066 偏离 arm-index 推荐的事实。基线锚点全绿（6 finding 全部实仓验证 TRUE）。
- Independent draft review iteration 2: accept (ses_0502be6efffeIzndtBDicVNwJR) after 066 改为实现（Phase 3 ORM ask-first noCapaReason propId=33 + ErrorCode ERR_NCR_RESOLVE_NO_CAPA + allActionsCompletedAndVerified/requireResolveGate 签名带 noCapaReason + resolve/IErpQaNonConformanceBiz 签名 + owner doc + 3-case Proof）+ Closure Gates 恢复 mvn 门控 + 064/065 各补「与 arm-index 推荐偏差声明」+ 066 标注「实现 arm-index 推荐方向」。实仓验证：propId=33 加性放置正确（现 max=32 returnCode）+ 门控方法签名可改 + resolve 签名可加可选参 + ErrorCode.define 模式 + ARG_NCR_CODE 已定义。066 blocker 已解决，064/065 未被过度纠正（仍 Deferred）。非阻塞注记：现有 `TestErpQaNcrCapaEndToEnd`/`TestErpQaNcrPosting` 均在 resolve 前播种完整 CAPA 生命周期，不依赖空措施 resolve 路径——测试迁移子句或为 no-op（仅 3 个新门控测试为净新增）。

## Closure Gates

> 本计划含 ORM + 代码变更（P1-MA2-066），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为/文档完成（064/065 Deferred 标注 + 066 闭环门控实现落地）
- [x] 相关文档对齐（quality/state-machine.md + inspection-integration.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + quality 域 `mvn test` 全绿 + compliance checker 本计划零新增命中；grep 验证 066 门控 + ORM 列 regen）
- [x] 无范围内项目降级为 deferred/follow-up（066 为范围内存活实现项；064/065 Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 业务作废联动取消质检单 Facade（P1-MA2-064 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已声明 Deferred；残留质检单经 useLogicalDelete 手工清理；不破坏主路径（CANCELLED 业务单据不触发二次流转）。方案A 属跨域 wiring 跨表面实现。
- Successor Required: `yes`（业务作废自动取消质检需求时实现 `IErpQaInspectionBiz.cancelForBusinessBill(billType, billCode)` Facade [PENDING→cancelled via useLogicalDelete] + purchase/sales/mfg cancel Processor config-gated 调用）

### CRUD 桩实体完整状态机（P1-MA2-065 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: dict 死状态保留为预留语义入口；CRUD 桩为主路径可用；状态字段不参与主路径迁移判定。
- Successor Required: `yes`（计量管理/QMS 全面需求时实现 QualityGoal/RiskRegister/Calibration/Review/SPC-STALE/CAPA-OVERDUE 各 BizMutation 状态机迁移）

### NCR CAPA-always 严格强制（禁止任何无 CAPA resolve）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划已实现「无 CAPA + 显式 noCapaReason 允许 resolve」闭环门控（误开/降级合法场景保留）；CAPA-always 严格强制（禁止任何无 CAPA resolve）是更严策略，本期不采用。
- Successor Required: `yes`（业务裁决禁止任何无 CAPA NCR resolve 时，移除 noCapaReason 放行分支）

## Closure

Status Note: 三项 finding 处置完成。064（业务作废联动取消）+ 065（dict 死状态/CRUD 桩）裁决 Deferred 并在 owner doc 正式化（含 arm-index 推荐偏差声明 + successor 触发条件）；066（NCR 无 CAPA resolve 闭环门控）实现落地——ORM `noCapaReason` 列(propId=33) + `ERR_NCR_RESOLVE_NO_CAPA` ErrorCode + `allActionsCompletedAndVerified`/`requireResolveGate` 签名带 `noCapaReason`（空措施路径不再无条件 `return true`）+ `resolve`/`IErpQaNonConformanceBiz.resolve` 增 `@Optional @Name("noCapaReason")` 参 + owner doc §NCR 与 CAPA + inspection-integration §4.3 + 3-case 测试。附带修复：qa-service pom 补 `app-erp-notify-service` test 依赖（修 finance→notify bean 注入的单域测试隔离缺口，对齐 finance/sales service 同型 test 依赖）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，task ses_05007067cffeJ50Z5O1D935wqL，explore agent，未执行本计划）
- Evidence: CLOSURE_AUDIT: PASS。9/9 check 逐项经实仓 file:line 验证——(1) ORM noCapaReason propId=33；(2) 生成实体 getNoCapaReason/setNoCapaReason + PROP_ID=33；(3) ErrorCode ERR_NCR_RESOLVE_NO_CAPA + ARG_NCR_CODE；(4) allActionsCompletedAndVerified 空措施路径 isNotBlank(noCapaReason)（非无条件 return true）+ requireResolveGate 双错误码区分；(5) BizModel + IBiz resolve 签名带 @Optional @Name("noCapaReason") + setNoCapaReason 落库；(6) state-machine.md CRUD 桩 Deferred 段 + §NCR 与 CAPA noCapaReason + 业务作废联动 Deferred；(7) inspection-integration §4.3 门控注；(8) 计划 Plan Status=completed + 3 Phase 全 completed/[x]；(9) roadmap R1.20=done。无阻塞缺陷。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
