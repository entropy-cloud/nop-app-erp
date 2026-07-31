package app.erp.sal.service.posting;

import app.erp.fin.dao.PostingEvent;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * G1 dispatcher tryPost 失败悬挂测试（plan {@code 2026-07-31-0744-3-r2-14}，P1-MA4-021(b) 残差）。
 *
 * <p>销售域 3 个过账派发器（Invoice/Receipt/Return）的 {@code tryPost} catch-swallow 路径此前零测试触发。
 * 用确定性子类桩 {@link SalPostingExecutor}（postEvent 抛异常）诱导过账失败，断言 tryPost 返回 false
 * （调用方 BizModel 据此置 posted=false，业财悬挂对测试可观测）。
 *
 * <p>范式对齐 R1.16 {@code TestDepreciationPostingFailureAlert}（无 Mockito，子类桩直调）。
 * {@link SalReturnPostingDispatcher#tryPost} 的 buildEvent 经 {@code loadLines} 查 DB（returnId 无匹配行→空列表），
 * 故本类挂 autotest 容器取真实 {@link IDaoProvider}；Invoice/Receipt 的 buildEvent 不触 DB（orgId 留空短路账套解析）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalPostingDispatcherFailureHangs extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    /** 模拟财务过账引擎宕机：postEvent 抛 NopException 走 dispatcher 的 warn 吞咽分支。 */
    private static SalPostingExecutor throwingExecutor() {
        return new SalPostingExecutor() {
            @Override
            public Long postEvent(PostingEvent event) {
                throw new NopException("test.sal-posting-engine-down", null, true, true);
            }
        };
    }

    @Test
    public void testSalInvoiceTryPostFailureReturnsFalse() {
        SalInvoicePostingDispatcher dispatcher = new SalInvoicePostingDispatcher();
        dispatcher.executor = throwingExecutor();
        dispatcher.daoProvider = daoProvider;

        ErpSalInvoice invoice = invoiceOf("SI-FAIL-001");

        boolean posted = dispatcher.tryPost(invoice);

        assertFalse(posted, "销售发票过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
    }

    @Test
    public void testSalReceiptTryPostFailureReturnsFalse() {
        SalReceiptPostingDispatcher dispatcher = new SalReceiptPostingDispatcher();
        dispatcher.executor = throwingExecutor();
        dispatcher.daoProvider = daoProvider;

        ErpSalReceipt receipt = receiptOf("SR-FAIL-001");

        boolean posted = dispatcher.tryPost(receipt);

        assertFalse(posted, "收款单过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
    }

    @Test
    public void testSalReturnTryPostFailureReturnsFalse() {
        SalReturnPostingDispatcher dispatcher = new SalReturnPostingDispatcher();
        dispatcher.executor = throwingExecutor();
        dispatcher.daoProvider = daoProvider;

        ErpSalReturn returnOrder = returnOf("SRT-FAIL-001");

        boolean posted = dispatcher.tryPost(returnOrder);

        assertFalse(posted, "销售退货过账失败应吞异常返回 false（保持 APPROVED+posted=false 悬挂）");
    }

    // ---------- helpers ----------

    private ErpSalInvoice invoiceOf(String code) {
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setCode(code);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(6201L);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
        invoice.setCustomerId(2201L);
        return invoice;
    }

    private ErpSalReceipt receiptOf(String code) {
        ErpSalReceipt receipt = new ErpSalReceipt();
        receipt.setCode(code);
        receipt.setBusinessDate(LocalDate.of(2026, 7, 1));
        receipt.setCurrencyId(6201L);
        receipt.setExchangeRate(BigDecimal.ONE);
        receipt.setTotalAmount(new BigDecimal("113"));
        receipt.setCustomerId(2201L);
        return receipt;
    }

    private ErpSalReturn returnOf(String code) {
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setCode(code);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(6201L);
        returnOrder.setExchangeRate(BigDecimal.ONE);
        returnOrder.setTotalAmountWithTax(new BigDecimal("20"));
        returnOrder.setCustomerId(2201L);
        return returnOrder;
    }
}
