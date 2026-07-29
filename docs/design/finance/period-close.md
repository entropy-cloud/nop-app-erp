# 期末结账流程

## 目的

说明 ERP 系统期末结账的完整流程、控制机制与恢复操作。期末结账是财务核算的关键环节，确保本期所有业务单据已正确过账、成本已正确计算、凭证已完整生成。

本文件是 `flow-overview.md` L4 节"期末结算层"的详细展开。

> **实现范围注记（计划 `2026-07-02-1000-3` + `2026-07-02-1538-1` + `2026-07-05-0540-2`）**：本流程已落地月度结账核心链路——期间状态机（OPEN→CLOSING→CLOSED→CLOSED_FINAL，含反结账）、前置检查、AR/AP/INV/AST/GL 模块按序关账、折旧集成门控、汇兑重估（承接 0300-3 deferred）、损益结转（收入/费用/成本三类）、试算平衡表快照、反结账红冲。**步骤2 存货成本核算已接线**：INV 模块关账经 `closeInvModule` 调 inventory `IErpInvCostingBiz.reclosePeriodCosts`（finance→inventory R，DAG 合法）兜底重算本期 FIFO 成本层/COGS 异常（config-gated `erp-fin.inv-costing-reclose-on-close`，单域无 inv-service 时 try/catch 告警跳过）；正常路径由 inventory 记账器在移动单 DONE 时按物料 costMethod 策略分派（MOVING_AVERAGE/FIFO）维护成本层与流水成本，关账仅兜底。**年度结转已落地**（plan `2026-07-05-0540-2`）：12 月结账时 `closePeriod` 增年度分支——辅助账跨年对账门控（AR/AP 辅助账合计 vs 总账科目余额，config-gated `erp-fin.auxiliary-recon-gate-enabled`）→ 本年利润→未分配利润结转凭证（新增业务类型 `PROFIT_TO_RETAINED_EARNINGS`，本年利润清零）→ populate 次年 1 月 `ErpFinGlBalance.yearOpeningDebit/Credit` 年初余额 → `generateNextYearPeriods(year+1)` 自动创建次年 12 期间（1 月 OPEN、其余 NEVER_OPENED，config-gated）；反结账覆盖年度结转凭证红冲，次年期间已创建时阻止反结账（须先删次年期间）。**银行存款外币汇兑重估已落地**（plan `2026-07-05-0540-2`）：`ExchangeRevaluationService` 扩展重估外币 `ErpFinFundAccount` 银行存款余额（currentBalance×期末汇率 vs 科目账面本位币聚合），差额生成 EXCHANGE_GAIN_LOSS 凭证（与 AR/AP 重估同业务类型同事务，config-gated `erp-fin.bank-fx-revaluation-enabled`），解除 1000-3 Non-Goal「银行存款外币重估需科目级币种标记」。以下子项为已裁定的 Non-Goal（详见计划 Deferred But Adjudicated）：BATCH/INDIVIDUAL/~~STANDARD~~/全月一次/LIFO 计价（inventory 域 successor）、费用摊销/待摊费用（步骤4，模块未落地）、年度报表渲染（资产负债表/利润表/现金流量表属 nop-report 报表面）、利润分配明细（法定/任意盈余公积、应付股利）、多账套/合并报表年度结转、历史年度追溯结转。

> **坏账准备充足性门控**（2026-07-04 新增设计）：期末结账前置检查新增 Allowance 充足性校验——必需准备（账龄分桶计算）vs 当前 GL Allowance 账面，不足阻止结账（提示补提 BAD_DEBT_RESERVE）、超额提示释放（BAD_DEBT_RELEASE）。NRV 是应收 #1 审计断言，未达标禁止结账。详见 `bad-debt.md` §期末 allowance 充足性门控。

## 期末结账流程总览

### 触发时机

期末结账可按以下周期触发：

| 周期 | 说明 | 触发时间 |
|------|------|----------|
| 月末结账 | 自然月结束后的账务整理 | 每月最后一天22:00 或次月1日 |
| 季末结账 | 自然季度结束 | 每季度末月结账时一并处理 |
| 年末结账 | 自然年度结束 | 每年12月结账，含年度数据汇总 |

### 结账前置检查

执行期末结账前，系统自动执行以下检查：

