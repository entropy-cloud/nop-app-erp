
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntSparePartUsageLineBiz;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;

@BizModel("ErpMntSparePartUsageLine")
public class ErpMntSparePartUsageLineBizModel extends CrudBizModel<ErpMntSparePartUsageLine> implements IErpMntSparePartUsageLineBiz{
    public ErpMntSparePartUsageLineBizModel(){
        setEntityName(ErpMntSparePartUsageLine.class.getName());
    }

    @BizLoader(forType = ErpMntSparePartUsageLine.class)
    public List<String> sparePartUsageCode(@ContextSource List<ErpMntSparePartUsageLine> list) {
        orm().batchLoadProps(list, Collections.singleton("sparePartUsage"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntSparePartUsageLine entity : list) {
            result.add(entity.getSparePartUsage() != null ? entity.getSparePartUsage().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntSparePartUsageLine.class)
    public List<String> materialName(@ContextSource List<ErpMntSparePartUsageLine> list) {
        orm().batchLoadProps(list, Collections.singleton("material"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntSparePartUsageLine entity : list) {
            result.add(entity.getMaterial() != null ? entity.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntSparePartUsageLine.class)
    public List<String> uomName(@ContextSource List<ErpMntSparePartUsageLine> list) {
        orm().batchLoadProps(list, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntSparePartUsageLine entity : list) {
            result.add(entity.getUoM() != null ? entity.getUoM().getName() : null);
        }
        return result;
    }
}
