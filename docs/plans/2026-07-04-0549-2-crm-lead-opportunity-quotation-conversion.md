# 2026-07-04-0549-2-crm-lead-opportunity-quotation-conversion CRM 线索→商机→报价单转化

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` 工作项 3.1（M3 首项，CRM 转化核心）；`docs/design/crm/README.md`
> Related: `2026-07-01-1426-2-sales-quotation-to-order-and-order-approval.md`（报价单 IErpSalQuotationBiz 创建契约基线）
> Audit: required

## Current Baseline

- CRM CRUD 全 done（`crud-roadmap.md`）。`ErpCrmLead`（`module-crm/model/app-erp-crm.orm.xml:184`）已建模为单实体 + `leadType ∈ {LEAD, OPPORTUNITY}` 判别，含 `partnerId`/`stageId`/`leadStatusId`/`docStatus`/`relatedBillType`/`relatedBillCode`/`lostReasonId`/`probability` 等字段——但仅有 codegen 空壳 `CrudBizModel`，**无自定义状态机/转化动作**。
- 支撑实体已就绪：`ErpCrmStage`（漏斗阶段，sequence + defaultProbability + isWonStage，orm.xml:265）、`ErpCrmLeadStatus`（orm.xml:245）、`ErpCrmLeadConvLog`（阶段流转审计 fromStageId/toStageId，orm.xml:471）、`ErpCrmSource`、`ErpCrmLostReason` 均已建模。
- 字典 `erp-crm/lead-doc-status`（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED）、`erp-crm/lead-type`（LEAD/OPPORTUNITY）已就绪。
- 跨域契约就绪：`IErpSalQuotationBiz`（`module-sales/erp-sal-dao/.../IErpSalQuotationBiz.java`，extends ICrudBiz）、`IErpMdPartnerBiz`（`module-master-data/erp-md-dao/.../IErpMdPartnerBiz.java`）。
- sales 域 `ErpSalQuotation` 已存在，**零 opportunityId 字段**（核心零污染约束成立）。
- CRM 域**无独立业财过账 businessType**（`crm/README.md §业财过账`）——转化不产生凭证，报价单凭证在 sales 域生成。

## Goals

- Lead `docStatus` 状态机：`NEW → QUALIFIED`（跟进验证入漏斗）、`QUALIFIED/NEW → LOST`（丢单原因必填）、`→ CANCELLED`，`QUALIFIED → CONVERTED`（转化终态）；非法迁移被拒。
- 漏斗阶段流转 `moveStage`：`stageId` 按 `ErpCrmStage.sequence` 前移，写 `ErpCrmLeadConvLog` 审计，`probability` 未填时取阶段 `defaultProbability`。
- 转化闭环：`convertToCustomer`（LEAD → `IErpMdPartnerBiz` 建客户 + 新建 OPPORTUNITY lead + 原 lead 弱指针 + CONVERTED）；`convertToQuotation`（OPPORTUNITY → `IErpSalQuotationBiz` 建报价单 + 弱指针回写 `relatedBillType=SALES_QUOTATION` + CONVERTED）。
- 线索查重：提交/保存 Lead 时按 companyName/contactEmail/contactPhone 检测重复（提示，config-gated auto-merge 默认关）。
- 核心零污染：sales/master-data 实体**零字段新增**，转化结果存在 CRM 侧弱指针。

## Non-Goals

- **CRM 活动日历/事件提醒（工作项 3.2）**：`ErpCrmEvent` 日历/提醒 Job 是独立结果面，本计划不做。
- **线索评分引擎（3.3）/销售预测（3.4）**：依赖本计划交付的 Lead 状态机/转化基线，独立计划承接。
- **营销活动 UTM 归因分析报表**：UTM 字段已建模，归因报表属 nop-report 独立面。
- **报价模板（QuoteTemplate）/多级转化审批工作流引擎**：本期单级转化，无审批引擎。
- **自动合并重复线索**：config `auto-convert-duplicate-lead` 默认关，本期仅提示不合并。

## Task Route

- Type: `implementation-only change`（无 ORM 模型变更——实体/字典全就绪，仅 BizModel 自定义动作 + 跨域 I*Biz 调用）
- Owner Docs: `docs/design/crm/README.md`（权威设计，§业务规则1 转化流 / §衔接契约 IErpCrmConversionBiz）、`docs/design/sales/README.md`（与 sales 边界）、`docs/design/master-data/README.md`（合作伙伴主数据）
- Skill Selection Basis: BizModel 自定义动作 + 状态机 + 跨实体 I*Biz 调用 → 加载 `nop-backend-dev`；状态机可达性自检由该技能路由

## Infrastructure And Config Prereqs

- 配置项 `erp-crm.auto-convert-duplicate-lead`（默认 false，发现重复线索仅提示不自动合并）。
- 配置项 `erp-crm.default-team-id`（新线索默认团队，可空）。
- 无外部服务/端口/密钥依赖；无数据迁移。

## Execution Plan

### Phase 1 - Lead docStatus 状态机 + 漏斗阶段流转审计

Status: completed
Targets: `ErpCrmLeadBizModel.java`（自定义动作）；`ErpCrmErrors`/`ErpCrmConstants`；`LeadDuplicateChecker.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] Add: `ErpCrmLeadBizModel` 状态机动作——`qualify`（NEW→QUALIFIED，入漏斗设默认 stageId）、`lose`（NEW/QUALIFIED→LOST，`lostReasonId` 必填校验，否则抛 `ERR_LOST_REASON_REQUIRED`）、`cancel`（→CANCELLED）。范式对照既有 purchase/sales 三轴审批 BizModel。
  - Skill: `nop-backend-dev`
