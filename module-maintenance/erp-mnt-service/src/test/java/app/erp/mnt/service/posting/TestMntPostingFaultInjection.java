package app.erp.mnt.service.posting;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntVisit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * maintenance G4 故障注入测试（A4-alert，设计文档 §5.2）。harness 首次覆盖 maintenance 域。
 *
 * <p>断言契约（设计文档 §4.2 G4）：
 * <ul>
 *   <li>A2+A4（告警闭环）：captured event type = {@code mnt.labor-posting-failure}</li>
 *   <li>A1（posted 一致性）：{@code dispatchFailureAlert} 仅在 {@code postLabor} catch 块内被调用，
 *       catch 同时返回 false（posted 语义不变），经代码审查核验</li>
 * </ul>
 *
 * <p>恢复路径：告警 + 期末试算平衡人工发现（不纳入前置检查，对齐 posting-log.md line 136）。
 */
public class TestMntPostingFaultInjection {

    @Test
    public void testLaborPostingFailureDispatchesAlert() {
        MaintenanceLaborPostingDispatcher dispatcher = new MaintenanceLaborPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpMntVisit visit = new ErpMntVisit();
        visit.setCode("VST-FAIL-001");

        dispatcher.dispatchFailureAlert(visit, FaultInjectionStubs.testFault("test.mnt-posting-down"));

        assertEquals(MaintenanceLaborPostingDispatcher.NOTIFY_EVENT_MAINTENANCE_LABOR_FAILURE, captured[0],
                "维修工时过账失败应派发 mnt.labor-posting-failure 告警（A2+A4）");
    }

    @Test
    public void testIssuePostingFailureDispatchesAlert() {
        MaintenanceIssuePostingDispatcher dispatcher = new MaintenanceIssuePostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpMntSparePartUsage usage = new ErpMntSparePartUsage();
        usage.setCode("SPU-FAIL-001");

        dispatcher.dispatchFailureAlert(usage, FaultInjectionStubs.testFault("test.mnt-issue-posting-down"));

        assertEquals(MaintenanceIssuePostingDispatcher.NOTIFY_EVENT_MAINTENANCE_ISSUE_FAILURE, captured[0],
                "维修备件消耗过账失败应派发 mnt.spare-part-posting-failure 告警（A2+A4）");
    }
}
