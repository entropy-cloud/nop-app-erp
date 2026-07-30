# 2026-07-30-0841-1-r1-27-budget-commitment-release-path 预算承付释放路径完整性修复

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.27（P1-MA2-081/082/083/084），源自 A2.16 承付释放路径完整性审计
> Related: `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`；`docs/design/finance/budget.md §承付会计`；plan `2026-07-21-1206-2`（承付能力 A2 落地）
> Audit: required

## Current Baseline

四项 finding 经实仓逐项确认：承付（commitment）释放路径在三个释放完整性缺口 + 一个余量聚合语义混淆缺陷上未闭合。**全部 config-gated 默认 OFF**（`IErpFinBudgetCommitmentBiz.isCommitmentEnabled()` 默认 false，`ErpFinBudgetControlBiz` 预算控制亦 config-gated），无活跃数据破坏（P1 非 P0）。

**P1-MA2-084（aggregateAmount 语义混淆）— 确认：**
- `ErpFinBudgetControlBiz.aggregateAmount:104-138`：`budget=false` 分支过滤器为 `or(isNull("postingType"), ne("postingType", POSTING_TYPE_BUDGET))`（:115）——**放行 COMMITMENT**（COMMITMENT ≠ BUDGET），故 `actualBalance` 实际含 `actual + commitment`。
- 结果 `available = budgetBalance − actualBalance` 等价于 `available = budget − (actual + commitment)`，**结果正确**，但方法 javadoc（:103「budget=false 取实际凭证行（NORMAL/NULL）」）与变量名 `actualBalance` 误导维护者——若"修正"为排除 COMMITMENT 会破坏预算控制（commitment 不再被减去，超预算放行）。
- 注：类级 javadoc（:40-44）已由 plan 2026-07-29-2322-2（P1-MA3-025）修正为「actualBalance = postingType≠BUDGET（含…COMMITMENT）…等价」；本 finding 残留的是**方法级 :103 javadoc + 变量名 `actualBalance`** 未对齐，非类级。
- 与 `ErpFinBudgetScenarioProcessor.aggregateActualForLine`（carry-forward 时正确排除 COMMITMENT）不对称。

**P1-MA2-081（部分开票释放语义未声明）— 确认：**
- `ErpFinBudgetCommitmentBizModel.release:81-99` + `CommitmentVoucherGenerator.reverseCommitment:77` 全额红冲（无 `amount` 入参），一张 PO 多次部分开票时首张发票 approve 全额释放，后续发票 approve 经 `ErpPurInvoiceProcessor:318-322` 容错吞掉。
- owner doc `budget.md §3 接入点表:252-256` 仅列 3 接入点（commit / release-on-cancel / release-on-invoice-approve），**未声明「全额释放 vs 部分释放」语义**。

**P1-MA2-082（采购退货未释放承付）— 确认：**
- `ErpPurReturnProcessor`（397 行）不调 `IErpFinBudgetCommitmentBiz`；owner doc `budget.md §3` 接入点表无 release-on-return。
- 若 PO 已 commit + 部分发货后退货（部分开票前），承付保持全额占用未对应实际减少的采购量（偏移方向为保守方向——承付多占用）。

**P1-MA2-083（发票冲销后 commitment 未恢复）— 确认：**
- `ErpPurInvoiceProcessor.reverseApprove:106-121` + `cancel:123-137` 仅红冲 AP ACTUAL 凭证，**不调 commit() 恢复承付**；`ErpSalInvoiceProcessor` 同型。
- 系统不对称：invoice approve → release commitment，invoice reverseApprove → AP ACTUAL 回退但 commitment 保持已释放。
- owner doc `budget.md §3` 未声明冲销恢复语义。

**保护区域：** 承付会计属会计保护区域。本计划触及承付凭证写路径（082 新增 config-gated hook）+ 余量聚合算术（084）。无 ORM 变更（无 ask-first）。修复须独立 plan-audit + closure-audit，已有 owner doc（budget.md §承付会计）+ 测试为门控。

## Goals

- **084** `ErpFinBudgetControlBiz.aggregateAmount` 显式三通道分离（budget / actual / commitment），`available = budget − actual − commitment` 显式三段计算，消除变量名/javadoc 误导。
- **081** owner doc `budget.md §3` 声明「全额释放语义」+ 补「部分开票多次发票容错」测试断言。
- **082** owner doc `budget.md §3` 补第 4 接入点「release-on-return」+ `ErpPurReturnProcessor.approve` config-gated 调 `releaseIfPresent`（默认 OFF）。
- **083** owner doc `budget.md §3` 声明「发票冲销不恢复承付（保守方向）」+ 残留风险 + successor 触发条件。

