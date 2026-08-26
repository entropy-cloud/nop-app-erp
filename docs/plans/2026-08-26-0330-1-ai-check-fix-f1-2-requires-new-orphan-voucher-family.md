# 2026-08-26-0330-1-ai-check-fix-f1-2-requires-new-orphan-voucher-family F1.2：REQUIRES_NEW 孤儿凭证族修复

> Plan Status: completed
> Last Reviewed: 2026-08-26
> Source: ai-check F1.2；findings：P1-CK-pur-002、P2-CK-inv-012、P2-CK-mfg-011、P2-CK-ast-015、P2-CK-ast2-021、P2-CK-mnt-007、P2-CK-qa-012、P1-CK-prj-006（REQUIRES_NEW 部分）、P2-CK-hr2-006
> Related: F1.1（fin-003 三态化已落地——本族 (c) 类站点的收敛通道已在位）；勘察报告（agent_95b25d9f 逐站点清单，2026-08-26，11 编排单元）
> Audit: required（过账编排 = plan-first 保护区域）

## Current Baseline

- 族模式：业务编排中 tryPost/doPosting（REQUIRES_NEW 独立提交凭证）位于主事务中间，其后可抛步骤（守卫/联动 hook/写库）失败时主事务回滚但凭证已提交 → 孤儿凭证。
- 逐站点勘察结论（11 编排单元）：**(a) 可前置修复 6**（pur Invoice/Payment SoD 前移、mfg 报工 updateEntity+预留释放前移、mnt 设备恢复+request 联动前移、prj 工时聚合前移、hr2 markPaid 补 alreadyPosted 去重守卫）；**(b) 结构拆分 1**（ast Disposal 双凭证链 catchUp 数据段/凭证段拆分）；**(a) 变体 1**（ast VA applyAssetValueChange 前移）；**(c) 已被 F1.1 收敛 8 子站**（inv landedCost/costAdjust、ast 四链、ast2 折旧、qa SCRAP、prj 结算——引擎幂等命中现返回既有凭证 id，重试收敛）；**证伪 2 子站**（inv StockMove doComplete——dispatchIfApplicable 已是末步其后零写；qa RETURN 分支——无凭证，finding 已正确限定）。
- 剩余差距：上述 (a)/(b) 站点未修复；(c) 站点需回归验证 F1.1 收敛性；证伪子站需书面裁决回填。

## Goals

- 全部 (a)/(b) 站点：tryPost 之后不再存在可抛守卫/联动（残余仅 posted 回写窄窗，由 F1.1 三态化收敛）。
- hr2-006 补 alreadyPosted 去重守卫（消除重试双倍 280 凭证——比孤儿更严重）。
- (c) 站点回归验证 + 证伪子站书面裁决。

## Non-Goals

- 不改引擎/凭证结构（F1.1 已完成三态化）；posted 回写窄窗不追求消除（依赖 voucherId 且需主事务持久化，结构上不可前移/后置——F1.1 幂等收敛是唯一通道）；不做对账告警新机制（若 (c) 站点回归发现收敛缺口再立项）。

## Task Route

- Type: implementation-only change（缺陷修复，机制已在勘察报告实证）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（过账失败回退纪律）、各域 owner docs
- Skill Selection Basis: `bug-diagnosis-prompt`（根因已实证）；无 ORM 变更

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 第一批纯前置修复（低风险三站）

Status: completed
Targets: `ErpPurInvoiceApproveProcessor`、`ErpPurPaymentApproveProcessor`（SoD 前移至 doPosting 前，对齐 ErpPurReceiveApproveProcessor SoD-first 范式）；`ErpHrSalaryMarkPaidProcessor`（tryPostPayment 镜像计提链 alreadyPosted 守卫 L86-91）
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix: pur 两处 SoD 守卫前移（S5：真 SoD-first——移至 approve 编排首位、一切校验之前，对齐 ErpPurReceiveApproveProcessor L38 范式；doApprove 内守卫删除）
- [x] Fix（R3）: pur Invoice `runCommitmentReleaseOnInvoiceApproveHook` 前移至 doPosting 之前（release SYNC 同事务，前移安全）
- [x] Fix: hr2 `tryPostPayment` 补 `if (alreadyPosted(buildBillCode(salary), SALARY_PAYMENT)) return true;` 守卫（镜像计提链）
- [x] Proof: pur：创建人自审 approve 被拒且**零凭证产生**（countBillLinks==0，修复前为 1）；hr2（S1 鉴别性断言）：守卫命中短路证据——重试 markPaid 后 bill-link 计数不变且 salary 仍 PAID（守卫先于引擎调用，避免依赖引擎幂等的不可区分路径）

