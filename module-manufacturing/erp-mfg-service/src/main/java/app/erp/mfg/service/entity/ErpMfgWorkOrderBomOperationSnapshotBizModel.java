
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderBomOperationSnapshotBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomOperationSnapshot;

@BizModel("ErpMfgWorkOrderBomOperationSnapshot")
public class ErpMfgWorkOrderBomOperationSnapshotBizModel extends AbstractErpCrudBizModel<ErpMfgWorkOrderBomOperationSnapshot> implements IErpMfgWorkOrderBomOperationSnapshotBiz{

    // P2-CK-mfg-006：快照不可变（RC-R1.49 LOCK_AT_CREATION）——禁止通用 update
    @SuppressWarnings("unchecked")
    @Override
    protected void defaultPrepareUpdate(EntityData entityData, io.nop.core.context.IServiceContext context) {
        throw new IllegalStateException("SNAPSHOT_IMMUTABLE");
    }



    public ErpMfgWorkOrderBomOperationSnapshotBizModel(){
        setEntityName(ErpMfgWorkOrderBomOperationSnapshot.class.getName());
    }
}
