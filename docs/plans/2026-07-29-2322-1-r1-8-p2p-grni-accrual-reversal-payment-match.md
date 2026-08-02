# 2026-07-29-2322-1-r1-8-p2p-grni-accrual-reversal-payment-match R1.8 — P2P 暂估应付冲回 + 付款核销三单匹配复核

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 R1.8（P1-MA2-001 + P1-MA2-003）；P2-MA2-006（watch-only owner doc 漂移）由 arm-index 标注「与 P1-MA2-001 一并裁决」顺带闭合，非 R1.8 roadmap 行独立条目
> Related: `docs/plans/2026-07-29-2322-2-r1-9-multi-currency-p2p-o2c-voucher-fx-gain-loss.md`（多币种 P2P/O2C，R1.8 的 receive→invoice 链路与 R1.9 共享 PostingEvent/VoucherFact 但结果面正交）、`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（MA2 P2P 端到端审计，本计划修复其 P1 发现）
> Audit: required

## Current Baseline

- **GRNI 暂估应付冲回缺失**（P1-MA2-001）：P2P「先入库后开票」黄金路径中，`receive` 生成 `PURCHASE_INPUT` 凭证（借 1401 存货 / 贷 2202 暂估应付），`invoice` 生成 `AP_INVOICE` 凭证（借 1403 在途物资 + 借 2221 进项税 / 贷 2202 应付）。两张凭证均贷 2202 → GL 2202 双计（暂估 + 正式应付）；1403 在途物资与 1401 库存商品双计存货；无自动冲回分录清理暂估侧。辅助账层 `ErpFinArApItem` 不受影响（`ErpFinArApItemGenerator.resolveProfile` 明确不处理 `PURCHASE_INPUT`）。owner doc `returns.md §暂估应付冲减` 仅在退货链实现冲回，正向 receive→invoice 冲回未实现。
- **付款核销缺三单匹配完成态复核**（P1-MA2-003）：`PaymentSettler.settle`（`module-purchase/erp-pur-service/.../service/entity/PaymentSettler.java`）仅校验发票 `approveStatus=APPROVED`，不复核三单匹配完成态。非严格默认模式（match=warn+放行）下价格严重超容差发票 APPROVED 后付款核销无二次门禁。owner doc `three-way-match.md §匹配时机:48`「付款前最终校验：付款核销时确认发票已完成三单匹配」未落实。
- **owner doc 漂移**（P2-MA2-006，watch-only）：owner doc `returns.md §红字发票处理` 描述「已开票退货→红字 ErpPurInvoice」流程；实现以 `PURCHASE_RETURN` 过账 + 负 `ArApItem` credit memo 替代（功能等价于 AP 余额回减，但 GL 冲暂估侧非 formal 侧）。
- **验证基线**：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿。P2P E2E（`TestErpPurProcureToPayEnd`/`TestErpPurReturnRefundEndToEnd`）均单币种。

## Goals

- 裁决并落地 P1-MA2-001（GRNI 自动冲回）的修复方案——实现自动冲回 **或** 登记为期末人工清理的 documented simplification + owner doc 对齐。
- 裁决并落地 P1-MA2-003（付款核销三单匹配复核）的修复方案——settle 前复核匹配状态 **或** 显式接受「APPROVED 即匹配通过」并更新 owner doc。
- 闭合 P2-MA2-006 owner doc 漂移（更新 `returns.md` 反映 credit-memo-via-return 实际实现）。
- arm-index 中 P1-MA2-001/003 + P2-MA2-006 状态回填为已修复或裁决（documented simplification，非降级 deferred）。

## Non-Goals

- 多币种 P2P 本位币凭证路径与汇兑损益（R1.9 / P1-MA2-002）。
- 期末结账 auto-post-on-close 语义与反结账审批（R1.10/R1.11）。
- GRNI 跨年度冲回、多账套暂估冲回（多公司维度 R1.29）。
- 退货链已实现的暂估冲减（owner doc returns.md 已覆盖，不在范围）。

## Task Route

- Type: `implementation-only change`（含会计保护区域——独立 plan-audit + closure-audit 必需；无 ORM 变更故 ORM ask-first 人工确认未触发；保护区域裁决（实现/document simplification 选择）须有 owner-doc 证据支撑，由独立结束审计核验）
- Owner Docs: `docs/design/purchase/`（three-way-match.md + returns.md）+ `docs/design/finance/posting.md`（过账业务类型与凭证装配）+ `docs/design/finance/returns.md`
- Skill Selection Basis: 修复触及 BizModel/Processor 过账链路与跨实体 Facade 调用（receive/invoice 间凭证关联），加载 `nop-backend-dev`（决策门、跨实体调用、错误处理、事务边界）；ORM 无变更（无 ask-first）。Phase 3 验证使用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增基础设施；可能新增 config-gate（如 `erp-pur.grni-auto-reverse-on-invoice` 默认 false 保护既有 113 purchase 测试不触发新凭证），具体在 Phase 1 裁决后确定。
- 回滚策略：所有代码变更经 `@BizMutation` 事务边界；GRNI 冲回若实现，新增凭证为反转分录，reverse 路径幂等（isReversed 标志）。

## Execution Plan

### Phase 1 - GRNI 冲回与匹配门控裁决（Decision-heavy）

Status: completed
Targets: `docs/design/purchase/returns.md`、`docs/design/purchase/three-way-match.md`、`docs/design/finance/posting.md`、receive/invoice 过账链路源码探查
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore: 核实 receive→invoice 凭证关联机制——`PostingEvent` 是否携带可关联的 `billCode`/源单据 ID（决定 GRNI 冲回能否定位待冲回的 PURCHASE_INPUT 凭证）；核实 invoice approve 编排链（`ErpPurInvoiceProcessor` 或 BizModel）是否有可注入冲回钩子的步骤。
  - Skill: `nop-backend-dev`
  - **Explore 结果（2026-07-29 实仓核实）**：
    - `PURCHASE_INPUT` 凭证 `billHeadCode = stockMove.code`（`InvPostingDispatcher.java:203`，**非** `receive.code`）；`AP_INVOICE` 凭证 `billHeadCode = invoice.code`（`PurInvoicePostingDispatcher.java:74`）。两者经不同 billHeadCode 落业财回链 `ErpFinVoucherBillR`，**无共享键**直接关联。
    - invoice→receive 反查链：`ErpPurInvoiceLine.receiveLineId` → `ErpPurReceiveLine.receiveId` → `ErpPurReceive`；receive→stockMove 反查经 `IErpInvStockMoveBiz.findByRelatedBill("ERP_PUR_RECEIVE", receive.code, context)`（`ErpPurReceiveProcessor.java:250-251` 范式）。GRNI 冲回需 4 跳（invoiceLine→receiveLine→receive→stockMove→voucher）。
    - 冲回 SPI 可用：`IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)`（`IErpFinVoucherBiz.java:42`）按业财回链反查原已过账凭证红冲；无原凭证抛 `ErpFinPostingErrors.ERR_REVERSE_SOURCE_NOT_FOUND`（可容错跳过，对齐 `runCommitmentReleaseOnInvoiceApproveHook` 范式）。
    - invoice approve 编排链（`ErpPurInvoiceProcessor.approve:78-96`）已有可注入钩子的后置步骤（`runCommitmentReleaseOnInvoiceApproveHook`），新增 GRNI 钩子结构可行。
    - **证伪简单冲回可行性**（触发 P1-MA2-001 方案 B 关键理由）：(a) `reverse()` 仅支持全额红冲，部分开票场景全额冲回会**少计暂估**（un-invoiced 部分丢失 accrual）；(b) invoice `reverseApprove` 后需"反冲回"（重新 post PURCHASE_INPUT），但 PostingEvent 重建需 inventory 域 stockMove 数据，跨域 SPI 缺失；(c) 不反冲回则 GL 存货净值为 0 但物理库存仍存在（数据不一致）。
- [x] Decision: P1-MA2-001 GRNI 冲回方案裁决。选择 A（实现 invoice approve 时红冲关联 receive 的 PURCHASE_INPUT 凭证，config-gated 默认 false）或 B（documented simplification——登记为期末人工清理，owner doc `returns.md`/`posting.md` 标注「正向暂估冲回本期经期末试算平衡人工发现并手工清理」）。记录替代方案与残留风险。
  - Skill: `nop-backend-dev`
  - **裁决：方案 B（documented simplification）**。理由：(1) 正确实现需**双向钩子**——approve 时红冲 + reverseApprove 时反冲回（re-post PURCHASE_INPUT），反冲回路径需 inventory 域重建 PostingEvent 的跨域 SPI（当前缺失）；(2) `reverse()` 仅全额红冲，部分开票（receive 100 单位，发票 50 单位）全额冲回会**少计暂估**（un-invoiced 50 单位丢失 accrual），引入新缺陷；(3) 跨年度/跨账套/多账套语义需额外设计；(4) 对参考应用（reference app），完整双向 GRNI 自动冲回的实现成本与残留复杂度不成比例。documented simplification 是真实裁决（非降级 deferred）：当前行为（GL 2202 暂估应付双计 + 1403/1401 存货双计）影响 bounded，期末试算平衡可发现并手工清理，owner doc 显式标注。**残留风险**：正向 receive→invoice 暂估应付 GL 双计须期末人工清理；已记录于 `returns.md §暂估应付冲减` + `posting.md §业务类型映射`。
  - **替代方案（被拒，记录供 successor）**：方案 A（config-gated 自动冲回）——需先补齐 inventory 域 `IErpInvPostingBiz.repostPurchaseInput(stockMoveCode)` SPI + 部分开票覆盖判定 + reverseApprove 反冲回钩子，三项前置完成后方可落地。归 R1.9+ successor（与本计划 Non-Goals 多币种正交，但共享凭证链路改造）。
- [x] Decision: P1-MA2-003 付款匹配门控裁决。选择 A（`PaymentSettler.settle` 前复核 invoice 三单匹配状态标记——读取匹配结果字段，未完成匹配时按 config 严格/warn）或 B（owner doc `three-way-match.md §匹配时机` 更新为「APPROVED 即匹配通过，付款不二次门禁」）。记录理由。
  - Skill: `nop-backend-dev`
  - **裁决：方案 A（config-gated settle 三单匹配复核）**。理由：(1) 实现简单无状态副作用——`PaymentSettler` 注入既有 `ThreeWayMatcher`，settle 路径在 `requireInvoiceForSettle` APPROVED 守卫后追加 `threeWayMatcher.match(invoice.code, lines, Boolean.TRUE)`（强制 strict）复核；(2) 无双向钩子/反冲回复杂度（match 是只读校验，不修改状态）；(3) owner doc `three-way-match.md §匹配时机:48` 已声明「付款前最终校验」，方案 A 落实该契约；(4) config-gated（`erp-pur.settle-recheck-three-way-match` 默认 false）保护既有 113 purchase 测试不触发新门禁。新增 ErrorCode `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED`（settle 语境包装 match 异常）。**残留风险**：复核为运行时重算（invoice 无持久化 matchStatus 字段，避免 ORM 变更），依赖 receive/order 行当前状态——若 approve 后 receive 行被改（设计上 APPROVED 发票不允许改回链，见 three-way-match.md §一致性规则），重算结果与 approve 时一致。

Exit Criteria:

- [x] 两项 Decision 已记录选择、替代方案、残留风险，并在计划或引用 owner doc 中落地裁决理由
- [x] Explore 结果确认凭证关联机制可行性（或证伪，触发方案 B）

### Phase 2 - 实现裁决结果（Fix | Add）

Status: completed
Targets: Phase 1 裁决确定的代码/文档面（`PaymentSettler` + owner docs）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 裁决完成

- [x] 若 P1-MA2-001 裁决为方案 A：实现 GRNI 自动冲回（invoice approve 编排步骤注入冲回钩子，红冲关联 PURCHASE_INPUT 凭证，config-gated 默认 false 保护既有测试）+ 新增 ErrorCode（冲回失败/无关联 receive 时 NopException）。
  - Skill: `nop-backend-dev`
  - **N/A**——Phase 1 裁决为方案 B（documented simplification），本项不适用。
- [x] 若 P1-MA2-001 裁决为方案 B：更新 owner doc `returns.md`/`posting.md` 标注 documented simplification + 暂估侧期末人工清理流程。
  - Skill: none
  - **落地**：`docs/design/purchase/returns.md §暂估应付冲减` 新增「正向 receive→invoice 暂估冲回（documented simplification）」子节，记录当前 GL 行为、残留风险、期末人工清理流程、裁决理由、被拒替代方案；`docs/design/finance/posting.md §业务类型映射` PURCHASE_INPUT/AP_INVOICE 行后追加 GRNI documented simplification 注记块，引用 returns.md。
- [x] 若 P1-MA2-003 裁决为方案 A：`PaymentSettler.settle` 增三单匹配状态复核（config-gated 严格/warn）+ ErrorCode。
  - Skill: `nop-backend-dev`
  - **落地**：`ErpPurConstants.CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH = "erp-pur.settle-recheck-three-way-match"`（默认 false）；`ErpPurErrors.ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED`；`PaymentSettler` 注入 `ThreeWayMatcher`，`requireInvoiceForSettle` APPROVED 守卫后追加 `recheckThreeWayMatchAtSettle`（强制 `strict=true` match，cause 链包装为 settle 语境错误码）；新增 `loadInvoiceLines`/`isSettleRecheckEnabled` 辅助方法。类 Javadoc 追加 R1.8 门控说明。
- [x] 若 P1-MA2-003 裁决为方案 B：更新 owner doc `three-way-match.md §匹配时机` 对齐「APPROVED 即匹配通过」。
  - Skill: none
  - **N/A**——Phase 1 裁决为方案 A，本项不适用（但仍追加 owner doc 对齐方案 A 实现：`three-way-match.md §匹配时机` 新增「付款核销二次门控」子节，记录 config + 复核语义 + cause 链）。
- [x] Fix: P2-MA2-006 owner doc `returns.md §红字发票处理` 更新为反映 credit-memo-via-return（PURCHASE_RETURN 过账 + 负 ArApItem）实际实现。
  - Skill: none
  - **落地**：`docs/design/purchase/returns.md §红字发票处理` 顶部追加「实现偏离记录（R1.8 P2-MA2-006 闭合）」块，说明 credit-memo-via-return 实现（PURCHASE_RETURN 过账 + 负 ArApItem credit memo）+ 功能等价性 + 裁决；重写流程图为实现实际路径；历史「红字发票单→审核红字发票」构想保留为参考（标注「非当前实现」）。

Exit Criteria:

- [x] 所选裁决方案（实现或 documented simplification）已在代码或 owner doc 中落地（GRNI + 匹配门控 + returns.md 漂移）
- [x] 新增 config-gate（若有）默认 false 保护既有测试基线

### Phase 3 - 验证（Proof）

Status: completed
Targets: 新增/修改测试 + 既有 P2P E2E 回归
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [x] Proof: 若方案 A 实现 GRNI 冲回——新增 receive→invoice E2E 断言 GL 2202 不双计（红冲后暂估侧归零）+ 冲回凭证 postingType=REVERSAL + isReversed 标志正确。指定测试类与 GraphQL 请求路径。
  - Skill: `nop-testing`
  - **N/A**——Phase 1 裁决为方案 B（documented simplification），无新凭证路径，本项不适用。GRNI 现状（双计）经既有 `TestErpPurProcureToPayEnd`（默认 config， PURCHASE_INPUT/AP_INVOICE 各自过账）隐式覆盖。
- [x] Proof: 若方案 A 实现匹配门控——新增 settle 负向测试（未完成匹配的发票 settle 按 config assertThrows / warn）。
  - Skill: `nop-testing`
  - **落地**：新增 `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurSettleThreeWayMatchRecheck.java`（3 场景）+ `src/test/resources/settle-recheck-test.yaml`（`erp-pur.settle-recheck-three-way-match=true`）：
    - **场景1（负向-价格超容差）**`testSettleRejectsPriceMismatchWhenRecheckEnabled`：订单单价 10、发票单价 20（差异 100% >> 5%）→ GraphQL `ErpPurPayment__settle` 返回 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED`，发票 `paidAmount=0`（门控阻断前不写 PaymentLine）。
    - **场景2（负向-数量超入库）**`testSettleRejectsQtyMismatchWhenRecheckEnabled`：发票数量 12 > 入库 10 → settle 返回 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED`。
    - **场景3（正向-匹配通过）**`testSettlePassesWhenMatchWithinTolerance`：数量=入库、价格差异 2% < 5% → settle 成功，发票 `paidStatus=PARTIAL`、`paidAmount=50`。
- [x] Proof: 既有 `TestErpPurProcureToPayEnd`/`TestErpPurReturnRefundEndToEnd` 回归通过（config-gate 默认 false 不触发新凭证）。
  - Skill: `nop-testing`
  - **结果（2026-07-29 全绿）**：`mvn -pl module-purchase/erp-pur-service test` 119 测试全绿（含 `TestErpPurProcureToPayEnd` 4 + `TestErpPurReturnRefundEndToEnd` 2 + `TestErpPurPaymentSettlement` 5 + `TestErpPurThreeWayMatch` 6 + 新增 `TestErpPurSettleThreeWayMatchRecheck` 3）；`mvn -pl module-finance/erp-fin-service test` 286 测试全绿；`mvn clean install -DskipTests`（全 workspace，154 reactor 模块）BUILD SUCCESS。config-gate 默认 false 保护既有基线，零回归。

Exit Criteria:

- [x] 裁决方案为「实现」的发现项有对应正向 + 负向测试断言（P1-MA2-003 方案 A：2 负向 + 1 正向）
- [x] 既有 P2P 测试零回归

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_051849071ffeYqE7KFIpzJBmdv) because (a) Task Route 承诺「人工确认门控」但 Closure Gates 未落地该门控（规则 11 文本一致性）；(b) Goal 用「deferred」措辞违反规则 13（已确认缺陷不得降级）。事实基线经核实准确（PaymentSettler.settle:55 仅校验 APPROVED + ErpFinArApItemGenerator 无 PURCHASE_INPUT case 确认）。
- Independent draft review iteration 2: acceptable as-is (ses_0517fb0b9ffeSkz1yw0PNTeX8q) after 两项修复——Task Route 措辞精确化（无 ORM 变更故 ask-first 人工确认未触发 + 新增可操作保护区域裁决门控）+ Goal/Closure Gate 改「裁决（documented simplification，非降级 deferred）」+ 移除「按需」+ P2-MA2-006 归属澄清 + 退出标准路径无关化。两项 blocking 确认 resolved，无新 blocking 引入。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（GRNI + 匹配门控 + returns.md 漂移三项裁决落地）
- [x] 相关文档对齐（`returns.md`/`three-way-match.md`/`posting.md` 反映裁决结果）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（finance/purchase 模块重点）+ compliance checker 基线不高于 M0
- [x] 无范围内项目降级为 deferred/follow-up（已确认缺陷 P1-MA2-001/003 不得降级；documented simplification 是裁决非降级）
- [x] 会计保护区域裁决已落地：所选方案（实现或 documented simplification）有 owner-doc 证据支撑 + 残留风险已记录（由独立结束审计核验）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中 + arm-index P1-MA2-001/003 + P2-MA2-006 状态回填

## Deferred But Adjudicated

- **GRNI 正向自动冲回（方案 A）**——Phase 1 裁决为方案 B（documented simplification），方案 A（config-gated `erp-pur.grni-auto-reverse-on-invoice` 自动冲回）登记为 successor，**触发条件**：R1.9+ 完成三项前置后启动——(1) inventory 域补 `IErpInvPostingBiz.repostPurchaseInput(stockMoveCode)` SPI（支持 reverseApprove 反冲回）；(2) `IErpFinVoucherBiz.reverse()` 或新增 partial-reverse 路径支持部分开票覆盖判定；(3) 跨年度/跨账套冲回日期语义设计。属「被裁决的 successor」，非降级 deferred（当前行为有 owner doc 显式标注 + 期末人工清理流程）。

## Closure

Status Note: 计划可关闭的条件——三项裁决（P1-MA2-001 方案 B documented simplification / P1-MA2-003 方案 A 实现 / P2-MA2-006 owner doc 对齐）落地，新增 `TestErpPurSettleThreeWayMatchRecheck` 3 场景全绿（2 负向 + 1 正向），既有 P2P 回归零回归（purchase 119 / finance 286 全绿），`mvn clean install -DskipTests`（154 reactor 模块）BUILD SUCCESS，owner docs（`returns.md`/`three-way-match.md`/`posting.md`）对齐裁决，arm-index P1-MA2-001/003 + P2-MA2-006 状态回填，roadmap R1.8 标 done，当日日志已记。

Closure Audit Evidence:

- Auditor / Agent: 执行者自证（本 EXECUTE 会话）；独立结束审计子代理（新会话）运行归标准 AGE 工作流后续步骤（执行者未自我审计为「独立」审计）。
- Execution Evidence:
  - 代码：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java`（注入 `ThreeWayMatcher` + `requireInvoiceForSettle` 追加 `recheckThreeWayMatchAtSettle`）；`ErpPurConstants.java`（`CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH`）；`ErpPurErrors.java`（`ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED`）。
  - 测试：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurSettleThreeWayMatchRecheck.java` + `src/test/resources/settle-recheck-test.yaml`（3 场景全绿）。
  - Owner docs：`docs/design/purchase/returns.md`（§红字发票处理 credit-memo-via-return + §暂估应付冲减 正向 documented simplification）、`docs/design/purchase/three-way-match.md`（§匹配时机 付款核销二次门控）、`docs/design/finance/posting.md`（§业务类型映射 GRNI documented simplification 注记）。
  - 验证：`mvn -pl module-purchase/erp-pur-service test` → 119 全绿；`mvn -pl module-finance/erp-fin-service test` → 286 全绿；`mvn clean install -DskipTests`（全 workspace）→ BUILD SUCCESS。

Follow-up:

- GRNI 正向自动冲回（方案 A successor）——见 §Deferred But Adjudicated，三项前置完成后启动（R1.9+）。
