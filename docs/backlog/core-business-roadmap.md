# Core Business Logic Roadmap

> 最后更新：2026-07-02
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
- 1.6 采购到付款串联：`partial`（AP 段 PO→Receive→Invoice→Pay + 三单匹配 + 域级核销 done，计划 0300-1；**财务辅助账 ErpFinArApItem 生成 + 正式核销单 ErpFinReconciliation + 往来余额/账龄 done，计划 0300-3**；自动核销/退货归 0300-3 Non-Goal / 1.9）
- 1.7 销售到收款串联：`partial`（AR 段 SO→Delivery→Invoice→Receipt + AR_INVOICE/RECEIPT 过账 + 域级核销 done，计划 0300-2；**财务辅助账 + 正式核销单 + 往来余额/账龄 done，计划 0300-3**；自动核销/汇兑损益归 0300-3 Non-Goal，销售退货归 1.10）
- 1.8 费用报销/票据/资金：`partial`（费用报销 + 员工借款子面 done，计划 0700-2：三实体 + 三轴审批 + EXPENSE_CLAIM/EMPLOYEE_ADVANCE/EMPLOYEE_ADVANCE_SETTLE 过账 + 员工应付/预支双方向辅助账 + 报销抵扣借款净额核销；票据/资金属同工作项另一结果表面，归后续计划）
- 1.9 采购退货与退款：`done`（计划 0456-1：退货单三轴审批状态机 + 库存反向出库 + PURCHASE_RETURN 红字冲减凭证 + DIRECTION_PAYABLE 负 openAmount 辅助账回减应付；红字发票自动生成/换货/批次退货/现金退款归 Non-Goal）
- 1.10 销售退货与退款：`done`（计划 0456-2：退货单三轴审批状态机 + 库存反向入库 + SALES_RETURN 反向 SALES_OUTPUT 凭证（借存货/贷成本）+ DIRECTION_RECEIVABLE 负 openAmount 辅助账回减应收 + 已收款退货反向收款核销；红字发票自动生成/退款方式路由/换货/退货质检/批次退货归 Non-Goal）
- 1.11 批次追溯链：`done`（计划 0700-1：移动单自追溯上链 originMoveId/originReturnedMoveId + 四类追溯查询 forward/backward/return/batch + 退货移动单透传挂链）

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
| 1.6 | 采购到付款端到端串联 | purchase/finance | `flow-overview.md` | 🔶 `partial`（AP 段 done 计划 0300-1；辅助账+核销+账龄 done 计划 0300-3；自动核销/退货归 0300-3 Non-Goal / 1.9） |
| 1.7 | 销售到收款端到端串联 | sales/finance | `flow-overview.md` | 🔶 `partial`（AR 段 done 计划 0300-2；辅助账+核销+账龄 done 计划 0300-3；自动核销/汇兑损益归 0300-3 Non-Goal，退货归 1.10） |
| 1.8 | 费用报销/票据/资金模块业务逻辑 | finance | `expense-claim.md`, `treasury.md` | 🔶 `partial`（费用报销+员工借款子面 done 计划 0700-2；票据/资金归后续计划） |
| 1.9 | 采购退货与退款 | purchase/finance | `purchase/returns.md` | ✅ `done`（计划 0456-1） |
| 1.10 | 销售退货与退款 | sales/finance | `sales/returns.md` | ✅ `done`（计划 0456-2：三轴审批 + 反向入库 + SALES_RETURN 凭证 + 负 AR 辅助账回减应收 + 反向收款核销） |
| 1.11 | 批次追溯链逻辑 | inventory | `inventory/trace-chain.md` | ✅ `done`（计划 0700-1：单 uplink 自追溯链 + 四类追溯查询 + 退货透传挂链） |

### M4 — 业财一体端到端

| # | 工作项 | 涉及域 |
|---|--------|---------|
| 4.1 | 采购到付款全链路测试（PO→Receive→Invoice→Pay） | purchase/finance |
| 4.2 | 销售到收款全链路测试（SO→Delivery→Invoice→Receipt） | sales/finance |
| 4.3 | 期末结账全流程（成本核算→汇兑重估→结转损益→关账） | finance |
| 4.4 | 采购/销售退货到退款全链路 | purchase/sales/finance |

## Reference

第一个 `task.xml` 文件参考：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/_task/ErpPurOrder/approve.task.xml`，展示了审批流的标准编排模式（校验→规则→分支→I*Biz→后处理）。
