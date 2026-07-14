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
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多套账（并行账簿）传播集成测试（L-1，杠杆 B）。验证 {@code SchemaPropagator.resolveTargetSchemas} +
 * {@code ErpFinPostingProcessor.process} 的账套循环 + 科目翻译回退行为。
 *
 * <p>覆盖 plan {@code 2026-07-13-0701-1} Phase 1：一笔业务事件在启用 {@code erp-fin.multi-schema-enabled}
 * 且源账套 {@code isPropagate=true} 时自动传播到同组织全部账套，每账套各生成一张凭证。
 *
 * <p>权威：{@code docs/design/finance/multiple-accounting-schemas.md}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinMultiSchemaPosting extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long PRIMARY_SCHEMA_ID = 8001L;   // FINANCIAL 主账套
    static final Long SECONDARY_SCHEMA_ID = 8002L; // MANAGEMENT 账套
    static final Long CURRENCY_ID = 1L;

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @AfterEach
    void resetMultiSchemaConfig() {
        // 复位为生产默认 false，避免跨方法泄漏（每个 @Test 独立 H2 库，但配置容器共享）
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpFinConstants.CONFIG_MULTI_SCHEMA_ENABLED, "false");
    }

    @Test
    public void testMultiSchemaPropagatesTwoVouchersWithDistinctSchema() {
        seedBaseline(true);

        PostingEvent event = apInvoiceEvent("AP-MS-PROP-001", PRIMARY_SCHEMA_ID);

        Long primaryVoucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(primaryVoucherId, "主账套应返回凭证 ID");

        List<ErpFinVoucher> vouchers = findVouchers("AP-MS-PROP-001");
        assertEquals(2, vouchers.size(), "启用多套账 + isPropagate 应生成 2 张凭证（1 事件→N 账套 N 凭证）");

        boolean hasPrimary = vouchers.stream().anyMatch(v -> PRIMARY_SCHEMA_ID.equals(v.getAcctSchemaId()));
        boolean hasSecondary = vouchers.stream().anyMatch(v -> SECONDARY_SCHEMA_ID.equals(v.getAcctSchemaId()));
        assertTrue(hasPrimary, "应存在主账套（FINANCIAL）凭证");
        assertTrue(hasSecondary, "应存在次账套（MANAGEMENT）凭证");

        vouchers.forEach(v ->
                assertEquals(VOUCHER_STATUS_POSTED, v.getDocStatus(), "每张凭证均应为已过账"));
    }

    @Test
    public void testNonPropagatePrimaryProducesSingleVoucher() {
        // 主账套 isPropagate=false → 即使启用多套账也仅主账套 1 张凭证
        seedBaseline(false);

        PostingEvent event = apInvoiceEvent("AP-MS-NOPROP-001", PRIMARY_SCHEMA_ID);

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "应生成主账套凭证");

        List<ErpFinVoucher> vouchers = findVouchers("AP-MS-NOPROP-001");
        assertEquals(1, vouchers.size(), "isPropagate=false 仅生成 1 张凭证");
        assertEquals(PRIMARY_SCHEMA_ID, vouchers.get(0).getAcctSchemaId(), "唯一凭证属于主账套");
    }

    @Test
    public void testMultiSchemaDisabledProducesSingleVoucher() {
        // 关闭多套账开关 → 向后兼容：仅主账套 1 张凭证（即使 isPropagate=true）
        seedBaseline(true);
        setMultiSchemaEnabled(false);

        PostingEvent event = apInvoiceEvent("AP-MS-DISABLED-001", PRIMARY_SCHEMA_ID);

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "应生成主账套凭证");

        List<ErpFinVoucher> vouchers = findVouchers("AP-MS-DISABLED-001");
        assertEquals(1, vouchers.size(), "multi-schema-enabled=false 向后兼容仅 1 张凭证");
        assertEquals(PRIMARY_SCHEMA_ID, vouchers.get(0).getAcctSchemaId());
    }

    @Test
    public void testSecondaryVoucherFallsBackToSourceSubjectWhenNoMapping() {
        seedBaseline(true);

        PostingEvent event = apInvoiceEvent("AP-MS-SUBJ-001", PRIMARY_SCHEMA_ID);
        ormTemplate.runInSession(() -> voucherBiz.post(event, CTX));

        ErpFinVoucher primary = findVoucher(PRIMARY_SCHEMA_ID, "AP-MS-SUBJ-001");
        ErpFinVoucher secondary = findVoucher(SECONDARY_SCHEMA_ID, "AP-MS-SUBJ-001");
        assertNotNull(primary, "主账套凭证存在");
        assertNotNull(secondary, "次账套凭证存在");

        // 主账套凭证使用原始科目（6602/2221/2202）
        assertTrue(hasVoucherLine(primary.getId(), "6602"), "主账套凭证含原始科目 6602");
        assertTrue(hasVoucherLine(primary.getId(), "2202"), "主账套凭证含原始科目 2202");

        // 无 COA 映射时次账套凭证回退源科目（同科目表场景），仍保持借贷平衡
        assertTrue(hasVoucherLine(secondary.getId(), "6602"), "无映射时次账套回退源科目 6602");
        assertEquals(0, secondary.getTotalDebit().compareTo(secondary.getTotalCredit()),
                "次账套凭证借贷平衡");
        assertEquals(0, secondary.getTotalDebit().compareTo(new BigDecimal("113")),
                "次账套凭证借方合计 113（与主账套同额）");
        assertFalse(primary.getId().equals(secondary.getId()), "主/次凭证为不同实体");
    }

    // ---------- helpers: seed ----------

    private void seedBaseline(boolean primaryPropagate) {
        setMultiSchemaEnabled(true);
        ormTemplate.runInSession(() -> {
            seedAcctSchema(PRIMARY_SCHEMA_ID, "FIN", "财务账套", "FINANCIAL", primaryPropagate);
            seedAcctSchema(SECONDARY_SCHEMA_ID, "MGT", "管理账套", "MANAGEMENT", false);
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
            seedSubject("6602", "管理费用", "EXPENSE", "DEBIT");
            seedSubject("2221", "应交税费-进项税", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
            seedApInvoiceTemplate();
        });
    }

    private void seedAcctSchema(Long id, String code, String name, String nature, boolean propagate) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.orm_propValueByName("id", id);
        schema.setCode(code);
        schema.setName(name);
        schema.setOrgId(ORG_ID);
        schema.setNature(nature);
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setIsPropagate(propagate);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedApInvoiceTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-AP-INVOICE");
        tpl.setName("应付发票模板");
        tpl.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
        tpl.setVoucherType(VOUCHER_TYPE_TRANSFER);
        tpl.setIsActive(true);
        dao.saveEntity(tpl);

        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(templateLine(tpl.getId(), 1, "6602", DC_DEBIT, "AMOUNT", "EXPENSE"));
        lineDao.saveEntity(templateLine(tpl.getId(), 2, "2221", DC_DEBIT, "TAX", "INPUT_TAX"));
        lineDao.saveEntity(templateLine(tpl.getId(), 3, "2202", DC_CREDIT, "TOTAL", "AP"));
    }

    private ErpFinVoucherTemplateLine templateLine(Long templateId, int lineNo, String subjectCode,
                                                   String dcDirection, String amountKey, String accountKey) {
        ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setSubjectCode(subjectCode);
        line.setDcDirection(dcDirection);
        line.setAmountKey(amountKey);
        line.setAccountKey(accountKey);
        return line;
    }

    private PostingEvent apInvoiceEvent(String billHeadCode, Long acctSchemaId) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(acctSchemaId);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_ID);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(LocalDate.of(2026, 7, 15));
        event.getBillData().put("AMOUNT", new BigDecimal("100"));
        event.getBillData().put("TAX", new BigDecimal("13"));
        event.getBillData().put("TOTAL", new BigDecimal("113"));
        event.getBillData().put("partnerId", 1L);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 15));
        return event;
    }

    private void setMultiSchemaEnabled(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpFinConstants.CONFIG_MULTI_SCHEMA_ENABLED, String.valueOf(enabled));
    }

    // ---------- helpers: queries ----------

    private List<ErpFinVoucher> findVouchers(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", BUSINESS_TYPE_AP_INVOICE));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        return links.stream()
                .map(l -> voucherDao.getEntityById(l.getVoucherId()))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private ErpFinVoucher findVoucher(Long acctSchemaId, String billCode) {
        return findVouchers(billCode).stream()
                .filter(v -> acctSchemaId.equals(v.getAcctSchemaId()))
                .findFirst().orElse(null);
    }

    private boolean hasVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        return !dao.findAllByQuery(q).isEmpty();
    }
}
