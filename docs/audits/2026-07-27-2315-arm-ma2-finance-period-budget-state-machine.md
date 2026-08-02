# MA2 finance 状态机审查 — 预算与期间（A2.5b）

> Audit Date: 2026-07-27
> Plan: `docs/plans/2026-07-27-2315-1-audit-remediation-ma2-finance-period-budget-state-machine.md`
> Domain: finance / 会计期间状态机 + 预算方案状态机（A2.5b S 级拆分 2/3）
> Skill: `state-machine-business-review-prompt.md`
> Verdict: **FAIL**（2 项新 P1 + 2 项新 P2；7 项已登记 MA2 finding 运行时影响复核无升级）

---

## 0. 审查范围与状态机基线

### 0.1 审查对象

| # | 状态机 | 状态集合（dict 实测） | 主权源 |
|---|--------|--------------------|--------|
| A | **会计期间状态机** | `erp-fin/period-status`（5 态）：OPEN / CLOSING / CLOSED / NEVER_OPENED / CLOSED_FINAL | `module-finance/model/app-erp-finance.orm.xml:200-206`、`docs/design/finance/state-machine.md §对象二`、`docs/design/finance/period-close.md §期间控制` |
| B | **per-module 关账子状态机** | `erp-fin/module-close-status`（3 态）：OPEN / CLOSING / CLOSED × 5 模块（AR/AP/INV/GL/AST） | `module-finance/model/app-erp-finance.orm.xml:240-244`、`docs/design/finance/period-close.md §per-module 关账结果数据源` |
| C | **预算方案状态机** | `erp-fin/budget-status`（6 态）：DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED / CLOSED | `module-finance/model/app-erp-finance.orm.xml:332-340`、`docs/design/finance/budget.md §ErpFinBudgetScenario 状态机 + §结转规则引擎 + §承付会计` |

### 0.2 实现证据基线（实时仓库核实）

| 组件 | 路径 | 行号 | 证据 |
|------|------|------|------|
| 期间 Facade | `ErpFinAccountingPeriodBizModel` | 43-44 / 49-50 / 55-56 / 61-62 | `closePeriod`/`finalizePeriod`/`reverseClose`/`generateNextYearPeriods` 均委托 Processor |
| 期间 Processor | `ErpFinAccountingPeriodProcessor` | closePeriod:130-163 / finalizePeriod:204-210 / reverseClose:274-309 / generateNextYearPeriods:219-267 / closeAnnual:174-186 / advanceModule:400-409 / reopenModules:411-417 / isReverseCloseApprovalRequired:701-704 | 5 态迁移 + per-module 关账 + 年度分支 + 反结账 kill-switch |
| 年度结转服务 | `AnnualCloseService` | executeAnnualClose:72-80 / populateNextYearOpening:135-182 / assertAuxiliaryReconciles:191-230 | 本年利润→未分配利润 + 次年年初余额 populate + 辅助账对账门控 |
| 汇兑重估服务 | `ExchangeRevaluationService` | revalue:69-77 / revalueForSchema:79-90 | GL 关账段在期间仍 OPEN 时执行 |
| 预算方案 Processor | `ErpFinBudgetScenarioProcessor` | submit:65-73 / approve:75-84 / reject:86-94 / cancel:96-104 / rollForward:112-138 / carryForward:144-175 / validateTransition:578-593 / validateCarryForwardPreconditions:305-325 | 6 态迁移 + 滚动 + 结转 + 状态守卫 |
| 预算控制 Biz | `ErpFinBudgetControlBiz` | check:57-99 / aggregateAmount:102-137 / findMatchingBudgetLine:140-157 | 余量校验 PASS/WARNED/BLOCKED |
| 承付 Biz | `ErpFinBudgetCommitmentBizModel` | commit:54-78 / release:80-99 / releaseIfPresent:101-113 | SYNC 同事务 commit/release COMMITMENT 凭证 + 已释放守卫 |
| 过账引擎期间守卫 | `ErpFinPostingProcessor.resolveOpenPeriod` | 495-512 | 仅要求 `status==OPEN`，非 OPEN 抛 `ERR_PERIOD_CLOSED` |
| 测试覆盖 | 服务层 10 个 + 浏览器层 3 个 | 见 §6.3 | OPEN→CLOSED→CLOSED_FINAL→OPEN + 非法迁移 + 模块顺序 + 预算审批/结转/滚动/承付 + 隔离 |

### 0.3 MA1/MA2 已登记 finding 在本审计 scope 的复核列表

| Finding | 原描述 | 本审计 scope 角色 | 复核结论 |
|---------|--------|------------------|---------|
| `P1-MA2-017` | auto-post-on-close doc/code 默认值 + 语义双重偏离 | preCheck 阻断门控与 OPEN→CLOSING 前置 | **仅治理缺陷**（门控分级与 owner doc 不一致，不破坏状态机正确性） |
| `P1-MA2-018` | 年初余额 populate 非累计 | 年度结转分支（CLOSED→次年 OPEN 期间年初余额写入） | **仅治理缺陷**（数值偏差，状态机迁移路径正确） |
| `P1-MA2-019` | 辅助账对账作用域不匹配 | `assertAuxiliaryReconciles` 在年度结账迁移前的门控作用域 | **仅治理缺陷**（作用域精度问题，门控本身存在） |
| `P1-MA2-020` | 反结账 kill-switch 无审批流 | reverseClose 状态迁移门控 | **维持 P1**（见 §3.3 升级评估：CLOSED_FINAL→OPEN 唯一合法路径被 kill-switch 阻断；但 `config=false` 时路径开放，不构成 P0 死锁） |
| `P1-MA2-021` | CLOSED_FINAL 凭证锁定未实现 | 期间侧守卫（凭证 mutation 前是否校验 CLOSED_FINAL） | **维持 P1**（见 §3.3 升级评估：业务路径 post/reverse 已守卫；仅直接 entity mutation 未守卫） |
| `P1-MA2-022` | FX 重估无前期 reversal + 无期间过滤 | `ExchangeRevaluationService` 在 GL 关账段时序 | **仅治理缺陷**（时序正确——在 CLOSED 前执行；前期 reversal 缺失是数值漂移） |
| `P2-MA2-025` | CLOSING 连续 setStatus 不可见 | 期间状态机并发互斥语义 | **交接 A2.17**（见 §6.1 并发敏感点 #1） |
| `P2-MA1-019` | ErpFinVoucherTemplateBizModel:95 LocalDate.now | （顺手复核） | 与本审计 scope 无关（VoucherTemplate 不参与期间/预算状态机迁移） |

---

## 1. 期间状态机状态图与转换矩阵

### 1.1 实测状态图（代码 + dict 综合还原）

```
                  generateNextYearPeriods（次年 1 月）
                          │
                          ▼
   (初始) ──────► OPEN ◄─────── NEVER_OPENED
                  │ ▲               │
                  │ │               │ （无 action——见 §3.5 可达性 P1-MA2-033）
   closePeriod    │ │               │
   (assertPeriodStatus(OPEN)) │
                  │ │               │
                  ▼ │               │
            (CLOSING — 不可观测，P2-MA2-025)
                  │                 │
                  ▼                 │
                CLOSED ────────────┘
                  │
   finalizePeriod │ (assertPeriodStatus(CLOSED))
                  ▼
            CLOSED_FINAL
                  │
   reverseClose   │ (assertPeriodStatus(CLOSED_FINAL)
                  │  + kill-switch P1-MA2-020 + 次年门控)
                  │
                  └────► OPEN（直接，无 CLOSING 中间态）
```

### 1.2 期间状态转换矩阵

