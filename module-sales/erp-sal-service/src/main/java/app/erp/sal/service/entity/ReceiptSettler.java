package app.erp.sal.service.entity;

import app.erp.md.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReceiptLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 收款→发票域级核销器（{@code docs/design/finance/ar-ap-reconciliation.md §核销}，sales 域
 * {@link ErpSalReceiptLine} 载体）。核销在收款审核后由独立 {@code settle} 动作触发（MVP 解耦审核与核销，
 * 见计划 Phase 2 Decision (b)，与采购域 PaymentSettler 对称）。
 *
 * <p>核销约束（{@code state-machine.md §场景D}）：同客户、双方 approveStatus=APPROVED、核销金额不超发票
 * 未收余额（{@code totalAmountWithTax − receivedAmount}）与收款未核销余额。违例抛 {@link ErpSalErrors#ERR_SETTLE_*}。
 *
 * <p>回写（派生状态，{@code state-machine.md §收款状态机}）：
 * <ul>
 *   <li>发票 {@code receivedAmount} = 该发票全部 ReceiptLine 金额之和（跨多收款单，含反向负金额行）；{@code receivedStatus}
 *       按累计 vs 含税总额判定 UNRECEIVED/PARTIAL/RECEIVED。</li>
 *   <li>收款 {@code writtenOffStatus} 按已核销 vs 收款总额判定。</li>
 * </ul>
 * 反向核销生成负金额 ReceiptLine（保留审计轨迹），余额与状态据此自然回退。
 *
 * <p>本类为非 BizModel 服务（intra-module 聚合写），对齐 {@code PaymentSettler} 用 {@code daoFor}
 * 处理同模块实体（发票/核销行）的模式；不跨 REQUIRES_NEW 边界（核销为纯域内操作，不过账）。
 */
public class ReceiptSettler {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * 按分配明细核销收款到发票。返回更新后的收款单（余额/状态已回写）。
     */
    public ErpSalReceipt settle(ErpSalReceipt receipt, List<SettlementAllocation> allocations) {
        if (receipt.getApproveStatus() == null
                || !Objects.equals(receipt.getApproveStatus(), ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpSalErrors.ERR_SETTLE_RECEIPT_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, receipt.getApproveStatus());
        }
        if (allocations == null || allocations.isEmpty()) {
            return receipt;
        }

        BigDecimal receiptSettled = sumReceiptLines(receipt.getId());
        BigDecimal receiptTotal = nz(receipt.getTotalAmount());
        BigDecimal receiptRemaining = receiptTotal.subtract(receiptSettled);

        Map<Long, BigDecimal> touchedInvoices = new HashMap<>();
        IEntityDao<ErpSalReceiptLine> lineDao = daoProvider.daoFor(ErpSalReceiptLine.class);
        for (SettlementAllocation alloc : allocations) {
            if (alloc.getInvoiceId() == null || alloc.getAmount() == null) {
                continue;
            }
            BigDecimal amount = alloc.getAmount();
            if (amount.signum() <= 0) {
                continue;
            }
            ErpSalInvoice invoice = requireInvoiceForSettle(receipt, alloc.getInvoiceId());

            BigDecimal invoiceBalance = nz(invoice.getTotalAmountWithTax()).subtract(nz(invoice.getReceivedAmount()));
            if (amount.compareTo(invoiceBalance) > 0) {
                throw new NopException(ErpSalErrors.ERR_SETTLE_OVER_INVOICE_BALANCE)
                        .param(ErpSalErrors.ARG_SETTLE_AMOUNT, amount)
                        .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                        .param(ErpSalErrors.ARG_INVOICE_BALANCE, invoiceBalance);
            }
            if (amount.compareTo(receiptRemaining) > 0) {
                throw new NopException(ErpSalErrors.ERR_SETTLE_OVER_RECEIPT_BALANCE)
                        .param(ErpSalErrors.ARG_SETTLE_AMOUNT, amount)
                        .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                        .param(ErpSalErrors.ARG_RECEIPT_BALANCE, receiptRemaining);
            }

            ErpSalReceiptLine line = lineDao.newEntity();
            line.setReceiptId(receipt.getId());
            line.setInvoiceId(alloc.getInvoiceId());
            line.setAmount(amount);
            lineDao.saveEntity(line);

            receiptRemaining = receiptRemaining.subtract(amount);
            touchedInvoices.merge(alloc.getInvoiceId(), amount, BigDecimal::add);
        }

        for (Long invoiceId : touchedInvoices.keySet()) {
            recomputeInvoiceReceived(invoiceId);
        }
        recomputeReceiptWrittenOff(receipt.getId());
        return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());
    }

    /**
     * 核销冲销：对指定发票生成反向（负金额）ReceiptLine，恢复余额与状态。幂等：无既有核销则空操作。
     */
    public ErpSalReceipt reverseSettlement(ErpSalReceipt receipt, Long invoiceId) {
        List<ErpSalReceiptLine> existing = findLines(receipt.getId(), invoiceId);
        BigDecimal settled = BigDecimal.ZERO;
        for (ErpSalReceiptLine l : existing) {
            settled = settled.add(nz(l.getAmount()));
        }
        if (settled.signum() == 0) {
            return receipt;
        }

        IEntityDao<ErpSalReceiptLine> lineDao = daoProvider.daoFor(ErpSalReceiptLine.class);
        ErpSalReceiptLine reversal = lineDao.newEntity();
        reversal.setReceiptId(receipt.getId());
        reversal.setInvoiceId(invoiceId);
        reversal.setAmount(settled.negate());
        reversal.setRemark("核销冲销");
        lineDao.saveEntity(reversal);

        recomputeInvoiceReceived(invoiceId);
        recomputeReceiptWrittenOff(receipt.getId());
        return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());
    }

    // ---------- helpers ----------

    private ErpSalInvoice requireInvoiceForSettle(ErpSalReceipt receipt, Long invoiceId) {
        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        if (invoice == null) {
            throw new NopException(ErpSalErrors.ERR_SETTLE_INVOICE_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_INVOICE_ID, invoiceId);
        }
        if (invoice.getCustomerId() == null || !invoice.getCustomerId().equals(receipt.getCustomerId())) {
            throw new NopException(ErpSalErrors.ERR_SETTLE_CUSTOMER_MISMATCH)
                    .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                    .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
        if (invoice.getApproveStatus() == null
                || !Objects.equals(invoice.getApproveStatus(), ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpSalErrors.ERR_SETTLE_INVOICE_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, invoice.getApproveStatus());
        }
        return invoice;
    }

    private void recomputeInvoiceReceived(Long invoiceId) {
        ormTemplate.flushSession();
        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        BigDecimal received = sumInvoiceLines(invoiceId);
        invoice.setReceivedAmount(received);
        BigDecimal withTax = nz(invoice.getTotalAmountWithTax());
        String status;
        if (received.signum() <= 0) {
            status = ErpSalConstants.RECEIVED_STATUS_UNRECEIVED;
        } else if (received.compareTo(withTax) >= 0) {
            status = ErpSalConstants.RECEIVED_STATUS_RECEIVED;
        } else {
            status = ErpSalConstants.RECEIVED_STATUS_PARTIAL;
        }
        invoice.setReceivedStatus(status);
        daoProvider.daoFor(ErpSalInvoice.class).updateEntity(invoice);
    }

    private void recomputeReceiptWrittenOff(Long receiptId) {
        ormTemplate.flushSession();
        ErpSalReceipt receipt = daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receiptId);
        BigDecimal settled = sumReceiptLines(receiptId);
        BigDecimal total = nz(receipt.getTotalAmount());
        String status;
        if (settled.signum() <= 0) {
            status = ErpSalConstants.RECEIVED_STATUS_UNRECEIVED;
        } else if (settled.compareTo(total) >= 0) {
            status = ErpSalConstants.RECEIVED_STATUS_RECEIVED;
        } else {
            status = ErpSalConstants.RECEIVED_STATUS_PARTIAL;
        }
        receipt.setWrittenOffStatus(status);
        daoProvider.daoFor(ErpSalReceipt.class).updateEntity(receipt);
    }

    private BigDecimal sumInvoiceLines(Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("invoiceId", invoiceId));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpSalReceiptLine l : daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q)) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private BigDecimal sumReceiptLines(Long receiptId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiptId", receiptId));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpSalReceiptLine l : daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q)) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private List<ErpSalReceiptLine> findLines(Long receiptId, Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiptId", receiptId));
        q.addFilter(eq("invoiceId", invoiceId));
        return new ArrayList<>(daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q));
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
