# 招聘管理（Recruitment）

## 目的

详细设计招聘管理全流程：招聘需求申请 → 职位发布 → 候选人管理 → 面试评估 → Offer 管理 → 入职交接。实现从"缺人"到"到岗"的完整闭环。

---

## 一、招聘需求管理

### 1.1 招聘需求申请（ErpHrRecruitmentRequest）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 AureusERP `recruitment_request.php` |
| departmentId | 申请部门（→ErpHrDepartment） | — |
| positionId | 招聘职位（→ErpHrPosition） | — |
| headcount | 需求人数 | — |
| headcountFilled | 已到岗人数 | — |
| urgency | 紧急程度 dict `erp-hr/recruitment-urgency`：NORMAL=10 / URGENT=20 / CRITICAL=30 | — |
| reason | 招聘原因 dict：NEW_POSITION（新增编制）/ REPLACEMENT（补员）/ PROJECT（项目制） | — |
| expectedOnboardDate | 期望到岗日期 | — |
| salaryRangeLow/salaryRangeHigh | 薪资范围 | — |
| jobDescription | 岗位描述 | — |
| requirements | 任职要求 | — |
| approvalStatus | 审批状态 dict `erp-hr/recruitment-req-status`：DRAFT / SUBMITTED / APPROVED / REJECTED / CLOSED | 🟢 AureusERP `approval.php` |
| approverId | 审批人（→ErpHrEmployee） | — |
| approvedAt | 审批时间 | — |
| 标准审计字段 | | |

**状态机**：

```
草稿 (DRAFT)
  ├─ 提交 → 已提交 (SUBMITTED)
  └─ 取消 → 已关闭 (CLOSED)

已提交 (SUBMITTED)
  ├─ 审批通过 → 已批准 (APPROVED)
  └─ 驳回 → 已驳回 (REJECTED)

已批准 (APPROVED)
  ├─ 招聘完成 → 已关闭 (CLOSED)
  └─ 取消 → 已关闭 (CLOSED)
```

### 1.2 审批流程

```
部门主管提交需求
        │
        ├─► HR 审核
        │       ├─ 编制检查（是否在年度 HC 计划内）
        │       ├─ 薪资范围合理性
        │       └─ 审核通过 → 部门经理审批
        │
        └─► 部门经理审批
                └─ 审批通过 → APPROVED，创建招聘计划
```

---

## 二、招聘计划与职位发布

### 2.1 招聘计划（ErpHrRecruitmentPlan）

| 字段 | 含义 |
|------|------|
| id/code/requestId/orgId | 标准 |
| planName | 招聘计划名称 |
| responsibleId | 招聘负责人（→ErpHrEmployee） |
| targetDate | 目标完成日期 |
| budgetAmount | 招聘预算（含猎头费/广告费） |
| status | dict：PLANNING / IN_PROGRESS / COMPLETED / CANCELLED |

### 2.2 职位发布渠道管理

| 渠道 | 说明 | 参考 |
|------|------|------|
| 公司官网 | 自有招聘页面 | 🟢 AureusERP `job_post.php` |
| 招聘网站 | 智联/前程无忧/Boss 直聘（API/手动） | — |
| 猎头 | 委托猎头公司 | — |
| 内推 | 员工推荐奖励 | 🟢 Odoo `hr_recruitment.referral` |
| 校园招聘 | 校园宣讲/双选会 | — |
| 社交媒体 | 领英/脉脉 | — |

**ErpHrJobPosting（职位发布）**：

| 字段 | 含义 |
|------|------|
| id/planId/orgId | 标准 |
| channel | 发布渠道 dict `erp-hr/job-channel` |
| postingUrl | 发布链接 |
| postingDate | 发布日期 |
| expiryDate | 截止日期 |
| cost | 发布费用 |
| status | dict：ACTIVE / EXPIRED / CLOSED |

> 🟢 AureusERP `job_post.php`（职位发布管理）。
> 🟢 Odoo `hr_recruitment` 多渠道发布支持。

---

## 三、候选人管理

