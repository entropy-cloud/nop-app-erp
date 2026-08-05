# A1.44 maintenance-F3 报修响应 + 停机排产联动 + 额外故障 + 资产处置联动 + OEE + 维护看板 — 需求-实现符合性五级追踪审计报告

> 报告类型：MA1(RC) 五级追踪审计（requirement-compliance mission）
> 工作项：A1.44（MA1 需求追踪矩阵审计 — maintenance-F3）
> UC 覆盖：UC-MAIN-05 / UC-MAIN-06 / UC-MAIN-07 / UC-MAIN-08 / UC-MAIN-10 / UC-MAIN-11（6 UC）
> 计划：`docs/plans/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）
> 真相源层级：L1=`docs/design/maintenance/use-cases.md`（权威）；L2=`equipment-integration.md §一/§四/§六` + `state-machine.md §维护请求/§4` + `dashboards.md §维护看板`（设计参考，冲突以 L1 为准）
> 本报告产出日：2026-08-05
> HEAD 复核 commit：工作树 dirty 仅文档变更，零 Java/ORM/契约变更

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/maintenance/use-cases.md`（功能契约真相源，权威性层级 2）

### UC-MAIN-05 报修响应性维护（use-cases.md:88-99）

**场景**：报修请求受理后生成维护访问。

**可验证断言**（逐字引用 use-cases.md:93-97，见 state-machine.md §维护请求）：
```
维护请求 OPEN → 受理 ACCEPTED → 生成维护访问(关联请求)
维护访问 COMPLETED → 请求 COMPLETED
请求 REJECTED/CANCELLED → 不生成维护访问
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-05-A**：维护请求 OPEN → 受理 ACCEPTED → 生成维护访问（关联请求）
- **UC-MAIN-05-B**：维护访问 COMPLETED → 请求 COMPLETED（自动联动）
- **UC-MAIN-05-C**：请求 REJECTED/CANCELLED → 不生成维护访问

### UC-MAIN-06 设备故障停机影响排产（use-cases.md:103-117）

**场景**：设备故障停机,发布事件影响制造域工单排产。

**可验证断言**（逐字引用 use-cases.md:108-115，见 equipment-integration.md §四）：
```
设备.状态 = DOWN(故障停机) →
  创建停机记录(DowntimeEntry)
  发布事件(设备停机) →
    制造域接收 → 暂停该设备的工单排产
设备恢复(RUNNING) →
  发布事件(设备恢复) → 恢复排产
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-06-A**：设备.状态 = DOWN → 创建停机记录(DowntimeEntry)
- **UC-MAIN-06-B**：发布事件(设备停机) → 制造域接收 → 暂停该设备的工单排产
- **UC-MAIN-06-C**：设备恢复(RUNNING) → 发布事件(设备恢复) → 恢复排产

### UC-MAIN-07 维护中发现额外故障（use-cases.md:121-132）

**场景**：维护执行中发现额外故障,本次记录并另开请求。

**可验证断言**（逐字引用 use-cases.md:126-130，见 state-machine.md §4）：
```
维护访问 IN_PROGRESS → 发现额外故障
本次访问记录(备注/工时), 不中断本次维护
另开新维护请求(OPEN)处理额外故障
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-07-A**：维护访问 IN_PROGRESS → 发现额外故障
- **UC-MAIN-07-B**：本次访问记录(备注/工时)，不中断本次维护
- **UC-MAIN-07-C**：另开新维护请求(OPEN)处理额外故障

### UC-MAIN-08 设备资产处置联动（use-cases.md:136-147）

**场景**：关联的资产被处置(SCRAPPED/SOLD),设备联动停用。

**可验证断言**（逐字引用 use-cases.md:141-145，见 equipment-integration.md §一）：
```
设备.资产(asset_id 关联) →
  资产 SCRAPPED/SOLD → 设备.状态 = DECOMMISSIONED(停用)
  设备不可再被新维护计划/工单引用
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-08-A**：资产 SCRAPPED/SOLD → 设备.状态 = DECOMMISSIONED(停用)（自动联动）
- **UC-MAIN-08-B**：设备不可再被新维护计划/工单引用（DECOMMISSIONED 守卫）

### UC-MAIN-10 OEE 计算（use-cases.md:166-179）

**场景**：计算设备 OEE(可用率×性能×质量)。

**可验证断言**（逐字引用 use-cases.md:171-177，见 equipment-integration.md §六）：
```
可用率 = 实际运行时长 / 计划生产时长 (排除停机)
性能效率 = 实际产量 / 理论产量
质量合格率 = 合格品 / 总产量
OEE = 可用率 × 性能效率 × 质量合格率
数据来源: 设备状态记录/工单报工/质检(见 §六)
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-10-A**：可用率 = 实际运行时长 / 计划生产时长（排除停机）
- **UC-MAIN-10-B**：性能效率 = 实际产量 / 理论产量
- **UC-MAIN-10-C**：质量合格率 = 合格品 / 总产量
- **UC-MAIN-10-D**：OEE = 可用率 × 性能效率 × 质量合格率
- **UC-MAIN-10-E**：数据来源 = 设备状态记录 / 工单报工 / 质检（见 §六）

### UC-MAIN-11 维护看板（use-cases.md:187-204）

**场景**：维护看板的指标展示与异常预警。见 ../dashboards.md §维护看板。

**可验证断言**（逐字引用 use-cases.md:192-202）：
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  设备总数/运行数/待处理请求, OEE, 状态分布, 停机/维护逾期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-11-A**：KPI 卡片值 == 对应实体的实时聚合（按期间/orgId/权限过滤）
- **UC-MAIN-11-B**：KPI 含 OEE 指标
- **UC-MAIN-11-C**：预警项 == 满足阈值条件的记录（阈值来自系统配置，非硬编码）
- **UC-MAIN-11-D**：看板数据受行级权限约束（只看自己组织/部门/成本中心）

---

## 2. 实现证据（L3，代码路径 `file#method` + 关键行为断言）

