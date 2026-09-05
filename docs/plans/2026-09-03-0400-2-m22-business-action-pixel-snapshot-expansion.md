# 2026-09-03-0400-2 M2.2 业务动作像素断言扩面

> Plan Status: completed
> Last Reviewed: 2026-09-06
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

Status: completed
Targets: `tests/e2e/visual/_helper.ts`、本计划（选型矩阵附录节）
Skill: nop-testing + nop-frontend-dev

- Item Types: `Decision | Add`
- Prereqs: N=1 计划（M2.1）落地为佳（复用其对话框/drawer 采集与 mask 处置先例）；最低 prereq = Phase 1 独立可执行

- [x] Decision: 三种采集模式裁决——(A) 对话框/抽屉打开态：点动作按钮 → 确认对话框渲染 → 截图 → 取消（零状态变更，优先用种子行驱动）；(B) 执行后态：自包含 setup（沿 business-actions 既有范式轻量化，不做凭证行数值断言级深度，**行编码确定性生成、禁嵌 `nanoTime`/`millis` 类时变值**）→ 执行 mutation → 截列表/详情态 → cleanup；(C) 拒绝弹窗/Toast 渲染存在性：守卫路径触发（优先用种子终态行触发非法迁移守卫，零状态变更），等待策略 + Toast 时变区域处置（element 级截图或 mask）记录。**状态安全论证义务**：模式 A/C 逐行核验「按钮对该种子行可见（`visibleOn` 匹配）+ 后端守卫拒绝 + 该路径无 REQUIRES_NEW 类旁路写者（如 `ErpFinPostingExceptionRecorder` 穿透外层事务回滚的先例）」，核验结论落矩阵行；三模式适用判据与论证落本计划（裁决全文 + 逐行论证见附录 A/B；2026-09-05 实仓核验：flux `runtime-action-helpers.ts:140-156` confirmText 门——env.confirm 取消即 `createCancelledResult` **不发起任何后端调用**，模式 A 零状态变更有运行时机制级保证；模式 C 守卫全部为状态机/门控首语句抛错（cs `ErpCsTicketStateMachine` / mfg `ErpMfgWorkOrderDocumentStateMachine.assertCanCancel` {DRAFT,SUBMITTED,NOT_STARTED} / crm `ErpCrmLeadStateMachine` qualify 须 NEW / qa NCR CAPA 门），先于任何 ORM 写，无 REQUIRES_NEW 旁路写者）
      - Skill: nop-testing
- [x] Decision: 选型矩阵——18 域代表性 mutation 路径 30~50 条，逐行含 域 / 实体 / 动作 / 模式(A|B|C) / 前置数据源（种子行 | 自包含 setup）/ mask 区域；模式 B ≥3 域、模式 C ≥8 路径；矩阵落本计划附录节（矩阵 35 行 ∈ [30,50] 落附录 A：A 23 行 14 域 + B 4 行 4 域 + C 8 行 4 域；master-data 无业务动作面、b2b `ErpB2bAsn` `<crud name="main"/>` 零自定义动作、cs reopen 后端 mutation 无 UI 行按钮——三处域级排除在矩阵前置节裁决；post 类别无种子可达按钮（种子凭证全 POSTED 态无 DRAFT 行）随 `等` 代表性口径登记残留）
      - Skill: nop-testing
- [x] Add: `_helper.ts` 新增 `assertBusinessActionPixelSnapshot`（只增不改；内部复用 `assertSnapshot` 范式，支持模式 C 的等待策略参数）（落地于 `assertCrudPixelSnapshot` 之后（结束审计勘误：实仓 `_helper.ts:150-168`，原记 `:396` 为陈旧行号）：`waitForSelector`（模式 C toast 出现等待，超时抛错 = 守卫拒绝渲染缺失即失败）+ `waitForSelectorTimeout` + `waitToastGone`（模式 B 成功 toast 消失等待）；`skipEchartsSettle` 缺省 true 委托既有 `assertSnapshot`，与 `assertCrudPixelSnapshot` 同款增量范式，既有 4 helper 零变更）
      - Skill: nop-testing
- [x] Decision: mask 区域清单——逐矩阵行标注动态区域（Toast 容器 / 时间戳列 / **模式 B 自包含 setup 生成的动态编码列** / 会话身份经 canonical 已覆盖项），处置结论落矩阵（结论：全部 35 行 canonical `header`/`canvas` 之外仅两类附加 mask——模式 C 8 行 mask `[data-sonner-toaster]`（toast 消息文案防漂移，`waitForSelector` 仍强制 toast 渲染存在，mask 不削弱存在性断言：元素缺席 = 无遮挡块 = 像素 diff 失败）；模式 B 4 行 mask 新行 `td[data-field="id"]`（seq 生成 id 跨 run 漂移防护，行定位经确定性 code）；其余行经 M2.1 既有几何实证（审计列 x≥2031 视口外 / 对话框覆盖区）零附加 mask，详见附录 A mask 列）
      - Skill: nop-testing
