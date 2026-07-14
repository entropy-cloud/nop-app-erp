
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstValueAdjustmentBiz;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.processor.ErpAstValueAdjustmentProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产价值调整 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstValueAdjustmentProcessor} 全权处理。
 */
@BizModel("ErpAstValueAdjustment")
public class ErpAstValueAdjustmentBizModel extends CrudBizModel<ErpAstValueAdjustment>
        implements IErpAstValueAdjustmentBiz {

    @Inject
    ErpAstValueAdjustmentProcessor adjustmentProcessor;

    public ErpAstValueAdjustmentBizModel() {
        setEntityName(ErpAstValueAdjustment.class.getName());
    }

    @BizLoader(forType = ErpAstValueAdjustment.class)
    public List<String> orgName(@ContextSource List<ErpAstValueAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstValueAdjustment row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstValueAdjustment.class)
    public List<String> assetCode(@ContextSource List<ErpAstValueAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("asset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstValueAdjustment row : rows) {
            result.add(row.getAsset() != null ? row.getAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstValueAdjustment.class)
    public List<String> currencyName(@ContextSource List<ErpAstValueAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstValueAdjustment row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public ErpAstValueAdjustment cancel(@Name("id") Long id, IServiceContext context) {
        return adjustmentProcessor.cancel(id, context);
    }
}
