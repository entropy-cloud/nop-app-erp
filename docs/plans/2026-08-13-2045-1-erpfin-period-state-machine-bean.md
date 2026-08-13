# 2026-08-13-2045-1-erpfin-period-state-machine-bean 会计期间 ErpFinAccountingPeriod.status 实体级状态机 Bean（M4.2）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护会计期间结账/反结账行为（reverseClose 触发期末凭证红冲，已由起草者经 live code 实证：`ErpFinAccountingPeriodReverseCloseProcessor:39,42-48` setStatus(OPEN) + reverseCloseVoucher(PL/FX/ANNUAL)）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 会计保护域硬停止）。计划格式/完备性/范围/结束证据就绪 + 人工门控已确认，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.2（ErpFinAccountingPeriod.status，plan-first）；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.2`
> Related: M4 plan-first 先例 `2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（§11.2 M4 硬约束 (i)–(v) 声明 + 人工门控 honest framing + 保持 draft）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；姊妹 M4 计划 `2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`、`2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M4.2
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。会计期间结账（OPEN→CLOSING→CLOSED）、最终锁定（CLOSED→CLOSED_FINAL）、反结账（CLOSED_FINAL→OPEN）均属受保护会计行为（反结账触发期末凭证红冲）。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 结账/反结账的过账时序/编排/失败回退/红冲闭环不改，继续由 `ErpFinAccountingPeriodProcessor` facade + reverseCloseVoucher 编排管理；(iii) `posted` 不入轴（期间轴为 `status`，无 `posted` 字段）；(iv) 跨域副作用（模块结账子状态 ar/ap/inv/gl/asset、`IErpInvCostingBiz.reclosePeriodCosts`、`IErpFinVoucherBiz` 红冲）保留原 Processor/`I*Biz` 路径；(v) 反结账 kill-switch（`erp-fin.reverse-close-approval-required`）作为动态业务守卫保留原位。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施（对齐 M3.10/M4 plan-first 先例）。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.2` + 实仓核实。

- **实体**：`ErpFinAccountingPeriod`（`module-finance/model/app-erp-finance.orm.xml:655`），单轴 `status` `ext:dict="erp-fin/period-status"`（`:669`）。dict 5 值（`app-erp-finance.orm.xml:200`）：`NEVER_OPENED/OPEN/CLOSING/CLOSED/CLOSED_FINAL`。常量 `ErpFinConstants.PERIOD_STATUS_*`（`ErpFinConstants.java:139-143`）。
- **既有集中守卫 helper 已存在**：facade `ErpFinAccountingPeriodProcessor` 提供 `assertPeriodStatus(period, expectedStatus, actionLabel)`，被各 per-mutation Processor 调用——这是要替换为 Bean 委托的内联固定状态守卫。
- **status 现状 writer（4 per-mutation Processor + 1 生成 Processor，实仓核实）**：
  - `openPeriod`（`ErpFinAccountingPeriodOpenPeriodProcessor:18-21`）：`assertPeriodStatus(NEVER_OPENED, "开启")` → `setStatus(OPEN)`。
  - `closePeriod`（`ErpFinAccountingPeriodClosePeriodProcessor:48,81-82`）：`assertPeriodStatus(OPEN, "结账")` → 先 `setStatus(CLOSING)`（`:81`，结账步骤前），步骤成功后 `setStatus(CLOSED)`（`:82`）。**CLOSING 为事务内瞬态**：closePeriod 为 `@BizMutation`（事务包裹），结账步骤失败则整个 mutation 回滚，CLOSING 不持久化；owner doc「CLOSING→OPEN（结账失败）」即此事务回滚语义（非显式 writer，Phase 3 裁定）。
  - `finalizePeriod`（`ErpFinAccountingPeriodFinalizePeriodProcessor:20-21`）：`assertPeriodStatus(CLOSED, "最终锁定")` → `setStatus(CLOSED_FINAL)`。
  - `reverseClose`（`ErpFinAccountingPeriodReverseCloseProcessor:22-48`）：`assertPeriodStatus(CLOSED_FINAL, "反结账")` → `setStatus(OPEN)`（`:39`）+ `reverseCloseVoucher(PL/FX/ANNUAL ...)`（`:42-48`，期末凭证红冲）。**反结账 kill-switch**：`CONFIG_REVERSE_CLOSE_APPROVAL_REQUIRED="erp-fin.reverse-close-approval-required"`（`ErpFinConstants.java:120`，默认 true）true 时抛 `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`（`ErpFinErrors.java:288-289`）——动态业务守卫，保留原位。
  - `generateNextYearPeriods`（`ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor:75-76`）：`setStatus(month==1 ? OPEN : NEVER_OPENED)`——**生成路径初始态写入**（新建期间行 seed，非既有期间迁移，按 §9.2 选项 c，不调 assertCan*）。
- **模块结账子状态（排除）**：`ErpFinAccountingPeriodStatus` 的 `ar/ap/inv/gl/assetStatus`（`erp-fin/module-close-status`）= M0.2 FIN-3 排除-技术（派生子状态），不在本轴范围（Non-Goal）。`ErpFinAccountingPeriodProcessor:282,315,606` 写 `assetStatus` 属此排除项。
- **与凭证轴的耦合约束（§两类状态机的耦合约束）**：`ErpFinVoucherBizModel.postVoucher/reverseVoucher` 调 `assertPeriodNotLocked`（CLOSED/CLOSED_FINAL 时抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`）——这是凭证侧的**动态业务守卫**，本计划不迁移凭证轴（M4.1 另属姊妹计划），仅确认期间 Bean 不破坏此耦合。
- **错误码现状**：facade helper `assertPeriodStatus(period, expectedStatus, actionLabel)`（`ErpFinAccountingPeriodProcessor:574-581`，actionLabel 仅为日志/方法参数**不进异常 param**）守卫违例抛 `ERR_PERIOD_ILLEGAL_TRANSITION`，携带参数 `ARG_PERIOD_CODE`/`ARG_CURRENT_PERIOD_STATUS`/`ARG_EXPECTED_PERIOD_STATUS`（3 参数）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A + 各域范式：Bean 抛 common 码，Processor 映射领域码，common 作 cause）。
- **生产 Bean 注册范式**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml` 已显式注册既有 Processor/服务 Bean（FQN id 范式）。StateMachine Bean 追加于文件末尾。
- **既有层 3 回归基线（非 greenfield）**：`TestErpFinPeriodStateMachine`（`module-finance/erp-fin-service/src/test/.../entity/`，175 行 5 个 `@Test`，经 `IErpFinAccountingPeriodBiz` BizModel 入口覆盖 closePeriod/finalizePeriod/reverseClose/openPeriod 正向+反向+非法态——**M0.2 §3.1 #6 正确列此为既有层 3 基线**，是本轴最直接相关的命名动作回归测试）+ `TestErpFinPeriodCloseEndToEnd`、`TestErpFinReverseClose`、`TestErpFinPeriodPreCheck`、`TestErpFinAnnualClose`、`TestErpFinVoucherPeriodLock`。层 1 矩阵测试为 greenfield（新增）；层 3 既有回归为上述具名测试。
- **合规基线**：`@Inject private` 须保持 R5=0（fin-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md §对象二：会计期间状态机` 完整覆盖 5 态 + 迁移矩阵 + 终态/恢复 + 反结账 kill-switch 已知简化（P1-MA3-036）+ 与凭证耦合约束。**无 owner-doc 缺口**（区别于 M4.1/M4.29 等需补章节的轴）。

