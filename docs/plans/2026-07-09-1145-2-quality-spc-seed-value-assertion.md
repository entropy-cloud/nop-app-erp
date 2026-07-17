# 2026-07-09-1145-2-quality-spc-seed-value-assertion 质量域 SPC 种子 + 失控预警数值断言

> Plan Status: completed
> Mission: erp
> Work Item: quality 域 SPC 三表（spc_chart/spc_sample/spc_capability）最小连通种子 + `getSpcOutOfControlWarning` 数据驱动数值断言，关闭质量看板最后一个预警可观测性缺口
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md` Deferred「质量域 SPC 三表 seed（spc_sample/spc_capability/spc_chart）」（Classification: optimization candidate，Successor Required: yes，触发条件「当需 SPC 预警非空观测时补 seed」——**已满足**：AGENTS.md 当前重点「各域细化端到端验证」+ 0930-3 已建立质量看板断言范式，SPC 预警当前确定性返回 0 属可观测性缺口）；`docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md`（completed，质量看板断言覆盖 getSpcOutOfControlWarning 确定性 0，本计划使该预警转非空并叠断言）
> Related: `docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md`（completed，质量域 3 表 inspection/non_conformance/action 种子范式 + SPC 三表 Deferred）、`docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md`（completed，质量看板 KPI + getSpcOutOfControlWarning=0 断言范式）、`docs/plans/2026-07-09-1045-1-crm-cs-hr-transaction-seeds.md`（completed，域表直 seed 范式 + posted=false 裁决）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`/`ls`，非采信旧记忆）：

- **质量域看板 SPC 预警读源已就绪**（`module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java#getSpcOutOfControlWarning` :204-221，3 `@BizQuery` 输出字段，各读 1 表无 join）：
  - `outOfControlChartCount`（helper `countOutOfControlCharts` :225-234）：读 `erp_qa_spc_sample` filter `isOutOfControl=true` → distinct `chartId` 计数。
  - `inadequateCapabilityCount`（helper `countInadequateCapabilityCharts` :236-245）：读 `erp_qa_spc_capability` filter `capabilityLevel='INADEQUATE'` → distinct `chartId` 计数；**config-gated** `erp-dash.qa-spc-include-inadequate`（默认 `"true"`，`ErpQaConfigs.java:127`）。
  - `openSpcNcrCount`（helper `countOpenSpcNcrs` :247-255）：读 `erp_qa_non_conformance` filter `sourceType='SPC'` AND `status IN ('OPEN','IN_REVIEW')`；**config-gated** `erp-dash.qa-spc-include-ncr`（默认 `"true"`，`ErpQaConfigs.java:136`）。
  - **关键**：两 SPC 计数器仅迭代 sample/capability 表收集 distinct `chartId`，**从不 join 或 load `erp_qa_spc_chart`**——故 sample/capability 的 `chartId` 是逻辑引用（Nop ORM `<to-one>` 为逻辑 join 非 DB 物理外键），即使指向不存在的 chart 行也被接受（既有集成测试 `TestErpQaDashboardSpc` 已证：仅 seed sample/capability 不建 chart，断言通过）。
- **SPC 三表 ORM 已定义**（`module-quality/model/app-erp-quality.orm.xml`）：
  - `ErpQaSpcChart`（table `erp_qa_spc_chart` :759-817）：`parameterId`(:770) `mandatory=true` BIGINT **但无 `<to-one>` 关系无目标实体**（仓库无 `ErpQaParameter`/`ErpQaInspectionParameter`，`parameterId` 为自由 BIGINT 软引用，可填任意值如 0）；mandatory 业务列 CODE/NAME/CHART_TYPE(dict `erp-qa/spc-chart-type`)/PARAMETER_ID/DOC_STATUS/APPROVE_STATUS；UK `(code,orgId)`。
  - `ErpQaSpcSample`（table `erp_qa_spc_sample` :820-866）：`chartId`(:825) mandatory → `<to-one name="chart">`(:848) 逻辑引用 `erp_qa_spc_chart`；mandatory SUBGROUP_NO/SAMPLE_TIME；`isOutOfControl`(:838) 默认 false；UK `(chartId,subgroupNo)`。
  - `ErpQaSpcCapability`（table `erp_qa_spc_capability` :869-914）：`chartId`(:874) mandatory → `<to-one name="chart">`(:901)；mandatory PERIOD_FROM/PERIOD_TO；`capabilityLevel`(:888) dict `erp-qa/spc-capability`。