```
期末结账前置检查
        │
        ├─► 检查本期单据是否全部过账 (posted=true)
        │      ├─► 查询 posted=false 的已审核单据
        │      ├─► 若存在，列出清单并阻止结账
        │      └─► 提示：先完成过账或确认可延期
        │
        ├─► 检查本期凭证是否全部审核
        │      ├─► 查询未审核的凭证
        │      └─► 若存在，列出清单
        │
        ├─► 检查本期是否有未核销的应收应付
        │      ├─► 查询应收应付核销状态（status≠SETTLED/CANCELLED/WRITTEN_OFF）
        │      └─► 提示：建议结账前完成核销
        │
        ├─► 检查坏账准备充足性（Allowance 门控，已落地 plan 2026-07-05-0540-1）
        │      ├─► 必需准备（账龄分桶法 Σ openAmount×损失率）vs 当前 Allowance GL 账面
        │      ├─► 必需 > 账面 → shortfall 阻止结账，提示补提（BAD_DEBT_RESERVE）
        │      ├─► 必需 < 账面 → excess 提示释放（BAD_DEBT_RELEASE，非阻断）
        │      └─► 精度内相等 → 通过（config-gated erp-fin.bad-debt-allowance-gate-enabled）
        │
        ├─► 检查本期是否已执行折旧
        │      ├─► 查询本期应折旧但未执行的固定资产
        │      └─► 若存在，阻止结账或强制执行
        │
        └─► 检查本期成本核算是否完成
               ├─► 查询存货成本核算状态
               └─► 若未完成，阻止结账
```

### 期末结账步骤

```
期末结账流程
        │
        ├─► 步骤1：业务单据过账检查
        │      ├─► 扫描本期 posted=false 的业务单据
        │      ├─► 触发兜底扫描重新过账
        │      └─► 生成过账异常报告
        │
        ├─► 步骤2：成本核算
        │      ├─► 按配置的成本核算方法计算存货成本
        │      │      ├─► 移动加权平均法
        │      │      ├─► FIFO（先进先出）
        │      │      └─► 批次成本法
        │      ├─► 更新库存流水单位成本
        │      └─► 生成存货估值凭证
        │
        ├─► 步骤3：折旧计提
        │      ├─► 查询本期应折旧资产
        │      ├─► 按折旧方法计算折旧额
        │      ├─► 生成折旧凭证
        │      └─► 更新资产卡片累计折旧
        │
        ├─► 步骤4：费用摊销
        │      ├─► 查询待摊费用
        │      ├─► 计算本期应摊销金额
        │      └─► 生成待摊费用凭证
        │
        ├─► 步骤5：损益结转
        │      ├─► 查询本期收入类科目余额
        │      ├─► 查询本期费用类科目余额
        │      ├─► 生成结转凭证（借：本年利润 ← 收入）
        │      └─► 生成结转凭证（借：本年利润 → 费用）
        │
        ├─► 步骤6：生成结账凭证
        │      ├─► 汇总本期所有凭证
        │      ├─► 生成结账汇总凭证
        │      └─► 可选：生成余额调节表
        │
        ├─► 步骤7：标记期间结账
        │      ├─► 更新会计期间状态为 CLOSED_FINAL
        │      ├─► 锁定本期凭证不允许修改
        │      └─► 自动开启下一会计期间
        │
        └─► 步骤8：生成结账报告
               ├─► 科目余额表
               ├─► 试算平衡表
               ├─► 应收应付账龄分析
               ├─► 库存台账
               └─► 固定资产折旧明细
```

## 成本核算方法

### 移动加权平均法

每次入库后重新计算加权平均单位成本：

```
加权平均单位成本 = (期初金额 + 本期入库金额) / (期初数量 + 本期入库数量)

出库成本 = 出库数量 × 加权平均单位成本
```

**适用场景**：价格波动较小、对成本精度要求一般的存货

### FIFO（先进先出）

按入库顺序出库，先入库的先出库：

```
出库成本 = 最早入库批次的单位成本 × 出库数量
```

**适用场景**：保质期管理、批次追溯、价格波动较大

### 批次成本法

按特定批次跟踪成本：

```
出库成本 = 指定批次的单位成本 × 出库数量
```

**适用场景**：高价商品、定制产品、严格批次管理

## 期间控制

### 期间状态

会计期间有独立状态机：

