package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** E3.4 TOC 试点：瓶颈工作中心清单（horizon 内负荷率超阈值者，`constraint-based-planning.md` §2）。 */
    private List<String> bottleneckMachineIds = new ArrayList<>();
    /** E3.4 TOC 试点：各工作中心 horizon 负荷率（派生链同 CrpLoadCalculator：loadHours/capacityHours）。 */
    private Map<String, BigDecimal> machineLoadRates = new LinkedHashMap<>();

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

    public List<String> getBottleneckMachineIds() {
        return bottleneckMachineIds;
    }

    public void setBottleneckMachineIds(List<String> bottleneckMachineIds) {
        this.bottleneckMachineIds = bottleneckMachineIds;
    }

    public Map<String, BigDecimal> getMachineLoadRates() {
        return machineLoadRates;
    }

    public void setMachineLoadRates(Map<String, BigDecimal> machineLoadRates) {
        this.machineLoadRates = machineLoadRates;
    }
}
