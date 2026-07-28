# 2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock P0 fix：aps 排产产能并发双倍占用防护

> Plan Status: planned
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

Status: planned

Targets: `module-aps/model/app-erp-aps.orm.xml`（新增 ErpApsCapacityReservation 实体 [方案 A]）；`module-aps/erp-aps-service/.../ErpApsSchedulingProcessor.java`（写入前校验）；`docs/design/aps/state-machine.md §4`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-019 已识别）；方案裁决（A/B/C）经人工确认；nop-entropy 父 POM 已在本地 Maven 仓库

- [ ] 方案裁决：经 owner doc + 人工确认选 A/B/C（推荐 A）
- [ ] 方案 A 落地：`app-erp-aps.orm.xml` 新增 `ErpApsCapacityReservation` 实体（workcenterId + plannedStartT + plannedEndT + orderId + versionProp + UK `(workcenterId, plannedStartT, plannedEndT)`）+ codegen 增量再生
- [ ] `ErpApsSchedulingProcessor.persist` 写入前校验重叠 + ConstraintViolation 兜底翻译为 `ERR_APS_CAPACITY_CONFLICT`（复用现有错误码）
- [ ] owner doc `docs/design/aps/state-machine.md §4` 更新实际落地机制（"乐观锁经 ErpApsCapacityReservation UK 兜底 + 引擎内存 pre-check"）
- [ ] 补负向测试：`testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict`（双线程并发 scheduleForward 共享 workcenter 应抛产能冲突）
- [ ] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-aps/erp-aps-service`（排产测试全绿）

Exit Criteria:

- [ ] 产能预留并发防护落地（方案 A UK 或方案 B 锁或方案 C 序列化）
- [ ] owner doc §4 同步实际落地机制
- [ ] 并发排产负向测试覆盖
- [ ] codegen 再生全绿

## Closure Gates

- [ ] 范围内行为完成（产能并发防护落地 + 测试通过）
- [ ] 相关文档对齐（state-machine.md §4 + 本审计报告）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-aps/erp-aps-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立 plan-audit 完成
- [ ] 文本一致性已验证

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写>
- Evidence: <待执行后填写>
