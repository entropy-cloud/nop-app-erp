
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderBomOperationSnapshotBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomOperationSnapshot;

@BizModel("ErpMfgWorkOrderBomOperationSnapshot")
public class ErpMfgWorkOrderBomOperationSnapshotBizModel extends CrudBizModel<ErpMfgWorkOrderBomOperationSnapshot> implements IErpMfgWorkOrderBomOperationSnapshotBiz{
    public ErpMfgWorkOrderBomOperationSnapshotBizModel(){
        setEntityName(ErpMfgWorkOrderBomOperationSnapshot.class.getName());
    }
}
