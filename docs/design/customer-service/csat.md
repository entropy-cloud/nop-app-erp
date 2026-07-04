# 客服工单域 — 客户满意度调查（CSAT）

## 目的

工单解决后自动/手动发起满意度调查，收集客户评分（CSAT/NPS/CES）与反馈意见，量化服务质量、驱动持续改进。

---

## 一、模型设计

### 1.1 ErpCsSurvey（满意度调查）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/orgId | 标准 | |
| ticketId | 关联工单（→ErpCsTicket） | 唯一（一工单一调查） |
| surveyToken | 调查链接令牌 | 无鉴权访问，UUID |
| csatScore | CSAT 评分（1-5） | 1=非常不满意 → 5=非常满意 |
| npsScore | NPS 评分（0-10） | 0=绝不推荐 → 10=极力推荐 |
| cesScore | CES 评分（1-7） | 1=非常困难 → 7=非常容易 |
| comment | 文字反馈意见 | large=true |
| respondedAt | 客户响应时间 | |
| surveySentAt | 调查发送时间 | |
| surveyChannel | 发送渠道（EMAIL/PHONE/PORTAL/CHAT） | dict |
| 标准审计字段 | | |

### 1.2 评分映射

```
CSAT（客户满意度）
        1  ←——— 非常不满意
        2  ←——— 不满意
        3  ←——— 一般
        4  ←——— 满意
        5  ←——— 非常满意

NPS（净推荐值）
        0 1 2 3 4 5 6  ←——— 贬损者（Detractor）
        7 8            ←——— 被动者（Passive）
        9 10           ←——— 推荐者（Promoter）

CES（客户费力度）
        1 2  ←——— 非常困难
        3 4 5 ←——— 一般
        6 7  ←——— 非常容易
```

---

## 二、调查触发与发送

### 2.1 触发时机

| 触发条件 | 说明 |
|----------|------|
| 工单 → RESOLVED（默认） | 解决后立即触发 |
| 工单 → CLOSED（可选） | 关闭时触发，适合最终确认后才做调查的场景 |
| 处理人手动触发 | 客服主动发送调查链接（如客户要求） |

### 2.2 延迟发送

```
RESOLVED 状态 → 延迟 X 小时后发送 → 客户收到调查
        │
        ├─► erp-cs.survey-send-delay=0（立即发送，默认）
        ├─► erp-cs.survey-send-delay=24（延迟 24 小时）
        └─► 延迟期间工单重回 IN_PROGRESS → 取消该次调查
```

延迟发送的目的：避免客户未充分体验解决方案即评分；给时间验证解决效果。

### 2.3 发送渠道

| 渠道 | 触发条件 | 表现形式 |
|------|----------|----------|
| EMAIL | 客户有邮箱地址 | 发送含 surveyToken 的调查链接邮件 |
| PORTAL | 客户自服务门户 | 门户主页弹窗/待办项 |
| CHAT | 实时聊天解决 | 聊天窗口关闭时弹出评分 |
| PHONE | 电话客服 | 短信/IVR 链接 |

### 2.4 调查链接

```
格式：{portal-url}/cs/survey/{surveyToken}

页面内容：
├─ CSAT 评分（5星）
├─ NPS 评分（0-10）
├─ CES 评分（7级）
└─ 可选文字反馈

提交后 → 更新 ErpCsSurvey.respondedAt
```

---

## 三、调查生命周期

### 3.1 状态机

```
PENDING（待发送） → SENT（已发送未响应） → COMPLETED（客户已提交）
        │                                        │
        └─ 发送失败 → FAILED ───────────────────────┘
```

| 状态 | 含义 |
|------|------|
| PENDING | 工单 RESOLVED，等待延迟时间到达 |
| SENT | 调查已发出，等待客户响应 |
| COMPLETED | 客户已提交评分（终态） |
| FAILED | 发送失败（邮箱无效/渠道不可达） |

### 3.2 超时与提醒

| 事件 | 动作 |
|------|------|
| SENT 后 48h 未响应 | 自动发送一次提醒 |
| SENT 后 7d 未响应 | 标记为 EXPIRED，不再提醒 |
| 客户点击链接超时 | 链接有效期 30 天，过期后提示重新发送 |

---

## 四、评分聚合与报表

### 4.1 关键指标

| 指标 | 计算方式 | 用途 |
|------|----------|------|
| CSAT 平均分 | AVG(csatScore) | 整体满意度趋势 |
| CSAT 达标率 | (csatScore >= 4 的数量) / 总调查数 | 服务质量达标考核 |
| NPS | (推荐者数 - 贬损者数) / 总调查数 × 100 | 客户忠诚度 |
| CES 平均分 | AVG(cesScore) | 服务易用性衡量 |
| 回复率 | COMPLETED 数 / (SENT 数 + COMPLETED 数) | 调查参与度 |

### 4.2 报表分类

| 报表 | 内容 | 周期 | 受众 |
|------|------|------|------|
| CSAT 月报 | 月 CSAT 平均分、达标率、回复率 | 月度 | 客服经理 |
| NPS 趋势 | 季度 NPS 走势、推荐者/贬损者分布 | 季度 | 客服总监 |
| CES 按工单类型 | 各 ticketType 的 CES 对比 | 月度 | 流程优化 |
| 客服个人 CSAT | 按 agentId 分组平均 CSAT | 月度 | 绩效评估 |
| 文本反馈分析 | 负面评论（csatScore <= 2）关键词提取 | 月度 | 服务质量改进 |

