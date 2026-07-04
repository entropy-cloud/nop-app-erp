# 坏账准备与应收核销（Bad Debt & Allowance for Doubtful Accounts）

## 目的

说明应收账款坏账的业务语义、生命周期分录、准备计提方法、应收净实现价值（NRV）呈现与期末充足性门控。本文件是 `ar-ap-reconciliation.md`（应收应付核销）在**坏账维度**的展开——`ar-ap-reconciliation.md` 定义"应收如何核销/账龄如何分析"，本文件定义"基于账龄如何计提坏账准备、如何核销、如何收回、如何在报表呈现 NRV"。

## 边界

- 本模块负责：坏账准备计提、坏账核销、坏账收回、准备释放、应收 NRV 呈现、期末 allowance 充足性门控。
- 本模块不负责：应收发票本身（sales 域）、应收核销多对多匹配（`ar-ap-reconciliation.md`）、总账报表渲染（nop-report 报表面）。
- 持久化字段、字典以 `model/app-erp-finance.orm.xml` 为准；本文件只描述稳定业务语义，不复述 schema。

## 设计依据

- **方法学来源**：`docs/analysis/erp-survey/2026-07-04-0000-ar-close-engine.md`（坏账准备五步分录 + 账龄分桶法 + 应收 NRV + SOX 控制，基于 ai-frankie/ar-close-engine 源码实测）。
- **会计准则**：CAS 14（收入）对坏账核销的账实一致要求；CAS 22（金融工具确认和计量）的预期信用损失模型（ECL）。本项目采用**简便实务**（账龄分桶 × 历史损失率），不强制实现完整 ECL 三阶段模型。
- **本项目既有基础**：`ar-ap-reconciliation.md` 已有 5 级账龄分级（0-30/31-60/61-90/91-180/180+）+ `ErpFinArApItem` 辅助账项（openAmount/settledAmount/status）+ `ErpFinReconciliation` 核销单。坏账设计复用这两层，不新建账龄体系、不新建辅助账。

## 关键概念（两个账户，同一事件）

> 这是审计师必问点，也是设计的核心区分。

| 账户 | 位置 | 性质 | 含义 |
|---|---|---|---|
| **Bad Debt Expense**（坏账损失） | 利润表（P&L） | 损益类 | **期间估计**的不可收回金额；配比原则下与同期间收入配比 |
| **Allowance for Doubtful Accounts**（坏账准备） | 资产负债表 | **抵减资产（contra asset）** | **累计**准备余额，抵减应收账款 gross，得到 NRV |

两者是同一事件的两个账户：计提时 Bad Debt Expense（借）配 Allowance（贷）；核销时只动 Allowance 与 AR，**不进 P&L**（计提时已确认损失）。

```
Accounts Receivable (gross)           ← sales 域 AR_INVOICE 确认
Less: Allowance for Doubtful Accounts  ← 本模块计提/核销/释放
Net Accounts Receivable (NRV)          ← 报表呈现
```

> Allowance 是 AR 的抵减资产，**配对抵消，不是负债**。

## 坏账生命周期五步分录

> 来源：`ar-reconciliation.md:7-57`（ar-close-engine 方法学全文）。

### 步骤 1 — 赊销确认（非本模块，sales 域 AR_INVOICE）

```
借：应收账款          X
    贷：主营业务收入        X
        应交税费-销项税     X
```

本步骤由 sales 域 `AR_INVOICE` 过账完成（见 `posting.md` 业务类型映射），产生 `ErpFinArApItem` 应收辅助账项。本模块的坏账计提基于此辅助账项的账龄。

### 步骤 2 — 月末计提准备（BAD_DEBT_RESERVE）

```
借：信用减值损失（Bad Debt Expense）     X
    贷：坏账准备（Allowance for Doubtful Accounts）   X
```

