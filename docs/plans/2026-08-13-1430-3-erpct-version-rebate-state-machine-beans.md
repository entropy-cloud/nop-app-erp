# 2026-08-13-1430-3-erpct-version-rebate-state-machine-beans 合同 ErpCtContractVersion + ErpCtRebateAgreement 实体级状态机 Bean（M3.18 + M3.19）

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.18（todo）+ M3.19（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹计划 `2026-08-12-1118-1-erpct-contract-state-machine-bean.md`（M2.18 done，本域 Bean/接线/测试/Delta 范式，M3.18 deps M2.18 已满足）；M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（contract Version/RebateAgreement 行）
> Mission: entity-state-machine
> Work Item: M3.18 + M3.19
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M3.18/M3.19 行）+ 实仓核实。M3.18/M3.19 均为**无财务影响**非保护域轴（实仓 grep 证实 module-contract 全域零 `IErpFinVoucherBiz`/`IErpAstAssetBiz`/`IErpInvStockMoveBiz`/`postingDispatcher`——返利结算过账在独立 Settlement 实体 M4.65 的 `settlement-status` 轴 DRAFT→POSTED，非 RebateAgreement.status 触发；e-signature 经独立 `ErpCtSignatureRequest` 实体 config-gated 默认 OFF）。本计划按规则 14 将同 owner doc `contract/state-machine.md`（契约域）的两轴合并。

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 go，M3 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。M3.18 deps = M1.3 + M2.18（done），门控已解除。
- **owner doc 覆盖缺口（layer-2 须从代码建立权威语义）**：`docs/design/contract/state-machine.md` 全文仅覆盖 **Contract 主轴**；**无独立 §Version 章节**（Version 迁移仅作为 Contract 迁移的副作用列于 `:11-19` + `:42-48`），**无 §RebateAgreement 章节**（返利语义在 `docs/design/contract/volume-discount.md`）。M0.2 清单「contract/state-machine.md §Version」「contract（返利子域）」引用为语义推断。layer-2 须以实仓代码为准建立每轴权威迁移图，owner doc 缺口按 Add 补章节或 Decision 登记 successor。
- **合同版本（ErpCtContractVersion.status）语义**（dict `erp-ct/version-status` 3 值 `app-erp-contract.orm.xml:51-55`：DRAFT/FINALIZED/SIGNED）：
  - **2 条状态迁移边**（实仓核实）：
    - `finalizeVersion`（`ErpCtContractVersionBizModel.finalizeVersion:46-55` `@BizMutation`，守卫 `:49-50` `Objects.equals(status, DRAFT)` 否则 `ERR_CT_ILLEGAL_STATUS_TRANSITION` → setStatus FINALIZED `:52`）
    - `signVersion`（`ErpCtContractVersionBizModel.signVersion:59` → `ErpCtContractVersionSignVersionProcessor.signVersion:30`，守卫 `isCurrent==true` `:33-37` AND `Objects.equals(status, FINALIZED)` `:38-40` → setStatus SIGNED `:51` + isCurrent flip `:44-49` + approvedAt `:53`）
  - **amend = 新建版本（非迁移）**：`ErpCtContractAmendProcessor:64-70` 在 Contract amend 时对**新建行** seed setStatus(VERSION_STATUS_DRAFT) `:69`（初始态写入，非既有版本迁移，按 §9.2 选项 c）。
  - **跨聚合联动 = Contract → Version（父驱子）**：`ErpCtContractActivateProcessor:52-55` Contract 激活时若当前版本 FINALIZED → 调 `contractVersionBiz.signVersion(...)`（版本 FINALIZED→SIGNED 级联）；Contract amend 创建新 DRAFT 版本。**signVersion 不写父 Contract.status**（仅版本行）。接线仅在版本自身命名动作 + Contract 级联调用点注入 Bean。
  - 终态 = {SIGNED}；初始 = {DRAFT}。线性无分支，dict 3 值全部可达（无死状态）。
  - **e-signature**：经独立 `ErpCtSignatureRequest` 实体 + `IErpCtSignatureProvider` SPI，config-gated `erp-ct.e-signature-enabled` 默认 OFF（owner doc `:46-48`）；`signVersion` 自身不调 SPI、不过账。