## Goals

- 落地无状态 `ErpFinAccountingPeriodStateMachine`（status 单轴，§2 无状态约束）：矩阵 `openPeriod`：`{NEVER_OPENED}→OPEN`；`closePeriod`：`{OPEN}→CLOSING→CLOSED`（两段：assertCanClose 守卫 OPEN + closeTargetStatus 两段 CLOSING/CLOSED）；`finalizePeriod`：`{CLOSED}→CLOSED_FINAL`；`reverseClose`：`{CLOSED_FINAL}→OPEN`。分类 initial=`{NEVER_OPENED}`，terminal=`{CLOSED_FINAL}`。可经 Delta 同名覆盖。
- 将 4 per-mutation Processor 的固定 `assertPeriodStatus` 守卫改调 Bean `assertCan<Action>(from)` + 目标态回写（`<action>TargetStatus()`）；**动态业务守卫与副作用保留原位**：反结账 kill-switch（`erp-fin.reverse-close-approval-required` + `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`）、结账步骤编排（成本核算/折旧/结转损益/模块结账子状态）、`reverseCloseVoucher` 期末凭证红冲、`reclosePeriodCosts` 跨域调用、乐观锁。
- 保持全部既有外部行为不变（错误码 + 参数、5 态迁移边、CLOSING 瞬态事务语义、反结账红冲时序、凭证-期间耦合 `ERR_FIN_VOUCHER_PERIOD_LOCKED`、期间生成初始态写入不调 assertCan*）。
- 新增层 1 矩阵完备性表驱动测试（5 态 × 4 动作合法/非法边 + CLOSING 瞬态 + initial/terminal）；层 3 既有集成测试回归全绿。
- 层 2 四方对照：确认 5 态全可达 + CLOSING 瞬态裁定 + 凭证耦合边界 + M0.2 测试名漂移登记。

