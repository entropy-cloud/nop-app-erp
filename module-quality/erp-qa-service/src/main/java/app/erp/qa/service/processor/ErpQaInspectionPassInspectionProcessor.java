package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

/**
 * ErpQaInspection passInspection per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 PENDING→ACCEPTED 终态翻转 + posted 簿记（{@code docs/design/quality/state-machine.md §适用对象一`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaInspectionProcessor}。
 *
 * <p>result 轴来源态守卫委托 {@code ErpQaInspectionResultStateMachine.assertCanPassInspection}（非法边→领域码
 * {@code ERR_INVALID_INSPECTION_STATUS_TRANSITION}）；目标态委托 {@code resultStateMachine.passInspectionTargetStatus()}。
 */
public class ErpQaInspectionPassInspectionProcessor extends AbstractErpQaInspectionProcessor {

    public ErpQaInspection passInspection(Long inspectionId, IServiceContext context) {
        ErpQaInspection inspection = requireInspection(inspectionId, context);
        String current = inspection.getResult();
        try {
            resultStateMachine.assertCanPassInspection(current);
        } catch (NopException e) {
            throw illegalInspectionTransition(inspection, current, "PENDING（终态不可恢复，复检请新建质检单）");
        }
        inspection.setResult(resultStateMachine.passInspectionTargetStatus());
        markPosted(inspection, context);
        inspectionDao().updateEntity(inspection);
        return inspection;
    }
}
