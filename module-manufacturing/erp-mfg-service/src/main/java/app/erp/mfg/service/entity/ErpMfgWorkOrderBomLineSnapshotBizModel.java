
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderBomLineSnapshotBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomLineSnapshot;

@BizModel("ErpMfgWorkOrderBomLineSnapshot")
public class ErpMfgWorkOrderBomLineSnapshotBizModel extends CrudBizModel<ErpMfgWorkOrderBomLineSnapshot> implements IErpMfgWorkOrderBomLineSnapshotBiz{
    public ErpMfgWorkOrderBomLineSnapshotBizModel(){
        setEntityName(ErpMfgWorkOrderBomLineSnapshot.class.getName());
    }
}
