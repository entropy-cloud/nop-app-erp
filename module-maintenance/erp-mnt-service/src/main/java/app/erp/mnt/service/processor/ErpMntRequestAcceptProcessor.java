package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpMntRequest accept per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN→ACCEPTED 受理编排：状态守卫 + 生成响应式访问（DRAFT, visitType=RESPONSIVE）+ 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestAcceptProcessor extends AbstractErpMntRequestProcessor {

    public ErpMntRequest accept(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        validateTransition(request, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "OPEN");
        ErpMntVisit visit = generateResponsiveVisit(request, context);
        doAccept(request, visit, context);
        return request;
    }

    protected ErpMntVisit generateResponsiveVisit(ErpMntRequest request, IServiceContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "VST-REQ-" + request.getId());
        data.put("equipmentId", request.getEquipmentId());
        data.put("visitDate", CoreMetrics.currentDate());
        data.put("status", ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        data.put("visitType", ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE);
        data.put("assignedTo", request.getAssignedTo() != null ? request.getAssignedTo() : request.getRequestedBy());
        return visitBiz.save(data, context);
    }

    protected void doAccept(ErpMntRequest request, ErpMntVisit visit, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED);
        requestDao().updateEntity(request);
    }
}
