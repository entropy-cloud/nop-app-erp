
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmProductConfigurator;
import app.erp.sal.dao.entity.ErpSalQuotation;

import java.util.Map;

public interface IErpCrmProductConfiguratorBiz extends ICrudBiz<ErpCrmProductConfigurator>{

    /**
     * CPQ 配置→报价生成（plan 2026-07-07-1430-2 §Phase 2）。
     *
     * <p>按 {@code configuratorId} 加载配置器（须 isActive 且在生效期间），
     * {@code selectedFeatures} 触发配置规则评估生成配置快照(JSON)，
     * 经 {@code PriceRuleEngine}/{@code BundlePricingCalculator} 定价后
     * 调 {@link IErpSalQuotationBiz} {@code save}（ICrudBiz，0549-2 范式）创建正式报价单 +
     * 回写 {@code lead.relatedBillType/relatedBillCode}。
     *
     * @param configuratorId   配置器 ID（必填）
     * @param selectedFeatures 选中特征映射（featureCode → featureValue）
     * @param bundlePricingId  捆绑包 ID（可空，使用捆绑定价时填）
     * @param priceRuleContext 价格规则上下文（productId/customerId/quantity/basePrice/currencyId，可空走标准定价）
     * @param leadId           关联商机 ID（可空，填则回写弱指针）
     * @param context          服务上下文
     * @return 创建的报价单
     */
    @BizMutation
    ErpSalQuotation generateQuote(@Name("configuratorId") Long configuratorId,
                                  @Name("selectedFeatures") Map<String, String> selectedFeatures,
                                  @Optional @Name("bundlePricingId") Long bundlePricingId,
                                  @Optional @Name("priceRuleContext") Map<String, Object> priceRuleContext,
                                  @Optional @Name("leadId") Long leadId,
                                  IServiceContext context);
}
