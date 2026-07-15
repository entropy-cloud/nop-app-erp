
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTimeEntryBiz;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import java.util.List;

@BizModel("ErpCsTimeEntry")
public class ErpCsTimeEntryBizModel extends CrudBizModel<ErpCsTimeEntry> implements IErpCsTimeEntryBiz{
    public ErpCsTimeEntryBizModel(){
        setEntityName(ErpCsTimeEntry.class.getName());
    }

    

}
