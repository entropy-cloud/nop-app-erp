package app.erp.pur.service;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
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
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-MA2-082 release-on-return 集成测试（plan §Phase 2 Proof）。
 *
 * <p>验证 {@code ErpPurReturnProcessor.approve} 后置承付释放钩子（budget.md §承付会计 §3 接入点 #4）：
 * <ul>
 *   <li>场景1（ON）：{@code commitment-release-on-return=true} 时退货审核红冲原 PO COMMITMENT 凭证。</li>
 *   <li>场景2（部分退货全额释放）：PO qty 远大于退货 qty，承付仍全额红冲（documented 全额释放语义，
 *       剩余未开票数量失去承付保护——按比例部分释放归 successor）。</li>
 * </ul>
 *
 * <p>OFF 默认回归（不调 release）由 {@code budget-commitment-enabled} 默认 false + 本开关默认 false
 * 双重门控，既有 {@link TestErpPurReturnApproval}（默认 config）已间接覆盖。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:return-commitment-test.yaml")
public class TestErpPurReturnCommitmentRelease extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    static final Long ORG_ID = 7101L;
    static final Long SUPPLIER_ID = 7201L;
    static final Long WAREHOUSE_ID = 7301L;
    static final Long MATERIAL_ID = 7401L;
    static final Long UOM_ID = 7501L;
    static final Long CURRENCY_ID = 7601L;
    static final Long ACCT_SCHEMA_ID = 7701L;
    static final String COMMITMENT_BILL_TYPE = "PURCHASE_ORDER_COMMITMENT";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpFinBudgetCommitmentBiz commitmentBiz;

    private final AtomicLong idSeq = new AtomicLong(700000L);

    @Test
    public void testReturnApproveReleasesCommitmentWhenEnabled() {
        Long periodId = seedPeriodAndSubjects("2026-07");
        Long commitmentSubjectId = findSubjectByCode("1408").getId();
        Long[] receiveCtx = seedApprovedReceive("PO-RTC-001", "PR-RTC-001", "2026-07",
                new BigDecimal("10"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        // 为 PO code 创建承付凭证（模拟 PO approve 时的 commit）
        Long commitmentVoucherId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-RTC-001",
                        commitmentSubjectId, null, periodId, new BigDecimal("500"), CTX));
        assertNotNull(commitmentVoucherId, "应预置承付凭证");

        // 创建退货单（SUBMITTED）+ 行
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RTC-001", returnId, receiveId);
            newReturnLine(nextId(), returnId, receiveLineId, new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });

        // 审核退货 → 触发 release-on-return hook
        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        // 断言原承付凭证已被红冲
        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(commitmentVoucherId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "release-on-return=true 时退货审核应红冲原 PO 承付凭证");
    }

    @Test
    public void testPartialReturnFullReleaseSemantics() {
        // P1-MA2-082 全额释放语义：PO qty=100（commit 全额），部分退货 qty=5 → 承付仍全额红冲。
        // 剩余未开票数量（95）失去承付保护——documented 行为，按比例部分释放归 successor。
        Long periodId = seedPeriodAndSubjects("2026-08");
        Long commitmentSubjectId = findSubjectByCode("1408").getId();
        Long[] receiveCtx = seedApprovedReceive("PO-RTC-002", "PR-RTC-002", "2026-08",
                new BigDecimal("100"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        Long commitmentVoucherId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-RTC-002",
                        commitmentSubjectId, null, periodId, new BigDecimal("5000"), CTX));
        assertNotNull(commitmentVoucherId);

        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RTC-002", returnId, receiveId);
            // 仅退 5（部分退货）
            newReturnLine(nextId(), returnId, receiveLineId, new BigDecimal("5"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "部分退货审核应成功");

        // 部分退货仍全额红冲承付（documented 全额释放语义）
        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(commitmentVoucherId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "部分退货应全额红冲承付（全额释放语义；剩余未开票数量失去承付保护，归 successor）");
    }

    // ---------- seed ----------

    private Long seedPeriodAndSubjects(String periodCode) {
        return ormTemplate.runInSession(session -> {
            int month = Integer.parseInt(periodCode.split("-")[1]);
            IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
            p.setCode(periodCode);
            p.setName(periodCode);
            p.setOrgId(ORG_ID);
            p.setYear(2026);
            p.setMonth(month);
            p.setStartDate(LocalDate.of(2026, month, 1));
            p.setEndDate(LocalDate.of(2026, month, 28));
            p.setStatus("OPEN");
            pDao.saveEntity(p);

            seedSubject("1408", "承付占用科目", "EXPENSE", "DEBIT");
            seedSubject("1401", "库存商品", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款-暂估", "LIABILITY", "CREDIT");
            seedAcctSchema();
            seedActiveSupplier();
            return p.getId();
        });
    }

    private Long[] seedApprovedReceive(String poCode, String receiveCode, String periodCode,
                                       BigDecimal receiveQty, BigDecimal unitPrice) {
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            Long orderId = newOrder(poCode);
            newOrderLine(orderId, orderLineId, 1, receiveQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty, unitPrice);
            return null;
        });
        assertEquals(0, executeRpc(mutation, "ErpPurReceive__approve",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId)))).getStatus(), "源入库单审核应成功");
        return new Long[]{receiveId, receiveLineId};
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

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private ErpMdSubject findSubjectByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = daoProvider.daoFor(ErpMdSubject.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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
        returnOrder.setTotalAmount(new BigDecimal("15"));
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

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
