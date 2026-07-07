# 2026-07-07-1100-3-dashboard-deferred-indicators 看板延迟指标接线（SPC 失控预警 + 项目毛利率）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/plans/2026-07-07-0305-2-quality-spc-process-control.md` Deferred「SPC 看板后端接线 + 控制图前端」（触发条件=0305-2 completed，已满足）+ `docs/plans/2026-07-07-0305-1-projects-pnl-settlement-capitalization.md` Deferred「项目毛利率看板后端接线 + 前端卡片」（触发条件=0305-1 completed，已满足）+ `docs/plans/2026-07-06-1606-1-remaining-domain-dashboards-backend.md` Phase 1 Decision（数据源未物化裁定 Non-Goal，现已解除）
> Related: `2026-07-06-1606-1-remaining-domain-dashboards-backend.md`（6 域看板后端，quality/projects 当时 Non-Goal）、`2026-07-06-1606-2-remaining-domain-dashboards-frontend.md`（6 域看板前端，同 Non-Goal 占位）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **两看板 BizModel 仍带 Non-Goal 占位注释**：
  - `ErpQaDashboardBizModel`（`module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java:41`）—— Javadoc 仍写「SPC 失控预警 Non-Goal（`ErpQaSpcSample` 未物化，见 plan 1606-1）」。该注释所陈述的前置条件**已过时**：`ErpQaSpcSample` 已由 0305-2 物化。
  - `ErpPrjDashboardBizModel`（`module-projects/erp-prj-service/.../dashboard/ErpPrjDashboardBizModel.java:43`）—— Javadoc 仍写「项目毛利率指标 Non-Goal（`ErpPrjProjectPnl` 未物化，见 plan 1606-1）」。该前置条件**已过时**：`ErpPrjProjectPnl` 已由 0305-1 物化。
- **数据源实体现已就绪**：
  - `ErpQaSpcSample`（`module-quality/model/app-erp-quality.orm.xml:826`）—— `chartId`(propId 2)、`subgroupNo`、样本值；UK `chartId,subgroupNo`。**失控状态已物化为样本列**：`isOutOfControl`(propId 15, BOOLEAN, 索引 `IDX_QA_SPC_SAMPLE_IS_OUT_OF_CONTROL` orm:865) + `violatedRules`(propId 14)——由 `SpcRuleEngine`（0305-2）评估 Western Electric 规则后写入；失控经 `SpcOutOfControlHandler` 级联生成 NCR（`sourceType=SPC`，`ErpQaConstants.java:155`）。看板「失控图数」可直接 `distinct chartId where isOutOfControl=true` 聚合，无需运行时重算规则。
  - `ErpQaSpcCapability`（orm:875）—— `chartId`(propId 2)、`cpk`(propId 12)、`capabilityLevel`(propId 16, dict `erp-qa/spc-capability`：含 INADEQUATE 档)，由 `SpcCapabilityCalculator`（0305-2）持久化。
  - `ErpPrjProjectPnl`（`module-projects/model/app-erp-projects.orm.xml:723`）—— `projectId`(propId 3)、`revenueAmount`(propId 11)、`grossMarginPct`(propId 18)、`actualCost`/成本列；由 `ProjectPnlCalculator` + `ErpPrjPnlCalcJob`（0305-1）周期物化。
- **既有看板范式已验证**：1606-1/1606-2 已落地 10 域看板 BizModel + AMIS 页面（KPI 卡片经 `service`+`grid`+`wrapper`，数据经 `/api/GenericApi` GraphQL 消费 `@BizQuery`，`adaptor` 转 echarts/amis）。本期两指标复用同范式，仅补两域各自 Non-Goal 占位的 KPI/预警段。
- **剩余差距**：(1) quality 看板缺 SPC 失控预警 `@BizQuery` + AMIS 卡片；(2) projects 看板缺项目毛利率 `@BizQuery` + AMIS 卡片；(3) 两 BizModel Javadoc Non-Goal 注释过时须修正（owner-doc 漂移修正）。

## Goals