> 引用格式锚点 = 文件路径 + 方法名 + 关键行为断言；行号为写时实测导航，漂移不构成引用失效。

### UC-MAIN-05 报修响应性维护（请求状态机完整 + visit↔request 联动缺失）

调用链（全路径列全）：
- 请求侧（5 per-mutation Processor，薄 facade `ErpMntRequestBizModel`）：
  - `.../entity/ErpMntRequestBizModel.java#accept`（:37，`@BizMutation` 委派 acceptProcessor）→ `.../processor/ErpMntRequestAcceptProcessor.java#accept`（:19，OPEN→ACCEPTED 编排）。
  - `#startRepair`（:43）→ `ErpMntRequestStartRepairProcessor#startRepair`（ACCEPTED→IN_PROGRESS）。
  - `#complete`（:49）→ `ErpMntRequestCompleteProcessor#complete`（:15，IN_PROGRESS→COMPLETED 守卫 + 状态翻转 + completedAt）。
  - `#rejectRequest`（:55）→ `ErpMntRequestRejectRequestProcessor#rejectRequest`（→REJECTED）。
  - `#cancel`（:61）→ `ErpMntRequestCancelProcessor#cancel`（→CANCELLED）。
- 访问侧 complete：`.../entity/ErpMntVisitBizModel.java#complete`（:46）→ `.../processor/ErpMntVisitCompleteProcessor.java#complete`（:27，IN_PROGRESS→COMPLETED + endTime/totalMinutes + 设备恢复 + GL 过账）。

关键行为断言（写时实测）：
- **accept 生成 visit**：`ErpMntRequestAcceptProcessor#generateResponsiveVisit:27-36` 构造 `code="VST-REQ-"+requestId / equipmentId / visitDate / status=DRAFT / visitType=RESPONSIVE / assignedTo`，委派 `visitBiz.save`。✅ 生成 visit。
- **accept-visit 关联缺失**：`generateResponsiveVisit:28-35` 的 data map **未设 requestId**；且 `ErpMntVisit` 实体（`_ErpMntVisit.java`）属性集 = id/code/scheduleId/equipmentId/visitDate/status/assignedTo/completedBy/completedAt/startTime/endTime/totalMinutes/visitType/result/remark/orgId/businessDate/posted/...，**无 requestId 字段**（grep `requestId` 跨 erp-mnt-dao 仅 `ErpMntSparePartUsage` 命中）。visit 仅经 code 前缀 + equipmentId 隐式关联请求，无显式 FK。⚠️ "关联请求"无结构载体。
- **visit complete 不回写 request**：`ErpMntVisitCompleteProcessor#doComplete:35-54` 仅翻转 visit 状态 + 设备恢复 + GL 过账，**零 request 操作**。request COMPLETED 须经独立 mutation `ErpMntRequestCompleteProcessor#complete`（守卫 IN_PROGRESS）手工触发。⚠️ "维护访问 COMPLETED → 请求 COMPLETED"自动联动缺失。
- **reject/cancel 不生成 visit**：`ErpMntRequestRejectRequestProcessor` / `ErpMntRequestCancelProcessor` 仅翻转 request 状态，零 visit 操作。✅ 满足 UC-MAIN-05-C。

### UC-MAIN-06 停机影响排产（停机记录完整 + 跨域事件发布完全缺失）

调用链（全路径列全）：
- `.../entity/ErpMntDowntimeEntryBizModel.java`（薄 facade）→ 2 per-mutation Processor：
  - `#record` → `.../processor/ErpMntDowntimeEntryRecordProcessor.java#record`（:16，未结束守卫 + startTime 兜底 + 落库 + 设备→DOWN）。
  - `#complete` → `.../processor/ErpMntDowntimeEntryCompleteProcessor.java#complete`（:18，已开始守卫 + 未结束守卫 + endTime/totalMinutes + 落库 + 设备→RUNNING）。
- 设备状态联动：`.../support/EquipmentStatusLinker.java#linkToDown`（record 侧）+ `#restoreToRunning`（complete 侧）。

关键行为断言（写时实测）：
- **创建停机记录**：`ErpMntDowntimeEntryRecordProcessor#doRecord:24-29` startTime 兜底 + 落库；`#record:20` 调 `equipmentStatusLinker.linkToDown`。✅ 满足 UC-MAIN-06-A。
- **跨域事件发布零实现**：grep `publish|IErpSysEventBus|notify.*event|IErpEvent|emit|EventBus|publishEvent|fireEvent` 跨 `module-maintenance` 全模块 **No files found**（零命中）。record/complete 两 Processor 均零事件发布。`EquipmentStatusLinker` 仅本地 setStatus，无事件广播。❌ UC-MAIN-06-B/C "发布事件 → 制造域接收 → 暂停/恢复排产"完全缺失。
- **制造域无消费者**：grep 制造域无 downtime/equipment-stop 事件订阅站点（maintenance 无任何事件发布，制造域无从消费）。

