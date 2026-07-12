package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdEmployee;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 费用报销过账派发器。报销 APPROVED 后组装 {@link PostingEvent}(EXPENSE_CLAIM) 经 {@link FinPostingExecutor}
 * 调用过账引擎（借费用科目/借进项税/贷方按 paymentMode：own_account→应付-员工，company_account→银行存款）。
 *
 * <p>对齐 {@code SalReceiptPostingDispatcher} 的失败语义：过账失败吞异常保持 APPROVED+posted=false；
 * 本类为 Facade 编排层，不持久化源单据（posted 由调用方 BizModel 主事务内统一持久化）。
 *
 * <p>billData 的 {@code EMPLOYEE_ID} 键携带<b>已解析的 {@code claimant.partnerId}</b>（即 ErpMdPartner.id，
 * 非 employee.id），供 ArApItemGenerator 直接采用生成员工应付辅助账（见 plan Task Route Decision）。
 */
public class ExpenseClaimPostingDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(ExpenseClaimPostingDispatcher.class);

    @Inject
    FinPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    public boolean tryPost(ErpFinExpenseClaim claim) {
        PostingEvent event = buildEvent(claim);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("报销单过账失败，报销单 {} 保持 APPROVED、posted=false：{}", claim.getCode(), e.getMessage());
            } else {
                LOG.error("报销单过账异常，报销单 {} 保持 APPROVED、posted=false", claim.getCode(), e);
            }
            return false;
        }
    }

    public void reverse(ErpFinExpenseClaim claim) {
        executor.reverse(claim.getCode(), ErpFinBusinessType.EXPENSE_CLAIM);
    }

    private PostingEvent buildEvent(ErpFinExpenseClaim claim) {
        Long partnerId = resolveEmployeePartnerId(claim.getClaimantId());

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM);
        event.setBillHeadCode(claim.getCode());
        event.setOrgId(claim.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(claim.getOrgId()));
        event.setCurrencyId(claim.getCurrencyId());
        event.setExchangeRate(claim.getExchangeRate() != null ? claim.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = claim.getBusinessDate() != null ? claim.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, partnerId);
        billData.put(ErpFinConstants.BILL_DATA_TOTAL_AMOUNT, nz(claim.getAmountWithoutTax()));
        billData.put(ErpFinConstants.BILL_DATA_TOTAL_TAX_AMOUNT, nz(claim.getTaxAmount()));
        billData.put(ErpFinConstants.BILL_DATA_TOTAL_AMOUNT_WITH_TAX, nz(claim.getAmountWithTax()));
        billData.put(ErpFinConstants.BILL_DATA_PAYMENT_MODE,
                claim.getPaymentMode() != null ? claim.getPaymentMode() : ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        if (claim.getDepartmentId() != null) {
            billData.put(ErpFinConstants.BILL_DATA_DEPARTMENT_ID, claim.getDepartmentId());
        }
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    /** 经 daoProvider 加载员工读取 partnerId（避免跨会话关系懒加载；master-data-service 为 test 作用域，对齐 PartnerBalanceUpdater）。 */
    private Long resolveEmployeePartnerId(Long claimantId) {
        if (claimantId == null) {
            return null;
        }
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = dao.getEntityById(claimantId);
        return emp != null ? emp.getPartnerId() : null;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
