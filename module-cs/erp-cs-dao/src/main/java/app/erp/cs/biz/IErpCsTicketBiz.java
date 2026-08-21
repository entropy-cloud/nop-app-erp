
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
    ErpCsTicket assign(@Name("ticketId") String ticketId,
                       @Optional @Name("assignedToId") String assignedToId,
                       IServiceContext context);

    @BizMutation
    ErpCsTicket start(@Name("ticketId") String ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket resolve(@Name("ticketId") String ticketId,
                        @Optional @Name("resolution") String resolution,
                        IServiceContext context);

    @BizMutation
    ErpCsTicket close(@Name("ticketId") String ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket reopen(@Name("ticketId") String ticketId, IServiceContext context);

    @BizMutation
    ErpCsTicket cancel(@Name("ticketId") String ticketId,
                       @Optional @Name("cancelReason") String cancelReason,
                       IServiceContext context);

    @BizMutation
    ErpCsTicket matchAndAttachSla(@Name("ticketId") String ticketId, IServiceContext context);

    @BizMutation
    List<ErpCsTicket> scanOverdueTickets(IServiceContext context);

    @BizQuery
    List<ErpCsTicket> findSlaWarnings(@Optional @Name("beforeMinutes") Integer beforeMinutes,
                                      IServiceContext context);

    /**
     * 采纳知识库文章（UC-CS-05 ⑤⑦⑧，RC-R1.69）：写 ADOPT_KNOWLEDGE 审计行（content 固定整串
     * {@code knowledgeBaseId={id}}）；{@code autoResolve=true} → 委托 resolveProcessor 转 RESOLVED
     * （复用既有 resolve 状态机守卫/审计/survey 触发链）。
     */
    @BizMutation
    ErpCsTicket adoptKnowledge(@Name("ticketId") String ticketId,
                               @Name("knowledgeBaseId") String knowledgeBaseId,
                               @Optional @Name("autoResolve") Boolean autoResolve,
                               IServiceContext context);

    // ---------- cs 质量事件联动（RC-R1.68，P1-RC-057，UC-CS-06） ----------

    /**
     * 工单升级为质量事件（UC-CS-06 流程①-④）：IN_PROCESS 守卫 + materialId/defectDescription 必填
     * （守卫链在 Processor 内，参数声明 Optional 以便域错误码成为拒绝点），
     * 经 {@code IErpQaNonConformanceBiz.save} 创建 NCR（sourceType=CS_TICKET + sourceCode=ticket.code
     * 双弱指针反向关联），写 QUALITY_ESCALATE 审计行（content=NCR:{code}）；quality 调用失败降级为
     * PENDING 审计行（工单状态保持，后台 job 重试）。工单不迁移状态（L1 ④ NCR 流程独立）。
     */
    @BizMutation
    ErpCsTicket escalateToQuality(@Name("ticketId") String ticketId,
                                  @Optional @Name("materialId") String materialId,
                                  @Optional @Name("defectDescription") String defectDescription,
                                  @Optional @Name("batchInfo") String batchInfo,
                                  @Optional @Name("quantity") java.math.BigDecimal quantity,
                                  @Optional @Name("severity") String severity,
                                  @Optional @Name("supplierId") String supplierId,
                                  IServiceContext context);

    /** 工单关联 NCR 闭环结果投影（UC-CS-06 ⑤）：{code,status,severity,ncrDate,resolvedAt,resolution}。 */
    @BizQuery
    List<Map<String, Object>> findQualityNcrs(@Name("ticketId") String ticketId, IServiceContext context);

    /**
     * 看板扁平图结构聚合查询（flux kanban 原生渲染）。6 列（NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）
     * + 每工单一个 card 节点（含 SLA 标记 data）。
     */
    @BizQuery
    Map<String, Object> findBoardData(@Optional @Name("customerId") String customerId, IServiceContext context);

    // ---------- 工单总计时聚合（RC-R1.66，UC-CS-11 ⑦；SQL 聚合口径 owner doc time-tracking.md §四，零 ticket 加列） ----------

    /** 总处理时长（分钟）＝ SUM(duration)，approvalStatus IN (APPROVED, PENDING)。 */
    @BizQuery
    long totalTimeSpent(@Name("ticketId") String ticketId, IServiceContext context);

    /** 总可计费时长（分钟）＝ SUM(duration)，isBillable=true AND approvalStatus=APPROVED。 */
    @BizQuery
    long totalBillableTime(@Name("ticketId") String ticketId, IServiceContext context);

    /** 总计费金额＝ SUM(billableAmount)，isBillable=true AND approvalStatus=APPROVED。 */
    @BizQuery
    java.math.BigDecimal totalBilledAmount(@Name("ticketId") String ticketId, IServiceContext context);
}
