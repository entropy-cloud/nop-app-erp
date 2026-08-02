package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntRequestBiz;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.service.processor.ErpMntRequestAcceptProcessor;
import app.erp.mnt.service.processor.ErpMntRequestCancelProcessor;
import app.erp.mnt.service.processor.ErpMntRequestCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntRequestRejectRequestProcessor;
import app.erp.mnt.service.processor.ErpMntRequestStartRepairProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntRequest")
public class ErpMntRequestBizModel extends CrudBizModel<ErpMntRequest> implements IErpMntRequestBiz {

    @Inject
    ErpMntRequestAcceptProcessor acceptProcessor;
    @Inject
    ErpMntRequestStartRepairProcessor startRepairProcessor;
    @Inject
    ErpMntRequestCompleteProcessor completeProcessor;
    @Inject
    ErpMntRequestRejectRequestProcessor rejectRequestProcessor;
    @Inject
    ErpMntRequestCancelProcessor cancelProcessor;

    public ErpMntRequestBizModel() {
        setEntityName(ErpMntRequest.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntRequest accept(@Name("requestId") Long requestId, IServiceContext context) {
        return acceptProcessor.accept(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest startRepair(@Name("requestId") Long requestId, IServiceContext context) {
        return startRepairProcessor.startRepair(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest complete(@Name("requestId") Long requestId, IServiceContext context) {
        return completeProcessor.complete(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest rejectRequest(@Name("requestId") Long requestId, IServiceContext context) {
        return rejectRequestProcessor.rejectRequest(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest cancel(@Name("requestId") Long requestId, IServiceContext context) {
        return cancelProcessor.cancel(requestId, context);
    }
}
