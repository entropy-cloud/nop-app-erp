# 2026-07-05-0427-1-demand-forecast-entity-mrp-drp-source 需求预测实体 + MRP/DRP 预测需求来源

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: deferred 项承接（`2026-07-02-2237-2-manufacturing-mrp-engine.md` 与 `2026-07-04-1115-2-drp-net-requirement-safety-stock.md` 均将「预测需求来源 / FORECAST 需求来源」记为 Deferred，触发条件「预测实体建模落地时」；本计划即落地该实体）
> Related: `2026-07-02-2237-2-manufacturing-mrp-engine.md`（MRP 引擎，FORECAST 需求来源 Deferred）；`2026-07-04-1115-2-drp-net-requirement-safety-stock.md`（DRP，forecastDemand 来源 Deferred）；`2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（CRM 销售预测，本计划 Decision 将裁定与其关系）
> Mission: erp
> Work Item: extended M2 制造/DRP 预测需求来源（承接多计划 Deferred）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **MRP 设计已规划预测来源但无实体**：`docs/design/manufacturing/mrp.md` §MRP 流程显式列出「销售预测（ErpMfgForecast）」为独立需求来源之一；`demandSource` 字典 `erp-mfg/mrp-demand-source` 已含 `FORECAST` 码值（`ErpMfgConstants.MRP_DEMAND_SOURCE_FORECAST="FORECAST"`）；但 ORM 无 `ErpMfgForecast`/`ErpMfgForecastLine` 实体（grep `ErpMfgForecast` 在 `module-manufacturing/model/app-erp-manufacturing.orm.xml` 命中 0）。
- **MRP 引擎本期不支持 FORECAST**：`module-manufacturing/erp-mfg-service/.../mrp/DemandAggregator.java:38` 显式 Non-Goal 注释「FORECAST 来源——ORM 无 ErpMfgForecast 实体，本期不支持」；`MrpEngine.java:47` 同；`mrp.md` §实现偏离补注（plan 2237-2）记录「需求整合仅销售订单 + 安全库存 + 手工需求」。
- **DRP 有字段无数据源**：`ErpDrpLine.forecastDemand`（DECIMAL，propId 11）字段存在；`module-drp/erp-drp-service/.../drp/DrpDemandAggregator.java:78` 将 `ctx.forecastDemand = BigDecimal.ZERO`，注释「无后端预测实体，本期默认 0（手工录入）」；`DrpEngine.java:78` 净需求公式 `safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty` 已消费该字段但恒为 0；`ErpDrpConfigs.java:13` 注释「无后端预测实体时 forecastDemand 默认 0/手工录入」。
- **CRM 销售预测已存在但语义不匹配**：`ErpCrmForecast`/`ErpCrmForecastLine`（plan 0700-1）为金额/分类/owner 维度的销售预测（COMMIT/UPSIDE/BEST_CASE，按金额），非「产品×数量×时间桶」的运营需求预测，不能直接作为 MRP/DRP 单位级需求来源。
- **既有需求整合扩展点**：MRP `DemandAggregator` 与 DRP `DrpDemandAggregator` 均为纯聚合类，新增来源即在这两个聚合器加查询分支。
- **codegen 链可用**：18 域 codegen + 冒烟测试全 done（`crud-roadmap.md`），新增实体经 `module-manufacturing/model/app-erp-manufacturing.orm.xml` 加性追加 + 重新 codegen 即可生成 dao/meta/CRUD。

剩余差距：无预测实体 → MRP FORECAST 码值空转、DRP forecastDemand 恒 0，两个计划引擎的预测需求来源缺失。

## Goals

- 新增制造域需求预测实体（`ErpMfgForecast` 头 + `ErpMfgForecastLine` 行），承载「产品×（仓库可选）×数量×时间桶」的运营需求预测。
- MRP `DemandAggregator` 接入 FORECAST 需求来源：`runMrp` 时按物料 + 计划区间聚合 approved 预测行进入毛需求，`demandSource=FORECAST` 与既有 SO/安全库存/手工需求并列。
- DRP `DrpDemandAggregator` 接入预测来源：`runDrp` 时按物料 + 目标仓库 + 区间聚合 approved 预测行，填充 `ErpDrpLine.forecastDemand`（替代当前恒 0）。
- 预测头/行具备状态机：本期实现 `DRAFT→APPROVED`（approve）与 `→CANCELLED`（cancel）迁移；`CONSUMED` 作为预留状态值（自动消费标记归后继，见 Deferred）。仅 `APPROVED` 进入引擎消费。

## Non-Goals

- **预测自动生成/统计预测算法**（移动平均/指数平滑/季节性分解等）：本期预测行手工或外部导入录入，不做算法生成。
- **CRM 销售预测 → 运营预测的自动 disaggregation**（金额→产品数量分解）：本期不实现（Decision 裁定关系，见 Task Route）。
- **MRP 需求时界 / CRP 产能校验 / 委外释放**（仍归 `2237-2` Deferred，与本计划正交）。
- **DRP 联合变分安全库存 / 越库 / 月台预约**（仍归 `1115-2` Deferred，需 ORM 保护区域列追加）。
- **预测准确率回写 / 预测 vs 实际差异报表**（属 nop-report 报表面，归后继）。

## Task Route

- Type: `implementation-only change`（承接已裁定 Deferred；ORM 加性新增 + 引擎扩展点接线，无契约破坏性变更）
- Owner Docs: `docs/design/manufacturing/mrp.md`（§MRP 流程预测来源、§实现偏离补注 FORECAST 项收口）；`docs/design/drp/README.md`（ErpDrpLine.forecastDemand 字段语义、§业务规则1 净需求公式）；`docs/design/manufacturing/README.md`（预测实体归属）
- Skill Selection Basis: 涉及 ORM 模型新增 + BizModel 方法 + 跨域 I*Biz 消费，加载 `nop-backend-dev`（决策门、实体服务、跨实体调用）。

### Key Decisions

- **Decision: 预测实体领域归属与粒度**（Phase 1 前置）
  - 选择：单一 `ErpMfgForecast`（制造域）+ `ErpMfgForecastLine`（materialId mandatory + warehouseId optional + periodStart/periodEnd + forecastQty），仓库维度可选以同时服务 MRP（产品级，warehouseId 空）与 DRP（仓级，warehouseId 填）。
  - 替代方案：(a) 制造预测 `ErpMfgForecast` + 独立分销预测 `ErpInvForecast` 两实体——拒绝，重复建模且 DRP 已有 `forecastDemand` 字段承接消费；(b) 复用 CRM `ErpCrmForecast`——拒绝，CRM 为金额/分类/owner 维度，非产品×数量，语义不符（见 Current Baseline）。
  - 残留风险：制造域实体带可选仓库维度略跨界，但 warehouseId 可空且 DRP 消费方按仓库过滤，语义自洽。
- **Decision: 预测与 CRM 销售预测的关系**
  - 选择：本期两者独立。CRM 预测（金额）与运营预测（数量）分开维护，不做自动 disaggregation。在 `mrp.md` 补注关系说明与 disaggregation 后继触发条件。
  - 替代方案：从 CRM 金额预测经产品标准售价反推数量——拒绝（依赖售价策略 + 多币种 + 折扣，误差大，归后继）。
- **Decision: 预测消费口径**
  - 选择：仅 `status=APPROVED` 且区间与 MRP/DRP 计划区间相交的预测行进入消费；按物料（+仓库，DRP 时）聚合 `forecastQty`；同一物料多桶累加。
  - 替代方案：含 DRAFT——拒绝，未审批预测不应驱动计划。

## Infrastructure And Config Prereqs

- 无新基础设施。新增 config 项：`erp-mfg.forecast-consume-enabled`（默认 true，MRP 消费总开关）、`erp-drp.forecast-consume-enabled`（默认 true，DRP 消费总开关），与既有引擎 config 门控范式一致。
- 无数据迁移（新实体，空表起步）；回滚策略：删除新实体 + 还原 `DemandAggregator`/`DrpDemandAggregator` 两处分支。

## Execution Plan

### Phase 1 - 预测实体建模 + codegen

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`；codegen 产物（`erp-mfg-dao` entity/_gen、`erp-mfg-meta` xmeta、`erp-mfg-web` view/page、字典）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`: 在计划中记录预测实体领域归属与粒度裁决（见 Task Route Key Decisions，含替代方案与残留风险）
  - Skill: `nop-backend-dev`
- [x] `Add`: 在 `module-manufacturing/model/app-erp-manufacturing.orm.xml` 加性追加 `ErpMfgForecast`（头：code/orgId/planName/periodFrom/periodTo/status dict `erp-mfg/forecast-status` DRAFT/APPROVED/CONSUMED/CANCELLED/标准审计字段）与 `ErpMfgForecastLine`（materialId mandatory/warehouseId optional/periodStart/periodEnd/forecastQty DECIMAL/sourcedFlag/comment/标准审计字段），头-行 to-many cascadeDelete；追加字典 `erp-mfg/forecast-status`
  - Skill: `nop-backend-dev`
- [x] `Add`: 重新 codegen 生成 dao/_gen/IBiz/meta/view/page（遵循既有制造域实体范式，不手改 _gen）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明实体可生成并可标准 CRUD；跨域消费接线在 Phase 2/3 验证。

- [x] `ErpMfgForecast`/`ErpMfgForecastLine` codegen 产物存在（entity/_gen/IBiz/xmeta），制造域编译通过
- [x] 新实体 CRUD 冒烟（创建头→添加行→查询行外键引用）经 GraphQL mutation/query 返回成功且非空 ID

### Phase 2 - MRP DemandAggregator 接入 FORECAST 来源

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../mrp/DemandAggregator.java`；`ErpMfgForecastBizModel`（approve 状态机）；`IErpMfgForecastBiz`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`: `ErpMfgForecastBizModel`（CrudBizModel）实现 approve（DRAFT→APPROVED）/cancel 状态机，@BizMutation 注解，非法迁移 ErrorCode
  - Skill: `nop-backend-dev`
- [x] `Add`: `DemandAggregator` 新增 `collectForecast(materialId, planStartDate, planEndDate)` 查询 `status=APPROVED` 且区间相交的预测行，按物料聚合 `forecastQty` 为毛需求行，`demandSource=FORECAST`；config-gated `erp-mfg.forecast-consume-enabled`
  - Skill: `nop-backend-dev`
- [x] `Add`: 移除 `DemandAggregator:38` 与 `MrpEngine:47` 的 FORECAST Non-Goal 注释（改为已支持）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] MRP 运行含 approved 预测时，结果含 `demandSource=FORECAST` 的毛需求行；无预测时行为与基线一致（无回归）

### Phase 3 - DRP forecastDemand 接入预测来源

Status: completed
Targets: `module-drp/erp-drp-service/.../drp/DrpDemandAggregator.java`；`ErpDrpConfigs.java`；跨域消费契约（声明于 drp-dao 或经 service-helper 范式）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（Phase 2 非前置，可并行）

- [x] `Add`: DRP 消费制造域预测——`DrpDemandAggregator` 按 materialId + warehouseId（目标仓库）+ 区间查询 approved 预测行聚合，填充 `ctx.forecastDemand`（替代当前 `BigDecimal.ZERO`）；config-gated `erp-drp.forecast-consume-enabled`；跨域经 I*Biz（`IErpMfgForecastBiz` 查询方法声明于 mfg-dao）或 service-helper 范式（记录选用理由，遵循 2237-2 释放跨域范式）
  - Skill: `nop-backend-dev`
- [x] `Add`: 移除 `DrpDemandAggregator:37/42/78` 与 `DrpEngine:36`、`ErpDrpConfigs:13` 的「无后端预测实体」注释（改为已接入）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] DRP 运行含 approved 预测（按目标仓库匹配）时，`ErpDrpLine.forecastDemand` 被填充且参与净需求公式；无预测时 forecastDemand=0（与基线一致，无回归）

### Phase 4 - 测试 + owner doc 对齐

Status: completed
Targets: 行为测试（mfg-service / drp-service）；`docs/design/manufacturing/mrp.md`；`docs/design/drp/README.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 2, Phase 3

