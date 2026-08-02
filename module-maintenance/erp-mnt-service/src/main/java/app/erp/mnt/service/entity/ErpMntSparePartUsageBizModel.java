package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntSparePartUsageBiz;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.service.processor.ErpMntSparePartUsageConfirmProcessor;
import app.erp.mnt.service.processor.ErpMntSparePartUsageReverseConfirmProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntSparePartUsage")
public class ErpMntSparePartUsageBizModel extends CrudBizModel<ErpMntSparePartUsage>
        implements IErpMntSparePartUsageBiz {

    @Inject
    ErpMntSparePartUsageConfirmProcessor confirmProcessor;
    @Inject
    ErpMntSparePartUsageReverseConfirmProcessor reverseConfirmProcessor;

    public ErpMntSparePartUsageBizModel() {
        setEntityName(ErpMntSparePartUsage.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntSparePartUsage confirm(@Name("usageId") Long usageId, IServiceContext context) {
        return confirmProcessor.confirm(usageId, context);
    }

    @Override
    @BizMutation
    public ErpMntSparePartUsage reverseConfirm(@Name("usageId") Long usageId, IServiceContext context) {
        return reverseConfirmProcessor.reverseConfirm(usageId, context);
    }
}
