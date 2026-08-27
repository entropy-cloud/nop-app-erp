# 2026-08-27-2100-1-ai-check-fix-f2-2-finance-arap-p1 F2.2：finance-AR/AP P1 簇（fin2-001/002/003/004 + fin2-005）

> Plan Status: completed
> Last Reviewed: 2026-08-27
> Source: ai-check F2.2；findings：P1-CK-fin2-001/002/003/004 + P2-CK-fin2-005（`docs/audits/check/ck-finance-arap.md`——控制点/证据/修复方向均已实证）
> Related: F2.1（同模块先例）；fin2-005 佐证 pur-005/sal-012 跨域
> Audit: required（会计/财务 = plan-first）

## Current Baseline

检查报告已实证五条控制点（含推演时序与测试缺口佐证）：
1. **fin2-001**：`AutoReconciliationEngine#matchByRatio` L174 分母在 payment 循环外一次计算不刷新；L209 尾差守卫 `invOpen.add(tail)` 方向反（应为 subtract）——三笔付款推演超开 125。
2. **fin2-002**：`ReconciliationSettler#settleWithFx` L63-74 双侧按各自汇率分别回写但实际生效金额不落行；`reverseSettle` L97-105 按行金额对称回滚——FX 红冲后收付款项残留 |Δrate×amount|。
3. **fin2-003**：`ErpFinReconciliationPostProcessor#post` 逐行独立 validateLine 不聚合同 item 多行累计——手工两行各 60 对 open=100 静默超核销（settled=120/open=-20/SETTLED）。
4. **fin2-004**：`ErpFinBadDebtProcessor` approveInternal/executeRecovery/executeReverseApprove 三处不校验 item 现态——三条交错时序写穿（负 settled/虚增 open/残额永久退出计提）。
5. **fin2-005**：`ErpFinArApItemGenerator#cancelOnReverse` 无条件 CANCELLED 不守卫 settled>0；`reverseSettle` 对 CANCELLED 项回写后 resolveStatus 复活为 OPEN。

## Goals

五条全修（P1×4 + P2×1），每条先写失败测试再修。

## Non-Goals

- fin2-006（P2 余额缓存刷新，归 F3.x）；双轨核销系统面（P2-MA2-038 watch-only 已登记）；fin-011 多账套幂等（C3.1）。

## Task Route

- Type: implementation-only change；Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`、`bad-debt.md`
- Skill Selection Basis: none（检查报告 `ck-finance-arap.md` 即根因实证；草案审查已对五条控制点逐条源码级复核成立）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fin2-002 测试需在测试内开启 `erp-fin.recon-fx-gain-loss-enabled=true`，非环境依赖）

## Execution Plan

### Phase 1 - fin2-001 BY_RATIO 修复

Status: planned
Targets: `AutoReconciliationEngine#matchByRatio`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix: 每笔付款迭代前重算 `totalInvoiceOpen = Σ invoiceOpen 剩余`；尾差守卫改 `invOpen.subtract(tail).compareTo(precision.negate()) >= 0`
- [x] Proof（审查 R3 修正场景）：换可先红输入——发票 500+500、付款 600+500：修复前 P2 尾差 300 对 open=100 的发票 B 生成超开行（settled 700/500、open −200）→ 断言「每张发票累计结算 ≤ open+precision」先红后绿；另补 3×500 vs 1000 的 P3 unmatched 报告断言（修复前 P3 无行也无报告）。Baseline 推演更正：原「3×500 超开 125」不成立（P2 尾差恰好补满、P3 静默丢弃）——缺陷为真但需正确输入触发；先写失败测试复现定谳（检查报告「最不确定」项）

Exit Criteria:

- [x] 多笔付款 BY_RATIO 失败测试先红后绿（超开行消除、Σ结算=Σ付款）

### Phase 2 - fin2-003 聚合校验

Status: planned
Targets: `AbstractErpFinReconciliationProcessor#validateLine` 或 PostProcessor
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（无代码依赖，仅执行顺序）

