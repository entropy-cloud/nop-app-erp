# 2026-07-13-1419-3-quality-maintenance-fk-name-resolution Quality + Maintenance 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件「高价值子集验证后批量推广需求」——**已满足**：经 6 批次验证机制 D 可行性 55 实体落地）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1043-1-finance-fk-name-resolution.md`（finance 先例）、`2026-07-13-1419-1-assets-fk-name-resolution.md`（同批 N=1，无依赖）、`2026-07-13-1419-2-projects-fk-name-resolution.md`（同批 N=2，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 `module-quality/erp-qa-meta/` + `erp-qa-service/` + `erp-qa-web/` + `module-maintenance/erp-mnt-meta/` + `erp-mnt-service/` + `erp-mnt-web/`）：

### 机制 D 已验证（全域 6 批次 55 实体）

机制 D 三层接线（参考 `ErpSalOrder` + `ErpMfgWorkOrderLine` 先例）：(1) 自定义 xmeta 增派生 `*Name` prop；(2) BizModel 增 `@BizLoader(forType = Entity.class)` 经 `orm().batchLoadProps` 批量加载；(3) view.xml `<grid id="list"><cols x:override="bounded-merge">` 替换列。`ext:relation` 已在全部相关 FK `*Id` prop 声明，零 ORM 变更。

### Quality 域覆盖现状

- **零 FK 名称解析覆盖**：全部 16 实体的自定义 view.xml 为空 `<grid id="list"/>`（继承生成基线列原样）。
- **未覆盖 15 实体**（1 实体 ErpQaSamplingPlan 无 FK 列，无需工作）。

### 未覆盖 15 quality 实体清单

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpQaInspection** | orgId, materialId, templateId, supplierId, warehouseId, inspectorId | 质检单（6 FK 列） |
| 2 | **ErpQaInspectionLine** | inspectionId | 检验参数行 |
| 3 | **ErpQaInspectionTemplate** | materialId | 检验模板 |
| 4 | **ErpQaInspectionTemplateLine** | templateId | 模板行 |
| 5 | **ErpQaNonConformance** | materialId, inspectionId, supplierId | NCR 不合格品 |
| 6 | **ErpQaAction** | ncrId | CAPA 行动 |
| 7 | **ErpQaRecall** | sourceNcrId, materialId | 召回事件 |
| 8 | **ErpQaRecallTarget** | recallId, partnerId | 召回目标 |
| 9 | **ErpQaSpcChart** | orgId, materialId, inspectionTypeId | SPC 控制图 |
| 10 | **ErpQaSpcSample** | chartId, orgId, inspectorId | SPC 样本 |
| 11 | **ErpQaSpcCapability** | chartId, orgId | SPC 能力 |
| 12 | **ErpQaCalibration** | orgId | 校准 |
| 13 | **ErpQaReview** | orgId | 质量评审 |
| 14 | **ErpQaQualityGoal** | responsiblePersonId | 质量目标 |
| 15 | **ErpQaRiskRegister** | — (ownerId 为 String 非 Long FK，无数值 FK 列需解析) | 风险登记 |

> ErpQaSamplingPlan 无 FK 列，无需工作。ErpQaRiskRegister 的 ownerId 为 String（stdDomain="userId"），非数值 FK 列，不做机制 D 变更（仅保留在清单中供完整性记录）。

> **清单约定**：每实体的「原始 `*Id` FK 列」列出该实体生成网格中全部 `ui:number="true"` FK 列。ext:relation 缺口列于下方独立裁决表，两表合集 = 生成网格中全部显示的 `*Id` FK 列。

### Maintenance 域覆盖现状

- **零 FK 名称解析覆盖**：全部 12 实体的自定义 view.xml 为空 `<grid id="list"/>`。
- **未覆盖 12 实体**——本计划范围。

