# 合同全生命周期管理（contract）

## 目的

设计合同从谈判 → 签署 → 执行 → 续期/终止的全生命周期管理。覆盖采购合同、销售合同、劳动合同、服务合同的统一管理。`purchase/README.md` 的抬头级合同（字段级）是采购单上的合同引用，本文是独立合同管理模块。

## 边界

- 本模块负责：合同模板库、合同版本管理（起草→审批→签署→归档）、合同变更单（Amendment）、开票计划（InvoicePlan）、到期提醒/续期、用量/消耗计费（Consumption）。
- **与 purchase/sales 的边界**：采购合同关联供应商和采购订单，销售合同关联客户和销售订单。合同是**框架协议**层，订单是执行层。
- 本模块不负责：订单级执行（purchase/sales 域）；法律条款的自动审核（属法律专家系统）。
- 持久化字段、字典、状态码以 `module-contract/model/app-erp-contract.orm.xml` 为权威源。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-contract` |
| appName | `erp-ct`（两级） |
| 权威模型 | `module-contract/model/app-erp-contract.orm.xml` |
| 实体包 | `app.erp.contract.dao.entity` |
| 表前缀 | `erp_ct_` |
| 类名前缀 | `ErpCt*` |
| 字典命名空间 | `erp-ct/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 合同头（ErpCtContract） | 一份合同的主记录：合同名称、类型（采购/销售/劳动/服务）、方向（进/出）、对方往来单位、币种、总额、有效期、签署日期、状态、合同模板、父合同（变更单关联原合同） |
| 合同行（ErpCtContractLine） | 合同下的产品/物料行：物料/产品（框架合同可不指定）、描述、数量（框架合同为预估总量）、单价、金额、交货日期 |
| 合同版本（ErpCtContractVersion） | 合同的版本快照：版本号、版本日期、条款变更说明、版本文件、是否当前版本、版本状态（草稿/定稿/签署）、批准人/时间。每次变更生成新版本，原版本保留以供审计追溯 |
| 开票计划（ErpCtInvoicePlan） | 按合同条款生成的开票安排：计划开票日期、金额、是否已开票、关联发票、实际开票日期、开票条款（预付款/里程碑/月结/完工） |
| 消耗计费行（ErpCtConsumptionLine） | 用量计费场景的消耗记录：消耗日期、数量、单价、金额、来源业务单。适用于 SaaS 订阅等按实际用量结算的合同 |
| 合同模板（ErpCtTemplate） | 合同模板库：适用合同类型、模板内容（占位符 + 条款）、是否启用 |

字段、类型、精度、字典码以 `module-contract/model/app-erp-contract.orm.xml` 为权威源。

## 状态机

合同头状态：`DRAFT（起草） → NEGOTIATION（谈判中） → ACTIVE（执行中） → EXPIRED（到期）`；`ACTIVE → SUSPENDED（中止） → ACTIVE（恢复）`；`ACTIVE/NEGOTIATION → TERMINATED（终止）`；`DRAFT → CANCELLED`。详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 采购合同关联采购订单 | purchase | 合同与 PO 弱指针关联（`relatedBillType/relatedBillCode`），合同已执行金额由 PO 回写 |
| 销售合同关联销售订单 | sales | 同上弱指针模式 |
| 开票计划触发生成发票 | finance | 开票计划生成 AP/AR 发票草稿，走标准过账 |
| 劳动合同关联员工 | hr | 劳动合同关联员工主数据 |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **合同版本管理**：每次合同变更（Amendment）创建一个新版本，原版本保留。当前版本 `isCurrent=true`，审计可追溯历史版本。
2. **开票计划驱动**：InvoicePlan 按合同条款生成开票计划（预付 30%/里程碑 50%/完工 20%），自动生成 AP/AR 发票草稿。
3. **用量计费**：ConsumptionLine 记录实际消耗量（如 SaaS 订阅的 API 调用次数/存储空间），周期结束时汇总生成发票。
4. **到期提醒**：endDate 前 30/15/7 天通过 nop-job 发送到期提醒通知。
5. **合同与订单弱关联**：合同与订单通过 `relatedBillType/relatedBillCode` 弱指针关联，订单执行回写合同已执行金额。

## 业财过账

合同本身不产生会计凭证。**开票计划触发的发票**（AP/AR invoice）走 purchase/sales 域的标准过账流程。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-ct.reminder-days-before-expiry` | 30 | 合同到期前多少天开始提醒 |
| `erp-ct.auto-create-renewal-draft` | false | 到期时是否自动创建续期草稿 |

## 菜单归属

purchase 域「合同管理」分组：合同列表、合同模板、开票计划、消耗记录。

## 反模式警示

- ⛔ **合同无版本管理**——每次修改直接覆盖原合同文件，丢失审计保留。必须用 ContractVersion。
- ⛔ **开票计划与合同行耦合**——InvoicePlan 是独立实体，支持按时间/里程碑/用量多种模式。
- ⛔ **合同与订单通过外键强耦合**——用弱指针 `relatedBillType/relatedBillCode`。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、合同生命周期模型、跨域协作 |
| `state-machine.md` | 合同头状态机、版本状态 |
| `approval-workflow.md` | 合同审批工作流 |
| `contract-repository.md` | 合同归档与检索 |
| `e-signature.md` | 电子签署集成 |
| `volume-discount.md` | 合同量价折扣 |
| `use-cases.md` | 用例说明 |
| `ui-patterns.md` | 页面与交互模式 |

## 参考

- 合同全生命周期设计参考业界完整合同管理实践（源码分析见 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理）
- `docs/design/purchase/README.md`（采购域）
- `docs/design/sales/README.md`（销售域）