| From \ To | OPEN | CLOSING | CLOSED | NEVER_OPENED | CLOSED_FINAL |
|-----------|------|---------|--------|-------------|--------------|
| (初始) | `generateNextYearPeriods:260-261` 1 月置 OPEN；手工 seed | — | — | `generateNextYearPeriods:260-261` 2-12 月置 NEVER_OPENED | — |
| OPEN | — | `closePeriod:157`（瞬时，无 flush） | `closePeriod:158`（最终落库态） | — | — |
| CLOSING | owner doc `§2:153` 声明回退；代码**无 try-catch+setStatus(OPEN) 回退**（见 §3.4 异常路径） | — | `closePeriod:158`（瞬时） | — | — |
| CLOSED | — | — | — | — | `finalizePeriod:207` |
| NEVER_OPENED | **无 action**（仅 DB 直改可达，P1-MA2-033） | — | — | — | — |
| CLOSED_FINAL | `reverseClose:291`（kill-switch 关时；一步态，owner doc 间不一致，见 §3.10） | — | — | — | — |

### 1.3 per-module 子状态机转换矩阵

| From \ To | OPEN | CLOSING | CLOSED |
|-----------|------|---------|--------|
| OPEN | `reopenModules:411-417`（反结账） | `advanceModule:407`（瞬时） | — |
| CLOSING | — | — | `advanceModule:408`（瞬时） |
| CLOSED | `reopenModules:411-417` | — | — |

子状态机协调规则（实测）：
- 主状态 `OPEN` 时 `advanceModule` 推进 per-module 从 OPEN→CLOSING→CLOSED（连续两行，同样不可观测，**与 P2-MA2-025 同型并发缺口**，交接 A2.17）。
- 主状态 `CLOSED_FINAL` 时 per-module 应全 CLOSED（`closePeriod` 已推进完五模块 + `reopenModules` 仅在 reverseClose 调用）。
- 主状态 `OPEN`（反结账后）时 per-module 全 OPEN（`reopenModules` 重置）。

---

## 2. 预算方案状态机状态图与转换矩阵

### 2.1 实测状态图（代码 + dict 综合还原）

```
                  (初始)
                    │
                    ▼
            ┌────► DRAFT ──────► SUBMITTED ──────► APPROVED ──────► CANCELLED（终态）
            │         ▲              │                │                ▲
            │         │              │ reject          │ cancel          │
            │         │              ▼                │ （红冲 BUDGET    │
   rollForward │         │         REJECTED ──┘    │ 凭证）         │
   目标置 DRAFT │         └────────────┘              │                  │
   （来源 Scenario │      submit                   approve            │
    不在状态    │         （修改重提）              （生成 BUDGET        │
    机迁移）   │                                    影子凭证）          │
              │                                                       │
              │                                                       │
              │             carryForward（源 Scenario APPROVED +      │
              │              目标 Scenario DRAFT + 同 org/schema/      │
              │              currency；owner doc 要求源年度全 CLOSED    │
              │              但代码未校验 P1-MA2-034）                 │
              │                                                       │
              │                                          源置 CLOSED（终态）
              │                                                       │
              └───────── 目标 Scenario 增补 BudgetLine + 写结转 BUDGET 凭证
```

### 2.2 预算方案状态转换矩阵

| From \ To | DRAFT | SUBMITTED | APPROVED | REJECTED | CANCELLED | CLOSED |
|-----------|-------|-----------|----------|----------|-----------|--------|
| (初始) | 手工 seed | — | — | — | — | — |
| DRAFT | — | `submit:67-68`（`validateTransition` 允许 DRAFT/REJECTED） | — | — | — | — |
| SUBMITTED | — | — | `approve:77-78`（生成 BUDGET 凭证） | `reject:88-89` | — | — |
| APPROVED | `rollForward` 目标置 DRAFT（新实体，源不变） | — | — | — | `cancel:98-99`（红冲 BUDGET 凭证） | `carryForward:167`（源置 CLOSED） |
| REJECTED | `submit:67-68`（validateTransition 允许 REJECTED→SUBMITTED→...实际链 REJECTED→SUBMITTED→DRAFT 路径无；代码逻辑：REJECTED 经 submit 回 SUBMITTED，再无 DRAFT 回退） | — | — | — | — | — |
| CANCELLED | — | — | — | — | — | — |
| CLOSED | — | — | — | — | — | — |

### 2.3 承付凭证 commit/release 路径

| 路径 | 触发 | 预算状态机迁移 | 凭证状态机迁移 |
|------|------|--------------|--------------|
| **commit** | `ErpPurOrder.approve` / `ErpSalOrder.approve` 后置 | 独立——Scenario `docStatus` 不变（COMMITMENT 凭证并行于 BUDGET 凭证） | 生成 `postingType=COMMITMENT` 凭证（Dr 承付占用科目 / Cr 应付-承付 或 收入面对称） |
| **release-on-cancel** | `ErpPurOrder.reverseApprove/cancel` / `ErpSalOrder` 同 | 独立 | 红冲原 COMMITMENT 凭证；无原凭证抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（采购 hook 容错 try-catch 镜像 sales，见 `budget.md:367-369`） |
| **release-on-invoice-approve** | `ErpPurInvoice.approve` / `ErpSalInvoice.approve` | 独立 | 红冲原 COMMITMENT 凭证（业务规则：发票过账 = ACTUAL 占用产生 = 释放承付） |

承付 commit/release 是**预算控制平面**，与 Scenario `docStatus` 状态机**解耦**——COMMITMENT 凭证作为独立的预算占用维度参与余量聚合（`available = budget − actual − commitment`），不触发 Scenario 状态迁移。这是 owner doc 明确的设计（`budget.md:347` 协同关系表）。

---

## 3. state-machine-business-review 10 维度 + 2 项目特定维度裁决

### 3.1 维度「状态定义」 — ⚠️ FAIL（2 项 P1 + 1 项 P2）

**期间 5 态语义清晰性核验：**

| 状态 | 业务含义（等待什么） | 清晰性 | 证据 |
|------|--------------------|--------|------|
| OPEN | 等待业务记账 / 等待结账发起 | ✅ 清晰 | `closePeriod:132` 前置 OPEN；凭证过账 `resolveOpenPeriod:507` 要求 OPEN |
| CLOSING | 等待期末结账流程完成 | ⚠️ 名义存在但**运行时不可观测** | `closePeriod:157-158` 连续 `setStatus(CLOSING)` 再 `setStatus(CLOSED)` 无中间 flush，CLOSING 态永不在事务提交前对外可见（P2-MA2-025 已登记，交接 A2.17） |
| CLOSED | 等待最终复核 | ✅ 清晰 | `finalizePeriod:206` 前置 CLOSED；CLOSED 是 closePeriod 直接到达的态 |
| NEVER_OPENED | 等待运营开启 | ❌ **状态存在但无开启路径** | `generateNextYearPeriods:260-261` 次年 2-12 月置 NEVER_OPENED；**全仓库 grep 无 `openPeriod` action / NEVER_OPENED→OPEN 迁移**（P1-MA2-033，见 §3.5 可达性） |
| CLOSED_FINAL | 终态：已完成期末结账 | ✅ 清晰 | `finalizePeriod:207` 直接到达 |

**NEVER_OPENED 业务等待点歧义：**
- `IErpFinPeriodCloseBiz:52` 注释「其余月份设为 NEVER_OPENED（待自然月到达时由运营开启）」——"运营开启" 是动作还是被动等待？无 action 即为被动等待（每月初运营手工 SQL 改 DB）。
- owner doc `state-machine.md §对象二:128-133` **未列 NEVER_OPENED**（仅 4 态 CLOSED/OPEN/CLOSING/CLOSED_FINAL），与 dict 5 态漂移。
- owner doc `period-close.md §期间控制:153-158` 同样仅列 4 态（OPEN/CLOSING/CLOSED/CLOSED_FINAL），缺 NEVER_OPENED。