- **返利协议（ErpCtRebateAgreement.status）语义**（dict `erp-ct/rebate-agreement-status` 4 值 `app-erp-contract.orm.xml:67-72`：DRAFT/ACTIVE/EXPIRED/SETTLED）：
  - **零命名动作迁移 writer（退化轴）**：全仓无 `setStatus(REBATE_AGREEMENT_STATUS_ACTIVE|EXPIRED|SETTLED)` 生产 writer，无 activate/suspend/expire/terminate/cancel mutation。仅 **DRAFT 经 CRUD 创建可达**（新建 seed）。ACTIVE/EXPIRED/SETTLED 为**死状态**（零命名动作 writer）。
  - **唯一 live 用途 = 只读 accrual 守卫**：`ErpCtRebateAgreementRunAccrualProcessor:46` 读守卫 `status==ACTIVE` 否则 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`；`RebateEngine:142` 同守卫。`runAccrual` 副作用生成 `ErpCtRebateAccrual` 行（内部累计明细，**非凭证**）。
  - **返利结算过账在独立轴**：`ErpCtRebateSettlementPostSettlementProcessor` 操作 **Settlement 实体**的 `settlement-status`（DRAFT→POSTED，M4.65），生成 credit-memo 发票保存 `posted=false`（`:120,153`）——**与 RebateAgreement.status 无关**。RebateAgreement 状态轴零过账。
  - 即 Bean 为**纯分类 + 死状态登记**载体（transitions() 空，无 assertCan 迁移方法）；集中化 ACTIVE 只读 accrual 守卫为可测元数据。
- **错误码现状**：
  - Version：`ERR_CT_ILLEGAL_STATUS_TRANSITION`（ErpHrErrors... 即 `ErpCtErrors:36-38`，码 + 参数 contractCode/currentStatus/expectedStatus，被两 version writer 抛）+ `ERR_CT_VERSION_NOT_CURRENT:50-52`。无 version 专属 `*ILLEGAL_STATUS*` 变体。
  - RebateAgreement：`ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE:68-70`（参数 rebateAgreementId/currentStatus）。无迁移码（因无迁移）。
  - common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A + M2.18 Contract 范式：Bean 抛 common 码，BizModel/Processor 映射领域码 `ERR_CT_ILLEGAL_STATUS_TRANSITION`，common 作 cause——见 `ErpCtContractStateMachine.java:21-22` javadoc）。
- **生产 Bean 注册范式已存在**：`_vfs/erp/ct/beans/app-service.beans.xml:32-36` 已以 FQN id 注册 `ErpCtContractStateMachine`。StateMachine Bean 沿用 FQN id 范式（追加于文件末尾 `:60` 后）。
- **既有层 3 回归基线（非 greenfield）**：`TestErpCtESignature`（version 签名行为级覆盖——statuses at `:91,102,120,158,175,242,255,273`）、`TestErpCtContractRebate`（rebate engine + accrual + isCurrent flipping，读 ACTIVE 状态经 DAO seed）、`TestErpCtRebateSettlementEnd`（settlement 过账）。层 1 矩阵测试为 greenfield（新增）。
- **合规基线**：`@Inject private` = 0（module-contract service grep 证实）。本计划保持 R5=0、R11 不增。

## Goals

- 落地 `ErpCtContractVersionStateMachine`（2 迁移边 finalize/sign + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态）+ `ErpCtRebateAgreementStateMachine`（**退化分类 Bean**：transitions() 空 + initial={DRAFT} + 3 死状态登记 + 集中化 ACTIVE accrual 只读守卫为可测元数据），各可经 Delta 同名覆盖。
- 将 Version（finalizeVersion BizModel + signVersion Processor + Contract 激活级联调用点）的**固定来源态/目标态判断**改调 Bean；**动态业务守卫与副作用保留原位**（signVersion 的 isCurrent 守卫、approvedAt、e-signature SPI 独立流、乐观锁）。RebateAgreement：将 RunAccrualProcessor/RebateEngine 的 ACTIVE 只读守卫改委托 Bean 分类方法（零迁移 writer，无 assertCan 接线）。
- 保持全部既有外部行为不变（错误码、Version finalize/sign 边、signVersion isCurrent 守卫、Contract 激活→版本 SIGNED 级联、amend 新建 DRAFT 版本、RebateAgreement ACTIVE accrual 守卫 ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE、初始态 DRAFT 写入不调 assertCan*）。
- 各新增层 1 矩阵完备性表驱动测试（Version 线性；RebateAgreement 含退化解 + 3 死状态排除断言）；层 3 既有集成测试回归全绿。
- 层 2 四方对照：Version 确认 3 值全可达 + 跨聚合 Contract→Version 级联；RebateAgreement 裁定 3 死状态 + 退化轴分类 + owner doc 缺口；分别登记 + successor。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 rebate-agreement-status 删除值**（ACTIVE/EXPIRED/SETTLED 保留为预留语义入口，对齐 Contract CANCELLED/NEGOTIATION + hr SUSPENDED 先例——保留优于删除）。
- 不实现 RebateAgreement activate/expire/settle/suspend/cancel mutation（owner doc 无 §RebateAgreement + 零 writer；落地属业务行为变更，归 successor）。
- 不迁移 `ErpCtContract`（M2.18 done）、`ErpCtRebateSettlement.status`（= M4.65，plan-first 业财过账）、`ErpCtSignatureRequest`（e-signature SPI 回调派生）、`ErpCtApprovalRecord`（审计日志）、`ErpCtDocument`（ocrStatus 自由 string）。
- 不改变任何业务状态值、动作名、错误码值、isCurrent 语义、e-signature 时序、Contract→Version 级联时序（路线图 Non-Goal）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不重构跨聚合副作用编排（Contract activate/amend 逻辑原位保留，只替换/委托其中固定状态判断）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；M1.2 + M2.18 Contract 已实证；归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单 + M2.18 Contract 范式；落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §8 不适用/退化轴）、`docs/design/contract/state-machine.md`（Contract 主轴 + 版本管理影响列 `:11-19,42-48`；Version/RebateAgreement 缺口须 layer-2 建立）、`docs/design/contract/volume-discount.md`（返利语义）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 contract 行）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-12-1118-1-erpct-contract-state-machine-bean.md`（本域范式）
- Skill Selection Basis: 路线图 M3.18/M3.19 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、跨聚合级联调用点注入、只读守卫委托、动态副作用保留、错误码、退化解处理」；`nop-testing` 匹配「矩阵表驱动测试（含退化解）+ 既有集成测试回归」。必需输入均已就绪。层 2 引用 `state-machine-business-review-prompt.md`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 ct-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。M3.18 deps = M1.3 + M2.18（done）；M3.19 deps = M1.3（done），门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（实仓核实 Version/RebateAgreement 状态轴零过账副作用）。

