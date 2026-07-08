# 2026-07-08-1234-2-crud-page-e2e-smoke 18 域 CRUD 页面 Playwright E2E 冒烟回归

> Plan Status: active
> Mission: erp
> Work Item: 18 域 CRUD 列表/表单页面浏览器冒烟回归套件
> Last Reviewed: 2026-07-08
> Source: AGENTS.md「当前项目阶段」当前重点「各域细化端到端验证」；deferred 项承接 `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md` Deferred「CRUD 页面 E2E（18 域表单/列表）」+ `docs/plans/2026-07-08-1107-1-fix-cs-ticket-view-restore-page-validation.md` Deferred「18 域 CRUD 页面完整 e2e 套件」（触发条件「当按域推进 CRUD e2e 覆盖时 / 当 CRUD 页面批量定制后需视觉/交互回归时」——Playwright 基础设施已由 0637-1 建立，触发条件已满足）
> Related: `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md`（completed，建立 Playwright 基础设施 + 10 看板 + 24 报表冒烟，其「CRUD 页面 E2E」Deferred 本计划承接）、`docs/plans/2026-07-08-1107-1-fix-cs-ticket-view-restore-page-validation.md`（completed，其「18 域 CRUD 页面完整 e2e 套件」Deferred 本计划承接）、`docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（同批 #1，种子库使 CRUD 列表有行可观测——本计划冒烟级不依赖种子，但种子落地后列表非空更有意义）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`/`find`，非采信旧记忆）：

- **Playwright 基础设施已就绪（0637-1 建立）**：根 `package.json`（`@playwright/test`）+ `playwright.config.ts`（webServer 自动启动 `quarkus-run.jar` + JVM 参数 `-Dnop.auth.service-public=true -Dnop.auth.login.allow-create-default-user=true` + `SKIP_WEBSERVER=1` 复用 + chromium 单项目 `channel: 'chrome'` fallback）+ `tests/e2e/auth.ts`（`performLogin`）+ `tests/e2e/fixtures.ts`（`loginAndNavigate(page, hashRoute)` + console error 检查器）+ `tests/e2e/global-setup.ts`。
- **既有 E2E 覆盖（34 spec 全绿，1107-1 后 35 spec）**：`tests/e2e/dashboards/`（10 域看板 `*.smoke.spec.ts` + `_helper.ts` `runDashboardSmoke`）+ `tests/e2e/reports/`（24 报表页 + `_helper.ts` `runReportSmoke`）+ `tests/e2e/crud/cs-kb-suggestion.smoke.spec.ts`（1107-1 新增，唯一 CRUD spec，证明 `/ErpCsTicket-main` 路由可达 + add 表单渲染 + `suggestForTicket` GraphQL 200）。
- **CRUD 页面路由范式确认**：AMIS SPA hash 路由 `/#/{EntityName}-main`（如 `/ErpCsTicket-main`、dashboards 用 `/#/fin-dashboard-main`）。1107-1 cs-kb spec 已验证该模式：`loginAndNavigate(page, '/ErpCsTicket-main')` → `#main-content` / `.cxd-Page` 渲染 → add 按钮（`button:has(.fa-plus)`）→ 表单字段。
- **18 域 CRUD 页面存量（源 `main.page.yaml` 计数，排除 dashboard/report，`find` 逐域核实）**：master-data 24 / inventory 19 / purchase 20 / sales 15 / finance 34 / assets 20 / projects 18 / manufacturing 28 / quality 20 / maintenance 12 / crm 34 / cs 16 / hr 35 / aps 6 / logistics 7 / b2b 13 / contract 15 / drp 7。**合计 343 个 main 页**（含头/行/config/master 实体）。全部经 codegen 生成（`x:gen-extends` 引 `view.xml` page="main"）。
- **代表性实体已由 Java CRUD 冒烟（2328-2）确立**：每域 1 主实体的 Java 冒烟测试已存在（`TestErp{Domain}{Entity}CrudSmoke.java`），可作为浏览器 E2E 的实体选择基准（md:ErpMdPartner / inv:ErpInvStockMove / pur:ErpPurRequisition / sal:ErpSalQuotation / fin:ErpFinVoucherTemplate / ast:ErpAstAsset / prj:ErpPrjProject / mfg:ErpMfgRouting / qa:ErpQaInspectionTemplate / mnt:ErpMntVisit / crm:ErpCrmBundlePricing / cs:ErpCsTicketType / hr:ErpHrSurvey / aps:ErpApsOperationOrder / log:ErpLogShipment / b2b:ErpB2bAsn / ct:ErpCtContract / drp:ErpDrpPlan）。
- **冒烟级空库可行性（0637-1 确立）**：空 H2 库下 CRUD 列表页渲染 DOM（0 行）+ GraphQL `/graphql` 查询返回 200（空 items）+ add 表单渲染字段——满足冒烟级（渲染 + DOM + 200 + 无 console error）。数据驱动断言（列表有行、断言字段值）需种子库（同批 #1 plan 解除）。
- **页面校验安全网已恢复（1107-1）**：`validate-page-model=true` 默认开，启动期 schema 校验全绿（35 spec 全绿基线）。新发现的 view.xml 缺陷会在启动期或运行时暴露。
- **非保护区域**：纯新增 Playwright 测试 spec（`tests/e2e/crud/*.spec.ts`）+ 共享 helper，**零 ORM/契约/认证/生产配置变更**。E2E 是纯消费侧测试。属 `plan-first`（跨 18 域 + >5 文件 + 跨多会话）。

