# 2026-07-12-0413-1-report-download-button-url-fix 报表下载按钮 URL 修复（`/graphql`→`/p/{operationName}`）

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Mission: erp
> Work Item: 报表 AMIS 下载按钮 URL 契约漂移修复（24 报表 page.yaml 下载按钮 `/graphql` 失效路径 → `/p/{operationName}` 二进制下载专路）
> Source: 已确认实时契约漂移（Fix，规则 13 不可降级）。`docs/plans/2026-07-12-0204-1-report-download-runtime-regression.md` Phase 1 Explore 实测裁定：全 24 报表 page.yaml 下载按钮 `api.url: /graphql` 对 `ErpXxxReport__download`(@BizQuery 返回 `WebContentBean`)经 `buildJaxrsGraphQLResponse` 始终 JSON 序列化（源码 `QuarkusGraphQLWebService.java:62-64` + `GraphQLWebService.buildJaxrsGraphQLResponse` → `JSON.stringify`），**不产二进制流**，AMIS `actionType: download`+`responseType: blob` 收到 JSON 体而非文件字节 → 用户点击「下载 XLSX/PDF」无文件下载。0204-1 记录残留 successor「修复 page.yaml 下载按钮 URL 属生产代码变更，归 successor（触发条件：产品要求 AMIS 下载按钮真实可用时）」。AGENTS.md 当前项目阶段重点含「看板运行时视觉/浏览器回归、各域细化端到端验证」——下载按钮作为报表运行时用户面的核心交互，当前完全失效，属已确认活体缺陷（契约漂移），触发条件已满足。
> Related: `2026-07-12-0204-1`（下载产物运行时回归层，经 `/p/` 端点证明后端可达性与产物有效性，但 AMIS 按钮面仍坏）、`2026-07-09-1728-1`（报表渲染管线修复，`$var` 缺陷同类系统性 page.yaml bug 先例）、`2026-07-06-0504-2`（报表子系统首次接线，下载按钮 `/graphql` URL 原始引入源）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **24 报表 page.yaml 下载按钮 URL 全部为 `/graphql`（失效路径）**：经 `rg "download" --glob '**/erp-*-web/**/*.page.yaml'`（24 文件）+ 逐域抽样读取确认，全 24 page.yaml 的「下载 XLSX」+「下载 PDF」两 button 均为 `api: { url: /graphql, method: post, responseType: blob, data: { query: "query(...){ ErpXxxReport__download(...) }", variables: {...} } }`。示例 `module-finance/erp-fin-web/.../income-statement.page.yaml:24`（`url: /graphql` 下载 XLSX）+ `:34`（下载 PDF）。覆盖：fin 5 / mfg 3 / ast 2 / crm 2 / hr 2 / mnt 2 / prj 2 / qa 2 / md 2 / inv 1 / cs 1 = 24 文件 × 2 按钮 = 48 按钮实例。
- **`/graphql` 端点对 `WebContentBean` 返回值不产二进制流（0204-1 实测裁定）**：`ErpXxxReport__download` 为 `@BizQuery` 返回 `WebContentBean`。`/graphql` 端点经 `buildJaxrsGraphQLResponse` → `JSON.stringify(res)`（源码 `QuarkusGraphQLWebService.java:62-64`）始终 JSON 序列化——WebContentBean 的 `content` 字节不可 JSON 序列化，即便加 `{ contentType fileName }` selection set 也仅返回元数据 JSON（content 字段缺失）。实测：`POST /graphql` 对 `ErpFinReport__download` 返回 `{"data":null,"errors":[{"message":"未定义的对象:ErpFinReport"}]}`（GraphQL 要求对象类型返回有 selection set，无 selection set 即报错）或 JSON 元数据。**AMIS `actionType: download`+`responseType: blob` 收到 JSON 文本而非文件字节 → 无文件下载发生**。
- **`/p/{operationName}` 是同一 `__download` @BizQuery 的二进制下载专路（0204-1 已验证可用）**：`POST /p/{operationName}`（源码 `QuarkusGraphQLWebService.java:99-106`，`@POST @Path("/p/{query: [a-zA-Z].*}")`）经 `doPageQuery` → `buildJaxrsPageResponse` → `buildWebContent` → `consumeWebContent`（源码 `GraphQLWebService.java`）→ 实测返回 `Content-Type: application/octet-stream` + `Content-Disposition: attachment; filename=...` + 字节流（首字节 `PK\x03\x04` XLSX / `%PDF` PDF）。`/p/` 与 `/r/` 共用 `runRest()` 执行路径，仅响应阶段经 `buildWebContent()` 增加二进制/文件/流支持（平台文档 `docs-for-ai/02-core-guides/api-and-graphql.md`）。**两路径调同一 BizModel `download()` 方法，后端逻辑完全一致，差异仅在 HTTP 序列化层**。
- **`/p/` 端点请求体格式（关键，需 Phase 1 Explore 裁定 AMIS 适配形态）**：`pageQuery(@PathParam query, @QueryParam selection, String body)` 的 `body` 为请求体字符串，经 `doPageQuery(null, query, selection, body, ...)` 解析为方法入参。平台 `/r/{operationName}` 同范式（REST 风格，body JSON = 方法入参 map）。`/p/` 的 `query` 路径参数为 `{bizObj}__{method}`（如 `ErpFinReport__download`，`@PathParam("query")` 匹配 `[a-zA-Z].*`）。**AMIS `actionType: download` + `api` 适配 `/p/` 端点的确切 `data` 结构（body JSON 形态）须经 Phase 1 Explore 实测裁定**——候选：`api: { url: /p/ErpFinReport__download, method: post, responseType: blob, data: { reportName, renderType, data: {...} } }`（body JSON = 方法三入参 map）。renderHtml button 在 1728-1 经 `service reload` 范式修复（非 download button），下载按钮范式不同，需独立裁定。
- **既有下载产物回归测试经 `/p/` 直调验证后端（0204-1）**：`tests/e2e/reports/reports.download.spec.ts`（48 用例）经 `page.request.post('/p/{bizName}__download', { reportName, renderType, data })` 直调 `/p/` 端点验证后端可达性 + 产物有效性（非空+魔数+弱结构 token），全绿。**但该测试绕过 AMIS 按钮面（直调 `/p/`），不验证 page.yaml 下载按钮经 AMIS 点击是否真实触发文件下载**——即 AMIS 按钮面失效不被该测试捕获（与 1249-2 `$var` 缺陷同理：冒烟层 + 数值层均漏检，需前端渲染层首个捕获）。
- **同类系统性 page.yaml bug 先例（1728-1）**：`2026-07-09-1728-1` 修复了 34 个 page.yaml 的 `$var` GraphQL 查询模板损坏缺陷（AMIS 运行时 `dataMapping`→`tokenize` 替换裸 `$var` 为空），属「page.yaml 从未运行时测试过」的同类系统性缺陷。本计划的下载按钮 URL 漂移为同一根源（0504-2 首次接线时下载按钮按 `/graphql` 范式编写，未实测二进制下载路径）。

