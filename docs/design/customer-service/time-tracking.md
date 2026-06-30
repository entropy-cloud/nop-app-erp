# 客服工单域 — 工单计时（Time Tracking）

## 目的

为客服工单提供精细的时间追踪：手动/计时器记录处理时长，区分可计费与不可计费时间，支持工时审批与项目成本归集。

---

## 一、模型设计

### 1.1 ErpCsTimeEntry（工单计时条目）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/orgId | 标准 | |
| ticketId | 关联工单（→ErpCsTicket） | 必填 |
| agentId | 处理人（→User） | 必填 |
| startTime | 开始时间 | |
| endTime | 结束时间 | |
| duration | 时长（分钟） | endTime - startTime |
| isBillable | 是否可计费 | 默认 true |
| billingRate | 计费费率（可选） | 覆盖默认费率 |
| billableAmount | 计费金额（可选） | duration × billingRate |
| description | 工作内容描述 | |
| approvalStatus | 审批状态（dict） | PENDING/APPROVED/REJECTED |
| approvedById | 审批人 | |
| approvedAt | 审批时间 | |
| projectId | 关联项目（→ErpProProject，可选） | 项目工时归集 |
| taskId | 关联任务（→ErpProTask，可选） | 任务工时归集 |
| source | 来源（dict） | MANUAL/TIMER_IMPORT |
| 标准审计字段 | | |

### 1.2 计费费率

计费费率按以下优先级确定：

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 1 | timeEntry.billingRate | 条目级覆盖 |
| 2 | 客服角色费率（ErpCsAgentRate.rate） | 按 agentId + serviceType 匹配 |
| 3 | 权益/合同费率（ErpCsEntitlement） | 按关联合同 |
| 4 | 全局默认费率（`erp-cs.default-billing-rate`） | 配置项默认值 |

ErpCsAgentRate（客服费率）参考：

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| agentId | 客服（→User） |
| serviceType | 服务类型（dict） |
| rate | 费率（元/小时） |
| effectiveDate | 生效日期 |
| isActive | 是否启用 |

---

## 二、计时模式

### 2.1 手动录入

```
客服手动填写时间条目
        │
        ├─► 选择工单 → 填写 startTime / endTime / duration
        │
        ├─► 填写 isBillable / description
        │
        ├─► 提交 → 创建 ErpCsTimeEntry
        │
        └─► 发起审批（可选，按配置）
```

### 2.2 计时器模式

```
客服点击"开始计时"
        │
        ├─► 创建计时器（session 级别）
        │
        ├─► 系统定时记录（每分钟更新 duration）
        │
        ├─► 客服点击"停止计时"
        │
        ├─► 生成 ErpCsTimeEntry
        │       ├─ startTime = 计时器开始时间
        │       ├─ endTime = 停止时间
        │       └─ duration = 自动计算
        │
        └─► 客服补充 description / isBillable → 提交
```

**多计时器约束**：同一客服同一时刻只能启动一个计时器。启动新计时器前自动停止当前计时器（提示确认）。

### 2.3 暂停/恢复计时

```
计时器运行中
        │
        ├─► 客服点击"暂停"
        │       ├─ 暂停计时（不生成条目）
        │       └─ 可填写"暂停原因"（如等待客户回复）
        │
        ├─► 客服点击"恢复"
        │       └─ 继续计时
        │
        └─► 停止时 totalDuration = 运行时长 - 暂停时长
```

---

## 三、审批流程

### 3.1 审批触发

| 触发条件 | 说明 |
|----------|------|
| isBillable=true | 可计费时间条目提交后自动进入审批 |
| 工时超过阈值 | `erp-cs.time-entry-approval-threshold` 分钟以上 |
| 按组织配置 | 特定 orgId 要求全部工时条目审批 |

### 3.2 审批状态机

```
DRAFT（草稿，未提交）
  │
  └─ 提交 → PENDING（待审批）
              ├─ 审批通过 → APPROVED（终态）
              ├─ 审批拒绝 → REJECTED（可修改后重新提交）
              └─ → DRAFT（退回给客服修改）
```

### 3.3 审批人

| 场景 | 审批人 |
|------|--------|
| 工单级 | 工单.responsibleId（负责人） |
| 团队级 | 客服团队负责人 |
| 无匹配 | 客服主管（Admin 手动指定） |

---

## 四、工单总计时聚合

