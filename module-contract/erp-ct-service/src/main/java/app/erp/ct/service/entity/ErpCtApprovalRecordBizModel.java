
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;

@BizModel("ErpCtApprovalRecord")
public class ErpCtApprovalRecordBizModel extends CrudBizModel<ErpCtApprovalRecord> implements IErpCtApprovalRecordBiz{
    public ErpCtApprovalRecordBizModel(){
        setEntityName(ErpCtApprovalRecord.class.getName());
    }

}
