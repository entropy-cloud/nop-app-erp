package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * P2-CK-pur-004：「CANCELLED 但仍 APPROVED 计入聚合」修正 + cancel/reverseApprove 后订单收货状态重算。
 *
 * <p>修复前三个症状：①作废已审核入库单后订单 receiveStatus 陈旧保持 RECEIVED/PARTIAL（rollup 仅在
 * approve 后触发）；②作废后同订单再入库被超收容差双计误拒（findApprovedReceives 不滤 docStatus）；
 * ③作废退货单后可退量不释放（ReturnQtyValidator 聚合不滤 docStatus）。
 *
 * <p>场景②③在严格/同容差边界取证：作废前拒绝、作废后同量放行，区分度来自 docStatus 过滤。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveCancelRollup extends JunitAutoTestCase {

    static final String ORG_ID = "1802";
    static final String SUPPLIER_ID = "2802";
    static final String WAREHOUSE_ID = "3802";
    static final String MATERIAL_ID = "4802";
    static final String UOM_ID = "5802";
    static final String CURRENCY_ID = "6802";
    static final String ACCT_SCHEMA_ID = "7802";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(200000L);

    // ---------- 症状①：作废已审核入库单后订单收货状态回落 ----------

    @Test
    public void testCancelledReceiveRollsOrderBackToUnreceived() {
        String orderLineId = nextId();
        String receiveId = nextId();
        String receiveLineId = nextId();
        String orderId = seedChain("PO-CR-001", "PR-CR-001", orderLineId, receiveId, receiveLineId,
                new BigDecimal("10"), new BigDecimal("10"));

        assertEquals(0, approve(receiveId).getStatus(), "入库 10/10 审核放行");
        assertEquals(ErpPurConstants.RECEIVE_STATUS_RECEIVED, orderReceiveStatus(orderId),
                "全额入库 → 订单 RECEIVED");

        assertEquals(0, cancel(receiveId).getStatus(), "已审核入库单作废放行");
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED,
                daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId).getDocStatus());
        assertEquals(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED, orderReceiveStatus(orderId),
                "作废后订单收货状态重算回落 UNRECEIVED（修复前陈旧保持 RECEIVED）");
    }

    // ---------- 症状②：作废释放超收容差预算（strict 同量拒绝→放行区分） ----------

    @Test
    public void testCancelledReceiveReleasesToleranceBudget() {
        String orderLineId = nextId();
        String receive1 = nextId();
        String receiveLine1 = nextId();
        String orderId = seedChain("PO-CR-002", "PR-CR-002A", orderLineId, receive1, receiveLine1,
                new BigDecimal("10"), new BigDecimal("8"));

        String receive2 = nextId();
        String receiveLine2 = nextId();
        ormTemplate.runInSession(session -> {
            newReceive("PR-CR-002B", receive2, orderId);
            newReceiveLine(receiveLine2, receive2, orderLineId, new BigDecimal("5"));
            return null;
        });

        withStrictMode(true, () -> {
            assertEquals(0, approve(receive1).getStatus(), "首张 8 ≤ 10.5 放行");
            assertNotEquals(0, approve(receive2).getStatus(),
                    "8+5=13 > 10.5 聚合超收 strict 拒绝（作废前）");

            assertEquals(0, cancel(receive1).getStatus(), "首张作废");
            assertEquals(0, approve(receive2).getStatus(),
                    "作废首张后同量 5 ≤ 10.5 放行（修复前 CANCELLED 仍计入聚合继续误拒）");
        });
    }

    // ---------- 症状③：作废退货单释放可退量 ----------

    @Test
    public void testCancelledReturnReleasesReturnableQty() {
        seedFinance();
        String[] receiveCtx = seedApprovedReceive("PR-CR-003", new BigDecimal("10"), new BigDecimal("5"));
        String receiveId = receiveCtx[0];
        String receiveLineId = receiveCtx[1];

        // 退货单1：直接落库为已审核（fixture 与审计缺陷面一致——作废单 approveStatus 仍 APPROVED）
        String return1 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-CR-003A", return1, receiveId);
            newReturnLine(nextId(), return1, receiveLineId, new BigDecimal("6"), new BigDecimal("5"));
            approveStatusByDao(return1);
            return null;
        });

        // 退货单2：SUBMITTED → 审核被拒（累计已退 6，剩余可退 4 < 6）
        String return2 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-CR-003B", return2, receiveId);
            newReturnLine(nextId(), return2, receiveLineId, new BigDecimal("6"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(return2).getStatus(), "累计 6 > 可退 4 拒绝（作废前）");

        // 退货单1作废（docStatus 翻转，approveStatus 保持 APPROVED——正是聚合缺陷面）
        ormTemplate.runInSession(session -> {
            ErpPurReturn r = daoProvider.daoFor(ErpPurReturn.class).getEntityById(return1);
            r.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
            daoProvider.daoFor(ErpPurReturn.class).saveOrUpdateEntity(r);
            return null;
        });

        // 退货单3：同量 6 放行（可退量已释放）
        String return3 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-CR-003C", return3, receiveId);
            newReturnLine(nextId(), return3, receiveLineId, new BigDecimal("6"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(return3).getStatus(),
                "作废退货单不再占用可退量，6 ≤ 10 放行（修复前继续误拒）");
    }

    // ---------- config helpers ----------

    private void withStrictMode(boolean strict, Runnable body) {
        AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, strict);
        try {
            body.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, false);
        }
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approve(String receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("id", receiveId)));
    }

    private ApiResponse<?> cancel(String receiveId) {
        return executeRpc(mutation, "ErpPurReceive__cancel", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> approveReturn(String id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private String orderReceiveStatus(String orderId) {
        return daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId).getReceiveStatus();
    }

    // ---------- seed helpers ----------

    private String seedChain(String orderCode, String receiveCode, String orderLineId, String receiveId,
                             String receiveLineId, BigDecimal orderQty, BigDecimal receiveQty) {
        seedFinance();
        String orderId = nextId();
        ormTemplate.runInSession(session -> {
            newOrder(orderCode, orderId);
            newOrderLine(orderId, orderLineId, orderQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty);
            return null;
        });
        return orderId;
    }

    private String[] seedApprovedReceive(String receiveCode, BigDecimal receiveQty, BigDecimal unitPrice) {
        String orderLineId = nextId();
        String receiveId = nextId();
        String receiveLineId = nextId();
        String orderId = nextId();
        ormTemplate.runInSession(session -> {
            newOrder("PO-" + receiveCode, orderId);
            newOrderLine(orderId, orderLineId, receiveQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty);
            return null;
        });
        assertEquals(0, approve(receiveId).getStatus(), "源入库单审核应成功");
        return new String[]{receiveId, receiveLineId};
    }

    private void approveStatusByDao(String returnId) {
        ErpPurReturn r = daoProvider.daoFor(ErpPurReturn.class).getEntityById(returnId);
        r.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        daoProvider.daoFor(ErpPurReturn.class).saveOrUpdateEntity(r);
    }

    private void seedFinance() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            seedActiveSupplier();
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
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void newOrder(String code, String orderId) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = new ErpPurOrder();
        order.setId(orderId);
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        dao.saveEntity(order);
    }

    private void newOrderLine(String orderId, String lineId, BigDecimal qty) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(qty.multiply(new BigDecimal("5")));
        dao.saveEntity(line);
    }

    private void newReceive(String code, String receiveId, String orderId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setId(receiveId);
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setOrderId(orderId);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(String lineId, String receiveId, String orderLineId, BigDecimal qty) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        dao.saveEntity(line);
    }

    private void newReturn(String code, String returnId, String receiveId) {
        IEntityDao<ErpPurReturn> dao = daoProvider.daoFor(ErpPurReturn.class);
        ErpPurReturn returnOrder = new ErpPurReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setReceiveId(receiveId);
        returnOrder.setSupplierId(SUPPLIER_ID);
        returnOrder.setWarehouseId(WAREHOUSE_ID);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(CURRENCY_ID);
        returnOrder.setExchangeRate(new BigDecimal("1"));
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(new BigDecimal("15"));
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(String lineId, String returnId, String receiveLineId, BigDecimal qty,
                               BigDecimal unitPrice) {
        IEntityDao<ErpPurReturnLine> dao = daoProvider.daoFor(ErpPurReturnLine.class);
        ErpPurReturnLine line = new ErpPurReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setReceiveLineId(receiveLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(qty.multiply(unitPrice));
        line.setReason("质量不合格");
        dao.saveEntity(line);
    }

    private String nextId() {
        return String.valueOf(idSeq.incrementAndGet());
    }
}
