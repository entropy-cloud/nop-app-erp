package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinConsolidationEliminationBiz;
import app.erp.fin.biz.IErpFinIntercompanyMatchBiz;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.entity.ErpFinConsolidationElimination;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A3 公司间配对 + 合并抵消候选识别测试（plan 2026-07-22-1000-1 §Phase 3 Proof）。
 *
 * <p>覆盖 runMatching + checkDualSideConsistency + generateEliminationCandidates + postElimination。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:intercompany-test.yaml")
public class TestErpFinIntercompanyMatchingAndElimination extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinIntercompanyMatchBiz matchBiz;
    @Inject
    IErpFinConsolidationEliminationBiz eliminationBiz;

    @Test
    public void testRunMatchingIdentifiesMatchedPairs() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-MATCH-1", 2026, 6);
            // 模拟跨公司配对凭证：SALE 凭证（orgId=1）+ PURCHASE 凭证（orgId=2），同一 billCode 配对，金额一致
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("1000"), "TRANSFER-PAIR-1");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("1000"), "TRANSFER-PAIR-1");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        int count = ormTemplate.runInSession(session ->
                matchBiz.runMatching(periodId, CTX));

        assertTrue(count > 0, "应识别至少 1 条配对记录");

        // 验证 MATCHED 记录（金额一致）
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("status", ErpFinConstants.INTERCOMPANY_MATCH_MATCHED));
        List<ErpFinIntercompanyMatch> matched =
                daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);
        assertTrue(!matched.isEmpty(), "应存在 MATCHED 配对记录");
    }

    @Test
    public void testRunMatchingIdentifiesDiffPairs() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-MATCH-2", 2026, 7);
            // 模拟金额不一致的配对：SALE=1000 vs PURCHASE=800 → DIFF，同一 billCode
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("1000"), "TRANSFER-PAIR-2");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("800"), "TRANSFER-PAIR-2");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        int count = ormTemplate.runInSession(session ->
                matchBiz.runMatching(periodId, CTX));
        assertTrue(count > 0);

        // 验证 DIFF 记录（差额 200）
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("status", ErpFinConstants.INTERCOMPANY_MATCH_DIFF));
        List<ErpFinIntercompanyMatch> diffRecords =
                daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);
        boolean hasDiff = diffRecords.stream().anyMatch(m ->
                m.getDiffAmount() != null && m.getDiffAmount().compareTo(new BigDecimal("200")) == 0);
        assertTrue(hasDiff, "应存在 DIFF=200 的配对记录");
    }

    @Test
    public void testCheckDualSideConsistencyReturnsReport() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-MATCH-3", 2026, 8);
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("500"), "TRANSFER-PAIR-3");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));

        DualSideDiffReport report = ormTemplate.runInSession(session ->
                matchBiz.checkDualSideConsistency("TRANSFER-PAIR-3", periodId, CTX));
        assertNotNull(report, "应返回非空 DiffReport");
    }

    @Test
    public void testGenerateEliminationCandidatesProducesCandidates() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-ELIM-1", 2026, 9);
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("3000"), "TRANSFER-PAIR-4");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("3000"), "TRANSFER-PAIR-4");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        // 先配对
        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));

        // 生成抵消候选
        int candidateCount = ormTemplate.runInSession(session ->
                eliminationBiz.generateEliminationCandidates(periodId, CTX));
        assertTrue(candidateCount > 0, "应识别抵消候选");

        // 验证 AR_AP + REVENUE_COST 两类候选存在
        QueryBean arApQ = new QueryBean();
        arApQ.addFilter(eq("periodId", periodId));
        arApQ.addFilter(eq("eliminationType", ErpFinConstants.ELIMINATION_TYPE_AR_AP));
        arApQ.addFilter(eq("status", ErpFinConstants.ELIMINATION_STATUS_CANDIDATE));
        List<ErpFinConsolidationElimination> arApCandidates =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).findAllByQuery(arApQ);
        assertTrue(!arApCandidates.isEmpty(), "应存在 AR_AP CANDIDATE");

        QueryBean rcQ = new QueryBean();
        rcQ.addFilter(eq("periodId", periodId));
        rcQ.addFilter(eq("eliminationType", ErpFinConstants.ELIMINATION_TYPE_REVENUE_COST));
        List<ErpFinConsolidationElimination> rcCandidates =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).findAllByQuery(rcQ);
        assertTrue(!rcCandidates.isEmpty(), "应存在 REVENUE_COST CANDIDATE");
    }

    @Test
    public void testPostEliminationGeneratesDraftVoucher() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-ELIM-2", 2026, 10);
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("2000"), "TRANSFER-PAIR-5");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("2000"), "TRANSFER-PAIR-5");
            seedSubject("1131", "内部应收");
            seedSubject("2202", "内部应付");
            seedSubject("5001", "内部收入");
            seedSubject("1401", "内部成本");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));
        ormTemplate.runInSession(session -> eliminationBiz.generateEliminationCandidates(periodId, CTX));

        // 取第一条 CANDIDATE
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("status", ErpFinConstants.ELIMINATION_STATUS_CANDIDATE));
        q.setLimit(1);
        List<ErpFinConsolidationElimination> candidates =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).findAllByQuery(q);
        ErpFinConsolidationElimination candidate = candidates.get(0);

        Long voucherId = ormTemplate.runInSession(session ->
                eliminationBiz.postElimination(candidate.getId(), CTX));
        assertNotNull(voucherId, "应生成草稿抵消凭证");

        // 验证凭证为 DRAFT 状态
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(ErpFinConstants.VOUCHER_STATUS_DRAFT, voucher.getDocStatus(),
                "抵消分录凭证应为 DRAFT 状态");

        // 验证候选状态翻转为 DRAFT_VOUCHER
        ErpFinConsolidationElimination updated =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).getEntityById(candidate.getId());
        assertEquals(ErpFinConstants.ELIMINATION_STATUS_DRAFT_VOUCHER, updated.getStatus(),
                "候选状态应翻转为 DRAFT_VOUCHER");
    }

    // ---------- P1-MA2-097/098 增强验证 ----------

    /** P1-MA2-097：runMatching 填充 5 审计列（arOrgId/apOrgId/arSideVoucherId/apSideVoucherId/materialId）。 */
    @Test
    public void testRunMatchingFillsAuditColumns() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-AUDIT-1", 2026, 11);
            seedIntercompanyVoucherWithMaterial(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("1000"), "AUDIT-PAIR-1", 7777L);
            seedIntercompanyVoucherWithMaterial(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("1000"), "AUDIT-PAIR-1", 7777L);
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));

        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("pairKey", "AUDIT-PAIR-1"));
        List<ErpFinIntercompanyMatch> records =
                daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);
        assertEquals(1, records.size(), "应识别 1 条配对记录");
        ErpFinIntercompanyMatch m = records.get(0);
        assertEquals(1L, m.getArOrgId(), "arOrgId = SALE 凭证组织 1");
        assertEquals(2L, m.getApOrgId(), "apOrgId = PURCHASE 凭证组织 2");
        assertNotNull(m.getArSideVoucherId(), "arSideVoucherId 已填充");
        assertNotNull(m.getApSideVoucherId(), "apSideVoucherId 已填充");
        assertEquals(7777L, m.getMaterialId(), "materialId 从凭证行反查填充");
        assertEquals(1L, m.getOrgId(), "配对记录归属 AR 侧组织（移除 hardcoded 1L 后按 arOrgId）");
    }

    /** P1-MA2-098：重复 runMatching 幂等（前置去重，无重复 Match 行）。 */
    @Test
    public void testRunMatchingIsIdempotent() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-IDEM-1", 2026, 12);
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 1L, periodId,
                    new BigDecimal("1000"), "IDEM-PAIR-1");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 2L, periodId,
                    new BigDecimal("1000"), "IDEM-PAIR-1");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        int first = ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));
        int second = ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));

        assertEquals(1, first, "首次配对识别 1 条");
        assertEquals(0, second, "二次 runMatching 幂等 skip（前置去重）");

        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("pairKey", "IDEM-PAIR-1"));
        List<ErpFinIntercompanyMatch> records =
                daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);
        assertEquals(1, records.size(), "重复 runMatching 不产生重复 Match 行");
    }

    /** P1-MA2-097：抵消候选设置 fromOrgId/toOrgId，草稿凭证 orgId per-pair。 */
    @Test
    public void testEliminationOrgPerPair() {
        Long[] ids = seedReturn(() -> {
            Long periodId = seedOpenPeriod("2026-ORG-1", 2027, 1);
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, 3L, periodId,
                    new BigDecimal("500"), "ORG-PAIR-1");
            seedIntercompanyVoucher(ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, 4L, periodId,
                    new BigDecimal("500"), "ORG-PAIR-1");
            seedSubject("1131", "内部应收");
            seedSubject("2202", "内部应付");
            return new Long[]{periodId};
        });
        Long periodId = ids[0];

        ormTemplate.runInSession(session -> matchBiz.runMatching(periodId, CTX));
        ormTemplate.runInSession(session -> eliminationBiz.generateEliminationCandidates(periodId, CTX));

        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("eliminationType", ErpFinConstants.ELIMINATION_TYPE_AR_AP));
        q.setLimit(1);
        ErpFinConsolidationElimination candidate =
                daoProvider.daoFor(ErpFinConsolidationElimination.class).findAllByQuery(q).get(0);
        assertEquals(3L, candidate.getFromOrgId(), "fromOrgId = AR 侧组织 3");
        assertEquals(4L, candidate.getToOrgId(), "toOrgId = AP 侧组织 4");
        assertEquals(3L, candidate.getOrgId(), "候选归属 AR 侧组织 3（移除 hardcoded 1L）");

        Long voucherId = ormTemplate.runInSession(session ->
                eliminationBiz.postElimination(candidate.getId(), CTX));
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(3L, voucher.getOrgId(), "草稿抵消凭证 orgId per-pair = 候选组织 3");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod p = new app.erp.fin.dao.entity.ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(java.time.LocalDate.of(year, month, 1));
        p.setEndDate(java.time.LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedIntercompanyVoucher(String billType, Long orgId, Long periodId, BigDecimal amount,
                                         String billCode) {
        seedIntercompanyVoucherWithMaterial(billType, orgId, periodId, amount, billCode, null);
    }

    private void seedIntercompanyVoucherWithMaterial(String billType, Long orgId, Long periodId, BigDecimal amount,
                                                     String billCode, Long materialId) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherBillR> billRDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherBillR.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode("IC-TEST-" + billType + "-" + orgId + "-" + periodId + "-" + billCode);
        voucher.setVoucherType("TRANSFER");
        voucher.setVoucherDate(io.nop.api.core.time.CoreMetrics.today());
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(1L);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(amount);
        voucher.setTotalCredit(amount);
        voucher.setIsReversed(false);
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucherDao.saveEntity(voucher);

        // 凭证行（携带 materialId 供配对审计列反查；仅 material 维度场景创建）
        if (materialId != null) {
            IEntityDao<app.erp.md.dao.entity.ErpMdSubject> subjDao =
                    daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
            app.erp.md.dao.entity.ErpMdSubject subj = subjDao.newEntity();
            subj.setCode("1131-" + orgId);
            subj.setName("内部应收");
            subj.setSubjectClass(ErpFinConstants.SUBJECT_CLASS_EXPENSE);
            subj.setDirection(ErpFinConstants.DC_DEBIT);
            subj.setStatus("ACTIVE");
            subjDao.saveEntity(subj);

            IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> lineDao =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
            app.erp.fin.dao.entity.ErpFinVoucherLine line = lineDao.newEntity();
            line.setVoucherId(voucher.getId());
            line.setLineNo(1);
            line.setSubjectId(subj.getId());
            line.setSubjectCode("1131");
            line.setDcDirection(ErpFinConstants.DC_DEBIT);
            line.setDebitAmount(amount);
            line.setCreditAmount(BigDecimal.ZERO);
            line.setCurrencyId(1L);
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountFunctional(amount);
            line.setAcctSchemaId(1L);
            line.setOrgId(orgId);
            line.setMaterialId(materialId);
            lineDao.saveEntity(line);
        }

        app.erp.fin.dao.entity.ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucher.getId());
        billR.setBillType(billType);
        billR.setBillCode(billCode);
        billR.setBusinessType(billType);
        billRDao.saveEntity(billR);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        app.erp.md.dao.entity.ErpMdSubject s = new app.erp.md.dao.entity.ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(ErpFinConstants.SUBJECT_CLASS_EXPENSE);
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }
}
