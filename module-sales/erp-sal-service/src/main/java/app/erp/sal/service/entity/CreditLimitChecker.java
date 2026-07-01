package app.erp.sal.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
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
 * <p>额度计算口径（MVP）：{@code available = ErpMdPartner.creditLimit − outstanding}，其中
 * {@code outstanding = Σ(totalAmountWithTax) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED
 * AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED}。若 {@code available < 本单 totalAmountWithTax} 判定超额度。
 *
 * <p>客户主数据读取经 {@link IErpMdPartnerBiz}（跨域只读经 I*Biz 管道）；
 * sales 域 outstanding 订单聚合为本域内部只读（{@code daoFor(ErpSalOrder.class)}，对齐 plan S2 C 段保留项）。
 */
public class CreditLimitChecker {

    private static final Logger LOG = LoggerFactory.getLogger(CreditLimitChecker.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    /**
     * @param customerId       客户 ID
     * @param thisOrderAmount  当前审核订单的含税总额（totalAmountWithTax），null 视为 0
     */
    public void check(Long customerId, BigDecimal thisOrderAmount, IServiceContext context) {
        if (customerId == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(customerId, context);
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
        // sales 域内部只读聚合（plan S2 C 段保留项），不走跨域 I*Biz。
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
