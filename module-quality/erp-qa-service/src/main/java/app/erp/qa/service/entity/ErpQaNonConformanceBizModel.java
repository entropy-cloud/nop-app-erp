
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;

@BizModel("ErpQaNonConformance")
public class ErpQaNonConformanceBizModel extends CrudBizModel<ErpQaNonConformance> implements IErpQaNonConformanceBiz{
    public ErpQaNonConformanceBizModel(){
        setEntityName(ErpQaNonConformance.class.getName());
    }
}
