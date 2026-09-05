# 2026-09-03-0400-2 M2.2 业务动作像素断言扩面

> Plan Status: active
> Last Reviewed: 2026-09-03
> Source: docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md M2.2（mission comprehensive-test-data-and-visual-coverage）
> Related: docs/plans/2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md（像素基线范式）；docs/plans/2026-09-01-0301-2-m03-visual-methodology-codification.md（M0.3 方法论 §7 helper 扩展规则）；docs/plans/2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion.md（同批 N=1，确立对话框/drawer 采集模式）
> Audit: required

## Current Baseline

（实仓核验 2026-09-03）

- Roadmap M0.1 / M0.2 / M0.3 全部 `done` + M1.x 11 项 seed 扩面全部 `done` → M2.2 Deps（M0.3 + M1.x 按域相关性）全部满足。
- `tests/e2e/business-actions/` 共 117 spec.ts + 1 `_helper.ts`（18 域，runbook §业务动作浏览器层；三原语范式 + 自包含 setup/cleanup 纪律成熟），覆盖 approve / submit / post / reverse / confirm / cancel / reopen / close 等高频 `@BizMutation` 路径。
- **业务动作对话框/抽屉/Toast 像素层零覆盖**：对话框宽度、按钮排布、拒绝弹窗/Toast 位置是 DOM 断言结构盲区；117 个 spec 全部断言 GraphQL 响应与实体状态，无一张像素基线。
- `tests/e2e/visual/_helper.ts`：`assertSnapshot`（L79）+ M0.3 §7 helper 扩展规则在位；`assertBusinessActionPixelSnapshot` 尚不存在——由本计划新增。
- seed 基线：M1.x 完成后全域种子可见（368 CSV + 1 SQL），为「零状态变更采集模式」提供确定性输入面（种子行打开对话框/触发守卫拒绝，不落库）。
- 剩余差距：业务动作视觉态 0 基线；helper 子集缺失。

## Goals

- 新增 `assertBusinessActionPixelSnapshot` 像素层 helper 子集（只增不改既有函数）。
- 新增业务动作像素 spec：30~50 个代表性 mutation 路径的视觉基线，覆盖三种采集模式——(A) 确认对话框/抽屉打开态、(B) 执行后列表/详情态、(C) 拒绝弹窗/Toast 渲染存在性（M0.1 边界：仅渲染存在断言，不校验样式细节，与负向视觉断言 Non-Goal 不冲突）。
- 选型覆盖 18 域中高频动作类别（approve / submit / post / reverse / confirm / cancel / reopen / close）。
- mask 区域按 M0.1 时机标准 + M0.3 手法落地；新增 mask 区域全集经 plan 内独立 plan-audit。
- 基线 3 次新鲜运行全绿；既有全部视觉 spec（含本批 N=1 新增 CRUD 像素层）零漂移——写路径套件不污染种子驱动基线。
- owner doc（e2e-runbook.md）业务动作视觉段回写 + 快照重录合规声明落 Draft Review Record。

## Non-Goals

