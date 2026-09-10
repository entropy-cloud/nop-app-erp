package app.erp.fin.service.reconciliation;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * M2.8 分片④ fin2-018-r3（DIM-T 覆盖缺口）：核销 FX 汇兑损益路径行为断言——
 * {@code ReconciliationSettler.settleWithFx}（per-item functional 结算 + 已实现汇兑差额
 * = Σ(payment.functional) − Σ(invoice.functional)，正=收益）与 F2.2 {@code reverseSettle(fxPath=true)}
 * 红冲对称回滚。修复前零测试断言（r1 交叉验证注记无 ID 承接）；辅助账双汇率（7.2/7.0）证明
 * 损益计算与红冲按各自汇率对称回退。整链单会话内完成（settler 经 session 身份缓存原位改写辅助账）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReconciliationFxPath extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ReconciliationSettler settler;

    @Test
    public void testSettleWithFxComputesRealizedGainLossAndReverseRollsBack() {
        ormTemplate.runInSession(session -> {
            // 收款侧汇率 7.2、发票侧 7.0：结算 100 源币
            // payment.functional = 100×7.2=720；invoice.functional = 100×7.0=700 → 汇兑差额 +20（收益）
            ErpFinArApItem paymentItem = seedItem("m28-fx-pay", "7.2", "500");
            ErpFinArApItem invoiceItem = seedItem("m28-fx-inv", "7.0", "500");

            ErpFinReconciliation head = new ErpFinReconciliation();
            head.setTotalAmountFunctional(BigDecimal.ZERO);
            head.setTotalAmountSource(BigDecimal.ZERO);

            ErpFinReconciliationLine line = new ErpFinReconciliationLine();
            line.setPaymentItemId(paymentItem.getId());
            line.setInvoiceItemId(invoiceItem.getId());
            line.setSettledAmountSource(new BigDecimal("100"));

            BigDecimal fxGainLoss = settler.settleWithFx(head, List.of(line));

            assertEquals(0, new BigDecimal("20").compareTo(fxGainLoss),
                    "已实现汇兑差额 = 720 − 700 = +20（收益）");
            assertEquals(0, new BigDecimal("700").compareTo(head.getTotalAmountFunctional()),
                    "head.totalAmountFunctional 取发票侧合计（AR/AP 清账口径）");
            assertEquals(0, new BigDecimal("100").compareTo(head.getTotalAmountSource()), "源币合计");
            assertNotNull(head.getFxGainLoss(), "fxGainLoss 落 head");

            // 辅助账回写断言（session 身份缓存同一实例）：settled 增 / open 减 / 状态降级 PARTIAL
            assertEquals(0, new BigDecimal("720").compareTo(paymentItem.getSettledAmountFunctional()),
                    "收款侧 functional 结算额 = 100×7.2");
            assertEquals(0, new BigDecimal("700").compareTo(invoiceItem.getSettledAmountFunctional()),
                    "发票侧 functional 结算额 = 100×7.0");
            assertEquals(ErpFinConstants.AR_AP_STATUS_PARTIAL, paymentItem.getStatus(),
                    "部分核销后状态 PARTIAL");

            // F2.2 红冲对称回滚：fxPath=true 现算重演（各按自身汇率），反向恢复
            settler.reverseSettle(List.of(line), true);
            assertEquals(0, BigDecimal.ZERO.compareTo(paymentItem.getSettledAmountFunctional()),
                    "红冲后收款侧 settled 归零（720−720）");
            assertEquals(0, BigDecimal.ZERO.compareTo(invoiceItem.getSettledAmountFunctional()),
                    "红冲后发票侧 settled 归零（700−700）");
            assertEquals(0, new BigDecimal("3600").compareTo(paymentItem.getOpenAmountFunctional()),
                    "收款侧 open functional 复原 500×7.2=3600");
            assertEquals(0, new BigDecimal("3500").compareTo(invoiceItem.getOpenAmountFunctional()),
                    "发票侧 open functional 复原 500×7.0=3500（fxPath 逐侧汇率回退无 |Δrate×amt| 残留）");
            assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, paymentItem.getStatus(),
                    "红冲后状态降级回 OPEN");
            return null;
        });
    }

    private ErpFinArApItem seedItem(String code, String rate, String amountSource) {
        java.time.LocalDate bd = java.time.LocalDate.of(2026, 7, 1);
        ErpFinArApItem item = daoProvider.daoFor(ErpFinArApItem.class).newEntity();
        item.setCode(code);
        item.setOrgId("1");
        item.setAcctSchemaId("1");
        item.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
        item.setPartnerId("1");
        item.setSourceBillType("AP_INVOICE");
        item.setSourceBillCode(code);
        item.setBusinessDate(bd);
        item.setDueDate(bd);
        item.setCurrencyId("1");
        item.setExchangeRate(new BigDecimal(rate));
        item.setAmountSource(new BigDecimal(amountSource));
        item.setAmountFunctional(new BigDecimal(amountSource).multiply(new BigDecimal(rate)));
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(new BigDecimal(amountSource));
        item.setOpenAmountFunctional(new BigDecimal(amountSource).multiply(new BigDecimal(rate)));
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        daoProvider.daoFor(ErpFinArApItem.class).saveEntity(item);
        return item;
    }
}
