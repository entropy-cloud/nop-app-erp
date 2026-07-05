package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.entity.ThreeWayMatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：三单匹配（订单↔入库↔发票）。
 *
 * <p>回链路径：发票行 receiveLineId → 入库行 orderLineId → 订单行 unitPrice（经实时仓库核实为 orderLineId，
 * 非 design 概念名 source_order_line_id）。直接注入 {@link ThreeWayMatcher} 测试严格/非严格两种模式，
 * 另经 GraphQL approve 验证非严格模式放行集成路径。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurThreeWayMatch extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long WAREHOUSE_ID = 3101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ThreeWayMatcher threeWayMatcher;

    /**
     * 严格模式：发票数量超入库 → 拒绝（ERR_INVOICE_QTY_MISMATCH）。
     */
    @Test
    public void testQtyOverReceiveRejectedInStrictMode() {
        long[] chain = seedChain("PI-QTY-STRICT", new BigDecimal("10"), new BigDecimal("10"));
        ErpPurInvoiceLine line = invoiceLineRef(chain[0], new BigDecimal("12"), new BigDecimal("10"));

        RuntimeException caught = runMatcherCatching(line, Boolean.TRUE);
        assertNotNull(caught, "严格模式发票数量超入库应拒绝");
        assertEquals(ErpPurErrors.ERR_INVOICE_QTY_MISMATCH.getErrorCode(), ((NopException) caught).getErrorCode(),
                "应抛 ERR_INVOICE_QTY_MISMATCH");
    }

    /**
     * 严格模式：发票单价超订单单价容差 → 拒绝（ERR_INVOICE_PRICE_MISMATCH）。
     */
    @Test
    public void testPriceOverToleranceRejectedInStrictMode() {
        // 订单单价 10，发票单价 20 → 差异 100% >> 5% 容差
        long[] chain = seedChain("PI-PRICE-STRICT", new BigDecimal("10"), new BigDecimal("10"));
        ErpPurInvoiceLine line = invoiceLineRef(chain[0], new BigDecimal("10"), new BigDecimal("20"));

        RuntimeException caught = runMatcherCatching(line, Boolean.TRUE);
        assertNotNull(caught, "严格模式价格超容差应拒绝");
        assertEquals(ErpPurErrors.ERR_INVOICE_PRICE_MISMATCH.getErrorCode(), ((NopException) caught).getErrorCode(),
                "应抛 ERR_INVOICE_PRICE_MISMATCH");
    }

    /**
     * 非严格模式：超入库 + 超价格容差均放行（仅告警）。
     */
    @Test
    public void testNonStrictModeAllowsOverTolerance() {
        long[] chain = seedChain("PI-NONSTRICT", new BigDecimal("10"), new BigDecimal("10"));
        ErpPurInvoiceLine line = invoiceLineRef(chain[0], new BigDecimal("12"), new BigDecimal("20"));

        RuntimeException caught = runMatcherCatching(line, Boolean.FALSE);
        assertTrue(caught == null, "非严格模式应放行（caught=" + caught + "）");
    }

    /**
     * 无 receiveLineId 的行跳过匹配（支持无订单/直接凭发票场景）。
     */
    @Test
    public void testNoReceiveLineSkipsMatch() {
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setQuantity(new BigDecimal("999"));
        line.setUnitPrice(new BigDecimal("999"));
        RuntimeException caught = runMatcherCatching(line, Boolean.TRUE);
        assertTrue(caught == null, "无回链行应跳过匹配（caught=" + caught + "）");
    }

    /**
     * 数量匹配（发票=入库）+ 价格在容差内 → 严格模式也通过。
     */
    @Test
    public void testMatchWithinTolerancePassesStrictMode() {
        long[] chain = seedChain("PI-OK", new BigDecimal("10"), new BigDecimal("10"));
        ErpPurInvoiceLine line = invoiceLineRef(chain[0], new BigDecimal("10"), new BigDecimal("10.2"));
        RuntimeException caught = runMatcherCatching(line, Boolean.TRUE);
        assertTrue(caught == null, "数量=入库且价格差异 2%<5% 应通过（caught=" + caught + "）");
    }

    /**
     * 集成路径：经 GraphQL approve，非严格模式（默认）下数量超入库放行审核。
     */
    @Test
    public void testApproveNonStrictAllowsQtyOver() {
        long[] chain = seedChain("PI-APPROVE-NS", new BigDecimal("10"), new BigDecimal("10"));
        ErpPurInvoice invoice = invoiceOf("PI-APPROVE-NS");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
            ErpPurInvoiceLine line = new ErpPurInvoiceLine();
            line.setInvoiceId(invoice.getId());
            line.setReceiveLineId(chain[0]);
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("12"));
            line.setUnitPrice(new BigDecimal("10"));
            lineDao.saveEntity(line);
        });

        // 默认非严格模式：数量超入库但 approve 应成功（不 seed 会计期间/科目 → 过账失败吞异常，仍 APPROVED）
        assertEquals(0, submit(invoice.getId()).getStatus(), "submit 应成功");
        assertEquals(0, approve(invoice.getId()).getStatus(), "非严格模式数量超入库应放行审核");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(invoice).getApproveStatus(),
                "审核通过 → APPROVED");
    }

    // ---------- helpers ----------

    private RuntimeException runMatcherCatching(ErpPurInvoiceLine line, Boolean strict) {
        Object[] holder = new Object[1];
        ormTemplate.runInSession(() -> {
            try {
                threeWayMatcher.match("PI-TEST", Collections.singletonList(line), strict);
            } catch (RuntimeException e) {
                holder[0] = e;
            }
        });
        return (RuntimeException) holder[0];
    }

    /**
     * @return [0]=receiveLineId（已回链 orderLineId）
     */
    private long[] seedChain(String tag, BigDecimal receivedQty, BigDecimal orderPrice) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode("PO-" + tag);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(BigDecimal.ONE);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);

        IEntityDao<ErpPurOrderLine> olDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine orderLine = new ErpPurOrderLine();
        orderLine.setOrderId(order.getId());
        orderLine.setLineNo(1);
        orderLine.setMaterialId(MATERIAL_ID);
        orderLine.setUoMId(UOM_ID);
        orderLine.setQuantity(receivedQty);
        orderLine.setUnitPrice(orderPrice);
        orderLine.setAmount(receivedQty.multiply(orderPrice));
        olDao.saveEntity(orderLine);

        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode("PR-" + tag);
        receive.setOrgId(ORG_ID);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(BigDecimal.ONE);
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);

        IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine receiveLine = new ErpPurReceiveLine();
        receiveLine.setReceiveId(receive.getId());
        receiveLine.setOrderLineId(orderLine.getId());
        receiveLine.setLineNo(1);
        receiveLine.setMaterialId(MATERIAL_ID);
        receiveLine.setUoMId(UOM_ID);
        receiveLine.setQuantity(receivedQty);
        rlDao.saveEntity(receiveLine);
        return new long[]{receiveLine.getId()};
    }

    private ErpPurInvoiceLine invoiceLineRef(Long receiveLineId, BigDecimal qty, BigDecimal price) {
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setReceiveLineId(receiveLineId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setQuantity(qty);
        line.setUnitPrice(price);
        return line;
    }

    private ErpPurInvoice invoiceOf(String code) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setPosted(false);
        return invoice;
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reload(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }
}
