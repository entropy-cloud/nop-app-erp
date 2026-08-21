package app.erp.fin.service.reconciliation;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核销结算器。负责核销单过账/红冲时对双方辅助账（{@link ErpFinArApItem}）的 settled/open/status 回写，
 * 以及核销单头金额合计的计算。纯算术与状态机，不含约束校验（校验在 BizModel 编排层）。
 *
 * <p>状态机（{@code ar-ap-status}）：open→(settled&lt;open)→PARTIAL；settled==open→SETTLED。
 * 红冲按原核销行的相反数恢复双方金额与状态。
 */
public class ReconciliationSettler {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 过账结算：按核销行回写双方辅助账，计算核销单头合计。返回核销行实际生效的本位币结算合计。
     */
    public BigDecimal settle(ErpFinReconciliation head, List<ErpFinReconciliationLine> lines) {
        Map<String, ErpFinArApItem> cache = loadItems(lines);
        BigDecimal totalFunctional = BigDecimal.ZERO;
        BigDecimal totalSource = BigDecimal.ZERO;
        for (ErpFinReconciliationLine line : lines) {
            BigDecimal amtFunctional = nz(line.getSettledAmountFunctional());
            BigDecimal amtSource = nz(line.getSettledAmountSource());
            applySettlement(cache.get(line.getPaymentItemId()), amtFunctional, amtSource, false);
            applySettlement(cache.get(line.getInvoiceItemId()), amtFunctional, amtSource, false);
            totalFunctional = totalFunctional.add(amtFunctional);
            totalSource = totalSource.add(amtSource);
        }
        head.setTotalAmountFunctional(totalFunctional);
        head.setTotalAmountSource(totalSource);
        return totalFunctional;
    }

    /**
     * 多币种过账结算（R1.9 / P1-MA2-009）：按 per-item functional（settledSource × item.exchangeRate）分别回写双方辅助账，
     * 计算已实现汇兑差额 = Σ(payment.functionalSettled) − Σ(invoice.functionalSettled)。
     *
     * <p>head.totalAmountFunctional 取发票侧合计（AR/AP 清账口径）；head.fxGainLoss 记录汇兑差额（正=收益，负=损失）。
     * 单币种（双方 rate 相同）时差额=0，退化为 {@link #settle} 行为。
     *
     * @return 已实现汇兑差额（payment − invoice；正=收益，负=损失）
     */
    public BigDecimal settleWithFx(ErpFinReconciliation head, List<ErpFinReconciliationLine> lines) {
        Map<String, ErpFinArApItem> cache = loadItems(lines);
        BigDecimal totalInvoiceFunctional = BigDecimal.ZERO;
        BigDecimal totalPaymentFunctional = BigDecimal.ZERO;
        BigDecimal totalSource = BigDecimal.ZERO;
        for (ErpFinReconciliationLine line : lines) {
            BigDecimal settledSource = nz(line.getSettledAmountSource());
            ErpFinArApItem paymentItem = cache.get(line.getPaymentItemId());
            ErpFinArApItem invoiceItem = cache.get(line.getInvoiceItemId());
            BigDecimal paymentFunctional = computeFunctionalSettled(paymentItem, settledSource);
            BigDecimal invoiceFunctional = computeFunctionalSettled(invoiceItem, settledSource);
            applySettlement(paymentItem, paymentFunctional, settledSource, false);
            applySettlement(invoiceItem, invoiceFunctional, settledSource, false);
            totalPaymentFunctional = totalPaymentFunctional.add(paymentFunctional);
            totalInvoiceFunctional = totalInvoiceFunctional.add(invoiceFunctional);
            totalSource = totalSource.add(settledSource);
        }
        BigDecimal fxGainLoss = totalPaymentFunctional.subtract(totalInvoiceFunctional);
        head.setTotalAmountFunctional(totalInvoiceFunctional);
        head.setTotalAmountSource(totalSource);
        head.setFxGainLoss(fxGainLoss);
        return fxGainLoss;
    }

