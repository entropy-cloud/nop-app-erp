package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaSpcCapabilityBiz;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.service.spc.SpcCapabilityCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.time.LocalDate;

/**
 * SPC 过程能力分析 BizModel（{@code docs/design/quality/spc.md}，plan 2026-07-07-0305-2 Phase 4）。
 */
@BizModel("ErpQaSpcCapability")
public class ErpQaSpcCapabilityBizModel extends CrudBizModel<ErpQaSpcCapability> implements IErpQaSpcCapabilityBiz {

    @Inject
    SpcCapabilityCalculator spcCapabilityCalculator;

    public ErpQaSpcCapabilityBizModel() {
        setEntityName(ErpQaSpcCapability.class.getName());
    }

    @BizLoader(forType = ErpQaSpcCapability.class)
    public List<String> orgName(@ContextSource List<ErpQaSpcCapability> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcCapability entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpQaSpcCapability.class)
    public List<String> chartCode(@ContextSource List<ErpQaSpcCapability> list) {
        orm().batchLoadProps(list, Collections.singleton("chart"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcCapability entity : list) {
            result.add(entity.getChart() != null ? entity.getChart().getCode() : null);
        }
        return result;
    }

    public void setSpcCapabilityCalculator(SpcCapabilityCalculator spcCapabilityCalculator) {
        this.spcCapabilityCalculator = spcCapabilityCalculator;
    }

    @Override
    @BizMutation
    public ErpQaSpcCapability calculateCapability(@Name("chartId") Long chartId,
                                                    @Optional @Name("periodFrom") LocalDate periodFrom,
                                                    @Optional @Name("periodTo") LocalDate periodTo,
                                                    IServiceContext context) {
        return spcCapabilityCalculator.calculateCapability(chartId, periodFrom, periodTo, context);
    }
}