### 未覆盖 12 maintenance 实体清单

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpMntEquipment** | orgId, assetId, locationId, categoryId | 设备（4 FK 列） |
| 2 | **ErpMntEquipmentCategory** | parentId | 设备类别 |
| 3 | **ErpMntMaintenanceTeam** | orgId, leaderId | 维护团队 |
| 4 | **ErpMntMaintenanceTeamMember** | teamId, employeeId | 团队成员 |
| 5 | **ErpMntSchedule** | equipmentId | 维护计划 |
| 6 | **ErpMntRequest** | equipmentId | 维护请求 |
| 7 | **ErpMntVisit** | scheduleId, equipmentId, orgId | 维护访问 |
| 8 | **ErpMntVisitTask** | visitId | 访问任务 |
| 9 | **ErpMntSparePartUsage** | orgId, visitId, requestId, equipmentId, warehouseId | 备件消耗（5 FK 列） |
| 10 | **ErpMntSparePartUsageLine** | sparePartUsageId, materialId, uoMId | 备件消耗行 |
| 11 | **ErpMntDowntimeEntry** | equipmentId, relatedJobOrderId | 停机记录 |
| 12 | **ErpMntCalibration** | orgId, equipmentId | 校准 |

### ext:relation 缺口（7 个 FK 列无 `ext:relation`，阻碍名称解析）

| # | 域 | 实体 | FK prop | 处置裁决 |
|---|---|---|---|---|
| 1 | quality | ErpQaInspectionLine | parameterId | 保留原始 ID（检验参数，无 ext:relation，归 successor） |
| 2 | quality | ErpQaRecall | batchId | 保留原始 ID（弱指针→ErpInvBatch，无 ext:relation，归 successor） |
| 3 | quality | ErpQaRecallTarget | salesDeliveryId | 保留原始 ID（弱指针，归 successor） |
| 4 | quality | ErpQaRecallTarget | generatedReturnId | 保留原始 ID（弱指针，归 successor） |
| 5 | quality | ErpQaSpcChart | parameterId | 保留原始 ID（检验参数，归 successor） |
| 6 | maintenance | ErpMntEquipment | workcenterId | 保留原始 ID（跨域→mfg，无 ext:relation，归 successor） |
| 7 | quality | ErpQaRiskRegister | ownerId | 保留原始 ID（String 类型 userId，非数值 FK） |

> 裁决原则：与 finance 批次（1043-1）同口径。6 个 ext:relation 缺口 + 1 个 String 类型 FK 均为弱指针/跨域链路/系统字段，不解析。

剩余差距：26 实体（14 quality + 12 maintenance）列表页显示原始数字 ID（用户面 P1 缺陷）。ErpQaSamplingPlan（无 FK）与 ErpQaRiskRegister（String ownerId）不纳入机制 D 变更范围。

## Goals

- 26 实体（14 quality 需机制 D 变更 + 12 maintenance）列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D：xmeta `*Name` + BizModel `@BizLoader` 批量加载 + view.xml `bounded-merge`）。ErpQaSamplingPlan（无 FK 列）与 ErpQaRiskRegister（ownerId 为 String 非数值 FK）不做机制 D 变更，仅记录于清单。
- 高价值 FK 定义：维度型外键（material→materialName 读 `ErpMdMaterial.name`；supplier→supplierName 读 `ErpMdPartner.name`；warehouse→warehouseName 读 `ErpMdWarehouse.name`；inspector/responsiblePerson→employeeName 读 `ErpMdEmployee.name`；org→orgName；partner→partnerName 读 `ErpMdPartner.name`；currency→currencyName；equipment→equipmentCode 读 `ErpMntEquipment.code`；category→categoryName 读 `ErpMntEquipmentCategory.name`；location→locationName 读 `ErpMdLocation.name`；asset→assetCode 读 `ErpAstAsset.code`；leader/employee→employeeName；team→teamName 读 `ErpMntMaintenanceTeam.name`；visit→visitCode 读 `ErpMntVisit.code`；material→materialName；uom→uomName 读 `ErpMdUom.name`）+ 高价值父单型内部链路（inspectionId→inspectionCode、templateId→templateCode、ncrId→ncrCode、recallId→recallCode、chartId→chartCode、scheduleId→scheduleCode、requestId→requestCode、sparePartUsageId→usageCode——均为父单 code 承载业务上下文）。
- 零 ORM/契约变更。

## Non-Goals

