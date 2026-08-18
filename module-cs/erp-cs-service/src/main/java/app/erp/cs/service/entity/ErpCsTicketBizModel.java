package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.biz.IErpCsTimeEntryBiz;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.dao.entity.ErpCsTicketType;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.processor.ErpCsTicketEscalateToQualityProcessor;
import app.erp.cs.service.processor.ErpCsTicketMatchAndAttachSlaProcessor;
import app.erp.cs.service.processor.ErpCsTicketReopenProcessor;
import app.erp.cs.service.processor.ErpCsTicketResolveProcessor;
import app.erp.cs.service.processor.ErpCsTicketScanOverdueTicketsProcessor;
import app.erp.cs.service.statemachine.ErpCsTicketStateMachine;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.EntityData;

/**
 * 客服工单 BizModel。权威：{@code docs/design/customer-service/state-machine.md}、
 * {@code docs/design/customer-service/sla.md}、{@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 1。
 *
 * <p>六态状态机（NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）：assign/start/resolve/close/reopen/cancel。
 * 非法迁移抛 {@link ErpCsErrors#ERR_INVALID_TICKET_STATUS_TRANSITION}；终态抛 {@link ErpCsErrors#ERR_TICKET_ALREADY_TERMINAL}。
 * 每迁移写 {@link ErpCsTicketAction} 审计（actionType 映射见 plan Decision；fromStatus/toStatus 承载精确迁移）。
 *
 * <p>SLA：{@link #matchAndAttachSla} 匹配策略 + 算 deadline；resolve 标记 isSlaCompleted；
 * {@link #scanOverdueTickets} 超时升级（ESCALATE）；{@link #findSlaWarnings} 预警查询。
 *
 * <p>CSAT 触发：resolve 成功后（config-gated）调 {@link IErpCsSurveyBiz#createSurvey}；
 * reopen 时取消未响应的调查（删除 SENT 状态调查避免误发）。
 */
