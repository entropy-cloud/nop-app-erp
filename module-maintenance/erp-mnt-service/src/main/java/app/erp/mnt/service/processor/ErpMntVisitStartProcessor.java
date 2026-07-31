package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;

/**
 * ErpMntVisit start per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SCHEDULED→IN_PROGRESS 编排：状态守卫 + 状态翻转 + startTime 兜底 + 落库 + 设备状态联动（UNDER_MAINTENANCE）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 */
public class ErpMntVisitStartProcessor extends AbstractErpMntVisitProcessor {

    public ErpMntVisit start(Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateTransition(visit, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, "SCHEDULED");
        doStart(visit, context);
        equipmentStatusLinker.linkToUnderMaintenance(visit.getEquipmentId(), context);
        return visit;
    }

    protected void doStart(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS);
        if (visit.getStartTime() == null) {
            visit.setStartTime(CoreMetrics.currentTimestamp());
        }
        visitDao().updateEntity(visit);
    }
}
