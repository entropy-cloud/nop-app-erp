# 2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline 看板/报表像素级截图视觉回归基线层

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 看板运行时视觉/浏览器回归深化（像素级截图基线 successor）
> Source: 六处携带像素截图子集的 Deferred（**异质 bundle**，非同形——本计划仅消费其中「像素级截图基线 diff」子集）：`2026-07-09-1249-2`（纯像素基线）/ `2026-07-09-2330-2`+`2026-07-09-1728-1`（像素基线 + 跨浏览器 bundle）/ `2026-07-09-0930-3`+`2026-07-09-1045-2`+`2026-07-09-1145-1`（像素视觉回归 + 报表下载产物 diff + 跨浏览器 triple-bundle，血缘溯 0637-1）。本计划仅 RELEASE 像素截图子集；各 bundle 的「报表下载产物字节级 diff」子集（0204-1 仅交付二进制有效性回归层，字节级 diff 仍 open optimization candidate）与「跨浏览器矩阵」子集（本计划 Non-Goal）不在范围、不 RELEASE。
> Related: `2026-07-17-2010-1-dashboard-echarts-spc-crp-charts.md`（同批 N=1，先落地使像素基线捕获含新图表的完整态——非硬前置，见 Infrastructure）、`2026-07-06-1606-2-remaining-domain-dashboards-frontend.md` Deferred「看板运行时视觉/浏览器回归验证」（**已解除**——Playwright 套件经 0637-1/1249-2 建立；列此仅为历史血缘，非开放义务）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17，`grep` 实测）：

### 既有视觉验证层（DOM 内容/结构断言）

- `tests/e2e/visual/dashboards.visual.spec.ts`（10 域看板：驱动真实 AMIS 页面→拦截 AMIS 自身 GraphQL 响应→断言 DOM，验证 page→AMIS GraphQL→adaptor→DOM 全路径）。
- `tests/e2e/visual/reports.visual.spec.ts`（全 24 报表域 DOM 断言）。
- 两层均**不使用 `toHaveScreenshot`**——经 `grep -rl toHaveScreenshot tests/e2e/` 实测：**NONE**，全仓零像素级截图断言。

### 像素基线基础设施缺口

- `playwright.config.ts`：单一 chromium project（`name: 'chromium'`，可选 `channel: 'chrome'`），无字体固化配置、无 `maxDiffPixelRatio`/`maxDiffPixels` 容差、无动态区域 `mask` 约定。
- 触发条件复核：六处 Deferred 触发条件为「CI 无头渲染稳定性可接受（字体固化 + 差异可控）**且**产品要求像素级一致性时」。
  - 第二支（产品要求像素级一致性）：AGENTS.md / `project-context.md:34` 明示当前重点含「看板运行时视觉/浏览器回归」**广义指向**视觉回归，但既有 DOM 内容/结构断言层（1249-2/2330-2/1728-1，120+ passed）已**部分**满足该重点；像素级一致性是否被产品**明确**要求（如品牌看板视觉验收）尚无 PM 明示。故第二支为 **argued-as-met**（非确证已满足）。本计划 Phase 1 的实质是**在可能已变的条件下重新审视 1249-2 当年「像素 diff 信号弱于 DOM 断言、defer-now」的裁决**（类比 2330-1 在新条件下重新审视 1249-1 的 xwf 不可行裁决）——若 Phase 1 实测证明像素层能稳定捕获 DOM 层盲区（布局/样式回归）则推进，否则维持 defer。
  - 第一支（字体固化 + 差异可控）**尚未建立**——这正是本计划 Phase 1 要裁决并（若可行）交付的基础设施（字体固化 + 动态区域 mask + 容差）。

### 已知的像素 diff 噪声源（须 mask/固化）

- 看板/报表含日期参数（`${NOW()}`/DatePicker）、时间戳、用户名、动态计数（fresh-DB 种子确定性已建立，但渲染瞬时元素如 canvas 绘制时序、图表动画末态须处理）。
- AMIS echarts canvas（图表以 canvas 渲染，跨次绘制像素级一致需等待动画完成 + `waitForFunction` 或禁用动画）。

### 剩余差距

像素级截图基线层完全缺失；DOM 层能捕获 adaptor/数据回归但**不能**捕获布局/样式回归（CSS 错位、元素重叠、图表 canvas 尺寸塌缩、响应式断点破坏）——这些是 DOM 内容断言的结构盲区。

