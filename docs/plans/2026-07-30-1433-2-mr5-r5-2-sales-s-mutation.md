# 2026-07-30-1433-2-mr5-r5-2-sales-s-mutation sales 域 S-mutation 逻辑下沉

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.2
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`（pilot，本 plan 沿用其迁移配方）、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`
> Audit: required

## Current Baseline

- sales 域 36 个 per-mutation Processor 文件已存在（6 实体 × 6 S-mutation），当前为空心委托（同 R5.1）。
- 当前运行时调用链：xbiz `<source>` → per-mutation Processor（空心）→ facade Processor（真实逻辑）。
- **文件计数（实测）**：30 个有 xbiz `<source>`（每实体 5 个 source-backed mutation）+ **6 个 `*CancelProcessor` 无 xbiz `<source>`**——`cancel` 经 BizModel Java `@BizMutation cancel()` → facade.cancel()（同 R5.1 purchase 模式）。
- 6 个 facade Processor：`ErpSalOrderProcessor`、`ErpSalQuotationProcessor`、`ErpSalDeliveryProcessor`、`ErpSalReceiptProcessor`、`ErpSalReturnProcessor`、`ErpSalInvoiceProcessor`。
- xbiz `<source>` 分类（per-mutation-processor-split-plan.md §source 分类总表，须逐 xbiz 复核——已发现该表 Receipt 行有误）：source-backed 30 个中 24 个为 delegation，6 个为 inline-script：
  - inline-script 为**全部 6 实体的 `withdrawApproval`**（含 ErpSalReceipt——实测 `ErpSalReceipt.xbiz:59-65` 为 `NopScriptError` 内联脚本，split plan 误标为 delegation）。
  - delegation = 6 实体 × 4（submit/approve/reject/reverseApprove）。
- sales 与 purchase 结构对称（同为 6 实体贸易单据，同样的审批六动作 + commitment/intercompany hooks 范式）。R5.1 pilot 已建立迁移配方。
- 剩余差距：同 R5.1——per-mutation 空心违反 `processor-extension-pattern.md:42`。

## Goals

- sales 域全部 36 个 per-mutation Processor 各自自包含（含 30 source-backed + 6 no-source cancel）。
- 6 个 inline-script `withdrawApproval`（全 6 实体，含 Receipt）提取为 Java hook（`NopScriptError` → `NopException` 语义等价）。
- 30 个 source-backed per-mutation 经运行时既有测试验证行为等价；6 个 no-source cancel 经静态 parity 校验确认保真（运行时验证移交 R5.8，同 R5.1 模式）。

## Non-Goals

- D-mutation（`confirmCustomerAccepted`、`convertToOrder`、`settle`、`reverseSettlement`）保留在 facade——MR5 范围外。
- BizModel 配线 / beans.xml / xbiz 清理——属 R5.8。
- `ErpSalContract` 等无 S-mutation 的实体——不在 R5.2 范围。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/sales/state-machine.md`、`docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`
- Skill Selection Basis: 同 R5.1（后端 Processor 重构），匹配 `nop-backend-dev` + `nop-testing`。沿用 R5.1 裁决的共享 hook 策略。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - delegation 类 + no-source cancel per-mutation 填充（沿用 R5.1 配方）

Status: planned
Targets: `module-sales/erp-sal-service/.../processor/ErpSal*{Approve,Reject,Cancel,SubmitForApproval,ReverseApprove,WithdrawApproval}Processor.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: R5.1 共享 hook 策略 Decision 已落定（本 plan 复用，若 sales 有域差异则在此补 Decision）

- [ ] Add: 24 个 delegation 类 source-backed per-mutation 填充——删除空心回委托，改为抽象基类骨架 + hook override 承载 facade step 逻辑。共享 hook 策略沿用 R5.1 裁决（候选 A：@Inject facade；B：提取共享 helper；C：内联——R5.1 Phase 1 裁决后本 plan 直接套用）。
  - 涉及：6 实体 × 4 delegation source-backed（submit/approve/reject/reverseApprove）；withdrawApproval 全为 inline（归 Phase 2）。
  - Skill: `nop-backend-dev`
- [ ] Add: 6 个 no-source `*CancelProcessor` 填充——无 xbiz `<source>`，逻辑源为 facade 的 `cancel()` 方法，迁移为 `AbstractCancelProcessor` hook override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链，运行时验证移交 R5.8。
  - Skill: `nop-backend-dev`
- [ ] Add: 若 sales facade 的 commitment/intercompany hook 调用签名与 purchase 不同（如 docType 常量 `INTERCOMPANY_DOC_TYPE_SALES_ORDER`），在 hook override 中使用 sales 域常量，验证 config-gated 语义等价。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 本阶段交付 delegation + no-source cancel 类 per-mutation 自包含化（共 30 文件：24 delegation + 6 cancel）。

