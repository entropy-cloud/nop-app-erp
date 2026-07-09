# 2026-07-09-2004-2-reverse-voucher-e2e 反向冲销浏览器层 E2E（红字凭证反转 + 域监听者状态回退）

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Mission: erp
> Work Item: 各域细化端到端验证（业财闭环方向二：财务侧 `ErpFinVoucher__reverse` 红字冲销→域监听者状态回退，浏览器层端到端）
> Source: 业财闭环方向二后端实现（`docs/plans/2026-07-04-1452-2-finance-reversal-writeback-loop.md` M5.2 completed）仅 Java 层覆盖（VoucherReversedEvent + 域监听者）；`ErpFinVoucher__reverse` 经 `/graphql` 全栈可达性 + 红字凭证 + 域回退的浏览器层 E2E 缺失。AGENTS.md 当前重点「各域细化端到端验证」+ 1249-1 已建立 orchestration 正向链范式（产已过账 AP/AR 凭证为反向前置）。
> Related: `docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（orchestration helper 范式源，runP2pChain/runO2cChain + 清理原语）、`docs/plans/2026-07-04-1452-2-finance-reversal-writeback-loop.md`（冲销反写闭环后端实现源 M5.2，VoucherReversedEvent + 域监听者）、`docs/architecture/posting.md`（冲销机制方向二）
> Audit: required

> **范围澄清（机制区分）**：本计划覆盖**财务侧 DIRECT 红字冲销** `ErpFinVoucher__reverse`（财务员直接红冲已过账凭证，M5.2 后端 1452-2，无审批/xwf 依赖）。这与 `2026-07-09-1249-1` Deferred「反向冲销（reverseApprove 红字凭证）浏览器层 E2E」标题中的 **域审批轴 `ErpXxx__reverseApprove`**（需 APPROVED 前置、approval-pattern）是**不同机制**。本计划**不解除** 1249-1 该 Deferred（域 reverseApprove 浏览器层仍开放，归 0814-2 approval-pattern successor）。

## Current Baseline

- **跨域编排链浏览器层 E2E 已建立**（`2026-07-09-1249-1`，completed）：`tests/e2e/orchestration/_helper.ts` 提供 `runP2pChain`（PO→Receive→Invoice 全链 `__save`(头+行)→`submitForApproval`→`approve`，DIRECT 审批）/ `runO2cChain`（SO→Delivery→Invoice 同型）+ 过账产物断言原语（`findItems`/`findPageTotal`）+ 清理原语（`cleanupVoucherByBillCode`/`cleanupArApByCode`/`cleanupStockMove`/`cleanupP2p`/`cleanupO2c`）。Invoice approve 后断言 `posted=true` + `ErpFinVoucherBillR`(billCode=invoice.code) 回链存在 + AR-AP 辅助账（PAYABLE 56.5 / RECEIVABLE 113 / OPEN）。
- **冲销反写闭环后端已落地**（M5.2，plan `2026-07-04-1452-2` completed）：finance 定义 `IErpFinVoucherReversedListener` SPI + `ErpFinReversalListenerRegistry`（镜像 `ErpFinAcctDocRegistry` 范式）。`ErpFinVoucherBizModel.reverse`（`module-finance/erp-fin-service/.../entity/ErpFinVoucherBizModel.java:57-63`）为 `@BizMutation` + `REQUIRES_NEW` 独立事务，签名 `reverse(String billHeadCode, ErpFinBusinessType businessType, context)`，委托 `ErpFinPostingProcessor.reverseProcess` 构造红字凭证 + 回链 + 标记原凭证 `isReversed=true` + 构造 `VoucherReversedEvent` 派发（默认 SYNC 同事务通知）。**经核实为 DIRECT 浏览器可调动作，无 xwf 审批依赖**（区别于 `ErpXxx__reverseApprove` 审批轴动作）。
- **域监听者回退目标态表已核实**（业财闭环方向二 §裁决4，各监听者 `onVoucherReversed` 按 businessType 分派）：
  - `PurReversalListener`（`module-purchase/.../posting/PurReversalListener.java`）：`AP_INVOICE`→`ErpPurInvoice` posted=false + approveStatus APPROVED→REJECTED；`PURCHASE_INPUT`→`ErpPurReceive` 仅 posted=false（库存物理冲销独立）。
  - `SalReversalListener`（`module-sales/.../posting/SalReversalListener.java`）：`AR_INVOICE`→`ErpSalInvoice` posted=false + approveStatus APPROVED→REJECTED。
- **业务类型枚举已核实**（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`）：`AP_INVOICE(30)`、`AR_INVOICE(40)`（枚举，GraphQL 入参以枚举名传入）。
- **当前套件规模**（e2e-runbook §概述 记录的基线计数，含 1249-2 看板 visual；1728-1 reports.visual 后可能已 +4，以 closure 实测为准）。orchestration 层 2 spec（P2P/O2C 正向链）。运行手册 `docs/testing/e2e-runbook.md` §跨域编排链浏览器层 E2E。
- **Payment/Receipt xwf 反向路径已裁决不可行**（1249-1 Deferred）：`nop` 浏览器用户被 wf 步骤参与者 `user:$0` 拒绝。但本计划的反向路径经 `ErpFinVoucher__reverse`（DIRECT 财务侧红冲），**不受 xwf 阻塞**——财务员直接红冲已过账凭证，不经 wf。
- **剩余差距**：业财闭环方向二（财务侧红冲→域单据回退）仅后端集成测试覆盖（`2026-07-04-1452-2` Java 层 + 1018-1 反向冲销段），浏览器层未验证：`ErpFinVoucher__reverse` 经 `/graphql` 全栈可达性 + 红字凭证生成 + 原凭证 isReversed + 域监听者回退（Invoice posted/approveStatus 翻转）。

