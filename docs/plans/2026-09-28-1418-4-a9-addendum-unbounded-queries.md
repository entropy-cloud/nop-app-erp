# 2026-09-28-1418-4 A9 补遗批——非核心域报表/看板无界查询收口

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: mission 收官全局增量重扫描（独立只读代理，2026-09-28，HEAD=23e7da109）新发现 4 站点——分析报告 §2.2 A9 明确覆盖面内的枚举漏盘（A9 枚举七域**含 cs**但 cs 行仅指向 ErpCsReportBizModel 未含 dashboard；mnt 整域、prj 整域完全未枚举——草案审查 M-3/R-3 表述更正）
> Related: `docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md`（批次 2，Phase 5 三分类处置范式与先例）
> Audit: required

## Current Baseline

- **F-1 [P2]** `module-cs/erp-cs-service/.../dashboard/ErpCsQualityDashboardBizModel.java`：`loadClosedTickets`（:259-272）`findAllByQuery(status=CLOSED)` + 起止日期全可选（缺省=全部历史关闭工单）全量物化，三个 `@BizQuery` 入口（getDashboardKpi :69-73 / getTeamSlaRanking :129-133 / getAgentCsatBreakdown :195-199）内存按 team/agent 分组聚合；`:316` 以全部 ticketId 做无分块 `in("ticketId",…)` 跟随查询。护栏：`TestErpCsQualityDashboard` 在位。
- **F-2 [P2]** `module-projects/erp-prj-service/.../report/ErpPrjReportBizModel.java`：`loadTimesheets`（:304-312，草案审查 M-1 行号刷新）projectId/起止日期全可选 → 全表物化，内存 (projectId,userId) 聚合 `SUM(hours)` + `SUM(costAmount)`（Aggregator :318-328（聚合块 :270-288），纯 BigDecimal 求和）。护栏：`TestErpPrjReportRendering` 在位。
- **F-3 [P3]** `module-maintenance/erp-mnt-service/.../report/ErpMntReportBizModel.java`：`loadVisits`（:291-299，已按 visitDate desc+code desc 排序）与 `loadDowntimeEntries`（:353-359，**无任何 orderBy**——草案审查 B-1 实核更正）全量物化；`countTasksByVisit/countUsagesByVisit`（:301-311/:313-323）以全部 visitId 无分块 `in()` 跟随。护栏：`TestErpMntReportDatasets`/`TestErpMntReportRendering` 在位。
- **F-4 [P3]** `module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java`：`countOutOfControlCharts`（:287-296）载入全部 `isOutOfControl=true` 的 SPC 样本（增长最快表的高基数子集）仅求 distinct chartId。护栏：`TestErpQaDashboard`/`TestErpQaDashboardSpc` 在位（testOutOfControlChartCount :88 断言 distinct 计数）。
- **B-3 补遗完备性（草案审查实核追加）**：同文件同模式站点一并纳入——`ErpQaDashboardBizModel.countInadequateCapabilityCharts`（:333-342，F-4 精确孪生：capabilityLevel=INADEQUATE 全量物化求 distinct chartId）、`ErpQaDashboardBizModel.findCapaOverdueAlert`（:176-179，status!=COMPLETED 全量物化+内存 dueDate 截止过滤的预警列表型）；`ErpPrjReportBizModel.loadProjects`（:295-302，主数据小表+可选过滤+code 排序）登记豁免（批次 2 ③类）。
- 机制先例（批次 2 全部落仓）：①**维度分组投影聚合**（`sumBalanceTotalCost` warehouseId 维度分组 SUM+内存合计 / F-2 的 (projectId,userId) 分组投影）；ast `loadAssetIdsWithExecutedDepreciationInPeriod` 与 F-4 实为 chartId/assetId **单列投影+内存 HashSet 去重**（平台对纯维度投影仍注入主键维度，详见 1418-4 执行期留痕）——F-2 与 F-4 形态不同；②明细/聚合型 cap（各域 `REPORT_LIST_MAX_ROWS=5000`/`ALERT_MAX_ROWS` 常量范式——F-1/F-3a 同款；F-3b 由先例 ① 投影覆盖），onTimeRate cap 的「截断方向留痕」义务）；③无维度 SUM 非法 SQL 坑（ErpInvDashboardBizModel.java:501-503 注释，维度分组纪律；另见 ErpSalDashboardBizModel.java:266、ErpFinDashboardBizModel.java:375）。
- 重扫描回归面抽验：五批次终态无回归（批次 1-5 全部抽验站点在位；唯一的既有裁决残留 mnt `loadScheduleIdsWithVisit` setLimit(5000) 无排序截断挂 watch-only 备注，见 Deferred）。
- 剩余差距：非核心域 4 站点随数据量线性恶化（CLOSED 工单/timesheet/SPC 样本均为持续增长表）。

