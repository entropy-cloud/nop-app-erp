package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.sal.dao.entity.ErpSalReceipt;
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
 * 收款单过账派发器。收款 APPROVED 后组装 {@link PostingEvent}(RECEIPT) 经 {@link SalPostingExecutor}
 * 调用财务过账引擎（借银行存款 / 贷应收，{@code posting.md}）。
 *
 * <p>对齐 {@code SalInvoicePostingDispatcher} 的失败语义与持久化边界：过账失败吞异常保持 APPROVED+posted=false；
 * 本类为 Facade 编排层，不持久化源单据（posted 由调用方 BizModel 主事务内统一持久化）。RECEIPT Provider 归属
 * 已并入 {@link SalAcctDocProvider}（同类集中，见 Phase 2 Decision (a)，与采购域 PAYMENT 口径一致）。
 */
public class SalReceiptPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SalReceiptPostingDispatcher.class);

    @Inject
    SalPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 收款审核通过后调用。成功返回 true；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpSalReceipt receipt) {
        PostingEvent event = buildEvent(receipt);
        try {
            String voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("Receipt document posting failed, receipt {} remains APPROVED, posted=false: {}", receipt.getCode(), e.getMessage());
            } else {
                LOG.error("Receipt document posting error, receipt {} remains APPROVED, posted=false", receipt.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核/作废前红字冲销已过账凭证。冲销是硬前置，失败向上抛出。
     */
    public void reverse(ErpSalReceipt receipt) {
        try {
            executor.reverse(receipt.getCode(), ErpFinBusinessType.RECEIPT);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("Receipt document reversal failed, receipt {}: {}", receipt.getCode(), e.getMessage());
            } else {
                LOG.error("Receipt document reversal error, receipt {}", receipt.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpSalReceipt receipt) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.RECEIPT);
        event.setBillHeadCode(receipt.getCode());
        event.setOrgId(receipt.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(receipt.getOrgId()));
        event.setCurrencyId(receipt.getCurrencyId());
        event.setExchangeRate(receipt.getExchangeRate() != null ? receipt.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = receipt.getBusinessDate() != null ? receipt.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SalAcctDocProvider.KEY_TOTAL, nz(receipt.getTotalAmount()));
        billData.put("CUSTOMER_ID", receipt.getCustomerId());
        event.setBillData(billData);
        return event;
    }

    private String resolveAcctSchemaId(String orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
