# 2026-07-07-0305-2 quality-spc-process-control

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.4b（UC-QA-09 SPC 统计过程控制 / UC-QA-10 SPC 规则引擎 / UC-QA-11 SPC 控制图）；承接 `docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md` 数据源前置 + `docs/plans/2026-07-06-1606-1-remaining-domain-dashboards-backend.md` Deferred「质量 SPC 失控预警 `ErpQaSpcSample` 缺失」
> Related: `2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（质检+NCR/CAPA 前置）、`2026-07-05-2352-2-ncr-financial-posting.md`（NCR 过账 successor）、`2026-07-06-1606-1-remaining-domain-dashboards-backend.md`（看板后置 successor）
> Audit: required

## Current Baseline

- **质检数据源已就绪**：`ErpQaInspection`（4 态状态机 posted）+ `ErpQaInspectionLine.measuredValue`（实测值，计量型数据）done 计划 2237-3。`ErpQaInspectionTemplate/TemplateLine`（parameterId 关联被控质量特性）存在。
- **NCR/CAPA 级联机制就绪**：`ErpQaNonConformance`（5 态状态机）+ `ErpQaAction`（CAPA 生命周期含效果验证门控）done 计划 2237-3；`ErpQaNonConformance.actions` to-many cascade-delete 已在 orm 中。NCR 财务过账 done 计划 2352-2（SCRAP/RETURN 处置）。失控预警可复用 `createForBusinessBill` 范式。
- **nop-job 已接线**：`app-erp-all/.../scheduler.yaml` 11 个 job，cron 经配置键门控范式已验证。
- **设计文档完整**：`docs/design/quality/spc.md`（106 行）定义三实体完整字段清单（`ErpQaSpcChart`/`ErpQaSpcSample`/`ErpQaSpcCapability`）+ 4 步流程 + 3 新字典 + 关键决策（从 InspectionLine 聚合不重复存原始值）。
- **三实体均未物化**：`grep ErpQaSpc module-quality/model` 返回 0 命中。`ErpQaDashboardBizModel`（1606-1）因 `ErpQaSpcSample` 缺失，SPC 失控预警指标裁定为 Non-Goal。
- **辅助实体存在性确认**：`ErpQaQualityGoal`（currentValue 回写）+ `ErpQaRiskRegister`（风险登记）存在（quality 域 CRUD 已 done）。
- **剩余差距**：控制图配置、样本聚合采样、控制限计算、Western Electric 判异规则引擎、失控 NCR/CAPA 级联、过程能力指数（Cp/Cpk/Pp/Ppk）全未实现。

## Goals

- 物化 `ErpQaSpcChart` / `ErpQaSpcSample` / `ErpQaSpcCapability` 三实体 + 3 字典（`erp-qa/spc-chart-type`、`erp-qa/spc-calc-status`、`erp-qa/spc-capability`），经 model→codegen 生成链。
- 实现 SPC 样本采集引擎：nop-job 定时扫描 APPROVED 的 `ErpQaInspectionLine`（命中 chart.parameterId）按 `subgroupSize`+`samplingFrequency` 聚合成 `ErpQaSpcSample`（mean/range/stdDev 计算字段 + sourceBillType 三元组反查，不重复存原始值）。
- 实现控制限计算与 Western Electric 判异规则引擎：子组数≥20 触发重算 chart.ucl/lcl/cl（系数 d2/D3/D4 按 subgroupSize 内置常量表）+ 每 sample.violatedRules/isOutOfControl 判定（规则 1~4：单点超 3σ / 连续 9 点同侧 / 连续 6 点递增递减 / 连续 14 点交替）。
- 实现失控预警级联：sample.isOutOfControl=true 经 `txn().afterCommit`（模式 B）创建 `ErpQaNonConformance`(sourceType=SPC) + `ErpQaAction`(CAPA)，severity 按 violatedRules 映射。
- 实现过程能力分析：周期性计算 `ErpQaSpcCapability`（Cp/Cpk/Pp/Ppk/Cpm + capabilityLevel 等级），等级低于 ACCEPTABLE 回写 `ErpQaQualityGoal.currentValue` + 登记 `ErpQaRiskRegister`。
- 解除 1606-1 Deferred「质量 SPC 失控预警 `ErpQaSpcSample` 缺失」数据源阻塞。

## Non-Goals

- SPC 看板后端 `@BizQuery` 失控预警查询接线 + 前端控制图卡片（独立 successor，触发条件=本计划 `ErpQaSpcSample` 落地；属 dashboard 结果面）。
- 计数型控制图（P/NP/C/U）的完整采样实现 —— 本计划聚焦计量型（X̄-R / X̄-s / I-MR），计数型 chartType 枚举预留但不实现计算（Decision：枚举完整、计算计量型先行，计数型归 successor）。
- 实时在线 SPC（设备直采 IoT 数据流）—— 本计划数据源限定 InspectionLine 离线检验实测值。
- 控制图 AMIS 可视化渲染前端（归前端 successor；nop-report 报表能力已就绪 0504-2/1247-3）。
- 失控 NCR 的财务过账（复用 2352-2 已 done 引擎；SCAP 处置经现有 NCR 流程，本计划不新增业务类型）。
- 自定义判异规则 DSL 编辑器（本计划内置 Western Electric 1~4，ruleSet 逗号选择；自定义规则归 successor）。

## Task Route

- Type: `implementation-only change`（owner 设计 done 于 `spc.md`，仅计数型计算范围一处 Decision）
- Owner Docs: `docs/design/quality/spc.md`（权威设计）、`docs/design/quality/inspection-integration.md`（数据源）、`docs/design/quality/state-machine.md`（NCR/CAPA 级联）
- Skill Selection Basis: 全部为后端 BizModel/IBiz/规则引擎/定时 job/NCR 级联/ErrorCode → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。必需输入（owner 设计、质检数据源、NCR/CAPA 范式）均就绪。

## Infrastructure And Config Prereqs

- nop-job 调度器已运行，新 job 仅需追加注册项。
- 新增配置键（`ErpQaConstants` 声明 + `NopSysVariable` 默认值）：`erp-qa.spc-sampling-cron`（采样周期，默认 `0 0 * * * ?` 每小时）、`erp-qa.spc-capability-cron`（能力分析周期，默认 `0 0 2 * * ?`）、`erp-qa.spc-enabled`（总开关默认 false）、`erp-qa.spc-auto-ncr-enabled`（失控自动建 NCR 默认 true）。
- 无新增 finance 业务类型（失控预警复用既有 NCR 机制）；无外部端口/密钥/数据迁移依赖。

## Execution Plan

### Phase 1 - ORM 模型物化与 codegen

Status: completed
Targets: `module-quality/model/app-erp-quality.orm.xml`、codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [x] Add: 在 `module-quality/model/app-erp-quality.orm.xml` 追加三实体（字段对齐 `spc.md` §实体清单）：`ErpQaSpcChart`（chartType/materialId/inspectionTypeId/parameterId/specMin/specMax/subgroupSize/samplingFrequency/clCenterType/ruleSet/alarmThreshold/ucl/lcl/cl/calcStatus/isActive/docStatus/approveStatus）、`ErpQaSpcSample`（chartId/subgroupNo/sampleTime/measuredValues JSON/mean/range/stdDev/sourceBillType+sourceCode+sourceLineCode/inspectorId/violatedRules/isOutOfControl）、`ErpQaSpcCapability`（chartId/periodFrom/periodTo/sampleCount/totalObservations/grandMean/overallStdDev/withinStdDev/cp/cpk/pp/ppk/cpm/capabilityLevel/isStable）。新增 3 字典 `erp-qa/spc-chart-type`、`erp-qa/spc-calc-status`、`erp-qa/spc-capability`。
  - Skill: `nop-backend-dev`
- [x] Add: 经 nop-cli 对 quality 域增量 codegen（dao entity + IBiz + meta + service/web 空壳 + action-auth），验证生成产物与既有 11 实体链一致。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三实体 + 3 字典在 orm.xml 通过 XDef 校验；quality 域 codegen 产物存在。
- [x] `mvn clean install -DskipTests -pl module-quality -am` BUILD SUCCESS（解除 Phase 2 编译依赖）。

### Phase 2 - 样本采集与控制限计算

Status: completed
Targets: `IErpQaSpcChartBiz`、`ErpQaSpcChartBizModel`、`IErpQaSpcSampleBiz`、`ErpQaSpcSampleBizModel`、`SpcSamplingService`、`SpcControlLimitCalculator`、`ErpQaSpcSamplingJob`、`ErpQaConstants`、`ErpQaErrors`、scheduler.yaml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] Add: `SpcSamplingService.collectSamples(chartId, context)` —— 扫描 APPROVED 的 `IErpQaInspectionLineBiz`（命中 chart.parameterId + 区间相交），按 chart.subgroupSize 聚合成 `ErpQaSpcSample`（measuredValues JSON 数组 + mean/range/stdDev 计算 + sourceBillType 三元组反查 + inspectorId）。幂等（同 subgroupNo 不重建，incremental 补缺）。**注意**：`ErpQaInspectionLine.measuredValue` 为 `VARCHAR(100)`（非 DECIMAL），采样引擎须字符串→数值解析，非数值/空值经 ErrorCode（`ERR_QA_SPC_MEASURED_VALUE_INVALID`）跳过或告警（config-gated）。
  - Skill: `nop-backend-dev`
- [x] Add: `SpcControlLimitCalculator.recalculate(chartId)` —— 当 chart 下 sample 数≥20 子组触发重算 ucl/lcl/cl（clCenterType=AUTO_FROM_DATA 用 grandMean；系数 d2/D3/D4 按 subgroupSize 内置常量表）；calcStatus=PENDING→CALCULATED。配置 `clCenterType=MANUAL/TARGET` 分支。
  - Skill: `nop-backend-dev`
- [x] Decision: 计数型 chartType（P/NP/C/U）范围裁定 —— 枚举完整保留供前端选择，但本期计算引擎仅实现计量型（X_BAR_R/X_BAR_S/X_MR），计数型聚合算法归 successor。选择/替代/残留风险/触发条件见下方 `Deferred But Adjudicated → 计数型控制图` 条目。
  - Skill: none
- [x] Add: `ErpQaSpcSamplingJob`（nop-job invoker bean `erpQaSpcSamplingJob`）扫描 isActive chart 批量 collectSamples + recalculate；scheduler.yaml 追加 job 项（cron 经 `erp-qa.spc-sampling-cron` + 总开关 `erp-qa.spc-enabled` 双层门控）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpQaErrors` 声明 `ERR_QA_SPC_INSUFFICIENT_SAMPLES`、`ERR_QA_SPC_PARAMETER_NOT_FOUND` 等；`ErpQaConstants` 声明配置键常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] collectSamples 对含命中 InspectionLine 的 chart 产出 `ErpQaSpcSample` 子组行，mean/range/stdDev 数值正确（成功模式）；无命中数据不报错；subgroupSize<2 抛 ErrorCode。
- [x] recalculate 在 sample≥20 时算出 ucl/lcl/cl（与手算一致）；sample<20 时 calcStatus 保持 PENDING 不抛错。

