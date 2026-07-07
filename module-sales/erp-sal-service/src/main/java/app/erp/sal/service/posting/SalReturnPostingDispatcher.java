package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
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

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售退货过账派发器。退货 APPROVED 后组装 {@link PostingEvent}(SALES_RETURN) 经 {@link SalPostingExecutor}
 * （独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）调用财务过账引擎，
 * 生成反向 SALES_OUTPUT 凭证（借库存商品 / 贷主营业务成本，{@code posting.md}）。
 *
 * <p>对齐 {@link SalInvoicePostingDispatcher} 的失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}
 * （由 DeferredPostingSweepJob（app.erp.fin.service.job）兜底扫描重试），不阻塞终态。本类为 Facade 编排层，**不持久化源单据**——源单据 {@code posted}
 * 标志由调用方 BizModel 在主事务内统一持久化。
 *
 * <p>billData 契约（供 SalAcctDocProvider + ErpFinArApItemGenerator 消费）：
 * {@code TOTAL_COST}（退货成本 = Σ 行 quantity×unitPrice，对齐 ReturnStockMoveBuilder 透传的 unitCost）、
 * {@code CUSTOMER_ID}、{@code TOTAL_AMOUNT_WITH_TAX}（退货含税售价，供辅助账 credit memo 用）。
 * 辅助账生成器据此产 DIRECTION_RECEIVABLE + 负 openAmount 项（credit memo，使 sumOpen 自然减计 receivableBalance）。
 */
public class SalReturnPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SalReturnPostingDispatcher.class);

    @Inject
    SalPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 退货审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpSalReturn returnOrder) {
        PostingEvent event = buildEvent(returnOrder);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("销售退货过账失败，退货单 {} 保持 APPROVED、posted=false：{}",
                        returnOrder.getCode(), e.getMessage());
            } else {
                LOG.error("销售退货过账异常，退货单 {} 保持 APPROVED、posted=false", returnOrder.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核/作废前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     * 调用方据成功后自行置 posted=false。红冲同事务内取消退货辅助账项（cancelOnReverse）。
     */
    public void reverse(ErpSalReturn returnOrder) {
        try {
            executor.reverse(returnOrder.getCode(), ErpFinBusinessType.SALES_RETURN);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("销售退货红字冲销失败，退货单 {}：{}", returnOrder.getCode(), e.getMessage());
            } else {
                LOG.error("销售退货红字冲销异常，退货单 {}", returnOrder.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpSalReturn returnOrder) {
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.SALES_RETURN);
        event.setBillHeadCode(returnOrder.getCode());
        event.setOrgId(returnOrder.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId()));
        event.setCurrencyId(returnOrder.getCurrencyId());
        event.setExchangeRate(returnOrder.getExchangeRate() != null ? returnOrder.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = returnOrder.getBusinessDate() != null
                ? returnOrder.getBusinessDate() : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SalAcctDocProvider.KEY_TOTAL_COST, computeTotalCost(lines));
        billData.put(SalAcctDocProvider.KEY_TOTAL_AMOUNT_WITH_TAX, nz(returnOrder.getTotalAmountWithTax()));
        billData.put("CUSTOMER_ID", returnOrder.getCustomerId());
        event.setBillData(billData);
        return event;
    }

    /**
     * 退货成本 = Σ 行 quantity × unitPrice（对齐 {@code ReturnStockMoveBuilder} 透传 unitCost = unitPrice 的口径，
     * 与库存移动单 stock ledger 的 totalCost 同源）。
     */
    private BigDecimal computeTotalCost(List<ErpSalReturnLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpSalReturnLine line : lines) {
            BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            BigDecimal price = line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice();
            total = total.add(qty.multiply(price));
        }
        return total;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private List<ErpSalReturnLine> loadLines(Long returnId) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return dao.findAllByQuery(q);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }
}
