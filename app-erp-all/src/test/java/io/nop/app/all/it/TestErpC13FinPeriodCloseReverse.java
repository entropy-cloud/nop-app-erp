package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B7 C13：期末结账全链与反结账（按 {@code docs/design/integration-testing.md §6 C13} 规格）。
 *
 * <p>全链（seed 会计期间 2026-07（id=1，OPEN，org 2）+ seed 已过账业务链（PZ-2026-001..004）
 * + seed OPEN 往来项（ARAP-EA-001/ARAP-EC-001）+ 自包含外币应收项）：
 * 自包含 USD OPEN AR 项（{@code ErpFinArApItem__save}，IT-C13-FX-001，100 USD / openFunctional 1000）
 * → {@code ErpFinAccountingPeriod__preCheck}(periodId=1)（未核销 AR/AP 列表 = seed OPEN 往来项
 * EMPLOYEE_ADVANCE/EXPENSE_CLAIM + 自包含外币项）→ {@code ErpFinAccountingPeriod__closePeriod}
 * （模块关账 AR→AP→INV→AST→GL → CLOSED；汇兑重估 FX-REVAL-2026-07 凭证 Dr 6603 150/Cr 1122 150 +
 * 损益结转 PERIOD-CLOSE-2026-07 凭证 Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150（合计 1280，含 FX 腿），
 * billCode 反查）→ {@code ErpFinAccountingPeriod__finalizePeriod}（CLOSED_FINAL）→
 * {@code ErpFinAccountingPeriod__reverseClose}（reason 必填 → OPEN + 模块状态回开 + 期末凭证红冲
 * 原凭证 isReversed）→ {@code ErpFinAccountingPeriod__closePeriod} 重新结账（新结转凭证生成 = 幂等断言）。
 *
 * <p>Phase 1 Decision（四项裁决，落盘设计文档 §6 C13 勘误）：
 * <ol>
 *   <li><b>preCheck 返回结构</b>：{@code PeriodPreCheckReport} 未核销列表按期间 businessDate 范围 +
 *       orgId/acctSchemaId 隔离（seed OPEN 项 ARAP-EA-001/ARAP-EC-001 在期间 2026-07 内，被列出）；
 *       另有 seed 折旧计划（erp_ast_depreciation_schedule id=1 EXECUTED posted=false）被
 *       {@code findUnresolvedDepreciationSchedules} 列为未处置悬挂键（depreciation:2#2026-07）
 *       → {@code hasIssues}=true → 须 {@code erp-fin.auto-post-on-close=true} 降级为提示（对齐
 *       {@code TestErpFinPeriodCloseEndToEnd} 先例 yaml 同键），否则 closePeriod 被
 *       {@code ERR_PRE_CHECK_BLOCKED} 阻断。GraphQL 序列化不含 hasXxx()/issueCount() 派生 getter
 *       → 快照输出归一化映射仅取属性 getter（未核销/未过账/未处置键 + allowance 四值）。</li>
 *   <li><b>期末凭证断言锚点（billCode 反查）</b>：汇兑重估 = {@code FX-REVAL-2026-07}
 *       （businessType EXCHANGE_GAIN_LOSS）；损益结转 = {@code PERIOD-CLOSE-2026-07}
 *       （businessType PERIOD_CLOSE）。无外币暴露时实仓不生成 FX 凭证（干净期间不重估），
 *       本用例自包含 USD 应收项（100 USD / 期末汇率 8.5 → 重估 diff=1000−850=150 损失
 *       → Dr 6603/Cr 1122）驱动 FX 凭证生成。<b>flush 边界缺陷已修复（2026-08-25，plan
 *       2026-08-25-0330-2）</b>：原「PERIOD_CLOSE = Dr 5001 1130 / Cr 4103 1130（不含 FX 损失结转腿）」为
 *       {@code CloseVoucherWriter} 直接 save 未 flush、损益结转聚合（DB 直查）见不到同事务 FX 凭证所致——
 *       修复后写侧统一 flush，PERIOD_CLOSE 含 FX 腿（1280 = Dr 5001 1130 + Dr 4103 150 ↔
 *       Cr 4103 1130 + Cr 6603 150），与 {@code TestErpFinProfitLossClosing.testProfitLossClosingIncludesFxGainLoss}
 *       语义统一为「含 FX」。科目 config 经
 *       {@code @NopTestProperty} 指定（erp-fin.current-year-profit-subject-code=4103 /
 *       ar-subject-code=1122 / ap-subject-code=2202 / exchange-gain-loss-subject-code=6603 /
 *       period-end-exchange-rate=8.5，对齐 07-25 基线 E2E JVM args 已知键 + fin-service 期间关账
 *       test yaml 同型）。</li>
 *   <li><b>反结账→重新结账幂等断言口径</b>：reverseClose 后原结转/重估凭证经
 *       {@code voucherBiz.reverse} 红冲（原凭证 isReversed=true + 红字凭证 REV-* 回链 reversalOfVoucherId；
 *       红字凭证自身 isReversed=true → 重新结账的损益结转聚合按 isReversed 过滤天然排除红字行，
 *       故重新 closePeriod 生成<b>新</b>结转/重估凭证，billCode 反查链接计数 ≥2 且金额与首轮一致
 *       （FX 150 + PL 1280）——对齐 {@code TestErpFinPeriodCloseEndToEnd} 先例断言，
 *       不做「复用原凭证」语义）。{@code erp-fin.reverse-close-approval-required=false} 必配
 *       （默认 true 反结账被拒）。</li>
 *   <li><b>冻结时钟</b>：本用例经 {@code C13C14FrozenClockExtension}（2026-07-17 ∈ 2026-07 期间）
 *       冻结日期（对齐 C07/C11/C12 先例）；期间解析（preCheck 未核销项 DUE_DATE / closePeriod
 *       凭证日期 = 期间 endDate）为静态数据不受时钟影响，时钟主要保证期初/期间边界日期确定性。
 *       快照时间戳列（closedAt/reverseCloseAt/voucher postedAt 等）按 B4/B6 先例以 {@code *} 通配处置。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（preCheck 未核销项列表 + CLOSED→CLOSED_FINAL→OPEN→CLOSED
 * 状态翻转 + 模块关账状态 + 汇兑重估/损益结转凭证借贷平衡 + 反结账红冲闭环 + 重新结账新凭证）；
 * 层 2 = 每步 response 快照；层 3 = output/tables 变更行（ar_ap_item + accounting_period +
 * accounting_period_status + voucher + voucher_line + voucher_bill_r + trial_balance）。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-fin.auto-post-on-close", value = "true")