- 全部 117 spec 逐路径像素化（代表性抽样 30~50 路径，M0.1 裁决口径）。
- 负向态样式细节断言（M0.1 Non-Goal；模式 C 仅断言渲染存在）。
- 跨浏览器矩阵（2010-2 Non-Goal 沿用）。
- 修改既有 `assertSnapshot` / DOM 层 helper / business-actions `_helper.ts` 既有函数（M0.3 §7 只增不改；setup 复用为 import 调用，不改签名）。
- seed CSV / ORM / page.yaml / view.xml / 生产代码变更。
- useWorkflow（xwf）审批轴（runbook §useWorkflow 可行性裁决：浏览器层不可行，既有 117 spec 已排除，本计划沿同一排除集）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/testing/e2e-runbook.md`（§视觉方法论、§视觉断言扩面边界、§业务动作浏览器层）
- Skill Selection Basis: `nop-testing`（主——Playwright E2E 规范、业务动作调用范式、快照纪律）+ `nop-frontend-dev`（辅——对话框/抽屉/Toast 的 AMIS/flux 渲染结构定位）。

## Infrastructure And Config Prereqs

- app 实例：`./scripts/start-app.sh`（fresh-DB 重置，:8011，flux 渲染）；`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1`；`E2E_ENGINE` 缺省即 flux。
- Playwright Chromium（`channel: 'chrome'`）；基线平台 macOS（`-chromium-darwin` 后缀）。
- 模式 B 路径涉及 mutation 执行——沿用 business-actions 既有自包含 setup/cleanup 纪律（cleanup 后零残留，种子驱动基线零污染）；无新增端口/环境变量/密钥。

## Execution Plan

### Phase 1 — 采集模式裁决 + 选型矩阵 + helper + mask plan-audit

Status: planned
Targets: `tests/e2e/visual/_helper.ts`、本计划（选型矩阵附录节）
Skill: nop-testing + nop-frontend-dev

- Item Types: `Decision | Add`
- Prereqs: N=1 计划（M2.1）落地为佳（复用其对话框/drawer 采集与 mask 处置先例）；最低 prereq = Phase 1 独立可执行

- [ ] Decision: 三种采集模式裁决——(A) 对话框/抽屉打开态：点动作按钮 → 确认对话框渲染 → 截图 → 取消（零状态变更，优先用种子行驱动）；(B) 执行后态：自包含 setup（沿 business-actions 既有范式轻量化，不做凭证行数值断言级深度，**行编码确定性生成、禁嵌 `nanoTime`/`millis` 类时变值**）→ 执行 mutation → 截列表/详情态 → cleanup；(C) 拒绝弹窗/Toast 渲染存在性：守卫路径触发（优先用种子终态行触发非法迁移守卫，零状态变更），等待策略 + Toast 时变区域处置（element 级截图或 mask）记录。**状态安全论证义务**：模式 A/C 逐行核验「按钮对该种子行可见（`visibleOn` 匹配）+ 后端守卫拒绝 + 该路径无 REQUIRES_NEW 类旁路写者（如 `ErpFinPostingExceptionRecorder` 穿透外层事务回滚的先例）」，核验结论落矩阵行；三模式适用判据与论证落本计划
      - Skill: nop-testing
- [ ] Decision: 选型矩阵——18 域代表性 mutation 路径 30~50 条，逐行含 域 / 实体 / 动作 / 模式(A|B|C) / 前置数据源（种子行 | 自包含 setup）/ mask 区域；模式 B ≥3 域、模式 C ≥8 路径；矩阵落本计划附录节
      - Skill: nop-testing
- [ ] Add: `_helper.ts` 新增 `assertBusinessActionPixelSnapshot`（只增不改；内部复用 `assertSnapshot` 范式，支持模式 C 的等待策略参数）
      - Skill: nop-testing
- [ ] Decision: mask 区域清单——逐矩阵行标注动态区域（Toast 容器 / 时间戳列 / **模式 B 自包含 setup 生成的动态编码列** / 会话身份经 canonical 已覆盖项），处置结论落矩阵
      - Skill: nop-testing
- [ ] 独立 plan-audit：新增 mask 区域全集（视觉 mask 保护区域）经独立子代理（fresh session）审查，批准记录落盘本计划 Draft Review Record
      - Skill: none

Exit Criteria:

- [ ] 三模式裁决记录 + 选型矩阵 30~50 行落盘（域/实体/动作/模式/数据源/mask 六列完整）
- [ ] `assertBusinessActionPixelSnapshot` 存在且 `npx playwright test --list` 无类型错误
- [ ] mask plan-audit 批准记录在案（含 task id）

### Phase 2 — 像素基线分批落地

Status: planned
Targets: `tests/e2e/visual/business-actions.snapshot.spec.ts`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 按矩阵落地全部基线断言（cfg 驱动，setup 优先 import `tests/e2e/business-actions/_helper.ts` 既有函数，不可复用路径自包含轻量 setup）；分 2~3 批落地，每批本批 spec 全绿后进下一批
      - Skill: nop-testing
- [ ] Proof: 每批验证 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/business-actions.snapshot.spec.ts --workers=1`（`--update-snapshots` 仅首批采集）
      - Skill: nop-testing
