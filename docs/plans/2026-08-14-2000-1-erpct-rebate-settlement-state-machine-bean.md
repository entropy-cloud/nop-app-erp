# 2026-08-14-2000-1-erpct-rebate-settlement-state-machine-bean 合同域 ErpCtRebateSettlement.status 实体级状态机 Bean（M4.65）

> Plan Status: active
> Review Hold: §11.2 M4 (i) plan-first 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）（postSettlement 触发 credit-memo AP/AR 负额发票生成 + ErpCtRebateAccrual.isSettled 回写）。门控非起草者/审查者可自主解除——经人工确认解除；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.65（ErpCtRebateSettlement.status），plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` CT-3
> Related: M3 同域先例 `2026-08-13-1430-3-erpct-version-rebate-state-machine-beans.md`（M3.18 ContractVersion done + M3.19 RebateAgreement done，退化分类轴 Bean 范式）；该计划 Deferred 明确列出 `ErpCtRebateSettlement.status（M4.65）→ Successor Required: yes（触发条件 = M4.65 工作项启动时）`——本计划即该 successor。M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.65
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。postSettlement DRAFT→POSTED 生成 credit-memo 负额发票（AP/AR `posted=false`，非 GL 凭证——O-4 架构豁免 `docs/architecture/posting-exemptions.md §ErpCtRebateSettlementBizModel`）+ 标记 `ErpCtRebateAccrual.isSettled=true`。声明 §11.2 M4 硬约束：(i) plan-first；(ii) credit-memo 生成编排/accrual 回写不改；(iii) `posted` 不入轴；(iv) 跨域副作用（AP/AR 发票创建 + accrual 回写）保留原 Processor/`IDaoProvider` 路径；(v) 既有行为不改。
>
> **规则 14 bundling 声明**：M4.65 为 contract 域最后一个状态轴工作项（单实体单轴），无需 bundling。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` CT-3 + 实仓核实。M3.19 `ErpCtRebateAgreementStateMachine`（退化分类轴 Bean）+ M3.18 `ErpCtContractVersionStateMachine` 已落地 done，是本计划的**同域接线模板参照**。

- **ErpCtRebateSettlement**（M4.65 status，单轴，per-mutation Processor 单写路径）：
  - **status 3 态**（`erp-ct/settlement-status`，`app-erp-contract.orm.xml:73-77`）：DRAFT/POSTED/CANCELLED。常量 `ErpCtConstants:24-26`（`SETTLEMENT_STATUS_DRAFT`/`SETTLEMENT_STATUS_POSTED`/`SETTLEMENT_STATUS_CANCELLED`）。
  - **writer（唯一生产 writer）**：`ErpCtRebateSettlementPostSettlementProcessor:90` 写 `setStatus(SETTLEMENT_STATUS_POSTED)`（DRAFT→POSTED，guard line 45 检查 `status==DRAFT` 否则抛 `ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION` `erp.err.ct.settlement-illegal-transition`）。**全域零 setStatus(CANCELLED) writer**——CANCELLED 为死状态。
  - **初始态写入路径**：DRAFT 经 CRUD `save` 写入（`defaultPrepareSave:63-70` 仅 seed businessDate，status 由调用方请求体提供——§9.2 选项 c 残留 CRUD 路径，同 Contract NEGOTIATION 先例）。
  - **无 reverse/unpost/cancel 命名动作**：BizModel `ErpCtRebateSettlementBizModel` 仅 `postSettlement` @BizMutation（`:73-76` 单行委托 Processor），无 `reverseSettlement`/`unpost`/`cancel` mutation。
  - **side-effect（DRAFT→POSTED）**：(a) 聚合未结算 accruals `findUnsettledAccruals`（`:52-56`）；(b) 按 `agreement.rebateType` 分支——PURCHASE → `createNegativeApInvoice`（`:101-132`，AP 负额发票 `posted=false`），SALES → `createNegativeArInvoice`（`:134-165`，AR 负额发票 `posted=false`）；(c) `settlement.setCreditMemoBillType/BillCode`（`:72-77`）；(d) `ErpCtRebateAccrual.isSettled=true` + `settledDate=today`（`:80-86`）；(e) `postedAt`/`postedBy`（`:91-92`）。
  - **`posted` 布尔列不对称（watch-only residual）**：ORM `posted`（`:605` BOOLEAN default false）存在但 Processor **从不 setPosted(true)**——仅写 `postedAt`/`postedBy`。credit-memo 发票自身的 `posted=false`（经 pur/sal 管道后续翻转）。Bean 不入轴 `posted`；此不对称登记为四方对照 finding（watch-only residual）。
  - **错误码**：`ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`（`erp.err.ct.settlement-illegal-transition`，`ErpCtErrors:72-74`，参数 `ARG_SETTLEMENT_ID` + `ARG_CURRENT_STATUS`）。Processor `:46-48` 抛此码时仅传 settlementId + currentStatus（无 expectedStatus）。
  - **既有测试**：`TestErpCtRebateSettlementEnd`（287 行——`testSalesRebateSettlementGeneratesArCreditMemo` DRAFT→POSTED + AR credit-memo 生成；`testSettlementMarksAccrualsSettled` accrual 回写）+ `TestErpCtContractRebate`（527 行——`testRebateProgressiveAccrualAndSettlement` PURCHASE path DRAFT→POSTED + AP credit-memo；`testSettlementIllegalTransition` POSTED→POSTED re-post 失败）。
  - **无矩阵测试**（statemachine/ 包含 Contract/Version/RebateAgreement 3 Bean 测试，无 Settlement Bean 测试）。

