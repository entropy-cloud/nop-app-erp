# 2026-07-09-0628-1-manufacturing-crp-load-seed-value-assertion 制造域工作中心配置链 + crp_load 种子 + CRP 负荷报表数值断言

> Plan Status: completed
> Mission: erp
> Work Item: 制造域工作中心配置链（workcenter/workcenter_calendar/workcenter_capacity）+ crp_load 最小连通种子，使 CRP 负荷报表（crp-load-report）由空转非空可观测，并叠加数据驱动数值断言
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md` Deferred「crp_load 表 + crp-load 报表 seed」（Classification: `out-of-scope improvement`，Successor Required: yes，触发条件「当 workcenter/workcenter_calendar/workcenter_capacity 配置链 seed 落地后，由独立 successor 承接 crp_load + crp-load 报表 seed」——**本计划即满足该触发条件：一并 seed 配置链 + crp_load**）；AGENTS.md 当前重点「各域细化端到端验证」（crp-load-report 是当前唯一无数值断言的报表，配置链 seed 后即可补齐，完成全报表域数值断言覆盖里程碑）
> Related: `docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md`（completed，制造域 work_order/cost_variance/forecast 种子范式 + crp_load 配置链依赖 Deferred）、`docs/plans/2026-07-09-1145-2-quality-spc-seed-value-assertion.md`（completed，Strategy C 完整参照 + 表映射/期望值派生/断言范式镜像源）、`docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md`（completed，制造域看板/报表数值断言范式，crp-load 当时归 Deferred 未覆盖）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`/`ls`，非采信旧记忆）：

- **CRP 负荷报表读源已就绪**（`module-manufacturing/erp-mfg-service/.../report/ErpMfgReportBizModel.java`）：
  - `buildCrpLoadDataset(workcenterId, startDate, endDate, context)`(:237-258)：委托 `crpLoadBiz.getLoadReport(from, to, wcIds)`，区间未指定时从既有 CrpLoad 快照推导 `[min,max] loadDate`（`:334-349` `deriveCrpWindow` 读 `daoProvider.daoFor(ErpMfgCrpLoad).findAll()` 取 min/max loadDate）。
  - 经 `reportName="crp-load-report"` case(:171-172) 路由；输出行字段 `crpItemToMap`(:262)：workcenterId/workcenterCode/loadDate/loadHours/setupHours/capacityHours/loadRate/overloaded，对齐 `crp.md §负载报表`。
  - GraphQL 暴露：`@BizQuery crpLoadData`(:206-211) + `renderHtml`（reportName 透传 `buildCrpLoadDataset`）。
- **CrpLoadCalculator.getLoadReport**(`CrpLoadCalculator.java:137-183`) 读源链已核实：
  - `resolveReportWorkcenters`(:140) = workcenterIds（若提供）否则 = 区间内有 CrpLoad 行**或**有 Calendar 的工作中心。
  - `indexLoads`(:145) 聚合 `erp_mfg_crp_load` 行 → 按 workcenter×date 累加 `loadHours`/`setupHours`。
  - `efficiencyByWorkcenter`(:147) 读 `erp_mfg_workcenter_capacity.efficiencyFactor`（缺省回退 `BigDecimal.ONE`）。
  - `calendarsByWorkcenter`(:148) 读 `erp_mfg_workcenter_calendar` → `availableHours(calendars, d)`(:164) 算当日产能小时。
  - `workcenterCodes`(:149) 读 `erp_mfg_workcenter.code`。
  - `capacityHours = availableHours × efficiency`；`loadRate = loadHours / capacityHours`；`overloaded = loadRate > 阈值`（`erp-mfg.crp-overload-threshold` 默认 1.0）。
  - **关键**：报表非空需 crp_load 行（驱动 loadHours）+ workcenter（驱动 code）+ workcenter_calendar（驱动 capacityHours）。workcenter_capacity 提供 efficiencyFactor（缺省 1 仍可工作，但 seed 一行保证参照完整）。
