# 2026-08-12-1841-3-erpmfg-forecast-state-machine-bean 制造 ErpMfgForecast 实体级状态机 Bean（M2.19）

> Plan Status: active
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.19（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（INLINE BizModel 接线 + 单实体单轴范本）
> Mission: entity-state-machine
> Work Item: M2.19
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **M2.19 归类**：M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（Forecast 头状态本身不触发业财过账；MRP 消费预测是只读聚合 `DemandAggregator:175` filter APPROVED，非头状态轴触发）。**非 plan-first**；单实体单轴，但跨模块行为变更，须独立 plan + 独立草案审查 + 独立结束审计（路线图规则 3）。
- **预测（ErpMfgForecast.status）语义**（owner doc `docs/design/manufacturing/mrp.md:88,90` + `state-machine.md:287`）：4 态 DRAFT/APPROVED/CONSUMED/CANCELLED；初始 = DRAFT；终态 = CANCELLED。命名动作：approve(DRAFT→APPROVED)、cancel(DRAFT|APPROVED→CANCELLED，多源，refuse-terminal 守卫拒绝 CANCELLED/CONSUMED)。**`CONSUMED` 为预留死状态**（owner doc `mrp.md:90` 明示 Deferred，零 writer，仅作 cancel 守卫只读引用——Decision A 已裁定保留 dict 值作预留）。
- **dict 实况（含 1 预留死状态）**：`erp-mfg/forecast-status`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:126-131`）= DRAFT/APPROVED/CONSUMED/CANCELLED 4 值，其中 **CONSUMED 预留死状态**（无 writer，owner doc Deferred 已裁定）。绑定 `ErpMfgForecast.status`（`:906 ext:dict`）。注意：该字段名为 `status`（非 `docStatus`），访问器 `getStatus()`/`setStatus()`。
- **生产 writer 实况（固定迁移判断散布，已核实）**：**仅 2 处，均 INLINE BizModel，无 Processor**：
  - `ErpMfgForecastBizModel.approve`（`entity/ErpMfgForecastBizModel.java:32-46`）：守卫 `:37 !Objects.equals(status, FORECAST_STATUS_DRAFT)`→APPROVED `:43`。
  - `ErpMfgForecastBizModel.cancel`（`:48-64`）：**refuse-terminal 守卫** `:53-54 Objects.equals(status, CANCELLED) || Objects.equals(status, CONSUMED)` 拒绝→CANCELLED `:61`（隐含接受 DRAFT/APPROVED 多源）。
  - **无私有 `illegalTransition` helper**——两处内联 `new NopException(ERR_FORECAST_ILLEGAL_STATUS_TRANSITION)`（`:38-41`/`:55-59`）。
  - **无 Forecast per-mutation Processor**（`processor/` 目录无 `ErpMfgForecast*Processor`）。**不经共享 `AbstractCancelProcessor`**（manufacturing 域无此骨架；Forecast cancel 手写于 BizModel）。
  - 只读引用（非 writer）：`DemandAggregator.java:175`、`ErpMfgReportBizModel.java:403` 均 `eq("status", APPROVED)` filter（MRP 聚合/报表只读消费）。
- **greenfield 范畴**：module-manufacturing 当前**不存在 `statemachine/` 包、不存在任何 `ErpMfg*StateMachine` Bean**（`TestErpMfgWorkOrderStateMachine` 是 Phase-2 集成测试名，非 Bean 矩阵测试；WorkOrder 状态机现为 facade+per-mutation Processor 实现，非 StateMachine Bean）。本计划为制造域首例 StateMachine Bean（跨域范式参考 cs 试点 + 已完成的 purchase/projects/contract Bean）。
- **错误码**：`ErpMfgErrors.ERR_FORECAST_ILLEGAL_STATUS_TRANSITION`（`ErpMfgErrors.java:158-161`，参数 forecastCode/currentStatus/expectedStatus）——approve 与 cancel 共享单码。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A 复用 + `action` 补充参数范式）。另 `ERR_FORECAST_NOT_FOUND`（`:153-156`）存在。
- **生产 Bean 注册范式已存在**：`module-manufacturing/erp-mfg-service/src/main/resources/_vfs/erp/mfg/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 per-mutation Processor（`:91-174`）。**当前未注册任何 Forecast Processor 或 StateMachine Bean**（实体 BizModel 经 `@BizModel` 自动发现，非显式 bean）。新 `ErpMfgForecastStateMachine` 须显式 FQN-id 注册。
- **既有层 3 回归基线（非 greenfield）**：`TestErpMfgForecastSource.testStateMachineApproveCancel`（`:62-85` approve DRAFT→APPROVED + approve APPROVED 拒绝 `ERR_FORECAST_ILLEGAL_STATUS_TRANSITION` + cancel APPROVED→CANCELLED + cancel CANCELLED 拒绝）、`TestErpMfgForecastSource` 另有 MRP 聚合用例、`TestErpMfgForecastCrudSmoke`。**已知覆盖缺口**（层 1 矩阵补）：cancel-from-DRAFT 多源分支未被层 3 直接覆盖、cancel-refuse-from-CONSUMED（死状态无 writer 难构造）。无 `TestErpMfgForecastStateMachine` 矩阵测试（greenfield 层 1）。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-manufacturing service 零违例）、R11= 0。本计划新增 1 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛至 Bean，R11 不增。

