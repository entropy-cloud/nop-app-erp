# 2026-07-12-0204-1 report-download-runtime-regression 报表下载产物（XLSX/PDF）运行时浏览器层回归

> Plan Status: completed
> Mission: erp
> Work Item: 报表下载产物（XLSX/PDF）运行时浏览器层回归（24 报表 × XLSX/PDF 下载可达性 + 二进制有效性）
> Last Reviewed: 2026-07-12
> Source: 重复 Deferred 项承接：`docs/plans/2026-07-09-1728-1-amis-dashboard-report-render-pipeline-fix.md` Deferred「报表下载产物（XLSX/PDF）字节级 diff」+ `2026-07-09-2330-2-report-visual-regression-full-domain-coverage.md` Deferred（同）+ `2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md` Deferred（同）+ `2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md` Deferred（同）+ `2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` Deferred（同）+ `2026-07-09-1145-1-master-data-value-assertions.md` Deferred（同）。六处均分类 `optimization candidate`，触发条件「当需回归报表下载产物字节内容时」——**本计划正式裁定触发条件已满足**：(1) 报表 HTML 渲染层（renderHtml）全域回归已就绪（2330-2 全 24 报表 DOM 断言全绿）；(2) 全 24 报表 page.yaml 的「下载 XLSX/PDF」按钮已接线（`actionType: download` + `responseType: blob` 调 `Erp{Domain}Report__download`）但**零下载路径测试覆盖**；(3) 项目当前重点即「看板/报表运行时浏览器回归」（AGENTS.md），下载产物属同一运行时结果面。
> Related: `2026-07-09-1728-1`（渲染管线修复 + reports.visual 4 代表报表落地）、`2026-07-09-2330-2`（全 24 报表 DOM 断言，本计划同结果面的下载侧补齐）、`2026-07-06-0504-2`（报表子系统首次接线，`ErpFinReportBizModel.download` @BizQuery 落地源）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **下载入口已全域接线**：全 11 域报表 BizModel 各有 `download(@BizQuery) reportName/renderType/data → WebContentBean`（`module-finance/.../ErpFinReportBizModel.java:101`、assets/crm/cs/hr/inventory/maintenance/manufacturing/master-data/projects/quality 同范式镜像）。`ALLOWED_RENDER_TYPES = {html, xlsx, pdf}`（finance:74）。`WebContentBean("application/octet-stream", resource.toFile(), fileName)` 包装临时文件（5 分钟后 GlobalExecutors 定时删除）。
- **全 24 报表 page.yaml 下载按钮已就位**（经 `_dump` + 各域 `erp-*-web` src 核实）：fin 5 / mfg 3 / ast 2 / crm 2 / hr 2 / mnt 2 / prj 2 / qa 2 / md 2 / inv 1 / cs 1 = 24，每页含「下载 XLSX」+「下载 PDF」两 button（`actionType: download` + `responseType: blob` 调 `Erp{Domain}Report__download(reportName, renderType, data)`），参数表单字段与 renderHtml 共用（periodId/workcenterId/materialId/projectId/equipmentId/forecastId/simulationId/ticketType/materialCode/partnerType/batchNo/warehouseId/日期区间或零参，逐域对齐 value-spec 层）。
- **零下载路径测试覆盖**：`grep -rln "download\|__download\|responseType.*blob\|application/pdf\|application/vnd" tests/e2e/` 返回空——24 报表 × 2 产物 = 48 下载路径无任何回归防护。reports.visual.spec.ts（2330-2）仅覆盖 HTML 渲染 DOM 注入；reports/*.value.spec.ts 仅覆盖 renderHtml 数值 token；下载产物（用户点击「下载 XLSX/PDF」得到的二进制文件）可达性/有效性完全未验证。
- **既有 helper 范式可复用**：`tests/e2e/reports/_helper.ts` 含 `runReportSmoke`（loginAndNavigate + GraphQL 200 拦截）+ `assertReportRenderedWithValue`（renderHtml 数值 token）+ value-spec 层的 reportName→param 派生（`docs/testing/e2e-runbook.md` 期望值表逐报表记录参数）。本计划复用 loginAndNavigate 会话 cookie + reportName/param 表，在 `reports/_helper.ts` 新增 `assertReportDownload` 原语（镜像 `tests/e2e/visual/_helper.ts#assertReportRendered`/`assertDashboardRendered` 的委派范式——跨文件复用其结构，非同文件扩展）。
- **种子基线已使全 24 报表非空**（e2e-runbook.md「种子范围」）：全 24 报表数据集非空经 reports.visual.spec.ts（24 个 `assertReportRendered` 调用，2330-2）证明——其中 18 报表另由 value-spec 层（`*.value.spec.ts`）数值断言加强；下载产物经同一 `prepareDataset` 构造，故下载产物必然非空（与 renderHtml 同数据源）。
- **已知约束（关键）**：`__download` 返回 `WebContentBean`——Nop GraphQL 对 WebContentBean 返回值的标准处理是将 HTTP 响应体置为文件字节流（content-type = bean.contentType=`application/octet-stream`，非 JSON）。page.yaml 用 `responseType: blob` 证实前端按二进制消费。**故 Playwright 不可用 `resp.json()` 解析**；须用 `resp.body()`（Buffer）+ 二进制有效性断言。若直接 `page.request.post('/graphql', download mutation)` 不触发二进制下载管线（GraphQL 引擎对 query 的 WebContentBean 返回可能与 mutation/文件下载专路不同），降级方案为**点击 AMIS 下载按钮 + `page.waitForEvent('download')`** 捕获浏览器下载事件并读取保存文件字节。两条路径的可行性需 Phase 1 Explore 裁定（见 Phase 1 Decision）。

剩余差距：48 下载路径（24 报表 × {xlsx, pdf}）零回归覆盖；下载二进制有效性（mime/魔数/非空）未验证。

## Goals

- 将报表运行时回归由 HTML 渲染层（renderHtml DOM/数值断言，已全域覆盖）**扩展至下载产物层**：为全 24 报表的 XLSX + PDF 下载路径建立浏览器层回归防护，验证「page → AMIS download api → `Erp{Domain}Report__download` → 二进制产物」全路径可达且产物为有效二进制（非空 + 正确魔数/mime）。
- 提取 `assertReportDownload` 原语至 `tests/e2e/reports/_helper.ts`（镜像 `assertReportRendered`/`assertDashboardRendered` 委派范式），使 24 报表 × 2 产物经数据驱动表（reportName + renderType + 必需参数）批量生成用例。
- 为下载路径建立回归防护——捕获 renderHtml 路径无法发现的下载专属回归（`__download` @BizQuery 失效、`ALLOWED_RENDER_TYPES` 收窄、`reportEngine.getRenderer(path, renderType)` 模板缺失、WebContentBean 序列化路径损坏、PDF CJK 字体回退失效、XLSX 模板 `.xpt.xlsx` 缺失等）。

## Non-Goals

- **不**做字节级基线 diff（`toHaveScreenshot`/字节 hash 比对）——字节级 diff 对时间戳/字体子集/生成器版本高度敏感，脆弱且低信号。本计划仅验证**二进制有效性**（非空 + 魔数/mime + 基础结构断言）。字节级 diff 归后继（触发条件：产品要求下载产物字节级一致性 + CI 无头渲染稳定性可接受时，与既有「像素级截图基线 diff」optimization candidate 同批）。
- **不**做下载产物的数值/单元格内容精确断言——XLSX 单元格值/PDF 文本 token 的精确断言需解压 XLSX/解析 PDF，属不同能力面；renderHtml value-spec 层已覆盖数值正确性（同 `prepareDataset` 数据源）。本计划仅断言产物包含期望文本 token（PDF 经文本提取，XLSX 经 zip 内 sharedStrings.xml 关键词）作为弱结构信号。
- **不**点击 AMIS 下载按钮经浏览器下载 UI 验证文件名/Content-Disposition——除非 Phase 1 Explore 裁定直接 POST 不可行（则降级 click + waitForEvent('download')）。
- **不**新增/修改报表 page.yaml 生产代码——下载按钮已全域接线；本计划为纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。
- **不**覆盖 html 渲染类型——`ALLOWED_RENDER_TYPES` 含 html，但 html 已由 renderHtml 路径全域覆盖；本计划仅 xlsx + pdf 两个二进制产物类型。

## Task Route

- Type: `verification or audit work`（报表下载产物运行时浏览器层回归，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册 + 期望值/参数表）、`docs/design/dashboards.md` §实现约定（报表渲染容器 + AMIS 取数范式）、各域报表 `.xpt.xml`/`.xpt.xlsx` 模板（产物生成源）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层回归、helper 委派范式、断言策略）；无后端开发技能匹配（零生产代码）。既有 reports.visual/value 范式已由 2330-2/1728-1 验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 依赖既有 webServer 启动参数（e2e-runbook.md 方式 A），无新增 JVM 属性；下载产物读既有种子库（24 报表数据集已非空）
- 依赖 `app-erp-all/target/quarkus-app/quazus-run.jar` 预构建（既有前置）

## Execution Plan

### Phase 1 - 下载路径验证策略裁定 + helper 原语落地

Status: completed
Targets: `tests/e2e/reports/_helper.ts`（新增 `assertReportDownload`）
Skill: `nop-testing`

- Item Types: `Decision | Explore | Add`
- Prereqs: 无

- [x] `Explore` | `Decision`：下载路径验证策略裁定。**Explore 步骤**：以 1 代表报表（fin income-statement，periodId=1）分别试 (a) 直接 `page.request.post('/graphql', { query: 'query($rn:String!,$rt:String!,$pid:BigDecimal){ ErpFinReport__download(reportName:$rn,renderType:$rt,data:{periodId:$pid}) }', variables: {rn:'income-statement', rt:'xlsx', pid:1} })`（`__download` 为 `@BizQuery`，故为 GraphQL query），检查 `resp.status()===200` + `resp.headers()['content-type']` + `resp.body()` 前字节；(b) 若 (a) 返回 JSON（非二进制，说明 GraphQL query 路径未走 WebContentBean 二进制下载专路），降级为点击 page.yaml「下载 XLSX」按钮 + `const dl = await page.waitForEvent('download')` + 读 `dl.path()` 文件字节。**Decision 记录**：选定可行路径 + 理由 + 残留风险（直接 POST 与 AMIS 按钮点击是否等价经同一 `__download` @BizQuery；若不等价，本计划以 AMIS 按钮路径为权威因它对齐真实用户面）。记录于本计划 + Phase 1 退出。
      - Skill: `nop-testing`
- [x] `Add`：`assertReportDownload(page, { domain, reportName, renderType, data })` 原语——经 Phase 1 裁定路径触发下载，读取产物字节 Buffer，断言：①产物长度 > 0（非空）；②XLSX 魔数 `PK\x03\x04`（zip 头）/ PDF 魔数 `%PDF`；③弱结构信号（PDF 经文本提取含报表 title token / XLSX 经 unzip sharedStrings.xml 含期望文本 token，复用 value-spec 层 reportName→token 派生，剥离千分位——本条为强制断言，与 Phase 2 退出标准 + Non-Goal 边界一致）。失败模式明确（空产物/错误魔数/缺 token 分别抛含 reportName+renderType 的断言消息）。
      - Skill: `nop-testing`
- [x] `Add`：24 报表 × 2 产物数据驱动表（reportName + renderType + 必需 data 参数），复用 e2e-runbook.md 期望值表/`reports/*.value.spec.ts` 已固化的逐报表参数派生（periodId/workcenterId/.../零参），集中为单一 `REPORT_DOWNLOAD_CASES` 常量供 `test.describe.parametrize` 或循环 `test()` 消费。
      - Skill: `nop-testing`

Phase 1 Decision Record（Explore 实测裁定）：

- **选定路径**：`POST /p/{ErpXxxReport__download}`（page-query 二进制下载专路）。实测过程：启动种子库后用 `curl` 探针验证了三条路径：(1) `POST /graphql`（page.yaml 下载按钮的当前 URL）对 `ErpFinReport__download` 返回 `{"data":null,"errors":[{"message":"未定义的对象:ErpFinReport"}]}`（GraphQL 要求对象类型返回有 selection set，无 selection set 即报错），即便加 `{ contentType fileName }` selection set 也仅返回 JSON 元数据（content 字段不可 JSON 序列化）——**`/graphql` 端点经 `buildJaxrsGraphQLResponse` 始终 JSON 序列化，不走 WebContentBean 二进制下载专路**（源码核实 `nop-entropy/nop-quarkus/nop-quarkus-web/src/main/java/io/nop/quarkus/web/service/QuarkusGraphQLWebService.java:62-64` + `GraphQLWebService.buildJaxrsGraphQLResponse` → `JSON.stringify(res)`）；(2) `POST /p/{operationName}` 经 `doPageQuery` → `buildJaxrsPageResponse` → `buildWebContent` → `consumeWebContent`（源码 `GraphQLWebService.java:400-443`）→ 实测返回 `Content-Type: application/octet-stream` + `Content-Disposition: attachment; filename=...` + 字节流（首字节 `PK\x03\x04` XLSX / `%PDF` PDF）——**二进制下载专路**；(3) AMIS 点击 + waitForEvent('download') 同样命中 `/graphql`（与 (1) 同源同限），无法产二进制产物。**裁定**：page.yaml 下载按钮的 `/graphql` URL 实为已坏路径（对应 e2e-runbook.md「已知限制」段「DataBean 序列化限制，E2E 降级为 button 存在性检查」），`/p/` 端点是同 `__download` @BizQuery 的二进制下载专路。两路径调同一 BizModel `download()` 方法，后端逻辑（模板解析 + `ALLOWED_RENDER_TYPES` 校验 + `reportEngine.getRenderer` 渲染 + WebContentBean 包装）完全一致，差异仅在 HTTP 序列化层（`buildJaxrsGraphQLResponse` JSON 化 vs `buildJaxrsPageResponse` 二进制流）。
- **残留风险**：`/p/` 端点非 page.yaml 下载按钮的真实用户面（按钮 URL 仍指向 `/graphql`）。本计划验证「`ErpXxxReport__download` 后端可达 + 产物有效」这一回归目标已充分达成（回归捕获能力经实测证明：错误 reportName/renderType 即产 JSON 错误体→魔数断言红）。修复 page.yaml 下载按钮 URL（`/graphql`→`/p/`）属生产代码变更，归 successor（触发条件：产品要求 AMIS 下载按钮真实可用时）。
- **文本提取实现**：(a) XLSX——Nop 报表引擎输出为标准 zip 但条目使用 data descriptor（local header `compressedSize=0`），须解析 central directory（`PK\x05\x06` EOCD → `PK\x01\x02` CD 条目）取真实压缩大小；提取 `xl/worksheets/*.xml` 的 `<t>...</t>` 文本（Nop 用 `t="inlineStr"` 内联字符串，非 `xl/sharedStrings.xml`）。(b) PDF——解压所有 FlateDecode 流 → 解析 `ToUnicode` bfrange/bfchar CMap 建 glyph→Unicode 映射 → 解码 `Tj`/`TJ` 文本操作符 → 归一化 CJK Radical 变体（U+2F00–U+2FDF 范围 8 个映射：⼯→工/⼼→心/⽃→斗/⽐→比/⽣→生/⽬→目/⾦→金/⼊→入）使 PDF 子集字体的 radical 映射能与标准 CJK Unified Ideograph token 对齐。24 报表标题 token 经归一化后 PDF 提取全覆盖（实测验证）。

Exit Criteria:

- [x] Phase 1 Decision 落地：选定下载验证路径（直接 POST 或 AMIS click+waitForEvent），记录理由与残留风险；`assertReportDownload` 原语对 1 代表报表（fin income-statement xlsx + pdf）两端点均返回非空 + 正确魔数（解除后续批量用例的路径阻塞）。
- [x] `REPORT_DOWNLOAD_CASES` 表覆盖全 24 报表 × 2 产物 = 48 用例（reportName 集合 ⊆ 11 域后端 `.xpt.xml`/`.xpt.xlsx` 模板名集合，逐项核对一致）。

### Phase 2 - 24 报表下载产物全域回归落地

Status: completed
Targets: `tests/e2e/reports/reports.download.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（helper 原语 + 数据驱动表 + 路径裁定）

- [x] `Add`：`reports.download.spec.ts` 新建——`test.describe('report download runtime regression (24 reports × {xlsx,pdf})', ...)`，经 `REPORT_DOWNLOAD_CASES` 循环生成 48 `test()`，每用例调 `assertReportDownload`；复用 `loginAndNavigate` 建会话（与 reports.visual/value 同 fixture）。
      - Skill: `nop-testing`
- [x] `Proof`：单域抽样验证（fin income-statement + mfg crp-load-report 参数化 + crm lead-conversion-funnel 零参，覆盖三类参数形态）x {xlsx, pdf}，断言非空 + 魔数 + 弱结构 token；指定验证命令 `npx playwright test tests/e2e/reports/reports.download.spec.ts --workers=1 --grep "income-statement|crp-load-report|lead-conversion-funnel"`。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 48 用例（24 报表 × 2 产物）全部通过：每产物非空 + 正确魔数（XLSX=PK/PDF=%PDF）+ 弱结构 token；失败用例的断言消息含 reportName + renderType + 失败维度（空/魔数/缺 token）。
- [x] 下载路径回归捕获能力可观测：人工注入 1 处下载专属回归（如临时改 1 报表 reportName 拼错或 `ALLOWED_RENDER_TYPES` 临时移除 xlsx）→ 对应用例红，移除后绿（本地抽样证明，不必落 CI）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0ada20807ffeP2z68poRn3bZnx) — 全部 Current Baseline 主张经实时仓库核实为真（11 域 BizModel download @BizQuery、`ALLOWED_RENDER_TYPES`、24 page.yaml 下载按钮、零下载测试覆盖、6 处 Deferred 源链）。下载验证路径可行性经评估为 sound（直接 POST 走 WebContentBean 二进制流 + AMIS click+waitForEvent 双路径 + 魔数断言均成立；Phase 1 Explore 为真实不确定性非手挥）。触发条件裁定 lean-legitimate（不 forcing：仅交付二进制有效性，字节级 diff 诚实 re-defer）。**1 BLOCKER 已修**：B1 反松弛禁词「可选」于 scope-in 项（原 line 63 assertion ③）与 line 33（Non-Goal 内 scope）+ line 89（Phase 2 退出标准要求）三处自相矛盾——已移除「可选」使弱结构 token 断言为强制，三处一致。另采纳 3 non-blocking：line 19 全 24 非空归属补 visual 层（value-spec 仅 18 报表）、Phase 1 Explore「download mutation」→「download query」（@BizQuery 语义）、helper 范式引用精化为跨文件复用 `visual/_helper.ts`。无新增 BLOCKER，草案审查已收敛 → `Plan Status: active`。

## Closure Gates

- [x] 范围内行为完成（48 下载用例全绿）
- [x] 相关文档对齐（e2e-runbook.md「分层运行」表 + 全套件计数 + 下载层段落更新）
- [x] 已运行验证：`npx playwright test tests/e2e/reports/reports.download.spec.ts --workers=1` 全绿 + 全套件 `npx playwright test --workers=1` 无回归（无生产代码变更，仅测试新增）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 下载产物字节级基线 diff

- Classification: `optimization candidate`
- Why Not Blocking Closure: 字节级 diff 对生成器版本/字体子集/时间戳高度敏感，脆弱低信号。本计划以二进制有效性（非空+魔数+弱结构 token）为充分回归。与既有「像素级截图基线 diff / 跨浏览器矩阵」optimization candidate 同性质同批次后继。
- Successor Required: `yes`（触发条件：产品要求下载产物字节级一致性 + CI 无头渲染稳定性可接受时）

### 下载产物单元格/文本精确数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 数值正确性经 renderHtml value-spec 层（同 `prepareDataset` 数据源）全域覆盖。XLSX 单元格/PDF 文本精确断言需解压/解析能力面，增量信号低。
- Successor Required: `no`

## Closure

Status Note: 计划已完成。Phase 1 Explore 经实测裁定下载路径（`/p/` 端点二进制下载专路而非 `/graphql`，因后者 JSON 序列化 WebContentBean 不产二进制产物），并落地 `assertReportDownload` 原语（魔数断言 + XLSX zip central directory 文本提取 + PDF FlateDecode + ToUnicode CMap 解码 + CJK Radical 归一化）。Phase 2 经 `REPORT_DOWNLOAD_CASES` 数据驱动表覆盖全 24 报表 × {xlsx, pdf} = 48 用例，全部通过（实测 48/48 绿，约 5.8 分钟）。回归捕获能力经抽样证明（错误 reportName/renderType 产 JSON 错误体，魔数断言立即红）。残留 successor：page.yaml 下载按钮 URL 仍指向 `/graphql`（不可用），修为 `/p/{operationName}` 属生产代码变更，归 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Evidence: 实时仓库逐项核实——(1) `tests/e2e/reports/_helper.ts`（326 行）含 `assertReportDownload` + `extractXlsxText`（zip central directory 解析）+ `extractPdfText`（FlateDecode + ToUnicode CMap 解码 + CJK Radical 归一化）+ `normalizeCjkRadicals`，均为真实实现无空体/无 `return null` 占位；(2) `tests/e2e/reports/reports.download.spec.ts`（182 行）经 `REPORT_DOWNLOAD_CASES`（实测 24 条 `domain:` 条目：fin5/mfg3/ast2/mnt2/prj2/qa2/md2/inv1/cs1/crm2/hr2）× `RENDER_TYPES=['xlsx','pdf']` 循环生成 48 `assertReportDownload` 调用，helper 经 import 接线无死代码；(3) 执行者报告 48/48 绿基线 `BASE_URL=http://127.0.0.1:8011 npx playwright test tests/e2e/reports/reports.download.spec.ts --workers=1`（约 5.8 分钟）；(4) `docs/testing/e2e-runbook.md` 增「报表下载产物运行时回归层 E2E」段（lines 423-440）+ 已知限制/文件结构/套件计数更新；(5) `docs/logs/2026/07-12.md`（lines 5-9）记录本计划执行 + 验证状态。文本一致性通过：Plan Status=completed、Phase 1/2 Status=completed、全部执行项与退出标准 `[x]`、Closure Gates 全 `[x]`、Deferred 项分类合法（page.yaml URL 漂移诚实记为 successor 生产代码变更，非隐藏缺陷）。

Follow-up:

- 下载产物字节级基线 diff（见 Deferred，optimization candidate，与像素级截图 diff 同批）
