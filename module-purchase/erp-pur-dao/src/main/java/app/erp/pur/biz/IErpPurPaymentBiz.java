
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurPayment;

import java.util.List;

/**
 * 付款单业务接口。除标准 CRUD 外，定义三轴审批状态机（对齐 {@code docs/design/purchase/state-machine.md}）
 * + 域级核销契约（{@code ar-ap-reconciliation.md §核销}，purchase 域 {@code ErpPurPaymentLine} 载体）：
 *
 * <ul>
 *   <li>{@link #submit}/{@link #withdrawSubmit}/{@link #approve}/{@link #reject}/{@link #reverseApprove}/
 *       {@link #cancel}：三轴审批，同 {@code IErpPurInvoiceBiz} 形状。approve 触发 PAYMENT 过账（借应付/贷银行存款）。</li>
 *   <li>{@link #settle}：付款审核后独立核销动作（MVP 解耦审核与核销，见 Phase 2 Decision (b)），按分配明细登记
 *       {@code ErpPurPaymentLine}，回写发票 paidStatus/paidAmount 与付款 writtenOffStatus。</li>
 *   <li>{@link #reverseSettlement}：生成反向 PaymentLine（冲销），恢复发票/付款余额与状态。</li>
 * </ul>
 */
public interface IErpPurPaymentBiz extends ICrudBiz<ErpPurPayment> {

    @BizMutation
    ErpPurPayment submit(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment withdrawSubmit(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment approve(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment reject(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment reverseApprove(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context);

    /**
     * 域级核销：按分配明细将付款金额核销到指定发票（多对多）。约束：同供应商、双方 approveStatus=APPROVED、
     * 核销金额不超发票未付余额与付款未核销余额。核销后回写发票 paidAmount/paidStatus 与付款 writtenOffStatus。
     */
    @BizMutation
    ErpPurPayment settle(@Name("paymentId") Long paymentId,
                         @Name("allocations") List<SettlementAllocation> allocations,
                         IServiceContext context);

    /**
     * 核销冲销：对指定发票生成反向 PaymentLine（负金额），恢复发票/付款余额与状态。
     */
    @BizMutation
    ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                    @Name("invoiceId") Long invoiceId,
                                    IServiceContext context);
}
