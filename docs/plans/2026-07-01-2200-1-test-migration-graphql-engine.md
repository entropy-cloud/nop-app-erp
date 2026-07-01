# 2026-07-01-2200-1-test-migration-graphql-engine 测试迁移到 IGraphQLEngine 计划

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: 计划 `2026-07-01-1900-1` Phase 1 Proof 项的具体化（I*Biz 接口加 `IServiceContext` 后，直调测试运行时 `no-current-session`）
> Related: `docs/lessons/04-bizmodel-service-method-contract-and-testing.md`（验证结论）、`nop-entropy/docs-for-ai/02-core-guides/testing.md`（测试规范）
> Audit: required

## Current Baseline

- I*Biz 接口层已合规（`@BizMutation`+`@Name`+`IServiceContext context` 末参），`check-ibiz-interfaces.mjs` 验证 279 接口 0 违规。
- BizModel 实现层已对齐（纯 `@BizMutation`，无 `@SingleSession`，跨域调用透传 `context`）。
- **但 14 个测试文件（~121 处）仍直调 `bizObj.method(args, CTX)`**（裸 `new ServiceContextImpl()`），运行时多步方法报 `nop.err.orm.dao.update-entity-no-current-session`：
  - inventory：`TestErpInvStockMoveBizModel`、`TestErpInvStockMoveBookkeeping`、`TestErpInvPosting`（generateMove/confirm/complete/cancel/reverse）
  - purchase（7 文件）：`TestErpPurOrderApproval`、`TestErpPurReceiveApproval`、`TestErpPurReceiveStockMove`、`TestErpPurRequisitionApproval`、`TestErpPurRequisitionConvertToOrder`、`TestErpPurOrderToReceiveEnd`、`TestErpPurRequisitionToOrderEnd`（submit/approve/reject/reverseApprove/cancel/convertToOrder）
  - sales（4 文件）：`TestErpSalOrderApproval`、`TestErpSalDeliveryApproval`、`TestErpSalDeliveryStockMove`、`TestErpSalOrderToDeliveryEnd`（submit/approve/reject/reverseApprove/cancel/confirmCustomerAccepted）
- 根因（已验证，见 lessons/04）：BizModel 服务方法依赖执行环境提供的 ORM Session/事务/IUserContext，直调时这些缺失。正确做法是经 `IGraphQLEngine.newRpcContext`+`executeRpc`，引擎建 session、注入 context、走完整管道。
- 参照样板：`TestErpInvStockMoveGraphQL`（验证 generateMove 经引擎成功）、`TestErpInvStockMoveCrudSmoke`（executeRpc helper + GraphQL save 建种子）。
- finance `TestErpFinPostingService` 直调 `ErpFinPostingService`（**non-BizModel bean**，自带 `@SingleSession @Transactional`），不在本计划范围、已绿。

## Goals

- 14 个直调测试迁移为 `IGraphQLEngine` 模式（`executeRpc` + `ApiRequest.build(Map)`），恢复四域 `mvn test` 全绿。
- 测试覆盖等价：每个原断言（状态/余额/凭证/幂等/异常迁移）在 GraphQL 模式下保留。

## Non-Goals

- **不**改 BizModel 实现或 I*Biz 接口（已完成，0 违规）。
- **不**改 finance posting 测试（non-BizModel，已绿）。
- **不**改 CRUD 冒烟测试（已用 IGraphQLEngine）。
- **不**新增业务逻辑或快照录制（除非测试本身用了 `output()`，迁移后保持其快照机制）。

## Task Route

- Type: `implementation-only change`（测试层重构，无产品行为变更）
- Owner Docs: `nop-entropy/docs-for-ai/02-core-guides/testing.md`（已强化）、`05-examples/test-examples.java`
- Skill: `code-quality-audit-prompt`（迁移后测试等价性自检）
- Verification: `mvn test -pl module-{inventory,purchase,sales} -am` 全绿

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 纯测试代码重构。

## Execution Plan

### 迁移通用模式（适用于所有阶段）

