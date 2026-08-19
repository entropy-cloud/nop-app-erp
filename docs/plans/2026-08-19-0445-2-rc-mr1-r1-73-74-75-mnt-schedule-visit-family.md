# 2026-08-19-0445-2-rc-mr1-r1-73-74-75-mnt-schedule-visit-family RC-R1.73/74/75 — maintenance 调度触发与访问生成链补全（A 类 ORM：StatusLog 运行时长 + 任务模板实体 + visit↔request 联动）

> Plan Status: completed
> Last Reviewed: 2026-08-19
> Mission: requirement-compliance
> Work Item: RC-R1.73（P1-RC-064，UC-MAIN-02 运行时长触发）+ RC-R1.74（P1-RC-065，UC-MAIN-01 任务模板套用）+ RC-R1.75（P1-RC-067，UC-MAIN-05 visit→request 联动）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.73/74/75 行 + `docs/audits/arm-index.md` P1-RC-064/065（A1.42 切片登记 + A4.2.147-154 运行时确认维持 P1）+ 2026-08-12 批量裁决 A 类（roadmap 头 :42：「maintenance: RC-R1.73（ErpMntSchedule 加 triggerType/threshold 列 + 设备加累计运行时长 或 新增 ErpMntEquipmentStatusLog 实体）、RC-R1.74（新增任务模板实体）、RC-R1.75（ErpMntVisit 加 requestId 列）」ORM 修改授权已批量批准，对齐 Q3 纯加性类自动执行，越界回落双独立子 agent 批准；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/maintenance/use-cases.md` UC-MAIN-01/02/05（L1 正文不动）+ `docs/design/maintenance/equipment-integration.md` §五（5.1/5.2/5.3）+ `docs/architecture/job-scheduling.md` §3.13（erp-mnt-due-visit-generation）；`docs/plans/2026-08-07-1932-3-rc-mr1-r1-2-fin-bank-recon-auto-reverse-job.md`（既有入口复用范式）；`docs/plans/2026-08-16-0424-2-rc-mr1-r1-52-53-54-ast-depreciation-disposal-family.md`（三 finding 单计划家族先例）
> Audit: required

## Current Baseline

- **finding P1-RC-064（UC-MAIN-02，A4.2.147 SP-2 静态确认 + 维持 P1）**：L1 `use-cases.md:39-43` 逐字：「周期类型=运行时长 / 设备累计运行时长 >= 阈值 → 生成维护访问(DRAFT) / 运行时长来源: 设备状态记录(RUNNING 时长累计)」。L3：grep runtime|meter|operatingHour|cumulativeHour 跨 erp-mnt-service/src/main 零业务命中；`ErpMntSchedule`（orm.xml:211-246，propId 1-19）无 triggerType/阈值列（recurrenceType dict 仅 DAILY/WEEKLY/MONTHLY/YEARLY 纯时间，scheduleType 仅 PREVENTIVE/PREDICTIVE/CALIBRATION）；`ErpMntEquipment`（:119-178，propId 1-21）无累计运行时长字段；无 ErpMntEquipmentStatusLog 实体；`ScheduleDueGenerator` 唯一 nextDueDate 基准；equipment-integration.md §5.1/§5.2 活跃要求三类型计划与运行时长触发未 Deferred。
- **finding P1-RC-065（UC-MAIN-01，A4.2.147 主路径闭合但模板缺失维持 P1）**：L1 `use-cases.md:24-25` 逐字：「套用维护任务模板(标准工时/标准备件) / 维护访问.设备/计划时间/任务清单 填充」。L3：`ScheduleDueGenerator.generateVisitForSchedule`（`ScheduleDueGenerator.java:71-81`）仅设 6 基本字段（code=VST-SCH-{scheduleId}-{asOfDate}/scheduleId/equipmentId/visitDate/status=DRAFT/visitType=PLANNED），无模板复制；grep applyTemplate|copyTemplate|standardHour|standardSpare 零命中；ORM 12 实体无任务模板实体，Schedule 无模板关联；生成 DRAFT 访问为「空壳」。owner doc §5.3（equipment-integration.md:222-226）活跃契约：「维护任务模板包含：任务名称、适用设备类型、标准工时、标准备件清单、操作说明」。
- **finding P1-RC-067（UC-MAIN-05，A4.2.151 运行时确认维持 P1）**：L1 `use-cases.md:94-95` 逐字：「维护请求 OPEN → 受理 ACCEPTED → 生成维护访问(关联请求) / 维护访问 COMPLETED → 请求 COMPLETED」。L3：`ErpMntVisit`（orm.xml:249-308，propId 1-26）无 requestId 列（唯一 visit↔request 关联 = ErpMntSparePartUsage.requestId :497）；`ErpMntRequestAcceptProcessor.generateResponsiveVisit`（:33-42）生成 VST-REQ-{requestId} 编码访问但**不设反向指针**（仅 code 命名约定）；`ErpMntVisitCompleteProcessor.complete`（:28-39）零 request 操作（终态手工两步，`TestErpMntVisitRequestStateMachine#testRequestFullFlow` 证实）；request 状态机 `ErpMntRequestStateMachine`（:106-115）7 边，complete 仅 IN_PROGRESS→COMPLETED。
- **既有机制（复用面）**：`EquipmentStatusLinker`（visit start/complete/cancel + downtime record/complete 五写点，config `erp-mnt.equipment-status-link-enabled` 默认 true）+ `ErpMntEquipmentBizModel.changeStatus`（通用手工）；状态 dict `erp-mnt/equipment-status`（RUNNING/IDLE/UNDER_MAINTENANCE/DOWN/DECOMMISSIONED，_ErpMntDaoConstants:79-99）；`ErpMntVisitTask` 子实体已存在（visitId/lineNo/taskDescription/status dict erp-mnt/visit-task-status/completedBy/completedAt，orm.xml:311-341）——任务清单落位载体就绪；到期访问 job `erp-mnt-due-visit-generation.job.yaml`（cron 门控 `erp-mnt.due-visit-cron` 默认空=跳过）+ `ErpMntDueVisitJob.execute()` → `IErpMntScheduleBiz.generateDueVisits()` 链路完整；幂等守卫 `existsVisitForScheduleDate`（:55-61，R1.28）。
- **Q4 判据**：三项均 §2 P1① 功能完全缺失；三判据复核均不成立（owner doc §5.1-5.3/state-machine.md 活跃契约未 Deferred + product-scope 未裁剪 + 无人工批准记录）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**：三行 ORM 授权已批量批准（纯加性，越界回落双独立子 agent 批准）。
- **测试基线**：erp-mnt-service **20 测试类 / 108 @Test 全绿**（TestErpMntVisitRequestStateMachine 18 / TestErpMntRequestStateMachine 9 / TestErpMntDueVisitJob + TestErpMntDueVisitIdempotency + TestErpMntSparePartAndSchedule 等）；job 计数基线 TestErpAllJobYamlLoading=29（本计划**零新 job.yaml**——复用 due-visit job，计数不变）。
- **compliance 基线**：R2b=235 / R2c=1439 / R2d=35 / R10=12 / R12a=70。新查询均同域（mnt dao）但 StatusLog/Template/TemplateLine 经 support 类 daoFor 访问大概率新增 R2c 站点——按 R1.48/51/57 baseline-raise per-site 证据先例处理。

