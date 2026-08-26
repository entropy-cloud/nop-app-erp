
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceAssignmentBiz;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import java.util.List;

@BizModel("ErpCrmSequenceAssignment")
public class ErpCrmSequenceAssignmentBizModel extends AbstractErpCrudBizModel<ErpCrmSequenceAssignment> implements IErpCrmSequenceAssignmentBiz{
    public ErpCrmSequenceAssignmentBizModel(){
        setEntityName(ErpCrmSequenceAssignment.class.getName());
    }

    

}
