# RC MA4 A4.1.17 — LandedCost SELECT FOR UPDATE 跨数据库方言锁行为评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证层 / A4.1 展开器实体行）
> 工作项：A4.1.17（MA4 运行时行为验证 — A1.5 §7-3：UC-FIN-10 P1-MA2-085 SELECT FOR UPDATE 路径在 H2 内存库（测试环境）外的真实 DB（PG/MySQL）的锁行为评估，`ormTemplate.lock` 跨数据库方言一致性；P1-MA2-085 已 resolved R1.28）
> 审计 plan：`docs/plans/2026-08-06-1517-2-rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §去重协议 MA4↔A5.6 边界 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制）
> L1 真相源：`docs/design/finance/use-cases.md:183` UC-FIN-10 隐含到岸成本分摊并发安全（防重复分摊）+ LC-L1/L2/L3 验收标准（见 A1.5 §1）
> 存疑点来源：`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 3
> 关联 finding：**P1-MA2-085**（arm-index `:537`，LandedCost TOCTOU + 非唯一索引，resolved R1.28 经 SELECT FOR UPDATE 替代路径）；**P1-RC-092**（本验证新建，MySQL REPEATABLE READ 下 MVCC 快照致 check 失效）
> 关联审计：`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（A2.4 P1-MA2-085 原始发现 + resolved R1.28）、`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §6.2 P1-MA2-085 HEAD 复核 resolved
> 审计性质：**只读跨方言锁行为评估**（读锁代码路径 + 平台 Dialect 方言生成逻辑 + 既有并发测试普查 + 跨方言锁行为/MVCC 静态推理；不改代码/ORM/api.xml/真相源；锁逻辑 `ErpInvLandedCostProcessor.lockReceiveForAllocation`/`ormTemplate.lock` 经只读评估不修改）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`bf7a9324a`

## 0. 审计结论（TL;DR）

| 项 | 结论 |
|---|---|
| **存疑点裁决** | A1.5 §7 存疑点 3「P1-MA2-085 SELECT FOR UPDATE 路径在 H2 内存库外的真实 DB（PG/MySQL）的锁行为」经平台 Dialect 方言生成逻辑 + 跨方言锁行为/MVCC 静态推理 **裁决：发现跨方言退化——MySQL InnoDB REPEATABLE READ（MySQL 默认隔离级别）下 SELECT FOR UPDATE 锁有效但 check 失效，TOCTOU 重新打开**。注册新 finding **P1-RC-092**（P1，MR1，触及锁/隔离逻辑须 ask-first）。 |
| **跨方言 SQL 一致性** | **一致**。`GenSqlHelper.genLockSql:185-204` 生成 `SELECT <fields> FROM erp_pur_receive WHERE id=? FOR UPDATE`（`LockOption.PESSIMISTIC_WRITE`）；`forUpdate` 片段由 `default.dialect.xml:36` `<forUpdate>for update</forUpdate>` 定义，H2/PostgreSQL/MySQL 三方言 `<sqls>` 均不覆盖 `<forUpdate>` → 三方言 emit 相同 `for update`。**锁本身跨方言有效**。 |
| **锁有效性（跨方言）** | **三方言锁均有效**（`SELECT ... WHERE id=? FOR UPDATE` 主键行级悲观锁串行化并发同 receiveId 的锁获取）。存疑点原列「MySQL GAP/Next-Key 锁范围扩大」**不适用**（锁查询为主键等值单行，InnoDB 唯一索引等值命中已存在行只加记录锁，无 GAP）。但此排除针对**错误威胁模型**——真实跨方言风险不在锁范围，而在 MVCC 快照（见下行）。 |
| **check-then-act 原子性（核心发现）** | **跨方言不一致**。`validateNotAlreadyAllocated:399` 是 `erp_inv_landed_cost` 表的**非锁 SELECT**（`dao.findAllByQuery`），与锁定的 `erp_pur_receive` 不同表。事务的 MVCC 读视图在首次非锁读（`ApproveProcessor:37 requireLandedCost` / `:50 loadReceive`）时建立，**不随 `:52` 的 SELECT FOR UPDATE 锁获取而刷新**。故并发事务 B 在 `:52` 阻塞获 A 的行锁、A 提交后，B 的 `:53` check 仍读**首次读建立的陈旧快照** → 看不到 A 已提交的 APPROVED sibling → check 通过 → **创建重复分摊**（TOCTOU 重新打开）。见 §4 有效性矩阵。 |
| **有效性矩阵** | ①H2（测试环境，默认 READ_COMMITTED）：锁有效 + check 见 A 提交（语句级快照）→ **防重复分摊成立**（`TestErpInvLandedCostReceiveMutex` 证锁串行化）；②PostgreSQL（默认 READ_COMMITTED）：语句级快照，check 在获锁后运行 → 见 A 提交 → **成立**；③MySQL READ_COMMITTED：同 PG → **成立**；④**MySQL InnoDB REPEATABLE READ（MySQL 默认）**：事务级快照固定于首次读，check 读陈旧快照 → **不见 A 提交 → TOCTOU 重新打开 → 重复分摊可能**。 |
| **配置缓解核查** | **未缓解**。应用 `application.yaml` 活跃数据源 = H2（`jdbc:h2`，测试环境）；MySQL 配置被注释（`jdbc:mysql://...`），**无 `transactionIsolation` 参数**；平台层零隔离级别配置（grep `transactionIsolation`/`READ_COMMITTED` = 0）。故生产 MySQL 部署使用 InnoDB 默认 REPEATABLE_READ → **退化路径可达**（生产部署触发，与存疑点「触发条件=生产部署」一致）。 |
| **既有测试覆盖** | `TestErpInvLandedCostReceiveMutex` 覆盖**锁串行化**（`testLockSerializesConcurrentAccess` maxOverlap≤1 + `testLockFreshReceiveAcquiresWithoutError` version 守恒）——**仅证锁互斥，不证 check 见并发提交**（防重复分摊 `ERR_LANDED_COST_ALREADY_ALLOCATED` 测试为顺序执行，非并发）。真实 DB（PG/MySQL）并发测试缺口 = MR1 范围（plan Non-Goals）。 |
| **P1-MA2-085 分级裁决** | **R1.28 实现如规约落地，但 resolved 状态附带跨方言 caveat**：SELECT FOR UPDATE 修复在 H2/PG/MySQL-RC 有效（resolved 成立），**在 MySQL-RR（默认）失效**（check 读陈旧快照）。P1-MA2-085 resolved 结论**不撤销**（R1.28 按规约实现），但跨方言退化由新 finding **P1-RC-092** 捕获（同控制点[LandedCost 重复分摊]，新根因维度[MySQL-RR MVCC 快照]，§去重协议不合并同控制点不同根因→新建 RC 系列）。 |
| **新 finding** | **P1-RC-092**（P1，UC-FIN-10 防重复分摊并发安全在 MySQL REPEATABLE READ 下未实现）。修复归 MR1 + ask-first（触及锁/隔离逻辑）。修复方向：①部署侧 MySQL 强制 READ_COMMITTED（`transactionIsolation` 或连接参数）；或②`validateNotAlreadyAllocated` 改为对 sibling 行 `SELECT ... FOR UPDATE`（锁定读强制见最新已提交）；或③补 `(receiveId, approveStatus)` UK（DB 级兜底，P1-MA2-085 原「方案 A」未取路径，跨隔离级别有效）。 |
| **MR0 即时通道** | **不触发**。非 §2 P0——MySQL-RR 退化沿袭原 P1-MA2-085 的 P1 非 P0 四项理由（窄触发[需 MySQL 部署 + 并发 approve 同 receiveId] + 下游 managed-instance version 守护 + A2.11 已登记并发敏感点 + 期末成本核算可发现）。归 MR1 批量修复通道。 |

