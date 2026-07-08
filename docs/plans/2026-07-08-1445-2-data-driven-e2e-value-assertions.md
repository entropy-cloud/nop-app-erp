# 2026-07-08-1445-2-data-driven-e2e-value-assertions 数据驱动看板/报表精确数值 E2E 断言

> Plan Status: active
> Mission: erp
> Work Item: 看板 KPI + 报表渲染数据驱动精确数值浏览器 E2E 断言（finance/sales/purchase 核心域）
> Last Reviewed: 2026-07-08
> Source: deferred 项承接 `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md` Deferred「数据驱动看板 KPI 断言（具体业务数值）」（Successor Required: yes，触发条件「当 `app-erp-test-data` 落地标准种子集 + 需端到端业务数值回归时」——**数据层由 1234-1 + 1445-1 满足**：1234-1 落地主数据部署 seed、1445-1 落地 P2P+O2C 交易种子；运行时/E2E 经部署 seed 可见）+ `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md` Deferred「数据驱动 KPI 精确数值断言」（Successor Required: yes，触发条件「当 seed 集固化且需回归断言看板/报表具体业务数值时」——**已满足**：1445-1 固化核心域交易种子）；AGENTS.md「当前项目阶段」当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md`（同批 #1，提供固化交易种子数据基线，本计划前置依赖）、`docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md`（completed，冒烟套件基线，本计划在其上增加数值断言层）、`docs/plans/2026-07-08-1234-2-crud-page-e2e-smoke.md`（completed，E2E helper 范式）
> Audit: required

## Current Baseline

实时仓库逐项核实（`rg`/`read`/`ls`，非采信旧记忆）：

- **E2E 基础设施已就绪（0637-1 交付）**：根 `package.json`（`@playwright/test`）+ `playwright.config.ts`（webServer 启动命令实测为 `rm -f db/erp.mv.db db/erp.trace.db && java -Dnop.auth.service-public=true -Dnop.auth.login.allow-create-default-user=true -Dnop.orm.init-database-data=true -jar ...quarkus-run.jar`，**无 `validate-page-model` 标志**——生产 `application.yaml` 保持 `validate-page-model: true`，应用在 seed 库上正常启动；+ `SKIP_WEBSERVER=1` 复用已运行实例）+ `tests/e2e/`（`auth.ts` UI 登录 helper + `fixtures.ts` console error 检查器 + `dashboards/_helper.ts` `runDashboardSmoke` + `reports/_helper.ts` `runReportSmoke` + `crud/_helper.ts` `runCrudListSmoke`）。
- **冒烟套件当前断言层级（约束本计划增量）**：10 看板 spec（断言 body 渲染 + KPI 关键词文本存在 + `/graphql` 200 + 0 console error）+ 24 报表 spec（断言渲染 button 存在 + 点击触发 `/graphql` `ErpXxxReport__renderHtml` 200 + 响应非空）+ 18 CRUD spec（断言列表 DOM + add 表单字段可见）。**全部为「存在性/渲染」冒烟级，无任何具体业务数值断言**（0637-1 Non-Goal / 1234-1 Non-Goal 显式 Deferred）。
- **数据层阻塞由 1445-1 解除（本计划前置）**：1234-1 落地 21 主数据 CSV；**1445-1 将落地 P2P+O2C 交易种子**（采购订单/收货/发票/付款 + 销售订单/发货/发票/收款 + 凭证/凭证行/AR-AP 辅助账/核销/GL 余额/期间 OPEN），使 finance/sales/purchase 域看板 KPI 与报表数值**非零可观测**。无 1445-1 则数值断言无意义（KPI 仍为 0/空）——**本计划 Phase 1 Prereqs 显式依赖 1445-1 完成**。
- **看板/报表后端聚合 API（断言取数源）**：核心 4 域看板 BizModel（`ErpFinDashboardBizModel`/`ErpSalDashboardBizModel`/`ErpPurDashboardBizModel`/`ErpInvDashboardBizModel`，0935-1）+ 6 域（1606-1）经 `@BizQuery` `getDashboardKpi`/`getDashboardTrend`/预警查询暴露于 GraphQL；报表 `ErpXxxReportBizModel.renderHtml/download`（0504-2 + 各域）。前端 page.yaml 经 `/graphql` GraphQL 消费（0637-1 修复了 `/api/GenericApi` 系统性 bug → `/graphql`）。
- **数值断言确定性来源（关键设计约束）**：Playwright 运行于 fresh-DB seed（`playwright.config.ts` webServer 每次重置 DB 后 seed），**种子集是确定性的**（固定 CSV 行 → 固定 KPI 聚合结果）。因此可在 spec 中硬编码「期望值」（派生自 1445-1 seed CSV 的确定性聚合），而非依赖动态计算——前提是 1445-1 seed 已固化（本计划 Prereqs）。
- **GraphQL 响应结构（断言定位）**：看板 KPI 经 `/graphql` POST 返回 `{ data: { ErpXxxDashboard__getDashboardKpi: { ...kpiFields } } }`（Map 返回类型，0637-1 已移除字段选择）；报表 `renderHtml` 返回 `{ data: { ErpXxxReport__renderHtml: { html: "..." } } }`。数值断言须经 adaptor 解析或在 spec 中直接发 GraphQL query 取原始数值（避免 AMIS 渲染层 DOM 抖动）。
- **保护区域**：纯测试 spec 新增（`tests/e2e/`）+ 可能的 helper 扩展。**无 ORM/契约/生产代码/生产配置变更**。属 `plan-first`（跨多会话 + 首次建立数值断言范式 + >5 文件）。若需调整 `playwright.config.ts` webServer（如非 fresh-DB 模式），属运行时配置但非 ask-first。

剩余差距：(1) 看板/报表 E2E 仅渲染冒烟，无数值正确性回归；(2) 1445-1 固化交易种子后，确定性数值期望可派生但未编写；(3) GraphQL 数值断言范式（直接 query 取值 vs AMIS DOM 文本解析）未确立。

## Goals

- 在 1445-1 固化的核心域交易种子基线上，为 finance/sales/purchase（+ inventory 若 1445-1 含库存移动）看板 KPI 增加**数据驱动精确数值断言**：直接经 GraphQL query 取后端聚合 API 返回值，断言非零且匹配 1445-1 seed 派生的确定性期望值（如 finance 现金流 KPI、sales AR 余额、purchase AP 余额）。
- 为核心域报表（finance 资产负债/利润表/AR-AP 账龄 等）增加**渲染内容非空 + 关键聚合值存在**断言（在 0637-1 「渲染 200 + 响应非空」之上提升一层：断言 HTML 含具体数值/表头行）。
- 确立「fresh-DB seed 确定性 → spec 硬编码期望值」的数值断言范式（helper 化，供后续扩展域 successor 复用）。
- 解除 0637-1 + 1234-1 Deferred「数据驱动 KPI 精确数值断言」。

## Non-Goals

- **不**做像素级视觉回归（screenshot diff）——0637-1 Deferred（触发条件：跨环境渲染稳定性 + 产品像素验收需求），保持。
- **不**做报表 PDF/XLSX 下载产物内容 diff——0637-1 Deferred（触发条件：报表口径回归缺陷或需自动化验证输出格式），保持。
- **不**覆盖扩展域看板/报表数值断言（assets/projects/manufacturing/maintenance/quality/master-data/CRM/CS/HR/logistics/b2b/contract/drp/aps）——扩展域交易种子未 seed（1445-1 Non-Goal），数值仍为空，断言无意义；归后续批次（触发条件：对应域交易种子 seed 后）。
- **不**修改任何 `*.orm.xml`/`*.xbiz`/`*.page.yaml`/`*.view.xml`/生产业务代码——E2E 是纯消费侧测试。
- **不**修改 1445-1 seed CSV 内容——种子是本计划输入；若断言与种子不符属 seed bug（回 1445-1 修），非本计划范围。
- **不**做跨浏览器矩阵（0637-1 Deferred，保持）。
- **不**接入 CI（2359-1 Deferred O-14，保持）。

## Task Route

- Type: `implementation-only change` + `verification or audit work`（纯测试 spec + helper 扩展）
- Owner Docs: `docs/testing/e2e-runbook.md`（E2E 运行手册）、`docs/design/dashboards.md`（看板 KPI 口径，派生期望值依据）、`docs/plans/2026-07-08-0637-1-*.md`（冒烟套件范式 + GraphQL 响应结构）、`docs/plans/2026-07-08-1445-1-*.md`（种子数据确定性，期望值派生源）、`docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`（seed 行数，期望值计算依据）
- Skill Selection Basis: `nop-testing`（E2E 断言范式 + GraphQL query 取值 + 确定性期望值派生）。`nop-frontend-dev` 仅当需理解 page.yaml GraphQL 调用结构以构造等价 query 时（局部）。后端聚合 API 已由 0935-1/1606-1 Java 测试覆盖口径正确性，本计划验证的是「seed → 聚合 → GraphQL → 断言」端到端数值链。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`app-erp-all/target/quarkus-app/quarkus-run.jar`。
- **前置依赖：1445-1 交易种子 CSV 已落地并 fresh-DB 启动验证通过**（Phase 1 Prereqs）。
- fresh-DB seed 模式（`playwright.config.ts` webServer 已就绪，无需改）。
- 回滚策略：纯新增 spec/helper，失败不影响生产构建；删除新增 spec 即回滚。

## Execution Plan

### Phase 1 - 期望值派生 + 断言范式确立（Proof + Decision）

Status: planned
Targets: `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`（seed 行数）、看板/报表后端聚合口径
Skill: `nop-testing`

- Item Types: `Proof | Decision`
- Prereqs: **1445-1 完成**（交易种子 CSV 落地 + fresh-DB 启动验证 + GraphQL 抽样可见）

- [ ] `Proof`：基于 1445-1 固化 seed CSV，手算/导出核心域看板 KPI 的**确定性期望值**（如 finance 现金流本期支出 = Σ seed 采购付款金额；sales AR 未核销余额 = Σ seed 销售收款 - 核销；purchase AP 余额 = Σ seed 采购发票 - 付款核销），落盘「KPI 期望值表」（KPI 名 → 期望值 → 派生公式 → seed 行依据）。同时导出核心报表的关键聚合值（如 AR-AP 账龄报表的应收/应付总额）。
      - Skill: `nop-testing`
- [ ] `Decision`：数值断言范式——
      - 选择：**直接 GraphQL query 取值断言**（spec 内 `page.evaluate` / `request.post('/graphql', {...ErpXxxDashboard__getDashboardKpi...})` 取后端聚合原始值，与期望值表比对），辅以 AMIS DOM 文本「数值非空 + 含期望数值字符串」弱断言兜底。
      - 替代 (a) 仅 AMIS DOM 文本解析（rejected——AMIS 渲染层数值格式化/千分位/币种符号致 DOM 文本抖动，断言脆弱）。
      - 替代 (b) 后端 Java 测试覆盖（rejected——0935-1/1606-1 已覆盖聚合口径，但不验证「seed → GraphQL → 浏览器」端到端数值链，本计划价值正是该链）。
      - 残留风险：seed 行变更致期望值漂移（缓解：期望值表标注 seed 依赖；1445-1 seed 变更须同步更新期望值表，Closure Gates 含此一致性检查）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] KPI 期望值表落盘（写入 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md` 或本计划），每 KPI 标注期望值 + 派生公式 + seed 行依据；解除 Phase 2/3 断言编写阻塞。