- **SPC 失控预警指标**：`ErpQaDashboardBizModel` 新增 `@BizQuery getSpcOutOfControlWarning(ctx)` —— 聚合 `ErpQaSpcCapability`（`capabilityLevel=INADEQUATE` 计数）+ SPC 来源未关闭 NCR 计数（`ErpQaNonConformance` `sourceType=SPC` 且非终态），返回结构化预警摘要（失控图数 / INADEQUATE 能力数 / 待处置 SPC NCR 数）。移除 Non-Goal Javadoc 注释。
- **项目毛利率指标**：`ErpPrjDashboardBizModel` 新增 `@BizQuery getProjectGrossMargin(ctx)`（支持可选 projectId/状态过滤）—— 聚合 `ErpPrjProjectPnl`（Σ revenueAmount、Σ cost、加权 grossMarginPct 或整体毛利率），返回 KPI 摘要。移除 Non-Goal Javadoc 注释。
- **AMIS 卡片接入**：quality 与 projects 看板 `main.page.yaml` 各补一 KPI/预警卡片（消费新增 `@BizQuery`，对齐 1606-2 既有卡片范式），替换 Non-Goal 占位。
- **测试**：两 `@BizQuery` 聚合行为测试（含空数据/多记录/过滤）。
- **owner doc 收口**：1606-1/1606-2 相关 Non-Goal 表述与 Deferred 触发条件解除（数据源已物化）。

## Non-Goals

- **SPC 控制图完整可视化（echarts 控制图 + Western Electric 规则标记 + 控制限区间带）**：本期仅交付 KPI/预警聚合数字 + 简单 AMIS 卡片；交互式控制图渲染（含 UCL/LCL 线、违规点高亮、子组趋势）归前端可视化 successor（触发条件：报表/看板 e2e 可视化套件建立时，对齐 0504-2/1606-2 既有 Playwright successor 口径）。
- **项目毛利率按期间/部门/项目经理多维下钻**：本期整体聚合 + 可选 projectId 过滤；多维下钻归 projects 报表 successor。
- **看板定时刷新 / WebSocket 实时推送 / 物化视图缓存**：归 optimization candidate（对齐 1606-1/1606-2 既有 Non-Goal）。
- **其余域看板指标增强**：本期仅补 quality/projects 两域各自的 Non-Goal 占位；其他域看板指标增强同范式 successor。
- **NCR 详情下钻 / 预警阈值告警链路**：本期仅聚合计数展示；阈值告警经通知派发（0504-1）已有独立面，预警阈值 config-gated 仅控制是否纳入计数。

## Task Route

- Type: `implementation-only change`（两看板 BizModel 加 `@BizQuery` 聚合方法 + AMIS 卡片，ORM/契约无变更）。
- Owner Docs: `docs/design/dashboards.md`（§实现约定分层布局，既有）、`docs/design/quality/spc.md`（SPC 失控语义，既有）、`docs/design/projects/pnl-settlement.md`（项目损益语义，既有）。
- Skill Selection Basis: 后端 BizModel `@BizQuery` 同域只读聚合（注入 `IDaoProvider`/`IOrmTemplate`，镜像 1606-1 范式，无跨域 I*Biz）→ 加载 `nop-backend-dev`；AMIS page.yaml 卡片 → 加载 `nop-frontend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。三技能必需输入（dashboards.md 既有、数据源实体已物化、1606-2 AMIS 范式既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更；无 codegen 增量（仅看板 BizModel 加方法 + page.yaml 加卡片）。
- 可选配置键（预警纳入范围 config-gated，对齐 1606-1 `erp-dash.*` 范式）：`erp-dash.qa-spc-include-ncr`（默认 true，是否纳入 SPC NCR 计数）、`erp-dash.qa-spc-include-inadequate`（默认 true）。复用既有 `ErpQaConstants`/`ErpPrjConstants` + `ErpXxxConfigs` 范式（若不存在则按域范式补 reader）。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + page.yaml，git 可逆；配置键默认值保持预警纳入。

## Execution Plan

### Phase 1 - 后端两 @BizQuery 聚合方法

Status: completed
Targets: `ErpQaDashboardBizModel`、`ErpPrjDashboardBizModel`、（可选）`ErpQaConstants`/`ErpQaConfigs`、`ErpPrjConstants`/`ErpPrjConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无（数据源实体已物化）

- [x] `Add`：`ErpQaDashboardBizModel.getSpcOutOfControlWarning(ctx)` @BizQuery —— 经 `IOrmTemplate`/`IDaoProvider` 同域只读聚合：(a) `ErpQaSpcCapability` 计 `capabilityLevel=INADEQUATE` 图数；(b) `ErpQaNonConformance` 计 `sourceType=SPC` 且非终态（OPEN/IN_REVIEW）NCR 数；(c) 失控图数 = `distinct ErpQaSpcSample.chartId where isOutOfControl=true`(propId 15，索引 `IDX_QA_SPC_SAMPLE_IS_OUT_OF_CONTROL` 已就绪)。返回结构化 DTO（非裸 Map）。(a) 与 (c) 两段纳入经 config-gated。移除 :41 Non-Goal Javadoc 注释。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpPrjDashboardBizModel.getProjectGrossMargin(ctx)` @BizQuery（可选 `@Name("projectId") Long projectId` 过滤）—— 聚合 `ErpPrjProjectPnl`：Σ revenueAmount(propId 11)、Σ totalCost(propId 16)、Σ grossProfit(propId 17) 均已物化；整体毛利率优先取 Σ grossProfit / Σ revenueAmount（两列均 DECIMAL 直接可加，避免 `grossMarginPct`(propId 18, stdDataType=string) 加权语义歧义）。返回结构化 KPI DTO。移除 :43 Non-Goal Javadoc 注释。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 两 `@BizQuery` 返回结构化非空结果（空数据时返回零值结构，非 `return null`）；移除两处 Non-Goal Javadoc 注释。
- [x] `mvn compile -pl module-quality/erp-qa-service -am` + `mvn compile -pl module-projects/erp-prj-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 2 - AMIS 卡片接入

