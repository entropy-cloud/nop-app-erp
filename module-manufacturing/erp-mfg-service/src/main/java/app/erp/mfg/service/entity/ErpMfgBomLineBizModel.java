
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgBomLineBiz;
import app.erp.mfg.dao.entity.ErpMfgBomLine;

import java.util.List;

@BizModel("ErpMfgBomLine")
public class ErpMfgBomLineBizModel extends AbstractErpCrudBizModel<ErpMfgBomLine> implements IErpMfgBomLineBiz{
    public ErpMfgBomLineBizModel(){
        setEntityName(ErpMfgBomLine.class.getName());
    }

}
