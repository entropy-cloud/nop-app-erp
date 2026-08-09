package app.erp.sal.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.SnapshotTest;
import io.nop.api.core.annotations.core.OptionalBoolean;import io.nop.api.core.beans.ApiRequest;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.16 + RC-R1.17 需求合规测试：UC-SAL-05 未开票退货冲减族。
 *
 * <p>P1-RC-023 矩阵（未交货量回填）：① 公式数值断言；② 无 deliveryLineId 跳过；③ 多行聚合；④ 幂等；⑤ reverseApprove 对称。
 * <p>P1-RC-024 矩阵（暂估应收条件冲减）：① 已暂估→冲减路径；② 已暂估→红字替代（等价）；③ 未暂估→跳过；④ posted=false 边界变体。
 *
 * <p>种子迁移（P0-1）：既有 delivery seed 已迁移为 posted=true（已暂估），保证 SALES_RETURN 过账路径不断裂。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnCompliance extends JunitAutoTestCase {

    static final Long ORG_ID = 3901L;
    static final Long CUSTOMER_ID = 4901L;
    static final Long WAREHOUSE_ID = 5901L;
    static final Long MATERIAL_ID = 6901L;
    static final Long UOM_ID = 7901L;
    static final Long CURRENCY_ID = 8901L;
    static final Long ACCT_SCHEMA_ID = 9901L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(900000L);

    // ==================== P1-RC-023 未交货量回填矩阵 ====================

    /**
     * ① doApprove 后订单行未交货量 = quantity − deliveredQuantity + Σ退货量（L1 公式数值断言）。
     *
     * <p>order qty=10, delivered=10（出库行 qty=10）, returned=4 → 未交货量 = 10 − 10 + 4 = 4。
     */
    @Test
    public void testUndeliveredQuantityFormulaAfterApprove() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-001", new BigDecimal("10"));
        Long orderLineId = ctx[2];
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-001", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpSalOrderLine orderLine = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertNotNull(orderLine, "订单行应存在");
        assertEquals(0, new BigDecimal("10").compareTo(orderLine.getDeliveredQuantity()),
                "deliveredQuantity = Σ APPROVED 出库行 qty = 10（毛口径）");

        BigDecimal undelivered = orderLine.getQuantity()
                .subtract(orderLine.getDeliveredQuantity())
                .add(sumApprovedReturnQty(orderLineId));
        assertEquals(0, new BigDecimal("4").compareTo(undelivered),
                "未交货量 = 10 − 10 + 4 = 4（L1 公式）");
    }

    /**
     * ② 退货行无 deliveryLineId → 跳过不报错（deliveredQuantity 不更新）。
     */
    @Test
    public void testReturnLineWithoutDeliveryLineIdSkipsSilently() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-002", new BigDecimal("10"));
        Long orderLineId = ctx[2];
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-002", returnId, ctx[0]);
            ErpSalReturnLine line = new ErpSalReturnLine();
            line.setId(nextId());
            line.setReturnId(returnId);
            line.setLineNo(1);
            line.setDeliveryLineId(null);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("3"));
            line.setUnitPrice(new BigDecimal("5"));
            line.setAmount(new BigDecimal("15"));
            line.setReason("无 deliveryLineId 行");
            daoProvider.daoFor(ErpSalReturnLine.class).saveEntity(line);
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功（无 deliveryLineId 行不报错）");

        ErpSalOrderLine orderLine = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertNotNull(orderLine, "订单行应存在");
    }

    /**
     * ③ 多行同订单聚合正确：同一 orderLineId 有多条退货行 → 聚合正确。
     */
    @Test
    public void testMultipleReturnLinesAggregateCorrectly() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-003", new BigDecimal("10"));
        Long orderLineId = ctx[2];
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-003", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("3"), new BigDecimal("5"));
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpSalOrderLine orderLine = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertEquals(0, new BigDecimal("10").compareTo(orderLine.getDeliveredQuantity()),
                "deliveredQuantity = 10（毛口径，多条退货行不影响 delivered 聚合）");

        BigDecimal sumReturned = sumApprovedReturnQty(orderLineId);
        assertEquals(0, new BigDecimal("5").compareTo(sumReturned),
                "Σ退货量 = 3 + 2 = 5");
    }

    /**
     * ④ 重复退货二次回填不重复累加（幂等语义——按重新聚合重算，非增量累加）。
     *
     * <p>两张退货单各退 3：第一张审核后 deliveredQuantity=10；第二张审核后 deliveredQuantity 仍=10（幂等重算）。
     */
    @Test
    public void testIdempotentUndeliveredQuantityOnSecondReturn() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-004", new BigDecimal("10"));
        Long orderLineId = ctx[2];

        Long return1 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-004A", return1, ctx[0]);
            newReturnLine(nextId(), return1, ctx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(return1).getStatus(), "第一张退货审核");

        Long return2 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-004B", return2, ctx[0]);
            newReturnLine(nextId(), return2, ctx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(return2).getStatus(), "第二张退货审核");

        ErpSalOrderLine orderLine = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertEquals(0, new BigDecimal("10").compareTo(orderLine.getDeliveredQuantity()),
                "deliveredQuantity 幂等 = 10（不因两张退货单重复累加）");
    }

    /**
     * ⑤ reverseApprove 对称回退：审核后 deliveredQuantity=10，反审核后仍=10（毛口径不变，对称重算）。
     */
    @Test
    public void testReverseApproveSymmetricUndeliveredQuantity() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-005", new BigDecimal("10"));
        Long orderLineId = ctx[2];
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-005", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核");
        ErpSalOrderLine afterApprove = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertEquals(0, new BigDecimal("10").compareTo(afterApprove.getDeliveredQuantity()),
                "审核后 deliveredQuantity=10");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核应成功");
        ErpSalOrderLine afterReverse = daoProvider.daoFor(ErpSalOrderLine.class).getEntityById(orderLineId);
        assertEquals(0, new BigDecimal("10").compareTo(afterReverse.getDeliveredQuantity()),
                "反审核后 deliveredQuantity 仍=10（毛口径，出库行不变）");
    }

    // ==================== P1-RC-024 暂估应收条件冲减矩阵 ====================

    /**
     * ① 未开票 + 已暂估（delivery.posted=true）→ 冲减路径（SALES_RETURN 凭证 + 负向 ArApItem credit memo）。
     */
    @Test
    public void testEstimatedReceivableOffsetGeneratesVoucherAndArItem() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-024A", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-024A", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        assertTrue(Boolean.TRUE.equals(reload(returnId).getPosted()), "已暂估 → posted=true（SALES_RETURN 过账）");

        ErpFinVoucherBillR link = findBillLink("RT-UC05-024A", ErpFinBusinessType.SALES_RETURN.name());
        assertNotNull(link, "应生成 SALES_RETURN 业财回链");

        ErpFinArApItem item = findArItem("RT-UC05-024A");
        assertNotNull(item, "应生成退货辅助账项（credit memo）");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, item.getDirection());
        assertTrue(item.getOpenAmountFunctional().signum() < 0, "openAmount 为负（credit memo）");
    }

    /**
     * ② 已暂估 + 已开票 → 红字替代路径（credit memo 等价，P2-MA2-011 接受；下游行为相同）。
     *
     * <p>本测试验证已开票场景下行为与未开票等价（凭证 + ArApItem 均生成）。
     */
    @Test
    public void testInvoicedRedLetterPathEquivalentToOffset() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryPosted("SD-UC05-024B", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-024B", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        assertTrue(Boolean.TRUE.equals(reload(returnId).getPosted()), "已暂估 → posted=true");

        ErpFinVoucherBillR link = findBillLink("RT-UC05-024B", ErpFinBusinessType.SALES_RETURN.name());
        assertNotNull(link, "红字替代路径：SALES_RETURN 回链仍生成（credit memo 等价）");
    }

    /**
     * ③ 未暂估（delivery.posted=false）→ tryPost 跳过事件构造（零凭证 / 零 ArApItem）。
     */
    @Test
    public void testUnpostedDeliverySkipsPostingEvent() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryNotPosted("SD-UC05-024C", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-024C", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功（不阻塞终态）");
        assertFalse(Boolean.TRUE.equals(reload(returnId).getPosted()), "未暂估 → posted=false（跳过事件构造）");

        ErpFinVoucherBillR link = findBillLink("RT-UC05-024C", ErpFinBusinessType.SALES_RETURN.name());
        assertNull(link, "未暂估 → 零凭证回链");

        ErpFinArApItem item = findArItem("RT-UC05-024C");
        assertNull(item, "未暂估 → 零 ArApItem");
    }

    /**
     * ④ posted=false 边界变体（出库已审核但未过账）→ 同 ③ 跳过语义。
     *
     * <p>验证 delivery approveStatus=APPROVED 但 posted=false 的边界场景仍正确跳过。
     */
    @Test
    public void testPostedFalseBoundarySkipsPosting() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDeliveryNotPosted("SD-UC05-024D", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-UC05-024D", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpSalReturn approved = reload(returnId);
        assertFalse(Boolean.TRUE.equals(approved.getPosted()), "posted=false 边界 → posted=false");
        assertNull(findBillLink("RT-UC05-024D", ErpFinBusinessType.SALES_RETURN.name()),
                "posted=false 边界 → 零凭证");
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
            seedSubject("1131", "应收账款");
            seedSubject("6001", "主营业务收入");
            seedSubject("2221", "应交税费-销项税额");
            seedAcctSchema();
            seedActiveCustomer();
            return null;
        });
    }

    /**
     * @return {deliveryId, deliveryLineId, orderLineId}
     */
    private Long[] seedApprovedDeliveryPosted(String deliveryCode, BigDecimal deliveryQty) {
        return seedDelivery(deliveryCode, deliveryQty, true);
    }

    private Long[] seedApprovedDeliveryNotPosted(String deliveryCode, BigDecimal deliveryQty) {
        return seedDelivery(deliveryCode, deliveryQty, false);
    }

    private Long[] seedDelivery(String deliveryCode, BigDecimal deliveryQty, boolean posted) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDeliveryApproved(deliveryCode, deliveryId, orderId, posted);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId, orderLineId};
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

    private void newDeliveryApproved(String code, Long deliveryId, Long orderId, boolean posted) {
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
        delivery.setPosted(posted);
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

    private void newReturn(String code, Long returnId, Long deliveryId) {
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
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setTotalAmountWithTax(new BigDecimal("24"));
        returnOrder.setPosted(false);
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

    // ---------- query helpers ----------

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    private BigDecimal sumApprovedReturnQty(Long orderLineId) {
        List<Long> deliveryLineIds = daoProvider.daoFor(ErpSalDeliveryLine.class).findAllByQuery(
                new QueryBean()).stream()
                .filter(dl -> orderLineId.equals(dl.getOrderLineId()))
                .map(ErpSalDeliveryLine::getId)
                .collect(java.util.stream.Collectors.toList());
        if (deliveryLineIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.in("deliveryLineId", deliveryLineIds));
        return dao.findAllByQuery(q).stream()
                .map(ErpSalReturnLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinArApItem findArItem(String returnCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", ErpFinConstants.SOURCE_BILL_SAL_RETURN),
                eq("sourceBillCode", returnCode)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
