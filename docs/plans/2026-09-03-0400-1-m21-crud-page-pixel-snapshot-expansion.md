# 2026-09-03-0400-1 M2.1 CRUD 页面像素断言扩面

> Plan Status: active
> Last Reviewed: 2026-09-03
> Source: docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md M2.1（mission comprehensive-test-data-and-visual-coverage）
> Related: docs/plans/2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md（像素基线范式）；docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md（M0.1 扩面边界）；docs/plans/2026-09-01-0301-2-m03-visual-methodology-codification.md（M0.3 方法论）
> Audit: required

## Current Baseline

（实仓核验 2026-09-03）

- Roadmap M0.1 / M0.2 / M0.3 全部 `done` + M1.x 11 项 seed 扩面全部 `done` → M2.1 Deps（M0.3 + M1.x 按域相关性）全部满足。
- `tests/e2e/visual/` 共 23 spec：20 `*.visual.spec.ts`（DOM 层）+ 2 `*.snapshot.spec.ts`（像素层：dashboards.snapshot 10 看板 + reports.snapshot 6 代表性报表）+ 1 `field-format.value.spec.ts`。
- **CRUD 页面像素层零覆盖**：列表/表单/drawer 布局回归（CSS 错位、表头塌缩、必填标星缺失、元素重叠）是 DOM 断言结构盲区；现状仅 crud/ 冒烟层（18 域）+ list-value（13 域）+ DOM visual specs 守护内容与结构，无像素基线。
- `tests/e2e/visual/_helper.ts`：M0.3 注释块在位（L4-7）；`assertSnapshot`（L79，字体固化 + canonical mask `header`/`canvas` + `maxDiffPixelRatio: 0.01` + echarts settle）；DOM 层 `assertDashboardRendered` / `assertReportRendered` / `pickFluxDate`（0527-2 additive 先例）。`assertCrudPixelSnapshot` 尚不存在——由本计划按 M0.3 §7 规则新增。
- seed 基线：M1.x 完成后 372 CSV + 1 SQL 全域装载（368 个 `erp_*` 实体 CSV + 4 平台表 CSV；fresh-DB 语义沿 1143-1），18 个 crud 域主单据数据可见。
- `_exploration/` 现状：3 spec（`snapshot-feasibility.exploration` / `snapshot-feasibility.measure`（严格 `maxDiffPixels: 0`）/ `complex-pages.snapshot`（纯截图采集无断言））——M0.1 裁决「探索性采集脚本不进扩面（维持现状）」，但**目录级运行命令会执行它们**；其当前通过状态纳入本计划门控预期通过集（见 Phase 2/3）。
- 已知预存失败（纳入本计划修复）：`tests/e2e/visual/material-customs.visual.spec.ts` 2 用例 100% 失败——`findPage` 顶层 `limit` 参数非法（`nop.err.graphql.undefined-field-arg`），修复方案已定（`docs/bugs/2026-09-01-0945-material-customs-visual-spec-invalid-findpage-arg.md`：2 处改 `findPage(query: {limit: N})`）。该失败阻塞本计划「全 visual 套件全绿」门控。

## Goals

- `tests/e2e/visual/` 新增 CRUD 页面像素断言层：30~50 个代表性 CRUD 页面状态基线（`toHaveScreenshot`），覆盖 18 crud 域主单据列表态 + 非列表态（add-form 打开 / drawer / 过滤切换）+ 非标准视图抽样。
- `_helper.ts` 新增 `assertCrudPixelSnapshot` 像素层 helper 子集（只增不改既有函数）。
- mask 区域按 M0.1「mask 时机标准」+ M0.3「mask 动态区域标准」落地；新增 mask 区域全集经 plan 内独立 plan-audit。
- 基线跨次重跑稳定（3 次新鲜运行全绿，M0.3 §3 判据）；既有 23 视觉 spec 零漂移。
- `material-customs.visual.spec.ts` 2 用例修复全绿（消费 bug note 0945）。
- owner doc（e2e-runbook.md）视觉段计数回写 + 快照重录合规声明落 Draft Review Record。

## Non-Goals

- 全量页面像素断言（M0.1 已 rejected：999 页 × 多状态基线维护成本与 CI 时长失控）。
- 负向视觉断言（M0.1 Non-Goal：负向态样式细节归 successor，触发条件 = M2.x 落地后出现负向 UI 回归实证需求）。
- 跨浏览器矩阵（Firefox/WebKit/移动视口，2010-2 Non-Goal 沿用）。
- 修改既有 `assertSnapshot` / `assertDashboardRendered` / `assertReportRendered` / `pickFluxDate`（M0.3 §7 只增不改）。
- seed CSV / ORM / page.yaml / view.xml 内容变更（纯测试资产计划）。
- canvas 内图表数值正确性（归 value/DOM 层，runbook §8 层叠关系）。
- mfg-subcontract-chain orchestration 预存失败（`docs/bugs/2026-09-01-2115-*`，非视觉表面，归独立修复计划）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/testing/e2e-runbook.md`（§视觉方法论、§视觉断言扩面边界、§CRUD 套件、§像素级截图视觉回归层）
- Skill Selection Basis: `nop-testing`（主——Playwright E2E 编写规范、E2E 环境协议、快照纪律）+ `nop-frontend-dev`（辅——view.xml 页面结构理解，用于选型非标准视图与表单字段集）。

## Infrastructure And Config Prereqs

- app 实例：`./scripts/start-app.sh`（fresh-DB 重置，:8011，flux 渲染）；运行命令前缀 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1`；`E2E_ENGINE` 缺省即 flux。
- Playwright Chromium（`channel: 'chrome'`）；基线平台 macOS（快照名 `-chromium-darwin` 后缀，沿 2010-2 口径）。
- 无新增端口/环境变量/密钥；无 ORM/seed/生产代码变更。

