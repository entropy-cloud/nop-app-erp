# 2026-07-06-1247-3-domain-reports-frontend 域业务报表 AMIS 菜单/页面（制造/库存/HR）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-0935-2-manufacturing-operational-reports.md`「制造报表 AMIS 菜单/页面接入」（Successor Required: yes，触发条件=制造报表前端展示需求落地，镜像 0504-2 财务报表菜单接入范式）+ `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`「各域业务报表」前端面；owner doc `docs/architecture/print-template.md`
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（finance 报表 `fin-report` 菜单组 + report page.yaml 范式，已完成，本计划镜像之）、`2026-07-06-0935-2-manufacturing-operational-reports.md`（制造报表后端，已完成）、`2026-07-06-1247-1-remaining-domain-reports-backend.md`（inventory/HR 报表后端，本计划前端依赖其落地）
> Audit: required

## Current Baseline

- **finance 报表前端范式已验证**（0504-2，已审计）：`erp-fin.action-auth.xml` 含 `fin-report` 菜单组（`resourceType="SUBM"` + icon + orderNo + i18n）+ 5 子资源（balance-sheet/income-statement/cash-flow/ar-ap-aging/period-close-report），各指向 `/erp/fin/pages/report/<name>.page.yaml`。page.yaml 范式：`form`（`input-number`/`input-date` 参数）+ `button`（`actionType: ajax` 调 `/api/GenericApi` GraphQL `ErpFinReport__renderHtml`，结果注入 `html` 容器）+ `button-toolbar`（下载 XLSX/PDF：`actionType: download` + `responseType: blob` 调 `ErpFinReport__download{ fileName }`）。
- **制造报表后端已落地、前端为零**（0935-2，已审计）：`ErpMfgReportBizModel`（`@BizModel("ErpMfgReport")`，模板根 `/nop/main/report/mfg/`）+ 3 张报表（`crp-load-report`/`production-variance-report`/`forecast-variance-report`）`renderHtml`/`download` @BizQuery 已就绪。实时 grep 确认 `erp-mfg.action-auth.xml` **无** `mfg-report` 菜单组、**无** report page.yaml。
- **inventory/HR 报表后端为 `2026-07-06-1247-1` 范围**（前置依赖）：`ErpInvReportBizModel`（`inventory-trace-report`）+ `ErpHrReportBizModel`（`employee-net-balance`/`payroll-simulation-comparison`）落地后本计划方可接入对应前端。本计划对这些后端的依赖是**接口契约**（`renderHtml`/`download` @BizQuery 签名 + reportName 值），不依赖其实现细节。
- **域 ErrorCode 就绪**：mfg `ErpMfgErrors.ERR_REPORT_*`（0935-2 落地）；inv/hr `ErpXxxErrors.ERR_REPORT_*`（1247-1 落地）。前端仅消费 GraphQL，不直接接触 ErrorCode。
- **剩余差距**：3 域（mfg/inv/hr）各缺报表 action-auth 菜单组 + report page.yaml。

## Goals

- 交付 **3 域业务报表 action-auth 菜单组** + **6 个 report page.yaml**：
  - 制造（3）：CRP 负荷 / 生产差异 / 预测差异（消费 `ErpMfgReport`，后端 0935-2 已就绪）；
  - 库存（1）：库存追溯可视化（消费 `ErpInvReport`，后端 1247-1）；
  - HR（2）：员工净余额 / 薪酬模拟对比（消费 `ErpHrReport`，后端 1247-1）。
- 镜像 0504-2 finance 报表 page.yaml 范式（参数表单 + 渲染按钮 + 下载 XLSX/PDF toolbar + html 容器），reportName 与各域后端模板名严格一致。

## Non-Goals