- **四表 ORM 已定义**（`module-manufacturing/model/app-erp-manufacturing.orm.xml`）：
  - `ErpMfgWorkcenter`（table `erp_mfg_workcenter` :429）：mandatory `CODE`/`NAME`；UK `(code)`；capacity/hourlyRate/workHoursPerDay/isExternal 可选。**无 mandatory FK**。
  - `ErpMfgWorkcenterCalendar`（:455）：mandatory `WORKCENTER_ID`/`CALENDAR_NAME`/`SHIFT_TYPE`（dict `erp-mfg/shift-type`）；`<to-one workcenter>` 逻辑引用 workcenter；`<to-one org>` 引用 erp_md_organization；startTime/endTime/effectiveFrom/effectiveTo/isActive/workDatePattern(dict `erp-mfg/work-date-pattern`) 可选。
  - `ErpMfgWorkcenterCapacity`（:490）：mandatory `WORKCENTER_ID`/`MATERIAL_ID`/`CAPACITY_PER_HOUR`；`<to-one workcenter>`/`<to-one material>`(erp_md_material)/`<to-one org>`；setupTime/cleanupTime/efficiencyFactor(默认1)/isActive 可选。
  - `ErpMfgCrpLoad`（:527）：mandatory `WORKCENTER_ID`/`LOAD_DATE`；`<to-one workcenter>`/`<to-one workOrder>`(弱指针 erp_mfg_work_order)/`<to-one org>`；loadHours/setupHours(默认0)/workOrderId 可选。
- **制造域既有种子（0930-1，提供 FK 锚点）**：`erp_mfg_work_order.csv` 4 行（ID 1-4，CODE WO-2026-001~004，orgId=2，productId=1，COMPLETED/IN_PROCESS/STOCK_PARTIAL 三态）。1234-1 主数据：org=2 / material 1-4 / currency=1 / uom=1。
- **四表种子当前不存在**：`ls app-erp-all/src/main/resources/_vfs/_init-data/ | grep -E 'workcenter|crp_load'` = 空；当前 87 CSV（含 0930-1 的 work_order/cost_variance/forecast/forecast_line + 1145-2 的 SPC 三表）。
- **断言范式已就绪**：`tests/e2e/reports/_helper.ts#assertReportRenderedWithValue`（0930-3/1045-2/1145-1 已用，断 `renderHtml` HTML 含期望数值 token，剥离千分位后匹配）；`tests/e2e/reports/mfg-production-variance.value.spec.ts` / `mfg-forecast-variance.value.spec.ts`（0930-3 范式，crp-load 是制造域唯一缺数值断言的报表）。当前 80 spec 文件 / 87 CSV。
- **保护区域**：纯部署期数据（CSV）+ 断言 spec（tests/）+ 分析/日志文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 0930-1/1145-2）。属 `plan-first`（跨表 FK 拓扑序 + 配置链依赖 + 涉及 >5 文件）。

剩余差距：(1) 四表零数据，crp-load-report 渲染空数据集（`getLoadReport` 返回空 list 或区间推导无行），无 CRP 负荷可观测；(2) `availableHours` 产能小时精确计算口径（单班 startTime~endTime vs workDatePattern）待 Phase 1 核实以派生确定性期望值；(3) shift-type/work-date-pattern 字典合法 code 待 Phase 1 核实。

## Goals

