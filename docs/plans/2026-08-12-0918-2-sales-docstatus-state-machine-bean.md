# 2026-08-12-0918-2-sales-docstatus-state-machine-bean 销售单据 docStatus 最小生命周期 StateMachine Bean 迁移（M2.9–M2.10）

> Plan Status: active
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.9 / M2.10（均 todo）
> Related: 前置 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 done，go 裁定，模板固化于 `entity-state-machine-bean.md §11`）；本计划解阻 M3.6/M3.7（销售 approveStatus 轴，各自 deps 含 M2.9/M2.10）；姊妹计划 `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（N=1，采购同轴迁移，跨实体 Decision 同源）
> Mission: entity-state-machine
> Work Item: M2.9 + M2.10
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（SAL-1/2/20/21 行）+ 实仓核实。docStatus 轴是销售单据三轴分离中的**业务生命周期轴**（与采购同构，`sales/state-machine.md` §三轴状态分离）。

- **轴语义（最小生命周期）**：`DRAFT`（初始态，创建时写入）→ `CANCELLED`（终态，作废）。dict `erp/doc-status`（与采购单据头共享）。SAL-1/2/20/21 八属性登记均为「纳入 / 无财务影响 / 最小生命周期 / approve 仅状态推进」。属模板 §11.2 M2 最小生命周期类别。
- **固定迁移判断当前所在位置（实仓核实）**：cancel 的「非已作废」守卫在共享骨架 `module-common-service/.../AbstractCancelProcessor.validateTransitionForCancel`（`AbstractCancelProcessor.java:30-35`）：内联 `Objects.equals(docStatus, cancelledDocStatus())` → 抛 `illegalStatusException`。这是本计划替换为 Bean 调用的固定判断。`module-common-service` 零改动（与采购计划一致，迁移经各域 `*CancelProcessor` 覆写委托）。
- **逐实体 writer 盘点（实仓核实，均为 PROC 路径，无缺失守卫漂移）**：
  - **M2.10 ErpSalOrder**：`ErpSalOrderCancelProcessor` extends `AbstractCancelProcessor<ErpSalOrder>`（`ErpSalOrderCancelProcessor.java`）。守卫经骨架；`beforeCancel` 承载 `runCommitmentReleaseHook` + `runIntercompanyReverseHook`（**动态副作用，保留原位**）；领域码 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION`（参数 `orderCode`/`currentDocStatus`/`expectedDocStatus`）。初始 DRAFT 由 `QuotationToOrderConverter`（`QuotationToOrderConverter.java:52`）/创建路径写入。
  - **M2.9 ErpSalQuotation**：`ErpSalQuotationCancelProcessor` extends `AbstractCancelProcessor<ErpSalQuotation>`（`ErpSalQuotationCancelProcessor.java`）。守卫经骨架；注释明示「报价单 cancel 无域特有 hook（facade cancel 仅 setDocStatus）」；领域码 `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION`（参数 `quotationCode`/...）。
- **与采购计划的关键差异（无漂移发现项）**：销售 Quotation/Order cancel **均经 CancelProcessor + 骨架守卫**，无采购 Quotation/Rfq 那种「BizModel 无守卫」漂移。本计划层 2 四方对照预期不产生 Fix（若发现意外漂移，按规则 5 登记 + successor，禁止静默排除）。
- **common 层非法迁移码已存在（参数形状已裁定）**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），cs 试点 M1.1 Decision Option A 裁定复用 + `action` 补充参数。本计划沿用。
- **Bean 注册范式已存在**：`module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/beans/app-service.beans.xml` 已注册既有 per-mutation Processor。
- **层 3 回归基线已存在（非 greenfield）**：销售域无既有 `TestErpSal*StateMachine` 矩阵测试（docStatus 轴无），层 1 为 greenfield；但存在覆盖 cancel 全生命周期的集成测试 = 层 3 基线：`TestErpSalOrderApproval`、`TestErpSalQuotationCrudSmoke`、`TestErpSalQuotationToOrder`、`TestErpSalOrderToCashEnd` 等。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。本计划保持 R5=0、R11 不增。
- **跨实体 Decision 复用采购计划**：Bean 接线点（覆写 `validateTransitionForCancel`）、common 码（Option A）、领域码映射三项 Decision 与采购计划 N=1 Phase 1 同源；本计划引用而非重复裁定（若采购计划尚未闭包，本计划独立成立——这些 Decision 源自 M0.1 契约 §5/§7 + cs 试点 M1.1，非采购计划独创）。

