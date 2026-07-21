
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinIntercompanyMatchBiz;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.DualSideDiffReport.DualSideDiffRow;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 公司间自动配对 BizModel（plan 2026-07-22-1000-1 A3，multi-company.md §公司间自动配对算法）。
 *
 * <p>{@code runMatching(periodId)} 按 pairKey 扫描跨公司 INTERCOMPANY_SALE/PURCHASE 凭证对，写配对记录。
 * {@code checkDualSideConsistency} 返回 DualSideDiffReport（复用 DualSideDiffRow 结构范式）。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §Decision C}。
 */
@BizModel("ErpFinIntercompanyMatch")
public class ErpFinIntercompanyMatchBizModel extends CrudBizModel<ErpFinIntercompanyMatch>
        implements IErpFinIntercompanyMatchBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinIntercompanyMatchBizModel.class);

    public ErpFinIntercompanyMatchBizModel() {
        setEntityName(ErpFinIntercompanyMatch.class.getName());
    }

    @Override
    @BizMutation
    public int runMatching(@Name("periodId") Long periodId, IServiceContext context) {
        if (periodId == null) {
            return 0;
        }
        assertPeriodOpen(periodId);

        // 经 ErpFinVoucherBillR 反查 INTERCOMPANY_SALE/PURCHASE 凭证，按 billCode（调拨单 code）配对
        java.util.Map<String, java.util.List<Long>> saleByBillCode = findIntercompanyVoucherIdsByBillCode(
                ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, periodId);
        java.util.Map<String, java.util.List<Long>> purchaseByBillCode = findIntercompanyVoucherIdsByBillCode(
                ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, periodId);

        int count = 0;
        IEntityDao<ErpFinIntercompanyMatch> matchDao = daoProvider().daoFor(ErpFinIntercompanyMatch.class);
        java.util.Set<String> allBillCodes = new java.util.HashSet<>();
        allBillCodes.addAll(saleByBillCode.keySet());
        allBillCodes.addAll(purchaseByBillCode.keySet());

        for (String billCode : allBillCodes) {
            BigDecimal saleAmt = sumVoucherAmounts(saleByBillCode.get(billCode));
            BigDecimal purchaseAmt = sumVoucherAmounts(purchaseByBillCode.get(billCode));
            BigDecimal matched = saleAmt.min(purchaseAmt);
            BigDecimal diff = saleAmt.subtract(purchaseAmt).abs();
            String status = diff.compareTo(new BigDecimal("0.01")) <= 0
                    ? ErpFinConstants.INTERCOMPANY_MATCH_MATCHED
                    : ErpFinConstants.INTERCOMPANY_MATCH_DIFF;

            ErpFinIntercompanyMatch record = matchDao.newEntity();
            record.setCode("MATCH-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            record.setOrgId(1L);
            record.setPairKey(billCode);
            record.setPeriodId(periodId);
            record.setMatchedAmount(matched);
            record.setDiffAmount(diff);
            record.setStatus(status);
            matchDao.saveEntity(record);
            count++;
        }

        LOG.info("公司间配对完成：期间 {} 识别 {} 条配对记录", periodId, count);
        return count;
    }

    /** 按 billType 反查凭证 ID，按 billCode 分组。 */
    private java.util.Map<String, java.util.List<Long>> findIntercompanyVoucherIdsByBillCode(String billType,
                                                                                             Long periodId) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider().daoFor(ErpFinVoucherBillR.class);
        QueryBean billRQ = new QueryBean();
        billRQ.addFilter(eq("billType", billType));
        List<ErpFinVoucherBillR> billRs = billRDao.findAllByQuery(billRQ);

        java.util.Map<String, java.util.List<Long>> result = new HashMap<>();
        if (billRs.isEmpty()) {
            return result;
        }
        java.util.Set<Long> voucherIds = new java.util.HashSet<>();
        for (ErpFinVoucherBillR br : billRs) {
            voucherIds.add(br.getVoucherId());
        }
        // 仅取本期间内未红冲凭证
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider().daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(in("id", voucherIds));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(vq);
        java.util.Set<Long> validVoucherIds = new java.util.HashSet<>();
        for (ErpFinVoucher v : vouchers) {
            if (!Boolean.TRUE.equals(v.getIsReversed())) {
                validVoucherIds.add(v.getId());
            }
        }
        for (ErpFinVoucherBillR br : billRs) {
            if (validVoucherIds.contains(br.getVoucherId())) {
                result.computeIfAbsent(br.getBillCode(), k -> new java.util.ArrayList<>())
                        .add(br.getVoucherId());
            }
        }
        return result;
    }

    private BigDecimal sumVoucherAmounts(java.util.List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider().daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new java.util.HashSet<>(voucherIds)));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(q);
        BigDecimal total = BigDecimal.ZERO;
        for (ErpFinVoucher v : vouchers) {
            BigDecimal amt = v.getTotalDebit() != null ? v.getTotalDebit() : BigDecimal.ZERO;
            total = total.add(amt);
        }
        return total;
    }

    @Override
    @BizQuery
    public DualSideDiffReport checkDualSideConsistency(@Name("pairKey") String pairKey,
                                                @Name("periodId") Long periodId,
                                                IServiceContext context) {
        DualSideDiffReport report = new DualSideDiffReport();
        report.setDirection("INTERCOMPANY");

        QueryBean q = new QueryBean();
        q.addFilter(eq("pairKey", pairKey));
        if (periodId != null) {
            q.addFilter(eq("periodId", periodId));
        }
        List<ErpFinIntercompanyMatch> matches = daoProvider().daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);

        if (matches.isEmpty()) {
            report.setConsistent(true);
            return report;
        }

        boolean allConsistent = true;
        for (ErpFinIntercompanyMatch m : matches) {
            DualSideDiffRow row = new DualSideDiffRow();
            row.setPartnerId(m.getArOrgId());
            row.setFinanceSettled(m.getMatchedAmount());
            row.setDomainSettled(m.getMatchedAmount());
            row.setDiff(m.getDiffAmount());
            boolean consistent = ErpFinConstants.INTERCOMPANY_MATCH_MATCHED.equals(m.getStatus());
            row.setStatus(consistent ? "CONSISTENT" : "INCONSISTENT");
            report.getRows().add(row);
            if (!consistent) {
                allConsistent = false;
            }
        }
        report.setConsistent(allConsistent);
        return report;
    }

    // ---------- 内部辅助 ----------

    private void assertPeriodOpen(Long periodId) {
        ErpFinAccountingPeriod period = daoProvider().daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND).param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
        String status = period.getStatus();
        if (ErpFinConstants.PERIOD_STATUS_CLOSED.equals(status)
                || ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL.equals(status)) {
            throw new NopException(ErpFinErrors.ERR_INTERCOMPANY_MATCH_PERIOD_CLOSED)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
    }

}