@BizModel("ErpCsTicket")
public class ErpCsTicketBizModel extends CrudBizModel<ErpCsTicket> implements IErpCsTicketBiz {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpCsTicketBizModel.class);

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IErpCsTimeEntryBiz timeEntryBiz;
    @Inject
    ErpCsTicketStateMachine stateMachine;
    @Inject
    ErpCsTicketMatchAndAttachSlaProcessor matchAndAttachSlaProcessor;
    @Inject
    ErpCsTicketReopenProcessor reopenProcessor;
    @Inject
    ErpCsTicketResolveProcessor resolveProcessor;
    @Inject
    ErpCsTicketScanOverdueTicketsProcessor scanOverdueTicketsProcessor;
    @Inject
    ErpCsTicketEscalateToQualityProcessor escalateToQualityProcessor;
    @Inject
    TicketAssignResolver ticketAssignResolver;

    public ErpCsTicketBizModel() {
        setEntityName(ErpCsTicket.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCsTicket> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCsTicket entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
        // 创建自动富化 fill-when-absent（plan 2026-08-17-2125-1 D1/D6，UC-CS-01 ②⑤）：
        // 显式传入永不覆盖；slaPolicyId 不在 save 侧填充（归属 matchAndAttachSla Processor 单一咽喉）
        if (entity.getStatus() == null) {
            entity.setStatus(ErpCsConstants.TICKET_STATUS_NEW);
        }
        if (entity.getPriority() == null && entity.getTicketTypeId() != null) {
            ErpCsTicketType type = entity.getTicketType();
            if (type != null && type.getDefaultPriority() != null) {
                entity.setPriority(type.getDefaultPriority());
            }
        }
    }

    /**
     * save 成功后置富化（plan 2026-08-17-2125-1 D1 选项 A，UC-CS-01 ③④⑤⑥⑦⑧ + UC-CS-09 reuse）：
     * 仅新建路径（{@code !isRecoverDeleted()}，D6——update/copy-for-new 外路径零触发；逻辑删除恢复不重复富化）。
     * ① 调用点守卫：slaPolicyId 与 deadlineDateTime 均为空才自动挂载（policy + deadline + 权益三合一，
     *    Processor 本体零改动）；② 自动分配（config 门控）；③ 客户确认通知（try/catch 降级）。
     */
    @Override
    public void doSaveEntity(EntityData<ErpCsTicket> entityData, IServiceContext context) {
        super.doSaveEntity(entityData, context);
        if (Boolean.TRUE.equals(entityData.isRecoverDeleted())) {
            return;
        }
        // 新建实体 flush 使 NEW→MANAGED：后置富化内 Processor/BizModel 的 updateEntity 语义可达
        // （OrmEntityDao.updateEntity 仅接受 MANAGED 态，未 flush 的同事务新建实体会被拒绝）
        dao().flushSession();
        enrichAfterCreate(entityData.getEntity(), context);
    }

    private void enrichAfterCreate(ErpCsTicket ticket, IServiceContext context) {
        // ① 自动挂载守卫（D1）：显式 slaPolicyId/deadline 已设 → 跳过（手动 mutation 仍可用）
        if (ticket.getSlaPolicyId() == null && ticket.getDeadlineDateTime() == null) {
            try {
                matchAndAttachSlaProcessor.matchAndAttachSla(ticket.getId(), context);
            } catch (Exception e) {
                LOG.warn("自动挂载 SLA 失败（降级，创建主流程继续）：ticketId={}, reason={}",
                        ticket.getId(), e.getMessage());
            }
        }
        // ② 自动分配（UC-CS-01 ④⑤⑦；config 仅门控分配维度，D6）
        autoAssignOnCreate(ticket, context);
        // ③ 客户确认通知（UC-CS-01 ⑥，含 TK 编号；IN_APP 占位语义）
        notifyTicketCreated(ticket, context);
    }

    /**
     * 自动分配（plan D3/D4）：team 解析（挂载策略 teamId → 工单类型默认策略 teamId，ORM 关系 getter）→
     * 同码 crm 团队成员池 → ROUND_ROBIN/LEAST_OPEN 挑人 → ASSIGNED + ASSIGN 审计（镜像 {@link #assign} 语义）；
     * config off / 池空 / 失败 → 留 NEW（池空时 ⑧ 升级通知客服主管）。
     */
    private void autoAssignOnCreate(ErpCsTicket ticket, IServiceContext context) {
        if (!ErpCsConfigs.isAutoAssignOnCreate()) {
            return;
        }
        if (!ErpCsConstants.TICKET_STATUS_NEW.equals(ticket.getStatus()) || ticket.getAssignedToId() != null) {
            return;
        }
        ErpCsTeam team = TicketAssignResolver.resolveTeam(ticket.getSlaPolicy(), resolveTypeDefaultPolicy(ticket));
        List<String> pool = ticketAssignResolver.resolveCandidatePool(team, context);
        if (pool.isEmpty()) {
            notifyAssignNoMatch(ticket, context);
            return;
        }
        String assignee = ticketAssignResolver.pickByConfig(pool, findLastAssigned(pool, context),
                countOpenTickets(pool, context));
        if (assignee == null) {
            notifyAssignNoMatch(ticket, context);
            return;
        }
        String from = ticket.getStatus();
        ticket.setAssignedToId(assignee);
        ticket.setStatus(stateMachine.assignTargetStatus());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_ASSIGN, from, stateMachine.assignTargetStatus(),
                "创建自动分配处理人: " + assignee, context);
    }

    /** 工单类型默认策略（②建议匹配 slaPolicy 载体；自动挂载主链 team 来源——SlaPolicyMatcher 仅匹配 teamId IS NULL）。 */
    private ErpCsSlaPolicy resolveTypeDefaultPolicy(ErpCsTicket ticket) {
        if (ticket.getTicketTypeId() == null) {
            return null;
        }
        ErpCsTicketType type = ticket.getTicketType();
        return type == null ? null : type.getDefaultSlaPolicy();
    }

    /** ROUND_ROBIN 历史：候选池成员内最近一张已分配工单（createTime desc limit 1）。 */
    private String findLastAssigned(List<String> pool, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(in("assignedToId", pool));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpCsTicket> last = findList(q, null, context);
        return last.isEmpty() ? null : last.get(0).getAssignedToId();
    }

    /** LEAST_OPEN 计数：候选成员活跃工单（ASSIGNED/IN_PROGRESS）按处理人分组计数。 */
    private Map<String, Integer> countOpenTickets(List<String> pool, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(in("assignedToId", pool));
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        List<ErpCsTicket> open = findList(q, null, context);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ErpCsTicket t : open) {
            if (t.getAssignedToId() != null) {
                counts.merge(t.getAssignedToId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setMdPartnerBiz(IErpMdPartnerBiz mdPartnerBiz) {
        this.mdPartnerBiz = mdPartnerBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    // ---------- 状态机 ----------

    @Override
    @BizMutation
    public ErpCsTicket assign(@Name("ticketId") Long ticketId,
                              @Optional @Name("assignedToId") String assignedToId,
                              IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        assertCan("assign", ticket, from, ErpCsConstants.TICKET_STATUS_NEW);
        ticket.setAssignedToId(assignedToId);
        ticket.setStatus(stateMachine.assignTargetStatus());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_ASSIGN, from, stateMachine.assignTargetStatus(),
                "分派处理人: " + assignedToId, context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket start(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        assertCan("start", ticket, from, ErpCsConstants.TICKET_STATUS_ASSIGNED);
        ticket.setStatus(stateMachine.startTargetStatus());
        // 计时起点：首次进入 IN_PROGRESS（见 plan Decision：startDateTime=首次 IN_PROGRESS 时间）
        ticket.setStartDateTime(CoreMetrics.currentTimestamp());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, stateMachine.startTargetStatus(),
                "开始处理", context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket resolve(@Name("ticketId") Long ticketId,
                               @Optional @Name("resolution") String resolution,
                               IServiceContext context) {
        return resolveProcessor.resolve(ticketId, resolution, context);
    }

    @Override
    @BizMutation
    public ErpCsTicket close(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        assertCan("close", ticket, from, ErpCsConstants.TICKET_STATUS_RESOLVED);
        // 关闭前检查：超时工单（isSlaCompleted=false）须在 remark 注明超时原因
        if (Boolean.FALSE.equals(ticket.getIsSlaCompleted())
                && (ticket.getRemark() == null || ticket.getRemark().trim().isEmpty())) {
            throw new NopException(ErpCsErrors.ERR_TICKET_CLOSE_BREACHED_NO_REASON)
                    .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode());
        }
        ticket.setStatus(stateMachine.closeTargetStatus());
        ticket.setEndDateTime(CoreMetrics.currentTimestamp());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_CLOSE, from, stateMachine.closeTargetStatus(),
                "关闭工单", context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket reopen(@Name("ticketId") Long ticketId, IServiceContext context) {
        return reopenProcessor.reopen(ticketId, context);
    }

    @Override
    @BizMutation
    public ErpCsTicket cancel(@Name("ticketId") Long ticketId,
                              @Optional @Name("cancelReason") String cancelReason,
                              IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        // 终态走领域码 ERR_TICKET_ALREADY_TERMINAL（保持既有外部错误码）；非终态经 Bean 矩阵守卫（cancel 非终态均合法）
        if (stateMachine.isTerminal(from)) {
            throw new NopException(ErpCsErrors.ERR_TICKET_ALREADY_TERMINAL)
                    .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                    .param(ErpCsErrors.ARG_CURRENT_STATUS, from);
        }
        stateMachine.assertCanCancel(from);
        ticket.setStatus(stateMachine.cancelTargetStatus());
        if (cancelReason != null) {
            ticket.setRemark(cancelReason);
        }
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_CANCEL, from, stateMachine.cancelTargetStatus(),
                "取消工单: " + (cancelReason == null ? "" : cancelReason), context);
        return ticket;
    }

    // ---------- SLA ----------

    /**
     * 采纳知识库文章（UC-CS-05 ⑤⑦⑧，RC-R1.69）：写独立 {@code ADOPT_KNOWLEDGE} 审计行
     * （content 固定整串 {@code knowledgeBaseId={id}}——派生统计 eq 精确匹配载体）；
     * {@code autoResolve=true} → 委托 {@link ErpCsTicketResolveProcessor} 转 RESOLVED
     * （L1 ⑤「如采纳的文章解决了问题，工单直接标记为 RESOLVED」；状态机守卫/RESOLVED 审计/survey
     * 触发链复用既有 resolve 路径）。adopt 审计行先写入，resolve 独立审计。
     */
    @Override
    @BizMutation
    public ErpCsTicket adoptKnowledge(@Name("ticketId") Long ticketId,
                                      @Name("knowledgeBaseId") Long knowledgeBaseId,
                                      @Optional @Name("autoResolve") Boolean autoResolve,
                                      IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String current = ticket.getStatus();
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_ADOPT_KNOWLEDGE, current, current,
                "knowledgeBaseId=" + knowledgeBaseId, context);
        if (Boolean.TRUE.equals(autoResolve)) {
            return resolveProcessor.resolve(ticketId,
                    "采纳知识库文章解决: knowledgeBaseId=" + knowledgeBaseId, context);
        }
        return ticket;
    }

    // ---------- cs 质量事件联动（RC-R1.68，P1-RC-057，UC-CS-06） ----------

    /**
     * 工单升级为质量事件（UC-CS-06 ①-④）：Facade 委托 per-mutation Processor——
     * NCR 双弱指针创建 + QUALITY_ESCALATE 审计 + 失败降级 PENDING（详见 Processor javadoc）。
     */
    @Override
    @BizMutation
    public ErpCsTicket escalateToQuality(@Name("ticketId") Long ticketId,
                                         @Optional @Name("materialId") Long materialId,
                                         @Optional @Name("defectDescription") String defectDescription,
                                         @Optional @Name("batchInfo") String batchInfo,
                                         @Optional @Name("quantity") java.math.BigDecimal quantity,
                                         @Optional @Name("severity") String severity,
                                         @Optional @Name("supplierId") Long supplierId,
                                         IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        return escalateToQualityProcessor.escalateToQuality(ticket, materialId, defectDescription,
                batchInfo, quantity, severity, supplierId, context);
    }

    /** 工单关联 NCR 闭环结果投影（UC-CS-06 ⑤，弱指针反查）。 */
    @Override
    @BizQuery
    public List<Map<String, Object>> findQualityNcrs(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        return escalateToQualityProcessor.findQualityNcrs(ticket, context);
    }

    @Override
    @BizMutation
    public ErpCsTicket matchAndAttachSla(@Name("ticketId") Long ticketId, IServiceContext context) {
        return matchAndAttachSlaProcessor.matchAndAttachSla(ticketId, context);
    }

    @Override
    @BizMutation
    public List<ErpCsTicket> scanOverdueTickets(IServiceContext context) {
        return scanOverdueTicketsProcessor.scanOverdueTickets(context);
    }

    @Override
    @BizQuery
    public List<ErpCsTicket> findSlaWarnings(@Optional @Name("beforeMinutes") Integer beforeMinutes,
                                              IServiceContext context) {
        int minutes = beforeMinutes != null ? beforeMinutes : ErpCsConfigs.getSlaWarningBeforeMinutes();
        LocalDateTime now = CoreMetrics.currentDateTime();
        QueryBean q = new QueryBean();
        // deadlineDateTime BETWEEN now AND now+beforeMinutes 且未完成（供 nop-job 预警）
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        q.addFilter(io.nop.api.core.beans.FilterBeans.dateTimeBetween("deadlineDateTime", now, now.plusMinutes(minutes)));
        q.addFilter(eq("isSlaCompleted", Boolean.FALSE));
        List<ErpCsTicket> warnings = findList(q, null, context);
        // 通知派发（config-gated）：临近 SLA 截止时预警通知，避免超期
        for (ErpCsTicket ticket : warnings) {
            notifySlaOverdue(ticket, context);
        }
        return warnings;
    }

    // ---------- helpers ----------

    @Override
    @BizQuery
    public Map<String, Object> findBoardData(@Optional @Name("customerId") Long customerId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.setLimit(200);
        if (customerId != null) {
            query.addFilter(eq("customerId", customerId));
        }
        List<ErpCsTicket> tickets = findList(query, null, context);

        String[] statuses = {ErpCsConstants.TICKET_STATUS_NEW, ErpCsConstants.TICKET_STATUS_ASSIGNED,
                ErpCsConstants.TICKET_STATUS_IN_PROGRESS, ErpCsConstants.TICKET_STATUS_RESOLVED,
                ErpCsConstants.TICKET_STATUS_CLOSED, ErpCsConstants.TICKET_STATUS_CANCELLED};
        String[] titles = {"新建", "已分派", "处理中", "已解决", "已关闭", "已取消"};

        Map<String, Object> board = new LinkedHashMap<>();
        List<String> rootChildren = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            rootChildren.add("col-" + statuses[i]);
        }
        board.put("root", boardNode("root", "root", null, rootChildren, null));

        for (int i = 0; i < statuses.length; i++) {
            String colId = "col-" + statuses[i];
            List<String> cardIds = new ArrayList<>();
            for (ErpCsTicket t : tickets) {
                if (Objects.equals(t.getStatus(), statuses[i])) {
                    cardIds.add("card-" + t.getId());
                }
            }
            Map<String, Object> colData = new LinkedHashMap<>();
            colData.put("title", titles[i]);
            colData.put("status", statuses[i]);
            board.put(colId, boardNode(colId, "column", null, cardIds, colData));
        }

        for (ErpCsTicket t : tickets) {
            String cardId = "card-" + t.getId();
            String colId = "col-" + t.getStatus();
            Map<String, Object> cardData = new LinkedHashMap<>();
            cardData.put("title", (t.getCode() != null ? t.getCode() : "") + " " + (t.getSubject() != null ? t.getSubject() : ""));
            cardData.put("ticketId", t.getId());
            cardData.put("code", t.getCode());
            cardData.put("subject", t.getSubject());
            cardData.put("priority", t.getPriority());
            cardData.put("status", t.getStatus());
            cardData.put("deadlineDateTime", t.getDeadlineDateTime());
            cardData.put("isSlaCompleted", t.getIsSlaCompleted());
            cardData.put("assignedToId", t.getAssignedToId());
            board.put(cardId, boardNode(cardId, "card", colId, new ArrayList<>(), cardData));
        }
        return board;
    }

    private static Map<String, Object> boardNode(String id, String type, String parentId,
                                                    List<String> children, Map<String, Object> data) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", type);
        if (parentId != null) node.put("parentId", parentId);
        node.put("children", children);
        if (data != null) node.put("data", data);
        return node;
    }

    // ---------- 工单总计时聚合（RC-R1.66，UC-CS-11 ⑦；口径 owner doc time-tracking.md §四，SQL 聚合零 ticket 加列） ----------

    @Override
    @BizQuery
    public long totalTimeSpent(@Name("ticketId") Long ticketId, IServiceContext context) {
        long sum = 0;
        for (ErpCsTimeEntry e : findEntries(ticketId, java.util.Arrays.asList(
                ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, ErpCsConstants.TIME_ENTRY_APPROVE_PENDING), context)) {
            if (e.getDuration() != null) {
                sum += e.getDuration();
            }
        }
        return sum;
    }

    @Override
    @BizQuery
    public long totalBillableTime(@Name("ticketId") Long ticketId, IServiceContext context) {
        long sum = 0;
        for (ErpCsTimeEntry e : findEntries(ticketId,
                java.util.Arrays.asList(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED), context)) {
            if (Boolean.TRUE.equals(e.getIsBillable()) && e.getDuration() != null) {
                sum += e.getDuration();
            }
        }
        return sum;
    }

    @Override
    @BizQuery
    public java.math.BigDecimal totalBilledAmount(@Name("ticketId") Long ticketId, IServiceContext context) {
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (ErpCsTimeEntry e : findEntries(ticketId,
                java.util.Arrays.asList(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED), context)) {
            if (Boolean.TRUE.equals(e.getIsBillable()) && e.getBillableAmount() != null) {
                sum = sum.add(e.getBillableAmount());
            }
        }
        return sum;
    }

    /** 经 IErpCsTimeEntryBiz findList 聚合口径查询（R2b 合规跨 BizModel 注入，plan D5）。 */
    private List<ErpCsTimeEntry> findEntries(Long ticketId, List<String> approvalStatuses,
                                             IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("ticketId", ticketId));
        query.addFilter(in("approvalStatus", approvalStatuses));
        return timeEntryBiz.findList(query, null, context);
    }

    /**
     * SLA 预警通知派发（config-gated by {@link ErpCsConfigs#isSlaNotifyEnabled}，findSlaWarnings 路径）。
     *
     * <p>复用既有 {@code cs.sla-overdue} 模板（7101 已改 USER_LIST 插值）。接收人目标修正（RC-R1.67 plan D4，
     * 修正既有漂移）：{@code escalationUserId} = policy.escalationUserId（BIGINT → stringify 类型归一化）优先，
     * 缺失回退 assignedToId。模板缺失或 notify 失败时静默降级（不阻断业务）。
     */
    private void notifySlaOverdue(ErpCsTicket ticket, IServiceContext context) {
        if (!ErpCsConfigs.isSlaNotifyEnabled()) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket.getCustomerId(), context));
            ctx.put("escalationUserId", resolveEscalationTarget(ticket));
            // 预警路径（未升级）级别/次数占位 0：模板 7101 主体插值键齐备，避免缺键渲染
            ctx.put("escalationLevel", 0);
            ctx.put("repeatCount", 0);
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE, ctx, context);
        } catch (Exception e) {
            // 通知派发失败不阻断 SLA 升级主流程（config-gated 降级语义）
            LOG.warn("SLA notify 派发失败（降级，主升级流程继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    /**
     * L1/预警通知目标：policy.escalationUserId（BIGINT → stringify）优先，缺失回退 assignedToId
     * （RC-R1.67 plan D4——与 ScanOverdueTicketsProcessor.resolveL1Target 同语义）。
     */
    private static String resolveEscalationTarget(ErpCsTicket ticket) {
        ErpCsSlaPolicy policy = ticket.getSlaPolicy();
        if (policy != null && policy.getEscalationUserId() != null) {
            return String.valueOf(policy.getEscalationUserId());
        }
        return ticket.getAssignedToId();
    }

    /**
     * 工单创建确认通知（UC-CS-01 ⑥，plan D5）：USER_LIST ${submitterUserId}=提单人（createdBy）插值，
     * 上下文含 TK 编号；客户为 ErpMdPartner 非系统用户，IN_APP 占位语义（实际邮件/门户投递归 nop-notification successor）。
     * notify 失败静默降级不阻断创建主流程（镜像 {@link #notifySlaOverdue} 范式）。
     */
    private void notifyTicketCreated(ErpCsTicket ticket, IServiceContext context) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket.getCustomerId(), context));
            ctx.put("submitterUserId", ticket.getCreatedBy());
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_TICKET_CREATED, ctx, context);
        } catch (Exception e) {
            LOG.warn("工单创建确认通知派发失败（降级，主流程继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    /**
     * 自动分派无匹配升级通知（UC-CS-01 ⑧，plan D5）：ROLE 客服主管（对齐 cs.sla-overdue 7101 先例）；
     * 工单留 NEW 待人工分派。notify 失败静默降级。
     */
    private void notifyAssignNoMatch(ErpCsTicket ticket, IServiceContext context) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket.getCustomerId(), context));
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_TICKET_ASSIGN_NO_MATCH, ctx, context);
        } catch (Exception e) {
            LOG.warn("分派无匹配升级通知派发失败（降级，主流程继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    private String resolveCustomerName(Long customerId, IServiceContext context) {
        if (customerId == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(customerId, context);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return requireEntity(String.valueOf(ticketId), null, context);
    }

    /**
     * 经 StateMachine Bean 断言来源态合法；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_INVALID_TICKET_STATUS_TRANSITION} + 实体编号/上下文，common 码作 cause 保留（契约 §7）。
     */
    private void assertCan(String action, ErpCsTicket ticket, String from, String expected) {
        try {
            switch (action) {
                case "assign":
                    stateMachine.assertCanAssign(from);
                    break;
                case "start":
                    stateMachine.assertCanStart(from);
                    break;
                case "close":
                    stateMachine.assertCanClose(from);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected action: " + action);
            }
        } catch (NopException e) {
            throw illegalTransition(ticket, from, expected, e);
        }
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current, String expected) {
        return illegalTransition(ticket, current, expected, null);
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current, String expected, Throwable cause) {
        NopException ex = new NopException(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION, cause)
                .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, expected);
        return ex;
    }

    private void writeAction(ErpCsTicket ticket, String actionType, String fromStatus, String toStatus,
                             String content, IServiceContext context) {
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticket.getId());
        action.setActionType(actionType);
        action.setFromStatus(fromStatus);
        action.setToStatus(toStatus);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    

}
