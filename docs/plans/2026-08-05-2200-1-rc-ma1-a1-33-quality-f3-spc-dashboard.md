# 2026-08-05-2200-1 rc-ma1-a1-33-quality-f3-spc-dashboard quality 域 quality-F3 SPC 与看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.33（MA1 需求追踪矩阵审计 — quality-F3 SPC 失控预警/过程能力分析/数据聚合 + 质量看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.33
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.33 的 0.2 依赖）、`2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md`（quality 域同批，F1 检验门控）、`2026-08-05-1830-3-rc-ma1-a1-32-quality-f2-ncr-capa-closure.md`（quality 域同批，F2 NCR-CAPA 闭环为 SPC 失控预警的下游 NCR 创建衔接）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.33 给出 UC 清单 = `UC-QA-09/10/11/12`（4 UC），含 `use-cases.md:148/166/187/207` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 quality 域第三个 RC 切片（quality 域共 3 切片 A1.31/A1.32/A1.33），含 SPC 引擎三条用例（失控预警/能力分析/数据聚合）+ 质量看板。

- **L1 需求契约（权威真相源）**：`docs/design/quality/use-cases.md`（机制见 `spc.md §关键流程`、`../dashboards.md §质量看板`）：
  - **UC-QA-09 SPC 失控预警**（`:148`）：SPC 采样(nop-job) → 聚合 InspectionLine.measuredValue → ErpQaSpcSample；控制图计算(≥20 子组) → 检查 violatedRules；子组.isOutOfControl == true → 事件驱动创建 NCR(sourceType=SPC) → 按 ruleSet 创建 CAPA(Action) → severity 按 violatedRules 映射。
  - **UC-QA-10 SPC 过程能力分析**（`:166`）：周期任务 → 计算 ErpQaSpcCapability（Cp=(USL-LSL)/(6×σ̂)，σ̂=R̄/d2；Cpk=min((USL-X̄̄),(X̄̄-LSL))/(3×σ̂)）；capabilityLevel 阈值 Cpk<1.0→INADEQUATE / 1.0-1.33→ACCEPTABLE / 1.33-1.67→CAPABLE / >1.67→EXCELLENT；若 < ACCEPTABLE → 回写 QualityGoal.currentValue + 创建 RiskRegister。
  - **UC-QA-11 SPC 数据从 InspectionLine 聚合**（`:187`）：SPC 采样任务 → 从 ErpQaInspectionLine.measuredValue 聚合(不重复存)；按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample；原始读数仍在 InspectionLine(单一真相源)；SpcSample.measuredValues 是聚合后的子组统计。
  - **UC-QA-12 质量看板**（`:207`）：KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤) — 质检数/合格率/不合格数/开放NCR/合格率趋势/不合格原因TOP/SPC失控/CAPA逾期预警；预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)；看板数据受行级权限约束(只看自己组织/部门/成本中心)。
  - **L1 关键不变量**：① 失控预警是**事件驱动**链（采样→控制图→违规检测→自动 NCR/CAPA）；② ≥20 子组门槛；③ Cpk 阈值分级 + 能力不足回写质量目标/风险登记；④ 数据聚合不重复存（InspectionLine 单一真相源）；⑤ 看板行级权限约束。