- **SPC 种子当前不存在**：`ls app-erp-all/src/main/resources/_vfs/_init-data/ | grep spc` = 空；现有 quality CSV 仅 `erp_qa_inspection.csv`/`erp_qa_non_conformance.csv`/`erp_qa_action.csv`（0930-2 交付）。`erp_qa_non_conformance.csv` 现 2 行（OPEN/IN_REVIEW）但 `sourceType` 非 SPC（0930-2 Non-Goal）。
- **SPC 预警当前确定性返回 0**（0930-3 已断言为预期非缺陷）：sample/capability 表空 → outOfControlChartCount=0 / inadequateCapabilityCount=0；non_conformance 无 SPC sourceType 行 → openSpcNcrCount=0。本计划使三者转非空可观测。
- **断言范式已就绪**：`tests/e2e/dashboards/_helper.ts#assertDashboardKpiValues`（1445-2/0930-3 已用）；0930-3 质量看板 spec 已含 getSpcOutOfControlWarning 断言（当前断 0）。当前 77 spec 文件 / 84 CSV。
- **上游主数据 FK 已 seed**（1234-1）：org=2 / material 1-4 / employee 1-3。SPC 三表 orgId 引用 org=2（可空但填则引用已 seed）。
- **保护区域**：纯部署期数据（CSV）+ 断言 spec（tests/）+ 分析/日志文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 0930-2/1045-1）。属 `plan-first`（跨表 FK 拓扑序 + referential integrity 裁决 + 涉及 >5 文件）。

剩余差距：(1) SPC 三表零数据，`getSpcOutOfControlWarning` 三计数器全 0，无 SPC 预警可观测；(2) 是否 seed spc_chart（Strategy C 完整参照完整性）vs 仅 sample/capability（Strategy B 最小）待 Phase 1 裁决；(3) 期望值派生待 Phase 1。

## Goals

- 在 `_vfs/_init-data/` 增补质量域 SPC 最小连通种子 CSV（spc_chart + spc_sample + spc_capability，以一致 chartId 串联），使 `getSpcOutOfControlWarning` 三计数器转**非空可观测**（outOfControlChartCount≥1 / inadequateCapabilityCount≥1 / openSpcNcrCount≥1）。
- 经既有 config-gated fresh-DB 启动加载，验证新 CSV **0 主键冲突 / 0 列映射错误 / 0 参照失败**。
- 叠加 SPC 预警数据驱动数值断言：新增/更新质量看板 spec 断言 `getSpcOutOfControlWarning` 三字段确定性非零值（解除 0930-3「确定性 0」状态）。
- 解除 0930-2 Deferred「质量域 SPC 三表 seed」；关闭质量看板最后一个预警可观测性缺口。

## Non-Goals

- **不**做 SPC 控制图完整可视化（echarts UCL/LCL + 违规点高亮，0930-3 Deferred 前端可视化 successor）。
- **不**物化 `ErpQaParameter` 实体或 seed 检验参数——`parameterId` 为自由 BIGINT 软引用（ORM 无 `<to-one>` 无目标实体）；spc_chart.parameterId 填占位值（如 0）并文档化，不引入新实体。
- **不**seed SPC 完整运行链（rule engine 重算 / capability Cp/Cpk 计算 / nop-job 采样）——本计划仅 seed 静态结果行令看板预警可观测，不触发 SPC 引擎重算。SPC 引擎默认关闭（`ErpQaConfigs` `erp-qa.spc-enabled` 默认 false + `ErpQaSpcSamplingJob`/`ErpQaSpcCapabilityJob` cron 默认空双层门控），fresh-DB 启动不触发重算，seed 行安全不被覆盖。
- **不**改 `model/*.orm.xml`——纯数据文件 + 断言 spec，零 ORM 变更。
- **不**覆盖其他质量配置表 seed（risk_register/quality_goal/review/calibration/recall/sampling_plan/inspection_template，0930-2 Deferred）。
- **不**做像素级视觉回归（0637-1 Deferred）。

