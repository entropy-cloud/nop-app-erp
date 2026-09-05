# 2026-09-03-0400-3 M2.3 报表像素断言扩面

> Plan Status: active
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

Status: planned
Targets: `tests/e2e/visual/_helper.ts`、本计划（选型矩阵附录节）
Skill: nop-testing

- Item Types: `Decision | Add`（Decision-heavy）
- Prereqs: 无（roadmap Deps 已满足）

- [ ] Decision: 「全 50 spec.ts 扩像素层」口径精确化——像素断言单位 = **AMIS 报表页面（24 个，与 reports.visual 24 域 1:1）**。裁决理由：(a) 页面是唯一像素语义承载单位——24 个 smoke spec 经 `runReportSmoke` 驱动的正是这 24 个页面（1:1 已枚举，逐 spec 重复断言只会复制基线不新增覆盖面）；(b) 24 个 value spec 经 `page.request.post` 直调后端绕过 AMIS 渲染（runbook §报表 AMIS 前端渲染层），无浏览器渲染态可断言；(c) 2 个 download spec 是二进制产物层（roadmap M2.3 Non-Goal 明示不做字节级 diff）。替代方案 rejected：逐 spec 盲目转像素 → 26 个非页面 spec（24 value + 2 download）无渲染态或属排除层。**残留风险**：roadmap/M0.1 的「50 spec」表述在 owner doc 中维持原文直至本计划 Phase 3 回写勘误注记；M2.4 的「26 看板 spec」表述存在同型代理计数陷阱，回写时一并登记提示
      - Skill: nop-testing
- [ ] Decision: 选型矩阵——全 24 报表页面逐行列 参数形态（参数化 | 零参）/ 快照态数（参数化 2 态：默认参 + 参数切换；零参 1 态）/ mask 区域（`${NOW()}` 日期戳容器等）；参数切换态的切换参数沿 reports.visual `fill`/`fillDates`/`pickFluxDate` 既有确定性填充范式；矩阵落本计划附录节。**残留风险**：mask 区域过宽可能掩盖真实布局回归——由独立 plan-audit（本 Phase）+ M0.3 §4 人工核查边界约束
      - Skill: nop-testing
- [ ] Add: `_helper.ts` 新增 `assertReportPixelSnapshot`（只增不改；内部复用 `assertSnapshot` 范式，`skipEchartsSettle=true` 默认——报表页无 canvas，与既有 6 报表基线采集方式一致）
      - Skill: nop-testing
- [ ] Proof: 独立 plan-audit——新增 mask 区域全集（视觉 mask 保护区域）经独立子代理（fresh session）审查，批准记录落盘本计划 Draft Review Record
      - Skill: none

Exit Criteria:

- [ ] 口径裁决记录在案（24 页面像素单位 + 26 个非新增表面 spec 的归因：smoke 1:1 / value 绕过渲染 / download 排除层）
- [ ] 选型矩阵 24 行落盘（页面/参数形态/快照态数/mask 四列完整），总基线数 = Σ快照态数（预期 24~48，其中 6 既有保留）
- [ ] `assertReportPixelSnapshot` 存在且 `npx playwright test --list` 无类型错误
- [ ] mask plan-audit 批准记录在案（含 task id）

### Phase 2 — 扩面落地 + ast-disposal 数值 token 升级

Status: planned
Targets: `tests/e2e/visual/reports.snapshot.spec.ts`（或新文件 `reports-all.snapshot.spec.ts`，Phase 1 矩阵裁决记录）、`tests/e2e/reports/ast-disposal.value.spec.ts`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 按矩阵为 24 页面落地像素断言（既有 6 页保留原基线，新页面走 `assertReportPixelSnapshot`）；分 2~3 批落地，每批本批 spec 全绿后进下一批
      - Skill: nop-testing
- [ ] Add: `ast-disposal.value.spec.ts` 升级——结构性 token（标题）→ 确定性数值 token 断言（消费 M1.2c Deferred，种子 2 行金额/状态确定性）；**同步清理文件头过期注释**（「E2E seeded DB has NO disposal rows / lacks erp_ast_disposal.csv」自 M1.2c 后失实）；同步落地 ast-disposal 报表页像素基线（数据态渲染）
      - Skill: nop-testing
