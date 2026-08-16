
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderBomSnapshotBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomSnapshot;

@BizModel("ErpMfgWorkOrderBomSnapshot")
public class ErpMfgWorkOrderBomSnapshotBizModel extends CrudBizModel<ErpMfgWorkOrderBomSnapshot> implements IErpMfgWorkOrderBomSnapshotBiz{
    public ErpMfgWorkOrderBomSnapshotBizModel(){
        setEntityName(ErpMfgWorkOrderBomSnapshot.class.getName());
    }
}
