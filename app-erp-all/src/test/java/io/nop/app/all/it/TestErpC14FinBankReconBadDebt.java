package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B7 C14：银行对账与坏账计提回收（按 {@code docs/design/integration-testing.md §6 C14} 规格）。
 *
 * <p>全链（自包含资金账户/银行对账单/OPEN AR 项，引用部署 seed：组织 2 + 账套 1 + 币种 CNY 1 +
 * 期间 2026-07（id=1，OPEN）+ 科目 1002/2240OTHER/1122/1231/6701 + 客户 CUST-001）：
 * 自包含资金账户 save（{@code ErpFinFundAccount__save}，IT-C14-FA-001，BANK 账户 + 科目 1002
 * 银行存款，currentBalance=1000）→ 银行对账单导入（{@code ErpFinBankStatement__importStatement}，
 * 2026-07-31，3 行 CREDIT 300/200/100，endingBalance=1600，全 UNMATCHED 未达）→
 * {@code ErpFinBankReconciliation__generate}（恒等式 1600−1000=600=未达净值 → 平衡，DRAFT）→
 * {@code ErpFinBankReconciliation__post}（BANK_RECON_ADJ 未达调整凭证 Dr 1002 600/Cr 2240OTHER 600，
 * POSTED）→ {@code ErpFinBankReconciliation__reverse}（红冲：CANCELLED + 原凭证 isReversed）→
 * 自包含 OPEN AR 项 save（{@code ErpFinArApItem__save}，IT-C14-AR-001，应收 1000，partner 隔离）
 * → {@code ErpFinBadDebt__writeOff}（WRITE_OFF，UNSUBMITTED）→ {@code ErpFinBadDebt__submit}
 * （SUBMITTED）→ {@code ErpFinBadDebt__approve} [DIRECT]（APPROVED + 项 WRITTEN_OFF +
 * BAD_DEBT_WRITE_OFF 凭证 Dr 1231/Cr 1122 1000）→ {@code ErpFinBadDebt__recover}（RECOVERY）→
 * submit → approve（项回退 OPEN + BAD_DEBT_RECOVERY 凭证 Dr 1122/Cr 1231 1000）→
 * {@code ErpFinBadDebt__runBadDebtProvision}(periodId=1)（期末计提：seed ARAP-EA-001
 * （1000，账龄 0-30 档 × 0.005=5）+ 自包含项（1000，61-90 档 × 0.05=50）→ 必需 55 → RESERVE
 * 凭证 Dr 6701 55/Cr 1231 55）。
 *
 * <p>Phase 2 Decision（三项裁决，落盘设计文档 §6 C14 勘误）：
 * <ol>
 *   <li><b>科目 config 裁决</b>：未达调整凭证科目 = 资金账户 subjectId（1002 银行存款）+
 *       {@code erp-fin.bank-recon-adj-subject-code}（缺省 2240OTHER，seed 在位，无需覆盖）；
 *       坏账核销/收回凭证 = {@code erp-fin.ar-subject-code=1122}（有键无 DEFAULT_* 缺省）+ 
 *       {@code erp-fin.bad-debt-allowance-subject-code=1231}；期末计提追加
 *       {@code erp-fin.bad-debt-expense-subject-code=6701}——app-erp-all 装配无 fin-service
 *       专用 test yaml，4 科目键经 {@code @NopTestProperty} 指定（对齐 07-25 基线 E2E JVM args
 *       已知键 + C06/C08/C10/C12 门控同型处理）。计提门控 {@code erp-fin.bad-debt-allowance-gate-enabled}
 *       默认 true（实仓核验）但仅作用于 closePeriod 前置检查，本用例直接调
 *       {@code runBadDebtProvision} 不经门控 → 无需覆盖。</li>
 *   <li><b>计提金额断言口径</b>：以实仓 {@code BadDebtProvisionCalculator} 行为为准——
 *       账龄基准 = dueDate（{@code erp-fin.ar-aging-base} 缺省 due_date）；asOf = 期间 endDate
 *       （2026-07-31）；自包含项 dueDate=2026-05-15 → 账龄 77 天 ∈ 61-90 档（0.05）；
 *       seed ARAP-EA-001 dueDate=2026-08-04 → 负账龄 ∈ 0-30 档（0.005）。必需 = 1000×0.005 +
 *       1000×0.05 = 55.000；Allowance 账面 0 → RESERVE（借 6701/贷 1231 55）。计提范围 =
 *       全应收未核销项（无 org/期间过滤，实仓 {@code findReceivableOpenItems}）。</li>
 *   <li><b>数据来源</b>：seed 无 fund account/bank statement/bad debt 行（银行账户
 *       {@code erp_md_bank_account} 为 MD 侧静态档案，资金账户 {@code erp_fin_fund_account}
 *       无 seed）→ 资金账户/对账单/OPEN AR 项全部自包含建数（bank-recon E2E 先例同型），
 *       未触发 seed 修正授权。对账单行不调用 autoMatch（全部 UNMATCHED 即未达项——seed 账面
 *       1002 科目凭证行金额与导入行不匹配，autoMatch 无命中项，设计文档 C14 步骤亦不含
 *       autoMatch 步骤）。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（对账状态机 DRAFT→POSTED→CANCELLED + BANK_RECON_ADJ
 * 凭证借贷平衡 + 红冲闭环原凭证 isReversed；坏账核销凭证 Dr 1231/Cr 1122 + 收回红冲
 * Dr 1122/Cr 1231 + AR 项状态翻转；计提金额 55 = 期末 AR × 计提率口径）；层 2 = 每步 response
 * 快照；层 3 = output/tables 变更行（fund_account + bank_statement + bank_statement_line +
 * bank_reconciliation + bank_reconciliation_line + ar_ap_item + bad_debt + voucher +
 * voucher_line + voucher_bill_r）。RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 * 冻结时钟 2026-07-17（{@code C13C14FrozenClockExtension}）：坏账单 businessDate =
 * CoreMetrics.today()（实仓 {@code ErpFinBadDebtProcessor.newBadDebt}），冻结使其落在
 * 2026-07 期间内且跨 run 确定；快照时间戳列按 B4/B6 先例以 {@code *} 通配处置。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-fin.ar-subject-code", value = "1122")
