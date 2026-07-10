package app.erp.sal.service.entity;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.auth.IActionAuthChecker;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 客户信用额度校验器。供 {@link app.erp.sal.service.processor.ErpSalOrderProcessor#approve} 在 SUBMITTED→APPROVED 时调用。
 *
 * <p>额度计算口径：{@code available = ErpMdPartner.creditLimit − outstanding}，其中
 * {@code outstanding = Σ(totalAmountWithTax × exchangeRate) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED
 * AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED}
 * <b>＋ Σ openAmountFunctional of ErpFinArApItem where partnerId=该客户 AND direction=RECEIVABLE AND status∈{OPEN,PARTIAL}</b>
 * （AR 未核销余额本位币，经 {@link IErpFinArApItemBiz#findOpenItemsByPartner} 跨域只读查询，config-gated
 * {@code erp-sal.credit-check-include-ar} 默认开启）。本单含税亦按其 {@code exchangeRate} 折算为本位币后比较。
 * {@code creditLimit} 与 outstanding/本单金额均以本位币（functional currency）口径比较，支持多币种订单与外币 AR。
 *
 * <p>三级策略（{@code erp-sal.credit-check-level}）：
 * <ul>
 *   <li>{@code SOFT_WARNING}（默认）：超额度记告警并放行。</li>
 *   <li>{@code HARD_BLOCK}：超额度抛 {@link ErpSalErrors#ERR_CREDIT_LIMIT_EXCEEDED}。</li>
 *   <li>{@code SPECIAL_APPROVAL}：超额度时校验当前用户是否持有专项权限 {@code erp-sal:creditOverLimitApprove}
 *       （经 {@link IServiceContext#getActionAuthChecker()} 命令式 {@link IActionAuthChecker#isPermitted}）；
 *       持有则放行，否则抛 {@link ErpSalErrors#ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED}。</li>
 * </ul>
 *
 * <p>客户主数据读取经 {@link IErpMdPartnerBiz}（跨域只读经 I*Biz 管道）；
 * sales 域 outstanding 订单聚合为本域内部只读（{@code daoFor(ErpSalOrder.class)}，对齐 plan S2 C 段保留项）；
 * AR 辅助账查询经 {@link IErpFinArApItemBiz}（sales→finance 单向 DAG，过账已建立依赖方向）。
 *
 * <p><b>Non-Goals（触发条件见 sales/README.md）</b>：
 * <ul>
 *   <li>多级 {@code .xwf} 审批工作流链（信用分析师→财务经理）：本期 SPECIAL_APPROVAL 经权限门控实现"超额度需更高权限"语义；
 *       多步 {@code .xwf} 定义归 Deferred（触发条件：多级审批链业务需求落地时，承接 use-approval 迁移 Deferred 范式）。</li>
 *   <li>信用冻结（credit hold）实时拦截开票/出库：本期信用控制仅在订单审核环节；开票/出库环节实时冻结归独立 successor。</li>
 *   <li>客户风险评分联动信用额度动态调整：依赖 CRM 客户信用评分体系落地。</li>
 *   <li>跨账套（multi AcctSchema）AR 余额聚合：本期单账套。</li>
 * </ul>
 *
 * <p><b>出库/发票环节信用冻结（{@link #checkCreditHold}）</b>：语义为检查客户<b>当前</b>是否已超额
 * （{@code available < 0}），不把本单金额叠加到 outstanding（订单审核时额度已被占用）。三级策略与订单审核一致：
 * HARD_BLOCK 抛 {@link ErpSalErrors#ERR_CREDIT_HOLD_DELIVERY} / {@link ErpSalErrors#ERR_CREDIT_HOLD_INVOICE}，
 * SOFT_WARNING 放行带告警，SPECIAL_APPROVAL 经权限门控。是否启用由 {@code erp-sal.credit-check-on-delivery}
 * 和 {@code erp-sal.credit-check-on-invoice}（均默认 false，向后兼容）控制，由调用方 Processor 在审核时门控。
 */
public class CreditLimitChecker {

    private static final Logger LOG = LoggerFactory.getLogger(CreditLimitChecker.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpFinArApItemBiz arApItemBiz;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * @param customerId             客户 ID
     * @param thisOrderAmount        当前审核订单的含税总额（totalAmountWithTax，原币），null 视为 0
     * @param thisOrderExchangeRate  当前审核订单折算本位币的汇率（{@code amountFunctional = amountSource × rate}），null 视为 1
     * @param context                服务上下文（提供命令式权限检查入口）
     */
    public void check(Long customerId, BigDecimal thisOrderAmount, BigDecimal thisOrderExchangeRate,
                      IServiceContext context) {
        check(customerId, thisOrderAmount, thisOrderExchangeRate, null, context);
    }

    /**
     * @param customerId             客户 ID
     * @param thisOrderAmount        当前审核订单的含税总额（totalAmountWithTax，原币），null 视为 0
     * @param thisOrderExchangeRate  当前审核订单折算本位币的汇率（{@code amountFunctional = amountSource × rate}），null 视为 1
     * @param orderCode              当前审核订单的单号（用于通知上下文，可为 null）
     * @param context                服务上下文（提供命令式权限检查入口）
     */
    public void check(Long customerId, BigDecimal thisOrderAmount, BigDecimal thisOrderExchangeRate,
                      String orderCode, IServiceContext context) {
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
        BigDecimal outstanding = sumOutstanding(customerId, context);
        BigDecimal available = creditLimit.subtract(outstanding);
        BigDecimal orderAmountFunctional = toFunctional(thisOrderAmount, thisOrderExchangeRate);
        // 订单审核语义：available >= thisOrderAmount（本单金额加到 outstanding 上后是否超额）
        if (available.compareTo(orderAmountFunctional) < 0) {
            enforceOverLimit(partner, orderCode, ErpSalConstants.BILL_TYPE_ORDER, creditLimit, outstanding,
                    available, orderAmountFunctional, context);
        }
    }

    /**
     * 信用冻结检查（credit hold）：检查客户<b>当前</b>信用状况是否已超额（{@code available < 0}）。
     *
     * <p>与 {@link #check} 的区别：本单金额不叠加到 outstanding——订单审核时额度已被占用，出库/发票审核是检查
     * "额度占用后信用是否恶化"，而非新增占用。三级策略（{@code erp-sal.credit-check-level}）与订单审核一致。
     *
     * @param customerId  客户 ID
     * @param billCode    当前审核单据的单号（用于错误消息与通知上下文）
     * @param billType    单据类型（{@link ErpSalConstants#BILL_TYPE_DELIVERY} / {@link ErpSalConstants#BILL_TYPE_INVOICE}）
     * @param context     服务上下文（提供命令式权限检查入口）
     */
    public void checkCreditHold(Long customerId, String billCode, String billType, IServiceContext context) {
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
        BigDecimal outstanding = sumOutstanding(customerId, context);
        BigDecimal available = creditLimit.subtract(outstanding);
        // 信用冻结语义：客户当前已超额（available < 0）
        if (available.signum() < 0) {
            enforceOverLimit(partner, billCode, billType, creditLimit, outstanding, available,
                    BigDecimal.ZERO, context);
        }
    }

    /**
     * 三级策略统一执行（订单审核与信用冻结复用）。
     *
     * <ul>
     *   <li>HARD_BLOCK：按 {@code billType} 抛对应错误码（ORDER→{@link ErpSalErrors#ERR_CREDIT_LIMIT_EXCEEDED}、
     *       DELIVERY→{@link ErpSalErrors#ERR_CREDIT_HOLD_DELIVERY}、INVOICE→{@link ErpSalErrors#ERR_CREDIT_HOLD_INVOICE}）。</li>
     *   <li>SPECIAL_APPROVAL：持专项权限放行，否则抛 {@link ErpSalErrors#ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED}。</li>
     *   <li>SOFT_WARNING：放行并派发超限通知（config-gated）。</li>
     * </ul>
     *
     * @param billAmountFunctional 本单含税本位币金额（信用冻结场景传 {@link BigDecimal#ZERO}）
     */
    private void enforceOverLimit(ErpMdPartner partner, String billCode, String billType,
                                  BigDecimal creditLimit, BigDecimal outstanding, BigDecimal available,
                                  BigDecimal billAmountFunctional, IServiceContext context) {
        String level = resolveLevel();
        if (ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK.equals(level)) {
            throw buildHardBlockException(billType, partner.getId(), billCode, creditLimit, available,
                    billAmountFunctional);
        }
        if (ErpSalConstants.CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL.equals(level)) {
            if (hasSpecialApprovalPermission(context)) {
                LOG.info("客户 {} 信用额度超限（额度={}, 可用={}, 本单含税(本位币)={}，单据类型={}），策略=SPECIAL_APPROVAL 持专项审批权限放行",
                        partner.getId(), creditLimit, available, billAmountFunctional, billType);
                return;
            }
            throw new NopException(ErpSalErrors.ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, partner.getId())
                    .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                    .param(ErpSalErrors.ARG_AVAILABLE, available)
                    .param(ErpSalErrors.ARG_ORDER_AMOUNT, billAmountFunctional);
        }
        LOG.warn("客户 {} 信用额度超限（额度={}, 可用={}, 本单含税(本位币)={}，单据类型={}），策略={} 放行",
                partner.getId(), creditLimit, available, billAmountFunctional, billType, level);
        // SOFT_WARNING 放行后派发通知（config-gated）：提醒销售员跟进客户超限单据
        notifyCreditOverLimit(partner, billCode, billAmountFunctional, creditLimit, outstanding, available,
                context);
    }

    private NopException buildHardBlockException(String billType, Long customerId, String billCode,
                                                 BigDecimal creditLimit, BigDecimal available,
                                                 BigDecimal billAmountFunctional) {
        if (ErpSalConstants.BILL_TYPE_DELIVERY.equals(billType)) {
            return new NopException(ErpSalErrors.ERR_CREDIT_HOLD_DELIVERY)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, customerId)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, billCode)
                    .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                    .param(ErpSalErrors.ARG_AVAILABLE, available);
        }
        if (ErpSalConstants.BILL_TYPE_INVOICE.equals(billType)) {
            return new NopException(ErpSalErrors.ERR_CREDIT_HOLD_INVOICE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, customerId)
                    .param(ErpSalErrors.ARG_INVOICE_CODE, billCode)
                    .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                    .param(ErpSalErrors.ARG_AVAILABLE, available);
        }
        // ORDER（默认）：订单审核信用超限
        return new NopException(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED)
                .param(ErpSalErrors.ARG_CUSTOMER_ID, customerId)
                .param(ErpSalErrors.ARG_CREDIT_LIMIT, creditLimit)
                .param(ErpSalErrors.ARG_AVAILABLE, available)
                .param(ErpSalErrors.ARG_ORDER_AMOUNT, billAmountFunctional);
    }

    /**
     * 派发信用额度超限通知（config-gated by {@code erp-sal.credit-notify-enabled}）。
     *
     * <p>仅在 SOFT_WARNING 路径触发（HARD_BLOCK 抛错拒绝无需提醒；SPECIAL_APPROVAL 已通过权限门控反馈）。
     * 接收人由模板 ROLE resolver 解析（销售员）；上下文含 customerId/customerName/orderNo/orderAmount/
     * creditLimit/overAmount。模板缺失或 notify 失败时静默降级（不阻断业务）。
     */
    private void notifyCreditOverLimit(ErpMdPartner partner, String orderCode, BigDecimal orderAmountFunctional,
                                       BigDecimal creditLimit, BigDecimal outstanding, BigDecimal available,
                                       IServiceContext context) {
        if (!isCreditNotifyEnabled() || notificationBiz == null) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("customerId", partner.getId());
            ctx.put("customerName", partner.getName());
            ctx.put("orderNo", orderCode);
            ctx.put("orderAmount", orderAmountFunctional);
            ctx.put("creditLimit", creditLimit);
            ctx.put("overAmount", orderAmountFunctional.subtract(available));
            notificationBiz.notify(ErpSalConstants.NOTIFY_EVENT_CREDIT_OVER_LIMIT, ctx, context);
        } catch (Exception e) {
            // 通知派发失败不阻断 SOFT_WARNING 放行（config-gated 降级语义）
            LOG.warn("信用超限 notify 派发失败（降级，主放行流程继续）：customerId={}, reason={}",
                    partner.getId(), e.getMessage());
        }
    }

    private boolean isCreditNotifyEnabled() {
        return AppConfig.var(ErpSalConstants.CONFIG_CREDIT_NOTIFY_ENABLED, true);
    }

    private boolean hasSpecialApprovalPermission(IServiceContext context) {
        if (context == null) {
            return false;
        }
        IActionAuthChecker checker = context.getActionAuthChecker();
        return checker != null
                && checker.isPermitted(ErpSalConstants.PERM_CREDIT_OVER_LIMIT_APPROVE, context);
    }

    private String resolveLevel() {
        String level = AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL,
                ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        return level == null ? ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING : level;
    }

    private BigDecimal sumOutstanding(Long customerId, IServiceContext context) {
        BigDecimal sum = sumOutstandingOrders(customerId);
        if (includeAr()) {
            sum = sum.add(sumArOpenFunctional(customerId, context));
        }
        return sum;
    }

    // sales 域内部只读聚合（plan S2 C 段保留项），不走跨域 I*Biz。
    private BigDecimal sumOutstandingOrders(Long customerId) {
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

    // AR 辅助账未核销余额（本位币）跨域只读聚合，经 IErpFinArApItemBiz 管道（sales→finance 单向 DAG）。
    private BigDecimal sumArOpenFunctional(Long customerId, IServiceContext context) {
        List<ErpFinArApItem> items = arApItemBiz.findOpenItemsByPartner(
                customerId, ErpFinConstants.DIRECTION_RECEIVABLE, context);
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            sum = sum.add(resolveOpenFunctional(it));
        }
        return sum;
    }

    // 优先 openAmountFunctional（未核销本位币余额）；缺失时按 config-gated 容错回退 openAmountSource × exchangeRate 近似折算。
    private BigDecimal resolveOpenFunctional(ErpFinArApItem item) {
        BigDecimal openFunctional = item.getOpenAmountFunctional();
        if (openFunctional != null) {
            return openFunctional;
        }
        if (arFallbackEnabled()) {
            BigDecimal openSource = item.getOpenAmountSource();
            if (openSource != null) {
                return openSource.multiply(toRate(item.getExchangeRate()));
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean includeAr() {
        return AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_INCLUDE_AR,
                ErpSalConstants.CREDIT_CHECK_INCLUDE_AR_DEFAULT);
    }

    private boolean arFallbackEnabled() {
        return AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_AR_FALLBACK,
                ErpSalConstants.CREDIT_CHECK_AR_FALLBACK_DEFAULT);
    }

    private BigDecimal toFunctional(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(toRate(exchangeRate));
    }

    private BigDecimal toRate(BigDecimal exchangeRate) {
        return exchangeRate == null ? BigDecimal.ONE : exchangeRate;
    }
}
