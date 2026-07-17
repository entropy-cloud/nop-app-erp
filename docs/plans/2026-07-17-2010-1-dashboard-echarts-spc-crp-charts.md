# 2026-07-17-2010-1-dashboard-echarts-spc-crp-charts 看板 echarts 交互式可视化深化（SPC 控制图 + CRP 负荷图）

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 看板运行时视觉深化（运营监控交互式图表 successor）
> Source: `docs/plans/2026-07-07-1100-3-dashboard-deferred-indicators.md` Deferred「SPC 控制图完整可视化（echarts 控制图 + Western Electric 规则标记 + 控制限区间带）」（触发条件=报表/看板 e2e 可视化套件建立时——**已满足**，`tests/e2e/visual/dashboards.visual.spec.ts` + `reports.visual.spec.ts` 已落地）+ `docs/plans/2026-07-09-0628-1-manufacturing-crp-load-seed-value-assertion.md` Deferred「CRP 负荷前端可视化增强（echarts 负荷/产能对比图）」（out-of-scope improvement，successor yes）+ `docs/plans/2026-07-09-1145-2-quality-spc-seed-value-assertion.md` Deferred「SPC 控制图完整可视化（echarts UCL/LCL + 违规点高亮）」
> Related: `2026-07-07-1100-3`（交付 SPC 预警 + 项目毛利率 KPI 卡片，本计划补其图表深度 successor）、`2026-07-06-1606-2-remaining-domain-dashboards-frontend.md`、`2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md`（同批 N=2，本计划先落地使像素基线捕获完整态）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17，`read`/`grep`，非采信旧记忆）：

### 触发条件已满足

- `tests/e2e/visual/dashboards.visual.spec.ts`（10 域看板 AMIS 渲染 DOM 断言）+ `tests/e2e/visual/reports.visual.spec.ts`（全 24 报表域）已落地——1100-3 SPC 控制图 Deferred 触发条件「报表/看板 e2e 可视化套件建立时」**已满足**。

### 后端数据源已就绪（仅缺看板取数 @BizQuery + 前端图表）

- **SPC 控制图数据**（quality 域，全物化）：
  - `ErpQaSpcChart`（`module-quality/model/app-erp-quality.orm.xml`）含 `cl`/`ucl`/`lcl`（中心线 + 控制上下限，由 `SpcControlLimitCalculator.recalculate` 计算并 `chart.setCl/setUcl/setLcl` 持久化，`SpcControlLimitCalculator.java:141-143`）+ `chartType`（计量型 X_BAR_R/X_BAR_S/X_MR）。注：`targetValue` 属 `ErpQaCalibration`、`grandMean` 属 `ErpQaSpcCapability`，均不在本 `@BizQuery` 范围（仅 ErpQaSpcChart + ErpQaSpcSample）；CL 取 `chart.cl`。
  - `ErpQaSpcSample` 含 `chartId`/`subgroupNo`/`mean`（样本均值，绘图主点）+ `isOutOfControl`(propId 15) + `violatedRules`（违规 Western Electric 规则码）。
  - 即 UCL/LCL/CL + 样本点 + 违规标记四要素全部已持久化，仅需同域只读聚合到看板。
