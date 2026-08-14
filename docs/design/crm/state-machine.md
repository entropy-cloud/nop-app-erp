# CRM 域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> CRM 域有两个状态对象：**线索/商机（Lead/docStatus）** 与 **活动/事件（Event/status）**。

## 适用对象一：线索/商机（Lead — docStatus）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 新建（NEW） | 新线索/新商机已创建，等待跟进 | 无单据联动 |
| 已验证（QUALIFIED） | 线索已验证为有效商机，进入漏斗阶段管理 | 允许设置 stageId，概率、预计收入可编辑 |
| 已转化（CONVERTED） | 终态：已转报价单或客户 | lead.relatedBillType/Code 不为空，业务已交接给 sales 域 |
| 已丢失（LOST） | 终态：丢单，录入丢单原因 | 关联活动已归档 |
| 已取消（CANCELLED） | 终态：无效/重复线索 | 不可再关联新活动 |

### 2. 迁移完整性

```
NEW（新线索/新商机创建）
  ├─ 跟进 → QUALIFIED（已验证，进入漏斗阶段管理）
  │            ├─ 转化 → CONVERTED（经 leadType 门控，终态）
  │            ├─ 标记丢失 → LOST（录入丢单原因，终态）
  │            └─ 取消 → CANCELLED（无效/重复，终态）
  ├─ 转化 → CONVERTED（LEAD 类型直接转客户+商机，经 leadType 门控，终态）
  ├─ 标记丢失 → LOST（线索阶段直接丢单）
  └─ 取消 → CANCELLED（无效/重复，终态）
```

> **convert 来源态 doc-drift 补正（plan 2026-08-13-0945-3 Phase 2 Fix）**：原 §2 声明「QUALIFIED→CONVERTED」与代码实况漂移。代码实况：`ErpCrmConversionConvertToCustomerProcessor`/`...ConvertToQuotationProcessor` 仅调 `validateNotConverted`（拒绝 CONVERTED 幂等）+ `validateLeadType` 门控，**零 QUALIFIED 来源态守卫**——即任何非 CONVERTED 态经 leadType 门控→CONVERTED（含 NEW→CONVERTED：`convertToCustomer` 从 LEAD 类型 NEW 直接转化，测试 `TestErpCrmLeadConversion.testAlreadyConvertedRejected` 证之）。裁定（路线图规则 5，保持既有外部行为不变）：Bean `assertCanConvert` 运行时仅拒 CONVERTED；owner doc §2 就地补正为「{NEW,QUALIFIED}→CONVERTED 经 leadType 门控」。`ErpCrmLeadStateMachine.transitions()` 编码意图矩阵 {NEW,QUALIFIED}→CONVERTED。
>
> **已知 latent gap（watch-only residual，非本计划引入）**：`assertCanConvert` 运行时仅拒 CONVERTED，故 LOST/CANCELLED→CONVERTED 在代码中技术上允许（`validateNotConverted` 不拦），但意图矩阵（`transitions()` + 上图）限定 {NEW,QUALIFIED}→CONVERTED。此为迁移前已存在的既有 latent gap（运行时宽放），裁定保持运行时宽放（不新增来源态限制，避免行为变化）；若产品要求收窄须开 successor（触及业务行为 ask-first）。
>
> **转化前置守卫实现注记（plan 2026-08-14-1815-1 RC-R1.21/RC-R1.22 落地）**：转化链前置条件在 Processor 层新增守卫，状态机 Bean 语义不变（本 § latent gap 分层裁决保持）：
> - **直接升格（convertToOpportunity，UC-CRM-02「不创建客户」分支）**：校验 leadType==LEAD + docStatus==QUALIFIED → 原 lead 原地 `leadType=OPPORTUNITY`，**不建 Partner/新 Lead、docStatus 保持 QUALIFIED**（保持 QUALIFIED 使后续 convertToQuotation 前置成立；置 CONVERTED 将阻断 UC-CRM-03 链路）。非 QUALIFIED 抛 `ERR_LEAD_NOT_QUALIFIED`；非 LEAD 类型抛 `ERR_LEAD_TYPE_MISMATCH`（leadType 校验先于 docStatus）。
> - **convertToCustomer 前置（UC-CRM-02）**：`validateLeadType(LEAD)` 后新增 `validateDocStatus(QUALIFIED)`——非 QUALIFIED（NEW/LOST/CANCELLED/CONVERTED）抛 `ERR_LEAD_NOT_QUALIFIED` 拒绝（L1「Lead.docStatus == QUALIFIED 且 leadType == LEAD」双前置落地）。
> - **convertToQuotation 前置（UC-CRM-03）**：`validateLeadType(OPPORTUNITY)` 后新增 `validateDocStatus(QUALIFIED)` + `validateWonStage`（经 `lead.stageId` → `ErpCrmStage.isWonStage==true`，stageId null 或非 won-stage 抛 `ERR_LEAD_STAGE_NOT_WON`）——守卫顺序确定化：docStatus → won-stage → partner（`ERR_OPPORTUNITY_PARTNER_REQUIRED` 可达性不受影响）。
> - **守卫与状态机分层边界**：`assertCanConvert` 运行时仅拒 CONVERTED 的既有宽放保持（本行前置守卫在 Processor 层新增，不修改状态机 Bean 语义——NEW 状态 LEAD 直接升格/转化仍被 Processor 层 QUALIFIED 守卫拒绝，属更严格的业务前置，非状态机迁移收窄）。

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| NEW→QUALIFIED | 销售员 | leadType=LEAD，联系人信息必填 | 允许设置 stageId，概率取阶段默认值 |
| NEW→LOST | 销售员 | — | lostReasonId 必填 |
| NEW→CANCELLED | 销售员/管理员 | — | 不可恢复 |
| NEW→CONVERTED | 销售员 | leadType=LEAD（convertToCustomer 从 LEAD 类型 NEW 直接转化：建客户 + 新建 OPPORTUNITY(NEW) + 原 lead 弱指针） | relatedBillType/Code 写入，触发跨域 master-data 创建客户 |
| QUALIFIED→CONVERTED | 销售员（转报价单需权限） | leadType=OPPORTUNITY（经 convertToQuotation 转报价单；LEAD 需先经 convertToCustomer 转为 OPPORTUNITY 再转报价单） | relatedBillType/Code 写入，触发跨域创建报价单 |
| QUALIFIED→LOST | 销售员 | lostReasonId 必填 | 丢单归档 |
| QUALIFIED→CANCELLED | 销售员/管理员 | — | 不可恢复 |

