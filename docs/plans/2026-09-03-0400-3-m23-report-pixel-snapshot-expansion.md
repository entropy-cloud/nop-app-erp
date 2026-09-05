# 2026-09-03-0400-3 M2.3 报表像素断言扩面

> Plan Status: completed（2026-09-06，独立结束审计 approve `ses_f8c17ee48ffec26okAkXJjq4OF`；三连跑 3 × 30 passed + 目录级 4 snapshot 套件全绿 + compliance R2c=1542 零漂移 + mvn 全 reactor BUILD SUCCESS）
> Last Reviewed: 2026-09-03
> Source: docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md M2.3（mission comprehensive-test-data-and-visual-coverage）
> Related: docs/plans/2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md（reports.snapshot 范式源头）；docs/plans/2026-09-01-0301-2-m03-visual-methodology-codification.md（M0.3 方法论）；docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md（disposal 数值 token Deferred 源）；docs/plans/2026-09-01-0527-2-report-flux-browser-assertion-drift-fix.md（M2.3 前置阻断已解除）
> Audit: required

## Current Baseline

（实仓核验 2026-09-03）

- Roadmap M0.1 / M0.2 / M0.3 全部 `done` + M1.x 11 项 seed 扩面全部 `done` → M2.3 Deps（M0.3 + M1.x 有报表域）全部满足。
- `tests/e2e/visual/reports.snapshot.spec.ts` 像素层现状 = **6 代表性报表**（fin-income-statement / md-material-price-list / crm-lead-conversion-funnel / fin-ar-ap-aging / cs-ticket-sla-csat-summary / mfg-crp-load，2010-2 建基线）；`reports.visual.spec.ts` DOM 层已覆盖**全 24 报表页面**（fin 5 / mfg 3 / ast 2 / crm 2 / hr 2 / mnt 2 / prj 2 / qa 2 / md 2 / inv 1 / cs 1，0527-2 修复后全绿）。
- `tests/e2e/reports/` 实仓 50 spec.ts：24 smoke + 24 value + 2 download（`reports.download.spec.ts` / `reports.amis-download.spec.ts`）——与 24 报表页面的关系见 Phase 1 口径裁决（smoke 与页面 1:1；value 绕过 AMIS 渲染；download 为二进制产物层）。
- 前置阻断已解除：runbook 登记的 reports 浏览器断言层 7 处预存失败已由 plan 0527-2 修复（30/30 全绿）。
- **待消费 Deferred**：M1.2c 登记「disposal 报表数值 token 断言升级 → M2.x 报表 value 工作」——successor 触发条件「ast-disposal gains E2E seed data」已由 M1.2c 达成（`erp_ast_disposal` 2 行种子在库）；runbook §冒烟层数据存在性约定 R3.2 缺口表 ast-disposal 行同源（「补 seed → 升级为确定性数值 token 断言」）。当前 `ast-disposal.value.spec.ts` 仍为结构性 token（仅标题）。
- `assertReportPixelSnapshot` 尚不存在——由本计划按 M0.3 §7 规则新增。

## Baseline Drift Addendum（2026-09-06 执行期登记，实仓核验）

本计划 Current Baseline（2026-09-03 写就）与执行时点实仓存在漂移，按「快照型断言引用前 grep 实仓重验」纪律（`docs/lessons/13-requirement-baseline-staleness.md`）逐项重验并裁决如下：

- **像素层已由平行 successor 扩至 24 页 × 1 态**：successor 草案计划 `2026-09-04-1721-1-m2-visual-pixel-assertion-expansion`（M2.1 Batch D/E + M2.2 前置批 + M2.3 +18 + M2.4 +9 打包）于 2026-09-05（commit `7d30c2f1b`）将 `reports.snapshot.spec.ts` 从 6 扩至 **24 页面 × 1 快照态**（直接经 `assertSnapshot` + `skipEchartsSettle: true`，与本计划 2010-2 范式一致；基线 PNG 24 张入库）。**影响裁决**：本计划 Phase 2「为 24 页面落地像素断言」的主体面已由 1721-1 交付；本计划增量收缩为**状态扩面**——按 Goals 的 2 态口径补齐缺省参渲染态（见 Appendix A 矩阵「M2.3 增量」列，+7 基线）；「既有 6 页保留原基线」改读「既有 24 页保留原基线」（24 张既有 PNG 零修改，快照重录合规声明对账口径不变：仅新增、零既有修改）。
- **`assertReportPixelSnapshot` 仍不存在**（roadmap L145「已有该子集」表述与实仓不符——1721-1 未落 helper；本计划 Phase 1 Add 照常执行，亦即 roadmap 细则段该句由本计划闭合后成立）。
- **ast-disposal value spec 过期头注释仍在**（M1.2c Deferred 消费未由 1721-1 承载）——Phase 2 照常执行。
- **e2e-runbook.md §像素级段已被 1721-1 刷新为 24**（`reports.snapshot.spec.ts | 24 | 2010-2 首建 6 + M2.3 扩面 +18` 行）——Phase 3 回写改为增量刷新（24 → 31 基线计数 + 本计划登记 + R3.2 闭合 + 口径勘误注记）。
- 其余基线事实（`tests/e2e/reports/` 50 spec 构成、0527-2 阻断解除、M1.2c 种子 2 行在库）实仓复核无漂移。

