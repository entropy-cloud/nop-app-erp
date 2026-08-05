# A1.33 quality-F3 SPC 与看板 需求-实现符合性五级追踪审计报告

> 报告类型：MA1（RC）需求-实现符合性五级追踪审计
> 切片：A1.33 quality-F3 SPC 与看板（SPC 失控预警 + 过程能力分析 + InspectionLine 聚合 + 质量看板）
> 审计范围：UC-QA-09 / UC-QA-10 / UC-QA-11 / UC-QA-12（4 UC，逐 UC 一矩阵行）
> 真相源层级（§4 Q1）：L1 = `docs/design/quality/use-cases.md`（UC-QA-09 `:148` / UC-QA-10 `:166` / UC-QA-11 `:187` / UC-QA-12 `:207`）；L2 = `docs/design/quality/spc.md §关键流程 / §ErpQaSpcCapability / §关键决策` + `docs/design/dashboards.md §质量看板`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.12 + A2.18 + 本切片差异。
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 结论速览：⚠️(P1) + 多项 P2 — **UC-QA-10 公式/阈值 完整实现[接受]** / **UC-QA-11 不重复存实现[接受 on 主断言]，samplingFrequency 死字段 P2** / **UC-QA-09 采样聚合 + ≥20 门槛 + 控制图计算 + 手动规则评估 + NCR/CAPA 创建主路径全实现[接受 on 主路径]，但 ⚠️ 自动调度链断裂（spc-sampling.batch.xml 漏调 evaluateRules）+ CAPA 1:1 非 per-ruleSet + QualityGoal 名称约定回写 + RiskRegister 硬编码值 + severity LOW 死分支 = 1 P1 + 3 P2 新登记** / **UC-QA-12 KPI 实时聚合 + 期间过滤 + CAPA 阈值 config 化主路径实现[接受 on 主路径]，orgId/行级权限 复用 P1-MA2-093[R1.29 resolved] + dispositionType 替代 defectType P2**。**零 P0**。**新登记 4 finding**：`P1-RC-042`（UC-QA-09 自动调度链断裂）+ `P2-RC-044`（UC-QA-09 CAPA 1:1 非 per-ruleSet + actionType 硬编码 + severity LOW 死分支）+ `P2-RC-045`（UC-QA-10 QualityGoal 名称约定回写 + RiskRegister 硬编码值）+ `P2-RC-046`（UC-QA-11 samplingFrequency 死字段）+ `P2-RC-047`（UC-QA-12 不合格原因 dispositionType 替代）。**复用 1 finding**：`P1-MA2-093`（UC-QA-12 看板 orgId/行级权限，R1.29 全局 ErpOrgIsolationQueryTransformer resolved）。

---

## 9. 与既有 MA2 报告差异增量声明（§6 段落 9，置顶便于去重）

本切片为 quality 域**第三批 RC 切片**（A1.31/A1.32/A1.33 三切片，本切片覆盖 UC-QA-09/10/11/12 SPC 引擎 + 质量看板；A1.31 已 done 覆盖 UC-QA-01/02/03/04/06/07/08 检验门控；A1.32 已 done 覆盖 UC-QA-05 NCR-CAPA 闭环）。按 §去重协议，本报告**不复跑** MA2 状态机/多公司行为审计，直接复用既有 MA2 报告已证实行为作为 L5 既有证据输入，只补"需求契约↔实际行为"差异。

### 复用 A2.12（quality 状态机审计，`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`）

- **复用 A2.12 已证实行为**：SPC 控制图生命周期经实仓确认（`spc.md §关键流程` → `SpcSamplingService` / `SpcControlLimitCalculator` / `SpcOutOfControlHandler` / `SpcCapabilityCalculator` 1:1 对应，A2.12 `:419`）；SPC 计数型分支（P/NP/C/U）已落地（A2.12 `:425-427`，plan 2026-07-19-0120-2）；**P1-MA2-065**（QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态 + CRUD 桩合并裁决，**resolved R1.20**——方案 B owner doc Deferred 标注 + 删除 dict 死状态项）。
- **本切片不重开 P1-MA2-065**（§去重协议）：
  - RiskRegister MITIGATED/CLOSED 死状态、QualityGoal 4 态死状态、SPC-CalcStatus STALE 死状态——属 MA2 行为审计维度（dict 死状态 + CRUD 桩），R1.20 已裁决方案 B Deferred。本切片**不重审行为裁决**，只补**需求视角差异**（如 RiskRegister 硬编码 likelihood/severity/riskScore 是否满足 UC-QA-10 需求契约 = P2-RC-045，不同维度）。
- **P2-MA2-063**（state-machine.md 缺 SPC 独立章节 watch-only）：本切片不投影，与本切片 4 UC 无控制点重叠。

### 复用 A2.18（多公司/多账套隔离审计，`docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`）

- **复用 A2.18 已证实行为**：**P1-MA2-093**（orgId 查询隔离全仓未落地，11 dashboard BizModel 经 `IDaoProvider` 直访绕过认证管道，**显式列 `ErpQaDashboardBizModel`** A2.18 `:101`）经 **R1.29 resolved**（全局 `ErpOrgIsolationQueryTransformer`，plan `2026-07-30-0841-3-r1-29`）。
- **本切片 UC-QA-12 看板行级权限投影同根因同控制点**（与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 / A1.24 UC-AST-12③ / A1.27 UC-INV-11 行级权限复用先例一致）：本切片**追加 RC A1.33 交叉引用注记**到 P1-MA2-093，**不新建编号**。
- **静态存疑点**（§7 SP-1）：R1.29 `ErpOrgIsolationQueryTransformer` 是否覆盖 `ErpQaDashboardBizModel` 直访路径（11 dashboard 之一）属运行时确认范围，本切片不静态定论，登记 SP-1 交 MA4 展开。

### 本切片只补的需求视角差异

1. **UC-QA-09 自动调度链断裂**（**新根因**——既有 arm-index 无 finding 涉及 SPC scheduler 接线断点）：`spc-sampling.batch.xml:23-24` 仅调 `collectSamples + recalculate`，**未调 `evaluateRules`** → 生产调度路径**永不触达**规则引擎 / NCR-CAPA 级联；规则评估仅经手动 @BizMutation `ErpQaSpcChartBizModel.evaluateRules:63-65` 可达 → 验收标准「事件驱动创建 NCR」自动链在生产调度路径断裂（新 finding **P1-RC-042**）。
2. **UC-QA-09 CAPA 1:1 非 per-ruleSet + actionType 硬编码**：L1 `use-cases.md:158` 逐字「按 ruleSet 创建 CAPA(Action)」，L3 `SpcOutOfControlHandler:116-123` 实为 **1 NCR 1 CAPA（1:1）非"按 ruleSet 逐条"** + `action.setActionType("CAPA")` `:119` 硬编码字面量；severity `SEVERITY_LOW` `:41` 声明但**永不返回**（`mapSeverity:127-138` 死分支）（新 finding **P2-RC-044**）。
3. **UC-QA-10 QualityGoal 名称约定回写 + RiskRegister 硬编码值**：L1 `use-cases.md:180` 逐字「回写 QualityGoal.currentValue + 创建 RiskRegister」，L3 `SpcCapabilityCalculator.writeBackQualityGoal:282-294` **按 `eq("code", chart.getCode())` 名称约定查找（非 FK）** `:284-287` + 无匹配 goal 静默 no-op `:288-290`；`registerRisk:296-310` `likelihood=3/severity=4/riskScore=12` **硬编码** `:305-307`（非按 Cpk 量级派生）（新 finding **P2-RC-045**）。
4. **UC-QA-11 samplingFrequency 死字段**：L1 `use-cases.md:194` 逐字「按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample」，L3 `service/spc/` grep `samplingFrequency|getSamplingFrequency` **零命中**——实际节奏由全局 nop-job cron 决定，**字段是死元数据**（新 finding **P2-RC-046**）。
5. **UC-QA-12 不合格原因 dispositionType 替代**：L1 `use-cases.md:215` 逐字「不合格原因 TOP」，L3 `ErpQaDashboardBizModel.findDefectTopN:142-143` GROUP BY `dispositionType`，Javadoc `:56-57` 显式声明「defectType 未物化，以 dispositionType 为聚合维度」（新 finding **P2-RC-047**）。
6. **UC-QA-10 调度窗口硬编码**：`spc-capability.batch.xml:22-23` `periodFrom = today.minusDays(30)` **硬编码 30 天窗口**（非按 chart 周期/日历月对齐）。此为实现细节偏离，不直接违反 L1「周期任务」字面（cron `0 0 2 * * ?` 仍是周期），归入 **P2-RC-045 描述**（同一控制点：能力分析调度配置）。
7. **UC-QA-12 看板 orgId/行级权限**（**复用 P1-MA2-093**，不新建）：`ErpQaDashboardBizModel` plain `@BizModel :59-60`（不继承 CrudBizModel）+ `IServiceContext context` 全文零引用（`context.` grep 仅 import :19）+ 所有 QueryBean 零 orgId（`loadInspectionsInRange:352-358` / `countOpenNcrs:360-367` / `countOutOfControlCharts:285-294` / `countInadequateCapabilityCharts:331-340` / `countOpenSpcNcrs:342-350`）。