- **触发**：期末结账期间，按账龄分桶法计算必需准备（见 §计提方法），与当前 Allowance 账面比较：不足则补提（本步骤），超额则走步骤 5 释放。
- **性质**：**估计**，非核销决策。计提后应收辅助账项**仍在账龄表**（继续催收），不改变 `ErpFinArApItem.status`。
- **businessType**：`BAD_DEBT_RESERVE`（建议新增，保护区域，见 §businessType 映射）。

### 步骤 3 — 坏账核销（BAD_DEBT_WRITE_OFF）

```
借：坏账准备（Allowance）     X
    贷：应收账款                    X
```

- **触发**：确认某客户/发票无法收回（催收穷尽、客户破产、诉讼终结），经财务主管审批。
- **关键**：**不进 P&L**——损失在步骤 2 计提时已确认。核销只动 BS（Allowance ↔ AR）。
- **辅助账影响**：`ErpFinArApItem.status` → `WRITTEN_OFF`，`openAmount` → 0（经核销单 `ErpFinReconciliation` 反向结算表达，参 `ar-ap-reconciliation.md` 红冲以 `docStatus=REVERSED` 表达的模式）。
- **businessType**：`BAD_DEBT_WRITE_OFF`。
- **SOX 控制**：需财务主管审批 + 催收记录留存（见 §SOX 控制）。

### 步骤 4 — 坏账收回（BAD_DEBT_RECOVERY，核销后回款）

分两步：

**4a — 恢复应收（反转核销）：**
```
借：应收账款          X
    贷：坏账准备            X
```

**4b — 收款（正常收款过账）：**
```
借：银行存款          X
    贷：应收账款            X
```

- **触发**：已核销的客户事后回款。
- **审批**：4a 恢复需与步骤 3 核销**同级审批**（财务主管）。
- **businessType**：4a 用 `BAD_DEBT_RECOVERY`；4b 复用 sales 域 `RECEIPT`（正常收款过账）。
- **辅助账影响**：4a 将 `ErpFinArApItem.status` 从 `WRITTEN_OFF` 回退到正常未核销态；4b 经核销单归零。

### 步骤 5 — 准备释放（BAD_DEBT_RELEASE，准备超额）

```
借：坏账准备（Allowance）     X
    贷：信用减值损失（Bad Debt Expense）   X
```

- **触发**：期末计算必需准备 < 当前 Allowance 账面（实际核销少于预期）。
- **关键**：这是**唯一贷记 Bad Debt Expense 的场景**（减少损失 = 增加利润），直接影响 P&L。
- **审批**：财务主管（直接 P&L 影响）。
- **businessType**：`BAD_DEBT_RELEASE`。

## 坏账准备计提方法

### 账龄分桶法（默认方法）

复用 `ar-ap-reconciliation.md` 的 5 级账龄分级，叠加历史损失率：

| 账龄区间 | 历史损失率（建议默认） | 说明 |
|---|---|---|
| 0-30 天 | 0.5% | 正常账期 |
| 31-60 天 | 2% | 逾期初期 |
| 61-90 天 | 5% | 逾期中期 |
| 91-180 天 | 15% | 严重逾期 |
| 180 天以上 | 40% | 坏账风险 |

```
必需准备 = Σ(各账龄区间应收 openAmount × 该区间历史损失率)
```

损失率为**配置项**（`erp-fin.bad-debt-loss-rate-{bucket}`），可按账套/行业调整，不硬编码。

### 替代估计方法（可选，配置切换）

> 来源：`ar-reconciliation.md:79-82`。

| 方法 | 适用场景 | 配置 |
|---|---|---|
| 历史百分比法 | 大量小额、稳定信用政策 | 全部 AR × 扁平 % |
| 风险分类法 | 集中组合（少数大客户） | 按客户风险评分分级适用损失率 |
| 帕累托法 | 混合组合 | 大额客户逐户评审 + 尾部历史 % |

默认用账龄分桶法；其余方法为 Follow-up（触发条件：客户风险评分体系落地时）。

### 计提范围排除

- **争议发票**（`ErpFinArApItem` 标记 disputed）排除出准备基础（`ar_close.py` 实测：disputed/un-ageable 排除）。
- **负余额**（预收/贷余）排除。
- **已核销项**（`status=WRITTEN_OFF`，openAmount=0）自然不在账龄基础。

