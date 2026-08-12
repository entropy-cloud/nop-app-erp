# 2026-08-13-0945-3-ercm-lead-state-machine-bean 线索/商机 ErpCrmLead.docStatus 生命周期 StateMachine Bean 迁移（M3.1）

> Plan Status: active
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.1（todo）
> Related: 前置 `2026-08-12-2142-2-erpcrm-event-state-machine-bean.md`（M2.2 done，crm 域 StateMachine Bean 范式首立 `ErpCrmEventStateMachine`；其 Deferred 将 crm 域其余状态轴（M3.1）指派为 successor，触发条件已满足）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M3.1
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（CRM-1/2 行，M3.1 详情行）+ 实仓核实。ErpCrmLead.docStatus 是线索/商机的**业务生命周期轴**（`crm/state-machine.md` §适用对象一 Lead，10 维度全覆盖），5 态有向无环生命周期。

- **轴语义（5 态生命周期）**：`NEW`（初始态，创建时写入）→ `QUALIFIED`（已验证进漏斗）；`NEW`/`QUALIFIED` → `CONVERTED`（终态，转报价单/客户）/ `LOST`（终态，丢单）/ `CANCELLED`（终态，无效/重复）。dict `erp-crm/lead-doc-status`（域专属字典）。CRM-1/2 八属性登记为「纳入 / **无**财务影响 / 复杂业务生命周期」。属模板 §11「M3 复杂业务生命周期」类别。终态 = CONVERTED/LOST/CANCELLED，无出边、不可恢复；QUALIFIED 不可回 NEW。
- **owner doc 矩阵（§2 迁移完整性，声明态）**：6 条声明迁移边——NEW→QUALIFIED（qualify）、NEW→LOST（lose）、NEW→CANCELLED（cancel）、QUALIFIED→CONVERTED（convert）、QUALIFIED→LOST（lose）、QUALIFIED→CANCELLED（cancel）。**但下方 convert 漂移表明 §2 的 QUALIFIED→CONVERTED 与代码实况不符，layer-2 须裁定权威矩阵。**
- **固定迁移判断当前所在位置（实仓核实）**：Lead 命名动作的来源态/目标态守卫当前分散在 per-mutation Processor + facade helper（`ErpCrmLead{Qualify,Lose,Cancel}Processor` + `ErpCrmLeadProcessor` facade 的 `validateTransitionFor{Qualify,Lose,Cancel}`（L51-72）+ Conversion 路径 `ErpCrmConversion*Processor` + `ErpCrmConversionProcessor` facade 的 `validateNotConverted`/`validateLeadType`）。这是本计划要替换为 Bean 调用的固定判断。crm 域无共享骨架（与采购/销售审批轴不同），Lead 守卫为域内 per-mutation Processor + facade。
- **逐动作 writer 盘点（实仓核实，命名 Processor 路径）**：
  - **qualify（NEW→QUALIFIED）**：`ErpCrmLeadQualifyProcessor`（per-mutation Processor 文件存在）。来源态守卫 NEW（facade `validateTransitionForQualify`）+ `leadType=LEAD` + 联系人信息必填（**动态业务守卫，保留原位**）。
  - **lose（NEW/QUALIFIED→LOST）**：`ErpCrmLeadLoseProcessor`。来源态守卫 NEW/QUALIFIED + `lostReasonId` 必填（**动态守卫，保留原位**）。
  - **cancel（NEW/QUALIFIED→CANCELLED）**：`ErpCrmLeadCancelProcessor`。来源态守卫 NEW/QUALIFIED。
  - **convert（⚠️ 漂移发现项，须 Decision）**：两 Conversion 路径——`ErpCrmConversionConvertToCustomerProcessor`（LEAD 类型）+ `ErpCrmConversionConvertToQuotationProcessor`（OPPORTUNITY 类型）。实仓核实：两者仅调 `facade.validateNotConverted`（L116-121，仅拒绝 CONVERTED 幂等）+ `facade.validateLeadType`（L123）+（quotation 路径）`requireOpportunityPartner`；**零 docStatus=QUALIFIED 来源态守卫**（grep 全 `ErpCrmConversion*.java` + `ErpCrmLeadBizModel.java` 对 QUALIFIED = 0 匹配）。即代码允许**任何非 CONVERTED 态**（含 NEW/QUALIFIED/LOST）经 leadType 门控→CONVERTED；既有测试 `TestErpCrmLeadConversion.testAlreadyConvertedRejected` 以 **NEW** lead 调 `convertToCustomer` 成功（NEW→CONVERTED），证明 NEW→CONVERTED 在代码中合法。owner doc §2 声明「QUALIFIED→CONVERTED」与代码「非 CONVERTED→CONVERTED（leadType 门控）」**漂移**，按路线图规则 5 须裁定。跨域副作用（`IErpSalQuotationBiz` 建报价单 / master-data `ErpMdPartner` 建客户 + 写 `relatedBillType/Code`）是**动态跨域副作用，保留原位**。
  - **stageId 前移（独立维度，非本轴）**：`ErpCrmLeadMoveStageProcessor` 管 stageId 维度，方向守卫 `validateStageDirection`（`ERR_STAGE_BACKWARD_MOVE`，config-gated `erp-crm.allow-stage-backward`）是**独立维度动态守卫，保留原位，不纳入 docStatus Bean 矩阵**。
