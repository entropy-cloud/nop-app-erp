# 客服工单域 - SLA 策略与计时管理

## 目的

详细说明客服工单 SLA（Service Level Agreement）策略的定义、计时机制、超时检测与升级链。涵盖工作时间 vs 日历小时、多级升级配置、SLA 绩效报表。

---

## 一、SLA 策略模型

### 1.1 ErpCsSlaPolicy（SLA 策略）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | 🟢 Axelor Sla |
| ticketTypeId | 适用工单类型（→ErpCsTicketType） | 🟢 Axelor Sla.ticketType |
| minPriority | 最低触发优先级（LOW/NORMAL/HIGH/URGENT） | 🟢 Axelor Sla.prioritySelect |
| teamId | 适用团队（→ErpCsTeam） | 🟢 Axelor Sla.team |
| resolveHours | 解决时限（小时数） | 🟢 Axelor Sla.hours |
| resolveDays | 解决时限（天数） | 🟢 Axelor Sla.days |
| isWorkingDays | 是否仅计算工作日（true=排除周末/节假日） | 🟢 Axelor Sla.isWorkingDays |
| workingHourStart | 工作时间起始（如 09:00，isWorkingDays=true 时生效） | — |
| workingHourEnd | 工作时间结束（如 18:00，isWorkingDays=true 时生效） | — |
| escalationUserId | 一级升级通知人（超时后通知） | — |
| secondEscalationUserId | 二级升级通知人（一级升级未响应后） | — |
| escalationDelayHours | 升级触发后等待小时数（一级→二级） | — |
| isActive | 是否激活 | — |
| description | 说明 | 🟢 Axelor Sla.description |

### 1.2 SLA 策略匹配规则

```
工单创建 / 优先级变更
        │
        ├─► 查询 ErpCsSlaPolicy 匹配条件：
        │       ticketTypeId = 工单.ticketTypeId
        │       AND minPriority <= 工单.priority
        │       AND (teamId = 工单.teamId OR teamId IS NULL)
        │       AND isActive = true
        │
        ├─► 按优先级从高到低排序（最精确匹配优先）
        │
        └─► 取第一条匹配策略 → 写入工单.slaPolicyId
```

**匹配优先级**：

| 优先级 | 匹配条件 | 示例 |
|--------|----------|------|
| 精确匹配 | ticketType + priority + team | 故障+URGENT+技术支持组 |
| 类型+团队 | ticketType + team（不限 priority） | 故障+技术支持组 |
| 类型+优先级 | ticketType + priority（不限 team） | 故障+URGENT |
| 类型兜底 | ticketType（不限 priority/team） | 故障 |

### 1.3 deadlineDateTime 计算

```
deadlineDateTime = now + resolveHours（日历小时模式）
    或 = 计算下一个工作日的累计 resolveHours（工作日模式）

日历小时模式 (isWorkingDays=false)：
    deadlineDateTime = now + resolveHours 小时
    示例：14:00 创建，resolveHours=8 → deadlineDateTime=当日 22:00

工作日模式 (isWorkingDays=true)：
    仅计入 workingHourStart ~ workingHourEnd 范围内的小时
    示例：
        周五 16:00 创建，resolveHours=4，工作时间 09:00~18:00
        → 周五还剩 2h(16:00~18:00) + 周一需再 2h(09:00~11:00)
        → deadlineDateTime = 周一 11:00
```

---

## 二、SLA 计时生命周期

### 2.1 计时起止点

```
工单状态变迁与 SLA 时间线：

NEW ──────────────────────────────────────────────
 │ 创建：SLA 开始计时，deadlineDateTime 写入
 │
ASSIGNED ─────────────────────────────────────────
 │
IN_PROGRESS ───────────────────────────────────────
 │ startDateTime = 首次进入 IN_PROGRESS 的时间
 │ 暂停计时场景（可选）：
 │   - 等待客户补充信息 → 计时暂停
 │   - 客户恢复信息 → 计时恢复
 │
RESOLVED ─────────────────────────────────────────
 │ SLA 停止计时
 │ duration = RESOLVED时间 - startDateTime（扣除暂停时间）
 │ isSlaCompleted = (RESOLVED时间 <= deadlineDateTime)
 │
CLOSED / CANCELLED（终态）
```

### 2.2 计时暂停与恢复（Pending 机制）

