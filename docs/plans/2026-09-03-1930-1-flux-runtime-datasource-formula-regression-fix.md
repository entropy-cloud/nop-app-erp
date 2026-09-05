# 2026-09-03-1930-1 Flux Runtime Data-Source/Formula Rendering Regression Fix

> Plan Status: completed
> Last Reviewed: 2026-09-03
> Source: docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md
> Related: docs/plans/2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion.md（Phase 2 阻塞解除）
> Audit: required

## Current Baseline

- nop-chaos-flux HEAD: `44ef5188f`（2026-09-03，picker v3.4 终态）
- nop-chaos-next HEAD: 包含旧 flux tgz（`118f4d7`，2026-08-29 已知绿色工件）
- 已知绿色 tgz：nop-chaos-next git commit `118f4d7` 内的 `libs/nop-chaos-flux-0.1.0.tgz`（2.9MB，2026-08-29 19:39 +0800）——已导出至 `/tmp/old-flux.tgz`
- 当前 tgz：`libs/nop-chaos-flux-0.1.0.tgz`（3.9MB，2026-09-03 19:04 重建）
- 症状：data-source 驱动页面（timeline/calendar/kanban）渲染回归——公式 mount 期抛错、看板组件缺席、错误框锁存
- 后端无责：GraphQL 响应 200 数据完整
- 对照实验：`/hr-org-chart` 正常（同为 data-source 结构，消费公式未做成员访问）
- M2.1 计划 Phase 2 Add/Proof 被此回归阻塞
- 已知绿色 flux commit 范围：`c4d1c36c6`（08-31 前最后绿色基线）→ `44ef5188f`（当前 HEAD），约 20 commits

## Goals

- 定位导致 flux runtime data-source/公式渲染回归的提交（动态 bisect）
- 修复 nop-chaos-flux 中的回归，恢复 data-source→scope 发布通路和公式求值容错
- 重建 tgz → 刷新 nop-chaos-next → 全链构建
- E2E 验证：f13 + crud-pages + material-customs 全绿（预期通过集），dashboard/report 无新增回归
- 解除 M2.1 计划 Phase 2 阻塞

## Non-Goals

- 修改 nop-app-erp 业务代码
- 修复已知预存红灯 Family A（ext-domains-child-table）/ Family B（AMIS 遗留选择器）——这些早于回归存在
- 修复 Family D 零散个案（_exploration 超时、party-search-picker、list-query-filter）
- 跨浏览器矩阵验证
- 修改 E2E spec 或测试基础设施

## Task Route

- Type: `bug investigation | implementation-only change`
- Owner Docs: `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`
- Skill Selection Basis: `nop-debugging`（根因定位）+ `nop-testing`（E2E 验证）

## Infrastructure And Config Prereqs

- nop-chaos-flux 仓库：`/Users/abc/app/nop-chaos-flux`（可 `git checkout`、`pnpm build`）
- nop-chaos-next 仓库：`/Users/abc/app/nop-chaos-next`（可 `pnpm refresh:flux`、`pnpm build`）
- nop-chaos-next app 实例：`./scripts/start-app.sh`（fresh-DB，:8011，flux 渲染）
- Playwright Chromium（`channel: 'chrome'`）
- 旧 tgz 已提取至 `/tmp/old-flux.tgz`（从 nop-chaos-next git `118f4d7` 导出）
- 保护区域：nop-chaos-flux / nop-chaos-next 代码修改须 `auto + dual-agent-approval`（跨仓库 plan + 双独立子 agent 分别检查批准）
- 回滚策略：若修复引入新回归或未解决问题，`git revert` fix commit → 重建 tgz → 刷新 nop-chaos-next → 全链构建
- 预估 bisect 迭代次数：~5 次（log₂(20) ≈ 4.3），每次含全链构建约 5-10 min，Phase 1 总计约 30-50 min

## Execution Plan

### Phase 1 — Dynamic Bisect 定位回归提交

Status: planned
Targets: nop-chaos-flux git history（`c4d1c36c6`→`44ef5188f` 区间，约 20 commits）
Skill: nop-debugging

- Item Types: `Fix | Proof | Decision`
- Prereqs: 无
- 预估耗时：30-50 min（含全链构建迭代）

- [ ] Decision: 确认 bisect 好端——从旧 tgz（`/tmp/old-flux.tgz`）解压源码，提取 git 元数据或 commit hash；若无法精确反查，使用 `c4d1c36c6`（`fix(e2e): close all baseline-red`，08-31 前最后绿色基线）作为候选好端，并在 bisect 前先验证：将旧 tgz 解压产物重新 pack → 拷贝到 nop-chaos-next → `repack-flux-and-refresh.sh` → 全链构建 → 探针 f13 spec → 确认旧 tgz 本身产生绿色结果，证成该 commit 为有效好端
      - Skill: nop-debugging