- [x] 独立 plan-audit：新增 mask 区域全集（视觉 mask 保护区域）经独立子代理（fresh session）审查，批准记录落盘本计划 Draft Review Record（iteration 1 APPROVE：独立子代理 ses_f8e02f7c9ffehxBgvGOpzyvMljNMSJXe，2026-09-05，0 Blocker / 0 Major / 2 Minor 已随批准落实，见 Draft Review Record）
      - Skill: none

Exit Criteria:

- [x] 三模式裁决记录 + 选型矩阵 30~50 行落盘（域/实体/动作/模式/数据源/mask 六列完整）（35 行，六列齐备，见附录 A）
- [x] `assertBusinessActionPixelSnapshot` 存在且 `npx playwright test --list` 无类型错误（`npx playwright test tests/e2e/visual/business-actions.snapshot.spec.ts --list` 编译/模块解析通过）
- [x] mask plan-audit 批准记录在案（含 task id）（Draft Review Record iteration 1 APPROVE，ses_f8e02f7c9ffehxBgvGOpzyvMljNMSJXe）

### Phase 2 — 像素基线分批落地

Status: completed
Targets: `tests/e2e/visual/business-actions.snapshot.spec.ts`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 按矩阵落地全部基线断言（cfg 驱动，setup 优先 import `tests/e2e/business-actions/_helper.ts` 既有函数，不可复用路径自包含轻量 setup）；分 2~3 批落地，每批本批 spec 全绿后进下一批（批1 A 23 行 → 批2 B 4 行 → 批3 C 8 行，三批全绿；两处实现级适配登记：① B01 `__save` 补 ORM 必填 `approveStatus: 'UNSUBMITTED'`（镜像 `TestErpInvStockMoveCrudSmoke` save 头字段集）；② m-2 守卫落地为「按 `__save` 返回 seq id 精确文本定位 id 单元格」——运行时探针实证本 flux build 表格 td 无 `data-field` 属性，矩阵原表述 `td[data-field="id"]` 不可匹配，零匹配守卫语义（结构变更响亮失败）不变，见附录 A.2 增补）
      - Skill: nop-testing
- [x] Proof: 每批验证 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/business-actions.snapshot.spec.ts --workers=1`（`--update-snapshots` 仅首批采集）（批1 A 23 passed 3.5m fresh-DB 零漂移；批2 B 4 passed 首采 1.1m；批3 C 8 passed 首采 1.2m；`--update-snapshots` 均以 `-g "m22-B|m22-C"` 限定不触碰既有基线）
      - Skill: nop-testing
- [x] Proof: 模式 B 路径 cleanup 后零残留复核（复跑受影响域既有 business-actions spec 全绿，证成状态机未被像素采集污染）（4 域 spec（inventory-stock-move / maintenance-visit / ct-contract-lifecycle / projects-task）复跑 10 passed 1.3m + GraphQL `__findPage` 直查 4 个 `E2E-BAS-*` code 全部 total=0）
      - Skill: nop-testing

Exit Criteria:

- [x] 矩阵全部行均有像素断言且 spec 全绿；基线文件数与矩阵行数对账一致（35 基线 png ∈ snapshots 目录新增（untracked）= 矩阵 35 行；既有 20 个 F/G/H 批 png `git diff` 零修改；整 spec 单轮 55 passed + 2 skipped（skip 为前置计划 F/G/H 批遗留 stale 种子码 LEAD-001/TK-2026-001 优雅跳过，非本批矩阵行，0 failed））
- [x] 模式 B 抽样域的既有 business-actions spec 复跑全绿（零状态污染证据）（同上 Proof 3：10 passed + total=0 直查证据）

### Phase 3 — 稳定性三连跑 + 全视觉套件零污染实测 + owner doc 回写

Status: completed
Targets: `docs/testing/e2e-runbook.md`、本计划 Draft Review Record
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] Proof: 3 次新鲜浏览器上下文运行全绿（M0.3 §3 判据）；漂移先诊断后按 M0.3 §4 双面协议重录并声明（2026-09-06 三连跑：55 passed + 2 skipped × 3（8.9m/9.0m/8.9m），零漂移零重录；2 skipped 为前置计划 F/G/H 批 stale 种子码遗留，非本批矩阵行）
      - Skill: nop-testing
- [x] Proof: 全 visual 目录 spec（既有 23 + N=1 新增 + 本批新增）单轮跑通零意外漂移——证成写路径采集与种子驱动基线互不污染（Phase 1 模式 A/C 零状态变更裁决的实测闭环）（目录级单轮 1.0h：276 passed + 6 skipped + 44 failed；**4 个 snapshot 套件 167/167 全绿零漂移**（business-actions 55/57 + crud-pages 69 + dashboards 19 + reports 24），即像素资产零意外漂移、污染证伪成立；44 failed 全部为 DOM 层既有预存红灯家族（ext-domains-child-table / f12 / tree-entity / status-tag / sensitive / gl-mapping / field-format / party-search-picker / list-query-filter / f16-complex 同 09-03 四族分账口径）+ 1 个 dashboards.visual KPI 断言； Identical-Failure-Set 对照实验：fresh-DB 重启后 12 个失败 spec 子集在 HEAD（stash 本批变更）与本批工作树两态各跑一轮 = **43=43 逐条同集（仅时长后缀差异）**，dashboards.visual KPI 项 fresh-DB 下通过（马拉松排序污染伪影）——与本批变更零因果，沿 M1.5 IDENTICAL FAILURE SETS 登记先例）
      - Skill: nop-testing
- [x] Proof: `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 最新基线行零漂移（exit 0，R2c=1542 与 2026-09-04 基线行精确一致；本批零 Java/seed/ORM 触碰）
      - Skill: none