## Goals

- 建立业财闭环方向二浏览器层 E2E：经 1249-1 `runP2pChain`/`runO2cChain` 驱动正向链产出已过账 AP/AR Invoice 凭证 → 调 `ErpFinVoucher__reverse(billHeadCode, businessType)` 红字冲销 → 断言红字凭证生成 + 原凭证 `isReversed=true` + 域监听者回退（Invoice `posted=false` + `approveStatus` APPROVED→REJECTED）。
- 核实并（仅在必要时）扩展 orchestration helper 清理原语以覆盖反向冲销产物：`reverseProcess` 以**同一 billHeadCode** 创建红字凭证的 voucher_bill_r，故既有 `cleanupVoucherByBillCode(page, billCode)` 大概率已覆盖原+红字凭证及其行/回链；Phase 1 Explore 探针确认后，仅在确认存在清理缺口时扩展（如 AR-AP 红冲——`reverseProcess` 对既有 AR-AP 行做取消/更新而非新增，故预计无新增 AR-AP 行需删除）。
- 验证 `ErpFinBusinessType` 枚举经 GraphQL `/graphql` 的入参序列化（枚举名 vs GraphQL enum scalar）+ REQUIRES_NEW 独立事务的浏览器层行为。

## Non-Goals

- Payment/Receipt xwf 反向浏览器层 E2E——Payment/Receipt 为 xwf WORKFLOW 模式，`nop` 用户被 wf 步骤参与者 `user:$0` 拒绝（1249-1 Deferred，触发条件：xwf 浏览器层审批 API 可行 / wf 步骤参与者配置放宽时）。本计划仅覆盖 DIRECT 财务侧红冲路径。
- 域级 settle 核销浏览器层 E2E——独立财务核销面 successor（1249-1 Deferred）。
- `ErpXxx__reverseApprove` 审批轴反向浏览器层 E2E——审批轴动作（需 APPROVED 前置，且部分经 wf），区别于本计划 DIRECT 财务侧红冲。归 0814-2 Deferred「审批工作流（xwf）业务动作 E2E」successor。
- 全业务类型反向覆盖（PURCHASE_RETURN/SALES_RETURN/PAYMENT/RECEIPT/PURCHASE_INPUT 等）——本计划覆盖 AP_INVOICE/AR_INVOICE 两核心类型证明范式，其余同范式 successor。
- 库存物理冲销浏览器层（reverseApprove 链触发 stockMoveBiz.reverse）——归 inventory 域 successor（库存物理冲销独立于凭证红冲）。
- 业财过账凭证借贷精确数值断言（红字凭证金额 = 原凭证负数镜像）——1249-1 已断言正向凭证 AR-AP openAmount 精确值；红字凭证精确数值镜像断言归 finance 数值断言层 successor。

## Task Route

