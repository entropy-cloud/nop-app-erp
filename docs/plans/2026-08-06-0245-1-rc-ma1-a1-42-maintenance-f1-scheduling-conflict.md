# 2026-08-06-0245-1 rc-ma1-a1-42-maintenance-f1-scheduling-conflict maintenance-F1 调度与冲突需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.42（MA1 需求追踪矩阵审计 — maintenance-F1 预防性调度(时间/运行时长) + 排程冲突检测）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.42
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.42 的 0.2 依赖）、`2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`（A1.41 done，同 RC 范式；本批次续编自 N=3 之后的最新 RC 编号，本切片 N=1 为 maintenance 域首个 RC 切片）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 + 三判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.42 给出 UC 清单 = `UC-MAIN-01/02/09`（3 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 **maintenance 域首个 RC 审计切片**（A1.42/A1.43/A1.44 三切片同批起草，共同完成 maintenance 域 11 UC 全覆盖；本切片覆盖 F1 调度与冲突 3 UC）。

- **L1 需求契约（权威真相源）**：`docs/design/maintenance/use-cases.md`（机制细节引用 `equipment-integration.md §五`，L2 设计参考）：
  - **UC-MAIN-01 预防性维护自动调度（时间周期）**（`:16`）：nop-job 定时任务 → 按维护计划（周期类型=时间，频率）→ 生成维护访问（DRAFT）→ 套用维护任务模板（标准工时/标准备件）→ 维护访问.设备/计划时间/任务清单 填充。use-cases.md:30 显式登记 nop-job 接线：`ErpMntDueVisitJob` + `app-service.beans.xml` + `scheduler.yaml` + cron 门控键 `erp-mnt.due-visit-cron`（默认空=跳过；非空时调 `IErpMntScheduleBiz.generateDueVisits()`）。
  - **UC-MAIN-02 运行时长触发维护**（`:34`）：周期类型=运行时长；设备累计运行时长 >= 阈值 → 生成维护访问（DRAFT）；运行时长来源：设备状态记录（RUNNING 时长累计）。
  - **UC-MAIN-09 排程冲突检测**（`:151`）：维护访问 SCHEDULED → 校验：设备/人员 同时段是否已有排程 → 冲突 → 警告或拒绝（配置决定）。

- **L3 代码实现现状（实测，`module-maintenance/erp-mnt-service`）**——**时间周期调度完整 + 运行时长触发零实现（候选 P1）+ 冲突检测仅 visit schedule 侧部分（候选 P1/P2）**：
  - **UC-MAIN-01 时间周期调度（✅ 完整）**：`ErpMntScheduleBizModel.java:27` generateDueVisits（@BizMutation，薄 facade）→ `ErpMntScheduleGenerateDueVisitsProcessor:21`（R6.7 per-mutation）→ `ScheduleDueGenerator.generateDueVisits`（扫描 active 计划 nextDueDate ≤ asOfDate 生成 DRAFT 访问 + 推进 nextDueDate，经 `erp-mnt.auto-generate-due-visits` 门控）。nop-job 接线：`ErpMntDueVisitJob` + cron 门控键已落地（use-cases.md:30 显式登记 + `docs/architecture/job-scheduling.md §3.13`）。模板套用（标准工时/标准备件）经 ScheduleDueGenerator 复制 VisitTask/SparePartUsageLine 模板行（须执行时核验复制完整性）。
  - **UC-MAIN-02 运行时长触发（❌ 候选 P1 完全缺失）**：grep `runtime|meter|operatingHour|cumulativeHour|usageHour|运行时长|RUNTIME` 跨 `module-maintenance/erp-mnt-service/src/main` **零业务命中**；ORM `app-erp-maintenance.orm.xml` Schedule 实体 grep `periodType|triggerType|cycleType|RUNTIME|METER|USAGE` **零命中**（仅 createTime/updateTime 不相关匹配）。`ScheduleDueGenerator` 以 `nextDueDate`（日期）为唯一触发基准，无"设备累计运行时长 >= 阈值"分支。设备状态记录（ErpMntEquipment.status RUNNING）无运行时长累计字段/Job。**UC-MAIN-02 全部 3 条断言（周期类型=运行时长 / 累计运行时长 >= 阈值 / 运行时长来源）不可满足**——候选 **P1 §2 ①（功能完全缺失）**。
  - **UC-MAIN-09 排程冲突检测（⚠️ 候选 P1/P2 部分实现）**：`ErpMntVisitScheduleProcessor` 是维护域唯一命中 `conflict` 关键词的 Processor（schedule 时含冲突校验逻辑，须执行时核验守卫断言：是否检测设备/人员同时段重叠 + 配置决定警告/拒绝）。**待核**：①校验维度是否覆盖"设备 + 人员"双维度（L1 明确"设备/人员"）；②"配置决定警告或拒绝"是否有 config key（如 `erp-mnt.schedule-conflict-mode`）；③`generateDueVisits`（UC-MAIN-01 自动生成）路径是否也走冲突检测，还是仅手动 schedule 路径走（自动批量生成若跳过冲突检测是候选缺口）。

