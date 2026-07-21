package app.erp.fin.service.budget;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 承付（COMMITMENT）凭证生成器（A2，plan 2026-07-21-1206-2，budget.md §承付会计）。
 *
 * <p>承付作为 {@code postingType=COMMITMENT} 的影子凭证，与实际凭证（{@code postingType=NORMAL/NULL}）和预算凭证
 * （{@code postingType=BUDGET}）并行入账，复用凭证结构（Voucher + VoucherLine + VoucherBillR），但**不走 Provider 模型**
 * （与 {@link BudgetVoucherGenerator} 同型，避免 ErpFinBusinessType 枚举污染）。
 *
 * <p><b>借贷规则</b>：单边凭证（Dr 配置的承付占用科目 / Cr 配置的应付-承付科目或对侧科目）。
 * 简化实现：仅写 Dr 行（承付占用科目，金额取自 commit 入参），Cr 行使用同一科目反向（保持平衡），
 * 实际部署时由客户在 ErpMdSubject 中配置承付科目对（Dr 占用 / Cr 释放）。
 *
 * <p><b>业财回链</b>：{@code billType=PURCHASE_ORDER_COMMITMENT}，{@code billCode=订单 code}，便于按订单反查全部承付凭证。
 *
 * <p>本类为 Bean（需 {@link IDaoProvider}），由 {@code ErpFinBudgetCommitmentBizModel} 在 commit/release 事务内调用。
 */
