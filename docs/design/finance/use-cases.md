# 财务域用例规格(Finance Use Cases)

> 从使用场景出发组织财务域可验证用例。机制细节引用不重复(指向 posting / period-close / ar-ap-reconciliation / costing-methods / bank-reconciliation / cost-center / budget)。
> 财务是业财一体的枢纽,用例偏多(机制最密集)。断言用自然语言与伪代码交织。

## 状态轴速查(详见 state-machine.md)

```
凭证状态:  DRAFT / POSTED / CANCELLED
期间状态:  OPEN / CLOSING / CLOSED / CLOSED_FINAL
核销状态:  未核销 / 部分 / 已核销 / 超额
```

---

## UC-FIN-01 业财自动过账

**场景**:业务单据(采购入库/销售出库/发票/收付款)审核触发自动生成财务凭证。

**行为链路**:见 posting.md §过账引擎、§异步过账
```
业务单据.审核通过 →
  按 businessType 路由到对应 IErpFinAcctDocProvider
  → 填充凭证模板(AMOUNT 等占位符)
  → FactsValidator 校验/改写
  → 写库(凭证 + 业财回链 VoucherBillR)
  → 单据.posted = true
```

**可验证断言**:
```
存在凭证: businessType 对应 + 来源单号 == 业务单据.单号
凭证行借贷平衡(Σ借 == Σ贷)
存在 VoucherBillR(billType, billCode=业务单据.单号) 双向回链
业务单据.posted == true
```

**涉及机制**:posting.md §过账引擎/§凭证模板/§异步过账/§业财回链

---

## UC-FIN-02 业务单据作废触发红字冲销

**场景**:已过账的业务单据作废,冲销对应凭证。

**可验证断言**(见 posting.md §冲销机制):
```
业务单据.作废 →
  经 VoucherBillR 反查关联凭证
  生成红字凭证(金额取负, 关联原凭证)
  原凭证标记 isReversed = true
  业务单据.posted = false
红字凭证走 DRAFT → POSTED 流程
```

**涉及机制**:posting.md §冲销机制、state-machine.md(凭证状态)

---

## UC-FIN-03 可插拔 Provider 路由

**场景**:新增一种业务类型(如 PROJECT_SETTLEMENT),验证零改核心即可接入。

**可验证断言**(见 posting.md §过账引擎):
```
新增 IErpFinAcctDocProvider Bean(注册 businessType=PROJECT_SETTLEMENT)
→ ErpFinAcctDocRegistry 自动聚合(@Inject List)
→ 项目结算单审核时, 该 Provider 被路由调用, 生成凭证
→ 核心过账引擎代码无改动
```

**涉及机制**:posting.md §过账引擎(可插拔)

---

## UC-FIN-04 FactsValidator 科目分摊

**场景**:凭证写库前,按成本中心分摊规则把一条凭证行拆成多行。

**可验证断言**(见 posting.md §FactsValidator、cost-center.md):
```
原始凭证行(挂成本中心A, 金额100) →
  命中 GlDistribution 规则(A→A:60%/B:40%) →
  拆为两行: 成本中心A 金额60, 成本中心B 金额40
Σ 拆分行金额 == 原行金额(平衡保持)
若 Σ percent != 100: 抛异常拒绝过账
```

**涉及机制**:posting.md §FactsValidator、cost-center.md §ErpFinGlDistribution

---

## UC-FIN-05 多账套并行过账

**场景**:同一业务在财务账与管理账(或税务账)各生成一组凭证。

**可验证断言**(见 multiple-accounting-schemas.md、posting.md §多套科目表):
```
业务单据.审核 →
  对每个启用的 AcctSchema 各生成一组凭证
  每组凭证.acctSchemaId 不同, 科目映射不同(同业务不同科目)
所有组凭证.posted == true
GlBalance 按 acctSchemaId 隔离(各账套余额独立)
```

**涉及机制**:multiple-accounting-schemas.md、posting.md §多套科目表

---

## UC-FIN-06 期末结账前置门禁

**场景**:期末结账时,前置检查拦截未过账/未核销/未折旧。

**可验证断言**(见 period-close.md §前置检查):
```
期间.结账(→CLOSING) 前置检查:
  若 存在 posted=false 的单据 → 拒绝(列出)
  若 存在未审核凭证 → 拒绝
  若 存在未核销应收应付(强制核销模式) → 拒绝
  若 资产未折旧 → 拒绝
  若 成本未算 → 拒绝
全部通过 → 进入结账步骤(见 §结账8步)
```

