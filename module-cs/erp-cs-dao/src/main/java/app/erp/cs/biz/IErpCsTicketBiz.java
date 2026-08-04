
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsTicket;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.orm.biz.ICrudBiz;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

public interface IErpCsTicketBiz extends ICrudBiz<ErpCsTicket> {

    @BizMutation
    ErpCsTicket assign(@Name("ticketId") Long ticketId,
                       @Optional @Name("assignedToId") String assignedToId,
                       IServiceContext context);

    @BizMutation
    ErpCsTicket start(@Name("ticketId") Long ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket resolve(@Name("ticketId") Long ticketId,
                        @Optional @Name("resolution") String resolution,
                        IServiceContext context);

    @BizMutation
    ErpCsTicket close(@Name("ticketId") Long ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket reopen(@Name("ticketId") Long ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket cancel(@Name("ticketId") Long ticketId,
                       @Optional @Name("cancelReason") String cancelReason,
                       IServiceContext context);

    @BizMutation
    ErpCsTicket matchAndAttachSla(@Name("ticketId") Long ticketId, IServiceContext context);

    @BizMutation
    List<ErpCsTicket> scanOverdueTickets(IServiceContext context);

    @BizQuery
    List<ErpCsTicket> findSlaWarnings(@Optional @Name("beforeMinutes") Integer beforeMinutes,
                                      IServiceContext context);

    @BizMutation
    ErpCsTicket adoptKnowledge(@Name("ticketId") Long ticketId,
                               @Name("knowledgeBaseId") Long knowledgeBaseId,
                               IServiceContext context);

    /**
     * 看板扁平图结构聚合查询（flux kanban 原生渲染）。6 列（NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）
     * + 每工单一个 card 节点（含 SLA 标记 data）。
     */
    @BizQuery
    Map<String, Object> findBoardData(@Optional @Name("customerId") Long customerId, IServiceContext context);
}