### UC-MAIN-07 额外故障（本次记录载体存在 + 另开新请求编排完全缺失）

关键行为断言（写时实测）：
- **本次记录载体存在**：`ErpMntVisit` 实体含 `result`/`remark` 字段（`_ErpMntVisit.java:77/81` PROP_NAME_result/remark），可记录备注；`totalMinutes` 可记工时。✅ UC-MAIN-07-B 载体存在。
- **另开新请求编排零实现**：grep `additionalFault|additionalIssue|openNewRequest|newRequest|额外故障` 跨 `module-maintenance` 全模块 **No files found**（零命中）。无"维护访问 IN_PROGRESS → 发现额外故障 → 另开新维护请求(OPEN)"编排方法。❌ UC-MAIN-07-C 完全缺失。

### UC-MAIN-08 资产处置联动（DECOMMISSIONED 状态常量存在 + 自动联动监听器完全缺失）

调用链（全路径列全）：
- `.../entity/ErpMntEquipmentBizModel.java#changeStatus`（:20，`@BizMutation`，通用手工状态变更：requireEntity + setStatus + updateEntity）。

关键行为断言（写时实测）：
- **DECOMMISSIONED 常量存在**：`ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED = "DECOMMISSIONED"`（`_ErpMntDaoConstants.java:99`），dashboard `countEquipmentNotDecommissioned` 过滤 `ne("status", DECOMMISSIONED)`。✅ 状态常量 + dashboard 过滤存在。
- **资产处置自动联动零实现**：grep `DECOMMISSIONED|assetDisposal|onAssetDisposal|assetStatus|SCRAPPED|SOLD|disposal` 跨 `module-maintenance` 全模块——仅命中 dashboard 过滤行 + `ErpMntDaoConstants` 常量 + 测试种子；**零事件监听器**将"资产 SCRAPPED/SOLD"自动映射到"设备 DECOMMISSIONED"。`changeStatus` 是通用手工方法（须操作员显式调用 newStatus=DECOMMISSIONED），非 assets 域事件驱动。❌ UC-MAIN-08-A "资产 SCRAPPED/SOLD → 设备 DECOMMISSIONED"自动联动完全缺失。
- **DECOMMISSIONED 守卫缺失**：grep 无"DECOMMISSIONED 设备不可被新维护计划/工单引用"的引用前置校验。❌ UC-MAIN-08-B 缺失（设备置 DECOMMISSIONED 后无 active 守卫阻止新引用）。
- **assets 域反向调用待核**：须核 assets 域处置 Processor 是否反向调 `IErpMntEquipmentBiz.changeStatus`（本切片静态 grep maintenance 侧零监听器；assets 域处置侧联动交 SP-3 运行时确认）。

### UC-MAIN-10 OEE 计算（完全缺失，零实现）

关键行为断言（写时实测）：
- **OEE 零实现**：grep `OEE|computeOee|availability|performanceEfficiency|qualityRate|可用率|性能效率|质量合格率|calculateOee` 跨 `module-maintenance` 全模块——**仅 1 命中**：`ErpMntDashboardBizModel.java:49` javadoc `OEE 指标 Non-Goal（精确性能/质量分量需设备采集数据，见 plan 2026-07-06-1606-1 Non-Goals）`。❌ UC-MAIN-10-A/B/C/D/E 五条验收标准全部不可满足——可用率/性能效率/质量合格率/OEE 乘积/数据来源聚合零实现。
- **数据来源实体现状**：`ErpMntDowntimeEntry`（停机时长可得）+ `ErpMntEquipment`（status）可推可用率的"实际运行时长"分子；但 `ErpMntEquipment` 无累计运行时长字段（A1.42 P1-RC-064 已确认），"计划生产时长"/"实际产量"/"理论产量"/"合格品"须跨域取 mfg 工单报工 + qa 质检数据，maintenance 域零跨域读站点。

### UC-MAIN-11 维护看板（KPI 聚合 + 预警 config 完整 + OEE 卡片缺失 + 行级权限 reuse）

调用链（全路径列全）：
- `.../dashboard/ErpMntDashboardBizModel.java`（服务型 BizObject，`@BizModel("ErpMntDashboard")` 非 CrudBizModel 继承，注入 `IDaoProvider`/`IOrmTemplate`）：
  - `#getDashboardKpi:60`（`@BizQuery`，设备总数/运行数/待处理请求/期内访问数实时聚合）。
  - `#getEquipmentStatusDistribution:86`（`@BizQuery`，DB 级 GROUP BY status + COUNT）。
  - `#findEquipmentDowntimeAlert:111`（`@BizQuery`，status=DOWN 且 DowntimeEntry.endTime=null）。
  - `#findMaintenanceOverdueAlert:138`（`@BizQuery`，阈值经 `AppConfig.var` 读 config）。