| 场景 | 计时行为 |
|------|----------|
| 处理人标记"等待客户回复" | SLA 暂停计时，工单进入 PENDING 子状态 |
| 客户回复/处理人取消 PENDING | SLA 恢复计时，deadlineDateTime 顺延暂停时长 |
| 处理人标记"等待第三方" | SLA 暂停计时 |
| 第三方反馈/取消 PENDING | SLA 恢复计时 |

**实现方式**：

```
ErpCsTicketSlaPause（SLA 暂停记录）
├── ticketId → ErpCsTicket
├── pauseStartDateTime
├── pauseEndDateTime（null 表示暂停中）
├── pauseReason（dict：AWAITING_CUSTOMER / AWAITING_THIRD_PARTY）
└── operatorId

总暂停时长 = SUM(pauseEndDateTime - pauseStartDateTime)
有效处理时长 = duration - 总暂停时长
```

### 2.3 暂停对 deadline 的影响

```
deadlineDateTime（原始） = 创建时按策略计算的截止时间
adjustedDeadlineDateTime = deadlineDateTime + 总暂停时长

SLA 超时判定依据 adjustedDeadlineDateTime
```

---

## 三、SLA 超时与升级

### 3.1 超时检测机制

```
nop-job 定时扫描（默认频率：每分钟）
        │
        └─► SELECT * FROM ErpCsTicket
            WHERE status IN ('ASSIGNED', 'IN_PROGRESS')
              AND adjustedDeadlineDateTime < now()
              AND isSlaCompleted = false
              AND lastEscalationLevel < 2
        │
        ├─► 创建 ErpCsTicketAction（actionType=ESCALATE）
        │
        ├─► 通知 slaPolicy.escalationUserId（一级升级）
        │
        ├─► 更新工单.lastEscalationLevel = 1
        │
        └─► 若 2h 后仍无响应（escalationDelayHours）：
                ├─► 通知 slaPolicy.secondEscalationUserId（二级升级）
                └─► 更新工单.lastEscalationLevel = 2
```

### 3.2 升级链配置

| 级别 | 通知对象 | 触发条件 | 通知方式 |
|------|----------|----------|----------|
| 一级（L1） | slaPolicy.escalationUserId | deadline 超时 | 站内信 + 邮件 |
| 二级（L2） | slaPolicy.secondEscalationUserId | 一级升级后 escalationDelayHours 未解决 | 站内信 + 邮件 + SMS |
| 三级（L3，可选） | 客服总监（硬编码或系统配置） | 二级升级后 escalationDelayHours 仍未解决 | 全部通道 + 升级会议 |

### 3.3 超时后操作

| 操作 | 说明 |
|------|------|
| 重新分派 | 客服主管将工单转派给其他处理人（保留原 SLA 计时数据） |
| 延长 deadline | 管理员手动延长 adjustedDeadlineDateTime（记录原因，计入审计） |
| 标记超时原因 | 处理人填写超时原因（如"等待供应商补件"），计入 SLA 报表 |
| 忽略超时 | 仅限管理员操作，标记 isSlaOverridden=true + 原因 |

### 3.4 超时前预警

```
nop-job 在 deadline 前 1h / 30min 发送预警通知给处理人
        │
        ├─► 查询 adjustedDeadlineDateTime BETWEEN now AND now+1h
        │
        ├─► 发送预警通知：工单编号 + 剩余时间 + 处理人
        │
        └─► 记录 warningSentAt（避免重复发送）
```

---

## 四、SLA 绩效报表

### 4.1 关键指标

| 指标 | 计算方式 | 用途 |
|------|----------|------|
| SLA 达标率 | 已完成工单中 isSlaCompleted=true 的比例 | 衡量整体 SLA 表现 |
| 平均解决时长（小时） | AVG(duration) | 衡量处理效率 |
| 超时工单数 | 当月 isSlaCompleted=false 的已关闭工单 | 衡量超时绝对数量 |
| 平均首次响应时长 | AVG(IN_PROGRESS.startDateTime - NEW.createDateTime) | 衡量响应速度 |
| 超时占比趋势 | 按月统计超时工单 / 总工单 | 衡量变化趋势 |
| 团队 SLA 达标率 | 按 teamId 分组计算达标率 | 衡量团队表现 |

### 4.2 报表分类

