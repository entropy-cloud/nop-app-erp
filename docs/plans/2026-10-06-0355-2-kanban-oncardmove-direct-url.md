# 2026-10-06-0355-2 三看板 onCardMove /r/ 直发修复

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §3.1 F-R2
> Related: 2026-10-05-0400-1-flux-handwritten-action-rewiring.md（G-002/F13 先例）；docs/architecture/flux-integration-gotchas.md G-002
> Audit: required

## Current Baseline

- 3 个看板 `onCardMove` 用 `@mutation:Entity__method` 标量参数 URL：prj `ErpPrjTask__startTask/completeTask/blockTask/unblockTask`（kanban.flux.yaml:54-62，data 含 taskId/blockReason + 误置的 `i18nEn` 杂键）、cs `ErpCsTicket__assign/start/resolve/close/reopen/cancel` 六分支条件路由（kanban.flux.yaml:83-93，cs-026-r3 注释）、crm `ErpCrmLead__moveStage`（opportunity-kanban.flux.yaml:53-58）。
- 批次 1 已运行时实锤同形态必败：壳层 `nopRpcResolver.ts:274-277` 对未注册 `@mutation:` 回退 `{data:...}` 信封，标量参数操作报 `unknown-operation-arg: 没有定义参数[data]`；同批 9 处标量 mutation 已改 `/r/Entity__method` 直发（resolver `/r/` 分支原样直达）。
- 后端签名全标量：`ErpPrjTaskBizModel.java:114,140` 等、`ErpCsTicketBizModel.java:232-300`、`ErpCrmLeadBizModel.moveStage`。cs `resolve` 的 `resolution` 为 `@Optional` 且 processor null 安全（round-3 报告 §4 修正①）——拖拽 resolve 传 ticketId 单参合法。
- onCardMove 的 ActionSchema 形态（`action: ajax` + `args`）本身正确，仅 URL scheme 错误；E2E 从不拖拽故零暴露。

## Goals

- 三处 onCardMove url 改 `/r/Erp*__method` 直发（保持既有条件路由表达式与分支语义不变，cs 六分支与 prj 四分支逐一保留）。
- 移除 prj data 内误置 `i18nEn` 杂键（mutation 参数面洁化；中文 literals 的 i18nEn 义务按 CJK checker CAT 口径另行安置，不进 mutation data）。
- gotchas G-002 增补：`@mutation:` 陷阱波及事件处理器路径（onCardMove 族）的登记，与按钮族并列。

## Non-Goals

- 不改看板列语义/路由分支（cs-026-r3 既有裁决保持）。
- 不为 cs resolve 拖拽补 resolution 输入交互（resolution 可选，§4 修正①）。
- 不动 nop-chaos-next 壳层（外部保护区）。

## Task Route

- Type: `bug investigation` → `implementation-only change`（缺陷修复）
- Owner Docs: docs/architecture/flux-integration-gotchas.md G-002；各域看板 owner doc（cs-026 路由裁决）
- Skill Selection Basis: Skill: none（机械 URL 形态修复 + 既有 gotchas 登记）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（E2E flux 引擎 + 测试账号种子既有）。

## Execution Plan

### Phase 1 - 改写与登记

Status: completed
Targets: 3 个 kanban/opportunity-kanban *.flux.yaml；docs/architecture/flux-integration-gotchas.md
Skill: none

- Item Types: `Fix | Add`

- [x] Fix: 三处 url 表达式内 `@mutation:` → `/r/`（prj 4 分支 + cs 6 分支 + crm moveStage，条件路由逐分支保留）；prj data 内 i18nEn 杂键移除——blockReason 为 DB payload 非 UI 文案、载体无法随 payload 传递，按 **cjk-baseline.md WHITELIST 文件级登记**（cats [4]，四要素齐备；迭代 1 审查预案的「onClick 兄弟级」对事件处理器不适用——onCardMove 非 button，实际走豁免通道）
- [x] Add: G-002 URL 形态规则增补 onCardMove 族条目（`@mutation:` 陷阱不限于按钮 onClick + data 载荷 i18nEn 纪律）

Exit Criteria:

- [x] 三文件 grep `@mutation:` 零命中（实测 0）
- [x] `npm run validate:flux` errors=325 零新增（实测持平，warnings 21079 持平）

### Phase 2 - 运行时探针定案

Status: completed
Targets: E2E（flux 渲染模式强制）
Skill: none

- Item Types: `Proof`

- [x] 探针：新建 `tests/e2e/business-actions/kanban-oncardmove-r.action.spec.ts` 3/3 绿——①结构断言（三文件零 `@mutation:`、/r/ 在位、prj 载荷无 i18nEn 杂键）；②prj startTask 分支（自建 TODO 任务[种子缺位回退]，`/r/` fetch 200 + rpc 0 + 状态翻转 IN_PROGRESS）；③cs assign/start/resolve/cancel 四分支（建单 → 逐分支 `/r/` fetch → ASSIGNED/IN_PROGRESS/RESOLVED/CANCELLED 状态翻转断言 + 报表防污染清理[actions→survey→ticket]）。crm moveStage 归结构断言面（种子商机/阶段构造重，fetch 语义与 prj/cs 同构）。运行命令=`npm run e2e`
- [x] 既有 E2E 回归零失败（rewired-buttons 1/1 绿、f13 3 skipped[种子依赖，与基线一致]）

Exit Criteria:

- [x] 正对照探针证据在案（prj + cs 四分支真实状态迁移）；回归零失败

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_18b3414c, 2026-10-06) — prj i18nEn 安置层级写死（onClick 兄弟级或 CAT4 豁免）；Deferred 补重开触发；探针指认 f13-kanban-drag spec 落点 → 全部采纳修订
- Independent draft review iteration 2: ACCEPT (agent_e418a158, 2026-10-06) — 迭代 1 全部要求实证级修复核验通过，无残留

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（G-002 onCardMove 族登记）
- [x] 已运行验证（validate:flux 零新增 + 探针 + E2E 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### nopRpcResolver @mutation: 信封回退本身

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 壳层属 nop-chaos-next 外部保护区；本仓侧已按 /r/ 直发规则全量规避
- Reopen Trigger: 上游壳层修复 @mutation: 回退语义时；或新增 @mutation: 标量站点时重审
- Successor Required: `no`

## Closure

Status Note: 三看板 onCardMove 全部 /r/ 直发（结构零 @mutation: 残留），prj/cs 分支 fetch 探针真实状态迁移断言通过，cjk WHITELIST 豁免登记合规（--strict PASS），G-002 onCardMove 族登记在案，回归零失败。

Closure Audit Evidence:

- Auditor / Agent: agent_977187d9（独立结束审计 2026-10-06：**ACCEPT** 附回填条件[已实施]——a-g 七项实证通过；审计者独立复跑 kanban-oncardmove-r 3 passed / rewired-buttons 1 passed / f13 3 skipped 与基线一致 / cjk --strict exit 0 / validate:flux errors=325 warnings=21079）

Follow-up:

- (none)
