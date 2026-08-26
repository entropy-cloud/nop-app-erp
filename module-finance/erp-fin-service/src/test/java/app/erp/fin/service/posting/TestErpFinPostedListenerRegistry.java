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
 * F2.1（P1-CK-fin-001）：正向过账监听者注册中心契约测试。镜像 {@link TestErpFinReversalListenerRegistry}
 * （红冲对偶）——纯单元测试（不启动 IoC），验证：
 * <ul>
 *   <li>空监听者列表时 {@code dispatch} 空操作不报错；</li>
 *   <li>同步遍历所有监听者，事件字段原样透传；</li>
 *   <li>失败隔离：单个监听者抛 {@link NopException} 不中断其他监听者；</li>
 *   <li>失败记录承载 listenerName/errorCode/errorMessage。</li>
 * </ul>
 */
public class TestErpFinPostedListenerRegistry {

    private static final IServiceContext CTX = new ServiceContextImpl();

    /** 捕获事件的测试监听者。 */
    private static class CapturingListener implements IErpFinVoucherPostedListener {
        final List<VoucherPostedEvent> captured = new ArrayList<>();

        @Override
        public void onVoucherPosted(VoucherPostedEvent event, IServiceContext context) {
            captured.add(event);
        }
    }

    /** 总是抛错的测试监听者。 */
    private static class FailingListener implements IErpFinVoucherPostedListener {
        @Override
        public void onVoucherPosted(VoucherPostedEvent event, IServiceContext context) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED)
                    .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, event.getBillHeadCode());
        }
    }

    private VoucherPostedEvent sampleEvent() {
        VoucherPostedEvent event = new VoucherPostedEvent();
        event.setVoucherId("3003");
        event.setBillHeadCode("EC-POST-001");
        event.setBusinessType("EXPENSE_CLAIM");
        event.setBillType("EXPENSE_CLAIM");
        event.setTraceId("TRACE-F21-001");
        return event;
    }

    @Test
    public void testEmptyListenersDispatchIsNoOp() {
        ErpFinPostedListenerRegistry registry = new ErpFinPostedListenerRegistry();
        registry.init();

        List<ErpFinPostedListenerRegistry.ListenerFailure> failures =
                registry.dispatch(sampleEvent(), CTX);

        assertTrue(failures.isEmpty(), "无监听者时派发应为空操作，返回空失败列表");
    }

    @Test
    public void testEventFieldsPassedThrough() {
        CapturingListener capturing = new CapturingListener();
        ErpFinPostedListenerRegistry registry = new ErpFinPostedListenerRegistry();
        registry.setListeners(Collections.singletonList(capturing));
        registry.init();

        VoucherPostedEvent event = sampleEvent();
        registry.dispatch(event, CTX);

        assertEquals(1, capturing.captured.size(), "监听者应收到 1 次事件");
        VoucherPostedEvent received = capturing.captured.get(0);
        assertSame(event, received, "事件应原样透传");
        assertEquals("3003", received.getVoucherId(), "voucherId 字段正确");
        assertEquals("EC-POST-001", received.getBillHeadCode(), "billHeadCode 字段正确");
        assertEquals("EXPENSE_CLAIM", received.getBusinessType(), "businessType 字段正确");
        assertEquals("EXPENSE_CLAIM", received.getBillType(), "billType 字段正确");
        assertEquals("TRACE-F21-001", received.getTraceId(), "traceId 字段正确");
    }

    @Test
    public void testFailureIsolationDoesNotBreakOtherListeners() {
        CapturingListener first = new CapturingListener();
        FailingListener middle = new FailingListener();
        CapturingListener last = new CapturingListener();
        ErpFinPostedListenerRegistry registry = new ErpFinPostedListenerRegistry();
        registry.setListeners(Arrays.asList(first, middle, last));
        registry.init();

        List<ErpFinPostedListenerRegistry.ListenerFailure> failures =
                registry.dispatch(sampleEvent(), CTX);

        assertEquals(1, first.captured.size(), "首监听者正常执行");
        assertEquals(1, last.captured.size(), "末监听者应不受中间监听者失败影响");
        assertEquals(1, failures.size(), "应收集到 1 条失败记录");
        ErpFinPostedListenerRegistry.ListenerFailure f = failures.get(0);
        assertNotNull(f.getListenerName(), "失败记录含 listenerName");
        assertEquals(ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED.getErrorCode(), f.getErrorCode(),
                "失败记录含 ErrorCode");
        assertTrue(f.getErrorMessage() != null && !f.getErrorMessage().isEmpty(),
                "失败记录含 errorMessage");
    }

    @Test
    public void testAddListenerAppends() {
        CapturingListener first = new CapturingListener();
        CapturingListener second = new CapturingListener();
        ErpFinPostedListenerRegistry registry = new ErpFinPostedListenerRegistry();
        registry.addListener(first);
        registry.addListener(second);
        registry.init();

        registry.dispatch(sampleEvent(), CTX);

        assertEquals(1, first.captured.size(), "编程式注册的首监听者收到事件");
        assertEquals(1, second.captured.size(), "编程式注册的次监听者收到事件");
    }
}
