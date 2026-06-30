# CRM 域 - 营销活动管理

## 目的

定义营销活动（Campaign）的全生命周期管理：活动计划 → 执行 → 效果分析 → 归档闭环。涵盖 UTM 归因追踪、多渠道活动类型、预算与 ROI 计算、线索归因、与 CRM 线索跟踪的集成。

---

## 一、营销活动模型

### 1.1 ErpCrmCampaign（营销活动）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| campaignName | 活动名称 | 🟢 Axelor Campaign.name |
| campaignType | dict `erp-crm/campaign-type`：EMAIL（邮件）/ SOCIAL（社交）/ EVENT（活动）/ CONTENT（内容）/ OTHER（其他） | 🟢 Axelor Campaign.campaignTypeSelect |
| medium | UTM medium（如 email、cpc、social） | 🟢 Odoo utm.mixin |
| source | UTM source（如 newsletter、google、wechat） | 🟢 Odoo utm.mixin |
| startDate/endDate | 活动起止日期 | 🟢 Axelor Campaign.startDate/endDate |
| budgetAmount | 预算金额 | 🟢 Axelor Campaign.budget |
| actualCost | 实际成本（从费用记录归集） | 🟢 Axelor Campaign.actualCost |
| expectedRevenue | 预期收入（关联线索预期收入汇总） | — |
| actualRevenue | 实际收入（关联线索转化后回写） | — |
| ownerId | 活动负责人（→User） | 🟢 Axelor Campaign.user |
| teamId | 执行团队（→ErpCrmTeam） | — |
| status | dict `erp-crm/campaign-status`：PLANNING（计划中）/ ACTIVE（进行中）/ ANALYZING（分析中）/ CLOSED（已归档） | — |
| isTemplate | 是否可复用模板 | 🟢 Odoo utm.campaign |
| 标准审计字段 | | |

### 1.2 活动生命周期状态机

```
PLANNING（计划中）
  ├─ 启动 → ACTIVE（进行中）
  │          ├─ 结束执行 → ANALYZING（分析中）
  │          │               ├─ 生成分析报告 → CLOSED（已归档，终态）
  │          │               └─ 取消归档 → CLOSED（终态）
  │          └─ → CLOSED（直接归档，终态）
  ├─ → CLOSED（计划取消，终态）
  └─ → ANALYZING（一次性活动跳过执行期）
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| PLANNING→ACTIVE | 营销主管 | budgetAmount 必填，至少一种 medium/source 配置 | 活动开始，开始接收 UTM 归因 |
| ACTIVE→ANALYZING | 营销主管 | endDate 已过（或手动结束） | 停止 UTM 新归因，开始 ROI 计算 |
| ANALYZING→CLOSED | 营销主管 | 分析报告已生成 | 终态，数据不可修改 |
| PLANNING→CLOSED | 营销主管 | — | 计划取消，终态 |
| ACTIVE→CLOSED | 营销主管 | — | 直接归档，不进行分析 |

### 1.3 活动类型配置

| 类型 | 特有字段 | UTM 默认值 | 归因方式 |
|------|----------|-----------|----------|
| EMAIL（邮件营销） | templateId, sendingListId, sentCount, openRate, clickRate | medium=email | 邮件链接携带 UTM 参数 |
| SOCIAL（社交媒体） | platform（微信/LinkedIn/Twitter）, postUrl, engagementCount | medium=social | 社交帖子链接携带 UTM 参数 |
| EVENT（活动/展会） | eventLocation, attendeeCount, boothNumber | medium=event | 现场二维码/活动专属落地页 |
| CONTENT（内容营销） | contentUrl, contentType（博客/白皮书/视频）, downloadCount | medium=cpc | 内容页 CTA 跳转携带 UTM 参数 |
| OTHER（其他） | — | — | 手动归因 |

---

## 二、UTM 追踪与归因

### 2.1 UTM 参数模型

UTM 参数是营销活动与线索之间的核心关联纽带。参数在线索创建时预填充，贯穿线索全生命周期。

```
外部渠道（网页表单 / 落地页 / 邮件链接 / 社交帖子）
        │
        ├─► 解析 URL 参数（campaignId, utm_medium, utm_source, utm_term, utm_content）
        │
        ├─► 创建 ErpCrmLead：
        │       campaignId  → 匹配 ErpCrmCampaign.id
        │       medium      → utm_medium（或取 campaign.medium 默认值）
        │       source      → utm_source（或取 campaign.source 默认值）
        │       utmTerm     → utm_term（搜索关键词，可选）
        │       utmContent  → utm_content（创意版本，可选）
        │
        └─► 写入 lead 的 campaignId / medium / source / utmTerm / utmContent