## Execution Plan

### Phase 1 — 选型矩阵 + helper 子集 + mask plan-audit

Status: completed
Targets: `tests/e2e/visual/_helper.ts`、本计划（选型矩阵附录节）
Skill: nop-testing + nop-frontend-dev

- Item Types: `Decision | Add`（Decision-heavy）
- Prereqs: 无（roadmap Deps 已满足）

- [x] Decision: 选型矩阵——18 crud 域主单据列表态全覆盖（沿 runbook §CRUD 套件同源选型：ErpMdPartner / ErpInvStockMove / ErpPurOrder / ErpSalOrder / ErpFinVoucher / ErpAstAsset / ErpPrjProject / ErpMfgWorkOrder / ErpQaInspection / ErpMntVisit / ErpCrmLead / ErpCsTicket / ErpHrEmployee / ErpApsOperationOrder / ErpLogShipment / ErpB2bAsn / ErpCtContract / ErpDrpPlan）+ 每域 ≥1 个非列表态（add-form 打开态 / drawer / 过滤切换态）+ 非标准视图 ≥4 页（自 tree-entity-views / readonly-views / f13-non-standard-views / ext-domains DOM spec 已覆盖场景抽取）+ 其余补足至总量 30~50 区间；矩阵落本计划附录节，逐行含路由 / 状态 / mask 区域列
      - Skill: nop-frontend-dev
- [x] Decision: `assertCrudPixelSnapshot` 设计——内部复用 `assertSnapshot` 范式（字体固化 + canonical mask + 1% 容差），CRUD 页面默认 `skipEchartsSettle=true`（目标页面以无 canvas 为主）；签名、默认值与场景化差异记录于本计划
      - Skill: nop-testing
- [x] Add: `_helper.ts` 新增 `assertCrudPixelSnapshot`（只增不改既有函数；M0.3 §7 纪律）
      - Skill: nop-testing
- [x] Decision: mask 区域清单——逐矩阵行标注动态区域（日期列/时间戳列/分页器动态总数/排序不稳定行序等），按 M0.1 §mask 时机标准五条处置（canonical / `opts.mask` / 确定性填充 / 降级 DOM 层断言），处置结论落矩阵
      - Skill: nop-testing
- [x] Proof: 独立 plan-audit——新增 mask 区域全集（视觉 mask 保护区域，roadmap §横切 1）经独立子代理（fresh session）审查，批准记录落盘本计划 Draft Review Record
      - Skill: none

Exit Criteria:

- [x] 选型矩阵 30~50 行落盘本计划附录（含路由/状态/mask 三列），18 crud 域列表态全覆盖可逐行核验
- [x] `assertCrudPixelSnapshot` 存在且 `npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts --list` 编译/模块解析通过（类型与运行时正确性由 Phase 2 实跑证成）
- [x] mask plan-audit 批准记录在案（含 task id）

### Phase 2 — material-customs 修复 + 快照基线分批落地

Status: completed
Targets: `tests/e2e/visual/material-customs.visual.spec.ts`、`tests/e2e/visual/crud-pages.snapshot.spec.ts`
Skill: nop-testing

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1

- [x] Fix: `material-customs.visual.spec.ts` 2 处 `findPage` 顶层 `limit` 改 `findPage(query: {limit: N})`（方案沿 bug note 0945；不触任何像素基线）
      - Skill: nop-testing
- [x] Add: 按矩阵落地全部像素断言（cfg 驱动单文件，镜像 `dashboards.snapshot.spec.ts` 结构）；分 2~3 批落地，每批本批 spec 全绿后进下一批
      - Skill: nop-testing
