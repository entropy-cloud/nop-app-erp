# 2026-08-14-1815-1-rc-mr1-r1-21-22-crm-conversion-family RC-R1.21 + RC-R1.22 — crm 线索转化族：直接升格 + 转化前置守卫（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-14
> Mission: requirement-compliance
> Work Item: RC-R1.21（P1-RC-032 crm 线索直接升格 convertToOpportunity mutation）+ RC-R1.22（P1-RC-033 convertToCustomer 前置守卫 + P1-RC-034 convertToQuotation 前置守卫，同域同控制点协同）— 同域同 owner doc（`docs/design/crm/` + `use-cases.md`）同结果表面（线索/商机转化契约符合性），按计划指南规则 14 合并为一个 owner plan 的两个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.21/RC-R1.22 行 + `docs/audits/arm-index.md` P1-RC-032/P1-RC-033/P1-RC-034 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（G4 crm 转化守卫族）
> Related: `docs/design/crm/use-cases.md`（L1 UC-CRM-02/03）；`docs/design/crm/state-machine.md`（§Lead）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.83/84/88 运行时证据）；`docs/plans/2026-08-08-2219-1-rc-mr1-r1-14-15-sal-pricing-family.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-032（arm-index 行，UC-CRM-02 直接升格分支缺失）**：L1（`use-cases.md:45-46`）逐字「if 不创建客户: lead.leadType → OPPORTUNITY（直接升格）」。L3 实仓：`IErpCrmConversionBiz.java:21-46` 仅 `convertToCustomer`/`convertToQuotation`/`getCreatedOpportunity` 三方法，**无 convertToOpportunity 入口**；`ErpCrmConversionProcessor.createOpportunityFromLead:92-108` 中 `setLeadType(OPPORTUNITY)`（`:96`）作用于**新建** ErpCrmLead（`leadDao().newEntity()`），非原 lead 原地升格；A4.2.88 运行时证实 grep `setLeadType` 主代码仅 1 命中（新建分支）+ `directPromote|promoteToOpportunity|直接升格` 零命中 + module-crm 无 _delta 目录零补偿实现 → **直接升格分支运行时不存在**。§2 P1①（功能完全缺失）。**非 P0**（CRM 域不产生会计凭证；缺失仅影响「不建客户直接升格」操作路径，convertToCustomer 主路径可用）。
- **finding P1-RC-033（arm-index 行，UC-CRM-02 convertToCustomer 前置条件弱）**：L1（`use-cases.md:39`）逐字「Lead.docStatus == QUALIFIED 且 leadType == LEAD」。L3 实仓：`ErpCrmConversionConvertToCustomerProcessor.convertToCustomer:19-28` 仅 `validateNotConverted:21`（状态机 `assertCanConvert` 仅拒 CONVERTED）+ `validateLeadType(LEAD):22`，**不查 docStatus==QUALIFIED**；`ErpCrmLeadStateMachine.assertCanConvert:81-89` javadoc 自承「运行时仅拒绝 CONVERTED，NEW/LOST/CANCELLED→CONVERTED 技术上允许」（既有 latent gap，类注释 :32-37）。A4.2.83 运行时证实 NEW 状态 LEAD 经 GraphQL convertToCustomer 成功转化。§2 P1②（前置条件未守卫）。**非 P0**。
- **finding P1-RC-034（arm-index 行，UC-CRM-03 convertToQuotation 前置条件弱）**：L1（`use-cases.md:60-61`）逐字「Lead.leadType == OPPORTUNITY 且 lead.docStatus == QUALIFIED 且 stage.isWonStage == true」。L3 实仓：`ErpCrmConversionConvertToQuotationProcessor.convertToQuotation:21-30` 仅 `validateNotConverted:23` + `validateLeadType(OPPORTUNITY):24` + `requireOpportunityPartner:25`，**不查 docStatus 且不查 stage.isWonStage**；`isWonStage` 在 crm-service 唯一消费点 = `ErpCrmLeadBizModel.findOpportunityBoardData:298-300`（🏆 emoji 展示）→ won-stage 前置静默丢弃。A4.2.84 运行时证实非 QUALIFIED/won-stage OPPORTUNITY 运行时成功转报价单。§2 P1②。**非 P0**。
- **实仓（HEAD 核查）**：
  - `IErpCrmConversionBiz.java:28-46`（接口，`ErpCrmLeadBiz` 继承之）+ `ErpCrmLeadBizModel.convertToCustomer:176-178`/`convertToQuotation:182-187`（@BizMutation Facade 委托 Processor）。
  - `ErpCrmConversionProcessor.java`（共享 protected helper 单一真相源）：`createPartnerFromLead:62-73`（经 `IErpMdPartnerBiz.save` 建客户）+ `createOpportunityFromLead:88-104`（新建 OPPORTUNITY lead，`:93 newEntity`/`:96 setLeadType`/`:98 setDocStatus(NEW)`）+ `markLeadConverted:110-116`（weak-pointer 回写 + `stateMachine.convertTargetStatus()`）+ `validateNotConverted:120-129`/`validateLeadType:131-137`/`requireOpportunityPartner:139-144`/`requireLead:148-155`（行号经 HEAD 复核，2026-08-13 commit 37e858d22 后基线）。
  - **P1-RC-032 修复点**：`ErpCrmConversionProcessor` 增 protected step（如 `promoteToOpportunity(lead, context)`：校验 `docStatus==QUALIFIED` + `leadType==LEAD` → 原地 `setLeadType(OPPORTUNITY)` + `updateEntity`，不建 Partner/新 Lead、不改 docStatus）+ `IErpCrmConversionBiz` 增 `convertToOpportunity(@Name("leadId") Long leadId, IServiceContext context)` 契约方法 + `ErpCrmLeadBizModel` 实现（@BizMutation + 委托新 per-mutation Processor，对齐 R6.6 每 mutation 一 Processor 范式——可复用 `ErpCrmConversionProcessor` facade 或新增 `ErpCrmConversionConvertToOpportunityProcessor`，**Decision 项**）。
  - **P1-RC-033 修复点**：`ErpCrmConversionConvertToCustomerProcessor.convertToCustomer` 增 protected step（如 `validateDocStatus(QUALIFIED)`，经 `ErpCrmLeadStateMachine` 或直接比对常量）——接入位置：`validateLeadType` 之后、`createPartnerFromLead` 之前。
  - **P1-RC-034 修复点**：`ErpCrmConversionConvertToQuotationProcessor.convertToQuotation` 增 `validateDocStatus(QUALIFIED)` + `validateWonStage`（经 `lead.getStageId()` → `IErpCrmStageBiz` 查 `isWonStage`，`ErpCrmStage` ORM `:317` isWonStage propId=8 现存；`ErpCrmLeadBizModel` 已注入 `IErpCrmStageBiz stageBiz:53-54` 可参照）——接入位置：`validateLeadType` 之后、`requireOpportunityPartner` 之前。
  - **错误码**：`ErpCrmErrors.java:67-99` 现存 `ERR_LEAD_ILLEGAL_STATUS_TRANSITION`（common 迁移码）、`ERR_LEAD_TYPE_MISMATCH:89`、`ERR_LEAD_ALREADY_CONVERTED:95`、`ERR_OPPORTUNITY_PARTNER_REQUIRED:92`——需新增 `ERR_LEAD_NOT_QUALIFIED`（docStatus 前置）+ `ERR_LEAD_STAGE_NOT_WON`（won-stage 前置），**Decision 项**（错误码命名与是否复用 ERR_LEAD_ILLEGAL_STATUS_TRANSITION）。
  - **测试基线（关键——既有测试与新守卫冲突，须纳入范围改造）**：`TestErpCrmLeadConversion.java`（402 行，9 @Test，HEAD 复核）——**seed 不统一**：`testAlreadyConvertedRejected:162-176` 种子 NEW LEAD 且断言**首次 convertToCustomer 成功**（`:171`）——P1-RC-033 守卫（NEW 拒绝）将使其失败（该测试正是 A4.2.83 缺陷的表现形态）；`testFullConversionChain:68-130` 在 convertToCustomer 后对**新建 OPPORTUNITY（docStatus=NEW + stageId=null**，`createOpportunityFromLead:98`）直接 convertToQuotation（`:118`）——P1-RC-034 守卫（QUALIFIED + won-stage）将使其失败；`testOpportunityWithoutPartnerRejected:178-191` 种子 QUALIFIED OPPORTUNITY **stageId=null** 断言 `ERR_OPPORTUNITY_PARTNER_REQUIRED`（`:189`）——守卫顺序（docStatus→won-stage→partner）下 `validateWonStage` 先抛 `ERR_LEAD_STAGE_NOT_WON` 致其失败。**既有 9 @Test 并非「seed 均 QUALIFIED 路径零回归」**——3 个测试须在 Phase 3 显式改造（qualify 前置/won-stage seed/断言顺序调整）+ 快照重录；其余 6 个测试（lose/illegalTransition/leadTypeMismatch/duplicate/cancel/zeroPollution 等）与守卫无冲突零回归。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor + ErrorCode + 接口契约方法，**不触 ORM 结构/会计过账/删除**；roadmap RC-R1.21/RC-R1.22 行 `todo`，Deps（R1.0 done）已满足；`IErpCrmConversionBiz` 接口增方法属 dao 模块接口契约扩展（非 ORM 结构变更，代码逻辑类预授权，A4.2.88 明示）。
- **涉及文件**：`module-crm/erp-crm-dao/.../biz/IErpCrmConversionBiz.java`；`module-crm/erp-crm-service/.../entity/ErpCrmLeadBizModel.java`；`.../processor/ErpCrmConversionProcessor.java`；`.../processor/ErpCrmConversionConvertToCustomerProcessor.java`；`.../processor/ErpCrmConversionConvertToQuotationProcessor.java`；`ErpCrmErrors.java`；测试 `TestErpCrmLeadConversion.java` 扩展或新增测试类。

## Goals

- **直接升格（P1-RC-032）**：`IErpCrmConversionBiz` 增 `convertToOpportunity` 契约 + `ErpCrmLeadBizModel` @BizMutation 实现（前置 `docStatus==QUALIFIED` + `leadType==LEAD` 校验 → 原地 `leadType=OPPORTUNITY`，不建 Partner/新 Lead、docStatus 保持 QUALIFIED）+ 正负向测试。
- **convertToCustomer 前置守卫（P1-RC-033）**：`validateBusinessRulesForApprove` 型 protected step `validateDocStatus(QUALIFIED)` 接入 convertToCustomer 链——非 QUALIFIED（NEW/LOST/CANCELLED/CONVERTED）抛 `ERR_LEAD_NOT_QUALIFIED` 拒绝。
- **convertToQuotation 前置守卫（P1-RC-034）**：`validateDocStatus(QUALIFIED)` + `validateWonStage`（stage.isWonStage==true，经 IErpCrmStageBiz）接入 convertToQuotation 链——非 QUALIFIED 或非 won-stage 抛对应错误码拒绝。
- **零回归**：既有 `TestErpCrmLeadConversion` 9 @Test 中 6 个无冲突测试零回归 + **3 个冲突测试（testAlreadyConvertedRejected/testFullConversionChain/testOpportunityWithoutPartnerRejected）按新守卫显式改造**（详见 Phase 3）+ 新增测试全绿 + erp-crm-service 全模块测试零回归。
- **owner doc 收敛注记**：`state-machine.md §Lead` 或 `README.md §业务规则1 转化流` 补直接升格 + 前置守卫实现注记；`use-cases.md` 需求契约段不动（真相源冻结条款）。
- **回填**：arm-index P1-RC-032/033/034 → `done (RC-R1.21/RC-R1.22)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引/零新实体——直接升格不需要 teamId/ownerId 变更载体，ErpCrmTeamMember 属 P1-RC-036 独立行）。
- **不实现 territory ROUND_ROBIN/LOAD_BALANCED 挑人（P1-RC-036）**（独立 finding，ORM ask-first，另行列管）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现 convertToCustomer 自动建客户的变体分支**（如客户已存在时复用——L1 未要求，超出最小修复面）。
- **不重审状态机 latent gap**（`assertCanConvert` 运行时仅拒 CONVERTED 的既有宽放保持——本行前置守卫在 Processor 层新增，不修改状态机 Bean 语义；owner doc 注记记录边界）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/crm/use-cases.md`（L1 UC-CRM-02/03）+ `docs/design/crm/state-machine.md`（§Lead）+ `docs/design/crm/README.md`（§业务规则1 转化流/§衔接契约）+ `docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.83/84/88 运行时证据）
- Skill Selection Basis: 实现面 = BizModel/Processor protected step + 接口契约扩展 + 错误码（`nop-backend-dev`：每 mutation 一 Processor 范式、protected step 模式、错误码规范、跨实体经 I*Biz）；测试（`nop-testing`：JunitAutoTestCase GraphQL 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key（守卫为硬编码前置校验，非 config-gated——L1 字面无配置开关）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-crm/erp-crm-service`。

## Execution Plan

### Phase 1 - 直接升格（P1-RC-032）

Status: completed
Targets: `IErpCrmConversionBiz.java`；`ErpCrmLeadBizModel.java`；`ErpCrmConversionProcessor.java`（或新增 per-mutation Processor）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **实现形态**：选项 A（推荐）= 新增独立 `ErpCrmConversionConvertToOpportunityProcessor`（对齐 R6.6 每 mutation 一 Processor 范式，镜像 ConvertToCustomer/ConvertToQuotation 两兄弟类）+ `ErpCrmConversionProcessor` 增 protected step `promoteToOpportunity`（共享 helper 单一真相源）；选项 B = 直接在 `ErpCrmLeadBizModel` 内联实现（破坏 delete-after-extract 范式，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [x] `Decision` **直接升格后 docStatus 语义**：选项 A（推荐）= docStatus 保持 QUALIFIED（L1 UC-CRM-02 直接升格分支未要求 CONVERTED；保持 QUALIFIED 使后续 convertToQuotation 前置（QUALIFIED + won-stage）成立）；选项 B = 置 CONVERTED（将阻断后续转报价单，违反 UC-CRM-03 链路，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [x] `Fix` `IErpCrmConversionBiz` 增 `convertToOpportunity(@Name("leadId") Long leadId, IServiceContext context)` 契约方法（javadoc 描述直接升格语义 + 前置条件）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmConversionProcessor` 增 protected step `promoteToOpportunity(lead, context)`：`validateLeadType(LEAD)` + `validateDocStatus(QUALIFIED)`（校验失败抛新增 `ERR_LEAD_NOT_QUALIFIED`）→ `setLeadType(LEAD_TYPE_OPPORTUNITY)` + `leadDao().updateEntity(lead)`（docStatus 不变）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 新增 `ErpCrmConversionConvertToOpportunityProcessor`（requireLead → promoteToOpportunity）+ `ErpCrmLeadBizModel.convertToOpportunity` @BizMutation 委托（beans.xml 注册新 Processor bean，对齐 `app-service.beans.xml` 既有 per-mutation Processor 注册段）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmErrors` 新增 `ERR_LEAD_NOT_QUALIFIED`（ARG_LEAD_CODE 参数，中文描述「仅 QUALIFIED 状态的线索可转化/升格」）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `convertToOpportunity` 经 GraphQL 可达：QUALIFIED+LEAD → leadType 变 OPPORTUNITY 且 docStatus 保持 QUALIFIED、不新建 Partner/Lead（Phase 3 断言证实）
- [x] 非 QUALIFIED/NEW 状态抛 `ERR_LEAD_NOT_QUALIFIED`；非 LEAD 类型抛既有 `ERR_LEAD_TYPE_MISMATCH`（Phase 3 断言证实）
- [x] 零 ORM 变更（`git diff --stat` 仅 erp-crm-service/erp-crm-dao Java + `_cases/` 快照）

### Phase 2 - 转化前置守卫族（P1-RC-033 + P1-RC-034）

Status: completed
Targets: `ErpCrmConversionConvertToCustomerProcessor.java`；`ErpCrmConversionConvertToQuotationProcessor.java`；`ErpCrmConversionProcessor.java`；`ErpCrmErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1 完成（共享 `validateDocStatus` protected step 先落地）

