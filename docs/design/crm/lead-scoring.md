# CRM 域 - 线索评分（Lead Scoring）

## 目的

设计线索评分系统：基于多维评分准则对线索进行量化评分，根据总分自动触发线索状态变更（如高分自动转商机），辅助销售员优先跟进高价值线索。评分规则支持管理员配置，评分历史可追溯。

## 边界

- 本模块负责：评分准则配置、评分引擎计算、评分阈值 → 自动线索状态变更、评分历史查询。
- 评分引擎是 CRM `ErpCrmLead` 的支撑子域——引用线索数据（来源/行业/公司规模/互动行为），计算结果写回 `lead.score` 并可选自动推进 `docStatus`。
- 本模块不负责：线索主数据（`ErpCrmLead`）；营销活动归因（`marketing.md`）；漏斗阶段管理（`README.md` §ErpCrmStage）；事件互动数据采集（`ErpCrmEvent`）。

## 设计依据

> 参考 **Odoo CRM** lead scoring（`crm_lead.py` 的 `priority`/`rating` 字段 + `crm.lead.scoring` 模型）：基于来源、活动、邮件互动等多维度加权评分，高分线索自动标记为优先。
>
> 参考 **ERPNext CRM** lead scoring（`crm/doctype/lead_scoring_criteria/` 可配置准则 + `lead_scoring_result/` 历史记录）：评分准则维护在独立 Doctype，执行结果存历史表。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmLeadScoreConfig（评分规则配置头）

管理员可配置的评分规则集。支持多规则版本（同一时间只有一个生效）。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | — |
| configName | 规则集名称（如"标准评分规则 v2"） | 🟢 ERPNext `lead_scoring_criteria` |
| isActive | 是否当前生效（同一时间仅一个 active） | — |
| effectiveFrom/effectiveTo | 生效起止日期（null 表示长期） | — |
| autoQualifyThreshold | 自动转商机阈值（总分超过后 `docStatus` 自动推进） | 🟢 Odoo `crm_lead.scoring` 自动标记 |
| minScoreForFollowUp | 最低跟进建议分数（低于此分暂不跟进） | — |
| 标准审计字段 | | |

### ErpCrmLeadScoreConfigLine（评分准则明细行）

每个准则定义一种评分维度及其权重、打分方式。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/configId/orgId | 标准 | — |
| criterionCode | 准则编码（`SOURCE_WEIGHT` / `ENGAGEMENT_SCORE` / `COMPANY_SIZE` / `INDUSTRY_WEIGHT` / `BUDGET_RANGE` / `JOB_TITLE`） | 🟢 ERPNext `lead_scoring_criteria` |
| criterionName | 准则名称（中文描述） | — |
| weight | 权重系数（0-100，同一配置内 Σweight 不强制等于 100，输出时归一化） | 🟢 ERPNext `.weight` |
| scoringMethod | dict `erp-crm/scoring-method`：LOOKUP（查值表映射得分）/ FORMULA（公式计算）/ BOOLEAN（是否匹配） | — |
| lookupTable | JSON 值表（如 `[{"value":"technology","score":20},{"value":"manufacturing","score":15}]`） | 🟢 Odoo `crm.lead.scoring` 值映射 |
| formula | 公式表达式（保留给规则引擎扩展，首次发版建议用 LOOKUP 覆盖主要场景） | ⚪ 预留扩展 |
| maxScore | 该准则最高分（归一化用） | — |
| sequence | 排序 | — |

**预置准则说明**：

| 准则编码 | 含义 | scoringMethod | lookupTable 示例 |
|----------|------|---------------|-------------------|
| SOURCE_WEIGHT | 线索来源权重 | LOOKUP | 广告推广=20, 老客户推荐=30, 官网留资=15, 行业展会=25 |
| ENGAGEMENT_SCORE | 互动活跃度（事件数加权） | FORMULA | count(email)×3 + count(meeting)×5 + count(call)×2（上限 30） |
| COMPANY_SIZE | 公司规模档次 | LOOKUP | 1-10人=5, 11-50人=10, 51-200人=15, 201-1000人=20, 1000+=25 |
| INDUSTRY_WEIGHT | 行业权重 | LOOKUP | 制造业=15, 金融=20, 医疗=18, 零售=12, 其他=10 |
| BUDGET_RANGE | 预算范围匹配 | LOOKUP | <10万=5, 10-50万=10, 50-100万=15, 100万+=20 |
| JOB_TITLE | 职位层级 | LOOKUP | C-level=15, 总监=10, 经理=5, 专员=2 |