## Goals

- 落地 `ErpMfgForecastStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。为制造域首例 StateMachine Bean（建立域内范式）。
- 将 `ErpMfgForecastBizModel`（approve/cancel）的**固定来源态/目标态判断**改调 Bean（cancel 多源 + refuse-terminal 守卫经 Bean 的正向 `assertCanCancel(DRAFT|APPROVED)` 表达）；**动态业务守卫保留原位**（乐观锁；当前 approve/cancel 无额外动态守卫，保持）。
- 保持全部既有外部行为不变（错误码 + 参数、cancel 多源、refuse-terminal 语义、CONSUMED 预留死状态只读引用）。
- 新增层 1 矩阵完备性表驱动测试（greenfield）；层 3 既有集成测试回归全绿。
- 层 2 四方对照单轴裁定，**确认 CONSUMED 死状态处置一致**（owner doc Decision A 已裁定，Bean 不编码 CONSUMED 边），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不删除 dict `CONSUMED` 预留值**（保护区，owner doc Decision A 已裁定保留）。
- 不新增 `consume` 命名动作（DRAFT/APPROVED→CONSUMED 预测消费回写）——属业务行为变更 + Deferred（owner doc `mrp.md:90`），归 successor ask-first。
- 不改变任何业务状态值、动作名、错误码值/参数形状、权限（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 `ErpMfgWorkOrder`/`ErpMfgJobCard`/`ErpMfgMrpPlan`/`ErpMfgSubcontractOrder` 等其他制造域状态轴（独立工作项 M4.35-/M3.13/M3.14/M4.37-，非 M2.19 结果表面）；本计划仅交付 Forecast 轴 Bean 并建立域内 Bean 范式，不暗示其他制造轴已迁移。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）；不声称全域 Delta 覆盖已验证（本计划证 Forecast 单轴 Delta，全域归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单，落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/manufacturing/mrp.md`（§实现约定 Forecast 状态机 + CONSUMED Deferred `:88,90`）、`docs/design/manufacturing/state-machine.md`（§预留死状态指引 `:287`）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 mfg forecast 行）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 路线图 M2.19 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel 接线、Bean 注册、动态守卫边界保留、错误码映射、产品化可定制性自检」；`nop-testing` 匹配「层 1 表驱动矩阵 + 既有集成测试回归 + Delta 双加载」。层 2 引用 `state-machine-business-review-prompt.md`（模板步骤 5 标配）。必需输入已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 mfg-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（Forecast 头状态不触发过账；MRP 消费为只读聚合）。

## Execution Plan