## Goals

- F-4：`countOutOfControlCharts` 改 chartId 单列投影 + 内存 HashSet 去重（**执行形态**：平台对纯维度投影仍注入主键维度，无 SQL GROUP BY——照 ast 先例「投影减列+内存去重」；null chartId 用 `notNull("chartId")` 过滤，实证原语 ErpCsQualityDashboardBizModel.java:319），distinct 语义逐位等价。
- F-4 孪生：`countInadequateCapabilityCharts` 同法 chartId 单列投影+内存 HashSet 去重（见 F-4 主条目执行形态；B-3 纳入）。
- F-2：`loadTimesheets` 聚合改 `(projectId,userId)` 维度分组投影 `SUM(hours)/SUM(costAmount)`，**保持 `ORDER BY projectId,userId`（B-2：行序为报表/API 用户可见契约，不因投影漂移）**；BigDecimal 求和逐位等价；移除内存 Aggregator。
- F-1：`loadClosedTickets` 加 `orderBy createTime desc` + 命名常量 `CS_DASHBOARD_SCAN_CAP=5000`（截断=仅计最近 5000 张关闭工单，确定性语义，留痕注释；`in("ticketId")` 跟随随之有界）。
- F-3 拆分（B-1 更正）：`loadVisits` 加 mnt 域 `REPORT_LIST_MAX_ROWS=5000` cap（visitDate desc 在位=确定性截断）；**`loadDowntimeEntries` 改 `(equipmentId,reason)` 分组投影 `SUM(totalMinutes)`+entry 计数**（原无排序，无排序 cap=非确定性聚合截断=本计划自己登记的 watch-only 反模式；投影行数=组合数天然有界、精确等价零损失），跟随 `in()` 随投影有界。
- B-3 纳入：`findCapaOverdueAlert` 日期过滤下推 SQL（`dueDate` 截止条件入查询，NULL 语义与内存跳过对齐）+ `orderBy dueDate asc` + cap（最早逾期优先=确定性）。
- 触及四模块 JUnit 全绿、数值断言不变。

## Non-Goals

- F-1 不做三入口完整 SQL 聚合重写（跨 entry 聚合维度各异 + 求平均/时长类非纯 SUM——Deferred，触发=该看板 RT 实测超标或 owner doc 对全量语义裁决）。
- 不动 mnt `loadScheduleIdsWithVisit` 既有 setLimit(5000)（本 mission 前既有裁决，watch-only 登记截断方向为错误数据而非保守下偏，触发=visits 实测接近 5000 时用 GROUP BY scheduleId 投影等价修法）。
- 不改任何 API 契约/ORM/页面；不动其他域（A9 已处置清单与本批白名单外站点）。
- `ErpPrjReportBizModel.loadProjects`（:295-302）**显式豁免登记**（B-3）：项目主数据小表+可选过滤+code 排序，批次 2 ③主数据小表豁免类，非本批施改面。

## Task Route

- Type: `implementation-only change`（读路径查询有界化/聚合化，用户可见行为=数值不变）
- Owner Docs: `docs/design/customer-service/sla.md`、`docs/design/customer-service/csat.md`（cs 看板语义）、`docs/design/dashboards.md`、各域 report owner doc
- Skill Selection Basis: 已扫描 `docs/skills/README.md`——无匹配专项技能；复用批次 2 Phase 5 范式与自检

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - F-4 QA OOC 投影计数 + F-2 PRJ 维度分组投影（等价变换）

Status: completed
Targets: `ErpQaDashboardBizModel.java`、`ErpPrjReportBizModel.java`
Skill: none

- Item Types: `Fix`×3
- Prereqs: none

- [x] Fix F-4：chartId 单列投影（减传输列）+ `notNull("chartId")` 过滤 + 内存 HashSet 去重（原语实证 `ErpQaDashboardBizModel.java:304` notNull 过滤——被归一实现自身；形态按 ast 先例 `loadAssetIdsWithExecutedDepreciationInPeriod`）→ 去重后 `size()` 即 distinct 计数
      - Skill: none
- [x] Fix F-2：`GROUP BY (projectId,userId)` 双维投影 `SUM(hours)/SUM(costAmount)`，替代全量物化+Aggregator（BigDecimal 求和交换律精确）；输出行字段名保持 projectId/userId/hours/costAmount 且 **`ORDER BY projectId,userId` 保持（B-2）**
      - Skill: none
- [x] Fix F-4 孪生：同 F-4——chartId 单列投影+`notNull` 过滤+内存 HashSet 去重（B-3 纳入；护栏 TestErpQaDashboardSpc.testInadequateCapabilityCount :70 在位）
      - Skill: none

