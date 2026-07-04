package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.dto.AutoReconUnmatched;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 自动核销规则引擎（plan 2026-07-05-0115-1 Phase 1）。
 *
 * <p>按 partner + direction 查询 OPEN/PARTIAL 的发票项（AP_INVOICE/AR_INVOICE）与收付款项（PAYMENT/RECEIPT），
 * 按可配置分摊策略（FIFO 按到期日/业务日期、BY_AMOUNT 精确匹配、BY_RATIO 按余额比例）生成核销候选行。
 *
 * <p>引擎只负责生成候选行（{@link ReconciliationLineInput}），核销约束校验/状态回写/partner 余额
 * 全部走 0300-3 既有 {@code ErpFinReconciliationBizModel.create+post} 路径，不重写核销原语。
 *
 * <p>金额精度：{@code erp-fin.reconcile-precision}（默认 0.01）；尾差调整末行。
 * 超额控制：{@code erp-fin.allow-over-reconcile=false} 时金额不超任一方 openAmount。
 */
public class AutoReconciliationEngine {

    public static final String UNMATCHED_NO_COUNTERPART = "NO_COUNTERPART";
    public static final String UNMATCHED_NO_CANDIDATE = "NO_CANDIDATE";
    public static final String UNMATCHED_OVER_AMOUNT = "OVER_AMOUNT";

    @Inject
    IErpFinArApItemBiz arApItemBiz;

    /**
     * 按 partner + direction + strategy 生成核销候选行 + 未匹配项报告。
     *
     * @param direction  RECEIVABLE/PAYABLE
     * @param partnerId  往来单位 ID（非 null）
     * @param strategy   FIFO/BY_AMOUNT/BY_RATIO
     * @param context    服务上下文（引擎内部查询用）
     * @return 候选行列表（可能为空，表示无匹配）+ 未匹配项报告
     */
    public MatchResult matchAndBuild(String direction, Long partnerId, String strategy, IServiceContext context) {
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        List<ErpFinArApItem> opens = arApItemBiz.findOpenItemsByPartner(partnerId, direction, ctx);
        List<ErpFinArApItem> invoices = filterInvoices(opens);
        List<ErpFinArApItem> payments = filterPayments(opens);

        MatchResult result = new MatchResult();
        if (invoices.isEmpty()) {
            for (ErpFinArApItem p : payments) {
                result.unmatched.add(unmatched(partnerId, direction, p, UNMATCHED_NO_COUNTERPART));
            }
            return result;
        }
        if (payments.isEmpty()) {
            for (ErpFinArApItem i : invoices) {
                result.unmatched.add(unmatched(partnerId, direction, i, UNMATCHED_NO_COUNTERPART));
            }
            return result;
        }

        BigDecimal precision = precision();
        boolean allowOver = isAllowOverReconcile();

        switch (strategy) {
            case ErpFinConstants.AUTO_RECON_STRATEGY_BY_AMOUNT:
                matchByAmount(invoices, payments, precision, allowOver, partnerId, direction, result);
                break;
            case ErpFinConstants.AUTO_RECON_STRATEGY_BY_RATIO:
                matchByRatio(invoices, payments, precision, allowOver, partnerId, direction, result);
                break;
            case ErpFinConstants.AUTO_RECON_STRATEGY_FIFO:
            default:
                matchFifo(invoices, payments, precision, allowOver, partnerId, direction, result);
                break;
        }
        return result;
    }

    // ---------- FIFO：按到期日升序（null 回退 businessDate），逐笔核销直至收付款项 open 耗尽 ----------

    protected void matchFifo(List<ErpFinArApItem> invoices, List<ErpFinArApItem> payments,
                             BigDecimal precision, boolean allowOver,
                             Long partnerId, String direction, MatchResult result) {
        List<ErpFinArApItem> sortedInvoices = sortByDueOrBusinessDate(invoices);
        Map<Long, BigDecimal> paymentOpen = indexOpen(payments);

        for (ErpFinArApItem invoice : sortedInvoices) {
            BigDecimal invoiceOpen = openFunctional(invoice);
            if (invoiceOpen.compareTo(precision) <= 0) {
                continue;
            }
            for (ErpFinArApItem payment : payments) {
                BigDecimal remain = paymentOpen.get(payment.getId());
                if (remain == null || remain.compareTo(precision) <= 0) {
                    continue;
                }
                BigDecimal settle = invoiceOpen.min(remain);
                if (!allowOver && settle.compareTo(invoiceOpen) > 0) {
                    settle = invoiceOpen;
                }
                if (settle.compareTo(precision) <= 0) {
                    continue;
                }
                result.lines.add(line(payment, invoice, settle));
                invoiceOpen = invoiceOpen.subtract(settle);
                paymentOpen.put(payment.getId(), remain.subtract(settle));
                if (invoiceOpen.compareTo(precision) <= 0) {
                    break;
                }
            }
            if (invoiceOpen.compareTo(precision) > 0) {
                result.unmatched.add(unmatched(partnerId, direction, invoice, UNMATCHED_NO_CANDIDATE));
            }
        }
        for (ErpFinArApItem payment : payments) {
            BigDecimal remain = paymentOpen.get(payment.getId());
            if (remain != null && remain.compareTo(precision) > 0) {
                result.unmatched.add(unmatched(partnerId, direction, payment, UNMATCHED_NO_CANDIDATE));
            }
        }
    }