## Non-Goals

- 不实现按开票金额比例的部分释放（P1-MA2-081 方案B——须 SPI 加 `amount` 入参 + `reverseCommitment` 重构 + 部分金额对账，与参考应用不成比例；归 successor）。
- 不实现发票冲销自动恢复承付（P1-MA2-083 方案A——须跨实体反查原始 PO/SO + 处理部分冲销 + 跨期语义；归 successor，保守方向 documented）。
- 不改 sales 侧承付（sales 承付对称问题随 083 owner doc 一并声明，sales 侧 hook 接线归 successor）。
- 不改 ORM（无 ask-first）；不改 commitment 科目映射配置。

## Task Route

- Type: `implementation-only change`（承付释放路径完整性 + 余量聚合算术 + owner doc 语义声明）
- Owner Docs: `docs/design/finance/budget.md §承付会计 §3 接入点表`、`docs/design/finance/posting.md §业务类型映射`
- Skill Selection Basis: 承付凭证写路径 + Processor hook + 跨实体调用 → `Skill: nop-backend-dev`（BizModel 动作/守卫/跨实体调用 + 产品化可定制性自检）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 全部修复 config-gated 默认 OFF（`erp-fin.commitment-enabled` / 新增 `erp-fin.commitment-release-on-return` 默认 false）。

## Execution Plan

### Phase 1 - 四项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision（084）**：**方案A（三通道分离）**。`aggregateAmount` 拆为按 postingType 显式三通道：budget=`eq(BUDGET)` / actual=`or(isNull, notIn(BUDGET, COMMITMENT))`（即当前 actual 通道精确表述——排除 BUDGET 与 COMMITMENT，**保留当前 RESERVATION/ADJUSTMENT/NORMAL 等仍计入 actual 的行为**）/ commitment=`eq(COMMITMENT)`；`available = budgetBalance − actualBalance − commitmentBalance` 显式三段（对齐 owner doc budget.md:59 三项式）。方案B（仅改 javadoc）被拒——变量名 `actualBalance` 含 commitment 是真实可维护性陷阱，非纯文档问题。残留风险：低——结果等价（actual 通道仍含 RESERVATION 等，仅从 actualBalance 中拆出 COMMITMENT 单独减去，与原 `available = budget − (actual+commitment)` 数值等价）；须补测试断言 commitment=0 时与旧公式等价 + RESERVATION 仍计入 actual。
      - Skill: `nop-backend-dev`
- [x] **Decision（081）**：**方案A（owner doc 声明 + 测试）**。owner doc `budget.md §3 接入点表`补「全额释放语义」注记（首张发票 approve 全额释放承付，后续发票容错跳过——实际占用产生时全额释放避免 actual+commitment 双重占用）。方案B（实现按比例部分释放）归 successor（触发条件见 Deferred）。
      - Skill: `nop-backend-dev`
- [x] **Decision（082）**：**方案A（config-gated release-on-return hook）**。owner doc 补第 4 接入点「release-on-return」+ `ErpPurReturnProcessor.approve` 经 `erp-fin.commitment-release-on-return`（默认 false）调 `budgetCommitmentBiz.releaseIfPresent(PURCHASE_ORDER, poCode)`。理由：退货减少实际采购量，承付应同步释放（消除保守方向的多占用偏移）；config-gate 默认 OFF 控制暴露面 + `releaseIfPresent` 容错（无原凭证静默跳过，**有意偏离 finding 文字的 `release`——对齐 release-on-cancel 容错范式 budget.md:369**）。方案B（永久不释放）被拒——真实释放路径缺口，非设计意图。**残留风险**：`releaseIfPresent`/`release` 走 `reverseCommitment` **全额红冲**（无 amount 入参），故**部分退货**会释放整张 PO 承付——剩余未开票数量失去承付保护，**可能允许超预算放行新订单**（与 083 同风险类）。缓解：owner doc §3 显式声明「release-on-return 为全额释放语义」+ Phase 2/3 补部分退货测试断言全额释放行为 + 按比例部分释放归 successor（与 081 方案B 同 successor）。
      - Skill: `nop-backend-dev`
- [x] **Decision（083）**：**方案B（documented simplification）**。owner doc `budget.md §3` 声明「发票冲销不恢复承付（保守方向：保持已释放状态）」+ 残留风险（冲销后 actual↓ + commitment=0 → 余量偏高，可能允许超预算放行新订单）+ successor 触发条件（多组织预算硬约束启用 + 冲销频率上升）。方案A（自动恢复）被拒——须跨实体反查原始 PO + 部分冲销 + 跨期语义，与 config-gated OFF 参考应用不成比例。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Phase 1 四项 Decision 逐项记录（084→Phase 2 算术，081/083→Phase 3 owner doc，082→Phase 2 hook + Phase 3 owner doc）。

