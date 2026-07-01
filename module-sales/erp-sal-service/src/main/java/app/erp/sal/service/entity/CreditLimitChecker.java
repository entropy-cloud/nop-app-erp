package app.erp.sal.service.entity;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 客户信用额度校验器。供 {@link ErpSalOrderBizModel#approve} 在 SUBMITTED→APPROVED 时调用。
 *
 * <p>权威：{@code docs/design/sales/README.md} §信用额度控制 + {@code docs/design/flow-overview.md} §2.2。
 *
 * <p>额度计算口径（MVP）：{@code available = ErpMdPartner.creditLimit − outstanding}，其中
 * {@code outstanding = Σ(totalAmountWithTax) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED
 * AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED}。若 {@code available < 本单 totalAmountWithTax} 判定超额度。
 *
 * <p>「未结算应收余额（AR_INVOICE 未核销）」分量当前为 0（销售发票未实现），MVP 不计入；Follow-up 在 AR_INVOICE 落地后补入。
 *
 * <p>三级策略（{@code erp-sal.credit-check-level}，默认 {@code SOFT_WARNING}）：
 * <ul>
 *   <li>{@code SOFT_WARNING}：超额度记录告警日志但放行审核。</li>
 *   <li>{@code HARD_BLOCK}：超额度抛 {@link NopException}({@link ErpSalErrors#ERR_CREDIT_LIMIT_EXCEEDED})。</li>
 *   <li>{@code SPECIAL_APPROVAL}：MVP 降级为 SOFT_WARNING 行为（nop-wf 未接线，无法路由额外审批人），记为 Follow-up。</li>
 * </ul>
 *
 * <p>{@code creditLimit} 为 null（未设置）视为不控制（放行）。调用时传入的「本单」尚未置 APPROVED，故不在 outstanding 内（不会被重复计算）。
 */
public class CreditLimitChecker {

    private static final Logger LOG = LoggerFactory.getLogger(CreditLimitChecker.class);

    @Inject
    IDaoProvider daoProvider;

    /**
     * @param customerId       客户 ID
     * @param thisOrderAmount  当前审核订单的含税总额（totalAmountWithTax），null 视为 0
     */
    public void check(Long customerId, BigDecimal thisOrderAmount) {
        if (customerId == null) {
            return;
        }
        ErpMdPartner partner = daoProvider.daoFor(ErpMdPartner.class).getEntityById(customerId);
        if (partner == null) {
            return;
        }
        BigDecimal creditLimit = partner.getCreditLimit();
        if (creditLimit == null) {
            return;
        }
        BigDecimal outstanding = sumOutstanding(customerId);
        BigDecimal available = creditLimit.subtract(outstanding);
        BigDecimal orderAmount = thisOrderAmount == null ? BigDecimal.ZERO : thisOrderAmount;
        if (available.compareTo(orderAmount) < 0) {
            String level = resolveLevel();
            if (ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK.equals(level)) {
                throw new NopException(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED)
                        .param(ErpSalErrors.ARG_CUSTOMER_ID, customerId)
                        .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                        .param(ErpSalErrors.ARG_AVAILABLE, available)
                        .param(ErpSalErrors.ARG_ORDER_AMOUNT, orderAmount);
            }
            LOG.warn("客户 {} 信用额度超限（额度={}, 可用={}, 本单含税={}），策略={} 放行审核",
                    customerId, creditLimit, available, orderAmount, level);
        }
    }

    private String resolveLevel() {
        String level = AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL,
                ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        return level == null ? ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING : level;
    }

    private BigDecimal sumOutstanding(Long customerId) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("customerId", customerId),
                eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED),
                ne("deliveryStatus", ErpSalConstants.DELIVERY_STATUS_DELIVERED),
                ne("docStatus", ErpSalConstants.DOC_STATUS_CANCELLED)
        ));
        List<ErpSalOrder> orders = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpSalOrder o : orders) {
            BigDecimal amt = o.getTotalAmountWithTax();
            if (amt != null) {
                sum = sum.add(amt);
            }
        }
        return sum;
    }
}
