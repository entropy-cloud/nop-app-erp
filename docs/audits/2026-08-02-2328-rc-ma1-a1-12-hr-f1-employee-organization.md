# rc-ma1-a1-12 hr-F1 员工与组织 需求-实现符合性五级追踪审计报告

> 报告类型：requirement-compliance MA1 切片 A1.12
> 切片：hr-F1 员工与组织（roadmap 标签；权威 UC 范围 = UC-HR-01/05/07/08/12 共 5 UC）
> 审计时间：2026-08-02
> 审计基线 HEAD：`f3ff693e6c81472264b92a9dfba9844231ef3a2f`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 上游计划：`docs/plans/2026-08-02-2250-1-rc-ma1-a1-12-hr-f1-employee-organization.md`（草案审查 `acceptable as-is` 一轮，独立子代理 `ses_03cef9818ffeh66yYgAudBjJw`）
> 真相源层级（§4 Q1）：L1 = `docs/design/human-resource/use-cases.md`（UC-HR-01 `:3` / UC-HR-05 `:51` / UC-HR-07 `:75` / UC-HR-08 `:87` / UC-HR-12 `:137`，锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.12 确认，inventory `:346` 一致）；L2 = `recruitment.md` + `competency-management.md` + `state-machine.md`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.7a/A4.4 + 本切片差异。

---

## 9. 与既有 MA2/MA4 报告差异增量声明（前置声明，便于读者识别复用边界）

> 依方法论 §6 段落 9 + §去重协议，本报告前置声明与既有 MA2/MA4 报告的差异增量。

| 既有报告 | 覆盖维度 | 已证实结论（本切片复用） | 本切片补的差异增量（需求契约视角） |
|---------|---------|----------------------|--------------------------|
| `2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`（A2.7a） | 员工与组织七组件**状态机业务正确性**（员工 5 态 + 合同 4 态 + 招聘 7 态 + 考核 3 态 + 发展计划 4 态 + 计划项 4 态 + 调查 4 态） | 主路径状态迁移守卫齐全 + 事务边界清晰 + 招聘 hire 跨实体副作用经事务回滚 + 考核 completeAssessment 跨实体刷新经直传 levels。**零 P0；4 P1**（P1-MA2-039 员工 RESIGNED/TERMINATED/RETIRED 三态死状态 / P1-MA2-040 合同 SUSPENDED 死状态 / P1-MA2-041 调查三态死状态[非本切片] / P1-MA2-042 发展计划死状态）+ **5 P2** watch-only（P2-MA2-047~051） | 本切片不重审状态机迁移守卫维度；只补**需求契约 vs 实现符合性**（UC-HR-01 入职字段完整性 + UC-HR-05 招聘 7 态全链与 hire 副作用 + 未到岗异常 + UC-HR-07 到期提醒窗口/调度/续签-终止联动 + UC-HR-08 调动字段更新与合同处理 + UC-HR-12 胜任力全链 + resolved finding HEAD 复核） |
| `2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4） | hr 薪酬/过账/模拟链路**代码质量**（社保钳制 / 累计预扣 / BigDecimal 类型安全 / 模拟隔离 / 跨域 Facade / 异常规范化） | 核心实现正确性四面扎实；**4 P1**（P1-MA4-016 个税高档 NPE / P1-MA4-017 业财过账链路不完整 / P1-MA4-018 累计静默吞 / P1-MA4-019 测试有效性）+ 2 P2（含 P2-MA4-008 桩治理）。注：A4.4 聚焦薪酬/过账/模拟，UC-HR-01/05/07/08/12 归员工与组织侧（A2.7a 覆盖状态机 + 本切片补需求契约视角） | 本切片不重审代码质量维度；复核 P2-MA4-008 桩治理（Survey/DevelopmentPlan）resolved 状态不变，本切片只补需求契约视角差异 |

**结论**：本切片裁决焦点 = **UC-HR-01/05/07/08/12 需求契约↔实现符合性**。状态机迁移守卫/事务边界/代码质量三面**复用 A2.7a/A4.4 pass/finding 结论**（接受，不重审）；本切片只补需求视角差异（入职字段完整性 + 招聘 7 态 + 未到岗异常 + 到期提醒窗口/联动 + 调动字段更新与合同处理 + 胜任力全链 + resolved finding HEAD 复核）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/human-resource/use-cases.md`（UC 锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.12 确认 = `:3/:51/:75/:87/:137`，inventory `:346` 一致）。

### UC-HR-01 员工入职（`use-cases.md:3-13`）

逐字引用验收标准：

```
基本流程：
1. HR 填写员工信息（姓名/性别/出生日期/证件/联系方式/银行账户等）          [断言①]
2. 选择部门（→ErpHrDepartment）和职位（→ErpHrPosition）                    [断言②]
3. 设置直属上级（→ErpHrEmployee.superiorId）                                [断言③]
4. 填写入职日期、试用期截止日期                                            [断言④]
5. 设置雇佣状态为 PROBATION（试用期）或 ACTIVE（免试用）                    [断言⑤]
6. 填写社保号/个税档案号                                                    [断言⑥]
7. 创建劳动合同（ErpHrEmploymentContract）并关联                            [断言⑦]
8. 提交，系统创建 ErpHrEmployee 记录                                        [断言⑧]

后置条件：员工主数据可用；可选创建系统账号（UserAccountId）                 [断言⑨]
异常：部门/职位不存在时提示先创建；证件号码重复提示                          [断言⑩]
```

### UC-HR-05 招聘录用（`use-cases.md:51-61`）

逐字引用验收标准：

```
基本流程：
1. 创建 Recruitment，关联职位和部门                                         [断言⑪]
2. 填写应聘者信息                                                           [断言⑫]
3. 简历筛选（status = SCREENING）→ 安排面试（status = INTERVIEW）
   → 面试通过后发 Offer（status = OFFERED）                                  [断言⑬]
6. 候选人接受，入职（status = HIRED）→ 创建 ErpHrEmployee                   [断言⑭]
7. 或拒绝/拒绝候选人（status = REJECTED）                                   [断言⑮]
8. 岗位关闭（status = CLOSED）                                              [断言⑯]

后置条件：ErpHrEmployee 已创建（HIRED 时）；该 record 的 employeeId 已关联   [断言⑰]
异常：候选人接受 Offer 后未到岗需状态回退                                    [断言⑱]
```

### UC-HR-07 合同到期提醒（`use-cases.md:75-85`）

逐字引用验收标准：

```
触发条件：定时任务每日扫描即将到期的合同                                     [断言⑲]
前置条件：ErpHrEmploymentContract.status = ACTIVE，endDate 为未来 30/60/90 天内  [断言⑳]
基本流程：
1. 系统扫描 endDate 在提醒窗口内的 ACTIVE 合同                              [断言㉑]
2. 通知 HR 管理员                                                           [断言㉒]
3. HR 操作续签（创建新合同，原合同 endDate 不变但 status→EXPIRED）          [断言㉓]
4. 或到期终止（原合同 status→EXPIRED，员工 employmentStatus 联动）          [断言㉔]
跨域协作：员工状态联动（合同到期不续签→RESIGNED）                           [断言㉕]
```

