# 2026-08-06-0245-3 rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard maintenance-F3 响应/联动/OEE/看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.44（MA1 需求追踪矩阵审计 — maintenance-F3 报修响应 + 停机排产联动 + 额外故障 + 资产处置联动 + OEE + 维护看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.44
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.44 的 0.2 依赖）、`2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`+`2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`（同批 N=1/N=2，maintenance 域 RC 切片；本切片 N=3 为 maintenance 域 F3，覆盖 6 UC，完成 maintenance 域 11 UC 全覆盖收尾）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.44 给出 UC 清单 = `UC-MAIN-05/06/07/08/10/11`（6 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片与 A1.42/A1.43 同批，本切片覆盖 F3 响应/联动/OEE/看板 6 UC，是 maintenance 域 3 切片中 UC 数最多、候选缺口密度最高的切片（OEE Non-Goal + 跨域事件联动 + 看板行级权限）。

- **L1 需求契约（权威真相源）**：`docs/design/maintenance/use-cases.md`（机制细节引用 `state-machine.md`、`equipment-integration.md §一/§四/§六`、`dashboards.md §维护看板`，L2 设计参考）：
  - **UC-MAIN-05 报修响应性维护**（`:88`）：维护请求 OPEN → 受理 ACCEPTED → 生成维护访问（关联请求）；维护访问 COMPLETED → 请求 COMPLETED；请求 REJECTED/CANCELLED → 不生成维护访问。
  - **UC-MAIN-06 设备故障停机影响排产**（`:103`）：设备.状态 = DOWN（故障停机）→ 创建停机记录（DowntimeEntry）→ 发布事件（设备停机）→ 制造域接收 → 暂停该设备的工单排产；设备恢复（RUNNING）→ 发布事件（设备恢复）→ 恢复排产。
  - **UC-MAIN-07 维护中发现额外故障**（`:121`）：维护访问 IN_PROGRESS → 发现额外故障 → 本次访问记录（备注/工时），不中断本次维护 → 另开新维护请求（OPEN）处理额外故障。
  - **UC-MAIN-08 设备资产处置联动**（`:136`）：设备.资产（asset_id 关联）→ 资产 SCRAPPED/SOLD → 设备.状态 = DECOMMISSIONED（停用）→ 设备不可再被新维护计划/工单引用。
  - **UC-MAIN-10 OEE 计算**（`:166`）：可用率 = 实际运行时长 / 计划生产时长（排除停机）；性能效率 = 实际产量 / 理论产量；质量合格率 = 合格品 / 总产量；OEE = 可用率 × 性能效率 × 质量合格率；数据来源：设备状态记录/工单报工/质检（见 §六）。
  - **UC-MAIN-11 维护看板**（`:187`）：KPI 卡片值 == 对应实体的实时聚合（按期间/orgId/权限过滤）——设备总数/运行数/待处理请求，OEE，状态分布，停机/维护逾期预警；预警项 == 满足阈值条件的记录（阈值来自系统配置，非硬编码）；看板数据受行级权限约束（只看自己组织/部门/成本中心）。

