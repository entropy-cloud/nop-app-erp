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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财过账编排服务 + 默认模板 Provider 的服务层集成测试（Phase 2）。
 *
 * <p>{@code @NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)} + H2，
 * 直接调用凭证聚合根 Facade {@link IErpFinVoucherBiz} 的 Java API（过账 Facade 入口，不走 GraphQL 快照），
 * 断言实体落库状态而非响应 JSON。测试自包含：seed 期间+科目+模板 → 调 post() → 断言凭证/分录/回链。
 *
 * <p>覆盖：happy path（凭证+分录+回链落库、凭证 docStatus=POSTED、借贷平衡）、幂等（重复过账空操作）、
 * 借贷不平衡拒绝、期间已结账拒绝。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPostingService extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;
    static final String PERIOD_STATUS_CLOSED = ErpFinConstants.PERIOD_STATUS_CLOSED;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testPostHappyPath() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-HAPPY-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));

        assertNotNull(voucherId, "happy path 应生成并返回凭证 ID");
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher voucher = voucherDao.requireEntityById(voucherId);
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证应为已过账");
        assertEquals(false, voucher.getIsReversed(), "非红字凭证");
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0,
                "借方合计 113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0,
                "贷方合计 113");
        assertEquals(VOUCHER_TYPE_TRANSFER, voucher.getVoucherType(), "凭证字来自模板");

        assertEquals(3, countLines(voucherId), "应生成 3 行分录");
        assertEquals(1, countBillLinks("AP-HAPPY-001", BUSINESS_TYPE_AP_INVOICE), "应生成 1 条业财回链");
    }

    @Test
    public void testPostIdempotent() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-IDEM-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        Long first = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        Long second = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));

        assertNotNull(first, "首次过账应生成凭证");
        assertNull(second, "重复过账应空操作返回 null");
        assertEquals(1, countBillLinks("AP-IDEM-001", BUSINESS_TYPE_AP_INVOICE),
                "幂等：不应产生第二张凭证/回链");
        assertNotEquals(first, second);
    }

    @Test
    public void testPostUnbalancedRejected() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // TOTAL 与 AMOUNT+TAX 不等 → 借贷不平衡
        PostingEvent event = apInvoiceEvent("AP-UNBAL-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("200"));

        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)), "借贷不平衡应抛 NopException");
        assertEquals(0, countBillLinks("AP-UNBAL-001", BUSINESS_TYPE_AP_INVOICE), "被拒不应落库回链");
    }

    @Test
    public void testPostPeriodClosedRejected() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    PERIOD_STATUS_CLOSED);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-CLOSED-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)), "期间已结账应抛 NopException");
        assertEquals(0, countBillLinks("AP-CLOSED-001", BUSINESS_TYPE_AP_INVOICE), "被拒不应落库回链");
    }

    @Test
    public void testReverse() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        Long originalId = ormTemplate.runInSession(session -> voucherBiz.post(apInvoiceEvent("AP-REV-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
        assertNotNull(originalId, "前置：先 happy 过账生成原凭证");

        Long redId = ormTemplate.runInSession(session -> voucherBiz.reverse("AP-REV-001", ErpFinBusinessType.AP_INVOICE, CTX));
        assertNotNull(redId, "红冲应生成红字凭证");
        assertNotEquals(originalId, redId, "红字凭证是新凭证，非原凭证");

        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher original = voucherDao.requireEntityById(originalId);
        ErpFinVoucher red = voucherDao.requireEntityById(redId);

        assertEquals(true, red.getIsReversed(), "红字凭证 isReversed=true");
        assertEquals(originalId, red.getReversalOfVoucherId(), "红字凭证关联原凭证");
        assertEquals(VOUCHER_STATUS_POSTED, red.getDocStatus(), "红字凭证走正常 DRAFT→POSTED");
        // O-8：原正常凭证经引擎公共流程 markOriginalVoucherReversed 统一补标 isReversed=true，
        // 使账簿反映原凭证已被红冲、并允许幂等重过账（同 billCode 再过账时 alreadyPosted 不再命中已冲销凭证）。
        assertEquals(true, original.getIsReversed(), "原凭证已被红冲，isReversed=true（O-8 统一行为）");

        BigDecimal redDebit = red.getTotalDebit();
        BigDecimal redCredit = red.getTotalCredit();
        assertTrue(redDebit.compareTo(BigDecimal.ZERO) < 0, "红字凭证借方合计为负");
        assertTrue(redCredit.compareTo(BigDecimal.ZERO) < 0, "红字凭证贷方合计为负");
        assertTrue(redDebit.compareTo(redCredit) == 0, "红字凭证自身借贷平衡");

        BigDecimal origDebit = original.getTotalDebit();
        BigDecimal origCredit = original.getTotalCredit();
        assertTrue(origDebit.add(redDebit).compareTo(BigDecimal.ZERO) == 0, "原+红 借方净额为 0");
        assertTrue(origCredit.add(redCredit).compareTo(BigDecimal.ZERO) == 0, "原+红 贷方净额为 0");

        assertEquals(3, countLines(redId), "红字凭证应有 3 行分录（金额取负）");

        // 双向回链：从红字凭证 reversalOfVoucherId 可达原凭证；从原凭证反查 reversalOfVoucherId 可达红字凭证
        List<ErpFinVoucher> reversedOf = findVouchersReversing(originalId);
        assertEquals(1, reversedOf.size(), "从原凭证应能反查到 1 张红字凭证");
        assertEquals(redId, reversedOf.get(0).getId(), "反查到的红字凭证 id 一致");

        // 业财回链：原 + 红 各 1 条，关联同一业务单据
        assertEquals(2, countBillLinks("AP-REV-001", BUSINESS_TYPE_AP_INVOICE), "原凭证与红字凭证各 1 条回链");
    }

    @Test
    public void testReverseNotFound() {
        assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.reverse("AP-NOSUCH-001", ErpFinBusinessType.AP_INVOICE, CTX)),
                "无已过账凭证应抛 NopException");
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
        // AP 发票生成应收应付辅助账（ErpFinArApItem）需要 partnerId 与业务日期
        event.getBillData().put("partnerId", 1L);
        event.getBillData().put("businessDate", voucherDate);
        return event;
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

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private long countLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private long countBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        return links.size();
    }

    private List<ErpFinVoucher> findVouchersReversing(Long originalVoucherId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reversalOfVoucherId", originalVoucherId));
        return dao.findAllByQuery(q);
    }
}
