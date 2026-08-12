# 2026-08-12-2142-1-erpmd-supplier-approval-state-machine-bean 主数据 ErpMdSupplierApproval 实体级状态机 Bean（M2.1）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.1（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-1118-1-erpct-contract-state-machine-bean.md`（单实体单轴 + INLINE BizModel 接线 + 多源 cancel/terminate 范本）
> Mission: entity-state-machine
> Work Item: M2.1
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **M2.1 归类**：M2 简单生命周期（§11.2 代表样例——5 态简单分支 + 既有层 3 基线），非保护域、无审批子矩阵、无跨域过账副作用（供应商准入头状态不触发业财过账；purchase 评分 standing=RED 经 `IErpMdSupplierApprovalBiz.suspendByPartner` 回写是跨域 `I*Biz` 调用，保留 Processor）。**非 plan-first**；但仍跨模块行为变更，须独立 plan + 独立草案审查 + 独立结束审计（路线图规则 3）。
- **供应商准入（ErpMdSupplierApproval.status）语义**（owner doc = `docs/design/purchase/supplier-evaluation.md:50-54`——注意：`docs/design/master-data/README.md:201` 明示「主数据域不包含状态机文档」，供应商准入状态语义实际由 purchase 域 `supplier-evaluation.md` 承载，属跨域 owner doc，层 2 以该文档为准）：5 态 APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED；设计声明边 APPLIED→APPROVED（正式准入）、APPROVED→PROBATION（试用）、PROBATION→APPROVED（试用通过）、APPROVED/PROBATION→SUSPENDED（评分 standing=RED）、SUSPENDED→APPROVED（恢复）、APPLIED→REJECTED。
- **dict 实况（无死状态）**：`module-master-data/model/app-erp-master-data.orm.xml:176-183` inline dict `erp-md/supplier-approval-status` = 5 值 APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED（YAML 镜像 `module-master-data/erp-md-meta/.../_vfs/dict/erp-md/supplier-approval-status.dict.yaml:6-26`）；绑定 `ErpMdSupplierApproval.status`（`:1187 ext:dict`）。5 值全部有生产 writer（无死状态）。**pre-existing ORM 异常**（非本计划范围）：`:1187 defaultValue="10"` 与 dict 字符串值不一致（数字 "10" 不匹配任何 dict option，疑似 stale artifact，生成镜像 `_app.orm.xml:1744` 重复）——登记 watch-only residual，不在本重构触碰 ORM（路线图 Non-Goal）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：**INLINE BizModel 路径为主 + 1 个 per-mutation Processor**：
  - `ErpMdSupplierApprovalBizModel`（`module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdSupplierApprovalBizModel.java`）：apply `:101-104`（守卫 `:101-103` status==null 或 REJECTED→APPLIED `:104`）、approve `:114-119`（守卫 `:114-117` status∈{APPLIED,PROBATION}→APPROVED `:119`）、probate `:131-134`（守卫 `:131-133` APPROVED→PROBATION `:134`）、suspend `:142-144`（委托 `doSuspend :202-211`，守卫 `:202-210` + setStatus SUSPENDED `:211`）、reinstate `:157-160`（守卫 `:157-159` SUSPENDED→APPROVED `:160`）、reject `:172-175`（守卫 `:172-174` APPLIED→REJECTED `:175`）；**protected `illegalTransition(...)` helper `:262-267`**（领域码 `ERR_INVALID_APPROVAL_STATUS_TRANSITION` + approvalId/currentStatus/expectedStatus 参数，6 个 INLINE mutation 共用）。
  - `ErpMdSupplierApprovalSuspendByPartnerProcessor`（`processor/`）：`doSuspend :60-75`，幂等短路 `:62-64`（已 SUSPENDED 直接 return），守卫 `:65-72`（status∈{APPLIED,APPROVED,PROBATION}，否则内联抛领域码 `:68-71`，**不经 BizModel 的 illegalTransition helper**）→setStatus SUSPENDED `:73` + `daoProvider.daoFor(...).updateEntity :74`。
  - BizModel javadoc `:34-35` 明示「单步状态推进不拆 Processor」；Processor javadoc `:20-29` 明示 `doSuspend` 是 BizModel 同语义的批量副本（批量循环内含写，R6.9 规则要求拆 Processor）——即 BizModel/Processor 间 `doSuspend` 守卫逻辑**有意重复**。
  - **无 `AbstractErp*SupplierApproval*Processor` 共享骨架**；Processor 独立类。
