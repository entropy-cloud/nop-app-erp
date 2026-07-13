package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsEntitlementBiz;
import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateTimeBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.Collections;

/**
 * 服务权益 BizModel（{@code docs/design/customer-service/entitlement.md}）。
 *
 * <p>权益生命周期：
 * <ul>
 *   <li>{@link #consumeEntitlement} —— PAY_PER_TICKET 按次扣减（{@code usedTickets+1}），超限抛
 *       {@link ErpCsErrors#ERR_ENTITLEMENT_EXHAUSTED}；WARRANTY/SUPPORT_CONTRACT 不限余量时仅记日志不增计。</li>
 *   <li>{@link #releaseEntitlement} —— 工单 CLOSED/取消退款回退（{@code usedTickets=max(0,usedTickets-1)}）。</li>
 *   <li>{@link #scanExpiringEntitlements} —— 窗口查询到期前权益供 nop-job 派发提醒。</li>
 *   <li>{@link #deactivateExpiredEntitlements} —— 自动停用 endDate&lt;now 的 active 权益。</li>
 *   <li>{@link #getEntitlementUsage} —— 按客户聚合使用率（对齐 entitlement.md §四报表）。</li>
 * </ul>
 *
 * <p>权益匹配经 {@link EntitlementMatcher}（纯函数式，加载函数注入便于单测）。
 */
