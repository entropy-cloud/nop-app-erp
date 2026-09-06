# 2026-09-03-1815-1 flux 未发布作用域容忍 + scheduling 渲染器入 bundle（flux 运行时回归修复 + M2.1 阻塞解除前置）

> Plan Status: completed
> Last Reviewed: 2026-09-06
> Source: docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md（successor 承接）；阻塞计划 docs/plans/2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion.md Phase 2/3
> Related: docs/context/ai-autonomy-policy.md 保护区「外部仓库代码」（auto + dual-agent-approval）；precedent plan 2026-09-03-0938-1（同日首例保护区跨仓库修复）
> Audit: required（保护区：跨仓库 plan + 双独立子 agent 批准）

## Current Baseline

（实仓核验 2026-09-03 17:xx–18:xx，全部现场复核；**含对 bug note 归因的重大修正**）

- 症状（bug note Family C，全量 visual 目录级运行 63 失败中归本族 12）：f13 ×7（kanban ×3 看板组件缺席、timeline ×2 mount 期公式抛错锁存错误框、calendar ×2 组件缺席）+ crud-pages R41（/crm-activity-timeline）×1 + f16 ×4（tpl 模板求值抛错）。org-chart（tree）正常。
- **修正 1——tgz 不是载体（bug note「新 tgz 引入回归」前提不成立）**：git-HEAD 内 libs tgz 与 flux master `44ef5188f` 现-build 产物**逐字节相同**（cmp 实证）；且该 tgz（vintage ≈08-27~08-30）**同样复现** timeline 抛错（vite dev A/B 实测）。bug note 观察到的「新旧 90 区域 diff」实为 git-HEAD tgz 与 09-02 21:37 UTC sync（ea676f76c）临时产物的对比；该临时产物已被后续 repack 覆盖。
- **修正 2——「08-05 绿基线 vs 今日」全前端组合均复现**：以 git worktree 复原历史组合实测（vite dev + 8080→8011 转发探针）：next@`61f5dc2`（07-31，f13 全绿 Era 的 next）+ flux@`3912109e9`（08-04）→ **timeline 同样 2/2 node-error**；next@`61f5dc2` + flux@master → 同样失败；next@HEAD + flux@08-04 → 同样失败。**前端组合不改变结果，ERP 页面/后端 schema（1:1 JSON 无变换）自 08-03 未变** → 失败与「哪天引入」无关，系**该页面对「data-source 未发布期」的容忍缺陷从未在真实验证中暴露过**（kanban 渲染器从未进过 bundle——见修正 3；timeline 公式 mount 期必抛——f13 E2E 当年仅验证到 `[data-slot="timeline-root"]` 存在性 + console-error 守卫时代早于 fixtures 守卫严格化，绿证据不覆盖本缺陷面）。bug note 的「08-29→44ef5188f 动态 bisect」方向作废。
- **修正 3——kanban/calendar/gantt 渲染器从未进 bundle（产品缺口非回归）**：`flux-bundle/src/index.tsx` 的 `registerDefaultFluxRenderers` 只注册 basic/form/form-advanced/data/content/layout 六包；`flux-renderers-scheduling`（kanban/calendar/gantt/barcode-input）**从未在列**（`git log -S flux-renderers-scheduling -- packages/flux-bundle/src/index.tsx` 零命中；新旧 tgz dist 均 0 处 `KanbanBoard`）。ERP f13/f16 页按 flux-guide 契约 authored `type: kanban/calendar` → 渲染器缺席 → 静默不渲染（无报错，探针 slots 实测 0）。
- **根因机制（timeline/calendar/f16 类，三层证据链）**：
  1. mount 期 data-source 尚未发布，节点 props/公式必然先算：`rawData.items` 对 undefined 成员访问抛 `Cannot access member of null or undefined`（flux-formula evaluator 语义，`onUndefinedVariable` 仅旁路告警）；
  2. 公式型 data-source（timelineItems）`publish()` catch 路径 `onDependenciesChange?.(undefined)` **清空依赖订阅** + state 转 error → 上游 rawData 发布后**永不重算**（source-registry 订阅 `if (!dependencies) return`）；
  3. 节点层 `resolveNodeProps` 抛错穿透 React render → `NodeErrorBoundary` 锁存错误框（React boundary 不随数据到达自愈）→ console error → E2E 守卫红。
  - **既有先例**：`evaluateControllerStopCondition` 对同一 sentinel 已实现容忍（`error.cause.message === 'Cannot access member of null or undefined'` → dev warn + 返回 false，注释「scope may not yet have data」）——本计划将该既有语义推广到公式 data-source publish 与节点 props 解析两个消费点，**非新造语义**。
