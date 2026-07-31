package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpInvDrpSafetyStockCalcBiz;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.service.processor.ErpInvDrpSafetyStockCalcCalculateProcessor;
import app.erp.drp.service.processor.ErpInvDrpSafetyStockCalcConfirmWritebackProcessor;
import app.erp.drp.service.safetystock.SafetyStockEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 安全库存计算 BizModel。薄委派层（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）：
 * {@link #calculate}/{@link #confirmWriteback} 各委派独立自包含 Processor（编排位置迁移，STATISTICAL→SIMPLE 降级/人工复核门语义不变）；
 * {@link #findEffectiveSafetyStock} 为只读查询保留委派 {@link SafetyStockEngine}。
 */
@BizModel("ErpInvDrpSafetyStockCalc")
public class ErpInvDrpSafetyStockCalcBizModel extends CrudBizModel<ErpInvDrpSafetyStockCalc>
        implements IErpInvDrpSafetyStockCalcBiz {

    @Inject
    SafetyStockEngine safetyStockEngine;
    @Inject
    ErpInvDrpSafetyStockCalcCalculateProcessor calculateProcessor;
    @Inject
    ErpInvDrpSafetyStockCalcConfirmWritebackProcessor confirmWritebackProcessor;

    public ErpInvDrpSafetyStockCalcBizModel() {
        setEntityName(ErpInvDrpSafetyStockCalc.class.getName());
    }

    public void setSafetyStockEngine(SafetyStockEngine safetyStockEngine) {
        this.safetyStockEngine = safetyStockEngine;
    }

    @Override
    @BizMutation
    public ErpInvDrpSafetyStockCalc calculate(@Name("calcId") Long calcId, IServiceContext context) {
        return calculateProcessor.calculate(calcId, context);
    }

    @Override
    @BizQuery
    public BigDecimal findEffectiveSafetyStock(@Name("parameterId") Long parameterId, IServiceContext context) {
        return safetyStockEngine.findEffectiveSafetyStockByParameterId(parameterId);
    }

    @Override
    @BizMutation
    public ErpInvDrpSafetyStockCalc confirmWriteback(@Name("calcId") Long calcId, IServiceContext context) {
        return confirmWritebackProcessor.confirmWriteback(calcId, context);
    }
}