---

## 1. 需求契约原文（逐字引用，§1 L1 格式）

> 来源 `docs/design/quality/use-cases.md`，逐字引用验收标准（禁止转述）。

### UC-QA-09 SPC 失控预警（`:148`）
```
SPC 采样(nop-job) → 聚合 InspectionLine.measuredValue → ErpQaSpcSample
控制图计算(≥20 子组) → 检查 violatedRules
子组.isOutOfControl == true →
  事件驱动创建 NCR(sourceType=SPC)
  按 ruleSet 创建 CAPA(Action)
  severity 按 violatedRules 映射
```

**断言计数（逐条完整枚举，禁止抽样）**：UC-QA-09 共 **7 条验收标准**：
- **断言①**「SPC 采样(nop-job)」（采样调度经 nop-job 触发）
- **断言②**「聚合 InspectionLine.measuredValue → ErpQaSpcSample」（数据源 + 单一真相源 + 不重复存）
- **断言③**「控制图计算(≥20 子组)」（≥20 子组门槛触发控制限计算）
- **断言④**「检查 violatedRules」（判异规则评估 + violatedRules 字段）
- **断言⑤**「子组.isOutOfControl == true → 事件驱动创建 NCR(sourceType=SPC)」（失控样本事件驱动 → NCR + sourceType=SPC）
- **断言⑥**「按 ruleSet 创建 CAPA(Action)」（CAPA 措施创建 + 按 ruleSet 维度）
- **断言⑦**「severity 按 violatedRules 映射」（severity 字段经 violatedRules 派生）

### UC-QA-10 SPC 过程能力分析（`:166`）
```
周期任务 → 计算 ErpQaSpcCapability:
  Cp = (USL - LSL) / (6 × σ̂)        // σ̂ = R̄/d2
  Cpk = min((USL - X̄̄), (X̄̄ - LSL)) / (3 × σ̂)
capabilityLevel:
  Cpk < 1.0 → INADEQUATE
  1.0-1.33 → ACCEPTABLE
  1.33-1.67 → CAPABLE
  > 1.67 → EXCELLENT
若 < ACCEPTABLE → 回写 QualityGoal.currentValue + 创建 RiskRegister
```

**断言计数**：UC-QA-10 共 **7 条验收标准**：
- **断言①**「周期任务」（cron-gated 调度）
- **断言②**「Cp = (USL - LSL) / (6 × σ̂)，σ̂ = R̄/d2」（Cp 公式 + σ̂=R̄/d2）
- **断言③**「Cpk = min((USL - X̄̄), (X̄̄ - LSL)) / (3 × σ̂)」（Cpk 公式）
- **断言④**「capabilityLevel 阈值分级（Cpk<1.0→INADEQUATE / 1.0-1.33→ACCEPTABLE / 1.33-1.67→CAPABLE / >1.67→EXCELLENT）」
- **断言⑤**「若 < ACCEPTABLE → 回写 QualityGoal.currentValue」
- **断言⑥**「若 < ACCEPTABLE → 创建 RiskRegister」
- **断言⑦**（隐式）「持久化 ErpQaSpcCapability」（计算结果落库，供看板/审计）

### UC-QA-11 SPC 数据从 InspectionLine 聚合（`:187`）
```
SPC 采样任务 → 从 ErpQaInspectionLine.measuredValue 聚合(不重复存)
按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample
原始读数仍在 InspectionLine(单一真相源)
SpcSample.measuredValues 是聚合后的子组统计
```

**断言计数**：UC-QA-11 共 **5 条验收标准**：
- **断言①**「SPC 采样任务 → 从 ErpQaInspectionLine.measuredValue 聚合」
- **断言②**「不重复存」（InspectionLine 原始读数不被写回/复制）
- **断言③**「按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample」（聚合维度含 subgroupSize **+** samplingFrequency）
- **断言④**「原始读数仍在 InspectionLine（单一真相源）」
- **断言⑤**「SpcSample.measuredValues 是聚合后的子组统计」

### UC-QA-12 质量看板（`:207`）
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  质检数/合格率/不合格数/开放NCR, 合格率趋势, 不合格原因TOP, SPC失控/CAPA逾期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**断言计数**：UC-QA-12 共 **6 条验收标准**：
- **断言①**「KPI 卡片值 == 对应实体的实时聚合（非硬编码）」
- **断言②**「按期间过滤」（startDate/endDate 维度）
- **断言③**「按 orgId 过滤」（多公司维度）
- **断言④**「按权限过滤」（行级权限）
- **断言⑤**「预警项 == 满足阈值条件的记录（阈值来自系统配置，非硬编码）」
- **断言⑥**「看板数据受行级权限约束（只看自己组织/部门/成本中心）」（与断言④强化）

---

## 2. 实现证据（代码路径，§1 L3 格式，含跨域调用链）

> 全部 `module-quality/erp-qa-service/src/main/...` + `app-erp-all/src/main/resources/_vfs/nop/job/conf/...`，含行号。