### Phase 3 - 判异规则引擎与失控 NCR/CAPA 级联

Status: completed
Targets: `SpcRuleEngine`、`ErpQaSpcSampleBizModel`、失控级联逻辑
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `SpcRuleEngine.evaluate(chart, samples)` —— 实现 Western Electric 规则 1~4（单点超 3σ / 连续 9 点同侧 / 连续 6 点单调 / 连续 14 点交替），按 chart.ruleSet 逗号启用子集，回写每 sample.violatedRules + isOutOfControl。规则算法独立可单测（纯函数）。
  - Skill: `nop-backend-dev`
- [x] Add: 在 recalculate / evaluate 末尾，sample.isOutOfControl=true 经 `txn().afterCommit`（模式 B post-commit）调用 `IErpQaNonConformanceBiz` 创建 NCR（sourceType=SPC，severity 按 violatedRules 映射 dict）+ 按 chart.ruleSet 创建 `IErpQaActionBiz`(actionType=CAPA)。config-gated `erp-qa.spc-auto-ncr-enabled`（默认 true 关闭则仅标记不建 NCR）。失败隔离（单 sample 建单失败不阻断其他）。
  - Skill: `nop-backend-dev`
- [x] Add: `@BizQuery findOutOfControlSamples(chartId, context)` 查询失控样本列表。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] evaluate 对构造的违规序列正确命中对应规则编号（成功模式）；受控序列 violatedRules 为空。失控自动建 NCR+CAPA 且 sourceType=SPC；config 关闭时不建 NCR 但 isOutOfControl 仍标记。

