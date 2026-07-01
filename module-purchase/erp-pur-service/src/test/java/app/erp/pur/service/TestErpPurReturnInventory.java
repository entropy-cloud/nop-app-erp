package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 1 服务层集成测试：采购退货审核触发库存反向出库移动（{@code generateMove}，{@code relatedBillType=PUR_RETURN}）
 * + 库存余额减少 + 幂等 + 反审核内部冲销（反向入库移动恢复库存）。
 *
 * <p>前置：先审核源入库单（生成入库移动 + 库存），再审核退货单。覆盖 purchase→inventory 端到端。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnInventory extends JunitAutoTestCase {

    static final Long ORG_ID = 3201L;
    static final Long SUPPLIER_ID = 4201L;
    static final Long WAREHOUSE_ID = 5201L;
    static final Long MATERIAL_ID = 6201L;
    static final Long UOM_ID = 7201L;
    static final Long CURRENCY_ID = 8201L;
    static final Long ACCT_SCHEMA_ID = 9201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(500000L);

    @Test
    public void testApproveGeneratesOutgoingMoveAndStockDecrease() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-INV-001", new BigDecimal("10"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-INV-001", returnId, receiveId);
            newReturnLine(returnLineId, returnId, receiveLineId, new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpPurReturn approved = daoProvider.daoFor(ErpPurReturn.class).getEntityById(returnId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(), "审核 → APPROVED");

        ErpInvStockMove move = findReturnMove("RT-INV-001");
        assertNotNull(move, "应生成出库移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType(), "出库类型");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");

        ErpInvStockBalance balance = findBalance();
        assertNotNull(balance, "应存在库存余额");
        // 入库 10 - 退货 4 = 6
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("6")),
                "退货后余额 = 入库 10 - 退货 4 = 6");
    }

    @Test
    public void testApproveIdempotent() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-IDM-001", new BigDecimal("10"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-IDM-001", returnId, receiveId);
            newReturnLine(returnLineId, returnId, receiveLineId, new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, approveReturn(returnId).getStatus(), "二次审核幂等空操作");
        assertEquals(1, countReturnMoves("RT-IDM-001"), "幂等：不应产生第二张出库移动单");
    }

    @Test
    public void testReverseApproveRestoresStock() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-REV-001", new BigDecimal("10"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-REV-001", returnId, receiveId);
            newReturnLine(returnLineId, returnId, receiveLineId, new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, new BigDecimal("6").compareTo(findBalance().getTotalQuantity()), "退货后余额=6");

        ErpInvStockMove original = findReturnMove("RT-REV-001");
        assertEquals(0, countReversals(original.getCode()), "反审核前无冲销单");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核应成功");
        ErpPurReturn reversed = daoProvider.daoFor(ErpPurReturn.class).getEntityById(returnId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(), "反审核 → REJECTED");
        assertEquals(1, countReversals(original.getCode()), "应生成 1 张反向冲销移动单（恢复库存）");

        // 冲销后余额恢复到入库量 10
        assertEquals(0, new BigDecimal("10").compareTo(findBalance().getTotalQuantity()),
                "反审核冲销后余额恢复 = 入库 10");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "二次反审核幂等");
        assertEquals(1, countReversals(original.getCode()), "幂等：不产生第二张冲销单");
    }

    // ---------- end-to-end seed: 审核源入库单（生成入库移动 + 库存）----------

    private Long[] seedApprovedReceive(String receiveCode, BigDecimal receiveQty, BigDecimal unitPrice) {
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-" + receiveCode);
            newOrderLine(orderId, orderLineId, 1, receiveQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty, unitPrice);
            return null;
        });
        assertEquals(0, approveReceive(receiveId).getStatus(), "源入库单审核应成功");
        return new Long[]{receiveId, receiveLineId};
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approveReceive(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> approveReturn(Long returnId) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("returnId", returnId)));
    }

    private ApiResponse<?> reverseApproveReturn(Long returnId) {
        return executeRpc(mutation, "ErpPurReturn__reverseApprove", ApiRequest.build(Map.of("returnId", returnId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedAcctSchema();
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
        schema.setNature(10);
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, int status) {
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
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private Long newOrder(String code) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = new ErpPurOrder();
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
        return order.getId();
    }

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
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

    private void newReceive(String code, Long receiveId, Long orderId) {
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

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        dao.saveEntity(line);
    }

    private void newReturn(String code, Long returnId, Long receiveId) {
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
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long receiveLineId, BigDecimal qty, BigDecimal unitPrice) {
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

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- query helpers ----------

    private ErpInvStockMove findReturnMove(String returnCode) {
        return findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnCode);
    }

    private long countReturnMoves(String returnCode) {
        return countMoves(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnCode);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countMoves(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return dao.findAllByQuery(q).size();
    }

    private long countReversals(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpPurConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).size();
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
