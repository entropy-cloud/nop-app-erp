package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntDowntimeEntryBiz;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.service.processor.ErpMntDowntimeEntryCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntDowntimeEntryRecordProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntDowntimeEntry")
public class ErpMntDowntimeEntryBizModel extends CrudBizModel<ErpMntDowntimeEntry> implements IErpMntDowntimeEntryBiz {

    @Inject
    ErpMntDowntimeEntryRecordProcessor recordProcessor;
    @Inject
    ErpMntDowntimeEntryCompleteProcessor completeProcessor;

    public ErpMntDowntimeEntryBizModel() {
        setEntityName(ErpMntDowntimeEntry.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry record(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        return recordProcessor.record(downtimeId, context);
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry complete(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        return completeProcessor.complete(downtimeId, context);
    }
}
