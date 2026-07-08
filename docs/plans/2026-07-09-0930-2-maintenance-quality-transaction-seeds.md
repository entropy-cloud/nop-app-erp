# 2026-07-09-0930-2-maintenance-quality-transaction-seeds 维护+质量域交易单据种子数据

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md` Deferred「其他扩展域交易种子（...maintenance/quality/...）」（Successor Required: yes，触发条件「当对应扩展域看板/报表端到端数值回归需交易数据时，按域逐批补 seed」——已满足：维护/质量域看板 `ErpMntDashboardBizModel`/`ErpQaDashboardBizModel` + 4 报表已就绪但缺数据）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md`（completed，运营域种子范式 + 本计划数据层前置）、`docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md`（同批 N=1，独立可并行）、`docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md`（同批 N=3，本计划与 N=1 的断言层后继）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **既有种子库（57 CSV）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 21 主数据（1234-1）+ 23 P2P/O2C（1445-1）+ 13 运营域（2210-1：库存/资产/项目）。维护/质量域零 CSV。
- **维护域看板读域表非 GL**（`module-maintenance/erp-mnt-service/.../dashboard/ErpMntDashboardBizModel.java:58-81`）：`getDashboardKpi(startDate,endDate,context)` `@BizQuery` 查 `erp_mnt_equipment`(equipmentTotal=非 DECOMMISSIONED 计数/runningCount=RUNNING 计数)+`erp_mnt_request`(openRequestCount=OPEN)+`erp_mnt_visit`(periodVisitCount=COMPLETED & businessDate∈区间)。其余 `@BizQuery`：`getEquipmentStatusDistribution`(:84 读 equipment)、`findEquipmentDowntimeAlert`(:108 读 equipment+downtime_entry endTime=null)、`findMaintenanceOverdueAlert`(:135 读 schedule+visit)。**零 GL/Voucher 引用**。
- **维护域报表读域表非 GL**（`ErpMntReportBizModel.java`，VFS 根 `/nop/main/report/mnt/`）：2 张 `.xpt.xml`（maintenance-history / downtime-summary）。`buildMaintenanceHistoryDataset`(:213) 读 visit(+visit_task taskCount +spare_part_usage usageCount+equipment 名)；`buildDowntimeSummaryDataset`(:249) 读 downtime_entry(+equipment 名)。
- **质量域看板读域表非 GL**（`module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java:65-94`）：`getDashboardKpi(startDate,endDate,context)` `@BizQuery` 查 `erp_qa_inspection`(inspectionCount=inspectionDate∈区间/passRate=ACCEPTED 占比/rejectedCount=REJECTED)+`erp_qa_non_conformance`(openNcrCount=status IN [OPEN,IN_REVIEW])。其余 `@BizQuery`：`getDashboardTrend`(:97 inspection)、`findDefectTopN`(:132 non_conformance)、`findCapaOverdueAlert`(:162 action)、`getSpcOutOfControlWarning`(:204 spc_sample+spc_capability+non_conformance，config-gated)。**零 GL/Voucher 引用**。
- **质量域报表读域表非 GL**（`ErpQaReportBizModel.java`，VFS 根 `/nop/main/report/qa/`）：2 张 `.xpt.xml`（inspection-summary / ncr-capa-summary）。`buildInspectionSummaryDataset`(:213) 读 inspection(+master-data material 名)；`buildNcrCapaSummaryDataset`(:260) 读 non_conformance(ncrDate∈区间)+action(capaActionCount/completedActionCount)。
- **维护/质量域看板/报表当前数值为 0/空**：两域零 seed → 看板 KPI 全 0、4 报表空集（2210-1 Deferred 明示「扩展域交易种子未 seed，数值仍空」）。
- **维护域最小 seed 表集（经数据源反推）**：`erp_mnt_equipment`（看板 equipmentTotal/runningCount + 报表 equipment 名，域内主数据但直接参与 KPI 必须 seed）+`erp_mnt_equipment_category`(equipment.categoryId FK 上游)+`erp_mnt_request`(看板 openRequestCount)+`erp_mnt_visit`(+`erp_mnt_visit_task` maintenance-history 报表 taskCount，看板 periodVisitCount，PRIMARY)+`erp_mnt_spare_part_usage`(maintenance-history 报表 usageCount)+`erp_mnt_downtime_entry`(看板 downtime 预警 + downtime-summary 报表)+`erp_mnt_schedule`(看板 maintenance-overdue 预警)。
- **质量域最小 seed 表集（经数据源反推）**：`erp_qa_inspection`(看板所有 KPI/trend + inspection-summary 报表，PRIMARY)+`erp_qa_non_conformance`(看板 openNcrCount/defect + ncr-capa-summary 报表)+`erp_qa_action`(看板 CAPA 预警 + ncr-capa 报表)。SPC 三表（spc_sample/spc_capability/spc_chart）可选——`getSpcOutOfControlWarning` config-gated 默认开，seed spc_sample(isOutOfControl=true)+spc_capability(capabilityLevel=INADEQUATE) 可令 SPC 预警非空（属增强，非核心阻塞）。
- **种子范式已建立（2210-1 交付）**：列名=ORM 列 `code`/`ID` 显式/省略审计+TENANT_ID/布尔小写/日期 `YYYY-MM-DD`/posted 统一 false（看板读域表非 GL）/拓扑序自动排序/域内金额自洽/UoM 列名陷阱 `UO_M_ID`（适用 mnt spare_part_usage_line；quality 用自由文本 `UNIT` 无 UoM FK）。落盘 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。
- **可复用固定主数据 ID**（1234-1/2210-1 已 seed）：orgId=2/material IDs 1-4/partner ID 3（供应商）/employee IDs 1-3/warehouse 1-2/location IDs 1-2/asset IDs 1-3（mnt equipment.assetId 可复用 AST-2026-002 数控机床，跨域可选）。
- **维护/质量域主交易头已有标准字段**（ORM）：`ErpMntVisit`(L256) 含 `ORG_ID`(L273)+`BUSINESS_DATE`(L274)+`POSTED`(L275，标准字段补齐 retrofit 已落地)；`ErpQaInspection`(L174) 含 `ORG_ID`(L178)+`BUSINESS_DATE`(L188)+`POSTED`(L196) 全标准字段（**不在** AGENTS.md「7 域 ask-first blocked」集合内）。`ErpQaNonConformance` 含 `POSTED`(L384)。
- **保护区域**：纯部署期数据（CSV）+ 分析文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 2210-1）。属 `plan-first`（跨域 FK + 拓扑序 + posted 裁决 + 跨两域 >5 文件）。

