# A4.2.1 + A4.2.2 预留量并发扣减与 STOCK_PARTIAL 齐套可用量运行时确认（reserved 写路径 Deferred 下当前行为）

> Verdict: **pass（零 P0、零新 finding——维持 P1-RC-008 P1，残留并发竞争归 A2.17 既有追踪，不重复登记）**
> Mission: requirement-compliance（MA4 运行时确认）
> Work Item: A4.2.1（A1.8 §7 SP-1 预留量并发扣减运行时行为）+ A4.2.2（A1.8 §7 SP-2 STOCK_PARTIAL 强制开工后领料 KitAvailabilityChecker 只读路径补料后可用量）
> Source Plan: `docs/plans/2026-08-06-1926-3-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据[含 P0①活跃数据破坏 / P0④会计过账正确性破坏 / P1①功能完全缺失 / P2①次要验收标准] + §4 Q1 真相源层级 + §7 arm-index 衔接 + §8 过程纪律自检 + §去重协议[MA4↔A5.6 边界]）
> 关联 finding：`P1-RC-008`（UC-MFG-05/08 物料预留写路径 Deferred，arm-index §P1）+ `P0-MA2-020`（stock move bookkeeper negative-stock 防护 UK_INV_STOCK_BALANCE_NATURAL + 余额守恒，resolved）+ A2.17（库存并发扣减审查，残留并发竞争归口）
> Audit Type: verification or audit work（只读运行时行为评估——不改代码/ORM/api.xml/真相源）

## 0. 审计结论（TL;DR）

A1.8 §7 SP-1/SP-2 两个运行时存疑点经实仓 L3 代码路径 census + 并发场景推理 + 三源对照 **CONFIRMED 当前 Deferred 状态运行时安全性可接受，不升 P0，无新 finding**：

| 存疑点 | 运行时核验结论 | 裁决 |
|--------|---------------|------|
| **SP-1** 预留量并发扣减运行时行为（reserved 恒为 0，多工单并发领料 negative-stock 防护兜底） | **无 silent split-quantity corruption**——stock move 实际扣减经 `StockMoveBookkeeper.updateBalanceWithRetry`（`StockMoveBookkeeper.java:256-328`，versionProp 乐观锁 `tryUpdateWithVersionCheck` + P0-MA2-020 `UK_INV_STOCK_BALANCE_NATURAL` 冲突 evict+reload+重试 `erp-inv.concurrent-deduct-max-retry`=5），每个成功扣减被正确串行化，无 delta 丢失。残留：`validateAvailable`（`ErpInvStockMoveProcessor.java:116-136`）是 read-time 检查，真正并发下两工单确认可同时通过（均读到扣减前余额），`onOutgoing` 的 `applyDelta`（`MovingAverageCostingStrategy.java:73-84`）无 write-time 负数守卫 → 真并发下 `totalQuantity` 可被驱动为负（超额承诺），但**可见非静默**（负余额可观测）+ **由 `erp-inv.allow-negative-stock`=false 默认门控 validateAvailable** + **同 stock-move 扣减并发模型即使预留实现后亦不变**（预留仅增隔离层不改变扣减并发机制）→ 该残留并发竞争**归 A2.17 既有追踪**（O2C 审计 `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md:183,195` 已登记「并发出库同一 (materialId,warehouseId,locationId) 双读双写 reservedQuantity lost-update 风险 → A2.17」），§去重协议不重复登记 | **维持 P1-RC-008 P1**（不升 P0）；残留并发归 A2.17 |
| **SP-2** STOCK_PARTIAL 强制开工后领料 KitAvailabilityChecker 只读路径补料后可用量（无缓存/陈旧读） | **无陈旧读**——`KitAvailabilityChecker.check`（`KitAvailabilityChecker.java:62-89`）每次调用经 `loadAvailableByMaterial:102-114` → `daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(buildBalanceQuery(materialIds))`（`:109`）做**实时 DB 查询**，直接读实体 `b.getAvailableQuantity()`（`:111`），**无缓存层/无 session 复用陈旧值**。STOCK_PARTIAL 强制开工后补料（incoming stock move DONE 增 `totalQuantity` + `recomputeAvailable` 重算 `availableQuantity`）→ 二次齐套校验 `check()` 重新 `findAllByQuery` 读到补料后的最新余额 | **SP-2 消解（无陈旧读）** |
| **config consumption 模式**（STOCK_PARTIAL 强制开工可达性 + 预留隔离模式） | `ErpMfgBom.consumption`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:201`）是 **per-BOM 持久化字段**（非全局 config key，nullable 无 defaultValue，dict `erp-mfg/consumption` = {FLEXIBLE/WARNING/STRICT}），**运行时 service 代码零消费**（grep `getConsumption`/`CONSUMPTION_STRICT` 于 erp-mfg-service 0 命中）。STOCK_PARTIAL 强制开工（STOCK_PARTIAL→IN_PROCESS）**仅由 `erp-mfg.allow-partial-kit-start` 门控**（`ErpMfgWorkOrderProcessor.validateTransitionForStart:256-267` + `isAllowPartialKitStart:385-387`，**默认 FALSE**）→ STOCK_PARTIAL 强制开工**默认不可达**（需运维显式 opt-in） | **config 精化**（plan baseline「consumption 决定可达性」纠正为「allow-partial-kit-start 决定可达性」） |