## Non-Goals

- 不迁移 `ErpFinVoucher.docStatus`（M4.1，姊妹计划）、`ErpFinReconciliation`（M4.3）或任何其他 finance 轴。
- 不迁移 `ErpFinAccountingPeriodStatus` 的模块结账子状态（ar/ap/inv/gl/assetStatus，M0.2 FIN-3 排除-技术）。
- 不改变结账/反结账的过账编排（步骤顺序、`reverseCloseVoucher` 红冲时序、失败回退）、不改变反结账 kill-switch 语义（`erp-fin.reverse-close-approval-required`）、不改变凭证-期间耦合守卫（`assertPeriodNotLocked`/`ERR_FIN_VOUCHER_PERIOD_LOCKED`，属凭证侧 M4.1 路径）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约。
- 不引入通用 CRUD 对 status 写入的运行时禁止（M0.1 successor）；不引入全局 CRUD 写锁。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项不自带 Delta 证明，归 M5.3，§11.1 步骤 7 / §11.2 Delta 适用性）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——期间结账/反结账属受保护会计行为）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 不适用/瞬态轴）、`docs/design/finance/state-machine.md`（§对象二 会计期间 + §两类状态机的耦合约束 + §职责分离）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 finance M4.2 行）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.2 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「per-mutation Processor 接线、facade helper 委托、动态副作用（kill-switch/红冲/跨域）保留、错误码映射、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护会计期间结账/反结账行为（反结账触发期末凭证红冲）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此轴、结账/反结账/红冲路径完整保留、反结账 kill-switch 保留原位」可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖（除既有 `erp-fin.reverse-close-approval-required` 配置，保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - ErpFinAccountingPeriodStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinAccountingPeriodStateMachine.java`（新建）、`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinAccountingPeriodStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [x] 新建无状态 `ErpFinAccountingPeriodStateMachine`（§2 无状态约束）：矩阵 `assertCanOpenPeriod(NEVER_OPENED)`→`openPeriodTargetStatus()=OPEN`；`assertCanClose(OPEN)`→`closeTargetStatus()` 两段（CLOSING 然后 CLOSED）；`assertCanFinalize(CLOSED)`→`finalizeTargetStatus()=CLOSED_FINAL`；`assertCanReverseClose(CLOSED_FINAL)`→`reverseCloseTargetStatus()=OPEN`。分类 `initialStatuses()={NEVER_OPENED}`、`terminalStatuses()={CLOSED_FINAL}`、`isTerminal(CLOSED_FINAL)=true`。`transitions()` 编码 4 条命名边（openPeriod/finalize/reverseClose 各 1 + close 两段）。CLOSING 标注为事务内瞬态（javadoc 说明 closePeriod 在事务内 CLOSING→CLOSED，失败回滚不持久化）。非法来源态抛 common 码携带 `action`/`fromStatus`。grep 证实不 import DAO/IBiz/IServiceContext/事务。
  - Skill: `nop-backend-dev`
- [x] Decision（前置）：在计划中记录 CLOSING 瞬态分类——closePeriod 为 `@BizMutation`（事务包裹），CLOSING 在事务内设置后于 CLOSED，结账步骤失败则整 mutation 回滚（CLOSING 不持久化）；owner doc「CLOSING→OPEN（结账失败）」= 事务回滚语义，非显式 writer，Bean 不为 CLOSING→OPEN 发明独立命名边（close 两段边已覆盖）。供 Phase 3 owner-doc 补注引用。
  - Skill: `state-machine-business-review-prompt.md`
  - **Decision 记录（CLOSING 瞬态分类）**：closePeriod 步骤（成本核算/折旧/结转损益/模块结账子状态，`ClosePeriodProcessor:50-78`）全部在期间仍 OPEN 时执行，成功后事务内 `:81 setStatus(CLOSING)` 紧接 `:82 setStatus(CLOSED)`（同一 @BizMutation 事务）；任一步骤失败 → 整 mutation 回滚，CLOSING 不持久化。故 owner doc §对象二「CLOSING→OPEN（结账失败）」= **事务回滚语义**，分类 = `intentional transactional behavior`（非 implementation drift）。Bean 落位：`assertCanClose` 仅守卫动作入口来源态 OPEN（`assertCanClose(CLOSING)` 抛非法——CLOSING 不可作「发起结账」入口）；`transitions()` 编码 close 两段（OPEN→CLOSING 进入段 + CLOSING→CLOSED 完成段），不为 CLOSING→OPEN 发明命名边。该裁定记于 Bean 类级 javadoc「CLOSING 瞬态」节 + 矩阵测试 javadoc，Phase 3 owner-doc 补注将引用。