- **关键澄清（stageId 非本轴）**：`crm/state-machine.md` §2 stageId 迁移规则声明 stageId 是**独立于 docStatus 的维度**。本 Bean 只迁移 docStatus 6 条边；stageId 维度的 sequence 方向守卫、convLog 审计留痕均保留原 Processor，不迁移、不改 `erp-crm.allow-stage-backward` 配置语义。
- **common 层非法迁移码已存在（参数形状已裁定）**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），cs 试点 M1.1 Decision Option A + 后续 M2/M3 计划裁定复用 + `action` 补充参数。本计划沿用，不新增 common 码。
- **领域错误码（既有）**：crm 域 Lead 命名动作的非法迁移错误码（如 `ERR_LEAD_ILLEGAL_*` 或既有码，layer-2 核实）保留；Bean 抛 common 码作 cause，Processor 映射领域码 + `leadCode`/上下文。layer-2 须核实 Lead 各动作是否已有专属非法迁移码，若无则按 sub-Decision 新增（对齐每实体/动作专属码模式）或复用既有（显式记录）。
- **Bean 命名**：Lead 仅有 docStatus 一轴（roadmap 无 Lead approveStatus），无双轴命名冲突 → Bean 命名 `ErpCrmLeadStateMachine`（与 M2.2 `ErpCrmEventStateMachine` 单轴命名范式一致，不带 `Document`/`Approval` 后缀）。
- **Bean 注册范式已存在**：`module-crm/erp-crm-service/src/main/resources/_vfs/erp/crm/beans/app-service.beans.xml` 已注册 `ErpCrmEventStateMachine`（M2.2 落地）；Lead Bean 沿用 `<bean id="<FQN>" class="<FQN>"/>` 范式。
- **层 3 回归基线已存在（非 greenfield）**：crm 域无既有 `TestErpCrmLeadStateMachine` 矩阵测试，层 1 为 greenfield；但存在覆盖 Lead 全生命周期的集成测试 = 层 3 基线（Lead qualify/lose/cancel/convert 经 BizModel/IGraphQLEngine 入口断言终态、跨域报价单/客户创建、stageId 前移、错误码）。
- **crm M2.2 计划遗留 follow-up（非本计划范围）**：M2.2 计划登记 BizModel 死代码 `ErpCrmEventBizModel:147-164` `requireEvent`/`validatePlanned`/`deriveLeadFields` 清理为 Follow-up（带触发条件）。本计划不处理 Event 死代码（独立 successor），仅在层 2 若发现 Lead 类似死代码则登记。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。本计划保持 R5=0、R11 不增。

## Goals