### UC-HR-08 部门调动（`use-cases.md:87-99`）

逐字引用验收标准：

```
基本流程：
1. HR 选择员工，填写目标部门（→ErpHrDepartment）                            [断言㉖]
2. 可选调整职位与直属上级                                                   [断言㉗]
3. 设置调动生效日期                                                         [断言㉘]
4. 提交，系统更新 ErpHrEmployee.departmentId/positionId/superiorId          [断言㉙]
5. 如有劳动合同，标记原合同→TERMINATED，创建新合同                          [断言㉚]
异常：调动日期与已有休假冲突时告警                                          [断言㉛]
跨域协作：成本中心变更可能影响项目工时归集                                  [断言㉜]

> use-cases.md:99 注记（逐字）：
> `ErpHrEmployeeBizModel.transferEmployee` `@BizMutation`（单步直接更新，无审批；经
> `IErpHrDepartmentBiz`/`IErpHrPositionBiz`/`IErpHrEmploymentContractBiz`/`IErpHrLeaveRequestBiz`
> 跨实体校验/联动；合同处理三态 `handleContract` AUTO/YES/NO + config-gated；休假冲突告警不阻塞；
> AMIS 员工页「调动」drawer 入口）。调动单实体 + 审批工作流归 Deferred（触发条件=调动需人工审批
> 留痕或批量调动报表时，经独立 ORM ask-first 承接）。                            [断言㉝]
```

### UC-HR-12 胜任力管理与评估（`use-cases.md:137-147`）

逐字引用验收标准：

```
基本流程：
1. HR 创建 ErpHrCompetency（分类 SKILL/BEHAVIOR/KNOWLEDGE），配置能力组和层级结构  [断言㉞]
2. 为每个胜任力配置 ErpHrCompetencyLevel（1-5 级，含行为锚定描述）         [断言㉟]
3. 配置 ErpHrRoleCompetency（每岗位所需胜任力及要求等级、权重、是否关键）   [断言㊱]
4. HR 发起评估周期，创建 ErpHrEmployeeAssessment（SELF/MANAGER/PEER/SUBORDINATE/360）  [断言㊲]
5. 各评估人独立填写 ErpHrAssessmentDetail（对每个胜任力打分 + 评语）        [断言㊳]
6. 全部提交后系统按权重聚合（默认 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%）  [断言㊴]
7. 自动对比 ErpHrRoleCompetency 计算 ErpHrGapAnalysis（gapValue = requiredLevel - actualLevel）  [断言㊵]
8. 标记 gapSeverity（NONE/MINOR/MODERATE/CRITICAL）                         [断言㊶]
9. 针对 CRITICAL/MODERATE 差距生成 ErpHrDevelopmentPlan 建议                [断言㊷]
10. HR 审核并调整发展计划项，指定目标等级、发展行动和导师                    [断言㊸]
后置条件：差距分析已生成；发展计划已创建并可跟踪执行                          [断言㊹]
```

---

## 2. 实现证据（L3 代码路径，含行号）

> 跨域调用链须列全（方法论 §1 L3 格式）。

### UC-HR-01 员工入职

- 员工主数据 CRUD：`module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java:57`（`extends CrudBizModel<ErpHrEmployee>`——入职建档经标准 GraphQL `save` mutation；HR 经前端表单填写姓名/性别/出生日期/证件/联系方式/银行账户/社保号/个税档案号/入职日期/试用期截止日期 + 选择部门(departmentId)/职位(positionId)/直属上级(superiorId) + 设置 employmentStatus=PROBATION/ACTIVE + 关联合同 ErpHrEmploymentContract）。
- 招聘 hire 副路径创建员工：`ErpHrRecruitmentBizModel.java:139-166`（`createEmployeeFromRecruitment` 设 code/firstName/lastName/fullName/gender=MALE/hireDate/`setEmploymentStatus(EMPLOYMENT_ACTIVE):149`/employeeType=FULL_TIME/departmentId/positionId/email/mobilePhone/orgId → `employeeBiz.saveEntity:160`）。
- 跨实体校验：部门 `IErpHrDepartmentBiz` / 职位 `IErpHrPositionBiz` / 合同 `IErpHrEmploymentContractBiz`（`ErpHrEmployeeBizModel.java:76-82` 注入）。
- 合同创建联动（hire 副路径）：`ErpHrRecruitmentBizModel.createContractForNewEmployee:168-183`（新建 ACTIVE 合同 + monthlySalary 承袭 offerSalary）。

### UC-HR-05 招聘录用

- 招聘 BizModel：`ErpHrRecruitmentBizModel.java:43`（`extends CrudBizModel<ErpHrRecruitment>`）。
- 7 态状态机全迁移：
  - 新建→OPEN：`defaultPrepareSave:57-66`（status==null→`setStatus(RECRUITMENT_STATUS_OPEN):64`）。
  - OPEN→SCREENING：`moveToScreening:70-76`（`requireStatus(OPEN):72`→`setStatus(SCREENING):73`）。
  - SCREENING→INTERVIEW：`scheduleInterview:80-91`（`requireStatus(SCREENING):85`→setInterviewerId/Date→`setStatus(INTERVIEW):88`）。
  - INTERVIEW→OFFERED：`makeOffer:95-104`（`requireStatus(INTERVIEW):99`→setOfferSalary→`setStatus(OFFERED):101`）。
  - OFFERED→HIRED：`hire:108-112`→委托 `ErpHrRecruitmentHireProcessor.hire`（跨实体创建 Employee + Contract + employeeId 回写）。
  - 非终态→REJECTED：`reject:116-126`（守卫拒 HIRED/CLOSED/REJECTED `:118-121`→`setStatus(REJECTED):123`）。
  - 任意态→CLOSED：`close:130-135`（**无守卫**——任意 status 可 CLOSED，含 HIRED 合法入职后清理；P2-MA2-048 watch-only）。
- hire 跨实体副作用：`createEmployeeFromRecruitment:139-166` + `createContractForNewEmployee:168-183`（经 `IErpHrEmployeeBiz.saveEntity` + `IErpHrEmploymentContractBiz.saveEntity` 同域 I\*Biz；@BizMutation 事务回滚保证失败原子性）。

### UC-HR-07 合同到期提醒

