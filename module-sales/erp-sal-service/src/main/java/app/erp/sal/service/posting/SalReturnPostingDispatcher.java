package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.entity.ReturnCostStrategyResolver;
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
 * {@code TOTAL_COST}（退货成本 = Σ 行 quantity×策略 unitCost，与 ReturnStockMoveBuilder 经同一
 * {@link ReturnCostStrategyResolver} 同源消费，对齐 config {@code erp-sal.return-cost-method}）、
 * {@code CUSTOMER_ID}、{@code TOTAL_AMOUNT_WITH_TAX}（退货含税售价，供辅助账 credit memo 用）。
 * 辅助账生成器据此产 DIRECTION_RECEIVABLE + 负 openAmount 项（credit memo，使 sumOpen 自然减计 receivableBalance）。
 */
public class SalReturnPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SalReturnPostingDispatcher.class);

    /**
     * billData 条件标记（P1-RC-024）：true = 未开票且已暂估 → 冲减暂估应收路径；
     * false = 已开票 → 红字替代路径（credit memo 等价，P2-MA2-011 接受）。下游消费方零变更——
     * 两种路径均生成 SALES_RETURN 凭证 + 负向 ArApItem，差异仅在标记。
     */
    static final String KEY_OFFSET_ESTIMATED_RECEIVABLE = "OFFSET_ESTIMATED_RECEIVABLE";

    @Inject
    SalPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 退货审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     *
     * <p>P1-RC-024 条件门控（运营代理）：源出库单 {@code delivery.posted=true}（SALES_OUTPUT 凭证存在）
     * ⇒ 视为暂估应收未清，维持 SALES_RETURN 冲减路径；{@code posted=false}（未暂估）⇒ 跳过事件构造
     * （零凭证 / 零 ArApItem），下游消费方零变更。
     */
    public boolean tryPost(ErpSalReturn returnOrder) {
        if (!isEstimatedReceivableOutstanding(returnOrder)) {
            LOG.debug("销售退货 {} 源出库单未暂估（posted=false），跳过 SALES_RETURN 事件构造",
                    returnOrder.getCode());
            return false;
        }
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
        billData.put(SalAcctDocProvider.KEY_TOTAL_COST, computeTotalCost(returnOrder, lines));
        billData.put(SalAcctDocProvider.KEY_TOTAL_AMOUNT_WITH_TAX, nz(returnOrder.getTotalAmountWithTax()));
        billData.put("CUSTOMER_ID", returnOrder.getCustomerId());
        billData.put(KEY_OFFSET_ESTIMATED_RECEIVABLE, Boolean.TRUE);
        event.setBillData(billData);
        return event;
    }

    /**
     * P1-RC-024 运营代理判定：源出库单 {@code posted=true}（SALES_OUTPUT 凭证存在）⇒ 视为暂估应收未清。
     * 实仓无独立「暂估应收」凭证载体——InvAcctDocProvider SALES_OUTPUT 仅成本侧 6401/1401，
     * 故以 delivery.posted 作为运营近似代理（残留风险：posted 与真实暂估应收状态存在运营近似偏差）。
     */
    private boolean isEstimatedReceivableOutstanding(ErpSalReturn returnOrder) {
        Long deliveryId = returnOrder.getDeliveryId();
        if (deliveryId == null) {
            return false;
        }
        ErpSalDelivery delivery = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
        return delivery != null && Boolean.TRUE.equals(delivery.getPosted());
    }

    /**
     * 退货成本 = Σ 行 quantity × 策略 unitCost。与 {@code ReturnStockMoveBuilder.buildLines} 经同一
     * {@link ReturnCostStrategyResolver} 同源消费（P1-RC-026）——current/agreement 策略下 GL 凭证
     * {@code TOTAL_COST} 与库存移动单 stock ledger 的 totalCost 同源；original 策略行为不变（行 unitPrice）。
     */
    private BigDecimal computeTotalCost(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpSalReturnLine line : lines) {
            BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            BigDecimal unitCost = ReturnCostStrategyResolver.resolveUnitCost(daoProvider, line.getUnitPrice(),
                    line.getMaterialId(), returnOrder.getWarehouseId());
            total = total.add(qty.multiply(unitCost));
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
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }
}
