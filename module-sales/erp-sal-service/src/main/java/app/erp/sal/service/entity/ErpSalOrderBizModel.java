
package app.erp.sal.service.entity;

import app.erp.md.biz.IErpMdMaterialSkuBiz;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.dto.PriceValidationResult;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.BatchOperationResult;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPricingRule;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.processor.ErpSalOrderApproveProcessor;
import app.erp.sal.service.processor.ErpSalOrderCancelProcessor;
import app.erp.sal.service.support.ErpSalPricingRuleEngine;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售订单 BizModel（聚合根 Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalOrderProcessor#onSubmit}/{@link ErpSalOrderProcessor#onApproved}；
 * 跨聚合写契约（报价→订单转化、发货进度回写、防重查询）留 Facade。
 */
@BizModel("ErpSalOrder")
public class ErpSalOrderBizModel extends CrudBizModel<ErpSalOrder> implements IErpSalOrderBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSalOrderBizModel.class);

    @Inject
    QuotationToOrderConverter quotationToOrderConverter;

    @Inject
    ErpSalOrderCancelProcessor cancelProcessor;

    @Inject
    ErpSalOrderApproveProcessor approveProcessor;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpMdMaterialSkuBiz mdMaterialSkuBiz;

    @Inject
    ErpSalPricingRuleEngine pricingRuleEngine;

    public ErpSalOrderBizModel() {
        setEntityName(ErpSalOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(orderId), context);
    }

    /**
     * F11 批量审批（plan 2026-07-22-0444-2 Phase 1）。逐行调 {@link ErpSalOrderApproveProcessor#approve}；
     * 行级失败（状态非 SUBMITTED / 业务规则不满足）记入 {@link BatchOperationResult#getFailures()}，不阻塞其他行。
     */
    @Override
    @BizMutation
    public BatchOperationResult batchApprove(@Name("ids") Collection<String> ids, IServiceContext context) {
        BatchOperationResult result = BatchOperationResult.forTotal(ids == null ? 0 : ids.size());
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        for (String id : ids) {
            try {
                approveProcessor.approve(id, context);
                result.recordSuccess();
            } catch (NopException e) {
                result.recordFailure(id, e.getErrorCode(), e.getDescription());
            }
        }
        return result;
    }

    // ---------- UC-SAL-11 促销规则应用 ----------

    @Override
    @BizMutation
    public void applyPricingRules(@Name("orderId") String orderId, IServiceContext context) {
        ErpSalOrder order = get(orderId, false, context);
        if (order == null) {
            return;
        }
        List<ErpSalOrderLine> lines = loadOrderLines(order);
        if (lines.isEmpty()) {
            return;
        }
        String customerGroup = resolveCustomerGroup(order.getCustomerId(), context);
        List<ErpSalPricingRule> activeRules = findActiveRules(context);

        ErpSalPricingRuleEngine.EvaluationResult result =
                pricingRuleEngine.evaluate(order, lines, customerGroup, activeRules);

        persistPricingResult(order, result, context);
    }

    /**
     * 通过 ORM to-many 关系 {@code ErpSalOrder.lines} 加载行（懒加载，复用主实体 session）。
     * 取代 {@code daoFor(ErpSalOrderLine.class).findAllByQuery(eq("orderId", ...))}——
     * 关系已在 {@code app-erp-sales.orm.xml} 声明，无需重复查询。
     */
    protected List<ErpSalOrderLine> loadOrderLines(ErpSalOrder order) {
        return new ArrayList<>(order.getLines());
    }

    protected String resolveCustomerGroup(Long partnerId, IServiceContext context) {
        if (partnerId == null) {
            return null;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(partnerId, context);
        return partner == null ? null : partner.getCustomerGroup();
    }

    protected List<ErpSalPricingRule> findActiveRules(IServiceContext context) {
        IEntityDao<ErpSalPricingRule> ruleDao = daoFor(ErpSalPricingRule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        return new ArrayList<>(ruleDao.findAllByQuery(q));
    }

    /**
     * 持久化促销结果：更新订单行折扣 + 追加赠品行 + 重算订单头合计。
     */
    protected void persistPricingResult(ErpSalOrder order,
                                        ErpSalPricingRuleEngine.EvaluationResult result,
                                        IServiceContext context) {
        IEntityDao<ErpSalOrderLine> lineDao = daoFor(ErpSalOrderLine.class);
        int lineNo = nextLineNo(result.getModifiedLines());
        for (ErpSalOrderLine line : result.getModifiedLines()) {
            recomputeLineAmount(line);
            if (line.getId() == null) {
                line.setOrderId(order.getId());
                line.setLineNo(lineNo++);
                lineDao.saveEntity(line);
            } else {
                lineDao.updateEntity(line);
            }
        }
        recomputeOrderTotals(order, result.getModifiedLines(), result.getOrderDiscountAmount());
        validatePromotionPrices(order, result.getModifiedLines(), context);
        updateEntity(order, null, context);
    }

    protected int nextLineNo(List<ErpSalOrderLine> lines) {
        int max = 0;
        for (ErpSalOrderLine line : lines) {
            if (line.getId() != null && line.getLineNo() != null && line.getLineNo() > max) {
                max = line.getLineNo();
            }
        }
        return max + 1;
    }

    protected void recomputeLineAmount(ErpSalOrderLine line) {
        BigDecimal unitPrice = nullSafe(line.getUnitPrice());
        BigDecimal qty = nullSafe(line.getQuantity());
        BigDecimal gross = unitPrice.multiply(qty);
        BigDecimal discountAmt = nullSafe(line.getDiscountAmount());
        BigDecimal net = gross.subtract(discountAmt).max(BigDecimal.ZERO);
        net = net.setScale(4, RoundingMode.HALF_UP);
        line.setAmount(net);
        // 价税分离（L1 公式：税额 = 折扣后金额 / (1 + 税率) × 税率；P1-RC-022）
        BigDecimal taxRate = nullSafe(line.getTaxRate());
        BigDecimal taxAmount;
        if (taxRate.signum() == 0) {
            taxAmount = BigDecimal.ZERO;
        } else {
            BigDecimal rate = taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            taxAmount = net.multiply(rate).divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP);
        }
        line.setTaxAmount(taxAmount);
        line.setAmountWithTax(net.add(taxAmount).setScale(4, RoundingMode.HALF_UP));
    }

    protected void recomputeOrderTotals(ErpSalOrder order, List<ErpSalOrderLine> lines,
                                        BigDecimal promotionDiscount) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        for (ErpSalOrderLine line : lines) {
            totalAmount = totalAmount.add(nullSafe(line.getAmount()));
            totalTaxAmount = totalTaxAmount.add(nullSafe(line.getTaxAmount()));
        }
        BigDecimal promotionDisc = nullSafe(promotionDiscount);
        if (promotionDisc.signum() > 0) {
            totalAmount = totalAmount.subtract(promotionDisc).max(BigDecimal.ZERO);
            order.setDiscountAmount(promotionDisc);
        }
        order.setTotalAmount(totalAmount.setScale(4, RoundingMode.HALF_UP));
        order.setTotalTaxAmount(totalTaxAmount.setScale(4, RoundingMode.HALF_UP));
        order.setTotalAmountWithTax(totalAmount.add(totalTaxAmount).setScale(4, RoundingMode.HALF_UP));
    }

    protected BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------- 促销最低价校验（P1-RC-021，复用 master-data validatePrice 三级语义） ----------

    /**
     * 促销结果落地后逐行触发最低价校验（UC-SAL-11 ⑥）。复用 master-data
     * {@link IErpMdMaterialSkuBiz#validatePrice} 的 OFF/WARN/HARD 三级语义：
     * OFF 放行 / WARN 放行带 LOG.warn / HARD 抛 {@code ERR_PRICE_BELOW_MIN} propagate
     * （整个 @BizMutation 事务回滚，促销变更不落库）。
     *
     * <p>赠品行（{@code amount==0}）显式跳过——L1 UC-SAL-08 要求赠品可成功生成（单价 0 语义），
     * HARD 级别下不排除将整体拒绝含赠品的促销（相对基线行为回归）。
     */
    protected void validatePromotionPrices(ErpSalOrder order, List<ErpSalOrderLine> lines,
                                           IServiceContext context) {
        for (ErpSalOrderLine line : lines) {
            Long skuId = line.getSkuId();
            if (skuId == null) {
                continue;
            }
            // 赠品行（amount==0）跳过
            if (nullSafe(line.getAmount()).signum() == 0) {
                continue;
            }
            BigDecimal qty = nullSafe(line.getQuantity());
            if (qty.signum() == 0) {
                continue;
            }
            // finalPrice = 促销后行净单价（amount/quantity，对齐「最终售价」字面）
            BigDecimal finalPrice = line.getAmount().divide(qty, 4, RoundingMode.HALF_UP);
            Long materialCategoryId = resolveMaterialCategoryId(line);
            PriceValidationResult vr = mdMaterialSkuBiz.validatePrice(
                    skuId, finalPrice, materialCategoryId, context);
            if (vr.isWarning()) {
                LOG.warn("销售订单 {} 促销后行 skuId={} 最终售价 {} 低于底线 {}（价格校验级别={} WARN 放行）",
                        order.getCode(), skuId, finalPrice, vr.getMinPrice(), vr.getLevel());
            }
        }
    }

    /**
     * 经 ORM 关系 {@code ErpSalOrderLine.material} 解析物料分类 ID，
     * 供 {@code validatePrice} 按 MaterialCategory.priceValidationLevel 分派级别。
     */
    protected Long resolveMaterialCategoryId(ErpSalOrderLine line) {
        ErpMdMaterial material = line.getMaterial();
        return material == null ? null : material.getCategoryId();
    }

    // ---------- 跨聚合写契约（报价→订单转化、发货进度回写） ----------

    @Override
    @BizAction
    public ErpSalOrder createFromQuotation(@Name("quotation") ErpSalQuotation quotation,
                                           @Name("lines") List<ErpSalQuotationLine> lines,
                                           IServiceContext context) {
        ErpSalOrder order = quotationToOrderConverter.build(quotation, lines);
        if (order.getCode() == null) {
            order.setCode("SO-FROM-Q-" + StringHelper.generateUUID());
        }
        saveEntity(order, null, context);
        IEntityDao<ErpSalOrderLine> lineDao = daoFor(ErpSalOrderLine.class);
        for (ErpSalOrderLine line : order.getLines() == null ? new ArrayList<ErpSalOrderLine>() : order.getLines()) {
            line.setOrderId(order.getId());
            lineDao.saveEntity(line);
        }
        return order;
    }

    @Override
    @BizAction
    public boolean existsActiveByQuotation(@Name("quotationId") Long quotationId, IServiceContext context) {
        if (quotationId == null) {
            return false;
        }
        // docStatus 的 xmeta 仅允许 eq/in 过滤（不支持 ne），故按 quotationId 经管道 findList 后于内存剔除已作废单。
        QueryBean q = new QueryBean();
        q.addFilter(eq("quotationId", quotationId));
        for (ErpSalOrder order : findList(q, null, context)) {
            if (!Objects.equals(ErpSalConstants.DOC_STATUS_CANCELLED, order.getDocStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @BizAction
    public void updateDeliveryStatus(@Name("orderId") Long orderId,
                                     @Name("deliveryStatus") String deliveryStatus,
                                     IServiceContext context) {
        if (orderId == null) {
            return;
        }
        ErpSalOrder order = get(String.valueOf(orderId), true, context);
        if (order == null) {
            return;
        }
        order.setDeliveryStatus(deliveryStatus);
        updateEntity(order, null, context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
