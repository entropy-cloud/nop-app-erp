package app.erp.qa.service.processor;

import app.erp.qa.biz.IErpQaRecallTargetBiz;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 召回事件 per-mutation Processor 共享基类（R6.6）。承载 close/generateReturns/locateTargets/notifyCustomers/
 * register 五个 per-mutation Processor 共用的加载、状态守卫与召回目标加载辅助（单一真相源，对齐
 * {@code processor-extension-pattern.md} facade protected helper 范式）。子类只编排单 mutation 步骤顺序。
 *
 * <p>注：召回审批 S-mutation（submitForApproval/approve/reject/reverseApprove/withdrawApproval）经
 * {@link ErpQaRecallProcessor} + 平台 approval-support，不在本基类范围。
 */
public abstract class AbstractErpQaRecallProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpQaRecallTargetBiz recallTargetBiz;

    protected IEntityDao<ErpQaRecall> recallDao() {
        return daoProvider.daoFor(ErpQaRecall.class);
    }

    protected ErpQaRecall requireRecall(Long recallId, IServiceContext context) {
        if (recallId == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_NOT_FOUND).param(ErpQaErrors.ARG_RECALL_ID, recallId);
        }
        ErpQaRecall recall = recallDao().getEntityById(recallId);
        if (recall == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_NOT_FOUND).param(ErpQaErrors.ARG_RECALL_ID, recallId);
        }
        return recall;
    }

    protected void requireRecallStatus(ErpQaRecall recall, String expected, String expectedLabel) {
        String current = recall.getStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalRecallTransition(recall, current, expectedLabel);
        }
    }

    protected NopException illegalRecallTransition(ErpQaRecall recall, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected List<ErpQaRecallTarget> loadTargets(Long recallId, Set<Long> targetIds, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("recallId", recallId));
        if (targetIds != null && !targetIds.isEmpty()) {
            q.addFilter(io.nop.api.core.beans.FilterBeans.in("id", new ArrayList<>(targetIds)));
        }
        return recallTargetBiz.findList(q, null, context);
    }
}
