# 2026-08-07-2300-3 rc-ma4-a4-2-40-46-purchase-f3-returns-runtime 采购退货/业财过账/红冲闭环运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.40 / A4.2.41 / A4.2.42 / A4.2.43 / A4.2.44 / A4.2.45 / A4.2.46
> Related: `docs/audits/2026-08-03-0300-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`（A1.17 MA1 报告 §7 存疑点 1..7 + §6 P2-RC-015 新建 + P1-MA2-083 reuse 重开[退货侧] + P2-MA2-006 resolved 复核）、`docs/audits/2026-08-03-0630-...-a1-20-...md`（A1.20 §7 SP-3 跨域期间 CLOSED guard，与 A4.2.43 合并）、`docs/plans/2026-08-03-0300-1-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`（A1.17 计划）
> Audit: required

## Current Baseline

A1.17（purchase-F3 退货与业财）MA1 报告 §7 列出 7 个静态存疑点，对应 §6 finding：P2-RC-015（UC-PUR-04 ④ 原入库凭证 isReversed 标记缺失，新建，与 P2-MA2-006 同根因不同维度，successor watch-only）、P1-MA2-083（承付恢复不对称 reuse 重开[退货侧]，Q4=(a) 下方案B Deferred 不成立）、P2-MA2-006（credit-memo-via-return resolved 复核）。A1.17 §5 裁决：UC-PUR-04 接受 on ①②③⑤ + UC-PUR-07 接受 on ①②③④⑤。

A4.2.43 为合并行（A1.17 §7-4 + A1.20 SP-3）：跨域期间 CLOSED guard 间接拦截，同根因（finance `resolveOpenPeriod`）同控制点，覆盖 purchase receive/invoice/return + sales return 过账路径。

这 7 项中多数已由既有 L4 测试强断言闭合（§7-2 credit memo / §7-3 GR/IR 凭证行 / §7-5 反审核红冲 / §7-7 多币种，报告自标"已闭合"），运行时确认即闭合确认；未闭合项为 §7-1 isReversed 标记缺失（P2-RC-015）、§7-4 跨域期间 CLOSED guard（合并 A1.20）、§7-6 承付恢复退货侧不对称（P1-MA2-083 reuse）。

- **A4.2.40（§7-1 UC-PUR-04 ④ isReversed 标记运行时确认）**：HEAD 静态判定 = `PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向过账）不调 `executor.reverse`，原入库 PURCHASE_INPUT 凭证保留 isReversed=false（仅以独立 PURCHASE_RETURN 反向凭证实现 GL 净零）。运行时确认 isReversed=false。裁决：维持 P2-RC-015 P2（GL 净零功能等价，会计过账正确性不破坏，documented simplification 满足 §4(i)，successor watch-only 不强制修复）。
- **A4.2.41（§7-2 UC-PUR-04 ⑤ credit-memo-via-return AP 余额回减复核）**：HEAD 静态判定 = 已闭合（`ErpFinArApItemGenerator.resolveProfile:157-160` 负 openAmount + sumOpen 自然减计，`TestErpPurReturnRefundEndToEnd:188-189` sumOpen=-20 强断言）。运行时确认闭合，维持 P2-MA2-006 resolved。
- **A4.2.42（§7-3 UC-PUR-07 ② GR/IR 暂估应付凭证行复核）**：HEAD 静态判定 = 已闭合（`InvAcctDocProvider:22-30` 借 1401 存货/贷 2202 暂估；`PurAcctDocProvider` 三行；`TestErpPurReceiveStockMove:112` + `TestErpPurInvoicePosting:70-100` 强断言）。运行时确认闭合。
- **A4.2.43（§7-4 + A1.20 SP-3 跨域期间 CLOSED guard 运行时拒绝过账）**：HEAD 静态判定 = `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局生效（period.status != OPEN 抛 ERR_PERIOD_CLOSED，对所有 businessType 全局生效）。purchase 侧无独立测试，finance 域测试覆盖。运行时确认采购侧 receive/invoice/return + sales return 过账路径经 finance 引擎间接拦截（同根因同控制点，合并确认）。裁决：主路径行为正确（间接守卫有效），闭合。
- **A4.2.44（§7-5 UC-PUR-07 ④ 反审核删凭证[红字冲销]复核）**：HEAD 静态判定 = 已闭合（`ErpPurReturnProcessor.ensureReversed:245-265` + `PurReversalListener` 四实体回写；`TestErpPurReturnPosting:122-148` + `TestErpPurFinanceReversalWriteback` 强断言）。运行时确认闭合。
- **A4.2.45（§7-6 UC-PUR-04 承付恢复运行时对称性[退货侧，reuse P1-MA2-083]）**：HEAD 静态判定 = 不对称（`runCommitmentReleaseOnReturnHook` release 已实现 + reverseApprove/cancel 无 commit()）。运行时构造 config-gated 启用 + return approve（释放）→ return reverseApprove（无 commit() 恢复）→ 断言 commitment 余额不归位。裁决：维持 P1-MA2-083 P1（reuse 重开，退货侧修复行须扩展覆盖 Return Processor，调既有 commit() 入口纯 BizModel/Processor 预授权不触 ask-first）。
- **A4.2.46（§7-7 UC-PUR-07 ③ 多币种行级金额复核）**：HEAD 静态判定 = 已闭合（`PurInvoicePostingDispatcher.buildEvent:78` exchangeRate + `TestErpPurMultiCurrencyPosting:70-132` source×rate==functional 强断言）。运行时确认闭合。

