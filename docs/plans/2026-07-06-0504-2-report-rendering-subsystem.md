# 2026-07-06-0504-2-report-rendering-subsystem 报表渲染子系统

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: deferred items recorded in 8+ completed plans（1000-3 结账报告 / 0540-2 年度三大表 / 1707-1 CRP 报表 / 1838-2 差异报表 / 0427-1 预测差异报表 / 0700-1 追溯可视化 / 0700-2 员工净余额报表 / 2200-3 薪酬模拟对比报表）；owner docs `docs/architecture/print-template.md`、`docs/design/dashboards.md`、`docs/design/finance/`
> Related: `2026-07-02-1000-3-finance-period-close.md`（结账报告 Deferred）、`2026-07-05-0540-2-finance-period-close-annual-close.md`（年度三大表 Deferred）、`2026-07-05-1838-2-manufacturing-production-variance.md`（差异报表 Deferred）
> Audit: required

## Current Baseline

- **平台基础设施已接入**：`app-erp-all/pom.xml` 已依赖 `nop-report-service` + `nop-report-web`；`app.action-auth.xml` 已 `x:extends` `/nop/report/auth/nop-report.action-auth.xml`。平台报表 CRUD BizModel（`NopReportDefinition`/`NopReportDataset`/`NopReportDatasource`/`NopReportResultFile`）与报表管理页面**已可用**。
- **渲染入口已明确**：`IReportEngine.getRenderer(path, renderType)` / `getHtmlRenderer(path)`，应用 BizModel 注入 `IReportEngine` 调用（参考 `nop-report-demo/.../ReportDemoBizModel.java`）。模板 VFS 路径约定 `/nop/main/report/{reportName}`，参数经 `IEvalScope.setLocalValues(data)` 传入。
- **平台默认路线**：`../nop-entropy/docs-for-ai/02-core-guides/reporting-and-notification-integration.md` 明确——后台经营统计/数据集驱动报表/按模板导出 默认走 `nop-report`，应用项目**不应自建平行报表引擎或导出框架**，不应在 design owner doc 展开"报表数据集实体细节/模板存储结构/引擎内部实现"。
- **应用层零内容**：全仓无 `.xpt.xlsx`/`.xpt.xml` 报表模板文件；无注入 `IReportEngine` 的 ERP BizModel；无 ERP 报表数据集定义。`print-template.md` 提议的 `ErpSysDocumentTemplate` 实体**不存在**（且与平台 `NopReportDefinition` 语义重叠）。
- **看板占位**：`dashboards.md` 为各域定义完整指标口径（销售/采购/库存/财务看板，KPI+趋势+预警），各域 action-auth 含 dashboard 菜单分组，页面为占位 `page.yaml`；本期看板未实现。
- **待解除的下游 Deferred**（触发条件="nop-report 接线时"）：1000-3 结账报告、0540-2 年度三大表（资产负债表/利润表/现金流量表）、1707-1 CRP 报表、1838-2 差异报表、0427-1 预测差异报表、0700-1 追溯可视化、0700-2 员工净余额报表、2200-3 薪酬模拟对比报表。

## Goals

- 落地**财务核心报表渲染能力**这一唯一结果面：经 `IReportEngine` 注入 + `.xpt.xlsx` 模板 + ORM/SQL 数据集，使 ERP 报表"可定义、可渲染、可下载（xlsx/pdf/html）"。
- 交付**财务核心报表种子集**（最高优先级、口径最明确、Deferred 最具体）：
  - 资产负债表、利润表、现金流量表（年度三大表，承接 0540-2 Deferred）
  - AR/AP 账龄报表（承接 finance ar-ap-reconciliation §账龄）
  - 期末结账报告（承接 1000-3 Deferred）
- 落地统一渲染 BizModel（`renderHtml`/`download`），模板路径经 `StringHelper.isValidVPath` 校验防注入（参考 ReportDemoBizModel）。
- 解除"nop-report 未接线"硬阻塞，使后续各域报表/差异报表/看板计划可启动。

## Non-Goals