- [ ] Proof: ast-disposal 日期参数确定性核验——确认空/缺省日期区间经 `buildAssetDisposalDetailDataset` 返回冻结的 2026-06/07 种子行；若返回空集则像素态与数值断言一律经 `fillDates` 确定性锚定 startDate/endDate（禁 NOW 锚定缺省值），核验结论落矩阵行
      - Skill: nop-testing
- [ ] Proof: 每批验证 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/reports.snapshot.spec.ts --workers=1`（`--update-snapshots` 仅首批采集）+ `npx playwright test tests/e2e/reports/ast-disposal.*.spec.ts` 全绿
      - Skill: nop-testing

Exit Criteria:

- [ ] 矩阵全部行均有像素断言且 spec 全绿；基线文件数与矩阵 Σ快照态数对账一致
- [ ] ast-disposal value spec 数值 token 断言全绿（空数据/全零即失败的强断言语义达成）

### Phase 3 — 稳定性三连跑 + owner doc 回写 + 合规声明

Status: planned
Targets: `docs/testing/e2e-runbook.md`、本计划 Draft Review Record、`docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md` 不回写（消费登记在本计划）
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [ ] Proof: 3 次新鲜浏览器上下文运行全绿（M0.3 §3 判据）；漂移先诊断后按 M0.3 §4 双面协议重录并声明
      - Skill: nop-testing
- [ ] Proof: `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 最新基线行零漂移
      - Skill: none
- [ ] Add: e2e-runbook.md 回写——像素级段计数更新（6 → 全 24 报表页面）+ R3.2 缺口表 ast-disposal 行更新为已闭合（升级证据链接本计划）+ **口径勘误注记**：「50 spec 转像素」精确化为 24 AMIS 报表页面单位（M2.4「26 看板 spec」同型代理计数陷阱提示一并登记）
      - Skill: none
- [ ] Add: Draft Review Record 追加快照重录合规声明段——触发源 = 新增基线清单 + ast-disposal value spec 期望值变更（DOM 层）；`git diff --stat tests/e2e/visual/**-snapshots/` 对账 = 仅新增文件、零既有基线修改
      - Skill: none

Exit Criteria:

- [ ] 三连跑全绿证据落本计划
- [ ] runbook 两处（像素级段计数 + R3.2 表）与实仓一致
- [ ] 合规声明段在案且对账闭合（含 value spec 期望值变更的 DOM 面声明）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_f9c40685fffeE7I47ZOHW6Breq，2026-09-03）——2 Major：(1) Current Baseline 算术错误（50 = 24 smoke + **24** value + 2 download，原写 22）；(2) 口径裁决理由含错误前提（smoke spec 经 `runReportSmoke` 真实驱动报表页面，非「无渲染态」）——裁决结论（24 页面像素单位）经审查判定忠实于 roadmap 意图非缩水，仅理由重述。4 Minor（plan-audit 项缺类型、Decision 缺残留风险、「按需」措辞、ast-disposal 过期头注释）。全部并入本 v2：计数修正 24/24/2；裁决理由重述为三段准确归因（smoke 1:1 / value 绕过 AMIS / download 排除层）；plan-audit 类型化 `Proof`；两个 Decision 补残留风险；「按需」改矩阵锚定；ast-disposal 过期注释清理 + 日期参数确定性核验项新增；Phase 3 增 M2.4 口径陷阱勘误注记。
- Independent draft review iteration 2: acceptable（独立子代理 ses_f9c35dca7ffeCAV4JMDzDtT1BM，2026-09-03，0 Blocker / 0 Major；2 项非阻塞 cosmetic：Deferred Classification 值 `consumed by this plan` 沿 M1.4a 先例保留、Phase 2 Targets 文件措辞已锚定 Phase 1 矩阵裁决）——两项 Major 修复逐项实仓复核确认（24/24/2 计数 + 三段准确归因理由），4 Minor 与 iteration-1 NOTES 两项（日期确定性核验 + M2.4 口径陷阱注记）全部落实，未引入新问题。共识达成，计划可执行。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物（执行时规则 7）。

- [ ] 范围内行为完成（24 页面像素层 + ast-disposal 升级）
- [ ] 相关文档对齐（e2e-runbook.md 像素级段 + R3.2 表）
- [ ] 已运行验证：`npx playwright test tests/e2e/visual/ --workers=1` 全绿（三连跑）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项；已确认的缺陷不得出现在此处>
