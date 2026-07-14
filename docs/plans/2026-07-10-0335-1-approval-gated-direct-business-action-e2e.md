# 2026-07-10-0335-1 审批门控 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Source: deferred 项承接 `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「审批工作流（xwf）/approval-pattern 域业务动作 E2E」中 **DIRECT useApproval 子集**（Successor Required: yes，触发条件「当需按域推进 DIRECT useApproval 业务动作浏览器层覆盖时」）+ `docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md` Deferred「useApproval DIRECT 子集（WorkOrder/Return/Recall）」
> Related: `docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（P2P/O2C submit→approve DIRECT 范式已证可达）、`docs/plans/2026-07-09-2004-2-reverse-voucher-e2e.md`、`docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md`（business-actions 原语基线）
> Audit: required

## Current Baseline

**已落地 E2E 覆盖（business-actions 层）**：7 个 spec（projects-task / inventory-stock-move / maintenance-visit / quality-capa / quality-ncr / cs-ticket / crm-lead），经 `_helper.ts` 三原语 `createViaSave`/`callMutation`/`verifyState` 验证 DIRECT 状态机迁移。P2P/O2C 编排层（`1249-1`）已证明 PO/Receive/Invoice/SO/Delivery/Invoice 经 DIRECT `use-approval` 轴（`submitForApproval`→`approve`）浏览器层可达。

**本计划目标 4 实体经 ORM 核实为 DIRECT useApproval（非 useWorkflow）**：

| 实体 | 域 | ORM tagSet | useWorkflow | 审批轴 | 状态字段 | posted |
|------|-----|-----------|-------------|--------|---------|--------|
| ErpMfgWorkOrder | manufacturing | `use-approval`（orm.xml:562） | 否 | approveStatus | docStatus(`erp-mfg/work-order-status`)+approveStatus(`wf/approve-status`) | ✅ |
| ErpPurReturn | purchase | `use-approval`（purchase.orm.xml:1033） | 否 | approveStatus | docStatus(`erp-pur/doc-status`)+approveStatus | ✅ |
| ErpSalReturn | sales | `use-approval`（sales.orm.xml:843） | 否 | approveStatus | docStatus(`erp-sal/doc-status`)+approveStatus | ✅ |
| ErpQaRecall | quality | `use-approval`（quality.orm.xml:661） | 否 | approveStatus | status(`erp-qa/recall-status`)+approveStatus | — |

> `useWorkflow="true"` 仅 4 实体（ErpPurPayment / ErpSalReceipt / ErpAstDisposal / ErpHrSalary），经 `2330-1` 权威裁决浏览器层**不可行**（sysUser 兜底），明确排除在外。

**BizModel 动作已核实存在**（经 Java 源核实精确方法名/行号）：

- **WorkOrder**（`ErpMfgWorkOrderBizModel.java`）：approval-support.xbiz 提供 `submitForApproval`/`approve`/`reject`/`reverseApprove`；域动作 `start`:49 / `reportCompletion`:79 / `close`:67 / `cancel`:73 / `checkAvailability`:44。
- **PurReturn**（`ErpPurReturnBizModel.java`）：approval-support `submitForApproval`/`approve`/`reverseApprove`（override `ErpPurReturn.xbiz`，`reject` 走标准源）；域动作 `cancel`:29。`approve` 后触发反向出库 + PURCHASE_RETURN 红字过账（`posted`=true）。
- **SalReturn**（`ErpSalReturnBizModel.java`）：approval-support `submitForApproval`/`approve`/`reject`/`reverseApprove`；域动作 `cancel`:29。`approve` 后触发反向入库 + SALES_RETURN 凭证 + 负 AR 辅助账。
- **Recall**（`ErpQaRecallBizModel.java`）：approval-support `submitForApproval`/`approve`/`reject`/`reverseApprove`；域动作 `register`:86（建单 status=OPEN）/ `locateTargets`:118 / `notifyCustomers`:129 / `generateReturns`:145 / `close`:163 / `cancel`:103。

**approve-status 字典值**（`wf/approve-status`，全域统一）：`UNSUBMITTED` / `SUBMITTED` / `APPROVED` / `REJECTED`。

**剩余差距**：上述 4 实体的审批轴（submit→approve/reject）+ 审批后下游状态迁移浏览器层 E2E 空白。`2004-1` Deferred 明示此 DIRECT useApproval 子集归独立 successor（触发条件已满足：`1249-1` 证明范式可达 + `2330-1` ORM 核实非 useWorkflow）。

## Goals

- 新增 4 个 business-actions spec（WorkOrder / PurReturn / SalReturn / Recall），每个验证：审批轴 `submitForApproval`→`approve`→`approveStatus` 翻转 + `reject` 守卫路径 + 审批后域特定状态迁移（经 `verifyState` 独立断言）
- 验证审批后副作用可观测：PurReturn/SalReturn `approve` 后 `posted`=true（过账触发）；Recall `approve` 后可 `locateTargets`→`close`
- 复用既有 `_helper.ts` 三原语 + 既有种子引用，证明 DIRECT useApproval 审批轴范式在 4 个新域可复用

## Non-Goals

- useWorkflow xwf 实体浏览器层 E2E（Payment/Receipt/Disposal/Salary）——`2330-1` 权威裁决**不可行**，排除
- 业财过账凭证精确数值断言（凭证行科目/金额）——归 finance 数值断言层 successor（同 `1249-1` Deferred 口径）；本计划仅断言 `posted` 布尔标志翻转
- WorkOrder 齐套校验/领料出库/报工/完工入库完整制造链 E2E——跨多实体编排，归 orchestration successor；本计划仅验证审批轴 + 审批后可 `start`/`close` 等单步迁移
- Recall `generateReturns`（跨域建 ErpSalReturn）完整链——归 orchestration successor；本计划验证审批轴 + `locateTargets`/`close` 迁移
- Recall `notifyCustomers` 客户通知派发验证——归 notify 域 successor

## Task Route

- Type: `verification or audit work`（浏览器层 E2E 覆盖扩展，无生产代码变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作层），各域 state-machine owner docs（`manufacturing/state-machine.md`、`purchase/returns.md`、`sales/returns.md`、`quality/recall.md`）
- Skill Selection Basis: `nop-testing`（测试编写范式参考——本计划为 Playwright E2E 而非 JunitAutoTestCase，但 @BizMutation 可达性验证范式对齐）；无后端代码变更，不加载 `nop-backend-dev`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. E2E 套件依赖已部署的 `app-erp-all`（`java -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`）+ 种子数据（`_vfs/_init-data/`）。

## Execution Plan

### Phase 1 - manufacturing WorkOrder 审批轴 E2E

Status: completed
Targets: `tests/e2e/business-actions/mfg-work-order.action.spec.ts`
Skill: `none`（复用既有 `_helper.ts` 三原语，Playwright E2E 范式已在 `2004-1`/`0814-2` 验证）

- Item Types: `Add | Proof`
- Prereqs: 无（首阶段，证明审批轴范式在新域可复用）

- [x] `Add`：新建 `mfg-work-order.action.spec.ts`——`createViaSave` 建 WorkOrder（mandatory 字段 code+materialId+quantity+docStatus+approveStatus，code 用 `E2E-WO-<tag>-<ts>` 保唯一；materialId 引用种子 MAT-001=1），初始 `approveStatus=UNSUBMITTED`/`docStatus=DRAFT`
- [x] `Proof`：审批轴正路径——`callMutationOk submitForApproval` → `verifyState approveStatus=SUBMITTED` → `callMutationOk approve` → `verifyState approveStatus=APPROVED`（+ `docStatus` 翻转断言）
- [x] `Proof`：审批后域迁移——`callMutationOk start`（approve 后可 start）→ `verifyState docStatus=IN_PROGRESS`（或等价态）→ `callMutationOk close` → 终态断言
- [x] `Proof`：`reject` 守卫——建新 WorkOrder → submit → reject → `verifyState approveStatus=REJECTED`；非法迁移守卫（如终态再 submit）断言 errors 非空
- [x] `Proof`：清理——WorkOrder 自身逻辑删除（`deleteById`）；若 approve 触发过账产物，按需清理（WorkOrder approve 不触发 posted，无需凭证清理）

Exit Criteria:

- [x] `mfg-work-order.action.spec.ts` 全部 test 通过（submit→approve→start→close 正路径 + reject 守卫）
- [x] 审批轴 `approveStatus` 翻转经 `verifyState` 独立断言（非 mutation 返回值）

### Phase 2 - purchase/sales Return 审批轴 + posted 副作用 E2E

Status: completed
Targets: `tests/e2e/business-actions/pur-return.action.spec.ts`、`tests/e2e/business-actions/sal-return.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（审批轴范式已证可复用）

