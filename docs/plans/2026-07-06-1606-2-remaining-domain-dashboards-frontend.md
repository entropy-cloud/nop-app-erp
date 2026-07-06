# 2026-07-06-1606-2-remaining-domain-dashboards-frontend 剩余 6 域看板 AMIS 前端（资产/项目/制造/维护/质量/主数据）

> Plan Status: active
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-1606-1-remaining-domain-dashboards-backend.md`「6 域看板 AMIS 前端页面」（Successor Required: yes，触发条件=后端 API 落地，由 1606-1 交付）+ `docs/plans/2026-07-06-1247-2-core-dashboards-frontend.md`「其余 6 域看板前端」（Successor Required: yes，触发条件=对应域看板后端 + 前端需求落地时）；owner doc `docs/design/dashboards.md` §5-9 + 主数据看板 + §实现约定
> Related: `2026-07-06-1606-1-remaining-domain-dashboards-backend.md`（6 域看板后端聚合 API，本计划消费其 `getDashboardKpi`/`getDashboardTrend`/预警查询）；`2026-07-06-1247-2-core-dashboards-frontend.md`（4 核心域看板前端范式，已完成，本计划镜像其 page.yaml + action-auth 接入模式）
> Audit: required

## Current Baseline

- **6 域看板后端聚合 API 尚未实现**（前置计划 `1606-1` 当前为 `draft`，未执行）：6 域 `ErpAstDashboardBizModel`/`ErpPrjDashboardBizModel`/`ErpMfgDashboardBizModel`/`ErpMntDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMdDashboardBizModel`（`@BizModel("ErpXxxDashboard")`，`getDashboardKpi`/`getDashboardTrend`/各域预警查询经 `@BizQuery`）**0 个存在于代码库**——仅 4 核心域（fin/sal/pur/inv，0935-1 done）的看板 BizModel 已落地。6 域后端 API 是 `1606-1` 的交付物，**本计划为其 successor，不可在 `1606-1` 完成（`completed`）前开始实施**。阈值经 `NopSysVariable` 配置化（`erp-dash.*` 键；4 核心域已有，6 域新增键 `mnt-maintenance-overdue-days`/`qa-capa-overdue-days` 由 1606-1 落地）。
- **前端占位现状（实时核实）**：6 域 `/{moduleId}/pages/dashboard/main.page.yaml` **均为 7 行占位页**（仅一个 alert「待实现占位页面」），位于各域 `erp-{xx}-web/src/main/resources/_vfs/erp/{xx}/pages/dashboard/main.page.yaml`。本计划替换全部 6 个占位页。
- **action-auth 菜单组现状（实时核实）**：6 域 action-auth.xml **均已存在看板菜单组**（`ast-dashboard`/`prj-dashboard`/`mfg-dashboard`/`mnt-dashboard`/`qa-dashboard`/`md-dashboard`，各含一 `*-dashboard-main` 子资源指向占位页）。本计划**不新建菜单组**（与 1247-2 采购域补建不同），仅替换占位页内容。
- **设计规格完备**：`dashboards.md` 为每域定义 KPI 卡片 / 趋势图 / 预警列表三类区块（资产 §5、项目 §6、制造 §7、维护 §8、质量 §9、主数据），并规定 AMIS 组合（`crud/table` + `chart` + `card`）+ 数据经 GraphQL 查询各域看板 BizModel + 阈值配置化 + 行级权限自动注入。
- **前端范式已验证**：1247-2 已落地 4 核心域看板 `main.page.yaml`（财务/销售/库存替换占位 + 采购新建），证明 `form`（区间筛选）+ `service`（KPI 卡片）+ `chart`（趋势/占比）+ `crud`（预警列表）+ `/api/GenericApi` GraphQL 消费 `@BizQuery` 范式在项目内可构建可验证。本计划逐域镜像该范式。
- **三处后端 Non-Goal 传导至前端**（由 1606-1 裁定）：(1) 项目看板无「项目毛利率」卡片；(2) 制造看板「齐套待产」为状态计数卡片（无缺件明细预警列表）；(3) 质量看板无「SPC 失控预警」列表。前端页面不渲染这些缺失指标对应的区块。
- **剩余差距**：6 域看板占位页替换为真实 KPI/趋势/预警 AMIS 页面。

## Goals

- 交付 **6 域看板 AMIS 页面**（资产/项目/制造/维护/质量/主数据），布局对齐 `dashboards.md`（顶部 KPI 卡片 → 中部趋势/占比图 → 底部预警列表），数据经 GraphQL 消费 1606-1 已落地 `@BizQuery`。
- 6 域复用既有 `*-dashboard` 菜单组（仅替换占位页内容，不新建菜单组）。
- 阈值/筛选默认值引用 `erp-dash.*` 配置与本期区间，前端不硬编码业务阈值。

## Non-Goals

- **后端 API 变更**——本计划纯前端消费 1606-1 交付的 API，不改 BizModel（如发现 API 缺口须另起后端计划，不在本计划范围）。
- **看板运行时视觉/浏览器回归（Playwright）**——1247-2 Deferred，触发条件=Playwright 看板 e2e 套件建立时。本计划静态验证（构建 + YAML 解析 + GraphQL 签名一致）对齐 1247-2 已验证前端结果面门控。
- **定时刷新 / WebSocket 实时推送 / 物化视图缓存**——`dashboards.md` §刷新为「默认进入加载 + 手动刷新」；定时/实时/缓存归 nop-job/性能 successor。
- **看板数据导出/打印**——归报表 successor。
- **三处后端 Non-Goal 指标对应的前端区块**——项目毛利率卡片 / 制造齐套缺件明细预警列表 / 质量 SPC 失控预警列表（由 1606-1 裁定，前端不渲染）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/dashboards.md`（看板指标/布局/刷新/权限规格，§5-9 + 主数据 + §实现约定）
- Skill Selection Basis: 任务为 AMIS page.yaml 定制 + GraphQL 消费，匹配 `nop-frontend-dev`（XView 三层模型 / page.yaml 定制 / bounded-merge / 业务动作按钮 / AMIS chart+card+crud 组件）。后端 API 由 1606-1 交付（见前置门控），本计划不改后端。不匹配 `nop-backend-dev`（无 BizModel/xbiz 改动）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。**4 核心域**看板后端 API 经 `app-erp-all` 已可访问；**6 域**后端 API 由 `1606-1` 落地后接入（本计划前置依赖）。`erp-dash.*` 配置键中 4 核心域已在 `ErpXxxConstants` 声明，6 域新增键（`mnt-maintenance-overdue-days`/`qa-capa-overdue-days`）由 1606-1 落地。无新外部服务/端口/密钥。
- **执行前置门控**：本计划所有 Phase 的 Prereqs 为 `1606-1` 达到 `completed`（6 域 `ErpXxxDashboardBizModel` + `@BizQuery` 经 GraphQL 可验证）。在 `1606-1` 完成前本计划保持 `active` 但不可开始实施。

