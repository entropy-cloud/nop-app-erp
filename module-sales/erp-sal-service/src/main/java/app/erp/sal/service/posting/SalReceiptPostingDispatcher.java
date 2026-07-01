package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalReceipt;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("收款单过账失败，收款单 {} 保持 APPROVED、posted=false：{}", receipt.getCode(), e.getMessage());
            } else {
                LOG.error("收款单过账异常，收款单 {} 保持 APPROVED、posted=false", receipt.getCode(), e);
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
                LOG.warn("收款单红字冲销失败，收款单 {}：{}", receipt.getCode(), e.getMessage());
            } else {
                LOG.error("收款单红字冲销异常，收款单 {}", receipt.getCode(), e);
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

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("orgId", orgId));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
