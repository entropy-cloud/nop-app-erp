package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IEntityDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多账套结账写路径隔离测试（plan 2026-08-28-2054-2 Phase 2，P1-CK-fin4-002）：
 * <ul>
 *   <li>损益结转：主账套 isPropagate=true + 多套账开关启用时，每账套结转凭证仅含<b>本账套</b>金额
 *       （修复前聚合无账套过滤，每账套凭证都含全域金额 N 倍重复入账）；</li>
 *   <li>年度结转：年初余额 populate 按账套维度 clear + 写入，每账套次年快照独立
 *       （修复前 clear 不带账套维度，多账套循环互删只余最后账套）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:annual-close-test.yaml")
public class TestErpFinClosingMultiSchema extends PeriodCloseTestSupport {

    private static final String PRIMARY_SCHEMA_ID = "8001";   // FINANCIAL 主账套（isPropagate=true）
    private static final String SECONDARY_SCHEMA_ID = "8002"; // MANAGEMENT 账套

    @AfterEach
    public void resetMultiSchemaConfig() {
        // 多套账开关恢复默认 false，避免泄漏到同 JVM 后续测试类。
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpFinConstants.CONFIG_MULTI_SCHEMA_ENABLED, "false");
    }

    /**
     * 损益结转每账套独立：同期间同科目两账套各 1 张凭证（收入 1000 / 3000），
     * 各凭证本年利润贷方 = 本账套金额（修复前每账套均为全域合计 4000）。
     */
    @Test
    public void testProfitLossClosePerSchemaIsolation() {
        enableMultiSchemaAndSeedSchemas();
        Map<String, ErpMdSubject> subjects = new HashMap<>();
        ormTemplate.runInSession(session -> {
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subjects.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedCurrency("1", "CNY", true);
            String pid = seedOpenPeriod("2026-07", 2026, 7);
            // 主账套 8001：收入 1000；管理账套 8002：收入 3000（同科目同期间）。
            seedPostedVoucherInSchema("V-MS-PRI", pid, LocalDate.of(2026, 7, 10), PRIMARY_SCHEMA_ID, subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("1000")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("1000")});
            seedPostedVoucherInSchema("V-MS-SEC", pid, LocalDate.of(2026, 7, 11), SECONDARY_SCHEMA_ID, subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("3000")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("3000")});
            return pid;
        });
        String periodId = findPeriodId("2026-07");

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 每账套 1 张损益结转凭证（共 2 张）。
        List<ErpFinVoucher> plVouchers = findCloseVouchers("PERIOD-CLOSE-2026-07",
                ErpFinBusinessType.PERIOD_CLOSE.name());
        assertEquals(2, plVouchers.size(), "多账套模式应生成 2 张损益结转凭证（每账套 1 张）");

        ErpFinVoucher priVoucher = voucherOfSchema(plVouchers, PRIMARY_SCHEMA_ID);
        ErpFinVoucher secVoucher = voucherOfSchema(plVouchers, SECONDARY_SCHEMA_ID);
        assertTrue(priVoucher != null && secVoucher != null, "主/管理账套各有 1 张结转凭证");

        // 主账套凭证本年利润贷方 = 1000（本账套金额，非全域 4000）。
        assertEquals(0, cypCreditOf(priVoucher).compareTo(new BigDecimal("1000")),
                "主账套结转凭证本年利润贷方应为 1000（仅本账套金额），实际 " + cypCreditOf(priVoucher));
        // 管理账套凭证本年利润贷方 = 3000（本账套金额，非全域 4000）。
        assertEquals(0, cypCreditOf(secVoucher).compareTo(new BigDecimal("3000")),
                "管理账套结转凭证本年利润贷方应为 3000（仅本账套金额），实际 " + cypCreditOf(secVoucher));
    }

    /**
     * 年度结转每账套独立：12 月两账套净利润 600 / 150 → 次年 1 月年初余额快照两账套各自独立存在
     * （修复前 populate clear 无账套维度，后一账套删除前一账套快照，只余最后账套）。
     */
    @Test
    public void testAnnualClosePopulatePerSchemaIndependent() {
        enableMultiSchemaAndSeedSchemas();
        Map<String, ErpMdSubject> subjects = new HashMap<>();
        ormTemplate.runInSession(session -> {
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subjects.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subjects.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            subjects.put("4104", seedSubject("4104", "未分配利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedCurrency("1", "CNY", true);
            String pid = seedOpenPeriod("2025-12", 2025, 12);
            // 主账套 8001：净利 600；管理账套 8002：净利 150。
            seedPostedVoucherInSchema("V-DEC-PRI-INC", pid, LocalDate.of(2025, 12, 10), PRIMARY_SCHEMA_ID, subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("1000")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("1000")});
            seedPostedVoucherInSchema("V-DEC-PRI-EXP", pid, LocalDate.of(2025, 12, 11), PRIMARY_SCHEMA_ID, subjects,
                    new Object[]{"6601", "销售费用", ErpFinConstants.DC_DEBIT, new BigDecimal("400")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("400")});
            seedPostedVoucherInSchema("V-DEC-SEC-INC", pid, LocalDate.of(2025, 12, 12), SECONDARY_SCHEMA_ID, subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("200")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("200")});
            seedPostedVoucherInSchema("V-DEC-SEC-EXP", pid, LocalDate.of(2025, 12, 13), SECONDARY_SCHEMA_ID, subjects,
                    new Object[]{"6601", "销售费用", ErpFinConstants.DC_DEBIT, new BigDecimal("50")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("50")});
            return pid;
        });
        String periodId = findPeriodId("2025-12");

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 年度结转凭证每账套 1 张：4104 贷方 600 / 150。
        List<ErpFinVoucher> annualVouchers = findCloseVouchers("ANNUAL-CLOSE-2025-12",
                ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name());
        assertEquals(2, annualVouchers.size(), "年度结转应生成 2 张凭证（每账套 1 张）");
        assertEquals(0, retainedCreditOf(annualVouchers, PRIMARY_SCHEMA_ID).compareTo(new BigDecimal("600")),
                "主账套年度结转未分配利润贷方应为 600，实际 " + retainedCreditOf(annualVouchers, PRIMARY_SCHEMA_ID));
        assertEquals(0, retainedCreditOf(annualVouchers, SECONDARY_SCHEMA_ID).compareTo(new BigDecimal("150")),
                "管理账套年度结转未分配利润贷方应为 150，实际 " + retainedCreditOf(annualVouchers, SECONDARY_SCHEMA_ID));

        // 次年 1 月年初余额快照两账套独立共存（P1-CK-fin4-002 核心：修复前 clear 无账套维度，
        // 8002 的 populate 循环删除 8001 快照，只余最后账套）。
        // 注：快照聚合排除 PROFIT_TO_RETAINED_EARNINGS 自身分录，故断言经营性科目 1001（库存现金）
        // 的年初净借方：8001 = 1000−400 = 600；8002 = 200−50 = 150。
        ErpFinAccountingPeriod nextJan = findPeriod("2026-01");
        assertTrue(nextJan != null, "次年 1 月期间已自动创建");
        assertEquals(0, yearOpeningDebitOf(nextJan.getId(), PRIMARY_SCHEMA_ID, "1001").compareTo(new BigDecimal("600")),
                "主账套次年 1 月 1001 年初借方应为 600，实际 " + yearOpeningDebitOf(nextJan.getId(), PRIMARY_SCHEMA_ID, "1001"));
        assertEquals(0, yearOpeningDebitOf(nextJan.getId(), SECONDARY_SCHEMA_ID, "1001").compareTo(new BigDecimal("150")),
                "管理账套次年 1 月 1001 年初借方应为 150（两账套快照独立共存，修复前为 0），实际 "
                        + yearOpeningDebitOf(nextJan.getId(), SECONDARY_SCHEMA_ID, "1001"));
    }

    // ---------- helpers: config + seed ----------

    private void enableMultiSchemaAndSeedSchemas() {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpFinConstants.CONFIG_MULTI_SCHEMA_ENABLED, "true");
        ormTemplate.runInSession(session -> {
            seedAcctSchema(PRIMARY_SCHEMA_ID, "FIN", "财务账套", "FINANCIAL", true);
            seedAcctSchema(SECONDARY_SCHEMA_ID, "MGT", "管理账套", "MANAGEMENT", false);
            return null;
        });
    }

    private void seedAcctSchema(String id, String code, String name, String nature, boolean propagate) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.orm_propValueByName("id", id);
        schema.setCode(code);
        schema.setName(name);
        schema.setOrgId("1");
        schema.setNature(nature);
        schema.setFunctionalCurrencyId("1");
        schema.setIsPropagate(propagate);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedPostedVoucherInSchema(String vcode, String periodId, LocalDate date, String acctSchemaId,
                                           Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(vcode);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId("1");
        v.setAcctSchemaId(acctSchemaId);
        v.setPeriodId(periodId);
        v.setTotalDebit(total);
        v.setTotalCredit(total);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        int lineNo = 1;
        for (Object[] l : lines) {
            ErpMdSubject subj = subjects.get((String) l[0]);
            String dc = (String) l[2];
            BigDecimal amt = (BigDecimal) l[3];
            ErpFinVoucherLine line = new ErpFinVoucherLine();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(subj.getId());
            line.setSubjectCode((String) l[0]);
            line.setSubjectName((String) l[1]);
            line.setDcDirection(dc);
            line.setDebitAmount(ErpFinConstants.DC_DEBIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCreditAmount(ErpFinConstants.DC_CREDIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCurrencyId("1");
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(acctSchemaId);
            lDao.saveEntity(line);
        }
    }

    // ---------- helpers: queries + assertions ----------

    private String findPeriodId(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q).get(0).getId();
    }

    private ErpFinAccountingPeriod findPeriod(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpFinAccountingPeriod> list = daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucher> findCloseVouchers(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        List<ErpFinVoucher> vouchers = new ArrayList<>();
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR r : dao.findAllByQuery(q)) {
            ErpFinVoucher v = vDao.getEntityById(r.getVoucherId());
            if (v != null) {
                vouchers.add(v);
            }
        }
        return vouchers;
    }

    private ErpFinVoucher voucherOfSchema(List<ErpFinVoucher> vouchers, String acctSchemaId) {
        return vouchers.stream()
                .filter(v -> acctSchemaId.equals(v.getAcctSchemaId()))
                .findFirst().orElse(null);
    }

    /** 凭证中本年利润（4103）科目的贷方合计。 */
    private BigDecimal cypCreditOf(ErpFinVoucher voucher) {
        return creditOfSubject(voucher.getId(), "4103");
    }

    /** 指定账套年度结转凭证中未分配利润（4104）科目的贷方合计。 */
    private BigDecimal retainedCreditOf(List<ErpFinVoucher> vouchers, String acctSchemaId) {
        ErpFinVoucher v = voucherOfSchema(vouchers, acctSchemaId);
        return v == null ? BigDecimal.ZERO : creditOfSubject(v.getId(), "4104");
    }

    private BigDecimal creditOfSubject(String voucherId, String subjectCode) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectId", s.getId()));
        BigDecimal credit = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q)) {
            credit = credit.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
        }
        return credit;
    }

    /** 次年 1 月指定账套 + 科目的年初借方余额（无行时返回 0）。 */
    private BigDecimal yearOpeningDebitOf(String nextJanPeriodId, String acctSchemaId, String subjectCode) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", nextJanPeriodId));
        q.addFilter(eq("acctSchemaId", acctSchemaId));
        q.addFilter(eq("subjectId", s.getId()));
        List<ErpFinGlBalance> list = daoProvider.daoFor(ErpFinGlBalance.class).findAllByQuery(q);
        return list.isEmpty() ? BigDecimal.ZERO
                : (list.get(0).getYearOpeningDebit() != null ? list.get(0).getYearOpeningDebit() : BigDecimal.ZERO);
    }
}
