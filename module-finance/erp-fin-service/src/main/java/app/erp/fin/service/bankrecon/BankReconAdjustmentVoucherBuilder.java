package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 未达账项调整凭证生成/红冲构造器（plan Phase 3）。
 *
 * <p>当余额调节表平衡且存在「银行已记企业未记」（UNMATCHED 银行流水行）时，构造 {@link PostingEvent}
 * 调用 {@link IErpFinVoucherBiz#post} 生成 {@code BANK_RECON_ADJ} 调整凭证：
 * <ul>
 *   <li>{@code billHeadCode} = 调节表 {@code code}（红冲时按此回链反查）。</li>
 *   <li>{@code billData.SUBJECT_CODE} = 资金账户科目编码（{@code FundAccount.subject.code}）。</li>
 *   <li>{@code billData.ADJ_SUBJECT_CODE} = 未达账项对方科目编码（配置 {@code erp-fin.bank-recon-adj-subject-code}）。</li>
 *   <li>{@code billData.TOTAL_BANK_CREDIT} = 银行已收合计（UNMATCHED 且 dc=CREDIT 的金额合计）。</li>
 *   <li>{@code billData.TOTAL_BANK_DEBIT} = 银行已付合计（UNMATCHED 且 dc=DEBIT 的金额合计）。</li>
 * </ul>
 *
 * <p>{@link #reverse} 按 {@code billHeadCode=调节表 code} + {@code businessType=BANK_RECON_ADJ} 反查
 * 原已过账调整凭证，调 {@link IErpFinVoucherBiz#reverse} 生成红字凭证。
 *
 * <p>科目解析前置：{@code FundAccount.subject} 须已配置；未达对方科目经配置必填。
 * 过账态不持久化 FK——调整凭证经 {@code ErpFinVoucherBillR.businessType=BANK_RECON_ADJ} 反查定位（D4）。
 */
public class BankReconAdjustmentVoucherBuilder {

    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IDaoProvider daoProvider;

    /**
     * 生成未达账项调整凭证（若调节表无未达项则空操作返回 null）。
     *
     * @return 调整凭证 ID；无未达项返回 null
     */
    public Long post(ErpFinBankReconciliation recon, ErpFinFundAccount fundAccount,
                     List<ErpFinBankStatementLine> unmatchedLines, IServiceContext context) {
        if (unmatchedLines == null || unmatchedLines.isEmpty()) {
            return null;
        }
        BigDecimal totalBankCredit = BigDecimal.ZERO;
        BigDecimal totalBankDebit = BigDecimal.ZERO;
        for (ErpFinBankStatementLine l : unmatchedLines) {
            if (ErpFinConstants.DC_CREDIT.equals(l.getDcDirection())) {
                totalBankCredit = totalBankCredit.add(nz(l.getAmount()));
            } else if (ErpFinConstants.DC_DEBIT.equals(l.getDcDirection())) {
                totalBankDebit = totalBankDebit.add(nz(l.getAmount()));
            }
        }
        if (totalBankCredit.signum() == 0 && totalBankDebit.signum() == 0) {
            return null;
        }

        String bankSubjectCode = resolveBankSubjectCode(fundAccount);
        String adjSubjectCode = resolveAdjSubjectCode();
        Long acctSchemaId = resolveAcctSchemaId(recon, fundAccount);

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.BANK_RECON_ADJ);
        event.setBillHeadCode(recon.getCode());
        event.setAcctSchemaId(acctSchemaId);
        event.setOrgId(recon.getOrgId());
        event.setCurrencyId(fundAccount.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(recon.getReconciliationDate());
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_BANK_SUBJECT_CODE, bankSubjectCode);
        billData.put("ADJ_SUBJECT_CODE", adjSubjectCode);
        billData.put("TOTAL_BANK_CREDIT", totalBankCredit);
        billData.put("TOTAL_BANK_DEBIT", totalBankDebit);
        event.setBillData(billData);
        return voucherBiz.post(event, context);
    }

    public Long reverse(ErpFinBankReconciliation recon, IServiceContext context) {
        if (!hasAdjustmentVoucher(recon)) {
            return null;
        }
        return voucherBiz.reverse(recon.getCode(), ErpFinBusinessType.BANK_RECON_ADJ, context);
    }

    /** 反查调节表是否已生成 BANK_RECON_ADJ 调整凭证（按 ErpFinVoucherBillR.businessType 反查，对齐 findBillLinks 范式）。 */
    public boolean hasAdjustmentVoucher(ErpFinBankReconciliation recon) {
        return countAdjustmentLinks(recon.getCode()) > 0;
    }

    protected long countAdjustmentLinks(String billHeadCode) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherBillR> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billHeadCode));
        q.addFilter(eq("businessType", ErpFinBusinessType.BANK_RECON_ADJ.name()));
        return dao.findAllByQuery(q).size();
    }

    protected String resolveBankSubjectCode(ErpFinFundAccount fundAccount) {
        if (fundAccount.getSubjectId() == null) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_BANK)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccount.getId())
                    .param(ErpFinErrors.ARG_ACCOUNT_TYPE, fundAccount.getAccountType());
        }
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        app.erp.md.dao.entity.ErpMdSubject subject = dao.getEntityById(fundAccount.getSubjectId());
        if (subject == null || subject.getCode() == null) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_BANK)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccount.getId());
        }
        return subject.getCode();
    }

    protected String resolveAdjSubjectCode() {
        String code = AppConfig.var("erp-fin.bank-recon-adj-subject-code", "2240OTHER");
        if (code == null || code.trim().isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, "erp-fin.bank-recon-adj-subject-code");
        }
        return code.trim();
    }

    protected Long resolveAcctSchemaId(ErpFinBankReconciliation recon, ErpFinFundAccount fundAccount) {
        Long orgId = fundAccount != null ? fundAccount.getOrgId() : null;
        if (orgId == null && recon != null) {
            orgId = recon.getOrgId();
        }
        Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
        return schemaId != null ? schemaId : 1L;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