- [ ] Fix: 执行动态 bisect——在 nop-chaos-flux 中 `git bisect start`，以确认的好端为 good、`44ef5188f` 为 bad；每个中间 commit 执行完整迭代循环：
  1. `git checkout <commit>`
  2. `pnpm --filter @nop-chaos/ui build && pnpm pack:flux-bundle`（先构建 ui peer dep，再用 `scripts/pack-flux-bundle.mjs` 打包；非标准 `pnpm pack`）
  3. 拷贝 tgz 到 nop-chaos-next `libs/nop-chaos-flux-0.1.0.tgz`
  4. `bash scripts/repack-flux-and-refresh.sh` 刷新依赖
  5. `pnpm build` 全链构建
  6. 启动 app：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`（或 `./scripts/start-app.sh`）
  7. 等待 :8011 就绪
  8. 探针：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/f13-non-standard-views.visual.spec.ts --workers=1`
  9. 记录 pass/fail → `git bisect good` 或 `git bisect bad`
  10. 停止 app 进程
      - Skill: nop-debugging
- [ ] Decision: 处理 bisect 非收敛——若 bisect 落在 merge commit（如 `5abee6983`）或 WIP commit（`e4bffbcae`/`2d50c8edc`），则：(1) 分别测试 merge 的两个 parent branch；(2) 若两者均 bad 则扩大搜索范围；(3) 若一好一坏则在 bad branch 内继续 bisect
      - Skill: nop-debugging
- [ ] Add: 将 culprit commit hash + 根因假说 + diff 摘要写入本计划 Phase 1 Exit Criteria 区域
      - Skill: none

Exit Criteria:

- [ ] culprit commit hash 落盘本计划
- [ ] 根因假说经至少一项代码级证据支撑（diff 审查或运行时探针）
- [ ] bisect 过程记录（每步 commit + good/bad 判定）落盘

### Phase 2 — 实施修复 + 双独立子 agent 批准（nop-chaos-flux 保护区）

Status: planned
Targets: nop-chaos-flux 源码
Skill: nop-debugging（诊断）→ nop-testing（验证）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] Fix: 根据 Phase 1 根因假说实施修复——候选修复面（以 bisect 结论为准）：
  - (a) 公式求值器对「未发布 data-source 名」的失败路径应可恢复（依赖订阅保留/延迟求值/`onDependenciesChange` 不因求值失败清空）
  - (b) 公式型 data-source start 失败后保留依赖记录待 scope 变更重算
  - (c) 看板渲染器核实 `9a21ad932`（kanban 列头聚合）引入的变更
      - Skill: none（修复方案由 Phase 1 bisect 结论确定，非通用调试流程）
- [ ] Proof: nop-chaos-flux 单元测试全绿（`pnpm test`）
      - Skill: nop-testing
- [ ] **Proof: 双独立子 agent 批准（保护区域门控）**——修复 commit 落盘后、推送前，启动两个独立子 agent（fresh session，互不共享执行者上下文）分别审查修复：
  - 子 agent 1（plan-audit 视角）：审查修复方案与 Phase 1 根因假说的一致性、变更范围合理性、是否引入新风险
  - 子 agent 2（code-quality 视角）：审查修复代码质量、边界条件、是否有遗漏的副作用
  - 两个子 agent 均返回 `APPROVE` 后方可继续；任一返回 `NEEDS-REVISION` 则按反馈修订后重新提交批准
  - 批准记录（agent 指针 + 结论 + task id）落盘本计划 Draft Review Record
      - Skill: none
- [ ] Fix: git commit，提交信息含回归描述和修复机制
      - Skill: none

Exit Criteria:

- [ ] nop-chaos-flux 单元测试全绿
- [ ] 修复 commit 落盘，含清晰的根因和修复描述
- [ ] 双独立子 agent 批准记录在案（两个 `APPROVE` + task id）

### Phase 3 — 重建 tgz + 刷新 nop-chaos-next + 全链构建

Status: planned
Targets: nop-chaos-next `libs/`、全链构建产物
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2（含双 agent 批准）

- [ ] Add: 执行 `bash scripts/rebuild-flux-and-build.sh`（或等效：`repack-flux-and-refresh.sh` + `pnpm build`），完成 tgz 重建 → 依赖刷新 → 全链构建
      - Skill: none
- [ ] Proof: 全链构建 exit 0
      - Skill: none

Exit Criteria:

- [ ] nop-chaos-next `pnpm build` exit 0
- [ ] 新 tgz 已就位且 nop-chaos-next 依赖已刷新

### Phase 4 — E2E 验证 + 回归防护 + M2.1 阻塞解除

Status: planned
Targets: nop-app-erp `tests/e2e/visual/`
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [ ] Proof: 启动 app 实例（fresh-DB，:8011，flux 渲染），等待就绪
      - Skill: nop-testing
- [ ] Proof: **Family C 核心验证**——`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/f13-non-standard-views.visual.spec.ts --workers=1`——看板/timeline/calendar 页面渲染正常，零 pageerror
      - Skill: nop-testing
- [ ] Proof: `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1`——2/2 绿
      - Skill: nop-testing