- **CRP 负荷数据**（manufacturing 域）：
  - `ErpMfgCrpLoad`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:536`）仅物化 `loadDate`/`workcenterId`/`loadHours`/`setupHours`（列级），**无** `capacityHours`/`loadRate` 列。
  - `capacityHours`/`loadRate`/`overloaded` 经 `CrpLoadCalculator.getLoadReport(LocalDate periodFrom, LocalDate periodTo, List<Long> workcenterIds)`（`CrpLoadCalculator.java:137`）由 `WorkcenterCalendar` 出勤时段 × `WorkcenterCapacity.efficiencyFactor` 运行时派生，装入既有 DTO `CrpLoadReportItem`（`item.setCapacityHours`/`setLoadRate`/`setOverloaded`，:176-177）。crp-load 报表路径 `buildCrpLoadDataset → getLoadReport` 已沿用此链。
  - 即看板取数 `@BizQuery` **委派** `CrpLoadCalculator.getLoadReport(...)` 后按 `loadDate` 聚合 `CrpLoadReportItem`，无需重算产能/负荷率，无新实体。

### 看板图表范式已验证（本计划复用，零范式新增）

quality 看板 `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml` 已含 `trendChart`（line，合格率趋势，`${'$'}months` 参数化 raw GraphQL）+ `defectChart`（pie，不合格原因 TOP），经 `type: chart` + `api.dataType: raw` + `adaptor` 转 echarts option。mfg 看板有 `statusChart`（pie，工单状态分布）+ `trendChart`（line）类 echarts 图表。本计划 SPC 控制图（line + markLine UCL/LCL/CL + 违规点高亮）+ CRP 负荷图（bar 负荷 vs line 产能 双轴）复用同范式。

### 剩余差距

1. quality 看板仅有 `spcWarningService`（1100-3 交付的 KPI/预警**卡片**），**缺交互式 SPC 控制图**（UCL/LCL/CL 区间带 + 样本点 + 违规点高亮）。
2. mfg 看板有 statusChart/trendChart 类图表，**缺 CRP 负荷/产能对比图**。
3. 两域看板各缺一个取数 `@BizQuery`（同域只读聚合/委派既有计算器，无新实体）。

## Goals

- **SPC 控制图**：`ErpQaDashboardBizModel` 新增 `@BizQuery getSpcControlChartData(ctx, chartId?)` —— 聚合 `ErpQaSpcChart`（`cl`/`ucl`/`lcl` 控制限 + `chartType`）+ `ErpQaSpcSample`（subgroupNo/mean/isOutOfControl/violatedRules），返回结构化 DTO（控制限 ucl/lcl + 中心线 cl=chart.cl + 样本点序列 + 违规点标记）。quality 看板 `main.page.yaml` 补一 echarts 控制图（line 样本均值 + markLine UCL/LCL/CL + 违规点 visualMap 高亮），消费新 `@BizQuery`。
- **CRP 负荷图**：`ErpMfgDashboardBizModel` 新增 `@BizQuery getCrpLoadChartData(ctx, workcenterId?, dateFrom, dateTo)` —— **委派** `CrpLoadCalculator.getLoadReport(periodFrom, periodTo, workcenterIds)`（复用既有产能/负荷率派生链）后按 `loadDate` 聚合 `CrpLoadReportItem`（Σ loadHours / Σ capacityHours / 派生 loadRate），返回结构化 DTO。mfg 看板 `main.page.yaml` 补一 echarts 双轴图（bar 负荷小时 + line 产能小时 + 负荷率），消费新 `@BizQuery`。
- **测试**：两 `@BizQuery` 同域只读聚合行为测试（空数据零值结构 / 多样本序列 / 过滤 / 控制限传递）。
- **owner doc 收口**：解除 1100-3/0628-1/1145-2 三处图表可视化 Deferred（补 `**RELEASED by 2026-07-17-2010-1**`）。

## Non-Goals

- **不改 ORM/契约/codegen**：数据源实体已物化，仅看板 BizModel 加同域只读 `@BizQuery` + page.yaml 加图表，无 `_gen/` 手改、无模型变更。
- **不做像素级截图基线**：归同批 N=2 计划 `2026-07-17-2010-2`，本计划交付 DOM 层即可。
- **不做 SPC 计数型控制图（P/NP/C/U）渲染**：0305-2 裁定计算引擎仅计量型，计数型聚合算法归独立 successor（触发条件：计数型计算引擎落地时）。
- **不做 CRP 工作中心日历甘特视图 / 产能细化拆分**：本期负荷 vs 产能对比图；甘特/精确费率拆分归独立 successor。
- **不做看板定时刷新 / WebSocket 实时推送 / 物化视图缓存**：归 optimization candidate（对齐 1606-1/1606-2 既有 Non-Goal）。
- **不做报表侧 crp-load 报表改造**：crp-load 报表已渲染，本计划仅看板图表。

## Task Route

- Type: `implementation-only change`（两看板 BizModel 加同域只读 `@BizQuery` + 两 AMIS echarts 图表；ORM/契约无变更）。
- Owner Docs: `docs/design/dashboards.md`（§实现约定分层布局，既有）、`docs/design/quality/spc.md`（SPC 控制图语义/UCL-LCL-Western Electric 规则，既有）、`docs/design/manufacturing/crp.md`（CRP 负荷/产能语义，既有）。
- Skill Selection Basis: 后端 BizModel 同域只读聚合（注入 `IDaoProvider`/`IOrmTemplate`，镜像 1606-1/1100-3 范式，无跨域 I*Biz）→ 加载 `nop-backend-dev`；AMIS page.yaml echarts 图表（`type: chart`+`adaptor`）→ 加载 `nop-frontend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。三技能必需输入（dashboards.md/spc.md/crp.md 既有、数据源实体已物化、trendChart/defectChart AMIS 范式既有）均就绪。
- Protected Areas: 看板 BizModel/page.yaml 属应用层非保护区域；不改 ORM/`_gen/`；不引入业财过账（纯只读）。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更；无 codegen 增量。
- 配置键（对齐 1606-1 `erp-dash.*` 范式，Phase 1 落地配套 reader）：`erp-dash.qa-spc-default-chart-id`（默认 null，未配置且 `chartId` 入参为空时返回最近一张 `ErpQaSpcChart`）；`erp-dash.mfg-crp-default-days`（默认 7，未传 dateFrom/dateTo 时取近 N 天）。复用既有 `ErpQaConfigs`/`ErpMfgConfigs` reader 范式。
- 回滚策略：全部改动为应用层 Java + page.yaml，git 可逆；配置键默认值保持现有看板行为（仅追加图表，不动既有卡片）。

