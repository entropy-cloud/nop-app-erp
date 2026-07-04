package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.DualSideDiffReport.DualSideDiffRow;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双面对账一致性兜底测试（plan 2026-07-05-0115-1 Phase 3）。
 *
 * <p>覆盖：双面一致（差额 0 → CONSISTENT）、域级多核销（域级开口 < finance 开口）、
 * finance 多核销（反向）、partner 级报告正确。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:auto-recon-test.yaml")
public class TestErpFinDualSideConsistency extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testConsistentWhenBothSidesEqual() {
        long partnerId = 3100L;
        // finance 侧 settled = 80；域级 ErpPurInvoice.paidAmount = 80 → 一致
        seedArApInvoice(partnerId, "AP-3100", new BigDecimal("100"), new BigDecimal("80"));
        seedPurInvoice(partnerId, "AP-3100", new BigDecimal("100"), new BigDecimal("80"));

        DualSideDiffReport report = reconciliationBiz.checkDualSideConsistency(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, CTX);

        assertTrue(report.isConsistent(), "双面一致应返回 consistent=true");
        assertEquals(1, report.getRows().size());
        assertEquals(DualSideConsistencyChecker.STATUS_CONSISTENT, report.getRows().get(0).getStatus());
        assertEquals(0, report.getRows().get(0).getDiff().compareTo(new BigDecimal("0")));
    }

    @Test
    public void testInconsistentWhenFinanceSettledMore() {
        long partnerId = 3200L;
        // finance settled = 90；域级 paidAmount = 50 → finance 多核销 40 → INCONSISTENT
        seedArApInvoice(partnerId, "AP-3200", new BigDecimal("100"), new BigDecimal("90"));
        seedPurInvoice(partnerId, "AP-3200", new BigDecimal("100"), new BigDecimal("50"));

        DualSideDiffReport report = reconciliationBiz.checkDualSideConsistency(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, CTX);

        assertTrue(!report.isConsistent(), "finance 多核销应返回 consistent=false");
        DualSideDiffRow row = report.getRows().get(0);
        assertEquals(DualSideConsistencyChecker.STATUS_INCONSISTENT, row.getStatus());
        assertEquals(0, row.getFinanceSettled().compareTo(new BigDecimal("90")));
        assertEquals(0, row.getDomainSettled().compareTo(new BigDecimal("50")));
        assertEquals(0, row.getDiff().compareTo(new BigDecimal("40")));
    }

    @Test
    public void testInconsistentWhenDomainSettledMore() {
        long partnerId = 3300L;
        // finance settled = 30；域级 paidAmount = 70 → 域级多核销 40 → INCONSISTENT
        seedArApInvoice(partnerId, "AP-3300", new BigDecimal("100"), new BigDecimal("30"));
        seedPurInvoice(partnerId, "AP-3300", new BigDecimal("100"), new BigDecimal("70"));

        DualSideDiffReport report = reconciliationBiz.checkDualSideConsistency(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, CTX);

        assertTrue(!report.isConsistent(), "域级多核销应返回 consistent=false");
        DualSideDiffRow row = report.getRows().get(0);
        assertEquals(0, row.getDiff().compareTo(new BigDecimal("40")));
    }

    @Test
    public void testPartnerLevelReportCorrect() {
        long partnerA = 3400L;
        long partnerB = 3401L;
        // A 一致；B 不一致
        seedArApInvoice(partnerA, "AP-3400", new BigDecimal("100"), new BigDecimal("50"));
        seedPurInvoice(partnerA, "AP-3400", new BigDecimal("100"), new BigDecimal("50"));
        seedArApInvoice(partnerB, "AP-3401", new BigDecimal("100"), new BigDecimal("80"));
        seedPurInvoice(partnerB, "AP-3401", new BigDecimal("100"), new BigDecimal("20"));

        DualSideDiffReport report = reconciliationBiz.checkDualSideConsistency(
                ErpFinConstants.DIRECTION_PAYABLE, null, CTX);

        assertEquals(2, report.getRows().size(), "应包含两个 partner 的差异行");
        assertTrue(report.getRows().stream().anyMatch(r -> Long.valueOf(partnerA).equals(r.getPartnerId())
                && DualSideConsistencyChecker.STATUS_CONSISTENT.equals(r.getStatus())));
        assertTrue(report.getRows().stream().anyMatch(r -> Long.valueOf(partnerB).equals(r.getPartnerId())
                && DualSideConsistencyChecker.STATUS_INCONSISTENT.equals(r.getStatus())));
    }

    // ---------- helpers ----------

    private void seedArApInvoice(long partnerId, String code, BigDecimal amount, BigDecimal settled) {
        ormTemplate.runInSession(() -> {
            seedPartner(partnerId);
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            ErpFinArApItem it = dao.newEntity();
            it.setCode("ARI-" + code);
            it.setOrgId(1L);
            it.setAcctSchemaId(1L);
            it.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
            it.setPartnerId(partnerId);
            it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AP_INVOICE);
            it.setSourceBillCode(code);
            it.setBusinessDate(LocalDate.of(2026, 5, 20));
            it.setCurrencyId(1L);
            it.setExchangeRate(BigDecimal.ONE);
            it.setAmountSource(amount);
            it.setAmountFunctional(amount);
            it.setSettledAmountSource(settled);
            it.setSettledAmountFunctional(settled);
            it.setOpenAmountSource(amount.subtract(settled));
            it.setOpenAmountFunctional(amount.subtract(settled));
            it.setStatus(settled.compareTo(BigDecimal.ZERO) == 0
                    ? ErpFinConstants.AR_AP_STATUS_OPEN : ErpFinConstants.AR_AP_STATUS_PARTIAL);
            dao.saveEntity(it);
        });
    }

    private void seedPurInvoice(long partnerId, String code, BigDecimal amountFunctional, BigDecimal paidAmount) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
            ErpPurInvoice inv = dao.newEntity();
            inv.setCode(code);
            inv.setSupplierId(partnerId);
            inv.setBusinessDate(LocalDate.of(2026, 5, 20));
            inv.setCurrencyId(1L);
            inv.setExchangeRate(BigDecimal.ONE);
            inv.setAmountSource(amountFunctional);
            inv.setAmountFunctional(amountFunctional);
            inv.setPaidAmount(paidAmount);
            inv.setDocStatus("APPROVED");
            inv.setApproveStatus("APPROVED");
            dao.saveEntity(inv);
        });
    }

    private void seedPartner(long partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner partner = new ErpMdPartner();
        partner.orm_propValue(1, partnerId);
        partner.setCode("P-" + partnerId);
        partner.setName("Partner " + partnerId);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        partner.setReceivableBalance(BigDecimal.ZERO);
        partner.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(partner);
    }
}