- 经营看板（dashboards.md 各域看板）的前端实现与 report-backed KPI 接线——独立后续计划（看板是 AMIS 页面定制面，非报表渲染引擎面）。
- 制造差异报表 / CRP 报表 / 预测差异报表 / 追溯可视化 / 员工净余额报表 / 薪酬模拟对比报表——各域后续计划（渲染能力就绪即可启动，本期只交付财务种子集证明能力）。
- 单据打印/套打（发票/凭证/订单套打，`print-template.md` 的 DETAIL/FORM/LIST）——相关但独立的打印能力面，归后续计划。
- 新建 `ErpSysDocumentTemplate` 实体——与平台 `NopReportDefinition` 重叠，违反平台默认路线（Decision 将裁决复用平台实体）。
- 定时报表/批量生成/报表结果缓存调度——归 nop-job/nop-batch 后继（0306-1/1600-1 范式）。
- 多账套合并报表——归多账套架构后继（1538-1/0540-2 Deferred）。

## Task Route

- Type: `architecture change`（接入平台渲染入口 + 模板/数据集规约）+ `implementation-only change`
- Owner Docs: `docs/design/finance/`（报表口径：ar-ap-reconciliation §账龄、posting §结账、period-close）、`docs/design/dashboards.md`（指标口径同源）、`docs/architecture/print-template.md`（打印机制边界）、`../nop-entropy/docs-for-ai/03-runbooks/generate-report.md`（XPT 语法 + 调用范式）、`../nop-entropy/docs-for-ai/03-modules/nop-report.md`（核心实体）
- Skill Selection Basis: 实现阶段涉及 BizModel + 平台 `IReportEngine` 注入 + XPT 模板 + 数据集定义 → `nop-backend-dev` 匹配（实体服务/自定义动作/跨平台调用/产品化自检）；前端/看板定制不在本期。报表口径以 finance owner docs 为业务真相。

## Infrastructure And Config Prereqs

- 平台 `nop-report-service`/`nop-report-web` 已接入（无需新增依赖）。
- 报表模板 VFS 路径：`/nop/main/report/fin/{name}`（如 `/nop/main/report/fin/balance-sheet.xpt.xlsx`），存放于 `module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/`（render BizModel 在 erp-fin-service；菜单接入在 erp-fin-web）。
- 数据集：优先 ORM 数据集（`UseOrmDataSet`，复用既有 `IErpFin*Biz`/`IOrmTemplate`）或原生 SQL（`UseJdbcDataSet`），口径数据源已存在（`ErpFinGlBalance`/`ErpFinArApItem`/`ErpFinVoucher`/期末结账产物）。
- 无外部服务依赖。

## Execution Plan

### Phase 1 - 模板存储与渲染入口 Decision + 渲染 BizModel 骨架

Status: completed
Targets: `module-finance/erp-fin-service/.../ErpFinReportBizModel.java`、`module-finance/*/resources/_vfs/nop/main/report/fin/`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: 报表模板存储机制
  - 候选：(a) VFS `.xpt.xlsx` 文件（开发者随仓维护，参考 runbook 主路线）；(b) 数据库 `NopReportDefinition` CRUD 维护（运行时管理）。
  - 倾向 (a) 为主、(b) 为运行时增强：种子报表随仓 `.xpt.xlsx` 可版本化、可审计；运行时定义经平台 CRUD。
  - 替代方案：(b) 全数据库维护（运行时灵活但种子不可版本化/不可审计）。
  - 残留风险：VFS 种子模板与运行时 DB 定义 `NopReportDefinition` 的共存/优先级策略本期未最终裁定（运行时 CRUD 已由平台提供，低风险；执行时若冲突，按 VFS 种子优先 + DB 覆盖记录于 owner doc）。
  - 实际裁决：种子报表采用 `.xpt.xml`（XML 序列化形式，与 `.xpt.xlsx` 等价、由 `XptConstants.FILE_TYPE_XPT_XML` 支持），便于 AI/开发者随仓版本化与审计，无需二进制编辑器；运行时 DB 定义共存策略按"VFS 种子优先 + DB 覆盖"记录于 `print-template.md`。Skill: none
- [x] Decision: 单据/打印模板实体
  - 候选：(a) 复用平台 `NopReportDefinition`（+ `billType` 维度），不新建 `ErpSysDocumentTemplate`；(b) 新建 `ErpSysDocumentTemplate`。
  - 裁决 (a)：避免与平台实体重叠 + 违反"不自建平行报表框架"默认路线。`print-template.md` 的 `ErpSysDocumentTemplate` 表述需修正为"复用 NopReportDefinition"。
  - 已落地：`docs/architecture/print-template.md` 全文重写为复用 `NopReportDefinition` + `IReportEngine`，移除 `ErpSysDocumentTemplate` 自建实体表述。Skill: none