- **幂等/多源语义（接线须保持）**：(i) apply 接受 `status==null`（新建）或 REJECTED（重新申请）→ APPLIED；(ii) suspend 已 SUSPENDED 时幂等 return（不抛）；(iii) approve 多源 {APPLIED,PROBATION}；(iv) suspend 多源 {APPLIED,APPROVED,PROBATION}。Bean 须如实编码或由 BizModel/Processor 在调 Bean 前短路幂等分支。
- **动态业务守卫（保留原位，不下沉 Bean）**：approve 内 `requireQualificationValid`（`:118` 调 `:216-224`，qualificationDoc 非空 + validFrom/validTo 非空 + to.isAfter(from)，否则 `ERR_APPROVAL_QUALIFICATION_MISSING`）；`defaultPrepareSave :56-59` / `defaultPrepareUpdate :62-65` 调 `enforceNoOverlapIfEffective :73-94`（C3 date-range MUTEX，同 partnerId 且 status≠REJECTED 不许区间重叠，否则 `ERR_MD_DATE_RANGE_OVERLAP`）。
- **错误码**：`ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION`（`module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java:35-37`，值 `erp.err.md.invalid-approval-status-transition`，参数 approvalId/currentStatus/expectedStatus）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`module-common-service/.../ErpCommonErrors.java:23-27`，值 `nop.err.erp.common.illegal-status-transition`，参数 currentStatus/expectedStatus）已存在并被 cs/hr/ct/pur/sal Bean 复用（M1.1 Option A + `action` 补充参数范式）。
- **既有层 3 回归基线（非 greenfield）**：`TestErpMdSupplierApprovalStateMachine`（`module-master-data/erp-md-service/src/test/java/app/erp/md/service/TestErpMdSupplierApprovalStateMachine.java`，199 行）——经 `IGraphQLEngine` 入口（`JunitAutoTestCase` + H2），M0.1 §10 + inventory §3.1 **登记的 8 个基线之一**。6 个 `@Test`：`testFullHappyPath`（APPLIED→APPROVED→PROBATION→APPROVED→SUSPENDED→APPROVED 链）、`testIllegalTransitions`（APPROVED 态 approve/reject/reinstate 各拒 `ERR_INVALID_APPROVAL_STATUS_TRANSITION`）、`testApproveRequiresQualification`、`testRejectFromApplied`、`testSuspendByPartnerSuspendsAllActive`（批量 APPROVED+PROBATION，REJECTED 跳过）、`testFindEffectiveByPartner`。另 `TestErpMdDateRangePilots.java:144-179` 含 3 个 C3 MUTEX 用例（与状态机正交，保留）。**已知覆盖缺口**（层 1 矩阵补）：无 REJECTED 终态全面拒绝断言、无 5×6 穷举矩阵、无 `transitions()` 元数据一致性、无基线 IoC + Delta 覆盖测试（cs/hr/ct 四件套范式）。
- **生产 Bean 注册范式已存在**：`module-master-data/erp-md-service/src/main/resources/_vfs/erp/md/beans/app-service.beans.xml:42-48` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 2 个 per-mutation Processor（CurrencyRefreshRatesFromApi `:44-45` + SupplierApprovalSuspendByPartner `:47-48`）。StateMachine Bean 沿用此范式。
- **greenfield 范畴**：`module-master-data/**/statemachine/` 不存在、无 `ErpMd*StateMachine` Bean（`TestErpMdSupplierApprovalStateMachine` 名称是误名——测 BizModel 非 Bean）。本计划为 master-data 域首例 StateMachine Bean。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-master-data service 零违例，`ErpMdSupplierApprovalBizModel:52-53` + Processor `:32-33` @Inject 字段均包级可见）、R11（Processor 重复状态判断方法）= 0。本计划新增 1 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛至 Bean，R11 不增。
- **死代码候选**（登记，非本计划行为变更范围）：`ErpMdSupplierApprovalBizModel.findActiveByPartner :226-238` 似为孤儿（仅 Processor `:46` 在用），迁移是清理时机，但删除属独立低风险清理，归 Follow-up（带触发条件）。

## Goals

- 落地真实 `ErpMdSupplierApprovalStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。为 master-data 域首例 StateMachine Bean（建立域内范式）。
- 将 `ErpMdSupplierApprovalBizModel`（apply/approve/probate/suspend/reinstate/reject）与 `ErpMdSupplierApprovalSuspendByPartnerProcessor`（doSuspend）的**固定来源态/目标态判断**改调 Bean；**动态业务守卫保留原位**（requireQualificationValid、C3 date-range MUTEX enforceNoOverlapIfEffective、幂等短路、实体加载、乐观锁）。
- 保持全部既有外部行为不变（错误码 + 参数形状、apply 接受 null/REJECTED、approve 多源 {APPLIED,PROBATION}、suspend 多源 {APPLIED,APPROVED,PROBATION} + 幂等、suspendByPartner 批量跳过 REJECTED、reinstate SUSPENDED→APPROVED、reject APPLIED→REJECTED）。
- 新增层 1 矩阵完备性表驱动测试（greenfield，不经 BizModel 入口）；层 3 既有集成测试回归全绿。
- 层 2 四方对照（dict ↔ `purchase/supplier-evaluation.md` owner doc ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）单轴裁定，**显式处置跨域 owner-doc 与 REJECTED 可恢复性语义**（Decision 登记），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不触碰 `defaultValue="10"` 异常**（保护区 + pre-existing，归 watch-only residual）；不向 dict 新增/删除值。
- 不新增 master-data 域 `state-machine.md`（owner doc 仍由 `purchase/supplier-evaluation.md` 承载——doc 组织归 successor ask-first，非状态机集中重构范围）。
- 不改变任何业务状态值、动作名、错误码值、权限、C3 MUTEX 时序、跨域 standing=RED 回写时序（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 master-data 其他 10 个 ACTIVE/INACTIVE 标志实体（M0.2 裁定排除-标志）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不删除 `ErpMdSupplierApprovalBizModel.findActiveByPartner :226-238` 死代码（归 Follow-up，带触发条件）。
- 不声称全域 Delta 覆盖已验证（M1.2 已证客服单轴；本计划证 SupplierApproval 单轴 Delta，全域回归归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M1.3 模板 + M0.2 清单，落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/purchase/supplier-evaluation.md`（§状态机 50-54 + §实现约定 101-103,110——跨域 owner doc）、`docs/design/master-data/README.md:94,201`（C3 date-range + 主数据无状态机文档声明）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 master-data 行 MD-11）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 BizModel/Processor 编排点）
- Skill Selection Basis: 路线图 M2.1 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「INLINE BizModel 接线、单 Processor 接线、幂等/多源守卫边界、C3 动态守卫保留、错误码」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 6 个 IGraphQLEngine 集成测试回归」。层 2 四方对照引用 `state-machine-business-review-prompt.md` 10 维度（模板步骤 5 标配）。必需输入（owner doc + M0.1 契约 + 既有层 3 基线）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 md-service 测试容器）。
- 前置依赖：M0.1 done + M0.2 done + M1.3 done（模板 go）。均已满足。

## Execution Plan

### Phase 1 - ErpMdSupplierApprovalStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/statemachine/ErpMdSupplierApprovalStateMachine.java`（新）；`module-master-data/erp-md-service/src/main/resources/_vfs/erp/md/beans/app-service.beans.xml`（追加 Bean 注册）；`module-master-data/erp-md-service/src/test/java/app/erp/md/service/statemachine/TestErpMdSupplierApprovalStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpMdSupplierApprovalStateMachine`（无状态、不注入 DAO/IBiz/IServiceContext/事务），按契约 §4 + §11.1 步骤 1 实现。矩阵编码**已实现**迁移：
  - 显式动作方法（主路径）：`assertCanApply(null|REJECTED)`（接受 null 与 REJECTED 为源）、`assertCanApprove(APPLIED|PROBATION)`（多源）、`assertCanProbate(APPROVED)`、`assertCanSuspend(APPLIED|APPROVED|PROBATION)`（多源；幂等「已 SUSPENDED」短路留 BizModel/Processor，不进 Bean）、`assertCanReinstate(SUSPENDED)`、`assertCanReject(APPLIED)`；非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
  - 目标态方法：`applyTargetStatus()`→APPLIED / `approveTargetStatus()`→APPROVED / `probateTargetStatus()`→PROBATION / `suspendTargetStatus()`→SUSPENDED / `reinstateTargetStatus()`→APPROVED / `rejectTargetStatus()`→REJECTED。
  - 终态分类：`isTerminal(...)`——按**已实现**行为 REJECTED 经 apply 可恢复，故无严格不可恢复终态；`isTerminal` 返回 false 对全部 dict 值（或仅作语义注记），层 2 Decision 登记此事实。
  - 只读元数据：`transitions()` 返回不可变快照（含 apply 的 null/REJECTED→APPLIED、approve 多源、suspend 多源）；`terminalStatuses()`（空集或显式标注 REJECTED 为「可恢复准终态」）；`initialStatuses()`（APPLIED；null 视为新建前的虚拟初始）。
  Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.md.service.statemachine.ErpMdSupplierApprovalStateMachine" class="...ErpMdSupplierApprovalStateMachine"/>` 注册（沿用既有 Processor FQN-id 范式，§11.1 步骤 2）。
  Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpMdSupplierApprovalStateMachineMatrix`，§11.1 步骤 4）：遍历每个动作的合法/非法来源态——(a) 无重复/冲突边；(b) 从 APPLIED 可达 APPROVED/PROBATION/SUSPENDED/REJECTED 全部声明状态（REJECTED 经 apply 可回到 APPLIED，断言此环）；(c) 多来源态动作（approve {APPLIED,PROBATION}、suspend {APPLIED,APPROVED,PROBATION}）覆盖全集；(d) `transitions()` 元数据与显式方法语义一致；(e) 终态/初始态集合正确。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] `ErpMdSupplierApprovalStateMachine` 落地（6 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] Bean 已在 `app-service.beans.xml` 注册（FQN id）；Bean 自身无 `@Inject`（严格无状态），BizModel/Processor 接线点的 `@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-master-data/erp-md-service -Dtest=TestErpMdSupplierApprovalStateMachineMatrix` 全绿（11/11），覆盖上述 (a)-(e)。
- [x] 本地化编译检查：`mvn compile -pl module-master-data/erp-md-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `ErpMdSupplierApprovalBizModel.java`（apply/approve/probate/suspend/reinstate/reject + doSuspend）、`ErpMdSupplierApprovalSuspendByPartnerProcessor.java`（doSuspend）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：BizModel 注入 `ErpMdSupplierApprovalStateMachine`（按类型注入，字段非 private），将 apply/approve/probate/reinstate/reject 的内联 `Objects.equals` 守卫替换为 `stateMachine.assertCan<Action>(from)`，目标态写回改 `stateMachine.<action>TargetStatus()`；suspend/doSuspend 在幂等短路（已 SUSPENDED return）**之后**调 `stateMachine.assertCanSuspend(from)` + `suspendTargetStatus()`。删除 BizModel `illegalTransition` helper 的**矩阵部分**（保留或下沉为 common→领域码映射 helper）。**动态业务守卫保留原位**：approve 的 `requireQualificationValid`、`defaultPrepareSave/Update` 的 `enforceNoOverlapIfEffective`（C3 MUTEX）、实体加载、乐观锁。Skill: `nop-backend-dev`
- [x] `Fix`：SuspendByPartnerProcessor 注入 `ErpMdSupplierApprovalStateMachine`，将 `doSuspend` 内联守卫（`:65-72`）替换为 Bean 调用（幂等短路 `:62-64` 保留在 Bean 调用前）；Processor 捕获 Bean 的 common 层非法边报告，映射为领域 `ERR_INVALID_APPROVAL_STATUS_TRANSITION`（保留 approvalId/currentStatus/expectedStatus 参数，common 码作 cause——对齐契约 §7 + M1.1 Option A 范式）。**保留** 批量循环 + findActiveByPartner 跳过 REJECTED + `daoProvider.daoFor(...).updateEntity` 持久化。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-master-data/erp-md-service` 全绿（137/137）——重点 `TestErpMdSupplierApprovalStateMachine`（6 个 @Test：happy path 链、illegal transitions、approveRequiresQualification、rejectFromApplied、suspendByPartnerSuspendsAllActive、findEffectiveByPartner）+ `TestErpMdDateRangePilots`（C3 MUTEX 用例）。证明错误码 + 参数、apply null/REJECTED、approve/probate/reinstate/reject、suspend 多源 + 幂等、批量跳过 REJECTED、C3 MUTEX 均不变。Skill: `nop-testing`

Exit Criteria:

- [x] 七处固定来源态/目标态判断（BizModel 6：apply/approve/probate/suspend(doSuspend)/reinstate/reject + Processor doSuspend 1）均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(*, APPROVAL_STATUS_*)` 矩阵判断（动态守卫 requireQualificationValid/enforceNoOverlapIfEffective/findEffectiveByPartner 的 REJECTED 排除 + doSuspend 幂等 SUSPENDED 短路除外）。
- [x] `ERR_INVALID_APPROVAL_STATUS_TRANSITION` + 参数（approvalId/currentStatus/expectedStatus）对外不变（层 3 断言证实）；apply null/REJECTED、suspend 多源 + 幂等、suspendByPartner 跳过 REJECTED 行为不变。
- [x] 层 3 `mvn test -pl module-master-data/erp-md-service` 全绿（137/137）。