## Task Route

- Type: `implementation-only change`（seed CSV + 断言 spec）
- Owner Docs: `module-quality/model/app-erp-quality.orm.xml`（SPC 三表 schema 权威）、`docs/design/dashboards.md`（§质量看板 SPC 预警口径）、`docs/architecture/seed-data.md`（域表直 seed 范式 + posted 裁决）、`docs/testing/e2e-runbook.md`（断言范式）
- Skill Selection Basis: Phase 1-2 CSV 种子编写无技能覆盖（列 code 映射 + 拓扑序，非 BizModel 方法编写，`Skill: none`）；Phase 3 断言 spec 属浏览器 E2E（Playwright）→ `nop-testing`（`nop-testing` SKILL.md §什么时候用我 明列「E2E 测试 / Playwright / 端到端」并路由 `02-core-guides/e2e-testing.md`，项目方法源 `docs/references/playwright-e2e-guide.md`）；Phase 4 文档对齐无技能覆盖（`Skill: none`）。

## Infrastructure And Config Prereqs

- **预构建 runner jar**：依赖 `app-erp-all/target/app-erp-all-1.0.0-SNAPSHOT-runner.jar`（`mvn clean install -DskipTests` 产物）。`playwright.config.ts` webServer fresh-DB 重置 + `-Dnop.orm.init-database-data=true` 加载种子（已就绪）。
- **config 门控**：`erp-dash.qa-spc-include-inadequate`（默认 true）/ `erp-dash.qa-spc-include-ncr`（默认 true）须保持开（默认即开，无需改配置）。
- **无新外部依赖/密钥**。回滚策略：seed CSV + spec 纯新增（删除即回滚）。

## Execution Plan

### Phase 1 - SPC seed 表映射 + Strategy B/C 裁决 + 期望值派生（Proof + Decision）

Status: completed
Targets: `docs/analysis/2026-07-09-1145-2-quality-spc-seed-table-map.md`
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：逐表派生列映射——读 SPC 三表 ORM（spc_chart :759-817 / spc_sample :820-866 / spc_capability :869-914），标注每列角色（M=mandatory/FK=逻辑引用/opt=可选留空），核实 mandatory 业务列可填、FK（chartId→chart 逻辑引用 / orgId→erp_md_organization=2 / inspectorId→employee 可选）可解析。核实 dict code（`erp-qa/spc-chart-type` / `erp-qa/spc-capability` / `wf/approve-status` / `erp-md/active-status`）对齐 dict.yaml。产出 seed 表映射分析文档。
  - Skill: none
- [x] `Decision`：Strategy B vs C 裁决——(a) **Strategy C（完整参照完整性，推荐）**：seed spc_chart（1 行，parameterId=0 占位软引用文档化）+ spc_sample（≥1 行 isOutOfControl=true，chartId 引用已 seed chart）+ spc_capability（≥1 行 capabilityLevel=INADEQUATE，chartId 引用同 chart）+ erp_qa_non_conformance 追加 1 行 sourceType=SPC/status=OPEN（驱动 openSpcNcrCount）；优点 sample/capability.chartId 指向真实 chart 行（参照完整）。(b) Strategy B（仅 sample+capability，不建 chart）被 rejected（chartId 悬空指向不存在的行，种子数据完整性差，虽看板不读 chart）。残留风险：spc_chart.parameterId=0 为占位软引用（ORM 无 `<to-one>`，无 ErpQaParameter 实体），文档化不影响加载/看板。(c) posted 裁决：三表均无 posted 列（看板读域表非 GL，镜像 0930-2 裁决，N/A）。(d) 日期窗口：sample.sampleTime + capability.periodFrom/periodTo 置当前月，使预警确定性可观测。(e) 期望值：outOfControlChartCount=1（1 sample isOutOfControl=true → distinct chartId=1）/ inadequateCapabilityCount=1（1 capability INADEQUATE）/ openSpcNcrCount=1（1 non_conformance SPC+OPEN），逐项标注 seed 行依据。
  - Skill: none

