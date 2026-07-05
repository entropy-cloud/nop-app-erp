package app.erp.fin.service.baddebt;

import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.config.AppConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 坏账准备计提引擎（{@code bad-debt.md §坏账准备计提方法}，账龄分桶法）。
 *
 * <p>按 5 级配置历史损失率 × 各账龄区间应收 openAmount 求必需准备，排除：
 * <ul>
 *   <li>已核销项（status=SETTLED/WRITTEN_OFF）与作废项（status=CANCELLED）——自然不在账龄基础</li>
 *   <li>负余额/零余额项（预收/贷余排除）</li>
 *   <li>争议项（config-gated {@code exclude-disputed}，当前无 disputed 字段，预留门控）</li>
 * </ul>
 *
 * <p>账龄基准复用 {@code ErpFinArApItemBizModel.aging}：应收按 {@code erp-fin.ar-aging-base}
 * （invoice_date/due_date，默认 due_date）配置，dueDate 为空回退 businessDate。
 *
 * <p>本类为无状态 Bean，供 {@code BadDebtProvisionService} 与期末门控复用。
 */
public class BadDebtProvisionCalculator {

    /**
     * 按账龄分桶法计算必需准备。
     *
     * @param receivableOpenItems 应收方向（RECEIVABLE）未核销辅助账项（status=OPEN/PARTIAL）
     * @param asOf               账龄计算截止日（通常为期末 endDate）
     * @return 必需准备 + 各区间明细（损失率已应用）
     */
    public BadDebtProvisionResult calculate(List<ErpFinArApItem> receivableOpenItems, LocalDate asOf) {
        BadDebtProvisionResult result = new BadDebtProvisionResult();
        if (receivableOpenItems == null || receivableOpenItems.isEmpty() || asOf == null) {
            return result;
        }
        boolean byDueDate = isAgingByDueDate();
        BigDecimal rate030 = lossRate(ErpFinConstants.CONFIG_BAD_DEBT_LOSS_RATE_0_30, ErpFinConstants.DEFAULT_LOSS_RATE_0_30);
        BigDecimal rate3160 = lossRate(ErpFinConstants.CONFIG_BAD_DEBT_LOSS_RATE_31_60, ErpFinConstants.DEFAULT_LOSS_RATE_31_60);
        BigDecimal rate6190 = lossRate(ErpFinConstants.CONFIG_BAD_DEBT_LOSS_RATE_61_90, ErpFinConstants.DEFAULT_LOSS_RATE_61_90);
        BigDecimal rate91180 = lossRate(ErpFinConstants.CONFIG_BAD_DEBT_LOSS_RATE_91_180, ErpFinConstants.DEFAULT_LOSS_RATE_91_180);
        BigDecimal rate180Plus = lossRate(ErpFinConstants.CONFIG_BAD_DEBT_LOSS_RATE_180_PLUS, ErpFinConstants.DEFAULT_LOSS_RATE_180_PLUS);

        for (ErpFinArApItem item : receivableOpenItems) {
            BigDecimal open = item.getOpenAmountFunctional();
            if (open == null || open.signum() <= 0) {
                continue;
            }
            String status = item.getStatus();
            if (Objects.equals(status, ErpFinConstants.AR_AP_STATUS_SETTLED)
                    || Objects.equals(status, ErpFinConstants.AR_AP_STATUS_CANCELLED)
                    || Objects.equals(status, ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF)) {
                continue;
            }
            LocalDate baseDate = byDueDate && item.getDueDate() != null ? item.getDueDate() : item.getBusinessDate();
            long ageDays = daysBetween(baseDate, asOf);
            result.setTotalConsidered(result.getTotalConsidered().add(open));
            if (ageDays <= 30) {
                result.setBucket030(result.getBucket030().add(open));
                result.setRequiredProvision(result.getRequiredProvision().add(open.multiply(rate030)));
            } else if (ageDays <= 60) {
                result.setBucket3160(result.getBucket3160().add(open));
                result.setRequiredProvision(result.getRequiredProvision().add(open.multiply(rate3160)));
            } else if (ageDays <= 90) {
                result.setBucket6190(result.getBucket6190().add(open));
                result.setRequiredProvision(result.getRequiredProvision().add(open.multiply(rate6190)));
            } else if (ageDays <= 180) {
                result.setBucket91180(result.getBucket91180().add(open));
                result.setRequiredProvision(result.getRequiredProvision().add(open.multiply(rate91180)));
            } else {
                result.setBucket180Plus(result.getBucket180Plus().add(open));
                result.setRequiredProvision(result.getRequiredProvision().add(open.multiply(rate180Plus)));
            }
        }
        return result;
    }

    protected boolean isAgingByDueDate() {
        String base = AppConfig.var(ErpFinConstants.CONFIG_AR_AGING_BASE, ErpFinConstants.AGING_BASE_DUE_DATE);
        return !ErpFinConstants.AGING_BASE_INVOICE_DATE.equalsIgnoreCase(base);
    }

    protected BigDecimal lossRate(String configKey, BigDecimal defaultRate) {
        Object raw = AppConfig.var(configKey, null);
        if (raw == null) {
            return defaultRate;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return defaultRate;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return defaultRate;
        }
    }

    protected long daysBetween(LocalDate from, LocalDate to) {
        if (from == null) {
            return 0;
        }
        return to.toEpochDay() - from.toEpochDay();
    }
}
