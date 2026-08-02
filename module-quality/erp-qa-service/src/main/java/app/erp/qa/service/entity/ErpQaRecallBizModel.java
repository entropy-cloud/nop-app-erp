
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaRecallBiz;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.processor.ErpQaRecallCloseProcessor;
import app.erp.qa.service.processor.ErpQaRecallGenerateReturnsProcessor;
import app.erp.qa.service.processor.ErpQaRecallLocateTargetsProcessor;
import app.erp.qa.service.processor.ErpQaRecallNotifyCustomersProcessor;
import app.erp.qa.service.processor.ErpQaRecallRegisterProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Objects;

/**
 * 召回事件 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。在 {@link CrudBizModel} 标准 CRUD 之上
 * 实现召回 5 态状态机（{@code docs/design/quality/recall.md §召回状态机`}）。单步状态翻转 cancel 留在 Facade；
 * 多步 mutation（register/locateTargets/notifyCustomers/generateReturns/close）委托 per-mutation Processor，
 * 下游可经 Delta beans.xml 同名 bean id 覆盖。
 *
 * <p>标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由平台 {@code approval-support.xbiz}
 * 提供，经 {@code app.erp.qa.service.processor.ErpQaRecallProcessor} 编排，recall.status 联动经 xbiz {@code <source>} 内联。
 * 非法迁移抛 {@link ErpQaErrors#ERR_INVALID_RECALL_STATUS_TRANSITION}。
 */
@BizModel("ErpQaRecall")
public class ErpQaRecallBizModel extends CrudBizModel<ErpQaRecall> implements IErpQaRecallBiz {

    @Inject
    ErpQaRecallRegisterProcessor registerProcessor;
    @Inject
    ErpQaRecallLocateTargetsProcessor locateTargetsProcessor;
    @Inject
    ErpQaRecallNotifyCustomersProcessor notifyCustomersProcessor;
    @Inject
    ErpQaRecallGenerateReturnsProcessor generateReturnsProcessor;
    @Inject
    ErpQaRecallCloseProcessor closeProcessor;

    public ErpQaRecallBizModel() {
        setEntityName(ErpQaRecall.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaRecall register(@Name("data") Map<String, Object> data, IServiceContext context) {
        return registerProcessor.register(data, context);
    }

    @Override
    @BizMutation
    public ErpQaRecall cancel(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        String current = recall.getStatus();
        if (current == null || (!Objects.equals(current, ErpQaConstants.RECALL_STATUS_OPEN)
                && !Objects.equals(current, ErpQaConstants.RECALL_STATUS_APPROVED)
                && !Objects.equals(current, ErpQaConstants.RECALL_STATUS_IN_PROGRESS))) {
            throw illegalRecallTransition(recall, current, "OPEN 或 APPROVED 或 IN_PROGRESS");
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CANCELLED);
        updateEntity(recall, null, context);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall locateTargets(@Name("recallId") Long recallId, IServiceContext context) {
        return locateTargetsProcessor.locateTargets(recallId, context);
    }

    @Override
    @BizMutation
    public ErpQaRecall notifyCustomers(@Name("recallId") Long recallId, IServiceContext context) {
        return notifyCustomersProcessor.notifyCustomers(recallId, context);
    }

    @Override
    @BizMutation
    public ErpQaRecall generateReturns(@Name("recallId") Long recallId, IServiceContext context) {
        return generateReturnsProcessor.generateReturns(recallId, context);
    }

    @Override
    @BizMutation
    public ErpQaRecall close(@Name("recallId") Long recallId, IServiceContext context) {
        return closeProcessor.close(recallId, context);
    }

    // ---------- helpers ----------

    private ErpQaRecall requireRecall(Long recallId, IServiceContext context) {
        if (recallId == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_NOT_FOUND).param(ErpQaErrors.ARG_RECALL_ID, recallId);
        }
        return requireEntity(String.valueOf(recallId), null, context);
    }

    private NopException illegalRecallTransition(ErpQaRecall recall, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