    /**
     * 按 item 自身汇率折算本位币结算额：settledSource × item.exchangeRate。
     * item 无汇率时回退 line 提供的 settledAmountFunctional（向后兼容无汇率辅助账）。
     */
    protected BigDecimal computeFunctionalSettled(ErpFinArApItem item, BigDecimal settledSource) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = item.getExchangeRate() != null ? item.getExchangeRate() : BigDecimal.ONE;
        return settledSource.multiply(rate);
    }

    /**
     * 红冲结算：按原核销行的相反数恢复双方辅助账（settled-=amt / open+=amt / 状态降级回 OPEN 或 PARTIAL）。
     */
    public void reverseSettle(List<ErpFinReconciliationLine> lines) {
        Map<String, ErpFinArApItem> cache = loadItems(lines);
        for (ErpFinReconciliationLine line : lines) {
            BigDecimal amtFunctional = nz(line.getSettledAmountFunctional());
            BigDecimal amtSource = nz(line.getSettledAmountSource());
            applySettlement(cache.get(line.getPaymentItemId()), amtFunctional, amtSource, true);
            applySettlement(cache.get(line.getInvoiceItemId()), amtFunctional, amtSource, true);
        }
    }

    protected void applySettlement(ErpFinArApItem item, BigDecimal amtFunctional, BigDecimal amtSource,
                                   boolean reverse) {
        if (item == null) {
            return;
        }
        int sign = reverse ? -1 : 1;
        BigDecimal deltaFunctional = amtFunctional.multiply(BigDecimal.valueOf(sign));
        BigDecimal deltaSource = amtSource.multiply(BigDecimal.valueOf(sign));

        BigDecimal settledF = nz(item.getSettledAmountFunctional()).add(deltaFunctional);
        BigDecimal settledS = nz(item.getSettledAmountSource()).add(deltaSource);
        BigDecimal openF = nz(item.getAmountFunctional()).subtract(settledF);
        BigDecimal openS = nz(item.getAmountSource()).subtract(settledS);

        item.setSettledAmountFunctional(settledF);
        item.setSettledAmountSource(settledS);
        item.setOpenAmountFunctional(openF);
        item.setOpenAmountSource(openS);
        item.setStatus(resolveStatus(settledF, item.getAmountFunctional()));
    }

    protected String resolveStatus(BigDecimal settledFunctional, BigDecimal amountFunctional) {
        BigDecimal settled = nz(settledFunctional);
        BigDecimal total = nz(amountFunctional);
        if (settled.compareTo(BigDecimal.ZERO) <= 0) {
            return ErpFinConstants.AR_AP_STATUS_OPEN;
        }
        if (settled.compareTo(total) >= 0) {
            return ErpFinConstants.AR_AP_STATUS_SETTLED;
        }
        return ErpFinConstants.AR_AP_STATUS_PARTIAL;
    }

    protected Map<String, ErpFinArApItem> loadItems(List<ErpFinReconciliationLine> lines) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        Map<String, ErpFinArApItem> cache = new HashMap<>();
        for (ErpFinReconciliationLine line : lines) {
            cache.computeIfAbsent(line.getPaymentItemId(), dao::getEntityById);
            cache.computeIfAbsent(line.getInvoiceItemId(), dao::getEntityById);
        }
        return cache;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** 收集核销行涉及的全部辅助账 ID（供校验/重算）。 */
    public List<String> collectItemIds(List<ErpFinReconciliationLine> lines) {
        List<String> ids = new ArrayList<>();
        for (ErpFinReconciliationLine line : lines) {
            if (line.getPaymentItemId() != null) {
                ids.add(line.getPaymentItemId());
            }
            if (line.getInvoiceItemId() != null) {
                ids.add(line.getInvoiceItemId());
            }
        }
        return ids;
    }
}