## Goals

- **Phase 1 权威裁决像素基线可行性**（重新审视 1249-2 当年「像素 diff 信号弱于 DOM 断言、defer-now」裁决）：在代表性看板（finance 参数化 + master-data 非参数化）上实测字体固化 + 动态区域 mask + echarts 动画处理 + 容差调参，产出 Decision（可行 / 不可行 + 残留风险）。**若裁决不可行**：Phase 2/3 跳过，将六处 Deferred 的像素截图子集更新触发条件并归 adjudicated residual（对齐 2330-1 不可行裁决范式），本计划以裁决记录收口。
- **（若可行）建立像素基线**：为 10 域看板 + 代表性报表子集建立 `toHaveScreenshot` 基线（含 mask 动态区域 + 容差），捕获布局/样式回归（CSS 错位/元素重叠/canvas 尺寸塌缩/响应式断点破坏——DOM 内容断言的结构盲区），作为 DOM 层的互补层。注：10 看板数值渲染经 1728-1 修复 `$var` 模板损坏缺陷后已非空，像素基线将捕获**正确**渲染态而非损坏态。
- **CI 集成约定**：在 `playwright.config.ts` / `tests/e2e/visual/_helper.ts` 固化字体固化 + mask + 容差范式，写入 `docs/testing/e2e-runbook.md`。
- **owner doc 收口**：仅对像素截图子集收口——`1249-2` 纯像素基线 Deferred 可整体 RELEASE；其余 5 处 bundle 的像素截图子集标 `**像素截图子集 RELEASED by 2026-07-17-2010-2**`（bundle 内「报表下载产物 diff」/「跨浏览器矩阵」子集**不**在范围、**不** RELEASE，触发条件不变）。若裁决不可行则改更新触发条件。

## Non-Goals

- **不替代既有 DOM 内容/结构断言层**：像素层为互补层，DOM 层（数值渲染进 DOM + echarts canvas 存在 + 表格行）保留为数据正确性主层。
- **不做跨浏览器矩阵（Firefox/WebKit/移动视口）**：AMIS 主目标为 Chromium，`playwright-e2e-guide.md` 基线为 chromium 单 project；跨浏览器触发条件（需支持非 Chromium 时）未满足，归独立 successor。
- **不新增数值断言**：数值正确性由既有 `*.value.spec.ts` 层覆盖；像素层不断言数值。
- **不改生产代码/ORM/契约/种子**：纯测试层 + Playwright 配置 + 文档；若 Phase 1 发现像素 diff 揭示真实前端缺陷，开显式 successor（不在本计划即时修）。
- **不追求 100% 像素零 diff**：容差（`maxDiffPixelRatio`）+ mask 动态区域为既定手段，接受受控残差。

## Task Route

- Type: `verification or audit work`（新增像素级截图测试层 + Playwright 配置 + 文档；纯测试层，零生产契约变更预期）。
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构/运行命令/已知限制，既有）、`docs/design/dashboards.md`（§实现约定，既有）。
- Skill Selection Basis: 纯 Playwright 浏览器层测试 + AMIS DOM 结构（判定 mask 区域）——`nop-testing` 路由目标 `e2e-testing.md` 不存在（2246-1 裁决先例：E2E 覆盖为空），故 E2E 测试本体 `Skill: none`；AMIS 页面区域结构（识别需 mask 的动态元素）参考 `nop-frontend-dev` 路由文档但不写平台页面代码。Phase 1 若发现像素 diff 揭示前端缺陷需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: 测试在根 `tests/e2e/` 非 reactor 模块；`playwright.config.ts` 变更属测试基础设施；不改 ORM/契约/`_gen/`；任何前端生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- **与 2010-1 的关系（非硬前置）**：2010-1（SPC/CRP 图表）先落地可使像素基线一次捕获含新图表的完整态；若 2010-1 未落地，本计划基线捕获既有态，2010-1 落地后增量重录对应域（quality/mfg）基线。两计划无硬依赖，可独立推进；排序 N=1→N=2 仅为减少基线重录。
- 字体固化方案（Phase 1 裁定）：候选 a) Playwright `page.addStyleTag` 注入 web-font 指向本地固化字体；候选 b) `channel: 'chrome'` + 系统字体固化；候选 c) 接受系统字体 + 容差吸收。Phase 1 实测选定。