- [ ] 断言范式 Decision 记录选择 + 替代方案 + 残留风险（seed 漂移同步机制）。

### Phase 2 - 看板 KPI 数值断言 spec（finance/sales/purchase 核心 3 域）

Status: planned
Targets: `tests/e2e/dashboards/*.value.spec.ts`（fin/sal/pur，扩展既有 `*.smoke.spec.ts` 为数值层）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表 + 断言范式

- [ ] `Add`：扩展 `dashboards/_helper.ts` 增 `assertDashboardKpiValues({ domain, expected })`——UI 登录后直接经 GraphQL query 取 `getDashboardKpi` 原始返回值，与期望值表逐字段断言（非零 + 等于期望值）；保留既有 `runDashboardSmoke` 冒烟断言不动。
      - Skill: `nop-testing`
- [ ] `Add`：为 finance/sales/purchase 3 域各新增数值断言（独立 spec 或扩展 smoke spec），断言核心 KPI 匹配 Phase 1 期望值表。
      - Skill: `nop-testing`
- [ ] `Proof`：运行 `npx playwright test tests/e2e/dashboards/ --workers=1`，核心 3 域数值断言全绿（既有 10 域冒烟不回归）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] finance/sales/purchase 看板 KPI 数值断言全绿，KPI 非零且匹配期望值表；既有 10 域冒烟 0 回归。