@NopTestProperty(name = "erp-fin.bad-debt-allowance-subject-code", value = "1231")
@NopTestProperty(name = "erp-fin.bad-debt-expense-subject-code", value = "6701")
public class TestErpC14FinBankReconBadDebt extends ErpIntegrationTestCase {

    static final String ORG_ID = "2";
    static final String ACCT_SCHEMA_ID = "1";
    static final String CURRENCY_ID = "1";
    static final String PERIOD_ID = "1";                    // seed 2026-07 OPEN
    static final String FA_CODE = "IT-C14-FA-001";
    static final String SUBJECT_BANK_ID = "2";              // seed 1002 银行存款
    static final String SUBJECT_BANK_CODE = "1002";
    static final String SUBJECT_ADJ_CODE = "2240OTHER";     // seed 未达账项调整-其他应付款（缺省键值）
    static final String SUBJECT_AR_CODE = "1122";           // seed 应收账款
    static final String SUBJECT_ALLOWANCE_CODE = "1231";    // seed 坏账准备
    static final String SUBJECT_EXPENSE_CODE = "6701";      // seed 信用减值损失
    static final String AR_CODE = "IT-C14-AR-001";
    static final String PARTNER_ID = "1";                   // seed CUST-001 华东科技
    static final BigDecimal BOOK_BALANCE = new BigDecimal("1000");
    static final BigDecimal UNRECONCILED = new BigDecimal("600");   // 3 行 CREDIT 300+200+100
    static final BigDecimal AR_AMOUNT = new BigDecimal("1000");
    static final BigDecimal PROVISION_REQUIRED = new BigDecimal("55.000");  // 1000×0.005 + 1000×0.05
    static final BigDecimal PROVISION_AMOUNT = new BigDecimal("55.000");
    static final String REASON_WRITE_OFF = "C14 坏账核销：客户经营异常";
    static final String REASON_RECOVER = "C14 坏账收回：部分回款";

