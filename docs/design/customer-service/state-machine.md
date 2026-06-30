# 售后服务/客服工单域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 客服域主状态对象：**工单（Ticket）**。T:SLA 计时器作为辅助状态对象，与工单状态联动。

## 适用对象：客服工单（Ticket）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | SLA 计时影响 |
|------|----------------------|--------------|
| 新建（NEW） | 工单已创建，等待分派处理人 | SLA 从创建时开始计时 |
| 已分配（ASSIGNED） | 已分派处理人，等待开始处理 | SLA 继续计时 |
| 处理中（IN_PROGRESS） | 处理人正在处理，等待解决 | SLA 继续计时 |
| 已解决（RESOLVED） | 已给出解决方案，等待客户确认关闭 | SLA 停止计时，标记 isSlaCompleted |
| 已关闭（CLOSED） | 终态：客户确认关闭 | — |
| 已取消（CANCELLED） | 终态：工单取消（误开/重复/客户主动取消） | SLA 停止（不计入绩效） |

### 2. 迁移完整性

```
NEW（新建）
  ├─ 自动/手动分派 → ASSIGNED（已分配）
  │                   └─ 开始处理 → IN_PROGRESS（处理中，SLA 计时）
  │                                    ├─ 标记解决 → RESOLVED（已解决，SLA 停止）
  │                                    │               └─ 客户确认 → CLOSED（终态）
  │                                    │               └─ 客户驳回 → IN_PROGRESS（重新处理）
  │                                    └─ → CANCELLED（终态）
  └─ → CANCELLED（终态）
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| NEW→ASSIGNED | 系统自动 / 客服主管 | 工单有效，类型/优先级已配置 | 分配处理人 |
| ASSIGNED→IN_PROGRESS | 处理人 | 处理人接受工单 | SLA 开始正式计时（startDateTime 设置） |
| IN_PROGRESS→RESOLVED | 处理人 | 已给出解决方案，填写 resolution | SLA 停止，deadlineDateTime 对比 |
| RESOLVED→CLOSED | 客户 / 客服 | 客户确认问题解决 | 终态，endDateTime 设置 |
| RESOLVED→IN_PROGRESS | 处理人 | 客户驳回解决方案 | 恢复计时（时长累加） |
| IN_PROGRESS→CANCELLED | 客服主管 | 重复工单/客户要求取消 | 终态 |
| NEW→CANCELLED | 客服主管 | 误开、无效请求 | 终态 |

### 3. 终态与恢复

- 终态：`已关闭（CLOSED）`、`已取消（CANCELLED）`。
- 终态不可恢复。若客户重新发起问题，新建工单并关联原工单号。
- RESOLVED 不是终态 — 客户可驳回，工单回到 IN_PROGRESS。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| SLA 超时未解决 | 触发 escalationUserId 升级通知；isSlaCompleted=false 保留 |
| 工单分派后处理人不响应 | 客服主管手动重新分派（→ ASSIGNED） |
| 客户驳回解决后处理人失联 | 触发二次升级通知 |
| 并发处理同一工单 | 乐观锁（version 字段） |
| 工单关联业务单据作废 | 不影响工单生命周期（弱指针关联） |
| 客户重复提交相同问题 | 合并到原工单，新工单 CANCELLED |

### 5. 可达性

- 所有状态均从 NEW 可达。
- 从 IN_PROGRESS 可达三个目标（RESOLVED、CANCELLED）。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| NEW→ASSIGNED | 系统（自动匹配）、客服主管（手动分派） |
| ASSIGNED→IN_PROGRESS | 工单处理人 |
| IN_PROGRESS→RESOLVED | 工单处理人 |
| RESOLVED→CLOSED | 客户（自助确认）、客服（代确认） |
| RESOLVED→IN_PROGRESS | 工单处理人（客户驳回后） |
| *→CANCELLED | 客服主管 |

危险操作：
- **关闭前检查**：CLOSED 前必须检查 isSlaCompleted——超时工单关闭需注明超时原因。
- **取消工单**：需客服主管审批，因取消后数据不可恢复。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 客户通过门户/邮件/聊天创建工单 | 外部渠道 → 系统自动创建 NEW 工单 |
| 质量问题升级到 NCR | 处理人标记 quality 联动 → 创建 ErpQaNonConformance |
| 设备报修触发维护 | 处理人标记 maintenance 联动 → 创建维护请求 |
| SLA 超时通知 | nop-job 定时扫描 deadlineDateTime，触发升级事件 |

外部触发渠道：
- 客户自助门户（主要渠道）。
- 客服人员代录入（电话/邮件接收）。
- API 集成（第三方系统）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| NEW | 是 | assigned（客服主管）—— 待分派 |
| ASSIGNED | 是 | assigned（处理人）—— 待处理 |
| IN_PROGRESS | 是 | assigned（处理人）—— 处理中 |
| RESOLVED | 是 | assigned（客户）—— 待确认关闭 |
| CLOSED | 否 | — |
| CANCELLED | 否 | — |

避免"工单滞留"规则：
- NEW 停留超过 1 小时 → 自动升级通知客服主管。
- ASSIGNED 停留超过 2 小时 → 自动提醒处理人。
- IN_PROGRESS 超 deadlineDateTime → 触发 SLA 超时升级。

### 9. 场景演练

#### 场景 A：正常解决流程（软件故障报修）

1. 客户通过门户提交工单 → NEW（subject="ERP 登录页面报错 500"，priority=HIGH）。
2. 系统自动匹配 ticketType="故障"，根据 SLA 策略（HIGH→8 小时）计算 deadlineDateTime=now+8h。
3. 系统按轮转规则分配给张三 → ASSIGNED。
4. 张三点击"开始处理" → IN_PROGRESS（startDateTime=now）。
5. 张三定位 bug 并修复，填写 resolution → RESOLVED（SLA 停止，耗时 3h < 8h ✅）。
6. 客户确认问题解决 → CLOSED（endDateTime=now）。

#### 场景 B：SLA 超时升级（紧急投诉处理）

1. 客户电话投诉，客服录入 → NEW（priority=URGENT，deadlineDateTime=now+4h）。
2. 手动分配给李四 → ASSIGNED。
3. 李四未及时响应，4h 后 deadlineDateTime 到达，isSlaCompleted=false。
4. ESCALATE 事件触发 → 通知 escalationUserId（客服经理），操作日志记录。
5. 客服经理重新分派给王五 → ASSIGNED（重置 deadlineDateTime）。
6. 王五处理完成 → RESOLVED → CLOSED（isSlaCompleted=false，注明超时原因）。

#### 场景 C：工单取消（重复提交）

1. 客户提交工单 → NEW。
2. 系统检测到相同问题已有进行中的工单（编号 TK2026060001）。
3. 客服主动联系客户确认合并。
4. 客服主管将新工单标记为取消，关联原工单 → CANCELLED。
5. 操作日志记录取消原因："重复工单，已合并至 TK2026060001"。

### 10. 与设计文档一致性

- 工单状态定义见 `customer-service/README.md` §ErpCsTicket。
- SLA 规则见 `customer-service/README.md` §SLA 策略。
- 状态码归 `module-cs/model/app-erp-cs.orm.xml`（dict: erp-cs/ticket-status）。
- 工单操作日志实体 ErpCsTicketAction 记录完整状态迁移历史。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- SLA 计时起止点是否正确（NEW 开始，RESOLVED 停止）。
- 超时升级机制是否落实（escalationUserId + nop-job 定时扫描）。
- 终态不可逆（CLOSED/CANCELLED 无出边）。
- RESOLVED→IN_PROGRESS 驳回路径确保客户满意度。
- CANCELLED 状态操作权限限制（仅客服主管）。