- [x] `Proof`: 行为测试——MRP FORECAST 来源（含预测聚合/无预测无回归/demandSource 标记）+ DRP forecastDemand 填充（含仓库过滤/净需求参与/无预测=0）；策略 `JunitAutoTestCase` + GraphQL 快照，`mvn test -pl module-manufacturing/erp-mfg-service -am` 与 `mvn test -pl module-drp/erp-drp-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`: owner doc 对齐——`mrp.md` §实现偏离补注 FORECAST 项标记收口（实体已落地 + 消费规则）；`drp/README.md` ErpDrpLine.forecastDemand 字段语义补「approved 预测行聚合填充」；补 CRM 预测关系说明与 disaggregation 后继触发条件
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿；既有 MRP（2237-2）/DRP（1115-2）套件零回归
- [x] owner doc 偏离补注收口且无残留「无实体」描述

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is after 1 Major fix (plan-audit subagent, 2026-07-05). Findings: (a) Blocker — none; (b) Major — Goals 列 4 态机含 `CONSUMED` 但执行项仅交付 `approve`/`cancel`，Goals↔Execution 范围歧义，已收窄 Goals 措辞并将 `CONSUMED` 自动消费标记移入 Deferred（含触发条件）+ 补 Follow-up；其余 Minor（Phase 3 跨域 I*Biz/service-helper 二选一由实现者记录理由、引用 2237-2 范式；header 携带额外 Mission/Work Item 元数据）留给下游结束审计/深度审计。状态、退出标准、Closure Gates 文本一致；技能选择 `nop-backend-dev` 与任务匹配。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（预测实体 + MRP/DRP 预测来源接线 + 状态机）
- [x] 相关文档对齐（mrp.md / drp/README.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn test -pl module-drp/erp-drp-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预测自动生成/统计预测算法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期手工/导入录入；算法生成属独立结果面（需算法选型 + 历史数据源）。
- Successor Required: yes（触发条件：统计预测/季节性建模需求落地时）