- **既有 Bean 注册**：`_vfs/erp/ct/beans/app-service.beans.xml`（69 行）——10 个 Processor（L48-68）已注册，含 `ErpCtRebateSettlementPostSettlementProcessor`（L60-61）。3 个已落地 SM Bean（Contract/Version/RebateAgreement）已注册（L35-44）。**Settlement SM Bean 未注册**（greenfield）。
- **M3.19 接线模板（同域直接范本）**：`ErpCtRebateAgreementStateMachine`（退化分类 Bean——`transitions()` 空 + 只读守卫 `isActive(status)`）+ M3.18 `ErpCtContractVersionStateMachine`（2-edge linear Bean——`assertCanXxx` + `*TargetStatus()` + `transitions()`）。**差异**：Settlement 是非退化最小矩阵 Bean（1 实现边 + CANCELLED 死状态裁定），需 `assertCanPostSettlement` 前向守卫接入。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（M3 各 Bean 已复用，本计划延续）。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/contract/state-machine.md` §适用对象三 §4（lines 264-269）仅含**边界声明**——声明 settlement-status 独立轴 DRAFT→POSTED + `ErpCtRebateSettlementPostSettlementProcessor` 操作 Settlement 实体 + 生成 credit-memo。**owner doc 缺口**：无独立 §RebateSettlement 章节——Phase 2 四方对照须补建章节（对齐 M3.18/M3.19 补章节先例）。

## Goals

- 为 ErpCtRebateSettlement 的 status 轴落地一个实体级 `ErpCtRebateSettlementStateMachine` Bean，严格无状态，承载 postSettlement 迁移矩阵（DRAFT→POSTED 唯一实现边）+ CANCELLED 死状态裁定（intentional reserved）+ 终态/初始态分类 + 只读 `transitions()` 元数据。
- 将固定来源态/目标态判断改调 Bean：`ErpCtRebateSettlementPostSettlementProcessor` 注入 Bean + 前向守卫 `:45` 改调 `assertCanPostSettlement`（try/catch common 码 → cause-chain 领域码 `ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`），目标态 `:90` 改调 `postSettlementTargetStatus()`。**credit-memo 生成编排 + accrual 回写 + `postedAt`/`postedBy` 保留原位**。
- 层 2 四方对照裁定 CANCELLED 死状态（dict `erp-ct/settlement-status` ↔ owner doc ↔ Bean 元数据 ↔ writer），补建 owner doc §RebateSettlement 章节。
- 新增层 1 矩阵完备性测试；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码、credit-memo 类型/金额/`posted=false`、accrual 回写、幂等）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；`posted` 布尔列不对称登记为 watch-only residual，不在本计划修正。
- 不修改共享骨架 `Abstract*Processor`（module-common-service 零改动）。
- 不实现 reverse/unpost/cancel 命名动作（零 writer + dict 死状态，successor）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。
- 不清理 BizModel 死代码 `createNegativeApInvoice`/`createNegativeArInvoice`（`:82-146`，R6.7 split 遗留 duplicate，非状态机行为变更）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M3.18/M3.19 同域范本**；落地 1 Bean + PostSettlementProcessor 接线 + 测试 + 四方对照 + owner doc 补章节。**M4 plan-first**——postSettlement 触发 credit-memo 生成 + accrual 回写）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 单轴命名 + §9 生成路径）、`docs/design/contract/state-machine.md`（§适用对象三 §4 边界声明 + 待补 §RebateSettlement 章节）、`docs/design/contract/volume-discount.md`（§结算流程 :157-181）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-1430-3-erpct-version-rebate-state-machine-beans.md`（M3.18/M3.19 同域范本）
- Skill Selection Basis: 路线图 M4.65 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、credit-memo 副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 2 个集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M3.18/M3.19 范本可直接镜像 Bean 形状。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护 credit-memo 生成行为（postSettlement 生成 AP/AR 负额发票 + accrual 回写）。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpCtRebateSettlement status Bean + PostSettlementProcessor 接线（M4.65）

