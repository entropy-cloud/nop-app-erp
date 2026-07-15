
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgBomLineBiz;
import app.erp.mfg.dao.entity.ErpMfgBomLine;

import java.util.List;

@BizModel("ErpMfgBomLine")
public class ErpMfgBomLineBizModel extends CrudBizModel<ErpMfgBomLine> implements IErpMfgBomLineBiz{
    public ErpMfgBomLineBizModel(){
        setEntityName(ErpMfgBomLine.class.getName());
    }

}