| 状态 | 含义 | 可新增凭证 | 可修改凭证 |
|------|------|------------|------------|
| OPEN（已开启） | 正常核算中 | 是 | 是 |
| CLOSING（结账中） | 正在执行结账 | 否 | 否 |
| CLOSED（已结账） | 结账完成，待复核 | 否 | 否 |
| CLOSED_FINAL（已复核） | 最终锁定 | 否 | 否 |

### 期间约束

- **OPEN → CLOSING**：结账操作触发，自动锁定
- **CLOSING → CLOSED**：结账完成，待生成报表
- **CLOSED → CLOSED_FINAL**：报表生成后最终锁定
- **CLOSED_FINAL → OPEN**：反结账操作（需高权限 + 审批）

## 反结账流程

### 触发场景

- 发现本期凭证错误需要调整
- 补录本期遗漏的业务单据
- 报表数据需要修正

### 反结账步骤

```
反结账流程
        │
        ├─► 步骤1：反结账审批
        │      ├─► 记录反结账原因
        │      ├─► 审批人确认
        │      └─► 记录审计日志
        │
        ├─► 步骤2：开启期间
        │      ├─► 期间状态：CLOSED_FINAL → CLOSING → OPEN
        │      └─► 解锁凭证编辑权限
        │
        ├─► 步骤3：处理结转凭证
        │      ├─► 查询损益结转凭证
        │      ├─► 生成反向凭证冲销
        │      └─► 恢复收入/费用科目余额
        │
        ├─► 步骤4：处理折旧凭证
        │      ├─► 查询本期折旧凭证
        │      ├─► 生成红字凭证冲销
        │      └─► 恢复资产累计折旧
        │
        ├─► 步骤5：处理成本凭证
        │      ├─► 查询存货估值凭证
        │      ├─► 生成红字凭证冲销
        │      └─► 恢复库存成本
        │
        ├─► 步骤6：解锁业务单据
        │      ├─► 允许修改已审核单据
        │      ├─► 重新审核触发过账
        │      └─► 记录调整原因
        │
        ├─► 步骤7：重新结账
        │      ├─► 重新执行期末结账流程
        │      └─► 生成新的结账凭证
        │
        └─► 步骤8：记录审计
               ├─► 反结账操作记录
               ├─► 调整单据清单
               ├─► 调整金额影响
               └─► 相关责任人签名
```

### 反结账约束

- **权限要求**：管理员 + 审批
- **报表影响**：已出具的财务报表需同步调整或重新出具
- **税务影响**：已申报税务需与税务机关沟通
- **数据一致性**：反结账后需重新执行期间内所有单据过账

## 与其他域的协作

| 对端域 | 协作内容 |
|--------|----------|
| purchase | 采购单据过账检查、应付凭证生成 |
| sales | 销售单据过账检查、应收凭证生成 |
| inventory | 存货成本核算、库存流水成本更新 |
| assets | 折旧计提、资产价值更新 |
| projects | 项目成本归集、工时成本结转 |

## 年度结转规则

> **已落地**（plan `2026-07-05-0540-2`）：步骤3（本年利润→未分配利润）、步骤4（辅助账跨年对账门控 + 次年年初余额 populate）、步骤5（次年 12 期间自动创建）均已实现。步骤1（常规期末结账）、步骤2（损益结转）由月度结账链路承载。步骤6（年度报表）属 nop-report 报表面，归 Non-Goal。

年末结账（12 月）在常规期末结账基础上增加以下步骤：

### 年度结转流程

```
年度结账（12月）
        │
        ├─► 步骤1：执行常规期末结账流程（同上）
        │
        ├─► 步骤2：结转损益
        │      ├─► 收入科目余额 → 本年利润（贷方）
        │      ├─► 费用科目余额 → 本年利润（借方）
        │      └─► 生成损益结转凭证
        │
        ├─► 步骤3：结转本年利润
        │      ├─► 本年利润余额 → 未分配利润
        │      └─► 生成利润分配凭证
        │
        ├─► 步骤4：辅助账结转
        │      ├─► 存货：余额结转至新年度
        │      ├─► AR/AP：未结清项结转至新年度
        │      ├─► 资产：净值结转，累计折旧保留
        │      └─► 各辅助账与总账对账
        │
        ├─► 步骤5：新开期间
        │      ├─► 自动创建次年 12 个会计期间
        │      └─► 1月状态设为 OPEN
        │
        └─► 步骤6：年度报表
               ├─► 生成资产负债表、利润表、现金流量表
               └─► 标记年度结账完成
```