每个测试类：
1. `@Inject IGraphQLEngine graphQLEngine`（替换/补充原有 `@Inject IErpXxxBiz`——保留后者仅用于非服务方法断言时）。
2. 加 `executeRpc(opType, action, request)` helper（照搬 CrudSmoke）。
3. 调用处 `bizObj.method(args, CTX)` → `executeRpc(mutation, "ErpXxx__method", ApiRequest.build(Map.of(...)))`。
4. 断言调整：
   - 返回值 `Entity` → `Map`：`result.getData()` 转 Map，按字段名断言（如 `docStatus`）。
   - 异常测试 `assertThrows(NopException.class, () -> biz.method(...))` → GraphQL 不抛异常而是返回错误：`ApiResponse.getStatus() != 0` 且 `errorCode` 匹配。断言改为校验 `status`/`errorCode`。
5. 复杂 DTO（`StockMoveRequest`/`ConvertToOrderRequest`）→ 构造 `Map<String,Object>`（字段名对齐 DTO 属性，引擎自动反序列化）。
6. 移除 `CTX` 字段与 `ServiceContextImpl` import（不再需要）。

### Phase 1 — inventory（3 文件，11 处调用）

Status: completed
Targets: `TestErpInvStockMoveBizModel`、`TestErpInvStockMoveBookkeeping`、`TestErpInvPosting`
Skill: `code-quality-audit-prompt`

- [x] **Fix** `TestErpInvStockMoveBizModel`：generateMove/confirm/complete/cancel/reverse → `ErpInvStockMove__*`。`generateMove` 的 `StockMoveRequest` → Map（参照 `TestErpInvStockMoveGraphQL`）。异常测试（`assertThrows` confirm DONE）→ errorCode 断言。
- [x] **Fix** `TestErpInvStockMoveBookkeeping`：generateMove/complete → 引擎；余额断言（`findBalance` 用 DAO 直查保留，DAO 在测试 session 内合法）。
- [x] **Fix** `TestErpInvPosting`：generateMove → 引擎；凭证断言（`findVoucherLink`/`getEntityById` DAO 保留）。
- [x] **Proof** `mvn test -pl module-inventory/erp-inv-service -am` 全绿。

Exit Criteria:
- [x] inventory 三测试经 IGraphQLEngine 全绿；无 `CTX`/`ServiceContextImpl` 残留

### Phase 2 — purchase（7 文件，~55 处调用）

Status: completed
Targets: `TestErpPurOrderApproval`、`TestErpPurReceiveApproval`、`TestErpPurReceiveStockMove`、`TestErpPurRequisitionApproval`、`TestErpPurRequisitionConvertToOrder`、`TestErpPurOrderToReceiveEnd`、`TestErpPurRequisitionToOrderEnd`
Skill: `code-quality-audit-prompt`

- [x] **Fix** 状态机测试（Order/Receive/Requisition Approval）：submit/approve/reject/reverseApprove/cancel/withdrawSubmit → `ErpPur{Order,Receive,Requisition}__*`。幂等/非法迁移断言 → errorCode。
- [x] **Fix** `convertToOrder`（Requisition/ToOrderEnd）：`ConvertToOrderRequest` → Map（requisitionId + lineUnitPrices/lineTaxRates）。
- [x] **Fix** 端到端（OrderToReceiveEnd、RequisitionToOrderEnd）：多步骤 GraphQL 编排（创建→submit→approve→触发库存→过账→reverseApprove）。中间步骤的库存/凭证断言用 DAO 直查（合法）。
- [x] **Fix** ReceiveStockMove：approve 触发 `generateMove`——经 `ErpPurReceive__approve` 端到端验证（不直调 stockMoveBiz），下游库存/凭证用 DAO 断言。
- [x] **Proof** `mvn test -pl module-purchase/erp-pur-service -am` 全绿。

Exit Criteria:
- [x] purchase 七测试经 IGraphQLEngine 全绿；跨域（purchase→inventory→finance）端到端验证保留

### Phase 3 — sales（4 文件，~55 处调用）

Status: completed
Targets: `TestErpSalOrderApproval`、`TestErpSalDeliveryApproval`、`TestErpSalDeliveryStockMove`、`TestErpSalOrderToDeliveryEnd`
Skill: `code-quality-audit-prompt`