- [x] Add: e2e-runbook.md 回写——新增「业务动作视觉段」（三模式范式、mask 清单、运行命令、spec 计数）（§像素级截图视觉回归层下新增「业务动作像素层（M2.2）」节 + 套件构成表 business-actions 行 22→57）
      - Skill: none
- [x] Add: Draft Review Record 追加快照重录合规声明段（M0.3 §5 全项）——触发源 = 新增基线清单；双面（DOM + 像素）同步结论；mask 合理性自查结论（交叉引用 Phase 1 独立 plan-audit 批准记录）；`git diff --stat tests/e2e/visual/**-snapshots/` 对账 = 仅新增文件、零既有基线修改（已落 Draft Review Record §快照重录合规声明（2026-09-06）：35 `??` 新增 / 0 `M` / 0 `D`，零重录触发）
      - Skill: none
- [x] Add: e2e-runbook.md §业务动作浏览器层段首计数勘误（「18 域 113 spec」→ 实测 117）随业务动作视觉段回写一并落地（`ls tests/e2e/business-actions/*.spec.ts | wc -l` = 117 实证）
      - Skill: none

Exit Criteria:

- [x] 三连跑 + 全套件零漂移证据落本计划（三连跑 ×3 零漂移；目录级单轮 4 snapshot 套件 167/167；Identical-Failure-Set 43=43 对照实验；checker R2c=1542 零漂移——证据均内联于本 Phase 各 Proof 项）
- [x] runbook 业务动作视觉段与实仓一致（M2.2 节三模式/mask/命令/计数与 spec 实态逐项对应；57 测试计数与 snapshots 目录 55 文件 + 2 skipped 口径一致）
- [x] 合规声明段在案且对账闭合（Draft Review Record §快照重录合规声明，35 新增/0 修改 git 对账闭合）

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is（独立子代理 ses_f9c40a8bdffeNnmdnj94op9QY3，2026-09-03，0 Blocker / 0 Major / 4 Minor）——Minor 全部作为非阻塞强化并入本 v2：(1) 模式 C 逐行 UI 可达性核验（`visibleOn` 匹配 + 守卫拒绝）；(2) 模式 B 确定性行编码 + 动态编码列 mask 类目；(3) 状态安全论证纳入 REQUIRES_NEW 旁路写者类；(4) 合规声明补双面同步与 mask 合理性自查（M0.3 §5 全项）。附带登记：runbook §业务动作浏览器层过期计数（113→117）随 Phase 3 回写勘误。
- Independent draft review iteration 2: 不适用——iteration 1 已 acceptable-as-is（共识于 iteration 1 达成）；上述 4 Minor 作为非阻塞强化并入 v2，无范围/语义变更，无需再审查轮次。计划可执行。

### 快照重录合规声明（M0.3 §5 全项，2026-09-06）

