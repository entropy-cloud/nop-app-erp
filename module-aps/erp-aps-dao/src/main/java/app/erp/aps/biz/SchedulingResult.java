package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 排产引擎单次运行的结果（{@code IErpApsOperationOrderBiz.scheduleForward/scheduleBackward} 返回值）。
 *
 * <p>{@code scheduledOperationIds} 为成功排定的工序；{@code conflicts} 为无法排定者及原因；
 * {@code feasible} 后向排产交期可达性标记。
 */
@DataBean
public class SchedulingResult {

    private List<ConflictReport> conflicts = new ArrayList<>();
    private List<String> scheduledOperationIds = new ArrayList<>();
    private boolean feasible = true;

    public List<ConflictReport> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictReport> conflicts) {
        this.conflicts = conflicts;
    }

    public List<String> getScheduledOperationIds() {
        return scheduledOperationIds;
    }

    public void setScheduledOperationIds(List<String> scheduledOperationIds) {
        this.scheduledOperationIds = scheduledOperationIds;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    public void addConflict(String operationOrderId, String code, String reason) {
        conflicts.add(new ConflictReport(operationOrderId, code, reason));
    }

    public void addScheduled(String operationOrderId) {
        scheduledOperationIds.add(operationOrderId);
    }
}