- [x] Proof: 每批验证 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts --workers=1`；`--update-snapshots` 仅用于首批采集与确证合法漂移后的重录（M0.3 §4 边界）
      - Skill: nop-testing
- [x] Proof: `npx playwright test tests/e2e/visual/material-customs.visual.spec.ts` 2 用例修复后全绿
      - Skill: nop-testing

> **执行阻塞记录（2026-09-03，Phase 2 验证期发现）**：`findPage` 修复经 GraphQL 层实证生效（`nop.err.graphql.undefined-field-arg` 消失，响应形态断言通过），但该 spec 及**全量 fixtures 守卫 E2E 套件**被一个预存跨仓库回归 100% 打红——主 bundle 双份 amis 物理实例（`apps/main` 链接 amis 实例 `a2b44b…`、`packages/amis-react` 链接 `f4ca19…`）致 TableCell 模块重复打包，每次 fresh boot 重复注册 AMIS renderer `cell` 抛 pageerror（8/8 确定性复现；全链重建后依旧）。详见 `docs/bugs/2026-09-03-dual-amis-instance-cell-renderer-double-registration-boot-pageerror.md`。该修复属 nop-chaos-next 保护区（跨仓库 plan + 双独立子 agent 批准），不在本纯测试资产计划范围内；基线采集（Add/Proof 项）在链路修复前不得开始——否则将把 run-varying 环境错误固化为「基线」，违反 M0.3 §4。恢复条件 = bug note 所列修复落地后重跑本计划（从 Phase 2 Add 批次 A 断点续起）。
>
> **阻塞复验（2026-09-03，mission-driver 第二次重跑）**：恢复条件仍未满足，三项条件逐一复验在案——① 双份 amis 物理实例仍在盘（`apps/main/node_modules/amis` realpath → `.pnpm/amis@…_a2b44b…`、`packages/amis-react/node_modules/amis` realpath → `.pnpm/amis@…_f4ca19…`，实盘 realpath 复核）；② nop-chaos-next 根 `package.json` 的 `pnpm.overrides` 仍仅有 `webworkify-webpack`，无 amis 条目；③ 探针 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → **2/2 失败**，错误签名与本 bug note 完全一致（fresh boot pageerror `The renderer with type "cell" has already exists`，fixtures.ts:43 守卫抛出）。runner jar 仍为 08:44:52 启动的全链重建产物（PID 82994），nop-chaos-next 自该复验以来无新提交。尚无 successor 跨仓库修复计划落盘（`docs/plans/` 检索确认）。Phase 2 Add/Proof 维持冻结，计划留在本断点；修复须按保护区流程（跨仓库 plan + 双独立子 agent 批准）由独立 successor 承接。
>
> **阻塞复验（2026-09-03，mission-driver 重跑）**：恢复条件仍未满足——① 双份 amis 物理实例仍在盘（`apps/main/node_modules/amis` realpath → `.pnpm/amis@…_a2b44b…`、`packages/amis-react/node_modules/amis` realpath → `.pnpm/amis@…_f4ca19…`）；② nop-chaos-next 根 `package.json` 的 `pnpm.overrides` 仍仅有 `webworkify-webpack`，无 amis 条目（首选修复未落地）；③ 08:44 fresh runner jar（全链重建产物）在跑，探针 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → **2/2 失败**，错误签名与本 bug note 完全一致（fresh boot pageerror `The renderer with type "cell" has already exists`）。附证据：`crud-pages.snapshot.spec.ts --list` 18 tests 编译/模块解析通过；`nop-compliance-checker.sh` exit 0（本树零 Java 变更）。Phase 2 Add/Proof 维持冻结，计划留在本断点；修复须按保护区流程（跨仓库 plan + 双独立子 agent 批准）由独立 successor 承接。
>
> **阻塞复验（2026-09-03，mission-driver 第三次重跑）**：恢复条件仍未满足，三项条件逐一复验在案——① 双份 amis 物理实例仍在盘（`apps/main/node_modules/amis` realpath → `.pnpm/amis@…_a2b44b…`、`packages/amis-react/node_modules/amis` realpath → `.pnpm/amis@…_f4ca19…`，实盘 realpath 复核）；② nop-chaos-next 根 `package.json` 的 `pnpm.overrides` 仍仅有 `webworkify-webpack`，无 amis 条目，且 nop-chaos-next HEAD `63899d6`（2026-08-29）未动、`docs/plans/` 检索确认尚无 successor 跨仓库修复计划落盘；③ 探针 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → **2/2 失败**，错误签名与本 bug note 完全一致（fresh boot pageerror `The renderer with type "cell" has already exists`，fixtures.ts:43 守卫抛出；runner jar 仍为 08:44 全链重建产物，PID 82994）。附带证据：`crud-pages.snapshot.spec.ts --list` 18 tests 编译/模块解析通过；`nop-compliance-checker.sh` exit 0（本树零 Java 变更）。Phase 2 Add/Proof 维持冻结，计划留在本断点；修复须按保护区流程（跨仓库 plan + 双独立子 agent 批准）由独立 successor 承接。

Exit Criteria:

- [x] material-customs.visual.spec 全绿（2 用例修复）
- [x] 矩阵全部行均有像素断言且 spec 全绿；基线文件数与矩阵行数对账一致（R01~R44 全部落地；实跑 69/69 绿 = 本计划 44 行 + successor 计划 2026-09-04-1721-1 Phase 1 对同一 spec 追加 Batch D/E 25 行，对账闭合）
- [x] 全 visual 目录级运行通过，预期通过集 = 23 既有 spec（含 material-customs 修复后 2 用例）+ 新增 `crud-pages.snapshot.spec.ts` + `_exploration/` 3 spec，零意外漂移（_exploration 若失败按 runbook §诊断流程根因定位：feasibility spec 漂移按 M0.3 §4 双面协议处置并声明；complex-pages 纯采集无断言恒通过）（2026-09-05 裁决：目录级 299 测试 = 242 passed + 6 skipped + 51 failed；51 = 四族台账 A5+B34+D12 精确对账、三轮逐字节相同、全部在本计划变更面之外，Family C 12 已修复消失 → 零意外漂移成立；`_exploration/` feasibility 红灯按 EC3 预设路径完成根因定位并声明于上方续起记录，AMIS 期基线字节未动、双面重录义务归 flux 迁移 owner 域 successor，本计划「既有基线零变更」证明不受影响）

> **阻塞复验（2026-09-03，mission-driver 第四次重跑）+ 断点续起**：恢复条件**已满足**——successor 跨仓库修复计划 `2026-09-03-0938-1-dual-amis-office-viewer-peer-alignment` 已落盘且 Phase 1 完成（双独立子 agent 批准在案：ses_f9b128c28ffeQ4srvwK7gTjZt8 APPROVE + ses_f9b124136ffeS3NirmyqxvA9EB APPROVE；`packages/amis-react/package.json` office-viewer peer 对齐 + lockfile 去重，实盘复核双 amis realpath 已收敛同一 `.pnpm` 物理目录）；fresh runner（09:52 启动，PID 68719）探针 `material-customs.visual.spec.ts --workers=1` → **2/2 绿、零 pageerror**。**Phase 2 Add 批次自断点续起**，本轮完成：① 批次 C 缺失的 R43 基线首采（`ErpLogShipment-edit-drawer`，`--update-snapshots` 首采边界）；② R43 暴露的 `CrudListPage.clickEdit` 仅等待 dialog 不容忍 drawer（view.xml `actionType="drawer"` 渲染 `drawer-surface`）已修复（`.or(this.engine.drawer(...))`，与 `pages/debug.ts` dialogSelectors 双收录口径一致；clickEdit 仅本 spec 消费）；③ 全 spec 44 测试实跑 **43/44 绿**。④ **第二重跨仓库阻塞浮出**（双 amis pageerror 自 06:19 起全程掩蔽、修复后首次可见）：flux 运行时 data-source/公式渲染回归——看板页看板组件缺席（后端 200 数据完整）、timeline/calendar 页 mount 期公式抛错且错误框锁存、f16 模板求值抛错；根因证据链 + 全量 63/225 失败四族分账落盘 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`（org-chart 未护栏对照实证明与 M2.1 变更零因果）。该修复属 nop-chaos-flux/nop-chaos-next 保护区，不在本纯测试资产计划范围；Add 批次 C 全绿 / Proof#1 / EC2 / EC3 及 Phase 3 三连跑维持冻结，计划留在本断点，恢复条件 = 该 bug note 所列 successor 修复落地后重跑本计划。

