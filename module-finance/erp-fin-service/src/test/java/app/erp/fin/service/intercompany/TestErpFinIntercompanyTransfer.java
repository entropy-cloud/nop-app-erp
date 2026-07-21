package app.erp.fin.service.intercompany;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A3 跨法人内部交易凭证生成测试（plan 2026-07-22-1000-1 §Phase 2 Proof）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>跨法人调拨 → 生成配对内部销售/采购凭证（INTERCOMPANY_SALE + INTERCOMPANY_PURCHASE）</li>
 *   <li>同法人调拨 → 零凭证生成（既有行为不变）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:intercompany-test.yaml")
public class TestErpFinIntercompanyTransfer extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinIntercompanyTransferBiz intercompanyTransferBiz;
    @Inject
    IErpFinTransferPriceResolver transferPriceResolver;

    @Test
    public void testCrossLegalEntityGeneratesPairedVouchers() {
        Long[] ids = seedReturn(() -> {
            ErpMdOrganization companyA = seedOrganization("ORG-CA", "公司A", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdOrganization companyB = seedOrganization("ORG-CB", "公司B", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdWarehouse whA = seedWarehouse("WH-A", "仓库A", companyA.getId());
            ErpMdWarehouse whB = seedWarehouse("WH-B", "仓库B", companyB.getId());
            seedPricingRule(companyA.getId(), companyB.getId());
            seedSubject("1131", "内部应收");
            seedSubject("5001", "内部销售收入");
            seedSubject("1401", "内部采购成本");
            seedSubject("2202", "内部应付");
            seedOpenPeriod("2026-IC-7", 2026, 7);
            return new Long[]{companyA.getId(), companyB.getId(), whA.getId(), whB.getId()};
        });
        transferPriceResolver.invalidateCache();
        Long companyAId = ids[0];
        Long companyBId = ids[1];
        Long whAId = ids[2];
        Long whBId = ids[3];

        List<Long> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed(5001L, whAId, whBId,
                        LocalDate.of(2026, 7, 15), CTX));

        assertEquals(2, voucherIds.size(), "跨法人调拨应生成 2 条配对凭证（AR + AP）");

        // 验证 INTERCOMPANY_SALE 凭证（AR 侧）
        ErpFinVoucher arVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(0));
        assertEquals(companyAId, arVoucher.getOrgId(), "AR 凭证 orgId 应为调出方法人");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, arVoucher.getDocStatus());

        QueryBean arBillR = new QueryBean();
        arBillR.addFilter(eq("voucherId", voucherIds.get(0)));
        arBillR.addFilter(eq("billType", ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE));
        List<ErpFinVoucherBillR> arLinks = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(arBillR);
        assertEquals(1, arLinks.size(), "AR 凭证应写 1 条 INTERCOMPANY_SALE 业财回链");

        // 验证 AP 凭证 orgId 为调入方法人
        ErpFinVoucher apVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(1));
        assertEquals(companyBId, apVoucher.getOrgId(), "AP 凭证 orgId 应为调入方法人");

        // 验证凭证借贷平衡（Dr = Cr = 定价金额 150）
        QueryBean lineQ = new QueryBean();
        lineQ.addFilter(eq("voucherId", voucherIds.get(0)));
        List<ErpFinVoucherLine> arLines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lineQ);
        assertEquals(2, arLines.size(), "AR 凭证应有借/贷 2 行");
        BigDecimal totalDebit = arLines.stream()
                .map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = arLines.stream()
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalDebit.compareTo(totalCredit), "AR 凭证借贷应平衡");
        assertEquals(0, totalDebit.compareTo(new BigDecimal("150")), "AR 凭证金额应为定价 150");
    }

    @Test
    public void testSameLegalEntityNoVoucher() {
        Long[] ids = seedReturn(() -> {
            ErpMdOrganization companyC = seedOrganization("ORG-CC", "公司C", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdWarehouse whC1 = seedWarehouse("WH-C1", "仓库C1", companyC.getId());
            ErpMdWarehouse whC2 = seedWarehouse("WH-C2", "仓库C2", companyC.getId());
            return new Long[]{whC1.getId(), whC2.getId()};
        });

        List<Long> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed(5002L, ids[0], ids[1],
                        LocalDate.of(2026, 7, 15), CTX));

        assertTrue(voucherIds.isEmpty(), "同法人调拨不应生成凭证");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private ErpMdOrganization seedOrganization(String code, String name, String orgType, Long parentId) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization org = new ErpMdOrganization();
        org.setCode(code);
        org.setName(name);
        org.setOrgType(orgType);
        org.setParentId(parentId);
        org.setStatus("ACTIVE");
        dao.saveEntity(org);
        return org;
    }

    private ErpMdWarehouse seedWarehouse(String code, String name, Long orgId) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse wh = new ErpMdWarehouse();
        wh.setCode(code);
        wh.setName(name);
        wh.setOrgId(orgId);
        wh.setStatus("ACTIVE");
        dao.saveEntity(wh);
        return wh;
    }

    private void seedPricingRule(Long fromOrgId, Long toOrgId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice.class);
        app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice rule =
                new app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice();
        rule.setCode("TP-TEST-" + fromOrgId + "-" + toOrgId);
        rule.setName("测试定价规则");
        rule.setOrgId(1L);
        rule.setFromOrgId(fromOrgId);
        rule.setToOrgId(toOrgId);
        rule.setPricingMethod(ErpFinConstants.TRANSFER_PRICING_NEGOTIATED);
        rule.setFixedPrice(new BigDecimal("150"));
        rule.setIsActive(true);
        dao.saveEntity(rule);
    }

    private ErpMdSubject seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(ErpFinConstants.SUBJECT_CLASS_EXPENSE);
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private void seedOpenPeriod(String code, int year, int month) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod p = new app.erp.fin.dao.entity.ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
    }
}
