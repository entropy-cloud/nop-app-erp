# 2026-08-12-1118-1-erpct-contract-state-machine-bean 合同 ErpCtContract 实体级状态机 Bean（M2.18）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.18（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 契约 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 清单 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；后继 `M3.18 ErpCtContractVersion.status`（Deps = M1.3 + **M2.18**，本计划解除其阻塞）
> Mission: entity-state-machine
> Work Item: M2.18
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）：一 Bean 一实体一轴、显式动作方法 + `transitions()` 只读元数据、FQN-id 在非生成 `app-service.beans.xml` 注册、按类型注入（字段非 private）、Bean 抛 common 层码 + Processor 映射领域 ErrorCode、CRUD 写入边界为选项 (c) 显式排除。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除（路线图规则 1：`todo → ready` 仍需独立 plan 草案审查）。
- **M2.18 归类**：M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（合同头状态本身不触发业财过账；InvoicePlan 触发/版本签署是独立轴/独立实体）。**非 plan-first**；但仍跨模块行为变更，须独立 plan + 独立草案审查 + 独立结束审计（路线图规则 3）。
- **合同状态语义**（owner doc `docs/design/contract/state-machine.md` §1-§3）：7 态 DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED/CANCELLED；终态 = EXPIRED/TERMINATED/CANCELLED；SUSPENDED↔ACTIVE 可往复；NEGOTIATION→TERMINATED（谈判破裂）。
- **dict 实况（关键漂移发现）**：`module-contract/model/app-erp-contract.orm.xml:27-34` dict `erp-ct/contract-status` 仅含 **6 值**：DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED——**缺 CANCELLED**（owner doc §1 L19 + §2 L35 + L21 补注声明 CANCELLED 为 DRAFT 草稿废弃终态，但 dict 无此值）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：
  - `ErpCtContractBizModel`（`entity/`）：suspend `:80-83`（守卫 `Objects.equals(ACTIVE)` `:80`→SUSPENDED）、resume `:92-95`（守卫 SUSPENDED→ACTIVE）、terminate `:107-116`（守卫 `status∈{ACTIVE,NEGOTIATION}` 多源 `:107-110`→TERMINATED）、expire `:125-128`（守卫 ACTIVE→EXPIRED）；私有 `illegalTransition(...)` helper `:184-189`（领域码 `ERR_CT_ILLEGAL_STATUS_TRANSITION` + contractCode/currentStatus/expectedStatus 参数）。
  - `ErpCtContractActivateProcessor`（`processor/`）：activate `:35-46`（守卫 `Objects.equals(NEGOTIATION)` `:35-36`→ACTIVE，非法抛 `:57/68/74/88`）。
  - `ErpCtContractAmendProcessor`：amend `:59-63`（contract `setStatus(DRAFT)` `:63` + 新版本 DRAFT `:59`，守卫 `Objects.equals(ACTIVE)` `:37`，非法抛 `:73/87`）。
  - activate/amend 经 BizModel 委托 Processor（`activate()` `:72-74`、`amend()` `:135-137`）。
