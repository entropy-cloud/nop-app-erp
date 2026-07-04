package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.biz.IErpFinBankStatementLineBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 余额调节表（{@link IErpFinBankReconciliationBiz}）集成测试（Phase 3）。
 *
 * <p>覆盖：平衡（diff=0, isBalanced=true）+ 不平衡抛错 ERR_BANK_RECON_NOT_BALANCED
 * + 未达账项调整凭证（BANK_RECON_ADJ）生成 + reverse 红冲 + 期间 CLOSED 拒绝生成。
 *
 * <p>平衡恒等式（简化承载，{@code BankReconciliationBuilder.generate}）：
 * {@code bookBalance = statementBalance + bankCreditUnrecorded - bankDebitUnrecorded}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinBankReconciliation extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBankStatementBiz bankStatementBiz;
    @Inject
    IErpFinBankStatementLineBiz bankStatementLineBiz;
    @Inject
    IErpFinBankReconciliationBiz bankReconciliationBiz;

    @Test
    public void testGenerateBalancedNoUnrecorded() {
        long seed = System.nanoTime();
        long subjectId = 9001L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
        });

        // 账户余额 1000，对账单 1 笔 0 元行（endingBalance=currentBalance=1000），无未达 → 平衡
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-BAL-" + seed,
                DC_CREDIT, new BigDecimal("0"));
        ErpFinBankStatement head = bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX);

        ErpFinBankReconciliation recon = bankReconciliationBiz.generate(head.getId(), CTX);
        assertTrue(recon.getIsBalanced(), "应平衡");
        assertEquals(0, recon.getUnreconciledDiff().compareTo(BigDecimal.ZERO));
        assertEquals(ErpFinConstants.VOUCHER_STATUS_DRAFT, recon.getDocStatus());
    }

    @Test
    public void testGenerateUnbalancedRejected() {
        long seed = System.nanoTime();
        long subjectId = 9002L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
        });

        // 账户余额 1000；对账单 1 笔 CREDIT 500 未达，未传 balanceAfter → endingBalance=1000
        // 恒等式：(1000 - 1000) - (500 - 0) = -500 ≠ 0 → 不平衡
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-UNBAL-" + seed,
                DC_CREDIT, new BigDecimal("500"));
        ErpFinBankStatement head = bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX);

        assertThrows(NopException.class, () -> bankReconciliationBiz.generate(head.getId(), CTX),
                "未达账项不平衡应拒绝生成");
    }

    @Test
    public void testPostNoAdjustmentVoucherWhenNoUnrecorded() {
        long seed = System.nanoTime();
        long subjectId = 9003L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-NOADJ-" + seed,
                DC_CREDIT, new BigDecimal("0"));
        ErpFinBankStatement head = bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX);

        ErpFinBankReconciliation recon = bankReconciliationBiz.generate(head.getId(), CTX);
        bankReconciliationBiz.post(recon.getId(), CTX);
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, reloadRecon(recon.getId()).getDocStatus());
        assertEquals(0, countBillLinks(recon.getCode()),
                "无未达项时不应生成调整凭证");
    }

    @Test
    public void testPostGeneratesAdjustmentVoucherAndReverse() {
        long seed = System.nanoTime();
        long subjectId = 9004L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject(90040L, "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            // 调整凭证过账需要 OPEN 期间匹配 voucherDate=2026-06-30
            seedPeriod("2026-06-OPEN", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        // 账户余额 1000；对账单 1 笔 CREDIT 500（银行已收）+ balanceAfter=1500 → endingBalance=1500
        // 1 笔未达 CREDIT：恒等式 (1500-1000) - (500-0) = 0 ✓ 平衡且含未达
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-ADJ-" + seed,
                DC_CREDIT, new BigDecimal("500"), new BigDecimal("1500"));
        ErpFinBankStatement head = bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX);

        ErpFinBankReconciliation recon = bankReconciliationBiz.generate(head.getId(), CTX);
        assertTrue(recon.getIsBalanced());

        bankReconciliationBiz.post(recon.getId(), CTX);
        assertTrue(countBillLinks(recon.getCode()) >= 1, "存在未达项时应生成 BANK_RECON_ADJ 调整凭证");

        Long adjVoucherId = findAdjVoucherId(recon.getCode());
        assertNotNull(adjVoucherId, "应能反查到调整凭证 ID");
        ErpFinVoucher adj = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(adjVoucherId);
        assertEquals(VOUCHER_STATUS_POSTED, adj.getDocStatus(), "调整凭证已过账");

        bankReconciliationBiz.reverse(recon.getId(), CTX);
        assertEquals(ErpFinConstants.VOUCHER_STATUS_CANCELLED, reloadRecon(recon.getId()).getDocStatus());
        assertTrue(countReversalVouchers(adjVoucherId) >= 1, "应生成红字调整凭证");
    }

    @Test
    public void testPeriodClosedRejectsGenerate() {
        long seed = System.nanoTime();
        long subjectId = 9005L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            ErpFinAccountingPeriod period = seedPeriod("2026-06-CLOSED", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            seedPeriodStatusClosed(period.getId());
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-CLOSED-" + seed,
                DC_CREDIT, new BigDecimal("0"));
        ErpFinBankStatement head = bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX);

        assertThrows(NopException.class, () -> bankReconciliationBiz.generate(head.getId(), CTX),
                "期间 CLOSED 应拒绝生成调节表");
    }

    // ---------- helpers ----------

    private long countBillLinks(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", "BANK_RECON_ADJ"));
        return dao.findAllByQuery(q).size();
    }

    private Long findAdjVoucherId(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", "BANK_RECON_ADJ"));
        q.setLimit(1);
        ErpFinVoucherBillR link = dao.findFirstByQuery(q);
        return link != null ? link.getVoucherId() : null;
    }

    private long countReversalVouchers(Long originalVoucherId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reversalOfVoucherId", originalVoucherId));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinBankReconciliation reloadRecon(Long id) {
        return daoProvider.daoFor(ErpFinBankReconciliation.class).getEntityById(id);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount) {
        return line(txnDate, refNo, dc, amount, null);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount,
                                        BigDecimal balanceAfter) {
        BankStatementLineInput in = new BankStatementLineInput();
        in.setTransactionDate(txnDate);
        in.setRefNo(refNo);
        in.setDcDirection(dc);
        in.setAmount(amount);
        in.setBalanceAfter(balanceAfter);
        return in;
    }

    private long seedFundAccount(long subjectId, BigDecimal currentBalance) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.newEntity();
        account.setCode("FA-" + System.nanoTime());
        account.setName("Bank");
        account.setOrgId(1L);
        account.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        account.setSubjectId(subjectId);
        account.setCurrencyId(1L);
        account.setOpeningBalance(currentBalance);
        account.setCurrentBalance(currentBalance);
        account.setStatus("ACTIVE");
        dao.saveEntity(account);
        return account.getId();
    }

    private void seedSubject(long id, String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
        s.setDirection("DEBIT");
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private ErpFinAccountingPeriod seedPeriod(String code, int year, int month,
                                               LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = dao.newEntity();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(status);
        dao.saveEntity(p);
        return p;
    }

    private void seedPeriodStatusClosed(Long periodId) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        ErpFinAccountingPeriodStatus s = dao.newEntity();
        s.setPeriodId(periodId);
        s.setAcctSchemaId(1L);
        s.setArStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        s.setApStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        s.setInvStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        s.setGlStatus(ErpFinConstants.PERIOD_STATUS_CLOSED);
        s.setAssetStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        dao.saveEntity(s);
    }

    @SuppressWarnings("unused")
    private ErpFinVoucherLine unused() {
        return null;
    }
}