| 控制点 | 代码路径（file:line） | 备注 |
|--------|----------------------|------|
| **采样调度（job→batch）** | `app-erp-all/_vfs/nop/job/conf/erp-qa-spc-sampling.job.yaml:1-13`（jobName `erp-qa-spc-sampling`，cron `0 0 * * * ?`，`enabled` 默认 false）→ `module-quality/erp-qa-service/_vfs/nop/batch-task/qa/spc-sampling.batch.xml:10-26`（loader 读 `ErpQaSpcChart isActive=true`；processor `:21-25` 注入 spcSamplingService + spcControlLimitCalculator，调 `collectSamples` + `recalculate`） | **⚠️ 关键缺口**：`spc-sampling.batch.xml:23-24` 仅调 `collectSamples + recalculate`，**未调 `evaluateRules`** → 自动级联断裂 |
| **能力分析调度（job→batch）** | `app-erp-all/_vfs/nop/job/conf/erp-qa-spc-capability.job.yaml:1-13`（cron `0 0 2 * * ?`，enabled 默认 false）→ `spc-capability.batch.xml:19-26`（`periodFrom = today.minusDays(30)` 硬编码 30 天窗口 `:22-23`）→ `IErpQaSpcCapabilityBiz.calculateCapability` | 周期任务实现，30 天窗口非按 chart 周期/日历月对齐（P2-RC-045 描述） |
| **采样聚合（计量型）** | `SpcSamplingService.collectSamples:105-210`（候选行 `findApprovedInspectionLines:366-404` `eq("parameterId")` `:370` + APPROVED 过滤；`parseMeasuredValue:466-480` 读 `line.getMeasuredValue()` `:467`，非数值 warn+skip `:475-479`；幂等 sampledKeys `:142-143/:156-159` 三元组去重） | InspectionLine 不写回（单一真相源 ✅） |
| **SpcSample 构建** | `SpcSamplingService:181-206`（subgroupSize 用于 `:122/:179`；mean/range/stdDev `:194-199`；measuredValues JSON `:192`；sourceBillType=ERP_QA_INSPECTION `:201`） | samplingFrequency 零读取（P2-RC-046） |
| **≥20 子组门槛 + 控制限计算** | `SpcControlLimitCalculator.recalculate:93-165`（门槛 `samples.size() < SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT` 早返回 false `:101-104`；σ̂=R̄/d2 经 `:139-140`；UCL/LCL = cl ± 3σ̂ `:142-145`；写回 chart `:159-163`）；`estimateWithinStdDev:200-209`（Cp/Cpk σ̂ 复用） | 阈值=默认 20（ErpQaConstants.SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT） |
| **规则引擎（判异）** | `SpcRuleEngine.evaluate:82-133`（UCL/LCL/CL null 时早返回 `:89-92`；Western Electric 规则 1-4 纯函数 `evaluateRules:106`/body `:144-242`；写回 `violatedRules`/`isOutOfControl` `:116-121`）；`parseRuleSet:252-264`（按 chart.ruleSet 逗号启用子集） | 纯函数强测；**仅手动 @BizMutation `ErpQaSpcChartBizModel.evaluateRules:63-65` 可达，调度链断点** |
| **手动规则评估入口** | `ErpQaSpcChartBizModel.evaluateRules:61-65`（@BizMutation 委托 `evaluateRulesProcessor.evaluateRules`）→ `ErpQaSpcChartEvaluateRulesProcessor` → `SpcRuleEngine.evaluate` | 唯一可达路径（手动） |
| **NCR 事件驱动创建** | `SpcRuleEngine.java:122-130`（isOutOfControl 时调 `outOfControlHandler.cascadeNcrAndCapa`，try/catch 不中止循环 `:126-129`）→ `SpcOutOfControlHandler.cascadeNcrAndCapa:78-91`（config-gated `ErpQaConfigs.isSpcAutoNcrEnabled()` `:80` 默认 true）注册 `transactionTemplate.afterCommit(null, () -> createNcrAndAction(...))` `:83`；NCR 创建 `createNcrAndAction:99-114`：`code="NCR-SPC-"+chart+"-"+subgroupNo` `:101`，`setSourceType(NCR_SOURCE_TYPE_SPC)` `:103`，`setStatus(NCR_STATUS_OPEN)` `:108`，幂等预检 `findExistingSpcNcr:96/140-151` | post-commit 时序（JunitAutoTestCase 下不可靠，测试经反射绕过） |
| **CAPA 创建** | `SpcOutOfControlHandler.createNcrAndAction:116-123`（`action.setActionType("CAPA")` **硬编码字面量** `:119`，`setStatus(ACTION_STATUS_PENDING)` `:122`）——**1 NCR 1 CAPA（1:1），非"按 ruleSet/按违规规则"逐条** | P2-RC-044 |
| **severity 映射** | `SpcOutOfControlHandler.mapSeverity:127-138`（空→NORMAL `:128-130`；size≥2→CRITICAL `:131-133`；含"1"→HIGH `:134-136`；else→NORMAL `:137`）；`SEVERITY_LOW` 声明于 `:41` 但**永不返回**（死分支）；severity 码为硬编码 Java 常量 `:41-44`，未校验 `erp-qa/severity` 字典 | P2-RC-044 |
| **能力分析计算器** | `SpcCapabilityCalculator.calculateCapability:77-196`（持久化 ErpQaSpcCapability `:161-180`）；σ̂=R̄/d2 经 `:147`→`SpcControlLimitCalculator.estimateWithinStdDev:200-209`；USL/LSL = `chart.getSpecMax()/getSpecMin()` `:149-150`；Cp `computeCp:233-238` / Cpk `computeCpk:240-247`（含 Pp/Ppk `:153-154` + Cpm `computeCpm:250-262`） | 公式实现 ✅ |
| **能力阈值分级** | `SpcCapabilityCalculator.classifyByCpk:265-280`（< 1.0 INADEQUATE / < 1.33 ACCEPTABLE / < 1.67 CAPABLE / else EXCELLENT）——与 L1 **逐字一致** | 阈值实现 ✅ |
| **QualityGoal 回写** | `SpcCapabilityCalculator.writeBackQualityGoal:282-294`（仅 !isAttributes && INADEQUATE 时触发 `:183`）：**按 `eq("code", chart.getCode())` 名称约定查找（非 FK）** `:284-287`；无匹配 goal 静默 no-op `:288-290`；`setCurrentValue(cpk)` `:292` | try/catch WARN 不中止持久化 `:186-188`；P2-RC-045 |
| **RiskRegister 创建** | `SpcCapabilityCalculator.registerRisk:296-310`（`risk.setStatus("OPEN")` `:308`；**likelihood=3/severity=4/riskScore=12 硬编码** `:305-307`，非按 Cpk 量级派生；category=`SPC_PROCESS_CAPABILITY` `:304`）；try/catch WARN `:191-193` | P2-RC-045 |
| **看板 KPI** | `ErpQaDashboardBizModel.getDashboardKpi:67-96`（plain `@BizModel :59-60` **不继承 CrudBizModel**）：`inspectionCount=inspections.size()` `:77`、accepted/rejected 流式计数 `:78-83`、passRate `:84`、openNcrCount `:85` | 实时聚合 ✅，硬编码 ❌（无） |
| **看板趋势 / 缺陷 TOP / CAPA 逾期 / SPC 预警** | `getDashboardTrend:99-131`；`findDefectTopN:134-160`（**DB GROUP BY dispositionType** `:143`，**非 defectType**——Javadoc `:56-57` 显式声明「defectType 未物化，以 dispositionType 为聚合维度」）；`findCapaOverdueAlert:166-194`（阈值 `AppConfig.var(CONFIG_DASH_QA_CAPA_OVERDUE_DAYS, ...)` `:168-170` ✅ config 化）；`getSpcOutOfControlWarning:208-225` | 实时聚合 ✅；CAPA 阈值 config ✅ |
| **看板 SPC 控制图数据** | `getSpcControlChartData:240-281`（chartId 解析优先级：入参 > config default > config attributes default > 最近一张 chart） | 计量型/计数型字段透传 ✅ |
| **期间过滤 ✅** | `getDashboardKpi:68-69/:73-74`；`loadInspectionsInRange:352-358` ge/le inspectionDate | ✅ |
| **orgId 过滤 ❌ / 行级权限 ❌** | 全 `ErpQaDashboardBizModel` 所有 QueryBean 零 orgId；`IServiceContext context` 在每个 @BizQuery 声明却**全文零引用**（`context.` grep 仅 import :19）；类不继承 CrudBizModel → 平台行级安全不施加于 `daoProvider.daoFor(...).findAllByQuery(q)` 直访 | 复用 P1-MA2-093 |

---

## 3. 测试证据（测试断言，§1 L4 格式，注明断言强度）

> 全部 `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/{spc,dashboard}/`。

### 强断言（纯函数）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `spc/TestSpcStatistics.java`（5 @Test） | 强 | BigDecimal 精确等值（mean/range/stdDev） |
| `spc/TestSpcRuleEnginePure.java`（8 @Test） | 强 | 违规规则 Set 精确等值，覆盖规则 1-4 + ruleSet 子集过滤 |
| `spc/TestSpcCapabilityFormulas.java`（6 @Test） | 强 | Cp/Cpk/Cpm 精确值 + `classifyByCpk` 阈值直接证明 UC-QA-10 AC-4 边界 |
| `spc/TestErpQaSpcAttributesControlLimit.java`（7 @Test） | 强 | P/NP/C/U 图精确 CL + LCL<0 钳位 |

### 强断言（DB 集成）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `spc/TestErpQaSpcSampling.java#recalculateComputesControlLimitsWhen20Subgroups:111` | 强 | 证明 ≥20 门槛（UC-QA-09 AC-3 / UC-QA-10 AC-1 调度的下游前置） |
| `spc/TestErpQaSpcSampling.java#recalculateKeepsPendingWhenLessThan20:132` | 强 | 反向门槛（< 20 不重算） |
| `spc/TestErpQaSpcSampling.java`（6 @Test，含幂等 + mean=30/range=40 精确） | 强 | UC-QA-11 AC-1/2/4/5（聚合 + 单一真相源） |
| `spc/TestErpQaSpcAttributesSampling.java`（5 @Test，defectCount/inspectedCount 精确 + P-CL=0.2） | 强 | 计数型聚合 |
| `spc/TestErpQaSpcCapability.java#calculateCapabilityInadequateWritesBackGoalAndRegistersRisk:82` | 强 | UC-QA-10 AC-5/6 回写 + RiskRegister category=SPC_PROCESS_CAPABILITY（**注**：未断言 goal.code 名称约定匹配 + 未断言 likelihood/severity/riskScore 硬编码值） |