- **关键漂移（layer-2 须裁定，非本重构静默折叠）**：全 module-contract **零** `setStatus(NEGOTIATION)` writer、**零** `setStatus(CANCELLED)` writer（grep 确证）。即 owner doc §2 的 `DRAFT→NEGOTIATION`（提交谈判）与 `DRAFT→CANCELLED`（草稿废弃）两条迁移**无命名动作 writer 落地**——NEGOTIATION 仅作为 activate/terminate 的**源态被消费**，其本身在命名动作路径下从 DRAFT **不可达**（仅经 CRUD `__save` 可写，属 M0.1 §9.4 已知残留风险）；CANCELLED 在 dict 与 writer 双侧均不存在。Bean 矩阵须编码**已实现**迁移，四方对照须将上述两条登记为 doc drift Fix/Decision + successor（路线图规则 5，禁止静默排除）。
- **既有层 3 回归基线（非 greenfield）**：`TestErpCtContractTerminate`、`TestErpCtContractPosting`、`TestErpCtContractCrudSmoke`、`TestErpCtContractRebate`、`TestErpCtESignature`（均经 BizModel/IGraphQLEngine 入口断言状态迁移/版本/过账/签署）。注意 M0.1 §10 登记的 8 个 `TestErp*StateMachine` 基线不含 contract——contract 域层 3 = 上述既有集成测试，**不是**命名矩阵测试（层 1 新增）。
- **领域错误码已存在**：`ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION`（`erp.err.ct.illegal-status-transition`，参数 contractCode/currentStatus/expectedStatus，`ErpCtErrors.java:36`）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 currentStatus/expectedStatus）已存在并被 `AbstractProcessor.defaultIllegalStatusException` 使用（M1.1 已裁定 Option A 复用 + `action` 补充参数范式）。
- **生产 Bean 注册范式已存在**：`module-contract/erp-ct-service/src/main/resources/_vfs/erp/ct/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 per-mutation Processor。StateMachine Bean 沿用此范式。
- **合规基线**：`docs/audits/compliance-baseline.md` R5（`@Inject private`）= 0、R11（Processor 重复状态判断方法）= 0。本计划新增 Bean 注册 + 注入须保持 R5=0；接线后 BizModel/Processor 内联守卫收敛至 Bean，R11 不增。

## Goals

- 落地真实 `ErpCtContractStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 `ErpCtContractBizModel`（suspend/resume/terminate/expire）与 Activate/Amend Processor 的**固定来源态/目标态判断**改调 Bean，动态业务守卫（contractType↔direction 组合校验、版本归档、e-signature config、InvoicePlan 隐式失效语义、乐观锁）保留原位。
- 保持全部既有外部行为不变（错误码 + 参数、terminate 多源 {ACTIVE,NEGOTIATION}、版本归档、e-signature）。
- 新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿。
- **层 2 四方对照**裁定 NEGOTIATION/CANCELLED 漂移：登记为 Decision/Fix + successor（不在此重构新增 dict 值或新增 submit/cancel 命名动作——保护区 ask-first），并与 owner doc 对齐。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 dict `erp-ct/contract-status` 新增 CANCELLED 值**（保护区），亦不删除任何值。
- 不新增 `submitForNegotiation`（DRAFT→NEGOTIATION）或 `cancel`（DRAFT→CANCELLED）命名动作（属业务行为变更 + 可能触及 dict，归 successor ask-first；本重构只集中既有固定判断）。
- 不改变任何业务状态值、动作名、错误码值、权限、e-signature 时序、版本归档语义（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 `ErpCtContractVersion.status`（= M3.18，本计划解除其 Deps M2.18 阻塞，但版本轴迁移归独立 plan）。
- 不迁移 `ErpCtRebateSettlement.status` / `ErpCtSignatureRequest.status`（独立轴/独立实体，非 M2.18 结果表面）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不声称全域 Delta 覆盖已验证（M1.2 已证客服单轴；本计划证合同单轴 Delta，全域回归归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M1.3 模板 + M0.2 清单，落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/contract/state-machine.md`（业务状态语义 + §实现约定 + §3 NEGOTIATION→TERMINATED）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 contract 行）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 Processor 编排点）
- Skill Selection Basis: 路线图 M2.18 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、Bean 注册、跨实体调用边界（IErpCtContractVersionBiz）、错误码、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。层 2 四方对照引用 `state-machine-business-review-prompt.md` 10 维度（模板步骤 5 标配）。必需输入（owner doc + M0.1 契约 + 既有测试）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 ct-service 测试容器）。
- 前置依赖：M0.1 done + M0.2 done + M1.3 done（模板 go）。均已满足。

## Execution Plan

### Phase 1 - ErpCtContractStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/statemachine/ErpCtContractStateMachine.java`（新）；`module-contract/erp-ct-service/src/main/resources/_vfs/erp/ct/beans/app-service.beans.xml`（追加 Bean 注册）；`module-contract/erp-ct-service/src/test/java/.../statemachine/TestErpCtContractStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpCtContractStateMachine`（无状态、不注入 DAO/IBiz/IServiceContext），按契约 §4 + §11.1 步骤 1 实现。矩阵编码**已实现**迁移：
  - 显式动作方法（主路径）：`assertCanActivate(NEGOTIATION)`、`assertCanSuspend(ACTIVE)`、`assertCanResume(SUSPENDED)`、`assertCanTerminate(ACTIVE|NEGOTIATION)`（多源）、`assertCanExpire(ACTIVE)`、`assertCanAmend(ACTIVE)`；非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
  - 目标态方法：`activateTargetStatus()`→ACTIVE / `suspendTargetStatus()`→SUSPENDED / `resumeTargetStatus()`→ACTIVE / `terminateTargetStatus()`→TERMINATED / `expireTargetStatus()`→EXPIRED / `amendTargetStatus()`→DRAFT。
  - 终态分类：`isTerminal(EXPIRED|TERMINATED)`（CANCELLED 因 dict/双侧均无，不纳入矩阵终态集；其漂移在层 2 裁定）。
  - 只读元数据：`transitions()` 返回不可变快照；`terminalStatuses()`（EXPIRED/TERMINATED）；`initialStatuses()`（DRAFT）。
  Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.ct.service.statemachine.ErpCtContractStateMachine" class="...ErpCtContractStateMachine"/>` 注册（沿用既有 Processor FQN-id 范式，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpCtContractStateMachineMatrix`，§11.1 步骤 4）：遍历每个动作的合法/非法来源态——(a) 无重复/冲突边；(b) 从 DRAFT 可达性按**已实现**迁移断言（NEGOTIATION 在命名动作下从 DRAFT 不可达——测试断言此事实并标记为层 2 漂移项，而非在 Bean 伪造可达边）；(c) terminate 多源 {ACTIVE,NEGOTIATION} 全覆盖、对终态非法；(d) `transitions()` 元数据与显式方法语义一致；(e) 终态/初始态集合正确。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] `ErpCtContractStateMachine` 落地（6 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段（如有）非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-contract/erp-ct-service -Dtest=TestErpCtContractStateMachineMatrix` 全绿，覆盖上述 (a)-(e)，且如实反映 NEGOTIATION 命名动作不可达（非伪造边）。
