# 2026-07-09-0814-2-business-action-graphql-e2e 代表域业务动作浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-09-0628-2-crud-write-path-list-value-assertions.md` Deferred「复杂业务动作 E2E（审批/状态机/过账触发等 BizModel 自定义动作）」（Successor Required: yes，触发条件「当需浏览器层验证自定义业务动作时」——**已满足**：AGENTS.md 当前重点「各域细化端到端验证」+ 全域业务逻辑已 done（240+ Java 集成测试），但浏览器层（全栈：Quarkus + DB + 种子）业务动作可达性与状态流转尚未验证）
> Related: `docs/plans/2026-07-09-0814-1-seed-sequence-advance-crud-write-e2e.md`（前置：序列推进修复使 GraphQL create 不再碰撞，本计划业务动作 spec 的 create 步骤受益）、`docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md`（Playwright 套件基线）
> Audit: required

## Current Baseline

- **Playwright 套件已成熟（0637-1/1234-2/0628-2/各域 value 断言）**：99 测试覆盖 10 域看板 + 24 域报表页 + 18 域 CRUD 列表/表单 + 28 数值断言 + 13 列表断言 + 1 写路径。所有 spec 经 GraphQL `/graphql` 或 AMIS 页面交互，fresh-DB 种子加载（91 CSV）。
- **业务逻辑全域 done，但仅 Java 层验证**：各域 BizModel 自定义动作（状态机迁移、过账触发、跨域编排）经 240+ Java 集成测试覆盖。这些动作的 GraphQL 可达性（`@BizMutation` 经 `/graphql` 暴露）+ 全栈状态流转（运行中的 Quarkus + H2 + 种子）尚未在浏览器层验证。
- **业务动作 GraphQL 调用范式已部分建立**：`value.spec.ts` 经 GraphQL 调 `@BizQuery`（getDashboardKpi/renderHtml）；`write.spec.ts` 经 GraphQL 调标准 `__save/__update`（CrudBizModel）。但自定义 `@BizMutation`（如 `ErpInvStockMove__confirm`、`ErpCrmLead__qualify`、`ErpCsTicket__assign`）尚未有 spec 调用。
- **候选代表动作已查明（无审批工作流依赖，状态机/过账型）**：
  - inventory `ErpInvStockMoveBizModel`：`generateMove(StockMoveRequest)` → `confirm(moveId)` → `complete(moveId)`（DRAFT→CONFIRMED→DONE + 过账 + 余额更新）；`cancel(moveId)`（→CANCELLED）。`ErpInvStockMoveBizModel.java:34-56`
  - crm `ErpCrmLeadBizModel`：`qualify(leadId)`（NEW→QUALIFIED）、`moveStage(leadId,toStageId)`（漏斗阶段流转）、`cancel(leadId)`（→CANCELLED）、`lose(leadId,...)`（→LOST）。参数简单（Long leadId + 可选 toStageId）。`ErpCrmLeadBizModel.java:60-89`
  - customer-service `ErpCsTicketBizModel`：6 态状态机 `assign/start/resolve/close/reopen/cancel`（6 个状态机 `@BizMutation` + 3 SLA/actions，文件共 9 `@BizMutation`）。`ErpCsTicketBizModel.java:109-247`
- **剩余差距**：业务动作浏览器层 E2E 空白——`@BizMutation` 经 GraphQL 全栈可达性 + 状态流转 + （过账型）下游产物未验证。

## Goals

- 建立业务动作浏览器层 E2E 范式：经 GraphQL 调用 `@BizMutation` 自定义动作，验证状态机迁移 + （过账型）下游产物。
- 覆盖 3 个代表域的非审批状态机/过账动作：inventory StockMove 生命周期（状态机 + 过账 + 余额）、CRM Lead 状态迁移、CS Ticket 6 态状态机。
- 验证这些动作在全栈运行环境（Quarkus + H2 + 种子）下的可达性与正确性，补 Java 集成测试之外的端到端证据。