**涉及机制**:period-close.md §前置检查、state-machine.md(期间状态)

---

## UC-FIN-07 反结账

**场景**:已 CLOSED_FINAL 的期间需要反结账(更正错误)。

**可验证断言**(见 period-close.md §反结账):
```
反结账需: 高权限 + 审批
CLOSED_FINAL → OPEN
冲销: 结转凭证/折旧凭证/成本凭证
解锁期间内单据(可修改)
重新结账 → CLOSED_FINAL
全程审计(记录反结账操作人/原因)
```

**涉及机制**:period-close.md §反结账、state-machine.md

---

## UC-FIN-08 收款核销发票

**场景**:收款单核销一张或多张应收发票(部分/全额)。

**可验证断言**(见 ar-ap-reconciliation.md §核销流程/§状态):
```
收款单.核销(发票1, 发票2, ...) →
  生成核销明细(每条: 收款单行 ↔ 发票行, 金额)
发票.核销状态: 按累计核销金额计算
  累计核销 < 发票金额 → 部分
  累计核销 == 发票金额 → 已核销
往来单位.应收余额 = Σ发票 - Σ核销 - Σ红字
```

**涉及机制**:ar-ap-reconciliation.md §核销流程/§状态/§余额计算

---

## UC-FIN-09 银行对账与未达账项

**场景**:月末银行对账,自动勾对 + 未达账项调整。

**可验证断言**(见 bank-reconciliation.md):
```
导入银行对账单(bankTxnCode 幂等去重)
自动勾对: 金额 + 反向方向 + valueDate±N天 + 对方账号 模糊匹配
余额调节恒等式:
  银行余额 + 在途(企已记银未记) == 账面余额 + 未达(银已记企未记)
  差额 == 0 才可 RECONCILED
未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ), 下月红冲
```

**涉及机制**:bank-reconciliation.md §业务规则/§余额调节恒等式

---

## UC-FIN-10 FIFO 出库成本与到岸成本

**场景**:出库按 FIFO 消耗队列;到岸成本(运费)分摊入入库成本。

**可验证断言**(见 costing-methods.md):
```
// FIFO 出库
出库移动单 → 按 incomingDate 升序消耗 StockQueue
队列不足 → 跨队列消耗
出库成本 = Σ(各队列消耗量 × 队列单价)

// 到岸成本
运费/保险/关税 → 按金额比例分摊到入库批次
入库成本 += 分摊费用
后续出库按更新后的队列单价计算
```

**涉及机制**:costing-methods.md §FIFO/§到岸成本

---

## UC-FIN-11 预算硬拦截

**场景**:采购订单审核时超预算,被硬拦截。

**可验证断言**(见 budget.md §业务规则):
```
采购订单.审核 →
  调用 IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = 预算(BUDGET凭证) - 承付(COMMITMENT凭证) - 实际(ACTUAL凭证)
  若 余量 < 0 且 控制级别 == HARD:
    返回 BLOCKED → 审核抛异常, 订单保持 SUBMITTED
  若 == WARN: 写日志放行
  若 == NONE: 放行
```

**涉及机制**:budget.md §业务规则/§PostingType

---

## UC-FIN-12 多币种过账

**场景**:外币业务的凭证折算。

**可验证断言**(见 posting.md §多币种):
```
凭证行.本位币金额 == 源币金额 × 汇率
若 汇率缺失 → 报错拒绝过账
外币银行账户对账: 未达账项调整考虑汇兑损益(FX_REVALUATION)
```

**涉及机制**:posting.md §多币种

---

## UC-FIN-13 预算管理(编制/控制/对比)

**场景**:编制年度预算,采购/付款时按预算控制,期末对比预算 vs 实际。见 budget.md。

**可验证断言**:
```
// 预算方案审批即过账(BUDGET 影子凭证)
预算方案.审核通过 →
  生成凭证(postingType=BUDGET, businessType=BUDGET_SCENARIO)
  借贷按 subject.direction 自动取(资产/费用借,负债/收入贷)

// 预算控制(采购订单审核时,强一致校验)
采购订单.审核 →
  IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = BUDGET凭证 - COMMITMENT凭证 - ACTUAL凭证(同维度)
  若 余量 < 0 且 控制级别==HARD: 返回 BLOCKED → 审核抛异常
  若 == WARN: 写 BudgetControlLog 放行

// 承付款
采购订单.APPROVED → 生成 COMMITMENT 凭证
订单 CANCELLED 或发票接收 → 红冲 COMMITMENT

// 预算对比(报表)
按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine
得到 Budget/Commitment/Actual 三列, 无需独立预算余额表
```

