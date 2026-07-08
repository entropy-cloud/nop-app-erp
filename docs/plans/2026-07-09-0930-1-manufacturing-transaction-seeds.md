# 2026-07-09-0930-1-manufacturing-transaction-seeds 制造域交易单据种子数据

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md` Deferred「其他扩展域交易种子（manufacturing/quality/maintenance/...）」（Successor Required: yes，触发条件「当对应扩展域看板/报表端到端数值回归需交易数据时，按域逐批补 seed」——已满足：制造域看板 `ErpMfgDashboardBizModel` + 3 报表已就绪但缺数据，2210-1 显式标注「manufacturing 因 BOM/routing/work-order 链复杂度单独 successor」）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md`（completed，运营域种子范式 + 本计划数据层前置）、`docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md`（同批 N=2，独立可并行）、`docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md`（同批 N=3，本计划与 N=2 的断言层后继）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **既有种子库（57 CSV）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 21 主数据（1234-1）+ 23 P2P/O2C 交易（1445-1）+ 13 运营域（2210-1：库存/资产/项目）。制造域零 CSV。
- **制造域看板读域表非 GL**（`module-manufacturing/erp-mfg-service/.../dashboard/ErpMfgDashboardBizModel.java:52-77`）：`getDashboardKpi(startDate,endDate,context)` `@BizQuery` 仅查 `erp_mfg_work_order`——`inProcessCount`=count(docStatus IN [IN_PROCESS,STOCK_RESERVED])、`periodCompletedQty`=Σ completedQuantity(docStatus=COMPLETED AND actualEndDate∈区间)、`stockPartialCount`=count(docStatus=STOCK_PARTIAL)、`onTimeRate`=count(COMPLETED & actualEndDate≤plannedEndDate)/count(COMPLETED)。其余 `@BizQuery`：`getWorkOrderStatusDistribution`(:80)、`getDashboardTrend`(:104)、`findDelayedWorkOrderAlert`(:133) 均查 work_order。**零 GL/Voucher 引用**。
- **制造域报表读域表非 GL**（`ErpMfgReportBizModel.java`，VFS 根 `/nop/main/report/mfg/`）：3 张 `.xpt.xml` 模板（crp-load-report / production-variance-report / forecast-variance-report）。数据源：`buildProductionVarianceDataset`(:279) 读 `erp_mfg_cost_variance`（workOrderCode 经 work_order 关系，cost_variance.workOrderId mandatory FK→work_order）；`buildForecastVarianceDataset`(:304) 读 `erp_mfg_forecast`(APPROVED)+`erp_mfg_forecast_line`+`erp_mfg_work_order`(COMPLETED 实际量，按 productId 聚合、区间重叠用 plannedStartDate/plannedEndDate)；`buildCrpLoadDataset`(:237) 读 `erp_mfg_crp_load`（**mandatory workcenterId FK→ErpMfgWorkcenter**，orm:546）并委托 `IErpMfgCrpLoadBiz.getLoadReport`→`CrpLoadCalculator` 内部 resolveReportWorkcenters 取 workcenterCode + 从 WorkcenterCalendar/WorkcenterCapacity 算 capacityHours——**crp-load 报表依赖 workcenter 配置链（workcenter/calendar/capacity 未 seed），故 crp_load + crp-load-report 归本计划 Deferred（触发条件：workcenter 配置链 seed 落地后）**。
- **制造域看板/报表当前数值为 0/空**：制造域零 seed → 看板 KPI 全 0、production-variance/forecast-variance 报表空集（crp-load 报表归 Deferred）（2210-1 Deferred 明示「扩展域交易种子未 seed，数值仍空」）。
- **制造域最小 seed 表集（经数据源反推，crp_load 已因配置链依赖移出范围）**：`erp_mfg_work_order`（看板全部 KPI + forecast-variance 报表实际量，PRIMARY）+ `erp_mfg_cost_variance`（production-variance 报表，workOrderId mandatory FK→work_order 在范围内可填）+ `erp_mfg_forecast`+`erp_mfg_forecast_line`（forecast-variance 报表）。`erp_mfg_work_order_line` 为可选子表（看板/报表不直接读行，弱关联可留 null）。
- **种子范式已建立（2210-1 交付）**：列名=ORM 列 `code`（UPPER_SNAKE）/`ID` 显式提供（FK 跨表）/省略审计列(CREATED_BY/CREATE_TIME/UPDATED_BY/UPDATE_TIME/DEL_VERSION/VERSION)+TENANT_ID/布尔小写 `true`/`false`/日期 `YYYY-MM-DD`/posted 统一 false（看板读域表非 GL，科目表无运营域科目）/加载拓扑序经 ORM `getEntityModelsInTopoOrder()` 自动排序/域内金额自洽/UoM 列名陷阱 `UO_M_ID`（非 `UOM_ID`）。列映射/拓扑序/范围裁决落盘 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。
- **可复用固定主数据 ID**（1234-1 已 seed）：orgId=2（ERP-CO COMPANY）/currencyId=1（CNY 功能币）/material IDs 1-4（MAT-001~004）/uom IDs 1-4（PCS/KG/M/BOX）/warehouse IDs 1-2（WH-MAIN/WH-RAW）/partner ID 3（SUP-001 供应商）/employee IDs 1-3。
- **制造域主交易头已有标准字段**（`module-manufacturing/model/app-erp-manufacturing.orm.xml`）：`ErpMfgWorkOrder`(`<entity` L560) 含 `ORG_ID`(L566)+`BUSINESS_DATE`(L577)+`POSTED`(L594) 全标准字段（**不在** AGENTS.md「7 域 posted/businessDate ask-first blocked」集合内）。`ErpMfgCostVariance`(`<entity` L1394) 含 `POSTED`(L1415) + `WORKORDER_ID` mandatory FK→work_order。
- **保护区域**：纯部署期数据（CSV）+ 分析文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 2210-1）。属 `plan-first`（跨域 FK + 拓扑序 + posted 裁决 + crp_load 配置链依赖裁决需严谨 + 涉及 >5 文件）。