- 落地 `ErpCrmLeadStateMachine` Bean（一实体一轴 docStatus），承载迁移矩阵（NEW→QUALIFIED/LOST/CANCELLED、QUALIFIED→LOST/CANCELLED + convert 边**据 Phase 1 漂移裁定**）+ 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。Bean 的 convert 处理**不得新增 QUALIFIED 来源态限制**（否则拒绝现行 NEW→CONVERTED，违反「保持既有外部行为不变」）。
- 将 Lead 命名动作的**固定来源态/目标态判断**改调 Bean（`assertCanQualify/Lose/Cancel/Convert(String docStatus)` + 对应 `*TargetStatus()`）：qualify/lose/cancel 经 per-mutation Processor 接线；convert 经 Conversion 路径接线，其 `assertCanConvert` 仅拒绝 CONVERTED（匹配现行 `validateNotConverted`），**leadType/partner 门控作为动态守卫保留原位**。**动态业务守卫与副作用保留原位**（leadType 校验、联系人/lostReasonId 必填、stageId 方向守卫、跨域报价单/客户创建、relatedBill 写入）。
- 裁定 **convert 来源态漂移**（owner doc §2 QUALIFIED-only vs 代码 非CONVERTED）：Phase 1 Decision 据实编码 + owner-doc §2 doc-drift Fix（见下）。
- 层 2 四方对照（dict `erp-crm/lead-doc-status` ↔ `crm/state-machine.md` §Lead（§2 矩阵 + §5 可达性）↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）裁定，发现项按规则 5 登记。
- 新增层 1 矩阵完备性表驱动测试（greenfield）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数/审计/跨域报价单/客户创建副作用/stageId 守卫语义）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 stageId 维度（独立于 docStatus；其 sequence 方向守卫/convLog/`erp-crm.allow-stage-backward` 配置保留原 Processor）。
- 不迁移 Event.status 轴（M2.2 已 done）。
- 不触碰 `posted`（crm 域 Lead 无 posted 字段；Lead 无财务过账）。
- 不修改跨域 `IErpCrmConversionBiz` / `IErpSalQuotationBiz` / master-data Partner 创建契约（动态跨域副作用保留原位）。
- 不清理 crm M2.2 登记的 Event BizModel 死代码（独立 Follow-up successor）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；cs 试点 M1.2 已实证机制；Delta 覆盖回归归 M5.3）。
- 不改变 qualify/lose/cancel/convert 的错误码值/参数形状/审计 actionType。
- 不构建反射型/泛型全局 `IStateMachine` 调度器。
- 不新增 Lead 状态值或新命名动作（属 dict/业务行为变更，归 successor ask-first）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + crm M2.2 范式，落地 1 个单实体单轴生命周期 Bean + 接线 + 测试 + 四方对照；不改契约/模型/公共 API/跨域契约）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板）、`docs/design/crm/state-machine.md`（§适用对象一 Lead，§1 状态定义 + §2 迁移完整性 + §3 终态 + §5 可达性）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（CRM-1/2 八属性 + M3.1 详情行）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-12-2142-2-erpcrm-event-state-machine-bean.md`（crm Bean 范式同源）
- Skill Selection Basis: 路线图 M3.1 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「per-mutation Processor/facade 接线、Bean 注册、`@Inject` 非 private、跨实体/跨域调用边界、错误码、事务边界、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。其必需输入（owner doc + M0.1 契约 + M1.3 模板 + crm M2.2 范式 + 既有测试）均已就绪。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 crm-service 测试容器）。
- 前置依赖：M1.3 done（已满足）；M3.1 deps = M1.3，门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（Lead 无财务过账；convert 跨域副作用保留原位不改）。

## Execution Plan

### Phase 1 - ErpCrmLeadStateMachine Bean + 接线 + 跨实体 Decision 固化

Status: planned
Targets: `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/statemachine/ErpCrmLeadStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpCrmLead{Qualify,Lose,Cancel}Processor.java`、Conversion 接线路径（`IErpCrmConversionBiz` 实现或其委托）、`module-crm/erp-crm-service/src/test/.../TestErpCrmLeadStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）

