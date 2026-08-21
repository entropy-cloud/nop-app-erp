
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.orm.biz.ICrudBiz;
import io.nop.core.context.IServiceContext;

/**
 * 工单计时器会话（RC-R1.66，P1-RC-055，UC-CS-11 ②③④⑧⑨）。
 *
 * <p>单计时器不变量：同一客服同一时刻只能存在一个进行中（RUNNING/PAUSED）会话——
 * 服务端拒绝二次启动（专属错误码，owner doc §2.2「自动停止+确认」由客户端 stop→start 编排实现，plan D1-②）。
 * 12h 上限经惰性结算：各入口先按 elapsed > max-hours 封顶结算（plan D3）。
 */
public interface IErpCsTicketTimerSessionBiz extends ICrudBiz<ErpCsTicketTimerSession> {

    /** 客服开始计时（UC-CS-11 ②）：创建 RUNNING 会话；已有进行中会话则拒绝。 */
    @BizMutation
    ErpCsTicketTimerSession startTimer(@Name("ticketId") String ticketId,
                                       IServiceContext context);

    /** 暂停计时（UC-CS-11 ③）：RUNNING→PAUSED，暂停原因可选。 */
    @BizMutation
    ErpCsTicketTimerSession pauseTimer(@Name("sessionId") String sessionId,
                                       @Optional @Name("pauseReason") String pauseReason,
                                       IServiceContext context);

    /** 恢复计时（UC-CS-11 ③）：PAUSED→RUNNING，累计暂停时长结算。 */
    @BizMutation
    ErpCsTicketTimerSession resumeTimer(@Name("sessionId") String sessionId,
                                        IServiceContext context);

    /** 停止计时（UC-CS-11 ④）：STOPPED + 生成 ErpCsTimeEntry（duration = 运行 − Σ暂停，source=TIMER_IMPORT）。 */
    @BizMutation
    ErpCsTicketTimerSession stopTimer(@Name("sessionId") String sessionId,
                                      IServiceContext context);

    /** 查询客服当前进行中的计时器（读取时惰性结算：超 12h 会话先封顶停止后返回 null）。 */
    @BizQuery
    ErpCsTicketTimerSession findActiveTimer(@Optional @Name("agentId") String agentId,
                                            IServiceContext context);
}
