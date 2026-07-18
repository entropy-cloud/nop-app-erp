package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
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
import io.nop.api.core.time.CoreMetrics;

/**
 * 员工借款过账派发器。借款 APPROVED 后组装 {@link PostingEvent}(EMPLOYEE_ADVANCE) 经 {@link FinPostingExecutor}
 * 调用过账引擎（借其他应收款-员工预支 / 贷银行存款，金额=票面）。
 *
 * <p>对齐 {@code ExpenseClaimPostingDispatcher} 的失败语义：过账失败吞异常保持 APPROVED+posted=false。
 * billData 的 {@code EMPLOYEE_ID} 键携带<b>已解析的 {@code employee.partnerId}</b>，供 ArApItemGenerator
 * 生成员工预支应收辅助账。
 */
public class EmployeeAdvancePostingDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(EmployeeAdvancePostingDispatcher.class);

    @Inject
    FinPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    public boolean tryPost(ErpFinEmployeeAdvance advance) {
        PostingEvent event = buildEvent(advance);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("借款单过账失败，借款单 {} 保持 APPROVED、posted=false：{}", advance.getCode(), e.getMessage());
            } else {
                LOG.error("借款单过账异常，借款单 {} 保持 APPROVED、posted=false", advance.getCode(), e);
            }
            return false;
        }
    }

    public void reverse(ErpFinEmployeeAdvance advance) {
        executor.reverse(advance.getCode(), ErpFinBusinessType.EMPLOYEE_ADVANCE);
    }

    /**
     * 抵扣清算过账（由 {@code AdvanceOffsetOrchestrator} 调用）：EMPLOYEE_ADVANCE_SETTLE 净额清算凭证
     * （借应付-员工 / 贷其他应收款-员工预支）。成功返回 true。
     */
    public boolean postSettle(String claimCode, Long partnerId, BigDecimal netAmount, Long orgId,
                              Long currencyId, java.time.LocalDate voucherDate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
        event.setBillHeadCode(claimCode);
        event.setOrgId(orgId);
        event.setAcctSchemaId(resolveAcctSchemaId(orgId));
        event.setCurrencyId(currencyId != null ? currencyId : 1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate != null ? voucherDate : CoreMetrics.today());
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, partnerId);
        billData.put("TOTAL", netAmount);
        event.setBillData(billData);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            LOG.error("借款清算过账失败，报销单 {} 净额 {}：{}", claimCode, netAmount, e.getMessage(), e);
            return false;
        }
    }

    public void reverseSettle(String claimCode) {
        executor.reverse(claimCode, ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
    }

    /**
     * 现金还款过账（由 {@code ErpFinEmployeeAdvanceBizModel.cashRepay} 调用）：EMPLOYEE_ADVANCE_SETTLE 现金还款凭证
     * （借银行存款 / 贷其他应收款-员工预支，{@code billData.SETTLE_TYPE=CASH}）。
     *
     * <p>billHeadCode 形如 {@code EA-CASH-REPAY-<advanceCode>-<millis>}，含时间戳避免同 advance 多次还款碰撞。
     * 失败语义对齐 {@link #postSettle}：catch Exception → log + return false（不阻断业务字段更新，残留风险由调用方记录）。
     */
    public boolean postCashRepay(ErpFinEmployeeAdvance advance, BigDecimal amount, io.nop.core.context.IServiceContext context) {
        Long partnerId = resolveEmployeePartnerId(advance.getEmployeeId());

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
        event.setBillHeadCode("EA-CASH-REPAY-" + advance.getCode() + "-" + CoreMetrics.currentTimeMillis());
        event.setOrgId(advance.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(advance.getOrgId()));
        event.setCurrencyId(advance.getCurrencyId() != null ? advance.getCurrencyId() : 1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(CoreMetrics.today());

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, partnerId);
        billData.put("TOTAL", amount);
        billData.put(ErpFinConstants.BILL_DATA_SETTLE_TYPE, ErpFinConstants.SETTLE_TYPE_CASH);
        event.setBillData(billData);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            LOG.error("借款现金还款过账失败，借款单 {} 还款金额 {}：{}", advance.getCode(), amount, e.getMessage(), e);
            return false;
        }
    }

    private PostingEvent buildEvent(ErpFinEmployeeAdvance advance) {
        Long partnerId = resolveEmployeePartnerId(advance.getEmployeeId());

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EMPLOYEE_ADVANCE);
        event.setBillHeadCode(advance.getCode());
        event.setOrgId(advance.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(advance.getOrgId()));
        event.setCurrencyId(advance.getCurrencyId());
        event.setExchangeRate(advance.getExchangeRate() != null ? advance.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = advance.getBusinessDate() != null ? advance.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, partnerId);
        billData.put("TOTAL", nz(advance.getAmountFunctional()));
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    /** 经 daoProvider 加载员工读取 partnerId（避免跨会话关系懒加载）。 */
    private Long resolveEmployeePartnerId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = dao.getEntityById(employeeId);
        return emp != null ? emp.getPartnerId() : null;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
