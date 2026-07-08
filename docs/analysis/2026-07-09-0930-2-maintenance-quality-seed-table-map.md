# 维护+质量域业务交易单据种子 — 表清单、列映射、加载拓扑序与范围裁决

> Owner: `docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md` Phase 1 Exit Criteria
> 权威源: `module-{maintenance,quality}/model/app-erp-*.orm.xml`（逐表逐列核实，非采信旧记忆）
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（21 张主数据 CSV 已 seed）
> 前序种子范式: `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（运营域域表「直 seed」范式，本计划镜像）+ `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`（同批 N=1 范式）

## 0. 约定（与 1234-1 / 1445-1 / 2210-1 / 0930-1 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（1234-1/1445-1/2210-1/0930-1 经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1445-1/2210-1/0930-1 `POSTED` 列一致）。
- 日期列值 `YYYY-MM-DD`；**datetime 列本次维护域 `erp_mnt_downtime_entry.START_TIME` 为 mandatory DATETIME，必须 seed**——格式 `YYYY-MM-DD HH:mm:ss`（经 `ConvertHelper.stringToLocalDateTime` + `TIMESTAMP_FORMAT = ISO_LOCAL_DATE + ' ' + ISO_LOCAL_TIME` 核实，与 `DateHelper.DATETIME_FORMATTER` 一致；非 mandatory datetime 审计列一律省略）。
- **关键陷阱（维护域 UoM 列名）**：`erp_mnt_spare_part_usage_line` 计量单位列 `code="UO_M_ID"`（驼峰 prop `uoMId`，与采购/库存/制造行表一致），非 `UOM_ID`。**本计划不 seed `spare_part_usage_line`**（看板/报表仅按 `spare_part_usage` 头按 visit 计数，不读行），故陷阱不触发——保留作预防性记录（successor seed 备件消耗行时必须按 `UO_M_ID`）。
- **质量域无 UoM FK**：`erp_qa_inspection_line.UNIT` 为自由文本 VARCHAR（无 FK），本计划不 seed inspection_line（看板/报表读 inspection 头），故不涉及。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

本批 11 表（维护 8 + 质量 3）仅引用 1234-1/2210-1 已 seed 主数据 + 本批先 seed 的上游域表，不引用 1445-1 P2P/O2C 单据或 2210-1 运营域表/0930-1 制造域表：

```
[1234-1/2210-1 主数据(已 seed)] md_organization/md_material/md_uom/md_warehouse/md_employee/md_partner
  /ast_asset(2210-1: AST-2026-002 数控机床，mnt equipment.assetId 跨域可选复用)
  → [维护域配置] mnt_equipment_category
    → [维护域头] mnt_equipment
      → [维护域单据/记录]
        mnt_schedule                            （equipmentId FK→equipment）
        mnt_request                             （equipmentId FK→equipment）
        mnt_downtime_entry                       （equipmentId FK→equipment，mandatory startTime DATETIME）
        mnt_visit                                （equipmentId FK→equipment，看板 PRIMARY）
          → mnt_visit_task                       （visitId FK→visit）
          → mnt_spare_part_usage                 （equipmentId FK→equipment + 可选 visitId FK→visit）
  → [质量域单据]
    qa_inspection                                （materialId/supplierId/warehouseId/inspectorId FK→md_*，看板+报表 PRIMARY）
    qa_non_conformance                           （materialId/inspectionId/supplierId FK→本域/md_*）
      → qa_action                                （ncrId FK→non_conformance，mandatory）
