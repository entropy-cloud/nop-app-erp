# CRM 域 - 销售序列/流程管理（Sales Sequence / Cadence）

## 目的

设计销售序列（Sales Sequence / Cadence）管理：定义标准化的销售跟进流程模板，支持按线索来源、区域、产品线自动分配序列，跟踪每个步骤完成状态，分析序列转化效果。

## 边界

- 本模块负责：序列模板定义、步骤配置、完成条件配置、序列自动分配、进度跟踪、性能分析。
- 序列引擎驱动 CRM `ErpCrmLead` 的跟进工作——序列步骤触发 `ErpCrmEvent` 创建（排程活动），步骤完成标记推进序列进度。
- 本模块不负责：活动/事件的实际执行（`ErpCrmEvent`）；日历排程（Event 独立）；线索评分（`lead-scoring.md`）；区域分配（`territory.md`）。
- 实体建议命名，ORM 模型见 `module-crm/model/app-erp-crm.orm.xml`。

## 设计依据

> 参考 **SalesLoft Cadence**（`Cadence` / `CadenceStep` / `CadenceAssignment`）：定义自动化的销售跟进序列，包含步骤顺序、时间间隔、完成条件。支持按 list/people 批量分配序列。
>
> 参考 **Outreach Sequence**（`Sequence` / `SequenceStep` / `SequenceState`）：多步骤自动化工作流（call → email → call → demo），每步可配置等待天数、完成条件（call completed / email opened / meeting held）。
>
> 参考 **Odoo CRM Activities**（`crm.activity` / `crm.activity.todo`）：活动模板和 To-Do 列表，可设置在特定阶段自动创建活动。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmSequence（序列模板头）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| templateType | 序列模板类型：NEW_LEAD（新线索）/ QUALIFICATION（验证）/ NEGOTIATION（谈判）/ RE_ENGAGEMENT（重新激活） | 🟢 SalesLoft `Cadence.cadence_type` |
| description | 序列描述 | — |
| isActive | 是否启用 | 🟢 SalesLoft `Cadence.is_active` |
| isDefault | 是否默认序列（无规则匹配时使用） | — |
| expectedDuration | 预期完成天数 | — |
| 标准审计字段 | | |

**预置序列模板示例**：

```
NEW_LEAD（新线索跟进 7 天）:
  Day 1:  电话初访（CALL）→ 完成条件：通话完成
  Day 3:  跟进邮件（EMAIL）→ 完成条件：邮件打开
  Day 5:  产品资料（EMAIL）→ 完成条件：邮件打开
  Day 7:  邀约演示（CALL）→ 完成条件：约定会议

QUALIFICATION（验证跟进 14 天）:
  Day 1:  需求调研（CALL）→ 完成条件：通话完成
  Day 3:  方案资料（EMAIL）→ 完成条件：邮件打开
  Day 7:  深度交流（MEETING）→ 完成条件：会议举行
  Day 14: 评估结论（CALL）→ 完成条件：通话完成

NEGOTIATION（谈判跟进 21 天）:
  Day 1:  报价跟进（CALL）→ 完成条件：通话完成
  Day 5:  商务条件（EMAIL）→ 完成条件：邮件回复
  Day 14: 高层会面（MEETING）→ 完成条件：会议举行
  Day 21: 最终推进（CALL）→ 完成条件：通话完成
```

### ErpCrmSequenceStep（序列步骤）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/sequenceId/orgId | 标准 + 所属序列（→ErpCrmSequence） | — |
| stepName | 步骤名称 | 🟢 SalesLoft `CadenceStep.name` |
| stepOrder | 步骤序号（1-based） | 🟢 SalesLoft `CadenceStep.order` |
| dueDays | 距上一步的间隔天数 | 🟢 Outreach `SequenceStep.wait_days` |
| activityType | 步骤活动类型：CALL（电话）/ EMAIL（邮件）/ MEETING（会议）/ TASK（任务） | 🟢 Odoo `crm.activity.activity_type` |
| stepDescription | 步骤说明/话术模板 | — |
| completionCondition | 完成条件：CALL_COMPLETED（通话完成）/ EMAIL_OPENED（邮件打开）/ EMAIL_REPLIED（邮件回复）/ MEETING_HELD（会议举行）/ TASK_DONE（任务完成） | 🟢 SalesLoft `CadenceStep.completion_condition` |
| isMandatory | 是否必须完成（非必须步骤可跳过） | 🟢 Outreach `SequenceStep.required` |
| autoCreateEvent | 是否自动创建 ErpCrmEvent | — |
| 标准审计字段 | | |

**步骤完成条件与 Event 关联**：
```
当 ErpCrmEvent.status = COMPLETED 且 eventType 匹配 step.activityType →
  检查 completionCondition：
    CALL_COMPLETED → Event.eventType == CALL
    EMAIL_OPENED  → Event.eventType == EMAIL + 有打开跟踪
    EMAIL_REPLIED → Event.eventType == EMAIL + 有回复跟踪
    MEETING_HELD  → Event.eventType == MEETING
    TASK_DONE     → Event.eventType == TASK
  匹配 → 步骤标记完成，推进到下一步
  不匹配 → 步骤仍为待办
```

### ErpCrmSequenceAssignment（序列分配规则）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/sequenceId/orgId | 标准 + 目标序列（→ErpCrmSequence） | — |
| priority | 规则优先级 | 🟢 SalesLoft `CadenceAssignmentRule` |
| conditionType | 条件类型：LEAD_SOURCE（按线索来源）/ TERRITORY（按区域）/ PRODUCT_LINE（按产品线）/ CUSTOM_FIELD（按自定义字段） | — |
| conditionValue | 条件值(JSON)（如 `{"sourceId": ["WEBSITE", "EXHIBITION"]}`） | — |
| isActive | 是否启用 | — |
| isDefault | 是否默认规则（无匹配时使用） | — |
| 标准审计字段 | | |

