# 2026-07-06-1247-2-core-dashboards-frontend 核心业务看板 AMIS 前端（财务/销售/采购/库存）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-0935-1-core-business-dashboards-backend.md`「看板 AMIS 前端页面」（Successor Required: yes，触发条件=后端 API 落地**已满足**）+ `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`「经营看板前端实现」（Successor Required: yes，触发条件=看板前端定制启动时）；owner doc `docs/design/dashboards.md`
> Related: `2026-07-06-0935-1-core-business-dashboards-backend.md`（看板后端聚合 API，已完成，本计划消费其 `getDashboardKpi`/`getDashboardTrend`/预警查询）、`2026-07-06-0504-2-report-rendering-subsystem.md`（报表前端 action-auth + page.yaml 范式，已完成，本计划镜像其菜单+页面接入模式）
> Audit: required

## Current Baseline

- **4 核心域看板后端聚合 API 已落地**（0935-1，已审计）：`ErpFinDashboardBizModel`/`ErpSalDashboardBizModel`/`ErpPurDashboardBizModel`/`ErpInvDashboardBizModel`（`@BizModel("ErpXxxDashboard")`，服务型 BizObject），`getDashboardKpi`/`getDashboardTrend`/各域预警查询经 `@BizQuery` 暴露于 GraphQL。跨域 AR/AP 余额经 `IErpFinArApItemBiz.findOpenItems(direction, ctx)` 只读聚合。预警阈值经 `NopSysVariable` 配置化（`erp-dash.*` 键：`fin-cash-flow-threshold`/`sal-ar-overdue-days`/`sal-ar-overdue-amount`/`pur-ap-overdue-days`/`inv-slow-moving-days`/`inv-batch-expiry-days`，默认 0=关闭）。
- **前端占位现状（实时核实）**：
  - **fin/sal/inv**：`/{moduleId}/pages/dashboard/main.page.yaml` 占位页存在（仅一个 alert「待实现占位页面」，7 行）；action-auth 看板菜单组存在（`fin-dashboard`/`sal-dashboard`/`inv-dashboard`，各含一 `*-dashboard-main` 子资源指向占位页）。
  - **purchase**：**无** `pur-dashboard` 菜单组、**无** `pages/dashboard/` 目录、**无**占位页（grep 确认 `erp-pur.action-auth.xml` 无 dashboard 行）。本计划须为采购域新建菜单组 + 页面目录 + 页面。
- **设计规格完备**：`dashboards.md` 为每域定义 KPI 卡片 / 趋势图 / 预警列表三类区块（销售看板 §1、采购 §2、库存 §3、财务 §4），并规定 AMIS 组合（`crud/table` + `chart` + `card`）+ 数据经 GraphQL 查询各域看板 BizModel + 阈值配置化 + 行级权限自动注入。
- **前端范式已验证**：0504-2 已落地 finance 报表 `fin-report` action-auth 菜单组 + 5 个 `report/*.page.yaml`（`form` + `button` 触发 GraphQL `ErpFinReport__renderHtml`/`download`），证明 page.yaml + action-auth + GraphQL 消费范式在项目内可构建可验证。
- **剩余差距**：4 域看板占位页（采购缺占位）需替换/新建为真实 KPI/趋势/预警 AMIS 页面；采购域补建菜单组。

## Goals

- 交付 **4 核心域看板 AMIS 页面**（财务/销售/采购/库存），布局对齐 `dashboards.md`（顶部 KPI 卡片 → 中部趋势图 → 底部预警列表），数据经 GraphQL 消费 0935-1 已落地 `@BizQuery`。
- 为**采购域补建**看板 action-auth 菜单组 + `pages/dashboard/` 目录（其余 3 域复用既有菜单组，仅替换占位页内容）。
- 阈值/筛选默认值引用 `erp-dash.*` 配置与本期区间，前端不硬编码业务阈值。

## Non-Goals