- 在 `_vfs/_init-data/` 增补制造域工作中心配置链 + crp_load 最小连通种子 CSV（workcenter + workcenter_calendar + workcenter_capacity + crp_load，以一致 workcenterId 串联），使 `crp-load-report`（`ErpMfgReport__renderHtml` 经 `CrpLoadCalculator.getLoadReport`）由空转**非空可观测**（报表含 workcenterCode + loadDate + 非零 loadHours + capacityHours + loadRate）。
- 经既有 config-gated fresh-DB 启动加载，验证新 CSV **0 主键冲突 / 0 列映射错误 / 0 参照失败**。
- 叠加 CRP 负荷报表数据驱动数值断言：新增 `mfg-crp-load.value.spec.ts`，断言 `renderHtml`（reportName=`crp-load-report`）HTML 含确定性派生 token（workcenterCode + loadDate + loadHours + capacityHours/loadRate）。
- 解除 0930-1 Deferred「crp_load 表 + crp-load 报表 seed」；完成全报表域数值断言覆盖里程碑（crp-load 为最后一个缺口）。

## Non-Goals

- **不**触发 `CrpLoadCalculator.calculateLoad` 重算链（写新 crp_load 行覆盖 seed）——本计划仅 seed 静态 crp_load 行 + 配置链令报表可观测。`ErpMfgCrpRunJob`/`calculateLoad` 经 nop-job 双层门控（cron 默认空），fresh-DB 启动不触发重算；config `erp-mfg.crp-load-source`（默认 WORK_ORDER）不影响 `getLoadReport` 读既有行。
- **不**seed 制造域配置/执行链其他表（BOM/Routing/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy/work_order_line）——0930-1 既定 Deferred，crp-load 报表 `getLoadReport` 不读这些表。
- **不**seed 制造域 GL 凭证——0930-1 既定 Deferred（制造域看板/报表读域表非 GL）。
- **不**做 CRP 负荷前端可视化增强（echarts 负荷/产能对比图等，前端能力面 successor）。
- **不**改 `model/*.orm.xml`——纯数据文件 + 断言 spec，零 ORM 变更。
- **不**做像素级视觉回归 / 报表下载产物 diff（0637-1 Deferred，触发条件未变）。

## Task Route

- Type: `implementation-only change`（seed CSV + 断言 spec）
- Owner Docs: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（四表 schema 权威）、`docs/design/manufacturing/crp.md`（§负载报表口径 + §核心设计点）、`docs/architecture/seed-data.md`（域表直 seed 范式 + posted 裁决）、`docs/testing/e2e-runbook.md`（断言范式 + 期望值表）
- Skill Selection Basis: Phase 1-2 CSV 种子编写无技能覆盖（列 code 映射 + 拓扑序 + 配置链 FK 解析，非 BizModel 方法编写，`Skill: none`）；Phase 3 断言 spec 属浏览器 E2E（Playwright）→ `nop-testing`（`nop-testing` SKILL.md §什么时候用我 明列「E2E 测试 / Playwright / 端到端」并路由 `02-core-guides/e2e-testing.md`，项目方法源 `docs/references/playwright-e2e-guide.md`）；Phase 4 文档对齐无技能覆盖（`Skill: none`）。

## Infrastructure And Config Prereqs

- **预构建 runner jar**：依赖 `app-erp-all/target/quarkus-app/quarkus-run.jar`（`mvn clean install -DskipTests` 产物）。`playwright.config.ts` webServer fresh-DB 重置 + `-Dnop.orm.init-database-data=true` 加载种子（已就绪）。
- **config 门控**：`erp-mfg.crp-overload-threshold`（默认 1.0）保持默认；`erp-mfg.crp-load-source`（默认 WORK_ORDER）不影响 `getLoadReport` 读行。无需改配置。
- **无新外部依赖/密钥**。回滚策略：seed CSV + spec 纯新增（删除即回滚）。

## Execution Plan

### Phase 1 - 四表 seed 表映射 + capacityHours 口径核实 + 期望值派生（Proof + Decision）

