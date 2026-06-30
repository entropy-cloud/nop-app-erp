
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtVolumeDiscountBiz;
import app.erp.contract.dao.entity.ErpCtVolumeDiscount;

@BizModel("ErpCtVolumeDiscount")
public class ErpCtVolumeDiscountBizModel extends CrudBizModel<ErpCtVolumeDiscount> implements IErpCtVolumeDiscountBiz{
    public ErpCtVolumeDiscountBizModel(){
        setEntityName(ErpCtVolumeDiscount.class.getName());
    }
}