Exit Criteria:

- [x] `TestErpQaDashboard(Spc)` / `TestErpPrjReportRendering` 全绿数值逐位不变；两模块 `mvn test -pl <module> -am` 0 failures

### Phase 2 - F-1/F-3 有界化（cap 范式）

Status: completed
Targets: `ErpCsQualityDashboardBizModel.java`、`ErpMntReportBizModel.java`、`ErpQaDashboardBizModel.java`
Skill: none

- Item Types: `Fix`×4 + `Decision`×1
- Prereqs: none

- [x] Fix F-1：`orderBy createTime desc` + `setLimit(CS_DASHBOARD_SCAN_CAP=5000)` + 截断方向注释（仅计最近 5000 张关闭工单；超 cap 历史 SLA/CSAT 聚合下偏）
      - Skill: none
- [x] Fix F-3a：`loadVisits` 加 mnt 域 `REPORT_LIST_MAX_ROWS=5000` cap（visitDate desc+code desc 在位=确定性截断）+ 注释
      - Skill: none
- [x] Fix F-3b：`loadDowntimeEntries` 改 `(equipmentId,reason)` 分组投影 `SUM(totalMinutes)`+entry 计数（B-1：原无 orderBy，禁无排序 cap；投影行数=组合数天然有界），输出字段名与消费方 :265-273 聚合逐字对齐
      - Skill: none
- [x] Fix findCapaOverdueAlert（B-3）：日期过滤下推 `lt("dueDate", cutoff)`（NULL dueDate 行 SQL 不命中=与内存 null 跳过对齐）+ `orderBy dueDate asc` + `QA_ALERT_CAP=5000` 命名常量 cap（最早逾期优先=确定性）
      - Skill: none
- [x] Decision（F-1 cap vs SQL 聚合重写）：cap 与 onTimeRate/aggregateActualQty 同型裁决（聚合型但跨 entry 维度各异+非纯 SUM → cap+截断方向留痕）；完整重写 Deferred（触发=看板 RT 实测超标）。替代方案「三入口各自 GROUP BY 投影」否决理由：getDashboardKpi 无分组维度（无维度 SUM 坑）+ 求均值/时长类需二次计算，重写面>收益
      - Skill: none

Exit Criteria:

- [x] `TestErpCsQualityDashboard` / `TestErpMntReport*` 全绿数值不变（种子量≪cap；downtime 投影后 `testDowntimeSummaryDatasetAggregatesByEquipmentAndReason` 数值逐位不变）；`TestErpQaDashboard.testCapaOverdueAlertTriggersAndNot`（:123-124 护栏）全绿；三模块 `mvn test -pl <module> -am` 0 failures


## 执行期留痕（2026-09-28 实施实测填充）