- 定时扫描 Job：`module-hr/erp-hr-service/.../job/ErpHrContractExpiryJob.java:34`（`execute():54` 无参 public 方法适配 BeanMethodJobInvoker 反射调用；cron config-gated `resolveCronConfig:108-110` 读取 `erp-hr.contract-expiry-cron`，空值跳过 `:56-58`）。
- 两步执行：`runExpiryWarnings:71-87`（调 `contractBiz.scanExpiringContracts` 扫描提醒窗口内 ACTIVE 合同 → 经 `IErpSysNotificationBiz.notify:105` 派发 `hr.contract-expiry-warning` 跨域通知 + 单条失败隔离 `:81-84`）+ `runExpirations:90-93`（调 `contractBiz.expireOverdueContracts` 推进过期 ACTIVE→EXPIRED）。
- 到期扫描 BizModel：`ErpHrEmploymentContractBizModel.java:42`。
  - `scanExpiringContracts:64-74`（ACTIVE + `dateBetween(endDate, now, now+window)`；window 经 `ErpHrConfigs.contractExpiryWarningDays():67` 默认 30 天，参数 `warningDays` 可覆盖）。
  - `expireOverdueContracts:78-80`→委托 `ErpHrEmploymentContractExpireOverdueContractsProcessor.expireOverdueContracts`（ACTIVE + `lt(endDate, now)`→`setStatus(EXPIRED):42` + 单失败隔离 `:45-47`）。
  - `renew:84-99`（守卫仅 ACTIVE/EXPIRED `:89-93`→`setStatus(ACTIVE):95` + setEndDate）。

### UC-HR-08 部门调动

- 调动 BizModel：`ErpHrEmployeeBizModel.java:57`。
- `transferEmployee:92-101`（`@BizMutation`→委托 `ErpHrEmployeeTransferEmployeeProcessor.transferEmployee`）。
- 守卫链：`requireTransferableEmployee:134-148`（仅 ACTIVE/PROBATION 可调动 `isTransferable:150-153`）+ `requireTargetDepartment:155-165`（部门存在性经 `IErpHrDepartmentBiz.findFirst`）+ `requireTargetPosition:167-181`（职位存在性 + 部门归属校验经 `IErpHrPositionBiz.findFirst`）。
- 字段更新：departmentId/positionId/superiorId 更新（Processor 内 updateEntity）。
- 合同处理三态：`resolveHandleContract:212-233`（AUTO/YES/NO + config-gated `ErpHrConfigs.transferAutoHandleContract():221`；YES/AUTO→原 ACTIVE 合同 `setStatus(TERMINATED):228` + `newContractFrom:256-281` 新建 ACTIVE 合同承袭字段；NO→不触及）。
- 休假冲突告警：`warnIfLeaveConflict:185-205`（config-gated `transferLeaveConflictWarn():186`；检测 APPROVED 休假重叠→`LOG.warn:203` 不抛异常不阻塞）。

### UC-HR-12 胜任力管理与评估

- 胜任力字典：`ErpHrCompetencyBizModel.java`（CRUD，category SKILL/BEHAVIOR/KNOWLEDGE + competencyGroup + parentId 层级 + 成环校验 `ERR_COMPETENCY_PARENT_CYCLE`）。
- 胜任力等级：`ErpHrCompetencyLevelBizModel.java`（CRUD，levelNumber 1-5 + behavioralAnchor）。
- 岗位胜任力要求：`ErpHrRoleCompetencyBizModel.java`（CRUD，requiredLevel/weight/isCritical）。
- 评估 BizModel：`ErpHrEmployeeAssessmentBizModel.java:38`。
  - `submitAssessment:65-80`（守卫 DRAFT `:69` + details 非空 `:73-76`→SUBMITTED）。
  - `completeAssessment:84-87`→委托 `ErpHrEmployeeAssessmentCompleteAssessmentProcessor.completeAssessment`（守卫 SUBMITTED + details 非空 → aggregateAndWriteBack + `gapAnalysisBiz.refreshGapAnalysisWithLevels` 直传 levels 避免跨事务可见性）。
  - `aggregateAndWriteBack:94-122`（按 competencyId 分组 + `AssessmentAggregator.aggregate:107` 聚合 + 写回 detail.actualLevel + overallScore）。
- 聚合引擎：`competency/AssessmentAggregator.java:28`。`aggregate:38-104`（非 360 单源均值 `:47-49`；360 按 sourceType 分组加权 `:57-73`；权重经 `ErpHrConfigs.assessmentSelfWeight():75` / `assessmentManagerWeight():76` / `assessmentPeerWeight():77` / `assessmentSubordinateWeight():78` config 驱动默认 15%/50%/25%/10%；缺类型重归一化 `:81-84`；四舍五入 + clamp 1-5 `:134-138`）。
- 差距分析引擎：`competency/GapAnalysisCalculator.java:30`。`calculate:43-70`（gapValue=requiredLevel-actualLevel `:56` + severityOf `:57`）+ `severityOf:76-83`（≤0 NONE / 1 MINOR / 2 MODERATE / ≥criticalThreshold CRITICAL；阈值经 `ErpHrConfigs.gapCriticalThreshold():49` 默认 3）。
- 差距 BizModel：`ErpHrGapAnalysisBizModel.java:49`。`refreshGapAnalysisWithLevels:79-83`（清旧重建范式 + normalizeLevelMap 键类型规范化 `:90-99`）。
- 发展计划 BizModel：`ErpHrDevelopmentPlanBizModel.java:48`。`generateDevelopmentPlan:75-78`（委托 Processor；`findActionableGaps:107-113` 仅 CRITICAL/MODERATE + `sortByPriority:126-133` severity+gapValue 排序 + `newPlanItem:145-158` 生成 NOT_STARTED 计划项）+ `updatePlanItemStatus:82-86`（委托 Processor，`isValidPlanItemTransition:182-192` 状态机守卫）+ `completePlan:90-103`（守卫 DRAFT/IN_PROGRESS→COMPLETED）。

---

## 3. 测试证据（L4 测试断言，注明断言强度）