- **L3 代码实现现状（实测）**——SPC 引擎已大量实现（非 stub），但有调度链断点；看板实时聚合实现但缺权限维度：
  - **UC-QA-09 失控预警链（⚠️ 调度链断点 + 手动路径可用）**：
    - 采样调度：`app-erp-all/.../nop/job/conf/erp-qa-spc-sampling.job.yaml:1-13`（jobName `erp-qa-spc-sampling`，cron `0 0 * * * ?`，`enabled` 默认 false）→ `module-quality/.../nop/batch-task/qa/spc-sampling.batch.xml:10-26`（loader 读 `ErpQaSpcChart isActive=true`；processor 注入 spcSamplingService + spcControlLimitCalculator，调 `samplingService.collectSamples` + `controlLimitCalculator.recalculate`）。
    - **⚠️ 关键缺口**：`spc-sampling.batch.xml:21-25` 调度链**仅调 collectSamples + recalculate，未调 evaluateRules** → 生产调度路径**永不触达**规则引擎 / NCR-CAPA 级联。规则评估仅经手动 @BizMutation `ErpQaSpcChartBizModel.evaluateRules:61-65`（委托 `SpcRuleEngine.evaluate`）可达。
    - 规则引擎：`SpcRuleEngine.java:82-133 evaluate(...)`（UCL/LCL/CL null 时早返回 `:89-92`；Western Electric 规则 1-4 纯函数 `evaluateRules` `:106`/body `:144-242`；写回 `violatedRules`/`isOutOfControl` `:116-121`）。
    - NCR 事件驱动创建：`SpcRuleEngine.java:122-130`（isOutOfControl 时调 `outOfControlHandler.cascadeNcrAndCapa`，try/catch 不中止循环）→ `SpcOutOfControlHandler.java:78-91 cascadeNcrAndCapa` 注册 `transactionTemplate.afterCommit(null, () -> createNcrAndAction(...))`（`:83`，config-gated `ErpQaConfigs.isSpcAutoNcrEnabled()` `:80` 默认 true）；NCR 创建 `:99-114`，`ncr.setSourceType(NCR_SOURCE_TYPE_SPC)` `:103`，`setStatus(NCR_STATUS_OPEN)` `:108`，幂等预检 `:96`/`:140-151`。
    - CAPA 创建：`SpcOutOfControlHandler.java:116-123`（`action.setActionType("CAPA")` **硬编码字面量** `:119`，`setStatus(ACTION_STATUS_PENDING)` `:122`）——**1 NCR 1 CAPA（1:1），非"按 ruleSet/按违规规则"逐条**。
    - severity 映射：`SpcOutOfControlHandler.java:127-138 mapSeverity`（空→NORMAL `:128-130`；size≥2→CRITICAL `:131-133`；含"1"→HIGH `:134-136`；else→NORMAL）；`SEVERITY_LOW` 声明于 `:41` 但**永不返回**（死分支）；severity 码为硬编码 Java 常量 `:41-44`，未校验 `erp-qa/severity` 字典。
  - **UC-QA-10 能力分析（✅ 公式/阈值全实现，⚠️ 回写脆弱）**：
    - 调度：`erp-qa-spc-capability.job.yaml:1-13`（cron `0 0 2 * * ?`，enabled 默认 false）→ `spc-capability.batch.xml:19-26`（periodFrom = today.minusDays(30) **硬编码 30 天窗口** `:22-23`，非按 chart 周期/日历月对齐）。
    - 计算器：`SpcCapabilityCalculator.java:77 calculateCapability`（持久化 ErpQaSpcCapability `:161-180`）；σ̂=R̄/d2 经 `:147`→`SpcControlLimitCalculator.estimateWithinStdDev:200-209`；USL/LSL = `chart.getSpecMax()/getSpecMin()` `:149-150`；Cp `:233-238`、Cpk `:240-247`（含 Pp/Ppk `:153-154` + Cpm `:155`）。
    - 阈值分级：`SpcCapabilityCalculator.classifyByCpk:265-280`（与 L1 **逐字一致**：<1.0 INADEQUATE / <1.33 ACCEPTABLE / <1.67 CAPABLE / else EXCELLENT）。
    - 回写：`SpcCapabilityCalculator.java:183`（!isAttributes && INADEQUATE 时触发）→ QualityGoal 回写 `:282-294`（**按 `eq("code", chart.getCode())` 名称约定查找，非 FK** `:284-287`；无匹配 goal 静默 no-op `:288-290`；`setCurrentValue(cpk)` `:292`）+ RiskRegister 创建 `:296-310`（`risk.setStatus("OPEN")` `:308`；**likelihood=3/severity=4/riskScore=12 硬编码** `:305-307`，非按 Cpk 量级派生）；均 try/catch WARN 不中止持久化 `:186-193`。
  - **UC-QA-11 数据聚合（✅ 不重复存，⚠️ samplingFrequency 死字段）**：
    - 聚合读：`SpcSamplingService.java:105 collectSamples`；候选行 `findApprovedInspectionLines:366-404`（`eq("parameterId")` `:370`、APPROVED inspection 过滤 `:388`）；`parseMeasuredValue:466-480`（读 `line.getMeasuredValue()` `:467`，非数值 warn+skip `:475-479`）；幂等 sampledKeys `:142-143/:156-159`（sourceCode#sourceLineCode#value 三元组去重）。
    - SpcSample 构建：`:181-206`（mean/range/stdDev `:194-199`、measuredValues JSON `:192`、sourceBillType=ERP_QA_INSPECTION `:201`）；InspectionLine **从不被写回**（单一真相源 ✅）。
    - **⚠️ 缺口**：`subgroupSize` 用于 `:122/:179`；但 `samplingFrequency` 是 ORM 持久化列（`app-erp-quality.orm.xml:770`，getter `_ErpQaSpcChart.java:1233`）却**被 SPC 全部代码零读取**（grep `service/spc/` `samplingFrequency|getSamplingFrequency` 零命中）——实际节奏由全局 nop-job cron 决定，**字段是死元数据**。
  - **UC-QA-12 质量看板（✅ 实时聚合，❌ 权限维度缺失）**：
    - 后端 `ErpQaDashboardBizModel.java`（plain `@BizModel :60`，**不继承 CrudBizModel**）：`getDashboardKpi:67-96`（inspectionCount=inspections.size() `:77`、accepted/rejected 流式计数 `:78-83`、passRate `:84`、openNcrCount `:85`）；`getDashboardTrend:99-131`；`findDefectTopN:134-160`（DB GROUP BY dispositionType `:143`，**非 defectType**——Javadoc `:56-57` 显式声明「defectType 未物化，以 dispositionType 为聚合维度」）；`getSpcOutOfControlWarning:208-225`；`findCapaOverdueAlert:166-194`（阈值 `AppConfig.var(CONFIG_DASH_QA_CAPA_OVERDUE_DAYS, ...)` `:168-170` ✅ config 化）。
    - **期间过滤 ✅**（`:68-69/:73-74/:355-356` ge/le inspectionDate）；**orgId 过滤 ❌**（所有 QueryBean 零 orgId，`loadInspectionsInRange:352-358`/`countOpenNcrs:360-367`/`countOutOfControlCharts:285-294`/`countInadequateCapabilityCharts:331-340`/`countOpenSpcNcrs:342-350`）；**行级权限 ❌**（`IServiceContext context` 在每个 @BizQuery 声明却**全文零引用**；类不继承 CrudBizModel → 平台行级安全不施加于 `daoProvider.daoFor(...).findAllByQuery(q)` 直访）。
  - **跨域 daoFor**：SPC 代码（`service/spc/`）跨域 daoFor **零命中**（仅 quality 域实体）。质量域其他位置（NcrPostingDispatcher/NcrReturnOrchestrator/ErpQaReportBizModel）跨域只读归 P1-MA1-022 todo MR1，本切片不重审。