## Goals

- **UC-MAIN-02 运行时成立**：新增 `ErpMntEquipmentStatusLog` 实体（状态变更历史）+ 运行时长查询时聚合（Σ RUNNING 段，无采集 Job 无物化漂移）+ `ErpMntSchedule` 纯加性 3 列（triggerType dict TIME/RUNTIME、thresholdHours、runtimeBaselineHours）+ `ScheduleDueGenerator` 运行时长分支（既有 due-visit job 同链评估，无新 job）。
- **UC-MAIN-01 任务清单填充成立**：新增 `ErpMntTaskTemplate` + `ErpMntTaskTemplateLine` 实体（名称/适用设备类型/标准工时/操作说明 + 任务行与标准备件提示）+ `ErpMntSchedule.templateId` 列 + `generateVisitForSchedule` 套用复制为 `ErpMntVisitTask` 行（标准工时写入任务行，标准备件作执行提示不自动产生消耗单据）。
- **UC-MAIN-05 自动联动成立**：`ErpMntVisit.requestId` 列（propId 27，可空 + to-one）+ `generateResponsiveVisit` 显式回填 + visit complete 写回 request COMPLETED（经既有状态机合法边合成迁移；终态请求 no-op 跳过不阻断访问完成；幂等）。
- **测试补强**：新 TestErpMntRuntimeTrigger / TestErpMntTaskTemplate / TestErpMntVisitRequestLinkage 测试组 + 108 基线零回归 + 全量构建 + checker 零漂移（或 baseline-raise per-site 证据）。
- **owner doc 收敛**：equipment-integration.md §5.1/5.2/5.3 实现注记 + state-machine.md §维护请求联动注记 + arm-index P1-RC-064/065/067 → done + roadmap 三行 done + 行标签改写 + logs 条目。