**预算 6 态语义清晰性核验：**

| 状态 | 业务含义 | 清晰性 | 证据 |
|------|---------|--------|------|
| DRAFT | 等待提交 | ✅ | `submit:67` |
| SUBMITTED | 等待审批 | ✅ | `approve:77` / `reject:88` |
| APPROVED | 生效参与控制（终态，可红冲或结转） | ✅ | `cancel:98` / `carryForward:167` |
| REJECTED | 等待修改重提 | ⚠️ | 见 §3.2——REJECTED→DRAFT 转换链不完整 |
| CANCELLED | 终态：作废 | ✅ | `cancel:101` |
| CLOSED | 终态：已结转 | ✅ | `carryForward:167-169` + `closedAt` 时间戳 |

**per-module 子状态机清晰性：**
- 3 态 OPEN/CLOSING/CLOSED 与主状态机协调清晰；与主状态 CLOSED_FINAL 时 per-module 应全 CLOSED 的不变量在 `reopenModules:411-417` 仅在 reverseClose 调用——主状态 CLOSED（未 FINAL）时 per-module 已全 CLOSED，与 `period-close.md §期间控制:153-158` 描述一致。

**裁决**：状态定义维度 FAIL，主要因 NEVER_OPENED 无开启路径（P1）+ owner doc 4 态 vs dict 5 态漂移（P2）。

### 3.2 维度「转换完整性」 — ⚠️ FAIL（2 项 P1 + 1 项 P2）

**期间状态机各状态出入边核验：**

- **OPEN**：入边齐全（NEVER_OPENED→OPEN 缺失见 §3.5；次年 1 月 generateNextYearPeriods 置 OPEN；CLOSED_FINAL→OPEN 反结账）。出边齐全（→CLOSING→CLOSED）。
- **CLOSING**：入边 `closePeriod:157` 瞬时。出边 `closePeriod:158` → CLOSED 瞬时。**失败回退路径缺失**（owner doc `state-machine.md §2:153` 声明 `CLOSING → OPEN`（结账失败）回退，代码 `closePeriod` 无 try-catch+setStatus(OPEN) 回退——任一结账步骤（成本核算/折旧/损益结转/汇兑重估）抛异常时，`@BizMutation` 事务回滚，期间状态回到事务开始前的 OPEN（**经事务回滚间接实现回退**，但非显式状态机回退——见 §3.4 异常路径裁决）。**裁决**：经 Nop `@BizMutation` 事务边界保证状态一致性，不构成运行时缺陷（owner doc 声明的"CLOSING→OPEN 显式回退"语义经事务回滚达到等效），维持现状即可（owner doc 可补注「回退经事务边界保证」）。
- **CLOSED**：入边 `closePeriod:158`。出边 `finalizePeriod:207` → CLOSED_FINAL。
- **NEVER_OPENED**：入边 `generateNextYearPeriods:260-261`。**无出边**（P1-MA2-033）。
- **CLOSED_FINAL**：入边 `finalizePeriod:207`。出边 `reverseClose:291` → OPEN。

**预算状态机各状态出入边核验：**

- **DRAFT**：入边 `(初始)` + `rollForward` 目标置 DRAFT（新实体）。出边 `submit:67`。
- **SUBMITTED**：入边 `submit`。出边 `approve` / `reject`。
- **APPROVED**：入边 `approve`。出边 `cancel` / `carryForward`（源置 CLOSED）。
- **REJECTED**：入边 `reject`。出边 `submit:67`（`validateTransition(SUBMITTED, DRAFT, REJECTED)` 允许 REJECTED→SUBMITTED）——**REJECTED→DRAFT 直接迁移缺失**（owner doc `budget.md §ErpFinBudgetScenario 状态机:41` 明示 `SUBMITTED → REJECTED → DRAFT`（修改重提），但代码 `submit` 从 REJECTED 直接跳到 SUBMITTED 跳过 DRAFT）。**裁决**：业务语义上"REJECTED 后修改重提"经 `submit(REJECTED→SUBMITTED)` 表达等效（无 DRAFT 中间态），不破坏控制流（提交即可重新审批），维持现状即可（owner doc 可微调表述或代码补 REJECTED→DRAFT 中间态——属低优先级治理，归 P2）。
- **CANCELLED**：入边 `cancel`。无出边（终态）。
- **CLOSED**：入边 `carryForward`。无出边（终态，owner doc `period-close.md:324` 明示"已结转的源 Scenario status=CLOSED 不回退"）。

**承付 commit/release 路径完整性：** 见 §2.3——commit/release 是独立凭证状态机迁移，与 Scenario 状态机解耦，3 接入点齐全（commit / release-on-cancel / release-on-invoice-approve），release 守卫 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 存在（`ErpFinBudgetCommitmentBizModel:88-92`），采购 hook 容错对称性已 fix（plan 2026-07-26-0410-2）。

**裁决**：转换完整性维度 FAIL，主要因 NEVER_OPENED 无出边（P1）+ carryForward 不校验源年度全 CLOSED（P1-MA2-034）+ REJECTED→DRAFT 直迁缺失（P2）。

### 3.3 维度「终端状态和恢复」 — ✅ PASS（升级评估裁决：P1-MA2-020/021 维持 P1）

**期间状态机终端：**

- **CLOSED_FINAL**（反结账可恢复）：
  - 恢复路径 `reverseClose:291` CLOSED_FINAL→OPEN（kill-switch 关时）。
  - **P1-MA2-020 kill-switch 升级评估**：默认 `reverse-close-approval-required=true` 时 `reverseClose:278-281` 直接 throw `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`，反结账完全不可用——CLOSED_FINAL 在默认 config 下成**事实终态**。这是否构成 P0？
    - **不构成 P0 的理由**：(i) config `reverse-close-approval-required=false` 时合法恢复路径开放（虽无审批流，但路径存在）；(ii) owner doc `state-machine.md §6` 明示"反结账需管理员+审批"，kill-switch 是"审批流 successor 前的保守阻断"，非状态机破缺；(iii) 默认 config=true 是保护性默认（防误操作），非业务死锁——运维或管理员显式置 false 即可恢复。
    - **裁决**：维持 P1（owner doc 承诺审批流，实现为 kill-switch——属"承诺但未实现"治理缺陷，非运行时死锁）。
  - **P1-MA2-021 期间侧 CLOSED_FINAL 凭证锁定升级评估**：业务路径 `ErpFinPostingProcessor.resolveOpenPeriod:507` 已守卫（凭证过账/红冲经 `post`/`reverse` action 调用 resolveOpenPeriod，要求 `status==OPEN`，CLOSED_FINAL 期间凭证无法新增/红冲）。这是否构成 P0？
    - **不构成 P0 的理由**：(i) 业务自动过账路径（业务单据审核触发）已守卫；(ii) 财务员手工 `post`/`reverse` action 已守卫；(iii) 仅直接 entity mutation（CrudBizModel 默认 update/delete）未守卫——但这些操作不经过 resolveOpenPeriod，且权限受 `enableActionAuth` 控制（生产环境启用时需财务员角色）；(iv) 影响范围有限（不破坏总账平衡——凭证已 POSTED 参与汇总，直接 entity mutation 仅改字段值，红冲需经 `reverse` action）。
    - **裁决**：维持 P1（业务路径已守卫，直接 entity mutation 未守卫——属"间接保证"治理缺陷，非全面凭证锁定破缺）。

**预算状态机终端：**

