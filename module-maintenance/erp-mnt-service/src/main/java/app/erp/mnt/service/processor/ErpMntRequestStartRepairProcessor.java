package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

/**
 * ErpMntRequest startRepair per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 ACCEPTED→IN_PROGRESS 编排：状态守卫 + 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestStartRepairProcessor extends AbstractErpMntRequestProcessor {

    public ErpMntRequest startRepair(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String from = request.getStatus();
        try {
            stateMachine.assertCanStartRepair(from);
        } catch (NopException e) {
            throw illegalRequestTransition(request, from, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, e);
        }
        doStartRepair(request, context);
        return request;
    }

    protected void doStartRepair(ErpMntRequest request, IServiceContext context) {
        request.setStatus(stateMachine.startRepairTargetStatus());
        requestDao().updateEntity(request);
    }
}
