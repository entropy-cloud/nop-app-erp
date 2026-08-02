package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.entity.NcrLifecycleService;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaInspection failInspection per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 PENDING→REJECTED 终态翻转 + posted 簿记 + 自动生成 NCR（{@code docs/design/quality/state-machine.md §适用对象一`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaInspectionProcessor}。
 */
public class ErpQaInspectionFailInspectionProcessor extends AbstractErpQaInspectionProcessor {

    @Inject
    NcrLifecycleService ncrLifecycleService;

    public ErpQaInspection failInspection(Long inspectionId, IServiceContext context) {
        ErpQaInspection inspection = requireInspection(inspectionId, context);
        requireInspectionPending(inspection);
        inspection.setResult(ErpQaConstants.INSPECTION_RESULT_REJECTED);
        markPosted(inspection, context);
        inspectionDao().updateEntity(inspection);
        ncrLifecycleService.autoCreateNcrFromInspection(inspection, loadLines(inspectionId), context);
        return inspection;
    }
}
