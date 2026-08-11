# 2026-08-12-0617-1-entity-state-machine-m0-1-contract 实体级 StateMachine Bean 契约定稿（M0.1）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Mission: entity-state-machine
> Work Item: M0.1
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M0.1（todo）；`docs/analysis/2026-08-06-1000-erp-state-machine-extension-strategy.md`
> Related: `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（N=2，依赖本计划产物）；`docs/architecture/processor-extension-pattern.md`、`docs/architecture/customization-capabilities.md`
> Audit: required

## Current Baseline

- `nop-entropy` 将 job fire/task/schedule 的状态判断集中在**静态 `final` 类**（`JobFireStateMachine` 等）；这些是无状态、无客户化需求的纯工具。ERP 业务状态机尚未建立对应 Bean——`grep -rln "StateMachine" module-*/ --include="*.java" | grep -v /target/ | grep -v /src/test/` 在生产源码**零命中**（即无生产 `Erp.*StateMachine` Bean 类）。注意：已有 **8 个 `TestErp*StateMachine` 集成测试类**存在于 `src/test/`（finance 3：Period/NotesPayable/NotesReceivable；maintenance VisitRequest；manufacturing WorkOrder；master-data SupplierApproval；quality Inspection/Recall），它们经 BizModel 入口断言状态迁移，是命名动作回归层的既有基线（见 Phase 2 测试策略），**非** Bean 类。
- ERP 的固定状态迁移判断当前**散布在三处**：BizModel 的 `validateTransitionForXxx` 守卫、per-mutation `<Entity><Action>Processor` 的内联 `Objects.equals(getXxxStatus(), CONSTANTS.YYY)`（`processor-extension-pattern.md §状态判断方法的复用约定` 记录 `APPROVED` 比较在 22 Processor 累计 132 次）、以及 facade helper。
- per-mutation Processor 已在非生成 `app-service.beans.xml` 显式注册（范例 `module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/beans/app-service.beans.xml`：`<bean id="<FQN>" class="<FQN>"/>` 或 `ioc:type="@bean:id"`），是本路线图 StateMachine Bean 的注册范式来源。
- 平台 Delta 同名 Bean 覆盖机制存在（`../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md`），但本项目**业务级 Delta 实证 = 0**（`customization-capabilities.md` 实证注记：仅 2 个平台层 nop-auth view delta）。业务级 Bean Delta 覆盖的运行时实证由 M1.2 负责，本计划不越权声称已验证。
- 三轴状态分离已是稳定设计约定（`domain-design-guidelines.md §16.1` 双轴 `docStatus` + `approveStatus`，§16.1 末尾「三轴状态」追加业财标志 `posted` 为第三轴；`posted` 是业财过账/红冲/物理锁定契约）。`posted` 不作为普通状态边——本路线图 Non-Goal 已确认。
- 17 个域有独立 `docs/design/<domain>/state-machine.md`（master-data/notify 的状态语义在各自 README）；这些是业务迁移语义的设计 owner，但**机器可读的迁移矩阵、状态分类、可达性元数据当前不存在**，无法系统回答死状态/可达性/终态出边问题。
- 已知风险（路线图基线）：dict 死状态——字典、owner doc 迁移图与生产 `setStatus` writer 可能漂移。本契约不解决漂移（归 M0.2/M5），但必须为 M0.2/M5 的完备性分析提供**机器可读元数据接口**。
- **M0.1/M0.2 是路线图硬门控**：在 M0.1 契约定稿 + M0.2 清单展开均通过审查前，任何代码迁移项（M1.1 及以后）不得转为 `ready`/`active`（路线图规则 2、Work Item Status 块）。

## Goals

- 产出一份**稳定技术架构 owner doc**（`docs/architecture/entity-state-machine-bean.md`），固化实体级 `ErpXxxStateMachine` Bean 的契约：颗粒度/命名、状态轴边界、无状态约束、方法形状、Bean ID 与注册、Delta 同名覆盖、错误/拒绝语义、与 Processor/task.xml/SPI 的分工边界表、CRUD 写入边界裁定、测试策略分层。
- 以**最小运行时探针**证明契约的机制可行性（IoC 注册、注入、非法边报告），不迁移任何真实业务实体、不证明业务级 Delta 覆盖（后者归 M1.2）。
- 在 `processor-extension-pattern.md` 与 `customization-capabilities.md` 增交叉引用，登记 StateMachine Bean 作为「拓扑稳定固定迁移矩阵」的承载者与 Delta 覆盖定制点（实证状态按各文档声明规范标注，业务级实证指向 M1.2 successor）。
- 路线图 M0.1 状态流转与 Work Item 表一致。

## Non-Goals

- 不迁移任何真实业务实体的状态机（含客服 Ticket 试点——归 M1.1）。
- 不证明业务级 Delta 同名 Bean 覆盖的运行时行为（归 M1.2；本计划最多声明平台机制可用）。
- 不修改任何 `model/*.orm.xml` / `model/*.api.xml`；不新增生产 `daoFor`/`import`（探针须测试作用域，见 Infrastructure 节）。
- 不建立覆盖全部实体的反射型/泛型全局 `IStateMachine` 运行时调度器（路线图 Non-Goal）。
- 不把 `docStatus`/`approveStatus`/`posted` 合并为笛卡尔积大状态机；`posted` 不作迁移轴（路线图 Non-Goal + 分析 §4）。
- 不展开全域状态轴清单与 M2/M3/M4 工作项（归 M0.2）。
- 不改动既有业务状态值、动作名、错误码、权限、过账时序或审批语义。

## Task Route

- Type: `architecture change`（新增稳定架构契约 + 配置余地登记）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（StateMachine Bean 嵌入 Processor 编排点）、`docs/architecture/customization-capabilities.md`（Delta 覆盖定制点）、`docs/design/domain-design-guidelines.md §16`（三轴分离——契约须与之对齐，本文不改动它）、`docs/analysis/2026-08-06-1000-erp-state-machine-extension-strategy.md`（策略输入）
- Skill Selection Basis: 本计划产物是架构决策文档而非业务状态机审查，故 `Skill: none`（`state-machine-business-review-prompt.md` 匹配的是 M0.2 清单审查与各迁移项的业务正确性审查，不匹配本契约定稿）。**注**：路线图 M0.1 行 Skill 列填的是 `state-machine-business-review-prompt.md`，本计划有意覆盖之——该技能的工作方法是「状态机业务正确性审查」（`docs/skills/README.md` 注册表：「工作流状态机…需要正确性审查」），与契约定稿/架构决策任务不匹配；契约定稿落地的最小探针仅复用平台 IoC/测试基线，无匹配技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无端口/密钥/外部服务）。
- 探针落点约束：必须**测试作用域**（`src/test/` 下，含测试用 `_vfs/.../beans.xml` 或 `@NopTestConfig` 测试 beans import），**不得**新增生产代码、生产 `app-service.beans.xml` 条目或生产 `daoFor`/`import`。若最小探针证明必须经生产 beans.xml 才能在真实容器加载，则**停止并 ask-first**（ORM/beans 生产层属保护区域），不得自主放宽。

## Execution Plan

### Phase 1 - 契约决策与探索（CRUD 写入边界 + 工具对比）

Status: completed
Targets: `docs/architecture/entity-state-machine-bean.md`（新）；探索产物纳入文档决策段
Skill: none

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] `Explore`：扫描当前对业务状态字段的**全部写路径**，区分三类 writer——生产命名动作（BizModel/Processor）、框架入口（标准 CRUD `__save`/`save`/`update`）、测试 fixture。重点回答：通用 CRUD/API 当前是否能直接写入业务状态字段（`status`/`docStatus`/`approveStatus`）？是否有现存 `__save` 写状态的既有行为会被「禁止」破坏？
  - Skill: none
- [x] `Decision`（CRUD 写入边界）：基于探索证据，在三选项中裁定并记录选择 + 考虑的替代方案 + 残留风险：
  - (a) **禁止**：通用 CRUD/API 不得写业务状态字段（经 xmeta/xbiz save-guard 或字段写保护强制）；
  - (b) **限制**：仅允许创建时写初始状态（NEW/DRAFT），所有迁移归命名动作；
  - (c) **显式排除**：声明 CRUD 可写但 StateMachine Bean 是命名动作迁移矩阵的唯一权威来源，运行时唯一性是「命名动作路径」声明而非全局写锁。
  - 此裁定直接决定后续迁移项能宣称多强的「唯一矩阵」运行时保证，必须在文档中给出可复核理由。Skill: none
- [x] `Decision`（颗粒度/命名/无状态约束）：固化 `Erp<Domain><Entity>[<Axis>]StateMachine`——一个 Bean 对应一个实体的一条状态轴；Bean 不注入 DAO/`I*Biz`/`IServiceContext`，只接收状态值与必要显式业务参数。记录与 nop-job 静态 `final` 类的取舍理由（ERP 迁移矩阵有行业差异、需 Delta 可覆盖）。Skill: none
- [x] `Decision`（方法形状）：裁定 Bean 暴露形式——显式动作方法（`assertCan<Action>(status)` + `<action>TargetStatus()` + `isTerminal(status)`）为主路径；并**额外**提供只读 `List<TransitionDefinition>` 元数据接口，仅服务于 M5.1/M5.2 可达性/完备性检查与文档一致性校验，不要求 Processor 经泛型接口调用。记录「显式方法优先于反射泛型」的理由（调用点/状态图/定制点可读）。Skill: none
- [x] `Decision`（三轴边界）：固化三轴分离——`docStatus` 与 `approveStatus` 各自独立 Bean（或独立轴），`posted` **不**作为 StateMachine 迁移轴（业财过账/红冲/物理锁定契约）。引用 `domain-design-guidelines.md §16` 为约束来源，不重复其正文。Skill: none
- [x] `Decision`（错误/拒绝语义）：裁定非法边报告方式——StateMachine 报告非法边（返回 false/目标态或抛通用非法迁移异常），**领域 ErrorCode、实体编号与上下文参数由 Processor 保留与映射**；common 层错误码不得抹平领域语义。记录拒绝元数据形状（来源态/目标态/动作名）以便 M5.2 守卫消费。Skill: none
- [x] `Decision`（分工边界表）：在文档中固化「何时用 StateMachine Bean / 静态辅助类 / Processor protected hook / 窄 SPI / task.xml」边界表（输入来自分析 §5），并登记不适用场景。Skill: none

Exit Criteria:

> 本阶段交付一份内部自洽、决策可复核的架构契约草案（文档文件已创建，含上述全部 Decision 段及探索证据摘要）。后续阶段依赖其方法形状与 CRUD 边界裁定，故阶段退出含文档存在性 + 决策完整性自检。

- [x] `docs/architecture/entity-state-machine-bean.md` 已创建，含：颗粒度/命名、状态轴边界、无状态约束、方法形状（含只读元数据接口）、Bean ID 与注册、Delta 同名覆盖、错误/拒绝语义、分工边界表、CRUD 写入边界裁定（含探索证据与替代方案）、测试策略分层 共 10 节。
- [x] CRUD 写入边界 Decision 已记录选择 + 至少 2 个被否替代方案 + 残留风险 + 对「唯一矩阵」宣称强度的影响声明。

### Phase 2 - 注册/Delta/测试策略定稿 + 最小运行时探针

Status: completed
Targets: `docs/architecture/entity-state-machine-bean.md`（补全注册/Delta/测试节）；探针测试（测试作用域）
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 的方法形状与 CRUD 边界裁定已落文档

- [x] `Decision`（Bean ID 与注册）：固化注册范式——在非生成 `app-service.beans.xml` 注册，沿用本项目既有 `<bean id="<FQN>" class="<FQN>"/>` 范式（证据：cs beans.xml）；Processor 内**按类型注入** `ErpXxxStateMachine`；`@Inject` 字段不得 `private`（平台规则 + `nop-backend-dev` 反模式）。记录「按类型注入 vs 按 id 注入」的取舍。Skill: none
- [x] `Decision`（Delta 同名覆盖语义）：固化客户替换矩阵路径——Delta `_vfs/_delta/{deltaDir}/.../beans/app-service.beans.xml` 以**同名 bean id** 注册派生/替换 Bean 覆盖基线（机制平台权威 `delta-customization.md`）。**声明**：业务级 Delta 运行时实证为 successor，由 M1.2 在真实应用容器证明；本契约只声明平台机制可用，不得声称已验证。Skill: none
- [x] `Decision`（测试策略分层）：固化三层测试义务，供后续每个迁移项与 M5.2 复用：
  1. 矩阵完备性（每个动作来源/目标态、无重复/冲突边、终态无出边、从初始态可达性）；
  2. dict ↔ owner-doc 迁移图 ↔ StateMachine 元数据 ↔ 生产 `setStatus` writer 四方对照（直接服务于 dict 死状态检测）；
  3. Delta 覆盖（M1.2/M5.3 证明替换生效）+ 既有命名动作回归（证明 Processor 写回/审计/副作用不变）。
  **既有基线登记**：层 3 的命名动作回归**并非 greenfield**——前述 8 个 `TestErp*StateMachine` 集成测试（finance/maintenance/manufacturing/master-data/quality）已通过 BizModel 入口断言状态迁移，构成该层既有基线；新增矩阵测试为层 1，二者关系须在文档中明确（既有 = 层 3 回归；新 = 层 1 矩阵），避免执行者误将层 3 当空白重建。Skill: none
- [x] `Add | Proof`（最小运行时探针）：在**测试作用域**实现一个合成的 `ErpXxxStateMachine` 形状 Bean（不绑定任何真实业务实体），经测试 beans.xml 由真实 IoC 容器加载，并断言：(1) Bean 可解析；(2) Processor 测试桩按类型注入成功；(3) 非法边经 `assertCan<Action>` 报告并由 Processor 桩映射为一个领域 ErrorCode。明确**不**断言 Delta 覆盖（M1.2 职责）。探针不得引入生产代码或生产 beans 条目。Skill: none

Exit Criteria:

- [x] 文档注册/Delta/测试三节已补全，且 Delta 节明确标注「业务级实证 successor = M1.2」，未声称已验证。
- [x] 探针测试在真实 IoC 容器下通过（注入解析成功 + 非法边报告 + ErrorCode 映射可观测）；`mvn test -pl <探针所在测试模块> -am` 绿，且 `git diff` 证实零生产代码/生产 beans 变更（`docs/audits/nop-compliance-checker.sh` 无 actual > baseline 漂移）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00d140c6bffehNC3b2hJcjN0tc`) — 1 BLOCKER（基线 grep 字面误称：生产 Bean 零命中为真，但裸 `grep -r "Erp.*StateMachine" module-*` 实际命中 8 个 `TestErp*StateMachine` 测试类）+ 1 MAJOR（Phase 2 测试策略层 3 未登记既有 8 个集成测试基线，致执行者可能误作 greenfield）+ 2 MINOR（路线图 Skill 列覆盖未显式说明 / §16 引用不精确）。修订：基线段改用排除 test+target 的精确 grep 并明示 8 个既有测试类；Phase 2 测试策略增「既有基线登记」段；Task Route Skill Selection 增路线图 Skill 覆盖说明；§16 引用改为 §16.1 + 三轴末尾行；Deferred 分类 `successor-owned`→`out-of-scope improvement`。
- Independent draft review iteration 2: `accept` (`ses_00d0f6092ffeTaO77cJ2bU579A`) — BLOCKER 与 MAJOR 均确认解决（基线 grep 改为排除 test+target 的精确命令并明示 8 个既有测试类；Phase 2 测试策略「既有基线登记」段落地）；MINOR 全部处理（Skill 覆盖说明 + §16.1 引用精确化 + Deferred 分类合规）；残余 1 cosmetic MINOR（§16 末尾→§16.1 末尾）已随后修正。完整性复扫全部通过（探针测试作用域 + ask-first 守卫、不迁移真实实体、Delta 证明归 M1.2、CRUD 边界未预判、硬门控保留）。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含少量测试作用域代码（探针）+ 架构文档。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（架构契约文档 10 节齐全；探针测试绿且仅测试作用域）
- [x] 相关文档对齐（`processor-extension-pattern.md` 增 StateMachine Bean 嵌入点交叉引用；`customization-capabilities.md` 登记 Delta 覆盖定制点并按其声明-实证规范标注业务级实证 successor=M1.2；两处不引用本路线图执行状态——架构文档只记稳定模式）
- [x] 已运行验证：`mvn clean install -DskipTests`（探针编译通过、无生产代码回归）+ 探针所在模块 `mvn test`；`bash docs/audits/nop-compliance-checker.sh` 无 actual > baseline 漂移（探针零生产代码）
- [x] 无范围内项目降级为 deferred/follow-up（CRUD 边界 Decision 必须落地为裁定，不得悬置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志、路线图 Work Item 表均一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中
- [x] 路线图 `entity-state-machine-migration-roadmap.md` M0.1 行状态与本计划一致（独立闭包审计后 `todo→ready→done` 由路线图状态机规则决定；本计划只保证自洽）

## Deferred But Adjudicated

### 业务级 Delta 同名 Bean 覆盖运行时实证

- Classification: `out-of-scope improvement`（指南模板仅允许 `watch-only residual | optimization candidate | out-of-scope improvement`；此项是真正的后续验证工作，非优化候选，故取 `out-of-scope improvement`）
- Why Not Blocking Closure: 平台 Delta 机制可用（平台层实证）；业务级 Bean Delta 覆盖的运行时证明是 M1.2 的硬交付（路线图 M1.2 + 规则 6「M1.2 成功证明前不得声称业务级 Delta 已验证」）。本契约只声明机制与覆盖路径，不越权声称已验证。
- Successor Required: yes（触发条件 = M1.1 试点 Bean 落地后，由 M1.2 在真实容器证明基线/Delta 两加载结果）

### 泛型状态机审计工具的统一遍历

- Classification: `optimization candidate`
- Why Not Blocking Closure: 只读 `List<TransitionDefinition>` 元数据接口已为 M5.1/M5.2 预留；是否构建统一遍历工具由 M5.2 按实际误报裁决决定。
- Successor Required: yes（触发条件 = M5.2 守卫落地时）

## Closure

Status Note: 独立结束审计 PASS（无 blocker）。两 Phase 全部执行完毕、全绿；架构契约 10 节落地；探针测试作用域零生产代码；交叉引用已登记；`mvn clean install -DskipTests` BUILD SUCCESS；`nop-compliance-checker.sh` exit 0（无 actual > baseline 漂移）。业务级 Delta 覆盖运行时实证为 successor（= M1.2），本契约不越权声称已验证。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，`ses_00cf64f9affeZ55oY7HG4edcJG`）
- Verdict: `CLOSURE AUDIT: PASS`（6 个审计域全部 PASS，0 blocker）
- Evidence:
  - 文档：`docs/architecture/entity-state-machine-bean.md` 10 节齐全；§9 CRUD 裁定=(c) 显式排除 + 否决替代 (a)/(b) + §9.4 残留风险与「唯一矩阵」宣称强度声明；§6 Delta 标注 successor=M1.2，未声称已验证。
  - 探针：`module-cs/erp-cs-service/src/test/` 下合成 `ErpProbeStateMachine` + `ProbeProcessorStub` + `TestErpProbeStateMachineContract`（6 tests, 0 failures）+ `test-probe-statemachine.beans.xml`；按类型注入（非 private `@Inject`）；非法边→领域 ErrorCode 映射可观测；不断言 Delta。
  - 零生产代码：`git diff --stat HEAD -- '*/src/main/*' '*.orm.xml' '*.api.xml'` 空；生产 `app-service.beans.xml` 未改。
  - 交叉引用：`processor-extension-pattern.md` 增「固定迁移矩阵的承载者：实体级 StateMachine Bean」节 + 关系表行；`customization-capabilities.md` 登记 Delta 覆盖定制点（successor=M1.2）+ 决策矩阵行；两处仅记稳定模式。
  - 验证：`mvn test -Dtest=TestErpProbeStateMachineContract` → 6/6 绿 BUILD SUCCESS；`bash docs/audits/nop-compliance-checker.sh` exit 0；`mvn clean install -DskipTests` BUILD SUCCESS（全 reactor）。
  - Non-Goals：无真实实体迁移；无 ORM/API 改动；`posted` 声明不作迁移轴（契约 §3）。
  - 一致性：Phase 1/2 全部 `[x]` + `Status: completed`；Closure Gates 全 `[x]`。
- Auditor 独立性：fresh session，未执行本计划、未编辑任何文件、未勾选任何项，仅只读审查 + 跑两条验证命令。
