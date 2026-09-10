package app.erp.fin.service.intercompany;

import app.erp.fin.biz.IErpFinConsolidationEliminationBiz;
import app.erp.fin.biz.IErpFinIntercompanyMatchBiz;
import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.entity.ErpFinConsolidationElimination;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-fin4-024-r3 失败测试：intercompany 配对凭证与合并抵销凭证通道的
 * currencyId / acctSchemaId 维度解析断言（plan 2026-09-10-0705-1 Phase 1）。
 *
 * <p>非恒等场景：≥2 账套 + 非 "1" 本位币 —— 断言产出凭证头 acctSchemaId 与凭证行
 * currencyId/acctSchemaId 为账套解析值而非硬编码 "1"。恒等控制组：无账套回退 "1"
 * （既有行为不变，恒等部署逐字节等价）。
 *
 * <p>config 门控经测试域 {@code intercompany-test.yaml} 启用，生产默认值零触碰。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:intercompany-test.yaml")
public class TestErpFinIntercompanyEliminationDimension extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinIntercompanyTransferBiz intercompanyTransferBiz;
    @Inject
    IErpFinTransferPriceResolver transferPriceResolver;
    @Inject
    IErpFinIntercompanyMatchBiz matchBiz;
    @Inject
    IErpFinConsolidationEliminationBiz eliminationBiz;

    // ---------- 非恒等场景（先红：现硬编码 "1" → 解析值断言失败） ----------

    @Test
    public void testTransferChannelResolvesDimensionsInNonIdentityDeployment() {
        String[] fixture = seedNonIdentityFixture("DIM-TR", true, true);
        transferPriceResolver.invalidateCache();
        String whAId = fixture[2];
        String whBId = fixture[3];
        String schemaAId = fixture[6];
        String schemaBId = fixture[7];
        String usdId = fixture[8];

        Map<String, BigDecimal> qtyByMaterial = new HashMap<>();
        qtyByMaterial.put("MAT-DIM-001", new BigDecimal("10"));

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed("DIM-1", whAId, whBId,
                        qtyByMaterial, LocalDate.of(2026, 8, 15), CTX));

        assertEquals(2, voucherIds.size(), "跨法人调拨应生成 2 条配对凭证（AR + AP）");
        assertVoucherDimensions(voucherIds.get(0), schemaAId, usdId, "AR（调出法人）");
        assertVoucherDimensions(voucherIds.get(1), schemaBId, usdId, "AP（调入法人）");
    }

    @Test
    public void testTradeDocumentChannelResolvesDimensionsInNonIdentityDeployment() {
        String[] fixture = seedNonIdentityFixture("DIM-PO", true, true);
        transferPriceResolver.invalidateCache();
        String buyerId = fixture[1];
        String schemaAId = fixture[6];
        String schemaBId = fixture[7];
        String usdId = fixture[8];

        // PO 执行方=买方（orgB），对手=卖方（orgA）：AR 凭证落卖方账套，AP 凭证落买方账套
        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTradeDocumentApproved(
                        ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, "DIM-2", "PO-DIM-1",
                        buyerId, new BigDecimal("800"), LocalDate.of(2026, 8, 16), CTX));

        assertEquals(2, voucherIds.size(), "跨法人 PO approve 应生成 2 条配对凭证（AR + AP）");
        assertVoucherDimensions(voucherIds.get(0), schemaAId, usdId, "AR（卖方法人）");
        assertVoucherDimensions(voucherIds.get(1), schemaBId, usdId, "AP（买方法人）");
    }

    @Test
    public void testEliminationChannelResolvesDimensionsInNonIdentityDeployment() {
        String[] fixture = seedNonIdentityFixture("DIM-EL", true, true);
        String orgAId = fixture[0];
        String orgBId = fixture[1];
        String periodId = fixture[5];
        String schemaAId = fixture[6];
        String usdId = fixture[8];

        // 抵消候选链：配对凭证 orgId = orgA/orgB（候选归属 AR 侧 orgA）
        seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, orgAId,
                periodId, new BigDecimal("600"), "DIM-PAIR-1", schemaAId, usdId);
        seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, orgBId,
                periodId, new BigDecimal("600"), "DIM-PAIR-1", schemaAId, usdId);

        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));
        ormTemplate.runInSession(session -> eliminationBiz.generateEliminationCandidates(periodId, CTX));

        ErpFinConsolidationElimination candidate = firstCandidate(periodId);
        assertEquals(orgAId, candidate.getOrgId(), "候选归属 AR 侧组织 orgA");

        String voucherId = ormTemplate.runInSession(session ->
                eliminationBiz.postElimination(candidate.getId(), CTX));

        assertVoucherDimensions(voucherId, schemaAId, usdId, "抵销 DRAFT 凭证（候选组织 orgA）");
    }

    // ---------- 恒等控制组（无账套 → 回退 "1"，既有行为不变） ----------

    @Test
    public void testIdentityDeploymentKeepsLegacyDimensions() {
        String[] fixture = seedNonIdentityFixture("DIM-ID", false, false);
        transferPriceResolver.invalidateCache();
        String whAId = fixture[2];
        String whBId = fixture[3];
        String periodId = fixture[5];
        String orgAId = fixture[0];
        String orgBId = fixture[1];

        List<String> voucherIds = ormTemplate.runInSession(session ->
                intercompanyTransferBiz.onTransferConfirmed("DIM-3", whAId, whBId,
                        LocalDate.of(2026, 8, 15), CTX));
        assertEquals(2, voucherIds.size(), "恒等场景仍应生成 2 条配对凭证");
        assertVoucherDimensions(voucherIds.get(0), "1", "1", "恒等 AR");
        assertVoucherDimensions(voucherIds.get(1), "1", "1", "恒等 AP");

        // 恒等抵销链：无账套 → 抵销凭证维度回退 "1"
        seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, orgAId,
                periodId, new BigDecimal("400"), "DIM-PAIR-2", "1", "1");
        seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, orgBId,
                periodId, new BigDecimal("400"), "DIM-PAIR-2", "1", "1");
        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));
        ormTemplate.runInSession(session -> eliminationBiz.generateEliminationCandidates(periodId, CTX));

        ErpFinConsolidationElimination candidate = firstCandidate(periodId);
        String voucherId = ormTemplate.runInSession(session ->
                eliminationBiz.postElimination(candidate.getId(), CTX));
        assertVoucherDimensions(voucherId, "1", "1", "恒等抵销 DRAFT 凭证");
    }

    // ---------- 断言辅助 ----------

    /** 凭证头 acctSchemaId + 凭证行 currencyId/acctSchemaId 三元组断言（可观察产出实值）。 */
    private void assertVoucherDimensions(String voucherId, String expectedSchemaId, String expectedCurrencyId,
                                         String label) {
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(expectedSchemaId, voucher.getAcctSchemaId(),
                label + " 凭证头 acctSchemaId 应为账套解析值 " + expectedSchemaId + "（现硬编码 \"1\"）");

        QueryBean lineQ = new QueryBean();
        lineQ.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lineQ);
        assertEquals(2, lines.size(), label + " 凭证应有借/贷 2 行");
        for (ErpFinVoucherLine line : lines) {
            assertEquals(expectedCurrencyId, line.getCurrencyId(),
                    label + " 凭证行 currencyId 应为本位币解析值 " + expectedCurrencyId + "（现硬编码 \"1\"）");
            assertEquals(expectedSchemaId, line.getAcctSchemaId(),
                    label + " 凭证行 acctSchemaId 应为账套解析值 " + expectedSchemaId + "（现硬编码 \"1\"）");
        }
    }

    private ErpFinConsolidationElimination firstCandidate(String periodId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("eliminationType", ErpFinConstants.ELIMINATION_TYPE_AR_AP));
        q.addFilter(eq("status", ErpFinConstants.ELIMINATION_STATUS_CANDIDATE));
        List<ErpFinConsolidationElimination> candidates =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).findAllByQuery(q);
        assertTrue(!candidates.isEmpty(), "应存在 AR_AP CANDIDATE");
        return candidates.get(0);
    }

    // ---------- fixture ----------

    /**
     * 非恒等 fixture：currency USD（非 "1" 本位币）+ orgA/orgB 各挂 FINANCIAL ACTIVE 账套
     * （functionalCurrencyId=USD）。withSchemas=false 时为恒等控制组（无账套 → 解析回退 "1"）。
     *
     * 返回 {orgAId, orgBId, whAId, whBId, pricingRuleId, periodId, schemaAId, schemaBId, usdId}；
     * 无账套时 schemaAId/schemaBId 为 null。
     */
    private String[] seedNonIdentityFixture(String tag, boolean seedCurrency, boolean withSchemas) {
        return ormTemplate.runInSession(session -> {
            String usdId = null;
            if (seedCurrency) {
                usdId = seedCurrency("USD-" + tag, "美元-" + tag);
            }

            ErpMdOrganization orgA = seedOrganization("ORG-" + tag + "-A", "法人A-" + tag,
                    ErpFinConstants.ORG_TYPE_COMPANY, null);
            ErpMdOrganization orgB = seedOrganization("ORG-" + tag + "-B", "法人B-" + tag,
                    ErpFinConstants.ORG_TYPE_COMPANY, null);

            String schemaAId = null;
            String schemaBId = null;
            if (withSchemas) {
                schemaAId = seedAcctSchema("AS-" + tag + "-A", "账套A-" + tag, orgA.getId(), usdId);
                schemaBId = seedAcctSchema("AS-" + tag + "-B", "账套B-" + tag, orgB.getId(), usdId);
            }

            ErpMdWarehouse whA = seedWarehouse("WH-" + tag + "-A", "仓库A-" + tag, orgA.getId());
            ErpMdWarehouse whB = seedWarehouse("WH-" + tag + "-B", "仓库B-" + tag, orgB.getId());

            seedPricingRule("TP-" + tag + "-1", orgA.getId(), orgB.getId());

            seedSubject("1131", "内部应收");
            seedSubject("2202", "内部应付");
            seedSubject("5001", "内部收入");
            seedSubject("1401", "内部成本");

            String periodId = seedOpenPeriod("2026-" + tag, 2026, 8);

            return new String[]{orgA.getId(), orgB.getId(), whA.getId(), whB.getId(),
                    "TP-" + tag + "-1", periodId, schemaAId, schemaBId, usdId};
        });
    }

    private String seedCurrency(String code, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.setCode(code);
        c.setName(name);
        c.setIsFunctional(true);
        dao.saveEntity(c);
        return c.getId();
    }

    private String seedAcctSchema(String code, String name, String orgId, String functionalCurrencyId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema s = dao.newEntity();
        s.setCode(code);
        s.setName(name);
        s.setOrgId(orgId);
        s.setNature("FINANCIAL");
        s.setFunctionalCurrencyId(functionalCurrencyId);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s.getId();
    }

    private ErpMdOrganization seedOrganization(String code, String name, String orgType, String parentId) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization org = dao.newEntity();
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
        ErpMdWarehouse wh = dao.newEntity();
        wh.setCode(code);
        wh.setName(name);
        wh.setOrgId(orgId);
        wh.setStatus("ACTIVE");
        dao.saveEntity(wh);
        return wh;
    }

    private void seedPricingRule(String code, String fromOrgId, String toOrgId) {
        IEntityDao<ErpFinIntercompanyTransferPrice> dao =
                daoProvider.daoFor(ErpFinIntercompanyTransferPrice.class);
        ErpFinIntercompanyTransferPrice rule = dao.newEntity();
        rule.setCode(code);
        rule.setName("测试定价规则-" + code);
        rule.setOrgId("1");
        rule.setFromOrgId(fromOrgId);
        rule.setToOrgId(toOrgId);
        rule.setPricingMethod(ErpFinConstants.TRANSFER_PRICING_NEGOTIATED);
        rule.setFixedPrice(new BigDecimal("150"));
        rule.setIsActive(true);
        dao.saveEntity(rule);
    }

    private ErpMdSubject findSubjectByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = daoProvider.daoFor(ErpMdSubject.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(ErpFinConstants.SUBJECT_CLASS_EXPENSE);
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private String seedOpenPeriod(String code, int year, int month) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod p = dao.newEntity();
        p.setCode(code);
        p.setName(code);
        p.setOrgId("1");
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedIntercompanyVoucher(String billType, String orgId, String periodId, BigDecimal amount,
                                         String billCode, String acctSchemaId, String currencyId) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode("IC-DIM-" + billType + "-" + orgId + "-" + billCode);
        voucher.setVoucherType("TRANSFER");
        voucher.setVoucherDate(io.nop.api.core.time.CoreMetrics.today());
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(amount);
        voucher.setTotalCredit(amount);
        voucher.setIsReversed(false);
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucherDao.saveEntity(voucher);

        ErpFinVoucherLine line = lineDao.newEntity();
        line.setVoucherId(voucher.getId());
        line.setLineNo(1);
        ErpMdSubject subject = findSubjectByCode("1131");
        if (subject != null) {
            line.setSubjectId(subject.getId());
        }
        line.setSubjectCode("1131");
        line.setDcDirection(ErpFinConstants.DC_DEBIT);
        line.setDebitAmount(amount);
        line.setCreditAmount(BigDecimal.ZERO);
        line.setCurrencyId(currencyId);
        line.setExchangeRate(BigDecimal.ONE);
        line.setAmountFunctional(amount);
        line.setAcctSchemaId(acctSchemaId);
        line.setOrgId(orgId);
        lineDao.saveEntity(line);

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucher.getId());
        billR.setBillType(billType);
        billR.setBillCode(billCode);
        billR.setBusinessType(billType);
        billRDao.saveEntity(billR);
    }
}
