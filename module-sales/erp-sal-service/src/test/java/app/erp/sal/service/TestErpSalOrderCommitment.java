package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * sales 承付 3 接入点端到端集成测试（plan 2026-07-24-1351-3 §Phase 2 Proof）。
 *
 * <p>验证销售订单审核 → COMMITMENT 凭证生成（billType=SALES_ORDER_COMMITMENT）的 hook 装配正确性，
 * 镜像 {@code TestErpPurOrderCommitment} 范式：
 *
 * <p>3 场景覆盖 budget.md §sales 承付扩展 §接入点：
 * <ul>
 *   <li>场景1（commit, 接入点 #1）：{@code erp-fin.budget-commitment-enabled=true} 时
 *       {@code ErpSalOrder__approve} 触发 COMMITMENT 凭证创建 + 业财回链
 *       billType=SALES_ORDER_COMMITMENT 反查订单 code。</li>
 *   <li>场景2（release-on-cancel, 接入点 #2）：{@code ErpSalOrder__reverseApprove} 红冲原 COMMITMENT 凭证。</li>
 *   <li>场景3（release-on-cancel via cancel）：{@code ErpSalOrder__cancel} 红冲原 COMMITMENT 凭证。</li>
 * </ul>
 *
 * <p>release-on-invoice-approve（接入点 #3）路径在 {@code TestErpFinBudgetCommitment} 单元测试覆盖
 * （SPI 入口相同，事务边界裁决一致）；本集成测试聚焦 commit + release-on-cancel hook 装配正确性。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-commitment-sales-test.yaml")
public class TestErpSalOrderCommitment extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long CUSTOMER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;
    static final String SALES_COMMITMENT_BILL_TYPE = "SALES_ORDER_COMMITMENT";
    static final String POSTING_TYPE_COMMITMENT = "COMMITMENT";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveTriggersSalesCommitmentVoucherWhenEnabled() {
        seedPeriodAndCommitmentSubject();

        ErpSalOrder order = newOrder("SO-CMT-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, submit(order.getId()).getStatus(), "提交应成功");
        assertEquals(0, approve(order.getId()).getStatus(), "审核应成功");

        ErpFinVoucherBillR link = findCommitmentBillLink(order.getCode());
        assertNotNull(link, "应生成业财回链 billType=SALES_ORDER_COMMITMENT");
        assertEquals(SALES_COMMITMENT_BILL_TYPE, link.getBillType());
        assertEquals(order.getCode(), link.getBillCode());

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "COMMITMENT 凭证应落库");
        assertEquals(POSTING_TYPE_COMMITMENT, voucher.getPostingType(),
                "凭证 postingType 应为 COMMITMENT");
        assertFalse(Boolean.TRUE.equals(voucher.getIsReversed()),
                "原承付凭证 isReversed=false");
    }

    @Test
    public void testReverseApproveReversesCommitmentVoucher() {
        seedPeriodAndCommitmentSubject();

        ErpSalOrder order = newOrder("SO-CMT-002");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus());
        ErpFinVoucherBillR link = findCommitmentBillLink(order.getCode());
        assertNotNull(link, "审核时应已生成 COMMITMENT 凭证");
        Long originalVoucherId = link.getVoucherId();

        assertEquals(0, reverseApprove(order.getId()).getStatus(),
                "反审核应成功（release-on-cancel 同事务）");

        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalVoucherId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "反审核后原承付凭证应 isReversed=true（红冲完成）");
    }

    @Test
    public void testCancelReversesCommitmentVoucher() {
        seedPeriodAndCommitmentSubject();

        ErpSalOrder order = newOrder("SO-CMT-003");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus());
        ErpFinVoucherBillR link = findCommitmentBillLink(order.getCode());
        assertNotNull(link);
        Long originalVoucherId = link.getVoucherId();

        assertEquals(0, cancel(order.getId()).getStatus(),
                "作废应成功（release-on-cancel 同事务）");

        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalVoucherId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "作废后原承付凭证应 isReversed=true（红冲完成）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> reverseApprove(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> cancel(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__cancel",
                ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalOrder newOrder(String code) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        return order;
    }

    private void saveOrderWithLine(ErpSalOrder order) {
        order.setAmountSource(new BigDecimal("50"));
        order.setAmountFunctional(new BigDecimal("50"));
        order.setTotalAmount(new BigDecimal("50"));
        order.setTotalTaxAmount(BigDecimal.ZERO);
        order.setTotalAmountWithTax(new BigDecimal("50"));
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(new BigDecimal("50"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndCommitmentSubject() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
            p.setCode("2026-07");
            p.setName("2026-07");
            p.setOrgId(ORG_ID);
            p.setYear(2026);
            p.setMonth(7);
            p.setStartDate(LocalDate.of(2026, 7, 1));
            p.setEndDate(LocalDate.of(2026, 7, 31));
            p.setStatus("OPEN");
            pDao.saveEntity(p);

            IEntityDao<ErpMdSubject> sDao = daoProvider.daoFor(ErpMdSubject.class);
            ErpMdSubject s = new ErpMdSubject();
            s.setCode("6001");
            s.setName("销售承付收入预留科目");
            s.setSubjectClass("INCOME");
            s.setDirection("CREDIT");
            s.setStatus("ACTIVE");
            sDao.saveEntity(s);
        });
    }

    private ErpFinVoucherBillR findCommitmentBillLink(String orderCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", orderCode));
        q.addFilter(eq("billType", SALES_COMMITMENT_BILL_TYPE));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        return links.isEmpty() ? null : links.get(0);
    }
}
