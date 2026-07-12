
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalOrderLineBiz;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.config.AppConfig;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpSalOrderLine")
public class ErpSalOrderLineBizModel extends CrudBizModel<ErpSalOrderLine> implements IErpSalOrderLineBiz {
    public ErpSalOrderLineBizModel() {
        setEntityName(ErpSalOrderLine.class.getName());
    }

    /**
     * UC-SAL-11：订单行保存时自动填充 pricingSource。
     * 若 pricingSource 为空且 unitPrice 由前端 resolvePrice 调用填充（非手工直接录入），标记为 MANUAL；
     * 实际促销/价格清单标记由 applyPricingRules 引擎写回覆盖。
     */
    @Override
    protected void defaultPrepareSave(EntityData<ErpSalOrderLine> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        fillPricingSource(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpSalOrderLine> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        fillPricingSource(entityData.getEntity());
    }

    protected void fillPricingSource(ErpSalOrderLine line) {
        if (line == null) {
            return;
        }
        if (line.getPricingSource() == null || line.getPricingSource().isEmpty()) {
            if (line.getUnitPrice() != null) {
                line.setPricingSource(ErpSalConstants.PRICING_SOURCE_MANUAL);
            }
        }
    }

    @BizLoader(forType = ErpSalOrderLine.class)
    public List<String> materialName(@ContextSource List<ErpSalOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalOrderLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