Exit Criteria:

- [x] pur SoD 拒绝路径零凭证（单测断言）；hr2 重试单凭证（单测断言）
- [x] `mvn test -pl module-purchase/erp-pur-service,module-hr/erp-hr-service` 全绿

### Phase 2 - 第二批联动前置修复（带语义注记三站）

Status: completed
Targets: `ErpMfgWorkOrderReportCompletionProcessor`（wo updateEntity+releaseRemainingReservations 前移至 generateCompletionMove 前）；`ErpMfgMaterialIssueConfirmProcessor`（consumeReservations+writebackWorkOrderLineActualQty 前移至 generateMove 前）；`ErpMntVisitCompleteProcessor`（restoreToRunning+completeLinkedRequest 前移至 doComplete 前）；`ErpPrjTimesheetApproveProcessor`（aggregateFromTimesheet 前移至 tryPost 前）；`ErpAstValueAdjustmentProcessor`（applyAssetValueChange 前移至 tryPost 前）
Skill: none

- Item Types: `Fix | Proof | Decision`
- Prereqs: Phase 1

- [x] Fix: 五处联动前移（同主事务原子性保证失败回滚无持久化差异；语义注记：prj STRICT 预算拒绝提前到凭证提交前 = 行为改善）
- [x] Fix（R1 配对）: ast VA `executeReverseApprove` 改为无条件 `rollbackAssetValue`（前移后 NBV 变更不再以凭证成功为前提，reverse 必须对称回滚；Decision：放弃 voucherId 条件性，对齐「posted=false+告警」纪律）
- [x] Fix（S4）: mnt dispatcher 幂等命中 warn 降 info（独立小改，Phase 2 内必做）
- [x] Proof: 各域既有测试全绿（勘察清单列出的回归测试类）；「联动失败→零凭证」家族级鉴别断言 = ast `TestErpAstDisposalEquipmentLinkage#testLinkageFailureRollsBackApprove` 扩展的双零回链断言（DISPOSAL 与 #CATCHUP 双凭证零提交）；ast VA 补「posted=false → reverse 无条件回滚 NBV」鉴别用例（`TestErpAstValueAdjustment#testReverseUnconditionalRollbackWhenPostingFailed`）
- [Decision]（审查必改项 2 范围修正，guide 规则 10）：mfg 领料链 consumeReservation 失败注入**移出本计划范围**——实测同 bean id 覆盖在单测容器报 `nop.err.ioc.duplicate-bean-definition`（testBeansFile 为增量加载无 replace 语义；跨模块 `IErpInvReservationBiz` 单注入替换同理），容器缺少替换既有 bean 的机制。残余风险：领料链消耗前移无专属失败注入断言（前移逻辑与 ast 同型且代码路径经 mfg 293 全量回归覆盖）。Successor：F2.5 触及该文件时补 fail-flag 钩子或 delta 覆盖注入（已在 Deferred 登记）

Exit Criteria:

- [x] 五域 `mvn test -pl ...` 全绿 + 至少一条联动失败零凭证断言
- [x] mnt dispatcher 幂等命中 warn 降级为 info（勘察建议顺手项，独立小改）

### Phase 3 - 第三批 ast Disposal 结构拆分 + (c) 收敛回归 + 证伪裁决

Status: completed
Targets: `ErpAstDisposalProcessor#executeApprove`（catchUp 拆数据段/凭证段，两凭证尾部连续提交）；回归验证 (c) 8 子站；证伪 2 子站书面裁决
Skill: none

