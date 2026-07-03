package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 往来余额缓存（{@link ErpMdPartner#getPayableBalance()}/{@link ErpMdPartner#getReceivableBalance()}）
 * 由辅助账 openAmount 驱动更新的集成测试（Phase 2）。核销过账/红冲后 partner 余额应正确反映未核销合计。
 *
 * <p>余额缓存仅在核销单 post/reverse 时由 {@code PartnerBalanceUpdater} 重算（非每次辅助账变动自动触发）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPartnerBalance extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testPayableBalanceDrivenByOpenAmount() {
        long partner = 80L;
        Long[] fixture = setupPayable(partner, new BigDecimal("200"), new BigDecimal("1000"));

        assertEquals(0, partner(partner).getPayableBalance().compareTo(BigDecimal.ZERO),
                "核销前 partner 应付余额缓存为 0（仅核销触发重算）");

        ReconciliationLineInput line = line(fixture[0], fixture[1], "200");
        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partner, LocalDate.of(2026, 6, 20),
                Collections.singletonList(line), CTX);
        reconciliationBiz.post(head.getId(), CTX);

        // 核销后：发票 open 800 + 付款 open 0 = 800
        assertEquals(0, partner(partner).getPayableBalance().compareTo(new BigDecimal("800")),
                "核销后 partner 应付余额 = Σ 未核销 openAmount");

        reconciliationBiz.reverse(head.getId(), CTX);
        // 红冲恢复后：发票 open 1000 + 付款 open 200 = 1200
        assertEquals(0, partner(partner).getPayableBalance().compareTo(new BigDecimal("1200")),
                "红冲后 partner 应付余额恢复");
    }

    @Test
    public void testReceivableBalanceViaReconciliation() {
        long partner = 81L;
        Long[] fixture = setupReceivable(partner, new BigDecimal("500"), new BigDecimal("500"));

        ErpFinReconciliation head = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_RECEIVABLE, partner, LocalDate.of(2026, 6, 20),
                Collections.singletonList(line(fixture[0], fixture[1], "500")), CTX);
        reconciliationBiz.post(head.getId(), CTX);

        assertEquals(0, partner(partner).getReceivableBalance().compareTo(BigDecimal.ZERO),
                "应收全额核销后应收余额为 0");
        assertEquals(0, partner(partner).getPayableBalance().compareTo(BigDecimal.ZERO),
                "无应付项时应付余额为 0（应收/应付独立路由）");
    }

    // ---------- helpers ----------

    /** 在 session 内建 partner + 付款项 + 发票项（应付方向），返回 [paymentItemId, invoiceItemId]。 */
    private Long[] setupPayable(long partner, BigDecimal paymentAmt, BigDecimal invoiceAmt) {
        return setup(partner, ErpFinConstants.DIRECTION_PAYABLE, "PAYMENT", "AP_INVOICE",
                paymentAmt, invoiceAmt);
    }

    private Long[] setupReceivable(long partner, BigDecimal receiptAmt, BigDecimal invoiceAmt) {
        return setup(partner, ErpFinConstants.DIRECTION_RECEIVABLE, "RECEIPT", "AR_INVOICE",
                receiptAmt, invoiceAmt);
    }

    private Long[] setup(long partner, String direction, String payBillType, String invBillType,
                         BigDecimal payAmt, BigDecimal invAmt) {
        final Long[][] holder = new Long[1][];
        ormTemplate.runInSession(() -> {
            seedPartner(partner);
            ErpFinArApItem pay = newItem(direction, partner, payBillType,
                    payBillType + "-" + partner, payAmt, LocalDate.of(2026, 6, 8));
            ErpFinArApItem inv = newItem(direction, partner, invBillType,
                    invBillType + "-" + partner, invAmt, LocalDate.of(2026, 6, 10));
            holder[0] = new Long[]{pay.getId(), inv.getId()};
        });
        return holder[0];
    }

    private void seedPartner(long partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner p = new ErpMdPartner();
        p.orm_propValue(1, partnerId);
        p.setCode("P-" + partnerId);
        p.setName("Partner " + partnerId);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private ErpFinArApItem newItem(String direction, long partnerId, String sourceBillType, String sourceBillCode,
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

    private ErpMdPartner partner(long id) {
        return daoProvider.daoFor(ErpMdPartner.class).getEntityById(id);
    }
}
