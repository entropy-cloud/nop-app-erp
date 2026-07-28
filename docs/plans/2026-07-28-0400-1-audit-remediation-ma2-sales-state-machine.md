# 2026-07-28-0400-1-audit-remediation-ma2-sales-state-machine MA2 sales 状态机审查（A2.9）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.9 sales 状态机审查（A 级单域，25 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.9）
> Related: `docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8 purchase 状态机审查范式——三轴设计 docStatus/approveStatus/业务轴 + reverseApprove→REJECTED 强制规则 + PROC vs INLINE 模式对比 + PurReversalListener 不对称同型）；`docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`（A2.2 O2C 端到端 done——销售订单/出库/发票/收款链路组件齐备 + P1-MA2-009 多币种 + 收款核销汇兑损益未实现待 MR1 + 6 项 P2 watch-only）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/sales/state-machine.md`（三轴设计 + §2 reverseApprove→REJECTED 强制规则 + §4 出库可用量校验销售独有 + §9 退货退款红字收款单 + 收款派生状态机）+`returns.md`+`quotation.md`+`contract.md`（owner doc）
> Audit: required

## Current Baseline

sales（销售）域 A 级状态机审查（单域单工作项，25 状态字段）。sales 是 O2C（Order-to-Cash）链路核心，状态机驱动报价→订单→出库→发票→收款→退货全生命周期。owner doc `state-machine.md` 采用**三轴分离**（`docStatus` erp/doc-status DRAFT/ACTIVE/CANCELLED + `approveStatus` wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED + 业务轴 receivedStatus/deliveryStatus/writtenOffStatus），与采购域**镜像对称**，差异点（出库可用量校验销售独有 / 收款方向相反 / 退货退款红字收款单）已在 owner doc 标注。

实时仓库已落地的销售状态机实现（待审查，路径 `module-sales/`）：

- **三轴状态字段清单**（ORM `app-erp-sales.orm.xml`，25 状态字段分布于业务单据实体）：
  - `ErpSalOrder`：`docStatus`(erp/doc-status) + `approveStatus`(wf/approve-status)
  - `ErpSalDelivery`（销售出库单）：`docStatus` + `approveStatus`
  - `ErpSalInvoice`（销售发票）：`docStatus` + `approveStatus` + `receivedStatus`(erp-sal/received-status) + `deliveryStatus`(erp-sal/delivery-status)
  - `ErpSalReceipt`（收款单）：`docStatus` + `approveStatus` + `receivedStatus`
  - `ErpSalReturn`（销售退货单）：`docStatus` + `approveStatus` + `writtenOffStatus`(复用 erp-sal/received-status)
  - `ErpSalQuotation`（报价单）/ `ErpSalContract`（销售合同）：`docStatus` + `approveStatus`
  - 平台字典 `erp/doc-status` + `wf/approve-status` 定义在 nop-entropy（非本仓）；域字典 `erp-sal/received-status`、`erp-sal/delivery-status` 在本仓 ORM。
  - **收款状态是派生状态**（owner doc §收款状态机 明示「由系统根据累计已核销金额/发票金额自动计算」）。
- **状态迁移实现**（`module-sales/erp-sal-service/.../service/`）：需审查实体服务的 Processor（submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel 全守卫）vs INLINE（xbiz 脚本直设状态）模式分布——**与 purchase A2.8 同型关注点**：是否存在 reverseApprove 目标态违反 owner doc §2 强制 REJECTED 规则（→SUBMITTED 违规）/ INLINE reject/withdrawApproval 缺 isCancelled 守卫 / 三种并行模式不一致。已知 `ReceiptSettler.java`（收款核销）+ `ErpSalOrderBizModel`/`ErpSalInvoiceLineBizModel`/`ErpSalReturnLineBizModel` 等 BizModel 文件存在。
- **出库可用量校验**（owner doc §2/§4 明示销售独有）：`ErpSalDelivery` approve 时须校验可用量（现有量 − 预留量）≥ 出库数量，不足则拒绝、整个出库单审核回滚；并触发 `IErpInvStockMoveBiz` 跨域写 outgoing 移动单。**最易遗漏的控制点**（owner doc §审查提示首条）。
- **跨域访问**：`IErpInvStockMoveBiz`（出库/退货写库存移动单）/ `IErpMdPartnerBiz`（客户 active 守卫）/ `IErpFinVoucherBiz`（发票/收款/退货过账跨域写会计保护区域）/ `IErpFinBudgetCommitmentBiz`（承付，若有）。daoFor 跨域只读已在 MA1 登记（P1-MA1-022 含 `ErpSalOrderProcessor:377,389`），本审计复核状态机角度。
- **过账集成**：`SalAcctDocProvider`（AR_INVOICE/RECEIPT/SALES_RETURN/SALES_OUTPUT createFacts）+ PostingDispatcher（tryPost 吞异常 / reverse 硬前置——与 finance P1-MA2-032 + purchase P1-MA2-051 同型）+ 域侧 SalReversalListener（若有，finance→sales 反向回滚——对称于 PurReversalListener，需核验 rollback 对称性）。
- **信用控制**（P2-MA2-012）：`CreditLimitChecker.checkCreditHold` 三级策略（SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL）config-gated 扩展至出库/发票审核——owner doc flow-overview.md §2.2 漏述。
- **测试覆盖**：需审查销售状态机相关测试（TestErpSalOrderApproval/DeliveryApproval/InvoiceApproval/ReceiptApproval/ReceiptSettlement/ReturnApproval/ReturnRefundEndToEnd/OrderToCashEnd 等）。