- [x] Add: `ErpFinReportBizModel`（`@BizModel`）注入 `IReportEngine`，提供 `renderHtml(reportName, data)` @BizQuery 与 `download(reportName, renderType, data)` @BizQuery；模板名经 `StringHelper.isValidVPath` 校验后拼路径，防路径注入（参考 ReportDemoBizModel）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付渲染入口骨架与两个 Decision，解除 Phase 2 阻塞。

- [x] 两 Decision 在计划内记录选择/替代/残留风险；`print-template.md` 的 `ErpSysDocumentTemplate` 表述修正为复用平台实体
- [x] `ErpFinReportBizModel` 类型检查通过（`mvn compile -DskipTests -pl module-finance/erp-fin-service -am`），`IReportEngine` 可注入（IoC bean 存在）

### Phase 2 - 财务三大表种子报表

Status: completed
Targets: `_vfs/nop/main/report/fin/balance-sheet.xpt.xlsx`、`income-statement.xpt.xlsx`、`cash-flow-statement.xpt.xlsx`、对应数据集
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 资产负债表 `.xpt.xlsx` 模板 + ORM/SQL 数据集（数据源 `ErpFinGlBalance` 按科目层级汇总资产/负债/权益，口径对齐 finance owner doc 与 0540-2 年度结转产物）
- [x] Add: 利润表 `.xpt.xlsx` 模板 + 数据集（数据源损益类科目本期累计发生额，对齐结转损益业务类型）
- [x] Add: 现金流量表 `.xpt.xlsx` 模板 + 数据集（数据源 `ErpFinVoucher` 现金类科目 + 间接法补充，对齐 finance 现金流口径）
- [x] Proof: 三大表 `renderHtml` 返回非空文本、`download`（xlsx/pdf）生成有效文件；数据集对 seeded 期末数据返回正确汇总（单元/集成测试，`JunitAutoTestCase`）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 三大表可渲染可下载，数据集口径对齐 finance owner doc。

- [x] 三大表 `renderHtml`/`download(xlsx|pdf)` 测试全绿，输出非空且格式有效
- [x] 三大表数据集对 seeded 期末余额/凭证返回正确资产/负债/权益/损益/现金流汇总（断言关键科目行金额）

### Phase 3 - AR/AP 账龄与期末结账报告

Status: completed
Targets: `_vfs/nop/main/report/fin/ar-ap-aging.xpt.xlsx`、`period-close-report.xpt.xlsx`、数据集
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add: AR/AP 账龄报表 `.xpt.xlsx` 模板 + 数据集（数据源 `ErpFinArApItem.openAmount` + 账龄分桶 0-30/31-60/61-90/90+，按 partnerType=CUSTOMER|VENDOR 分组，口径对齐 ar-ap-reconciliation §账龄）
- [x] Add: 期末结账报告 `.xpt.xlsx` 模板 + 数据集（数据源期末结账产物：结转损益凭证、汇兑重估、坏账计提、存货成本核算、期间状态汇总，对齐 1000-3/0540-2 结账流程）
- [x] Proof: 两报表 `renderHtml`/`download` 测试全绿；账龄分桶金额与 `ErpFinArApItem` 明细合计一致；结账报告行覆盖各结账步骤产物
  - Skill: `nop-backend-dev`

Exit Criteria:

> 账龄 + 结账报告可渲染，口径可验证。

- [x] 两报表 `renderHtml`/`download` 测试全绿
- [x] 账龄报表分桶合计 = `ErpFinArApItem.openAmount` 按 bucket 聚合；结账报告覆盖结转/汇兑/坏账/存货成本/期间状态

### Phase 4 - 报表菜单接入与端到端验证