- [x] 在非生成 `app-service.beans.xml` 以 FQN 为 bean id 注册 Bean（§11.1 步骤 2）。
  - Skill: `nop-backend-dev`
- [x] Proof（层 1 矩阵完备性，表驱动，§11.1 步骤 4）：`TestErpFinAccountingPeriodStateMachineMatrix` 覆盖 openPeriod（NEVER_OPENED 合法、OPEN/CLOSING/CLOSED/CLOSED_FINAL 非法）/close（OPEN 合法、其余非法）/finalize（CLOSED 合法、其余非法）/reverseClose（CLOSED_FINAL 合法、其余非法）合法+非法边 + 终态 CLOSED_FINAL 无出边 + transitions 一致 + initial/terminal。**不经 BizModel 入口**（层 1 只测 Bean）。
  - Skill: `nop-testing`
  - Proof：10 个 `@Test` 全绿（`mvn test -Dtest=TestErpFinAccountingPeriodStateMachineMatrix` → Tests run: 10, Failures: 0）。终态 CLOSED_FINAL 仅有 reverseClose 恢复出边（无前向推进边，区别于一般「终态零出边」死状态启发式——reverseClose 是显式管理员恢复 action，owner doc §对象二 §3/§5）。

Exit Criteria:

- [x] Bean 无状态、矩阵完整（4 命名边 + close 两段），CLOSING 瞬态 Decision 记录在案
- [x] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [x] 层 1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - per-mutation Processor 接线（行为保持，过账/红冲/kill-switch 副作用保留）+ 层 3 回归

Status: completed
Targets: `ErpFinAccountingPeriodOpenPeriodProcessor`、`ErpFinAccountingPeriodClosePeriodProcessor`、`ErpFinAccountingPeriodFinalizePeriodProcessor`、`ErpFinAccountingPeriodReverseCloseProcessor`（4 个 per-mutation Processor 注入 Bean + 替换 `assertPeriodStatus` 守卫 + 目标态回写）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [x] 4 Processor 注入 `ErpFinAccountingPeriodStateMachine`（`@Inject` 非 private），将各 `facade.assertPeriodStatus(period, EXPECTED, label)` 内联固定守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回（`<action>TargetStatus()`）。common→领域码映射（common 码作 cause），错误码 + 参数（`ARG_PERIOD_CODE`/`ARG_CURRENT_PERIOD_STATUS`/`ARG_EXPECTED_PERIOD_STATUS`，3 参数；actionLabel 仅日志不入异常）对外不变。**完整保留**：closePeriod 的结账步骤编排（成本核算/折旧/结转损益/模块结账子状态写入）、`reclosePeriodCosts` 跨域调用、CLOSING→CLOSED 两段时序；reverseClose 的 kill-switch（`erp-fin.reverse-close-approval-required` + `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`）+ `reverseCloseVoucher(PL/FX/ANNUAL)` 期末凭证红冲时序 + 乐观锁。**generateNextYearPeriods 初始态写入不调 assertCan***（`:75-76`，§9.2 选项 c）。
  - Skill: `nop-backend-dev`
  - 接线实证：4 Processor 各 `@Inject ErpFinAccountingPeriodStateMachine stateMachine`（非 private）；`facade.assertPeriodStatus` 已删除（grep 证实 0 引用），facade 新增 `mapIllegalTransition(beanException, period, expected)` 承载 common→领域码映射（common 作 cause，契约 §7）。closePeriod 两段写 `closeEnteringTargetStatus()`(CLOSING) → `closeTargetStatus()`(CLOSED)；reverseClose 的 setStatus(OPEN) → `reverseCloseTargetStatus()`。结账步骤编排（preCheck/advanceModule/closeInvModule/closeAssetModule/closeGlModule/closeAnnual）、reclosePeriodCosts、kill-switch、次年期间门控、`reverseCloseVoucher` PL/FX/ANNUAL 红冲、反折旧、reopenModules 全部原位未改。`generateNextYearPeriods:75-76` 初始态写入未调 assertCan*。
