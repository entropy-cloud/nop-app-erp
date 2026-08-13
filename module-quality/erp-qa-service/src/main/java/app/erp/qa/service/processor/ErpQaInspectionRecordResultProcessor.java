package app.erp.qa.service.processor;

import app.erp.qa.biz.InspectionLineResultInput;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.entity.InspectionResultEvaluator;
import app.erp.qa.service.entity.NcrLifecycleService;
import app.erp.qa.service.statemachine.ErpQaInspectionApprovalStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ErpQaInspection recordResult per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含行级评测 + 结果汇总 + posted + REJECTED 自动 NCR 编排（{@code docs/design/quality/state-machine.md §适用对象一`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaInspectionProcessor}。
 *
 * <p>result 轴来源态守卫委托 {@code ErpQaInspectionResultStateMachine.assertCanRecordResult}（非法边→领域码
 * {@code ERR_INVALID_INSPECTION_STATUS_TRANSITION}）；目标态由行级评测器决定（数据驱动三分支，非 Bean 固定值）。
 * approveStatus 轴让步审批目标态委托 {@link ErpQaInspectionApprovalStateMachine#concessionApproveTargetStatus()}。
 * posted 三件套写入 + NCR auto-create 保留原位（动态副作用，契约 §8）。
 */
public class ErpQaInspectionRecordResultProcessor extends AbstractErpQaInspectionProcessor {

    @Inject
    NcrLifecycleService ncrLifecycleService;

    @Inject
    ErpQaInspectionApprovalStateMachine approvalStateMachine;

    public ErpQaInspection recordResult(Long inspectionId,
                                        List<InspectionLineResultInput> lineResults,
                                        Boolean allowConcession,
                                        IServiceContext context) {
        ErpQaInspection inspection = requireInspection(inspectionId, context);
        String current = inspection.getResult();
        try {
            resultStateMachine.assertCanRecordResult(current);
        } catch (NopException e) {
            throw illegalInspectionTransition(inspection, current, "PENDING（终态不可恢复）");
        }

        List<ErpQaInspectionLine> lines = loadLines(inspectionId);
        if (lines.isEmpty()) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_LINES_EMPTY)
                    .param(ErpQaErrors.ARG_INSPECTION_CODE, inspection.getCode());
        }

        Set<Long> explicitResultLineIds = applyLineResults(lines, lineResults);
        for (ErpQaInspectionLine line : lines) {
            if (!explicitResultLineIds.contains(line.getId())) {
                line.setResult(InspectionResultEvaluator.evaluateLine(line));
            }
            lineDao().updateEntity(line);
        }

        boolean concession = Boolean.TRUE.equals(allowConcession);
        String aggregated = InspectionResultEvaluator.aggregate(lines, concession, inspection.getCode());
        inspection.setResult(aggregated);
        inspection.setPosted(Boolean.TRUE);
        if (concession && Objects.equals(aggregated, ErpQaConstants.INSPECTION_RESULT_CONDITIONAL)) {
            inspection.setApproveStatus(approvalStateMachine.concessionApproveTargetStatus());
            inspection.setApprovedBy(context.getUserId());
            inspection.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        inspectionDao().updateEntity(inspection);

        // Phase 3：REJECTED 自动生成 NCR（经 NcrLifecycleService，配置门控）
        if (Objects.equals(aggregated, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
            ncrLifecycleService.autoCreateNcrFromInspection(inspection, lines, context);
        }
        return inspection;
    }

    private Set<Long> applyLineResults(List<ErpQaInspectionLine> lines, List<InspectionLineResultInput> inputs) {
        Set<Long> explicitResultLineIds = new java.util.HashSet<>();
        if (inputs == null || inputs.isEmpty()) {
            return explicitResultLineIds;
        }
        Map<Long, InspectionLineResultInput> byId = new HashMap<>();
        Map<Integer, InspectionLineResultInput> byNo = new HashMap<>();
        for (InspectionLineResultInput in : inputs) {
            if (in.getLineId() != null) {
                byId.put(in.getLineId(), in);
            } else if (in.getLineNo() != null) {
                byNo.put(in.getLineNo(), in);
            }
        }
        for (ErpQaInspectionLine line : lines) {
            InspectionLineResultInput in = byId.get(line.getId());
            if (in == null && line.getLineNo() != null) {
                in = byNo.get(line.getLineNo());
            }
            if (in == null) {
                continue;
            }
            if (in.getMeasuredValue() != null) {
                line.setMeasuredValue(in.getMeasuredValue());
            }
            if (in.getResult() != null) {
                line.setResult(in.getResult());
                explicitResultLineIds.add(line.getId());
            }
        }
        return explicitResultLineIds;
    }
}