- [x] Add: `moveStage(leadId, toStageId)`——按 `ErpCrmStage.sequence` 校验前移（禁止回退或允许回退记 Decision）、写 `ErpCrmLeadConvLog`（fromStageId/toStageId/changedAt/changedBy）、`probability` 为空时取目标阶段 `defaultProbability`。
  - Skill: `nop-backend-dev`
- [x] Decision: 阶段回退策略——记录选择：允许回退（销售流程中阶段可能反复），但 convLog 全量留痕（审计不丢）。替代方案：禁止回退——被否（不符合真实销售跟进节奏）。
  - Skill: `nop-backend-dev`
- [x] Add: `LeadDuplicateChecker`——保存/提交 Lead 时按 companyName/contactEmail/contactPhone 命中既有非终态 Lead 则返回候选列表（`auto-convert-duplicate-lead=false` 默认仅提示，不阻断）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅交付状态机 + 阶段流转 + 查重。完整仓库验证归 Closure Gates。

- [x] 状态机四迁移（qualify/lose/cancel + moveStage）可达且非法迁移/缺 lostReason 抛正确 ErrorCode
- [x] moveStage 写入 convLog 审计行 + probability 默认回填可验证

### Phase 2 - 转化闭环（convertToCustomer / convertToQuotation）

Status: completed
Targets: `IErpCrmConversionBiz.java`；`ErpCrmConversionBizModel.java`（或 ErpCrmLeadBizModel 转化动作）；跨域注入 IErpMdPartnerBiz / IErpSalQuotationBiz
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] Add: `IErpCrmConversionBiz` 契约（`convertToCustomer(leadId) → ErpMdPartner`、`convertToQuotation(leadId, quotationData) → ErpSalQuotation`），范式对照 `crm/README.md §衔接契约`。
  - Skill: `nop-backend-dev`
- [x] Add: `convertToCustomer`——校验 `leadType==LEAD`；`IErpMdPartnerBiz` 从 contactName/companyName/contactPhone/contactEmail 派生建客户；新建 `ErpCrmLead(leadType=OPPORTUNITY, partnerId=新客户, docStatus=NEW)`；原 lead `relatedBillType=CRM_LEAD`+新商机 code + `docStatus=CONVERTED`。
  - Skill: `nop-backend-dev`
- [x] Add: `convertToQuotation`——校验 `leadType==OPPORTUNITY` 且 `partnerId` 非空（否则抛 `ERR_OPPORTUNITY_PARTNER_REQUIRED`）；`IErpSalQuotationBiz` 建报价单（跨域经 I*Biz，核心零污染）；回写 lead `relatedBillType=SALES_QUOTATION`+`relatedBillCode`+`docStatus=CONVERTED`。
  - Skill: `nop-backend-dev`
- [x] Decision: 转化幂等性——记录选择：`docStatus==CONVERTED` 的 lead 再次转化抛 `ERR_LEAD_ALREADY_CONVERTED`（不可重复转化）；幂等键为 lead.id。替代方案：返回既有转化结果——被否（隐藏重复操作，销售流程需显式反馈）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] LEAD→客户+商机、OPPORTUNITY→报价单两条转化链可验证；sales/master-data 实体零字段新增（核心零污染约束由 grep 门控证明：`ErpSalQuotation`/`ErpMdPartner` 无 opportunityId 新增）
- [x] 重复转化 / OPPORTUNITY 缺 partner 抛正确 ErrorCode

### Phase 3 - 端到端 + 文档对齐