### 3.1 候选人实体（ErpHrCandidate）

扩展 `README.md` §ErpHrRecruitment 为独立实体，分离"招聘职位"与"候选人"。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 AureusERP `candidate.php` |
| firstName/lastName | 姓名 | — |
| gender | 性别 | — |
| phone/email | 联系方式 | — |
| birthDate | 出生日期 | — |
| source | 来源渠道 dict `erp-hr/candidate-source`：WEBSITE / AGENCY / REFERRAL / CAMPUS / SOCIAL / WALK_IN | 🟢 Odoo `hr.candidate.source` |
| resumeAttachmentId | 简历附件 | — |
| resumeText | 简历文本（解析后全文检索） | — |
| currentCompany | 现公司 | — |
| currentPosition | 现职位 | — |
| workYears | 工作年限 | — |
| educationLevel | 学历 dict：HIGH_SCHOOL / COLLEGE / BACHELOR / MASTER / DOCTOR | — |
| expectedSalary | 期望薪资 | — |
| status | 状态 dict `erp-hr/candidate-status`：NEW / SCREENING / INTERVIEW / OFFERED / HIRED / REJECTED / WITHDRAWN | 🟢 AureusERP `candidate_status.php` |
| tags | 标签（逗号分隔，如"Java/架构师/985"） | — |
| blacklisted | 是否黑名单 | — |
| blacklistReason | 黑名单原因 | — |
| 标准审计字段 | | |

### 3.2 候选人管道

```
新进 (NEW)
  │
  ├─ HR 筛选 → 筛选中 (SCREENING)
  │
筛选中 (SCREENING)
  │
  ├─ 简历初筛通过 → 面试中 (INTERVIEW)
  │  （可记录筛选中阶段的多个面试轮次）
  │
  └─ 不合适 → 已拒绝 (REJECTED)
  │
面试中 (INTERVIEW)
  │
  ├─ 全部面试通过 → 待发 Offer (OFFERED)
  │
  ├─ 面试未通过 → 已拒绝 (REJECTED)
  │
  └─ 候选人放弃 → 已放弃 (WITHDRAWN)
  │
待发 Offer (OFFERED)
  │
  ├─ 候选人接受 → 已录用 (HIRED)
  │     │
  │     ├─ 创建 ErpHrEmployee（员工主数据）
  │     ├─ 入职通知
  │     └─ 触发入职清单
  │
  ├─ 候选人拒绝 → 已拒绝 (REJECTED)
  │
  └─ Offer 过期 → 已拒绝 (REJECTED)
```

### 3.3 筛选与评分

**ErpHrCandidateScreening（筛选记录）**：

| 字段 | 含义 |
|------|------|
| id/candidateId/orgId | 标准 |
| stage | 阶段 dict：RESUME_SCREEN（简历筛选）/ PHONE_INTERVIEW（电话面试）/ WRITTEN_TEST（笔试） |
| reviewerId | 筛选人 |
| rating | 评分（1-5） |
| conclusion | 结论 dict：PASS / FAIL / PENDING |
| comment | 评语 |

---

## 四、面试管理

### 4.1 面试安排（ErpHrInterview）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/candidateId/orgId | 标准 | 🟢 AureusERP `interview.php` |
| roundNo | 面试轮次（1/2/3/终面） | — |
| interviewType | 类型 dict：ONSITE（现场）/ VIDEO（视频）/ PHONE（电话） | — |
| scheduledDateT | 计划时间 | — |
| durationMinutes | 预计时长 | — |
| location/meetingLink | 地点/视频链接 | — |
| interviewers | 面试官列表（JSON，[ErpHrEmployee.id]） | — |
| status | dict：SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED | — |
| result | 结果 dict：PASS / FAIL / HOLD | — |
| 标准审计字段 | | |

### 4.2 面试评分表（ErpHrInterviewScorecard）

| 字段 | 含义 |
|------|------|
| id/interviewId/interviewerId | 标准 |
| dimension | 评分维度 dict：PROFESSIONAL_SKILL / COMMUNICATION / TEAMWORK / LEADERSHIP / CULTURE_FIT |
| score | 分值（1-10） |
| weight | 权重（该维度占总分的百分比） |
| comment | 评价 |