剩余差距：(1) 18 域 CRUD 列表/表单页无浏览器层回归（仅 dashboards/reports + 1 cs CRUD spec）；(2) CRUD 页面从未经浏览器运行时验证（0637-1 已证浏览器验证能抓真实 bug：34 page.yaml `/api/GenericApi` bug + 1107-1 ErpHrEmployee view bug）；(3) 代表性实体选择 + helper 抽象待 Phase 1 确认。

## Goals

- 落地 18 域 CRUD 列表/表单页冒烟回归套件——每域 1 个代表性「主单据头」实体（Phase 1 确认，基准 2328-2 Java 冒烟实体选择），共 18 spec。
- 每 spec 断言：列表页 DOM 渲染 + 关键元素存在（表格容器 / add 按钮）+ `/graphql` 查询返回 200 + 无未捕获 console error；add 表单打开后表单字段渲染。
- 抽象共享 `tests/e2e/crud/_helper.ts`（`runCrudListSmoke` 函数，镜像 dashboards/reports helper 范式），使每域 spec 极简委派。
- 复用 0637-1 已建立的基础设施（auth/fixtures/config），零新增基础设施。
- 解除 0637-1 + 1107-1「CRUD 页面 E2E」Deferred 触发条件。

## Non-Goals

- **不**做数据驱动断言（断言列表有 N 行 / 字段值为 X）——冒烟级（渲染 + DOM + 200 + 无错误），数据驱动需种子库（同批 #1 plan）+ 断言逻辑，归 successor。
- **不**覆盖全部 343 个 CRUD 页——每域 1 代表性主实体（18 spec），镜像 2328-2 Java 冒烟的「每域 1 主实体」口径；全实体覆盖归 successor（触发条件：CRUD 页面批量定制后需全实体回归时）。
- **不**做完整 CRUD 写操作（create→read→update→delete 真实持久化）——冒烟级不持久化（避免测试间状态污染 + 需 mandatory 字段知识）；写操作 E2E 归 successor（需种子数据 + 业务规则知识）。
- **不**做像素级视觉回归（screenshot diff）——同 0637-1 Non-Goal，归优化层 successor。
- **不**修改任何 `*.orm.xml`/`*.xbiz`/`*.page.yaml`/`*.view.xml`/生产代码——E2E 是纯消费侧测试。
- **不**做跨浏览器矩阵（Firefox/WebKit/移动）——同 0637-1 Non-Goal。
- **不**接入 CI 管道（归 2359-1 Deferred O-14）。

## Task Route

- Type: `implementation-only change`（纯新增 Playwright 测试 spec + helper，零生产代码/契约/模型变更）
- Owner Docs: `docs/references/playwright-e2e-guide.md`（Playwright 权威，已由 0637-1 定制化）、`docs/testing/e2e-runbook.md`（运行手册）、各域 `docs/design/<domain>/` use-cases（确认代表性主单据头实体）
- Skill Selection Basis: `nop-frontend-dev`（AMIS CRUD 页面 DOM 结构 + `crud` 组件 + 表单字段定位 + hash 路由 `/{Entity}-main`；0637-1/1107-1 已验证范式）。`nop-testing` 不适用（Java 后端测试范式，非浏览器 E2E）；`nop-backend-dev` 不适用（零生产后端变更）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`app-erp-all/target/quarkus-app/quarkus-run.jar`（0637-1 webServer 范式）。
- 认证：0637-1 确立的 `-Dnop.auth.login.allow-create-default-user=true` + UI 登录（`loginAndNavigate` helper）。
- 端口：默认 8080（`PLAYWRIGHT_PORT` 可切换）。
- 回滚策略：纯新增 `tests/e2e/crud/*.spec.ts`，失败不影响生产构建；删除新增文件即回滚。

