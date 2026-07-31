package app.erp.sal.service.posting;

import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * G2 SalReversalListener rollback 对称测试（plan {@code 2026-07-31-0744-3-r2-14}，P1-MA4-021(f) 残差）。
 *
 * <p>验证财务侧红冲后 {@link SalReversalListener} 4 rollback 方法的行为对称性。此前仅 {@code rollbackInvoice}
 * （AR_INVOICE）由 {@code TestErpSalFinanceReversalWriteback} 覆盖；本类补齐另 3 路径（listener 单元直调，
 * 范式对齐 R1.17 {@code TestPurReversalListenerReceiveRollback}）：
 * <ul>
 *   <li>{@code rollbackReceipt}（RECEIPT）：posted=false + APPROVED→REJECTED</li>
 *   <li>{@code rollbackReturn}（SALES_RETURN）：posted=false + APPROVED→REJECTED</li>
 *   <li>{@code rollbackDelivery}（SALES_OUTPUT）：仅 posted=false（approveStatus 不变——库存物理冲销独立于凭证红冲，
 *       该不对称为 P2-MA2-057 watch-only deferred 项，此处断言当前设计行为以闭合测试覆盖对称性）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestSalReversalListenerRollback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1203L;
    static final Long CUSTOMER_ID = 2201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testRollbackReceiptAlignsToRejected() {
        String code = "SR-RL-001";
        seed(receiptOf(code), ErpSalReceipt.class);

        ErpSalReceipt before = findByCode(ErpSalReceipt.class, code);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus(), "前置：receipt APPROVED");
        assertEquals(Boolean.TRUE, before.getPosted(), "前置：receipt posted=true");

        dispatchReverse("RECEIPT", code);

        ErpSalReceipt after = findByCode(ErpSalReceipt.class, code);
        assertFalse(Boolean.TRUE.equals(after.getPosted()), "财务红冲后 posted 应回退为 false");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "rollbackReceipt 应与 rollbackInvoice 对齐：APPROVED→REJECTED");
    }

    @Test
    public void testRollbackReturnAlignsToRejected() {
        String code = "SRT-RL-001";
        seed(returnOf(code), ErpSalReturn.class);

        ErpSalReturn before = findByCode(ErpSalReturn.class, code);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus(), "前置：return APPROVED");

        dispatchReverse("SALES_RETURN", code);

        ErpSalReturn after = findByCode(ErpSalReturn.class, code);
        assertFalse(Boolean.TRUE.equals(after.getPosted()), "财务红冲后 posted 应回退为 false");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "rollbackReturn 应与 rollbackInvoice 对齐：APPROVED→REJECTED");
    }

    @Test
    public void testRollbackDeliverySetsPostedFalseOnly() {
        String code = "DLV-RL-001";
        seed(deliveryOf(code), ErpSalDelivery.class);

        ErpSalDelivery before = findByCode(ErpSalDelivery.class, code);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus(), "前置：delivery APPROVED");

        dispatchReverse("SALES_OUTPUT", code);

        ErpSalDelivery after = findByCode(ErpSalDelivery.class, code);
        assertFalse(Boolean.TRUE.equals(after.getPosted()),
                "财务红冲后 posted 应回退为 false（库存物理冲销独立处理）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, after.getApproveStatus(),
                "rollbackDelivery 仅回退 posted，不翻转 approveStatus（P2-MA2-057 watch-only 当前设计行为）");
    }

    // ---------- helpers ----------

    private void dispatchReverse(String businessType, String billHeadCode) {
        ormTemplate.runInSession(session -> {
            SalReversalListener listener = new SalReversalListener();
            listener.daoProvider = daoProvider;
            VoucherReversedEvent event = new VoucherReversedEvent();
            event.setBusinessType(businessType);
            event.setBillHeadCode(billHeadCode);
            listener.onVoucherReversed(event, CTX);
            return null;
        });
    }

    private <T extends IDaoEntity> void seed(T entity, Class<T> clazz) {
        ormTemplate.runInSession(session -> {
            daoProvider.daoFor(clazz).saveEntity(entity);
            return null;
        });
    }

    private <T extends IDaoEntity> T findByCode(Class<T> clazz, String code) {
        IEntityDao<T> dao = daoProvider.daoFor(clazz);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ---------- entity factories (posted=true + APPROVED 使 rollback 守卫放行) ----------

    private ErpSalReceipt receiptOf(String code) {
        ErpSalReceipt receipt = new ErpSalReceipt();
        receipt.setCode(code);
        receipt.setOrgId(ORG_ID);
        receipt.setCustomerId(CUSTOMER_ID);
        receipt.setBusinessDate(LocalDate.of(2026, 7, 1));
        receipt.setCurrencyId(6201L);
        receipt.setExchangeRate(BigDecimal.ONE);
        receipt.setTotalAmount(new BigDecimal("113"));
        receipt.setAmountSource(new BigDecimal("113"));
        receipt.setAmountFunctional(new BigDecimal("113"));
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setWrittenOffStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        receipt.setPosted(true);
        return receipt;
    }

    private ErpSalReturn returnOf(String code) {
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setDeliveryId(8201L);
        returnOrder.setCustomerId(CUSTOMER_ID);
        returnOrder.setWarehouseId(3201L);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(6201L);
        returnOrder.setExchangeRate(BigDecimal.ONE);
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setTotalTaxAmount(BigDecimal.ZERO);
        returnOrder.setTotalAmountWithTax(new BigDecimal("20"));
        returnOrder.setPosted(true);
        return returnOrder;
    }

    private ErpSalDelivery deliveryOf(String code) {
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(3201L);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(6201L);
        delivery.setExchangeRate(BigDecimal.ONE);
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setPosted(true);
        return delivery;
    }
}