### 混合（反射绕过 afterCommit）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `spc/TestErpQaSpcOutOfControl.java`（4 @Test） | 强（NCR+CAPA 断言）+ 反射绕过 | **经反射 `invokeCreateNcrDirectly:165-174` 绕过 `afterCommit`**——Javadoc `:46-48` 显式声明「post-commit hook 在 JunitAutoTestCase 下不可靠，生产代码仍走 afterCommit post-commit 路径，本测试仅验证建单算法」 |

### 强断言（看板）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `dashboard/TestErpQaDashboard.java`（5 @Test） | 强 | passRate=0.5/openNcrCount=2/SCRAP=3 数值等值 |
| `dashboard/TestErpQaDashboardSpc.java`（5 @Test） | 强 | 去重 chart 计数 + config-gate 差分 |
| `dashboard/TestErpQaDashboardSpcChart.java`（7 @Test） | 强 | cl/ucl/lcl 透传 + 排序 + config 覆盖 |

### E2E（`tests/e2e/dashboards/`）
| Spec | 断言强度 | 备注 |
|------|---------|------|
| `dashboards/quality.value.spec.ts` | 强值断言 | inspectionCount=3/passRate=0.6666/openNcrCount=3/outOfControlChartCount=1/... |
| `dashboards/qa-dashboard-spc-attributes.value.spec.ts` | 强值断言 | P/NP/C/U 图 CL 排序 UCL≥CL≥LCL + 子组≥20 |
| `dashboards/quality.smoke.spec.ts` | 仅冒烟 | 不进 L4 强断言矩阵 |

### ⚠️ 测试缺口（与功能缺口一致）
1. **`afterCommit` 事件驱动路径零端到端覆盖**（UC-QA-09 AC-5 timing/并发幂等未验证；测试经反射绕过 post-commit hook）；
2. **无测试覆盖调度链端到端**（job.yaml→batch.xml→...→NCR），故 batch XML 漏调 evaluateRules（**P1-RC-042**）未被捕获；
3. **无测试覆盖看板行级权限/orgId 过滤**（与功能缺失一致，复用 P1-MA2-093）；
4. **`TestErpQaSpcCapability` 未断言 goal.code 名称约定查找**（P2-RC-045 静默 no-op 路径无验证）+ **未断言 RiskRegister likelihood/severity/riskScore 值**（P2-RC-045 硬编码值无验证）。

---

## 4. 运行时行为证据（§1 L5 格式）

> 按 §去重协议，本切片复用 A2.12 + A2.18 已证实行为，只补需求视角差异。

### 复用 A2.12（quality 状态机审计）已证实行为
- **SPC 引擎生命周期 1:1 对应 spc.md §关键流程**（A2.12 `:419`）：`SpcSamplingService` → `SpcControlLimitCalculator` → `SpcRuleEngine` → `SpcOutOfControlHandler` → `SpcCapabilityCalculator` 五组件均存在且经实仓确认。
- **计数型 SPC 分支（P/NP/C/U）实现**（A2.12 `:425-427`，plan 2026-07-19-0120-2）。
- **P1-MA2-065 resolved R1.20**（RiskRegister/QualityGoal/SPC-CalcStatus dict 死状态 + CRUD 桩，方案 B Deferred）——本切片不重开，只补需求视角差异（P2-RC-045 不同维度）。

### 复用 A2.18（多公司隔离审计）已证实行为
- **P1-MA2-093 resolved R1.29**（11 dashboard BizModel 经 IDaoProvider 直访绕过认证管道，含 `ErpQaDashboardBizModel`，A2.18 `:101`）——全局 `ErpOrgIsolationQueryTransformer` 已注入。
- **本切片 UC-QA-12 AC-3/4/6 行级权限投影**同根因同控制点，**追加 RC A1.33 交叉引用注记不新建**。
- **静态存疑点 SP-1**：R1.29 `ErpOrgIsolationQueryTransformer` 是否覆盖 `ErpQaDashboardBizModel` 直访路径属运行时确认（与 A1.7 SP-4 / A1.11 SP-3 / A1.21 SP / A1.24 SP-4 / A1.27 SP 同根因），交 MA4 A4.1 展开。

### 本切片 L5 行为判读（结合 L3 代码静态分析 + 既有测试）
- **UC-QA-09**：手动路径（`ErpQaSpcChartBizModel.evaluateRules`）规则评估 → 失控样本 → NCR+CAPA 创建主路径已实现（`TestErpQaSpcOutOfControl` 4 @Test 强断言 sourceType=SPC + status=OPEN + actionType=CAPA + 幂等预检）；**但自动调度路径（nop-job cron）→ batch → 仅 `collectSamples + recalculate`，永不触达 `evaluateRules`** → 生产环境下，若不手动触发 `evaluateRules`，失控样本不会自动产生 NCR/CAPA。这是**新根因**（既有 arm-index 无 finding 涉及 SPC scheduler 接线断点）。
- **UC-QA-10**：公式/阈值/回写/RiskRegister 全链已实现（`TestErpQaSpcCapability` 3 @Test 强断言 INADEQUATE 回写 + RiskRegister 创建），但 goal 查找用名称约定（无匹配 no-op）+ risk 硬编码值（非按 Cpk 派生）属需求契约维度的"次要不完全满足"。
- **UC-QA-11**：聚合不重复存 ✅（InspectionLine 不写回，`TestErpQaSpcSampling` 6 @Test 强断言 mean=30/range=40 + 幂等三元组）；samplingFrequency 零读取 ✅（grep 实证）——主路径（聚合 + 不重复存 + subgroupSize + 单一真相源）全实现，samplingFrequency 字段是死元数据。
- **UC-QA-12**：KPI 实时聚合 ✅ + 期间过滤 ✅ + CAPA 阈值 config ✅（`TestErpQaDashboard` 5 @Test + `TestErpQaDashboardSpc` 5 @Test + `TestErpQaDashboardSpcChart` 7 @Test 强断言）；**但 orgId 过滤缺失 + 行级权限缺失**（复用 P1-MA2-093 resolved R1.29 + SP-1）+ 不合格原因 dispositionType 替代 defectType（功能替代品，主路径可用，边界维度弱）。

---

## 5. 五级追踪矩阵 + 符合性结论（§1 矩阵 + §2 判据）

### UC-QA-09 SPC 失控预警

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① SPC 采样(nop-job) | `use-cases.md:154` | `erp-qa-spc-sampling.job.yaml:1-13` + `spc-sampling.batch.xml:10-26` + `SpcSamplingService.collectSamples:105` | `TestErpQaSpcSampling` 6 @Test 强 | 调度链已落地（默认 enabled=false） | **接受** |
| ② 聚合 InspectionLine.measuredValue → ErpQaSpcSample | `use-cases.md:154` | `SpcSamplingService.collectSamples:105-210` + `findApprovedInspectionLines:366-404` + `parseMeasuredValue:466-480` | `TestErpQaSpcSampling` 强 | 聚合 + 不重复存 ✅ | **接受**（与 UC-QA-11 AC-1/2 同控制点） |
| ③ 控制图计算(≥20 子组) | `use-cases.md:155` | `SpcControlLimitCalculator.recalculate:93-165`（门槛 `:101`） | `recalculateComputesControlLimitsWhen20Subgroups:111` 强 + `recalculateKeepsPendingWhenLessThan20:132` 强 | ≥20 门槛 + 反向验证 ✅ | **接受** |
| ④ 检查 violatedRules | `use-cases.md:155` | `SpcRuleEngine.evaluate:82-133` + `evaluateRules:144-242`（规则 1-4） | `TestSpcRuleEnginePure` 8 @Test 强 | 规则评估纯函数强 ✅；**⚠️ 调度链断点**（batch XML 未调 evaluate） | **P1**（AC-4 + AC-5 联动：自动调度路径不可达规则评估）→ P1-RC-042 |
| ⑤ isOutOfControl → 事件驱动创建 NCR(sourceType=SPC) | `use-cases.md:156-157` | `SpcRuleEngine:122-130` → `SpcOutOfControlHandler.cascadeNcrAndCapa:78-91`（afterCommit `:83` + config-gated `:80`）→ `createNcrAndAction:99-114`（sourceType=SPC `:103` + status=OPEN `:108` + 幂等 `:96`） | `TestErpQaSpcOutOfControl` 4 @Test 强（**反射绕过 afterCommit，建单算法强断言**） | 主路径（手动评估触发）✅；**自动调度路径断裂**（仅 batch 不评估规则 → 不达此步） | **P1**（自动链断点，与 AC-4 同根因）→ P1-RC-042 |
| ⑥ 按 ruleSet 创建 CAPA(Action) | `use-cases.md:158` | `SpcOutOfControlHandler.createNcrAndAction:116-123`（1 NCR 1 CAPA **1:1**，**非按 ruleSet 逐条**；`actionType="CAPA"` 硬编码 `:119`） | `TestErpQaSpcOutOfControl` 强（断言 1 CAPA 数量，未断言 per-ruleSet） | 1:1 实现，ruleSet 维度未实现 | **P2** → P2-RC-044 |
| ⑦ severity 按 violatedRules 映射 | `use-cases.md:159` | `SpcOutOfControlHandler.mapSeverity:127-138`（空→NORMAL / size≥2→CRITICAL / 含"1"→HIGH / else→NORMAL；**`SEVERITY_LOW :41` 死分支**） | `TestErpQaSpcOutOfControl` 强（HIGH/CRITICAL 断言） | severity 字段经 violatedRules 派生 ✅，LOW 死分支 | **P2** → P2-RC-044 |

