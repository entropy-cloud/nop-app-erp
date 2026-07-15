
package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpLeadTimeRecordBiz;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;

@BizModel("ErpInvDrpLeadTimeRecord")
public class ErpInvDrpLeadTimeRecordBizModel extends CrudBizModel<ErpInvDrpLeadTimeRecord> implements IErpInvDrpLeadTimeRecordBiz{
    public ErpInvDrpLeadTimeRecordBizModel(){
        setEntityName(ErpInvDrpLeadTimeRecord.class.getName());
    }

}