| 报表 | 内容 | 周期 | 受众 |
|------|------|------|------|
| SLA 月报 | 达标率、平均解决时长、超时分布 | 月度 | 客服经理 |
| 团队排名 | 各团队 SLA 达标率、平均响应时长 | 月度 | 客服总监 |
| 超时工单明细 | 超时工单编号、处理人、超时时长、原因 | 实时/周度 | 客服主管 |
| SLA 趋势图 | 逐月 SLA 达标率折线图 | 月度 | 管理层 |
| 工单类型 SLA 分析 | 按 ticketType 分组 SLA 达标率 | 月度 | 流程优化 |

### 4.3 报表查询示例

```
-- SLA 达标率报表（按月）
SELECT
    DATE_TRUNC('month', t.createdDate) AS month,
    count(t.id) AS totalTickets,
    sum(CASE WHEN t.isSlaCompleted THEN 1 ELSE 0 END) AS completedInSla,
    sum(CASE WHEN NOT t.isSlaCompleted THEN 1 ELSE 0 END) AS breached,
    round(avg(t.duration) / 3600000, 2) AS avgDurationHours
FROM ErpCsTicket t
WHERE t.status = 'CLOSED'
GROUP BY DATE_TRUNC('month', t.createdDate)
ORDER BY month DESC

-- 团队 SLA 排名
SELECT
    tm.name AS teamName,
    count(t.id) AS totalTickets,
    sum(CASE WHEN t.isSlaCompleted THEN 1 ELSE 0 END) AS slaCompleted,
    round(avg(t.duration) / 3600000, 2) AS avgDurationHours
FROM ErpCsTicket t
JOIN ErpCsTeam tm ON tm.id = t.teamId
WHERE t.status = 'CLOSED'
  AND t.createdDate >= :startDate
GROUP BY tm.id, tm.name
ORDER BY slaCompleted DESC
```

---

## 五、SLA 配置管理

### 5.1 SLA 策略配置界面

| 配置项 | 说明 |
|--------|------|
| ticketType | 适用工单类型（多选或单选） |
| minPriority | 最低触发优先级 |
| team | 适用团队（可选，空=全部团队） |
| resolveHours | 解决时限小时数（isWorkingDays=false 时优先） |
| resolveDays | 解决时限天数（isWorkingDays=true 时优先） |
| isWorkingDays | 是否工作日模式 |
| workingHours | 工作时间段（如 09:00-18:00，可配置节假日日历） |
| escalationUserId | 一级升级通知人 |
| secondEscalationUserId | 二级升级通知人 |
| escalationDelayHours | 二级升级等待小时数 |
| isActive | 启用/禁用 |

### 5.2 节假日日历配置

工作日模式依赖节假日日历（可复用 master-data 域的组织日历）：

```
ErpHolidayCalendar（节假日日历）
├── year
├── date
├── isHoliday
└── description
```

工作日模式计算时跳过 isHoliday=true 的日期。

### 5.3 系统配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.sla-enabled` | true | 是否启用 SLA 计时 |
| `erp-cs.sla-scan-interval` | 1（分钟） | SLA 超时检测 Job 扫描间隔（语义键） |
| `erp-cs.sla-scan-cron` | —（默认不执行，运维启用配置键生效） | SLA 超时扫描 cron 门控。**SCHEDULED**（plan 2026-07-05-0306-1）：`ErpCsSlaScanJob` + `scheduler.yaml` 已接线，空值=跳过门控；非空时调 `IErpCsTicketBiz.scanOverdueTickets()` |
| `erp-cs.sla-warning-before` | 60（分钟） | 超时预警提前时间 |
| `erp-cs.escalation-l1-to-l2-hours` | 2 | 一级→二级升级等待小时数 |
| `erp-cs.sla-default-working-hours` | 09:00-18:00 | 默认工作时间段 |

---

## 六、审计与追溯

### 6.1 SLA 审计日志

| 记录点 | 内容 |
|--------|------|
| SLA 策略匹配 | 工单创建时匹配到哪个策略、计算的 deadlineDateTime |
| SLA 暂停/恢复 | 暂停时间、原因、恢复时间、暂停时长 |
| 超时检测 | 超时时间、升级级别、通知人 |
| deadline 延长 | 延长前/后的 deadline、原因、操作人 |
| SLA 标记覆盖 | 覆盖前 isSlaCompleted、覆盖原因、操作人 |