## Goals

- 为销售 2 个单据实体的 docStatus 轴各落地 `ErpSal<Entity>DocumentStateMachine` Bean，承载 DRAFT→CANCELLED 最小迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。
- 将 `ErpSalOrderCancelProcessor` / `ErpSalQuotationCancelProcessor` 的 `validateTransitionForCancel` 覆写为委托 Bean（`assertCanCancel` + `cancelTargetStatus`），`beforeCancel` 动态副作用（Order 的 commitment-release/intercompany-reverse）保留原位。
- 层 2 四方对照（dict ↔ `sales/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）逐实体裁定，发现项按规则 5 登记。
- 新增层 1 矩阵完备性表驱动测试（greenfield，2 个 Bean 各一）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数/审计/commitment/intercompany 副作用时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 `approveStatus` 轴（归 M3.6/M3.7，依赖本计划 M2.9/M2.10 done 后启动）。
- 不迁移 `deliveryStatus`/`receivedStatus`/`writtenOffStatus`（SAL-3/4/11 等已裁定排除-技术/派生）。
- 不触碰 `posted`（业财过账/红冲契约，不作迁移轴）；销售 docStatus cancel 不直接触发过账（approveStatus APPROVED 才触发，归 M4）。
- 不修改共享骨架 `AbstractCancelProcessor`（module-common-service 零改动）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不在本计划证 Delta 覆盖（M2 非保护域可选；cs 试点 M1.2 已实证机制；Delta 覆盖回归归 M5.3）。
- 不改变 cancel 的错误码值/参数形状/审计 actionType。
- 不构建反射型/泛型全局 `IStateMachine` 调度器。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11，落地 2 个单实体单轴 Bean + 接线 + 测试 + 四方对照）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板）、`docs/design/sales/state-machine.md`（业务状态语义）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（SAL-1/2/20/21 八属性）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`
- Skill Selection Basis: 路线图 M2.9/M2.10 指定 `nop-backend-dev` + `nop-testing`（理由同采购计划）；`state-machine-business-review-prompt.md` 匹配层 2 四方对照。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 sal-service 测试容器）。
- 前置依赖：M1.3 done（已满足）；M2.9/M2.10 deps = M1.3，门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发。

## Execution Plan

### Phase 1 - ErpSalOrder docStatus Bean（M2.10）

Status: planned
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalOrderDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSalOrderCancelProcessor.java`、`module-sales/erp-sal-service/src/test/.../TestErpSalOrderDocumentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）

- [ ] `Add`：落地 `ErpSalOrderDocumentStateMachine` Bean（显式 `assertCanCancel` + `cancelTargetStatus` + `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`，DRAFT→CANCELLED 一条边）。严格无状态（§2）。`Document` 后缀为 M3.7 approveStatus Bean 预留命名空间。在 `_vfs/erp/sal/beans/app-service.beans.xml` 注册。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`：接线 Decision 引用 M0.1 契约 §5/§7 + cs 试点 M1.1 Option A（与采购计划同源）——`ErpSalOrderCancelProcessor` 注入 `@Inject ErpSalOrderDocumentStateMachine`（非 private），覆写 `validateTransitionForCancel` 委托 Bean；`doCancel` 用 `cancelTargetStatus()` 写回；`beforeCancel`（commitment-release/intercompany-reverse）保留原位；Bean 抛 common 码（+`action`/`fromStatus`），Processor 既有 `illegalStatusException` 映射 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION`。grep 证内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield，五点：无重复/冲突边、DRAFT 可达 CANCELLED、CANCELLED 终态无出边、`assertCanCancel` DRAFT 合法/CANCELLED 抛 common 码、元数据一致、初始/终态集合正确）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Order 单条，预期无漂移；writer 含 CancelProcessor + 创建路径 + 通用 CRUD）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpSalOrderDocumentStateMachine` 存在/注册/无状态；`ErpSalOrderCancelProcessor` 委托 Bean，内联矩阵判断已移除（动态 hook 除外）。
- [ ] Order 层 1 矩阵测试本地 `mvn test -pl module-sales/erp-sal-service -am -Dtest=TestErpSalOrderDocumentStateMachineMatrix` 全绿（解除 Phase 2 复用阻塞）。

### Phase 2 - ErpSalQuotation docStatus Bean（M2.9）

Status: planned
Targets: `.../statemachine/ErpSalQuotationDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSalQuotationCancelProcessor.java`、`.../test/.../TestErpSalQuotationDocumentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（接线范式已固化）