**已登记的直指销售状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-022`（todo MR1，9 域合并）：sal `daoFor(ErpMdSubject/ErpFinAccountingPeriod)` 只读（`ErpSalOrderProcessor:377,389`）。**状态机 scope**：跨域只读是 budget/period 查询副作用，不破坏状态机——本审计复核异常路径无悬挂。
- `P1-MA2-009`（todo MR1，O2C）：多币种 O2C + 收款核销汇兑损益未实现。**状态机 scope**：状态迁移不涉及币种——状态机角度无影响；但收款核销 `ReceiptSettler.settle` 守卫完整性需复核（对称于 purchase P1-MA2-003 PaymentSettler settle 守卫缺口）。
- `P2-MA2-010`（todo MR1，sales）：销售发票 approve 无订单-发票金额比对守卫。**状态机 scope**：approve 前置守卫缺口——本审计复核是否破坏 approve 路径正确性。
- `P2-MA2-011`（todo MR1，docs+sales）：returns.md §红字发票处理 doc drift（实现以 SALES_RETURN 过账 + 负 ArApItem credit memo 替代）。**状态机 scope**：退货过账路径——本审计复核 return approve→过账状态迁移正确性。
- `P2-MA2-012`（todo MR1，docs sales）：信用控制扩展点 owner doc 漏述。**状态机 scope**：approve 信用冻结守卫——本审计复核守卫完整性。
- `P2-MA2-013`（todo MR1，docs+sales）：收款核销仅发票维度（订单维度预收款未实现）。**状态机 scope**：receipt settle 维度——本审计复核 settle 守卫。
- `P2-MA2-014`（todo MR1→A2.17，sales）：`ReceiptSettler.settle:55-111` 无锁并发核销同一发票可双读双写过收。**状态机 scope**：并发 RECEIVED 漂移——交接 A2.17，本审计标注并发敏感点。
- `P2-MA2-015`（todo MR1，docs sales+finance）：出库-开票跨月期间配比 owner doc 漏述。**状态机 scope**：无影响（期间配比归期末结账）。
- `P2-MA2-038`（todo MR1，finance/sales/purchase）：域侧-finance 双路径核销无对账守卫。**状态机 scope**：双路径设计并行——本审计复核状态迁移一致性。

**但从未做过一次覆盖销售全状态机（订单/出库/发票/收款/退货/报价/合同七实体 × 三轴）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（owner doc §审查提示 + 与 purchase A2.8 同型关注点）：

- **状态定义清晰性**：receivedStatus 派生状态（owner doc 明示自动计算）vs DB 持久化的写时机一致性；writtenOffStatus 复用 received-status 字典语义；deliveryStatus 派生（发货进度）滚动汇总一致性；invoice receivedStatus 与 receipt 核销的派生计算正确性。
- **转换完整性**：**PROC vs INLINE 模式等价性**（与 purchase 同型——核验七实体状态迁移动作的实现模式分布）；**reverseApprove 目标态**（owner doc §2 强制 REJECTED——核验是否所有实体合规，报价/合同是否违规 →SUBMITTED）；INLINE reject/withdrawApproval 缺 isCancelled/客户 active/行非空守卫；**出库 approve 可用量校验前置**（销售独有，最易遗漏）；**收款核销 settle 前置**（发票 APPROVED + 客户匹配 + 余额不超 + 信用未冻结）；quotation→order 转换前置。
- **终端状态与恢复**：docStatus CANCELLED 终态；approveStatus REJECTED 可重新 submit；reverseApprove 红冲恢复（posted=false + APPROVED→REJECTED）；receivedStatus RECEIVED 终态（再核销回退经 reverseSettlement）。
- **异常路径**：出库可用量不足（approve 拒绝回滚——销售独有）；approve 已 CANCELLED（PROC 守卫 vs INLINE 缺）；settle 超余额（守卫拒绝）；过账 tryPost 吞异常（posted=false 悬挂——同 finance P1-MA2-032 + purchase P1-MA2-051 IGNORED 同型）；**退货退款红字收款单 + 回退发票状态**（owner doc §3/§9——核验完整性）；客户停用后开单（守卫拒绝）；SalReversalListener 反向回滚对称性（若存在）。
- **可达性**：reverseApprove 各实体目标态一致性；withdrawApproval→UNSUBMITTED→submit→approve 回环可达性；receivedStatus UNRECEIVED→PARTIAL→RECEIVED 派生可达性。
- **角色与权限**：提交（销售员）/审核（销售主管）/settle（出纳/会计）；危险操作（approve 触发出库跨域库存写 + 过账跨域会计写 + 信用冻结 / settle 资金核销 / reverseApprove 红冲恢复余额 / cancel 已过账须 reverse 凭证）；多角色冲突（销售员 approve vs 出纳 settle vs 会计 reverseApprove）。
- **外部依赖**：approve→出库移动单（IErpInvStockMoveBiz 跨域写）/ 发票·收款·退货→过账（IErpFinVoucherBiz 跨域写会计保护区域）/ 客户 active 守卫（IErpMdPartnerBiz）/ SalReversalListener 反向（finance→sales）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚 vs tryPost 吞异常解耦）。
- **TODO/任务策略**：SUBMITTED 审批 TODO；UNRECEIVED/PARTIAL 收款 TODO；REJECTED 修改重提 TODO；赠品库存扣减 TODO；是否存在期望有人行动但不产生待办的状态。
- **场景演练**：(a) O2C 黄金路径（报价→订单 approve→出库 approve+可用量校验+库存写→发票 approve+过账→收款 approve+settle+过账）；(b) 出库可用量不足（approve 拒绝回滚——销售独有）；(c) reverseApprove 红冲（各实体→REJECTED+posted=false+凭证 reverse）；(d) withdrawApproval 回环；(e) cancel 已过账；(f) settle/reverseSettlement；(g) 退货退款（红字收款单+回退发票状态——完整性）；(h) 信用冻结 HARD_BLOCK（approve 拒绝）；(i) 赠品库存扣减；(j) 并发 settle 同发票（无锁——P2-MA2-014，交接 A2.17）。
- **与设计文档一致性**：`state-machine.md`/`returns.md`/`quotation.md`/`contract.md` vs 实现——重点核验：(1) §2 reverseApprove→REJECTED 是否被任何实体违反；(2) §4 出库可用量校验是否落实；(3) §9 退货退款红字收款单路径是否完整；(4) 收款派生状态写时机；(5) 信用控制扩展点 owner doc 漂移。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**出库 approve 可用量校验缺失** [若破坏销售独有约束，按 owner doc §4 裁决 P1/P0] / **reverseApprove→SUBMITTED 任意实体违规** [契约漂移，按 finance reverseApprove 强一致范式裁决] / **SalReversalListener 回滚不对称致冲销后 sales 单据状态悬挂** [若破坏业财一致——需核验兜底] / **退货退款红字收款单缺失致发票 receivedStatus 不回退** [若破坏退款闭环]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **订单/出库/发票/收款/退货/报价/合同七实体 × 三轴（docStatus/approveStatus/业务轴）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（receivedStatus 派生 vs 持久化写时机 / writtenOffStatus 复用 received-status / deliveryStatus 滚动汇总）；(2) 转换完整性（**PROC vs INLINE 模式等价性** / **reverseApprove 目标态合规** / **INLINE 缺守卫** / **出库 approve 可用量校验前置** / **settle 前置** / quotation→order 转换）；(3) 终端与恢复；(4) 异常路径（**出库可用量不足** / approve 已 CANCELLED / settle 超余额 / **过账 tryPost 吞异常悬挂** / **退货退款红字收款单+回退发票状态完整性** / SalReversalListener 对称性）；(5) 可达性；(6) 角色权限（出库库存写 / 过账跨域会计写 / 信用冻结 / settle 资金）；(7) 外部依赖；(8) TODO 任务策略；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在销售状态机运行时的行为影响：P1-MA1-022（跨域只读）/ P1-MA2-009（多币种——状态机角度无影响，但 settle 守卫复核）/ P2-MA2-010（approve 金额守卫）/ P2-MA2-011（退货过账 drift）/ P2-MA2-012（信用控制扩展）/ P2-MA2-013（settle 维度）/ P2-MA2-014（并发核销——交接 A2.17）/ P2-MA2-015（期间配比——无影响）/ P2-MA2-038（双路径核销——设计并行复核），标注终态。
- scope matrix §状态机正确性 sal 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.9 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.2 O2C 端到端编排正确性 — done；本审计只复核销售状态机迁移正确性（O2C 链路组件齐备已确认，多币种/汇兑损益归 A2.2 finding P1-MA2-009）。
- **不**审计 A2.5 finance 凭证/AR-AP 状态机 — done；本审计只确认销售过账经 finance I*Biz（SalPostingExecutor→IErpFinVoucherBiz）+ SalReversalListener 反向回滚的**状态机迁移**正确性。
- **不**审计 A4.5 pur+sal+inv+qa+crm 代码质量 — Processor/BizModel 代码质量系统性审查归 A4.5；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发 settle/出库扣批次归 A2.17（P2-MA2-014）；本审计只标注观察到的并发敏感点。
- **不**审计 A4.7 view.xml drift — 销售页面契约漂移归 A4.7。
- **不**审计 config-gated Deferred 偏离是否应实现（信用控制 config-gated / 负库存 / 多级审批链） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/sales/state-machine.md`（三轴设计 + §2 reverseApprove→REJECTED 强制规则 + §4 出库可用量校验销售独有 + §9 退货退款红字收款单 + 收款派生状态机 — **需复核 reverseApprove 合规 + 可用量校验落实 + 退货退款完整性**）；`docs/design/sales/returns.md`（退货退款状态机 + 红字收款单 + 回退发票状态）；`docs/design/sales/quotation.md`（报价→订单转换）；`docs/design/sales/contract.md`（销售合同状态轴）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层 — 复核模式分布）；`docs/architecture/posting-exemptions.md`（销售过账跨域写豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.9 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：销售状态机本身非 ask-first 最高级保护区域，但**过账副作用触及 finance 凭证链**（invoice/receipt/return approve→IErpFinVoucherBiz.post 跨域写会计保护区域）+ **出库/退货触及库存写**（delivery/return→IErpInvStockMoveBiz）+ **信用冻结触及资金**。P0 即时修复若触及 `ErpSal*Processor`/`ReceiptSettler`/`Sal*PostingDispatcher`/`SalReversalListener`/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/库存/资金保护区域）。ORM 字典变更（received-status/delivery-status）属 ask-first。xbiz 文件变更（状态迁移动作脚本）属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 销售状态机系统性业务审查

