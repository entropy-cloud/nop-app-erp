package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
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

/**
 * P1-RC-019 / RC-R1.11 receive-vs-order 超收容差校验（plan 2026-08-08-1603-1）。
 *
 * <p>验证 {@code ErpPurReceive__approve} 经 {@code ErpPurReceiveProcessor.validateOverReceiptTolerance}
 * 的六路径矩阵：strict 超收拒绝 / 非 strict 超收 warn 放行 / 容差内放行 / 多入库单聚合超收 /
 * 无订单行跳过 / 恰好等于容差边界放行；config 默认值回归（不设 config 时默认 5% 容差 + 非 strict）。
 *
 * <p>校验点位于 {@code triggerIncomingMove} 之前（ErpPurReceiveApproveProcessor.approve:34 先于 :37），
 * 超收拒绝发生在库存移动/过账之前。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveOverReceiptTolerance extends JunitAutoTestCase {

    static final Long ORG_ID = 1801L;
    static final Long SUPPLIER_ID = 2801L;
    static final Long WAREHOUSE_ID = 3801L;
    static final Long MATERIAL_ID = 4801L;
    static final Long UOM_ID = 5801L;
    static final Long CURRENCY_ID = 6801L;
    static final Long ACCT_SCHEMA_ID = 7801L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStrictRejectsOverReceipt() {
        // 订单 10 + 入库 20（超收 100% > 5% 容差），strict=true → ERR_RECEIVE_QTY_OVER_TOLERANCE
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        seedChain("PO-OVER-STRICT-001", "PR-OVER-STRICT-001", orderLineId, receiveId, receiveLineId,
                new BigDecimal("10"), new BigDecimal("20"));

        withStrictMode(true, () -> {
            ApiResponse<?> bad = approve(receiveId);
            assertEquals(ErpPurErrors.ERR_RECEIVE_QTY_OVER_TOLERANCE.getErrorCode(), bad.getCode(),
                    "订单10+入库20 strict → 超收拒绝");
            ErpPurReceive receive = dao().getEntityById(receiveId);
            assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, receive.getApproveStatus(),
                    "拒绝后审核状态保持 SUBMITTED");
        });
    }

    @Test
    public void testNonStrictWarnsAndApproves() {
        // 同场景 strict=false（默认）→ warn 放行 APPROVED（config 默认值回归：非 strict 生效）
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        seedChain("PO-OVER-WARN-001", "PR-OVER-WARN-001", orderLineId, receiveId, receiveLineId,
                new BigDecimal("10"), new BigDecimal("20"));

        ApiResponse<?> ok = approve(receiveId);
        assertEquals(0, ok.getStatus(), "订单10+入库20 非 strict（默认）→ warn 放行");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                dao().getEntityById(receiveId).getApproveStatus(), "审核 → APPROVED");
    }

    @Test
    public void testWithinToleranceApproves() {
        // 订单 10 + 入库 10.5，5% 容差内（默认 config）→ 放行
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        seedChain("PO-OVER-IN-001", "PR-OVER-IN-001", orderLineId, receiveId, receiveLineId,
                new BigDecimal("10"), new BigDecimal("10.5"));

        ApiResponse<?> ok = approve(receiveId);
        assertEquals(0, ok.getStatus(), "10.5 在 10×(1+5%) 容差内 → 放行");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                dao().getEntityById(receiveId).getApproveStatus(), "审核 → APPROVED");
    }

    @Test
    public void testBoundaryExactlyToleranceApproves() {
        // 恰好等于容差边界 10.5（含边界放行），strict=true 同样放行
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        seedChain("PO-OVER-BOUND-001", "PR-OVER-BOUND-001", orderLineId, receiveId, receiveLineId,
                new BigDecimal("10"), new BigDecimal("10.5"));

        withStrictMode(true, () -> {
            ApiResponse<?> ok = approve(receiveId);
            assertEquals(0, ok.getStatus(), "恰好 = 容差边界 strict → 放行");
            assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                    dao().getEntityById(receiveId).getApproveStatus(), "审核 → APPROVED");
        });
    }

    @Test
    public void testAggregatedOverReceiptRejectsSecond() {
        // 订单 10 分批入库 6 + 5 = 11 > 10.5，strict → 第二张入库单拒绝、第一张保持 APPROVED
        Long orderLineId = nextId();
        Long receive1 = nextId();
        Long receiveLine1 = nextId();
        Long receive2 = nextId();
        Long receiveLine2 = nextId();
        seedChain("PO-OVER-AGG-001", "PR-OVER-AGG-001", orderLineId, receive1, receiveLine1,
                new BigDecimal("10"), new BigDecimal("6"));
        ormTemplate.runInSession(session -> {
            newReceive("PR-OVER-AGG-002", receive2, findOrderId("PO-OVER-AGG-001"));
            newReceiveLine(receiveLine2, receive2, orderLineId, new BigDecimal("5"));
            return null;
        });

        withStrictMode(true, () -> {
            assertEquals(0, approve(receive1).getStatus(), "第一张 6 ≤ 10.5 → 放行 APPROVED");

            ApiResponse<?> bad = approve(receive2);
            assertEquals(ErpPurErrors.ERR_RECEIVE_QTY_OVER_TOLERANCE.getErrorCode(), bad.getCode(),
                    "6+5=11 > 10.5 聚合超收 strict → 第二张拒绝");
            assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED,
                    dao().getEntityById(receive2).getApproveStatus(), "第二张保持 SUBMITTED");
            assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                    dao().getEntityById(receive1).getApproveStatus(), "第一张保持 APPROVED");
        });
    }

    @Test
    public void testNoOrderLineSkipped() {
        // 行 orderLineId=null（无订单关联独立入库）→ 跳过校验，strict 下超量也放行
        Long orderId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedFinanceAndSupplier();
            newOrder("PO-OVER-NOLINE-001", orderId);
            newReceive("PR-OVER-NOLINE-001", receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, null, new BigDecimal("999"));
            return null;
        });

        withStrictMode(true, () -> {
            ApiResponse<?> ok = approve(receiveId);
            assertEquals(0, ok.getStatus(), "orderLineId=null 行不触发校验 → 放行");
            assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                    dao().getEntityById(receiveId).getApproveStatus(), "审核 → APPROVED");
        });
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

    private ApiResponse<?> approve(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    // ---------- seed helpers ----------

    private void seedChain(String orderCode, String receiveCode, Long orderLineId, Long receiveId,
                           Long receiveLineId, BigDecimal orderQty, BigDecimal receiveQty) {
        ormTemplate.runInSession(session -> {
            seedFinanceAndSupplier();
            Long orderId = newOrder(orderCode, nextId());
            newOrderLine(orderId, orderLineId, orderQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty);
            return null;
        });
    }

    private void seedFinanceAndSupplier() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
        seedSubject("1401", "库存商品");
        seedSubject("2202", "应付账款-暂估");
        seedSubject("6401", "主营业务成本");
        seedAcctSchema();
        seedActiveSupplier();
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

    private Long newOrder(String code, Long orderId) {
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
        return order.getId();
    }

    private Long findOrderId(String code) {
        return daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(new io.nop.api.core.beans.query.QueryBean())
                .stream().filter(o -> code.equals(o.getCode())).findFirst()
                .map(ErpPurOrder::getId).orElseThrow(() -> new IllegalStateException("order not found: " + code));
    }

    private void newOrderLine(Long orderId, Long lineId, BigDecimal qty) {
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

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty) {
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

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