- Type: `verification or audit work`（浏览器层端到端验证，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/architecture/posting.md`（§冲销机制方向二，财务侧红冲→域回退）、`docs/testing/e2e-runbook.md`（套件运行手册）、`docs/design/flow-overview.md`（P2P/O2C 全链设计）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation 经 GraphQL 驱动范式 + orchestration helper 复用，1249-1 已验证）；`nop-backend-dev`（确认 `reverse` GraphQL 签名 + `ErpFinBusinessType` 枚举入参序列化 + REQUIRES_NEW 事务语义 + 域监听者回退目标字段）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests` → `app-erp-all/target/quarkus-app/quarkus-run.jar`
- Node.js + `npm install`（Playwright 依赖已就绪）
- fresh-DB 重置机制不变（`rm -f db/erp.mv.db`，种子非幂等）
- 复用既有 Playwright 基础设施 + 1249-1 orchestration helper（runP2pChain/runO2cChain + 清理原语）
- 种子 COA 完备（1249-1 补齐 1401/1403/1131/2221/6401 使过账 Provider 科目码可达）——正向链 Invoice approve 产 posted=true 凭证，为反向提供前置
- 无新增端口/环境变量/密钥/外部服务

## Execution Plan

### Phase 1 - P2P 反向冲销浏览器层 E2E（AP_INVOICE 红字冲销 + 采购发票回退）

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（反向驱动 + 清理核实/扩展）、`tests/e2e/orchestration/p2p-reverse.spec.ts`（新建）；探针 spec（临时，裁决后删除）
Skill: `nop-testing`

- Item Types: `Explore | Decision | Add | Proof`
- Prereqs: 1249-1 orchestration helper（runP2pChain 正向链产 posted AP_INVOICE 凭证 + cleanupP2p）+ finance reverse 后端已落地（M5.2）

- [x] `Explore`：`ErpFinBusinessType` 枚举经 GraphQL `/graphql` 的入参序列化探针——经 `runP2pChain` 产一张 posted AP_INVOICE 凭证后，原型调用 `ErpFinVoucher__reverse` 以枚举名（`AP_INVOICE`）作为 GraphQL enum scalar 入参，抓取实际请求/响应确认：(a) 枚举名是否被 GraphQL schema 接受（vs 平台枚举 code 整数 30）；(b) `reverseProcess` 是否对 `nop` 浏览器用户无授权/xwf 门控（已核实后端 `ErpFinPostingProcessor.java:161-208` 无授权门控，本项确认浏览器层一致）；(c) `reverseProcess` 产物结构——红字凭证 voucher_bill_r 是否以同一 billHeadCode 关联（决定既有 `cleanupVoucherByBillCode` 是否已覆盖清理）；(d) AR-AP 红冲是否新增行（决定是否需新增清理）。探针 spec 裁决后删除。
  - Skill: `nop-testing | nop-backend-dev`
- [x] `Decision`：基于 Explore 结果裁决反向冲销驱动策略 + 入参形式 + 清理范围——选定 `callMutation('ErpFinVoucher__reverse', { billHeadCode: invoice.code, businessType: <Explore 确认的入参形式> })`；裁决清理原语：若 Explore 确认既有 `cleanupVoucherByBillCode` 已覆盖原+红字凭证（同 billHeadCode）且无新增 AR-AP 行，则复用既有清理不扩展；若发现清理缺口，扩展 `cleanupReverse`（具体缺口由 Explore 证据定）。
  - 记录替代方案：引用种子既有凭证直 reverse——但种子凭证为静态直 seed，billHeadCode 可能不匹配业务类型且不可控；选全链驱动 `runP2pChain` 可控。
  - 残留风险：`reverseProcess` 内部监听者失败隔离（监听者失败不回滚红字凭证，落入 5.1 异常工作台）——若某监听者异常，Invoice 回退可能未发生，spec 断言须以 `__get` 权威查库为准。
  - Skill: `nop-testing | nop-backend-dev`
- [x] `Add`：`_helper.ts` 增 `runP2pReverse(page)`——编排 `runP2pChain` 正向链（产 Invoice posted=true + voucher bill_r）→ `callMutation('ErpFinVoucher__reverse', ...)`（入参形式按 Decision）→ 返回 reversalVoucherId + 各实体 id 供断言/清理。清理按 Decision：复用既有 `cleanupVoucherByBillCode`/`cleanupArApByCode`/`cleanupP2p`，仅在 Explore 确认缺口时扩展 `cleanupReverse`。清理保护共享 DB（finance dashboard/资产负债表/ar-ap-aging 报表读 voucher/gl_balance/ar_ap）。
  - Skill: `nop-testing`