- [ ] `Decision`（convert 来源态漂移裁定，路线图规则 5）：owner doc §2 声明 QUALIFIED→CONVERTED，代码实况为「任何非 CONVERTED 态经 leadType 门控→CONVERTED」（含 NEW→CONVERTED，测试证之）。裁定分支：(a) **Bean 据实编码代码行为**——`assertCanConvert` 仅拒绝 CONVERTED（匹配 `validateNotConverted`），leadType/partner 门控保留 Conversion Processor 动态守卫；owner doc §2 **doc-drift Fix**：就地补正为「convert 从非 CONVERTED 态经 leadType 门控→CONVERTED（NEW→CONVERTED 合法：convertToCustomer 从 LEAD 类型 NEW 直接转化）」。此分支保持既有外部行为（推荐）。(b) 若 PM 要求 owner-doc 权威 QUALIFIED-only → 属行为变化 Fix（拒绝现行 NEW→CONVERTED），移交 successor（触及业务行为 ask-first）。默认采 (a)。Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpCrmLeadStateMachine` Bean——显式 `assertCanQualify/Lose/Cancel/Convert(String docStatus)`（非法来源态 → 抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 补充参数；`assertCanConvert` 据 Decision 分支 (a) 仅拒绝 CONVERTED）+ `qualifyTargetStatus/loseTargetStatus/cancelTargetStatus/convertTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`（据 Decision：NEW→QUALIFIED/LOST/CANCELLED、QUALIFIED→LOST/CANCELLED、{NEW,QUALIFIED}→CONVERTED）。严格无状态（§2）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在非生成 `_vfs/erp/crm/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（§5，沿用 M2.2 `ErpCrmEventStateMachine` 范式）。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`（接线 Decision）：(A) Bean 接线点 = qualify/lose/cancel per-mutation Processor + convert 两 Conversion Processor 各自注入 `@Inject ErpCrmLeadStateMachine`（非 private），固定来源态守卫处调 `assertCanXxx`，目标态写入调 `*TargetStatus()`；(B) common 错误码沿用 Option A；(C) **领域码按动作分轨**——qualify/lose/cancel → 既有 `ERR_LEAD_ILLEGAL_STATUS_TRANSITION`（`ErpCrmErrors.java:70`，layer-1 复核）；**convert 幂等拒绝（CONVERTED→CONVERTED）→ 保持既有 `ERR_LEAD_ALREADY_CONVERTED`**（`ErpCrmErrors.java:95`，distinct 幂等码，**不复用** `ERR_LEAD_ILLEGAL_STATUS_TRANSITION`；Bean `assertCanConvert` 抛 common 码作 cause，Conversion Processor 捕获后映射 `ERR_LEAD_ALREADY_CONVERTED`）；(D) 初始态 NEW 写入不经 Bean（§9.2 选项 c）；(E) 动态守卫/副作用保留原位（leadType 校验、联系人/lostReasonId 必填、stageId 方向守卫、convert 跨域报价单/客户创建 + relatedBill 写入、convert 的 `validateLeadType`/`requireOpportunityPartner`）。grep 证 Processor 内不再有内联 docStatus 来源态矩阵判断（动态守卫除外）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动）——(a) 无重复/冲突边；(b) NEW→QUALIFIED 可达、NEW/QUALIFIED→LOST/CANCELLED 可达、{NEW,QUALIFIED}→CONVERTED 可达；(c) QUALIFIED 不可回 NEW（assertCanQualify 对 QUALIFIED 抛 common 码）；(d) `assertCanConvert` 据 Decision 分支 (a) **仅对 CONVERTED 抛 common 码、对一切非 CONVERTED 态（NEW/QUALIFIED/LOST/CANCELLED）运行时通过**——严格匹配代码现行 `validateNotConverted` 行为（不新增任何来源态限制，杜绝 NEW→CONVERTED 回归）；(e) 终态 CONVERTED/LOST/CANCELLED 的 qualify/lose/cancel `assertCanXxx` 抛 common 码携带 `action`/`fromStatus`；(f) `transitions()` 编码**意图矩阵** {NEW,QUALIFIED}→CONVERTED（声明态，与运行时 `assertCanConvert` 仅拒 CONVERTED 的范围有意不同——见下方 layer-2 已知漂移）；(g) 初始集 {NEW}、终态集 {CONVERTED,LOST,CANCELLED} 正确。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpCrmLeadStateMachine` Bean 存在、已注册、严格无状态；qualify/lose/cancel/convert 接线委托 Bean，grep 证内联 docStatus 来源态矩阵判断已移除（动态守卫除外）。
- [ ] Lead 层 1 矩阵测试本地 `mvn test -pl module-crm/erp-crm-service -am -Dtest=TestErpCrmLeadStateMachineMatrix` 全绿。

