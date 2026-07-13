package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import app.erp.drp.biz.IErpInvDrpSafetyStockCalcBiz;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.safetystock.SafetyStockEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.config.AppConfig;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 安全库存计算 BizModel。薄委派层：{@link #calculate}/{@link #confirmWriteback}/{@link #findEffectiveSafetyStock}
 * 委派给 {@link SafetyStockEngine}（{@code mrp.md §服务层} 范式）。STATISTICAL→SIMPLE 降级在引擎内处理。
 */
@BizModel("ErpInvDrpSafetyStockCalc")
public class ErpInvDrpSafetyStockCalcBizModel extends CrudBizModel<ErpInvDrpSafetyStockCalc>
        implements IErpInvDrpSafetyStockCalcBiz {

    @Inject
    SafetyStockEngine safetyStockEngine;

    public ErpInvDrpSafetyStockCalcBizModel() {
        setEntityName(ErpInvDrpSafetyStockCalc.class.getName());
    }

    public void setSafetyStockEngine(SafetyStockEngine safetyStockEngine) {
        this.safetyStockEngine = safetyStockEngine;
    }

    @Override
    @BizMutation
    public ErpInvDrpSafetyStockCalc calculate(@Name("calcId") Long calcId, IServiceContext context) {
        return safetyStockEngine.calculate(calcId);
    }

    @Override
    @BizQuery
    public BigDecimal findEffectiveSafetyStock(@Name("parameterId") Long parameterId, IServiceContext context) {
        return safetyStockEngine.findEffectiveSafetyStockByParameterId(parameterId);
    }

    @Override
    @BizMutation
    public ErpInvDrpSafetyStockCalc confirmWriteback(@Name("calcId") Long calcId, IServiceContext context) {
        // 配置 erp-inv.drp-ss-auto-writeback 默认 false：必须人工显式调用此方法才回写（人工复核门）
        AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_AUTO_WRITEBACK,
                ErpDrpConfigs.DEFAULT_DRP_SS_AUTO_WRITEBACK);
        safetyStockEngine.confirmWriteback(calcId);
        return get(String.valueOf(calcId), false, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpInvDrpSafetyStockCalc.class)
    public List<String> materialName(@ContextSource List<ErpInvDrpSafetyStockCalc> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpSafetyStockCalc row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpSafetyStockCalc.class)
    public List<String> warehouseName(@ContextSource List<ErpInvDrpSafetyStockCalc> rows) {
        orm().batchLoadProps(rows, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpSafetyStockCalc row : rows) {
            result.add(row.orm_attached() && row.getWarehouse() != null ? row.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpSafetyStockCalc.class)
    public List<String> orgName(@ContextSource List<ErpInvDrpSafetyStockCalc> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpSafetyStockCalc row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
