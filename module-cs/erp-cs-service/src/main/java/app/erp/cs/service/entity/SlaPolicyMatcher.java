package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTicket;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * SLA 策略匹配器。权威：{@code docs/design/customer-service/sla.md §1.2}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 1。
 *
 * <p>匹配条件（无 ORM isActive 列，不做 active 过滤——见 plan Decision）：
 * <ul>
 *   <li>{@code policy.ticketTypeId} 为空（通用兜底）或等于 {@code ticket.ticketTypeId}</li>
 *   <li>{@code policy.minPriority} 为空（不限优先级）或 {@code rank(policy.minPriority) <= rank(ticket.priority)}</li>
 *   <li>{@code policy.teamId} 为空——工单 ORM 无 teamId 列，无法做团队维度匹配，仅匹配无团队约束的策略</li>
 * </ul>
 *
 * <p>排序按精确度（约束维度越多越优先）：type+priority > type > 通用兜底。team 维度因工单无 teamId 而不参与。
 * 取首条匹配策略；无匹配返回 null（工单不挂策略，deadlineDateTime 留空）。
 */
public final class SlaPolicyMatcher {

    private SlaPolicyMatcher() {
    }

    /** 返回最匹配的 SLA 策略；无匹配返回 null。 */
    public static ErpCsSlaPolicy match(IDaoProvider daoProvider, ErpCsTicket ticket) {
        IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
        QueryBean q = new QueryBean();
        // teamId 必须为空（工单无 teamId，无法匹配团队策略）
        q.addFilter(isNull("teamId"));
        // ticketTypeId：等于工单类型 或 为空（通用兜底）
        if (ticket.getTicketTypeId() != null) {
            q.addFilter(or(
                    eq("ticketTypeId", ticket.getTicketTypeId()),
                    isNull("ticketTypeId")));
        } else {
            q.addFilter(isNull("ticketTypeId"));
        }
        List<ErpCsSlaPolicy> candidates = dao.findAllByQuery(q);
        if (candidates.isEmpty()) {
            return null;
        }
        int ticketPriorityRank = TicketPriorityRank.rank(ticket.getPriority());
        return candidates.stream()
                .filter(p -> p.getMinPriority() == null
                        || TicketPriorityRank.rank(p.getMinPriority()) <= ticketPriorityRank)
                .max(Comparator.comparingInt(SlaPolicyMatcher::precisionScore))
                .orElse(null);
    }

    /**
     * 精确度评分：约束维度越多分越高。
     * teamId 维度因工单无 teamId 始终为空不计分。
     */
    private static int precisionScore(ErpCsSlaPolicy p) {
        int score = 0;
        if (p.getTicketTypeId() != null) {
            score += 2;
        }
        if (p.getMinPriority() != null) {
            score += 1;
        }
        return score;
    }
}