## Execution Plan

### Phase 1 - Explore：像素基线可行性裁决（代表性看板）

Status: completed
Targets: `tests/e2e/visual/dashboards.visual.spec.ts`（finance 参数化 + master-data 非参数化代表）、`playwright.config.ts`、`tests/e2e/visual/_helper.ts`
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: none

- [x] `Proof`：在 finance（参数化，含 echarts）+ master-data（非参数化）两代表看板上实测 `toHaveScreenshot` 跨 3 次新鲜运行的 diff 稳定性——分别测 (a) 裸 toHaveScreenshot（基线噪声水平）、(b) + 字体固化方案、(c) + 动态区域 mask（日期/时间戳/用户名/canvas 动画末态 `waitForFunction` 或 echarts `animation: false`）、(d) + `maxDiffPixelRatio` 容差调参。记录每组合的跨次 diff 像素率与稳定性。
  - Skill: none
  - **实测证据**（`tests/e2e/visual/_exploration/`）：
    - 探索 spec：`snapshot-feasibility.exploration.spec.ts`（finance + master-data × 4 变体 = 8 张基线）+ `snapshot-feasibility.measure.spec.ts`（5 关键变体强测：`maxDiffPixels: 0` 最严格口径）。
    - 跨 3 次新鲜浏览器上下文运行：run-1（基线捕获，预期 fail 仅写盘）→ run-2（默认 threshold=0.2 全 pass）→ run-3（全 pass）。
    - 强测结果（`_exploration-measurements.json` run-2，`maxDiffPixels: 0`）：finance-v-a-bare / finance-v-b-font / finance-v-c-mask / master-data-v-a-bare / master-data-v-b-font **全部 0 diff pixels（exact match）**——跨次 pixel-exact。
    - 结论：macOS + Chrome（channel: 'chrome'）+ 系统 PingFang/Apple 系字体栈在同一 OS/浏览器版本下跨次渲染像素级一致；echarts canvas 经 `waitForLoadState('networkidle') + waitForTimeout(1500)` 等待动画末态后亦 pixel-exact。
- [x] `Decision`：据实测裁决像素基线**可行性**——可行（受控残差 + 稳定基线）/ 不可行（跨次 diff 不可控，字体/canvas 动画根因不可消）。若可行，记录选定方案（字体固化 + mask 清单 + 容差阈值）写入计划 + `_helper.ts`/`playwright.config.ts`。若不可行，Phase 2/3 跳过，转 Phase 3 不可行收口路径。
  - Skill: none
  - **裁决：可行**（cross-run pixel-exact 已证，Phase 2/3 推进）。
  - **选定方案**：
    - **字体固化**：`page.addStyleTag` 注入显式字体链 `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif`（防御未来环境漂移，本期实测中系统字体本已稳定）。
    - **mask 清单**：echarts `canvas`（防御未来 echarts 动画时序/canvas 绘制跨环境漂移）+ 各页 `header` 区域（防御用户名/头像动态文本漂移）。报表 `renderHtml` 输出含 `${NOW()}`/日期戳的容器亦 mask。
    - **容差**：`maxDiffPixelRatio: 0.01`（1%）——本期实测 0 diff，1% 容差作为 CI 环境次像素抗锯齿漂移的 belt-and-suspenders，不掩盖真实布局/样式回归（CSS 错位/重叠/canvas 塌缩远 > 1%）。
    - **动画末态等待**：`page.waitForLoadState('networkidle') + waitForTimeout(1500)`（echarts 默认动画 1s，1500ms 覆盖末态 + 容余）。
    - **threshold**：默认 0.2（per-pixel 颜色差异容忍，Playwright 默认，不变更）。

Exit Criteria:

- [x] 可行性裁决结论（含实测数据：每组合跨次 diff 像素率）记录入计划；若可行，选定方案（字体固化/mask 清单/容差）明确。
- [x] 若不可行，根因明确（字体/canvas 动画/其他）+ 不可行证据记录。（N/A —— 裁决为可行）

---

### Phase 2 - 像素基线建立（条件执行：Phase 1 裁决可行）

Status: completed
Targets: `tests/e2e/visual/dashboards.snapshot.spec.ts`（新）、`tests/e2e/visual/reports.snapshot.spec.ts`（新，代表性报表子集）、`tests/e2e/visual/_helper.ts`、`playwright.config.ts`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 裁决可行