剩余差距：(1) 维护域看板 KPI 为 0/空（缺 equipment/request/visit seed）；(2) 质量域看板 KPI 为 0/空（缺 inspection/non_conformance seed）；(3) 维护/质量 4 报表为空集；(4) 两域 seed 表映射分析文档未派生。

## Goals

- 在 57 CSV 基础上新增维护域最小连通种子集（equipment+category/request/visit+visit_task/spare_part_usage/downtime_entry/schedule）+ 质量域最小连通种子集（inspection/non_conformance/action，可选 inspection_line/SPC 三表），使维护域看板 `getDashboardKpi` + 2 报表、质量域看板 `getDashboardKpi` + 2 报表**数值转非空可观测**（Closure Gate 验证 getDashboardKpi + 报表非空；维护 3 预警/质量 SPC 预警 `@BizQuery` 经附条件 seed 尽力非空——见 Phase 2 预警种子条件；SPC 预警因 SPC 三表可选，未 seed 时 `getSpcOutOfControlWarning` 返回 outOfControlChartCount=0 属预期）。
- seed 保持 `posted=false`（看板/报表读域表非 GL，镜像 2210-1 裁决），引用 1234-1/2210-1 固定主数据 ID，域内金额自洽，列名严格对齐 ORM `code`（含 mnt `UO_M_ID` 陷阱；quality 无 UoM FK）。
- 落盘维护+质量域 seed 表映射分析文档（表清单 + 列角色 M/FK/opt + 拓扑序 + 范围裁决 + 域内金额自洽约束）。
- 解除 2210-1 Deferred「其他扩展域交易种子（maintenance/quality）」。

