# 2026-08-29-0000-1 ai-check F2.11 inventory P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4-F2.10 先例）
> Last Reviewed: 2026-08-29
> Source: `docs/backlog/ai-check-roadmap.md` F2.11（todo）；`docs/audits/check/ck-inventory.md`（inv-001..004 P1 finding）
> Related: `2026-08-28-2355-1`（F2.10）/ `docs/design/inventory/state-machine.md` / `docs/design/inventory/README.md`
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 inventory P1 簇（inv-003 已由 F1.3 修复、验证回填）：

- **P1-CK-inv-001（D6/D8）** 4 个出库策略 locationId 回退误用 warehouseId → 仓库 ID 写入库位列，余额维度污染。
- **P1-CK-inv-002（D6/D8）** upsertBalance/findBalance 查询键与 UK 自然键不一致 → 余额行错配写入。
- **P1-CK-inv-003（D3/D5）** 流水/余额不可变 CRUD 强制——已由 F1.3（AbstractErpImmutableCrudBizModel + TestErpInvLedgerImmutable）修复，本批验证回填。
- **P1-CK-inv-004（D3/D5）** 批次/序列号缺失拒绝确认未实现——批次管控无批号出库被跳过放行。

## Execution（全部完成）

- inv-001：WeightedAverage/Batch/Lifo/Specific 四策略 onOutgoing locationId 回退改 `move.getSourceLocationId()`。
- inv-002：StockMoveBookkeeper.findBalance 统一委托 findBalanceByNaturalKey（skuId + nullable IS NULL，owner 门控保留）；ErpInvReservationBizModel.findBalance 同步；CostAdjustmentService 补 batchNo IS NULL（skuId/locationId 维度行无此字段，残留 successor）。
- inv-004：validateBatchSerialPresence 前置守卫（ERR_BATCH_REQUIRED / ERR_BATCH_NOT_FOUND / ERR_SERIAL_REQUIRED）；「序列号未售/在库状态」翻转 writer 缺失为独立特性 Deferred。

## Tests

- `TestErpInvBatchExpiryInterception#testBatchManagedMaterialRequiresBatchNoOnOutgoing`（inv-004）
- `TestErpInvBatchExpiryInterception#testBatchNotInWarehouseRejectedOnOutgoing`（inv-004）
- inv-001/002 由既有 inv 246 + mfg 308 跨域回归覆盖（无快照漂移）

## Validation

- `mvn test -pl module-inventory/erp-inv-service` 246/0/0 全绿
- `mvn test -pl module-manufacturing/erp-mfg-service` 308/0/0 全绿（costing 策略跨域回归）
- compliance 零漂移（R2c=1536 ≤ 1537）
- ai-check 77/533 → 80/533 fixed

## Deferred But Adjudicated

### CostAdjustmentService skuId/locationId 维度 / 序列号状态翻转 writer

- Classification: `out-of-scope improvement`（成本调整行无 skuId/locationId 字段；序列号 IN_STOCK→OUT writer 为独立特性）
- Successor Required: `yes`

## Closure

Status Note: 4 P1 finding 处理（3 修复 + 1 验证回填）→ F2.11 → done，ai-check 77/533 → 80/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4-F2.10 同模式已获批）
- Evidence: 见 Execution/Tests/Validation 三节（2 新测试红→绿 + inv 246/mfg 308 全绿 + compliance 零漂移）
- Commit：F2.11 提交 hash 回填于日志