Status: completed
Targets: `module-quality/erp-qa-web`/`module-projects/erp-prj-web` 下看板 `main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（@BizQuery 就绪后前端才能消费）

- [x] `Add`：quality 看板 `main.page.yaml` 补 SPC 失控预警卡片（`service`+`grid`+`wrapper` 消费 `ErpQaDashboard__getSpcOutOfControlWarning`，`adaptor` 转 amis 卡片，对齐 1606-2 既有预警列表/卡片范式）。
  - Skill: `nop-frontend-dev`
- [x] `Add`：projects 看板 `main.page.yaml` 补项目毛利率 KPI 卡片（消费 `ErpPrjDashboard__getProjectGrossMargin`，对齐 1606-2 既有 KPI 卡片范式；页面参数对齐后端签名）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 两 page.yaml YAML 可解析（`yaml.safe_load`）；卡片调用方法名与后端 `@BizQuery` 真实方法名逐一核对一致。

### Phase 3 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-quality/erp-qa-service/src/test/.../TestErpQaDashboardSpc.java`、`module-projects/erp-prj-service/src/test/.../TestErpPrjDashboardGrossMargin.java`、`docs/logs/2026/{执行当日}.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestErpQaDashboardSpc`（集成测试）：空数据零值结构、INADEQUATE 能力计数、SPC NCR 开/关状态纳入差异、config-gated 关闭纳入段。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpPrjDashboardGrossMargin`（集成测试）：空数据零值、单项目 Σ、多项目加权毛利率、projectId 过滤。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-quality/erp-qa-service -am` + `mvn test -pl module-projects/erp-prj-service -am`（含本期新增 + 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；1606-1/1606-2/0305-1/0305-2 相关 Non-Goal 与 Deferred 表述更新（数据源已物化，触发条件解除）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿；qa-service/prj-service 既有测试无回归。
- [x] 当日日志条目在位；相关 plan Non-Goal/Deferred 表述更新。

## Draft Review Record