## Execution Plan

### Phase 1 - 后端两 @BizQuery 同域只读聚合

Status: completed
Targets: `ErpQaDashboardBizModel`（`module-quality/erp-qa-service/.../dashboard/`）、`ErpMfgDashboardBizModel`（`module-manufacturing/erp-mfg-service/.../dashboard/`）、`ErpQaConfigs`/`ErpMfgConfigs` reader（默认值 config 键，对齐 1606-1 `erp-dash.*` 范式）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无（数据源实体已物化）

- [x] `Add`：`ErpQaDashboardBizModel.getSpcControlChartData(ctx, @Name("chartId") Long chartId)` `@BizQuery` —— 经 `IOrmTemplate`/`IDaoProvider` 同域只读聚合：chartId 为空时取最近一张 `ErpQaSpcChart`（`erp-dash.qa-spc-default-chart-id` 覆盖）；返回结构化 DTO（chartType + cl/ucl/lcl（cl=chart.cl，三控制限由 `SpcControlLimitCalculator` 已持久化于 chart 实体）+ 样本点 List<{subgroupNo, mean, isOutOfControl, violatedRules}> 按 subgroupNo 升序）。空数据返回零值结构非 `null`。镜像 1100-3 `getSpcOutOfControlWarning` 范式。配套新增 `ErpQaConfigs.isDashQaSpcDefaultChartId()` reader（默认 null）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgDashboardBizModel.getCrpLoadChartData(ctx, @Name("workcenterId") Long workcenterId, @Name("dateFrom") String dateFrom, @Name("dateTo") String dateTo)` `@BizQuery` —— **委派** `CrpLoadCalculator.getLoadReport(periodFrom, periodTo, workcenterIds)`（复用既有产能/负荷率派生链，`CrpLoadCalculator.java:137`）后按 `loadDate` 聚合返回的 `CrpLoadReportItem`（Σ loadHours / Σ capacityHours / 派生 loadRate），返回结构化 DTO（List<{loadDate, loadHours, capacityHours, loadRate}> 按 loadDate 升序 + 汇总负荷率）。dateFrom/dateTo 为空时取近 N 天（`erp-dash.mfg-crp-default-days`，默认 7，经新增 `ErpMfgConfigs.getDashMfgCrpDefaultDays()` reader）；宽容日期解析（空串→null，对齐 1321-3 范式）。空数据零值结构非 `null`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 两 `@BizQuery` 返回结构化非空结果（空数据返回零值结构，非 `return null`）；控制限 ucl/lcl/cl 经 chart 实体字段真实传递。
- [x] `mvn compile -pl module-quality/erp-qa-service -am` + `mvn compile -pl module-manufacturing/erp-mfg-service -am` 通过（解除 Phase 2 前端消费阻塞的本地化检查）。

---

### Phase 2 - AMIS echarts 图表接入

Status: completed
Targets: `module-quality/erp-qa-web`/`module-manufacturing/erp-mfg-web` 下看板 `main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（@BizQuery 就绪后前端才能消费）

- [x] `Add`：quality 看板 `main.page.yaml` 补 SPC 控制图（`type: chart` + `api.dataType: raw` + `adaptor` 转 echarts option：line 样本均值序列（xAxis subgroupNo）+ `markLine` UCL/LCL/CL 三水平线 + `visualMap` 或 series `itemStyle` 将 `isOutOfControl=true` 点高亮为红色；对齐既有 trendChart/defectChart 范式 + `${'$'}chartId` 参数化 raw GraphQL 转义）。补入顶部 refresh target 序列。
  - Skill: `nop-frontend-dev`