- **L4 测试证据现状**（`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/{spc,dashboard}/`）：
  - **强（纯函数）**：`spc/TestSpcStatistics.java`（5 @Test，BigDecimal 精确等值）、`spc/TestSpcRuleEnginePure.java`（8 @Test，违规规则 Set 精确等值，覆盖规则 1-4 + ruleSet 子集过滤）、`spc/TestSpcCapabilityFormulas.java`（6 @Test，Cp/Cpk/Cpm 精确值 + `classifyByCpkThresholds:45` 直接证明 UC-QA-10 AC-3 阈值边界）、`spc/TestErpQaSpcAttributesControlLimit.java`（7 @Test，P/NP/C/U 图精确 CL + LCL<0 钳位）。
  - **强（DB 集成）**：`spc/TestErpQaSpcSampling.java`（6 @Test，含 `recalculateComputesControlLimitsWhen20Subgroups:111` 证明 ≥20 门槛、`recalculateKeepsPendingWhenLessThan20:132`、幂等、mean=30/range=40 精确）、`spc/TestErpQaSpcAttributesSampling.java`（5 @Test，defectCount/inspectedCount 精确 + P-CL=0.2）、`spc/TestErpQaSpcCapability.java`（3 @Test，含 `calculateCapabilityInadequateWritesBackGoalAndRegistersRisk:82` 证明 UC-QA-10 AC-4 回写 + RiskRegister category=SPC_PROCESS_CAPABILITY）。
  - **混合**：`spc/TestErpQaSpcOutOfControl.java`（4 @Test，NCR+CAPA 断言强，但**经反射 `invokeCreateNcrDirectly:165-174` 绕过 `afterCommit`**——Javadoc `:46-48` 显式声明 post-commit hook 在 JunitAutoTestCase 下不可靠）。
  - **强（看板）**：`dashboard/TestErpQaDashboard.java`（5 @Test，passRate=0.5/openNcrCount=2/SCRAP=3 数值等值）、`dashboard/TestErpQaDashboardSpc.java`（5 @Test，去重 chart 计数 + config-gate 差分）、`dashboard/TestErpQaDashboardSpcChart.java`（7 @Test，cl/ucl/lcl 透传 + 排序 + config 覆盖）。
  - **E2E**：`tests/e2e/dashboards/quality.value.spec.ts`（强值断言 inspectionCount=3/passRate=0.6666/openNcrCount=3/outOfControlChartCount=1/...）、`dashboards/qa-dashboard-spc-attributes.value.spec.ts`（强值断言 P/NP/C/U 图 CL 排序 UCL≥CL≥LCL + 子组≥20）、`dashboards/quality.smoke.spec.ts`（仅冒烟）。
  - **⚠️ 测试缺口**：① `afterCommit` 事件驱动路径零端到端覆盖（UC-QA-09 AC-3 timing/并发幂等未验证）；② 无测试覆盖调度链端到端（job.yaml→batch.xml→...→NCR），故 batch XML 漏调 evaluateRules 未被捕获；③ 无测试覆盖看板行级权限/orgId 过滤（与功能缺失一致）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12）：SPC 控制图生命周期经实仓确认（`spc.md §关键流程` → `SpcSamplingService`/`SpcControlLimitCalculator`/`SpcOutOfControlHandler`/`SpcCapabilityCalculator` 1:1 对应，`:419`）；**P1-MA2-065**（SPC calcStatus STALE 死状态 + RiskRegister MITIGATED/CLOSED + QualityGoal 全 4 态 dict 死状态，CRUD 桩）resolved R1.20；**P2-MA2-063**（state-machine.md 缺 SPC 独立章节 watch-only）。
  - `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18）：**P1-MA2-093**（orgId 查询隔离全仓未落地，11 dashboard BizModel 经 IDaoProvider 直访绕过认证管道，**含 ErpQaDashboardBizModel**）resolved R1.29（全局 `ErpOrgIsolationQueryTransformer`）。本切片 UC-QA-12 AC-1/4 行级权限投影**同根因同控制点**（与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 行级权限复用先例一致）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用 P1-MA2-065（SPC 死状态 resolved）+ P1-MA2-093（看板行级权限 resolved R1.29）已证实行为，只补需求视角差异（调度链 evaluateRules 断点 / samplingFrequency 死字段 / QualityGoal 名称约定回写 / RiskRegister 硬编码值 / 不合格原因 dispositionType 替代 / R1.29 是否覆盖 ErpQaDashboardBizModel 路径）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-065`（SPC 死状态 resolved R1.20）、`P2-MA2-063`（state-machine.md 缺 SPC 章节 watch-only）、`P1-MA2-093`（看板 orgId/行级权限 resolved R1.29）。**RC 系列对 quality SPC/看板为零**（本切片为 quality 域首批 RC 切片之一）。本切片须 grep arm-index qa spc/capability/dashboard/samplingFrequency/ruleSet/afterCommit 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（调度链断点 / samplingFrequency 死字段 / QualityGoal 名称回写 / RiskRegister 硬编码 / dispositionType 替代）均属**代码逻辑 + 调度配置**类（预授权——BizModel/batch.xml/job.yaml 调整）；若回写要求新增 FK 列则触及 ORM ask-first。