- [x] `Add`：`_helper.ts` 固化 `assertSnapshot(page|locator, name, {mask})` 原语（封装字体固化注入 + mask 动态区域 + 容差 + echarts 动画完成等待）；`playwright.config.ts` 增 `expect.toHaveSnapshot` 默认 `maxDiffPixelRatio`（Phase 1 选定阈值）。
  - Skill: none
  - 落地：`_helper.ts:1-99` 增 `assertSnapshot(page, opts)` 原语（字体固化 addStyleTag + echarts settle networkidle+1500ms + canonical mask `[header, canvas]` + `maxDiffPixelRatio: 0.01` + 可选 opts.mask/skipFontHardening/skipEchartsSettle）；`playwright.config.ts:36-44` 增 `expect.toHaveScreenshot.maxDiffPixelRatio: 0.01` 全局默认。
- [x] `Add`：`dashboards.snapshot.spec.ts` 为 10 域看板建立 `toHaveSnapshot` 基线（经 `assertSnapshot`，mask 动态区域，复用既有 `dashboards.visual.spec.ts` 的 page 驱动 + GraphQL 拦截范式使数据确定性）。
  - Skill: none
  - 落地：`tests/e2e/visual/dashboards.snapshot.spec.ts`（10 域 finance/sales/purchase/inventory/assets/projects/manufacturing/maintenance/quality/master-data），10 张 baseline 已捕获于 `dashboards.snapshot.spec.ts-snapshots/`。
- [x] `Add`：`reports.snapshot.spec.ts` 为代表性报表子集（4-6 张，覆盖参数化/零参/日期参/字符串参四形态）建立基线。
  - Skill: none
  - 落地：`tests/e2e/visual/reports.snapshot.spec.ts`（6 张覆盖四形态：fin-income-statement 参数化 ID / md-material-price-list + crm-lead-conversion-funnel 零参 / fin-ar-ap-aging 日期参 / cs-ticket-sla-csat-summary 字符串参 / mfg-crp-load 数值参表重型），6 张 baseline 已捕获于 `reports.snapshot.spec.ts-snapshots/`。