- [ ] sales 域 delegation + cancel 类 per-mutation 本地编译通过（`mvn compile -pl module-sales/erp-sal-service -am -DskipTests`）

### Phase 2 - inline-script withdrawApproval 提取

Status: planned
Targets: `module-sales/erp-sal-service/.../processor/ErpSal*WithdrawApprovalProcessor.java`（Order/Quotation/Delivery/**Receipt**/Return/Invoice——全 6 实体）、`module-sales/erp-sal-service/.../resources/_vfs/erp/sal/model/ErpSal*/ErpSal*.xbiz`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 6 个 `withdrawApproval` inline-script 提取为 Java hook override（`validateTransitionForWithdraw`/`doWithdraw` + 错误码参数），`NopScriptError` → `NopException` 语义等价（含 ErpSalReceipt——实测为 inline，split plan 误标 delegation）。
  - Skill: `nop-backend-dev`
- [ ] Proof: 错误码语义等价验证——既有测试 withdrawApproval 负向状态守卫断言不变；快照因类名变化失配则重录并注明。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] inline-script 提取的 per-mutation 本地编译通过
- [ ] 错误码语义等价验证通过

### Phase 3 - sales 域行为等价回归

Status: planned
Targets: `module-sales/erp-sal-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [ ] Proof: sales 域既有测试全绿；快照漂移仅限类名/堆栈，重录为新基线。
  - Skill: `nop-testing`
- [ ] Proof: 补充 withdrawApproval 负向状态守卫断言，确认错误码 + `.param()` 参数等价（覆盖 inline-script 提取路径）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] sales 域 `mvn test -pl module-sales/erp-sal-service -am` 全绿（source-backed 路径行为等价 + no-source cancel 无回归）
- [ ] withdrawApproval 负向状态守卫错误码 + 参数等价断言覆盖

## Draft Review Record

- Independent draft review iteration 1: needs revision（task ses_04e40c67affel5peBuwSr4q9ui）—1 blocking：25 delegation + 5 inline = 30≠36，6 个 no-source CancelProcessor 漏列。已修正 Phase 1 纳入 6 cancel。
- Independent draft review iteration 2: needs revision（task ses_04e35ac08ffewND72B7ThZmpu8）—1 新 blocking：ErpSalReceipt withdrawApproval 实测为 inline-script（`ErpSalReceipt.xbiz:59-65` NopScriptError），split plan 误标 delegation；delegation 25→24、inline 5→6（含 Receipt）。已修正基线/Goals/Phase 1（24+6 cancel=30）/Phase 2（6 含 Receipt）/Closure Gates。
- Independent draft review iteration 3: accept（task ses_04e319148ffe3UrRoz25fdt43K）—Round-2 B2（Receipt inline）已解决，算术全 6 处一致（24 delegation + 6 inline 含 Receipt + 6 cancel = 36），Receipt 显式列入 Phase 2 targets，无新 blocking。非阻塞：Phase 1 targets glob 含 WithdrawApproval（work item 正确排除，实现者按 work item 读）。可转 active。

## Closure Gates

- [ ] sales 域 36 个 per-mutation Processor 自包含（含 30 source-backed + 6 no-source cancel）
- [ ] 6 个 inline-script withdrawApproval（全 6 实体含 Receipt）提取为 Java hook，错误码语义等价验证通过
- [ ] 30 source-backed per-mutation 经 sales 域 `mvn test` 行为等价验证；6 no-source cancel 经静态 parity 校验确认保真（运行时验证移交 R5.8）
- [ ] sales 域 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [ ] 快照漂移仅限类名/堆栈变化，已重录并注明
- [ ] R5.1 共享 hook 策略在 sales 适用性已确认（域差异已补 Decision）
- [ ] 无范围内项目降级为 deferred/follow-up（no-source cancel 运行时验证是显式 successor 所有权转移）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### no-source cancel per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: 6 个 `*CancelProcessor` 无 xbiz `<source>`，经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链。R5.2 已完成填充 + 静态 parity 校验。运行时激活归 R5.8。
- Successor Required: `yes`（R5.8）

### D-mutation 保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 明示 D-mutation 范围外。
- Successor Required: `no`

### BizModel 配线 + beans.xml + xbiz 清理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8。
- Successor Required: `yes`

## Closure

Status Note: _（待填充）_

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填充）_

Follow-up:

- 若 sales 域 commitment/intercompany hook 签名/时序与 purchase 不一致（如 docType 常量、config 键名差异），回注 `per-mutation-processor-split-plan.md` 配方。