public class CommitmentVoucherGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CommitmentVoucherGenerator.class);

    public static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";

    @Inject
    IDaoProvider daoProvider;

    /**
     * 生成承付凭证（commit）。
     *
     * @param sourceBillCode 订单号（业财回链 billCode）
     * @param subject        承付占用科目
     * @param costCenterId   成本中心（可空）
     * @param orgId          组织
     * @param acctSchemaId   账套
     * @param periodId       期间
     * @param currencyId     币种
     * @param amount         金额（本位币，正数）
     * @return 凭证 ID
     */
    public Long generateCommitment(String sourceBillCode, ErpMdSubject subject, Long costCenterId,
                                   Long orgId, Long acctSchemaId, Long periodId, Long currencyId,
                                   BigDecimal amount) {
        ErpFinVoucher voucher = writeCommitmentVoucher(sourceBillCode, subject, costCenterId, orgId,
                acctSchemaId, periodId, currencyId, amount, false, null);
        return voucher != null ? voucher.getId() : null;
    }

    /**
     * 红冲承付凭证（release）。按 {@code billCode=订单 code} 反查承付凭证，逐张生成红字冲销凭证
     * （金额取负，{@code isReversed=true}，{@code reversalOfVoucherId} 指向原凭证）。
     *
     * @return 红冲凭证 ID 列表（空列表表示无原凭证可红冲）
     */
    public List<Long> reverseCommitment(String sourceBillCode) {
        List<ErpFinVoucher> originals = findCommitmentVouchers(sourceBillCode);
        List<Long> reversalIds = new ArrayList<>();
        for (ErpFinVoucher original : originals) {
            if (Boolean.TRUE.equals(original.getIsReversed())) {
                continue;
            }
            List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());
            ErpFinVoucher reversal = writeReversalFromLines(original, origLines);
            if (reversal != null) {
                original.setIsReversed(true);
                daoProvider.daoFor(ErpFinVoucher.class).updateEntity(original);
                reversalIds.add(reversal.getId());
            }
        }
        return reversalIds;
    }

    /** 检查给定订单 code 是否存在未红冲的承付凭证（release 守卫：无原凭证时返回 false）。 */
    public boolean hasUnreversedCommitment(String sourceBillCode) {
        for (ErpFinVoucher v : findCommitmentVouchers(sourceBillCode)) {
            if (!Boolean.TRUE.equals(v.getIsReversed())) {
                return true;
            }
        }
        return false;
    }

    private ErpFinVoucher writeCommitmentVoucher(String sourceBillCode, ErpMdSubject subject, Long costCenterId,
                                                 Long orgId, Long acctSchemaId, Long periodId, Long currencyId,
                                                 BigDecimal amount, boolean isReversal, Long reversalOfVoucherId) {
        if (amount == null || amount.signum() == 0 || subject == null) {
            return null;
        }
        BigDecimal absAmount = amount.abs();
        String dc = resolveDcDirection(subject);

        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(ErpFinConstants.COMMITMENT_VOUCHER_BILL_CODE_PREFIX + StringHelper.generateUUID().substring(0, 12));
        voucher.setVoucherType(VOUCHER_TYPE_TRANSFER);
        voucher.setPostingType(ErpFinConstants.POSTING_TYPE_COMMITMENT);
        voucher.setVoucherDate(CoreMetrics.today());
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        boolean isCredit = ErpFinConstants.DC_CREDIT.equals(dc);
        BigDecimal debit = isCredit ? BigDecimal.ZERO : absAmount;
        BigDecimal credit = isCredit ? absAmount : BigDecimal.ZERO;
        voucher.setTotalDebit(debit);
        voucher.setTotalCredit(credit);
        voucher.setIsReversed(isReversal);
        if (reversalOfVoucherId != null) {
            voucher.setReversalOfVoucherId(reversalOfVoucherId);
        }
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentTimestamp());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        ErpFinVoucherLine line = lineDao.newEntity();
        line.setVoucherId(voucherId);
        line.setLineNo(1);
        line.setSubjectId(subject.getId());
        line.setSubjectCode(subject.getCode());
        line.setSubjectName(subject.getName());
        line.setDcDirection(dc);
        line.setDebitAmount(debit);
        line.setCreditAmount(credit);
        line.setCurrencyId(currencyId);
        line.setExchangeRate(BigDecimal.ONE);
        line.setAmountSource(absAmount);
        line.setAmountFunctional(absAmount);
        line.setAcctSchemaId(acctSchemaId);
        line.setOrgId(orgId);
        line.setBusinessType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
        line.setMemo(isReversal ? "承付释放红冲" : "采购订单承付占用");
        line.setCostCenterId(costCenterId);
        lineDao.saveEntity(line);

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucherId);
        billR.setBillType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
        billR.setBillCode(sourceBillCode);
        billR.setBusinessType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
        billRDao.saveEntity(billR);

        return voucher;
    }

    private ErpFinVoucher writeReversalFromLines(ErpFinVoucher original, List<ErpFinVoucherLine> origLines) {
        if (origLines.isEmpty()) {
            return null;
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : origLines) {
            BigDecimal debit = l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO;
            totalDebit = totalDebit.add(credit);
            totalCredit = totalCredit.add(debit);
        }
        if (totalDebit.signum() == 0 && totalCredit.signum() == 0) {
            return null;
        }

        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher reversal = voucherDao.newEntity();
        reversal.setCode(ErpFinConstants.COMMITMENT_VOUCHER_REVERSAL_BILL_CODE_PREFIX
                + StringHelper.generateUUID().substring(0, 12));
        reversal.setVoucherType(VOUCHER_TYPE_TRANSFER);
        reversal.setPostingType(ErpFinConstants.POSTING_TYPE_COMMITMENT);
        reversal.setVoucherDate(CoreMetrics.today());
        reversal.setOrgId(original.getOrgId());
        reversal.setAcctSchemaId(original.getAcctSchemaId());
        reversal.setPeriodId(original.getPeriodId());
        reversal.setTotalDebit(totalDebit);
        reversal.setTotalCredit(totalCredit);
        reversal.setIsReversed(true);
        reversal.setReversalOfVoucherId(original.getId());
        reversal.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        reversal.setPostedAt(CoreMetrics.currentTimestamp());
        voucherDao.saveEntity(reversal);
        Long reversalId = reversal.getId();

        int lineNo = 1;
        for (ErpFinVoucherLine ol : origLines) {
            BigDecimal origDebit = ol.getDebitAmount() != null ? ol.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal origCredit = ol.getCreditAmount() != null ? ol.getCreditAmount() : BigDecimal.ZERO;
            ErpFinVoucherLine line = lineDao.newEntity();
            line.setVoucherId(reversalId);
            line.setLineNo(lineNo++);
            line.setSubjectId(ol.getSubjectId());
            line.setSubjectCode(ol.getSubjectCode());
            line.setSubjectName(ol.getSubjectName());
            line.setDcDirection(ol.getDcDirection());
            line.setDebitAmount(origCredit);
            line.setCreditAmount(origDebit);
            line.setCurrencyId(ol.getCurrencyId());
            line.setExchangeRate(ol.getExchangeRate() != null ? ol.getExchangeRate() : BigDecimal.ONE);
            line.setAmountSource(origDebit.add(origCredit));
            line.setAmountFunctional(origDebit.add(origCredit));
            line.setAcctSchemaId(ol.getAcctSchemaId());
            line.setOrgId(ol.getOrgId());
            line.setBusinessType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
            line.setMemo("承付释放红冲");
            line.setCostCenterId(ol.getCostCenterId());
            lineDao.saveEntity(line);
        }

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(reversalId);
        billR.setBillType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
        billR.setBillCode(findOriginalBillCode(original.getId()));
        billR.setBusinessType(ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE);
        billRDao.saveEntity(billR);

        return reversal;
    }

    private String findOriginalBillCode(Long voucherId) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucherId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("billType", ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE));
        q.setLimit(1);
        List<ErpFinVoucherBillR> list = billRDao.findAllByQuery(q);
        return list.isEmpty() ? "" : list.get(0).getBillCode();
    }

    /** 资产/费用类（DEBIT 余额方向）记借方；负债/收入类（CREDIT 余额方向）记贷方。 */
    private String resolveDcDirection(ErpMdSubject subject) {
        return ErpFinConstants.DC_CREDIT.equals(subject.getDirection())
                ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;
    }

    private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucher> findCommitmentVouchers(String sourceBillCode) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        io.nop.api.core.beans.query.QueryBean bq = new io.nop.api.core.beans.query.QueryBean();
        bq.addFilter(io.nop.api.core.beans.FilterBeans.eq("billCode", sourceBillCode));
        bq.addFilter(io.nop.api.core.beans.FilterBeans.eq("billType", ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE));
        List<ErpFinVoucherBillR> links = billRDao.findAllByQuery(bq);
        if (links.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> voucherIds = new ArrayList<>(links.size());
        for (ErpFinVoucherBillR link : links) {
            voucherIds.add(link.getVoucherId());
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        io.nop.api.core.beans.query.QueryBean vq = new io.nop.api.core.beans.query.QueryBean();
        vq.addFilter(io.nop.api.core.beans.FilterBeans.in("id", voucherIds));
        vq.addFilter(io.nop.api.core.beans.FilterBeans.eq("postingType", ErpFinConstants.POSTING_TYPE_COMMITMENT));
        return voucherDao.findAllByQuery(vq);
    }
}