Status: planned
Targets: `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/statemachine/ErpCtRebateSettlementStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpCtRebateSettlementPostSettlementProcessor.java`、`.../test/.../statemachine/TestErpCtRebateSettlementStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M3.18/M3.19 同域 Bean 范本已 done

- [ ] `Decision`（CANCELLED 死状态裁定 + `posted` 不对称登记）：(A) **CANCELLED = intentional reserved 死状态**——dict 含 CANCELLED 但全域零 setStatus(CANCELLED) writer + 无 cancel mutation。Bean `transitions()` 不含 CANCELLED 边，`terminalStatuses()` 不含 CANCELLED（非真正终态，仅预留语义入口），dict 值保留不删（对齐 Contract CANCELLED/RebateAgreement EXPIRED+SETTLED 先例）。Successor：PM 要求 settlement cancel 工作流时开独立 plan。(B) **`posted` 布尔列不对称**——Processor 从不 setPosted(true)，仅写 postedAt/postedBy。credit-memo 发票自身 `posted=false`。此为 watch-only residual（owner doc 登记非修正），Bean 不入轴 posted。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpCtRebateSettlementStateMachine` Bean——1 实现边（postSettlement DRAFT→POSTED）+ `assertCanPostSettlement(String status)` + `postSettlementTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses`（初始={DRAFT}/终态={POSTED}）+ `transitions()`（1 边）。CANCELLED 不在任一集合（intentional reserved，见 Decision (A)）。严格无状态。非法边抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`currentStatus`/`expectedStatus` 参数。镜像 M3.18 `ErpCtContractVersionStateMachine` 结构。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/ct/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（紧邻已注册的 3 个 SM Bean）。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：`ErpCtRebateSettlementPostSettlementProcessor` 注入 `@Inject ErpCtRebateSettlementStateMachine stateMachine`（非 private）；`:45` 守卫改调 `stateMachine.assertCanPostSettlement(status)`（try/catch common 码 → cause-chain 领域码 `ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`，common NopException 作 cause；领域 re-throw 仅传 `ARG_SETTLEMENT_ID` + `ARG_CURRENT_STATUS`——`action`/`expectedStatus` 仅存于 common 码 cause，不向领域码传播）；`:90` 目标态改调 `stateMachine.postSettlementTargetStatus()`。credit-memo 生成 + accrual 回写 + `postedAt`/`postedBy` + `setCreditMemoBillType/BillCode` 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动，镜像 `TestErpCtContractVersionStateMachine` 范式）——(a) 无重复/冲突边（1 边唯一 action|fromStatus 键）；(b) postSettlement DRAFT→POSTED 可达；(c) `assertCanPostSettlement(DRAFT)` 通过、`assertCanPostSettlement(POSTED)`/`assertCanPostSettlement(CANCELLED)` 抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始={DRAFT}/终态={POSTED}；(f) CANCELLED 不在 initialStatuses/terminalStatuses/transitions 任一集合。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpCtRebateSettlementStateMachine` Bean 存在、已注册、严格无状态；PostSettlementProcessor 委托 Bean 前向守卫 + 目标态，内联 `Objects.equals` 守卫已移除。
- [ ] Settlement 层 1 矩阵测试本地 `mvn test -pl module-contract/erp-ct-service -am -Dtest=TestErpCtRebateSettlementStateMachineMatrix` 全绿。

### Phase 2 - 层 2 四方对照 + owner doc 补章节 + 层 3 既有回归

Status: planned
Targets: `module-contract/erp-ct-service/src/test/`（既有集成测试，零新建）、`docs/design/contract/state-machine.md`（补 §RebateSettlement 章节）
Skill: `nop-testing` + `state-machine-business-review-prompt.md`

- Item Types: `Proof | Add`
- Prereqs: Phase 1（Bean + Processor 接线已落地）

- [ ] `Add`（owner doc 补章节）：在 `docs/design/contract/state-machine.md` 新增 **§适用对象四：返利结算（RebateSettlement）**，集中建立 `ErpCtRebateSettlement.status` 轴（dict `erp-ct/settlement-status`）权威迁移语义——状态定义（3 值）+ 迁移完整性（DRAFT→POSTED 唯一实现边）+ CANCELLED intentional reserved 裁定 + `posted` 不对称 watch-only residual 登记 + postSettlement 副作用边界声明（credit-memo 生成 + accrual 回写）。对齐 M3.18/M3.19 补章节先例。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`：层 2 四方对照——dict `erp-ct/settlement-status`（3 值）↔ owner doc §RebateSettlement（Phase 2 新建）↔ Bean 元数据 ↔ 全部 writer（PostSettlementProcessor:90 唯一生产 writer + 创建写 DRAFT CRUD 路径 + CANCELLED 零 writer）。**CANCELLED 死状态 finding**：分类 = intentional reserved + successor。**`posted` 不对称 finding**：分类 = watch-only residual + owner doc 登记。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`：层 3 既有命名动作回归——复用 `TestErpCtRebateSettlementEnd`（2 case：SALES DRAFT→POSTED + AR credit-memo + accrual 回写）+ `TestErpCtContractRebate`（PURCHASE DRAFT→POSTED + AP credit-memo + illegal transition guard），证明 Processor 写回、credit-memo 类型/金额/`posted=false`、accrual 回写、领域错误码 + 参数不变。本地 `mvn test -pl module-contract/erp-ct-service -am` 全绿。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] owner doc §适用对象四：返利结算（RebateSettlement）章节已新增。
- [ ] 层 2 四方对照已完成（dict ↔ owner doc ↔ Bean ↔ writer，CANCELLED 死状态 + `posted` 不对称 findings 已登记）。
- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_002f77288ffexADeXFdscI2Im1`) — 零信任实仓核实 12 项核心 baseline 声明全 pass（entity/dict/唯一 writer/CANCELLED 零 writer/BizModel 仅 postSettlement/credit-memo posted=false/posted 不对称/测试/M3.18+M3.19 Bean/owner doc 缺口/M4.65 todo）。1 MAJOR 已修正：错误码参数 baseline 误写 `settlementCode + currentStatus + expectedStatus`，实为 `ARG_SETTLEMENT_ID + ARG_CURRENT_STATUS`（无 settlementCode/expectedStatus）——baseline 订正 + Phase 1 接线 cause-chain 项澄清领域 re-throw 仅传 settlementId + currentStatus（action/expectedStatus 仅存 common 码 cause）。4 MINOR 已修正：(1) Bean 注册行号 L39-61→L48-68；(2) Phase 2 Exit Criteria 补 owner doc 补章节 + 四方对照 checkbox；(3) "M3/M4 各 Bean"→"M3 各 Bean"（无前序 M4 Bean）；(4) owner doc §4 行号 264-274→264-269。
- Independent draft review iteration 2: `acceptable-as-draft` (`ses_002f34605ffedunybuJO0fceTq`) — focused re-review。iteration 1 MAJOR + 4 MINOR 全部修正并经实仓验证 pass（错误码参数/cause-chain 澄清/Phase 2 checkbox/M3 措辞/owner doc 行号/Draft Review Record）。1 residual MINOR 已修正：3 SM Bean 注册行号 L39-47→L35-44（Contract SM Bean 在 L35-36）。无 BLOCKER / MAJOR。草案审查收敛。**Review Hold 维持**：§11.2 M4 (i) plan-first 人工/owner-doc 门控触及 postSettlement→credit-memo 生成 + accrual 回写会计/财务保护区，属 project-context.md §AI 阻塞条件硬停止——非审查者可自主解除（batch-consistent with 0930-1/0810-x/1146-x/1931-x/0456-x）。计划保持 `Plan Status: draft`。
- Independent draft review iteration 3: `acceptable-as-draft (hold maintained)` (MISSION_DRIVER:2026-08-13-193118-mission-driver) — 格式/完备性/范围/闭包证据全 pass（零 BLOCKER/MAJOR）：全部必需章节齐全，两阶段 Item Types/Skill/Prereqs 合规，Exit Criteria 可测，Deferred 项分类 + successor 触发条件齐备。零信任实仓复核 12 项 baseline 声明全 pass（Processor :45 守卫 / :90 目标态 / :46-48 错误码参数 ARG_SETTLEMENT_ID+ARG_CURRENT_STATUS / credit-memo posted=false :120,:153 / 无 setPosted(true) / accrual 回写 :83-84 / 无 cancel mutation / owner doc §4 :264-269 边界声明 + §5 :274 缺口确认）。**Review Hold 确认为真实人工门控 blocker，非 AI 审查者可自主解除**：§11.2 M4 (i) 明文「触及受保护行为时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」+ project-context.md §AI 阻塞条件「任何更改触及会计/财务保护区域」硬停止——本计划编辑编排 credit-memo 生成 + accrual 回写的 PostSettlementProcessor，触及会计/财务保护区。计划保持 `Plan Status: draft`，待人工确认 §11.2 M4 (i) 门控后解除。
- Independent draft review iteration 4: `acceptable-as-draft (hold maintained)` (MISSION_DRIVER:2026-08-13-193118-mission-driver) — checklist-focused re-confirmation pass。零信任实仓复核 baseline 全 pass（Processor :45 DRAFT 守卫 / :46-48 ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION 仅 ARG_SETTLEMENT_ID+ARG_CURRENT_STATUS / :90 setStatus(POSTED) 唯一生产 writer / credit-memo posted=false AP:120+AR:153 / 无 setPosted(true) / accrual isSettled+settledDate 回写 :82-86 / 零 setStatus(CANCELLED) / app-service.beans.xml 3 SM Bean L35-44 + Settlement SM Bean 缺席 greenfield + Processor L60-61）。**Checklist 全 pass（零 BLOCKER/MAJOR）**：(1) Format compliance—全部必需章节 + 字段名 + 两阶段 Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）合规；(2) Completeness—Phase 1/2 Exit Criteria 均可测，Execution Plan 覆盖全部 9 项 Closure Gates；(3) Scope—单实体单轴边界清晰，Non-Goals 9 项排除充分，无 "and also" 蔓延，规则 14 bundling 声明合规（单轴无需 bundling）；(4) Closure evidence—9 项 Closure Gates 含验证命令（mvn build + 单域 test + compliance-checker）+ 独立结束审计门控，Deferred But Adjudicated 5 项均带 Classification/Successor Required/触发条件，反松弛规则无违例。**Review Hold 维持**：§11.2 M4 (i) plan-first 人工门控触及会计/财务保护区（credit-memo 生成 + accrual 回写），project-context.md §AI 阻塞条件硬停止，非 AI 审查者可自主解除（missing upstream human decision——escape-hatch 条件成立）。计划保持 `Plan Status: draft`，emitted `approved` marker 报告「review ran」。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移 rebate settlement 轴、postSettlement credit-memo AP/AR 负额发票生成 + ErpCtRebateAccrual.isSettled 回写完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。M4 plan-first 门控为阻塞前置。

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（status Bean + PostSettlementProcessor 接线 + 层 1 矩阵 + 层 2 四方对照 + owner doc 补章节 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.65 → done；CANCELLED 死状态 + `posted` 不对称登记）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-contract/erp-ct-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### CANCELLED 死状态（intentional reserved）