- [x] 本地化编译检查：`mvn compile -pl module-contract/erp-ct-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `ErpCtContractBizModel.java`（suspend/resume/terminate/expire）、`ErpCtContractActivateProcessor.java`（activate）、`ErpCtContractAmendProcessor.java`（amend）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：BizModel 注入 `ErpCtContractStateMachine`（按类型注入，字段非 private），将 suspend/resume/expire 的内联 `Objects.equals` 守卫替换为 `stateMachine.assertCan<Action>(from)`，目标态写回改 `stateMachine.<action>TargetStatus()`；terminate 多源改 `stateMachine.assertCanTerminate(from)`（Bean 内部判定 {ACTIVE,NEGOTIATION}）。删除 BizModel 私有 `illegalTransition` 的**矩阵部分**（领域错误码映射 helper 保留或下沉为 common→领域映射）。**动态业务守卫保留原位**：contractType↔direction 组合校验（`validateTypeDirectionCombo`）、版本归档、e-signature config、requireContract（实体加载）、乐观锁。Skill: `nop-backend-dev`
- [x] `Fix`：Activate/Amend Processor 注入 `ErpCtContractStateMachine`，将内联守卫替换为 Bean 调用；Processor 捕获 Bean 的 common 层非法边报告，映射为领域 `ERR_CT_ILLEGAL_STATUS_TRANSITION`（保留 contractCode/currentStatus/expectedStatus 参数，common 码作 cause 保留——对齐契约 §7 + M1.1 Option A 范式）。**保留** activate 的签署/版本生效副作用、amend 的新版本创建。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-contract/erp-ct-service` 全绿——重点 `TestErpCtContractTerminate`（terminate ACTIVE/NEGOTIATION 多源 + 错误码）、`TestErpCtContractPosting`、`TestErpCtContractCrudSmoke`、`TestErpCtESignature`。证明错误码 + 参数、terminate 多源、版本归档、e-signature 均不变。若既有测试因 helper 调整需微调断言，仅调整与矩阵无关部分并记录理由（不得弱化断言）。Skill: `nop-testing`