- **剩余差距**：A1.33 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.33 报告并登记 finding，解除 quality 域 SPC+看板切片证据缺口。

## Goals

- 产出 A1.33 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-33-quality-f3-spc-dashboard.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-QA-09/10/11/12 逐条核验**每条验收标准**（完整枚举，§3）：失控预警事件驱动链/≥20 门槛/severity 映射、Cp/Cpk 公式/阈值分级/能力不足回写、聚合不重复存/subgroupSize+samplingFrequency、看板实时聚合/orgId 过滤/阈值 config 化/行级权限 全链逐条。
- 对候选缺口给出分级结论：①调度链漏调 evaluateRules 致自动级联断裂（倾向 **P1**，新根因——scheduler 接线断点，仅手动路径可达）；②CAPA 1:1 非 per-ruleSet + actionType 硬编码（倾向 **P2**）；③QualityGoal 名称约定回写（倾向 **P2**）；④RiskRegister likelihood/severity/riskScore 硬编码（倾向 **P2**）；⑤samplingFrequency 死字段（倾向 **P2**）；⑥看板 orgId/行级权限（**复用 P1-MA2-093** resolved R1.29 + SP：R1.29 ErpOrgIsolationQueryTransformer 是否覆盖 ErpQaDashboardBizModel 路径）；⑦不合格原因 dispositionType 替代（倾向 **P2**）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调序号，最新 P2-RC-043/P1-RC-041）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/spc.md/dashboards.md/product-scope.md）。
- **不修改代码/ORM/api.xml/batch.xml/job.yaml**（只读审计）。
- **不审计其他 MA1 切片**（A1.31 quality-F1 / A1.32 quality-F2 独立 plan；A1.33 只覆盖 UC-QA-09/10/11/12）。
- **不复审质检单/NCR-CAPA 主路径**（UC-QA-01~08 属 A1.31/A1.32 done；本切片仅核 SPC 引擎 + 看板）。
- **不重审 P1-MA2-065 SPC 死状态**（§去重协议：resolved R1.20，只补需求视角差异[scheduling 断点等]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.33 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.33 UC 锚点）+ `docs/design/quality/use-cases.md`（L1 真相源）+ `docs/design/quality/spc.md` + `docs/design/dashboards.md §质量看板`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.12/A2.18 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-quality/erp-qa-service -Dtest=TestErpQaSpcOutOfControl,TestErpQaSpcCapability,TestErpQaDashboard,TestErpQaDashboardSpc`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-33-quality-f3-spc-dashboard.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-QA-09/10/11/12 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:148/166/187/207` 验收标准原文；L2 引用 `spc.md §关键流程`/`§ErpQaSpcCapability`/`§关键决策` + `dashboards.md §质量看板`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `SpcSamplingService`/`SpcControlLimitCalculator`/`SpcRuleEngine`/`SpcOutOfControlHandler`/`SpcCapabilityCalculator`/`ErpQaSpcChartBizModel`/`ErpQaDashboardBizModel` + `spc-sampling.batch.xml`/`spc-capability.batch.xml`/`*.job.yaml`（含行号）；L4 引用上述测试类#method（注明断言强度 + 反射绕过 afterCommit 注记）；L5 复用 A2.12（P1-MA2-065 resolved）+ A2.18（P1-MA2-093 resolved R1.29）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-QA-09 ①采样聚合（collectSamples:105 + InspectionLine:467）②≥20 门槛（SpcControlLimitCalculator:101）+ **违规检测调度断点**（spc-sampling.batch.xml:21-25 漏调 evaluateRules——自动级联断裂，仅手动 evaluateRules:61-65 可达）③isOutOfControl→NCR sourceType=SPC（SpcRuleEngine:122-130 + SpcOutOfControlHandler:99-114 afterCommit:83）④CAPA per ruleSet（**实为 1:1 非 per-rule，actionType 硬编码 "CAPA" :119**）⑤severity 映射（mapSeverity:127-138，LOW 死分支）；UC-QA-10 公式/阈值（classifyByCpk:265-280 逐字一致）+ **回写 QualityGoal 名称约定（:284-287 非 FK）+ RiskRegister 硬编码值（:305-307）**；UC-QA-11 不重复存（InspectionLine 不写回）+ **samplingFrequency 死字段（零读取）**；UC-QA-12 实时聚合 + 期间过滤 + CAPA 阈值 config 化 + **orgId 过滤缺失 + 行级权限缺失（IServiceContext 全文零引用，不继承 CrudBizModel）+ 不合格原因 dispositionType 替代（:143/:56-57）**。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 4 UC 给出符合性结论（取最高）：UC-QA-10 公式/阈值 **接受**；UC-QA-09 调度级联断裂 → 倾向 **P1**（验收标准②③"事件驱动"自动链在生产调度路径不可达，行为实质偏离）；UC-QA-11 samplingFrequency 死字段 → 倾向 **P2**（次要验收标准，subgroupSize 已用）；UC-QA-12 行级权限 → **复用 P1-MA2-093**（同根因同控制点，resolved R1.29）+ SP 核 R1.29 覆盖；其余 CAPA/ruleSet/QualityGoal 名称/RiskRegister 硬编码/dispositionType → 倾向 **P2**。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（P1 项核 plan-audit/owner doc documented simplification/product-scope 裁剪）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-QA-09/10/11/12 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度（含反射绕过 afterCommit 注记）、L5 标注复用 A2.12/A2.18 来源
- [x] 4 UC 各有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-⑦ 有明确分级；调度级联断裂有 §4 三判据复核路径；看板行级权限有复用 P1-MA2-093 裁决 + SP

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-33-quality-f3-spc-dashboard.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` qa spc/capability/dashboard/samplingFrequency/ruleSet/afterCommit/evaluateRules 同域同控制点后裁决——看板行级权限**复用 P1-MA2-093**（追加 RC A1.33 交叉引用，不新建）；调度级联断裂为**新根因**（既有 arm-index 无 finding 涉及 SPC scheduler 接线断点）→ 若确认 P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调，最新 P2-RC-043/P1-RC-041）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 R1.29 ErpOrgIsolationQueryTransformer 是否覆盖 ErpQaDashboardBizModel 直访路径 / afterCommit 在真实调度事务下的 NCR 创建时序与并发幂等 / 调度 job enabled=true 时 batch→evaluateRules 缺失的实际运行时表现 / QualityGoal 名称不匹配时回写静默 no-op 的运行时确认 等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-quality-state-machine.md`（A2.12 P1-MA2-065 SPC 死状态 resolved + spc.md 1:1）+ `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18 P1-MA2-093 看板行级权限 resolved R1.29），列明只补的需求视角差异（调度级联 evaluateRules 断点 / samplingFrequency 死字段 / QualityGoal 名称回写 / RiskRegister 硬编码 / dispositionType 替代）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.33 行。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（若有）已写入 `arm-index.md`；P1-MA2-093 追加 RC A1.33 交叉引用；静态存疑点清单已登记（供 A4.1/A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_030955928ffeI5I8n4DM1mOWay，fresh session，未起草本计划）。全部 load-bearing 引用经实仓复核 CONFIRMED：①`spc-sampling.batch.xml:21-25` 漏调 evaluateRules（glob 确认仅 2 qa batch + 2 qa job，无 spc-evaluate-rules；grep evaluateRules *.xml 零命中）——核心 P1 候选真实；②`SpcOutOfControlHandler.java:83` afterCommit + `:103/:108/:119` sourceType=SPC/OPEN + actionType 硬编码 "CAPA" + `:41` SEVERITY_LOW 死分支；③`SpcCapabilityCalculator.classifyByCpk:265-280` 阈值与 UC-QA-10 逐字一致 + `:285-287` QualityGoal 名称约定 + `:305-307` RiskRegister 硬编码值；④`ErpQaDashboardBizModel` 不继承 CrudBizModel（plain @BizModel :59-60）+ `IServiceContext context` 全文零引用（grep `context\.` 仅 import :19）+ 全 QueryBean 零 orgId + `findDefectTopN:143` GROUP BY dispositionType + Javadoc :56-57 defectType 未物化；⑤`service/spc/` grep `samplingFrequency|getSamplingFrequency` 零命中。P1-MA2-093 复用先例（A2.18:101 显式列 ErpQaDashboardBizModel）与 A1.7/A1.11/A1.21/A1.24/A1.27 同型。scope（UC-QA-09/10/11/12 only，只读）、anti-slack（零禁用词，倾向 P1/P2 为方法论预裁决语言）、exit criteria（不重复全量 build/test，只读审计删除验证门控已 justification）、methodology §1-§9 + §4 三判据 + §7 reuse-or-new + §10 MR0/MR1 全对齐。仅 2 处亚行精度非阻塞注记（batch.xml :21-25 含 CDATA closer :25；@BizModel :59 vs 类声明 :60），不影响可验证性。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/batch.xml/job.yaml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.33 报告 9 段齐全 + UC-QA-09/10/11/12 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.33 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（调度级联断点 / samplingFrequency 死字段 / QualityGoal 名称回写 / RiskRegister 硬编码 / CAPA per-rule / dispositionType 替代）均属**代码逻辑 + 调度配置**类（预授权——BizModel/batch.xml/job.yaml 调整，不涉及 ORM 结构变更）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: A1.33 quality-F3 SPC 与看板需求符合性审计完成。产出审计报告 `docs/audits/2026-08-05-2200-1-rc-ma1-a1-33-quality-f3-spc-dashboard.md`（9 段齐全）。UC-QA-09/10/11/12 主路径行为经 A2.12 SPC 引擎 5 组件生命周期 1:1 + A2.18 看板 orgId/行级权限 resolved R1.29 双重复用证实，只补需求视角差异。结论：UC-QA-10 公式/阈值（`classifyByCpk:265-280` 与 L1 逐字一致）**接受 on 主路径**，回写脆弱+硬编码值 P2；UC-QA-11 聚合不重复存+单一真相源+subgroupSize+measuredValues **接受 on 主路径**，samplingFrequency 死字段 P2；UC-QA-09 采样聚合+≥20 门槛+手动规则评估+NCR/CAPA 创建主路径 **接受 on 主路径**，但**自动调度链断裂**（`spc-sampling.batch.xml:23-24` 漏调 evaluateRules，生产调度路径永不触达规则评估→NCR/CAPA 自动级联断裂，仅手动可达）= **P1-RC-042** 新登记（§4 三判据均不成立→Q4=(a) 强制实现；修复=纯调度接线预授权不触 §5 ask-first）+ CAPA 1:1 非 per-ruleSet + actionType 硬编码 + severity LOW 死分支 = P2；UC-QA-12 KPI 实时聚合+期间过滤+CAPA 阈值 config 化 **接受 on 主路径**，行级权限 **复用 P1-MA2-093**（R1.29 resolved + SP 交 MA4）+ dispositionType 替代 defectType P2。**零 P0**。**1 新 P1（P1-RC-042）+ 4 新 P2（P2-RC-044/045/046/047）+ 1 复用 P1（P1-MA2-093）**。arm-index 已更新（A1.33 报告行 + 5 finding 行 + A1.33 交叉引用注记 + P1-MA2-093 RC A1.33 交叉引用）。roadmap A1.33 todo→done。本审计为只读审计（无代码/ORM/api.xml/view.xml/真相源变更），checker 实测可见规则（R1a-R2d/R6/R7/R10/R12a）actual 精确等于 baseline（无生产代码变更，无回归风险）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0300b6a52ffeI24bXWcYV2sPdL（新会话，fresh session，未执行本计划）
- Evidence: VERDICT: PASS（8/8 checklist 全通过）。①报告 9 段齐全（§1:42/§2:125/§3:153/§4:201/§5:223/§6:306/§7:334/§8:346/§9 置顶:13+重申:377）；②4 UC 结论正确（UC-QA-09=P1/UC-QA-10=P2 accept on 主路径/UC-QA-11=P2 accept on 主路径/UC-QA-12=P1 reuse P1-MA2-093，零 P0）；③L1 逐字引用核验（UC-QA-09 报告 §1 lines 47-54 与 use-cases.md:154-159 逐字一致）；④5 新 finding（P1-RC-042 @ arm-index:193/P2-RC-044 @ :194/P2-RC-045 @ :195/P2-RC-046 @ :196/P2-RC-047 @ :197）+ A1.33 报告表行 @ :101 + A1.33 交叉引用注记 @ :203 + P1-MA2-093 行 @ :409 追加 A1.33 交叉引用；⑤**P1-RC-042 代码断言 ACCURATE**（`spc-sampling.batch.xml:21-24` processor 调 collectSamples:23 + recalculate:24，**未调 evaluateRules**，finding 核实）；⑥无真相源修改（git diff use-cases.md/spc.md/dashboards.md/product-scope.md = empty，工作树仅 docs/audits/+docs/plans/+docs/backlog/）；⑦§8 含 checker actual-vs-baseline 表 + 独立性声明 + 交叉去重声明；⑧§4 三判据复核 P1-RC-042（报告 lines 237-241+297-302）+ §6 reuse-or-new 裁决（P1-MA2-093 复用 §6.1，5 新建含差异依据 §6.2）。