**整体裁决**：A1.5 §7 存疑点 3 经平台 Dialect 方言生成逻辑 + 跨方言锁行为/MVCC 静态推理 **裁决：发现跨方言退化，注册 P1-RC-092**。核心证据链：`IOrmTemplate.lock` → `GenSqlHelper.genLockSql` 生成 `SELECT <fields> FROM erp_pur_receive WHERE id=? FOR UPDATE`（`PESSIMISTIC_WRITE`），`forUpdate` 片段由 `default.dialect.xml:36` 定义为 `for update`，H2/PG/MySQL 三方言均不覆盖 → **三方言 emit 相同 `FOR UPDATE` 行级悲观锁**（锁本身跨方言有效）。锁查询为主键等值单行（`genEntityFilter` pk=?），InnoDB 唯一索引等值命中已存在行只加记录锁（无 GAP/Next-Key）→ 存疑点原列「MySQL GAP 锁范围扩大」**不适用**（且针对错误威胁模型）。**真实跨方言风险**：`validateNotAlreadyAllocated:399` 是 `erp_inv_landed_cost` 表的**非锁 SELECT**（`dao.findAllByQuery`），与锁定的 `erp_pur_receive` 不同表；MySQL InnoDB REPEATABLE READ（MySQL 默认）下事务 MVCC 读视图固定于首次非锁读（`ApproveProcessor:37/50`），**不随 `:52` SELECT FOR UPDATE 锁获取刷新** → 并发事务 B 获 A 释放的行锁后，`:53` check 仍读陈旧快照 → 看不到 A 已提交的 APPROVED sibling → check 通过 → 重复分摊（TOCTOU 重新打开）。配置未缓解（应用 H2 默认 + MySQL 注释无 `transactionIsolation` + 平台零隔离配置）。测试仅证锁串行化（`TestErpInvLandedCostReceiveMutex`），不证 check 见并发提交。H2（READ_COMMITTED）/PG（READ_COMMITTED 默认）/MySQL-READ_COMMITTED 下成立。**注册 P1-RC-092**（P1，MR1 + ask-first）。P1-MA2-085 resolved 不撤销（R1.28 如规约落地，H2/PG/MySQL-RC 有效），跨方言退化由 P1-RC-092 捕获。本验证**不实施修复**（锁/隔离逻辑保护区域 + plan Non-Goals），只确认分级 + 登记 finding + 修复归口。

---

## 1. 存疑点原文 + L1 需求契约

