package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.fx.ExchangeRevaluationService;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 期末汇兑重估单测（Phase 3，承接 0300-3 deferred）。验证外币 AR 未核销项按期末汇率重估生成
 * EXCHANGE_GAIN_LOSS(130) 凭证、正/负差额收益/损失方向正确、本位币项不重估。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:exchange-revaluation-test.yaml")
public class TestErpFinExchangeRevaluation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ExchangeRevaluationService exchangeRevaluationService;

    @Test
    public void testForeignReceivableGain() {
        // 外币应收：源币 100，账面本位币 800（历史汇率 8），期末汇率 8.5 → 重估 850，应收升值 50（收益）。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-06", 2024, 6);
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
            seedSubject("6603", "财务费用-汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedOpenArAp("ARI-FX-001", pid, LocalDate.of(2024, 6, 10),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 2L,
                    new BigDecimal("100"), new BigDecimal("800"));
            return pid;
        });

        Long voucherId = exchangeRevaluationService.revalue(loadPeriod(periodId), CTX);

        assertNotNull(voucherId, "应生成汇兑重估凭证");
        ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertTrue(v.getTotalDebit().compareTo(v.getTotalCredit()) == 0, "汇兑凭证借贷平衡");
        assertEquals(0, v.getTotalDebit().compareTo(new BigDecimal("50")), "重估差额 50");

        // 应收收益：借应收 50 / 贷汇兑损益 50。
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        ErpFinVoucherLine arLine = lineOfSubject(lines, "1122");
        assertEquals(ErpFinConstants.DC_DEBIT, arLine.getDcDirection(), "应收升值借记应收");
        ErpFinVoucherLine fxLine = lineOfSubject(lines, "6603");
        assertEquals(ErpFinConstants.DC_CREDIT, fxLine.getDcDirection(), "收益贷记汇兑损益");
    }

    @Test
    public void testFunctionalItemNotRevalued() {
        // 本位币应收项不重估 → 无凭证。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-07", 2024, 7);
            seedCurrency(1L, "CNY", true);
            seedOpenArAp("ARI-FN-001", pid, LocalDate.of(2024, 7, 10),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 1L,
                    new BigDecimal("100"), new BigDecimal("100"));
            return pid;
        });

        Long voucherId = exchangeRevaluationService.revalue(loadPeriod(periodId), CTX);

        assertNull(voucherId, "本位币项不重估，无凭证");
    }

    /**
     * G2（P1-MA4-005 残差 / P1-MA2-022 测试可见性）：FX 跨期 reversal 累计漂移测试。
     *
     * <p>seed 外币 AR 项跨两期（P1=2024-06 / P2=2024-07），P1 重估生成 FX 凭证 → P2 重估（同批开放项再次按新汇率重估），
     * 断言累计 FX 损益行为。
     *
     * <p>实测行为（对齐 {@code period-close.md §已知简化「FX 重估无前期 reversal — IAS 21 残留风险」} + arm-index P1-MA2-022
     * ✅ resolved as documented simplification）：{@code revalueArAp} 查询所有未核销外币项（不按期间过滤），重估后不更新
     * {@code openAmountFunctional}、不 reversal 前期 FX 凭证。故 P2 重估仍以**原始** openFunctional(800) 为基准计算 diff，
     * 前期 FX 凭证不被冲回，累计 FX 入账 = P1(50) + P2(100) = 150 > 真实累计变动(100)，存在 50 漂移。
     *
     * <p>步断言使 P1-MA2-022 documented simplification（当期 spot-rate 重估、无前期 reversal、累计漂移）对测试可见并锁定
     * 为回归基线——若后续实现 IAS 21 spot-rate 前期 reversal（successor），本测试将标记行为变化。本测试不修复生产代码。
     */
    @Test
    public void testCrossPeriodRevaluationCumulativeBehavior() {
        // 原始账面：源币 100，本位币 800（隐含历史汇率 8.0）。
        BigDecimal openSource = new BigDecimal("100");
        BigDecimal openFunctional = new BigDecimal("800");

        Long[] seed = seedReturn(() -> {
            Long p1 = seedOpenPeriod("2024-06", 2024, 6, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));
            Long p2 = seedOpenPeriod("2024-07", 2024, 7, LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31));
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
            seedSubject("6603", "财务费用-汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            // AR 项 originated in P1，但 revalue 不按期间过滤，故 P2 重估仍会拾取同一项。
            seedOpenArAp("ARI-FX-CROSS-001", p1, LocalDate.of(2024, 6, 10),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 2L, openSource, openFunctional);
            return new Long[]{p1, p2};
        });
        Long p1Id = seed[0];
        Long p2Id = seed[1];

        BigDecimal originalRate = AppConfig.var(
                ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, BigDecimal.ONE);
        try {
            // P1 重估：期末汇率 8.5 → revalued 850，diff = 800 − 850 = −50 → 应收升值收益 50。
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, new BigDecimal("8.5"));
            Long p1VoucherId = exchangeRevaluationService.revalue(loadPeriod(p1Id), CTX);
            assertNotNull(p1VoucherId, "P1 重估应生成 FX 凭证");
            ErpFinVoucher p1Voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(p1VoucherId);
            BigDecimal p1Diff = new BigDecimal("50");
            assertFxCumulativeStep(p1Voucher, p1Diff, "P1");

            // P2 重估：期末汇率 9.0 → revalued 900，diff 仍以**原始** openFunctional(800) 为基准 = 800 − 900 = −100
            // （非以 P1 重估后 850 为基准 → 证明无前期 reversal、无 openAmountFunctional 更新）。
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, new BigDecimal("9.0"));
            Long p2VoucherId = exchangeRevaluationService.revalue(loadPeriod(p2Id), CTX);
            assertNotNull(p2VoucherId, "P2 重估应生成 FX 凭证");
            ErpFinVoucher p2Voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(p2VoucherId);
            BigDecimal p2Diff = new BigDecimal("100");
            assertFxCumulativeStep(p2Voucher, p2Diff, "P2");

            // P1 FX 凭证未被冲销（无前期 reversal）。
            assertFalse(Boolean.TRUE.equals(p1Voucher.getIsReversed()),
                    "P1 FX 凭证未被冲销（documented simplification：无前期 reversal）");

            // openAmountFunctional 跨两次重估保持不变（documented simplification：不更新 openAmountFunctional）。
            app.erp.fin.dao.entity.ErpFinArApItem item = findArApItem("ARI-FX-CROSS-001");
            assertEquals(0, item.getOpenAmountFunctional().compareTo(openFunctional),
                    "openAmountFunctional 跨期重估保持不变（documented simplification）");
            assertEquals(0, item.getOpenAmountSource().compareTo(openSource),
                    "openAmountSource 跨期重估保持不变");

            // 累计 FX 入账 vs 真实累计变动（characterize documented 累计漂移，P1-MA2-022 测试可见性）。
            BigDecimal cumulativeBooked = p1Diff.add(p2Diff);
            BigDecimal trueCumulativeMovement = openSource.multiply(new BigDecimal("9.0")).subtract(openFunctional);
            output("1_cross_period_fx_summary.json5", crossPeriodFxSummary(
                    p1Diff, p2Diff, cumulativeBooked, trueCumulativeMovement,
                    cumulativeBooked.subtract(trueCumulativeMovement)));
            assertEquals(0, cumulativeBooked.compareTo(new BigDecimal("150")), "累计 FX 入账 = P1(50)+P2(100)");
            assertEquals(0, trueCumulativeMovement.compareTo(new BigDecimal("100")),
                    "真实累计变动 = 100×9.0 − 800 = 100");
            assertTrue(cumulativeBooked.compareTo(trueCumulativeMovement) > 0,
                    "累计入账(150) > 真实累计变动(100)：documented 累计漂移 50（P1-MA2-022 可见）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, originalRate);
        }
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        return seedOpenPeriod(code, year, month,
                LocalDate.of(year, month, 1), LocalDate.of(year, month, 28));
    }

    private void seedCurrency(Long id, String code, boolean functional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private void seedOpenArAp(String code, Long periodId, LocalDate date, String direction,
                             Long currencyId, BigDecimal openSource, BigDecimal openFunctional) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinArApItem> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinArApItem.class);
        app.erp.fin.dao.entity.ErpFinArApItem item = new app.erp.fin.dao.entity.ErpFinArApItem();
        item.setCode(code);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(1L);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(code);
        item.setBusinessDate(date);
        item.setCurrencyId(currencyId);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(openSource);
        item.setAmountFunctional(openFunctional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openSource);
        item.setOpenAmountFunctional(openFunctional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
    }

    private ErpFinAccountingPeriod loadPeriod(Long periodId) {
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream().filter(l -> subjectCode.equals(l.getSubjectCode())).findFirst().orElseThrow();
    }

    private app.erp.fin.dao.entity.ErpFinArApItem findArApItem(String code) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinArApItem> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<app.erp.fin.dao.entity.ErpFinArApItem> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 断言单期 FX 重估凭证（应收收益方向）：应收升值 → 借应收 / 贷汇兑损益，借贷平衡且等于 diff，
     * FX 凭证行级 amountFunctional == diff（FX 凭证为本位币凭证，amountSource==amountFunctional==diff）。
     */
    private void assertFxCumulativeStep(ErpFinVoucher voucher, BigDecimal diff, String label) {
        assertNotNull(voucher, label + " FX 凭证应存在");
        assertTrue(voucher.getTotalDebit().compareTo(voucher.getTotalCredit()) == 0,
                label + " FX 凭证借贷平衡");
        assertEquals(0, voucher.getTotalDebit().compareTo(diff), label + " 重估差额 = " + diff);

        List<ErpFinVoucherLine> lines = linesOf(voucher.getId());
        ErpFinVoucherLine arLine = lineOfSubject(lines, "1122");
        assertEquals(ErpFinConstants.DC_DEBIT, arLine.getDcDirection(), label + " 应收升值借记应收");
        assertEquals(0, arLine.getAmountFunctional().compareTo(diff),
                label + " AR 行 amountFunctional = diff");
        ErpFinVoucherLine fxLine = lineOfSubject(lines, "6603");
        assertEquals(ErpFinConstants.DC_CREDIT, fxLine.getDcDirection(), label + " 收益贷记汇兑损益");
        assertEquals(0, fxLine.getAmountFunctional().compareTo(diff),
                label + " FX 行 amountFunctional = diff");
        // FX 凭证为本位币凭证：exchangeRate=ONE，amountSource==amountFunctional。
        assertEquals(0, fxLine.getExchangeRate().compareTo(BigDecimal.ONE),
                label + " FX 凭证 exchangeRate = ONE（本位币凭证）");
        assertEquals(0, fxLine.getAmountSource().compareTo(fxLine.getAmountFunctional()),
                label + " FX 凭证 amountSource == amountFunctional");
    }

    private java.util.Map<String, Object> crossPeriodFxSummary(BigDecimal p1Diff, BigDecimal p2Diff,
                                                                BigDecimal cumulativeBooked,
                                                                BigDecimal trueCumulativeMovement,
                                                                BigDecimal drift) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("p1Diff", p1Diff);
        m.put("p2Diff", p2Diff);
        m.put("cumulativeBooked", cumulativeBooked);
        m.put("trueCumulativeMovement", trueCumulativeMovement);
        m.put("documentedDrift", drift);
        return m;
    }
}