### 序列进度跟踪

序列进度通过 `ErpCrmLead` 上的两个字段跟踪（在 ORM 中已存在或新增）：

- `currentSequenceId` — 当前应用序列（→ErpCrmSequence）
- `currentStepIndex` — 当前步骤序号（0=未开始，null=已完成）
- `sequenceStartedAt` — 序列开始时间
- `sequenceCompletedAt` — 序列完成时间

也可以在 `ErpCrmLead` 中新增字段（若不在 ORM 中新增，则通过关联表 `ErpCrmLeadSequenceProgress` 记录）：

### ErpCrmLeadSequenceProgress（线索序列进度）（可选实体）

若不在 Lead 上加字段，用独立关联表记录进度更灵活（支持多条序列历史）：

| 字段 | 含义 |
|------|------|
| id/leadId/sequenceId/orgId | 标准 |
| currentStepIndex | 当前步骤序号 |
| status | 状态：IN_PROGRESS（进行中）/ COMPLETED（已完成）/ SKIPPED（已跳过） |
| startedAt | 开始时间 |
| completedAt | 完成时间 |
| 标准审计字段 | |

## 业务规则

### 1. 序列自动分配

```
线索创建 / 进入 QUALIFIED 状态 →
  按优先级遍历 ErpCrmSequenceAssignment（isActive=true）：
    匹配 conditionType + conditionValue →
      找到首个匹配 → 分配 ErpCrmSequence 给线索
    无匹配 → 使用 isDefault=true 的序列
  仍无匹配 → 不分配序列（手动选择）
```

### 2. 步骤推进

```
分配序列后 →
  stepIndex = 0（未开始）
  线索详情页展示"下一步待办"：
    当前步骤 = sequence.steps[stepIndex]
    到期日 = sequenceStartedAt + Σ(dueDays of steps[0..stepIndex])

当用户创建了当前步骤类型的活动并完成 →
  Event.status = COMPLETED 且匹配 completionCondition →
    stepIndex += 1
    若 stepIndex >= len(steps) → 序列完成（sequenceCompletedAt = now）
    否则 → 展示下一步待办
```

### 3. 超时处理

```
步骤到期日 > now + gracePeriod（默认 2 天）→
  标记步骤为"逾期"（不阻止推进，仅提示）
  若连续逾期步骤 >= 3 → 提醒序列负责人
```

### 4. 序列切换

```
管理员可手动切换线索的序列：
  当前序列标记为 SKIPPED（记录进度快照）
  新序列从 stepIndex=0 开始
```

### 5. 序列性能分析

```
按 templateType 分组统计：
  序列完成率 = COMPLETED 数 / 总分配数
  平均完成天数 = AVG(completedAt - startedAt)
  按步骤统计流失率：完成 stepN 但未完成 stepN+1 的比例
  序列转化率：分配序列的线索最终转化率 vs 未分配序列的转化率
```

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.sequence.auto-assign-on-qualify` | true | 线索进入 QUALIFIED 后是否自动分配序列 |
| `erp-crm.sequence.grace-period-days` | 2 | 步骤逾期宽限天数 |
| `erp-crm.sequence.max-overdue-steps` | 3 | 连续逾期超过此数提醒负责人 |
| `erp-crm.sequence.default-template` | NEW_LEAD | 默认序列模板类型 |

## 状态机关联

序列进度与 `ErpCrmLead.docStatus` 独立。序列在 `docStatus=QUALIFIED` 后开始，不影响生命周期状态迁移。序列完成后不改变 `docStatus`（仅标记序列进度完成）。

## 反模式警示

- ⛔ **序列步骤硬编码在 Java 枚举中**——步骤和完成条件应通过 `ErpCrmSequenceStep` 表配置，新增模板零代码。
- ⛔ **序列进度混淆在 Lead 状态字段中**——序列进度是独立维度，不应与 `docStatus` 或 `stageId` 耦合。
- ⛔ **将序列分配规则写在代码逻辑中**——`ErpCrmSequenceAssignment` 表管理条件映射，允许销售管理员自行配置。
- ⛔ **步骤完成条件只用 Event 状态判断**——部分完成条件（如 EMAIL_OPENED）需要集成邮件跟踪服务，不能仅依赖 Event.status。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 序列分配到 lead.currentSequenceId，步骤推进回写 |
| CRM（ErpCrmEvent） | 步骤创建 ErpCrmEvent（autoCreateEvent=true）；Event.status 变化触发步骤完成检查 |
| nop-sys（定时任务） | 步骤到期提醒 Job、逾期检查 Job |
| master-data（ErpMdPartner） | 线索来源作为序列分配条件 |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 多步骤自动化序列（cadence/sequence） | 🟢 | SalesLoft `Cadence` + steps；Outreach `Sequence` |
| 步骤完成条件（call/email/meeting） | 🟢 | SalesLoft `CadenceStep.completion_condition` |
| 序列分配规则 | 🟢 | SalesLoft `CadenceAssignmentRule`（list/person match） |
| 序列模板类型（new lead/qualification/negotiation） | 🟢 | Outreach `Sequence` 模板分类 |
| 超时/逾期处理 | 🟢 | Outreach `SequenceStep.wait_days` + 逾期提醒 |
| 序列性能分析 | 🟡 | SalesLoft `Cadence.reporting` 功能 |

## 参考

- `README.md` §ErpCrmLead §ErpCrmEvent（序列推进依赖的核心实体）
- `state-machine.md` §Lead §Event（状态迁移约束）
- `use-cases.md` §UC-CRM-14（序列管理用例）
- `../../analysis/erp-survey/` — SalesLoft/Outreach 序列机制分析
