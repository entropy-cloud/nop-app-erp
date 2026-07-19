# 2026-07-17-1005-1-b2b-aps-inbound-scheduling-orchestration-e2e b2b ASN 入站编排 + aps 插单重排浏览器层 E2E

> **PLAN CANCELLED (2026-07-17) — redundant; work already delivered by completed plan 0941-2.**
> 独立草案审查（ses_0922a6ffdffeZRU8meNoVS1Tps，新会话冷重播）裁定本计划三项 0508-1 Deferred 目标已被 `2026-07-14-0941-2-b2b-logistics-aps-orchestration-e2e.md`（completed）全量交付：
>   - b2b ASN→PO 匹配 / retryMatch → `tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`（0941-2 Phase 1）
>   - b2b createReceiveFromAsn 跨域建收 → 同上 spec（8 字段 ErpPurReceive 草稿头断言）
>   - aps insertRushOrder 插单区间重排 → `tests/e2e/business-actions/aps-rush-order.action.spec.ts`（0941-2 Phase 2）
> 本计划 Current Baseline「零浏览器层覆盖」主张**经实时仓库核实为伪**（两 spec 文件已存在）。唯一残留动作为 0508-1 Deferred 段的 RELEASED 归属订正（已就地标注 RELEASED by 0941-2，非本计划成果）+ 可选的单个 createReceiveFromAsn 非法态守卫测试（琐碎本地编辑，无需计划）。
> 本计划取消，不进入 active；保留本文作为审计轨迹。详见下方 Draft Review Record。

> Plan Status: cancelled
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 各域细化端到端验证（b2b 入站编排 + aps 插单编排浏览器层 E2E 深化）
> Source: `docs/plans/2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e.md` Deferred But Adjudicated 三项（Successor Required: yes）：
>   - 「b2b ASN→PO 匹配编排 E2E（matchPurchaseOrder / retryMatch）」（触发条件「ASN→PO 匹配浏览器层 E2E 需求落地时，或 ASN 入站编排 successor 落地时」）
>   - 「b2b createReceiveFromAsn 跨域建入库草稿」（触发条件「ASN→入库编排浏览器层 E2E 需求落地时」）
>   - 「aps insertRushOrder 插单区间重排」（触发条件「插单浏览器层 E2E 需求落地时」）
>
> 三项触发条件均以「按域推进编排链浏览器层覆盖」为口径，AGENTS.md「当前项目阶段」明示当前重点含「各域细化端到端验证」，与本仓近 10 份编排 E2E 计划（0508-1/0941-1/0941-2/0742-2/1218-1/1218-2 等）一致裁定**触发条件已满足**。
> Related: `2026-07-14-0508-1`（DIRECT 状态机本体，已 completed；本计划承接其 3 项编排深度 successor）、`docs/plans/2026-07-11-2329-1-logistics-path2-landed-cost-orchestration.md`（b2b↔purchase 跨域只读编排范式先例）、`docs/testing/e2e-runbook.md`（业务动作表）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17）：

### 后端编排动作已落地（DIRECT `@BizMutation`，浏览器层 `/graphql` 可达）

- **b2b ASN→PO 匹配**：`module-b2b/erp-b2b-service/.../entity/ErpB2bAsnBizModel.java:123 matchPurchaseOrder(@Name("asnId"), context)`——RECEIVED 态 ASN 跨域只读 `ErpPurOrder`/`ErpPurOrderLine`（按 `relatedBillCode` + 逐行 `materialId` 匹配）→ MATCHED；`:235 retryMatch` 幂等重试委派回 `matchPurchaseOrder`（0508-1 已核实方法签名与跨域依赖方向 b2b→purchase 只读）。
- **b2b 跨域建入库草稿**：`:188 createReceiveFromAsn(@Name("asnId"), context)`——config-gated（`erp-b2b.asn-auto-create-receive`，0508-1 webServer JVM arg 已启用），MATCHED→RECEIVED_TO_STOCK + 跨域建 `ErpPurReceive` 草稿（仅头无行，0941-2 范式）。
- **aps 插单区间重排**：`module-aps/erp-aps-service/.../entity/ErpApsOperationOrderBizModel.java:59 insertRushOrder(@Name("operationOrderId"), context)` → `ErpApsSchedulingProcessor.insertRushOrder`（0508-1 Explore 裁定：`ErpApsOperationOrder.workOrderId/machineId` 无 FK 约束 + `ErpApsSchedulingEngine` 纯算法 POJO capacity=1 无工作中心日历/产能配置依赖，自包含 setup 可达）。

### 浏览器层覆盖缺口（本计划对象）