```

### 2.2 UTM 字段（ErpCrmLead 扩展）

| 字段 | 含义 | 对应 UTM 参数 |
|------|------|---------------|
| campaignId | 活动（→ErpCrmCampaign） | —（自有 ID，非 UTM 标准） |
| medium | 媒介 | utm_medium |
| source | 来源 | utm_source |
| utmTerm | 付费关键词 | utm_term |
| utmContent | 创意版本（A/B 测试标识） | utm_content |

> 参考 🟢 Odoo `crm_lead.py:24-28`（campaign_id/medium_id/source_id 三字段）+ `utm.mixin`（支持 utm_term/utm_content）。

### 2.3 归因规则

| 规则 | 说明 |
|------|------|
| 首次触点归因（默认） | 线索首个关联的活动获得 100% 归因 |
| 最后触点归因 | 线索转化前最后一个关联的活动获得 100% 归因 |
| 多点归因（可选深化） | 线索生命周期内所有关联活动按时间权重分配 |
| 自定义归因（可选深化） | 营销主管手动分配合比例（总和=100%） |

**首次触点归因实现**：

```
Lead 创建时 campaignId 记录来源活动 → 转化后回写 actualRevenue
    → 归因报表：SELECT campaignId, count(leadId), sum(actualRevenue)
      FROM ErpCrmLead WHERE relatedBillCode IS NOT NULL
      GROUP BY campaignId
```

---

## 三、预算与 ROI 计算

### 3.1 预算追踪

| 概念 | 计算方式 | 来源 |
|------|----------|------|
| 预算（budgetAmount） | 活动创建时填写 | 人工录入 |
| 实际成本（actualCost） | 可通过费用报销归集（可选）或手动录入 | 财务域费用单 / 手动输入 |
| 预期收入（expectedRevenue） | SUM(关联线索.expectedRevenue) | 自动汇总 |
| 实际收入（actualRevenue） | SUM(已转化线索.expectedRevenue) | 自动汇总（转化时回写） |

### 3.2 ROI 计算公式

| 指标 | 公式 | 用途 |
|------|------|------|
| ROI | (actualRevenue - actualCost) / actualCost × 100% | 衡量投资回报率 |
| CPA（单客户获取成本） | actualCost / 转化线索数 | 衡量获客效率 |
| CPL（单线索成本） | actualCost / 线索总数 | 衡量线索获取成本 |
| 转化率 | 转化线索数 / 线索总数 × 100% | 衡量线索质量 |
| 赢单率 | 赢单线索数 / 转化线索数 × 100% | 衡量销售跟进效率 |

### 3.3 ROI 计算时机

```
活动状态 → ANALYZING
        │
        ├─► 汇总活动期间所有关联线索
        │
        ├─► 计算：
        │       actualCost（从费用单或手动录入汇总）
        │       actualRevenue（关联线索中 relatedBillCode 非空的总和）
        │       ROI = (actualRevenue - actualCost) / actualCost
        │
        └─► 写入 ErpCrmCampaign：
                actualCost = 汇总值
                actualRevenue = 汇总值
                roi = 计算值
```

---

## 四、线索→活动归因

### 4.1 归因关联模型

```
ErpCrmCampaign (1) ←── (N) ErpCrmLead
    │                        │
    │                        ├─ campaignId → 关联活动
    │                        ├─ medium/source → 活动媒介/来源（冗余加速报表查询）
    │                        └─ utmTerm/utmContent → 细粒度 UTM 参数
    │
    └─ 活动中创建的线索计入活动归因报表