### Phase 3 - 层 2 四方对照（dict ↔ owner-doc ↔ 元数据 ↔ writer）+ Delta 适用性

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；SupplierApproval 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 SupplierApproval 单轴——
  - **dict ↔ 元数据**：dict 5 值 ↔ Bean `transitions()` 边覆盖；每个 dict 值 writer 可达性（含 CRUD 路径，M0.1 §9.4）。
  - **owner-doc 迁移图 ↔ 元数据**：`purchase/supplier-evaluation.md:50-54` 迁移图 ↔ Bean 边覆盖；显式裁定 §11.4「owner-doc §迁移表 vs §实现约定 内部漂移」（supplier-evaluation.md 为单一状态机块，无独立 §实现约定 段，重点核对 apply null/REJECTED 与 REJECTED 可恢复性是否在 owner doc 表达）。
  - **元数据 ↔ 全部 writer**：盘点 `ErpMdSupplierApproval.status` 全部写路径——生产命名动作（BizModel 6 + Processor 1）+ 框架入口（`__save`/`save`，xmeta `status` insertable/updatable）+ 测试 fixture。
  - **可达性/终态/异常路径**：从 APPLIED 可达性、REJECTED 经 apply 可恢复（非严格终态）、suspend 幂等、并发乐观锁与 owner doc 一致。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移/语义裁定，路线图规则 5）：
  - **跨域 owner doc**：供应商准入状态语义由 `purchase/supplier-evaluation.md` 承载而非 master-data 自有 state-machine.md（`master-data/README.md:201` 明示）——登记为 intentional doc organization，层 2 以 supplier-evaluation.md 为准；successor = master-data 增独立审批实体时评估新建 master-data state-machine.md（ask-first）。
  - **REJECTED 可恢复性**：apply 接受 REJECTED 为源（重新申请）→ REJECTED 非严格不可恢复终态；Bean 如实编码 apply(REJECTED)→APPLIED 边；owner doc `supplier-evaluation.md:54` 原未显式列 REJECTED→APPLIED，登记为 doc 表达不完整 → Fix owner doc 补注「REJECTED 可经 apply 重新申请回到 APPLIED」。
  - **APPLIED→SUSPENDED 补充边**：owner doc `supplier-evaluation.md:54` 原声明 suspend 源为 APPROVED/PROBATION，但既有实现（BizModel doSuspend + Processor doSuspend）接受 APPLIED/APPROVED/PROBATION 三源（APPLIED 为「申请中也可暂停」补充边）→ 登记 implementation-supplemented edge → Fix owner doc 补注「APPLIED 也可 suspend」。
  - **`defaultValue="10"` ORM 异常**：pre-existing，登记 watch-only residual（successor = 数据模型清理时 ask-first 修正）。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域可选证 Delta）：经 VFS Delta 层同名 bean id 覆盖证明替换生效——派生类覆盖一个 `assertCan<Action>`（收紧 approve 仅 APPLIED，移除 PROBATION 源），基线/Delta 双加载可区分（复用 M1.2 范式：`TestErpMdSupplierApprovalStateMachineBaselineIoC` + `TestErpMdSupplierApprovalStateMachineDeltaOverride`）。Skill: `nop-testing`

Exit Criteria:

- [x] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] 跨域 owner doc、REJECTED 可恢复性、APPLIED→SUSPENDED 补充边、`defaultValue="10"` 异常均已按 Decision/Fix 登记 + successor，无静默排除；owner doc 补注落地（REJECTED 可恢复性 + APPLIED→SUSPENDED 补充边 + 状态机 Bean 注记 Phase 3 已裁定为必需）。
- [x] Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分（6/6 绿）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_009c503b1ffeNusctpPB6o86Hf`）——无 BLOCKER、无 MAJOR。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（owner doc 跨域至 `purchase/supplier-evaluation.md:50-54`、`master-data/README.md:201` 无状态机文档声明、dict 5 值 + `defaultValue="10"` 异常、BizModel/Processor writer 行号逐行核实、`illegalTransition` helper、错误码、层 3 基线 6 @Test、common 码、Bean 注册范式、greenfield、R5=0/R11=0、REJECTED 可恢复性漂移发现、inventory MD-11 owner-doc 引用本身有误但计划已诚实路由）。4 MINOR（owner-doc Fix hedge 与 Phase 3 Decision 矛盾→已就地收紧、`_app.orm.xml` 行号 off-by-one、Processor javadoc 范围、inventory 引用错误可标注）均非阻塞；MINOR-1（hedge 矛盾）已就地修正以保文本一致性，其余为执行期精度修正。反松弛扫描 clean（无 optional/consider/maybe/nice to have/if time permits/as needed 在活跃范围项）。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [x] 范围内行为完成（Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [x] 相关文档对齐（`purchase/supplier-evaluation.md` REJECTED 可恢复性 + APPLIED→SUSPENDED 补充边 + 状态机 Bean 注记补注——Phase 3 Decision 已裁定并落地；路线图 M2.1 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-master-data/erp-md-service`（全绿 143/143）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（漂移裁定必须落地登记 + successor，不得悬置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `defaultValue="10"` ORM 异常

