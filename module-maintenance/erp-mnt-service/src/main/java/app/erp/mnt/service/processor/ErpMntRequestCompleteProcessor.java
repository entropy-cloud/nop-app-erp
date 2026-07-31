package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;

/**
 * ErpMntRequest complete per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_PROGRESS→COMPLETED 编排：状态守卫 + 状态翻转 + completedAt 时间戳 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestCompleteProcessor extends AbstractErpMntRequestProcessor {

    public ErpMntRequest complete(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        validateTransition(request, ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, "IN_PROGRESS");
        doComplete(request, context);
        return request;
    }

    protected void doComplete(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED);
        request.setCompletedAt(CoreMetrics.currentTimestamp());
        requestDao().updateEntity(request);
    }
}