Exit Criteria:

- [x] 六处固定来源态/目标态判断（BizModel 4：suspend/resume/terminate/expire + Activate + Amend）均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(from, CONTRACT_STATUS_*)` 矩阵判断（动态守卫如 contractType↔direction 组合校验除外）。
- [x] `ERR_CT_ILLEGAL_STATUS_TRANSITION` + 参数（contractCode/currentStatus/expectedStatus）对外不变（层 3 断言证实）；terminate 多源 {ACTIVE,NEGOTIATION} 行为不变。
- [x] 层 3 `mvn test -pl module-contract/erp-ct-service` 全绿。

### Phase 3 - 层 2 四方对照（dict ↔ owner-doc ↔ 元数据 ↔ writer）+ Delta 适用性

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；合同单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Fix | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查合同单轴——
  - **dict ↔ 元数据**：dict 6 值 ↔ Bean `transitions()` 边覆盖；每个 dict 值 writer 可达性（含 CRUD 路径，M0.1 §9.4）。
  - **owner-doc 迁移图 ↔ 元数据**：`state-machine.md §2` 迁移图 ↔ Bean 边覆盖；重点裁定 §11.4 警示「owner-doc §迁移表 vs §实现约定 内部漂移」在合同域是否存在。
  - **元数据 ↔ 全部 writer**：盘点 `ErpCtContract.status` 全部写路径——生产命名动作（BizModel 4 + Activate + Amend）+ 框架入口（`__save`/`save`）+ 测试 fixture。
  - **可达性/终态/异常路径**：从 DRAFT 命名动作可达性、终态无出边、terminate/expire/并发乐观锁异常路径与 owner doc §3-5 一致。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定，路线图规则 5）：对 NEGOTIATION（命名动作下从 DRAFT 不可达 / 仅经 CRUD 可写）与 CANCELLED（owner-doc 声明但 dict 缺值 + 零 writer）逐条分类（implementation drift / doc drift / intentional legacy）并指派 successor：
  - **CANCELLED**：owner-doc §1/§2 声明 DRAFT→CANCELLED 草稿废弃终态，但 dict `erp-ct/contract-status` 无此值且零 writer → **doc drift**。Fix = owner doc 补注「CANCELLED 为目标态未落地（dict 缺值 + 零 writer），DRAFT 废弃当前经 CRUD 删除」+ successor（PM 要求草稿废弃命名动作时，新增 dict 值 + cancel mutation，触及 ORM 保护区 ask-first）。**不在此重构新增 dict 值**。
  - **NEGOTIATION 可达性**：命名动作路径下从 DRAFT 无出边至 NEGOTIATION（零 `setStatus(NEGOTIATION)` writer）→ 登记 implementation drift（submitForNegotiation 未落地）+ successor。Bean 如实不编码该边。
  - 任何 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补 §迁移表缺失行。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域可选证 Delta）：经 VFS Delta 层同名 bean id 覆盖证明替换生效——派生类覆盖一个 `assertCan<Action>`（如收紧 terminate 仅 ACTIVE，移除 NEGOTIATION 源），基线/Delta 双加载可区分（复用 M1.2 范式：`TestErpCtContractStateMachineBaselineIoC` + `TestErpCtContractStateMachineDeltaOverride`）。Skill: `nop-testing`

Exit Criteria:

- [x] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] NEGOTIATION/CANCELLED 漂移已按 Fix/Decision 登记 + successor，无静默排除；owner doc 补注落地。
- [x] Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_00bb37a62ffeVprolXL7h9c00Q`) — 无 BLOCKER、无 MAJOR。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（dict 6 值无 CANCELLED、BizModel/Processor writer 行号、零 setStatus(NEGOTIATION)/(CANCELLED) writer、NEGOTIATION/CANCELLED 漂移发现、错误码、层 3 基线、common 码、Bean 注册范式、M3.18 Deps 解除）。3 MINOR（Phase 2 数字「五→六处」、AmendProcessor 守卫行 :37 而非 :73/87、validateTypeDirectionCombo 框架）已就地修正前两项；第三项为执行期认知（pre-existing R11 式重复，非本矩阵重构范围）。反松弛扫描 clean。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [x] 范围内行为完成（Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [x] 相关文档对齐（`contract/state-machine.md` NEGOTIATION/CANCELLED 漂移补注 + §迁移表内部漂移补正；路线图 M2.18 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-contract/erp-ct-service`（全绿，57/57）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（漂移裁定必须落地登记 + successor，不得悬置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CANCELLED dict 值 + DRAFT→CANCELLED 命名动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 声明 CANCELLED 草稿废弃终态，但 dict 缺值 + 零 writer。补 dict 值 + cancel mutation 触及 `model/*.orm.xml` 保护区（路线图 Non-Goal + AI 阻塞条件），属业务行为变更，须 ask-first。本重构只集中既有固定判断 + 登记漂移。
- Successor Required: yes（触发条件 = PM 要求合同草稿废弃命名动作时，开独立 plan 新增 dict 值 + cancel mutation）

### submitForNegotiation（DRAFT→NEGOTIATION）命名动作

- Classification: `watch-only residual`
- Why Not Blocking Closure: 命名动作路径下 NEGOTIATION 从 DRAFT 不可达（零 writer）；合同经 CRUD 创建可直接写 NEGOTIATION/ACTIVE（M0.1 §9.4 残留风险）。submitForNegotiation 落地属业务行为变更，非状态机集中重构范围。
- Successor Required: yes（触发条件 = 合同提交谈判业务流落地时）

### ErpCtContractVersion.status 版本轴（M3.18）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 版本轴是独立实体独立 Bean（M3.18），Deps 含 M2.18。本计划交付合同头轴 Bean 并解除 M3.18 的 M2.18 阻塞，不迁移版本轴。
- Successor Required: yes（触发条件 = 本计划闭包后，M3.18 可启动独立 plan）

## Closure

Status Note: 三 Phase 全部执行完成。`ErpCtContractStateMachine` Bean 落地（6 动作 + 目标态 + isTerminal + transitions 元数据，7 条已实现边），BizModel 4 处 + Activate/Amend Processor 2 处共 6 处固定判断接线至 Bean，层 1 矩阵测试 9/9 绿、层 3 既有集成回归 57/57 绿、Delta 双加载运行时实证 6/6 绿。NEGOTIATION/CANCELLED 漂移按路线图规则 5 登记为 Decision/Fix + successor（已入 Deferred But Adjudicated），owner doc 已补注。

### 层 2 四方对照审计记录（Phase 3 Proof，按 `state-machine-business-review-prompt.md` 10 维度）

**维度 1 — dict ↔ 元数据**：dict `erp-ct/contract-status`（`module-contract/model/app-erp-contract.orm.xml:27-34`）含 6 值 DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED（**缺 CANCELLED**）。Bean `transitions()` 编码 7 条已实现边，覆盖 dict 中除纯 CRUD 可达的 NEGOTIATION 外的全部目标态。dict **无死状态**（6 值均有 writer 路径，含 CRUD `__save`）：DRAFT（初始+amend 回写）、NEGOTIATION（仅 CRUD，命名动作不可达——漂移项见下）、ACTIVE（activate/resume 目标）、SUSPENDED（suspend 目标）、EXPIRED/TERMINATED（终态，terminate/expire 目标）。

**维度 2 — owner-doc 迁移图 ↔ 元数据**：`state-machine.md §2` 声明 9 条边，Bean 编码其中 7 条已实现边（activate/suspend/resume/terminate×2/expire/amend），全部与 owner-doc 一致。差异 2 条均登记为漂移（见 Decision 段）：DRAFT→NEGOTIATION（零命名 writer）、DRAFT→CANCELLED（dict 缺值+零 writer）。owner-doc §迁移表 与 §实现约定 无内部语义漂移（§11.4 警示已显式核对）。

**维度 3 — 元数据 ↔ 全部 writer**：`ErpCtContract.status` 写路径盘点：
- 生产命名动作（Bean 治理，6 处）：`ErpCtContractBizModel` suspend(:85)/resume(:99)/terminate(:116)/expire(:135) + `ErpCtContractActivateProcessor` activate(:45) + `ErpCtContractAmendProcessor` amend(:46) —— 全部已改调 `stateMachine.assertCan<Action>` + `<action>TargetStatus()`，grep 证相关方法体零内联 `Objects.equals(*, CONTRACT_STATUS_*)` 矩阵判断。
- 框架入口（CRUD `__save`/`save`）：`defaultPrepareSave` 仅兜底 businessDate，不写 status；但 xmeta `status` insertable/updatable，GraphQL save 可直写状态字段（M0.1 §9.4 残留，非矩阵运行时强制范围）。
- 测试 fixture：`TestErpCtContractTerminate.createContract` / `TestErpCtContractPosting` 经 save 直写 status 构造初始/任意态（层 3 基线，不变）。

**维度 4 — 可达性/终态/异常路径**：从 DRAFT 命名动作可达集 = **空**（NEGOTIATION 不可达=implementation drift，CANCELLED=dict drift）——层 1 `testReachabilityFromDraftIsEmptyDueToNoSubmitWriter` 断言此事实。终态 EXPIRED/TERMINATED 无出边（`testTerminalStatusesHaveNoOutgoingEdges`）。terminate/expire 非法来源态经 Bean 抛 common 层码 → Processor/BizModel 映射 `ERR_CT_ILLEGAL_STATUS_TRANSITION`（contractCode/currentStatus/expectedStatus，common 码作 cause）；层 3 `TestErpCtContractTerminate` 证实 DRAFT/SUSPENDED/EXPIRED/TERMINATED 全部拒绝 + ACTIVE/NEGOTIATION 多源接受。并发乐观锁（version 字段）不变。

### 漂移裁定（Phase 3 Decision，路线图规则 5——禁止静默排除）

- **CANCELLED = doc drift**：owner-doc §1 L19/§2 L35/§3 声明 `DRAFT→CANCELLED` 草稿废弃终态，但 dict 缺 CANCELLED 值（`app-erp-contract.orm.xml:27-34` 仅 6 值）+ 全域零 `setStatus(CANCELLED)` writer（grep 确证）。Fix：owner doc §1/§2 已补注「CANCELLED 目标态未落地，DRAFT 废弃当前经 CRUD 删除」；Bean 不纳入终态集（javadoc 标注）。Successor：PM 要求草稿废弃命名动作时开独立 plan 新增 dict 值 + cancel mutation（触及 ORM 保护区 ask-first）→ 入 Deferred But Adjudicated。
- **NEGOTIATION 可达性 = implementation drift**：owner-doc §2 声明 `DRAFT→NEGOTIATION`（提交谈判），但零 `setStatus(NEGOTIATION)` 命名 writer（无 submitForNegotiation），NEGOTIATION 仅作 activate/terminate 源态被消费，命名动作下从 DRAFT 不可达（仅 CRUD 可写，M0.1 §9.4 残留）。Fix：Bean 如实不编码该边；层 1 矩阵断言不可达事实；owner doc §2 已补注。Successor：合同提交谈判业务流落地时开独立 plan → 入 Deferred But Adjudicated。
- **owner-doc §迁移表 vs §实现约定 内部漂移**：按 §11.4 警示显式核对——§2 迁移表 9 边与 §3 终态/§实现约定 语义内部一致（CANCELLED 在 §1/§3 均作终态声明），无额外内部漂移；上述 2 条均属 owner-doc↔实现 漂移（已登记）。

### Delta 适用性证据（Phase 3 Add|Proof，M2 非保护域可选证 Delta）

经 VFS Delta 层 `test-ct-delta` 同名 bean id 覆盖基线为派生类 `ErpCtContractStateMachineDelta`（收紧 terminate 仅 ACTIVE，移除 NEGOTIATION 源）。运行时双加载实证：
- `TestErpCtContractStateMachineBaselineIoC`（3/3 绿）：容器解析基线类，`assertCanTerminate(NEGOTIATION)` **放行**。
- `TestErpCtContractStateMachineDeltaOverride`（3/3 绿，`@NopTestProperty nop.core.vfs.delta-layer-ids=test-ct-delta`）：容器解析 Delta 派生类，`assertCanTerminate(NEGOTIATION)` **抛异常**；非覆盖动作（activate/suspend/isTerminal）继承基线。
- 同一 `assertCanTerminate(NEGOTIATION)` 在基线放行 / Delta 抛异常 → 构成可区分的基线/Delta 双加载运行时证据（契约 §6 业务级 Delta 实证义务）。

### 验证结果

- `mvn compile -pl module-contract/erp-ct-service -am`：BUILD SUCCESS。
- `mvn test -pl module-contract/erp-ct-service`：Tests run: 57, Failures: 0, Errors: 0（含层 1 矩阵 9 + BaselineIoC 3 + DeltaOverride 3 + 既有层 3 集成回归 42）。
- Bean 无状态：grep 证实不 import DAO/IBiz/IServiceContext/事务；`@Inject` 字段（BizModel/Processor）均非 private（合规 R5）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，新会话，不重用执行者上下文）
- Evidence:
  - 语义复核全通过：Plan Status=completed / 三 Phase Status=completed / 三 Phase Exit Criteria 全 [x] / Closure Gates 全 [x]（含本审计门控）/ Deferred 项均带 successor 触发条件，文本一致。
  - 实时代码核对（grep+read）：`ErpCtContractStateMachine`（7 条已实现边 + 6 动作 + 目标态 + isTerminal[EXPIRED/TERMINATED] + transitions/terminalStatuses/initialStatuses 元数据）落地且严格无状态（零 import DAO/IBiz/IServiceContext/事务）；`app-service.beans.xml:35` 已以 FQN id 注册。
  - 接线核对：BizModel（suspend:85/resume:99/terminate:116/expire:135 全改调 `assertCan<Action>` + `<action>TargetStatus()`）+ ActivateProcessor(:45/:57) + AmendProcessor(:46/:73) 共 6 处固定判断改调 Bean，方法体内零内联 `Objects.equals(*, CONTRACT_STATUS_*)` 矩阵判断；动态守卫（contractType↔direction `validateTypeDirectionCombo`）保留原位。`@Inject` 字段全非 private（合规 R5）。
  - 错误码映射核对：Processor try/catch 捕获 Bean common 层码 → 映射领域 `ERR_CT_ILLEGAL_STATUS_TRANSITION`（contractCode/currentStatus/expectedStatus，common 码作 cause）。
  - Anti-Hollow：Bean 方法体均有真实实现并经 BizModel/Processor 运行时调用，非占位/空体/吞异常。
  - 验证命令复跑（独立审计会话实测）：`mvn test -pl module-contract/erp-ct-service` = Tests run: 63, Failures: 0, Errors: 0, BUILD SUCCESS（含层 1 矩阵 9 + BaselineIoC 3 + DeltaOverride 3 + 既有层 3 集成回归 48；plan 原记 57 为执行期计数，实测 63 全绿，方向有利不构成漂移）；`bash docs/audits/nop-compliance-checker.sh` = EXIT 0，R5(@Inject private)=0 / R11(Processor 重复状态判断)=0 无漂移。
  - 文档同步核对：`docs/logs/2026/08-12.md` 有 M2.18 条目；`docs/design/contract/state-machine.md` §1/§2 已补 NEGOTIATION/CANCELLED 漂移注记 + successor。

Follow-up:

- 独立结束审计（CLOSURE_VERIFY）已执行通过（Closure Gates 末项 [x]）。
- CANCELLED dict 值 + cancel mutation、submitForNegotiation、ErpCtContractVersion.status (M3.18) 见 Deferred But Adjudicated（非阻塞，已带 successor 触发条件）。