- Classification: `watch-only residual`
- Why Not Blocking Closure: `app-erp-master-data.orm.xml:1187` 的 `defaultValue="10"` 与 dict 字符串值不一致，pre-existing stale artifact。修正触及 `model/*.orm.xml` 保护区（路线图 Non-Goal + AI 阻塞条件），不在状态机集中重构范围。
- Successor Required: yes（触发条件 = master-data 数据模型清理计划时 ask-first 修正）

### `findActiveByPartner` BizModel 死代码清理

- Classification: `optimization candidate`
- Why Not Blocking Closure: `ErpMdSupplierApprovalBizModel:226-238` 似为孤儿（仅 Processor `:46` 在用）。删除属独立低风险清理，非状态机行为变更。
- Successor Required: yes（触发条件 = master-data 下次代码整理时删除并核实无反射/测试引用）

### master-data 独立 state-machine.md

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 供应商准入状态语义跨域由 `purchase/supplier-evaluation.md` 承载，`master-data/README.md:201` 明示主数据无状态机文档。是否新建 master-data state-machine.md 是 doc 组织决策，非状态机集中重构范围。
- Successor Required: yes（触发条件 = master-data 增加独立多步审批实体时评估新建）

## Closure

Status Note: 三 Phase 全部执行完成。`ErpMdSupplierApprovalStateMachine` Bean 落地（6 动作 + 目标态 + isTerminal + transitions 元数据，10 条已实现边，为 master-data 域首例 StateMachine Bean），BizModel 6 处 + Processor doSuspend 1 处共 7 处固定判断接线至 Bean，层 1 矩阵测试 11/11 绿、层 3 既有集成回归 6/6 绿（全 md-service 143/143 绿）、Delta 双加载运行时实证 6/6 绿。REJECTED 可恢复性 + APPLIED→SUSPENDED 补充边 + 跨域 owner doc + `defaultValue="10"` 异常均按路线图规则 5 登记为 Decision/Fix + successor（已入 Deferred But Adjudicated），owner doc 已补注。

### 层 2 四方对照审计记录（Phase 3 Proof，按 `state-machine-business-review-prompt.md` 10 维度）

**维度 1 — dict ↔ 元数据**：dict `erp-md/supplier-approval-status`（`module-master-data/model/app-erp-master-data.orm.xml:177-183`）含 5 值 APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED（YAML 镜像 `module-master-data/erp-md-meta/.../_vfs/dict/erp-md/supplier-approval-status.dict.yaml`）。Bean `transitions()` 编码 10 条已实现边，覆盖全部 5 个 dict 值作为目标态或源态。dict **无死状态**（5 值均有 writer 路径，含 CRUD `__save`）：APPLIED（apply 目标/初始）、APPROVED（approve/reinstate 目标）、PROBATION（probate 目标）、SUSPENDED（suspend 目标）、REJECTED（reject 目标，经 apply 可恢复）。

**维度 2 — owner-doc 迁移图 ↔ 元数据**：`supplier-evaluation.md:54` 声明 6 组迁移（apply 入口、APPLIED→APPROVED、APPROVED→PROBATION、PROBATION→APPROVED、SUSPENDED→APPROVED、APPLIED→REJECTED + suspend APPROVED/PROBATION→SUSPENDED），Bean 编码 10 条已实现边（含多源展开），全部与 owner-doc 一致或经 Fix 补注对齐。差异均登记为漂移（见 Decision 段）：(i) REJECTED→APPLIED（apply 接受 REJECTED 重新申请，owner-doc 原未显式列）；(ii) APPLIED→SUSPENDED（owner-doc 原仅列 APPROVED/PROBATION，实现含 APPLIED 源）；(iii) apply 接受 null（新建，owner-doc 原未显式表达）。owner-doc §迁移表 与 §实现约定 无内部语义漂移（supplier-evaluation.md 为单一状态机块，无独立 §实现约定 段，§11.4 警示已显式核对）。三项均已在 `supplier-evaluation.md:54` 补注。

**维度 3 — 元数据 ↔ 全部 writer**：`ErpMdSupplierApproval.status` 写路径盘点：
- 生产命名动作（Bean 治理，7 处）：`ErpMdSupplierApprovalBizModel` apply(:112)/approve(:128)/probate(:145)/reinstate(:173)/reject(:190)/doSuspend(:227) + `ErpMdSupplierApprovalSuspendByPartnerProcessor` doSuspend(:80) —— 全部已改调 `stateMachine.assertCan<Action>` + `<action>TargetStatus()`，grep 证相关方法体零内联 `Objects.equals(*, APPROVAL_STATUS_*)` 矩阵判断（动态守卫 enforceNoOverlapIfEffective/findEffectiveByPartner 的 REJECTED 排除 + doSuspend 幂等 SUSPENDED 短路除外）。
- 框架入口（CRUD `__save`/`save`）：`defaultPrepareSave/Update` 仅做 C3 date-range MUTEX 校验，不写 status；但 xmeta `status` insertable/updatable，GraphQL save 可直写状态字段（M0.1 §9.4 残留，非矩阵运行时强制范围，选项 (c) 显式排除）。
- 测试 fixture：`TestErpMdSupplierApprovalStateMachine.seedApproval` 经 dao `saveEntity` 直写 status 构造初始/任意态（层 3 基线，不变）。