### Phase 1 - ErpMfgForecastStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/statemachine/ErpMfgForecastStateMachine.java`（新，建立域内首例 `statemachine/` 包）；`.../beans/app-service.beans.xml`（追加 1 Bean 注册）；`TestErpMfgForecastStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [ ] `Add`：创建 `ErpMfgForecastStateMachine`（无状态，不注入 DAO/IBiz/IServiceContext/事务，§4 + §11.1 步骤 1），矩阵编码**已实现**迁移：
  - 显式动作方法：`assertCanApprove(DRAFT)`→APPROVED、`assertCanCancel(DRAFT|APPROVED)`（多源，正向表达；非法来源含 CANCELLED/CONSUMED/ERROR 等抛 common 层码）→CANCELLED。
  - 目标态方法：`approveTargetStatus()`→APPROVED / `cancelTargetStatus()`→CANCELLED。
  - 终态分类：`isTerminal(CANCELLED)`。**CONSUMED 不入终态集**（预留死状态，不可达——javadoc 标注 Decision A）。
  - 只读元数据：`transitions()`（2 边：approve/cancel）；`terminalStatuses()`(CANCELLED) + `initialStatuses()`(DRAFT)。
  - 非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.mfg.service.statemachine.ErpMfgForecastStateMachine" class="...ErpMfgForecastStateMachine"/>` 注册（沿用既有 Processor FQN-id 范式，§11.1 步骤 2；实体 BizModel 经 `@BizModel` 自动发现，Bean 须显式注册）。
      - Skill: `nop-backend-dev`
- [ ] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpMfgForecastStateMachineMatrix`，§11.1 步骤 4，不经 BizModel 入口）：(a) 无重复/冲突边；(b) 从 DRAFT 可达 APPROVED/CANCELLED（cancel 多源含 DRAFT 直达）；(c) cancel 多源 {DRAFT, APPROVED} 合法、对终态 CANCELLED 非法、**对 CONSUMED 非法**（断言 refuse-dead-state）；(d) approve 仅 DRAFT 合法、对 APPROVED/CANCELLED/CONSUMED 非法；(e) CANCELLED 终态无出边；(f) `transitions()` 元数据与显式方法语义一致；(g) 终态/初始态集合正确；(h) **CONSUMED 无任何边、不在终态集**（断言 Bean 不编码该态，javadoc 标注预留死状态）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpMfgForecastStateMachine` 落地（2 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [ ] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [ ] 层 1 矩阵测试 `mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgForecastStateMachineMatrix` 全绿。
- [ ] 本地化编译检查：`mvn compile -pl module-manufacturing/erp-mfg-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel 接线（行为保持）+ 层 3 回归

Status: planned
Targets: `entity/ErpMfgForecastBizModel.java`（approve/cancel）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] `Fix`：`ErpMfgForecastBizModel` 注入 `ErpMfgForecastStateMachine`（按类型注入，字段非 private），将 approve（`:37`）内联守卫替换为 `stateMachine.assertCanApprove(from)` + `stateMachine.approveTargetStatus()` 写回；cancel（`:53-54` refuse-terminal 守卫）替换为 `stateMachine.assertCanCancel(from)`（Bean 正向判定 {DRAFT, APPROVED}）+ `stateMachine.cancelTargetStatus()` 写回。**删除两处内联矩阵判断**；保留 `requireEntity`/`updateEntity`/乐观锁。BizModel 捕获 Bean common 层非法边映射领域 `ERR_FORECAST_ILLEGAL_STATUS_TRANSITION`（forecastCode/currentStatus/expectedStatus 参数不变，common 码作 cause——对齐契约 §7 + M1.1 Option A 范式）。
      - Skill: `nop-backend-dev`
- [ ] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿——重点 `TestErpMfgForecastSource.testStateMachineApproveCancel`（approve DRAFT→APPROVED + approve APPROVED 拒绝 + cancel APPROVED→CANCELLED + cancel CANCELLED 拒绝，错误码 `ERR_FORECAST_ILLEGAL_STATUS_TRANSITION`）、`TestErpMfgForecastSource` MRP 聚合用例、`TestErpMfgForecastCrudSmoke`。证明错误码 + 参数、cancel 多源、refuse-terminal 语义、CONSUMED 只读引用均不变。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] approve + cancel 两处固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(status, FORECAST_STATUS_*)` 矩阵判断（动态守卫如 requireEntity/乐观锁除外）。
- [ ] `ERR_FORECAST_ILLEGAL_STATUS_TRANSITION` + 参数（forecastCode/currentStatus/expectedStatus）对外不变（层 3 断言证实）；cancel 多源 + refuse-terminal 行为不变。
- [ ] 层 3 `mvn test -pl module-manufacturing/erp-mfg-service` 全绿。

