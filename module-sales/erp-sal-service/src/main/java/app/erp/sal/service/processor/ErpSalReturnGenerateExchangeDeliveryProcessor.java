package app.erp.sal.service.processor;

import app.erp.sal.biz.ErpSalExchangeDeliveryLine;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.ReturnRefundOrchestrator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 换货出库单生成 per-mutation Processor（RC-R1.51 P1-RC-025，UC-SAL-06 断言②④，D1 选项 A）。
 * {@code IErpSalReturnBiz.generateExchangeDelivery} 经 BizModel Facade 路由到本类。
 *
 * <p>流程：守卫链（returnType==EXCHANGE + 已 APPROVED + 源出库已审核 + 期间 OPEN + 发票未核销——
 * 复用 {@link ErpSalReturnProcessor} R1.19 守卫族 helper，同包 protected 可调）→ 幂等拒绝
 * （exchangeDeliveryId 非空抛 {@code ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED}）→ 换货行解析
 * （入参缺省复制退货行）→ 复制退货头（customer/warehouse/currency/businessDate）新建 ErpSalDelivery
 * （DRAFT + 行 [material/sku/uoM/quantity/unitPrice/taxRate] + exchangeReturnId 反向列）经
 * {@link IErpSalDeliveryBiz#save}（跨实体经 I*Biz 管道）→ 同事务回写 ErpSalReturn.exchangeDeliveryId
 * （D2 双向关联双写）→ 价差分支（D3 头级口径 Δ = 换货出库 totalAmountWithTax − 退货 totalAmountWithTax：
 * Δ>0 经 {@link IErpSalInvoiceBiz#save} 建 DRAFT 补差价发票 / Δ<0 经 {@link ReturnRefundOrchestrator}
 * 既有 reverse-settlement 能力退款 / Δ=0 无动作；价差金额 + 方向记录换货出库单 remark 审计可追溯）。
 *
 * <p>事务边界：@BizMutation 单事务包裹（含 delivery/invoice save 管道与回写）；各 step 为
 * {@code protected} 方法供下游经 Delta beans.xml 同名 bean id 覆盖。
 *
 * <p>跨实体：出库单/发票创建经 I*Biz 注入（service-layer 跨实体访问规则）；退货行读取经
 * {@link IDaoProvider} 同域查询（对齐 ReturnRefundOrchestrator 同模块 daoFor 先例）。
 */
public class ErpSalReturnGenerateExchangeDeliveryProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSalDeliveryBiz deliveryBiz;

    @Inject
    IErpSalInvoiceBiz invoiceBiz;

    @Inject
    ErpSalReturnProcessor returnProcessor;

    @Inject
    ReturnRefundOrchestrator refundOrchestrator;

    public ErpSalReturn generateExchangeDelivery(Long returnId,
                                                 List<ErpSalExchangeDeliveryLine> lines,
                                                 IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        assertReturnTypeExchange(returnOrder);
        assertApproved(returnOrder, context);
        assertExchangeNotGenerated(returnOrder);
        assertSourceDeliveryApproved(returnOrder, context);
        assertPeriodOpen(returnOrder, context);
        assertInvoiceNotSettled(returnOrder, context);

        List<ErpSalExchangeDeliveryLine> effectiveLines = resolveLines(returnOrder, lines);
        requireLinesNonEmpty(effectiveLines, returnOrder);

        ErpSalDelivery delivery = createExchangeDelivery(returnOrder, effectiveLines, context);
        returnOrder = linkExchangeDelivery(returnOrder, delivery);
        processPriceDifference(returnOrder, delivery, effectiveLines, context);
        return returnOrder;
    }

    // ---------- steps（protected，下游可经 Delta 覆盖） ----------

    protected ErpSalReturn requireReturn(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = returnDao().getEntityById(returnId);
        if (returnOrder == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RETURN_ID, returnId);
        }
        return returnOrder;
    }

    protected void assertReturnTypeExchange(ErpSalReturn returnOrder) {
        if (!Objects.equals(returnOrder.getReturnType(), ErpSalConstants.RETURN_TYPE_EXCHANGE)) {
            throw new NopException(ErpSalErrors.ERR_EXCHANGE_RETURN_TYPE_INVALID)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, returnOrder.getReturnType());
        }
    }

    protected void assertApproved(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw returnProcessor.illegalTransition(returnOrder, status, ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void assertExchangeNotGenerated(ErpSalReturn returnOrder) {
        if (returnOrder.getExchangeDeliveryId() != null) {
            throw new NopException(ErpSalErrors.ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                    .param(ErpSalErrors.ARG_DELIVERY_ID, returnOrder.getExchangeDeliveryId());
        }
    }

    protected void assertSourceDeliveryApproved(ErpSalReturn returnOrder, IServiceContext context) {
        returnProcessor.requireSourceDeliveryApproved(returnOrder, context);
    }

    protected void assertPeriodOpen(ErpSalReturn returnOrder, IServiceContext context) {
        returnProcessor.requirePeriodOpen(returnOrder, context);
    }

    protected void assertInvoiceNotSettled(ErpSalReturn returnOrder, IServiceContext context) {
        List<ErpSalReturnLine> returnLines = loadReturnLines(returnOrder.getId());
        returnProcessor.validateInvoiceNotSettled(returnOrder, returnLines, context);
    }

    /**
     * 换货行解析：入参缺省复制退货行（D1 选项 A「默认复制退货行」）。
     */
    protected List<ErpSalExchangeDeliveryLine> resolveLines(ErpSalReturn returnOrder,
                                                            List<ErpSalExchangeDeliveryLine> lines) {
        if (lines != null && !lines.isEmpty()) {
            return lines;
        }
        List<ErpSalExchangeDeliveryLine> result = new ArrayList<>();
        for (ErpSalReturnLine line : loadReturnLines(returnOrder.getId())) {
            ErpSalExchangeDeliveryLine copy = new ErpSalExchangeDeliveryLine();
            copy.setMaterialId(line.getMaterialId());
            copy.setSkuId(line.getSkuId());
            copy.setUoMId(line.getUoMId());
            copy.setQuantity(line.getQuantity());
            copy.setUnitPrice(line.getUnitPrice());
            copy.setTaxRate(line.getTaxRate());
            result.add(copy);
        }
        return result;
    }

    protected void requireLinesNonEmpty(List<ErpSalExchangeDeliveryLine> lines, ErpSalReturn returnOrder) {
        if (lines.isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    /**
     * 复制退货头（customer/warehouse/currency/businessDate）新建换货出库单（DRAFT）。
     * 行金额 = quantity × unitPrice（scale 4 HALF_UP），税额 = amount × taxRate / 100（无税率 0）；
     * 头 totalAmount/totalTaxAmount/totalAmountWithTax 按行聚合。行入参缺省（null）时按 0 聚合。
     * 换货出库单经既有出库状态机流转（DRAFT→SUBMITTED→APPROVED，Non-Goal 不自动审核），
     * 审核后经既有 DeliveryStockMoveBuilder 生成 OUTGOING 移动单扣库存（断言②运行时成立）。
     */
    protected ErpSalDelivery createExchangeDelivery(ErpSalReturn returnOrder,
                                                    List<ErpSalExchangeDeliveryLine> lines,
                                                    IServiceContext context) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("code", exchangeDeliveryCode(returnOrder));
        header.put("orgId", returnOrder.getOrgId());
        header.put("customerId", returnOrder.getCustomerId());
        header.put("warehouseId", returnOrder.getWarehouseId());
        header.put("businessDate", returnOrder.getBusinessDate());
        header.put("currencyId", returnOrder.getCurrencyId());
        header.put("exchangeRate", returnOrder.getExchangeRate() != null ? returnOrder.getExchangeRate() : BigDecimal.ONE);
        header.put("docStatus", ErpSalConstants.DOC_STATUS_DRAFT);
        header.put("approveStatus", ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        header.put("posted", false);
        // D2 双向关联反向列：换货出库单 → 来源退货单
        header.put("exchangeReturnId", returnOrder.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        List<Map<String, Object>> lineMaps = new ArrayList<>();
        int lineNo = 1;
        for (ErpSalExchangeDeliveryLine line : lines) {
            Map<String, Object> lineMap = new LinkedHashMap<>();
            lineMap.put("lineNo", lineNo++);
            lineMap.put("materialId", line.getMaterialId());
            lineMap.put("skuId", line.getSkuId());
            lineMap.put("uoMId", line.getUoMId());
            BigDecimal qty = nz(line.getQuantity());
            BigDecimal unitPrice = nz(line.getUnitPrice());
            BigDecimal amount = qty.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP);
            BigDecimal taxRate = nz(line.getTaxRate());
            BigDecimal taxAmount = amount.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            lineMap.put("quantity", qty);
            lineMap.put("unitPrice", unitPrice);
            lineMap.put("taxRate", taxRate);
            lineMap.put("amount", amount);
            lineMap.put("taxAmount", taxAmount);
            totalAmount = totalAmount.add(amount);
            totalTaxAmount = totalTaxAmount.add(taxAmount);
            lineMaps.add(lineMap);
        }
        header.put("totalAmount", totalAmount);
        header.put("totalTaxAmount", totalTaxAmount);
        header.put("totalAmountWithTax", totalAmount.add(totalTaxAmount));
        header.put("lines", lineMaps);
        return deliveryBiz.save(header, context);
    }

    /**
     * D2 同事务双写：回写 ErpSalReturn.exchangeDeliveryId（反向列 exchangeReturnId 已在建单时写入）。
     */
    protected ErpSalReturn linkExchangeDelivery(ErpSalReturn returnOrder, ErpSalDelivery delivery) {
        ErpSalReturn reloaded = returnDao().getEntityById(returnOrder.getId());
        reloaded.setExchangeDeliveryId(delivery.getId());
        returnDao().updateEntity(reloaded);
        return reloaded;
    }

    /**
     * 价差分支（D3 头级口径）：Δ = 换货出库 totalAmountWithTax − 退货 totalAmountWithTax。
     * Δ>0 补差价开票（经 {@link IErpSalInvoiceBiz#save} 既有入口，DRAFT 待操作员审核——复用既有发票
     * 创建/过账链，不新增过账 Provider）；Δ<0 退款（复用 {@link ReturnRefundOrchestrator} 既有
     * reverse-settlement 能力；标准场景退货审核已先行反转客户已核销发票，本分支为退款兜底 +
     * remark 审计记录）；Δ=0 无动作。价差金额 + 方向记录换货出库单 remark。
     */
    protected void processPriceDifference(ErpSalReturn returnOrder, ErpSalDelivery delivery,
                                          List<ErpSalExchangeDeliveryLine> lines,
                                          IServiceContext context) {
        BigDecimal delta = nz(delivery.getTotalAmountWithTax())
                .subtract(nz(returnOrder.getTotalAmountWithTax()));
        if (delta.signum() == 0) {
            return;
        }
        if (delta.signum() > 0) {
            createDifferenceInvoice(returnOrder, delivery, delta, lines, context);
        } else {
            refundOrchestrator.orchestrateRefund(returnOrder);
        }
        // delivery 为 deliveryBiz.save 返回的会话托管实体（SAVING），同事务内直接改字段，
        // @BizMutation 提交时 ORM 会话自动 flush（SAVING 实体不可再调 updateEntity——nop.err.orm.dao.update-entity-not-managed）
        String direction = delta.signum() > 0 ? "补差价开票" : "退款";
        String prefix = delivery.getRemark() == null || delivery.getRemark().trim().isEmpty()
                ? "" : delivery.getRemark() + "；";
        delivery.setRemark(prefix + "换货价差 " + delta.toPlainString() + "（" + direction
                + "，退货单 " + returnOrder.getCode() + "）");
    }

    /**
     * 补差价发票创建（Δ>0）：经 {@link IErpSalInvoiceBiz#save} 既有入口（CrudBizModel 管道）。
     * 单行 quantity=1、unitPrice=Δ（价差含税口径，taxRate=0 → amount=Δ/taxAmount=0/
     * totalAmount=Δ/totalAmountWithTax=Δ），DRAFT + UNSUBMITTED 待操作员经既有发票审核流提交过账。
     */
    protected void createDifferenceInvoice(ErpSalReturn returnOrder, ErpSalDelivery delivery,
                                           BigDecimal delta, List<ErpSalExchangeDeliveryLine> lines,
                                           IServiceContext context) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("code", differenceInvoiceCode(returnOrder));
        header.put("orgId", returnOrder.getOrgId());
        header.put("customerId", returnOrder.getCustomerId());
        header.put("businessDate", returnOrder.getBusinessDate());
        header.put("currencyId", returnOrder.getCurrencyId());
        header.put("exchangeRate", returnOrder.getExchangeRate() != null ? returnOrder.getExchangeRate() : BigDecimal.ONE);
        header.put("totalAmount", delta);
        header.put("totalTaxAmount", BigDecimal.ZERO);
        header.put("totalAmountWithTax", delta);
        header.put("receivedAmount", BigDecimal.ZERO);
        header.put("docStatus", ErpSalConstants.DOC_STATUS_DRAFT);
        header.put("approveStatus", ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        header.put("receivedStatus", ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        header.put("posted", false);
        header.put("remark", "换货补差价（退货单 " + returnOrder.getCode()
                + " → 换货出库单 " + delivery.getCode() + "）");

        ErpSalExchangeDeliveryLine firstLine = lines == null || lines.isEmpty() ? null : lines.get(0);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("lineNo", 1);
        line.put("materialId", firstLine == null ? null : firstLine.getMaterialId());
        line.put("uoMId", firstLine == null ? null : firstLine.getUoMId());
        line.put("quantity", BigDecimal.ONE);
        line.put("unitPrice", delta);
        line.put("taxRate", BigDecimal.ZERO);
        line.put("amount", delta);
        line.put("taxAmount", BigDecimal.ZERO);
        header.put("lines", java.util.List.of(line));
        invoiceBiz.save(header, context);
    }

    // ---------- helpers ----------

    private String exchangeDeliveryCode(ErpSalReturn returnOrder) {
        String code = "EX-" + returnOrder.getCode();
        if (code.length() <= 50) {
            return code;
        }
        return "EXR" + returnOrder.getId();
    }

    private String differenceInvoiceCode(ErpSalReturn returnOrder) {
        String code = "EXDIFF-" + returnOrder.getCode();
        if (code.length() <= 50) {
            return code;
        }
        return "EXDR" + returnOrder.getId();
    }

    private List<ErpSalReturnLine> loadReturnLines(Long returnId) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected IEntityDao<ErpSalReturn> returnDao() {
        return daoProvider.daoFor(ErpSalReturn.class);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
