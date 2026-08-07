# 2026-08-07-2345-2 rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime 移动单时序/批次效期/盘点并发运行时确认

> Plan Status: active
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.74 / A4.2.75 / A4.2.76 / A4.2.77 / A4.2.78 / A4.2.80 / A4.2.81 / A4.2.82（A4.2.79 排除——MR1 P1-RC-031 修复落地前阻塞，见 Non-Goals）
> Related: `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（A1.25 §7 存疑点 SP-1/SP-2 + 零新 finding）、`docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（A1.26 §7 存疑点 SP-1..SP-4 + §6 新建 P1-RC-031[效期拦截完全缺失]）、`docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（A1.27 §7 存疑点 SP-1/SP-2/SP-3 + §6 reuse P1-MA2-062[completeTake stub]/P2-RC-029/P2-RC-030/P1-MA2-093）
> Audit: required

## Current Baseline

inventory 域三切片（A1.25 移动单主链与追溯 / A1.26 批次与可用量 / A1.27 盘点-估值-并发-看板）MA1 报告 §7 共列出 10 个静态存疑点（A1.25 SP-1/SP-2 + A1.26 SP-1..SP-4 + A1.27 SP-1/SP-2/SP-3 + A1.27 SP-4[orgId reuse P1-MA2-093 已由 A4.1.25/A4.2.10 闭合]）。其中 A1.26 SP-4（A4.2.79）显式声明「MR1 P1-RC-031 修复落地后」前置依赖，MR1 R1.0 尚未启动（roadmap R1.0 = todo），故本计划**排除 A4.2.79**（同 A4.2.3 MR1-P1-RC-008 阻塞先例，保留 todo 待 MR1 落地）。本计划覆盖其余 8 项。

- **A4.2.74（A1.25 SP-1 InvPostingDispatcher post-commit 时序边缘风险）**：HEAD 静态判定 = `dispatchIfApplicable:113` 在 doComplete 内同步调 `voucherBiz.post`（REQUIRES_NEW 独立事务），非 post-commit；若 REQUIRES_NEW 凭证已 commit 后外层 @BizMutation rollback（极罕见——dispatchIfApplicable 是 doComplete 最后一步），凭证孤立。运行时确认边缘时序风险（L1「异步生成」由失败隔离+posted 标志+DeferredPostingSweepJob 兜底满足，L2「post-commit」漂移以 L1 为准）。
- **A4.2.75（A1.25 SP-2 forwardTrace 超深链/多分支链 truncated 行为）**：HEAD 静态判定 = `TraceChainQuery.forwardTrace:50-88` BFS max-depth 默认 10；多分支链节点数可能超阈值。运行时确认 truncated=true + 节点数符合 root+层级 + 无无限循环（TestErpInvTraceChain 已强覆盖 9 方法，运行时确认边界）。
- **A4.2.76（A1.26 SP-1 allow-negative-stock=true 下并发出库实际余额下限行为）**：HEAD 静态判定 = config 翻转 true 后 `validateAvailable:117-119` 短路所有检查；极端并发下 totalQuantity 可能深度为负（乐观锁保证不超扣，但无下界守卫）。运行时确认余额下限行为（config 默认 false，UC-INV-09 默认安全）。
- **A4.2.77（A1.26 SP-2 batchTrace 跨域 move 链下聚合正确性）**：HEAD 静态判定 = `batchTrace:168-192` 聚合 findLinesByBatch+findLedgersByBatch→moveIds→findActiveMove（filter delVersion=0）。跨域 move 链共享 batchNo（批次继承语义）下聚合完整性。运行时确认 batchTrace(BATCH-001) 返回采购入库+mfg 领料 2 nodes（不含新批次节点）。
- **A4.2.78（A1.26 SP-3 expiryDate 字段无 writer 时默认值行为）**：HEAD 静态判定 = `ErpInvBatch.expiryDate` ORM `:908` 存在但 `ErpInvBatchBizModel` 15 行 CRUD 桩——expiryDate 由 BatchGenealogyWriter.newBatchEntity（mfg 完工时）写入，其他入库路径是否写未确认。运行时确认采购入库移动单带 batchNo 是否自动创建 ErpInvBatch + expiryDate/shelfLifeDays 默认值（为 P1-RC-031 修复 null 语义设计输入）。**为 MR1 P1-RC-031（效期拦截）修复提供设计输入，本项只读确认不改字段语义。**
- **A4.2.80（A1.27 SP-1 UC-INV-07 completeTake DONE 后手工 generateMove 处置差异的实际余额影响）**：HEAD 静态判定 = completeTake stub（仅 setDocStatus(DONE) 无 generateMove）；手工入口仍走移动单状态机（余额可追溯，A2.11 已证）。运行时确认账实差异在库管员介入前悬留（运营风险非数据破坏）。**P1-MA2-062 reuse，§4 复核倾向重开须人工确认 product-scope。**
- **A4.2.81（A1.27 SP-2 UC-INV-08 高并发下 max-retry 耗尽后移动单状态）**：HEAD 静态判定 = `updateBalanceWithRetry:255-328` 重试上限默认 5 + @BizMutation 事务回滚移动单不留悬挂（A2.17 §13 PASS）。运行时确认极端竞争（>5 线程同维度）最终一致性。
- **A4.2.82（A1.27 SP-3 UC-INV-10 posting 失败留 posted=false 时 DeferredPostingSweepJob 兜底触发频率）**：HEAD 静态判定 = DeferredPostingSweepJob 兜底重试（属 P1-MA4-001 family 业财悬挂维度）。运行时确认 posted=false 悬挂凭证的兜底触发频率与成功率。

剩余差距：八项均为只读运行时确认。A4.2.78 为 MR1 P1-RC-031（效期拦截缺失）修复提供 expiryDate null 语义设计输入——只读确认不改字段语义，P1-RC-031 修复纯 BizModel/Processor + ErrorCode + 测试补充按 roadmap 预授权类目[代码逻辑修复]可自动执行不触 ask-first（不触及 ORM/会计过账核心路径，仅消费既有 expiryDate 字段）；A4.2.80（P1-MA2-062 reuse）为已 resolved finding §4 复核倾向重开须人工确认 product-scope，修复归 MR1。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.74-A4.2.78、A4.2.80-A4.2.82 八项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：边界/数值项（A4.2.74/75/76/77/78/81/82）确认行为正确或登记 watch-only；resolved/§4 复核项（A4.2.80 P1-MA2-062 reuse）确认 stub 行为 + 维持 §4 复核倾向重开须人工确认 product-scope；若运行时发现活跃数据破坏则触发 MR0。
- 完成后回写 roadmap A4.2.74-A4.2.78、A4.2.80-A4.2.82 `todo → done`（A4.2.79 保留 todo 待 MR1 落地），并按裁决更新 arm-index（维持注记，无未经比对新建）。

## Non-Goals

- 不实现效期拦截（P1-RC-031）/ completeTake 自动差异移动单（reuse P1-MA2-062）——修复义务归 MR1 R1.0 展开器；P1-RC-031 纯 BizModel/Processor + ErrorCode + 测试补充按 roadmap 预授权类目[代码逻辑修复]可自动执行不触 ask-first；reuse P1-MA2-062 §4 复核倾向重开须人工确认 product-scope。
- **不覆盖 A4.2.79（A1.26 SP-4 MR1 P1-RC-031 修复落地后 reserved/available 一致性）**——显式声明 MR1 修复落地前置依赖，MR1 R1.0 尚未启动（roadmap R1.0 = todo），保留 todo 待 MR1 落地后回队（同 A4.2.3 MR1-P1-RC-008 阻塞先例）。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）；A4.2.78 只读确认 expiryDate 默认值不改字段语义。
- 不复跑 MA2 状态机审计（A2.11/A2.17 移动单状态机 + 并发乐观锁作为既有证据输入，不重新核实行为本身）；不重审 P1-RC-031（A1.26 已审，本计划仅运行时确认 + 为修复提供 null 语义输入）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md` §5/§6/§7 + `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md` §5/§6/§7 + `docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md` §5/§6/§7 + `docs/design/inventory/`（use-cases.md / state-machine.md / trace-chain.md / cross-domain.md）+ `docs/design/dashboards.md`（库存看板）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 移动单过账时序确认 + 追踪链边界确认 + 负库存并发边界 + 批次聚合完整性 + expiryDate writer census + completeTake stub 行为 + 重试耗尽事务回滚 + DeferredPostingSweepJob 兜底（grep census / dispatchIfApplicable 时序追踪 / forwardTrace BFS max-depth 边界追踪 / updateBalanceWithRetry 重试链追踪 / batchTrace 聚合路径追踪 / BatchGenealogyWriter vs 采购入库 batch 创建 writer census），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.74-A4.2.78、A4.2.80-A4.2.82）

