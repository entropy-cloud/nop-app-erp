package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import io.nop.core.context.IServiceContext;

import java.util.Objects;

/**
 * ErpMntRequest rejectRequest per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN/ACCEPTED→REJECTED 编排：双源状态守卫 + 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntRequestProcessor}。
 */
public class ErpMntRequestRejectRequestProcessor extends AbstractErpMntRequestProcessor {

    public ErpMntRequest rejectRequest(Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String status = request.getStatus();
        if (status == null || (!Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_OPEN)
                && !Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED))) {
            throw illegalRequestTransition(request, status, "OPEN 或 ACCEPTED");
        }
        doReject(request, context);
        return request;
    }

    protected void doReject(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_REJECTED);
        requestDao().updateEntity(request);
    }
}