## 应收 NRV 呈现

```
应收账款（gross）           = Σ ErpFinArApItem.openAmount（未核销应收）
减：坏账准备（Allowance）    = Allowance 科目余额
应收账款净额（NRV）          = gross − allowance
```

NRV 是应收在资产负债表的列报价值（CAS 22"以预期信用损失为基础的减值"）。NRV 必须为正——若翻负，说明数据错误或 Allowance 反转（`ar_close.py` 实测：NRV 正值校验为控制项）。

## businessType 映射（保护区域建议）

> 新增 businessType 属 `erp-fin/business-type` 字典扩展（保护区域），落地须 ORM 计划 + 人工批准。本节为设计建议。

| businessType | 步骤 | 借贷方向 | 触发动作 |
|---|---|---|---|
| `BAD_DEBT_RESERVE` | 2 计提 | 借：信用减值损失 / 贷：坏账准备 | 期末结账计提（或补提） |
| `BAD_DEBT_WRITE_OFF` | 3 核销 | 借：坏账准备 / 贷：应收账款 | 财务主管审批核销 |
| `BAD_DEBT_RECOVERY` | 4a 恢复 | 借：应收账款 / 贷：坏账准备 | 核销后回款恢复（同级审批） |
| `BAD_DEBT_RELEASE` | 5 释放 | 借：坏账准备 / 贷：信用减值损失 | 期末准备超额释放 |

步骤 4b 收款复用既有 `RECEIPT` businessType，不新增。

## 期末 allowance 充足性门控

> 对标 ar-close-engine C-R1 控制（`CONTROLS.md:83-91`）。

期末结账（`period-close.md`）前置检查新增一项：

```
Allowance 充足性检查：
  必需准备（按账龄分桶计算）  vs  当前 Allowance GL 账面
  - 必需 > 账面 → 阻止结账，提示补提（BAD_DEBT_RESERVE）
  - 必需 < 账面 → 提示释放（BAD_DEBT_RELEASE，财务主管审批）
  - 相等（精度内）→ 通过
```

此检查接入 `period-close.md §结账前置检查`，与"posted=false 单据检查""未核销 AR/AP 检查"并列。NRV 是应收"#1 审计断言"（准确性/计价），未达标禁止结账。

## SOX 控制启示

> 来源：`CONTROLS.md`（ar-close-engine 8 控制）。本项目 `posting-log.md` 异常工作台可借鉴。

| 控制号 | ar-close-engine 控制 | 对本项目启示 |
|---|---|---|
| **C8** | 手工 JE 直达 AR 控制账户检测（管理越权红旗） | `posting-log.md` 异常工作台：标记所有**绕过业财回链**（无 `ErpFinVoucherBillR`）的 AR/AP 控制科目凭证 |
| **C-R1** | Allowance 充足性（NRV 检查） | 见上节期末门控 |
| **C21** | 职责分离（一人不能既发起又清应收） | 坏账核销/恢复的审批人与发起人分离（`state-machine.md` 危险操作双控） |
| — | 独立 reviewer 重算复现 | `posting-log.md` 规则命中日志支持从账龄基础重算必需准备，审计复现 |

## 状态含义（应收辅助账扩展）

`ErpFinArApItem.status` 在 `ar-ap-reconciliation.md` 已有 UNRECONCILED/PARTIAL/RECONCILED 基础上，坏账维度新增：

| status | 含义 | 进入条件 | 退出条件 |
|---|---|---|---|
| `WRITTEN_OFF` | 已核销 | 步骤 3 坏账核销 | 步骤 4a 恢复（回退正常） |
| `IN_COLLECTION` | 催收中（可选） | 账龄超期触发 | 核销/收回 |

> status 扩展属字典扩展（保护区域），落地须计划批准。本节为设计建议。

## 与现有机制的协作