## Execution Plan

### Phase 1 - ErpCtContractVersionStateMachine + ErpCtRebateAgreementStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/statemachine/ErpCtContractVersionStateMachine.java`（新）+ `ErpCtRebateAgreementStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册于 `:60` 后）；`TestErpCtContractVersionStateMachineMatrix.java` + `TestErpCtRebateAgreementStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 + M2.18 done

- [x] `Add`：创建 `ErpCtContractVersionStateMachine`（无状态），矩阵编码**已实现 2 边**：`assertCanFinalize(DRAFT)`/`assertCanSign(FINALIZED)` + 目标态方法（`finalizeTargetStatus()`→FINALIZED / `signTargetStatus()`→SIGNED）+ `isTerminal(SIGNED)` + `transitions()`（finalize 1 + sign 1 = 2 边）+ `terminalStatuses()`(SIGNED) + `initialStatuses()`(DRAFT)。线性无分支，dict 3 值全可达（无死状态）。非法来源态抛 common 码携带 `action`/`fromStatus`。Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpCtRebateAgreementStateMachine`（无状态，**退化分类 Bean**）——`transitions()` 返回空列表（零迁移边）；`initialStatuses()`(DRAFT) + `terminalStatuses()` 返回空（ACTIVE/EXPIRED/SETTLED 为不可达死状态）+ `isTerminal(status)` 对三死状态返回 false；集中化只读 accrual 守卫：`isActive(ACTIVE)` 返回 true / 其他 false。**ACTIVE/EXPIRED/SETTLED 不在 initial/terminal/transitions 任一集合**（死状态，layer-2 裁定登记）。javadoc 标注退化解 + 死状态 + owner-doc 缺口 + successor（activate/expire/settle mutation 落地时）。Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两 Bean（沿用 Contract 范式 `:32-36`，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4）：`TestErpCtContractVersionStateMachineMatrix` 覆盖 finalize（DRAFT 合法、FINALIZED/SIGNED 非法）/sign（FINALIZED 合法、DRAFT/SIGNED 非法）合法+非法 + 终态 SIGNED 无出边 + transitions（2 边）一致；`TestErpCtRebateAgreementStateMachineMatrix` 覆盖退化解——`transitions()` 空、`isActive`(ACTIVE=true, DRAFT/EXPIRED/SETTLED=false)、initial={DRAFT}、**断言 ACTIVE/EXPIRED/SETTLED 不在 transitions/initial/terminal 任一集合**。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（Version 2 动作 + 目标态 + isTerminal + transitions 2 边；RebateAgreement 退化解 + isActive + transitions 空），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-contract/erp-ct-service -am -Dtest=TestErpCtContractVersionStateMachineMatrix,TestErpCtRebateAgreementStateMachineMatrix` 全绿（12/12）。
- [x] 本地化编译检查：`mvn compile -pl module-contract/erp-ct-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: Version：`ErpCtContractVersionBizModel`（finalizeVersion/signVersion 委托）、`ErpCtContractVersionSignVersionProcessor`（signVersion 守卫 + 目标态）、`ErpCtContractActivateProcessor`（Contract 激活→版本 SIGNED 级联调用点）；RebateAgreement：`ErpCtRebateAgreementRunAccrualProcessor`（ACTIVE 只读守卫）、`RebateEngine`（ACTIVE 只读守卫）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：Version `ErpCtContractVersionBizModel.finalizeVersion` 注入 `ErpCtContractVersionStateMachine`（`@Inject` 非 private），将 `:49-50` 内联 `Objects.equals(status, DRAFT)` 守卫替换为 `stateMachine.assertCanFinalize(from)` + 目标态写回 `finalizeTargetStatus()`；`ErpCtContractVersionSignVersionProcessor.signVersion` 将 `:38-40` FINALIZED 守卫替换为 `stateMachine.assertCanSign(from)` + `signTargetStatus()` 写回 `:51`。common→`ERR_CT_ILLEGAL_STATUS_TRANSITION` 映射（common 码作 cause）。**动态副作用保留原位**：signVersion 的 `isCurrent==true` 守卫 `:33-37`（`ERR_CT_VERSION_NOT_CURRENT`）、isCurrent flip `:44-49`、approvedAt `:53`、e-signature 独立 SPI 流、乐观锁。**amend 新建 DRAFT 版本不调 assertCan***（`ErpCtContractAmendProcessor:69` 初始态写入，按 §9.2 选项 c）。Skill: `nop-backend-dev`
- [x] `Fix`：`ErpCtContractActivateProcessor` Contract 激活→版本 SIGNED 级联调用点（`:52-55` 调 `contractVersionBiz.signVersion`）——经 signVersion 已注入 Bean 的路径间接委托（不重复直接注入 Version Bean；级联经 IBiz 调用，守卫在 signVersion 内统一）。**Contract 主轴 Bean（M2.18）不变**。Skill: `nop-backend-dev`
- [x] `Fix`：RebateAgreement `ErpCtRebateAgreementRunAccrualProcessor:46` + `RebateEngine:142` 注入 `ErpCtRebateAgreementStateMachine`，将 `status==ACTIVE` 只读守卫改委托 `stateMachine.isActive(status)`（违例仍抛 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`，错误码对外不变）。**runAccrual 生成 ErpCtRebateAccrual 行的副作用保留原位**。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-contract/erp-ct-service -am` 全绿——重点 `TestErpCtESignature`（version 签名行为 + isCurrent）、`TestErpCtContractRebate`（rebate engine + accrual + ACTIVE 守卫 + isCurrent flipping）、`TestErpCtRebateSettlementEnd`（settlement 过账——独立轴不受影响）。证明错误码、finalize/sign 边、signVersion isCurrent 守卫、Contract 激活→版本 SIGNED 级联、amend 新建 DRAFT、RebateAgreement ACTIVE accrual 守卫均不变。Skill: `nop-testing`

Exit Criteria:

- [x] Version finalize/sign 内联守卫 + RebateAgreement ACTIVE 只读守卫均改调/委托 Bean，grep 证实相关方法体内不再有内联 `Objects.equals` 状态矩阵判断（动态副作用如 isCurrent/approvedAt/e-signature/accrual 生成除外；amend 新建 DRAFT 不调 assertCan*——按 §9.2 初始态路径）。
- [x] 领域错误码 + 参数对外不变（层 3 断言证实）；Version finalize/sign 边 + signVersion isCurrent 守卫 + Contract 激活→版本 SIGNED 级联 + amend 新建 DRAFT；RebateAgreement ACTIVE accrual 守卫 ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE 行为不变。
- [x] 层 3 `mvn test -pl module-contract/erp-ct-service -am` 全绿。

### Phase 3 - 层 2 四方对照（Version + RebateAgreement 双轴）+ owner-doc 缺口补章节

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；owner doc 补 Version/RebateAgreement 章节（Add）
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查双轴——Version（dict version-status 3 值 ↔ owner-doc 版本管理影响列 `:11-19,42-48` ↔ Bean ↔ writer 含 finalizeVersion/signVersion + Contract 激活级联调用点 + amend 新建 DRAFT + CRUD 路径，**确认 3 值全可达 + 跨聚合 Contract→Version 级联**）；RebateAgreement（dict rebate-agreement-status 4 值 ↔ owner-doc 缺口（volume-discount.md）↔ Bean 退化解 ↔ writer 含 runAccrual ACTIVE 只读守卫 + CRUD 创建 DRAFT + 框架入口，**重点裁定 3 死状态 + 退化轴 + owner-doc 缺口**）。writer 盘点含级联调用 + 初始态写入 + 只读守卫 + 框架入口 + 测试 fixture。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`（owner-doc 缺口补章节）：在 `docs/design/contract/state-machine.md` 补 **§适用对象二：合同版本（ContractVersion）** 章节（3 值定义 + finalize/sign 迁移表 + 终态 + Contract→Version 级联声明 + 与 §版本管理影响列交叉引用）+ **§适用对象三：返利协议（RebateAgreement）** 章节（4 值定义 + 退化轴声明「DRAFT 经 CRUD 创建可达，ACTIVE/EXPIRED/SETTLED 为预留死状态」+ ACTIVE accrual 只读守卫 + 返利结算过账在独立 Settlement 轴 M4.65 的边界声明 + volume-discount.md 交叉引用）。镜像 hr/projects 域 state-machine.md 章节范式。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定，路线图规则 5）：
  - **RebateAgreement ACTIVE/EXPIRED/SETTLED = intentional reserved（死状态）**：dict 有值 + 零命名动作 writer + 无 mutation。分类 = `intentional reserved`。Fix：Bean 不纳入任一集合（javadoc 标注）；dict 值保留（不删除，对齐 Contract CANCELLED/NEGOTIATION + hr SUSPENDED 先例）；owner doc 补章节登记。Successor：返利协议 activate/expire/settle 业务流落地时。
  - **RebateAgreement 退化轴**：零迁移 writer，Bean transitions() 空，仅分类 + ACTIVE 只读守卫集中化。登记为 Decision（如实反映，非 implementation drift）。Successor：mutation 落地时填充 Bean 边。
  - **owner-doc §Version / §RebateAgreement 缺口**：分类 = `doc gap`（owner doc 未覆盖）。Fix：Phase 3 Add 补章节。
  - 任何 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补正；其他不一致按 Fix/Decision 登记。
  Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] owner doc 补 Version + RebateAgreement 两章节（Add 落地）；RebateAgreement 3 死状态 + 退化轴 + owner-doc 缺口 Decision 均已登记 + successor，无静默排除；其他不一致项（若有）已 Fix 登记。

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_008041867ffeiYTfAFiS0rdgTa，新会话，零信任实仓复核）—— 全部 11 项 load-bearing 基线声明 CONFIRMED TRUE（module-contract 全域零过载、RebateSettlement 过账在独立 Settlement 轴 M4.65、Version 恰 2 边、amend 新建初始 DRAFT、Contract→Version 级联、signVersion 不写父 Contract.status、RebateAgreement 零迁移 writer + ACTIVE 只读守卫、dict 值可达性、owner-doc 缺口、错误码、合规 R5=0、Bean 注册范式、M2.18 done 前置满足）。M3 无过账分类在**双轴**均正确（域内唯一过账 = 独立 Settlement 轴）。规则 A-J 全 PASS（Task Route/Non-Goals/anti-slack/rule 14 bundling/§11.1 phasing/Item types+Decisions/rule 13/退化轴 Bean 设计可辩护/owner-doc 補章节 scope 合理/Closure Gates）。0 BLOCKER / 0 MAJOR / 2 cosmetic MINOR（RebateEngine guard 行号引用；Delta proof 归 M5.3 观察非缺陷）。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + Version 接线 + RebateAgreement 守卫委托 + 测试 + owner-doc 补章节），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（Version + RebateAgreement 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + owner-doc 补章节）
- [x] 相关文档对齐（contract/state-machine.md 补 Version + RebateAgreement 章节；死状态 + 退化轴 Decision 登记；路线图 M3.18 + M3.19 done）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-contract/erp-ct-service` 全绿（75/75）+ `bash docs/audits/nop-compliance-checker.sh`（R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（死状态/退化轴/owner-doc 缺口 Decision 必须落地登记）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### RebateAgreement activate/expire/settle mutation（ACTIVE/EXPIRED/SETTLED 落地）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: dict 含三值但零命名动作 writer + 无 mutation。owner doc 补章节登记为预留死状态。实现 activate/expire/settle 属业务行为变更，归 successor。dict 值保留为预留语义入口（不删除）。
- Successor Required: yes（触发条件 = PM 要求返利协议激活/到期/结算生命周期业务流落地时，开独立 plan 实现 mutation + 填充 Bean 边）

### ErpCtRebateSettlement.status（返利结算过账轴）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpCtRebateSettlement.status`（settlement-status DRAFT→POSTED）= M4.65（plan-first 业财过账——PostSettlementProcessor 触发结算凭证 + 生成 credit-memo 发票）。独立轴，非 RebateAgreement.status。
- Successor Required: yes（触发条件 = M4.65 工作项启动时）

