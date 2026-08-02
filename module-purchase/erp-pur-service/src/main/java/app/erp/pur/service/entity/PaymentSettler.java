package app.erp.pur.service.entity;

import app.erp.md.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurPaymentLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
 * 付款→发票域级核销器（{@code docs/design/finance/ar-ap-reconciliation.md §核销}，purchase 域
 * {@link ErpPurPaymentLine} 载体）。核销在付款审核后由独立 {@code settle} 动作触发（MVP 解耦审核与核销，
 * 见计划 Phase 2 Decision (b)）。
 *
 * <p>核销约束（{@code state-machine.md §场景D}）：同供应商、双方 approveStatus=APPROVED、核销金额不超发票
 * 未付余额（{@code totalAmountWithTax − paidAmount}）与付款未核销余额。违例抛 {@link ErpPurErrors#ERR_SETTLE_*}。
 *
 * <p>R1.8 P1-MA2-003 方案 A：付款核销三单匹配二次门控（{@code three-way-match.md §匹配时机「付款前最终校验」}）。
 * 经 config {@code erp-pur.settle-recheck-three-way-match}（默认 false）启用后，{@code requireInvoiceForSettle}
 * APPROVED 守卫后追加强制 strict 三单匹配复核——任何数量/价格超容差抛
 * {@link ErpPurErrors#ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED}（cause 链保留原始匹配异常）。
 *
 * <p>回写（派生状态，{@code state-machine.md §付款状态机}）：
 * <ul>
 *   <li>发票 {@code paidAmount} = 该发票全部 PaymentLine 金额之和（跨多付款单，含反向负金额行）；{@code paidStatus}
 *       按累计 vs 含税总额判定 UNPAID/PARTIAL/PAID。</li>
 *   <li>付款 {@code writtenOffStatus} 按已核销 vs 付款总额判定。</li>
 * </ul>
 * 反向核销生成负金额 PaymentLine（保留审计轨迹），余额与状态据此自然回退。
 *
 * <p>本类为非 BizModel 服务（intra-module 聚合写），对齐 {@code ErpPurReceiveBizModel} 用 {@code daoFor}
 * 处理同模块实体（订单行/入库行）的模式；不跨 REQUIRES_NEW 边界（核销为纯域内操作，不过账）。
 */
public class PaymentSettler {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    ThreeWayMatcher threeWayMatcher;

    /**
     * 按分配明细核销付款到发票。返回更新后的付款单（余额/状态已回写）。
     */
    public ErpPurPayment settle(ErpPurPayment payment, List<SettlementAllocation> allocations) {
        if (payment.getApproveStatus() == null
                || !Objects.equals(payment.getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpPurErrors.ERR_SETTLE_PAYMENT_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, payment.getApproveStatus());
        }
        if (allocations == null || allocations.isEmpty()) {
            return payment;
        }

        BigDecimal paymentSettled = sumPaymentLines(payment.getId());
        BigDecimal paymentTotal = nz(payment.getTotalAmount());
        BigDecimal paymentRemaining = paymentTotal.subtract(paymentSettled);

        Map<Long, BigDecimal> touchedInvoices = new HashMap<>();
        IEntityDao<ErpPurPaymentLine> lineDao = daoProvider.daoFor(ErpPurPaymentLine.class);
        for (SettlementAllocation alloc : allocations) {
            if (alloc.getInvoiceId() == null || alloc.getAmount() == null) {
                continue;
            }
            BigDecimal amount = alloc.getAmount();
            if (amount.signum() <= 0) {
                continue;
            }
            ErpPurInvoice invoice = requireInvoiceForSettle(payment, alloc.getInvoiceId());

            BigDecimal invoiceBalance = nz(invoice.getTotalAmountWithTax()).subtract(nz(invoice.getPaidAmount()));
            if (amount.compareTo(invoiceBalance) > 0) {
                throw new NopException(ErpPurErrors.ERR_SETTLE_OVER_INVOICE_BALANCE)
                        .param(ErpPurErrors.ARG_SETTLE_AMOUNT, amount)
                        .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                        .param(ErpPurErrors.ARG_INVOICE_BALANCE, invoiceBalance);
            }
            if (amount.compareTo(paymentRemaining) > 0) {
                throw new NopException(ErpPurErrors.ERR_SETTLE_OVER_PAYMENT_BALANCE)
                        .param(ErpPurErrors.ARG_SETTLE_AMOUNT, amount)
                        .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                        .param(ErpPurErrors.ARG_PAYMENT_BALANCE, paymentRemaining);
            }

            ErpPurPaymentLine line = lineDao.newEntity();
            line.setPaymentId(payment.getId());
            line.setInvoiceId(alloc.getInvoiceId());
            line.setAmount(amount);
            lineDao.saveEntity(line);

            paymentRemaining = paymentRemaining.subtract(amount);
            touchedInvoices.merge(alloc.getInvoiceId(), amount, BigDecimal::add);
        }

        for (Long invoiceId : touchedInvoices.keySet()) {
            recomputeInvoicePaid(invoiceId);
        }
        recomputePaymentWrittenOff(payment.getId());
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    /**
     * 核销冲销：对指定发票生成反向（负金额）PaymentLine，恢复余额与状态。幂等：无既有核销则空操作。
     */
    public ErpPurPayment reverseSettlement(ErpPurPayment payment, Long invoiceId) {
        List<ErpPurPaymentLine> existing = findLines(payment.getId(), invoiceId);
        BigDecimal settled = BigDecimal.ZERO;
        for (ErpPurPaymentLine l : existing) {
            settled = settled.add(nz(l.getAmount()));
        }
        if (settled.signum() == 0) {
            return payment;
        }

        IEntityDao<ErpPurPaymentLine> lineDao = daoProvider.daoFor(ErpPurPaymentLine.class);
        ErpPurPaymentLine reversal = lineDao.newEntity();
        reversal.setPaymentId(payment.getId());
        reversal.setInvoiceId(invoiceId);
        reversal.setAmount(settled.negate());
        reversal.setRemark("核销冲销");
        lineDao.saveEntity(reversal);

        recomputeInvoicePaid(invoiceId);
        recomputePaymentWrittenOff(payment.getId());
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    // ---------- helpers ----------

    private ErpPurInvoice requireInvoiceForSettle(ErpPurPayment payment, Long invoiceId) {
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        if (invoice == null) {
            throw new NopException(ErpPurErrors.ERR_SETTLE_INVOICE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_INVOICE_ID, invoiceId);
        }
        if (invoice.getSupplierId() == null || !invoice.getSupplierId().equals(payment.getSupplierId())) {
            throw new NopException(ErpPurErrors.ERR_SETTLE_SUPPLIER_MISMATCH)
                    .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
        if (invoice.getApproveStatus() == null
                || !Objects.equals(invoice.getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpPurErrors.ERR_SETTLE_INVOICE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, invoice.getApproveStatus());
        }
        // R1.8 P1-MA2-003 方案 A：付款核销三单匹配二次门控（three-way-match.md §匹配时机「付款前最终校验」）。
        // config-gated 默认 false；启用后强制 strict 复核 invoice 三单匹配完成态。
        // match 为只读校验（无状态变更），重算依赖 invoice/receive/order 行当前状态（APPROVED 发票回链不允许修改，见 three-way-match.md §一致性规则）。
        if (isSettleRecheckEnabled()) {
            recheckThreeWayMatchAtSettle(invoice);
        }
        return invoice;
    }

    private boolean isSettleRecheckEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpPurConstants.CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH, Boolean.FALSE));
    }

    private void recheckThreeWayMatchAtSettle(ErpPurInvoice invoice) {
        List<ErpPurInvoiceLine> lines = loadInvoiceLines(invoice.getId());
        try {
            // 强制 strict 复核：任何数量/价格超容差即抛 ERR_INVOICE_QTY_MISMATCH / ERR_INVOICE_PRICE_MISMATCH。
            threeWayMatcher.match(invoice.getCode(), lines, Boolean.TRUE);
        } catch (NopException e) {
            // 包装为 settle 语境错误码，保留 cause 链（原始匹配异常携带 invoiceCode/lineNo/qty/price 参数）。
            throw new NopException(ErpPurErrors.ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED, e)
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
    }

    private List<ErpPurInvoiceLine> loadInvoiceLines(Long invoiceId) {
        ormTemplate.flushSession();
        IEntityDao<ErpPurInvoiceLine> dao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("invoiceId", invoiceId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private void recomputeInvoicePaid(Long invoiceId) {
        ormTemplate.flushSession();
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        BigDecimal paid = sumInvoiceLines(invoiceId);
        invoice.setPaidAmount(paid);
        BigDecimal withTax = nz(invoice.getTotalAmountWithTax());
        String status;
        if (paid.signum() <= 0) {
            status = ErpPurConstants.PAID_STATUS_UNPAID;
        } else if (paid.compareTo(withTax) >= 0) {
            status = ErpPurConstants.PAID_STATUS_PAID;
        } else {
            status = ErpPurConstants.PAID_STATUS_PARTIAL;
        }
        invoice.setPaidStatus(status);
        daoProvider.daoFor(ErpPurInvoice.class).updateEntity(invoice);
    }

    private void recomputePaymentWrittenOff(Long paymentId) {
        ormTemplate.flushSession();
        ErpPurPayment payment = daoProvider.daoFor(ErpPurPayment.class).getEntityById(paymentId);
        BigDecimal settled = sumPaymentLines(paymentId);
        BigDecimal total = nz(payment.getTotalAmount());
        String status;
        if (settled.signum() <= 0) {
            status = ErpPurConstants.PAID_STATUS_UNPAID;
        } else if (settled.compareTo(total) >= 0) {
            status = ErpPurConstants.PAID_STATUS_PAID;
        } else {
            status = ErpPurConstants.PAID_STATUS_PARTIAL;
        }
        payment.setWrittenOffStatus(status);
        daoProvider.daoFor(ErpPurPayment.class).updateEntity(payment);
    }

    private BigDecimal sumInvoiceLines(Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("invoiceId", invoiceId));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPurPaymentLine l : daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q)) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private BigDecimal sumPaymentLines(Long paymentId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("paymentId", paymentId));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPurPaymentLine l : daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q)) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private List<ErpPurPaymentLine> findLines(Long paymentId, Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("paymentId", paymentId));
        q.addFilter(eq("invoiceId", invoiceId));
        return new ArrayList<>(daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q));
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