### 结转约束

- 损益结转凭证必须在所有业务单据过账完成后执行
- 辅助账（存货/AR/AP/资产）余额必须与总账一致（跨账对账）
- 结转凭证不可修改，只能通过反结账冲销
- 新年度期间必须在旧年度结账完成后才能开启

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-fin.auto-post-on-close` | false | 结账前置检查门控：false=未过账凭证/未处置异常阻断结账（安全默认），true=降级为提示放行结账。**未核销 AR-AP 始终为结构化提示不阻断**；**坏账准备 shortfall 始终硬阻断**（不受本 config 影响，见 bad-debt.md §期末 allowance 充足性门控） |
| `erp-inv.allow-negative-stock (引用库存域)` | false | 结账时是否允许负库存 |
| `erp-fin.auto-depreciation` | true | 结账时自动计提折旧 |
| `erp-fin.closing-reminder-days` | 3 | 结账提醒提前天数 |
| `erp-fin.period-close-cron` | `0 0 22 L * ?` | 期末结账定时触发（每月最后一天 22:00）；登记于 `docs/architecture/job-scheduling.md` §3.1 `erp-fin-period-close` 作业，cron 接线归 follow-up |
| `erp-fin.annual-close-enabled` | true | 年度结转总开关（12 月结账后是否执行本年利润→未分配利润 + 次年期间创建 + 年初余额 populate） |
| `erp-fin.auto-generate-next-year-periods` | true | 年度结转时是否自动触发次年期间创建 |
| `erp-fin.period-generate-skip-existing` | false | 次年期间生成幂等策略：已存在同年期间时是否仅补缺失月份（false=抛错） |
| `erp-fin.auxiliary-recon-gate-enabled` | true | 辅助账跨年对账门控（AR/AP 辅助账合计 vs 总账科目余额不一致阻止年度结账） |
| `erp-fin.bank-fx-revaluation-enabled` | true | 银行存款外币汇兑重估开关 |

## 已知简化与残留风险（R1.10+R1.11 审计修复裁决）

> 以下为 P1-MA2-017~022 审计发现的裁决落地记录。实现项标注代码位置；documented simplification 项标注 successor 触发条件。

### P1-MA2-017 auto-post-on-close 阻断分级（已实现）

- `erp-fin.auto-post-on-close` 语义澄清为前置检查门控（非 auto-post 动作）：false=阻断未过账凭证/未处置异常（安全默认），true=降级为提示。
- **未核销 AR-AP** 移出阻断集（`PeriodPreCheckReport.hasIssues()`），改为结构化提示（`hasReminders()`），不阻断结账（对齐 §结账前置检查「未核销=提示」）。
- **坏账准备 shortfall** 独立硬阻断，不受 `auto-post-on-close` 影响（对齐 bad-debt.md shortfall 阻断）。
- 代码位置：`ErpFinAccountingPeriodProcessor.closePeriod`、`PeriodPreCheckReport.hasIssues/hasReminders/hasAllowanceShortfall`。

### P1-MA2-018 年初余额非累计（documented simplification）

- `AnnualCloseService.populateNextYearOpening` 经 `aggregateYearSubjectActivity(year)` 仅聚合**本年度**分录净额写入次年 `ErpFinGlBalance.yearOpeningDebit/Credit`。
- **资产负债类科目缺上年结转额**：第 2 年及以后年度结转年初余额不精确（首年期初为零故正确）。
- 受 `ErpFinGlBalance` 未由过账引擎维护的架构限制约束（`AnnualCloseService` 注释明示）。
- **Successor**：补 GL 余额维护（过账引擎在 postVoucher 时维护 opening/closing 余额），使年初余额 = 累计期末。

### P1-MA2-019 辅助账跨年对账作用域（已修复作用域一致性 + documented simplification 残留）

- **作用域修复**：`AnnualCloseService.sumArApOpenFunctional` 增年度过滤（businessDate 落在结账年度内），使辅助账与 GL 同为单年作用域，消除跨年假阳性/假阴性。
- **残留简化**：当前为单年作用域对账，累计余额对账需 GL 余额维护 successor。

### P1-MA2-020 反结账审批（documented simplification — kill-switch successor）

- 当前 `reverse-close-approval-required`（默认 true）是**保护性 kill-switch**：true 时直接拒绝反结账（需管理员设 false 才能执行），false 时由 @BizMutation 角色权限门控。
- **非完整审批流**：owner doc §反结账约束要求「管理员+审批」，实现为 config kill-switch + 角色权限（无独立审批 action）。
- **Successor**：实现完整审批流（反结账申请→审批→执行）。解除条件见 `state-machine.md §已知限制：浏览器层 xwf 审批路径`。

### P1-MA2-021 CLOSED_FINAL 凭证锁定（已实现）

- `ErpFinVoucherBizModel.postVoucher`/`reverseVoucher` 前校验凭证所属期间状态：CLOSED/CLOSED_FINAL 时抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`。
- 代码位置：`ErpFinVoucherBizModel.assertPeriodNotLocked`。