Status: planned
Targets: `docs/audits/2026-08-07-2345-rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.25 done ✓；A1.26 done ✓；A1.27 done ✓

- [ ] **A4.2.74 InvPostingDispatcher post-commit 时序边缘风险确认**：确认 `dispatchIfApplicable:57-80` 在 `doComplete:113` 内同步调 `executor.postEvent`→`voucherBiz.post`（@Transactional(REQUIRES_NEW) 独立事务），非 post-commit；确认 dispatchIfApplicable 是 doComplete 最后一步（其后无其他写操作，外层 rollback 极罕见）；确认 L1「异步生成」由失败隔离（try/catch :69-76）+ posted 标志 + DeferredPostingSweepJob 兜底共同满足（L2「post-commit」漂移以 L1 为准）。**触及业财过账基础设施探针——只读确认，不改过账逻辑。** 裁决：主路径行为正确闭合（与 A1.25 §5 倾向接受一致），登记 watch-only residual（REQUIRES_NEW 凭证孤立边缘风险极低）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.75 forwardTrace 超深链/多分支链 truncated 行为确认**：确认 `TraceChainQuery.forwardTrace:50-88` BFS 按 originMoveId 反查下游 + max-depth 默认 10；确认 TestErpInvTraceChain 9 方法（testForwardAndBackwardTraceChain/testMaxDepthTruncation/testRingDetectionTruncated 等）已覆盖 truncated 边界；确认多分支链（1→N→M）节点数超阈值时 truncated=true + 无无限循环。裁决：主路径行为正确闭合（dedicated 强测试存在，A1.25 §5 接受）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.76 allow-negative-stock=true 下并发出库实际余额下限确认**：确认 `validateAvailable:117-119` 在 config `CONFIG_ALLOW_NEGATIVE_STOCK`=true 时短路所有检查；确认 `updateBalanceWithRetry:256-328` + 乐观锁保证不超扣（A2.17 §13 PASS）但无下界守卫——极端并发下 totalQuantity 可深度为负；确认 config 默认 false（UC-INV-09 默认安全）。裁决：登记 watch-only residual（config-gated 部署启用决策，非默认活跃，与 A4.1.4 config-gate 范式一致）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.77 batchTrace 跨域 move 链下聚合正确性确认**：确认 `batchTrace:168-192` 聚合 findLinesByBatch+findLedgersByBatch→moveIds→findActiveMove（filter delVersion=0）；确认批次继承语义下 batchTrace(BATCH-001) 返回采购入库+mfg 领料 2 nodes（不含新批次 BATCH-002 节点）+ batchTrace(BATCH-002) 返回 mfg 完工+sales 出库 2 nodes。裁决：主路径聚合正确闭合（批次继承语义下按 batchNo 精确聚合，跨批次不混）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.78 expiryDate 字段无 writer 时默认值行为确认**：grep census `setExpiryDate|setShelfLifeDays` 跨 module-inventory 生产代码 writer 集合；确认 `BatchGenealogyWriter.newBatchEntity:159-168`（mfg 完工时）写入 expiryDate，确认采购入库/销售退货入库路径是否写 expiryDate（不写则 null）；确认采购入库移动单带 batchNo 是否自动创建 ErpInvBatch + expiryDate/shelfLifeDays 默认值（null vs 具体值）。**为 MR1 P1-RC-031 修复提供 null 语义设计输入，只读确认不改字段语义。** 裁决：主路径行为确认 + 为 P1-RC-031 修复登记 null 语义设计输入（null = 跳过/永不过期/立即过期的修复时决策）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.80 UC-INV-07 completeTake DONE 后手工 generateMove 实际余额影响确认（reuse P1-MA2-062）**：确认 `completeTake:40-50` 仅 setDocStatus(DONE) 无 generateMove + 无 actualQuantity vs totalQuantity 比对；确认手工入口 generateMove 仍走移动单状态机（余额可追溯，A2.11 已证）+ 账实差异在库管员介入前悬留（运营风险非数据破坏）。裁决：维持 reuse P1-MA2-062 §4 复核倾向重开须人工确认 product-scope（completeTake stub = P1-MA2-062 字面描述，§7 复用不新建；修复归 MR1）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.81 UC-INV-08 高并发下 max-retry 耗尽后移动单状态确认**：确认 `updateBalanceWithRetry:255-328` 重试上限 `CONFIG_CONCURRENT_DEDUCT_MAX_RETRY` 默认 5 + 耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT`；确认 doComplete 经 @BizMutation 事务回滚移动单不留悬挂（移动单未达 DONE）；确认极端竞争（>5 线程同维度）最终一致性（A2.17 §13 已静态证实 retry 机制）。裁决：主路径行为正确闭合（移动单不留悬挂），登记 watch-only residual（极端竞争下需运维介入重试）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **A4.2.82 UC-INV-10 posting 失败 posted=false 时 DeferredPostingSweepJob 兜底触发频率确认**：确认 posting 失败留 posted=false 时 DeferredPostingSweepJob 兜底重试链路（属 P1-MA4-001 family 业财悬挂维度）；确认 InvPostingDispatcher 失败隔离 try/catch + posted 标志 + sweep job 三层兜底。**触及业财过账基础设施探针——只读确认，不改过账逻辑。** 裁决：主路径兜底行为闭合（属 P1-MA4-001 family 不重复登记，运行时确认兜底链路可达）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] **验证报告撰写**：八项存疑点各出 §裁决（主路径闭合 / 维持 reuse + 运行时证据 / 登记 watch-only / 为 P1-RC-031 修复设计输入 / 触发 MR0）+ §与既有 finding 衔接（P1-RC-031 / P1-MA2-062 / P1-MA4-001 family / P0-MA2-020/P2-MA2-028/P1-MA4-021 resolved HEAD 复核 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.78 为 P1-RC-031 修复提供 null 语义设计输入——只读确认不改字段语义。