- [x] Fix: post 前按 invoiceItemId/paymentItemId 分组聚合行金额对累计值 assertNotOver（引擎本地 map 逻辑下沉校验层）
- [x] Proof: 手工 create 两行共享 item 各 60 vs open=100 → post 拒绝（ERR 超核销）；单行合法路径零回归

Exit Criteria:

- [x] 共享 item 两行累计超开 post 拒绝测试绿 + 单行合法路径回归绿

### Phase 3 - fin2-002 FX 对称回滚

Status: planned
Targets: `ReconciliationSettler#settleWithFx/reverseSettle`
Skill: none

- Item Types: `Fix | Decision | Proof`
- Prereqs: Phase 2

- [x] Fix: reverse 时按 `settledSource × item.rate` 现算重演逆运算（两侧各按自身汇率回退）——避免 ORM 新列（fin2-002 修复方向二选一裁决：现算重演，无 schema 变更；替代方案「行级新增 per-side functional 字段」被否决——ORM 模型属保护区域且现算重演在 rate 不可变假设下无信息损失）
- [x] Proof: FX settle（rate 7.0/7.1）→ reverse → 双侧归位断言——`TestErpSalMultiCurrencyReconFx#testFxSettleThenReverseRestoresBothSides`（发票侧退 7910/收款项退 8023 双侧 settled 归零 + open 恢复；修复前收款项按行值 7910 对称回滚残留 113）；单币种路径 settle↔reverse 对称零回归

Exit Criteria:

- [x] FX settle→reverse 双侧归位断言测试绿（config 仅测试内开启）

### Phase 4 - fin2-004 坏账状态守卫

Status: planned
Targets: `ErpFinBadDebtProcessor`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 3

- [x] Fix（审查 R2 补全）: execute* 顶部按 docType 断言 item 现态（writeOff/recovery approve 需 OPEN/PARTIAL 或 WRITTEN_OFF 各自对称；**executeReverseApprove 同补**——writeOff 反审需 item 现为 WRITTEN_OFF、recovery 反审需现为 OPEN，否则时序 2 不可拦截）；executeRecovery 补 amount ≤ settled 对称校验；executeWriteOff 补 open−amount 后置断言（残额 > precision 拒绝）
- [x] Proof: 三条交错时序负向测试——`testDanglingRecoveryApproveRejected`（悬空 recovery：快路径预检先拒，守卫等价）+ `testReverseWriteOffAfterRecoveryRejected`（时序 2：反审原单被拒，item-state-mismatch 或引擎期间失败均阻断双重回滚）+ `testWriteOffWithEnlargedOpenRejected`（交错 settled=200 < amount=500：recovery-exceeds-settled 对称校验拒）+ `testSettledItemSourceReverseRejected`（settled=50 源单红冲：settled-not-reversable 拒）+ happy path 零回归（TestErpFinBadDebt 11/11）

Exit Criteria:

- [x] 三条交错时序负向测试 + happy path 回归全绿

### Phase 5 - fin2-005 回滚通道守卫

Status: planned
Targets: `ErpFinArApItemGenerator#cancelOnReverse`、`ReconciliationSettler#applySettlement`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 4

- [x] Fix（审查 R1 冲突处理，方案 a）：cancelOnReverse 前置 `settledAmount>0` 抛错（提示先 reverse 核销单）+ reverseSettle 对 CANCELLED 目标拒绝（不复活）+ **扩展 `AdvanceOffsetOrchestrator#reverseOffset` 在红冲凭证前先对报销 payable 侧 `reverseSettlement` 归零 settled**（默认开启主路径 `erp-fin.advance-auto-offset-on-expense=true` 的报销反审流不被新守卫击穿；`TestErpFinExpenseOffsetAdvance#testOffsetThenReverse` 影响面登记——扩展后断言仍成立：settled 归零再 cancel，终态不变）
- [x] Decision: 借款单在有抵扣后反审核将被新守卫拒（`ErpFinEmployeeAdvanceProcessor#doReverseApprove` 无 outstanding 守卫的现状改变）——显式记为有意变更（防写穿 > 便利性）
- [x] Proof: 已核销 item 源单反审核被拒；CANCELLED 项核销单 reverse 被拒（不复活 OPEN）；报销抵扣→反审核 happy path 零回归（`TestErpFinExpenseOffsetAdvance` 全绿）