- **触发源**：本计划新增 35 张业务动作像素基线（模式 A 23 + 模式 B 4 + 模式 C 8，落 `tests/e2e/visual/business-actions.snapshot.spec.ts-snapshots/`，`-chromium-darwin` 平台后缀）；**零既有基线重录**——Phase 2 三批验证中既有 20 张 F/G/H 批 png 与 dashboards/reports/crud-pages 三套件基线全程未以 `--update-snapshots` 触碰（批 2/批 3 首采均以 `-g "m22-B|m22-C"` 限定）。
- **双面（DOM + 像素）同步结论**：像素层 35 张新基线三连跑全绿（55 passed + 2 skipped × 3）；DOM 层模式 C 的 toast 渲染存在性由 `waitForSelector('[data-sonner-toast]')` 强制（缺失抛错）、模式 A 的 surface 关闭由 Escape + hidden 断言闭环——DOM 与像素双面对同一渲染事实，无单面孤证。
- **mask 合理性自查结论**：新增 mask 全集仅两类（模式 C 8 行 `[data-sonner-toaster]` + 模式 B 4 行新行 seq id 单元格），均已随 Phase 1 独立 plan-audit 批准（iteration 1 APPROVE，`ses_f8e02f7c9ffehxBgvGOpzyvMljNMSJXe`，0 Blocker / 0 Major / 2 Minor 已落实）；落地期登记一处实现级适配——运行时探针实证本 flux build 表格 td 无 `data-field` 属性，模式 B id 单元格改为按 `__save` 返回 seq id 精确文本定位，零匹配守卫语义不变（附录 A.2 增补），不引入新 mask 区域，无需再审。
- **`git diff --stat tests/e2e/visual/**-snapshots/` 对账**：`git status` 快照目录 = 35 个 `??`（新增）+ 0 个 `M`（零修改）+ 0 个 `D`——仅新增文件、零既有基线修改，对账闭合。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物（执行时规则 7）。

- [x] 范围内行为完成（矩阵全行落地，三模式均有交付）（35 行全落地：A 23 基线零变更对话框/抽屉态 + B 4 执行后列表态 + C 8 拒绝 Toast 存在性，35 PNG ↔ 35 矩阵行 1:1 对账）
- [x] 相关文档对齐（e2e-runbook.md 业务动作视觉段）（「业务动作像素层（M2.2）」节 + 套件构成表 57 + §业务动作浏览器层计数勘误 117 + 目录树注释计数随审计 Minor m-2 一并勘误）
- [x] 已运行验证：`npx playwright test tests/e2e/visual/ --workers=1` 全绿（三连跑）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（business-actions.snapshot 三连跑 55 passed + 2 skipped × 3 零漂移；目录级单轮 4 snapshot 套件 167/167 全绿、44 DOM 层失败经 fresh-DB stash 对照 43=43 Identical Failure Sets 裁决预存与本批零因果；checker exit 0 R2c=1542 与基线行一致；`_exploration/` 预存红灯按 runbook 口径排除于门禁集）
- [x] 无范围内项目降级为 deferred/follow-up（矩阵 35 行全交付；Deferred But Adjudicated 仅登记范围外 Linux CI 基线 successor，非降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 acceptable-as-is + iteration 2 不适用声明；mask plan-audit iteration 1 APPROVE ses_f8e02f7c9ffehxBgvGOpzyvMljNMSJXe）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（三 Phase Status: completed 与 [x] 勾选一致；证据日期 2026-09-06 统一；日志 docs/logs/2026/09-06.md 与计划条目对应）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（fresh-session 独立子代理 ses_f8cfa7d4bffexel9hmATFQWMnZ，2026-09-06，9 审计项 8 PASS，0 Blocker / 1 Major（Closure Gates 机械性勾选 sequencing，实质逐项核验成立）/ 2 Minor（陈旧行号引用 + runbook 目录树预存漂移）→ NEEDS REVISION；Major 与 Minor 全部随闭包修订落实（gates 勾选 + `:396`→`:150-168` 勘误 + 目录树计数勘误），按审计意见「修订后即可 APPROVE」闭合）
- [x] 结束证据存在于文件中（本计划各 Phase 内联证据 + Draft Review Record 合规声明 + 日志 09-06 条目）

## Deferred But Adjudicated

### Linux CI 平台像素基线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 沿 2010-2 macOS 本地基线口径；runbook §CI 集成约定将 CI 像素门禁的平台基线捕获设为启用前置，非本计划结果表面
- Successor Required: `yes`（触发条件：`.github/workflows/e2e.yml` 启用像素层 CI 门禁时，与 N=1/N=3 同一 successor 合并处置）

## Closure