Status: completed
Targets: `module-finance/erp-fin-web/.../auth/erp-fin.action-auth.xml`（报表菜单分组）、`docs/architecture/print-template.md`、`docs/design/dashboards.md`
Skill: `nop-frontend-dev`（菜单/页面接入）+ `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] Add: finance 报表菜单分组接入 `erp-fin.action-auth.xml`（三大表/账龄/结账报告），报表页面经平台报表管理 + `ErpFinReportBizModel` 渲染入口
  - Skill: `nop-frontend-dev`
- [x] Proof: 端到端经 GraphQL `@BizQuery`（`renderHtml`/`download`）触发五张报表，全绿（`JunitAutoTestCase` 快照 CHECKING 模式）；路径注入防护测试（非法 reportName 被拒）
  - Skill: `nop-backend-dev`
- [x] Add: `print-template.md` 修正（复用 `NopReportDefinition`，移除 `ErpSysDocumentTemplate` 自建实体表述）；`dashboards.md` 标注看板实现归后续计划（渲染能力已就绪）
  - Skill: none

Exit Criteria:

> 报表菜单可达，五张报表端到端绿，文档对齐平台默认路线。

- [x] finance 报表菜单分组可达（action-auth 接入，类型检查通过）
- [x] 五张报表端到端 `@BizQuery` 测试全绿；路径注入防护测试绿（非法 reportName 抛 ErrorCode）
- [x] `print-template.md`/`dashboards.md` 实现落位与平台默认路线一致（无"自建平行报表框架/实体"反模式）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0cbe078bbffek8huLcBhPSioUZ`) — 独立子代理经实时仓库验证全部 baseline 声明为真（`nop-report-service`/`nop-report-web` 已接入 app-erp-all pom+action-auth、无 `.xpt` 模板、无 `ErpSysDocumentTemplate` 实体、`IReportEngine`/`NopReportDefinition` 平台存在、三处 finance Deferred 真实带"nop-report 接线时"触发、`ar-ap-reconciliation.md` §账龄分桶口径存在）。范围裁定合规（Rule 4/14：渲染能力 + 5 张财务种子报表为同一结果面，不可验证的空骨架拆分属过度碎片化），平台默认路线合规（复用 `NopReportDefinition` 拒绝 `ErpSysDocumentTemplate` 平行实体、注入 `IReportEngine` 而非 CRUD BizModel、`StringHelper.isValidVPath` 防注入、无私有 @Inject），类型/Skill/Decision/反松弛（零禁用词）均合规。已应用非阻塞精度修正：Decision 1 补充残留风险（VFS 种子 vs DB 运行时定义共存/优先级策略）、模板 VFS 位置消歧（定 erp-fin-service）。

## Closure Gates

> 完整仓库验证在结束处运行一次：`mvn clean install -DskipTests`（全 reactor 含报表模板资源）+ `mvn test -pl module-finance -am`（报表渲染测试）。

- [x] 范围内行为完成（渲染入口 + 五张财务种子报表可渲染可下载）
- [x] 相关文档对齐（`print-template.md` 复用平台实体修正、`dashboards.md` 看板归后续）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（154 reactor 模块）、`mvn test -pl module-finance/erp-fin-service` 报表测试全绿（170 tests, 0 failures, 0 errors，含新增 `TestErpFinReportRendering` 8 tests）
- [x] 无范围内项目降级为 deferred/follow-up（看板/各域报表/单据打印/定时批量/多账套合并明确为 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 经营看板（dashboards.md 各域 KPI/趋势/预警）前端实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 看板是 AMIS 页面定制面，非报表渲染引擎面；本期交付渲染能力 + 财务种子报表，看板 report-backed 接线归独立前端计划。
- Successor Required: `yes`（触发条件：看板前端定制启动时）

### 各域业务报表（制造差异/CRP/预测差异/追溯可视化/员工净余额/薪酬模拟对比）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 渲染能力就绪后各域按 owner doc 口径独立接线；本期仅交付财务种子集证明能力。
- Successor Required: `partial done`（制造部分 ✅ `done` 计划 `2026-07-06-0935-2`——`ErpMfgReportBizModel` + 3 张制造运营报表 CRP 负荷/生产差异/预测差异，模板根 `/nop/main/report/mfg/`；其余域 inventory/HR 报表触发条件仍为各域报表需求落地时）

### 单据打印/套打（发票/凭证/订单 DETAIL/FORM/LIST，print-template.md）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 打印是相关但独立的能力面（套打背景图 + 单据维度模板）；Decision 已裁决复用 `NopReportDefinition`，模板制作归后续。
- Successor Required: `yes`（触发条件：单据打印需求落地时）