- **CANCELLED**（不可恢复）：`cancel:101` 终态，红冲原 BUDGET 凭证（`reverseBudgetVoucher:573-576` 经 `BudgetVoucherGenerator.reverse:79` 反查 `billCode=scenario.code` 全部 BUDGET 凭证并红冲）。归档与活动期间可区分（`docStatus` 字段）。
- **CLOSED**（不可恢复，反结账不回退）：owner doc `period-close.md:324` 明示"已结转的源 Scenario status=CLOSED 不回退"，代码 `carryForward:167` 置 CLOSED 后无任何 mutation 出边——反结账源年度期间（reverseClose）不影响已 CLOSED 的 Scenario。**裁决**：终态语义清晰，符合"已结转数据不可改"业务规则。

### 3.4 维度「异常路径」 — ✅ PASS（结账失败经事务回滚保证一致性）

**结账失败路径核验：**
- `closePeriod:130-163` 编排：assertPeriodStatus(OPEN) → preCheck 阻断 → advanceModule(AR/AP) → closeInvModule（recloseInvCosts 失败 try-catch 告警不阻断，`recloseInvCosts:375-377`）→ closeAssetModule（runDepreciation 失败 try-catch 告警不阻断，`runDepreciation:353-356`）→ closeGlModule（revalue + profitLossClosingService.close 无 try-catch，失败抛异常）→ 年度分支（assertAuxiliaryReconciles 失败抛异常；executeAnnualClose 失败抛异常）→ setStatus(CLOSING) → setStatus(CLOSED) → flushSession。
- **任一步骤抛 NopException 时**：`@BizMutation`（Facade `closePeriod:42-44`）事务回滚，期间状态 setStatus(CLOSING)/setStatus(CLOSED) 不落库（事务回滚到事务开始前的 OPEN）——**经事务边界保证状态一致性**。
- **CLOSING 悬挂风险评估**：因 CLOSING setStatus 与 CLOSED setStatus 在同一事务同一 flushSession 内（line 157-158 + 161），无悬挂窗口——事务要么全提交（CLOSED），要么全回滚（OPEN）。CLOSING 态永不在事务外可见（与 P2-MA2-025 同型并发缺口，交接 A2.17）。
- **裁决**：结账失败经事务回滚保证一致性，不构成运行时缺陷。owner doc `state-machine.md §2:153` 声明的"CLOSING→OPEN 显式回退"语义经事务边界等效实现，建议 owner doc 补注。

**反结账失败路径核验：**
- `reverseClose:274-309`：assertPeriodStatus(CLOSED_FINAL) → kill-switch → 次年门控 → setStatus(OPEN) → 红冲本期凭证（PL/FX/ANNUAL 三 prefix + 条件折旧）→ reopenModules → flushSession。
- 任一红冲步骤失败抛异常时事务回滚，期间状态回到 CLOSED_FINAL（事务回滚到事务开始前）。
- 红冲容错：`reverseCloseVoucher:606-615` 在 `ErpFinVoucherBillR` 反查为空时直接 return（无凭证可红冲不抛错），`reverseDepreciation:381-396` try-catch 告警不阻断。
- **裁决**：反结账失败经事务回滚保证一致性，红冲容错齐全。

**并发结账：** 见 §6.1 并发敏感点 #1（P2-MA2-025，交接 A2.17）。

**预算 HARD 阻断：** `ErpFinBudgetControlBiz.check:86-89` HARD 控制下余量<0 抛 `ERR_BUDGET_EXCEEDED`，单据审核事务回滚，预算 Scenario 状态不变。**裁决**：HARD 阻断经事务回滚保证 Scenario 状态一致性。

