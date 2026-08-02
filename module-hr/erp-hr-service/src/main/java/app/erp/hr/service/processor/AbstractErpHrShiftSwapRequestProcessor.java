package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

/**
 * 排班调换审批 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 submit/approve 共用的加载与状态转换守卫辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 *
 * <p>假设：ErpHrErrors 未定义调换申请 not-found 专用错误码，故 {@link #requireSwapRequest} 复刻
 * {@code CrudBizModel.requireEntity} 的语义，不存在时抛平台 {@link UnknownEntityException}（与原 BizModel 行为一致）。
 */
public abstract class AbstractErpHrShiftSwapRequestProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    protected IEntityDao<ErpHrShiftSwapRequest> swapRequestDao() {
        return daoProvider.daoFor(ErpHrShiftSwapRequest.class);
    }

    protected ErpHrShiftSwapRequest requireSwapRequest(String id, IServiceContext context) {
        ErpHrShiftSwapRequest req = swapRequestDao().getEntityById(id);
        if (req == null) {
            throw new UnknownEntityException(swapRequestDao().getEntityName(), id);
        }
        return req;
    }

    protected void assertTransition(ErpHrShiftSwapRequest req, String expectedFrom, String targetTo) {
        String current = req.getStatus();
        if (current == null || !current.equals(expectedFrom)) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SWAP_REQUEST_ID, req.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, targetTo);
        }
    }
}