> 参考 🟢 Odoo `crm_lead_score` 模型：`score_total`（总分）/ `scoring_criteria_ids`（多对多准则）。

### ErpCrmLeadScore（线索评分记录头）

每次评分计算生成一条记录，关联线索 + 使用的评分规则版本。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/leadId/orgId | 标准 | — |
| configId | 使用的评分规则版本（→ErpCrmLeadScoreConfig，追溯评分口径） | 🟢 ERPNext `lead_scoring_result` |
| totalScore | 总分（Σ 行级加权得分，归一化到 0-100） | 🟢 Odoo `crm_lead.score_total` |
| scoreBreakdown | JSON 快照：各准则原始得分 + 加权得分（冗余加速展示） | — |
| autoQualified | 是否触发自动转商机（totalScore ≥ autoQualifyThreshold） | — |
| triggeredAction | 触发动作：NONE / AUTO_QUALIFY / NOTIFY_OWNER | — |
| calculatedAt | 计算时间（实际执行时间戳） | — |
| triggerEvent | 触发计算的事件：MANUAL（手动重新评分）/ LEAD_UPDATE（线索字段变更）/ SCHEDULED（定时批量） | — |
| 标准审计字段 | | |

### ErpCrmLeadScoreLine（评分记录明细行）

| 字段 | 含义 |
|------|------|
| id/scoreId/orgId | 标准 |
| configLineId | 对应准则行（→ErpCrmLeadScoreConfigLine） |
| criterionCode | 冗余准则编码（快照，防止历史 configLine 被修改后追溯失真） |
| criterionName | 冗余准则名称 |
| rawValue | 从线索/事件中提取的原始值（如 companySize=50） |
| lookupValue | 匹配到的档次标签（如"51-200人"） |
| rawScore | 原始得分（查表或公式计算的分数） |
| weightedScore | 加权得分（= rawScore × weight / 100） |
| sequence | 排序 |

## 业务规则

1. **评分计算不出域**：所有评分所需数据均在 CRM 域内（`ErpCrmLead` + `ErpCrmEvent`），不跨域读取。
2. **总分归一化**：`totalScore = Σ(weightedScore) / Σ(maxPossibleWeightedScore) × 100`，保证输出 0-100。
3. **评分触发的时机**：
   - 手动触发（销售员点击"重新评分"）
   - 线索字段变更（`sourceId` / `companyName` / `expectedRevenue` / `industry` 等评分相关字段更新后异步触发）
   - 定时批量（nop-job 每日扫描 `docStatus=NEW` 未评分的线索）
4. **自动转商机（AUTO_QUALIFY）**：
   ```
   线索评分完成 →
     if totalScore >= autoQualifyThreshold 且 lead.leadType == LEAD 且 lead.docStatus == NEW →
       自动执行:
         lead.docStatus → QUALIFIED
         lead.score = totalScore
         记录 ErpCrmLeadScore.autoQualified=true, triggeredAction=AUTO_QUALIFY
         通知销售负责人
   ```
5. **阈值可覆盖**：同一阈值应用于所有 LEAD，但管理员可在 `ErpCrmLeadScoreConfig` 中调整 `autoQualifyThreshold`。
6. **评分历史只追加**：每次评分生成新的 `ErpCrmLeadScore` + 行记录，不覆盖旧记录。线索当前分数取最新一条 `calculatedAt` 的记录。
7. **配置变更不影响历史评分**：`ErpCrmLeadScore` 行级冗余 `criterionCode`/`criterionName`/`rawScore`/`weightedScore`，保证历史评分可追溯至原始配分。
8. **同一时间仅一个评分规则生效**：`isActive=true` 的唯一性约束由业务层保证，切换规则时旧线索不重新评分（保留历史口径）。

### 评分计算流程