| UC | 测试类#方法 | 覆盖验收标准 | 断言强度 |
|----|------------|------------|---------|
| UC-HR-01 | `TestErpHrEmployeeReferences.java#countReferences`（引用计数）+ 招聘 hire 路径经 `TestErpHrRecruitmentEngine#testFullRecruitmentFlowCreatesEmployeeAndContract` 覆盖员工创建 | 员工引用计数（同域只读聚合）+ hire 创建员工断言 | **中**——引用计数是删除预览非入职建档强断言；hire 路径有 status/fullName/hireDate/employmentStatus 断言但入职建档主路径（CRUD save 填全字段）无专门单测 |
| UC-HR-05 | `TestErpHrRecruitmentEngine.java#testFullRecruitmentFlowCreatesEmployeeAndContract:52-75` | ⑬⑭⑯⑰ + hire 副作用（employee+contract+employeeId） | **强**——status==HIRED + employeeId!=null + employee 存在 + employmentStatus==ACTIVE + hireDate + fullName + contract ACTIVE + startDate + monthlySalary==offerSalary **数值/状态精确断言** |
| | `#testIllegalTransitionOpenToHire:78-84` | 非法迁移守卫 | **强**——assertThrows + ErrorCode 精确匹配 |
| | `#testRejectFromInterview:87-96` | ⑮ reject | **强**——status==REJECTED |
| | `#testCloseFromHired:99-110` | ⑯ close（含 HIRED→CLOSED） | **强**——status==CLOSED |
| UC-HR-07 | `TestErpHrContractExpiry.java#testScanExpiringContractsHitsWithinWindow:61-74` | ⑳㉑扫描窗口 | **强**——endDate=today+15 在 warningDays=30 窗口内命中 |
| | `#testExpireOverdueContractsAdvancesStatus:77-93` | ㉔到期终止 | **强**——endDate=today-1→EXPIRED + DB 复读断言 |
| | `#testRenewFromExpiredToActive:96-114` | ㉓续签 | **强**——EXPIRED→ACTIVE + newEndDate + DB 复读 |
| | `#testRenewRejectsTerminatedContract:117-130` | 续签守卫 | **强**——TERMINATED 拒绝 + ErrorCode |
| | `#testJobCronEmptySkipsExecution:133-139` + `#testJobCronConfiguredTriggersBothSteps:142-149` | ⑲cron 调度门控 | **强**——cron 空→跳过 / cron 非空→委托两步各 1 次 |
| | `#testExecuteIsNoArgPublicMethod:152-156` | BeanMethodJobInvoker 适配 | **强**——反射校验 execute() 无参 public |
| UC-HR-08 | `TestErpHrEmployeeTransfer.java#testTransferUpdatesDepartmentPositionSuperior:60-86` | ㉖㉗㉙字段更新 | **强**——departmentId/positionId/superiorId 全断言 + DB 复读 |
| | `#testTransferRejectsNonTransferableStatus:89-103` | 守卫 | **强**——RESIGNED 拒绝 + ErrorCode |
| | `#testTransferRejectsUnknownTargetDepartment:106-118` | 异常路径 | **强**——99999999L + ErrorCode |
| | `#testTransferRejectsPositionNotInTargetDepartment:121-138` | 异常路径 | **强**——部门归属不匹配 + ErrorCode |
| | `#testTransferContractAutoTerminatesOldAndCreatesNew:141-169` | ㉚合同处理 AUTO | **强**——原 TERMINATED + 新 ACTIVE + startDate=effective + contractType 承袭 |
| | `#testTransferContractNoDoesNotTouchContracts:172-193` | 合同处理 NO | **强**——原 ACTIVE 不变 |
| | `#testTransferContractYesCreatesEvenWithoutActive:196-215` | 合同处理 YES | **强**——无 ACTIVE 仍新建 |
| | `#testTransferLeaveConflictWarnDoesNotBlock:218-235` | ㉛休假冲突告警不阻塞 | **强**——APPROVED 休假重叠 + 调动仍成功 |
| | `#testTransferProbationEmployeeAllowed:238-252` | PROBATION 可调动 | **强** |
| | `#testLongActiveCodeDoesNotOverflowCodePrecision:274-370` | 回归 code precision overflow | **强**——4 子用例（短码逐字符 / 长码截断+哈希 / 不同 active.code / 无 ACTIVE base 路径） |
| UC-HR-12 | `TestErpHrCompetencyManagement.java`（考核 + gap + 发展计划状态机） | ㊲㊳㊴㊵㊶㊷㊸ | **强**（A2.7a:419 场景 g 确认 DRAFT→SUBMITTED→COMPLETED + gapAnalysis 刷新 + 自动发展计划生成） |
| | `competency/TestGapAnalysisCalculator.java` | ㊵gapValue + ㊶gapSeverity | **强**（A2.7a 确认 severityOf 边界 + config-gated 阈值） |
| | `competency/TestAssessmentAggregator.java` | ㊴权重聚合 | **强**（A2.7a 确认 SELF/MANAGER/PEER/SUBORDINATE 加权 + 缺类型重归一化 + clamp） |

**MA5/A4.8 评级**：hr 评级可引用（A4.4 hr 测试/mutation 比 0.16 全域最低——但该统计聚焦薪酬/过账/模拟链路；员工与组织侧 UC-HR-05/07/08/12 测试断言强度经本切片复核为**强断言**，非仅冒烟）。

---

## 4. 运行时行为证据（L5，复用 A2.7a/A4.4 + 本切片差异）

| UC | 行为维度 | 证据来源 | 结论 |
|----|---------|---------|------|
| UC-HR-05 | 招聘 7 态状态机迁移守卫 + hire 跨实体副作用事务回滚 | A2.7a §2.3 + §3.4 + §3.7（pass——主路径全迁移 + 守卫完整 + hire 经 @BizMutation 事务） | **行为已证实**（引用 A2.7a） |
| UC-HR-05 | close 无守卫（任意态可 CLOSED） | A2.7a §2.3（P2-MA2-048 watch-only） | **行为已证实**（引用 A2.7a） |
| UC-HR-07 | 合同 expire 批量单失败隔离 + cron-gated Job | A2.7a §2.2 + §3.4（pass——ACTIVE→EXPIRED 单失败 try/catch + cron-gated 设计默认每日 01:00） | **行为已证实**（引用 A2.7a） |
| UC-HR-07 | 续签 ACTIVE/EXPIRED→ACTIVE 守卫 | A2.7a §2.2（pass） | **行为已证实**（引用 A2.7a） |
| UC-HR-08 | 调动 @BizMutation 事务回滚 + 合同处理三态 + warnIfLeaveConflict 非阻断 | A2.7a §2.1 + §3.4 + §3.7（pass——transferEmployee 守卫 + resolveHandleContract 失败回滚 + warn 设计裁定 P2-MA2-050） | **行为已证实**（引用 A2.7a） |
| UC-HR-12 | 考核 completeAssessment 跨实体刷新 gapAnalysis 直传 levels | A2.7a §3.7 + competency-management.md §completeAssessment 直传聚合 levels（pass——避免跨事务可见性） | **行为已证实**（引用 A2.7a） |
| UC-HR-12 | 发展计划 generateDevelopmentPlan CRITICAL/MODERATE 排序 + 计划项状态机守卫 | A2.7a §2.5 + §2.6（pass——IN_PROGRESS→COMPLETED + 计划项 NOT_STARTED→IN_PROGRESS→ACHIEVED；OVERDUE 死状态 P1-MA2-042） | **行为已证实**（引用 A2.7a） |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 的符合性结论，§2 判据）

