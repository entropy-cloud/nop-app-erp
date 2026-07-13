
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntVisitTaskBiz;
import app.erp.mnt.dao.entity.ErpMntVisitTask;

@BizModel("ErpMntVisitTask")
public class ErpMntVisitTaskBizModel extends CrudBizModel<ErpMntVisitTask> implements IErpMntVisitTaskBiz{
    public ErpMntVisitTaskBizModel(){
        setEntityName(ErpMntVisitTask.class.getName());
    }

    @BizLoader(forType = ErpMntVisitTask.class)
    public List<String> visitCode(@ContextSource List<ErpMntVisitTask> list) {
        orm().batchLoadProps(list, Collections.singleton("visit"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntVisitTask entity : list) {
            result.add(entity.getVisit() != null ? entity.getVisit().getCode() : null);
        }
        return result;
    }
}
