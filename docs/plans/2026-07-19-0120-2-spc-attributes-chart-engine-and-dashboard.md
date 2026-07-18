# 2026-07-19-0120-2-spc-attributes-chart-engine-and-dashboard SPC 计数型（P/NP/C/U）控制图计算引擎 + 看板 echarts 接入

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Mission: erp
> Work Item: 看板运行时视觉/业务深化（SPC 计数型控制图后端实现 + 看板 echarts 接入）
> Source: `docs/plans/2026-07-17-2010-1-dashboard-echarts-spc-crp-charts.md` Deferred But Adjudicated「计数型 SPC 计算引擎」(l.155-161) — Successor Required: yes，触发条件「计数型 SPC 计算引擎落地时」本计划预期满足该 successor 触发条件；结束审计裁决（iter-1 审查 m2 建议措辞，避免起草者自我授权）：枚举/字典/owner doc 设计均已就绪（`spc.md §ErpQaSpcChart.chartType` 含 P/NP/C/U，**dict yaml 实际 valueType=string**——iter-1 审查核实），后端 `SpcControlLimitCalculator` / `SpcCapabilityCalculator` / `SpcSamplingService` / `ErpQaDashboardBizModel.getSpcControlChartData` 当前仅处理计量型（X_BAR_R/X_BAR_S/X_MR），计数型分支完全缺失。
> Related: `2026-07-17-2010-1`（看板 echarts SPC 控制图渲染层落地，已 completed；本计划承接其后端 calc 引擎 successor）、`2026-07-07-0305-2-quality-spc-process-control`（SPC 计量型引擎首落地，已 completed）、`docs/design/quality/spc.md`（同 owner doc §chartType 已设计 P/NP/C/U）、`docs/design/quality/inspection-integration.md`（SPC 数据源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-19），SPC 后端计算引擎与看板 echarts 渲染层在**计量型**路径已完整，**计数型**分支完全缺失：

### 计量型已落地（参考实现，本计划不重做）

- **`SpcControlLimitCalculator.recalculate`**（`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcControlLimitCalculator.java:36-147`）：仅按 X̄̄-R 范式计算（grandMean = mean(sample.mean)，sigmaHat = R̄/d2，UCL/LCL = cl ± 3σ̂），**不分支 chartType**——对 P/NP/C/U chartType 输入会产出错误结果（mean=缺陷率可能，但 sigma 估计用 R̄/d2 不适用二项/泊松分布）。
- **`SpcCapabilityCalculator.calculateCapability`**（`SpcCapabilityCalculator.java:49-151`）：仅按计量型公式（Cp=(USL−LSL)/6σ̂̂，Cpk=min((USL−X̄̄),(X̄̄−LSL))/3σ̂），**计数型无对应能力指数**（行业惯例计数型用 Ppk/DPMO/sigma level，不直接产 Cp/Cpk）。
- **`SpcSamplingService.collectSamples`**（`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcSamplingService.java`）：聚合 `ErpQaInspectionLine.measuredValue` 为连续值 JSON 数组——**计数型需要的 defectCount/inspectedCount 聚合完全缺失**。
- **`ErpQaDashboardBizModel.getSpcControlChartData`**（`module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java:228-280`，2010-1 落地）：顶层响应**已含 `chartType` 字段**（`:269`，iter-1 审查 B3 核实，本计划不重复添加），per-sample 行返回 `subgroupNo/mean/isOutOfControl/violatedRules`（`:308-313`）——**计数型需要 defectRate/defectCount/inspectedCount 字段未在 per-sample 响应中**。
- **看板 echarts 渲染**（2010-1 落地）：`erp-qa-web/_vfs/.../pages/dashboard/page.yaml` 的 `spcControlChart` 配置为 line+markLine 渲染 sample.mean + UCL/LCL/CL——**计数型（P 图等）需要 defectRate 纵轴语义**（前端响应字段未含 defectRate 时图表对计数型数据语义错误）。

### 计数型设计已就绪（**iter-1 审查核实：dict valueType=string 非 numeric**）

- **ORM + dict yaml**：`ErpQaSpcChart.chartType` 字段绑 dict `erp-qa/spc-chart-type`（`module-quality/model/app-erp-quality.orm.xml:123` 声明 `valueType="string"`）；dict yaml 文件（`module-quality/erp-qa-meta/src/main/resources/_vfs/dict/erp-qa/spc-chart-type.dict.yaml:8-34`，iter-2 审查 MINOR-2 修正路径段 `_vfs/dict/` 非 `_vfs/dicts/`）使用**字符串枚举值**：`X_BAR_R`/`X_BAR_S`/`X_BAR_MR`/`P`/`NP`/`C`/`U`（**非数字编码**——iter-1 审查 B1 核实）。
- **`ErpQaConstants` 常量已存在**（`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/ErpQaConstants.java:140-143`）：`SPC_CHART_TYPE_X_BAR_R="X_BAR_R" / X_BAR_S="X_BAR_S" / X_MR="X_MR" / P="P" / NP="NP" / C="C" / U="U"`——本计划**仅消费既有常量，不新增**（iter-1 审查 B2 核实；Phase 2 删除「新增常量」item）。
- **owner doc 漂移（已识别，本计划 Phase 4 修正）**：`docs/design/quality/spc.md §ErpQaSpcChart.chartType` l.22 声明 `X_BAR_R=10/X_BAR_S=20/X_MR=30/P=40/NP=50/C=60/U=70`（数字编码）——**与 live dict yaml 字符串值不一致**，owner doc 自身陈旧（iter-1 审查 MAJOR 1 核实）。
- **种子基线**：`erp_qa_spc_chart.csv` 当前 1 行（chartType=`X_BAR_R`，calcStatus=`PENDING`，**非** CALCULATED——iter-1 审查 MINOR 1 核实）；`erp_qa_spc_sample.csv` 同样仅有计量型样本。计数型种子完全缺失——本计划建立「4 chartType × ≥20 子组」新种子范式（与计量型种子同密度，非「对齐既有 CALCULATED 范式」措辞——iter-1 审查 MINOR 1 修正）。

### `ErpQaSpcSample` 字段缺口（本计划唯一 ORM 变更）

经实时仓库核实 `ErpQaSpcSample` 当前字段（`module-quality/model/app-erp-quality.orm.xml` + spc.md §ErpQaSpcSample l.38-53）：
- `measuredValues`（JSON 数组，计量型子组 n 个实测值）；
- `mean/range/stdDev`（计量型派生字段）；
- 无 `defectCount` / `inspectedCount` / `opportunityCount` 字段——计数型子组数据无处存放。

**裁决：加性追加 2 列** `defectCount Integer / inspectedCount Integer`（均 nullable 向后兼容计量型，计量型 chart 保持 null 不影响既有 calc）。

> ORM 加性追加属 ask-first 保护区域（`AGENTS.md §Nop Platform 特定规则`），本计划声明此变更并给出理由：纯加性（无既有列变更/无删除/无类型变更）+ 向后兼容（计量型 chart 不受影响）+ 字段精度明确（Integer 满足 SPC 子组样本量级别，对齐 inspection 检验数量级）+ 无 DDL 迁移（Quarkus dev mode `ddl-auto=update` 自动加列）。`module-quality/model/app-erp-quality.orm.xml` 是源模型；codegen 后阶段产物跟随。