### 定时报表/批量生成/结果缓存调度

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期按需 `renderHtml`/`download`；定时报送归 nop-job（0306-1）/ nop-batch（1600-1）后继。
- Successor Required: `yes`（触发条件：定时报送/批量生成需求时）

### 多账套合并报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖多账套架构（1538-1/0540-2 Deferred）；本期单账套。
- Successor Required: `yes`（触发条件：多账套上线时）

## Closure

Status Note: 计划已完成。全部 4 个 Phase 执行完毕，五张财务种子报表（资产负债表/利润表/现金流量表/AR-AP 账龄/期末结账报告）可经 `ErpFinReportBizModel` 渲染 html/xlsx/pdf，端到端测试全绿，文档对齐平台默认路线。

Closure Audit Evidence:

- 验证命令：
  - `mvn clean install -DskipTests` → BUILD SUCCESS（154 reactor 模块 / 18 域 + notify 子系统 + app-erp-all）
  - `mvn test -pl module-finance/erp-fin-service -Dsurefire.failIfNoSpecifiedTests=false` → Tests run: 170, Failures: 0, Errors: 0（含 `TestErpFinReportRendering` 8 tests：五报表 renderHtml 非空、download xlsx/pdf 文件有效、数据集口径断言、路径注入防护）
- 交付物：
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java`（`@BizModel("ErpFinReport")`，注入 `IReportEngine`，`renderHtml`/`download` + 5 个数据集构造方法 + `StringHelper.isValidVPath` 防注入）
  - `module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/{balance-sheet,income-statement,cash-flow-statement,ar-ap-aging,period-close-report}.xpt.xml`（5 张种子报表模板）
  - `module-finance/erp-fin-service/src/main/resources/_vfs/fonts/default.ttf`（PDF CJK 字体回退，PDFBox 渲染中文必需）
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/report/TestErpFinReportRendering.java`（端到端测试）
  - `module-finance/erp-fin-web/.../auth/erp-fin.action-auth.xml`（fin-report 菜单分组扩展 2 项：ar-ap-aging、period-close-report）
  - `module-finance/erp-fin-web/.../pages/report/*.page.yaml`（5 张报表 AMIS 页面，调 `ErpFinReport__renderHtml`/`__download`）
  - `docs/architecture/print-template.md`（全文重写为复用 `NopReportDefinition` + `IReportEngine`，移除 `ErpSysDocumentTemplate`）
  - `docs/design/dashboards.md`（标注看板归后续，渲染能力已就绪）
  - `module-finance/erp-fin-service/pom.xml`（新增 `nop-report-core` + `nop-report-pdf` compile 依赖）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`（新增 `ERR_REPORT_NAME_INVALID` / `ERR_REPORT_RENDER_TYPE_INVALID`）
- Auditor / Agent: 独立子代理 closure audit（新会话 `ses_0cb9c1498ffegOWQ42YkXsbq9U`），OVERALL: **close**。6 项验证全通过：(1) 计划一致性——无 `- [ ]`，4 Phase + Closure Gates 全 `[x]`；(2) 交付物全在（BizModel/5 模板/字体/测试/bean 注册/菜单/5 page.yaml/pom/2 ErrorCode）；(3) 文档对齐（`print-template.md` 复用 `NopReportDefinition` 拒绝 `ErpSysDocumentTemplate`，`dashboards.md` 看板归后续）；(4) 无反模式（用 `IReportEngine`、`@Inject` 非 private、`IServiceContext` 末参、`NopException`+`ErrorCode`）；(5) 验证命令实跑全绿（`mvn clean install -DskipTests` BUILD SUCCESS；`mvn test -pl module-finance/erp-fin-service` Tests run: 170 Failures: 0 Errors: 0，`TestErpFinReportRendering` 8 tests 全绿）；(6) 路线图 `core-business-roadmap.md` 已记 `✅ done 2026-07-06-0504-2`。

Follow-up:

- 经营看板前端实现（独立前端计划）
- 各域业务报表（渲染能力就绪后按域计划）
- 单据打印/套打（独立计划）
- 定时报表/批量（nop-job/nop-batch 后继）