- [ ] 验证报告落盘，含八项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [ ] 每项裁决明确：主路径闭合 / 维持分级（reuse / watch-only / 设计输入）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: planned
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.74-78、A4.2.80-82 done；A4.2.79 保留 todo）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [ ] `Decision` arm-index 衔接裁决：P1-RC-031（效期拦截缺失）维持 P1（运行时确认 expiryDate 无 writer/null 默认值，为修复 null 语义提供设计输入；修复纯 BizModel/Processor + ErrorCode + 测试补充预授权不触 ask-first）；reuse P1-MA2-062（completeTake stub）维持 §4 复核倾向重开须人工确认 product-scope（修复归 MR1）；P0-MA2-020/P2-MA2-028/P1-MA4-021/P1-MA4-020 维持 resolved（HEAD 复核无回退）。无新 finding 新建（全部维持）。
- [ ] `Add` roadmap A4.2.74-A4.2.78、A4.2.80-A4.2.82 `todo → done`（A4.2.79 保留 todo 注记 MR1 阻塞）；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [ ] roadmap 八项状态已更新为 done 且与报告裁决一致；A4.2.79 保留 todo 并注记 MR1 阻塞原因
- [ ] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0271308f0ffebgyBVVy6T4akei) — no blocking issues. Item→roadmap→§7 1:1 mapping correct (8 items); A4.2.79 exclusion correctly justified (MR1 P1-RC-031 前置依赖, same precedent as A4.2.3 MR1-P1-RC-008 blocked + retained todo, recorded in Non-Goals + Deferred But Adjudicated not silently dropped); protected-area discipline correct (A4.2.78 expiryDate read-only null-semantics probe; A4.2.80 P1-MA2-062 §4 复核须人工确认 product-scope noted); Deps satisfied; citation accuracy verified against A1.25/A1.26/A1.27 §6. Non-blocking addressed: added bold `触及业财过账基础设施探针——只读确认` markers to A4.2.74/82 for pattern parity. Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [ ] 范围内行为完成（八项存疑点均有 file:line 运行时证据 + 明确裁决）
- [ ] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [ ] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [ ] 无范围内项目降级为 deferred/follow-up（A4.2.79 非「降级」——显式 MR1 前置依赖，保留 todo 待 MR1 落地后回队，记录于 Non-Goals）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A4.2.79（MR1 P1-RC-031 修复落地后 reserved/available 一致性）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A4.2.79 显式声明「MR1 P1-RC-031 修复落地后」前置依赖——验证 P1-RC-031 效期拦截修复实现的拦截点选择（doConfirm vs doComplete）对 reserved/available 一致性的影响。MR1 R1.0 尚未启动（roadmap R1.0 = todo），无法验证不存在的修复。保留 todo 待 MR1 P1-RC-031 修复落地后回队（同 A4.2.3 MR1-P1-RC-008 阻塞先例）。
- Successor Required: yes（MR1 R1.0 → P1-RC-031 修复落地后回队 A4.2.79）

### P1-RC-031 / reuse P1-MA2-062 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-RC-031（效期拦截缺失）修复归 MR1 纯 BizModel/Processor + ErrorCode + 测试补充预授权不触 ask-first（不触及 ORM/会计过账核心路径，仅消费既有 expiryDate 字段）；reuse P1-MA2-062（completeTake stub）修复归 MR1 §4 复核倾向重开须人工确认 product-scope。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- A4.2.79 待 MR1 P1-RC-031 修复落地后回队（显式 MR1 前置依赖，记录于 Deferred But Adjudicated 节）