- [x] `Decision` **错误码复用 vs 新建**：选项 A（推荐）= 新建 `ERR_LEAD_NOT_QUALIFIED`（docStatus 前置专属码，Phase 1 已建）+ `ERR_LEAD_STAGE_NOT_WON`（won-stage 专属码，ARG_LEAD_CODE/ARG_STAGE 参数）；选项 B = 复用 `ERR_LEAD_ILLEGAL_STATUS_TRANSITION`（common 迁移码——语义含混：守卫是业务前置非状态机迁移，且 LOST/CANCELLED 也属「非法」但错误码名称误导，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmConversionProcessor` 增 protected step `validateDocStatus(lead, expectedStatus, context)`（复用 Phase 1）+ `validateWonStage(lead, context)`：经 `stageBiz`/stageDao 查 `lead.getStageId()` 对应 `ErpCrmStage.isWonStage`（stageId null 或 isWonStage!=true 抛 `ERR_LEAD_STAGE_NOT_WON`）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmConversionConvertToCustomerProcessor.convertToCustomer`：`validateLeadType(LEAD)` 后接入 `validateDocStatus(QUALIFIED)`（P1-RC-033）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmConversionConvertToQuotationProcessor.convertToQuotation`：`validateLeadType(OPPORTUNITY)` 后接入 `validateDocStatus(QUALIFIED)` + `validateWonStage`（P1-RC-034，先 docStatus 后 won-stage，守卫顺序确定化）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmErrors` 新增 `ERR_LEAD_STAGE_NOT_WON`（ARG_LEAD_CODE/ARG_STAGE_ID 参数，中文描述「仅赢单阶段（isWonStage）的商机可转报价单」）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] convertToCustomer：非 QUALIFIED 拒绝（`ERR_LEAD_NOT_QUALIFIED`，lead 状态不变）+ QUALIFIED 主路径零回归（Phase 3 断言证实）
- [x] convertToQuotation：非 QUALIFIED 拒绝 + 非 won-stage 拒绝（`ERR_LEAD_STAGE_NOT_WON`）+ QUALIFIED+won-stage 主路径零回归（Phase 3 断言证实）
- [x] 零 ORM 变更 + 守卫顺序固定（docStatus → won-stage → partner）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/`（扩展 `TestErpCrmLeadConversion.java` 或新增 `TestErpCrmConversionGuards.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add` P1-RC-032 矩阵：① QUALIFIED+LEAD 直接升格成功（leadType=OPPORTUNITY + docStatus=QUALIFIED + 不新建 Partner/Lead + 原 lead id 不变）；② NEW 状态拒绝（`ERR_LEAD_NOT_QUALIFIED`）；③ 已升格（leadType=OPPORTUNITY）后再次调用 → `ERR_LEAD_TYPE_MISMATCH`（leadType 校验先于 docStatus，非 CONVERTED 幂等路径——docStatus 仍为 QUALIFIED）。
      - Skill: `nop-testing`
- [x] `Add` **既有冲突测试改造（独立 Fix 项，范围义务）**：① `testAlreadyConvertedRejected:162-176` — seed 改为 QUALIFIED LEAD（或先 qualify），保留「首次成功 + 二次 ERR_LEAD_ALREADY_CONVERTED」断言；② `testFullConversionChain:68-130` — convertToCustomer 与 convertToQuotation 之间插入 qualify/QUALIFIED + moveStage 至 isWonStage=true 阶段（新增 won-stage seed，如 seedStage(STAGE_WON,...,isWonStage=true)），使新 OPPORTUNITY 达 QUALIFIED+won-stage 前置；③ `testOpportunityWithoutPartnerRejected:178-191` — seed 补 won-stage stageId，保留 `ERR_OPPORTUNITY_PARTNER_REQUIRED` 断言（partner 守卫仍可达）；④ 对应 `_cases/` 快照重录。
      - Skill: `nop-testing`
- [x] `Add` P1-RC-033 矩阵：① NEW 状态 convertToCustomer 拒绝（`ERR_LEAD_NOT_QUALIFIED`，无 Partner/新 Lead 创建）；② QUALIFIED 主路径成功（既有 testFullConversionChain 改造后零回归）。
      - Skill: `nop-testing`
- [x] `Add` P1-RC-034 矩阵：① 非 QUALIFIED OPPORTUNITY（NEW/LOST/CANCELLED）convertToQuotation 拒绝（`ERR_LEAD_NOT_QUALIFIED`）；② QUALIFIED 但 stage 非 won-stage 拒绝（`ERR_LEAD_STAGE_NOT_WON`）；③ QUALIFIED + won-stage 主路径成功（既有测试改造后零回归）。
      - Skill: `nop-testing`
- [x] `Proof` GraphQL 冒烟断言（`ErpCrmLead__convertToOpportunity` GraphQL 动作名执行时核实——接口方法经 BizModel 暴露为 mutation）+ `_cases/` 快照录制；既有 `TestErpCrmLeadConversion` 9 @Test 全部改造后全绿。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试矩阵全绿 + 既有 `TestErpCrmLeadConversion` 9 @Test 改造后全绿（3 个冲突测试适配新守卫 + 6 个无冲突测试零回归）：`mvn test -pl module-crm/erp-crm-service`（BUILD SUCCESS）
- [x] 三 finding 全部路径均有断言证据；快照（含改造测试）录制完成

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/crm/state-machine.md`（或 `README.md`）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：转化流补直接升格 + 双前置守卫实现注记（含 docStatus 保持 QUALIFIED 语义 + won-stage 判定载体 + 状态机 latent gap 边界声明）；不修改需求契约段。
      - Skill: none