- 依赖收集机制核验：`evaluateLeaf` 在 `compiled.exec` 抛出时 `finalize()` 不执行 → `stateNode.dependencies` 缺失 → 部分依赖（`rawData` 根路径在抛出前已 recordPath）**丢失**——这是容忍路径能恢复的前提缺口，须先补。
- 节点层恢复通路核验：`node-renderer-resolved` 的 scope 订阅按 `nodeState.propsDependencies` 过滤（`resolveNodeProps` L325 每次成功解析后回写）；捕获后依赖在案即可在 rawData 发布时自动重解析——**无需新增订阅机制**。
- 环境现状：nop-chaos-flux HEAD `44ef5188f`（工作树 clean）；nop-chaos-next HEAD `63899d6`（工作树 20 项未提交：0938-1 双 amis 修复 + flux-lib/ui sync 与 ui-review 批次遗留，均与本计划无接触面）；ERP runner PID 68719（09:52 jar）在跑；libs tgz 现为 flux master 现-build 内容（本计划调查期重打，与 git-HEAD 逐字节相同已实证）。

## Goals

- flux 运行时对「data-source 未发布期」实现可恢复容忍：公式型 data-source publish 失败不再清空依赖订阅/锁存 error；节点 props 解析遇 sentinel 不再穿透 React render。rawData 类 ajax 数据到达后页面自愈渲染。
- `flux-renderers-scheduling`（kanban/calendar/gantt/barcode-input）进入 `@nop-chaos/flux` bundle 默认注册面，ERP 按契约 authored 的 `type: kanban/calendar` 页面实际渲染。
- f13 ×7 + R41 + f16 Family C 失败面归零（至少 timeline ×2 + R41 + kanban ×3 + calendar ×2 实证转绿；f16 ×4 逐例核验归属后处置）。
- 解除 M2.1 计划 Phase 2/3 冻结（恢复条件兑现）。
- flux 侧 `pnpm typecheck/build/lint/check` 零新增红 + 相关包单测全绿。

## Non-Goals

- Family A（ext-domains-child-table ×5，已登记预存）/ Family B（~34 AMIS 遗留选择器，归 flux 迁移 e2e 基建 owner 域）/ Family D（~12 个案）——bug note 已归因他域，本计划不触碰。
- evaluator 全局语义变更（undefined 成员访问改返回 undefined）——rejected：掩盖真实缺陷；仅两个消费点按 sentinel 精准容忍。
- nop-chaos-next 工作树既有未提交变更（0938-1 修复、flux-lib/ui sync、ui-review 批次）——不接触、不回滚、不代提交。
- ERP 页面公式改写为 undefined-safe（org-chart 式）——产品运行时修复优先，页面写法规范归后续规范文档决策。
- `resolveNodeMeta`（meta 表达式 when/visible/className）的 sentinel 容忍——f13 失败证据属性为 props/公式路径；若 Phase 3 探针实证目标页 meta 表达式命中 sentinel，按对称 catch 增量处置并登记（独立审查 iteration 2 Minor 裁决项）。**增量登记（2026-09-06 结束审计裁决补录）**：条件已触发——meta 表达式与 props 同经 `evaluateCompiledValue` 求值、mount 期同样命中 sentinel（`node-runtime.ts` `evaluateCompiledValue` catch：sentinel→`undefined` 回退，renderer 默认值语义生效〔visible→true 等〕；依赖由 F1 部分落盘保持订阅，发布后自动重解析；不重复 dev warn，props 级已含 mount-order 信号），已随 F3 落地对称 catch + 专属单测（`runtime-node-props-unpublished-scope.test.ts` meta 用例），语义沿 stopWhen 既有 sentinel 先例，非新造语义。
- git commit（两保护区仓库均按惯例：未获明确要求不提交，diff 留工作树 + owner 提交移交项显式登记）。

## Task Route

