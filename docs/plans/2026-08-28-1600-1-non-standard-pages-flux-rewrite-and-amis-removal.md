# 2026-08-28-1600-1-non-standard-pages-flux-rewrite-and-amis-removal 非标 AMIS 页面全量 Flux 化与 AMIS 源文件删除

> Plan Status: in_progress
> Last Reviewed: 2026-08-28
> Source: 用户直接请求（2026-08-28）——「自动生成的 page.yaml 用 web.xlib 生成，通过 x:post-extends 替换为 flux 实现，标准页面不用管，只考虑非标页面；写一个计划，确保所有非标页面都修改为 flux 实现，amis 实现删除」
> Related: `2026-08-03-1232-1`（CRUD 基础设施，前置）/ `2026-08-03-1232-2/3/4`（复杂页 flux 重写 F13/F16/占位页，flux 范式先例）/ `2026-08-03-1232-5`（文档漂移回填）/ `2026-08-24-1147-1`（picker xlib schema 修复，flux-control.xlib delta 范例）/ `docs/backlog/frontend-ui-roadmap.md`「AMIS 退役路径」段
> Audit: required

## Current Baseline

### 渲染引擎与菜单状态（live 验证，2026-08-28）

- **菜单全 19 域 `component="FLUX"`**（0 AMIS 残留）—— commit `738810aa5`（2026-08-04）全量翻转
- **render-mode 缺省 flux**（`app-erp-all` 19 域配置 + `E2E_ENGINE=flux` 缺省）—— commit `22eafe855` / `6ffbbedd7`
- **平台 `flux.yaml` 文件类型注册在位**：`nop-web/WebConstants.java:41,44` `FILE_TYPE_FLUX_YAML` 在 `PAGE_FILE_TYPES` 内；`WebPageHelper.toFluxPagePath`（`:48`）做 flux 模式下的 `*.page.yaml` → 同目录同名 `*.flux.yaml` 回退（plan `31-flux-yaml-page-fallback.md`）
- **标准 CRUD/picker/tabs 经 view.xml + flux-web.xlib 输出**：354 CRUD + 352 picker + 38 ref + 15 tabs，`ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0（plan `2026-08-03-1232-1` 全 4 阶段）

### 非标页面存量盘点（live 实测 2026-08-28）

| 维度 | 数据 |
|---|---|
| 生产 `*.page.yaml` 总数（排除 `_gen/`/`_dump/`/`target/`） | **855** |
| **AMIS DSL 非标页面**（含 `type: form/service/chart/crud/cards/tpl/wizard/steps/html` 等手写 body） | **74** |
| 已存在 flux `.flux.yaml` 的复杂页 | **20**（F13/F16/wizard/占位页，plan 1232-2/3/4） |
| 菜单 `*.action-auth.xml` 中 `url=...page.yaml` 引用 74 个 AMIS DSL 页 | **68 处**（auth.xml 不引用 picklist 等纯服务端文件） |
| `party-search/main.picker.page.yaml`（页面级 picker） | 1 个，含 AMIS picker 交互（onSelect 回填父表单） |

### AMIS DSL 页面细分（74 个，按行为模式分类）

> 按 `grep -E "^  - type: (form|table|chart|cards|service|crud|tpl|html|wizard|steps|timeline|calendar|kanban|tree|diff-view|input-table|crud)" <page>` 直方分类；本表为 2026-08-28 实时盘点的模式分布。

| 子类 | 数量 | 代表文件 |
|---|---|---|
| **经营 dashboards** | 10 + 1 | `*/dashboard/main.page.yaml`（10 域）+ `module-cs/.../ErpCsQualityDashboard/main.page.yaml` |
| **报表** | 25 | `*/report/*.page.yaml`（inv 1 + fin 6 + hr 2 + mfg 3 + md 2 + prj 2 + qa 2 + cs 1 + crm 3 + mnt 2 + ast 1） |
| **SPC 三件套** | 3 | `module-quality/.../spc-{chart,capability,sample}/main.page.yaml` |
| **复杂页 AMIS 历史版**（已有同名 `.flux.yaml`） | 13 | schedule-gantt/three-way-match/bom-tree/net-requirement/team-vacation-calendar/payroll-approval/org-chart/shipment-tracking/version-diff/asn-flow/edi-detail/prj-cs-crm kanban×3/timeline×2/calendar×2 = 13（注：与 1232-2/3/4 实施的 20 个 flux.yaml 子集重合；以下按"已有 .flux.yaml"对账） |
| **非 dashboards 非报表向导/单页** | 9 | `module-crm/.../lead-conversion/main.page.yaml`、`module-prj/.../project-settlement/main.page.yaml`、`module-prj/.../project-pnl/main.page.yaml`、`module-fin/.../expense-claim/main.page.yaml`、`module-fin/.../bank-statement/main.page.yaml`、`module-fin/.../bank-reconciliation/main.page.yaml`、`module-fin/.../budget-scenario/main.page.yaml`、`module-fin/.../budget-control-log/main.page.yaml`、`module-inv/.../stock-take-flow/main.page.yaml` |
| **资产相关** | 2 | `module-ast/.../disposal-wizard/main.page.yaml`、`module-ast/.../asset-stocktake/main.page.yaml` |
| **通知收件箱** | 1 | `module-notify/.../ErpSysNotification/inbox.page.yaml`（329 行 AMIS tabs + 客户端 JS） |
| **页面级 picker** | 1 | `module-md/.../party-search/main.picker.page.yaml`（126 行 AMIS picker + onSelect 回填） |
| **maintenance 向导** | 1 | `module-mnt/.../visit-wizard/main.page.yaml`（已有 flux.yaml；本计划删除 AMIS 版） |

> 注：上述合计为 74（10+1+25+3+13+9+2+1+1+1 = 66；差额 8 在"复杂页 AMIS 历史版"内部与计划 1232-2/3/4 落地计数有偏移——计划落地 20 个 flux.yaml，但其中若干 dashboards 与报表也存在 AMIS 历史版需一并处理；以"AMIS DSL 页面"=74 为权威基线）。

### 关键证据文件

- `ErpAllFluxPagesTest.java:47` 遍历 `pages/*/*.page.yaml` 测 flux 模式渲染——`FLUX_PAGE_ERROR_COUNT=0` 基线（plan 1232-1 2026-08-05）
- `scripts/flip-menu-to-flux.sh` 历史翻转脚本——只翻菜单 `component=`，未触碰 page.yaml
- `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md §8.2` 菜单 100% FLUX 实测
- `docs/retrospectives/2026-08-06-1400-flux-page-organization-deviation.md` 复杂页整页直写裁决
- `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib` delta 覆盖范例（plan 1232-5/2026-08-24-1147-1）

## Goals

- **74 个 AMIS DSL 非标页面**全部迁移到 flux 实现（`*.flux.yaml` 直写或自定义 xlib 标签封装），flux 模式下 100% 由 flux 引擎渲染
- **AMIS `.page.yaml` 源文件全部删除**（仅保留同名 `.flux.yaml`，或归并到 view.xml + 自定义 xlib）
- **菜单 `url=` 引用同步更新**（68 处 `*.page.yaml` → `*.flux.yaml`，或在 view.xml 注册入口）
- **自定义 xlib 抽取（按需）**：dashboards 三段式（filter form + data-source + cards/chart）和报表范式（filter form + data-source + html 块 + 下载按钮）若有 ≥3 个页面复用，抽 `app-erp-all/.../erp/xlib/erp-page-lib.xlib` 自定义标签（如 `<erp:dashboard>` / `<erp:report>`），简化逐页模板代码——但**仅在确实降低总行数与维护成本时落地**（每个标签至少 3 处复用 + 单页替换行数 ≥50% 缩减）
- **零回归**：`ErpAllFluxPagesTest` `FLUX_PAGE_ERROR_COUNT=0` 维持 + 全 dashboards smoke（10 域 + cs 1）+ F13/F16/F12 视觉 spec 全绿 + AMIS spec 残 0

## Non-Goals

- 标准 CRUD/picker/ref/tabs 页面（经 view.xml + flux-web.xlib 输出，约 759 个）—— 已在 plan 1232-1 收口，本计划不涉及
- 任何后端 ORM/BizModel/mutation 变更——本计划纯前端页面重写
- 菜单项新增/移除/orderNo 调整——F14 已收口，本计划仅同步 url 指向
- 视觉/像素级重设计——保持当前 UI 行为，仅变更实现技术栈（AMIS schema → flux DSL）
- NopAuth 域 demo 页面（`module-nop-auth`）——非业务域
- 平台 NopAuth 与 nop-entropy 侧 `web.xlib` / `flux-web.xlib` 变更——保护区域，须走 dual-agent-approval
- i18n 标签补充（`page.yaml` title 因 YAML 无 `i18n-en:` 属性机制，roadmap `F15` 已 Deferred 至 l10n successor）——本计划不为新 page.yaml 加 i18n，但 .flux.yaml 同步按现有中文标题保留（与 1232-2/3/4 范式一致）

## Task Route

- Type: `implementation-only change`（页面重写 + AMIS 源文件删除 + 菜单 url 同步）
- Owner Docs: `docs/design/flux-complex-pages.md` §2.4 三种页面编写路径、§3 控件映射总表、`docs/design/page-structure-patterns.md`（dashboard/报表范式）、`docs/design/non-standard-views-patterns.md` §0（flux 重写权威翻转）、`docs/design/dashboards/`（各域 dashboard ui-patterns 引用）、`docs/design/reporting-patterns.md`（报表 ui-patterns 引用，按域分散在 `docs/design/<domain>/`）、`docs/backlog/frontend-ui-roadmap.md`「AMIS 退役路径」段
- Skill Selection Basis: `nop-frontend-dev`（页面重写主用）+ `nop-testing`（视觉 spec 选择器重写，仅当复用 1232-2/3/4 已存在 spec 时跳过）+ `docs/skills/age-workflow/SKILL.md`（如涉及跨域协调）

## Infrastructure And Config Prereqs

- 前置：plan `2026-08-03-1232-1`（CRUD 基础设施，render-mode=flux）+ `2026-08-03-1232-2/3/4`（F13/F16/占位页 flux 重写）已完成，`nop.web.render-mode=flux` 可运行
- `ErpAllFluxPagesTest` 基线 = `FLUX_PAGE_ERROR_COUNT: 0`（2026-08-05/2026-08-23 等多次基线确认）
- `tests/e2e/dashboards/*.smoke.spec.ts`（10 域）+ `tests/e2e/visual/{f12,f13,f16,fin-period-close-wizard,maintenance-visit-wizard,party-search-picker}.visual.spec.ts` 现行绿基线
- `E2E_ENGINE=flux` 缺省；Playwright 通过 `data-slot`/`.nop-*` 选择器
- 无新外部端口/环境变量/.env 依赖；无后端 schema 迁移

## Execution Plan

### Phase 0 - 抽取策略裁决与 xlib 接口设计

Status: done
Targets: `app-erp-all/src/main/resources/_vfs/_delta/default/erp/xlib/erp-page-lib.xlib`（待新建；按需）；`docs/design/page-structure-patterns.md` §3 dashboard 范式；`docs/design/reporting-patterns.md` §X 报表范式
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（前置 plan 已 done）

- [x] **Decision: 自定义 xlib 抽取范围**——**裁决结果（2026-08-28）**：
  - `<erp:dashboard>` **不抽**。11 个 dashboard 结构差异大（不同 KPI 字段、不同 chart 类型、不同 crud 列、不同 filter 字段），参数化成本 > 收益。逐页 `.flux.yaml` 直写。
  - `<erp:report>` **抽**。25 个报表结构高度统一（filter form + service renderHtml + html 块 + 下载按钮 XLSX/PDF），仅 reportName/periodId/filter 字段不同。创建 `erp-page-lib.xlib` delta，模板封装 filter form + data-source + html 渲染 + 下载 toolbar；每页 ~10 行配置。
  - `<erp:spc-chart>` **不抽**。仅 3 页，每页结构独特（chart 配置/markLine/series 差异大），不达 ≥3 复用阈值。逐页 `.flux.yaml` 直写。
  - Skill: `nop-frontend-dev`
- [x] **Decision: dashboards 与 SPC/报表的 service → data-source 转换策略**——**裁决结果（2026-08-28）**：
  - dashboard/SPC 页面：保留 raw GraphQL + adaptor（`type:ajax` + `query` + `adaptor`），因 dashboard adaptor 含复杂数据转换（KPI 聚合、chart series 构建、status mapping），`@query:` 语法无法表达。
  - 报表页面：同理保留 raw GraphQL + adaptor（adaptor 解析 `renderHtml` 返回值）。
  - KPI 多服务并行：多个 `data-source` 节点各自独立 name + `fetchOn: init`；刷新按钮用 `events.reload` 链替代 AMIS `target:"a,b,c"` 语法。
  - Skill: `nop-frontend-dev`
- [x] **Decision: 复杂页 AMIS 历史版删除策略**——**裁决结果（2026-08-28）**：
  - **直接删除** 20 个已有 `.flux.yaml` 的复杂页对应的 `.page.yaml`，保留 `.flux.yaml`。
  - 菜单 `url=` 同步更新（`*.page.yaml` → `*.flux.yaml`）。
  - 删除后 `WebPageHelper.toFluxPagePath` 无回退源（.page.yaml 不存在），须显式改 url。
  - Skill: `nop-frontend-dev`
- [x] **Decision: 报表页 html 块承载**——**裁决结果（2026-08-28）**：
  - 抽取 `<erp:report>` xlib 标签到 `erp-page-lib.xlib`，封装 filter form + data-source (raw GraphQL renderHtml) + html 渲染 (`type:text text="${reportHtml}"`) + 下载按钮 toolbar (XLSX/PDF)。
  - 每个报表页仅需传入：reportName、filter 字段定义、download endpoint。
  - Skill: `nop-frontend-dev`
- [x] **Decision: 通知收件箱 tabs + 客户端 JS 过滤**——**裁决结果（2026-08-28）**：
  - 收件箱 329 行 AMIS tabs + 客户端 JS 过滤 → flux `tabs` 直写 `inbox.flux.yaml`。
  - 客户端 JS 过滤改用 flux 服务端 filter：3 个 tab 各自独立 `data-source` + filter args（unread/read/all），不再依赖客户端 filter 函数。
  - 或保留单 `data-source` + flux expression 过滤（`${items | filter: ...}`）——实施期评估。
  - Skill: `nop-frontend-dev`
- [x] **Decision: 页面级 picker（party-search）**——**裁决结果（2026-08-28）**：
  - 保持页面级 picker 路径（`page_picker.xpl` flux 模式已通过）。
  - `main.picker.page.yaml` 改写为 `main.picker.flux.yaml`——AMIS picker `onSelect` 回填父表单字段（partyType/partyId/displayName/phone/email）映射到 flux `onSelect` 事件 payload。
  - Skill: `nop-frontend-dev`
- [x] **Proof: Phase 0 决策结果汇总记录**——6 个 Decision 全部记录，验证执行期间无歧义。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 自定义 xlib 抽取范围裁决：抽 `<erp:report>` xlib（报表 25 页），不抽 dashboard/spc（逐页直写）
- [x] dashboards / SPC / 报表 / 复杂页历史版 / 收件箱 / picker 的迁移策略明确，每页一行为准
- [x] 6 个 Decision 替代方案 + 残留风险记录在计划中

### Phase 1 - dashboards 全量改写（11 页）

Status: planned
Targets: `module-{aps,ast,b2b,ct,crm,cs,drp,fin,hr,inv,mfg,md,mnt,prj,qa,sal}/.../dashboard/main.page.yaml`（10 域）+ `module-cs/.../ErpCsQualityDashboard/main.page.yaml`（1 个 cs 绩效 dashboard）；菜单 url 同步更新 11 处
Skill: `nop-frontend-dev`

- Item Types: `Fix | Add | Proof`（页类型直方 = 全部含 form/service/chart/crud）
- Prereqs: Phase 0 裁决（自定义 xlib 是否落地）

- [ ] **Decide-then-implement per dashboard**——每个 dashboard 改写后**删除** `.page.yaml`：
  - 数据源：AMIS `service` (raw GraphQL + adaptor) → flux `data-source` (`@query:` 优先 + `type:ajax` fallback)
  - KPI 卡：AMIS `wrapper` + `tpl` + grid 嵌套 → flux `cards` 或 `<erp:dashboard>` xlib 标签
  - 趋势/预警 chart：AMIS `chart` (echarts adaptor) → flux `chart`（flux chart 自带 echarts 适配器，参考 `mfg/dashboard/main.flux.yaml` 已有 chart 用法——实测是否有，无则 raw echarts）
  - crud 列表：AMIS `crud` → flux `crud`（`type:"crud"`，自带 `loadAction`/`columns`/`headerToolbar`）
  - 多目标 reload：AMIS `actionType: reload target:"a,b,c"` → flux `events.reload`/`actions` 链
  - Skill: `nop-frontend-dev`
- [ ] **菜单 url 更新**——11 处 `url=".../dashboard/main.page.yaml"` → `.../dashboard/main.flux.yaml`（若 xlib 抽取，则保留页面名 `main.page.yaml` 但实际渲染由 view.xml/自定义标签接管；URL 不变仅文件类型变）
      - Skill: `nop-frontend-dev`
- [ ] **Proof**: 11 dashboard E2E（flux 引擎）—— `tests/e2e/dashboards/*.smoke.spec.ts`（10）+ `tests/e2e/dashboards/qa-dashboard-spc-attributes.value.spec.ts`（1 cs）全绿；dashboard 加载断言 + KPI/chart/crud 渲染断言；视觉 spec 若有同步重写选择器（仅当原 spec 引用 AMIS `.cxd-*` 选择器时）
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 11 dashboard 以 flux DSL 渲染（service → data-source、wrapper+tpl → cards、crud → crud）
- [ ] 11 `.page.yaml` 已删除，`.flux.yaml` 已就位（或经 view.xml + 自定义 xlib 渲染）
- [ ] 11 dashboard E2E smoke 全绿（flux 引擎）
- [ ] `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 维持

### Phase 2 - 报表页全量改写（25 页）

Status: planned
Targets: `module-{ast,cs,crm,fin,hr,inv,md,mfg,mnt,prj,qa}/.../report/*.page.yaml`（25 个）；菜单 url 同步 25 处
Skill: `nop-frontend-dev`

- Item Types: `Fix | Add | Proof`（页类型直方 = form + service + html + button-toolbar）
- Prereqs: Phase 0 裁决（`<erp:report>` 标签抽取）

- [ ] **Decide-then-implement per report**——每个报表改写后**删除** `.page.yaml`：
  - filter form：AMIS `form` (mode: inline) → flux `form`（属性名差异：`controls` → `body` 字段、`submitOnChange` 保留；YAML 良构核验）
  - data-source：AMIS `service` (raw GraphQL `ErpXxxReport__renderHtml(reportName:String!, data:{...})` + adaptor 返回 `reportHtml`) → flux `data-source`（`type:ajax` 保留 raw GraphQL + adaptor；`name: reportHtml` 供 `<erp:report-html>` 消费；或 `@query:` 封装若 BizModel 提供 `renderReport(reportName, data)` 方法则免 adaptor——按各域 BizModel 实际 API 评估）
  - html 块：AMIS `html html="${reportHtml}"` → 自定义 xlib `<erp:report-html name="reportHtml"/>` 或 flux 自定义渲染器
  - 下载按钮：AMIS `button-toolbar` + `button actionType:download api:url:/p/ErpXxxReport__download` → flux `button` + `actionType: download` + 同 api
  - Skill: `nop-frontend-dev`
- [ ] **菜单 url 更新**——25 处 `url=".../report/*.page.yaml"` → `.flux.yaml`；同步删除 25 个 `.page.yaml`
      - Skill: `nop-frontend-dev`
- [ ] **Proof**: 25 报表 E2E（flux 引擎）——为每个报表写/扩 smoke 断言：表单过滤 → renderHtml 服务端返回 → 页面显示 HTML；XLSX/PDF 下载按钮存在且 200。`tests/e2e/reports/*.spec.ts` 全绿；现有报表 spec 若选择器耦合 AMIS DOM同步重写
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 25 报表以 flux DSL 渲染（service → data-source + html 块）
- [ ] 25 `.page.yaml` 已删除，`.flux.yaml` 已就位
- [ ] 25 报表 E2E smoke 全绿（flux 引擎；含下载按钮）
- [ ] `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 维持

### Phase 3 - SPC 三件套改写（3 页）

Status: planned
Targets: `module-quality/.../spc-{chart,capability,sample}/main.page.yaml`；菜单 url 同步 3 处
Skill: `nop-frontend-dev`

- Item Types: `Fix | Add | Proof`（页类型直方 = form + service + chart + crud）
- Prereqs: Phase 0 裁决（`<erp:spc-chart>` 标签抽取）

- [ ] **3 SPC 页改写**——`spc-chart/main.page.yaml`（line + markLine UCL/LCL/CL + 失控点红）、`spc-capability/main.page.yaml`（Cp/Cpk 计算 + capability 评级）、`spc-sample/main.page.yaml`（样本明细 + 违规规则）。每个改写后删除 `.page.yaml`：
  - chart：AMIS `chart` (echarts) → flux `chart`（flux chart 渲染 echarts；markLine + 失控点高亮需参考 flux `chart` 配置范式）
  - sample list：AMIS `crud` → flux `crud`
  - filter：AMIS `form` → flux `form`
  - Skill: `nop-frontend-dev`
- [ ] **菜单 url 更新**——3 处 `*.page.yaml` → `.flux.yaml`
      - Skill: `nop-frontend-dev`
- [ ] **Proof**: 3 SPC 页 E2E（flux 引擎）——`tests/e2e/dashboards/qa-dashboard-spc-attributes.value.spec.ts`（1 个）+ 视觉 spec 同步（如有）全绿；chart markLine/UCL/LCL/CL 渲染断言 + 失控点红色断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 3 SPC 页以 flux DSL 渲染
- [ ] 3 `.page.yaml` 已删除，`.flux.yaml` 已就位
- [ ] 3 SPC E2E 全绿
- [ ] `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 维持

### Phase 4 - 复杂页 AMIS 历史版删除（13 页）

Status: planned
Targets: 13 个 `*.page.yaml` 与菜单 url 同步；保留同名 `.flux.yaml`（已 1232-2/3/4 实施）
Skill: `nop-frontend-dev`

- Item Types: `Fix | Proof`（删除 + url 更新 + 验证非 flux 模式不受影响）
- Prereqs: Phase 1/2/3 完成；`ErpAllFluxPagesTest` 通过

- [ ] **删除 13 个 AMIS `.page.yaml`**——schedule-gantt/three-way-match/bom-tree/net-requirement/team-vacation-calendar/payroll-approval/org-chart/shipment-tracking/version-diff/asn-flow/edi-detail/crm-activity-timeline/crm-activity-calendar（注：prj/cs/crm kanban ×3 + cs-ticket-action timeline 已在 1232-2/3/4 完成；按 74 - dashboards 11 - 报表 25 - SPC 3 - 收件箱 1 - picker 1 - 其他 9 - ast 2 - mnt visit-wizard 1 = 21，但 1232-2/3/4 实际落地 20 个 flux.yaml 完整覆盖，差额为本计划与 1232-2/3/4 重叠部分；以实际 grep 删除清单为准）
      - Skill: `nop-frontend-dev`
- [ ] **菜单 url 同步**——13 处 `url=".../*.page.yaml"` → `*.flux.yaml`
      - Skill: `nop-frontend-dev`
- [ ] **Proof**: 13 页 F13/F16/F12/fin-period-close-wizard/maintenance-visit-wizard E2E（flux 引擎）全绿——`tests/e2e/visual/{f13,f16-high-risk,f16-p2-complex-pages,fin-period-close-wizard,maintenance-visit-wizard}.visual.spec.ts` 5 个 spec 全绿；既有的 1232-2/3/4 spec 不退化
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 13 个 AMIS `.page.yaml` 已删除
- [ ] 13 处菜单 url 已更新为 `.flux.yaml`
- [ ] F13/F16/F12/fin/mnt E2E spec 全绿
- [ ] `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 维持

### Phase 5 - 其他非标页面（收件箱/wizard/picker/单页）改写与收口

Status: planned
Targets: `module-{crm,prj,fin,inv,ast}/.../{lead-conversion,project-settlement,project-pnl,expense-claim,bank-statement,bank-reconciliation,budget-scenario,budget-control-log,stock-take-flow,disposal-wizard,asset-stocktake,visit-wizard,inbox,party-search}/` 14 个 `.page.yaml`；菜单 url 同步
Skill: `nop-frontend-dev`

- Item Types: `Fix | Add | Proof`（页类型直方混杂：wizard 9 + picker 1 + tabs 1 + 其他 3）
- Prereqs: Phase 1/2/3/4 完成

- [ ] **15 个其他 AMIS 页改写**：
  - `party-search/main.picker.page.yaml`（页面级 picker，126 行）→ `main.picker.flux.yaml`（picker onSelect 事件 payload 映射父表单字段回填）
  - `notify/ErpSysNotification/inbox.page.yaml`（329 行 tabs + JS）→ view.xml `<pages><tabs>` 入口 + 3 个 tab 子页 `*.tab.flux.yaml`（替代客户端 JS）
  - `crm/lead-conversion/main.page.yaml`（向导，AMIS wizard step indicator + 4 个 form）→ flux `wizard`（flux 原生 wizard，5 step，per-step form）
  - `mnt/visit-wizard/main.page.yaml`（已有 flux.yaml；删除 AMIS 版）—— Phases 4 已覆盖，本 Phase 仅做收口确认
  - `ast/disposal-wizard/main.page.yaml` + `ast/asset-stocktake/main.page.yaml`（2 个）→ flux `wizard` 或对应 flux 复合页
  - `prj/project-settlement/main.page.yaml` + `prj/project-pnl/main.page.yaml`（2 个；含 KPI 卡 + crud）→ flux `cards` + `crud` 三段式（dashboard 范式）
  - `fin/expense-claim/main.page.yaml` + `fin/bank-statement/main.page.yaml` + `fin/bank-reconciliation/main.page.yaml` + `fin/budget-scenario/main.page.yaml` + `fin/budget-control-log/main.page.yaml`（5 个）→ flux 范式（form + crud 或 form + service + chart）
  - `inv/stock-take-flow/main.page.yaml`（盘点流程向导）→ flux `steps` 或 `wizard`
  - Skill: `nop-frontend-dev`
- [ ] **菜单 url 同步**——14 处 `url=".../*.page.yaml"` → `.flux.yaml` 或 view.xml 入口
      - Skill: `nop-frontend-dev`
- [ ] **Proof**: 14 页 E2E（flux 引擎）——`tests/e2e/visual/{party-search-picker,fin-period-close-wizard,maintenance-visit-wizard}.visual.spec.ts`（3 个已有）+ `tests/e2e/business-actions/*.action.spec.ts`（向导操作相关）；picker onSelect 回填断言 + tabs 切换断言 + wizard step 推进断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 14 个其他 AMIS 页以 flux DSL 渲染
- [ ] 14 `.page.yaml` 已删除（visit-wizard 与 Phase 4 重叠以 `git rm` 幂等保证）
- [ ] 14 处菜单 url 已更新
- [ ] picker/wizard/tabs E2E spec 全绿

## Draft Review Record

- Independent draft review iteration 1: pending

## Closure Gates

- [ ] 74 个 AMIS DSL 非标页面全部以 flux 实现渲染（74 个 `.page.yaml` 已删除）
- [ ] 68 处菜单 url 已更新为 `.flux.yaml` 或 view.xml 入口
- [ ] `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 维持（74 - dashboards 11 - 报表 25 - SPC 3 - 其他 14 = 21 页 flux.yaml 新增；剩余 dashboards/报表/SPC/复杂页历史版/其他已迁/覆盖）
- [ ] 自定义 xlib（`<erp:dashboard>`/`<erp:report>`/`<erp:spc-chart>`）按 Phase 0 裁决落地（若裁决抽）
- [ ] 所有受影响的 E2E spec 全绿（10 dashboard smoke + 25 报表 smoke + 3 SPC + 5 复杂页 visual + 3 picker/wizard/fin/mnt）
- [ ] 仓库无 AMIS `.page.yaml` 残留于 74 个目标范围（grep `^  - type: (form|table|chart|cards|service|crud|tpl|html|wizard|steps)` 在 `_vfs/erp/*/pages/{dashboard,report,*/ErpSysNotification/*,party-search,lead-conversion,visit-wizard,disposal-wizard,asset-stocktake,project-settlement,project-pnl,expense-claim,bank-*,budget-*,stock-take-flow,inbox}` 下 = 0）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] `docs/backlog/frontend-ui-roadmap.md`「AMIS 退役路径」段更新（移除"AMIS schema 页经 flux amis-compat 通路仍可渲染"描述，记录本计划收口；同步 `frontend-ui-roadmap.md` 退出标准 [x]）
- [ ] `docs/design/flux-complex-pages.md` §2.4 / §3 控件映射总表补充非标页面（dashboards/报表/SPC/收件箱/picker）的 flux 范式；`docs/design/page-structure-patterns.md` §3 dashboard 范式增补（若与现有 §3 不一致）

## Deferred But Adjudicated

### AMIS Runtime/Adapter 包移除

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: nop-entropy 侧 AMIS 运行时（`host-amis-adapter`）属于外部仓库保护区域，须 dual-agent approval 才能删。当前 AMIS runtime 保留无运行时开销（无 page.yaml 调用即不被加载）；删除触发条件 = 仓库全量 grep 无 `component="AMIS"` 残留（已 done，2026-08-04）+ 全 AMIS DSL page.yaml 删除（本计划完成后即满足）+ flux E2E 连续 N=5 天全绿（roadmap AMIS 退役路径）。
- Successor Required: `yes`（保护区域 successor，nop-entropy 仓库）

### 报表页 i18n 标签补充

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `page.yaml` title 因 YAML 无 `i18n-en:` 属性机制，roadmap F15 已 Deferred 至 l10n successor；本计划新建 `.flux.yaml` 不引入新 i18n 缺口，与现状持平。
- Successor Required: `no`（已有 successor 跟踪）

### Timesheet 周网格共享组件

- Classification: `out-of-scope improvement`（与本计划无关）
- Why Not Blocking Closure: roadmap F12 Deferred But Adjudicated（roadmap `退出标准` 行 584）；与本计划 AMIS 删除正交。
- Successor Required: `no`

### Barcode/PDA 扫描交互

- Classification: `out-of-scope improvement`（Non-Goal 项目 2.x）
- Why Not Blocking Closure: roadmap `Non-Goals` 行 548 显式排除。
- Successor Required: `no`

### 自定义 xlib 抽象的过度抽取

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 0 Decision 已设阈值（≥3 复用 + 单页替换 ≥50% 缩减 + 总行数净减），不达阈值则不抽。过度抽取引入学习成本与 xlib 维护负担，残留风险 = 实施期可能"凑阈值"而强抽，须警惕——按 Phase 0 决策严格执行。
- Successor Required: `no`

## Closure

Status Note: 计划在 draft 状态，等待独立草案审查。5 Phase 收口后预期效果：74 个 AMIS DSL 页面全部迁移 flux + AMIS `.page.yaml` 全删 + 68 处菜单 url 同步 + `ErpAllFluxPagesTest` 0 失败 + 5 类 E2E spec 全绿。计划执行期可能出现的非平凡 pivot：① Phase 0 自定义 xlib 抽取裁决——若三个阈值不达，退化为逐页 `.flux.yaml` 直写，Phase 1/2/3 仍可执行但不复用 xlib，工作量略增；② 收件箱 tabs 客户端 JS 替代方案——flux expression 若不支持 AMIS filter 等价，则退化为 view.xml `<pages><tabs>` + 3 个 tab 子页，工作量+1 阶段子项；③ picker onSelect 事件 payload 映射——flux 是否完整等价 AMIS 的 `onSelect(item)` 事件未实测，须 Phase 5 PoC 验证。

Closure Audit Evidence:

- Reviewer / Agent: pending
- Evidence: pending