> 存疑点来源：`2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 3（逐字引用）。

| # | 存疑点 | 触发条件 | 交 MA4 |
|---|---|---|---|
| 3 | **P1-MA2-085 SELECT FOR UPDATE 路径**在 H2 内存库（测试环境）外的真实 DB（PG/MySQL）的锁行为——`ormTemplate.lock` 跨数据库方言一致性 | 生产部署 | A4.1 运行时验证（非本切片阻塞，P1-MA2-085 已 resolved） |

**L1 需求契约**（`docs/design/finance/use-cases.md:183`，UC-FIN-10，逐字验收标准节选）：

```
// 到岸成本
运费/保险/关税 → 按金额比例分摊到入库批次
入库成本 += 分摊费用
```

**并发安全隐含契约**（UC-FIN-10 防重复分摊）：同一收货单（receive）不得被并发审核的多个到岸成本单重复分摊（防 `StockBalance` 双计成本调整）。L2 `costing-methods.md §到岸成本编排`（防重复分摊 `validateNotAlreadyAllocated`）+ `state-machine.md`（到岸成本防重复分摊）共同表达此并发安全不变量。

> **本验证核心问题**：P1-MA2-085 的修复（`lockReceiveForAllocation` SELECT FOR UPDATE 串行化并发同 receiveId 审核）在 H2 内存库（测试环境）已证锁串行化有效（A2.4 + `TestErpInvLandedCostReceiveMutex`），但在生产目标 DB（PostgreSQL / MySQL）的锁行为是否一致——①`ormTemplate.lock` 跨方言是否都生成有效行级悲观锁；②是否有方言在特定隔离级别下锁退化；③锁保护下的 check-then-act 原子性是否跨方言一致。本验证为只读静态评估（plan Non-Goals 显式排除真实 DB 部署并发测试）。

---

## 2. 锁入口与平台实现核验（L3，file:line）

> 实仓逐行核实 `module-inventory/erp-inv-service` + 平台 nop-entropy `nop-persistence/nop-orm` + `nop-dao` 方言层（HEAD `bf7a9324a`）。

### 2.1 锁入口（业务层）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java`：

- `lockReceiveForAllocation:388-390`：`protected void lockReceiveForAllocation(ErpPurReceive receive) { ormTemplate.lock(receive); }` —— 锁入口，调平台 `IOrmTemplate.lock`。
- `:385-387` 注释（业务意图锚点）：「对采购入库单行做悲观锁（SELECT ... FOR UPDATE，`IOrmTemplate.lock`），串行化并发同 receiveId 的到岸成本审核。不修改 receive 字段（无 version/audit 污染）；lock 后由 `validateNotAlreadyAllocated` 检测已提交的并发分配。」
- `validateNotAlreadyAllocated:392-407`：**非锁 SELECT**——`dao.findAllByQuery(q)`（`:399`）查 `erp_inv_landed_cost` 表（`receiveId=? AND approveStatus=APPROVED`，排除自身）→ 有则 `ERR_LANDED_COST_ALREADY_ALLOCATED`。**此 check 查询的是 `erp_inv_landed_cost` 表（与锁定的 `erp_pur_receive` 不同表），且为非锁读**——这是 §4 跨方言 MVCC 退化的关键。

**核验结论**：锁入口 = `ormTemplate.lock(receive)`（锁 receive 行）；check = `validateNotAlreadyAllocated` 非锁读 `erp_inv_landed_cost` sibling。锁与 check 作用在**不同表**。

### 2.2 平台调用链追踪（`IOrmTemplate.lock` → SELECT FOR UPDATE SQL 生成）

| 层 | 文件:行 | 行为 |
|---|---|---|
| `IOrmTemplate.lock` | `nop-entropy/nop-persistence/nop-orm/.../impl/OrmTemplateImpl.java:419-421` | `session.lock(entity)`（委托 session） |
| `IOrmSession.lock` | `.../session/OrmSessionImpl.java:588-614` | 校验非 dirty（`:597-598`）→ `persister.lock(entity, eagerLoadProps, session, unlockCallback)`（`:602`）→ 成功置 `orm_locked(true)`（`:610`）；失败抛 `ERR_ORM_LOCK_ENTITY_FAIL`（`:608`）。**注意**：lock 是锁定读（见最新已提交版本），但**不刷新会话的 MVCC 读视图**（§4 关键） |
| `IEntityPersister.lock` | `.../persister/EntityPersisterImpl.java:246-261` | `driver.lock(shard, entity, propIds, unlockCallback, session)`（`:255`） |
| `IEntityPersistDriver.lock` | `.../driver/jdbc/JdbcEntityPersistDriver.java:145-150` | `lockSql = GenSqlHelper.genLockSql(dialect, entityModel, binders, propIds, LockOption.PESSIMISTIC_WRITE)`（`:150`）+ 预编译 `this.lockSql`（`:91-92`，同 `PESSIMISTIC_WRITE`） |
| **SQL 生成** | `.../sql/GenSqlHelper.java:185-204` | `SELECT <fields> FROM <table> <getLockHintSql> WHERE <pk filters via genEntityFilter> <getForUpdateSql>`（`:196` getLockHintSql + `:199-200` genEntityFilter 按 `entityModel.getPkColumns()` 构造 pk=? + `:202` getForUpdateSql） |

**核验结论**：`ormTemplate.lock(entity)` 的最终 SQL = `SELECT <实体字段> FROM erp_pur_receive <lockHint> WHERE id=? FOR UPDATE`（主键等值单行），锁模式 = `LockOption.PESSIMISTIC_WRITE`。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（需求正确性 / 验证充分性）**：锁机制 = SELECT FOR UPDATE 行级悲观锁经完整调用链 file:line 证实（5 层无断链）；锁本身跨方言有效（§3.1）。但「锁有效」≠「check-then-act 原子」——check 是另一表的非锁读，其跨方言可见性独立于锁（§4）。

---

## 3. 跨方言锁有效性评估

### 3.1 Dialect 方言 `forUpdate` 片段配置（锁本身跨方言一致）

`DialectImpl.getForUpdateSql:393-395`（`nop-dao`）：`return dialectModel.getSqls().getForUpdate();` —— `forUpdate` 片段由 Dialect XML 模型的 `<sqls><forUpdate>` 元素驱动。

