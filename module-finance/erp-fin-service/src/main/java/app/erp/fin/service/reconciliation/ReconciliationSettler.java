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
        Map<Long, ErpFinArApItem> cache = loadItems(lines);
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
     * 红冲结算：按原核销行的相反数恢复双方辅助账（settled-=amt / open+=amt / 状态降级回 OPEN 或 PARTIAL）。
     */
    public void reverseSettle(List<ErpFinReconciliationLine> lines) {
        Map<Long, ErpFinArApItem> cache = loadItems(lines);
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

    protected int resolveStatus(BigDecimal settledFunctional, BigDecimal amountFunctional) {
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

    protected Map<Long, ErpFinArApItem> loadItems(List<ErpFinReconciliationLine> lines) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        Map<Long, ErpFinArApItem> cache = new HashMap<>();
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
    public List<Long> collectItemIds(List<ErpFinReconciliationLine> lines) {
        List<Long> ids = new ArrayList<>();
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