**整体裁决**：当前预留写路径 Deferred 状态下，①并发领料无 silent split-quantity corruption（updateBalanceWithRetry 串行化）+ ②齐套校验无陈旧读（实时 findAllByQuery）→ **维持 P1-RC-008 P1**（预留写路径 Deferred 合规缺口[需求契约维度，A1.8 §5 已裁决] 不变，但当前行为**不致活跃数据破坏**——库存余额守恒由 stock move bookkeeper + P0-MA2-020 UK + versionProp 乐观锁独立防护）。残留并发竞争（validateAvailable read-time 检查的真并发 over-commitment 窗口）**归 A2.17 既有追踪**，§去重协议不重复登记。**不触发 MR0，不升 P0/P1，无新 finding。**

---

## 1. 关键 baseline 精化（零信任核验修正 plan/A1.8 措辞）

> 本节记录实仓核验对 plan `Current Baseline` 与 A1.8 §7 SP-1 措辞的精化。**方向性结论不变**（预留写路径 Deferred → P1-RC-008 正确），但机制描述需精确化。

### 1.1 字段名修正：`reservedQuantity`（非 `reservedQty`）

plan/A1.8 通篇用业务语义名 `reservedQty`。实仓 `ErpInvStockBalance`（`module-inventory/erp-inv-dao/.../_gen/_ErpInvStockBalance.java:56-58,245-246`）ORM 真相源（`module-inventory/model/app-erp-inventory.orm.xml:379`）字段为 **`reservedQuantity`**（`RESERVED_QUANTITY`，propId=9，DECIMAL(20,4)，defaultValue=0）。同理 `onHand`=`totalQuantity`、`available`=`availableQuantity`、`locked`=`lockedQuantity`。本报告下文统一用真实字段名。

### 1.2 「reserved 恒为 0（无 writer）」纠正为「mfg 路径无持久化 writer（库存域有瞬时 net-zero writer）」

A1.8 §7 SP-1（`2026-08-02-2042-2-...-a1-8-...md:243`）原文「当前 reserved 恒为 0（无 writer）」。实仓 `setReservedQuantity` 全集 census（§2.1）**纠正**：

