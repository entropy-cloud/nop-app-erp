package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 1 服务层集成测试：销售订单三轴审批状态机（仅状态推进，不触发库存/凭证）+ 客户启用校验 +
 * 客户信用额度校验（SOFT_WARNING 放行 / HARD_BLOCK 拒绝 / outstanding 口径）。
 *
 * <p>直接调用 {@link IErpSalOrderBiz} 的 Java API（不走 GraphQL 快照），自建客户/行明细后断言状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2301L;
    static final Long CUSTOMER_ID_2 = 2302L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSalOrderBiz orderBiz;

    @Test
    public void testOrderSubmitApproveRejectResubmit() {
        ErpSalOrder order = newOrder("SO-SUBMIT-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        ErpSalOrder submitted = orderBiz.submit(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        ErpSalOrder rejected = orderBiz.reject(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        ErpSalOrder resubmitted = orderBiz.submit(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");

        // approve 仅状态推进（无库存/凭证触发），信用额度不超限 → APPROVED
        ErpSalOrder approved = orderBiz.approve(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核 → APPROVED");
    }

    @Test
    public void testOrderIllegalTransitionRejected() {
        ErpSalOrder order = newOrder("SO-ILL-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        // UNSUBMITTED→approve 非法（仅 SUBMITTED 可审核）
        assertThrows(NopException.class, () -> orderBiz.approve(order.getId()),
                "未提交不可直接审核，应抛 NopException");

        orderBiz.submit(order.getId());
        orderBiz.withdrawSubmit(order.getId());
        // UNSUBMITTED 不可撤回提交
        assertThrows(NopException.class, () -> orderBiz.withdrawSubmit(order.getId()),
                "UNSUBMITTED 不可撤回提交，应抛 NopException");
    }

    @Test
    public void testOrderInactiveCustomerRejected() {
        ErpSalOrder order = newOrder("SO-INACTIVE-001", "100");
        ormTemplate.runInSession(() -> {
            seedCustomer(CUSTOMER_ID, 20, null);
            saveOrderWithLine(order, "10");
        });

        assertThrows(NopException.class, () -> orderBiz.submit(order.getId()),
                "客户停用 → submit 应抛 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCreditLimitSoftWarningAllows() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        ErpSalOrder order = newOrder("SO-CREDIT-SOFT-001", "150");
        ormTemplate.runInSession(() -> {
            // 额度 100，本单 150 → 超额度，SOFT_WARNING 放行
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        orderBiz.submit(order.getId());
        ErpSalOrder approved = orderBiz.approve(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "SOFT_WARNING 超额度应放行 → APPROVED");
    }

    @Test
    public void testCreditLimitHardBlockRejects() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder order = newOrder("SO-CREDIT-HARD-001", "150");
        ormTemplate.runInSession(() -> {
            // 额度 100，本单 150 → 超额度，HARD_BLOCK 拒绝
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        orderBiz.submit(order.getId());
        assertThrows(NopException.class, () -> orderBiz.approve(order.getId()),
                "HARD_BLOCK 超额度应抛 ERR_CREDIT_LIMIT_EXCEEDED");
    }

    @Test
    public void testCreditLimitNullNoCheck() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder order = newOrder("SO-CREDIT-NULL-001", "999999");
        ormTemplate.runInSession(() -> {
            // creditLimit=null → 不控制，即使 HARD_BLOCK 也放行
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        orderBiz.submit(order.getId());
        ErpSalOrder approved = orderBiz.approve(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "creditLimit=null 不控制 → APPROVED");
    }

    @Test
    public void testOutstandingIncludesApprovedUndeliveredOrders() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder orderA = newOrder("SO-OUT-A-001", "60");
        ErpSalOrder orderB = newOrder("SO-OUT-B-001", "50");
        ormTemplate.runInSession(() -> {
            // 额度 100：A(60) + B(50) = 110 > 100
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(orderA, "10");
            saveOrderWithLine(orderB, "10");
        });

        // A 审核：outstanding 此时空（无已审核单），60 ≤ 100 → APPROVED
        orderBiz.submit(orderA.getId());
        ErpSalOrder approvedA = orderBiz.approve(orderA.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedA.getApproveStatus(),
                "A 首单审核不超额度 → APPROVED");

        // B 审核：outstanding 含 A(60, 未出库)，available=40 < 50 → 拒绝（证明 A 计入 outstanding）
        orderBiz.submit(orderB.getId());
        assertThrows(NopException.class, () -> orderBiz.approve(orderB.getId()),
                "outstanding 含已审核未出库 A → B 超额度拒绝");

        // 将 A 置为 DELIVERED（已发货）→ 不再计入 outstanding，B 应可审核通过
        ormTemplate.runInSession(() -> {
            ErpSalOrder a = daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderA.getId());
            a.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_DELIVERED);
            daoProvider.daoFor(ErpSalOrder.class).updateEntity(a);
        });
        ErpSalOrder approvedB = orderBiz.approve(orderB.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedB.getApproveStatus(),
                "A 已发货不计入 outstanding → B 通过");
    }

    // ---------- helpers ----------

    private io.nop.dao.api.IEntityDao<ErpSalOrder> dao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    private ErpSalOrder newOrder(String code, String totalAmountWithTax) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setTotalAmountWithTax(new BigDecimal(totalAmountWithTax));
        order.setTotalAmount(new BigDecimal(totalAmountWithTax));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpSalOrder order, String quantity) {
        dao().saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal("10"));
        line.setAmount(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        seedCustomer(id, ErpSalConstants.PARTNER_STATUS_ACTIVE, creditLimit);
    }

    private void seedCustomer(Long id, int status, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType(10);
        partner.setStatus(status);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }
}
