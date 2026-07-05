package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：出库审核触发库存移动（{@code generateMove} OUTGOING）+ 可用量校验 + posted 接线
 * + 发货状态回写 + 反向冲销。
 *
 * <p>覆盖 sales→inventory→finance 三域经 {@code generateMove} 端到端，含销售独有可用量门控。
 * 审核/反审核/预置库存经 {@link IGraphQLEngine} 调 {@code ErpSalDelivery__approve/reverseApprove} 与
 * {@code ErpInvStockMove__generateMove}，引擎建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDeliveryStockMove extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long CUSTOMER_ID = 2201L;
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
    public void testApproveGeneratesOutgoingMoveAndPosting() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-POST-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newDelivery("SD-POST-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("10"));
            return null;
        });
        seedStock("SEED-OUT-001", new BigDecimal("20"), new BigDecimal("5"));

        ApiResponse<?> resp = approve(deliveryId);
        assertEquals(0, resp.getStatus(), "审核应成功");

        ErpSalDelivery approved = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(), "审核 → APPROVED");
        assertEquals(true, approved.getPosted(), "出库移动单 DONE + 过账成功 → posted=true");

        ErpInvStockMove move = findMove("SD-POST-001");
        assertNotNull(move, "应生成出库移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType(), "出库类型");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");
        assertEquals(true, move.getPosted(), "移动单存货过账成功 posted=true");

        ErpInvStockBalance balance = findBalance();
        assertNotNull(balance, "应存在库存余额");
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")),
                "余额 total = 20(预置) - 10(出库) = 10");

        ErpFinVoucherBillR link = findVoucherLink(move.getCode());
        assertNotNull(link, "应生成业财回链（billCode=移动单 code）");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("50")) == 0,
                "借方合计=主营业务成本 10×5=50");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("50")) == 0,
                "贷方合计=库存商品 50");
    }

    @Test
    public void testApproveInsufficientAvailableRollsBack() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-INSUF-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newDelivery("SD-INSUF-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("10"));
            return null;
        });
        // 仅预置 5（不足出库需要的 10）
        seedStock("SEED-INSUF-001", new BigDecimal("5"), new BigDecimal("5"));

        // 可用量不足 → 库存域 CONFIRM 拒绝 → 整个审核事务回滚
        ApiResponse<?> bad = approve(deliveryId);
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), bad.getCode(),
                "可用量不足应返回 ERR_AVAILABLE_INSUFFICIENT 致审核回滚");

        // 出库单保持 SUBMITTED
        ErpSalDelivery after = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, after.getApproveStatus(),
                "审核回滚 → 出库单保持 SUBMITTED");
        assertFalse(after.getPosted(), "回滚 → posted 仍为 false");
        // 移动单未推进到 DONE（库存域 CONFIRM 拒绝致整笔回滚）
        ErpInvStockMove residual = findMove("SD-INSUF-001");
        assertTrue(residual == null || !ErpInvConstants.DOC_STATUS_DONE.equals(residual.getDocStatus()),
                "回滚 → 移动单未推进到 DONE（无库存记账/过账效果）");
        // 余额未变（仍为预置 5）
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("5")),
                "回滚 → 库存余额未扣减");
    }

    @Test
    public void testApproveIdempotent() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-IDEM-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newDelivery("SD-IDEM-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("10"));
            return null;
        });
        seedStock("SEED-IDEM-001", new BigDecimal("20"), new BigDecimal("5"));

        assertEquals(0, approve(deliveryId).getStatus());
        assertTrue(approve(deliveryId).getStatus() == 0, "二次审核幂等返回成功（isAlreadyApproved 守卫：不重复执行业务回调）");

        assertEquals(1, countMoves("SD-IDEM-001"), "幂等：不应产生第二张出库移动单");
    }

    @Test
    public void testNegativeStockConfigAllowsShortage() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-NEG-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("5"));
            newDelivery("SD-NEG-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("5"));
            return null;
        });

        setNegativeStock(true);
        try {
            // erp-inv.allow-negative-stock=true → 库存域跳过可用量校验，不足仍可审核
            assertEquals(0, approve(deliveryId).getStatus(), "负库存放行 → 审核 APPROVED");

            ErpInvStockMove move = findMove("SD-NEG-001");
            assertNotNull(move, "应生成出库移动单");
            assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");

            ErpInvStockBalance balance = findBalance();
            assertNotNull(balance, "应建立库存余额");
            assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("-5")),
                    "totalQty 允许为负 -5");
        } finally {
            setNegativeStock(false);
        }
    }

    @Test
    public void testDeliveryStatusRollupToOrder() {
        seedPeriodAndSubjects();
        Long orderLine1 = nextId();
        Long orderLine2 = nextId();
        Long delivery1 = nextId();
        Long deliveryLine1 = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-ROLL-001");
            newOrderLine(orderId, orderLine1, 1, new BigDecimal("10"));
            newOrderLine(orderId, orderLine2, 2, new BigDecimal("10"));
            newDelivery("SD-ROLL-001", delivery1, orderId);
            newDeliveryLine(deliveryLine1, delivery1, orderLine1, new BigDecimal("10"));
            return null;
        });
        seedStock("SEED-ROLL-001", new BigDecimal("40"), new BigDecimal("5"));

        assertEquals(0, approve(delivery1).getStatus());
        ErpSalOrder order = findOrder("SO-ROLL-001");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_PARTIAL, order.getDeliveryStatus(),
                "订单仅 1/2 行发清 → PARTIAL");

        // 补发第 2 行 10 → 订单应 DELIVERED
        Long delivery2 = nextId();
        Long deliveryLine2 = nextId();
        ormTemplate.runInSession(session -> {
            newDelivery("SD-ROLL-002", delivery2, order.getId());
            newDeliveryLine(deliveryLine2, delivery2, orderLine2, new BigDecimal("10"));
            return null;
        });
        assertEquals(0, approve(delivery2).getStatus());

        assertEquals(ErpSalConstants.DELIVERY_STATUS_DELIVERED, findOrder("SO-ROLL-001").getDeliveryStatus(),
                "两行均发清 → 订单 DELIVERED");
    }

    @Test
    public void testReverseApproveInternallyReversesMove() {
        seedPeriodAndSubjects();
        Long orderLineId = nextId();
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-REV-001");
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newDelivery("SD-REV-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("10"));
            return null;
        });
        seedStock("SEED-REV-001", new BigDecimal("20"), new BigDecimal("5"));

        assertEquals(0, approve(deliveryId).getStatus());
        ErpInvStockMove original = findMove("SD-REV-001");
        assertNotNull(original);
        assertEquals(0, countReversals(original.getCode()), "反审核前无冲销单");

        assertEquals(0, reverseApprove(deliveryId).getStatus());
        ErpSalDelivery reversed = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核 → REJECTED（保留曾审核语义）");
        assertEquals(1, countReversals(original.getCode()), "应内部生成 1 张反向冲销移动单");

        ErpInvStockMove reversal = findReversal(original.getCode());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, reversal.getDocStatus(), "冲销单自动 DONE");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, reversal.getMoveType(), "出库的反向=入库");

        // 余额被冲销回原预置值（20 - 10 出库 + 10 入库冲销 = 20）
        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("20")),
                "冲销后余额恢复为预置 20");

        // 冲销单 posted 与红字凭证回链一致（库存 reverse() 不传播 acctSchemaId，红字凭证是否生成由库存域决定）
        ErpFinVoucherBillR reversalLink = findVoucherLink(reversal.getCode());
        assertEquals(Boolean.TRUE.equals(reversal.getPosted()), reversalLink != null,
                "冲销单 posted 与红字凭证回链一致");

        // 二次反审核幂等：不产生第二张冲销单
        assertTrue(reverseApprove(deliveryId).getStatus() == 0, "二次反审核幂等返回成功（isAlreadyRejected 守卫：不重复执行冲销）");
        assertEquals(1, countReversals(original.getCode()), "二次反审核幂等，不产生第二张冲销单");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approve(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("id", String.valueOf(deliveryId))));
    }

    private ApiResponse<?> reverseApprove(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(deliveryId))));
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
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
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

    /**
     * 预置库存：经 GraphQL 调 {@code ErpInvStockMove__generateMove}(INCOMING) 业务联动建余额
     * （avgCost/totalCost 就位），供后续出库校验可用量 + 成本快照。
     */
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

    private Long newOrder(String code) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
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
        return order.getId();
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

    private void newDelivery(String code, Long deliveryId, Long orderId) {
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
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        delivery.setPosted(false);
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

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- query helpers ----------

    private ErpSalOrder findOrder(String code) {
        return daoProvider.daoFor(ErpSalOrder.class).findAllByQuery(new QueryBean()).stream()
                .filter(o -> code.equals(o.getCode())).findFirst().orElse(null);
    }

    private ErpInvStockMove findMove(String deliveryCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY));
        q.addFilter(eq("relatedBillCode", deliveryCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countMoves(String deliveryCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY));
        q.addFilter(eq("relatedBillCode", deliveryCode));
        return dao.findAllByQuery(q).size();
    }

    private ErpInvStockMove findReversal(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countReversals(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_REVERSAL));
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

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