**UC-QA-09 整体裁决：P1**（取最高）。核心：**自动调度链断裂**（AC-4 + AC-5 联动），生产调度路径永不触达规则评估→NCR/CAPA 级联。**§4 三判据复核（P1 项强制）**：
- **(i) plan-audit**：本切片候选偏差**未经独立 plan-audit**（spc-sampling.batch.xml 漏调 evaluateRules 是设计/实现遗漏，无 plan-audit 记录将其裁决为 documented simplification）；
- **(ii) owner doc documented simplification**：`spc.md §关键流程` 完整描述了自动链路（采样→控制图→规则评估→NCR/CAPA），**未声明**「自动调度链 evaluateRules 步骤 Deferred」或「经手动 evaluateRules 触发」；
- **(iii) product-scope 范围裁剪**：`product-scope.md` 未将 SPC 失控预警的自动级联裁剪出范围；
- **Q4=(a) 张力**：三判据均不成立 → P1 强制实现，禁方案 B 关闭。修复 = `spc-sampling.batch.xml` processor 段追加 `spcRuleEngine.evaluate(item.id, ...)` 调用（纯调度接线 + Bean 注入，预授权自动执行，**不触发 §5 ask-first**——不触及 ORM/会计过账核心路径）。

**非 P0 论证**：① 不破坏活跃数据（失控样本不创建 NCR 仅意味告警延迟，不导致库存/会计错误写入）；② 不属会计过账正确性破坏；③ 不属核心业务循环断裂（质检主路径 + NCR-CAPA 手动闭环完整）。归 §2 P1①（功能实质偏离验收标准——自动链路在生产调度路径不可达）+ §2 P1②（异常路径未实现——事件驱动自动级联）。

### UC-QA-10 SPC 过程能力分析

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 周期任务 | `use-cases.md:172` | `erp-qa-spc-capability.job.yaml:1-13`（cron `0 0 2 * * ?`）+ `spc-capability.batch.xml:19-26` | （调度链测试缺口） | cron-gated 周期任务 ✅ | **接受**（30 天窗口细节偏离归 P2-RC-045 描述） |
| ② Cp = (USL-LSL)/(6×σ̂)，σ̂=R̄/d2 | `use-cases.md:173` | `SpcCapabilityCalculator.computeCp:233-238` + `estimateWithinStdDev:200-209` | `TestSpcCapabilityFormulas` 6 @Test 强（精确值） | 公式实现 ✅ | **接受** |
| ③ Cpk = min((USL-X̄̄),(X̄̄-LSL))/(3×σ̂) | `use-cases.md:174` | `computeCpk:240-247` | `TestSpcCapabilityFormulas` 强 | 公式实现 ✅ | **接受** |
| ④ capabilityLevel 阈值分级 | `use-cases.md:175-179` | `classifyByCpk:265-280`（< 1.0 INADEQUATE / < 1.33 ACCEPTABLE / < 1.67 CAPABLE / else EXCELLENT） | `TestSpcCapabilityFormulas#classifyByCpkThresholds:45` 直接证明边界 | 阈值与 L1 **逐字一致** ✅ | **接受** |
| ⑤ 若 < ACCEPTABLE → 回写 QualityGoal.currentValue | `use-cases.md:180` | `writeBackQualityGoal:282-294`（**名称约定查找 `:284-287` 非 FK + 无匹配静默 no-op `:288-290` + setCurrentValue(cpk) `:292`） | `calculateCapabilityInadequateWritesBackGoalAndRegistersRisk:82` 强（断言 goal.currentValue 更新，**未断言 goal.code 名称约定匹配**） | 主路径（goal 存在）✅；goal 不存在静默 no-op ❌ | **P2** → P2-RC-045 |
| ⑥ 若 < ACCEPTABLE → 创建 RiskRegister | `use-cases.md:180` | `registerRisk:296-310`（`setStatus("OPEN") :308` + `likelihood=3/severity=4/riskScore=12 硬编码 :305-307` + category=SPC_PROCESS_CAPABILITY `:304`） | `calculateCapabilityInadequateWritesBackGoalAndRegistersRisk:82` 强（断言 category，**未断言 likelihood/severity/riskScore 值**） | RiskRegister 创建 ✅；硬编码值（非按 Cpk 量级派生）❌ | **P2** → P2-RC-045 |
| ⑦（隐式）持久化 ErpQaSpcCapability | （隐式） | `SpcCapabilityCalculator.calculateCapability:161-180`（持久化全字段） | `TestErpQaSpcCapability` 3 @Test 强 | 持久化 ✅ | **接受** |

**UC-QA-10 整体裁决：P2**（取最高）。**接受 on 主路径**（公式/阈值/持久化全实现强测）；P2 on AC-5/6（回写脆弱 + RiskRegister 硬编码）。**Q4 张力评估**：UC-QA-10 主路径（Cp/Cpk/阈值/持久化）全实现且强测覆盖，AC-5/6 主路径（INADEQUATE 触发回写 + RiskRegister 创建）行为也存在，仅"查找方式 + 量级派生"是次要不完全满足 → §2 P2①（次要验收标准未完全满足，主路径 OK 边界弱）。**§4 三判据复核**：spc.md 未声明 QualityGoal 回写/RiskRegister 创建为 Deferred，但主路径已实现（不像 P1-RC-042 完全断链），归 P2。

### UC-QA-11 SPC 数据从 InspectionLine 聚合

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 从 InspectionLine.measuredValue 聚合 | `use-cases.md:193` | `SpcSamplingService.findApprovedInspectionLines:366-404` + `parseMeasuredValue:466-480` | `TestErpQaSpcSampling` 6 @Test 强 | ✅ | **接受** |
| ② 不重复存 | `use-cases.md:193` | InspectionLine **从不被写回**；幂等 sampledKeys 三元组 `:142-143/:156-159` | `TestErpQaSpcSampling` 幂等测试强 | ✅ | **接受** |
| ③ 按 chart.subgroupSize + samplingFrequency 聚合 | `use-cases.md:194` | subgroupSize 用于 `:122/:179`；**samplingFrequency 零读取**（grep 实证） | （无 samplingFrequency 测试） | subgroupSize ✅；samplingFrequency ❌（死字段） | **P2** → P2-RC-046 |
| ④ 原始读数仍在 InspectionLine（单一真相源） | `use-cases.md:195` | SpcSample 构建 `:181-206` 不写 InspectionLine | `TestErpQaSpcSampling` 强（mean/range 派生自 line.measuredValue） | ✅ | **接受** |
| ⑤ SpcSample.measuredValues 是聚合后的子组统计 | `use-cases.md:196` | `setMeasuredValues(JsonTool.stringify(values)) :192` | `TestErpQaSpcSampling` 强（mean=30/range=40 派生） | ✅ | **接受** |