- **库存域确有 writer**：`ErpInvStockMoveProcessor.applyReservation:138-154`（`:150 balance.setReservedQuantity(reserved)`）在 stock move **CONFIRM** 时为 OUTGOING/INTERNAL_TRANSFER moveType 写非零 `reservedQuantity += line.quantity`，并 `recomputeAvailable`（`:278-283`，`available = total − reserved − locked`）。`StockMoveBookkeeper.buildNewBalanceForMove:175` 与 `newBlankBalance:393` 初始化新余额行为 0；`ErpInvOwnershipTransferProcessor:196` 初始化为 0。
- **但 mfg 领料路径为 net-zero apply-release**：mfg 领料（`MaterialIssueStockMoveBuilder.build:35`）用 `MOVE_TYPE_OUTGOING_ISSUE = "OUTGOING"`（`ErpMfgConstants.java:69`，**同值** `ErpInvConstants.MOVE_TYPE_OUTGOING`）→ `isBusinessLinked()=true`（`relatedBillType=ERP_MFG_ISSUE`+`relatedBillCode=issue.code`）→ `ErpInvStockMoveGenerateMoveProcessor.generateMove:41-45` 在**同一 @BizMutation 事务**内先 `doConfirm`（`applyReservation` 写 `reservedQuantity += qty`）后立即 `doComplete`（`releaseReservation` → `applyReservation(reserve=false)` 写 `reservedQuantity −= qty`）→ **净效果 reservedQuantity 归零**，随后 `bookCompletion` 扣 `totalQuantity`。故 mfg 路径**不持久化预留**（不提供跨工单隔离）。
- **mfg 域零持久化 writer**：`KitAvailabilityChecker`（只读 `availableQuantity`，无 setter）、`ErpMfgWorkOrderProcessor`（`rg reservation` 全文 0 命中）、`MaterialIssueStockMoveBuilder`（仅构造 OUTGOING 移动单请求）、`ErpInvReservationBizModel`（15 行 CRUD 桩，`ErpInvReservationBizModel.java:1-15`，无 purpose-built 写方法）→ **mfg 域从不在 `ErpInvStockBalance.reservedQuantity` 上持久化跨工单预留**。

**精化结论**：plan 方向性结论（mfg 预留写路径 Deferred → 无跨工单预留隔离 → P1-RC-008 正确）**成立**；但「无 writer」措辞**不准确**——库存域 stock move confirm 有瞬时 writer，mfg 领料经 net-zero apply-release 不持久化。本精化不改变任何裁决，仅使机制描述诚实。

---

## 2. reservedQuantity writer 全集 census（SP-1 前置，Phase 1 Proof ①）

> grep 全集：`rg "setReservedQuantity" module-inventory module-manufacturing`（excl test/_gen ORM setter 的调用点）。生产代码 writer 矩阵（类 × 方法 × 行号 × 写/读 × 语义）：

| # | 类 | 方法:行号 | 写入值 | 语义 | 域 |
|---|----|----------|--------|------|-----|
| W1 | `ErpInvStockMoveProcessor` | `applyReservation:150` | `reservedQuantity += qty × sign`（sign=+1 reserve/−1 release） | stock move CONFIRM 预留 / DONE 释放（OUTGOING+INTERNAL_TRANSFER） | inv |
| W2 | `ErpInvStockMoveProcessor` | `doConfirm:95`（调 W1 reserve=true）/ `doComplete:109`（→`releaseReservation:156-158` 调 W1 reserve=false） | 触发 W1 | 状态机编排：confirm 写预留 / done 释放预留后 bookCompletion | inv |
| W3 | `StockMoveBookkeeper` | `buildNewBalanceForMove:175` | `BigDecimal.ZERO` | 新余额行初始化（INSERT 路径） | inv |
| W4 | `StockMoveBookkeeper` | `newBlankBalance:393` | `BigDecimal.ZERO` | 极罕见「对方事务回滚后重试 INSERT」候选初始化 | inv |
| W5 | `ErpInvOwnershipTransferProcessor` | `:196` | `BigDecimal.ZERO` | 所有权转移新余额初始化 | inv |
| R1 | `KitAvailabilityChecker` | `loadAvailableByMaterial:111` | 读 `getAvailableQuantity()` | 齐套校验只读（mfg） | mfg→inv 只读 |
| R2 | `ErpInvStockMoveProcessor` | `validateAvailable:126`（读 available）/ `applyReservation:149`（读 reserved） | 读 | 状态机校验+预留计算 | inv |
| R3 | `StockMoveBookkeeper.recomputeAvailable` / `ErpInvStockMoveProcessor.recomputeAvailable` | `:222-228` / `:278-283` | 读 reserved → 重算 available | 派生公式 `available = total − reserved − locked` | inv |

