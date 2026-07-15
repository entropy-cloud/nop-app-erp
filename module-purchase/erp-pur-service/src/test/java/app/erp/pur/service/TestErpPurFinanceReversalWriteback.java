package app.erp.pur.service;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财闭环方向二集成测试（计划 {@code 2026-07-04-1452-2} Phase 3）。
 *
 * <p>验证财务侧直接红冲已过账凭证时，{@code PurReversalListener} 监听 {@link IErpFinVoucherBiz#reverse}
 * 派发的 {@code VoucherReversedEvent}，回退采购发票状态（posted=false + approveStatus APPROVED→REJECTED），
 * 实现业财闭环方向二（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4}）。
 *
 * <p>区别于既有 {@code TestErpPurInvoicePosting.testReverseApproveGeneratesRedVoucher}（方向一：业务侧
 * reverseApprove 触发红冲），本测试覆盖**方向二**：财务员直接调 {@code voucherBiz.reverse()} 红冲，
 * 采购单据**被动**经监听者回退。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurFinanceReversalWriteback extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1003L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7003L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testFinanceReverseRollsBackPurchaseInvoicePostedAndApproveStatus() {
        seedPeriodAndSubjects();
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
        });

        // 1) 构造已过账的采购发票（posted=true + APPROVED）。
        ErpPurInvoice invoice = invoiceOf("PI-FIN-REV-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
            invoice.setPosted(true);
            invoice.setPostedAt(CoreMetrics.currentTimestamp());
            invoice.setPostedBy("test-user");
            saveInvoiceWithLine(invoice);
        });

        // 2) 构造业财回链 + 已过账凭证（模拟正向过账的产物，供 reverse() 反查）。
        Long originalVoucherId = seedPostedVoucherFor(invoice.getCode(),
                ErpFinBusinessType.AP_INVOICE, new BigDecimal("113"));

        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()),
                "前置：发票已过账 posted=true");

        // 3) 财务侧直接红冲凭证（方向二）—— 采购域监听者应被动回退发票状态。
        Long redVoucherId = ormTemplate.runInSession(session -> voucherBiz.reverse(invoice.getCode(), ErpFinBusinessType.AP_INVOICE, CTX));

        assertNotNull(redVoucherId, "财务侧红冲应生成红字凭证");
        assertNotEquals(originalVoucherId, redVoucherId);

        ErpPurInvoice reloaded = reload(invoice);
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()),
                "方向二：财务红冲后采购发票 posted 应被监听者回退为 false");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus(),
                "方向二：财务红冲后采购发票 approveStatus APPROVED→REJECTED");
        assertEquals(null, reloaded.getPostedAt(), "postedAt 应清空");
        assertEquals(null, reloaded.getPostedBy(), "postedBy 应清空");
    }

    @Test
    public void testFinanceReverseWithoutExistingSourceLeavesRedVoucherPostedAndAlerts() {
        seedPeriodAndSubjects();
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));

        // 1) 构造已过账凭证但其关联的采购发票**已不存在**（模拟源单已被删除/状态不一致）。
        String ghostBillCode = "PI-GHOST-001";
        seedPostedVoucherFor(ghostBillCode, ErpFinBusinessType.AP_INVOICE, new BigDecimal("113"));

        // 2) 财务侧红冲——监听者反查不到源单应静默（不抛错，因 posted 标志未翻转为 true），
        //    红字凭证照常落库，无告警记录（监听者未抛错即视为成功）。
        Long redVoucherId = ormTemplate.runInSession(session -> voucherBiz.reverse(ghostBillCode, ErpFinBusinessType.AP_INVOICE, CTX));
        assertNotNull(redVoucherId, "源单不存在时红字凭证仍应过账（法律效力）");

        // 无 ErpFinPostingException 记录（监听者 findByCode 返回 null，不抛错即静默成功）
        assertEquals(0, countReversalListenerFailures(ghostBillCode),
                "源单不存在时监听者静默（findByCode 返回 null，无失败记录）");
    }

    // ---------- helpers ----------

    private ErpPurInvoice reload(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private ErpPurInvoice invoiceOf(String code, BigDecimal amount, BigDecimal tax, BigDecimal withTax) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private void saveInvoiceWithLine(ErpPurInvoice invoice) {
        daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
        IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        lineDao.saveEntity(line);
    }

    /** 直接构造已过账凭证 + 业财回链（绕过过账引擎，模拟"已存在过账结果"的最小前置态）。 */
    private Long seedPostedVoucherFor(String billCode, ErpFinBusinessType businessType, BigDecimal total) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        IEntityDao<ErpFinAccountingPeriod> periodDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(eq("code", "2026-07"));
        pq.setLimit(1);
        ErpFinAccountingPeriod period = periodDao.findAllByQuery(pq).get(0);
        return ormTemplate.runInSession(session -> {
            ErpFinVoucher voucher = new ErpFinVoucher();
            voucher.setCode("PST-SEED-" + billCode);
            voucher.setVoucherType("TRANSFER");
            voucher.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
            voucher.setVoucherDate(LocalDate.of(2026, 7, 1));
            voucher.setOrgId(ORG_ID);
            voucher.setAcctSchemaId(ACCT_SCHEMA_ID);
            voucher.setPeriodId(period.getId());
            voucher.setTotalDebit(total);
            voucher.setTotalCredit(total);
            voucher.setIsReversed(false);
            voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
            voucher.setPostedAt(CoreMetrics.currentTimestamp());
            vDao.saveEntity(voucher);

            ErpFinVoucherBillR billR = new ErpFinVoucherBillR();
            billR.setVoucherId(voucher.getId());
            billR.setBillType(businessType.name());
            billR.setBillCode(billCode);
            billR.setBusinessType(businessType.name());
            billRDao.saveEntity(billR);
            return voucher.getId();
        });
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1403", "在途物资");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2202", "应付账款");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao = daoProvider.daoFor(
                app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
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

    private long countReversalListenerFailures(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        q.addFilter(eq("failedStage", ErpFinConstants.FAILED_STAGE_NOTIFY_REVERSAL_LISTENER));
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.size();
    }
}
