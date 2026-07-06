# 2026-07-07-0024-2 库存并发扣减乐观锁加固 + 负库存行为验证（UC-INV-08/09）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.3（StockMove BizModel，`partial`——并发扣减锁 UC-INV-08 / 负库存拦截 UC-INV-09 仍 todo）；`docs/audits/2026-07-06-use-case-implementation-audit.md` §4（UC-INV-08 ❌ 乐观锁未测试 / UC-INV-09 🔶）
> Related: `docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（StockMove BizModel 基线计划，已 completed）、`docs/design/inventory/use-cases.md`（UC-INV-08/09 可验证断言权威）、平台 `../nop-entropy/docs-for-ai/02-core-guides/concurrency-and-transactions.md`（乐观锁 tryLock 模式权威）
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：StockMove BizModel 已落地（`2026-07-01-0811-2` completed）——状态机 + generateMove 契约 + 不可变流水 + 余额驱动（移动加权平均/FIFO/标准成本三策略）+ 可用量校验 + 存货过账。但 roadmap 1.3 仍标 `partial`，审计 §4 定位两项遗留：

**UC-INV-08 并发扣减乐观锁（❌ 真实缺口）**：
- `ErpInvStockBalance` **已有 `version` 列**（`module-inventory/model/app-erp-inventory.orm.xml:381`，versionProp="version"，INTEGER mandatory defaultValue=0）——乐观锁基础设施齐备。
- 但 `StockMoveBookkeeper.findBalance()`（`module-inventory/erp-inv-service/.../stock/StockMoveBookkeeper.java:194-213`）以 `dao.findAllByQuery(q)` 无锁加载余额，行内修改后**依赖 session flush 落盘**——**未调用**平台 `IOrmEntityDao.tryUpdateWithVersionCheck(entity)` / `tryUpdateManyWithVersionCheck(entities)`，**无 `orm_unload()` 重试循环**。
- 平台权威（`concurrency-and-transactions.md` §默认规则 3 + §模式一/三/四）：乐观锁是默认并发控制机制，须经 `tryUpdateWithVersionCheck()` 显式启用（`UPDATE ... WHERE version=?`，只返回成功实体），冲突后须 `entity.orm_unload()` 刷新 baseline 再重试。当前实现未采纳此模式 → 并发 DONE 扣减同一余额行时，in-memory 修改 + flush **不保证** version-check 保护扣减（须 Explore 核实 session flush 是否隐式带 version；无论结论如何，显式 tryLock 模式缺失）。
- **无并发测试**（审计 §4：UC-INV-08 ❌ 无并发测试框架）。

**UC-INV-09 负库存放行（🔶 审计保守，实测多数已落地）**：
- `erp-inv.allow-negative-stock` 配置 + `StockMoveBookkeeper` 的可用量校验已存在（0811-2 Phase 2）。
- 实时核实测试**已存在**：`TestErpInvStockMoveBookkeeping.testNegativeStockConfigAllowsShortage`(:130) + `testConfirmInsufficientAvailableRejected`(:115)。即审计 §4「配置开/关行为未测试」**过保守**——基本 on/off 行为已测。
- 残留缺口：负库存下 `reserved`/`locked` 与 `available` 三量交互（出库放行后 available 变负、后续入库补回）的端到端断言可能不全（须核实覆盖度）。

**剩余差距**：(1) UC-INV-08 采纳平台乐观锁 tryLock 模式 + 重试循环保护并发扣减；(2) 建立并发测试框架证明不超扣；(3) UC-INV-09 核实并补全三量交互测试。

## Goals

- **UC-INV-08 并发扣减不超扣**：`StockMoveBookkeeper` 余额扣减路径采纳平台乐观锁 tryLock 模式——DONE 写流水前/后，对目标 `ErpInvStockBalance` 行经 `tryUpdateWithVersionCheck` 提交增量（totalQuantity/reserved/available/cost），冲突时 `orm_unload()` 刷新 baseline + 有限次重试（默认上限可配），重试耗尽抛 `NopException(ERR_INV_CONCURRENT_DEDUCT_CONFLICT)`；保证并发 DONE 扣同一批次时一个成功、另一个重试成功或可控失败，最终 `totalQuantity == 初始 − A − B`（除非允许负库存）。
- **并发测试框架建立**：新增可重复的并发集成测试（多线程 + 独立 session/事务扣同一余额行），断言不超扣 + 最终余额正确，填补审计「无并发测试框架」。
- **UC-INV-09 三量交互验证**：核实并补全负库存 on/off 下 `total/reserved/locked/available` 四量交互的端到端断言（含出库放行 available 变负、入库补回、reserved/locked 不被负库存绕过）。
- **无回归**：既有 StockMove 状态机/流水/余额/过账/三策略成本行为不回归。

## Non-Goals

- **不改任何 `model/*.orm.xml`/`.api.xml`**（保护区域）——`ErpInvStockBalance.version` 列已存在，复用；不加余额维度唯一约束（0811-2 Deferred，保护区域）。
- **不改三策略成本核算逻辑**（MovingAverage/FIFO/Standard）——仅在其共享的余额 load+update 路径注入乐观锁；策略内部记账行为不变。
- **不做悲观锁（SELECT FOR UPDATE）**——平台默认乐观锁；悲观锁须 DB/方言相关，非本期。
- **不加余额维度唯一约束**——未在 `2026-07-01-0811-2` 落地（保护区域，本期 Non-Goal；首次显式记录于本计划 Deferred）；乐观锁 tryLock 已防并发超扣。
- **不做分布式锁/Redis**——单库乐观锁足够覆盖 UC-INV-08（同库并发扣减）。
- **不重构 `complete()`/Processor 事务边界**——`@BizMutation` 已包事务；乐观锁重试在 bookkeeper 内循环（冲突=版本不匹配，重试在同一逻辑操作内，不跨外部事务边界）。
- **不补 UC-INV-06 批次效期过期拦截 / UC-INV-07 盘点差异移动单**（审计 🔶，独立 successor）。

## Task Route

- Type: `bug investigation` + `implementation-only change`（已确认并发保护缺口，加固既有 bookkeeper；不改公共 API 契约或 ORM）。
- Owner Docs: `docs/design/inventory/use-cases.md`（UC-INV-08/09 断言）、`docs/design/inventory/state-machine.md`（DONE 写流水+更新余额）、`docs/design/inventory/cross-domain.md`（余额更新与流水同一事务）、平台 `../nop-entropy/docs-for-ai/02-core-guides/concurrency-and-transactions.md`（乐观锁 tryLock/orm_unload 重试权威——本计划核心模式来源）。
- Skill Selection Basis: 实施阶段 `Skill: nop-backend-dev`（BizModel/记账器改造）；并发问题排查 `Skill: nop-debugging`（技能描述「any bug/unexpected behavior」匹配并发扣减缺口）；并发测试 `Skill: nop-testing`。独立草案/结束审计用审计提示模板。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-inv-app` 已含 `quarkus-jdbc-h2`）；并发测试需多连接（H2 内存库 + 连接池，测试以独立线程 + 独立 session 模拟并发事务——具体机制经 Phase 1 Decision 裁定）。
- 新增配置键（`ErpInvConstants` 声明，`AppConfig.var` 读取）：`erp-inv.concurrent-deduct-max-retry`（默认 5）、`erp-inv.concurrent-deduct-retry-backoff-ms`（默认 0=同步重试）。
- 无数据迁移/回滚脚本需求（加固既有逻辑，复用既有实体与列）。

## Execution Plan

### Phase 1 - UC-INV-08 乐观锁 tryLock 模式 + 并发测试框架

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/stock/StockMoveBookkeeper.java`、`.../costing/*.java`（策略余额 load+update 注入点）、`module-inventory/erp-inv-service/.../ErpInvConstants.java`、`module-inventory/erp-inv-service/src/test/.../`
Skill: nop-backend-dev

- Item Types: `Explore | Decision | Fix | Add | Proof`
- Prereqs: 无

- [x] `Explore`（须先于 Decision 完成）：Nop ORM session flush 是否隐式带 version 校验。核实 `IOrmEntityDao` 对带 versionProp 实体的普通 `saveEntity`/session flush 是否生成 `UPDATE ... WHERE id=? AND version=?`（即隐式乐观锁），还是仅 `tryUpdateWithVersionCheck` 显式生效。产出书面结论（引用平台源码/文档行号）写入本阶段记录，决定加固是「补显式 tryLock」还是「补显式 tryLock + 证实隐式已失效」。
  - Skill: nop-debugging
  - **书面结论**：平台对带 `versionProp` 实体的 UPDATE SQL 生成始终包含 `WHERE id=? AND version=?` 条件（`io.nop.orm.sql.GenSqlHelper.genUpdateSql` 第 322-327 行：`if (entityModel.getVersionPropId() > 0) { ... appendCol ... }`，无开关）。session flush 默认走 `EntityPersisterImpl.checkUpdateResult`（第 504-520 行）：count==0 时，`orm_disableVersionCheckError()==false`（默认）则抛 `ERR_ORM_UPDATE_ENTITY_NOT_FOUND`，`true` 则置 `entity.orm_readonly(true)` 静默返回。`IOrmEntityDao.tryUpdateWithVersionCheck` 默认方法（`IOrmEntityDao.java:40-44`）即设 `disableVersionCheckError=true` + `updateEntityDirectly` + 返回 `!orm_readonly()`。**结论**：隐式乐观锁确实存在（默认抛错），但生产并发扣减需要「失败可重试」而非「抛错回滚整个事务」，故仍需补显式 tryLock + 重试循环。**额外发现**：`OrmEntity.orm_readonly(boolean)` 设置器实现为 `this.readonly = true`（无视入参，第 227-229 行），即 readonly 一旦置 true 不可在原实例上复位——重试必须 `session.evict(entity)` + `dao.requireEntityById(id)` 重新加载新实例，不能依赖文档示例的 `orm_unload + getEntityById`（同一实例 readonly 仍粘性，flush 会跳过）。
- [x] `Decision`：乐观锁注入粒度与重试边界（依 Explore 结论）。裁决：在 `StockMoveBookkeeper` 暴露的余额 load+update 通道（策略经 `BookingContext` 调用的 `upsertBalance`/`recomputeAvailable` 及最终落盘点）引入 `tryUpdateWithVersionCheck`；冲突经 `entity.orm_unload()` + `dao.requireEntityById(id)` 刷新 baseline（对齐 `concurrency-and-transactions.md` §模式四），按 `concurrent-deduct-max-retry` 有限重试，耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`。重试在 bookkeeper 内部循环（同一外部事务内重复 load→重算增量→tryUpdate）。备选（被否）：在 Processor 跨事务层重试——事务边界扩散、与 `@BizMutation` 单事务语义冲突。残留风险：高冲突场景下重试耗尽抛错（可配重试次数缓解）；同事务内多行扣减若部分行冲突，须整体重试（已在循环内处理）。
  - Skill: nop-backend-dev
  - **实施修订（依 Explore）**：刷新 baseline 改为 `session.evict(current) + dao.requireEntityById(id)` 加载新实例（而非文档示例的 `orm_unload + getEntityById` 复用同实例），返回值改为新实例引用——因 `orm_readonly` 粘性使同实例无法再次 flush。新增 `BookingContext.updateBalanceWithRetry(initial, applyDelta)` 方法，签名 `Consumer<ErpInvStockBalance>` 接收每次重试刷新后的 baseline，策略把字段计算+设置移入回调（保证幂等重试）。
- [x] `Decision`：并发测试机制。裁决：多线程 + `CountDownLatch` 栅栏同步起步，每线程独立 `IOrmTemplate` session / `@Transactional(REQUIRES_NEW)` 事务执行一次出库 DONE 扣同一余额行；主线程等待全部完成，断言成功扣减数 × 单次量 ≤ 初始量（不超扣），最终 `totalQuantity` 正确。备选（被否）：单线程模拟——无法验证真实并发竞争。残留风险：H2 内存库并发行为与生产 DB 可能差异（并发测试主要证明乐观锁逻辑正确，生产级竞争须 DB 级压测 successor）。
  - Skill: nop-testing
  - **实施修订**：混合机制——`testConcurrentDeductRetrySucceeds`/`testConcurrentDeductRetryExhaustedThrows` 用单线程模拟（确定性，直接调 `updateBalanceWithRetry`，绕开 strategy 经 `upsertBalance.findAllByQuery` 触发平台 `lazyCheck` 在二次查询时的 `ERR_ORM_ENTITY_VERSION_CHANGED`——真实生产冲突走 tryUpdate 0-row 返回路径，不走 lazyCheck）；`testConcurrentDeductNoOversell`/`testConcurrentDeductWithNegativeStockAllowed` 用真实多线程 + CountDownLatch + 每线程独立 IOrmTemplate.runInSession + ContextProvider.newContext 验证并发不丢失更新。
- [x] `Fix | Add`：修复 UC-INV-08 并发扣减无乐观锁保护的实时缺陷——`StockMoveBookkeeper` 余额落盘点改用 `tryUpdateWithVersionCheck`（经 `daoProvider.daoFor(ErpInvStockBalance.class)`）；新增内部 `deductWithRetry(balance, deltaComputer)` 循环（load baseline → 算增量 → tryUpdate → 失败 orm_unload+reload 重试）；`ErpInvConstants` 声明 `CONFIG_CONCURRENT_DEDUCT_MAX_RETRY`/`CONFIG_CONCURRENT_DEDUCT_RETRY_BACKOFF_MS` + `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` ErrorCode（中文描述）。`Fix` 盖确认的并发缺陷修复（Rule 7/13），`Add` 盖新增 ErrorCode/配置/重试循环。三策略记账行为不变（仅共享落盘通道加固）。
  - Skill: nop-backend-dev
  - **实施记录**：`BookingContext` 新增 `updateBalanceWithRetry` 接口方法；`StockMoveBookkeeper` 实现（含 MANAGED/TRANSIENT/SAVING 三态分支 + evict+reload 循环 + ERR_INV_CONCURRENT_DEDUCT_CONFLICT 抛错）；三策略（MovingAverage/FIFO/Standard）的 `saveOrUpdateEntity(balance)` 改为 `ctx.updateBalanceWithRetry(balance, b -> {...})`，字段计算+设置全部移入回调（保证重试幂等，FIFO 成本层消耗留在回调外作为一次性副作用）；`ErpInvConstants` 增 2 配置键 + `ErpInvErrors` 增 1 ErrorCode + 2 ARG 常量。
- [x] `Proof`：并发集成测试 `TestErpInvConcurrentDeduct`——`testConcurrentDeductNoOversell`（两/多线程扣同一余额，断言不超扣 + 最终余额正确）、`testConcurrentDeductRetrySucceeds`（冲突经重试最终成功）、`testConcurrentDeductRetryExhaustedThrows`（超 max-retry 抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`）、`testConcurrentDeductWithNegativeStockAllowed`（**并发层面**：允许负库存时乐观锁机制仍正确放行并发扣减、最终可为负且一致——本用例归属 Phase 1 并发域，与 Phase 2 四量不变量测试互补不重叠）。`mvn test -pl module-inventory/erp-inv-service -am` 全绿。
  - Skill: nop-testing
  - **验证证据**：4 测试全绿（`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`）；既有 79 测试全绿无回归（83 tests total 含本计划新增 4 测试）。

Exit Criteria:

> 本阶段交付 UC-INV-08 并发不超扣 + 并发测试框架。完整仓库 `mvn test` 归 Closure Gates。

- [x] Explore 结论已书面记录且 Decision 依其裁定
- [x] 4 个并发行为测试存在且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [x] 既有 StockMove 记账/状态机测试（0811-2 资产）无回归

### Phase 2 - UC-INV-09 三量交互验证 + 收尾

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/.../TestErpInvStockMoveBookkeeping.java`（补全）、`docs/logs/2026/07-07.md`
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 1

- [x] `Proof`：核实并补全 UC-INV-09 三量交互测试——`testNegativeStockAvailableGoesNegative`（放行后 available 变负）、`testSubsequentIncomingReplenishes`（后续入库补回负余额，avgCost 平滑）、`testReservedNotBypassedByNegativeStock`（reserved/locked 不被负库存绕过，available=total−reserved−locked 在负库存下仍成立）、`testNegativeStockOffRejectsByDefault`（默认配置 off 拒绝，覆盖既有 `testConfirmInsufficientAvailableRejected` 的默认态）。`mvn test -pl module-inventory/erp-inv-service -am` 全绿。
  - Skill: nop-testing
  - **验证证据**：4 新测试全绿（`Tests run: 9, Failures: 0` 含既有 5 + 新增 4）；既有 5 测试无回归；总模块测试 87 全绿（83 + 4 新增）。新增 `generateOutgoingConfirmed` 辅助方法（不设 relatedBillType → 停在 CONFIRMED，供 reserved 状态测试）。
- [x] `Add`：更新当日开发日志 `docs/logs/2026/07-07.md`（时间倒序），记录 UC-INV-08 乐观锁加固 + 并发测试框架 + UC-INV-09 验证 + 验证状态；同步 `core-business-roadmap.md` 工作项 1.3 状态。
  - Skill: none
  - **完成证据**：日志 `## 库存并发扣减乐观锁加固 + 负库存行为验证 — plan 2026-07-07-0024-2` 节已添加到 `07-07.md` 顶部；roadmap 工作项 1.3 从 `partial` 改为 `done`，两处文案（Implementation Order 表 + M1 列表）已同步。

Exit Criteria:

> 本阶段交付 UC-INV-09 三量交互验证 + 收尾。完整仓库 `mvn test` 归 Closure Gates。

- [x] UC-INV-09 三量交互测试补全且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [x] 当日日志已记并发加固与验证状态

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0c7bb9818ffeoPjWK7khffis2，独立 general 子代理，新会话）— 基线主张实时核实全部属实（ErpInvStockBalance.version 列 + versionProp@381/361、索引均单列 non-unique、StockMoveBookkeeper.findBalance@211 用 findAllByQuery 无锁、全 stock 服务目录无 tryUpdateWithVersionCheck/orm_unload——并发缺口真实、UC-INV-09 测试@115/130 实存故「审计过保守」定性核实、平台 concurrency-and-transactions.md retry 模式权威）。1 项阻塞：B1——Phase 1 UC-INV-08 修复项仅标 `Add`，但系确认的实时并发缺陷，Rule 7/13 须标 `Fix`。2 项建议：S1（余额维度唯一约束「0811-2 Deferred」归属不实——0811-2 Deferred 列表未含）；S2（Phase 1 vs Phase 2 负库存测试范围重叠须澄清）。已据以修订。
- Independent draft review iteration 2: **accept / consensus**（ses_0c7b542d1ffeQu1BV4hMUmCHNV，独立 general 子代理，新会话）— B1 已解决（Phase 1 修复项改标 `Fix | Add` 并引 Rule 7/13，Item Types 行含 Fix）；S1 已处理（Non-Goals + Deferred 两处改为「未在 0811-2 落地，首次显式记录于此」）；S2 已处理（testConcurrentDeductWithNegativeStockAllowed 标注归属 Phase 1 并发域、与 Phase 2 四量不变量互补不重叠）。实时仓库复核基线准确、并发缺口真实；Rule 7/9/13 + anti-slack 全过；无新阻塞。**共识达成**：计划为可接受执行契约。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：UC-INV-08 乐观锁 tryLock + 重试 + 并发测试；UC-INV-09 三量交互验证全部落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 工作项 1.3 标 ✅ done；当日日志已记
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am` 全绿（87 tests）；根 `mvn test -fae` = BUILD SUCCESS（无回归，含三策略成本/状态机/过账既有测试）
- [x] 无范围内项目降级为 deferred/follow-up（UC-INV-06/07 为计划内 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### ErpInvStockBalance 余额维度唯一约束

- Classification: `optimization candidate`
- Why Not Blocking Closure: 未在 `2026-07-01-0811-2` 落地（0811-2 Deferred 列表未含此项，首次显式记录于此）；加唯一约束须改 ORM（保护区域）；乐观锁已防并发超扣，唯一约束防并发 upsert 重复行（低概率，因业务单据联动多为顺序扣减）。
- Successor Required: yes（触发条件：批量/高并发场景观察到余额维度重复行时，或保护区域唯一约束计划获批时）

### 生产级 DB 并发压测

- Classification: `optimization candidate`
- Why Not Blocking Closure: H2 并发测试证明乐观锁逻辑正确；生产 DB（MySQL/PG）竞争行为须 DB 级压测。
- Successor Required: yes（触发条件：目标生产 DB 选定 + 压测环境就绪时）

## Closure

Status Note: 计划完成条件已全部满足——Phase 1（UC-INV-08 乐观锁 tryLock + 重试循环 + 4 并发测试）与 Phase 2（UC-INV-09 三量交互 4 测试 + 日志 + roadmap 同步）全部交付，全仓库 `mvn test -fae` BUILD SUCCESS 无回归，独立结束审计通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（ses_0c73db90dffeu3Se1aMikfPUXf，新会话无执行者上下文）— 2026-07-07
- Verdict: `passes closure audit`
- Evidence verified by auditor:
  - 代码：`StockMoveBookkeeper.updateBalanceWithRetry` 含 tryUpdateWithVersionCheck + evict+requireEntityById 重试循环 + ERR_INV_CONCURRENT_DEDUCT_CONFLICT + MANAGED/TRANSIENT/SAVING 三态分支（StockMoveBookkeeper.java:207-251）；三策略（MovingAverage/FIFO/Standard）saveOrUpdateEntity 全部改为 ctx.updateBalanceWithRetry，字段计算入回调（重试幂等）；ErpInvConstants + ErpInvErrors 新增配置/错误码齐全。
  - 测试：`mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvConcurrentDeduct,TestErpInvStockMoveBookkeeping` = `Tests run: 13, Failures: 0, Errors: 0` / BUILD SUCCESS（4 并发测试 + 4 UC-INV-09 测试 + 5 既有基线）。多线程测试用 CountDownLatch + ExecutorService + 每线程独立 IOrmTemplate.runInSession + ContextProvider.newContext，符合 Decision。
  - 反模式自检：@Inject 字段均非 private；异常用 NopException + ErrorCode；无策略残留 daoProvider.daoFor(...).saveOrUpdateEntity(balance)；bookkeeper 注入 IDaoProvider 是记账原语合规。
  - 保护区域：`git status --porcelain` 修改文件清单无 model/*.orm.xml / *.api.xml；Non-Goal 已遵守。
  - 文本一致性：Plan Status `active`（审计前）↔ Phase 1/2 `completed` ↔ 所有 `[x]` ↔ 日志记录验证状态——无内部矛盾。
  - 残留风险（已记录为 Non-Goal / Deferred，非阻塞）：FIFO 成本层并发无 version-check（Non-Goal：不改三策略成本核算逻辑）；生产 DB 压测（Deferred successor）；余额维度唯一约束（Deferred successor）。

Follow-up:

- UC-INV-06 批次效期过期拦截（触发条件：启用批次有效期管理时）
- UC-INV-07 盘点差异移动单（触发条件：实施盘点功能 successor 时）
- 余额维度唯一约束 / 生产 DB 压测（见上方 Deferred）
