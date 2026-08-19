package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkOrder 下达→OperationOrder 批量创建结果（UC-APS-01，{@code IErpApsOperationOrderBiz.createOperationOrdersFromWorkOrder}
 * 返回值）。三模式可观察：created（成功）/ skippedNoRouting（工艺路线缺失跳过）/ rejectedSequences（工作中心
 * 不存在拒绝的工序 sequence 列表）。
 */
@DataBean
public class WorkOrderOperationCreationResult {

    private Long workOrderId;
    private String workOrderCode;
    /** 已存在 OperationOrder（幂等守卫命中，本次零创建）。 */
    private boolean alreadyCreated;
    /** 工艺路线缺失或无工序行：整单跳过 + 告警。 */
    private boolean skippedNoRouting;
    /** 本次成功创建的 OperationOrder 数量。 */
    private int createdCount;
    /** 因工作中心不存在被拒绝创建的工序 sequence 列表（每项均已告警）。 */
    private List<Integer> rejectedSequences = new ArrayList<>();

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getWorkOrderCode() {
        return workOrderCode;
    }

    public void setWorkOrderCode(String workOrderCode) {
        this.workOrderCode = workOrderCode;
    }

    public boolean isAlreadyCreated() {
        return alreadyCreated;
    }

    public void setAlreadyCreated(boolean alreadyCreated) {
        this.alreadyCreated = alreadyCreated;
    }

    public boolean isSkippedNoRouting() {
        return skippedNoRouting;
    }

    public void setSkippedNoRouting(boolean skippedNoRouting) {
        this.skippedNoRouting = skippedNoRouting;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public List<Integer> getRejectedSequences() {
        return rejectedSequences;
    }

    public void setRejectedSequences(List<Integer> rejectedSequences) {
        this.rejectedSequences = rejectedSequences;
    }
}
