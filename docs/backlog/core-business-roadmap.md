# 核心业务逻辑路线图

> 最后更新：2026-06-30
> 本路线图覆盖**进销存+财务** 5 域的自定义 BizModel 方法、跨域编排、业财过账。
> 前置条件：`crud-roadmap.md` 中对应域的 CRUD 已完成。

## 阶段状态

- P1. 核心业务循环：`todo`
- P4. 业财一体端到端：`todo`

## 实施顺序

### P1 — 核心业务循环

| # | 工作项 | 域 | 设计文档 |
|---|--------|-----|---------|
| 1.0a | 采购申请审批→转订单逻辑 | purchase | `purchase/requisition.md` |
| 1.0b | 销售报价单审批→转订单逻辑 | sales | `sales/quotation.md` |
| 1.1 | Purchase Order BizModel（审批/入库触发/过账） | purchase | `purchase/state-machine.md` |
| 1.2 | Sales Order BizModel（审批/出库触发/过账） | sales | `sales/state-machine.md` |
| 1.3 | StockMove BizModel（库存移动/流水/余额） | inventory | `inventory/state-machine.md` |
| 1.4 | 三单匹配逻辑（PO/Receive/Invoice） | purchase | `purchase/three-way-match.md` |
| 1.5 | IErpFinAcctDocProvider 过账 Provider | finance | `finance/posting.md` |
| 1.6 | 采购到付款端到端串联 | purchase/finance | `flow-overview.md` |
| 1.7 | 销售到收款端到端串联 | sales/finance | `flow-overview.md` |
| 1.8 | 费用报销/票据/资金模块业务逻辑 | finance | `expense-claim.md`, `treasury.md` |
| 1.9 | 采购退货与退款 | purchase/finance | `purchase/returns.md` |
| 1.10 | 销售退货与退款 | sales/finance | `sales/returns.md` |
| 1.11 | 批次追溯链逻辑 | inventory | `inventory/trace-chain.md` |

### P4 — 业财一体端到端

| # | 工作项 | 涉及域 |
|---|--------|---------|
| 4.1 | 采购到付款全链路测试（PO→Receive→Invoice→Pay） | purchase/finance |
| 4.2 | 销售到收款全链路测试（SO→Delivery→Invoice→Receipt） | sales/finance |
| 4.3 | 期末结账全流程（成本核算→汇兑重估→结转损益→关账） | finance |
| 4.4 | 采购/销售退货到退款全链路 | purchase/sales/finance |

## 参考示例

第一个 `task.xml` 文件参考：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/_task/ErpPurOrder/approve.task.xml`，展示了审批流的标准编排模式（校验→规则→分支→I*Biz→后处理）。
