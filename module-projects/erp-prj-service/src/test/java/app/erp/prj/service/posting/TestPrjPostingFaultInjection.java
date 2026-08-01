package app.erp.prj.service.posting;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * projects G4 故障注入测试（A4-alert，设计文档 §5.2）。
 *
 * <p>复用既有先例 {@code TestTimesheetPostingFailureAlert} 范式，改用 harness
 * {@link FaultInjectionStubs#recordingNotificationBiz} 统一桩（证明 harness 被消费）。
 *
 * <p>断言契约（设计文档 §4.2 G4）：
 * <ul>
 *   <li>A2+A4（告警闭环）：captured event type = {@code prj.timesheet-posting-failure}</li>
 *   <li>A1（posted 一致性）：{@code dispatchFailureAlert} 仅在 {@code tryPost} catch 块内被调用，
 *       catch 同时返回 false（posted 不被置 true），经代码审查核验</li>
 * </ul>
 *
 * <p>恢复路径：告警 + 期末试算平衡人工发现（不纳入前置检查，对齐 posting-log.md line 136）。
 */
public class TestPrjPostingFaultInjection {

    @Test
    public void testTimesheetPostingFailureDispatchesAlert() {
        TimesheetPostingDispatcher dispatcher = new TimesheetPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpPrjTimesheet timesheet = new ErpPrjTimesheet();
        timesheet.setCode("TS-FAIL-001");

        dispatcher.dispatchFailureAlert(timesheet, FaultInjectionStubs.testFault("test.prj-posting-down"));

        assertEquals(TimesheetPostingDispatcher.NOTIFY_EVENT_TIMESHEET_FAILURE, captured[0],
                "工时过账失败应派发 prj.timesheet-posting-failure 告警（A2+A4）");
    }
}
