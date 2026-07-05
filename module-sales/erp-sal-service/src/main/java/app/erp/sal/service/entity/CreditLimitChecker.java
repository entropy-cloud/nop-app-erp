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
 * <p>额度计算口径：{@code available = ErpMdPartner.creditLimit − outstanding}，其中
 * {@code outstanding = Σ(totalAmountWithTax × exchangeRate) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED
 * AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED}。本单含税亦按其 {@code exchangeRate} 折算为本位币后比较。
 * {@code creditLimit} 与 outstanding/本单金额均以本位币（functional currency）口径比较，支持多币种订单。
 *
 * <p>客户主数据读取经 {@link IErpMdPartnerBiz}（跨域只读经 I*Biz 管道）；
 * sales 域 outstanding 订单聚合为本域内部只读（{@code daoFor(ErpSalOrder.class)}，对齐 plan S2 C 段保留项）。
 *
 * <p><b>Non-Goals（触发条件见 sales/README.md）</b>：
 * <ul>
 *   <li>AR 未核销余额未纳入 outstanding —— 开票后绕过信用控制的风险已知，待业财一体端到端验证启动时纳入。</li>
 *   <li>SPECIAL_APPROVAL 审批流未实现 —— 需 use-approval 多级审批工作流迁移，属独立 successor。</li>
 * </ul>
 */
public class CreditLimitChecker {

    private static final Logger LOG = LoggerFactory.getLogger(CreditLimitChecker.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    /**
     * @param customerId             客户 ID
     * @param thisOrderAmount        当前审核订单的含税总额（totalAmountWithTax，原币），null 视为 0
     * @param thisOrderExchangeRate  当前审核订单折算本位币的汇率（{@code amountFunctional = amountSource × rate}），null 视为 1
     */
    public void check(Long customerId, BigDecimal thisOrderAmount, BigDecimal thisOrderExchangeRate,
                      IServiceContext context) {
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
        BigDecimal orderAmountFunctional = toFunctional(thisOrderAmount, thisOrderExchangeRate);
        if (available.compareTo(orderAmountFunctional) < 0) {
            String level = resolveLevel();
            if (ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK.equals(level)) {
                throw new NopException(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED)
                        .param(ErpSalErrors.ARG_CUSTOMER_ID, customerId)
                        .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                        .param(ErpSalErrors.ARG_AVAILABLE, available)
                        .param(ErpSalErrors.ARG_ORDER_AMOUNT, orderAmountFunctional);
            }
            LOG.warn("客户 {} 信用额度超限（额度={}, 可用={}, 本单含税(本位币)={}），策略={} 放行审核",
                    customerId, creditLimit, available, orderAmountFunctional, level);
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
            sum = sum.add(toFunctional(o.getTotalAmountWithTax(), o.getExchangeRate()));
        }
        return sum;
    }

    private BigDecimal toFunctional(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        return amount.multiply(rate);
    }
}