**UC-QA-11 整体裁决：P2**（取最高）。**接受 on 主路径**（聚合 + 不重复存 + 单一真相源 + subgroupSize + measuredValues 全实现强测）；P2 on AC-3（samplingFrequency 死字段）。**§4 三判据复核**：spc.md `§关键决策` 声明 samplingFrequency 是聚合维度之一，实仓代码零读取 → 字段是死元数据。**Q4 张力评估**：实际采样节奏由全局 nop-job cron `0 0 * * * ?`（每小时）决定，**字段 samplingFrequency 是 UI/语义信息但运行时无消费**——主路径（subgroupSize 维度聚合）正确，AC-3 字面"subgroupSize **+** samplingFrequency"的"+"未完全满足（次要不完全满足）→ §2 P2①。修复 = 或在 `collectSamples` 中按 samplingFrequency 过滤/分组，或在 owner doc 显式标注「samplingFrequency 仅作 UI 显示，实际节奏由全局 cron 决定」（后者纯文档修复预授权自动执行）。

### UC-QA-12 质量看板

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① KPI 卡片值 == 实时聚合（非硬编码） | `use-cases.md:213-215` | `ErpQaDashboardBizModel.getDashboardKpi:67-96`（流式聚合 + daoProvider 直访） | `TestErpQaDashboard` 5 @Test 强（数值等值）+ E2E `quality.value.spec.ts` 强 | ✅ 实时聚合，无硬编码 | **接受** |
| ② 按期间过滤 | `use-cases.md:214` | `getDashboardKpi:68-69/:73-74` + `loadInspectionsInRange:352-358`（ge/le inspectionDate） | `TestErpQaDashboard` 强 | ✅ | **接受** |
| ③ 按 orgId 过滤 | `use-cases.md:214` | **所有 QueryBean 零 orgId**（loadInspectionsInRange / countOpenNcrs / countOutOfControlCharts / countInadequateCapabilityCharts / countOpenSpcNcrs） | （无 orgId 过滤测试） | ❌ orgId 维度无过滤 | **P1** → **复用 P1-MA2-093**（R1.29 resolved + SP-1） |
| ④ 按权限过滤 | `use-cases.md:214` | `IServiceContext context` 全文零引用；类不继承 CrudBizModel → 平台行级安全不施加 | （无行级权限测试） | ❌ 行级权限无强制 | **P1** → **复用 P1-MA2-093**（R1.29 resolved + SP-1） |
| ⑤ 预警项 == 满足阈值条件（阈值来自系统配置，非硬编码） | `use-cases.md:218` | `findCapaOverdueAlert:166-194`（阈值 `AppConfig.var(CONFIG_DASH_QA_CAPA_OVERDUE_DAYS, ...) :168-170` ✅ config 化）；SPC 失控预警 `getSpcOutOfControlWarning:208-225`（基于 isOutOfControl 物化字段，无硬编码阈值） | `TestErpQaDashboard` + `TestErpQaDashboardSpc` 强 | ✅ CAPA 阈值 config；SPC 预警无阈值需求（基于状态字段） | **接受** |
| ⑥ 看板数据受行级权限约束（只看自己组织/部门/成本中心） | `use-cases.md:221` | （与 AC-4 同根因） | （与 AC-4 同） | ❌ | **P1** → **复用 P1-MA2-093**（R1.29 resolved + SP-1） |

**UC-QA-12 整体裁决：P1（复用 P1-MA2-093，不新建）**。**接受 on 主路径**（KPI 实时聚合 + 期间过滤 + CAPA 阈值 config 化）；P1 on AC-3/4/6 行级权限（复用 P1-MA2-093，A2.18 `:101` 显式列 ErpQaDashboardBizModel，R1.29 全局 ErpOrgIsolationQueryTransformer resolved，与 A1.7/A1.11/A1.21/A1.24/A1.27 行级权限复用先例一致，追加 RC A1.33 交叉引用注记不新建）+ SP（R1.29 是否覆盖 ErpQaDashboardBizModel 直访路径交 MA4 A4.1 运行时展开）。

**不合格原因 TOP 的 dispositionType 替代 defectType**（`findDefectTopN:142-143` GROUP BY dispositionType，Javadoc `:56-57` 显式声明「defectType 未物化」）：是 UC-QA-12 AC-1「实时聚合」的子维度实现细节，L1 `use-cases.md:215` 字面「不合格原因 TOP」未指定具体字段名（defectType 还是 dispositionType）。dispositionType 是规范枚举（SCRAP/RETURN/CONCESSION/DOWNGRADE），语义最接近"不合格原因"。倾向 P2（次要验收标准未完全满足——主路径聚合 OK，但"原因"语义维度偏弱，dispositionType 实际是"处置决定"非"原因"）→ **P2-RC-047**（独立 finding，独立于行级权限复用）。

### 矩阵结论汇总

| UC | 整体裁决 | 主 finding |
|----|---------|-----------|
| UC-QA-09 | **P1** | P1-RC-042（自动调度链断裂）+ P2-RC-044（CAPA 1:1 + actionType 硬编码 + severity LOW 死分支） |
| UC-QA-10 | **P2**（接受 on 主路径） | P2-RC-045（QualityGoal 名称回写 + RiskRegister 硬编码值） |
| UC-QA-11 | **P2**（接受 on 主路径） | P2-RC-046（samplingFrequency 死字段） |
| UC-QA-12 | **P1**（复用，接受 on 主路径） | 复用 P1-MA2-093（R1.29 resolved + SP-1）+ P2-RC-047（dispositionType 替代 defectType） |

**零 P0**。**1 新 P1 + 4 新 P2 + 1 复用 P1**。

### §4 三判据复核汇总（P1 项强制）

| P1 候选 | (i) plan-audit | (ii) owner doc documented simplification | (iii) product-scope 裁剪 | 裁决 |
|---------|---------------|------------------------------------------|------------------------|------|
| P1-RC-042（UC-QA-09 自动调度链断裂） | ❌（无独立 plan-audit 将 batch 漏调 evaluateRules 裁决为简化） | ❌（spc.md §关键流程 完整描述自动链路，未声明「evaluateRules 步骤 Deferred」） | ❌（product-scope 未裁剪 SPC 失控预警自动级联） | **P1 强制实现**（Q4=(a)，三判据均不成立；修复=纯调度接线预授权自动执行不触 §5 ask-first） |
| UC-QA-12 AC-3/4/6 行级权限（复用 P1-MA2-093） | （沿用 A2.18 + R1.29 已 resolved） | （沿用） | （沿用） | **复用 P1-MA2-093**（R1.29 resolved + SP-1） |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

按 §7 规则，本报告产出 finding 前已 grep `arm-index.md` 同域（quality）同控制点（spc / capability / dashboard / samplingFrequency / ruleSet / afterCommit / evaluateRules），裁决如下：

### 6.1 复用既有 finding（追加 RC A1.33 交叉引用，不新建）

| Finding ID | 报告 | 复用理由 |
|-----------|------|---------|
| **P1-MA2-093** | `2026-07-28-1510-arm-ma2-multi-company-isolation.md` | UC-QA-12 AC-3/4/6 看板 orgId/行级权限投影**同根因同控制点**（11 dashboard BizModel 经 IDaoProvider 直访绕过认证管道，A2.18 `:101` 显式列 `ErpQaDashboardBizModel`）；R1.29 全局 `ErpOrgIsolationQueryTransformer` resolved；与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 / A1.24 UC-AST-12③ / A1.27 UC-INV-11 行级权限复用先例一致。**追加 RC A1.33 交叉引用注记**，不新建。SP：R1.29 是否覆盖 ErpQaDashboardBizModel 直访路径交 MA4 A4.1 运行时展开（SP-1）。 |

### 6.2 新建 finding（与既有 arm-index 无同根因同控制点）