**评分聚合**：`加权总分 = Σ(score × weight) / Σ(weight)`

### 4.3 面试流程常见模式

```
第一轮（技术面/HR 初面）
    │
    ├─► 通过 → 第二轮
    │
    └─► 不通过 → REJECTED

第二轮（部门主管/技术总监）
    │
    ├─► 通过 → 第三轮（终面）
    │
    └─► 不通过 → REJECTED

第三轮 - 终面（高管/HR 总监）
    │
    ├─► 通过 → OFFERED
    │
    └─► 不通过 → REJECTED
```

> 轮次数量可配置（`erp-hr.default-interview-rounds`）。

---

## 五、Offer 管理

### 5.1 Offer 实体（ErpHrOffer）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/candidateId/orgId | 标准 | 🟢 AureusERP `offer.php` |
| offerDate | 发出日期 | — |
| expiryDate | 有效期（通常 3-7 天） | — |
| baseSalary | 基本月薪 | — |
| positionAllowance | 岗位津贴 | — |
| performanceBonusGuarantee | 绩效保障（如"首年保障 3 个月"） | — |
| stockOptions | 期权/股票（可选） | — |
| signOnBonus | 签字费（可选） | — |
| relocationAllowance | 搬迁补贴（可选） | — |
| probationMonths | 试用期月数 | — |
| probationSalaryPercent | 试用期工资比例（通常 80%） | — |
| expectedOnboardDate | 期望入职日期 | — |
| templateId | 模板（→ErpHrOfferTemplate） | — |
| content | Offer 正文（由模板生成 + 填充变量） | — |
| status | dict：DRAFT / SENT / ACCEPTED / REJECTED / EXPIRED / WITHDRAWN | — |
| acceptedAt | 接受时间 | — |
| signedDocAttachmentId | 签署的 Offer 文件（电子签名后） | — |
| 标准审计字段 | | |

### 5.2 Offer 模板（ErpHrOfferTemplate）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| contentTemplate | 模板内容（含变量占位符，如 `{candidateName}`、`{baseSalary}`） |
| isDefault | 是否默认模板 |
| language | 语言（zh_CN / en_US） |

### 5.3 Offer 审批流程

```
HR 生成 Offer（DRAFT）
    │
    ├─► 填写薪资/条款（引用面试评估结果）
    │
    ├─► 提交审批
    │       ├─ HR 经理审批（薪资合规）
    │       └─ 部门主管确认
    │
    ├─► 审批通过 → 发送候选人（SENT）
    │
    ├─► 候选人接受（ACCEPTED）
    │       └─ 电子签名 → 触发入职流程
    │
    ├─► 候选人拒绝（REJECTED）→ 回溯面试流程
    │
    └─► 超时未签（EXPIRED）→ 自动关闭
```

### 5.4 Offer 状态机

```
草稿 (DRAFT) → 已发送 (SENT) → 已接受 (ACCEPTED) [终态]
                              → 已拒绝 (REJECTED) [终态]
                              → 已过期 (EXPIRED)  [终态]
草稿 (DRAFT) → 已撤回 (WITHDRAWN) [终态]
已接受 (ACCEPTED) → 入职完成 → 关联 ErpHrEmployee
```

---

## 六、入职管理

### 6.1 入职清单（ErpHrOnboardingChecklist）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/offerId/employeeId | 标准（HIRED 后关联） | 🟢 AureusERP `onboarding.php` |
| item | 事项 | 🟢 Odoo `hr.onboarding` |
| owner | 负责人（HR / IT / 行政 / 部门） | — |
| dueDate | 完成期限 | — |
| status | dict：PENDING / COMPLETED / SKIPPED | — |
| completedAt | 完成时间 | — |
| remark | 备注 | — |

### 6.2 标准入职事项

