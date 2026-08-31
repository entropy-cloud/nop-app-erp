# 2026-09-01-0301-2-m03-visual-methodology-codification M0.3 视觉方法论固化

> Plan Status: active
> Last Reviewed: 2026-09-01
> Mission: comprehensive-test-data-and-visual-coverage
> Work Item: M0.3 视觉方法论固化——AI 截屏仅诊断不裁决 + mask/重录/命名规范 + helper 扩展规则
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` §Milestone M0 工作项 M0.3（2026-08-31 人工批准转 ready）
> Related: `2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md`（像素基线范式与 6 处 Deferred 血缘源）、`2026-09-01-0301-1-m01-seed-scope-adjudication.md`（弱依赖前置：最终 mask 区域与扩面边界需 M0.1 裁决后回调修订）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-09-01）：

- **像素基线基础设施已落地**（2010-2，completed）：
  - `tests/e2e/visual/_helper.ts`：`SNAPSHOT_FONT_CHAIN` 字体注入常量、canonical mask（AMIS shell header 用户名/头像 + 全部 canvas）、`waitForEchartsSettle`（networkidle + 1500ms 宽限）、统一封装 `assertSnapshot`（:75）、DOM 层 `assertDashboardRendered`（:151）/ `assertReportRendered`（:260）。
  - `playwright.config.ts`：`toHaveScreenshot.maxDiffPixelRatio: 0.01`（:42-43）+ 单 chromium project；webServer 以 JVM flag `nop.web.render-mode=flux` 启动。
  - 既有像素基线：`dashboards.snapshot.spec.ts` + `reports.snapshot.spec.ts`（2 个 snapshot spec）+ `_exploration/` 可行性裁决实证（`maxDiffPixels: 0` 跨次精确一致）。
- **缺失项（本计划差距）**：
  - `_helper.ts` **无「AI 截屏仅诊断不裁决」注释块**（roadmap §横切 2 给定精确文案与位置：import 块下、Pixel-snapshot section comment 之上）。
  - `docs/testing/e2e-runbook.md` 已有 §837「像素级截图视觉回归层」段，但**无「视觉方法论」段**：mask 动态区域标准、跨次重跑稳定性阈值、snapshot 双面重录协议、快照重录合规声明协议、`toHaveScreenshot` 命名锁定、M2.x helper 扩展规则、6 处 Deferred 层叠关系均未成文。
  - **命名漂移实仓证据**：runbook §837 段行文（L839）使用旧别名 `toHaveSnapshot` 且写「共 1 spec / 16 测试」，而实际 Playwright API 与 `_helper.ts:75` 封装均为 `toHaveScreenshot`、snapshot spec 实为 2 个（dashboards + reports）——命名规范锁定须同时勘误该段行文与计数。
  - `docs/design/dashboards.md` **无视觉扩面注记**（roadmap M0.3 owner doc 列要求）。
  - **实仓无 PR 模板**（`.github/PULL_REQUEST_TEMPLATE.md` 不存在）——roadmap 横切 §3/§8 要求「PR 模板 grep 快照重录合规声明段」，其落点载体须裁决。
- **6 处 Deferred 血缘**（2010-2 L7 消费清单）：`2026-07-09-1249-2`（纯像素基线）/ `2026-07-09-2330-2` + `2026-07-09-1728-1`（像素基线 + 跨浏览器 bundle）/ `2026-07-09-0930-3` + `2026-07-09-1045-2` + `2026-07-09-1145-1`（像素视觉回归 + 报表下载产物 diff + 跨浏览器 triple-bundle，血缘溯 0637-1）——M0.3 须在方法论段记录与它们的层叠关系（DOM 主层 + 像素互补层）。
- **下游门控**：roadmap §执行机制 5——M2.x DRAFT_PLANS 须待 M0.3 `done`（latch：`_helper.ts` 注释块 + e2e-runbook.md 视觉方法论段 + `playwright.config.ts` 字体固化配置生效 + `toHaveScreenshot` 命名规范锁定）。

## Goals

- `_helper.ts` 顶部加「AI 截屏仅诊断不裁决」注释块（roadmap 给定文案，位置锁定），固化「像素 diff 信号由 AI 仅作根因诊断；pass/fail 裁决严格遵循 `toHaveScreenshot` 容差结果 + DOM 断言结果」。
- `docs/testing/e2e-runbook.md` 新增「视觉方法论」段：(a) AI 截屏仅诊断不裁决声明；(b) mask 动态区域标准；(c) 跨次重跑稳定性阈值（基于 2010-2 实测）；(d) snapshot 重录协议（DOM + 像素双面同步，禁单面）；(e) 快照重录合规声明协议（含载体裁决）；(f) `toHaveScreenshot` 命名锁定 + §837 旧别名勘误；(g) M2.x helper 扩展规则（仅新增 `assertXxxPixelSnapshot` 子集、不改既有 `assertSnapshot` / `assertXxxRendered`）；(h) 6 处 Deferred 层叠关系；(i) CI 集成约定。
- 核对既有 `playwright.config.ts` + `_helper.ts` 范式与方法论一致，缺失项补齐；证明既有 2 个 snapshot spec 全绿无回归。
- `docs/design/dashboards.md` 增视觉扩面注记。
- 登记 M0.1 回调义务（mask 区域与扩面边界最终值）。

## Non-Goals

- **不新增任何页面/动作/报表/看板的像素断言**——归 M2.1~M2.4。
- **不改既有 helper 函数语义**——`assertSnapshot` / `assertDashboardRendered` / `assertReportRendered` 仅引用不重命名；像素层子集扩展归 M2.x（规则在本次成文）。
- **不引入跨浏览器矩阵、报表下载产物字节级 diff、AI 视觉平台化改造**——roadmap §Non-Goals 沿用。
- **不修改 seed CSV 与 ORM**——本计划不触 seed 资产；「执行期 seed 变更触发双面重录」仅成文为协议，不发生实际重录。
- **不改变渲染模式**——flux-only 强制不变；`playwright.config.ts` webServer 语义不动。

## Task Route

- Type: `implementation-only change`（注释 + 测试基建约定成文 + 文档；无产品行为变更）
- Owner Docs: `docs/testing/e2e-runbook.md` + `docs/design/dashboards.md` + `tests/e2e/visual/_helper.ts`（注释块）
- Skill Selection Basis: roadmap M0.3 行指定 `Skill: none`——注释块与文档成文不构成测试用例开发或页面开发；E2E 运行遵循 `docs/testing/e2e-runbook.md`「E2E 编写规范（强制）」既有规范，不新写测试用例。执行阶段按 AGENTS.md 强制技能加载规则重扫确认（如实际发生 spec 文件修改则须加载 `nop-testing` 并修订本计划）。

## Infrastructure And Config Prereqs

- E2E 验证依赖 `playwright.config.ts` webServer 自启（JVM flag `nop.web.render-mode=flux`，fresh-DB 语义沿 1143-1 `scripts/start-app.sh`）；无新增端口/密钥/外部服务。
- 无数据迁移前置（No infra prereqs beyond existing baseline）。

## Execution Plan

### Phase 1 - 方法论成文与命名锁定

Status: planned
Targets: `tests/e2e/visual/_helper.ts`、`docs/testing/e2e-runbook.md`、`playwright.config.ts`（仅核对，预期零改）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（M0.1 为弱依赖：仅「mask 区域与扩面边界最终值」回调时需要，见 Phase 2 Follow-up）

- [ ] Add: `_helper.ts` 顶部注释块——按 roadmap §横切 2 给定文案与位置（现有 import 块下、`// Pixel-snapshot layer` section comment 之上）：声明所有 `assertSnapshot` / `assertXxxPixelSnapshot` 调用结果为 pass/fail 唯一裁决依据；CI 中 `toHaveScreenshot` 失败由独立子代理 plan-audit 复核根因，不得由 AI 主观判定
      - Skill: none