- [x] `Add`：`p2p-reverse.spec.ts`——P2P 反向冲销 E2E：(1) 正向链断言（Invoice approve→posted=true + voucher bill_r 回链存在，复用 1249-1 断言）；(2) 反向断言——`ErpFinVoucher__reverse` 后原凭证 `isReversed=true`（经 `ErpFinVoucher__findPage` 或 `__get` 断言）+ 红字凭证生成（findPage total≥1，含 reversal 标记）+ Invoice 回退（`ErpPurInvoice__get` 断言 `posted=false` + `approveStatus=REJECTED`，经 `PurReversalListener.rollbackInvoice` 机制）；(3) 清理（按 Decision）。
  - Skill: `nop-testing`
- [x] `Proof`：P2P 反向 spec 全绿（正向链 + 反向回退断言）；orchestration/ 套件无回归。
  - 验证命令：`npx playwright test tests/e2e/orchestration/ --workers=1`（局部回归；全套件验证归 Closure Gates）
  - Skill: `nop-testing`

Exit Criteria:

- [x] P2P 反向冲销全链绿：正向链产 AP_INVOICE posted 凭证 → `ErpFinVoucher__reverse` → 原凭证 isReversed=true + 红字凭证生成 + Invoice posted=false + approveStatus=REJECTED（经 `__get`/`findPage` 独立断言）
- [x] Explore 探针完成：枚举入参形式 + reverseProcess 产物结构 + 清理范围裁决已记录；orchestration helper 反向驱动落地，非空壳（真实 reverse mutation 调用；清理按裁决复用既有或扩展）