    @RegisterExtension
    static C13C14FrozenClockExtension frozenClock = new C13C14FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testBankReconBadDebtClosedLoop() {
        // ---------- 1. 自包含资金账户（BANK + 科目 1002，currentBalance=1000） ----------
        ApiResponse<?> faSave = rpcMutation("ErpFinFundAccount__save",
                request("1_fund_account_save.json5", Map.class));
        output("1_fund_account_save_response.json5", faSave);
        assertEquals(0, faSave.getStatus(), "资金账户保存应成功");
        String faId = idOf(faSave);
        addVar("faId", faId);
        addVar("faCode", FA_CODE);

        // ---------- 2. 银行对账单导入（2026-07-31，3 行 CREDIT 未达，endingBalance=1600） ----------
        ApiResponse<?> stmtImport = rpcMutation("ErpFinBankStatement__importStatement",
                request("2_statement_import.json5", Map.class));
        output("2_statement_import_response.json5", stmtImport);
        assertEquals(0, stmtImport.getStatus(), "对账单导入应成功");
        String stmtId = idOf(stmtImport);
        addVar("stmtId", stmtId);
        ErpFinBankStatement statement = reload(ErpFinBankStatement.class, stmtId);
        assertEquals(0, new BigDecimal("1600").compareTo(statement.getEndingBalance()),
                "对账单期末余额=1600（期初 1000 + 未达 600）");

        // ---------- 3. generate：恒等式平衡 → DRAFT ----------
        ApiResponse<?> genResp = rpcMutation("ErpFinBankReconciliation__generate",
                request("3_recon_generate.json5", Map.class));
        output("3_recon_generate_response.json5", genResp);
        assertEquals(0, genResp.getStatus(), "调节表生成应成功");
        String reconId = idOf(genResp);
        addVar("reconId", reconId);
        ErpFinBankReconciliation recon = reload(ErpFinBankReconciliation.class, reconId);
        assertEquals(Boolean.TRUE, recon.getIsBalanced(), "对账应平衡（1600−1000=600=未达净值）");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_DRAFT, recon.getDocStatus(), "调节表 DRAFT");
        assertEquals(0, BOOK_BALANCE.compareTo(recon.getBookBalance()), "账面余额=1000");
        assertEquals(0, new BigDecimal("1600").compareTo(recon.getStatementBalance()), "对账单余额=1600");
        assertEquals(0, BigDecimal.ZERO.compareTo(recon.getUnreconciledDiff()), "未达差异=0");
        addVar("reconCode", recon.getCode());

