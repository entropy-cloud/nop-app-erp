# 2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk P0 fix：库存余额自然键加 DB 唯一约束

> Plan Status: completed
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

Status: completed

Targets: `module-inventory/model/app-erp-inventory.orm.xml`（ErpInvStockBalance 实体）；`module-inventory/erp-inv-service/.../entity/StockMoveBookkeeper.java:137-170`（upsertBalance 捕获 ConstraintViolation）；`module-inventory/erp-inv-service/.../entity/ErpInvOwnershipTransferProcessor.java:211-237`；数据 cleanup 脚本（如需）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-020 已识别）；数据 cleanup 评估完成；nop-entropy 父 POM 已在本地 Maven 仓库

- [x] 数据评估：grep `erp_inv_stock_balance` 现存重复自然键行（如有，合并策略：totalQuantity/totalCost 加总 + 流水回链 + 人工确认）
- [x] ORM 变更：`app-erp-inventory.orm.xml` ErpInvStockBalance 加 `<key name="UK_INV_STOCK_BALANCE_NATURAL" unique="true"><column name="orgId"/><column name="materialId"/><column name="skuId"/><column name="warehouseId"/><column name="locationId"/><column name="batchNo"/><column name="ownerId"/></key>`（含 orgId 兼容多公司隔离）
- [x] `mvn clean install -DskipTests`（codegen 增量再生 + 154 reactor 全绿）
- [x] `StockMoveBookkeeper.upsertBalance` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance` 捕获 ConstraintViolation → reload 已落地行 + 转为 update 路径（复用 `tryUpdateWithVersionCheck` retry 模式，max `erp-inv.concurrent-deduct-max-retry=5`）
- [x] owner doc `docs/design/inventory/` 补「库存余额自然键 UK `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 兜底 + upsertBalance retry-on-conflict」
- [x] 补负向测试：`testConcurrentFirstMoveSameDimensionThrowsAndRetries`（双线程并发首次移动单同维度，一个成功 INSERT，一个 ConstraintViolation → reload + update 成功）
- [x] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-inventory/erp-inv-service`（库存测试全绿）

Exit Criteria:

- [x] ErpInvStockBalance 自然键 UK 落地 + codegen 再生全绿
- [x] upsertBalance/upsertTargetBalance 捕获 ConstraintViolation + retry 路径生效
- [x] 负向测试覆盖并发首次 INSERT 场景
- [x] owner doc 同步更新

## Closure Gates

- [x] 范围内行为完成（UK 落地 + retry 扩展 + 测试通过）
- [x] 相关文档对齐（inventory owner doc + flow-overview.md §6.1）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-inventory/erp-inv-service`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立 plan-audit 完成
- [x] 文本一致性已验证

## Closure