### Phase 2 - 层 2 四方对照 + 漂移裁定

Status: planned
Targets: `docs/design/crm/state-machine.md`（若漂移补注）、本计划 Closure 段
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Decision | Fix | Proof`
- Prereqs: Phase 1（Bean + 接线已落地）

- [ ] `Proof | Decision`：层 2 四方对照——dict `erp-crm/lead-doc-status`（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED）↔ `crm/state-machine.md` §Lead（§2 矩阵 + §5 可达性）↔ Bean 元数据 ↔ 全部 writer（qualify/lose/cancel/convert + 创建路径写 NEW + 通用 CRUD 路径 §9.4）。逐条分类（一致 / implementation drift / doc drift / intentional legacy），任何不一致按规则 5 Fix/Decision + successor（禁止静默排除）。**已知漂移 1**：convert 来源态（owner doc §2 QUALIFIED-only vs 代码 非CONVERTED，Phase 1 Decision 分支 a 裁定 + Phase 2 Fix 闭环）。**已知漂移 2（运行时 vs 意图矩阵 latent gap）**：`assertCanConvert` 运行时仅拒 CONVERTED，故 LOST/CANCELLED→CONVERTED 在代码中技术上允许（`validateNotConverted` 不拦），但意图矩阵（`transitions()` + owner doc §2）限定 {NEW,QUALIFIED}→CONVERTED。此为**既有 latent gap**（迁移前已存在，非本计划引入），layer-2 须裁定：(i) 保持运行时宽放（Bean 不加来源态限制，匹配现行行为）+ owner doc §2 补注 latent gap 为 watch-only residual + successor；或 (ii) 视为 implementation drift → Fix 收窄（但属行为变化，须 successor/ask-first）。默认 (i) 保持行为不变。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Fix`（convert doc-drift，落地 Phase 1 Decision 分支 a）：就地补正 `crm/state-machine.md` §2 迁移图 + 迁移表：convert 从「QUALIFIED→CONVERTED」补正为「{NEW,QUALIFIED}→CONVERTED 经 leadType 门控」（convertToCustomer 从 LEAD 类型 NEW 直接转化；convertToQuotation 从 OPPORTUNITY 类型经 createOpportunityFromLead 新建 OPPORTUNITY(NEW) 再转化）。补注 doc-drift 裁定结论 + 引用本计划。其他漂移（若有）按裁定落地。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Lead 四方对照记录可追溯；任何漂移已按 Fix/Decision 登记 + successor，无静默排除。

### Phase 3 - 层 3 既有命名动作回归 + 一致性复核