        // ---------- 4. post：BANK_RECON_ADJ 未达调整凭证（Dr 1002 600 / Cr 2240OTHER 600） ----------
        ApiResponse<?> postResp = rpcMutation("ErpFinBankReconciliation__post",
                request("4_recon_post.json5", Map.class));
        output("4_recon_post_response.json5", postResp);
        assertEquals(0, postResp.getStatus(), "调节表过账应成功");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, reload(ErpFinBankReconciliation.class, reconId).getDocStatus(),
                "调节表 POSTED");
        ErpFinVoucher adjVoucher = requireVoucherBalanced(
                findBillLink(recon.getCode(), "BANK_RECON_ADJ"), UNRECONCILED, "未达调整凭证");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, adjVoucher.getDocStatus(), "调整凭证已过账");
        List<ErpFinVoucherLine> adjLines = findVoucherLines(adjVoucher.getId());
        ErpFinVoucherLine adjDr = findLineBySubject(adjLines, SUBJECT_BANK_CODE);
        ErpFinVoucherLine adjCr = findLineBySubject(adjLines, SUBJECT_ADJ_CODE);
        assertNotNull(adjDr, "调整凭证应含借方 1002 银行存款");
        assertNotNull(adjCr, "调整凭证应含贷方 2240OTHER 未达调整");
        assertEquals(ErpFinConstants.DC_DEBIT, adjDr.getDcDirection(), "银行存款借方方向");
        assertEquals(0, UNRECONCILED.compareTo(adjDr.getDebitAmount()), "银行存款借方=600");
        assertEquals(ErpFinConstants.DC_CREDIT, adjCr.getDcDirection(), "未达调整贷方方向");
        assertEquals(0, UNRECONCILED.compareTo(adjCr.getCreditAmount()), "未达调整贷方=600");

        // ---------- 5. reverse：红冲闭环（原凭证 isReversed + 红字凭证） ----------
        ApiResponse<?> reverseResp = rpcMutation("ErpFinBankReconciliation__reverse",
                request("5_recon_reverse.json5", Map.class));
        output("5_recon_reverse_response.json5", reverseResp);
        assertEquals(0, reverseResp.getStatus(), "调节表红冲应成功");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_CANCELLED,
                reload(ErpFinBankReconciliation.class, reconId).getDocStatus(), "调节表 CANCELLED");
        ErpFinVoucher adjReversed = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(adjVoucher.getId());
        assertEquals(Boolean.TRUE, adjReversed.getIsReversed(), "原调整凭证已标记红冲");
        assertTrue(countReversalVouchers(adjVoucher.getId()) >= 1, "调整凭证红字凭证生成");

        // ---------- 6. 自包含 OPEN AR 项（partner 隔离，dueDate 61-90 账龄档） ----------
        ApiResponse<?> arSave = rpcMutation("ErpFinArApItem__save",
                request("6_ar_ap_item_save.json5", Map.class));
        output("6_ar_ap_item_save_response.json5", arSave);
        assertEquals(0, arSave.getStatus(), "应收项保存应成功");
        String arItemId = idOf(arSave);
        addVar("arItemId", arItemId);
        addVar("arCode", AR_CODE);

        // ---------- 7. 坏账核销：writeOff → submit → approve [DIRECT] ----------
        ApiResponse<?> writeOffResp = rpcMutation("ErpFinBadDebt__writeOff",
                request("7_bad_debt_write_off.json5", Map.class));
        output("7_bad_debt_write_off_response.json5", writeOffResp);
        assertEquals(0, writeOffResp.getStatus(), "坏账核销单创建应成功");
        String woId = idOf(writeOffResp);
        addVar("woId", woId);
        assertEquals(ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF, reload(ErpFinBadDebt.class, woId).getDocType(),
                "坏账单类型 WRITE_OFF");
        assertEquals(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                reload(ErpFinBadDebt.class, woId).getApprovalStatus(), "核销单 UNSUBMITTED（审批门控默认 true）");

        ApiResponse<?> woSubmit = rpcMutation("ErpFinBadDebt__submit",
                request("8_bad_debt_write_off_submit.json5", Map.class));
        output("8_bad_debt_write_off_submit_response.json5", woSubmit);
        assertEquals(0, woSubmit.getStatus(), "核销单提交应成功");
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                reload(ErpFinBadDebt.class, woId).getApprovalStatus(), "核销单 SUBMITTED");

        ApiResponse<?> woApprove = rpcMutation("ErpFinBadDebt__approve",
                request("9_bad_debt_write_off_approve.json5", Map.class));
        output("9_bad_debt_write_off_approve_response.json5", woApprove);
        assertEquals(0, woApprove.getStatus(), "核销单审批应成功");
        ErpFinBadDebt writtenOff = reload(ErpFinBadDebt.class, woId);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, writtenOff.getApprovalStatus(), "核销单 APPROVED");
        addVar("woCode", writtenOff.getCode());

        // 层 1 锚点：AR 项 WRITTEN_OFF + openAmount→0；BAD_DEBT_WRITE_OFF 凭证 Dr 1231/Cr 1122
        ErpFinArApItem arAfterWriteOff = reload(ErpFinArApItem.class, arItemId);
        assertEquals(ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF, arAfterWriteOff.getStatus(), "核销后项 WRITTEN_OFF");
        assertEquals(0, BigDecimal.ZERO.compareTo(arAfterWriteOff.getOpenAmountFunctional()), "核销后 openAmount=0");
        assertEquals(0, AR_AMOUNT.compareTo(arAfterWriteOff.getSettledAmountFunctional()), "核销后 settledAmount=1000");
        ErpFinVoucher woVoucher = requireVoucherBalanced(
                findBillLink(writtenOff.getCode(), "BAD_DEBT_WRITE_OFF"), AR_AMOUNT, "坏账核销凭证");
        List<ErpFinVoucherLine> woLines = findVoucherLines(woVoucher.getId());
        ErpFinVoucherLine woAllowance = findLineBySubject(woLines, SUBJECT_ALLOWANCE_CODE);
        ErpFinVoucherLine woAr = findLineBySubject(woLines, SUBJECT_AR_CODE);
        assertNotNull(woAllowance, "核销凭证应含借方 1231 坏账准备");
        assertNotNull(woAr, "核销凭证应含贷方 1122 应收账款");
        assertEquals(ErpFinConstants.DC_DEBIT, woAllowance.getDcDirection(), "坏账准备借方方向");
        assertEquals(0, AR_AMOUNT.compareTo(woAllowance.getDebitAmount()), "坏账准备借方=1000");
        assertEquals(ErpFinConstants.DC_CREDIT, woAr.getDcDirection(), "应收账款贷方方向");
        assertEquals(0, AR_AMOUNT.compareTo(woAr.getCreditAmount()), "应收账款贷方=1000");

        // ---------- 8. 坏账收回：recover → submit → approve [DIRECT] ----------
        ApiResponse<?> recoverResp = rpcMutation("ErpFinBadDebt__recover",
                request("10_bad_debt_recover.json5", Map.class));
        output("10_bad_debt_recover_response.json5", recoverResp);
        assertEquals(0, recoverResp.getStatus(), "坏账收回单创建应成功");
        String rcId = idOf(recoverResp);
        addVar("rcId", rcId);
        assertEquals(ErpFinConstants.BAD_DEBT_TYPE_RECOVERY, reload(ErpFinBadDebt.class, rcId).getDocType(),
                "坏账单类型 RECOVERY");

        ApiResponse<?> rcSubmit = rpcMutation("ErpFinBadDebt__submit",
                request("11_bad_debt_recover_submit.json5", Map.class));
        output("11_bad_debt_recover_submit_response.json5", rcSubmit);
        assertEquals(0, rcSubmit.getStatus(), "收回单提交应成功");

        ApiResponse<?> rcApprove = rpcMutation("ErpFinBadDebt__approve",
                request("12_bad_debt_recover_approve.json5", Map.class));
        output("12_bad_debt_recover_approve_response.json5", rcApprove);
        assertEquals(0, rcApprove.getStatus(), "收回单审批应成功");
        ErpFinBadDebt recovered = reload(ErpFinBadDebt.class, rcId);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, recovered.getApprovalStatus(), "收回单 APPROVED");
        addVar("rcCode", recovered.getCode());

        // 层 1 锚点：AR 项回退 OPEN + openAmount 恢复；BAD_DEBT_RECOVERY 凭证 Dr 1122/Cr 1231（收回红冲）
        ErpFinArApItem arAfterRecover = reload(ErpFinArApItem.class, arItemId);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, arAfterRecover.getStatus(), "收回后项回退 OPEN");
        assertEquals(0, AR_AMOUNT.compareTo(arAfterRecover.getOpenAmountFunctional()), "收回后 openAmount 恢复=1000");
        assertEquals(0, BigDecimal.ZERO.compareTo(arAfterRecover.getSettledAmountFunctional()), "收回后 settledAmount=0");
        ErpFinVoucher rcVoucher = requireVoucherBalanced(
                findBillLink(recovered.getCode(), "BAD_DEBT_RECOVERY"), AR_AMOUNT, "坏账收回凭证");
        List<ErpFinVoucherLine> rcLines = findVoucherLines(rcVoucher.getId());
        ErpFinVoucherLine rcAr = findLineBySubject(rcLines, SUBJECT_AR_CODE);
        ErpFinVoucherLine rcAllowance = findLineBySubject(rcLines, SUBJECT_ALLOWANCE_CODE);
        assertNotNull(rcAr, "收回凭证应含借方 1122 应收账款");
        assertNotNull(rcAllowance, "收回凭证应含贷方 1231 坏账准备");
        assertEquals(ErpFinConstants.DC_DEBIT, rcAr.getDcDirection(), "应收账款借方方向");
        assertEquals(0, AR_AMOUNT.compareTo(rcAr.getDebitAmount()), "应收账款借方=1000");
        assertEquals(ErpFinConstants.DC_CREDIT, rcAllowance.getDcDirection(), "坏账准备贷方方向");
        assertEquals(0, AR_AMOUNT.compareTo(rcAllowance.getCreditAmount()), "坏账准备贷方=1000");

        // ---------- 9. 期末计提：runBadDebtProvision(periodId=1) → RESERVE 55 ----------
        ApiResponse<?> provisionResp = rpcMutation("ErpFinBadDebt__runBadDebtProvision",
                request("13_bad_debt_provision.json5", Map.class));
        output("13_bad_debt_provision_response.json5", provisionResp);
        assertEquals(0, provisionResp.getStatus(), "期末计提应成功");
        Map<?, ?> prov = (Map<?, ?>) provisionResp.getData();
        assertEquals("RESERVE", String.valueOf(prov.get("action")), "必需 55 > 账面 0 → 补提");
        assertEquals(0, PROVISION_REQUIRED.compareTo(asBigDecimal(prov.get("requiredProvision"))), "必需准备=55");
        assertEquals(0, BigDecimal.ZERO.compareTo(asBigDecimal(prov.get("allowanceBalance"))), "Allowance 账面=0");
        assertEquals(0, new BigDecimal("2000").compareTo(asBigDecimal(prov.get("totalConsidered"))),
                "计提基数=期末 AR 合计 2000（seed EA 1000 + 自包含 1000）");
        String provisionVoucherId = String.valueOf(prov.get("voucherId"));
        assertNotNull(provisionVoucherId, "计提生成凭证");
        ErpFinVoucher provisionVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(provisionVoucherId);
        assertNotNull(provisionVoucher, "计提凭证落库");
        assertEquals(0, PROVISION_AMOUNT.compareTo(provisionVoucher.getTotalDebit()), "计提凭证借方合计=55");
        assertEquals(0, PROVISION_AMOUNT.compareTo(provisionVoucher.getTotalCredit()), "计提凭证贷方合计=55");
        List<ErpFinVoucherLine> provLines = findVoucherLines(provisionVoucherId);
        ErpFinVoucherLine provExpense = findLineBySubject(provLines, SUBJECT_EXPENSE_CODE);
        ErpFinVoucherLine provAllowance = findLineBySubject(provLines, SUBJECT_ALLOWANCE_CODE);
        assertNotNull(provExpense, "计提凭证应含借方 6701 信用减值损失");
        assertNotNull(provAllowance, "计提凭证应含贷方 1231 坏账准备");
        assertEquals(ErpFinConstants.DC_DEBIT, provExpense.getDcDirection(), "信用减值损失借方方向");
        assertEquals(0, PROVISION_AMOUNT.compareTo(provExpense.getDebitAmount()), "信用减值损失借方=55");
        assertEquals(ErpFinConstants.DC_CREDIT, provAllowance.getDcDirection(), "坏账准备贷方方向");
        assertEquals(0, PROVISION_AMOUNT.compareTo(provAllowance.getCreditAmount()), "坏账准备贷方=55");
    }

    // ---------- helpers ----------

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        return links.isEmpty() ? null : links.get(0);
    }

    private long countReversalVouchers(String originalVoucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("reversalOfVoucherId", originalVoucherId));
        return daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(q).size();
    }

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine findLineBySubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        for (ErpFinVoucherLine l : lines) {
            if (subjectCode.equals(l.getSubjectCode())) {
                return l;
            }
        }
        return null;
    }

    private static BigDecimal asBigDecimal(Object raw) {
        return raw == null ? BigDecimal.ZERO : new BigDecimal(raw.toString());
    }
}