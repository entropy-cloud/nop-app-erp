package app.erp.fin.service.intercompany;

import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.dto.GlMappingDimensions;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 跨法人内部交易配对凭证生成器（plan 2026-07-22-1000-1 A3，multi-company.md §Decision B）。
 *
 * <p>一次跨法人调拨 → 两条配对凭证（双法人双账套），**不走 {@code ErpFinAcctDocRegistry} Provider 路由**
 * （与 A2 {@code CommitmentVoucherGenerator} 同型，避免 ErpFinBusinessType 枚举污染）。
 *
 * <p><b>配对凭证结构</b>：
 * <ul>
 *   <li>AR 侧（fromOrg 法人根，内部销售）：Dr INTERCOMPANY_AR / Cr INTERCOMPANY_REVENUE</li>
 *   <li>AP 侧（toOrg 法人根，内部采购）：Dr INTERCOMPANY_COST / Cr INTERCOMPANY_AP</li>
 * </ul>
 *
 * <p>科目经 A1 {@link IErpFinGlMappingResolver} 按 INTERCOMPANY_* accountKey + intercompany 维度解析
 * （解除 A1 Deferred「intercompany 维度规则」）；规则表无匹配时回落默认科目编码（5001/6001/1401/2202）。
 *
 * <p>业财回链：billType=INTERCOMPANY_SALE/PURCHASE，billCode=调拨单 code（按调拨单反查配对凭证）。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §Decision B}。
 */
