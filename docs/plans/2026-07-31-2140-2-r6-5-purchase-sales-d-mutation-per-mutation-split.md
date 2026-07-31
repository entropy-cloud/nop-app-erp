# 2026-07-31-2140-2-r6-5-purchase-sales-d-mutation-per-mutation-split purchase + sales 域 D-mutation + 内联多步 mutation per-mutation 拆分

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.5
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split.md`（R6.1 同范式先例 + helper 归属裁决）；`docs/plans/2026-07-30-1433-*-mr5-r5-1-purchase*`/`...-r5-2-sales*`（R5.1/R5.2 S-mutation 先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.5
> Audit: required

## Current Baseline

- **MR5 purchase（R5.1）+ sales（R5.2）域 S-mutation 已完成**：Payment/Quotation/Receipt 各 6 个 S-mutation per-mutation Processor 自包含（Glob 实测 ErpPurPayment{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval,Cancel}Processor + ErpSalQuotation{...}Processor + ErpSalReceipt{...}Processor 均存在），3 facade 公共 S-mutation 方法已精简为单行委托。MR6 **不重开 MR5**。
- **类别 A 违规 facade（3 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + 公共 D-mutation 入口 + 处置**：
  - `ErpPurPaymentProcessor`（326 行）— D-mutation 入口 2：`settle`、`reverseSettlement`。处置：**slim-to-S-delegation-facade**（保留 S-mutation 单行委托 + delete 2 D-mutation）。
  - `ErpSalQuotationProcessor`（313 行）— D-mutation 入口 2：`confirmCustomerAccepted`、`convertToOrder`。处置：**slim-to-S-delegation-facade**。
  - `ErpSalReceiptProcessor`（278 行）— D-mutation 入口 2：`settle`、`reverseSettlement`。处置：**slim-to-S-delegation-facade**。
  - **类别 A 须拆合计：6 D-mutation → 6 个新 `<Entity><Method>Processor`**（PurPayment 2 + SalQuotation 2 + SalReceipt 2）。D-mutation per-mutation 文件**尚不存在**（Glob 实测各域 processor 目录仅含 S-mutation 文件 + facade），本 plan 须**新建**。
- **类别 A BizModel 配线现状**（实测）：3 BizModel 对 S-mutation 已 `@Inject` per-mutation Processor（MR5 成果），对 D-mutation 仍委托对应 facade（`return facade.method(...)`）。facade 瘦身 D-mutation 后，3 BizModel 须**重配线** D-mutation 为 `@Inject` 对应 per-mutation Processor + 单行委托（S-mutation 配线保持不动）。
- **类别 B 违规 BizModel（1 个，1 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）**：`ErpPurSupplierScorecardBizModel`（位于 `.../service/entity/` 包，`finalizeScorecard` 方法）。须拆 → `ErpPurSupplierScorecardFinalizeScorecardProcessor`。
- **须拆合计：7**（类别 A 6 + 类别 B 1），与 roadmap R6.5 行计数一致。另 roadmap R6.5 注记 R6.8 backstop 含 `ErpPurRequisitionProcessor.convertToOrder`（单 D-mutation facade）——该 backstop 由 R6.8 承接，**不在本 plan 范围**。
- **[会计保护区域]** Payment.settle/reverseSettlement 涉及付款核销 + AP 余额 + 凭证；Receipt.settle/reverseSettlement 涉及收款核销 + AR 余额 + 凭证；Quotation.convertToOrder/confirmCustomerAccepted 为运营操作（无凭证）。R1.8（P2P 核销复核）+ R1.9（O2C 收款核销汇兑损益）+ R1.16（业财过账悬挂）+ R2.14（核销/成本链路测试）已修复相关缺陷。owner doc `docs/design/purchase/`+`docs/design/sales/`（state-machine）已固化语义。本 plan 仅做**编排位置迁移**（facade/BizModel → per-mutation Processor），不改业务语义或核销/凭证逻辑。
- **既有测试基线**：purchase 域 erp-pur-service 测试源文件 36 个；sales 域 erp-sal-service 测试源文件 37 个（R5.1/R5.2 实测行为等价基线）。
- **helper 归属裁决（继承 R6.1 方案 A）**：facade 被多 D-mutation 共享的 protected helper（如 `ErpPurPaymentProcessor.requirePayment`/核销 helper、`ErpSalReceiptProcessor.matchOpenItems`）保留 facade protected + per-mutation 经 `@Inject` facade 调用（同包 protected 可达，单一真相源）。

## Goals

- purchase + sales 域 7 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 6 + 类别 B 1），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 类别 A 3 facade 全部 slim-to-S-delegation（保留 S-mutation 单行委托 + delete D-mutation）；3 BizModel D-mutation 重配线为 `@Inject` per-mutation Processor + 单行委托。facade 共享辅助方法保留 facade protected helper（继承 R6.1 方案 A）。
- 类别 B 1 个 BizModel（SupplierScorecard）的内联 `@BizMutation` `finalizeScorecard` 改为 `@Inject` Processor + 单行委托。
- beans.xml 注册全部新 Processor bean（bean id = 全限定类名）；xbiz 无 inline-script 残留。
- purchase + sales 域 `mvn test` 全绿（0 failures），会计保护区域（核销/凭证/AR-AP 余额）语义不变经既有测试验证。
- arm-index P1-MA3-062 purchase+sales 域须拆项标记 done。

## Non-Goals

- R6.4/R6.6-R6.8（其他域 + 全量验证）——属同批或后续 plan。
- Payment/Quotation/Receipt S-mutation 重构（MR5 R5.1/R5.2 已完成，状态保持 done）。
- 新增业务测试——本 plan 仅验证既有测试行为等价。
- 业务语义变更、核销算法调整、状态机迁移、错误码语义调整——仅编排位置迁移。
- `ErpPurRequisitionProcessor.convertToOrder`（R6.8 backstop 单 D-mutation facade）——由 R6.8 承接。
- PurQuotation.cancel / PurRfq.cancel（合法豁免 `:46` 单步状态翻转，保留 BizModel）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/purchase/`（state-machine）、`docs/design/sales/`（state-machine）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`。涉及会计保护区域（付款/收款核销 + 凭证），须对照 R1.8/R1.9/R1.16/R2.14 owner doc 静态校验语义不变。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（3 facade → 6 per-mutation Processor）+ 类别 B BizModel 拆分（1 → 1）+ BizModel 重配线

Status: planned
Targets: `module-purchase/erp-pur-service/.../processor/ErpPurPayment*Processor.java`（新建 2）；`module-sales/erp-sal-service/.../processor/ErpSal{Quotation,Receipt}*Processor.java`（新建 4）；`module-purchase/erp-pur-service/.../processor/ErpPurSupplierScorecardFinalizeScorecardProcessor.java`（新建 1）；3 facade 瘦身；4 BizModel 重配线/改单行委托；`.../_vfs/erp/{pur,sal}/beans/app-service.beans.xml` 注册新 bean
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [ ] Decision: 辅助方法归属策略——继承 R6.1 方案 A：facade 被多 D-mutation 共享的 protected helper 保留 facade（同包 protected 可达），per-mutation 经 `@Inject` facade 调用。在首个 facade（PurPayment）拆分时确认 helper 可达性并记录替代分析。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpPurPaymentProcessor` 2 D-mutation 拆分 → `ErpPurPaymentSettleProcessor` / `...ReverseSettlementProcessor`。facade slim-to-S-delegation。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpSalQuotationProcessor` 2 D-mutation 拆分 → `ErpSalQuotationConfirmCustomerAcceptedProcessor` / `...ConvertToOrderProcessor`。facade slim-to-S-delegation。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpSalReceiptProcessor` 2 D-mutation 拆分 → `ErpSalReceiptSettleProcessor` / `...ReverseSettlementProcessor`。facade slim-to-S-delegation。
  - Skill: `nop-backend-dev`