| 对端 | 协作内容 |
|---|---|
| `ar-ap-reconciliation.md` | 复用 5 级账龄分级 + `ErpFinArApItem` 辅助账 + `ErpFinReconciliation` 核销单（坏账核销/恢复经核销单反向结算表达） |
| `posting.md` | 坏账四类 businessType 经 `IErpFinAcctDocProvider` 过账，业财回链关联源发票/核销单 |
| `period-close.md` | 期末结账前置检查新增 allowance 充足性门控；损益结转含信用减值损失科目 |
| `state-machine.md` | 坏账核销/恢复属影响 BS 的高风险操作，需财务主管权限 + 二次确认（参红冲控制） |
| `posting-log.md` | 坏账核销/恢复进规则命中日志；C8 异常工作台监测绕过回链的 AR/AP 凭证 |
| sales 域 | 步骤 1 赊销由 AR_INVOICE 过账，步骤 4b 收款由 RECEIPT 过账 |

## 反模式警示

- ⛔ **核销进 P&L**——核销只动 Allowance ↔ AR，损失在计提时已确认。核销再进 P&L 会双重计量。
- ⛔ **把 Allowance 当负债**——它是 AR 的抵减资产，配对抵消得到 NRV，不是负债科目。
- ⛔ **账龄基础包含已核销项/争议项**——会虚增/虚减必需准备。计提范围必须排除。
- ⛔ **手工 JE 直达 AR 控制账户**——绕过业财回链，是管理越权红旗（C8）。所有 AR 变动必须经辅助账 + 业财回链。
- ⛔ **损失率硬编码**——损失率按账套/行业/经济周期调整，必须配置化（`erp-fin.bad-debt-loss-rate-{bucket}`）。

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-fin.bad-debt-method` | AGING_BUCKET | 计提方法（AGING_BUCKET/HISTORICAL_PERCENT/RISK_CLASS/PARETO） |
| `erp-fin.bad-debt-loss-rate-0-30` | 0.005 | 0-30 天历史损失率 |
| `erp-fin.bad-debt-loss-rate-31-60` | 0.02 | 31-60 天历史损失率 |
| `erp-fin.bad-debt-loss-rate-61-90` | 0.05 | 61-90 天历史损失率 |
| `erp-fin.bad-debt-loss-rate-91-180` | 0.15 | 91-180 天历史损失率 |
| `erp-fin.bad-debt-loss-rate-180-plus` | 0.40 | 180 天以上历史损失率 |
| `erp-fin.bad-debt-write-off-require-approval` | true | 坏账核销是否强制财务主管审批 |
| `erp-fin.bad-debt-exclude-disputed` | true | 计提基础是否排除争议发票 |

## 菜单归属

finance 域「应收应付」分组下：坏账准备计提、坏账核销单、坏账收回、准备释放。期末 allowance 充足性报告归期末结账流程。

## 参考与证据

- `docs/analysis/erp-survey/2026-07-04-0000-ar-close-engine.md`（方法学来源，基于源码实测）
- `~/sources/erp/ar-close-engine/ar-reconciliation.md`（五步分录 + 账龄分桶 + NRV 全文）
- `~/sources/erp/ar-close-engine/CONTROLS.md`（8 SOX 控制 + C8/C-R1 深度解读）
- `docs/design/finance/ar-ap-reconciliation.md`（账龄分级 + 辅助账 + 核销单复用）
- `docs/design/finance/period-close.md`（期末结账前置检查）
- `docs/design/finance/posting-log.md`（异常工作台 C8 借鉴）

## 不做边界

- **不实现完整 ECL 三阶段模型**（CAS 22 正向减值）——采用简便实务（账龄分桶），ECL 属 Follow-up（触发条件：金融工具准则严格合规要求时）。
- **不实现客户风险评分体系**——风险分类法/帕累托法依赖客户风险评分，属 Follow-up（触发条件：CRM 客户信用评分落地时）。
- **不做坏账准备多维分摊**（按部门/产品线分摊准备）——属 GL Distribution 范畴（见 `posting.md` §GL Distribution Deferred）。
- **不做应收账款保理/质押**——属资金面（`treasury.md`）。