- [x] `Add` arm-index P1-RC-032 → `done (RC-R1.21)` + P1-RC-033/P1-RC-034 → `done (RC-R1.22)` + 修复落地摘要；roadmap RC-R1.21/RC-R1.22 → done；`docs/logs/2026/08-14.md` 日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_000369413ffeR2AOea7ahrg7Xj）— 1 Blocker：既有测试与新守卫冲突——`testAlreadyConvertedRejected:162-176` 种子 NEW 且断言首次 convertToCustomer 成功（`:171`，恰是 A4.2.83 缺陷形态）/ `testFullConversionChain:68-130` 对新建 OPPORTUNITY（docStatus=NEW+stageId=null，`createOpportunityFromLead:98`）直接 convertToQuotation（`:118`）/ `testOpportunityWithoutPartnerRejected:178-191` 种子 QUALIFIED+stageId=null 断言 ERR_OPPORTUNITY_PARTNER_REQUIRED 但守卫顺序下 validateWonStage 先抛 → 已修正：Current Baseline 测试基线节改为「3 个冲突测试须显式改造 + 6 个无冲突测试零回归」，Goals/Phase 3 增独立 Fix 项（qualify 前置/won-stage seed/断言顺序调整 + 快照重录）；4 Minor（行号漂移[createOpportunityFromLead:92-108/markLeadConverted:110-116/validateNotConverted:120-129/requireLead:148-155/assertCanConvert:85-89/stageBiz:53-54/use-cases:45-46,60-61]已刷新；isWonStage 消费点 :298-300 已修正；P1-RC-032 矩阵③ 重复调用拒绝机制改为 ERR_LEAD_TYPE_MISMATCH（leadType 校验先于 docStatus，非 CONVERTED 幂等路径）已修正）。其余证据核验全部属实（9 项 baseline 全通过）。
- Independent draft review iteration 2: `accept`（本次独立草案审查，mission-driver 2026-08-14-070716）— 0 BLOCKER / 0 MAJOR。全量 load-bearing 主张经实时仓库零信任核实零伪：processor 链（`ErpCrmConversionProcessor` createOpportunityFromLead:92-108[`:93 newEntity`/`:96 setLeadType`/`:98 setDocStatus(NEW)`]/markLeadConverted:110-116/validateNotConverted:120-129/validateLeadType:131-137/requireOpportunityPartner:139-144/requireLead:148-155 ✓ + ConvertToCustomerProcessor:19-28 仅 validateNotConverted:21+validateLeadType:22 ✓ + ConvertToQuotationProcessor:21-30 仅 validateNotConverted:23+validateLeadType:24+requireOpportunityPartner:25 ✓）；`IErpCrmConversionBiz` 三方法无 convertToOpportunity ✓（:27-45）+ `ErpCrmLeadBizModel` Facade 委托 :176-178/:182-187 + stageBiz 注入 :53-54 + isWonStage 唯一消费点 findOpportunityBoardData:298-300 ✓；`ErpCrmErrors` ERR_LEAD_TYPE_MISMATCH:89/ERR_OPPORTUNITY_PARTNER_REQUIRED:92/ERR_LEAD_ALREADY_CONVERTED:95 ✓；`ErpCrmLeadStateMachine.assertCanConvert` 运行时仅拒 CONVERTED（:85-89）+ 类注释 latent gap（:32-37）+ owner doc state-machine.md:32 doc-drift 注记（plan 2026-08-13-0945-3 登记）✓；测试基线 9 @Test/402 行，testAlreadyConvertedRejected:162-176[NEW seed+首次成功 :171]/testFullConversionChain:68-130[新 OPPORTUNITY docStatus=NEW+stageId=null 直接 convertToQuotation :118]/testOpportunityWithoutPartnerRejected:178-191[QUALIFIED+stageId=null 断言 :189] 三冲突测试判定成立 + 其余 6 测试零冲突 ✓；roadmap RC-R1.21/RC-R1.22 `todo` + R1.0 done ✓；arm-index 三行 todo + 预授权类目（纯 BizModel/Processor，A4.2.88 明示不触 §5 ask-first）✓；beans.xml per-mutation Processor 注册范式 `app-service.beans.xml:101-104` ✓；ORM isWonStage :317 propId=8 ✓；use-cases L1 UC-CRM-02 直接升格分支 :45-46/UC-CRM-03 三前置 :60-61 ✓。规则 14 合并（同 owner doc 同结果表面）与范式计划 `2026-08-08-2219-1` 一致。2 MINOR 行号残留已顺手修订：(1) `use-cases.md:31`→`:39`（"Lead.docStatus == QUALIFIED 且 leadType == LEAD" 实际行）；(2) `ErpCrmLeadStateMachine.assertCanConvert:99-105`→`:81-89`（:99-105 实为 isTerminal javadoc）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-crm/erp-crm-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-032 直接升格后 weak-pointer/relatedBillType 回写

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 UC-CRM-02 直接升格分支字面仅「lead.leadType → OPPORTUNITY」，无 weak-pointer 回写要求（convertToCustomer 分支的 `markLeadConverted` 属建客户路径）；直接升格后 lead 保持 QUALIFIED 无 relatedBillType，后续转报价单时由 convertToQuotation 路径写回——不新增弱指针是本行最小修复面，若业务上需标记升格来源属策略增强非 L1 义务。
- Successor Required: `no`