Status Note: 计划闭合（2026-09-06）。选型矩阵 35 行三模式全落地，`assertBusinessActionPixelSnapshot` helper 子集只增不改落地；35 张像素基线入库与矩阵行 1:1 对账、既有基线零修改（快照重录合规声明在案，git 对账 35 新增/0 修改）；business-actions.snapshot 三连跑 55 passed + 2 skipped × 3 零漂移；目录级单轮 4 snapshot 套件 167/167 全绿（44 DOM 层失败经 fresh-DB stash 对照实验 43=43 Identical Failure Sets 裁决为预存、与本批零因果）；模式 B 零残留双证据（4 域既有 spec 复跑 10/10 + GraphQL 直查 total=0）；compliance checker exit 0（R2c=1542 与基线行零漂移）；runbook「业务动作像素层（M2.2）」节 + 计数勘误（117）回写，roadmap M2.2 `done`。全 reactor `mvn clean install -DskipTests` + `mvn test` BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: 独立 fresh-session 结束审计子代理（task id ses_f8cfa7d4bffexel9hmATFQWMnZ，2026-09-06）
- Evidence: 9 审计项 8 PASS（spec/baselines 三向对账 23/4/8 ↔ 35 PNG ↔ 57 测试；helper `git diff --numstat` +48/-0 纯增量；矩阵抽样 5 行逐项一致；runbook/roadmap/日志/合规声明/脏面逐项实仓核验）；0 Blocker / 1 Major（Closure Gates 机械性勾选 sequencing）/ 2 Minor（`:396` 陈旧行号、runbook 目录树预存漂移）→ NEEDS REVISION；Major + Minor 全部随闭包修订落实（gates 勾选、行号勘误、目录树计数勘误），按审计意见「修订后即可 APPROVE」闭合。

Follow-up:

- Linux CI 平台像素基线（范围外 successor，见 Deferred But Adjudicated——与 N=1/N=3 同一 successor 合并处置，触发条件 = `.github/workflows/e2e.yml` 启用像素层 CI 门禁）

## Appendix A — 选型矩阵（Phase 1 产出，2026-09-05）

> 总量 35 行 ∈ [30, 50]：模式 A 23 行（14 域）+ 模式 B 4 行（4 域）+ 模式 C 8 行（4 域）。
>
> **域级排除裁决**（18 crud 域 → 16 域有动作面 → 本矩阵覆盖 16 域中的 14 域 + maintenance/projects 经模式 B 覆盖）：
> - **master-data**：全域无业务动作行按钮（标准 CRUD），无 mutation 视觉面 → 排除。
> - **b2b**：`ErpB2bAsn.view.xml` `<crud name="main"/>` 为空，零自定义 rowAction → 排除。
> - **cs reopen**：`reopen` 仅为后端 @BizMutation（`ErpCsTicketStateMachine` 迁移边），view.xml 无对应行按钮 → 类别级排除（reopen 无 UI 采集面）。
> - **post 类别**：种子凭证 8 行全部 `POSTED` 态（无 DRAFT 行），`row-post-button` `visibleOn docStatus == 'DRAFT'` 对全部种子行不可见；自包含凭证 setup（头+平衡行）成本超出本批轻量边界 → `等` 代表性口径下登记为类别残留（Non-Blocking，见 Deferred But Adjudicated 前置说明——不构成范围内降级， Goals 类别清单为 `等` 后代表性枚举）。
>
> **状态安全论证义务**（Phase 1 Decision 1 逐行核验结论，三列合并陈述于「安全论证」列）：
> - 模式 A 全部行：flux `runtime-action-helpers.ts` confirmText 门机制级保证——env.confirm 取消（Escape/cancel 按钮）→ `createCancelledResult`，**不发起任何后端调用**，零状态变更与旁路写者无关（无后端事务开启）。逐行核验「按钮对该种子行可见（visibleOn 匹配）」→ 见矩阵「种子态→可见性」列。
> - 模式 C 全部行：守卫为 BizModel/状态机方法**首语句**抛错，先于任何 ORM 写——cs `start/close/resolve/assign`（`ErpCsTicketStateMachine` 迁移矩阵 9 边，非法边 `illegal()` 抛 ERR_ILLEGAL_STATUS_TRANSITION）、mfg `cancel`（`validateTransitionForCancel` → `assertCanCancel` 仅 {DRAFT,SUBMITTED,NOT_STARTED}，WO-2026-003 IN_PROCESS 被拒）、crm `qualify`（须 NEW，LEAD QUALIFIED 被拒）、qa NCR `resolve`（CAPA 闭包门，无 CAPA 行被拒 ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED）。事务回滚路径核验：上述四域守卫路径无 REQUIRES_NEW 类旁路写者（`ErpFinPostingExceptionRecorder` 先例属 fin 过账编排，不在本批 4 域路径上；cs/crm/qa/mfg 守卫拒绝点之后无代码执行）。
> - 模式 B 全部行：mutation 仅翻转自包含行状态（schedule/confirm/activate/startProject 均无凭证/流水类下游产物——mnt schedule 仅状态+冲突检查读、inv confirm 不产流水（complete 才产）、ct activate 仅状态、prj startProject 仅状态）；行编码 `E2E-BAS-*` 确定性，cleanup `deleteByFilter(code)` 后零残留（Phase 2 Proof 3 实测闭合）。
>
> **mask 处置结论**（Phase 1 Decision 4）：canonical `header`（会话身份）+ `canvas`（0 匹配 no-op）继承自 `assertSnapshot`；附加 mask 仅两类——C 行 `[data-sonner-toaster]`（存在性由 `waitForSelector` 强制，mask 仅防文案漂移）、B 行新行 id 单元格（seq 跨 run 漂移防护，带零匹配守卫——plan-audit m-2：`idCell.waitFor visible` 先于截图，列结构变更时响亮失败而非静默烘焙未 mask 的 seq id）；其余行零附加 mask（审计列几何实证沿 M2.1 附录 A 双 fresh-DB 启动结论：stamp 列 x≥2031 在 1280×720 视口外；对话框/抽屉打开态不改变列表列几何；M2.1 R19~R36 对话框基线先例三连跑全绿佐证）。**plan-audit Major-1 运行时探针补证（2026-09-05，临时 probe spec 实测后删除）**：4 个 page 类对话框行（A08 红冲预览 / A09 资产仪表板 / A12 完整档案 / A13 调动）在 1280×720 视口内 **stamp 文本总数 = 0**（TreeWalker 全 DOM 扫描 `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}` 形态，非仅视口判断——对话框内容不含任何审计时间戳文本），Major-1 关闭；同探针实证 A09/A12/A13 渲染面为 **drawer-surface**（非 dialog-surface），spec `surfaceFor('page')` 按 dialog∪drawer 联合等待（镜像 `CrudListPage.clickEdit` 先例）。