### Phase 2 - O2C 反向冲销浏览器层 E2E（AR_INVOICE 红字冲销 + 销售发票回退）

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（增 `runO2cReverse`）、`tests/e2e/orchestration/o2c-reverse.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 反向驱动 + 枚举入参范式已建立

- [x] `Add`：`_helper.ts` 增 `runO2cReverse(page)`——编排 `runO2cChain` 正向链（产 Sales Invoice posted=true + voucher bill_r，含 O2C 备货前置）→ `callMutation('ErpFinVoucher__reverse', {billHeadCode: salesInvoice.code, businessType: 'AR_INVOICE'})`（入参形式按 Phase 1 Decision）→ 返回 reversalVoucherId + 各实体 id；清理按 Phase 1 Decision（复用既有 `cleanupVoucherByBillCode`/`cleanupArApByCode`/`cleanupO2c`，仅在确认缺口时扩展）。
  - Skill: `nop-testing`
- [x] `Add`：`o2c-reverse.spec.ts`——O2C 反向冲销 E2E：正向链断言（SO→Delivery→Invoice approve→posted=true + AR 辅助账 RECEIVABLE/113/OPEN）→ `ErpFinVoucher__reverse(AR_INVOICE)` → 原凭证 isReversed=true + 红字凭证生成 + Sales Invoice 回退（`ErpSalInvoice__get` 断言 `posted=false` + `approveStatus=REJECTED`，经 `SalReversalListener`）→ 清理（按 Decision）。
  - Skill: `nop-testing`
- [x] `Proof`：O2C 反向 spec 全绿；orchestration/ 套件无回归。
  - 验证命令：`npx playwright test tests/e2e/orchestration/ --workers=1`（局部回归；全套件验证归 Closure Gates）
  - Skill: `nop-testing`

Exit Criteria:

- [x] O2C 反向冲销全链绿：正向链产 AR_INVOICE posted 凭证 → `ErpFinVoucher__reverse` → 原凭证 isReversed=true + 红字凭证生成 + Sales Invoice posted=false + approveStatus=REJECTED

### Phase 3 - 文档对齐 + Deferred 状态登记

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1/2 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 增反向冲销层段（业财闭环方向二：正向链→`ErpFinVoucher__reverse`→域监听者回退浏览器层 E2E + helper 范式 + 枚举入参序列化裁决 + 与正向 orchestration 层的层间关系）+ 套件计数更新为实测值（含本计划 2 新 reverse spec）；`docs/testing/known-good-baselines.md` 增本计划基线行。**明确不解除** 1249-1 Deferred「反向冲销（reverseApprove 红字凭证）」（域审批轴 reverseApprove，不同机制，仍开放归 approval-pattern successor）；本计划补的是财务侧 DIRECT 红冲（M5.2 后端 1452-2）的浏览器层覆盖。
  - Skill: none

Exit Criteria:

- [x] e2e-runbook 含反向冲销层段 + 层间关系 + 套件计数（实测）；known-good-baselines 含基线行

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0b935b3a7ffekI10C1rwU7hcBf`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（_helper.ts runP2pChain/runO2cChain + 清理原语 / `reverse` @BizMutation REQUIRES_NEW 无 xwf / PurReversalListener AP_INVOICE 回退 / SalReversalListener AR_INVOICE 回退 / AP_INVOICE(30)/AR_INVOICE(40) 枚举 / 1249-1 Deferred 存在 / 正向链产 posted 凭证 / reverseProcess 无授权门控 nop 用户不撞 user:$0）。1 BLOCKER + 1 MAJOR + 4 MINOR：
  - B1（BLOCKER）：内部矛盾——Phase 3 声称「解除 1249-1 Deferred『反向冲销（reverseApprove 红字凭证）』」但 Non-Goals 排除 reverseApprove 归 0814-2 successor。1249-1 该 Deferred 标题即 reverseApprove（域审批轴），与本计划财务侧 `ErpFinVoucher__reverse`（M5.2 后端）不同机制。违反 rule 1（baseline honesty）+ rule 11（文本一致）。
  - M1：枚举入参 GraphQL 可行性是计划关键，但仅由「probe later」Decision 守门，应拆出独立 Explore 项在 Decision/Add 前完成。
  - m1：套件计数 provenance 模糊（误称「无 1249-2 计划」——1249-2 实为看板 visual 计划）。
  - m2：cleanupReverse Goal 可能冗余（reverseProcess 以同一 billHeadCode 建 voucher_bill_r，既有 cleanupVoucherByBillCode 已覆盖）。
  - m3：「若产生」引入清理软范围（reverseProcess 对既有 AR-AP 行取消/更新而非新增）。
  - m4：Closure Gates 跑 mvn clean install（零 Java 变更）可接受无害。
  - **已修复**：B1——Source 重构为 M5.2 财务侧 DIRECT 红冲浏览器层缺口（1452-2 后端），新增范围澄清段明示不解除 1249-1 reverseApprove Deferred（不同机制），Phase 3 移除「解除」声明 + Deferred 增「域审批轴 reverseApprove」独立项；M1——Phase 1 拆出独立 `Explore` 项（枚举入参 probe + reverseProcess 产物 + 清理范围）gate Decision/Add；m1——套件计数 cite e2e-runbook 为源 + closure 实测；m2——cleanupReverse Goal 改为「核实既有覆盖，仅在缺口时扩展」；m3——清理改为按 Explore 证据具体化（AR-AP 取消非新增）。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增 reverse spec + 既有 spec）+ 既有后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（P2P + O2C 反向冲销浏览器层 E2E 全绿 + 域监听者回退断言）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines）
- [x] 已运行验证：`npx playwright test`（全套件 133 passed / 0 回归 / 17.7m）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，E2E 新增文件在根 tests/ 非 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 域审批轴 `ErpXxx__reverseApprove` 浏览器层 E2E（1249-1 Deferred，本计划不解除）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1249-1 Deferred「反向冲销（reverseApprove 红字凭证）浏览器层 E2E」覆盖的是**域审批轴** `ErpXxx__reverseApprove`（需 APPROVED 前置、approval-pattern），与本计划的**财务侧 DIRECT 红冲** `ErpFinVoucher__reverse`（无审批依赖）是不同机制。本计划不解除该 Deferred。
- Successor Required: `yes`
- Trigger Condition: 当 xwf 浏览器层审批 API 验证可行 / approval-pattern 域浏览器层反向验证需求时（归 0814-2 approval-pattern successor）。
- **useWorkflow 子集（Payment/Receipt/Disposal/Salary reverseApprove）经 plan `2026-07-09-2330-1` 权威裁决：不可行**——reverseApprove 需 APPROVED 前置，而 useWorkflow 实体达 APPROVED 须经 wf submit（user:$0 阻塞），故其 reverseApprove 浏览器层同步阻塞。DIRECT 域（PO/SO 等 useApproval 轴）reverseApprove 浏览器层仍可达（submit→approve 经 DIRECT，1249-1 已证），归独立 successor。