剩余差距：七项均为只读运行时确认。A4.2.45（P1-MA2-083 退货侧）修复归 MR1（调既有 commit() 入口纯 BizModel/Processor 预授权）；A4.2.40（P2-RC-015）successor watch-only 不强制修复。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.40-A4.2.46 七项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：已闭合项（§7-2/3/5/7）确认行为正确闭合；未闭合项（§7-1 P2-RC-015 / §7-4 跨域 guard / §7-6 P1-MA2-083 退货侧）维持分级 + 记录运行时证据；若运行时发现活跃会计错误则触发 MR0。
- 完成后回写 roadmap A4.2.40-A4.2.46 `todo → done`（含 A4.2.43 合并行覆盖 A1.20 SP-3），并按裁决更新 arm-index。

## Non-Goals

- 不实现 isReversed 标记（P2-RC-015，successor watch-only）/ 承付对称恢复退货侧（P1-MA2-083）/ formal 红字发票——修复义务归 MR1 R1.0 展开器。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）。
- 不复跑 MA2 状态机审计（A2.8 已证实的 reverseApprove 红冲闭环 + 跨域 Facade 作为既有证据输入）；不重审 P1-RC-018 PPV 维度（归 A1.16/A4.2.34）；不重审 GRNI 冲回（归 A2.1 finance 会计保护区）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0300-rc-ma1-a1-17-purchase-f3-returns-business-finance.md` §5/§6/§7 + A1.20 §7 SP-3 + `docs/design/purchase/`（use-cases.md / returns.md / state-machine.md）+ `docs/design/finance/posting.md`（红冲/期间守卫/业财回链）+ `docs/design/finance/budget.md`（承付恢复）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 红冲闭环追踪 + 凭证标记确认 + 期间守卫间接拦截确认（grep census / tryPost vs reverse 调用链追踪 / resolveOpenPeriod 全局生效复核 / config 消费点普查），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.40-A4.2.46）

Status: completed
Targets: `docs/audits/2026-08-07-2300-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.17 done ✓

