# 2026-08-13-0805-1-erpmnt-request-state-machine-bean 维护请求 status 状态机 Bean

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M3.17；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.4`
> Related: `2026-08-13-1430-2-erphr-employee-timesheet-state-machine-beans.md`（同类 Processor 接线先例）、`2026-08-13-1430-3-erpct-version-rebate-state-machine-beans.md`（同类 owner-doc 缺口补章节先例）
> Audit: required

## Current Baseline

- **实体与状态轴**：`ErpMntRequest.status`（`module-maintenance/model/app-erp-maintenance.orm.xml:354`），`ext:dict="erp-mnt/request-status"`，字典 6 值：`OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED`（`app-erp-maintenance.orm.xml:46-51`）。
- **既有实现**：`ErpMntRequestBizModel`（5 个 `@BizMutation`）逐行委托 5 个 per-mutation Processor（R6.6 每 mutation 一 Processor）：
  - `accept` → `ErpMntRequestAcceptProcessor`：`OPEN → ACCEPTED`（guard `AbstractErpMntRequestProcessor.validateTransition(..., OPEN)`，`AcceptProcessor:21,39`）。
  - `startRepair` → `ErpMntRequestStartRepairProcessor`：`ACCEPTED → IN_PROGRESS`（`StartRepairProcessor:16,22`）。
  - `complete` → `ErpMntRequestCompleteProcessor`：`IN_PROGRESS → COMPLETED`（`CompleteProcessor:17,23`，并置 `completedAt`）。
  - `rejectRequest` → `ErpMntRequestRejectRequestProcessor`：`OPEN/ACCEPTED → REJECTED`（双源 guard `:19-21`，`RejectRequestProcessor:28`）。
  - `cancel` → `ErpMntRequestCancelProcessor`：`OPEN/ACCEPTED → CANCELLED`（双源 guard `:19-21`，`CancelProcessor:28`）。
- **守卫现状**：每个 Processor 内联 `Objects.equals(request.getStatus(), REQUEST_STATUS_X)` + `AbstractErpMntRequestProcessor.illegalRequestTransition(...)`（抛 `ERR_INVALID_REQUEST_STATUS_TRANSITION`）；共享 helper 含 `requireRequest`/`requestDao`/`validateTransition`/`illegalRequestTransition`（`AbstractErpMntRequestProcessor:29-53`）。固定迁移判断散落在 5 个 Processor，无法机器化回答可达性/终态出边/全部 dict 值是否有 writer。
- **副作用（保留在 Processor，不迁移）**：`accept` 受理后生成维护访问（Visit）跨实体副作用；`complete` 仅置 `completedAt`（`CompleteProcessor:24`，**无 `completedBy` writer**）。这些动态业务行为不在 StateMachine Bean 范围（Non-Goals：Bean 无状态、无副作用）。
- **owner doc 漂移**：`docs/design/maintenance/state-machine.md §适用对象二：维护请求` 状态表仅列 5 态（OPEN/ACCEPTED/COMPLETED/REJECTED/CANCELLED），**遗漏 `IN_PROGRESS`**；迁移图描述「维修中」但状态表无对应行。字典与代码均含 6 态。属 doc drift（非 code drift，Minimum Rule 13 不可降级），需在本计划 Phase 3 补齐 owner doc 状态表（Fix）。
- **M0.2 分类**：M3.17 归 M3（非保护域、无财务影响）。M0.2 清单 §3.4 行财务影响/跨域/过账均 = `无`；`accept→生成 Visit` 为跨实体副作用但不过账（不触发会计过账，不适用 §11.2 M3(iii) 升级条件）。M1.3（done）已解除 Deps 门控。

## Goals

- 新建无状态 `ErpMntRequestStateMachine` Bean，集中 `ErpMntRequest.status` 一条轴的完整迁移矩阵（5 命名动作 × 合法源态/目标态）、状态分类（initial/terminal）与机器可读元数据。
- 将 5 个 per-mutation Processor 的内联固定迁移守卫接线为 Bean 委托（`stateMachine.assertCan<Action>(from)` + 目标态回写），**保持既有外部行为、错误码、副作用、乐观锁与审批语义不变**。
- 层 1（矩阵完备性表驱动测试）+ 层 2（dict↔owner-doc↔Bean↔writer 四方对照）+ 层 3（既有动作回归）三层证据落地。
- 修正 owner doc §维护请求状态表遗漏 `IN_PROGRESS` 的 doc drift。

## Non-Goals

- 不迁移 `ErpMntRequest.approveStatus`（xbiz 另有 5 个 approveStatus mutation，不在本工作项 M3.17 范围；非路线图纳入轴）。
- 不迁移 `ErpMntVisit.status`（维护访问，属 M4.54 plan-first 财务影响域）。
- 不改变 `accept` 生成 Visit、`complete` 置时间字段等副作用；不改变错误码、权限、乐观锁。
- 不修改 `model/*.orm.xml`、字典值或 API 契约。
- 不引入通用 CRUD 对 status 写入的运行时禁止（M0.1 裁定的 CRUD 写入边界为 successor，见 Deferred）。

## Task Route

- Type: `implementation-only change`（固定迁移矩阵集中化，不改外部行为/契约/模型）
- Owner Docs: `docs/design/maintenance/state-machine.md §适用对象二：维护请求`、`docs/architecture/entity-state-machine-bean.md`（§1 命名、§2 无状态约束、§11 批量迁移模板）
- Skill Selection Basis: 本项是「写 BizModel/Processor 接线方法 + 矩阵完备性测试」的后端开发，匹配 `nop-backend-dev`（决策门、xbiz 动作、跨实体调用边界、`@Inject` 非 private）；矩阵测试与回归匹配 `nop-testing`（矩阵表驱动 + 服务级回归）；Phase 3 四方对照匹配 `state-machine-business-review-prompt.md`（10 维度审查方法）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpMntRequestStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/statemachine/ErpMntRequestStateMachine.java`（新建）、`module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/beans/app-service.beans.xml`（注册）、`module-maintenance/erp-mnt-service/src/test/.../statemachine/TestErpMntRequestStateMachine.java`（新建）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: M1.3 done（已解除门控）

- [x] 新建无状态 `ErpMntRequestStateMachine` Bean（命名遵循 `entity-state-machine-bean.md §1` 单轴省略 Axis 后缀；**不注入 DAO/I*Biz/IServiceContext**，§2 无状态约束）：
      - 迁移矩阵（5 动作）：
        - `accept`：源 `{OPEN}` → 目标 `ACCEPTED`
        - `startRepair`：源 `{ACCEPTED}` → 目标 `IN_PROGRESS`
        - `complete`：源 `{IN_PROGRESS}` → 目标 `COMPLETED`
        - `rejectRequest`：源 `{OPEN, ACCEPTED}` → 目标 `REJECTED`（双源）
        - `cancel`：源 `{OPEN, ACCEPTED}` → 目标 `CANCELLED`（双源）
      - 状态分类：`initialStatuses()={OPEN}`、`terminalStatuses()={COMPLETED, REJECTED, CANCELLED}`
      - 方法形状对齐 §11.1 步骤 1 与先例 Bean（`assertCan<Action>(from)`、`<action>TargetStatus()`、`isTerminal(status)`、`transitions()` 返回全边集）
  - Skill: `nop-backend-dev`
- [x] 在非生成 `app-service.beans.xml` 以 FQN 为 bean id 显式注册 `ErpMntRequestStateMachine`（对齐先例注册写法与 §6 Delta 同名覆盖契约）
  - Skill: `nop-backend-dev`
- [x] Proof（层 1 矩阵完备性，表驱动）：新建 `TestErpMntRequestStateMachine`，对全部 6 dict 值 × 5 动作遍历合法/非法边，断言：合法边返回目标态、非法边抛预期异常、`transitions()` 覆盖全部 5 条边、终态（COMPLETED/REJECTED/CANCELLED）无出边、initial={OPEN}。验证命令：`mvn test -pl module-maintenance/erp-mnt-service -Dtest=TestErpMntRequestStateMachine`
  - Skill: `nop-testing`

Exit Criteria:

> 仅此阶段交付 Bean + 注册 + 矩阵完备性证明，解除 Phase 2 接线依赖。

- [x] `ErpMntRequestStateMachine` 无状态、矩阵完整、6 dict 值在矩阵中均有合法入边或为 initial（OPEN）/可经 cancel/reject 到达终态
- [x] 层 1 表驱动测试通过（合法/非法边、终态无出边、transitions 全覆盖）

### Phase 2 - Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `ErpMntRequestAcceptProcessor`、`ErpMntRequestStartRepairProcessor`、`ErpMntRequestCompleteProcessor`、`ErpMntRequestRejectRequestProcessor`、`ErpMntRequestCancelProcessor`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [x] 将 5 个 Processor 内联固定守卫（`Objects.equals(...REQUEST_STATUS_X)` + `validateTransition`/`illegalRequestTransition`）替换为 `@Inject ErpMntRequestStateMachine stateMachine` + `stateMachine.assertCan<Action>(from)`；目标态回写改为 `request.setStatus(stateMachine.<action>TargetStatus())`。**保留**：`AbstractErpMntRequestProcessor` 共享 helper（`requireRequest`/`requestDao`/`validateTransition`/`illegalRequestTransition`）、`accept` 生成 Visit 跨实体副作用、`complete` 仅置 `completedAt`（**无 completedBy writer，不得新增**）、错误码 `ERR_INVALID_REQUEST_STATUS_TRANSITION` 及其参数（实体编号/上下文，§错误语义：Bean 报非法边，Processor 保留领域 ErrorCode + 实体编号）
  - Skill: `nop-backend-dev`
- [x] Proof（层 3 回归）：运行维护域既有动作测试（含 `accept/startRepair/complete/rejectRequest/cancel` happy path + 非法态拒绝 + 双源 reject/cancel）。验证命令：`mvn test -pl module-maintenance/erp-mnt-service`
  - Skill: `nop-testing`

Exit Criteria:

- [x] 5 Processor 接线后既有动作测试全绿（行为、错误码、副作用、双源 reject/cancel 无回归）

### Phase 3 - 层 2 四方对照（dict↔owner-doc↔Bean↔writer）+ owner doc doc-drift 修正

Status: completed
Targets: `docs/design/maintenance/state-machine.md §适用对象二：维护请求`、本计划 Closure
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Fix | Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（层 2 四方对照，10 维度 `state-machine-business-review-prompt.md`）：逐维度核对 dict（`erp-mnt/request-status` 6 值）↔ owner doc（§维护请求）↔ Bean 矩阵 ↔ 全部 writer（5 Processor），输出可追溯结论写入本计划 Closure
  - Skill: `state-machine-business-review-prompt.md`
- [x] Fix owner doc doc-drift：`docs/design/maintenance/state-machine.md §适用对象二：维护请求` 状态表补 `IN_PROGRESS（维修中）` 行（对齐字典 6 值与代码 5 Processor），并在迁移图显式标注 `ACCEPTED → startRepair → IN_PROGRESS → complete → COMPLETED` 边
  - Skill: `state-machine-business-review-prompt.md`
- [x] Decision：记录 rejectRequest/cancel 双源（OPEN/ACCEPTED）语义裁定——选择保持代码现状（`:19-21` dual-source guard），替代方案（单源收紧为仅 OPEN 或仅 ACCEPTED）被否决因会破坏既有 reject 已受理请求 / cancel 已受理请求的业务路径；残留风险：无（纯对齐既有行为）。确认 owner doc 已覆盖该双源声明。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 四方对照无未裁决漂移（IN_PROGRESS doc drift 已修正；双源语义已裁定并落入 owner doc）
- [x] owner doc §维护请求状态表与迁移图与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00776e4ba6ffeb23LuxGMLLg6cJ`) — 2 blockers + 1 major：fabricated `completedBy` side-effect（complete 仅置 completedAt）；beans.xml 注册路径误写 `_vfs/erp/mnt/service/`（应为 `_vfs/erp/mnt/beans/`）；Phase 3 doc-drift 项缺 `Fix` 类型。v2 已修正：移除 completedBy；订正 beans.xml 路径；Phase 3 item types 补 `Fix`；方法命名对齐 `<action>TargetStatus()`；helper 描述订正；Decision 补替代方案。
- Independent draft review iteration 2: `acceptable as-is` (`ses_007762e259ffeWihWVtXKNgz4s3`) — B1/B2/M1 全部 RESOLVED（live repo 实证 complete 仅置 completedAt、beans.xml 路径文件存在、Phase 3 含 Fix）；迁移矩阵 5 动作×6 态与代码一致；owner-doc IN_PROGRESS 漂移真实。anti-slack + 模板一致性 sweep 无残留 blocker/major。计划可转 `active`。

## Closure Gates

> 完整仓库验证在此处运行一次。无 ORM/API/字典变更，故 Compliance 基线预期无漂移（如漂移则在闭包前裁决）。

- [x] 范围内行为完成（Bean + 接线 + 三层证据）
- [x] 相关文档对齐（owner doc §维护请求 doc-drift 修正）
- [x] 已运行验证：`mvn test -pl module-maintenance/erp-mnt-service` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ErpMntRequest.approveStatus 轴

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: xbiz 另有 5 个 approveStatus mutation（submitForApproval/approve/reject/reverseApprove/withdrawApproval），不在路线图 M3.17 纳入轴范围（M0.2 仅纳入 status 业务生命周期轴）。
- Successor Required: `no`（非路线图项；若 PM 纳入则另起 plan）

### 通用 CRUD 对 status 写入的运行时禁止

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 裁定的 CRUD 写入边界（限制/禁止/排除）为全局 successor，本计划仅集中命名动作矩阵，不改变 CRUD 管道对 status 的既有写入能力。
- Successor Required: `no`（M0.1 CRUD 写入边界全局裁定 successor）

### Delta 同名 Bean 覆盖运行时证明

- Classification: `optimization candidate`
- Why Not Blocking Closure: M1.2 已在客服试点证明业务级 Delta 覆盖运行时生效；本批迁移沿用已验证的注册/Delta 契约，逐项不再重复运行时覆盖证明。
- Successor Required: `no`（归 M5.3 最终 Delta 覆盖回归）

## Closure

Status Note: 执行完成（3 Phase 全 done）。层 1 矩阵完备性测试 9 项全绿；层 3 回归 `mvn test -pl module-maintenance/erp-mnt-service` 71 项全绿（含 `TestErpMntVisitRequestStateMachine` 10 项：accept/startRepair/complete/rejectRequest/cancel happy path + 非法态 `ERR_INVALID_REQUEST_STATUS_TRANSITION` 断言 + accept 生成响应式 Visit 副作用 + 双源 reject/cancel）。

层 2 四方对照（10 维度 `state-machine-business-review-prompt.md`，dict ↔ owner-doc ↔ Bean ↔ writer）：

| 维度 | dict（`erp-mnt/request-status` 6 值） | owner-doc（§维护请求，已 Fix） | Bean（`ErpMntRequestStateMachine`） | writer（5 Processor） | 结论 |
|------|--------|--------|--------|--------|------|
| 1 状态定义 | OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED | 表已补 IN_PROGRESS（6 行） | 6 态全覆盖 | — | 一致（doc drift 已修正） |
| 2 转换完整性 | — | 图显式 7 边 + startRepair/complete 命名 | `transitions()` 7 边 | 5 动作均有 writer | 一致 |
| 3 终态/恢复 | COMPLETED/REJECTED/CANCELLED | 标注 [终态] | `terminalStatuses()`={3}，`isTerminal` | 终态无出边（矩阵测试验证） | 一致，终态不可恢复 |
| 4 异常路径 | — | reject/cancel 为异常出口 | 双源守卫 | rejectRequest/cancel Processor | 一致 |
| 5 可达性 | — | 从 OPEN 全可达 | 矩阵测试 `testReachabilityFromInitial` 全可达 | — | 无死状态 |
| 6 角色/权限 | — | 与维护访问类似（不展开） | Bean 无状态不涉权限（§2） | 权限留 xbiz/auth | 一致（M3 非保护域） |
| 7 外部依赖 | — | accept→生成 Visit 跨实体 | Bean 不持副作用（§8） | accept Processor 内 `visitBiz.save` | 副作用保留 Processor，一致 |
| 8 TODO | — | 与维护访问类似 | — | — | 不展开（非本工作项） |
| 9 场景演练 | — | happy path（OPEN→ACCEPTED→IN_PROGRESS→COMPLETED）+ reject/cancel 双源 | 矩阵 + 层 3 回归覆盖 | 层 3 测试覆盖 | 一致 |
| 10 文档一致性 | 6 值 | 表/图/Bean 对齐 | — | 全部 writer 走 Bean `targetStatus()` | 一致 |
| 11 dict 可达性 | 6 值 | — | — | 6 值均有 writer（OPEN=初始态/CRUD 创建；ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED=5 Processor） | **无 dict 死状态** |

四方对照裁决：**Verdict: pass**——无 P0/P1 漂移。IN_PROGRESS doc drift 已在 Phase 3 Fix（表补行 + 图显式边）；双源语义已裁定（见下 Decision）。

Decision（rejectRequest/cancel 双源 OPEN/ACCEPTED 语义裁定）：保持代码现状（双源 guard，`:19-21` 既有行为）。替代方案（单源收紧为仅 OPEN 或仅 ACCEPTED）被否决——会破坏既有「拒绝已受理请求」/「取消已受理请求」业务路径（accept 后请求仍可被拒绝/取消直到 startRepair 进入 IN_PROGRESS）。残留风险：无（纯对齐既有行为）。owner doc §维护请求已显式覆盖该双源声明（blockquote 标注）。

writer 盘点（含 §9.4 CRUD 路径）：5 命名动作 Processor 为矩阵治理目标路径；通用 CRUD/API 当前可写 `status` 字段（xmeta `insertable/updatable=true`，M0.1 §9.2 裁定 (c) 显式排除——命名动作路径唯一矩阵权威，CRUD 写入不在运行时强制范围，全局写锁 successor）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure auditor，新会话，非执行者）
- Audit Walkthrough: 全计划重读 + 对照实仓语义核验。(1) 退出标准 vs 实仓：`ErpMntRequestStateMachine`（6 态/7 边/5 动作/dual-source）实仓存在且矩阵与计划声明一致；`app-service.beans.xml:56-57` FQN-id 注册落地；层 1 `TestErpMntRequestStateMachine` 9 方法（无重复边/可达性/终态无出边/双源/显式守卫/终态-初始态集合）实仓存在；5 Processor（Accept/StartRepair/Complete/RejectRequest/Cancel）均 `@Inject stateMachine` + `assertCan<Action>` + `<action>TargetStatus()` 写回，无空体/`return null`/吞噬异常（anti-hollow 通过）。(2) `AbstractErpMntRequestProcessor:30-31` `@Inject ErpMntRequestStateMachine stateMachine` 包级非 private（§`@Inject` 非 private 规则通过）；共享 helper `requireRequest`/`requestDao`/`illegalRequestTransition` 保留。(3) 副作用保持：accept 生成响应式 Visit（`AcceptProcessor:28,33-42`）；complete 仅置 `completedAt` 无 `completedBy`（`CompleteProcessor:29-31`，对齐计划 Non-Goal）。(4) owner doc doc-drift Fix 落地：`docs/design/maintenance/state-machine.md:139` 补 `维修中（IN_PROGRESS）` 行 + `:122-123` 迁移图显式 `startRepair→IN_PROGRESS→complete→COMPLETED` 边 + `:130-133` blockquote 双源声明。(5) 五点一致性：Plan Status=completed / 3 Phase Status=completed / 3 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据（四方对照表）实存。(6) Deferred honesty：approveStatus/CRUD 写锁/Delta 覆盖均带 successor 裁定，无范围内缺陷降级。(7) Docs sync：补 `docs/logs/2026/2026-08-13.md` 维护域条目（先前缺漏）。Verdict: approved。

Follow-up:

- <非阻塞跟进；已确认缺陷不得出现在此处>