- [ ] Proof: `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts --workers=1`——44/44 绿
      - Skill: nop-testing
- [ ] Proof: **回归防护**——`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts tests/e2e/visual/reports.snapshot.spec.ts --workers=1`——dashboards（10）+ reports（6）无新增回归（公式修复不应影响已通过的 dashboard/report 像素基线）
      - Skill: nop-testing
- [ ] Proof: **全 visual 目录级运行**——`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/ --workers=1`——预期通过集（23 既有 spec 含 material-customs 修复后 2 用例 + crud-pages 44 用例 + Family C 12 用例修复）全绿；Family A/B 预存红灯计数与基线一致（零新增回归）
      - Skill: nop-testing
- [ ] Add: 更新 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md` 状态为 `fixed`（仅在 Phase 4 全部 Exit Criteria 满足后执行）
      - Skill: none
- [ ] Add: 更新 `docs/plans/2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion.md` Phase 2 阻塞记录——恢复条件已满足，Phase 2 Add/Proof 解冻（仅在 Phase 4 全部 Exit Criteria 满足后执行）
      - Skill: none

Exit Criteria:

- [ ] Family C 12 用例全部通过（f13 × 7 + crud-pages R41 × 1 + f16 × 4）
- [ ] Family A/B 预存红灯计数与修复前基线一致（零新增回归）
- [ ] dashboards + reports 像素基线无新增漂移
- [ ] bug note 状态更新为 fixed（条件性）
- [ ] M2.1 计划阻塞记录更新（条件性）

### Phase 4.5 — 回滚决策点

Status: planned
Targets: nop-chaos-flux fix commit
Skill: none

- Item Types: `Decision`
- Prereqs: Phase 4 运行中

- [ ] Decision: 若 Phase 4 全 visual 目录级运行出现修复前基线中不存在的新失败（非 Family A/B/C/D），执行回滚：`git revert` fix commit → 重建 tgz → 刷新 nop-chaos-next → 全链构建 → 重新评估修复方案
      - Skill: none

Exit Criteria:

- [ ] 新回归处置决策落盘（回滚 or 标记为已知风险）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_f98f6bab7ffeTo7z9Je2nFSHQl，2026-09-03）——1 Blocker：dual-agent-approval 工作流未纳入执行项；3 Major：bisect 好端验证缺失、Phase 4 退出标准混合关注点、无回归防护/回滚策略；3 Minor：Phase 1 退出标准缺显式 item、bug note 状态转换条件未文档化、Skill 标注错位
- Independent draft review iteration 2: needs-revision（独立子代理 ses_f98f6a7b0ffeEWAgkN1a6yzCN1，2026-09-03）——2 Blocker：Phase 1 header 引用不存在 commit `63899d6`、引用不存在脚本 `rebuild-flux-chain.sh`；4 Major：Phase 3 冗余构建步骤、bisect 非收敛处理缺失、Phase 4 验证遗漏 dashboard/report 回归面、无回滚策略；5 Minor：探针缺 app 启动步骤、bisect 预估缺失、Draft Review Record 空、Closure Gates 不一致、无预算估计
- **修订后重审**：两个审计发现的全部 Blocker/Major/Minor 已并入本 v2。关键修正：(1) Phase 2 新增双独立子 agent 批准门控项；(2) Phase 1 header 修正为 `c4d1c36c6`→`44ef5188f`；(3) 移除 `rebuild-flux-chain.sh` 引用；(4) Phase 1 新增旧 tgz 好端验证步骤；(5) Phase 3 简化为单脚本执行；(6) Phase 4 新增 dashboard/report 回归防护 + 全 visual 目录级运行；(7) 新增 Phase 4.5 回滚决策点；(8) Phase 1 新增 bisect 非收敛处理；(9) 全部 Minor 已落实
- Independent draft review iteration 3 (post-revision): **APPROVE**（独立子代理 ses_f98f06908ffej5wq6TYeXoMPXn，2026-09-03，0 Blocker / 0 Major / 0 Minor，全部 16 项修正验证通过）
- Independent draft review iteration 4 (post-revision, technical focus): **APPROVE**（独立子代理 ses_f98f05a97ffeILQkcG27Geuhus，2026-09-03，0 Blocker / 0 Major / 0 Minor——已落实 Phase 1 bisect 循环中 `pnpm --filter @nop-chaos/ui build` 步骤以匹配 canonical `import-flux-to-libs.sh` 流程，技术执行路径验证通过）

## Closure Gates

- [ ] 范围内行为完成（bisect + fix + 双 agent 批准 + rebuild + E2E 验证）
- [ ] 相关文档对齐（bug note 状态、M2.1 阻塞记录）
- [ ] 已运行验证：E2E 全 visual 目录级运行，Family C 归零，Family A/B 无新增，dashboards/reports 无新增漂移
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 保护区域双独立子 agent 批准记录在案（Phase 2）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-chaos-flux 提交区间全量 diff 审查

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: bisect 定位到单点后，其余提交的 diff 审查不阻塞修复
- Successor Required: `no`

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项>