0508-1 聚焦 DIRECT 状态机本体（EDI 信封出/入站 + shipment advise/complete/cancel + aps publish/archive + 正/反向排产引擎 PLANNED 翻转），将上述三项**编排深度**显式 Deferred（耦合度高/复杂度高，归独立 successor）。当前零浏览器层覆盖：
- b2b ASN→PO 匹配 + retryMatch + createReceiveFromAsn 跨域编排链无 spec。
- aps insertRushOrder 插单区间重排无 spec。

### 既有验证范式（本计划复用，零 helper 新增预期）

- `tests/e2e/business-actions/_helper.ts`（0508-1/0941-2 范式）：`createViaSave`（`__save` 预置状态入口）/ `callMutation`（原始 `@BizMutation`）/ `verifyState`（`__get` 独立断言翻转）/ `findFirst`（0730-2 原语）/ `GraphQLClient`（2246-1 PageObject 中心化）。
- 跨域只读/建单编排范式：0941-2 `b2b-asn-match-receive.spec.ts` 已部分触及 matchPurchaseOrder（MATCHED 断言）+ createReceiveFromAsn（ErpPurReceive 草稿头断言）；本计划承接其 Deferred「ASN→入库编排浏览器层」深度（注：0941-2 覆盖了匹配/建单的存在性断言，但 retryMatch 幂等 + createReceiveFromAsn 完整状态翻转 + 跨域草稿字段精确断言仍是缺口，Phase 1 Explore 将核实与 0941-2 的重叠面，避免重复）。

### 剩余差距

b2b 入站编排链（匹配/重试/跨域建单）+ aps 插单区间重排为 0508-1 三项显式 Deferred，后端齐备但浏览器层零深度覆盖；属当前重点「各域细化端到端验证」的明确 successor 面。

## Goals

- 为 0508-1 三项 Deferred 各交付浏览器层 E2E spec，经 GraphQL `/graphql` 驱动 `@BizMutation`，状态/字段翻转经 `verifyState`（`__get`）独立断言：
  1. b2b ASN→PO 匹配编排（matchPurchaseOrder RECEIVED→MATCHED + retryMatch 幂等 + 非匹配守卫）。
  2. b2b createReceiveFromAsn 跨域建单（MATCHED→RECEIVED_TO_STOCK + `ErpPurReceive` 草稿头跨域回断言 + 已收货/状态守卫）。
  3. aps insertRushOrder 插单区间重排（背景工序 plannedStart 被推移区间可观测 + `SchedulingResult.feasible`/`scheduledOperationIds` 非空）。
- 在 `docs/testing/e2e-runbook.md` 业务动作表补 3 行 + 套件计数更新；`docs/backlog/README.md` +1 done 行。
- 解除 0508-1 三项 Deferred（在 0508-1 Deferred 段补 `**RELEASED by 2026-07-17-1005-1**` 行）。

## Non-Goals

- **不重新实现 0508-1 的 DIRECT 状态机范围**——本计划仅消费侧编排深度 E2E + 测试层，零生产代码/契约/ORM/种子变更预期。
- **不新增编排面后端**——三项后端均已落地；若 Explore 发现某 `@BizMutation` 不可达或有 bug，开显式 successor（不改生产代码即时修，对齐 0941-1 triggerDuePlans 修复先例属执行期豁免，须 Phase 内记录）。
- **不覆盖 logistics DELIVERED 运费过账 / path-2 到岸成本自动建单**——0508-1 裁定经 webhook/轮询事件驱动非浏览器面 mutation 入口，归独立 successor（不同结果面，不并入）。
- **不触及 xwf 审批轴**——经 2330-1 权威裁决浏览器层不可行，触发条件未满足。
- **不做凭证行精确数值断言**——b2b 入站编排/aps 插单均不过账（匹配/建草稿/排程），无 GL 凭证产物可断言；凭证行深度归 0704-1/0742-1/0742-2 范式下的过账 successor。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的编排深度 successor；纯消费侧 + 测试维护，零生产契约变更预期）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构/运行命令/业务动作表）、`docs/design/b2b/asn-processing.md`（ASN 入站编排）、`docs/design/aps/scheduling.md`（插单排程）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1 裁决先例），依技能实质内容判定 `Skill: none`（nop-testing）。Phase 1 Explore 阶段如发现后端不可达需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- webServer JVM args 已含 `erp-b2b.asn-auto-create-receive=true`（0508-1/0941-2 启用），createReceiveFromAsn 可达。无新增端口/环境变量/密钥。

## Execution Plan

### Phase 1 - Explore：后端可达性 + 0941-2 重叠面核实

Status: planned
Targets: `module-b2b/erp-b2b-service/.../ErpB2bAsnBizModel.java`、`module-aps/erp-aps-service/.../ErpApsOperationOrderBizModel.java`、`tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`（0941-2 产物）
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

