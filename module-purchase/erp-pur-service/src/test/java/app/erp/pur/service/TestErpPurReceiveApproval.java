package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurReceiveBiz;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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
 * Phase 1 服务层集成测试：采购入库三轴审批状态机 + 供应商启用校验。
 *
 * <p>直接调用 {@link IErpPurReceiveBiz} 的 Java API（不走 GraphQL 快照），自建供应商/行明细后断言状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveApproval extends JunitAutoTestCase {
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
    IErpPurReceiveBiz receiveBiz;

    @Test
    public void testSubmitRejectResubmit() {
        ErpPurReceive receive = newReceive("PR-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        ErpPurReceive submitted = receiveBiz.submit(receive.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        ErpPurReceive rejected = receiveBiz.reject(receive.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        ErpPurReceive resubmitted = receiveBiz.submit(receive.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ErpPurReceive receive = newReceive("PR-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        receiveBiz.submit(receive.getId(), CTX);
        ErpPurReceive approved = receiveBiz.approve(receive.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // APPROVED → 再次 submit 非法（仅 UNSUBMITTED/REJECTED 可提交）
        assertThrows(NopException.class, () -> receiveBiz.submit(receive.getId(), CTX),
                "APPROVED 不可再提交，应抛 NopException");
        // APPROVED → withdrawSubmit 非法
        assertThrows(NopException.class, () -> receiveBiz.withdrawSubmit(receive.getId(), CTX),
                "APPROVED 不可撤回提交，应抛 NopException");
    }

    @Test
    public void testInactiveSupplierRejected() {
        ErpPurReceive receive = newReceive("PR-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedSupplier(SUPPLIER_ID, 20);
            saveReceiveWithLine(receive);
        });

        assertThrows(NopException.class, () -> receiveBiz.submit(receive.getId(), CTX),
                "供应商停用 → submit 应抛 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpPurReceive receive = newReceive("PR-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        ErpPurReceive cancelled = receiveBiz.cancel(receive.getId(), CTX);
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        // 已作废不可再提交
        assertThrows(NopException.class, () -> receiveBiz.submit(receive.getId(), CTX),
                "已作废单据不可提交，应抛 NopException");
    }

    // ---------- helpers ----------

    private io.nop.dao.api.IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    private ErpPurReceive newReceive(String code) {
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        return receive;
    }

    private void saveReceiveWithLine(ErpPurReceive receive) {
        dao().saveEntity(receive);
        IEntityDao<ErpPurReceiveLine> lineDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setReceiveId(receive.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
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