剩余差距：(1) 制造域看板 KPI 为 0/空（缺 work_order seed）；(2) 制造域 production-variance/forecast-variance 报表为空集（缺 cost_variance/forecast seed；crp-load 报表归 Deferred）；(3) 制造域 seed 表映射分析文档未派生。

## Goals

- 在 57 CSV 基础上新增制造域最小连通种子集（work_order + cost_variance + forecast + forecast_line，可选 work_order_line），使制造域看板 4 `@BizQuery`（getDashboardKpi/StatusDistribution/Trend/DelayedAlert，均查 work_order）+ 2 报表（production-variance / forecast-variance）**数值转非空可观测**。
- seed 保持 `posted=false`（看板/报表读域表非 GL，镜像 2210-1 裁决），引用 1234-1 固定主数据 ID，域内金额自洽，列名严格对齐 ORM `code`（含 `UO_M_ID` 陷阱）。
- 落盘制造域 seed 表映射分析文档（表清单 + 列角色 M/FK/opt + 拓扑序 + 范围裁决 + 域内金额自洽约束）。
- 解除 2210-1 Deferred「其他扩展域交易种子（manufacturing）」。

## Non-Goals

- **不** seed GL 凭证/业财一体（库存估值/制造费用/差异过账）——制造域看板/报表读域表非 GL，1234-1 科目表无制造域专用科目；触发条件：制造域业财一体端到端数值回归需 GL 串联时。
- **不** seed crp_load + crp-load 报表——`erp_mfg_crp_load.workcenterId` 是 mandatory FK→ErpMfgWorkcenter，且 crp-load 报表内部经 `CrpLoadCalculator` 依赖 workcenter/workcenter_calendar/workcenter_capacity 配置链（均未 seed）算 capacityHours/loadRate；seed crp_load 需先 seed 整条 workcenter 配置链，超出「域表直 seed」范式。触发条件：workcenter 配置链 seed 落地后，由独立 successor 或本计划扩展承接 crp_load + crp-load 报表。
- **不** seed BOM/Routing/Workcenter(+Calendar/Capacity)/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy——这些表不被范围内看板/报表 `QueryBean` 直接读，属配置/执行链（work_order.bomId/routingId 非强制可留 null）；按需 successor。
- **不**做精确 KPI/报表数值断言——本计划解除「制造域交易数据存在」阻塞（数值非零可观测）；精确断言是 N=3 `2026-07-09-0930-3` 后继层。
- **不** seed 其他扩展域（maintenance/quality → 同批 N=2；CRM/CS/HR/logistics/b2b/contract/drp/aps → 后续批次，1445-1 Deferred 既定策略）。
- **不**改后端 `@BizQuery`/报表模板/ORM——纯部署数据层（镜像 2210-1）。

## Task Route