- **其他域 FK 名称解析**（assets 由 `2026-07-13-1419-1` 承接；projects 由 `2026-07-13-1419-2` 承接；CRM/CS/HR/master-data/logistics/contract/b2b/drp/aps 归后续 successor）。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`。
- **7 个 ext:relation 缺口 FK 的名称解析**（见 Current Baseline 裁决表）——弱指针/跨域链路/系统字段，归 successor（触发条件：对应 ext:relation 落地或业务需求要求解析时）。
- **ErpQaSamplingPlan**（无 FK 列，无需工作）。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。

## Task Route

- Type: `app-layer design change`（改用户可见的列表页显示行为，跨 quality + maintenance 两域多实体，不改 API/模型/认证）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（机制 D 权威参考）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`
- Skill Selection Basis: xmeta 派生 + view.xml bounded-merge → `nop-frontend-dev`；BizModel `@BizLoader` → `nop-backend-dev`；JUnit 测试 → `nop-testing`。
- Protected Areas: 无 ORM/ask-first 变更。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - Quality 域 FK 名称解析（Inspection/InspectionLine/InspectionTemplate/InspectionTemplateLine/NonConformance/Action/Recall/RecallTarget/SpcChart/SpcSample/SpcCapability/Calibration/Review/QualityGoal）

Status: completed
Targets: `module-quality/erp-qa-meta/.../ErpQa*/ErpQa*.xmeta`；`module-quality/erp-qa-service/.../entity/ErpQa*BizModel.java`；`module-quality/erp-qa-web/.../ErpQa*/ErpQa*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无（机制 D 已由 6 批次验证）

- [x] `Decision`: 裁决 quality 14 实体的目标 FK 清单——维度型 FK 全部解析（material/supplier/warehouse/inspector/org/partner/responsiblePerson）。父单型内部链路 FK 解析为 code（inspectionId→inspectionCode、templateId→templateCode、ncrId→ncrCode、recallId→recallCode、chartId→chartCode、sourceNcrId→ncrCode 弱链路解析、inspectionTypeId@SpcChart→inspectionTemplateCode 同 ext:relation `inspectionType` 已声明指向 ErpQaInspectionTemplate）。7 个 ext:relation 缺口/特殊类型 FK 保留原始 ID（见 Current Baseline 裁决表）。ErpQaRiskRegister 不做机制 D 变更（ownerId 为 String 类型）。ErpQaInspectionLine 已有 denormalized parameterName 原生列，parameterId 虽保留原始 ID 但 parameterName 列已提供名称显示，不新增派生字段。
  - Skill: `nop-backend-dev`
- [x] `Add`: 14 实体 xmeta 增派生 `*Name` prop（镜像 `ErpSalOrder.xmeta`，`queryable="false" sortable="false"` + `schema type="java.lang.String"`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 14 实体 BizModel 增 `@BizLoader(forType = ErpQa*.class)` 方法（`orm().batchLoadProps` 批量加载 + null 安全读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 14 实体 view.xml `<grid id="list">` 由空占位改为 `<cols x:override="bounded-merge">`，用 `*Name` 列替换原始 `*Id` 列。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 14 quality 实体列表网格显示 `*Name` 而非原始 `*Id`（14 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）

### Phase 2 - Maintenance 域 FK 名称解析（Equipment/EquipmentCategory/MaintenanceTeam/MaintenanceTeamMember/Schedule/Request/Visit/VisitTask/SparePartUsage/SparePartUsageLine/DowntimeEntry/Calibration）

Status: completed
Targets: `module-maintenance/erp-mnt-meta/.../ErpMnt*/ErpMnt*.xmeta`；`module-maintenance/erp-mnt-service/.../entity/ErpMnt*BizModel.java`；`module-maintenance/erp-mnt-web/.../ErpMnt*/ErpMnt*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无（与 Phase 1 独立）