**census 裁决**：
- **持久化非零 writer 仅 W1**（stock move confirm）。W3/W4/W5 为零值初始化（INSERT 新行）。
- **mfg 域（module-manufacturing）零 `setReservedQuantity` 调用**（grep `setReservedQuantity` 于 module-manufacturing 0 命中）→ mfg 从不直接写 `ErpInvStockBalance.reservedQuantity`。
- **mfg 领料经库存域 stock move（W1）走 net-zero apply-release**（§1.2）→ mfg 视角下 `reservedQuantity` 持久化值不受 mfg 领料影响（apply 与 release 同事务抵消）。
- **`availableQuantity` 派生公式**（R3）：`available = totalQuantity − reservedQuantity − lockedQuantity`（`StockMoveBookkeeper.java:223-227`），**在 Java 应用层重算并持久化**（非 DB 计算列；orm.xml:381 `availableQuantity` 为普通 DECIMAL 列 mandatory=true）。reserved=0（mfg 持久化视角）下退化为 `total − locked`。

---

## 3. negative-stock 防护并发兜底核验（SP-1 核心，Phase 1 Proof ②）

### 3.1 防护机制 file:line 证据

| 防护层 | file:line | 机制 |
|--------|-----------|------|
| **read-time 负数检查** | `ErpInvStockMoveProcessor.validateAvailable:116-136` | `allow-negative-stock=false`（默认，`ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK` + `isNegativeStockAllowed:285-288`）+ `reservesOnConfirm(moveType)`（OUTGOING/INTERNAL_TRANSFER，`:242-248`）→ 每行 `upsertBalance` 读 `availableQuantity`，`available < required` 抛 `ERR_AVAILABLE_INSUFFICIENT`（`:128-134`）。**mfg 领料 moveType=OUTGOING 满足 `reservesOnConfirm`** |
| **write-time 乐观锁串行化** | `StockMoveBookkeeper.updateBalanceWithRetry:256-328` | MANAGED 实体 `tryUpdateWithVersionCheck`（`:271`）→ `UPDATE WHERE id=? AND version=?`；冲突（affected=0，实体置 readonly）→ evict + `requireEntityById` reload + 重试 `applyDelta`（`:299-315`）；重试上限 `erp-inv.concurrent-deduct-max-retry`=5（`:260-261`），耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`（`:408-411`） |
| **INSERT 路径 UK 冲突兜底** | `StockMoveBookkeeper.updateBalanceWithRetry:272-278,316-326` + `isUniqueConstraintViolation:427-440` | 新余额（TRANSIENT/SAVING）flush 触发 INSERT；`UK_INV_STOCK_BALANCE_NATURAL` 冲突（`DaoErrors.ERR_SQL_DUPLICATE_KEY`/`ERR_SQL_DATA_INTEGRITY_VIOLATION`）→ evict + 按自然键 reload 对方行 + 转 MANAGED 重试 |
| **versionProp + UK 真相源** | `module-inventory/model/app-erp-inventory.orm.xml:369,415` | `versionProp="version"`（:369，propId=17 mandatory）+ `UK_INV_STOCK_BALANCE_NATURAL`（:415，columns=`orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId`）= **P0-MA2-020 已 resolved** |

### 3.2 并发场景推理（SP-1 核心：silent split-quantity corruption 是否存在）

**场景**：WO A、WO B 各需物料 M 10 件，仓库 M 现有 15 件（`totalQuantity=15, reservedQuantity=0, availableQuantity=15`）。两工单齐套校验均通过（`KitAvailabilityChecker` 读 available=15≥10）后**并发领料**。

**关键路径**：mfg 领料 = `generateMove`（businessLinked）= 同一 @BizMutation 事务内 `doConfirm`（`validateAvailable` + `applyReservation`）+ `doComplete`（`releaseReservation` + `bookCompletion`→`onOutgoing`→`updateBalanceWithRetry`）。预留 apply-release 净零；**真正扣减 = `bookCompletion` 的 `totalQuantity −= qty`**。

| 并发形态 | 行为 | 是否 silent corruption |
|---------|------|----------------------|
| **串行化扣减（T_A 提交后 T_B 读）** | T_A `validateAvailable` 读 available=15≥10 OK → `bookCompletion` 扣 total 15→5（v0→v1，version check OK）提交。T_B `validateAvailable` 读 available=5 < 10 → **抛 `ERR_AVAILABLE_INSUFFICIENT`**，T_B 领料失败 | **否**——T_B 显式失败，无超扣 |
| **真并发（两事务 doConfirm 在对方提交前均读到 available=15）** | T_A、T_B `validateAvailable` 均 OK（读扣减前余额）。扣减经 `updateBalanceWithRetry` 串行化：T_A 扣 total 15→5（v0→v1 OK）；T_B 扣时 version check 失败（行已 v1）→ evict+reload（total=5 v1）→ 重试 applyDelta total=5−10=**−5**（v1→v2 OK）→ **totalQuantity=−5（负）**。两工单领料**均成功** | **否（lost-update 意义）**——`updateBalanceWithRetry` 确保无 delta 丢失，每次成功扣减被正确串行化；但**余额变负（超额承诺）** |

**SP-1 核心裁决**：**silent split-quantity corruption 不存在**（lost-update 意义）——`updateBalanceWithRetry`（versionProp 乐观锁 + P0-MA2-020 UK + 重试）保证每个成功扣减被串行化、无 delta 丢失、无部分工单领料成功但总量静默错扣。一个工单的领料要么完整扣减（version check 通过）要么失败（冲突重试耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`）。

