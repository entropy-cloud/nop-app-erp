package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReceiptLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售退货换货集成测试（RC-R1.51 P1-RC-025，UC-SAL-06 四断言）。
 *
 * <p>覆盖（对齐计划 Phase 5 ①-⑦）：
 * ① returnType 落库 + 默认 RETURN 零回归；
 * ② EXCHANGE 审核库存恢复（断言①：INCOMING 移动单 + 余额增加）；
 * ③ generateExchangeDelivery 生成换货出库单 + 双向关联（断言②④：exchangeDeliveryId ↔ exchangeReturnId）；
 * ④ 价差 Δ>0 补差价开票（DRAFT 发票，totalAmountWithTax=Δ）；
 * ⑤ 价差 Δ<0 退款（复用 ReturnRefundOrchestrator reverse-settlement，反向核销行 + 发票回 UNRECEIVED）；
 * ⑥ 守卫族（returnType!=EXCHANGE / 未审核 / 期间 CLOSED / 源出库未审核 / 重复生成幂等拒绝）；
 * ⑦ 换货出库单经既有出库状态机审核 → OUTGOING 移动单扣库存（断言②运行时闭合）。
 *
 * <p>换货出库单走标准出库状态机（DRAFT→SUBMITTED→APPROVED，Non-Goal 不自动审核）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnExchange extends JunitAutoTestCase {

    static final Long ORG_ID = 6201L;
    static final Long CUSTOMER_ID = 7201L;
    static final Long WAREHOUSE_ID = 8201L;
    static final Long MATERIAL_ID = 9201L;
    static final Long MATERIAL2_ID = 9202L;
    static final Long UOM_ID = 10201L;
    static final Long CURRENCY_ID = 11201L;
    static final Long ACCT_SCHEMA_ID = 12201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(760000L);

    /** ① returnType 落库 + 默认 RETURN 零回归（既有退货路径零行为变化）。 */
    @Test
    public void testReturnTypeDefaultMaterializesReturnFlowUnchanged() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-DEF-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            // 不显式设置 returnType → defaultValue 应物化为 RETURN
            newReturn("RT-XC-DEF-001", returnId, deliveryCtx[0], null);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(ErpSalConstants.RETURN_TYPE_RETURN,
                reload(returnId).getReturnType(), "returnType 默认值物化为 RETURN");
        assertEquals(0, approveReturn(returnId).getStatus(), "RETURN 类型退货审核成功（零回归）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(returnId).getApproveStatus());
        assertNotNull(findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, "RT-XC-DEF-001"),
                "RETURN 审核仍触发 INCOMING 入库移动单（既有路径不变）");
    }

    /** ② EXCHANGE 审核库存恢复（UC-SAL-06 断言①）。 */
    @Test
    public void testExchangeApproveRestoresStock() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-STK-001", new BigDecimal("10"));
        seedStock("SEED-XC-STK-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-STK-001", returnId, deliveryCtx[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "EXCHANGE 退货审核成功");
        ErpInvStockMove move = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, "RT-XC-STK-001");
        assertNotNull(move, "EXCHANGE 审核应生成 INCOMING 入库移动单（断言① 库存恢复）");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, move.getMoveType());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("13")),
                "库存恢复：10(预置) + 3(退货入库) = 13");
    }

    /** ③ generateExchangeDelivery 生成换货出库单 + 双向关联（断言②④）。 */
    @Test
    public void testGenerateExchangeDeliveryCreatesDeliveryAndBidirectionalLink() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-GEN-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GEN-001", returnId, deliveryCtx[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());

        // 换货改发不同货物（L1「换发等值或不同货物」）：物料换为 MATERIAL2，价 8
        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(exchangeLine(MATERIAL2_ID, new BigDecimal("3"), new BigDecimal("8")));
        ApiResponse<?> resp = generateExchangeDelivery(returnId, lines);
        assertEquals(0, resp.getStatus(), "generateExchangeDelivery 应成功: " + resp);

        ErpSalReturn returnOrder = reload(returnId);
        assertNotNull(returnOrder.getExchangeDeliveryId(), "退货单回写 exchangeDeliveryId（断言④ 正向）");
        ErpSalDelivery delivery = daoProvider.daoFor(ErpSalDelivery.class)
                .getEntityById(returnOrder.getExchangeDeliveryId());
        assertNotNull(delivery, "换货出库单存在");
        assertEquals("EX-RT-XC-GEN-001", delivery.getCode(), "换货出库单 code 前缀 EX-");
        assertEquals(ErpSalConstants.DOC_STATUS_DRAFT, delivery.getDocStatus(), "DRAFT 待操作员走标准出库审核");
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, delivery.getApproveStatus());
        assertEquals(returnOrder.getId(), delivery.getExchangeReturnId(), "换货出库单回写 exchangeReturnId（断言④ 反向）");
        assertEquals(CUSTOMER_ID, delivery.getCustomerId(), "客户继承退货单");
        assertEquals(WAREHOUSE_ID, delivery.getWarehouseId(), "仓库继承退货单");
        assertEquals(CURRENCY_ID, delivery.getCurrencyId(), "币种继承退货单");
        assertEquals(0, new BigDecimal("24").compareTo(delivery.getTotalAmountWithTax()),
                "换货出库含税金额 = 3×8 = 24");
        List<ErpSalDeliveryLine> dl = findDeliveryLines(delivery.getId());
        assertEquals(1, dl.size());
        assertEquals(MATERIAL2_ID, dl.get(0).getMaterialId(), "换货行物料为操作员指定的不同货物");
    }

    /** ⑦ 换货出库单经既有出库状态机审核 → OUTGOING 移动单扣库存（断言②运行时闭合）。 */
    @Test
    public void testExchangeDeliveryApproveDeductsStock() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-OUT-001", new BigDecimal("10"));
        seedStock("SEED-XC-OUT-001", new BigDecimal("20"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-OUT-001", returnId, deliveryCtx[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("23")),
                "退货入库后 20+3=23");

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(exchangeLine(MATERIAL_ID, new BigDecimal("3"), new BigDecimal("5")));
        assertEquals(0, generateExchangeDelivery(returnId, lines).getStatus());
        ErpSalReturn returnOrder = reload(returnId);
        ErpSalDelivery delivery = daoProvider.daoFor(ErpSalDelivery.class)
                .getEntityById(returnOrder.getExchangeDeliveryId());

        assertEquals(0, submitDelivery(delivery.getId()).getStatus(), "换货出库提交 → SUBMITTED");
        assertEquals(0, approveDelivery(delivery.getId()).getStatus(), "换货出库审核 → APPROVED（既有出库状态机）");

        ErpInvStockMove move = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode());
        assertNotNull(move, "换货出库审核生成 OUTGOING 移动单（断言② 扣库存）");
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("20")),
                "扣库存：23 − 3 = 20");
    }

    /** ④ 价差 Δ>0：补差价开票（经既有 IErpSalInvoiceBiz 入口，DRAFT 待审核）。 */
    @Test
    public void testPriceDifferencePositiveCreatesInvoice() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-DIFF-P-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-DIFF-P-001", returnId, deliveryCtx[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus()); // 退货含税 15

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(exchangeLine(MATERIAL_ID, new BigDecimal("3"), new BigDecimal("10")));
        assertEquals(0, generateExchangeDelivery(returnId, lines).getStatus()); // 换货含税 30 → Δ=+15

        ErpSalInvoice invoice = findInvoice("EXDIFF-RT-XC-DIFF-P-001");
        assertNotNull(invoice, "Δ>0 应生成补差价发票");
        assertEquals(ErpSalConstants.DOC_STATUS_DRAFT, invoice.getDocStatus(), "补差价发票 DRAFT 待操作员审核");
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, invoice.getApproveStatus());
        assertEquals(0, new BigDecimal("15").compareTo(invoice.getTotalAmountWithTax()),
                "补差价发票含税金额 = Δ = 30 − 15 = 15");
        assertEquals(CUSTOMER_ID, invoice.getCustomerId(), "补差价发票客户继承退货单");
        assertTrue(invoice.getRemark() != null && invoice.getRemark().contains("换货补差价"),
                "补差价发票 remark 记录价差来源");

        ErpSalReturn returnOrder = reload(returnId);
        ErpSalDelivery delivery = daoProvider.daoFor(ErpSalDelivery.class)
                .getEntityById(returnOrder.getExchangeDeliveryId());
        assertTrue(delivery.getRemark() != null && delivery.getRemark().contains("补差价开票"),
                "换货出库单 remark 记录价差方向（审计可追溯）");
    }

    /** ⑤ 价差 Δ<0：退款（复用 ReturnRefundOrchestrator reverse-settlement）。 */
    @Test
    public void testPriceDifferenceNegativeRefunds() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-XC-DIFF-N-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-DIFF-N-001", returnId, deliveryCtx[0], ErpSalConstants.RETURN_TYPE_EXCHANGE,
                    new BigDecimal("30"), new BigDecimal("30"));
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("6"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus()); // 退货含税 30

        // 退货审核后客户再核销（非标准时序构造：orchestrateRefund 客户级退款兜底的可观察断言）
        Long invoiceId = nextId();
        Long receiptId = nextId();
        ormTemplate.runInSession(session -> {
            newApprovedInvoice("SI-XC-DIFF-N-001", invoiceId, new BigDecimal("113"));
            newApprovedReceipt("SR-XC-DIFF-N-001", receiptId, new BigDecimal("113"));
            return null;
        });
        assertEquals(0, settle(receiptId, invoiceId, new BigDecimal("113")).getStatus(), "再核销应成功");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED,
                daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId).getReceivedStatus());

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(exchangeLine(MATERIAL_ID, new BigDecimal("3"), new BigDecimal("5")));
        assertEquals(0, generateExchangeDelivery(returnId, lines).getStatus()); // 换货含税 15 → Δ=−15

        assertTrue(hasNegativeLine(receiptId, invoiceId), "Δ<0 应生成反向（负金额）核销行（退款）");
        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getReceivedAmount()), "退款后发票 receivedAmount 回减为 0");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED, invoice.getReceivedStatus(),
                "退款后发票 receivedStatus 回 UNRECEIVED");

        ErpSalReturn returnOrder = reload(returnId);
        ErpSalDelivery delivery = daoProvider.daoFor(ErpSalDelivery.class)
                .getEntityById(returnOrder.getExchangeDeliveryId());
        assertTrue(delivery.getRemark() != null && delivery.getRemark().contains("退款"),
                "换货出库单 remark 记录价差方向（审计可追溯）");
    }

    /** ⑥ 守卫族 + 幂等拒绝。 */
    @Test
    public void testGuardsAndIdempotency() {
        seedPeriodAndSubjects();

        // (a) returnType=RETURN（非换货）→ ERR_EXCHANGE_RETURN_TYPE_INVALID
        Long[] ctxA = seedApprovedDelivery("SD-XC-GD-001", new BigDecimal("10"));
        Long returnA = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GD-001", returnA, ctxA[0], null);
            newReturnLine(nextId(), returnA, ctxA[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnA).getStatus());
        ApiResponse<?> bad = generateExchangeDelivery(returnA, Collections.emptyList());
        assertEquals(ErpSalErrors.ERR_EXCHANGE_RETURN_TYPE_INVALID.getErrorCode(), bad.getCode(),
                "returnType=RETURN 生成换货应拒绝");

        // (b) 未审核（UNSUBMITTED）→ ERR_RETURN_ILLEGAL_STATUS_TRANSITION
        Long[] ctxB = seedApprovedDelivery("SD-XC-GD-002", new BigDecimal("10"));
        Long returnB = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GD-002", returnB, ctxB[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            ((ErpSalReturn) daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnB))
                    .setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
            newReturnLine(nextId(), returnB, ctxB[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        ApiResponse<?> bad2 = generateExchangeDelivery(returnB, Collections.emptyList());
        assertEquals(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad2.getCode(),
                "未审核生成换货应拒绝");

        // (c) 期间 CLOSED → ERR_RETURN_PERIOD_CLOSED（审核通过后结账，换货生成时点拦截）
        Long[] ctxC = seedApprovedDelivery("SD-XC-GD-003", new BigDecimal("10"));
        Long returnC = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GD-003", returnC, ctxC[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnC, ctxC[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnC).getStatus());
        closePeriod();
        ApiResponse<?> bad3 = generateExchangeDelivery(returnC, Collections.emptyList());
        assertEquals(ErpSalErrors.ERR_RETURN_PERIOD_CLOSED.getErrorCode(), bad3.getCode(),
                "期间已结账生成换货应拒绝");
        reopenPeriod();

        // (d) 源出库未审核（退货审核后源出库被反审核）→ ERR_RETURN_DELIVERY_NOT_APPROVED
        Long[] ctxD = seedApprovedDelivery("SD-XC-GD-004", new BigDecimal("10"));
        Long returnD = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GD-004", returnD, ctxD[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnD, ctxD[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnD).getStatus());
        ormTemplate.runInSession(session -> {
            ErpSalDelivery sourceD = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(ctxD[0]);
            sourceD.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
            daoProvider.daoFor(ErpSalDelivery.class).updateEntity(sourceD);
            return null;
        });
        ApiResponse<?> bad4 = generateExchangeDelivery(returnD, Collections.emptyList());
        assertEquals(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED.getErrorCode(), bad4.getCode(),
                "源出库未审核生成换货应拒绝");

        // (e) 重复生成幂等拒绝 → ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED
        Long[] ctxE = seedApprovedDelivery("SD-XC-GD-005", new BigDecimal("10"));
        Long returnE = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-XC-GD-005", returnE, ctxE[0], ErpSalConstants.RETURN_TYPE_EXCHANGE);
            newReturnLine(nextId(), returnE, ctxE[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnE).getStatus());
        assertEquals(0, generateExchangeDelivery(returnE, Collections.emptyList()).getStatus(),
                "首次生成成功（换货行缺省复制退货行）");
        ApiResponse<?> bad5 = generateExchangeDelivery(returnE, Collections.emptyList());
        assertEquals(ErpSalErrors.ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED.getErrorCode(), bad5.getCode(),
                "重复生成应幂等拒绝");
        assertEquals(1, countExchangeDeliveries(returnE), "同一退货单仅一张换货出库单");
    }

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> generateExchangeDelivery(Long returnId, List<Map<String, Object>> lines) {
        return executeRpc(mutation, "ErpSalReturn__generateExchangeDelivery",
                ApiRequest.build(Map.of("returnId", returnId, "lines", lines)));
    }

    private ApiResponse<?> submitDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> settle(Long receiptId, Long invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("receiptId", receiptId);
        req.put("allocations", Collections.singletonList(alloc));
        return executeRpc(mutation, "ErpSalReceipt__settle", ApiRequest.build(req));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed ----------

    private Long[] seedApprovedDelivery(String deliveryCode, BigDecimal deliveryQty) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDeliveryApproved(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId};
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            seedOrganization();
            seedWarehouseAndCurrency();
            seedMaterialAndUoM();
            return null;
        });
    }

    private void seedMaterialAndUoM() {
        IEntityDao<app.erp.md.dao.entity.ErpMdUoM> udao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdUoM.class);
        app.erp.md.dao.entity.ErpMdUoM uom = new app.erp.md.dao.entity.ErpMdUoM();
        uom.setId(UOM_ID);
        uom.setCode("UOM-" + UOM_ID);
        uom.setName("个");
        udao.saveEntity(uom);
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        for (Long materialId : new Long[]{MATERIAL_ID, MATERIAL2_ID}) {
            ErpMdMaterial material = new ErpMdMaterial();
            material.setId(materialId);
            material.setCode("MAT-" + materialId);
            material.setName("物料" + materialId);
            material.setMaterialType("GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
            dao.saveEntity(material);
        }
    }

    private void seedOrganization() {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization org = new ErpMdOrganization();
        org.setId(ORG_ID);
        org.setCode("ORG-" + ORG_ID);
        org.setName("组织" + ORG_ID);
        org.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        org.setOrgType("COMPANY");
        dao.saveEntity(org);
    }

    private void seedWarehouseAndCurrency() {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse warehouse = new ErpMdWarehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setCode("WH-" + WAREHOUSE_ID);
        warehouse.setName("仓库" + WAREHOUSE_ID);
        warehouse.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(warehouse);
        IEntityDao<ErpMdCurrency> cdao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(CURRENCY_ID);
        currency.setCode("CNY");
        currency.setName("人民币");
        currency.setIsActive(true);
        cdao.saveEntity(currency);
    }

    private void closePeriod() {
        setPeriodStatus(ErpSalConstants.PERIOD_STATUS_CLOSED);
    }

    private void reopenPeriod() {
        setPeriodStatus(ErpSalConstants.PERIOD_STATUS_OPEN);
    }

    private void setPeriodStatus(String status) {
        ormTemplate.runInSession(session -> {
            ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class)
                    .findAllByQuery(new QueryBean()).stream()
                    .filter(p -> "2026-07".equals(p.getCode())).findFirst().orElse(null);
            period.setStatus(status);
            daoProvider.daoFor(ErpFinAccountingPeriod.class).updateEntity(period);
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(CUSTOMER_ID) != null) {
            return;
        }
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(subject);
    }

    private void seedStock(String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "SEED_STOCK");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
    }

    private void newOrderWithId(String code, Long orderId) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setId(orderId);
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        dao.saveEntity(order);
    }

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(qty.multiply(new BigDecimal("5")));
        dao.saveEntity(line);
    }

    private void newDeliveryApproved(String code, Long deliveryId, Long orderId) {
        IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setId(deliveryId);
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setOrderId(orderId);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setPosted(true);
        dao.saveEntity(delivery);
    }

    private void newDeliveryLine(Long lineId, Long deliveryId, Long orderLineId, BigDecimal qty) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setId(lineId);
        line.setDeliveryId(deliveryId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        dao.saveEntity(line);
    }

    private void newReturn(String code, Long returnId, Long deliveryId, String returnType) {
        newReturn(code, returnId, deliveryId, returnType, new BigDecimal("15"), new BigDecimal("15"));
    }

    private void newReturn(String code, Long returnId, Long deliveryId, String returnType,
                           BigDecimal totalAmount, BigDecimal totalAmountWithTax) {
        IEntityDao<ErpSalReturn> dao = daoProvider.daoFor(ErpSalReturn.class);
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setDeliveryId(deliveryId);
        returnOrder.setCustomerId(CUSTOMER_ID);
        returnOrder.setWarehouseId(WAREHOUSE_ID);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(CURRENCY_ID);
        returnOrder.setExchangeRate(new BigDecimal("1"));
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(totalAmount);
        returnOrder.setTotalAmountWithTax(totalAmountWithTax);
        returnOrder.setPosted(false);
        if (returnType != null) {
            returnOrder.setReturnType(returnType);
        }
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long deliveryLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        ErpSalReturnLine line = new ErpSalReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setDeliveryLineId(deliveryLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(qty.multiply(unitPrice));
        line.setReason("质量不合格");
        dao.saveEntity(line);
    }

    private void newApprovedInvoice(String code, Long invoiceId, BigDecimal withTax) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setId(invoiceId);
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        invoice.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(withTax);
        invoice.setTotalTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newApprovedReceipt(String code, Long receiptId, BigDecimal total) {
        IEntityDao<ErpSalReceipt> dao = daoProvider.daoFor(ErpSalReceipt.class);
        ErpSalReceipt receipt = new ErpSalReceipt();
        receipt.setId(receiptId);
        receipt.setCode(code);
        receipt.setOrgId(ORG_ID);
        receipt.setCustomerId(CUSTOMER_ID);
        receipt.setBusinessDate(LocalDate.of(2026, 7, 1));
        receipt.setCurrencyId(CURRENCY_ID);
        receipt.setExchangeRate(BigDecimal.ONE);
        receipt.setTotalAmount(total);
        receipt.setAmountSource(total);
        receipt.setAmountFunctional(total);
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setWrittenOffStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        receipt.setPosted(false);
        dao.saveEntity(receipt);
    }

    // ---------- query helpers ----------

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    private ErpInvStockMove findMove(String relatedBillType, String relatedBillCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", relatedBillType));
        q.addFilter(eq("relatedBillCode", relatedBillCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countExchangeDeliveries(Long returnId) {
        IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("exchangeReturnId", returnId));
        return dao.findAllByQuery(q).size();
    }

    private List<ErpSalDeliveryLine> findDeliveryLines(Long deliveryId) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("deliveryId", deliveryId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpSalInvoice findInvoice(String code) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private boolean hasNegativeLine(Long receiptId, Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiptId", receiptId));
        q.addFilter(eq("invoiceId", invoiceId));
        for (ErpSalReceiptLine l : daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q)) {
            if (l.getAmount() != null && l.getAmount().signum() < 0) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> exchangeLine(Long materialId, BigDecimal qty, BigDecimal unitPrice) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitPrice", unitPrice);
        line.put("taxRate", BigDecimal.ZERO);
        return line;
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