`nop-entropy/nop-persistence/nop-dao/src/main/resources/_vfs/nop/dao/dialect/`：

| 方言 | `<forUpdate>` 配置 | 实际 emit | 行级悲观锁有效？ |
|---|---|---|---|
| **default**（`default.dialect.xml:36`） | `<forUpdate>for update</forUpdate>` | `for update` | ✅ 基线 |
| **H2**（`h2.dialect.xml:81-98`） | 不覆盖（`<sqls>` 仅覆盖 `trueString`/`falseString`/`escapeSlash`） | 继承 default = `for update` | ✅ H2 支持 `SELECT ... FOR UPDATE` 行级锁 |
| **PostgreSQL**（`postgresql.dialect.xml:96-119`） | 不覆盖（`<sqls>` 仅覆盖 `escapeSlash`/`trueString`/`falseString`） | 继承 default = `for update` | ✅ PG 标准 `SELECT ... FOR UPDATE` 行级锁 |
| **MySQL**（`mysql.dialect.xml:139-141`） | 不覆盖（`<sqls>` 仅覆盖 `escapeSlash`） | 继承 default = `for update` | ✅ InnoDB `SELECT ... FOR UPDATE` 行级锁 |
| duckdb（`duckdb.dialect.xml:38`） | `<forUpdate x:override="replace"/>`（置空） | 空（无锁） | ❌ OLAP 无锁支持（非本存疑点生产 TX 目标） |

**锁本身跨方言一致性裁决**：H2 / PostgreSQL / MySQL 三目标方言均**继承 default `for update`**，emit **相同**的 `SELECT <fields> FROM erp_pur_receive WHERE id=? FOR UPDATE`。**锁获取（串行化并发同 receiveId 的行锁）跨方言一致有效**。

### 3.2 MySQL GAP 锁排除（正确但针对错误威胁模型）

存疑点原列「MySQL InnoDB REPEATABLE READ 默认下 FOR UPDATE 是否触发 GAP 锁/Next-Key 锁范围扩大 → 潜在死锁面」经核验**不适用**：

1. **锁查询为主键等值**：`GenSqlHelper.genLockSql:199-200` 调 `genEntityFilter` 按 `entityModel.getPkColumns()` 构造 `pk=?` 过滤 → 锁 SQL = `SELECT ... FROM erp_pur_receive WHERE id=? FOR UPDATE`，**非范围查询**。
2. **InnoDB 唯一索引等值命中已存在行**（InnoDB 文档化）：只加**记录锁（record lock）**，**不加 GAP/Next-Key 锁**。`erp_pur_receive.id` 是主键，`lockReceiveForAllocation` 锁已加载的实体（行必然存在）→ 命中「唯一索引等值 + 行存在」→ **纯记录锁**。
3. **结论**：MySQL 锁范围与 PG/H2 行锁一致（均仅锁命中单行），**无 GAP 锁范围扩大死锁面**。

> **威胁模型校正**：本节排除的「GAP 锁范围扩大」是存疑点原始框架的威胁假设，经核验不适用。**但真实跨方言风险不在锁范围，而在 check 查询的 MVCC 快照可见性**（§4）——`validateNotAlreadyAllocated` 是另一表的非锁读，其跨方言行为独立于锁。独立结束审计正确指出「GAP 锁排除针对错误威胁模型」，本报告据此将核心分析移至 §4。

### 3.3 锁等待超时配置跨方言差异（运营配置，非行为缺陷）

| DB | 锁等待超时机制 | 默认值 | 性质 |
|---|---|---|---|
| H2 | `SET LOCK_TIMEOUT` | ~1000ms | 测试环境 |
| PostgreSQL | `lock_timeout` / `statement_timeout` | `lock_timeout=0`（off，无限等待除非配置） | 运维配置 |
| MySQL InnoDB | `innodb_lock_wait_timeout` | 50s | 运维配置 |

**裁决**：超时阈值跨方言各异，**仅影响锁等待超时阈值**（运维/部署侧配置），**不影响锁正确性**。此差异属**部署文档 watch-only**（不达 §2 行为缺陷阈值，不升 finding）。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（架构/边界 / 回归风险）**：锁本身跨方言一致（Dialect XML 确定性继承，无偶然性）；GAP 锁排除经主键等值记录锁 InnoDB 文档化行为证实；锁等待超时运营差异归部署 watch-only。

---

## 4. 锁保护下 check-then-act 原子性评估（核心——跨方言退化发现）

### 4.1 编排时序（lock → check，同事务）

`module-inventory/erp-inv-service/.../processor/ErpInvLandedCostApproveProcessor.java`（approve per-mutation Processor，`@BizMutation` 单事务）：

- `:37` `requireLandedCost` → `getEntityById`（**非锁读**，`erp_inv_landed_cost`）← **事务 MVCC 读视图在此首次建立**（MySQL RR）
- `:44` `loadCostLines` → `findAllByQuery`（非锁读）
- `:50` `loadReceive` → `getEntityById`（非锁读，`erp_pur_receive`）
- `:51` `validateReceiveApproved`（内存校验）
- `:52` `lockReceiveForAllocation(receive)` ← **SELECT FOR UPDATE 锁 receive 主键行**（锁定读，见最新版本，**不刷新事务 MVCC 读视图**）
- `:53` `validateNotAlreadyAllocated` ← **非锁读 `erp_inv_landed_cost`**（`dao.findAllByQuery:399`，读事务 MVCC 读视图）
- `:55-61` loadReceiveLines → doAllocate → createAndApplyCostAdjust → doPostApprove