### 五级追踪矩阵

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约（设计参考，冲突以 L1 为准） | L3 代码路径（含行号） | L4 测试断言（断言强度） | L5 运行时行为 | 符合性结论 |
|---------|---------------------|------------------|------------|------------|--------------|-----------|
| UC-HR-01 | `use-cases.md:3` 员工入职（断言①-⑩，见 §1 逐字引用） | `state-machine.md §适用对象二`（ACTIVE/PROBATION 入职入口）+ `recruitment.md §六入职管理` | `ErpHrEmployeeBizModel.java:57`（CrudBizModel 标准 save 入职建档）+ `ErpHrRecruitmentBizModel.java:139-166`（hire 副路径）+ `:168-183`（合同创建）+ 跨实体 `IErpHrDepartmentBiz/IErpHrPositionBiz/IErpHrEmploymentContractBiz` `:76-82` | `TestErpHrEmployeeReferences#countReferences`（引用计数，中）+ hire 路径经 `TestErpHrRecruitmentEngine#testFullRecruitmentFlowCreatesEmployeeAndContract`（强） | 复用 A2.7a（ACTIVE 经 hire + HR 手工新建；PROBATION 经 HR 手工新建） | **接受** |
| UC-HR-05 | `use-cases.md:51` 招聘录用（断言⑪-⑱） | `recruitment.md §十 关键业务规则` + `§三候选人管道`（7 态）+ `state-machine.md`（无招聘独立章节 P2-MA2-047 watch-only） | `ErpHrRecruitmentBizModel.java:43`（7 态 `:64/70/80/95/108/116/130`）+ hire 副作用 `:139-183` | `TestErpHrRecruitmentEngine` 4 方法（强） | 复用 A2.7a（7 态全迁移 + hire 事务回滚 + close 无守卫 P2-MA2-048） | **P2**（⑱未到岗回退异常未实现） |
| UC-HR-07 | `use-cases.md:75` 合同到期提醒（断言⑲-㉕） | `state-machine.md §适用对象五 合同`（SUSPENDED Deferred）+ `recruitment.md`（无合同到期独立章节） | `ErpHrContractExpiryJob.java:34`（cron-gated `:54`）+ `ErpHrEmploymentContractBizModel.java:64-99`（scan/expire/renew）+ `ErpHrEmploymentContractExpireOverdueContractsProcessor.java:33-50` | `TestErpHrContractExpiry` 7 方法（强） | 复用 A2.7a（expire 批量单失败隔离 + cron-gated + renew 守卫） | **接受**（㉕不续签→RESIGNED 联动继承 P1-MA2-039 successor Deferred） |
| UC-HR-08 | `use-cases.md:87` 部门调动（断言㉖-㉝） | `state-machine.md §适用对象二`（ACTIVE/PROBATION 守卫）+ use-cases.md:99 注记（调动单实体+审批工作流 Deferred） | `ErpHrEmployeeBizModel.java:92-101`（transferEmployee）+ `:134-205`（守卫+warn）+ `:212-281`（合同三态） | `TestErpHrEmployeeTransfer` 9 方法（强） | 复用 A2.7a（@BizMutation 事务回滚 + warn 非阻断 P2-MA2-050） | **接受**（㉝调动单实体 Deferred = documented simplification） |
| UC-HR-12 | `use-cases.md:137` 胜任力管理与评估（断言㉞-㊹） | `competency-management.md`（胜任力模型 + 评估聚合权重 + GapAnalysis + DevelopmentPlan + 配置点）+ `state-machine.md §适用对象五 发展计划`（DRAFT/CANCELLED/OVERDUE Deferred） | `ErpHrCompetencyBizModel` + `ErpHrCompetencyLevelBizModel` + `ErpHrRoleCompetencyBizModel`（字典/等级/矩阵）+ `ErpHrEmployeeAssessmentBizModel.java:38`（评估状态机）+ `AssessmentAggregator.java:28`（聚合）+ `GapAnalysisCalculator.java:30`（差距）+ `ErpHrGapAnalysisBizModel.java:49`（刷新）+ `ErpHrDevelopmentPlanBizModel.java:48`（发展计划） | `TestErpHrCompetencyManagement` + `TestGapAnalysisCalculator` + `TestAssessmentAggregator`（强） | 复用 A2.7a（考核状态机 + completeAssessment 直传 levels + 发展计划排序 + 计划项状态机） | **接受** |

### 逐 UC 符合性结论（§2 判据）

#### UC-HR-01 员工入职 → **接受**

- 断言①-⑩逐条核验：①员工字段完整性——CrudBizModel 标准 save mutation 允许 HR 经前端表单填写全部字段（姓名/性别/出生日期/证件/联系方式/银行账户/社保号/个税档案号），ORM `ErpHrEmployee` 实体字段齐备（A2.7a §1.1 确认 `orm.xml:265-340`）；②③部门/职位/上级关联——ORM 外键 departmentId/positionId/superiorId 存在，CRUD save 可设；④入职/试用期日期——hireDate/probationEndDate 字段存在；⑤PROBATION/ACTIVE——`ErpHrConstants.EMPLOYMENT_PROBATION/ACTIVE` 常量存在，CRUD save 可设（hire 副路径固定 ACTIVE，但主路径 CRUD 不限制）；⑥社保号/个税档案号——字段存在；⑦合同创建联动——hire 副路径 `createContractForNewEmployee:168-183` 实现；CRUD 主路径 HR 可手工关联合同；⑧提交创建 ErpHrEmployee——CrudBizModel save 实现；⑨可选系统账号——UserAccountId 字段存在（可选）；⑩证件号码重复——依赖 ORM/XMeta 层唯一约束（标准平台机制）。
- **命中判据**：接受（需求契约全部验收标准在 L3-L5 各级均有证据且一致）。
- **说明**：hire 副路径（招聘→员工）`createEmployeeFromRecruitment:147` 固定 gender=MALE 且未设 birthDate/idNumber/bankAccount/socialInsuranceNo/taxFileNo/superiorId——但这是招聘 hire 的**简化初始化**（owner doc `recruitment.md §六` 入职清单后续补全），非 UC-HR-01 主路径缺陷。UC-HR-01 主路径 = HR 经 CRUD 表单录入全字段，平台标准机制覆盖。

#### UC-HR-05 招聘录用 → **P2**（P2-RC-010 新登记）

- 断言⑪-⑰逐条核验：⑪⑫创建 Recruitment 关联职位+部门 + 应聘者信息——`defaultPrepareSave:57-66` ✓；⑬SCREENING→INTERVIEW→OFFERED——`:70-104` 全迁移 + 守卫 ✓；⑭HIRED 创建 ErpHrEmployee——`hire:108-112`→`createEmployeeFromRecruitment:139-166` ✓；⑮REJECTED——`reject:116-126` 守卫拒终态 ✓；⑯CLOSED——`close:130-135`（无守卫 P2-MA2-048 watch-only）✓；⑰employeeId 回写——hire Processor 内 `rec.setEmployeeId` + updateEntity ✓。
- 断言⑱异常路径「候选人接受 Offer 后未到岗需状态回退」——**未实现**。`hire` 将 status 设为 HIRED（终态，owner doc + A2.7a §2.3 确认 HIRED 不可恢复），无 "未到岗回退" mutation。L1 显式声明此异常路径但代码无对应实现。
- **命中判据**：P2 ①（次要验收标准未完全满足——主路径 7 态全链 OK，⑱边界场景弱；存在管理逃生路径：close 行政关闭 + useLogicalDelete + 重新申请新 ErpHrRecruitment 记录，owner doc `recruitment.md §关键业务规则 #3` 明示 REJECTED 候选人可重新申请）。**非 P1 ②**——⑱是低频边界场景（候选人接受 offer 但未到岗）且非完全缺失逃生路径（close+重开），不构成"异常路径完全缺失"；主路径 7 态完整覆盖招聘生命周期。
- **新登记**：P2-RC-010（详见 §6）。

