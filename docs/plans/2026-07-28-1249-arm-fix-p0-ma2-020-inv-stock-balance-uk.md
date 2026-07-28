# 2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk P0 fix：库存余额自然键加 DB 唯一约束

> Plan Status: planned
> Mission: audit-remediation
> Work Item: P0-MA2-020 fix（A2.17 并发与乐观锁审查发现的 P0）
> Last Reviewed: 2026-07-28
> Source: `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11 P0-MA2-020`
> Related: `docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`（来源审计 plan，A2.17 done）；`docs/design/inventory/`（库存余额 owner doc）；`docs/plans/2026-07-07-0024-2-inventory-concurrency-negative-stock.md`（并发负库存前置实现）
> Audit: required

## Current Baseline

A2.17 并发与乐观锁审计发现 **P0-MA2-020**：`erp_inv_stock_balance` **无自然键 UK** on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（仅非唯一 `IDX_INV_STOCK_BALANCE_*`）；`StockMoveBookkeeper.upsertBalance:137-170` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance:211-237` 实现 check-then-insert（`findBalance==null` → INSERT）。

**实时仓库证据**（`module-inventory/erp-inv-service/src/main/java/`）：

- `StockMoveBookkeeper.java:137-170` `upsertBalance`：`findBalance:301 findAllByQuery` → if null → new `ErpInvStockBalance` + `saveEntity`
- `ErpInvOwnershipTransferProcessor.java:211-237` `upsertTargetBalance`：同型 check-then-insert
- `module-inventory/model/app-erp-inventory.orm.xml:404-422` 仅非唯一 `IDX_INV_STOCK_BALANCE_*` indexes（materialId / warehouseId / locationId / ownerId），**无自然键 UK**
- `ErpInvStockBalance.versionProp="version"`（`:369`）但不保护 INSERT 路径

**影响**：并发首次移动单针对同一新维度（新品上架时多个并发收货单 / 多仓并行初始化）→ 两事务都 `findBalance==null` → 都 INSERT → **重复余额行**。后续 `findAllByQuery(...).get(0)` 读到任一行 → **silent split-quantity corruption**（数量/金额被分散到两行，totalQuantity/totalCost 漂移，破坏余额守恒不变量）。silent corruption（无异常抛出）。

## Goals

- 修复 P0-MA2-020：(1) `module-inventory/model/app-erp-inventory.orm.xml` 给 `ErpInvStockBalance` 加唯一约束 `<key name="UK_INV_STOCK_BALANCE_NATURAL" unique="true">` on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（含 orgId 以兼容多公司隔离 A2.18）；(2) 数据 cleanup（若现存重复余额行，须先合并）；(3) `StockMoveBookkeeper.upsertBalance` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance` 捕获 ConstraintViolation → reload + retry（复用 `tryUpdateWithVersionCheck` retry 模式）。
- 触及 inventory 库存余额保护区域 + ORM ask-first（UK 变更）→ 须独立 plan-audit + 人工确认。
- 补并发首次 INSERT 负向测试（双线程并发首次移动单同维度应抛约束违例 + retry 成功）。

## Non-Goals

- **不**改 `StockMoveBookkeeper.updateBalanceWithRetry` 的 DONE 路径 retry 机制（已最强模式，本 P0 仅补 INSERT 路径）。
- **不**重构 costing strategies（仅补 upsertBalance INSERT 竞态防护）。
- **不**修复 LandedCost 同 receiveId 窗口期（P1-MA2-085 归 MR1）。
- **不**实现悲观锁 SELECT FOR UPDATE（余额表高并发写，死锁风险高，不推荐）。

## Task Route

- Type: `Bug investigation` + `implementation change`
- Owner Docs: `docs/design/inventory/`（库存余额自然键 owner doc）+ `docs/design/flow-overview.md §6.1 事务边界`
- Skill: `nop-backend-dev`（ORM UK 变更 + retry 模式扩展）+ ORM ask-first
- Verification: `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-inventory/erp-inv-service`（库存测试）+ 并发首次 INSERT 负向新测试通过

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。Maven Reactor 走标准构建。
- **保护区域门控**：inventory 库存余额保护区域（最高级）+ ORM ask-first（UK 变更）→ 须 owner doc + 人工确认 + 独立 plan-audit。
- **数据迁移门控**：UK 落地前须确认 `erp_inv_stock_balance` 现存无重复（如有重复须先合并——按 totalQuantity/totalCost 加总）。

## Execution Plan

### Phase 1 - 加 ErpInvStockBalance 自然键唯一约束 + retry 模式扩展

Status: planned

Targets: `module-inventory/model/app-erp-inventory.orm.xml`（ErpInvStockBalance 实体）；`module-inventory/erp-inv-service/.../entity/StockMoveBookkeeper.java:137-170`（upsertBalance 捕获 ConstraintViolation）；`module-inventory/erp-inv-service/.../entity/ErpInvOwnershipTransferProcessor.java:211-237`；数据 cleanup 脚本（如需）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-020 已识别）；数据 cleanup 评估完成；nop-entropy 父 POM 已在本地 Maven 仓库

- [ ] 数据评估：grep `erp_inv_stock_balance` 现存重复自然键行（如有，合并策略：totalQuantity/totalCost 加总 + 流水回链 + 人工确认）
- [ ] ORM 变更：`app-erp-inventory.orm.xml` ErpInvStockBalance 加 `<key name="UK_INV_STOCK_BALANCE_NATURAL" unique="true"><column name="orgId"/><column name="materialId"/><column name="skuId"/><column name="warehouseId"/><column name="locationId"/><column name="batchNo"/><column name="ownerId"/></key>`（含 orgId 兼容多公司隔离）
- [ ] `mvn clean install -DskipTests`（codegen 增量再生 + 154 reactor 全绿）
- [ ] `StockMoveBookkeeper.upsertBalance` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance` 捕获 ConstraintViolation → reload 已落地行 + 转为 update 路径（复用 `tryUpdateWithVersionCheck` retry 模式，max `erp-inv.concurrent-deduct-max-retry=5`）
- [ ] owner doc `docs/design/inventory/` 补「库存余额自然键 UK `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 兜底 + upsertBalance retry-on-conflict」
- [ ] 补负向测试：`testConcurrentFirstMoveSameDimensionThrowsAndRetries`（双线程并发首次移动单同维度，一个成功 INSERT，一个 ConstraintViolation → reload + update 成功）
- [ ] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-inventory/erp-inv-service`（库存测试全绿）

Exit Criteria:

- [ ] ErpInvStockBalance 自然键 UK 落地 + codegen 再生全绿
- [ ] upsertBalance/upsertTargetBalance 捕获 ConstraintViolation + retry 路径生效
- [ ] 负向测试覆盖并发首次 INSERT 场景
- [ ] owner doc 同步更新

## Closure Gates

- [ ] 范围内行为完成（UK 落地 + retry 扩展 + 测试通过）
- [ ] 相关文档对齐（inventory owner doc + flow-overview.md §6.1）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-inventory/erp-inv-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立 plan-audit 完成
- [ ] 文本一致性已验证

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写>
- Evidence: <待执行后填写>