关键行为断言（写时实测）：
- **KPI 实时聚合**：`getDashboardKpi:60-82` 经 `countEquipmentNotDecommissioned`/`countEquipmentByStatus`/`countRequestsByStatus`/`countCompletedVisitsInRange` 四 count 查询实时聚合，非硬编码。✅ UC-MAIN-11-A。
- **OEE 卡片缺失**：`getDashboardKpi` 返回 kpi map 含 equipmentTotal/runningCount/openRequestCount/periodVisitCount，**无 OEE 字段**。❌ UC-MAIN-11-B（reuse UC-MAIN-10 finding P1-RC-071 同根因）。
- **预警阈值 config 驱动**：`findMaintenanceOverdueAlert:139-141` 经 `AppConfig.var(ErpMntConstants.CONFIG_DASH_MNT_MAINTENANCE_OVERDUE_DAYS, DEFAULT_...)` 读 `erp-dash.mnt-maintenance-overdue-days`，非硬编码。✅ UC-MAIN-11-C。
- **行级权限未应用**：BizModel 经 `IDaoProvider:55`/`IOrmTemplate:57` 直接访问，所有 QueryBean 零 orgId 过滤，`IServiceContext` 收而不用。⚠️ UC-MAIN-11-D reuse **P1-MA2-093**（resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`——SP-5 运行时覆盖确认交 MA4）。

---

## 3. 测试证据（L4，测试断言引用 + 断言强度）

> 断言强度：强断言 = 断言验收标准语义；弱断言 = 仅断言非空/状态码；仅冒烟 = 仅通过无值断言。

### UC-MAIN-05 测试证据

- `TestErpMntVisitRequestStateMachine.java#testRequestAcceptGeneratesResponsiveVisit:158-174`：**强断言**——accept → ACCEPTED + 生成 visit（visitType=RESPONSIVE, status=DRAFT, equipmentId 匹配）。**未断言** visit.requestId 关联（字段不存在）。
- `TestErpMntVisitRequestStateMachine.java#testRequestFullFlow:195-209`：**强断言**——accept→startRepair→completeRequest 链路。**关键**：request COMPLETED 经独立 `completeRequest` 手工调用（:207），非 visit complete 自动联动——**证实 UC-MAIN-05-B 自动联动缺失**。
- `TestErpMntDowntimeAndE2E.java#testResponsiveRequestFullFlow:190-218`：**强断言**——报修全场景。**关键**：`completeVisit(visit.getId()):212` 后仍须手工 `completeRequest(requestId):216`（独立两步），证实 visit→request 无自动联动。

### UC-MAIN-06 测试证据

- `TestErpMntDowntimeAndE2E.java#testDowntimeRecordSetsDownAndCompleteRestores:86-116`：**强断言**——record 设备 DOWN + complete 设备 RUNNING + totalMinutes + 终态保护。**仅断言本地停机记录 + 设备状态**，**零跨域事件发布/制造域排产暂停断言**（候选缺口证实——仅本地记录断言）。

### UC-MAIN-07 测试证据

- **零 dedicated 测试**：grep `additionalFault|additionalIssue|openNewRequest|额外故障` 跨 `module-maintenance` 测试目录零命中。UC-MAIN-07-C 无测试覆盖。

### UC-MAIN-08 测试证据

- **零 dedicated 测试**：grep `assetDisposal|onAssetDisposal|SCRAPPED|SOLD.*DECOMMISSIONED|disposal.*equipment` 跨 `module-maintenance` 测试目录零命中。UC-MAIN-08-A 自动联动无测试覆盖。`TestErpMntDashboard.testKpiCounts:72` 仅种子 DECOMMISSIONED 设备验证 dashboard 计数排除（非联动测试）。

### UC-MAIN-10 测试证据

- **零测试**：OEE 零实现 → 零 dedicated 测试。grep OEE 跨测试目录仅 dashboard javadoc 注释。

### UC-MAIN-11 测试证据

- `dashboard/TestErpMntDashboard.java`（6 @Test）：`testKpiCounts:66` **强断言**（equipmentTotal=2/runningCount=1/openRequestCount=1/periodVisitCount=1）+ `testEquipmentDowntimeAlertTriggersAndNot:106` **强断言**（DOWN + 未恢复触发 / 已恢复不触发）+ `testMaintenanceOverdueAlertTriggersAndNot:124` **强断言**（阈值 + active + 无 Visit 三条件）。**零 OEE 断言** + **零行级权限断言**（orgId 过滤）。
- E2E：`tests/e2e/dashboards/maintenance.value.spec.ts`（KPI 值断言）+ `maintenance.smoke.spec.ts`（冒烟）。须核 OEE/行级权限断言（候选零）。

---

## 4. 运行时行为证据（L5）

> 本切片为 maintenance 响应/联动/OEE/看板需求契约符合性首份证据（与 A1.42/A1.43 并列 maintenance 域）。无 maintenance 专属 MA2 报告。L5 存疑点登记 §7 静态存疑点清单交 MA4。

| UC | L5 行为证据 | 来源 |
|----|------------|------|
| UC-MAIN-05 | 请求状态机 6 态 + accept 生成 visit 已证实（A2.14 maintenance visit/request 状态机 PASS）；visit→request 自动联动 + requestId FK 缺失为本切片新发现 | A2.14 + 本切片静态 grep |
| UC-MAIN-06 | 停机记录 + 设备 DOWN/RUNNING 联动已证实（TestErpMntDowntimeAndE2E 强测）；跨域事件发布/制造域排产暂停为本切片新发现（零事件发布） | 本切片 TestErpMntDowntimeAndE2E + grep |
| UC-MAIN-07 | 本次记录载体（remark/result）存在；另开新请求编排为本切片新发现（零实现） | 本切片 grep |
| UC-MAIN-08 | DECOMMISSIONED 常量 + dashboard 排除过滤存在；资产处置自动联动为本切片新发现（零监听器） | 本切片 grep |
| UC-MAIN-10 | OEE 零实现（dashboard javadoc Non-Goal + grep 零命中） | 本切片 grep |
| UC-MAIN-11 | KPI 聚合 + 预警 config 驱动已证实（TestErpMntDashboard 强测）；OEE 卡片 + 行级权限为本切片新发现 | TestErpMntDashboard + grep |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论，§2 判据）