## Non-Goals

- 审批工作流（xwf）触发的业务动作 E2E——审批经 `use-approval` 迁移至 xwf（plan 2026-07-06-0315-1），浏览器层审批流端到端属独立 successor（触发条件：xwf 浏览器层验证需求时）。
- 全 18 域全业务动作覆盖——3 代表域证明范式，其余域同范式 successor。
- 业财过账凭证精确数值断言——本计划验证过账「触发且产物存在」（posted 翻转 + 凭证生成），凭证借贷平衡精确数值归 finance 数值断言层 successor。
- 跨域编排链完整 E2E（如 P2P 全链 PO→Receive→Invoice→Pay）——归独立 successor（触发条件：跨域编排浏览器层验证需求时）。
- AMIS 页面按钮触发业务动作（UI 按钮点击）——本计划经 GraphQL 调 `@BizMutation`（与 value/list-value/write spec 同范式，稳定可重复）；AMIS 按钮→action 端到端归 successor。

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册）、各域状态机设计文档（`inventory/state-machine.md`、`crm/README.md`、`customer-service/README.md`）
- Skill Selection Basis: `nop-testing`（Playwright E2E + GraphQL mutation 范式）、`nop-backend-dev`（确认 `@BizMutation` GraphQL 签名与输入类型）

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests`
- Node.js + `npm install`
- fresh-DB 重置机制不变
- 受益于 0814-1 序列推进修复（业务动作 spec 的 create 步骤不再碰撞）；若 0814-1 未落地则 warm-up 重试兜底

## Execution Plan

### Phase 1 - 业务动作 E2E helper + 范式确立

Status: planned
Targets: `tests/e2e/business-actions/_helper.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: 无（可与 0814-1 并行，但受益于其序列修复）

- [ ] `Add`：新建业务动作 E2E helper（如 `runBusinessAction`）：经 GraphQL `mutation` 调用 `@BizMutation` 动作，返回结果实体；提供 `createViaSave`（经标准 `__save` 建前置实体）、`callMutation`（调自定义动作）、`verifyState`（经 `__get` 断言状态字段）三个原语。
  - 实现约束：复用 `loginAndNavigate` 或直接 `/graphql` POST（service-public 旁路认证）；动作参数经 GraphQL variables 传入（input 类型经 xbiz 签名确认）；状态字段名经 ORM/xbiz 核实（docStatus/approveStatus/posted 等）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] helper 落地，含 create/callMutation/verifyState 三原语，非空壳（实测 GraphQL 调用逻辑）

### Phase 2 - inventory StockMove 生命周期（状态机 + 过账）

Status: planned
Targets: `tests/e2e/business-actions/inventory-stock-move.action.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：inventory StockMove 业务动作 spec——`generateMove`（建 DRAFT 移动单，StockMoveRequest 引用种子 material/warehouse）→ `confirm`（→CONFIRMED）→ `complete`（→DONE）→ verify `docStatus=DONE` + `posted=true` + 过账产物存在（凭证/余额非空查询）。
  - 异常路径：`cancel`（CONFIRMED→CANCELLED）verify 状态翻转。
  - 实现约束：StockMoveRequest 字段经 xbiz/`IErpInvStockMoveBiz` 签名确认（materialId/warehouseId from+to/qty/moveType 等）；种子 master-data 提供 material（MAT-001 id=1）/warehouse（id=2）固定引用。
  - Skill: `nop-testing`
- [ ] `Proof`：StockMove 动作 spec 全绿（happy path complete + cancel 异常路径）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] StockMove generateMove→confirm→complete 全链绿；docStatus/posted 状态翻转经 `__get` 断言；过账产物存在性验证通过

### Phase 3 - CRM Lead + CS Ticket 状态机

Status: planned
Targets: `tests/e2e/business-actions/crm-lead.action.spec.ts`、`tests/e2e/business-actions/cs-ticket.action.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：CRM Lead 状态机 spec——`__save` 建 NEW lead → `qualify`（→QUALIFIED）→ verify `status=QUALIFIED`；`moveStage`(toStageId) verify `stageId` 翻转（convLog 留痕为下游验证，本 spec 不断言——归 Deferred）；`cancel`（→CANCELLED）verify。
  - 实现约束：lead 必填字段经 ORM 核实；toStageId 引用种子 `erp_crm_stage`（seed 行 id=1,2）。
  - Skill: `nop-testing`