public class IntercompanyVoucherGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(IntercompanyVoucherGenerator.class);

    /** 默认回落科目编码（规则表无匹配时兜底，与各 Provider 既有硬编码常量同型）。 */
    static final String DEFAULT_AR_SUBJECT_CODE = "1131";
    static final String DEFAULT_REVENUE_SUBJECT_CODE = "5001";
    static final String DEFAULT_COST_SUBJECT_CODE = "1401";
    static final String DEFAULT_AP_SUBJECT_CODE = "2202";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinGlMappingResolver glMappingResolver;

    /**
     * 生成配对凭证（AR 侧 + AP 侧）。
     *
     * @param transferOrderCode 调拨单 code（业财回链）
     * @param fromOrgLegalId    调出方法人根 ID
     * @param toOrgLegalId      调入方法人根 ID
     * @param fromAcctSchemaId  调出方账套
     * @param toAcctSchemaId    调入方账套
     * @param periodId          会计期间
     * @param currencyId        币种
     * @param amount            交易金额（本位币，正数）
     * @return 配对凭证 ID 列表（AR 凭证 + AP 凭证）
     */
    public List<Long> generatePairedVouchers(String transferOrderCode, Long fromOrgLegalId, Long toOrgLegalId,
                                             Long fromAcctSchemaId, Long toAcctSchemaId, Long periodId,
                                             Long currencyId, BigDecimal amount) {
        List<Long> voucherIds = new ArrayList<>();
        if (amount == null || amount.signum() <= 0) {
            return voucherIds;
        }

        GlMappingDimensions intercompanyDims = new GlMappingDimensions();
        intercompanyDims.setFromOrgId(fromOrgLegalId);
        intercompanyDims.setToOrgId(toOrgLegalId);

        // AR 侧凭证（fromOrg 内部销售）：Dr INTERCOMPANY_AR / Cr INTERCOMPANY_REVENUE
        String arSubjectCode = resolveSubjectCode(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,
                ErpFinConstants.ACCOUNT_KEY_INTERCOMPANY_AR, intercompanyDims, fromAcctSchemaId,
                DEFAULT_AR_SUBJECT_CODE);
        String revenueSubjectCode = resolveSubjectCode(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,
                ErpFinConstants.ACCOUNT_KEY_INTERCOMPANY_REVENUE, intercompanyDims, fromAcctSchemaId,
                DEFAULT_REVENUE_SUBJECT_CODE);
        Long arVoucherId = writeIntercompanyVoucher(transferOrderCode, ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,
                fromOrgLegalId, fromAcctSchemaId, periodId, currencyId, amount,
                arSubjectCode, "内部应收", revenueSubjectCode, "内部销售收入");
        if (arVoucherId != null) {
            voucherIds.add(arVoucherId);
        }

        // AP 侧凭证（toOrg 内部采购）：Dr INTERCOMPANY_COST / Cr INTERCOMPANY_AP
        String costSubjectCode = resolveSubjectCode(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE,
                ErpFinConstants.ACCOUNT_KEY_INTERCOMPANY_COST, intercompanyDims, toAcctSchemaId,
                DEFAULT_COST_SUBJECT_CODE);
        String apSubjectCode = resolveSubjectCode(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE,
                ErpFinConstants.ACCOUNT_KEY_INTERCOMPANY_AP, intercompanyDims, toAcctSchemaId,
                DEFAULT_AP_SUBJECT_CODE);
        Long apVoucherId = writeIntercompanyVoucher(transferOrderCode, ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE,
                toOrgLegalId, toAcctSchemaId, periodId, currencyId, amount,
                costSubjectCode, "内部采购成本", apSubjectCode, "内部应付");
        if (apVoucherId != null) {
            voucherIds.add(apVoucherId);
        }

        LOG.info("跨法人内部交易配对凭证生成：调拨单 {} AR凭证={} AP凭证={} 金额={}",
                transferOrderCode, arVoucherId, apVoucherId, amount);
        return voucherIds;
    }

    /**
     * 红冲配对 intercompany 凭证（reverseApprove）。按 {@code billCode} 反查 approve 时生成的配对凭证
     * （billType=INTERCOMPANY_SALE/PURCHASE），逐张生成红字冲销凭证（借贷互换、isReversed=true、reversalOfVoucherId 回链）。
     *
     * <p>对齐 {@code CommitmentVoucherGenerator.reverseCommitment} 范式（plan 2026-07-24-1351-2）。
     *
     * @param sourceBillCode 业财回链 billCode（订单 code）
     * @return 红冲凭证 ID 列表（空列表表示无原凭证可红冲）
     */
    public List<Long> reverseIntercompany(String sourceBillCode) {
        List<ErpFinVoucher> originals = findIntercompanyVouchers(sourceBillCode);
        List<Long> reversalIds = new ArrayList<>();
        for (ErpFinVoucher original : originals) {
            if (Boolean.TRUE.equals(original.getIsReversed())) {
                continue;
            }
            List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());
            ErpFinVoucher reversal = writeIntercompanyReversalFromLines(original, origLines);
            if (reversal != null) {
                original.setIsReversed(true);
                daoProvider.daoFor(ErpFinVoucher.class).updateEntity(original);
                reversalIds.add(reversal.getId());
            }
        }
        LOG.info("跨法人内部交易红冲：单据 {} → 红冲凭证 {}", sourceBillCode, reversalIds);
        return reversalIds;
    }

    /** 检查给定 billCode 是否存在未红冲的 intercompany 配对凭证（reverseApprove 守卫）。 */
    public boolean hasUnreversedIntercompany(String sourceBillCode) {
        for (ErpFinVoucher v : findIntercompanyVouchers(sourceBillCode)) {
            if (!Boolean.TRUE.equals(v.getIsReversed())) {
                return true;
            }
        }
        return false;
    }

    private List<ErpFinVoucher> findIntercompanyVouchers(String sourceBillCode) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        io.nop.api.core.beans.query.QueryBean bq = new io.nop.api.core.beans.query.QueryBean();
        bq.addFilter(io.nop.api.core.beans.FilterBeans.eq("billCode", sourceBillCode));
        bq.addFilter(io.nop.api.core.beans.FilterBeans.in("billType",
                java.util.Arrays.asList(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,
                        ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE)));
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
        return voucherDao.findAllByQuery(vq);
    }

    private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private ErpFinVoucher writeIntercompanyReversalFromLines(ErpFinVoucher original,
                                                              List<ErpFinVoucherLine> origLines) {
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

        String origBillType = origLines.get(0).getBusinessType();
        ErpFinVoucher reversal = voucherDao.newEntity();
        reversal.setCode(ErpFinConstants.INTERCOMPANY_VOUCHER_REVERSAL_BILL_CODE_PREFIX
                + StringHelper.generateUUID().substring(0, 12));
        reversal.setVoucherType("TRANSFER");
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
            line.setBusinessType(ol.getBusinessType());
            line.setMemo("跨法人内部交易红冲");
            lineDao.saveEntity(line);
        }

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(reversalId);
        billR.setBillType(origBillType);
        billR.setBillCode(findOriginalIntercompanyBillCode(original.getId()));
        billR.setBusinessType(origBillType);
        billRDao.saveEntity(billR);

        return reversal;
    }

    private String findOriginalIntercompanyBillCode(Long voucherId) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucherId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.in("billType",
                java.util.Arrays.asList(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,
                        ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE)));
        q.setLimit(1);
        List<ErpFinVoucherBillR> list = billRDao.findAllByQuery(q);
        return list.isEmpty() ? "" : list.get(0).getBillCode();
    }

    /**
     * 经 A1 GlMappingResolver 解析 intercompany 科目；规则表无匹配时回落默认编码。
     */
    private String resolveSubjectCode(String billTypeForLog, String accountKey, GlMappingDimensions dims,
                                      Long acctSchemaId, String defaultCode) {
        if (glMappingResolver == null) {
            return defaultCode;
        }
        try {
            String resolved = glMappingResolver.resolveSubjectCode(billTypeForLog, accountKey, dims, acctSchemaId);
            return resolved != null ? resolved : defaultCode;
        } catch (RuntimeException e) {
            LOG.debug("intercompany 科目解析失败，回落默认：billType={} accountKey={}: {}", billTypeForLog, accountKey,
                    e.getMessage());
            return defaultCode;
        }
    }

    private Long writeIntercompanyVoucher(String transferOrderCode, String billType, Long orgId, Long acctSchemaId,
                                          Long periodId, Long currencyId, BigDecimal amount,
                                          String debitSubjectCode, String debitSubjectName,
                                          String creditSubjectCode, String creditSubjectName) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpMdSubject debitSubject = findSubjectByCode(debitSubjectCode);
        ErpMdSubject creditSubject = findSubjectByCode(creditSubjectCode);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(ErpFinConstants.INTERCOMPANY_VOUCHER_BILL_CODE_PREFIX
                + StringHelper.generateUUID().substring(0, 12));
        voucher.setVoucherType("TRANSFER");
        voucher.setVoucherDate(CoreMetrics.today());
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(amount);
        voucher.setTotalCredit(amount);
        voucher.setIsReversed(false);
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentTimestamp());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        // 借方行
        ErpFinVoucherLine debitLine = lineDao.newEntity();
        debitLine.setVoucherId(voucherId);
        debitLine.setLineNo(1);
        applySubject(debitLine, debitSubject, debitSubjectCode, debitSubjectName);
        debitLine.setDcDirection(ErpFinConstants.DC_DEBIT);
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setCurrencyId(currencyId);
        debitLine.setExchangeRate(BigDecimal.ONE);
        debitLine.setAmountSource(amount);
        debitLine.setAmountFunctional(amount);
        debitLine.setAcctSchemaId(acctSchemaId);
        debitLine.setOrgId(orgId);
        debitLine.setBusinessType(billType);
        debitLine.setMemo(debitSubjectName);
        lineDao.saveEntity(debitLine);

        // 贷方行
        ErpFinVoucherLine creditLine = lineDao.newEntity();
        creditLine.setVoucherId(voucherId);
        creditLine.setLineNo(2);
        applySubject(creditLine, creditSubject, creditSubjectCode, creditSubjectName);
        creditLine.setDcDirection(ErpFinConstants.DC_CREDIT);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setCurrencyId(currencyId);
        creditLine.setExchangeRate(BigDecimal.ONE);
        creditLine.setAmountSource(amount);
        creditLine.setAmountFunctional(amount);
        creditLine.setAcctSchemaId(acctSchemaId);
        creditLine.setOrgId(orgId);
        creditLine.setBusinessType(billType);
        creditLine.setMemo(creditSubjectName);
        lineDao.saveEntity(creditLine);

        // 业财回链
        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucherId);
        billR.setBillType(billType);
        billR.setBillCode(transferOrderCode);
        billR.setBusinessType(billType);
        billRDao.saveEntity(billR);

        return voucherId;
    }

    private void applySubject(ErpFinVoucherLine line, ErpMdSubject subject, String fallbackCode, String fallbackName) {
        if (subject != null) {
            line.setSubjectId(subject.getId());
            line.setSubjectCode(subject.getCode());
            line.setSubjectName(subject.getName());
        } else {
            line.setSubjectCode(fallbackCode);
            line.setSubjectName(fallbackName);
        }
    }

    private ErpMdSubject findSubjectByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
