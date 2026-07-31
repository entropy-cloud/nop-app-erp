package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.processor.ErpMntVisitCancelProcessor;
import app.erp.mnt.service.processor.ErpMntVisitCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntVisitScheduleProcessor;
import app.erp.mnt.service.processor.ErpMntVisitStartProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntVisit")
public class ErpMntVisitBizModel extends CrudBizModel<ErpMntVisit> implements IErpMntVisitBiz {

    @Inject
    ErpMntVisitScheduleProcessor scheduleProcessor;
    @Inject
    ErpMntVisitStartProcessor startProcessor;
    @Inject
    ErpMntVisitCompleteProcessor completeProcessor;
    @Inject
    ErpMntVisitCancelProcessor cancelProcessor;

    public ErpMntVisitBizModel() {
        setEntityName(ErpMntVisit.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntVisit schedule(@Name("visitId") Long visitId, IServiceContext context) {
        return scheduleProcessor.schedule(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit start(@Name("visitId") Long visitId, IServiceContext context) {
        return startProcessor.start(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit complete(@Name("visitId") Long visitId, IServiceContext context) {
        return completeProcessor.complete(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit cancel(@Name("visitId") Long visitId, IServiceContext context) {
        return cancelProcessor.cancel(visitId, context);
    }
}
