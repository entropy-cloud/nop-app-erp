# 员工调研设计（Employee Survey）

## 目的

员工调研模块提供问卷/调研创建、分发、匿名回收和结果分析功能，覆盖敬业度调研（Engagement Survey）、脉搏调研（Pulse Survey）、eNPS 等常见 HR 调研场景。支持对调研结果的部门对比、趋势分析和驱动因子分析。

## 边界

- 本模块负责：问卷模板管理、调研计划与分发、匿名/实名回收、结果汇总与仪表盘。
- 本模块不负责：通用调研平台（不包含 NPS 之外的客户调研）；培训需求调研（属 competency/training 域）；问卷的复杂跳题逻辑（如 CATI）。

## 设计依据

> 参考 **Odoo hr_survey** 模块：survey.template（题库）+ survey.survey（调研实例）+ survey.user_input（答卷）+ survey.result（结果聚合）。
>
> 参考 **Culture Amp** 模式：Engagement Survey（30-60 题）+ Pulse Survey（3-10 题）+ eNPS（单题）的问卷分层设计。
>
> 参考 **Glint** 模式：Driver Analysis（驱动因子分析），将问题分类到 Engagement Driver（如成长、认可、管理）并分析各驱动因素对总体 Engagement 的贡献。

## 实体设计

### ErpHrSurvey（问卷模板）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 Odoo survey.survey |
| title | 问卷标题 | 🟢 Odoo survey.survey.title |
| description | 问卷说明 | — |
| surveyType | 调研类型（ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC） | 🟢 Culture Amp 分层 |
| isAnonymous | 是否匿名 | 🟢 Odoo survey.survey.users_can_go_back |
| status | 状态（DRAFT/OPEN/CLOSED/ARCHIVED） | 🟢 Odoo survey.survey.state |
| startDate | 开始日期 | — |
| endDate | 截止日期 | — |
| targetDepartmentId | 目标部门（空=全公司） | — |
| targetEmployeeIds | 指定员工（可选） | — |
| includeENps | 是否包含 eNPS 问题 | — |
| eNpsQuestion | eNPS 题面（默认"你有多大可能向朋友推荐本公司？"） | 🟢 Glint eNPS |
| reminderDays | 未填写催填间隔天数 | — |
| totalQuestions | 总题数（派生） | — |
| totalResponses | 总答卷数（派生） | — |
| completionRate | 完成率（派生） | — |
| avgScore | 平均分（派生） | 🟢 Odoo survey.survey.score |
| eNpsScore | eNPS 得分（派生） | 🟢 Culture Amp eNPS |
| 标准审计字段 | | |

**状态机**：`DRAFT（编辑中）→ OPEN（发布，可填写）→ CLOSED（截止）→ ARCHIVED（归档）`；`OPEN 可直接→ CLOSED`。

### ErpHrSurveyQuestion（问卷题目）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/surveyId | 标准（主从于 ErpHrSurvey） | 🟢 Odoo survey.question |
| sortOrder | 排序号 | 🟢 Odoo survey.question.sequence |
| questionText | 题目内容 | 🟢 Odoo survey.question.title |
| questionType | 题型（RATING/SINGLE_CHOICE/MULTI_CHOICE/OPEN_TEXT/eNPS） | Culture Amp 题型 |
| ratingScaleMin | 评分题最低分（默认 1） | — |
| ratingScaleMax | 评分题最高分（默认 5） | — |
| ratingScaleLabelMin | 评分最低分标签（如"非常不满意"） | — |
| ratingScaleLabelMax | 评分最高分标签（如"非常满意"） | — |
| options | 选择题选项（JSON 数组，如 `["A","B","C"]`） | — |
| driverCategory | 驱动因子分类（如 GROWTH/RECOGNITION/MANAGEMENT/WELLBEING） | 🟢 Glint Driver Analysis |
| isRequired | 是否必填 | — |
| allowAnonymous | 是否允许匿名回答（覆盖问卷级设置） | — |

### ErpHrSurveyResponse（答卷）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/surveyId | 标准 | 🟢 Odoo survey.user_input |
| employeeId | 填写员工（若匿名则略） | 🟢 Odoo survey.user_input.partner_id |
| respondentHash | 匿名应答者哈希标识（用于区分同一人是否重复提交） | — |
| submittedAt | 提交时间 | 🟢 Odoo survey.user_input.create_date |
| timeSpentSeconds | 填写耗时（秒） | — |
| isComplete | 是否完整提交 | — |