### A.1 模式 A — 对话框/抽屉打开态（种子行驱动，零状态变更）

采集范式：`CrudListPage.navigate()` → `findRowByText(种子码)` → 行内 `[data-slot="table-actions"]` 按钮点击 → `[data-slot="alert-dialog-content"]`（或 dialog/drawer surface）visible 等待 → `assertBusinessActionPixelSnapshot` → Escape 取消 → surface hidden 断言（零变更闭环）。

| # | 域 | 实体/动作 | 种子行（态 → 可见性） | 对话框面 | 数据源 | mask（canonical 外） | 安全论证 |
|---|----|-----------|----------------------|----------|--------|---------------------|----------|
| A01 | purchase | ErpPurOrder 反审批 | PO-2026-001（APPROVED/ACTIVE → `approveStatus=='APPROVED'` ✓） | alert-dialog（确认反审批此采购订单） | 种子 | 无 | confirm 取消零调用 |
| A02 | purchase | ErpPurOrder 作废 | PO-2026-001（ACTIVE → `docStatus!='CANCELLED'` ✓） | alert-dialog | 种子 | 无 | 同上 |
| A03 | purchase | ErpPurOrder 关联入库单 | PO-2026-001 | drawer（ref-order） | 种子 | 无 | drawer 只读 |
| A04 | sales | ErpSalOrder 反审批 | SO-2026-001（APPROVED ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A05 | sales | ErpSalOrder 作废 | SO-2026-001（ACTIVE ✓） | alert-dialog | 种子 | 无 | 同上 |
| A06 | manufacturing | ErpMfgWorkOrder 报告完工 | WO-2026-003（IN_PROCESS ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用（不触发 completedQty=0 写） |
| A07 | manufacturing | ErpMfgWorkOrder 结案 | WO-2026-003（IN_PROCESS ✓） | alert-dialog | 种子 | 无 | 同上 |
| A08 | finance | ErpFinVoucher 红冲预览 | PZ-2026-001（POSTED+!isReversed ✓） | dialog 页 reversePreview（previewReverseVoucher 只读查询） | 种子 | 无 | 预览为 @BizQuery 只读；内层确认不点击 |
| A09 | assets | ErpAstAsset 完整仪表板 | AST-2026-001 | dialog 页 assetDashboard（tabs 只读） | 种子 | 无 | 只读 |
| A10 | projects | ErpPrjProject 暂停 | PRJ-2026-001（OPEN ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A11 | projects | ErpPrjProject 完成 | PRJ-2026-001（OPEN ✓） | alert-dialog | 种子 | 无 | 同上 |
| A12 | hr | ErpHrEmployee 完整档案 | HR-EMP-001 | dialog 页 employeeArchive（4 crud tabs 只读） | 种子 | 无 | 只读 |
| A13 | hr | ErpHrEmployee 调动 | HR-EMP-001 | dialog 页 transfer（表单，不提交） | 种子 | 无 | 表单不提交 |
| A14 | crm | ErpCrmLead 丢单 | LEAD-2026-001（QUALIFIED，无 visibleOn ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A15 | inventory | ErpInvStockMove 关联流水 | MV-2026-001（DONE，无 confirm 类按钮可见） | drawer（ref-move） | 种子 | 无 | drawer 只读 |
| A16 | aps | ErpApsOperationOrder 开始 | APS-OP-2026-001（PLANNED ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A17 | aps | ErpApsOperationOrder 作废 | APS-OP-2026-001（PLANNED ✓） | alert-dialog | 种子 | 无 | 同上 |
| A18 | logistics | ErpLogShipment 作废 | SHP-2026-001（IN_TRANSIT → `status!='CANCELLED'&&!='DELIVERED'` ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用（后端 cancel 合法但**不确认**） |
| A19 | contract | ErpCtContract 中止 | CT-SEED-2026-001（ACTIVE ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A20 | contract | ErpCtContract 终止 | CT-SEED-2026-001（ACTIVE → `status!='TERMINATED'` ✓） | alert-dialog | 种子 | 无 | 同上 |
| A21 | drp | ErpDrpPlan 运行DRP | DRP-PLAN-SEED-001（DRAFT ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A22 | quality | ErpQaNonConformance 评审 | NCR-2026-001（OPEN ✓） | alert-dialog | 种子 | 无 | confirm 取消零调用 |
| A23 | quality | ErpQaNonConformance 拒绝 | NCR-2026-003（OPEN → `OPEN\|\|IN_REVIEW` ✓） | alert-dialog | 种子 | 无 | 同上 |

