package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.entity.NcrLifecycleService;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaInspection failInspection per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 PENDING→REJECTED 终态翻转 + posted 簿记 + 自动生成 NCR（{@code docs/design/quality/state-machine.md §适用对象一`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaInspectionProcessor}。
 *
 * <p>result 轴来源态守卫委托 {@code ErpQaInspectionResultStateMachine.assertCanFailInspection}（非法边→领域码
 * {@code ERR_INVALID_INSPECTION_STATUS_TRANSITION}）；目标态委托 {@code resultStateMachine.failInspectionTargetStatus()}。
 */
public class ErpQaInspectionFailInspectionProcessor extends AbstractErpQaInspectionProcessor {

    @Inject
    NcrLifecycleService ncrLifecycleService;

    public ErpQaInspection failInspection(Long inspectionId, IServiceContext context) {
        ErpQaInspection inspection = requireInspection(inspectionId, context);
        String current = inspection.getResult();
        try {
            resultStateMachine.assertCanFailInspection(current);
        } catch (NopException e) {
            throw illegalInspectionTransition(inspection, current, "PENDING（终态不可恢复，复检请新建质检单）");
        }
        inspection.setResult(resultStateMachine.failInspectionTargetStatus());
        markPosted(inspection, context);
        inspectionDao().updateEntity(inspection);
        ncrLifecycleService.autoCreateNcrFromInspection(inspection, loadLines(inspectionId), context);
        return inspection;
    }
}