- **L3 代码实现现状（实测，`module-maintenance/erp-mnt-service`）**——**请求状态机完整 + 停机记录完整 + 看板 KPI/预警 config 驱动(OEE/行级权限候选 P1) + 跨域事件联动/额外故障/OEE 计算候选 P1 缺失**：
  - **UC-MAIN-05 报修响应性维护（✅ 请求状态机完整，⚠️ accept→生成 visit 关联待核）**：`ErpMntRequestBizModel.java`（薄 facade）委派 5 per-mutation Processor（R6.7）：`ErpMntRequestAcceptProcessor`#accept（OPEN→ACCEPTED，:39）+ `ErpMntRequestStartRepairProcessor`#startRepair + `ErpMntRequestCompleteProcessor`#complete（→COMPLETED，:23）+ `ErpMntRequestRejectRequestProcessor`#rejectRequest（→REJECTED，:28）+ `ErpMntRequestCancelProcessor`#cancel（→CANCELLED，:28）。**待核**：①accept 是否生成维护访问并关联 requestId（L1"ACCEPTED → 生成维护访问（关联请求）"）②visit COMPLETED 是否回写 request COMPLETED（L1"维护访问 COMPLETED → 请求 COMPLETED"，须核 ErpMntVisitCompleteProcessor 是否联动 request 状态）。
  - **UC-MAIN-06 停机影响排产（⚠️ 停机记录完整 + 跨域事件发布待核，候选 P1）**：`ErpMntDowntimeEntryBizModel.java`（薄 facade）委派 `ErpMntDowntimeEntryRecordProcessor`#record（创建停机记录）+ `ErpMntDowntimeEntryCompleteProcessor`#complete（恢复）。`TestErpMntDowntimeAndE2E.java` 存在。**待核**：①record/complete 是否发布"设备停机/恢复"事件供制造域消费（L1"发布事件 → 制造域接收 → 暂停/恢复工单排产"）——grep `IErpSysEventBus|publish|notify.*event|制造域|mfg|workorder.*pause|pauseSchedule` 跨 maintenance main 是否有事件发布站点；②制造域是否有对应消费者（grep manufacturing 域 equipment downtime 事件订阅）。**候选缺口**：若仅本地 DowntimeEntry CRUD 无跨域事件发布 → UC-MAIN-06 "暂停该设备工单排产"不可满足（候选 P1 §2 ①/④ 跨域契约行为不一致）。
  - **UC-MAIN-07 额外故障（⚠️ 候选 P1/P2 待核）**：**待核** grep `额外故障|additionalFault|newRequest|openNewRequest|additionalIssue` 跨 maintenance main——维护访问 IN_PROGRESS 发现额外故障时是否支持"本次记录 + 另开新维护请求"。若仅有 visit remark 无"另开新请求"编排 → 候选 P1/P2（L1 明确"另开新维护请求(OPEN)"）。
  - **UC-MAIN-08 资产处置联动（⚠️ 候选 P1 待核）**：**待核** grep `DECOMMISSIONED|asset.*disposal|SCRAPPED|SOLD|onAssetDisposal|assetStatusEvent` 跨 maintenance main——是否有资产处置事件监听器将设备置 DECOMMISSIONED。L1 明确"资产 SCRAPPED/SOLD → 设备 DECOMMISSIONED + 不可再被新维护计划/工单引用"——若 maintenance 无 assets 域事件消费者，设备停用依赖纯手工 changeStatus，UC-MAIN-08 自动联动不可满足（候选 P1 §2 ①/④）。须核 assets 域是否反向调 IErpMntEquipmentBiz.changeStatus（双向）。
  - **UC-MAIN-10 OEE 计算（❌ 候选 P1 完全缺失）**：`ErpMntDashboardBizModel.java:49` javadoc 显式 **"OEE 指标 Non-Goal（精确性能/质量分量需设备采集数据，见 plan 2026-07-06-1606-1 Non-Goals）"**。grep `OEE|computeOee|availability|performanceEfficiency|qualityRate|可用率|性能效率|质量合格率` 跨 maintenance main——OEE 三分量计算零实现。**UC-MAIN-10 全部 4 条断言（可用率 / 性能效率 / 质量合格率 / OEE=三者乘积）+ 数据来源（设备状态记录/工单报工/质检）不可满足**——候选 **P1 §2 ①（功能完全缺失）**。**§4 三判据关键裁决**：dashboard javadoc + plan 2026-07-06-1606-1 Non-Goals 是 owner doc/plan 层标注，须核是否经**人工批准**（AI 自标 ≠ 人工批准 methodology §4 line 172）；L1 `use-cases.md:166-178` 明确要求 OEE 计算且引用 mfg 报工 + qa 质检数据来源，属活跃需求契约非 Non-Goal。
  - **UC-MAIN-11 维护看板（✅ KPI 实时聚合 + 预警 config 驱动，⚠️ 行级权限候选 P1 reuse + OEE 卡片候选 P1）**：`ErpMntDashboardBizModel.java`（服务型 BizObject，@BizModel 非实体聚合）——`getDashboardKpi:60`（设备总数/运行数/待处理请求/期内维护访问数，实时聚合 count）+ `getEquipmentStatusDistribution:86`（DB 级 GROUP BY）+ `findEquipmentDowntimeAlert:111`（status=DOWN 且 DowntimeEntry.endTime=null）+ `findMaintenanceOverdueAlert:138`（**阈值经 `erp-dash.mnt-maintenance-overdue-days` 配置** `AppConfig.var`，:139-141 ✅ 非硬编码）。经 `IDaoProvider`/`IOrmTemplate` 直接访问。**候选缺口**：①**OEE 卡片缺失**（L1 KPI 含"OEE"——与 UC-MAIN-10 同根因，reuse 不新建）②**行级权限未应用**——BizModel 经 IDaoProvider/IOrmTemplate 直接访问无 orgId/权限过滤（与 P1-MA2-093 看板直接访问绕鉴权 + P2-RC-009/P2-RC-056 看板阈值/权限同型）——**疑似复用 P1-MA2-093**（须核全局 ErpOrgIsolationQueryTransformer 是否覆盖 ErpMntDashboard 查询，resolved R1.29）。

