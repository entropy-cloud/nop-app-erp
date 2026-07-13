
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntEquipmentCategoryBiz;
import app.erp.mnt.dao.entity.ErpMntEquipmentCategory;

@BizModel("ErpMntEquipmentCategory")
public class ErpMntEquipmentCategoryBizModel extends CrudBizModel<ErpMntEquipmentCategory> implements IErpMntEquipmentCategoryBiz{
    public ErpMntEquipmentCategoryBizModel(){
        setEntityName(ErpMntEquipmentCategory.class.getName());
    }

    @BizLoader(forType = ErpMntEquipmentCategory.class)
    public List<String> parentName(@ContextSource List<ErpMntEquipmentCategory> list) {
        orm().batchLoadProps(list, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntEquipmentCategory entity : list) {
            result.add(entity.getParent() != null ? entity.getParent().getName() : null);
        }
        return result;
    }
}
