
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsEntitlement;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

public interface IErpCsEntitlementBiz extends ICrudBiz<ErpCsEntitlement> {

    /**
     * 扣减权益余量（按次计费 PAY_PER_TICKET 场景）。{@code usedTickets+1}，超限抛
     * {@code ERR_ENTITLEMENT_EXHAUSTED}；WARRANTY/SUPPORT_CONTRACT 不限余量时仅记日志不增计。
     */
    @BizMutation
    ErpCsEntitlement consumeEntitlement(@Name("entitlementId") Long entitlementId,
                                        IServiceContext context);

    /**
     * 退回权益余量（工单 CLOSED 退款回退）。{@code usedTickets=max(0, usedTickets-1)}，不低于 0。
     */
    @BizMutation
    ErpCsEntitlement releaseEntitlement(@Name("entitlementId") Long entitlementId,
                                        IServiceContext context);

    /**
     * 扫描临近到期权益（窗口默认由 {@code erp-cs.entitlement-expiry-warning-days} 决定）。
     * 供 nop-job 每日扫描派发到期提醒通知。
     */
    @BizQuery
    List<ErpCsEntitlement> scanExpiringEntitlements(@Optional @Name("warningDays") Integer warningDays,
                                                     IServiceContext context);

    /**
     * 扫描已到期但仍 active 的权益，自动停用（{@code isActive=false}）。供 nop-job 到期日批量执行。
     */
    @BizMutation
    List<ErpCsEntitlement> deactivateExpiredEntitlements(IServiceContext context);

    /**
     * 按客户聚合权益使用率（对齐 entitlement.md §四报表）。
     *
     * @return 每客户一条聚合：partnerId, totalEntitlements, totalUsed, totalMax, usageRate
     */
    @BizQuery
    List<Map<String, Object>> getEntitlementUsage(@Name("partnerId") Long partnerId,
                                                   IServiceContext context);

    /**
     * 公开匹配入口：工单建单时调用，返回最优先的有效权益（或 null）。
     * config-gated 由调用方（ErpCsTicketBizModel）控制是否调用本方法。
     * 此方法为内部业务调用（非 GraphQL 暴露），不加 @BizQuery/@BizMutation。
     */
    ErpCsEntitlement matchForCustomer(Long customerIdAsPartnerId);
}
