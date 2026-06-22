
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntEquipmentCategoryBiz;
import app.erp.mnt.dao.entity.ErpMntEquipmentCategory;

@BizModel("ErpMntEquipmentCategory")
public class ErpMntEquipmentCategoryBizModel extends CrudBizModel<ErpMntEquipmentCategory> implements IErpMntEquipmentCategoryBiz{
    public ErpMntEquipmentCategoryBizModel(){
        setEntityName(ErpMntEquipmentCategory.class.getName());
    }
}