    // ---------- BY_AMOUNT：收付款项金额精确匹配发票项金额，仅 1:1 命中 ----------

    protected void matchByAmount(List<ErpFinArApItem> invoices, List<ErpFinArApItem> payments,
                                 BigDecimal precision, boolean allowOver,
                                 Long partnerId, String direction, MatchResult result) {
        Map<BigDecimal, List<ErpFinArApItem>> invoiceByAmount = new HashMap<>();
        for (ErpFinArApItem inv : invoices) {
            BigDecimal key = norm(openFunctional(inv), precision);
            invoiceByAmount.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
        }
        for (ErpFinArApItem payment : payments) {
            BigDecimal pmtOpen = norm(openFunctional(payment), precision);
            List<ErpFinArApItem> candidates = invoiceByAmount.get(pmtOpen);
            if (candidates == null || candidates.isEmpty()) {
                result.unmatched.add(unmatched(partnerId, direction, payment, UNMATCHED_NO_CANDIDATE));
                continue;
            }
            ErpFinArApItem invoice = candidates.remove(0);
            result.lines.add(line(payment, invoice, openFunctional(invoice)));
        }
        for (List<ErpFinArApItem> remaining : invoiceByAmount.values()) {
            for (ErpFinArApItem inv : remaining) {
                result.unmatched.add(unmatched(partnerId, direction, inv, UNMATCHED_NO_CANDIDATE));
            }
        }
    }

    // ---------- BY_RATIO：按发票开口余额比例分摊每笔收付款项，尾差归末行 ----------

    protected void matchByRatio(List<ErpFinArApItem> invoices, List<ErpFinArApItem> payments,
                                BigDecimal precision, boolean allowOver,
                                Long partnerId, String direction, MatchResult result) {
        List<ErpFinArApItem> sortedInvoices = sortByDueOrBusinessDate(invoices);
        Map<Long, BigDecimal> invoiceOpen = indexOpen(sortedInvoices);
        BigDecimal totalInvoiceOpen = sumOpen(sortedInvoices);

        for (ErpFinArApItem payment : payments) {
            BigDecimal pmtOpen = openFunctional(payment);
            if (pmtOpen.compareTo(precision) <= 0 || totalInvoiceOpen.compareTo(precision) <= 0) {
                result.unmatched.add(unmatched(partnerId, direction, payment, UNMATCHED_NO_CANDIDATE));
                continue;
            }
            BigDecimal allocated = BigDecimal.ZERO;
            int lastValidIdx = -1;
            for (int i = 0; i < sortedInvoices.size(); i++) {
                ErpFinArApItem invoice = sortedInvoices.get(i);
                BigDecimal invOpen = invoiceOpen.get(invoice.getId());
                if (invOpen == null || invOpen.compareTo(precision) <= 0) {
                    continue;
                }
                lastValidIdx = i;
                BigDecimal share = pmtOpen.multiply(invOpen)
                        .divide(totalInvoiceOpen, 2, RoundingMode.HALF_UP);
                BigDecimal remainInv = invOpen.subtract(share);
                if (!allowOver && remainInv.compareTo(precision.negate()) < 0) {
                    share = invOpen;
                }
                if (share.compareTo(precision) <= 0) {
                    continue;
                }
                result.lines.add(line(payment, invoice, share));
                invoiceOpen.put(invoice.getId(), invOpen.subtract(share));
                allocated = allocated.add(share);
            }
            // 尾差归末行（确保 Σallocated == pmtOpen，消除比例除法误差）
            BigDecimal tail = pmtOpen.subtract(allocated);
            if (tail.abs().compareTo(precision) > 0 && lastValidIdx >= 0) {
                ErpFinArApItem lastInvoice = sortedInvoices.get(lastValidIdx);
                BigDecimal invOpen = invoiceOpen.get(lastInvoice.getId());
                if (invOpen != null && (allowOver || invOpen.add(tail).compareTo(precision.negate()) >= 0)) {
                    result.lines.add(line(payment, lastInvoice, tail));
                    invoiceOpen.put(lastInvoice.getId(), invOpen.subtract(tail));
                }
            }
        }
        for (ErpFinArApItem invoice : sortedInvoices) {
            BigDecimal remain = invoiceOpen.get(invoice.getId());
            if (remain != null && remain.compareTo(precision) > 0) {
                result.unmatched.add(unmatched(partnerId, direction, invoice, UNMATCHED_NO_CANDIDATE));
            }
        }
    }