- Type: `implementation-only change`（纯部署期种子 CSV + 分析文档，零生产代码变更）
- Owner Docs: `docs/architecture/seed-data.md`（种子范式 + Non-Goals 段）、`docs/testing/e2e-runbook.md`（种子库启动段 + 域清单）、`docs/design/dashboards.md` §7（制造看板 KPI 口径，验证非空依据）、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（2210-1 范式，本计划镜像）
- Skill Selection Basis: `none`——可用技能集（nop-backend-dev=BizModel 逻辑 / nop-frontend-dev=AMIS/Playwright / nop-testing=Java JunitAutoTestCase / nop-debugging / nop-git-master / deep-interview）均不覆盖部署期 CSV 种子编写（数据建模 + 列映射 + FK 拓扑，非后端逻辑/前端/Java 测试）。Phase 3 GraphQL 抽样验证属数据可观测性确认，不涉及 BizModel 方法编写，仍无匹配技能。

## Infrastructure And Config Prereqs

- 既有 57 CSV 种子库已落地（fresh-DB seed 加载 0 冲突，制造域看板/报表可读取）——前置已满足。
- 平台 `DataInitInitializer` + `-Dnop.orm.init-database-data=true` + fresh-DB 重置（webServer 已含 `rm -f db/erp.mv.db`）——无需改。
- 回滚策略：纯新增 CSV + 分析文档，删除即回滚。

## Execution Plan

### Phase 1 - 制造域 seed 表映射 + 范围裁决（Proof + Decision）

Status: completed
Targets: 制造域 ORM `module-manufacturing/model/app-erp-manufacturing.orm.xml`、`docs/design/dashboards.md` §7、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`
Skill: `none`

- Item Types: `Proof | Decision`
- Prereqs: 既有 57 CSV 种子库（制造域可读取）

- [x] `Proof`：逐表派生列映射——读制造域 ORM 各候选 seed 表（work_order/cost_variance/forecast/forecast_line/work_order_line），标注每列角色（M=mandatory/FK=引用已 seed 主数据 ID/opt=可选留空），核实 mandatory 业务列全可填、FK 全指向已 seed 主数据（org=2/material 1-4/uom 1-4/warehouse 1-2/currency 1）或范围内更低拓扑表（cost_variance.workOrderId→work_order），UoM 列名确认为 `UO_M_ID`。核实 dict code（work-order-status: DRAFT/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/CANCELLED 等；forecast-status: DRAFT/APPROVED/CANCELLED）对齐 dict.yaml。**核实 crp_load 不在范围**（mandatory workcenterId FK + crp-load 报表配置链依赖，归 Deferred）。产出 seed 表映射分析文档（`docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`）。
      - Skill: `none`
- [x] `Decision`：seed 范围与 posted 裁决——(a) 范围 Decision：仅 seed 看板/报表直接读且 FK 可解析的 4 表（work_order/cost_variance/forecast/forecast_line），work_order_line 可选；crp_load 因 mandatory workcenterId FK + crp-load 报表依赖 workcenter/calendar/capacity 配置链移出范围至 Deferred（考虑替代方案 A「seed workcenter 配置链」vs B「crp_load 移出范围」+ 残留风险：A 引入配置链膨胀超出域表直 seed 范式 / B 致 crp-load 报表仍空但 production-variance/forecast-variance 两报表+看板非空已达成核心 Goal → 选 B）。(b) posted Decision：统一 `posted=false`，依据看板/报表读域表非 GL + 科目表无制造域专用科目（镜像 2210-1 裁决，约束记录非新决策）。(c) 日期窗口 Decision：work_order.businessDate/actualEndDate/plannedEndDate + forecast 期间置于当前月与若干历史月，使看板本期 KPI + trend 非空；注意 forecast-variance 报表实际量按 productId 聚合、区间重叠用 plannedStartDate/plannedEndDate（非 actualEndDate，区别于看板 trend）。本项记录每表关键日期/状态/dict 选择依据。
      - Skill: `none`

Exit Criteria:

- [x] 制造域 seed 表映射分析文档落盘，每表标注列角色 M/FK/opt + FK 目标 ID + dict code 核实 + 拓扑序 + 范围/posted/日期 Decision + 残留风险。

### Phase 2 - 制造域 seed CSV 编写（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_mfg_*.csv`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1 表映射 + 裁决

