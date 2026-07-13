
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.service.crp.CrpLoadCalculator;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgCrpLoad")
public class ErpMfgCrpLoadBizModel extends CrudBizModel<ErpMfgCrpLoad> implements IErpMfgCrpLoadBiz {
    @Inject
    CrpLoadCalculator crpLoadCalculator;

    public ErpMfgCrpLoadBizModel() {
        setEntityName(ErpMfgCrpLoad.class.getName());
    }

    public void setCrpLoadCalculator(CrpLoadCalculator crpLoadCalculator) {
        this.crpLoadCalculator = crpLoadCalculator;
    }

    @Override
    @BizMutation
    public Integer calculateLoad(@Name("periodFrom") LocalDate periodFrom,
                                 @Name("periodTo") LocalDate periodTo,
                                 @Optional @Name("workcenterIds") List<Long> workcenterIds,
                                 IServiceContext context) {
        return crpLoadCalculator.calculateLoad(periodFrom, periodTo, workcenterIds);
    }

    @Override
    @BizQuery
    public List<CrpLoadReportItem> getLoadReport(@Name("periodFrom") LocalDate periodFrom,
                                                 @Name("periodTo") LocalDate periodTo,
                                                 @Optional @Name("workcenterIds") List<Long> workcenterIds,
                                                 IServiceContext context) {
        return crpLoadCalculator.getLoadReport(periodFrom, periodTo, workcenterIds);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------

    @BizLoader(forType = ErpMfgCrpLoad.class)
    public List<String> workcenterName(@ContextSource List<ErpMfgCrpLoad> loads) {
        orm().batchLoadProps(loads, Collections.singleton("workcenter"));
        List<String> result = new ArrayList<>(loads.size());
        for (ErpMfgCrpLoad load : loads) {
            result.add(load.getWorkcenter() != null ? load.getWorkcenter().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgCrpLoad.class)
    public List<String> orgName(@ContextSource List<ErpMfgCrpLoad> loads) {
        orm().batchLoadProps(loads, Collections.singleton("org"));
        List<String> result = new ArrayList<>(loads.size());
        for (ErpMfgCrpLoad load : loads) {
            result.add(load.getOrg() != null ? load.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgCrpLoad.class)
    public List<String> workOrderNo(@ContextSource List<ErpMfgCrpLoad> loads) {
        orm().batchLoadProps(loads, Collections.singleton("workOrder"));
        List<String> result = new ArrayList<>(loads.size());
        for (ErpMfgCrpLoad load : loads) {
            result.add(load.getWorkOrder() != null ? load.getWorkOrder().getCode() : null);
        }
        return result;
    }
}
