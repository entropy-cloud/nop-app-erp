package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntScheduleBiz;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.service.processor.ErpMntScheduleGenerateDueVisitsProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

@BizModel("ErpMntSchedule")
public class ErpMntScheduleBizModel extends CrudBizModel<ErpMntSchedule> implements IErpMntScheduleBiz {

    @Inject
    ErpMntScheduleGenerateDueVisitsProcessor generateDueVisitsProcessor;

    public ErpMntScheduleBizModel() {
        setEntityName(ErpMntSchedule.class.getName());
    }

    @Override
    @BizMutation
    public Integer generateDueVisits(@Name("asOfDate") LocalDate asOfDate, IServiceContext context) {
        return generateDueVisitsProcessor.generateDueVisits(asOfDate, context);
    }
}