- [ ] Add: `docs/testing/e2e-runbook.md` 新增「视觉方法论」段（置于 §837 像素级截图视觉回归层段之后），覆盖九要素：AI 截屏仅诊断不裁决 / mask 动态区域标准（日期参数、时间戳、用户名、echarts canvas 动画末态、AMIS 自适应布局断点）/ 跨次重跑稳定性阈值（严格档 `maxDiffPixels: 0` + 宽容档 `maxDiffPixelRatio: 0.01`，依 2010-2 Phase 1 实测）/ snapshot 双面重录协议（DOM + 像素同步，禁单面，`--update-snapshots` 仅 CI 重录 + PR review 人工核查 mask 合理性）/ 快照重录合规声明协议（按 Phase 1 Decision 载体）/ `toHaveScreenshot` 命名锁定 + §837 旧别名 `toHaveSnapshot` 及「共 1 spec / 16 测试」计数勘误（实为 2 spec）/ M2.x helper 扩展规则（仅新增 `assertXxxPixelSnapshot`，不改 `assertSnapshot`，与 DOM 层 `assertXxxRendered` 并列）/ 6 处 Deferred 层叠关系（DOM 主层 + 像素互补层 + 各 bundle 未 RELEASE 子集去向）/ CI 集成约定（字体固化 + mask + 容差 + flux webServer + `playwright.config.ts` 不引入任何 AI 模型/服务调用）
      - Skill: none
- [ ] Decision: 「快照重录合规声明」载体裁决——实仓无 `.github/PULL_REQUEST_TEMPLATE.md`。候选：(A) 创建最小 PR 模板含「快照重录合规声明」必填段（与 roadmap 横切 §3「PR 模板 grep」字面一致）；(B) 以各 plan `Draft Review Record` 内强制「快照重录合规声明」段 + runbook 协议为载体（声明审查已由 plan EXECUTE 阶段独立子代理承担，roadmap 横切 §8 同款机制）。记录选择、替代方案、残留风险于 runbook 方法论段
      - Skill: none