- [ ] `Add`：CS Ticket 状态机 spec——`__save` 建 NEW ticket → `assign` → `start`（IN_PROGRESS）→ `resolve`（RESOLVED）→ `close`（CLOSED）→ verify 各步 docStatus 翻转；非法迁移 ErrorCode 验证（如 CLOSED→start 报错）。
  - 实现约束：ticket 必填字段经 ORM 核实（ticketType 引用种子 `erp_cs_ticket_type` seed 行 id=1,2）；状态字典经 `cs/ticket-status` 确认（6 态 NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED，无 OPEN）。
  - Skill: `nop-testing`
- [ ] `Proof`：CRM Lead + CS Ticket 动作 spec 全绿；既有 99+ 测试无回归。
  - 验证命令：`npx playwright test tests/e2e/business-actions/ --workers=1` + 全套件 `npx playwright test`
  - Skill: `nop-testing`

Exit Criteria:

- [ ] CRM Lead qualify/moveStage/cancel 状态翻转全绿
- [ ] CS Ticket assign→start→resolve→close 状态机全链绿 + 非法迁移 ErrorCode 验证通过

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_0bbc31d41ffeSWCf22furx7Tt`）— 范围/基线/退出标准/protected-area 全通过；4 NON-BLOCKER 建议已采纳：N1 CS Ticket `@BizMutation` 计数修正为「6 状态机 + 3 SLA」、N2 convLog 留痕从「若可查」改为明确非断言（归 Deferred）+ Lead spec 仅断言 status、N3 CS Ticket 状态机无 OPEN 仅 NEW（移除 OPEN）、N4 种子 stage/ticketType 引用补具体 id（1,2）。独立核实 BizModel 签名/种子存在/无既有自定义 `@BizMutation` spec 全确认。无 BLOCKER。

## Closure Gates

- [ ] 范围内行为完成（3 代表域业务动作浏览器层 E2E）
- [ ] 相关文档对齐（`docs/testing/e2e-runbook.md` 增业务动作层 + 套件计数）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 + `npx playwright test` 全套件 0 回归）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 审批工作流（xwf）业务动作 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 审批经 `use-approval` 迁移至 xwf，浏览器层审批流端到端属独立能力面。本计划覆盖非审批状态机/过账动作。
- Successor Required: `yes`
- Trigger Condition: 当需浏览器层验证 xwf 审批→触发→过账端到端时。

### 全 18 域全业务动作覆盖

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 3 代表域（inventory/crm/cs）证明范式。其余域同范式 successor。
- Successor Required: `yes`
- Trigger Condition: 当需按域推进全业务动作浏览器层覆盖时。

### 业财过账凭证精确数值断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划验证过账「触发且产物存在」；凭证借贷平衡精确数值归 finance 数值断言层。
- Successor Required: `yes`
- Trigger Condition: 当需业务动作触发过账后的凭证精确数值断言时。

### 跨域编排链完整 E2E（P2P/O2C 全链）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖单域状态机；跨域编排（PO→Receive→Invoice→Pay）属独立 successor。
- Successor Required: `yes`
- Trigger Condition: 当需浏览器层验证跨域编排全链时。

## Closure

Status Note: <待执行完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理，新会话>
- Evidence: <task id / 日志链接 / 验证记录>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现在此处>
