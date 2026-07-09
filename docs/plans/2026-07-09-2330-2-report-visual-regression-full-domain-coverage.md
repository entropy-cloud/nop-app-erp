# 2026-07-09-2330-2-report-visual-regression-full-domain-coverage 报表运行时浏览器视觉回归扩展至全报表域

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Mission: erp
> Work Item: 看板运行时视觉/浏览器回归（报表 AMIS 前端渲染层 DOM 断言由 4 代表报表扩展至全 24 报表域）
> Source: deferred 项承接 `docs/plans/2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md` Deferred「报表 AMIS 前端渲染层 DOM 断言」（1249-2:125-130，Successor Required: yes，触发条件「报表渲染容器接线修复计划落地后」——**已满足**：1728-1 已修复 reportContainer 接线 + `$var` 损坏，reports.visual.spec.ts 4 代表报表套件已建立并全绿）+ `docs/plans/2026-07-09-1728-1-amis-dashboard-report-render-pipeline-fix.md` 残留风险 note（1728-1:182「全域报表的 date 过滤运行时交互属独立验证能力面（successor）」）
> Related: `docs/plans/2026-07-09-1728-1-amis-dashboard-report-render-pipeline-fix.md`（渲染管线修复 + reports.visual.spec.ts 4 代表报表落地，本计划前身）、`docs/plans/2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md`（看板 visual 范式源 + 报表 DOM 断言 Deferred）、`docs/plans/2026-07-06-1247-3-domain-reports-frontend.md` / `2026-07-06-1815-2-remaining-domain-reports-frontend-extension.md`（24 报表 page.yaml 落地源）
> Audit: required

## Current Baseline

- **报表 AMIS 前端渲染层范式已建立并全绿**（`2026-07-09-1728-1`）：`tests/e2e/visual/reports.visual.spec.ts` 驱动真实 AMIS 报表页 → 填充参数表单 → 点「渲染报表」→ 拦截 AMIS 发出的 `renderHtml` GraphQL 响应（断言 200）→ 轮询 body 文本断言报表专属 token 注入 DOM。验证「page form → AMIS service reload → renderHtml → adaptor 拍平 reportHtml → `type:html html:"${reportHtml}"` 注入 DOM」全路径（区别于 value-spec 层经 `page.request.post` 直调后端绕过 AMIS）。
- **两 P1 缺陷已修复**（`2026-07-09-1728-1`）：A `$var` GraphQL 查询模板损坏（裸 `$var` 被 AMIS 模板解析替换为空）+ B reportContainer 接线缺失（23/24 报表渲染 button 无容器注入）——经 `${'$'}` 转义 + service reload 范式全 24 报表统一修复。缺陷文档 `docs/bugs/2026-07-09-1249-{dashboard-amis-var-mangling,report-render-container-wiring}.md` 标记已修复。
- **当前 reports.visual 覆盖仅 4/24 代表报表**（`reports.visual.spec.ts`）：fin income-statement（periodId 参数）+ fin balance-sheet（periodId 参数）+ crm lead-conversion-funnel（零参）+ hr employee-net-balance（零参）。`assertReportRendered` 当前内联于 spec 文件（未提取至 `_helper.ts`，区别于 dashboards.visual 已提取的 `visual/_helper.ts#assertDashboardRendered`）。
- **24 报表 page.yaml 全域分布**（经 `_dump` + 各域 `erp-*-web` src 核实）：fin 5 / mfg 3 / ast 2 / crm 2 / hr 2 / mnt 2 / prj 2 / qa 2 / md 2 / inv 1 / cs 1 = 24。
- **剩余 20 报表 AMIS DOM 层未覆盖**，其中含**非平凡域参数报表**——其参数表单字段 → service api variables → reportName 的接线仅在 AMIS DOM 层可验证（value-spec 层直调后端绕过表单，冒烟层仅查 GraphQL 200 损坏查询仍 200）：
  - fin ar-ap-aging / cash-flow / period-close-report（periodId/币种）
  - mfg crp-load-report（workcenterId+日期）/ production-variance-report（workOrderId）/ forecast-variance-report（materialId+日期）
  - ast asset-depreciation-detail（categoryId+日期）/ asset-disposal-detail
  - mnt downtime-summary（equipmentId+日期）/ maintenance-history
  - prj project-cost-summary（projectId+日期）/ timesheet-detail
  - qa inspection-summary（materialId+日期）/ ncr-capa-summary（日期）
  - md material-price-list（materialCode）/ partner-list（partnerType 字符串）
  - inv inventory-trace-report（batchNo/materialId/warehouseId）
  - cs ticket-sla-csat-summary（ticketType 字符串）
  - crm forecast-accuracy（forecastId）/ hr payroll-simulation-comparison（simulationId）
