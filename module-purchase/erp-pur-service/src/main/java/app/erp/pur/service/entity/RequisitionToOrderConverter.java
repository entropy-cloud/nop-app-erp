package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 请购→订单转化组装器：将 APPROVED 的 {@link ErpPurRequisition}+{@link ErpPurRequisitionLine} 组装为
 * {@link ErpPurOrder}(approveStatus=UNSUBMITTED, docStatus=DRAFT) + {@link ErpPurOrderLine}。
 *
 * <p>权威：{@code docs/design/purchase/requisition.md}（请购→转订单）。
 *
 * <p>关键映射（requisition.md + Phase 2 Decision）：
 * <ul>
 *   <li>订单头：{@code code}=新生成订单号；{@code requisitionId}=请购（转化回链键）；{@code supplierId}=请购行一致供应商；
 *       {@code warehouseId}/{@code currencyId}=调用方提供；{@code businessDate}={@code requisition.businessDate}；
 *       {@code orgId}={@code requisition.orgId}；{@code approveStatus}=UNSUBMITTED，{@code docStatus}=DRAFT。</li>
 *   <li>订单行：复制请购行 {@code materialId}/{@code skuId}(请购行无此列,置 null)/{@code uoMId}/{@code quantity}/{@code projectId}；
 *       {@code unitPrice}=调用方按 {@code lineNo} 提供（VARCHAR 存储）；金额族（{@code amount}={@code unitPrice}×{@code quantity}、
 *       {@code taxRate}/{@code taxAmount}/{@code amountWithTax}）由本组件计算，按 VARCHAR 写入（对齐采购域 VARCHAR 金额约定）。</li>
 *   <li>单价解析防护：{@code unitPrice} 空/非法格式抛 {@link ErpPurErrors#ERR_INVALID_UNIT_PRICE}。</li>
 * </ul>
 *
 * <p>订单头金额族（{@code totalAmount}/{@code totalTaxAmount}/{@code totalAmountWithTax}）按行汇总（VARCHAR 写入）。
 */
public class RequisitionToOrderConverter {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public ErpPurOrder build(ErpPurRequisition req, List<ErpPurRequisitionLine> lines,
                             Long supplierId, ConvertToOrderRequest request) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(generateOrderCode(req));
        order.setOrgId(req.getOrgId());
        order.setRequisitionId(req.getId());
        order.setSupplierId(supplierId);
        order.setWarehouseId(request.getWarehouseId());
        order.setBusinessDate(req.getBusinessDate());
        order.setCurrencyId(request.getCurrencyId());
        order.setExchangeRate("1");
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        order.setPosted(false);
        return order;
    }

    public List<ErpPurOrderLine> buildLines(ErpPurOrder order, List<ErpPurRequisitionLine> reqLines,
                                            ConvertToOrderRequest request) {
        List<ErpPurOrderLine> result = new ArrayList<>(reqLines.size());
        int lineNo = 1;
        for (ErpPurRequisitionLine reqLine : reqLines) {
            ErpPurOrderLine line = new ErpPurOrderLine();
            line.setOrderId(order.getId());
            line.setLineNo(lineNo++);
            line.setMaterialId(reqLine.getMaterialId());
            line.setUoMId(reqLine.getUoMId());
            line.setQuantity(reqLine.getQuantity());
            line.setProjectId(reqLine.getProjectId());

            String unitPrice = request.getLineUnitPrices().get(reqLine.getLineNo());
            BigDecimal price = parseUnitPrice(unitPrice, reqLine);
            line.setUnitPrice(price.toPlainString());

            String taxRate = request.getLineTaxRates().get(reqLine.getLineNo());
            BigDecimal rate = parseTaxRate(taxRate);
            line.setTaxRate(rate == null ? null : rate.toPlainString());

            BigDecimal amount = price.multiply(reqLine.getQuantity());
            line.setAmount(amount.toPlainString());

            BigDecimal taxAmount = rate == null ? BigDecimal.ZERO
                    : amount.multiply(rate).divide(HUNDRED, 2, BigDecimal.ROUND_HALF_UP);
            line.setTaxAmount(taxAmount.toPlainString());
            line.setAmountWithTax(amount.add(taxAmount).toPlainString());

            result.add(line);
        }
        return result;
    }

    private BigDecimal parseUnitPrice(String unitPrice, ErpPurRequisitionLine reqLine) {
        if (StringHelper.isBlank(unitPrice)) {
            throw new NopException(ErpPurErrors.ERR_INVALID_UNIT_PRICE)
                    .param(ErpPurErrors.ARG_LINE_TEXT, String.valueOf(reqLine.getLineNo()))
                    .param(ErpPurErrors.ARG_PRICE_TEXT, unitPrice);
        }
        try {
            BigDecimal price = new BigDecimal(unitPrice.trim());
            if (price.signum() < 0) {
                throw new NopException(ErpPurErrors.ERR_INVALID_UNIT_PRICE)
                        .param(ErpPurErrors.ARG_LINE_TEXT, String.valueOf(reqLine.getLineNo()))
                        .param(ErpPurErrors.ARG_PRICE_TEXT, unitPrice);
            }
            return price;
        } catch (NumberFormatException e) {
            throw new NopException(ErpPurErrors.ERR_INVALID_UNIT_PRICE)
                    .param(ErpPurErrors.ARG_LINE_TEXT, String.valueOf(reqLine.getLineNo()))
                    .param(ErpPurErrors.ARG_PRICE_TEXT, unitPrice);
        }
    }

    private BigDecimal parseTaxRate(String taxRate) {
        if (StringHelper.isBlank(taxRate)) {
            return null;
        }
        try {
            return new BigDecimal(taxRate.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String generateOrderCode(ErpPurRequisition req) {
        return "PO-FROM-REQ-" + req.getId();
    }
}
