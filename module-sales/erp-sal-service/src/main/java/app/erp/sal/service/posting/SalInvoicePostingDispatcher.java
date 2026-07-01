package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalInvoice;
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
 * 销售发票过账派发器。发票 APPROVED 后组装 {@link PostingEvent}(AR_INVOICE) 经 {@link SalPostingExecutor}
 * （独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）调用财务过账引擎。
 *
 * <p>对齐 {@code PurInvoicePostingDispatcher} 的失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}
 * （由 Deferred 兜底扫描重试），不阻塞终态。本类为 Facade 编排层，**不持久化源单据**——源单据 {@code posted}
 * 标志由调用方 BizModel 在主事务内统一持久化（避免跨 REQUIRES_NEW 边界操作被挂起事务的托管实体）。
 */
public class SalInvoicePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SalInvoicePostingDispatcher.class);

    @Inject
    SalPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 发票审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpSalInvoice invoice) {
        PostingEvent event = buildEvent(invoice);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("销售发票过账失败，发票 {} 保持 APPROVED、posted=false：{}", invoice.getCode(), e.getMessage());
            } else {
                LOG.error("销售发票过账异常，发票 {} 保持 APPROVED、posted=false", invoice.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核/作废前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     * 调用方据成功后自行置 posted=false。
     */
    public void reverse(ErpSalInvoice invoice) {
        try {
            executor.reverse(invoice.getCode(), ErpFinBusinessType.AR_INVOICE);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("销售发票红字冲销失败，发票 {}：{}", invoice.getCode(), e.getMessage());
            } else {
                LOG.error("销售发票红字冲销异常，发票 {}", invoice.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpSalInvoice invoice) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AR_INVOICE);
        event.setBillHeadCode(invoice.getCode());
        event.setOrgId(invoice.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(invoice.getOrgId()));
        event.setCurrencyId(invoice.getCurrencyId());
        event.setExchangeRate(invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = invoice.getBusinessDate() != null ? invoice.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SalAcctDocProvider.KEY_TOTAL_AMOUNT, nz(invoice.getTotalAmount()));
        billData.put(SalAcctDocProvider.KEY_TOTAL_TAX_AMOUNT, nz(invoice.getTotalTaxAmount()));
        billData.put(SalAcctDocProvider.KEY_TOTAL_AMOUNT_WITH_TAX, nz(invoice.getTotalAmountWithTax()));
        billData.put("CUSTOMER_ID", invoice.getCustomerId());
        event.setBillData(billData);
        return event;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * 按发票业务组织解析账套（ErpMdAcctSchema.orgId）。发票实体无 acctSchemaId 字段，
     * 过账所需账套由组织推导。取第一个匹配（单组织单账套基线）。
     */
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
}