- [x] **A4.2.40 isReversed 标记运行时缺失确认（P2-RC-015）**：确认 `PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向过账）不调 `executor.reverse`；确认 `ErpFinPostingProcessor.markOriginalVoucherReversed:252+933-947` 仅在 reverse() 路径触发——原入库 PURCHASE_INPUT 凭证保留 isReversed=false；确认 GL 净零经独立 PURCHASE_RETURN 反向凭证实现（功能等价）。**触及业财保护区域探针——只读确认，不改过账逻辑。** 裁决：维持 P2-RC-015 P2（GL 净零功能等价，documented simplification 满足 §4(i)，successor watch-only 不强制）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.41 credit-memo-via-return AP 余额回减复核（P2-MA2-006 resolved）**：确认 `ErpFinArApItemGenerator.resolveProfile:157-160` DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount + sumOpen 自然减计；确认 `TestErpPurReturnRefundEndToEnd:188-189` sumOpen=-20 强断言覆盖。裁决：闭合，维持 P2-MA2-006 resolved。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.42 GR/IR 暂估应付凭证行复核**：确认 `InvAcctDocProvider:22-30` 借 1401 存货/贷 2202 暂估应付 + `PurAcctDocProvider` 三行（1403/2221/2202）；确认 `TestErpPurReceiveStockMove:112` + `TestErpPurInvoicePosting:70-100` 强断言覆盖 UC-PUR-07 ①② 凭证行结构。裁决：闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.43 跨域期间 CLOSED guard 运行时拒绝过账确认（A1.17 §7-4 + A1.20 SP-3 合并）**：确认 `ErpFinPostingProcessor.resolveOpenPeriod:524-527` period.status != OPEN 抛 ERR_PERIOD_CLOSED 对所有 businessType 全局生效；确认 purchase receive/invoice/return + sales return 过账路径经 finance 引擎间接拦截（采购侧无独立测试，finance 域测试覆盖；同根因同控制点合并确认）。裁决：主路径行为正确（间接守卫有效），闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.44 反审核删凭证[红字冲销]复核**：确认 `ErpPurReturnProcessor.ensureReversed:245-265` 调 `postingDispatcher.reverse()` 经 IErpFinVoucherBiz Facade 红冲 + posted=false + 辅助账 cancelOnReverse；确认 `PurReversalListener.rollbackInvoice/Payment/Return/Receive:70-126` 四实体全部 posted=false + APPROVED→REJECTED；确认 `TestErpPurReturnPosting:122-148` + `TestErpPurFinanceReversalWriteback` 强断言覆盖。裁决：闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.45 承付恢复运行时对称性[退货侧]确认（P1-MA2-083 reuse 重开）**：确认 `runCommitmentReleaseOnReturnHook` release 已实现 + return reverseApprove/cancel 无 commit()（不对称）；构造 config-gated 启用（`erp-fin.budget-commitment-enabled=true` + `erp-fin.commitment-release-on-return=true`）+ return approve（释放）→ return reverseApprove（无 commit() 恢复）断言 commitment 余额不归位。裁决：维持 P1-MA2-083 P1（reuse 重开，退货侧修复行须扩展覆盖 Return Processor，调既有 commit() 入口纯 BizModel/Processor 预授权不触 ask-first）。config-gated 默认 false 确认非默认活跃。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.46 多币种行级金额复核**：确认 `PurInvoicePostingDispatcher.buildEvent:78` exchangeRate 兜底 + `ErpFinPostingProcessor.prepareContext:537` + VoucherFact 行级 amountSource/amountFunctional 分离；确认 `TestErpPurMultiCurrencyPosting:70-132` source×rate==functional（100×7=700/13×7=91/113×7=791）强断言覆盖。裁决：闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：七项存疑点各出 §裁决（闭合 / 维持 P2 successor watch-only / 维持 P1 reuse 重开 / 触发 MR0）+ §与既有 finding 衔接（P2-RC-015 / P1-MA2-083 / P2-MA2-006 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）+ §业财保护区域探针纪律声明 + §A4.2.43 合并声明（A1.17 §7-4 + A1.20 SP-3 同根因同控制点）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.40 触及业财保护区域探针——只读确认 isReversed 标记，不改过账逻辑。

- [x] 验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`，含七项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：闭合 / 维持分级（P1 reuse 重开 / P2 successor watch-only）+ 运行时证据记录，或升级触发 MR0
- [x] A4.2.43 合并声明明确覆盖 A1.20 SP-3（同根因同控制点，roadmap 两处同步 done）

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.40-46 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-MA2-083 reuse 重开维持 P1（退货侧不对称确认，修复归 MR1 调既有 commit() 入口，不触 ask-first）；P2-RC-015 维持 P2 successor watch-only（GL 净零功能等价，不强制修复）；P2-MA2-006 维持 resolved（复核闭合）。无新 finding 新建（全部 reuse/维持）。
- [x] `Add` roadmap A4.2.40-A4.2.46 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 七项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is (ses_02763e108ffe08kiZIQeToBelb) — no blocking issues. Deps satisfied (A4.2 expander done); scope/rule-14 correct (A1.17 §7 存疑点 1-7 = A4.2.40-46); A4.2.43 merged row (A1.17 §7-4 + A1.20 SP-3) handled explicitly in 3 locations + Exit Criteria requires both source points + roadmap 两处同步 done; citation accuracy verified (P2-RC-015/P1-MA2-083 退货侧 reuse/P2-MA2-006 + file:line match); protected-area READ-ONLY correct + P1-MA2-083 return-side fix preauthorization (commit() entry, distinct from invoice-side ask-first cases) source-supported; anti-slack clean; two Deferred-But-Adjudicated subsections acceptable (different classifications); pattern conforms. Non-blocking: bare `§5` standardized to `roadmap §横切关注点 #5`. Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（七项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA2-083 修复实现（退货侧扩展）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-MA2-083 承付恢复不对称（发票侧已归 A4.2.31 确认 + 本切片确认退货侧同型不对称）修复归 MR1 R1.0 展开器，Q4 裁决 P1 强制实现。退货侧修复 = Return Processor reverseApprove/cancel 路径调既有 commit() 入口，纯 BizModel/Processor 代码逻辑，按 roadmap 预授权类目可自动执行，不触 roadmap §横切关注点 #5 ask-first。本审计维持 P1 不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接，与 A4.2.31 发票侧修复行合并）