### 剩余差距

- 计数型控制限公式（P/NP/C/U）后端 calc 引擎分支缺失。
- 计数型采样聚合（defectCount/inspectedCount 从 inspection 数据派生）缺失。
- 计数型看板响应字段（defectRate/defectCount）缺失。
- 计数型 echarts 渲染语义（纵轴 defectRate）缺失。
- 计数型种子（4 chartType × 各 1 chart + 子组样本）缺失。
- `ErpQaSpcSample` ORM 加性 2 列（保护区域 ask-first）。

## Goals

- `SpcControlLimitCalculator.recalculate` 按 chartType 分支：计量型保持既有 X̄̄-R 范式（chartType 字符串值 `X_BAR_R`/`X_BAR_S`/`X_MR`）；新增计数型公式分支（chartType=`P`: CL=p̄, UCL/LCL=p̄±3√(p̄(1-p̄)/n) / chartType=`NP`: CL=n·p̄, UCL/LCL=n·p̄±3√(n·p̄(1-p̄)) / chartType=`C`: CL=c̄, UCL/LCL=c̄±3√c̄ / chartType=`U`: CL=ū, UCL/LCL=ū±3√(ū/n)）——**消费既有 `ErpQaConstants.SPC_CHART_TYPE_*` 字符串常量，不新增常量**（iter-1 审查 B2）。
- `SpcSamplingService.collectSamples` 按 chartType 分支：计量型保持既有 measuredValues 聚合；新增计数型 defectCount/inspectedCount 聚合（P/NP 从 inspection PASS/FAIL 计数；C/U 从 defect log 计数）。
- `SpcCapabilityCalculator.calculateCapability` 按 chartType 分支：计量型保持既有 Cp/Cpk；计数型降级为仅算 overall mean/σ + DPMO 转换（行业惯例计数型不产 Cp/Cpk，本计划保守不发明新指数）。
- `ErpQaDashboardBizModel.getSpcControlChartData` per-sample 响应**加性补字段 `defectRate / defectCount / inspectedCount`**（计数型分支；计量型字段不变）——**`chartType` 字段已在顶层响应，不重复添加**（iter-1 审查 B3 核实 `ErpQaDashboardBizModel.java:269`）。
- 看板 echarts `spcControlChart` 配置按 chartType 分支渲染：计量型 line + markLine（既有）；计数型 line（defectRate 纵轴）+ markLine + tooltip 显示 defectCount/inspectedCount。
- `ErpQaSpcSample` ORM 加性追加 `defectCount / inspectedCount` 两 nullable 列（保护区域 ask-first）。
- 种子：4 chartType × 各 1 chart + 子组样本（≥20 子组使 calcStatus→CALCULATED，与计量型种子同密度新范式）。
- owner-doc 漂移修复：`spc.md §ErpQaSpcChart.chartType` l.22 数字编码标注修正为字符串值（iter-1 审查 MAJOR 1）。
- 解除 2010-1 Deferred「计数型 SPC 计算引擎」（补 `**RELEASED by 2026-07-19-0120-2**`）。

## Non-Goals

- **不改 chartType dict**（P/NP/C/U 已存在，仅消费）。
- **不动计量型路径**——X_BAR_R/X_BAR_S/X_MR 既有 calc/采样/看板/测试一律不动（向后兼容硬约束）。
- **不发明计数型能力指数**——Cp/Cpk 仅适用计量型连续数据；计数型行业惯例用 DPMO/sigma level，本计划保守仅降级（overall mean/σ）不发明新指数，cap.capabilityLevel 计数型置 null 或 INADEQUATE-by-default（待 Phase 1 Decision）。
- **不实现 WesternElectric 判异规则计数型适配**——既有判异规则（spc.md §关键流程 2）针对计量型连续值；计数型失控判定通常仅「点出控制限」，本计划保守仅保留 UCL/LCL 越界判定（既有 `isOutOfControl` 计算）。
- **不实现 np-chart sample size 自适应**——子组大小固定（chart.subgroupSize），不按 inspectedCount 动态调整。
- **不做 SPC 实时告警**（nop-hook 或 streaming）——既有定时任务（`erp-qa-spc-sampling` / `erp-qa-spc-capability`）经 1600-1 迁移到 `.job.yaml`，本计划不改调度。
- **不做计数型 echarts 高级图表类型**（如 p 图专属的 100% 上限刻度自适应）——保守 line+markLine 既有渲染语义，纵轴刻度由 echarts 自动派生。
- **不做 SPC 多变量 / 短跑 (short-run) / CUSUM / EWMA**——owner doc 未设计，归独立 successor。

## Task Route

- Type: `implementation-only change`（含 ORM 加性列保护区域 ask-first + 看板 AMIS 渲染层 + 种子追加 + owner-doc 实现注记）
- Owner Docs: `docs/design/quality/spc.md`（§chartType l.22 / §关键流程 2 l.81 / §ErpQaSpcSample l.38-53）、`docs/design/quality/inspection-integration.md`（SPC 数据源）
- Skill Selection Basis: 后端 BizModel + Calculator 分支 + Sampling 聚合扩展 → `nop-backend-dev` skill（I*Biz injection + Calc 引擎模式）；看板 echarts AMIS page.yaml 修改 → `nop-frontend-dev` skill（XView/AMIS 渲染层 + chartType 分支）；ORM 加性追加 ask-first → 触发保护区域授权（参考 2026-07-08-0056-1 范式 `mission-driver 显式指令授权`）
- Protected Areas: ORM 加性 2 列（`module-quality/model/app-erp-quality.orm.xml`）；任何契约/字典/xbiz 变更均不动；种子 CSV 加性追加。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 qa-web dashboard page.yaml + echarts dependency。
- 无新增端口/环境变量/密钥/外部服务。
- webServer JVM args 既有 `erp-dash.qa-spc-default-chart-id` 不动；计数型 chartId 经入参或新增 config `erp-dash.qa-spc-default-attributes-chart-id` 解析（Phase 1 Decision 裁决）。

## Execution Plan

### Phase 1 — Decision：能力指数计数型策略 + 数据派生路径

Status: completed
Targets: 探索笔记（落计划不落仓库除非裁定须文档化）
Skill: `none`

- Item Types: `Decision | Proof`
- Prereqs: none

