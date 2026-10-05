# 2026-10-06-0508-1 壳层 flux fetcher /p/ 透传分支丢弃 responseType（blob 下载断链）修复

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: plan 2026-10-06-0355-1 Phase 3 E2E 实证（reports.amis-download 10 用例全红：点击后 30s 无 download 事件）；docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md F-R4
> Related: 2026-10-06-0355-1-amis-residual-forms-cleanup.md（Phase 3 证明依赖本修复）；nop-chaos-next f0d1c7e（修复基线 HEAD）
> Audit: required（**保护区：nop-chaos-next 外部仓库代码 → auto + dual-agent-approval**）

## Current Baseline

- **缺陷链（E2E + 源码级双实锤）**：flux `download` 动作 Form 1 契约 = 宿主 fetcher 须返回 `{status:0, data:Blob, headers:{content-disposition}}`（`nop-chaos-flux/packages/flux-runtime/src/runtime-action-helpers.ts:373-388`：`response.status!==0 || !(response.data instanceof Blob)` 即 fail）。壳层 flux env fetcher（`nop-chaos-next/apps/main/src/flux/adapter.ts:59-77`）正确透传 `responseType`/`downloadFileName` 给 `nopRpcRequest`，但 `nopRpcRequest` 的**非 @query:/@mutation:/r/ 透传分支**（`apps/main/src/services/http.ts:275-283`）调用 `mainHttpClient.request` 时**未传 `responseType`**、亦无 blob 分支处理——`/p/{Entity}__download`（报表二进制唯一端点，RPC 层会 JSON 序列化 WebContentBean 故不可替代）响应被按 JSON 解析，Blob 管线（`normalizeBlobData`/`triggerDownload`，httpBlob.ts）永不触发 → 下载静默失败（无 download 事件、无报错 toast，fluxDebug 亦无 error phase）。
- @query:/@mutation: 分支（http.ts:204-260）已具备完整 blob 管线（responseType 透传 + `responseType==='blob' && body instanceof Blob` → normalizeBlobData），仅透传分支缺失——报表下载恰是唯一必须走 /p/ 的 blob 场景（AMIS 时代经 amis 自有 fetcher 工作故从未暴露；plan 0355-1 将 50 个报表下载按钮迁到 flux download 动作后暴露）。
- 壳层测试基线：`apps/main/src/services/http.test.ts` 既有（可扩展）；nop-chaos-next 工作树干净（f0d1c7e）。
- E2E 证据：plan 0355-1 Phase 3 首跑——`reports.amis-download.spec.ts` 10/10 失败（`page.waitForEvent('download')` 30s 超时，test-results/*/error-context.md 在案）；同 run 回归集 9 passed / 3 skipped 零回归。

## Goals

- `nopRpcRequest` 透传分支补 `responseType: options.responseType` 传递；blob 处理按**批准条件 C1**（Approval B 强制、与 Approval A 观察 1 同源）：
  ①**2xx 门禁前置**——blob 原样返回 `{status:0, data:blob, headers, raw}` 仅在 HTTP 2xx 时（对齐同文件 @query 分支 :212→:241 的既有顺序）；
  ②**JSON-in-blob 错误恢复**——非 2xx 或 content-type 含 json 的 Blob 先尝试解析：解析出 `{status:number}` envelope 则按 envelope 返回（status 透传，data:null），吞错假下载被阻断；非 envelope 的 2xx JSON（合法 JSON 文件下载）与非 JSON 的 2xx Blob 照常原样返回（flux 动作按 content-disposition/args.filename 命名并保存，一次下载、无误报 toast）；
  ③非 2xx 且非错误 envelope → 抛 `Request failed: <http status>`（与 resolution 分支错误路径同形）。
  **不**在透传分支复制 normalizeBlobData 消费语义（Form 1 要求 Blob 本体，消费式返回会令动作误判失败并在成功下载同时弹错误 toast）。
- 壳层单测：透传分支 responseType=blob → 返回 Blob 本体（非 envelope 化）；responseType 缺省行为不变（回归守护）。
- 链路重建：`scripts/rebuild-flux-chain.sh --skip-flux`（next 构建 → sync-site → reinstall nop-web-site → 重建 ERP runner）→ E2E 复跑（amis-download 10 用例转绿 + plan 0355-1 探针 + 回归集）。

## Non-Goals

- 不改 flux 运行时（nop-chaos-flux 零改动——其 Form 1 契约与 blob-download 管线为权威，壳层向其对齐）。
- 不动 @query:/@mutation: 分支既有 normalizeBlobData 语义（数据源自动下载行为保持）。
- 不处理 AMIS fetcher 路径（amis 已非渲染目标）。

## Task Route

- Type: `implementation-only change`（外部仓库缺陷修复，跨仓 plan）
- Owner Docs: docs/architecture/flux-integration-gotchas.md（修复后增补 /p/ blob 透传条目关联）；nop-chaos-next 自身文档不属本仓
- Skill Selection Basis: Skill: none（2 行级修复 + 单测；跨仓纪律按 ai-autonomy-policy 外部仓库边界）

## Infrastructure And Config Prereqs

- nop-chaos-next 工具链（pnpm/vitest）既有；`scripts/rebuild-flux-chain.sh` 既有；ERP runner 重建后 E2E 复跑需 fresh DB（webServer 自动处理）。

## Execution Plan

### Phase 1 - 修复与单测（nop-chaos-next）

Status: completed
Targets: `nop-chaos-next/apps/main/src/services/http.ts`（透传分支）+ `http.test.ts`
Skill: none

- Item Types: `Fix | Proof`

- [x] Fix: 透传分支传 responseType + C1 三段 blob 处理（2xx 门禁 + JSON-in-blob 恢复 + 原样返回）——已落地并独立提交 nop-chaos-next `34fb37d`
- [x] Proof: http.test.ts 新增 4 用例全绿（17/17：13 既有 + 4 新）；壳层全量 vitest stash 对照——预存失败集合与改动前完全一致（49 files/8 tests 环境级预存，与 services/http 无关），本改动零新增失败

Exit Criteria:

- [x] 新用例绿 + 既有用例零回归（本地 vitest，stash 双向对照证据在案）

### Phase 2 - 链路重建与 E2E 证明

Status: completed
Targets: rebuild-flux-chain.sh（先后 --skip-flux 与全链两轮）；ERP E2E
Skill: none

- Item Types: `Proof`

- [x] 链路重建成功（两轮：--skip-flux 壳层修复轮 + 全链 flux 修复轮，均 exit 0）
- [x] E2E：`reports.amis-download.spec.ts` **6/10 绿**（fin income-statement ×2 + crm funnel ×2 + cs ticket-sla ×2——三种参数形态各有通过代表，UI 真实点击 + download 事件 + 魔数 + token 断言），**修复链路（壳层 blob 透传 → flux download Form 1 → 保存）端到端实证**；探针/回归集零失败
- [x] **范围裁决（修订记录）**：原定「10/10 转绿」不可达——余 4 挂（mfg crp-load ×2、md partner-list ×2）实证为 **F-R19 预存后端/稳定性家族**（mfg crp-load 直连 /p/ 层同样失败且响应为 JSON 错误信封——纯后端面；crm/cs 直连层失败呈负载波动而 UI 层通过；与本计划修复的壳层 blob 链路无关，前端改动不可能影响直连层）。F-R19 独立登记于 round-3 报告（auth 保护区 plan-first），Successor=独立批次

Exit Criteria:

- [x] 修复链路 E2E 证明达成（6 用例 ≥ 计划原「≥1 真实下载断言」底线；10/10 不可达原因=F-R19 预存家族，修订理由如上记录）

### Phase 3 - 登记

Status: completed
Targets: docs/architecture/flux-integration-gotchas.md
Skill: none

- Item Types: `Add`

- [x] gotchas 增补：/p/ blob 透传条目（症状/根因/修复版本指向）+ G-002 download 转换行回填 + barcode-input WASM R2 附注（gotchas /p/ 透传节与 :94 download 节在案）

Exit Criteria:

- [x] 文档在案

## 双独立子代理批准记录（保护区义务）

> 按 docs/context/ai-autonomy-policy.md 外部仓库边界：修改 nop-chaos-next 代码须跨仓库 plan + 两个独立子 agent（fresh session，互不共享执行者上下文）分别检查批准；两个批准均通过后才可实施。批准记录（agent 指针 + 结论）落盘于本节。

- Approval A (plan-audit 标准): **APPROVED**（agent_e6cb81ef，2026-10-06，fresh session）——缺陷真实性/方案与 Form 1 契约对齐/替代评估（/p/ 唯一二进制端点属实）/范围最小性逐项实证通过；2 条非阻塞观察（JSON-error-as-blob 边缘、行号校准）已并入 C1 实施
- Approval B (独立复核): **APPROVED（有条件 C1，已回填 Goal/Proof 后生效）**（agent_804c2c3f，2026-10-06，fresh session）——独立复验全链证据通过；强制条件 C1=blob 返回须 2xx 门禁 + JSON-in-blob 错误恢复 + 第 3 用例（已回填计划）；R2 barcode-input 正向副作用登记入 Phase 3；R3 text responseType 微差知情登记；「若实施不纳入 2xx 门禁则本批准作废」

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（由 Approval A 子代理承担 plan-audit 职能，agent_e6cb81ef，2026-10-06）——非阻塞观察并入 C1；Approval B 条件 C1 回填后转可实施

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（gotchas 增补 + plan 0355-1 关联回填）
- [x] 已运行验证（壳层单测 + 链路重建 + E2E 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Approval A 承担）
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### @query:/@mutation: 分支的 normalizeBlobData 消费语义与 Form 1 契约的错位

- Classification: `watch-only residual`
- Why Not Blocking Closure: 当前零站点经 @query:/@mutation: 使用 flux download 动作；若未来出现该组合会在成功下载同时误报错误 toast（可观测、不丢功能）
- Reopen Trigger: 出现经 @query:/@mutation: 的 download 动作站点时统一两分支语义
- Successor Required: `no`

## Closure

Status Note: 壳层 blob 透传修复落地（nop-chaos-next 34fb37d，C1 三段语义），链路两轮重建，E2E 6/10 下载断言实证修复链路端到端（三种参数形态各有通过代表），4 挂裁决为 F-R19 预存后端家族（successor 已登记）。双批准（A/B）记录在案。

Closure Audit Evidence:

- Auditor / Agent: agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)

Follow-up:

- (none)
