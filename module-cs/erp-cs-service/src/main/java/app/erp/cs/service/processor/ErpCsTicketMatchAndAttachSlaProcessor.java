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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpCsTicket）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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

        // P1-CK-cs-001 修正挂载序（plan 2026-09-11-2350-1 Phase 5）：
        // 生效策略单一化——权益 slaPolicyId 命中时直接按该策略计算 deadline（来源一致），
        // 无权益策略时回退 matcher 匹配结果。
        ErpCsSlaPolicy effectivePolicy = null;
        if (matched != null && matched.getSlaPolicyId() != null) {
            ticket.setSlaPolicyId(matched.getSlaPolicyId());
            effectivePolicy = loadPolicy(matched.getSlaPolicyId());
        } else {
            ErpCsSlaPolicy policy = SlaPolicyMatcher.match(daoProvider, ticket);
            if (policy != null) {
                ticket.setSlaPolicyId(policy.getId());
                effectivePolicy = policy;
            }
        }
        // deadline 按生效策略计算；calculate 返回 null（策略无 hours/days 配置）时跳过写入
        // 保持 null，不再 Timestamp.valueOf(null) 裸 NPE
        if (effectivePolicy != null) {
            LocalDateTime deadline = SlaDeadlineCalculator.calculate(CoreMetrics.currentDateTime(), effectivePolicy);
            if (deadline != null) {
                ticket.setDeadlineDateTime(Timestamp.valueOf(deadline));
            }
        }
        // 权益 maxResolutionTime 覆盖最终权益（恒胜出，entitlement.md §三 优先级 1）——
        // 置于一切策略计算之后，不再被策略 deadline 覆写
        if (matched != null) {
            applyEntitlementSlaOverride(ticket, matched);
        }
        dao().updateEntity(ticket);
        return ticket;
    }

    private ErpCsSlaPolicy loadPolicy(String policyId) {
        return daoProvider.daoFor(ErpCsSlaPolicy.class).getEntityById(policyId);
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
        // P1-CK-cs-003：目录建单工单的权益扣减已在 createFromCatalog 落地（catalogItemId 列标记），
        // enrichAfterCreate 自动挂载路径不得二次扣减（修复前纯计次权益 usedTickets 双计）
        if (ticket.getCatalogItemId() != null) {
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
                ticket.setRemark("no active service entitlement");
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
