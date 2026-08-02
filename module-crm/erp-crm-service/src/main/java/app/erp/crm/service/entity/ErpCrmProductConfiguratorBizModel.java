
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmProductConfiguratorBiz;
import app.erp.crm.dao.entity.ErpCrmProductConfigurator;
import app.erp.crm.service.processor.ErpCrmProductConfiguratorGenerateQuoteProcessor;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * 产品配置器 BizModel（plan 2026-07-07-1430-2 §Phase 2）。{@link #generateQuote} 委托
 * {@link ErpCrmProductConfiguratorGenerateQuoteProcessor} 编排配置→定价→报价生成跨域链路。
 *
 * <p>对齐 {@code docs/design/crm/README.md}（CPQ §产品配置器）。
 */
@BizModel("ErpCrmProductConfigurator")
public class ErpCrmProductConfiguratorBizModel extends CrudBizModel<ErpCrmProductConfigurator>
        implements IErpCrmProductConfiguratorBiz {

    @Inject
    ErpCrmProductConfiguratorGenerateQuoteProcessor generateQuoteProcessor;

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
        return generateQuoteProcessor.generateQuote(configuratorId, selectedFeatures, bundlePricingId,
                priceRuleContext, leadId, context);
    }

    

}