- **finance 报表前端**（0504-2 已落地，本计划不重复）。
- **新报表后端**——本计划纯前端接入，不改 BizModel/.xpt.xml。如发现某报表后端 reportName/参数缺口，须另起后端计划（制造报表后端已就绪；inventory/HR 后端由 1247-1 提供）。
- **单据打印/套打**（0504-2 Deferred，`print-template.md`）——相关但独立能力面。
- **定时报表/批量生成**（0504-2 optimization candidate）。
- **报表运行时浏览器视觉回归**——归 Playwright successor（同 `2026-07-06-1247-2` Deferred）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/print-template.md`（报表模板存储/渲染/复用 `NopReportDefinition` 决策）；各域报表口径 owner docs（`manufacturing/crp.md`/`inventory/trace-chain.md`/`finance/expense-claim.md`/`human-resource/payroll-simulation.md`）仅用于确认 page 参数对齐口径
- Skill Selection Basis: 任务为 AMIS page.yaml 定制 + action-auth 菜单接入 + GraphQL 消费 `ErpXxxReport__renderHtml`/`download`，匹配 `nop-frontend-dev`（page.yaml 定制 / bounded-merge / AMIS form+button+download 组件）。后端 API 已就绪（mfg）或由前置计划提供（inv/hr），本计划不改后端。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。报表渲染引擎（`nop-report` + `IReportEngine`）已由 0504-2 接线。**前置依赖**：inventory/HR 报表前端（Phase 3-5）须在 `2026-07-06-1247-1` 落地后方可验证；制造报表前端（Phase 1-2）无前置依赖（后端 0935-2 已就绪）。无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 制造报表菜单组 + CRP 负荷报表页面（establish frontend pattern）

Status: completed
Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/auth/erp-mfg.action-auth.xml`；`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/crp-load-report.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（制造报表后端 0935-2 已就绪）

- [x] **Decision: 制造报表菜单组落位**——在 `erp-mfg.action-auth.xml` 新增 `mfg-report` 菜单组（displayName「制造报表」/ icon / orderNo / `resourceType="SUBM"`）+ 各报表子资源，镜像 finance `fin-report` 范式。
  - 理由：0504-2 finance `fin-report` 已验证 action-auth 报表菜单组范式；制造域同构接入，reportName 与 0935-2 后端模板名（`crp-load-report` 等）严格一致。
  - 替代方案：复用 finance 菜单组跨域挂载（拒绝：域边界错位 + URL 前缀混乱）。
  - Skill: `nop-frontend-dev`
- [x] **Add: `mfg-report` 菜单组 + `crp-load-report` 子资源**——action-auth 接入，URL 指向 `/erp/mfg/pages/report/crp-load-report.page.yaml`。
  - Skill: `nop-frontend-dev`
- [x] **Add: `crp-load-report.page.yaml`**——镜像 finance report page.yaml：参数表单（workcenterId/startDate/endDate，对齐 `buildCrpLoadDataset` 参数）+ 渲染按钮（调 `ErpMfgReport__renderHtml(reportName="crp-load-report")`）+ 下载 XLSX/PDF toolbar（调 `ErpMfgReport__download`）+ html 容器。
  - Skill: `nop-frontend-dev`
- [x] **Proof: CRP 报表页面静态验证**——page.yaml 可被 AMIS 解析；reportName 与 0935-2 后端模板一致；action-auth well-formed；构建通过。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 制造 `mfg-report` 菜单组 + CRP 报表页面落地；reportName 与后端一致；action-auth well-formed；构建通过

### Phase 2 - 制造生产差异 + 预测差异报表页面

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../pages/report/production-variance-report.page.yaml`；`.../forecast-variance-report.page.yaml`；`erp-mfg.action-auth.xml`（补子资源）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] **Add: `production-variance-report.page.yaml`**——参数表单（工单/差异类型/区间）+ 渲染/下载（`reportName="production-variance-report"`）+ html 容器；action-auth 补子资源。
  - Skill: `nop-frontend-dev`
- [x] **Add: `forecast-variance-report.page.yaml`**——参数表单（物料/区间）+ 渲染/下载（`reportName="forecast-variance-report"`）+ html 容器；action-auth 补子资源。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 两报表页面静态验证**——page.yaml 可解析；reportName 与后端一致；action-auth well-formed。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 制造生产差异 + 预测差异报表页面落地；reportName 与后端一致；构建通过

### Phase 3 - 库存报表菜单组 + 库存追溯可视化报表页面