## Non-Goals

- **不** seed GL 凭证/业财一体——维护/质量域看板/报表读域表非 GL，1234-1 科目表无维护/质量域专用科目；触发条件：维护/质量域业财一体端到端数值回归需 GL 串联时。
- **不** seed 维护域 calibration（校准，不被看板/报表读）/质量域 risk_register/quality_goal/review/calibration/recall(+recall_target)/sampling_plan/inspection_template(+line)——这些表不被看板/报表 `QueryBean` 直接读（inspection.templateId 非强制可留 null）；按需 successor。
- **不**做精确 KPI/报表数值断言——本计划解除「数据存在」阻塞；精确断言是 N=3 `2026-07-09-0930-3` 后继层。
- **不** seed 其他扩展域（manufacturing → 同批 N=1；CRM/CS/HR/logistics/b2b/contract/drp/aps → 后续批次）。
- **不**改后端 `@BizQuery`/报表模板/ORM——纯部署数据层（镜像 2210-1）。

## Task Route

- Type: `implementation-only change`（纯部署期种子 CSV + 分析文档，零生产代码变更）
- Owner Docs: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/design/dashboards.md` §8（维护）/§9（质量）看板 KPI 口径、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（2210-1 范式）
- Skill Selection Basis: `none`——可用技能集均不覆盖部署期 CSV 种子编写（数据建模 + 列映射 + FK 拓扑，非后端逻辑/前端/Java 测试）。镜像 2210-1 范式。
- Bundling 裁决（规则 14）：维护 + 质量分属不同 module/不同 owner-doc 章节（§8 vs §9），严格说不满足「同一 owner doc」；但二者 (a) 共享完全相同的种子范式（镜像 2210-1 域表直 seed）、(b) 共享同一验证路径（fresh-DB + GraphQL 非空 + E2E 0 回归）、(c) 共享同一后继 N=3 断言层、(d) 各自种子集很小。拆分会产生两个近乎复制的微计划，正是规则 14 欲避免的 clutter。结果表面类型一致（交易种子解除看板/报表空集阻塞），故合并为一个 plan。

## Infrastructure And Config Prereqs

- 既有 57 CSV 种子库已落地（维护/质量域可读取）——前置已满足。
- 平台 `DataInitInitializer` + `-Dnop.orm.init-database-data=true` + fresh-DB 重置——无需改。
- 回滚策略：纯新增 CSV + 分析文档，删除即回滚。

## Execution Plan

### Phase 1 - 维护+质量域 seed 表映射 + 范围裁决（Proof + Decision）

Status: completed
Targets: `module-maintenance/model/app-erp-maintenance.orm.xml`、`module-quality/model/app-erp-quality.orm.xml`、`docs/design/dashboards.md` §8/§9、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`
Skill: `none`

- Item Types: `Proof | Decision`
- Prereqs: 既有 57 CSV 种子库（维护/质量域可读取）

- [x] `Proof`：逐表派生列映射——读维护域（equipment/equipment_category/request/visit/visit_task/spare_part_usage/downtime_entry/schedule）+ 质量域（inspection/non_conformance/action，可选 inspection_line/spc_sample/spc_capability/spc_chart）ORM，标注每列角色（M/FK/opt），核实 mandatory 业务列可填、FK 指向已 seed 主数据（org=2/material 1-4/partner 3/employee 1-3/warehouse 1-2/location 1-2；mnt equipment.assetId 可复用 asset ID 2 可选），UoM 列名陷阱核实（mnt spare_part_usage_line=`UO_M_ID`；quality inspection_line=`UNIT` 自由文本无 FK）。核实 dict code（mnt: visit-status/request-status/equipment-status/schedule-type/visit-type/visit-result；qa: inspection-type/inspection-result/doc-status/ncr-status/disposition-type/severity；shared: wf/approve-status）对齐 dict.yaml。产出 seed 表映射分析文档（`docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`）。
      - Skill: `none`