- **其余 6 域看板**（资产/项目/制造/维护/质量/主数据）——0935-1 Deferred，同范式 successor，触发条件=对应域看板需求落地时。
- **大表聚合物化视图/缓存**——0935-1 optimization candidate，触发条件=聚合时延压测不达标时。
- **定时刷新 / WebSocket 实时推送**——`dashboards.md` §刷新为「默认进入加载 + 手动刷新」；定时刷新归 nop-job successor。
- **看板后端 API 变更**——本计划纯前端消费 0935-1 已审计 API，不改 BizModel（如发现 API 缺口须另起后端计划，不在本计划范围）。
- **看板数据导出/打印**——归报表 successor。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/dashboards.md`（看板指标/布局/刷新/权限规格，§1-4 + §实现约定）
- Skill Selection Basis: 任务为 AMIS page.yaml 定制 + action-auth 菜单接入 + GraphQL 消费，匹配 `nop-frontend-dev`（XView 三层模型 / page.yaml 定制 / bounded-merge / 业务动作按钮 / AMIS chart+card+crud 组件）。后端 API 已审计就绪，本计划不改后端。不匹配 `nop-backend-dev`（无 BizModel/xbiz 改动）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。看板后端 API 经 `app-erp-all` 已可访问；`erp-dash.*` 配置键已在 `IErpXxxConstants` 声明（默认 0=关闭）。无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 财务看板页面（establish AMIS pattern: card+chart+crud）

Status: completed
Targets: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（后端 API 0935-1 已就绪）

- [x] **Decision: 看板页面区块组合选型**——采用 AMIS `grid`+`card`（KPI 卡片区）+ `chart`（趋势/占比，line/pie）+ `crud`（预警/明细列表），区间筛选用 `form`+`input-date-range` 默认本期，经 `/api/GenericApi` GraphQL 调 `ErpFinDashboard__getDashboardKpi`/`getDashboardTrend`/预警查询。
  - 理由：`dashboards.md` §实现约定明确规定此组合；0504-2 已验证 page.yaml 经 `/api/GenericApi` 消费 `@BizQuery` 的范式（报表页同路径）。`chart` 组件复用 AMIS 内置图表能力，无需新前端依赖。
  - 替代方案：自建 ECharts 容器组件（拒绝：重复平台能力）；纯表格无图（拒绝：不符合 §分层展示）。
  - Skill: `nop-frontend-dev`
- [x] **Add: 财务看板 `main.page.yaml`**——替换占位：KPI 卡片（本期收入/支出/净利润/银行存款余额/AR·AP 对比）+ 收支趋势双线图 + 利润趋势图 + 现金流预警卡片（`fin-cash-flow-threshold`）。页面 GraphQL 调用对齐 `ErpFinDashboardBizModel` `@BizQuery`：`getDashboardKpi`/`getDashboardTrend`/`findCashFlowAlert`。查询参数带区间；菜单组 `fin-dashboard` 复用既有（仅替换占位页内容）。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 财务看板页面静态验证**——page.yaml 可被 AMIS 解析（构建通过 + YAML well-formed）；页面内 `ErpFinDashboard__*` GraphQL 调用逐一映射到 `ErpFinDashboardBizModel` 真实 `@BizQuery` 方法（`rg -o 'ErpFinDashboard__[A-Za-z]+' main.page.yaml` ⊆ BizModel 源 `@BizQuery` 方法集）；action-auth `fin-dashboard-main` URL 指向该页。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 财务看板 `main.page.yaml` 替换占位且包含 KPI/趋势/预警三类区块；GraphQL 调用签名与后端 `@BizQuery` 一致；构建通过

### Phase 2 - 销售 + 库存看板页面（replicate pattern）

Status: completed
Targets: `module-sales/erp-sal-web/.../pages/dashboard/main.page.yaml`；`module-inventory/erp-inv-web/.../pages/dashboard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（AMIS 范式确立）

- [x] **Add: 销售看板 `main.page.yaml`**——KPI（本期销售额/订单量/转化率/应收余额）+ 销售趋势图 + 客户 TOP10 占比图 + 应收账龄预警（`sal-ar-overdue-days`/`sal-ar-overdue-amount`），对齐 `dashboards.md` §1。GraphQL 对齐 `ErpSalDashboardBizModel`：`getDashboardKpi`/`getDashboardTrend`/`findCustomerTopN`/`findArOverdueAlert`。
  - Skill: `nop-frontend-dev`