Status: completed
Targets: `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/auth/erp-inv.action-auth.xml`；`module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/report/inventory-trace-report.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: **`2026-07-06-1247-1` Phase 1 落地**（`ErpInvReport` 后端 + `inventory-trace-report` 模板）

- [x] **Add: `inv-report` 菜单组 + `inventory-trace-report` 子资源**——action-auth 接入，URL 指向 `/erp/inv/pages/report/inventory-trace-report.page.yaml`，镜像 finance 范式。
  - Skill: `nop-frontend-dev`
- [x] **Add: `inventory-trace-report.page.yaml`**——参数表单（batchId/materialId/warehouseId，对齐 `buildInventoryTraceDataset`）+ 渲染/下载（`ErpInvReport__renderHtml`/`download`，`reportName="inventory-trace-report"`）+ html 容器。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 库存报表页面静态验证**——page.yaml 可解析；reportName 与 1247-1 后端一致；action-auth well-formed；构建通过。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 库存 `inv-report` 菜单组 + 追溯报表页面落地；reportName 与后端一致；构建通过

### Phase 4 - HR 报表菜单组 + 员工净余额报表页面

Status: completed
Targets: `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/auth/erp-hr.action-auth.xml`；`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/employee-net-balance.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: **`2026-07-06-1247-1` Phase 2 落地**（`ErpHrReport` 后端 + `employee-net-balance` 模板）

- [x] **Add: `hr-report` 菜单组 + `employee-net-balance` 子资源**——action-auth 接入，URL 指向 `/erp/hr/pages/report/employee-net-balance.page.yaml`。
  - Skill: `nop-frontend-dev`
- [x] **Add: `employee-net-balance.page.yaml`**——参数表单（部门/员工/区间）+ 渲染/下载（`ErpHrReport__renderHtml`/`download`，`reportName="employee-net-balance"`）+ html 容器。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 员工净余额报表页面静态验证**——page.yaml 可解析；reportName 与后端一致；action-auth well-formed；构建通过。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] HR `hr-report` 菜单组 + 员工净余额报表页面落地；reportName 与后端一致；构建通过

### Phase 5 - 薪酬模拟对比报表页面

Status: completed
Targets: `module-hr/erp-hr-web/.../pages/report/payroll-simulation-comparison.page.yaml`；`erp-hr.action-auth.xml`（补子资源）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: **`2026-07-06-1247-1` Phase 3 落地**（`payroll-simulation-comparison` 模板）

