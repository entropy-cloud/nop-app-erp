package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：入库审核触发库存移动（{@code generateMove}）+ posted 接线 + 收货状态回写 + 反向冲销。
 *
 * <p>覆盖 purchase→inventory→finance 三域经 {@code generateMove} 端到端。审核/反审核经 {@link IGraphQLEngine}
 * 调 {@code ErpPurReceive__approve/reverseApprove}，引擎建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）；
 * 审核→入库移动单 DONE、库存余额增加、存货估值凭证落地（{@code billHeadCode}=移动单 code）、{@code receive.posted=true}；
 * 幂等重审、收货状态回写、反审核内部冲销 + 余额冲回 + 幂等防双冲销。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveStockMove extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long SUPPLIER_ID = 2201L;
    static final Long WAREHOUSE_ID = 3201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7201L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveGeneratesIncomingMoveAndPosting() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-POST-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newReceive("PR-POST-001", receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });

        ApiResponse<?> resp = approve(receiveId);
        assertEquals(0, resp.getStatus(), "审核应成功");

        ErpPurReceive approved = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(), "审核 → APPROVED");
        assertEquals(true, approved.getPosted(), "入库移动单 DONE + 过账成功 → posted=true");
        assertEquals(ErpPurConstants.RECEIVE_STATUS_RECEIVED, approved.getReceiveStatus(), "本单收货=已收清");

        ErpInvStockMove move = findMove("PR-POST-001");
        assertNotNull(move, "应生成入库移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, move.getMoveType(), "入库类型");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");
        assertEquals(true, move.getPosted(), "移动单存货过账成功 posted=true");

        ErpInvStockBalance balance = findBalance();
        assertNotNull(balance, "应建立库存余额");
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")),
                "余额 total = 实收数量 10");

        ErpFinVoucherBillR link = findVoucherLink(move.getCode());
        assertNotNull(link, "应生成业财回链（billCode=移动单 code）");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("50")) == 0,
                "借方合计=存货 10×5=50");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("50")) == 0,
                "贷方合计=暂估应付 50");
    }

    @Test
    public void testApproveIdempotent() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-IDEM-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newReceive("PR-IDEM-001", receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approve(receiveId).getStatus());
        assertEquals(0, approve(receiveId).getStatus()); // 二次审核幂等空操作

        assertEquals(1, countMoves("PR-IDEM-001"), "幂等：不应产生第二张入库移动单");
    }

    @Test
    public void testReceiveStatusRollupToOrder() {
        seedPeriodAndSubjects();
        Long orderLine1 = nextId();
        Long orderLine2 = nextId();
        Long receive1 = nextId();
        Long receiveLine1 = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-ROLL-001");
            newOrderLine(orderId, orderLine1, 1, new BigDecimal("10"));
            newOrderLine(orderId, orderLine2, 2, new BigDecimal("10"));
            newReceive("PR-ROLL-001", receive1, orderId);
            newReceiveLine(receiveLine1, receive1, orderLine1, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approve(receive1).getStatus());
        ErpPurReceive approved = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receive1);
        assertEquals(ErpPurConstants.RECEIVE_STATUS_RECEIVED, approved.getReceiveStatus(), "本单已收清");

        ErpPurOrder order = findOrder("PO-ROLL-001");
        assertEquals(ErpPurConstants.RECEIVE_STATUS_PARTIAL, order.getReceiveStatus(),
                "订单仅 1/2 行收清 → PARTIAL");

        Long receive2 = nextId();
        Long receiveLine2 = nextId();
        ormTemplate.runInSession(session -> {
            newReceive("PR-ROLL-002", receive2, order.getId());
            newReceiveLine(receiveLine2, receive2, orderLine2, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approve(receive2).getStatus());

        assertEquals(ErpPurConstants.RECEIVE_STATUS_RECEIVED, findOrder("PO-ROLL-001").getReceiveStatus(),
                "两行均收清 → 订单 RECEIVED");
    }

    @Test
    public void testReverseApproveInternallyReversesMove() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-REV-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newReceive("PR-REV-001", receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approve(receiveId).getStatus());
        ErpInvStockMove original = findMove("PR-REV-001");
        assertNotNull(original);
        assertEquals(0, countReversals(original.getCode()), "反审核前无冲销单");

        assertEquals(0, reverseApprove(receiveId).getStatus());
        ErpPurReceive reversed = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核 → REJECTED（保留曾审核语义）");
        assertEquals(1, countReversals(original.getCode()), "应内部生成 1 张反向冲销移动单");

        ErpInvStockMove reversal = findReversal(original.getCode());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, reversal.getDocStatus(), "冲销单自动 DONE");
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, reversal.getMoveType(), "入库的反向=出库");

        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(BigDecimal.ZERO),
                "冲销后余额归零");

        ErpFinVoucherBillR reversalLink = findVoucherLink(reversal.getCode());
        assertEquals(Boolean.TRUE.equals(reversal.getPosted()), reversalLink != null,
                "冲销单 posted 与红字凭证回链一致");

        assertEquals(0, reverseApprove(receiveId).getStatus());
        assertEquals(1, countReversals(original.getCode()), "二次反审核幂等，不产生第二张冲销单");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approve(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> reverseApprove(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__reverseApprove", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
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

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- query helpers ----------

    private ErpPurOrder findOrder(String code) {
        return daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(new QueryBean()).stream()
                .filter(o -> code.equals(o.getCode())).findFirst().orElse(null);
    }

    private ErpInvStockMove findMove(String receiveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE));
        q.addFilter(eq("relatedBillCode", receiveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countMoves(String receiveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE));
        q.addFilter(eq("relatedBillCode", receiveCode));
        return dao.findAllByQuery(q).size();
    }

    private ErpInvStockMove findReversal(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpPurConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countReversals(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpPurConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinVoucherBillR findVoucherLink(String moveCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> moveCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
