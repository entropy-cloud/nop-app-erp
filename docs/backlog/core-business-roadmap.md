# Core Business Logic Roadmap

> 最后更新：2026-07-01
> 本路线图覆盖**进销存+财务** 5 域的自定义 BizModel 方法、跨域编排、业财过账。
> 前置条件：`crud-roadmap.md` 中对应域的 CRUD 已完成。

## Work Item Status

> 状态在工作项上；Milestone 仅为分组。各工作项的实施记录见 Implementation Order。

### Milestone M1 — 核心业务循环
- 1.0a 采购申请审批→转订单逻辑：`done`
- 1.0b 销售报价单审批→转订单逻辑：`done`
- 1.1 Purchase Order BizModel：`partial`
- 1.2 Sales Order BizModel：`partial`（审批/信用额度 + 出库触发段 done；过账/发票/收款仍 todo，归 1.7）
- 1.3 StockMove BizModel：`done`
- 1.4 三单匹配逻辑：`done`
- 1.5 过账 Provider：`done`
- 1.6 采购到付款串联：`partial`（AP 段 PO→Receive→Invoice→Pay + 三单匹配 + 域级核销 done，计划 0300-1；财务辅助账/自动核销/退货归 0300-3/1.9）
- 1.7 销售到收款串联：`todo`
- 1.8 费用报销/票据/资金：`todo`
- 1.9 采购退货与退款：`todo`
- 1.10 销售退货与退款：`todo`
- 1.11 批次追溯链：`todo`

### Milestone M4 — 业财一体端到端
- 4.1–4.4：`todo`

## Implementation Order

### M1 — 核心业务循环

| # | 工作项 | 域 | 设计文档 | 状态 |
|---|--------|-----|---------|------|
| 1.0a | 采购申请审批→转订单逻辑 | purchase | `purchase/requisition.md` | ✅ `done` |
| 1.0b | 销售报价单审批→转订单逻辑 | sales | `sales/quotation.md` | ✅ `done` |
| 1.1 | Purchase Order BizModel（审批/入库触发/过账） | purchase | `purchase/state-machine.md` | 🔶 `partial` |
| 1.2 | Sales Order BizModel（审批/出库触发/过账） | sales | `sales/state-machine.md` | 🔶 `partial`（审批+信用额度+出库触发 done；过账归 1.7） |
| 1.3 | StockMove BizModel（库存移动/流水/余额） | inventory | `inventory/state-machine.md` | ✅ `done` |
| 1.4 | 三单匹配逻辑（PO/Receive/Invoice） | purchase | `purchase/three-way-match.md` | ✅ `done` |
| 1.5 | IErpFinAcctDocProvider 过账 Provider | finance | `finance/posting.md` | ✅ `done` |
| 1.6 | 采购到付款端到端串联 | purchase/finance | `flow-overview.md` | 🔶 `partial`（AP 段 done 计划 0300-1；辅助账/自动核销/退货归 0300-3/1.9） |
| 1.7 | 销售到收款端到端串联 | sales/finance | `flow-overview.md` | — |
| 1.8 | 费用报销/票据/资金模块业务逻辑 | finance | `expense-claim.md`, `treasury.md` | — |
| 1.9 | 采购退货与退款 | purchase/finance | `purchase/returns.md` | — |
| 1.10 | 销售退货与退款 | sales/finance | `sales/returns.md` | — |
| 1.11 | 批次追溯链逻辑 | inventory | `inventory/trace-chain.md` | — |

### M4 — 业财一体端到端

| # | 工作项 | 涉及域 |
|---|--------|---------|
| 4.1 | 采购到付款全链路测试（PO→Receive→Invoice→Pay） | purchase/finance |
| 4.2 | 销售到收款全链路测试（SO→Delivery→Invoice→Receipt） | sales/finance |
| 4.3 | 期末结账全流程（成本核算→汇兑重估→结转损益→关账） | finance |
| 4.4 | 采购/销售退货到退款全链路 | purchase/sales/finance |

## Reference

第一个 `task.xml` 文件参考：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/_task/ErpPurOrder/approve.task.xml`，展示了审批流的标准编排模式（校验→规则→分支→I*Biz→后处理）。