### A.2 模式 B — 执行后列表态（自包含 setup + cleanup，≥3 域）

采集范式：GraphQL `__save` 建自包含行（`E2E-BAS-*` 确定性编码）→ 导航列表 → 点动作 → alert-dialog 确认 → mutation 执行 → 成功 toast 消失等待（`waitToastGone`）+ 列表刷新等待 → 截列表态（新行 id 单元格 mask）→ `deleteByFilter(code)` cleanup → Phase 2 Proof 3 复跑受影响域 spec 证零残留。

> **Phase 2 落地增补（2026-09-05 实仓核验）**：① 会话/刷新机制——`Navigation.login` 的表单等待仅在未认证上下文可解，模式 B 测试体改为「`loginAndNavigate` 直接落地实体列表（单次登录）→ `__save` → `page.reload()` 全文档重载拾取新行（同 URL `page.goto` 为同文档 hash 导航不触发重取）」；② id 单元格定位——运行时探针（TreeWalker）实证本 flux build 表格 td **无 `data-field` 属性**，mask 定位改为「`__save` 返回 seq id 精确文本（`^<id>$`）匹配行内 td」，零匹配守卫语义不变；③ B01 字段集补 `approveStatus: 'UNSUBMITTED'`（ORM mandatory，镜像 smoke save 头）。

| # | 域 | 实体/动作 | 自包含 setup（确定性字段） | 执行后态 | 数据源 | mask（canonical 外） | 安全论证 |
|---|----|-----------|---------------------------|----------|--------|---------------------|----------|
| B01 | inventory | ErpInvStockMove 提交确认 | `E2E-BAS-MV-001` DRAFT（moveType INCOMING/orgId 2/仓库 1→2/businessDate 固定） | CONFIRMED（无流水——complete 才产） | 自包含 | 新行 `td[data-field="id"]` | 状态翻转无下游产物；cleanup 删行 |
| B02 | maintenance | ErpMntVisit 排程 | `E2E-BAS-VIS-001` DRAFT（equipmentId 1/visitDate 2026-12-25/assignedTo 非空，镜像 maintenance-visit spec setup） | SCHEDULED | 自包含 | 新行 id 单元格 | 仅状态+冲突读；cleanup 删行 |
| B03 | contract | ErpCtContract 生效 | `E2E-BAS-CT-001` NEGOTIATION（SALES/OUTBOUND，镜像 ct-contract-lifecycle setup） | ACTIVE | 自包含 | 新行 id 单元格 | 仅状态；cleanup 删行（cascade 行） |
| B04 | projects | ErpPrjProject 启动 | `E2E-BAS-PRJ-001` DRAFT（镜像 projects-task spec Project setup 字段） | OPEN | 自包含 | 新行 id 单元格 | 仅状态；cleanup 删行 |

### A.3 模式 C — 拒绝 Toast 渲染存在性（种子终态行守卫触发，零状态变更，≥8 路径）

采集范式：种子行 → 点动作 → alert-dialog 确认 → 后端守卫拒绝 → flux `notify('error')` → sonner toast（`[data-sonner-toast]`，top-right）→ `waitForSelector('[data-sonner-toast]')` 等待（超时抛错 = 渲染缺失即失败）→ 截图（toaster mask 防文案漂移；元素缺席 = 无遮挡块 = diff 失败，存在性断言不被 mask 削弱）。