- [ ] Proof: 核对既有范式与方法论一致——`playwright.config.ts` 字体/容差配置 + `_helper.ts` 字体链/canonical mask/echarts settle/flux webServer 逐项对照九要素；预期零缺失（字体/容差/mask/settle 均已在位，见 Current Baseline）；若发现缺失，不得静默修改——在该项下登记显式子项（缺失点 + 建议修正）后作为独立 Add 执行并在计划中记录理由；运行既有 2 个 snapshot spec 确认全绿无回归（命令按 runbook §837 段既有运行方式，如 `npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts tests/e2e/visual/reports.snapshot.spec.ts`）
      - Skill: none

Exit Criteria:

- [ ] `_helper.ts` 注释块落位正确且 `git diff` 显示仅新增注释（无逻辑变更）
- [ ] runbook「视觉方法论」段九要素齐备；§837 段旧别名与「共 1 spec」计数勘误完成；合规声明载体裁决已记录（含替代方案与残留风险）
- [ ] 既有 `dashboards.snapshot.spec.ts` + `reports.snapshot.spec.ts` 全绿（exit 0；失败则先诊断是否本计划引入，非本计划引入的失败按 e2e-runbook 渲染模式三路径排查并登记）

### Phase 2 - 看板 owner doc 注记与回调义务登记

Status: planned
Targets: `docs/design/dashboards.md`
Skill: none

- Item Types: `Add | Follow-up`
- Prereqs: Phase 1（注记引用方法论段锚点）

- [ ] Add: `docs/design/dashboards.md` 增「视觉扩面注记」小节——指向 runbook「视觉方法论」段与 §837 像素层段，说明 10 域看板像素断言扩面（M2.4）将遵循该方法论；不复制规范正文（单一真相源在 runbook）
      - Skill: none
- [ ] Follow-up: M0.1 回调义务登记——runbook 方法论段与 `docs/design/dashboards.md` 注记中显式登记：「mask 区域与扩面边界的最终值待 M0.1 裁决（plan 2026-09-01-0301-1）落地后回调修订；回调触发条件 = M0.1 完成」。本计划收口不等待该回调（roadmap M0.3 Deps 列明示弱依赖可并行）
      - Skill: none

Exit Criteria:

- [ ] `docs/design/dashboards.md` 注记落地且仅引用不复制规范正文
- [ ] 回调义务在 runbook 方法论段与注记两处均有显式触发条件文字

## Draft Review Record

- Independent draft review iteration 1: accept (独立子代理 ses_fa6c8a550fferRAGPcz0dzgIuy) because 0 Blocker + 0 Major；2 Minor 已在转 active 前内联修复：(1) Proof 项「发现缺失即补齐」开放授权改为「不得静默修改——登记显式子项后独立 Add 执行，预期零缺失」；(2) §837 勘误范围扩展至「共 1 spec / 16 测试」失实计数（实为 2 spec），基线 / Add 项 / Exit Criteria 三处同步。依赖处理（M0.1 弱依赖可并行 + 回调义务三处登记触发条件）与保护区域纪律（仅注释新增，git diff 门控）经审查确认合规。

## Closure Gates

> 本计划含一处 `_helper.ts` 注释新增（无逻辑变更）+ 文档成文；验证以「既有 snapshot spec 全绿 + 注释 diff 仅注释」为门控，不要求全量 `mvn test`（无 Java/模型变更）；按 roadmap §规则 9 保留 compliance checker 复跑。

- [ ] 范围内行为完成（Phase 1 / Phase 2 全部执行项 `[x]`）
- [ ] 相关文档对齐（runbook / dashboards.md / roadmap §横切 2/3/8 无矛盾；`toHaveScreenshot` 命名全仓文档段一致性抽检）
- [ ] 已运行验证：既有 2 个 snapshot spec 全绿 + `bash docs/audits/nop-compliance-checker.sh` 对照 `known-good-baselines.md` 零漂移
- [ ] 无范围内项目降级为 deferred/follow-up（M0.1 回调义务为显式 Follow-up 且带触发条件，非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### mask 区域与扩面边界最终值回调

- Classification: `watch-only residual`
- Why Not Blocking Closure: roadmap M0.3 Deps 列明示弱依赖可并行——M0.3 规范本身（九要素）不依赖 M0.1 裁决，仅最终值需回调；回调已登记为带触发条件的 Follow-up
- Successor Required: `no`（回调由本计划产出段承载，M0.1 完成后原位修订即可，无需新 plan；若回调引发方法论结构性变更则升级为独立修订）

## Closure

Status Note: <待结束后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- M0.1 裁决落地后回调 mask 区域与扩面边界最终值（触发条件 = plan 2026-09-01-0301-1 完成；原位修订 runbook 方法论段 + dashboards.md 注记）