Exit Criteria:

- [x] 两条守卫拒绝路径测试绿

### Phase 6 - 收口

Status: completed
Targets: 索引/roadmap/日志
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: Phase 1-5

- [x] fin 全量绿（522/522）+ 全 reactor install SUCCESS + compliance R2c=1538 基线一致（+2 per-site 登记）
- [x] 索引回填 5 条 + roadmap F2.2 done + 日志
- [x] 独立结束审计

## Draft Review Record

- Independent draft review iteration 1: `accept（修订后通过）`（草案审查会话，2026-08-27）——五条控制点逐条源码级复核全部成立（fin2-001 `AutoReconciliationEngine` L174 分母循环外/L209 `add(tail)` 方向反；fin2-002 `settleWithFx` L63-74 per-side 回写不落行 vs `reverseSettle` L97-105 按行对称；fin2-003 `PostProcessor#post` L37-39 逐行 validateLine 无聚合；fin2-004 `approveInternal`/`executeRecovery`/`executeReverseApprove`/`executeWriteOff` 四方法零/弱守卫实证；fin2-005 `cancelOnReverse` L129-140 无条件 CANCELLED）；roadmap F2.2 范围（001-005，ready）与计划范围一致、索引五条均 open、owner docs 存在；fin2-002 现算重演方向裁决成立（避免 ORM 保护区域）。Minor 格式修订就地完成：Task Route 字段名对齐模板（Skill Selection Basis）、补 Infrastructure And Config Prereqs、各 Phase 补 Skill/Item Types/Prereqs/Exit Criteria、Deferred 补 Why Not Blocking Closure。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成（5 条修复 + 8 个新测试：fin 522/522 全绿含 11 BadDebt/9 AutoRecon/9 Recon/2 Offset + sal FX reverse 315/315）
- [x] `mvn test -pl module-finance/erp-fin-service` 全绿 + reactor install
- [x] compliance 与基线一致
- [x] 索引回填 + roadmap done + 日志
- [x] 独立结束审计

## Deferred But Adjudicated

### 坏账守卫测试债（复裁条件）

- Classification: watch-only residual（非阻塞测试债）
- Why Not Blocking Closure: item-state-mismatch 直接触达需冻结时钟变体（红冲凭证期间解析先于守卫）；residual-not-zero 触达需 approval-gated 构造（require-approval=false 下无审批窗口）——两者守卫代码经源码复核语义自洽且同族模式已由 recovery-exceeds-settled 精确证明
- Successor Required: yes（F3 finance P2 簇）

### fin2-006 余额缓存刷新

- Classification: F3.x 范围（P2）
- Why Not Blocking Closure: 独立控制点（坏账/抵扣路径余额缓存 refresh 缺失 vs 本簇核销算术与守卫），非 fin2-001..005 修复面；roadmap F2.2 未含此项
- Successor Required: yes（F3 finance P2 簇）

## Closure

Status Note: 五条修复全部落地 + 首轮审计 FAIL 三缺口（FX reverse 测试/坏账时序 2·3/已核销源单测试）当日补齐——fin **522/522** + sal **315/315** 全绿；reactor install SUCCESS；compliance R2c=1538 与基线一致（+2 per-site 登记）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `agent_a4c6685d`（fresh session，两轮）
- Evidence: 首轮 **FAIL**（三测试缺口：FX reverse 归位/坏账时序 2·3/已核销源单 + 簿记）→ 当日补齐（`testFxSettleThenReverseRestoresBothSides` 先红后绿属性成立 + 时序 2 双码阻断 + recovery-exceeds-settled 精确断言 + settled-not-reversable 精确断言）→ 复裁 **PASS on substance**（独立重跑 fin 522/sal 315/reactor install 156 模块/R2c=1538 四项复现；residual-not-zero 裁决接受为非阻塞测试债）；3 机械收尾（Phase 6 勾选/日志 493/测试债登记）本条目一并处理