**维度 4 — 可达性/终态/异常路径**：从 APPLIED 命名动作可达集 = {APPROVED, PROBATION, SUSPENDED, REJECTED}（层 1 `testReachabilityFromAppliedCoversAllDeclaredStatuses` 断言）。REJECTED 经 apply 可恢复（`testRejectedIsRecoverableViaApplyEdge` 断言 REJECTED→APPLIED 边 + 与 reject 构成环）。无严格终态（`testNoStrictTerminalStatuses`：isTerminal 对全部 dict 值返回 false，terminalStatuses 空集）。suspend 幂等（已 SUSPENDED 短路 return 不抛，保留 BizModel/Processor 在 Bean 调用前）。非法来源态经 Bean 抛 common 层码 → BizModel/Processor 映射 `ERR_INVALID_APPROVAL_STATUS_TRANSITION`（approvalId/currentStatus/expectedStatus，common 码作 cause）；层 3 `testIllegalTransitions` 证实 APPROVED 态 approve/reject/reinstate 全部拒绝。并发乐观锁（version 字段）不变。

### 漂移裁定（Phase 3 Decision，路线图规则 5——禁止静默排除）

- **跨域 owner doc = intentional doc organization**：供应商准入状态语义由 `purchase/supplier-evaluation.md` 承载而非 master-data 自有 state-machine.md（`master-data/README.md:201` 明示「主数据域不包含状态机文档」）。层 2 以 supplier-evaluation.md 为准。Fix：owner doc 已补状态机 Bean 注记（`supplier-evaluation.md:56`）。Successor：master-data 增加独立多步审批实体时评估新建 master-data state-machine.md（ask-first）→ 入 Deferred But Adjudicated。
- **REJECTED 可恢复性 = doc 表达不完整 → Fix**：apply 接受 REJECTED 为源（重新申请）→ REJECTED 非严格不可恢复终态。Bean 如实编码 apply(REJECTED)→APPLIED 边；`isTerminal` 对全部 dict 值返回 false、`terminalStatuses()` 空集。owner-doc `supplier-evaluation.md:54` 原未显式列 REJECTED→APPLIED，已补注「apply(null|REJECTED → APPLIED)... REJECTED 非严格终态，经 apply 可恢复」（`:54` 迁移图 + `:56` Bean 注记块）。
- **APPLIED→SUSPENDED 补充边 = implementation-supplemented edge → Fix**：owner-doc `supplier-evaluation.md:54` 原声明 suspend 源为 APPROVED/PROBATION，但既有实现（BizModel doSuspend + Processor doSuspend）接受 APPLIED/APPROVED/PROBATION 三源（APPLIED 为「申请中也可暂停」补充边，已存在于迁移前代码）。Bean 如实编码三源；owner doc 已补注「APPLIED/APPROVED/PROBATION → SUSPENDED... 含 APPLIED 申请中暂停——既有实现补充边」。
- **`defaultValue="10"` ORM 异常 = watch-only residual**：`app-erp-master-data.orm.xml:1187` 的 `defaultValue="10"` 与 dict 字符串值不一致（数字 "10" 不匹配任何 dict option），pre-existing stale artifact。修正触及 `model/*.orm.xml` 保护区（路线图 Non-Goal + AI 阻塞条件），不在状态机集中重构范围。Successor：master-data 数据模型清理计划时 ask-first 修正 → 入 Deferred But Adjudicated。
- **owner-doc §迁移表 vs §实现约定 内部漂移**：按 §11.4 警示显式核对——supplier-evaluation.md 为单一状态机块，无独立 §实现约定 段，无内部漂移；上述 3 条均属 owner-doc↔实现 漂移（已登记 + 补注）。

### Delta 适用性证据（Phase 3 Add|Proof，M2 非保护域可选证 Delta）

