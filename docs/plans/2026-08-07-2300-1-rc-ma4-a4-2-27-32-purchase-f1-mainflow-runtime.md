# 2026-08-07-2300-1 rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime 采购主流程/请购运行时触发链与一致性确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.27 / A4.2.28 / A4.2.29 / A4.2.30 / A4.2.31 / A4.2.32
> Related: `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（A1.15 MA1 报告 §7 存疑点 1..6 + §6 P1-RC-017 新建 + P1-MA2-083 reuse 重开 + P2-RC-011/P2-RC-012 新建）、`docs/plans/2026-08-03-0145-1-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（A1.15 计划）
> Audit: required

## Current Baseline

A1.15（purchase-F1 主流程与请购）MA1 报告 §7 列出 6 个静态存疑点，对应 §6 多项 finding：P1-RC-017（UC-PUR-08 ⑫ 多供应商拆分阻断，新建，Q4 强制实现）、P1-MA2-083（承付恢复不对称，reuse 重开，Q4=(a) 下方案B Deferred 不成立）、P2-RC-011（businessType 命名漂移，登记不强制）、P2-RC-012（幂等实现漂移，登记不强制）。A1.15 §5 裁决：UC-PUR-01 主路径接受 on 6/8 + UC-PUR-08 接受 on 4/6。

这 6 项存疑点分两类：(1) 主路径行为正确性的运行时实测（§7-1/§7-2/§7-3/§7-6，HEAD 静态判定 = 实现 OK，运行时确认闭合）；(2) 缺陷确认（§7-4 P1-RC-017 多供应商拆分阻断、§7-5 P1-MA2-083 承付恢复不对称，HEAD 静态判定 = 缺陷，运行时确认闭合维持 P1）。

- **A4.2.27（§7-1 UC-PUR-01 ④ GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链）**：HEAD 静态判定 = 全链已实现（receive approve → `triggerIncomingMove` → `IErpInvStockMoveBiz.generateMove` → `InvPostingDispatcher` PURCHASE_INPUT 凭证 → `move.posted=true` → `receive.posted=true`）。运行时可经 `TestErpPurProcureToPayEnd#receiveApprove` E2E 确认凭证落地。
- **A4.2.28（§7-2 UC-PUR-01 ⑦ paidStatus 派生运行时一致性）**：HEAD 静态判定 = 实现 OK（`PaymentSettler.recomputeInvoicePaid` 累计 SUM(PaymentLine.amount)）。运行时可构造 2 付款单核销 1 发票场景确认。
- **A4.2.29（§7-3 UC-PUR-01 ⑧ 应付余额辅助账聚合运行时一致性）**：HEAD 静态判定 = 实现 OK（`ErpFinArApItem.openAmount` SUM == 发票金额 − 已核销金额，`TestErpPurProcureToPayEnd:244-272` 强断言）。运行时可构造复杂场景确认。
- **A4.2.30（§7-4 UC-PUR-08 ⑫ 多供应商拆分运行时阻断）**：HEAD 静态判定 = 阻断（`validateConsistentSupplier:171-186` 强制单一供应商，`TestErpPurRequisitionConvertToOrder#test_convertFailsWhenMixedSuppliers:122-124` 断言；P1-RC-017 已确认）。运行时确认多供应商请购行 convertToOrder 被 ERR_REQ_MIXED_OR_MISSING_SUPPLIER 拒绝。
- **A4.2.31（§7-5 P1-MA2-083 承付恢复运行时不对称）**：HEAD 静态判定 = 不对称（`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()；invoice approve→commitment release，reverseApprove→AP 红冲但 commitment 不归位）。运行时可构造 approve→reverseApprove 序列断言 commitment 余额不归位。
- **A4.2.32（§7-6 UC-PUR-08 ⑬ 取消后再转化运行时允许）**：HEAD 静态判定 = 允许（`test_convertIsIdempotentButReallowsAfterCancel:147-162` 断言；P2-RC-012 已确认）。运行时确认 cancel 全部衍生订单后再次转化被允许。

剩余差距：六项均为只读运行时确认。A4.2.27/A4.2.28/A4.2.29 追踪过账触发链与辅助账聚合（触及业财保护区域探针——只读确认不改过账逻辑）；A4.2.30（P1-RC-017）+ A4.2.31（P1-MA2-083）的修复义务归 MR1（P1-MA2-083 承付恢复调既有 commit() 入口属纯 BizModel/Processor 预授权；P1-RC-017 多供应商拆分属纯 BizModel/Processor 预授权）；A4.2.32（P2-RC-012）登记不强制。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.27-A4.2.32 六项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：主路径项（§7-1/2/3/6）确认行为正确闭合；缺陷项（§7-4 P1-RC-017 / §7-5 P1-MA2-083）维持 P1（reuse 重开 finding 不降级，Q4 强制实现）+ 记录运行时证据；若运行时发现活跃数据破坏或会计错误已活跃则触发 MR0。
- 完成后回写 roadmap A4.2.27-A4.2.32 `todo → done`，并按裁决更新 arm-index。

## Non-Goals

- 不实现多供应商拆分（P1-RC-017）/ 承付对称恢复（P1-MA2-083 退货+发票侧）/ businessType 重命名 / 幂等持久化字段——修复义务归 MR1 R1.0 展开器。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）。
- 不复跑 MA2 状态机审计（A2.8 已证实的 receive/invoice 状态机迁移与 reverseApprove 红冲闭环作为既有证据输入，不重新核实）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md` §5/§6/§7 + `docs/design/purchase/`（use-cases.md / state-machine.md / three-way-match.md / README.md）+ `docs/design/finance/posting.md` + `docs/design/finance/budget.md`（承付恢复）+ `docs/design/flow-overview.md §2.1`（P2P 链路）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 过账触发链追踪 + Processor/Provider 行为确认（grep census / 调用链追踪 / Test*.java 断言复核 / config 消费点普查），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.27-A4.2.32）

