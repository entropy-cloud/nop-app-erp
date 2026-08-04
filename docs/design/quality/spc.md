# SPC 统计过程控制(Statistical Process Control)

## 目的

设计质量域的统计过程控制能力:质量数据采集、控制图计算、过程能力指数(Cp/Cpk)、失控预警。

## 设计边界

SPC 只做**计量型数据的过程分析**,不做离线检验判定(判定归 ErpQaInspection/ErpQaSamplingPlan)。数据源是 `ErpQaInspectionLine.measuredValue`(实测值),SPC 在其上做**聚合采样**而非重复存储原始读数。

失控预警通过事件驱动(模式 B)触发 `ErpQaNonConformance`(NCR)与 `ErpQaAction`(CAPA),不新建预警实体。

## 实体清单

> 字段约定遵循 `docs/design/domain-design-guidelines.md` §10/§11。表前缀 `erp_qa_`、类名 `ErpQa*`。

### ErpQaSpcChart(SPC 控制图配置,表 `erp_qa_spc_chart`)

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| chartType | 图类型 dict `erp-qa/spc-chart-type`:X_BAR_R/X_BAR_S/X_MR/P/NP/C/U（**字符串枚举值**，对齐 `module-quality/erp-qa-meta/src/main/resources/_vfs/dict/erp-qa/spc-chart-type.dict.yaml valueType=string`，修正原数字编码标注） |
| materialId | 物料(→ErpMdMaterial notGenCode) |
| inspectionTypeId | 关联质检模板 |
| parameterId | 关键检验参数(被控质量特性,→ErpQaInspectionTemplateLine) |
| specMin/specMax | 规格上下限(DECIMAL(20,6),用于 Cp/Cpk) |
| subgroupSize | 子组样本量 n(默认 5) |
| samplingFrequency | 采样频率(cron 或描述) |
| clCenterType | 中心线计算方式 dict:AUTO_FROM_DATA=10/MANUAL=20/TARGET=30 |
| ruleSet | 启用的判异规则集(逗号分隔 WesternElectric 规则编号,如 "1,2,3,4") |
| alarmThreshold | 触发预警的违规次数(默认 1) |
| ucl/lcl/cl | 控制上限/下限/中心线(DECIMAL(20,6),由计算或手工填) |
| calcStatus | 计算状态 dict `erp-qa/spc-calc-status`:PENDING=10/CALCULATED=20/STALE=30 |
| isActive | 是否启用 |
| docStatus/approveStatus | 双轴状态(复用 erp-qa/doc-status、erp-qa/approve-status) |
| 标准审计字段 | |

### ErpQaSpcSample(SPC 样本数据,表 `erp_qa_spc_sample`)

| 字段 | 含义 |
|---|---|
| id/chartId/subgroupNo | 主键/控制图/子组序号 |
| orgId | 标准 |
| sampleTime | 采样时间 |
| measuredValues | 子组内 n 个实测值(JSON 数组,如 [10.1,10.2,...]) |
| mean | 子组均值 X̄(计算字段) |
| range | 子组极差 R(max−min) |
| stdDev | 子组标准差 s |
| defectCount | 缺陷数(计数型 P/NP/C/U；计量型 chart 此字段为 null) |
| inspectedCount | 检验数(计数型 P/NP/C/U；计量型 chart 此字段为 null) |
| sourceBillType/sourceCode/sourceLineCode | 数据来源三元组(反查 ErpQaInspection/ErpQaInspectionLine,凭证指针模式) |
| inspectorId | 检验员(→ErpMdEmployee) |
| violatedRules | 本子组违反的判异规则(如 "1,2",空表示受控) |
| isOutOfControl | 是否失控(冗余,便于查询) |
| 标准审计字段 | |

### ErpQaSpcCapability(过程能力分析结果,表 `erp_qa_spc_capability`)

