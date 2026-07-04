package app.erp.drp.biz;

import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;

public interface IErpInvDrpSafetyStockCalcBiz extends ICrudBiz<ErpInvDrpSafetyStockCalc> {

    /**
     * 运行安全库存计算：按 method（STATISTICAL/SIMPLE/DDMRP）算 calculatedSafetyStock/calculatedRop。
     */
    @BizMutation
    ErpInvDrpSafetyStockCalc calculate(@Name("calcId") Long calcId, IServiceContext context);

    /**
     * 查询参数的有效安全库存：优先级 overrideSafetyStock > calculatedSafetyStock > ErpDrpParameter.safetyStock。
     */
    @BizQuery
    BigDecimal findEffectiveSafetyStock(@Name("parameterId") Long parameterId, IServiceContext context);

    /**
     * 人工确认后回写 ErpDrpParameter.safetyStock（受配置 erp-inv.drp-ss-auto-writeback 控制，默认 false 强制人工）。
     */
    @BizMutation
    ErpInvDrpSafetyStockCalc confirmWriteback(@Name("calcId") Long calcId, IServiceContext context);
}