- [x] **Add: 库存看板 `main.page.yaml`**——KPI（库存总值/本期出入库量/周转率）+ 库存趋势图 + 仓库分布占比图 + 缺料/滞销/批次效期预警列表（`inv-slow-moving-days`/`inv-batch-expiry-days`），对齐 `dashboards.md` §3。GraphQL 对齐 `ErpInvDashboardBizModel`：`getDashboardKpi`/`getDashboardTrend`/`findWarehouseDistribution`/`findShortageAlert`/`findSlowMovingAlert`/`findBatchExpiryAlert`。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 销售/库存看板静态验证**——两页可被 AMIS 解析；`ErpSalDashboard__*`/`ErpInvDashboard__*` 调用逐一映射到对应 BizModel 真实 `@BizQuery` 方法；action-auth URL 指向正确。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 销售 + 库存看板页面各含 KPI/趋势/预警三类区块；GraphQL 调用签名与后端一致；构建通过

### Phase 3 - 采购看板页面 + 菜单组新建（fill gap）

Status: completed
Targets: `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.page.yaml`；`module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/auth/erp-pur.action-auth.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] **Add: 采购域 action-auth 看板菜单组**——在 `erp-pur.action-auth.xml` 新增 `pur-dashboard`（displayName「采购概览」/ icon / orderNo）+ `pur-dashboard-main` 子资源（`component="AMIS"`、`url="/erp/pur/pages/dashboard/main.page.yaml"`、`app:useCases` 对齐采购看板用例），镜像 fin/sal/inv 既有范式。
  - Skill: `nop-frontend-dev`
- [x] **Add: 采购看板 `main.page.yaml`**——KPI（本期采购额/订单量/应付余额/到货及时率）+ 采购趋势图 + 供应商 TOP10 占比图 + 三单匹配差异/应付超期预警（`pur-ap-overdue-days`），对齐 `dashboards.md` §2。GraphQL 对齐 `ErpPurDashboardBizModel`：`getDashboardKpi`/`getDashboardTrend`/`findVendorTopN`/`findThreeWayMatchDiffAlert`/`findApOverdueAlert`。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 采购看板端到端静态验证**——page.yaml 可被 AMIS 解析；action-auth well-formed（`xmllint --noout`）；`ErpPurDashboard__*` 调用逐一映射到 `ErpPurDashboardBizModel` 真实 `@BizQuery` 方法；菜单组可达。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 采购看板页面 + 菜单组新建完成；action-auth well-formed；GraphQL 调用签名与后端一致；构建通过

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0ca3b1396ffexS45olB4N1bctl`) — 全部 baseline 经实时仓库核实为真（4 Dashboard BizModel + 精确 @BizQuery 方法名 fin/sal/pur/inv；fin/sal/inv 占位页 + dashboard 菜单组存在；purchase 无 dashboard 页/菜单组；finance report page.yaml+action-auth 范式可镜像；`erp-dash.*` 6 键齐备；`/api/GenericApi` 既有）。Rule 4/14（4 域同 owner doc `dashboards.md` + 同 UX 范式 + 同结束标准，正确合一计划）、Rule 7/8/9、反松弛、前端验证门控诚实性（构建+xmllint+YAML 解析+GraphQL 签名逐一映射，Playwright 视觉回归正确裁定为非阻塞 successor 带触发条件）均合规，无阻塞。作者已应用非阻塞精度改进（各域精确 @BizQuery 方法名 + `rg ErpXxxDashboard__` 签名映射核验法）。可转 active。

## Closure Gates

> 完整仓库验证在结束处运行一次。前端结果表面为页面/菜单接入，验证门控对齐 0504-2 已验证的 page.yaml+action-auth 范式（构建通过 + well-formed + GraphQL 签名一致 + 菜单可达），不以浏览器视觉为硬门控（运行时视觉核验作为非阻塞 successor 见 Deferred）。

