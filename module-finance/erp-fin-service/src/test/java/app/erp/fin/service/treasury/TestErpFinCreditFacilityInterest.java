package app.erp.fin.service.treasury;

import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.fin.service.posting.provider.CreditFacilityInterestAcctDocProvider;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授信利息计提端到端集成测试（plan 2026-07-18-0718-1，Phase 2）。覆盖 {@link IErpFinCreditFacilityBiz#accrueInterest}
 * 全路径：
 * <ul>
 *   <li>正路径（计息公式 usedAmount × rate × 闭区间天数 / 360 + 凭证生成 + Dr 6603 / Cr 1002 精确数值 + facility 不变）</li>
 *   <li>幂等命中（同 facility + 同区间二次调用返回 null + 仅 1 张凭证 + facility 字段不变）</li>
 *   <li>usedAmount=0 空操作（业务前置不满足 → 返回 null + 无凭证生成）</li>
 *   <li>rate=0 抛守卫（ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED + 无凭证生成）</li>
 *   <li>非法日期区间守卫（ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE + 无凭证生成）</li>
 *   <li>Provider 单元测试（createFacts 返回 Dr 6603 / Cr 1002 + 非支持类型返回空列表）</li>
 * </ul>
 *
 * <p>{@code testConfigFile} 提供 {@code erp-fin.credit-facility-default-interest-rate=0.05}（5% 年化）。
 * rate=0 守卫测试经 {@code AppConfig.getConfigProvider().assignConfigValue(...)} 临时覆盖并 finally 恢复。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:credit-facility-interest-test.yaml")
public class TestErpFinCreditFacilityInterest extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String POSTING_TYPE_NORMAL = ErpFinConstants.POSTING_TYPE_NORMAL;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinCreditFacilityBiz creditFacilityBiz;

    @Test
    public void testAccrueInterestHappyPath() {
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            Long facilityId = seedCreditFacility("CF-INT-001",
                    new BigDecimal("300"), new BigDecimal("1000"));
            return new Long[]{facilityId};
        });
        Long facilityId = ids[0];

        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long voucherId = ormTemplate.runInSession(session ->
                creditFacilityBiz.accrueInterest(facilityId, from, to, CTX));

        assertNotNull(voucherId, "正路径应生成并返回凭证 ID");
        output("1_voucher_id.json5", voucherId);

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(voucherId);
        assertEquals(POSTING_TYPE_NORMAL, voucher.getPostingType(), "凭证 postingType=NORMAL");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=POSTED");
        assertEquals(false, voucher.getIsReversed(), "非红字凭证");
        String expectedBillHeadCode = "CFI-INT-" + facilityId + "-" + from + "_" + to;
        assertEquals(expectedBillHeadCode, findBillHeadCode(voucherId, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()),
                "billHeadCode 区间级幂等键格式");

        // 300 × 0.05 × 31 / 360 = 1.2917（HALF_UP scale=4）
        BigDecimal expectedInterest = new BigDecimal("1.2917");
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        output("2_voucher_lines.json5", lines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
        ErpFinVoucherLine dr = lineOfSubject(lines, "6603");
        assertEquals(DC_DEBIT, dr.getDcDirection(), "借方 6603 财务费用-利息支出");
        assertEquals(0, expectedInterest.compareTo(dr.getDebitAmount()), "借方金额 1.2917");
        ErpFinVoucherLine cr = lineOfSubject(lines, "1002");
        assertEquals(DC_CREDIT, cr.getDcDirection(), "贷方 1002 银行存款");
        assertEquals(0, expectedInterest.compareTo(cr.getCreditAmount()), "贷方金额 1.2917");
        assertEquals(0, voucher.getTotalDebit().compareTo(expectedInterest), "借方合计");
        assertEquals(0, voucher.getTotalCredit().compareTo(expectedInterest), "贷方合计");

        // facility 三值在计提后不变（计息非额度占用，仅生成凭证）
        ErpFinCreditFacility after = reloadFacility(facilityId);
        assertEquals(0, new BigDecimal("300").compareTo(after.getUsedAmount()), "usedAmount 不变");
        assertEquals(0, new BigDecimal("1000").compareTo(after.getTotalAmount()), "totalAmount 不变");
    }

    @Test
    public void testIdempotentSecondCallReturnsNull() {
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            Long facilityId = seedCreditFacility("CF-INT-002",
                    new BigDecimal("300"), new BigDecimal("1000"));
            return new Long[]{facilityId};
        });
        Long facilityId = ids[0];

        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long firstVoucherId = ormTemplate.runInSession(session ->
                creditFacilityBiz.accrueInterest(facilityId, from, to, CTX));
        Long secondVoucherId = ormTemplate.runInSession(session ->
                creditFacilityBiz.accrueInterest(facilityId, from, to, CTX));

        assertNotNull(firstVoucherId, "首次调用应生成凭证");
        assertNull(secondVoucherId, "二次同参数调用幂等命中返回 null");

        // 仅 1 张 (billHeadCode, businessType) 回链
        String billHeadCode = "CFI-INT-" + facilityId + "-" + from + "_" + to;
        assertEquals(1, countBillLinks(billHeadCode, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()),
                "幂等键命中后无第二张凭证生成");
        output("1_idempotent_links.json5", countBillLinks(billHeadCode, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()));

        // facility 不变
        ErpFinCreditFacility after = reloadFacility(facilityId);
        assertEquals(0, new BigDecimal("300").compareTo(after.getUsedAmount()), "facility.usedAmount 不变");
    }

    @Test
    public void testZeroUsedAmountReturnsNullNoVoucher() {
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            Long facilityId = seedCreditFacility("CF-INT-003",
                    BigDecimal.ZERO, new BigDecimal("1000"));
            return new Long[]{facilityId};
        });
        Long facilityId = ids[0];

        Long voucherId = ormTemplate.runInSession(session ->
                creditFacilityBiz.accrueInterest(facilityId,
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), CTX));

        assertNull(voucherId, "usedAmount=0 业务前置不满足 → 空操作返回 null");
        String prefix = "CFI-INT-" + facilityId + "-";
        assertEquals(0, countBillLinksByPrefix(prefix, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()),
                "无凭证生成");
    }

    @Test
    public void testRateZeroThrowsGuard() {
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            Long facilityId = seedCreditFacility("CF-INT-004",
                    new BigDecimal("300"), new BigDecimal("1000"));
            return new Long[]{facilityId};
        });
        Long facilityId = ids[0];

        BigDecimal originalRate = AppConfig.var(
                ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, BigDecimal.ZERO);
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, BigDecimal.ZERO);
            assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                            creditFacilityBiz.accrueInterest(facilityId,
                                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), CTX)),
                    "rate=0 应抛 ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, originalRate);
        }
        String prefix = "CFI-INT-" + facilityId + "-";
        assertEquals(0, countBillLinksByPrefix(prefix, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()),
                "rate=0 守卫拒绝时无凭证生成");
    }

    @Test
    public void testInvalidDateRangeThrowsGuard() {
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            Long facilityId = seedCreditFacility("CF-INT-005",
                    new BigDecimal("300"), new BigDecimal("1000"));
            return new Long[]{facilityId};
        });
        Long facilityId = ids[0];

        assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        creditFacilityBiz.accrueInterest(facilityId,
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1), CTX)),
                "fromDate > toDate 应抛 ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE");

        String prefix = "CFI-INT-" + facilityId + "-";
        assertEquals(0, countBillLinksByPrefix(prefix, ErpFinBusinessType.CREDIT_FACILITY_INTEREST.name()),
                "非法日期区间守卫拒绝时无凭证生成");
    }

    @Test
    public void testProviderCreateFactsDr6603Cr1002() {
        CreditFacilityInterestAcctDocProvider provider = new CreditFacilityInterestAcctDocProvider();

        assertTrue(provider.getSupportedBusinessTypes()
                        .contains(ErpFinBusinessType.CREDIT_FACILITY_INTEREST),
                "Provider 支持 CREDIT_FACILITY_INTEREST");

        BigDecimal interest = new BigDecimal("1.2917");
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.CREDIT_FACILITY_INTEREST);
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL", interest);
        event.setBillData(billData);

        List<VoucherFact> facts = provider.createFacts(event, new AcctDocContext());
        assertEquals(2, facts.size(), "返回 2 行分录");

        VoucherFact dr = facts.get(0);
        assertEquals("6603", dr.getSubjectCode(), "Dr 6603 财务费用-利息支出");
        assertEquals(DC_DEBIT, dr.getDcDirection(), "Dr 方向 DEBIT");
        assertEquals(0, interest.compareTo(dr.getAmount()), "Dr 金额 = TOTAL");
        output("1_dr_fact.json5", voucherFactState(dr));

        VoucherFact cr = facts.get(1);
        assertEquals("1002", cr.getSubjectCode(), "Cr 1002 银行存款");
        assertEquals(DC_CREDIT, cr.getDcDirection(), "Cr 方向 CREDIT");
        assertEquals(0, interest.compareTo(cr.getAmount()), "Cr 金额 = TOTAL");
        output("2_cr_fact.json5", voucherFactState(cr));

        // 非支持 businessType 返回空列表
        PostingEvent other = new PostingEvent();
        other.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        other.setBillData(new LinkedHashMap<>());
        assertTrue(provider.createFacts(other, new AcctDocContext()).isEmpty(),
                "非支持 businessType 返回空列表");
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-08", 2026, 8, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        seedAcctSchema(1L);
        seedSubject("6603", "财务费用-利息支出", ErpFinConstants.SUBJECT_CLASS_EXPENSE, DC_DEBIT);
        seedSubject("1002", "银行存款", "ASSET", DC_DEBIT);
    }

    private Long seedCreditFacility(String code, BigDecimal used, BigDecimal total) {
        IEntityDao<ErpFinFundAccount> faDao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = faDao.newEntity();
        account.setCode("FA-" + code);
        account.setName("Bank-" + code);
        account.setOrgId(1L);
        account.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        account.setCurrencyId(1L);
        account.setOpeningBalance(total);
        account.setCurrentBalance(total);
        account.setStatus("ACTIVE");
        faDao.saveEntity(account);

        IEntityDao<ErpFinCreditFacility> dao = daoProvider.daoFor(ErpFinCreditFacility.class);
        ErpFinCreditFacility facility = dao.newEntity();
        facility.setCode(code);
        facility.setOrgId(1L);
        facility.setFundAccountId(account.getId());
        facility.setFacilityType("BANK_ACCEPTANCE_LINE");
        facility.setTotalAmount(total);
        facility.setUsedAmount(used);
        facility.setAvailableAmount(total.subtract(used));
        facility.setValidFrom(LocalDate.of(2026, 1, 1));
        facility.setValidTo(LocalDate.of(2026, 12, 31));
        facility.setStatus("ACTIVE");
        dao.saveEntity(facility);
        return facility.getId();
    }

    private ErpFinCreditFacility reloadFacility(Long facilityId) {
        return daoProvider.daoFor(ErpFinCreditFacility.class).getEntityById(facilityId);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = dao.newEntity();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = dao.newEntity();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    // ---------- query helpers ----------

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream().filter(l -> subjectCode.equals(l.getSubjectCode())).findFirst().orElseThrow();
    }

    private long countBillLinks(String billHeadCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).size();
    }

    private long countBillLinksByPrefix(String prefix, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("businessType", businessType));
        return dao.findAllByQuery(q).stream()
                .filter(r -> r.getBillCode() != null && r.getBillCode().startsWith(prefix))
                .count();
    }

    private String findBillHeadCode(Long voucherId, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("voucherId", voucherId), eq("businessType", businessType)));
        q.setLimit(1);
        ErpFinVoucherBillR link = dao.findFirstByQuery(q);
        return link != null ? link.getBillCode() : null;
    }

    @SuppressWarnings("unused")
    private void unusedKeepImports() {
        assertFalse(false);
        assertTrue(true);
    }

    private Map<String, Object> voucherLineState(ErpFinVoucherLine l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subjectCode", l.getSubjectCode());
        m.put("dcDirection", l.getDcDirection());
        m.put("debitAmount", l.getDebitAmount());
        m.put("creditAmount", l.getCreditAmount());
        return m;
    }

    private Map<String, Object> voucherFactState(VoucherFact f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subjectCode", f.getSubjectCode());
        m.put("subjectName", f.getSubjectName());
        m.put("dcDirection", f.getDcDirection());
        m.put("amount", f.getAmount());
        m.put("businessType", f.getBusinessType());
        return m;
    }
}