### 五级追踪矩阵

| UC 编号 | L1 需求契约 | L2 owner doc（设计参考） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---------|------------|------------------------|------------|------------|--------------|-----------|
| UC-MAIN-05 | use-cases.md:93-97（逐字见 §1） | state-machine.md §维护请求（设计参考，冲突以 L1 为准） | `ErpMntRequestAcceptProcessor#accept:19`（生成 visit ✅，无 requestId FK ⚠️）+ `ErpMntVisitCompleteProcessor#complete:27`（不回写 request ⚠️）+ `ErpMntRequestCompleteProcessor#complete:15`（独立手工 complete） | TestErpMntVisitRequestStateMachine#testRequestAcceptGeneratesResponsiveVisit（强，未断言 requestId）+ #testRequestFullFlow（强，证实手工两步） | 状态机 6 态已证实；visit→request 自动联动缺失 | **P1**（P1-RC-067） |
| UC-MAIN-06 | use-cases.md:108-115（逐字见 §1） | equipment-integration.md §四（活跃要求事件发布 + 制造域消费，设计参考） | `ErpMntDowntimeEntryRecordProcessor#record:16`（创建记录 + 设备 DOWN ✅）+ grep 事件发布跨 module-maintenance **零命中** ❌ | TestErpMntDowntimeAndE2E#testDowntimeRecordSetsDownAndCompleteRestores（强，仅本地断言零跨域事件） | 停机记录完整；跨域事件发布/排产暂停完全缺失 | **P1**（P1-RC-068） |
| UC-MAIN-07 | use-cases.md:126-130（逐字见 §1） | state-machine.md §4（活跃要求另开新请求，设计参考） | visit remark/result 载体存在 ✅；grep `additionalFault\|openNewRequest` **零命中** ❌ | 零 dedicated 测试 | 本次记录载体存在；另开新请求编排完全缺失 | **P1**（P1-RC-069） |
| UC-MAIN-08 | use-cases.md:141-145（逐字见 §1） | equipment-integration.md §一（活跃要求资产处置→设备 DECOMMISSIONED 联动，设计参考） | `ErpMntEquipmentBizModel#changeStatus:20`（通用手工，非事件驱动）+ grep 资产处置监听器 **零命中** ❌ | 零 dedicated 测试 | DECOMMISSIONED 常量存在；资产处置自动联动完全缺失 | **P1**（P1-RC-070） |
| UC-MAIN-10 | use-cases.md:171-177（逐字见 §1） | equipment-integration.md §六（活跃要求 OEE 三分量 + 数据来源，设计参考）+ dashboards.md §维护看板:192（**产品基线外**标注——§4 三判据复核见下） | `ErpMntDashboardBizModel:49` javadoc Non-Goal + grep OEE/availability/performance **零实现** ❌ | 零测试 | OEE 零实现 | **P1**（P1-RC-071） |
| UC-MAIN-11 | use-cases.md:192-202（逐字见 §1） | dashboards.md §维护看板（KPI 口径 + 预警阈值 + 行级权限，设计参考） | `ErpMntDashboardBizModel#getDashboardKpi:60`（实时聚合 ✅）+ `#findMaintenanceOverdueAlert:138`（config 阈值 ✅）+ 无 OEE 卡片 ❌（reuse UC-MAIN-10）+ IDaoProvider 直访零 orgId ⚠️（reuse P1-MA2-093） | TestErpMntDashboard 6 @Test（强 KPI/预警）+ E2E maintenance.value.spec.ts；零 OEE/行级权限断言 | KPI 聚合 + 预警 config 已证实；OEE 卡片 + 行级权限缺失 | **接受 on A/C** + reuse P1-RC-071（OEE 卡片）+ reuse P1-MA2-093（行级权限） |

### 每 UC 符合性结论（§2 判据 + §4 三判据复核）

#### UC-MAIN-05 = **P1**（new P1-RC-067）

- **命中判据**：§2 ①（行为实质偏离验收标准——UC-MAIN-05-B "维护访问 COMPLETED → 请求 COMPLETED"自动联动缺失，须手工独立 mutation）。
- **三源对照**：L1 use-cases.md:94-95 明确"维护访问 COMPLETED → 请求 COMPLETED"为自动联动；L3 `ErpMntVisitCompleteProcessor#complete` 零 request 操作；L4 testRequestFullFlow 证实手工两步。
- **§4 三判据**：不适用（无 Deferred/Non-Goal 标注，L1 明确要求自动联动）。
- **触及保护区域**：修复须为 `ErpMntVisit` 加 `requestId` FK 列（**ORM 结构变更须 ask-first + 独立 plan-audit §5 ORM 类**）+ VisitCompleteProcessor 自动回写 request 逻辑（代码逻辑预授权）。

#### UC-MAIN-06 = **P1**（new P1-RC-068）

- **命中判据**：§2 ④（跨域契约行为不一致——L1 要求 maintenance 发布设备停机/恢复事件供制造域消费暂停/恢复排产，实际零事件发布）。
- **三源对照**：L1 use-cases.md:111-114 明确"发布事件(设备停机) → 制造域接收 → 暂停该设备的工单排产"；L2 equipment-integration.md §四:154-188 活跃要求事件发布 + 制造域消费；L3 grep 事件发布跨 module-maintenance 零命中。
- **§4 三判据**：均不成立——(i) 无独立 plan-audit 裁决跨域事件裁剪；(ii) owner doc equipment-integration.md §四活跃要求事件发布未声明 Deferred；(iii) product-scope 未裁剪。
- **触及保护区域**：修复须与 manufacturing 域工单排产协调（跨域契约 ask-first）+ maintenance 侧事件发布属代码逻辑预授权。