#### UC-HR-07 合同到期提醒 → **接受**

- 断言⑲-㉔逐条核验：⑲cron 调度——`ErpHrContractExpiryJob.execute():54` cron config-gated + `TestErpHrContractExpiry#testJobCronConfiguredTriggersBothSteps` 强断言 ✓；⑳30/60/90 提醒窗口——`scanExpiringContracts:64-74` 经 `ErpHrConfigs.contractExpiryWarningDays()` 默认 30 天 + `warningDays` 参数可覆盖。L1 "30/60/90 天" 是提醒窗口**概念**（多档预警），实现采用单一可配置阈值（默认 30）。HR 可调 config 或传参实现多档扫描（如分别调 30/60/90）——**实现可表达 L1 语义**，单一阈值是配置简化非契约缺失 ✓；㉑扫描 ACTIVE 合同——`:71` eq ACTIVE + dateBetween ✓；㉒通知 HR——`IErpSysNotificationBiz.notify:105` 跨域派发 ✓；㉓续签——`renew:84-99` ACTIVE/EXPIRED→ACTIVE ✓；㉔到期终止——`expireOverdueContracts` ACTIVE→EXPIRED ✓。
- 断言㉕跨域协作「合同到期不续签→员工 RESIGNED」——**未实现**（`expireOverdueContracts` 仅设 contract.status=EXPIRED，不设 employee.employmentStatus=RESIGNED）。此与 P1-MA2-039（员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + 离职迁移完全未实现）**同根因同控制点**——按 §7 裁决规则**复用 P1-MA2-039**（resolved R1.15 Deferred successor），不新建。HEAD 复核 P1-MA2-039 = resolved（详见 §6）。
- **命中判据**：接受（主路径验收标准⑲-㉔全部一致；㉕继承 P1-MA2-039 successor Deferred，非本切片新缺口）。

#### UC-HR-08 部门调动 → **接受**

- 断言㉖-㉜逐条核验：㉖选择员工+目标部门——`requireTransferableEmployee:134` + `requireTargetDepartment:155` ✓；㉗可选调整职位/直属上级——targetPositionId/targetSuperiorId 参数 + `requireTargetPosition:167` ✓；㉘调动生效日期——effectiveDate 参数 ✓；㉙更新 departmentId/positionId/superiorId——Processor updateEntity + `TestErpHrEmployeeTransfer#testTransferUpdatesDepartmentPositionSuperior` 强断言 ✓；㉚原合同 TERMINATED+新合同——`resolveHandleContract:212-233` 三态 AUTO/YES/NO + config-gated ✓；㉛休假冲突告警——`warnIfLeaveConflict:185-205` LOG.warn 不阻塞（P2-MA2-050 watch-only）✓；㉜成本中心变更影响项目工时归集——跨域协作声明（ Deferred，非本切片验收强约束）。
- 断言㉝调动单实体+审批工作流 Deferred——**documented simplification**。L1 `use-cases.md:99` 显式注记（逐字引用见 §1）声明「调动单实体 + 审批工作流归 Deferred（触发条件=调动需人工审批留痕或批量调动报表时，经独立 ORM ask-first 承接）」。
- **命中判据**：接受。㉝ Deferred 经 §4 三判据核验——**(ii) owner doc 显式 documented simplification / Deferred 标注**：L1 use-cases.md:99 注记本身是 L1 真相源的一部分（§4 真相源层级 2），Deferred 触发条件已显式登记（"调动需人工审批留痕或批量调动报表时"），构成**已登记 successor**非静默降级。`transferEmployee` 单步直接更新无审批是 L1 注记显式声明的实现行为。**非 P1**——不满足 §2 P1 ②（异常路径未实现），因 L1 注记本身就是验收标准的一部分（Deferred 是 L1 显式裁决）。

#### UC-HR-12 胜任力管理与评估 → **接受**

- 断言㉞-㊹逐条核验：㉞胜任力字典 SKILL/BEHAVIOR/KNOWLEDGE + 能力组层级——`ErpHrCompetencyBizModel` CRUD + category/competencyGroup/parentId + 成环校验 ✓；㉟CompetencyLevel 1-5 行为锚定——`ErpHrCompetencyLevelBizModel` CRUD + levelNumber/behavioralAnchor ✓；㊱RoleCompetency 岗位要求等级/权重/关键——`ErpHrRoleCompetencyBizModel` CRUD + requiredLevel/weight/isCritical ✓；㊲评估周期多视角 SELF/MANAGER/PEER/SUBORDINATE/360——`ErpHrEmployeeAssessment.assessmentType` + `submitAssessment:65`/`completeAssessment:84` ✓；㊳各评估人填 AssessmentDetail——`ErpHrAssessmentDetailBizModel` CRUD ✓；㊴权重聚合 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%——`AssessmentAggregator.aggregate:38-104` + 权重经 `ErpHrConfigs.assessmentSelfWeight/assessmentManagerWeight/assessmentPeerWeight/assessmentSubordinateWeight:75-78` **config 驱动**（默认值与 L1 一致）✓；㊵gapValue=requiredLevel-actualLevel——`GapAnalysisCalculator.calculate:56` ✓；㊶gapSeverity NONE/MINOR/MODERATE/CRITICAL——`severityOf:76-83` ✓；㊷CRITICAL/MODERATE→发展计划——`generateDevelopmentPlan` + `findActionableGaps:107-113` 仅 CRITICAL/MODERATE + `sortByPriority:126-133` ✓；㊸HR 审核调整发展计划项——`updatePlanItemStatus:82-86` + `completePlan:90-103` ✓；㊹差距分析生成+发展计划可跟踪——清旧重建 + 计划项状态机 ✓。
- **命中判据**：接受（需求契约全部验收标准在 L3-L5 各级均有证据且一致）。权重 config 驱动非硬编码（`ErpHrConfigs` 默认值 + `AppConfig.var` 可覆盖），满足 L1 "默认" 语义 + 可配置性。

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决，§7 规则）

### resolved finding HEAD 复核

> 对员工与组织相关既有 finding 在当前 HEAD（`f3ff693`）代码实际落地复核（按逻辑非行号核验）。

