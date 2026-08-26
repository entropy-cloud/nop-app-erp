
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.fin.biz.IErpFinReconciliationLineBiz;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;

@BizModel("ErpFinReconciliationLine")
public class ErpFinReconciliationLineBizModel extends AbstractErpCrudBizModel<ErpFinReconciliationLine> implements IErpFinReconciliationLineBiz{
    public ErpFinReconciliationLineBizModel(){
        setEntityName(ErpFinReconciliationLine.class.getName());
    }
}