- [ ] Proof: 模式 B 路径 cleanup 后零残留复核（复跑受影响域既有 business-actions spec 全绿，证成状态机未被像素采集污染）
      - Skill: nop-testing

Exit Criteria:

- [ ] 矩阵全部行均有像素断言且 spec 全绿；基线文件数与矩阵行数对账一致
- [ ] 模式 B 抽样域的既有 business-actions spec 复跑全绿（零状态污染证据）

### Phase 3 — 稳定性三连跑 + 全视觉套件零污染实测 + owner doc 回写

Status: planned
Targets: `docs/testing/e2e-runbook.md`、本计划 Draft Review Record
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [ ] Proof: 3 次新鲜浏览器上下文运行全绿（M0.3 §3 判据）；漂移先诊断后按 M0.3 §4 双面协议重录并声明
      - Skill: nop-testing
- [ ] Proof: 全 visual 目录 spec（既有 23 + N=1 新增 + 本批新增）单轮跑通零意外漂移——证成写路径采集与种子驱动基线互不污染（Phase 1 模式 A/C 零状态变更裁决的实测闭环）
      - Skill: nop-testing
- [ ] Proof: `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 最新基线行零漂移
      - Skill: none
- [ ] Add: e2e-runbook.md 回写——新增「业务动作视觉段」（三模式范式、mask 清单、运行命令、spec 计数）
      - Skill: none
- [ ] Add: Draft Review Record 追加快照重录合规声明段（M0.3 §5 全项）——触发源 = 新增基线清单；双面（DOM + 像素）同步结论；mask 合理性自查结论（交叉引用 Phase 1 独立 plan-audit 批准记录）；`git diff --stat tests/e2e/visual/**-snapshots/` 对账 = 仅新增文件、零既有基线修改
      - Skill: none
- [ ] Add: e2e-runbook.md §业务动作浏览器层段首计数勘误（「18 域 113 spec」→ 实测 117）随业务动作视觉段回写一并落地
      - Skill: none

Exit Criteria:

- [ ] 三连跑 + 全套件零漂移证据落本计划
- [ ] runbook 业务动作视觉段与实仓一致
- [ ] 合规声明段在案且对账闭合

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is（独立子代理 ses_f9c40a8bdffeNnmdnj94op9QY3，2026-09-03，0 Blocker / 0 Major / 4 Minor）——Minor 全部作为非阻塞强化并入本 v2：(1) 模式 C 逐行 UI 可达性核验（`visibleOn` 匹配 + 守卫拒绝）；(2) 模式 B 确定性行编码 + 动态编码列 mask 类目；(3) 状态安全论证纳入 REQUIRES_NEW 旁路写者类；(4) 合规声明补双面同步与 mask 合理性自查（M0.3 §5 全项）。附带登记：runbook §业务动作浏览器层过期计数（113→117）随 Phase 3 回写勘误。
- Independent draft review iteration 2: 不适用——iteration 1 已 acceptable-as-is（共识于 iteration 1 达成）；上述 4 Minor 作为非阻塞强化并入 v2，无范围/语义变更，无需再审查轮次。计划可执行。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物（执行时规则 7）。

- [ ] 范围内行为完成（矩阵全行落地，三模式均有交付）
- [ ] 相关文档对齐（e2e-runbook.md 业务动作视觉段）
- [ ] 已运行验证：`npx playwright test tests/e2e/visual/ --workers=1` 全绿（三连跑）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Linux CI 平台像素基线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 沿 2010-2 macOS 本地基线口径；runbook §CI 集成约定将 CI 像素门禁的平台基线捕获设为启用前置，非本计划结果表面
- Successor Required: `yes`（触发条件：`.github/workflows/e2e.yml` 启用像素层 CI 门禁时，与 N=1/N=3 同一 successor 合并处置）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项；已确认的缺陷不得出现在此处>
