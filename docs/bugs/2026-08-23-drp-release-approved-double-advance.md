# 2026-08-23 drp releaseApproved 双段推进计划状态触发非法迁移（post-08-12 守卫硬化暴露的潜伏缺陷）

## 现象

`drp-release-approved.action.spec.ts` happy path（APPROVED plan + TRANSFER/PURCHASE 混合行 → 批量释放）在多行计划上确定性失败：

```
ErpDrpLine__releaseApproved → 非法状态转换：当前=EXECUTED，期望=APPROVED
```

单行计划不受影响；仅「计划下 ≥2 行全部释放后计划翻 EXECUTED」的批量场景触发。

## 根因

`DrpReleaseService`（module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpReleaseService.java）：

1. `releaseApproved(planId)` 循环逐行调 `releaseLine(lineId)`；
2. 每个 `releaseLine` 尾部**已**调 `advancePlanToExecutedIfComplete(line.getPlanId())`（:96）——最后一行释放后计划即 APPROVED→EXECUTED；
3. 循环结束后 `releaseApproved` **再次**调 `advancePlanToExecutedIfComplete(planId)`（:117）——计划已 EXECUTED，仍通过「全行终态」门控，进入 `planStateMachine.assertCanAdvanceToExecuted(EXECUTED)`。

`assertCanAdvanceToExecuted` 为 plan 2026-08-12-1841-1 Phase 2「防御性不变量加强」新增（fb5e7d5c3 同族，落地 2026-08-01/08-12）：plan 非 APPROVED 时 Bean 抛 common 层非法迁移码。加固前重复 setStatus(EXECUTED) 幂等无害；加固后双段推进第二段必抛。

## 影响

- 批量释放（UC-DRP-03 多行计划）在当前产品下不可用（第一段已全部落库，仅计划状态翻转报错——副作用行/下游单据已生成，报错发生在事务内应整体回滚，实际语义待核）。
- E2E `drp-release-approved` 1 用例红（2026-08-23 flux 全量回归发现；该 spec 上次全量绿基线 2026-08-11 系对旧产品态的度量）。

## 修复建议（successor）

`advancePlanToExecutedIfComplete` 增加幂等短路：计划已 EXECUTED（或终态）直接 return；或删除 `releaseApproved` 尾部的冗余二次调用（releaseLine 内已逐行推进）。任一均为产品代码变更，按 plan 2026-08-23-0434-2 Non-Goal「不改生产代码」不在该计划内修复。

## 复现

fresh-DB + enforcement 栈 E2E 服务器：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/drp-release-approved.action.spec.ts --workers=1`（确定性红，错误码 nop.err.erp.common.illegal-status-transition）。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = backlog 择期产品修复）