@NopTestProperty(name = "erp-fin.auto-depreciation-on-close", value = "false")
@NopTestProperty(name = "erp-fin.reverse-close-approval-required", value = "false")
@NopTestProperty(name = "erp-fin.bad-debt-allowance-gate-enabled", value = "false")
@NopTestProperty(name = "erp-fin.current-year-profit-subject-code", value = "4103")
@NopTestProperty(name = "erp-fin.exchange-gain-loss-subject-code", value = "6603")
@NopTestProperty(name = "erp-fin.ar-subject-code", value = "1122")
@NopTestProperty(name = "erp-fin.ap-subject-code", value = "2202")
@NopTestProperty(name = "erp-fin.period-end-exchange-rate", value = "8.5")
public class TestErpC13FinPeriodCloseReverse extends ErpIntegrationTestCase {

    static final String SEED_PERIOD_ID = "1";       // seed 2026-07（org 2, 账套 1）
    static final String SEED_PERIOD_CODE = "2026-07";
    static final String SEED_STATUS_ID = "1";       // seed period_status（5 模块全 OPEN）
    static final String ORG_ID = "2";
    static final String ACCT_SCHEMA_ID = "1";
    static final String CURRENCY_CNY = "1";
    static final String CURRENCY_USD = "2";
    static final String PARTNER_ID = "1";           // seed CUST-001 华东科技
    static final String FX_ITEM_CODE = "IT-C13-FX-001";
    static final String SUBJECT_AR_CODE = "1122";   // seed 应收账款
    static final String SUBJECT_FX_CODE = "6603";   // seed 财务费用-利息支出（汇兑损益科目键）
    static final String SUBJECT_INCOME_CODE = "5001"; // seed 主营业务收入
    static final String SUBJECT_CYP_CODE = "4103";  // seed 本年利润
    static final String FX_BILL_CODE = "FX-REVAL-" + SEED_PERIOD_CODE;
    static final String PL_BILL_CODE = "PERIOD-CLOSE-" + SEED_PERIOD_CODE;
    static final BigDecimal FX_AMOUNT = new BigDecimal("150");      // 重估 diff = 1000 − 100×8.5
    static final BigDecimal PL_TOTAL = new BigDecimal("1280");      // Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150（含 FX 结转腿，plan 2026-08-25-0330-2 修复后语义）
    static final BigDecimal PL_INCOME = new BigDecimal("1130");     // seed 5001 收入
    static final BigDecimal PL_FX = new BigDecimal("150");          // FX 损失结转腿（Cr 6603，费用类贷方结转）

