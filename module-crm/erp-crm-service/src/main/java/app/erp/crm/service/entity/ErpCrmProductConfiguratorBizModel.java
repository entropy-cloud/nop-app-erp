
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmProductConfiguratorBiz;
import app.erp.crm.dao.entity.ErpCrmBundlePricing;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;
import app.erp.crm.dao.entity.ErpCrmConfigRule;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmPriceRule;
import app.erp.crm.dao.entity.ErpCrmProductConfigurator;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.BundlePricingCalculator;
import app.erp.crm.service.support.PriceRuleEngine;
import app.erp.crm.service.support.ProductConfigRuleEngine;
import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.json.JSON;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 产品配置器 BizModel（plan 2026-07-07-1430-2 §Phase 2）。
 *
 * <p>{@link #generateQuote} 编排配置→定价→报价生成跨域链路：
 * <ol>
 *   <li>校验配置器 isActive 且在生效期间，否则抛 {@code ERR_CPQ_CONFIGURATOR_INACTIVE}。</li>
 *   <li>配置规则评估 → 配置快照(JSON)。</li>
 *   <li>{@code bundlePricingId} 提供时经 {@link BundlePricingCalculator} 计算捆绑价格；
 *       否则 {@code priceRuleContext} 提供时经 {@link PriceRuleEngine} 解析最优价格；
 *       均无提供时 totalAmount 取 {@code priceRuleContext.basePrice}（标准定价）。</li>
 *   <li>调 {@link IErpSalQuotationBiz#save}（{@code ICrudBiz.save}，0549-2 范式）建报价单。</li>
 *   <li>{@code leadId} 提供时回写 {@code lead.relatedBillType/relatedBillCode}。</li>
 * </ol>
 *
 * <p>跨域经 I*Biz 接口（非 IDaoProvider），符合 AGENTS.md。{@code IErpSalQuotationBiz}
 * 在 sales 域；CRM 仅通过 ICrudBiz.save 标准管道建单，不直接操作 sales 的 dao。
 */
@BizModel("ErpCrmProductConfigurator")
public class ErpCrmProductConfiguratorBizModel extends CrudBizModel<ErpCrmProductConfigurator>
        implements IErpCrmProductConfiguratorBiz {

    @Inject
    IErpSalQuotationBiz quotationBiz;

    @Inject
    ProductConfigRuleEngine configRuleEngine;

    @Inject
    PriceRuleEngine priceRuleEngine;

    @Inject
    BundlePricingCalculator bundlePricingCalculator;

    public ErpCrmProductConfiguratorBizModel() {
        setEntityName(ErpCrmProductConfigurator.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalQuotation generateQuote(@Name("configuratorId") Long configuratorId,
                                         @Name("selectedFeatures") Map<String, String> selectedFeatures,
                                         @Optional @Name("bundlePricingId") Long bundlePricingId,
                                         @Optional @Name("priceRuleContext") Map<String, Object> priceRuleContext,
                                         @Optional @Name("leadId") Long leadId,
                                         IServiceContext context) {
        ErpCrmProductConfigurator configurator = requireConfiguratorActive(configuratorId, context);

        // 配置规则评估 → 配置快照(JSON)
        List<ErpCrmConfigRule> rules = loadConfigRules(configuratorId);
        Map<String, ProductConfigRuleEngine.EvaluationResult> eval =
                configRuleEngine.evaluate(selectedFeatures, rules);
        String configSnapshot = buildConfigSnapshot(selectedFeatures, eval);

        // 定价计算（currencyId 优先取 priceRuleContext.currencyId）
        Long currencyId = readLong(priceRuleContext, "currencyId");
        BigDecimal totalAmount;
        String pricingSource;
        if (bundlePricingId != null) {
            BundlePricingCalculator.BundleResult br = computeBundle(bundlePricingId);
            totalAmount = br.getFinalAmount();
            pricingSource = "BUNDLE:" + br.getAppliedRule();
        } else if (priceRuleContext != null && !priceRuleContext.isEmpty()) {
            PriceRuleEngine.PriceResult pr = computePriceRule(priceRuleContext);
            if (!pr.isMatched() && readBigDecimal(priceRuleContext, "basePrice") == null) {
                throw new NopException(ErpCrmErrors.ERR_CPQ_NO_PRICE_MATCHED)
                        .param(ErpCrmErrors.ARG_PRODUCT_ID, readLong(priceRuleContext, "productId"))
                        .param(ErpCrmErrors.ARG_CUSTOMER_ID, readLong(priceRuleContext, "customerId"))
                        .param(ErpCrmErrors.ARG_QUANTITY, readBigDecimal(priceRuleContext, "quantity"));
            }
            totalAmount = pr.isMatched()
                    ? pr.getFinalPrice()
                    : readBigDecimal(priceRuleContext, "basePrice");
            pricingSource = pr.isMatched() ? "PRICE_RULE" : "BASE_PRICE";
        } else {
            throw new NopException(ErpCrmErrors.ERR_CPQ_NO_PRICE_MATCHED)
                    .param(ErpCrmErrors.ARG_PRODUCT_ID, null)
                    .param(ErpCrmErrors.ARG_CUSTOMER_ID, null)
                    .param(ErpCrmErrors.ARG_QUANTITY, null);
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        if (currencyId == null) {
            // 无显式 currencyId：拒绝（report quotation save 强制 currencyId 非空）
            throw new NopException(ErpCrmErrors.ERR_CPQ_NO_PRICE_MATCHED)
                    .param(ErpCrmErrors.ARG_PRODUCT_ID, readLong(priceRuleContext, "productId"))
                    .param(ErpCrmErrors.ARG_CUSTOMER_ID, readLong(priceRuleContext, "customerId"))
                    .param(ErpCrmErrors.ARG_QUANTITY, readBigDecimal(priceRuleContext, "quantity"));
        }

        // 跨域建报价单（IErpSalQuotationBiz.save，0549-2 范式）
        Long leadOrgId = null;
        Long leadCustomerId = null;
        ErpCrmLead lead = null;
        if (leadId != null) {
            lead = leadDao().getEntityById(leadId);
            if (lead != null) {
                leadOrgId = lead.getOrgId();
                leadCustomerId = lead.getPartnerId();
            }
        }
        Map<String, Object> quotationData = buildQuotationData(
                configurator, configSnapshot, totalAmount, currencyId,
                leadOrgId, leadCustomerId, pricingSource, priceRuleContext);
        ErpSalQuotation quotation = quotationBiz.save(quotationData, context);

        // lead 弱指针回写
        if (lead != null) {
            lead.setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION);
            lead.setRelatedBillCode(quotation.getCode());
            leadDao().updateEntity(lead);
        }
        return quotation;
    }

    // ---------- 校验 ----------

    protected ErpCrmProductConfigurator requireConfiguratorActive(Long configuratorId, IServiceContext context) {
        if (configuratorId == null) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_CONFIGURATOR_INACTIVE)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, configuratorId);
        }
        ErpCrmProductConfigurator configurator = get(String.valueOf(configuratorId), false, context);
        if (configurator == null || !Boolean.TRUE.equals(configurator.getIsActive())) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_CONFIGURATOR_INACTIVE)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, configuratorId);
        }
        LocalDate today = CoreMetrics.today();
        if (configurator.getEffectiveFrom() != null && today.isBefore(configurator.getEffectiveFrom())) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_CONFIGURATOR_INACTIVE)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, configuratorId);
        }
        if (configurator.getEffectiveTo() != null && today.isAfter(configurator.getEffectiveTo())) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_CONFIGURATOR_INACTIVE)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, configuratorId);
        }
        return configurator;
    }

    // ---------- 加载 ----------

    protected List<ErpCrmConfigRule> loadConfigRules(Long configuratorId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("configuratorId", configuratorId));
        return configRuleDao().findAllByQuery(q);
    }

    protected BundlePricingCalculator.BundleResult computeBundle(Long bundlePricingId) {
        ErpCrmBundlePricing bundle = bundleDao().getEntityById(bundlePricingId);
        QueryBean q = new QueryBean();
        q.addFilter(eq("bundleId", bundlePricingId));
        List<ErpCrmBundlePricingLine> lines = bundleLineDao().findAllByQuery(q);
        return bundlePricingCalculator.calculate(bundle, lines);
    }

    protected PriceRuleEngine.PriceResult computePriceRule(Map<String, Object> ctx) {
        Long productId = readLong(ctx, "productId");
        Long customerId = readLong(ctx, "customerId");
        BigDecimal quantity = readBigDecimal(ctx, "quantity");
        Long ctxCurrency = readLong(ctx, "currencyId");
        BigDecimal basePrice = readBigDecimal(ctx, "basePrice");
        LocalDate now = CoreMetrics.today();
        List<ErpCrmPriceRule> activeRules = loadActivePriceRules(productId, customerId, ctxCurrency);
        return priceRuleEngine.resolvePrice(productId, customerId, quantity, ctxCurrency, now, basePrice, activeRules);
    }

    protected List<ErpCrmPriceRule> loadActivePriceRules(Long productId, Long customerId, Long currencyId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        List<ErpCrmPriceRule> all = priceRuleDao().findAllByQuery(q);
        // 进一步缩小到 productId 维度（含全局规则 productId=null），减少 PriceRuleEngine 输入集
        List<ErpCrmPriceRule> filtered = new ArrayList<>();
        for (ErpCrmPriceRule rule : all) {
            if (rule.getProductId() != null && !Objects.equals(rule.getProductId(), productId)) {
                continue;
            }
            filtered.add(rule);
        }
        return filtered;
    }

    // ---------- 快照/报价数据构建 ----------

    protected String buildConfigSnapshot(Map<String, String> selectedFeatures,
                                         Map<String, ProductConfigRuleEngine.EvaluationResult> eval) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("selectedFeatures", selectedFeatures != null ? selectedFeatures : new HashMap<>());
        List<Map<String, Object>> marks = new ArrayList<>();
        for (Map.Entry<String, ProductConfigRuleEngine.EvaluationResult> e : eval.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("featureCode", e.getKey());
            m.put("mark", e.getValue().getMark());
            m.put("featureValue", e.getValue().getFeatureValue());
            marks.add(m);
        }
        snapshot.put("ruleEvaluation", marks);
        return JSON.stringify(snapshot);
    }

    protected Map<String, Object> buildQuotationData(ErpCrmProductConfigurator configurator,
                                                     String configSnapshot,
                                                     BigDecimal totalAmount, Long currencyId,
                                                     Long leadOrgId, Long leadCustomerId,
                                                     String pricingSource,
                                                     Map<String, Object> priceRuleContext) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CPQ-" + configurator.getId() + "-" + CoreMetrics.currentTimeMillis());
        data.put("orgId", leadOrgId != null ? leadOrgId : configurator.getOrgId());
        if (leadCustomerId != null) {
            data.put("customerId", leadCustomerId);
        } else if (priceRuleContext != null) {
            Long ctxCustomer = readLong(priceRuleContext, "customerId");
            if (ctxCustomer != null) {
                data.put("customerId", ctxCustomer);
            }
        }
        LocalDate today = CoreMetrics.today();
        data.put("businessDate", today);
        data.put("validFrom", today);
        data.put("validTo", today.plusMonths(1));
        data.put("currencyId", currencyId);
        data.put("exchangeRate", BigDecimal.ONE);
        data.put("totalAmount", totalAmount);
        data.put("totalTaxAmount", BigDecimal.ZERO);
        data.put("totalAmountWithTax", totalAmount);
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        data.put("remark", "CPQ pricingSource=" + pricingSource + "; snapshot=" + truncate(configSnapshot, 500));
        return data;
    }

    protected String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    // ---------- 辅助 ----------

    protected Long readLong(Map<String, Object> ctx, String key) {
        if (ctx == null) {
            return null;
        }
        Object v = ctx.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected BigDecimal readBigDecimal(Map<String, Object> ctx, String key) {
        if (ctx == null) {
            return null;
        }
        Object v = ctx.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected IEntityDao<ErpCrmConfigRule> configRuleDao() {
        return daoProvider().daoFor(ErpCrmConfigRule.class);
    }

    protected IEntityDao<ErpCrmPriceRule> priceRuleDao() {
        return daoProvider().daoFor(ErpCrmPriceRule.class);
    }

    protected IEntityDao<ErpCrmBundlePricing> bundleDao() {
        return daoProvider().daoFor(ErpCrmBundlePricing.class);
    }

    protected IEntityDao<ErpCrmBundlePricingLine> bundleLineDao() {
        return daoProvider().daoFor(ErpCrmBundlePricingLine.class);
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider().daoFor(ErpCrmLead.class);
    }
}
