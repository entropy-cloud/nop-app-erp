package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

/**
 * ErpMntRequest cancel per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN/ACCEPTED→CANCELLED 编排：双源状态守卫（经 {@code ErpMntRequestStateMachine} 矩阵）+ 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestCancelProcessor extends AbstractErpMntRequestProcessor {

    public ErpMntRequest cancel(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String from = request.getStatus();
        try {
            stateMachine.assertCanCancel(from);
        } catch (NopException e) {
            throw illegalRequestTransition(request, from, "OPEN 或 ACCEPTED", e);
        }
        doCancel(request, context);
        return request;
    }

    protected void doCancel(ErpMntRequest request, IServiceContext context) {
        request.setStatus(stateMachine.cancelTargetStatus());
        requestDao().updateEntity(request);
    }
}