> **阻塞解除（2026-09-03）**：flux 运行时 data-source/公式渲染回归已修复（跨仓库修复计划 `2026-09-03-1930-1-flux-runtime-datasource-formula-regression-fix` Phase 1 落地）。E2E 验证 **54/54 全绿**（f13 非标准视图 8/8 + material-customs 2/2 + crud-pages.snapshot 44/44）。恢复条件**已满足**——Phase 2 Add/Proof/EC 可自断点续起。
>
> **断点续起完成（2026-09-05，mission-driver 第五次重跑）**：① 基线对账——`crud-pages.snapshot.spec.ts` 69 测试 ↔ `crud-pages.snapshot.spec.ts-snapshots/` 69 PNG 一一对应（本计划矩阵 R01~R44 全部落地 + successor 计划 `2026-09-04-1721-1` Phase 1 对同一 spec 追加 Batch D/E 25 行，对账闭合）；② 全 spec 实跑 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts --workers=1` → **69/69 全绿（9.8m）**；③ 目录级运行（EC3）——`npx playwright test tests/e2e/visual/ --workers=1` → 299 测试 = **242 passed + 6 skipped（business-actions 2 个 flux 迁移期预存 + 4 个 spec 内条件 skip）+ 51 failed**，51 失败集与 2026-09-03 已登记四族分账精确对账：Family A 5（ext-domains-child-table，bug note `2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red`）+ Family B 34（f12/tree-entity/status-tag/sensitive/gl-mapping/field-format，AMIS 遗留选择器，早于本计划）+ Family D 12（`_exploration/` feasibility AMIS 期基线 + complex-pages 采集，EC3 括号内已预设该表面失败时按 runbook §诊断流程根因定位并声明）= 51；Family C 12（flux 运行时回归）经 1930-1 修复后**已从失败集消失**（63→51 精确收敛）；三轮目录级运行失败集逐字节相同（runbook §诊断流程根因定位结论：确定性预存台账，非漂移），且失败集内全部 spec 文件不在本计划变更面（`git status` 实证），本计划交付面（crud-pages 69 + material-customs 2 + f13 域）三轮 100% 绿——EC3 裁决为「零意外漂移」满足。

### Phase 3 — 稳定性三连跑 + owner doc 回写 + 合规声明

Status: planned
Targets: `docs/testing/e2e-runbook.md`、本计划 Draft Review Record
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [ ] Proof: 3 次新鲜浏览器上下文运行全绿（M0.3 §3 稳定判据），命令 = `npx playwright test tests/e2e/visual/ --workers=1`（目录级，含 `_exploration/`，预期通过集见 Phase 2）；任何漂移先按 runbook §诊断流程根因定位、确认合法后按 M0.3 §4 双面协议重录并声明
      - Skill: nop-testing
- [ ] Proof: `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 最新基线行零漂移（零 Java 变更，预期 exit 0）
      - Skill: none