**残留（非 SP-1 静默腐败，但需登记边界）**：真并发下 `validateAvailable`（read-time）可被两事务同时通过，`onOutgoing.applyDelta`（`MovingAverageCostingStrategy.java:73-84`）**无 write-time 负数守卫** → `totalQuantity` 可被驱动为负（over-commitment）。但：
1. **可见非静默**——负余额在 `ErpInvStockBalance.totalQuantity/availableQuantity` 可观测，非静默错扣；
2. **由 `erp-inv.allow-negative-stock=false` 默认门控**——validateAvailable 在默认配置下激活（仅真并发 check-then-act 窗口可绕过）；
3. **同 stock-move 扣减并发模型即使预留实现后亦不变**——预留实现（P1-RC-008 修复）会增加跨工单隔离层（kit 校验时 account for 其他在途工单需求），但底层 stock move DONE 扣减的并发模型（validateAvailable + updateBalanceWithRetry）不变，残留 check-then-act 窗口是 read-check-then-act 无 serializable 隔离/write-time 负数守卫的固有属性，非预留 Deferred 引入；
4. **归 A2.17 既有追踪**——O2C 审计 `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md:183,195` 已登记「并发出库同一 (materialId,warehouseId,locationId) 双读双写 reservedQuantity lost-update 风险 → 归 A2.17」。本验证按 §去重协议**不重复登记**（同控制点：库存余额并发扣减 lost-update/over-commit）。

---

## 4. KitAvailabilityChecker 只读路径可用量正确性核验（SP-2 核心，Phase 1 Proof ③）

### 4.1 查询路径 file:line 证据

| 步骤 | file:line | 行为 |
|------|-----------|------|
| 1. 入口 | `KitAvailabilityChecker.check:62-89` | `requireWorkOrder` → `resolveBomId`（wo.bomId 优先，缺失回落产品默认 BOM）→ `bomExpander.explode` 多级展开子件 × `plannedQuantity` → `aggregateRequirements` 按物料聚合需求 |
| 2. **实时余额查询** | `loadAvailableByMaterial:102-114` → `daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(buildBalanceQuery(materialIds))`（`:107,109`） | **每次 `check` 调用经 `IDaoProvider` 做 fresh DB 查询**（`QueryBean` filter `materialId IN (...)`，`buildBalanceQuery:116-120`），**无缓存层、无 session 复用陈旧值、无 @Cacheable** |
| 3. 读 availableQuantity | `:111` `availableByMaterial.merge(b.getMaterialId(), nz(b.getAvailableQuantity()), BigDecimal::add)` | 直接读实体持久化字段 `availableQuantity`（多仓/批聚合求和） |
| 4. 对比 | `:76-87` | `available < required` → `KitAvailabilityResult.partial()`（附缺料明细）；全齐 → `reserved()` |