| 事项 | 负责人 | 说明 |
|------|--------|------|
| 劳动合同签署 | HR | 准备合同文件，完成签署 |
| 工位/办公设备 | 行政 | 工位分配、电脑/电话/文具 |
| 系统账号开通 | IT | ERP 账号、邮箱、VPN、OA |
| 员工手册/制度培训 | HR | 发放员工手册，签字确认 |
| 门禁卡/工牌 | 行政 | — |
| 薪资卡登记 | HR | 银行卡信息录入 ErpHrEmployee |
| 社保/公积金增员 | HR | 提交社保/公积金系统 |
| 部门入职引导 | 用人部门 | 工作交接、团队介绍 |
| 新员工培训 | HR/培训 | — |

### 6.3 入职流程

```
候选人接受 Offer（ACCEPTED）
    │
    ├─► 创建 ErpHrEmployee（employmentStatus = PROBATION）
    │
    ├─► 生成入职清单（ErpHrOnboardingChecklist）
    │
    ├─► 分配任务到各负责人（TODO）
    │
    ├─► expectedOnboardDate 到达
    │
    ├─► 入职日各负责人确认完成
    │
    └─► 入职清单 ALL COMPLETED → 员工正式生效
```

---

## 七、看板视图

### 7.1 招聘管道 Kanban 阶段映射

| 看板列 | 对应状态 | 说明 |
|--------|----------|------|
| 新进（NEW） | NEW | 新收到的简历 |
| 筛选中（SCREENING） | SCREENING | 简历筛选/初筛 |
| 面试中（INTERVIEW） | INTERVIEW | 已安排面试或面试中 |
| 待发 Offer（OFFERED） | OFFERED | 面试通过，审批中/待发 Offer |
| 已录用（HIRED） | HIRED | 接受 Offer，待入职 |

> 🟢 Odoo `hr_recruitment` Kanban 视图（drag-and-drop 阶段切换）。
> 🟢 AureusERP `recruitment.php` 面试流程网格视图。

---

## 八、报表与指标

| 指标 | 计算方式 | 用途 |
|------|----------|------|
| 招聘周期 | 平均从需求批准到 HIRED 的天数 | 衡量招聘效率 |
| 面试转化率 | HIRED / INTERVIEW 数 | 衡量面试质量 |
| Offer 接受率 | ACCEPTED / SENT | 衡量 Offer 竞争力 |
| 渠道有效率 | HIRED（来源）/ 总 HIRED | 各渠道质量排名 |
| 人均招聘成本 | 总招聘费用 / HIRED 数 | 成本控制 |
| 在招职位数 | APPROVED 且未 CLOSED 的需求 | 当前工作负载 |
| 试用期留存率 | 通过试用期 / 总入职 × 100% | 招聘质量后验 |

---

## 九、跨域协作

| 对端 | 协作方式 |
|------|---------|
| ErpHrEmployee | HIRED 时创建员工主数据 |
| nop-auth（User） | 入职时可选创建系统账号 |
| nop-file | 简历/Offer/合同附件存储 |

---

## 十、关键业务规则总结

1. **招聘需求审批链**：部门主管 → HR → 部门经理（按紧急程度可简化）
2. **Offer 审批链**：HR → HR 经理 → 部门主管（薪资与编制审核）
3. **候选人状态不可逆**：REJECTED 不可回退（误操作通过黑名单恢复模式处理）
4. **Offer 有效期控制**：超时自动 EXPIRED，需重新审批
5. **入职清单全完成**：一项未完成则员工不可正式激活
6. **黑名单机制**：已标记黑名单的候选人不可再进入招聘管道

## 参考

- `docs/design/human-resource/README.md`（HR 域基础实体）
- `docs/design/human-resource/state-machine.md`（员工状态机）
- `docs/analysis/erp-survey/2026-06-30-0000-aureuserp.md` §HR（recruitments 113 PHP）
- 🟢 Odoo `hr_recruitment` 模块（Kanban 管道 + 面试管理 + 入职清单）
- 🟢 AureusERP `recruitments/`（完整招聘流：request → post → candidate → interview → offer → onboard）