### Phase 3 - 层 2 四方对照（Forecast 单轴）+ Delta 适用性

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；Forecast 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2

- [ ] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 Forecast 单轴——dict（forecast-status 4 值含 CONSUMED 预留死）↔ owner doc `mrp.md:88,90` + `state-machine.md:287` ↔ Bean `transitions()` ↔ 全部 writer（approve/cancel INLINE + 只读 filter 引用 + CRUD 路径 §9.4）。确认 dict 无新增死状态；CONSUMED 处置与 owner doc Decision A 一致。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] `Decision`（CONSUMED 死状态确认）：确认 owner doc Decision A（`mrp.md:90`）——CONSUMED 保留 dict 值作预留，零 writer，Bean 不编码边，仅作 cancel 守卫只读引用。intentional legacy / Deferred，successor = 预测消费回写需求上线时。无需 Fix owner doc（已裁定标注）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域）：经 VFS Delta 层同名 bean id 覆盖证明替换生效——派生类覆盖 `assertCanCancel`（如收紧 cancel 仅 DRAFT，移除 APPROVED 源），基线/Delta 双加载可区分（复用 M1.2/contract 范式：`TestErpMfgForecastStateMachineBaselineIoC` + `TestErpMfgForecastStateMachineDeltaOverride`）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 单轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [ ] CONSUMED 死状态处置确认与 owner doc Decision A 一致，无静默排除。
- [ ] Forecast 轴 Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: accept (2026-08-12-111827-mission-driver) because — 格式合规（全部必需段落 + front matter 字段完整；Item Types 与阶段级类型声明一致）；Exit Criteria 全部可测（具体 mvn 命令 + grep 断言 + 证据要求）；单结果表面边界清晰（Forecast 单实体单轴，Non-Goals 显式排除其他制造轴/consume 动作/CONSUMED dict 删除/全局写锁）；阶段退出正确使用本地化检查（Phase 1 `-am` 编译解锁 Phase 2；Phase 2 模块测试证明行为保持）而全仓库验证留在 Closure Gates，符合指南执行规则 7；CONSUMED 预留死状态处置完整（owner doc Decision A 引用 + 层 1 矩阵 refuse-dead-state 断言 + Phase 3 Decision 项 + Deferred 命名继任触发条件，无静默排除）；Current Baseline 从实时仓库文件:行号盘点编写（指南规则 1）；Closure Gates 含合规检查器命令，覆盖已知失败模式（compliance 基线漂移 + R5/R11）；反松弛规则无违例。无 Blocker/Major 问题，Minor 留待下游结束审计/深度审计。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [ ] 范围内行为完成（Forecast 单轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [ ] 相关文档对齐（CONSUMED 死状态 Decision A 确认；路线图 M2.19 done）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-manufacturing/erp-mfg-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### CONSUMED 预测消费回写（consume 命名动作）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc `mrp.md:90` Decision A 已裁定 Deferred——CONSUMED 保留 dict 值作预留，零 writer，本期不自动迁移。补 consume mutation 属业务行为变更，归 successor。
- Successor Required: yes（触发条件 = 预测消费后状态回写需求上线时，开独立 plan 新增 consume mutation，可能触及 dict 行为 ask-first）

### 制造域其他状态轴（WorkOrder/JobCard/MrpPlan/SubcontractOrder 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 独立实体独立 Bean（M4.35-/M3.13/M3.14/M4.37- 等），非 M2.19 结果表面。本计划仅交付 Forecast 轴 Bean。
- Successor Required: yes（触发条件 = 各对应 M3/M4 工作项启动时）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 选项 (c) 显式排除；更强写锁须改 ORM/xmeta ask-first。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: <待执行 + 独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理填充>
- Evidence: <待填充>

Follow-up:

- <待填充；CONSUMED Decision A 确认为范围内项，Deferred 项见上>
