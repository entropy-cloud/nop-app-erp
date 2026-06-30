# 胜任力管理设计（Competency Management）

## 目的

胜任力管理模块提供企业级胜任力字典维护、岗位胜任力要求配置、员工多视角评估和差距分析，支持针对差距自动生成发展计划。帮助组织系统化管理"岗位需要什么能力"和"员工实际具备什么能力"之间的匹配度。

## 边界

- 本模块负责：胜任力字典、胜任力等级锚定、岗位-胜任力矩阵、员工评估（自评/上级/360）、差距分析、发展计划生成。
- 本模块不负责：培训课程管理（与 competency 衔接的训练计划是下游任务）；绩效评估流程（如 KPI/PBC 考核，属未来绩效模块）；人才盘点（继任计划属高级 HR 功能，远期扩展）。

## 设计依据

> 参考 **SAP SuccessFactors Competency Management**：Competency Library（字典库）+ Proficiency Level（1-5 行为锚定）+ Role Competency Mapping（岗位映射）+ Employee Assessment（多评估者）+ Gap Analysis（差距分析）+ Development Plan（发展计划）。
>
> 参考 **Cornerstone OnDemand**：Competency 分类（Skills/Behaviors/Knowledge）+ Rating Scale + Assessment Workflow + Gap Reports。

## 实体设计

### ErpHrCompetency（胜任力字典）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 SuccessFactors Competency Library |
| name | 胜任力名称 | 🟢 SuccessFactors competency.name |
| description | 胜任力描述 | — |
| category | 分类（SKILL/BEHAVIOR/KNOWLEDGE） | 🟢 Cornerstone 分类 |
| competencyGroup | 能力组（如 LEADERSHIP/COMMUNICATION/TECHNICAL） | — |
| isTechnical | 是否技术能力 | — |
| parentId | 上级胜任力（用于层级结构如"编程"→"Java") | — |
| expectedProficiencyLevel | 期望熟练度（可选默认值，按岗位再细化） | — |
| 标准审计字段 | | |

**分类说明**：
| 分类 | 含义 | 示例 |
|------|------|------|
| SKILL | 可观察的实操技能 | 数据分析、项目管理、谈判 |
| BEHAVIOR | 行为特征 | 团队协作、抗压能力、主动性 |
| KNOWLEDGE | 领域知识 | 财务知识、行业法规、产品知识 |

### ErpHrCompetencyLevel（胜任力等级）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/competencyId | 标准（主从于 ErpHrCompetency） | 🟢 SuccessFactors Proficiency Level |
| levelNumber | 等级编号（1-5） | 🟢 SuccessFactors level.number |
| levelName | 等级名称（如"初级/中级/高级/资深/专家"） | — |
| behavioralAnchor | 行为锚定描述（该等级的具体行为表现） | 🟢 SuccessFactors behavioral anchor |
| sortOrder | 排序号 | — |

**典型 5 级量表**：
| 级别 | 名称 | 行为锚定示例（沟通能力） |
|------|------|------------------------|
| 1 | 初级（Novice） | 能在指导下进行基本的信息传递 |
| 2 | 中级（Intermediate） | 能独立完成日常沟通并撰写简单报告 |
| 3 | 高级（Advanced） | 能主导会议、跨部门协调并影响他人 |
| 4 | 资深（Expert） | 能处理复杂谈判、冲突调解和变革沟通 |
| 5 | 大师（Master） | 能制定沟通战略、塑造组织文化 |

### ErpHrRoleCompetency（岗位胜任力要求）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/positionId | 岗位（→ErpHrPosition） | 🟢 SuccessFactors Role Profile |
| competencyId | 胜任力（→ErpHrCompetency） | — |
| requiredLevel | 要求等级（→ErpHrCompetencyLevel.levelNumber） | 🟢 SuccessFactors targetProficiency |
| weight | 权重（用于差距评分计算优先级） | — |
| isCritical | 是否关键胜任力（差距不可忽略） | — |

### ErpHrEmployeeAssessment（员工评估）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/employeeId | 被评估员工 | 🟢 SuccessFactors Employee Assessment |
| assessmentType | 评估类型（SELF/MANAGER/PEER/SUBORDINATE/360） | 🟢 SuccessFactors assessment type |
| assessorId | 评估人（若自评则为同一 employeeId） | — |
| assessmentDate | 评估日期 | — |
| status | 状态（DRAFT/SUBMITTED/COMPLETED） | — |
| overallScore | 综合评分（派生） | — |
| 标准审计字段 | | |

**评估类型说明**：
| 类型 | 含义 | 评估人 |
|------|------|--------|
| SELF | 自评 | 员工本人 |
| MANAGER | 上级评估 | 直接上级 |
| PEER | 同级评估 | 同部门/跨部门同事 |
| SUBORDINATE | 下级评估 | 下属 |
| 360 | 全视角 | 以上全部 |