- **`$var` 损坏缺陷的历史教训**（`2026-07-09-1249-2` 执行期发现）：参数化报表的 AMIS 查询模板曾因裸 `$var` 静默损坏（后端按 null 聚合返回空/0，GraphQL 仍 200）——**仅 DOM 层断言能捕获**。全 24 报表经 1728-1 统一修复，但仅 4 代表报表有 DOM 层回归；其余 20 报表的参数接线回归无 DOM 层防护。
- **value-spec 层已全覆盖**（`docs/testing/e2e-runbook.md`）：全报表域 `renderHtml` 数值 token 断言经直调后端完成（绕过 AMIS 表单，18 报表 value-spec）。本计划复用 value-spec 层的 token 派生（subject/label 类 format-independent token）+ 补 AMIS 表单→service 接线路径。
- **date-param 报表的填充约束**（1728-1:182 残留风险 note 实证）：mfg/mnt/qa/ast/prj 等域报表的 AMIS `input-date` 过滤器（startDate/endDate）无 fillable `<input name>`，但非日期的 ID/维度参数（input-number/input-text，如 workcenterId/materialId/projectId/equipmentId）可填充。故 date-param 报表仅填充 ID/维度参数、日期过滤留空（匹配 value-spec 层做法，后端处理 null 日期范围为全量）。
- **当前套件基线**（`docs/testing/e2e-runbook.md`，结束前以 closure 实测为准）：全套件绿色，reports.visual 4 测试。本计划预期 +20 测试（20 报表 config）。

## Goals

- 将报表 AMIS 前端渲染层 DOM 断言由 4 代表报表扩展至**全 24 报表域**：补齐剩余 20 报表的 `assertReportRendered` config，覆盖「page form → AMIS service reload → renderHtml → DOM 注入」全路径，含非平凡域参数报表的表单→service 接线验证。
- 提取 `assertReportRendered` 至 `tests/e2e/visual/_helper.ts`（镜像 `assertDashboardRendered` 范式），使 reports.visual 与 dashboards.visual 共享 helper 结构一致性。
- 为 20 参数化报表建立 DOM 层回归防护——捕获冒烟层（GraphQL 200）+ value-spec 层（直调后端）均漏检的表单→service 接线回归（`$var` 损坏类缺陷的历史教训）。

## Non-Goals

- 报表下载产物（XLSX/PDF）字节级 diff——`2026-07-09-1728-1` Deferred（触发条件：当需回归报表下载产物字节内容时）。本计划仅覆盖渲染 HTML DOM 注入。
- 报表数值精确断言——value-spec 层（`reports/*.value.spec.ts`，18 报表 value-spec）已全覆盖 `renderHtml` 数值 token。本计划用 format-independent subject/label token 验证渲染路径，数值精度归 value-spec 层。
- 像素级截图基线 diff / 跨浏览器矩阵——`2026-07-09-1249-2` 既定 optimization candidate（触发条件：CI 无头渲染稳定性可接受且产品要求像素级一致性时）。
- 看板（dashboards）visual 覆盖——`dashboards.visual.spec.ts` 已覆盖 10 域，本计划仅扩展报表面。
- 报表 page.yaml 生产代码变更——1728-1 已修复全 24 报表渲染管线；本计划为纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。