- [ ] `Proof`：冷核实三方法浏览器层可达性——`matchPurchaseOrder`/`retryMatch`/`createReceiveFromAsn`/`insertRushOrder` 的 `@BizMutation` 注解 + `@Name` 入参 + 返回类型（实体标量 / `SchedulingResult` / `ErpDrpPlan`）对齐 GraphQL 浏览器层调用约束（枚举 String scalar、返回标量无选择集，0941-1/2004-2 先例）。
  - Skill: `nop-debugging`
- [ ] `Decision`：核实 `tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`（0941-2）已覆盖的面，界定本计划 Phase 2 增量（避免重复）：retryMatch 幂等 + createReceiveFromAsn 完整状态翻转 + 跨域 `ErpPurReceive` 草稿头字段精确断言是否仍为缺口。若 0941-2 已全覆盖某子项，将该项移出范围并在 Deferred 记录理由（规则 10）。
  - Skill: none

Exit Criteria:

- [ ] 三方法浏览器层可达性冷核实结论（含 `@BizMutation`/`@Name`/返回类型约束）记录入计划
- [ ] 0941-2 重叠面裁决：明确 Phase 2 的净增量 spec 边界

---

### Phase 2 - spec 落地 + 全套件回归

Status: planned
Targets: `tests/e2e/business-actions/b2b-asn-match.action.spec.ts`、`tests/e2e/business-actions/b2b-asn-create-receive.action.spec.ts`、`tests/e2e/business-actions/aps-rush-order.action.spec.ts`（文件名按 Phase 1 裁决最终确定）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：b2b ASN→PO 匹配 spec——自包含建 ASN（RECEIVED 态入口）+ 匹配 `ErpPurOrder`（`relatedBillCode` 一致 + 逐行 `materialId` 匹配）→ `matchPurchaseOrder` → `verifyState` MATCHED；`retryMatch` 幂等（二次调用状态不变）；非匹配 PO（materialId 不一致）守卫保持 RECEIVED。
  - Skill: none
- [ ] `Add`：b2b createReceiveFromAsn spec——MATCHED 前置 → `createReceiveFromAsn` → `verifyState` RECEIVED_TO_STOCK + 跨域 `findFirst` 反查 `ErpPurReceive` 草稿头（`code`/`supplierId`/`relatedBillType`/`relatedBillCode` 精确断言，仅头无行）+ 非法态守卫。
  - Skill: none
- [ ] `Add`：aps insertRushOrder spec——自包含建 machineId + 背景工序 priority=50 `scheduleForward`→PLANNED（`plannedStart` 锚定）+ 急单 priority=10 同 machineId 窗口重叠 → `insertRushOrder` → 背景工序 `plannedStart` 被推移区间可观测 + `SchedulingResult.feasible`/`scheduledOperationIds` 非空。
  - Skill: none