```
触发（手动/字段变更/定时）
    │
    ├─ 加载 ErpCrmLeadScoreConfig（isActive=true）
    │
    ├─ 遍历 configLines（按 sequence）：
    │     │
    │     ├─ LOOKUP：从线索取对应字段值，查 lookupTable 取 rawScore
    │     │
    │     ├─ FORMULA：按 formula 计算（预留规则引擎）
    │     │
    │     └─ BOOLEAN：匹配得 maxScore，不匹配得 0
    │
    ├─ 计算 totalScore（归一化）
    │
    ├─ 创建 ErpCrmLeadScore + ErpCrmLeadScoreLine
    │
    ├─ if totalScore >= autoQualifyThreshold 且满足条件 →
    │     自动 QUALIFY 线索（docStatus → QUALIFIED）
    │
    └─ 更新 lead.score 为本次 totalScore
```

### 阈值→动作矩阵

| 分数区间 | 动作 | 说明 |
|----------|------|------|
| totalScore >= autoQualifyThreshold | AUTO_QUALIFY | 自动转商机（NEW→QUALIFIED） |
| autoQualifyThreshold > totalScore >= minScoreForFollowUp | NOTIFY_OWNER | 通知销售优先跟进（不改 docStatus） |
| totalScore < minScoreForFollowUp | — | 暂不跟进，在列表中标记"低价值" |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.lead-scoring.auto-qualify` | true | 是否启用自动转商机（开关） |
| `erp-crm.lead-scoring.schedule-cron` | 0 2 * * * | 定时批量评分 cron（每日凌晨 2 点） |
| `erp-crm.lead-scoring.recalc-on-lead-update` | true | 线索字段变更是否触发自动重新评分 |

## 状态机关联

线索评分不引入新的状态机。评分引擎通过 `docStatus` 联动 `state-machine.md` §Lead：

```
评分触发条件：
  leadType == LEAD, docStatus == NEW
    评分结果：
      totalScore >= threshold →
        docStatus: NEW → QUALIFIED（复用 state-machine.md §Lead NEW→QUALIFIED）
        自动设 stageId 为第一个漏斗阶段
```

- 已 QUALIFIED / CONVERTED / LOST / CANCELLED 的线索不再参与自动评分（手动重新评分除外）。

## 反模式警示

- ⛔ **评分逻辑硬编码 Java 常量**——评分维度、权重、值表通过 `ErpCrmLeadScoreConfig` + `ErpCrmLeadScoreConfigLine` 可配置，零改代码。
- ⛔ **每次评分覆盖旧记录**——每次创建新评分记录，不 UPDATE 旧记录（查询 `lead.score` 取最新一条）。
- ⛔ **评分跨域读取采购/财务数据**——评分不依赖跨域，降低延迟与耦合。
- ⛔ **实时评分阻塞界面操作**——评分异步执行（字段变更触发事件，eventual 计算），不阻塞用户保存/提交。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 评分引擎读取线索字段，回写 lead.score |
| CRM（ErpCrmEvent） | ENGAGEMENT_SCORE 从关联事件计数 |
| nop-sys（定时任务） | 定时批量评分 Job |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 可配置评分准则（权重+值表） | 🟢 | ERPNext `lead_scoring_criteria` Doctype 源码实测 |
| 评分结果历史表 | 🟢 | ERPNext `lead_scoring_result` Doctype 源码实测 |
| 高分自动转商机 | 🟡 | Odoo `crm_lead.score_total` 自动标记 priority, 但无硬编码 threshold |
| 互动评分（事件/邮件） | 🟢 | Odoo `crm_lead_score` 基于活动事件自动评分 |
| LOOKUP 值表映射 | 🟢 | ERPNext `.scoring_field` + `.score`（按字段值查表打分） |
| 管理员可配 + 多版本 | 🟡 | ERPNext 单一 Doctype 未严格做版本；多版本为本项目设计意图 |
| 评分历史可追溯（行级快照） | ⚪ | 本项目设计意图，ERPNext 无独立明细行 |

## 参考

- `state-machine.md` §Lead（评分触发 docStatus 迁移）
- `README.md` §ErpCrmLead §ErpCrmEvent（评分数据源）
- `marketing.md`（活动归因, 评分间接关联）
- `docs/design/crm/README.md` §业务规则 4（事件提醒 Job）
- `docs/analysis/erp-survey/` — Odoo/ERPNext CRM 评分源码分析