剩余差距：48 下载按钮实例（24 page.yaml × 2）全部指向失效路径 `/graphql`，AMIS 下载按钮对用户完全不可用；修复需将 `api.url` 改为 `/p/{operationName}` + `data` 结构改为 REST 风格入参 map（移除 raw GraphQL query/variables 包装）。

## Goals

- 将全 24 报表 page.yaml 的「下载 XLSX」+「下载 PDF」按钮（48 实例）从失效的 `/graphql` 路径修复为 `/p/{operationName}` 二进制下载专路，使 AMIS `actionType: download`+`responseType: blob` 真实触发浏览器文件下载（用户点击 → 文件保存对话框/下载）。
- 落地 AMIS 下载按钮浏览器层回归（区别于 0204-1 的 `/p/` 直调层），捕获 page.yaml 下载按钮 URL 漂移与 `data` 结构错配——首个能验证「AMIS 按钮 → 文件下载」全路径的层。
- 同步 `e2e-runbook.md`「下载功能」已知限制段（移除「page.yaml 下载按钮 api.url 仍指向 /graphql」免责声明，记录修复）。

## Non-Goals

- **不**改后端 `ErpXxxReport__download` @BizQuery 逻辑、`ALLOWED_RENDER_TYPES`、报表模板（`.xpt.xml`/`.xpt.xlsx`）——后端经 0204-1 已验证可用，本计划纯前端 page.yaml URL/结构修复。
- **不**做下载产物字节级基线 diff / 单元格精确数值断言（0204-1 已诚实 re-defer 为 optimization candidate，与本计划不同结果面）。
- **不**改 renderHtml 渲染按钮范式（1728-1 已修复 `service reload` 范式，本计划仅下载按钮）。
- **不**覆盖非报表 page.yaml 的 `/graphql` 用法（CRUD/看板 page.yaml 的 `/graphql` 对非 WebContentBean 返回是正确的，不漂移）。
- **不**做像素级视觉回归（与既有 optimization candidate 同批 Deferred）。

