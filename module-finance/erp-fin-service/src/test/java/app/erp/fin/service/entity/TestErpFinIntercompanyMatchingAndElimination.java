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
