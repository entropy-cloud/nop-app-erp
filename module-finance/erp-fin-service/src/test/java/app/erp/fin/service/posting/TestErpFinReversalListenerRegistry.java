package app.erp.fin.service.posting;

import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 凭证红冲监听者注册中心契约测试（计划 {@code 2026-07-04-1452-2} Phase 2）。
 *
 * <p>纯单元测试（不启动 IoC），验证：
     * <ul>
 *   <li>空监听者列表时 {@code dispatch} 空操作不报错；</li>
 *   <li>同步遍历所有监听者，事件字段原样透传；</li>
 *   <li>失败隔离：单个监听者抛 {@link NopException} 不中断其他监听者；</li>
 *   <li>失败记录承载 listenerName/errorCode/errorMessage。</li>
 * </ul>
 */
public class TestErpFinReversalListenerRegistry {

    private static final IServiceContext CTX = new ServiceContextImpl();

    /** 捕获事件的测试监听者。 */
    private static class CapturingListener implements IErpFinVoucherReversedListener {
        final List<VoucherReversedEvent> captured = new ArrayList<>();

        @Override
        public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
            captured.add(event);
        }
    }

    /** 总是抛错的测试监听者。 */
    private static class FailingListener implements IErpFinVoucherReversedListener {
        @Override
        public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
            throw new NopException(ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED)
                    .param(ErpFinPostingErrors.ARG_LISTENER, getClass().getName())
                    .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, event.getBillHeadCode());
        }
    }

    private VoucherReversedEvent sampleEvent() {
        VoucherReversedEvent event = new VoucherReversedEvent();
        event.setVoucherId(1001L);
        event.setReversalOfVoucherId(2002L);
        event.setBillHeadCode("AP-REV-001");
        event.setBusinessType("AP_INVOICE");
        event.setBillType("AP_INVOICE");
        event.setTraceId("TRACE-TEST-001");
        return event;
    }

    @Test
    public void testEmptyListenersDispatchIsNoOp() {
        ErpFinReversalListenerRegistry registry = new ErpFinReversalListenerRegistry();
        registry.init();

        List<ErpFinReversalListenerRegistry.ListenerFailure> failures =
                registry.dispatch(sampleEvent(), CTX);

        assertTrue(failures.isEmpty(), "无监听者时派发应为空操作，返回空失败列表");
    }

    @Test
    public void testEventFieldsPassedThrough() {
        CapturingListener capturing = new CapturingListener();
        ErpFinReversalListenerRegistry registry = new ErpFinReversalListenerRegistry();
        registry.setListeners(Collections.singletonList(capturing));
        registry.init();

        VoucherReversedEvent event = sampleEvent();
        registry.dispatch(event, CTX);

        assertEquals(1, capturing.captured.size(), "监听者应收到 1 次事件");
        VoucherReversedEvent received = capturing.captured.get(0);
        assertSame(event, received, "事件应原样透传");
        assertEquals(Long.valueOf(1001L), received.getVoucherId(), "voucherId 字段正确");
        assertEquals(Long.valueOf(2002L), received.getReversalOfVoucherId(), "reversalOfVoucherId 字段正确");
        assertEquals("AP-REV-001", received.getBillHeadCode(), "billHeadCode 字段正确");
        assertEquals("AP_INVOICE", received.getBusinessType(), "businessType 字段正确");
        assertEquals("AP_INVOICE", received.getBillType(), "billType 字段正确");
        assertEquals("TRACE-TEST-001", received.getTraceId(), "traceId 字段正确");
    }

    @Test
    public void testFailureIsolationDoesNotBreakOtherListeners() {
        CapturingListener first = new CapturingListener();
        FailingListener middle = new FailingListener();
        CapturingListener last = new CapturingListener();
        ErpFinReversalListenerRegistry registry = new ErpFinReversalListenerRegistry();
        registry.setListeners(Arrays.asList(first, middle, last));
        registry.init();

        List<ErpFinReversalListenerRegistry.ListenerFailure> failures =
                registry.dispatch(sampleEvent(), CTX);

        assertEquals(1, first.captured.size(), "首监听者正常执行");
        assertEquals(1, last.captured.size(), "末监听者应不受中间监听者失败影响");
        assertEquals(1, failures.size(), "应收集到 1 条失败记录");
        ErpFinReversalListenerRegistry.ListenerFailure f = failures.get(0);
        assertNotNull(f.getListenerName(), "失败记录含 listenerName");
        assertEquals(ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED.getErrorCode(), f.getErrorCode(),
                "失败记录含 ErrorCode");
        assertTrue(f.getErrorMessage() != null && !f.getErrorMessage().isEmpty(),
                "失败记录含 errorMessage");
    }

    @Test
    public void testAddListenerAppends() {
        ErpFinReversalListenerRegistry registry = new ErpFinReversalListenerRegistry();
        registry.init();
        assertEquals(0, registry.getListeners().size(), "初始无监听者");

        CapturingListener listener = new CapturingListener();
        registry.addListener(listener);

        assertEquals(1, registry.getListeners().size(), "addListener 应追加");
        assertSame(listener, registry.getListeners().get(0));
    }
}