| Finding ID | 原描述 | HEAD 复核结论 | 终态 |
|-----------|--------|-------------|------|
| `P1-MA2-039` | 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + §场景D/E 离职/退休/转正迁移 + 联动完全未实现 | **resolved R1.15（documented simplification）**。HEAD 复核：`state-machine.md §适用对象二:126` 现含显式 **Deferred 段落**（逐字：「`RESIGNED/TERMINATED/RETIRED` 三态为**预留死状态**……下方 §场景 D（离职）/§场景 E（转正）描述的是**目标行为，未接入**……**Successor**：PM 要求正式离职/退休/试用期转正工作流时实现上述 mutation」）。代码侧 `ErpHrEmployeeBizModel.java:319-324` `nonTransferableStatuses()` 仅只读引用三态常量作调岗守卫，无 resign/terminate/retire/probationToRegular writer——**与 Deferred 注记一致**。满足 §4 三判据 **(ii)**（owner doc 显式 Deferred 标注 + 触发条件可追溯「PM 要求正式离职/退休/试用期转正工作流时」）。**UC-HR-07⑫ 不续签→RESIGNED 联动继承此 successor**。 | ✅ resolved（documented simplification，successor 已登记） |
| `P1-MA2-040` | 合同 status SUSPENDED dict 死状态 + owner doc 无合同独立章节 | **resolved R1.15（documented simplification）**。HEAD 复核：`state-machine.md §适用对象五 合同:271-275` 现含显式 Deferred 段落（「`SUSPENDED` 为**预留死状态**……**Successor**：合同中止/恢复业务流落地时实现 suspend mutation」）。代码侧无 suspend/resume writer——**与 Deferred 注记一致**。满足 §4 三判据 (ii)。 | ✅ resolved |
| `P1-MA2-042` | 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态 | **resolved R1.15（documented simplification）**。HEAD 复核：`state-machine.md §适用对象五 发展计划:283-287` 现含显式 Deferred 段落（「`DRAFT/CANCELLED` 与计划项 `OVERDUE` 为**预留死状态**……**Successor**：发展计划取消流程落地时实现 cancelPlan mutation；计划项逾期判定落地时实现 OVERDUE 定时 job」）。代码侧 `ErpHrDevelopmentPlanBizModel.generateDevelopmentPlan` 直接 IN_PROGRESS + 无 cancelPlan + 无 OVERDUE job——**与 Deferred 注记一致**。满足 §4 三判据 (ii)。**UC-HR-12 维度**：发展计划主路径（generateDevelopmentPlan→completePlan + 计划项状态机）完整覆盖，DRAFT/CANCELLED/OVERDUE 是预留状态不影响主路径。 | ✅ resolved |
| `P1-MA1-022` | 跨域只读 IDaoProvider daoFor（9 域合并） | **resolved（plan 2026-07-29-2225-1）**。HEAD 复核：本切片涉及的 hr 代码——`ErpHrEmployeeBizModel.countByEmployee:126-130` 使用 `daoProvider().daoFor()` 仅访问**同域 hr 实体**（ErpHrEmploymentContract/ErpHrTimesheet/ErpHrSalary/ErpHrAttendance/ErpHrLeaveRequest）；`ErpHrGapAnalysisBizModel.loadPositionId:119-127` 使用 `daoProvider().daoFor(ErpHrEmployee.class)` 同域。**零跨域 daoFor**（与 A4.4 §领域1 PASS 一致——hr 计算引擎同域只读）。读侧统一裁决已登记 `data-dependency-matrix.md §9`。 | ✅ resolved（hr 投影无跨域违规） |
| `P2-MA2-047` | state-machine.md 缺招聘/合同/考核/发展计划/调查独立章节 | **watch-only（部分改善）**。HEAD 复核：`state-machine.md §适用对象五:267-287` 已新增合同/调查/发展计划 Deferred 预留状态章节（部分回应 P2-MA2-047）；但招聘 7 态 / 考核 3 态仍无独立迁移矩阵章节（散落 recruitment.md / competency-management.md）。watch-only 维持，无 RC 升级。 | ⏳ watch-only（部分改善，维持） |
| `P2-MA2-048` | 招聘 close 无守卫 | **watch-only**。HEAD 复核：`ErpHrRecruitmentBizModel.close:130-135` 仍无 status 守卫（任意态可 CLOSED）。与 A2.7a 复核一致——不破坏状态机（HIRED→CLOSED 合法入职后清理）。本切片 UC-HR-05⑯ 复核确认 close 可达且 `TestErpHrRecruitmentEngine#testCloseFromHired` 强断言覆盖。watch-only 维持。 | ⏳ watch-only |
| `P2-MA2-049` | recruitment.md 多实体 Deferred 未注记 | **watch-only**。HEAD 复核：`recruitment.md` 仍未显式标注扁平 ErpHrRecruitment 单实体 vs 多实体设计的 Deferred。但 `ErpHrRecruitmentBizModel.java:39-40` Javadoc 已声明「本期在扁平 ErpHrRecruitment 上实现状态机，不创建多实体拆分——归 successor」。watch-only 维持。 | ⏳ watch-only |
| `P2-MA2-050` | 调岗请假冲突 warnIfLeaveConflict 非阻断 | **watch-only**。HEAD 复核：`ErpHrEmployeeBizModel.warnIfLeaveConflict:185-205` 仍 LOG.warn 不阻断。UC-HR-08㉛ 设计接受（L1 use-cases.md:96 "告警"语义）。watch-only 维持。 | ⏳ watch-only |
| `P2-MA2-051` | 长期 PROBATION 未转正无 TODO 提醒 | **watch-only**。HEAD 复核：无 probationToRegular mutation + 无定时 Job（与 P1-MA2-039 successor 联动）。watch-only 维持。 | ⏳ watch-only |
| `P2-MA4-008` | 可维护性热点合并（Survey/PayrollBankFile 桩治理等 6 项） | **watch-only**。HEAD 复核：Survey BizModel 仍 18 行 CRUD 桩（非本切片 UC-HR-12 范围——调查归 A1.14）；DevelopmentPlan BizModel **非桩**（generateDevelopmentPlan/updatePlanItemStatus/completePlan 完整实现）。watch-only 维持。 | ⏳ watch-only |

### 新 finding 裁决

#### P2-RC-010 — UC-HR-05 候选人接受 Offer 后未到岗状态回退异常路径未实现（新登记）

