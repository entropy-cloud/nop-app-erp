package app.erp.mnt.service.processor;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.statemachine.ErpMntRequestStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 维护请求 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 accept/startRepair/complete/rejectRequest/cancel 五个 per-mutation Processor 共用的加载、状态守卫辅助（单一真相源）。
 * 子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpMntRequestProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMntVisitBiz visitBiz;

    @Inject
    ErpMntRequestStateMachine stateMachine;

    protected IEntityDao<ErpMntRequest> requestDao() {
        return daoProvider.daoFor(ErpMntRequest.class);
    }

    protected ErpMntRequest requireRequest(Long requestId, IServiceContext context) {
        ErpMntRequest request = requestDao().getEntityById(requestId);
        if (request == null) {
            throw new NopException(ErpMntErrors.ERR_REQUEST_NOT_FOUND).param(ErpMntErrors.ARG_REQUEST_ID, requestId);
        }
        return request;
    }

    protected void validateTransition(ErpMntRequest request, String expected, String expectedName) {
        String status = request.getStatus();
        if (status == null || !Objects.equals(status, expected)) {
            throw illegalRequestTransition(request, status, expectedName);
        }
    }

    protected NopException illegalRequestTransition(ErpMntRequest request, String current, String expected) {
        return illegalRequestTransition(request, current, expected, null);
    }

    /**
     * 非法迁移领域异常（带 cause）：StateMachine Bean 报告 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}，
     * 此处映射为领域 {@code ERR_INVALID_REQUEST_STATUS_TRANSITION} + 实体编号/上下文，common 码作 cause 保留（契约 §7）。
     */
    protected NopException illegalRequestTransition(ErpMntRequest request, String current, String expected, Throwable cause) {
        return new NopException(ErpMntErrors.ERR_INVALID_REQUEST_STATUS_TRANSITION, cause)
                .param(ErpMntErrors.ARG_REQUEST_CODE, request.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