## Execution Plan

### Phase 1 - 资产 + 主数据看板页面（establish non-core pattern）

Status: planned
Targets: `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.page.yaml`；`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: `1606-1` 达到 `completed`（6 域 `ErpXxxDashboardBizModel` + `@BizQuery` 经 GraphQL 可验证）

- [ ] **Decision: 看板页面区块组合选型**——沿用 1247-2 已验证组合：AMIS `grid`+`card`（KPI 卡片区）+ `chart`（趋势/占比，line/pie）+ `crud`（预警/明细列表），区间筛选用 `form`+`input-date-range` 默认本期，经 `/api/GenericApi` GraphQL 调各域 `ErpXxxDashboard__getDashboardKpi`/`getDashboardTrend`/预警查询。
  - 理由：1247-2 已在 4 核心域验证此组合可构建可解析；`dashboards.md` §实现约定明确规定此组合。主数据看板偏轻量（无趋势图），仅 KPI 卡片 + 数据质量预警列表。
  - 替代方案：自建 ECharts 容器组件（拒绝：重复平台能力）；纯表格无图（拒绝：不符合 §分层展示，主数据例外因其指标少且静态）。
  - 残留风险：低——镜像已验证的 1247-2 范式，无新模式引入。
  - Skill: `nop-frontend-dev`
- [ ] **Add: 资产看板 `main.page.yaml`**——替换占位：KPI 卡片（资产原值合计/累计折旧/资产净值/本期折旧/在建工程余额）+ 资产类别分布占比图 + 折旧趋势图 + 折旧未计提预警列表。页面 GraphQL 调用对齐 `ErpAstDashboardBizModel` `@BizQuery`：`getDashboardKpi`/`getAssetCategoryDistribution`/`getDashboardTrend`/`findDepreciationMissingAlert`。菜单组 `ast-dashboard` 复用既有（仅替换占位页内容）。
  - Skill: `nop-frontend-dev`
- [ ] **Add: 主数据看板 `main.page.yaml`**——替换占位：KPI 卡片（物料总数/往来单位总数 customer+vendor/停用主数据数）+ 无 SKU 物料预警列表 + 无价格 SKU 预警列表（主数据看板无趋势图，对齐 §说明「指标少且静态」）。页面 GraphQL 调用对齐 `ErpMdDashboardBizModel`：`getDashboardKpi`/`findMaterialWithoutSkuAlert`/`findSkuWithoutPriceAlert`。菜单组 `md-dashboard` 复用既有。
  - Skill: `nop-frontend-dev`
- [ ] **Proof: 资产/主数据看板页面静态验证**——page.yaml 可被 AMIS 解析（构建通过 + YAML well-formed）；页面内 `ErpAstDashboard__*`/`ErpMdDashboard__*` GraphQL 调用逐一映射到对应 BizModel 真实 `@BizQuery` 方法（`rg -o 'ErpAstDashboard__[A-Za-z]+' main.page.yaml` ⊆ BizModel 源 `@BizQuery` 方法集）；action-auth `*-dashboard-main` URL 指向该页。
  - Skill: `nop-frontend-dev`

Exit Criteria:

> 仅写此阶段交付的可观察结果 + 解除后续阶段阻塞的本地化检查。

- [ ] 资产/主数据看板 `main.page.yaml` 替换占位且包含 KPI/趋势或占比/预警区块（主数据无趋势，仅 KPI + 预警）；GraphQL 调用签名与后端 `@BizQuery` 一致；构建通过
- [ ] 区块组合 Decision 已记录，后续 4 域沿用同范式（解除 Phase 2-3 阻塞）

### Phase 2 - 项目 + 制造看板页面（replicate pattern）

Status: planned
Targets: `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/dashboard/main.page.yaml`；`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（AMIS 范式确立）