**预算 commitment release 重复触发：** `ErpFinBudgetCommitmentBizModel.release:88-92` 守卫 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`，采购 hook `runCommitmentReleaseHook` try-catch 容错（plan 2026-07-26-0410-2 fix）。**裁决**：幂等性守卫齐全。

**幂等性：** `generateNextYearPeriods:229-233` 同年期间已存在抛 `ERR_PERIODS_ALREADY_EXIST`；config `period-generate-skip-existing=true` 时仅补缺失月份（`existingMonths.contains(month)` 跳过）。**裁决**：幂等策略齐全。

### 3.5 维度「可达性」 — ❌ FAIL（P1-MA2-033 NEVER_OPENED→OPEN 迁移路径缺失）

**期间状态机可达性核验：**

- **OPEN**：从 (初始) 经 `generateNextYearPeriods` 1 月置 OPEN 可达；从 CLOSED_FINAL 反结账可达。
- **CLOSING**：从 OPEN 经 `closePeriod` 可达（瞬时不可观测）。
- **CLOSED**：从 CLOSING 经 `closePeriod` 可达。
- **CLOSED_FINAL**：从 CLOSED 经 `finalizePeriod` 可达。
- **NEVER_OPENED**：从 (初始) 经 `generateNextYearPeriods` 2-12 月可达。
  - **NEVER_OPENED→OPEN 迁移**：**全仓库 grep `openPeriod` / `NEVER_OPENED.*OPEN` 无任何 mutation 实现**。`IErpFinPeriodCloseBiz:52` 注释「待自然月到达时由运营开启」承诺有 action，但代码无。次年 2-12 月期间陷入可达性死锁——只能经手工 DB update 改 status=OPEN 才能进入记账。
  - **裁决**：**P1-MA2-033**（新 finding）——NEVER_OPENED 状态存在但无合法迁移路径至 OPEN，运营无法经系统 action 开启次月期间，违反 owner doc `IErpFinPeriodCloseBiz:52` 承诺。修复方式：MR1 裁决——方案 A（推荐）补 `openPeriod(periodId)` @BizMutation action（NEVER_OPENED→OPEN 守卫 + 角色权限）；方案 B owner doc 标注「NEVER_OPENED 仅标记，运营经 DB 直改或重新生成次年期间」为已知简化。

**预算状态机可达性核验：**

- 全部 6 态从 (初始) 经主路径可达；CLOSED 经 carryForward 可达。无死循环或不可达终态。

### 3.6 维度「角色和权限」 — ⚠️ FAIL（P1-MA2-020 维持）

**期间状态机角色绑定核验：**

| 迁移 | owner doc 角色 | 代码实现 | 裁决 |
|------|--------------|---------|------|
| closePeriod (OPEN→CLOSED) | 财务员 | `@BizMutation` 无显式 `@BizAuth`，依赖 CrudBizModel 默认 + `enableActionAuth` 配置 | ⚠️ 无显式角色绑定，依赖配置层 |
| finalizePeriod (CLOSED→CLOSED_FINAL) | 财务员 | 同上 | ⚠️ |
| reverseClose (CLOSED_FINAL→OPEN) | **管理员 + 审批** | kill-switch `isReverseCloseApprovalRequired()` 默认 true 阻断；config=false 时**无角色绑定也无审批流** | ❌ **P1-MA2-020 维持**（kill-switch 无审批流，config=false 时任何能调 mutation 的角色均可反结账） |
| generateNextYearPeriods | 财务员 | 同 closePeriod | ⚠️ |

**预算状态机角色绑定核验：**

| 迁移 | owner doc 角色 | 代码实现 | 裁决 |
|------|--------------|---------|------|
| submit / approve / reject | 财务员 / 财务管理员 | `@BizMutation` 无显式 `@BizAuth`，依赖默认 | ⚠️ |
| cancel (APPROVED→CANCELLED) | 财务员（红冲 BUDGET 凭证需权限） | 同上 | ⚠️ |
| carryForward / rollForward | 财务管理员 | 同上 | ⚠️ |

**危险操作：**
- **反结账**：owner doc `state-machine.md §6` 明示"需管理员+审批，因影响已出具报表与税务申报"，实现为 kill-switch 无审批流（P1-MA2-020 维持）。
- **HARD 预算阻断**：阻断业务单据审核，业务正确性已保证（事务回滚），无角色漏洞。
- **预算 CLOSED 终态**：经 carryForward 唯一可达，无可绕过路径。

**裁决**：角色权限维度 FAIL，主要因反结账 kill-switch（P1-MA2-020）+ 全部 mutation 缺显式 `@BizAuth`（依赖配置层 `enableActionAuth`，属平台默认模式，不构成本审计 finding——归 A2.18 权限注解审计）。

### 3.7 维度「外部依赖」 — ✅ PASS

**期间状态机与凭证状态机耦合：**
- 凭证过账需期间 OPEN：`ErpFinPostingProcessor.resolveOpenPeriod:507` 守卫 ✅。
- 期间结账需凭证已过账：`preCheck:97` 扫描未过账凭证（`findUnpostedVoucherCodes:565-573`），config `auto-post-on-close=false` 时阻断（`closePeriod:135-139`）。
- **裁决**：耦合约束齐全。

**预算状态机与凭证状态机耦合：**
- 预算 APPROVED 生成 BUDGET 凭证：`approve:79` 调 `generateBudgetVoucher:562-570` → `BudgetVoucherGenerator.generate:53-73` 按 periodId 分组生成。
- COMMITMENT 凭证 commit/release：见 §2.3，独立凭证状态机。
- **裁决**：耦合约束齐全。

**折旧/成本/汇兑外部步骤时序：**
- `closePeriod` 编排顺序：AR→AP→INV（recloseInvCosts）→AST（runDepreciation）→GL（revalue + profitLossClosingService.close）→ 年度分支 → setStatus(CLOSING/CLOSED)。
- 所有外部步骤均在期间仍 OPEN 时执行（setStatus 在最后），状态簿记在期末凭证生成完成后。
- **裁决**：时序正确（owner doc `period-close.md §期末结账步骤` 步骤 1-7 顺序对齐）。

### 3.8 维度「TODO/任务策略」 — ⚠️ FAIL（NEVER_OPENED 静默下沉）

**期间状态机各状态 TODO 策略：**

| 状态 | owner doc TODO | 实现 | 裁决 |
|------|---------------|------|------|
| OPEN（月末） | assigned（财务员）—— 月末待结账提醒（`closing-reminder-days` 配置） | config 存在（`period-close.md:288`），无显式 TODO 派发 action | ⚠️ TODO 派发未实现（属运营成熟度层，归 M5 通知派发子系统 successor） |
| CLOSING（失败回退） | assigned（财务员）—— 结账失败待处理 | 经事务回滚回 OPEN，无 CLOSING 悬挂，无 TODO 产生 | ✅ 无悬挂即无 TODO 需求 |
| CLOSED_FINAL | 无 | 无 | ✅ |
| NEVER_OPENED | （owner doc 未提及） | **无 TODO 派发**——次年 2-12 月期间静默不可用，运营无系统提示需开启 | ❌ **P1-MA2-033 关联**（NEVER_OPENED 无 action + 无 TODO = 静默下沉） |

**预算状态机各状态 TODO 策略：**

| 状态 | TODO | 实现 | 裁决 |
|------|------|------|------|
| DRAFT（手工创建） | assigned（财务管理员）—— 待提交 | 无显式 TODO | ⚠️ 同上 |
| SUBMITTED | assigned（财务管理员）—— 待审批 | 无显式 TODO | ⚠️ |
| HARD 阻断单据悬挂 | assigned（业务员）—— 待调整预算或申请追加 | `ErpFinBudgetControlLog` 审计日志写入，无 TODO 派发 | ⚠️ 审计有据，无主动通知 |

**裁决**：TODO 维度 FAIL，主要因 NEVER_OPENED 静默下沉（P1-MA2-033 关联）；其余 TODO 派发缺失属运营成熟度层（M5 通知派发子系统已落地 notify 域，跨域接线归 successor）。

### 3.9 维度「场景演练（最重要）」 — ✅ PASS（9 场景全部经证据确认）

#### 场景 A：月末结账快乐路径（OPEN→CLOSED→CLOSED_FINAL）

1. 6 月末，财务员确认所有业务单据已审核、`posted=true`。
2. `preCheck`（`@BizQuery`）—— 返回结构化报告（unposted/unsettled/postingException/allowance），不阻断。
3. `closePeriod`（`@BizMutation`）—— assertPeriodStatus(OPEN) → preCheck 阻断（config=false 时）→ AR→AP→INV→AST→GL 模块关账 → setStatus(CLOSING)/setStatus(CLOSED) → flushSession。
4. `finalizePeriod`（`@BizMutation`）—— assertPeriodStatus(CLOSED) → setStatus(CLOSED_FINAL) → flushSession。
5. **证据**：`TestErpFinPeriodStateMachine.testForwardAndReverse:47-68` 覆盖 OPEN→CLOSED→CLOSED_FINAL 全链。

#### 场景 B：结账失败（成本异常——状态回退 OPEN）

1. 6 月结账 closePeriod，`closeInvModule` 内 recloseInvCosts 失败（inventory impl 未就绪）。
2. `recloseInvCosts:375-377` try-catch 告警不阻断（设计：单域 finance 测试无 inv-service 时跳过）。
3. 若 GL 段 `profitLossClosingService.close` 抛 NopException（如科目未配置）→ `@BizMutation` 事务回滚 → 期间状态回 OPEN。
4. **裁决**：经事务回滚保证状态一致性，无 CLOSING 悬挂。
5. **证据**：`TestErpFinPeriodPreCheck` 覆盖 preCheck 阻断模式；事务回滚经 `@BizMutation` 平台契约保证。

#### 场景 C：反结账调整（CLOSED_FINAL→kill-switch throw [默认] / CLOSED_FINAL→OPEN [config=false]）

1. 6 月已 CLOSED_FINAL，7 月发现 6 月某凭证错误。
2. 默认 config `reverse-close-approval-required=true` → `reverseClose:278-281` throw `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`，反结账不可用（P1-MA2-020）。
3. config=false 时 → `reverseClose:291` setStatus(OPEN) → 红冲 PL/FX 凭证 → reopenModules → flushSession。
4. 财务员红冲错误凭证 + 新正确凭证 → 重新 closePeriod → 重新 finalizePeriod。
5. **证据**：`TestErpFinReverseClose` + `TestErpFinPeriodStateMachine.testForwardAndReverse:62-67` 覆盖反结账 + 重新结账。

#### 场景 D：年度结转（12 月 CLOSED→次年期间创建→年初余额 populate）

1. 12 月末，财务员 closePeriod。
2. `closePeriod:152-154` 检测 `isYearEnd(period)` 且 `isAnnualCloseEnabled()` → 调 `closeAnnual:174-186`。
3. `closeAnnual:177-179` 辅助账对账门控（config-gated）→ `assertAuxiliaryReconciles`。
4. `closeAnnual:181-183` 次年期间创建（config-gated）→ `generateNextYearPeriods(year+1)` 1 月 OPEN / 2-12 月 NEVER_OPENED。
5. `closeAnnual:185` 年度结转 + 年初余额 populate → `executeAnnualClose` 本年利润→未分配利润 + populateNextYearOpening。
6. **证据**：`TestErpFinAnnualClose`（含 `testAnnualCloseTransferProfitToRetainedEarnings` + `testReverseCloseBlockedWhenNextYearExists` + `testReverseCloseReversesAnnualVoucherWhenNoNextYear`）+ `TestErpFinAuxiliaryReconGate`。

#### 场景 E：次年期间开启（次年 2 月 NEVER_OPENED→OPEN——**无 action**）

1. 次年 2 月初，运营需开启 2 月期间。
2. **全仓库 grep 无 `openPeriod` action**——NEVER_OPENED→OPEN 迁移路径缺失（P1-MA2-033）。
3. 当前唯一路径：手工 DB update `UPDATE erp_fin_accounting_period SET status='OPEN' WHERE code='2027-02'`。
4. **裁决**：场景 E 不可经系统 action 完成，违反 owner doc `IErpFinPeriodCloseBiz:52` 承诺。

#### 场景 F：预算审批（DRAFT→SUBMITTED→APPROVED→BUDGET 凭证）

1. 财务管理员创建 Scenario（DRAFT）+ BudgetLine。
2. `submit`（DRAFT→SUBMITTED）。
3. `approve`（SUBMITTED→APPROVED）→ `generateBudgetVoucher` 按 periodId 分组生成 BUDGET 凭证（postingType=BUDGET，借贷按 subject.direction）。
4. BUDGET 凭证参与预算控制（`ErpFinBudgetControlBiz.check` 聚合 BUDGET 凭证为 budgetBalance）。
5. **证据**：`TestErpFinBudgetEndToEnd` + `TestErpFinBudgetIsolation`（BUDGET vs ACTUAL 隔离）。

#### 场景 G：预算结转（源 APPROVED + 目标 DRAFT + 年度 CLOSED→源 CLOSED）

1. 财务管理员创建源 Scenario（APPROVED）+ 目标 Scenario（DRAFT）+ BudgetLine。
2. `carryForward`（源 APPROVED + 目标 DRAFT + 同 org/schema/currency）→ `validateCarryForwardPreconditions:305-325` 校验。
3. **owner doc `budget.md:209` 要求"源 Scenario 所在年度的所有会计期间必须 CLOSED"硬前置，代码 `validateCarryForwardPreconditions` 未校验**（P1-MA2-034）。
4. 计算结转金额（按 rule）→ 增补目标 BudgetLine → 写结转 BUDGET 凭证 → 源置 CLOSED + closedAt → 写 CarryForwardLog。
5. **证据**：`TestErpFinBudgetCarryForward`（4 规则各 1 测试，但 seed 用 OPEN 期间，未验证 CLOSED 前置）。

#### 场景 H：预算滚动（源 APPROVED→目标 DRAFT）

1. `rollForward`（源 APPROVED）→ 创建目标 Scenario（DRAFT，parentScenarioId=源 id，budgetGroupCode 继承）→ 按 strategy 复制 BudgetLine（periodId 重映射 + 金额调整）→ 写 RollforwardLog。
2. **证据**：`TestErpFinBudgetRollForward`（3 策略各 1 测试）。

#### 场景 I：承付 commit/release（订单 approve→commit / 订单 cancel 或发票 approve→release）

1. 采购订单 approve → `IErpFinBudgetCommitmentBiz.commit(PURCHASE_ORDER, orderCode, ...)` → 生成 COMMITMENT 凭证（Dr 承付占用 / Cr 应付-承付）。
2. 采购订单 reverseApprove/cancel → `release(PURCHASE_ORDER, orderCode)` → 红冲原 COMMITMENT 凭证；hook 容错 try-catch（重复 release 静默跳过）。
3. 采购发票 approve（AP 发票过账 = ACTUAL 占用产生）→ `release(PURCHASE_ORDER, orderCode)` → 红冲原 COMMITMENT 凭证。
4. 销售镜像：billType SALES_ORDER_COMMITMENT 派发。
5. **证据**：`TestErpFinBudgetCommitment` + 浏览器层 `fin-commitment-accounting.action.spec.ts`（4 用例 + helper 扩展）。

**裁决**：场景演练维度 PASS（9 场景全部经证据确认；场景 E 暴露 P1-MA2-033，场景 G 暴露 P1-MA2-034——发现即登记）。

### 3.10 维度「与设计文档一致性」 — ❌ FAIL（4 处 owner doc 漂移）

**期间状态机 owner doc 漂移：**

| # | 漂移 | owner doc 表述 | 代码/dict 实测 | 严重性 |
|---|------|--------------|--------------|--------|
| 1 | **NEVER_OPENED 缺失** | `state-machine.md §对象二:128-133` 仅 4 态（CLOSED/OPEN/CLOSING/CLOSED_FINAL）；`period-close.md §期间控制:153-158` 同 4 态 | dict `erp-fin/period-status` 5 态（含 NEVER_OPENED）；`generateNextYearPeriods:260-261` 实际置 NEVER_OPENED | P2（owner doc 漏更新，无运行时影响——NEVER_OPENED 状态本身在 dict 已定义） |
| 2 | **CLOSED 中间态 + finalizePeriod 独立步骤** | `state-machine.md §对象二 §2 迁移图:142` `CLOSING → CLOSED_FINAL`（结账成功）直接迁移，无 CLOSED 中间态 | 代码 `closePeriod` 到 CLOSED + 独立 `finalizePeriod` CLOSED→CLOSED_FINAL；owner doc `period-close.md §期间控制:153-158` 实际已文档化 CLOSED 中间态（与 state-machine.md 不一致） | P2（owner doc 间不一致——period-close.md 正确，state-machine.md 过时） |
| 3 | **反结账三态 vs 一步态** | `period-close.md §反结账步骤2:186` `CLOSED_FINAL → CLOSING → OPEN` 三态；`state-machine.md §2/§3:153/182/222` `CLOSED_FINAL → OPEN` 一步态 | 代码 `reverseClose:291` 直接 `setStatus(OPEN)` 一步态（与 state-machine.md 一致，与 period-close.md 不一致） | P2（owner doc 间不一致——代码与 state-machine.md 一致；裁决：以代码 + state-machine.md 一步态为准，period-close.md §反结账步骤2 描述需更正） |

**预算状态机 owner doc 漂移：**

| # | 漂移 | owner doc 表述 | 代码/dict 实测 | 严重性 |
|---|------|--------------|--------------|--------|
| 4 | **state-machine.md 无预算独立章节** | `state-machine.md` 仅覆盖会计凭证 + 会计期间两类状态机，无预算方案状态机独立章节（散落在 `budget.md:41/223/331`） | 代码 6 态 + 状态迁移齐全；`budget.md §ErpFinBudgetScenario 状态机:41` 实际已有状态机描述（仅未在 state-machine.md 集中） | P2（文档组织问题，无运行时影响） |
| 5 | **carryForward 源年度全 CLOSED 前置未实现** | `budget.md §结转算法:209` + `period-close.md §预算结转与期间状态机:322` 明示"源 Scenario 所在年度的所有会计期间必须 CLOSED（glStatus=CLOSED）——年度已结账是结转的硬前置" | 代码 `validateCarryForwardPreconditions:305-325` 仅校验 source APPROVED + target DRAFT + 同 org/schema/currency，**未校验源年度期间 CLOSED** | **P1-MA2-034**（owner doc 承诺硬前置，实现未落实——结转可在年度未结账时执行，违反业务规则） |
| 6 | **budget.md:78 预算审批即过账** | `budget.md §业务规则1:78` "预算方案审批即过账" | 代码 `approve:79` 调 `generateBudgetVoucher` 生成 BUDGET 凭证——实现与 owner doc 一致 | ✅ 无漂移 |

**裁决**：与设计文档一致性维度 FAIL，主要因 P1-MA2-034（carryForward 期间状态机前置未实现）+ 4 处 P2 owner doc 漂移。

### 3.11 维度「期间状态机并发互斥（项目特定，P2-MA2-025 复核）」 — ⚠️ FAIL（交接 A2.17）

**核验：**
- `closePeriod:157-158` 连续 setStatus(CLOSING) 再 setStatus(CLOSED) 无中间 flush——CLOSING 态运行时不可观测，并发结账同期间两事务均读到 OPEN 均进入结账。
- **乐观锁保护**：`ErpFinAccountingPeriod` ORM 实体含 `version` 字段（`app-erp-finance.orm.xml:669` `versionProp="version"`）——Nop ORM 在事务提交时检查 version，第二个事务提交时若 version 已变会抛乐观锁异常。**故并发结账的实际互斥经乐观锁保证**，非经 CLOSING 态。
- 但 owner doc `state-machine.md §对象二 §4 异常路径:168` 明示"并发结账（多人同时发起）→ 乐观锁，后者失败"——**owner doc 已预期乐观锁机制**，CLOSING 态的并发互斥语义是设计冗余。
- **裁决**：CLOSING 态运行时不可观测是设计简化（经乐观锁等效保护），不构成运行时缺陷。维持 P2-MA2-025 watch-only，交接 A2.17 系统性并发审计。

**finalizePeriod / reverseClose 同型并发风险：** 同样经 `version` 乐观锁保护。交接 A2.17。

**per-module 关账并发：** `advanceModule:400-409` 同样连续 setStatus 无 flush——但 per-module 关账在主 closePeriod 事务内同步执行，无独立并发入口。交接 A2.17。

**预算状态机并发（同 Scenario 并行 submit/approve）：** `ErpFinBudgetScenario` 含 `version` 字段，经乐观锁保护。交接 A2.17。

### 3.12 维度「预算承付 commit/release 状态机（项目特定）」 — ✅ PASS

**核验：**
- commit/release 是独立凭证状态机迁移，与 Scenario `docStatus` 状态机解耦（设计明确，`budget.md:347` 协同关系表）。
- 3 接入点齐全（commit / release-on-cancel / release-on-invoice-approve），billType 派发泛化（PURCHASE_ORDER_COMMITMENT / SALES_ORDER_COMMITMENT，`budget.md:316-318`）。
- release 守卫 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 存在（`ErpFinBudgetCommitmentBizModel:88-92`）。
- 采购 hook `runCommitmentReleaseHook` 容错对称性已 fix（plan 2026-07-26-0410-2，镜像 sales `ErpSalOrderProcessor.runCommitmentReleaseHook:359-370` try-catch 范式）。
- release-on-receive-vs-invoice 业务规则正确（`budget.md:260` 在 invoice approve 释放非 receive approve——receive 是库存移动不产生 AP ACTUAL）。
- **裁决**：commit/release 状态机维度 PASS，迁移路径完整，容错对称性已保证。

---

## 4. 9 个已识别控制点 PASS/FAIL 汇总

| # | 控制点 | 裁决 | 关键发现 |
|---|--------|------|---------|
| 1 | 状态定义清晰性 | ❌ FAIL | NEVER_OPENED 无开启路径（P1-MA2-033）；CLOSING 运行时不可观测（P2-MA2-025 交接 A2.17） |
| 2 | 转换完整性 | ❌ FAIL | NEVER_OPENED 无出边（P1-MA2-033）；carryForward 不校验源年度全 CLOSED（P1-MA2-034）；REJECTED→DRAFT 直迁缺失（P2） |
| 3 | 终端状态和恢复 | ✅ PASS | CLOSED_FINAL 反结账可恢复（kill-switch config=false 时）；预算 CLOSED/CANCELLED 终态语义清晰；P1-MA2-020/021 维持 P1 不升 P0 |
| 4 | 异常路径 | ✅ PASS | 结账失败经 @BizMutation 事务回滚保证一致性；红冲容错齐全；幂等策略齐全 |
| 5 | 可达性 | ❌ FAIL | NEVER_OPENED 不可经系统 action 到 OPEN（P1-MA2-033） |
| 6 | 角色和权限 | ⚠️ FAIL | 反结账 kill-switch 无审批流（P1-MA2-020 维持）；其余 mutation 无显式 @BizAuth（归 A2.18） |
| 7 | 外部依赖 | ✅ PASS | 凭证/预算凭证与期间耦合约束齐全；折旧/成本/汇兑外部步骤时序正确（在 CLOSED 前执行） |
| 8 | TODO/任务策略 | ⚠️ FAIL | NEVER_OPENED 静默下沉（P1-MA2-033 关联）；其余 TODO 派发缺失归 M5 successor |
| 9 | 场景演练 | ✅ PASS | 9 场景经证据确认；场景 E/G 暴露新 P1 |

---

## 5. MA2 finding 运行时影响复核结论

| Finding | 原严重性 | 本审计 scope 角色运行时影响复核 | 终态裁决 |
|---------|---------|------------------------------|---------|
| `P1-MA2-017` | P1 | preCheck 阻断门控与 OPEN→CLOSING 前置——门控分级与 owner doc 不一致，不破坏状态机正确性（OPEN→CLOSING 迁移本身正确，仅门控阻断/提示行为偏离） | **仅治理缺陷**，维持 P1 |
| `P1-MA2-018` | P1 | 年度结转 CLOSED→次年 OPEN 期间年初余额写入——数值偏差（非累计），状态机迁移路径正确 | **仅治理缺陷**，维持 P1 |
| `P1-MA2-019` | P1 | assertAuxiliaryReconciles 在 CLOSED 迁移前门控——作用域精度问题（全历史 vs 本年），门控本身存在 | **仅治理缺陷**，维持 P1 |
| `P1-MA2-020` | P1 | reverseClose 状态迁移门控——**升级评估**：CLOSED_FINAL→OPEN 唯一合法路径被 kill-switch 阻断；config=false 时路径开放，不构成 P0 死锁 | **维持 P1 不升 P0**（config 路径开放 + owner doc 承诺审批流 successor） |
| `P1-MA2-021` | P1 | 期间侧 CLOSED_FINAL 凭证锁定——**升级评估**：业务路径 post/reverse 已守卫（resolveOpenPeriod:507）；仅直接 entity mutation 未守卫 | **维持 P1 不升 P0**（业务路径守卫 + 直接 mutation 受权限保护） |
| `P1-MA2-022` | P1 | ExchangeRevaluation 在 GL 关账段时序——时序正确（在 CLOSED 前执行）；前期 reversal 缺失是数值漂移 | **仅治理缺陷**，维持 P1 |
| `P2-MA2-025` | P2 | CLOSING 连续 setStatus 不可见——并发互斥经乐观锁等效保护 | **交接 A2.17**，维持 P2 |

---

## 6. 残留风险与并发敏感点

### 6.1 并发敏感点（交接 A2.17）

1. **#1 期间状态机并发互斥（P2-MA2-025 复核）**：`closePeriod:157-158` 连续 setStatus 无 flush 致 CLOSING 不可观测——并发结账同期间经 `ErpFinAccountingPeriod.version` 乐观锁保护，后者失败。owner doc 已预期乐观锁机制。**交接 A2.17**。
2. **#2 finalizePeriod / reverseClose 并发**：同型经 version 乐观锁保护。**交接 A2.17**。
3. **#3 per-module 关账并发**：`advanceModule:400-409` 同型连续 setStatus 无 flush，但 per-module 关账在主 closePeriod 事务内同步执行无独立并发入口。**交接 A2.17**。
4. **#4 预算状态机并发（同 Scenario 并行 submit/approve）**：经 `ErpFinBudgetScenario.version` 乐观锁保护。**交接 A2.17**。
5. **#5 预算 HARD 控制竞态**：`ErpFinBudgetControlBiz.check` 余量校验与业务单据审核同事务，余量读取与占用在同一事务内串行——无竞态窗口。但若两个业务单据并发审核同维度，两事务均读到余量充足均放行，第二个提交时虽 version 不冲突（ErpFinBudgetScenario 未改），但实际占用超额。**交接 A2.17**（预算控制竞态需独立的悲观/乐观锁机制，归 A2.17 scope）。

### 6.2 新发现 P1（待 MR1）

| Finding ID | 描述 | 严重性 | 修复方式 |
|-----------|------|--------|---------|
| `P1-MA2-033` | **NEVER_OPENED→OPEN 迁移路径缺失**：`generateNextYearPeriods:260-261` 次年 2-12 月置 NEVER_OPENED，全仓库无 `openPeriod` action 或任何 NEVER_OPENED→OPEN 迁移实现。次年 2-12 月期间陷入可达性死锁，运营只能经手工 DB update 开启。违反 `IErpFinPeriodCloseBiz:52` 注释承诺「待自然月到达时由运营开启」+ owner doc `state-machine.md §对象二`（虽未列 NEVER_OPENED 但 dict 含）。 | P1（major） | MR1 裁决——方案 A（推荐）补 `@BizMutation openPeriod(periodId)` action（NEVER_OPENED→OPEN 守卫 + 角色权限 + 二次确认）；方案 B owner doc 标注「NEVER_OPENED 仅标记，运营经 DB 直改或重新生成次年期间」为已知简化 |
| `P1-MA2-034` | **carryForward 不校验源年度全 CLOSED 前置**：owner doc `budget.md §结转算法:209` + `period-close.md §预算结转与期间状态机:322` 明示"源 Scenario 所在年度的所有会计期间必须 CLOSED（glStatus=CLOSED）——年度已结账是结转的硬前置"，代码 `ErpFinBudgetScenarioProcessor.validateCarryForwardPreconditions:305-325` 仅校验 source APPROVED + target DRAFT + 同 org/schema/currency，**未校验源年度期间 CLOSED**。结转可在年度未结账时执行，违反业务规则（owner doc 显式 hard precondition）。 | P1（major） | MR1 在 `validateCarryForwardPreconditions` 增源年度期间 CLOSED 校验（按 source.fiscalYear 查全部期间 glStatus==CLOSED，未全 CLOSED 抛 `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID` 或新增 `ERP_FIN_BUDGET_CARRY_FORWARD_PERIOD_NOT_CLOSED`）+ 补"源年度未结账时 carryForward 拒绝"测试 |

### 6.3 新发现 P2（watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA2-034` | **期间状态机 owner doc 4 处漂移**：(1) `state-machine.md §对象二` 4 态 vs dict 5 态（NEVER_OPENED 缺失）；(2) `state-machine.md §2 迁移图` CLOSING→CLOSED_FINAL 无 CLOSED 中间态 vs 代码 CLOSED 中间态 + finalizePeriod（period-close.md 已正确，state-machine.md 过时）；(3) `period-close.md §反结账步骤2:186` CLOSED_FINAL→CLOSING→OPEN 三态 vs 代码 + state-machine.md 一步态（裁决以代码 + state-machine.md 为准，period-close.md 需更正）；(4) `state-machine.md` 无预算方案状态机独立章节（散落在 budget.md）。 | watch-only，MR1 顺手更新 owner doc：(a) state-machine.md §对象二 状态表追加 NEVER_OPENED 行 + 迁移图补 CLOSED 中间态 + finalizePeriod 步骤；(b) period-close.md §反结账步骤2 改为 CLOSED_FINAL→OPEN 一步态；(c) state-machine.md 新增"对象三：预算方案状态机"章节或链接到 budget.md |
| `P2-MA2-035` | **REJECTED→DRAFT 直迁缺失**：owner doc `budget.md §ErpFinBudgetScenario 状态机:41` 明示 `SUBMITTED → REJECTED → DRAFT`（修改重提），代码 `submit:67-68` 从 REJECTED 直接跳到 SUBMITTED 跳过 DRAFT。业务语义经 submit(REJECTED→SUBMITTED) 表达等效，不破坏控制流。 | watch-only，MR1 顺手——方案 A 补 REJECTED→DRAFT 中间态 + DRAFT→SUBMITTED 两步迁移；方案 B owner doc 微调表述为「REJECTED→SUBMITTED（修改重提）」 |