### ErpHrAssessmentDetail（评估明细）

| 字段 | 含义 |
|------|------|
| id/assessmentId | 标准（主从于 ErpHrEmployeeAssessment） |
| competencyId | 胜任力（→ErpHrCompetency） |
| actualLevel | 实际等级（评估人对该员工该能力的打分） |
| comment | 评语/证据 |
| sourceType | 评估来源类型（冗余，便于按类型聚合） |

### ErpHrGapAnalysis（差距分析）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/employeeId | 员工 | 🟢 SuccessFactors Gap Analysis |
| competencyId | 胜任力 | — |
| requiredLevel | 岗位要求等级（从 ErpHrRoleCompetency 获取） | — |
| actualLevel | 实际等级（聚合各评估源得分） | — |
| gapValue | 差距值 = requiredLevel - actualLevel | — |
| gapSeverity | 差距严重程度（NONE/MINOR/MODERATE/CRITICAL） | — |
| assessmentDate | 评估日期（快照） | — |
| analysisDate | 分析日期 | — |

**差距严重程度规则**：
| gapValue | 严重程度 |
|----------|---------|
| <= 0 | NONE（无差距） |
| 1 | MINOR（轻微差距） |
| 2 | MODERATE（明显差距） |
| >= 3 | CRITICAL（严重差距） |

### ErpHrDevelopmentPlan（发展计划）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/employeeId | 员工 | 🟢 SuccessFactors Development Plan |
| planName | 计划名称 | — |
| targetDate | 目标完成日期 | — |
| status | 状态（DRAFT/IN_PROGRESS/COMPLETED/CANCELLED） | — |
| 标准审计字段 | | |

### ErpHrDevelopmentPlanItem（发展计划项）

| 字段 | 含义 |
|------|------|
| id/planId | 标准（主从于 ErpHrDevelopmentPlan） |
| competencyId | 目标胜任力 |
| gapId | 关联差距分析（→ErpHrGapAnalysis） |
| targetLevel | 目标等级 |
| developmentAction | 发展行动（如"参加 XX 培训""跟岗学习"） |
| trainingCourseId | 推荐培训课程（远期关联培训模块） |
| mentorId | 导师（→ErpHrEmployee） |
| startDate | 开始日期 |
| endDate | 预计完成日期 |
| status | 状态（NOT_STARTED/IN_PROGRESS/ACHIEVED/OVERDUE） |
| progressNote | 进度说明 |

## 评估流程

### 自评流程
1. HR 发起评估周期，创建 ErpHrEmployeeAssessment（assessmentType=SELF）
2. 员工填写各胜任力 level + 自评证据
3. 提交 → status = SUBMITTED
4. HR/上级确认 → status = COMPLETED

### 360 评估流程
1. HR 选择被评估员工、选择评估人（上级+同级+下级+自评）
2. 为每位评估人创建评估记录
3. 各评估人独立填写（互相不可见）
4. 全部提交后触发聚合：按评估类型加权平均 actualLevel
5. 生成差距分析报告

**聚合规则**：`actualLevel = selfWeight × selfLevel + managerWeight × managerLevel + peerWeight × avgPeerLevels + subWeight × avgSubLevels`
默认权重：SELF=15%、MANAGER=50%、PEER=25%、SUBORDINATE=10%（可配置）

## 差距分析与发展计划

### 差距分析触发
- 评估 COMPLETED 后自动触发
- 对比 ErpHrRoleCompetency.requiredLevel 和聚合后的 actualLevel
- 对每个岗位-胜任力组合计算 gapValue
- 标记 gapSeverity

### 发展计划生成
- 针对 CRITICAL/MODERATE 差距自动生成建议发展计划项
- 按权重排序优先处理关键差距
- 支持 HR 手动调整计划项

## 菜单归属

新增 `hr-competency` 分组：
- 胜任力字典（ErpHrCompetency-main）
- 岗位胜任力要求（ErpHrRoleCompetency-main）
- 员工评估（ErpHrEmployeeAssessment-main）
- 差距分析（ErpHrGapAnalysis-main）
- 发展计划（ErpHrDevelopmentPlan-main）

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-hr.assessment-self-weight` | 0.15 | 自评权重 |
| `erp-hr.assessment-manager-weight` | 0.50 | 上级评估权重 |
| `erp-hr.assessment-peer-weight` | 0.25 | 同级评估权重 |
| `erp-hr.assessment-subordinate-weight` | 0.10 | 下级评估权重 |
| `erp-hr.gap-critical-threshold` | 3 | 严重差距阈值 |

## 参考

- `docs/design/human-resource/README.md`（HR 域 README）
- 🟢 SAP SuccessFactors Competency Management 产品文档
- 🟢 Cornerstone OnDemand 产品文档