Status: completed
Targets: `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md`
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：逐表派生列映射——读四表 ORM（workcenter :429-450 / workcenter_calendar :455-485 / workcenter_capacity :490-522 / crp_load :527-557），标注每列角色（M=mandatory/FK=逻辑引用/opt=可选留空），核实 mandatory 业务列可填、FK（workcenterId→workcenter / orgId→erp_md_organization=2 / materialId→material 1-4 / workOrderId→work_order 1-4 弱指针）可解析。核实 dict code（`erp-mfg/shift-type` / `erp-mfg/work-date-pattern`）对齐 dict.yaml 取合法值。产出 seed 表映射分析文档。
  - Skill: none
- [x] `Proof`：核实 `CrpLoadCalculator.availableHours(calendars, date)` 产能小时精确口径——读 `CrpLoadCalculator.java` `availableHours` 实现（单班 startTime~endTime 时长 vs workDatePattern 周末跳过逻辑），据 seed 的 calendar startTime/endTime 派生 capacityHours 确定性值（08:00~16:00 = 8h），使 loadRate = loadHours/capacityHours 可手算。记录口径 + 派生公式。
  - Skill: none
- [x] `Decision`：seed 范围裁决——(a) **seed 4 表完整配置链**（workcenter 1 行 + workcenter_calendar 1 行 + workcenter_capacity 1 行 + crp_load 1 行），以一致 workcenterId=1 串联；crp_load.workOrderId 引用 0930-1 WO-1（id=1，弱指针参照丰富）；workcenter_capacity.materialId=1（1234-1 产品甲）。(b) 仅 seed workcenter + crp_load（不 seed calendar/capacity）被 rejected——`getLoadReport` 需 calendar 算 capacityHours（缺 calendar 则 capacityHours=0 → loadRate 除零/无穷），报表数值不可观测/不稳定。(c) posted 裁决：四表均无 posted 列（crp_load 为负荷快照非 GL，看板/报表读域表，镜像 0930-1 裁决，N/A）。(d) 日期窗口：crp_load.loadDate 置 2026-07-15（种子月内确定性单日），calendar.effectiveFrom=2026-07-01/effectiveTo=2026-07-31 覆盖，使报表区间 [2026-07-15, 2026-07-15] 确定性返回 1 行。(e) 期望值 token 派生：workcenterCode(WC-001) + loadDate(2026-07-15) + loadHours(4.00) + capacityHours(8.00) + loadRate(0.50)，逐 token 标注 seed 行依据 + 计算口径。记录选择 + rejected 替代 + 残留风险。
  - Skill: none

Exit Criteria:

- [x] seed 表映射 + capacityHours 口径 + 期望值派生分析文档落盘（每 token 标注期望值 + seed 行依据 + availableHours 计算口径 + dict 合法值确认）

### Phase 2 - 四表 seed CSV 编写（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_mfg_workcenter.csv`、`erp_mfg_workcenter_calendar.csv`、`erp_mfg_workcenter_capacity.csv`、`erp_mfg_crp_load.csv`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：编写 `erp_mfg_workcenter.csv`（≥1 行：CODE/NAME 必填 + 可选产能列如 HOURLY_RATE/WORK_HOURS_PER_DAY 填值辅助 capacityHours 语义，省略审计+TENANT 列）。
  - Skill: none
- [x] `Add`：编写 `erp_mfg_workcenter_calendar.csv`（≥1 行：WORKCENTER_ID 引用已 seed workcenter / CALENDAR_NAME / SHIFT_TYPE(dict 合法值) / START_TIME+END_TIME（Phase 1 capacityHours 口径所需）/ EFFECTIVE_FROM+EFFECTIVE_TO 覆盖种子月 / ORG_ID=2 / IS_ACTIVE=true，省略审计列）。
  - Skill: none
- [x] `Add`：编写 `erp_mfg_workcenter_capacity.csv`（≥1 行：WORKCENTER_ID 引用同 workcenter / MATERIAL_ID=1 / CAPACITY_PER_HOUR / EFFICIENCY_FACTOR=1 / ORG_ID=2 / IS_ACTIVE=true，省略审计列）。
  - Skill: none
