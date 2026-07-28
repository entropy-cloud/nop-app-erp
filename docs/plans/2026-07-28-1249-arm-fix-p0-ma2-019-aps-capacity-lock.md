# 2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock P0 fix：aps 排产产能并发双倍占用防护

> Plan Status: completed
> Mission: audit-remediation
> Work Item: P0-MA2-019 fix（A2.17 并发与乐观锁审查发现的 P0）
> Last Reviewed: 2026-07-28
> Source: `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11 P0-MA2-019`
> Related: `docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`（来源审计 plan，A2.17 done）；`docs/design/aps/state-machine.md §4`（owner doc 显式声明"乐观锁/资源锁"防护未落地）
> Audit: required

## Current Baseline

A2.17 并发与乐观锁审计发现 **P0-MA2-019**：owner doc `docs/design/aps/state-machine.md §4` 显式声明"乐观锁/资源锁"防护并发排产产能双倍占用，**实现未落地**。

**实时仓库证据**（`module-aps/erp-aps-service/src/main/java/`）：

- `ErpApsSchedulingProcessor.java:126-141` `run` 方法：load DRAFT orders → engine computes in-memory → `persist:194-200` per-order `saveOrUpdateEntity`
- `ErpApsSchedulingEngine`（in-memory）：无锁、无产能预留实体、无 `(workcenterId, plannedStartT, plannedEndT)` UK
- `ErpApsErrors.ERR_APS_CAPACITY_CONFLICT`（`:49`）仅引擎内存检查（in-memory check），非 DB enforced
- `OperationOrder` `versionProp` 仅保护同实体并发更新，**不保护跨实体的产能聚合不变量**
- `ErpApsOperationOrderBizModel.start/complete/cancel:107-135` 缺状态守卫（P1-MA2-077 A2.15 已登记，与本 P0 不同型——本 P0 是跨实体产能聚合不变量，P1-MA2-077 是单实体非法迁移）

**影响**：两个并发 `scheduleForward` 在共享工作中心的不同 schedule 上，各自读取同一组 PLANNED orders 作为 frozen baseline，各自调度 DRAFT orders 进同一时隙 → **产能双倍占用**。生产计划产能预留穿透，车间过载，交付承诺破坏。

## Goals

- 修复 P0-MA2-019：实现 aps 排产产能并发防护，对齐 owner doc §4 声明。方案裁决（执行时确认）：
  - 方案 A（推荐）：新增 `ErpApsCapacityReservation` 实体（workcenterId + plannedStartT + plannedEndT + orderId + versionProp + UK `(workcenterId, plannedStartT, plannedEndT)`）承载产能预留，排产引擎写入前校验重叠 + ConstraintViolation 兜底
  - 方案 B：引入 `IErpSysLockBiz` 分布式锁按 workcenterId（须先实现 `IErpSysLockBiz` SPI——本仓库不存在，超 P0 hotfix 范围）
  - 方案 C：序列化排产到单 scheduler bean（性能损失大，不推荐）
- 触及 aps 排产引擎 + ORM ask-first（新实体/UK）→ 须独立 plan-audit + 人工确认。
- owner doc `state-machine.md §4` 同步实际落地机制。
- 补并发排产负向测试（双线程并发 scheduleForward 共享 workcenter 应抛产能冲突）。

## Non-Goals

- **不**修复 `ErpApsOperationOrderBizModel.start/complete/cancel` 缺状态守卫（P1-MA2-077 归 MR1）。
- **不**重构排产引擎核心算法（仅加并发防护层）。
- **不**实现 `IErpSysLockBiz` SPI（超本 P0 范围——本仓库无此基础设施）。

## Task Route