### Phase 4 - 过程能力分析与测试

Status: completed
Targets: `IErpQaSpcCapabilityBiz`、`ErpQaSpcCapabilityBizModel`、`SpcCapabilityCalculator`、`ErpQaSpcCapabilityJob`、scheduler.yaml、`TestErpQaSpc*`
Skill: `nop-backend-dev`（实现）、`nop-testing`（测试）

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] Add: `SpcCapabilityCalculator.calculateCapability(chartId, periodFrom, periodTo, context)` —— 计算 `ErpQaSpcCapability`（grandMean/overallStdDev/withinStdDev=R̄/d2/cp=(USL−LSL)/6σ̂/cpk=min((USL−X̄̄),(X̄̄−LSL))/3σ̂/pp/ppk/cpm + capabilityLevel 按 Cpk 阈值分档 INADEQUATE<1.0/ACCEPTABLE 1.0-1.33/CAPABLE 1.33-1.67/EXCELLENT>1.67）。等级<ACCEPTABLE 回写 `IErpQaQualityGoalBiz.currentValue` + 登记 `IErpQaRiskRegisterBiz`。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpQaSpcCapabilityJob`（invoker bean `erpQaSpcCapabilityJob`）周期（月/周）批量 calculateCapability；scheduler.yaml 追加 job 项（cron 经 `erp-qa.spc-capability-cron` 门控）。
  - Skill: `nop-backend-dev`
- [x] Proof: `TestErpQaSpcSampling`（样本聚合 mean/range/stdDev 数值 + 幂等增量）、`TestErpQaSpcControlLimit`（ucl/lcl/cl 系数表 + sample<20 不重算）、`TestErpQaSpcRuleEngine`（4 规则各构造序列命中 + ruleSet 子集过滤 + 受控空）、`TestErpQaSpcOutOfControl`（失控建 NCR+CAPA sourceType=SPC + config 关闭不建 + afterCommit 时序）、`TestErpQaSpcCapability`（Cp/Cpk 公式数值 + capabilityLevel 分档 + QualityGoal 回写）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] quality 域新测试全绿（0 failures/0 errors），覆盖采样/控制限/规则/失控级联/能力分析成功 + 异常路径。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0c7291f29ffeENp885FJa8fqZJ) — 所有最低规则满足，全部基线主张经实时仓库核实为真（三 SPC 实体缺失、质检数据源/NCR-CAPA 级联/nop-job/QualityGoal-RiskRegister 均存在、「无新增 finance 业务类型」主张正确——`ErpQaNonConformance.sourceType` 为自由 VARCHAR，失控 NCR 复用既有引擎按 dispositionType 过账）。采纳非阻塞建议：Phase 2 补 `measuredValue` VARCHAR→数值解析说明 + ErrorCode；计数型 Decision 交叉引用 Deferred 条目自洽。

## Closure Gates

- [x] 范围内行为完成（采样聚合 + 控制限 + 判异规则 + 失控 NCR/CAPA 级联 + 能力分析）
- [x] 相关文档对齐（`spc.md` 计数型范围 Decision 记录、roadmap 状态更新、`docs/logs/` 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 154+ 模块）+ `mvn test -pl module-quality -am` + 全 workspace `mvn test` 0 failures/0 errors
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SPC 看板后端接线 + 控制图前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 dashboard 结果面（1606-1/1606-2 owner）+ 前端控制图渲染面，非本计划「SPC 引擎」结果面。本计划落地 `ErpQaSpcSample` 即解除数据源阻塞。
- Successor Required: yes —— 触发条件=本计划 completed；successor 在 `ErpQaDashboardBizModel` 补失控预警 `@BizQuery` + AMIS 控制图卡片。

### 计数型控制图（P/NP/C/U）计算引擎

- Classification: `optimization candidate`
- Why Not Blocking Closure: 计量型覆盖连续质量特性主场景；计数型（不合格品率/缺陷数）算法不同（基于二项/泊松分布），Phase 1 Decision 已裁定枚举完整、计算计量型先行。
- Successor Required: yes —— 触发条件=计数型业务需求上线时。

### 自定义判异规则 DSL 编辑器

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划内置 Western Electric 1~4 经 ruleSet 选择已满足主流场景；自定义规则需 DSL 设计面。
- Successor Required: yes —— 触发条件=多客户需自定义判异规则时。

## Closure

Status Note: 4 Phase 全部完成且退出标准全绿；范围内行为（采样聚合 + 控制限 + Western Electric 判异规则 + 失控 NCR/CAPA post-commit 级联 + 过程能力 Cp/Cpk/Pp/Ppk/Cpm + QualityGoal/RiskRegister 回写）均落地并接线到 nop-job 与 BizModel `@BizQuery`；32 SPC 测试全绿（6 集成 + 19 纯函数）；Deferred 三项均为显式 successor（非范围内降级）。可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor 新会话，不重用执行者上下文）
- Evidence: 
  - 实时仓库核对通过：三实体存在于 `module-quality/model/app-erp-quality.orm.xml:765,826,875`（ErpQaSpcChart/ErpQaSpcSample/ErpQaSpcCapability）；3 字典 `erp-qa/spc-chart-type`/`spc-calc-status`/`spc-capability` 在 orm.xml + erp-qa-dao/_app.orm.xml + erp-qa-meta i18n 均已生成。
  - 服务类齐全且非空壳：`SpcSamplingService`、`SpcControlLimitCalculator`、`SpcRuleEngine`（4 规则算法完整，纯函数 `evaluateRules` 可单测，`module-quality/erp-qa-service/.../spc/SpcRuleEngine.java:82-242`）、`SpcOutOfControlHandler`（post-commit NCR+CAPA 级联）、`SpcCapabilityCalculator`（Cp/Cpk/Pp/Ppk/Cpm 计算并持久化 `ErpQaSpcCapability`，等级 INADEQUATE 回写 QualityGoal + RiskRegister，`:100-149`）。
  - nop-job 接线：`scheduler.yaml:111,120` 注册 `erp-qa-spc-sampling` + `erp-qa-spc-capability` 双层门控（cron + 总开关）。
  - 测试落地：`TestErpQaSpcSampling`（6 @Test）、`TestErpQaSpcOutOfControl`（4 @Test）、`TestErpQaSpcCapability`（3 @Test）共 13 集成 + 19 纯函数测试在 `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/spc/`。
  - 日志：`docs/logs/2026/07-07.md:111-154` 完整记录 4 Phase 交付、Decision（post-commit 时序/会话包装/纯函数单测/双重幂等/计数型 Deferred）与下一步 successor。
  - 文档同步：`docs/design/quality/spc.md`（设计真相）+ `docs/architecture/job-scheduling.md:200`（SPC 能力分析 job 行已标注）已对齐。

Follow-up:

- SPC 看板接线（见 Deferred successor）。