| 字段 | 含义 |
|---|---|
| id/chartId | 主键/控制图 |
| periodFrom/periodTo | 分析周期 |
| sampleCount | 样本数(子组数) |
| totalObservations | 总观测点数(子组数×n) |
| grandMean | 总均值 X̄̄ |
| overallStdDev | 总体标准差(用于 Pp/Ppk) |
| withinStdDev | 组内标准差(σ̂ = R̄/d2,用于 Cp/Cpk) |
| cp | 过程能力指数 Cp = (USL−LSL)/6σ̂ |
| cpk | 过程能力指数 Cpk = min((USL−X̄̄),(X̄̄−LSL))/3σ̂ |
| pp/ppk | 过程性能指数(用总体标准差) |
| cpm | 偏度修正 Cpm(可选) |
| capabilityLevel | 能力等级评定 dict `erp-qa/spc-capability`:INADEQUATE=10/ACCEPTABLE=20/CAPABLE=30/EXCELLENT=40(Cpk<1.0/1.0-1.33/1.33-1.67/>1.67) |
| isStable | 过程是否统计受控(周期内无违规) |
| calculatedBy/calculatedAt | 计算作业/人/时间 |
| remark | 备注 |

> 新增字典:`erp-qa/spc-chart-type`、`erp-qa/spc-calc-status`、`erp-qa/spc-capability`。

## 关键流程

1. **数据采集**:定时任务(依赖 nop-job)扫描 ErpQaInspectionLine 中已审核(ApproveStatus=APPROVED)且对应 templateLine 命中 SPC chart.parameterId 的记录,按 chart.subgroupSize 与 samplingFrequency 聚合成 ErpQaSpcSample。聚合用业务单号三元组反查,不重复存原始值。

2. **控制图计算**:样本数≥20 子组后触发重算 chart.ucl/lcl/cl 与每个 sample.violatedRules/isOutOfControl。控制限系数 d2/D3/D4 等按 subgroupSize 内置常量表。

   **计量型实现注记**（X_BAR_R/X_BAR_S/X_MR）：`SpcControlLimitCalculator.recalculate` 按 X̄̄-R 范式计算 grandMean = mean(sample.mean), sigmaHat = R̄/d2, UCL/LCL = cl ± 3σ̂；clCenterType 三分支（AUTO_FROM_DATA=grandMean / MANUAL=chart.cl 当前值 / TARGET=(specMin+specMax)/2 规格中值）。

   **计数型实现注记**（P/NP/C/U）：`SpcControlLimitCalculator.recalculate` 按 chartType 字符串值分支调 `AttributesControlLimitFormulas` 对应公式——
   - **P 图**：CL=p̄=Σdᵢ/Σnᵢ，UCL/LCL=p̄±3√(p̄(1−p̄)/n̄)，n̄=平均 inspectedCount；
   - **NP 图**：CL=n·p̄，UCL/LCL=n·p̄±3√(n·p̄(1−p̄))；
   - **C 图**：CL=c̄=Σcᵢ/k，UCL/LCL=c̄±3√c̄；
   - **U 图**：CL=ū=Σcᵢ/Σnᵢ，UCL/LCL=ū±3√(ū/n̄)；
   - 负数下限钳到 0（CL−3σ < 0 时 lcl=0，行业标准——缺陷率/数下界 ≥ 0）；clCenterType 不适用计数型（CL 为统计均值）。计数型采样：P/NP 从 ErpQaInspectionLine 按 result=REJECTED 计数 defectCount + line 总数作 inspectedCount；C/U 从 ErpQaNonConformance 按 sourceType=INSPECTION + inspectionId 反查 quantity 累计 defectCount + inspection 数作 inspectedCount。计数型能力指数保守降级（Phase 1 Decision (a)）：cap.cp/cpk/pp/ppk/cpm 全 null，capabilityLevel=null，仅算 grandMean/overallStdDev 按 defectRate 序列供参考。

3. **失控预警**:sample.isOutOfControl=true 时,事件驱动(模式 B,post-commit)创建 ErpQaNonConformance(sourceType=SPC,severity 按 violatedRules 映射),并按 chart.ruleSet 创建 ErpQaAction(actionType=CAPA)。NCR→Action 的级联已在现有 orm 中存在(ErpQaNonConformance.actions to-many cascade-delete)。

4. **能力分析**:周期性(月/周)任务对每个 chart 计算 ErpQaSpcCapability,等级低于 ACCEPTABLE 触发 ErpQaQualityGoal.currentValue 回写与风险登记 ErpQaRiskRegister。

## 与现有实体的关系

- **ErpQaInspection/InspectionLine**:数据源,SPC 聚合其 measuredValue。
- **ErpQaInspectionTemplate/TemplateLine**:parameterId 关联被控质量特性。
- **ErpQaNonConformance/Action**:失控预警的下游 NCR/CAPA。
- **ErpQaQualityGoal/RiskRegister**:能力等级回写。
- **nop-job**:SPC 采样与能力分析依赖定时任务。