- [x] `Proof`：新增 snapshot spec `--workers=1` 跨 3 次新鲜运行全绿（基线稳定性证明）+ visual 全套件回归 0 新增失败。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts tests/e2e/visual/reports.snapshot.spec.ts --workers=1`（×3 跨次稳定性）+ visual 全套件抽样回归
  - Skill: none
  - 实测：run-1（baseline 写盘，预期 fail）→ run-2（16/16 PASS）→ run-3（16/16 PASS）；visual 全套件回归（dashboards.visual + reports.visual + dashboards.snapshot + reports.snapshot）50/50 PASS，0 新增失败。

Exit Criteria:

- [x] 10 看板 + 代表性报表子集 snapshot 基线建立，跨 3 次新鲜运行稳定（0 意外 diff）。
- [x] visual 全套件回归 0 新增失败；`assertSnapshot` 原语 + 容差范式固化。

---

### Phase 3 - CI 集成约定 + 文档 + Deferred 收口

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/plans/2026-07-09-1249-2-*.md`/`2026-07-09-2330-2-*.md`/`2026-07-09-1728-1-*.md`/`2026-07-09-0930-3-*.md`/`2026-07-09-1045-2-*.md`/`2026-07-09-1145-1-*.md` Deferred 段、`docs/logs/2026/07-17.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2（或 Phase 1 不可行裁决）

- [x] `Add`：`docs/testing/e2e-runbook.md` 增「像素级截图视觉回归层」段（字体固化/mask 清单/容差/基线更新流程/已知限制）。
  - Skill: none
  - 落地：`docs/testing/e2e-runbook.md` 在「报表 AMIS 前端渲染层」段后插入「像素级截图视觉回归层 E2E」段（可行性裁决依据 + `assertSnapshot` 范式 + mask 清单 + 基线更新流程 + 已知限制），分层运行表新增「像素截图基线套件」行。
- [x] `Add`（Phase 1 可行路径，**按 bundle 异质分流**）：`1249-2` 纯像素基线 Deferred 整体补 `**RELEASED by 2026-07-17-2010-2**`；`2330-2`/`1728-1`/`0930-3`/`1045-2`/`1145-1` 五处 bundle **仅**对其「像素截图子集」补 `**像素截图子集 RELEASED by 2026-07-17-2010-2**`，显式注明 bundle 内「报表下载产物字节级 diff」（0204-1 仅交付二进制有效性回归层，字节级 diff 仍 open optimization candidate）/「跨浏览器矩阵」（本计划 Non-Goal）子集**不**在本次 RELEASE、触发条件不变。
  - Skill: none
  - 落地：六处 Deferred 段均已更新（1249-2:l135 整体 RELEASED；2330-2:l141、1728-1:l223、0930-3:l154、1045-2:l139、1145-1:l172 仅像素子集 RELEASED + bundle 内非像素子集显式保留开放）。
- [x] `Add`（Phase 1 不可行路径）：六处 Deferred 的像素截图子集更新触发条件为「当 nop-entropy / AMIS 支持 canvas 动画禁用 / 字体固化可控时」并标注本计划裁决证据（对齐 2330-1 不可行范式）；不强行 RELEASED。
  - Skill: none
  - N/A —— Phase 1 裁决**可行**，本项不可行路径不触发（Phase 1 可行路径项已承接所有六处 Deferred 的像素截图子集 RELEASED）。
- [x] `Add`：`docs/logs/2026/07-17.md` 增聚合条目（可行性裁决/基线数/验证状态/范围纪律）。
  - Skill: none
  - 落地：`docs/logs/2026/07-17.md` 顶部新增 2010-2 聚合条目（背景/Phase 1 裁决 + 实测/Phase 2 基线 + 稳定性证明/Phase 3 文档 + Deferred 分流/验证状态/范围纪律）。

Exit Criteria:

- [x] e2e-runbook「像素级截图视觉回归层」段落地；六处 Deferred 的像素截图子集据可行性路径 RELEASED（1249-2 整体 / 其余 5 处仅像素子集）或触发条件更新；bundle 内非像素子集显式保留开放；日志条目在位。

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_090004140ffecNWBZcX5KjLOh5`，新会话冷重播无执行者/起草者上下文，2026-07-17) — 实时核实：零 `toHaveSnapshot/toHaveScreenshot`、chromium 单 project 无字体固化/容差/mask、AGENTS.md/project-context.md:34 含「看板运行时视觉/浏览器回归」重点、2330-1 设 Explore-first 不可行先例均 PASS。**B1** 「六处同形 Deferred」失实（实为异质 bundle：1249-2 纯像素 / 2330-2·1728-1 像素+跨浏览器 bundle / 0930-3·1045-2·1145-1 像素+下载 diff+跨浏览器 triple-bundle 血缘溯 0637-1）， blanket RELEASE 会假标「报表下载 diff」（范围外）与「跨浏览器」（Non-Goal）为已交付，违反 R13/R5——已重写 Source/Work Item 标异质 bundle、Phase 3 改为按 bundle 分流（1249-2 整体 RELEASE / 其余 5 处仅像素子集 RELEASE + bundle 内非像素子集显式保留开放）；**M1** 触发第二支「已满足」过誉（既有 DOM 层已部分满足重点，像素级为深化非新需求）——已改「argued-as-met」+ Phase 1 框架为「重新审视 1249-2 当年 defer-now 裁决」（类比 2330-1 重新审视 1249-1）；**m1** 补「$var 缺陷经 1728-1 修复后基线捕获正确态」；**m2** Phase 3 触发条件更新项 `Fix`→`Add`（doc-edit 非缺陷修复）；**m3** Related 1606-2 Deferred 标「已解除，历史血缘」。范围裁决 legitimately warranted（像素层捕获 DOM 层结构盲区，非 padding；Explore-first 不可行出口为诚实纪律）。
- Independent draft review iteration 2: **accept** (`ses_08ff90f30ffe372M4yW7rxA4y1`，新会话冷重播无执行者/起草者上下文，2026-07-17) — 实时核实六处源 plan Deferred 段，iteration-1 五项（B1 异质 bundle 分流 + M1 触发 argued-as-met + m1 $var 已修复 + m2 触发更新项 Add + m3 1606-2 历史血缘）全部 RESOLVED。Explore-first 条件结构（Phase 1 Decision/Proof → Phase 2 Prereqs「Phase 1 裁决可行」→ Phase 3 Prereqs「Phase 2 或 Phase 1 不可行裁决」）内部一致；Source/Goals/Phase 3/Closure Gates 在 split-RELEASE 语义上一致；R13 合规（无静默降级、无误标范围外子集已交付）。两非阻塞 Minor 已修订：m-new-1 Phase 3 聚合 Item Types `Add | Fix`→`Add`（全部 Phase 3 item 均为 Add）；m-new-2 0204-1 括注精确化（二进制有效性回归层 vs 字节级 diff 仍 open）。范围裁决 legitimately warranted（像素层捕获 DOM 层结构盲区，Explore-first 不可行出口为诚实纪律，非 padding）。**共识达成 → Plan Status 置 `active`。**

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为测试层 + Playwright 配置 + 文档（预期零生产契约变更）。结束时运行新增 snapshot spec + visual 回归。

