# 2026-08-07-2300-2 rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime 三单匹配/容差/价格差异过账运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.33 / A4.2.34 / A4.2.35 / A4.2.36 / A4.2.37 / A4.2.38 / A4.2.39
> Related: `docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`（A1.16 MA1 报告 §7 存疑点 1..7 + §6 P1-RC-018 新建[会计过账] + P1-RC-019 新建 + P2-RC-013/P2-RC-014 新建）、`docs/plans/2026-08-03-0200-1-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`（A1.16 计划）
> Audit: required

## Current Baseline

A1.16（purchase-F2 三单匹配与差异）MA1 报告 §7 列出 7 个静态存疑点，对应 §6 多项 finding：P1-RC-018（UC-PUR-05 ⑪⑫ 价格差异处理不完整含让步接收 PPV 过账行缺失，新建，**会计过账正确性类 Q4 无例外**）、P1-RC-019（UC-PUR-02 ② 超收容差校验缺失，新建）、P2-RC-013（receivedQuantity 未写入，新建，登记不强制）、P2-RC-014（短收差异处理缺失，新建，登记不强制）。A1.16 §5 裁决：UC-PUR-02 接受 on ①③④ + UC-PUR-03 接受 on ③④ + UC-PUR-05 接受 on ①② + UC-PUR-06 接受 on ①②④⑤。

这 7 项存疑点分两类：(1) 缺陷确认（§7-2/§7-3 P1-RC-018 价格差异过账+策略、§7-4 P1-RC-019 超收容差、§7-5 P2-RC-014 短收差异、§7-7 P2-RC-013 receivedQuantity，HEAD 静态判定 = 缺陷，运行时确认闭合维持分级）；(2) 主路径行为正确性确认（§7-1 两次入库独立过账、§7-6 关闭释放预留 config-gated，HEAD 静态判定 = 实现 OK，运行时确认闭合）。

- **A4.2.33（§7-1 UC-PUR-03 ⑦ 两次入库独立过账凭证数==2）**：HEAD 静态判定 = per-mutation approve 架构隐含成立（每次 `ErpPurReceiveApproveProcessor.approve` 独立 triggerIncomingMove→凭证），但无测试构造"100→60→40→断言凭证数==2"。运行时确认架构隐含成立。
- **A4.2.34（§7-2 UC-PUR-05 ⑫ 让步接收价格差异过账运行时生成）**：HEAD 静态判定 = 完全缺失（`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行 1403/2221/2202 无 PPV 行）。运行时确认 PPV 过账行不存在。**触及业财保护区域探针——只读确认不改过账逻辑。**
- **A4.2.35（§7-3 UC-PUR-05 ⑪ 三处理策略运行时分支可达性）**：HEAD 静态判定 = 仅"拒绝"（strict mode）可达，"审批后接收"+"接收并过账差异"未实现。运行时确认无隐藏接线（xbiz/审批流 config 覆盖）。
- **A4.2.36（§7-4 UC-PUR-02 ② 超收容差运行时门控）**：HEAD 静态判定 = 完全缺失（receive approve 无 qty-vs-order 校验）。运行时确认"订单10+入库20（超收100%）"approve 无门控通过。
- **A4.2.37（§7-5 UC-PUR-06 ⑮ 短收超容差运行时差异处理）**：HEAD 静态判定 = 完全缺失（无"差异处理"触发）。运行时确认"订单100+入库50（短收50>容差）"无差异处理触发。
- **A4.2.38（§7-6 UC-PUR-06 ⑰ 关闭释放预留运行时 config-gated 行为）**：HEAD 静态判定 = 已实现（config-gated `erp-fin.budget-commitment-enabled` 默认 false）。运行时确认 config-gated 语义（与 A1.2/A1.15 已接受 config-gated 范式一致）。
- **A4.2.39（§7-7 UC-PUR-03 ⑤⑥ receivedQuantity 运行时值）**：HEAD 静态判定 = 列始终 0（零 writer，`rollupOrderReceiveStatus` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity）。运行时确认两次入库后查询值得 0。

剩余差距：七项均为只读运行时确认。A4.2.34（P1-RC-018）触及会计过账保护区域探针——只读确认 PPV 行缺失，修复义务归 MR1 且触及 PurAcctDocProvider/VoucherFact 核心路径须 ask-first；A4.2.35（P1-RC-018 策略）+ A4.2.36（P1-RC-019）+ A4.2.37（P2-RC-014）+ A4.2.39（P2-RC-013）修复归 MR1（纯 BizModel/Processor 预授权）。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.33-A4.2.39 七项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：缺陷项（§7-2/3 P1-RC-018 / §7-4 P1-RC-019 / §7-5 P2-RC-014 / §7-7 P2-RC-013）维持分级（P1 reuse 重开不降级 Q4 强制实现 / P2 维持）+ 记录运行时证据；主路径项（§7-1/§7-6）确认行为正确闭合；若运行时发现会计错误已活跃（PPV 缺失致 GL 不平衡）则触发 MR0。
- 完成后回写 roadmap A4.2.33-A4.2.39 `todo → done`，并按裁决更新 arm-index。

## Non-Goals

- 不实现 PPV 过账行（P1-RC-018）/ 三处理策略（P1-RC-018）/ 超收容差校验（P1-RC-019）/ 短收差异处理（P2-RC-014）/ receivedQuantity 写入（P2-RC-013）——修复义务归 MR1 R1.0 展开器；P1-RC-018 触及 PurAcctDocProvider/VoucherFact 核心路径须 ask-first + 独立 plan-audit。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）。
- 不复跑 MA2 状态机审计（A2.8 已证实的 receive/invoice 状态机迁移作为既有证据输入，不重新核实）；不重审 P1-RC-018 PPV 维度（A1.16 已审，本计划仅运行时确认缺失）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md` §5/§6/§7 + `docs/design/purchase/`（use-cases.md / three-way-match.md / state-machine.md）+ `docs/design/finance/posting.md`（AP_INVOICE 凭证范式）+ `docs/design/finance/budget.md`（承付释放 config）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 过账凭证行追踪 + Processor 守卫确认（grep census / createFacts 行级结构追踪 / validateBusinessRulesForApprove 守卫复核 / config 消费点普查），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.33-A4.2.39）

