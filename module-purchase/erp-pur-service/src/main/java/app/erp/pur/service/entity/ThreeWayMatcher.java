package app.erp.pur.service.entity;

import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 三单匹配器（{@code docs/design/purchase/three-way-match.md}）。发票审核时校验「订单↔入库↔发票」的数量与价格一致性。
 *
 * <p>回链路径（经实时仓库核实，非采信 design 概念名）：发票行 {@code receiveLineId} → {@link ErpPurReceiveLine}
 * → 后者 {@code orderLineId}（{@code _ErpPurReceiveLine.java:33}，**非** design 概念名 {@code source_order_line_id}）
 * → {@link ErpPurOrderLine#getUnitPrice} 用于价格比对。
 *
 * <p>匹配规则：
 * <ul>
 *   <li>数量：发票数量不得超过入库数量（强制项，three-way-match.md §数量差异「发票数量 &gt; 入库数量 → 拒绝」）。</li>
 *   <li>价格：发票单价 vs 订单单价，差异比例超 {@code erp-pur.match-price-tolerance}（默认 5%）视为超容差。</li>
 * </ul>
 * 严格模式（{@code erp-pur.match-strict-mode} 默认 false）：超容差拒绝审核（抛 {@link ErpPurErrors#ERR_INVOICE_QTY_MISMATCH}
 * / {@link ErpPurErrors#ERR_INVOICE_PRICE_MISMATCH}）；非严格模式：记录警告但放行。
 * {@code receiveLineId} 缺失的行跳过匹配（支持无订单/直接凭发票场景）。
 */
public class ThreeWayMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ThreeWayMatcher.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * @param strictOverride 测试注入用覆盖；生产传 null 走配置
     */
    public void match(String invoiceCode, List<ErpPurInvoiceLine> lines, Boolean strictOverride) {
        boolean strict = strictOverride != null ? strictOverride : isStrictMode();
        BigDecimal qtyTolerance = qtyTolerancePercent();
        // qtyTolerance 当前保留为配置项占位（强制不得超入库为基线，容差比例留作运费/杂费明细扩展）。
        if (qtyTolerance == null) {
            qtyTolerance = BigDecimal.ZERO;
        }
        BigDecimal priceTolerance = priceTolerancePercent();
        if (priceTolerance == null) {
            priceTolerance = new BigDecimal("5");
        }

        for (ErpPurInvoiceLine line : lines) {
            if (line.getReceiveLineId() == null) {
                continue;
            }
            ErpPurReceiveLine receiveLine = loadReceiveLine(line.getReceiveLineId());
            if (receiveLine == null) {
                continue;
            }

            // 数量匹配：发票不得超入库（强制项）
            BigDecimal invoiceQty = nz(line.getQuantity());
            BigDecimal receivedQty = nz(receiveLine.getQuantity());
            if (invoiceQty.compareTo(receivedQty) > 0) {
                NopException err = new NopException(ErpPurErrors.ERR_INVOICE_QTY_MISMATCH)
                        .param(ErpPurErrors.ARG_INVOICE_CODE, invoiceCode)
                        .param(ErpPurErrors.ARG_LINE_NO, line.getLineNo())
                        .param(ErpPurErrors.ARG_INVOICE_QTY, invoiceQty)
                        .param(ErpPurErrors.ARG_RECEIVED_QTY, receivedQty);
                if (strict) {
                    throw err;
                }
                LOG.warn("三单匹配数量超入库（非严格模式放行）：发票={} 行={} 发票数量={} 入库数量={}",
                        invoiceCode, line.getLineNo(), invoiceQty, receivedQty);
            }

            // 价格匹配：发票单价 vs 订单单价（订单行经入库行 orderLineId 回链）
            if (receiveLine.getOrderLineId() != null) {
                ErpPurOrderLine orderLine = loadOrderLine(receiveLine.getOrderLineId());
                if (orderLine != null) {
                    BigDecimal invoicePrice = nz(line.getUnitPrice());
                    BigDecimal orderPrice = nz(orderLine.getUnitPrice());
                    if (orderPrice.signum() > 0 && priceDiffPercent(invoicePrice, orderPrice).compareTo(priceTolerance) > 0) {
                        NopException err = new NopException(ErpPurErrors.ERR_INVOICE_PRICE_MISMATCH)
                                .param(ErpPurErrors.ARG_INVOICE_CODE, invoiceCode)
                                .param(ErpPurErrors.ARG_LINE_NO, line.getLineNo())
                                .param(ErpPurErrors.ARG_INVOICE_PRICE, invoicePrice)
                                .param(ErpPurErrors.ARG_ORDER_PRICE, orderPrice);
                        if (strict) {
                            throw err;
                        }
                        LOG.warn("三单匹配价格超容差（非严格模式放行）：发票={} 行={} 发票单价={} 订单单价={} 容差={}%",
                                invoiceCode, line.getLineNo(), invoicePrice, orderPrice, priceTolerance);
                    }
                }
            }
        }
    }

    private BigDecimal priceDiffPercent(BigDecimal invoicePrice, BigDecimal orderPrice) {
        // |invoicePrice - orderPrice| / orderPrice * 100
        BigDecimal diff = invoicePrice.subtract(orderPrice).abs();
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(orderPrice, 4, RoundingMode.HALF_UP);
    }

    private boolean isStrictMode() {
        String raw = readStringConfig(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, "false");
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private BigDecimal qtyTolerancePercent() {
        return readDecimalConfig(ErpPurConstants.CONFIG_MATCH_QTY_TOLERANCE, "5");
    }

    private BigDecimal priceTolerancePercent() {
        return readDecimalConfig(ErpPurConstants.CONFIG_MATCH_PRICE_TOLERANCE, "5");
    }

    private String readStringConfig(String key, String defaultValue) {
        try {
            String value = AppConfig.var(key, defaultValue);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal readDecimalConfig(String key, String defaultValue) {
        String raw = readStringConfig(key, defaultValue);
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warn("容差配置 {} 值非法={}，回退默认 {}", key, raw, defaultValue);
            return new BigDecimal(defaultValue);
        }
    }

    private ErpPurReceiveLine loadReceiveLine(Long id) {
        ormTemplate.flushSession();
        return daoProvider.daoFor(ErpPurReceiveLine.class).getEntityById(id);
    }

    private ErpPurOrderLine loadOrderLine(Long id) {
        ormTemplate.flushSession();
        return daoProvider.daoFor(ErpPurOrderLine.class).getEntityById(id);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