- [x] 范围内行为完成（Phase 1 可行性裁决落地；若可行，10 看板 + 代表性报表子集像素基线建立 + 跨次稳定）
- [x] 相关文档对齐（e2e-runbook 像素层段、六处 Deferred 的像素截图子集 RELEASED/触发条件更新且 bundle 内非像素子集显式保留开放、当日日志）
- [x] 已运行验证：新增 snapshot spec `--workers=1` 跨 3 次全绿 + visual 全套件回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认零后端污染）；若 Phase 1 不可行，验证 = 不可行裁决证据 + 跳过路径记录
- [x] 无范围内项目降级为 deferred/follow-up（若 Phase 1 裁决不可行为明确裁决路径非静默降级；跨浏览器矩阵为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果）。执行期确认后分类。

### 跨浏览器矩阵（Firefox/WebKit/移动视口）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: AMIS 主目标为 Chromium，`playwright-e2e-guide.md` 基线为 chromium 单 project；六处 Deferred 同源裁定。跨浏览器为不同结果面（测试执行环境，非断言深度）。
- Successor Required: `yes`（触发条件：需支持非 Chromium 浏览器或移动端看板时）

### Phase 1 可能揭示的前端布局缺陷

- Classification: `watch-only residual`（执行期方可判定是否存在）
- Why Not Blocking Closure: Phase 1 像素 diff 可能揭示真实前端布局/样式缺陷（CSS 错位/重叠/canvas 塌缩）。本计划为测试层，不即时修生产代码。
- Successor Required: `yes`（触发条件：Phase 1/2 实测发现确认前端缺陷时，立即开 successor 承接）

## Closure

Status Note: 全部 3 阶段完成并运行时验证通过（2026-07-17）。Phase 1 Explore 在 finance（参数化+echarts）+ master-data（非参数化）× 4 变体实测跨 3 次新鲜浏览器上下文 `maxDiffPixels: 0` 全部 0 diff pixels（exact match），裁决**可行**——重新审视并推翻 1249-2 当年「像素 diff 信号弱、defer-now」裁决（在新条件下：macOS + Chrome channel:chrome + 系统字体 + echarts 末态等待已可保证 cross-run pixel-exact）。Phase 2 落地 `assertSnapshot` 原语 + 10 看板 + 6 代表性报表（覆盖四参数形态）`toHaveScreenshot` 基线，跨 3 次新鲜运行全绿 + visual 全套件 50/50 PASS 0 新增失败 + 154 模块 BUILD SUCCESS。Phase 3 e2e-runbook 增「像素级截图视觉回归层」段 + 六处 Deferred 按 bundle 异质分流（1249-2 整体 RELEASED / 其余 5 处仅像素截图子集 RELEASED + bundle 内报表下载字节级 diff·跨浏览器矩阵子集显式保留开放触发条件不变）。结束审计由独立子代理执行（见下方）。

Closure Audit Evidence:

- Auditor / Agent: ses_independent-closure-audit-2026-07-17-2010-2（独立 general 子代理，新会话冷重播无执行者/起草者上下文）— VERDICT: PASS，8/8 Gates 通过 + R13 bundle-heterogeneous split 6/6 合规 + 范围纪律通过（零生产代码变更）；新增 snapshot spec 独立复跑 16/16 PASS；MINOR：既存 reports.visual DOM 层（mfg-crp-load/ast-depreciation/ast-disposal）在全量 50 串行负载下有 flakiness（隔离复跑 3/3 PASS，非本计划引入，配置变更仅 toHaveScreenshot 域不触及 DOM 断言）

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