#### UC-MAIN-07 = **P1**（new P1-RC-069）

- **命中判据**：§2 ①（功能完全缺失——UC-MAIN-07-C "另开新维护请求(OPEN)处理额外故障"编排完全缺失）。
- **三源对照**：L1 use-cases.md:129 明确"另开新维护请求(OPEN)"；L2 state-machine.md §4 活跃要求；L3 grep `additionalFault|openNewRequest` 零命中。
- **§4 三判据**：均不成立——(i) 无 plan-audit；(ii) owner doc 未声明 Deferred；(iii) product-scope 未裁剪。
- **触及保护区域**：纯 BizModel/Processor 代码逻辑（新增 reportAdditionalFault → openNewRequest 编排方法），预授权不触 §5 ask-first。

#### UC-MAIN-08 = **P1**（new P1-RC-070）

- **命中判据**：§2 ①/④（功能完全缺失 + 跨域契约行为不一致——UC-MAIN-08-A "资产 SCRAPPED/SOLD → 设备 DECOMMISSIONED"自动联动完全缺失；UC-MAIN-08-B "不可再被引用"守卫缺失）。
- **三源对照**：L1 use-cases.md:143-144 明确"资产 SCRAPPED/SOLD → 设备 DECOMMISSIONED(停用) + 不可再被引用"；L2 equipment-integration.md §一:30-43 活跃要求资产状态联动（资产处置事件 → 维护域消费 → 设备停用）；L3 grep 资产处置监听器零命中，`changeStatus` 为通用手工方法。
- **§4 三判据**：均不成立——(i) 无 plan-audit；(ii) owner doc equipment-integration.md §一活跃要求资产联动未声明 Deferred；(iii) product-scope 未裁剪。
- **触及保护区域**：修复须与 assets 域 `IErpAstDisposalBiz` 双向协调（跨域契约 ask-first）+ maintenance 侧事件监听器属代码逻辑预授权。

#### UC-MAIN-10 = **P1**（new P1-RC-071）—— §4 三判据关键裁决

- **命中判据**：§2 ①（功能完全缺失——UC-MAIN-10-A/B/C/D/E 五条验收标准全部不可满足，OEE 三分量 + 乘积 + 数据来源聚合零实现）。
- **三源对照**：L1 use-cases.md:171-177 明确要求 OEE 三分量 + 乘积 + 数据来源（设备状态记录/工单报工/质检）；L2 equipment-integration.md §六:228-246 活跃要求 OEE 公式 + 数据来源表；L3 `ErpMntDashboardBizModel:49` javadoc Non-Goal + grep OEE/availability/performance 零实现。
- **§4 三判据关键裁决**（OEE Non-Goal 标注是否经人工批准）：
  - **(i) plan 含独立 plan-audit 通过记录**：plan `2026-07-06-1606-1` 存在且 Plan Status=completed + Audit=required，但其 Non-Goals 段将 OEE 标为"采集数据缺失指标需裁定为 Non-Goal"。该 plan 裁决为 AI 子代理执行（git log 全 AI commits `feat(erp): plan-2026-07-06-1606`），按 methodology §4 line 177「代理独立审计通过 = 审计裁决质量证据，不算人工批准」→ **(i) 在"人工批准"意义上不成立**。
  - **(ii) owner doc 显式 documented simplification 标注且经人工批准**：L2 `dashboards.md §维护看板:192` 含"产品基线外——精确性能/质量分量需设备采集数据"标注，但 git log `docs(dashboards)` 全 AI commits 无人工批准痕迹（methodology §4 line 172「AI 自标 ≠ 人工批准」）→ **(ii) 不成立**。
  - **(iii) product-scope 范围裁剪登记**：product-scope.md 列 maintenance 为域能力（含设备维护/OEE），未将 OEE 明确列入"不在范围"或"后续阶段"→ **(iii) 不成立**。
  - **裁决**：三判据均不成立 → L2 Non-Goal 标注不构成合法范围裁剪。L1 use-cases.md:166-178 为活跃需求契约，权威性高于 L2（§4 冲突以 L1 为准）。**OEE 计算为 P1 强制实现**（Q4=(a) 无例外通道）。对齐 A1.42 UC-MAIN-02（运行时长触发）§4 三判据先例。
- **触及保护区域**：修复须为 `ErpMntEquipment` 加累计运行时长/OEE 字段 或 新增 OEE 聚合实体（**ORM 结构变更须 ask-first + 独立 plan-audit §5 ORM 类**）+ OEE 数据来源须与 mfg 工单报工 + qa 质检跨域协调（跨域契约 ask-first）+ OEE 计算引擎属代码逻辑预授权。

#### UC-MAIN-11 = **接受 on A/C** + reuse P1-RC-071（OEE 卡片）+ reuse P1-MA2-093（行级权限）

