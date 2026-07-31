package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.safetystock.SafetyStockEngine;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvDrpSafetyStockCalc confirmWriteback per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含人工确认回写编排：配置 {@code erp-inv.drp-ss-auto-writeback} 默认 false（人工复核门）+ 委派 {@link SafetyStockEngine#confirmWriteback}
 * 回写 ErpDrpParameter.safetyStock + 回读计算记录。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvDrpSafetyStockCalcConfirmWritebackProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    SafetyStockEngine safetyStockEngine;

    public ErpInvDrpSafetyStockCalc confirmWriteback(Long calcId, IServiceContext context) {
        // 配置 erp-inv.drp-ss-auto-writeback 默认 false：必须人工显式调用此方法才回写（人工复核门）
        AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_AUTO_WRITEBACK,
                ErpDrpConfigs.DEFAULT_DRP_SS_AUTO_WRITEBACK);
        safetyStockEngine.confirmWriteback(calcId);
        return requireCalc(calcId);
    }

    // ---------- 内部辅助 ----------

    protected ErpInvDrpSafetyStockCalc requireCalc(Long calcId) {
        ErpInvDrpSafetyStockCalc calc = dao().getEntityById(calcId);
        if (calc == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SS_METHOD_UNSUPPORTED)
                    .param(ErpDrpErrors.ARG_METHOD, "安全库存计算记录不存在: " + calcId);
        }
        return calc;
    }

    private IEntityDao<ErpInvDrpSafetyStockCalc> dao() {
        return daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class);
    }
}