- **L4 测试证据现状**（`module-maintenance/erp-mnt-service/src/test`）：
  - UC-MAIN-01：`TestErpMntDueVisitIdempotency.java`（幂等：重复 generateDueVisits 不重复生成）+ `job/TestErpMntDueVisitJob.java`（Job 触发）+ `TestErpMntSparePartAndSchedule.java`（计划/调度）；须执行时核验是否断言"生成 DRAFT + 模板套用 + 字段填充"。
  - UC-MAIN-02：**零测试**（功能零实现，无对应测试）。
  - UC-MAIN-09：须执行时核验 `TestErpMntSparePartAndSchedule` 或 `TestErpMntVisitCrudSmoke` 是否含冲突场景断言（grep `conflict|overlap` 跨测试目录）。
  - E2E：`tests/e2e/dashboards/maintenance.{smoke,value}.spec.ts`（看板域，不覆盖调度）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **无 maintenance 专属 MA2 状态机报告**（MA2 报告覆盖 finance/inventory/mfg/hr/purchase/sales/assets，零 maintenance 调度行为证据）。本切片为 maintenance 调度行为的首份证据。
  - **maintenance 相关既有 finding**：`P1-MA2-086`（cron 并发——`erp-mnt-due-visit-generation` 无 existence check before insert，resolved R1.28，并发幂等维度不重审）；`P1-MA1-011/013`（ErpMntVisit propId 缺失，fixed）；`P1-MA1-022`（跨域只读 daoFor，maintenance MaintenanceLabor/IssuePostingDispatcher 命中，resolved plan 2026-07-29-2225-1，平台一致性维度不重审）。**无任何 UC-MAIN-01/02/09 需求符合性 finding**。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：无 maintenance 专属 MA2 报告，无可复用调度行为证据；P1-MA2-086 引作并发幂等现状证据（不重审并发维度），只补需求视角差异（运行时长触发缺失/冲突检测维度）。

- **arm-index 既有 finding 衔接**：grep arm-index maintenance/mnt/UC-MAIN/schedule/due-visit/runtime/conflict → **无 UC-MAIN-01/02/09 finding**。非 UC 的 maintenance tag 既有 finding 见上（P1-MA2-086/P1-MA1-011/013/022）。本切片须 grep arm-index maintenance schedule/runtime/meter/conflict/overlap 同域同控制点后裁决复用 or 新建 `P*-RC-xxx`（续编，执行时取最新——当前全仓 RC 序列至 P2-RC-059 / P1-RC-063）。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）。本切片候选偏差（运行时长触发缺失/冲突检测缺失）若为代码逻辑补全则预授权；若触及 ORM 结构（如 Schedule 加 triggerType 列 / ErpMntEquipment 加累计运行时长字段 / ErpMntEquipmentStatusLog 实体）→ **ORM 结构变更须 ask-first + 独立 plan-audit**（roadmap 预授权声明明确排除 ORM 结构变更）；须在报告逐项标注触及保护区域。

- **剩余差距**：A1.42 切片五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.42 报告并登记 finding，解除 maintenance 调度与冲突证据缺口。

## Goals