- **UC-MAIN-11-A（KPI 实时聚合）= 接受**：§2"接受"判据——`getDashboardKpi` 四 count 实时聚合，TestErpMntDashboard 强断言。命中"接受"。
- **UC-MAIN-11-B（OEE 卡片）= reuse P1-RC-071**：与 UC-MAIN-10 同根因（OEE 零实现），按 §7 复用规则追加 RC 交叉引用不新建。
- **UC-MAIN-11-C（预警阈值 config）= 接受**：`findMaintenanceOverdueAlert:139-141` 经 `AppConfig.var` 读 config key 非硬编码。命中"接受"。
- **UC-MAIN-11-D（行级权限）= reuse P1-MA2-093**：`ErpMntDashboardBizModel` 经 `IDaoProvider` 直访绕过认证管道，与 A2.18 `:99-101` 显式列的 11 dashboard 同根因同控制点；R1.29 全局 `ErpOrgIsolationQueryTransformer` 已 resolved，追加 RC A1.44 交叉引用注记不新建（与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 / A1.24 UC-AST-12③ / A1.27 UC-INV-11 / A1.33 UC-QA-12 行级权限复用先例一致）。SP-5 运行时覆盖确认交 MA4。

### P0 即时通道评估

**本切片无 P0**。最高级 = P1（5 新 P1 + 2 reuse）。理由：
- OEE 缺失属功能缺失类（无活跃数据破坏 / GL 不平衡 / 安全漏洞 / 核心循环断裂）。
- 跨域事件/资产联动缺失属状态一致性非数据破坏（设备状态经本地 setStatus 正确，仅跨域通知缺失）。
- visit→request 联动缺失属流程自动化缺失（request 仍可手工完成，终态可达）。
- 行级权限 reuse P1-MA2-093 resolved R1.29。

---

## 6. 与 arm-index 衔接（finding 复用/新增裁决，§7 规则）

> grep arm-index maintenance request/downtime/event/disposal/OEE/dashboard/row-level 同域同控制点后裁决。既有 maintenance finding：`P1-MA2-086`（cron 并发 resolved R1.28）、`P1-MA1-022`（跨域 daoFor resolved）、`P1-MA2-093`（看板行级权限 resolved R1.29）、`P2-RC-061`（IDLE 分支）、A1.42 `P1-RC-064/065/066`+`P2-RC-060`（调度/冲突维度）、A1.43 `P2-RC-061`。**无 UC-MAIN-05/06/07/08/10/11 需求符合性 finding**。

### 新建 finding（5 新 P1，续编 P1-RC-067~071）

| Finding ID | UC | 根因 | 与既有 finding 差异 | 触及保护区域 |
|-----------|-----|------|-------------------|-------------|
| **P1-RC-067** | UC-MAIN-05 | visit→request 自动联动缺失 + Visit 无 requestId FK | 新根因（visit↔request 双向联动维度），非 P2-RC-061（IDLE 设备恢复分支，不同控制点） | **ORM 结构[ErpMntVisit 加 requestId 列]须 ask-first** + 联动逻辑预授权 |
| **P1-RC-068** | UC-MAIN-06 | 跨域事件发布/制造域排产暂停完全缺失 | 新根因（跨域事件契约维度），非 P1-MA1-022（跨域 daoFor 平台一致性） | 跨域契约须与 mfg 工单排产协调（ask-first）+ 事件发布预授权 |
| **P1-RC-069** | UC-MAIN-07 | 额外故障另开新请求编排完全缺失 | 新根因（额外故障编排维度），无既有同控制点 | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-070** | UC-MAIN-08 | 资产处置→设备 DECOMMISSIONED 自动联动完全缺失 | 新根因（资产处置事件维度），非 P1-MA1-022（跨域 daoFor） | 跨域契约须与 assets 域 IErpAstDisposalBiz 双向协调（ask-first）+ 监听器预授权 |
| **P1-RC-071** | UC-MAIN-10 | OEE 三分量 + 乘积 + 数据来源聚合零实现 | 新根因（OEE 计算维度），§4 三判据裁决 L2 Non-Goal 不成立 | **ORM 结构[ErpMntEquipment 加累计运行时长/OEE 字段 或 新增 OEE 聚合实体]须 ask-first** + 跨域 mfg 报工 + qa 质检数据协调（ask-first）+ 计算引擎预授权 |

### 复用 finding（2 reuse）

| 复用既有 ID | UC | 投影 | 裁决 |
|-----------|-----|------|------|
| **P1-MA2-093** | UC-MAIN-11-D | ErpMntDashboardBizModel 经 IDaoProvider 直访绕过认证管道，零 orgId 过滤 | resolved R1.29 全局 `ErpOrgIsolationQueryTransformer` 覆盖直访路径，追加 RC A1.44 交叉引用注记不新建（与 A1.7/A1.11/A1.21/A1.24/A1.27/A1.33 先例一致）；SP-5 运行时覆盖交 MA4 |
| **P1-RC-071**（本切片新建） | UC-MAIN-11-B | OEE 卡片缺失 | 与 UC-MAIN-10 同根因（OEE 零实现），MR1 修复 P1-RC-071 时看板 OEE 卡片同步落地，不新建 |

### 双向可追溯

- 5 新 P1-RC-067~071 入 arm-index RC 发现追踪分区（§arm-index 更新）。
- MR1 修复行须含 finding ID 交叉引用：P1-RC-067（Visit requestId + 联动）/ P1-RC-068（跨域事件 + mfg 排产）/ P1-RC-069（额外故障编排）/ P1-RC-070（资产处置联动）/ P1-RC-071（OEE 引擎 + ORM 字段）。
- **ORM 结构类修复（P1-RC-067 Visit requestId / P1-RC-071 ErpMntEquipment OEE 字段或新实体）须 ask-first + 独立 plan-audit**。
- **跨域协调**：UC-MAIN-06 须与 manufacturing 域工单排产协调；UC-MAIN-08 须与 assets 域 IErpAstDisposalBiz 协调（双向）；UC-MAIN-10 OEE 数据来源须与 mfg 工单报工 + qa 质检跨域协调。
- **行级权限随 P1-MA2-093/R1.29 全局 transformer 方向**（reuse）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> 登记 L5 无法静态定论、需运行时确认的点。每存疑点一行。