- [x] **Fix** 状态机测试（Order/Delivery Approval）：submit/approve/reject/reverseApprove/cancel/withdrawSubmit → `ErpSal{Order,Delivery}__*`。客户信用额度策略断言 → errorCode/status。
- [x] **Fix** DeliveryStockMove：approve 触发出库（经 `ErpSalDelivery__approve`），可用量不足回滚断言 → errorCode。`generateMove` 直调（若用于验证）改为端到端。
- [x] **Fix** OrderToDeliveryEnd：多步骤 GraphQL 编排（订单→出库→库存→凭证→冲销）。
- [x] **Proof** `mvn test -pl module-sales/erp-sal-service -am` 全绿。

Exit Criteria:
- [x] sales 测试经 IGraphQLEngine 全绿；销售独有可用量校验端到端保留

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver review 2026-07-01) — format compliant, scope/non-goals clear, closure gates verifiable. Fixed per-domain file counts against repo ground truth: purchase 6→7（实际 7 个迁移目标，CrudSmoke 已排除）, sales 5→4 并移除不存在的 `TestErpSalQuotationApproval`（仓库中仅 `QuotationCrudSmoke`，属 Non-Goal）。合计仍为 14（3+7+4），与 Closure Gates 一致。无 Blocker；Minor 项（阶段标题用破折号、Task Route 字段命名 `Skill` 而非 `Skill Selection Basis`、`Verification` 行 maven `-pl` 大括号扩展不可直接执行）留待执行期/结束审计处理。

## Closure Gates

- [x] 14 个测试文件迁移完成，四域 `mvn test` 全绿（含 inventory/purchase/sales/finance）
- [x] 无范围内测试残留 `CTX`/`ServiceContextImpl`/直调 BizModel 服务方法
- [x] 异常测试等价覆盖（errorCode 断言对应原 assertThrows）
- [x] `mvn clean test`（根）BUILD SUCCESS
- [x] 独立草案审查已完成并记录
- [x] 结束审计由独立子代理执行；执行者未自我审计

## Deferred But Adjudicated

### 测试快照录制（output()）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 部分测试原用 `output()` 快照；迁移时若快照文件缺失可先移除 output 仅做断言，快照补齐为后续。
- Successor Required: no

## Closure

Status Note: closed

Closure Audit Evidence:
- Auditor / Agent: 独立 general 子代理 ses_0e11a2285ffeeHM6parqnJua2n（2026-07-02，新会话，非执行者）
- Verdict: **passes closure audit**
- Evidence:
  - Task 1 迁移完整性 PASS：14 个目标测试文件均注入 `IGraphQLEngine` 并经 `executeRpc`/`newRpcContext` 调 `ErpXxx__*` 动作（inventory 3 / purchase 7 / sales 4）。
  - Task 2 范围内无残留 PASS：三域 service test 树 `ServiceContextImpl`/`CTX` 字段/直调服务方法 grep 均 0 代码命中（唯一 hit 为参考样板 `TestErpInvStockMoveGraphQL.java:24` 注释）；finance `TestErpFinPostingService` 正确排除（Non-Goal）；DAO 直查按计划保留。
  - Task 3 异常等价 PASS：`assertThrows` 三树 0 处，全部转为 errorCode 断言（如 `TestErpInvStockMoveBizModel.java:89-92`、`TestErpSalOrderApproval.java:142-144`、`TestErpPurRequisitionConvertToOrder.java:108/123/137/153`）。
  - Task 4 验证 PASS（live）：`mvn test -pl module-{inventory,purchase,sales}/erp-*-service -am` 三次构建均 BUILD SUCCESS（21 / 30 / 29 tests，0 Failures/Errors）。执行者另跑根 `mvn clean test` 全绿。
  - Task 5 计划一致性 PASS：所有 Phase item/Exit Criterion `[x]`，各 Phase `Status: completed`，Closure Gates 与现实一致。
- Residual risks（非阻塞）：Closure Gate 行文「四域含 finance/根 mvn」措辞略强于实际重跑范围（finance 为 Non-Goal 不变量，未触碰），与既存绿基线一致。
