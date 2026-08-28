# 2026-08-28-2340-1 ai-check F2.9 assets-折旧 P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4-F2.8 先例）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/ai-check-roadmap.md` F2.9（todo）；`docs/audits/check/ck-assets-depreciation.md`（ast2-001..006 P1 finding）
> Related: `2026-08-28-2315-1`（F2.8）/ `docs/design/assets/depreciation-and-posting.md` / `docs/design/assets/value-adjustment`（VA）
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 assets-折旧 P1 簇 6 finding：

- **P1-CK-ast2-001（D6）** 工作量法（UNITS）折旧恒 0——静默零掩盖漏提 → 显式业务错误 + Deferred 注记。
- **P1-CK-ast2-002（D6）** 资本化维修折旧基数增量双计 → recalc 按当前卡片状态重算（去 `.add(increment)`）。
- **P1-CK-ast2-003（D6/D4）** 批量折旧缺「当月增加下月提」守卫 → period 必须晚于资本化月份。
- **P1-CK-ast2-004（D8）** 逆资本化红冲回退断裂 → 已执行折旧存在时拒绝 + cancelSchedules 仅 PENDING。
- **P1-CK-ast2-005（D8/D2）** assets 零 ReversalListener → 新增 ErpAstDepreciationReversalListener。
- **P1-CK-ast2-006（D8）** 价值调整后三方失同步 → REVALUATION_UP 同步 originalValue + 处置净值读实际 NBV（1604 结转腿 Deferred 保护区域）。

## Execution（全部完成，见 Closure Evidence）

- ast2-001：`executeDepreciation`/`catchUpDepreciation` 对 UNITS 抛 `ERR_DEPRECIATION_UNITS_NOT_CONFIGURED`。
- ast2-002：`recalculateForCapitalizationMaintenance` 基数/计划行 NBV 去掉 `.add(increment)`。
- ast2-003：`executeDepreciation` 前置 `ERR_DEPRECIATION_PERIOD_BEFORE_ACQUISITION` 守卫（period 晚于获取月）。
- ast2-004：`executeReverseApprove` 前置 `ERR_CAPITALIZATION_HAS_EXECUTED_DEPRECIATION` + `cancelSchedules` PENDING-only。
- ast2-005：`ErpAstDepreciationReversalListener`（DEPRECIATION billHeadCode=资产码#期间 → 计划行 posted=false/voucherId=null/REVERSED + 资产累计/净值回退；REVERSED 标记防域内双重回退；bean 注册；经 I*Biz 零新增 daoFor）。
- ast2-006：REVALUATION_UP 同步 originalValue（rollback 对称）+ 处置净值读实际 NBV（Dispatcher billData + Provider raw-key 回退）。

## Tests

- `TestErpAstDepreciation#testUnitsMethodRejectedNotSilentZero`（ast2-001）
- `TestErpAstDepreciation#testPeriodBeforeAcquisitionMonthRejected`（ast2-003）
- `TestErpAstMaintenance#testCapitalizePathWithDepreciationRecalc` 增计划总额断言（ast2-002）
- `TestErpAstCapitalization#testReverseApproveRejectedAfterDepreciationExecuted`（ast2-004）
- 快照重录：maintenance recalc 计划金额（145000→120000 口径）、VA revaluation-up 资产原值、幂等重执行资产 VERSION+1

## Validation

- `mvn test -pl module-assets/erp-ast-service` 337/0/0 全绿
- `bash docs/audits/nop-compliance-checker.sh` 零漂移（R2b=239≤240 / R2c=1537 / R10=14——监听器/countExecuted 经 I*Biz 重构消除初版 +5 daoFor 漂移）
- ai-check 67/533 → 73/533 fixed

## Deferred But Adjudicated

### ast2-006 减值准备（1604）处置结转腿 / ast2-007..014 P2/P3

- Classification: `out-of-scope improvement`（1604 结转涉及会计科目口径 = 保护区域，独立计划裁决；P2/P3 在 F3.x）
- Successor Required: `yes`

## Closure

Status Note: 6 P1 finding 修复完成 → F2.9 → done，ai-check 67/533 → 73/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4-F2.8 同模式已获批）
- Evidence: 见 Execution/Tests/Validation 三节（4 新测试/断言红→绿 + 快照重录 + ast 337 全绿 + compliance 零漂移 + 监听器域内双重回退防护实证）
- Commit：F2.9 提交 hash 回填于日志
