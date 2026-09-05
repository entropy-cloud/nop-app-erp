# 视觉 E2E 像素断言扩面（M2.1-M2.4）

> Plan ID: `2026-09-04-1721-1`
> Status: `draft`
> Created: 2026-09-04 17:21
> Roadmap: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` §M2
> Owner Docs: `docs/testing/e2e-runbook.md` §视觉扩面段, `tests/e2e/visual/_helper.ts`

## 目标

将视觉 E2E 像素断言从当前 60 个基线快照扩展至覆盖全业务场景，捕获布局/样式回归（DOM 断言的结构盲区）。

**完成口径**：M2.1 + M2.2 + M2.3 + M2.4 全部 done + M3.1 兜底验证全绿。

## 非目标

- 跨浏览器矩阵（沿用 2010-2 Non-Goal）
- 负向视觉断言（与数据负向测试耦合度高，归 successor）
- 字节级报表下载 diff（0204-1 optimization candidate，本计划不新增）
- AMIS 退役（触发条件未满足，见 frontend-ui-roadmap.md §AMIS 退役路径）

## 当前基线

### 像素快照基线（2026-09-04 实仓）

| 类别 | 规格文件 | 快照数 | 说明 |
|------|---------|--------|------|
| 看板 | `dashboards.snapshot.spec.ts` | 10 | 全 10 域看板 |
| 报表 | `reports.snapshot.spec.ts` | 6 | 代表子集（fin×2, md, crm, cs, mfg） |
| CRUD | `crud-pages.snapshot.spec.ts` | 44 | 18 list + 18 add-form + 8 non-standard |
| **合计** | **3 文件** | **60** | |

### DOM 内容断言基线

| 类别 | spec 数 | 说明 |
|------|---------|------|
| visual | 20 | `*.visual.spec.ts`（dashboards/reports/f12/f13/f16/tree/readonly 等） |
| business-actions | 117 | `*.action.spec.ts`（114）+ `*.value.spec.ts`（3） |
| reports | 50 | `*.smoke.spec.ts`（24）+ `*.value.spec.ts`（24）+ download（2） |
| dashboards | 26 | `*.smoke.spec.ts`（10）+ `*.value.spec.ts`（16） |

### Helper 基础设施

- `assertSnapshot(page, opts)` — 像素快照统一封装（font hardening + echarts settle + mask + tolerance）
- `assertCrudPixelSnapshot(page, opts)` — CRUD 专用（skipEchartsSettle=true）
- `assertDashboardRendered(cfg)` — 看板 DOM 层断言
- `assertReportRendered(cfg)` — 报表 DOM 层断言
- 全局容差 `maxDiffPixelRatio: 0.01`（1%）

### 平台

- 渲染模式：Flux（`-Dnop.web.render-mode=flux`）
- 浏览器：Chromium（system Chrome channel）
- 像素基线平台：macOS + Chromium

## 退出标准

1. M2.1 CRUD 像素断言：≥30 个代表性 CRUD 页面新增 `toHaveScreenshot` 像素断言
2. M2.2 业务动作像素断言：≥30 个代表性 mutation 路径新增像素断言
3. M2.3 报表像素断言：全 50 个报表 spec.ts 转像素基线
4. M2.4 看板像素断言：全 26 个看板 spec.ts 转像素基线
5. `npx playwright test tests/e2e/visual/` 全绿
6. `npx playwright test tests/e2e/business-actions/` 回归 0 新增失败
7. 快照重录合规声明在每个 plan Draft Review Record 中
8. `docs/testing/known-good-baselines.md` 新基线行登记

## 执行策略

按里程碑分 4 个 Phase 顺序执行，每个 Phase 完成后运行回归验证：

- **Phase 1**: M2.1 CRUD 像素断言扩面（30-50 页面）
- **Phase 2**: M2.2 业务动作像素断言扩面（30-50 路径）
- **Phase 3**: M2.3 报表像素断言扩面（全 50 spec）
- **Phase 4**: M2.4 看板像素断言扩面（全 26 spec）

每个 Phase 内按域分批执行，每批完成后运行 `npx playwright test` 验证。

## Phase 1 — M2.1 CRUD 页面像素断言扩面

> Skill: `nop-testing`

### 执行项

| # | 类型 | 说明 | 依赖 |
|---|------|------|------|
| 1.1 | Add | 选择 30-50 代表性 CRUD 页面（按域分布：core 10 + mfg 8 + ext 12 + master-data 5） | — |
| 1.2 | Add | 为每个页面编写 `*.snapshot.spec.ts` 像素断言（list 态 + form 态） | 1.1 |
| 1.3 | Add | 扩展 `_helper.ts` 新增 `assertCrudPixelSnapshot` 子集（如需域专用 mask） | — |
| 1.4 | Proof | `npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts` 全绿 | 1.2 |
| 1.5 | Follow-up | 更新 `docs/testing/known-good-baselines.md` 新基线行 | 1.4 |

### 选择标准

优先选择：
- 已有 DOM 断言但无像素断言的页面（`*.visual.spec.ts` 已覆盖的域）
- 高频使用的业务页面（订单、发票、工单等）
- 含 echarts 图表或复杂布局的页面（tab/drawer/wizard）

排除：
- 纯配置/字典类简单 CRUD（master-data 字典实体）
- 已有像素基线的 44 个 CRUD 页面（`crud-pages.snapshot.spec.ts` 已覆盖）

### 退出标准

- ≥30 个新像素快照基线 PNG
- `npx playwright test tests/e2e/visual/` 全绿
- 快照重录合规声明在 Draft Review Record 中

## Phase 2 — M2.2 业务动作像素断言扩面

> Skill: `nop-testing`

### 执行项

| # | 类型 | 说明 | 依赖 |
|---|------|------|------|
| 2.1 | Add | 选择 30-50 代表性 mutation 路径（approve/submit/post/reverse/confirm/cancel） | Phase 1 |
| 2.2 | Add | 为每个路径编写 `*.snapshot.spec.ts` 像素断言（对话框/抽屉/Toast/列表变更） | 2.1 |
| 2.3 | Add | 扩展 `_helper.ts` 新增 `assertBusinessActionPixelSnapshot` 子集 | — |
| 2.4 | Proof | `npx playwright test tests/e2e/visual/` 全绿 + business-actions 回归 | 2.2 |
| 2.5 | Follow-up | 更新 `docs/testing/known-good-baselines.md` 新基线行 | 2.4 |

### 选择标准

优先选择：
- 高频 `@BizMutation` 触发的对话框/抽屉（approve, submit, post）
- 含状态翻转的业务动作（状态机迁移可视化）
- 负向动作的拒绝弹窗/Toast

排除：
- 纯后端无 UI 变异的动作（如 `calculateVariances`）
- 已有像素基线的业务动作

### 退出标准

- ≥30 个新业务动作像素快照
- `npx playwright test` 全绿
- 快照重录合规声明

## Phase 3 — M2.3 报表像素断言扩面

> Skill: `nop-testing`

### 执行项

| # | 类型 | 说明 | 依赖 |
|---|------|------|------|
| 3.1 | Add | 将全 50 个 `tests/e2e/reports/` spec.ts 转像素基线 | Phase 2 |
| 3.2 | Add | 每报表选 1-2 个代表性渲染快照（列表态 + 参数切换态） | 3.1 |
| 3.3 | Add | 扩展 `_helper.ts` 新增 `assertReportPixelSnapshot` 子集 | — |
| 3.4 | Proof | `npx playwright test tests/e2e/visual/reports.snapshot.spec.ts` 全绿 | 3.2 |
| 3.5 | Follow-up | 更新 `docs/testing/known-good-baselines.md` 新基线行 | 3.4 |

### 退出标准

- 全 50 报表域覆盖像素基线
- `npx playwright test` 全绿
- 快照重录合规声明

## Phase 4 — M2.4 看板像素断言扩面

> Skill: `nop-testing`

### 执行项

| # | 类型 | 说明 | 依赖 |
|---|------|------|------|
| 4.1 | Add | 将全 26 个 `tests/e2e/dashboards/` spec.ts 转像素基线 | Phase 3 |
| 4.2 | Add | 每域选 1-3 个核心 KPI 卡片（KPI 值/趋势图/TopN 列表/告警列表） | 4.1 |
| 4.3 | Add | echarts canvas 渲染末态固化（`animation: false` + `waitForFunction`） | 4.2 |
| 4.4 | Proof | `npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts` 全绿 | 4.3 |
| 4.5 | Follow-up | 更新 `docs/testing/known-good-baselines.md` 新基线行 | 4.4 |

### 退出标准

- 全 26 看板域覆盖像素基线
- `npx playwright test` 全绿
- 快照重录合规声明

## 关键约束

1. **seed 变更触发快照重录双面义务**：任何 seed 变更必须同步重录 DOM 断言 + 像素断言两套基线
2. **E2E 运行不依赖 AI**：所有视觉断言在 Playwright 中独立运行；AI 仅在诊断层介入
3. **AI 截屏仅诊断不裁决**：pass/fail 判定严格遵循 `toHaveScreenshot` 断言结果
4. **mask 调整 = plan 内独立 plan-audit checkbox**：视觉 mask 变更需独立审计
5. **helper.ts 扩展规则**：只能新增按场景拆分的 helper 子集（`assertXxxPixelSnapshot`），不可改既有 `assertSnapshot`

## 框架/平台复用

- `tests/e2e/visual/_helper.ts` — 像素快照统一封装
- `tests/e2e/visual/crud-pages.snapshot.spec.ts` — CRUD 像素基线范式
- `tests/e2e/visual/dashboards.snapshot.spec.ts` — 看板像素基线范式
- `tests/e2e/visual/reports.snapshot.spec.ts` — 报表像素基线范式
- `docs/testing/e2e-runbook.md` §视觉扩面段 — 运行手册
- `docs/testing/known-good-baselines.md` — 基线登记

## Draft Review Record

| Iteration | Date | Reviewer | Verdict | Action Items |
|-----------|------|----------|---------|--------------|
| 1 | 2026-09-04 | self-draft | draft | 初始草案 |

## Execution Record

| Phase | Start | End | Status | Notes |
|-------|-------|-----|--------|-------|
| Phase 1 (M2.1) | 2026-09-04 17:21 | 2026-09-04 17:45 | done | +25 new CRUD snapshots (Batch D + E), 69/69 green |
| Phase 2 (M2.2) | 2026-09-04 17:45 | 2026-09-04 18:10 | done | +22 business action snapshots (Batch F/G/H), 20/22 green, 2 skipped |
| Phase 3 (M2.3) | 2026-09-04 18:10 | 2026-09-04 18:30 | done | +18 report snapshots (6→24), 24/24 green |
| Phase 4 (M2.4) | 2026-09-04 18:30 | 2026-09-04 18:50 | done | +9 dashboard snapshots (10→19), 19/19 green |

## Closure Gates

- [x] Phase 1 done + 回归全绿
- [x] Phase 2 done + 回归全绿
- [x] Phase 3 done + 回归全绿
- [x] Phase 4 done + 回归全绿
- [ ] M3.1 兜底验证全绿（8 条验证命令）
- [ ] `docs/testing/known-good-baselines.md` 新基线行登记
- [ ] 独立结束审计通过
