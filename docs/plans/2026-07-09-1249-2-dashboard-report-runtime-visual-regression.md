# 2026-07-09-1249-2-dashboard-report-runtime-visual-regression 看板运行时浏览器回归套件

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Mission: erp
> Work Item: 看板运行时视觉/浏览器回归（看板 AMIS 前端渲染层端到端 DOM 内容 + 结构断言）
> Source: `2026-07-08-0637-1` Deferred「像素级视觉回归（screenshot baseline diff）」+ AGENTS.md 当前重点「看板运行时视觉/浏览器回归」
> Related: `2026-07-08-0637-1`（E2E 冒烟套件源，已解除「套件建立时」类 Deferred 触发）、`2026-07-08-1445-2`（数值断言层源）、`2026-07-09-0814-2`（业务动作 E2E 范式源）
> Audit: required

## Current Baseline

- **看板 E2E 覆盖两层已建立**，但存在看板前端渲染验证缺口：
  - **冒烟层**（`dashboards/*.smoke.spec.ts`，0637-1）：10 看板。断言页面 DOM 渲染（body 文本 > 100 字符）+ KPI 标签关键词存在于 body 文本（如「收入」「支出」）+ GraphQL `/graphql` 请求 200 + 无 console error。**不验证 AMIS 将数值渲染进 DOM 元素。**（核实：`tests/e2e/dashboards/_helper.ts:16-21` 仅查 `bodyText.includes(kw)` 标签关键词。）
  - **数值断言层**（`dashboards/*.value.spec.ts`，1445-2/2210-2/0930-3/1145-1/1145-2）：经 `page.request.post('/graphql')` **直接调后端** `getDashboardKpi` 取响应值断言确定性数值。**完全绕过 AMIS 前端渲染**——验证后端聚合正确，不验证 AMIS 页面将 GraphQL 响应渲染进 DOM。（核实：`tests/e2e/dashboards/_helper.ts:45` 直调 `/graphql` 不经 AMIS。）
  - **看板前端渲染缺口**：AMIS 前端渲染层（page → AMIS 自身 GraphQL 调用 → DOM 渲染数值/图表/表格）未被验证。冒烟层仅查「标签关键词」，数值层绕过前端。若 AMIS adaptor 将 GraphQL 响应转换为 echarts config / crud items 时出错（字段映射错位、adaptor 抛错被吞），冒烟 + 数值层均无法捕获。核实看板 KPI 经 `tpl` 渲染为文本 DOM（如 finance `main.page.yaml:48` `tpl: "¥${revenue | round:2}"`，textContent 可匹配）；趋势图经 `type: chart`（echarts→canvas）；预警经 `type: crud`（table 行）。
- Playwright 配置 `playwright.config.ts`：单 chromium project、`screenshot: 'only-on-failure'`、无 screenshot baseline 比对、无 `toHaveScreenshot` 调用。
- **报表渲染容器接线缺陷（草案审查期发现，本计划 Non-Goal 但记录为 successor）**：24 张报表 page.yaml 中仅 `balance-sheet.page.yaml` 将渲染 button 的 ajax 响应经 `setVariable reportHtml → setValue target reportContainer` 注入 html 容器；其余 23 张（income-statement/cash-flow/period-close/ar-ap-aging + 全域其余报表）渲染 button 仅 `actionType: ajax` 触发 GraphQL 但**无响应管线**注入 `reportContainer`（`html: ""` 静态空）。用户点「渲染报表」后端计算返回但页面容器不显示。此为已确认产品缺陷（非测试缺口），归本计划 Deferred successor「报表渲染容器接线修复」，修复后报表 DOM 断言方可有意义。
- 0637-1 已解除「套件建立时」类 Deferred 触发（在冒烟层级别）。本计划在冒烟层之上深化至 AMIS 渲染层 DOM 断言，部分回应 0637-1 Deferred「像素级视觉回归」的视觉回归意图（以 DOM 内容+结构断言替代像素 diff）。
- **剩余差距**：看板 AMIS 前端渲染层的运行时浏览器回归缺失——需验证页面经 AMIS 自身 GraphQL 调用后，KPI 数值实际渲染进 DOM（卡片含数字而非仅标签）、echarts 图表渲染 canvas 元素（非空容器）、预警 crud 表格渲染数据行。

## Goals