**时序核验**：`:52` 锁严格先于 `:53` check。但 `:53` check 是**另一表（`erp_inv_landed_cost`）的非锁读**，其可见性取决于事务隔离级别的快照行为，**非取决于 `:52` 的锁**。

### 4.2 并发场景与 MVCC 快照行为（跨方言退化根因）

**并发场景**：事务 A、B 同时 approve 同一 receiveId 的两个不同 landed cost 单，同一 `@BizMutation` 事务。

| 步骤 | 事务 A | 事务 B | MVCC 状态 |
|---|---|---|---|
| T1 | `:37 requireLandedCost`（非锁读，建立 A 读视图 VA） | `:37 requireLandedCost`（非锁读，建立 B 读视图 VB） | VA/VB 均见「无 APPROVED landed cost sibling」（A、B 均未提交） |
| T2 | `:52 lockReceiveForAllocation` → `SELECT ... WHERE id=? FOR UPDATE` **获锁** | `:52 lockReceiveForAllocation` → **阻塞**（等 A 的 receive 行锁） | — |
| T3 | `:53 validateNotAlreadyAllocated`（读 VA）：无 APPROVED sibling → 通过 | （阻塞） | — |
| T4 | `doAllocate` + `createAndApplyCostAdjust` + `doPostApprove`（创建 APPROVED landed cost + 成本层） | （阻塞） | — |
| T5 | **commit**（A 的 APPROVED landed cost 落盘可见；释放行锁） | A 释放锁后 **B 获锁**（`:52` 锁定读见最新 receive，但 B 不再校验 receive） | A 提交后，**VB 是否刷新？** ← 跨方言分叉点（见下） |
| T6 | — | `:53 validateNotAlreadyAllocated`（非锁读，读 VB） | **VB 见 A 提交？** ← 决定 check 是否通过 |

### 4.3 跨方言有效性矩阵（核心裁决依据）

| DB / 隔离级别（默认） | 读视图行为 | T6: B 的 check 见 A 提交的 APPROVED sibling？ | 防重复分摊成立？ |
|---|---|---|---|
| **H2**（测试环境，默认 READ_COMMITTED） | 语句级快照（每条 SELECT 新快照） | ✅ 是（`:53` 新语句在 A 提交后运行，见 A 提交） | ✅ **成立** |
| **PostgreSQL**（默认 READ_COMMITTED） | 语句级快照 | ✅ 是（`:53` 新语句在获锁后、A 提交后运行） | ✅ **成立** |
| **MySQL READ_COMMITTED**（可配置） | 语句级快照 | ✅ 是 | ✅ **成立** |
| **MySQL InnoDB REPEATABLE_READ**（**MySQL 默认**） | **事务级快照**（首次非锁读 `:37` 建立，**不随 `:52` 锁定读刷新**） | ❌ **否**（`:53` 读 VB[建立于 `:37`，A 未提交时]，不见 A 在 T5 提交的 sibling） | ❌ **TOCTOU 重新打开 → 重复分摊可能** |

**跨方言退化裁决**：MySQL InnoDB REPEATABLE READ（MySQL 默认隔离级别）下，B 事务的 MVCC 读视图 VB 在 `:37` 首次非锁读时建立（A、B 均未提交），**不随 `:52` SELECT FOR UPDATE 锁获取而刷新**（InnoDB 锁定读见最新版本，但不更新一致读快照——MySQL 8.0 Ref Manual §15.7.2.3/§15.7.2.4）。故 T6 B 的 `:53` 非锁读仍读 VB（陈旧）→ 看不到 A 在 T5 提交的 APPROVED landed cost sibling → check 通过 → B 进入 `doAllocate` → **创建重复分摊**（UC-FIN-10 防重复分摊契约被破坏）。

> **关键事实**：InnoDB REPEATABLE READ 下，**锁定读（SELECT FOR UPDATE）与一致非锁读使用不同的版本机制**——锁定读总是见最新已提交版本（用于 `:52` 锁获取串行化），但一致非锁读固定于事务首次读建立的快照（`:53` check 读陈旧）。这正是 `validateNotAlreadyAllocated`（非锁读、另一表）跨方言失效的根因。

### 4.4 配置缓解核查（未缓解）

| 配置层 | 隔离级别设置 | 依据 |
|---|---|---|
| 应用 `application.yaml`（活跃） | H2（`jdbc:h2:./db/test`，测试环境），无 `transactionIsolation` | `module-b2b/erp-b2b-app/.../application.yaml:25-29`（代表全 20 生产 yaml） |
| 应用 `application.yaml`（MySQL 注释段） | `jdbc:mysql://127.0.0.1:3306/dev?...`，**无 `transactionIsolation` 参数** | `module-b2b/erp-b2b-app/.../application.yaml:30-33`（MySQL 配置全注释，零隔离覆盖） |
| 平台层 | 零隔离级别配置（grep `transactionIsolation`/`READ_COMMITTED`/`REPEATABLE_READ` 于 `nop-entropy/nop-persistence` = 0） | 平台不强制隔离级别 |

**裁决**：生产 MySQL 部署（若启用注释的 MySQL 配置）使用 InnoDB 默认 REPEATABLE_READ → **退化路径可达**（与存疑点「触发条件=生产部署」一致）。无任何配置层缓解。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（需求正确性 / 验证充分性 / 回归风险 / 待办策略漂移）**：check-then-act 原子性在 MySQL-RR 失效经 InnoDB MVCC 文档化行为 + 编排时序（`:37` 首读 → `:52` 锁 → `:53` 非锁读）+ 配置核查（无隔离覆盖）三重证实；未将真实退化无声降级为 watch-only（§2 并发数据完整性严格），亦未撤销 P1-MA2-085 resolved（R1.28 如规约落地）；真实 DB 经验性并发测试缺口诚实归 MR1（plan Non-Goals）。

