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
        String[] ids = seedReturn(() -> {
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
            return new String[]{companyA.getId(), companyB.getId(), whA.getId(), whB.getId()};
        });
        transferPriceResolver.invalidateCache();
        String companyAId = ids[0];
        String companyBId = ids[1];
        String whAId = ids[2];
        String whBId = ids[3];

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed("5001", whAId, whBId,
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
        String[] ids = seedReturn(() -> {
            ErpMdOrganization companyC = seedOrganization("ORG-CC", "公司C", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdWarehouse whC1 = seedWarehouse("WH-C1", "仓库C1", companyC.getId());
            ErpMdWarehouse whC2 = seedWarehouse("WH-C2", "仓库C2", companyC.getId());
            return new String[]{whC1.getId(), whC2.getId()};
        });

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed("5002", ids[0], ids[1],
                        LocalDate.of(2026, 7, 15), CTX));

        assertTrue(voucherIds.isEmpty(), "同法人调拨不应生成凭证");
    }

    // ---------- 跨公司 PO/SO trade-document 路径（plan 2026-07-24-1351-2）----------

    @Test
    public void testCrossLegalEntityPurchaseOrderGeneratesPairedVouchers() {
        String[] ids = seedReturn(() -> {
            ErpMdOrganization seller = seedOrganization("ORG-PO-S", "卖方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdOrganization buyer = seedOrganization("ORG-PO-B", "买方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            // 定价规则：fromOrg=seller(卖方) → toOrg=buyer(买方)，编码 intercompany 关系
            seedPricingRule(seller.getId(), buyer.getId());
            seedSubject("1131", "内部应收");
            seedSubject("5001", "内部销售收入");
            seedSubject("1401", "内部采购成本");
            seedSubject("2202", "内部应付");
            seedOpenPeriod("2026-IC-PO", 2026, 7);
            return new String[]{seller.getId(), buyer.getId()};
        });
        transferPriceResolver.invalidateCache();
        String sellerId = ids[0];
        String buyerId = ids[1];

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentApproved(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, "7001", "PO-TEST-1",
                        buyerId, new BigDecimal("1000"), LocalDate.of(2026, 7, 15), CTX));

        assertEquals(2, voucherIds.size(), "跨法人 PO approve 应生成 2 条配对凭证（AR + AP）");

        ErpFinVoucher arVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(0));
        assertEquals(sellerId, arVoucher.getOrgId(), "AR 凭证 orgId 应为卖方法人（对手方）");

        ErpFinVoucher apVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(1));
        assertEquals(buyerId, apVoucher.getOrgId(), "AP 凭证 orgId 应为买方法人（执行方）");

        assertIntercompanyBillR(voucherIds.get(0), ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, "PO-TEST-1");
        assertIntercompanyBillR(voucherIds.get(1), ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, "PO-TEST-1");
        assertVoucherBalanced(voucherIds.get(0), new BigDecimal("1000"));
    }

    @Test
    public void testCrossLegalEntitySalesOrderGeneratesPairedVouchers() {
        String[] ids = seedReturn(() -> {
            ErpMdOrganization seller = seedOrganization("ORG-SO-S", "卖方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdOrganization buyer = seedOrganization("ORG-SO-B", "买方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            seedPricingRule(seller.getId(), buyer.getId());
            seedSubject("1131", "内部应收");
            seedSubject("5001", "内部销售收入");
            seedSubject("1401", "内部采购成本");
            seedSubject("2202", "内部应付");
            seedOpenPeriod("2026-IC-SO", 2026, 7);
            return new String[]{seller.getId(), buyer.getId()};
        });
        transferPriceResolver.invalidateCache();
        String sellerId = ids[0];
        String buyerId = ids[1];

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentApproved(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_SALES_ORDER, "8001", "SO-TEST-1",
                        sellerId, new BigDecimal("2000"), LocalDate.of(2026, 7, 16), CTX));

        assertEquals(2, voucherIds.size(), "跨法人 SO approve 应生成 2 条配对凭证（AR + AP）");
        assertEquals(sellerId,
                daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(0)).getOrgId(),
                "AR 凭证 orgId 应为卖方法人（执行方）");
        assertEquals(buyerId,
                daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherIds.get(1)).getOrgId(),
                "AP 凭证 orgId 应为买方法人（对手方）");
        assertVoucherBalanced(voucherIds.get(0), new BigDecimal("2000"));
    }

    @Test
    public void testTradeDocumentReverseApproveRedLetters() {
        String[] ids = seedReturn(() -> {
            ErpMdOrganization seller = seedOrganization("ORG-RV-S", "卖方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdOrganization buyer = seedOrganization("ORG-RV-B", "买方法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            seedPricingRule(seller.getId(), buyer.getId());
            seedSubject("1131", "内部应收");
            seedSubject("5001", "内部销售收入");
            seedSubject("1401", "内部采购成本");
            seedSubject("2202", "内部应付");
            seedOpenPeriod("2026-IC-RV", 2026, 7);
            return new String[]{seller.getId(), buyer.getId()};
        });
        transferPriceResolver.invalidateCache();
        String buyerId = ids[1];

        String docCode = "PO-REV-1";
        List<String> originalIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentApproved(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, "9001", docCode,
                        buyerId, new BigDecimal("500"), LocalDate.of(2026, 7, 17), CTX));
        assertEquals(2, originalIds.size(), "前置：approve 应生成 2 条配对凭证");

        List<String> reversalIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentReversed(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, "9001", docCode, CTX));
        assertEquals(2, reversalIds.size(), "reverseApprove 应红冲 2 条配对凭证");

        for (String originalId : originalIds) {
            ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalId);
            assertTrue(Boolean.TRUE.equals(original.getIsReversed()), "原凭证应标记 isReversed=true");
        }
        for (String reversalId : reversalIds) {
            ErpFinVoucher reversal = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(reversalId);
            assertTrue(Boolean.TRUE.equals(reversal.getIsReversed()), "红冲凭证应 isReversed=true");
            assertTrue(reversal.getReversalOfVoucherId() != null, "红冲凭证应回链原凭证 reversalOfVoucherId");
        }
    }

    @Test
    public void testSameLegalEntityTradeDocumentNoVoucher() {
        String[] ids = seedReturn(() -> {
            ErpMdOrganization solo = seedOrganization("ORG-SOLO", "单法人", ErpFinConstants.ORG_TYPE_COMPANY, null);
            return new String[]{solo.getId()};
        });

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentApproved(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, "9101", "PO-SOLO-1",
                        ids[0], new BigDecimal("300"), LocalDate.of(2026, 7, 18), CTX));
        assertTrue(voucherIds.isEmpty(), "无定价规则（无对手方）的单法人订单不应生成 intercompany 凭证");
    }

    // ---------- trade-document 断言辅助 ----------

    private void assertIntercompanyBillR(String voucherId, String billType, String expectedBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("billType", billType));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        assertEquals(1, links.size(), "凭证应写 1 条 " + billType + " 业财回链");
        assertEquals(expectedBillCode, links.get(0).getBillCode(), "业财回链 billCode 应为订单 code");
    }

    private void assertVoucherBalanced(String voucherId, BigDecimal expectedAmount) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
        assertEquals(2, lines.size(), "凭证应有借/贷 2 行");
        BigDecimal totalDebit = lines.stream()
                .map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalDebit.compareTo(totalCredit), "凭证借贷应平衡");
        assertEquals(0, totalDebit.compareTo(expectedAmount), "凭证金额应为订单金额 " + expectedAmount);
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private ErpMdOrganization seedOrganization(String code, String name, String orgType, String parentId) {
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

    private ErpMdWarehouse seedWarehouse(String code, String name, String orgId) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse wh = new ErpMdWarehouse();
        wh.setCode(code);
        wh.setName(name);
        wh.setOrgId(orgId);
        wh.setStatus("ACTIVE");
        dao.saveEntity(wh);
        return wh;
    }

    private void seedPricingRule(String fromOrgId, String toOrgId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice.class);
        app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice rule =
                new app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice();
        rule.setCode("TP-TEST-" + fromOrgId + "-" + toOrgId);
        rule.setName("测试定价规则");
        rule.setOrgId("1");
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
        p.setOrgId("1");
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
    }
}