- **L4 测试证据现状**（`module-maintenance/erp-mnt-service/src/test` + `tests/e2e`）：
  - UC-MAIN-05：`TestErpMntVisitRequestStateMachine.java`（visit↔request 状态联动）——须核 accept→生成 visit 关联 + complete→request COMPLETED 断言。
  - UC-MAIN-06：`TestErpMntDowntimeAndE2E.java`——须核是否断言跨域事件发布/制造域排产暂停（候选仅本地记录断言）。
  - UC-MAIN-07/08：须核是否有额外故障/资产处置联动测试（grep additionalFault/assetDisposal 跨测试目录）。
  - UC-MAIN-10：**零测试**（OEE 零实现）。
  - UC-MAIN-11：`dashboard/TestErpMntDashboard.java`——KPI 计数 + 停机/逾期预警断言；E2E `tests/e2e/dashboards/maintenance.value.spec.ts`（KPI 值断言）+ `maintenance.smoke.spec.ts`（冒烟）。须核 OEE 断言（候选零）+ 行级权限断言（候选零）。
  - MA5 `2026-07-29-1430-arm-ma5-{test-isolation,e2e-effectiveness}.md` 评级待引用（maintenance E2E 可能仅冒烟/弱）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **无 maintenance 专属 MA2 状态机报告**。本切片为 maintenance 响应/联动/OEE/看板行为的首份证据（与 A1.42/A1.43 并列 maintenance 域首份）。
  - **maintenance 相关既有 finding**：`P1-MA2-086`（cron 并发，maintenance `erp-mnt-due-visit-generation` 含，resolved R1.28，并发维度不重审）；`P1-MA1-022`（跨域 daoFor，maintenance MaintenanceLabor/IssuePostingDispatcher 命中，resolved plan 2026-07-29-2225-1，平台一致性维度不重审）；**P1-MA2-093**（看板直接访问绕鉴权，resolved R1.29 全局 ErpOrgIsolationQueryTransformer——UC-MAIN-11 行级权限 reuse 候选）；**P2-RC-009/P2-RC-056**（mfg/master-data 看板阈值硬编码/行级权限同型——UC-MAIN-11 预警阈值已 config 驱动故不复用阈值维度，行级权限维度核 P1-MA2-093 覆盖）。**无任何 UC-MAIN-05/06/07/08/10/11 需求符合性 finding**。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：无 maintenance 专属 MA2 报告；P1-MA2-093 引作看板行级权限同型现状（reuse 候选）；只补需求视角差异（OEE 缺失 / 跨域事件联动 / 额外故障 / 资产处置联动）。

- **arm-index 既有 finding 衔接**：grep arm-index maintenance/mnt/UC-MAIN/request/downtime/OEE/dashboard/equipment disposal → **无 UC-MAIN-05/06/07/08/10/11 finding**。P1-MA2-086/P1-MA1-022/P1-MA2-093 非 UC-MAIN 维度（reuse 注记候选）。本切片须 grep arm-index maintenance request/downtime/event/disposal/OEE/dashboard/row-level 同域同控制点后裁决复用 or 新建 `P*-RC-xxx`（续编，执行时取最新——当前至 P2-RC-059 / P1-RC-063）。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10。本切片候选偏差多为**代码逻辑**类（预授权——OEE 计算引擎 / 跨域事件发布 / 额外故障编排 / 资产处置联动监听器）；**行级权限修复随 P1-MA2-093/R1.29 全局 transformer 方向**（reuse）；若触及 ORM 结构（如 ErpMntEquipment 加累计运行时长/OEE 字段）→ **ORM 结构变更须 ask-first + 独立 plan-audit**；须在报告逐项标注触及保护区域。