## Goals

- `_helper.ts` 新增 `assertReportPixelSnapshot` 像素层 helper 子集（只增不改既有函数）。
- 报表像素层从 6 代表性报表扩至**全 24 报表页面**：参数化报表 2 态（默认参渲染态 + 参数切换态）、零参报表 1 态（M0.1「列表态 + 参数切换态 + 导出 HTML 态」选型口径，渲染态即导出 HTML 注入容器像素）。
- 消费 M1.2c Deferred：`ast-disposal.value.spec.ts` 升级为确定性数值 token 断言（种子 2 行，金额/日期静态确定性）+ ast-disposal 报表页像素基线（数据态渲染）。
- mask 区域按 M0.1 时机标准 + M0.3 手法落地，mask 区域全集以 Phase 1 选型矩阵为准（报表 `${NOW()}` 日期戳容器等）；新增 mask 区域全集经 plan 内独立 plan-audit。
- 基线 3 次新鲜运行全绿；既有视觉 spec 零漂移。
- owner doc 回写：e2e-runbook.md（报表视觉段计数 + R3.2 缺口表 ast-disposal 行闭合）+ 快照重录合规声明落 Draft Review Record。

## Non-Goals

- 报表下载产物**字节级** diff（0204-1 裁决 open optimization candidate；roadmap M2.3 明示仅像素层；download 2 spec 维持二进制魔数 + token 回归层现状）。
- 负向视觉断言（M0.1 Non-Goal）。
- 跨浏览器矩阵（2010-2 Non-Goal 沿用）。
- 修改既有 `assertSnapshot` / `assertDashboardRendered` / `assertReportRendered` / 既有 6 报表基线的范式（新页面走新 helper；既有基线仅在确证合法漂移时按 M0.3 §4 双面协议重录并声明）。
- seed CSV / ORM / 报表 `.xpt.xml` / page.yaml 内容变更（纯测试资产计划）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/testing/e2e-runbook.md`（§视觉方法论、§视觉断言扩面边界、§报表 AMIS 前端渲染层、§像素级截图视觉回归层、§冒烟层数据存在性约定）
- Skill Selection Basis: `nop-testing`（主——Playwright E2E 规范、报表渲染断言范式、快照纪律）+ `nop-frontend-dev`（辅——报表 page.yaml 参数表单结构理解）。

## Infrastructure And Config Prereqs

- app 实例：`./scripts/start-app.sh`（fresh-DB 重置，:8011，flux 渲染）；`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1`；`E2E_ENGINE` 缺省即 flux。
- Playwright Chromium（`channel: 'chrome'`）；基线平台 macOS（`-chromium-darwin` 后缀）。
- 无新增端口/环境变量/密钥；无 seed/模板/生产代码变更。

## Execution Plan

### Phase 1 — 口径裁决 + 选型矩阵 + helper + mask plan-audit

Status: completed
Targets: `tests/e2e/visual/_helper.ts`、本计划（选型矩阵附录节）
Skill: nop-testing

- Item Types: `Decision | Add`（Decision-heavy）
- Prereqs: 无（roadmap Deps 已满足）

- [x] Decision: 「全 50 spec.ts 扩像素层」口径精确化——像素断言单位 = **AMIS 报表页面（24 个，与 reports.visual 24 域 1:1）**。裁决理由：(a) 页面是唯一像素语义承载单位——24 个 smoke spec 经 `runReportSmoke` 驱动的正是这 24 个页面（1:1 已枚举，逐 spec 重复断言只会复制基线不新增覆盖面）；(b) 24 个 value spec 经 `page.request.post` 直调后端绕过 AMIS 渲染（runbook §报表 AMIS 前端渲染层），无浏览器渲染态可断言；(c) 2 个 download spec 是二进制产物层（roadmap M2.3 Non-Goal 明示不做字节级 diff）。替代方案 rejected：逐 spec 盲目转像素 → 26 个非页面 spec（24 value + 2 download）无渲染态或属排除层。**残留风险**：roadmap/M0.1 的「50 spec」表述在 owner doc 中维持原文直至本计划 Phase 3 回写勘误注记；M2.4 的「26 看板 spec」表述存在同型代理计数陷阱，回写时一并登记提示 → 裁决落 Appendix A.1（执行期以 Baseline Drift Addendum 适配：24 页 × 1 态主体面已由 successor 1721-1 交付，本计划增量收缩为状态扩面）
      - Skill: nop-testing
- [x] Decision: 选型矩阵——全 24 报表页面逐行列 参数形态（参数化 | 零参）/ 快照态数（参数化 2 态：默认参 + 参数切换；零参 1 态）/ mask 区域（`${NOW()}` 日期戳容器等）；参数切换态的切换参数沿 reports.visual `fill`/`fillDates`/`pickFluxDate` 既有确定性填充范式；矩阵落本计划附录节。**残留风险**：mask 区域过宽可能掩盖真实布局回归——由独立 plan-audit（本 Phase）+ M0.3 §4 人工核查边界约束 → 矩阵落 Appendix A.2/A.3（Σ=30；5 页两态塌缩裁决 + ast-disposal 零参等效裁决；mask 全集 canonical-only）
      - Skill: nop-testing
- [x] Add: `_helper.ts` 新增 `assertReportPixelSnapshot`（只增不改；内部复用 `assertSnapshot` 范式，`skipEchartsSettle=true` 默认——报表页无 canvas，与既有 6 报表基线采集方式一致）→ 已落地（`ReportPixelSnapshotOptions` + `assertReportPixelSnapshot`，镜像 M2.1 `assertCrudPixelSnapshot` 先例；plan-audit 双迭代确认 `git diff` 纯增量）
      - Skill: nop-testing
- [x] Proof: 独立 plan-audit——新增 mask 区域全集（视觉 mask 保护区域）经独立子代理（fresh session）审查，批准记录落盘本计划 Draft Review Record → iteration 1 needs-revision（`ses_f8cc10eb6ffejPz96CmQK54OEf`，1 Major 修正 Σ 31→30）→ iteration 2 **approve**（`ses_f8cb882b4ffeGRE5QBi7wn1WZM`）
      - Skill: none

Exit Criteria:

- [x] 口径裁决记录在案（24 页面像素单位 + 26 个非新增表面 spec 的归因：smoke 1:1 / value 绕过渲染 / download 排除层）（Appendix A.1）
- [x] 选型矩阵 24 行落盘（页面/参数形态/快照态数/mask 四列完整），总基线数 = Σ快照态数（预期 24~48，其中 6 既有保留）（Appendix A.3，Σ=30=既有 24 + 新增 6，含 plan-audit M-1 修正）
- [x] `assertReportPixelSnapshot` 存在且 `npx playwright test --list` 无类型错误（2026-09-06 `--list` 25 tests in 2 files 零错误）
- [x] mask plan-audit 批准记录在案（含 task id）（Draft Review Record iteration 2 approve `ses_f8cb882b4ffeGRE5QBi7wn1WZM`）

### Phase 2 — 扩面落地 + ast-disposal 数值 token 升级

Status: completed
Targets: `tests/e2e/visual/reports.snapshot.spec.ts`（或新文件 `reports-all.snapshot.spec.ts`，Phase 1 矩阵裁决记录）、`tests/e2e/reports/ast-disposal.value.spec.ts`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 按矩阵为 24 页面落地像素断言（既有 6 页保留原基线，新页面走 `assertReportPixelSnapshot`）；分 2~3 批落地，每批本批 spec 全绿后进下一批 → Baseline Drift Addendum 适配：既有 24 页 × 1 态已由 successor 1721-1 交付（PNG 零修改保留），本计划落地缺省参渲染态 +6（分 2 批各 3：b1 fin-ar-ap-aging / mfg-crp-load / mnt-downtime-summary，b2 prj-timesheet-detail / cs-ticket-sla-csat-summary / crm-forecast-accuracy；经 `assertReportPixelSnapshot`，基线名 `<label>-report-default.png`；每批 `--update-snapshots` 采集后 3 passed，两批后全 spec 30/30 绿）
      - Skill: nop-testing
- [x] Add: `ast-disposal.value.spec.ts` 升级——结构性 token（标题）→ 确定性数值 token 断言（消费 M1.2c Deferred，种子 2 行金额/状态确定性）；**同步清理文件头过期注释**（「E2E seeded DB has NO disposal rows / lacks erp_ast_disposal.csv」自 M1.2c 后失实）；同步落地 ast-disposal 报表页像素基线（数据态渲染）→ 9 token 断言（DSP-2026-001/002 + 500.00 + 2026-07-10/12 + SOLD/SCRAPPED + DRAFT/CANCELLED）落地、过期头注释重写为 M1.2c 消费登记；数据态像素基线 = 既有 `ast-asset-disposal-detail-report.png`（缺省空日期 = 全域 2 行数据态，A.2-2 裁决，零修改）
      - Skill: nop-testing
- [x] Proof: ast-disposal 日期参数确定性核验——确认空/缺省日期区间经 `buildAssetDisposalDetailDataset` 返回冻结的 2026-06/07 种子行；若返回空集则像素态与数值断言一律经 `fillDates` 确定性锚定 startDate/endDate（禁 NOW 锚定缺省值），核验结论落矩阵行 → 主分支成立：代码层 `loadDisposals(null,null)` 零过滤（`ErpAstReportBizModel.java:337`）+ 实仓 `/r/ErpAstReport__renderHtml` 实跑返回 2 行冻结种子（500.00/2026-07-10/DRAFT 等全部在渲染 HTML）；结论落矩阵行 15（A.2-2）
      - Skill: nop-testing
- [x] Proof: 每批验证 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/reports.snapshot.spec.ts --workers=1`（`--update-snapshots` 仅首批采集）+ `npx playwright test tests/e2e/reports/ast-disposal.*.spec.ts` 全绿 → 每批采集 3 passed ×2；`--update-snapshots` 移除后全 spec 30 passed（4.2m，fresh-DB runner）；ast-disposal value+smoke 2 passed
      - Skill: nop-testing

