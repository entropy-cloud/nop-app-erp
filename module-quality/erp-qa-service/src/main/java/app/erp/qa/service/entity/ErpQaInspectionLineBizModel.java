
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionLineBiz;
import app.erp.qa.dao.entity.ErpQaInspectionLine;

@BizModel("ErpQaInspectionLine")
public class ErpQaInspectionLineBizModel extends CrudBizModel<ErpQaInspectionLine> implements IErpQaInspectionLineBiz{
    public ErpQaInspectionLineBizModel(){
        setEntityName(ErpQaInspectionLine.class.getName());
    }
}