- [x] `Add`：编写 `erp_mfg_crp_load.csv`（≥1 行：WORKCENTER_ID 引用同 workcenter / LOAD_DATE=2026-07-15 / LOAD_HOURS（非零如 6）/ SETUP_HOURS（如 1）/ WORK_ORDER_ID=1（弱指针 0930-1 WO-1）/ ORG_ID=2，省略审计列）。
  - Skill: none
- [x] `Proof`：脚本校验所有新 CSV 列名对齐 ORM `code`（0 错配）+ mandatory 业务列全填 + FK 引用上游存在（org=2 / workcenterId 自洽 / materialId=1 / workOrderId=1 均经 1234-1/0930-1 核实存在）+ dict code 合法。
  - Skill: none

Exit Criteria:

- [x] 4 张新 CSV 落地，列名对齐 ORM（0 错配），mandatory 列全填，FK 自洽，dict code 合法

### Phase 3 - fresh-DB seed 加载 + GraphQL 非零验证 + CRP 负荷报表断言（Proof + Add）

Status: completed
Targets: `tests/e2e/reports/mfg-crp-load.value.spec.ts`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2；本阶段需重打包 jar 使新 CSV 入 app jar（`mvn clean install -DskipTests` 为打包前置，完整仓库验证在 Closure Gates 运行）

- [x] `Proof`：重打包 app jar（新 CSV 打包入 jar）+ fresh-DB 启动（`-Dnop.orm.init-database-data=true`），验证 87+N CSV 全 load-csv-data 成功（0 冲突/0 列错配/0 参照失败）。GraphQL 抽样 `ErpMfgReport__renderHtml`（reportName=`crp-load-report`，data 含 workcenterId=1/startDate=2026-07-15/endDate=2026-07-15）返回 HTML 含 Phase 1 派生的非空 token（workcenterCode + loadDate + 非零 loadHours + capacityHours + loadRate），与期望值一致。
  - Skill: none
- [x] `Add`：新增 `tests/e2e/reports/mfg-crp-load.value.spec.ts`，调 `assertReportRenderedWithValue`（`reports/_helper.ts`）断言 `ErpMfgReport__renderHtml`（reportName=`crp-load-report`，经 `data` 内联 map 传 workcenterId/startDate/endDate，镜像 fin-income-statement `data:{periodId}` 范式）HTML 含 Phase 1 派生 token（workcenterCode + loadDate + loadHours + capacityHours/loadRate，剥离千分位后匹配）。
  - Skill: `nop-testing`
- [x] `Proof`：`npx playwright test tests/e2e/reports/mfg-crp-load.value.spec.ts --workers=1` 全绿（CRP 负荷报表非空 token 断言 + 既有制造域报表断言无回归）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] fresh-DB 加载 4 张新 CSV 0 冲突 + crp-load-report GraphQL 返回 HTML 含非空 token 且与 Phase 1 期望值一致
- [x] `mfg-crp-load.value.spec.ts` 落地全绿，token 与 Phase 1 期望值表逐项一致

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/README.md`、`docs/logs/2026/07-09.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3

- [x] `Add`：`docs/architecture/seed-data.md` 增「制造域工作中心配置链 + crp_load 种子」段（4 表最小连通 + capacityHours 口径 + Non-Goals 移除 crp_load）；`docs/testing/e2e-runbook.md` 种子库计数（87→91）+ 制造域 CRP 负荷报表非空 + 期望值表 crp-load 行 + 分析文档清单增 0628-1；`docs/testing/known-good-baselines.md` 增 0628-1 基线行；`docs/backlog/README.md` 增制造域 CRP 种子工作项 ✅ done；`docs/logs/2026/07-09.md` 增 0628-1 日志条目（含验证状态）；0930-1 plan Deferred「crp_load 表 + crp-load 报表 seed」标记 RELEASED（本计划 Closure 段登记）。
  - Skill: none

Exit Criteria:

- [x] 文档对齐落地（seed-data + runbook + baselines + backlog + 日志 + 0930-1 Deferred 解除登记）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0bc22dd8bffeuwrJbkABoReQaS`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（buildCrpLoadDataset 委托 getLoadReport / getLoadReport 读源链 workcenter+calendar+capacity / 四表 ORM mandatory+FK+dict / work_order 种子 4 行 / CSV 87 / spec 80 / assertReportRenderedWithValue helper / 0930-1 crp_load Deferred 触发条件 / shift-type+work-date-pattern 字典存在；预应用 1145-2 iteration-1 修复：rule-7 本地化 + nop-testing 正确应用）。无 BLOCKER/MAJOR。1 MINOR：基线观察函数名漂移（`resolveReportRange`→`deriveCrpWindow`，行 `:335-340`→`:334-349`，行为描述正确）→ **已修复**。规则合规全核实（rule 1/4/7/8/9 + 反松弛 + Deferred 触发条件 + 模板结构 + header）。草案收敛为可接受执行契约，计划转 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（4 表配置链 + crp_load 种子落地 + crp-load-report 非空可观测 + 数值断言全绿）
- [x] 相关文档对齐（seed-data + e2e-runbook + known-good-baselines + backlog + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL crp-load-report 非空 + `npx playwright test`（全套件 fresh-DB seed 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（CRP 前端可视化/重算链/其他配置链表/GL 凭证均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CRP 负荷前端可视化增强（echarts 负荷/产能对比图）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划使 crp-load 报表数值可观测（HTML token 断言），不做 echarts 可视化增强（前端能力面）。
- Successor Required: `yes`
- Trigger Condition: 当产品要求 CRP 负荷看板可视化（负荷/产能趋势图、超负荷高亮）时。

### crp_load 重算链 seed / calculateLoad 端到端

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划 seed 静态 crp_load 行令报表可观测，不触发 `CrpLoadCalculator.calculateLoad` 重算（重算会清区间写新行覆盖 seed）。重算链端到端回归（WorkOrder 计划日期→负荷分派→快照写入）属独立能力面，且 0930-1/2237-1 Java 测试（`TestErpMfgCrpLoad`）已覆盖重算逻辑。
- Successor Required: `yes`
- Trigger Condition: 当需在部署期种子上验证 calculateLoad 从 WorkOrder 重算到 crp_load 快照的端到端正确性时。

## Closure

Status Note: 执行完成（2026-07-09，主代理执行 4 阶段全绿，独立结束审计 PASS）。4 张制造域工作中心配置链 + crp_load 表 CSV 落地 `_vfs/_init-data/`（workcenter 1 行 WC-001 主装配线 + workcenter_calendar 1 行 单班 08:00~16:00 ALL_WEEK IS_ACTIVE=true + workcenter_capacity 1 行 efficiencyFactor=1 IS_ACTIVE=true + crp_load 1 行 loadDate=2026-07-15 loadHours=4 setupHours=1 workOrderId→0930-1 WO-1），共 4 行，引用 1234-1 主数据固定 ID（org=2/material=1）+ 0930-1 work_order=1 弱指针，四表均无 posted 列（crp_load 为负荷快照非 GL，看板/报表读域表，镜像 0930-1 裁决）。验证全绿：`mvn clean install -DskipTests`（154 模块，1:26）BUILD SUCCESS；fresh-DB 启动（91 CSV = 87 + 4 制造域工作中心配置链+crp_load）0 冲突/0 列映射错误/0 参照失败（webServer 启动成功）；`npx playwright test tests/e2e/reports/mfg-crp-load.value.spec.ts --workers=1` **1 passed（23.9s）**；既有制造域报表无回归（mfg-crp-load.smoke + production-variance/forecast-variance.value 3 passed）；`npx playwright test` 全套件 **85 passed（11.8m）0 回归**。crp-load-report 经 `CrpLoadCalculator.getLoadReport` 由空转非空可观测，HTML 含确定性 token WC-001/2026-07-15/4.00/8.00/0.50（capacityHours=shiftHours(08:00,16:00)=8.0000×efficiency(1)、loadRate=4.00/8.0000=0.5000，与 Phase 1 期望值逐项一致）。列名经脚本逐表对齐 ORM `code`（0 错配，处理 ext:dict 含 `/` 的 regex 陷阱）；mandatory 业务列全填；FK 全指向已 seed ID；dict code（ONE_SHIFT/ALL_WEEK）合法。文档对齐：seed-data.md 增「制造域工作中心配置链 + crp_load 种子」段 + crp_load Non-Goal 移除/RELEASED；e2e-runbook 种子库 87→91 + crp-load 期望值表 + 27→28 数值断言 spec；known-good-baselines 增 0628-1 基线行；backlog README 增 0628-1 ✅ done；日志 `docs/logs/2026/07-09.md` 增 0628-1 条目（含验证状态）；分析文档 `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md` 落盘。完成全报表域数值断言覆盖里程碑（crp-load 为最后一个缺口）。

Deferred 解除登记：0930-1 Deferred「crp_load 表 + crp-load 报表 seed」RELEASED（successor 本计划已落地：seed 完整 workcenter 配置链 + crp_load，使 crp-load 报表非空可观测 + 数值断言）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bc00d7bbffeoehMli66h2ga6O`（general，新会话冷重播无执行者上下文，逐项核实 LIVE 仓库非采信执行者自述）。VERDICT: **PASS**（0 BLOCKER / 0 MAJOR / 2 MINOR）。逐项核实：(A) 4 CSV 落地 + _init-data 计数 91（87+4）；(B) CSV 列名逐表比对 ORM `code`（workcenter/calendar/capacity/crp_load）0 未知列，所有行字段数==表头；(C) mandatory 业务列全填非空（框架审计列已排除）；(D) FK 参照全解析（workcenterId=1 自洽 / orgId=2→ERP-CO / materialId=1→MAT-001 / workOrderId=1→WO-2026-001）；(E) dict 码值合法（ONE_SHIFT/ALL_WEEK）；(F) 四表 ORM 无 posted 列（确认 crp_load posted 列不存在，:594+ posted 属 ErpMfgWorkOrder）；(G) 报表路由核实（prepareDataset case "crp-load-report" :171 → buildCrpLoadDataset :237 → getLoadReport）；(H) capacityHours/loadRate 独立重派生（shiftHours(08:00,16:00)=480min/60=8.0000 × efficiency 1 = 8.0000；loadRate=4.00/8.0000=0.5000；overloaded=false）与 spec token 8.00/0.50 逐项一致；(I) 2026-07-15=周三 + ALL_WEEK 恒命中 + EFFECTIVE 2026-07-01~07-31 覆盖 + IS_ACTIVE=true 三重防护确认；(J) 零生产代码变更（git status 仅 CSV + spec + docs，无 .java/.orm.xml/.xbiz.xml/.page.yaml/.view.xml）；(K) 文档对齐齐（seed-data crp-load 段+Non-Goal 移除、e2e-runbook 87→91+crp-load 期望值表+28 spec、known-good-baselines 0628-1 行、backlog 0628-1 ✅、日志 0628-1 条目、0930-1 Deferred RELEASED）；(L) 4 Phase Status completed + 全部 [x] + Exit Criteria 全 [x] + Draft Review 存在。2 MINOR：① Plan Status/Closure Gates/Closure Status Note 待审计后由执行者翻完成（本编辑同步修复）；② 工作树另有无关 plan 0628-2 草案文件同行（与本计划无关，cosmetic）。

Follow-up:

- CRP 负荷前端可视化 / crp_load 重算链 seed 为计划内 Non-Goal（附触发条件），非阻塞跟进项。