## Non-Goals

- **不做产量周期触发**（L2 §5.1 表列三类型但 L1 UC 仅要求运行时长；产量采集无数据源实体，登记 successor，触发条件 = 产量采集数据落地，对齐 dashboards.md OEE 同型 successor 范式）。
- **不做标准备件自动预填消耗单据**（SparePartUsage 是真实消耗单含过账链；模板备件作任务行提示，实际消耗走既有 confirm 链——L1「套用(标准工时/标准备件)」在模板携带 + 提示层达成）。
- **不做设备前态持久快照 / OEE**（P2-RC-061 人工裁决已排除当前实施；OEE 归 RC-R1.78 独立行——StatusLog 实体为 OEE 可用率分子预留数据源，注记衔接）。
- **不做 mfg 报工反写运行时长**（§7.3「工单报工记录（用于计算运行时长和产量）」——L1 权威来源 = 设备状态记录；报工源登记 successor）。
- **不新增 job.yaml / 不改 due-visit job cron 默认值**。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/maintenance/equipment-integration.md`（§五 + §3.3 注记衔接）+ `docs/design/maintenance/state-machine.md`（§4 维护请求）+ `docs/design/maintenance/use-cases.md`（L1 正文不动）+ `docs/architecture/job-scheduling.md`（§3.13 注记）
- Skill Selection Basis: ORM 新实体/加列 + 增量重生成；BizModel/Processor/job 扩展（`nop-backend-dev`）；测试（`nop-testing`）。

## Infrastructure And Config Prereqs

- 无新 config 键（运行时长评估复用 `erp-mnt.auto-generate-due-visits` + `erp-mnt.due-visit-cron` 既有门控；模板套用与联动为验收标准硬契约不加 config 门控，对齐 R1.58 D4 裁决）。
- ORM 纯加性：ErpMntSchedule +4 列（triggerType propId 20 dict 新 `erp-mnt/trigger-type`[TIME/RUNTIME，null=TIME 派生兼容]、thresholdHours 21、runtimeBaselineHours 22、templateId 23）+ ErpMntVisit.requestId 27 + 新实体 ErpMntEquipmentStatusLog / ErpMntTaskTemplate / ErpMntTaskTemplateLine + ErpMntVisitTask.standardMinutes 列（propId 顺延，执行期按 ORM 实际最大值落位）——`mvn clean install -DskipTests` 增量重生成；零迁移（全可空无默认）。
- 新 dict `erp-mnt/trigger-type`（orm dict 声明 + meta dict yaml 手写，R1.45/R1.70 先例）；无新 seed 模板、无新 job。

## Execution Plan

### Phase 1 - R1.73：StatusLog 实体 + 运行时长聚合 + Schedule 触发分支

Status: completed
Targets: `module-maintenance/model/app-erp-maintenance.orm.xml`（StatusLog 实体 + Schedule 3 列 + trigger-type dict）、`module-maintenance/erp-mnt-meta/_vfs/dict/erp-mnt/trigger-type.dict.yaml`（新）、`EquipmentStatusLinker.java`、`ErpMntEquipmentBizModel.java`、`ScheduleDueGenerator.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无