### Phase 3 - 报表渲染内容数值断言（核心域报表）

Status: planned
Targets: `tests/e2e/reports/*.value.spec.ts`（finance 资产负债/利润表/AR-AP 账龄 等，扩展既有 `*.smoke.spec.ts`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表

- [ ] `Add`：扩展 `reports/_helper.ts` 增 `assertReportRenderedWithValue({ domain, reportName, expectedTokens })`——点击渲染 button 触发 `renderHtml`，断言响应 HTML 含期望数值 token（如 AR-AP 账龄报表 HTML 含应收/应付总额数值；资产负债表含资产合计），在 0637-1「渲染 200 + 响应非空」之上提升为「含具体数值」。
      - Skill: `nop-testing`
- [ ] `Add`：为 finance 核心报表（至少资产负债表/利润表/AR-AP 账龄 3 张）新增渲染内容数值断言。
      - Skill: `nop-testing`
- [ ] `Proof`：运行 `npx playwright test tests/e2e/reports/ --workers=1`，核心报表数值断言全绿（既有 24 报表冒烟 0 回归）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] finance 核心报表渲染内容含具体数值 token 断言全绿；既有 24 报表冒烟 0 回归。

### Phase 4 - 文档对齐（Add）

Status: planned
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2/3 全绿

- [ ] `Add`：`docs/testing/e2e-runbook.md` 增「数值断言层」说明——fresh-DB seed 确定性 → 期望值表派生 → GraphQL 取值断言范式 + seed 漂移同步机制（seed 变更须更新期望值表）。
      - Skill: none
