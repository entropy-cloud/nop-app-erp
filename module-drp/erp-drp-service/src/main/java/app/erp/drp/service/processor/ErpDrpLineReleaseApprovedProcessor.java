package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.drp.DrpReleaseService;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpDrpLine releaseApproved per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量释放编排：委派 {@link DrpReleaseService#releaseApproved}（计划下所有 APPROVED 行逐行释放，全部 ORDERED/CANCELLED 后计划→EXECUTED）。
 * 原 BizModel 返回 null（仅触发副作用），本 Processor 保持同语义。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpLineReleaseApprovedProcessor {

    @Inject
    DrpReleaseService drpReleaseService;

    public ErpDrpPlan releaseApproved(Long planId, IServiceContext context) {
        drpReleaseService.releaseApproved(planId);
        return null;
    }
}