- [x] `Add`：mfg 看板 `main.page.yaml` 补 CRP 负荷/产能对比图（bar loadHours + line capacityHours 双 yAxis + 负荷率 tooltip；`${'$'}dateFrom`/`${'$'}dateTo`/`${'$'}workcenterId` 参数化 raw GraphQL 转义）。补入顶部 refresh target 序列。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 两 page.yaml YAML 可解析（`yaml.safe_load`）；图表消费方法名与后端 `@BizQuery` 真实方法名逐一核对一致；`${'$'}` 转义对齐既有 trendChart 范式（避免 1249-2 `$var` 损坏缺陷回归）。

---

### Phase 3 - 行为测试 + Deferred RELEASED + 日志

Status: completed
Targets: `module-quality/erp-qa-service/src/test/.../TestErpQaDashboardSpcChart.java`、`module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgDashboardCrpChart.java`、`docs/plans/2026-07-07-1100-3-*.md`/`2026-07-09-0628-1-*.md`/`2026-07-09-1145-2-*.md` Deferred 段、`docs/logs/2026/07-17.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestErpQaDashboardSpcChart`（集成测试）：空数据零值结构、多样本序列按 subgroupNo 升序、ucl/lcl/cl 经 chart 字段传递、isOutOfControl 标记透传、chartId 入参过滤 + 默认最近一张。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpMfgDashboardCrpChart`（集成测试）：空数据零值、日期区间过滤、workcenterId 过滤、近 N 天默认、负荷率派生。
  - Skill: `nop-testing`
- [x] `Add`：1100-3/0628-1/1145-2 三处 Deferred 段各补 `**RELEASED by 2026-07-17-2010-1**` 行（触发条件已满足 + 本计划交付证据）；`docs/logs/2026/07-17.md` 增聚合条目。
  - Skill: none
- [x] `Proof`：`mvn test -pl module-quality/erp-qa-service -am` + `mvn test -pl module-manufacturing/erp-mfg-service -am`（含本期新增 + 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 新增行为测试全绿；qa-service/mfg-service 既有测试无回归。
- [x] 三处 Deferred RELEASED 登记落地 + 日志条目在位。

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_090007a0bffe0Ndzd03KC2Qe5b`，新会话冷重播无执行者/起草者上下文，2026-07-17) — 0 Blocker 之外发现：**B1** CRP 数据源基线失实（`ErpMfgCrpLoad` 仅物化 `loadHours`/`setupHours`，无 `capacityHours`/`loadRate` 列；须经 `CrpLoadCalculator.getLoadReport()` 派生入 `CrpLoadReportItem`），经实时 ORM/Java 核实确认，已重写 CRP 基线 + Goal + Phase 1 item 改为委派 `getLoadReport` 后按 loadDate 聚合 `CrpLoadReportItem`；**M1** Phase 1 Targets「（可选）config reader」违反反松弛 `optional`，已将 reader 提升为明确 Phase 1 `Add` item + Infra 去除「可选」措辞；**m1** mfg 看板图表名 `trend/defect` → `statusChart/trendChart`；**m2** 删除 `isOutOfControl` 未核实的「索引就绪」。SPC 数据源/trend-defect AMIS 范式/`${'$'}` 转义/R14 SPC+CRP 同组件 bundling/三处 Deferred 触发条件已满足均 PASS。范围裁决 legitimately warranted（消费三处真实触发 successor，非 padding）。
- Independent draft review iteration 2: needs revision (`ses_08ff94049ffeuhgf7xvE823S3y`，新会话冷重播，2026-07-17) — iteration-1 四项全部 RESOLVED（CRP 委派 getLoadReport 签名匹配 :137、config reader 已定化、mfg 图表名 statusChart/trendChart、索引就绪已删）。但 R1 复核 SPC 基线发现**新 B1**：`ErpQaSpcChart` 无 `targetValue`/`grandMean` 列（前者属 `ErpQaCalibration`、后者属 `ErpQaSpcCapability`），中心线实为专用 `cl` 列（propId 18，`SpcControlLimitCalculator.java:141 chart.setCl` 持久化，与已引 :142-143 setUcl/setLcl 同源）。已重写 baseline 第 22 行 + Goal + Phase 1 item：CL 取 `chart.cl`，并注明 targetValue/grandMean 不在 `@BizQuery` 范围（仅 ErpQaSpcChart + ErpQaSpcSample）。其余（CRP R1、`${'$'}`、refresh-target 序列、item 类型、per-item Skill、Deferred 触发、Closure Gates、反松弛）均 clean。
- Independent draft review iteration 3: **accept** (`ses_08ff2eac5ffeDt2gfrTuI9xTze`，新会话冷重播无执行者/起草者上下文，2026-07-17) — iteration-2 新 B1（SPC `cl` 源）三处修复点经实时仓库核实全部正确：baseline l.22 命名 `cl`/`ucl`/`lcl` 于 `ErpQaSpcChart`（ORM :759-817，cl propId 18，无 targetValue/grandMean 列）+ 注明 targetValue 属 ErpQaCalibration/grandMean 属 ErpQaSpcCapability 出 `@BizQuery` 范围；Goal l.42 + Phase 1 item l.80 均 `cl=chart.cl`，对齐 `SpcControlLimitCalculator.java:141 chart.setCl`。最终 R1 双 phase 扫描 clean：SPC（cl/ucl/lcl/chartType + subgroupNo/mean/isOutOfControl/violatedRules 均在命名实体）+ CRP（ErpMfgCrpLoad 仅 loadHours/setupHours，getLoadReport :137 → CrpLoadReportItem setCapacityHours/setLoadRate 运行时派生）。Baseline↔Goal↔Phase 1↔Phase 3 测试↔Closure Gates 无矛盾；验证命令正确分层（Closure 级全量 + Phase 1/2 本地化解阻塞）。无剩余 Blocker/Major/Minor。**共识达成 → Plan Status 置 `active`。**

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。本计划为后端 @BizQuery + AMIS 图表（应用层 + 前端结果面），运行时行为须经验证。

