package app.erp.fin.service.treasury;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授信利息计提凭证构造器（plan 2026-07-18-0718-1，{@code treasury.md §业财过账 CREDIT_FACILITY_INTEREST}）。
 *
 * <p>镜像 {@code BankReconAdjustmentVoucherBuilder} 范式（VoucherBuilder 模式非 BizModel 直调）：由
 * {@code ErpFinCreditFacilityBizModel.accrueInterest} 计算利息金额后委派本 Builder 构造 {@link PostingEvent}
 * 并调 {@link IErpFinVoucherBiz#post}（经 {@code ErpFinAcctDocRegistry} 路由到
 * {@code CreditFacilityInterestAcctDocProvider}）。
 *
 * <p>{@code billHeadCode}=「CFI-INT-{facilityId}-{fromDate}_{toDate}」作**区间级幂等键**：同 facility + 同区间
 * 二次调用经 {@code IErpFinVoucherBiz.post} 内置 {@code alreadyPosted}（按 {@code (billHeadCode, businessType)}
 * 反查）命中返回 {@code null}，无第二张凭证。
 *
 * <p>科目经 Provider 硬编码（Dr 6603 财务费用-利息支出 / Cr 1002 银行存款，treasury.md:148）。
 * 利率与计息公式经 BizModel 计算，本 Builder 仅承载 PostingEvent 构造 + 凭证生成委派。
 *
 * <p>币种解析顺序：{@code facility.fundAccount.currencyId} → {@code acctSchema.functionalCurrencyId} 兜底
 * （对齐 plan §Phase 1「账套本位币兜底」裁决，使 facility 未绑定资金账户时仍可计提）。
 */
public class CreditFacilityInterestVoucherBuilder {

    /** billHeadCode 前缀（区间级幂等键组成：CFI-INT-{facilityId}-{fromDate}_{toDate}）。 */
    public static final String BILL_HEAD_CODE_PREFIX = "CFI-INT-";

    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IDaoProvider daoProvider;

    /**
     * 构造授信利息计提 PostingEvent 并调 {@link IErpFinVoucherBiz#post} 生成凭证。
     *
     * @param facility       授信额度实体（须非空，含 orgId/fundAccountId/usedAmount）
     * @param fromDate       计息开始日（闭区间）
     * @param toDate         计息结束日（闭区间，作 voucherDate）
     * @param interestAmount 已计算的利息金额（本位币，HALF_UP scale=4）
     * @param context        服务上下文
     * @return 新生成的凭证 ID；幂等命中（已过账同区间）返回 {@code null}
     */
    public Long post(ErpFinCreditFacility facility, LocalDate fromDate, LocalDate toDate,
                     BigDecimal interestAmount, IServiceContext context) {
        Long acctSchemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, facility.getOrgId());
        if (acctSchemaId == null) {
            acctSchemaId = 1L;
        }
        Long currencyId = resolveCurrencyId(facility, acctSchemaId);

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.CREDIT_FACILITY_INTEREST);
        event.setBillHeadCode(buildBillHeadCode(facility.getId(), fromDate, toDate));
        event.setAcctSchemaId(acctSchemaId);
        event.setOrgId(facility.getOrgId());
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(toDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL", interestAmount);
        billData.put("FUND_ACCOUNT_ID", facility.getFundAccountId());
        event.setBillData(billData);

        return voucherBiz.post(event, context);
    }

    /** 构造区间级幂等 billHeadCode：CFI-INT-{facilityId}-{fromDate}_{toDate}。 */
    public static String buildBillHeadCode(Long facilityId, LocalDate fromDate, LocalDate toDate) {
        return BILL_HEAD_CODE_PREFIX + facilityId + "-" + fromDate + "_" + toDate;
    }

    /**
     * 币种解析：优先 {@code facility.fundAccount.currencyId}；facility 未绑定资金账户或账户无币种时
     * 兜底取 {@code acctSchema.functionalCurrencyId}（本位币）。
     */
    protected Long resolveCurrencyId(ErpFinCreditFacility facility, Long acctSchemaId) {
        if (facility.getFundAccountId() != null) {
            IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
            ErpFinFundAccount account = dao.getEntityById(facility.getFundAccountId());
            if (account != null && account.getCurrencyId() != null) {
                return account.getCurrencyId();
            }
        }
        if (acctSchemaId != null) {
            IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema schema = dao.getEntityById(acctSchemaId);
            if (schema != null) {
                return schema.getFunctionalCurrencyId();
            }
        }
        return null;
    }
}