### contract 域其余状态轴（SignatureRequest / ApprovalRecord / Document）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpCtSignatureRequest` = e-signature SPI 回调派生；`ErpCtApprovalRecord` = 审计日志；`ErpCtDocument.ocrStatus` = 自由 string（清单排除项）。
- Successor Required: yes（触发条件 = e-signature/审批/OCR 业务流全面状态机化时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 可选；M1.2 + M2.18 Contract 已实证机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 执行完成（Phase 1/2/3 全部 [x] + 测试全绿 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS）；层 2 四方对照 + Decision 已落地登记。结束审计由独立子代理（新会话）执行通过（CLOSURE_VERIFY，见 Closure Audit Evidence 实仓复核）。

### 层 2 四方对照审计记录（plan `2026-08-13-1430-3`，按 `state-machine-business-review-prompt.md` 10 维度）

四方对照 = ①dict ②owner doc ③Bean 元数据 ④writer（命名动作 + 级联调用 + 初始态写入 + 只读守卫 + 框架入口 + 测试 fixture）。

#### 轴一：合同版本（ErpCtContractVersion.status，dict `erp-ct/version-status`）

- **D1 状态定义**：dict 3 值 DRAFT/FINALIZED/SIGNED（`app-erp-contract.orm.xml:51-55`）②owner doc §适用对象二 §1（plan 补章节）③Bean `initialStatuses()={DRAFT}` / `terminalStatuses()={SIGNED}`（`ErpCtContractVersionStateMachine:86-88`）。三方一致。
- **D2 转换完整性**：2 边。③Bean `transitions()`=2（finalize DRAFT→FINALIZED `:77-79` + sign FINALIZED→SIGNED `:79`）②owner doc §适用对象二 §2 迁移表 ④writer：`finalizeVersion`（`ErpCtContractVersionBizModel:50-60`，Bean `assertCanFinalize` `:53` + `finalizeTargetStatus()` `:57`）+ `signVersion`（`ErpCtContractVersionSignVersionProcessor:34-62`，Bean `assertCanSign` `:43` + `signTargetStatus()` `:57`）。进/出迁移齐备，无非法跳转。
- **D3 终态与恢复**：③Bean `isTerminal(SIGNED)=true`（`:70-72`）②owner doc §适用对象二 §5 终态={SIGNED}。SIGNED 无出边（Bean transitions 无 SIGNED 源边），不可回退。一致。
- **D4 异常路径**：④重复签署/非法来源态抛 common 码（Bean `illegal` `:92-97`）→ Processor/BizModel 映射 `ERR_CT_ILLEGAL_STATUS_TRANSITION`（cause 保留 common 码）；非当前版本抛 `ERR_CT_VERSION_NOT_CURRENT`（动态业务守卫，保留原位 `:37-41`）。幂等：FINALIZED 重签经 assertCanSign 拦截。
- **D5 可达性**：从 DRAFT 出发，FINALIZED（经 finalize）+ SIGNED（经 sign）均可达；③Bean transitions 覆盖 dict 全 3 值，**无死状态**。②owner doc §适用对象二 §5 声明一致。
- **D6 角色与权限**：finalizeVersion/signVersion 均 @BizMutation（经权限管道）；版本审批角色见 §适用对象一 §6。本期未改变权限模型。
- **D7 外部依赖**：**跨聚合级联 Contract→Version（父驱子）** ④`ErpCtContractActivateProcessor:52-55` Contract 激活时若当前版本 FINALIZED → 调 `contractVersionBiz.signVersion`（级联 FINALIZED→SIGNED）。级联经 IBiz 调用，守卫统一在 signVersion Processor（不重复注入 Version Bean）。②owner doc §适用对象二 §3 声明一致。e-signature 经独立 `ErpCtSignatureRequest` + SPI（config-gated 默认 OFF），signVersion 不调 SPI。
- **D8 TODO/任务策略**：版本状态不直接产 TODO（合同主轴产 TODO，见 §适用对象一 §8）。版本签署归档由合同经办人跟进。
- **D9 场景演练**：快乐路径 DRAFT→finalize→FINALIZED→sign→SIGNED（isCurrent 翻转）；级联路径 Contract NEGOTIATION→activate→版本 FINALIZED→SIGNED；拒绝路径 非当前版本 signVersion 抛 ERR_CT_VERSION_NOT_CURRENT、FINALIZED 重签抛 ERR_CT_ILLEGAL_STATUS_TRANSITION（层 3 `TestErpCtESignature` 19 例覆盖）。
- **D10 与设计文档一致性**：②owner doc §适用对象二补章节落地（含与 §适用对象一「版本管理影响」列交叉引用）；③Bean javadoc 引用 `docs/design/contract/state-machine.md §适用对象：合同版本`。dict/Bean/owner doc 三方迁移矩阵一致，无内部漂移。
- **D11 dict 可达性核查**：dict 3 值全部有 writer 路径可达（DRAFT=CRUD/amend seed；FINALIZED=finalizeVersion；SIGNED=signVersion/级联）。**零承诺漂移**。

