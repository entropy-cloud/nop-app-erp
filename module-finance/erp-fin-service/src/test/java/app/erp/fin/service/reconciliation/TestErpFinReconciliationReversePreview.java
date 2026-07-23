package app.erp.fin.service.reconciliation;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.dto.ReconciliationReversePreview;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 核销单冲销预览（{@code IErpFinReconciliationBiz.previewReverse}）集成测试
 * （plan 2026-07-23-1145-2 Phase 3）。
 *
 * <p>覆盖：预览返回辅助账回退列表（结构非空 + 金额/状态正确）+ 预览与实际 reverse 结果一致。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReconciliationReversePreview extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testPreviewReverseMatchesActual() {
        long partnerId = 160L;
        Long[] fixture = setup(partnerId, new BigDecimal("400"), new BigDecimal("400"),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8));

        ErpFinReconciliation head = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, partnerId, LocalDate.of(2026, 6, 20),
                java.util.Collections.singletonList(line(fixture[0], fixture[1], "400")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(head.getId(), CTX));

        // 预览：不执行实际冲销
        ReconciliationReversePreview preview = reconciliationBiz.previewReverse(head.getId(), CTX);
        assertEquals(head.getId(), preview.getReconciliationId());
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, preview.getDirection());
        assertTrue(preview.isWillSetReversed(), "预览应标记将置 REVERSED");
        assertTrue(preview.isWillRefreshPartnerBalance(), "预览应标记将刷新 partner 余额");
        assertEquals(0, preview.getTotalAmountFunctional().compareTo(new BigDecimal("400")));
        // 每行产生 payment + invoice 两个回退项
        assertEquals(2, preview.getRevertedItems().size(), "1 行核销应有 2 个回退项（付款+发票）");

        ReconciliationReversePreview.RevertedItem invoiceItem = preview.getRevertedItems().stream()
                .filter(r -> "invoice".equals(r.getSide())).findFirst().orElseThrow();
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, invoiceItem.getCurrentStatus(),
                "全额核销的发票项当前应为 SETTLED");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, invoiceItem.getWillBecomeStatus(),
                "全额回退后预估为 OPEN");
        assertEquals(0, invoiceItem.getRestoreAmountFunctional().compareTo(new BigDecimal("400")));

        // 预览为只读：核销单状态仍为 POSTED
        assertEquals(ErpFinConstants.RECON_STATUS_POSTED, recon(head.getId()).getDocStatus(),
                "预览不应改变核销单状态");

        // 实际 reverse 后验证预览一致
        ormTemplate.runInSession(() -> reconciliationBiz.reverse(head.getId(), CTX));
        assertEquals(ErpFinConstants.RECON_STATUS_REVERSED, recon(head.getId()).getDocStatus());
        ErpFinArApItem invoice = item(fixture[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, invoice.getStatus(), "实际红冲恢复发票为 OPEN");
        assertEquals(invoiceItem.getWillBecomeStatus(), invoice.getStatus(),
                "预览的 willBecomeStatus 应与实际 reverse 后状态一致");
    }

    // ---------- helpers (镜像 TestErpFinReconciliation) ----------

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
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        partner.setReceivableBalance(BigDecimal.ZERO);
        partner.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(partner);
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

    private ErpFinArApItem item(Long id) {
        return daoProvider.daoFor(ErpFinArApItem.class).getEntityById(id);
    }

    private ErpFinReconciliation recon(Long id) {
        return daoProvider.daoFor(ErpFinReconciliation.class).getEntityById(id);
    }
}
