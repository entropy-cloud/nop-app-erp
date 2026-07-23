# 2026-07-23-1408-3-frontend-ui-roadmap-closure 路线图收尾与回归门控

> Plan Status: active
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §退出标准（line 551-575，未勾选项 F1/F6/Timesheet/Barcode/回归测试）+ `docs/backlog/implementation-roadmap.md`（frontend-ui-roadmap 状态 `planned`）
> Related: `docs/plans/2026-07-23-1408-1-f9-long-tail-cross-doc-navigation.md`（F9 长尾域增量增强 successor，非硬阻塞——F9 退出标准已勾选）；`docs/plans/2026-07-23-1408-2-cross-domain-voucher-back-link.md`（凭证回链增量增强 successor，非硬阻塞）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-23）：

**frontend-ui-roadmap 16 个 F 项中 14 项 status 标 `completed`/`done`，2 项 `partial`（F12/F16）已由后续 plan 收窄至仅 Deferred successor**。退出标准清单（roadmap line 551-575）仍有 5 项未勾选：

| 退出标准 | 当前状态 | 差距性质 |
|---------|---------|---------|
| `[ ] F1: 18 域按钮完整（0 blocker, 0 major）+ visibleOn` | F1 plan `2026-07-19-1122-1` status `completed`（6 phase 全绿）| **疑似 checkbox drift** — 需对账核实 |
| `[ ] F6: 金额/数量/日期千分位格式（xmeta 统一）` | F6 plan `2026-07-19-2200-2` status `completed`（489 col × 17 域）| **疑似 checkbox drift** — 需对账核实 |
| `[ ] Timesheet 周网格共享组件` | F12 §Deferred（归跨域共享组件 successor），P3 | 显式 defer |
| `[ ] Barcode/PDA 扫描交互` | roadmap Non-Goal（line 547，项目 2.x 硬件集成）| 显式 Non-Goal |
| `[ ] 回归测试 npx playwright test 全绿` | 各 F 项 plan 均含局部回归；全量 `npx playwright test` 未作为门控运行 | **需执行** |

**F9/F12 退出标准已勾选**：roadmap line 562（F9）和 line 565（F12）均已 `[x]`。`2026-07-23-1408-1`（F9 长尾域 successor）和 `2026-07-23-1408-2`（凭证回链 successor）是**已勾选退出标准之外的增量增强**，不对应任何未勾选项。本计划与它们的唯一关联是：全量 Playwright 回归门控（Phase 2）应在它们落地后执行，以纳入其新增 spec；但若它们尚未完成，本计划 Phase 2 可先执行（已含 successor-scope 失败分类路径 96c），不构成硬阻塞。

**F12/F16 partial 状态**：F12 15/16 done（1 Deferred: Timesheet 周网格）；F16 全部 done 或 Deferred（inventory PDA 归 Non-Goal successor，maintenance 向导已落地）。

**剩余差距**：
1. F1/F6 退出标准 checkbox drift 需对账（核实 plan 已交付 + 实时仓库无残留 blocker/格式缺口）
2. 全量 `npx playwright test` 回归门控从未作为整体执行（各 plan 仅跑局部 spec）
3. roadmap 退出标准清单需勾选 + 显式 deferral 文档化（Timesheet P3 / Barcode Non-Goal / F4 Tier 2 ORM-blocked）
4. frontend-ui-roadmap 整体状态需从 `planned` 推进至 `done`

## Goals

1. **F1/F6 退出标准对账**：核实 F1（0 blocker/0 major 按钮 + visibleOn）和 F6（千分位格式）plan 已交付且实时仓库无残留缺口；确认完成后勾选退出标准；若发现实质 gap 则升级独立 Fix plan（规则 13：已确认的实时缺陷不可降级为 follow-up）
2. **全量 Playwright 回归门控**：执行 `npx playwright test` 全量套件，确认全绿（或记录已知非回归环境问题）
3. **退出标准清单收尾**：勾选所有已完成项；显式文档化 Timesheet（P3 defer）/ Barcode（Non-Goal）/ F4 Tier 2（ORM-blocked defer）
4. **roadmap 状态推进**：`frontend-ui-roadmap.md` 状态从 `planned` 推进至 `done`；`implementation-roadmap.md` 更新