    @RegisterExtension
    static C13C14FrozenClockExtension frozenClock = new C13C14FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testPeriodCloseReverseClosedLoop() {
        // ---------- 1. 自包含外币应收项（USD 100 / openFunctional 1000 → 驱动汇兑重估） ----------
        ApiResponse<?> itemSave = rpcMutation("ErpFinArApItem__save",
                request("1_ar_ap_item_save.json5", Map.class));
        output("1_ar_ap_item_save_response.json5", itemSave);
        assertEquals(0, itemSave.getStatus(), "外币应收项保存应成功");
        String fxItemId = idOf(itemSave);
        addVar("fxItemId", fxItemId);
        addVar("fxItemCode", FX_ITEM_CODE);

        ErpFinArApItem fxItem = reload(ErpFinArApItem.class, fxItemId);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, fxItem.getStatus(), "外币应收项 OPEN");
        assertEquals(0, new BigDecimal("1000").compareTo(fxItem.getOpenAmountFunctional()),
                "openAmountFunctional=1000（USD 100 @ 隐含汇率 10）");

        // ---------- 2. preCheck（@BizQuery）：未核销 AR/AP 列表 = seed OPEN 往来项 + 自包含外币项 ----------
        ApiResponse<?> preCheck = executeRpc(GraphQLOperationType.query, "ErpFinAccountingPeriod__preCheck",
                request("2_period_pre_check.json5", Map.class));
        output("2_period_pre_check_response.json5", preCheckState(preCheck));
        assertEquals(0, preCheck.getStatus(), "前置检查应成功");
        @SuppressWarnings("unchecked")
        List<String> unsettled = (List<String>) ((Map<?, ?>) preCheck.getData()).get("unsettledArApCodes");
        assertTrue(unsettled.contains("ARAP-EA-001"), "preCheck 列出 seed OPEN 应收 EMPLOYEE_ADVANCE");
        assertTrue(unsettled.contains("ARAP-EC-001"), "preCheck 列出 seed OPEN 应付 EXPENSE_CLAIM");
        assertTrue(unsettled.contains(FX_ITEM_CODE), "preCheck 列出自包含外币应收项");
        @SuppressWarnings("unchecked")
        List<String> exceptionKeys = (List<String>) ((Map<?, ?>) preCheck.getData())
                .get("unresolvedPostingExceptionKeys");
        assertTrue(exceptionKeys.contains("depreciation:2#2026-07"),
                "preCheck 列出 seed 折旧计划未过账悬挂键（auto-post-on-close=true 降级为提示）");