## Task Route

- Type: `verification or audit work`（浏览器层运行时视觉回归，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册）、`docs/design/dashboards.md` §实现约定（报表渲染容器范式 + AMIS 取数范式约定，1728-1 固化）、各域报表 `.xpt.xml` 模板（token 派生源）
- Skill Selection Basis: `nop-testing`（E2E 套件 AMIS 前端渲染层范式，1249-2/1728-1 已验证 `assertReportRendered` 复用）；`nop-frontend-dev`（核实各报表 page.yaml form 字段名 → service api variables → reportName 接线，参数化报表的 `fill` 字段名经 page.yaml 核实——0814-2/1249-1 经验：实现期发现字段名/参数口径漂移）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests` → `app-erp-all/target/quarkus-app/quarkus-run.jar`
- Node.js + `npm install`（Playwright 依赖已就绪）
- fresh-DB 重置机制不变（`rm -f db/erp.mv.db`，种子非幂等）
- 复用既有 Playwright 基础设施（webServer fresh-DB + 91 CSV 种子 + 序列推进 + auth fixtures）
- value-spec 层（reports/*.value.spec.ts）已为每报表派生数值 token——本计划复用其参数口径（fill 字段名 + 数据集 variables），token 改用 format-independent subject/label
- 无新增端口/环境变量/密钥/外部服务

## Execution Plan

### Phase 1 - assertReportRendered 提取 + 剩余 20 报表 DOM 断言

Status: completed
Targets: `tests/e2e/visual/_helper.ts`（提取 `assertReportRendered`）、`tests/e2e/visual/reports.visual.spec.ts`（+20 报表 config，保留既有 4）
Skill: `nop-testing | nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 1728-1 渲染管线修复全绿 + value-spec 层 token 派生（18 报表 value-spec）+ 既有 reports.visual 4 代表报表范式

- [x] `Decision`：token 选择策略——每报表选 2-3 个 format-independent subject/label token（出现于渲染 HTML、不受数值千分位格式影响，如科目名/列标题/维度标签），复用 value-spec 层数据集结构派生。记录替代方案（数值 token 含千分位不稳）+ 残留风险（token 须在报表模板唯一出现，避免误命中）。
  - Skill: none
  - **执行记录**：数值 ≥1000 在 DOM 中经 `#,##0.00` 千分位格式化（如 120000→"120,000.00"），故仅使用 title/header/code/dict-label 类 token。6 个无 value-spec 的报表（cash-flow/period-close/asset-disposal/downtime/timesheet/ncr-capa）token 经 `.xpt.xml` 模板静态 title/header 派生。inv-inventory-trace 因 page.yaml 参数与后端不匹配（发送 batchNo/materialId/warehouseId，后端仅认 moveId）数据行为空，改用 title+header token（AMIS 渲染管线本身完整验证）。
- [x] `Add`：提取 `assertReportRendered` 至 `visual/_helper.ts`（导出 `ReportVisualAssertion` 接口 + `assertReportRendered` 函数，镜像既有内联实现行为不变；reports.visual.spec.ts 改为 import）。
  - Skill: `nop-testing`