```

### 4.2 归因报表结构

| 报表 | 用途 | 数据源 |
|------|------|--------|
| 活动归因汇总 | 各活动带来的线索数、转化数、收入 | ErpCrmLead GROUP BY campaignId |
| UTM 媒介分析 | 不同渠道的 ROI 对比 | ErpCrmLead GROUP BY medium |
| UTM 来源分析 | 不同来源的线索质量和转化率 | ErpCrmLead GROUP BY source |
| 活动趋势 | 各活动线索数随时间变化 | ErpCrmLead.createDate + campaignId |
| 转化漏斗 | 线索 → 商机 → 报价单各阶段转化率 | ErpCrmLead.docStatus + stageId |

### 4.3 归因查询示例

```
-- 活动 ROI 报表
SELECT
    c.id,
    c.campaignName,
    c.budgetAmount,
    c.actualCost,
    count(l.id) AS totalLeads,
    sum(CASE WHEN l.relatedBillCode IS NOT NULL THEN 1 ELSE 0 END) AS convertedLeads,
    sum(l.expectedRevenue) AS expectedRevenue,
    sum(CASE WHEN l.relatedBillCode IS NOT NULL THEN l.expectedRevenue ELSE 0 END) AS actualRevenue
FROM ErpCrmCampaign c
LEFT JOIN ErpCrmLead l ON l.campaignId = c.id
WHERE c.status = 'CLOSED' OR c.status = 'ANALYZING'
GROUP BY c.id
```

---

## 五、与 CRM 线索跟踪的集成

### 5.1 线索创建时归因

| 触发场景 | 归因方式 |
|----------|----------|
| 客户通过活动落地页提交表单 | URL 解析 UTM → 自动填充 campaignId/medium/source |
| 销售员手动创建线索 | 可选选择活动 → 手动关联 campaignId |
| 邮件营销链接点击 | 邮件链接携带 UTM → 自动归因 |
| 社交媒体帖子转化 | 社交帖子链接携带 UTM → 自动归因 |

### 5.2 活动期间线索跟踪

```
活动 ACTIVE 期间
        │
        ├─► 新线索创建时自动归因 medim/source
        │
        ├─► 线索状态变化（stageId 前移）实时更新
        │
        ├─► 线索转化后 actualRevenue 自动累加到活动
        │
        └─► 活动 dashboard 实时更新：
                线索数 / 商机数 / 转化数 / 收入
```

### 5.3 活动归档后的数据冻结

活动进入 CLOSED 后：
- 不再接受新的 UTM 归因
- 历史归因数据不可修改（只读）
- 报表数据冻结作为分析基准

---

## 六、活动模板（可选项）

### 6.1 复用模板

| 场景 | 模板内容 |
|------|----------|
| 月度邮件营销 | medium=email, campaignType=EMAIL, 预算模板, 目标受众 |
| 季度展会活动 | medium=event, campaignType=EVENT, 标准展会物料清单 |
| 产品发布 | medium=social, campaignType=SOCIAL, 多平台发布计划 |

### 6.2 模板与实例的关系

```
ErpCrmCampaign（isTemplate=true）
        │
        └─► 复制模板 → 新建 ErpCrmCampaign（isTemplate=false）
                修改活动名称、时间、预算等实例字段
```

---

## 七、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| 营销活动生命周期 + 预算/ROI | 🟢 | Axelor `axelor-marketing` 模块（9 Java 文件）：Campaign 实体 + 预算追踪 + ROI 计算 |
| UTM 归因（campaign/medium/source） | 🟢 | Odoo `utm.mixin` + `crm_lead.py:24-28` |
| 多活动类型（EMAIL/SOCIAL/EVENT） | 🟡 | Axelor Campaign.campaignTypeSelect（EMAIL/SOCIAL/EVENT/WEBINAR/POST/OTHER） |
| 首次/最后触点归因 | 🟡 | 业界通用归因模型 |
| 线索→活动关联 + 归因报表 | 🟢 | Odoo `crm_lead_report.py`（按 campaign/medium/source 分组报表） |
| 活动模板复用 | ⚪ | 本项目设计意图，Axelor 无直接对应 |
| 多点归因（权重分配） | ⚪ | 可选深化项，首次发布不实现 |

## 八、跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 活动归因线索 → 线索携带 campaignId/medium/source |
| sales（ErpSalQuotation） | 线索转化后 actualRevenue 回写活动 |
| master-data | 无直接依赖 |
| nop-sys（定时任务） | 活动到期自动结束（endDate 检查 Job） |

## 参考

- `state-machine.md`（Lead 状态机，线索转化流程）
- `use-cases.md` §UC-CRM-07（UTM 归因用例）
- `README.md` §ErpCrmCampaign §ErpCrmLead
- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §CRM