- [ ] `Add`：`docs/testing/known-good-baselines.md` 增数值断言基线行（断言 spec 数 + 全套件状态）。
      - Skill: none

Exit Criteria:

- [ ] e2e-runbook 含数值断言层说明；known-good-baselines 含数值断言基线行。

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is` (ses_0bf807c34ffefCv7166HPjHW1b, general 新会话) — 全部 Current Baseline 主张经实时仓库独立核实通过：根 `package.json`/`playwright.config.ts`/`tests/e2e/`（auth.ts/fixtures.ts/dashboards/_helper.ts runDashboardSmoke/reports/_helper.ts runReportSmoke/crud/_helper.ts runCrudListSmoke）存在；webServer `playwright.config.ts:18` 实测为 fresh-DB reset + `-Dnop.orm.init-database-data=true` + 认证 JVM 属性（**无 `validate-page-model` 标志**——本审计发现并修正了原 Baseline 虚构的该标志，已采纳修正）；spec 计数 10 看板/24 报表/18 CRUD（+1 KB）属实且当前仅渲染冒烟无数值断言（抽样 spec 证实）；0637-1 + 1234-1 均 completed 且其 Deferred「数据驱动 KPI 精确数值断言」触发条件经 1234-1+1445-1 部署 seed 数据层满足（0637-1 措辞指向 test-scope `app-erp-test-data` 经部署 seed 调和，同 1234-1 迭代 2 既定论证）；1445-1 跨计划前置（Phase 1 Prereqs + Related）正确陈述，N=1→N=2 排序合法非误述依赖。规则 4/14 单结果面（看板 KPI + 报表渲染内容数值断言共享 Phase 1 范式 + 同 owner doc + 同验证路径，拆分会碎片化范式确立）、规则 7/8/9、anti-slack、执行时规则 7（全仓 typecheck/build 仅在 Closure Gates）、模板结构全合规。无 BLOCKER / 无 MAJOR。1 项 MINOR 已采纳修正（validate-page-model 标志虚构→更正）。**草案审查已收敛**，Plan Status 升级 active（plan-first 纯测试 spec，非 ORM ask-first；执行排序在 1445-1 之后）。

## Closure Gates

> 本计划为前端/浏览器 E2E 数值断言（数据驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增数值断言）+ 后端构建（确认无后端污染）。

- [ ] 范围内行为完成（核心域看板 KPI + 报表渲染内容数值断言 spec 全绿）
- [ ] 相关文档对齐（e2e-runbook 数值断言层 + known-good-baselines）
- [ ] 已运行验证：`npx playwright test`（全套件含数值断言全绿）+ `mvn clean install -DskipTests`（154 模块，确认 spec 新增无后端污染）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 扩展域看板/报表数值断言（assets/projects/manufacturing/maintenance/quality/master-data/CRM/CS/HR/logistics/b2b/contract/drp/aps）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 扩展域交易种子未 seed（1445-1 Non-Goal），看板/报表数值仍为空，断言无意义。本计划确立的数值断言范式（helper + 期望值表）可供扩展域 successor 复用。
- Successor Required: `yes`
- Trigger Condition: 当对应扩展域交易种子 seed（1445-1 Deferred 后续批次）后，按域逐批补数值断言。

### 像素级视觉回归 + 报表下载产物 diff + 跨浏览器矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 0637-1 既定 Deferred，触发条件未变（跨环境渲染稳定性 / 产品像素验收 / 报表口径回归缺陷 / 非 Chromium 支持需求）。本计划数值断言属不同层（数据正确性 vs. 视觉/格式）。
- Successor Required: `yes`
- Trigger Condition: 同 0637-1 Deferred。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>
- Evidence: <待填写>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