Status: completed
Targets: 端到端测试；`crm/README.md`；`extended-roadmap.md`；`docs/logs/{year}/07-04.md`
Skill: `nop-backend-dev`, `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] Proof: 端到端测试 `TestErpCrmLeadConversion`（JunitAutoTestCase）——(a) 全链路：建 LEAD（触发查重提示）→ qualify → convertToCustomer（建客户+OPPORTUNITY+原 lead CONVERTED 弱指针）→ moveStage 前移 + convLog → convertToQuotation（经 `IErpSalQuotationBiz` 的 `ICrudBiz.save` 建报价单+弱指针+CONVERTED）；(b) 异常路径：LOST 缺 lostReason 报错、已 CONVERTED 重复转化报错、OPPORTUNITY 缺 partner 转报价单报错；(c) 核心零污染断言：sales/master-data 实体无 CRM 外键。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 全链路端到端（LEAD→客户→商机→报价单）+ 异常路径全部可验证
- [x] 核心零污染断言通过（弱指针仅在 CRM 侧）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0d60781f2ffeO2rqhzrhlurh4n`) — VERDICT acceptable as-is，无 BLOCKER。独立子代理逐项核实基线（ErpCrmLead/Stage/LeadStatus/LeadConvLog 已建模、lead-doc-status/lead-type 字典就绪、IErpSalQuotationBiz extends ICrudBiz、IErpMdPartnerBiz 存在、ErpSalQuotation 无 opportunityId 即核心零污染约束成立、CRM 无 businessType、ErpCrmLeadBizModel 为 codegen 空壳无转化逻辑）。3.1 vs 3.2/3.3/3.4 拆分经 rule 14 例外（验证路径实质性不同）裁断合理。应用 advisory 修订：Phase 3 补 `Skill: nop-testing`（JunitAutoTestCase 工作方法）、convertToQuotation 经 `ICrudBiz.save` 建报价单补述。无范围内缺陷隐藏于 Deferred。
- Last Reviewed: 2026-07-04

## Closure Gates

> 完整仓库验证在此处一次运行。无业财过账（CRM 无 businessType），删除过账相关验证门控。

- [x] 范围内行为完成（docStatus 状态机 + 漏斗阶段流转 + 双转化闭环 + 查重）
- [x] 相关文档对齐（`crm/README.md` 实现偏离补注、`extended-roadmap.md` 3.1 标 done、`docs/logs/{year}/07-04.md`）
- [x] 已运行验证：`mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-crm -am`（含新增端到端 + 既有 CRUD 冒烟零回归）；sales/master-data 既有套件零回归（跨域 I*Biz 调用）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CRM 活动日历/事件提醒（工作项 3.2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpCrmEvent` 日历 + nop-job 提醒是独立结果面，本计划聚焦转化流。
- Successor Required: `yes`（触发条件：CRM 活动时间线/提醒需求时——下一 CRM 计划）

### 线索评分引擎（3.3）/ 销售预测（3.4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖本计划交付的 Lead 状态机/转化基线；评分配置实体（ErpCrmLeadScoreConfig/Line/Score/Line）已建模但引擎未实现，独立计划承接。
- Successor Required: `yes`（触发条件：CRM 评分/预测需求时）

### 自动合并重复线索 / 报价模板 / 多级转化审批

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config 默认关仅提示；模板本期不用；审批本期单级。
- Successor Required: `yes`（触发条件：对应需求落地时）

## Closure

Status Note: closed (independent closure audit APPROVED, all 8 gates pass)

Closure Audit Evidence:

- Auditor / Agent: Independent closure audit subagent (fresh session) — independent of implementer session
- Verdict: APPROVED (all 8 gates PASS)
- Verification performed against live repo on 2026-07-04:
  * Gate 1: `ErpCrmLeadProcessor` (qualify/lose/cancel/moveStage+convLog) + `ErpCrmConversionProcessor`
    (convertToCustomer via `IErpMdPartnerBiz.save`, convertToQuotation via `IErpSalQuotationBiz.save`) +
    `LeadDuplicateChecker` + `ErpCrmErrors` (5 required codes) all present and behaviorally complete.
  * Gate 1 ZERO-POLLUTION: grep proves `ErpSalQuotation`/`ErpMdPartner` have no opportunityId/leadId field;
    weak pointers exist only on `ErpCrmLead` in module-crm.
  * Gate 3: `mvn clean install -DskipTests` → BUILD SUCCESS; `mvn test -pl module-crm/erp-crm-service`
    → 14/14 pass (9 new + 5 smoke); module-sales 69/69 pass, module-master-data 11/11 pass (zero cross-domain regression).
  * Gate 5: Draft Review Record accepted (`ses_0d60781f2ffeO2rqhzrhlurh4n`, no BLOCKER).
- Status Note: All in-scope Phase 1/2/3 items delivered; deferred items (3.2/3.3/3.4/
  quote-template/multi-level-approval) are pre-declared Non-Goals, not mid-execution downgrades.

Follow-up:

- CRM 活动日历/事件提醒（3.2，见 Deferred）
- 线索评分引擎（3.3）/ 销售预测（3.4，见 Deferred）