### CRM 销售预测 → 运营预测 disaggregation

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 金额→产品数量分解依赖售价策略 + 多币种 + 折扣，误差大；本期两者独立维护。
- Successor Required: yes（触发条件：CRM 金额预测驱动运营数量需求的产品决策落地时）

### 预测准确率回写 / 预测 vs 实际差异报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 nop-report 报表面；本期仅录入 + 消费。
- Successor Required: yes（触发条件：预测准确性度量需求落地时）

### 预测自动消费标记（CONSUMED 状态迁移）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `CONSUMED` 状态值已预留于字典，但自动标记（MRP/DRP 消费后回写）涉及幂等性、重跑语义与多引擎协调，需独立设计；本期仅消费 `APPROVED` 行，不改变其状态。
- Successor Required: yes（触发条件：预测消费后状态回写或预测周期生命周期管理需求落地时）

## Closure

Status Note: 全部 4 阶段已完成并通过本地验证（mfg-service 56 tests / 0 Failures；drp-service 22 tests / 0 Failures；根 `mvn clean install -DskipTests` BUILD SUCCESS）。范围内行为（预测实体 ErpMfgForecast/ErpMfgForecastLine + 状态机 DRAFT→APPROVED/CANCELLED + MRP DemandAggregator FORECAST 接线 + DRP DrpDemandAggregator forecastDemand 填充）由 11 个新增 case 覆盖（3 forecast CRUD smoke + 4 forecast MRP source + 4 forecast DRP source）；所有 Non-Goal（统计预测算法 / CRM disaggregation / 准确率回写 / CONSUMED 自动迁移）已在计划 Deferred + mrp.md 显式记录。owner docs 已对齐：mrp.md §实现偏离补注 FORECAST 项标记「已收口」、配置点回流、CRM 关系说明；drp/README.md forecastDemand 字段语义补注 + §业务规则1 来源说明 + 配置点回流；日志见 `docs/logs/2026/07-05.md` 顶部章节。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（general subagent，task ses_0d0b33e9cffeIAO8h4WJ7dzPH5），新会话非执行者本人，2026-07-05
- Verdict: AUDIT_VERDICT: PASS（无 Blocker；2 个 Major/Minor 收口建议——根 build evidence 入日志、test count 算术——均已在闭合前补正）
- Evidence: 全量核实覆盖 Phase 1 ORM 实体/dict/codegen 产物、Phase 2 IBiz/BizModel/常量/错误码/Non-Goal 注释移除、Phase 3 DrpDemandAggregator/Configs/pom 依赖/Non-Goal 注释移除、Phase 4 测试文件存在性 + owner docs 对齐 + 日志章节存在；plan checklist 全部 `[x]` + Phase Status 全部 `completed`；独立草案审查记录在 §Draft Review Record。

Follow-up:

- 预测自动生成算法（见上方 Deferred）
- CRM → 运营预测 disaggregation（见上方 Deferred）
- 预测自动消费标记 CONSUMED 状态迁移（见上方 Deferred）
- 预测准确率回写 / 差异报表（见上方 Deferred）
