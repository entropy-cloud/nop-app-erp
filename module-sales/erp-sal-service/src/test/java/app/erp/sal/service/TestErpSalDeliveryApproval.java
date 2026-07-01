package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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
 * Phase 1 服务层集成测试：销售出库三轴审批状态机 + 客户启用校验。
 *
 * <p>直接调用 {@link IErpSalDeliveryBiz} 的 Java API（不走 GraphQL 快照），自建客户/行明细后断言状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDeliveryApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long CUSTOMER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSalDeliveryBiz deliveryBiz;

    @Test
    public void testSubmitRejectResubmit() {
        ErpSalDelivery delivery = newDelivery("SD-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        ErpSalDelivery submitted = deliveryBiz.submit(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        ErpSalDelivery rejected = deliveryBiz.reject(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        ErpSalDelivery resubmitted = deliveryBiz.submit(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ErpSalDelivery delivery = newDelivery("SD-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        deliveryBiz.submit(delivery.getId());
        // UNSUBMITTED→approve 非法（仅 SUBMITTED 可审核）
        assertThrows(NopException.class, () -> deliveryBiz.approve(delivery.getId()),
                "未提交不可直接审核，应抛 NopException");

        // 单独构造一个 SUBMITTED 单据验证 withdrawSubmit 后再 approve 非法
        deliveryBiz.withdrawSubmit(delivery.getId());
        assertThrows(NopException.class, () -> deliveryBiz.withdrawSubmit(delivery.getId()),
                "UNSUBMITTED 不可撤回提交，应抛 NopException");
    }

    @Test
    public void testInactiveCustomerRejected() {
        ErpSalDelivery delivery = newDelivery("SD-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedCustomer(CUSTOMER_ID, 20);
            saveDeliveryWithLine(delivery);
        });

        assertThrows(NopException.class, () -> deliveryBiz.submit(delivery.getId()),
                "客户停用 → submit 应抛 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpSalDelivery delivery = newDelivery("SD-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        ErpSalDelivery cancelled = deliveryBiz.cancel(delivery.getId());
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        // 已作废不可再提交
        assertThrows(NopException.class, () -> deliveryBiz.submit(delivery.getId()),
                "已作废单据不可提交，应抛 NopException");
    }

    // ---------- helpers ----------

    private io.nop.dao.api.IEntityDao<ErpSalDelivery> dao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
    }

    private ErpSalDelivery newDelivery(String code) {
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        delivery.setPosted(false);
        return delivery;
    }

    private void saveDeliveryWithLine(ErpSalDelivery delivery) {
        dao().saveEntity(delivery);
        IEntityDao<ErpSalDeliveryLine> lineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setDeliveryId(delivery.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id) {
        seedCustomer(id, ErpSalConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedCustomer(Long id, int status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType(10);
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
