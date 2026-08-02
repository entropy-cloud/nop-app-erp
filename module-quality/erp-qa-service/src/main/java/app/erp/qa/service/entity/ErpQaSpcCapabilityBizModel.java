package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaSpcCapabilityBiz;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.service.processor.ErpQaSpcCapabilityCalculateCapabilityProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * SPC 过程能力分析 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构；
 * {@code docs/design/quality/spc.md}，plan 2026-07-07-0305-2 Phase 4）。calculateCapability 编排委托
 * per-mutation Processor，下游可经 Delta beans.xml 同名 bean id 覆盖。
 */
@BizModel("ErpQaSpcCapability")
public class ErpQaSpcCapabilityBizModel extends CrudBizModel<ErpQaSpcCapability> implements IErpQaSpcCapabilityBiz {

    @Inject
    ErpQaSpcCapabilityCalculateCapabilityProcessor calculateCapabilityProcessor;

    public ErpQaSpcCapabilityBizModel() {
        setEntityName(ErpQaSpcCapability.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaSpcCapability calculateCapability(@Name("chartId") Long chartId,
                                                    @Optional @Name("periodFrom") LocalDate periodFrom,
                                                    @Optional @Name("periodTo") LocalDate periodTo,
                                                    IServiceContext context) {
        return calculateCapabilityProcessor.calculateCapability(chartId, periodFrom, periodTo, context);
    }
}
