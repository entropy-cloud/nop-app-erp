package app.erp.prj.service.posting;

import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.notify.biz.IErpSysNotificationBiz;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * G3 错误传播分级 Proof（plan 2026-07-30-0341-2 Phase 4 P1-MA2-068）：
 * 工时过账失败时 TimesheetPostingDispatcher 派发 IErpSysNotificationBiz 告警（prj.timesheet-posting-failure）。
 *
 * <p>单元测试直接构造 dispatcher + Proxy 桩 notificationBiz，验证 catch-swallow→告警 闭环。
 */
public class TestTimesheetPostingFailureAlert {

    @SuppressWarnings("unchecked")
    private IErpSysNotificationBiz recordingNotify(String[] capturedEventType) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("notify".equals(method.getName()) && args.length == 3) {
                capturedEventType[0] = (String) args[0];
            }
            return java.util.Collections.emptyList();
        };
        return (IErpSysNotificationBiz) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{IErpSysNotificationBiz.class}, handler);
    }

    @Test
    public void testFailureDispatchesAlert() {
        TimesheetPostingDispatcher dispatcher = new TimesheetPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = recordingNotify(captured);

        ErpPrjTimesheet timesheet = new ErpPrjTimesheet();
        timesheet.setCode("TS-FAIL-001");

        dispatcher.dispatchFailureAlert(timesheet,
                new RuntimeException("GL engine down"));

        assertEquals(TimesheetPostingDispatcher.NOTIFY_EVENT_TIMESHEET_FAILURE, captured[0],
                "工时过账失败应派发 prj.timesheet-posting-failure 告警");
    }
}