| Finding ID | 报告 | 域 | UC | 描述 | 分级判据 | 目标 MR | 修复状态 |
|-----------|------|---|----|------|---------|--------|---------|
| **P1-RC-042** | rc-ma1-a1-33-quality-f3-spc-dashboard | quality | UC-QA-09 AC-4/5 | **SPC 自动调度链断裂（spc-sampling.batch.xml 漏调 evaluateRules，自动 NCR/CAPA 级联在生产调度路径不可达）**：L1（`use-cases.md:155-157`）逐字要求"检查 violatedRules → 子组.isOutOfControl == true → 事件驱动创建 NCR(sourceType=SPC)"，"事件驱动"隐含自动链路。L3 实仓：`spc-sampling.batch.xml:23-24` processor 段仅调 `samplingService.collectSamples(item.id, ...)` + `controlLimitCalculator.recalculate(item.id)`，**未调 `spcRuleEngine.evaluate(item.id, ...)`** → 生产调度路径（nop-job cron `0 0 * * * ?` → batch → collectSamples+recalculate）**永不触达**规则评估 → 失控样本不会自动 isOutOfControl=true → 不会触发 `SpcOutOfControlHandler.cascadeNcrAndCapa` afterCommit → NCR/CAPA 自动级联断裂。规则评估仅经手动 @BizMutation `ErpQaSpcChartBizModel.evaluateRules:63-65` 可达（运维须手工逐图调 evaluate）。**新根因**（既有 arm-index 全分区 grep `spc|evaluateRules|scheduler|batch` 无 finding 涉及 SPC scheduler 接线断点；P1-MA2-065 是 dict 死状态维度，P1-MA2-086 是 job 并发幂等维度，P1-MA2-064 是业务作废联动维度，三者不同控制点）。**§4 三判据均不成立**（spc.md §关键流程 完整描述自动链路未声明 Deferred + 无 plan-audit + product-scope 未裁剪）→ Q4=(a) 强制实现。**非 P0**（不破坏活跃数据：失控样本不创建 NCR 仅意味告警延迟，不导致库存/会计错误写入；非核心循环断裂：质检主路径 + NCR-CAPA 手动闭环完整；非会计过账破坏）。**与 P1-MA2-086（job 并发幂等）不同控制点**（P1-MA2-086 = spc-sampling append sample 重复副作用，本 finding = batch 未调 evaluateRules；同一 batch XML 但不同断点）。 | §2 P1①（功能实质偏离验收标准——自动链路在生产调度路径不可达）+ §2 P1②（异常路径未实现——事件驱动自动级联） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = `spc-sampling.batch.xml` processor 段追加 `const ruleEngine = inject('spcRuleEngine'); ruleEngine.evaluate(item.id, batchChunkCtx.serviceContext);`，纯调度接线 + Bean 注入，按 roadmap 预授权类目[代码逻辑修复]可自动执行，**不触发 §5 ask-first**——不触及 ORM/会计过账核心路径；建议补 batch→evaluate→NCR 端到端测试覆盖 afterCommit 时序） |
| **P2-RC-044** | rc-ma1-a1-33-quality-f3-spc-dashboard | quality | UC-QA-09 AC-6/7 | **CAPA 1:1 非 per-ruleSet + actionType 硬编码字面量 + severity LOW 死分支**：L1（`use-cases.md:158-159`）逐字「按 ruleSet 创建 CAPA(Action) / severity 按 violatedRules 映射」。L3 实仓 `SpcOutOfControlHandler.createNcrAndAction:116-123`：1 NCR 对应 1 CAPA（**1:1**），**非按 ruleSet 逐条/按违规规则维度创建**；`action.setActionType("CAPA")` `:119` 硬编码字面量（未引用字典或常量）；`mapSeverity:127-138` severity 字段经 violatedRules 派生主路径 OK（含"1"→HIGH / size≥2→CRITICAL / else→NORMAL），但 `SEVERITY_LOW :41` 声明**永不返回**（死分支——violatedRules 非空时含"1"返回 HIGH / size≥2 返回 CRITICAL / 否则 NORMAL，无任何分支返回 LOW）；severity 码为硬编码 Java 常量 `:41-44`，未校验 `erp-qa/severity` 字典码值一致性。主路径（CAPA 创建 + severity 派生）✅，AC-6 字面"按 ruleSet"维度未实现 + 字面量硬编码 + 死分支。**§4 三判据核验**：spc.md 未声明 1:1 简化；非 P0（不破坏主路径）。**与 P2-RC-040（UC-QA-07 类别级模板解析缺失）不同控制点**（不同 UC、不同 Processor）。 | §2 P2①（次要验收标准未完全满足，主路径[1:1 CAPA + severity 派生]OK 边界[per-ruleSet 维度 + LOW 死分支]弱） | successor watch-only（P2 登记不强制） | todo（修复 = a) CAPA per-ruleSet 维度化（按 violatedRules 拆分创建多 CAPA）+ actionType 引用常量；b) 删除 `SEVERITY_LOW :41` 死分支或为 mapSeverity 增加 LOW 返回路径（如单规则且非"1"时返回 LOW 而非 NORMAL）；c) severity 码引用 `erp-qa/severity` 字典常量。纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，**不触发 §5 ask-first**） |
| **P2-RC-045** | rc-ma1-a1-33-quality-f3-spc-dashboard | quality | UC-QA-10 AC-5/6 | **QualityGoal 回写按名称约定查找（非 FK）+ 无匹配静默 no-op + RiskRegister likelihood/severity/riskScore 硬编码值（非按 Cpk 量级派生）+ 能力分析调度 30 天窗口硬编码**：L1（`use-cases.md:180`）逐字「若 < ACCEPTABLE → 回写 QualityGoal.currentValue + 创建 RiskRegister」。L3 实仓：`SpcCapabilityCalculator.writeBackQualityGoal:282-294` 按 `eq("code", chart.getCode())` **名称约定查找（非 FK）** `:284-287`，无匹配 goal **静默 no-op** `:288-290`（运维配置不齐时不告警）；`registerRisk:296-310` `likelihood=3/severity=4/riskScore=12` **硬编码值** `:305-307`（非按 Cpk 量级派生，如 Cpk=0.5 vs Cpk=0.9 同为 INADEQUATE 但 risk 量级不同却获同分）；`spc-capability.batch.xml:22-23` `periodFrom = today.minusDays(30)` **硬编码 30 天窗口**（非按 chart 周期/日历月对齐）。主路径（INADEQUATE 触发回写 + RiskRegister 创建）✅，AC-5/6 字面"回写 QualityGoal"+ "创建 RiskRegister"行为存在但**实现脆弱**（名称约定 + 硬编码值）；调度窗口偏离"周期任务"字面。**§4 三判据核验**：spc.md §ErpQaSpcCapability 未声明名称约定/硬编码值为 documented simplification。**与 P1-MA2-065 不同维度**（P1-MA2-065 = dict 死状态 + CRUD 桩行为维度，resolved R1.20；本 finding = 需求契约维度，QualityGoal 回写脆弱 + RiskRegister 硬编码值，同一控制点但不同审计轴投影，§去重协议）+ **与 A1.32 P2-RC-043（ErpQaAction verificationResult 字段缺失）不同控制点**（不同实体/UC）。 | §2 P2①（次要验收标准未完全满足，主路径[回写 + RiskRegister 创建]OK 边界[名称约定 + 硬编码值]弱） | successor watch-only（P2 登记不强制） | todo（修复 = a) QualityGoal 回写按显式 FK 关联而非名称约定 + 无匹配抛 WARN 而非静默 no-op；b) RiskRegister likelihood/severity/riskScore 按 Cpk 量级派生（如 Cpk<0.5 → likelihood=4/severity=5；Cpk 0.5-1.0 → likelihood=3/severity=4）；c) 能力分析调度窗口按 chart 字段或 config 派生。修复触及 ORM 结构变更[QualityGoal FK]须 ask-first；其余纯 BizModel+config 代码逻辑预授权自动执行） |
| **P2-RC-046** | rc-ma1-a1-33-quality-f3-spc-dashboard | quality | UC-QA-11 AC-3 | **samplingFrequency 死字段（持久化列但运行时零读取）**：L1（`use-cases.md:194`）逐字「按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample」。L3 实仓：`ErpQaSpcChart.samplingFrequency` 是 ORM 持久化列（`app-erp-quality.orm.xml`）+ getter `_ErpQaSpcChart.java`，但全 `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/` grep `samplingFrequency\|getSamplingFrequency` **零命中**——实际采样节奏由全局 nop-job cron `0 0 * * * ?`（每小时）决定，**字段是死元数据**。主路径（subgroupSize 维度聚合）✅；AC-3 字面"subgroupSize **+** samplingFrequency"的"+"未完全满足。**§4 三判据核验**：spc.md §关键决策 未声明 samplingFrequency 仅 UI 显示。**Q4 张力**：实际节奏由全局 cron 决定 = 行为实现（聚合周期存在），AC-3 字面要求 samplingFrequency 字段参与聚合 = 字段未被消费；属"次要验收标准未完全满足"。 | §2 P2①（次要验收标准未完全满足，主路径[subgroupSize 聚合]OK 边界[samplingFrequency 字段未被消费]弱） | successor watch-only（P2 登记不强制） | todo（修复 = a) `collectSamples` 中按 samplingFrequency 过滤/分组（如按"日/周/月"切分子组）；b) owner doc `spc.md §关键决策` 显式标注「samplingFrequency 仅作 UI 显示，实际节奏由全局 cron 决定」（纯文档修复预授权自动执行）；c) 若 samplingFrequency 真无业务语义则删除字段[触及 ORM ask-first]。选 (a) 或 (b) 均可，(b) 为最小变更） |
| **P2-RC-047** | rc-ma1-a1-33-quality-f3-spc-dashboard | quality | UC-QA-12 AC-1（不合格原因 TOP 子维度） | **不合格原因 TOP 以 dispositionType 替代 defectType（defectType 未物化）**：L1（`use-cases.md:215`）字面「不合格原因 TOP」（未指定字段名）。L3 实仓 `ErpQaDashboardBizModel.findDefectTopN:142-143` DB GROUP BY **dispositionType**（SCRAP/RETURN/CONCESSION/DOWNGRADE），Javadoc `:56-57` 显式声明「defectType 未物化，以 dispositionType 为聚合维度——语义最接近且为规范枚举」。dispositionType 实际是"不合格处置决定"（如何处置不合格品），非"不合格原因"（为什么不合格——如尺寸超差/外观缺陷/功能失效等）。主路径（聚合实现 + DB GROUP BY 优化）✅，但"原因"语义维度弱（实际是"处置"维度）。**§4 三判据核验**：dashboards.md §质量看板 未声明 dispositionType 替代为 documented simplification。**Q4 张力**：聚合功能存在（KPI 实时聚合），仅语义维度偏离（处置 vs 原因）；属"次要验收标准未完全满足"。 | §2 P2①（次要验收标准[不合格原因字段语义]未完全满足，主路径[聚合 + GROUP BY 优化]OK 边界[语义维度弱]弱） | successor watch-only（P2 登记不强制） | todo（修复 = a) ORM `ErpQaNonConformance` 增 `defectType`/`defectReason` 字段[触及 ORM ask-first] + NCR 创建时填写 + findDefectTopN 改用 defectType；b) owner doc `dashboards.md §质量看板` 显式标注「defectType 未物化，本期以 dispositionType 为近似聚合维度」+ Javadoc 已声明可引用（纯文档修复预授权自动执行）。选 (b) 为最小变更） |

