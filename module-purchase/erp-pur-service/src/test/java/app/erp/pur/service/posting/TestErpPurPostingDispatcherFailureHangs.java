package app.erp.pur.service.posting;

import app.erp.fin.dao.PostingEvent;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurReturn;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * G1 dispatcher tryPost 失败悬挂测试（plan {@code 2026-07-31-0744-3-r2-14}，P1-MA4-021(b) 残差）。
 *
 * <p>采购域 3 个过账派发器（Invoice/Payment/Return）的 {@code tryPost} catch-swallow 路径此前零测试触发：
 * 过账失败时纯 {@code LOG.warn}/{@code LOG.error} 吞异常返回 {@code false}（保持 APPROVED+posted=false 悬挂）。
 * 本测试用确定性子类桩 {@link PurPostingExecutor}（postEvent 抛异常）诱导过账失败，断言：
 * <ol>
 *   <li>{@code tryPost} 返回 {@code false}（调用方 BizModel 据此置 posted=false，业财悬挂对测试可观测）</li>
 *   <li>传入源单据未被持久化层触碰（Facade 编排层不持久化源单——终态不受影响）</li>
 * </ol>
 *
 * <p>范式对齐 R1.16 {@code TestDepreciationPostingFailureAlert}（无 Mockito，子类/Proxy 桩直调）：
 * 派发器为 Facade 编排层，posted 标志由调用方 BizModel 据 tryPost 返回值在主事务内统一持久化，
 * 故 tryPost→false 即「posted=false 悬挂」的可观测契约（happy-path posted=true 由既有
 * {@code TestErpPurInvoicePosting}/{@code TestErpPurReturnPosting} 等集成测试覆盖）。
 */
public class TestErpPurPostingDispatcherFailureHangs {

    /** 模拟财务过账引擎宕机：postEvent 抛 NopException 走 dispatcher 的 warn 吞咽分支。 */
    private static PurPostingExecutor throwingExecutor() {
        return new PurPostingExecutor() {
            @Override
            public Long postEvent(PostingEvent event) {
                throw new NopException("test.pur-posting-engine-down", null, true, true);
            }
        };
    }

    @Test
    public void testPurInvoiceTryPostFailureReturnsFalse() {
        PurInvoicePostingDispatcher dispatcher = new PurInvoicePostingDispatcher();
        dispatcher.executor = throwingExecutor();

        ErpPurInvoice invoice = invoiceOf("PI-FAIL-001");

        boolean posted = dispatcher.tryPost(invoice);

        assertFalse(posted, "采购发票过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
        assertNull(invoice.getPosted(), "Facade 派发器不持久化源单据 posted 标志（终态由调用方 BizModel 决定）");
    }

    @Test
    public void testPurPaymentTryPostFailureReturnsFalse() {
        PurPaymentPostingDispatcher dispatcher = new PurPaymentPostingDispatcher();
        dispatcher.executor = throwingExecutor();

        ErpPurPayment payment = paymentOf("PAY-FAIL-001");

        boolean posted = dispatcher.tryPost(payment);

        assertFalse(posted, "付款单过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
    }

    @Test
    public void testPurReturnTryPostFailureReturnsFalse() {
        PurReturnPostingDispatcher dispatcher = new PurReturnPostingDispatcher();
        dispatcher.executor = throwingExecutor();

        ErpPurReturn returnOrder = returnOf("RT-FAIL-001");

        boolean posted = dispatcher.tryPost(returnOrder);

        assertFalse(posted, "采购退货过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
    }

    // ---------- helpers ----------

    private ErpPurInvoice invoiceOf(String code) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(6101L);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
        invoice.setSupplierId(2101L);
        return invoice;
    }

    private ErpPurPayment paymentOf(String code) {
        ErpPurPayment payment = new ErpPurPayment();
        payment.setCode(code);
        payment.setBusinessDate(LocalDate.of(2026, 7, 1));
        payment.setCurrencyId(6101L);
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setTotalAmount(new BigDecimal("113"));
        payment.setSupplierId(2101L);
        return payment;
    }

    private ErpPurReturn returnOf(String code) {
        ErpPurReturn returnOrder = new ErpPurReturn();
        returnOrder.setCode(code);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(6101L);
        returnOrder.setExchangeRate(BigDecimal.ONE);
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setSupplierId(2101L);
        return returnOrder;
    }
}