        // ---------- 3. closePeriod：模块关账 + 汇兑重估 + 损益结转 → CLOSED ----------
        ApiResponse<?> closeResp = rpcMutation("ErpFinAccountingPeriod__closePeriod",
                request("3_period_close.json5", Map.class));
        output("3_period_close_response.json5", closeResp);
        assertEquals(0, closeResp.getStatus(), "结账应成功");
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, reloadPeriod().getStatus(), "结账后 CLOSED");
        assertModulesClosed("结账后模块关账状态");

        // 层 1 锚点：汇兑重估凭证（FX-REVAL-2026-07：Dr 6603 150 / Cr 1122 150）
        ErpFinVoucher fxVoucher = requireVoucherBalanced(
                findBillLink(FX_BILL_CODE, ErpFinBusinessTypeName.EXCHANGE_GAIN_LOSS), FX_AMOUNT, "汇兑重估凭证");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, fxVoucher.getDocStatus(), "汇兑重估凭证已过账");
        List<ErpFinVoucherLine> fxLines = findVoucherLines(fxVoucher.getId());
        ErpFinVoucherLine fxDr = findLineBySubject(fxLines, SUBJECT_FX_CODE);
        ErpFinVoucherLine fxCr = findLineBySubject(fxLines, SUBJECT_AR_CODE);
        assertNotNull(fxDr, "汇兑凭证应含借方 6603 汇兑损益（损失）");
        assertNotNull(fxCr, "汇兑凭证应含贷方 1122 应收账款");
        assertEquals(ErpFinConstants.DC_DEBIT, fxDr.getDcDirection(), "汇兑损失借方方向");
        assertEquals(0, FX_AMOUNT.compareTo(fxDr.getDebitAmount()), "汇兑损失金额=150");
        assertEquals(ErpFinConstants.DC_CREDIT, fxCr.getDcDirection(), "应收贷方方向");
        assertEquals(0, FX_AMOUNT.compareTo(fxCr.getCreditAmount()), "应收贷方=150");

        // 层 1 锚点：损益结转凭证（PERIOD-CLOSE-2026-07：Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150，
        // 合计 1280 含 FX 结转腿——flush 边界修复后两套件语义统一为「含 FX」，plan 2026-08-25-0330-2）。
        // 4103 本年利润有借贷两腿（Cr 收入 1130 + Dr 费用 150），断言按借贷方向分别锚定，不能单腿匹配。
        ErpFinVoucher plVoucher = requireVoucherBalanced(
                findBillLink(PL_BILL_CODE, ErpFinBusinessTypeName.PERIOD_CLOSE), PL_TOTAL, "损益结转凭证");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, plVoucher.getDocStatus(), "损益结转凭证已过账");
        List<ErpFinVoucherLine> plLines = findVoucherLines(plVoucher.getId());
        ErpFinVoucherLine plIncome = findLineBySubjectAndDirection(plLines, SUBJECT_INCOME_CODE, ErpFinConstants.DC_DEBIT);
        ErpFinVoucherLine plCypIncome = findLineBySubjectAndDirection(plLines, SUBJECT_CYP_CODE, ErpFinConstants.DC_CREDIT);
        ErpFinVoucherLine plCypExpense = findLineBySubjectAndDirection(plLines, SUBJECT_CYP_CODE, ErpFinConstants.DC_DEBIT);
        ErpFinVoucherLine plFx = findLineBySubjectAndDirection(plLines, SUBJECT_FX_CODE, ErpFinConstants.DC_CREDIT);
        assertNotNull(plIncome, "结转凭证应含 Dr 5001 收入结转腿");
        assertNotNull(plCypIncome, "结转凭证应含 Cr 4103 本年利润（收入侧）");
        assertNotNull(plCypExpense, "结转凭证应含 Dr 4103 本年利润（费用侧）");
        assertNotNull(plFx, "结转凭证应含 Cr 6603 汇兑损益结转腿（费用类贷方结转）");
        assertEquals(0, PL_INCOME.compareTo(plIncome.getDebitAmount()), "收入借方=1130");
        assertEquals(0, PL_INCOME.compareTo(plCypIncome.getCreditAmount()), "本年利润贷方（收入侧）=1130");
        assertEquals(0, FX_AMOUNT.compareTo(plCypExpense.getDebitAmount()), "本年利润借方（费用侧）=150");
        assertEquals(0, PL_FX.compareTo(plFx.getCreditAmount()), "汇兑损益贷方结转=150");

        // 汇兑损益科目结账后净额归零（FX 凭证 Dr 150 + 结转凭证 Cr 150）——对齐 fin-service FX 语义。
        assertEquals(0, subjectNetAmount(SUBJECT_FX_CODE).compareTo(BigDecimal.ZERO),
                "汇兑损益科目结账后净额归零（含 FX 语义）");

        // ---------- 4. finalizePeriod → CLOSED_FINAL ----------
        ApiResponse<?> finalizeResp = rpcMutation("ErpFinAccountingPeriod__finalizePeriod",
                request("4_period_finalize.json5", Map.class));
        output("4_period_finalize_response.json5", finalizeResp);
        assertEquals(0, finalizeResp.getStatus(), "最终锁定应成功");
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, reloadPeriod().getStatus(), "锁定后 CLOSED_FINAL");

        // ---------- 5. reverseClose（reason 必填）→ OPEN + 模块回开 + 期末凭证红冲 ----------
        ApiResponse<?> reverseCloseResp = rpcMutation("ErpFinAccountingPeriod__reverseClose",
                request("5_period_reverse_close.json5", Map.class));
        output("5_period_reverse_close_response.json5", reverseCloseResp);
        assertEquals(0, reverseCloseResp.getStatus(), "反结账应成功");
        ErpFinAccountingPeriod reversed = reloadPeriod();
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, reversed.getStatus(), "反结账后 OPEN");
        assertNotNull(reversed.getReverseCloseReason(), "反结账原因落库");
        assertModulesOpen("反结账后模块状态回开");

        // 层 1 锚点：红冲闭环——原结转/重估凭证 isReversed=true + 红字凭证回链
        ErpFinVoucher plReversed = requireVoucherBalanced(
                findBillLink(PL_BILL_CODE, ErpFinBusinessTypeName.PERIOD_CLOSE), PL_TOTAL, "损益结转凭证（红冲后）");
        assertEquals(Boolean.TRUE, plReversed.getIsReversed(), "原结转凭证已标记红冲");
        assertTrue(countReversalVouchers(plVoucher.getId()) >= 1, "结转凭证红字凭证生成");
        ErpFinVoucher fxReversed = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(fxVoucher.getId());
        assertEquals(Boolean.TRUE, fxReversed.getIsReversed(), "原汇兑重估凭证已标记红冲");
        assertTrue(countReversalVouchers(fxVoucher.getId()) >= 1, "汇兑重估红字凭证生成");

        // ---------- 6. closePeriod 重新结账（幂等：新结转/重估凭证生成） ----------
        ApiResponse<?> recloseResp = rpcMutation("ErpFinAccountingPeriod__closePeriod",
                request("6_period_reclose.json5", Map.class));
        output("6_period_reclose_response.json5", recloseResp);
        assertEquals(0, recloseResp.getStatus(), "重新结账应成功");
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, reloadPeriod().getStatus(), "重新结账后 CLOSED");
        assertModulesClosed("重新结账后模块关账状态");

        assertTrue(countBillLinks(PL_BILL_CODE, ErpFinBusinessTypeName.PERIOD_CLOSE) >= 2,
                "重新结账生成新的结转凭证（PERIOD_CLOSE 链接 ≥2：原凭证 + 红字 + 新凭证）");
        assertTrue(countBillLinks(FX_BILL_CODE, ErpFinBusinessTypeName.EXCHANGE_GAIN_LOSS) >= 2,
                "重新结账生成新的汇兑重估凭证（FX-REVAL 链接 ≥2）");
        ErpFinVoucherBillR reclosePlLink = findLastBillLink(PL_BILL_CODE, ErpFinBusinessTypeName.PERIOD_CLOSE);
        assertNotNull(reclosePlLink, "重新结账结转凭证回链存在");
        requireVoucherBalanced(reclosePlLink, PL_TOTAL, "重新结账结转凭证");
    }

    // ---------- helpers ----------

    private ErpFinAccountingPeriod reloadPeriod() {
        return reload(ErpFinAccountingPeriod.class, SEED_PERIOD_ID);
    }

    private ErpFinAccountingPeriodStatus reloadPeriodStatus() {
        return reload(ErpFinAccountingPeriodStatus.class, SEED_STATUS_ID);
    }

    private void assertModulesClosed(String label) {
        ErpFinAccountingPeriodStatus st = reloadPeriodStatus();
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, st.getArStatus(), label + " AR");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, st.getApStatus(), label + " AP");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, st.getInvStatus(), label + " INV");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, st.getAssetStatus(), label + " AST");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, st.getGlStatus(), label + " GL");
    }

    private void assertModulesOpen(String label) {
        ErpFinAccountingPeriodStatus st = reloadPeriodStatus();
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, st.getArStatus(), label + " AR");
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, st.getApStatus(), label + " AP");
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, st.getInvStatus(), label + " INV");
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, st.getAssetStatus(), label + " AST");
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, st.getGlStatus(), label + " GL");
    }

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        return links.isEmpty() ? null : links.get(0);
    }

    /** 反查末条链接（重新结账后取最新凭证回链，避免命中已红冲的首轮链接）。 */
    private ErpFinVoucherBillR findLastBillLink(String billCode, String businessType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        return links.isEmpty() ? null : links.get(links.size() - 1);
    }

    private long countBillLinks(String billCode, String businessType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q).size();
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

    /** 按科目 + 借贷方向锚定分录（4103 本年利润有借贷两腿，单腿匹配会命中歧义）。 */
    private ErpFinVoucherLine findLineBySubjectAndDirection(List<ErpFinVoucherLine> lines, String subjectCode,
                                                            String dcDirection) {
        for (ErpFinVoucherLine l : lines) {
            if (subjectCode.equals(l.getSubjectCode()) && dcDirection.equals(l.getDcDirection())) {
                return l;
            }
        }
        return null;
    }

    /** 科目本期净额（Σdebit − Σcredit，本期已过账凭证分录全量——结账后损益类科目应归零）。 */
    private BigDecimal subjectNetAmount(String subjectCode) {
        ErpMdSubject subject = findSubjectByCode(subjectCode);
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", SEED_PERIOD_ID));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        List<String> voucherIds = vDao.findAllByQuery(vq).stream()
                .map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
        if (voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("subjectId", subject.getId()));
        lq.addFilter(io.nop.api.core.beans.FilterBeans.in("voucherId", voucherIds));
        BigDecimal net = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lDao.findAllByQuery(lq)) {
            net = net.add(l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount())
                    .subtract(l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount());
        }
        return net;
    }

    private ErpMdSubject findSubjectByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** preCheck 响应归一化快照：列表排序保证跨 run 确定性（参考 TestErpFinPeriodCloseEndToEnd 过滤输出先例）。 */
    private Map<String, Object> preCheckState(ApiResponse<?> preCheck) {
        Map<String, Object> data = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) preCheck.getData();
        data.put("unsettledArApCodes", sorted(raw.get("unsettledArApCodes")));
        data.put("unresolvedPostingExceptionKeys", sorted(raw.get("unresolvedPostingExceptionKeys")));
        data.put("unpostedVoucherCodes", sorted(raw.get("unpostedVoucherCodes")));
        data.put("allowanceRequired", raw.get("allowanceRequired"));
        data.put("allowanceBalance", raw.get("allowanceBalance"));
        data.put("allowanceShortfall", raw.get("allowanceShortfall"));
        data.put("allowanceExcess", raw.get("allowanceExcess"));
        return data;
    }

    private List<String> sorted(Object raw) {
        if (raw == null) {
            return java.util.Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) raw;
        List<String> copy = new java.util.ArrayList<>(list);
        java.util.Collections.sort(copy);
        return copy;
    }

    private interface ErpFinBusinessTypeName {
        String EXCHANGE_GAIN_LOSS = "EXCHANGE_GAIN_LOSS";
        String PERIOD_CLOSE = "PERIOD_CLOSE";
    }
}