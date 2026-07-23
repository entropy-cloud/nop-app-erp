package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.dto.VoucherReversePreview;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 凭证红字冲销预览（{@code IErpFinVoucherBiz.previewReverseVoucher}）集成测试
 * （plan 2026-07-23-1145-2 Phase 3）。
 *
 * <p>覆盖：预览返回结构化冲销信息（原凭证信息 + 红字预估 + 回链）+ 只读不改变状态 + 非 POSTED 凭证拒绝。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherReversePreview extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testPreviewReverseVoucherStructure() {
        BigDecimal amount = new BigDecimal("150.00");
        Long voucherId = seedPostedVoucher("PREVIEW-REV-1", 1L, 1L, amount, "PURCHASE_ORDER", "PO-1");

        VoucherReversePreview preview = voucherBiz.previewReverseVoucher(voucherId, CTX);

        assertEquals(voucherId, preview.getVoucherId());
        assertEquals("PREVIEW-REV-1", preview.getVoucherCode());
        assertEquals("TRANSFER", preview.getVoucherType());
        assertEquals(0, preview.getTotalDebit().compareTo(amount));
        assertEquals(0, preview.getTotalCredit().compareTo(amount));
        // 红字预估 = 原金额取负
        assertEquals(0, preview.getReversedDebit().compareTo(amount.negate()));
        assertEquals(0, preview.getReversedCredit().compareTo(amount.negate()));
        assertTrue(preview.isWillSetReversed(), "预览应标记将置 isReversed");
        assertEquals(1, preview.getBillLinks().size(), "应有 1 条业财回链");
        assertEquals("PO-1", preview.getBillLinks().get(0).getBillCode());

        // 预览为只读：凭证 isReversed 仍为 false
        assertFalse(voucher(voucherId).getIsReversed(), "预览不应改变 isReversed");
    }

    @Test
    public void testPreviewReverseVoucherRejectsDraft() {
        Long draftId = seedVoucherWithStatus("PREVIEW-REV-DRAFT", ErpFinConstants.VOUCHER_STATUS_DRAFT);
        // DRAFT 凭证不可红冲，预览应镜像 reverseVoucher 的前置校验抛异常
        assertThrows(NopException.class, () -> voucherBiz.previewReverseVoucher(draftId, CTX),
                "DRAFT 凭证预览红冲应拒绝");
    }

    // ---------- helpers ----------

    private Long seedPostedVoucher(String code, Long orgId, Long periodId, BigDecimal amount,
                                   String billType, String billCode) {
        return seedVoucher(code, orgId, periodId, amount, ErpFinConstants.VOUCHER_STATUS_POSTED, billType, billCode);
    }

    private Long seedVoucherWithStatus(String code, String docStatus) {
        return seedVoucher(code, 1L, 1L, new BigDecimal("100"), docStatus, null, null);
    }

    private Long seedVoucher(String code, Long orgId, Long periodId, BigDecimal amount, String docStatus,
                             String billType, String billCode) {
        final Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
            ErpFinVoucher voucher = voucherDao.newEntity();
            voucher.setCode(code);
            voucher.setVoucherType("TRANSFER");
            voucher.setVoucherDate(io.nop.api.core.time.CoreMetrics.today());
            voucher.setOrgId(orgId);
            voucher.setAcctSchemaId(1L);
            voucher.setPeriodId(periodId);
            voucher.setTotalDebit(amount);
            voucher.setTotalCredit(amount);
            voucher.setIsReversed(false);
            voucher.setDocStatus(docStatus);
            voucherDao.saveEntity(voucher);
            holder[0] = voucher.getId();

            if (billType != null) {
                IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
                ErpFinVoucherBillR billR = billRDao.newEntity();
                billR.setVoucherId(voucher.getId());
                billR.setBillType(billType);
                billR.setBillCode(billCode);
                billR.setBusinessType(billType);
                billRDao.saveEntity(billR);
            }
        });
        return holder[0];
    }

    private ErpFinVoucher voucher(Long id) {
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(id);
    }
}
