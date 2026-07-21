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
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CM-1", 2024, 6);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new Long[]{pid, subject.getId()};
        });
        Long periodId = ids[0];
        Long subjectId = ids[1];

        Long voucherId = ormTemplate.runInSession(session ->
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
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CM-2", 2024, 7);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new Long[]{pid, subject.getId()};
        });
        Long periodId = ids[0];
        Long subjectId = ids[1];

        // 先 commit
        Long originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-002",
                        subjectId, null, periodId, new BigDecimal("300"), CTX));
        assertNotNull(originalId);

        // release（release-on-cancel 路径，与 release-on-invoice-approve 共用 SPI.release）
        Long reversalId = ormTemplate.runInSession(session ->
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
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CM-3", 2024, 8);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new Long[]{pid, subject.getId()};
        });
        Long periodId = ids[0];
        Long subjectId = ids[1];

        Long originalId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-003",
                        subjectId, null, periodId, new BigDecimal("700"), CTX));
        assertNotNull(originalId);

        // release（release-on-invoice-approve 路径，SPI 入口相同）
        Long reversalId = ormTemplate.runInSession(session ->
                commitmentBiz.release(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-CM-003", CTX));
        assertNotNull(reversalId);

        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalId);
        assertTrue(Boolean.TRUE.equals(original.getIsReversed()),
                "invoice-approve release 应红冲原凭证");
    }

    @Test
    public void testDoubleReleaseThrowsGuard() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CM-4", 2024, 9);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new Long[]{pid, subject.getId()};
        });
        Long periodId = ids[0];
        Long subjectId = ids[1];

        Long originalId = ormTemplate.runInSession(session ->
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

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
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
