
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherTemplateBiz;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;

@BizModel("ErpFinVoucherTemplate")
public class ErpFinVoucherTemplateBizModel extends CrudBizModel<ErpFinVoucherTemplate> implements IErpFinVoucherTemplateBiz{
    public ErpFinVoucherTemplateBizModel(){
        setEntityName(ErpFinVoucherTemplate.class.getName());
    }
}