### Phase 2 - 余量聚合三通道分离（084）+ 退货释放 hook（082）

Status: completed
Targets: `ErpFinBudgetControlBiz.java`、`ErpPurReturnProcessor.java`（+ 必要时 `ErpPurReturnProcessor` 注入 `IErpFinBudgetCommitmentBiz`）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Fix（084）**：`ErpFinBudgetControlBiz` 余量计算重构——`aggregateAmount` 拆出 commitment 通道：`budgetBalance=aggregate(budget=true)` / `actualBalance=aggregate(actual=true)`（过滤器排除 BUDGET 与 COMMITMENT）/ `commitmentBalance=aggregate(commitment=true)`；`available = budgetBalance − actualBalance − commitmentBalance`（显式三项式，对齐 budget.md:59）。更新 javadoc 为「actual=NORMAL/NULL/ADJUSTMENT 等（排除 BUDGET 与 COMMITMENT）」。
      - Skill: `nop-backend-dev`
- [x] **Add（082）**：`ErpPurReturnProcessor.approve` 后置经 config-gate `erp-fin.commitment-release-on-return`（默认 false）调 `budgetCommitmentBiz.releaseIfPresent(ErpFinConstants.BILL_TYPE_PURCHASE_ORDER, return.getPoCode())`；Processor 注入 `IErpFinBudgetCommitmentBiz`（非 private 字段，对齐 Nop `@Inject` 规则）。`releaseIfPresent` 容错：无原承付凭证静默跳过（既有 :102-113 逻辑）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——（a）084：构造 BUDGET + ACTUAL + COMMITMENT 凭证各一，断言 `available == budget − actual − commitment` 三段精确（含 commitment≠0 场景）；commitment=0 时与旧公式等价回归；RESERVATION 凭证仍计入 actual 通道断言。（b）082：`release-on-return=true` 时 return.approve 后承付凭证红冲 + 余量恢复；`=false`（默认）时不调 release（回归无变化）；**部分退货场景断言全额释放语义**（PO qty=100 commit + return 30 → 承付全额红冲，剩余 70 失去承付保护——documented 行为）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 084 余量三段断言通过（含 commitment 通道）；082 config-gated hook 可测（OFF 默认回归零变化，ON 时承付释放）。

### Phase 3 - owner doc 承付释放语义声明（081/082/083）+ 测试补全（081）

Status: completed
Targets: `docs/design/finance/budget.md §承付会计 §3 接入点表`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] **Add（081，owner doc）**：`budget.md §3 接入点表` 接入点 3 补「全额释放语义」注记——首张发票 approve 全额红冲承付；后续部分开票的发票 approve 经容错守卫跳过（`hasUnreversedCommitment` 已为 false）。
      - Skill: `nop-backend-dev`
- [x] **Add（082，owner doc）**：`budget.md §3 接入点表` 增第 4 接入点「release-on-return」（`ErpPurReturn.approve` config-gated，默认 OFF，调 `releaseIfPresent`）+ 语义说明（退货减少实际采购量→同步释放承付；**声明全额释放语义**——部分退货亦全额红冲承付，剩余未开票数量失去承付保护，按比例部分释放归 successor）。
      - Skill: `nop-backend-dev`
- [x] **Add（083，owner doc）**：`budget.md §3` 增「冲销恢复语义」节——发票 reverseApprove/cancel 不恢复承付（保守方向：保持已释放状态）+ 残留风险（余量偏高可能超预算放行）+ successor 触发条件（多组织预算硬约束启用时实现 commit() 恢复）。
      - Skill: `nop-backend-dev`
- [x] **Proof（081，测试）**：补「部分开票多次发票」E2E/单元测试——PO approve(commit) → invoice1.approve(全额 release) → invoice2.approve(容错跳过, posted 不变) → 断言承付凭证仅一张红冲 + invoice2 容错无异常。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] owner doc §3 接入点表含 4 接入点 + 全额释放语义 + 冲销恢复语义声明；081 多发票容错测试通过。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04f8401abffecg7KlkjACLmg1D) because (a) Decision 082 记录无残留风险，但 releaseIfPresent 走全额红冲→部分退货释放整张 PO 承付→剩余未开票数量失去保护（与 083 同风险类）；(b) Decision 084 称「残留风险：无」但替换 actual 通道过滤器留「...」歧义，且未注明类级 javadoc 已由 plan 2026-07-29-2322-2 修正。基线事实全绿（084 filter 放行 COMMITMENT / 081 release 全额无 amount / 082 ErpPurReturnProcessor.approve 零 commitment SPI / 083 invoice reverseApprove/cancel 不恢复 commitment 均确认）。
- Independent draft review iteration 2: accept (ses_04f8401abffecg7KlkjACLmg1D) after 082 增残留风险（全额释放语义）+ 部分退货测试 + 有意偏离 release→releaseIfPresent 注记；084 显式过滤器 `or(isNull, notIn(BUDGET, COMMITMENT))` 保留 RESERVATION 计入 actual + 类级 javadoc 预修注记 + 残留风险「低」；Phase 3 Targets 去「（如需）」；Closure Gates 补 purchase 域测试（082 hook 在 module-purchase）。

