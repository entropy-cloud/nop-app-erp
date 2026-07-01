
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalReceipt;

import java.util.List;

/**
 * 收款单业务接口。除标准 CRUD 外，定义三轴审批状态机（对齐 {@code docs/design/sales/state-machine.md}）
 * + 域级核销契约（{@code ar-ap-reconciliation.md §核销}，sales 域 {@code ErpSalReceiptLine} 载体）：
 *
 * <ul>
 *   <li>{@link #submit}/{@link #withdrawSubmit}/{@link #approve}/{@link #reject}/{@link #reverseApprove}/
 *       {@link #cancel}：三轴审批，同 {@code IErpSalInvoiceBiz} 形状。approve 触发 RECEIPT 过账（借银行存款/贷应收）。</li>
 *   <li>{@link #settle}：收款审核后独立核销动作（MVP 解耦审核与核销，见 Phase 2 Decision (b)），按分配明细登记
 *       {@code ErpSalReceiptLine}，回写发票 receivedStatus/receivedAmount 与收款 writtenOffStatus。</li>
 *   <li>{@link #reverseSettlement}：生成反向 ReceiptLine（冲销），恢复发票/收款余额与状态。</li>
 * </ul>
 */
public interface IErpSalReceiptBiz extends ICrudBiz<ErpSalReceipt> {

    @BizMutation
    ErpSalReceipt submit(@Name("receiptId") Long receiptId, IServiceContext context);

    @BizMutation
    ErpSalReceipt withdrawSubmit(@Name("receiptId") Long receiptId, IServiceContext context);

    @BizMutation
    ErpSalReceipt approve(@Name("receiptId") Long receiptId, IServiceContext context);

    @BizMutation
    ErpSalReceipt reject(@Name("receiptId") Long receiptId, IServiceContext context);

    @BizMutation
    ErpSalReceipt reverseApprove(@Name("receiptId") Long receiptId, IServiceContext context);

    @BizMutation
    ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context);

    /**
     * 域级核销：按分配明细将收款金额核销到指定发票（多对多）。约束：同客户、双方 approveStatus=APPROVED、
     * 核销金额不超发票未收余额与收款未核销余额。核销后回写发票 receivedAmount/receivedStatus 与收款 writtenOffStatus。
     */
    @BizMutation
    ErpSalReceipt settle(@Name("receiptId") Long receiptId,
                         @Name("allocations") List<SettlementAllocation> allocations,
                         IServiceContext context);

    /**
     * 核销冲销：对指定发票生成反向 ReceiptLine（负金额），恢复发票/收款余额与状态。
     */
    @BizMutation
    ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,
                                    @Name("invoiceId") Long invoiceId,
                                    IServiceContext context);
}