### 6.3 双向可追溯

- **新 finding 入 arm-index**：本报告产出即同步更新 `arm-index.md`（4 条新 finding P1-RC-042 / P2-RC-044 / P2-RC-045 / P2-RC-046 / P2-RC-047 入 RC 发现追踪区；audit reports 表新增 A1.33 行）。
- **修复行引用 finding**：MR1 的 RC-R1.n 修复行须含本 finding ID 交叉引用（修复落地后回填 arm-index）。
- **MV V.3 校验**：closure audit 核验全部 P0/P1 RC finding 修复状态均为 done 或显式 successor（本切片 P1-RC-042 todo + UC-QA-12 行级权限复用 P1-MA2-093 R1.29 resolved）。

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

每存疑点一行；无运行时确认手段的静态分歧交 MA4 探针展开。

- **SP-1（UC-QA-12 AC-3/4/6，复用 P1-MA2-093 SP）**：R1.29 全局 `ErpOrgIsolationQueryTransformer` 是否覆盖 `ErpQaDashboardBizModel` 的 `daoProvider.daoFor(...).findAllByQuery(q)` 直访路径（11 dashboard BizModel 之一）。`ErpOrgIsolationQueryTransformer` 是全局 IQueryTransformer，按 bizObj 的 orgId 列自动追加 `eq("orgId", currentUserOrgId)`，理论覆盖所有 QueryBean；但需运行时确认（多组织种子 + 跨组织查询断言空）。（与 A1.7 SP-4 / A1.11 SP-3 / A1.21 SP / A1.24 SP-4 / A1.27 SP 同根因）
- **SP-2（UC-QA-09 AC-5）**：`afterCommit` 钩子在真实调度事务（nop-job cron batch chunk transactionScope=chunk）下的 NCR 创建时序与并发幂等。`SpcOutOfControlHandler.cascadeNcrAndCapa:83` 注册 `transactionTemplate.afterCommit(null, () -> createNcrAndAction(...))`，post-commit hook 在 chunk 事务提交后执行——同 chart 多样本并发触发时，幂等预检 `findExistingSpcNcr:96/140-151` 是否能正确去重（无锁，理论存在竞态窗口）。JunitAutoTestCase 下经反射绕过未验证（`TestErpQaSpcOutOfControl:46-48`）。
- **SP-3（UC-QA-09 AC-4/5，P1-RC-042 修复后）**：调度 job `enabled=true` 时 batch→evaluateRules 缺失的实际运行时表现（cron 触发 → collectSamples+recalculate 执行，但样本永不被评估为 isOutOfControl → 失控预警永不触发）。P1-RC-042 修复落地后此 SP 自动消解。
- **SP-4（UC-QA-10 AC-5）**：QualityGoal 名称不匹配时回写静默 no-op 的运行时确认（chart.code 与 qualityGoal.code 不一致时，writeBackQualityGoal `:288-290` 静默返回，无 WARN/异常——运维误以为回写成功）。
- **SP-5（UC-QA-09 AC-6/7）**：CAPA 1:1 在多违规规则场景下的运行时业务影响（如同一样本违反规则 1+规则 2，理论应生成"纠正 + 预防"两类 CAPA，实际只生成 1 CAPA）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决，无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（HEAD 本审计执行时）

> 本审计为**只读审计**（无代码/ORM/api.xml/view.xml/batch.xml/job.yaml/真相源变更），故 checker 无回归风险。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

**结论**：全 19 规则 actual ≤ baseline，零漂移。本审计无生产代码变更，无回归风险。

---

## 9. 与既有 MA2 报告差异增量声明（重申，见报告开头）

按 §去重协议，本报告不复跑 MA2 状态机/多公司行为审计，直接复用：
- **A2.12**（`2026-07-28-1020-arm-ma2-quality-state-machine.md`）：SPC 引擎 5 组件生命周期 1:1 对应 + 计数型分支 + P1-MA2-065 dict 死状态 resolved R1.20。
- **A2.18**（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）：P1-MA2-093 看板 orgId/行级权限 resolved R1.29（ErpQaDashboardBizModel 在 A2.18 `:101` 显式列出）。

本切片只补的需求视角差异（不复跑行为本身）：
1. UC-QA-09 自动调度链断裂（**新根因**，P1-RC-042）；
2. UC-QA-09 CAPA 1:1 + actionType 硬编码 + severity LOW 死分支（P2-RC-044）；
3. UC-QA-10 QualityGoal 名称回写 + RiskRegister 硬编码值 + 调度 30 天窗口（P2-RC-045）；
4. UC-QA-11 samplingFrequency 死字段（P2-RC-046）；
5. UC-QA-12 不合格原因 dispositionType 替代（P2-RC-047）；
6. UC-QA-12 看板 orgId/行级权限（**复用 P1-MA2-093**，不新建；与 A1.7/A1.11/A1.21/A1.24/A1.27 复用先例一致）。

**未修改真相源声明**（§9 真相源冻结条款）：本审计未修改 `product-scope.md` / `quality/use-cases.md` / `spc.md` / `dashboards.md` 的任何需求契约内容；发现的分歧记入本报告 §5/§6，未直改真相源。

---

## 报告 9 段完整性自检

- [x] §1 需求契约原文（UC-QA-09/10/11/12 验收标准逐字引用 + 断言计数）
- [x] §2 实现代码证据（含行号 + 跨域调用链）
- [x] §3 测试证据（注明断言强度 + 反射绕过 afterCommit 注记）
- [x] §4 运行时行为证据（复用 A2.12 + A2.18 + 本切片 L5 判读）
- [x] §5 符合性结论（五级追踪矩阵 + 每 UC 结论 + §4 三判据复核）
- [x] §6 与 arm-index 衔接（1 复用 + 5 新建 + 双向可追溯）
- [x] §7 静态存疑点清单（5 SP 交 MA4 展开）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明（置顶 + 重申）

9 段齐全，完整性自检通过。