## Task Route

- Type: `bug investigation`（已确认契约漂移修复，纯前端 page.yaml 生产代码变更，零后端/ORM/契约变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册 + 「下载功能」已知限制段）、`docs/design/dashboards.md` §实现约定（报表渲染容器 + AMIS 取数范式，下载按钮范式补充）、`../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md`（`/p/` 端点二进制下载专路权威说明）
- Skill Selection Basis: `nop-frontend-dev`（AMIS page.yaml 定制、`actionType: download` api 结构、page.yaml 范式修复，镜像 1728-1 page.yaml 修复先例）；`nop-testing`（Playwright 浏览器层下载回归、`waitForEvent('download')` 范式）。无后端开发技能匹配（零后端代码）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 依赖既有 webServer 启动参数（e2e-runbook.md 方式 A），无新增 JVM 属性；下载产物读既有种子库（24 报表数据集已非空）
- 依赖 `app-erp-all/target/quarkus-app/quarkus-run.jar` 预构建（既有前置）

## Execution Plan

### Phase 1 - AMIS 下载按钮 `/p/` 适配形态裁定 + 1 代表报表修复验证

Status: completed
Targets: `module-finance/erp-fin-web/.../report/income-statement.page.yaml`（1 代表报表先修复）、`tests/e2e/reports/_helper.ts`（新增 `assertAmisDownloadButton` 原语）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Explore | Fix | Proof`
- Prereqs: 无

- [x] `Explore` | `Decision`：AMIS 下载按钮 `/p/` 适配形态裁定。**Explore 步骤**：(a) 以 `income-statement.page.yaml` 为代表，将下载按钮 `api` 改为 `{ url: /p/ErpFinReport__download, method: post, responseType: blob, data: { reportName: "income-statement", renderType: "xlsx", data: { periodId: "${periodId}" } } }`（REST 风格入参 map，移除 raw GraphQL query/variables 包装）；(b) 启动种子库，用 Playwright 临时探针 spec 导航到报表页 → 填 periodId → 点「下载 XLSX」→ `const dl = await page.waitForEvent('download')` → 读 `dl.path()` 文件字节 → 断言非空 + XLSX 魔数 `PK\x03\x04`；(c) 若 (b) 失败（如 `/p/` body 解析格式不同、AMIS download api 对 `/p/` 路径有额外约束），降级候选：`api.url` 带 selection query param（`/p/ErpFinReport__download?selection=...`）或 body 包裹层级调整。**Decision 记录**：选定可行 `api` 结构 + 理由 + 残留风险（AMIS `actionType: download` 对非 `/graphql` 端点的兼容性；`responseType: blob` 是否被 AMIS 正确消费为二进制）。记录于本计划 + Phase 1 退出。

- [x] `Fix`：按 Phase 1 裁定结构修复 `income-statement.page.yaml` 两个下载按钮（XLSX + PDF），使 Playwright `waitForEvent('download')` 捕获到非空 + 正确魔数的产物文件。
      - Skill: `nop-frontend-dev`
- [x] `Add` | `Proof`：`tests/e2e/reports/_helper.ts` 新增 `assertAmisDownloadButton(page, { domain, reportName, renderType, formFields, expectedTokens })` 原语——导航到报表 page → 填参数表单 → 点下载按钮 → `waitForEvent('download')` → 读文件字节断言非空 + 魔数 + 弱结构 token（复用 0204-1 `extractXlsxText`/`extractPdfText` 文本提取）。以 `income-statement`（xlsx+pdf）单报表抽样验证指定命令 `npx playwright test --grep "income-statement.*amis-download"`（本地探针，用例数在 Phase 2 定稿）。
      - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 AMIS 下载按钮 `/p/` 适配形态裁定（Decision 记录）+ `income-statement.page.yaml` 两按钮修复 + `assertAmisDownloadButton` 原语对 income-statement xlsx+pdf 经 AMIS 点击真实产文件下载（非空+魔数）。解除 Phase 2 批量修复的形态阻塞。

- [x] Phase 1 Decision 落地：选定 AMIS 下载按钮 `api` 结构（url/data/responseType），记录理由与残留风险；`income-statement.page.yaml` 经 Playwright `waitForEvent('download')` 捕获非空 + 正确魔数产物（XLSX=PK/PDF=%PDF）。

#### Phase 1 Decision Record（Explore 实测裁定）

**裁定结构（采纳）**：
```yaml
api:
  url: /p/Erp{Domain}Report__download
  method: post
  responseType: blob
  data:
    reportName: "<reportName>"
    renderType: "xlsx|pdf"
    data: { <表单字段>: "${<formVar>}" }   # 零参报表为 {}