- [ ] Add: 类别 B `ErpPurSupplierScorecardBizModel.finalizeScorecard` 内联 `@BizMutation` 提取到 `ErpPurSupplierScorecardFinalizeScorecardProcessor`（process + protected step），BizModel 改 `@Inject` Processor + 单行委托。
  - Skill: `nop-backend-dev`
- [ ] Add: beans.xml 注册全部 7 新 Processor bean（bean id = 全限定类名）。
  - Skill: `nop-backend-dev`
- [ ] Add: 类别 A 3 BizModel（Payment/Quotation/Receipt）D-mutation 重配线为 `@Inject` 对应 per-mutation Processor + 单行委托（S-mutation 配线保持不动）。
  - Skill: `nop-backend-dev`
- [ ] Proof: purchase + sales service 本地编译通过（`mvn compile -pl module-purchase/erp-pur-service,module-sales/erp-sal-service -am -DskipTests`）+ grep 确认类别 B BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 6 + 类别 B 1 = 7 per-mutation 自包含 + 3 facade 瘦身 + 4 BizModel 重配线/改单行委托 + 编译通过。

- [ ] 7 个新 `<Entity><Method>Processor` 文件存在且自包含（`process()` + protected step，非回委托）
- [ ] 3 类别 A facade slim-to-S-delegation（保留 S-mutation 委托 + delete D-mutation）+ 3 BizModel D-mutation 重配线 + SupplierScorecardBizModel 改单行委托 + beans.xml 更新
- [ ] purchase + sales service 本地编译通过