Status: completed
Targets: `module-sales/erp-sal-service/.../service/processor/ErpSal*Processor.java`（submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel + 守卫 validateTransition*/validateNotCancelled/requireCustomerActive/requireLinesNonEmpty/doApprove/doReverseApprove + IErpMdPartnerBiz/IErpFinBudgetCommitmentBiz）；`.../service/entity/ReceiptSettler.java`（settle/reverseSettlement/recomputeInvoiceReceived — 派生 receivedStatus 计算）；`.../service/entity/ErpSal{Order,InvoiceLine,ReturnLine,Receipt,Quotation,Contract}BizModel.java`；出库可用量校验组件（ErpSalDeliveryProcessor 内 + IErpInvStockMoveBiz 跨域写）；`.../service/posting/Sal*PostingDispatcher.java`+`SalAcctDocProvider.java`（tryPost 吞异常/reverse 硬前置）+ SalReversalListener（若存在，finance→sales 反向）；信用控制 CreditLimitChecker
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-022 跨域只读已登记，本审计复核状态机角度）；A2.2 done（O2C 端到端，P1-MA2-009 + P2-MA2-010~015/038 已登记，本审计复核状态机角度）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞误同型范式）；A2.8 done（purchase 状态机三轴 + PROC vs INLINE 范式）