- [x] **双独立子 agent 批准（保护区域 checkbox）**：新实体 + 加列（Q3/A 类批量授权范围内），按 R1.49/57/60/66 先例取得两个独立子 agent 分别检查批准，批准记录落盘本计划。
      - Skill: none
      - **批准记录（2026-08-19）**：
        - Approver A（task `ses_fe852c87cffenZiPm52fDKpCmp`，fresh session）：**approved**。核验：①授权范围在 roadmap:42 批量 A 类授权内（R1.73 StatusLog 分支选择符合 L1 use-cases.md:42）②纯加性确认（无既有结构改动，前向实体引用合法 orm.xml:150 先例）③propId 20-23/27/15 顺序正确 ④最小充分集确认。非阻塞 findings：a) dict `erp-mnt/status-log-source` 去掉 DISPOSAL 值（无写点 = dangling，P2-RC-057 同型）→ **已采纳：初始值集 = VISIT/DOWNTIME/MANUAL**；b) 两个 dict yaml 均须手写（trigger-type + status-log-source）→ 已采纳；c) i18n-en/domain 复用/审计列惯例 → 已采纳；d) isActive 保持可空无默认（Q3 边界），匹配逻辑 null=非启用 → 已采纳；e) templateId/runtimeBaselineHours/standardMinutes 越出 roadmap:42 字面列集但纯加性，归「越界回落双独立子 agent 批准」条款覆盖。
        - Approver B（task `ses_fe8529b3bffeMt3ftAAp2rSwPR`，fresh session）：**approved**。核验：①授权原文 roadmap:31-33 + :42 逐字确认 + plan:133 draft review acceptable ②无名称/relation/propId 冲突（ErpMntVisit 无既有 requestId/request relation；ErpMntSparePartUsage.request 为他实体非冲突）③跨模块 grep 再生成安全（仅注释/路由字符串弱引用；R1.60 reflect-config 先例）④状态机边集不受影响（D6 仅合成既有边）⑤最小充分。非阻塞 findings：a) 新实体审计列按 sibling 惯例 mandatory+defaultValue（「全可空」仅约束业务列）→ 已采纳；b) 两 dict yaml 均手写 → 已采纳；c) 不加 Visit.requestId/Schedule.triggerType 索引（Q3 禁既有实体索引改造）→ 已采纳。
- [x] **D1 运行时长载体 = StatusLog 实体 + 查询时聚合**（否决「累计列物化 + 采集 Job」：增量累加在 job 中断/回滚下漂移需对账，查询时聚合以状态记录为唯一真相幂等可重算）。`ErpMntEquipmentStatusLog`（equipmentId + fromStatus + toStatus + changeAt + source[VISIT/DOWNTIME/MANUAL/DISPOSAL] + sourceBillCode，索引 equipmentId+changeAt）；写点 = `EquipmentStatusLinker` 三个迁移方法 + `ErpMntEquipmentBizModel.changeStatus`（同一事务追加日志行）。**遗留基线语义**：无日志历史设备——当前 status=RUNNING → createTime 起算 RUNNING 段；当前非 RUNNING（IDLE/DOWN/UNDER_MAINTENANCE/DECOMMISSIONED）→ 运行时长记 0 直至首条日志行（保守 fail-safe，防无历史设备虚计触发）。
      - 实现注记：dict 值集按 Approver A finding 落地为 VISIT/DOWNTIME/MANUAL（DISPOSAL 无写点 dangling，落地时移除，见批准记录）；载体 = 新 `EquipmentStatusLogWriter`（写点单一真相）+ `EquipmentRuntimeCalculator`（Σ RUNNING 段查询时聚合，显式 asOf 参数确定性可测）。
      - Skill: `nop-backend-dev`
- [x] **D2 触发基线 = schedule.runtimeBaselineHours 列**（否决「从上次生成 visit 反推」：依赖 visit 查询链脆弱且首触发无锚点）。RUNTIME 计划生成 DRAFT 时同事务置 baseline=当前累计运行时长；触发条件 = 当前累计 ≥ baseline + thresholdHours；`advanceNextDueDate` 仅对 TIME 计划执行（RUNTIME 计划 nextDueDate 不推进保持 null/原值）；`findDueSchedules`（nextDueDate 基准）排除 triggerType=RUNTIME，新增 findRuntimeDueSchedules 分支在同一 `generateDueVisits` 入口评估（触发粒度 = job cron 部署节奏，对齐 L2 §5.2 单一定时任务模型）。
      - 实现注记：thresholdHours 缺失/非正不触发（未配置语义）；既有 VST-SCH-{schedId}-{asOfDate} code 幂等锚点双保险保留。
      - Skill: `nop-backend-dev`