### Phase 2 - purchase + sales 域运行时行为等价回归

Status: planned
Targets: `module-purchase/erp-pur-service/src/test/`、`module-sales/erp-sal-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] Proof: purchase 域 `mvn test -pl module-purchase/erp-pur-service -am` + sales 域 `mvn test -pl module-sales/erp-sal-service -am` 全绿（0 failures）。mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 purchase + sales 域行为等价证据。

- [ ] purchase + sales 域 `mvn test` 全绿（0 failures）
- [ ] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: accept（task `ses_046b87e40ffew5XkhKuU2VJYiQ`）— 全部事实声明独立实仓复核通过：facade 行数（PurPayment 326/SalQuotation 313/SalReceipt 278 精确）、D-mutation 方法名（settle/reverseSettlement/confirmCustomerAccepted/convertToOrder）、3 个 slim 处置经 Glob 确认各实体均有完整 6 个 MR5 S-mutation per-mutation 文件（无 R6.4 式误标）、catB SupplierScorecardBizModel 位置/内联 finalizeScorecard、7 计数算术、R6.8 backstop 正确排除、豁免（PurQuotation.cancel/PurRfq.cancel）已登 registry。source-of-truth 合规、scope 单一结果面、内部一致（7=catA6+catB1，3 slim）、plan-guide 全项达标。可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 purchase+sales 域 + compliance + 全量编译。

- [ ] purchase + sales 域 7 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 6 + 类别 B 1）
- [ ] 3 类别 A facade slim-to-S-delegation 执行（保留 S-mutation 委托 + delete D-mutation）
- [ ] 3 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [ ] 1 类别 B BizModel 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托
- [ ] beans.xml 注册一致性（7 新 bean id 与 @Inject 匹配）
- [ ] 合法豁免 PurQuotation.cancel / PurRfq.cancel（`:46` 单步状态翻转）保留未动
- [ ] 会计保护区域语义不变（核销/凭证/AR-AP 余额经既有测试行为等价）
- [ ] `mvn compile` 全域通过 + purchase/sales 域 `mvn test` 全绿
- [ ] compliance checker 基线不高于当前基线
- [ ] arm-index P1-MA3-062 purchase+sales 域须拆项标记 done
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: <draft — 待执行与独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（CLOSURE_VERIFY，新会话），待填>
- Evidence: <待填>

Follow-up:

- `ErpPurRequisitionProcessor.convertToOrder`（R6.8 backstop 单 D-mutation facade）由 R6.8 承接，本 plan 不处理。