```
ErpCsTicket 扩展字段（可选，非实体字段，通过 SQL 聚合）
        │
        ├─► totalTimeSpent（总处理时长）
        │       SELECT sum(duration) FROM ErpCsTimeEntry
        │       WHERE ticketId = :ticketId
        │         AND approvalStatus IN ('APPROVED', 'PENDING')
        │
        ├─► totalBillableTime（总可计费时长）
        │       SELECT sum(duration) FROM ErpCsTimeEntry
        │       WHERE ticketId = :ticketId
        │         AND isBillable = true
        │         AND approvalStatus = 'APPROVED'
        │
        └─► totalBilledAmount（总计费金额）
                SELECT sum(billableAmount) FROM ErpCsTimeEntry
                WHERE ticketId = :ticketId
                  AND isBillable = true
                  AND approvalStatus = 'APPROVED'
```

---

## 五、集成与跨域协作

### 5.1 项目工时归集

当 timeEntry 关联了 projectId 时，工时时长可汇总到项目工时：

```
ErpCsTimeEntry.ticketId = A, projectId = B

报表：项目 B 的总客服工时 = sum(duration) WHERE projectId = B
```

### 5.2 财务计费

可计费工时条目审批通过后 → 触发计费流程：

| 计费方式 | 说明 | 跨域动作 |
|----------|------|----------|
| 按合同计费 | 根据权益合同的总计费打包 | 不计单个条目 |
| 按次计费 | 根据条目汇总金额开票 | 触发 sales 域创建销售订单/发票 |
| 按工单计费 | 工单关闭时汇总计费 | 工单 CLOSED 触发计费 |

### 5.3 与 HR 考勤集成（可选）

客服工时不直接写入 HR 考勤，但可导出供 HR 工时成本核算：

```
ErpCsTimeEntry（客服工时）
        │
        ├─► 客服个人日报：当日总工时 / 可计费工时
        │
        └─► 导出接口 → HR 域的成本归集（可选）
```

---

## 六、报表

| 报表 | 内容 | 用途 |
|------|------|------|
| 客服工时日报 | 按客服列当日总工时、可计费工时 | 工作量评估 |
| 工单工时明细 | 按工单列所有 timeEntry 明细 | 客户计费对账 |
| 项目客服工时 | 按项目汇总客服工时 | 项目成本核算 |
| 团队产能报告 | 按团队列人均工时/可利用率 | 资源规划 |
| 计费金额月报 | 按月汇总可计费工时与金额 | 收入确认 |

### 查询示例

```
-- 客服工时日报
SELECT
    t.assignedToId,
    s.agentId,
    sum(te.duration) AS totalMinutes,
    sum(CASE WHEN te.isBillable THEN te.duration ELSE 0 END) AS billableMinutes,
    count(te.id) AS entries
FROM ErpCsTimeEntry te
JOIN ErpCsTicket t ON t.id = te.ticketId
WHERE te.startTime >= :todayStart
  AND te.startTime < :tomorrowStart
GROUP BY t.assignedToId, s.agentId
ORDER BY totalMinutes DESC

-- 工单计费汇总
SELECT
    t.code AS ticketCode,
    t.subject,
    round(sum(te.duration)::decimal / 60, 2) AS totalHours,
    round(sum(te.billableAmount), 2) AS totalAmount
FROM ErpCsTimeEntry te
JOIN ErpCsTicket t ON t.id = te.ticketId
WHERE te.isBillable = true
  AND te.approvalStatus = 'APPROVED'
  AND t.endDate BETWEEN :startDate AND :endDate
GROUP BY t.id, t.code, t.subject
```

---

## 七、配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.time-tracking-enabled` | true | 是否启用工单计时 |
| `erp-cs.time-entry-require-description` | true | 时间条目是否必须填写描述 |
| `erp-cs.time-entry-approval-threshold` | 480（分钟） | 超过此时长需审批 |
| `erp-cs.time-entry-auto-approve` | false | 是否自动审批（跳过审批流程） |
| `erp-cs.default-billing-rate` | 0 | 全局默认计费费率（元/小时） |
| `erp-cs.time-entry-timer-max-hours` | 12 | 单次计时器最大时长（自动停止） |

---

## 八、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| Odoo helpdesk timesheet integration | 🟢 | `helpdesk.ticket` → `timesheet_ids` 关联 `account.analytic.line` |
| ServiceNow time tracking | 🟢 | Task → time_worked / billable_time 字段 |
| Jira time tracking | 🟡 | Worklog → issue 关联 + 计费/非计费分类 |
| Axelor timesheet | 🟡 | TimesheetLine → project + task 归集 |

## 九、跨域协作

| 对端 | 协作方式 |
|------|---------|
| 工单（ErpCsTicket） | 时间条目关联工单，聚合总工时 |
| 项目（ErpProProject） | 工时可归集到项目成本（可选） |
| 任务（ErpProTask） | 工时可归集到任务（可选） |
| sales（计费发票） | 审批通过的可计费时间 → 创建销售订单/发票 |
| HR（工时成本） | 导出客服工时供人力资源成本分析 |