- **SP-1**（UC-MAIN-05）：accept 运行时生成的 visit 是否经其他隐式机制（如 code 解析）关联回 request；visit complete 运行时是否经其他 hook（如 ORM 拦截器）回写 request 状态——静态 grep 零 hook/拦截器，倾向确认无自动联动，运行时确认。
- **SP-2**（UC-MAIN-06）：DowntimeEntry record/complete 运行时是否经其他隐式通道（如平台事件总线全局拦截）发布跨域事件——静态 grep 零事件发布，倾向确认无，运行时确认；制造域是否有 downtime 设备停机事件消费者运行时确认。
- **SP-3**（UC-MAIN-08）：资产 SCRAPPED/SOLD 运行时（assets 域处置 Processor）是否反向调 `IErpMntEquipmentBiz.changeStatus(DECOMMISSIONED)`——静态 grep maintenance 侧零监听器，须核 assets 域处置侧是否有反向调用（双向联动确认）。
- **SP-4**（UC-MAIN-07）：额外故障运行时是否支持经 visit remark/result + 手工创建 request 的半自动流程——静态无编排方法，运行时确认是否有前端编排补全。
- **SP-5**（UC-MAIN-11）：全局 `ErpOrgIsolationQueryTransformer`（R1.29）是否覆盖 `ErpMntDashboardBizModel` 的 `dao.countByQuery`/`ormTemplate.findListByQuery` 直访路径——与 A1.7 SP-4 / A1.11 SP-3 / A1.21 SP / A1.24 SP-4 / A1.27 SP / A1.33 SP 同根因（P1-MA2-093 reuse 判定），运行时注入有效性确认交 MA4 A4.1。

**P0 即时通道评估结论**：本切片无活跃数据破坏候选——OEE 缺失属功能缺失类；跨域事件/资产联动缺失属状态一致性非数据破坏（设备本地状态正确）；visit→request 联动缺失属流程自动化缺失（终态手工可达）；行级权限 reuse P1-MA2-093 resolved。**无 P0**。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，退出码 0（纯 reporter，退出码恒 0）。本报告**无生产代码变更**（纯审计报告 + arm-index 文档更新），checker 无回归风险。**不以 checker 脚本退出码 0 作为门控通过依据**——区分门控退出码 vs 纯 reporter 退出码（checker 是纯 reporter，真正门控在 CI workflow 解析 actual > baseline）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index maintenance request/downtime/event/disposal/OEE/dashboard/row-level 同域同控制点后给出"复用 or 新增"裁决（5 新建 P1-RC-067~071 + 2 reuse P1-MA2-093/P1-RC-071），无未经比对直接新建的 finding。

---

## 9. 与 MA2 报告差异增量声明

> 对齐 methodology §去重协议。本切片为 maintenance 响应/联动/OEE/看板需求契约符合性首份证据。

- **无 maintenance 专属 MA2 状态机/业财链路行为审计报告**。本切片为 maintenance 域 F3 响应/联动/OEE/看板行为的首份证据（与 A1.42 调度/A1.43 访问备件并列 maintenance 域 3 切片）。
- **P1-MA2-093**（orgId 查询隔离全仓未落地，resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`）引作 UC-MAIN-11 看板行级权限同型现状——**reuse**（追加 RC A1.44 交叉引用注记不新建），与 A1.7/A1.11/A1.21/A1.24/A1.27/A1.33 行级权限复用先例一致。
- **P1-MA2-086**（cron 并发，maintenance `erp-mnt-due-visit-generation` 含，resolved R1.28）非本切片维度（并发维度不重审）。
- **P1-MA1-022**（跨域 daoFor，maintenance MaintenanceLabor/IssuePostingDispatcher 命中，resolved）非本切片维度（平台一致性不重审）。
- **本切片只补需求视角差异**（use-case 验收标准视角）：
  - UC-MAIN-05 visit→request 自动联动 + requestId FK 缺失（P1-RC-067，新根因）。
  - UC-MAIN-06 跨域事件发布/制造域排产暂停缺失（P1-RC-068，新根因）。
  - UC-MAIN-07 额外故障另开新请求编排缺失（P1-RC-069，新根因）。
  - UC-MAIN-08 资产处置→设备 DECOMMISSIONED 自动联动缺失（P1-RC-070，新根因）。
  - UC-MAIN-10 OEE 计算完全缺失（P1-RC-071，新根因 + §4 三判据裁决 L2 Non-Goal 不成立）。
  - UC-MAIN-11 OEE 卡片缺失（reuse P1-RC-071）+ 行级权限（reuse P1-MA2-093）。
- **真相源冻结声明**：本审计**未修改**任何真相源（use-cases.md / equipment-integration.md / dashboards.md / product-scope.md），分歧记入本报告（§5/§6）。

---

> 本审计解除 A1.44 在 MA4（A4.1/A4.2 业财展开器）及 MR1（R1.0）链路的该切片证据缺口。**maintenance 域 MA1 三切片（A1.42/A1.43/A1.44）全 done，maintenance 域 11 UC 全覆盖收尾**。
