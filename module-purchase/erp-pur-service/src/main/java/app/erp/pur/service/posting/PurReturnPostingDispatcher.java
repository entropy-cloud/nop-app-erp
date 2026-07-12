package app.erp.pur.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.pur.dao.entity.ErpPurReturn;
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
 * 采购退货过账派发器。退货 APPROVED 后组装 {@link PostingEvent}(PURCHASE_RETURN) 经 {@link PurPostingExecutor}
 * （独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）调用财务过账引擎，
 * 生成红字冲减凭证（反向 PURCHASE_INPUT：借暂估应付 / 贷存货，{@code posting.md}）。
 *
 * <p>对齐 {@code PurInvoicePostingDispatcher} 的失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}
 * （由 DeferredPostingSweepJob（app.erp.fin.service.job）兜底扫描重试），不阻塞终态。本类为 Facade 编排层，**不持久化源单据**——源单据 {@code posted}
 * 标志由调用方 BizModel 在主事务内统一持久化。
 *
 * <p>billData 契约（供 PurAcctDocProvider + ErpFinArApItemGenerator 消费）：
 * {@code TOTAL_AMOUNT}（不含税）、{@code SUPPLIER_ID}。辅助账生成器据此产 DIRECTION_PAYABLE + 负 openAmount 项
 * （credit memo，使 sumOpen 自然减计 payableBalance）。
 */
public class PurReturnPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PurReturnPostingDispatcher.class);

    @Inject
    PurPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 退货审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpPurReturn returnOrder) {
        PostingEvent event = buildEvent(returnOrder);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("采购退货过账失败，退货单 {} 保持 APPROVED、posted=false：{}",
                        returnOrder.getCode(), e.getMessage());
            } else {
                LOG.error("采购退货过账异常，退货单 {} 保持 APPROVED、posted=false", returnOrder.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核/作废前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     * 调用方据成功后自行置 posted=false。红冲同事务内取消退货辅助账项（cancelOnReverse）。
     */
    public void reverse(ErpPurReturn returnOrder) {
        try {
            executor.reverse(returnOrder.getCode(), ErpFinBusinessType.PURCHASE_RETURN);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("采购退货红字冲销失败，退货单 {}：{}", returnOrder.getCode(), e.getMessage());
            } else {
                LOG.error("采购退货红字冲销异常，退货单 {}", returnOrder.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpPurReturn returnOrder) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PURCHASE_RETURN);
        event.setBillHeadCode(returnOrder.getCode());
        event.setOrgId(returnOrder.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId()));
        event.setCurrencyId(returnOrder.getCurrencyId());
        event.setExchangeRate(returnOrder.getExchangeRate() != null ? returnOrder.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = returnOrder.getBusinessDate() != null
                ? returnOrder.getBusinessDate() : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(PurAcctDocProvider.KEY_TOTAL_AMOUNT, nz(returnOrder.getTotalAmount()));
        billData.put("SUPPLIER_ID", returnOrder.getSupplierId());
        event.setBillData(billData);
        return event;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }
}