经 VFS Delta 层 `test-md-delta` 同名 bean id 覆盖基线为派生类 `ErpMdSupplierApprovalStateMachineDelta`（收紧 approve 仅 APPLIED，移除 PROBATION 源）。运行时双加载实证：
- `TestErpMdSupplierApprovalStateMachineBaselineIoC`（3/3 绿）：容器解析基线类，`assertCanApprove(PROBATION)` **放行**。
- `TestErpMdSupplierApprovalStateMachineDeltaOverride`（3/3 绿，`@NopTestProperty nop.core.vfs.delta-layer-ids=test-md-delta`）：容器解析 Delta 派生类，`assertCanApprove(PROBATION)` **抛异常**；非覆盖动作（apply/suspend/isTerminal）继承基线。
- 同一 `assertCanApprove(PROBATION)` 在基线放行 / Delta 抛异常 → 构成可区分的基线/Delta 双加载运行时证据（契约 §6 业务级 Delta 实证义务）。

### 验证结果

- `mvn compile -pl module-master-data/erp-md-service -am`：BUILD SUCCESS。
- `mvn clean install -DskipTests`（全仓库）：BUILD SUCCESS。
- `mvn test -pl module-master-data/erp-md-service`：Tests run: 143, Failures: 0, Errors: 0（含层 1 矩阵 11 + BaselineIoC 3 + DeltaOverride 3 + 既有层 3 集成回归含 SupplierApproval 6 + DateRangePilots 10 + 其余 md-service 测试）。
- `bash docs/audits/nop-compliance-checker.sh`：EXIT 0，R5(@Inject private)=0 / R11(Processor 重复状态判断)=0 无漂移。
- Bean 无状态：grep 证实不 import DAO/IBiz/IServiceContext/事务；Bean 自身无 `@Inject`；BizModel/Processor `@Inject` 字段均非 private（合规 R5）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，新会话 `ses_00913c336ffec8HtM7jupd2bcD`，不重用执行者上下文）
- Evidence:
  - Verdict: **pass**（无 BLOCKER）。独立会话复跑 `mvn test -pl module-master-data/erp-md-service` = Tests run: 143, Failures: 0, Errors: 0, BUILD SUCCESS；`bash docs/audits/nop-compliance-checker.sh` = EXIT 0，R5(@Inject private)=0 / R11(Processor 重复状态判断)=0 无漂移。
  - 语义复核：Plan Status=completed / 三 Phase Status=completed / 三 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Deferred 项均带 successor 触发条件，文本一致。
  - 实时代码核对：`ErpMdSupplierApprovalStateMachine`（10 条已实现边 + 6 动作 + 目标态 + isTerminal[全 false] + transitions/terminalStatuses[空集]/initialStatuses[APPLIED] 元数据）落地且严格无状态（零 import DAO/IBiz/IServiceContext/事务/@Inject）；`app-service.beans.xml:52` 已以 FQN id 注册。
  - 接线核对：BizModel（apply:112/approve:128/probate:145/reinstate:173/reject:190/doSuspend:227 全改调 `assertCan<Action>` + `<action>TargetStatus()`；doSuspend 幂等 SUSPENDED 短路 :219 在 Bean 调用前）+ Processor doSuspend(:81) 共 7 处固定判断改调 Bean，方法体内零内联矩阵判断（动态守卫 enforceNoOverlapIfEffective/findEffectiveByPartner REJECTED 排除 + doSuspend 幂等除外）；requireQualificationValid + C3 MUTEX 保留原位。`@Inject` 字段全非 private（合规 R5）。
  - 错误码映射核对：BizModel/Processor try/catch 捕获 Bean common 层码 → 映射领域 `ERR_INVALID_APPROVAL_STATUS_TRANSITION`（approvalId/currentStatus/expectedStatus，common 码作 cause）。
  - Anti-Hollow：Bean 方法体均有真实实现并经 BizModel/Processor 运行时调用（层 3 `TestErpMdSupplierApprovalStateMachine` 6 @Test 实证），非占位/空体/吞异常。
  - 四方对照 + 漂移裁定：跨域 owner doc / REJECTED 可恢复性 / APPLIED→SUSPENDED 补充边 / `defaultValue="10"` 异常均带 successor 登记，无静默排除；owner doc `supplier-evaluation.md:54,56` 补注落地。
  - Delta：`ErpMdSupplierApprovalStateMachineDelta`（收紧 approve 仅 APPLIED）+ 基线/Delta 双加载测试可区分（`assertCanApprove(PROBATION)` 基线放行 / Delta 抛异常）。
  - MINOR（非阻塞）：closure 段 owner-doc 行号引用 `:55` → 实测 blockquote 在 `:56`（off-by-one，已就地修正）。

Follow-up:

- 独立结束审计（CLOSURE_VERIFY）已执行通过（Closure Gates 末项 [x]）。
- `defaultValue="10"` ORM 异常、`findActiveByPartner` 死代码清理、master-data 独立 state-machine.md 见 Deferred But Adjudicated（非阻塞，已带 successor 触发条件）。