## Execution Plan

### Phase 1 - 代表性实体确认 + helper 抽象 + 1 域范式证明（Explore + Add）

Status: planned
Targets: `tests/e2e/crud/_helper.ts`、`tests/e2e/crud/<domain>.smoke.spec.ts`（1 域范式）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 0637-1 基础设施可用

- [ ] `Decision`：代表性实体选择——以 2328-2 Java 冒烟实体为基准，但浏览器 E2E 倾向选「主业务单据头」（用户最常打开的列表页）而非 config 实体。逐域确认 1 实体 + 其 `/{Entity}-main` 路由可达性（抽查 18 域路由 hash 在 SPA 实际渲染）。记录最终 18 实体清单 + 路由。若某域 Java 冒烟实体是 config 类（如 cs `ErpCsTicketType`/qa `ErpQaInspectionTemplate`/fin `ErpFinVoucherTemplate`），浏览器层改选主单据头（如 cs `ErpCsTicket`/qa `ErpQaInspection`/fin `ErpFinVoucher`）并记理由。**残留风险**：某域主单据头 add 表单可能有 mandatory 字段阻断空库渲染；冒烟级仅断言字段可见、不断言可提交，故可接受。
      - Skill: `nop-frontend-dev`
- [ ] `Add`：`tests/e2e/crud/_helper.ts`——`runCrudListSmoke(page, testInfo, { entityRoute, listKeyword, addFormField })` 函数：`loginAndNavigate(page, '/' + entityRoute)` → 断言 `#main-content`/`.cxd-Page` 渲染 → 断言表格/crud 容器存在 + add 按钮存在 → 断言 `/graphql` POST 查询返回 200（监听 `entityRoute` 相关 query）→ 断言无 console error → 点击 add 按钮 → 断言表单字段渲染（`addFormField` 指定的 input name 可见）。镜像 dashboards/reports `_helper.ts` 委派范式。
      - Skill: `nop-frontend-dev`
- [ ] `Add | Proof`：1 域范式 spec（如 purchase `ErpPurOrder`）——调 `runCrudListSmoke` 证明 helper + 路由 + 断言全通。作为 Phase 2/3 复制锚点。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 18 域代表性实体清单 + 路由落盘（写入本计划），每实体一行理由（基准 2328-2 或改选主单据头的理由）。
- [ ] `_helper.ts` `runCrudListSmoke` 存在且非 stub（含 loginAndNavigate + GraphQL 200 断言 + 表单字段断言 + console error 检查）；1 域范式 spec 通过（解除 Phase 2/3 阻塞）。

### Phase 2 - 核心域 CRUD 冒烟（Add-heavy）

Status: planned
Targets: `tests/e2e/crud/{md,inv,pur,sal,fin}.smoke.spec.ts`（5 核心域）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 helper + 范式通过

- [ ] `Add`：按 Phase 1 范式补齐 5 核心域（master-data/inventory/purchase/sales/finance）CRUD 冒烟 spec，每域调 `runCrudListSmoke` 委派，传入该域代表性实体路由 + 表单字段名。
      - Skill: `nop-frontend-dev`
- [ ] `Proof`：运行 `npx playwright test tests/e2e/crud/ --workers=1`（含 Phase 1 范式域 purchase + 本批新增 md/inv/sal/fin，补齐至 5 核心域），验证全绿。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 5 核心域 CRUD spec 全绿；每页 DOM 渲染 + add 按钮存在 + GraphQL 200 + 表单字段渲染 + 无 console error。

### Phase 3 - 扩展域 CRUD 冒烟（Add-heavy）

Status: planned
Targets: `tests/e2e/crud/{ast,prj,mfg,qa,mnt,crm,cs,hr,aps,log,b2b,ct,drp}.smoke.spec.ts`（13 扩展域）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 核心域范式稳定

- [ ] `Add`：按同一范式补齐 13 扩展域 CRUD 冒烟 spec。
      - Skill: `nop-frontend-dev`
- [ ] `Proof`：运行 `npx playwright test tests/e2e/crud/ --workers=1`（全部 18 域 spec），验证全绿。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 18 域 CRUD 冒烟 spec 全绿（含 cs-kb-suggestion 既有 spec 不回归）；每页 DOM 渲染 + add 按钮存在 + GraphQL 200 + 表单字段渲染 + 无 console error。