- [ ] `Add`：落地 `ErpSalQuotationDocumentStateMachine`（同 Phase 1 结构，领域码 `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION`）；`ErpSalQuotationCancelProcessor` 覆写 `validateTransitionForCancel` 委托 Bean。Quotation cancel 无域特有 hook（保持）。注册 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（同 Phase 1 五点，Quotation 独立测试）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Quotation 单条，预期无漂移）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Quotation Bean 存在/注册/无状态；`ErpSalQuotationCancelProcessor` 委托 Bean，内联矩阵判断已移除。
- [ ] Quotation 层 1 矩阵测试本地全绿。

### Phase 3 - 层 3 既有命名动作回归 + 两实体一致性复核

Status: planned
Targets: `module-sales/erp-sal-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–2（两实体 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归（非 greenfield）——复用既有集成测试基线（`TestErpSalOrderApproval`/`TestErpSalQuotationCrudSmoke`/`TestErpSalQuotationToOrder`/`TestErpSalOrderToCashEnd` 等），证明 Processor 写回、审计、领域错误码 + 参数、终态不可恢复、commitment-release/intercompany-reverse 副作用时序不变。本地 `mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：两实体一致性复核——两 Bean 命名/注册/无状态/元数据形状一致；接线范式可追溯；四方对照记录写入本计划 Closure 段。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `mvn test -pl module-sales/erp-sal-service -am` 全绿（层 3 回归无行为回归）。
- [ ] 两实体四方对照记录可追溯、漂移处置闭环（预期无漂移）。

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_00c6f998dffeMrbfknvkC3m0a2`) — 实仓逐项核实通过（`AbstractCancelProcessor` 内联守卫 + protected、两 sales Processor 均 extends AbstractCancelProcessor、SalOrder beforeCancel 含 commitment+intercompany、SalQuotation 无域 hook、QuotationToOrderConverter 写 DRAFT、common 码存在、层 3 测试基线均存在、`sales/state-machine.md` 存在）。Rule 4/14 分组、§11 七步覆盖（Delta 正确按 §11.2 M2 出范围）、Rule 5/13 漂移处置纪律、Non-Goals、跨计划 Decision 复用可追溯到 M0.1 §5/§7 + cs 试点 M1.1（非采购计划独创，本计划独立成立）、退出标准、命名、anti-fluff 全部 PASS；无 BLOCKER/MAJOR。2 个非阻塞观察（§11 步骤显式编号可在闭包时补；Phase 1/2 数值序倒置是合理工程判断因 Order 含动态 hook 先验证接线范式）无需修订。草案审查收敛，Plan Status → active。

## Closure Gates

- [ ] 范围内行为完成（两实体 docStatus Bean + 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（架构 doc 不引用本路线图执行状态；若层 2 发现 owner doc 漂移则按 Fix 登记）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-sales/erp-sal-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` exit 0（R5=0 不漂移、R11 不增）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 销售 approveStatus 轴迁移（M3.6–M3.7）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: approveStatus 独立轴（独立 Bean `ErpSal<Entity>ApprovalStateMachine`，§3）；本计划只迁移 docStatus。approveStatus deps 含本计划 M2.9/M2.10 done 后启动。
- Successor Required: yes（触发条件 = 本计划闭包后，M3.6/M3.7 各自独立 plan 启动）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M2 非保护域 Delta 可选（模板 §11.2）；cs 试点 M1.2 已运行时实证机制。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: <待执行与独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理，新会话>
- Evidence: <待填写>