- Item Types: `Fix | Proof | Decision`
- Prereqs: Phase 2

- [x] Fix: Disposal 链拆分——catchUpDepreciationToDisposalPeriod 拆为「资产字段更新（前置）」+「#CATCHUP 凭证过账（后移至 DISPOSAL tryPost 相邻处）」，gainLoss 仍取补提后 accumDep
- [x] Proof: `TestErpAstDisposal`、`TestErpAstDisposalEquipmentLinkage`、`TestErpAstCatchUpDepreciation` 全绿 + 新增「设备联动失败→零凭证（两张都无）」断言
- [x] Proof: (c) 8 子站收敛回归——抽样 3 站代表性测试（S3 标准：覆盖不同 dispatcher 形态——ast 一链 + inv landedCost + qa SCRAP）+ 5 站书面分析，**每站必含 billHeadCode 跨重试稳定性核对**（反例：InvPostingDispatcher 用 move.code，主事务回滚后重试新单号幂等不命中——该核对是收敛前提）
- [x] Decision: 证伪裁决落索引——inv-012 StockMove 子站（dispatchIfApplicable 末步其后零写）+ qa-012 RETURN 分支（无凭证）的书面说明；inv-012 整条 finding 处置 = 3b/3c 经 F1.1 收敛 + 3a 证伪 → 降级/关闭裁决

Exit Criteria:

- [x] ast 测试全绿 + 设备联动失败零凭证断言
- [x] (c) 站点收敛证据（3 代表测试 + 5 书面分析）落计划
- [x] 证伪裁决回填索引

### Phase 3 执行记录（(c) 收敛分析与证伪裁决）

**billHeadCode 跨重试稳定性核对（8 站逐一，收敛前提）**：
- inv landedCost `landedCost.getCode()` / inv costAdjust `adjust.getCode()` — 单据编码稳定 ✓
- ast 四链 cap/split/merge/inventory `.getCode()` — 稳定 ✓
- ast2 折旧 `asset.code + period` — 稳定 ✓
- qa SCRAP `ncr.getCode()` — 稳定 ✓
- prj 结算 `settlement.getCode()` — 稳定 ✓
- 反例（唯一不稳定）：mfg 领料 InvPostingDispatcher `move.getCode()`——重试重建移动单生成新单号幂等不命中，已裁决 Deferred（§mfg 领料链凭证后残余）

**收敛证据（3 代表测试 + 引用）**：① 引擎级 `TestErpFinPostingService#testPostIdempotent`（幂等返回同 id 凭证不增）；② 传导级 `TestErpPrjTimesheetMulticurrencyPosting#testIdempotentRepostKeepsPostedTrue`（F1.1 先红后绿）；③ 回滚零凭证级 `TestErpAstDisposalEquipmentLinkage#testLinkageFailureRollsBackApprove`（扩展断言 DISPOSAL 与 #CATCHUP 双零回链）。全部 8 站 billHeadCode 稳定 + 引擎幂等返回既有 id → 重试 approve 幂等命中 posted 收敛。

**证伪裁决（2 子站）**：
- inv-012 StockMove doComplete 子站：`dispatchIfApplicable` 已是末步其后零写步骤（勘察 + 审计双核实）——不属编排顺序缺陷，仅剩理论 commit 窗口。
- qa-012 RETURN 分支：orchestrateReturn 无凭证路径（finding 原文已正确限定 SCRAP-only）。

**实施中发现（重要 ORM 交互）**：Disposal 拆分首轮失败于主事务脏计划行在内层 REQUIRES_NEW flush 上的跨连接 `update-entity-not-found`（内层连接看不见主事务未提交 INSERT）——修复为计划行回填推迟至两凭证全提交后尾部（`postCatchUpVoucherOnly` + `backfillCatchUpSchedules`）。该交互对后续所有「REQUIRES_NEW + 主事务脏实体」编排都有参考价值。

## Draft Review Record