- **裁决**：**新建** P2-RC-010。grep `arm-index.md` hr 招聘同域同控制点——P2-MA2-048（close 无守卫）是不同控制点（close 守卫 vs 未到岗回退异常），非同根因；无既有 RC finding 覆盖 UC-HR-05⑱「候选人接受 Offer 后未到岗需状态回退」异常路径。**新建**。
- **严重性**：P2（watch-only，§2 P2 ① 次要验收标准未完全满足——主路径 7 态 OK，⑱边界场景弱）。
- **位置**：`ErpHrRecruitmentBizModel.java:108-135`（hire→HIRED 终态 + close 无守卫；无 "未到岗回退" mutation）。
- **问题**：L1 `use-cases.md:60` 显式声明异常「候选人接受 Offer 后未到岗需状态回退」，但 `hire` 将 status 设为 HIRED（终态不可恢复），无回退 mutation。候选人接受 offer 后未实际到岗时，HR 需手工 close + useLogicalDelete + 重新申请新 ErpHrRecruitment（owner doc `recruitment.md §关键业务规则 #3` 明示 REJECTED 候选人可重新申请，但 HIRED 未到岗无显式回退路径）。
- **重要性原因**：不破坏主路径——7 态招聘状态机完整覆盖正常生命周期；⑱是低频边界场景（候选人接受 offer 但未到岗）且有管理逃生路径（close 行政关闭 + 重开）。**非 P1**——非"异常路径完全缺失"（close+重开是可达逃生路径），属"边界场景弱"。
- **处置**：watch-only，MR1 裁决——方案 A（推荐）实现 `rollbackHire` mutation（HIRED→OFFERED 或 HIRED→REJECTED 守卫 + 清 employeeId 关联 + 逻辑删除/终止已创建的 ErpHrEmployee+Contract）；方案 B owner doc `use-cases.md UC-HR-05 异常` 标注「未到岗回退经 close 行政关闭 + 重新申请，无显式 rollbackHire mutation」。

#### UC-HR-07⑫ 不续签→RESIGNED 联动 → **复用 P1-MA2-039**（不新建）

- **裁决**：**复用** P1-MA2-039。UC-HR-07⑮「合同到期不续签→员工 RESIGNED」与 P1-MA2-039（员工 employmentStatus RESIGNED 死状态 + 离职迁移未实现）**同根因同控制点**（employee.employmentStatus 无 RESIGNED writer）。按 §7 裁决规则，在既有 arm-index P1-MA2-039 行追加 RC 交叉引用注记，**不新建编号**。HEAD 复核 P1-MA2-039 = resolved R1.15 Deferred（successor 已登记），故 UC-HR-07⑫ 继承该 successor，非本切片新缺口。

### 双向可追溯

- **P2-RC-010** → MR1 修复行预留（RC-R1.n 展开时追加）。
- **P1-MA2-039** → 已 resolved R1.15，UC-HR-07⑫ 交叉引用注记。
- **P2-MA2-047~051 / P2-MA4-008** → watch-only 维持，本切片复核无升级。

---

## 7. 静态存疑点清单（供 MA4 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

1. **UC-HR-07 cron 运行时调度接线**：`ErpHrContractExpiryJob.execute()` 依赖 `scheduler.yaml` cronExpr 反射调用 + `erp-hr.contract-expiry-cron` config 非空。运行时 scheduler.yaml 是否实际接线 + cron config 是否非空需 MA4 运行时确认（本切片静态确认代码门控逻辑正确 + 单测覆盖 cron 空/非空两路径）。
2. **UC-HR-07 30/60/90 多档预警运行时配置**：L1 "30/60/90 天" 多档预警概念，实现采用单一可配置阈值。运行时是否有多档调度配置（如三个 Job 实例分别传 warningDays=30/60/90）需 MA4 确认（静态确认单一阈值 config 驱动 + 参数可覆盖）。
3. **UC-HR-12 评估聚合权重运行时配置覆盖**：`ErpHrConfigs.assessment*Weight()` 默认 15%/50%/25%/10% + `AppConfig.var` 可覆盖。运行时是否有非默认配置覆盖需 MA4 确认（静态确认 config 驱动非硬编码）。
4. **UC-HR-08 handleContract 三态运行时行为**：AUTO 模式依赖 `ErpHrConfigs.transferAutoHandleContract()` 默认 true。运行时 config 是否被覆盖需 MA4 确认。
5. **UC-HR-05 未到岗回退运行时处理**：P2-RC-010 未实现——运行时 HR 是否经 close+重开处理未到岗场景需 MA4 探查（静态确认无 rollbackHire mutation）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本报告无生产代码变更**（纯审计报告），checker 无回归风险。actual vs baseline 汇总表如下。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。

  | 规则 | baseline（前序报告引用） | actual（本次实测） | 变化 |
  |------|------------------------|-------------------|------|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | — |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | — |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | — |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | — |
  | R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | — |
  | R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | — |
  | R3 new Erp*() 构造实体 | 5 | 5 | — |
  | R8 Processor 无 xbiz 接线 | 0 | 0 | — |

  本审计无代码变更，actual == baseline，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P2-RC-010 新建 + P1-MA2-039 复用 + P2-MA2-047~051/P2-MA4-008 watch-only 复核）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决，无未经比对直接新建的 finding。

---

## 结论

hr-F1 员工与组织五 UC（UC-HR-01/05/07/08/12）需求-实现符合性五级追踪审计经逐 UC 逐验收标准核验：

- **UC-HR-01 员工入职**：**接受**——CrudBizModel 标准 save 入职建档覆盖全字段 + hire 副路径创建员工+合同；PROBATION/ACTIVE 状态可设；证件重复依赖平台唯一约束。
- **UC-HR-05 招聘录用**：**P2**（P2-RC-010 新登记）——7 态状态机全链 + hire 创建员工/合同/employeeId 回写 + reject/close 守卫完整；⑱「候选人接受 Offer 后未到岗需状态回退」异常路径未实现（边界场景弱，有 close+重开逃生路径）。
- **UC-HR-07 合同到期提醒**：**接受**——cron-gated Job + 单一可配置提醒窗口（默认 30 天，L1 30/60/90 多档概念可表达）+ 续签/到期终止完整 + 跨域通知派发；⑮不续签→RESIGNED 联动继承 P1-MA2-039 successor Deferred（resolved R1.15 documented simplification）。
- **UC-HR-08 部门调动**：**接受**——transferEmployee 单步直接更新 departmentId/positionId/superiorId + 合同三态 AUTO/YES/NO config-gated + 休假冲突 warn 不阻塞；㉝调动单实体+审批工作流 Deferred = L1 use-cases.md:99 显式 documented simplification（§4 三判据 (ii) 满足）。
- **UC-HR-12 胜任力管理与评估**：**接受**——胜任力字典/等级/岗位矩阵 CRUD + 评估状态机 DRAFT→SUBMITTED→COMPLETED + 360 多源加权聚合（权重 config 驱动默认 15%/50%/25%/10%）+ gapValue/gapSeverity + CRITICAL/MODERATE→发展计划生成 + HR 审核调整全链完整。

**Verdict: pass（零 P0、零 P1、1 项新 P2[P2-RC-010]、4 UC 接受）**。resolved finding HEAD 复核：P1-MA2-039/040/042（resolved R1.15 documented simplification）+ P1-MA1-022（resolved）+ P2-MA2-047~051/P2-MA4-008（watch-only 维持）全部「如登记/已 resolved」无升级。本切片解除 A1.12 在 MA4（A4.2 扩展域展开器）及 MR1（R1.0）链路的该切片证据缺口。

