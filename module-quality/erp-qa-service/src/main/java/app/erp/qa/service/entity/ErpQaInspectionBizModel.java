
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.dao.entity.ErpQaInspection;

@BizModel("ErpQaInspection")
public class ErpQaInspectionBizModel extends CrudBizModel<ErpQaInspection> implements IErpQaInspectionBiz{
    public ErpQaInspectionBizModel(){
        setEntityName(ErpQaInspection.class.getName());
    }
}
