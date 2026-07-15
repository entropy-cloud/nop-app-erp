
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgCostRollupLineBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;

import java.util.List;

@BizModel("ErpMfgCostRollupLine")
public class ErpMfgCostRollupLineBizModel extends CrudBizModel<ErpMfgCostRollupLine> implements IErpMfgCostRollupLineBiz{
    public ErpMfgCostRollupLineBizModel(){
        setEntityName(ErpMfgCostRollupLine.class.getName());
    }

}