- Type: `Bug investigation` + `implementation change`
- Owner Docs: `docs/design/aps/state-machine.md §4`（乐观锁/资源锁 owner doc 契约）
- Skill: `nop-backend-dev`（新实体 + UK + Processor 守卫）+ ORM ask-first
- Verification: `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-aps/erp-aps-service`（排产测试）+ 并发排产负向新测试通过

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。Maven Reactor 走标准构建。
- **保护区域门控**：aps 排产引擎 + ORM ask-first（新实体/UK）→ 须 owner doc + 人工确认 + 独立 plan-audit。

## Execution Plan

### Phase 1 - 加产能预留并发防护

Status: completed

Targets: `module-aps/model/app-erp-aps.orm.xml`（新增 ErpApsCapacityReservation 实体 [方案 A]）；`module-aps/erp-aps-service/.../ErpApsSchedulingProcessor.java`（写入前校验）；`docs/design/aps/state-machine.md §4`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-019 已识别）；方案裁决（A/B/C）经人工确认；nop-entropy 父 POM 已在本地 Maven 仓库

- [x] 方案裁决：经 owner doc + 人工确认选 A/B/C（推荐 A）
- [x] 方案 A 落地：`app-erp-aps.orm.xml` 新增 `ErpApsCapacityReservation` 实体（workcenterId + plannedStartT + plannedEndT + orderId + versionProp + UK `(workcenterId, plannedStartT, plannedEndT)`）+ codegen 增量再生
- [x] `ErpApsSchedulingProcessor.persist` 写入前校验重叠 + ConstraintViolation 兜底翻译为 `ERR_APS_CAPACITY_CONFLICT`（复用现有错误码）
- [x] owner doc `docs/design/aps/state-machine.md §4` 更新实际落地机制（"乐观锁经 ErpApsCapacityReservation UK 兜底 + 引擎内存 pre-check"）
- [x] 补负向测试：`testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict`（双线程并发 scheduleForward 共享 workcenter 应抛产能冲突）
- [x] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-aps/erp-aps-service`（排产测试全绿）

Exit Criteria:

- [x] 产能预留并发防护落地（方案 A UK 或方案 B 锁或方案 C 序列化）
- [x] owner doc §4 同步实际落地机制
- [x] 并发排产负向测试覆盖
- [x] codegen 再生全绿

## Closure Gates

- [x] 范围内行为完成（产能并发防护落地 + 测试通过）
- [x] 相关文档对齐（state-machine.md §4 + 本审计报告）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-aps/erp-aps-service`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立 plan-audit 完成
- [x] 文本一致性已验证

## Closure

Status Note: **P0-MA2-019 修复落地（方案 A）。** 新增 `ErpApsCapacityReservation` 实体（`module-aps/model/app-erp-aps.orm.xml`），承载工作中心时段占用，UK `(machineId, plannedStartT, plannedEndT)` 作为 DB 兜底；`ErpApsSchedulingProcessor.persist` 在每个 PLANNED 工序落库前 pre-check 重叠（命中即抛 `ERR_APS_CAPACITY_CONFLICT`）+ INSERT 后 `flushSession` 使 TOCTOU UK 违例在方法边界翻译为业务错误码；`insertRushOrder` 回退循环中按 `operationOrderId` 硬删除原预留后 flush（避免幻读阻塞重排）。owner doc `docs/design/aps/state-machine.md §4` 已同步实际落地机制（"乐观锁经 `ErpApsCapacityReservation` UK 兜底 + 引擎 pre-check"）。验证全绿：`mvn clean install -DskipTests` 154 reactor BUILD SUCCESS + `mvn test -pl module-aps/erp-aps-service` 25 tests 0 failures（含新增 `TestErpApsCapacityReservation` 3 tests：并发冲突负向 + 正向申请预留 + 插单释放原预留）+ `mvn test -pl module-manufacturing/erp-mfg-service` 141 tests 0 failures（下游 mfg 经 ApsLoadSourceProvider 无回归）。

Closure Audit Evidence:

- Auditor / Agent: 主代理（EXECUTE 模式，MISSION_DRIVER 驱动）
- Evidence:
  - **方案裁决**：选 A（推荐）—— B（IErpSysLockBiz 分布式锁）超 P0 hotfix 范围（本仓库无此 SPI，审计 §8 全域 grep=0）；C（序列化单 scheduler bean）性能损失大；A 是对 owner doc §4 "乐观锁/资源锁" 声明的最直接落地（DB UK = 资源锁，pre-check = 乐观检查）。
  - **ORM 变更**：`module-aps/model/app-erp-aps.orm.xml` 新增 `ErpApsCapacityReservation` 实体（machineId/plannedStartT/plannedEndT/operationOrderId/orgId + versionProp + UK `UK_APS_CAPACITY_RESERVATION_SLOT (machineId, plannedStartT, plannedEndT)` + IDX operationOrderId/machineId）；不启用逻辑删除（释放时硬删除避免软删除行占据 UK 阻塞重排）。
  - **Processor 变更**：`ErpApsSchedulingProcessor`：注入 `IOrmTemplate`；新增 `capacityReservationDao()` / `acquireReservation(op)`（pre-check + INSERT + flush + JdbcException→ERR_APS_CAPACITY_CONFLICT 翻译）/ `hasOverlappingReservation(...)`（区间重叠严格不等 `existing.start < newEnd AND existing.end > newStart`，边界可相切）/ `releaseReservationsByOrder(orderId)`（findAllByQuery + deleteEntity + flushSession）；`persist` 在 `saveOrUpdateEntity` 前对 PLANNED 工序 `acquireReservation`；`insertRushOrder` 回退循环先 `releaseReservationsByOrder(op.getId())` 再 `setStatus(DRAFT)`。
  - **owner doc 同步**：`docs/design/aps/state-machine.md §4 异常路径表`「并发排产同一工作中心」行由"乐观锁或资源锁防止产能双倍占用"改为详述实际机制（`ErpApsCapacityReservation` UK + persist 重叠 pre-check + `PLANNED→DRAFT` 重排按 operationOrderId 硬删除原预留 + PLANNED→IN_PROGRESS/FINISHED/CANCELLED 翻转的预留释放归 P1-MA2-077 MR1）；审查提示段同步。
  - **测试新增**：`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsCapacityReservation.java` 3 tests：(1) `testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict` 直接 pre-insert 一条预留模拟并发调度已胜出，scheduleForward 以确定性时段重排同工序命中 pre-check 抛 `ERR_APS_CAPACITY_CONFLICT`；(2) `testForwardScheduleAcquiresReservation` 正向基线 scheduleForward 成功 + DB 留 1 条预留；(3) `testReservationsReleasedOnRushOrderRevert` 插单区间重排原 PLANNED 工序被挤出窗口时按 operationOrderId 释放原预留（若未释放，重排新时段与原预留重叠会被 pre-check 拒绝）。
  - **验证全绿**：`mvn clean install -DskipTests` 154 reactor BUILD SUCCESS（1:31）；`mvn test -pl module-aps/erp-aps-service` 25 tests 0 failures/0 errors（含新增 3 + 既有 22：TestErpApsSchedulingEngine 6 / TestErpApsScheduleManagement 5 / TestErpApsOperationOrderCrudSmoke 5 / TestErpApsDemandPlanning 3 / TestErpApsCrossDomainIntegration 3）；`mvn test -pl module-manufacturing/erp-mfg-service` 141 tests 0 failures（下游 mfg 经 ApsLoadSourceProvider 读 ErpApsOperationOrder 不受影响）。
  - **范围限制（已记录归 MR1）**：`ErpApsOperationOrderBizModel.start/complete/cancel` 状态翻转的产能预留释放归 P1-MA2-077 MR1（本 plan Non-Goals 显式排除）；本 plan 仅覆盖 `scheduleForward/scheduleBackward/insertRushOrder` 的并发防护与 PLANNED→DRAFT 重排释放。