- 产出 A1.42 切片审计报告 `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`，含方法论 §6 **9 段全部内容**：①UC-MAIN-01/02/09 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，调用链列全）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 3 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-MAIN-01 时间周期自动生成 DRAFT/模板套用/字段填充/cron 门控；UC-MAIN-02 周期类型=运行时长/累计运行时长>=阈值/运行时长来源；UC-MAIN-09 设备+人员同时段冲突/配置决定警告或拒绝。
- 对候选缺口给出分级结论：①UC-MAIN-02 **运行时长触发完全缺失**（grep 零实现 + ORM 无 triggerType/运行时长字段）倾向 **P1**（§2 ① 功能完全缺失；§4 三判据核 owner doc `equipment-integration.md §五` 是否显式 Deferred 经**人工批准**——须核 git log，AI 自标 ≠ 人工批准 methodology §4 line 172；**修复触及 ORM 结构[Schedule 加 triggerType 列 + ErpMntEquipment 加累计运行时长字段 或 ErpMntEquipmentStatusLog 实体]须 ask-first**）；②UC-MAIN-09 **冲突检测维度/配置门控/自动生成路径覆盖**（待执行时核验 ErpMntVisitScheduleProcessor 守卫完整性后定级 P1/P2/接受）；③UC-MAIN-01 时间周期调度倾向**接受**（cron 接线 + 幂等 + 模板套用须核验完整性）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx`（续编，执行时取最新）并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复；**ORM 结构类须 ask-first**）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；audit reports 表新增 A1.42 行——maintenance 域首行之一）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / maintenance use-cases / owner doc 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计 maintenance-F2/F3**（A1.43/A1.44 各自独立 plan；A1.42 只覆盖 UC-MAIN-01/02/09 调度与冲突）。
- **不重审 P1-MA2-086 并发幂等维度**（§去重协议：resolved R1.28 并发维度裁决不重审；本切片只引用其幂等守卫现状，从需求契约视角审运行时长触发缺失/冲突检测维度）。
- **不重审 P1-MA1-011/013/022**（propId/跨域 daoFor，非 UC-MAIN 维度，复用不复审）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.42 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.42 UC 锚点）+ `docs/design/maintenance/use-cases.md`（L1 真相源）+ `docs/design/maintenance/equipment-integration.md §五`+`state-machine.md §4`（L2 设计参考，非真相源——Deferred/Non-Goal 标注须 §4 三判据复核）+ `docs/audits/arm-index.md`（finding 衔接）+ `docs/architecture/job-scheduling.md §3.13`（UC-MAIN-01 nop-job 接线现状）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-maintenance/erp-mnt-service -Dtest=TestErpMntDueVisitIdempotency,TestErpMntDueVisitJob,TestErpMntSparePartAndSchedule`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-MAIN-01/02/09 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:16/34/151` 验收标准原文；L2 引用 `equipment-integration.md §五`+`state-machine.md §4`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpMntScheduleBizModel`#generateDueVisits + `ErpMntScheduleGenerateDueVisitsProcessor` + `ScheduleDueGenerator` + `ErpMntDueVisitJob` + `ErpMntVisitScheduleProcessor`#schedule（冲突校验站点）+ ErpMntSchedule/ErpMntEquipment ORM（含行号）；L4 引用 `TestErpMntDueVisitIdempotency`/`TestErpMntDueVisitJob`/`TestErpMntSparePartAndSchedule`#method + grep conflict 测试（注明断言强度）；L5 标注无 maintenance 专属 MA2 报告（首份证据）+ P1-MA2-086 并发幂等现状复用。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条对照）：UC-MAIN-01 时间周期自动生成（✅ 完整——cron 门控 + generateDueVisits + 模板套用 + 字段填充，须核模板复制完整性）；UC-MAIN-02 运行时长触发（❌ grep `runtime|meter|operatingHour|cumulativeHour|运行时长` 跨 main **零命中** + ORM Schedule 无 triggerType/cycleType + ErpMntEquipment 无累计运行时长字段 + ScheduleDueGenerator 唯一 nextDueDate 基准 → 全 3 断言不可满足）；UC-MAIN-09 冲突检测（⚠️ 待核 ErpMntVisitScheduleProcessor 守卫：①是否覆盖设备+人员双维度 ②是否有 config key 决定警告/拒绝 ③generateDueVisits 自动生成路径是否也走冲突检测）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-MAIN-01/02/09 给出符合性结论（取最高）：UC-MAIN-02 运行时长触发完全缺失倾向 **P1**（**§4 三判据关键裁决**：L1 `use-cases.md:34-43`+L2 `equipment-integration.md §五`明确要求"周期类型=运行时长"；核 owner doc 是否显式 Deferred 且经**人工批准**：判据[i]plan-audit / [ii]owner doc 显式 Deferred 经**人工批准**痕迹（grep git log，AI 自标 ≠ 人工批准 methodology §4 line 172）/ [iii]product-scope 裁剪；**ORM 加 triggerType 列 + 累计运行时长字段 触及保护区域须 ask-first**）；UC-MAIN-09 冲突检测按执行时核验结果定 P1/P2/接受（覆盖不全→P1，配置门控缺→P2）；UC-MAIN-01 时间周期倾向**接受**（须核模板套用完整性）。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 owner doc Deferred/Non-Goal 标注的人工批准痕迹**）+ 触及保护区域标注。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MAIN-01/02/09 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L2 引用 equipment-integration.md §五（Deferred 注记 §4 复核）、L3 含行号、L4 注明断言强度、L5 标注无专属 MA2 + P1-MA2-086 复用
- [x] UC-MAIN-01/02/09 有符合性结论且列明 §2 判据编号；UC-MAIN-02 P1 裁决须含 owner doc Deferred 标注的人工批准痕迹核查结论；触及 ORM 保护区域项显式标注 ask-first

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` maintenance schedule/runtime/meter/conflict/overlap/due-visit 同域同控制点后裁决——UC-MAIN-02 运行时长触发为**新根因**（无 UC-MAIN-02 finding）→ 新建 P1-RC（UC-MAIN-02）；UC-MAIN-09 冲突检测若为独立缺口则新建 P1/P2-RC（视核验结果）。执行时 grep arm-index 取最新续编号避免冲突（当前至 P2-RC-059 / P1-RC-063）。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **ORM 结构类修复（Schedule 加 triggerType 列 / ErpMntEquipment 加累计运行时长字段 / 可能新增 ErpMntEquipmentStatusLog 实体）须 ask-first + 独立 plan-audit** + **UC-MAIN-02 运行时长修复须与 ErpMntEquipment 设备状态记录（RUNNING 时长累计）+ 可能的 mfg 设备采集数据协同**。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 generateDueVisits 自动生成路径是否走冲突检测 / SP-2 ScheduleDueGenerator 模板套用（标准工时/标准备件）复制完整性运行时行为 / SP-3 设备累计运行时长是否有未发现的采集入口；每存疑点一行）。**P0 即时通道评估**（本切片无活跃数据破坏候选——UC-MAIN-02 运行时长触发缺失属功能缺失类非数据破坏；UC-MAIN-09 冲突检测缺失可能导致排程重叠但非数据破坏——评估在报告 §7 给结论）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：无 maintenance 专属 MA2 报告（无可复用调度行为证据，本切片为首份）；P1-MA2-086 引作并发幂等现状证据（不重审并发维度）；列明只补的需求视角差异（运行时长触发缺失/冲突检测维度）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding 入 RC 发现追踪分区；audit reports 表新增 A1.42 行（maintenance 域首行之一）。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1/A4.2 展开）；P0 候选评估有结论（本切片倾向无 P0）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02db36a4effeG6kur04GthBbts，fresh session，未起草本计划）。范围/UC 覆盖（A1.42=UC-MAIN-01/02/09）/依赖（0.2 done）/结果表面/方法论（9 段 §6 + §4 三判据 + §5 ask-first + §7 reuse + §去重协议）/反 slack/模板/保护区域标注全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①`ErpMntScheduleBizModel:27`→`ErpMntScheduleGenerateDueVisitsProcessor:21`→`ScheduleDueGenerator:35` ✅；②`ErpMntDueVisitJob` 存在 ✅；③grep `runtime|meter|operatingHour|cumulativeHour|运行时长|RUNTIME` 跨 erp-mnt-service/src/main **零业务命中** ✅；④ORM `app-erp-maintenance.orm.xml` Schedule 无 `periodType|triggerType|cycleType|RUNTIME|METER` 列 ✅；⑤`ErpMntVisitScheduleProcessor` 是唯一 conflict Processor 命中，`checkScheduleConflict` 仅 equipmentId+visitDate 维度（无人员维度、无 config-gated warn/reject、恒抛 ERR）证实本切片候选缺口框架 ✅；⑥arm-index 最新 ID = P2-RC-059 / P1-RC-063 ✅；⑦baseline-inventory:376 A1.42 锚点一致 ✅。ORM 结构类修复全标 ask-first。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.42 报告 9 段齐全 + UC-MAIN-01/02/09 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.42 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（运行时长触发缺失/冲突检测缺失）多为**代码逻辑 + ORM 结构**类（ORM 结构类[Schedule triggerType 列 / ErpMntEquipment 累计运行时长字段 / ErpMntEquipmentStatusLog 实体]须 ask-first + 独立 plan-audit，roadmap 预授权声明明确排除 ORM 结构变更）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-MAIN-02 运行时长修复须与 ErpMntEquipment 设备状态记录 + 可能的 mfg 设备采集数据协同）

## Closure

Status Note: 执行完成（2026-08-05）。Phase 1 + Phase 2 全部 [x]，报告 `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md` 9 段齐全，arm-index 已登记 A1.42 行 + 4 新 finding（P1-RC-064/065/066 + P2-RC-060）+ RC 交叉引用注记。只读审计零生产代码变更；目标域测试 `mvn test -pl module-maintenance/erp-mnt-service -Dtest='TestErpMntDueVisitIdempotency,TestErpMntDueVisitJob,TestErpMntSparePartAndSchedule,TestErpMntVisitRequestStateMachine'` → 17/0/0 BUILD SUCCESS（行为基线确认）；`nop-compliance-checker.sh` actual=baseline 0 漂移。执行期关键发现：①UC-MAIN-02 运行时长触发完全缺失→P1-RC-064（ORM 结构 ask-first）；②**纠正计划基线错误假设**——UC-MAIN-01 模板套用 ScheduleDueGenerator 不复制模板行（计划基线称「须执行时核验」执行时核验证伪）→P1-RC-065（ORM 结构 ask-first）；③UC-MAIN-09 设备维度接受但人员维度缺失→P1-RC-066（纯 BizModel 预授权）+ warn/config 模式缺失→P2-RC-060（watch-only）。零 P0。结束审计已由独立子代理（新会话，MISSION_DRIVER:2026-08-04-224309-mission-driver）执行并通过，load-bearing 代码断言经实仓复核全部 CONFIRMED TRUE（见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-04-224309-mission-driver，新会话，不重用执行者上下文，未起草/未执行本计划）
- Evidence: 结束审计独立复核通过。①报告存在性 + 9 段齐全已核：`docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`（286 行，§1-§9 全部存在 [行 15/73/123/147/155/199/220/232/258] + 报告 9 段完整性自检段）。②arm-index 衔接已核：`docs/audits/arm-index.md` 已新增 audit reports 表 A1.42 行 + 4 新 finding（P1-RC-064/065/066 + P2-RC-060）+ RC 交叉引用注记行。③load-bearing 代码断言经实仓复核 CONFIRMED TRUE：`ScheduleDueGenerator.generateVisitForSchedule:71-81` 仅设 6 基本字段（code/scheduleId/equipmentId/visitDate/status/visitType）无模板复制 → 证实 P1-RC-065；`ErpMntVisitScheduleProcessor.checkScheduleConflict:44-64` 查询过滤仅 equipmentId+visitDate+status 无 assignedTo 维度且恒抛 ERR_VISIT_SCHEDULE_CONFLICT → 证实 P1-RC-066 + P2-RC-060；grep runtime|meter|operatingHour|cumulativeHour|运行时长 跨 erp-mnt-service/src/main 零业务命中 → 证实 P1-RC-064。④Anti-Hollow：finding 均为「功能/逻辑缺失」类非空壳实现，报告本身是结果表面（非代码），无可达性疑虑。⑤文本一致性：Plan Status=completed + Phase 1/2 Status=completed + Exit Criteria 全 [x] + Closure Gates 全 [x] + 日志 `docs/logs/2026/08-06.md` 已登记 A1.42。⑥§4 三判据复核 + ORM 结构类修复全标 ask-first，无误降级。审计结论：计划可关闭。

Follow-up:

- MR0/MR1 按 §10 展开本报告 finding 修复（UC-MAIN-02 运行时长触发 ORM 结构类须 ask-first + 跨域 mfg 设备采集协调）。
- MA4 运行时探针展开 §7 静态存疑点清单 SP-1~SP-3（运行时确认自动生成冲突检测路径 / 模板套用复制完整性 / 设备累计运行时长采集入口）。
