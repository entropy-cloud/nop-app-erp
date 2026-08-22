package app.erp.hr.service.posting;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.hr.dao.entity.ErpHrSalary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * hr G4 故障注入测试（A4-alert，设计文档 §5.2）。harness 首次覆盖 hr 域。
 *
 * <p>断言契约（设计文档 §4.2 G4）：
 * <ul>
 *   <li>A1（posted 一致性）：{@code tryPostPayment} 返回 false（过账失败 → posted 不被置 true）</li>
 *   <li>A2+A4（告警闭环）：captured event type = {@code hr.salary-posting-failure}</li>
 * </ul>
 *
 * <p>恢复路径：告警 + 期末试算平衡人工发现（不纳入前置检查，对齐 posting-log.md line 136）。
 */
public class TestHrPostingFaultInjection {

    @Test
    public void testSalaryPostingFailureReturnsFalseAndAlerts() {
        SalaryPostingDispatcher dispatcher = new SalaryPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpHrSalary salary = new ErpHrSalary();
        salary.setId("1001");
        salary.setYear(2026);
        salary.setMonth(8);
        salary.setEmployeeId(null);

        boolean posted = dispatcher.tryPostPayment(salary);

        assertFalse(posted,
                "薪酬过账失败应吞异常返回 false（保持 posted=false，A1）");
        assertEquals(SalaryPostingDispatcher.NOTIFY_EVENT_SALARY_FAILURE, captured[0],
                "薪酬过账失败应派发 hr.salary-posting-failure 告警（A2+A4）");
    }

    @Test
    public void testPostingFailureDispatchesAlert() {
        SalaryPostingDispatcher dispatcher = new SalaryPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpHrSalary salary = new ErpHrSalary();
        salary.setId("1002");
        salary.setYear(2026);
        salary.setMonth(8);

        dispatcher.dispatchFailureAlert(salary, "发放", FaultInjectionStubs.testFault("test.hr-posting-down"));

        assertEquals(SalaryPostingDispatcher.NOTIFY_EVENT_SALARY_FAILURE, captured[0],
                "薪酬过账失败应派发 hr.salary-posting-failure 告警（A4）");
    }
}