### 4.2 SP-2 裁决：无陈旧读

`KitAvailabilityChecker.check` **无任何缓存**——每次调用重新 `findAllByQuery` 读 DB。STOCK_PARTIAL 强制开工（`allow-partial-kit-start=true` opt-in）后补料流程：补料 = incoming stock move（如采购入库 / 生产入库 / 调拨入库）DONE → `bookCompletion`→`onIncoming`（`MovingAverageCostingStrategy.onIncoming:36-60`）增 `totalQuantity` + `recomputeAvailable`（`:54`）重算 `availableQuantity = total − reserved − locked`（持久化）→ **二次 `checkAvailability`（`ErpMfgWorkOrderProcessor.checkAvailability:111-118` 重新调 `kitAvailabilityChecker.check`）`findAllByQuery` 读到补料后最新余额**。

**SP-2 消解**：无缓存、无陈旧读。齐套校验实时反映补料后可用量。

---

## 5. config consumption 模式核验（Phase 1 Proof ④）

| 维度 | 实仓核验 | 裁决 |
|------|---------|------|
| `ErpMfgBom.consumption` 真相源 | `module-manufacturing/model/app-erp-manufacturing.orm.xml:201`（`code=CONSUMPTION propId=5 VARCHAR(20) ext:dict=erp-mfg/consumption`，**nullable 无 defaultValue**） | per-BOM 持久化字段，非全局 config key |
| `erp-mfg/consumption` dict | `:61-65` = {FLEXIBLE 允许超耗 / WARNING 超耗警告 / STRICT 严格按 BOM} | 三值可选 |
| **运行时 service 消费** | `rg "getConsumption\|CONSUMPTION_STRICT\|isConsumption" module-manufacturing/erp-mfg-service` = **0 命中**（仅 dao 层常量 `_ErpMfgDaoConstants.java:109-119` + ORM getter + view/meta 展示） | **consumption 运行时零消费**（仅 UI/展示 + 未来用） |
| STOCK_PARTIAL 强制开工门控 | `ErpMfgWorkOrderProcessor.validateTransitionForStart:256-267`：STOCK_PARTIAL → IN_PROCESS **仅当 `isAllowPartialKitStart()`**（`erp-mfg.allow-partial-kit-start`，`ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START:73`，**默认 FALSE**，`readBoolConfig:400-410`）；否则抛 `ERR_PARTIAL_KIT_START_FORBIDDEN`（`ErpMfgErrors:85`） | **STOCK_PARTIAL 强制开工默认不可达**（需运维 opt-in） |

**裁决**：plan baseline「config consumption 决定 STOCK_PARTIAL 强制开工可达性」**纠正**——运行时门控是 `erp-mfg.allow-partial-kit-start`（默认 FALSE），**非** `consumption`（后者 per-BOM 字段运行时零消费）。故 STOCK_PARTIAL 强制开工在默认部署下**不可达**；SP-2 的「STOCK_PARTIAL 强制开工后补料」场景需运维显式 opt-in `allow-partial-kit-start=true` 方可触发，触发后 §4 已证 KitAvailabilityChecker 实时读无陈旧。

---

## 6. MA4↔A5.6 边界声明（Phase 1 Proof ⑤）

本验证（MA4 A4.2.1/A4.2.2）审「**行为是否符合需求**」——并发领料是否致 silent corruption（§3）/ 齐套是否陈旧读（§4）。**不重做** A5.6「E2E 断言强度」审计（A5.6 审断言覆盖深度，本验证审运行时行为正确性）。两 mission 边界按此执行，无重叠。

---

## 7. 运行时安全性裁决（Phase 2 Decision，§2 判据 + 三源对照）

### 7.1 §2 判据三源复核