**涉及机制**:budget.md §PostingType/§业务规则、cost-center.md

---

## UC-FIN-14 银行对账与未达账项

**场景**:月末银行对账,自动勾对 + 未达账项调整。见 bank-reconciliation.md。

**可验证断言**:
```
// 导入幂等
导入银行对账单 → 以 (fundAccount, statementDate, bankTxnCode) 去重, 重复导入报错

// 自动勾对
按 (金额, 反向方向, valueDate±N天, 对方账号) 模糊匹配
命中唯一 → MATCHED; 多候选 → UNMATCHED; 金额对户名差 → SUSPENSE

// 余额调节恒等式
银行余额 + 在途(企已记银未记) == 账面余额 + 未达(银已记企未记)
差额 == 0 才可 RECONCILED, 否则抛异常

// 未达账项调整
RECONCILED 时若存在未达 → 生成调整凭证(businessType=BANK_RECON_ADJ)
下月初自动红冲(跨期还原)

// 与 AR/AP 核销解耦
银行对账只确认钱到账/已付, 不替代发票核销(ErpFinReconciliation)
```

**涉及机制**:bank-reconciliation.md §业务规则/§余额调节恒等式、ar-ap-reconciliation.md

---

## UC-FIN-15 科目分摊(GL Distribution)

**场景**:凭证写库前,按成本中心分摊规则把一条凭证行拆成多行。见 cost-center.md。

**可验证断言**:
```
原始凭证行(成本中心A, 金额100) →
  命中 GlDistribution 规则(A→A:60%/B:40%) →
  拆为两行: 成本中心A 金额60, 成本中心B 金额40

Σ 拆分行金额 == 原行金额(平衡保持)
若 Σ percent != 100 → 抛异常拒绝过账
分摊由 ErpFinGlDistributionValidator(IErpFinFactsValidator 实现)执行
getOrder() 较高, 确保在其他 Validator 之后
```

**涉及机制**:cost-center.md §ErpFinGlDistribution、posting.md §FactsValidator

---

## UC-FIN-16 财务三大报表

**场景**:基于科目余额生成资产负债表/利润表/现金流量表。

**可验证断言**:
```
// 数据来源
三表基于 ErpFinGlBalance(按 subject×period×维度 聚合的余额)

// 资产负债表(恒等式)
资产合计 == 负债 + 所有者权益

// 利润表
收入 - 成本 - 费用 = 净利润
净利润结转至资产负债表"未分配利润"

// 现金流量表
按科目现金流分类(经营/投资/筹资)调整
间接法: 净利润 + 非现金项目 + 营运资金变动

// 期间控制
报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)
```

**涉及机制**:ErpFinGlBalance、period-close.md、多账套(每账套独立三表)

---



---

## UC-FIN-17 财务看板

**场景**:财务看板的指标展示与异常预警。见 ../dashboards.md §财务看板。

**可验证断言**:
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  收入/支出/净利润, 银行余额, 收支趋势, 预算执行率, 现金流预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**:../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

## 用例与测试的衔接

- 过账(U01/U02)→ finance 核心:验证凭证生成/冲销/业财回链
- 可插拔(U03)→ 验证新增 businessType 零改核心(扩展性测试)
- 结账(U06/U07)→ 期间状态机 + 前置门禁 + 反结账审计
- 核销(U08)→ 多对多核销 + 派生状态
- 成本(U10)→ FIFO 队列消耗 + 到岸成本分摊
- 预算(U11)→ 三 PostingType 并行 + 硬拦截

## 参考机制文档

- `posting.md` — 过账引擎/模板/FactsValidator/冲销/多币种/多账套
- `period-close.md` — 结账步骤/前置门禁/反结账
- `ar-ap-reconciliation.md` — 核销模型/状态/余额/账龄
- `costing-methods.md` — 移动加权/FIFO/批次/到岸成本
- `bank-reconciliation.md` — 银行对账/未达账项/余额调节
- `cost-center.md` — 成本中心/GL Distribution
- `budget.md` — 预算/PostingType/控制
- `multiple-accounting-schemas.md` — 多账套并行
- `state-machine.md` — 凭证/期间状态机
