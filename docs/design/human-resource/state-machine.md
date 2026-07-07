# 人力资源管理域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> HR 域有三类状态对象：**休假申请**（LeaveRequest）、**员工雇佣状态**（EmploymentStatus）与**工时表**（Timesheet）。

## 适用对象一：休假申请（LeaveRequest）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 草稿（DRAFT） | 员工填写未提交，等待提交 | 无，不影响考勤 |
| 已提交（SUBMITTED） | 已提交审批，等待直属上级/HR 审批 | 考勤计算暂不计入缺勤（等待审批结果） |
| 已批准（APPROVED） | 终态：审批通过 | 考勤扣减假期余额，写入 attendance 关联 |
| 已驳回（REJECTED） | 终态：审批不通过 | 不影响假期余额，不产生考勤变动 |
| 已取消（CANCELLED） | 终态：提交人撤销 | 同驳回 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ 提交 → 已提交 (SUBMITTED)
  └─ 取消 → 已取消 (CANCELLED)

已提交 (SUBMITTED)
  ├─ 审批通过 → 已批准 (APPROVED)
  ├─ 审批驳回 → 已驳回 (REJECTED)
  └─ 撤回 → 已取消 (CANCELLED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→SUBMITTED | 员工本人 | 必填字段完整（日期/类型/原因），不与其他休假冲突 | 生成审批待办 |
| SUBMITTED→APPROVED | 审批人（上级/HR） | 假期余额充足，无并行冲突 | 扣减假期余额，通知考勤模块 |
| SUBMITTED→REJECTED | 审批人 | — | 退回，员工可见驳回原因 |
| DRAFT/SUBMITTED→CANCELLED | 员工本人 | 仅限未 APPOVED 状态 | 释放假期暂扣额度 |

### 3. 终态与恢复

- 终态：`已批准（APPROVED）`、`已驳回（REJECTED）`、`已取消（CANCELLED）`。
- 终态不可直接恢复；若需修改已批准的休假，新建休假申请。
- 已批准的休假如需提前结束或取消，通过特殊流程（如加班调休充抵或 HR 手动调整）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 假期余额不足仍提交 | 前端校验拦截，禁止提交 |
| 已提交后员工离职 | 自动取消未完成的 LeaveRequest |
| 审批人长期不处理 | 超时自动转上级或代班人（可配置） |
| 并发提交同一时段 | 后端校验时间段重叠，拒绝后提交的请求 |
| 已批准的休假与已录入的考勤冲突 | 以休假为准，考勤行标记为由 leaveRequestId 覆盖 |

### 5. 可达性

- 从 DRAFT 可达所有终态；所有状态至少有一条入边。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| DRAFT→SUBMITTED | 员工（本人） |
| SUBMITTED→APPROVED | 直属上级 / HR 管理员 |
| SUBMITTED→REJECTED | 审批人 |
| 取消（→CANCELLED） | 员工本人（仅 DRAFT/SUBMITTED）或 HR 管理员（任意状态） |

危险操作：
- **已批准的休假取消**：仅 HR 管理员可操作（需补偿假期余额）。
- **休假转调休后取消**：需 HR 审批，因涉及调休额度变动。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 考勤模块读取已批准的休假 | LeaveRequest 发布状态变更事件，attendance 订阅 |
| 薪资计算引用已批准的休假 | Salary 模块查询 leaveType 统计缺勤扣款 |
| 假期余额管理 | 每次 APPROVED 扣减余额；取消时返还 |

外部触发渠道：
- 员工自助提交（主要渠道）。
- HR 管理员代提交（线下请假补录）。
- 系统自动取消（离职联动）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 否 | — |
| SUBMITTED | 是 | assigned（审批人）—— 待审批休假 |
| APPROVED/REJECTED/CANCELLED | 否 | — |

避免"已提交休假长期未审批"：SUBMITTED 产生审批待办，超时自动催办（可配置催办间隔）。

### 9. 场景演练

#### 场景 A：年假申请审批通过

1. 员工创建年假休假申请（DRAFT），填写 7 月 15-19 日共 5 天。
2. 提交 → SUBMITTED，直属上级收到审批待办。
3. 上级审核 → APPROVED，扣减年假余额 5 天。
4. 考勤模块收到事件，7/15-7/19 标记为休假。

#### 场景 B：事假被驳回

1. 员工提交事假申请（SUBMITTED）。
2. HR 认为理由不充分 → REJECTED。
3. 员工收到驳回通知，可修改原因重新提交。

#### 场景 C：已提交后自行取消

1. 员工提交年假→SUBMITTED。
2. 计划有变，员工在审批前自行取消→CANCELLED。
3. 审批人待办自动关闭，余额未变动。

### 10. 与设计文档一致性

- 状态定义见 `human-resource/README.md` §LeaveRequest。
- 状态码归 `model/app-erp-hr.orm.xml` dict `erp-hr/leave-status`。

---

## 适用对象二：员工雇佣状态（EmploymentStatus）

### 1. 状态定义

| 状态 | 业务含义（等待什么） |
|------|----------------------|
| 在职（ACTIVE） | 正常在职状态 |
| 试用（PROBATION） | 新员工试用期 |
| 已离职（RESIGNED） | 终态：员工主动提出离职 |
| 已解雇（TERMINATED） | 终态：公司解雇 |
| 已退休（RETIRED） | 终态：达到退休年龄 |

### 2. 迁移完整性

```
在职 (ACTIVE) ──────────────────────────────────
  ├─ 主动离职 → 已离职 (RESIGNED)
  ├─ 解雇 → 已解雇 (TERMINATED)
  └─ 退休 → 已退休 (RETIRED)

试用 (PROBATION)
  ├─ 转正 → 在职 (ACTIVE)
  ├─ 主动离职 → 已离职 (RESIGNED)
  ├─ 试用期解雇 → 已解雇 (TERMINATED)
  └─ 试用期取消 → 已离职 (RESIGNED)  [特殊处理]
```

### 3. 终态与恢复

- 终态：`已离职（RESIGNED）`、`已解雇（TERMINATED）`、`已退休（RETIRED）`。
- 终态不可直接恢复；再入职需重新建立 ErpHrEmployee 记录（或标记 rehire）。

### 4. 场景演练

#### 场景 D：员工主动离职

1. 员工提交离职申请（外部系统或纸质）。
2. HR 在系统中将 employmentStatus 置为 RESIGNED，填写 resignationDate/resignationReason。
3. 系统联动：
   - 该员工的所有 ACTIVE 劳动合同→TERMINATED。
   - 该员工的未完成 LeaveRequest→CANCELLED。
   - 停用系统账号（UserAccount）。

#### 场景 E：试用期转正

1. 员工状态为 PROBATION。
2. HR 操作转正，设置 regularDate。
3. 状态→ACTIVE，probationEndDate 自动推导（hireDate + 合同约定的试用期月数）。

---

## 适用对象三：工时表（Timesheet）

### 1. 状态定义

| 状态 | 业务含义 |
|------|----------|
| 草稿（DRAFT） | 工时行录入中，未提交 |
| 已提交（SUBMITTED） | 员工已提交，等待审核 |
| 已批准（APPROVED） | 终态：审核通过，工时归集到项目成本 |
| 已驳回（REJECTED） | 终态：审核不通过，退回修改 |

### 2. 迁移

```
DRAFT → SUBMITTED → APPROVED
                  → REJECTED → DRAFT（修改后重新提交）
DRAFT → CANCELLED
```

### 3. 场景演练

#### 场景 F：工时表提交审批

1. 员工填写本周工时明细（DRAFT），记录每天在各项目上的工时。
2. 提交 → SUBMITTED，项目经理收到审批待办。
3. 审核通过 → APPROVED，工时数据归集到 `projects/cost-collection`。
4. 若发现工时填错，项目经理驳回 → REJECTED，员工修改后重新提交。

---

## 适用对象四：薪酬审批（ErpHrSalary.approveStatus）

### 1. 设计说明

薪酬审批不定义独立的状态机——采用平台标准 `wf/approve-status` 四态，与全 ERP 审批机制统一。多级审批链（HR 复核 → 财务审批 → 经理审批）由 nop-wf WORKFLOW 模式内部管理，不在业务表状态字段中编码。

### 2. 状态定义

| approveStatus | 业务含义 |
|--------------|----------|
| UNSUBMITTED | 新建/撤回，核算完成待提交 |
| SUBMITTED | 已提交待审批（WF 内部多级处理中） |
| APPROVED | 审批通过（全部三级均通过，终态） |
| REJECTED | 任一级审批人驳回（终态，可修改后重新提交） |

### 3. 迁移

```
UNSUBMITTED → SUBMITTED → APPROVED
                       → REJECTED
        ← withdrawApproval
APPROVED → SUBMITTED（reverseApprove，需配置门控）
```

### 4. 发放执行独立轴

审批通过后（`approveStatus=APPROVED`），发放执行和作废由 `paymentStatus` 独立管理：

| paymentStatus | 业务含义 |
|--------------|----------|
| PENDING | 待发放 |
| PAID | 已发放（终态，锁定） |
| VOID | 已作废（终态） |

迁移：`PENDING → PAID`（`markPaid`），`PENDING → VOID`（`voidSalary`），`PAID` 后锁定。

### 5. 角色与权限

| WF 步骤 | 执行角色 |
|---------|----------|
| hr-review | HR 薪酬专员 |
| finance-review | 财务主管 |
| manager-approval | 部门负责人/总经理 |
| 发放（markPaid） | HR 薪酬专员/出纳 |

### 6. 外部依赖

- 薪酬核算引擎（`IErpHrSalaryBiz.calculateSalary`）在 `approveStatus=UNSUBMITTED` 下操作
- 业财过账：`approveStatus → APPROVED` 触发 SALARY(270) 计提凭证 + SOCIAL_INSURANCE_ER(290)/HOUSING_FUND_ER(300)
- `paymentStatus → PAID` 触发 SALARY_PAYMENT(280) 发放凭证
- 银行文件生成：查询 `paymentStatus=PENDING AND approveStatus=APPROVED`

### 7. 与设计文档一致性

- 状态定义见 `payroll.md §五/§六`。
- 状态码归 `wf/approve-status` 标准字典（nop-wf 模块统一定义），`ErpHrSalary.approveStatus` 字段引用 `ext:dict="wf/approve-status"`。
- 业务覆盖：`docs/design/human-resource/payroll.md`

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 休假申请已批准后的取消权限是否落实（仅 HR 管理员）。
- 员工离职时未完成休假申请的联动取消。
- 假期余额扣减与返还是否原子化（APPROVED 扣减、CANCELLED 返还）。
- 工时表 APPROVED 后工时归集到项目成本的触发机制。
- 薪酬审批 `approveStatus` 是否严格遵循四态标准，不含 WF 中间步骤值。
- `paymentStatus` 的 PAID 锁定是否独立于 `approveStatus` 运作。