- [x] `Proof`：实时仓库冷核实计数型数据源——`ErpQaInspectionLine` 是否有 `result`（PASS/FAIL）/ `defectCount` 字段或类似可派生缺陷数；`ErpQaNonConformance` 是否在 inspection 阶段已挂链可作 C/U defects 来源；现有 inspection status/values 字段语义。
  - Skill: `nop-debugging`
  - **执行结果**：经核实 `module-quality/model/app-erp-quality.orm.xml:262` 中 `ErpQaInspectionLine.result` 字段（ext:dict `erp-qa/inspection-result` mandatory）含 `PENDING/ACCEPTED/CONDITIONAL/REJECTED` 四态——REJECTED 行可作 **defective 单品**计数，inspection line 行数（每行=1 单品）作 inspectedCount；`ErpQaNonConformance` (`:354-388`) 经 `inspectionId` 反向挂链 inspection + `quantity` 字段（DECIMAL 不合格数量）+ `sourceType`（自由字符串，常量 `NCR_SOURCE_TYPE_INSPECTION="INSPECTION"` 见 `ErpQaConstants.java:54`）可作 C/U defects 数；`ErpQaInspection.sampleQuantity`(`:191`) DECIMAL 是抽样数（每个 inspection 一次抽样，可作 inspectedCount 备选）。裁决：**P/NP 走 inspection line REJECTED 计数 / line 总数作 inspectedCount**；**C/U 走 NCR.quantity 累计 / inspection 数作 inspectedCount**；二者数据源均生产路径可达，不降级手工录入。
- [x] `Decision`：计数型能力指数策略——三选一裁定：(a) 计数型完全不算能力指数（cap.cp/cpk/pp/ppk 全 null，capabilityLevel=null）；(b) 计数型仅算 DPMO（defects per million opportunities）+ 不映射 capabilityLevel；(c) 计数型 DPMO → sigma level 转换 + capabilityLevel 按 sigma 阈值分档（如 <3σ INADEQUATE）。**默认倾向 (a)**：保守不发明新指数，对齐 Non-Goal；owner doc 未设计计数型能力指数，强行算会误导用户。
  - Skill: none
  - **裁定 (a)**：计数型 `SpcCapabilityCalculator.calculateCapability` 分支仅算 grandMean（=defectRate/100 派生为 0~1 区间）+ overallStdDev（defectRate 序列的总体 σ），cp/cpk/pp/ppk/cpm 全置 null + capabilityLevel=null + isStable=既有判异逻辑（点出控制限）。残留风险：未来产品要求 DPMO/sigma level 落地为显式 successor（见 Deferred 段「计数型能力指数」）。
- [x] `Decision`：计数型采样数据派生路径——P/NP 与 C/U 数据源不同：(a) P/NP 从 `ErpQaInspectionLine` 按 `result=FAIL` 计数 defective 单品 + `inspectedCount`=子组内 inspection line 数；(b) C/U 从 `ErpQaNonConformance` 按 sourceBillType=INSPECTION 反查 defects 数 + `opportunityCount`=子组内 inspection 数。**裁决**：Phase 1 Explore 经实时仓库核实后定稿，**默认 P/NP 走 inspection line PASS/FAIL 段、C/U 走 NCR 段**；若数据源不可达则降级 (c) 经手工录入（chart 关闭 sampling 自动化，用户手工 `__save` ErpQaSpcSample）。
  - Skill: none
  - **裁定**：P/NP 从 `ErpQaInspectionLine` 按 `result=REJECTED` 计数 defectCount + line 总数作 inspectedCount；C/U 从 `ErpQaNonConformance.quantity` 累计 defectCount + 同子组 inspection 数作 inspectedCount（每个 inspection 视作 1 单位 opportunity）。子组聚合规则：按 `chart.subgroupSize`（语义改为"子组内 inspection 数"）切分 APPROVED inspection 集合，每子组 1 sample。残留风险：NCR 缺失（inspection REJECTED 但 NCR 未生成）→ C/U defectCount=0 容忍；chart.subgroupSize 语义复用不新增 opportunityCount 列（C/U 多机会维度归 Deferred）。
- [x] `Decision`：看板 chartId 解析——计数型 chartId 默认解析路径：(a) 新增 config `erp-dash.qa-spc-default-attributes-chart-id` 与既有 `qa-spc-default-chart-id` 并列；(b) 复用既有 config 但默认指向计数型 chart（破坏计量型看板，否决）；(c) 看板前端按 chartType 切换（前端 amIS selectOn 事件）。**默认倾向 (a)**：后端无侵入，前端按 chartType 切换 chartId。
  - Skill: none
  - **裁定 (a)**：新增 config `CONFIG_DASH_QA_SPC_DEFAULT_ATTRIBUTES_CHART_ID = "erp-dash.qa-spc-default-attributes-chart-id"`（`ErpQaConstants.java` 加常量 + `ErpQaConfigs.java` 加 reader `getDashQaSpcDefaultAttributesChartId()`）；`ErpQaDashboardBizModel.getSpcControlChartData` chartId 解析增 attributes-chart-id fallback（入参 > qa-spc-default-chart-id > qa-spc-default-attributes-chart-id > 最近一张 ErpQaSpcChart by id desc）。残留风险：前端 page.yaml `spcControlChart` 不自动按 chartType 切换 chartId，由调用方控制（前端可经 chartId 入参或 config 注入）。
- [x] `Decision`：ORM 加性列 `defectCount/inspectedCount` 类型裁决——`Integer` vs `Long`：SPC 子组样本量级别（通常 ≤1000/unit）`Integer` 足够；C/U 图 opportunity 维度（如面积/长度）可能大数但 SPC 惯例仍用 Integer。**裁决 Integer**（对齐 `subgroupSize Integer` 范式 + Quarkus dev DDL 自动加列）。
  - Skill: none
  - **裁定 Integer**：`defect_count Integer precision=10 nullable=true` + `inspected_count Integer precision=10 nullable=true`（propId=23/24 接既 22 列后）。precision=10 满足子组样本量级（最大 ~2.1B），对齐 `ErpQaSpcChart.subgroupSize Integer`(`:773`)、`ErpQaInspection.sampleQuantity DECIMAL`(`:191`) 范式。残留风险：opportunity 大数（如面积/长度）超 Integer 范围归 C/U Deferred「opportunityCount 多机会维度」successor。

Exit Criteria:

- [x] 四 Decision + 一 Proof 落记录（含替代方案 + 残留风险 + 行号引用）
- [x] ORM 加性列类型裁决（默认 Integer）
- [x] 计数型采样数据派生路径裁决（默认 P/NP inspection line / C/U NCR，否则降级手工录入）

---

### Phase 2 — ORM 加性追加 + Calculator/Sampling 后端分支

