# 质量域 SPC 三表种子 — 表清单、列映射、加载拓扑序、Strategy 裁决与期望值派生

> Owner: `docs/plans/2026-07-09-1145-2-quality-spc-seed-value-assertion.md` Phase 1 Exit Criteria
> 权威源: `module-quality/model/app-erp-quality.orm.xml`（spc_chart :759-817 / spc_sample :820-866 / spc_capability :869-914，逐列核实，非采信旧记忆）
> 看板读源: `module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java#getSpcOutOfControlWarning` :204-221 + helpers :225-255
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（org=2 / material 1-4 / employee 1-3 已 seed）
> 前序种子范式: `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`（质量域 inspection/non_conformance/action「直 seed」范式，本计划镜像 + 解除其 SPC Deferred）

## 0. 约定（与 1234-1 / 1445-1 / 2210-1 / 0930-1 / 0930-2 / 1045-1 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（前序 6 批经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1445-1/2210-1/0930-1/0930-2 `POSTED`/`IS_OUT_OF_CONTROL` 列一致）。
- 日期列值 `YYYY-MM-DD`；**datetime 列 `erp_qa_spc_sample.SAMPLE_TIME` 为 mandatory DATETIME，必须 seed**——格式 `YYYY-MM-DD HH:mm:ss`（与 0930-2 downtime_entry.START_TIME 同机制，经 `ConvertHelper.stringToLocalDateTime` + `TIMESTAMP_FORMAT = ISO_LOCAL_DATE + ' ' + ISO_LOCAL_TIME` 核实）；非 mandatory datetime 审计列（CREATED_TIME/UPDATED_TIME/CALCULATED_AT）一律省略。
- 框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

本批 3 张新表 + 1 处既有 CSV 加性追加，仅引用 1234-1/2210-1 已 seed 主数据 + 本批先 seed 的上游域表 + 0930-2 已 seed 的 quality 表：

```
[1234-1/2210-1 主数据(已 seed)] md_organization(2) / md_material(1) / md_employee(1)
  → [SPC 配置头] qa_spc_chart                                   （orgId/materialId 逻辑 to-one，非强制）
      → [SPC 结果行]
        qa_spc_sample                                            （chartId mandatory 逻辑 to-one→chart）
        qa_spc_capability                                        （chartId mandatory 逻辑 to-one→chart）
  → [既有 quality 单据加性追加] qa_non_conformance +1 行         （materialId FK→md_material=1；inspectionId 留空）
```

> `spc_chart` 必须先于 `spc_sample`/`spc_capability`：二者的 chartId mandatory FK→chart（逻辑 to-one）。
> `qa_non_conformance` 追加行不引用 SPC 表（sourceType/code 为自由文本，非 FK），独立于拓扑序。
> **关键（与 0930-2 baseline 一致）**：`getSpcOutOfControlWarning` 三计数器仅迭代 sample/capability/non_conformance 表收集 distinct `chartId` 或行数，**从不 join 或 load `erp_qa_spc_chart`**（Nop ORM `<to-one>` 为逻辑 join 非 DB 物理外键）。即使指向不存在的 chart 行也被接受；Strategy C 选择 seed chart 仅为完整参照完整性，非看板读取所必需。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 质量域 SPC（quality）— 3 张新表 + 1 处加性追加

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_qa_spc_chart | ID; CODE(M); NAME(M); ORG_ID(FK org=2,opt); CHART_TYPE(M, dict erp-qa/spc-chart-type: X_BAR_R/X_BAR_S/X_MR/P/NP/C/U); MATERIAL_ID(FK material,opt); INSPECTION_TYPE_ID(FK qa template,opt,留空); PARAMETER_ID(M BIGINT **占位软引用=0**，ORM 无 to-one 无目标实体，自由值); SPEC_MIN/SPEC_MAX(opt decimal); SUBGROUP_SIZE(opt int, default 5); SAMPLING_FREQUENCY(opt); CL_CENTER_TYPE(M, dict erp-qa/spc-cl-center-type: AUTO_FROM_DATA/MANUAL/TARGET, default AUTO_FROM_DATA); RULE_SET(opt, default "1,2,3,4"); ALARM_THRESHOLD(opt int, default 1); UCL/LCL/CL(opt decimal); CALC_STATUS(M, dict erp-qa/spc-calc-status: PENDING/CALCULATED/STALE, default PENDING); IS_ACTIVE(opt bool, default true); DOC_STATUS(M, dict erp-qa/doc-status: DRAFT/ACTIVE/CANCELLED); APPROVE_STATUS(M, dict wf/approve-status: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED); REMARK(opt) | 1 |
| erp_qa_spc_sample | ID; CHART_ID(FK chart,M); ORG_ID(FK org=2,opt); SUBGROUP_NO(M int); SAMPLE_TIME(M DATETIME `YYYY-MM-DD HH:mm:ss`); MEASURED_VALUES(opt); MEAN/RANGE/STD_DEV(opt decimal); SOURCE_BILL_TYPE/SOURCE_CODE/SOURCE_LINE_CODE(opt); INSPECTOR_ID(FK emp,opt); VIOLATED_RULES(opt); IS_OUT_OF_CONTROL(opt bool, default false → **本批置 true 驱动 outOfControlChartCount**); REMARK(opt) | 1 |
| erp_qa_spc_capability | ID; CHART_ID(FK chart,M); ORG_ID(FK org=2,opt); PERIOD_FROM(M DATE); PERIOD_TO(M DATE); SAMPLE_COUNT(opt int); TOTAL_OBSERVATIONS(opt int); GRAND_MEAN/OVERALL_STD_DEV/WITHIN_STD_DEV(opt decimal); CP/CPK/PP/PPK/CPM(opt decimal); CAPABILITY_LEVEL(opt, dict erp-qa/spc-capability: INADEQUATE/ACCEPTABLE/CAPABLE/EXCELLENT → **本批置 INADEQUATE 驱动 inadequateCapabilityCount**); IS_STABLE(opt bool); CALCULATED_BY(opt); CALCULATED_AT(opt DATETIME,留空); REMARK(opt) | 1 |
| erp_qa_non_conformance（追加） | ID; CODE(M, UK); NCR_DATE(M DATE); SOURCE_TYPE(opt → **追加行置 SPC** 驱动 openSpcNcrCount); SOURCE_CODE(opt); MATERIAL_ID(FK material,M); INSPECTION_ID(FK inspection,opt,留空); QUANTITY(opt decimal); DESCRIPTION(opt); SEVERITY(M, dict erp-qa/severity: LOW/NORMAL/HIGH/CRITICAL); DISPOSITION_TYPE(opt, dict erp-qa/disposition-type: SCRAP/RETURN/CONCESSION/DOWNGRADE); STATUS(M, dict erp-qa/ncr-status: OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED → **追加行置 OPEN**); SUPPLIER_ID(FK partner,opt); REMARK(opt); POSTED(opt bool, default false) | +1（ID=3） |