- [x] **Add: `payroll-simulation-comparison.page.yaml`**——参数表单（simulationId/部门）+ 渲染/下载（`ErpHrReport__renderHtml`/`download`，`reportName="payroll-simulation-comparison"`）+ html 容器；action-auth 补子资源。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 薪酬模拟对比报表页面静态验证**——page.yaml 可解析；reportName 与后端一致；action-auth well-formed。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 薪酬模拟对比报表页面落地；reportName 与后端一致；构建通过

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0ca3adac9ffeis0IEuKgytSD1c`) because 全部 baseline/Rule 合规，但 BLOCKING：Phase 4/5 Targets 误用 `module-human-resource/erp-hr-web`（实时为 `module-hr/erp-hr-web`，root pom 注册 `module-hr`），违反 Rule 1。附带非阻塞：reportName-match 门控须给具体命令。
- Independent draft review iteration 2: accept (`ses_0ca34cad2affeqZ2hgZqwfJGavP`) after 修正 HR 路径（Phase 4/5 Targets + 依赖 1247-1 三处均改 `module-hr`，grep 零残留）+ reportName-match 门控补具体可复现命令——独立核实 `module-hr/erp-hr-web/.../erp-hr.action-auth.xml` 存在、Phase 前置门控正确（mfg Phase 1-2 无后端前置；inv/hr Phase 3-5 门控 1247-1 各 Phase）、Skill/类型/反松弛全合规，无新阻塞。可转 active。

## Closure Gates

> 完整仓库验证在结束处运行一次。前端结果表面验证门控对齐 0504-2 已验证的 page.yaml+action-auth 范式（构建通过 + well-formed + reportName 与后端模板一致 + 菜单可达）。inventory/HR 前端（Phase 3-5）的端到端可渲染性依赖 1247-1 后端落地——若 1247-1 未完成，本计划对应 Phase 保持阻塞不得勾选，但制造前端（Phase 1-2）可独立关闭仅当其范围内完成。

- [x] 范围内行为完成（mfg 3 + inv 1 + hr 2 报表菜单组与页面落地）
- [x] 相关文档对齐（`print-template.md` 各域报表菜单接入说明 + 0935-2/0504-2/1247-1 Deferred 标记承接 + roadmap done 条目 + 当日日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `xmllint --noout` 各 action-auth.xml + 各 page.yaml YAML 可解析（`python3 -c "import yaml,sys;[yaml.safe_load(open(f)) for f in sys.argv[1:]]" <page.yaml files>`）+ reportName 与后端模板名逐一核对一致（`rg -o 'reportName: "[^"]*"' 各 page.yaml` 输出集合 ⊆ `ls module-*/erp-*-service/src/main/resources/_vfs/nop/main/report/{mfg,inv,hr}/*.xpt.xml` 去扩展名集合）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 域报表运行时浏览器视觉回归（Playwright）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 项目 page.yaml+action-auth 范式经 0504-2 已验证可构建可解析；本计划静态验证（构建+well-formed+reportName 一致）对齐既有前端结果面门控。
- Successor Required: `yes`（触发条件：Playwright 报表 e2e 套件建立时）

### 其余域报表前端（资产/项目/维护/质量/主数据/CRM/客服）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 对应域报表后端 successor 落地后再做前端；本计划聚焦后端已就绪/即将就绪的 mfg/inv/hr 三域。
- Successor Required: `yes`（触发条件：对应域报表后端落地时）

### 单据打印/套打 / 定时报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0504-2 已裁定为独立能力面；本计划仅交付各域报表的列表/渲染/下载页面。
- Successor Required: `yes`（触发条件：单据打印 / 定时批量需求落地时）

## Closure

Status Note: 3 域业务报表前端（mfg/inv/hr）action-auth 菜单组 + 6 report page.yaml 全部落地，镜像 0504-2 finance report 范式。reportName 与各域后端 `.xpt.xml` 模板名逐一核对一致（6==6），page 参数严格对齐后端 `buildXxxDataset` 真实签名（inv `batchNo` 非 batchId；hr employee-net-balance 后端零参故页面无输入字段）。`mvn clean install -DskipTests` 154 模块全绿 + 全 workspace `mvn test` 0 failures/0 errors + 3 action-auth well-formed + 6 page.yaml YAML 可解析。解除 0935-2 Non-Goal「制造报表 AMIS 菜单/页面」+ 1247-1 Non-Goal「inventory/HR 报表 AMIS 菜单/页面」+ 0504-2 Deferred「各域业务报表前端面」。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0c99e9794ffezVpPg61wW6X6Bn`（general，新会话，未参与执行）
- Evidence: 9 项核验全 PASS——(1) 6 page.yaml + 3 菜单组（mfg-report 3 子/inv-report 1 子/hr-report 2 子）存在且 url 正确；(2) 3 action-auth `xmllint --noout` well-formed（src + target/classes）；(3) 6 page.yaml `yaml.safe_load` ALL YAML OK；(4) reportName 集合 == 后端模板名集合（6==6 精确匹配）；(5) page 参数逐一核验后端 `prepareDataset` switch（mfg:172/176/180、inv:162、hr:169/172），无 stray/missing 参数；(6) `mvn clean install -DskipTests` BUILD SUCCESS 全 reactor（1:24）；(7) GraphQL 目标 `ErpMfgReport__*`/`ErpInvReport__*`/`ErpHrReport__*` 与 `@BizModel` 注解一致；(8) 计划文本一致（5 Phase Status:completed + 全 `[x]`，Phase 体零残留 `[ ]`）；(9) `print-template.md` mfg/inv/hr 三段均标注前端已接入 + roadmap `2026-07-06-1247-3` done 条目。反模式自检 PASS（6 page.yaml 在保留层非 `_gen/`；mfg/inv 用 `x:extends`、hr 直接定义 `<auth>` 与既有结构一致）。VERDICT: PASS，无缺陷。

Follow-up:

- 报表运行时浏览器视觉回归（Playwright successor，触发条件=报表 e2e 套件建立时）——非阻塞，见「Deferred But Adjudicated」。