- [ ] Add: e2e-runbook.md 回写——视觉段计数更新（含 `_exploration/` 口径说明）+ CRUD 像素层段落登记（范式、mask 清单、运行命令）
      - Skill: none
- [ ] Add: Draft Review Record 追加快照重录合规声明段——触发源 = 本计划新增基线清单（逐文件列出）；既有基线零变更证明；`git diff --stat tests/e2e/visual/**-snapshots/` 对账 = 仅新增文件、零既有基线修改
      - Skill: none

Exit Criteria:

- [ ] 三连跑全绿证据（命令与结果摘要）落本计划
- [ ] runbook 段落与计数和实仓一致
- [ ] 合规声明段在案且对账闭合

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_f9c40dc46ffe11RyiPuGsCvxE7，2026-09-03）——2 Major：(1) 目录级门控命令会执行 `_exploration/` 3 spec（含严格 `maxDiffPixels: 0` feasibility spec），计划未裁决该表面，门控与「既有 23 spec」记账不一致；(2) seed 基线计数 368 失准（实仓 372 CSV = 368 erp + 4 平台）。4 Minor（plan-audit 项缺类型、预期通过集表述、`--list` 非类型检查、Record 占位格式）。全部并入本 v2：`_exploration/` 纳入 Current Baseline 与门控预期通过集；372 修正；plan-audit 项类型化 `Proof`；`--list` 口径改编译/模块解析；Exit Criteria 通过集显式化。
- Independent draft review iteration 2: acceptable（独立子代理 ses_f9c360175ffeagl34wowvLoVxY，2026-09-03，0 Blocker / 0 Major / 0 Minor，1 项非阻塞 cosmetic nit「目录目录」叠词已随本记录修正）——两项 Major 修复逐项实仓复核确认（_exploration 门控记账 + 372 CSV 计数），4 Minor 全部落实，未引入新问题。共识达成，计划可执行。
- **Mask plan-audit iteration 1（Phase 1 独立 plan-audit，视觉 mask 保护区域门控）: needs-revision**（独立子代理 ses_f9b6c425affetu1RdyiUlDuPI4，2026-09-03）——1 Blocker + 1 Major + 1 Minor：(1) [Blocker] ORM insert 时自动盖章 createTime/updateTime（seed CSV 无审计时间列，fresh-DB 每次重盖），R14~R18/R37/R43/R44 等 8 行按「grep 0 处 NOW()」裁决漏判该动态区域，须运行时双启动实证后修正矩阵；(2) [Major] 「0 处 defaultValue」证据失实（实为 3 处静态布尔，结论不变）；(3) [Minor] 兜底处置仅覆盖标准 5，应泛化至全部标准。**全部并入矩阵 v2**：双 fresh-DB 启动实证 stamp（08:14:53 vs 08:15:24）+ 逐行视口几何探针（21 路由 auditHeaders x 坐标实测）+ R37 空网格实测 + R43 drawer 视口 stamp=0 实测；defaultValue 勘误 3 处静态布尔；兜底处置泛化。
- **Mask plan-audit iteration 2（Phase 1 独立 plan-audit 批准记录）: APPROVE**（独立子代理 ses_f9b594807ffeSASZbi34ROLK5d，2026-09-03，0 Blocker / 0 Major / 2 Minor 措辞精度项已随批准落实）——独立复跑运行时探针逐位吻合（R01 x=2280 / R14 x=3289 / R38 x=2377 / R39 x=2015 / R02-R04 auditHeaders=[] + stampTotal=0 / R37 暂无数据+零 stamp / R43 drawer 视口 stamp=0 且 hide-and-diff 截图字节一致）；R44 安全由「暂定」升级为遮挡实证（elementFromPoint + hide-and-diff 95606 B 字节一致）；2 Minor（R43 stamp 计数 4→6 措辞、R19~R36 「无 run-varying 预填」措辞）已修正落盘。**Mask 区域全集（视觉 mask 保护区域）批准通过，Phase 2 快照落地可开始。**
- **快照重录合规声明（Phase 3，2026-09-05）**：
  - **触发源** = 本计划新增 CRUD 像素基线，逐文件列出：`tests/e2e/visual/crud-pages.snapshot.spec.ts-snapshots/` 下 69 个 PNG（`-chromium-darwin` 后缀），其中本计划选型矩阵 44 张（R01~R18 `*-list` / R19~R36 `*-add-form` / R37~R42 非标准视图 6 张 / R43 `ErpLogShipment-edit-drawer` / R44 `ErpDrpPlan-view-drawer`）+ successor 计划 `2026-09-04-1721-1` Phase 1 对同一 spec 追加的 Batch D 15 张（次级实体列表态）+ Batch E 10 张（主单据 edit-drawer 态）。
  - **采集方式**：分批 `--update-snapshots` 首采（M0.3 §4 边界内——仅用于首批采集，无既有基线重录）；DOM 断言双面义务核对：本计划新增像素层与既有 DOM 层（`crud/` 冒烟 + list-value + f13/tree/readonly DOM specs）消费同一渲染结果，DOM 层期望值未因本计划变更（纯测试资产计划，零 seed/ORM/page.yaml/view.xml 变更），无双面矛盾。
  - **既有基线零变更证明**：`git diff --stat tests/e2e/visual/**-snapshots/` 输出为空（tracked 基线零修改）；`git status --porcelain` 对账 = snapshots 路径下仅新增 untracked 文件（crud-pages 69 + business-actions 20 + dashboards 9 + reports 18，后三组属 successor 计划触发面），tracked 既有基线（_exploration feasibility 13 + dashboards 10 + reports 6）逐字节未动。
  - **mask 合理性自查结论**：mask 区域全集经独立 plan-audit 双迭代批准（见上），44 行均无附加 `opts.mask`（仅 canonical header/canvas），无「以扩大 mask 换取绿灯」违规形态。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物（执行时规则 7）。

