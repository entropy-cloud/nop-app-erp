package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntScheduleBiz;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.service.support.ScheduleDueGenerator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

@BizModel("ErpMntSchedule")
public class ErpMntScheduleBizModel extends CrudBizModel<ErpMntSchedule> implements IErpMntScheduleBiz {

    @jakarta.inject.Inject
    ScheduleDueGenerator scheduleDueGenerator;

    public ErpMntScheduleBizModel() {
        setEntityName(ErpMntSchedule.class.getName());
    }

    @BizLoader(forType = ErpMntSchedule.class)
    public List<String> equipmentCode(@ContextSource List<ErpMntSchedule> list) {
        orm().batchLoadProps(list, Collections.singleton("equipment"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntSchedule entity : list) {
            result.add(entity.getEquipment() != null ? entity.getEquipment().getCode() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public Integer generateDueVisits(@Name("asOfDate") LocalDate asOfDate, IServiceContext context) {
        return scheduleDueGenerator.generateDueVisits(asOfDate, context);
    }
}