### Phase 4 - 运行手册 + 基线 + Deferred 解除登记（Add）

Status: planned
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3 全绿

- [ ] `Add`：`docs/testing/e2e-runbook.md` 更新——crud/ 目录结构 + 18 域 CRUD spec 清单 + 新增 `npx playwright test tests/e2e/crud/` 套件层级（同 dashboards/reports 套件级别分层）。
      - Skill: none
- [ ] `Add`：`docs/testing/known-good-baselines.md` 增 CRUD E2E 基线行（spec 总数 35→53 + 全绿状态）。
      - Skill: none

Exit Criteria:

- [ ] e2e-runbook 含 CRUD spec 目录/清单/分层；known-good-baselines 含 CRUD E2E 基线行；0637-1 + 1107-1「CRUD 页面 E2E」Deferred 触发条件满足登记。

## Draft Review Record

- Independent draft review iteration 1: `accept` (ses_0bff9366effe9e7TYgIZLw3Qov) — 全部基线事实经实时仓库逐项核实，零虚假/陈旧事实：18 域 CRUD main.page.yaml 计数逐域精确匹配（合计 343）；18 个 Java 冒烟实体名逐一核实存在；Playwright 基础设施（auth.ts/fixtures.ts/playwright.config.ts）+ 35 spec 计数确认；路由 `/{Entity}-main` 经 cs-kb-suggestion.spec.ts 核实；`validate-page-model=true` 默认 + 35-spec 全绿基线确认；0637-1 + 1107-1 Deferred 承接链 + 触发条件逐一核实；反松弛扫描干净（0 optional/maybe/consider）；Phase exit vs Closure Gates 分层正确；item types 完整（规则 7）；Skill 选择合理（nop-frontend-dev 非 nop-testing，规则 8）；单结果面（规则 4/14）；implementation-only 自分类正确（零 ORM/契约/认证变更，非保护区域）。3 MINOR polish 已采纳：(M1) Phase 2「本批 5 核心域」措辞澄清为 md/inv/sal/fin 新增 + purchase 沿用 P1（消除 18-vs-19 歧义）；(M2) 「Level 2」编号改为描述性套件层级（对齐 live runbook 无数字编号）；(M3) Phase 1 Decision 补残留风险行。**草案审查已收敛，计划为可接受的执行契约。**

## Closure Gates

> 本计划为前端/浏览器 E2E（视觉/UX 驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增 18 CRUD spec + 既有 35 spec）+ 既有后端构建（确认 E2E 未污染后端）。

- [ ] 范围内行为完成（18 域 CRUD 冒烟 spec + `_helper.ts` 全绿）
- [ ] 相关文档对齐（e2e-runbook + known-good-baselines）
- [ ] 已运行验证：`npx playwright test`（全套件：既有 35 + 新增 18 CRUD spec 全绿）+ `mvn clean install -DskipTests`（154 模块，确认 E2E 新增文件无后端污染——在根 tests/，非 reactor 模块）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 全实体 CRUD 页面覆盖（343 页）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划每域 1 代表性主实体（18 spec），镜像 2328-2 Java 冒烟口径。全 343 页（含行/config/master 实体）覆盖成本高、边际收益递减，属覆盖扩展。
- Successor Required: `yes`
- Trigger Condition: 当 CRUD 页面批量定制后需全实体浏览器回归，或按域推进全实体 CRUD e2e 覆盖时。

### CRUD 写操作 E2E（create/update/delete 真实持久化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 冒烟级不持久化（避免测试间状态污染 + 需 mandatory 字段/业务规则知识）。完整 CRUD 写操作需种子数据 + 字段知识 + 状态清理，是更深层端到端验证。
- Successor Required: `yes`
- Trigger Condition: 当种子库（同批 #1 plan）落地 + 需验证 CRUD 写操作端到端时。

### 数据驱动 CRUD 列表断言（列表有 N 行 / 字段值）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 空库冒烟断言「列表 DOM 存在 + GraphQL 200」，非「列表有 N 行」。数据断言需种子库（同批 #1 plan）。
- Successor Required: `yes`
- Trigger Condition: 当种子库落地 + 需 CRUD 列表数据驱动回归时。

### 像素级视觉回归（screenshot baseline diff）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 同 0637-1 Non-Goal，像素 diff 属套件之上增值层。
- Successor Required: `yes`
- Trigger Condition: 当跨环境渲染稳定性可接受且产品要求像素级一致性时。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>
- Evidence: <待填写>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