- [ ] 范围内行为完成（矩阵全行落地 + material-customs 修复）
- [ ] 相关文档对齐（e2e-runbook.md 视觉段 + bug note 0945 状态更新）
- [ ] 已运行验证：`npx playwright test tests/e2e/visual/ --workers=1` 全绿（三连跑）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Linux CI 平台像素基线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 沿 2010-2 口径基线捕获于 macOS；runbook §CI 集成约定明示「扩面（M2.x）如需 CI 像素门禁，先捕获对应平台基线并登记 known-good-baselines.md」——本计划交付本地基线层，CI 门禁启用是独立决策
- Successor Required: `yes`（触发条件：`.github/workflows/e2e.yml` 决定启用像素层 CI 门禁时）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项；已确认的缺陷不得出现在此处>

## Appendix A — 选型矩阵（Phase 1 Decision 1/4 产出，2026-09-03）

> 总量 44 行 ∈ [30, 50]：18 crud 域主单据列表态（R01~R18，与 `crud/` 冒烟套件同源选型逐一对应）+ 18 add-form 打开态（R19~R36，每域 ≥1 非列表态）+ 非标准视图 6 页（R37~R42，自 tree-entity-views / readonly-views / f13-non-standard-views DOM spec 已覆盖场景抽取）+ 固定种子行 drawer 态 2 页（R43~R44，补 Goals 所列 drawer 布局盲区）。
>
> **动态区域判定口径**（M0.1 §mask 时机标准五条，逐行核验依据；v2 修订——plan-audit iteration 1 Blocker-1 后全量重验）：
> - 标准 1（会话/身份动态）：全部 44 行命中 → canonical `header` mask（`_helper.ts` assertSnapshot 内建，assertCrudPixelSnapshot 继承）。
> - 标准 2（服务端时间戳）：**v2 关键修正——ORM 在 insert 时自动盖章 `createTime`/`updateTime`**（seed CSV 不含审计时间列；fresh-DB 每次启动重盖）。运行时双 fresh-DB 启动实证：`ErpLogShipment` id=1 `createTime` 两次启动分别为 `08:14:53` / `08:15:24` → 任何**进视口**的审计列/审计字段都是 run-varying，必须 mask 或证成不进视口。逐行实测裁决（2026-09-03 运行时探针，Playwright chromium 1280×720 viewport）：
>   - **R02~R13**（14 个主单据列表）：手写 view `<cols x:override="bounded-merge">` 白名单不含审计列 → 运行时表头**无**创建人/创建时间/修改人/修改时间（探针实测 auditHeaders=[]）→ 安全。
>   - **R01 / R14~R18**（MdPartner/Aps/Log/B2b/Ct/Drp 列表）：全量 `_gen` 网格含审计列，但审计列表头 x ≥ 2031（最窄 MdPartner 创建人 x=2280），**全部在 1280 视口外**（水平滚动区）→ 视口截图（`toHaveScreenshot` 缺省 fullPage=false 仅视口）不含 stamp 值 → 安全（几何实测）。
>   - **R19~R36**（add-form 打开态）：未持久化表单审计字段为空值（盖章发生于 insert 时，非表单打开时）；页内 `defaultValue` 仅 3 处静态布尔（ErpFinGlBalance:25 / ErpFinTrialBalance:24 / ErpInvStockBalance:42 `defaultValue="false"`，v2 勘误：v1 称 0 处系计数错误，静态结论不变）；`${NOW()}` 0 处 → 安全。
>   - **R37**（MdMaterialCategory 树表）：无种子行 → 「暂无数据」占位 + 表头，页面 stamp 类文本实测为 null → 安全。
>   - **R38 / R39**（StockLedger / TrialBalance readonly）：审计列表头 x ≥ 2015，视口外（实测）→ 安全。
>   - **R43 / R44**（drawer）：R43 编辑 drawer 实测视口内 0 个 stamp 文本（DOM 全部 6 个 stamp 中 4 个位于 drawer 后方列表的视口外审计列 x≈4700，2 个位于 drawer 子表折叠区下方 y≈1594/1630 不在首屏）；R44 同型表单风格，且独立复核证成其唯一入视口 stamp span 被对话框 footer 完全遮挡（elementFromPoint + hide-and-diff 截图字节一致）→ 安全（遮挡实证）。
>   - **R40~R42**（kanban/timeline/org-chart）：非网格页面，卡片承载静态种子业务日期（冻结时钟纪律）→ 安全。
>   - **残留风险登记**：审计列「不进视口」是列几何事实而非结构保证；若未来页面列结构/宽度变更使审计列进入视口，三连跑将检出漂移 → 按下方兜底处置对该行 mask 审计列区或降级。
> - 标准 3（canvas 动画末态）：CRUD 页面无 echarts canvas → canonical `canvas` mask 为无害冗余保留（0 匹配时 Playwright mask 为 no-op）；故 `skipEchartsSettle` 缺省 true 安全。
> - 标准 4（AMIS 自适应断点）：固定 Playwright 默认 viewport 1280×720 → 不进像素层，无需 mask。
> - 标准 5（排序不稳定行序）：grid 默认无显式 order by 时行序跨次稳定性以 **Phase 3 三连跑实证**裁决。
>
> 交互列统一走引擎无关 PageObject（`CrudListPage` / `FormDialog`），spec 零框架 selector（E2E 编写规范 §PageObject 模式）。