**stageId 迁移规则**：stageId 是独立于 docStatus 的维度。docStatus=QUALIFIED 后，stageId 沿 ErpCrmStage.sequence 递增前移（不能跳级回退），isWonStage 到达时允许触发转化。阶段回退（toStage.sequence < fromStage.sequence）经 `ErpCrmLeadProcessor.validateStageDirection` 守卫拦截：STRICT 模式（`erp-crm.allow-stage-backward` 默认 false）抛 `ERR_STAGE_BACKWARD_MOVE`；业务确需回退（销售流程阶段反复）时设 `erp-crm.allow-stage-backward`=true 放行（LOG.warn + 仍写 convLog 审计留痕）。fromStageId 为 null（首次入漏斗）跳过方向校验。

### 3. 终态与恢复

- 终态：`CONVERTED`、`LOST`、`CANCELLED`。
- 终态不可直接恢复。若需重新跟进，复制原线索创建新 Lead（标记参考原记录）。
- CONVERTED 后不可再关联新活动（仅保留历史活动时间线）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| LEAD 类型直接转报价单 | 系统拦截：先转 OPPORTUNITY 再转报价单 |
| LOST 不填丢单原因 | 拒绝迁移：lostReasonId 必填 |
| 重复线索提交 | 查重服务提示合并/跳过，不阻塞创建 |
| 并发更新同一 Lead | 乐观锁 |
| 阶段跳级（跳过 sequence 递增） | 拒绝：只能前移到下一阶段。回退（toSeq<fromSeq）由 `validateStageDirection` 守卫：STRICT 默认抛 `ERR_STAGE_BACKWARD_MOVE`；`erp-crm.allow-stage-backward`=true 放行 |
| 已转化的 Lead 创建新 Event | 允许（保留活动历史），但不可修改 stageId |

### 5. 可达性

- 从 NEW 可达全部终态（CONVERTED/LOST/CANCELLED）。
- QUALIFIED 不可回到 NEW（已进入漏斗的商机不可降级为线索）。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| NEW→QUALIFIED | 销售员（owner 或 team 成员） |
| NEW→LOST | 销售员 |
| NEW→CANCELLED | 销售员/管理员 |
| QUALIFIED→CONVERTED | 销售员 + 转报价单需报价权限 |
| QUALIFIED→LOST | 销售员 |
| QUALIFIED→CANCELLED | 销售员/管理员 |
| stageId 前移 | 销售员（owner 或 team 成员） |