```

> `equipment_category` 必须先于 `equipment`：equipment.categoryId→equipment_category（categoryId 非 mandatory，但 seed 填以建立分类）。
> `equipment` 必须先于 schedule/request/downtime_entry/visit：它们的 equipmentId mandatory FK→equipment。
> `visit` 必须先于 visit_task/spare_part_usage(visitId)：visit_task.visitId mandatory FK→visit；spare_part_usage.visitId 可选 FK→visit。
> `non_conformance` 必须先于 action：action.ncrId mandatory FK→non_conformance。
> `qa_inspection` 先于 `qa_non_conformance`（ncr.inspectionId 可选 FK→inspection，seed 填以建立关联）。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 维护域（maintenance）— 8 表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_mnt_equipment_category | ID; CODE(M); NAME(M); REMARK(opt) | 1 |
| erp_mnt_equipment | ID; CODE(M); NAME(M); ORG_ID(FK org=2,opt); ASSET_ID(FK ast=2,opt 跨域复用 数控机床); WORKCENTER_ID(FK mfg,opt,留空); LOCATION_ID(FK loc,opt); CATEGORY_ID(FK category,opt); STATUS(M, dict erp-mnt/equipment-status: RUNNING/IDLE/UNDER_MAINTENANCE/DOWN/DECOMMISSIONED); SERIAL_NO(opt); MANUFACTURER(opt); MODEL(opt); INSTALL_DATE(opt DATE); WARRANTY_EXPIRY(opt DATE); REMARK(opt) | 3 |
| erp_mnt_schedule | ID; CODE(M); NAME(M); EQUIPMENT_ID(FK equipment,M); SCHEDULE_TYPE(M, dict erp-mnt/schedule-type: PREVENTIVE/PREDICTIVE/CALIBRATION); FREQUENCY(opt int); RECURRENCE_TYPE(opt, dict erp-mnt/recurrence-type); START_DATE(M DATE); END_DATE(opt DATE); NEXT_DUE_DATE(opt DATE); IS_ACTIVE(opt int, default 1) | 1 |
| erp_mnt_request | ID; CODE(M); EQUIPMENT_ID(FK equipment,M); REQUEST_DATE(M DATE); DESCRIPTION(M); PRIORITY(M, dict erp-mnt/priority: LOW/NORMAL/HIGH/URGENT); STATUS(M, dict erp-mnt/request-status: OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED); REQUESTED_BY(FK emp,M); ASSIGNED_TO(FK emp,opt); ACCEPTED_BY(FK emp,opt); REMARK(opt) | 1 |
| erp_mnt_downtime_entry | ID; EQUIPMENT_ID(FK equipment,M); START_TIME(M DATETIME `YYYY-MM-DD HH:mm:ss`); END_TIME(opt DATETIME); TOTAL_MINUTES(opt decimal); REASON(opt); RELATED_JOB_ORDER_ID(FK opt,留空); REMARK(opt) | 2 |
| erp_mnt_visit | ID; CODE(M); SCHEDULE_ID(FK schedule,opt); EQUIPMENT_ID(FK equipment,M); VISIT_DATE(M DATE); STATUS(M, dict erp-mnt/visit-status: DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED); ASSIGNED_TO(FK emp,opt); COMPLETED_BY(FK emp,opt); TOTAL_MINUTES(opt decimal); VISIT_TYPE(opt, dict erp-mnt/visit-type: PLANNED/RESPONSIVE); RESULT(opt, dict erp-mnt/visit-result: NORMAL/ABNORMAL/PARTIAL); ORG_ID(FK org=2,opt); BUSINESS_DATE(opt DATE); POSTED(opt bool, default false) | 2 |
| erp_mnt_visit_task | ID; VISIT_ID(FK visit,M); LINE_NO(M int); TASK_DESCRIPTION(M); STATUS(M, dict erp-mnt/visit-task-status: PENDING/IN_PROGRESS/COMPLETED/SKIPPED/FAILED); COMPLETED_BY(FK emp,opt); REMARK(opt) | 1 |
| erp_mnt_spare_part_usage | ID; CODE(M); ORG_ID(FK org=2,opt); VISIT_ID(FK visit,opt); REQUEST_ID(FK request,opt); EQUIPMENT_ID(FK equipment,M); BUSINESS_DATE(M DATE); WAREHOUSE_ID(FK wh,opt); TOTAL_AMOUNT(opt decimal, default 0); DOC_STATUS(M, dict erp-mnt/doc-status: DRAFT/ACTIVE/CANCELLED); APPROVE_STATUS(M, dict wf/approve-status); APPROVED_BY(opt); POSTED(opt bool, default false); REMARK(opt) | 1 |

### 2.2 质量域（quality）— 3 表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_qa_inspection | ID; CODE(M); ORG_ID(FK org=2,opt); INSPECTION_TYPE(M, dict erp-qa/inspection-type: INCOMING/IN_PROCESS/FINAL/OUTGOING); RELATED_BILL_TYPE(opt); RELATED_BILL_CODE(opt); MATERIAL_ID(FK material,M); TEMPLATE_ID(FK qa template,opt,留空); SUPPLIER_ID(FK partner,opt); WAREHOUSE_ID(FK wh,opt); BATCH_NO(opt); BUSINESS_DATE(M DATE); INSPECTION_DATE(M DATE); LOT_QUANTITY(opt decimal); SAMPLE_QUANTITY(opt decimal); INSPECTOR_ID(FK emp,opt); RESULT(M, dict erp-qa/inspection-result: PENDING/ACCEPTED/CONDITIONAL/REJECTED); DOC_STATUS(M, dict erp-qa/doc-status: DRAFT/ACTIVE/CANCELLED); APPROVE_STATUS(M, dict wf/approve-status); POSTED(opt bool, default false); REMARK(opt) | 3 |
| erp_qa_non_conformance | ID; CODE(M); NCR_DATE(M DATE); SOURCE_TYPE(opt); SOURCE_CODE(opt); MATERIAL_ID(FK material,M); INSPECTION_ID(FK inspection,opt); QUANTITY(opt decimal); DESCRIPTION(opt); SEVERITY(M, dict erp-qa/severity: LOW/NORMAL/HIGH/CRITICAL); DISPOSITION_TYPE(opt, dict erp-qa/disposition-type: SCRAP/RETURN/CONCESSION/DOWNGRADE); STATUS(M, dict erp-qa/ncr-status: OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED); SUPPLIER_ID(FK partner,opt); REMARK(opt); POSTED(opt bool, default false) | 2 |
| erp_qa_action | ID; NCR_ID(FK non_conformance,M); ACTION_TYPE(M, dict erp-qa/action-type: CORRECTIVE/PREVENTIVE/CAPA); DESCRIPTION(opt); RESPONSIBLE_PERSON(FK emp,opt); DUE_DATE(opt DATE); STATUS(M, dict erp-qa/action-status: PENDING/IN_PROGRESS/COMPLETED/OVERDUE); REMARK(opt) | 1 |

**字典码值（已核实 ORM 内联 dict 定义，dict.yaml 不存在，ORM 为权威源）**：
- `erp-mnt/equipment-status`：RUNNING/IDLE/UNDER_MAINTENANCE/DOWN/DECOMMISSIONED → 看板 `countEquipmentNotDecommissioned` 统计 status≠DECOMMISSIONED；`runningCount` 统计 RUNNING；`findEquipmentDowntimeAlert` 统计 DOWN + downtime endTime=null
- `erp-mnt/request-status`：OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED → 看板 `openRequestCount` 统计 OPEN
- `erp-mnt/visit-status`：DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED → 看板 `periodVisitCount` 统计 COMPLETED + businessDate 区间
- `erp-mnt/schedule-type`：PREVENTIVE/PREDICTIVE/CALIBRATION
- `erp-mnt/recurrence-type`：DAILY/WEEKLY/MONTHLY/YEARLY
- `erp-mnt/visit-task-status`：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/FAILED
- `erp-mnt/visit-type`：PLANNED/RESPONSIVE
- `erp-mnt/visit-result`：NORMAL/ABNORMAL/PARTIAL
- `erp-mnt/doc-status`：DRAFT/ACTIVE/CANCELLED（spare_part_usage 用 ACTIVE）
- `erp-mnt/priority`：LOW/NORMAL/HIGH/URGENT
- `erp-qa/inspection-type`：INCOMING/IN_PROCESS/FINAL/OUTGOING
- `erp-qa/inspection-result`：PENDING/ACCEPTED/CONDITIONAL/REJECTED → 看板 passRate = ACCEPTED/total；报表 acceptedCount = ACCEPTED+CONDITIONAL；rejectedCount = REJECTED
- `erp-qa/ncr-status`：OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED → 看板 `openNcrCount` 统计 IN [OPEN,IN_REVIEW]
- `erp-qa/severity`：LOW/NORMAL/HIGH/CRITICAL → ncr-capa 报表按 severity 聚合
- `erp-qa/disposition-type`：SCRAP/RETURN/CONCESSION/DOWNGRADE → `findDefectTopN` 按 dispositionType 聚合
- `erp-qa/action-type`：CORRECTIVE/PREVENTIVE/CAPA
- `erp-qa/action-status`：PENDING/IN_PROGRESS/COMPLETED/OVERDUE → `findCapaOverdueAlert` 统计 status≠COMPLETED + dueDate<today
- `wf/approve-status`（平台共享）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → spare_part_usage/inspection 的 APPROVE_STATUS 用 APPROVED

## 3. 范围 Decision（Phase 1 item 2）

**选择**：维护域 8 表（equipment_category/equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage）+ 质量域 3 表（inspection/non_conformance/action）最小连通集；**SPC 三表归 Deferred（仅核心裁决）**。

### 3.1 (a) 范围 Decision：维护 8 表 + 质量 3 表为最小集；SPC 三表归 Deferred

**替代方案分析**：
- 「含 SPC 三表（spc_chart/spc_sample/spc_capability）」：**rejected（归 Deferred）**。理由：(1) `spc_chart.parameterId` 为 mandatory BIGINT FK，但 quality ORM 无独立 `ErpQaParameter` 实体（检验参数仅以 `inspection_template_line.parameterName` 自由文本形式存在），parameterId 引用目标无 seed 实体，seed spc_chart 缺 FK 上游——与制造域 crp_load 因 workcenter 配置链依赖归 Deferred 同构；(2) `getSpcOutOfControlWarning` config-gated 默认开但 SPC 预警非核心看板 KPI（核心 KPI 为 inspection/passRate/openNcrCount，均读 inspection/non_conformance 已 seed）；(3) 不 seed 则该预警返回 outOfControlChartCount=0 属**预期非缺陷**（Goal 收窄说明，Draft Review iteration 1 已确认）。触发条件：SPC 配置链（检验参数实体/template_line 物化为 parameterId）seed 落地后，由本计划或 N=3 successor 承接。
- 「仅核心」：**selected**。维护 8 表 + 质量 3 表令看板 `getDashboardKpi` + 4 报表（maintenance-history/downtime-summary/inspection-summary/ncr-capa-summary）数值转非空，且附条件 seed 令 3 预警（findEquipmentDowntimeAlert/findMaintenanceOverdueAlert/findCapaOverdueAlert）尽力非空。

### 3.2 (b) posted Decision：统一 `posted=false`

镜像 2210-1/0930-1 裁决。依据：
1. 维护/质量域看板/报表**读域表非 GL**（经 `ErpMntDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMntReportBizModel`/`ErpQaReportBizModel` 逐方法核实，零 GL/Voucher 引用），`posted` 标志不被消费；
2. 1234-1 seed 的 `erp_md_subject` 仅 8 个 GL 科目（库存现金/银行存款/应收/库存商品/应付/主营收入/主营成本/销售费用），**无维护费用/备件消耗/质量损失/报废处置专用科目**，seed GL 凭证徒增参照复杂度且不解除额外看板阻塞；
3. 两域过账 → GL 凭证 seed 归后续（Deferred：维护/质量域业财一体端到端数值回归需 GL 串联时）。

`posted=false` 显式写入有 posted 列的事务头（visit/spare_part_usage/inspection/non_conformance），使裁决可观测。

### 3.3 (c) 日期窗口 Decision

当前日期 2026-07-09，看板默认窗口 from=2026-07-01, to=2026-07-09。

- **visit.businessDate**：置当前月（visit 1=2026-07-03）使看板 `periodVisitCount` 非空；另置 1 条历史月（visit 2=2026-06-20）使 maintenance-history 报表 + 趋势更丰满。
- **visit.visitDate**（maintenance-history 报表过滤列，经 `loadVisits` ge/le visitDate 核实）：与 businessDate 同日，落入报表查询区间（2026-06/07）。
- **downtime_entry.startTime**（downtime-summary 报表过滤列，经 `loadDowntimeEntries` ge/le startTime.atStartOfDay() 核实，mandatory DATETIME）：置 2026-07（entry 1=2026-07-02 08:00:00 ongoing endTime=null；entry 2=2026-07-05 10:00:00 已恢复），落入报表区间。
- **inspection.inspectionDate**（看板 inspectionCount + trend + inspection-summary 报表过滤列）：置当前月（2026-07-02/04/06）使本期 KPI + 趋势 + 报表非空。
- **non_conformance.ncrDate**（ncr-capa 报表过滤列 + 看板 openNcrCount 不按日期）：置当前月（2026-07-04/05）使报表非空。
- **action.dueDate**（findCapaOverdueAlert 过滤列）：置 < today（2026-07-01）使逾期预警非空。
- **schedule.nextDueDate**（findMaintenanceOverdueAlert 过滤列）：置 < today（2026-07-01）+ isActive=1 + 无 visit 关联，使逾期预警非空。

### 3.4 (d) equipment.assetId 跨域复用 Decision

复用 2210-1 已 seed asset ID 2（AST-2026-002 数控机床）作为 equipment 1.assetId，建立维护域设备 ↔ 资产卡片跨域关联（equipment ORM 显式声明 to-one asset→ErpAstAsset）。equipment 2/3 留空（无对应资产卡片，非强制）。这是可选增强，不阻塞看板（看板不读 assetId）。

### 记录设计（引用 1234-1/2210-1 已 seed 主数据固定 ID）

通用：orgId=2(ERP-CO)、material 1(产品甲/PCS)/2(产品乙)/3(原料X钢材/KG)/4(包装纸箱)、warehouse 1(WH-MAIN)/2(WH-RAW)、employee 1(张三)/2(李四)/3(王五)、partner 3(SUP-001 北方钢铁供应商)/4(SUP-002 东方化工原料厂)、asset 2(AST-2026-002 数控机床)。

**维护链**：
- **equipment_category**（1 行）：1=`MNT-CAT-MACH` 机器设备类。
- **equipment**（3 行，覆盖 RUNNING×2 + DOWN×1 驱动 equipmentTotal=3/runningCount=2 + DOWN 驱动停机预警）：
  - 1：`EQ-2026-001` 数控机床 CNC-001，orgId=2，assetId=2（跨域复用），locationId=2，categoryId=1，status=RUNNING，manufacturer=DMG MORI，model=CTX 350，installDate=2026-01-05。
  - 2：`EQ-2026-002` 注塑机 INJ-001，orgId=2，categoryId=1，status=RUNNING，installDate=2026-03-10。
  - 3：`EQ-2026-003` 输送带 CONV-001，orgId=2，categoryId=1，status=DOWN（驱动 findEquipmentDowntimeAlert），installDate=2025-11-15。
- **schedule**（1 行，overdue 驱动 findMaintenanceOverdueAlert）：1=`SCH-2026-001` 数控机床月度预防维护，equipmentId=1，scheduleType=PREVENTIVE，frequency=1，recurrenceType=MONTHLY，startDate=2026-01-01，nextDueDate=2026-07-01（< today），isActive=1。**无 visit 关联 scheduleId=1**（visits 的 scheduleId 留空）。
- **request**（1 行 OPEN）：1=`REQ-2026-001` 输送带故障报修，equipmentId=3(DOWN)，requestDate=2026-07-03，priority=URGENT，status=OPEN，requestedBy=3(王五)。
- **downtime_entry**（2 行，1 ongoing 驱动预警 + 1 resolved 驱动报表）：
  - 1：equipmentId=3(DOWN)，startTime=2026-07-02 08:00:00，endTime=null（ongoing → findEquipmentDowntimeAlert），reason=主电机烧毁。
  - 2：equipmentId=1，startTime=2026-07-05 10:00:00，endTime=2026-07-05 14:00:00，totalMinutes=240，reason=预防性维护停机。
- **visit**（2 行 COMPLETED）：1=`VIS-2026-001` 数控机床月检（scheduleId 留空，避免触发 overdue 排除），equipmentId=1，visitDate=2026-07-03，businessDate=2026-07-03，status=COMPLETED，visitType=PLANNED，result=NORMAL，totalMinutes=120，assignedTo=2，completedBy=2，orgId=2，posted=false。2=`VIS-2026-002` 注塑机响应维修，equipmentId=2，visitDate=2026-06-20，businessDate=2026-06-20，status=COMPLETED，visitType=RESPONSIVE，result=ABNORMAL，totalMinutes=90，orgId=2，posted=false。
- **visit_task**（1 行）：1：visitId=1，lineNo=1，taskDescription=更换主轴润滑油并检查精度，status=COMPLETED，completedBy=2。
- **spare_part_usage**（1 行 ACTIVE）：1=`SPU-2026-001` 数控机床备件消耗，orgId=2，visitId=1，equipmentId=1，businessDate=2026-07-03，warehouseId=2(WH-RAW)，totalAmount=170.00，docStatus=ACTIVE，approveStatus=APPROVED，posted=false。

**看板 KPI 预期**（确定性派生）：equipmentTotal=3（无 DECOMMISSIONED）、runningCount=2、openRequestCount=1、periodVisitCount=1（visit 1 businessDate 2026-07-03 ∈ [2026-07-01,2026-07-09]；visit 2 在 6 月不计）。findEquipmentDowntimeAlert=1（equipment 3 DOWN + downtime 1 endTime=null）。findMaintenanceOverdueAlert=1（schedule 1 isActive=1 + nextDueDate 2026-07-01<today + 无 visit 关联）。

**质量链**：
- **inspection**（3 行，ACCEPTED×2 + REJECTED×1 驱动 passRate/rejectedCount）：
  - 1：`INS-2026-001` 来料检验（原料X钢材），inspectionType=INCOMING，materialId=3，supplierId=3(SUP-001)，warehouseId=2，batchNo=B20260701，businessDate=2026-07-01，inspectionDate=2026-07-02，lotQuantity=100，sampleQuantity=10，inspectorId=1，result=ACCEPTED，docStatus=ACTIVE，approveStatus=APPROVED，orgId=2，posted=false。
  - 2：`INS-2026-002` 制程检验（产品甲），inspectionType=IN_PROCESS，materialId=1，warehouseId=1，batchNo=B20260702，businessDate=2026-07-03，inspectionDate=2026-07-04，lotQuantity=100，sampleQuantity=10，inspectorId=2，result=REJECTED，docStatus=ACTIVE，approveStatus=APPROVED，orgId=2，posted=false。
  - 3：`INS-2026-003` 完工检验（产品甲），inspectionType=FINAL，materialId=1，warehouseId=1，batchNo=B20260703，businessDate=2026-07-05，inspectionDate=2026-07-06，lotQuantity=80，sampleQuantity=8，inspectorId=1，result=ACCEPTED，docStatus=ACTIVE，approveStatus=APPROVED，orgId=2，posted=false。
- **non_conformance**（2 行 OPEN/IN_REVIEW）：
  - 1：`NCR-2026-001` 产品甲制程不合格，ncrDate=2026-07-04，sourceType=INSPECTION，sourceCode=INS-2026-002，materialId=1，inspectionId=2，quantity=20，description=关键尺寸超差，severity=HIGH，dispositionType=RETURN，status=OPEN。
  - 2：`NCR-2026-002` 产品甲表面缺陷，ncrDate=2026-07-05，sourceType=INSPECTION，sourceCode=INS-2026-002，materialId=1，inspectionId=2，quantity=5，description=表面划痕，severity=NORMAL，dispositionType=CONCESSION，status=IN_REVIEW。
- **action**（1 行 overdue 驱动 findCapaOverdueAlert）：1：ncrId=1，actionType=CAPA，description=供应商整改+进料复检加强+作业指导书更新，responsiblePerson=1(张三)，dueDate=2026-07-01（< today），status=IN_PROGRESS（≠COMPLETED）。

**看板 KPI 预期**（确定性派生）：inspectionCount=3（inspectionDate 2026-07-02/04/06 ∈ [2026-07-01,2026-07-09]）、passRate=2/3≈0.6667（ACCEPTED=2/total=3）、rejectedCount=1、openNcrCount=2（NCR 1 OPEN + NCR 2 IN_REVIEW）。findCapaOverdueAlert=1（action 1 status=IN_PROGRESS + dueDate 2026-07-01<today）。findDefectTopN：RETURN=1/CONCESSION=1。getSpcOutOfControlWarning：outOfControlChartCount=0（SPC 未 seed，预期）。

### 域内金额/计数自洽约束

- maintenance-history 报表：visit 1 的 taskCount=1（visit_task 1.visitId=1）、sparePartUsageCount=1（spare_part_usage 1.visitId=1）。
- downtime-summary 报表：equipment 1 聚合 totalMinutes=240（downtime 2，entry 1 endTime=null 不计 totalMinutes 为 null→0）、entryCount=1（仅 resolved 计入聚合，ongoing totalMinutes=null）。
- ncr-capa 报表：HIGH severity NCR 1 capaActionCount=1（action 1.ncrId=1）、completedActionCount=0；NORMAL severity NCR 2 capaActionCount=0。
- inspection-summary 报表：material 3 totalInspections=1/acceptedCount=1；material 1 totalInspections=2/acceptedCount=1/rejectedCount=1。

## 4. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有维护/质量域种子经 CSV INSERT 表达，无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-mnt-*.sql` / `NN-init-qa-*.sql`。