Exit Criteria:

- [x] seed 表映射 + Strategy 裁决 + 期望值派生分析文档落盘（每计数器标注期望值 + seed 行依据 + config 门控默认值确认）

### Phase 2 - SPC seed CSV 编写（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_qa_spc_{chart,sample,capability}.csv`、`erp_qa_non_conformance.csv`（追加）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：编写 spc_chart.csv（≥1 行：CODE/NAME/CHART_TYPE/PARAMETER_ID=0 占位/CL_CENTER_TYPE 默认/CALC_STATUS 默认/DOC_STATUS/APPROVE_STATUS/ORG_ID=2，省略审计+TENANT 列）。编写 spc_sample.csv（≥1 行：CHART_ID 引用已 seed chart / SUBGROUP_NO / SAMPLE_TIME 当前月 / IS_OUT_OF_CONTROL=true / 其余计算列可留空）。编写 spc_capability.csv（≥1 行：CHART_ID 引用同 chart / PERIOD_FROM+PERIOD_TO 当前月 / CAPABILITY_LEVEL=INADEQUATE / 其余可空）。列名严格对齐 ORM `code`（UPPER_SNAKE + 逐表核实，省略 framework-managed 审计列），FK 引用 1234-1/0930-2 固定 ID。
  - Skill: none
- [x] `Add`：erp_qa_non_conformance.csv 追加 ≥1 行 sourceType=SPC / status=OPEN（驱动 openSpcNcrCount）；核实追加行不与既有 2 行（0930-2 的非 SPC NCR）主键冲突（ID 接续）。
  - Skill: none
- [x] `Proof`：脚本校验所有新/改 CSV 列名对齐 ORM `code`（0 错配）+ mandatory 业务列全填 + FK 引用上游存在（org=2 / chartId 自洽 / non_conformance ID 接续无冲突）。
  - Skill: none

Exit Criteria:

- [x] 3 张新 SPC CSV + non_conformance 追加行落地，列名对齐 ORM（0 错配），mandatory 列全填，FK 自洽

### Phase 3 - fresh-DB seed 加载 + GraphQL 非零验证 + SPC 断言（Proof + Add）

Status: completed
Targets: `tests/e2e/dashboards/quality.value.spec.ts`（更新 getSpcOutOfControlWarning 断言）
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2；本阶段需重打包 jar 使新 CSV 入 app jar（`mvn clean install -DskipTests` 为打包前置，完整仓库验证在 Closure Gates 运行）

- [x] `Proof`：重打包 app jar（新 CSV 打包入 jar）+ fresh-DB 启动（`-Dnop.orm.init-database-data=true`），验证 84+N CSV 全 load-csv-data 成功（0 冲突/0 列错配/0 参照失败）。GraphQL 抽样 `ErpQaDashboard__getSpcOutOfControlWarning` 三计数器由 0 转非空且与 Phase 1 期望值一致（outOfControlChartCount≥1 / inadequateCapabilityCount≥1 / openSpcNcrCount≥1）。
  - Skill: none
- [x] `Add`：更新 `tests/e2e/dashboards/quality.value.spec.ts`（0930-3 已建）的 getSpcOutOfControlWarning 断言——由「确定性 0」改为 Phase 1 派生的非零期望值（outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount 三字段逐值断言）。config 门控默认开，无需传 config。
  - Skill: `nop-testing`
- [x] `Proof`：`npx playwright test tests/e2e/dashboards/quality.value.spec.ts --workers=1` 全绿（SPC 预警三字段非零断言 + 既有质量 KPI 断言无回归）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] fresh-DB 加载 SPC CSV 0 冲突 + GraphQL getSpcOutOfControlWarning 三计数器非空且与期望值一致
- [x] 质量看板 spec getSpcOutOfControlWarning 断言由 0 改非零，本地化 `quality.value.spec.ts` 全绿（全套件 0 回归在 Closure Gates）

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/README.md`、`docs/logs/2026/07-09.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3