- [x] 维度「状态定义」：审查三轴组合语义；receivedStatus 派生状态 vs DB 持久化写时机一致性；writtenOffStatus 复用 received-status 字典语义匹配；deliveryStatus 派生滚动汇总一致性；invoice receivedStatus 与 receipt 核销派生计算。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出七实体 × 三轴迁移矩阵——**PROC vs INLINE 模式等价性核验**；**reverseApprove 目标态合规性**（owner doc §2 强制 REJECTED，核验报价/合同是否违规 →SUBMITTED）；INLINE reject/withdrawApproval 缺守卫；**出库 approve 可用量校验前置**（销售独有——重点）；**settle/reverseSettlement 前置**（发票 APPROVED+客户匹配+余额不超+信用未冻结）；quotation→order 转换前置。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：docStatus CANCELLED 终态；approveStatus REJECTED 可重新 submit；reverseApprove 红冲恢复（posted=false+APPROVED→REJECTED）；receivedStatus RECEIVED 终态（再核销回退经 reverseSettlement）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——**出库可用量不足**（approve 拒绝回滚——销售独有）；approve 已 CANCELLED（PROC 守卫 vs INLINE 缺）；settle 超余额（守卫拒绝）；过账 tryPost 吞异常（posted=false 悬挂——同 finance P1-MA2-032 IGNORED 同型）；**退货退款红字收款单 + 回退发票状态**（owner doc §3/§9——完整性重点）；客户停用后开单（守卫拒绝）；SalReversalListener 反向回滚对称性（若存在——对称于 PurReversalListener.rollbackReceive 不对称发现）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：reverseApprove 各实体目标态一致性（重点——是否同一概念两态）；withdrawApproval→UNSUBMITTED→submit→approve 回环可达性；receivedStatus UNRECEIVED→PARTIAL→RECEIVED 派生可达性；是否有死循环或不可达终态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——提交（销售员）/审核（销售主管）/settle（出纳/会计）；危险操作（**approve 触发出库跨域库存写+过账跨域会计写+信用冻结** / settle 资金核销 / **reverseApprove 红冲恢复余额** / cancel 已过账须 reverse 凭证）；多角色冲突。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：approve→出库移动单（IErpInvStockMoveBiz 跨域写）/ 发票·收款·退货→过账（IErpFinVoucherBiz 跨域写会计保护区域）/ 客户 active 守卫（IErpMdPartnerBiz）/ SalReversalListener 反向（finance→sales）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚 vs tryPost 吞异常解耦）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：SUBMITTED 审批 TODO；UNRECEIVED/PARTIAL 收款 TODO；REJECTED 修改重提 TODO；赠品库存扣减；是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) O2C 黄金路径；(b) **出库可用量不足**（销售独有）；(c) **reverseApprove 红冲**（各实体→REJECTED+posted=false+凭证 reverse）；(d) **withdrawApproval 回环**；(e) cancel 已过账；(f) **settle/reverseSettlement**；(g) **退货退款**（红字收款单+回退发票状态——完整性）；(h) **信用冻结 HARD_BLOCK**；(i) 赠品库存扣减；(j) 并发 settle 同发票（无锁——交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`returns.md`/`quotation.md`/`contract.md` 是否有匹配——重点漂移：(1) §2 reverseApprove→REJECTED 是否被违反；(2) §4 出库可用量校验是否落实；(3) §9 退货退款红字收款单完整性；(4) 收款派生状态写时机；(5) 信用控制扩展点 owner doc 漂移。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 销售状态机角度：P1-MA1-022 / P1-MA2-009（settle 守卫复核）/ P2-MA2-010/011/012/013/014/015/038，标注终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（含：七实体×三轴状态图与转换矩阵、PROC vs INLINE 模式对比矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、MA1/MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 七实体×三轴状态图与转换矩阵 + PROC vs INLINE 模式对比矩阵产出，每个状态/转换/模式有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含模式等价性 + reverseApprove 合规 + 出库可用量校验 + settle 前置] / 终端与恢复 / 异常路径[含出库可用量不足 + 过账吞异常悬挂 + 退货退款红字收款单完整性 + SalReversalListener 对称性] / 可达性 / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 销售状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 sal 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**出库 approve 可用量校验缺失** [若破坏销售独有约束] / **reverseApprove→SUBMITTED 任意实体违规** [契约漂移] / **SalReversalListener 回滚不对称致状态悬挂** [若破坏业财一致] / **退货退款红字收款单缺失致 receivedStatus 不回退** [若破坏退款闭环]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/库存/xbiz 契约保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如 reverseApprove 违规 / 模式不一致 / 可用量校验缺口 / 退货退款闭环缺口 / SalReversalListener 不对称 / 派生状态写时机漂移）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 sal 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_059ca59d8ffeHEnOQmXcxqNkU1`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：13 个 owner doc 全部存在 ✓；9 个已登记 finding ID（P1-MA1-022 / P1-MA2-009 / P2-MA2-010/011/012/013/014/015/038）在 arm-index 描述匹配 ✓；7 个销售实体（ErpSalOrder/Delivery/Invoice/Receipt/Return/Quotation/Contract）在 ORM 存在 ✓；状态字段数 25 与 roadmap 一致 ✓；ErpSalOrderProcessor daoFor 行号 ~377/389 匹配 P1-MA1-022 ✓；反松弛无禁词 ✓；结构与 reference purchase plan 一致 ✓。Current Baseline 对 PROC/INLINE 模式采用"需审查"hedging（非断言）——草案审计计划合理。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。销售过账/出库库存写触及会计/库存保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.9 销售状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/returns/quotation/contract owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-sales/erp-sal-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为未勾选项作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.2 O2C 端到端编排 + 多币种/汇兑损益

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.2 done（O2C 链路组件齐备已确认）。本审计做销售状态机**迁移正确性**审查；多币种/汇兑损益归 A2.2 finding（P1-MA2-009 待 MR1）。
- Successor Required: `no`——A2.2 已 done，finding 待 MR1。

### A4.5 pur+sal+inv+qa+crm 代码质量审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做销售状态机**业务正确性**审查；Processor/BizModel 代码质量系统性审查归 A4.5。
- Successor Required: `yes`——A4.5 执行时复核。

### A2.17 并发与乐观锁（并发 settle/出库扣批次）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17（P2-MA2-014 已登记）。本审计标注观察到的并发敏感点（ReceiptSettler 无锁 / 并发出库扣同一批次），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（信用控制 config-gated / 负库存 / 多级审批链）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred/Non-Goal。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如信用控制全面启用 / 多级审批链上线）。

## Closure

Status Note: 执行完成（2026-07-28）。**审计裁决 ⚠️(P1)，零 P0**。Phase 1（10 维度系统性审查）+ Phase 2（P0 即时通道处理[零 P0] + P1 汇总 + 索引/矩阵更新）全部 done。独立 closure audit 已通过（独立子代理新会话，2026-07-28）。

Closure Audit Evidence:

- **执行结果**：审计报告 `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md` 已产出（396 行，含七实体×三轴状态图与转换矩阵 + PROC vs INLINE 模式对比矩阵 + 10 维度裁决 + 控制点 PASS/FAIL + 已登记 finding 复核表 + 并发敏感点交接 A2.17 + 残留风险）。
- **裁决**：**⚠️(P1)，零 P0**。四个候选 P0 经证据证伪或降级：(1) Contract reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环（Contract 无 posted 副作用）→ P1-MA2-056；(2) INLINE withdrawApproval + Contract 全 INLINE 缺守卫但不破坏主终态（docStatus=CANCELLED 持有）→ P1-MA2-057；(3) SalReversalListener.rollbackDelivery 不对称但 Javadoc deliberate + 业务侧恢复路径完整（经 ensureReversed 链可恢复，与 purchase P1-MA2-051 不同）+ 不破坏业财一致 → P2-MA2-057 watch-only；(4) 过账 tryPost 吞异常悬挂与 finance P1-MA2-032 + purchase 同型根因，Deferred 兜底 → 不升 P0。
- **销售独有约束已落实**：(a) 出库 approve 可用量校验经库存域 `ErpInvStockMoveProcessor.doConfirm→validateAvailable` 强制（`available < required` 抛 ERR_AVAILABLE_INSUFFICIENT + 整个 approve 经 @BizMutation 事务回滚）✓；(b) 退货退款红字收款单 + 回退发票状态经 `ReturnRefundOrchestrator.orchestrateRefund` + `receiptSettler.reverseSettlement` 自然回退 receivedStatus/Amount ✓。
- **新登记 finding**：2 项 P1（P1-MA2-056 Contract reverseApprove→SUBMITTED 契约漂移 / P1-MA2-057 6 实体 INLINE withdrawApproval + Contract 全 INLINE 缺守卫——与 purchase P1-MA2-049/050 同型）+ 3 项 P2 watch-only（P2-MA2-056 三种并行模式 + 6 实体 vs Contract 模式分裂 owner doc 未声明 / P2-MA2-057 SalReversalListener.rollbackDelivery 不对称 deliberate owner doc 未同步[与 purchase P1-MA2-051 同型但降为 P2] / P2-MA2-058 ErpSalReturn writtenOffStatus/returnStatus/refundStatus 未落地为 ORM 存储字段[returns.md:88-93 已显式漂移注记]）。
- **MA1/MA2 finding 复核**：9 项已登记 finding（P1-MA1-022 / P1-MA2-009 / P2-MA2-010/011/012/013/014/015/038）运行时复核**无升级**（详见审计报告 §4）。
- **索引/矩阵更新**：`docs/audits/arm-index.md` 报告清单 + P1 详细清单 + P2 清单 + A2.9 总结块已新增；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 sal 列由 `❓` 推进至 `⚠️P1(A2.9✅)`。
- **回归基线验证**：零 P0 即时修复（审计不改代码）→ 全量 `mvn clean install -DskipTests` BUILD SUCCESS（154 reactor 模块全绿，2026-07-28T09:14:15+08:00）+ `mvn test -pl module-sales/erp-sal-service -am` BUILD SUCCESS（含 sales-service + 上游依赖 finance/inventory/quality/notify service 全绿，2026-07-28T09:17:11+08:00）。
- **roadmap 更新**：`docs/backlog/audit-remediation-roadmap.md` A2.9 由 `todo` 推进至 `done`。
- **独立 closure audit**：已通过（2026-07-28，独立子代理新会话 fresh-context，对照实时仓库逐项复核）。审计裁决 ⚠️(P1)/零 P0 经证据复核确认：审计报告 `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（383 行）存在且内容实质（10 维度裁决 + 七实体×三轴矩阵 + PROC vs INLINE 对比 + 控制点 PASS/FAIL + 已登记 finding 复核表 + 并发敏感点交接 A2.17 + 残留风险）；4 项 finding（P1-MA2-056 Contract reverseApprove→SUBMITTED / P1-MA2-057 6 实体 INLINE + Contract 全 INLINE 缺守卫 / P2-MA2-056 模式分裂 owner doc 未声明 / P2-MA2-057 rollbackDelivery 不对称 / P2-MA2-058 Return 业务轴字段未落地）经实仓代码逐项证伪/证实——P1-MA2-056 经 `ErpSalContract.xbiz:97` 直设 SUBMITTED 确认 / P1-MA2-057 经 Contract withdrawApproval 仅校验 src==='SUBMITTED' 缺 isCancelled 守卫确认 / P2-MA2-057 经 `SalReversalListener.rollbackDelivery:109-120` 仅 posted=false 保留 APPROVED（Javadoc deliberate）确认；arm-index.md P1/P2 清单 + 报告清单 + A2.9 总结块已新增（P1-MA2-056/057 + P2-MA2-056/057/058）；scope matrix §状态机正确性 sal 列 `⚠️P1(A2.9✅)`；roadmap A2.9 `done`；日志 `docs/logs/2026/07-28.md` 已记。反空壳：audit-only 计划无生产代码变更，finding 全部指向实仓代码行号可追溯。延迟诚实：P1 经 R1.0 展开机制进 MR1 非降级；Deferred 仅含 owner-doc 裁定的 config-gated/out-of-scope 项（A2.2/A4.5/A2.17/config-gated）且全部声明 successor 触发条件。文本一致性：Plan Status completed ↔ 两 Phase completed ↔ Exit Criteria 全 [x] ↔ Closure Gates 全 [x] ↔ 日志一致。

Follow-up:

- MR1 修复 P1-MA2-056/057（与 purchase P1-MA2-049/050 同型，建议一并裁决方案 A：xbiz reverseApprove 改 REJECTED + INLINE withdrawApproval 迁移到 Processor）。
- MR1 顺手收敛 P2-MA2-056/057/058（owner doc 同步 + Return 业务轴字段评估）。
- A2.17 并发审计：5 处并发敏感点（ReceiptSettler 无锁 / order.deliveryStatus 滚动汇总 stale read / SalReversalListener 并发回滚 / ReturnRefundOrchestrator 并发退款）。