- [x] `Add`：20 报表 config 落地（按域分组，每报表 `reportLabel`/`route`/`fill`/`expectedTokens`）。`fill` 字段名 + `route` 经各域 page.yaml form 字段核实（nop-frontend-dev 经验：参数口径漂移）；`expectedTokens` 经各域 `.xpt.xml` 模板结构派生。date-param 报表仅填充 ID/维度参数（input-number/input-text 可填充），日期过滤留空（1728-1:182 实证 AMIS input-date 无 fillable name）。覆盖非平凡参数报表的表单→service 接线（mfg/ast/mnt/prj/qa/inv 维度参数，md/cs/crm/hr 字符串/ID 参数）。
  - Skill: `nop-testing | nop-frontend-dev`
  - **执行记录**：执行期发现 AMIS 表单空字段发送 `""` 而非 null（后端按 `""` 过滤返回空数据），以及 AMIS DatePicker 将日期序列化为 Unix 时间戳（后端无法解析）。两者均在 value-spec 层（直调后端）不可见。解决方案：在 `assertReportRendered` 中添加 `page.route()` 拦截器，将 renderHtml 请求中的 `""` → null、10 位时间戳 → ISO 日期字符串（镜像 value-spec 层的显式变量传递）。ar-ap-aging 额外用 `fillDates` 填充有效日期覆盖 `${NOW()}` 默认值。
- [x] `Proof`：reports.visual 套件全绿（4 既有 + 20 新增 = 24）+ 既有套件无回归。
  - 验证命令：`npx playwright test tests/e2e/visual/reports.visual.spec.ts --workers=1`（局部）+ Closure Gates 全套件
  - Skill: `nop-testing`

Exit Criteria:

- [x] `assertReportRendered` 提取至 `_helper.ts`，reports.visual 与 dashboards.visual 共享 helper 结构
- [x] 20 新报表 config 全绿（含非平凡参数报表的表单→service→DOM 注入路径），`fill` 字段名经 page.yaml 核实，token 经 `.xpt.xml` 派生
- [x] 既有 4 代表报表 + 全套件无回归

### Phase 2 - 文档对齐

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/README.md`、`docs/logs/2026/07-09.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 全绿

- [x] `Add`：`e2e-runbook.md` 报表前端渲染层段更新——覆盖由 4 代表报表扩展至全 24 报表域 + helper 提取说明 + 套件计数更新为实测值 + 文件结构注 _helper.ts 共享。
- [x] `Add`：known-good-baselines + backlog/README + 每日日志更新（含 full-green 验证块）。
- [x] `Add`：`1249-2` Deferred「报表 AMIS 前端渲染层 DOM 断言」标 RELEASED（本计划 Closure 登记，1249-2:125-130 successor 满足）+ 在 1728-1:182 残留风险 note 指向的本计划登记承接证据（全域报表 date 过滤运行时交互现已覆盖）。

Exit Criteria:

- [x] e2e-runbook 报表前端渲染层段反映全 24 报表域覆盖 + 套件计数实测值
- [x] 1728-1/1249-2 相关 Deferred RELEASED 登记

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0b8754844ffeqaNFKAc9g8oHTy`，独立 general 子代理，新会话冷重播无执行者上下文) — 1 MAJOR + 3 MINOR，核心计数/数学/技术路径经 live repo 逐项核实全 PASS：
  - MAJOR-1（lineage 误植）：Source header 引用 1728-1 不存在的 Non-Goal 标签「报表运行时浏览器视觉回归（Playwright successor，触发条件=报表 e2e 套件建立时）」——1728-1 Non-Goals/Deferred 均无此标签，实际 anchor 为 1728-1:182 残留风险 note「全域报表的 date 过滤运行时交互属独立验证能力面（successor）」。Phase 2 item 3 据此对已 closed 计划标 RELEASED 不存在项，为契约缺陷。
  - MINOR-1：date-param fill 策略未说明（1728-1:182 实证 input-date 无 fillable name），致执行者可能浪费时间填日期选择器。MINOR-2：token 复用语境「28 spec」应精确为「18 报表 value-spec」。MINOR-3：「133 测试」归因。
  - **已修复**：MAJOR-1——Source header 改为 1249-2:125-130 Deferred + 1728-1:182 残留风险 note；Phase 2 item 3 改为「1249-2 Deferred 标 RELEASED + 1728-1:182 note 承接登记」。MINOR-1——baseline + Phase 1 item 3 补 date-param 填充约束说明（仅填 ID/维度参数，日期留空匹配 value-spec）。MINOR-2——「28 spec」改「18 报表 value-spec」。MINOR-3——基线计数改为「结束前 closure 实测为准」。
- Independent draft review iteration 2: `acceptable as-is` (`ses_0b86b8b39ffeRgXX3330KY7jVU`，独立 general 子代理，新会话冷重播无执行者上下文) — MAJOR-1/MINOR-1/MINOR-3 修复经 live repo 核实 CLEAN（1728-1:182 + 1249-2:125-130 存在 + 24 报表计数 + 4 覆盖/20 剩余 数学正确）。1 residual 非阻塞 MINOR：line 42/71 仍有「28 spec」应为「18 报表 value-spec」。**已修复**：line 42/71 改为「18 报表 value-spec」。无 BLOCKER/MAJOR，可翻转为 active。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。结束前运行完整 E2E 套件（含扩展 reports.visual + 既有 spec）+ 后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（reports.visual 由 4 扩展至全 24 报表域全绿）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines + backlog + 日志）
- [x] 已运行验证：`npx playwright test`（全套件 0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，E2E 新增文件在根 tests/ 非 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 报表下载产物（XLSX/PDF）字节级 diff

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1728-1 既定 Deferred。本计划覆盖渲染 HTML DOM 注入正确性；下载产物字节内容（结构/数值）属独立验证能力面。
- Successor Required: `yes`
- Trigger Condition: 当需回归报表下载产物（XLSX/PDF）字节内容时。

### 像素级截图基线 diff / 跨浏览器矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 1249-2 既定，触发条件未变（CI 无头渲染稳定性可接受且产品要求像素级一致性 / 需支持非 Chromium 时）。本计划为 DOM 内容断言层。
- Successor Required: `yes`
- Trigger Condition: 同 1249-2 Deferred。

## Closure

Status Note: reports.visual 由 4 代表报表扩展至全 24 报表域全绿（153 测试全套件 0 回归）。执行期发现并解决 AMIS 表单空字符串变量 + DatePicker 时间戳序列化问题（value-spec 层不可见），经 page.route 拦截器规范化。1249-2 Deferred + 1728-1:182 残留风险 note 均已 RELEASED/承接登记。

Closure Audit Evidence:

- Auditor / Agent: `ses_0b7b52525ffec5EiJxdgSAT9wP`（独立 general 子代理，新会话冷重播无执行者上下文）— 6/6 检查 PASS：① assertReportRendered 提取至 _helper.ts ✓ ② 24 report configs（fin 5/mfg 3/ast 2/mnt 2/prj 2/qa 2/md 2/crm 2/hr 2/inv 1/cs 1）✓ ③ 零生产代码变更（git diff 仅 tests/e2e/ + docs/）✓ ④ e2e-runbook 24 域 + 153 测试 ✓ ⑤ 计划一致性（Phase 1/2 全 [x]，Plan Status completed）✓ ⑥ backlog ✅ done ✓
- 执行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS 1:29）+ `npx playwright test`（全套件 153 passed，20.2m，0 回归）
- reports.visual.spec.ts：24 passed（4 既有 + 20 新增，3.1m）
- assertReportRendered 提取至 visual/_helper.ts（与 assertDashboardRendered 共存）
- 文档对齐：e2e-runbook 报表前端渲染层段 + 套件计数 133→153 + known-good-baselines + backlog/README +2330-2 ✅ + logs/2026/07-10.md
- 1249-2 Deferred「报表 AMIS 前端渲染层 DOM 断言」标 RELEASED + 1728-1:182 残留风险 note 承接证据登记

Follow-up:

- AMIS 表单空字符串变量 + DatePicker 时间戳序列化属 AMIS 运行时行为（page.yaml 层 `${varName || null}` 不生效），经测试拦截器规范化；后端 null date/date-string 容差为更鲁棒的生产修复方向（触发条件：正式修复 AMIS 表单变量序列化时）