### P1-RC-033/034 守卫语义与状态机 latent gap 的分层

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpCrmLeadStateMachine.assertCanConvert` 运行时仅拒 CONVERTED 的既有宽放（NEW/LOST/CANCELLED→CONVERTED 代码层允许）保持——本行在 Processor 层新增 QUALIFIED 前置守卫（更严格），状态机 Bean 的 transitions() 意图矩阵（{NEW,QUALIFIED}→CONVERTED）与运行时宽放的既有声明漂移由 owner doc 注记记录（plan 2026-08-13-0945-3 已登记为 watch-only residual），本行不重开该裁决。
- Successor Required: `no`

## Closure

Status Note: 执行完成——四 Phase 全部 [x]（Phase 1 P1-RC-032 直接升格[契约方法 + per-mutation Processor + promoteToOpportunity step + beans.xml + ERR_LEAD_NOT_QUALIFIED]；Phase 2 P1-RC-033/034 前置守卫族[validateDocStatus + validateWonStage step + 两 Processor 接线 + ERR_LEAD_STAGE_NOT_WON，守卫顺序 docStatus→won-stage→partner]；Phase 3 测试矩阵[3 冲突测试改造 + 快照重录 + TestErpCrmConversionGuards 6 组 + RECORDING→CHECKING]；Phase 4 文档回填[state-machine.md/README.md 注记 + arm-index 三行 done + roadmap RC-R1.21/22 done ✅ + 日志]）。验证全绿：`mvn test -pl module-crm/erp-crm-service` 168/168 + `mvn clean install -DskipTests` 全仓 BUILD SUCCESS + compliance checker exit 0 actual==baseline（R1d=14/R2a=34/R2b=229/R2c=1392/R2d=34/R3=5/R5=0/R6=2/R10=7/R11=0/R12a=69/R12b=66/R12c=40 零漂移）+ 零 ORM 变更（git diff 仅 erp-crm-dao/service Java + beans.xml + `_cases/` 快照 + docs）。独立结束审计由独立子代理（新会话）执行通过（见 Closure Audit Evidence 实仓复核）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（本审计会话 ses_00006f147ffe3uO69C03ntfI12，零执行者上下文，只读零信任）
- Evidence: 8 项核实全 PASS：(1) 计划状态一致性——4 Phase 全 [x]/completed + Closure Gates 7/8 [x]（审计门控留白待独立审计，正确）+ 占位符按设计待填；(2) P1-RC-032——`IErpCrmConversionBiz.java:36-37` 契约 + `ErpCrmConversionConvertToOpportunityProcessor.java:13-22` + `promoteToOpportunity`（`ErpCrmConversionProcessor.java:179-185`，validateLeadType 先于 validateDocStatus → setLeadType+updateEntity 不建 Partner/新 Lead 不改 docStatus）+ `ErpCrmLeadBizModel.java:193-197` @BizMutation 委托 + beans.xml 注册 + `ErpCrmErrors.java:92-94` ERR_LEAD_NOT_QUALIFIED；(3) P1-RC-033/034——`validateDocStatus`（:151-157）+ `validateWonStage`（:164-172，经 stageBiz 查 isWonStage，ORM propId=8 现存）+ 两 Processor 接线（守卫顺序实证 :23/:25-26）+ `ErpCrmErrors.java:96-98` ERR_LEAD_STAGE_NOT_WON；(4) 零 ORM 变更（git status/diff 零命中 model/*.orm.xml、*.api.xml、会计/删除文件）；(5) 测试——3 冲突测试改造（testAlreadyConvertedRejected QUALIFIED 种子 / testFullConversionChain qualify+moveStage(STAGE_WON) / testOpportunityWithoutPartnerRejected won-stage stageId）+ TestErpCrmConversionGuards 6 组 + `_cases/` 快照齐备（新类 6 目录 + 改造测试 output/input 更新）；(6) 验证亲跑——erp-crm-service 168 tests 0 失败 + checker exit 0 全 baseline 对齐 + 全仓 `mvn clean install -DskipTests` BUILD SUCCESS + app-erp-all 3 项已知失败（ErpMfgCostRollupLine materialBand / TestAuthSeedLoadingProof NPE）经 `known-good-baselines.md:63-64` + `docs/bugs/` 两 bug 文件归因先于本计划、无因果（执行者 stash 验证同失败，根因 mfg E4.1 commit 452e418d0 + nop-entropy lazy-property 重建）；(7) 文档回填——arm-index 三行 done + roadmap 两行 done ✅ + owner doc 注记 + 日志条目 + use-cases.md 契约段零修改；(8) 反模式——R5=0（@Inject 全非 private）、R3 零增量（新文件无 new Erp*()）、R4=0、错误码全 ErrorCode.define。**结论 PASS，无 FAIL 项、无必须修复问题。**

Follow-up:

- 无范围外 follow-up（Deferred But Adjudicated 2 项均 watch-only residual + successor no）；MR1 第一批后续 RC-R1.23+（评分调度接线/UTM 归因族等）由 mission driver 继续。