Status: completed
Targets: `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.16 done ✓

- [x] **A4.2.33 两次入库独立过账凭证数==2 确认**：确认 per-mutation approve 架构——每次 `ErpPurReceiveApproveProcessor.approve` 独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证；确认虽无"100→60→40→断言凭证数==2"专属测试，但 per-mutation approve 架构 + `applyPostingResult:221-227` per-mutation posted 回写隐含成立（复核 A2.8 状态机证据）。裁决：主路径行为正确，闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.34 让步接收价格差异过账运行时缺失确认（P1-RC-018）**：确认 `PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行（1403 在途物资 / 2221 进项税 / 2202 应付账款）无价格差异（PPV）科目行；确认差异埋在 1403 在途物资金额中（按发票金额入账）未分集到 PPV 科目。确认 GL 仍平衡（debit 在途物资+进项税 == credit 应付）属管理会计可视性缺口非活跃数据破坏。**触及业财保护区域探针——只读确认，不改过账逻辑。** 裁决：维持 P1-RC-018 P1（Q4 会计类无例外，修复归 MR1 触 PurAcctDocProvider/VoucherFact 须 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.35 三处理策略分支可达性确认（P1-RC-018）**：确认 `ThreeWayMatcher.match:62-107` 仅 strict 拒绝/非 strict warn（1/3 策略）；grep xbiz/审批流 config/Processor 覆盖确认"审批后接收"+"接收并过账差异"无隐藏接线（零命中）。裁决：维持 P1-RC-018 P1（两策略未实现，修复归 MR1）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.36 超收容差运行时门控缺失确认（P1-RC-019）**：确认 `ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive，无 receive-vs-order qty 容差校验；确认 `ThreeWayMatcher.match` 只做 invoice-vs-receive；确认"订单10+入库20"approve 无门控通过。裁决：维持 P1-RC-019 P1（超收运行时无门控，修复归 MR1 纯 BizModel/Processor 预授权）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.37 短收超容差差异处理缺失确认（P2-RC-014）**：确认无 receive-vs-order 短收容差判定 + 无"差异处理"触发机制；确认"订单100+入库50"无差异处理触发（短收继续入库或手动关闭主路径 OK）。裁决：维持 P2-RC-014 P2（次要验收标准未完全满足，登记不强制，修复归 MR1 与 P1-RC-019 协同）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.38 关闭释放预留 config-gated 行为确认**：确认 cancel 路径 commitment release 经 config-gated `erp-fin.budget-commitment-enabled` 默认 false 门控；确认与 A1.2/A1.15 已接受 config-gated 语义一致（config-gate = 部署启用决策非契约缺失）。裁决：主路径接受，闭合。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.39 receivedQuantity 运行时值确认（P2-RC-013）**：确认 `rg setReceivedQuantity` 生产代码零 writer（仅 _gen 框架 setter）；确认 `rollupOrderReceiveStatus:244-284` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity；确认两次入库后查询值得 0（header 级进度跟踪 UNRECEIVED/PARTIAL/RECEIVED 主路径 OK）。裁决：维持 P2-RC-013 P2（次要验收标准未完全满足，登记不强制，修复归 MR1 纯 Processor 预授权）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：七项存疑点各出 §裁决（主路径闭合 / 维持 P1 reuse 重开 + 运行时证据 / 维持 P2 / 触发 MR0）+ §与既有 finding 衔接（P1-RC-018 / P1-RC-019 / P2-RC-013 / P2-RC-014 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）+ §业财保护区域探针纪律声明。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.34 触及业财保护区域探针——只读确认 PPV 行缺失，不改过账逻辑。

- [x] 验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`，含七项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：主路径闭合 / 维持分级（P1 Q4 强制实现 / P2 登记）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.33-39 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-RC-018 维持 P1（运行时确认 PPV 过账行缺失 + 两策略未实现，Q4 会计类无例外，修复归 MR1 触 PurAcctDocProvider/VoucherFact 须 ask-first）；P1-RC-019 维持 P1（运行时确认超收无门控，修复归 MR1 纯 BizModel/Processor 预授权）；P2-RC-013/P2-RC-014 维持 P2（登记不强制）。无新 finding 新建（全部维持）。
- [x] `Add` roadmap A4.2.33-A4.2.39 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 七项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is (ses_027640131ffeS0xZzlklQnErYc) — no blocking issues. Deps satisfied (A4.2 expander done); scope/rule-14 correct (A1.16 §7 存疑点 1-7 = A4.2.33-39 1:1); citation accuracy verified against source report §5/§6/§7 (P1-RC-018/P1-RC-019/P2-RC-013/P2-RC-014 file:line + verdicts match); protected-area READ-ONLY probe exemplary + P1-RC-018 ask-first boundary for MR1 fix (VoucherFact/PostingProcessor core) explicit; anti-slack clean; pattern conforms to reference. Non-blocking: bare `§5` standardized to `roadmap §横切关注点 #5`; Deferred But Adjudicated header expanded to include P2 findings. Consensus reached → flipped to active.

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