### P1-MA2-022 FX 重估无前期 reversal（documented simplification — IAS 21 残留风险）

- `ExchangeRevaluationService.revalueArAp` 查询所有未核销外币项（不按期间过滤），重估后不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。
- **当前为当期 spot-rate 重估**：每月结账对同一批开放项按新汇率重估，前期汇兑损益不冲回，累计漂移。
- **IAS 21 合规性残留风险**：非 IAS 21 spot-rate「前期重估期末自动 reversal」完整语义。config-gated（`erp-fin.exchange-revaluation-enabled` 可关闭）。
- **Successor**：实现前期 FX 凭证期末自动 reversal + 期间过滤 + 更新 `openAmountFunctional`。

## 异常处理

| 异常场景 | 处理 |
|----------|------|
| 过账失败 | 列出失败单据，阻止结账 |
| 成本核算失败 | 检查存货数据完整性，修复后重试 |
| 折旧执行失败 | 检查资产数据，修复后重试 |
| 凭证借贷不平衡 | 检查凭证模板配置，修复后重试 |
| 期间已被下游引用 | 需先解除引用（如已上报税务） |

## 审查要点

审查期末结账设计时，重点检查：

1. 结账前置检查是否覆盖所有必要条件
2. 成本核算方法是否与业务需求匹配
3. 期间锁定机制是否严格
4. 反结账流程是否有足够的审计与审批
5. 与其他域的数据一致性是否保证

## 预算结转与期间状态机（A2，plan 2026-07-21-1206-2）

> A2 落地预算结转规则引擎（budget.md §结转规则引擎）。结转将上年度预算剩余（或已用）按规则结转至下年度，与期间状态机强相关。

### 结转前置条件（期间状态机协调）

- **源 Scenario 所在年度的所有会计期间必须 CLOSED**（`ErpFinAccountingPeriodStatus.glStatus = CLOSED`）——年度已结账是结转的硬前置。
- 期间未结账时执行 carryForward 抛 `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID`（rule 校验失败/前置不满足）。
- 反结账（reverseClose）后，已结转的源 Scenario status=CLOSED 不回退（终态不可逆），但可经新 Scenario 重新结转。

### 结转后 Scenario 状态扩展

| 状态 | 含义 | A2 新增 |
|------|------|---------|
| DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED | 既有 | 否 |
| **CLOSED** | 已结转（源 Scenario 终态，结转后不可再调整） | ✅ |

结转后源 Scenario：
- `docStatus = CLOSED`（终态，结转后不可再调整，避免已结转数据被改）
- `closedAt` = 结转时间戳（审计标识）
- 写 `ErpFinBudgetCarryForwardLog`：sourceScenarioId / targetScenarioId / rule / sourceRemaining / sourceUsed / carriedAmount

### 跨年度期间状态机协调

```
上年度 12 月期间 glStatus = OPEN/CLOSING → 拒绝结转
       ↓
上年度 12 月期间 glStatus = CLOSED → 允许结转
       ↓
carryForward 执行：源 Scenario status=CLOSED + 目标 Scenario 增补 BudgetLine + 写 BUDGET 凭证
       ↓
目标 Scenario 进入 DRAFT（待用户审批后生效参与预算控制）
```

### commitment 与结转

A2 默认 **commitment 不结转**（与 actualAmount 合并记录在源 Scenario 的余量计算中，结转后由源 Scenario 的 CLOSED 终态保留审计轨迹）。客户如有"未释放 commitment 一并结转至下年度"需求，归 successor（`commitment 一并结转` Deferred）。