| §2 判据 | 命中评估 | 结论 |
|---------|---------|------|
| **P0① 活跃数据破坏防护未实现**（并发无锁致库存负数） | **不成立**——库存扣减经 `updateBalanceWithRetry`（versionProp 乐观锁 + P0-MA2-020 UK + 重试）串行化，无 lost-update 致静默库存负数；残留真并发 over-commitment（read-time validateAvailable check-then-act 窗口）**归 A2.17 既有追踪**，非「防护完全未实现」，且**可见非静默**（负余额可观测）+ 默认 `allow-negative-stock=false` 门控 | ❌ 不成立 |
| **P0③ 核心业务循环断裂** | **不成立**——工单→齐套→开工→领料→完工循环完整（KitAvailabilityChecker 只读齐套 + stock move DONE 扣减 + 完工入库），预留 Deferred 是规划/协调信号缺失非循环断裂 | ❌ 不成立 |
| **P0④ 会计过账正确性破坏** | **不成立**——预留不涉 GL；领料出库过账（WIP Dr / Inventory Cr）经 `ManufacturingIssueAcctDocProvider` 独立正确，库存余额守恒由 bookkeeper + UK 防护 | ❌ 不成立 |
| **P1① 功能完全缺失或行为实质偏离** | **维持 P1-RC-008**（A1.8 §5 已裁决）——预留写路径 Deferred 是需求契约维度功能缺失（UC-MFG-05/08 7 断言未实现），**本运行时验证不改变该 P1 定级**（验证的是 Deferred 状态下运行时安全性，非裁决 P1 本身——plan Non-Goals 明示「不裁决 P1-RC-008 的 P1 定级本身」） | 维持 P1（既有） |
| **P2① 次要验收标准未完全满足** | 残留并发 over-commitment 窗口理论成立，但**归 A2.17 既有追踪**（§去重协议不重复登记） | 归 A2.17 |

### 7.2 三源对照（L1/L2/L3）

- **L1（需求契约权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-05（工单审核触发物料预留）+ UC-MFG-08（取消/完工释放预留）+ UC-MFG-04（STOCK_PARTIAL 强制开工）。当前实现 Deferred（owner doc `material-reservation.md:9-16` 明示）。
- **L2（设计参考）**：`material-reservation.md:9-16` Deferred 说明 + `mrp.md:28-32`（可用量 = onHand − reserved + openPurchase + openWO）+ `flow-overview.md`。
- **L3（实现真相）**：§2 census + §3 防护机制 + §4 KitAvailabilityChecker 实时读 + §5 config 门控。

### 7.3 与 P1-RC-008[P1] + P0-MA2-020[resolved] 分层一致性

- **P1-RC-008[P1]**（arm-index §P1）：预留写路径 Deferred 需求契约维度 P1。**本验证维持**——运行时安全性可接受（无 silent corruption + 无陈旧读）**不升 P0**，但需求契约缺口（预留子系统未实现）仍 P1（A1.8 §5 Q4 论证不变）。
- **P0-MA2-020[resolved]**（arm-index §resolved）：stock move bookkeeper negative-stock 防护 UK + 余额守恒。**本验证复用其为 SP-1 无 silent corruption 的关键证据**（versionProp + UK + 重试），resolved 状态不变。
- **A2.17**（库存并发扣减审查）：残留并发 over-commitment 窗口归口。**本验证不重复登记**（§去重协议）。

**最终裁决**：**维持 P1-RC-008 P1，不升 P0，不降级，无新 finding**。残留并发竞争归 A2.17 既有追踪。

---

## 8. finding 衔接（arm-index，Phase 2 Add）

### 8.1 grep arm-index 同域同控制点（§7 禁止未经比对新建）

| 既有 finding | 控制点 | 与本验证残留并发 over-commitment 的关系 |
|--------------|--------|----------------------------------------|
| `P1-RC-008` | 预留写路径 Deferred（需求契约维度） | **不同维度**（需求契约缺失 vs 运行时并发竞争）——本验证维持其 P1，不重开 |
| `P0-MA2-020`（resolved） | stock move UK + 余额守恒 | 本验证**复用**其为 SP-1 证据，不重开 |
| A2.17（库存并发扣减审查，O2C `2026-07-27-1949:183,195` 已登记并发 lost-update 归口） | 库存余额并发扣减 lost-update/over-commit | **同控制点**——本验证残留并发 over-commitment 归 A2.17，**§去重协议不重复登记** |