### 6.4 测试覆盖残留风险

- **结账失败事务回滚无显式测试**：场景 B（结账失败 → 事务回滚 → 期间回 OPEN）经 `@BizMutation` 平台契约保证，但无显式测试断言"closePeriod 抛异常后 period.status 仍为 OPEN"。MR1 顺手补显式测试。
- **carryForward 源年度 CLOSED 前置无测试**：`TestErpFinBudgetCarryForward` 4 规则均 seed OPEN 期间，未覆盖 CLOSED 前置。MR1 修复 P1-MA2-034 时一并补。

---

## 7. 裁决与剩余风险

### 7.1 总裁决：**FAIL**

按 `state-machine-business-review-prompt.md` 严重性指南，本审计发现：
- **零 P0**（无破坏业务路径或数据错误的 blocker）
- **2 项新 P1**（P1-MA2-033 NEVER_OPENED 无开启路径 + P1-MA2-034 carryForward 不校验源年度 CLOSED）
- **2 项新 P2** watch-only（P2-MA2-034 owner doc 漂移 + P2-MA2-035 REJECTED→DRAFT 直迁）
- **7 项已登记 MA2 finding 运行时影响复核无升级**（P1-MA2-017/018/019/022 仅治理缺陷；P1-MA2-020/021 维持 P1；P2-MA2-025 交接 A2.17）
- **5 处并发敏感点交接 A2.17**