| # | 域 | 实体/动作 | 种子行（态 → 守卫） | 预期拒绝源（实仓核验） | 数据源 | mask（canonical 外） | 安全论证 |
|---|----|-----------|---------------------|------------------------|--------|---------------------|----------|
| C01 | manufacturing | ErpMfgWorkOrder 作废 | WO-2026-003（IN_PROCESS → 按钮可见 `!=CANCELLED/CLOSED/COMPLETED` ✓；后端仅 {DRAFT,SUBMITTED,NOT_STARTED}） | `assertCanCancel` 非法迁移（源码核验 `ErpMfgWorkOrderDocumentStateMachine`） | 种子 | `[data-sonner-toaster]` | 守卫首语句抛错，先于 ORM 写，无旁路写者 |
| C02 | cs | ErpCsTicket 分派 | TKT-2026-002（IN_PROGRESS → 按钮无 visibleOn ✓；后端 assign 须 NEW） | 迁移矩阵非法边（源码核验） | 种子 | 同上 | 同上 |
| C03 | cs | ErpCsTicket 开始处理 | TKT-2026-002（IN_PROGRESS；后端 start 须 ASSIGNED） | 同上 | 种子 | 同上 | 同上 |
| C04 | cs | ErpCsTicket 关闭 | TKT-2026-002（IN_PROGRESS；后端 close 须 RESOLVED） | 同上 | 种子 | 同上 | 同上 |
| C05 | cs | ErpCsTicket 开始处理 | TKT-2026-001（RESOLVED → start 须 ASSIGNED） | 同上 | 种子 | 同上 | 同上 |
| C06 | cs | ErpCsTicket 解决 | TKT-2026-001（RESOLVED → resolve 须 IN_PROGRESS） | 同上 | 种子 | 同上 | 同上 |
| C07 | crm | ErpCrmLead 资质认定 | LEAD-2026-001（QUALIFIED → 按钮无 visibleOn ✓；后端 qualify 须 NEW） | `ErpCrmLeadStateMachine` 非法边（源码核验） | 种子 | 同上 | 同上 |
| C08 | quality | ErpQaNonConformance 解决 | NCR-2026-002（IN_REVIEW → 按钮可见 ✓；后端 CAPA 闭包门） | ERR_NCR_RESOLVE_NO_CAPA（plan-audit m-1 勘误：NCR-2026-002 零 seed CAPA 行——`erp_qa_action.csv` 仅 NCR_ID=1——且 resolve 按钮不传 `noCapaReason`，空 actions 无豁免理由走 `requireResolveGate` NO_CAPA 拒绝，非 0335-2 spec 的 CAPA_NOT_COMPLETED 路径；守卫仍为首语句抛错零写） | 种子 | 同上 | 同上 |

兜底处置（泛化）：任一行三连跑证伪（漂移）时，对该行启用 `opts.mask` 动态区域（审计列区/表体区/toast 文案区）或降级 DOM 层断言，并在本计划登记行号、漂移证据与理由。

## Appendix B — 三模式适用判据与 helper 设计（Phase 1 Decision 1/3 记录）

- **模式适用判据**：(A) 目标动作在种子行上有 confirm 类对话框/抽屉面且 confirm 取消语义可零变更退出 → 模式 A（优先）；(B) 目标动作为**执行语义本身**（执行后状态可视）且自包含 setup 轻量（≤3 实体、无凭证/流水下游）→ 模式 B；(C) 种子行存在「按钮可见（visibleOn 匹配）+ 后端守卫拒绝」组合 → 模式 C（零状态变更 + Toast 渲染存在性）。三模式互斥按行分配；同一实体可多模式多行（如 mfg A06/A07 + C01）。
- **`assertBusinessActionPixelSnapshot` 设计**：委托既有 `assertSnapshot`（字体固化 + canonical mask + 1% 容差），`skipEchartsSettle` 缺省 `true`（业务动作面为对话框/抽屉/列表，无 canvas）；新增模式 C 等待策略参数 `waitForSelector`（默认超时 8s，超时**抛错**——拒绝渲染缺失是断言失败而非跳过）+ `waitForSelectorTimeout`；新增模式 B `waitToastGone`（成功 toast 消失后再采集，10s 兜底 catch 放行）。考虑过的替代方案：(a) 复用 `assertCrudPixelSnapshot` 加散列参数——rejected：调用面语义混乱且违反 M0.3 §7 按场景命名并列规则；(b) spec 内裸调 `toHaveScreenshot`——rejected：脱离统一字体/mask/容差范式。
- **残留风险**：模式 C toast 文案含实体 code/状态枚举（确定性种子值），仍 mask toaster 全区（防 i18n 渲染层漂移）；toast 自动消失（sonner ~4s）与截图时序由 `waitForSelector` 先行同步消解。