- [x] 范围内行为完成（4 域看板页面落地，采购菜单组补建，3 占位页替换）
- [x] 相关文档对齐（`dashboards.md` §实现状态标前端落地 + roadmap done 条目 + 当日日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `xmllint --noout` 各 action-auth.xml well-formed + 4 page.yaml YAML 可解析
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 看板运行时视觉/浏览器回归验证（Playwright）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 项目 page.yaml+action-auth 范式经 0504-2 已验证为可构建可解析；本计划静态验证（构建+well-formed+GraphQL 签名一致）对齐既有前端结果面门控。浏览器级视觉回归属独立测试基础设施面。
- Successor Required: `yes`（触发条件：Playwright 看板 e2e 套件建立时——`docs/references/playwright-e2e-guide.md` 已存在但未覆盖看板页面）

### 其余 6 域看板前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0935-1 Deferred；本计划聚焦 4 核心业务域（后端已就绪 + 最高管理价值）。其余 6 域后端 successor 落地后再做前端。
- Successor Required: `yes`（触发条件：对应域看板后端 + 前端需求落地时）

### 定时刷新 / 实时推送 / 物化视图缓存

- Classification: `optimization candidate`
- Why Not Blocking Closure: `dashboards.md` 默认进入加载 + 手动刷新满足 bootstrap；定时/WebSocket/物化为性能与 UX 增量。
- Successor Required: `yes`（触发条件：看板实时性/性能需求落地时）

## Closure

Status Note: 4 核心域看板 AMIS 前端页面全部落地（财务/销售/库存替换占位 + 采购新建页面与 `pur-dashboard` 菜单组），3 Phase 完成，独立结束审计 PASS（无阻塞项）。每页经 `/api/GenericApi` GraphQL 消费 0935-1 已审计 `@BizQuery`（fin 3/sal 4/inv 6/pur 5 方法签名逐一映射），分层布局对齐 `dashboards.md` §实现约定（form 筛选 + service KPI 卡片 + chart 趋势/占比图 + crud 预警列表）。验证全绿：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 全 workspace `mvn test` 0 failures/0 errors + 4 action-auth `xmllint` well-formed + 4 page.yaml YAML 可解析。

Closure Audit Evidence:

- Auditor / Agent: independent subagent (fresh session, `ses_0c9cf1863ffeCiXOZ0lE6Mc93p`)
- Verdict: PASS（无阻塞项）
- Evidence:
  - A. 计划结构一致：12 Phase line-items 全 `[x]`、3 Phase `Status: completed`、`Plan Status: completed`。
  - B. 逐页核验（4 页）：占位已替换（fin 143 行/sal 168 行/inv 226 行/pur 196 行，均非 7 行占位）、YAML well-formed、含 `service`+`chart`+`crud` 三类区块、`ErpXxxDashboard__*` token 逐一映射到 BizModel 真实 `@BizQuery` 方法（fin 3/3、sal 4/4、inv 6/6、pur 5/5，含源行号）。
  - C. action-auth：fin/sal/inv 既有 `{domain}-dashboard`+`{domain}-dashboard-main` URL 指向正确；purchase 新增 `pur-dashboard`+`pur-dashboard-main`（`url="/erp/pur/pages/dashboard/main.page.yaml"`）；4 文件 `xmllint --noout` 全 well-formed。
  - D. 构建：`mvn clean install -DskipTests` BUILD SUCCESS 154 模块；全 workspace `mvn test` BUILD SUCCESS 0 failures/0 errors。
  - E. 文档：`dashboards.md` §实现状态标前端落地、`core-business-roadmap.md` 新增 `✅ done` 条目、`docs/logs/2026/07-06.md` 新增本计划日志段。
  - F. 8 项结束门控全满足。

Follow-up:

- 看板运行时视觉/浏览器回归（Playwright successor，触发条件=看板 e2e 套件建立时）—— 非阻塞，本计划静态门控对齐 0504-2 已验证范式。
- 其余 6 域看板前端（资产/项目/制造/维护/质量/主数据，同范式 successor，触发条件=对应域看板后端+前端需求落地时）。
- 定时刷新/WebSocket 实时推送/物化视图缓存（optimization candidate，触发条件=实时性/性能需求落地时）。