Status: planned
Targets: `module-crm/erp-crm-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–2（Bean + 接线 + 四方对照已落地）

- [ ] `Proof`：层 3 既有命名动作回归（非 greenfield）——复用既有 Lead 集成测试基线，证明 Processor 写回、终态不可恢复（CONVERTED/LOST/CANCELLED 无出边）、QUALIFIED 不可回 NEW、领域错误码 + 参数、convert 跨域报价单/客户创建副作用、stageId 方向守卫语义不变。本地 `mvn test -pl module-crm/erp-crm-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：一致性复核——Bean 命名/注册/无状态/元数据形状与 crm M2.2 `ErpCrmEventStateMachine` 范式一致；四方对照记录写入本计划 Closure 段。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `mvn test -pl module-crm/erp-crm-service -am` 全绿（层 3 回归无行为回归）。
- [ ] 四方对照记录可追溯、漂移处置闭环。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_0088c6280ffe3lnW0qxat3KYGV，新会话）——实仓核实发现 1 BLOCKER：Current Baseline 虚假声称 convert 路径有「来源态守卫 QUALIFIED」（标注「实仓核实」），实际 `ErpCrmConversionConvertToCustomerProcessor`/`...ConvertToQuotationProcessor` 仅调 `validateNotConverted`+`validateLeadType`(+partner)，零 QUALIFIED 守卫；`TestErpCrmLeadConversion` 以 NEW lead convertToCustomer 成功证明 NEW→CONVERTED 合法。owner doc §2「QUALIFIED→CONVERTED」与代码漂移。若按原计划 Bean 编码 QUALIFIED-only 并接线 assertCanConvert(QUALIFIED) 会新增限制、违反「保持既有外部行为不变」。另 1 MINOR：`ERR_LEAD_ILLEGAL_STATUS_TRANSITION` 已存在（ErpCrmErrors.java:70）。已采纳全部修订：baseline convert 改为据实（仅 validateNotConverted+leadType 门控，零 QUALIFIED 守卫，NEW→CONVERTED 合法）；Phase 1 新增 convert 来源态漂移 Decision（分支 a Bean 据实仅拒 CONVERTED + owner-doc §2 doc-drift Fix；分支 b PM 要求 QUALIFIED-only 则行为变化 successor）；矩阵/层1断言改为 {NEW,QUALIFIED}→CONVERTED；领域码据实为既有 `ERR_LEAD_ILLEGAL_STATUS_TRANSITION`；区分两 convert 动作（LEAD/OPPORTUNITY）。
- Independent draft review iteration 2: `acceptable as-is`（独立子代理 ses_00883967cffexfQ2nQ6Yb33IS8，新会话）——实仓复核确认 BLOCKER 已解决（convert 路径零 QUALIFIED 守卫、NEW→CONVERTED 合法、`ERR_LEAD_ILLEGAL_STATUS_TRANSITION`@ErpCrmErrors:70 + convert 幂等码 `ERR_LEAD_ALREADY_CONVERTED`@:95 均存在）。Rule 5/13/anti-slack/baseline-honesty/internal-consistency 全 PASS。3 MINOR 已采纳：(1) Decision C 按 convert 幂等拒绝 carve-out `ERR_LEAD_ALREADY_CONVERTED`（不复用通用迁移码）；(2) Proof (d) 明确 `assertCanConvert` 运行时仅拒 CONVERTED（LOST/CANCELLED 亦通过）+ `transitions()` 编码意图矩阵 {NEW,QUALIFIED} 的有意区分；(3) Phase 2 预声明 LOST/CANCELLED→CONVERTED latent gap（既有非本计划引入）+ 默认保持行为不变裁定。草案审查收敛，Plan Status → active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（Lead docStatus Bean + 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（漂移裁定按分支落地 owner-doc 补注或 Fix；架构 doc 不引用本路线图执行状态）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-crm/erp-crm-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline（R5=0 不漂移、R11=0 不增）
- [ ] 无范围内项目降级为 deferred/follow-up（漂移裁定必须落地登记 + successor，不得悬置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 为可选（模板 §11.2）；cs 试点 M1.2 已运行时实证业务级 Delta 同名覆盖机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first），独立 successor。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

### crm Event BizModel 死代码清理（M2.2 遗留 follow-up）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M2.2 计划已登记 `ErpCrmEventBizModel:147-164` `requireEvent`/`validatePlanned`/`deriveLeadFields` 死代码清理为带触发条件的 Follow-up。本计划（Lead 轴）不处理 Event 死代码。
- Successor Required: yes（触发条件 = crm 域低风险清理批次启动时）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话，非执行者上下文）>
- Evidence: <待填充>

Follow-up:

- <无非阻塞跟进；crm Event 死代码清理已在 Deferred But Adjudicated 登记并指派 successor 触发条件>