### P1-RC-018 / P1-RC-019 / P2-RC-013 / P2-RC-014 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-RC-018（PPV 过账行 + 三处理策略）修复归 MR1 R1.0 展开器，Q4 会计类无例外强制实现，触 PurAcctDocProvider/VoucherFact 核心路径须 ask-first + 独立 plan-audit（roadmap §横切关注点 #5 会计过账逻辑类）；P1-RC-019（超收容差校验）修复归 MR1 纯 BizModel/Processor 预授权；P2-RC-013（receivedQuantity 写入）/ P2-RC-014（短收差异处理）修复归 MR1 纯 Processor 预授权（登记不强制）。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: 计划已完整执行。Phase 1（运行时证据采集与验证报告撰写）+ Phase 2（Finding 衔接、roadmap/log 同步）全数完成。七项静态存疑点（A4.2.33-A4.2.39）运行时行为全部确认成立，A1.16 静态判定无一翻转：两项主路径闭合（A4.2.33 per-mutation approve 架构凭证数==2 / A4.2.38 关闭释放预留 config-gated）+ 两项 P1 维持（A4.2.34/A4.2.35 P1-RC-018 / A4.2.36 P1-RC-019）+ 两项 P2 维持（A4.2.37 P2-RC-014 / A4.2.39 P2-RC-013）。0 新 finding / 不触发 MR0 / 不归 MR1（本审计）。验证报告落盘 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（`Audit Status: closed`）；arm-index P1-RC-018/P1-RC-019/P2-RC-013/P2-RC-014 行追加 RC 交叉引用注记（维持既有分级不撤销）；roadmap A4.2.33-A4.2.39 `todo → done ✅`；log `docs/logs/2026/08-07.md` 追加完成条目。零生产代码变更（git status 仅 .md 文件），checker actual == baseline 无回归风险。 Closure Gates 中「结束审计由独立子代理执行」项已由独立结束审计子代理（新会话，无执行者上下文）核实并勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文；closure-audit 任务）
- Evidence: 独立核实通过——(1) 代码主张逐项复核活仓：`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行[1403 在途物资 DEBIT / 2221 进项税 DEBIT / 2202 应付账款 CREDIT]无 PPV 行（A4.2.34 ✓）；`ThreeWayMatcher.match:62-107` 仅 strict 拒绝/非 strict warn 1/3 策略无"审批后接收"/"接收并过账差异"分支（A4.2.35 ✓）；`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive 无 receive-vs-order qty 容差校验（A4.2.36 ✓）；`rollupOrderReceiveStatus:244-284` 仅 `orderBiz.updateReceiveStatus:283` 更新 header receiveStatus 不写 orderLine.receivedQuantity + `setReceivedQuantity` 生产代码零 writer（A4.2.39 ✓）。(2) 产物落地核实：验证报告 `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md` 存在且 `Audit Status: closed`；roadmap A4.2.33-A4.2.39 全 `done ✅` 且裁决与报告一致；arm-index P1-RC-018/P1-RC-019/P2-RC-013/P2-RC-014（:159-162）均追加 RC 运行时确认交叉注记且维持既有分级不撤销；log `docs/logs/2026/08-07.md:3-23` 含七项 SP 裁决明细 + 聚合裁决条目。(3) 反空心核实：本计划为只读审计零生产代码变更，`git status` 仅 .md 文件改动（arm-index/roadmap/log/plan + 新建报告），checker actual==baseline 无回归风险。(4) 五点一致性：Plan Status completed / Phase 1-2 completed / 全 Exit Criteria [x] / 全 Closure Gates [x] / Closure 证据在文件中——均一致。(5) Deferred honesty：P1-RC-018/P1-RC-019/P2-RC-013/P2-RC-014 修复义务明确归 MR1（P1-RC-018 触 PurAcctDocProvider/VoucherFact 须 ask-first + 独立 plan-audit §5；其余纯 BizModel/Processor 预授权），无活缺陷隐藏于 Deferred/Follow-up。审计通过，无阻塞项。

Follow-up:

- 无非阻塞跟进项目（P1 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