- [x] `Decision`：seed 范围与 posted 裁决——(a) 范围 Decision：维护 7 表（equipment+category/request/visit+visit_task/spare_part_usage/downtime_entry/schedule）+ 质量 3 表（inspection/non_conformance/action）为最小集；SPC 三表为可选增强（考虑替代方案「含 SPC」vs「仅核心」+ 残留风险：缺 SPC 致 getSpcOutOfControlWarning 返回 outOfControlChartCount=0，属预期非缺陷，可在本计划或 N=3 补）。(b) posted Decision：统一 `posted=false`（镜像 2210-1，看板/报表读域表非 GL，约束记录）。(c) 日期窗口 Decision：visit.businessDate + **visit.visitDate（maintenance-history 报表过滤列，须落入报表查询区间）** + **downtime_entry.startTime（downtime-summary 报表过滤列，须落入区间）** + inspection.inspectionDate + non_conformance.ncrDate 置于当前月与历史月使本期 KPI + trend + 报表非空。(d) equipment.assetId 跨域复用 Decision：复用 asset ID 2（AST-2026-002 数控机床）或留 null（可选非强制）。本项记录每表关键日期/状态/dict/跨域 ID 选择依据。
      - Skill: `none`

Exit Criteria:

- [x] 维护+质量域 seed 表映射分析文档落盘，每表标注列角色 M/FK/opt + FK 目标 ID + dict code 核实 + 拓扑序 + 范围/posted/日期/跨域 Decision + 残留风险。

### Phase 2 - 维护+质量域 seed CSV 编写（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{mnt,qa}_*.csv`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1 表映射 + 裁决

- [x] `Add`：编写维护域 CSV（equipment_category ≥1 + equipment ≥2 覆盖 RUNNING/其他状态驱动 equipmentTotal/runningCount + **1 条 equipment status=DOWN + 其 downtime_entry endTime=null（驱动 findEquipmentDowntimeAlert）** + request ≥1 OPEN + visit ≥2 含 COMPLETED 驱动 periodVisitCount（visit.visitDate 落入 maintenance-history 报表区间）+ visit_task ≥1 + spare_part_usage ≥1 + downtime_entry ≥1（startTime 落入 downtime-summary 报表区间）+ **schedule ≥1 isActive=true + nextDueDate<today + 无关联 visit（驱动 findMaintenanceOverdueAlert）**）；质量域 CSV（inspection ≥3 覆盖 ACCEPTED/REJECTED 驱动 passRate/rejectedCount + non_conformance ≥2 含 OPEN/IN_REVIEW 驱动 openNcrCount + **action ≥1 status!=COMPLETED + dueDate<today（驱动 findCapaOverdueAlert）**；可选 SPC 三表，未 seed 则 getSpcOutOfControlWarning 返回 0 属预期）。列名严格对齐 ORM `code`（`ID` 显式、mnt `UO_M_ID`、qa 无 UoM FK、省略审计+TENANT_ID、布尔小写、posted 统一 false），FK 引用 1234-1/2210-1 固定 ID，域内金额/计数自洽。
      - Skill: `none`

Exit Criteria:

- [x] 维护+质量域 CSV 文件落地 `_vfs/_init-data/`，列名经脚本逐表对齐 ORM `code`（0 错配，含 mnt `UO_M_ID` 陷阱），mandatory 业务列全填，FK 全指向已 seed ID。

### Phase 3 - fresh-DB seed 加载 + GraphQL 非空验证 + E2E 0 回归（Proof）

Status: completed
Targets: fresh-DB 启动、`/graphql` 抽样
Skill: `none`

- Item Types: `Proof`
- Prereqs: Phase 2 CSV 落地

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块，确认新 CSV 打包入 runner jar 无后端污染）+ fresh-DB 启动（删 `db/erp.mv.db` + `-Dnop.orm.init-database-data=true`）确认 57+N CSV 全 `load-csv-data` 成功（0 主键冲突 / 0 列映射错误 / 0 参照完整性失败）+ GraphQL 抽样 `ErpMntDashboard__getDashboardKpi`/`ErpQaDashboard__getDashboardKpi`（传 startDate/endDate 覆盖种子区间）KPI 由 0 转非空 + maintenance-history/downtime-summary/inspection-summary/ncr-capa-summary `Erp{Mnt,Qa}Report__renderHtml` 返回非空 + `npx playwright test`（既有 spec 0 回归）。指定成功/失败模式（同 N=1）。
      - Skill: `none`

