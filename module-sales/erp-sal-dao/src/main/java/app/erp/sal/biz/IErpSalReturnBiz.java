
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalReturn;

import java.util.List;

/**
 * 销售退货单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>审批状态机（对齐 {@code docs/design/sales/returns.md} 与 {@code state-machine.md}；复用现有
 * {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段）。approve 触发库存反向入库移动 +
 * SALES_RETURN 过账 + 回减客户应收余额。reverseApprove 前置冲销已生成库存移动单 + 红字冲销已过账凭证。
 * 每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalReturnBiz extends ICrudBiz<ErpSalReturn> {

    @BizMutation
    ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context);

    /**
     * 换货出库单生成（UC-SAL-06 断言②④，RC-R1.51 P1-RC-025，D1 选项 A）。
     * 前置：returnType=EXCHANGE 且已 APPROVED + 源出库已审核 + 期间 OPEN + 发票未核销（复用 R1.19 守卫族）。
     * 换货行入参（materialId/skuId/uoMId/quantity/unitPrice/taxRate）缺省复制退货行。
     * 同事务双写双向关联（ErpSalReturn.exchangeDeliveryId ↔ ErpSalDelivery.exchangeReturnId）；
     * 已生成时幂等拒绝（ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED）。
     * 价差分支（D3 头级口径）：Δ>0 补差价开票（经 IErpSalInvoiceBiz.save 既有入口，DRAFT 待操作员审核）；
     * Δ<0 退款（复用 ReturnRefundOrchestrator 既有 reverse-settlement 能力）；Δ=0 无动作。
     */
    @BizMutation
    ErpSalReturn generateExchangeDelivery(@Name("returnId") Long returnId,
                                          @Name("lines") List<ErpSalExchangeDeliveryLine> lines,
                                          IServiceContext context);
}
