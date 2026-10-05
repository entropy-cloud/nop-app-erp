# 2026-10-06-0555-1 flux runtime refreshSource 跨 fragment scope 解析失败修复（parent-walk + nameIndex 兜底）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: plan 2026-10-06-0355-1 Phase 3 运行时探针实锤（notify error "Source not found: boardData"，fluxDebug 证据在案）；docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md 新发现 F-R18
> Related: 2026-10-06-0355-1（Phase 3 验收依赖）、2026-10-06-0508-1（同族跨仓修复先例，壳层 http blob）；nop-chaos-flux 修复基线 HEAD（执行时实查）
> Audit: required（**保护区：nop-chaos-flux 外部仓库代码 → auto + dual-agent-approval**）

## Current Baseline

- **缺陷（E2E 运行时实锤）**：flux `refreshSource` 动作携带 `targetId` 时，`runtime.refreshDataSource({name, scope: ctx.scope})`（`packages/flux-runtime/src/action-adapter.ts:511-527`）传入按钮所在 scope；`source-registry.ts:440-448` 对 `args.scope` **仅查该 scope 自己的 bucket（exact-bucket，无父链遍历）**，未命中即 `{found:false}` → 动作失败 notify error「Source not found: boardData」，页面级数据源零重发（fluxDebug 捕获：request 1 次[初始加载] + notify error，点击后零请求）。
- **这是文档化规范用法的运行期不可达**：flux-guide `01-quickstart.md:47,:70`（表单 onRefresh/onSubmitSuccess 刷新页面级 `pagedUsers`）与 `05-data-flow.md:47,:53` 的官方示例即「子 scope（表单）刷新页面级 source」；`04-action-system.md:102` 明文「refreshSource 刷新指定 name 的 data-source」无任何 scope 限制说明。E2E 探针实证 prj kanban 刷新按钮（form 内）刷新页面级 boardData 失败——guide 示例形态在本 runtime 全部不可用。
- **波及面（app 仓）**：①本批 plan 0355-1 转换的 17 个 refreshSource 按钮（16 孪生页）；②第二轮 plan 2026-10-05-0400-1 交付的 26 个 `then: refreshSource` 步骤中跨 fragment scope 的站点（同机制静默失效——各页按钮与数据源的 fragment 归属逐页而异，修复后由按钮探针抽样复验）；③guide 示例的通用形态。
- **既有测试未钉死该行为**：`runtime-sources-dedup.test.ts` 仅覆盖同 scope 命中（:330）与未知名（:351，`{found:false}`）；无「子 scope 找父 scope source」用例——parent-walk 不违背任何已钉死契约。registry 自身持有全局 `nameIndex`（:171），仅当 scope 未传时被咨询——精确 bucket 未命中后连 nameIndex 都不可达，属明显实现缺口。
- **对照**：`refreshNearest`（`refresh-nearest.ts:59-88`）对 component/source 均做 `scope = scope.parent` 父链遍历——refreshSource（具名精确刷新）反而不如 refreshNearest（就近模糊刷新），进一步佐证缺口而非设计。

## Goals

- `source-registry.ts` `refreshDataSource`：`args.scope` 提供时改为**父链最近优先遍历**（同名字段遮蔽语义保持——nearer scope 同名 source 优先），链上未命中再回退全局 `nameIndex`，仍未命中才 `{found:false}`。
- flux 单测新增：①子 fragment scope 刷新父 scope source 命中；② nearer scope 同名遮蔽优先（不误刷父 scope 同名源）；③未知名仍 `{found:false}`（既有用例保持绿）。
- 链路重建（flux 变更须全链：flux build+pack → next build → sync-site → nop-web-site reinstall → ERP runner）→ E2E 复跑：plan 0355-1 探针（kanban 刷新点击重发）转绿 + 抽样 1 个 round-2 then:refreshSource 站点复验 + 回归集零失败。

## Non-Goals

- 不改 refreshNearest / refreshTable / 其他动作语义。
- 不改 guide 文档（其描述即正确行为，runtime 向文档对齐）。
- 不在 app 仓页面侧做绕过（refreshNearest targetType 替代仅适用单源页面，多源页 version-diff 会刷错目标——已评估否决）。

## Task Route

- Type: `bug investigation` → `implementation-only change`（外部仓库缺陷修复，跨仓 plan）
- Owner Docs: docs/architecture/flux-integration-gotchas.md（修复后增补 G-002 关联条目：跨 scope refreshSource 曾不可达）
- Skill Selection Basis: Skill: none（runtime 单点修复 + 单测；跨仓纪律按 ai-autonomy-policy）

## Infrastructure And Config Prereqs

- nop-chaos-flux 工具链（pnpm/vitest）既有；`scripts/rebuild-flux-chain.sh`（**全链，不加 --skip-flux**）。

## Execution Plan

### Phase 1 - 修复与单测（nop-chaos-flux）

Status: completed
Targets: `nop-chaos-flux/packages/flux-runtime/src/async-data/source-registry.ts` + `runtime-sources-dedup.test.ts` + `flux-guide/design-patterns/data-source.md`
Skill: none

- Item Types: `Fix | Proof`