详见 [`budget.md §结转规则引擎`](budget.md#结转规则引擎a2plan-2026-07-21-1206-2)。

## 期末结账向导（F12 Tier C，plan 2026-07-23-0818-2）

> **零后端 delta**：向导纯 UI 编排既有 M4 已审计 Facade mutation，不引入新会计逻辑/写入语义。前任 successor 触发条件「后端 mutation 重构授权」经 Phase 0 Explore 核实失效（moot）——5 个 Facade mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods）完全充分，roadmap §F12「5 步：成本转结→汇兑损益→损益结转→凭证复审→结账」属笔误（closePeriod 内部一次性多步执行）。

### 向导页面

- 路径：`module-finance/erp-fin-web/.../erp/fin/pages/period-close-wizard/main.page.yaml`
- 菜单：`fin-period-close-wizard`（会计期间分组，orderNo=205，紧随 ErpFinAccountingPeriod）

### 步骤映射（owner doc 8 步概念 + roadmap 5 步笔误 → 实际 mutation 链）

| 向导步骤 | 调用 action | 类型 | 说明 |
|---------|------------|------|------|
| Step 1 选择期间 + 前置检查 | `ErpFinAccountingPeriod__preCheck(periodId)` | @BizQuery 只读 | 结构化展示 `PeriodPreCheckReport`（unpostedVoucherCodes/unsettledArApCodes/unresolvedPostingExceptionKeys + 坏账准备 shortfall/excess）。阻断项（unposted + allowanceShortfall>0）红色高亮，禁用继续 |
| Step 2 执行月度结账 | `ErpFinAccountingPeriod__closePeriod(periodId)` | @BizMutation 一次性 | 一次性同步编排 AR→AP→INV→AST→GL 模块关账 + 损益结转 + 汇兑重估。UI 展示 per-module 关账**结果**（ErpFinAccountingPeriodStatus 的 arStatus/apStatus/invStatus/glStatus/assetStatus）——closePeriod 同步一次性，故展示执行后结果（非实时进度） |
| Step 3 年度结转（仅 12 月可见） | （closePeriod 已内含年度分支） | 非独立 mutation | 12 月期间 closePeriod 一并执行本年利润→未分配利润结转凭证 + 次年年初余额 populate + `generateNextYearPeriods(year+1)`。向导此步展示年度结转结果（次年期间列表），非独立 action |
| Step 4 终关 | `ErpFinAccountingPeriod__finalizePeriod(periodId)` | @BizMutation | CLOSED → CLOSED_FINAL 最终锁定 |
| 反结账（独立入口） | `ErpFinAccountingPeriod__reverseClose(periodId)` | @BizMutation 一次性 | 一次性执行 §反结账步骤 8 步概念模型（红冲结转凭证 + 处理折旧/成本凭证 + 解锁业务单据 + 重开期间）。向导提供红冲影响预览 + 二次确认 dialog |

### per-module 关账结果数据源

`ErpFinAccountingPeriodStatus`（`app-erp-finance.orm.xml:669-673`）按 `periodId` 关联，字段 `arStatus`/`apStatus`/`invStatus`/`glStatus`/`assetStatus`（dict `erp-fin/module-close-status`：OPEN/CLOSING/CLOSED）。closePeriod 同步一次性执行，故向导展示执行**后**的 per-module status（result visualization，非实时 progress）。

### 组件选型裁决（Phase 0）

- `form layoutControl="wizard"` AMIS 渲染器**未实现**（仅实现 `tabs`，`nop-entropy/docs-for-ai/02-core-guides/layout-syntax-reference.md:288`）→ view.xml wizard 布局不可用
- AMIS `type:steps` prop 契约不稳（`page-structure-patterns.md §8.12` b2b ASN 先例）→ 不采用
- **采用**：手写 `page.yaml` + 步骤指示器（service adaptor 预渲染 HTML 单 tpl，避免 each+对象 scope 怪癖）+ 分步 button-driven mutation（`actionType:ajax` + `reload:wizardService`）

### E2E 覆盖

- action spec：`tests/e2e/business-actions/fin-period-close-wizard.action.spec.ts`——驱动 preCheck→closePeriod→finalizePeriod→reverseClose 全链 + 非法状态守卫（closePeriod on CLOSED_FINAL 拒绝）
- visual spec：`tests/e2e/visual/fin-period-close-wizard.visual.spec.ts`——page 可达 + 步骤指示器 + preCheck 结果区 + 反结账 dialog DOM 结构