- 建立看板 AMIS 前端渲染层运行时浏览器回归：验证 page → AMIS GraphQL 调用 → DOM 渲染全路径，覆盖 10 看板（KPI 数值渲染进 DOM + echarts 图表容器 + 预警表格行）。
- 捕获冒烟层（仅标签）+ 数值层（绕过前端）均无法发现的看板前端渲染回归：adaptor 字段映射错位、数值格式化破坏、echarts config 构建失败、crud items 映射断裂。
- 为后续前端定制（页面 delta/AMIS 组件调整）提供回归安全网。

## Non-Goals

- 报表 AMIS 前端渲染层 DOM 断言——24 张报表 page.yaml 中 23 张渲染容器未接线（草案审查期核实的产品缺陷，仅 balance-sheet 接线），DOM 断言在接线修复前不可达。归 Deferred successor「报表渲染容器接线修复 + 报表 DOM 断言」，触发条件=接线修复计划落地后。
- 像素级截图基线 diff（`toHaveScreenshot` 全页截图比对）——跨环境渲染稳定性（字体/无头差异）成本高，且 AMIS 布局为框架控制非应用控制。本期以 DOM 内容+结构断言（稳定高信号）为主；截图比对归 Deferred successor（触发条件=CI 无头渲染稳定性可接受且产品要求像素级一致性时）。
- 跨浏览器矩阵（Firefox/WebKit/移动视口）——AMIS 主目标为 Chromium 内核。归 Deferred successor（0637-1 Deferred，触发条件=需支持非 Chromium 浏览器时）。
- 报表下载产物（PDF/XLSX）内容 diff——后端 `download` 方法有 DataBean 序列化限制（0637-1 已知），属内容验证层 successor。
- CRUD 页面 DOM 渲染回归——CRUD 页面结构由平台 codegen 生成，非应用定制。归 successor。

## Task Route

- Type: `verification or audit work`（前端渲染层运行时回归，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（E2E 运行手册）、`docs/architecture/dashboards.md`（看板实现约定分层布局）
- Skill Selection Basis: `nop-frontend-dev` 为主（AMIS 页面 DOM 结构 + 渲染路径理解，page.yaml 分层布局 service/grid/wrapper/chart/crud 元素映射）；`nop-testing` 辅助（Playwright locator + waitForResponse 模式，既有 fixtures 复用）。两者均影响 helper 编写，阶段内项目按主导技能标记。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（webServer fresh-DB + 91 CSV 种子 + auth fixtures）。
- 看板/报表页面已落地（0637-1 已修复 115 处 page.yaml URL + 91 处字段选择，页面 DOM 渲染稳定）。
- 种子库已有确定性数值基线（1445-1/2210-1/0930-1/2/1045-1/1145-2/0628-1），KPI/报表数值非空。

## Execution Plan

### Phase 1 - 看板 AMIS 前端渲染层 DOM 断言（10 域）

