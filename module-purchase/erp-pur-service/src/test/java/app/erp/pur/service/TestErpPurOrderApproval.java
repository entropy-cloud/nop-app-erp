package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 1 服务层集成测试：采购订单三轴审批状态机 + 供应商启用校验。
 *
 * <p>直接调用 {@link IErpPurOrderBiz} 的 Java API（不走 GraphQL 快照），自建供应商/行明细后断言状态迁移。
 * 订单审核 = 纯状态推进（state-machine §2「采购订单｜仅状态推进」），不触发库存/凭证。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurOrderApproval extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPurOrderBiz orderBiz;

    @Test
    public void testOrderSubmitApproveRejectResubmit() {
        ErpPurOrder order = newOrder("PO-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        ErpPurOrder submitted = orderBiz.submit(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        ErpPurOrder approved = orderBiz.approve(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核通过 → APPROVED");
        assertEquals("PO-SUBMIT-001", approved.getCode(), "订单审核不触发库存/凭证，仅状态推进");
    }

    @Test
    public void testOrderRejectAndResubmit() {
        ErpPurOrder order = newOrder("PO-REJ-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        orderBiz.submit(order.getId(), CTX);
        ErpPurOrder rejected = orderBiz.reject(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        ErpPurOrder resubmitted = orderBiz.submit(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testOrderIllegalTransitionRejected() {
        ErpPurOrder order = newOrder("PO-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        orderBiz.submit(order.getId(), CTX);
        ErpPurOrder approved = orderBiz.approve(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // APPROVED → 再次 submit 非法（仅 UNSUBMITTED/REJECTED 可提交）
        assertThrows(NopException.class, () -> orderBiz.submit(order.getId(), CTX),
                "APPROVED 不可再提交，应抛 NopException");
        // APPROVED → withdrawSubmit 非法
        assertThrows(NopException.class, () -> orderBiz.withdrawSubmit(order.getId(), CTX),
                "APPROVED 不可撤回提交，应抛 NopException");

        // 反审核 APPROVED → REJECTED，目标态非 UNSUBMITTED（state-machine §3/§11.4）
        ErpPurOrder reversed = orderBiz.reverseApprove(order.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核目标态 = REJECTED 非 UNSUBMITTED");
    }

    @Test
    public void testOrderInactiveSupplierRejected() {
        // submit 路径：供应商停用 → submit 拒绝
        ErpPurOrder order = newOrder("PO-INACTIVE-001");
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        ormTemplate.runInSession(() -> {
            seedSupplier(SUPPLIER_ID, 20);
            saveOrderWithLine(order);
        });
        assertThrows(NopException.class, () -> orderBiz.submit(order.getId(), CTX),
                "供应商停用 → submit 应抛 ERR_PARTNER_INACTIVE");

        // approve 路径：直接置 SUBMITTED 后审核也应被供应商停用拒绝（approve 双点校验）
        ErpPurOrder submittedOrder = newOrder("PO-INACTIVE-002");
        submittedOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> saveOrderWithLine(submittedOrder));
        assertThrows(NopException.class, () -> orderBiz.approve(submittedOrder.getId(), CTX),
                "供应商停用 → approve 也应抛 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testOrderCancelFromDraft() {
        ErpPurOrder order = newOrder("PO-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        ErpPurOrder cancelled = orderBiz.cancel(order.getId(), CTX);
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        // 已作废不可再提交
        assertThrows(NopException.class, () -> orderBiz.submit(order.getId(), CTX),
                "已作废订单不可提交，应抛 NopException");
    }

    // ---------- helpers ----------

    private ErpPurOrder newOrder(String code) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpPurOrder order) {
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);
        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(new BigDecimal("50"));
        lineDao.saveEntity(line);
    }

    private void seedActiveSupplier(Long id) {
        seedSupplier(id, ErpPurConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedSupplier(Long id, int status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType(10);
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