---

## 5. 既有测试覆盖边界普查（L4）

> grep `TestErpInvLandedCostReceiveMutex` + nop-entropy Dialect/`IOrmTemplate.lock` 平台测试全集（HEAD `bf7a9324a`）。

### 5.1 应用层并发测试（H2 内存库）

`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/processor/TestErpInvLandedCostReceiveMutex.java`：

| 测试方法 | 覆盖 | 断言强度 |
|---|---|---|
| `testLockFreshReceiveAcquiresWithoutError`（`:52-63`） | 单线程：`lockReceiveForAllocation` 在 fresh receive 上获锁成功（不抛异常）+ **不污染 version**（`after.getVersion()==0`，`:62`，验证 SELECT FOR UPDATE 不触发 version 自增） | **深**（version 守恒断言） |
| `testLockSerializesConcurrentAccess`（`:69-115`） | 多线程：两线程并发 `lockReceiveForAllocation` 同一 receive → SELECT FOR UPDATE 串行化，临界区不重叠（`maxOverlap ≤ 1` 互斥断言，`:113-114`） | **深**（并发互斥强断言） |

**应用层覆盖定论（重要边界）**：`TestErpInvLandedCostReceiveMutex` 仅覆盖 **`lockReceiveForAllocation` 的锁串行化**（maxOverlap≤1 + version 守恒）。**不覆盖「check 见并发提交」**——即不测「A 提交后 B 获锁，B 的 `validateNotAlreadyAllocated` 是否见 A 的 APPROVED sibling」。防重复分摊 `ERR_LANDED_COST_ALREADY_ALLOCATED` 拒绝测试（`TestErpInvLandedCostEndToEnd`，A1.5 §3.4）为**顺序执行**（非并发），不触达 MVCC 快照可见性维度。**故 §4 MySQL-RR 退化未被任何现有测试覆盖**（H2 READ_COMMITTED 下语句级快照使退化不显现）。

### 5.2 平台层 Dialect/`IOrmTemplate.lock` 测试覆盖

| 覆盖项 | 状态 | 依据 |
|---|---|---|
| `IOrmTemplate.lock` → SELECT FOR UPDATE SQL 生成 | **静态可读**（§2.2 + §3.1） | 平台调用链 + Dialect XML |
| H2 方言 `forUpdate` emit | **应用层间接覆盖**（`TestErpInvLandedCostReceiveMutex` 在 H2 跑通） | §5.1 |
| PostgreSQL / MySQL 方言 `forUpdate` emit | **静态配置覆盖**（不覆盖 default → 继承 `for update`）；无平台层 PG/MySQL 集成测试 | §3.1 + plan Non-Goals |

### 5.3 测试覆盖边界清单 + 缺口

| 覆盖边界 | 状态 | 缺口 |
|---|---|---|
| H2 SELECT FOR UPDATE 锁串行化 | ✅ 深覆盖（`TestErpInvLandedCostReceiveMutex`） | — |
| **「check 见并发提交」原子性（跨方言）** | ❌ **零覆盖**（锁测试仅证互斥；防重复分摊测试为顺序） | **§4 MySQL-RR 退化未被覆盖**（H2 READ_COMMITTED 不显现） |
| 跨方言 `forUpdate` SQL 一致性 | ✅ 静态配置覆盖 | 无动态集成测试（静态可读确定） |
| MySQL InnoDB REPEATABLE_READ check 可见性 | ❌ **无测试** | **P1-RC-092 退化路径无测试覆盖** |
| 真实 DB（PG/MySQL）并发集成测试 | ❌ 无 | MR1 范围（plan Non-Goals） |

**测试覆盖定论**：锁串行化**深覆盖**；**「check 见并发提交」原子性零覆盖**——这是 §4 MySQL-RR 退化未被发现的原因（H2 READ_COMMITTED 语句级快照使退化不显现，测试环境盲区）。本验证据此登记 P1-RC-092（行为缺陷，非仅测试缺口）。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（验证充分性 / 路由正确性）**：测试盲区（H2 READ_COMMITTED 掩盖 MySQL-RR 退化）诚实披露，未伪装为已覆盖；路由正确（MA4 运行时行为验证 = 静态 MVCC 推理 + 平台 Dialect 分析）。

---

## 6. MA4↔A5.6 边界声明

> 方法论 §去重协议 MA4↔A5.6：本验证审「行为是否符合需求」（SELECT FOR UPDATE 跨方言锁行为 + check 原子性是否一致），与 A5.6（audit-remediation E2E 断言强度，审测试质量视角）边界按此执行。