### 7.2 可达性摘要

- 期间状态机：NEVER_OPENED 不可经系统 action 到 OPEN（P1-MA2-033）。
- 预算状态机：6 态全部可达，无死循环。

### 7.3 角色/权限摘要

- 反结账 kill-switch 无审批流（P1-MA2-020 维持）。
- 全部 mutation 缺显式 `@BizAuth`，依赖 `enableActionAuth` 配置（归 A2.18）。

### 7.4 外部依赖摘要

- 凭证/预算凭证与期间耦合约束齐全（resolveOpenPeriod 守卫）。
- 折旧/成本/汇兑外部步骤时序正确（在 CLOSED 前执行）。

### 7.5 剩余风险或跳过的区域

- **A2.5c AR/AP 核销状态机**：本审计仅确认期间 CLOSED_FINAL 时核销是否被阻止（期间侧守卫——经 resolveOpenPeriod 间接保证）；AR/AP 辅助账核销状态机归 A2.5c。
- **A2.16 commitment 释放路径完整性**：本审计仅复核 commitment 凭证在预算状态机中的 commit/release 迁移正确性；release 覆盖所有触发场景的完整性归 A2.16。
- **A2.17 并发与乐观锁**：5 处并发敏感点交接 A2.17。
- **A4.1b finance 代码质量**：期间/预算 Processor 代码质量（异常处理类型/N+1/索引）归 A4.1b。
- **M5 通知派发**：TODO 派发缺失归运营成熟度层 successor。

---

## 8. P0 处置

**无 P0 发现**。本审计零 P0。

---

## 9. 引用

- Plan：`docs/plans/2026-07-27-2315-1-audit-remediation-ma2-finance-period-budget-state-machine.md`
- Skill：`docs/skills/state-machine-business-review-prompt.md`
- Owner docs：`docs/design/finance/state-machine.md §对象二` / `period-close.md` / `budget.md` / `posting.md`
- 关联审计：
  - A2.5a：`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（凭证状态机 done）
  - A2.3：`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`（期末结账端到端 done）
  - A2.4：`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（库存核算 done）
- 索引更新：`docs/audits/arm-index.md`（新增 P1-MA2-033/034 + P2-MA2-034/035 + 本报告行）
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2（状态机正确性 + 预算与承付 finance 列推进至 ⚠️(P1)）