- [ ] `Proof`：新增 spec `--workers=1` 全绿 + business-actions 全套件回归 0 新增失败（依赖既有 helper，预期零生产代码变更）。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/b2b-asn-*.action.spec.ts tests/e2e/business-actions/aps-rush-order.action.spec.ts --workers=1` + 全套件抽样回归
  - Skill: none

Exit Criteria:

- [ ] 3 spec 全绿，状态/字段翻转均经 `verifyState`（`__get`）/`findFirst` 独立断言（非仅 mutation 返回值）
- [ ] business-actions 全套件回归 0 新增失败

---

### Phase 3 - 文档对齐 + Deferred RELEASED 登记

Status: planned
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/plans/2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e.md`、`docs/logs/2026/07-17.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] `Add`：`e2e-runbook.md` 业务动作表 +3 行（b2b match/retry、b2b createReceiveFromAsn、aps insertRushOrder）+ 套件计数更新；`backlog/README.md` +1 done 行（2026-07-17-1005-1）。
  - Skill: none
- [ ] `Add`：0508-1 Deferred 三段各补 `**RELEASED by 2026-07-17-1005-1**` 行（触发条件已满足 + 本计划交付证据）；`docs/logs/2026/07-17.md` 增聚合条目（spec 数/验证状态/范围纪律）。
  - Skill: none

Exit Criteria:

- [ ] e2e-runbook + backlog README + 0508-1 RELEASED 登记 + 日志四点落地一致

## Draft Review Record

- Independent draft review iteration 1: **cancel**（独立 general 子代理 `ses_0922a6ffdffeZRU8meNoVS1Tps`，新会话冷重播无执行者/起草者上下文，2026-07-17）。**VERDICT: needs revision（实质 = cancel）**。
  - **B1（plan-killing，已核实）**：三项 0508-1 Deferred 目标已被 completed 计划 0941-2 全量交付——`tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`（0941-2 Phase 1：matchPurchaseOrder RECEIVED→MATCHED + retryMatch 幂等 + createReceiveFromAsn MATCHED→RECEIVED_TO_STOCK + ErpPurReceive 草稿头 8 字段断言 + 非匹配/非 RECEIVED 守卫）+ `tests/e2e/business-actions/aps-rush-order.action.spec.ts`（0941-2 Phase 2：insertRushOrder + SchedulingResult.feasible/scheduledOperationIds + 背景工序 plannedStart 被推移）。本计划 Current Baseline「零浏览器层覆盖」主张**经主代理复核两 spec 文件 + 0941-2 plan line 60-94 确认为伪**。
  - **B2/B3**：本计划 Goal #3 RELEASED 归属将错记 1005-1（实际交付者 0941-2）；line 35「0941-2 仅触及存在性断言」主张亦伪（0941-2 覆盖完整状态翻转 + 幂等 + 字段断言）。
  - Scope-manufacturing 裁决：**redundant / pattern-matching padding**——起草者模式匹配「消费 0508-1 Deferred + 写 E2E spec」形状而未核实目标已被 0941-2 消费。README:65 明示「0941-2 ... 承接 0508-1 Deferred But Adjudicated 四项」，起草遗漏此线索。
  - 裁决：CANCEL。残留动作（0508-1 RELEASED 归属订正 → 已就地执行 + 标注 RELEASED by 0941-2；可选单个非法态守卫测试 → 琐碎本地编辑无需计划）不构成立项依据。
- **结论**：Plan Status 置 `cancelled`（指南状态流：当计划不再以其原始形式拥有实时结束时使用）。不进入 active，不实施。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧编排深度 successor + 测试层（预期零生产契约变更）。结束前运行新增 spec + business-actions 回归 + 后端构建（确认 spec 变更未污染后端）。

- [ ] 范围内行为完成（0508-1 三项 Deferred 各交付浏览器层 E2E + 状态/字段翻转独立断言）
- [ ] 相关文档对齐（e2e-runbook 业务动作表 +3 行 + 套件计数、backlog README done 行、0504... 0508-1 RELEASED 登记、日志）
- [ ] 已运行验证：新增 spec `--workers=1` 全绿 + business-actions 回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认零后端污染）
- [ ] 无范围内项目降级为 deferred/follow-up（疑似生产缺陷须开显式 successor，不得模糊化）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果）。执行期确认后分类。

### createReceiveFromAsn 跨域建单深度（行级回填）

- Classification: `out-of-scope improvement`（待 Explore）
- Why Not Blocking Closure: 0508-1 裁定 createReceiveFromAsn 仅建头无行；行级回填属增强面。本计划聚焦状态翻转 + 草稿头跨域断言。
- Successor Required: `yes`（触发条件：ASN→入库行级回填浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-19-0849-1**：plan `2026-07-19-0849-1-b2b-asn-line-level-receive-fill.md` 全 3 phase 全绿交付——后端 `ErpB2bAsnBizModel.createReceiveFromAsn` 扩展 iterate AsnLine → ErpPurReceiveLine 字段映射（materialId/uoMId 经 ErpMdMaterial 反查 / quantity=shippedQty 优先 / unitPrice/taxRate/orderLineId 经 PO line 反查 / amount=unitPrice×qty HALF_UP scale=4 派生 / warehouseId 复用 receive.warehouseId / lineNo 透传）+ 新 ErrorCode `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED`（materialId null/material 不存在守卫）+ 空白 AsnLine 边界（0 行合法仅建头）+ JUnit +3 用例（multi-line / empty / config-gate）+ 浏览器层 1 新 spec 2 用例（多行映射正路径 2 AsnLine → 2 ReceiveLine + 逐行字段精确数值断言 amount=5×15=75/12×8=96 + 空白 AsnLine 边界对照）；1005-1 Deferred 解除。

### logistics path-2 / 运费过账浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 webhook/轮询事件驱动非浏览器面 mutation 入口（0508-1 裁定）。不同结果面，不并入本计划。
- Successor Required: `yes`（触发条件：运费过账/path-2 到岸成本浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-19-0849-2**：plan `2026-07-19-0849-2-logistics-path2-landed-cost-browser-e2e.md` 全 3 phase 全绿交付——1 新 spec（2 用例）`log-path2-landed-cost-auto-create.action.spec.ts` 覆盖 logistics path-2 采购运费→到岸成本自动创建完整链路（PURCHASE_RECEIPT DELIVERED → generateFreightLandedCost → DRAFT ErpInvLandedCost FREIGHT 费用行）正路径 + freightAmount=0 边界对照；`playwright.config.ts` webServer JVM arg 追加 `-Derp-log.path2-landed-cost-auto-create=true`；1005-1 Deferred 解除。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理（新会话）执行>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