### ErpHrSurveyAnswer（回答明细）

| 字段 | 含义 |
|------|------|
| id/responseId | 标准（主从于 ErpHrSurveyResponse） |
| questionId | 对应题目（→ErpHrSurveyQuestion） |
| ratingValue | 评分题分值 |
| selectedOption | 选择题选项 |
| openText | 开放文本回答 |

### ErpHrSurveyResult（调研结果聚合）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/surveyId | 标准 | 🟢 Odoo survey.survey_result |
| departmentId | 部门（空=全公司） | — |
| totalResponses | 各部门答卷数 | — |
| avgScore | 各部门平均分 | — |
| eNpsScore | 各部门 eNPS 得分 | — |
| driverScores | 各驱动因子得分（JSON：`{"GROWTH":4.2,"RECOGNITION":3.8}`） | 🟢 Glint Driver Analysis |
| questionBreakdown | 每题得分明细（JSON） | — |
| trendData | 历史趋势（JSON：历年/次得分数组） | — |
| lastCalculatedAt | 最后计算时间 | — |

## 调研类型说明

### 1. 敬业度调研（Annual Engagement Survey）

- 频率：每季度/半年/年
- 题量：30-60 题
- 典型驱动因子分类：
  | 驱动因子 | 示例题目 |
  |----------|---------|
  | GROWTH（成长） | "我在公司有清晰的职业发展路径" |
  | RECOGNITION（认可） | "我的贡献得到了及时认可" |
  | MANAGEMENT（管理） | "我的直接上级能有效支持我的工作" |
  | WELLBEING（健康） | "我的工作压力在可控范围内" |
  | ALIGNMENT（认同） | "我认同公司的战略方向" |

### 2. 脉搏调研（Pulse Survey）

- 频率：每月/双周
- 题量：3-10 题
- 特点：短小精悍，快速检测情绪变化
- 常用模板：
  - 通用脉搏："你本周的工作状态如何？"（1-5 分）
  - 压力指数："你当前的工作压力水平是？"（1-5 分）
  - 协作感知："你是否有足够的跨部门支持？"（1-5 分）

### 3. eNPS（Employee Net Promoter Score）

- 单题形式："你有多大可能向朋友推荐本公司作为工作场所？"（0-10 分）
- 评分规则：
  - Promoters（推荐者）：9-10 分
  - Passives（中立者）：7-8 分
  - Detractors（贬损者）：0-6 分
- 计算公式：`eNPS = %Promoters - %Detractors`（范围 -100 ~ +100）

## 匿名模式

| 模式 | 说明 |
|------|------|
| FULL_ANONYMOUS | 完全不记录 employeeId，仅存 respondentHash 防重复 |
| DEPARTMENT_ONLY | 记录部门但隐藏员工身份 |
| NAMED | 实名填写（如 360 评估） |

匿名模式下：
- respondentHash = `SHA256(employeeId + surveyId + salt)`，服务端不存储映射
- 催填通过系统通知而非定位具体作答者
- 结果聚合仅展示部门级/公司级视图

## 结果分析

### 分析维度

1. **Score Trends**：同一问卷多期得分趋势图（折线图），按驱动因子分别展示
2. **Department Comparison**：各部门得分横向对比（柱状图），标注显著差异
3. **Driver Analysis**：各驱动因子与总分的相关性分析（散点图+回归线），识别对整体敬业度影响最大的因子
4. **eNPS Trend**：eNPS 得分历史趋势
5. **Open Text Analysis（远期）**：关键词提取和情感分析

### 结果聚合机制

- 调研 CLOSED 时触发自动聚合 → ErpHrSurveyResult
- 支持手动重新聚合（如新增问卷后）
- 聚合历史保留在 trendData 中，对比历史绩效应保持题项一致性（同一问卷模板的多次发版）

## 菜单归属

新增 `hr-survey` 分组：
- 问卷模板管理（ErpHrSurvey-main）
- 调研答卷（ErpHrSurveyResponse-main）
- 结果分析仪表盘（ErpHrSurveyResult-main）

## 参考

- `docs/design/human-resource/README.md`（HR 域 README）
- 🟢 Odoo `hr_survey` 模块源码
- 🟢 Culture Amp 产品文档
- 🟢 Glint 产品文档