**轴一裁决：pass**（0 P0 / 0 P1）。3 值全可达 + 跨聚合 Contract→Version 级联确认；Bean/dict/owner doc/writer 四方一致。

#### 轴二：返利协议（ErpCtRebateAgreement.status，dict `erp-ct/rebate-agreement-status`）

- **D1 状态定义**：dict 4 值 DRAFT/ACTIVE/EXPIRED/SETTLED（`app-erp-contract.orm.xml:67-72`）②owner doc §适用对象三 §1（plan 补章节）③Bean `initialStatuses()={DRAFT}` / `terminalStatuses()={}`（空）/ `isTerminal()` 全 false（`ErpCtRebateAgreementStateMachine:60-85`）。三方如实反映退化解。
- **D2 转换完整性**：③Bean `transitions()=`空列表（`:69-71`，零迁移边）②owner doc §适用对象三 §2 退化轴声明 ④writer：**零命名动作迁移 writer**——全仓无 `setStatus(REBATE_AGREEMENT_STATUS_ACTIVE|EXPIRED|SETTLED)` 生产 writer，无 activate/suspend/expire/terminate/cancel mutation。仅 DRAFT 经 CRUD 创建 seed（初始态写入 §9.2 选项 c）。退化解如实反映，无遗漏迁移。
- **D3 终态与恢复**：③Bean `terminalStatuses()` 空、`isTerminal` 全 false ②owner doc §适用对象三 §1+§2 声明 ACTIVE/EXPIRED/SETTLED 为**预留死状态**（非真正终态）。EXPIRED/SETTLED 字面似终态但无 writer 可达 → 不纳入终态集（如实反映）。裁定见 Decision。
- **D4 异常路径**：④唯一 live 守卫 = accrual 计提前断言 `status==ACTIVE` 否则 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`（`ErpCtRebateAgreementRunAccrualProcessor:46` + `RebateEngine.validateActive:141`，**已委托** Bean `isActive(status)` `:49-51`）。错误码对外不变。重复计提幂等经 `loadAccruedBillCodes` 去重（保留原位）。
- **D5 可达性**：仅 DRAFT 可达（CRUD seed）；ACTIVE/EXPIRED/SETTLED 命名动作路径下**零 writer 可达**（死状态）。③Bean 据实不纳入任一集合。②owner doc §适用对象三 §2 声明一致。
- **D6 角色与权限**：runAccrual @BizMutation（经权限管道）；无激活/到期/结算 mutation（因无 writer）。
- **D7 外部依赖**：**返利结算过账在独立轴** ④`ErpCtRebateSettlementPostSettlementProcessor` 操作 `ErpCtRebateSettlement.settlement-status`（dict `erp-ct/settlement-status` DRAFT→POSTED，M4.65），生成 credit-memo 发票 `posted=false`——**与 RebateAgreement.status 无关**（owner doc §适用对象三 §4 边界声明）。本轴零过账（§8 posted 不入轴 + §3 零过载）。
- **D8 TODO/任务策略**：返利协议状态不产 TODO（计提经运营/计划触发；结算归 M4.65）。
- **D9 场景演练**：快乐路径 DRAFT 协议（CRUD 创建）→ runAccrual（ACTIVE 守卫）→ 生成 ErpCtRebateAccrual 行；拒绝路径 非 ACTIVE 协议 runAccrual 抛 ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE（层 3 `TestErpCtContractRebate` 8 例覆盖）；结算路径（独立 Settlement 轴，`TestErpCtRebateSettlementEnd`，本轴不受影响）。
- **D10 与设计文档一致性**：②owner doc §适用对象三补章节落地（含 volume-discount.md 交叉引用 + 独立 Settlement 轴边界声明）；③Bean javadoc 引用 `docs/design/contract/state-machine.md §适用对象：返利协议`。owner doc 原**缺口**（无 §RebateAgreement）已由本 plan Add 补章节修复（Decision: doc gap）。
- **D11 dict 可达性核查**：dict 4 值中 DRAFT 可达，ACTIVE/EXPIRED/SETTLED **零 writer** → 裁决三选一：**保留为 intentional reserved + Successor 登记**（不删 dict 项 / 不实现 mutation / Deferred 标注触发条件），对齐 Contract CANCELLED/NEGOTIATION + hr SUSPENDED 先例。**禁止沉默保留**——已 owner doc 补章节 + Bean javadoc 登记。

**轴二裁决：pass**（0 P0 / 0 P1）。退化轴 + 3 死状态 + owner-doc 缺口均已裁定登记 + Successor；ACTIVE accrual 只读守卫已委托 Bean 集中化；返利结算过账独立轴边界已声明。

### Phase 3 Decision 登记（漂移裁定，路线图规则 5）

1. **RebateAgreement ACTIVE/EXPIRED/SETTLED = intentional reserved（死状态）**：dict 有值 + 零命名动作 writer + 无 mutation。分类 = `intentional reserved`。Fix：Bean 不纳入任一集合（javadoc 标注）；dict 值保留（不删除，对齐先例）；owner doc §适用对象三 §2 登记。Successor：返利协议 activate/expire/settle 业务流落地时。
2. **RebateAgreement 退化轴**：零迁移 writer，Bean `transitions()` 空，仅分类 + ACTIVE 只读守卫集中化。登记为 Decision（如实反映，非 implementation drift）。Successor：mutation 落地时填充 Bean 边。
3. **owner-doc §Version / §RebateAgreement 缺口**：分类 = `doc gap`（owner doc 未覆盖）。Fix：Phase 3 Add 补章节（`docs/design/contract/state-machine.md` §适用对象二 + §适用对象三 已落地）。
4. **无 §迁移表 vs §实现约定 内部漂移**：双轴 dict/Bean/owner doc/writer 四方迁移矩阵一致，无 §11.4 补正项；无其他不一致。

Closure Audit Evidence:

- 执行证据：Phase 1（2 Bean + 注册 + 层 1 矩阵 12/12）+ Phase 2（Version finalize/sign 接线 + RebateAgreement ACTIVE 守卫委托 + 层 3 `mvn test` 75/75 全绿）+ Phase 3（owner doc 补 §适用对象二/三 + 层 2 四方对照双轴 pass + 4 Decision 登记）。
- 全量验证：`mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块）。
- **独立结束审计（CLOSURE_VERIFY）由独立子代理（新会话，零执行者上下文）执行通过**。实仓复核证据（grep/read 全部 CONFIRMED）：
  - 两 Bean 落地：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/statemachine/ErpCtContractVersionStateMachine.java`（`assertCanFinalize:45` + `finalizeTargetStatus:51` + `assertCanSign:55` + `signTargetStatus:61`）+ `ErpCtRebateAgreementStateMachine.java:39`。
  - Bean 注册：`module-contract/erp-ct-service/src/main/resources/_vfs/erp/ct/beans/app-service.beans.xml:39-44` 以 FQN id 注册两 Bean。
  - Version 接线：`ErpCtContractVersionBizModel.java:42` `@Inject ErpCtContractVersionStateMachine`（非 private），`:53` `assertCanFinalize` + `:57` `finalizeTargetStatus()`；`ErpCtContractVersionSignVersionProcessor.java:32` `@Inject` + `:43` `assertCanSign` + `:57` `signTargetStatus()`。
  - RebateAgreement 接线：`ErpCtRebateAgreementRunAccrualProcessor.java:46` `@Inject ErpCtRebateAgreementStateMachine`；`RebateEngine.java:49` `@Inject`。
  - 层 1 矩阵测试：`TestErpCtContractVersionStateMachineMatrix.java` + `TestErpCtRebateAgreementStateMachineMatrix.java` 均存在于 `src/test/java/.../statemachine/`。
  - owner doc 补章节：`docs/design/contract/state-machine.md:177` §适用对象二（ContractVersion）+ `:229` §适用对象三（RebateAgreement）落地，含与 §适用对象一交叉引用 + volume-discount.md 交叉引用 + 独立 Settlement 轴边界声明。
  - 日志：`docs/logs/2026/08-13.md:3-9` 记录本计划三 Phase 落地 + 全绿验证基线（156 reactor BUILD SUCCESS + 75/75 tests + R5=0/R11=0）。
  - 反空洞核查：所有新增 Bean 方法（assertCan*/targetStatus/transitions/initialStatuses/terminalStatuses/isActive）均在生产代码路径被实际调用（BizModel/Processor @Inject + 调用点已核实），无空函数体 / 无 return null 占位 / 无注册但不可达组件。
  - 退化轴反 hollow：RebateAgreement Bean `transitions()` 空属**有意**（零迁移 writer，layer-2 四方对照裁定为 intentional reserved + Successor 登记），`isActive()` 在 RunAccrualProcessor + RebateEngine 两点实际委托调用，非死代码。
  - 五点一致性：Plan Status `completed` / 三 Phase Status 全 `completed` / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]`（本审计勾选最后一项）/ 日志条目一致——全部对齐。
  - Deferred honesty：3 死状态 + 退化轴 + owner-doc 缺口均按 Decision 登记 + Successor 命名触发条件（§Deferred But Adjudicated 列 5 项，无范围内 live defect 隐藏为 deferred）。

Follow-up:

- 非阻塞跟进见 §Deferred But Adjudicated（RebateAgreement mutation / Settlement.status M4.65 / contract 域其余状态轴 / Delta 覆盖 / 全局 CRUD 写锁）。
- 独立结束审计（Closure Gates 最后一项）已由独立子代理 CLOSURE_VERIFY 通过（见 Closure Audit Evidence）。