### 4.3 查询示例

```
-- CSAT 月报
SELECT
    DATE_TRUNC('month', t.createdDate) AS month,
    round(avg(s.csatScore), 2) AS avgCsat,
    count(s.id) AS totalSurveys,
    sum(CASE WHEN s.csatScore >= 4 THEN 1 ELSE 0 END) AS satisfied,
    round(
        sum(CASE WHEN s.csatScore >= 4 THEN 1 ELSE 0 END)::decimal
        / nullif(count(s.id), 0), 2
    ) AS satisfactionRate
FROM ErpCsSurvey s
JOIN ErpCsTicket t ON t.id = s.ticketId
WHERE s.respondedAt IS NOT NULL
GROUP BY DATE_TRUNC('month', t.createdDate)
ORDER BY month DESC

-- 客服个人 CSAT
SELECT
    t.assignedToId,
    count(s.id) AS total,
    round(avg(s.csatScore), 2) AS avgCsat
FROM ErpCsSurvey s
JOIN ErpCsTicket t ON t.id = s.ticketId
WHERE s.respondedAt IS NOT NULL
  AND t.assignedToId IS NOT NULL
GROUP BY t.assignedToId
ORDER BY avgCsat DESC
```

---

## 五、调查配置管理

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.survey-enabled` | true | 是否启用满意度调查 |
| `erp-cs.survey-trigger-status` | RESOLVED | 触发调查的工单状态（RESOLVED/CLOSED） |
| `erp-cs.survey-send-delay` | 0（小时） | 触发后延迟发送时间 |
| `erp-cs.survey-reminder-hours` | 48 | 未响应提醒延迟（小时） |
| `erp-cs.survey-expire-days` | 7 | 调查链接有效期（天） |
| `erp-cs.survey-csat-enabled` | true | 启用 CSAT 评分 |
| `erp-cs.survey-nps-enabled` | false | 启用 NPS 评分 |
| `erp-cs.survey-ces-enabled` | false | 启用 CES 评分 |

---

## 六、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| Odoo helpdesk satisfaction survey | 🟢 | `helpdesk.ticket` → `satisfaction` 字段 + `satisfaction_value` |
| Zendesk CSAT | 🟢 | 工单解决后自动发送调查 + 5 星评分 + 文字反馈 |
| ServiceNow satisfaction survey | 🟢 | survery trigger → survey response aggregation |
| NPS 方法论 | 🟡 | 0-10 分 → 推荐者/被动者/贬损者分类 |
| CES 方法论 | 🟡 | 客户费力度 1-7 级评分 |

## 七、跨域协作

| 对端 | 协作方式 |
|------|---------|
| 工单（ErpCsTicket） | 调查关联工单，RESOLVED 触发调查创建 |
| 通知（nop-sys） | 发送调查链接邮件/站内信 |
| 客服绩效（HR） | CSAT 作为客服绩效 KPI 输入 |

## 实现偏离补注（2026-07-04 实现）

> 权威计划：`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`。本节相对 §1-3 设计的实现取舍。

- **§1.1 / §3.1 调查状态持久化**：`ErpCsSurvey` ORM 无独立 `status` 列；状态由时间戳派生——PENDING=`surveySentAt` 空 / SENT=`surveySentAt` 非空且 `respondedAt` 空 / COMPLETED=`respondedAt` 非空。FAILED/EXPIRED 仅查询期判定不持久。扩 `status` 列归 Non-Goal。
- **§1.2 NPS 分类**：9-10 推荐者 / 7-8 被动者 / 0-6 贬损者经 `NpsClassifier` 派生，**不持久化**（ORM 无分类列）；报表需按分类聚合时运行时计算或扩列。
- **§2.1 触发时机**：`resolve` 动作成功后（config-gated `survey-enabled` + `survey-trigger-status` 默认 RESOLVED）自动调 `createSurvey`。
- **§2.2 延迟发送**：`survey-send-delay>0` 时 `surveySentAt` 留空（状态 PENDING），实际延迟发送由 nop-job 接线（cron 注册归 Non-Goal）；`delay=0`（默认）立即置 `surveySentAt=now`（状态 SENT）。
- **§2.3 发送渠道**：`createSurvey` 默认 `surveyChannel=PORTAL`；实际邮件/门户渲染/发送归 nop-notification 独立面。
- **§2.4 调查链接**：`surveyToken` 为 UUID（无鉴权访问），链接格式 `{portal-url}/cs/survey/{token}` 由前端渲染；本计划仅交付 token 生成与提交逻辑。
- **§3.2 超时提醒**：`findSurveyReminders(reminderHours)` / `findExpiredSurveys(expireDays)` 查询方法交付；cron 实际注册归 Non-Goal。
- **reopen 取消调查**：工单 RESOLVED→IN_PROGRESS（reopen）时，若调查未响应（`respondedAt` 空）则删除，避免误发。
- **配置默认值**：`erp-cs.survey-enabled=true`、`erp-cs.survey-trigger-status=RESOLVED`、`erp-cs.survey-send-delay=0`（小时）、`erp-cs.survey-csat-enabled=true`、`erp-cs.survey-nps-enabled=false`、`erp-cs.survey-ces-enabled=false`、`erp-cs.survey-reminder-hours=48`、`erp-cs.survey-expire-days=7`。