Exit Criteria:

- [x] 矩阵全部行均有像素断言且 spec 全绿；基线文件数与矩阵 Σ快照态数对账一致（`reports.snapshot.spec.ts-snapshots/` 30 PNG = Σ30 = 既有 24（`git diff` 零修改）+ 新增 6 untracked）
- [x] ast-disposal value spec 数值 token 断言全绿（空数据/全零即失败的强断言语义达成）（2 passed，种子 2 行 token 全命中）

### Phase 3 — 稳定性三连跑 + owner doc 回写 + 合规声明

Status: completed
Targets: `docs/testing/e2e-runbook.md`、本计划 Draft Review Record、`docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md` 不回写（消费登记在本计划）
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] Proof: 3 次新鲜浏览器上下文运行全绿（M0.3 §3 判据）；漂移先诊断后按 M0.3 §4 双面协议重录并声明 → **3 × 30 passed**（2026-09-06，fresh-DB runner `:8011`，各 4.2m，零漂移零重录）
      - Skill: nop-testing
- [x] Proof: `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 最新基线行零漂移 → exit 0，R2c=1542 与 2026-09-04 M3.1 基线行精确一致（本计划零生产代码变更）
      - Skill: none
- [x] Add: e2e-runbook.md 回写——像素级段计数更新（6 → 全 24 报表页面）+ R3.2 缺口表 ast-disposal 行更新为已闭合（升级证据链接本计划）+ **口径勘误注记**：「50 spec 转像素」精确化为 24 AMIS 报表页面单位（M2.4「26 看板 spec」同型代理计数陷阱提示一并登记） → 已落地：构成表行 24→30（含 1721-1 归属勘误 +6 归因）、§套件表 175 测试/173 PNG、新增「报表像素层（M2.3）」小节（范式/矩阵/mask/命令/基线平台镜像 M2.1/M2.2 小节）、§视觉断言扩面边界报表行勘误注记（含 M2.4 陷阱提示）、R3.2 ast-disposal 行 ✅ 已闭合（seed 归 M1.2c + 升级归本计划）
      - Skill: none
- [x] Add: Draft Review Record 追加快照重录合规声明段——触发源 = 新增基线清单 + ast-disposal value spec 期望值变更（DOM 层）；`git diff --stat tests/e2e/visual/**-snapshots/` 对账 = 仅新增文件、零既有基线修改 → 「快照重录合规声明」段已落本计划（触发源 2 项 + 对账 30=24+6 零修改闭合）
      - Skill: none

Exit Criteria:

- [x] 三连跑全绿证据落本计划（3 × 30 passed 实测记录见 Phase 3 首项）
- [x] runbook 两处（像素级段计数 + R3.2 表）与实仓一致（30 PNG 实盘对账 + R3.2 行闭合证据链接）
- [x] 合规声明段在案且对账闭合（含 value spec 期望值变更的 DOM 面声明）

## Appendix A — 口径裁决 + 选型矩阵（Phase 1 交付，2026-09-06 实仓锚定）

### A.1 口径裁决：像素断言单位 = AMIS 报表页面（24 个，与 reports.visual 24 域 1:1）

`tests/e2e/reports/` 全 50 spec.ts 不逐 spec 转像素。裁决理由（三段归因）：

1. **24 个 smoke spec 经 `runReportSmoke` 驱动的正是这 24 个页面（1:1 已枚举）**——页面是唯一像素语义承载单位，逐 smoke spec 重复断言只会复制同页基线、不新增覆盖面；
2. **24 个 value spec 经 `page.request.post`/GraphQL 直调后端**，绕过 AMIS/flux 渲染（runbook §报表 AMIS 前端渲染层），无浏览器渲染态可断言；
3. **2 个 download spec（`reports.download.spec.ts` / `reports.amis-download.spec.ts`）是二进制产物层**（roadmap M2.3 Non-Goal 明示不做字节级 diff）。

替代方案 rejected：逐 spec 盲目转像素 → 26 个非页面 spec（24 value + 2 download）无渲染态或属排除层。

**残留风险登记**：roadmap/M0.1 的「50 spec」表述在 owner doc 中维持原文直至 Phase 3 回写勘误注记；M2.4 的「26 看板 spec」表述存在同型代理计数陷阱，Phase 3 一并登记提示。

### A.2 快照态口径裁决（2 态语义的实仓塌缩）

页面级 2 态定义（M0.1 选型口径）：**缺省参渲染态**（page.yaml `filterForm` 缺省值驱动的初始渲染）+ **参数切换态**（经 `fill`/`fillDates`/`pickFluxDate` 确定性填充后的重渲染）。实仓裁决两点（含 plan-audit iteration 1 修正，见 Draft Review Record）：

1. **5 页两态塌缩为 1 态**（page.yaml 缺省 = 唯一 seeded 填充值，既有 fill 为 no-op，既有基线即两态同像）：fin 4 页（income-statement / balance-sheet / cash-flow / period-close-report，`periodId` 缺省 `value: 1`，种子库仅 1 个会计期间 `erp_fin_accounting_period` ID=1，reports.visual 既有确定性填充范式即 `fill: { periodId: '1' }`）+ **hr-payroll-simulation-comparison（`simulationId` 缺省 `value: 1`，plan-audit iteration 1 Major M-1 修正——执行初版矩阵误归 2 态桶）**。全仓 sweep 证实恰好这 5 个报表 page.yaml 携带 `value:` 缺省。切换到未 seed 的 ID=2 只产生无数据空表渲染，不构成有意义的状态面，rejected。
2. **ast-disposal 归零参等效单态**：page.yaml 仅有 startDate/endDate 两个 input-date（无缺省值）；`buildAssetDisposalDetailDataset(null, null)` 经 `loadDisposals` 空区间不过滤 → 返回冻结 2 行种子（DSP-2026-001/002，金额/日期/状态静态确定性；2026-09-06 经 `/r/ErpAstReport__renderHtml` 实跑复核：`500.00` / `2026-07-10` / `DRAFT` / `CANCELLED` 等全部在渲染 HTML 中）。禁 NOW 锚定前提成立（page.yaml 无 `${NOW()}` 残留，0527-2 已清）。数据态像素基线 = 既有 `ast-asset-disposal-detail-report.png`（即缺省全域 2 行数据态渲染）。

### A.3 选型矩阵（24 行；快照态总数 Σ = 30，M2.3 增量 +6）

mask 列全集 = **零附加 `opts.mask`，仅 canonical `header`/`canvas`**（报表页无 canvas 时为无害 no-op）：0527-2 已移除 page.yaml `${NOW()}` 缺省（plan-audit 实证全 25 个报表 page.yaml 零 `${NOW()}`）；`.xpt.xml` 模板无渲染时间戳/当前时间引用（plan-audit 全模板 grep 零命中）；报表页 DOM = 静态 filterForm + 种子冻结 renderHtml 表格，会话可变区仅 shell header（canonical mask 覆盖）。既有 24 张基线稳定性实证：唯一采集 commit `7d30c2f1b` 后零修改（采集期 2026-09-04 基线运行全绿）；本计划 Phase 3 三连跑为预期增量实证。逐行按 M0.1 §mask 时机标准判定无「日期参数/时间戳/用户名（header 外）/canvas 动画」类视口内动态区域。

| # | 页面（reportLabel / route） | 参数形态 | page.yaml 缺省 | 快照态数 | mask | 既有基线态（锚定来源） | M2.3 增量 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | fin-income-statement / `/income-statement` | ID 参数 periodId | `value: 1` | 1（两态塌缩，A.2-1） | canonical | 缺省≡切换态（2010-2） | — |
| 2 | fin-balance-sheet / `/balance-sheet` | ID 参数 periodId | `value: 1` | 1（塌缩） | canonical | 同上（1721-1） | — |
| 3 | fin-cash-flow / `/cash-flow` | ID 参数 periodId | `value: 1` | 1（塌缩） | canonical | 同上（1721-1） | — |
| 4 | fin-period-close-report / `/period-close-report` | ID 参数 periodId | `value: 1` | 1（塌缩） | canonical | 同上（1721-1） | — |
| 5 | fin-ar-ap-aging / `/ar-ap-aging` | 日期参数 asOfDate | 无（0527-2 移除） | 2 | canonical | 切换态 `2026-07-08`（2010-2） | +缺省态（空=全域） |
| 6 | mfg-crp-load / `/crp-load-report` | number workcenterId + 可选日期 | 无 | 2 | canonical | 切换态 `1`（2010-2） | +缺省态（全域） |
| 7 | mnt-downtime-summary / `/downtime-summary` | number equipmentId + 可选日期 | 无 | 2 | canonical | 切换态 `1`（1721-1） | +缺省态（全域） |
| 8 | prj-timesheet-detail / `/timesheet-detail` | number projectId + 可选日期 | 无 | 2 | canonical | 切换态 `1`（1721-1） | +缺省态（全域） |
| 9 | cs-ticket-sla-csat-summary / `/ticket-sla-csat-summary` | text ticketType | 无 | 2 | canonical | 切换态 `1`（2010-2） | +缺省态（全类型） |
| 10 | crm-forecast-accuracy / `/forecast-accuracy` | number forecastId | 无 | 2 | canonical | 切换态 `1`（1721-1） | +缺省态（全域） |
| 11 | hr-payroll-simulation-comparison / `/payroll-simulation-comparison` | number simulationId | `value: 1` | 1（塌缩，A.2-1；plan-audit M-1 修正） | canonical | 缺省≡切换态（1721-1） | — |
| 12 | mfg-production-variance / `/production-variance-report` | 零参（可选过滤留空） | — | 1 | canonical | 缺省态（1721-1） | — |
| 13 | mfg-forecast-variance / `/forecast-variance-report` | 零参等效（可选 materialId/期间留空） | — | 1 | canonical | 同上（1721-1） | — |
| 14 | ast-asset-depreciation-detail / `/asset-depreciation-detail` | 零参等效（可选 categoryId/日期留空） | 无 | 1 | canonical | 同上（1721-1） | — |
| 15 | ast-asset-disposal-detail / `/asset-disposal-detail` | 零参等效（可选日期留空=全域 2 行，A.2-2） | 无 | 1 | canonical | 同上（1721-1） | — |
| 16 | mnt-maintenance-history / `/maintenance-history` | 零参等效（可选 equipmentId/日期留空） | — | 1 | canonical | 同上（1721-1） | — |
| 17 | prj-project-cost-summary / `/project-cost-summary` | 零参（可选过滤留空） | — | 1 | canonical | 同上（1721-1） | — |
| 18 | qa-inspection-summary / `/inspection-summary` | 零参等效（可选 materialId/日期留空） | — | 1 | canonical | 同上（1721-1） | — |
| 19 | qa-ncr-capa-summary / `/ncr-capa-summary` | 零参等效（可选日期留空） | — | 1 | canonical | 同上（1721-1） | — |
| 20 | md-material-price-list / `/material-price-list` | 零参等效（可选 materialCode 留空） | — | 1 | canonical | 同上（2010-2） | — |
| 21 | md-partner-list / `/partner-list` | 零参等效（可选 partnerType 留空） | — | 1 | canonical | 同上（1721-1） | — |
| 22 | inv-inventory-trace / `/inventory-trace-report` | 零参等效（表单参数后端仅认 moveId，留空） | — | 1 | canonical | 同上（1721-1） | — |
| 23 | crm-lead-conversion-funnel / `/lead-conversion-funnel` | 零参 | — | 1 | canonical | 同上（2010-2） | — |
| 24 | hr-employee-net-balance / `/employee-net-balance` | 零参 | — | 1 | canonical | 同上（1721-1） | — |

对账：Σ快照态数 = 5×1（塌缩，A.2-1 含 plan-audit M-1 修正的 hr 页）+ 6×2（空缺省参数页：fin-ar-ap-aging / mfg-crp-load / mnt-downtime-summary / prj-timesheet-detail / cs-ticket-sla-csat-summary / crm-forecast-accuracy）+ 13×1（零参/零参等效）= **30** = 既有 24（零修改）+ 新增 6（M2.3）。落点 spec：既有 24 测试留在 `reports.snapshot.spec.ts` 原位不动，+6 缺省态测试同文件追加（经 `assertReportPixelSnapshot`），基线命名 `<reportLabel>-report-default.png` 与既有 `<reportLabel>-report.png` 正交。

### A.4 范围外观察（非本计划处置）

`crm/pages/report/campaign-attribution.page.yaml` 存在（crm 域第 3 个报表页面），但无 smoke/value/visual 任何 E2E 覆盖，不在 reports.visual 24 页 1:1 枚举内——不属本计划口径单位（24 = 与 reports.visual 1:1），登记为覆盖面缺口观察，归 roadmap M1.x/M2.x 后续批次裁决，不计入本计划 Σ。

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_f9c40685fffeE7I47ZOHW6Breq，2026-09-03）——2 Major：(1) Current Baseline 算术错误（50 = 24 smoke + **24** value + 2 download，原写 22）；(2) 口径裁决理由含错误前提（smoke spec 经 `runReportSmoke` 真实驱动报表页面，非「无渲染态」）——裁决结论（24 页面像素单位）经审查判定忠实于 roadmap 意图非缩水，仅理由重述。4 Minor（plan-audit 项缺类型、Decision 缺残留风险、「按需」措辞、ast-disposal 过期头注释）。全部并入本 v2：计数修正 24/24/2；裁决理由重述为三段准确归因（smoke 1:1 / value 绕过 AMIS / download 排除层）；plan-audit 类型化 `Proof`；两个 Decision 补残留风险；「按需」改矩阵锚定；ast-disposal 过期注释清理 + 日期参数确定性核验项新增；Phase 3 增 M2.4 口径陷阱勘误注记。
- Independent draft review iteration 2: acceptable（独立子代理 ses_f9c35dca7ffeCAV4JMDzDtT1BM，2026-09-03，0 Blocker / 0 Major；2 项非阻塞 cosmetic：Deferred Classification 值 `consumed by this plan` 沿 M1.4a 先例保留、Phase 2 Targets 文件措辞已锚定 Phase 1 矩阵裁决）——两项 Major 修复逐项实仓复核确认（24/24/2 计数 + 三段准确归因理由），4 Minor 与 iteration-1 NOTES 两项（日期确定性核验 + M2.4 口径陷阱注记）全部落实，未引入新问题。共识达成，计划可执行。
- Mask/matrix plan-audit iteration 1: needs-revision（独立子代理 fresh session，task `ses_f8cc10eb6ffejPz96CmQK54OEf`，2026-09-06 执行日）——1 Major M-1（矩阵行 11 `hr-payroll-simulation-comparison` 误归 2 态桶：page.yaml `simulationId` 缺省 `value: 1`，既有 fill 为 no-op，按 A.2-1 塌缩逻辑应归 1 态桶；Σ 由 31/+7 修正为 **30/+6**）；2 Minor m-1（mask 前言「本计划三连跑零漂移实证」超前置陈述，改为唯一采集 commit `7d30c2f1b` 零修改 + Phase 3 预期实证）、m-2（行 13/16/20/21 标签统一「零参等效（可选…留空）」）。其余五项确认全部通过：helper 纯增量、24 行 ↔ spec 1:1、fin 4 页塌缩、mask=canonical-only 安全（25 page.yaml 零 `${NOW()}` + 模板零时间戳）、A.4 排除正确。
- Mask/matrix plan-audit iteration 2: **approve**（独立子代理 fresh session，task `ses_f8cb882b4ffeGRE5QBi7wn1WZM`，2026-09-06 执行日，0 Blocker / 0 Major / 2 非阻塞 cosmetic）——M-1/m-1/m-2 修复逐项实仓复核确认（行 11 塌缩 + Σ=30 对账 + 6 个 +缺省态行枚举正确 + 独立 sweep 恰好 5 page.yaml 携带 `value:` 缺省）；helper 纯增量与 24↔24 无回归复核通过。2 项非阻塞 cosmetic（行 18 标签统一——已随本记录落实；Draft Review Record 前向引用补齐——即本条）。mask 区域全集（零附加 `opts.mask`，canonical-only，30 快照态）经独立 plan-audit 双迭代批准收敛。

## 快照重录合规声明（M0.3 §快照重录协议，Phase 3 交付）

- **触发源 1（新增基线清单）**：`reports.snapshot.spec.ts` 新增 6 个缺省参渲染态测试（fin-ar-ap-aging / mfg-crp-load / mnt-downtime-summary / prj-timesheet-detail / cs-ticket-sla-csat-summary / crm-forecast-accuracy 的 `-report-default` 基线）——新增覆盖率扩展，非漂移重录；`--update-snapshots` 仅在首批采集经 `-g "default-state"` 限定执行（批 1/批 2 各一次）。
- **触发源 2（value spec 期望值变更，DOM 层）**：`tests/e2e/reports/ast-disposal.value.spec.ts` 结构性 token（标题）→ 确定性数值 token（9 token）——**DOM/API 断言面变更，非像素基线变更**；该 spec 无任何 `toHaveScreenshot` 断言，像素面零关联。
- **对账**：`git status`/`git diff --stat tests/e2e/visual/reports.snapshot.spec.ts-snapshots/` = **仅 6 个新增 untracked PNG、零既有基线修改**（30 PNG = 24 既有 + 6 新增；既有 24 张经 3 连跑逐字节匹配零漂移）。全仓其余 snapshot 基线目录（dashboards/crud-pages/business-actions）零触碰。
- **结论**：本次变更 = 纯新增基线 + DOM 层期望值升级，无既有像素基线重录，合规。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物（执行时规则 7）。

- [x] 范围内行为完成（24 页面像素层 + ast-disposal 升级）→ Baseline Drift Addendum 适配口径：24 页面像素层 = 既有 24 态（1721-1 交付，零修改保留）+ 本计划缺省态 +6 = 30 快照态 ↔ 矩阵 Σ30 对账一致；ast-disposal 数值 token 升级落地全绿
- [x] 相关文档对齐（e2e-runbook.md 像素级段 + R3.2 表）→ 构成表 30 行 + §套件表 + 新增「报表像素层（M2.3）」小节 + §扩面边界勘误注记 + R3.2 ast-disposal 行 ✅ 已闭合
- [x] 已运行验证：`npx playwright test tests/e2e/visual/ --workers=1` 全绿（三连跑）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移 → 实测（2026-09-06）：交付面 reports.snapshot 3 × 30 passed（4.2m/轮，fresh-DB runner）；目录级单轮 340 测试 = **283 passed + 6 skipped + 51 failed**——4 个正式 snapshot 套件全绿（reports 30/30 + dashboards 19/19 + crud-pages 69/69 + business-actions 55 + 2 skipped），51 failed 与 runbook 2026-09-05 刷新登记的「51 预存 AMIS 遗留/feasibility 红灯」同数同族（`_exploration` 8 + AMIS 遗留 DOM 层 43，全部在本计划变更面外，零新增失败）；compliance checker exit 0，R2c=1542 与 2026-09-04 基线行精确一致；另 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + 全 reactor `mvn test` BUILD SUCCESS（app-erp-all 70/0/0/1 与已知基线一致，历史 hr/drp 预存回归已被 successor 修复清零）
- [x] 无范围内项目降级为 deferred/follow-up → 「Linux CI 平台像素基线」为计划就绪期既登记的 out-of-scope improvement（非执行期降级）；A.4 campaign-attribution 为范围外观察登记（不属 24 页口径单位）
- [x] 独立草案审查已完成并记录 → Draft Review Record iteration 1/2（2026-09-03）+ mask/matrix plan-audit iteration 1 needs-revision / iteration 2 approve（2026-09-06，含 task id）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致 → 3 Phase 全 `completed` + 全 checkbox `[x]` + 本门控全 `[x]`；legacy 格式 `> Plan Status:` 已于独立结束审计 APPROVE 后更新为 `completed`（含闭包证据摘要）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符 → 见 Closure Audit Evidence
- [x] 结束证据存在于文件中 → 本节 + 各 Phase 项内联实测记录 + Draft Review Record + 快照重录合规声明段

## Deferred But Adjudicated

### M1.2c Deferred「disposal 报表数值 token 断言升级」

- Classification: `consumed by this plan`（沿 M1.4a 消费登记先例）
- Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务；消费义务由 Phase 2 承载，闭环后 M1.2c 计划文件中的 Deferred 登记由本段 + runbook R3.2 表闭合注记共同对账
- Successor Required: `no`

### Linux CI 平台像素基线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 沿 2010-2 macOS 本地基线口径；CI 门禁平台基线捕获为启用前置，非本计划结果表面
- Successor Required: `yes`（触发条件：`.github/workflows/e2e.yml` 启用像素层 CI 门禁时，与 N=1/N=2 同一 successor 合并处置）

## Closure

Status Note: 计划完成（2026-09-06 执行日）。交付 = `assertReportPixelSnapshot` helper 子集（纯增量）+ 报表像素层缺省参渲染态 +6 基线（30 快照态 ↔ 矩阵 Σ 对账，既有 24 PNG 零修改）+ ast-disposal value spec 数值 token 升级（M1.2c Deferred 消费闭合）+ e2e-runbook 四处回写（构成表/套件表/报表像素层小节/扩面边界勘误 + R3.2 ast-disposal 行闭合）+ 快照重录合规声明。执行期适配 = Baseline Drift Addendum（24 页 × 1 态主体面由 successor 1721-1 于 2026-09-05 先行交付，本计划增量收缩为状态扩面，已登记）。验证 = 三连跑 3 × 30 passed + 目录级 340 测试（4 正式 snapshot 套件全绿、51 failed 与 2026-09-05 登记预存集同数同族、零新增失败）+ compliance R2c=1542 零漂移 + `mvn clean install -DskipTests` / 全 reactor `mvn test` BUILD SUCCESS。M1.2c「disposal 数值 token 断言升级」Deferred 已由 Phase 2 消费闭合（见 Deferred But Adjudicated 段）；「Linux CI 平台像素基线」维持 out-of-scope improvement（successor 触发条件不变）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session），closure audit task id `ses_f8c17ee48ffec26okAkXJjq4OF`（2026-09-06 执行日）
- Evidence: **VERDICT: approve**（0 Blocker / 0 Major / 4 Minor——(1) `_exploration/` 目录级运行诊断产物漂移，已 `git checkout` 还原；(2) 每日开发日志待落，随本闭包提交落地；(3) 本 Closure 段占位符待填，即本条；(4) roadmap 回写待落，随本闭包提交落地）。8 项 Closure Gates 逐项 VERIFIED（文本一致性：3 Phase 全 completed + 30 项 checkbox 全 `[x]`；helper `git diff` 纯增量 +21/−0；spec 30 tests 1:1；基线 30 PNG = 6 untracked + 24 零修改；ast-disposal 9 token；runbook 五处对账；算术 24+6=30 / 19+30+69+57=175 / 283+6+51=340 与 `--list` 340 精确互证；4 轮独立审查 4 个互异 ses_ id 无自审）

Follow-up:

- 无阻塞跟进项。Linux CI 平台像素基线维持既登记 successor（触发条件：`.github/workflows/e2e.yml` 启用像素层 CI 门禁）；A.4 campaign-attribution 覆盖面缺口观察归 roadmap 后续批次裁决。
