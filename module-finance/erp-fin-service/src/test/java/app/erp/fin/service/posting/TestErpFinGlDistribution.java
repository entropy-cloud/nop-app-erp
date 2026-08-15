package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GlDistribution 科目分摊 FactsValidator 测试（RC-R1.41 / P1-RC-001，L1 UC-FIN-04/15）。
 *
 * <p>测试策略（plan 2026-08-15-1838-1 Phase 2 记录选择）：引擎级容器测试——经
 * {@code @NopTestConfig(testBeansFile="/erp/fin/beans/test-gl-distribution.beans.xml")} 注入
 * 测试专用规则 Validator（test 前缀 bean id，与生产空规则表 Validator 一同被 registry 聚合），
 * 再调 {@link IErpFinVoucherBiz#post} 断言过账引擎链路行为（拆行/拒绝/透传均发生在
 * {@code generateFacts} 的 Validator 链消费点，凭证落库行为可断言）。直断言范式（R1.32），不录快照。
 *
 * <p>覆盖（对齐 plan Phase 2 测试矩阵）：
 * <ol>
 *   <li>命中规则 60/40 拆行 + 金额守恒 + costCenterId 正确；</li>
 *   <li>Σpercent=95 拒绝（错误码 + 凭证/回链零落库）；</li>
 *   <li>无规则命中原样透传（零回归）；</li>
 *   <li>规则载体 Bean 静态表加载路径（testBeansFile 注入的规则可经容器取回断言）。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/fin/beans/test-gl-distribution.beans.xml")
public class TestErpFinGlDistribution extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    // ---------- ① 命中规则拆行 ----------

    @Test
    public void testSplitRuleSplitsFactByPercent() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            // 6602 命中 TEST-SPLIT-60-40 规则（6602 → 101:60% / 102:40%）
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate("6602", "2221", "2202");
        });

        PostingEvent event = apInvoiceEvent("GL-SPLIT-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "分摊命中应正常过账");

        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        // 模板 3 行 → 6602 行拆为 2 行 → 共 4 行
        assertEquals(4, lines.size(), "6602 行应拆为 2 行，总行数 3→4");

        ErpFinVoucherLine line101 = lineOfCostCenter(lines, 101L);
        ErpFinVoucherLine line102 = lineOfCostCenter(lines, 102L);
        assertNotNull(line101, "目标成本中心 101 行应存在");
        assertNotNull(line102, "目标成本中心 102 行应存在");
        assertEquals("6602", line101.getSubjectCode(), "拆分行科目不变");
        assertEquals("6602", line102.getSubjectCode(), "拆分行科目不变");
        assertTrue(line101.getDebitAmount().compareTo(new BigDecimal("60.0000")) == 0,
                "101 行金额 = 100 × 60% = 60");
        assertTrue(line102.getDebitAmount().compareTo(new BigDecimal("40.0000")) == 0,
                "102 行金额 = 100 × 40% = 40");
        // 平衡保持：拆分行金额 Σ == 原行金额 100
        BigDecimal splitSum = line101.getDebitAmount().add(line102.getDebitAmount());
        assertTrue(splitSum.compareTo(new BigDecimal("100.0000")) == 0, "拆分行 Σ == 原行金额（平衡保持）");

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(voucherId);
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0, "借方合计不变 113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0, "贷方合计不变 113");
    }

    // ---------- ② Σpercent != 100 拒绝 ----------

    @Test
    public void testPercentSumNot100RejectsPosting() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            // 9901 命中 TEST-REJECT-95 规则（Σ=95 ≠ 100 → 拒绝）
            seedSubject("9901", "测试费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate("9901", "2221", "2202");
        });

        PostingEvent event = apInvoiceEvent("GL-REJECT-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)),
                "Σpercent=95 应抛 NopException 拒绝过账");
        assertEquals(ErpFinErrors.ERR_GL_DISTRIBUTION_PERCENT_SUM.getErrorCode(), ex.getErrorCode(),
                "错误码应为 ERR_GL_DISTRIBUTION_PERCENT_SUM");
        assertEquals("TEST-REJECT-95", ex.getParam(ErpFinErrors.ARG_RULE_CODE), "错误参数含 ruleCode");
        assertEquals("95", ex.getParam(ErpFinErrors.ARG_PERCENT_SUM).toString(), "错误参数含实际 Σ");
        assertEquals(0, countBillLinks("GL-REJECT-001", ErpFinBusinessType.AP_INVOICE.name()), "被拒不应落库回链");
        assertEquals(0, countVouchersOfBill("GL-REJECT-001"), "被拒不应落库凭证");
    }
    // ---------- ③ 无规则命中透传 ----------

    @Test
    public void testNoRuleMatchPassThrough() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            // 7701 无任何规则命中 → 原样透传（对照断言）
            seedSubject("7701", "无分摊费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate("7701", "2221", "2202");
        });

        PostingEvent event = apInvoiceEvent("GL-PASS-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "无规则命中应正常过账");
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        assertEquals(3, lines.size(), "无规则命中应原样透传（3 行不变）");
        for (ErpFinVoucherLine line : lines) {
            assertNull(line.getCostCenterId(), "透传行 costCenterId 应为 null（未拆行）");
        }
    }

    // ---------- ④ 规则载体 Bean 静态表加载 ----------

    @Test
    public void testRuleCarrierLoadedFromBeans() {
        ErpFinGlDistributionValidator validator = (ErpFinGlDistributionValidator) io.nop.api.core.ioc.BeanContainer.instance()
                .getBean("testGlDistributionValidator");
        assertNotNull(validator, "testBeansFile 注入的 Validator bean 应可经容器取回");
        assertEquals(ErpFinGlDistributionValidator.ORDER, validator.getOrder(), "getOrder 应为较高值 100");
        List<ErpFinGlDistributionRule> rules = validator.getRules();
        assertNotNull(rules);
        assertEquals(2, rules.size(), "规则表应含 2 条规则（Bean 静态表加载路径）");
        ErpFinGlDistributionRule split = rules.get(0);
        assertEquals("TEST-SPLIT-60-40", split.getRuleCode());
        assertEquals("6602", split.getSourceSubjectCode());
        assertTrue(split.isActive(), "默认启用态");
        assertEquals(2, split.getTargets().size(), "60/40 两条目标行");
        assertTrue(split.getTargets().get(0).getPercent().compareTo(new BigDecimal("60")) == 0, "目标行 60%");
        assertTrue(split.getTargets().get(1).getPercent().compareTo(new BigDecimal("40")) == 0, "目标行 40%");
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private PostingEvent apInvoiceEvent(String billHeadCode, LocalDate voucherDate, BigDecimal amount,
                                        BigDecimal tax, BigDecimal total) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(1L);
        event.setOrgId(1L);
        event.setCurrencyId(1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("AMOUNT", amount);
        event.getBillData().put("TAX", tax);
        event.getBillData().put("TOTAL", total);
        event.getBillData().put("partnerId", 1L);
        event.getBillData().put("businessDate", voucherDate);
        return event;
    }

    private void seedApInvoiceTemplate(String expenseSubject, String taxSubject, String apSubject) {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-GL-TEST");
        tpl.setName("分摊测试模板");
        tpl.setBusinessType(ErpFinBusinessType.AP_INVOICE.name());
        tpl.setVoucherType("TRANSFER");
        tpl.setIsActive(true);
        dao.saveEntity(tpl);

        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(templateLine(tpl.getId(), 1, expenseSubject, DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(templateLine(tpl.getId(), 2, taxSubject, DC_DEBIT, "TAX"));
        lineDao.saveEntity(templateLine(tpl.getId(), 3, apSubject, DC_CREDIT, "TOTAL"));
    }

    private ErpFinVoucherTemplateLine templateLine(Long templateId, int lineNo, String subjectCode,
                                                   String dcDirection, String amountKey) {
        ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setSubjectCode(subjectCode);
        line.setDcDirection(dcDirection);
        line.setAmountKey(amountKey);
        return line;
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedOpenPeriod(LocalDate voucherDate) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-06");
        period.setName("2026-06");
        period.setOrgId(1L);
        period.setYear(2026);
        period.setMonth(6);
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = new java.util.ArrayList<>(dao.findAllByQuery(q));
        lines.sort(java.util.Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));
        return lines;
    }

    private ErpFinVoucherLine lineOfCostCenter(List<ErpFinVoucherLine> lines, Long costCenterId) {
        return lines.stream().filter(l -> costCenterId.equals(l.getCostCenterId())).findFirst().orElse(null);
    }

    private long countBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        return dao.findAllByQuery(q).size();
    }

    private long countVouchersOfBill(String billCode) {
        // 凭证与回链在 persistVoucher 内同事务成对落库：零回链 = 凭证零落库
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        return dao.findAllByQuery(q).size();
    }
}