**字典码值（dict.yaml 已核实）**：
- `erp-qa/spc-chart-type`：X_BAR_R/X_BAR_S/X_MR/P/NP/C/U（dict.yaml 7 选项）
- `erp-qa/spc-capability`：INADEQUATE(Cpk<1.0)/ACCEPTABLE(1.0≤Cpk<1.33)/CAPABLE(1.33≤Cpk<1.67)/EXCELLENT(Cpk≥1.67) → `countInadequateCapabilityCharts` 过滤 `capabilityLevel=INADEQUATE`（常量 `SPC_CAPABILITY_INADEQUATE="INADEQUATE"`，`ErpQaConstants.java:149`）
- `erp-qa/spc-cl-center-type`：AUTO_FROM_DATA/MANUAL/TARGET（spc_chart.CL_CENTER_TYPE 默认 AUTO_FROM_DATA）
- `erp-qa/spc-calc-status`：PENDING/CALCULATED/STALE（spc_chart.CALC_STATUS 默认 PENDING）
- `erp-qa/doc-status`：DRAFT/ACTIVE/CANCELLED（spc_chart.DOC_STATUS 用 ACTIVE）
- `wf/approve-status`（平台共享）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → spc_chart.APPROVE_STATUS 用 APPROVED
- `erp-qa/ncr-status`：OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED → `countOpenSpcNcrs` 过滤 status IN [OPEN, IN_REVIEW]（常量 `NCR_STATUS_OPEN="OPEN"`/`NCR_STATUS_IN_REVIEW="IN_REVIEW"`，`ErpQaConstants.java:24-25`）
- NCR sourceType 自由文本列（ORM 无 ext:dict），SPC 来源值 = `"SPC"`（常量 `NCR_SOURCE_TYPE_SPC="SPC"`，`ErpQaConstants.java:160`）
- `erp-qa/severity`：LOW/NORMAL/HIGH/CRITICAL；`erp-qa/disposition-type`：SCRAP/RETURN/CONCESSION/DOWNGRADE

## 3. 范围 Decision（Phase 1 item 1.b）

### 3.1 (a) Strategy B vs C 裁决：**选择 Strategy C（完整参照完整性）**

**选择**：Strategy C —— seed spc_chart（1 行，parameterId=0 占位软引用）+ spc_sample（1 行 isOutOfControl=true，chartId 引用已 seed chart）+ spc_capability（1 行 capabilityLevel=INADEQUATE，chartId 引用同 chart）+ erp_qa_non_conformance 追加 1 行 sourceType=SPC/status=OPEN（驱动 openSpcNcrCount）。