Exit Criteria:

- [x] fresh-DB seed 加载 0 冲突 + GraphQL 抽样维护/质量域看板 KPI + 4 报表由 0/空转非空可观测 + 既有 E2E spec 0 回归。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 3 全绿

- [x] `Add`：`docs/architecture/seed-data.md` 增「维护+质量域交易单据种子」段 + Non-Goals 更新移除 maintenance/quality + `docs/testing/e2e-runbook.md` 种子库 CSV 计数 + 域清单补维护/质量域 + `docs/testing/known-good-baselines.md` 增维护/质量域种子基线行；2210-1 Deferred「其他扩展域交易种子（maintenance/quality）」登记解除（本计划 Closure 段登记）。
      - Skill: `none`

Exit Criteria:

- [x] seed-data.md 含维护+质量域种子段；e2e-runbook + known-good-baselines 种子库计数/域同步；2210-1 Deferred maintenance/quality 子集登记解除。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bd79d9b1ffeBiDhxScFXDLYx2`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线硬事实（行号/KPI 口径/ORM 字段/dict 枚举/CSV/ID/零 GL/跨计划依赖）逐项 live 验真。无 BLOCKER。2 MAJOR：(1) dashboards.md 章节号错位（§7/§8 应为 §8 维护/§9 质量）→ **已修复**；(2) Goal「全部 @BizQuery 非空」与 Phase 3 仅抽样 getDashboardKpi+报表不一致（3 预警触发条件未给）→ **已修复**（Goal 收窄为 getDashboardKpi+报表非空 + 预警尽力，Phase 2 补 3 预警种子条件：DOWN equipment+endTime=null downtime、isActive+nextDueDate<today+无 visit schedule、status!=COMPLETED+dueDate<today action）。4 MINOR：Phase 1 Decision (c) 补 visit.visitDate/downtime_entry.startTime 报表过滤列对齐 → **已采纳**；SPC getSpcOutOfControlWarning 允许返回 0 说明 → **已采纳**（Goal）；rule 14 bundling 裁决 → **已采纳**（Task Route）；UO_M_ID 陷阱说明保留作预防性记录。Bundling 裁决：keep bundled（同范式+同验证+同后继，拆分致 clutter）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。本计划结果表面为部署期数据（CSV），无生产 Java 代码变更；验证门控以 fresh-DB seed 加载 + 既有 E2E 0 回归 + GraphQL 非空一致为主。

- [x] 范围内行为完成（维护 7 表 + 质量 3 表种子 CSV 落地 + 看板/报表数值非空）
- [x] 相关文档对齐（seed-data.md + e2e-runbook + known-good-baselines）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL 抽样 KPI 非空 + `npx playwright test`（既有 spec 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（GL 凭证 seed/未读配置表/SPC 可选/其他扩展域/精确数值断言均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 维护+质量域 GL 凭证/业财一体 seed

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 两域看板/报表读域表（equipment/visit/inspection/non_conformance 等）非 GL；1234-1 种子科目表无维护/质量域专用科目。seed GL 凭证不解除额外阻塞。
- Successor Required: `yes`
- Trigger Condition: 当维护/质量域业财一体端到端数值回归需 GL 串联时。

### 维护域 calibration / 质量域 risk_register/quality_goal/review/calibration/recall/sampling_plan/inspection_template seed

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些表不被看板/报表 `QueryBean` 直接读；inspection.templateId 非强制可留 null。seed 它们不解除看板/报表数值阻塞。
- Successor Required: `yes`
- Trigger Condition: 当对应域配置/执行链端到端回归需这些数据，或 inspection 需引用真实 template 时。

### 质量域 SPC 三表 seed（spc_sample/spc_capability/spc_chart）

- Classification: `optimization candidate`
- Why Not Blocking Closure: `getSpcOutOfControlWarning` config-gated 默认开但 SPC 预警非核心看板 KPI；不 seed 则该预警返回 0，核心 KPI（inspection/passRate/openNcrCount）仍非空。
- Successor Required: `yes`
- Trigger Condition: 本计划 Phase 1 Decision 若选「仅核心」则归此；当需 SPC 预警非空观测时补 seed（本计划或 N=3 均可承接）。

### 精确维护/质量域 KPI/报表数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「数据存在」阻塞；精确断言是 N=3 successor 层。
- Successor Required: `yes`
- Trigger Condition: 本计划固化后，由 `2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md` 承接。

## Closure

Status Note: 计划完成。维护+质量域 11 表 18 行最小连通种子集落地（维护 8 表 + 质量 3 表），使维护域看板 `getDashboardKpi` + 2 报表 + 2 预警、质量域看板 `getDashboardKpi` + 2 报表 + 1 预警数值转非空可观测。验证全绿（build 154 模块 + fresh-DB 72 CSV 0 冲突 + 66 E2E spec 0 回归 + GraphQL KPI/报表非空）。SPC 三表、备件消耗行、GL 凭证/业财一体、精确数值断言归 Non-Goal Deferred。零 ORM/xbiz/page/view/Java 生产代码变更（纯部署期 CSV + 文档）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bd1d4aefffe4evODIsjSGOziN`（general，新会话冷重播无执行者上下文，2026-07-09）— VERDICT: **PASS**（0 BLOCKER / 0 MAJOR / 2 MINOR 非阻塞）。
  - Task A（CSV 列名对齐，CRITICAL）：11/11 CSV 对齐 ORM `code`，0 列名错配 / 0 字段计数错配 / 0 mandatory 业务列违规；POSTED=false 小写确认（4 表）；downtime_entry.START_TIME 格式 `YYYY-MM-dd HH:mm:ss` 确认；30/30 dict 码值合法。
  - Task B（保护区域）：git status 仅 5 docs + 11 CSV + analysis + 2 plan 文件变更，**零** orm.xml/xbiz.xml/page.yaml/view.xml/.java/.beans.xml/sql-lib.xml/.xpt.xml 变更（镜像 2210-1）。
  - Task C（FK 参照完整性）：46 非空 FK 单元检查，0 悬挂（上游 org=2/material 1-4/warehouse 1-2/employee 1-3/partner SUP-001=3/asset AST-2026-002=2 + 域内 FK 全解析）。
  - Task D（计划一致性）：0 未勾 `[ ]`；4/4 Phase `Status: completed`；8/8 Closure Gates `[x]`；`Plan Status: completed`。
  - Task E（文档同步）：seed-data.md 新增「维护+质量域」段 + 3 Non-Goals 更新；e2e-runbook CSV 计数 72 + 表清单；known-good-baselines 新基线行；README 新 ✅ done 0930-2 行；2210-1 Deferred `Resolved (subset)` 登记。21+23+13+4+11=72 一致。
  - Task F（可选运行时）：未运行（静态检查 A-C 充分；db/erp.mv.db 存在证实 Phase 3 fresh-DB load 执行；known-good-baselines KPI 值与 CSV 内容+当日日期精确自洽，不可伪造）。
- MINOR（非阻塞）：(1) 已补每日日志 `docs/logs/2026/07-09.md`；(2) wf/approve-status 平台运行时 dict（classpath 解析，APPROVED 与 57-CSV 基线一致）。

Follow-up:

- SPC 三表 seed（spc_chart/spc_sample/spc_capability）——spc_chart.parameterId 配置链依赖；触发：检验参数实体/template_line 物化为 parameterId 后。
- 精确维护/质量域 KPI/报表数值断言——`2026-07-09-0930-3` successor 承接。
- 备件消耗行 `erp_mnt_spare_part_usage_line` seed——触发：备件消耗明细端到端回归（注意 UoM 列名 `UO_M_ID`）。
- 维护/质量域 GL 凭证/业财一体 seed——触发：两域业财一体端到端数值回归需 GL 串联时。