危险操作：
- **转化（→CONVERTED）**：不可逆，需确认用户意图（二次确认弹窗）。
- **取消（→CANCELLED）**：不可逆，关联活动保留但 Lead 不可操作。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| Lead 转化时创建报价单 | 调用 `IErpCrmConversionBiz.convertToQuotation()` → 跨域调用 `IErpSalQuotationBiz` |
| Lead 转化时创建客户 | 调用 `IErpCrmConversionBiz.convertToCustomer()` → 在 master-data 域创建 ErpMdPartner |
| 线索查重 | 查询同企业名/邮箱/电话的已有 Lead，提示用户处理 |
| 事件提醒 Job | nop-job 定时查询 ErpCrmEvent（PLANNED + startDateTime 临近），发送通知 |

> 漏斗/转化率报表（`FunnelAggregationEngine`）按 stage `sequence` 排序假设 monotonic progression。STRICT 守卫（`erp-crm.allow-stage-backward` 默认 false）保护该不变量；allow-backward=true 放行回退时，转化率/dropOffRate 按 sequence 排序为近似值（历史 convLog 仍全量留痕，审计不丢）。

外部触发渠道：
- 销售员手动创建/编辑（主要渠道）。
- 营销活动归因（新线索通过 UTM 参数预填充 campaignId/medium/source）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| NEW | 否（新建时刻已分配 owner） | — |
| QUALIFIED | 是 | assigned（销售员）—— 按 stageId 待跟进 |
| CONVERTED | 否 | — |
| LOST | 否 | — |
| CANCELLED | 否 | — |

避免"QUALIFIED 阶段长期停滞"：超过 7 天无 stageId 前移或 Event 记录时，向 owner 发出跟进提醒。

### 9. 场景演练

见 `use-cases.md`。

### 10. 与设计文档一致性

- Lead 字段定义见 `README.md` §ErpCrmLead。
- 转化流见 `README.md` §跨域协作。
- 状态码归 `model/app-erp-crm.orm.xml`。

---

## 适用对象二：活动/事件（Event — status）

### 1. 状态定义

| 状态 | 业务含义 |
|------|----------|
| 已计划（PLANNED） | 事件已排程，等待执行 |
| 已完成（COMPLETED） | 事件已执行完成 |
| 已取消（CANCELLED） | 事件取消 |

### 2. 迁移完整性

```
PLANNED
  ├─ 完成 → COMPLETED
  └─ 取消 → CANCELLED
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| PLANNED→COMPLETED | owner 或任意有权限用户 | 无 | 事件归档，写入活动时间线 |
| PLANNED→CANCELLED | 创建者/管理员 | — | 事件取消 |

### 3. 终态与恢复

- 终态：`COMPLETED`、`CANCELLED`。
- 终态不可恢复。若需重新安排，新建 Event（可关联原事件的 parentEventId）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| COMPLETED 后修改事件内容 | 拒绝：已完成事件不可编辑 |
| 重复事件实例完成 | 单个实例独立完成，不影响其他实例 |
| 关联 Lead 已 CONVERTED 后创建 Event | 允许（保留活动历史） |

### 5. 可达性

- 从 PLANNED 可达 COMPLETED 或 CANCELLED。
- 终态无出边。无不可达状态。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| PLANNED→COMPLETED | owner/团队/管理员 |
| PLANNED→CANCELLED | 创建者/管理员 |

### 7. 外部依赖

- 事件提醒 Job 读取 PLANNED 事件，按 per-event reminderMinutesBefore 计算到期窗口（reminderMinutesBefore 非 null 时用它，否则 fallback 全局 windowMinutes 默认 60）发送通知（邮件/站内信）。
- Event 可关联 ErpCrmLead（relatedLeadId）、ErpMdPartner（partnerId）实现多态弱指针。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| PLANNED | 是 | assigned（owner）—— 待执行事件 |
| COMPLETED | 否 | — |
| CANCELLED | 否 | — |

### 9. 场景演练

见 `use-cases.md`。

### 10. 与设计文档一致性

- Event 字段定义见 `README.md` §ErpCrmEvent。
- 提醒 Job 见 `README.md` §业务规则 4。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- Lead 从 QUALIFIED 回退到 NEW 是否被禁止。
- LOST 时 lostReasonId 必填是否落实。
- 转化（CONVERTED）的不可逆确认机制。
- 阶段迁移（stageId）的 sequence 单向递增约束（`ErpCrmLeadProcessor.validateStageDirection`：STRICT 默认 `erp-crm.allow-stage-backward`=false 拦截回退，true 放行）。
- 事件提醒 Job 与 Event 状态的联动（PLANNED 才触发提醒）。