@BizModel("ErpCsEntitlement")
public class ErpCsEntitlementBizModel extends CrudBizModel<ErpCsEntitlement> implements IErpCsEntitlementBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsEntitlementBizModel.class);

    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    public ErpCsEntitlementBizModel() {
        setEntityName(ErpCsEntitlement.class.getName());
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setMdPartnerBiz(IErpMdPartnerBiz mdPartnerBiz) {
        this.mdPartnerBiz = mdPartnerBiz;
    }

    // ============ 余量扣减/回退（entitlement.md §2.3） ============

    @Override
    @BizMutation
    public ErpCsEntitlement consumeEntitlement(@Name("entitlementId") Long entitlementId,
                                                IServiceContext context) {
        ErpCsEntitlement entitlement = requireEntitlement(entitlementId, context);
        validateConsumable(entitlement);
        if (ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET.equals(entitlement.getServiceType())) {
            Integer max = entitlement.getMaxTickets();
            Integer used = entitlement.getUsedTickets() == null ? 0 : entitlement.getUsedTickets();
            if (max != null && used >= max) {
                throw new NopException(ErpCsErrors.ERR_ENTITLEMENT_EXHAUSTED)
                        .param(ErpCsErrors.ARG_ENTITLEMENT_CODE, entitlement.getCode())
                        .param(ErpCsErrors.ARG_USED_TICKETS, used)
                        .param(ErpCsErrors.ARG_MAX_TICKETS, max);
            }
            entitlement.setUsedTickets(used + 1);
            updateEntity(entitlement, null, context);
        } else {
            // WARRANTY / SUPPORT_CONTRACT：不限余量，仅记录扣减日志（usedTickets 不增）
            LOG.debug("entitlement-consume-non-quota: type={}, id={}, maxTickets={}",
                    entitlement.getServiceType(), entitlement.getId(), entitlement.getMaxTickets());
        }
        return entitlement;
    }

    @Override
    @BizMutation
    public ErpCsEntitlement releaseEntitlement(@Name("entitlementId") Long entitlementId,
                                                IServiceContext context) {
        ErpCsEntitlement entitlement = requireEntitlement(entitlementId, context);
        if (!ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET.equals(entitlement.getServiceType())) {
            return entitlement;
        }
        Integer used = entitlement.getUsedTickets();
        if (used == null || used <= 0) {
            // 已为 0，幂等返回，不降至负
            return entitlement;
        }
        entitlement.setUsedTickets(used - 1);
        updateEntity(entitlement, null, context);
        return entitlement;
    }

    // ============ 到期扫描（entitlement.md §2.2） ============

    @Override
    @BizQuery
    public List<ErpCsEntitlement> scanExpiringEntitlements(@Optional @Name("warningDays") Integer warningDays,
                                                            IServiceContext context) {
        int window = warningDays != null ? warningDays : ErpCsConfigs.getEntitlementExpiryWarningDays();
        LocalDate now = CoreMetrics.currentDateTime().toLocalDate();
        LocalDate windowEnd = now.plusDays(window);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        // endDate 为 DATE 列，XMeta 仅允许 dateBetween/dateTimeBetween（不支持 ge/le）
        q.addFilter(io.nop.api.core.beans.FilterBeans.dateBetween("endDate", now, windowEnd));
        return findList(q, null, context);
    }

    @Override
    @BizMutation
    public List<ErpCsEntitlement> deactivateExpiredEntitlements(IServiceContext context) {
        LocalDate now = CoreMetrics.currentDateTime().toLocalDate();
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        // endDate < now（已过期）；endDate 为 null（无固定到期日）永不自动停用
        // endDate 为 DATE 列；用 lt + LocalDate（不用 isNotNull，因 null endDate 永不过期）
        q.addFilter(io.nop.api.core.beans.FilterBeans.lt("endDate", now));
        // 经 doFindListByQueryDirectly 绕过 endDate 字段 meta 的 lt 限制（同 ErpCsTicket.scanOverdueTickets 模式）
        List<ErpCsEntitlement> expired = doFindListByQueryDirectly(q, context);
        List<ErpCsEntitlement> deactivated = new ArrayList<>();
        for (ErpCsEntitlement e : expired) {
            try {
                e.setIsActive(Boolean.FALSE);
                updateEntity(e, null, context);
                deactivated.add(e);
            } catch (Exception ex) {
                // 单条失败隔离，不阻断批量停用
                LOG.warn("entitlement-deactivate-failed: id={}, reason={}", e.getId(), ex.getMessage());
            }
        }
        return deactivated;
    }

    // ============ 使用率聚合（entitlement.md §四） ============

    @Override
    @BizQuery
    public List<Map<String, Object>> getEntitlementUsage(@Name("partnerId") Long partnerId,
                                                          IServiceContext context) {
        QueryBean q = new QueryBean();
        if (partnerId != null) {
            q.addFilter(eq("partnerId", partnerId));
        } else {
            // 无 partnerId 时返回空（聚合需指定客户）
            return new ArrayList<>();
        }
        List<ErpCsEntitlement> all = findList(q, null, context);
        // 单客户聚合为一条
        long totalEntitlements = all.size();
        long totalUsed = all.stream()
                .mapToLong(e -> e.getUsedTickets() == null ? 0 : e.getUsedTickets())
                .sum();
        long totalMax = all.stream()
                .filter(e -> e.getMaxTickets() != null)
                .mapToLong(ErpCsEntitlement::getMaxTickets)
                .sum();
        double usageRate = totalMax == 0 ? 0.0 : (double) totalUsed / totalMax * 100;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("partnerId", partnerId);
        row.put("partnerName", resolvePartnerName(partnerId, context));
        row.put("totalEntitlements", totalEntitlements);
        row.put("totalUsed", totalUsed);
        row.put("totalMax", totalMax);
        row.put("usageRate", Math.round(usageRate * 100) / 100.0);
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(row);
        return result;
    }

    // ============ helpers ============

    /**
     * 加载某客户全部 active 权益（供 EntitlementMatcher 注入）。protected 以允许测试覆盖。
     */
    protected List<ErpCsEntitlement> loadActiveByPartner(Long partnerId) {
        if (partnerId == null) {
            return new ArrayList<>();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("isActive", Boolean.TRUE));
        IEntityDao<ErpCsEntitlement> dao = daoProvider().daoFor(ErpCsEntitlement.class);
        return dao.findAllByQuery(q);
    }

    /**
     * 公开匹配入口：工单建单时调用，返回最优先的有效权益（或 null）。
     * config-gated 由调用方（ErpCsTicketBizModel）控制是否调用本方法。
     */
    @Override
    public ErpCsEntitlement matchForCustomer(Long customerIdAsPartnerId) {
        LocalDate now = CoreMetrics.currentDateTime().toLocalDate();
        return EntitlementMatcher.match(customerIdAsPartnerId, now, this::loadActiveByPartner);
    }

    private ErpCsEntitlement requireEntitlement(Long entitlementId, IServiceContext context) {
        if (entitlementId == null) {
            throw new NopException(ErpCsErrors.ERR_ENTITLEMENT_NOT_FOUND)
                    .param(ErpCsErrors.ARG_ENTITLEMENT_ID, entitlementId);
        }
        return requireEntity(String.valueOf(entitlementId), null, context);
    }

    private void validateConsumable(ErpCsEntitlement entitlement) {
        Boolean active = entitlement.getIsActive();
        LocalDate now = CoreMetrics.currentDateTime().toLocalDate();
        LocalDate end = entitlement.getEndDate();
        boolean expired = end != null && now.isAfter(end);
        if (active == null || !active || expired) {
            throw new NopException(ErpCsErrors.ERR_ENTITLEMENT_EXPIRED)
                    .param(ErpCsErrors.ARG_ENTITLEMENT_CODE, entitlement.getCode());
        }
    }

    private String resolvePartnerName(Long partnerId, IServiceContext context) {
        if (partnerId == null || mdPartnerBiz == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(partnerId, context);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsEntitlement.class)
    public List<String> orgName(@ContextSource List<ErpCsEntitlement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsEntitlement row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsEntitlement.class)
    public List<String> partnerName(@ContextSource List<ErpCsEntitlement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsEntitlement row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsEntitlement.class)
    public List<String> contractName(@ContextSource List<ErpCsEntitlement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("contract"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsEntitlement row : rows) {
            result.add(row.orm_attached() && row.getContract() != null ? row.getContract().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsEntitlement.class)
    public List<String> slaPolicyName(@ContextSource List<ErpCsEntitlement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("slaPolicy"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsEntitlement row : rows) {
            result.add(row.orm_attached() && row.getSlaPolicy() != null ? row.getSlaPolicy().getName() : null);
        }
        return result;
    }

}