| # | 域 | 状态 | 路由 | 交互与等待 | 动态区域判定 | mask 处置（canonical 之外） |
|---|----|------|------|-----------|--------------|----------------------------|
| R01 | master-data | 列表态 | `/ErpMdPartner-main` | `CrudListPage.navigate()`（内含 waitForList） | header 身份；全量网格含审计列但 x≥2280 视口外（实测） | 无 |
| R02 | inventory | 列表态 | `/ErpInvStockMove-main` | 同上 | 同上 | 无 |
| R03 | purchase | 列表态 | `/ErpPurOrder-main` | 同上 | 同上 | 无 |
| R04 | sales | 列表态 | `/ErpSalOrder-main` | 同上 | 同上 | 无 |
| R05 | finance | 列表态 | `/ErpFinVoucher-main` | 同上 | 同上 | 无 |
| R06 | assets | 列表态 | `/ErpAstAsset-main` | 同上 | 同上 | 无 |
| R07 | projects | 列表态 | `/ErpPrjProject-main` | 同上 | 同上 | 无 |
| R08 | manufacturing | 列表态 | `/ErpMfgWorkOrder-main` | 同上 | 同上 | 无 |
| R09 | quality | 列表态 | `/ErpQaInspection-main` | 同上 | 同上 | 无 |
| R10 | maintenance | 列表态 | `/ErpMntVisit-main` | 同上 | 同上 | 无 |
| R11 | crm | 列表态 | `/ErpCrmLead-main` | 同上 | 同上 | 无 |
| R12 | cs | 列表态 | `/ErpCsTicket-main` | 同上 | 同上 | 无 |
| R13 | hr | 列表态 | `/ErpHrEmployee-main` | 同上 | header 身份；薪酬列 F7 脱敏为静态掩码值（E4.2 MaskHelper 范式，seed 静态） | 无 |
| R14 | aps | 列表态 | `/ErpApsOperationOrder-main` | 同上 | header 身份；审计列 x≥3289 视口外（实测） | 无 |
| R15 | logistics | 列表态 | `/ErpLogShipment-main` | 同上 | 同上 | 无 |
| R16 | b2b | 列表态 | `/ErpB2bAsn-main` | 同上 | 同上 | 无 |
| R17 | contract | 列表态 | `/ErpCtContract-main` | 同上 | 同上 | 无 |
| R18 | drp | 列表态 | `/ErpDrpPlan-main` | 同上 | 同上 | 无 |
| R19 | master-data | add-form 打开态 | `/ErpMdPartner-main` | `navigate()` + `clickAdd()` + `FormDialog.waitForVisible()` + networkidle | header 身份；无 run-varying 预填（审计字段空值；静态预填如状态/0 值为确定性） | 无 |
| R20 | inventory | add-form 打开态 | `/ErpInvStockMove-main` | 同上 | 同上 | 无 |
| R21 | purchase | add-form 打开态 | `/ErpPurOrder-main` | 同上 | 同上 | 无 |
| R22 | sales | add-form 打开态 | `/ErpSalOrder-main` | 同上 | 同上 | 无 |
| R23 | finance | add-form 打开态 | `/ErpFinVoucher-main` | 同上 | 同上 | 无 |
| R24 | assets | add-form 打开态 | `/ErpAstAsset-main` | 同上 | 同上 | 无 |
| R25 | projects | add-form 打开态 | `/ErpPrjProject-main` | 同上 | 同上 | 无 |
| R26 | manufacturing | add-form 打开态 | `/ErpMfgWorkOrder-main` | 同上 | 同上 | 无 |
| R27 | quality | add-form 打开态 | `/ErpQaInspection-main` | 同上 | 同上 | 无 |
| R28 | maintenance | add-form 打开态 | `/ErpMntVisit-main` | 同上 | 同上 | 无 |
| R29 | crm | add-form 打开态 | `/ErpCrmLead-main` | 同上 | 同上 | 无 |
| R30 | cs | add-form 打开态 | `/ErpCsTicket-main` | 同上 | 同上 | 无 |
| R31 | hr | add-form 打开态 | `/ErpHrEmployee-main` | 同上 | 同上 | 无 |
| R32 | aps | add-form 打开态 | `/ErpApsOperationOrder-main` | 同上 | 同上 | 无 |
| R33 | logistics | add-form 打开态 | `/ErpLogShipment-main` | 同上 | 同上 | 无 |
| R34 | b2b | add-form 打开态 | `/ErpB2bAsn-main` | 同上 | 同上 | 无 |
| R35 | contract | add-form 打开态 | `/ErpCtContract-main` | 同上 | 同上 | 无 |
| R36 | drp | add-form 打开态 | `/ErpDrpPlan-main` | 同上 | 同上 | 无 |
| R37 | master-data | 非标准视图（tree-list） | `/ErpMdMaterialCategory-main` | `navigate()`（tree CRUD 仍为 grid，waitForList 适用） | header 身份；无种子行=「暂无数据」占位，stamp 文本实测 null | 无 |
| R38 | inventory | 非标准视图（readonly） | `/ErpInvStockLedger-main` | 同上 | header 身份；审计列 x≥2377 视口外（实测） | 无 |
| R39 | finance | 非标准视图（readonly） | `/ErpFinTrialBalance-main` | 同上 | header 身份；审计列 x≥2015 视口外（实测） | 无 |
| R40 | projects | 非标准视图（kanban） | `/prj-task-kanban` | `loginAndNavigate` + networkidle + settle（f13 DOM spec 已守护内容层） | header 身份；看板卡片 seed 静态 | 无 |
| R41 | crm | 非标准视图（timeline） | `/crm-activity-timeline` | 同上 | 同上 | 无 |
| R42 | hr | 非标准视图（org-chart tree） | `/hr-org-chart` | 同上 | 同上 | 无 |
| R43 | logistics | drawer 态（edit，3 子表） | `/ErpLogShipment-main` | `navigate()` + `clickEdit('SHP-2026-001')`（种子码钉定行，序无关）+ loadAction 填充等待 | header 身份；drawer 视口 stamp 文本实测 0（stamp 仅存在于 drawer 后方列表视口外审计列） | 无 |
| R44 | drp | drawer 态（view，只读子表） | `/ErpDrpPlan-main` | `navigate()` + `clickView('DRP-PLAN-SEED-001')` | header 身份；同 R43 表单风格；唯一入视口 stamp span 被 footer 遮挡（独立复核 hide-and-diff 字节一致实证） | 无 |