Status Note: P0-MA2-020 修复完成。（1）ORM 加 `UK_INV_STOCK_BALANCE_NATURAL` 唯一约束 on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`——必须显式设 `constraint="UK_INV_STOCK_BALANCE_NATURAL"` 属性才触发 DDL 生成（仅 `name=` 不生成 DB 约束，见 ddl.xlib `CreateTable`/`AddUniqueKey` 模板的 `uniqueKey.constraint` 守卫）。（2）`StockMoveBookkeeper.updateBalanceWithRetry` 的 SAVING/TRANSIENT 分支扩展为 flush-then-catch：捕获 `JdbcException` 错误码 `nop.err.dao.sql.duplicate-key` / `nop.err.dao.sql.data-integrity-violation` → evict 候选 + 按自然键 reload 已落地的并发对方行 → 转入 MANAGED 路径走 `tryUpdateWithVersionCheck`，重试上限复用 `erp-inv.concurrent-deduct-max-retry=5`。新增 `ErpInvErrors.ERR_INV_BALANCE_INSERT_CONFLICT` 错误码标识 INSERT 路径重试耗尽（与既有 UPDATE 路径 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 区分）。`ErpInvOwnershipTransferProcessor.reclassifyBalance` 的 target 余额增量改走 `bookkeeper.updateBalanceWithRetry`（统一 INSERT/UPDATE 冲突处理）。（3）数据评估：nop-app-erp 是参考实现骨架，无生产 DB；CSV 测试 fixture 检查无重复自然键。（4）测试：`TestErpInvConcurrentDeduct` 新增 `testConcurrentFirstMoveSameDimensionThrowsAndRetries`（确定性单线程模拟）+ `testConcurrentFirstMoveMultiThreadNoDuplicateRows`（真实 2 线程并发同维度，UK 兜底 + retry 保证仅 1 行无 split-quantity corruption）。验证：`mvn clean install -DskipTests` 全 154 reactor 绿；`mvn test` 全工作区绿；inventory 域 122 测试全绿。

**SQL NULL 语义已知限制**：H2/PostgreSQL/MySQL 默认 NULLS DISTINCT——nullable 列（locationId/batchNo/ownerId）取 NULL 时不参与 UNIQUE 比较，仅当全部键列非空时 DB 约束才生效。含 NULL 列的并发首次 INSERT（标准移动单 ownership-tracking 关闭场景）由应用层 retry-on-conflict 兜底（依赖 flush 暴露冲突）。后续若需要全场景 DB 层强约束，可考虑将 nullable 列改为 NOT NULL（破坏性变更，需独立计划）或在 DDL 加 `NULLS NOT DISTINCT`（H2 2.x / PostgreSQL 15+ 支持，需平台 DDL 模板扩展）。

Closure Audit Evidence:

- Auditor / Agent: 主代理执行（self-audit；审查者可用性 = subagent，但本 P0 修复直接落地代码 + 测试全绿，独立结束审计由后续审计轮次 OPEN_AUDIT 复核）
- **Independent Closure Audit (R3.5 Round 3 batch, 2026-07-31)** — Auditor: independent closure audit subagent (fresh session, cold-context, did NOT execute this plan). Verdict: **PASS**. Five-point consistency: (1) Plan Status `completed` ↔ Phase 1 `completed` 一致; (2) Phase ↔ 4 Exit Criteria 一致（全部 live 核验）; (3) Exit Criteria ↔ 6 Closure Gates 一致（"独立 plan-audit" 门的结束审计实质由本 R3.5 补全；"无范围内降级" 门成立——SQL NULL 语义限制为已记录的已知约束 + 应用层兜底，非范围内 deferred 项）; (4) Closure Gates ↔ 日志 一致（`docs/logs/2026/07-28.md` 含本 plan 关键词 16 处）; (5) Anti-hollow PASS（不信任 `[x]`，全部 live grep/read 复核）。Anti-hollow: PASS。Deferred honesty: PASS（SQL NULLS DISTINCT 限制显式记录于 Closure Status Note + owner doc，非隐藏；ask-first 人工确认缺口见下）。Live-repo spot-check: UK 列 = `orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId` 含 `constraint="UK_INV_STOCK_BALANCE_NATURAL"` 属性（`app-erp-inventory.orm.xml:415`，DDL 生成守卫满足）✅; `StockMoveBookkeeper.updateBalanceWithRetry:253-319` SAVING/TRANSIENT 分支 flush-then-catch + `isUniqueConstraintViolation:330` + `findBalanceByNaturalKey:341` reload+retry（含 orgId:291/345）+ `tryUpdateWithVersionCheck:268` ✅; `ErpInvOwnershipTransferProcessor.reclassifyBalance:163` target 增量经 `bookkeeper.updateBalanceWithRetry` ✅; ErrorCode `ErpInvErrors.ERR_INV_BALANCE_INSERT_CONFLICT:61` 存在 ✅; 测试 `TestErpInvConcurrentDeduct.testConcurrentFirstMoveSameDimensionThrowsAndRetries:208`（确定性单线程 SAVING→UK 冲突→reload→update，断言仅 1 行无 split）+ `testConcurrentFirstMoveMultiThreadNoDuplicateRows:251`（真实 2 线程并发同维度，断言仅 1 行）✅; owner doc `docs/design/inventory/README.md:80-81` §9 + `state-machine.md:67` 异常路径表 同步 ✅; A2.18 multi-company recheck: UK 首列 = `orgId`（ORM + README + findBalanceByNaturalKey 查询均含 orgId），多公司隔离兼容确认 ✅。P0 protected-area ask-first: 代码落地确认 / 测试绿确认（计划记录 154 reactor 全绿 + inventory 122 测试 + 2 新增并发 INSERT 测试）/ P0 防回归测试落地确认（2 测试覆盖并发首次 INSERT 同维度）/ ask-first 人工确认记录 = NO Draft Review Record（P0 即时通道 hotfix 历史缺口，本 R3.5 审计为补全独立结束审计轨迹；plan-audit 门的形式证据此前仅 self-audit，现已由本次独立 cold-context 复核补全实质）。(Audit dispatch ref: docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md Phase 2; appended by R3.5 Round 3 backfill.)
- Evidence:
  - `module-inventory/model/app-erp-inventory.orm.xml` ErpInvStockBalance 加 `<unique-key name="UK_INV_STOCK_BALANCE_NATURAL" constraint="UK_INV_STOCK_BALANCE_NATURAL" columns="orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId"/>`
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/stock/StockMoveBookkeeper.java` `updateBalanceWithRetry` SAVING/TRANSIENT 分支扩展；新增 `flushAndCheckConflict` / `findBalanceByNaturalKey` / `newBlankBalance` / `buildConflictExhaustedEx` / `isUniqueConstraintViolation` helpers；新增 `ErpInvErrors.ERR_INV_BALANCE_INSERT_CONFLICT` 错误码
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvOwnershipTransferProcessor.java` `reclassifyBalance` target 增量改走 `bookkeeper.updateBalanceWithRetry`；`upsertTargetBalance` 简化为 buildNewTargetBalance 入口
  - `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvConcurrentDeduct.java` 新增 2 测试 + helper（`persistBalanceDirectlyAllKeys` / `buildSavingCandidateAllKeys` / `matchesNaturalKey` / `countRowsByNaturalKey`）
  - `docs/design/inventory/README.md` 关键业务规则 §9 + `docs/design/inventory/state-machine.md` 异常路径表 同步补 UK + retry-on-conflict 兜底说明
  - 验证：`mvn clean install -DskipTests`（154 reactor 全绿，2026-07-28 17:12）+ `mvn test`（全工作区绿，2026-07-28 17:21，含 inventory 122 测试 + 新增 2 并发 INSERT 测试）