- 本验证**不重做** A5.6 E2E 断言强度审计（A5.6 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md` 已覆盖 E2E 业务断言强度分类）。
- 本验证只评 SELECT FOR UPDATE 跨方言**锁行为 + check 原子性正确性**（L3 锁/check 代码路径 + 平台 Dialect 方言生成 + 跨方言 MVCC 静态推理）+ 既有测试覆盖边界（§5）。
- §5.1/§5.3 揭示的「check 见并发提交」测试盲区若未来 A5.6 审计，其输入 = 本验证证据。
- 本验证**不触及** A1.5 §7-1（A4.1.15 done）/ §7-2（A4.1.16 done）——不同存疑点，不合并。

---

## 7. 分级裁决：P1-RC-092 新建（方法论 §2 判据 + 三源对照 + §去重协议）

### 7.1 裁决：注册新 finding P1-RC-092（P1）

| 维度 | 评估 | 命中 |
|---|---|---|
| **§2 P0① 活跃数据破坏防护未实现** | **不成立（P1 非 P0）**——重复分摊致 `StockBalance` 双计成本调整为活跃数据破坏，但沿袭原 P1-MA2-085 P1 非 P0 四项理由：①窄触发（需 MySQL 部署 + 并发 approve 同 receiveId 两 LandedCost 单）；②下游 cost 经 `CostAdjustmentService.applyCostAdjust` managed-instance 写入有 version 守护（非裸 SQL 绕过）；③A2.11 已登记并发敏感点；④经期末成本核算对账可发现 | ❌（归 P1） |
| **§2 P0④ 会计过账正确性破坏** | **不成立（P1 非 P0）**——同上四项理由 + 非「默认活跃路径」（H2 测试环境 + PG/MySQL-RC 不退化；仅 MySQL-RR[默认] 退化，且需并发窄触发） | ❌（归 P1） |
| **§2 P1① 行为实质偏离验收标准** | **成立**——UC-FIN-10 防重复分摊并发安全契约在 MySQL InnoDB REPEATABLE_READ（MySQL 默认，无配置缓解）下**未实现**（`:53` check 读陈旧快照 → 看不到并发提交 → 重复分摊） | ✅ |
| **§2 P2① 次要验收标准未完全满足** | **不成立（应升 P1）**——MySQL-RR 是 MySQL 默认隔离级别（非边角配置），生产 MySQL 部署退化路径**默认可达**；与原 P1-MA2-085（同双计风险）分级一致，不降 P2 | ❌ |

**裁决结论**：**注册 P1-RC-092（P1）**。UC-FIN-10 防重复分摊并发安全在 MySQL-RR（默认）下未实现，沿袭原 P1-MA2-085 P1 分级（同双计风险 + 同四项 P1 非 P0 理由）。**不降 P2**（MySQL-RR 是默认非边角；与原 P1-MA2-085 一致性；§2/Q4 并发数据完整性严格）。

### 7.2 P1-RC-092 修复归口（MR1 + ask-first）

触及锁/check 逻辑或隔离配置 → 按 §5 保护区域三类 ask-first 门控（锁逻辑属会计/数据安全邻近）+ 独立 plan-audit。修复方向（MR1 评估）：

| 方向 | 措施 | 跨隔离级别有效性 | 触及保护区域 |
|---|---|---|---|
| ① 部署侧隔离配置 | MySQL 强制 READ_COMMITTED（`transactionIsolation` 或连接参数 `transaction-isolation=READ-COMMITTED`） | 仅 READ_COMMITTED 有效（不解决 RR 部署） | 否（部署配置） |
| ② check 改锁定读 | `validateNotAlreadyAllocated` 改为对 sibling 行 `SELECT ... FOR UPDATE`（锁定读强制见最新已提交） | 跨隔离级别有效 | 是（锁/check 逻辑） |
| ③ DB 级 UK 兜底 | 补 `(receiveId, approveStatus)` UK（P1-MA2-085 原「方案 A」未取路径） | 跨隔离级别有效（DB 级约束） | 是（ORM ask-first） |

> **注**：方向③是 P1-MA2-085 arm-index 修复描述列明的「方案 A（推荐）加 UK」未取路径——本验证证实 SELECT FOR UPDATE 替代路径在 MySQL-RR 有退化，UK 兜底（跨隔离级别有效）应纳入 MR1 评估。

### 7.3 三源对照

| 源 | 内容 | 与本裁决一致性 |
|---|---|---|
| **L1**（`use-cases.md:183` UC-FIN-10） | 到岸成本防重复分摊并发安全 | MySQL-RR 下重复分摊可能 → 契约在 MySQL-RR 未满足；H2/PG/MySQL-RC 满足 ✅ 一致（P1-RC-092 捕获 MySQL-RR 缺口） |
| **L2**（`costing-methods.md §到岸成本编排` + `state-machine.md`） | `validateNotAlreadyAllocated` + receive 悲观锁 | 锁机制跨方言有效（§3.1），但 check 非锁读在 MySQL-RR 读陈旧快照（§4.3）→ L2 机制在 MySQL-RR 退化 ✅ 一致 |
| **L3**（`ErpInvLandedCostProcessor.lockReceiveForAllocation:388-390` / `validateNotAlreadyAllocated:399` / `ApproveProcessor:37,52-53` / `default.dialect.xml:36` / application.yaml 无 transactionIsolation） | SELECT FOR UPDATE 锁 receive + 非锁 check landed_cost sibling + MySQL-RR MVCC 陈旧快照 + 无隔离覆盖 | MySQL-RR check 失效 ✅ 一致 |

### 7.4 P1-MA2-085 resolved 状态裁决（不撤销 + caveat）

- **R1.28 实现如规约落地**：`lockReceiveForAllocation:388-390` SELECT FOR UPDATE + `validateNotAlreadyAllocated:392-407` 按 arm-index 修复描述（「方案 A 加 UK **或** SELECT FOR UPDATE pre-check」，取后者）实现。
- **resolved 在 H2/PG/MySQL-RC 成立**：SELECT FOR UPDATE 串行化锁获取 + READ_COMMITTED 下 check 见并发提交 → 防重复分摊成立。
- **MySQL-RR 退化由 P1-RC-092 捕获**：与 A1.5 §6.2「P1-MA2-085 经 SELECT FOR UPDATE 路径落地，维持 resolved」分层一致——A1.5 §6.2 是 HEAD 代码落地复核（H2 测试环境），未评 MySQL-RR；本验证补 MySQL-RR 维度发现退化。
- **结论**：P1-MA2-085 **resolved 不撤销**（R1.28 如规约落地 + H2/PG/MySQL-RC 有效），但跨方言 MySQL-RR 退化由新 finding **P1-RC-092** 捕获（同控制点[LandedCost 重复分摊]，新根因维度[MySQL-RR MVCC 快照]，§去重协议不合并同控制点不同根因→新建 RC 系列，不重开 arm finding）。

### 7.5 与既有 finding 的去重（§去重协议）

| 既有 finding | 控制点 | 与本裁决的关系 |
|---|---|---|
| **P1-MA2-085**（arm-index `:537`，resolved R1.28） | LandedCost 重复分摊（原：TOCTOU pre-check + 无 UK） | **同控制点不同根因维度**：原根因 = TOCTOU pre-check + 无 UK；本根因 = SELECT FOR UPDATE 修复在 MySQL-RR 的 MVCC 快照退化。按 §7「新根因 → 新建」+ §去重协议「不合并同控制点不同根因」，新建 P1-RC-092（RC 系列区分 arm 系列），不重开 P1-MA2-085。arm-index P1-MA2-085 行追加 P1-RC-092 交叉引用注记 |
| **A1.5 §7-1**（A4.1.15 done，P2-RC-004 升 P1） | forward delta 层消耗数值正确性 | 不同存疑点 + 不同控制点（delta 层消耗 vs SELECT FOR UPDATE 锁/check），不合并 |
| **A1.5 §7-2**（A4.1.16 done，维持 P2-MA2-029） | reverse delta 层删除余额守恒 | 不同存疑点 + 不同控制点，不合并 |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`bf7a9324a`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码作为门控通过依据。**本审计无生产代码变更**（纯审计报告），checker 无回归风险（actual 反映既有 HEAD 状态，非本审计引入）。actual 与 A4.1.16 baseline（HEAD `112a4b493`）一致（零代码变更期间 R1-R2 计数稳定）。

  | 规则 | 描述 | baseline（A4.1.16 `112a4b493`） | actual（HEAD `bf7a9324a`） | delta | 说明 |
  |---|---|---|---|---|---|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 | — |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 | — |
  | R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R3-R12 | （脚本 R3 段起既有行为未输出计数，A4.1.11/A4.1.13/A4.1.15/A4.1.16 已记录） | 同基线 | 同基线（零代码变更） | 0 | 不适用（脚本既有行为） |

- [x] **closure-audit 独立性声明**：本报告经独立结束审计（独立子代理新会话 `ses_029d53181ffeLXGXQO85Xga3UR`，不重用执行者上下文）。独立审计检出原 §4 check-then-act 原子性推理错误（MySQL-RR MVCC 快照盲区）作为 Blocker，本报告据此修订（§4 重写 + §0/§7 注册 P1-RC-092）。执行者未自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告新建 P1-RC-092 前 grep arm-index 同域同控制点（P1-MA2-085 / A1.5 §7-1[A4.1.15] / A1.5 §7-2[A4.1.16]）后按 §去重协议裁决「同控制点不同根因→新建 RC 系列」（§7.5），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9 冻结）：本审计未修改 product-scope / use-cases / costing-methods / state-machine 需求契约段落。
- [x] **保护区域纪律声明**：本审计为只读评估，未修改 `ErpInvLandedCostProcessor.lockReceiveForAllocation` / `validateNotAlreadyAllocated` / `ormTemplate.lock` 平台实现 / Dialect XML / application.yaml（锁/check/隔离逻辑保护区域）。P1-RC-092 修复归 MR1，触及锁/check 逻辑或 ORM UK 须 ask-first + 独立 plan-audit（§5）。

---

## §自检清单（报告产出前强制）

- [x] §1 存疑点原文 + L1 需求契约（UC-FIN-10 防重复分摊并发安全 + LC 验收标准逐字）
- [x] §2 锁入口与平台实现核验（L3 file:line，5 层调用链 `IOrmTemplate.lock` → `GenSqlHelper.genLockSql` PESSIMISTIC_WRITE + check 非锁读不同表）
- [x] §3 跨方言锁有效性评估（Dialect XML `<forUpdate>` 三方言继承 default + 主键等值记录锁排除 MySQL GAP 锁[针对错误威胁模型] + 锁等待超时运营差异）
- [x] §4 锁保护下 check-then-act 原子性评估（编排时序 `:37→:52→:53` + MVCC 快照跨方言分叉 + 有效性矩阵 + 配置缓解核查 → MySQL-RR 退化）
- [x] §5 既有测试覆盖边界普查（H2 锁串行化深覆盖 + 「check 见并发提交」零覆盖盲区 + 真实 DB 缺口归 MR1）
- [x] §6 MA4↔A5.6 边界声明
- [x] §7 分级裁决（注册 P1-RC-092 P1 + MR1 ask-first 修复方向 + 三源对照 + P1-MA2-085 resolved 不撤销 caveat + 去重）
- [x] §8 过程纪律自检（checker actual vs baseline + 独立性[含 Blocker 修订记录] + 交叉去重 + 真相源未修改 + 保护区域）

**报告完整性自检结论**：§1-§8 全部存在，无缺失。