    // ---------- helpers ----------

    protected List<ErpFinArApItem> filterInvoices(List<ErpFinArApItem> opens) {
        List<ErpFinArApItem> r = new ArrayList<>();
        for (ErpFinArApItem it : opens) {
            if (isInvoice(it)) {
                r.add(it);
            }
        }
        return r;
    }

    protected List<ErpFinArApItem> filterPayments(List<ErpFinArApItem> opens) {
        List<ErpFinArApItem> r = new ArrayList<>();
        for (ErpFinArApItem it : opens) {
            if (isPayment(it)) {
                r.add(it);
            }
        }
        return r;
    }

    protected boolean isInvoice(ErpFinArApItem it) {
        String t = it.getSourceBillType();
        return Objects.equals(t, ErpFinConstants.SOURCE_BILL_AP_INVOICE)
                || Objects.equals(t, ErpFinConstants.SOURCE_BILL_AR_INVOICE);
    }

    protected boolean isPayment(ErpFinArApItem it) {
        String t = it.getSourceBillType();
        return Objects.equals(t, ErpFinConstants.SOURCE_BILL_PAYMENT)
                || Objects.equals(t, ErpFinConstants.SOURCE_BILL_RECEIPT);
    }

    protected List<ErpFinArApItem> sortByDueOrBusinessDate(List<ErpFinArApItem> items) {
        List<ErpFinArApItem> r = new ArrayList<>(items);
        r.sort(Comparator.comparing(it -> it.getDueDate() != null ? it.getDueDate()
                : (it.getBusinessDate() != null ? it.getBusinessDate() : LocalDate.MAX)));
        return r;
    }

    protected Map<Long, BigDecimal> indexOpen(List<ErpFinArApItem> items) {
        Map<Long, BigDecimal> m = new HashMap<>();
        for (ErpFinArApItem it : items) {
            m.put(it.getId(), openFunctional(it));
        }
        return m;
    }

    protected BigDecimal sumOpen(List<ErpFinArApItem> items) {
        BigDecimal s = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            s = s.add(openFunctional(it));
        }
        return s;
    }

    protected BigDecimal openFunctional(ErpFinArApItem it) {
        BigDecimal v = it.getOpenAmountFunctional();
        return v != null ? v : BigDecimal.ZERO;
    }

    protected BigDecimal norm(BigDecimal v, BigDecimal precision) {
        return v.setScale(precision.scale(), RoundingMode.HALF_UP);
    }

    protected ReconciliationLineInput line(ErpFinArApItem payment, ErpFinArApItem invoice, BigDecimal amountFunctional) {
        ReconciliationLineInput in = new ReconciliationLineInput();
        in.setPaymentItemId(payment.getId());
        in.setInvoiceItemId(invoice.getId());
        BigDecimal rate = invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE;
        in.setSettledAmountFunctional(amountFunctional);
        in.setSettledAmountSource(rate.compareTo(BigDecimal.ZERO) == 0
                ? amountFunctional : amountFunctional.divide(rate, 2, RoundingMode.HALF_UP));
        return in;
    }

    protected AutoReconUnmatched unmatched(Long partnerId, String direction, ErpFinArApItem item, String reason) {
        AutoReconUnmatched u = new AutoReconUnmatched();
        u.setPartnerId(partnerId);
        u.setDirection(direction);
        u.setArApItemId(item.getId());
        u.setOpenAmount(openFunctional(item));
        u.setUnmatchedReason(reason);
        return u;
    }

    protected BigDecimal precision() {
        BigDecimal p = AppConfig.var(ErpFinConstants.CONFIG_RECONCILE_PRECISION, new BigDecimal("0.01"));
        return p != null ? p : new BigDecimal("0.01");
    }

    protected boolean isAllowOverReconcile() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_ALLOW_OVER_RECONCILE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 查询指定 direction 下所有有开口余额的 partner ID（partnerId=null 全量遍历用）。
     */
    public List<Long> findPartnersWithOpenItems(String direction, IServiceContext context) {
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        QueryBean query = new QueryBean();
        query.addFilter(eq("direction", direction));
        query.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        List<ErpFinArApItem> items = arApItemBiz.findList(query, null, ctx);
        List<Long> partners = new ArrayList<>();
        for (ErpFinArApItem it : items) {
            if (it.getPartnerId() != null && !partners.contains(it.getPartnerId())) {
                partners.add(it.getPartnerId());
            }
        }
        return partners;
    }

    /** 候选行 + 未匹配项报告。 */
    public static class MatchResult {
        private final List<ReconciliationLineInput> lines = new ArrayList<>();
        private final List<AutoReconUnmatched> unmatched = new ArrayList<>();

        public List<ReconciliationLineInput> getLines() {
            return lines;
        }

        public List<AutoReconUnmatched> getUnmatched() {
            return unmatched;
        }
    }
}