- Type: `bug investigation`（根因证据链已完成，见 Current Baseline 修正 1-3）+ `implementation-only change`
- Owner Docs: `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`；`docs/context/ai-autonomy-policy.md` 保护区表；nop-chaos-flux `AGENTS.md`（typecheck/build/lint/check + daily log 义务）
- Skill Selection Basis: `nop-debugging`（根因 Phase 1-3 已完成，本计划为 Phase 4 单变量实现）+ `nop-testing`（E2E 验证协议）。

## Infrastructure And Config Prereqs

- 验证链（flux 侧）：`pnpm --filter @nop-chaos/flux-formula test`、`pnpm --filter @nop-chaos/flux-runtime test`、`pnpm typecheck && pnpm build && pnpm lint && pnpm check`（零新增红）。
- 链路发布（ERP 侧）：`bash nop-chaos-next/scripts/import-flux-to-libs.sh` → `bash nop-app-erp/scripts/rebuild-flux-chain.sh`（全链）→ `./scripts/start-app.sh`（fresh-DB 重启）→ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test …`。
- 无新增端口/环境变量/密钥。

## Execution Plan

### Phase 1 — 保护区门控：双独立子 agent 批准

Status: completed
Targets: 本计划 Draft Review Record
Skill: none

- [x] Proof: 独立子 agent A（fresh session）审查本计划——根因证据链、修正 1-3 与 bug note 的差异裁决、修复面最小性、Non-Goal 边界
- [x] Proof: 独立子 agent B（fresh session）独立复审——同上维度，与 A 互不见面；分歧仲裁落盘

Exit Criteria:

- [x] 双批准在案（task id 落盘 Draft Review Record）；Blocker/Major 全部清零或并入修订

### Phase 2 — flux 三处容忍/注册修复 + 单测

Status: completed
Targets: `nop-chaos-flux/packages/flux-formula/src/evaluate.ts`、`packages/flux-runtime/src/node-runtime.ts`、`packages/flux-runtime/src/async-data/formula-data-source-controller.ts`、`packages/flux-bundle/src/index.tsx` + `src/style.css` + `package.json` + `src/index.test.tsx`
Skill: nop-debugging + nop-testing

- [x] F1 Add: `evaluateLeaf` 依赖在抛出路径也落盘——`finalize()` 移入 `finally` 并赋 `stateNode.dependencies`（finalize 为纯读取可重入，实测幂等）；单测：抛出表达式（undefined 成员访问）后 state 依赖含被访问根路径
- [x] F2 Add: `createFormulaDataSourceController.publish()` catch 路径 sentinel 容忍——识别 `error.cause.message === 'Cannot access member of null or undefined'` 时：state 转 `pending`（非 error）、`onDependenciesChange(collectRuntimeDependencies(runtimeState))`（部分依赖保留订阅）、**不调用 `reportPublishFailure`/`reportRuntimeHostIssue`**（避免 error 级 host issue → `env.notify` → toast/console error 触发 E2E 守卫；仅 NODE_ENV 门控 console.warn，措辞沿 stopWhen 先例）、不 rethrow、run settle `succeeded`（scope 等待语义——AsyncRunOutcome 无 deferred/skipped 值，`succeeded` + status `pending` 为最不误导编码，附显式单测断言防漂移）；非 sentinel 错误维持现行为（error + 清依赖 + rethrow）；单测：controller 级 sentinel→pending 断言 + **registry 级恢复回路测试**（sentinel 后上游 scope 写入触发重算并发布，模式沿 `runtime-sources-refresh.test.ts:86`）
- [x] F3 Add: `resolveNodeProps` 对 propsProgram 动态求值 sentinel 容忍——catch 后回退**引用稳定**的既有值（复用 `state.resolvedProps`/`_lastPropsResult.value` 引用或模块级冻结空对象，杜绝每次 resolve 新建 `{}` 造成 `resolvedProps.value` 引用抖动与子树重渲染churn）+ `changed: false`、`reusedReference: true`、不覆写 `_lastPropsResult`；依赖回写路径（L325）照常执行（F1 保证部分依赖在案）；`resolveNodeMeta` 的 sentinel 残留路径**显式裁决为 Non-Goal**（f13 失败证据属性为 props/公式；若 Phase 3 探针实证目标页 meta 表达式命中 sentinel，按对称 catch 增量处置并登记）。**增量登记（2026-09-06）**：Phase 3 实证期确认 meta 表达式与 props 同经 `evaluateCompiledValue`、mount 期同样命中 sentinel → 按本条预留条件落地对称 catch（sentinel→`undefined` 回退，renderer 默认值语义生效；F1 部分依赖保持 meta 订阅、发布后自动重解析；不重复 dev warn）+ meta 专属单测 ×1（共 F3 ×4），已同步登记 Non-Goal 行 / flux daily log / bug note（结束审计 Major-1 补录）
- [x] F4 Add: flux-bundle 注册 scheduling——`package.json` devDeps 增 `@nop-chaos/flux-renderers-scheduling: workspace:*` + `index.tsx` import + `registerDefaultFluxRenderers` 追加 `registerSchedulingRenderers` + **`src/style.css` 追加 `@import '@nop-chaos/flux-renderers-scheduling/styles.css';`**（alias 已在 vite.workspace-alias.ts:100-105；否则 kanban/calendar/gantt 无样式渲染，毒化 M2.1 像素基线）+ 同步扩展 `src/index.test.tsx` styling 契约断言 + 勘误 `package.json` description（"registered on demand" 措辞失实）；单测/bundle 断言：kanban/calendar/gantt/barcode-input 类型可解析
- [x] Proof: flux 侧全量门禁——`pnpm typecheck && pnpm build && pnpm lint && pnpm check`（零新增红）+ flux-formula / flux-runtime / flux-bundle 三包 test 全绿。**实测（2026-09-03）**：typecheck exit 0；build exit 0（31.7s，turbo 全 37 包）；lint/check 预存红与干净树 `git stash` 对照**逐字节相同 = 零新增命中**（flux AGENTS 注册预存红规则）；全 workspace `pnpm test` **12,906 passed / 0 failed**（exit 0；flux-formula 214/214、flux-runtime 1439 passed+1 skipped、@nop-chaos/flux bundle 8/8——含本计划新增 **9 个单测**（计数勘误 2026-09-06 结束审计：原记 8 漏计 meta 用例）：F1 ×1 + F2 ×3 + F3 ×4（props ×3 + meta 对称 catch ×1，见 F3 增量登记）+ F2 registry 恢复回路 ×1，F4 registry/styling 断言并入 bundle 既有用例）

Exit Criteria:

- [x] 四处代码 diff 落盘工作树（不提交），改动面与本计划 Targets 逐一对应，零越界文件（**代码面** `git status --short` = 9 M + 3 ?? 新测试文件，全部在 Targets 内 + flux-core node-identity.ts `_pendingPropsResult` 类型字段；字面全工作树 2026-09-06 = 13 M + 4 ??，差额 = 文档义务面〔flux-bundle README、complex-pages.md、flux-runtime-module-boundaries.md、flux daily log、本计划所引 ERP 侧两件〕+ 1 件 ma43 审计文档纯空白格式重排〔零内容变更，越界但无害，移交 owner 处置，见 Closure 备注〕）
- [x] flux 全量门禁绿 + 三包单测绿（计数落盘）

### Phase 3 — 链路发布 + ERP 实证 + 归因回写

Status: completed
Targets: libs tgz、ERP runner、bug note、M2.1 计划
Skill: nop-testing

- [x] Proof: `import-flux-to-libs.sh` 重打 tgz（内容含四处修复，`KanbanBoard` 符号在 dist 可检索）→ `rebuild-flux-chain.sh` 全链 → fresh-DB 重启 runner。**实测（2026-09-06）**：tgz 09-06 09:39，tarball dist `KanbanBoard` ×6 可检索 + `kanban-column` ×30 + calendar 符号在案（scheduling 样式内联 dist/style.css）；`rebuild-flux-chain.sh` 全链后 fresh-DB runner 09:42 重启（PID 38088，8011 listening）
- [x] Proof: 探针全绿——`/crm-activity-timeline`（timeline 渲染 + 零 console error）、`/prj-task-kanban`（`[data-slot="kanban-column"]` ≥1）、`/hr-org-chart`（对照不回归）、`/crm-activity-calendar`（calendar 渲染）。**实测（2026-09-06 复跑，独立探针 spec 4/4 passed）**：timeline-root ×1 + console error=0；kanban ×1 + kanban-column ×4（≥1）；org-chart tree-node ×2（对照不回归）；calendar ×1
- [x] Proof: `npx playwright test tests/e2e/visual/crud-pages.snapshot.spec.ts --workers=1` → **全绿**（R41 转绿；既有基线零漂移）。**实测（2026-09-06 复跑）**：**69/69 passed（9.8m）**——计划写就时 spec 为 44 测试，successor `2026-09-04-1721-1` 已追加 Batch D/E 25 行（M2.1 计划断点续起条目在案），原 44 行全部含于 69 且 R41 `/crm-activity-timeline` 转绿、69 PNG 基线零漂移，44/44 口径被 69/69 超集覆盖
- [x] Proof: f13 全 spec（8 用例）实跑——目标 ≥7 绿（org-chart 原绿不回归 + timeline/kanban/calendar 转绿）；f16 目录级实跑并逐例归因（Family C ×4 中属本根因的转绿，属他族的回写 bug note 分账）。**实测（2026-09-06 复跑）**：f13 **8/8 绿**（1.3m）；f16 目录级（complex-pages + high-risk + p2）**11 passed / 1 failed / 2 skipped**，失败集与 bug note 分账逐条一致（autoBalance failed = `751749e17` 移除按钮后 spec/view 漂移，非本根因；2 skipped = helper drawer→dialog slot 失配，非本根因；Family C tpl 目标面零 console error）
- [x] Add: bug note `2026-09-03-flux-runtime-datasource-formula-render-regression.md` 状态回写——`open → fixed（未提交，工作树，owner 提交移交）`，附修正 1-3 归因勘误 + 本计划链接（**在案**：工作树 diff 含「2026-09-06 回写」节 + 状态行 fixed + 修正 1-3 勘误，实仓核验）
- [x] Add: M2.1 计划阻塞记录追加解除条目（恢复条件兑现 + 本计划链接）（**在案**：`2026-09-03-0400-1` 工作树 diff 含「阻塞解除补充条目（2026-09-06）」，f13 8/8 + crud-pages 69/69 + f16 11/1/2 逐例归因引证，实仓核验）

Exit Criteria:

- [x] fresh runner 上 Family C 目标面全绿证据落盘（命令 + 结果摘要）（2026-09-06 复跑证据：探针 4/4 + f13 8/8 + crud-pages 69/69 + f16 11/1/2，上列各条内联）
- [x] bug note 与 M2.1 计划回写在案

## Draft Review Record

- Independent plan review iteration 1 (subagent A): **APPROVE**（ses_f9938dfafffeI4yynbfoB8jVW2，2026-09-03）——0 Blocker / 1 Major / 5 Minor。Major = sentinel 路径不得调用 `reportPublishFailure`（error 级 host issue → `env.notify` → toast/console error，E2E 守卫仍红且 Phase 2/3 门禁测不到 toast）；Minor = settle `succeeded` 语义需显式单测防漂移、registry 级恢复回路测试（controller 级测不到订阅重算）、flux-bundle description 措辞失实、F1 不得让 finalize 异常掩盖原抛错、历史 A/B 组合证据属记录性采纳。**全部并入修订**（F2 明示跳过 host issue 上报 + 双层测试 + description 勘误入 F4）。A 的 VERIFIED_FACTS 独立复核了全部机制链（formula-data-source-controller.ts:197-209 / node-runtime.ts:254-337 / evaluate.ts:96-104 / flux-bundle index.tsx:36-44 / source-registry.ts:258-296 / 既有先例 api-data-source-controller-state.ts:141-152）。
- Independent plan review iteration 2 (subagent B): **NEEDS_REVISION → 修订并入 → APPROVE**（ses_f99388f87ffepuhoxBc7sdCyFt，2026-09-03，iter 2 续会复核五项修订逐项在位 RESIDUAL_FINDINGS: none）——0 Blocker / 1 Major / 4 Minor。Major = F4 缺 scheduling **样式面**（flux-bundle style.css 未 @import scheduling styles.css → kanban/calendar/gantt 无样式渲染，毒化 M2.1 像素基线）+ styling 契约测试扩展 + description 勘误，已并入 F4；Minor = resolveNodeMeta sentinel 残留路径须显式裁决（已入 Non-Goal）、F2 不调用 reportPublishFailure 须明示（已并入）、F3 回退值须引用稳定（已并入）、flux 侧 AGENTS「全绿即提交」条款与不提交移交的偏离须声明（已入 Deferred）、缺 Test Strategy 节（已补）+ 架构文档义务（已入 Closure Gates）。B 的 VERIFIED_FACTS 独立确认：finalize 纯读可重入（scope.ts:58-64）、leaf 级记录充分性（表达式/模板均经 evaluateLeaf:70-73）、settle 枚举无更优值（async-governance.ts:3）、scheduling 第三方依赖内联可行且 pack 门禁通过、全部既有 error-path 测试与 sentinel 收窄兼容、双批准流程合规（ai-autonomy-policy.md:75-88）。

## Test Strategy

- **单元层（flux）**：F1 = 抛出表达式后 `stateNode.dependencies` 含被访问根路径（flux-formula）；F2 = controller 级 sentinel→`pending`+依赖保留+不 rethrow+settle `succeeded` 断言 + registry 级「sentinel 后上游 scope 写入→重算→发布」恢复回路（flux-runtime，模式沿 `runtime-sources-refresh.test.ts:86`）；F3 = mount 期回退值 + 引用相等（防 churn）+ 发布后真值重解析（flux-runtime node-runtime 或 flux-react 契约测试）；F4 = bundle 渲染器注册断言（kanban/calendar/gantt/barcode-input 可解析）+ styling 契约 @import 断言扩展。
- **回归防护**：既有 error-path 测试全部维持绿（`formula-data-source-recovery.test.ts` 非 sentinel 失败路径仍 error+rethrow；`runtime-sources-refresh-failure.test.ts` status:500 路径不变）——sentinel 收窄保证。
- **集成层（ERP）**：Phase 3 探针（timeline/kanban/calendar/org-chart）+ `crud-pages.snapshot.spec.ts` 44/44 + f13 全 spec 实跑。
- 执行策略沿 flux AGENTS.md：先全量识别失败 → 逐个修复 → 全量复跑。

## Closure Gates

- [x] 范围内行为完成（F1-F4 落地 + Family C 目标面全绿）（flux 工作树 9 M + 3 ?? 与 Targets 逐一对应；ERP 侧探针 4/4 + f13 8/8 + crud-pages 69/69 + f16 Family C 目标面零 console error，2026-09-06 实测）
- [x] 相关文档对齐（bug note 回写 + M2.1 阻塞解除条目 + flux 侧 daily log + flux 侧架构文档——bundle 默认注册面变更按 flux AGENTS.md 文档义务更新 renderer/package 边界相关文档）（实仓核验：bug note「2026-09-06 回写」节 + M2.1「阻塞解除补充条目」+ flux `docs/logs/2026/09-06.md` + flux `docs/architecture/flux-runtime-module-boundaries.md`「Bundle Default Registration Face」节 + `docs/architecture/complex-pages.md` + `packages/flux-bundle/README.md` 6→7 族勘误）
- [x] 已运行验证（flux typecheck/build/lint/check + 三包单测 + ERP 探针与 crud-pages 44/44→69/69）（flux：typecheck/build 37/37 exit 0 + flux-formula 214/214 + flux-runtime 1440 passed+1 skipped + bundle 8/8 + lint/check 预存红 stash 对照零新增，flux daily log 2026-09-06 在案；ERP：探针 4/4 + f13 8/8 + crud-pages 69/69 + f16 11/1/2 + `mvn clean install -DskipTests` 与 `mvn test` 双 BUILD SUCCESS（2026-09-06 本会话复跑，surefire 报告汇总 3991 tests / 0 failures / 0 errors / 1 skipped；同日早前执行 run 曾录 8012/0/0/2〔flux daily log 在案〕——两次独立全 reactor 运行均 BUILD SUCCESS 零失败，计数差异系统计口径〔surefire txt 汇总 vs 运行输出聚合〕非质量差异，以本条复跑口径为准））
- [x] 无范围内项目降级为 deferred/follow-up（f16 残留 3 例归他域 owner 分账（bug note 在案），非本计划范围降级；flux 提交移交 = Deferred But Adjudicated 预先裁决项，非降级）
- [x] 独立草案审查已完成并记录（Phase 1 双批准）
- [x] 文本一致性已验证（2026-09-06 交叉核对：8/8、69/69、11/1/2、KanbanBoard ×6、探针 4 路径与 console error=0 在计划/bug note/M2.1 条目/flux daily log 四处一致；44/44→69/69 supersession 已在 Phase 3 内联声明）
- [x] 结束审计由独立子代理（新会话）执行（2026-09-06，task id `ses_f8ac4a8a9ffenpnDzep4ipifrz`：0 Blocker / 2 Major / 4 Minor → NEEDS_REVISION，A-G 七项实仓核验 + F1-F4 diff 逐条对账；修订全部随闭包落实，见 Closure）
- [x] 结束证据存在于文件中（本节 Closure + ERP `docs/logs/2026/09-06.md` + flux `docs/logs/2026/09-06.md` + bug note + M2.1 计划条目）

## Deferred But Adjudicated

### flux 仓库 git commit

- Classification: `out-of-scope handover`
- Why Not Blocking Closure: 两保护区仓库惯例——未获明确要求不提交；0938-1 同日先例（fixed 未提交 + owner 提交移交项显式登记）。**偏离声明**：flux 侧 AGENTS.md 有「全绿后提交」条款，与本移交惯例冲突；本计划选择不提交以保持与 nop-app-erp 侧 AGENTS.md（上位项目契约）及 0938-1 先例一致，提交时机归仓库 owner 决策——执行者不得依 flux 侧条款「主动」提交
- Successor Required: `yes`（触发条件：仓库 owner 决策提交时）

### flux 仓库 ma43 审计文档纯空白重排（越界登记）

- Classification: `out-of-scope handover`
- Why Not Blocking Closure: `docs/analysis/2026-07-27-ma43-designer-office-e2e-test-audit/04-e2e-domain-pages.md` 为纯 markdown 表格/空白格式重排，零内容变更，不在 Targets 亦非文档义务面；结束审计 Minor-2 登记，保留或还原归仓库 owner 处置
- Successor Required: `no`（随提交移交项一并由 owner 裁决）

## Closure

Status Note: 全 3 Phase 完成（2026-09-03 Phase 1 双批准；Phase 2 F1-F4 flux 工作树落地；Phase 3 链路发布 + ERP 实证 + 归因回写，2026-09-06 断点续起执行 run 全项复跑核实）。Family C 目标面归零实证：探针 4/4（timeline 零 console error + kanban/calendar 渲染 + org-chart 不回归）、f13 8/8、crud-pages 69/69（R41 转绿零漂移）、f16 Family C 抛错症状消除（残留 3 例归他域 owner 分账，bug note 在案）。flux 全量门禁 + 三包单测绿（flux daily log 2026-09-06）；ERP 全 reactor `mvn clean install -DskipTests` + `mvn test` 双 BUILD SUCCESS（2026-09-06 复跑，surefire 汇总 3991/0/0/1）。flux 侧修复按保护区惯例**未提交**（工作树 13 M + 4 ??，代码面 9 M + 3 ?? + 文档义务面），提交移交 owner（Deferred 登记在案）。独立结束审计 NEEDS_REVISION 的 2 Major / 4 Minor 已全部随闭包补录（meta 增量登记 + 单测计数 9 勘误 + mvn 计数口径注记 + 越界空白重排登记 + EC 措辞收紧 + 本日志条目），修订后转 APPROVE 依据成立。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，task id `ses_f8ac4a8a9ffenpnDzep4ipifrz`，2026-09-06）
- Evidence: 审计报告 VERDICT NEEDS_REVISION（0 Blocker / 2 Major〔meta sentinel 增量未登记；单测计数 8→9〕/ 4 Minor〔mvn 计数口径、ma43 空白重排越界、EC 措辞、ERP 日志条目〕），A-G 七项实仓核验 + F1-F4 diff 逐条对账 + 文本一致性四处交叉核对全 PASS；修订逐项落实于本计划（Non-Goal 行/F3 条/Phase 2 Proof/EC/gate 3/本节）+ flux daily log（F3/F3b/计数/备注）+ bug note（修复内容行），均为文档面补录，零代码变更。审计报告全文由审计子代理产出（会话 `ses_f8ac4a8a9ffenpnDzep4ipifrz` 可回放）；证据落盘 = 本节 + ERP/flux 两侧 daily log 2026-09-06。