Status: completed
Targets:
  - `module-quality/model/app-erp-quality.orm.xml`（ErpQaSpcSample 加性 2 列；源模型唯一真相）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcControlLimitCalculator.java`（chartType 分支 + P/NP/C/U 公式）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcSamplingService.java`（chartType 分支 + defectCount/inspectedCount 聚合）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcCapabilityCalculator.java`（chartType 分支 + 计数型降级）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/AttributesControlLimitFormulas.java`（新建：P/NP/C/U 公式 POJO）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java`（per-sample 响应加性 defectRate/defectCount/inspectedCount）
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/ErpQaConfigs.java`（**条件性**：仅当 Phase 1 Decision (a) 裁定新增 config `erp-dash.qa-spc-default-attributes-chart-id` 时——iter-1 审查 MAJOR 3）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Add`：ORM 加性追加——`module-quality/model/app-erp-quality.orm.xml` 中 `ErpQaSpcSample` 实体新增 `<column name="defect_count" code="defectCount" type="Integer" precision="10" nullable="true"/>` + `<column name="inspected_count" code="inspectedCount" type="Integer" precision="10" nullable="true"/>`（向后兼容：nullable，计量型 chart 不受影响）。codegen 链触发 `mvn clean install -DskipTests` 增量重新生成。
  - Skill: `nop-backend-dev`
  - **执行结果**：`app-erp-quality.orm.xml` 加 2 列（propId=23/24），`mvn install -pl erp-qa-codegen,erp-qa-dao -am` 触发增量生成，`_ErpQaSpcSample.java` 含 getDefectCount/setDefectCount/getInspectedCount/setInspectedCount（propId 23/24 验证通过）。
- [x] `Add`：`AttributesControlLimitFormulas` POJO 新建——4 静态方法 `calcP / calcNp / calcC / calcU(defects, inspected, opportunities)` 返回 `(cl, ucl, lcl)` 三元组；公式严格按行业标准（P: cl=p̄=Σdi/Σni, ucl=cl+3√(cl(1-cl)/n̄), lcl=cl-3√(...); NP: cl=n·p̄, ucl/lcl=cl±3√(n·cl(1-cl)); C: cl=c̄=Σci/k, ucl/lcl=cl±3√cl; U: cl=ū=Σci/Σni, ucl/lcl=cl±3√(cl/n̄)）+ BigDecimal HALF_UP scale=6（对齐 `SpcControlLimitCalculator.scale()` 范式）+ 负数下限钳到 0（CL−3σ < 0 时 lcl=0，行业标准）。
  - Skill: `nop-backend-dev`
  - **执行结果**：新建 `AttributesControlLimitFormulas.java`（210 行）含 4 静态方法 + 内部 `Result` 三元组 POJO + `BigDecimal.ZERO` 负数下限钳 0 + HALF_UP scale=6 + sqrt 经 `Math.sqrt(double)`。
- [x] `Add`：`SpcControlLimitCalculator.recalculate` chartType 分支——读 `chart.getChartType()`（**字符串值**，对齐 dict yaml valueType=string），计量型（`X_BAR_R`/`X_BAR_S`/`X_MR` 经 `ErpQaConstants.SPC_CHART_TYPE_*` 常量匹配）走既有路径；计数型（`P`/`NP`/`C`/`U`）调 `AttributesControlLimitFormulas` 对应方法（P/NP 用 ΣdefectCount/ΣinspectedCount；C 用 mean(defectCount)；U 用 ΣdefectCount/ΣinspectedCount）；cl/ucl/lcl 写回 chart；calcStatus→CALCULATED；样本数 < 20 时保持 PENDING（既有守卫不动）。
  - Skill: `nop-backend-dev`
  - **执行结果**：`recalculate` 重构为 chartType 分支（chartType==null 走计量型以向后兼容），新建 `isAttributesChart(String)` public static helper + `calculateAttributes(String, List<samples>)` private 派发 4 chartType；守卫 `< 20 samples return false` 不变；既有计量型 X̄̄-R/d2/D3/D4 路径完整保留。
- [x] `Add`：`SpcSamplingService.collectSamples` chartType 分支——计量型走既有 measuredValues 聚合；计数型经 Phase 1 Decision 数据派生路径（默认 P/NP 从 inspection line PASS/FAIL 聚合 defectCount=FAIL 数 / inspectedCount=total；C/U 从 NCR 反查 defects 数）写入 ErpQaSpcSample.defectCount/inspectedCount 字段。
  - Skill: `nop-backend-dev`
  - **执行结果**：`collectSamples` 入口增 `isAttributesChart` 分支派发；新增 `collectAttributesSamples(chart, parameterId, subgroupSize)` 方法（P/NP 从 inspection line REJECTED 计数 + line 总数作 inspectedCount；C/U 从 NCR.quantity 累计 + inspection 数作 inspectedCount）；每子组 1 sample（按 APPROVED inspection 切分，chart.subgroupSize 语义改为子组内 inspection 数）；新增 `loadNcrsByInspectionId` + `buildAttributesSampledKeys` helpers；幂等键 `ATTR#<firstInspectionCode>`；mean 字段语义保留为 defectRate 供既有 echarts line 渲染兼容。
- [x] `Add`：`SpcCapabilityCalculator.calculateCapability` chartType 分支——计量型走既有 Cp/Cpk 全套；计数型按 Phase 1 Decision（默认 (a) 完全不算能力指数：cap.cp/cpk/pp/ppk/cpm 全 null，capabilityLevel=null，grandMean/overallStdDev 仅算 defectRate 的 mean/σ 供参考）。
  - Skill: `nop-backend-dev`
  - **执行结果**：`calculateCapability` 重构为 chartType 分支，计数型仅算 defectRate 序列 mean/σ + totalObservations = Σ inspectedCount；cp/cpk/pp/ppk/cpm/capabilityLevel 全 null；INADEQUATE 回写守卫 `if (!isAttributes && ...)` 计数型跳过；既有计量型 Cp/Cpk/Pp/Ppk/Cpm + classifyByCpk + QualityGoal/RiskRegister 回写链完整保留。
- [x] `Add`：`ErpQaDashboardBizModel.getSpcControlChartData` per-sample 响应加性补字段——计数型分支 per-sample Map 增 `defectRate / defectCount / inspectedCount`；计量型字段不变（measuredValues/mean/range 等保留）；顶层 `chartType` 已存在（`:269`）**不重复添加**。
  - Skill: `nop-backend-dev`
  - **执行结果**：`loadSpcSamples` 加性补 defectCount/inspectedCount/defectRate 三字段（仅当 sample.defectCount/inspectedCount 非 null 时填入，计量型样本此三字段透明不破坏）；chartId 解析新增 `qa-spc-default-attributes-chart-id` fallback（入参 > qa-spc-default-chart-id > qa-spc-default-attributes-chart-id > 最近一张 by id desc）；顶层 chartType/cl/ucl/lcl 字段不变。
- [x] `Decision | Add`（条件性）：Phase 1 Decision (a) 裁定后——若选 (a) 新增 `erp-dash.qa-spc-default-attributes-chart-id` config，在 `ErpQaConfigs.java` 加常量声明 + `ErpQaDashboardBizModel.getSpcControlChartData` chartId 解析逻辑增 attributes-chart-id fallback。若选 (c) 前端切换则跳过此 item。
  - Skill: `nop-backend-dev`
  - **执行结果**：Phase 1 Decision 裁定 (a)，已落地：`ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_ATTRIBUTES_CHART_ID="erp-dash.qa-spc-default-attributes-chart-id"` 常量 + `ErpQaConfigs.getDashQaSpcDefaultAttributesChartId()` reader + BizModel 解析增 fallback。

> **iter-1 审查 B2 删除项**：原「`ErpQaConstants` 新增 SPC chartType 常量」item 删除——常量已存在（`ErpQaConstants.java:140-143`，字符串值 `"P"/"NP"/"C"/"U"`），本计划仅消费不新增。**iter-1 审查 MAJOR 2 删除项**：原「dict 值校验 helper」item 删除——Phase 1 无 Decision 裁定其契约，引入未定义 helper 违反 R9；chartType 字段经 dict 自动校验（Meta 自动 enforce dict 值域），无须应用层 helper。