- [x] 范围内行为完成（SPC 控制图 @BizQuery + AMIS echarts 图表 + CRP 负荷图 @BizQuery + AMIS echarts 图表）
- [x] 相关文档对齐（1100-3/0628-1/1145-2 Deferred RELEASED、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-quality/erp-qa-service -am` + `mvn test -pl module-manufacturing/erp-mfg-service -am`（0 failures / 0 errors）+ 两 page.yaml YAML 可解析
- [x] 无范围内项目降级为 deferred/follow-up（计数型控制图/甘特视图/实时推送均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SPC 计数型控制图渲染（P/NP/C/U）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0305-2 裁定 SPC 计算引擎仅实现计量型（X_BAR_R/X_BAR_S/X_MR），计数型聚合算法未落地；本计划控制图仅渲染计量型样本均值。计数型渲染依赖计数型计算引擎先落地。
- Successor Required: `yes`（触发条件：计数型 SPC 计算引擎落地时）

### CRP 工作中心甘特视图 / 精确费率拆分

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期交付负荷 vs 产能对比图（bar+line 双轴）；甘特（按工作中心日历时段排布）+ 工作中心级精确 laborRate/overheadRate 拆分（0455-2 既有 successor，须 ask-first ORM）属不同结果面。
- Successor Required: `yes`（触发条件：CRP 甘特可视化 / 工作中心级精确费率业务需求落地时）

## Closure

Status Note: 执行完成（2026-07-17，主代理 3 阶段全绿，独立结束审计 PASS）。范围内行为全部落地：(1) `ErpQaDashboardBizModel.getSpcControlChartData` `@BizQuery`（同域只读聚合 `ErpQaSpcChart` cl/ucl/lcl/chartType + `ErpQaSpcSample` subgroupNo/mean/isOutOfControl/violatedRules 按 subgroupNo 升序；chartId 解析：入参 > config `erp-dash.qa-spc-default-chart-id` > 最近一张 id 降序；空数据零值结构非 null）；(2) `ErpMfgDashboardBizModel.getCrpLoadChartData` `@BizQuery`（委派 `CrpLoadCalculator.getLoadReport` 复用既有产能/负荷率派生链后按 loadDate 聚合 Σ loadHours / Σ capacityHours / 派生 loadRate + overallLoadRate；dateFrom/dateTo 空时取近 N 天 `erp-dash.mfg-crp-default-days`=7；空数据零值结构非 null）；(3) 两 AMIS echarts 图表（quality `spcControlChart` line+markLine UCL/LCL/CL+失控点红色高亮+tooltip violatedRules；mfg `crpLoadChart` bar 负荷 + line 产能 + loadRate tooltip + overallLoadRate 标题）；(4) `${'$'}var` YAML 安全转义对齐 1249-2 修复范式避免 `$var` AMIS 模板损坏回归。`ErpQaConfigs` +1 reader + `ErpQaConstants` +1 配置键；`ErpMfgConfigs` 由空 interface 重写为 final helper class +1 reader + `ErpMfgConstants` +1 配置键 +1 默认值。验证全绿：根 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:34 min，2026-07-17T20:53:41+08:00）；`mvn test -pl module-quality/erp-qa-service` qa-service 全绿（含新增 `TestErpQaDashboardSpcChart` 7 cases）；`mvn test -pl module-manufacturing/erp-mfg-service` mfg-service 全绿 122 tests（含新增 `TestErpMfgDashboardCrpChart` 5 cases）；两 page.yaml `yaml.safe_load` 可解析。解除 1100-3/0628-1/1145-2 三处图表可视化 Deferred RELEASED。独立结束审计由新会话子代理（`ses_08fd885c1ffezWR1FdzpE32y9x`）执行：全部技术交付物（后端 `@BizQuery`、AMIS echarts、测试、Deferred RELEASED、日志、范围纪律、反模式）经实时仓库核实正确，0 BLOCKER / 0 MAJOR / 0 MINOR（首轮 2 MAJOR 为 Status Note 占位符 + 日志全绿措辞矛盾，执行者已补全 + 措辞对齐后转 PASS）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_08fd885c1ffezWR1FdzpE32y9x`（general，新会话冷重播无执行者上下文，逐项核实 LIVE 仓库非采信执行者自述）。VERDICT: **PASS**（首轮 FAIL 0 BLOCKER / 2 MAJOR / 0 MINOR —— Status Note 占位符 + 日志「Closure 待...」与 gate `[x]` 矛盾；执行者补全 Status Note + 对齐日志全绿措辞后转 PASS）。逐项核实：(A) 计划文本一致（3 Phase Status=completed + 全 [x] 项 + Exit Criteria 全 [x] + Closure Gates 全 [x] 除审计 gate）；(B) `ErpQaDashboardBizModel.getSpcControlChartData` 实质实现非 hollow（@BizQuery + @Optional @Name chartId + IServiceContext LAST + chart 实体 cl/ucl/lcl/chartType + Sample subgroupNo asc + 空 LinkedHashMap 零值结构 + chartId 解析三段）；(C) `ErpMfgDashboardBizModel.getCrpLoadChartData` 实质实现非 hollow（委派 `CrpLoadCalculator.getLoadReport` + loadDate 聚合 + 近 7 天默认 + 空串 parseDate 容忍 + 空结构非 null）；(D) quality 看板 `spcControlChart` echarts（markLine UCL/LCL/CL + 失控点 itemStyle 红 + tooltip violatedRules + `${'$'}chartId` 转义 + 方法名 `ErpQaDashboard__getSpcControlChartData` 对齐 + refresh target 含 spcControlChart）；(E) mfg 看板 `crpLoadChart` echarts（bar 负荷 + line 产能 + loadRate tooltip + overallLoadRate 标题 + `${'$'}`workcenterId/dateFrom/dateTo 转义 + 方法名对齐 + refresh target 含 crpLoadChart）；(F) 测试落地（TestErpQaDashboardSpcChart 7 cases ≥ 5 要求 + TestErpMfgDashboardCrpChart 5 cases ≥ 4 要求 + dao.newEntity()/orm_propValueByName 无 new Entity() + crpLoadBiz.calculateLoad 签名匹配）；(G) 三处 Deferred RELEASED 行落地（1100-3 L138 / 0628-1 L175 / 1145-2 L163 含交付证据）；(H) 日志 `docs/logs/2026/07-17.md` L3 新条目在位；(I) git diff --stat 13 文件全在允许桶（2 BizModel + 2 Configs + 2 Constants + 2 page.yaml + log/backlog + 3 RELEASED markers）+ 未跟踪 2 测试 java + 2 _cases 快照 + 2 plan md，无 `_gen/`/`.orm.xml`/`.xbiz.xml`/`.api.xml` 触动（grep exit=1）；(J) 反模式 clean（@Inject 非 private ✓ + IServiceContext LAST ✓ + findAllByQuery/3-arg findList ✓ + dao.newEntity() ✓ + CoreMetrics 时间源 ✓ + 无新异常类型）。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
