package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsEntitlement;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 服务权益匹配器。权威：{@code docs/design/customer-service/entitlement.md §2.1/§三}、
 * {@code docs/plans/2026-07-07-1430-1-cs-entitlement-service-catalog.md} Phase 1。
 *
 * <p>纯函数式实现（无 ORM 依赖），加载函数经构造参数注入便于单测 mock：
 * <ul>
 *   <li>{@link #match(Long, LocalDate, Function)} 以工单 {@code customerId}（作为权益 partnerId）
 *       + 当前日期 + 加载函数过滤候选权益。</li>
 *   <li>过滤条件：{@code partnerId} 相等 + 期间有效（{@code startDate≤now≤endDate}）+ {@code isActive=true}
 *       + 余量（{@code maxTickets IS NULL OR usedTickets<maxTickets}）。</li>
 *   <li>排序取 {@code endDate} 最近者（即将到期者优先消费，符合 entitlement.md §2.1）。</li>
 * </ul>
 *
 * <p>{@link #resolveSlaOverrideMinutes(ErpCsEntitlement)} 返回权益级 SLA 覆盖（不为空时覆盖策略）。
 */
public final class EntitlementMatcher {

    private EntitlementMatcher() {
    }

    /**
     * 匹配最优先的有效权益。返回 null 表示无匹配。
     *
     * @param customerIdAsPartnerId 工单 customerId（作为权益 partnerId 查询）
     * @param now                   当前日期（用于期间有效判断；精确到日）
     * @param loadActiveByPartner   加载函数：传入 partnerId，返回该客户全部 active 权益（由调用方实现 ORM 查询）
     */
    public static ErpCsEntitlement match(Long customerIdAsPartnerId,
                                         LocalDate now,
                                         Function<Long, List<ErpCsEntitlement>> loadActiveByPartner) {
        if (customerIdAsPartnerId == null || loadActiveByPartner == null) {
            return null;
        }
        List<ErpCsEntitlement> candidates = loadActiveByPartner.apply(customerIdAsPartnerId);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(e -> isPartnerMatched(e, customerIdAsPartnerId))
                .filter(e -> isPeriodValid(e, now))
                .filter(EntitlementMatcher::isActive)
                .filter(EntitlementMatcher::hasRemainingQuota)
                .min(Comparator.comparing(ErpCsEntitlement::getEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /** 权益级 SLA 覆盖：maxResolutionTime 不为空时返回，否则 null（沿用 slaPolicyId 策略）。 */
    public static Integer resolveSlaOverrideMinutes(ErpCsEntitlement entitlement) {
        if (entitlement == null) {
            return null;
        }
        return entitlement.getMaxResolutionTime();
    }

    /** 权益级响应时限覆盖（maxResponseTime），用于覆盖策略 responseHours。 */
    public static Integer resolveResponseOverrideMinutes(ErpCsEntitlement entitlement) {
        if (entitlement == null) {
            return null;
        }
        return entitlement.getMaxResponseTime();
    }

    private static boolean isPartnerMatched(ErpCsEntitlement e, Long partnerId) {
        return partnerId.equals(e.getPartnerId());
    }

    private static boolean isPeriodValid(ErpCsEntitlement e, LocalDate now) {
        LocalDate start = e.getStartDate();
        LocalDate end = e.getEndDate();
        if (now == null) {
            return false;
        }
        boolean afterStart = start == null || !now.isBefore(start);
        boolean beforeEnd = end == null || !now.isAfter(end);
        return afterStart && beforeEnd;
    }

    private static boolean isActive(ErpCsEntitlement e) {
        Boolean active = e.getIsActive();
        return active != null && active;
    }

    private static boolean hasRemainingQuota(ErpCsEntitlement e) {
        Integer max = e.getMaxTickets();
        if (max == null) {
            return true;
        }
        Integer used = e.getUsedTickets();
        return used == null || used < max;
    }
}