## 5. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| maintenance（设备分类+设备+计划+请求+停机+访问+任务+备件消耗） | 8（equipment_category, equipment, schedule, request, downtime_entry, visit, visit_task, spare_part_usage） | 1+3+1+1+2+2+1+1 = 12 |
| quality（质检+NCR+CAPA） | 3（inspection, non_conformance, action） | 3+2+1 = 6 |
| **合计** | **11 张表 CSV** | **18 行** |

> 11 张新维护/质量域表 CSV 加入 `_vfs/_init-data/`，与 1234-1 的 21 张主数据 + 1445-1 的 23 张 P2P/O2C + 2210-1 的 13 张运营域 + 0930-1 的 4 张制造域 CSV 共存（总计 61 + 11 = **72 张 CSV**）。

### 残留风险与防护

- 参照完整性遗漏（FK 列引用未 seed 的上游 ID，如 equipment.assetId 跨域引用需 asset 2 已 seed）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配（尤其维护域 `UO_M_ID`，本批不 seed spare_part_usage_line 故不触发，预防性记录）→ 同上启动期暴露。
- **datetime 格式**（downtime_entry.startTime mandatory DATETIME）：必须 `YYYY-MM-DD HH:mm:ss`（经 `ConvertHelper.stringToLocalDateTime` 核实）；格式错配会在启动期抛 NopException。
- 非幂等（1234-1/1445-1/2210-1/0930-1 已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。
- 字典码值错配（如 equipment-status/visit-status/inspection-result/ncr-status 非法值）→ 同上启动期暴露（dict 校验在 entity 校验阶段）。
