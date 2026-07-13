package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.entity.ErpMntEquipment;
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

@BizModel("ErpMntEquipment")
public class ErpMntEquipmentBizModel extends CrudBizModel<ErpMntEquipment> implements IErpMntEquipmentBiz {
    public ErpMntEquipmentBizModel() {
        setEntityName(ErpMntEquipment.class.getName());
    }

    @BizLoader(forType = ErpMntEquipment.class)
    public List<String> orgName(@ContextSource List<ErpMntEquipment> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntEquipment entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntEquipment.class)
    public List<String> categoryName(@ContextSource List<ErpMntEquipment> list) {
        orm().batchLoadProps(list, Collections.singleton("category"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntEquipment entity : list) {
            result.add(entity.getCategory() != null ? entity.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntEquipment.class)
    public List<String> locationName(@ContextSource List<ErpMntEquipment> list) {
        orm().batchLoadProps(list, Collections.singleton("location"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntEquipment entity : list) {
            result.add(entity.getLocation() != null ? entity.getLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntEquipment.class)
    public List<String> assetCode(@ContextSource List<ErpMntEquipment> list) {
        orm().batchLoadProps(list, Collections.singleton("asset"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntEquipment entity : list) {
            result.add(entity.getAsset() != null ? entity.getAsset().getCode() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public ErpMntEquipment changeStatus(@Name("equipmentId") Long equipmentId,
                                        @Name("newStatus") String newStatus,
                                        IServiceContext context) {
        ErpMntEquipment equipment = requireEntity(String.valueOf(equipmentId), null, context);
        equipment.setStatus(newStatus);
        updateEntity(equipment, null, context);
        return equipment;
    }
}