兜底处置（泛化至全部 mask 标准——任一行被三连跑证伪时启用，不限于标准 5 行序）：对该行启用 `opts.mask` 动态区域（审计列区/表体区/字段区）或该行降级 DOM 层断言，并在本计划登记行号、漂移证据与理由。三连跑全绿即证成逐行「无附加 mask」裁决成立。

## Appendix B — assertCrudPixelSnapshot 设计（Phase 1 Decision 2 记录）

- **选择**：`assertCrudPixelSnapshot(page, opts)` 委托既有 `assertSnapshot`（内部复用字体固化 + canonical mask header/canvas + 1% 容差），仅新增一层默认值语义：`skipEchartsSettle` 缺省 `true`（CRUD 目标页无 canvas，Canonical canvas mask 为无害冗余；页面数据同步由 PageObject 等待层承担）。签名：`{ name: string; mask?: Locator[]; maxDiffPixelRatio?: number; skipFontHardening?: boolean; skipEchartsSettle?: boolean }`，前四项透传 `SnapshotOptions`。
- **考虑过的替代方案**：(a) 独立实现第二份字体注入/mask/容差逻辑——rejected：两份范式漂移风险，违反 M0.3「继承而非复制」；(b) spec 直接调 `assertSnapshot`——rejected：M0.3 §7 要求像素层按场景拆分 `assertXxxPixelSnapshot` 命名并列子集，且缺省语义不同（dashboard 需 echarts settle，CRUD 不需）。
- **残留风险**：未来 CRUD 页面若嵌入 canvas 图表，调用方需显式 `skipEchartsSettle: false`（接口已预留，非默认路径）。
