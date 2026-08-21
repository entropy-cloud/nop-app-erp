package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2 承付占用/释放 SPI 测试（plan 2026-07-21-1206-2 §Phase 2 Proof）。
 *
 * <p>4 测试覆盖 IErpFinBudgetCommitmentBiz 生命周期（budget.md §承付会计 §承付占用/释放 SPI）：
 * <ul>
 *   <li>commit：生成 COMMITMENT 凭证（postingType=COMMITMENT + 业财回链 billType=PURCHASE_ORDER_COMMITMENT）</li>
 *   <li>release-on-cancel：红冲 COMMITMENT（原凭证 isReversed=true，红冲凭证 isReversed=true）</li>
 *   <li>release-on-invoice-approve：同 release 路径（与 cancel 共用 SPI.release）</li>
 *   <li>重复 release 守卫：抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-a2-test.yaml")
public class TestErpFinBudgetCommitment extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetCommitmentBiz commitmentBiz;

    @Test
    public void testCommitGeneratesCommitmentVoucher() {
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-1", 2024, 6);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        String voucherId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-001",
                        subjectId, null, periodId, new BigDecimal("500"), CTX));

        assertNotNull(voucherId, "应生成承付凭证 ID");

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(ErpFinConstants.POSTING_TYPE_COMMITMENT, voucher.getPostingType(),
                "凭证 postingType 应为 COMMITMENT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus());

        // 业财回链应记 billType=PURCHASE_ORDER_COMMITMENT, billCode=PO-CM-001
        QueryBean bq = new QueryBean();
        bq.addFilter(eq("voucherId", voucherId));
        bq.addFilter(eq("billType", ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(bq);
        assertEquals(1, links.size(), "应写 1 条业财回链");
        assertEquals("PO-CM-001", links.get(0).getBillCode());
    }

    @Test
    public void testReleaseOnCancelReversesCommitment() {
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-2", 2024, 7);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        // 先 commit
        String originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-002",
                        subjectId, null, periodId, new BigDecimal("300"), CTX));
        assertNotNull(originalId);

        // release（release-on-cancel 路径，与 release-on-invoice-approve 共用 SPI.release）
        String reversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-002", CTX));
        assertNotNull(reversalId, "应生成红冲凭证 ID");

        // 原凭证 isReversed=true
        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalId);
        assertEquals(Boolean.TRUE, original.getIsReversed(), "原承付凭证应 isReversed=true");

        // 红冲凭证也是 isReversed=true（避免参与余量聚合）
        ErpFinVoucher reversal = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(reversalId);
        assertEquals(Boolean.TRUE, reversal.getIsReversed(), "红冲凭证自身 isReversed=true");
        assertEquals(originalId, reversal.getReversalOfVoucherId(), "reversalOfVoucherId 指向原凭证");
    }

    @Test
    public void testReleaseOnInvoiceApproveReversesCommitment() {
        // 与 release-on-cancel 共用 SPI.release（事务边界裁决：均 SYNC 同事务）。
        // 此测试断言 invoice-approve 路径的 release 行为 = cancel 路径行为。
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-3", 2024, 8);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        String originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-003",
                        subjectId, null, periodId, new BigDecimal("700"), CTX));
        assertNotNull(originalId);

        // release（release-on-invoice-approve 路径，SPI 入口相同）
        String reversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-003", CTX));
        assertNotNull(reversalId);

        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "invoice-approve release 应红冲原凭证");
    }

    @Test
    public void testDoubleReleaseThrowsGuard() {
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-4", 2024, 9);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        String originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-004",
                        subjectId, null, periodId, new BigDecimal("200"), CTX));
        assertNotNull(originalId);

        // 第一次 release 成功
        ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-004", CTX));

        // 第二次 release 应抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED
        assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session ->
                        commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-004", CTX)),
                "重复 release 应抛 NopException(ERR_BUDGET_COMMITMENT_ALREADY_RELEASED)");
    }

    @Test
    public void testSalesCommitmentDispatchesSalesBillType() {
        // plan 2026-07-24-1351-3：sales 承付 sourceBillType=SALES_ORDER 派发 billType=SALES_ORDER_COMMITMENT，
        // 且与采购 billType=PURCHASE_ORDER_COMMITMENT lookup 不碰撞（同 billCode 不同 billType 隔离）。
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-5", 2024, 10);
            ErpMdSubject subject = seedSubject("6001", "销售承付收入预留科目",
                    ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        // 同一 billCode 同时存在采购承付与销售承付（验证 billType 隔离）
        ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "MIXED-001",
                        subjectId, null, periodId, new BigDecimal("100"), CTX));
        String salesVoucherId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_SALES_ORDER, "MIXED-001",
                        subjectId, null, periodId, new BigDecimal("200"), CTX));
        assertNotNull(salesVoucherId, "sales 承付应生成凭证");

        // 验证 sales 凭证 billType=SALES_ORDER_COMMITMENT
        QueryBean salesLink = new QueryBean();
        salesLink.addFilter(eq("voucherId", salesVoucherId));
        salesLink.addFilter(eq("billType", ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE_SALES));
        List<ErpFinVoucherBillR> salesLinks = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(salesLink);
        assertEquals(1, salesLinks.size(), "sales 承付应写 SALES_ORDER_COMMITMENT 回链");
        assertEquals("MIXED-001", salesLinks.get(0).getBillCode());

        // release sales 承付不应影响采购承付（billType 隔离）
        String reversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_SALES_ORDER, "MIXED-001", CTX));
        assertNotNull(reversalId, "sales 承付 release 应成功");

        ErpFinVoucher salesOriginal = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(salesVoucherId);
        assertEquals(Boolean.TRUE, salesOriginal.getIsReversed(), "sales 原凭证应 isReversed=true");

        // 采购承付仍可独立 release（未被 sales release 误红冲）
        String purReversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "MIXED-001", CTX));
        assertNotNull(purReversalId, "采购承付 release 应成功（billType 隔离未被 sales release 影响）");
    }

    @Test
    public void testMultiInvoiceFullReleaseProducesSingleReversal() {
        // P1-MA2-081 全额释放语义 + 多发票容错：一张 PO 多次部分开票时，首张发票 approve 全额释放承付，
        // 后续发票 approve 经容错守卫（ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）跳过——实际占用产生时全额释放，
        // 避免 actual + commitment 双重占用。断言：仅一张红冲凭证 + 第二张发票 release 抛守卫异常（被 Processor 容错吞掉）。
        String[] ids = seedReturn(() -> {
            String pid = seedOpenPeriod("2024-CM-6", 2024, 11);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{pid, subject.getId()};
        });
        String periodId = ids[0];
        String subjectId = ids[1];

        // PO approve → commit（全额）
        String originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-MULTI-INV",
                        subjectId, null, periodId, new BigDecimal("1000"), CTX));
        assertNotNull(originalId);

        // invoice1 approve → 全额 release（首张发票释放整张 PO 承付）
        String reversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-MULTI-INV", CTX));
        assertNotNull(reversalId, "首张发票 approve 应全额红冲承付");

        // 断言仅一张红冲凭证（全额释放，非按比例部分释放）
        assertEquals(1, countCommitmentReversals("PO-MULTI-INV"),
                "全额释放语义：仅一张承付红冲凭证（部分释放归 successor）");

        // invoice2 approve → 重复 release 抛守卫异常（Processor 经 try-catch 容错吞掉，不阻断业务流）
        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session ->
                        commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-MULTI-INV", CTX)),
                "第二张发票 approve 应抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED（容错守卫）");
        assertEquals(ErpFinErrors.ERR_BUDGET_COMMITMENT_ALREADY_RELEASED.getErrorCode(), ex.getErrorCode(),
                "应抛承付已释放守卫（invoice2 容错跳过）");

        // 红冲凭证数量不变（invoice2 容错未产生新红冲）
        assertEquals(1, countCommitmentReversals("PO-MULTI-INV"),
                "invoice2 容错跳过后红冲凭证数量不变");
    }

    private int countCommitmentReversals(String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("billType", ErpFinConstants.COMMITMENT_VOUCHER_BILL_TYPE));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        int reversals = 0;
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && Boolean.TRUE.equals(v.getIsReversed())
                    && v.getReversalOfVoucherId() != null
                    && ErpFinConstants.POSTING_TYPE_COMMITMENT.equals(v.getPostingType())) {
                reversals++;
            }
        }
        return reversals;
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private String seedOpenPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
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

    private ErpMdSubject seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }
}