### 6.2 SLA 追溯链

```
工单 → SLA 策略 → deadlineDateTime 计算明细
        │
        ├─► 暂停记录（多次暂停/恢复时间轴）
        │
        ├─► 超时升级记录（级别、时间、通知人）
        │
        └─► 最终 SLA 结果（达标/超时 + 原因）
```

---

## 七、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| SLA 策略（days/hours/isWorkingDays/team/ticketType/prioritySelect） | 🟢 | Axelor `Sla.xml` 完整结构 |
| 工单 SLA 字段（deadlineDateTime/isSlaCompleted/startDateTime/duration） | 🟢 | Axelor `Ticket.xml` |
| AppHelpdesk 配置（状态映射/计时/SLA 开关） | 🟢 | Axelor `AppHelpdesk.xml`（manageTimer/isSla/resolvedTicketStatus） |
| 升级链（escalationUserId） | 🟡 | Axelor Sla.escalationUserId |
| 暂停机制（AWAITING_CUSTOMER 子状态） | 🟡 | 业界通用模式（Zendesk/Jira Service Management） |
| 多级升级（L1→L2→L3） | ⚪ | 本项目深化设计 |
| SLA 预警（deadline 前通知） | ⚪ | 本项目深化设计 |
| 节假日日历配置 | 🟡 | Axelor 无直接支持，参照 master-data 组织日历 |

## 八、跨域协作

| 对端 | 协作方式 |
|------|---------|
| nop-sys（定时任务） | SLA 超时检测 Job + 超时前预警 Job + 升级通知 Job |
| master-data（ErpMdPartner） | 节假日日历（工作日模式依赖） |
| CRM（ErpCrmLead） | 售后工单关联客户全生命周期 |

## 实现偏离补注（2026-07-04 实现）

> 权威计划：`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`。本节相对 §1-3 设计的实现取舍。

- **§1.2 匹配规则**：ORM 无 `isActive` 列——匹配器不做 active 过滤，按精确度（type+priority > type > 通用兜底；team 维度因工单无 `teamId` 列不参与，仅匹配 `policy.teamId IS NULL`）取首条。多策略同精确度取序为 Follow-up（触发条件：需显式启用/禁用策略切换时加 `isActive` 列）。
- **§1.3 deadline 计算**：日历小时模式 `now + resolveHours`（days 折算 24h/天）；工作日模式仅跳周末（Sat/Sun），不含 `workingHourStart/End` 工作时段窗口与节假日日历（ORM 无 workingHour 字段，`ErpHolidayCalendar` 未确认存在）。精确工时累计与法定节假日准确截止归 Non-Goal。
- **§2.1 计时起止**：`startDateTime = 首次 IN_PROGRESS 时间`（start 动作设置，非 NEW 创建时）；`duration = resolve 时 now - startDateTime`（分钟）。
- **§2.2 暂停/恢复机制**：归 Non-Goal（无 `ErpCsTicketSlaPause` 实体与 `adjustedDeadlineDateTime` 列）。
- **§3.1-3.2 超时升级**：仅 L1 通知 `escalationUserId`（`scanOverdueTickets` 创建 ESCALATE 审计）；L2/L3 多级升级链归 Non-Goal（ORM 无 `secondEscalationUserId`/`escalationDelayHours`）。`escalationUserId` 类型为 BIGINT(long)，非 `stdDomain=userId` 的 VARCHAR(36)。
- **§3.4 预警**：`findSlaWarnings(beforeMinutes)` 查询 `deadlineDateTime BETWEEN now AND now+beforeMinutes`（dateTimeBetween）且未完成，供 nop-job 调用；cron 实际注册归 Non-Goal（Follow-up：生产部署需定时自动触发时接 nop-job）。
- **§5.2 节假日日历**：`ErpHolidayCalendar` 未确认存在，首版不接入。
- **配置默认值**：`erp-cs.sla-enabled=true`、`erp-cs.sla-warning-before=60`（分钟）、`erp-cs.auto-assign-on-create=true`。

## 参考

- `state-machine.md`（工单状态机 + SLA 计时联动）
- `use-cases.md` §用例 4（SLA 超时与升级）
- `README.md` §ErpCsSlaPolicy
- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §售后服务