**替代方案分析**：
- 「Strategy B（仅 sample+capability，不建 chart）」：**rejected**。理由：sample/capability.chartId 为 mandatory FK（ORM `<to-one name="chart">`），不 seed chart 则 chartId 悬空指向不存在的行。虽 `getSpcOutOfControlWarning` 三 helper 不 join chart（Nop `<to-one>` 为逻辑 join 非 DB 物理外键，加载时仅做对象导航），但种子数据完整性差（chartId 指向幽灵行）。既有集成测试 `TestErpQaDashboardSpc` 曾用 Strategy B 验证逻辑 FK 可被接受，但生产种子数据应取完整参照完整性。
- 「Strategy C」：**selected**。chart 行存在使 sample/capability.chartId 指向真实行（完整参照），与 0930-2 范式一致（域内 FK 全自洽）。代价仅为多 1 行 chart（极小），收益为种子数据完整性 + 未来 SPC 可视化 successor 可直接消费 chart 行。

### 3.2 (b) posted 裁决：三 SPC 表无 posted 列（N/A）；non_conformance 追加行 posted=false

镜像 0930-2 裁决。依据：
1. SPC 三表（spc_chart/spc_sample/spc_capability）ORM 均**无 `posted` 列**（逐表 ORM 核实），看板 `getSpcOutOfControlWarning` 读域表非 GL，`posted` 非任何过滤列；
2. non_conformance 有 `posted` 列，追加行 `posted=false`（镜像 0930-2 既有 2 行，NCR 过账 → GL 凭证 seed 归后续 Deferred）。

### 3.3 (c) 日期窗口 Decision

当前日期 2026-07-09。`getSpcOutOfControlWarning` **无日期参数**（三计数器全表扫描不过滤日期），故日期窗口仅为种子数据真实性与未来 SPC 可视化 successor 可读性：

- **spc_sample.SAMPLE_TIME**（mandatory DATETIME）：置当前月 `2026-07-08 10:00:00`（与既有 inspection 区间 2026-07-02/04/06 一致，落入质量看板默认区间 2026-07）。
- **spc_capability.PERIOD_FROM/PERIOD_TO**（mandatory DATE）：置当前月 `2026-07-01`/`2026-07-31`（覆盖本月分析周期）。
- **non_conformance.NCR_DATE**（追加行）：置 `2026-07-06`（落入 ncr-capa 报表 + 与既有 NCR 1/2 同月）。

### 3.4 (d) parameterId=0 占位软引用文档化

`spc_chart.parameterId` 是 mandatory BIGINT **但 ORM 无 `<to-one>` 无目标实体**（仓库无 `ErpQaParameter`/`ErpQaInspectionParameter`，检验参数仅以 `inspection_template_line.parameterName` 自由文本形式存在）。本批 `parameterId=0` 为占位软引用：
- 加载层：BIGINT 列接受任意值（无 FK 约束 / 无 dict 校验），0 安全。
- 看板层：`getSpcOutOfControlWarning` 三 helper 不读 chart 表（更不读 parameterId），0 不影响预警计数。
- 物化 `ErpQaParameter` 实体属 schema 扩展（ask-first），超出 seed 范畴 → 归 Deferred（触发条件：SPC 控制图需绑定真实检验参数维度时）。

### 3.5 (e) 期望值派生（每计数器标注 seed 行依据 + config 门控默认值确认）

`getSpcOutOfControlWarning` 三计数器 + 两 config 门控默认值（经 `ErpQaConfigs.isDashQaSpcIncludeInadequate`/`isDashQaSpcIncludeNcr` 核实均默认 `true`）：

| 字段 | 期望值 | 派生依据 | config 门控 |
|------|--------|---------|------------|
| outOfControlChartCount | **1** | `countOutOfControlCharts`：读 `erp_qa_spc_sample` filter `isOutOfControl=true` → distinct `chartId`；seed 1 行（id=1, chartId=1, isOutOfControl=true）→ distinct chartId 集合 = {1} → size=1 | 无门控（恒计入） |
| inadequateCapabilityCount | **1** | `countInadequateCapabilityCharts`：读 `erp_qa_spc_capability` filter `capabilityLevel='INADEQUATE'` → distinct `chartId`；seed 1 行（id=1, chartId=1, capabilityLevel=INADEQUATE）→ distinct chartId 集合 = {1} → size=1 | `erp-dash.qa-spc-include-inadequate` 默认 `true` → 计入（门控关则返 0） |
| openSpcNcrCount | **1** | `countOpenSpcNcrs`：读 `erp_qa_non_conformance` filter `sourceType='SPC'` AND `status IN ('OPEN','IN_REVIEW')` → 行数；追加 1 行（id=3, sourceType=SPC, status=OPEN）→ 1 | `erp-dash.qa-spc-include-ncr` 默认 `true` → 计入（门控关则返 0） |