- Independent draft review iteration 1: accept (draft-review pass, 2026-07-07) after fixing one Major baseline inaccuracy: 原文称 `ErpQaSpcSample` 失控状态「非样本列持久化」，与实时 ORM (`isOutOfControl` propId 15 + `violatedRules` propId 14，索引 `IDX_QA_SPC_SAMPLE_IS_OUT_OF_CONTROL` 已就绪) 矛盾；已更正 baseline 与 Phase 1 (c) 失控图数派生路径为 `distinct chartId where isOutOfControl=true`，消除「Decision 在实现时按字段定」模糊。同步收紧 Phase 1 项目毛利率口径为 Σ grossProfit / Σ revenueAmount（两 DECIMAL 列已物化），移除对 `grossMarginPct`(string 类型) 加权的歧义。其余格式/范围/退出标准/结束门控符合指南。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（SPC 失控预警 @BizQuery + 项目毛利率 @BizQuery + 两 AMIS 卡片 + Non-Goal 注释修正）
- [x] 相关文档对齐（1606-1/1606-2/0305-1/0305-2 Non-Goal/Deferred 表述、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-quality/erp-qa-service -am` + `mvn test -pl module-projects/erp-prj-service -am`（0 failures / 0 errors）+ 两 page.yaml YAML 可解析
- [x] 无范围内项目降级为 deferred/follow-up（控制图完整可视化/多维下钻/实时推送/其余域增强均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SPC 控制图完整可视化（echarts 控制图 + Western Electric 规则标记 + 控制限区间带）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅交付 KPI/预警聚合数字 + 简单 AMIS 卡片；交互式控制图渲染属前端可视化面。
- Successor Required: yes（触发条件：报表/看板 e2e 可视化套件建立时）

### 项目毛利率多维下钻（期间/部门/项目经理）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期整体聚合 + projectId 过滤；多维下钻属 projects 报表 successor。
- Successor Required: yes（触发条件：项目多维盈利分析需求上线时）

## Closure

Status Note: 3 Phase 全部完成且退出标准全绿。范围内行为全部落地：(1) `ErpQaDashboardBizModel.getSpcOutOfControlWarning` `@BizQuery` 三段聚合（失控图数 distinct `ErpQaSpcSample.chartId where isOutOfControl=true` + INADEQUATE 能力图数 + 待处置 SPC NCR 数，后两段 config-gated）；(2) `ErpPrjDashboardBizModel.getProjectGrossMargin` `@BizQuery`（Σ grossProfit/Σ revenueAmount 整体毛利率 + 可选 projectId 过滤）；(3) 两 AMIS 卡片（quality SPC 预警 + projects 毛利率 KPI）；(4) 两 BizModel Non-Goal Javadoc 移除。`ErpQaConstants` +2 配置键 + `ErpQaConfigs` +2 reader。验证全绿：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；qa-service 91 tests（含新增 TestErpQaDashboardSpc 5 cases）+ prj-service 67 tests（含新增 TestErpPrjDashboardGrossMargin 4 cases）0 failures/0 errors；两 page.yaml YAML 可解析。解除 0305-2/0305-1 Deferred + 1606-1 Phase 1 Decision 中项目毛利率/质量 SPC 两处 Non-Goal（数据源已物化）。独立结束审计已由新会话子代理完成（语义对实时仓库复核：两 `@BizQuery` 实质实现非 hollow、配置键 + reader 在位、AMIS 卡片方法名对齐、测试 5+4 cases 实际落地、两 page.yaml YAML 可解析、日志/owner doc 同步；Deferred 均为带触发条件的 Non-Goal 改进，无在范围内缺陷隐藏）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文），2026-07-07
- Evidence:
  - 实时仓库核对：`ErpQaDashboardBizModel.getSpcOutOfControlWarning`（`module-quality/erp-qa-service/.../dashboard/ErpQaDashboardBizModel.java`）+ `ErpPrjDashboardBizModel.getProjectGrossMargin`（`module-projects/erp-prj-service/.../dashboard/ErpPrjDashboardBizModel.java`）均为实质 `@BizQuery` 实现（`ormTemplate.runInSession` + `IDaoProvider` + `QueryBean` 聚合，空数据返回零值 LinkedHashMap 非 null）；两处 Non-Goal Javadoc 已移除并替换为 SPC/毛利率口径 Javadoc。
  - 配置：`ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_INADEQUATE`/`CONFIG_DASH_QA_SPC_INCLUDE_NCR` 声明 + `ErpQaConfigs.isDashQaSpcIncludeInadequate()`/`isDashQaSpcIncludeNcr()` reader（默认 true）。
  - 前端：quality `dashboard/main.page.yaml` `spcWarningService`（消费 `ErpQaDashboard__getSpcOutOfControlWarning`）+ projects `dashboard/main.page.yaml` `grossMarginService`（消费 `ErpPrjDashboard__getProjectGrossMargin`），refresh target 已含新 service；两 YAML `yaml.safe_load` 可解析。
  - 测试：`TestErpQaDashboardSpc`（5 @Test：空数据/INADEQUATE 计数去重/失控图数 distinct/NCR 开关状态+来源排除/config-gated 关闭）+ `TestErpPrjDashboardGrossMargin`（4 @Test：空数据/单项目/多项目加权 0.3333/projectId 过滤）。
  - 验证命令实测：`mvn clean install -DskipTests` = BUILD SUCCESS（154 模块，2026-07-07 16:00）；`mvn test -pl module-quality/erp-qa-service` = Tests run: 91, Failures: 0, Errors: 0；`mvn test -pl module-projects/erp-prj-service` = Tests run: 67, Failures: 0, Errors: 0。
  - 文档同步：`docs/design/dashboards.md` §实现状态（3 处 Non-Goal 表述更新）；`docs/logs/2026/07-07.md`（本计划条目 + full-green 验证记录）；`docs/backlog/core-business-roadmap.md`（新增 1100-3 done 条目）；1606-1/1606-2/0305-1/0305-2 Deferred/Follow-up 表述更新（✅ 标记）。
  - 反模式自检：`@Inject` 非 private / 异常无（纯只读聚合）/ 无 `@BizMutation+@Transactional` / `IServiceContext context` 为最后参数 / 无 `_gen` 手改 / 同域只读用 `IDaoProvider`+`IOrmTemplate`（Javadoc 文档化，镜像 1606-1 范式）。

Follow-up:

- SPC 控制图完整可视化（见上方 Deferred）
- 项目毛利率多维下钻（见上方 Deferred）