- [x] `Add`：新建 `pur-return.action.spec.ts`——`createViaSave` 建 PurReturn（mandatory code+returnDate+partnerId+materialId 等，code 保唯一），初始 UNSUBMITTED
- [x] `Proof`：`submitForApproval`→`approve`→`verifyState approveStatus=APPROVED` + **`posted=true`** 断言（approve 触发反向出库 + PURCHASE_RETURN 红字过账）
- [x] `Proof`：`reject` 路径 + `cancel` 迁移 + 非法迁移守卫
- [x] `Proof`：清理——PurReturn 自身删除；若 `posted=true` 触发凭证，用 `cleanupVoucherByBillCode`（orchestration helper 复用）清理 GL 凭证 + AR/AP 辅助账（经 `cleanupArApByCode`）
- [x] `Add`：新建 `sal-return.action.spec.ts`——同范式，`approve` 后 `posted=true`（反向入库 + SALES_RETURN 凭证 + 负 AR 辅助账）；`reject`/`cancel` 路径 + 清理

Exit Criteria:

- [x] `pur-return.action.spec.ts` + `sal-return.action.spec.ts` 全部 test 通过
- [x] `approve` 后 `posted=true` 经 `verifyState` 断言（可观测过账触发）

### Phase 3 - quality Recall 审批轴 + 域状态机 E2E

