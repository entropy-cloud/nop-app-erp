package app.erp.sal.service.entity;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.time.CoreMetrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 报价→订单域内转化组装器。将 {@link ErpSalQuotation}+{@link ErpSalQuotationLine} 组装为
 * {@link ErpSalOrder}(UNSUBMITTED/DRAFT) + {@link ErpSalOrderLine}，供 {@link ErpSalQuotationBizModel#convertToOrder} 调用。
 *
 * <p>权威：{@code docs/design/sales/quotation.md}（ACCEPTED → 转订单，报价行转订单行）。
 *
 * <p>关键映射（Phase 2 Decision）：
 * <ul>
 *   <li>头：回链 {@code order.quotationId}；复制 {@code customerId}/{@code currencyId}/{@code exchangeRate} + 金额族
 *       ({@code totalAmount}/{@code totalTaxAmount}/{@code totalAmountWithTax})；{@code businessDate}={@link CoreMetrics#currentDate()}；
 *       {@code approveStatus}=UNSUBMITTED、{@code docStatus}=DRAFT、{@code deliveryStatus}=UNDELIVERED、{@code posted}=false。</li>
 *   <li>行：复制 {@code materialId}/{@code uoMId}/{@code quantity}/{@code unitPrice}/{@code taxRate}/{@code taxAmount}/
 *       {@code amount}/{@code amountWithTax}（DECIMAL 直读直写）；**{@code skuId} 不复制**——{@link ErpSalQuotationLine}
 *       无该列（订单行 {@code skuId} 留空）。</li>
 * </ul>
 *
 * <p>不在此处持久化（持久化 + 回链 {@code isAccepted} 由 {@code ErpSalQuotationBizModel} 统一在事务内完成）。
 */
public class QuotationToOrderConverter {

    /**
     * 组装转化订单（含行）。{@code order.code} 留空由调用方/生成规则填充。
     */
    public ErpSalOrder build(ErpSalQuotation quotation, List<ErpSalQuotationLine> quotationLines) {
        ErpSalOrder order = new ErpSalOrder();
        order.setQuotationId(quotation.getId());
        order.setOrgId(quotation.getOrgId());
        order.setCustomerId(quotation.getCustomerId());
        order.setBusinessDate(CoreMetrics.currentDate());
        order.setCurrencyId(quotation.getCurrencyId());
        order.setExchangeRate(quotation.getExchangeRate());
        order.setTotalAmount(quotation.getTotalAmount());
        order.setTotalTaxAmount(quotation.getTotalTaxAmount());
        order.setTotalAmountWithTax(quotation.getTotalAmountWithTax());
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        order.getLines().addAll(buildLines(quotationLines));
        return order;
    }

    private List<ErpSalOrderLine> buildLines(List<ErpSalQuotationLine> quotationLines) {
        List<ErpSalOrderLine> result = new ArrayList<>(quotationLines.size());
        int lineNo = 1;
        for (ErpSalQuotationLine ql : quotationLines) {
            ErpSalOrderLine line = new ErpSalOrderLine();
            line.setLineNo(ql.getLineNo() == null ? lineNo : ql.getLineNo());
            line.setMaterialId(ql.getMaterialId());
            // ErpSalQuotationLine 无 skuId 列 → 订单行 skuId 留空（不复制）。
            line.setUoMId(ql.getUoMId());
            line.setQuantity(ql.getQuantity());
            line.setUnitPrice(ql.getUnitPrice());
            line.setTaxRate(ql.getTaxRate());
            line.setTaxAmount(ql.getTaxAmount());
            line.setAmount(ql.getAmount());
            line.setAmountWithTax(ql.getAmountWithTax());
            result.add(line);
            lineNo++;
        }
        return result;
    }
}
