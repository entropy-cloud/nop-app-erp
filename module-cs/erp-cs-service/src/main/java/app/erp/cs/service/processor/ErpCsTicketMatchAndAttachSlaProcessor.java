package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsEntitlementBiz;
import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.EntitlementMatcher;
import app.erp.cs.service.entity.SlaDeadlineCalculator;
import app.erp.cs.service.entity.SlaPolicyMatcher;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * ErpCsTicket matchAndAttachSla per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SLA 策略匹配 + deadline 计算 + 权益集成编排。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketMatchAndAttachSlaProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsEntitlementBiz entitlementBiz;

    public ErpCsTicket matchAndAttachSla(String ticketId, IServiceContext context) {
        if (!ErpCsConfigs.isSlaEnabled()) {
            return requireTicket(ticketId, context);
        }
        ErpCsTicket ticket = requireTicket(ticketId, context);

        // 权益集成（plan 2026-07-07-1430-1 §Phase 1 Decision）：config-gated
        // 与 SLA 匹配同点装配（"为工单装配服务级别"语义一致，避免双触发）。
        ErpCsEntitlement matched = matchAndConsumeEntitlement(ticket, context);
        if (matched != null) {
            // 权益级 SLA 覆盖优先：entitlement.slaPolicyId 覆盖工单类型默认
            if (matched.getSlaPolicyId() != null) {
                ticket.setSlaPolicyId(matched.getSlaPolicyId());
            }
            applyEntitlementSlaOverride(ticket, matched);
            // 权益无独立 deadlineDateTime 列，沿用 SLA 策略 deadline 计算路径
        }

        ErpCsSlaPolicy policy = SlaPolicyMatcher.match(daoProvider, ticket);
        if (policy == null) {
            // 无匹配策略：不挂策略，deadlineDateTime 留空
            dao().updateEntity(ticket);
            return ticket;
        }
        // 权益未覆盖 slaPolicyId 时取 SLA 策略匹配结果
        if (ticket.getSlaPolicyId() == null) {
            ticket.setSlaPolicyId(policy.getId());
        }
        LocalDateTime deadline = SlaDeadlineCalculator.calculate(CoreMetrics.currentDateTime(), policy);
        ticket.setDeadlineDateTime(Timestamp.valueOf(deadline));
        // priority 变更重算时保留原 startDateTime（plan Phase 1 item 3）
        dao().updateEntity(ticket);
        return ticket;
    }

    /**
     * 权益匹配 + 扣减（config-gated by {@link ErpCsConfigs#isEntitlementCheckEnabled}）。
     *
     * <p>决策（plan §Phase 1 Decision）：与 SLA 匹配同事务、工单生命周期内单一触发点（matchAndAttachSla 入口），
     * 避免在状态迁移时重复扣减。匹配返回权益时调 {@link IErpCsEntitlementBiz#consumeEntitlement}；
     * 无匹配时按 {@link ErpCsConfigs#isAllowNoEntitlement} 放行或抛 {@link ErpCsErrors#ERR_ENTITLEMENT_NONE_ACTIVE}。
     */
    private ErpCsEntitlement matchAndConsumeEntitlement(ErpCsTicket ticket, IServiceContext context) {
        if (!ErpCsConfigs.isEntitlementCheckEnabled() || entitlementBiz == null) {
            return null;
        }
        ErpCsEntitlement matched = entitlementBiz.matchForCustomer(ticket.getCustomerId());
        if (matched == null) {
            if (!ErpCsConfigs.isAllowNoEntitlement()) {
                throw new NopException(ErpCsErrors.ERR_ENTITLEMENT_NONE_ACTIVE)
                        .param(ErpCsErrors.ARG_PARTNER_ID, ticket.getCustomerId());
            }
            // 放行：标记"无服务权益"（写 remark 仅在为空时，避免覆盖既有备注）
            if (ticket.getRemark() == null || ticket.getRemark().isEmpty()) {
                ticket.setRemark("无有效服务权益");
            }
            return null;
        }
        // 扣减（PAY_PER_TICKET 增计，其他类型仅记日志）
        entitlementBiz.consumeEntitlement(matched.getId(), context);
        return matched;
    }

    /**
     * 应用权益级 SLA 覆盖：maxResolutionTime/maxResponseTime 不为空时覆盖策略默认值。
     * 因 deadlineDateTime 沿用 SLA 策略计算路径，此处仅记录权益覆盖（如需精确覆盖可扩展 SlaDeadlineCalculator）。
     */
    private void applyEntitlementSlaOverride(ErpCsTicket ticket, ErpCsEntitlement entitlement) {
        Integer overrideMinutes = EntitlementMatcher.resolveSlaOverrideMinutes(entitlement);
        if (overrideMinutes != null && overrideMinutes > 0) {
            LocalDateTime base = CoreMetrics.currentDateTime();
            LocalDateTime overrideDeadline = base.plusMinutes(overrideMinutes);
            // 权益级覆盖优先于 SLA 策略计算的 deadline（entitlement.md §三 优先级 1）
            ticket.setDeadlineDateTime(Timestamp.valueOf(overrideDeadline));
        }
    }

    private ErpCsTicket requireTicket(String ticketId, IServiceContext context) {
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        ErpCsTicket ticket = dao().getEntityById(ticketId);
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return ticket;
    }

    private IEntityDao<ErpCsTicket> dao() {
        return daoProvider.daoFor(ErpCsTicket.class);
    }
}