### Payment/Receipt xwf 反向浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Payment/Receipt 为 xwf WORKFLOW 模式，`nop` 浏览器用户被 wf 步骤参与者 `user:$0` 拒绝（1249-1 Deferred）。本计划仅覆盖 DIRECT 财务侧红冲（`ErpFinVoucher__reverse`）。
- Successor Required: `yes`
- Trigger Condition: 当 xwf 浏览器层审批 API 验证可行 / nop 用户 wf 委托配置落地 / wf 步骤参与者配置放宽时（同 1249-1 裁决）。
- **经 plan `2026-07-09-2330-1` 权威裁决：不可行（NOT FEASIBLE）**。useWorkflow 审批轴浏览器层 3 条路径均阻断（委托 sysUser(0) seq PK 物化失败 / 无浏览器层身份映射 / .xwf 放宽属生产变更）。**重评触发条件更新**：当 nop-entropy 平台支持浏览器层测试用户身份映射 / 委托免 sysUser(0) 物化 / sysUser 种子物化时。详见 `docs/testing/e2e-runbook.md`「useWorkflow 审批轴浏览器层（xwf）」段 + plan 2330-1 Phase 1 Decision。

### 全业务类型反向覆盖（PURCHASE_RETURN/SALES_RETURN/PAYMENT/RECEIPT/PURCHASE_INPUT 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖 AP_INVOICE/AR_INVOICE 两核心类型证明范式（DIRECT 财务侧红冲 + 域监听者回退）。其余业务类型同监听者分派范式（PurReversalListener/SalReversalListener switch case），边际收益递减。
- Successor Required: `yes`
- Trigger Condition: 当需按业务类型推进全反向浏览器层覆盖时。

### 库存物理冲销浏览器层（reverseApprove 链触发 stockMoveBiz.reverse）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 库存物理冲销（库存移动反向）独立于凭证红冲（PurReversalListener.rollbackReceive 注释明示「库存物理冲销独立于凭证红冲」）。财务侧红冲仅回退 posted 标志，保留库存物理状态。
- Successor Required: `yes`
- Trigger Condition: 当需浏览器层验证库存物理冲销端到端时。

### 红字凭证借贷精确数值镜像断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: 1249-1 已断言正向凭证 AR-AP openAmount 精确值（PAYABLE 56.5 / RECEIVABLE 113）。本计划断言红字凭证生成存在性 + 原凭证 isReversed + 域回退；红字凭证金额 = 原凭证负数镜像的精确数值断言归 finance 数值断言层 successor。
- Successor Required: `yes`
- Trigger Condition: 当需业务动作触发过账后的红字凭证精确数值断言时。

## Closure

Status Note: closed — 3 Phase 全部完成，独立结束审计 VERDICT: PASS。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话冷重播，session `ses_0b8931129ffewJQuxMiz3PdqS1`）— VERDICT: PASS。逐项 live 验真：Phase 1/2 helper 导出 + spec 三层断言（original voucher NORMAL+isReversed / reversal voucher REVERSAL+reversalOfVoucherId / 域单据 posted=false+REJECTED）+ 后端 reverse 签名（标量 Long，REQUIRES_NEW 无 xwf）+ Pur/SalReversalListener 回退目标态 + Phase 3 文档对齐（e2e-runbook 反向冲销层段 + 计数 133 / known-good-baselines 基线行）+ reverseProcess 同 billHeadCode 写红字 voucher_bill_r + cancelOnReverse 取消非新增（cleanup 充分性确认）+ 机制区分一致性（不解除 1249-1 reverseApprove Deferred）+ 测试计数数学（131+2=133）。无 BLOCKER/MAJOR；MINOR housekeeping（日志条目）已补 `docs/logs/2026/07-09.md`。
- 执行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `npx playwright test --workers=1`（全套件 133 passed / 17.7m / 0 回归，含 2 新 reverse spec）+ orchestration 局部 `npx playwright test tests/e2e/orchestration/`（4 passed / 46.4s）。

Follow-up:

- Payment/Receipt xwf 反向 / 全业务类型反向 / 库存物理冲销 / 红字凭证精确数值 —— 见「Deferred But Adjudicated」各自 successor 触发条件。