- **剩余差距**：A1.44 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 A1.44 报告并登记 finding，解除 maintenance 响应/联动/OEE/看板证据缺口（本切片完成后 maintenance 域 A1.42-44 全 done，maintenance 域 11 UC 全覆盖收尾）。

## Goals

- 产出 A1.44 切片审计报告 `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-MAIN-05/06/07/08/10/11 逐条核验**每条验收标准**（完整枚举，§3，禁止跳号）：UC-MAIN-05 请求状态机/accept 生成 visit/complete 回写 request；UC-MAIN-06 DOWN→DowntimeEntry/事件发布/制造域暂停/恢复事件；UC-MAIN-07 IN_PROGRESS 额外故障记录/另开新请求；UC-MAIN-08 资产 SCRAPPED/SOLD→设备 DECOMMISSIONED/不可引用；UC-MAIN-10 可用率/性能效率/质量合格率/OEE 乘积/数据来源；UC-MAIN-11 KPI 实时聚合/OEE 卡片/预警阈值 config/行级权限。
- 对候选缺口给出分级结论：①UC-MAIN-10 **OEE 计算完全缺失**（dashboard javadoc Non-Goal + grep 零实现）倾向 **P1**（§2 ①；**§4 三判据关键裁决**：L1 明确要求 OEE + 数据来源 mfg/qa，dashboard javadoc + plan 2026-07-06-1606-1 Non-Goals 须核人工批准痕迹——AI 自标 ≠ 人工批准 methodology §4 line 172；**修复触及 ORM 结构[ErpMntEquipment 加累计运行时长/OEE 字段 或 新实体]须 ask-first**）；②UC-MAIN-06 **跨域事件发布/制造域排产暂停**（待核——若仅本地记录无事件发布倾向 **P1** §2 ①/④ 跨域契约行为不一致）；③UC-MAIN-08 **资产处置联动**（待核——若无 assets 事件消费者倾向 **P1** §2 ①/④）；④UC-MAIN-07 **额外故障另开新请求**（待核 P1/P2）；⑤UC-MAIN-11 **行级权限 reuse P1-MA2-093** + **OEE 卡片 reuse UC-MAIN-10 finding**（预警阈值已 config 驱动倾向接受 on 阈值维度）；⑥UC-MAIN-05 请求状态机 + UC-MAIN-11 KPI 聚合倾向**接受**——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编）并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复；**ORM 结构类须 ask-first**）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；audit reports 表新增 A1.44 行——maintenance 域收尾行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/equipment-integration.md/dashboards.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计 maintenance-F1/F2**（A1.42/A1.43 各自独立 plan；A1.44 只覆盖 UC-MAIN-05/06/07/08/10/11）。
- **不重审 P1-MA2-086/P1-MA1-022**（并发/跨域 daoFor 平台一致性维度，resolved，不复审）。
- **UC-MAIN-11 行级权限不新建 finding 若 P1-MA2-093 全局 transformer 覆盖 ErpMntDashboard**（reuse 注记，§7 复用规则）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.44 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.44 UC 锚点）+ `docs/design/maintenance/use-cases.md`（L1 真相源）+ `docs/design/maintenance/equipment-integration.md §一/§四/§六`+`state-machine.md §维护请求/§4`（L2 设计参考，非真相源——Deferred/Non-Goal 标注须 §4 三判据复核）+ `docs/design/dashboards.md §维护看板`（L2 设计参考）+ `docs/audits/arm-index.md`（finding 衔接）+ `docs/plans/2026-07-06-1606-1-*`（UC-MAIN-10 OEE Non-Goal 标注来源 plan，§4 三判据复核人工批准痕迹）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-maintenance/erp-mnt-service -Dtest=TestErpMntVisitRequestStateMachine,TestErpMntDowntimeAndE2E,TestErpMntDashboard`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-MAIN-05/06/07/08/10/11 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:88/103/121/136/166/187` 验收标准原文；L2 引用 `equipment-integration.md §一/§四/§六`+`state-machine.md §维护请求/§4`+`dashboards.md §维护看板`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpMntRequestBizModel`#accept/startRepair/complete/reject/cancel + 5 Request Processor + `ErpMntDowntimeEntryBizModel`#record/complete + `ErpMntDowntimeEntryRecordProcessor`/`CompleteProcessor` + `ErpMntEquipmentBizModel`#changeStatus + `ErpMntDashboardBizModel`#getDashboardKpi/getEquipmentStatusDistribution/findEquipmentDowntimeAlert/findMaintenanceOverdueAlert + grep OEE/event/disposal/additionalFault 站点（含行号）；L4 引用 `TestErpMntVisitRequestStateMachine`/`TestErpMntDowntimeAndE2E`/`dashboard/TestErpMntDashboard`#method + E2E `maintenance.value.spec.ts`（注明断言强度）；L5 标注无 maintenance 专属 MA2 报告 + P1-MA2-093 看板行级权限 reuse 候选。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条对照）：UC-MAIN-05 请求状态机（✅ 5 Processor 完整）+ **accept 生成 visit 关联**（⚠️ 待核 AcceptProcessor）+ **visit complete 回写 request**（⚠️ 待核 VisitCompleteProcessor 联动 request）；UC-MAIN-06 DowntimeEntry record/complete（✅）+ **跨域事件发布/制造域排产暂停**（⚠️ 待核 grep `IErpSysEventBus|publish|notify.*event|mfg|pauseSchedule`）；UC-MAIN-07 **额外故障另开新请求**（⚠️ 待核 grep additionalFault/newRequest）；UC-MAIN-08 **资产处置联动→DECOMMISSIONED**（⚠️ 待核 grep DECOMMISSIONED/assetDisposal/onAssetDisposal + assets 域反向调用）；UC-MAIN-10 **OEE 计算**（❌ `ErpMntDashboardBizModel:49` javadoc Non-Goal + grep OEE/availability/performance 零实现 → 全 4 断言不可满足）；UC-MAIN-11 KPI 聚合（✅）+ 预警阈值 config（✅ `erp-dash.mnt-maintenance-overdue-days`）+ **OEE 卡片**（❌ reuse UC-MAIN-10）+ **行级权限**（❌ IDaoProvider 直访 reuse P1-MA2-093 候选）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-MAIN-05/06/07/08/10/11 给出符合性结论（取最高）：UC-MAIN-10 OEE 完全缺失倾向 **P1**（**§4 三判据关键裁决**：核 dashboard javadoc + plan 2026-07-06-1606-1 Non-Goals 是否经**人工批准**——判据[i]plan-audit / [ii]owner doc 显式 Deferred 经**人工批准**痕迹（grep git log，AI 自标 ≠ 人工批准 methodology §4 line 172）/ [iii]product-scope 裁剪；L1 明确要求 OEE 且引用 mfg 报工 + qa 质检数据来源；**ORM 加 OEE/累计运行时长字段 触及保护区域须 ask-first**）；UC-MAIN-06 跨域事件 + UC-MAIN-08 资产联动（按执行时核验，缺失倾向 P1 §2 ①/④ 跨域契约行为不一致）；UC-MAIN-07 额外故障（按核验 P1/P2）；UC-MAIN-11 行级权限 **reuse P1-MA2-093**（核全局 transformer 覆盖则追加 RC 交叉引用不新建）+ OEE 卡片 **reuse UC-MAIN-10 finding** + 阈值维度接受；UC-MAIN-05 状态机 + UC-MAIN-11 KPI 聚合倾向**接受**。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 owner doc/plan Deferred/Non-Goal 标注的人工批准痕迹**）+ 触及保护区域标注。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MAIN-05/06/07/08/10/11 矩阵行（逐验收标准进入 L5 判读，6 UC 无跳号），L1 逐字引用、L2 引用 equipment-integration.md §一/§四/§六 + dashboards.md（Non-Goal 注记 §4 复核）、L3 含行号 + grep 事件/disposal/OEE 站点、L4 注明断言强度、L5 标注无专属 MA2 + P1-MA2-093 reuse 候选
- [x] UC-MAIN-05/06/07/08/10/11 有符合性结论且列明 §2 判据编号；UC-MAIN-10 P1 裁决须含 OEE Non-Goal 标注的人工批准痕迹核查结论；触及 ORM 保护区域项显式标注 ask-first

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` maintenance request/downtime/event/disposal/OEE/dashboard/row-level 同域同控制点后裁决——UC-MAIN-10 OEE 为**新根因** → 新建 P1-RC（UC-MAIN-10）；UC-MAIN-06 跨域事件 / UC-MAIN-08 资产联动若独立缺口则新建 P1-RC（视核验）；UC-MAIN-07 额外故障新建 P1/P2-RC；UC-MAIN-11 行级权限 **reuse P1-MA2-093**（核全局 transformer 覆盖则追加 RC 交叉引用不新建）+ OEE 卡片 **reuse UC-MAIN-10 finding**。执行时 grep arm-index 取最新续编号避免冲突（当前至 P2-RC-059 / P1-RC-063）。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **ORM 结构类修复（ErpMntEquipment 加累计运行时长/OEE 字段 / 可能新增 OEE 聚合实体）须 ask-first + 独立 plan-audit** + **UC-MAIN-06 跨域事件修复须与 manufacturing 域工单排产协调** + **UC-MAIN-08 资产处置联动须与 assets 域 IErpAstDisposalBiz 协调（双向）** + **UC-MAIN-10 OEE 数据来源须与 mfg 工单报工 + qa 质检跨域协调** + **UC-MAIN-11 行级权限随 P1-MA2-093/R1.29 全局 transformer 方向**。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 accept 运行时是否生成 visit 关联 + complete 回写 request / SP-2 DowntimeEntry record 运行时是否发布跨域事件 + mfg 是否消费 / SP-3 资产 SCRAPPED/SOLD 运行时是否联动设备 DECOMMISSIONED / SP-4 额外故障运行时是否支持另开新请求 / SP-5 全局 ErpOrgIsolationQueryTransformer 是否覆盖 ErpMntDashboard 查询[P1-MA2-093 复用判定]；每存疑点一行）。**P0 即时通道评估**（本切片无活跃数据破坏候选——OEE 缺失属功能缺失类；跨域事件/资产联动缺失属状态一致性非数据破坏；行级权限 reuse P1-MA2-093 resolved——评估在报告 §7 给结论）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：无 maintenance 专属 MA2 报告；P1-MA2-093 引作看板行级权限同型现状（reuse 候选）；列明只补的需求视角差异（OEE 缺失 / 跨域事件联动 / 额外故障 / 资产处置联动 / OEE 卡片）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding 入 RC 发现追踪分区；audit reports 表新增 A1.44 行（maintenance 域收尾行——A1.42-44 done 标记 maintenance 域 11 UC 全覆盖完成）。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；UC-MAIN-11 行级权限 reuse P1-MA2-093 裁决有结论；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）；P0 候选评估有结论（本切片倾向无 P0）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02db36a4effeG6kur04GthBbts，fresh session，未起草本计划）。范围/UC 覆盖（A1.44=UC-MAIN-05/06/07/08/10/11，6 UC 无跳号）/依赖（0.2 done）/结果表面/方法论（9 段 §6 + §4 三判据 + §5 ask-first + §7 reuse + §去重协议）/反 slack/模板/保护区域标注全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①`ErpMntRequestBizModel` 委派 5 per-mutation Processor（accept:38/startRepair:44/complete:50/rejectRequest:56/cancel:62）✅；②`ErpMntDowntimeEntryBizModel` record:28/complete:34 ✅；③`ErpMntDashboardBizModel.getDashboardKpi:60`/`findMaintenanceOverdueAlert:138` 用 `AppConfig.var:139` 读 `erp-dash.mnt-maintenance-overdue-days`（config 驱动非硬编码）✅；④经 `IDaoProvider:55`/`IOrmTemplate:57` 直接访问 ✅；⑤`ErpMntDashboardBizModel:49` javadoc 显式 "OEE 指标 Non-Goal（见 plan 2026-07-06-1606-1 Non-Goals）" + grep OEE/availability/performance 跨 maintenance main 仅 javadoc 命中（零实现）✅；⑥plan `2026-07-06-1606-1` 存在 + arm-index P1-MA2-093 存在 ✅；⑦baseline-inventory:378 A1.44 锚点一致 ✅；⑧arm-index 最新 ID = P2-RC-059 / P1-RC-063 ✅。ORM 结构类修复（OEE/累计运行时长字段）全标 ask-first；行级权限 reuse P1-MA2-093 + 跨域 mfg/assets/qa 协调已标注。**INFO（非阻塞）**：实际路径 `dashboard/ErpMntDashboardBizModel.java` 非 `entity/`，计划引 `:49` 未带文件夹可接受（方法论 §1 明示行号漂移非阻塞）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.44 报告 9 段齐全 + UC-MAIN-05/06/07/08/10/11 矩阵行（逐验收标准，6 UC 无跳号）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.44 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差多为**代码逻辑**类（预授权——OEE 计算引擎 / 跨域事件发布 / 额外故障编排 / 资产处置联动监听器）；**ORM 结构类（ErpMntEquipment 加累计运行时长/OEE 字段 / OEE 聚合实体）须 ask-first + 独立 plan-audit**（roadmap 预授权声明明确排除 ORM 结构变更）；**行级权限修复随 P1-MA2-093/R1.29 全局 transformer 方向**（reuse）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-MAIN-06 跨域事件须与 mfg 排产协调；UC-MAIN-08 资产处置联动须与 assets 域双向协调；UC-MAIN-10 OEE 数据来源须与 mfg 报工 + qa 质检跨域协调；UC-MAIN-11 行级权限随 P1-MA2-093/R1.29）

## Closure

Status Note: 执行完成。两 Phase 全部 `[x]`，报告 9 段齐全落盘 `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`，5 新 P1-RC-067~071 + 2 reuse（P1-MA2-093 / P1-RC-071）登记入 arm-index，audit reports 表新增 A1.44 行（maintenance 域收尾行）。零 P0。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-04-224309-mission-driver，fresh session，未执行本计划任何 Phase，未起草本计划）
- Evidence: 独立会话实测复核——①报告 `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md` 存在（38502 bytes）+ §1-§9 九段齐全（grep `^## ` 实测命中 §1 L1 真相源/§2 L3 代码证据/§3 L4 测试证据/§4 L5 运行时/§5 五级追踪矩阵+符合性结论/§6 arm-index 衔接/§7 静态存疑点/§8 过程纪律自检/§9 MA2 差异增量）；②arm-index A1.44 行落盘（audit reports 表）+ RC 交叉引用注记段（A1.44 maintenance 域收尾行）+ 5 新 finding 行 P1-RC-067/068/069/070/071（RC 发现追踪分区，每行含 §2 判据编号 + §4 三判据复核 + MR1 修复行 + todo 状态）；③P1-MA2-093 RC A1.44 交叉引用注记 + UC-MAIN-11-D 行级权限 reuse 裁决；④5 新 finding 均含 file:line 实仓引用（`ErpMntVisitCompleteProcessor#complete:27`/`ErpMntDowntimeEntryRecordProcessor#record:16`/`ErpMntEquipmentBizModel#changeStatus:20`/`ErpMntDashboardBizModel:49` 等）+ grep 实测站点（`publish|IErpSysEventBus` 跨 module-maintenance 零命中 / `additionalFault|openNewRequest` 零命中 / `OEE|availability|performance` 仅 javadoc 命中）——非空壳；⑤§4 三判据关键裁决（OEE Non-Goal）覆盖 (i)plan 2026-07-06-1606-1 AI 子代理≠人工批准 / (ii)dashboards.md §维护看板:192 git log 全 AI commits / (iii)product-scope 未裁剪 三项；⑥ORM 结构类修复（P1-RC-067 Visit requestId / P1-RC-071 ErpMntEquipment OEE 字段）全标 ask-first + 独立 plan-audit；跨域契约（P1-RC-068 mfg 排产 / P1-RC-070 assets 双向）全标 ask-first；⑦反空壳：报告 38KB 非 stub，每段含具体证据非 `return null`/`{}`/swallowed exception；⑧Deferred honesty：finding 修复正确归类 `out-of-scope improvement` + Successor Required=yes（MR0/MR1），无活跃缺陷隐藏；⑨零 P0 评估有结论（OEE 缺失=功能缺失类 / 跨域事件=状态一致性非数据破坏 / 行级权限 reuse P1-MA2-093 resolved）。两 Phase Status=completed 与全部 Exit Criteria `[x]` 一致；Closure Gates 全 `[x]`；文本一致性 PASS。

Follow-up:

- MR0/MR1 按 §10 展开本报告 finding 修复（UC-MAIN-10 OEE ORM 结构类须 ask-first + 跨域 mfg/qa 数据协调；UC-MAIN-06/08 跨域联动须与 mfg/assets 协调）。
- MA4 运行时探针展开 §7 静态存疑点清单 SP-1~SP-5（运行时确认 accept/complete 联动 / 跨域事件发布消费 / 资产处置联动 / 额外故障另开请求 / 全局 transformer 覆盖）。
