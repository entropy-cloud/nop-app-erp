package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * ErpCsTicket scanOverdueTickets per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SLA 超时多级升级链编排（RC-R1.67 / P1-RC-056 / UC-CS-04 ⑩，plan 2026-08-17-2125-3）：
 * L1 通知 escalationUserId（policy 优先/assignedToId 回退）→ 每 escalationDelayHours 重复（上限 1+max-repeat 次）
 * → L2 通知 secondEscalationUserId（null 跳级）→ L3 通知 config 总监 → 封顶等 resolve。
 * 幂等语义（R1.28 P1-MA2-086 保持）：计数器/时间戳与 ESCALATE 审计同事务写入（versionProp 乐观锁），
 * 窗口内（now − lastEscalationAt &lt; delayHours）重复扫描天然跳过。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketScanOverdueTicketsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsTicketScanOverdueTicketsProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public List<ErpCsTicket> scanOverdueTickets(IServiceContext context) {
        if (!ErpCsConfigs.isSlaEnabled()) {
            return new ArrayList<>();
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        QueryBean q = new QueryBean();
        // status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now AND isSlaCompleted=false
        //（多级化后查询过滤维持不变：级别/次数/时间窗判定逐单进行，见 escalateOne）
        q.addFilter(in("status", Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        q.addFilter(lt("deadlineDateTime", now));
        q.addFilter(eq("isSlaCompleted", Boolean.FALSE));
        // deadlineDateTime 的 XMeta 仅允许 eq/in/dateBetween/dateTimeBetween（不支持 lt），
        // 内部派生查询走 entity dao 绕过 meta 限制（同 ErpCrmEventBizModel.findDueReminders 模式）
        List<ErpCsTicket> overdue = dao().findAllByQuery(q);
        List<ErpCsTicket> escalated = new ArrayList<>();
        for (ErpCsTicket ticket : overdue) {
            try {
                if (escalateOne(ticket, now, context)) {
                    escalated.add(ticket);
                }
            } catch (Exception ex) {
                // 逐单失败隔离（plan D3，对齐 R1.4/R1.35 批量 job 先例）：单工单乐观锁冲突等失败仅 WARN
                // 不中止本轮批量，下轮 cron 自然重试
                LOG.warn("sla-escalation-failed: ticketId={}, reason={}", ticket.getId(), ex.getMessage());
            }
        }
        return escalated;
    }

    /**
     * 单工单多级升级判定与执行（plan D1/D3 精确判定式）。
     *
     * <p>可升级 ⇔ lastEscalationAt==null ∨ now−lastEscalationAt ≥ delayHours；level≥3 封顶跳过。
     * level=0 → L1；level=1 ∧ count&lt;1+maxRepeat → L1 重复；level=1 ∧ count≥1+maxRepeat →
     * L2（secondEscalationUserId 空则跳级 L3）；level=2 → L3（config 总监，空则 WARN 跳过 + 推进窗口时间戳）。
     * L1 目标双 null（policy.escalationUserId 与 assignedToId 均空）时仍写审计 + 推进计数链，
     * 通知经模板接收人解析静默降级（对齐既有「接收人解析为空 WARN 不阻断」范式）。
     *
     * @return 是否实际执行了一次升级（审计 + 计数器推进）
     */
    protected boolean escalateOne(ErpCsTicket ticket, LocalDateTime now, IServiceContext context) {
        int level = ticket.getLastEscalationLevel() == null ? 0 : ticket.getLastEscalationLevel();
        int count = ticket.getEscalationCount() == null ? 0 : ticket.getEscalationCount();
        if (level >= ErpCsConstants.ESCALATION_LEVEL_MAX) {
            return false;
        }
        ErpCsSlaPolicy policy = ticket.getSlaPolicy();
        int delayHours = resolveDelayHours(policy);
        // 时间窗幂等判定（R1.28 语义保持：窗口内重复扫描跳过）
        if (ticket.getLastEscalationAt() != null
                && ticket.getLastEscalationAt().toLocalDateTime().plusHours(delayHours).isAfter(now)) {
            return false;
        }

        int nextLevel;
        String target;
        if (level == 0) {
            nextLevel = 1;
            target = resolveL1Target(ticket, policy);
        } else if (level == 1) {
            if (count < 1 + ErpCsConfigs.getEscalationMaxRepeat()) {
                nextLevel = 1;
                target = resolveL1Target(ticket, policy);
            } else {
                Long second = policy != null ? policy.getSecondEscalationUserId() : null;
                if (second != null) {
                    nextLevel = 2;
                    target = String.valueOf(second);
                } else {
                    nextLevel = ErpCsConstants.ESCALATION_LEVEL_MAX;
                    target = ErpCsConfigs.getEscalationL3UserId();
                }
            }
        } else {
            nextLevel = ErpCsConstants.ESCALATION_LEVEL_MAX;
            target = ErpCsConfigs.getEscalationL3UserId();
        }

        if (nextLevel == ErpCsConstants.ESCALATION_LEVEL_MAX && (target == null || target.isEmpty())) {
            // L3 目标不可解析（config 空，plan D6）：WARN 跳过级别推进/通知/审计，
            // 仅推进窗口时间戳——约束 WARN 频率至每窗口至多一次，且配置后补后下一窗口可真实升级。
            // （L1 目标 null 不走此分支：升级事件审计与计数链独立于通知目标可达性——通知侧静默降级，
            // 对齐既有「模板接收人解析为空 WARN 不阻断」范式）
            LOG.warn("sla-escalation-l3-target-unresolved: ticketId={}, fromLevel={}",
                    ticket.getId(), level);
            ticket.setLastEscalationAt(Timestamp.valueOf(now));
            dao().updateEntity(ticket);
            return false;
        }

        if (target == null || target.isEmpty()) {
            // L1 双 null 回退穷尽（policy.escalationUserId 与 assignedToId 均空）：仍写审计 + 推进计数链，
            // 通知经模板接收人解析降级（USER_LIST 插值空 → 接收人空，notify 子系统 WARN 不阻断）
            LOG.warn("sla-escalation-l1-target-unresolved-notify-degraded: ticketId={}", ticket.getId());
        }

        int nextCount = count + 1;
        ticket.setLastEscalationLevel(nextLevel);
        ticket.setEscalationCount(nextCount);
        ticket.setLastEscalationAt(Timestamp.valueOf(now));
        dao().updateEntity(ticket);
        // ESCALATE 审计 content 承载级别/次数/目标（plan D5；R1.68 质量路径须用独立 actionType 区分；
        // 目标空时无尾随空格——CSV 快照往返对尾随空白敏感）
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_ESCALATE, ticket.getStatus(), ticket.getStatus(),
                "SLA 超时升级 L" + nextLevel + "（第 " + nextCount + " 次）"
                        + (target == null || target.isEmpty() ? "通知" : "通知 " + target), context);
        notifyEscalation(ticket, policy, nextLevel, nextCount, target, context);
        return true;
    }

    /**
     * 升级等待小时数：policy.escalationDelayHours 优先，null 回退 config 默认 2（plan D2）。
     */
    protected int resolveDelayHours(ErpCsSlaPolicy policy) {
        Integer policyHours = policy != null ? policy.getEscalationDelayHours() : null;
        int hours = policyHours != null ? policyHours : ErpCsConfigs.getEscalationL1ToL2Hours();
        return Math.max(0, hours);
    }

    /**
     * L1 通知目标：policy.escalationUserId（BIGINT → stringify，plan D4 类型归一化）优先，
     * 缺失回退 assignedToId（stdDomain=userId VARCHAR(36)）——修正既有漂移（UC-CS-04 ③）。
     */
    protected String resolveL1Target(ErpCsTicket ticket, ErpCsSlaPolicy policy) {
        Long escalation = policy != null ? policy.getEscalationUserId() : null;
        if (escalation != null) {
            return String.valueOf(escalation);
        }
        return ticket.getAssignedToId();
    }

    /**
     * SLA 升级通知派发（config-gated by {@link ErpCsConfigs#isSlaNotifyEnabled}，plan D4）。
     *
     * <p>复用既有 {@code cs.sla-overdue} 模板（7101 已改 USER_LIST {@code ${escalationUserId}} 单键插值）：
     * ctx {@code escalationUserId} 键承载当前级别有效目标（L1=policy 优先/assignedToId 回退；
     * L2=secondEscalationUserId；L3=config 总监），L2/L3 复用同事件经目标键切换路由；
     * 附 escalationLevel/repeatCount 信息键与 secondEscalationUserId/l3UserId 原始键。
     * 模板缺失或 notify 失败时静默降级（不阻断业务）。
     */
    protected void notifyEscalation(ErpCsTicket ticket, ErpCsSlaPolicy policy, int level, int count,
                                     String targetUserId, IServiceContext context) {
        if (!ErpCsConfigs.isSlaNotifyEnabled()) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket.getCustomerId(), context));
            ctx.put("escalationUserId", targetUserId == null ? "" : targetUserId);
            if (policy != null && policy.getSecondEscalationUserId() != null) {
                ctx.put("secondEscalationUserId", String.valueOf(policy.getSecondEscalationUserId()));
            }
            String l3UserId = ErpCsConfigs.getEscalationL3UserId();
            if (!l3UserId.isEmpty()) {
                ctx.put("l3UserId", l3UserId);
            }
            ctx.put("escalationLevel", level);
            ctx.put("repeatCount", count);
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE, ctx, context);
        } catch (Exception e) {
            // 通知派发失败不阻断 SLA 升级主流程（config-gated 降级语义）
            LOG.warn("SLA notify 派发失败（降级，主升级流程继续）：ticketId={}, reason={}",
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

    private IEntityDao<ErpCsTicket> dao() {
        return daoProvider.daoFor(ErpCsTicket.class);
    }
}