## Non-Goals

- 实施任何新前端功能——本计划仅验证 + 对账 + 文档收尾
- 修复 F1/F6 对账中发现的 gap（若有）——若对账发现实质 gap，升级为独立 Fix plan（规则 13 不可降级项）；本计划仅覆盖「确认完成」路径
- Timesheet 周网格共享组件实现——P3 defer，归独立 successor
- Barcode/PDA 硬件集成——项目 2.x Non-Goal
- F4 P3 Tier 2 ORM cascade-delete——ORM 保护区域，需人工批准
- 像素级视觉回归基线更新——已有 plan `2026-07-17-2010-2` 覆盖

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/backlog/frontend-ui-roadmap.md`（退出标准清单）、`docs/backlog/implementation-roadmap.md`、`docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1）、`docs/plans/2026-07-19-2200-2-f6-field-formatting-xmeta.md`（F6）
- Skill Selection Basis: `nop-frontend-dev`（view.xml 审计辅助）；`nop-testing`（回归测试执行）；`nop-debugging` 不适用（除非对账发现 defect）

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. Playwright 已配置（`playwright.config.ts` + webServer JVM args + 种子 CSV 全就绪）。

## Execution Plan

### Phase 1 — F1/F6 退出标准对账

Status: planned
Targets: `module-*/erp-*-web/**/Erp*.view.xml`（F1 按钮审计 + F6 格式审计）+ `docs/backlog/frontend-ui-roadmap.md` 退出标准清单
Skill: `nop-frontend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无（与 successor plan 1408-1/1408-2 可并行对账）

- [ ] Proof: F1 对账——核实 F1 plan `2026-07-19-1122-1` 交付物（25 blocker + 12 major 实体按钮 + visibleOn）在实时仓库中存在且无回退；抽样审计 18 域代表实体确认 0 blocker / 0 major 残留
      - Skill: `nop-frontend-dev`
- [ ] Proof: F6 对账——核实 F6 plan `2026-07-19-2200-2` 交付物（489 col × 17 域千分位格式）在实时仓库中存在且无回退；抽样审计金额/数量列确认 `gen-control` number 格式在位
  - Skill: `nop-frontend-dev`
- [ ] Decision: 若对账发现 F1/F6 残留 gap——本计划为纯验证工作（规则 4 单结果面），任何已确认的实时缺陷/契约漂移均升级独立 Fix plan（规则 13 不可降级项），本计划退出标准保持未勾选直至 Fix plan 闭环；仅当对账结论为「确认完成」时勾选退出标准
  - Skill: `none`

Exit Criteria:

> Phase 1 产出 F1/F6 对账结论（确认完成 / 发现 gap）。若确认完成，退出标准可勾选。

- [ ] F1 对账结论已记录（0 blocker / 0 major 残留 or gap 清单）
- [ ] F6 对账结论已记录（千分位格式在位 or gap 清单）
- [ ] 若发现 gap，Decision 已裁决处理路径

### Phase 2 — 全量 Playwright 回归门控

Status: planned
Targets: `tests/e2e/**`（全量 spec 套件）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 对账完成；successor plan 1408-1/1408-2 若已完成则其新增 spec 纳入回归（若未完成不构成硬阻塞——本计划 Phase 2 仅验证当前 live 基线，不依赖增量功能）

- [ ] 执行全量 `npx playwright test`，记录通过/失败/跳过数
      - Skill: `nop-testing`
- [ ] 若有失败——分类为 (a) 预存环境问题（如 AMIS 渲染超时）→记录为已知非回归，不影响门控；(b) successor plan 范围（1408-1/1408-2 新增 spec）→交回对应 plan；(c) 确认的回归缺陷→升级独立 Fix plan（规则 13，不在本验证计划内修复）
  - Skill: `nop-testing`
- [ ] Proof: 全量回归结论（全绿 / 已知非回归环境问题清单）
  - Skill: `none`

Exit Criteria:

> Phase 2 产出全量回归结论。全绿或仅预存环境问题（非本计划引入回归）可过门控。

- [ ] 全量 `npx playwright test` 执行结论已记录
- [ ] 若有失败，分类裁决已记录（回归修复 / 预存环境 / successor 范围）

### Phase 3 — 退出标准清单收尾 + roadmap 状态推进

Status: planned
Targets: `docs/backlog/frontend-ui-roadmap.md` 退出标准清单 + `docs/backlog/implementation-roadmap.md`
Skill: `none`

- Item Types: `Add | Decision`
- Prereqs: Phase 1-2 完成

- [ ] 勾选 frontend-ui-roadmap 退出标准清单中已完成项（F1/F6/F9 经 successor 完成/F12/F16/回归测试）
      - Skill: `none`
- [ ] 显式文档化 Timesheet 周网格（P3 defer + successor 触发条件）+ Barcode/PDA（Non-Goal 项目 2.x）+ F4 Tier 2（ORM-blocked defer）
  - Skill: `none`
- [ ] `frontend-ui-roadmap.md` 顶部状态从 `planned` 推进至 `done`（标注残留 defer 项）
  - Skill: `none`
- [ ] `implementation-roadmap.md` 更新 frontend-ui-roadmap 行状态
  - Skill: `none`

Exit Criteria:

- [ ] 退出标准清单所有可勾选项已勾选，残留项有显式 defer/Non-Goal 文档
- [ ] roadmap 状态已推进

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_07263c7a0ffeXVwg9qC6nvvRSF) because F9/F12 退出标准已 `[x]`（非未勾选），不可作为 1408-1/1408-2 硬前置理由；Phase 1 与 Phase 2 前置矛盾（可并行 vs 须先完成）；Non-Goal 与 Phase 1 Decision(a) 在计划内修复 gap 自相矛盾
- Independent draft review iteration 2: needs revision (ses_0725d1d9cffeOCtxc8lXVkcmS5) after 正文修正，但 Related 头仍标「须先完成」与修正后正文矛盾
- Independent draft review iteration 3: accept (ses_0725658d1ffe3md0UlV3c2MoDD) after Related 头修正为「非硬阻塞」；纯验证工作所有 gap 升级独立 Fix plan（规则 13）；Phase 1/2 前置一致

## Closure Gates

> 本计划主要结果面是验证 + 文档收尾。验证命令门控保留（全量 Playwright 回归是核心交付）。

- [ ] F1/F6 退出标准对账完成（确认完成 or gap 升级独立 plan）
- [ ] 全量 `npx playwright test` 已执行并记录结论
- [ ] 退出标准清单已收尾（勾选 + defer 文档化）
- [ ] roadmap 状态已推进
- [ ] 无范围内项目降级为 deferred/follow-up（Timesheet/Barcode/F4-Tier2 为显式移出范围非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Timesheet 周网格共享组件（hr 考勤 + projects 工时）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P3 优先级；Flux 渲染引擎备选（roadmap「Flux 备选」段）。当前 hr/projects 各自经标准 CRUD 表达，周网格交互（行=任务/列=星期/0.5h 步进）属共享组件增强
- Successor Required: `yes`（触发条件：hr 考勤或 projects 工时录入周网格交互需求落地时，或 Flux DSL 接入时）

### Barcode/PDA 扫描交互

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式 Non-Goal（line 547，硬件集成属项目 2.x）
- Successor Required: `no`（除非项目 2.x 启动 PDA 硬件集成）

### F4 P3 Tier 2（9 对配置对 cascade-delete）

- Classification: `watch-only residual`
- Why Not Blocking Closure: ORM 缺 cascade-delete，属 ORM 保护区域（AGENTS.md AI 阻塞条件），需人工批准修改
- Successor Required: `yes`（触发条件：ORM cascade-delete 修改批准后）

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending>
- Evidence: <pending>

Follow-up:

- Timesheet 周网格 successor（P3）
- F4 Tier 2 successor（ORM 批准后）
