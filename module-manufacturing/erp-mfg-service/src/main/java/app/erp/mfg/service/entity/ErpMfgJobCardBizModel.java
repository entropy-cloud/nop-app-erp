
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgJobCardBiz;
import app.erp.mfg.dao.entity.ErpMfgJobCard;

@BizModel("ErpMfgJobCard")
public class ErpMfgJobCardBizModel extends CrudBizModel<ErpMfgJobCard> implements IErpMfgJobCardBiz{
    public ErpMfgJobCardBizModel(){
        setEntityName(ErpMfgJobCard.class.getName());
    }
}