## 关键决策

> **SPC 数据从 InspectionLine 聚合,不重复存储原始读数** —— 避免数据冗余与一致性问题。物化 ErpQaSpcSample 是聚合后的子组统计量,原始值仍在 InspectionLine。

## 菜单归属

quality 域「过程控制(SPC)」分组:控制图配置、样本数据、过程能力分析。

## 页面交互设计（Flux 实现权威）

> 本节补齐 SPC 独立页交互设计（`2026-08-03-1232-5` Phase 2 回填，输入来源 `flux-complex-pages.md` §7 #1 + `2026-08-03-1232-4` P4 实施结果）。SPC 三件套已以 flux 落地（消除原占位 alert），nop-chaos-flux 于 2026-08-03 实现 chart `referenceLines`/`band`/`markers` 完整能力，SPC 控制图无需近似。

### 控制图页（spc-chart）

**布局**：complex 外壳（header 筛选区 + body 主区），数据来自 `ErpQaDashboardBizModel.getSpcControlChartData`（既有 @BizQuery）。

```yaml
type: page
body:
  - type: select              # 控制图选择（chartId）
    name: chartId
    source: { ... ErpQaSpcChart__findPage ... }
  - type: data-source         # 控制图数据（UCL/LCL/CL + 样本序列 + 失控点标记）
    name: chartData
    action: ajax
    args: { url: "/r/ErpQaDashboard__getSpcControlChartData", params: { chartId: "${chartId}" } }
  - type: chart               # 控制图主体
    chartType: line
    source: "${chartData.samples}"
    xAxis: { dataKey: "subgroupNo" }
    yAxis: { label: "均值 X̄" }
    referenceLines:           # UCL/LCL/CL 控制限参考线（flux chart 原生能力）
      - { yAxis: "${chartData.ucl}", label: "UCL ${chartData.ucl}", color: "#ef4444" }
      - { yAxis: "${chartData.cl}", label: "CL ${chartData.cl}", color: "#3b82f6" }
      - { yAxis: "${chartData.lcl}", label: "LCL ${chartData.lcl}", color: "#ef4444" }
    band:                     # 上下界阴影带（控制限内区域）
      { yFrom: "${chartData.lcl}", yTo: "${chartData.ucl}", color: "rgba(59,130,246,0.08)" }
    markers:                  # 失控点标记（dataKey 读 isOutOfControl，红色高亮）
      { dataKey: "isOutOfControl", color: "#ef4444", symbol: "circle", size: 8 }
  - type: crud                # 样本明细列表
    source: "${chartData.samples}"
    columns:
      - { name: "subgroupNo", label: "子组" }
      - { name: "mean", label: "均值" }
      - { name: "violatedRules", label: "违反规则", type: "mapping" }
      - { name: "isOutOfControl", label: "失控", type: "status", labelMap: { true: "失控" }, levelMap: { true: "error" } }
```

**交互**：选择控制图 → data-source 重载控制限 + 样本序列；失控点（`isOutOfControl=true`）以红色 markers 高亮，控制限外区域直观可见；样本明细 crud 支持行级反查数据来源三元组（sourceBillType/sourceCode/sourceLineCode → ErpQaInspection）。

### 过程能力页（spc-capability）

`crud`（能力分析结果列表，含 cp/cpk/capabilityLevel）+ `chart`（bar 能力等级分布）。能力等级 `capabilityLevel` 经 flux `status`（labelMap：INADEQUATE=红/ACCEPTABLE=黄/CAPABLE=绿/EXCELLENT=深绿）着色。

### 样本数据页（spc-sample）

`crud`（样本列表）+ `chart`（line 样本均值趋势，X=subgroupNo/Y=mean，叠加控制限 referenceLines）。

### 数据契约

- 控制图：`getSpcControlChartData(chartId)` 返回 `{ ucl, lcl, cl, samples:[{subgroupNo, mean, range, stdDev, violatedRules, isOutOfControl}] }`
- 能力/样本：标准 `findPage`（`{items, total}`）

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-wmes.md`(MES 质量/SPC 边界)
- `docs/design/quality/inspection-integration.md`(质检集成,SPC 数据源)
