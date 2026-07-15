
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjMilestoneBiz;
import app.erp.prj.dao.entity.ErpPrjMilestone;

import java.util.List;

@BizModel("ErpPrjMilestone")
public class ErpPrjMilestoneBizModel extends CrudBizModel<ErpPrjMilestone> implements IErpPrjMilestoneBiz{
    public ErpPrjMilestoneBizModel(){
        setEntityName(ErpPrjMilestone.class.getName());
    }

}