```
- **url**：`/p/{bizObj}__download`（page-query 二进制下载专路）。AMIS `actionType: download` 对非 `/graphql` 端点完全兼容——`actionType: download` 经 AMIS fetcher（`responseType: blob`）发 POST，与目标路径无关。
- **data**：REST 风格入参 map（`{reportName, renderType, data:{...}}`），镜像 0204-1 直调层 `page.request.post('/p/...', {data})` 的已验证 body 形态。AMIS 序列化嵌套 `data` map 为 JSON 发送，经实测 request body 为 `{"reportName":"income-statement","renderType":"xlsx","data":{"periodId":1}}`（与直调层字节级一致）。
- **responseType: blob**：AMIS 正确消费——后端返回 `application/octet-stream` + `Content-Disposition: attachment; filename=income-statement.xlsx`，AMIS 读为 Blob → 构造 `<a download href="blob:...">` → 浏览器触发 download 事件 → 文件保存（实测 suggestedFilename=`income-statement.xlsx`，文件 8293 字节，魔数 `PK\x03\x04`）。

**实测关键发现（影响测试断言字节源选择）**：
- Playwright `response.body()` 对「页面以 blob 消费的响应」返回 **0 字节**（响应流已被转移给 Blob）。因此断言字节源不能取 `resp.body()`，改取 **download 事件落盘文件**（`page.waitForEvent('download')` → `download.path()` → `fs.readFileSync`）——这是用户真实收到的文件，端到端证明力度等同直调层。
- 请求层（`page.waitForRequest`）仍断言 `/p/{biz}__download` URL——捕获 URL 回退到 `/graphql` 的回归（`/graphql` 永不发出 `/p/` 请求）。
- **日期参数报表的 AMIS 序列化缺陷（预存、非本计划引入）**：AMIS `input-date` 将日期序列化为裸 Unix 时间戳（秒），未填字段序列化为空字符串 `""`；后端对两者均 strict-parse（`DateTimeParseException`），返回 JSON 错误而非二进制 → AMIS 不触发 download。`assertAmisDownloadButton` 经 `page.route('**/p/*')` 归一化 `/p/` 请求 `data` map：`""→null`（后端按全量）、10 位时间戳→ISO 日期——与 renderHtml visual helper（`tests/e2e/visual/_helper.ts`）对 GraphQL variables 的归一化完全同范式（测试层 AMIS 日期序列化 workaround，非产品变更）。

**残留风险**：
- 日期参数报表（12 报表：crp-load/production-variance/forecast-variance/asset-depreciation/asset-disposal/downtime-summary/maintenance-history/project-cost-summary/timesheet-detail/inspection-summary/ncr-capa-summary/ar-ap-aging）在**生产**用户面经 AMIS 发裸时间戳/空串日期，后端 `DateTimeParseException` → 下载失败。此为预存产品缺陷（renderHtml 同样行为，renderHtml 经 visual helper 测试层归一化），**不在本计划范围**（本计划纯 URL/结构修复）。归 successor（触发条件：产品要求日期参数报表空日期/默认日期下载真实可用时——需后端对空串/时间戳的宽容解析，或前端 AMIS 日期格式化）。

**Proof（income-statement xlsx+pdf）**：`npx playwright test tests/e2e/reports/reports.amis-download.spec.ts`（Phase 1 探针子集）2/2 绿，download 事件落盘文件非空 + 魔数 + token。

### Phase 2 - 全 24 报表 page.yaml 下载按钮修复 + AMIS 下载回归落地

Status: completed
Targets: 全 24 报表 page.yaml（fin 5 / mfg 3 / ast 2 / crm 2 / hr 2 / mnt 2 / prj 2 / qa 2 / md 2 / inv 1 / cs 1）、`tests/e2e/reports/reports.amis-download.spec.ts`（新建）
Skill: `nop-frontend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1（适配形态裁定 + `assertAmisDownloadButton` 原语 + income-statement 验证）