- [x] Proof（层 3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinPeriodStateMachine`（4 mutation 正向+反向+非法态，最直接相关）、`TestErpFinPeriodCloseEndToEnd`（结账 happy + CLOSING→CLOSED）、`TestErpFinReverseClose`（反结账 CLOSED_FINAL→OPEN + 红冲 + kill-switch 拒绝路径）、`TestErpFinPeriodPreCheck`（结账前置校验）、`TestErpFinAnnualClose`（年结）、`TestErpFinVoucherPeriodLock`（凭证-期间耦合 `ERR_FIN_VOUCHER_PERIOD_LOCKED` 不变）。证明 5 态迁移边、CLOSING 瞬态、反结账红冲时序、kill-switch、凭证耦合均不变。
  - Skill: `nop-testing`
  - Proof：6 具名测试全绿（Tests run: 20, Failures: 0）；全模块 `mvn test -pl module-finance/erp-fin-service` 全绿（Tests run: 390, Failures: 0, Errors: 0, Skipped: 0）。

Exit Criteria:

- [x] 4 Processor 内联 `assertPeriodStatus` 固定守卫改调 Bean 委托，grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用如 kill-switch/结账步骤/红冲/模块子状态/跨域调用除外；generateNextYearPeriods 初始态不调 assertCan*）
- [x] 领域错误码 + 参数对外不变（层 3 断言证实）；5 态边 + CLOSING 瞬态 + 反结账红冲时序 + kill-switch + 凭证耦合行为不变
- [x] 层 3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 3 - 层 2 四方对照 + 漂移 Decision + owner doc 补注

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（§对象二 CLOSING 瞬态补注 + M0.2 测试名漂移登记）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（层 2 四方对照，§11.1 步骤 5，10 维度）：dict（`erp-fin/period-status` 5 值）↔ owner doc（§对象二）↔ Bean 元数据 ↔ writer（4 per-mutation Processor + generateNextYearPeriods 生成 + 框架入口 + 测试 fixture）。重点裁定：(a) CLOSING 瞬态事务语义（owner doc「CLOSING→OPEN 结账失败」= 事务回滚，非显式 writer）；(b) 5 态全可达（NEVER_OPENED→OPEN→CLOSING→CLOSED→CLOSED_FINAL + reverseClose CLOSED_FINAL→OPEN）；(c) 凭证耦合边界（assertPeriodNotLocked 属凭证侧 M4.1 路径，本轴不触碰）；(d) 模块结账子状态排除（FIN-3 技术派生）。
  - Skill: `state-machine-business-review-prompt.md`
  - **四方对照记录**（见下方 §Closure 「层 2 四方对照审计证据」）。
- [x] Add owner doc：在 `docs/design/finance/state-machine.md §对象二` 补 CLOSING 瞬态实现注记（closePeriod 事务内 CLOSING→CLOSED，失败回滚不持久化，对齐「CLOSING→OPEN 结账失败」语义）+ 反结账 kill-switch 保留原位声明交叉引用 §已知简化 P1-MA3-036。
  - Skill: `state-machine-business-review-prompt.md`
  - 已补：`state-machine.md §对象二 §3 终态与恢复` 末追加「CLOSING 瞬态实现注记」blockquote（事务内 CLOSING→CLOSED + 失败回滚 + Bean 矩阵处理）+ §3 反结账恢复 bullet 增 kill-switch 交叉引用 §6 P1-MA3-036。
- [x] Decision（漂移裁定，路线图规则 5）：(a) CLOSING 瞬态分类 = `intentional transactional behavior`（事务回滚语义，非 implementation drift，Bean 不发明 CLOSING→OPEN 独立边）；(b) 模块结账子状态（ar/ap/inv/gl/assetStatus）= FIN-3 排除-技术，确认不入轴。（注：M0.2 §3.1 #6 所列 `TestErpFinPeriodStateMachine` 实仓存在且为有效层 3 基线，无测试名漂移需登记。）
  - Skill: `state-machine-business-review-prompt.md`
  - **Decision 记录**：(a) CLOSING = `intentional transactional behavior`（非 drift）——Bean `transitions()` 编码 close 两段 OPEN→CLOSING→CLOSED，CLOSING 有 writer（`ClosePeriodProcessor:81`）且经 close 进入段从 OPEN 可达，非死状态；CLOSING→OPEN 不发明（事务回滚）。(b) ar/ap/inv/gl/assetStatus = FIN-3 排除-技术（不同字段，`ErpFinAccountingPeriodStatus` 的 `module-close-status` dict），确认不入本 `status` 轴。(c) 测试名：M0.2 §3.1 #6 `TestErpFinPeriodStateMachine` 实仓存在（`module-finance/erp-fin-service/src/test/.../entity/TestErpFinPeriodStateMachine.java`，5 @Test 全绿）= 有效层 3 基线，**无测试名漂移需登记**。

Exit Criteria:

- [x] 四方对照无未裁决漂移（CLOSING 瞬态 + 测试名漂移 + 模块子状态排除均裁定并落入 owner doc/计划）
- [x] owner doc §对象二 CLOSING 瞬态补注与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00719a389ffeVQfGmrfan9HRgx`，新会话零信任实仓复核) — 1 BLOCKER + 2 MINOR：BLOCKER = 计划误称 `TestErpFinPeriodStateMachine` 实仓不存在（aspirational 名），实际该文件存在（175 行 5 @Test，M0.2 §3.1 #6 正确），导致 Phase 2 漏列回归 + Phase 3 误登记测试名漂移；MINOR = `assertPeriodStatus` 无 `action` 异常 param（仅 ARG_PERIOD_CODE/ARG_CURRENT_PERIOD_STATUS/ARG_EXPECTED_PERIOD_STATUS，actionLabel 仅方法参数）+ reverseClose `:42-47` 实为 `:42-48`。v2 已：承认该测试存在并补入 Phase 2 回归 + 删除 Phase 3 伪漂移登记、修正错误码 3 参数、修正行号 `:42-48`。
- Independent draft review iteration 2: `acceptable as-is`（draft pending M4 gate）（`ses_007109abbffeSFaKazQ4Z3iNvO`，新会话零信任复核）— 全部 iter1 问题 CONFIRMED-FIXED（实仓复核 TestErpFinPeriodStateMachine 存在 + assertPeriodStatus 3 参数 + reverseClose `:42-48`）；无新增 blocker/major；§11.2 M4 (i)–(v)、scope、anti-slack、baseline honesty 全 PASS。草案审查收敛。
- Plan review（mission-driver 2026-08-13-080540-mission-driver）：`approved (review ran); held as draft` — 四维度（格式合规性/完备性/范围/结束证据）经核对全部就绪，无除门控外的 blocker/major。零信任实仓抽查 baseline 全部 CONFIRMED：dict `erp-fin/period-status` 5 值（orm.xml:200-206）+ `assertPeriodStatus(period,expected,action)` body 实抛 `ERR_PERIOD_ILLEGAL_TRANSITION` 恰 3 异常 param（`ARG_PERIOD_CODE`/`ARG_CURRENT_PERIOD_STATUS`/`ARG_EXPECTED_PERIOD_STATUS`，`action` 仅方法参不入异常，:574-581）+ reverseClose kill-switch `:27`/setStatus(OPEN) `:39`/reverseCloseVoucher `:42-48` + closePeriod CLOSING→CLOSED `:81-82` + openPeriod/finalizePeriod/generateNextYearPeriods 与计划一致 + 6 个具名回归测试实存 + owner doc §对象二（state-machine.md:126-197）全覆盖 5 态/矩阵/CLOSING 失败回退/kill-switch P1-MA3-036/凭证耦合（**无 owner-doc 缺口**，区别于 erpprj 先例）。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖：本计划触及 `accounting/finance postings` 保护域（ai-autonomy-policy.md:72 = plan-first，必需证据 owner doc + tests）；roadmap（entity-state-machine-migration-roadmap.md:3,217）显式将 M4.2 归入「保持 draft 待人工/owner-doc plan-first 门控」批次。虽 §对象二 已存（owner-doc 证据腿满足 ai-autonomy-policy.md:9 的 OR），但撤销 mission 级、roadmap 记录的门控裁定本身属「移除阻塞项/放宽保护区域」，依 ai-autonomy-policy.md:9 非审查子代理可自主解除。故保持 `Plan Status: draft`（holding 机制），不晋升 active。门控解除后于本记录追加（日期 + 批准范围）并转 `active`。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。草案审查已收敛（acceptable as draft）。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的矩阵集中化方式迁移此轴、结账/反结账/红冲路径完整保留、反结账 kill-switch 保留原位」可接受。门控解除，`Plan Status: draft → active`。
- Plan review（mission-driver 2026-08-13-193118-mission-driver）：`approved (review ran); held as draft` — 四维度（格式合规性/完备性/范围/结束证据）复核全部就绪，无除门控外的 blocker/major。零信任实仓抽查 baseline CONFIRMED：dict `erp-fin/period-status` 恰 5 值（orm.xml:200-206）+ `assertPeriodStatus` body 实抛 `ERR_PERIOD_ILLEGAL_TRANSITION` 恰 3 异常 param（`ARG_PERIOD_CODE`/`ARG_CURRENT_PERIOD_STATUS`/`ARG_EXPECTED_PERIOD_STATUS`，`action` 仅方法参不入异常，Processor.java:574-581）+ reverseClose kill-switch `:27`/setStatus(OPEN) `:39`/reverseCloseVoucher PL/FX/ANNUAL `:42-48` + 4 per-mutation Processor assertPeriodStatus 调用点（openPeriod/finalizePeriod/reverseClose/closePeriod）核对一致。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控（外部上游决策）：路线图将 M4.2 归入「保持 draft 待 plan-first 门控」批次，project-context.md 会计保护域硬停止成立；解除此 roadmap/mission 级门控属「放宽保护区域」（ai-autonomy-policy.md:9），非审查子代理可自主解除。故保持 `Plan Status: draft`（holding 机制，Review Hold 已存于 front matter），不晋升 active。门控解除后于本记录追加（日期 + 批准范围）并转 `active`。