- **F-4 平台坑二度变体（重要执行期发现）**：纯维度投影（无 aggFunc）在 Nop ORM 中**仍被强制注入主键维度**（生成 `select o.chartId, o.id ...` 无 GROUP BY，每行一结果）——首轮实现 `size()` 直数行数得 3≠2，被护栏 `testOutOfControlChartCount`/`testInadequateCapabilityCount` 立即捕获。复核批次 2 ast 先例 `loadAssetIdsWithExecutedDepreciationInPeriod` 实为「单列投影减传输 + **内存 HashSet 去重**」而非 SQL GROUP BY——已照先例修正两处（报告 §2.4 对该先例的「维度投影」描述建议随 doc-only 批校正）。终版 QA 10/10 全绿。
- **F-3b 消费方保真**：`(equipmentId,reason)` 投影 + 内存 reason null→"(unspecified)" 映射 + equipmentNames 解析（collectEquipmentIds 死辅助随旧 Aggregator 一并移除）；`downtimeMinutes` SQL SUM 全 NULL 组返回 NULL → nz() 归零对齐原语义。
- **F-2 行序契约**：投影后内存按 (projectId,userId) 排序确定性保持原 ORDER BY（B-2）。
- 修复过程中同命令管道 grep 滤掉失败详情导致一次误判（BUILD FAILURE 无上下文）——重跑保留全量输出定位（MNT 编译错：死辅助 collectEquipmentIds 引用已删除的 DowntimeAggregator 未随旧聚合器移除）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_ba036e10，2026-09-28）——基线诚实性/A9 漏盘认定（git 考古证实）/F-1 Decision 三入口聚合形态逐核/F-4 等价/四站点护栏全部成立；**B-1 [Major]**：loadDowntimeEntries 无 orderBy（「排序已在」为假），无排序 cap=非确定性聚合截断（恰为本计划登记的 watch-only 反模式）→ 改 `(equipmentId,reason)` 分组投影 SUM+COUNT；**B-2 [Major]**：F-2 投影须保持 ORDER BY projectId,userId（行序=报表用户可见契约）→ 规格补排序；**B-3 [Major]**：同文件同模式站点漏盘（countInadequateCapabilityCharts=F-4 孪生纳入/findCapaOverdueAlert 过滤下推+排序+cap 纳入/loadProjects 豁免登记）；Minor M-1 行号刷新/M-2 悬空引用更正（ErpInvDashboardBizModel:501-503）/M-3 prj 整域未枚举表述/M-4 notNull() 原语/M-6 owner doc 路径坐实——全部修订。
- Independent draft review iteration 2: accept（同一审查代理两轮定点复核，2026-09-28）——第一轮复核 B-1 执行契约未落地（Goals 改而 Phase 2 Fix 条目未改）+ R-2 findCapaOverdueAlert 执行条目/Targets/Types/Exit 缺 + R-3 七域表述 + R-4 micro → 全部修正；**最终 RE-REVIEW: RESOLVED，计划可置 active 实施**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-2 全部退出标准达成）
- [x] 相关文档对齐：`docs/logs/2026/09-28.md` 登记；分析报告 §2.2 A9 行追加补遗回填
- [x] 已运行验证：四模块 `mvn test -pl <module> -am` 各 0 failures + `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（对照基线）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### F-1 CS 质量看板三入口 SQL 聚合重写

- Classification: `optimization candidate`
- Why Not Blocking Closure: 跨 entry 聚合维度各异 + 均值/时长类非纯 SUM + getDashboardKpi 无分组维度（无维度 SUM 坑）；cap 已消除无界面
- Successor Required: `yes`——触发条件：CS 质量看板 RT 实测超标或 owner doc 对全量语义裁决

### mnt loadScheduleIdsWithVisit 无排序截断（既有裁决 watch-only）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本 mission 前既有 setLimit(5000) 裁决；截断方向为错误数据非保守下偏，现量级远低于 cap
- Successor Required: `yes`——触发条件：visits 实测接近 5000 时改 `GROUP BY scheduleId` 投影（零成本等价修法，批次 2 ast 先例）

## Closure

Status Note: Phase 1-2 全部完成。四站点收口：F-4+孪生改 chartId 单列投影+内存 HashSet 去重（ast 先例形态；执行期发现纯维度投影仍被平台注入主键维度，首轮被护栏捕获后照先例修正）；F-2 改 (projectId,userId) 分组投影+内存排序保持行序契约（旧 loadTimesheets/Aggregator 移除）；F-1 加 createTime desc+CS_DASHBOARD_SCAN_CAP=5000（截断方向留痕）；F-3a loadVisits cap/F-3b loadDowntimeEntries 改 (equipmentId,reason) 分组投影（消费方字段名逐字对齐）；findCapaOverdueAlert 日期下推+dueDate asc+QA_ALERT_CAP=5000。验证：四触及模块联合 `mvn test -am` 全绿（cs/prj/mnt/qa 计数 0 失败，QA SPC 10/10）；全仓 `mvn clean install -DskipTests` BUILD SUCCESS；checker 除 4 处 daoFor 移除净降（R2b 236→232/R2c 1563→1559）外零漂移。执行期两处护栏捕获（QA 投影计数 3≠2、MNT 死辅助编译错）均即时修正——护栏有效。

Closure Audit Evidence:

- Auditor / Agent: **独立结束审计两轮收敛（agent_5b486d55，全程只读+独立复跑）**——
  - round 1 NEEDS REVISION（B-1 [Major] F-1 截断方向与声称相反：`addOrderField("createTime", false)`=ASC=保留最旧——平台 QueryBean.java:435 实裁，本 mission 第二次 asc/desc 陷阱；B-2 [Major] 分析报告 A9 补遗回填勾选但未落盘；M-1/M-2/M-3 文本）→ 整改：排序改 `true`=DESC+查询构造提取包级 `buildClosedTicketQuery`+白盒护栏 `testClosedTicketQueryRecencyOrderAndCap`（getOrderBy/isDesc/limit 断言，6/6 全绿）；A9 补遗回填段落盘 §2.2；M 全修。
  - **round 2 RE-REVIEW: RESOLVED**：B-1 DESC 平台源码再实裁+护栏 surefire 实证执行；B-2 回填段在位；M 全落；验证链独立复跑（cs `-o` 6/6 + checker R2b 232/R2c 1559）。审计方同意置位门控/completed/按路径圈定提交。非阻断残留：计划 Goals F-4 孪生与 Phase 1 条目的字面「GROUP BY chartId」措辞（执行形态已三处权威记载）留待 doc-only 归一；流浪 view.xml 编辑事故产物已删除。

Follow-up:

- （无阻塞跟进）