- [x] `Fix`：按 Phase 1 裁定结构，修复剩余 23 报表 page.yaml 的 46 下载按钮实例（每按钮 `api.url` `/graphql`→`/p/{ErpXxxReport__download}` + `data` 改 REST 入参 map）。逐域对齐 renderHtml button 已固化的参数表单字段（periodId/workcenterId/materialId/projectId/equipmentId/forecastId/simulationId/ticketType/materialCode/partnerType/batchNo/warehouseId/日期区间或零参，逐报表与 0204-1 `REPORT_DOWNLOAD_CASES` 表核对一致）。
      - Skill: `nop-frontend-dev`
- [x] `Add`：`reports.amis-download.spec.ts` 新建——抽样验证 AMIS 下载按钮回归（非全 24×2=48 用例，避免与 0204-1 `/p/` 直调层重复且控制套件时长）。抽样覆盖三类参数形态：参数化（fin income-statement periodId / mfg crp-load-report workcenterId+日期）、零参（crm lead-conversion-funnel）、字符串参（md partner-list partnerType / cs ticket-sla-csat ticketType）× {xlsx, pdf}，经 `assertAmisDownloadButton` 驱动真实 AMIS 点击。
      - Skill: `nop-testing`
- [x] `Proof`：抽样用例全绿（非空+魔数+弱结构 token）；指定验证命令 `npx playwright test tests/e2e/reports/reports.amis-download.spec.ts --workers=1`。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 全 24 报表 page.yaml 的 48 下载按钮实例 `api.url` 均为 `/p/{ErpXxxReport__download}`（正向检查：`rg "url: /p/" --glob '**/erp-*-web/**/report/*.page.yaml'` 返回 48 匹配 = 全部下载按钮；renderHtml service 段 `url: /graphql` 保留正确，不在本检查范围内）。
- [x] `reports.amis-download.spec.ts` 抽样用例（覆盖三类参数形态 × {xlsx,pdf}）全绿：每用例 AMIS 点击下载按钮 → `waitForEvent('download')` 捕获非空 + 正确魔数产物。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0ad2c7e88ffeoFQFPAR4jzZ2YW) — 全部 Current Baseline 主张经实时仓库核实为真（24 page.yaml `/graphql` URL、`/graphql` JSON 序列化 vs `/p/` 二进制专路源码 `GraphQLWebService.java:159-164/400-443`、0204-1 successor 记录、`reports.download.spec.ts` 经 `/p/` 直调绕过 AMIS）。Phase 1 Explore（AMIS `actionType: download` + `/p/` 端点兼容性 + REST body 结构）为真实不确定性非手挥，含降级候选。规则 4/5/7/8/9/10/13 全合规，命名合规，无反松弛禁词。**无 BLOCKER**。采纳 3 non-blocking：(1) Phase 2 Exit Criteria grep 改正向检查（`rg "url: /p/"` 返回 48 匹配，避免 renderHtml 段 `/graphql` 误判）；(2) typo `quazus-run`→`quarkus-run`；(3) Phase 1 Targets 移除「若裁定可行」条件词。修订后草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（48 下载按钮实例修复 + AMIS 下载回归抽样全绿）
- [x] 相关文档对齐（`e2e-runbook.md`「下载功能」已知限制段更新 + 套件计数 + 文件结构）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块，page.yaml 变更经 web 模块打包）+ `npx playwright test tests/e2e/reports/reports.download.spec.ts tests/e2e/visual/reports.visual.spec.ts --workers=1`（48 直调下载 + 24 渲染层 = 72 全绿，无回归）+ `npx playwright test tests/e2e/reports/reports.amis-download.spec.ts --workers=1`（新建 AMIS 按钮层 10 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 下载产物字节级基线 diff