Status: completed
Targets: `docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.15 done ✓

- [x] **A4.2.27 GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链确认**：追踪 receive approve → `triggerIncomingMove` → `IErpInvStockMoveBiz.generateMove` → `InvPostingDispatcher` PURCHASE_INPUT 凭证 → `receive.posted=true` 全链；确认 `TestErpPurProcureToPayEnd#receiveApprove` E2E 断言凭证落地 + `applyPostingResult:221-227` receive.posted=move.posted 回写。**触及业财保护区域探针——只读确认，不改过账逻辑。**
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.28 paidStatus 派生运行时一致性确认**：确认 `PaymentSettler.recomputeInvoicePaid` 在多付款单跨单据核销同一发票时累计 SUM(PaymentLine.amount) 一致性 + 反向负金额行回退 paidStatus；确认无并发 lost-update（乐观锁/事务边界）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.29 应付余额辅助账聚合运行时一致性确认**：确认 `ErpFinArApItem.openAmount` 在多发票/多付款/部分核销/红冲场景下 SUM == 发票金额 − 已核销金额 恒等式（`TestErpPurProcureToPayEnd:244-272` 强断言复核）；确认 sumOpen 自然减计覆盖 credit memo 负金额。**触及业财保护区域探针——只读确认，不改辅助账逻辑。**
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.30 多供应商拆分运行时阻断确认（P1-RC-017）**：确认 `ErpPurRequisitionProcessor.validateConsistentSupplier:171-186` 对多供应商请购行 convertToOrder 抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER；确认 `TestErpPurRequisitionConvertToOrder#test_convertFailsWhenMixedSuppliers:122-124` 断言覆盖。裁决：维持 P1-RC-017 P1（Q4 强制实现，修复归 MR1，纯 BizModel/Processor 预授权）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.31 承付恢复运行时不对称确认（P1-MA2-083 reuse 重开）**：确认 `ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()；构造 approve→commitment release→reverseApprove→AP 红冲但 commitment 不归位序列断言不对称。裁决：维持 P1-MA2-083 P1（Q4=(a) 下方案B Deferred 不成立，修复归 MR1 调既有 commit() 入口，纯 BizModel/Processor 预授权不触 roadmap §横切关注点 #5 ask-first）。config-gated `erp-fin.budget-commitment-enabled` 默认 false 确认非默认活跃。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.32 取消后再转化运行时允许确认（P2-RC-012）**：确认 cancel 全部衍生订单后 `existsActiveByRequisition=false` 允许再次转化；确认 `test_convertIsIdempotentButReallowsAfterCancel:147-162` 断言覆盖。裁决：维持 P2-RC-012 P2（比 L1 更宽松，登记不强制）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：六项存疑点各出 §裁决（主路径闭合 / 维持 P1 reuse 重开 + 运行时证据记录 / 触发 MR0）+ §与既有 finding 衔接（P1-RC-017 / P1-MA2-083 / P2-RC-011 / P2-RC-012 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.27/A4.2.29 触及业财保护区域探针——只读确认不改过账/辅助账逻辑。

- [x] 验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`，含六项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：主路径闭合 / 维持 P1（Q4 强制实现，reuse 重开 finding 不降级）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.27-32 done）、`docs/audits/arm-index.md`（reuse 重开注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-MA2-083 reuse 重开维持 P1（运行时证据确认方案 B Deferred 关闭在 Q4=(a) 下不成立，修复归 MR1 调既有 commit() 入口，不触 ask-first）；P1-RC-017 维持 P1（运行时确认阻断存在，修复归 MR1 纯 BizModel/Processor 预授权）；P2-RC-011/P2-RC-012 维持 P2（登记不强制）。无新 finding 新建（全部 reuse/维持）。
- [x] `Add` roadmap A4.2.27-A4.2.32 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 六项状态已更新为 done 且与报告裁决一致
- [x] arm-index reuse 重开注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is (ses_027641750ffePBU5QTmruQReQ6) — no blocking issues. Deps satisfied (A4.2 expander done); scope/rule-14 correct (A1.15 §7 存疑点 1-6 = A4.2.27-32 1:1); citation accuracy verified against source report §5/§6/§7 (P1-RC-017/P1-MA2-083/P2-RC-011/P2-RC-012 file:line + verdicts match); protected-area READ-ONLY probes correct + P1-MA2-083 fix preauthorization (Processor-logic vs PostingProcessor-core distinction) source-supported; anti-slack clean; pattern conforms to reference. Non-blocking: bare `§5` standardized to `roadmap §横切关注点 #5` for consistency. Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（六项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-017 / P1-MA2-083 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-RC-017（多供应商拆分按 supplier 生成多 ErpPurOrder）+ P1-MA2-083（承付对称恢复，发票侧 + 退货侧均调既有 commit() 入口）修复归 MR1 R1.0 展开器，Q4 裁决 P1 强制实现。两项均纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触 roadmap §横切关注点 #5 ask-first。本审计维持 P1 不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: A1.15 §7 六项静态存疑点运行时行为确认全数收口——四项主路径（A4.2.27/28/29/32）「闭合」+ 两项缺陷（A4.2.30 P1-RC-017 / A4.2.31 P1-MA2-083 reuse 重开）「维持 P1 + 运行时证据记录」，A1.15 静态判定无一翻转，0 新 finding / 不触发 MR0 / 不归 MR1。本审计为只读运行时确认（零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更），A4.2.27/A4.2.29 业财保护区域探针仅 READ-ONLY 未修改过账/辅助账逻辑。两项 P1 修复义务明确归 MR1 R1.0 展开器，已登记于 Deferred But Adjudicated 节。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-04-224309-mission-driver，新会话，不重用执行者上下文）。执行者未自我审计，结束审计门控由本独立子代理勾选。
- Evidence:
  - 验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（37489 字节，`> Audit Status: closed`，9 段齐全，六项 §2-1..§2-6 各含 L3 file:line + L4 测试断言 + L5 行为 + 裁决分支）
  - 路径核验（独立子代理 grep/read 复核 live codebase）：(1) `ErpPurRequisitionProcessor.validateConsistentSupplier:171-186` 抛 `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`（ErpPurErrors.java:95 定义），对应 L4 `TestErpPurRequisitionConvertToOrder:123,137` 双断言——A4.2.30 维持 P1-RC-017 证据落地；(2) `ErpPurInvoiceReverseApproveProcessor` 存在，`commit()` 调用方 census 全域仅 `ErpPurOrderProcessor:208`（commit-on-order-approve），reverse/cancel Processor 全集零 commit()——A4.2.31 维持 P1-MA2-083 reuse 重开 证据落地；(3) 报告 §2-1/2-2/2-3 主路径触发链 + SUM 聚合 + openAmount 恒等式与既有 A2.1 P2P e2e / A1.1 业财过账引擎证据一致
  - roadmap A4.2.27-A4.2.32 全数 `todo → done ✅`（requirement-compliance-roadmap.md:180-185 六行与报告裁决一致，无降级）
  - `docs/logs/2026/08-07.md` 已追加完成条目（:3-:22，含工作项/类型/产物/裁决/下一步五段）
  - `docs/audits/arm-index.md:353` 追加「A4.2.27-32 purchase-F1 运行时确认」RC 交叉引用注记（P1-RC-017 :156 / P1-MA2-083 :544 / P2-RC-011 :157 / P2-RC-012 :158 维持既有分级不撤销，无新 finding）
  - 文本一致性：Plan Status `completed` ↔ Phase 1/2 `completed` ↔ 全 Exit Criteria `[x]` ↔ Closure Gates 全 `[x]` ↔ 报告 Audit Status `closed` ↔ 日志条目「全数收口」一致
  - 不可降级自检：两项 P1（P1-RC-017 / P1-MA2-083）为已确认缺陷，未降级为 follow-up，明确登记于 Deferred But Adjudicated 节并命名后继触发（MR1 R1.0 展开为 RC-R1.n 时承接）

Follow-up:

- 无非阻塞跟进项目（P1 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