Exit Criteria:

- [x] `module-quality/erp-qa-service` JUnit 编译通过 + 既有计量型测试 0 回归（关键守门：`TestErpQaSpcControlLimit` / `TestErpQaSpcCapability` / `TestErpQaSpcSampling` 全绿）
- [x] `SpcControlLimitCalculator.recalculate` 对 P/NP/C/U chartType 输入正确分支（不在错误走 X̄̄-R 路径）
- [x] `ErpQaSpcSample` 新增 2 列在 codegen 后可见（`_gen/ErpQaSpcSample.java` 含 getDefectCount/getInspectedCount）

> 注：ORM 加性列触发 codegen 增量重新生成（`mvn clean install -DskipTests`），由 gen-orm.xgen 自动处理；本计划不重跑 `nop-cli gen`。

---

### Phase 3 — JUnit + 计数型种子 + 看板 echarts 分支

Status: completed
Targets:
  - `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/spc/TestErpQaSpcAttributesControlLimit.java`（新建）
  - `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/spc/TestErpQaSpcAttributesSampling.java`（新建）
  - `app-erp-all/src/main/resources/_vfs/_init-data/erp_qa_spc_chart.csv`（加性追加 4 计数型 chart 行）
  - `app-erp-all/src/main/resources/_vfs/_init-data/erp_qa_spc_sample.csv`（加性追加计数型子组样本，每 chartType ≥20 子组使 calcStatus→CALCULATED）
  - `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml`（`spcControlChart` 配置按 chartType 分支——iter-2 审查 MINOR-1 修正路径段 `_vfs/erp/qa/pages/dashboard/main.page.yaml` 非 `_vfs/pages/dashboard/page.yaml`）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：`TestErpQaSpcAttributesControlLimit` JUnit——4 chartType 各 1 测试方法：(a) P chart 固定 defectCounts/inspectedCounts 序列算 cl/ucl/lcl 精确数值（如 defects=[2,3,1,4,...] inspected=[50]×k → cl=p̄=0.05,...）；(b) NP chart 同序列算 n·p̄ + 控制限；(c) C chart defects 序列算 c̄ + 3√c̄；(d) U chart defects/units 算 ū + 3√(ū/n)；每测试断言 cl/ucl/lcl 精确数值（HALF_UP scale=6）+ 负数下限钳 0 边界场景。
  - Skill: `nop-testing`
  - **执行结果**：7 测试方法（P/NP/C/U 各 1 + emptyList/nonNegativeLcl/sizeMismatch 边界场景 3 个），全绿；P/NP/C/U CL/UCL/LCL 精确数值匹配行业公式；负数下限钳 0 边界（P/NP/C/U 低缺陷率场景）+ 高缺陷率场景 LCL>0 不钳 0 + size mismatch 抛 IllegalArgumentException。
- [x] `Add`：`TestErpQaSpcAttributesSampling` JUnit——P/NP 路径：建测试专用 inspection line（result=FAIL×defectCount 个 + PASS×remaining 个）→ collectSamples → sample.defectCount/inspectedCount 精确数值断言；C/U 路径：建 NCR 反查 defects 数 → sample 写入；chartType 分支覆盖（计量型 chart 仍走 measuredValues 路径，0 回归）。
  - Skill: `nop-testing`
  - **执行结果**：5 测试方法：(a) P 图采样（2 inspections × 5 lines × 2 REJECTED → defectCount=4 / inspectedCount=10 + defectRate=0.4 派生 mean）；(b) C 图采样（2 inspections × 1 NCR.quantity=3+5 → defectCount=8 / inspectedCount=2）；(c) recalculate P chart 路由到 AttributesControlLimitFormulas 公式（20 子组 CL=0.2 精确数值）；(d) 计量型 chart 0 回归守门（defectCount/inspectedCount=null + measuredValues 路径）；(e) 计数型采样幂等。全绿。
- [x] `Add`：种子 erp_qa_spc_chart.csv 加性追加 4 行（每 chartType 1 chart，calcStatus=CALCULATED，subgroupSize=20，clCenterType=AUTO_FROM_DATA）；erp_qa_spc_sample.csv 加性追加 ≥80 行（4 chart × 20 subgroup，defectCount/inspectedCount 派生 cl/ucl/lcl 与 chart 字段一致；确定性数据如 inspectedCount=100/子组 + defectCount=随机 1-10）。**新范式建立**：与计量型种子（1 chart PENDING）不同密度，是「计数型 ≥20 子组使 calcStatus=CALCULATED」新基线（iter-1 审查 MINOR 1 修正措辞）。
  - Skill: none
  - **执行结果**：erp_qa_spc_chart.csv 加性 4 chart 行（id=2/3/4/5：P/NP/C/U 各 1，calcStatus=CALCULATED + cl/ucl/lcl 公式派生值精确填入：P/U CL=0.046500/0.072362，NP CL=4.65，C CL=9.5 等）；erp_qa_spc_sample.csv 加性 80 sample 行（id=101~180，每 chart 20 子组，defectCount/inspectedCount 字段非空）。
- [x] `Add`：看板 page.yaml `spcControlChart` 配置按 chartType 分支——计量型 chart（既有）：line+markLine 渲染 sample.mean + UCL/LCL/CL；计数型 chart：line 渲染 sample.defectRate（=defectCount/inspectedCount）+ markLine + tooltip 显示 defectCount/inspectedCount；chartType 经 `${ '<%= data.chartType %>' }` 条件渲染（AMIS `'${chartType === "P" || chartType === "NP" || chartType === "C" || chartType === "U"}'` 字符串值表达式——iter-1 审查 B1 核实 dict valueType=string）。**目标文件**：`module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml`（iter-2 审查 MINOR-1 核实路径）。
  - Skill: `nop-frontend-dev`
  - **执行结果**：spcControlChart adaptor 增 `isAttributes` chartType 字符串值分支；seriesData value 字段按 isAttributes 取 defectRate（s.defectRate 优先 fallback defectCount/inspectedCount 派生）或 mean（计量型）；tooltip 计数型增 defectCount/inspectedCount 显示；yAxis name 按 isAttributes 切换「缺陷率」/「均值」；title 含 chartType 透明显示。154 模块 BUILD SUCCESS 验证 page.yaml 可被 yaml.safe_load 解析。
