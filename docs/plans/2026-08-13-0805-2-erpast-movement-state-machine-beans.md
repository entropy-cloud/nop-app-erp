# 2026-08-13-0805-2-erpast-movement-state-machine-beans 资产移动单状态机 Bean

> Plan Status: active
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M3.15（docStatus）+ M3.16（approveStatus）；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.4`
> Related: `2026-08-13-1430-3-erpct-version-rebate-state-machine-beans.md`（退化轴 Decision 先例：RebateAgreement）、`2026-08-13-0805-1-erpmnt-request-state-machine-bean.md`（同批 M3 迁移）
> Audit: required

## Current Baseline

- **实体与双轴**：`ErpAstMovement`（`module-assets/model/app-erp-assets.orm.xml:414-415`）：
  - `docStatus`：`ext:dict="erp/doc-status"`（DRAFT/ACTIVE/CANCELLED）。
  - `approveStatus`：`ext:dict="wf/approve-status"`（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
- **实现位置（关键差异点）**：Movement 状态逻辑 **100% INLINE 在 xbiz `<source>` 脚本**（`module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstMovement/ErpAstMovement.xbiz`，159 行），**无 `ErpAstMovement*Processor` Java 类**（glob 零命中）。`ErpAstMovementBizModel`（17 行）为空 CrudBizModel 桩。这是本批迁移中**首个 INLINE→Bean 迁移**（此前 M3 先例均为 Processor 接线）。
- **approveStatus（M3.16）5 命名动作**（全部 inline，`xbiz` 行号为现状证据）：
  - `submitForApproval`（`:5-33`）：guard `approveStatus !== UNSUBMITTED && !== null && !== REJECTED` → `entity.approveStatus = 'SUBMITTED'`（`:30`）。
  - `approve`（`:34-65`，`<auth permissions="ErpAstMovement:approve"/>`）：guard `!== SUBMITTED` → `'APPROVED'` + approvedBy/approvedAt（`:60-62`）。
  - `reject`（`:66-96`）：guard `!== SUBMITTED` → `'REJECTED'` + approvedBy/approvedAt（`:91-93`）。
  - `reverseApprove`（`:97-128`，`<auth permissions="ErpAstMovement:reverseApprove"/>`）：guard `!== APPROVED` → `'REJECTED'` + 置空 approvedBy/approvedAt（`:123-125`）。
  - `withdrawApproval`（`:129-157`）：guard `!== SUBMITTED` → `'UNSUBMITTED'`（`:154`）。
  - 5 动作均前置 `if (entity.docStatus === 'CANCELLED') throw nop.err.wf.approve.doc-cancelled` 守卫（`:13,:43,:74,:106,:137`）。
- **docStatus（M3.15）退化轴**：5 inline 动作**仅读** `docStatus === 'CANCELLED'` 作防御守卫，**从不写 docStatus**。生产 Java 与 xbiz 全域对 Movement **零 `setDocStatus` writer**（对比 Split/Merge/Disposal/ValueAdjustment/Capitalization 均有 `setDocStatus(ACTIVE/CANCELLED)` writer，Movement 是 assets 域唯一无 docStatus writer 的单据）。`docStatus=CANCELLED` 经 `useLogicalDelete` 承载（owner doc `assets/state-machine.md`「实现模式与守卫边界」已声明）。`ACTIVE` 在 dict 内但 Movement 无 writer → ACTIVE 为 Movement 的死状态（对齐 assets 域 R1.x 保留死状态先例）。
- **无过账副作用**：owner doc 明确「资产域移动单无 posted 副作用（不过账、无凭证需 reverse）」；`reverseApprove` 目标态为 REJECTED（与其他域对齐）。无 reversal listener。
- **owner doc 现状**：`docs/design/assets/state-machine.md` 仅在「实现模式与守卫边界」散文段提及 Movement INLINE 路径，**无编号 §适用对象矩阵章节**（§适用对象仅覆盖资产卡片 Asset）。Movement 缺矩阵化 owner doc 章节 → Phase 3 需新增 §适用对象章节（对齐 contract 先例补 §适用对象二/三）。
- **M0.2 分类**：M3.15/M3.16 归 M3（非保护、无财务影响）。inventory 行 M3.15=`DRAFT→ACTIVE→CANCELLED（无 reversal listener）`、M3.16=`UNSUBMITTED→SUBMITTED→APPROVED/REJECTED`，财务/跨域/过账均 = `无`。M1.3（done）+ M3.15（docStatus 先于 approveStatus）Deps 已就绪。

## Goals

- 新建无状态 `ErpAstMovementApprovalStateMachine`（approveStatus 5 动作矩阵）+ `ErpAstMovementDocumentStateMachine`（docStatus 退化分类轴）双 Bean，遵循 `entity-state-machine-bean.md §3` 双轴各自独立 Bean + §1 `Document`/`Approval` 后缀命名。
- 将 `ErpAstMovement.xbiz` 5 个 inline 固定守卫 + 目标态回写接线为 Bean 委托（**首个 INLINE→Bean 迁移范式**，验证 xbiz `<source>` 内可注入并调用 Bean）；docStatus 的 `isCancelled` 防御守卫委托 `ErpAstMovementDocumentStateMachine.isCancelled(status)`。行为/错误码/权限/`<auth>` 声明保持不变。
- 裁决 docStatus 退化轴（ACTIVE 死状态 + 无命名动作 writer）并落地退化分类 Bean（`transitions()` 空，对齐 RebateAgreement 先例）。
- 层 1（双轴矩阵完备性）+ 层 2（四方对照 + 退化轴/死状态 Decision）+ 层 3（既有 INLINE 动作回归）三层证据。
- owner doc 新增 Movement §适用对象矩阵章节（docStatus 退化轴 + approveStatus 5 动作）。

## Non-Goals

- 不给 docStatus 退化轴新增 `setDocStatus` writer 或 `cancel` 命名动作（docStatus=CANCELLED 经 useLogicalDelete 既有路径；ACTIVE 死状态保留为预留语义入口，不从 ORM 删除）。
- 不迁移 `posted`（assets Movement 无 posted 副作用；Non-Goals：posted 不作 StateMachine 轴）。
- 不改变 xbiz 的 `<auth permissions>` 声明、错误码、`<x:extends>` 继承结构。
- 不修改 `model/*.orm.xml`、字典值或 API 契约。
- 不引入通用 CRUD 对 status 写入的运行时禁止（M0.1 successor）。

## Task Route

- Type: `implementation-only change`（固定迁移矩阵集中化 + 首个 INLINE→Bean 迁移范式验证，不改外部行为/契约/模型）
- Owner Docs: `docs/design/assets/state-machine.md`（新增 Movement §适用对象章节）、「实现模式与守卫边界」段、`docs/architecture/entity-state-machine-bean.md`（§1 命名、§2 无状态、§3 双轴分离、§11 批量迁移模板）
- Skill Selection Basis: 本项是「xbiz inline 守卫改 Bean 委托 + 退化轴裁定」后端开发，匹配 `nop-backend-dev`；矩阵测试与回归匹配 `nop-testing`；Phase 3 四方对照 + 退化轴/死状态裁定匹配 `state-machine-business-review-prompt.md`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 无数据迁移。

## Execution Plan

### Phase 1 - ErpAstMovementApprovalStateMachine + ErpAstMovementDocumentStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstMovementApprovalStateMachine.java`、`.../ErpAstMovementDocumentStateMachine.java`（新建）、`module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/beans/app-service.beans.xml`（注册）、`module-assets/erp-ast-service/src/test/.../statemachine/TestErpAstMovementStateMachines.java`（新建）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建无状态 `ErpAstMovementApprovalStateMachine`（approveStatus 轴，§1 `Approval` 后缀，§2 无状态约束）：
      - 5 动作矩阵：`submitForApproval`：源 `{UNSUBMITTED, null, REJECTED}` → `SUBMITTED`；`approve`：源 `{SUBMITTED}` → `APPROVED`；`reject`：源 `{SUBMITTED}` → `REJECTED`；`reverseApprove`：源 `{APPROVED}` → `REJECTED`；`withdrawApproval`：源 `{SUBMITTED}` → `UNSUBMITTED`。
      - 分类：initial=`{UNSUBMITTED}`，terminal=`{APPROVED}`（APPROVED 无 approveStatus 出边；REJECTED 经 submitForApproval 可重新进入 → 非终态，对齐现状 guard）。
  - Skill: `nop-backend-dev`
- [ ] Decision + 新建无状态 `ErpAstMovementDocumentStateMachine`（docStatus 退化分类轴，对齐 RebateAgreement 退化 Bean 先例）：`transitions()` 返回空（无命名动作 writer）；`initialStatuses()={DRAFT}`；`terminalStatuses()={CANCELLED}`；提供 `isCancelled(status)` 只读守卫 helper（供 xbiz 5 动作前置守卫委托）；记录 ACTIVE 为死状态（dict 内有值、Movement 无 writer，保留为预留语义入口，对齐 assets 域保留死状态先例）。理由：docStatus 经 useLogicalDelete 走 CANCELLED，无独立 cancel mutation，退化轴不发明迁移边。
  - Skill: `state-machine-business-review-prompt.md` | `nop-backend-dev`
- [ ] 在非生成 `app-service.beans.xml` 以 FQN 为 bean id 注册双 Bean
  - Skill: `nop-backend-dev`
- [ ] Proof（层 1 矩阵完备性，双轴表驱动）：`TestErpAstMovementStateMachines`——Approval 轴遍历 dict 值 × 5 动作合法/非法边 + `transitions()` 全 5 边 + initial/terminal；Document 退化轴断言 `transitions()` 空 + `isCancelled(CANCELLED)=true` + ACTIVE 死状态无 writer（机器化核对）。验证命令：`mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstMovementStateMachines`
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 双 Bean 无状态、矩阵/分类完整；Approval 轴 5 动作合法/非法边与现状 guard 一致；Document 退化轴 Decision 落地（ACTIVE 死状态裁决记录）
- [ ] 层 1 双轴表驱动测试通过

### Phase 2 - xbiz INLINE 接线（行为保持）+ 层 3 回归

Status: planned
Targets: `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstMovement/ErpAstMovement.xbiz`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 双 Bean 落地

- [ ] 将 `ErpAstMovement.xbiz` 5 动作 inline 固定守卫 + 目标态回写替换为 Bean 委托：在 `<source>` 内经 `ioc` 取得双 Bean（`nop-backend-dev` 决策门确认 xbiz source 注入 Bean 的合规方式），`approveStatus` 守卫 → `approvalStateMachine.assertCan<Action>(entity.approveStatus)`，目标态 → `entity.approveStatus = approvalStateMachine.targetStatusFor<Action>()`；`docStatus === 'CANCELLED'` 防御守卫 → `documentStateMachine.isCancelled(entity.docStatus)`。**保留**：`<auth permissions>` 声明、`approvedBy/approvedAt` 置位与置空、错误码 `nop.err.wf.approve.*`、`<x:extends="_ErpAstMovement.xbiz">` 继承结构
  - Skill: `nop-backend-dev`
- [ ] Proof（层 3 回归）：运行 Movement 既有动作测试（含 `TestErpAstMovementReverseApprove` happy path + 非法态拒绝 + docStatus=CANCELLED 防御守卫 + reverseApprove→REJECTED 置空 approvedBy/approvedAt）。执行前确认既有测试是否已覆盖 `withdrawApproval` 与全部 5 处 CANCELLED 守卫；若有缺口，在本阶段补回归用例。验证命令：`mvn test -pl module-assets/erp-ast-service`
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 5 inline 动作接线后既有测试全绿（行为、错误码、权限守卫、docStatus 防御守卫无回归）

### Phase 3 - 层 2 四方对照 + 退化轴/死状态 Decision + owner doc 补 §适用对象章节

Status: planned
Targets: `docs/design/assets/state-machine.md`（新增 Movement §适用对象章节 + 退化轴/死状态 Decision 登记）、本计划 Closure
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [ ] Proof（层 2 四方对照，10 维度）：approveStatus 轴 dict（`wf/approve-status`）↔ owner doc ↔ Approval Bean ↔ writer（xbiz 5 动作）；docStatus 退化轴 dict（`erp/doc-status`）↔ owner doc ↔ Document Bean ↔ writer（零命名动作 writer，CANCELLED 经 useLogicalDelete）
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：`docs/design/assets/state-machine.md` 新增 Movement §适用对象章节——approveStatus 5 动作矩阵（状态定义/迁移完整性/终态/异常/可达性）+ docStatus 退化轴声明（无命名动作 writer，CANCELLED 经 useLogicalDelete，ACTIVE 死状态保留）；在「实现模式与守卫边界」段补注 INLINE→Bean 迁移已完成
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision：登记 docStatus ACTIVE 死状态裁决（保留为预留语义入口，不从 ORM 删除，successor=资产移动单独立 cancel/activate 工作流时）+ 退化轴 Bean 形状（transitions 空，对齐 RebateAgreement 先例）+ reverseApprove→REJECTED 目标态裁定（与其他域对齐，owner doc §16.4）
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移；退化轴/死状态/reverseApprove 目标态 Decision 均落入 owner doc 或计划
- [ ] owner doc 新增 Movement §适用对象章节与 dict/Bean/xbiz 一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_0076e4ba6ffeb23LuxGMLLg6cJ` alt / `ses_0076e256effeeLxxY25GYMdlk4`) — 11 项 load-bearing baseline 全部经 live repo 核实（含「Movement 零 docStatus writer」退化轴 + INLINE→Bean 首迁移 + RebateAgreement 退化 Bean 先例）。无 blocker、无 major。v2 仅应用 polish：beans.xml 路径订正 `_vfs/erp/ast/beans/`、reject baseline 补 approvedBy/approvedAt、Phase 2 测试覆盖补 withdrawApproval/5×CANCELLED 守卫核对说明。

## Closure Gates

> 完整仓库验证在此处运行一次。无 ORM/API/字典变更（ACTIVE 死状态保留不删），Compliance 基线预期无漂移。

- [ ] 范围内行为完成（双 Bean + INLINE 接线 + 三层证据）
- [ ] 相关文档对齐（owner doc 新增 Movement §适用对象章节 + 退化轴 Decision）
- [ ] 已运行验证：`mvn test -pl module-assets/erp-ast-service` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### docStatus ACTIVE 死状态 + 独立 cancel/activate 动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Movement docStatus 无命名动作 writer，ACTIVE 为 dict 内死状态（保留为预留语义入口）。本计划仅集中既有固定矩阵（approveStatus 5 动作），不发明 docStatus 迁移边。
- Successor Required: `yes`（资产移动单独立 cancel/activate 工作流需求时，新增 `cancel`/`activate` BizMutation + docStatus 迁移矩阵）

### Movement 的 `posted` 业财过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 明确 Movement 无 posted 副作用（不过账、无凭证）。`posted` 非 StateMachine 轴（Non-Goals）。
- Successor Required: `no`

### 通用 CRUD 对 status 写入的运行时禁止 / Delta 覆盖运行时证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: 同 M3 批量先例（M0.1 CRUD 写入边界 successor；M1.2 已验证 Delta 覆盖，逐项不重复）。
- Successor Required: `no`（Delta 覆盖归 M5.3）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- <非阻塞跟进；已确认缺陷不得出现在此处>