- [x] **D3 dict 值域 = TIME/RUNTIME 两值**（产量周期 OUTPUT 不入——无数据源，dangling 值违反 dict 契约先例[priceValidationLevel "20" 孤儿值 P2-RC-057]；null=TIME 派生兼容存量计划）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：TestErpMntRuntimeTrigger 测试组：①RUNNING 段聚合数学断言（跨多个状态变更周期 + 当前开放段 + 遗留基线双分支[无日志+当前 RUNNING=createTime 起算 / 无日志+当前 IDLE/DOWN=0 直至首条日志]）②累计 ≥ 阈值 → 生成 DRAFT + baseline 重置 ③未达阈值不生成 ④同日 job 重跑幂等（baseline 已推进不重复生成）⑤TIME 计划零回归（null triggerType 走既有 nextDueDate 链）⑥IDLE/DOWN/UNDER_MAINTENANCE 段不计入 ⑦手动 changeStatus 写日志行 + `_cases/` 快照。验证命令：`mvn test -pl module-maintenance/erp-mnt-service`。
      - 实现注记：11 @Test 全绿（含 linker 三写点来源断言 + `_cases/testManualChangeStatusSnapshot` 快照）；既有 TestErpMntVisitRequestStateMachine/TestErpMntDowntimeAndE2E 因新增 StatusLog 行与 default 序列块分配产生合法 DB 快照漂移，经 forceSaveOutput 重录后归零（业务断言零回归）；模块 108 基线 + 11 新增 = 119 全绿 + `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 运行时长触发链落地 + 既有 TIME 计划与 due-visit job 测试零回归

### Phase 2 - R1.74：任务模板实体 + 套用复制

Status: completed
Targets: `module-maintenance/model/app-erp-maintenance.orm.xml`（Template + TemplateLine 实体 + Schedule.templateId + VisitTask.standardMinutes）、`ScheduleDueGenerator.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（同 ORM 文件串行编辑，避免重生成冲突）

- [x] **D4 模板实体与套用范围**：`ErpMntTaskTemplate`（code UK + name + equipmentCategoryId 可空[适用设备类型] + standardMinutes[标准工时] + instruction[操作说明] + isActive）+ `ErpMntTaskTemplateLine`（templateId + lineNo + taskName + standardMinutes 行级 + materialId/quantity 可空[标准备件提示]）。套用 = `generateVisitForSchedule` 解析模板（**显式 schedule.templateId 优先，空则按 equipment.categoryId 匹配唯一 active 模板回退，零/多匹配跳过 LOG.warn 不阻断**）→ 逐行创建 `ErpMntVisitTask`（taskDescription=taskName，standardMinutes 透传，status=PENDING）+ visit.totalMinutes 不预填（执行时长语义）。RESPONSIVE 访问（generateResponsiveVisit）不套用模板（L1 联动断言不含任务清单；模板属计划性维护语义）。
      - 实现注记：套用落位 = `generateVisit`（TIME/RUNTIME 两计划性路径共享叶子）内 `applyTaskTemplate` protected step + `resolveTaskTemplate` 解析（显式锚定不校验 isActive；回退匹配 eq(isActive,1)；行级 standardMinutes 缺失回落模板级）。ORM/BizModel/meta/web 页面由既有重生成落位（ErpMntTaskTemplate/Line 实体 + ErpMntTaskTemplateBizModel/LineBizModel 纯 CRUD）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：TestErpMntTaskTemplate 测试组：①显式 templateId 套用 → VisitTask 行数/描述/标准工时断言 ②categoryId 自动匹配回退（唯一 active）③无匹配/多匹配跳过不阻断（visit 仍生成，任务零行 + warn）④标准备件行携带提示字段不产生 SparePartUsage ⑤模板 CRUD 冒烟（GraphQL save/findPage）⑥无模板计划零回归（既有 6 字段行为）+ `_cases/` 快照。验证命令：`mvn test -pl module-maintenance/erp-mnt-service`。
      - 实现注记：7 @Test 全绿（testTemplateCrudSnapshot 含 `_cases/` 快照：1_save/2_findPage response + erp_mnt_task_template/nop_sys_sequence tables）；模块 119 基线 + 7 新增 = 126 全绿 + mnt 链 `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 模板套用链落地 + 无模板路径零回归

### Phase 3 - R1.75：visit↔request 双向联动

Status: completed
Targets: `module-maintenance/model/app-erp-maintenance.orm.xml`（ErpMntVisit.requestId + to-one request）、`ErpMntRequestAcceptProcessor.java`、`ErpMntVisitCompleteProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（ORM 串行）

