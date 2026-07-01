package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 核销单 BizModel（{@link IErpFinReconciliationBiz}）集成测试（Phase 2）。覆盖：
 * 部分核销（status=PARTIAL）、全额核销（SETTLED）、跨 partner 拒绝、超额拒绝、核销日期早于发票业务日期拒绝、
 * reverse 恢复辅助账与状态机，以及状态机门控。
 *
 * <p>直接装配辅助账（{@link ErpFinArApItem}）作为核销对象（绕过过账管线，聚焦核销逻辑本身）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReconciliation extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testPartialSettlement() {
        long partnerId = 10L;
        Long[] fixture = setup(partnerId, new BigDecimal("300"), new BigDecimal("1000"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "300")), CTX);
        reconciliationBiz.post(head.getId(), CTX);

        ErpFinArApItem payment = item(fixture[0]);
        ErpFinArApItem invoice = item(fixture[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_PARTIAL, invoice.getStatus(), "发票部分核销");
        assertEquals(0, invoice.getOpenAmountFunctional().compareTo(new BigDecimal("700")));
        assertEquals(0, invoice.getSettledAmountFunctional().compareTo(new BigDecimal("300")));
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, payment.getStatus(), "付款项全额核销");
        assertEquals(0, payment.getOpenAmountFunctional().compareTo(BigDecimal.ZERO));

        head = recon(head.getId());
        assertEquals(ErpFinConstants.RECON_STATUS_POSTED, head.getDocStatus());
        assertEquals(0, head.getTotalAmountFunctional().compareTo(new BigDecimal("300")));
    }

    @Test
    public void testFullSettlement() {
        long partnerId = 20L;
        Long[] fixture = setup(partnerId, new BigDecimal("500"), new BigDecimal("500"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "500")), CTX);
        reconciliationBiz.post(head.getId(), CTX);

        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[0]).getStatus());
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[1]).getStatus());
    }

    @Test
    public void testCrossPartnerRejected() {
        long partnerA = 30L;
        long partnerB = 32L;
        Long[] a = setup(partnerA, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));
        Long[] b = setup(partnerB, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        // 用 A 的付款核销 B 的发票 → 跨 partner
        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerA, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(a[0], b[1], "100")), CTX);
        assertThrows(NopException.class, () -> reconciliationBiz.post(head.getId(), CTX),
                "跨往来单位核销应拒绝");
    }

    @Test
    public void testOverAmountRejected() {
        long partnerId = 40L;
        // 付款 100，发票 50，核销 100 → 超出发票 open
        Long[] fixture = setup(partnerId, new BigDecimal("100"), new BigDecimal("50"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX);
        assertThrows(NopException.class, () -> reconciliationBiz.post(head.getId(), CTX),
                "核销金额超过未核销余额应拒绝");
    }

    @Test
    public void testDateBeforeInvoiceRejected() {
        long partnerId = 50L;
        // 发票业务日期 6-25，核销日期 6-20 → 早于发票
        Long[] fixture = setup(partnerId, "100", "100", LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX);
        assertThrows(NopException.class, () -> reconciliationBiz.post(head.getId(), CTX),
                "核销日期早于发票业务日期应拒绝");
    }

    @Test
    public void testReverseRestoresItems() {
        long partnerId = 60L;
        Long[] fixture = setup(partnerId, new BigDecimal("400"), new BigDecimal("400"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "400")), CTX);
        reconciliationBiz.post(head.getId(), CTX);
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[1]).getStatus());

        reconciliationBiz.reverse(head.getId(), CTX);

        assertEquals(ErpFinConstants.RECON_STATUS_REVERSED, recon(head.getId()).getDocStatus());
        ErpFinArApItem invoice = item(fixture[1]);
        ErpFinArApItem payment = item(fixture[0]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, invoice.getStatus(), "红冲恢复发票为未核销");
        assertEquals(0, invoice.getOpenAmountFunctional().compareTo(new BigDecimal("400")));
        assertEquals(0, invoice.getSettledAmountFunctional().compareTo(BigDecimal.ZERO));
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, payment.getStatus(), "红冲恢复付款项为未核销");
        assertEquals(0, payment.getOpenAmountFunctional().compareTo(new BigDecimal("400")));
    }

    @Test
    public void testPostPostedAgainRejected() {
        long partnerId = 70L;
        Long[] fixture = setup(partnerId, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));
        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX);
        reconciliationBiz.post(head.getId(), CTX);
        assertThrows(NopException.class, () -> reconciliationBiz.post(head.getId(), CTX),
                "已过账核销单不应再次过账");
        assertNotEquals(ErpFinConstants.RECON_STATUS_DRAFT, recon(head.getId()).getDocStatus());
    }

    // ---------- helpers ----------

    /** 在 session 内建 partner + 付款项 + 发票项，返回 [paymentItemId, invoiceItemId]。 */
    private Long[] setup(long partnerId, String paymentAmt, String invoiceAmt,
                         LocalDate invoiceDate, LocalDate paymentDate) {
        return setup(partnerId, new BigDecimal(paymentAmt), new BigDecimal(invoiceAmt), invoiceDate, paymentDate);
    }

    private Long[] setup(long partnerId, BigDecimal paymentAmt, BigDecimal invoiceAmt,
                         LocalDate invoiceDate, LocalDate paymentDate) {
        final Long[][] holder = new Long[1][];
        ormTemplate.runInSession(() -> {
            seedPartner(partnerId);
            ErpFinArApItem payment = newItem(ErpFinConstants.DIRECTION_PAYABLE, partnerId,
                    "PAYMENT", "PAY-" + partnerId, paymentAmt, paymentDate);
            ErpFinArApItem invoice = newItem(ErpFinConstants.DIRECTION_PAYABLE, partnerId,
                    "AP_INVOICE", "AP-" + partnerId, invoiceAmt, invoiceDate);
            holder[0] = new Long[]{payment.getId(), invoice.getId()};
        });
        return holder[0];
    }

    private void seedPartner(long partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner partner = new ErpMdPartner();
        partner.orm_propValue(1, partnerId);
        partner.setCode("P-" + partnerId);
        partner.setName("Partner " + partnerId);
        partner.setPartnerType(10);
        partner.setStatus(10);
        partner.setReceivableBalance(BigDecimal.ZERO);
        partner.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(partner);
    }

    private ErpFinArApItem newItem(int direction, long partnerId, String sourceBillType, String sourceBillCode,
                                   BigDecimal amount, LocalDate businessDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(partnerId);
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(businessDate);
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(amount);
        item.setAmountFunctional(amount);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amount);
        item.setOpenAmountFunctional(amount);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
        return item;
    }

    private ReconciliationLineInput line(Long paymentItemId, Long invoiceItemId, String amount) {
        BigDecimal amt = new BigDecimal(amount);
        ReconciliationLineInput in = new ReconciliationLineInput();
        in.setPaymentItemId(paymentItemId);
        in.setInvoiceItemId(invoiceItemId);
        in.setSettledAmountSource(amt);
        in.setSettledAmountFunctional(amt);
        return in;
    }

    private ErpFinArApItem item(Long id) {
        return daoProvider.daoFor(ErpFinArApItem.class).getEntityById(id);
    }

    private ErpFinReconciliation recon(Long id) {
        return daoProvider.daoFor(ErpFinReconciliation.class).getEntityById(id);
    }
}