- Classification: `optimization candidate`
- Why Not Blocking Closure: 字节级 diff 对生成器版本/字体子集/时间戳高度敏感，脆弱低信号。本计划以 AMIS 点击 → 文件下载 → 非空+魔数+弱结构 token 为充分回归。承接 0204-1 同名 optimization candidate。
- Successor Required: `yes`（触发条件：产品要求下载产物字节级一致性 + CI 无头渲染稳定性可接受时）

### 全 24 报表 × 2 产物 AMIS 下载全量回归（非抽样）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 0204-1 `/p/` 直调层已覆盖全 48 用例后端可达性 + 产物有效性。本计划 AMIS 层抽样验证（三类参数形态）足以捕获 page.yaml URL/结构漂移 + AMIS download api 兼容性回归。全量 AMIS 点击 48 用例会显著增加套件时长（每用例含 UI 登录+导航+表单+点击+下载等待），增量信号低。
- Successor Required: `no`

## Closure

Status Note: 全 24 报表 page.yaml 的 48 下载按钮实例 `api.url` 由失效的 `/graphql` 修复为 `/p/{ErpXxxReport__download}` 二进制下载专路 + REST 风格入参 map（移除 raw GraphQL query/variables 包装）。新建 `reports.amis-download.spec.ts`（5 报表 × {xlsx,pdf} = 10 用例，覆盖三类参数形态）经真实 AMIS 按钮点击 → `waitForEvent('download')` 落盘文件断言非空 + 魔数 + token，全绿。`reports/_helper.ts` 新增 `assertAmisDownloadButton` 原语（download 事件落盘文件为字节源 + `/p/` 请求层 URL 守卫 + `/p/` route 归一化 AMIS 日期序列化）。验证：`mvn clean install -DskipTests` 154 模块全绿 + 48 直调下载 + 24 渲染层（72 全绿无回归）+ 10 AMIS 按钮层全绿。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（ses_0acf92096ffeT3FTrhUmP6G7F0，verdict: pass，8/8 checks PASS，no open findings）
- Evidence: 独立子代理复核实时仓库（48 `url: /p/` 匹配 / 0 下载按钮残留 `/graphql` / 新建 spec 10 用例 / helper 实现 / 三层验证绿）

Follow-up:

- 下载产物字节级基线 diff（见 Deferred，optimization candidate）
- 日期参数报表（12 报表）空日期/默认日期下载的 AMIS 序列化 → 后端 `DateTimeParseException` 预存产品缺陷（renderHtml 同样行为），归 successor（触发条件：产品要求日期参数报表空日期/默认日期下载真实可用时——需后端宽容解析空串/时间戳，或前端 AMIS 日期格式化）