Status: completed
Targets: `tests/e2e/visual/_helper.ts`（新建前端渲染断言 helper）、`tests/e2e/visual/dashboards.visual.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 既有 fixtures（loginAndNavigate）+ 10 看板 page.yaml 已稳定（0637-1 修复后）

- [x] `Decision`：DOM 断言策略裁决——选择 (a) 经 AMIS 自身 GraphQL 响应（`page.waitForResponse` 拦截 `/graphql` 含 `getDashboardKpi`）后断言 DOM（避免竞态：DOM 断言早于渲染完成）。**执行期发现生产缺陷（计划未预期）**：8 参数化看板（finance/sales/purchase/inventory/assets/manufacturing/maintenance/quality）的 page.yaml 手写 `query($var:Type){...($var)...}` 经 AMIS 运行时模板解析，裸 `$var` 被当作模板变量替换为空，致查询损坏（实测请求体 `query(:Long){ ...periodId: }`）、KPI 恒 0/空。2 非参数化看板（projects/master-data，查询无 `$var`）渲染正确。缺陷记入 `docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md` + 本计划 Deferred successor；本计划为验证任务（零生产代码/契约/模型变更），不实施修复。
  - DOM 断言目标据此调整：KPI 卡片结构（`.border.rounded.p-3` wrapper 渲染，全 10 域）+ echarts canvas（有趋势图的域，boundingBox 非零）+ 预警 crud 表格（`table` 渲染）经 AMIS GraphQL 管线（200）；确定性数值 token 断言仅对 2 非参数化域（projects/master-data，AMIS 路径完整）断言（projects `50000`、master-data `4`）。8 参数化域的数值 token 断言随 `$var` 修复 successor 落地。
  - 记录替代方案：(b) 直接 DOM 断言更简但有竞态风险；(c) 仅截图比对（跨环境不稳定）。
  - Skill: `nop-frontend-dev`
- [x] `Add`：`tests/e2e/visual/_helper.ts`——前端渲染断言 helper `assertDashboardRendered(cfg)`：编排 `loginAndNavigate` → `page.waitForResponse`（拦截含 `getDashboardKpi` 的 `/graphql` 响应，断言 200）→ 断言 KPI 卡片结构（`.border.rounded.p-3` wrapper 可见且 count≥1）→ 条件化数值 token（`cfg.expectedKpiTokens` 提供时，`expect.poll` 断言 `span.h3` textContent 含 token）→ 条件化 echarts canvas（`cfg.hasChart` 时 `toBeVisible` + boundingBox 非零）→ 条件化预警表格（`cfg.alertTable` 时 `table` 可见）。
  - Skill: `nop-frontend-dev | nop-testing`
- [x] `Add`：`tests/e2e/visual/dashboards.visual.spec.ts`——10 域看板前端渲染断言。全 10 域断言 AMIS GraphQL 管线 + KPI 卡片结构 + echarts canvas（master-data 无 trend 跳过）+ 预警表格；2 非参数化域（projects/master-data）额外断言确定性数值 token。8 参数化域因 `$var` 损坏缺陷（见 docs/bugs/）暂仅断言结构，文件头注释标注缺陷锚点与 successor。
  - Skill: `nop-frontend-dev`
- [x] `Proof`：运行 `npx playwright test tests/e2e/visual/dashboards.visual.spec.ts --workers=1`，10 域全绿（1.5m）；全套件 `npx playwright test --workers=1` 120 passed（110 既有 + 10 新增 visual，16.2m，0 回归）。
  - 验证命令：`npx playwright test tests/e2e/visual/dashboards.visual.spec.ts --workers=1` + 全套件 `npx playwright test --workers=1`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 10 域看板经 AMIS 自身 GraphQL 调用（200）后渲染层管线完整性断言全绿（KPI 卡片结构 + 有趋势图的域 echarts canvas 非零尺寸 + 预警表格）；2 非参数化域（projects/master-data）KPI 确定性数值渲染进 DOM（span.h3 含期望 token）；8 参数化域 `$var` 损坏缺陷锁定记入 docs/bugs/ + Deferred successor（数值 token 断言随修复 successor 落地）

### Phase 2 - 文档对齐 + Deferred 解除登记

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/bugs/`（报表接线缺陷 + AMIS `$var` 损坏缺陷记录）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 增看板前端渲染回归层段（AMIS 渲染管线 DOM 断言 + helper 范式 + 与冒烟层/数值层的层间区分 + 本层首次发现的 `$var` 损坏缺陷）+ 套件计数更新（110→120）；`docs/testing/known-good-baselines.md` 增本计划基线行。登记 0637-1 Deferred「像素级视觉回归」部分回应（DOM 内容+结构层交付替代像素 diff；纯像素 diff 仍 Deferred）。
  - Skill: none
- [x] `Add`：`docs/bugs/` 记录两缺陷为 successor 锚点：(1) 报表渲染容器接线缺陷（23/24 报表 page.yaml 渲染 button ajax 无响应管线注入 reportContainer，仅 balance-sheet 接线；核实证据：`rg 'target:.*"reportContainer"|reportHtml'` 仅 1 命中）；(2) AMIS `$var` GraphQL 查询模板损坏缺陷（Phase 1 执行期发现，`docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md`，8 参数化看板 + 全参数化报表受影响）。
  - Skill: none

Exit Criteria:

- [x] e2e-runbook 含看板前端渲染回归层段 + 层间区分 + 套件计数（110→120）；known-good-baselines 含基线行；报表接线缺陷 + AMIS `$var` 损坏缺陷记录于 docs/bugs/

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0baba4643ffe0rUDdRV7zLLXtz`，独立 general 子代理，新会话冷重播无执行者上下文) — 1 BLOCKER + 1 MAJOR + 2 MINOR：
  - B1（BLOCKER）：Phase 2 报表 DOM 断言不可达——24 报表 page.yaml 中仅 balance-sheet 将 ajax 响应注入 reportContainer，其余 23 张渲染 button 无响应管线（`html: ""` 静态空）。Phase 2 exit criteria 必然失败。经实时仓库核实确认（`rg 'target:.*"reportContainer"|reportHtml'` 仅 1 命中）。
  - M1（MAJOR）：Deferred 血缘双重解除——0637-1 已解除「套件建立时」类触发（冒烟层级别），本计划原文声称再次解除误导。
  - m1（MINOR）：KPI token 格式示例错误（finance tpl 无千分位 `¥1130.00` 非 `1,130`）。
  - m2（MINOR）：Skill 不一致（Task Route 列双技能但项目仅标 nop-frontend-dev）。
  - n1（NOTE）：Phase 1 看板可行性 SOLID（KPI tpl 文本 DOM + echarts canvas + master-data 无 chart 核实）。
  - **已修复**：B1——移除报表 Phase 2，报表接线缺陷记入 docs/bugs/ + Deferred successor（触发条件=接线修复后）；M1——重写 Source/Related 与基线血缘（0637-1 在冒烟层解除，本计划深化至 AMIS 渲染层）；m1——token 格式改为 `¥1130.00` 实际 tpl 输出；m2——Task Route 技能表述统一，helper 项标记 `nop-frontend-dev | nop-testing`。

## Closure Gates

> 本计划为前端/浏览器 E2E（视觉/UX 驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增 visual spec + 既有 spec）+ 既有后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（10 看板 AMIS 前端渲染层 DOM 断言全绿：全 10 域渲染管线结构 + 2 非参数化域数值 token）
- [x] 相关文档对齐（e2e-runbook 看板前端渲染层段 + 套件计数 110→120 + known-good-baselines 基线行 + 报表接线缺陷 + AMIS `$var` 损坏缺陷记录于 docs/bugs/）
- [x] 已运行验证：`npx playwright test`（全套件 120 passed，16.2m，含新增 visual spec 全绿 0 回归）+ `mvn install -DskipTests`（154 模块 BUILD SUCCESS，确认 E2E 新增文件无后端污染——在根 tests/，非 reactor 模块）
- [x] 无「原计划范围内」项目未裁决即降级：报表 DOM 断言（产品缺陷接线缺失，草案期已知 Non-Goal + successor）+ 像素 diff / 跨浏览器（计划内 Non-Goal）均附触发条件；8 参数化看板数值 token 断言因 **Phase 1 执行期新发现** 的 AMIS `$var` 损坏产品缺陷降级，已裁决入 Deferred successor（含 docs/bugs/ + 触发条件），结构断言仍交付（非空降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符（审计 `ses_0b9ee0e6affeEYbPhCPV9HdKe4`，初轮 CHANGES REQUESTED 1 BLOCKER 已修复，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中（visual/_helper.ts + dashboards.visual.spec.ts + docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md + e2e-runbook + known-good-baselines + 本计划 Closure）

## Deferred But Adjudicated

### 看板/报表 AMIS `$var` GraphQL 查询模板损坏修复 + 8 参数化看板数值 token 断言

- Classification: `out-of-scope improvement`（含 Phase 1 执行期发现的产品缺陷）
- Why Not Blocking Closure: Phase 1 执行期发现 8 参数化看板（finance/sales/purchase/inventory/assets/manufacturing/maintenance/quality）+ 全参数化报表 page.yaml 的手写 `query($var:Type){...($var)...}` 经 AMIS 运行时模板解析，裸 `$var` 被替换为空，致查询损坏、KPI 恒 0/空（实测请求体 `query(:Long){ ...periodId: }`）。此为已确认产品缺陷（记入 `docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md`，含根因/证据/影响范围/备选修复方向）。本计划为验证任务（零生产代码/契约/模型变更），不实施 page.yaml 修复；8 参数化看板的确定性数值 token 断言随修复 successor 落地（当前为渲染管线结构断言，2 非参数化域 projects/master-data 已含数值 token 断言）。此缺陷正是本计划目标「捕获冒烟层+数值层均无法发现的看板前端渲染回归」的实例——本层为首个能捕获它的层。
- Successor Required: `yes`
- Trigger Condition: 当看板/报表 AMIS `$var` 查询模板修复计划（迁移至平台规范 `@query:` URL 范式，或 `${'$'}` 转义裸 `$`，或 requestAdaptor 重建——successor 须选定方向并实测 Long 类型参数推断风险）落地后；可与下方「报表渲染容器接线修复」合并为统一报表/看板前端 successor。

### 报表渲染容器接线修复 + 报表 AMIS 前端渲染层 DOM 断言

- Classification: `out-of-scope improvement`（含已确认产品缺陷子集）
- Why Not Blocking Closure: 24 报表 page.yaml 中 23 张渲染 button ajax 无响应管线注入 reportContainer（仅 balance-sheet 接线），用户点「渲染报表」后端计算返回但页面容器不显示。此为已确认产品缺陷（草案审查期核实，记入 docs/bugs/），报表 DOM 断言在接线修复前不可达。本计划聚焦看板前端渲染层（已验证可行），报表面归独立 Fix-heavy successor。
- Successor Required: `yes`
- Trigger Condition: 当报表渲染容器接线修复计划（镜像 balance-sheet `setVariable reportHtml → setValue target reportContainer` 范式补全 23 张报表 page.yaml）落地后。

### 像素级截图基线 diff（toHaveScreenshot 全页比对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 跨环境渲染稳定性（字体/无头/CI 差异）成本高，AMIS 布局为框架控制非应用控制。本期 DOM 内容+结构断言（数值渲染进 DOM + echarts canvas + 表格行）捕获 adaptor/渲染路径回归，信号优于像素 diff。
- Successor Required: `yes`
- Trigger Condition: 当 CI 无头渲染稳定性可接受（字体固化 + 差异可控）且产品要求像素级一致性（如品牌看板视觉验收）时。

### 跨浏览器矩阵（Firefox/WebKit/移动视口）

- Classification: `optimization candidate`
- Why Not Blocking Closure: AMIS 主目标为 Chromium 内核，`playwright-e2e-guide.md` 基线为 chromium 单 project。归 0637-1 既定 Deferred。
- Successor Required: `yes`
- Trigger Condition: 当需支持非 Chromium 浏览器或移动端看板时。

## Closure

Status Note: 完成。看板 AMIS 前端渲染层运行时浏览器回归套件落地（`tests/e2e/visual/`，10 域，120 passed 0 回归）。执行期达成计划核心目标「捕获冒烟层+数值层均无法发现的看板前端渲染回归」——首个能捕获 AMIS `$var` GraphQL 查询模板损坏 P1 产品缺陷的层（8 参数化看板 KPI 恒 0/空）。该缺陷因属生产 page.yaml 变更（超本计划验证任务范围）归 Deferred successor（含 docs/bugs/ + 触发条件）；2 非参数化域（projects/master-data）数值 token 断言全绿证明 AMIS 渲染管线本身完整。报表接线缺陷 + `$var` 损坏缺陷均记入 docs/bugs/ 为 successor 锚点。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（`ses_0b9ee0e6affeEYbPhCPV9HdKe4`，general 类型，新会话冷重播无执行者上下文）。VERDICT 初轮 `CHANGES REQUESTED`——1 BLOCKER（B1：Phase 2 item 2 + Closure Gate「相关文档对齐」声称报表接线缺陷记录于 docs/bugs/，实际仅 `$var` 损坏缺陷在 docs/bugs/，报表接线缺陷仅在 plan body）+ 1 non-blocking 观察（n1：projects grossMarginService 亦含 `$var`）。
- **已修复**：B1——新建 `docs/bugs/2026-07-09-1249-report-render-container-wiring.md`（报表接线缺陷记录，含根因/实测证据/影响范围/修复方向），现 docs/bugs/ 含两缺陷文档；n1——`docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md` 影响范围段补 projects grossMarginService + 含 `$var` 子查询 successor 覆盖注。审计核查的 1-5 全项（deliverables/defect 真实性/scope 裁决正当性/exit criteria 诚实性/closure gates/daily log/backlog）除 B1 外均 PASS，B1 修复后无遗留阻塞项。

Follow-up:

- 看板/报表 AMIS `$var` 查询模板损坏修复 + 8 参数化看板数值 token 断言升级（Deferred successor，见上方 Deferred 段，触发条件=修复计划落地）
- 报表渲染容器接线修复 + 报表 AMIS 前端渲染层 DOM 断言（Deferred successor，可与上一项合并）
