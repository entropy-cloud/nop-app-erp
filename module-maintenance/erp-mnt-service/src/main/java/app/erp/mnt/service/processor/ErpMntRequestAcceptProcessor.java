package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.support.DecommissionedEquipmentGuard;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpMntRequest accept per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN→ACCEPTED 受理编排：状态守卫 + 生成响应式访问（DRAFT, visitType=RESPONSIVE）+ 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestAcceptProcessor extends AbstractErpMntRequestProcessor {

    // RC-R1.77 / UC-MAIN-08：处置前登记的 OPEN 请求，accept 时显式拒绝（对已处置设备的新维护工作
    // 开访问正是 L1 禁止语义；用户显式操作报错可理解且事务回滚，区别于批量路径的查询侧排除豁免）。
    @Inject
    DecommissionedEquipmentGuard decommissionedGuard;

    public ErpMntRequest accept(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String from = request.getStatus();
        try {
            stateMachine.assertCanAccept(from);
        } catch (NopException e) {
            throw illegalRequestTransition(request, from, ErpMntDaoConstants.REQUEST_STATUS_OPEN, e);
        }
        decommissionedGuard.rejectIfDecommissioned(request.getEquipmentId(), context);
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
        // RC-R1.75 / UC-MAIN-05（D5）：显式反向指针回填（code 命名约定保留——幂等锚点复用），
        // 供 visit complete 写回 request COMPLETED 消费；不套用任务模板（模板属计划性维护语义）。
        data.put("requestId", request.getId());
        return visitBiz.save(data, context);
    }

    protected void doAccept(ErpMntRequest request, ErpMntVisit visit, IServiceContext context) {
        request.setStatus(stateMachine.acceptTargetStatus());
        requestDao().updateEntity(request);
    }
}