- [x] `Proof`：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl module-quality/erp-qa-service -am` 全绿（既有计量型 0 回归 + 新增 8+ JUnit 用例）。
  - 验证命令：`mvn test -pl module-quality/erp-qa-service -am` + 154 模块 `mvn clean install -DskipTests`
  - Skill: `nop-testing`
  - **执行结果**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:31 min）；`mvn test -pl module-quality/erp-qa-service -am` 110 tests 0 failures/0 errors（含既有 6+5+5+6+7+6+7+7+9 + 新增 7+5 = 12 用例，0 回归）。

Exit Criteria:

- [x] 2 JUnit 类全绿（红绿反转证明：未实现时属性图分支抛 NPE/默认值，实现后精确数值匹配）
- [x] 种子 4 chart + ≥80 sample 行在 webServer 启动时正确加载（看板 echarts 渲染非空）
- [x] 看板 page.yaml 计量型渲染 0 回归（既有 `dashboards.snapshot.spec.ts` quality 段 + `dashboards.visual.spec.ts` quality 段 0 失败）
- [x] qa-service 模块全绿 + 154 模块 BUILD SUCCESS

---

### Phase 4 — 浏览器层 E2E + owner-doc 对齐 + Deferred RELEASED 登记

Status: completed
Targets:
  - `tests/e2e/business-actions/qa-spc-attributes-chart.action.spec.ts`（新建）
  - `tests/e2e/dashboards/qa-dashboard-spc-attributes.value.spec.ts`（新建——iter-2 审查 MINOR-3 修正：`*.value.spec.ts` 文件归 `tests/e2e/dashboards/` 非 `tests/e2e/value-spec/`，对齐既有 `quality.value.spec.ts`）
  - `docs/design/quality/spc.md`（§计数型实现注记）
  - `docs/testing/e2e-runbook.md`（业务动作表 / 套件计数）
  - `docs/plans/2026-07-17-2010-1-dashboard-echarts-spc-crp-charts.md`（Deferred RELEASED 登记）
  - `docs/logs/2026/07-19.md`（聚合条目）
  - `docs/backlog/README.md`（+1 done 行）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] `Add`：`qa-spc-attributes-chart.action.spec.ts`——经 GraphQL `/graphql` 调 **`ErpQaSpcChart__recalculateControlLimit`** `@BizMutation`（`module-quality/erp-qa-service/.../ErpQaSpcChartBizModel.java:69`，iter-1 审查 B4 核实实际方法名）对 4 计数型 chartType 各 1 用例；断言 cl/ucl/lcl 经 `verifyState` `__get` 独立断言（数值 + scale=6 精确）+ assert UCL ≥ CL ≥ LCL 不变量。
  - Skill: none
  - **执行结果**：4 用例（P/NP/C/U 各 1），每用例建 chart + 20 sample → raw GraphQL mutation `ErpQaSpcChart__recalculateControlLimit(chartId:X)`（mutation 返回 Boolean 非实体，用 raw 避免字段选择）→ verifyState `__get` 独立断言 calcStatus/cl/ucl/lcl + UCL ≥ CL ≥ LCL 不变量 + P 图 CL ∈ (0,1) / NP 图 CL ∈ (0, n=100) 语义边界。4 passed。
- [x] `Add`：`qa-dashboard-spc-attributes.value.spec.ts`（`tests/e2e/dashboards/`，iter-2 审查 MINOR-3 修正路径）——经 GraphQL `ErpQaDashboard__getSpcControlChartData(chartId=...@chartType="P")` 取响应 + 断言 **顶层 chartType 字段 + per-sample defectRate/defectCount/inspectedCount 字段非空** + cl/ucl/lcl 一致性（不需经 AMIS 渲染层，纯 @BizQuery 数据断言，对齐 1430-1 范式）。
  - Skill: none
  - **执行结果**：4 参数化用例（chartId=2/3/4/5 对应 P/NP/C/U 种子 chart），每用例 GraphQL query 断言顶层 chartType 字段 + cl/ucl/lcl 非空 + UCL ≥ CL ≥ LCL + samples ≥ 20 + per-sample defectCount/inspectedCount/defectRate 非空 + defectRate = defectCount/inspectedCount 派生关系（toBeCloseTo precision=5）。4 passed。
- [x] `Add`：owner-doc 对齐——`docs/design/quality/spc.md §关键流程 2` 补计数型公式实现注记（P/NP/C/U 公式 + 负数下限钳 0 + chartType 分支）+ `§ErpQaSpcSample` 表补 defectCount/inspectedCount 2 列说明。**修正 `spc.md §ErpQaSpcChart.chartType` l.22 owner-doc 漂移**——数字编码标注（`X_BAR_R=10/.../U=70`）订正为字符串值（`X_BAR_R/X_BAR_S/X_MR/P/NP/C/U`，对齐 live dict yaml `valueType=string`），iter-1 审查 MAJOR 1。
  - Skill: none
  - **执行结果**：spc.md l.22 数字编码标注修正为字符串值（X_BAR_R/X_BAR_S/X_MR/P/NP/C/U 对齐 live dict yaml valueType=string）；§关键流程 2 增「计量型实现注记」+「计数型实现注记」两段（4 公式 + 负数下限钳 0 + 数据派生路径 + 能力指数降级）；§ErpQaSpcSample 表加 defectCount + inspectedCount 2 列说明。
- [x] `Add`：`e2e-runbook` 业务动作表 +1 qa 行（计数型 SPC recalculate）+ 套件计数更新；`backlog/README.md` +1 done 行；2010-1 Deferred「计数型 SPC 计算引擎」补 `**RELEASED by 2026-07-19-0120-2**`；`docs/logs/2026/07-19.md` 增聚合条目。
  - Skill: none
  - **执行结果**：e2e-runbook 业务动作套件计数 84→85 + 新增 plan 2026-07-19-0120-2 spec 计数段；backlog/README.md +1 done 行（plan 2026-07-19-0120-2 ✅ done）；2010-1 Deferred 段补 `**RELEASED by 2026-07-19-0120-2**` + 交付证据；docs/logs/2026/07-19.md 增聚合条目（背景 + Phase 1-4 + 验证 + 范围裁决 + 后续）。
- [x] `Proof`：新增 spec `--workers=1` 全绿 + business-actions + value-spec 全套件抽样回归 0 新增失败 + visual 全套件（dashboards.snapshot / dashboards.visual）quality 段 0 回归。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/qa-spc-attributes-chart.action.spec.ts tests/e2e/dashboards/qa-dashboard-spc-attributes.value.spec.ts --workers=1` + visual 抽样回归
  - Skill: none
  - **执行结果**：8 new tests passed（4 action + 4 value）；quality 既有 5 测试 0 回归（quality.smoke + quality.value 含 KPI + spc-warning + dashboards.snapshot quality 段 + dashboards.visual quality 段）。

Exit Criteria:

- [x] 2 spec 全绿，数值/字段翻转均经 `verifyState`（`__get`）+ GraphQL 响应断言独立断言
- [x] owner-doc 计数型公式 + ErpQaSpcSample 字段补全
- [x] e2e-runbook + backlog + 2010-1 RELEASED + 日志四点落地一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_089bbfd1fffeLgVDWbtLcB8xEQ`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 4 BLOCKER + 3 MAJOR + 3 MINOR，全部 4 BLOCKER 同源：起草者从 `spc.md:22` 复制了陈旧的数字编码 chart-type 值（`P=40/NP=50/C=60/U=70`），未核实 live dict yaml 与 `ErpQaConstants.java`。逐项核实证据：(1) `spc-chart-type.dict.yaml:8-34` 使用字符串值 `X_BAR_R/X_BAR_S/X_MR/P/NP/C/U`，`app-erp-quality.orm.xml:123` `valueType="string"`；(2) `ErpQaConstants.java:140-143` 字符串常量已存在；(3) `ErpQaDashboardBizModel.java:269` 顶层响应已含 chartType 字段；(4) 实际 mutation 名 `ErpQaSpcChart__recalculateControlLimit`（`ErpQaSpcChartBizModel.java:69`）非 `recalculate`。MAJOR 1：owner doc `spc.md:22` 自身陈旧须本计划修正。MAJOR 2：Phase 1 「dict 值校验 helper」item 无 Decision 支撑。MAJOR 3：`ErpQaConfigs.java` 未列入 Phase 2 Targets 条件性。MINOR 1-3：种子范式措辞 / 自我授权措辞 / 前瞻性 closure gate。
- **本 iter-1 修订**：依据 4 BLOCKER 全部修订——(B1) 全文 `P=40/NP=50/C=60/U=70` → 字符串值 `P/NP/C/U`，AMIS 表达式 `'${chartType === "40" || ... }'` → `'${chartType === "P" || ... }'`；(B2) Phase 2 「`ErpQaConstants` 新增常量」item 删除（常量已存在）；(B3) Goals `chartType 加性补字段` 删除（已在顶层响应 `:269`），仅保留 `defectRate/defectCount/inspectedCount` per-sample 加性；(B4) mutation 名 `ErpQaSpcChart__recalculate` → `ErpQaSpcChart__recalculateControlLimit` + 删除「fallback 经 `__save`」推测性措辞。依据 3 MAJOR——(M1) Phase 4 增 owner-doc l.22 数字编码标注修正 item；(M2) Phase 2 删除「dict 值校验 helper」item；(M3) Phase 2 Targets 增 `ErpQaConfigs.java` 条件性（仅 Phase 1 Decision (a) 选定时）。依据 MINOR——(m1) 种子范式措辞订正「与计量型同密度新范式」；(m2) Source 行措辞「本计划预期满足该 successor 触发条件；结束审计裁决」；(m3) Closure Gates 措辞保留前瞻性但显式声明授权工件待补。
- Independent draft review iteration 2: **accept** (`ses_089b1e430ffecq6Ps2Yc7JdZ2J`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKER + 0 MAJOR + 3 MINOR。iter-1 全部 4 BLOCKER（dict valueType / 常量已存在 / chartType 已在响应 / mutation 名错）+ 3 MAJOR（owner-doc 漂移修复 / 删除 vague helper / ErpQaConfigs 条件性 Targets）经核实**全部 genuine 修订落地**——body 与 Draft Review Record 一致无 drift。Current Baseline 9 项主张逐行核实 verified。R1-R14 + anti-slack + template 全 PASS。ORM ask-first 纪律诚实（forward-looking 授权声明，无伪造）。**3 MINOR 路径引用订正**：(m1) Phase 3 web 文件路径 `_vfs/pages/dashboard/page.yaml` → `_vfs/erp/qa/pages/dashboard/main.page.yaml`；(m2) dict yaml 路径 `_vfs/dicts/` → `_vfs/dict/`（单数）；(m3) Phase 4 `tests/e2e/value-spec/` 不存在，`*.value.spec.ts` 实际归 `tests/e2e/dashboards/`。**本 iter-2 修订**：3 MINOR 全部路径订正落地（Phase 3 Targets + Phase 3 item / Current Baseline l.25 / Phase 4 Targets + Phase 4 item）。**共识达成 → `Plan Status: active`**。

## Closure Gates

> 本计划为后端实现 + 看板 AMIS 渲染 + 种子加性追加 + owner-doc 对齐（结果面 = 计数型 SPC 后端 + 前端闭环）。结束前运行新增 JUnit + E2E + 全套件回归 + 154 模块构建。

- [x] 范围内行为完成（4 计数型 chartType calc 引擎 + 采样聚合 + 看板 echarts 分支 + 种子 + 测试）
- [x] 相关文档对齐（spc.md §计数型实现注记 + e2e-runbook + backlog + 2010-1 RELEASED + 日志）
- [x] 已运行验证：`mvn test -pl module-quality/erp-qa-service -am` 全绿（既有计量型 0 回归 + 新增 JUnit）+ 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS（codegen + 种子加性）+ 新 E2E spec 全绿 + visual 抽样回归 0 失败
- [x] 无范围内项目降级为 deferred/follow-up（ORM 加性列经 mission-driver 授权非降级，未授权前 plan 不进 active）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果）。执行期确认后分类。

### 计数型能力指数（Cp/Cpk/DPMO/sigma level）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 行业惯例计数型不直接产 Cp/Cpk；本计划保守 (a) 完全不算；如未来产品要求 DPMO/sigma level 转换，开 successor。
- Successor Required: `yes`（触发条件：产品要求计数型能力指数或 DPMO 报表落地时）

### 计数型 WesternElectric 判异规则适配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 既有判异规则针对计量型连续值；计数型失控判定通常仅「点出控制限」，本计划保守仅保留 UCL/LCL 越界判定。
- Successor Required: `yes`（触发条件：产品要求计数型多规则判异（如连续 7 点同侧趋势）落地时）

### C/U 图 opportunityCount 多机会维度（面积/长度）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 inspectedCount 字段单一；C/U 真实业务可能有面积/长度/时间多机会维度。
- Successor Required: `yes`（触发条件：C/U 业务需求落地需机会维度多样化时——须再加 `opportunityCount` ORM 列）

### CRP 甘特可视化（2010-1 同期 Deferred）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同结果面（mfg 域 vs qa 域）；本计划仅承接 SPC 段。
- Successor Required: `yes`（触发条件：CRP 甘特可视化 / 工作中心级精确费率业务需求落地时）

## Closure

Status Note: 执行完成（2026-07-19，主代理 4 阶段全绿）。范围内行为全部落地：(1) **Phase 1 Decision** 闭合 5 items（4 Decisions + 1 Proof：能力指数保守降级 (a) 完全不算 / P/NP inspection line + C/U NCR 数据派生路径 / 新增 config `erp-dash.qa-spc-default-attributes-chart-id` / Integer 类型裁决）；(2) **Phase 2 后端** ORM 加性追加 `ErpQaSpcSample.defectCount/inspectedCount` Integer precision=10 nullable=true（propId=23/24）+ 新建 `AttributesControlLimitFormulas` POJO（4 静态方法 P/NP/C/U + BigDecimal HALF_UP scale=6 + 负数下限钳 0）+ `SpcControlLimitCalculator.recalculate` chartType 字符串值分支（计量型保持既有 X̄̄-R 范式，计数型调 AttributesControlLimitFormulas）+ `SpcSamplingService.collectSamples` chartType 分支（计数型 `collectAttributesSamples` 新方法 P/NP 走 inspection line REJECTED 计数 / C/U 走 NCR.quantity 累计）+ `SpcCapabilityCalculator.calculateCapability` chartType 分支（计数型保守降级 cp/cpk/pp/ppk/cpm/capabilityLevel 全 null）+ `ErpQaDashboardBizModel.getSpcControlChartData` per-sample 加性 defectRate/defectCount/inspectedCount + chartId 解析增 attributes-chart-id fallback + `ErpQaConfigs.getDashQaSpcDefaultAttributesChartId()` + `ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_ATTRIBUTES_CHART_ID`；(3) **Phase 3 JUnit + 种子 + 看板 echarts 分支** 2 新 JUnit 类 12 用例（`TestErpQaSpcAttributesControlLimit` 7 纯函数 + `TestErpQaSpcAttributesSampling` 5 端到端集成，全绿 0 回归）+ 种子 `erp_qa_spc_chart.csv` 加性 4 行（P/NP/C/U 各 1 chart CALCULATED + cl/ucl/lcl 公式派生值精确填入）+ `erp_qa_spc_sample.csv` 加性 80 行（每 chart 20 子组 defectCount/inspectedCount 字段非空）+ 看板 page.yaml `spcControlChart` adaptor 按 chartType 字符串值分支渲染（缺陷率纵轴 + tooltip 含 defectCount/inspectedCount + yAxis name 切换）；(4) **Phase 4 E2E + owner-doc + RELEASED 登记** 2 新 spec 8 用例（`qa-spc-attributes-chart.action.spec.ts` 4 + `qa-dashboard-spc-attributes.value.spec.ts` 4 参数化）+ owner-doc `spc.md` chartType 数字编码标注修正为字符串值（iter-1 审查 MAJOR 1）+ §关键流程 2 计数型公式实现注记 + §ErpQaSpcSample 表补 defectCount/inspectedCount 2 列说明 + 2010-1 Deferred RELEASED 登记 + e2e-runbook 业务动作套件计数 84→85 + backlog/README.md +1 done 行 + docs/logs/2026/07-19.md 聚合条目。验证全绿：根 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:31 min）；`mvn test -pl module-quality/erp-qa-service -am` qa-service 110 tests 0 failures/0 errors（既有 98 + 新增 12，0 回归）；新 E2E 8 passed（4 action + 4 value）；quality 既有 5 测试 0 回归（quality.smoke + quality.value + spc-warning + dashboards.snapshot quality 段 + dashboards.visual quality 段）。独立结束审计 gate `[ ]` 留待独立子代理（新会话）执行——执行者未自我审计。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话冷重播，无执行者上下文，2026-07-19）
- Scope: 全计划重读 + 5 点一致性 + 仓库 live 证据逐项核实（不仅是最近切片）
- Phase 1 (Decision) 核实：4 Decisions + 1 Proof 全部 `[x]` 且记录含替代方案 + 残留风险 + 行号引用；ORM Integer 类型裁决、采样数据派生路径裁决、chartId config 裁决、能力指数保守降级 (a) 裁决均落地。
- Phase 2 (后端) 核实（live 仓库逐文件核实）：
  - `module-quality/model/app-erp-quality.orm.xml:839-840` 含 `defect_count`/`inspected_count` Integer precision=10 nullable=true（propId=23/24）✓
  - `AttributesControlLimitFormulas.java` 新建（169 行，4 静态方法 calcP/calcNp/calcC/calcU + Result POJO + BigDecimal HALF_UP scale=6 + 负数下限钳 0）✓
  - `SpcControlLimitCalculator.java:110-197` chartType 字符串值分支（`isAttributesChart` public static + `calculateAttributes` private 派发 4 chartType + 守卫 `< 20 samples return false` 不变 + 既有计量型 X̄̄-R/d2/D3/D4 完整保留）✓
  - `SpcSamplingService.java:129-130, 222+` chartType 分支派发 + `collectAttributesSamples` 新方法 ✓
  - `SpcCapabilityCalculator.java:90` `isAttributes` chartType 分支 ✓
  - `ErpQaDashboardBizModel.java:319-324` per-sample 加性 defectCount/inspectedCount/defectRate 字段（仅非 null 时填入，计量型透明）✓
  - `ErpQaDashboardBizModel.java:246` chartId 解析增 `qa-spc-default-attributes-chart-id` fallback ✓
  - `ErpQaConfigs.java:157-158` + `ErpQaConstants.java:135` 新增 config 常量 + reader ✓
- Phase 3 (JUnit + 种子 + 看板) 核实：
  - `TestErpQaSpcAttributesControlLimit.java` + `TestErpQaSpcAttributesSampling.java` 存在 ✓
  - `erp_qa_spc_chart.csv` 6 行（header + 1 X_BAR_R + 4 计数型 P/NP/C/U CALCULATED + cl/ucl/lcl 公式派生值精确填入）✓
  - `erp_qa_spc_sample.csv` 82 行（header + 81 sample，每 chart ≥20 子组）✓
  - `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml` 存在 + 7 处 isAttributes/defectRate/chartType 字符串值分支 ✓
- Phase 4 (E2E + owner-doc + RELEASED) 核实：
  - `tests/e2e/business-actions/qa-spc-attributes-chart.action.spec.ts` + `tests/e2e/dashboards/qa-dashboard-spc-attributes.value.spec.ts` 存在 ✓
  - `docs/design/quality/spc.md:22,49-50,87-92` chartType 数字编码标注修正为字符串值 + §关键流程 2 计数型实现注记 + §ErpQaSpcSample 表补 defectCount/inspectedCount 2 列说明 ✓
  - `docs/plans/2026-07-17-2010-1-dashboard-echarts-spc-crp-charts.md:161` 补 `**RELEASED by 2026-07-19-0120-2**` + 交付证据 ✓
  - `docs/testing/e2e-runbook.md:116` 业务动作套件计数 84→85 + `:424` plan 2026-07-19-0120-2 新增 2 spec 段 ✓
  - `docs/backlog/README.md` +1 done 行 ✓
  - `docs/logs/2026/07-19.md` 存在含本计划聚合条目 ✓
- Anti-Hollow 检查：所有新代码均含真实实现（公式含 BigDecimal 数学 + 分支派发 + 边界守卫），无空函数体/return null 占位/吞异常；新代码经 chartType 分支真实可达（`SpcControlLimitCalculator.recalculate` → `calculateAttributes` → `AttributesControlLimitFormulas.calcP/Np/C/U` 运行时调用链完整）。
- Deferred honesty：4 项 Deferred But Adjudicated 全部含 Successor Required 触发条件；2 Non-Goal 段（不发明计数型能力指数 / 不实现 WesternElectric 计数型适配）与 Phase 1 Decision (a) 保守降级一致，无范围内的 live 缺陷隐藏在 Deferred。
- 五点一致性：`Plan Status: completed` / 4 Phase `Status: completed` / 4 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]`（含本审计门控）/ Closure 证据落地 — 全部一致。
- 结论：批准 closure。

Follow-up:

- 计数型能力指数 Cp/Cpk/DPMO/sigma level（Deferred But Adjudicated 显式 successor，触发条件：产品要求计数型能力指数或 DPMO 报表落地时）
- 计数型 WesternElectric 判异规则适配（Deferred But Adjudicated，触发条件：产品要求计数型多规则判异落地时）
- C/U 图 opportunityCount 多机会维度（Deferred But Adjudicated，触发条件：C/U 业务需求落地需机会维度多样化时——须再加 `opportunityCount` ORM 列）
- CRP 甘特可视化（2010-1 同期 Deferred，触发条件：CRP 甘特可视化/工作中心级精确费率业务需求落地时）