- Classification: `intentional reserved (dead state)`
- Why Not Blocking Closure: dict `erp-ct/settlement-status` 含 CANCELLED 但全域零 setStatus(CANCELLED) writer + 无 cancel mutation。Bean `transitions()`/`terminalStatuses()` 不含 CANCELLED。dict 值保留（对齐 Contract CANCELLED/RebateAgreement EXPIRED+SETTLED 先例：保留优于删除）。
- Successor Required: yes（触发条件 = PM 要求 settlement cancel 工作流时，开独立 plan 实现 cancel mutation + dict 值激活）

### `posted` 布尔列不对称（watch-only residual）

- Classification: `watch-only residual`
- Why Not Blocking Closure: Processor 从不 setPosted(true)，仅写 postedAt/postedBy。credit-memo 发票自身 `posted=false`。此为既有架构（credit-memo 经 pur/sal 管道后续翻转 posted），非状态机轴范畴。Bean 不入轴 posted。owner doc §RebateSettlement 登记。
- Successor Required: no（归过账侧/架构豁免，仅当 PM 要求 settlement 独立 posted 翻转语义时重评）

### BizModel 死代码清理（R6.7 split 遗留 duplicate）

- Classification: `optimization candidate`
- Why Not Blocking Closure: `ErpCtRebateSettlementBizModel:82-217` 的 `createNegativeApInvoice`/`createNegativeArInvoice`/`resolve*`/`requireSettlement`/`findUnsettledAccruals` 在 Processor 亦有同名实现（R6.7 split 遗留 duplicate）。删除属独立低风险清理，非状态机行为变更。
- Successor Required: yes（触发条件 = contract 域低风险清理批次启动时，核实 BizModel 零引用后删除）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待执行后填写_
- Evidence: _待执行后填写_

Follow-up:

- <待执行后填写；Deferred 项均为既定 successor>
