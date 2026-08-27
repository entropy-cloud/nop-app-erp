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
        String partnerId = "10";
        String[] fixture = setup(partnerId, new BigDecimal("300"), new BigDecimal("1000"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "300")), CTX));
        final String headId = head.getId();
        ormTemplate.runInSession(() -> reconciliationBiz.post(headId, CTX));

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

    /**
     * F2.2（P1-CK-fin2-003）：手工核销单多行共享同一辅助账项——聚合校验拒绝超核销。
     * 修复前：两行各 60 vs open=100 的逐行校验通过 → settled=120/open=−20/SETTLED（静默超核销）。
     */
    @Test
    public void testMultiLineSharedItemAggregatedRejected() {
        String partnerId = "30";
        String[] fixture = setup(partnerId, new BigDecimal("200"), new BigDecimal("100"),
                LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 10));

        // 两笔付款项（各 open 100）对同一发票项（open=100）各 60——发票项聚合 120 > 100
        String payment2 = ormTemplate.runInSession(sess -> {
            ErpFinArApItem p2 = newItem(ErpFinConstants.DIRECTION_PAYABLE, partnerId,
                    "PAYMENT", "PAY-" + partnerId + "-2", new BigDecimal("100"), LocalDate.of(2026, 6, 10));
            return p2.getId();
        });
        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Arrays.asList(
                        line(fixture[0], fixture[1], "60"),
                        line(payment2, fixture[1], "60")), CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> reconciliationBiz.post(head.getId(), CTX)),
                "F2.2：多行共享同一发票项累计 120 > open 100 应被聚合校验拒绝（修复前静默超核销）");
        assertEquals("erp.err.fin.reconciliation.over-amount", ex.getErrorCode());
        // 单行合法路径零回归：单行 60 通过
        ErpFinReconciliation ok = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 21),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "60")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(ok.getId(), CTX));
        assertEquals(ErpFinConstants.AR_AP_STATUS_PARTIAL, item(fixture[1]).getStatus(),
                "单行 60 正常核销（open 100 → PARTIAL）");
    }

    @Test
    public void testFullSettlement() {
        String partnerId = "20";
        String[] fixture = setup(partnerId, new BigDecimal("500"), new BigDecimal("500"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "500")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(head.getId(), CTX));

        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[0]).getStatus());
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[1]).getStatus());
    }

    @Test
    public void testCrossPartnerRejected() {
        String partnerA = "30";
        String partnerB = "32";
        String[] a = setup(partnerA, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));
        String[] b = setup(partnerB, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        // 用 A 的付款核销 B 的发票 → 跨 partner
        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerA, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(a[0], b[1], "100")), CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> reconciliationBiz.post(head.getId(), CTX)),
                "跨往来单位核销应拒绝");
    }

    @Test
    public void testOverAmountRejected() {
        String partnerId = "40";
        // 付款 100，发票 50，核销 100 → 超出发票 open
        String[] fixture = setup(partnerId, new BigDecimal("100"), new BigDecimal("50"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> reconciliationBiz.post(head.getId(), CTX)),
                "核销金额超过未核销余额应拒绝");
    }

    @Test
    public void testDateBeforeInvoiceRejected() {
        String partnerId = "50";
        // 发票业务日期 6-25，核销日期 6-20 → 早于发票
        String[] fixture = setup(partnerId, "100", "100", LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> reconciliationBiz.post(head.getId(), CTX)),
                "核销日期早于发票业务日期应拒绝");
    }

    @Test
    public void testReverseRestoresItems() {
        String partnerId = "60";
        String[] fixture = setup(partnerId, new BigDecimal("400"), new BigDecimal("400"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "400")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(head.getId(), CTX));
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, item(fixture[1]).getStatus());

        ormTemplate.runInSession(() -> reconciliationBiz.reverse(head.getId(), CTX));

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
        String partnerId = "70";
        String[] fixture = setup(partnerId, "100", "100", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));
        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(head.getId(), CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> reconciliationBiz.post(head.getId(), CTX)),
                "已过账核销单不应再次过账");
        assertNotEquals(ErpFinConstants.RECON_STATUS_DRAFT, recon(head.getId()).getDocStatus());
    }

    /**
     * F2.2（P2-CK-fin2-005）：已作废辅助账项的核销单 reverse 被拒（防复活 OPEN）。
     */
    @Test
    public void testReverseSettleOnCancelledItemRejected() {
        String partnerId = "40";
        String[] fixture = setup(partnerId, new BigDecimal("100"), new BigDecimal("100"),
                LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 10));
        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "100")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(head.getId(), CTX));

        // 模拟源单红冲将辅助账项置 CANCELLED（settled=0 场景下 cancelOnReverse 合法通过）
        ormTemplate.runInSession(sess -> {
            ErpFinArApItem invoice = item(fixture[1]);
            invoice.setStatus(ErpFinConstants.AR_AP_STATUS_CANCELLED);
            daoProvider.daoFor(ErpFinArApItem.class).saveOrUpdateEntity(invoice);
            return null;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> reconciliationBiz.reverse(head.getId(), CTX)),
                "F2.2：CANCELLED 项的核销单 reverse 应被拒（修复前无条件回写复活 OPEN）");
        assertEquals("erp.err.fin.ar-ap-item.cancelled-not-settlable", ex.getErrorCode());
    }

    // ---------- helpers ----------

    /** 在 session 内建 partner + 付款项 + 发票项，返回 [paymentItemId, invoiceItemId]。 */
    private String[] setup(String partnerId, String paymentAmt, String invoiceAmt,
                         LocalDate invoiceDate, LocalDate paymentDate) {
        return setup(partnerId, new BigDecimal(paymentAmt), new BigDecimal(invoiceAmt), invoiceDate, paymentDate);
    }

    private String[] setup(String partnerId, BigDecimal paymentAmt, BigDecimal invoiceAmt,
                         LocalDate invoiceDate, LocalDate paymentDate) {
        final String[][] holder = new String[1][];
        ormTemplate.runInSession(() -> {
            seedPartner(partnerId);
            ErpFinArApItem payment = newItem(ErpFinConstants.DIRECTION_PAYABLE, partnerId,
                    "PAYMENT", "PAY-" + partnerId, paymentAmt, paymentDate);
            ErpFinArApItem invoice = newItem(ErpFinConstants.DIRECTION_PAYABLE, partnerId,
                    "AP_INVOICE", "AP-" + partnerId, invoiceAmt, invoiceDate);
            holder[0] = new String[]{payment.getId(), invoice.getId()};
        });
        return holder[0];
    }

    private void seedPartner(String partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner partner = new ErpMdPartner();
        partner.orm_propValue(1, partnerId);
        partner.setCode("P-" + partnerId);
        partner.setName("Partner " + partnerId);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        partner.setReceivableBalance(BigDecimal.ZERO);
        partner.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(partner);
    }

    private ErpFinArApItem newItem(String direction, String partnerId, String sourceBillType, String sourceBillCode,
                                   BigDecimal amount, LocalDate businessDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId("1");
        item.setAcctSchemaId("1");
        item.setDirection(direction);
        item.setPartnerId(partnerId);
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(businessDate);
        item.setCurrencyId("1");
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

    private ReconciliationLineInput line(String paymentItemId, String invoiceItemId, String amount) {
        BigDecimal amt = new BigDecimal(amount);
        ReconciliationLineInput in = new ReconciliationLineInput();
        in.setPaymentItemId(paymentItemId);
        in.setInvoiceItemId(invoiceItemId);
        in.setSettledAmountSource(amt);
        in.setSettledAmountFunctional(amt);
        return in;
    }

    private ErpFinArApItem item(String id) {
        return daoProvider.daoFor(ErpFinArApItem.class).getEntityById(id);
    }

    private ErpFinReconciliation recon(String id) {
        return daoProvider.daoFor(ErpFinReconciliation.class).getEntityById(id);
    }
}