### P2-RC-015 isReversed 标记

- Classification: `watch-only residual`
- Why Not Blocking Closure: GL 净零经独立 PURCHASE_RETURN 反向凭证功能等价实现，会计过账正确性不破坏；documented simplification 满足 §4(i)（独立 plan-audit 通过记录）。successor watch-only，不强制修复。
- Successor Required: no（维持 P2 watch-only，除非未来审计要求字面 isReversed 标记）

## Closure

Status Note: 只读审计计划（零生产代码/ORM/api.xml/view.xml/config/真相源变更），七项存疑点（A4.2.40-A4.2.46）运行时确认全部落地。五项主路径闭合（A4.2.41/42/43/44/46）+ 一项维持 P2 successor watch-only（A4.2.40 → P2-RC-015）+ 一项维持 P1 reuse 重开（A4.2.45 → P1-MA2-083 退货侧扩展）。A1.17 §7 静态判定无一翻转，零新 finding，不触发 MR0，修复义务明确归 MR1 R1.0 展开器（P1-MA2-083 调既有 commit() 入口纯 Processor 预授权）或 watch-only（P2-RC-015）。所有 Phase Status `completed` + Exit Criteria 全 `[x]` + Plan Status `completed` 文本一致。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（独立子代理，新会话，不重用执行者上下文）
- Evidence:
  - 验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`（249 行，`> Audit Status: closed`，9 段齐全，7 项裁决各附 L3 file:line + L4 强断言 + §裁决分支）
  - live code 实测复核（反空洞自检）：`PurReturnPostingDispatcher.tryPost:44-47` 调 `executor.postEvent` 正向过账，`reverse():64-66` 为独立方法调 `executor.reverse`（确认 tryPost 不调 reverse→原 PURCHASE_INPUT 凭证 isReversed=false）；`ErpFinPostingProcessor.resolveOpenPeriod:508` + `ERR_PERIOD_CLOSED` 抛点 :525（全局生效）；`markOriginalVoucherReversed` 调用点 :252 位于 reverse() 内部 + 实现 :933（确认仅 reverse 路径触发）；`ErpPurReturnProcessor.ensureReversed:245` 调 `postingDispatcher.reverse:247`（红冲闭环）
  - `git status` 确认零生产代码变更（仅 `docs/` 下 4 文件修改 + 1 新建报告：arm-index / roadmap / 08-07 log / 本 plan + 新报告），无 .java/.xml/.yaml 触及，确认只读审计承诺
  - roadmap `requirement-compliance-roadmap.md:193-199` A4.2.40-A4.2.46 全 `done ✅` 且各行证据摘要与报告裁决一致（含 A4.2.43 A1.20 SP-3 合并声明两处同步 done）
  - `docs/audits/arm-index.md` 已追加 RC A4.2.40-46 运行时确认注记（P2-RC-015 :163 / P2-MA2-006 :713 / P1-MA2-083 :546 维持既有分级不撤销，无新 finding）
  - `docs/logs/2026/08-07.md` 已追加完成条目（七项存疑点逐项裁决摘要 + 裁决汇总 + 下一步）
  - 文本一致性：Plan Status `completed` ↔ Phase 1/2 Status `completed` ↔ 全 Exit Criteria `[x]` ↔ Closure Gates 全 `[x]` ↔ log 条目一致
  - 过程纪律：独立草案审查记录于 `## Draft Review Record`（acceptable-as-is，ses_02763e108ffe08kiZIQeToBelb）；本结束审计由独立子代理新会话执行，执行者未自我审计

Follow-up:

- 无非阻塞跟进项目（P1 修复义务已明确归 MR1 R1.0 展开器，P2-RC-015 watch-only，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