**副作用 — getDashboardKpi.openNcrCount 联动变更**（关键，须同步更新断言）：
`getDashboardKpi.openNcrCount` 经 `countOpenNcrs` 计数**所有** NCR status IN [OPEN, IN_REVIEW]（**不按 sourceType 过滤**，`ErpQaDashboardBizModel.java:265-272`）。追加 NCR id=3 status=OPEN → openNcrCount 由 **2 → 3**（NCR 1 OPEN + NCR 2 IN_REVIEW + NCR 3 OPEN）。须同步更新 `tests/e2e/dashboards/quality.value.spec.ts` 的 `getDashboardKpi` 断言 `openNcrCount: 2 → 3`。

其余 getDashboardKpi 字段不受影响：inspectionCount/passRate/rejectedCount 仅读 `erp_qa_inspection`（本批不 seed inspection，3 行不变）。

## 4. 记录设计（引用 1234-1/0930-2 已 seed 主数据固定 ID）

通用：orgId=2(ERP-CO)、material 1(产品甲/PCS)、employee 1(张三)。

**SPC 链**：
- **spc_chart**（1 行 ACTIVE/APPROVED）：
  - 1：`SPC-CHART-001` 产品甲关键尺寸 X̄-R 控制图，orgId=2，chartType=X_BAR_R，materialId=1(产品甲)，parameterId=0（占位软引用，文档化见 §3.4），clCenterType=AUTO_FROM_DATA（默认），calcStatus=PENDING（默认），isActive=true（默认），docStatus=ACTIVE，approveStatus=APPROVED，remark=占位说明。
- **spc_sample**（1 行 isOutOfControl=true 驱动 outOfControlChartCount）：
  - 1：chartId=1，orgId=2，subgroupNo=1，sampleTime=2026-07-08 10:00:00，inspectorId=1(张三)，isOutOfControl=true，violatedRules=1，remark=失控说明。
- **spc_capability**（1 行 INADEQUATE 驱动 inadequateCapabilityCount）：
  - 1：chartId=1，orgId=2，periodFrom=2026-07-01，periodTo=2026-07-31，capabilityLevel=INADEQUATE，isStable=false，remark=能力不足说明。

**non_conformance 加性追加**（1 行 SPC+OPEN 驱动 openSpcNcrCount）：
- 3：`NCR-2026-003` SPC 失控 NCR，ncrDate=2026-07-06，sourceType=SPC，sourceCode=SPC-CHART-001，materialId=1，inspectionId 留空（SPC 来源非 inspection），quantity=10，description=SPC 失控点说明，severity=HIGH，dispositionType=SCRAP，status=OPEN，posted=false。

## 5. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有 SPC 种子经 CSV INSERT 表达，无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-qa-spc-*.sql`。

## 6. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| quality SPC（控制图+样本+能力） | 3（spc_chart, spc_sample, spc_capability） | 1+1+1 = 3 |
| quality 既有加性追加 | 1（non_conformance +1 行） | +1 |
| **合计** | **3 张新 CSV + 1 处加性追加** | **4 行** |

> 3 张新 SPC CSV + 1 处加性追加加入 `_vfs/_init-data/`，与前序 84 张 CSV 共存（总计 84 + 3 = **87 张 CSV**；non_conformance 追加不增表数）。

## 7. 残留风险与防护

- 参照完整性遗漏（FK 列引用未 seed 的上游 ID）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配（尤其 mandatory DATETIME `SAMPLE_TIME`）→ 同上启动期暴露。
- **datetime 格式**（spc_sample.SAMPLE_TIME mandatory DATETIME）：必须 `YYYY-MM-DD HH:mm:ss`（与 0930-2 downtime_entry.START_TIME 同机制）；格式错配会在启动期抛 NopException。
- 非幂等（前序 6 批已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。
- 字典码值错配（如 spc-chart-type/spc-capability/spc-cl-center-type/spc-calc-status/doc-status 非法值）→ 同上启动期暴露（dict 校验在 entity 校验阶段）。
- **SPC 引擎重算覆盖风险**：SPC 引擎双层门控（`erp-qa.spc-enabled` 默认 false + `ErpQaSpcSamplingJob`/`ErpQaSpcCapabilityJob` cron 默认空），fresh-DB 启动不触发重算，seed 静态结果行（isOutOfControl/capabilityLevel）安全不被覆盖。重算链端到端回归属 Deferred。
- **getDashboardKpi.openNcrCount 联动**：追加 SPC NCR 使 openNcrCount 由 2→3，须同步更新 `quality.value.spec.ts` 断言（§3.5 已标注），否则该 spec 回归失败。
