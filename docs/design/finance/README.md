# 财务域（finance）

## 目的

说明财务域的业务语义、凭证模型、状态机、业财打通机制与期末结账。财务域是"业财一体化"的核心：业务单据自动生成会计凭证，实现业务与财务数据的一致。

## 边界

- 本域负责：会计凭证、凭证分录行、凭证模板、业财回链、科目（过账逻辑）、会计期间、期末结账、账户、成本核算、总账。
- 本域不负责：科目表/科目主数据维护（master-data 域共享）；业务单据本身（purchase/sales/inventory 域）。
- 持久化字段、字典、状态码以 `model/app-erp-finance.orm.xml` 为准。
- 跨域协作规则见 `../domain-design-guidelines.md`，业财打通总览见 `../flow-overview.md` 的 L3 节，详细规则见 [`posting.md`](posting.md)。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-finance` |
| appName | `app-erp-fin` |
| 权威模型 | `model/app-erp-finance.orm.xml` |
| 实体包 | `app.erp.fin.dao.entity` |
| 表前缀 | `erp_fin_` |
| 类名前缀 | `ErpFin*` |
| 字典命名空间 | `erp-fin/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 会计凭证（Voucher） | 一次复式记账的完整记录，含凭证字（收/付/转）、凭证号、凭证日期 |
| 凭证分录行（VoucherLine） | 凭证内的一条借贷分录，只填借方或贷方一侧，关联科目 |
| 凭证模板（VoucherTemplate） | 预定义借贷模板，业务单据触发时按模板生成凭证 |
| 业财回链（VoucherBillR） | 凭证与业务单据的双向关联，保证生命周期一致 |
| 会计期间（AccountingPeriod） | 财务结账的时间区间（月/季/年） |
| 账户（Account/FundAccount） | 银行/现金账户，收付款资金流向 |
| 总账（GL） | 按科目汇总的账簿（派生视图，非独立表） |
| 成本核算（Costing） | 存货成本计算（移动加权平均/FIFO/批次） |

### 凭证模型（中式复式记账）

```
会计凭证（Voucher）
  ├─ 凭证头：凭证字（收/付/转）、凭证号（按字分类连续编号）、凭证日期、状态
  └─ 凭证分录行（VoucherLine）[]
       ├─ 科目（subjectCode）
       ├─ 借方金额（drAmount）或贷方金额（crAmount）—— 每行只填一侧
       ├─ 摘要（memo）
       ├─ 业务维度（往来单位/部门/项目等辅助核算）
       └─ 源币种金额 + 本位币金额（多币种）

业财回链（VoucherBillR）
  ├─ 凭证号（voucherHeadCode）
  ├─ 业务单据类型（billType：PURCHASE_INPUT/SALES_OUTPUT/AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT）
  └─ 业务单据号（billHeadCode）
```

### 借贷平衡规则

- 每张凭证的借方合计 = 贷方合计（必须平衡，否则不可过账）。
- 每行只填借方（drAmount）或贷方（crAmount）一侧，另一侧为 0。

## 状态机

凭证有独立状态机（草稿→已过账→红冲/作废），会计期间有独立状态机（未开启→已开启→结账中→已结账），两类状态机相互耦合约束。按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度设计。详细规则见 [`state-machine.md`](state-machine.md)。

## 业财打通

业务单据审核通过时自动触发凭证生成。详细规则见 [`posting.md`](posting.md)。

| 业务单据 | businessType | 凭证方向 |
|----------|--------------|----------|
| 采购入库 | PURCHASE_INPUT | 存货增加、暂估应付 |
| 销售出库 | SALES_OUTPUT | 存货减少、结转成本 |
| 采购发票 | AP_INVOICE | 确认应付、进项税 |
| 销售发票 | AR_INVOICE | 确认应收、收入、销项税 |
| 付款 | PAYMENT | 应付减少、资金减少 |
| 收款 | RECEIPT | 应收减少、资金增加 |

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 采购发票 → 应付凭证 | purchase | 本域监听发票审核事件，按 AP_INVOICE 模板生成凭证 |
| 付款 → 付款凭证 | purchase | 本域监听付款单审核事件，按 PAYMENT 模板生成凭证 |
| 销售发票 → 应收凭证 | sales | 本域监听发票审核事件，按 AR_INVOICE 模板生成凭证 |
| 收款 → 收款凭证 | sales | 本域监听收款单审核事件，按 RECEIPT 模板生成凭证 |
| 库存移动 → 存货估值凭证 | inventory | 本域监听移动单完成事件，按 SALES_OUTPUT/PURCHASE_INPUT 生成凭证 |
| 科目引用 | master-data | 凭证分录行引用科目编码 + `IErpMdAccountBiz` |

## 关键业务规则

1. **借贷平衡**：每张凭证借方合计 = 贷方合计，否则不可过账。
2. **凭证号连续**：按凭证字（收/付/转）分类连续编号，符合中国"字第号"规范。
3. **业财回链**：每张业务生成的凭证通过回链表关联源单据，作废源单据时自动冲销凭证。
4. **多币种**：凭证分录行同时记录源币种金额与本位币金额，按业务日期汇率转换。
5. **多套科目表**：支持多套会计科目表并行（管理账/税务账），同一业务在多套下各出一组凭证。
6. **辅助核算**：凭证分录行可挂业务维度（往来单位/部门/项目/仓库），支持多维报表。
7. **期间控制**：已结账的会计期间不允许新增/修改凭证。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、凭证模型、跨域协作 |
| `state-machine.md` | 凭证状态机、会计期间状态 |
| `posting.md` | 业财打通机制、凭证模板、过账引擎、科目映射 |
| `period-close.md` | 期末结账流程、成本核算、反结账机制 |
| `ar-ap-reconciliation.md` | 应收应付核销机制、余额计算、账龄分析 |
| `multiple-accounting-schemas.md` | 多账套并行核算机制、账套管理、账套转换 |

## 实现落位提示

| 设计含义 | 默认实现落位 |
|----------|--------------|
| 借贷平衡校验、凭证号生成、金额合计 | Entity（稳定领域事实、只读 helper） |
| 凭证 CRUD、过账/作废动作、期间校验 | BizModel（事务入口） |
| 业务单据触发生成凭证的多步骤编排 | Processor（按 businessType 选模板 + 填充金额 + 写凭证 + 写回链） |
| 凭证生成规则可插拔（垂直行业定制） | `IErpFinAcctDocProvider` 接口 + `ErpFinAcctDocRegistry`（注入 `List<IErpFinAcctDocProvider>`） |
| 过账异步化（不阻塞业务单据） | post-commit 事件 + `posted` 标志兜底扫描 |
| 科目映射多维决策 | 规则引擎或元数据驱动（按伙伴组/产品类别/项目/仓库 → 科目） |
