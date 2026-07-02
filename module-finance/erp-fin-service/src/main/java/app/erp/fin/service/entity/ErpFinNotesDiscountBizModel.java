
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinNotesDiscountBiz;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;

@BizModel("ErpFinNotesDiscount")
public class ErpFinNotesDiscountBizModel extends CrudBizModel<ErpFinNotesDiscount> implements IErpFinNotesDiscountBiz{
    public ErpFinNotesDiscountBizModel(){
        setEntityName(ErpFinNotesDiscount.class.getName());
    }
}