## Closure Gates

- [x] 范围内行为完成（4 项 finding 全部修复：084 三通道分离 + 082 release-on-return hook + 081/083 owner doc 语义声明）
- [x] 相关文档对齐（budget.md §3 接入点表 + 冲销恢复语义）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + finance 域与 purchase 域 `mvn test` 全绿——082 hook 落在 module-purchase `ErpPurReturnProcessor` + commitment 聚合在 module-finance + compliance checker 基线不高于 M0）
- [x] 无范围内项目降级为 deferred/follow-up（081 方案B / 083 方案A 为显式 successor，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 按开票金额比例部分释放（P1-MA2-081 方案B）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 全额释放语义（方案A）已声明 + 容错路径已测；部分释放须 SPI 加 `amount` 入参 + `reverseCommitment` 重构 + 部分金额对账，与 config-gated OFF 参考应用不成比例。
- Successor Required: `yes`（当多组织预算硬约束启用 + 部分开票为常态业务路径时，实现按比例部分释放）

### 发票冲销自动恢复承付（P1-MA2-083 方案A）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 保守方向（保持已释放）已 documented + 残留风险已声明；自动恢复须跨实体反查原始 PO/SO + 部分冲销 + 跨期语义。
- Successor Required: `yes`（当多组织预算硬约束启用 + 冲销频率上升致超预算放行成为实际痛点时）

### sales 侧承付释放路径

- Classification: `watch-only residual`
- Why Not Blocking Closure: sales 承付对称问题随 083 owner doc 一并声明；sales 侧 hook 接线与 purchase 同型，归 successor。
- Successor Required: `yes`（当 sales 承付控制启用时对称接线）

## Closure

Status Note: 已执行（Phase 1/2/3 全完成）+ 独立结束审计 PASS。所有范围内 finding 已闭合（084 三通道分离 + 082 config-gated release-on-return hook + 081/083 owner doc 语义声明 + 全额释放/冲销恢复语义测试）。验证：`mvn clean install -DskipTests` 全 reactor 绿 + finance/purchase 域全 `mvn test` 绿（084/081/082 新增测试 + 既有回归零失败）。

Closure Audit Evidence:

- 独立结束审计（ses_04f6c76abffeNUpvkMkTo6nLuq，新会话，执行者未自我审计）：**PASS**。
  - 084：`ErpFinBudgetControlBiz` `aggregateAmount` 三通道分离（`AmountChannel` enum + `applyPostingTypeFilter`，BUDGET=`eq`/COMMITMENT=`eq`/ACTUAL=`or(isNull, notIn(BUDGET,COMMITMENT))`），`check()` 显式三项式 `available = budget − actual − commitment`，数值等价旧公式，无残留误导 javadoc。
  - 082：`ErpPurReturnProcessor.approve` 后置 `runCommitmentReleaseOnReturnHook`（`doApprove` 之后），config-gated `erp-fin.commitment-release-on-return` 默认 false + `releaseIfPresent(PURCHASE_ORDER, poCode)`；`@Inject` 非 private；PO code 经 `return.receive.order.code` 解析；config 常量 + 接口方法 + impl `@Override` 均就位。
  - owner doc：`budget.md §承付会计` 4 接入点 + 全额释放语义 + 冲销恢复语义 + 配置表项齐全。
  - 测试：084（commitment≠0 三段精确 / commitment=0 等价回归 / RESERVATION 仍计入 actual）+ 081（多发票全额释放单红冲 + 第二发票守卫异常）+ 082（ON 红冲 + 部分退货全额释放）。
  - 回归安全：`IErpFinBudgetCommitmentBiz` 仅 1 个实现者，接口扩展向后兼容；082 双重 config-gate（总开关 + release-on-return）默认 OFF → 既有 113 purchase 测试零行为变化；084 对无 commitment 多数场景数值等价。
  - 反模式自检：无 `@Inject private` / 无硬编码 postingType / `NopException` + ErrorCode 正确。
  - roadmap：`audit-remediation-roadmap.md` R1.27 = `done`。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