- [x] `Add`：编写制造域 4 表 CSV（work_order 含 ≥3 行覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED 三态驱动看板 KPI；cost_variance 含 ≥1 行 workOrderId 指向范围内 work_order 驱动 production-variance 报表；forecast APPROVED + forecast_line ≥1 行驱动 forecast-variance 报表）。列名严格对齐 ORM `code`（`ID` 显式、`UO_M_ID` 非 `UOM_ID`、省略审计+TENANT_ID、布尔小写、posted 统一 false），FK 引用 1234-1 固定 ID（org=2/currency=1/material 1-4）+ 范围内更低拓扑表（cost_variance.workOrderId→work_order），域内金额自洽（cost_variance 金额与 work_order 完工量逻辑自洽；forecast_line.materialId 宜与 work_order.productId 对齐使 forecast-vs-actual 对比有意义）。
      - Skill: `none`

Exit Criteria:

- [x] 制造域 CSV 文件落地 `_vfs/_init-data/`，列名经脚本逐表对齐 ORM `code`（0 错配，含 `UO_M_ID` 陷阱），mandatory 业务列全填，FK 全指向已 seed ID。

### Phase 3 - fresh-DB seed 加载 + GraphQL 非空验证 + E2E 0 回归（Proof）

Status: completed
Targets: fresh-DB 启动、`/graphql` 抽样
Skill: `none`

- Item Types: `Proof`
- Prereqs: Phase 2 CSV 落地

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块，确认新 CSV 打包入 runner jar 无后端污染）+ fresh-DB 启动（删 `db/erp.mv.db` + `-Dnop.orm.init-database-data=true`）确认 57+N CSV 全 `load-csv-data` 成功（0 主键冲突 / 0 列映射错误 / 0 参照完整性失败）+ GraphQL 抽样 `ErpMfgDashboard__getDashboardKpi`（传 startDate/endDate 覆盖种子区间）KPI 由 0 转非空且 FK 一致 + production-variance/forecast-variance `ErpMfgReport__renderHtml` 返回非空 HTML + `npx playwright test`（既有 spec 全绿 0 回归）。指定成功模式（KPI 非空/2 报表非空/0 回归）与失败模式（列映射错误→回 Phase 2 修 CSV/FK 失败→回 Phase 2 修 ID）。
      - Skill: `none`

Exit Criteria:

- [x] fresh-DB seed 加载 0 冲突 + GraphQL 抽样制造域看板 4 KPI + 2 报表（production-variance/forecast-variance）由 0/空转非空可观测 + 既有 E2E spec 0 回归。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 3 全绿

- [x] `Add`：`docs/architecture/seed-data.md` 增「制造域交易单据种子」段（域表直 seed 范式 + 4 表清单 + posted=false 裁决 + crp_load 配置链依赖移出范围说明 + Non-Goals 更新移除 manufacturing）+ `docs/testing/e2e-runbook.md` 种子库启动段 CSV 计数 57→57+N + 域清单补制造域 + `docs/testing/known-good-baselines.md` 增制造域种子基线行；2210-1 Deferred「其他扩展域交易种子（manufacturing）」登记解除（本计划 Closure 段登记）。
      - Skill: `none`

Exit Criteria:

- [x] seed-data.md 含制造域种子段；e2e-runbook + known-good-baselines 种子库计数/域同步；2210-1 Deferred manufacturing 子集登记解除（本计划 Closure 段登记）。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bd7a03b9ffeYBpt4vCcwdcyMO`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线核实整体高度诚实（行号/dict/ID/UO_M_ID 陷阱/跨计划依赖均逐一验真）。1 BLOCKER：crp_load.workcenterId mandatory FK + crp-load 报表依赖 workcenter/calendar/capacity 配置链，与 Non-Goal「不 seed workcenter」冲突致 Goal 不可达成 → **已修复**（crp_load + crp-load 报表移出范围至 Deferred，范围收为 4 表 + 看板 + 2 报表，同步修订 Goals/Non-Goals/Phase1/2/3/Deferred/Closure Gates）。1 MAJOR：Skill: none 理由误引「2210-1 标注 Skill: none」（实际 2210-1 用 nop-backend-dev）→ **已修复**（理由改为「CSV 种子编写无技能覆盖 + Phase 3 GraphQL 抽样不涉及 BizModel 方法编写」）。3 MINOR：实体行号订正（WorkOrder L560/CostVariance L1394）、forecast-variance 实际量口径补注（productId 聚合 + planned 区间）、forecast_line.materialId 与 work_order.productId 对齐建议 → **已采纳**。
- Independent draft review iteration 2: accept (`ses_0bd6fd8dcffe4e6jdnIxN76iS0`，独立 general 子代理，新会话冷重播无执行者上下文) — 实质性修订全部正确落地（crp_load 配置链依赖移出范围、4 表+2 报表范围、Skill 理由、行号、forecast-variance 口径经 live 核实）。发现 3 处文本残留（编辑未完全清扫）：Phase 3 Exit Criteria「3 报表」应「2 报表」、Phase 4「5 表清单」应「4 表清单」、Phase 1 Targets「§4」应「§7」→ **已修复**（与 Goal/Proof/Closure Gate/Deferred 范围一致）。跨计划一致性 PASS（N=1 ⊥ N=2，N=3 依赖二者，crp-load deferral 链 N=1→N=3 一致）。迭代 2 后无 BLOCKER/MAJOR，草案收敛为可接受执行契约。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。本计划结果表面为部署期数据（CSV），无生产 Java 代码变更；验证门控以 fresh-DB seed 加载 + 既有 E2E 0 回归 + GraphQL 非空一致为主。

- [x] 范围内行为完成（制造域 4 表种子 CSV 落地 + 看板/2 报表数值非空；crp-load 报表为计划内 Deferred）
- [x] 相关文档对齐（seed-data.md + e2e-runbook + known-good-baselines）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL 抽样 KPI 非空 + `npx playwright test`（既有 spec 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（crp_load/GL 凭证 seed/BOM/Routing/其他扩展域/精确数值断言均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### crp_load 表 + crp-load 报表 seed

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `erp_mfg_crp_load.workcenterId` 是 mandatory FK→ErpMfgWorkcenter（orm:546），且 crp-load 报表经 `CrpLoadCalculator.getLoadReport` 内部 resolveReportWorkcenters 取 workcenterCode + 从 WorkcenterCalendar/WorkcenterCapacity 算 capacityHours/loadRate——依赖整条 workcenter 配置链（workcenter/calendar/capacity 均未 seed）。seed crp_load 需先 seed 配置链，超出「域表直 seed」范式且与制造域看板（仅查 work_order）非空无关。本计划范围内 production-variance/forecast-variance 两报表 + 看板已达成非空核心 Goal。
- Successor Required: `yes`
- Trigger Condition: 当 workcenter/workcenter_calendar/workcenter_capacity 配置链 seed 落地后，由独立 successor 或本计划扩展承接 crp_load + crp-load 报表 seed。

### 制造域 GL 凭证/业财一体 seed（制造费用/差异过账凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 制造域看板/报表读域表（work_order/crp_load/cost_variance/forecast）非 GL；1234-1 种子科目表无制造费用/差异科目。seed GL 凭证不解除额外看板/报表阻塞，徒增参照复杂度。
- Successor Required: `yes`
- Trigger Condition: 当制造域业财一体端到端数值回归需 GL 串联（凭证↔源单据↔辅助账）时，补 GL 凭证 seed + 扩展种子科目表。

### 制造域配置/执行链 seed（BOM/Routing/Workcenter/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy/work_order_line）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些表不被看板/报表 `QueryBean` 直接读；work_order.bomId/routingId 非强制可留 null。seed 它们不解除看板/报表数值阻塞。
- Successor Required: `yes`
- Trigger Condition: 当制造域配置/执行链端到端回归需 BOM/Routing/Workcenter 等数据，或 work_order 需引用真实 BOM/Routing 时。

### 精确制造域 KPI/报表数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「制造域交易数据存在」阻塞（数值非零可观测）；精确断言「在制工单数=X」需固定种子集确定性 + 断言逻辑，是 N=3 successor 层。
- Successor Required: `yes`
- Trigger Condition: 本计划固化后，由 `2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md` 承接。

## Closure

Status Note: 执行完成（2026-07-09，主代理执行 4 阶段全绿）。4 张制造域表 CSV 落地 `_vfs/_init-data/`（work_order 4 行覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED×2；cost_variance 1 行 MATERIAL_USAGE→WO-001；forecast 1 行 APPROVED；forecast_line 1 行 materialId=1 对齐 work_order.productId=1），共 7 行，引用 1234-1 主数据固定 ID（org=2/currency=1/material 1/productId=1/uom 1），posted 统一 false（无 GL 科目 + 看板/报表读域表非 GL）。验证全绿：`mvn clean install -DskipTests`（154 模块，1:26）BUILD SUCCESS；fresh-DB 启动（61 CSV = 21 主数据 + 23 P2P/O2C + 13 运营域 + 4 制造域）0 冲突/0 列映射错误/0 参照失败，11.5s started（mfg 插入：work_order batch=4 / cost_variance=1 / forecast=1 / forecast_line=1 全成功）；`npx playwright test`（66 spec，9.6m）0 回归；GraphQL 抽样制造域看板 4 `@BizQuery` + 2 报表由 0/空转非空且 FK 一致（getDashboardKpi inProcessCount=1/periodCompletedQty=180/stockPartialCount=1/onTimeRate=0.5、statusDistribution COMPLETED=2/IN_PROCESS=1/STOCK_PARTIAL=1、trend 2026-06=100/2026-07=80、delayedAlert WO-003+WO-004 overdueDays=9、production-variance HTML 含 6,000.00/6,300.00/300.00/WO-2026-001、forecast-variance HTML 含 200/180/-20）。域内金额自洽（cost_variance.standardAmount=6000=WO-001.materialCost、varianceAmount=300=6300−6000、forecast-variance actualQty=180=100+80）。列名经脚本逐表对齐 ORM `code`（0 错配，含 `UO_M_ID` 陷阱）；mandatory 业务列全填；FK 全指向已 seed ID。文档对齐：seed-data.md 增「制造域交易单据种子」段、e2e-runbook + known-good-baselines 种子库计数 57→61 + 制造域 KPI 非空域；分析文档 `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md` 落盘；backlog README 增制造域种子工作项 ✅ done。crp_load + crp-load 报表因 mandatory workcenterId FK + workcenter/calendar/capacity 配置链依赖归 Deferred（触发条件：workcenter 配置链 seed 落地后）。

Deferred 解除登记：2210-1 Deferred「其他扩展域交易种子（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）」中 **manufacturing 子集已 seed（本计划解除）**；其余扩展域（maintenance/quality 同批 N=2 `2026-07-09-0930-2`；CRM/CS/HR/logistics/b2b/contract/drp/aps 后续批次）仍按域逐批（本计划 Non-Goal）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bd4f738bffearWrKcwOrdykY2`（general，新会话冷重播无执行者上下文，逐项核实 LIVE 仓库非采信执行者自述）。VERDICT: **PASS**（无 BLOCKER）。逐项核实：(A) 4 CSV 落地 + _init-data 计数 61（57+4）+ 分析文档 184 行存在；(B) 脚本逐表比对 ORM `code` 属性 0 未知列（work_order 27/cost_variance 17/forecast 8/forecast_line 9），UO_M_ID 陷阱正确（ORM `code="UO_M_ID"` mandatory），所有行字段数==表头；(C) mandatory 业务列全填非空（框架审计列已排除）；(D) FK 参照全解析（org 2/currency 1/material 1+3/uom 1 经 erp_md_*.csv 核实存在，workOrderId=1→WO-1/forecastId=1→FC-1 域内解析）；(E) dict 码值合法（IN_PROCESS/STOCK_PARTIAL/COMPLETED/MATERIAL_USAGE/MATERIAL/APPROVED/NORMAL 均在 ORM `<dicts>`，L35-152）；(F) posted 全 false（work_order+cost_variance；forecast/forecast_line ORM 无 POSTED 列→N/A 一致）；(G) 金额自洽（cost_variance 6300−6000=300、300/6000=5%→5.0000；forecast-variance actualQty 180=100+80 vs forecastQty 200→−20 可计算）；(H) work_order 4 行覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED×2 三态驱动看板 4 KPI；(I) crp_load.WORKCENTER_ID mandatory FK 已确认 + 未 seed crp_load/workcenter CSV，与 Deferred 裁决一致；(J) 文档对齐齐（seed-data.md 制造域段+状态行+Non-Goals、e2e-runbook 57→61+制造域、known-good-baselines 2026-07-09 行、backlog README 0930-1 ✅ done）；(K) 阶段/退出标准全 [x]、4 Phase Status completed、Closure Gates 除审计门外全 [x]。另独立核实 BizModel 读源：dashboard 4 @BizQuery 均查 ErpMfgWorkOrder、production-variance 读 ErpMfgCostVariance、forecast-variance 读 forecast(APPROVED)+forecast_line+work_order(COMPLETED,by productId,planned-overlap)，与种子设计完全对齐。审计发现 1 MINOR（Plan Status header L3 仍 `active` 应翻 `completed`，镜像 2210-1 同类 MINOR，已由执行者随审计门勾选一并修复），无技术交付物返工。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
