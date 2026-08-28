# 2026-08-28-2315-1 ai-check F2.8 assets-生命周期 P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4-F2.7 先例）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/ai-check-roadmap.md` F2.8（todo）；`docs/audits/check/ck-assets-lifecycle.md`（ast-001..006 P1 finding）
> Related: `2026-08-28-2245-1`（F2.7）/ `docs/design/assets/split-merge.md` / `docs/design/assets/`（value-adjustment / inventory / depreciation）
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 assets-生命周期 P1 簇 6 finding（其中 ast-004 经验证为 F1.2 已修复、回填状态）：

- **P1-CK-ast-001（D6）** Split PROPORTIONAL 累计折旧被二次派生覆盖——补差失效，Σ 新卡累计折旧 ≠ 源值。
- **P1-CK-ast-002（D6/D8）** Split 目标卡残值全额复制——Σ 残值 N 倍放大。
- **P1-CK-ast-003（D5）** 盘点 reconcile 无实盘完整性校验——漏录行按实盘 0 判盘亏报废在用资产。
- **P1-CK-ast-004（D8/D2）** VA 资产净值联动仅在过账成功时执行——**已由 F1.2（P2-CK-ast-015 审查 R1）修复**（applyAssetValueChange 无条件前移 tryPost 前），本批验证 + 回填。
- **P1-CK-ast-005（D5/D6）** VA 减值金额无上限——NBV 可为负。
- **P1-CK-ast-006（D6）** Split/Merge 新卡折旧口径三重错位——Merge 残值归零+全年限加权 + 两链计划起点覆盖历史 + 计划基数不减已提。

## Execution（全部完成，见 Closure Evidence）

- ast-001：`ErpAstSplitProcessor.computeAllocation` 二次派生循环加 `if (fixedMode)` 包裹。
- ast-002：`createTargetAssets` 残值经 `allocatedResidual`（比例/金额比）分摊。
- ast-003：`ErpAstInventoryProcessor.calculateVariance` 前置 actualQuantity==null 完整性校验（新错误码 `ERR_AST_INVENTORY_ACTUAL_QUANTITY_MISSING`）。
- ast-004：验证 F1.2 代码（无条件前移 + 对称回滚）与既有测试覆盖 → 回填 fixed。
- ast-005：`validateForApproval` 上限（非 UP 类型金额 ≤ NBV−残值，`ERR_ADJUSTMENT_AMOUNT_EXCEEDS_NBV`）+ `applyAssetValueChange` 下调分支下限 max(残值,0)。
- ast-006：Merge 残值=Σ源残值 + `resolveUsefulLifeMonths` 剩余期间加权 + 计划基数=剩余可折旧净值；Split 计划基数=(orig−accumDep)−残值；两链计划起点=继承点次月。

## Tests

- `TestErpAstSplitMerge#testProportionalSplitConservesAccumDepAndResidual`（ast-001+002）
- `TestErpAstSplitMerge#testMergeConservesResidualAndRemainingLife`（ast-006）
- `TestErpAstInventory#testReconcileRejectsMissingActualQuantity`（ast-003）
- `TestErpAstValueAdjustment#testImpairmentAmountExceedsNbvRejected`（ast-005）
- 快照重录：split/merge 折旧计划（Merge 60→48 行、计划总额 100000→62400）

## Validation

- `mvn test -pl module-assets/erp-ast-service` 334/0/0 全绿
- `bash docs/audits/nop-compliance-checker.sh` 零漂移（R2b=239≤240 / R2c=1537 / R10=14）
- ai-check 61/533 → 67/533 fixed

## Deferred But Adjudicated

### ast-007..013 / 折旧 ast2 簇

- Classification: `out-of-scope improvement`——P2/P3 在 F2.9/F3.x 批次处理
- Successor Required: `yes`

## Closure

Status Note: 6 P1 finding 处理完成（5 修复 + 1 验证回填）→ F2.8 → done，ai-check 61/533 → 67/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4-F2.7 同模式已获批）
- Evidence: 见 Execution/Tests/Validation 三节（4 新测试红→绿 + 快照重录 + ast 334 全绿 + compliance 零漂移）；ast-004 回填证据 = `ErpAstValueAdjustmentProcessor` F1.2 注释 + `TestErpAstValueAdjustment` L204 配对鉴别用例
- Commit：F2.8 提交 hash 回填于日志