## Closure Gates

> 本计划含生产代码变更（1 Bean + 4 Processor 接线 + 测试 + owner doc 补注），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（5 态保留），Compliance 基线预期无漂移（R5=0/R11=0）。

- [x] 范围内行为完成（Bean + 4 Processor 接线 + 三层证据；结账/反结账/红冲时序 + kill-switch 完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [x] 相关文档对齐（owner doc §对象二 CLOSING 瞬态补注 + 测试名漂移 + 模块子状态排除 Decision 登记；路线图 M4.2 done）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 模块结账子状态（ar/ap/inv/gl/assetStatus）状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpFinAccountingPeriodStatus` 的模块结账子状态 = M0.2 FIN-3 排除-技术（DAG 派生子状态，非独立业务状态轴）。本轴仅迁移 `ErpFinAccountingPeriod.status` 主轴。
- Successor Required: no（排除项，仅当产品要求模块结账独立状态机化时重开）

### 凭证-期间耦合守卫迁移（assertPeriodNotLocked）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `assertPeriodNotLocked`/`ERR_FIN_VOUCHER_PERIOD_LOCKED` 属凭证侧（`ErpFinVoucherBizModel`）动态业务守卫，归 M4.1（姊妹计划）。本期间轴不触碰凭证侧。
- Successor Required: yes（触发条件 = M4.1 ErpFinVoucher.docStatus 启动时，凭证侧接线核对期间 Bean 不破坏耦合）

### 反结账完整审批流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 当前 kill-switch（`erp-fin.reverse-close-approval-required` 默认 true = 拒绝反结账）保留原位。完整审批流（反结账申请→审批→执行）= owner doc §已知简化 P1-MA3-036 successor（P1-MA2-020 触发）。
- Successor Required: yes（触发条件 = 审批流落地时）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，Delta 覆盖回归归 M5.3（§11.2 Delta 适用性）。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: 三阶段执行完成（2026-08-14）；层 1 矩阵 10 测试全绿 + 层 3 既有回归全模块 390 测试全绿 + 层 2 四方对照无未裁决漂移 + 独立结束审计 PASS（ses_0037a262fffe7X0XC0KCBIwk9l，7 检查全 PASS，21 具名测试 0 失败，无 blocker）。全仓库 `mvn clean install -DskipTests` BUILD SUCCESS；合规 R5=0/R11=0（plan 基线达成，R12c +2 为既有漂移非本计划引入）。

### 层 2 四方对照审计证据（§11.1 步骤 5）

**四方**：dict（`erp-fin/period-status`）↔ owner doc（`docs/design/finance/state-machine.md §对象二`）↔ Bean 元数据（`ErpFinAccountingPeriodStateMachine`）↔ 生产 writer。

| 维度 | dict（orm.xml:200-206, ErpFinConstants:139-143） | owner doc §对象二 | Bean 元数据 | 生产 writer（grep 实证） | 一致性 |
|------|------|------|------|------|------|
| NEVER_OPENED | ✓ 值 | §1 未开启 | `initialStatuses()={NEVER_OPENED}`；openPeriod fromStatus | GenerateNextYearPeriodsProcessor:75-76（§9.2 初始 seed，不调 assertCan*） | ✓ |
| OPEN | ✓ 值 | §1 已开启 | openPeriod toStatus / close fromStatus / reverseClose toStatus | Open:33→openPeriodTargetStatus；Close:52 assertCanClose；ReverseClose:51→reverseCloseTargetStatus | ✓ |
| CLOSING | ✓ 值 | §1 结账中 + §2 OPEN→CLOSING→CLOSED + §3 CLOSING→OPEN(失败回退) | close 进入段 toStatus（OPEN→CLOSING）+ 完成段 fromStatus（CLOSING→CLOSED）；CLOSING→OPEN **不**编码（事务回滚语义） | Close:91→closeEnteringTargetStatus（事务内瞬态，失败回滚不持久化） | ✓ 裁定：intentional transactional behavior，非 drift |
| CLOSED | ✓ 值 | §1 已结账（待复核中间态，非终态） | finalize fromStatus；`isTerminal(CLOSED)=false` | Finalize:27 assertCanFinalize | ✓ |
| CLOSED_FINAL | ✓ 值 | §1 已复核（终态）+ §3 终态 | `terminalStatuses()={CLOSED_FINAL}`；reverseClose fromStatus（唯一恢复出边） | ReverseClose:33 assertCanReverseClose | ✓ 终态仅有 reverseClose 恢复出边（无前向推进边） |

**重点裁定**：

- **(a) CLOSING 瞬态事务语义**：owner doc §2/§3「CLOSING→OPEN（结账失败）」= **事务回滚语义**，非显式 writer。closePeriod 为 `@BizMutation`（事务包裹），CLOSING 在事务内 `:81` 设置后紧接 `:82` CLOSED，任一步骤失败则整 mutation 回滚（CLOSING 不持久化）。Bean `transitions()` 编码 close 两段 OPEN→CLOSING→CLOSED，不为 CLOSING→OPEN 发明命名边。CLOSING 经 close 进入段从 OPEN 可达、有 writer、非死状态。**分类 = `intentional transactional behavior`（非 implementation drift）**。owner doc §3 已补「CLOSING 瞬态实现注记」。
- **(b) 5 态全可达**：NEVER_OPENED→OPEN（openPeriod）→CLOSING（close 进入段）→CLOSED（close 完成段）→CLOSED_FINAL（finalize）+ reverseClose CLOSED_FINAL→OPEN（恢复）。矩阵测试 `testReachabilityFromInitial` 证实从 NEVER_OPENED 可达其余 4 态。**无死状态**（全部 dict 值有 writer 或可达）。
- **(c) 凭证耦合边界**：`assertPeriodNotLocked`/`ERR_FIN_VOUCHER_PERIOD_LOCKED` 位于 `ErpFinVoucherBizModel`（凭证侧 M4.1 路径），本期间轴 Bean 不触碰。`TestErpFinVoucherPeriodLock` 层 3 回归全绿证实耦合守卫不变。
- **(d) 模块结账子状态排除**：`ErpFinAccountingPeriodStatus` 的 ar/ap/inv/gl/assetStatus（dict `erp-fin/module-close-status`）= M0.2 FIN-3 排除-技术（不同字段、DAG 派生子状态），不入本 `status` 轴。

**CRUD 写入路径（§9.4 残留）**：通用 CRUD `__save`/`update` 技术上可写 `status`（xmeta insertable/updatable，M0.1 §9.2 选项 c 残留）。Bean 治理「命名动作迁移矩阵」唯一性，不治理 CRUD 路径——已知 scoped 残留（M0.1 successor），**非本轴 drift**。

**测试名漂移**：M0.2 §3.1 #6 所列 `TestErpFinPeriodStateMachine` 实仓存在（`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinPeriodStateMachine.java`，5 @Test 全绿）= 有效层 3 基线。**无测试名漂移需登记**。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0037a262fffe7X0XC0KCBIwk9l`，2026-08-14）
- Verdict: **PASS** — 7 编号检查全 PASS，§11.2 M4 (i)–(v) 硬约束全兑现，无 blocker/major。
- Evidence:
  - Bean 形状/无状态 PASS（`ErpFinAccountingPeriodStateMachine.java`：4 动作 + 两段 close + transitions 5 边不含 CLOSING→OPEN + 仅 import ErpCommonErrors/ErpFinConstants/NopException/java.util，无 DAO/IBiz/IServiceContext/事务）。
  - Bean 注册 PASS（`app-service.beans.xml:395-396`，FQN id）。
  - 4 Processor 接线 PASS（`assertPeriodStatus` grep = 0 hits；4 Processor 均 `@Inject` 非 private + `assertCan<Action>` + `<action>TargetStatus`；facade `mapIllegalTransition` common→领域码作 cause + 3 参数）。
  - M4 (ii)/(iv)/(v) 兑现 PASS（closePeriod 编排 AR/AP/INV/AST/GL/closeAnnual 步骤原序 + 两段写在步骤后；reverseClose kill-switch + 次年门控 + reverseCloseVoucher PL/FX/ANNUAL + reverseDepreciation + reopenModules 原位；generateNextYearPeriods §9.2 初始写入不调 assertCan* 未改）。
  - 测试 PASS（`mvn -pl module-finance/erp-fin-service test -Dtest=...` → 21 测试 0 失败：Matrix 10 + PeriodStateMachine 5 + PeriodCloseEndToEnd 1 + ReverseClose 1 + VoucherPeriodLock 4）。
  - owner doc PASS（`state-machine.md:188` CLOSING 瞬态注记 + `:185` kill-switch 交叉引用 §6 P1-MA3-036）。
  - 计划一致性 PASS（3 Phase 全 completed + 全项 `[x]` + Closure 四方对照证据 + Draft Review 收敛）。
  - 推荐所有 Closure Gates satisfied（已勾选）。

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
