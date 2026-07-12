package app.erp.pur.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.pur.dao.entity.ErpPurPayment;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 付款单过账派发器。付款 APPROVED 后组装 {@link PostingEvent}(PAYMENT) 经 {@link PurPostingExecutor}
 * 调用财务过账引擎（借应付 / 贷银行存款，{@code posting.md}）。
 *
 * <p>对齐 {@code PurInvoicePostingDispatcher} 的失败语义与持久化边界：过账失败吞异常保持 APPROVED+posted=false；
 * 本类为 Facade 编排层，不持久化源单据（posted 由调用方 BizModel 主事务内统一持久化）。PAYMENT Provider 归属
 * 已并入 {@link PurAcctDocProvider}（同类集中，见 Phase 2 Decision (a)）。
 */
public class PurPaymentPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PurPaymentPostingDispatcher.class);

    @Inject
    PurPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 付款审核通过后调用。成功返回 true；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpPurPayment payment) {
        PostingEvent event = buildEvent(payment);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("付款单过账失败，付款单 {} 保持 APPROVED、posted=false：{}", payment.getCode(), e.getMessage());
            } else {
                LOG.error("付款单过账异常，付款单 {} 保持 APPROVED、posted=false", payment.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核/作废前红字冲销已过账凭证。冲销是硬前置，失败向上抛出。
     */
    public void reverse(ErpPurPayment payment) {
        try {
            executor.reverse(payment.getCode(), ErpFinBusinessType.PAYMENT);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("付款单红字冲销失败，付款单 {}：{}", payment.getCode(), e.getMessage());
            } else {
                LOG.error("付款单红字冲销异常，付款单 {}", payment.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpPurPayment payment) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PAYMENT);
        event.setBillHeadCode(payment.getCode());
        event.setOrgId(payment.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(payment.getOrgId()));
        event.setCurrencyId(payment.getCurrencyId());
        event.setExchangeRate(payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = payment.getBusinessDate() != null ? payment.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(PurAcctDocProvider.KEY_TOTAL, nz(payment.getTotalAmount()));
        billData.put("SUPPLIER_ID", payment.getSupplierId());
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