- [x] **D5 生成侧回填**：`generateResponsiveVisit` 增设 requestId（code 命名约定保留——幂等锚点复用）；`ErpMntVisit.requestId` 可空 + to-one（PLANNED 访问恒 null 零影响）。
      - 实现注记：ORM requestId propId 27 + to-one request relation，无索引（Approver B finding c：Q3 禁既有实体索引改造）；`mvn clean install -DskipTests` 增量重生成（_gen/_ErpMntVisit + xmeta + api beans + deploy SQL）。
      - Skill: `nop-backend-dev`
- [x] **D6 完成侧写回 = 状态机合法边合成迁移**：visit complete 后置 protected step：requestId 非空时读 request——IN_PROGRESS → 经既有 complete 边置 COMPLETED；ACCEPTED → 先 startRepair 后 complete 合成迁移（两条均为既有合法边，不加新边不改状态机契约——否决「加 ACCEPTED→COMPLETED 直边」：A2.14 已审计状态机边集，加边属契约变更）；REJECTED/CANCELLED/COMPLETED 终态 → no-op LOG.warn 不阻断访问完成（幂等 + 容忍请求侧独立关闭）；写回失败（乐观锁冲突）rethrow 回滚 visit complete（联动为 L1 硬语义非 best-effort，区别于 R1.59 辅助语义降级先例）。visit cancel 对 request 无动作（L1 无断言）。
      - 实现注记：`completeLinkedRequest` protected step（注入实体级 ErpMntRequestStateMachine Bean，非法边映射领域 ERR_INVALID_REQUEST_STATUS_TRANSITION）；既有 TestErpMntDowntimeAndE2E#testResponsiveRequestFullFlow 手工 completeRequest 两步改写为断言 D6 自动联动（行为变更属 UC-MAIN-05 语义本身：终态手工两步 → 自动写回）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：TestErpMntVisitRequestLinkage 测试组：①accept 生成访问 requestId 回填断言 ②visit 完整链 start→complete → request=COMPLETED（IN_PROGRESS 输入）③ACCEPTED 输入合成迁移同样 COMPLETED ④request 终态（REJECTED/CANCELLED）no-op 访问正常完成 ⑤PLANNED 访问（requestId null）零影响回归 ⑥request 侧终态 no-op 幂等（visit 侧二次 complete 经既有 assertCanComplete 拒绝——既有行为不重测）+ `_cases/` 快照。验证命令：`mvn test -pl module-maintenance/erp-mnt-service`。
      - 实现注记：7 @Test 全绿（testAcceptBackfillSnapshot 含 `_cases/` 快照：accept response + visit/request/equipment/status_log tables）；REQUEST_ID 列新增经列子集比对零快照漂移（无需重录既有基线）；模块 126 + 7 新增 = 133 全绿 + mnt 链 `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 双向联动落地 + 既有 visit/request 状态机测试零回归

### Phase 4 - 验证收口 + 文档回填

Status: completed
Targets: `docs/design/maintenance/equipment-integration.md`、`docs/design/maintenance/state-machine.md`、`docs/architecture/job-scheduling.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-{当期}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-3 全绿