- [ ] **Add: 项目看板 `main.page.yaml`**——替换占位：KPI 卡片（在手项目数/项目总预算/已发生成本/预算执行率）+ 项目状态分布占比图 + 成本超支项目预警列表 + 项目延期预警列表（无毛利率卡片，见 Non-Goal）。对齐 `dashboards.md` §6。GraphQL 对齐 `ErpPrjDashboardBizModel`：`getDashboardKpi`/`getProjectStatusDistribution`/`findCostOverrunAlert`/`findDelayedProjectAlert`。
  - Skill: `nop-frontend-dev`
- [ ] **Add: 制造看板 `main.page.yaml`**——替换占位：KPI 卡片（在制工单数/本期完工量/工单准时率/齐套待产）+ 工单状态分布占比图 + 产成品产出趋势图 + 工单延期预警列表（齐套仅状态计数卡片，无缺件明细预警列表，见 Non-Goal）。对齐 `dashboards.md` §7。GraphQL 对齐 `ErpMfgDashboardBizModel`：`getDashboardKpi`/`getWorkOrderStatusDistribution`/`getDashboardTrend`/`findDelayedWorkOrderAlert`。
  - Skill: `nop-frontend-dev`
- [ ] **Proof: 项目/制造看板静态验证**——两页可被 AMIS 解析；`ErpPrjDashboard__*`/`ErpMfgDashboard__*` 调用逐一映射到对应 BizModel 真实 `@BizQuery` 方法；action-auth URL 指向正确。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 项目 + 制造看板页面各含 KPI/趋势或占比/预警区块；GraphQL 调用签名与后端一致；构建通过

### Phase 3 - 维护 + 质量看板页面（replicate pattern）