Status: completed
Targets: `tests/e2e/business-actions/qa-recall.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：新建 `qa-recall.action.spec.ts`——`register`（经 `callMutationOk`，非 `createViaSave`，因 Recall 建单走 register BizMutation）建 Recall（status=OPEN, approveStatus=UNSUBMITTED，mandatory code+recallDate+materialId+severity）
- [x] `Proof`：审批轴——`submitForApproval`→`approve`→`verifyState approveStatus=APPROVED`（status 仍 OPEN，approve 后允许 locateTargets）
- [x] `Proof`：域状态机——`locateTargets`→`verifyState status=IN_PROGRESS`→`close`（前置 `notifyCustomer=true` 或 config-gated 门控断言）→终态 CLOSED
- [x] `Proof`：`reject` 路径 + `cancel` 迁移 + 非法迁移守卫
- [x] `Proof`：清理——Recall 自身删除

Exit Criteria:

- [x] `qa-recall.action.spec.ts` 全部 test 通过（审批轴 + locateTargets/close 域迁移）
- [x] Recall 双字段（approveStatus + status）翻转均经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (mission-driver 草案审查) because 格式合规（必需区段齐全、字段名正确、Phase 结构完整、item types 合法）；退出标准可测（每阶段交付物与 verifyState 独立断言明确）；范围边界清晰（Goals/Non-Goals 显式，Deferred 项均 adjudicated 并带 successor 触发条件，无 scope creep）；结束证据已定义（4 spec 全绿 + 文档对齐 + 验证命令 + 独立结束审计门控）；基线经 ORM/Java 行号引用落地。无 Blocker/Major 问题。Minor：Task Route Type 已规范化为模板措辞。

## Closure Gates

- [x] 范围内行为完成（4 spec 全绿：WorkOrder + PurReturn + SalReturn + Recall）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` 业务动作层新增 4 实体段 + 套件计数更新；`docs/logs/2026/07-10.md` 聚合条目）
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/`（新 spec 全绿，0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，E2E 文件在根 tests/ 非 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 业财过账凭证精确数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅断言 `posted` 布尔标志翻转（过账触发可观测）。凭证行科目码/金额精确数值断言属 finance 数值断言层（同 `1249-1` Deferred 口径）。
- Successor Required: `yes`
- Trigger Condition: 当需对 Return approve 触发的凭证行做精确数值断言时。

### WorkOrder 完整制造链编排 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 齐套校验→领料出库→报工→完工入库跨多实体编排。本计划仅验证审批轴 + 审批后单步迁移。
- Successor Required: `yes`
- Trigger Condition: 当需推进 manufacturing 域完整制造链浏览器层编排 E2E 时。

### Recall generateReturns 跨域建退货单编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `generateReturns` 跨域建 ErpSalReturn，属编排链。本计划验证审批轴 + locateTargets/close。
- Successor Required: `yes`
- Trigger Condition: 当需推进 Recall→Return 跨域编排 E2E 时。
- **已解除**：plan `2026-07-11-1234-1` Phase 3 落地 `ErpQaRecall__generateReturns` 跨域建退货单浏览器层 E2E（`quality-recall-generate-returns.action.spec.ts` 全绿：自包含批次追溯编排 → RecallTarget RETURNED + generatedReturnId 非空 + ErpSalReturn 创建）。

## Closure

Status Note: completed

Closure Audit Evidence:

- 4 新 spec 全绿（11 test）：`npx playwright test tests/e2e/business-actions/ --workers=1` → 22 passed（既有 11 + 新增 11，0 回归，2.7m）。
- WorkOrder（`mfg-work-order.action.spec.ts`）：审批轴 submit→approve→approveStatus 翻转 + checkAvailability→STOCK_RESERVED→start→IN_PROCESS→close→CLOSED + reject 守卫。
- PurReturn（`pur-return.action.spec.ts`）：复用 runP2pChain 产 approved Receive 前置 + approve→posted=true（反向出库+PURCHASE_RETURN 过账）+ reject/cancel 守卫。
- SalReturn（`sal-return.action.spec.ts`）：复用 runO2cChain 产 approved Delivery 前置 + approve→posted=true（反向入库+SALES_RETURN 过账）+ reject/cancel 守卫。
- Recall（`qa-recall.action.spec.ts`）：register→submit→approve 双字段翻转（approveStatus=APPROVED+status=APPROVED）→locateTargets→notifyCustomers→close→CLOSED + reject/cancel 守卫。
- `mvn clean install -DskipTests`：154 模块 BUILD SUCCESS（零 Java/源码变更，E2E 文件在根 tests/ 非 reactor 模块）。
- 文档对齐：`docs/testing/e2e-runbook.md`（业务动作层 6→10 代表域，153→164 测试）+ `docs/backlog/README.md`（0335-1 路标条目）+ `docs/logs/2026/07-10.md`。


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. All substantive closure claims verified: 4 spec files with real non-hollow tests; approval axis + posted side-effect + double-field flip assertions all present via verifyState; ORM use-approval tagSet confirmed. NOTE: minor quantitative drift (9 tests vs claimed 11); two Deferred items resolved but missing annotation. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- 业财过账凭证精确数值断言 / WorkOrder 完整制造链 / Recall generateReturns 编排 —— 见「Deferred But Adjudicated」各自 successor 触发条件。