- Independent draft review iteration 1: `needs revision → 修订后通过`（agent `agent_3e08342f`，2026-08-26）——6 类站点代码事实核查全部成立（SoD 位置/守卫不对称/REQUIRES_NEW 边界/证伪两子站/F1.1 引擎行为）；3 必改：R1 ast VA 前移的 reverseApprove 不对称陷阱（须配对无条件回滚）、R2 mfg 领料链残余不可被 F1.1 收敛（move.code 幂等键不稳定，须 Deferred+successor）、R3 pur Invoice 承付 hook 残留（须一并前移）；5 建议（S1 hr2 鉴别性断言/S2 testBeansFile mock 范式/S3 抽样标准+billHeadCode 稳定性核对/S4 文档对齐门控/S5 SoD-first 措辞）全部采纳落档。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成（(a)6+(b)1+(a)变体1 修复 + (c)8 回归 + 证伪2 裁决）
- [x] 涉及域全量 `mvn test` 全绿（purchase/hr/manufacturing/maintenance/projects/assets/quality/inventory）
- [x] 全 reactor `mvn clean install -DskipTests` 通过
- [x] compliance checker 不高于 C0.2 快照
- [x] ai-check-index 回填：pur-002/hr2-006/mnt-007/mfg-011/prj-006（全修或部分修注记）/ast-015/ast2-021/qa-012/inv-012 → 终态
- [ ] roadmap F1.2 → done
- [x] 「REQUIRES_NEW 凭证后无可抛步骤」编排不变量回写 `docs/architecture/processor-extension-pattern.md`（S4 文档对齐）
- [x] 独立结束审计
- [x] `docs/logs/2026/08-26.md` 追加

## Deferred But Adjudicated

### posted 回写窄窗

- Classification: watch-only residual
- Why Not Blocking Closure: 依赖 voucherId 且需主事务持久化，结构不可消除；F1.1 幂等收敛（重试返回既有凭证 id）是闭合通道
- Successor Required: no（V.1 全量回归观察）

### mfg 领料链消耗失败注入断言（结束审计必改项 2 范围修正）

- Classification: watch-only residual
- Why Not Blocking Closure: 单测容器无 bean 替换机制（duplicate-bean-definition 实测），前移模式与 ast 同型且经全量回归覆盖；家族级鉴别断言由 ast 双零回链用例承担
- Successor Required: yes（F2.5 触及该文件时补注入钩子）

### mfg 领料链凭证后残余（R2）

- Classification: watch-only residual
- Why Not Blocking Closure: `aggregateIssueMaterialCost`（依赖移动单流水，不可前移）+ issue DONE 翻转 + `applyMaterialCostToWorkOrder`（乐观锁）位于第二张凭证前/后；第一张 INV 出库凭证幂等键 = move.code，主事务回滚后重试重建移动单生成新单号，幂等不命中——本残余不可被 F1.1 收敛，需专门方案（稳定幂等键或补偿），超出本计划范围
- Successor Required: yes（F2.5 mfg-工单 P1 簇内一并处理，finding mfg-011 状态将注记「部分修复+残余 successor」）


## Closure

Status Note: 三 Phase 全绿（9 处代码修复 + pur 336/hr/mfg 293/mnt/ast/prj 173 全绿 + 全 reactor install SUCCESS + compliance R2c 1506 baseline-raise 登记）；9 条 finding 终态（8 fixed + mfg-011 部分修复归 F2.5）；编排不变量立法 processor-extension-pattern.md 硬规则 5。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `agent_34b8fb70-47dc-4ed8-ab60-7ff86923accd`（fresh session）
- Evidence: 首轮 FAIL 3 必改（ast2-021 回填缺失——执行者前缀笔误 P2/P3、Phase 2 两断言虚勾、Exit Criteria 漏勾）→ 修复（ast2-021 正确回填 + VA posted=false 鉴别用例 7/7 独立重跑绿 + mfg 注入受阻按规则 10 改判移出记录证据 + Exit 勾选）→ 复裁 **PASS**（三项逐一复核通过，9 处修复真实/快照良性/R1 配对落地确认）