Status: planned
Targets: `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.page.yaml`；`module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] **Add: 维护看板 `main.page.yaml`**——替换占位：KPI 卡片（设备总数/运行中设备/待处理维护请求/本期维护访问数）+ 设备状态分布占比图 + 设备停机预警卡片 + 维护逾期预警列表（无 OEE 精确卡片，见 Non-Goal）。对齐 `dashboards.md` §8。GraphQL 对齐 `ErpMntDashboardBizModel`：`getDashboardKpi`/`getEquipmentStatusDistribution`/`findEquipmentDowntimeAlert`/`findMaintenanceOverdueAlert`。
  - Skill: `nop-frontend-dev`
- [ ] **Add: 质量看板 `main.page.yaml`**——替换占位：KPI 卡片（本期质检数/合格率/不合格数/开放 NCR 数）+ 合格率趋势图 + 不合格原因 TOP 占比图 + CAPA 逾期预警列表（无 SPC 失控预警列表，见 Non-Goal）。对齐 `dashboards.md` §9。GraphQL 对齐 `ErpQaDashboardBizModel`：`getDashboardKpi`/`getDashboardTrend`/`findDefectTopN`/`findCapaOverdueAlert`。
  - Skill: `nop-frontend-dev`
- [ ] **Proof: 维护/质量看板静态验证**——两页可被 AMIS 解析；`ErpMntDashboard__*`/`ErpQaDashboard__*` 调用逐一映射到对应 BizModel 真实 `@BizQuery` 方法；action-auth URL 指向正确。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 维护 + 质量看板页面各含 KPI/趋势或占比/预警区块；GraphQL 调用签名与后端一致；构建通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0c981dce1ffedGLNNSek2tXYmP`，独立 general 子代理，新会话）— 结构高质量（6 域正确合一 owner plan、GraphQL 方法签名与 1606-1 预对齐、镜像 1247-2 范式、item/skill 标记、Non-Goals 合法、anti-slack 干净、Dependency/Verification/R14 合规）。**单阻塞项 B1**：Current Baseline/Phase 1 Prereqs/Infra Prereqs 将 6 域后端描述为「已就绪/已可访问」，但 6 域 BizModel 0 个存在于代码库（1606-1 仍 draft）。nits：`IErpXxxConstants` 应为 `ErpXxxConstants`（无 I 前缀非接口）、`erp-dash.*` 6 域键未声明（仅 4 核心域有）。Baseline 其余全部经实时仓库核实为真（4 核心域页面非占位、6 域 7 行占位页、6 域 dashboard 菜单组齐备、1247-2 completed 范式可镜像、方法签名与 1606-1 完全匹配、playwright guide 存在）。**已修订**：B1 Current Baseline 改「尚未实现」+ 0 个 BizModel 存在 + 不可在 1606-1 完成前开始；Phase 1 Prereqs 改「1606-1 达到 completed」；Infra Prereqs 区分 4 核心域已可访问 vs 6 域待 1606-1 + 新增「执行前置门控」；nits `IErpXxxConstants`→`ErpXxxConstants`、6 域键归属 1606-1。
- Independent draft review iteration 2: **accept / consensus**（`ses_0c9776542ffe8ujeQ4MlNyoOo8`，独立 general 子代理，新会话）— B1（后端就绪误述）确认修复：Current Baseline/Phase 1 Prereqs/Infra Prereqs 三处不再声称 6 域后端就绪，正确表述 0 个 BizModel 存在 + 不可在 1606-1 完成前实施 + 「执行前置门控」区分 active 排队与实施启动；6 域 GraphQL 方法签名与 1606-1 1:1 对齐；nit `ErpXxxConstants` 已修正。规则 1/4/11/12 满足，无新阻塞。**共识达成**（迭代 1-2 收敛）：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 完整仓库验证在结束处运行一次。前端结果表面为页面接入，验证门控对齐 1247-2 已验证的 page.yaml 范式（构建通过 + well-formed + YAML 可解析 + GraphQL 签名一致 + 菜单可达），不以浏览器视觉为硬门控（运行时视觉核验作为非阻塞 successor 见 Deferred）。

- [ ] 范围内行为完成（6 域看板页面落地，6 占位页替换）
- [ ] 相关文档对齐（`dashboards.md` §实现状态标 6 域前端落地 + 当日日志）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ 6 page.yaml YAML 可解析 + 各域 action-auth `xmllint --noout` well-formed
- [ ] 无范围内项目降级为 deferred/follow-up（Playwright 视觉回归 / 三处后端 Non-Goal 对应前端区块均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 看板运行时视觉/浏览器回归验证（Playwright）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 项目 page.yaml 范式经 1247-2/0504-2 已验证为可构建可解析；本计划静态验证（构建 + well-formed + GraphQL 签名一致）对齐既有前端结果面门控。
- Successor Required: `yes`（触发条件：Playwright 看板 e2e 套件建立时——`docs/references/playwright-e2e-guide.md` 已存在但未覆盖看板页面）

### 定时刷新 / 实时推送 / 物化视图缓存

- Classification: `optimization candidate`
- Why Not Blocking Closure: `dashboards.md` 默认进入加载 + 手动刷新满足 bootstrap；定时/WebSocket/物化为性能与 UX 增量。
- Successor Required: `yes`（触发条件：看板实时性/性能需求落地时）

### 三处后端 Non-Goal 指标对应前端区块

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 项目毛利率 / 制造齐套缺件明细 / 质量 SPC 失控预警的后端数据源实体未物化（1606-1 裁定）；前端不渲染对应区块。
- Successor Required: `yes`（触发条件：对应后端 successor 落地时——见 1606-1 Deferred But Adjudicated）

## Closure

Status Note: <关闭原因——仅在独立结束审计通过后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 看板运行时视觉/浏览器回归（Playwright successor，触发条件=看板 e2e 套件建立时）
- 定时刷新/WebSocket 实时推送/物化视图缓存（optimization candidate，触发条件=实时性/性能需求落地时）
- 三处后端 Non-Goal 指标对应前端区块（触发条件=对应后端 successor 落地时，见 1606-1）