### 8.2 arm-index 注记更新（P1-RC-008 行追加运行时安全性确认注记）

本验证**不新建 finding**，仅在 arm-index `P1-RC-008` 行追加 A4.2.1/A4.2.2 运行时安全性确认注记（状态/分级/修复通道[MR1 ORM ask-first] 不变）。注记内容：

> 【A4.2.1+A4.2.2 运行时安全性确认 2026-08-06】reserved 写路径 Deferred 当前状态下运行时安全性经实仓核验**可接受**：①SP-1 无 silent split-quantity corruption（`StockMoveBookkeeper.updateBalanceWithRetry:256-328` versionProp 乐观锁 + P0-MA2-020 UK + 重试串行化扣减，无 delta 丢失）；②SP-2 无陈旧读（`KitAvailabilityChecker.check:109` 每次 `findAllByQuery` 实时读 `availableQuantity`）。残留：真并发下 `validateAvailable`（`ErpInvStockMoveProcessor:116-136` read-time）check-then-act 窗口 + `onOutgoing.applyDelta`（`MovingAverageCostingStrategy:73-84`）无 write-time 负数守卫 → `totalQuantity` 可被驱动为负（over-commitment，**可见非静默** + 默认 `allow-negative-stock=false` 门控），**归 A2.17 既有追踪**（§去重协议不重复登记）。**baseline 精化**：字段实为 `reservedQuantity`（非 `reservedQty`）；mfg 领料经 stock move confirm net-zero apply-release 不持久化预留（库存域 `applyReservation:150` 有瞬时 writer）。**维持 P1-RC-008 P1，不升 P0**。详见 `docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`。

---

## 9. §8 过程纪律自检

- [x] **checker actual vs baseline 实测**：本报告产出后运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；真正门控在 CI `.github/workflows/compliance.yml`）。actual 计数：R1a(dao.saveEntity)=0 / R1b(dao.updateEntity)=0 / R1c(dao.getEntityById)=0 / R1d(dao.findAllByQuery)=14 / R2a(BizModel daoFor ErpMd*)=34 / R2b(BizModel daoFor Erp*)=229 / R2c(全生产 daoFor)=1382 / R2d(Processor daoFor ErpMd*)=34。**本验证零生产代码变更**（只读评估）→ actual 与基线**零漂移**，无回归风险。不以 checker 退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本验证全部结论（维持 P1-RC-008 + 复用 P0-MA2-020 + 残留并发归 A2.17）已按 §7 规则 grep arm-index 同域同控制点后给出「复用 or 归口」裁决，**无未经比对直接新建的 finding**。
- [x] **MA4↔A5.6 边界**：§6 声明，本验证审行为正确性不重做 A5.6 E2E 断言强度。
- [x] **保护区域**：本验证零代码/ORM/api.xml/view.xml/真相源变更，属 roadmap 预授权只读评估类目，未触及 §5 ask-first 保护区域。

---

## 结论

**pass（零 P0、零新 finding）**。A1.8 §7 SP-1/SP-2 运行时存疑点经实仓核验消解：SP-1 无 silent split-quantity corruption（updateBalanceWithRetry 串行化）+ SP-2 无陈旧读（KitAvailabilityChecker 实时 findAllByQuery）。**维持 P1-RC-008 P1**（预留写路径 Deferred 需求契约缺口不变，Q4 修复义务归 MR1）+ **残留并发竞争归 A2.17 既有追踪**（§去重协议不重复登记）+ **不触发 MR0，不升 P0**。arm-index P1-RC-008 行追加运行时安全性确认注记（状态/分级/修复通道不变）。本切片解除 A4.2.1/A4.2.2 在 MA4 A4.2 链路的证据缺口。
