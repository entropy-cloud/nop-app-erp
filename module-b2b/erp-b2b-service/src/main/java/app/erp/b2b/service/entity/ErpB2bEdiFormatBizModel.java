
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bEdiFormatBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;

@BizModel("ErpB2bEdiFormat")
public class ErpB2bEdiFormatBizModel extends CrudBizModel<ErpB2bEdiFormat> implements IErpB2bEdiFormatBiz{
    public ErpB2bEdiFormatBizModel(){
        setEntityName(ErpB2bEdiFormat.class.getName());
    }
}