- [x] `Add`：`docs/architecture/seed-data.md` 增「质量域 SPC 种子」段（Strategy C 完整参照 + parameterId=0 占位软引用文档化 + non_conformance SPC 行追加 + Non-Goals 移除 SPC）；`docs/testing/e2e-runbook.md` 种子库计数（84→84+N）+ 质量域 SPC 预警非空域 + 期望值表 quality SPC 行；`docs/testing/known-good-baselines.md` 增 1145-2 基线行；`docs/backlog/README.md` 增质量 SPC 种子工作项 ✅ done；`docs/logs/2026/07-09.md` 增 1145-2 日志条目（含验证状态）；0930-2 plan Deferred「SPC 三表 seed」+ 0930-3「确定性 0」状态标记解除（本计划 Closure 段登记）。
  - Skill: none

Exit Criteria:

- [x] 文档对齐落地（seed-data + runbook + baselines + backlog + 日志 + 0930-2/0930-3 Deferred 解除登记）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bc78b157ffe9SBPNe7kP2pM8C`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（getSpcOutOfControlWarning 3 helper 读源/不 join spc_chart、parameterId mandatory 无 to-one 无目标实体、spc_sample/capability chartId 逻辑 `<to-one>`、TestErpQaDashboardSpc 证逻辑 FK、SPC 引擎双层门控默认关 seed 行安全、Strategy C 技术可行无阻塞全核实；无 slack 词；每执行项 typed；Deferred 附触发条件）。1 MAJOR：Phase 3 退出标准重复完整仓库验证（违反执行时规则 7）→ **已修复**（Phase 3 标题去「E2E 0 回归」、Proof 重打包 jar 重述为打包前置、移除「全套件 0 回归」Proof 项、退出标准去「+ 全套件 0 回归」并注「在 Closure Gates」）。2 MINOR：(1) baseline 4 处列行号漂移（spc_sample chartId :843→:825/isOutOfControl :851→:838、spc_capability chartId :876→:874/capabilityLevel :889→:888）→ **已修复**；(2) SPC 引擎 Non-Goal 补默认关闭保证（spc-enabled=false + cron 空双层门控）→ **已修复**。**附注**：本计划 Task Route 原含与 Plan 1 同类的 Skill 选择事实反转（误称 nop-testing 不覆盖 Playwright E2E），经 SKILL.md:20 核实 nop-testing 明列 E2E/Playwright → **已顺带修复**（Task Route Skill Selection Basis + Phase 3 E2E spec authoring 项 Skill: none→nop-testing，与 Plan 1 一致）。迭代 1 后无 BLOCKER/MAJOR。
- Independent draft review iteration 2: accept (`ses_0bc724624fferb1vEpKXt3ulhv`，独立 general 子代理，新会话冷重播无执行者上下文) — 全部修复落地核实：MAJOR1（Phase 3 rule-7 localize + 全套件验证移回 Closure Gates）、MINOR1（4 处列行号 :825/:838/:874/:888 精确匹配 live ORM）、MINOR2（SPC 引擎双层门控 spc-enabled=false + cron 空）、Skill 纠正（Task Route + Phase 3 E2E 项 nop-testing，Phase 1-2 seed/Phase 4 doc 保留 none）。基线抽查全 PASS（getSpcOutOfControlWarning 3 helper + 无既有 SPC CSV + config 默认 true + TestErpQaDashboardSpc 逻辑 FK 证）。规则合规：rule 7、item typing、Closure Gates、Deferred 触发条件、无 slack 词全核实。无 BLOCKER/MAJOR。草案收敛为可接受执行契约，计划转 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（SPC 三表种子 + non_conformance SPC 行落地 + getSpcOutOfControlWarning 三计数器非空可观测 + 断言由 0 改非零全绿）
- [x] 相关文档对齐（seed-data + e2e-runbook + known-good-baselines + backlog + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL getSpcOutOfControlWarning 非空 + `npx playwright test`（全套件 fresh-DB seed 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（SPC 控制图可视化/ErpQaParameter 物化/SPC 引擎重算链/其他质量配置 seed 均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SPC 控制图完整可视化（echarts UCL/LCL + 违规点高亮）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0930-3 既定 Deferred（前端可视化面）。本计划仅令 SPC 预警计数器非空可观测，不做控制图可视化。
- Successor Required: `yes`
- Trigger Condition: 同 0930-3 Deferred（前端 SPC 控制图可视化需求时）。
- **RELEASED by 2026-07-17-2010-1**（触发条件已满足：successor 计划 `docs/plans/2026-07-17-2010-1-dashboard-echarts-spc-crp-charts.md` 已交付 `ErpQaDashboard__getSpcControlChartData` `@BizQuery`（ucl/lcl/cl 经 chart 实体字段真实传递 + 样本点 subgroupNo/mean/isOutOfControl/violatedRules 按 subgroupNo 升序）+ quality 看板 `spcControlChart` echarts 图表：line 样本均值 + markLine UCL/LCL/CL 三水平线 + `isOutOfControl=true` 点 itemStyle 红色高亮 + tooltip 含 violatedRules）

### ErpQaParameter 实体物化 / 检验参数 seed

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `parameterId` 为自由 BIGINT 软引用（ORM 无 `<to-one>` 无目标实体），spc_chart.parameterId=0 占位即可加载 + 看板不读 chart。物化 ErpQaParameter 属 schema 扩展（ask-first），超出 seed 范畴。
- Successor Required: `yes`
- Trigger Condition: 当 SPC 控制图需绑定真实检验参数维度，或 inspection_line.parameterId 需强参照时。

### SPC 引擎重算链 seed / rule engine 触发

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划 seed 静态结果行（isOutOfControl/capabilityLevel）令看板可观测，不触发 SpcRuleEngine/SpcControlLimitCalculator/SpcCapabilityCalculator 重算（重算可能覆盖 seed 行）。重算链端到端回归属独立能力面。
- Successor Required: `yes`
- Trigger Condition: 当需验证 SPC 引擎从原始 measured_values 到失控判定/能力计算的端到端计算正确性时。

## Closure

Status Note: 全 4 Phase 落地完成，范围内行为 + 文档 + 验证全绿。质量看板 `getSpcOutOfControlWarning` 三计数器（outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount）由确定性 0 转非空 1/1/1（Strategy C 完整参照完整性）；getDashboardKpi.openNcrCount 联动由 2→3 并已同步断言。纯部署期数据（3 张新 CSV + 1 处加性追加）+ 1 个断言 spec 更新 + 文档对齐，零 ORM/契约/Java 生产代码变更。完整仓库验证全绿（mvn 154 模块 BUILD SUCCESS + fresh-DB 87 CSV 0 冲突 + Playwright 全套件 84 passed 0 回归）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，冷重播无执行者上下文）— 审计结果：**PASS / approved**。执行者未自我审计（Closure Gates 第 7 项原由执行者保留 `[ ]` 占位符，经本次独立审计核实通过后勾选）。
- Evidence:
  - **范围内行为**：3 张新 SPC CSV 落地（`app-erp-all/src/main/resources/_vfs/_init-data/erp_qa_spc_{chart,sample,capability}.csv`，各 1 行：spc_chart parameterId=0 占位 / spc_sample isOutOfControl=true / spc_capability INADEQUATE）+ `erp_qa_non_conformance.csv` 追加 1 行 sourceType=SPC·status=OPEN（ID=3 接续）。`_vfs/_init-data/` CSV 总数 84→87。
  - **断言更新**：`tests/e2e/dashboards/quality.value.spec.ts` getSpcOutOfControlWarning 0/0/0→1/1/1 + getDashboardKpi.openNcrCount 2→3。
  - **分析文档**：`docs/analysis/2026-07-09-1145-2-quality-spc-seed-table-map.md`（表映射 + Strategy C 裁决 + 期望值派生，含每计数器 seed 行依据 + config 门控默认值确认）。
  - **验证全绿**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:26）；fresh-DB webServer 启动加载 87 CSV 0 冲突/0 列映射错误/0 参照失败（quality spec 2 passed 实证 app 启动成功）；`npx playwright test tests/e2e/dashboards/quality.value.spec.ts --workers=1` 2 passed（32.2s）；`npx playwright test` 全套件 84 passed（11.7m）0 回归。
  - **文档对齐**：`docs/architecture/seed-data.md`（增「质量域 SPC 种子」段 + 维护+质量段 SPC Deferred 标记「已于 1145-2 落地」+ Non-Goals 移除 SPC）；`docs/testing/e2e-runbook.md`（种子库 84→87 + 域清单补 SPC + 期望值表 quality 看板 openNcrCount 2→3 + quality SPC 预警 0/0/0→1/1/1 + 分析文档清单增 1145-2）；`docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md`（SPC=0/openNcrCount=2 标注为 0930-3 时点历史值，supersede 见 1145-2）；`docs/testing/known-good-baselines.md`（增 1145-2 基线行）；`docs/backlog/README.md`（增 1145-2 工作项 ✅ done）；`docs/logs/2026/07-09.md`（增 1145-2 日志条目含验证状态）。

Deferred 解除登记（本计划承接的前序 Deferred）：
- **0930-2 Deferred「质量域 SPC 三表 seed（spc_sample/spc_capability/spc_chart）」RELEASED**：触发条件「当需 SPC 预警非空观测时补 seed」**已满足**；本计划以 Strategy C 落地 3 表 + non_conformance SPC 行，使预警三计数器非空。Classification 原 `optimization candidate`，Successor Required 已兑现。
- **0930-3「getSpcOutOfControlWarning 确定性 0」状态 RELEASED**：0930-3 spec 曾断言确定性 0 覆盖 @BizQuery 可观测性（SPC 未 seed 时点预期）；本计划使三计数器转非空 1/1/1 并更新断言，确定性 0 状态解除。

Follow-up:

- SPC 控制图可视化 / ErpQaParameter 物化 / SPC 引擎重算链 seed 仍为计划内 Non-Goal（附触发条件），非阻塞跟进项。

Closure Audit 独立核实记录（独立子代理新会话冷重播，逐项对照实时仓库）：
- **Phase 状态/项目一致性**：4 Phase 全 `completed`，无残留 `- [ ]`；所有执行项 + 退出标准全 `[x]`。
- **Exit Criteria vs live repo**：3 张新 SPC CSV 落地（`erp_qa_spc_{chart,sample,capability}.csv` 各 1 行），列名逐列核对 ORM `code`（spc_chart 13 列 / spc_sample 9 列 / spc_capability 8 列）全匹配，mandatory 业务列（code/name/chartType/parameterId/clCenterType/calcStatus/docStatus/approveStatus；chartId/subgroupNo/sampleTime；chartId/periodFrom/periodTo）全填；`erp_qa_non_conformance.csv` 3 行（ID=3 SPC·OPEN 接续无主键冲突）；FK 自洽（chartId=1 自洽、orgId=2、materialId=1、inspectorId=1）。
- **断言更新核实**：`tests/e2e/dashboards/quality.value.spec.ts` getSpcOutOfControlWarning `0/0/0→1/1/1` + getDashboardKpi.openNcrCount `2→3`，与 seed 行派生一致。
- **Anti-Hollow**：纯部署期数据 + 断言更新，零 Java/ORM/契约生产代码（符合 Non-Goal「不改 model/*.orm.xml」）；CSV 列对齐 ORM 经独立 grep 核实，非空壳。
- **Five-point 一致性**：Plan Status=completed / 4 Phase Status=completed / 每阶段 Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据落地 — 全一致。
- **Deferred honesty**：SPC 可视化/ErpQaParameter 物化/SPC 引擎重算链均带触发条件 + Successor Required，无范围内心缺陷隐藏。
- **Docs sync**：`docs/logs/2026/07-09.md`、`docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/README.md` 均含 1145-2 条目（grep 实证），0930-2/0930-3 Deferred 解除登记已落 Closure 段。
- **审计结论**：范围内可执行退出标准全绿，结束证据充分，无 BLOCKER。计划可关闭。