- [x] 全量验证：`mvn test -pl module-maintenance/erp-mnt-service` 全绿（108 基线 + 新增零回归）+ `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline 或 baseline-raise per-site 证据）+ TestErpAllJobYamlLoading=29 不变。
      - 实现注记：mnt-service 133/0/0（108 基线 + 11 RuntimeTrigger + 7 TaskTemplate + 7 VisitRequestLinkage）；全仓 clean install 156 模块 BUILD SUCCESS + 全仓 `mvn test` 3666/0/0/1（唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）；checker R2c 1460→1468 baseline-raise（8 per-site 证据落盘 compliance-baseline.md，全部 mnt 同域：VisitCompleteProcessor×3[D6] + ScheduleDueGenerator×2[RUNTIME 分支] + StatusLogWriter/Calculator×3）其余 18 规则零漂移；TestErpAllJobYamlLoading=29 通过（零新 job.yaml）。
      - Skill: none
- [x] owner doc 回填：equipment-integration.md §5.1/5.2/5.3 实现注记（StatusLog/trigger-type/模板实体/套用语义/备件提示 successor）+ §3.3 StatusLog 衔接注记 + state-machine.md §4 维护请求联动注记（D6 合成迁移裁决）+ job-scheduling.md §3.13 注记（运行时长评估并入既有 job）+ arm-index P1-RC-064/065/067 → done (RC-R1.73/74/75) + roadmap 三行 done + 行标签改写 + logs 条目（全绿验证状态）。
      - 实现注记：六处回填齐——equipment-integration.md（§5.1 trigger-type/OUTPUT successor + §5.2 StatusLog/聚合/同入口评估 + §5.3 模板实体/套用语义/备件 successor + §3.3 StatusLog 衔接块）+ state-machine.md（§适用对象二 联动注记块）+ job-scheduling.md（§3.13 due-visit 行扩展 + usage-based-trigger 行收窄为产量 successor）+ arm-index 三 finding done + roadmap 三行 done A 类改写 + logs 08-19 条目（full-green verification）。
      - Skill: none

Exit Criteria:

- [x] 六处回填一致（代码 / equipment-integration / state-machine / arm-index / roadmap / logs）

## Draft Review Record

- Independent draft review iteration 1: acceptable（task `ses_fe951f596ffeO1rbG4w5luUoDd`，2026-08-19）——零 BLOCKER；MINORS 5 项已全部采纳修订：①TestErpMntRequestFullFlow 引用更正为 TestErpMntVisitRequestStateMachine#testRequestFullFlow ②遗留无日志设备基线双分支语义钉死（当前非 RUNNING=0 直至首条日志，Proof ① 同步）③Proof ⑥ 幂等措辞精确化（request 侧终态 no-op）④compliance 预期改 baseline-raise per-site 口径 ⑤Phase 1/2 Targets 清理（ErpMntDueVisitJob 移除——设计零 job 改动；RequestAcceptProcessor 非变更注记移除，RESPONSIVE 排除语义保留于 D4 正文）。基线事实（ORM propId/dict/Java 行号/job 29/108 @Test/compliance 基线/L1 逐字/A 类授权 + Q3 覆盖/rule 14 打包正当性）全部复核确认。

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn clean install -DskipTests` + 分域 `mvn test` + compliance checker + TestErpAllJobYamlLoading）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 产量周期触发（OUTPUT triggerType）

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 UC 仅要求运行时长触发（UC-MAIN-02）；产量采集无数据源实体（owner doc §7.3 报工源同缺）；dict 不加 dangling 值。
- Successor Required: yes（触发条件：产量采集数据落地 / OEE（RC-R1.78）实施时性能效率分量数据源就绪）

### 标准备件自动预填消耗单据

- Classification: `optimization candidate`
- Why Not Blocking Closure: SparePartUsage 含审批/过账链，预填 DRAFT 消耗单超出 L1「套用」语义；模板行提示已达成「标准备件清单」携带。
- Successor Required: yes（触发条件：计划性备件预占需求立项）

### mfg 工单报工反写运行时长

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 权威来源 = 设备状态记录（use-cases.md:41 逐字）；§7.3 报工源为 L2 补充通道。
- Successor Required: yes（触发条件：报工数据精度要求立项，与 RC-R1.78 OEE 数据源协调）

## Closure

Status Note: closed

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，task `ses_fe74d67b5ffe7PuLiPfWzNWB7v`，2026-08-19）
- Evidence: 审计结论 = 实质工作全部 PASS（7 大项逐项 live-repo 核验：计划文本一致性[Phase 1-4 Status completed + 12 执行项 + 4 Exit Criteria 全 [x]]；ORM 实体/列/dict 值集/无 requestId 索引逐行核对[orm.xml :94-103/:246-250/:298/:303/:344/:368-433/:514-542]；ScheduleDueGenerator RUNTIME 分支 + applyTaskTemplate/resolveTaskTemplate 套用语义[:109/:177/:211-234]；completeLinkedRequest D6 语义[null 短路/终态 no-op/ACCEPTED 合成迁移/失败传播无吞] + ErpMntRequestStateMachine 仍恰 7 边零新边；beans 注册；三测试组 11/7/7 @Test + `_cases/` 快照目录；**live 复跑 `mvn test -pl module-maintenance/erp-mnt-service` = 133/0/0 BUILD SUCCESS**；六处文档回填逐处行号核对；compliance-baseline R2c=1468 + 8 per-site 注记与实仓吻合）。唯一 FAIL 发现 = Closure Gates 1-6 未勾选（程序性遗漏非工作缺口，审计者逐项独立证实为真后要求补勾）——已按审计 remediation 补勾 gates 1-6 并记录本证据后勾选 gates 7-8。

Follow-up:

- 无已确认缺陷；产量周期/备件预填/报工源归 Deferred But Adjudicated successor。
