package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.DualSideDiffReport.DualSideDiffRow;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.sal.dao.entity.ErpSalInvoice;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 双面对账一致性兜底检查器（plan 2026-07-05-0115-1 Phase 3）。
 *
 * <p>比对 finance 侧 {@link ErpFinArApItem} 已核销额聚合（发票项 settledAmountFunctional）
 * vs 域级侧已核销额聚合（purchase {@link ErpPurInvoice#getPaidAmount} /
 * sales {@link ErpSalInvoice#getReceivedAmount}，经 ArApItem.sourceBillCode 关联发票）。
 *
 * <p>差额超 {@code erp-fin.reconcile-precision} 的 partner 标记 INCONSISTENT 并结构化日志告警。
 * 不自动修复（避免静默修改域级核销权威）。
 *
 * <p>跨实体访问：finance→purchase/sales 为只读 R（按 data-dependency-matrix.md 合法），经 daoFor 机制 B
 * 直读域级发票实体（不需 IBiz 管道，因兜底检查是 finance 内部对账，无业务权限语义）。
 */
public class DualSideConsistencyChecker {
    static final Logger LOG = LoggerFactory.getLogger(DualSideConsistencyChecker.class);

    public static final String STATUS_CONSISTENT = "CONSISTENT";
    public static final String STATUS_INCONSISTENT = "INCONSISTENT";

    @Inject
    IErpFinArApItemBiz arApItemBiz;
    @Inject
    IDaoProvider daoProvider;

    public DualSideDiffReport check(String direction, Long partnerId, IServiceContext context) {
        DualSideDiffReport report = new DualSideDiffReport();
        report.setDirection(direction);
        report.setPartnerId(partnerId);

        List<ErpFinArApItem> invoiceItems = findInvoiceItems(direction, partnerId, context);
        if (invoiceItems.isEmpty()) {
            report.setConsistent(true);
            return report;
        }

        BigDecimal precision = precision();
        Map<Long, BigDecimal> financeSettledByPartner = new HashMap<>();
        Map<Long, BigDecimal> domainSettledByPartner = new HashMap<>();
        Map<Long, BigDecimal> invoiceAmountByPartner = new HashMap<>();

        for (ErpFinArApItem item : invoiceItems) {
            Long pid = item.getPartnerId();
            BigDecimal settled = nz(item.getSettledAmountFunctional());
            BigDecimal amount = nz(item.getAmountFunctional());
            financeSettledByPartner.merge(pid, settled, BigDecimal::add);
            invoiceAmountByPartner.merge(pid, amount, BigDecimal::add);

            BigDecimal domainPaid = resolveDomainSettled(direction, item);
            domainSettledByPartner.merge(pid, domainPaid, BigDecimal::add);
        }

        boolean allConsistent = true;
        for (Long pid : financeSettledByPartner.keySet()) {
            BigDecimal finSettled = financeSettledByPartner.getOrDefault(pid, BigDecimal.ZERO);
            BigDecimal domSettled = domainSettledByPartner.getOrDefault(pid, BigDecimal.ZERO);
            BigDecimal diff = finSettled.subtract(domSettled).abs();

            DualSideDiffRow row = new DualSideDiffRow();
            row.setPartnerId(pid);
            row.setFinanceSettled(finSettled);
            row.setDomainSettled(domSettled);
            row.setDiff(diff);
            boolean consistent = diff.compareTo(precision) <= 0;
            row.setStatus(consistent ? STATUS_CONSISTENT : STATUS_INCONSISTENT);
            report.getRows().add(row);

            if (!consistent) {
                allConsistent = false;
                LOG.warn("dual-side-consistency-inconsistent: partnerId={} direction={} financeSettled={} domainSettled={} diff={}",
                        pid, direction, finSettled, domSettled, diff);
            }
        }
        report.setConsistent(allConsistent);
        return report;
    }

    protected List<ErpFinArApItem> findInvoiceItems(String direction, Long partnerId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("direction", direction));
        if (partnerId != null) {
            query.addFilter(eq("partnerId", partnerId));
        }
        query.addFilter(in("sourceBillType", Arrays.asList(
                ErpFinConstants.SOURCE_BILL_AP_INVOICE, ErpFinConstants.SOURCE_BILL_AR_INVOICE)));
        return arApItemBiz.findList(query, null, context);
    }

    /**
     * 解析域级侧已核销额。AP_INVOICE → ErpPurInvoice.paidAmount；AR_INVOICE → ErpSalInvoice.receivedAmount。
     * 经 ArApItem.sourceBillCode = 域级发票 code 关联。找不到域级发票返回 ZERO（视为未核销）。
     */
    protected BigDecimal resolveDomainSettled(String direction, ErpFinArApItem item) {
        String code = item.getSourceBillCode();
        if (code == null || code.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (ErpFinConstants.DIRECTION_PAYABLE.equals(direction)) {
            ErpPurInvoice inv = findPurInvoiceByCode(code);
            return inv != null ? nz(inv.getPaidAmount()) : BigDecimal.ZERO;
        }
        ErpSalInvoice inv = findSalInvoiceByCode(code);
        return inv != null ? nz(inv.getReceivedAmount()) : BigDecimal.ZERO;
    }

    protected ErpPurInvoice findPurInvoiceByCode(String code) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpPurInvoice> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpSalInvoice findSalInvoiceByCode(String code) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpSalInvoice> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected BigDecimal precision() {
        BigDecimal p = AppConfig.var(ErpFinConstants.CONFIG_RECONCILE_PRECISION, new BigDecimal("0.01"));
        return p != null ? p : new BigDecimal("0.01");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