- [x] `Decision`: 裁决 maintenance 12 实体的目标 FK 清单——维度型 FK 全部解析（equipment→equipmentCode、category→categoryName、location→locationName、asset→assetCode、leader/employee→employeeName、org→orgName、material→materialName、warehouse→warehouseName、uom→uomName）。父单型内部链路 FK 解析为 code（scheduleId→scheduleCode、requestId→requestCode、visitId→visitCode、teamId→teamName、sparePartUsageId→usageCode、relatedJobOrderId→workOrderCode 跨域）。workcenterId@Equipment 保留原始 ID（ext:relation 缺口，归 successor）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 12 实体 xmeta 增派生 `*Name` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 12 实体 BizModel 增 `@BizLoader(forType = ErpMnt*.class)` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 12 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`，`*Name` 替换 `*Id`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 12 maintenance 实体列表网格显示 `*Name` 而非原始 `*Id`（12 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）

### Phase 3 - BizLoader 测试验证

Status: completed
Targets: `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaFkNameLoader.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntFkNameLoader.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add`: 新建 `TestErpQaFkNameLoader.java`（extends `JunitAutoTestCase`），经 `IGraphQLEngine` findList 请求 `*Name` 字段，断言 `ErpQaInspection`（materialName/supplierName/warehouseName）+ `ErpQaNonConformance`（materialName/supplierName）名称对齐。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `TestErpMntFkNameLoader.java`（extends `JunitAutoTestCase`），经 `IGraphQLEngine` findList 请求 `*Name` 字段，断言 `ErpMntEquipment`（categoryName/locationName/assetCode）+ `ErpMntSparePartUsage`（equipmentCode/warehouseName）名称对齐。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `TestErpQaFkNameLoader` + `TestErpMntFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a5d06699ffeg8TTj6FSvBmymZ, 2026-07-13) — 0 Blocker / 2 Major / 2 Minor. M1 (`ErpQaSpcChart.inspectionTypeId` resolvable FK silently dropped — ext:relation `inspectionType` exists pointing to ErpQaInspectionTemplate) — fixed: added inspectionTypeId to baseline table + Phase 1 Decision as `inspectionTemplateCode`. M2 (`ErpQaRiskRegister` counted as needing work but ownerId is String non-numeric, inflating count to 27) — fixed: scope restated as 26 entities (14 quality + 12 maintenance), RiskRegister explicitly no-op with note. m1 (convention note added above gap table), m2 (InspectionLine denormalized parameterName noted) — both fixed.
- Independent draft review iteration 2: `accept` (ses_0a5bdefeeffeGOzrbFkUJpykLV, 2026-07-13) — M1 (inspectionTypeId added to SpcChart baseline + Phase 1 Decision) + M2 (count corrected to 26, RiskRegister explicitly no-op) both fully resolved. No new issues. Count consistency verified across Goals/Phases/Closure Gates. Template compliant. Plan ready for `active`.

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-quality/erp-qa-service -am` + `mvn test -pl module-maintenance/erp-mnt-service -am`（含新增 FkNameLoader 测试）+ 26 view.xml `xmllint --noout` 一次。

- [x] 范围内行为完成（26 实体列表页 FK 显示名称；ErpQaSamplingPlan + ErpQaRiskRegister 不做机制 D 变更）
- [x] 相关文档对齐（`view-and-page-strategy.md` / `cross-module-entity-reference.md` 机制 D 范式无需更新）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + quality-service/maintenance-service `mvn test` 0 failures/0 errors + 26 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 7 个 ext:relation 缺口 FK 的名称解析 + 1 个运行时裁决不可解析 FK

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: quality 5 个（InspectionLine.parameterId / Recall.batchId / RecallTarget.salesDeliveryId+generatedReturnId / SpcChart.parameterId）+ maintenance 1 个（Equipment.workcenterId）+ quality 1 个 String 类型（RiskRegister.ownerId）均为弱指针/跨域链路/系统字段，ext:relation 缺失或类型不匹配。**运行时裁决新增**：`ErpMntDowntimeEntry.relatedJobOrderId` 经实时 ORM 核实，其 `relatedJobOrder` 关系为自引用（refEntityName=ErpMntDowntimeEntry 自身，疑似模型 bug——列名/语义指向生产工单但 ORM 声明为自指），且 ErpMntDowntimeEntry 无 `code`/`name` 显示列，无可解析的显示字段，故保留原始 ID 并从列表网格裁剪，归 successor（触发条件：关系修正为指向真实工单实体或本实体增列显示字段时）。
- Successor Required: `yes`（触发条件：对应 ext:relation 落地、DowntimeEntry 关系模型修正、或业务需求要求解析时）

### 其他域 FK 名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖 quality + maintenance 域。CRM/CS/HR/master-data/logistics/contract/b2b/drp/aps 归后续 successor。
- Successor Required: `yes`（触发条件：对应域 FK 名称解析需求落地时）

## Closure

Status Note: 全 3 Phase 完成。机制 D 三层接线在 quality 14 实体 + maintenance 12 实体全量落地（26 xmeta 派生 prop + 26 BizModel `@BizLoader` 共 54 个 loader + 26 view.xml bounded-merge），零 ORM 变更。新增 `TestErpQaFkNameLoader`（2 测试：Inspection 6 名称 + NonConformance 3 名称）+ `TestErpMntFkNameLoader`（2 测试：Equipment 4 名称 + SparePartUsage 5 名称）经 `IGraphQLEngine` 触发 `@BizLoader` 断言名称对齐 master-data/assets，全方法绿。7 个 ext:relation 缺口 + 1 String 类型 + 运行时裁决 1 个不可解析 FK（`ErpMntDowntimeEntry.relatedJobOrderId` 自引用无显示列）按 Decision 诚实保留原始 ID/从网格裁剪并归 successor。仓库验证全绿：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + quality-service 93 测试 / maintenance-service 45 测试 0 失败 0 错误 + 26 view.xml `xmllint --noout` well-formed。2 个 CRUD 快照测试（TestErpQaInspectionTemplateCrudSmoke / TestErpMntVisitCrudSmoke）因实体输出新增派生字段而重录（仅 response json5 增 `*Name`/`*Code` 字段，tables CSV 保持原 `*` 通配符不变）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_0a4b612a4，非执行者上下文）— verdict **APPROVE (closure met)**
- 实时仓库语义核实（grep/glob/read 全量）：
  - 26 view.xml 全部含 `<cols x:override="bounded-merge">` + ≥1 `*Name`/`*Code` 派生列（14 QA + 12 MNT）
  - 26 BizModel 全部含 `@BizLoader(forType = Erp{Qa,Mnt}*.class)` 共 **54 个 loader**（28 QA + 26 MNT），反空心检查通过：抽样 4 个（ErpQaInspection 6 loader / ErpQaSpcChart inspectionTemplateCode via relation `inspectionType` / ErpMntSparePartUsageLine uomName via relation `uoM` 大写 M+getUoM() / ErpMntDowntimeEntry 仅 equipmentCode 无 relatedJobOrder loader）均为真实实现（`orm().batchLoadProps` + for-loop + `getName()`/`getCode()`），零 `return null` 占位
  - 26 xmeta 全部含派生 prop（`queryable="false" sortable="false"` + `schema type="java.lang.String"`），prop 数与 loader 数逐一匹配
  - `TestErpQaFkNameLoader`（2 @Test，Inspection 6 名称 + NonConformance 3 名称）+ `TestErpMntFkNameLoader`（2 @Test，Equipment 4 名称 + SparePartUsage 5 名称）含真实 assertEquals 断言，经 IGraphQLEngine 触发 @BizLoader
  - Deferred 诚实性：7 ext:relation 缺口 + RiskRegister.ownerId(String) + 运行时裁决 relatedJobOrderId（自引用无显示列）显式记录；grep 确认无 relatedJobOrder 派生 prop/loader（未隐藏为"已完成"）
- 五点一致性：Plan Status=completed / 3 Phase Status=completed + 全 [x] + Exit Criteria [x] / Closure Gates 全 [x] / `docs/logs/2026/07-13.md` 顶部条目（quality+maintenance 26 实体）/ 父 plan `2026-07-11-1643-1` Successor Progress 记录本批次完成 — 全部一致
- 验证状态：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + quality-service 93 测试 + maintenance-service 45 测试 0 failures/0 errors（已由执行者运行确认）

Follow-up:

- 其他域 FK 名称解析 successor（见上方 Deferred）