- [x] Fix: refreshDataSource 父链最近优先遍历 + nameIndex 兜底（**C2 落实**：isolate 边界截断遍历并抑制兜底，与 resolveScopePath 读取语义一致；**A-R1 落实**：findFirstInScope 过时注释同步 + refreshDataSource JSDoc 写明解析顺序）——已独立提交 nop-chaos-flux `b18117e97`（严格限定 3 文件路径，无关在途脏文件未触碰[A-R3]）
- [x] Proof: 新增 4 用例全绿（父链命中/就近遮蔽/兄弟 scope nameIndex 兜底[C3]/isolate 边界+隔离内自 scope 命中[C2 用例]）；`pnpm vitest run packages/flux-runtime` stash 对照——289 失败为预存集合与改动前完全一致，+4 通过零新增失败

Exit Criteria:

- [x] 新用例绿 + flux-runtime 既有用例零回归（stash 双向对照证据在案）

### Phase 2 - 全链重建与 E2E 证明

Status: completed
Targets: rebuild-flux-chain.sh（全链）；ERP E2E
Skill: none

- Item Types: `Proof`

- [x] 链路重建成功（flux build+pack → next build → sync-site → nop-web-site reinstall → ERP runner，exit 0）
- [x] E2E：`amis-residual-refresh.action.spec.ts` 2/2 转绿（kanban 刷新点击重发 findBoardData + 结构断言）；回归集（rewired-buttons/f13/pda/menu-filter）同 run 零失败
- [x] ~~抽样复验 disposal-wizard 预览链~~ **范围裁决（修订记录）**：抽样原目标需动态 select 表单自动化（现 E2E 工装不支持自定义 select 选项交互），且机制级证明已由 kanban 探针覆盖（同族拓扑：form 子 scope 按钮 → 页面级 source 经父链命中）；抽样降级为 Deferred（触发条件见下），非静默缩围

Exit Criteria:

- [x] 探针 E2E 绿 + 回归零失败（17 passed/4 failed 运行中 4 挂均为 F-R19 报表下载面，见 0355-1）

### Phase 3 - 登记

Status: completed
Targets: docs/architecture/flux-integration-gotchas.md（**G-003 新条目**[A-R2]）；docs/analysis/2026-10-06-0352 F-R18 状态回填
Skill: none

- Item Types: `Add`

- [x] gotchas G-003：refreshSource 跨 scope 解析（症状/根因/修复 nop-chaos-flux b18117e97）
- [x] round-3 报告 F-R18 回填修复指针

Exit Criteria:

- [x] 文档在案

## 双独立子代理批准记录（保护区义务）

- Approval A (plan-audit 标准): **APPROVED**（agent_01496b5b，2026-10-06，fresh session）——缺陷/文档缺口/方案正确性（逐既有用例核对不变量）/波及面（全仓恰 3 调用方）/范围最小性逐项实证通过；非阻塞 R1（过时注释+JSDoc）/R2（gotchas 以 G-003 新条目，不覆写 G-002）/R3（flux 工作树有无关在途脏文件，提交严格限定路径）均已落实
- Approval B (独立复核): **APPROVED（有条件 C1/C2，均已落实后生效）**（agent_8f3ef7f3，2026-10-06，fresh session）——独立复验全链通过；**C1**=flux-guide/design-patterns/data-source.md:120-131 在案限制文档（plan 513 L4.10）须同步改写为新语义（已改写，component:refresh 保留为等价替代姿势）；**C2**=isolate 语义二选一→选 (a)：walk 在 isolate 边界截断 + 抑制 nameIndex 兜底 + 用例钉死（已实现）；**C3**=补 nameIndex 兜底正路径用例（已补）

## Draft Review Record

- Independent draft review iteration 1: (pending——由 Approval A 子代理承担 plan-audit 职能)

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（gotchas + round-3 报告回填）
- [x] 已运行验证（flux 单测 + 全链重建 + E2E）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### refreshNearest 多源页面首源歧义

- Classification: `watch-only residual`
- Why Not Blocking Closure: refreshNearest 语义=就近第一个，多源页作者应使用具名 refreshSource（修复后跨 scope 可达）；无站点受害
- Reopen Trigger: 出现依赖 refreshNearest 刷新特定多源页面源的站点时评估 targetType 语义扩展
- Successor Required: `no`

### round-2 then:refreshSource 站点 UI 抽样（disposal 预览链）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 机制级证明已由 kanban 探针覆盖（同族拓扑：form 子 scope → 页面级 source 父链命中）；disposal 表单自动化需动态 select 交互（现工装不支持），非修复缺陷信号
- Reopen Trigger: E2E 工装获得 flux select 选项交互能力后补 UI 抽样；或 disposal 向导 E2E 立项时顺带
- Successor Required: `no`

## Closure

Status Note: flux refreshSource scope 链解析修复落地（nop-chaos-flux b18117e97，C1/C2/C3 全落实），E2E 探针 2/2 绿（独立审计复跑确认），+4 单测 stash 对照零漂移，guide 限制文档改写，G-003/F-R18 回填在案。双批准记录在案。

Closure Audit Evidence:

- Auditor / Agent: agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)（本计划审计裁决=ACCEPT 附闭包回填条件，条件已全部实施）

Follow-up:

- (none)
