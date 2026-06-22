
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherTemplateLineBiz;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;

@BizModel("ErpFinVoucherTemplateLine")
public class ErpFinVoucherTemplateLineBizModel extends CrudBizModel<ErpFinVoucherTemplateLine> implements IErpFinVoucherTemplateLineBiz{
    public ErpFinVoucherTemplateLineBizModel(){
        setEntityName(ErpFinVoucherTemplateLine.class.getName());
    }
}
